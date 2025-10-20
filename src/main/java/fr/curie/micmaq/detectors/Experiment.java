package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import org.checkerframework.checker.units.qual.A;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
/**
 * Author : Camille RABIER
 * Date : 31/03/2022
 * Class for
 * - associating different channels of a set of experience
 */
public class Experiment {
    private  NucleiDetector nuclei;
    private  CellDetector cell;
    private  ArrayList<SpotDetector> spots;
    private  String experimentName;
    private  ResultsTable finalResultsNuclei;
    private  ResultsTable finalResultsCellSpot;
    private ResultsTable summary;
    private boolean onlyPositive4Spots=false;

    private ArrayList<ResultsTable> spotsInNucleiTable;
    private ArrayList<ResultsTable> spotsInCellsTable;
    private ArrayList<ResultsTable> spotsInCytoplasmsTable;

    private boolean interrupt = false;

    private  MeasureCalibration measureCalibration;

    /**
     * Constructor
     * @param nucleiDetector : {@link NucleiDetector}
     * @param cellDetector : {@link CellDetector}
     * @param spotDetectors :{@link ArrayList} of {@link SpotDetector}
     * @param finalResultsCellSpot : ResultsTable corresponding to measure in cell/cytoplasm or nuclei if no cell
     * @param finalResultsNuclei : ResultsTable corresponding to measure in nuclei if cell
     * @param measureCalibration : calibration to use for measures
     */
    public Experiment(NucleiDetector nucleiDetector, CellDetector cellDetector, ArrayList<SpotDetector> spotDetectors, ResultsTable finalResultsCellSpot, ResultsTable finalResultsNuclei, MeasureCalibration measureCalibration) {
        this.nuclei = nucleiDetector;
        this.cell = cellDetector;
        this.spots = spotDetectors;
        this.spotsInNucleiTable=new ArrayList<>();
        this.spotsInCellsTable=new ArrayList<>();
        this.spotsInCytoplasmsTable=new ArrayList<>();
        if(this.spots==null) {
            this.spots=new ArrayList<>();
        }else{
            for(int i=0;i<spots.size();i++) {
                spotsInNucleiTable.add(null);
                spotsInCellsTable.add(null);
                spotsInCytoplasmsTable.add(null);
            }
            IJ.log("experiment spot tables "+spots.size()+", "+spotsInNucleiTable.size()+", "+spotsInCellsTable.size()+", "+spotsInCytoplasmsTable.size());
        }
        if (nucleiDetector!=null) this.experimentName = nucleiDetector.getNameExperiment();
        else if (cellDetector!=null) this.experimentName = cellDetector.getNameExperiment();
        else if (spotDetectors.size()>0) this.experimentName = spotDetectors.get(0).getNameExperiment();
        else this.experimentName = null;
        this.finalResultsNuclei = finalResultsNuclei;
        this.finalResultsCellSpot = finalResultsCellSpot;
        this.measureCalibration=measureCalibration;

        if(nucleiDetector!=null) System.out.println("contructor experiment macro nuclei preprocess: "+nucleiDetector.getPreprocessingMacro());
    }

    /**
     * Constructor
     * @param experimentName : name of experiment
     * @param measureCalibration : calibration to use for measures
     */
    public Experiment(String experimentName, MeasureCalibration measureCalibration) {
        this.experimentName=experimentName;
        this.measureCalibration=measureCalibration;
    }

    /**
     * set detector fo nuclei
     * @param nucleiDetector : {@link NucleiDetector}
     */

    public void setNucleiDetector(NucleiDetector nucleiDetector){
        this.nuclei=nucleiDetector;
    }

    /**
     * set detectorr for cell
     * @param cellDetector : {@link CellDetector}
     */
    public void setNucleiDetector(CellDetector cellDetector){
        this.cell=cellDetector;
    }

