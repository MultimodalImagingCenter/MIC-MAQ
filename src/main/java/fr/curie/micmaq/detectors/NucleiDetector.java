package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.ExpandMask;
import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

/**
 * Author : Camille RABIER
 * Date : 25/03/2022
 * Class for
 * - segmenting and measuring nuclei
 */
public class NucleiDetector {
    private final ImagePlus image; /*Original image*/
    private final ResultsTable rawMeasures;
    private Analyzer analyzer;
    private ImagePlus imageToMeasure; /*Projected or not image, that will be the one measured*/
    private ImagePlus imageToParticleAnalyze; /*Binary mask that particle analyzer use to count and return the ROIs*/
    private String nameExperiment; /*Name of experiment (common with spot images)*/
    private final Detector detector; /*Helper class that do the projection, thresholding and particle analyzer that are common with the spots*/
    private final String resultsDirectory; /*Directory to save results if necessary*/
    private final boolean showPreprocessingImage; /*Display or not the images (projection and binary)*/
    private boolean cellpose; /*use Cellpose, if false use thresholding*/
    private int cellposeDiameter;
    private String cellposeModel;
    private double cellposeCellproba_threshold;
    private boolean stardist; /*use Stardist, if false use thresholding*/
    String stardistModel;
    double stardistPercentileBottom;
    double stardistPercentileTop;
    double stardistProbThresh;
    double stardistNmsThresh;
    String stardistModelFile;
    double stardistScale;
    private boolean useWatershed;
    private Roi[] nucleiRois;
    private boolean finalValidation;
    private boolean useMacro;
    private String macroText;
    private boolean excludeOnEdges;
    private boolean saveMask;
    private boolean saveRois;
    private boolean showBinaryImage;
    private int measurements;

    protected String nameChannel="";

    private boolean expand4Cells=false;
    private int expandRadius = 10;
    private ArrayList<Roi[]> roisExpanded;

    /**
     * Constructor with basic parameters, the other are initialized only if needed
     * @param image image to analyze
     * @param nameExperiment name without channel
     * @param resultsDir : directory for saving results
     * @param showPreprocessingImage : display or not of the images
     */
    public NucleiDetector(ImagePlus image, String nameExperiment, String resultsDir, boolean showPreprocessingImage) {
        this.image = image;
        detector = new Detector(image, "Nucleus");
        this.showPreprocessingImage =showPreprocessingImage;
        if (nameExperiment.endsWith("_")){
            this.nameExperiment = nameExperiment.substring(0,nameExperiment.length()-1);
        }else {
            this.nameExperiment =nameExperiment;
        }
        //IJ.log("name experiment "+nameExperiment.replaceAll("[\\\\/:,;*?\"<>|]","_"));
        this.resultsDirectory =resultsDir+"/Results/"+nameExperiment.replaceAll("[\\\\/:,;*?\"<>|]","_").replaceAll(" ","");
        File dir=new File(resultsDirectory);
        if(!dir.exists()) dir.mkdirs();
        rawMeasures = new ResultsTable();
        IJ.log("nuclei detector : resultdir="+this.resultsDirectory);
    }

