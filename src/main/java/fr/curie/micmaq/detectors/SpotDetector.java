package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.*;
import ij.io.RoiEncoder;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.LutLoader;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;
import mcib3d.geom.Point3D;
import mcib3d.geom.Voxel3D;
import mcib3d.geom2.*;
import mcib3d.geom2.measurements.MeasureCentroid;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureObject;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.MaximaFinder;
import mcib_plugins.Manager3D.Create3DRoi;
import mcib_plugins.Manager3D.Rois3D;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Author : Camille RABIER
 * Date : 07/06/2022
 * Class for
 * - analyzing the spot images
 * It uses the ROIs obtained from NucleiDetector to analyze per nucleus
 * It analyzes either by threshold+particle analyzer or by find Maxima method
 */
public class SpotDetector {
    //    Images
    private final ImagePlus image; /*Image without modifications*/
    private ImagePlus imageToMeasure; /*Image that will be measured : only with projection*/

    //    Infos for results
    private final String nameExperiment; /*Part of image name that differentiates them from*/
    private final String spotName;
    private MeasureCalibration measureCalibration;

    //    Saving results infos
    private final String resultsDirectory;
    private boolean saveImage;
    private boolean saveRois;
    private boolean savePreprocessedImage;


    //    Showing images
    private boolean showMaximaImage;
    private boolean showThresholdImage;
    private final boolean showPreprocessedImage;


    //    Preprocessing
    //    --> subtract background
    private boolean useRollingBallSize;
    private double rollingBallSize;

    //    --> macro
    private String macroText;

    //Detection of spots
    //    --> find maxima
    private ImagePlus findMaximaIP;
    private ImagePlus findMaximaMask;

    private ImagePlus livePreview;
    private ImagePlus liveOriginal;
    private int livePreviewBestSlice = 1;
    private boolean spotByFindMaxima;
    private double prominence;
    private float maxima3D_minThreshold = 100;
    private float maxima3D_rxy = 1.5f;
    private float maxima3D_rz = 1.5f;
    private float maxima3D_noise = 10;
    private ArrayList<PointRoi> maxima3D_maximaList = null;

    //    --> threshold
    private boolean spotByThreshold;
    private ImagePlus thresholdIP;
    private ImagePlus thresholdPreviewIP;
    private boolean useWatershed;
    private final Detector detector;
    private Objects3DIntPopulation threshold3DObjects = null;

    /* Spotiflow */
    private boolean spotBySpotiflow;
    private String spotiflowModel = "general";
    private boolean pretrainedModel = true;
    private double spotiflowMinProbability = 0;
    private ArrayList<PointRoi> spotiflowRois;
    private double spotiflowScale = 1.0;
    private int spotiflowMinDistance = 0;
    private int spotiflowSubpixRadius = 0;
    private double spotiflowComputedProbability = -1; //for livepreview


    private int measurements = Measurements.MEAN + Measurements.INTEGRATED_DENSITY;

    private int spotMeasurements = Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY + Measurements.MIN_MAX + Measurements.SHAPE_DESCRIPTORS + Measurements.ELLIPSE + Measurements.FERET + Measurements.CENTROID;

    // for groups in RoiManager
    private static final int GROUP_STEP = 10;
    private static final int GROUP_MAXIMA = 1;
    private static final int GROUP_THRESHOLD = 2;
    private static final int GROUP_SPOTIFLOW = 3;



    /**
     * @param image            : image corresponding to spots
     * @param spotName         : name of protein analyzed
     * @param nameExperiment   : name of image without channel specific information
     * @param resultsDirectory : directory to save results
     *                         //     * @param showImage
     */
    public SpotDetector(ImagePlus image, String spotName, String nameExperiment, String resultsDirectory, boolean showPreprocessedImage) {
        //IJ.log("SpotDetector constructor "+spotName);
        //IJ.log("SpotDetector constructor "+nameExperiment);

        detector = new Detector(image, spotName);
        this.showPreprocessedImage = showPreprocessedImage;
        this.image = image;
        this.spotName = spotName;
        if (nameExperiment.endsWith("_")) {
            this.nameExperiment = nameExperiment.substring(0, nameExperiment.length() - 1);
        } else {
            this.nameExperiment = nameExperiment;
        }
        //IJ.log("name experiment "+nameExperiment.replaceAll("[\\\\/:,;*?\"<>|]","_"));
        this.resultsDirectory = resultsDirectory + "/Results/" + nameExperiment.replaceAll("[\\\\/:,;*?\"<>|]", "_").replaceAll(" ", "");
        File dir = new File(this.resultsDirectory);
        if (!dir.exists()) dir.mkdirs();

        this.rollingBallSize = 0;
        this.useRollingBallSize = false;
        this.spotByThreshold = false;
        this.spotByFindMaxima = false;
        this.spotBySpotiflow = false;
        this.spotiflowRois = null;
//        this.regionROI = null;

    }

    /**
     * used only for preview
     *
     * @param image
     * @param spotName
     */
    public SpotDetector(ImagePlus image, String spotName) {
        detector = new Detector(image, spotName);
        this.image = image;
        IJ.log("SpotDetector constructor image"+ this.image.getNSlices() +" slices");
        IJ.log("SpotDetector constructor image(detector)"+ detector.getImage().getNSlices() +" slices");
        this.spotName = spotName;
        this.nameExperiment = "livePreview";
        this.resultsDirectory = "";
        showPreprocessedImage = false;
        this.spotBySpotiflow = false;
        this.spotiflowRois = null;
    }

    /**
     * @param measureCalibration Calibration value and unit to use for measurements
     */
    public void setMeasureCalibration(MeasureCalibration measureCalibration) {
        detector.setMeasureCalibration(measureCalibration);
        this.measureCalibration = measureCalibration;
    }

    /**
     * Set all parameters for projection if necessary
     *
     * @param zStackProj       Method of projection
     * @param zStackFirstSlice First slice of stack to use
     * @param zStackLastSlice  Last slice of stack to use
     */
    public void setzStackParameters(String zStackProj, int zStackFirstSlice, int zStackLastSlice) {
        IJ.log("spotdetector setzStackParameters "+zStackProj+" "+zStackFirstSlice+" "+zStackLastSlice);
        detector.setzStackParameters(zStackProj, zStackFirstSlice, zStackLastSlice);
    }

    /**
     * Set projection method and the slices to use with default values (1 and last slice)
     *
     * @param zStackProj Method of projection
     */
    public void setzStackParameters(String zStackProj) {
        setzStackParameters(zStackProj, 1, image.getNSlices());
    }

    /**
     * Set parameters for unifying the background
     *
     * @param rollingBallSize : size of ball that should be of the size of the biggest object the user wants to analyze
     */
    public void setRollingBallSize(double rollingBallSize) {
        this.useRollingBallSize = true;
        this.rollingBallSize = rollingBallSize;
    }

    /**
     * Set parameters for search of local intensity maxima
     *
     * @param prominence      : minimal difference of intensity with neighbors needed to be considered a local maxima
     * @param showMaximaImage : choice to show resulting mask
     */
    public void setSpotByFindMaxima(double prominence, boolean showMaximaImage) {
        this.spotByFindMaxima = true;
        this.prominence = prominence;
        this.showMaximaImage = showMaximaImage;
    }

    public void setSpotByFindMaxima3D(boolean showMaximaImage, float minThreshold, float rxy, float rz, float noise) {
        this.spotByFindMaxima = true;
        this.showMaximaImage = showMaximaImage;
        this.maxima3D_minThreshold = minThreshold;
        this.maxima3D_rxy = rxy;
        this.maxima3D_rz = rz;
        this.maxima3D_noise = noise;
    }

    /**
     * Set parameters for thresholding and
     *
     * @param thresholdMethod    : method of thresholding
     * @param minSizeSpot        : minimum size of particle to consider
     * @param useWatershed       : if true, use watershed method in addition to thresholding
     * @param showThresholdImage : chooice to show the resulting image
     */
    public void setSpotByThreshold(String thresholdMethod, double minSizeSpot, boolean useWatershed, boolean darkBg, boolean showThresholdImage) {
        this.spotByThreshold = true;
        this.showThresholdImage = showThresholdImage;
        this.useWatershed = useWatershed;
        //macroText = null;
        detector.setThresholdParameters(thresholdMethod, false, minSizeSpot,darkBg); /*does not exclude spot on edges*/
    }

    /**
     * set parameters for thresholding
     * @param thresholdMethod thresholding method
     * @param minTreshold if thresholding method is "user" use this value for minimum
     * @param maxThreshold if thresholding method is "user" use this value for maximum
     * @param minSizeSpot        : minimum size of particle to consider
     * @param useWatershed       : if true, use watershed method in addition to thresholding
     * @param showThresholdImage : chooice to show the resulting image
     *
     */
    public void setSpotByThreshold(String thresholdMethod, double minTreshold, double maxThreshold, double minSizeSpot, boolean useWatershed, boolean darkBg, boolean showThresholdImage) {
        this.spotByThreshold = true;
        this.showThresholdImage = showThresholdImage;
        this.useWatershed = useWatershed;
        //macroText = null;
        detector.setThresholdParameters(thresholdMethod, minTreshold, maxThreshold, false, darkBg, minSizeSpot); /*does not exclude spot on edges*/
    }

    public void setSpotBySpotiflow(String model, boolean pretrainedModel, double minProbability, double spotiflowScale, int minDistance, int subpixRadius) {
        this.spotBySpotiflow = true;
        this.spotiflowModel = model;
        this.pretrainedModel = pretrainedModel;
        this.spotiflowMinProbability = minProbability;
        this.spotiflowScale = spotiflowScale;
        this.spotiflowMinDistance = minDistance;
        this.spotiflowSubpixRadius = subpixRadius;
    }


