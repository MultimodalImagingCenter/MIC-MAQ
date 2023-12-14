package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
/**
 * Author : Camille RABIER
 * Date : 07/06/2022
 * Class for
 * - segmenting and measuring cytoplasm
 * - it uses ROI from cell and nuclei to obtain cytoplasm
 */
public class CytoDetector {
    private final boolean saveBinaryImage;
    private final ResultsTable rawMeasures;
    private final Analyzer analyzer;
    private Roi[] nucleiRois;
    private final Detector detector;
    private final boolean saveRois;
    private final ImagePlus cellCytoImageToMeasure;
    private final double minNucleiCellOverlap;
    private final double minCytoSize;
    private final String resultsDirectory;

    private String nameExperiment;
    private final boolean showBinaryImage;

    private int[] associationCell2Nuclei;
    private final Roi[] cellRois;
    private Roi[] trueCellRois;
    private Roi[] cytoplasmRois;
    private int[] numberOfNucleiPerCell;
    private int[] trueNbNucleiPerCell;
    private Roi[] trueAssociatedRoi;

    /**
     * Constructor
     * @param cellCytoImageToMeasure : image to measure
     * @param imageName : name of image (for naming of binary mask and rois)
     * @param cellRois : rois corresponding to cells
     * @param resultsDir : directory for saving results
     * @param showBinaryImage : choice to show mask
     * @param saveBinaryImage : choice to save mask
     * @param saveRois : choice to save Rois
     * @param minNucleiCellOverlap : minimum overlap between nucleus and cell to consider a nucleus to be part of a cell
     * @param minCytoSize : minimum size (in pixel) of cytoplasm to be qualified for measurements
     */
    public CytoDetector(ImagePlus cellCytoImageToMeasure, String imageName, Roi[] cellRois, String resultsDir, boolean showBinaryImage, boolean saveBinaryImage, boolean saveRois, double minNucleiCellOverlap, double minCytoSize) {
        this.cellRois = cellRois;
        this.cellCytoImageToMeasure = cellCytoImageToMeasure.duplicate();
        this.minNucleiCellOverlap = minNucleiCellOverlap;
        this.minCytoSize = minCytoSize;
        this.cellCytoImageToMeasure.setTitle(imageName);
        if (imageName.endsWith("_")){
            nameExperiment = imageName.substring(0,imageName.length()-1);
        }else {
            nameExperiment =imageName;
        }
        //IJ.log("name experiment "+nameExperiment.replaceAll("[\\\\/:,;*?\"<>|]","_"));
        //this.resultsDirectory =resultsDir+"/Results/"+nameExperiment.replaceAll("[\\\\/:,;*?\"<>|]","_").replaceAll(" ","");
        //File dir=new File(resultsDirectory);
        //if(!dir.exists()) dir.mkdirs();
        this.resultsDirectory=resultsDir;
        this.showBinaryImage = showBinaryImage;
        this.saveBinaryImage = saveBinaryImage;
        this.detector = new Detector(cellCytoImageToMeasure, "Cytoplasm");
        this.saveRois = saveRois;
        rawMeasures = new ResultsTable();
//        Define measurements to do
        analyzer = new Analyzer(this.cellCytoImageToMeasure, Measurements.MEAN + Measurements.AREA + Measurements.INTEGRATED_DENSITY, rawMeasures);
    }

    /**
     *
     * @param nucleiRois regions corresponding to nuclei
     */
    public void setNucleiRois(Roi[] nucleiRois) {
        this.nucleiRois = nucleiRois;
    }

    /**
     * @param measureCalibration Calibration value and unit to use for measurements
     */
    public void setMeasureCalibration(MeasureCalibration measureCalibration) {
        detector.setMeasureCalibration(measureCalibration);
    }

