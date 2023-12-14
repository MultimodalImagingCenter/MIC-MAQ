package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

import java.io.File;

/**
 * Author : Camille RABIER
 * Date : 25/03/2022
 * Class for
 * - segmenting and measuring cell
 */
public class CellDetector {
    private final ImagePlus image; /*Original image*/
    private final ResultsTable rawMeasures;
    private Analyzer analyzer;
    private ImagePlus imageToMeasure; /*Projected or not image, that will be the one measured*/

    //General parameters
    private  String nameExperiment; /*Name of experiment (common with spot/nuclei images)*/
    private final Detector detector; /*Helper class that do the projection, thresholding and particle analyzer that are common with the spots*/
    private final String resultsDirectory; /*Directory to save results if necessary*/
    private String macroText; /*Macro text if the user wants to treat image*/
    private Roi[] cellRois;
    //    Showing/Saving
    private final boolean showPreprocessedImage; /*Display or not the images (projection and binary)*/
    private final boolean showCompositeImage;
    private boolean saveBinary;
    private boolean saveRois;
    private boolean showBinaryImage;

    //    Parameters for cellpose
    private int minSizeCell; /*minimum size of the cell*/
    private double cellposeCellproba_threshold; /* threshold for cell probability in cellpose*/
    private String cellposeModel; /*model to be used by cellpose*/
    private boolean excludeOnEdges; /*exclude the cells on the edge*/

    //    Cytoplasm parameters
    private NucleiDetector nucleiDetector; /*Object associated to nuclei images */
    private double minNucleiCellOverlap;
    private double minCytoSize;
    private int measurements;
    private boolean finalValidation;

    private String nameChannel="";

// CONSTRUCTOR
    /**
     * Constructor with basic parameters, the other are initialized only if needed
     *
     * @param image              image to analyze
     * @param nameExperiment    name without channel
     * @param resultsDir         : directory for saving results
     * @param showPreprocessedImage          : display or not of the images
     */
    public CellDetector(ImagePlus image, String nameExperiment, String resultsDir, boolean showPreprocessedImage,boolean showBinaryImage, boolean showCompositeImage) {
        this.image = image;
        this.showPreprocessedImage = showPreprocessedImage;
        this.showBinaryImage = showBinaryImage;
        this.showCompositeImage = showCompositeImage;
        if (nameExperiment.endsWith("_")) {
            this.nameExperiment = nameExperiment.substring(0, nameExperiment.length() - 1);
        } else {
            this.nameExperiment = nameExperiment;
        }
        //IJ.log("name experiment "+nameExperiment.replaceAll("[\\\\/:,;*?\"<>|]","_"));
        this.resultsDirectory =resultsDir+"/Results/"+nameExperiment.replaceAll("[\\\\/:,;*?\"<>|]","_").replaceAll(" ","");
        File dir=new File(resultsDirectory);
        if(!dir.exists()) dir.mkdirs();
        detector = new Detector(image, "Cell");
        nucleiDetector = null;
        rawMeasures = new ResultsTable();
    }


//    SETTER
    /**
     *
     * @param measureCalibration Calibration value and unit to use for measurements
     */
    public void setMeasureCalibration(MeasureCalibration measureCalibration){
        detector.setMeasureCalibration(measureCalibration);
    }
    /**
     * @param nucleiDetector : Nuclei object that improve the cell detection with cellpose
     */
    public void setNucleiDetector(NucleiDetector nucleiDetector) {
        this.nucleiDetector = nucleiDetector;
    }
    /**
     * Set all parameters for projection if necessary
     *
     * @param zStackProj       Method of projection
     * @param zStackFirstSlice First slice of stack to use
     * @param zStackLastSlice  Last slice of stack to use
     */
    public void setZStackParameters(String zStackProj, int zStackFirstSlice, int zStackLastSlice) {
        detector.setzStackParameters(zStackProj, zStackFirstSlice, zStackLastSlice);
    }

    /**
     * Set projection method and the slices to use with default values (1 and last slice)
     *
     * @param zStackProj Method of projection
     */
    public void setZStackParameters(String zStackProj) {
        setZStackParameters(zStackProj, 1, image.getNSlices());
    }

    /**
     * Set parameters for macro if necessary
     * @param macroText : macro text to use
     */
    public void setPreprocessingMacro(String macroText) {
        this.macroText = macroText;
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
        this.saveBinary = saveMask;
        this.saveRois = saveROIs;
    }

