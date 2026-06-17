package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import mcib3d.geom2.*;
import mcib3d.image3d.ImageByte;
import mcib3d.image3d.ImageHandler;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Detector detector;
    private final boolean saveRois;
    private final ImagePlus cellCytoImageToMeasure;
    private final double minNucleiCellOverlap;
    private final double minCytoSize;
    private final String resultsDirectory;

    private String nameExperiment;
    private final boolean showBinaryImage;

    private int[] associationCell2Nuclei;
    private int[] associationCell2NucleiTrue;
    private Roi[] cellRois;
    private Roi[] nucleiRois;
    private Roi[] cytoplasmRois;
    private Roi[] trueCellRois;
    private Roi[] trueAssociatedRoi;

    private Object3DInt[] cellRois3D;
    private Object3DInt[] nucleiRois3D;
    private Object3DInt[] cytoplasmRoi3D;
    private Object3DInt[] trueCellRoi3D;
    private Object3DInt[] trueAssociatedRoi3D;
    private int[] numberOfNucleiPerCell;
    private int[] trueNbNucleiPerCell;

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
        this(cellCytoImageToMeasure,imageName,resultsDir,showBinaryImage,saveBinaryImage,saveRois,minNucleiCellOverlap,minCytoSize);
        this.cellRois = cellRois;
    }

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
    public CytoDetector(ImagePlus cellCytoImageToMeasure, String imageName, Object3DInt[] cellRois, String resultsDir, boolean showBinaryImage, boolean saveBinaryImage, boolean saveRois, double minNucleiCellOverlap, double minCytoSize) {
        this(cellCytoImageToMeasure,imageName,resultsDir,showBinaryImage,saveBinaryImage,saveRois,minNucleiCellOverlap,minCytoSize);
        this.cellRois3D = cellRois;
        IJ.log("CytoDetector constructor cellRois3D is used");
    }

    protected CytoDetector(ImagePlus cellCytoImageToMeasure, String imageName, String resultsDir, boolean showBinaryImage, boolean saveBinaryImage, boolean saveRois, double minNucleiCellOverlap, double minCytoSize) {
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
     *
     * @param nucleiRois regions corresponding to nuclei
     */
    public void setNucleiRois3D(Object3DInt[] nucleiRois) {
        this.nucleiRois3D = nucleiRois;
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
            File tmp=new File(resultsDirectory + "/Images/Validated/");
            if(!tmp.exists()) tmp.mkdirs();
        }
        //3D or 2D ???
        if(nucleiRois3D!=null){
            IJ.log("##########\ncytoplasm detector 3D nuclei ROIs are used\n##########");
//        Associated each cell to a nuclei
            associateNucleiCell3D();
            IJ.log(trueCellRoi3D.length+" cells associated to "+trueAssociatedRoi3D.length+" nuclei" );
//        Subtract nuclei regions from cell to obtain cytoplasm regions
            getCytoplasmRois3D();
//        Get mask associated to cytoplasm
        }else {
            IJ.log("##########\n2D nuclei ROIs are used\n##########");
//        Associated each cell to a nuclei
            associateNucleiCell();
//        Subtract nuclei regions from cell to obtain cytoplasm regions
            getCytoplasmRois();

        }
//        Get mask associated to cytoplasm
        ImagePlus cytoplasmLabeledMask = createCytoplasmImage();
        //ImagePlus cytoplasmLabeledMask = detector.labeledImage(cytoplasmRois);
        detector.setLUT(cytoplasmLabeledMask);
        if (showBinaryImage) {
            cytoplasmLabeledMask.show();
            if(cytoplasmRois!=null)cytoplasmLabeledMask.setDisplayRange(0, cytoplasmRois.length + 5);
            else if(cytoplasmRoi3D!=null) cytoplasmLabeledMask.setDisplayRange(0, cytoplasmRoi3D.length + 5);
        }
//        SAVING
        if (resultsDirectory != null) {
            if (saveBinaryImage) {
                if (IJ.saveAsTiff(cytoplasmLabeledMask, resultsDirectory + "/Images/Validated/" + "Cytoplams_" + cytoplasmLabeledMask.getTitle())) {
                    IJ.log("The cytoplasm mask " + cytoplasmLabeledMask.getTitle() + " was saved in " + resultsDirectory + "/Images/Validated/" );
                } else {
                    IJ.log("The cytoplasm mask " + cytoplasmLabeledMask.getTitle() + " could not be saved in " + resultsDirectory + "/Images/Validated/");
                }
            }
            if (saveRois){
                if(cytoplasmRois!=null && cytoplasmRois.length>1) {/*need to put Rois in roimanager to save*/
                    RoiManager roiManager = RoiManager.getInstance();
                    if (roiManager == null) { /*if no instance of roiManager, creates one*/
                        roiManager = new RoiManager();
                    } else { /*if already exists, empties it*/
                        roiManager.reset();
                    }
                    for (int i = 0; i < cytoplasmRois.length; i++) {
                        Roi roi = cytoplasmRois[i];
                        if (roi != null) {
                            roi.setName("Cytoplasm_" + (i + 1));
                            roiManager.addRoi(roi);
                        }
                    }
                    if (roiManager.getCount() > 0) {
                        String extension = (roiManager.getCount() == 1) ? ".roi" : ".zip";
                        if (roiManager.save(resultsDirectory + "/ROI/Validated/" + cellCytoImageToMeasure.getTitle() + "_CytoplasmROIs" + extension)) {
                            IJ.log("The cytoplasm ROIs of " + cellCytoImageToMeasure.getTitle() + " were saved in " + resultsDirectory + "/ROI/Validated/");
                        } else {
                            IJ.log("#########\nThe cytoplasm ROIs of " + cellCytoImageToMeasure.getTitle() + " could not be saved in " + resultsDirectory + "/ROI/Validated/\n######");
                        }
                    }
                } else if (cytoplasmRoi3D!=null) {

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
     * @return nuclei rois that have been associated to a cell
     */
    public Object3DInt[] getAssociatedNucleiRois3D(){
        return trueAssociatedRoi3D;
    }
    /**
     *
     * @return table of association between cell and nuclei with Cell IDs of detected cells
     */
    public int[] getAssociationCell2Nuclei() {
        return associationCell2Nuclei;
    }

    /**
     *
     * @return table of association between cell and nuclei with cell IDs of validated cells
     */
    public int[] getAssociationCell2NucleiTrue() {
        if(associationCell2NucleiTrue!=null) return associationCell2NucleiTrue;
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
     *
     * @return Rois corresponding to cytoplasm
     */
    public Object3DInt[] getCytoRois3D() {
        return cytoplasmRoi3D;
    }

    /**
     * For each cell, find each nucleus associated
     * The association is done through the verification of the overlap between a cell's and nuclei's bounding rectangle and then the verification
     * that at least a pixel is contained by both regions (in particular for cases of non-ovoid forms) and verify that the overlap is enough
     * If multiple nuclei associated : combines them
     * Only the nuclei that are at minimum at 90% in the cell are considered
     */
    private void associateNucleiCell() {
        if(nucleiRois==null) return;
        associationCell2Nuclei = new int[nucleiRois.length]; /*table of association between nuclei and cells*/
        Arrays.fill(associationCell2Nuclei,-1);

        Roi[] associatedNucleiRois = new Roi[cellRois.length];
        numberOfNucleiPerCell = new int[cellRois.length];
//        IDs of ROIs to keep
        ArrayList<Integer> cellRoisToKeep = new ArrayList<>();
        ArrayList<Integer> associatedNucleiToKeep = new ArrayList<>();
        ArrayList<Integer> numberNucleiToKeep = new ArrayList<>();

        //int counttmp1=0;
        //int counttmp2=0;
        //int counttmp3=0;
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
                        //counttmp1++;
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
                            //counttmp2++;
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
                //counttmp3++;
                cellRoisToKeep.add(cellID);
                associatedNucleiToKeep.add(cellID);
                numberNucleiToKeep.add(numberOfNucleiPerCell[cellID]);
            }
        }
        //System.out.println("validated: "+counttmp1);
        //System.out.println("nuclei completed "+counttmp2);
        //System.out.println("cells with at least one nucleus "+counttmp3);
        int counttmp=0;
        for(int tmp:associationCell2Nuclei) if(tmp>=0) counttmp++;
        int counttmp4=0;
        for(int tmp=0; tmp<numberOfNucleiPerCell.length; tmp++)  {
            counttmp4+=numberOfNucleiPerCell[tmp];
            if(numberOfNucleiPerCell[tmp]>1) System.out.println("cell "+tmp+" has "+numberOfNucleiPerCell[tmp]+" nuclei");
        }
        System.out.println("total number of nuclei in cells: "+counttmp4);
        System.out.println("associate nuclei to cells: before "+cellRois.length+" cells after "+cellRoisToKeep.size()+"("+counttmp+")");
//        If not all cells are kept, the ids in cellRois and associatedNucleiRois need to be changed
        if (cellRoisToKeep.size()!=cellRois.length){
            int[] conversionCellID=new int[cellRois.length+1];
            Arrays.fill(conversionCellID,-1);
            //Map<Integer,Integer> conversionCellID=new HashMap<Integer,Integer>();
            trueCellRois = new Roi[cellRoisToKeep.size()];
            trueAssociatedRoi = new Roi[cellRoisToKeep.size()];
            trueNbNucleiPerCell = new int[cellRoisToKeep.size()];
            for (int trueCellID = 0; trueCellID < cellRoisToKeep.size() ; trueCellID++) {
                trueCellRois[trueCellID] = cellRois[cellRoisToKeep.get(trueCellID)];
                trueAssociatedRoi[trueCellID] = associatedNucleiRois[associatedNucleiToKeep.get(trueCellID)];
                trueNbNucleiPerCell[trueCellID]=numberNucleiToKeep.get(trueCellID);
                conversionCellID[cellRoisToKeep.get(trueCellID)]=trueCellID;
                //conversionCellID.put(associatedNucleiToKeep.get(trueCellID),trueCellID);
            }
            //System.out.println(trueCellRois.length+" "+trueAssociatedRoi.length+" "+trueNbNucleiPerCell);
            //convert the associated cell number to nuclei to new ids
            associationCell2NucleiTrue=new int[associationCell2Nuclei.length];
            //int tmpcountfilter=0;
            //int tmpcountfilterTrue=0;
            //int tmpcountConverion=0;
            //for(int tmp:associationCell2Nuclei) if(tmp>0) tmpcountfilter++;
            //for(int tmp:associationCell2NucleiTrue) if(tmp>0) tmpcountfilterTrue++;
            //for(int tmp:conversionCellID) if(tmp>0) tmpcountConverion++;
            //System.out.println("before changing indexes : "+tmpcountfilter);
            //System.out.println("before changing indexes (true): "+tmpcountfilterTrue);
            //System.out.println("before changing indexes (conversion): "+tmpcountConverion);
            //tmpcountfilter=0;
            for(int nID=0;nID<associationCell2Nuclei.length;nID++){
                //System.out.println("nuclei "+nID+" associated to "+associationCell2Nuclei[nID]);
                if(associationCell2Nuclei[nID]>=0){
                    //tmpcountfilter++;
                    associationCell2NucleiTrue[nID]=conversionCellID[associationCell2Nuclei[nID]-1]+1;
                    //System.out.println(" -> "+associationCell2NucleiTrue[nID]+"("+conversionCellID[associationCell2Nuclei[nID]]+")");
                    //associationCell2NucleiTrue[nID]=conversionCellID.get(nID);
                }else{
                    associationCell2NucleiTrue[nID]=associationCell2Nuclei[nID];
                }
            }
            //System.out.println("final size of rois "+trueCellRois.length+"("+tmpcountfilter+")");
            //tmpcountfilter=0;
            //tmpcountfilterTrue=0;
            //for(int tmp:associationCell2Nuclei) if(tmp>0) tmpcountfilter++;
            //for(int tmp:associationCell2NucleiTrue) if(tmp>0) tmpcountfilterTrue++;
            //System.out.println("after changing indexes : "+tmpcountfilter);
            //System.out.println("after changing indexes (true): "+tmpcountfilterTrue);
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
        if(trueCellRois==null) return;
        cytoplasmRois = new Roi[trueCellRois.length];
//        Iterate on each cell roi
        for (int cellID = 0; cellID < trueCellRois.length; cellID++) {
            Roi nuclei = trueAssociatedRoi[cellID];
            Roi cell = trueCellRois[cellID];
            Roi cytoplasm = (Roi) cell.clone();
            if (nuclei != null) {
                double common = (double) (cell.getContainedPoints().length - nuclei.getContainedPoints().length)/cell.getContainedPoints().length;
                if (common > minCytoSize){
                    cytoplasm = computeCytoplasmROI(cell, nuclei);
                }else {
                    cytoplasm = null;
                }
            }
            cytoplasmRois[cellID] = cytoplasm;
        }
    }

    private Roi computeCytoplasmROI(Roi cellROI, Roi nucleusROI){
        Roi cytoplasm = (Roi) cellROI.clone();
        ShapeRoi s0 = new ShapeRoi(cellROI);
        ShapeRoi s1 = new ShapeRoi(cytoplasm);
        ShapeRoi s2 = new ShapeRoi(nucleusROI);
        s1.xor(s2); /*get part of both ROI that do not overlap (mainly the cytoplasm thus)*/
        s1.and(s0); /*remove part of "non overlapping roi" that is part of nucleus and not cell (some nucleus can be segmented partially outside of cell depending on staining)*/
        cytoplasm = s1.trySimplify();
        return cytoplasm;
    }

    /**
     * For each cell, find each nucleus associated
     * The association is done through the verification of the overlap between a cell's and nuclei's bounding rectangle and then the verification
     * that at least a pixel is contained by both regions (in particular for cases of non-ovoid forms) and verify that the overlap is enough
     * If multiple nuclei associated : combines them
     * Only the nuclei that are at minimum at 90% in the cell are considered
     */
    private void associateNucleiCell3D() {
        associationCell2Nuclei = new int[nucleiRois3D.length]; /*table of association between nuclei and cells*/
        Arrays.fill(associationCell2Nuclei,-1);

        Object3DInt[] associatedNucleiRois = new Object3DInt[cellRois3D.length];
        numberOfNucleiPerCell = new int[cellRois3D.length];
//        IDs of ROIs to keep
        ArrayList<Integer> cellRoisToKeep = new ArrayList<>();
        ArrayList<Integer> associatedNucleiToKeep = new ArrayList<>();
        ArrayList<Integer> numberNucleiToKeep = new ArrayList<>();

//        Iterate on each cell roi
        for (int cellID = 0; cellID < cellRois3D.length; cellID++) {
            Object3DInt cellROI = cellRois3D[cellID];
            BoundingBox cellBounding = cellROI.getBoundingBox();
//            Iterate on each nucleus roi
            for (int nucleusID = 0; nucleusID < nucleiRois3D.length; nucleusID++) {
                Object3DInt nucleusROI = nucleiRois3D[nucleusID];
                BoundingBox nucleiBounding = nucleusROI.getBoundingBox();
                IJ.log("checking association cell "+cellID+" nucleus "+nucleusID);
//                Verify overlap
                if (roisBoundingBoxOverlap(cellBounding,nucleiBounding)) {
                    IJ.log("overlap detected");
                    Object3DComputation comp1=new Object3DComputation(cellROI);
                    Object3DComputation comp2=new Object3DComputation(comp1.getObject3DCopy());
                    Object3DInt intersection = comp2.getIntersection(nucleusROI);
                    double commonPoints = nbVoxelsInRoi3D( intersection);
                    IJ.log("common points : "+commonPoints);

                    if (commonPoints>minNucleiCellOverlap*nbVoxelsInRoi3D(nucleusROI)){
//                            If multiple nuclei in cell, consider it is a cell in division. The nuclei are thus combined to be considered as only one
//                            To combine them : first we convert them to shape ROI, then use the function "or" and "tryToSimplify" of ShapeROI
                        numberOfNucleiPerCell[cellID]++;
                        //counttmp1++;
//                        Associate nuclei in same cell
                        IJ.log("association validated");
                        if (associatedNucleiRois[cellID]!=null){
                            Object3DInt roi1 = associatedNucleiRois[cellID];
                            Object3DComputation comptmp = new Object3DComputation(roi1);
                            associatedNucleiRois[cellID] = comptmp.getUnion(nucleusROI);
                            IJ.log("union of nuclei ROI for cell "+cellID + associatedNucleiRois[cellID]);
                        }else {
                            associatedNucleiRois[cellID]=nucleusROI;
                        }
//                        Fill association table
                        if (associationCell2Nuclei[nucleusID] == -1) {
                            associationCell2Nuclei[nucleusID] = cellID+1;
                            //counttmp2++;
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
                //counttmp3++;
                cellRoisToKeep.add(cellID);
                associatedNucleiToKeep.add(cellID);
                numberNucleiToKeep.add(numberOfNucleiPerCell[cellID]);
            }
        }
        int counttmp=0;
        for(int tmp:associationCell2Nuclei) if(tmp>=0) counttmp++;
        int counttmp4=0;
        for(int tmp=0; tmp<numberOfNucleiPerCell.length; tmp++)  {
            counttmp4+=numberOfNucleiPerCell[tmp];
            if(numberOfNucleiPerCell[tmp]>1) System.out.println("cell "+tmp+" has "+numberOfNucleiPerCell[tmp]+" nuclei");
        }
        IJ.log("total number of nuclei in cells: "+counttmp4);
        IJ.log("associate nuclei to cells: before "+cellRois3D.length+" cells after "+cellRoisToKeep.size()+"("+counttmp+")");
//        If not all cells are kept, the ids in cellRois and associatedNucleiRois need to be changed
        if (cellRoisToKeep.size()!=cellRois3D.length){
            int[] conversionCellID=new int[cellRois3D.length+1];
            Arrays.fill(conversionCellID,-1);
            //Map<Integer,Integer> conversionCellID=new HashMap<Integer,Integer>();
            trueCellRoi3D = new Object3DInt[cellRoisToKeep.size()];
            trueAssociatedRoi3D = new Object3DInt[cellRoisToKeep.size()];
            trueNbNucleiPerCell = new int[cellRoisToKeep.size()];
            for (int trueCellID = 0; trueCellID < cellRoisToKeep.size() ; trueCellID++) {
                trueCellRoi3D[trueCellID] = cellRois3D[cellRoisToKeep.get(trueCellID)];
                trueAssociatedRoi3D[trueCellID] = associatedNucleiRois[associatedNucleiToKeep.get(trueCellID)];
                trueNbNucleiPerCell[trueCellID]=numberNucleiToKeep.get(trueCellID);
                conversionCellID[cellRoisToKeep.get(trueCellID)]=trueCellID;
            }
            //System.out.println(trueCellRois.length+" "+trueAssociatedRoi.length+" "+trueNbNucleiPerCell);
            //convert the associated cell number to nuclei to new ids
            associationCell2NucleiTrue=new int[associationCell2Nuclei.length];

            for(int nID=0;nID<associationCell2Nuclei.length;nID++){
                //System.out.println("nuclei "+nID+" associated to "+associationCell2Nuclei[nID]);
                if(associationCell2Nuclei[nID]>=0){
                    associationCell2NucleiTrue[nID]=conversionCellID[associationCell2Nuclei[nID]-1]+1;
                }else{
                    associationCell2NucleiTrue[nID]=associationCell2Nuclei[nID];
                }
            }

        }else {
            trueCellRoi3D = cellRois3D;
            trueAssociatedRoi3D = associatedNucleiRois;
            trueNbNucleiPerCell=numberOfNucleiPerCell;
        }
    }

    private void getCytoplasmRois3D() {
        cytoplasmRoi3D = new Object3DInt[trueCellRoi3D.length];
        for (int cellID = 0; cellID < trueCellRoi3D.length; cellID++) {
            Object3DInt nuclei = trueAssociatedRoi3D[cellID];
            Object3DInt cell = trueCellRoi3D[cellID];
            Object3DInt cytoplasm = null;
            if (nuclei != null) {
                int sizeCell = nbVoxelsInRoi3D(cell);
                int sizeNuclei = nbVoxelsInRoi3D(nuclei);
                double common = (double) (sizeCell - sizeNuclei)/sizeCell;
                IJ.log("ID "+cellID+" size cell "+sizeCell+" size nuclei "+sizeNuclei+" common "+common);
                if (common > minCytoSize){
                    IJ.log("cells nb planes : "+cell.getObject3DPlanes().size()+" nuclei nb planes : "+nuclei.getObject3DPlanes().size() );
                    cytoplasm = computeCytoplasmROI3D(cell, nuclei);
                }else {
                    cytoplasm = null;
                }
            }
            cytoplasmRoi3D[cellID] = cytoplasm;
        }
    }
    public static Object3DInt computeCytoplasmROI3D(Object3DInt cellROI, Object3DInt nucleusROI){
        if(false){
            //optim 1
            BoundingBox box = cellROI.getBoundingBox();
            ImageHandler compute1 = new ImageByte("Edge Morpho", box.xmax + 1, box.ymax + 1, box.zmax + 1);
            cellROI.drawObject(compute1, 1.0F);
            ImageHandler compute2 = new ImageByte("Edge Morpho", box.xmax + 1, box.ymax + 1, box.zmax + 1);
            nucleusROI.drawObject(compute1, 1.0F);
            ImageHandler compute = compute1.addImage(compute2, 1, -1);
            Object3DInt cytoplasm = new Object3DInt(compute, 1.0F);
            cytoplasm.setVoxelSizeXY(cellROI.getVoxelSizeXY());
            cytoplasm.setVoxelSizeZ(cellROI.getVoxelSizeZ());
            return cytoplasm;
        }else {
            //optim2
            Object3DInt cytoplasm = new Object3DInt();
            List<Object3DPlane> planescell = cellROI.getObject3DPlanes();
            List<Object3DPlane> planesnucleus = nucleusROI.getObject3DPlanes();
            for (int i = 0; i < planescell.size(); i++) {
                List<VoxelInt> planeVoxelscell = planescell.get(i).getVoxels();
                for (int k = 0; k < planeVoxelscell.size(); k++) {
                    VoxelInt voxel = planeVoxelscell.get(k);
                    boolean inNucleus = false;
                    for(int j=0;j<planesnucleus.size() && !inNucleus;j++){
                        List<VoxelInt> nucleusVoxels = planesnucleus.get(j).getVoxels();
                        if(nucleusVoxels.contains(voxel)){
                            inNucleus=true;
                        }
                    }
                    if(!inNucleus)cytoplasm.addVoxel(voxel);
                }
            }
            cytoplasm.setVoxelSizeXY(cellROI.getVoxelSizeXY());
            cytoplasm.setVoxelSizeZ(cellROI.getVoxelSizeZ());
            return cytoplasm;
        }
    }

    private int nbVoxelsInRoi3D(Object3DInt roi){
        int count=0;
        for(Object3DPlane plane:roi.getObject3DPlanes()){
            count+=plane.getVoxels().size();
        }
        return count;
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
     * @return cell ROIs, with the cell without nucleus = null
     */
    public Object3DInt[] getCellRois3D() {
        return trueCellRoi3D;
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
     *
     * @param cellRec bounding rectangle of cell ROI
     * @param nucleusRec bounding rectangle of nucleus ROI
     * @return true if cell rectangle contains the nucleus rectangle geometric center pixel
     */
    public boolean roisBoundingBoxOverlap(BoundingBox cellRec, BoundingBox nucleusRec){
        double cx=(nucleusRec.xmax+nucleusRec.xmin)/2.0;
        double cy=(nucleusRec.ymax+nucleusRec.ymin)/2.0;
        double cz=(nucleusRec.zmax+nucleusRec.zmin)/2.0;
        IJ.log("nucleus cx : "+cx+" cy : "+cy+" cz : "+cz);
        IJ.log("cell xmin : "+cellRec.xmin+" xmax : "+cellRec.xmax+" ymin : "+cellRec.ymin+" ymax : "+cellRec.ymax+" zmin : "+cellRec.zmin+" zmax : "+cellRec.zmax);
        return (cellRec.contains(cx,cy,cz));
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

    public int getNumberOfNuclei(int index){
        return trueNbNucleiPerCell[index];
    }
    public int[] getNumberOfNucleiPerCells(){
        return trueNbNucleiPerCell;
    }

    public ImagePlus createCytoplasmImage(){
        if(cytoplasmRois!=null) {
            return detector.labeledImage(cytoplasmRois);
        } else if (cytoplasmRoi3D!=null) {
            ImageStack stack = new ImageStack(cellCytoImageToMeasure.getWidth(), cellCytoImageToMeasure.getHeight() );
            for(int z=0; z<cellCytoImageToMeasure.getStackSize(); z++){
                if(cytoplasmRoi3D.length<255){
                    stack.addSlice(new ByteProcessor(cellCytoImageToMeasure.getWidth(),cellCytoImageToMeasure.getHeight()));
                }else{
                    stack.addSlice(new ShortProcessor(cellCytoImageToMeasure.getWidth(),cellCytoImageToMeasure.getHeight()));
                }
            }
            ImagePlus cytoplasmLabeledMask = new ImagePlus("labelmask",stack);
            ImageHandler imagehandler = ImageHandler.wrap(cytoplasmLabeledMask.getImageStack());
            for(int r=0;r<cytoplasmRoi3D.length;r++){
                List<Object3DPlane>planes =cytoplasmRoi3D[r].getObject3DPlanes();
                for(Object3DPlane plane:planes){
                    plane.drawObject(imagehandler,r+1);
                }
            }
            return cytoplasmLabeledMask;
        }
        return null;
    }

    public boolean is3D(){
        return cellRois3D!=null && cellRois3D.length>0;
    }
}
