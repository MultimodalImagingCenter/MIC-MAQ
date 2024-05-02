package fr.curie.micmaq.detectors;

import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.LutLoader;
import ij.plugin.ZProjector;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.image.IndexColorModel;

import static ij.IJ.d2s;

/**
 * Created by : Camille RABIER
 * Date : 13/04/2022
 * Class with common methods for spots and nuclei :
 * - Z-projection
 * - Thresholding
 * - Analyze particles
 * - Set results
 */
public class Detector {
    private ImagePlus image;
    private final String nameImage;
    private final String nameObject;
    private MeasureCalibration measureCalibration;

    //Projection parameters
    private String zStackProjMethod;
    private int zStackFirstSlice;
    private int zStackLastSlice;

    //Thresholding parameters
    private String thresholdMethod;
    private boolean excludeOnEdges;
    private double minSizeParticle;
    private Roi[] thresholdRois;

    private String quantifMacro = "";

    private double minThreshold = Integer.MIN_VALUE;
    private double maxThreshold = Integer.MAX_VALUE;

//    CONSTRUCTOR

    /**
     * Constructor with basics
     *
     * @param image      : image to analyze/measure
     * @param nameObject : "Nucleus" or name of protein
     */
    public Detector(ImagePlus image, String nameObject) {
        this.image = image;
//        this.image = image.duplicate();
        this.nameImage = image.getTitle();
        this.nameObject = nameObject;
    }

//    SETTER

    /**
     * @param measureCalibration calibration to use for measurements (conversion pixel to um for example)
     */
    public void setMeasureCalibration(MeasureCalibration measureCalibration) {
        this.measureCalibration = measureCalibration;
    }

    /**
     * if projection to do, sets the necessary parameters and do the projection
     *
     * @param zStackProjMethod : method of projection
     * @param zStackFirstSlice : first slice of stack to use for projection
     * @param zStackLastSlice  : last slice of stack to use for projection
     */
    public void setzStackParameters(String zStackProjMethod, int zStackFirstSlice, int zStackLastSlice) {
        this.zStackProjMethod = zStackProjMethod;
        this.zStackFirstSlice = zStackFirstSlice;
        this.zStackLastSlice = zStackLastSlice;
        //IJ.log("detector : zStack folowed by projection");
        projection();
    }

    /**
     * if thresholding to do, sets the necessary parameters for thresholding and particle analysis
     *
     * @param thresholdMethod : method to get value of threshold
     * @param excludeOnEdges  : excludes particles on edges of selection/image ?
     * @param minSizeParticle : minimum size of particle to analyze
     *                        Maximum size of particle if the max value possible
     */
    public void setThresholdParameters(String thresholdMethod, boolean excludeOnEdges, double minSizeParticle,boolean darkBg) {
        this.thresholdMethod = thresholdMethod + ((darkBg)? " dark":"");
        this.excludeOnEdges = excludeOnEdges;
        this.minSizeParticle = minSizeParticle;
    }

    public void setThresholdParameters(String thresholdMethod, double minThreshold, double maxThreshold, boolean excludeOnEdges,boolean darkBg, double minSizeParticle) {
        this.thresholdMethod = thresholdMethod + ((darkBg)? " dark":"");
        this.excludeOnEdges = excludeOnEdges;
        this.minSizeParticle = minSizeParticle;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
    }

//    GETTER


    /**
     * returns a copy of original image
     * @return image that either does not need a projection or is already projected or null if it needs projection
     */
    public ImagePlus getImage() {
        if (image.getNSlices() > 1) {
            IJ.error("The image " + image + " is a stack, please precise the Z-projection parameters");
            return null;
        } else {
            return image.duplicate();
        }
    }

    /**
     * give the image preprocessed for quantification
     * @return
     */
    public ImagePlus getImageQuantification() {
        if (image.getNSlices() > 1) projection();
        //System.out.println("detector getimage quantif preprocess macro: "+quantifMacro);
        if (quantifMacro != null && !quantifMacro.isEmpty()) {
            ImagePlus imageToReturn = image.duplicate(); /*detector class does the projection if needed*/
            ImagePlus temp;
//      MACRO : apply custom commands of user
            imageToReturn.show();
            IJ.selectWindow(imageToReturn.getID());
            IJ.runMacro("setBatchMode(true);" + quantifMacro + "setBatchMode(false);"); /*accelerates the treatment by displaying only the last image*/
            temp = WindowManager.getCurrentImage();
            imageToReturn = temp.duplicate();
            temp.changes = false;
            temp.close();
            return imageToReturn;
        }
        return image.duplicate();
    }