    /**
     * Get cytoplasm and associated nuclei ROIs + masks
     *
     * @return true if everything worked
     */
    public boolean prepare() {
        if(saveRois){
            File tmp=new File(resultsDirectory + "/ROI/Validated/");
            if(!tmp.exists()) tmp.mkdirs();
        }
        if (saveBinaryImage){
            File tmp=new File(resultsDirectory + "/Images/");
            if(!tmp.exists()) tmp.mkdirs();
        }
//        Associated each cell to a nuclei
        associateNucleiCell();
//        Subtract nuclei regions from cell to obtain cytoplasm regions
        getCytoplasmRois();
//        Get mask associated to cytoplasm
        ImagePlus cytoplasmLabeledMask = detector.labeledImage(cytoplasmRois);
        detector.setLUT(cytoplasmLabeledMask);
        if (showBinaryImage) {
            cytoplasmLabeledMask.show();
            cytoplasmLabeledMask.setDisplayRange(0, cytoplasmRois.length + 5);
        }
//        SAVING
        if (resultsDirectory != null) {
            if (saveBinaryImage) {
                if (IJ.saveAsTiff(cytoplasmLabeledMask, resultsDirectory + "/Images/" + "Validated_Cytoplams_" + cytoplasmLabeledMask.getTitle())) {
                    IJ.log("The binary mask " + cytoplasmLabeledMask.getTitle() + " was saved in " + resultsDirectory + "/Images/" + "Validated_Cytoplams_" + cytoplasmLabeledMask.getTitle());
                } else {
                    IJ.log("The binary mask " + cytoplasmLabeledMask.getTitle() + " could not be saved in " + resultsDirectory + "/Images/");
                }
            }
            if (saveRois){ /*need to put Rois in roimanager to save*/
                RoiManager roiManager = RoiManager.getInstance();
                if (roiManager == null) { /*if no instance of roiManager, creates one*/
                    roiManager = new RoiManager();
                } else { /*if already exists, empties it*/
                    roiManager.reset();
                }
                for (int i = 0; i < cytoplasmRois.length; i++) {
                    Roi roi = cytoplasmRois[i];
                    if (roi!=null){
                        roi.setName("Cytoplasm_"+(i+1));
                        roiManager.addRoi(roi);
                    }
                }
                if(roiManager.getCount()>0) {
                    String extension=(roiManager.getCount()==1)?".roi":".zip";
                    if (roiManager.save(resultsDirectory + "/ROI/Validated/" + "Validated_Cytoplams_" + cellCytoImageToMeasure.getTitle() + "_cytoplasm_roi"+extension)) {
                        IJ.log("The cytoplasm ROIs of " + cellCytoImageToMeasure.getTitle() + " were saved in " + resultsDirectory + "/ROI/Validated/" + "Validated_Cytoplams_" + cellCytoImageToMeasure.getTitle() + "_cytoplasm_roi"+extension);
                    } else {
                        IJ.log("#########\nThe cytoplasm ROIs of " + cellCytoImageToMeasure.getTitle() + " could not be saved in " + resultsDirectory + "/ROI/Validated/\n######");
                    }
                }

            }
        }
        return true;
    }

    /**
     *
     * @return nuclei rois that have been associated to a cell
     */
    public Roi[] getAssociatedNucleiRois(){
        return trueAssociatedRoi;
}

    /**
     *
     * @return table of association between cell and nuclei
     */
    public int[] getAssociationCell2Nuclei() {
        return associationCell2Nuclei;
    }

    /**
     *
     * @return Rois corresponding to cytoplasm
     */
    public Roi[] getCytoRois() {
        return cytoplasmRois;
    }

    /**
     * For each cell, find each nucleus associated
     * The association is done through the verification of the overlap between a cell's and nuclei's bounding rectangle and then the verification
     * that at least a pixel is contained by both regions (in particular for cases of non-ovoid forms) and verify that the overlap is enough
     * If multiple nuclei associated : combines them
     * Only the nuclei that are at minimum at 90% in the cell are considered
     */
    private void associateNucleiCell() {
        associationCell2Nuclei = new int[nucleiRois.length]; /*table of association between nuclei and cells*/
        Arrays.fill(associationCell2Nuclei,-1);

        Roi[] associatedNucleiRois = new Roi[cellRois.length];
        numberOfNucleiPerCell = new int[cellRois.length];
//        IDs of ROIs to keep
        ArrayList<Integer> cellRoisToKeep = new ArrayList<>();
        ArrayList<Integer> associatedNucleiToKeep = new ArrayList<>();
        ArrayList<Integer> numberNucleiToKeep = new ArrayList<>();

//        Iterate on each cell roi
        for (int cellID = 0; cellID < cellRois.length; cellID++) {
            Roi cellROI = cellRois[cellID];
            Rectangle cellBounding = cellROI.getBounds();
//            Iterate on each nucleus roi
            for (int nucleusID = 0; nucleusID < nucleiRois.length; nucleusID++) {
                Roi nucleusROI = nucleiRois[nucleusID];
                Rectangle nucleiBounding = nucleusROI.getBounds();
//                Verify overlap
                if (roisBoundingBoxOverlap(cellBounding,nucleiBounding)) {
                    double commonPoints = 0;
                    for (Point cellPoint : cellROI.getContainedPoints()) {
                        if (nucleusROI.contains(cellPoint.x, cellPoint.y)) {
                            commonPoints++;
                        }
                    }
                    if (commonPoints>minNucleiCellOverlap*nucleusROI.getContainedPoints().length){
//                            If multiple nuclei in cell, consider it is a cell in division. The nuclei are thus combined to be considered as only one
//                            To combine them : first we convert them to shape ROI, then use the function "or" and "tryToSimplify" of ShapeROI
                        numberOfNucleiPerCell[cellID]++;
//                        Associate nuclei in same cell
                        if (associatedNucleiRois[cellID]!=null){
                            Roi roi1 = associatedNucleiRois[cellID];
                            ShapeRoi s1 = new ShapeRoi(roi1);
                            ShapeRoi s2 = new ShapeRoi(nucleusROI);
                            s1.or(s2); /*union*/
                            Roi roi = s1.trySimplify();
                            associatedNucleiRois[cellID] = roi;
                        }else {
                            associatedNucleiRois[cellID]=nucleusROI;
                        }
//                        Fill association table
                        if (associationCell2Nuclei[nucleusID] == -1) {
                            associationCell2Nuclei[nucleusID] = cellID+1;
                        } else {
                            //IJ.error("The nucleus :"+nucleusID+" is associated to multiple cells.");
                            IJ.log("##################################");
                            IJ.log("#         WARNING !!!                  #");
                            IJ.log("##################################");
                            IJ.log("The nucleus :"+nucleusID+" is associated to multiple cells.");
                            IJ.log("##################################");
                        }
                    }
                }
            }
//            Keep only cell with minimum 1 nucleus


            if (numberOfNucleiPerCell[cellID]>0){
                cellRoisToKeep.add(cellID);
                associatedNucleiToKeep.add(cellID);
                numberNucleiToKeep.add(numberOfNucleiPerCell[cellID]);
            }
        }
        System.out.println("associate nuclei to cells: before "+cellRois.length+" cells after "+cellRoisToKeep.size());
//        If not all cells are kept, the ids in cellRois and associatedNucleiRois need to be changed
        if (cellRoisToKeep.size()!=cellRois.length){
            trueCellRois = new Roi[cellRoisToKeep.size()];
            trueAssociatedRoi = new Roi[cellRoisToKeep.size()];
            trueNbNucleiPerCell = new int[cellRoisToKeep.size()];
            for (int trueCellID = 0; trueCellID < cellRoisToKeep.size() ; trueCellID++) {
                trueCellRois[trueCellID] = cellRois[cellRoisToKeep.get(trueCellID)];
                trueAssociatedRoi[trueCellID] = associatedNucleiRois[associatedNucleiToKeep.get(trueCellID)];
                trueNbNucleiPerCell[trueCellID]=numberNucleiToKeep.get(trueCellID);
            }
            System.out.println(trueCellRois.length+" "+trueAssociatedRoi.length+" "+trueNbNucleiPerCell);
        }else {
            trueCellRois = cellRois;
            trueAssociatedRoi = associatedNucleiRois;
            trueNbNucleiPerCell=numberOfNucleiPerCell;
        }
    }

