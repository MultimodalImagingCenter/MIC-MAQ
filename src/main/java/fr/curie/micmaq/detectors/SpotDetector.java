package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.RoiEncoder;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.*;
import java.io.File;

/**
 * Author : Camille RABIER
 * Date : 07/06/2022
 * Class for
 * - analyzing the spot images
 * It uses the ROIs obtained from NucleiDetector to analyze per nucleus
 * It analyzes either by threshold+particle analyzer or by find Maxima method
 */
public class SpotDetector {
//    Images
    private final ImagePlus image; /*Image without modifications*/
    private ImagePlus imageToMeasure; /*Image that will be measured : only with projection*/

//    Infos for results
    private final String nameExperiment; /*Part of image name that differentiates them from*/
    private final String spotName;
    private MeasureCalibration measureCalibration;

//    Saving results infos
    private final String resultsDirectory;
    private boolean saveImage;
    private boolean saveRois;


    //    Showing images
    private boolean showMaximaImage;
    private boolean showThresholdImage;
    private final boolean showPreprocessedImage;


    //    Preprocessing
    //    --> subtract background
    private boolean useRollingBallSize;
    private double rollingBallSize;

//    --> macro
    private String macroText;

    //Detection of spots
    //    --> find maxima
    private ImagePlus findMaximaIP;
    private ImagePlus findMaximaMask;
    private boolean spotByFindMaxima;
    private double prominence;

//    --> threshold
    private boolean spotByThreshold;
    private ImagePlus thresholdIP;
    private boolean useWatershed;
    private final Detector detector;

    private int measurements=Measurements.MEAN + Measurements.INTEGRATED_DENSITY;

    /**
     *
     * @param image : image corresponding to spots
     * @param spotName : name of protein analyzed
     * @param nameExperiment : name of image without channel specific information
     * @param resultsDirectory : directory to save results
//     * @param showImage
     */
    public SpotDetector(ImagePlus image, String spotName, String nameExperiment, String resultsDirectory, boolean showPreprocessedImage) {
        detector = new Detector(image, spotName);
        this.resultsDirectory = resultsDirectory;
        this.showPreprocessedImage = showPreprocessedImage;
        this.image = image;
        this.spotName = spotName;
        if (nameExperiment.endsWith("_")) {
            this.nameExperiment = nameExperiment.substring(0, nameExperiment.length() - 1);
        } else {
            this.nameExperiment = nameExperiment;
        }
        this.rollingBallSize = 0;
        this.useRollingBallSize = false;
        this.spotByThreshold = false;
        this.spotByFindMaxima = false;
//        this.regionROI = null;

    }

    /**
     *
     * @param measureCalibration Calibration value and unit to use for measurements
     */
    public void setMeasureCalibration(MeasureCalibration measureCalibration){
        detector.setMeasureCalibration(measureCalibration);
        this.measureCalibration = measureCalibration;
    }

    /**
     * Set all parameters for projection if necessary
     * @param zStackProj Method of projection
     * @param zStackFirstSlice First slice of stack to use
     * @param zStackLastSlice Last slice of stack to use
     */
    public void setzStackParameters(String zStackProj, int zStackFirstSlice, int zStackLastSlice) {
        detector.setzStackParameters(zStackProj, zStackFirstSlice, zStackLastSlice);
    }

    /**
     * Set projection method and the slices to use with default values (1 and last slice)
     * @param zStackProj Method of projection
     */
    public void setzStackParameters(String zStackProj) {
        setzStackParameters(zStackProj, 1, image.getNSlices());
    }

    /**
     * Set parameters for unifying the background
     * @param rollingBallSize : size of ball that should be of the size of the biggest object the user wants to analyze
     */
    public void setRollingBallSize(double rollingBallSize){
        this.useRollingBallSize = true;
        this.rollingBallSize = rollingBallSize;
    }

    /**
     * Set parameters for search of local intensity maxima
     * @param prominence : minimal difference of intensity with neighbors needed to be considered a local maxima
     * @param showMaximaImage : choice to show resulting mask
     */
    public void setSpotByFindMaxima(double prominence, boolean showMaximaImage) {
        this.spotByFindMaxima = true;
        this.prominence = prominence;
        this.showMaximaImage = showMaximaImage;
    }