    public String getQuantifMacro() {
        return quantifMacro;
    }

    public void setQuantifMacro(String quantifMacro) {
        this.quantifMacro = quantifMacro;
    }

    //    FUNCTIONS/METHODS

    /**
     * If the original image is a stack, projects it in one image for further analysis
     * The projections proposed are maximum projection or standard deviation projection
     */
    private void projection() {
        if (this.image.getNSlices() > 1) {
            IJ.log("detector projection " + zStackProjMethod);
            if (zStackProjMethod.equals("Maximum projection")) {
                this.image = ZProjector.run(image, "max", zStackFirstSlice, zStackLastSlice); /*projects Stack to only one image by maximal intensity projection*/
            } else {
                this.image = ZProjector.run(image, "sd", zStackFirstSlice, zStackLastSlice); /*projects Stack to only one image by standard deviation projection*/
            }
            renameImage(this.image, "projection");
        } else {
            //IJ.log("The image "+ this.image+" is not a stack.");
        }
    }


    /**
     * Analyze the binary image to detect particles (here spots or nuclei)
     * It scans the image/selection until it finds the edge on an object(different color)
     * Then it outlines the object using the wand tool and measures it.
     * It fills the found object to make it invisible, so it is not detected another time.
     * The scan ends when the end of image or selection is reached
     *
     * @param threshold_IP : binary image with particles to find
     * @return RoiManager that contains the Rois corresponding to the particles found
     */
    public RoiManager analyzeParticles(ImagePlus threshold_IP) {
//        Get RoiManager
        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager == null) { /*if no instance of roiManager, creates one*/
            roiManager = new RoiManager();
        } else { /*if already exists, empties it*/
            roiManager.reset();
        }

//        Set options for particle analyzer
        ParticleAnalyzer.setRoiManager(roiManager); /*precise with RoiManager to use*/
        int analyzer_option = ParticleAnalyzer.SHOW_OVERLAY_MASKS + ParticleAnalyzer.ADD_TO_MANAGER;
        if (excludeOnEdges) analyzer_option += ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(analyzer_option, 0, null, minSizeParticle, Integer.MAX_VALUE);

//        Analyze
        ImageProcessor threshold_proc = threshold_IP.getProcessor();
        threshold_proc.setAutoThreshold("Default dark");
//        threshold_proc.setThreshold(128,255); /*Needs to set threshold for the binary image*/
        particleAnalyzer.analyze(threshold_IP); /*adds particles found to RoiManager and add overlay (see options)*/
        return roiManager;
    }

    /**
     * Creates a binary image to differentiate the objects to analyze
     *
     * @param image : image to threshold
     * @return binary image
     */
    public ImagePlus getThresholdMask(ImagePlus image) {
//        GET IMAGE TO THRESHOLD
        ImagePlus threshold_IP = image.duplicate();
        ImageProcessor threshold_proc = threshold_IP.getProcessor(); /*get processor*/

//        DEFINE THRESHOLD VALUES THROUGH METHOD GIVEN
        if (!thresholdMethod.startsWith("user")) {
            threshold_proc.setAutoThreshold(thresholdMethod);
        } else {
            threshold_proc.setThreshold(minThreshold, maxThreshold);
            IJ.log("set User thresholds to : " + minThreshold + ", " + maxThreshold);
        }
//        GET BINARY MASK OF THRESHOLD IMAGE THROUGH PARTICLE ANALYZER
//        Set options
        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager == null) {
            roiManager = new RoiManager();
        } else {
            roiManager.reset();
        }
        int analyzer_option = /*ParticleAnalyzer.SHOW_MASKS+*/ParticleAnalyzer.ADD_TO_MANAGER;
        if (excludeOnEdges) analyzer_option += ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
//        Analyze
        ParticleAnalyzer.setRoiManager(roiManager);
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(analyzer_option, 0, null, minSizeParticle, Integer.MAX_VALUE);
        particleAnalyzer.analyze(threshold_IP);
        thresholdRois = roiManager.getRoisAsArray();