    /**
     * Set saving's choice
     *
     * @param saveImage : save find maxima or threshold image
     * @param saveRois  : save corresponding regions
     */
    public void setSaving(boolean saveImage, boolean saveRois, boolean savePreprocessedImage) {
        this.saveImage = saveImage;
        this.saveRois = saveRois;
        this.savePreprocessedImage = savePreprocessedImage;
        System.out.println("saveImage: "+saveImage+" saveRois: "+saveRois+" savePreprocessedImage: "+savePreprocessedImage);
    }

    /**
     * Set parameters for macro if necessary
     *
     * @param macroText : macro text to use
     */
    public void setPreprocessingMacro(String macroText) {
        this.macroText = macroText;
    }

    public String getPreprocessingMacro() {
        return macroText;
    }

    public void setPreprocessingMacroQuantif(String macroText) {
        detector.setQuantifMacro(macroText);
        //System.out.println("spot detector macro quantif: "+detector.getQuantifMacro());
    }

    public String getPreprocessingMacroQuantif() {
        return detector.getQuantifMacro();
    }

    /**
     * @return name of image without channel specific information
     */
    public String getNameExperiment() {
        return nameExperiment;
    }

    public String getSpotName() {
        return spotName;
    }

    public int getMeasurements() {
        return measurements;
    }

    public void setMeasurements(int measurements) {
        this.measurements = measurements;
    }

    /**
     * @return name of image
     */
    public String getImageTitle() {
        return image.getTitle();
    }

    private void applyLiveOverlay(Overlay overlay, int bestZ) {
        livePreview.setOverlay(null);
        if (overlay != null) livePreview.setOverlay(overlay);
        livePreview.setHideOverlay(false);
        setLivePreviewSlice(bestZ);
        livePreview.updateAndDraw();
        livePreview.repaintWindow();
    }
    private void setLivePreviewSlice(int z) {
        if (livePreview == null || livePreview.getNSlices() <= 1) return;
        int zz = Math.max(1, Math.min(z, livePreview.getNSlices()));
        if (livePreview.isHyperStack()) livePreview.setZ(zz);
        else livePreview.setSlice(zz);
        livePreview.getProcessor().resetMinAndMax();
    }

    /**
     * live preview for find maxima approach
     * @param prominence
     */
    public void livePreviewFindMaxima(double prominence, boolean show) {
        if (livePreview == null) {
            livePreview = preprocessing();
            autocontrast(livePreview);
            livePreview.setTitle(image.getTitle() + "_" + spotName + "_live_maxima_preprocessed");
            livePreview.show();
            liveOriginal = detector.getImage();
        }
        this.prominence = prominence;
        this.maxima3D_noise = (float) prominence;
        livePreview.show();
        Roi old = livePreview.getRoi();
        livePreview.resetRoi();
        livePreview.setOverlay(null);
        if (show) {
            if (livePreview.getNSlices() > 1) {
                findMaximaIP = livePreview.duplicate();
                detector.renameImage(findMaximaIP, "maxima_live_preprocessed");
                IJ.log("Live Maxima 3D source=" + findMaximaIP.getTitle() + " slices=" + findMaximaIP.getNSlices() + " noise=" + maxima3D_noise);
                prepareFindMaxima3D();
                int count = maxima3D_maximaList.size();
                updateLivePreviewTitle("Spot-Maxima", count);
                IJ.log("Maxima3D detected " + count + " spots");
                Overlay overlay = createPointOverlayBySlice(maxima3D_maximaList, 2);
                applyLiveOverlay(overlay, livePreviewBestSlice);
            } else {
                PointRoi tmp = findMaxima(livePreview, prominence, "full");
                livePreviewBestSlice = 1;
                int count = tmp.getContainedPoints().length;
                updateLivePreviewTitle("Spot-Maxima", count);
                IJ.log("Spot-Maxima detected " + count + " spots");
                applyLiveOverlay(createPointOverlay(tmp, 2), 1);
            }
        } else applyLiveOverlay(new Overlay(), livePreviewBestSlice);
        livePreview.setRoi(old);
    }


    /**
     * live preview for Threhold approach. needs to set parameters by using setSpotByThreshold method
     *
     */
    public void livePreviewThreshold() {
        if (livePreview == null) {
            livePreview = preprocessing();
            autocontrast(livePreview);
            liveOriginal = detector.getImage();
            liveOriginal.show();
        }
        livePreview.show();
        thresholdIP = detector.getThresholdMask(livePreview);
        int numberObjects;
        if (thresholdIP.getNSlices() > 1) {
            Objects3DIntPopulation pop = detector.maskToObjects3D(thresholdIP);
            threshold3DObjects = detector.fixThreshold3DObjects(pop);
            numberObjects = threshold3DObjects.getNbObjects();
        } else {
            RoiManager rm = detector.maskToRoiManager(thresholdIP);
            numberObjects = rm.getCount();
            rm.reset();
        }
        updateLivePreviewTitle("Spot-Threshold", numberObjects);
        IJ.log("Threshold detected " + numberObjects + " objects");
        Overlay overlay = thresholdIP.getNSlices() > 1 ? createThresholdOverlay3D() : createThresholdOverlay(thresholdIP);
        applyLiveOverlay(overlay, livePreviewBestSlice);
    }

    public void livePreviewSpotiflow(String model, boolean pretrained, double threshold,double scale, int minDist,
                                     int subpixRadius, boolean show) {
        if (livePreview == null) {
            livePreview = preprocessing();
            autocontrast(livePreview);
            liveOriginal = detector.getImage();
            liveOriginal.show();
        }
        Roi old=livePreview.getRoi();
        livePreview.resetRoi();
        livePreview.show();
        livePreview.setHideOverlay(true);
        IJ.log("live preview spotiflow show : "+show);
        if(show) {
            IJ.log("live preview (" + threshold + ") is already started :" + (spotiflowRois!=null));
            boolean needRecompute =
                    spotiflowRois == null
                            || !this.spotiflowModel.equals(model)
                            || scale != spotiflowScale
                            || spotiflowMinDistance != minDist
                            || spotiflowSubpixRadius != subpixRadius
                            || threshold < spotiflowComputedProbability;

            if(needRecompute ) {
                IJ.log("compute spotiflow");
                try {
                    this.spotiflowScale = scale;
                    this.spotiflowMinProbability = threshold;
                    this.spotiflowMinDistance = minDist;
                    this.spotiflowSubpixRadius = subpixRadius;
                    this.spotiflowModel = model;
                    this.pretrainedModel = pretrained;
                    SpotiflowLauncher launcher = new SpotiflowLauncher(livePreview);
                    if (pretrainedModel) launcher.setPretrainedModel(spotiflowModel);
                    else launcher.setModelPath(spotiflowModel);
                    launcher.setMinProbability(spotiflowMinProbability);
                    if(scale!=1.0) launcher.setScale(spotiflowScale);
                    if(minDist>0) launcher.setMinDistance(spotiflowMinDistance);
                    if(subpixRadius>0) launcher.setSubpixRadius(spotiflowSubpixRadius);
                    //IJ.setDebugMode(true);
                    File csv = launcher.runSpotiflow();
                    //IJ.setDebugMode(true);
                    spotiflowRois = launcher.loadPoints(csv);
                    int count = spotiflowRois.size();
                    updateLivePreviewTitle("Spot-Spotiflow", count);
                    IJ.log("Spotiflow detected " + count + " spots");

                    Overlay overlay = livePreview.getNSlices() > 1 ? createPointOverlayBySlice(spotiflowRois, 2) : createPointOverlay(SpotiflowLauncher.getGlobalPointRoi(spotiflowRois, threshold), 2);
                    applyLiveOverlay(overlay, livePreviewBestSlice);
                    launcher.cleanup();
                    IJ.log("Spotiflow detected " + spotiflowRois.size() + " spots for " + image.getTitle());
                    this.spotiflowComputedProbability=spotiflowMinProbability;
                } catch (Exception e) {
                    IJ.error("Spotiflow detection failed: " + e.getMessage());
                    e.printStackTrace();
                    spotiflowRois = new ArrayList<>();
                }
            }else{
                ArrayList<PointRoi> filteredRois = filterSpotiflowByProbability(spotiflowRois, threshold);
                int count = filteredRois.size();
                updateLivePreviewTitle("Spot-Spotiflow", count);
                IJ.log("Spotiflow detected " + count + " spots");
                Overlay overlay = livePreview.getNSlices() > 1 ? createPointOverlayBySlice(filteredRois, 2) : createPointOverlay(SpotiflowLauncher.getGlobalPointRoi(filteredRois, threshold), 2);
                applyLiveOverlay(overlay, livePreviewBestSlice);
                /*
                PointRoi globalRoi = SpotiflowLauncher.getGlobalPointRoi(spotiflowRois,threshold);
                Overlay overlay = livePreview.getNSlices() > 1 ? createPointOverlayBySlice(spotiflowRois, 2) : createPointOverlay(SpotiflowLauncher.getGlobalPointRoi(spotiflowRois, threshold), 2);
                applyLiveOverlay(overlay, livePreviewBestSlice);*/
                IJ.log("Spotiflow detected " + filteredRois.size() + " spots for " + image.getTitle());
            }
        }
        //livePreview.setRoi(old);
        livePreview.updateAndRepaintWindow();

    }
    private ArrayList<PointRoi> filterSpotiflowByProbability(ArrayList<PointRoi> input,double threshold) {
        ArrayList<PointRoi> filtered = new ArrayList<>();

        for(PointRoi roi : input) {
            double probability = SpotiflowLauncher.getProbability(roi);
            if(probability >= threshold) {
                filtered.add(roi);
            }
        }

        return filtered;
    }

    private void updateLivePreviewTitle(String method, int count) {
        livePreview.setTitle(
                image.getTitle()
                        + "_" + spotName
                        + "_live_" + method
                        + "_Nb" + count
        );
    }