    public void setNucleiAssociatedRois(Roi[] associatedToCellNucleiRois){
        this.nucleiRois=associatedToCellNucleiRois;
        if (associatedToCellNucleiRois.length>0){
            if (resultsDirectory!=null && saveRois) {
                RoiManager roiManager = RoiManager.getInstance();
                if (roiManager == null) { /*if no instance of roiManager, creates one*/
                    roiManager = new RoiManager();
                } else { /*if already exists, empties it*/
                    roiManager.reset();
                }
                for (int i = 0; i < associatedToCellNucleiRois.length; i++) {
                    Roi roi = associatedToCellNucleiRois[i];
                    if (roi!=null){
                        roi.setName("Nuclei_Cell_"+(i+1));
                        roiManager.addRoi(roi);
                    }
                }
                if(roiManager.getCount()>0) {
                    String extension=(roiManager.getCount()==1)?".roi":".zip";
                    File dir=new File(resultsDirectory + "/ROI/Validated/");
                    if(!dir.exists()) dir.mkdirs();
                    if (roiManager.save(resultsDirectory + "/ROI/Validated/" + image.getTitle() + "_NucleiAssociatedToCellROIs"+extension)) {
                        IJ.log("The nuclei ROIs of " + image.getTitle() + " were saved in " + resultsDirectory + "/ROI/Validated/");
                    } else {
                        IJ.log("The nuclei ROIs of " + image.getTitle() + " could not be saved in " + resultsDirectory + "/ROI/Validated/");
                    }
                }
            }
            else if (resultsDirectory==null && saveRois){
                IJ.error("No directory given for the results");
            }
            if (resultsDirectory!=null && saveMask){
                File dir=new File(resultsDirectory + "/Images/Validated/");
                if(!dir.exists()) dir.mkdirs();
                ImagePlus nucleiLabeledMask = detector.labeledImage(associatedToCellNucleiRois);
                if (IJ.saveAsTiff(nucleiLabeledMask, resultsDirectory + "/Images/Validated/" + "Nuclei_" + nucleiLabeledMask.getTitle())) {
                    IJ.log("The validated nuclei mask " + nucleiLabeledMask.getTitle() + " was saved in " + resultsDirectory + "/Images/Validated/");
                } else {
                    IJ.log("The validated nuclei mask " + nucleiLabeledMask.getTitle() + " could not be saved in " + resultsDirectory + "/Images/Validated/");
                }
            }
        }else {
            IJ.log("No nuclei were associated to a cell");
        }
    }

    /**
     *
     * @param measureCalibration Calibration value and unit to use for measurements
     */
    public void setMeasureCalibration(MeasureCalibration measureCalibration){
        detector.setMeasureCalibration(measureCalibration);
    }

    /**
     * Set all parameters for projection if necessary
     * @param zStackProj Method of projection
     * @param zStackFirstSlice First slice of stack to use
     * @param zStackLastSlice Last slice of stack to use
     */
    public void setzStackParameters(String zStackProj, int zStackFirstSlice,int zStackLastSlice){
        //IJ.log("nuclei detector setZstack(String,int,int)");
        detector.setzStackParameters(zStackProj,zStackFirstSlice,zStackLastSlice);
    }

    /**
     * Set projection method and the slices to use with default values (1 and last slice)
     * @param zStackProj Method of projection
     */
    public void setzStackParameters(String zStackProj) {
        //IJ.log("nuclei detector setZstack(String)");
        setzStackParameters(zStackProj, 1, image.getNSlices());
    }

    /**
     * Set parameters for macro if necessary
     * @param macroText : macro text to use
     */
    public void setPreprocessingMacro(String macroText){
        this.useMacro = (macroText!=null && !macroText.equals(""));
        this.macroText = macroText;
    }

    public String getPreprocessingMacro() {
        return macroText;
    }

    public void setPreprocessingMacroQuantif(String macroText){
        detector.setQuantifMacro(macroText);
    }

    public String getPreprocessingMacroQuantif() {
        return detector.getQuantifMacro();
    }

    /**
     *
     * @param saveMask : boolean to save the labeled mask or not
     * @param saveROIs : boolean to save the ROI obtained after segmentation or not
     */
    public void setSavings(boolean saveMask,boolean saveROIs){
        this.saveMask = saveMask;
        this.saveRois = saveROIs;
    }

    /**
     * Set parameters for thresholding
     * @param thresholdMethod : method of thresholding
     * @param minSizeNucleus : minimum size of particle to consider
     * @param useWatershed : if true, use watershed method in addition to thresholding
     * @param excludeOnEdges : if true, exclude particles on edge of image
     */
    public void setThresholdMethod(String thresholdMethod,double minSizeNucleus,boolean useWatershed,boolean excludeOnEdges) {
        this.cellpose = false;
        this.stardist = false;
        detector.setThresholdParameters(thresholdMethod,excludeOnEdges,minSizeNucleus,true);
        this.useWatershed = useWatershed;
        this.excludeOnEdges = excludeOnEdges;
    }