//        Get binary mask output and renames it
        ImagePlus mask_IP = binaryImage();
        renameImage(mask_IP, "binary_mask");
        return mask_IP;
    }

    /**
     * same as Detector#getThresholdMask(ImagePlus) without RoiManager stuff
     * @param image
     * @return
     * @see Detector#getThresholdMask(ImagePlus)
     */
    public ImagePlus getThresholdMaskWithoutManager(ImagePlus image) {

//        GET IMAGE TO THRESHOLD
        ImagePlus threshold_IP = image.duplicate();
        ImageProcessor threshold_proc = threshold_IP.getProcessor(); /*get processor*/

//        DEFINE THRESHOLD VALUES THROUGH METHOD GIVEN
        if (!thresholdMethod.startsWith("user")) {
            threshold_proc.setAutoThreshold(thresholdMethod);
        } else {
            threshold_proc.setThreshold(minThreshold, maxThreshold);
            IJ.log("set User thresholds to : " + minThreshold + ", " + maxThreshold);
        }
//        GET BINARY MASK OF THRESHOLD IMAGE THROUGH PARTICLE ANALYZER
//        Set options
        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager == null) {
            roiManager = new RoiManager(false);
        } else {
            roiManager.reset();
        }
        int analyzer_option = /*ParticleAnalyzer.SHOW_MASKS+*/ParticleAnalyzer.ADD_TO_MANAGER;
        if (excludeOnEdges) analyzer_option += ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
//        Analyze
        ParticleAnalyzer.setRoiManager(roiManager);
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(analyzer_option, 0, null, minSizeParticle, Integer.MAX_VALUE);
        particleAnalyzer.analyze(threshold_IP);
        thresholdRois = roiManager.getRoisAsArray();