    /**
     * set detector for spots
     * @param spotDetectors :{@link ArrayList} of {@link SpotDetector}
     */
    public void setSpotsDetectors(ArrayList<SpotDetector> spotDetectors){
        this.spots=spotDetectors;
    }

    /**
     * set resultstables to get measurements
     * @param finalResultsCellSpot : ResultsTable corresponding to measure in cell/cytoplasm or nuclei if no cell
     * @param finalResultsNuclei : ResultsTable corresponding to measure in nuclei if cell
     */
    public void setResultsTables(ResultsTable finalResultsCellSpot, ResultsTable finalResultsNuclei){
        this.finalResultsNuclei=finalResultsNuclei;
        this.finalResultsCellSpot=finalResultsCellSpot;
    }

    public void setSpotsTables(int index,ResultsTable spotsInNucleiTable, ResultsTable spotsInCellsTable, ResultsTable spotsInCytoplasmsTable){
        this.spotsInNucleiTable.set(index,spotsInNucleiTable);
        this.spotsInCellsTable.set(index,spotsInCellsTable);
        this.spotsInCytoplasmsTable.set(index, spotsInCytoplasmsTable);
        IJ.log("experiment set Spots tables at index "+index);
        if(spotsInNucleiTable!=null) IJ.log("spots in nuclei size:"+spotsInNucleiTable.size());
        if(spotsInCellsTable!=null) IJ.log("spots in nuclei size:"+spotsInCellsTable.size());
        if(spotsInCytoplasmsTable!=null) IJ.log("spots in nuclei size:"+spotsInCytoplasmsTable.size());
    }

    public void setSummaryTable(ResultsTable summary,boolean onlyPositive4Spots) {
        this.summary = summary;
        this.onlyPositive4Spots=onlyPositive4Spots;
    }

    /**
     *
     * @return name without channel specific information
     */
    public String getExperimentName() {
        return experimentName;
    }