    void autocontrast(ImagePlus imp) {
        ImageStatistics stats = imp.getRawStatistics();
        int limit = stats.pixelCount / 10;
        int[] histogram = stats.histogram;
        int threshold = stats.pixelCount / 5000;
        int i = -1;
        boolean found = false;
        int count;
        do {
            i++;
            count = histogram[i];
            if (count > limit) count = 0;
            found = count > threshold;
        } while (!found && i < 255);
        int hmin = i;
        i = 256;
        do {
            i--;
            count = histogram[i];
            if (count > limit) count = 0;
            found = count > threshold;
        } while (!found && i > 0);
        int hmax = i;
        if (hmax > hmin) {
            double min = stats.histMin + hmin * stats.binSize;
            double max = stats.histMin + hmax * stats.binSize;
            if (min == max) {
                min = stats.min;
                max = stats.max;
            }
            imp.setDisplayRange(min, max);
        } else imp.resetDisplayRange();
    }

    public void endLivePreview() {
        if (livePreview != null) {
            livePreview.changes = false;
            livePreview.close();
            livePreview = null;
        }
        if (liveOriginal != null) {
            liveOriginal.changes = false;
            liveOriginal.close();
            liveOriginal = null;
        }
        if (thresholdPreviewIP != null) {
            thresholdPreviewIP.changes = false;
            thresholdPreviewIP.close();
            thresholdPreviewIP = null;
        }
    }

    public void showHideToggle(boolean show) {
        if (livePreview != null) {
            livePreview.setHideOverlay(!show);
            livePreview.updateAndDraw();
            livePreview.repaintWindow();
        }
        if(thresholdPreviewIP != null) {
            thresholdPreviewIP.setHideOverlay(!show);
            thresholdPreviewIP.updateAndDraw();
            thresholdPreviewIP.repaintWindow();
        }

    }

    /**
     *
     */
   /* public void preview() {
//        PREPROCESSING : PROJECTION, PRE-TREATMENT (subtract background, macro)....
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        if (preprocessed != null) {
            if (showPreprocessedImage) {
                preprocessed.show();
            }
            if (spotByThreshold) {
                thresholdIP = detector.getThresholdMask(preprocessed);
                if (showThresholdImage) {
                    thresholdIP.show();
                }
                if (useWatershed) detector.getWatershed(thresholdIP.getProcessor()).show();
            }
            if (spotByFindMaxima) {
                findMaximaIP = preprocessed.duplicate();
                detector.renameImage(findMaximaIP, "maxima");

                findMaxima(findMaximaIP, prominence, "preview");
            }
        }
    }*/
    public void preview() {
        preview(0);
    }
    public void preview(int channelIndex) {
        ImagePlus preprocessed = preprocessing();
        if (preprocessed == null) return;
        int baseGroup = channelIndex * GROUP_STEP;
        RoiManager rm = RoiManager.getRoiManager();
        rm.reset();
        preprocessed.show();
        int nMaxima = spotByFindMaxima ? previewFindMaxima(preprocessed, baseGroup + GROUP_MAXIMA) : 0;
        int nSpotiflow = spotBySpotiflow ? previewSpotiflow(preprocessed, baseGroup + GROUP_SPOTIFLOW) : 0;
        int nThreshold = spotByThreshold ? previewThreshold(preprocessed, baseGroup + GROUP_THRESHOLD) : 0;
        preprocessed.setTitle(image.getTitle() + "_" + spotName + "_preview_M" + nMaxima + "_S" + nSpotiflow + "_T" + nThreshold);
        rm.runCommand("Show All");
    }

    private int previewFindMaxima(ImagePlus preprocessed, int group) {
        ArrayList<PointRoi> points;
        if (preprocessed.getNSlices() > 1) {
            findMaximaIP = preprocessed.duplicate();
            detector.renameImage(findMaximaIP, "maxima");
            prepareFindMaxima3D();
            points = maxima3D_maximaList;
        } else {
            points = findMaximaPoints2D(preprocessed);
        }
        return addPointsToRoiManagerBySlice(points, group, "Maxima",preprocessed.getNSlices()>1);
    }
    private int previewSpotiflow(ImagePlus preprocessed, int group) {
        try {
            SpotiflowLauncher launcher = new SpotiflowLauncher(preprocessed);
            if (pretrainedModel) launcher.setPretrainedModel(spotiflowModel);
            else launcher.setModelPath(spotiflowModel);
            launcher.setMinProbability(spotiflowMinProbability);
            if (spotiflowScale != 1.0) launcher.setScale(spotiflowScale);
            if (spotiflowMinDistance > 0) launcher.setMinDistance(spotiflowMinDistance);
            if (spotiflowSubpixRadius > 0) launcher.setSubpixRadius(spotiflowSubpixRadius);
            File csv = launcher.runSpotiflow();
            spotiflowRois = launcher.loadPoints(csv);
            int n = addPointsToRoiManagerBySlice(spotiflowRois, group, "Spotiflow", preprocessed.getNSlices() > 1);
            launcher.cleanup();
            return n;
        } catch (Exception e) {
            IJ.error("Spotiflow detection failed: " + e.getMessage());
            return 0;
        }
    }

    private int previewThreshold(ImagePlus preprocessed, int group) {
        thresholdIP = detector.getThresholdMask(preprocessed);
        if (thresholdIP.getNSlices() > 1) {
            Objects3DIntPopulation pop = detector.maskToObjects3D(thresholdIP);
            threshold3DObjects = detector.fixThreshold3DObjects(pop);
            IJ.log("Threshold3D detected " + threshold3DObjects.getNbObjects() + " objects");
            return previewThreshold3D(group);
        }
        RoiManager rm = RoiManager.getRoiManager();
        Roi[] previousRois = rm.getRoisAsArray();
        RoiManager thresholdManager = detector.maskToRoiManager(thresholdIP);
        Roi[] thresholdRois = thresholdManager.getRoisAsArray();
        rm.reset();
        for (Roi roi : previousRois) rm.addRoi(roi);
        if (thresholdRois.length == 0) {
            IJ.log("Threshold2D detected 0 objects");
            return 0;
        }
        ShapeRoi global = null;
        for (Roi thresholdRoi : thresholdRois) {
            ShapeRoi current = new ShapeRoi(thresholdRoi);
            if (global == null) global = current;
            else global.or(current);
        }
        global.setGroup(group);
        global.setName("Spot-Threshold-Nb" + thresholdRois.length);
        rm.addRoi(global);
        IJ.log("Threshold2D detected " + thresholdRois.length + " objects");
        return thresholdRois.length;
    }

    private int previewThreshold3D(int group) {
        RoiManager rm = RoiManager.getRoiManager();
        TreeMap<Integer, ArrayList<Roi>> roisBySlice = new TreeMap<>();
        for (Object3DInt obj : threshold3DObjects.getObjects3DInt()) {
            Rois3D rois3D = Create3DRoi.computeRois(obj);
            for (Object3DPlane plane : obj.getObject3DPlanes()) {
                int z = plane.getZPlane() + 1;
                Roi roi = rois3D.getRoi(z);
                if (roi == null) continue;
                roisBySlice.computeIfAbsent(z, k -> new ArrayList<>()).add(roi);
            }
        }
        for (Integer z : roisBySlice.keySet()) {
            ShapeRoi global = null;
            for (Roi roi : roisBySlice.get(z)) {
                ShapeRoi current = new ShapeRoi(roi);
                if (global == null) global = current;
                else global.or(current);
            }
            if (global == null) continue;
            int nbObjects = roisBySlice.get(z).size();
            //IJ.log("Threshold z=" + z);
            global.setPosition(z);
            global.setGroup(group);
            global.setName("Spot-Threshold-Z" + z + "-Nb" + nbObjects);
            rm.addRoi(global);
        }
        IJ.log("Threshold3D detected " + threshold3DObjects.getNbObjects() + " objects");
        return threshold3DObjects.getNbObjects();
    }

    private int addPointsToRoiManagerBySlice(ArrayList<PointRoi> points, int group, String prefix, boolean stack) {
        if (points == null || points.isEmpty()) return 0;
        RoiManager rm = RoiManager.getRoiManager();
        TreeMap<Integer, FloatPolygon> polygonsBySlice = new TreeMap<>();
        int nPoints = 0;
        for (PointRoi spot : points) {
            int z = stack ? getPointSliceSafe(spot) : 1;
            FloatPolygon fpSpot = spot.getFloatPolygon();
            if (fpSpot == null || fpSpot.npoints == 0) continue;
            double x = fpSpot.xpoints[0];
            double y = fpSpot.ypoints[0];
            FloatPolygon fp = polygonsBySlice.get(z);
            if (fp == null) {
                fp = new FloatPolygon();
                polygonsBySlice.put(z, fp);
            }
            fp.addPoint(x, y);
            nPoints++;
        }
        for (Integer z : polygonsBySlice.keySet()) {
            PointRoi roi = new PointRoi(polygonsBySlice.get(z));
            int nb = roi.getContainedPoints().length;
            if(stack) roi.setName("Spot-" + prefix + "-Z" + z + "-Nb" + nb);
            else roi.setName("Spot-" + prefix + "-Nb" + nb);
            roi.setGroup(group);
            if (stack) {
                roi.setPosition( z);
                roi.setPosition(0, z, 0);
            }
            rm.addRoi(roi);
        }
        IJ.log(prefix + ": " + nPoints + " points in " + polygonsBySlice.size() + " ROI(s), group " + group);
        return nPoints;
    }

    private Overlay createPointOverlay(PointRoi roi, int size) {
        livePreviewBestSlice = 1;
        roi.setSize(size);
        roi.setStrokeColor(Color.RED);
        Overlay overlay = new Overlay();
        overlay.add(roi);
        return overlay;
    }