//        Get binary mask output and renames it
        ImagePlus mask_IP = binaryImage();
        thresholdRois = null;
        roiManager.reset();
        renameImage(mask_IP, "binary_mask");
        return mask_IP;
    }

    /**
     * Uses the ROIs found in getThresholdMask (thresholdRois)
     *
     * @param rois : region of all the objects segmented
     * @return ImagePlus with an intensity per object
     */
    public ImagePlus labeledImage(Roi[] rois) {
        ImagePlus ip = NewImage.createShortImage("labeledImage", image.getWidth(), image.getHeight(), 1, NewImage.FILL_BLACK);
        int color = 1;
        for (Roi points : rois) {
            ip.getProcessor().setColor(color);
            if (points != null) {
                ip.getProcessor().fill(points);
                color++;
            }
        }
        renameImage(ip, nameObject + "_labeled_mask");
        return ip;
    }

    /**
     * Do the same as the previous labeledImage, but without renaming the image and in static.
     *
     * @param imageHeight : height of segmented image
     * @param imageWidth  : width of segmented image
     * @param rois        : : region of all the objects segmented
     * @return image with an intensity per object
     */
    public static ImagePlus labeledImage(int imageWidth, int imageHeight, Roi[] rois) {
        ImagePlus imagePlus = NewImage.createShortImage("labeledImage", imageWidth, imageHeight, 1, NewImage.FILL_BLACK);
        for (int i = 0; i < rois.length; i++) {
            imagePlus.getProcessor().setColor(i + 1);
            imagePlus.getProcessor().fill(rois[i]);
        }
        return imagePlus;
    }

    /**
     * Uses the ROIs detected to create binary image (with same dimension)
     *
     * @return Binary image
     */
    private ImagePlus binaryImage() {
        ImagePlus binaryMask = NewImage.createByteImage("binaryMask", image.getWidth(), image.getHeight(), 1, NewImage.FILL_BLACK);
        for (Roi rois : thresholdRois) {
            binaryMask.getProcessor().setColor(255);
            binaryMask.getProcessor().fill(rois);
        }
        return binaryMask;
    }

    /**
     * @param binary_threshold_proc : processor of mask, already ByteProcessor
     * @return ImagePlus corresponding to watershed binary image
     */
    public ImagePlus getWatershed(ImageProcessor binary_threshold_proc) {
        EDM edm = new EDM();
        edm.toWatershed(binary_threshold_proc);
        return new ImagePlus(nameImage + "_watershed", binary_threshold_proc);
    }

    /**
     * Mean an array
     *
     * @param values : array of values
     * @return the mean of the array
     */
    public double mean(double[] values) {
        return sum(values) / values.length;
    }

    /**
     * Renames image with original image prefix (without extension)
     * + modification identifier suffix (+if present, the extension)
     *
     * @param imageToRename : ImagePlus that will be renamed
     * @param toAdd         : suffix to add
     */
    public void renameImage(ImagePlus imageToRename, String toAdd) {
        int lastPoint = nameImage.lastIndexOf("."); /*get position of last point*/
        String getTitleWOExtension;
        String extension;
        if (lastPoint != -1) { /*a point is present in the string*/
            getTitleWOExtension = nameImage.substring(0, lastPoint);
            extension = nameImage.substring(lastPoint);
            imageToRename.setTitle(getTitleWOExtension + "_" + toAdd + extension);
        } else { /*no extension to add*/
            imageToRename.setTitle(nameImage + "_" + toAdd);
        }
    }

    public void setLUT(ImagePlus imp) {
        IndexColorModel cm = LutLoader.getLut("3-3-2 RGB");
        imp.getProcessor().resetMinAndMax();
        if (imp.getNSlices() > 1) {
            imp.getImageStack().setColorModel(cm);
            imp.getProcessor().setColorModel(cm);
        } else {
            imp.getProcessor().setColorModel(cm);
        }

        imp.updateAndRepaintWindow();
    }


    /**
     * Add results from a ResultTable to another for clearer headings and adding of calibrated area.
     * Removes IntDen results, as it is equal to RawIntDen value
     *
     * @param rawMeasures    : ResultTable with all the results for all the nucleus
     * @param customMeasures : ResultTable that will contain the final results
     * @param nucleus        : line number of the nucleus for which the results have to be customized
     */
    public void setResultsAndRename(ResultsTable rawMeasures, ResultsTable customMeasures, int nucleus, String preNameColumn) {
        for (String measure : rawMeasures.getHeadings()) {
            if (measure.equals("Area")) {
                if (nucleus == -1) {
                    customMeasures.addValue(preNameColumn + " " + measure + " (pixel)", Double.NaN);
                    customMeasures.addValue(preNameColumn + " " + measure + " (" + measureCalibration.getUnit() + ")", Double.NaN);
                } else {
                    customMeasures.addValue(preNameColumn + " " + measure + " (pixel)", d2s(rawMeasures.getValue(measure, nucleus)));
                    customMeasures.addValue(preNameColumn + " " + measure + " (" + measureCalibration.getUnit() + ")", d2s(rawMeasures.getValue("Area", nucleus) * measureCalibration.getPixelArea()));
                }
            } else if (!measure.equals("IntDen")) {
                if (nucleus == -1) {
                    customMeasures.addValue(preNameColumn + " " + measure, Double.NaN);
                } else {
                    customMeasures.addValue(preNameColumn + " " + measure, d2s(rawMeasures.getValue(measure, nucleus)));
                }
            }
        }
    }

    public void setNullResultsAndRename(ResultsTable rawMeasures, ResultsTable customMeasures, String preNameColumn) {
        for (String measure : rawMeasures.getHeadings()) {
            if (measure.equals("Area")) {
                customMeasures.addValue(preNameColumn + " " + measure + " (pixel)", d2s(0));
                customMeasures.addValue(preNameColumn + " " + measure + " (" + measureCalibration.getUnit() + ")", d2s(0));
            } else if (measure.equals("IntDen")) {
                continue;
            } else {
                customMeasures.addValue(preNameColumn + " " + measure, d2s(0));
            }
        }
    }

    /**
     * Summarize results : from an entire ResultsTable create one line for another resultTable
     * For area, adding of with calibration column
     * Removal of IntDen column (RawIntDen is equal)
     * Differentiation between mean of mean and arithmetic mean
     * RawIntDen and Area are summed, the other use means
     *
     * @param rawMeasures    : ResultTable with measures of all spots not summarized
     * @param customMeasures : ResultTable that will contain the summarized results
     * @param prenameColumn  : prefix for column
     */
    public void setSummarizedResults(ResultsTable rawMeasures, ResultsTable customMeasures, String prenameColumn) {
        for (String measure : rawMeasures.getHeadings()) {
            switch (measure) {
                case "Area":
                    double sumArea = sum(rawMeasures.getColumn(measure));
                    customMeasures.addValue(prenameColumn + " threshold " + measure + " (pixel)", d2s(sumArea));
                    customMeasures.addValue(prenameColumn + " threshold " + measure + " (" + measureCalibration.getUnit() + ")", d2s(sumArea * measureCalibration.getPixelArea()));
                    break;
                case "IntDen":
                    continue;
                case "Mean":
                    break;
                case "RawIntDen":
                    customMeasures.addValue(prenameColumn + " threshold " + measure, d2s(sum(rawMeasures.getColumn(measure))));
                default:
                    customMeasures.addValue(prenameColumn + " threshold " + measure, d2s(mean(rawMeasures.getColumn(measure))));
                    break;
            }
        }
        /*Arithmetic mean*/
        customMeasures.addValue(prenameColumn + " threshold Mean", d2s(sum(rawMeasures.getColumn("RawIntDen")) / sum(rawMeasures.getColumn("Area"))));
    }

    /**
     * Sum an array
     *
     * @param values : array of values
     * @return sum of the array
     */
    public double sum(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum;
    }
}