    /**
     * @param minSizeDLCell : minimum size of cell to detect
     * @param cellposeModel : model used by cellpose to segment
     * @param excludeOnEdges : exclude cell on image edges
     */
    public void setDeepLearning(int minSizeDLCell, double cellproba_threshold, String cellposeModel, boolean excludeOnEdges, boolean finalValidation, boolean showBinaryImage) {
        this.minSizeCell = minSizeDLCell;
        this.cellposeCellproba_threshold=cellproba_threshold;
        this.cellposeModel = cellposeModel;
        this.excludeOnEdges = excludeOnEdges;
        this.finalValidation = finalValidation;
        this.showBinaryImage = showBinaryImage;
    }
    public void setCytoplasmParameters(double minOverlap, double minCytoSize){
        this.minNucleiCellOverlap = minOverlap/100;
        this.minCytoSize = minCytoSize/100;
    }

    /**
     * The cell without nuclei are considered as errors, so they are not analyzed
     * --> saves cellRoi
     * --> save new binary mask associated to new Rois
     * @param modifiedCellRois : CellRois that are associated to a nuclei
     */
    public void setCellRois(Roi[]modifiedCellRois){
        cellRois=modifiedCellRois;
        if (cellRois.length>0){
//            Save Rois
            if (resultsDirectory!=null && saveRois) {
                RoiManager roiManager = RoiManager.getInstance();
                if (roiManager == null) { /*if no instance of roiManager, creates one*/
                    roiManager = new RoiManager();
                } else { /*if already exists, empties it*/
                    roiManager.reset();
                }
                /*Add to roiManager*/
                for (int i = 0; i < modifiedCellRois.length; i++) {
                    Roi roi = modifiedCellRois[i];
                    if (roi!=null){
                        roi.setName("Cell_"+(i+1));
                        roiManager.addRoi(roi);
                    }
                }
                /*Save RoiManager*/
                if(roiManager.getCount()>0) {
                    String extension=(roiManager.getCount()==1)?".roi":".zip";
                    File dir=new File(resultsDirectory + "/ROI/Validated/");
                    if(!dir.exists()) dir.mkdirs();
                    if (roiManager.save(resultsDirectory + "/ROI/Validated/" + image.getTitle() + "_CellsWithNucleusROIs"+extension)) {
                        IJ.log("The cell ROIs containing a nucleus of " + image.getTitle() + " were saved in " + resultsDirectory + "/ROI/Validated/");
                    } else {
                        IJ.log("The cell ROIs containing a nucleus of " + image.getTitle() + " could not be saved in " + resultsDirectory + "/ROI/Validated/");
                    }
                }
            }
            else if (resultsDirectory==null && saveRois){
                IJ.error("No directory given for the results");
            }
//            Save binary mask
            if (resultsDirectory!=null && saveBinary){
                ImagePlus cellLabeledMask = detector.labeledImage(modifiedCellRois);
                File dir=new File(resultsDirectory + "/Images/");
                if(!dir.exists()) dir.mkdirs();
                if (IJ.saveAsTiff(cellLabeledMask, resultsDirectory + "/Images/" + "Validated_Cells_" + cellLabeledMask.getTitle())) {
                    IJ.log("The binary mask validated" + cellLabeledMask.getTitle() + " with only cells containing a nucleus was saved in " + resultsDirectory + "/Images/" + "Validated_Cells_" + cellLabeledMask.getTitle());
                } else {
                    IJ.log("The binary mask validated" + cellLabeledMask.getTitle() + " with only cells containing a nucleus could not be saved in " + resultsDirectory + "/Images/");
                }
            }
        }else {
            IJ.log("No cells with a nucleus were detected");
        }

    }

//    GETTER

    /**
     * @return CytoDetector associated to CellDetector (cellRois are given)
     */
    public CytoDetector getCytoDetector(){
        return new CytoDetector(imageToMeasure,image.getTitle(),cellRois,resultsDirectory,showBinaryImage, saveBinary,saveRois, minNucleiCellOverlap,minCytoSize);
    }