    private Overlay createPointOverlayBySlice(ArrayList<PointRoi> points, int size) {
        Overlay overlay = new Overlay();
        TreeMap<Integer, FloatPolygon> polygonsBySlice = new TreeMap<>();
        if (points == null || points.isEmpty()) {
            livePreviewBestSlice = 1;
            return overlay;
        }
        for (PointRoi spot : points) {
            int z = getPointSliceSafe(spot);
            FloatPolygon fpSpot = spot.getFloatPolygon();
            if (fpSpot == null || fpSpot.npoints == 0) continue;
            FloatPolygon fp = polygonsBySlice.get(z);
            if (fp == null) {
                fp = new FloatPolygon();
                polygonsBySlice.put(z, fp);
            }
            fp.addPoint(fpSpot.xpoints[0], fpSpot.ypoints[0]);
        }
        livePreviewBestSlice = findBestSliceFromPoints(polygonsBySlice);
        for (Integer z : polygonsBySlice.keySet()) {
            PointRoi roi = new PointRoi(polygonsBySlice.get(z));
            roi.setPosition(z);
            roi.setPosition(0,z,0);
            roi.setSize(size);
            roi.setStrokeColor(Color.RED);
            overlay.add(roi);

        }
        return overlay;
    }
    private int findBestSliceFromPoints(TreeMap<Integer, FloatPolygon> polygonsBySlice) {
        int bestZ = 1;
        int maxCount = 0;
        for (Integer z : polygonsBySlice.keySet()) {
            int count = polygonsBySlice.get(z).npoints;
            if (count > maxCount) {
                maxCount = count;
                bestZ = z;
            }
        }
        return bestZ;
    }

    private int getPointSliceSafe(PointRoi spot){
        int z = 1;
        try{
            if(spot.getZPosition() > 0) z = spot.getZPosition() + 1;
            else if(spot.getPosition() > 0) z = spot.getPosition() + 1;
        }catch(Exception e){
            z = 1;
        }
        return Math.max(1,z);
    }

    public boolean prepare() {
        return prepare(false);
    }

    public boolean prepare(boolean preview) {


        //if (image != null) IJ.log("spotdetector prepare start image " + image.getNSlices() + " slices");
        //else IJ.log("spotdetector prepare start image is null");

        //if (imageToMeasure != null) IJ.log("spotdetector prepare start image to measure " + imageToMeasure.getNSlices() + " slices");
        //else IJ.log("spotdetector prepare start image to measure is null ");

        ImagePlus preprocessed = preprocessing();
        if (preprocessed == null) return false;
        if(!preview) createSavingDirectories(preprocessed.getNSlices() > 1);

        //IJ.log("spotdetector prepare getPreprocessed " + preprocessed.getNSlices() + " slices");

        if (showPreprocessedImage) preprocessed.show();

        if (spotByThreshold) prepareThreshold(preprocessed, preview);
        if (spotByFindMaxima) prepareFindMaxima(preprocessed, preview);
        if (spotBySpotiflow) prepareSpotiflow(preprocessed, preview);
        if(!preview) saveAllDetectedSpotRois(preprocessed.getNSlices() > 1);

        if (!preview && resultsDirectory != null && savePreprocessedImage) {
            IJ.log("savePreprocessedImage " + savePreprocessedImage + " " + imageToMeasure.getNSlices() + " slices");
            //IJ.saveAsTiff(imageToMeasure, resultsDirectory + "/Images/preprocessed/" + image.getTitle() + spotName + "_quantif_preprocessed.tif");
            IJ.saveAsTiff(preprocessed,resultsDirectory + "/Images/preprocessed/" + image.getTitle() + spotName + "_preprocessed.tif");
        }

        return true;
    }


    private void createSavingDirectories(boolean stack) {
        File tmp;
        if(saveRois){
            tmp = new File(resultsDirectory + "/ROI/Spot" + spotName + "/");
            if(!tmp.exists()) tmp.mkdirs();
            if(spotByThreshold){
                tmp = new File(resultsDirectory + "/ROI/Spot" + spotName + "/" + (stack ? "thresholding3D" : "thresholding") + "/");
                if(!tmp.exists()) tmp.mkdirs();
            }
            if(spotByFindMaxima){
                tmp = new File(resultsDirectory + "/ROI/Spot" + spotName + "/" + (stack ? "maxima3D" : "findmaxima") + "/");
                if(!tmp.exists()) tmp.mkdirs();
            }
            if(spotBySpotiflow){
                tmp = new File(resultsDirectory + "/ROI/Spot" + spotName + "/" + (stack ? "spotiflow3D" : "spotiflow") + "/");
                if(!tmp.exists()) tmp.mkdirs();
            }
        }
        if(saveImage){
            tmp = new File(resultsDirectory + "/Images/Spot" + spotName + "/");
            if(!tmp.exists()) tmp.mkdirs();
            if(spotByThreshold){
                tmp = new File(resultsDirectory + "/Images/Spot" + spotName + "/" + (stack ? "thresholding3D" : "thresholding") + "/");
                if(!tmp.exists()) tmp.mkdirs();
            }
        }
        if(savePreprocessedImage){
            tmp = new File(resultsDirectory + "/Images/preprocessed/");
            if(!tmp.exists()) tmp.mkdirs();
        }
    }

    private void prepareThreshold(ImagePlus preprocessed, boolean preview) {
        IJ.log("# prepareThreshold");
        thresholdIP = detector.getThresholdMask(preprocessed);
        int numberObjects;
        if (thresholdIP.getNSlices() > 1) {
            Objects3DIntPopulation pop = detector.maskToObjects3D(thresholdIP);
            //IJ.log("avant fix : " + pop.getNbObjects());
            threshold3DObjects = detector.fixThreshold3DObjects(pop);
            numberObjects = threshold3DObjects.getNbObjects();
            //IJ.log("après fix : " + numberObjects);
            IJ.log("Threshold3D detected " + numberObjects + " spots");
        } else {
            RoiManager rm = detector.maskToRoiManager(thresholdIP);
            numberObjects = rm.getCount();
            IJ.log("Threshold2D detected " + numberObjects + " spots");
        }
        if (preview) {
            showThresholdPreview(preprocessed, thresholdIP, numberObjects);
            return;
        }
        if (showThresholdImage) thresholdIP.show();
        if (resultsDirectory != null && saveImage) {
            String folder = thresholdIP.getNSlices() > 1 ? "/Images/Spot" + spotName + "/thresholding3D/" : "/Images/Spot" + spotName + "/thresholding/";

            if (IJ.saveAsTiff(thresholdIP, resultsDirectory + folder + thresholdIP.getTitle())) {
                IJ.log("The spot threshold mask " + thresholdIP.getTitle() + " was saved in " + resultsDirectory + folder);
            } else {
                IJ.log("The spot threshold mask " + thresholdIP.getTitle() + " could not be saved in " + resultsDirectory + folder);
            }
        }
    }


    private void showThresholdPreview(ImagePlus preprocessed, ImagePlus binaryMask, int numberObjects) {
        if (thresholdPreviewIP != null) {
            thresholdPreviewIP.changes = false;
            thresholdPreviewIP.close();
        }
        thresholdPreviewIP = preprocessed.duplicate();
        thresholdPreviewIP.setTitle(preprocessed.getTitle() + " - " + spotName + " threshold preview - " + numberObjects + " objects");
        autocontrast(thresholdPreviewIP);
        thresholdPreviewIP.setOverlay(createThresholdOverlay(binaryMask));
        thresholdPreviewIP.show();
        thresholdPreviewIP.updateAndDraw();
    }
    private Overlay createThresholdOverlay(ImagePlus binaryMask) {
        livePreviewBestSlice = 1;
        Overlay overlay = new Overlay();
        RoiManager rm = detector.maskToRoiManager(binaryMask);
        Roi[] rois = rm.getRoisAsArray();
        ShapeRoi global = null;
        for (Roi roi : rois) {
            ShapeRoi current = new ShapeRoi(roi);
            if (global == null) global = current;
            else global.or(current);
        }
        if (global != null) {
            global.setStrokeColor(Color.RED);
            global.setStrokeWidth(1.5);
            global.setFillColor(new Color(255,0,0,80));
            overlay.add(global);
        }
        return overlay;
    }
    private Overlay createThresholdOverlay3D() {
        Overlay overlay = new Overlay();
        TreeMap<Integer, ArrayList<Roi>> roisBySlice = new TreeMap<>();
        for (Object3DInt obj : threshold3DObjects.getObjects3DInt()) {
            Rois3D rois3D = Create3DRoi.computeRois(obj);
            for (Object3DPlane plane : obj.getObject3DPlanes()) {
                int z = plane.getZPlane();
                Roi roi = rois3D.getRoi(z);
                if (roi == null) continue;
                roisBySlice.computeIfAbsent(z, k -> new ArrayList<>()).add(roi);
            }
        }
        livePreviewBestSlice = findBestSliceFromRois(roisBySlice);
        for (Integer z : roisBySlice.keySet()) {
            ShapeRoi global = null;
            for (Roi roi : roisBySlice.get(z)) {
                ShapeRoi current = new ShapeRoi(roi);
                if (global == null) global = current;
                else global.or(current);
            }
            if (global == null) continue;
            global.setPosition(z);
            global.setStrokeColor(Color.RED);
            global.setStrokeWidth(1.5);
            global.setFillColor(new Color(255,0,0,80));
            overlay.add(global);
        }
        return overlay;
    }
    private int findBestSliceFromRois(TreeMap<Integer, ArrayList<Roi>> roisBySlice) {
        int bestZ = 1;
        int maxCount = 0;
        for (Integer z : roisBySlice.keySet()) {
            int count = roisBySlice.get(z).size();
            if (count > maxCount) {
                maxCount = count;
                bestZ = z;
            }
        }
        return bestZ;
    }