    /**
     * Set parameters for thresholding and
     * @param thresholdMethod : method of thresholding
     * @param minSizeSpot : minimum size of particle to consider
     * @param useWatershed : if true, use watershed method in addition to thresholding
     * @param showThresholdImage : chooice to show the resulting image
     */
    public void setSpotByThreshold(String thresholdMethod, double minSizeSpot, boolean useWatershed, boolean showThresholdImage) {
        this.spotByThreshold = true;
        this.showThresholdImage = showThresholdImage;
        this.useWatershed = useWatershed;
        //macroText = null;
        detector.setThresholdParameters(thresholdMethod,false,minSizeSpot); /*does not exclude spot on edges*/
    }

    public void setSpotByThreshold(String thresholdMethod,double minTreshold,double maxThreshold, double minSizeSpot, boolean useWatershed, boolean showThresholdImage) {
        this.spotByThreshold = true;
        this.showThresholdImage = showThresholdImage;
        this.useWatershed = useWatershed;
        //macroText = null;
        detector.setThresholdParameters(thresholdMethod,minTreshold, maxThreshold,false,minSizeSpot); /*does not exclude spot on edges*/
    }


    /**
     * Set saving's choice
     * @param saveImage : save find maxima or threshold image
     * @param saveRois : save corresponding regions
     */
    public void setSaving(boolean saveImage, boolean saveRois){
        this.saveImage =saveImage;
        this.saveRois =saveRois;
    }

    /**
     * Set parameters for macro if necessary
     * @param macroText : macro text to use
     */
    public void setPreprocessingMacro(String macroText){
        this.macroText = macroText;
    }

    public String getPreprocessingMacro() {
        return macroText;
    }

    public void setPreprocessingMacroQuantif(String macroText){
        detector.setQuantifMacro(macroText);
        //System.out.println("spot detector macro quantif: "+detector.getQuantifMacro());
    }

    public String getPreprocessingMacroQuantif() {
        return detector.getQuantifMacro();
    }

    /**
     *
     * @return name of image without channel specific information
     */
    public String getNameExperiment() {
        return nameExperiment;
    }

    public String getSpotName(){
        return spotName;
    }

    public int getMeasurements() {
        return measurements;
    }

    public void setMeasurements(int measurements) {
        this.measurements = measurements;
    }

    /**
     *
     * @return name of image
     */
    public String getImageTitle() {
        return image.getTitle();
    }