    /**
     * Segment cell to obtain ROIs for further analysis
     * @return true if no problem occurred
     */
    public boolean prepare() {
        //IJ.showMessage("CellDetector showPreprocessed="+showPreprocessedImage+" showComposite="+showCompositeImage+" showBinary="+showBinaryImage);
        //check saving directory
        if(saveRois){
            File tmp=new File(resultsDirectory + "/ROI/AllDetected/");
            if(!tmp.exists()) tmp.mkdirs();
        }
        if (saveBinary){
            File tmp=new File(resultsDirectory + "/Images/");
            if(!tmp.exists()) tmp.mkdirs();
        }
//        PREPROCESSING
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed != null) {
            if (showPreprocessedImage){
                //preprocessed.setTitle("preprocessed");
                preprocessed.show();
                WindowManager.setWindow(WindowManager.getWindow("Log"));
            }


//            SEGMENTATION : launch cellpose command to obtain mask
            /*cyto channel : 0=grayscale, 1=red, 2=green, 3=blue*/
            /*nuclei channel : 0=None (will set to zero), 1=red, 2=green, 3=blue*/
            RoiManager roiManagerCell;
            CellposeLauncher cellposeLauncher;
            /*Use nuclei image, if exists, to improve segmentation*/
            /*Cellpose needs in this case a composite image that contains the nuclei and cytoplasm channels*/
            if (nucleiDetector != null) {
                ImagePlus nucleiProcessedImage = nucleiDetector.getPreprocessing();
                if (nucleiProcessedImage != null) {
//                    Create composite
                    ImagePlus composite = RGBStackMerge.mergeChannels(new ImagePlus[]{preprocessed, nucleiDetector.getPreprocessing()}, true);
                    if (showCompositeImage){
                        composite.setTitle(nameExperiment + "_composite");
                        composite.show();
                    }
//                    Create CellposeLauncher objet
                    cellposeLauncher = new CellposeLauncher(composite, minSizeCell, cellposeCellproba_threshold,cellposeModel, 1, 2, excludeOnEdges);
                } else {
                    IJ.error("There is a problem with the nuclei preprocessing, please verify the parameters");
                    return false;
                }
            } else {/*No nuclei channel*/
                cellposeLauncher = new CellposeLauncher(preprocessed, minSizeCell,cellposeCellproba_threshold, cellposeModel, excludeOnEdges);
            }
//            Launch Cellpose
            cellposeLauncher.analysis();
//            Get cellpose mask and roiManager
            ImagePlus cellposeOutput = cellposeLauncher.getCellposeMask();
            detector.renameImage(cellposeOutput, "cellpose_Cells");
            roiManagerCell = cellposeLauncher.getCellposeRoiManager();
            if (showBinaryImage) {
                cellposeOutput.show();
                cellposeOutput.setDisplayRange(0, (cellposeLauncher.getCellposeRoiManager().getCount() + 10));
                cellposeOutput.updateAndDraw();
            }
//            Allow user to redefine the regions of interest
            if (finalValidation){
                roiManagerCell.toFront();
                ImagePlus tempImage = imageToMeasure.duplicate(); /*Need to duplicate, as closing the image nullify the ImageProcessor*/
                /*if (showBinaryImage){
                    IJ.selectWindow(imageToMeasure.getID());
                }else {
                    tempImage.show();
                }*/
                tempImage.show();
                IJ.selectWindow(tempImage.getID());
                roiManagerCell.runCommand("Show All");
                new WaitForUserDialog("Cell selection","Delete cells : select the ROIs + delete").show();
                if (!showBinaryImage){
                    tempImage.close();
                }
                /*Obtain new ROIs*/
                cellposeOutput = detector.labeledImage(roiManagerCell.getRoisAsArray());
            }
//            SAVINGS
            if (resultsDirectory != null && saveBinary) {
                detector.setLUT(cellposeOutput);
                if (IJ.saveAsTiff(cellposeOutput, resultsDirectory + "/Images/" + "Detected_Cells_" + cellposeOutput.getTitle())){
                    IJ.log("The binary mask " + cellposeLauncher.getCellposeMask().getTitle() + " was saved in " + resultsDirectory + "/Images/" + "Detected_Cells_" + cellposeOutput.getTitle());
                }else {
                    IJ.log("The binary mask " + cellposeLauncher.getCellposeMask().getTitle() + " could not be saved in " + resultsDirectory + "/Images/");
                }
            }
            if (resultsDirectory != null && saveRois) {
                for (int i = 0; i < roiManagerCell.getCount(); i++) {
                    roiManagerCell.rename(i,"Cell_"+(i+1));
                }
                if(roiManagerCell.getCount()>0) {
                    String extension=(roiManagerCell.getCount()==1)?".roi":".zip";
                    if (roiManagerCell.save(resultsDirectory + "/ROI/AllDetected/" + image.getTitle() + "_Cells_detected_roi"+extension)) {
                        IJ.log("The cell ROIs of " + image.getTitle() + " were saved in " + resultsDirectory + "/ROI/AllDetected/");
                    } else {
                        IJ.log("The cell ROIs of " + image.getTitle() + " could not be saved in " + resultsDirectory + "/ROI/AllDetected/");
                    }
                }
            }
            cellRois = roiManagerCell.getRoisAsArray();
//            Create analyzer for future measurements
            analyzer = new Analyzer(imageToMeasure, measurements, rawMeasures);
            return true;
        } else return false;
    }

    /**
     * Preview of segmentation (similar to prepare without final validation and RoiManager)
     * - does the preprocessing:
     * - does the segmentation by cellpose
     * - show result labeled image
     */
    public void preview() {
//        PREPROCESSING
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed != null) {
            preprocessed.show();
            WindowManager.setWindow(WindowManager.getWindow("Log"));
//            SEGMENTATION : launch cellpose command to obtain mask
            /*cyto channel : 0=grayscale, 1=red, 2=green, 3=blue*/
            /*nuclei channel : 0=None (will set to zero), 1=red, 2=green, 3=blue*/
            CellposeLauncher cellposeLauncher;
            /*Use nuclei image, if exists, to improve segmentation*/
            /*Cellpose needs in this case a composite image that contains the nuclei and cytoplasm channels*/
            if (nucleiDetector != null) {
                ImagePlus nucleiProcessedImage = nucleiDetector.getPreprocessing();
                if (nucleiProcessedImage != null) {
                    /*Create composite*/
                    ImagePlus composite = RGBStackMerge.mergeChannels(new ImagePlus[]{imageToMeasure, nucleiDetector.getPreprocessing()}, true);
                    composite.setTitle(nameExperiment + "_composite");
                    if (showCompositeImage){
                        composite.show();
                    }
//                    Create CellposeLauncher object
                    cellposeLauncher = new CellposeLauncher(composite, minSizeCell,cellposeCellproba_threshold,
                            cellposeModel, 1, 2, excludeOnEdges);
                } else {
                    IJ.error("There is a problem with the nuclei preprocessing, please verify the parameters");
                    return;
                }
            } else { /*no nuclei channel*/
                cellposeLauncher = new CellposeLauncher(preprocessed, minSizeCell,cellposeCellproba_threshold,
                        cellposeModel, excludeOnEdges);
            }
//            Launch Cellpose
            cellposeLauncher.analysis();
//            Get Cellpose mask
            ImagePlus cellposeOutput = cellposeLauncher.getCellposeMask();
//            Show cellpose mask
            cellposeOutput.show();
            cellposeOutput.setDisplayRange(0, (cellposeLauncher.getCellposeRoiManager().getCount() + 10));
            cellposeOutput.updateAndDraw();
        }
    }

    /**
     * Preprocess image for segmentation
     * @return ImagePlus or null if preprocessing did not work
     */
    private ImagePlus getPreprocessing() {
        if (detector.getImage() != null) {
            IJ.run("Options...", "iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
            this.imageToMeasure = detector.getImageQuantification(); /*detector class does the projection if needed*/
            ImagePlus imageToReturn = detector.getImage().duplicate(); /*detector class does the projection if needed*/
            ImagePlus temp;
//      MACRO : apply custom commands of user
            if (macroText!=null){
                //IJ.log("use macro: "+macroText);
                imageToReturn.show();
                IJ.selectWindow(imageToReturn.getID());
                IJ.runMacro("setBatchMode(true);"+macroText+"setBatchMode(false);"); /*accelerates the treatment by displaying only the last image*/
                temp = WindowManager.getCurrentImage();
                imageToReturn = temp.duplicate();
                imageToReturn.setTitle(imageToReturn.getTitle()+"afterMacro");
                temp.changes=false;
                temp.close();
            }else{
                //IJ.log("no macro defined");
            }
            return imageToReturn;
        } else return null;

    }

    public void cleanNameExperiment(){
        nameExperiment=nameExperiment.replaceAll(":","_");
    }
    /**
     *
     * @return name of experiment
     */
    public String getNameExperiment() {
        return nameExperiment;
    }

    /**
     * @return image title
     */
    public String getImageTitle() {
        return image.getTitle();
    }

    /**
     *
     * @return Array of the cell ROIs
     */
    public Roi[] getRoiArray() {
        return cellRois;
    }

    public String getNameChannel() {
        return nameChannel;
    }

    public void setNameChannel(String nameChannel) {
        this.nameChannel = nameChannel;
    }

    /**
     *
     * @param cell : number of the cell in the array
     * @param resultsTableFinal : {@link ResultsTable} that will contain the results
     */
    public void measureEachCell(int cell,ResultsTable resultsTableFinal) {
        imageToMeasure.setRoi(cellRois[cell]);
        //    Results
        analyzer.measure();
        detector.setResultsAndRename(rawMeasures,resultsTableFinal,cell,"Cell"+nameChannel);
    }

    /**
     *
     * @param measurements : integer corresponding to measurements to do on cells
     */
    public void setMeasurements(int measurements) {
        this.measurements = measurements;
    }
}