    private void prepareFindMaxima(ImagePlus preprocessed, boolean preview) {
        IJ.log("# prepareFindMaxima");
        findMaximaIP = preprocessed.duplicate();
        detector.renameImage(findMaximaIP, "maxima");

        if (findMaximaIP.getNSlices() > 1) {
            prepareFindMaxima3D();
        } else {
            createFindMaximaMask(findMaximaIP);
        }

        if (preview) {
            if (findMaximaIP.getNSlices() == 1) {
                PointRoi roiMaxima = findMaxima(findMaximaIP, prominence, "full");
                findMaximaIP.setRoi(roiMaxima);
                if (showMaximaImage) findMaximaIP.show();
            }
            return;
        }


        if (resultsDirectory == null) return;

    }
    private ArrayList<PointRoi> findMaximaPoints2D(ImagePlus preprocessed) {
        ArrayList<PointRoi> points = new ArrayList<>();
        MaximumFinder maximumFinder = new MaximumFinder();
        ImageProcessor proc = preprocessed.getProcessor().duplicate();
        Polygon maxima = maximumFinder.getMaxima(proc, prominence, true);
        for (int i = 0; i < maxima.npoints; i++) points.add(new PointRoi(maxima.xpoints[i], maxima.ypoints[i]));
        return points;
    }
    private void prepareFindMaxima3D() {
        ImageHandler img = ImageHandler.wrap(this.findMaximaIP);
        //IJ.log("Removing peaks below " + this.maxima3D_minThreshold);

        ImageHandler thresholded = img.duplicate();
        thresholded.thresholdCut(this.maxima3D_minThreshold, false, true);

        MaximaFinder maximaFinder = new MaximaFinder(thresholded, this.maxima3D_noise);
        maximaFinder.setRadii(this.maxima3D_rxy, this.maxima3D_rz);
        maximaFinder.setVerbose(false);

        ArrayList<Voxel3D> list = maximaFinder.getListPeaks();
        maxima3D_maximaList = new ArrayList<>();

        for (Voxel3D v : list) {
            PointRoi tmp = new PointRoi(v.getX(), v.getY());
            int z= (int) v.getZ() ;
            tmp.setPosition(z);
            tmp.setPosition(0, z, 0);
            tmp.setProperty("intensity", Double.toString(v.getValue()));
            maxima3D_maximaList.add(tmp);
        }

        IJ.log("Maxima3D detected " + maxima3D_maximaList.size() + " spots for " + image.getTitle());
    }

    private void prepareSpotiflow(ImagePlus preprocessed, boolean preview) {
        IJ.log("# prepareSpotiflow");
        try {
            SpotiflowLauncher launcher = new SpotiflowLauncher(preprocessed);

            if (pretrainedModel) launcher.setPretrainedModel(spotiflowModel);
            else launcher.setModelPath(spotiflowModel);

            launcher.setMinProbability(spotiflowMinProbability);

            if (spotiflowScale != 1.0) launcher.setScale(spotiflowScale);
            if (spotiflowMinDistance > 0) launcher.setMinDistance(spotiflowMinDistance);
            if (spotiflowSubpixRadius > 0) launcher.setSubpixRadius(spotiflowSubpixRadius);

            File csv = launcher.runSpotiflow();
            spotiflowRois = launcher.loadPoints(csv);

            IJ.log("Spotiflow detected " + spotiflowRois.size() + " spots for " + image.getTitle());

            PointRoi globalRoi = SpotiflowLauncher.getGlobalPointRoi(spotiflowRois, spotiflowMinProbability);

            if (preview) {
                preprocessed.show();
                preprocessed.setOverlay(new Overlay(globalRoi));
                RoiManager rm = RoiManager.getRoiManager();
                rm.reset();
                for (PointRoi roi : spotiflowRois) rm.addRoi(roi);
                rm.runCommand("Show All");
            }
            /*else if(resultsDirectory != null && saveRois && globalRoi != null && preprocessed.getNSlices() == 1){
                RoiEncoder.save(globalRoi,resultsDirectory + "/ROI/Spot" + spotName + "/spotiflow/" + image.getTitle() + "_spotiflow_all_roi.roi");
            }*/

            launcher.cleanup();
        } catch (Exception e) {
            IJ.error("Spotiflow detection failed: " + e.getMessage());
            e.printStackTrace();
            spotiflowRois = new ArrayList<>();
        }
    }

    /**
     * Detect spot in region wanted : nucleus, cell or cytoplasm
     *
     * @param regionID          : id of objet (number in list of ROI) for saving of threshold roi
     * @param regionROI         : ROI where the spot have to be detected
     * @param resultsTableFinal : results table to fill
     * @param type              : image, cell, nucleus or cytoplasm
     */
    public void analysisPerRegion(int regionID, Roi regionROI, ResultsTable resultsTableFinal, String type, ResultsTable spotsMeasuresTable) {
        imageToMeasure.setRoi(regionROI);
        ResultsTable rawMeasures = new ResultsTable();
//        Measures of mean and raw intensities in the whole ROI are always done for spot images
        Analyzer analyzer = new Analyzer(imageToMeasure, measurements, rawMeasures);
        analyzer.measure();
        detector.setResultsAndRename(rawMeasures, resultsTableFinal, 0, type  + spotName); /*always first line, because analyzer replace line*/
        //IJ.log("SpotDetector analysis per region : "+spotName);
        //IJ.log("SpotDetector analysis per region : "+type);
//        Detection and measurements
        if(spotByFindMaxima){
            PointRoi roiMaxima = findMaximaPerRegion(regionID, regionROI, resultsTableFinal, type, spotsMeasuresTable);

            if(resultsDirectory != null && saveRois && roiMaxima != null && roiMaxima.getContainedPoints().length > 0){
                RoiEncoder.save(roiMaxima, resultsDirectory + "/ROI/Spot" + spotName + "/findmaxima/" + image.getTitle() + "_findmaxima_" + type + (regionID + 1) + "_ROIs.roi");
            }
        }
        if (spotBySpotiflow) {
            PointRoi roiAll = findSpotiflow(regionID, regionROI, resultsTableFinal, type, spotsMeasuresTable);
            if (resultsDirectory != null && saveRois && roiAll!=null) {
                RoiEncoder.save(roiAll, resultsDirectory + "/ROI/Spot" + spotName + "/spotiflow/" + image.getTitle() + "_spotiflow_" + type + (regionID + 1) + "_ROIs.roi");
            }
        }
        if (spotByThreshold) {
            findThresholdPerRegion(regionID, regionROI, resultsTableFinal, type,spotsMeasuresTable);
        }
    }

    /**
     * Detect spot in region wanted : nucleus, cell or cytoplasm
     *
     * @param regionID          : id of objet (number in list of ROI) for saving of threshold roi
     * @param measureRoi3D         : ROI where the spot have to be detected
     * @param resultsTableFinal : results table to fill
     * @param type              : image, cell, nucleus or cytoplasm
     */
    public void analysisPerRegion(int regionID, Object3DInt measureRoi3D, ResultsTable resultsTableFinal, String type, ResultsTable spotsMeasuresTable) {
        measureRoi3D.setVoxelSizeXY(measureCalibration.getPixelArea());
        measureRoi3D.setVoxelSizeZ(measureCalibration.getPixelsZ());
        measureRoi3D.setUnit(measureCalibration.getUnit());
        String[] headingsMeasureIntensity = MeasureRois.getValidMeasurements3DIntensity(measurements);
        MeasureObject measureObjectIntensity = new MeasureObject(measureRoi3D);
        this.imageToMeasure.setTitle("spot measure intensity"+spotName);
        if(showPreprocessedImage) this.imageToMeasure.show();
        Double[] measurementsIntensity = measureObjectIntensity.measureIntensityList(headingsMeasureIntensity, ImageHandler.wrap(this.getImageToMeasure()));
        MeasureRois.setResults3D(measurementsIntensity, headingsMeasureIntensity, resultsTableFinal, type + this.getSpotName(), measureCalibration);


        //imageToMeasure.setRoi(regionROI);
        //ResultsTable rawMeasures = new ResultsTable();
//        Measures of mean and raw intensities in the whole ROI are always done for spot images
        //Analyzer analyzer = new Analyzer(imageToMeasure, measurements, rawMeasures);
        //analyzer.measure();
        //detector.setResultsAndRename(rawMeasures, resultsTableFinal, 0, type  + spotName); /*always first line, because analyzer replace line*/
        //IJ.log("SpotDetector analysis per region : "+spotName);
        //IJ.log("SpotDetector analysis per region : "+type);
//        Detection and measurements
        if(spotByFindMaxima){
            ArrayList<PointRoi> nucleusPoints = findMaximaPerRegion(regionID,measureRoi3D,resultsTableFinal,type,spotsMeasuresTable);
            if(resultsDirectory != null && saveRois && nucleusPoints != null && !nucleusPoints.isEmpty()){
                RoiManager rm = createPointRoiManagerBySlice(nucleusPoints,"Maxima",true);
                saveRoiManager(rm,resultsDirectory + "/ROI/Spot" + spotName + "/maxima3D/" + image.getTitle() + "_maxima3D_" + type + (regionID + 1) + "_ROIs.zip","Maxima3D " + type + (regionID + 1) + " ROI");
            }
        }
        if(spotBySpotiflow){
            ArrayList<PointRoi> nucleusPoints = findSpotiflow(regionID,measureRoi3D,resultsTableFinal,type,spotsMeasuresTable);
            if(resultsDirectory != null && saveRois && nucleusPoints != null && !nucleusPoints.isEmpty()){
                RoiManager rm = createPointRoiManagerBySlice(nucleusPoints,"Spotiflow",true);
                saveRoiManager(rm,resultsDirectory + "/ROI/Spot" + spotName + "/spotiflow3D/" + image.getTitle() + "_spotiflow3D_" + type + (regionID + 1) + "_ROIs.zip","Spotiflow3D " + type + (regionID + 1) + " ROI");
            }
        }
        if (spotByThreshold) {
            findThresholdPerRegion3D(regionID, measureRoi3D, resultsTableFinal, type,spotsMeasuresTable);
        }
    }