    /**
     *
     */
    public void preview() {
//        PREPROCESSING : PROJECTION, PRE-TREATMENT (subtract background, macro)....
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        if (preprocessed!=null){
            if (showPreprocessedImage){
                preprocessed.show();
            }
            if (spotByThreshold) {
                thresholdIP = detector.getThresholdMask(preprocessed);
                if (showThresholdImage){
                    thresholdIP.show();
                }
                if (useWatershed)   detector.getWatershed(thresholdIP.getProcessor()).show();
            }
            if (spotByFindMaxima) {
                findMaximaIP = preprocessed.duplicate();
                detector.renameImage(findMaximaIP, "maxima");
                /*Find maxima*/
                findMaxima(findMaximaIP,prominence,"preview");
            }
        }
    }
    /**
     * Prepare for measurement
     * - does the preprocessing
     * - if selected by user show preprocessing image
     * - detect spot in the entire image (by threshold and/or find maxima)
     * - if selected by user : show masks with spot detected by chosen method(s)
     * @return true if no error
     */
    public boolean prepare(){
        //check saving directory
        if(saveRois){
            File tmp=new File(resultsDirectory + "/Results/Spot"+spotName+"/ROI/");
            if(!tmp.exists()) tmp.mkdirs();
            if(spotByThreshold){
                tmp=new File(resultsDirectory + "/Results/Spot"+spotName+"/ROI/thresholding/");
                if(!tmp.exists()) tmp.mkdirs();
            }
            if(spotByFindMaxima){
                tmp=new File(resultsDirectory + "/Results/Spot"+spotName+"/ROI/findmaxima/");
                if(!tmp.exists()) tmp.mkdirs();
            }
        }
        if (saveImage){
            File tmp=new File(resultsDirectory + "/Results/Spot"+spotName+"/Images/");
            if(!tmp.exists()) tmp.mkdirs();
            if(spotByThreshold){
                tmp=new File(resultsDirectory + "/Results/Spot"+spotName+"/Images/thresholding");
                if(!tmp.exists()) tmp.mkdirs();
            }
            if(spotByFindMaxima){
                tmp=new File(resultsDirectory + "/Results/Spot"+spotName+"/Images/findmaxima");
                if(!tmp.exists()) tmp.mkdirs();
            }
        }
//        Preprocessing
        ImagePlus preprocessed = preprocessing();
        if (preprocessed!=null){
            if (showPreprocessedImage){
                preprocessed.show();
            }
//            Detection by threshold
            if (spotByThreshold) {
                thresholdIP = detector.getThresholdMask(preprocessed);
                if (useWatershed){
                    thresholdIP = detector.getWatershed(thresholdIP.getProcessor());
                    detector.renameImage(thresholdIP,"watershed");
                }
                if (showThresholdImage){
                    thresholdIP.show();
                }
                if (resultsDirectory != null && saveImage){
                    if (IJ.saveAsTiff(thresholdIP, resultsDirectory +"/Results/Spot"+spotName+"/Images/thresholding/"+thresholdIP.getTitle())){
                        IJ.log("The spot binary mask "+thresholdIP.getTitle() + " was saved in "+ resultsDirectory+"/Results/Spot"+spotName+"/Images/thresholding/");
                    }else {
                        IJ.log("The spot binary mask "+thresholdIP.getTitle() + " could not be saved in  "+ resultsDirectory+"/Results/Spot"+spotName+"/Images/thresholding/");
                    }
                }
            }
//            Detection by find maxima
            if (spotByFindMaxima) {
                findMaximaIP = preprocessed.duplicate();
                detector.renameImage(findMaximaIP, "maxima");
                createFindMaximaMask(findMaximaIP);
                if (resultsDirectory != null) {
                    if (saveRois){
                        findMaximaIP.setRoi((Roi)null);
                        PointRoi roiMaxima = findMaxima(findMaximaIP, prominence, "full");
                        findMaximaIP.setRoi(roiMaxima);
                        boolean wasSaved = RoiEncoder.save(roiMaxima, resultsDirectory +"/Results/Spot"+spotName+"/ROI/findmaxima/" + image.getTitle() + "findMaxima_all_roi.roi");
                        if (!wasSaved) {
                            IJ.error("Could not save ROIs");
                        }else {
                            IJ.log("The ROIs of the spot found by find Maxima method of "+image.getTitle() + " were saved in "+ resultsDirectory+"/Results/Spot"+spotName+"/ROI/" + image.getTitle() + "findMaxima_all_roi.roi");
                        }
                    }
                    if (saveImage){
                        ImagePlus toSave = findMaximaIP.flatten();
                        if(IJ.saveAsTiff(toSave, resultsDirectory +"/Results/Spot"+spotName+"/Images/findmaxima/"+findMaximaIP.getTitle())){
                            IJ.log("The find maxima spots image "+findMaximaIP.getTitle() + " was saved in "+ resultsDirectory+"/Results/Spot"+spotName+"/Images/findmaxima/"+findMaximaIP.getTitle());
                        } else {
                            IJ.log("The find maxima spots image "+findMaximaIP.getTitle() + " could not be saved in "+ resultsDirectory+"/Results/Spot"+spotName+"/Images/findmaxima/");
                        }
                    }
                }
            }
            return true;
        }else return false;
    }

    /**
     * Detect spot in region wanted : nucleus, cell or cytoplasm
     * @param regionID : id of objet (number in list of ROI) for saving of threshold roi
     * @param regionROI : ROI where the spot have to be detected
     * @param resultsTableFinal : results table to fill
     * @param type : image, cell, nucleus or cytoplasm
     */
    public void analysisPerRegion(int regionID,Roi regionROI, ResultsTable resultsTableFinal, String type) {
        imageToMeasure.setRoi(regionROI);
        ResultsTable rawMeasures = new ResultsTable();
//        Measures of mean and raw intensities in the whole ROI are always done for spot images
        Analyzer analyzer = new Analyzer(imageToMeasure, measurements, rawMeasures);
        analyzer.measure();
        detector.setResultsAndRename(rawMeasures,resultsTableFinal,0,type + "_"+ spotName); /*always first line, because analyzer replace line*/
//        Detection and measurements
        if (spotByFindMaxima) {
            findMaximaPerRegion(regionROI, resultsTableFinal,type);
        }
        if (spotByThreshold) {
            findThresholdPerRegion(regionID,regionROI, resultsTableFinal,type);
        }
    }

