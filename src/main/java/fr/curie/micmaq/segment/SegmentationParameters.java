package fr.curie.micmaq.segment;

import fr.curie.micmaq.config.MeasureValue;
import fr.curie.micmaq.config.MeasurementsParameters;
import fr.curie.micmaq.gui.Measures;
import ij.measure.Measurements;

import java.io.File;

public class SegmentationParameters {
    static final public  int CELLPOSE=1;
    static final public int THRESHOLDING=2;
    static final public int STARDIST=3;
    static final public int MACRO_SEGMENTATION=4;

    static final public int PROJECTION_MAX=0;
    static final public int PROJECTION_STDDEV=1;
    static final public int PROJECTION_SUM=2;

    int method=THRESHOLDING;

    //thresholding parameters
    String thresholdMethod="Default";
    boolean thresholdingWatershed=false;

    //CELLPOSE parameters
    String cellposeModel="cyto2";
    int cellposeDiameter;
    double cellposeCellproba_trheshold;
    File pathToModel;

    //Stardist parameters
    String stardistModel="Versatile (fluorescent nuclei)";
    double stardistPercentileBottom=0.0;
    double stardistPercentileTop=100.0;
    double stardistProbThresh=0.5;
    double stardistNmsThresh=0.0;
    String stardistModelFile=null;
    double stardistScale=1.0;

    //other macro segmentation segmentation
    String segmentationMacro="";
    boolean macroOutputRoiManager=false;
    boolean macroOutputImage=false;

    //cytoplasm extraction parameters
    double minOverlap=-1;
    double minCytoSize=-1;

    //preprocessing parameters
    String preprocessMacro=null;
    String preprocessMacroQuantif=null;
    boolean Zproject=false;

    boolean projectInMacro=false;
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

    int tileSize=-1;
    int tileOverlap=-1;

    int ExpansionRadius = -1;

    public static SegmentationParameters createCellpose(String model, int diameter, double cellproba_threshold){
        SegmentationParameters param=new SegmentationParameters();
        param.setCellposeDiameter(diameter);
        param.setCellposeCellproba_trheshold(cellproba_threshold);
        param.setCellposeModel(model);

        param.setMethod(CELLPOSE);
        MeasureValue tmp=new MeasureValue(true);
        tmp.setMeasure(Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY); /*Default measurements*/
        param.setMeasurements(tmp);
        return param;
    }

    public static SegmentationParameters createStarDist(String model,
                                                        double stardistPercentileBottom,
                                                        double stardistPercentileTop,
                                                        double stardistProbThresh,
                                                        double stardistNmsThresh,
                                                        String stardistModelFile,
                                                        double stardistScale){
        SegmentationParameters param=new SegmentationParameters();
        param.setStardistModel(model);
        param.setStardistPercentileBottom(stardistPercentileBottom);
        param.setStardistPercentileTop(stardistPercentileTop);
        param.setStardistProbThresh(stardistProbThresh);
        param.setStardistNmsThresh(stardistNmsThresh);
        param.setStardistScale(stardistScale);
        if(stardistModelFile!=null) param.setStardistModelFile(stardistModelFile);
        param.setMethod(STARDIST);
        MeasureValue tmp=new MeasureValue(true);
        tmp.setMeasure(Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY); /*Default measurements*/
        param.setMeasurements(tmp);
        return param;
    }

    public static SegmentationParameters createThresholding(String method, boolean watershed){
        SegmentationParameters param=new SegmentationParameters();
        param.setMethod(THRESHOLDING);
        param.setThresholdMethod(method);
        param.setThresholdingWatershed(watershed);
        MeasureValue tmp=new MeasureValue(true);
        tmp.setMeasure(Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY); /*Default measurements*/
        param.setMeasurements(tmp);
        return param;
    }

    public static SegmentationParameters createMacroSegmentation(String macro, boolean ouputRoiManager, boolean outputImage){
        SegmentationParameters param=new SegmentationParameters();
        param.setMethod(MACRO_SEGMENTATION);
        param.setSegmentationMacro(macro);
        param.setMacroOutputRoiManager(ouputRoiManager);
        param.setMacroOutputImage(outputImage);
        MeasureValue tmp=new MeasureValue(true);
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

    public double getCellposeCellproba_trheshold() {
        return cellposeCellproba_trheshold;
    }

    public void setCellposeCellproba_trheshold(double cellposeCellproba_trheshold) {
        this.cellposeCellproba_trheshold = cellposeCellproba_trheshold;
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
            case PROJECTION_SUM: return "Sum Slices";
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

    public String getStardistModel() {
        return stardistModel;
    }

    public void setStardistModel(String stardistModel) {
        this.stardistModel = stardistModel;
    }

    public double getStardistPercentileBottom() {
        return stardistPercentileBottom;
    }

    public void setStardistPercentileBottom(double stardistPercentileBottom) {
        this.stardistPercentileBottom = stardistPercentileBottom;
    }

    public double getStardistPercentileTop() {
        return stardistPercentileTop;
    }

    public void setStardistPercentileTop(double stardistPercentileTop) {
        this.stardistPercentileTop = stardistPercentileTop;
    }

    public double getStardistProbThresh() {
        return stardistProbThresh;
    }

    public void setStardistProbThresh(double stardistProbThresh) {
        this.stardistProbThresh = stardistProbThresh;
    }

    public double getStardistNmsThresh() {
        return stardistNmsThresh;
    }

    public void setStardistNmsThresh(double stardistNmsThresh) {
        this.stardistNmsThresh = stardistNmsThresh;
    }

    public String getStardistModelFile() {
        return stardistModelFile;
    }

    public void setStardistModelFile(String stardistModelFile) {
        this.stardistModelFile = stardistModelFile;
    }

    public double getStardistScale() {
        return stardistScale;
    }

    public void setStardistScale(double stardistScale) {
        this.stardistScale = stardistScale;
    }

    public int getTileSize() {
        return tileSize;
    }

    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
    }

    public int getTileOverlap() {
        return tileOverlap;
    }

    public void setTileOverlap(int tileOverlap) {
        this.tileOverlap = tileOverlap;
    }

    public int getExpansionRadius() {
        return ExpansionRadius;
    }

    public void setExpansionRadius(int expansionRadius) {
        ExpansionRadius = expansionRadius;
    }

    public boolean isProjectInMacro() {
        return projectInMacro;
    }

    public void setProjectInMacro(boolean projectInMacro) {
        this.projectInMacro = projectInMacro;
    }

    public String getSegmentationMacro() {
        return segmentationMacro;
    }

    public void setSegmentationMacro(String segmentationMacro) {
        this.segmentationMacro = segmentationMacro;
    }

    public boolean isMacroOutputRoiManager() {
        return macroOutputRoiManager;
    }

    public void setMacroOutputRoiManager(boolean macroOutputRoiManager) {
        this.macroOutputRoiManager = macroOutputRoiManager;
    }

    public boolean isMacroOutputImage() {
        return macroOutputImage;
    }

    public void setMacroOutputImage(boolean macroOutputImage) {
        this.macroOutputImage = macroOutputImage;
    }
}
