package fr.curie.micmaq.segment;

import fr.curie.micmaq.config.MeasureValue;
import fr.curie.micmaq.config.MeasurementsParameters;
import fr.curie.micmaq.gui.Measures;
import ij.measure.Measurements;

import java.io.File;

public class SegmentationParameters {
    static final public  int CELLPOSE=1;
    static final public int THRESHOLDING=2;

    static final public int PROJECTION_MAX=0;
    static final public int PROJECTION_STDDEV=1;

    int method=THRESHOLDING;

    //thresholding parameters
    String thresholdMethod="Default";
    boolean thresholdingWatershed=false;

    //CELLPOSE parameters
    String cellposeModel="cyto2";
    int cellposeDiameter;
    File pathToModel;

    //cytoplasm extraction parameters
    double minOverlap=-1;
    double minCytoSize=-1;

    //preprocessing parameters
    String preprocessMacro=null;
    String preprocessMacroQuantif=null;
    boolean Zproject=false;
    int projectionMethod=PROJECTION_MAX;
    int projectionSliceMin=-1;
    int projectionSliceMax=-1;

    //global parameters on found objects
    boolean excludeOnEdge=true;
    double minSize=-1;
    boolean userValidation=false;
    boolean saveROIs=false;
    boolean saveMasks=false;
    MeasureValue measurements;

    public static SegmentationParameters createCellpose(String model, int diameter){
        SegmentationParameters param=new SegmentationParameters();
        param.setCellposeDiameter(diameter);
        param.setCellposeModel(model);

        param.setMethod(CELLPOSE);
        MeasureValue tmp=new MeasureValue();
        tmp.setMeasure(Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY); /*Default measurements*/
        param.setMeasurements(tmp);
        return param;
    }

    public static SegmentationParameters createThresholding(String method, boolean watershed){
        SegmentationParameters param=new SegmentationParameters();
        param.setMethod(THRESHOLDING);
        param.setThresholdMethod(method);
        param.setThresholdingWatershed(watershed);
        MeasureValue tmp=new MeasureValue();
        tmp.setMeasure(Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY); /*Default measurements*/
        param.setMeasurements(tmp);
        return param;
    }

    public SegmentationParameters setProjection(int method){
        Zproject=true;
        projectionMethod=method;
        return this;
    }

    public SegmentationParameters setProjection(int method, int slicestart, int sliceend){
        Zproject=true;
        projectionMethod=method;
        if(slicestart>0) projectionSliceMin=slicestart;
        if(sliceend>0) projectionSliceMax=sliceend;
        return this;
    }

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public String getThresholdMethod() {
        return thresholdMethod;
    }

    public void setThresholdMethod(String thresholdMethod) {
        this.thresholdMethod = thresholdMethod;
    }

    public boolean isThresholdingWatershed() {
        return thresholdingWatershed;
    }

    public void setThresholdingWatershed(boolean thresholdingWatershed) {
        this.thresholdingWatershed = thresholdingWatershed;
    }

    public String getCellposeModel() {
        return cellposeModel;
    }

    public void setCellposeModel(String cellposeModel) {
        this.cellposeModel = cellposeModel;
    }

    public int getCellposeDiameter() {
        return cellposeDiameter;
    }

    public void setCellposeDiameter(int cellposeDiameter) {
        this.cellposeDiameter = cellposeDiameter;
    }

    public String getPreprocessMacro() {
        return preprocessMacro;
    }

    public void setPreprocessMacro(String preprocessMacro) {
        this.preprocessMacro = preprocessMacro;
    }

    public String getPreprocessMacroQuantif() {
        return preprocessMacroQuantif;
    }

    public void setPreprocessMacroQuantif(String preprocessMacroQuantif) {
        this.preprocessMacroQuantif = preprocessMacroQuantif;
    }

    public boolean isZproject() {
        return Zproject;
    }


    public int getProjectionMethod() {
        return projectionMethod;
    }

    public String getProjectionMethodAsString() {
        switch (projectionMethod){
            default:
            case PROJECTION_MAX: return "Maximum projection";
            case PROJECTION_STDDEV: return "Standard deviation projection";

        }
    }


    public int getProjectionSliceMin() {
        return projectionSliceMin;
    }

    public void setProjectionSliceMin(int projectionSliceMin) {
        this.projectionSliceMin = projectionSliceMin;
    }

    public int getProjectionSliceMax() {
        return projectionSliceMax;
    }

    public void setProjectionSliceMax(int projectionSliceMax) {
        this.projectionSliceMax = projectionSliceMax;
    }

    public boolean isExcludeOnEdge() {
        return excludeOnEdge;
    }

    public void setExcludeOnEdge(boolean excludeOnEdge) {
        this.excludeOnEdge = excludeOnEdge;
    }

    public double getMinSize() {
        return minSize;
    }

    public void setMinSize(double minSize) {
        this.minSize = minSize;
    }

    public MeasureValue getMeasurements(){
        return measurements;
    }

    public void setMeasurements(MeasureValue measurements) {
        this.measurements = measurements;
    }

    public File getPathToModel() {
        return pathToModel;
    }

    public void setPathToModel(File pathToModel) {
        this.pathToModel = pathToModel;
    }

    public boolean isUserValidation() {
        return userValidation;
    }

    public void setUserValidation(boolean userValidation) {
        this.userValidation = userValidation;
    }

    public boolean isSaveROIs() {
        return saveROIs;
    }

    public void setSaveROIs(boolean saveROIs) {
        this.saveROIs = saveROIs;
    }

    public boolean isSaveMasks() {
        return saveMasks;
    }

    public void setSaveMasks(boolean saveMasks) {
        this.saveMasks = saveMasks;
    }

    public void setCytoplasmParameters(double minOverlap, double minCytoSize){
        this.minOverlap=minOverlap;
        this.minCytoSize=minCytoSize;
    }

    public double getMinOverlap() {
        return minOverlap;
    }

    public double getMinCytoSize() {
        return minCytoSize;
    }
}