    /**
     * Detect and measure spot in specific region by threshold
     * @param regionID : id of objet (number in list of ROI) for saving of threshold roi
     * @param regionROI : ROI where the spot have to be detected
     * @param resultsTableToAdd : results table to fill
     * @param type : image, cell, nucleus or cytoplasm
     */
    private void findThresholdPerRegion(int regionID,Roi regionROI, ResultsTable resultsTableToAdd, String type) {
        RoiManager roiManagerFoci=null;
        int numberSpot = 0; /*count number of spot detected*/
//        Detection
        if (regionROI!=null){
            thresholdIP.setRoi(regionROI);
            //thresholdIP.getProcessor().invertLut();
            roiManagerFoci = detector.analyzeParticles(thresholdIP);
            numberSpot = roiManagerFoci.getCount();
//            --> Saving
            if (resultsDirectory!=null&&saveRois && numberSpot>0){
                if (roiManagerFoci.save(resultsDirectory +"/Results/Spot"+spotName+"/ROI/thresholding/"+ image.getTitle() + "_threshold_"+type +(regionID+1)+"_roi.zip")){
                    IJ.log("The ROIs of the "+type + " "+(regionID+1)+" of the image "+image.getTitle() + " by threshold method were saved in "+ resultsDirectory+"/Results/Spot"+spotName+"/ROIs/thresholding/");
                }else {
                    IJ.log("The ROIs of the "+type + " "+ (regionID+1)+" of the image "+image.getTitle() + " by threshold method could not be saved in "+ resultsDirectory+"/Results/Spot"+spotName+"/ROIs/thresholding/");
                }
            }
        }
//        Measurement
        resultsTableToAdd.addValue(type+"_"+spotName+" threshold nr. spot",numberSpot);
        if (numberSpot > 0) {
            ResultsTable resultsTable = new ResultsTable();
            Analyzer analyzer = new Analyzer(imageToMeasure, Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY, resultsTable);
            for (int spot = 0; spot < numberSpot; spot++) {
                roiManagerFoci.select(imageToMeasure, spot);
                analyzer.measure();
            }
            detector.setSummarizedResults(resultsTable,resultsTableToAdd,type + "_"+ spotName);
        } else {
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold Area (pixel)", Double.NaN);
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold Area (" + measureCalibration.getUnit() + ")", Double.NaN);
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold Mean", Double.NaN);
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold RawIntDen", Double.NaN);
        }
    }

    /**
     * Detect and measure spots by searching local maxima
     * @param regionROI : ROI where the spot have to be detected
     * @param resultsTableToAdd : results table to fill
     * @param type : image, cell, nucleus or cytoplasm
     */
    private void findMaximaPerRegion(Roi regionROI, ResultsTable resultsTableToAdd, String type) {
        ImagePlus tmp= (findMaximaMask!=null) ? findMaximaMask:findMaximaIP;
        double prom=(findMaximaMask!=null)? 10:prominence;
        tmp.setRoi(regionROI);
//                    Find maxima

        PointRoi roiMaxima = findMaxima(tmp, prom,type);
//                    Get statistics
        int size = 0;
        float mean = 0;
        for (Point p : roiMaxima) {
            size++;
            mean += tmp.getProcessor().getPixelValue(p.x, p.y);
        }
        mean = mean / size;
        resultsTableToAdd.addValue(type + "_"+ spotName + " maxima prominence", prominence);
        resultsTableToAdd.addValue(type + "_"+ spotName + " maxima nr. spots", size);
        resultsTableToAdd.addValue(type + "_"+ spotName + " maxima mean", mean);
    }

