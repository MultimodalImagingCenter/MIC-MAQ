package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;

import java.util.ArrayList;

import static ij.IJ.d2s;

public class MeasureRois {
    public static String NUCLEI="Nuclei";
    public static String CELL="Cell";
    public static String CYTO="Cyto";

    int intensityMeasures = Measurements.MEAN+Measurements.INTEGRATED_DENSITY+Measurements.MEDIAN+Measurements.MIN_MAX+Measurements.STD_DEV+Measurements.KURTOSIS+Measurements.SKEWNESS+Measurements.MODE;
    NucleiDetector nuclei;
    CellDetector cells;
    CytoDetector cyto;

    ArrayList<SpotDetector> spots;

    ResultsTable rawMeasures;
    Analyzer analyzer;


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
            for (int nucleusID = 0; nucleusID < allNuclei.length; nucleusID++) {
                finalResultsNuclei.addValue("Name experiment", experimentName);
                finalResultsNuclei.addValue("Nucleus nr.", "" + (nucleusID + 1));
                finalResultsNuclei.addValue("Cell associated",""+association2Cell[nucleusID]);
                measure(nucleusID, finalResultsNuclei,allNuclei[nucleusID],NUCLEI,calibration);
                finalResultsNuclei.incrementCounter();
            }

            nuclei.setNucleiAssociatedRois(cyto.getAssociatedNucleiRois());
        }
        int numberOfObject = cells!=null ? cells.getRoiArray().length : nuclei.getRoiArray().length;
        for (int cellID = 0; cellID < numberOfObject; cellID++) {
                finalResultsCellSpot.addValue("Name experiment", experimentName);
                finalResultsCellSpot.addValue("Cell nr.", "" + (cellID + 1));
                if (cells!=null){
                    Roi[] roiscell= cells.getRoiArray();
                    measure(cellID, finalResultsCellSpot,roiscell[cellID],CELL, calibration);
                }
                if (nuclei!=null && cells==null){
                    Roi[] rois= nuclei.getRoiArray();
                    measure(cellID, finalResultsCellSpot,rois[cellID],NUCLEI, calibration);
                }
                if (cyto!=null){
                    Roi[] rois= cyto.getCytoRois();
                    measure(cellID, finalResultsCellSpot,rois[cellID],CYTO, calibration);
                }
                finalResultsCellSpot.incrementCounter();

        }
    }

    public void measure(int nb, ResultsTable resultsTableFinal, Roi measureRoi, String type, MeasureCalibration calib){
        rawMeasures.reset();
        if(type.equals(NUCLEI)){
            if(nuclei!=null) measureNuclei(measureRoi,type,resultsTableFinal,calib);
            if(cells!=null) measureCell(measureRoi,type,resultsTableFinal,calib);
        }else{
            if(cells!=null) measureCell(measureRoi,type,resultsTableFinal,calib);
            if(nuclei!=null) measureNuclei(measureRoi,type,resultsTableFinal,calib);
        }
        if(spots!=null){
            for (SpotDetector spot : spots) {
                if (spot != null) {
                    measureSpot(spot,measureRoi,type,resultsTableFinal,calib);
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
        setResultsAndRename(rawMeasures,resultsTableFinal,type+cells.getNameChannel(),calib);
    }

    public  void measureNuclei(Roi measureRoi, String type, ResultsTable resultsTableFinal, MeasureCalibration calib){
        ImagePlus tmp=nuclei.getImageToMeasure();
        tmp.setRoi(measureRoi);
        int measures=nuclei.getMeasurements();
        if(!type.equals(NUCLEI)) measures= measures&intensityMeasures;
        //IJ.log("measure nuclei :"+type+" : "+measures+" ("+cells.getMeasurements()+")");
        rawMeasures.reset();
        analyzer = new Analyzer(tmp, measures, rawMeasures);
        analyzer.measure();
        setResultsAndRename(rawMeasures,resultsTableFinal,type+nuclei.getNameChannel(),calib);
    }
    public  void measureSpot(SpotDetector spot,Roi measureRoi, String type, ResultsTable resultsTableFinal, MeasureCalibration calib){
        ImagePlus tmp = spot.getImageToMeasure();
        tmp.setRoi(measureRoi);
        int measures = spot.getMeasurements();
        rawMeasures.reset();
        analyzer = new Analyzer(tmp, measures, rawMeasures);
        analyzer.measure();
        setResultsAndRename(rawMeasures, resultsTableFinal, type + spot.getNameChannel(), calib);
    }

    public void setResultsAndRename(ResultsTable rawMeasures, ResultsTable customMeasures,  String preNameColumn, MeasureCalibration calib) {
        for (String measure : rawMeasures.getHeadings()) {
            if (measure.equals("Area")) {
                customMeasures.addValue(preNameColumn + " " + measure + " (pixel)", d2s(rawMeasures.getValue(measure, 0)));
                customMeasures.addValue(preNameColumn + " " + measure + " (" + calib.getUnit() + ")", d2s(rawMeasures.getValue("Area", 0) * calib.getPixelArea()));

            } else if (!measure.equals("IntDen")) {
                customMeasures.addValue(preNameColumn + " " + measure, d2s(rawMeasures.getValue(measure, 0)));
            }
        }
    }
}
