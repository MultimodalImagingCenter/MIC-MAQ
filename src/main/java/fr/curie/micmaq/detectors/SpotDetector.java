package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.*;
import ij.io.RoiEncoder;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.LutLoader;
import ij.plugin.OverlayLabels;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;

import java.awt.*;
import java.io.File;

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
    private boolean spotByFindMaxima;
    private double prominence;

    //    --> threshold
    private boolean spotByThreshold;
    private ImagePlus thresholdIP;
    private boolean useWatershed;
    private final Detector detector;

    private int measurements = Measurements.MEAN + Measurements.INTEGRATED_DENSITY;

    private int spotMeasurements =Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY+Measurements.SHAPE_DESCRIPTORS+Measurements.ELLIPSE+Measurements.FERET;



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
        File dir = new File(resultsDirectory);
        if (!dir.exists()) dir.mkdirs();

        this.rollingBallSize = 0;
        this.useRollingBallSize = false;
        this.spotByThreshold = false;
        this.spotByFindMaxima = false;
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
        this.spotName = spotName;
        this.nameExperiment = "livePreview";
        this.resultsDirectory = "";
        showPreprocessedImage = false;
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
     * @see Detector#setThresholdParameters(String, boolean, double)
     */
    public void setSpotByThreshold(String thresholdMethod, double minTreshold, double maxThreshold, double minSizeSpot, boolean useWatershed, boolean darkBg, boolean showThresholdImage) {
        this.spotByThreshold = true;
        this.showThresholdImage = showThresholdImage;
        this.useWatershed = useWatershed;
        //macroText = null;
        detector.setThresholdParameters(thresholdMethod, minTreshold, maxThreshold, false, darkBg, minSizeSpot); /*does not exclude spot on edges*/
    }


    /**
     * Set saving's choice
     *
     * @param saveImage : save find maxima or threshold image
     * @param saveRois  : save corresponding regions
     */
    public void setSaving(boolean saveImage, boolean saveRois) {
        this.saveImage = saveImage;
        this.saveRois = saveRois;
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

    /**
     * live preview for find maxima approach
     * @param prominence
     */
    public void livePreviewFindMaxima(double prominence,boolean show) {
        if (livePreview == null) livePreview = preprocessing();
        livePreview.show();
        Roi old=livePreview.getRoi();
        livePreview.resetRoi();
        livePreview.setHideOverlay(true);
        if(show) {
            PointRoi tmp = findMaxima(livePreview, prominence, "full");
            IJ.log("live preview (" + prominence + ") nb points:" + tmp.getPolygon().npoints);
            //livePreview.setRoi(tmp, true);
            livePreview.setOverlay(new Overlay(tmp));
        }
        livePreview.setRoi(old);
        livePreview.updateAndRepaintWindow();
    }

    /**
     * live preview for Threhold approach. needs to set parameters by using setSpotByThreshold method
     *
     */
    public void livePreviewThreshold() {
        if (livePreview == null) {
            livePreview = preprocessing();
            autocontrast(livePreview);
        }
        Roi old=livePreview.getRoi();
        livePreview.resetRoi();
        livePreview.show();

        ImagePlus test = livePreview.duplicate();
        ImagePlus tmp = detector.getThresholdMaskWithoutManager(test);
        if (useWatershed) {
            tmp = detector.getWatershed(tmp.getProcessor());
            detector.renameImage(tmp, "watershed");
        }
        tmp.setLut(new LUT(LutLoader.getLut("Red"), tmp.getProcessor().getMin(), tmp.getProcessor().getMax()));
        Roi roi = new ImageRoi(0, 0, tmp.getProcessor());
        ((ImageRoi) roi).setZeroTransparent(true);
        livePreview.setOverlay(null);
        livePreview.getCanvas().setShowAllList(null);

        //livePreview.setRoi(roi);
        livePreview.setOverlay(new Overlay(roi));
        //test.setOverlay(roi,Color.RED,1,Color.RED);
        livePreview.setRoi(old);
        livePreview.updateAndRepaintWindow();

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
        livePreview.close();
        livePreview = null;
    }

    /**
     *
     */
    public void preview() {
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
                /*Find maxima*/
                findMaxima(findMaximaIP, prominence, "preview");
            }
        }
    }

    /**
     * Prepare for measurement
     * - does the preprocessing
     * - if selected by user show preprocessing image
     * - detect spot in the entire image (by threshold and/or find maxima)
     * - if selected by user : show masks with spot detected by chosen method(s)
     *
     * @return true if no error
     */
    public boolean prepare() {
        //check saving directory
        if (saveRois) {
            File tmp = new File(resultsDirectory + "/ROI/Spot" + spotName + "/");
            if (!tmp.exists()) tmp.mkdirs();
            if (spotByThreshold) {
                tmp = new File(resultsDirectory + "/ROI/Spot" + spotName + "/thresholding/");
                if (!tmp.exists()) tmp.mkdirs();
            }
            if (spotByFindMaxima) {
                tmp = new File(resultsDirectory + "/ROI/Spot" + spotName + "/findmaxima/");
                if (!tmp.exists()) tmp.mkdirs();
            }
        }
        if (saveImage) {
            File tmp = new File(resultsDirectory + "/Images/Spot" + spotName + "/");
            if (!tmp.exists()) tmp.mkdirs();
            if (spotByThreshold) {
                tmp = new File(resultsDirectory + "/Images/Spot" + spotName + "/thresholding");
                if (!tmp.exists()) tmp.mkdirs();
            }
            if (spotByFindMaxima) {
                tmp = new File(resultsDirectory + "/Images/Spot" + spotName + "/findmaxima");
                if (!tmp.exists()) tmp.mkdirs();
            }
        }
//        Preprocessing
        ImagePlus preprocessed = preprocessing();
        if (preprocessed != null) {
            if (showPreprocessedImage) {
                preprocessed.show();
            }
//            Detection by threshold
            if (spotByThreshold) {
                thresholdIP = detector.getThresholdMask(preprocessed);
                if (useWatershed) {
                    thresholdIP = detector.getWatershed(thresholdIP.getProcessor());
                    detector.renameImage(thresholdIP, "watershed");
                }
                if (showThresholdImage) {
                    thresholdIP.show();
                }
                if (resultsDirectory != null && saveImage) {
                    if (IJ.saveAsTiff(thresholdIP, resultsDirectory + "/Images/Spot" + spotName + "/thresholding/" + thresholdIP.getTitle())) {
                        IJ.log("The spot binary mask " + thresholdIP.getTitle() + " was saved in " + resultsDirectory + "/Images/Spot" + spotName + "/thresholding/");
                    } else {
                        IJ.log("The spot binary mask " + thresholdIP.getTitle() + " could not be saved in  " + resultsDirectory + "/Images/Spot" + spotName + "/thresholding/");
                    }
                }
            }
//            Detection by find maxima
            if (spotByFindMaxima) {
                findMaximaIP = preprocessed.duplicate();
                detector.renameImage(findMaximaIP, "maxima");
                createFindMaximaMask(findMaximaIP);
                if (resultsDirectory != null) {
                    if (saveRois) {
                        findMaximaIP.setRoi((Roi) null);
                        PointRoi roiMaxima = findMaxima(findMaximaIP, prominence, "full");
                        findMaximaIP.setRoi(roiMaxima);
                        boolean wasSaved = RoiEncoder.save(roiMaxima, resultsDirectory + "/ROI/Spot" + spotName + "/findmaxima/" + image.getTitle() + "_findMaxima_all_roi.roi");
                        if (!wasSaved) {
                            IJ.error("Could not save ROIs");
                        } else {
                            IJ.log("The ROIs of the spot found by find Maxima method of " + image.getTitle() + " were saved in " + resultsDirectory + "/ROI/Spot" + spotName + "/" + image.getTitle() + "findMaxima_all_roi.roi");
                        }
                    }
                    if (saveImage) {
                        ImagePlus toSave = findMaximaIP.flatten();
                        if (IJ.saveAsTiff(toSave, resultsDirectory + "/Images/Spot" + spotName + "/findmaxima/" + findMaximaIP.getTitle())) {
                            IJ.log("The find maxima spots image " + findMaximaIP.getTitle() + " was saved in " + resultsDirectory + "/Images/Spot" + spotName + "/findmaxima/" + findMaximaIP.getTitle());
                        } else {
                            IJ.log("The find maxima spots image " + findMaximaIP.getTitle() + " could not be saved in " + resultsDirectory + "/Images/Spot" + spotName + "/findmaxima/");
                        }
                    }
                }
            }
            return true;
        } else return false;
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
        if (spotByFindMaxima) {
            findMaximaPerRegion(regionROI, resultsTableFinal, type);
        }
        if (spotByThreshold) {
            findThresholdPerRegion(regionID, regionROI, resultsTableFinal, type,spotsMeasuresTable);
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
//        Detection
        ImageProcessor tmpIP = thresholdIP.getProcessor().duplicate();
        if (regionROI != null) {
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
        resultsTableToAdd.addValue(type + spotName + " threshold nr. spots", numberSpot);
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
                for(int r=0;r<resultsTable.getCounter();r++){
                    spotMeasuresTable.addValue("Name experiment",nameExperiment);
                    spotMeasuresTable.addValue(type+" nr",regionID);
                    for(int c=0;c<headings.length;c++)
                        spotMeasuresTable.addValue(headings[c],resultsTable.getValue(headings[c],r));
                    spotMeasuresTable.incrementCounter();
                }
            }
            detector.setSummarizedResults(resultsTable, resultsTableToAdd, type  + spotName);
        } else {
            resultsTableToAdd.addValue(type + spotName + " threshold Area (pixel)", Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " threshold Area (" + measureCalibration.getUnit() + ")", Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " threshold Mean", Double.NaN);
            resultsTableToAdd.addValue(type + spotName + " threshold RawIntDen", Double.NaN);
        }
    }

    /**
     * Detect and measure spots by searching local maxima
     *
     * @param regionROI         : ROI where the spot have to be detected
     * @param resultsTableToAdd : results table to fill
     * @param type              : image, cell, nucleus or cytoplasm
     */
    private void findMaximaPerRegion(Roi regionROI, ResultsTable resultsTableToAdd, String type) {
        ImagePlus tmp = (findMaximaMask != null) ? findMaximaMask : findMaximaIP;
        double prom = (findMaximaMask != null) ? 10 : prominence;
        tmp.setRoi(regionROI);
//                    Find maxima

        PointRoi roiMaxima = findMaxima(tmp, prom, type);
//                    Get statistics
        int size = 0;
        float mean = 0;
        for (Point p : roiMaxima) {
            size++;
            mean += findMaximaIP.getProcessor().getPixelValue(p.x, p.y);
        }
        mean = mean / size;
        resultsTableToAdd.addValue(type + spotName + " maxima prominence", prominence);
        resultsTableToAdd.addValue(type + spotName + " maxima nr. spots", size);
        resultsTableToAdd.addValue(type + spotName + " maxima mean", mean);
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
                    imageToReturn.close();
                    imageToReturn = temp.duplicate();
                    temp.changes = false;
                    temp.close();
                }
            }
//            SUBTRACT BACKGROUND : correct background with rolling ball algorithm
            if (useRollingBallSize) {
                imageToReturn = getSubtractBackground();
            }
            return imageToReturn;
        } else return null;
    }

    /**
     * use rolling ball algorithm to correct background
     *
     * @return image with corrected background
     */
    private ImagePlus getSubtractBackground() {
        ImagePlus wobkgIP = imageToMeasure.duplicate();
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
            imageToMeasure=detector.getImageQuantification();
        }
        return imageToMeasure;
    }


}