    /**
     * Do segmentation and detection for channel concerned and aggregates the measurements done in ResultTable(s)
     * @return true if everything worked
     */
    public boolean run() {
        IJ.log("start experiment");
        Instant dateBegin = Instant.now(); /*get starting time*/
//        PREPARE NECESSARY IMAGES FOR MEASUREMENTS
//        --> Cells images
        if (cell !=null && !interrupt){
            IJ.log("Cell/Cytoplasm image: "+ cell.getImageTitle());
            cell.setMeasureCalibration(measureCalibration);
            if (!cell.prepare()) {
                interrupt=true;
                return false;
            }
        }
//        --> Nuclei images
        if (nuclei!=null && !interrupt){
            IJ.log("Nuclei image: "+nuclei.getImageTitle());
            nuclei.setMeasureCalibration(measureCalibration);
            if (!nuclei.prepare()) {
                interrupt=true;
                return false;
            }
        }
//        --> Spots images
        for (int i = 0; i < spots.size(); i++) {
            if (!interrupt && spots.get(i)!=null){
                SpotDetector spot = spots.get(i);
                IJ.log("Quantification channel "+(i+1)+ " image:" + spot.getImageTitle());
                spot.setMeasureCalibration(measureCalibration);
                if (!spot.prepare()) {
                    interrupt=true;
                    return false;
                }
            }
        }
//        Prepare cytoplasm and get the ROIs for spot detection
        Roi[] cellRois = null;
        Roi[] nucleiRois = null;
        Roi[] cytoplasmRois = null;
        boolean onlySpot = false;
        int numberOfObject;
        CytoDetector cytoDetector = null;
        if (cell!=null && !interrupt){
            cellRois = cell.getRoiArray();
            if (nuclei!=null){ /*Prepare cytoplasm et get new cellRois*/
                cytoDetector = cell.getCytoDetector();
                cytoDetector.setNucleiRois(nuclei.getRoiArray());
                cytoDetector.setMeasureCalibration(measureCalibration);
                cytoDetector.prepare();
                nucleiRois = cytoDetector.getAssociatedNucleiRois();
                cytoplasmRois = cytoDetector.getCytoRois();
                cellRois = cytoDetector.getCellRois();
                cell.setCellRois(cellRois);
            }
            numberOfObject = cellRois.length;
            IJ.log("(cell"+((nuclei!=null)?"/nuclei":"")+")number of objects: "+numberOfObject);
        }else { /*No cell images*/
            if (nuclei!=null && !interrupt){ /*But nuclei images*/
                nucleiRois = nuclei.getRoiArray();
                numberOfObject = nucleiRois.length;
                IJ.log("(nuclei"+")number of objects: "+numberOfObject);
            } else { /*Only spot images*/
                numberOfObject = 1;
                onlySpot = true;
                IJ.log("only spots");
            }
        }
        IJ.log("measurements launch");
        MeasureRois measureRois=new MeasureRois(nuclei,cell,cytoDetector,spots);
        measureRois.setSpotsTables(spotsInNucleiTable,spotsInCellsTable, spotsInCytoplasmsTable);
        measureRois.measureAll(finalResultsNuclei,finalResultsCellSpot, experimentName, measureCalibration);


        if(summary!=null) {
            if(finalResultsNuclei!=null && finalResultsCellSpot!=null) measureRois.summary(summary,finalResultsNuclei, finalResultsCellSpot,experimentName, onlyPositive4Spots);
            else if(finalResultsNuclei!=null) measureRois.summary(summary,finalResultsNuclei,experimentName);
            else if(finalResultsCellSpot!=null) measureRois.summary(summary,finalResultsCellSpot,experimentName);
        }
/*
        if (cell!=null && nuclei!=null && !interrupt){ //Get result table for all nuclei in addition to cell ResultTable
            Roi[] allNuclei = nuclei.getRoiArray();
            int[] association2Cell = cytoDetector.getAssociationCell2Nuclei();
            for (int nucleusID = 0; nucleusID < allNuclei.length; nucleusID++) {
                finalResultsNuclei.addValue("Name experiment", experimentName);
                finalResultsNuclei.addValue("Nucleus nr.", "" + (nucleusID + 1));
                finalResultsNuclei.addValue("Cell associated",""+association2Cell[nucleusID]);
                nuclei.measureEachNuclei(nucleusID, finalResultsNuclei,allNuclei[nucleusID]);
                for (int s=0;s<spots.size();s++) {
                    SpotDetector spot=spots.get(s);
                    if(spot!=null) {
                        IJ.log("channel "+s+" measure in nuclei");
                        spot.analysisPerRegion(nucleusID,allNuclei[nucleusID], finalResultsNuclei,"Nuclei",spotsInNucleiTable.get(s));
                        //IJ.log("analysis region nuclei spots exists"+(spotsInNucleiTable.get(s)!=null));
                    }
                }
                finalResultsNuclei.incrementCounter();
            }
            nuclei.setNucleiAssociatedRois(nucleiRois);
        }
        IJ.log("measurements");
//        Measurements for each cell or nuclei or image (in case of only spot)
        for (int cellID = 0; cellID < numberOfObject; cellID++) {
            if (!interrupt){
                finalResultsCellSpot.addValue("Name experiment", experimentName);
                finalResultsCellSpot.addValue("Cell nr.", "" + (cellID + 1));
                if (cell!=null){
                    cell.measureEachCell(cellID, finalResultsCellSpot);
                }
                if (nuclei!=null && cell==null){
                    nuclei.measureEachNuclei(cellID, finalResultsCellSpot,nucleiRois[cellID]);
                }
                if (cytoDetector!=null){
                    System.out.println("measure cyto: "+cellID+"/"+numberOfObject);
                    cytoDetector.measureEachCytoplasm(cellID, finalResultsCellSpot);
                }
                for (int s=0;s<spots.size();s++) {
                    SpotDetector spot=spots.get(s);
                    if(spot!=null) {
                        //IJ.log("measure spot "+spot.getSpotName()+ "     "+spot);
                        if (cellRois != null) {
                            //IJ.log("results channel "+s+" measure in cell");
                            spot.analysisPerRegion(cellID, cellRois[cellID], finalResultsCellSpot, "Cell", spotsInCellsTable.get(s));
                        }
                        if (nucleiRois != null && cellRois == null) {
                            //IJ.log("results channel "+s+" measure in nuclei");
                            spot.analysisPerRegion(cellID, nucleiRois[cellID], finalResultsCellSpot, "Nuclei", spotsInNucleiTable.get(s));
                            //IJ.log("analysis region nuclei spots exists"+(spotsInNucleiTable.get(s)!=null));
                        }
                        if (cytoplasmRois != null) {
                            //IJ.log("channel "+s+" measure in cytoplasm");
                            spot.analysisPerRegion(cellID, cytoplasmRois[cellID], finalResultsCellSpot, "Cytoplasm", spotsInCytoplasmsTable.get(s));
                        }if (onlySpot) spot.analysisPerRegion(cellID, null, finalResultsCellSpot, "Image",spotsInNucleiTable.get(s));
                    }
                }
                finalResultsCellSpot.incrementCounter();
            }
        }

 */
//        TIMING OF EXPERIENCE
        Instant dateEnd = Instant.now();
        long duration = Duration.between(dateBegin,dateEnd).toMillis();
        IJ.log("Experiment "+experimentName+" is done in :" +duration/1000+" seconds");
        return true;
    }