    /**
     * Detect and measure spot in specific region by threshold
     *
     * @param regionID          : id of objet (number in list of ROI) for saving of threshold roi
     * @param regionROI         : ROI where the spot have to be detected
     * @param resultsTableToAdd : results table to fill
     * @param type              : image, cell, nucleus or cytoplasm
     */
    private void findThresholdPerRegion(int regionID, Roi regionROI, ResultsTable resultsTableToAdd, String type, ResultsTable spotMeasuresTable) {
        RoiManager roiManagerFoci = null;
        int numberSpot = 0; /*count number of spot detected*/
        double nucleusAreaPix = Double.NaN;
        if (regionROI != null) {
            nucleusAreaPix = regionROI.getStatistics().area;

//        Detection
            ImageProcessor tmpIP = thresholdIP.getProcessor().duplicate();
            if (regionROI instanceof PolygonRoi || regionROI instanceof ShapeRoi) {
                Roi tmproi = regionROI.getInverse(thresholdIP);
                tmpIP.setValue(0);
                //System.out.println("debug fill");
                tmpIP.fill(tmproi);
                //new ImagePlus("debug2_" + type, findMaximaProc).show();
            } else {
                System.out.println("not a polygonRoi " + regionROI.getClass());
            }

            tmpIP.setRoi(regionROI);
            //thresholdIP.getProcessor().invertLut();
            roiManagerFoci = detector.analyzeParticles(new ImagePlus("", tmpIP));
            numberSpot = roiManagerFoci.getCount();
//            --> Saving
            if (resultsDirectory != null && saveRois && numberSpot > 0) {
                String extension = (roiManagerFoci.getCount() == 1) ? ".roi" : ".zip";
                if (roiManagerFoci.save(resultsDirectory + "/ROI/Spot" + spotName + "/thresholding/" + image.getTitle() + "_threshold_" + type + (regionID + 1) + "_ROIs" + extension)) {
                    IJ.log("The ROIs of the " + type + " " + (regionID + 1) + " of the image " + image.getTitle() + " by threshold method were saved in " + resultsDirectory + "/ROI/Spot" + spotName + "/thresholding/");
                } else {
                    IJ.log("The ROIs of the " + type + " " + (regionID + 1) + " of the image " + image.getTitle() + " by threshold method could not be saved in " + resultsDirectory + "/ROI/Spot" + spotName + "/thresholding/");
                }
            }
        }

//        Measurement
        resultsTableToAdd.addValue(type + spotName + " Spot(threshold) nr. spots", numberSpot);
        if (numberSpot > 0) {
            ResultsTable resultsTable = new ResultsTable();
            Analyzer analyzer = new Analyzer(imageToMeasure, spotMeasurements, resultsTable);
            for (int spot = 0; spot < numberSpot; spot++) {
                roiManagerFoci.select(imageToMeasure, spot);
                analyzer.measure();

            }
            if(spotMeasuresTable!=null) {
                String[] headings=resultsTable.getHeadings();
                //for(String head:headings) IJ.log(head);
                int spotID = 1;
                for(int r=0;r<resultsTable.getCounter();r++){
                    spotMeasuresTable.addValue("Name experiment",nameExperiment);
                    spotMeasuresTable.addValue(type+" nr",regionID + 1);
                    spotMeasuresTable.addValue("Spot method","Threshold2D");
                    spotMeasuresTable.addValue("Spot ID",spotID++);
                    for(int c=0;c<headings.length;c++){
                        String heading = headings[c];
                        double value = resultsTable.getValue(heading,r);
                        if(heading.equals("Mean")) spotMeasuresTable.addValue("Intensity",value);
                        else spotMeasuresTable.addValue(heading,value);
                        if(heading.startsWith("Area")) spotMeasuresTable.addValue("Area (" + measureCalibration.getUnit() + ")",measureCalibration.getPixelArea() * value);
                    }
                    spotMeasuresTable.incrementCounter();
                }
            }
            detector.setSummarizedResults(resultsTable, resultsTableToAdd, type  + spotName, nucleusAreaPix);
        } else {
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold) AreaTotal (pixel)",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold) AreaTotal (" + measureCalibration.getUnit() + ")",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold) AreaOccupancy (%)",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold) RawIntDenTotal",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold) MeanGlobal",Double.NaN);
        }
    }

    /**
     * Detect and measure spots by searching local maxima
     *
     * @param regionROI         : ROI where the spot have to be detected
     * @param resultsTableToAdd : results table to fill
     * @param type              : image, cell, nucleus or cytoplasm
     */
    private PointRoi findMaximaPerRegion(int regionID,Roi regionROI,ResultsTable resultsTableToAdd,String type,ResultsTable spotsMeasuresTable){
        ImagePlus tmp = (findMaximaMask != null) ? findMaximaMask : findMaximaIP;
        double prom = (findMaximaMask != null) ? 10 : prominence;
        tmp.setRoi(regionROI);
        PointRoi roiMaxima = findMaxima(tmp,prom,type);
        int size = 0;
        double sumIntensity = 0;
        int spotID = 1;
        for(Point p : roiMaxima){
            double intensity = findMaximaIP.getProcessor().getPixelValue(p.x,p.y);
            size++;
            sumIntensity += intensity;
            if(spotsMeasuresTable != null){
                spotsMeasuresTable.addValue("Name experiment",nameExperiment);
                spotsMeasuresTable.addValue(type + " nr",regionID + 1);
                spotsMeasuresTable.addValue("Spot method","Maxima2D");
                spotsMeasuresTable.addValue("Spot ID",spotID++);
                spotsMeasuresTable.addValue("X",p.x);
                spotsMeasuresTable.addValue("Y",p.y);
                spotsMeasuresTable.addValue("Intensity",intensity);
                spotsMeasuresTable.incrementCounter();
            }
        }
        resultsTableToAdd.addValue(type + spotName + " Spot(maxima) prominence",prominence);
        resultsTableToAdd.addValue(type + spotName + " Spot(maxima) nr. spots",size);
        if(size > 0) resultsTableToAdd.addValue(type + spotName + " Spot(maxima) mean",sumIntensity / size);
        else resultsTableToAdd.addValue(type + spotName + " Spot(maxima) mean",Double.NaN);
        return roiMaxima;
    }

    protected PointRoi findSpotiflow(int regionID, Roi regionROI, ResultsTable resultsTableToAdd, String type, ResultsTable spotsMeasuresTable) {
        if (spotiflowRois == null) {
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) model", spotiflowModel);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) probathr", spotiflowMinProbability);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) nr. spots", Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean intensity", Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean probability", Double.NaN);
            return null;
        }

        int numberSpot = 0;
        int spotID = 1;
        double sumIntensity = 0;
        double sumProbability = 0;

        PointRoi roiAll = null;

        for (PointRoi spot : spotiflowRois) {
            double x = spot.getFloatPolygon().xpoints[0];
            double y = spot.getFloatPolygon().ypoints[0];

            if (regionROI != null && !regionROI.contains((int)Math.round(x), (int)Math.round(y))) continue;
            if(roiAll==null) roiAll= new PointRoi(x,y);
            else roiAll.addPoint(x,y);

            double intensity = SpotiflowLauncher.getIntensity(spot);
            double probability = SpotiflowLauncher.getProbability(spot);

            numberSpot++;
            sumIntensity += intensity;
            sumProbability += probability;

            if (spotsMeasuresTable != null) {
                spotsMeasuresTable.addValue("Name experiment", nameExperiment);
                spotsMeasuresTable.addValue(type + " nr", regionID + 1);
                spotsMeasuresTable.addValue("Spot method", "Spotiflow2D");
                spotsMeasuresTable.addValue("Spot ID",spotID++);
                spotsMeasuresTable.addValue("X", x);
                spotsMeasuresTable.addValue("Y", y);
                spotsMeasuresTable.addValue("Intensity", intensity);
                spotsMeasuresTable.addValue("Probability", probability);
                spotsMeasuresTable.incrementCounter();
            }
        }

        resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) model", spotiflowModel);
        resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) probathr", spotiflowMinProbability);
        resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) nr. spots", numberSpot);

        if (numberSpot > 0) {
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean intensity", sumIntensity / numberSpot);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean probability", sumProbability / numberSpot);
        } else {
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean intensity", Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean probability", Double.NaN);
        }

        return roiAll;
    }

    private void findThresholdPerRegion3D(int regionID,Object3DInt regionROI,ResultsTable resultsTableToAdd,String type,ResultsTable spotMeasuresTable){
        ArrayList<Object3DInt> nucleusObjects = new ArrayList<>();
        if(threshold3DObjects==null) {
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) nr. spots",Double.NaN);
            return;
        }
        for(Object3DInt spot : threshold3DObjects.getObjects3DInt()){
            Point3D center = new MeasureCentroid(spot).getCentroidAsPoint();
            VoxelInt voxel = new VoxelInt((int)Math.round(center.getX()),(int)Math.round(center.getY()),(int)Math.round(center.getZ()),1);
            if(containsSafe(regionROI,voxel)) nucleusObjects.add(spot);
        }
        int numberSpot = nucleusObjects.size();
        if(resultsDirectory != null && saveRois && numberSpot > 0){
            RoiManager rm = createThreshold3DRoiManager(nucleusObjects,"Threshold");
            saveRoiManager(rm,resultsDirectory + "/ROI/Spot" + spotName + "/thresholding3D/" + image.getTitle() + "_threshold3D_" + type + (regionID + 1) + "_ROIs.zip","Threshold3D " + type + (regionID + 1) + " ROI");
        }
        double totalVolumePix = 0;
        double totalRawIntDen = 0;
        double voxelVolume = measureCalibration.getPixelArea() * measureCalibration.getPixelsZ();
        ImageHandler imageHandler = ImageHandler.wrap(imageToMeasure);
        int spotID = 1;
        //measure nucleus volume
        MeasureVolume nucleusVolumeMeasure = new MeasureVolume(regionROI);
        double nucleusVolumePix = nucleusVolumeMeasure.getVolumePix();

        for(Object3DInt spot : nucleusObjects){
            MeasureVolume mv = new MeasureVolume(spot);
            double volumePix = mv.getVolumePix();
            MeasureIntensity mi = new MeasureIntensity(spot,imageHandler);

            double rawIntDen = mi.getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
            double mean = mi.getValueMeasurement(MeasureIntensity.INTENSITY_AVG);
            double min = mi.getValueMeasurement(MeasureIntensity.INTENSITY_MIN);
            double max = mi.getValueMeasurement(MeasureIntensity.INTENSITY_MAX);

            Point3D center = new MeasureCentroid(spot).getCentroidAsPoint();
            totalVolumePix += volumePix;
            totalRawIntDen += rawIntDen;
            if(spotMeasuresTable != null){
                spotMeasuresTable.addValue("Name experiment",nameExperiment);
                spotMeasuresTable.addValue(type + " nr",regionID + 1);
                spotMeasuresTable.addValue("Spot method","Threshold3D");
                spotMeasuresTable.addValue("Spot ID",spotID);
                spotMeasuresTable.addValue("Volume (pixel3)",volumePix);
                spotMeasuresTable.addValue("Volume (" + measureCalibration.getUnit() + "3)",volumePix * voxelVolume);
                spotMeasuresTable.addValue("Intensity",mean);
                spotMeasuresTable.addValue("RawIntDen",rawIntDen);
                spotMeasuresTable.addValue("X",center.getX());
                spotMeasuresTable.addValue("Y",center.getY());
                spotMeasuresTable.addValue("Z",center.getZ());
                addAdditional3DSpotMeasures(spot,imageHandler,spotMeasuresTable);
                spotMeasuresTable.incrementCounter();
            }
            spotID++;
        }
        resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) nr. spots",numberSpot);
        if(numberSpot > 0 && totalVolumePix > 0){
            double occupancy = 100.0 * totalVolumePix / nucleusVolumePix;
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) VolumeTotal (pixel³)",totalVolumePix);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) VolumeTotal (" + measureCalibration.getUnit() + "³)",totalVolumePix * voxelVolume);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) VolumeOccupancy (%)", occupancy);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) RawIntDenTotal",totalRawIntDen);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) MeanGlobal",totalRawIntDen / totalVolumePix);
        } else {
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) VolumeTotal (pixel³)",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) VolumeTotal (" + measureCalibration.getUnit() + "³)",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) VolumeOccupancy (%)",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) RawIntDenTotal",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(threshold3D) MeanGlobal",Double.NaN);
        }
    }

    /**
     * Detect and measure spots by searching local maxima
     *
     * @param regionROI         : ROI where the spot have to be detected
     * @param resultsTableToAdd : results table to fill
     * @param type              : image, cell, nucleus or cytoplasm
     */
    private ArrayList<PointRoi> findMaximaPerRegion(int regionID,Object3DInt regionROI,ResultsTable resultsTableToAdd,String type,ResultsTable spotsMeasuresTable){
        ArrayList<PointRoi> nucleusPoints = new ArrayList<>();
        if(maxima3D_maximaList == null){
            resultsTableToAdd.addValue(type + spotName + " Spot(maxima3D) threshold",maxima3D_minThreshold);
            resultsTableToAdd.addValue(type + spotName + " Spot(maxima3D) noise",maxima3D_noise);
            resultsTableToAdd.addValue(type + spotName + " Spot(maxima3D) nr. spots",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(maxima3D) mean intensity",Double.NaN);
            return nucleusPoints;
        }
        int numberSpot = 0;
        int spotID = 1;
        double sumIntensity = 0;


        //IJ.log("find maxima per region : nb spot" + maxima3D_maximaList.size() + " (" + regionID + ") " );
        for(PointRoi spot : maxima3D_maximaList){

            double x = spot.getFloatPolygon().xpoints[0];
            double y = spot.getFloatPolygon().ypoints[0];
            int z = getPointSliceSafe(spot);
            double intensity = Double.parseDouble(spot.getProperty("intensity"));
            VoxelInt point3D = new VoxelInt((int)Math.round(x),(int)Math.round(y),z,(int)intensity);


            if(regionROI != null && !containsSafe(regionROI,point3D)) continue;
            nucleusPoints.add(spot);
            numberSpot++;
            sumIntensity += intensity;
            if(spotsMeasuresTable != null){
                spotsMeasuresTable.addValue("Name experiment",nameExperiment);
                spotsMeasuresTable.addValue(type + " nr",regionID + 1);
                spotsMeasuresTable.addValue("Spot method","Maxima3D");
                spotsMeasuresTable.addValue("Spot ID",spotID++);
                spotsMeasuresTable.addValue("X",x);
                spotsMeasuresTable.addValue("Y",y);
                spotsMeasuresTable.addValue("Z",z);
                spotsMeasuresTable.addValue("Intensity",intensity);
                spotsMeasuresTable.incrementCounter();
            }
        }
        resultsTableToAdd.addValue(type + spotName + " Spot(maxima3D) threshold",maxima3D_minThreshold);
        resultsTableToAdd.addValue(type + spotName + " Spot(maxima3D) noise",maxima3D_noise);
        resultsTableToAdd.addValue(type + spotName + " Spot(maxima3D) nr. spots",numberSpot);
        if(numberSpot > 0) resultsTableToAdd.addValue(type + spotName + " Spot(maxima3D) mean intensity",sumIntensity / numberSpot);
        else resultsTableToAdd.addValue(type + spotName + " Spot(maxima3D) mean intensity",Double.NaN);
        return nucleusPoints;
    }

    private boolean containsSafe(Object3DInt object,VoxelInt voxel){
        if(object == null) return false;
        for(Object3DPlane plane : object.getObject3DPlanes()){
            if(plane.getZPlane() == voxel.getZ()) return plane.contains(voxel);
        }
        return false;
    }



    protected ArrayList<PointRoi> findSpotiflow(int regionID,Object3DInt regionROI,ResultsTable resultsTableToAdd,String type,ResultsTable spotsMeasuresTable){
        ArrayList<PointRoi> nucleusPoints = new ArrayList<>();
        if(spotiflowRois == null){
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) model",spotiflowModel);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) probathr",spotiflowMinProbability);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) nr. spots",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean intensity",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean probability",Double.NaN);
            return nucleusPoints;
        }
        int numberSpot = 0;
        int spotID = 1;
        double sumIntensity = 0;
        double sumProbability = 0;
        for(PointRoi spot : spotiflowRois){
            double x = spot.getFloatPolygon().xpoints[0];
            double y = spot.getFloatPolygon().ypoints[0];
            int z = getPointSliceSafe(spot);
            double intensity = SpotiflowLauncher.getIntensity(spot);
            double probability = SpotiflowLauncher.getProbability(spot);
            VoxelInt point3D = new VoxelInt((int)Math.round(x),(int)Math.round(y),z,(int)intensity);
            if(regionROI != null && !containsSafe(regionROI,point3D)) continue;
            nucleusPoints.add(spot);
            numberSpot++;
            sumIntensity += intensity;
            sumProbability += probability;
            if(spotsMeasuresTable != null){
                spotsMeasuresTable.addValue("Name experiment",nameExperiment);
                spotsMeasuresTable.addValue(type + " nr",regionID + 1);
                spotsMeasuresTable.addValue("Spot method","Spotiflow3D");
                spotsMeasuresTable.addValue("Spot ID",spotID++);
                spotsMeasuresTable.addValue("X",x);
                spotsMeasuresTable.addValue("Y",y);
                spotsMeasuresTable.addValue("Z",z);
                spotsMeasuresTable.addValue("Intensity",intensity);
                spotsMeasuresTable.addValue("Probability",probability);
                spotsMeasuresTable.incrementCounter();
            }
        }
        resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) model",spotiflowModel);
        resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) probathr",spotiflowMinProbability);
        resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) nr. spots",numberSpot);
        if(numberSpot > 0){
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean intensity",sumIntensity / numberSpot);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean probability",sumProbability / numberSpot);
        }else{
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean intensity",Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " Spot(Spotiflow) mean probability",Double.NaN);
        }
        return nucleusPoints;
    }

    private void addAdditional3DSpotMeasures(Object3DInt spot,ImageHandler imageHandler,ResultsTable spotMeasuresTable){
        spot.setVoxelSizeXY(measureCalibration.getPixelArea());
        spot.setVoxelSizeZ(measureCalibration.getPixelsZ());
        spot.setUnit(measureCalibration.getUnit());
        MeasureObject measureObject = new MeasureObject(spot);
        String[] morphoHeadings = MeasureRois.getValidMeasurements3DMorpho(spotMeasurements);
        Double[] morphoValues = measureObject.measureList(morphoHeadings);
        for(int i=0;i<morphoHeadings.length;i++){
            if(morphoHeadings[i].equals(MeasureVolume.VOLUME_PIX)) continue;
            if(morphoHeadings[i].equals(MeasureVolume.VOLUME_UNIT)) continue;
            if(morphoHeadings[i].contains("CX")) continue;
            if(morphoHeadings[i].contains("CY")) continue;
            if(morphoHeadings[i].contains("CZ")) continue;
            spotMeasuresTable.addValue("3D " + morphoHeadings[i],morphoValues[i]);
        }
        String[] intensityHeadings = MeasureRois.getValidMeasurements3DIntensity(spotMeasurements);
        Double[] intensityValues = measureObject.measureIntensityList(intensityHeadings,imageHandler);
        for(int i=0;i<intensityHeadings.length;i++){
            if(intensityHeadings[i].equals(MeasureIntensity.INTENSITY_SUM)) continue;
            if(intensityHeadings[i].equals(MeasureIntensity.INTENSITY_AVG)) continue;
            spotMeasuresTable.addValue("3D " + intensityHeadings[i],intensityValues[i]);
        }
    }


    /**
     * Preprocess image for better segmentation
     * - Projection
     * - Macro
     * - Subtract background
     *
     * @return ImagePlus
     */
    private ImagePlus preprocessing() {
        if (detector.getImage() != null) {
            IJ.run("Options...", "iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
            imageToMeasure = detector.getImageQuantification();
            ImagePlus imageToReturn = detector.getImage();
            //IJ.log("SpotDetector preprocessing imageToMeasure "+imageToMeasure.getNSlices()+" slices");
            //IJ.log("SpotDetector preprocessing imageToReturn "+imageToReturn.getNSlices()+" slices");
            ImagePlus temp;
//      MACRO : apply custom commands of user
            if (macroText != null) {
                IJ.log("SpotDetector macro:" + macroText);
                imageToReturn.show();
                IJ.selectWindow(imageToReturn.getID());
                IJ.runMacro("setBatchMode(true);" + macroText + "setBatchMode(false);");
                temp = WindowManager.getCurrentImage();
                if(temp!=imageToReturn) {
                    imageToReturn.changes=false;
                    imageToReturn.hide();
                    imageToReturn = temp.duplicate();
                }
                temp.changes = false;
                temp.hide();
            }
//            SUBTRACT BACKGROUND : correct background with rolling ball algorithm
            if (useRollingBallSize) {
                ImagePlus tmp= getSubtractBackground(imageToReturn);
                imageToReturn.hide();
                imageToReturn = tmp;
            }
            imageToReturn.setTitle(image.getTitle() + "_" + spotName + "_preprocessed");
            return imageToReturn;
        } else return null;
    }

    /**
     * use rolling ball algorithm to correct background
     *
     * @return image with corrected background
     */
    private ImagePlus getSubtractBackground(ImagePlus image) {
        ImagePlus wobkgIP = image.duplicate();
        detector.renameImage(wobkgIP, "withoutBackground");
        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        backgroundSubtracter.rollingBallBackground(wobkgIP.getProcessor(), rollingBallSize, false, false, false, true, false);
        return wobkgIP;
    }

    private ImagePlus createFindMaximaMask(ImagePlus findMaximaIP) {
        MaximumFinder maximumFinder = new MaximumFinder();
        ImageProcessor findMaximaProc = findMaximaIP.getProcessor().duplicate();
        Polygon maxima = maximumFinder.getMaxima(findMaximaProc, prominence, true);
        PointRoi roiMaxima = new PointRoi(maxima);
        findMaximaIP.setRoi(roiMaxima);
        findMaximaMask = new ImagePlus("find maxima mask", findMaximaIP.createRoiMask());
        findMaximaMask.setRoi(roiMaxima);
        IJ.log("Maxima2D detected " + maxima.npoints + " spots for " + image.getTitle());
        //findMaximaMask.show();
        return findMaximaMask;
    }

    /**
     * The algorithm scan the image to find all values all the pixel of
     *
     * @param findMaximaIP : image to use for finding local maxima
     * @return : pointRoi corresponding to all local maxima
     */
    private PointRoi findMaxima(ImagePlus findMaximaIP, double prominence, String type) {
        MaximumFinder maximumFinder = new MaximumFinder();
        ImageProcessor findMaximaProc = findMaximaIP.getProcessor().duplicate();
        //ImageProcessor debug= findMaximaProc.duplicate();
        //new ImagePlus("debug",debug).show();
        //ImageProcessor debug2 = debug.duplicate();
        Roi roi = findMaximaIP.getRoi();
        if (roi != null) {
            if (roi instanceof PolygonRoi || roi instanceof ShapeRoi) {
                roi = roi.getInverse(findMaximaIP);
                findMaximaProc.setValue(0);
                //System.out.println("debug fill");
                findMaximaProc.fill(roi);
                //new ImagePlus("debug2_" + type, findMaximaProc).show();
            } else {
                System.out.println("not a polygonRoi " + roi.getClass());
            }
            Polygon maxima = maximumFinder.getMaxima(findMaximaProc, prominence, true);
            PointRoi roiMaxima = new PointRoi(maxima);
            findMaximaIP.setRoi(roiMaxima);
            if (showMaximaImage) {
                findMaximaIP.flatten();
                findMaximaIP.show();
            }
            return roiMaxima;
        } else if (type.equals("full")) {
            Polygon maxima = maximumFinder.getMaxima(findMaximaProc, prominence, true);
            PointRoi roiMaxima = new PointRoi(maxima);
            findMaximaIP.setRoi(roiMaxima);
            if (showMaximaImage) {
                findMaximaIP.flatten();
                findMaximaIP.show();
            }
            return roiMaxima;
        } else {
            return new PointRoi();
        }
        //debug2.setRoi((PolygonRoi)findMaximaIP.getRoi());
        //System.out.println("debug setMask");
        //debug2.setMask(((PolygonRoi)findMaximaIP.getRoi()).getMask());


    }

    public ImagePlus getImageToMeasure() {
        if(imageToMeasure==null) {
            IJ.log("SpotDetector imageToMeasure : null give quantification image");
            imageToMeasure=detector.getImageQuantification();
        }
        //IJ.log("SpotDetector imageToMeasure : "+imageToMeasure.getNSlices());
        return imageToMeasure;
    }

    private void saveRoiManager(RoiManager rm,String path,String logName){
        if(rm == null || rm.getCount() == 0) return;
        String outputPath = rm.getCount() == 1 && path.toLowerCase().endsWith(".zip") ? path.substring(0,path.length() - 4) + ".roi" : path;
        if(rm.save(outputPath)) IJ.log(logName + " saved in " + outputPath);
        else IJ.log(logName + " could not be saved in " + outputPath);
    }

    private RoiManager createPointRoiManagerBySlice(ArrayList<PointRoi> points,String prefix,boolean stack){
        RoiManager rm = new RoiManager(false);
        if(points == null || points.isEmpty()) return rm;
        TreeMap<Integer,FloatPolygon> polygonsBySlice = new TreeMap<>();
        for(PointRoi spot : points){
            int z = stack ? getPointSliceSafe(spot) : 1;
            FloatPolygon fpSpot = spot.getFloatPolygon();
            if(fpSpot == null || fpSpot.npoints == 0) continue;
            FloatPolygon fp = polygonsBySlice.get(z);
            if(fp == null){fp = new FloatPolygon();polygonsBySlice.put(z,fp);}
            fp.addPoint(fpSpot.xpoints[0],fpSpot.ypoints[0]);
        }
        for(Integer z : polygonsBySlice.keySet()){
            PointRoi roi = new PointRoi(polygonsBySlice.get(z));
            int nb = roi.getContainedPoints().length;
            roi.setName(stack ? "Spot-" + prefix + "-Z" + z + "-Nb" + nb : "Spot-" + prefix + "-Nb" + nb);
            if(stack) {
                roi.setPosition(z);
                roi.setPosition(0,z,0);
            }
            rm.addRoi(roi);
        }


        return rm;
    }

    public static RoiManager createThreshold3DRoiManager(Collection<Object3DInt> objects, String prefix){
        RoiManager rm = new RoiManager(false);
        if(objects == null || objects.isEmpty()) return rm;
        TreeMap<Integer,ArrayList<Roi>> roisBySlice = new TreeMap<>();
        for(Object3DInt obj : objects){
            Rois3D rois3D = Create3DRoi.computeRois(obj);
            for(Object3DPlane plane : obj.getObject3DPlanes()){
                int z = plane.getZPlane();
                Roi roi = rois3D.getRoi(z);
                if(roi == null) continue;
                roisBySlice.computeIfAbsent(z,k -> new ArrayList<>()).add(roi);
            }
        }
        for(Integer z : roisBySlice.keySet()){
            ShapeRoi global = null;
            for(Roi roi : roisBySlice.get(z)){
                ShapeRoi current = new ShapeRoi(roi);
                if(global == null) global = current;
                else global.or(current);
            }
            if(global == null) continue;
            int nb = roisBySlice.get(z).size();
            global.setPosition(z);
            global.setName("Spot-" + prefix + "-Z" + z + "-Nb" + nb);
            rm.addRoi(global);
        }
        return rm;
    }

    private void saveAllDetectedSpotRois(boolean stack){
        if(resultsDirectory == null || !saveRois) return;
        if(spotByFindMaxima){
            if(stack && maxima3D_maximaList != null){
                RoiManager rm = createPointRoiManagerBySlice(maxima3D_maximaList,"Maxima",true);
                saveRoiManager(rm,resultsDirectory + "/ROI/Spot" + spotName + "/maxima3D/" + image.getTitle() + "_maxima3D_allSpotsDetected.zip","Maxima3D all spots ROI");
            }else if(!stack && findMaximaIP != null){
                findMaximaIP.setRoi((Roi)null);
                PointRoi roiMaxima = findMaxima(findMaximaIP,prominence,"full");
                RoiManager rm = new RoiManager(false);
                if(roiMaxima != null && roiMaxima.getContainedPoints().length > 0) rm.addRoi(roiMaxima);
                saveRoiManager(rm,resultsDirectory + "/ROI/Spot" + spotName + "/findmaxima/" + image.getTitle() + "_findmaxima_allSpotsDetected.zip","Maxima all spots ROI");
            }
        }
        if(spotBySpotiflow && spotiflowRois != null){
            RoiManager rm = createPointRoiManagerBySlice(spotiflowRois,"Spotiflow",stack);
            String folder = stack ? "spotiflow3D" : "spotiflow";
            String suffix = stack ? "_spotiflow3D_allSpotsDetected.zip" : "_spotiflow_allSpotsDetected.zip";
            saveRoiManager(rm,resultsDirectory + "/ROI/Spot" + spotName + "/" + folder + "/" + image.getTitle() + suffix,stack ? "Spotiflow3D all spots ROI" : "Spotiflow all spots ROI");
        }
        if(spotByThreshold){
            if(stack && threshold3DObjects != null){
                RoiManager rm = createThreshold3DRoiManager(threshold3DObjects.getObjects3DInt(),"Threshold");
                saveRoiManager(rm,resultsDirectory + "/ROI/Spot" + spotName + "/thresholding3D/" + image.getTitle() + "_threshold3D_allSpotsDetected.zip","Threshold3D all spots ROI");
            }else if(!stack && thresholdIP != null){
                RoiManager rm = detector.maskToRoiManager(thresholdIP);
                saveRoiManager(rm,resultsDirectory + "/ROI/Spot" + spotName + "/thresholding/" + image.getTitle() + "_threshold_allSpotsDetected.zip","Threshold all spots ROI");
            }
        }
    }


}