    /**
     *
     * @param minSizeDLNuclei : minimum size of nucleus to detect
     * @param cellposeModel : model used by cellpose to segment
     * @param excludeOnEdges : exclude nuclei on image edges
     */
    public void setCellposeMethod(int minSizeDLNuclei, double cellproba_threshold, String cellposeModel, boolean excludeOnEdges) {
        this.cellpose = true;
        this.stardist = false;
        this.cellposeDiameter = minSizeDLNuclei;
        this.cellposeModel = cellposeModel;
        this.excludeOnEdges = excludeOnEdges;
        this.cellposeCellproba_threshold=cellproba_threshold;
    }

    public void setStarDistMethod(String model,
                                  double stardistPercentileBottom,
                                  double stardistPercentileTop,
                                  double stardistProbThresh,
                                  double stardistNmsThresh,
                                  String stardistModelFile,
                                  double stardistScale,
                                  boolean excludeOnEdges){
        this.cellpose = false;
        this.stardist = true;
        this.stardistModel=model;
        this.stardistPercentileBottom=stardistPercentileBottom;
        this.stardistPercentileTop=stardistPercentileTop;
        this.stardistProbThresh=stardistProbThresh;
        this.stardistNmsThresh=stardistNmsThresh;
        this.stardistModelFile=stardistModelFile;
        this.stardistScale=stardistScale;
        this.excludeOnEdges=excludeOnEdges;

    }

    /**
     * Set common parameters for cellpose and threshold method
     * @param finalValidation : let the user redefine the ROI found automatically
     * @param showBinaryImage : show result image if true
     */
    public void setSegmentation(boolean finalValidation, boolean showBinaryImage){
        this.finalValidation = finalValidation;
        this.showBinaryImage = showBinaryImage;
    }

    /**
     *
     * @return Array of the nuclei ROIs
     */
    public Roi[] getRoiArray() {
        return nucleiRois;
    }

    /**
     *
     * @return image
     */
    public String getImageTitle() {
        return image.getTitle();
    }

