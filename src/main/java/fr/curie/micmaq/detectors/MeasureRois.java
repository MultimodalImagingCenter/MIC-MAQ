package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.MeasureCalibration;
import fr.curie.micmaq.helpers.SummarizeResults;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;


public class MeasureRois {
    public static String NUCLEI="Nuclei";
    public static String CELL="Cell";
    public static String CYTO="Cyto";
    public static String NUCLEI_EXP_CELL="Nuclei_expanded_cell";
    public static String NUCLEI_EXP_CYTO="Nuclei_expanded_cyto";

    int intensityMeasures = Measurements.MEAN+Measurements.INTEGRATED_DENSITY+Measurements.MEDIAN+Measurements.MIN_MAX+Measurements.STD_DEV+Measurements.KURTOSIS+Measurements.SKEWNESS+Measurements.MODE;
    NucleiDetector nuclei;
    CellDetector cells;
    CytoDetector cyto;

    ArrayList<SpotDetector> spots;

    ResultsTable rawMeasures;
    Analyzer analyzer;

    private ArrayList<ResultsTable> spotsInNucleiTable;
    private ArrayList<ResultsTable> spotsInCellsTable;
    private ArrayList<ResultsTable> spotsInCytoplasmsTable;


    public MeasureRois(NucleiDetector nuclei, CellDetector cells, CytoDetector cyto, ArrayList<SpotDetector> spots) {
        this.nuclei = nuclei;
        this.cells = cells;
        this.cyto = cyto;
        this.spots = spots;

        rawMeasures = new ResultsTable();
    }

    public void measureAll(ResultsTable finalResultsNuclei, ResultsTable finalResultsCellSpot, String experimentName, MeasureCalibration calibration){

        if(cells!=null && nuclei!=null){
            Roi[] allNuclei = nuclei.getRoiArray();
            int[] association2Cell = cyto.getAssociationCell2Nuclei();
            int[] association2CellTrue = cyto.getAssociationCell2NucleiTrue();
            ResultsTable tmp =new ResultsTable();
            int padding=(""+allNuclei.length).length();
            for (int nucleusID = 0; nucleusID < allNuclei.length; nucleusID++) {
                tmp.addValue("Name experiment", experimentName);
                tmp.addValue("Nucleus ID",  IJ.pad(nucleusID + 1,padding));
                tmp.addValue("Cell ID associated (detection)",IJ.pad(association2Cell[nucleusID],padding));
                tmp.addValue("Cell ID associated (validated)",IJ.pad(association2CellTrue[nucleusID],padding));
                measure(nucleusID, tmp,allNuclei[nucleusID],NUCLEI,calibration);
                if(nucleusID< allNuclei.length-1) tmp.incrementCounter();
            }

            tmp.sort("Cell ID associated (validated)");
            String[] headings= tmp.getHeadings();
            for (int ind=0;ind<tmp.getCounter();ind++){
                for(int col=0;col<headings.length;col++) {
                    String val=tmp.getStringValue(col,ind);
                    try{
                        try{
                            int valI = Integer.parseInt(val);
                            finalResultsNuclei.addValue(headings[col],valI);
                        }catch (NumberFormatException nfeI) {
                            double valD = Double.parseDouble(val);
                            finalResultsNuclei.addValue(headings[col], valD);
                        }
                    }catch (Exception e){
                        finalResultsNuclei.addValue(headings[col],val);
                    }
                }
                finalResultsNuclei.incrementCounter();
            }

            nuclei.setNucleiAssociatedRois(cyto.getAssociatedNucleiRois());
        }
        int numberOfObject = cells!=null ? cells.getRoiArray().length : nuclei.getRoiArray().length;
        for (int cellID = 0; cellID < numberOfObject; cellID++) {
                finalResultsCellSpot.addValue("Name experiment", experimentName);
                finalResultsCellSpot.addValue("Cell ID", "" + (cellID + 1));
                if(cyto!=null) finalResultsCellSpot.addValue("Number of nuclei in Cell", cyto.getNumberOfNuclei(cellID));
                if (cells!=null){
                    Roi[] roiscell= cells.getRoiArray();
                    measure(cellID, finalResultsCellSpot,roiscell[cellID],CELL, calibration);
                }else if (nuclei!=null){
                    Roi[] rois= nuclei.getRoiArray();
                    measure(cellID, finalResultsCellSpot,rois[cellID],NUCLEI, calibration);
                    if(nuclei.isExpand4Cells()){
                        ArrayList<Roi[]> roisExpanded= nuclei.getExpandedRois();
                        measure(cellID, finalResultsCellSpot,roisExpanded.get(1)[cellID],NUCLEI_EXP_CELL, calibration);
                        measure(cellID, finalResultsCellSpot,roisExpanded.get(2)[cellID],NUCLEI_EXP_CYTO, calibration);
                    }
                }
                if (cyto!=null){
                    Roi[] rois= cyto.getCytoRois();
                    measure(cellID, finalResultsCellSpot,rois[cellID],CYTO, calibration);
                }
                finalResultsCellSpot.incrementCounter();

        }
    }

