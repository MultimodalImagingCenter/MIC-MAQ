package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.ExpandMask;
import fr.curie.micmaq.helpers.MeasureCalibration;
import fr.curie.micmaq.helpers.SummarizeResults;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.measurements.*;
import mcib3d.image3d.ImageHandler;
import mcib_plugins.Manager3D.ImportImage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


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
        //IJ.log("MeasureRois");
    }

    public void measureAll(ResultsTable finalResultsNuclei, ResultsTable finalResultsCellSpot, String experimentName, MeasureCalibration calibration){
        IJ.log("measureAll");
        Objects3DIntPopulation expandLabels=null;
        if(cells!=null && nuclei!=null){
            if(nuclei.getNumberOfNuclei()>0) prepareCytoMeasure(finalResultsNuclei,experimentName,calibration);
        }
        int numberOfObject = cells!=null ? cells.getNumberOfCells() : nuclei.getNumberOfNuclei();
        IJ.log("(cell"+((nuclei!=null)?"/nuclei":"")+")number of objects: "+numberOfObject);
        for (int cellID = 0; cellID < numberOfObject; cellID++) {
                finalResultsCellSpot.addValue("Name experiment", experimentName);
                finalResultsCellSpot.addValue("Cell ID", "" + (cellID + 1));
                if(cyto!=null) finalResultsCellSpot.addValue("Number of nuclei in Cell", cyto.getNumberOfNuclei(cellID));
                if (cells!=null){
                    Objects3DIntPopulation roi3Ds=cells.getRoi3D();
                    if(roi3Ds!=null) {
                        IJ.log("cell measure3D");
                        measure3D(cellID, finalResultsCellSpot, roi3Ds.getObjects3DInt().get(cellID), CELL, calibration);
                    }else {
                        IJ.log("cell measure2D");
                        Roi[] roiscell = cells.getRoiArray();
                        measure(cellID, finalResultsCellSpot, roiscell[cellID], CELL, calibration);
                    }
                }else if (nuclei!=null){
                    Objects3DIntPopulation roi3Ds=nuclei.getRoi3D();
                    if(roi3Ds!=null){
                        IJ.log("nuclei measure3D");
                        if(roi3Ds.getObjects3DInt().size()>cellID) {
                            measure3D(cellID, finalResultsCellSpot, roi3Ds.getObjects3DInt().get(cellID), NUCLEI, calibration);
                        }
                    }else {
                        IJ.log("nuclei measure2D");
                        Roi[] rois = nuclei.getRoiArray();
                        measure(cellID, finalResultsCellSpot, rois[cellID], NUCLEI, calibration);
                    }
                    if(nuclei.isExpand4Cells()){
                        if(nuclei.getRoi3D()!=null) {
                            IJ.log("expand nuclei measure3D");
                            ArrayList<Objects3DIntPopulation> rois3Dexpands= nuclei.expandNuclei3D(calibration);
                            if( rois3Dexpands.get(1).getNbObjects()>cellID){
                                measure3D(cellID, finalResultsCellSpot, rois3Dexpands.get(1).getObjects3DInt().get(cellID), NUCLEI_EXP_CELL, calibration);
                                measure3D(cellID, finalResultsCellSpot, rois3Dexpands.get(2).getObjects3DInt().get(cellID), NUCLEI_EXP_CYTO, calibration);

                            }
                        }else {
                            IJ.log("expand nuclei measure2D");
                            ArrayList<Roi[]> roisExpanded = nuclei.getExpandedRois();
                            if (roisExpanded.get(1).length > cellID) {
                                measure(cellID, finalResultsCellSpot, roisExpanded.get(1)[cellID], NUCLEI_EXP_CELL, calibration);
                                measure(cellID, finalResultsCellSpot, roisExpanded.get(2)[cellID], NUCLEI_EXP_CYTO, calibration);
                            }
                        }
                    }
                }
                if (cyto!=null){
                    if(cyto.is3D()){
                        Object3DInt[] rois = cyto.getCytoRois3D();
                        measure3D(cellID, finalResultsCellSpot, rois[cellID], CYTO, calibration);
                    }else {
                        Roi[] rois = cyto.getCytoRois();
                        measure(cellID, finalResultsCellSpot, rois[cellID], CYTO, calibration);
                    }
                }
                finalResultsCellSpot.incrementCounter();
                //nuclei.saveAll(nuclei.getAnalysisType());
        }
    }

    public boolean prepareCytoMeasure(ResultsTable finalResultsNuclei, String experimentName, MeasureCalibration calibration){
        Objects3DIntPopulation allnuclei3D=nuclei.getRoi3D();

        ResultsTable tmp = new ResultsTable();
        if(allnuclei3D!=null){
            int[] association2Cell = cyto.getAssociationCell2Nuclei();
            int[] association2CellTrue = cyto.getAssociationCell2NucleiTrue();
            int padding = ("" + allnuclei3D.getNbObjects()).length();
            for (int nucleusID = 0; nucleusID < allnuclei3D.getNbObjects(); nucleusID++) {
                tmp.addValue("Name experiment", experimentName);
                tmp.addValue("Nucleus ID", IJ.pad(nucleusID + 1, padding));
                tmp.addValue("Cell ID associated (detection)", IJ.pad(association2Cell[nucleusID], padding));
                tmp.addValue("Cell ID associated (validated)", IJ.pad(association2CellTrue[nucleusID], padding));
                measure3D(nucleusID, tmp, allnuclei3D.getObjects3DInt().get(nucleusID), NUCLEI, calibration);
                if (nucleusID < allnuclei3D.getNbObjects() - 1) tmp.incrementCounter();
                nuclei.setNucleiAssociatedRois3D(cyto.getAssociatedNucleiRois3D());
            }
        }else {
            Roi[] allNuclei = nuclei.getRoiArray();
            if(allNuclei==null||allNuclei.length==0) return false;
            int[] association2Cell = cyto.getAssociationCell2Nuclei();
            int[] association2CellTrue = cyto.getAssociationCell2NucleiTrue();
            int padding = ("" + allNuclei.length).length();
            for (int nucleusID = 0; nucleusID < allNuclei.length; nucleusID++) {
                tmp.addValue("Name experiment", experimentName);
                tmp.addValue("Nucleus ID", IJ.pad(nucleusID + 1, padding));
                tmp.addValue("Cell ID associated (detection)", IJ.pad(association2Cell[nucleusID], padding));
                tmp.addValue("Cell ID associated (validated)", IJ.pad(association2CellTrue[nucleusID], padding));
                measure(nucleusID, tmp, allNuclei[nucleusID], NUCLEI, calibration);
                if (nucleusID < allNuclei.length - 1) tmp.incrementCounter();
            }
            nuclei.setNucleiAssociatedRois(cyto.getAssociatedNucleiRois());
        }
        tmp.sort("Cell ID associated (validated)");
        String[] headings = tmp.getHeadings();
        for (int ind = 0; ind < tmp.getCounter(); ind++) {
            for (int col = 0; col < headings.length; col++) {
                String val = tmp.getStringValue(col, ind);
                try {
                    try {
                        int valI = Integer.parseInt(val);
                        finalResultsNuclei.addValue(headings[col], valI);
                    } catch (NumberFormatException nfeI) {
                        double valD = Double.parseDouble(val);
                        finalResultsNuclei.addValue(headings[col], valD);
                    }
                } catch (Exception e) {
                    finalResultsNuclei.addValue(headings[col], val);
                }
            }
            finalResultsNuclei.incrementCounter();
        }
        return true;
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
        System.out.println("############### measureCell "+measures+"\t "+(measures&Measurements.AREA)+" ##############");
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
        System.out.println("######### measureNuclei "+measures+"\t "+(measures&Measurements.AREA)+" ############");
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

    public void measure3D(int cellID, ResultsTable resultsTableFinal, Object3DInt measureRoi3D, String type, MeasureCalibration calib){
        rawMeasures.reset();
        if(type.equals(NUCLEI)){
            if(nuclei!=null) measure3DNuclei(cellID,resultsTableFinal,measureRoi3D,type,calib);
            if(cells!=null) measure3DCell(cellID,resultsTableFinal,measureRoi3D,type,calib);
        }else{
            if(cells!=null) measure3DCell(cellID,resultsTableFinal,measureRoi3D,type,calib);
            if(nuclei!=null) measure3DNuclei(cellID,resultsTableFinal,measureRoi3D,type,calib);
        }
        if(spots!=null){
            for (int s=0;s<spots.size();s++) {
                SpotDetector spot = spots.get(s);
                if (spot != null) {
                    measureSpot3D(spot,cellID,s,measureRoi3D,type,resultsTableFinal,calib);
                }
            }
        }
    }

    public void measure3DCell(int cellID, ResultsTable resultsTableFinal, Object3DInt measureRoi3D, String type, MeasureCalibration calib){
        IJ.log("measure3D "+type);
        IJ.log(measureRoi3D.getName());
        measureRoi3D.setVoxelSizeXY(calib.getPixelArea());
        measureRoi3D.setVoxelSizeZ(calib.getPixelsZ());
        measureRoi3D.setUnit(calib.getUnit());
        int measures=cells.getMeasurements();
        if(!type.equals(NUCLEI)) {
            String[] headingsMeasureMorpho = getValidMeasurements3DMorpho(measures);
            //measure morpho
            MeasureObject measureObjectMorpho = new MeasureObject(measureRoi3D);
            Double[] measurementsMorpho = measureObjectMorpho.measureList(headingsMeasureMorpho);
            //put in table
            setResults3D(measurementsMorpho, headingsMeasureMorpho, resultsTableFinal, type + cells.getNameChannel(), calib);
        }
        //IJ.log(Arrays.toString(measurements));
        String[] headingsMeasureIntensity = getValidMeasurements3DIntensity(measures);
        AtomicInteger ai = new AtomicInteger(0);
        MeasureObject measureObjectIntensity = new MeasureObject(measureRoi3D);
        Double[] measurementsIntensity = measureObjectIntensity.measureIntensityList(headingsMeasureIntensity,ImageHandler.wrap(cells.getImageToMeasure()));
        setResults3D(measurementsIntensity, headingsMeasureIntensity, resultsTableFinal, type + cells.getNameChannel(), calib);
    }
    public void measure3DNuclei(int cellID, ResultsTable resultsTableFinal, Object3DInt measureRoi3D, String type, MeasureCalibration calib){
        IJ.log("measure3D "+type);
        IJ.log(measureRoi3D.getName());
        measureRoi3D.setVoxelSizeXY(calib.getPixelArea());
        measureRoi3D.setVoxelSizeZ(calib.getPixelsZ());
        measureRoi3D.setUnit(calib.getUnit());
        int measures=nuclei.getMeasurements();
        if(!type.equals(CELL)&&!type.equals(CYTO)) {
            String[] headingsMeasureMorpho = getValidMeasurements3DMorpho(measures);
            //measure morpho
            MeasureObject measureObjectMorpho = new MeasureObject(measureRoi3D);
            Double[] measurementsMorpho = measureObjectMorpho.measureList(headingsMeasureMorpho);
            //put in table
            setResults3D(measurementsMorpho, headingsMeasureMorpho, resultsTableFinal, type + nuclei.getNameChannel(), calib);
        }
        //IJ.log(Arrays.toString(measurements));
        String[] headingsMeasureIntensity = getValidMeasurements3DIntensity(measures);
        AtomicInteger ai = new AtomicInteger(0);
        MeasureObject measureObjectIntensity = new MeasureObject(measureRoi3D);
        Double[] measurementsIntensity = measureObjectIntensity.measureIntensityList(headingsMeasureIntensity,ImageHandler.wrap(nuclei.getImageToMeasure()));
        setResults3D(measurementsIntensity, headingsMeasureIntensity, resultsTableFinal, type + nuclei.getNameChannel(), calib);
    }

    public  void measureSpot3D(SpotDetector spot,int cellID, int spotID, Object3DInt measureRoi3D, String type, ResultsTable resultsTableFinal, MeasureCalibration calib){
        //IJ.log("measure spot "+spot.getSpotName());
        spot.setMeasureCalibration(calib);
        if(type.equals(NUCLEI)) spot.analysisPerRegion(cellID,measureRoi3D,resultsTableFinal,type,spotsInNucleiTable.get(spotID));
        if(type.equals(CELL)||type.equals(NUCLEI_EXP_CELL)) spot.analysisPerRegion(cellID,measureRoi3D,resultsTableFinal,type ,spotsInCellsTable.get(spotID));
        if(type.equals(CYTO)||type.equals(NUCLEI_EXP_CYTO)) spot.analysisPerRegion(cellID,measureRoi3D,resultsTableFinal,type ,spotsInCytoplasmsTable.get(spotID));

    }

    public static String[] getValidMeasurements3DMorpho(int measurements){
        List<String> validMeasurements = new ArrayList<>();
        if((measurements&Measurements.AREA)!=0) {
            validMeasurements.add(MeasureVolume.VOLUME_PIX);
            validMeasurements.add(MeasureVolume.VOLUME_UNIT);
        }
        if((measurements&Measurements.PERIMETER)!=0) {
            validMeasurements.add(MeasureSurface.SURFACE_PIX);
            validMeasurements.add(MeasureSurface.SURFACE_UNIT);
            validMeasurements.add(MeasureSurface.SURFACE_CORRECTED);
            validMeasurements.add(MeasureSurface.SURFACE_NB_VOXELS);
        }
        if((measurements&Measurements.RECT)!=0){
            validMeasurements.add(MeasureBoundingBox.XMIN);
            validMeasurements.add(MeasureBoundingBox.YMIN);
            validMeasurements.add(MeasureBoundingBox.ZMIN);
            validMeasurements.add(MeasureBoundingBox.XMAX);
            validMeasurements.add(MeasureBoundingBox.YMAX);
            validMeasurements.add(MeasureBoundingBox.ZMAX);
        }
        if((measurements&Measurements.CENTROID)!=0){
            validMeasurements.add(MeasureCentroid.CX_PIX);
            validMeasurements.add(MeasureCentroid.CY_PIX);
            validMeasurements.add(MeasureCentroid.CZ_PIX);
        }
        if((measurements&Measurements.CENTER_OF_MASS)!=0){
            validMeasurements.add(MeasureCenterOfMass.MASS_CENTER_X_PIX);
            validMeasurements.add(MeasureCenterOfMass.MASS_CENTER_Y_PIX);
            validMeasurements.add(MeasureCenterOfMass.MASS_CENTER_Z_PIX);
        }
        if((measurements&Measurements.AREA_FRACTION)!=0){
            //nothing available in 3DImageJSuite?
        }
        if((measurements&Measurements.ELLIPSE)!=0){
            validMeasurements.add(MeasureEllipsoid.ELL_MAJOR_RADIUS_UNIT);
            validMeasurements.add(MeasureEllipsoid.ELL_ELONGATION);
            validMeasurements.add(MeasureEllipsoid.ELL_FLATNESS);
            validMeasurements.add(MeasureEllipsoid.ELL_VOL_UNIT);
            validMeasurements.add(MeasureEllipsoid.ELL_SPARENESS);
        }
        if((measurements&Measurements.SHAPE_DESCRIPTORS)!=0) {
            validMeasurements.add(MeasureCompactness.COMP_PIX);
            validMeasurements.add(MeasureCompactness.COMP_UNIT);
            validMeasurements.add(MeasureCompactness.COMP_CORRECTED);
            validMeasurements.add(MeasureCompactness.COMP_DISCRETE);
            validMeasurements.add(MeasureCompactness.SPHER_PIX);
            validMeasurements.add(MeasureCompactness.SPHER_UNIT);
            validMeasurements.add(MeasureCompactness.SPHER_CORRECTED);
            validMeasurements.add(MeasureCompactness.SPHER_DISCRETE);
        }
        if((measurements&Measurements.FERET)!=0) {
            validMeasurements.add(MeasureFeret.FERET_PIX);
            validMeasurements.add(MeasureFeret.FERET_UNIT);
        }
        return validMeasurements.toArray(new String[0]);
    }


    public static String[] getValidMeasurements3DIntensity(int measurements){
        List<String> validMeasurements = new ArrayList<>();
        if((measurements&Measurements.INTEGRATED_DENSITY)!=0) {
            validMeasurements.add(MeasureIntensity.INTENSITY_SUM);
        }
        if((measurements&Measurements.MEAN)!=0) {
            validMeasurements.add(MeasureIntensity.INTENSITY_AVG);
        }
        if((measurements&Measurements.MEDIAN)!=0){
            validMeasurements.add(MeasureIntensityHist.INTENSITY_MEDIAN);
        }
        if((measurements&Measurements.STD_DEV)!=0){
            validMeasurements.add(MeasureIntensity.INTENSITY_SD);
        }
        if((measurements&Measurements.MIN_MAX)!=0){
            validMeasurements.add(MeasureIntensity.INTENSITY_MIN);
            validMeasurements.add(MeasureIntensity.INTENSITY_MAX);
        }
        if((measurements&Measurements.MODE)!=0){
            validMeasurements.add(MeasureIntensityHist.INTENSITY_MODE);
            validMeasurements.add(MeasureIntensityHist.INTENSITY_MODE_NONZERO);
        }
        if((measurements&Measurements.SKEWNESS)!=0){
            //nothing available in 3DImageJSuite?
        }
        if((measurements&Measurements.KURTOSIS)!=0) {
            //nothing available in 3DImageJSuite?
        }
        return validMeasurements.toArray(new String[0]);
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

    public static void setResults3D(Double[] rawMeasures, String[] headings,ResultsTable customMeasures,  String preNameColumn, MeasureCalibration calib) {
        for (int i = 0; i < headings.length; i++) {
            if (headings[i].equals("Area")) {
                customMeasures.addValue(preNameColumn + " " + headings[i] + " (pixel)", rawMeasures[i]);
                customMeasures.addValue(preNameColumn + " " + headings[i] + " (" + calib.getUnit() + ")", rawMeasures[i] * calib.getPixelArea());
            }
            customMeasures.addValue(preNameColumn + " "+headings[i], rawMeasures[i]);
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