    /**
     * Subtract nucleus roi for associated cell Roi
     */
    private void getCytoplasmRois() {
        cytoplasmRois = new Roi[trueCellRois.length];
//        Iterate on each cell roi
        for (int cellID = 0; cellID < trueCellRois.length; cellID++) {
            Roi nuclei = trueAssociatedRoi[cellID];
            Roi cell = trueCellRois[cellID];
            Roi cytoplasm = (Roi) cell.clone();
            if (nuclei != null) {
                double common = (double) (cell.getContainedPoints().length - nuclei.getContainedPoints().length)/cell.getContainedPoints().length;
                if (common > minCytoSize){
                    ShapeRoi s0 = new ShapeRoi(cell);
                    ShapeRoi s1 = new ShapeRoi(cytoplasm);
                    ShapeRoi s2 = new ShapeRoi(nuclei);
                    s1.xor(s2); /*get part of both ROI that do not overlap (mainly the cytoplasm thus)*/
                    s1.and(s0); /*remove part of "non overlapping roi" that is part of nucleus and not cell (some nucleus can be segmented partially outside of cell depending on staining)*/
                    cytoplasm = s1.trySimplify();
                }else {
                    cytoplasm = null;
                }
            }
            cytoplasmRois[cellID] = cytoplasm;
        }
    }

    /**
     *
     * @return cell ROIs, with the cell without nucleus = null
     */
    public Roi[] getCellRois() {
        return trueCellRois;
    }

    /**
     *
     * @param cellRec bounding rectangle of cell ROI
     * @param nucleusRec bounding rectangle of nucleus ROI
     * @return true if cell rectangle contains the nucleus rectangle geometric center pixel
     */
    public boolean roisBoundingBoxOverlap(Rectangle cellRec, Rectangle nucleusRec){
        return (cellRec.contains(nucleusRec.getCenterX(),nucleusRec.getCenterY()));
    }
    /**
     * Do the measurement for each cell and add them to result table
     *
     * @param cellID            : index of the cell to analyze
     * @param resultsTableFinal : resultTable to fill
     */
    public void measureEachCytoplasm(int cellID, ResultsTable resultsTableFinal) {
        if (cytoplasmRois[cellID]!=null){
            Roi cytoRoi = cytoplasmRois[cellID];
            cellCytoImageToMeasure.setRoi(cytoRoi);
            analyzer.measure();
            resultsTableFinal.addValue("Number of nuclei", trueNbNucleiPerCell[cellID]);
            detector.setResultsAndRename(rawMeasures, resultsTableFinal, cellID, "Cytoplasm");
        }else {
            analyzer.measure();
            resultsTableFinal.addValue("Number of nuclei", numberOfNucleiPerCell[cellID]);
            detector.setResultsAndRename(rawMeasures, resultsTableFinal, -1, "Cytoplasm");
        }
    }
}