    public void measure(int cellID, ResultsTable resultsTableFinal, Roi measureRoi, String type, MeasureCalibration calib){
        rawMeasures.reset();
        if(type.equals(NUCLEI)){
            if(nuclei!=null) measureNuclei(measureRoi,type,resultsTableFinal,calib);
            if(cells!=null) measureCell(measureRoi,type,resultsTableFinal,calib);
        }else{
            if(cells!=null) measureCell(measureRoi,type,resultsTableFinal,calib);
            if(nuclei!=null) measureNuclei(measureRoi,type,resultsTableFinal,calib);
        }
        if(spots!=null){
            for (int s=0;s<spots.size();s++) {
                SpotDetector spot = spots.get(s);
                if (spot != null) {
                    measureSpot(spot,cellID,s,measureRoi,type,resultsTableFinal,calib);
                }
            }
        }
    }
    public void measureCell(Roi measureRoi, String type, ResultsTable resultsTableFinal, MeasureCalibration calib){
        ImagePlus tmp=cells.getImageToMeasure();
        tmp.setRoi(measureRoi);
        int measures=cells.getMeasurements();
        if(type.equals(NUCLEI)) measures= measures&intensityMeasures;
        //IJ.log("measure cell :"+type+" : "+measures+" ("+cells.getMeasurements()+")");
        rawMeasures.reset();
        analyzer = new Analyzer(tmp, measures, rawMeasures);
        analyzer.measure();
        setResultsAndRename(rawMeasures,resultsTableFinal,type + cells.getNameChannel(),calib);
    }

    public  void measureNuclei(Roi measureRoi, String type, ResultsTable resultsTableFinal, MeasureCalibration calib){
        ImagePlus tmp=nuclei.getImageToMeasure();
        tmp.setRoi(measureRoi);
        int measures=nuclei.getMeasurements();
        if(type.equals(CELL)||type.equals(CYTO)) measures= measures&intensityMeasures;
        //IJ.log("measure nuclei :"+type+" : "+measures+" ("+cells.getMeasurements()+")");
        rawMeasures.reset();
        analyzer = new Analyzer(tmp, measures, rawMeasures);
        analyzer.measure();
        setResultsAndRename(rawMeasures,resultsTableFinal,type + nuclei.getNameChannel(),calib);
    }
    public  void measureSpot(SpotDetector spot,int cellID, int spotID, Roi measureRoi, String type, ResultsTable resultsTableFinal, MeasureCalibration calib){
        //IJ.log("measure spot "+spot.getSpotName());
        spot.setMeasureCalibration(calib);
        if(type.equals(NUCLEI)) spot.analysisPerRegion(cellID,measureRoi,resultsTableFinal,type,spotsInNucleiTable.get(spotID));
        if(type.equals(CELL)||type.equals(NUCLEI_EXP_CELL)) spot.analysisPerRegion(cellID,measureRoi,resultsTableFinal,type ,spotsInCellsTable.get(spotID));
        if(type.equals(CYTO)||type.equals(NUCLEI_EXP_CYTO)) spot.analysisPerRegion(cellID,measureRoi,resultsTableFinal,type ,spotsInCytoplasmsTable.get(spotID));
        /*ImagePlus tmp = spot.getImageToMeasure();
        tmp.setRoi(measureRoi);
        int measures = spot.getMeasurements();
        rawMeasures.reset();
        analyzer = new Analyzer(tmp, measures, rawMeasures);
        analyzer.measure();
        setResultsAndRename(rawMeasures, resultsTableFinal, type + spot.getNameChannel(), calib);*/
    }

    public void summary(ResultsTable summaryTable, ResultsTable Table, String experimentName){
        System.out.println("summary "+experimentName);
        SummarizeResults.summarize(summaryTable,Table,0);
    }

    public void summary(ResultsTable summaryTable, ResultsTable table1, ResultsTable table2, String experimentName,boolean onlyPositive4spots){
        System.out.println("summary "+experimentName);
        SummarizeResults.summarize(summaryTable,table1, 0, table2,0,onlyPositive4spots);
    }

    public void setResultsAndRename(ResultsTable rawMeasures, ResultsTable customMeasures,  String preNameColumn, MeasureCalibration calib) {
        for (String measure : rawMeasures.getHeadings()) {
            if (measure.equals("Area")) {
                customMeasures.addValue(preNameColumn + " " + measure + " (pixel)", rawMeasures.getValue(measure, 0));
                customMeasures.addValue(preNameColumn + " " + measure + " (" + calib.getUnit() + ")", rawMeasures.getValue("Area", 0) * calib.getPixelArea());

            } else if (!measure.equals("IntDen")) {
                customMeasures.addValue(preNameColumn + " " + measure, rawMeasures.getValue(measure, 0));
            }
        }
    }

    private double digit(double value, int digits){
        BigDecimal bigDecimal = BigDecimal.valueOf(value);
        return bigDecimal.setScale(digits, RoundingMode.HALF_UP).doubleValue();
    }



    public ArrayList<ResultsTable> getSpotsInNucleiTable() {
        return spotsInNucleiTable;
    }

    public ArrayList<ResultsTable> getSpotsInCellsTable() {
        return spotsInCellsTable;
    }

    public ArrayList<ResultsTable> getSpotsInCytoplasmsTable() {
        return spotsInCytoplasmsTable;
    }

    public void setSpotsTables(ArrayList<ResultsTable> spotsInNucleiTable, ArrayList<ResultsTable> spotsInCellsTable, ArrayList<ResultsTable> spotsInCytoplasmsTable) {
        this.spotsInNucleiTable = spotsInNucleiTable;
        this.spotsInCellsTable = spotsInCellsTable;
        this.spotsInCytoplasmsTable = spotsInCytoplasmsTable;
    }


}
