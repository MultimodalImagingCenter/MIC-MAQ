package fr.curie.micmaq.config;

import fr.curie.micmaq.detectors.SpotDetector;
import fr.curie.micmaq.segment.SegmentationParameters;
import ij.measure.Measurements;

public class MeasureValue {

    static final public int PROJECTION_MAX=0;
    static final public int PROJECTION_STDDEV=1;

    static final public int PROJECTION_SUM=2;

    public int measure;
    public String name;

    protected boolean spotThreshold=false;
    protected String thresholdMethod;
    protected double minSizeSpot;
    protected boolean useWatershed=false;

    protected boolean darkBg=true;
    protected double minThreshold=Integer.MIN_VALUE;
    protected double maxThreshold=Integer.MAX_VALUE;

    protected boolean spotFindMaxima=false;
    protected double maximaProminence;

    //preprocessing parameters
    String preprocessMacro=null;
    String preprocessMacroQuantif=null;
    boolean Zproject=false;
    int projectionMethod=PROJECTION_MAX;
    int projectionSliceMin=-1;
    int projectionSliceMax=-1;

    double subtractBGRadius=-1;


    public MeasureValue(boolean morphology) {
        if(morphology){
            measure= Measurements.AREA;
        }
        measure += Measurements.MEAN + Measurements.INTEGRATED_DENSITY;
    }

    public void setMeasure(int measure) {
        this.measure = measure;
    }

    public int getMeasure() {
        return measure;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSpotThreshold() {
        return spotThreshold;
    }

    public void setSpotThreshold(String thresholdMethod,double minThreshold, double maxThreshold, double minSizeSpot, boolean useWatershed, boolean darkBg) {
        this.spotThreshold = true;
        this.thresholdMethod = thresholdMethod;
        this.minSizeSpot = minSizeSpot;
        this.useWatershed=useWatershed;
        this.minThreshold=minThreshold;
        this.maxThreshold=maxThreshold;
        this.darkBg=darkBg;
    }

    public String getThresholdMethod() {
        return thresholdMethod;
    }

    public void setThresholdMethod(String thresholdMethod) {
        this.thresholdMethod = thresholdMethod;
    }

    public double getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(double minThreshold) {
        this.minThreshold = minThreshold;
    }

    public double getMaxThreshold() {
        return maxThreshold;
    }

    public void setMaxThreshold(double maxThreshold) {
        this.maxThreshold = maxThreshold;
    }

    public double getMinSizeSpot() {
        return minSizeSpot;
    }

    public void setMinSizeSpot(double minSizeSpot) {
        this.minSizeSpot = minSizeSpot;
    }

    public boolean isUseWatershed() {
        return useWatershed;
    }

    public void setUseWatershed(boolean useWatershed) {
        this.useWatershed = useWatershed;
    }

    public boolean isSpotFindMaxima() {
        return spotFindMaxima;
    }

    public void setSpotFindMaxima(double prominence) {
        this.spotFindMaxima = true;
        this.maximaProminence=prominence;
    }

    public double getMaximaProminence() {
        return maximaProminence;
    }

    public void setMaximaProminence(double maximaProminence) {
        this.maximaProminence = maximaProminence;
    }

    //preprocess
    public String getPreprocessMacro() {
        return preprocessMacro;
    }

    public void setPreprocessMacro(String preprocessMacro) {
        this.preprocessMacro = preprocessMacro;
        System.out.println("set macro in preprocessing : "+preprocessMacro);
    }

    public String getPreprocessMacroQuantif() {
        return preprocessMacroQuantif;
    }

    public void setPreprocessMacroQuantif(String preprocessMacroQuantif) {
        this.preprocessMacroQuantif = preprocessMacroQuantif;
        System.out.println("set macro in preprocessingQuantif : "+preprocessMacroQuantif);
    }

    public boolean isZproject() {
        return Zproject;
    }

    public void setZproject(boolean zproject) {
        Zproject = zproject;
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
    public void setProjection(int method){
        Zproject=true;
        projectionMethod=method;
    }

    public void setProjectionMethod(int projectionMethod) {
        this.projectionMethod = projectionMethod;
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

    public double getSubtractBGRadius() {
        return subtractBGRadius;
    }

    public void setSubtractBGRadius(double subtractBGRadius) {
        this.subtractBGRadius = subtractBGRadius;
    }

    public boolean isDarkBg() {
        return darkBg;
    }

    public void setDarkBg(boolean darkBg) {
        this.darkBg = darkBg;
    }
}