    /**
     * Preview of segmentation
     * - does the preprocessing:
     * - segmentation either by threshold or by cellpose
     * - show result labeled image
     */
    public void preview(){
//        int[] idsToKeep = WindowManager.getIDList();
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed!=null){
            preprocessed.show();
            WindowManager.setWindow(WindowManager.getWindow("Log"));
            if (cellpose){
//            launch cellpose command to obtain mask
                /*cyto channel = 0 for gray*/
                /*nuclei channel = 0 for none*/
                CellposeLauncher cellposeLauncher = new CellposeLauncher(preprocessed, cellposeDiameter, cellposeCellproba_threshold,cellposeModel, excludeOnEdges);
                cellposeLauncher.analysis();
                ImagePlus cellposeOutput = cellposeLauncher.getCellposeMask();
                cellposeOutput.show();
                cellposeOutput.setDisplayRange(0,(cellposeLauncher.getCellposeRoiManager().getCount()+10));
                cellposeOutput.updateAndDraw();
            } else if(stardist){
                StarDistLauncher starDistLauncher = new StarDistLauncher(preprocessed);
                starDistLauncher.setModel(stardistModel);
                starDistLauncher.setPercentileBottom(stardistPercentileBottom);
                starDistLauncher.setPercentileTop(stardistPercentileTop);
                starDistLauncher.setProbThresh(stardistProbThresh);
                starDistLauncher.setNmsThresh(stardistNmsThresh);
                starDistLauncher.setModelFile(stardistModelFile);
                starDistLauncher.setScale(stardistScale);
                starDistLauncher.setExcludeOnEdges(excludeOnEdges);
                starDistLauncher.analysis();
                ImagePlus stardistOuput = starDistLauncher.getInstanceMask();
                stardistOuput.show();
                stardistOuput.setDisplayRange(0,(starDistLauncher.getStardistRoiManager().getCount()+10));
                stardistOuput.updateAndDraw();


            }else {
                thresholding(preprocessed);
            }
        }
    }

    /**
     * Prepare for measurement
     * - does the preprocessing
     * - if selected by user show preprocessing image
     * - segment image
     * - if selected by user, the user can delete/modify ROIs
     * - if selected by user show segmentation image
     * - if selected by user save segmentation image and ROIs
     * @return true if no error
     */
    public boolean prepare(){
        //check saving directory
        if(saveRois){
            File tmp=new File(resultsDirectory + "/ROI/");
            if(!tmp.exists()) tmp.mkdirs();
        }
        if (saveMask){
            File tmp=new File(resultsDirectory + "/Images/");
            if(!tmp.exists()) tmp.mkdirs();
        }

//        PREPROCESSING
       // System.out.println("nuclei detector prepare macro preprocess: "+getPreprocessingMacro());
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed!=null){ /*if no error during preprocessing*/
            if (showPreprocessingImage){
                preprocessed.show();
                WindowManager.getWindow("Log").toFront();
            }
            RoiManager roiManagerNuclei;
            ImagePlus labeledImage;
            String analysisType;
//            SEGMENTATION
            if (cellpose){
                WindowManager.getWindow("Log").toFront();
                analysisType = "cellpose";
                CellposeLauncher cellposeLauncher = new CellposeLauncher(preprocessed, cellposeDiameter, cellposeCellproba_threshold,cellposeModel, excludeOnEdges);
                cellposeLauncher.analysis();
                labeledImage = cellposeLauncher.getCellposeMask();
                detector.renameImage(labeledImage,"cellpose");
                roiManagerNuclei = cellposeLauncher.getCellposeRoiManager();
            } else if(stardist){
                analysisType = "StarDist";
                StarDistLauncher starDistLauncher = new StarDistLauncher(preprocessed);
                starDistLauncher.setModel(stardistModel);
                starDistLauncher.setPercentileBottom(stardistPercentileBottom);
                starDistLauncher.setPercentileTop(stardistPercentileTop);
                starDistLauncher.setProbThresh(stardistProbThresh);
                starDistLauncher.setNmsThresh(stardistNmsThresh);
                starDistLauncher.setModelFile(stardistModelFile);
                starDistLauncher.setScale(stardistScale);
                starDistLauncher.setExcludeOnEdges(excludeOnEdges);
                starDistLauncher.analysis();
                labeledImage = starDistLauncher.getInstanceMask();
                detector.renameImage(labeledImage,"stardist");
                roiManagerNuclei = starDistLauncher.getStardistRoiManager();

            }else {
                analysisType = "threshold";
                thresholding(preprocessed);
                // Analyse particle
                roiManagerNuclei =detector.analyzeParticles(imageToParticleAnalyze);
                labeledImage = detector.labeledImage(roiManagerNuclei.getRoisAsArray());
            }
            if (showBinaryImage){
                labeledImage.show();
                labeledImage.setDisplayRange(0,roiManagerNuclei.getCount()+5);
                labeledImage.updateAndDraw();
            }
//            User can redefine ROIs if option selected
            if (finalValidation){
                roiManagerNuclei.toFront();
                ImagePlus tempImage = detector.getImageQuantification().duplicate(); /*Need to duplicate, as closing the image nullify the ImageProcessor*/
               /* if (showBinaryImage){
                    IJ.selectWindow(imageToMeasure.getID());
                }else {
                    tempImage.show();
                }*/
                tempImage.show();
                IJ.selectWindow(tempImage.getID());
                roiManagerNuclei.runCommand("Show All");
                new WaitForUserDialog("Nuclei selection", "Delete nuclei : select the ROIs + delete").show();
                if (!showBinaryImage){
                    tempImage.close();
                }
                labeledImage = detector.labeledImage(roiManagerNuclei.getRoisAsArray());
            }
//            SAVING
            if (resultsDirectory !=null && saveMask){
                detector.renameImage(labeledImage,analysisType+"_labeledMask");
                detector.setLUT(labeledImage);
                File dir=new File(resultsDirectory + "/Images/AllDetected/");
                if(!dir.exists()) dir.mkdirs();
                if(IJ.saveAsTiff(labeledImage, resultsDirectory +"/Images/AllDetected/" + "Nuclei_" +labeledImage.getTitle())){
                    IJ.log("The nuclei segmentation mask "+labeledImage.getTitle() + " was saved in "+ resultsDirectory+"/Images/AllDetected/");
                } else {
                    IJ.log("The nuclei segmentation mask "+labeledImage.getTitle() + " could not be saved in "+ resultsDirectory+"/Images/AllDetected/");
                }
            }else if (resultsDirectory==null && saveMask){
                IJ.error("No directory given for the results");
            }

            if (resultsDirectory!=null && saveRois) {
                for (int i = 0; i < roiManagerNuclei.getCount(); i++) {
                    roiManagerNuclei.rename(i,"Nucleus_"+(i+1));
                }
                if(roiManagerNuclei.getCount()>0) {
                    String extension=(roiManagerNuclei.getCount()==1)?".roi":".zip";
                    File dir=new File(resultsDirectory + "/ROI/AllDetected/");
                    if(!dir.exists()) dir.mkdirs();
                    if (roiManagerNuclei.save(resultsDirectory + "/ROI/AllDetected/" + image.getTitle() + "_" + analysisType + "_NucleiDetectedROIs"+extension)) {
                        IJ.log("The nuclei ROIs of " + image.getTitle() + " were saved in " + resultsDirectory + "/ROI/AllDetected/");
                    } else {
                        IJ.log("The nuclei ROIs of " + image.getTitle() + " could not be saved in " + resultsDirectory + "/ROI/AllDetected/");
                    }
                }
            }
            else if (resultsDirectory==null && saveRois){
                IJ.error("No directory given for the results");
            }
            if(imageToMeasure==null) imageToMeasure=detector.getImageQuantification();
            analyzer = new Analyzer(imageToMeasure, measurements, rawMeasures); /*set measurements and image to analyze*/
            nucleiRois = roiManagerNuclei.getRoisAsArray();
            if(resultsDirectory!=null && expand4Cells && saveRois){
                getExpandedRois();
                RoiManager tmpCell=new RoiManager(true);
                tmpCell.reset();
                Roi[] cells=roisExpanded.get(1);
                for(Roi r:cells) tmpCell.addRoi(r);
                RoiManager tmpCyto=new RoiManager(true);
                tmpCyto.reset();
                Roi[] cytos=roisExpanded.get(2);
                for(Roi r:cytos) tmpCyto.addRoi(r);

                String extension=(roiManagerNuclei.getCount()==1)?".roi":".zip";
                File dir=new File(resultsDirectory + "/ROI/AllDetected/");
                if(!dir.exists()) dir.mkdirs();
                if (tmpCell.save(resultsDirectory + "/ROI/AllDetected/" + image.getTitle() + "_" + analysisType + "_NucleiDetectedROIs_ExpandedCell"+extension)) {
                    IJ.log("The nuclei ROIs of " + image.getTitle() + " were saved in " + resultsDirectory + "/ROI/AllDetected/");
                } else {
                    IJ.log("The nuclei ROIs of " + image.getTitle() + " could not be saved in " + resultsDirectory + "/ROI/AllDetected/");
                }
                if (tmpCyto.save(resultsDirectory + "/ROI/AllDetected/" + image.getTitle() + "_" + analysisType + "_NucleiDetectedROIs_ExpandedCyto"+extension)) {
                    IJ.log("The nuclei ROIs of " + image.getTitle() + " were saved in " + resultsDirectory + "/ROI/AllDetected/");
                } else {
                    IJ.log("The nuclei ROIs of " + image.getTitle() + " could not be saved in " + resultsDirectory + "/ROI/AllDetected/");
                }
            }

            //imageToMeasure=null;
            return true;
        }else return false;
    }


    /**
     * If useThreshold, prepare threshold image
     * @param imagePlus : image to segment
     */
    public void thresholding(ImagePlus imagePlus){
//        OBTAIN BINARY MASK OF THRESHOLD IMAGE
        ImagePlus mask_IP = detector.getThresholdMask(imagePlus);

        if (showBinaryImage){
            mask_IP.show();
        }

        if (useWatershed){
            ImagePlus watershed_mask = detector.getWatershed(mask_IP.getProcessor());
            detector.renameImage(watershed_mask,"watershed");
            if (showBinaryImage){
                watershed_mask.show();
            }
            imageToParticleAnalyze = watershed_mask;
        }else {
            imageToParticleAnalyze = mask_IP;
        }
    }



    /**
     * Preprocess image for better segmentation
     * - Projection
     * - Macro
     * @return ImagePlus
     */
    protected ImagePlus getPreprocessing() {
        if (detector.getImage()!=null){
            IJ.run("Options...","iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
            //System.out.println("nuclei detector getpreprocessing macro: "+getPreprocessingMacro());
            imageToMeasure = detector.getImageQuantification();
            ImagePlus imageToReturn = detector.getImage(); /*detector class does the projection if needed*/
            ImagePlus temp;
//      MACRO : apply custom commands of user
            if (useMacro){
                imageToReturn.show();
                IJ.log("macro: nslices:"+imageToReturn.getNSlices());
                IJ.selectWindow(imageToReturn.getID());
                IJ.runMacro("setBatchMode(true);"+macroText+"setBatchMode(false);"); /*accelerates the treatment by displaying only the last image*/
                temp = WindowManager.getCurrentImage();
                imageToReturn.close();
                imageToReturn = temp.duplicate();
                temp.changes=false;
                temp.close();
            }
            return imageToReturn;
        }else return null;
    }

    public void cleanNameExperiment(){
        nameExperiment=nameExperiment.replaceAll(":","_");
    }

    /**
     *
     * @return name without channel specific information
     */
    public String getNameExperiment() {
        return nameExperiment;
    }

    public String getNameChannel() {
        return nameChannel;
    }

    public void setNameChannel(String nameChannel) {
        this.nameChannel = nameChannel;
    }

    /**
     *
     * @param measurements integer corresponding to addition of measurements to do for nuclei
     */
    public void setMeasurements(int measurements) {
        this.measurements = measurements;
    }

    public int getMeasurements() {
        return measurements;
    }

    public ImagePlus getImageToMeasure() {
        if(imageToMeasure==null) {
            imageToMeasure=detector.getImageQuantification();
        }
        return imageToMeasure;
    }

    public boolean isExpand4Cells() {
        return expand4Cells;
    }

    public void setExpand4Cells(boolean expand4Cells) {
        this.expand4Cells = expand4Cells;
        roisExpanded=null;
    }

    public int getExpandRadius() {
        return expandRadius;
    }

    public void setExpandRadius(int expandRadius) {
        this.expandRadius = expandRadius;
        if(expandRadius>0) setExpand4Cells(true);
        else setExpand4Cells(false);
        roisExpanded=null;
    }

    /**
     * get the differents rois corresponding to nuclei, expanded nuclei (cell) and cytoplasm (xor or the previous 2)
     * @return
     */
    public ArrayList<Roi[]> getExpandedRois(){
        if(expand4Cells && roisExpanded!=null) {
            //IJ.log("return expanded roi already computed");
            return roisExpanded;
        }
        IJ.log("compute expanded nuclei");
        roisExpanded=new ArrayList<>(3);
        roisExpanded.add(nucleiRois);
        if(!expand4Cells) return roisExpanded;
        // expand nuclei
        ImagePlus mask= Detector.labeledImage(image.getWidth(),image.getHeight(),nucleiRois);
        //mask.show();
        ImageProcessor expanded= ExpandMask.expandsMask(mask.getProcessor(),expandRadius);
        ImagePlus expandedIP= new ImagePlus("expanded",expanded);
        //expandedIP.show();
        RoiManager.getRoiManager().reset();
        RoiManager cells = label2Roi(expandedIP,0,0,0, "Cell ");
        Roi[] cellRois=cells.getRoisAsArray();
        roisExpanded.add(cellRois);
        //xor
        //ImagePlus cytoIP=expandedIP.duplicate();
        //cytoIP.setTitle("cyto mask");
        //cytoIP.getProcessor().copyBits(mask.getProcessor(), 0,0, Blitter.SUBTRACT);
        RoiManager.getRoiManager().reset();
        //RoiManager cyto = label2Roi(cytoIP,0,0,0, "Cyto ");
        Roi[] cytoRois = new Roi[cellRois.length];
        for(int r=0;r<cellRois.length;r++){
            ShapeRoi roiCyto=new ShapeRoi(cellRois[r]);
            roiCyto.xor(new ShapeRoi(nucleiRois[r]));
            roiCyto.setName("Cyto "+(r+1));
            cytoRois[r]=roiCyto;
            RoiManager.getRoiManager().addRoi(roiCyto);
        }
        //cytoIP.show();
        roisExpanded.add(cytoRois);
        return roisExpanded;
    }

    /***
     * convert label image into Rois
     * @param cellposeIP
     * @param xoffset
     * @param yoffset
     * @return
     */
    public static RoiManager label2Roi(ImagePlus cellposeIP, int xoffset, int yoffset,int minSize, String prefix) {
        if (cellposeIP == null) System.out.println("error cellposeIP is null!");
        ImageProcessor cellposeProc = cellposeIP.getProcessor().duplicate();
        //cellposeIP.duplicate().show();
        Wand wand = new Wand(cellposeProc);

//        Set RoiManager
        RoiManager cellposeRoiManager = RoiManager.getRoiManager();
        //cellposeRoiManager.reset();


        /*
         * Will iterate through pixels, when getPixel > 0 ,
         * then use the magic wand to create a roi
         * finally set value to 0 and add to the roiManager
         */

        // will "erase" found ROI by setting them to 0
        cellposeProc.setColor(0);
        Roi tmp=new Roi(0,0,1,1);
        tmp.setGroup(10);
        ImageStatistics statistics=cellposeProc.getStatistics();
        for(int i=0;i<statistics.max;i++) {
            tmp.setName(prefix+(i+1));
            cellposeRoiManager.addRoi(tmp);
        }

        for (int y_coord=0;y_coord<cellposeProc.getHeight();y_coord++) {
            for (int x_coord =0; x_coord<cellposeProc.getWidth();x_coord++) {
                int val=Math.round(cellposeProc.getPixelValue(x_coord, y_coord));
                if (val > 0.0) {
                    // use the magic wand at this coordinate
                    wand.autoOutline(x_coord, y_coord);

                    // if there is a region , then it has npoints
//                    There can be problems with very little ROIs, so threshold of 20 points
                    if (wand.npoints > minSize) {
                        // get the Polygon, fill with 0 and add to the manager
                        ShapeRoi roi = new ShapeRoi(new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI));
                        // ip.fill should use roi, otherwise make a rectangle that erases surrounding pixels
                        cellposeProc.fill(roi);
                        Rectangle r = roi.getBounds();
                        roi.setLocation(xoffset + r.x, yoffset + r.y);
                        if(cellposeRoiManager.getRoi(val-1).getGroup()==10) {
                            roi.setName(prefix+val);
                            cellposeRoiManager.setRoi(roi, val - 1);

                        }else{
                            Roi previous = cellposeRoiManager.getRoi(val-1);
                            ShapeRoi prev=new ShapeRoi(previous);
                            prev.or(roi);
                            prev.setName(prefix+val);
                            cellposeRoiManager.setRoi(prev,val-1);
                        }
                    }
                }
            }
        }
        return cellposeRoiManager;
    }
}