    public boolean previewNucleiSegmentation(){
        IJ.log("start previewNuclei");
        if(nuclei==null) {
            IJ.log("no nuclei defined!");
            return false;
        }
//        PREPARE NECESSARY IMAGES FOR MEASUREMENTS
//        --> Nuclei images
        IJ.log("Nuclei image: "+nuclei.getImageTitle());
        nuclei.setMeasureCalibration(measureCalibration);
        if (!nuclei.prepare()) {
            interrupt=true;
            return false;
        }

//        Prepare cytoplasm and get the ROIs for spot detection
        Roi[] nucleiRois = null;
        int numberOfObject;
        nucleiRois = nuclei.getRoiArray();
        numberOfObject = nucleiRois.length;
        IJ.log("(nuclei"+")number of objects: "+numberOfObject);

        return true;
    }

    public boolean previewCellSegmentation() {
//        PREPARE NECESSARY IMAGES FOR MEASUREMENTS
//        --> Cells images
        if(cell==null){
            IJ.log("no cells defined");
            return false;
        }

        IJ.log("Cell/Cytoplasm image: "+ cell.getImageTitle());
        cell.setMeasureCalibration(measureCalibration);

        if (!cell.prepare()) {
            interrupt=true;
            return false;
        }

//        Prepare cytoplasm and get the ROIs for spot detection
        Roi[] cellRois = null;
        int numberOfObject;

        cellRois = cell.getRoiArray();
        numberOfObject = cellRois.length;
        IJ.log("(cell"+((nuclei!=null)?"/nuclei":"")+")number of objects: "+numberOfObject);

        return true;
    }

    public boolean previewSpotDetection(int index){

//        --> Spots images
        SpotDetector spot = spots.get(index-1);
        IJ.log("Quantification channel "+(index)+ " image:" + spot.getImageTitle());

        IJ.log("macro: "+spot.getPreprocessingMacro());
        IJ.log("macro quantif: "+spot.getPreprocessingMacroQuantif());
        spot.setMeasureCalibration(measureCalibration);
        if (!spot.prepare()) {
            interrupt=true;
            return false;
        }
        spot.analysisPerRegion(0,null, finalResultsCellSpot,"Image",null);

        return true;
    }

    /**
     * interrupt process if error or user clicked on cancel button
     */
    public void interruptProcess() {
        interrupt = true;
    }
}