    /**
     * Preprocess image for better segmentation
     * - Projection
     * - Macro
     * - Subtract background
     * @return ImagePlus
     */
    private ImagePlus preprocessing() {
        if(detector.getImage()!=null){
            IJ.run("Options...", "iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
            imageToMeasure=detector.getImageQuantification();
            ImagePlus imageToReturn = detector.getImage().duplicate();
            ImagePlus temp;
//      MACRO : apply custom commands of user
            if (macroText!=null){
                IJ.log("SpotDetector macro:"+macroText);
                imageToReturn.show();
                IJ.selectWindow(imageToReturn.getID());
                IJ.runMacro("setBatchMode(true);"+macroText+"setBatchMode(false);");
                temp = WindowManager.getCurrentImage();
                imageToReturn = temp.duplicate();
                temp.changes=false;
                temp.close();
            }
//            SUBTRACT BACKGROUND : correct background with rolling ball algorithm
            if (useRollingBallSize){
                imageToReturn = getSubtractBackground();
            }
            return imageToReturn;
        }else return null;
    }

    /**
     * use rolling ball algorithm to correct background
     * @return image with corrected background
     */
    private ImagePlus getSubtractBackground() {
        ImagePlus wobkgIP = imageToMeasure.duplicate();
        detector.renameImage(wobkgIP, "withoutBackground");
        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        backgroundSubtracter.rollingBallBackground(wobkgIP.getProcessor(), rollingBallSize, false, false, false, true, false);
        return wobkgIP;
    }

    private ImagePlus createFindMaximaMask(ImagePlus findMaximaIP){
        MaximumFinder maximumFinder = new MaximumFinder();
        ImageProcessor findMaximaProc = findMaximaIP.getProcessor().duplicate();
        Polygon maxima = maximumFinder.getMaxima(findMaximaProc, prominence, true);
        PointRoi roiMaxima = new PointRoi(maxima);
        findMaximaIP.setRoi(roiMaxima);
        findMaximaMask=new ImagePlus("find maxima mask",findMaximaIP.createRoiMask());
        //findMaximaMask.show();
        return findMaximaMask;
    }

    /**
     * The algorithm scan the image to find all values all the pixel of
     * @param findMaximaIP : image to use for finding local maxima
     * @return : pointRoi corresponding to all local maxima
     */
    private PointRoi findMaxima(ImagePlus findMaximaIP, double prominence,String type) {
        MaximumFinder maximumFinder = new MaximumFinder();
        ImageProcessor findMaximaProc = findMaximaIP.getProcessor().duplicate();
        //ImageProcessor debug= findMaximaProc.duplicate();
        //new ImagePlus("debug",debug).show();
        //ImageProcessor debug2 = debug.duplicate();
        Roi roi=findMaximaIP.getRoi();
        if(roi!=null) {
            if (roi instanceof PolygonRoi || roi instanceof ShapeRoi) {
                roi = roi.getInverse(findMaximaIP);
                findMaximaProc.setValue(0);
                //System.out.println("debug fill");
                findMaximaProc.fill(roi);
                //new ImagePlus("debug2_" + type, findMaximaProc).show();
            } else {
                System.out.println("not a polygonRoi " + roi.getClass());
            }
            Polygon maxima = maximumFinder.getMaxima(findMaximaProc, prominence, true);
            PointRoi roiMaxima = new PointRoi(maxima);
            findMaximaIP.setRoi(roiMaxima);
            if (showMaximaImage) {
                findMaximaIP.flatten();
                findMaximaIP.show();
            }
            return roiMaxima;
        } else if (type.equals("full")) {
            Polygon maxima = maximumFinder.getMaxima(findMaximaProc, prominence, true);
            PointRoi roiMaxima = new PointRoi(maxima);
            findMaximaIP.setRoi(roiMaxima);
            if (showMaximaImage) {
                findMaximaIP.flatten();
                findMaximaIP.show();
            }
            return roiMaxima;
        }else{
            return new PointRoi();
        }
        //debug2.setRoi((PolygonRoi)findMaximaIP.getRoi());
        //System.out.println("debug setMask");
        //debug2.setMask(((PolygonRoi)findMaximaIP.getRoi()).getMask());



    }
}
