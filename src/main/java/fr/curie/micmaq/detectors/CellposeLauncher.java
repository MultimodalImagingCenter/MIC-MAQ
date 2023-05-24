package fr.curie.micmaq.detectors;

import ch.epfl.biop.wrappers.cellpose.CellposeTaskSettings;
import ch.epfl.biop.wrappers.cellpose.DefaultCellposeTask;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.io.FileSaver;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.*;
import java.io.File;
import java.util.stream.IntStream;

/**
 * Author : Camille RABIER
 * Date : 05/05/2022
 * Class for
 * - segmenting nuclei using cellpose
 *
 * Class based on <a href="https://github.com/BIOP/ijl-utilities-wrappers/blob/master/src/main/java/ch/epfl/biop/wrappers/cellpose/ij2commands/Cellpose_SegmentImgPlusOwnModelAdvanced.java">...</a>
 * It has been simplified for images with only one frame and to be used with or without GUI.
 * It is used to launch cellpose in command line.
 * All parameters are set by default other than the method and diameter and channels to use
 *
 */
public class CellposeLauncher {
    private final ImagePlus imagePlus;
    private final int minSizeNucleus;
    private final String model;
    private final int nucleiChannel;
    private final int cytoChannel;
    private final boolean excludeOnEdges;
    private ImagePlus cellposeMask;
    private RoiManager cellposeRoiManager;
//    CONSTRUCTOR

    /**
     * Constructor for cell segmentation improved by nuclei channel
     *
     * @param imagePlus      : image to analyze -> mono-channel or composite
     * @param minSizeNucleus : diameter used
     * @param model          :  model used (integrated or retrained)
     * @param cytoChannel    : id of channel corresponding to cytoplasm
     *                       cyto channel : 0=grayscale, 1=red, 2=green, 3=blue
     * @param nucleiChannel  : id of channel corresponding to nuclei
     *                       nuclei channel : 0=None (will set to zero), 1=red, 2=green, 3=blue
     * @param excludeOnEdges : choice to exclude cell on edge
     */
    public CellposeLauncher(ImagePlus imagePlus, int minSizeNucleus, String model, int cytoChannel, int nucleiChannel, boolean excludeOnEdges) {
        this.imagePlus = imagePlus;
        this.minSizeNucleus = minSizeNucleus;
        this.model = model;
        this.nucleiChannel = nucleiChannel;
        this.cytoChannel = cytoChannel;
        this.excludeOnEdges = excludeOnEdges;
    }

    /**
     * Constructor for nuclei and cell without nuclei channel
     *
     * @param imagePlus      : image to analyze -> mono-channel or composite
     * @param minSizeNucleus : diameter used
     * @param model          :  model used (integrated or retrained)
     * @param excludeOnEdges : choice to exclude nuclei or cell on edge
     */
    public CellposeLauncher(ImagePlus imagePlus, int minSizeNucleus, String model, boolean excludeOnEdges) {
        this(imagePlus, minSizeNucleus, model, 0, 0, excludeOnEdges);
    }


    /**
     * @return binary mask
     */
    public ImagePlus getCellposeMask() {
        return cellposeMask;
    }

    /**
     * @return RoiManager associated to cellpose output
     */
    public RoiManager getCellposeRoiManager() {
        return cellposeRoiManager;
    }

    /**
     * Launching of cellpose resulting with a binary mask and a RoiManager
     */
    public void analysis() {
        DefaultCellposeTask cellposeTask = new DefaultCellposeTask();
        File cellposeTempDir = getCellposeTempDir();
        System.out.println("cellpose tmp dir: "+cellposeTempDir.getAbsolutePath());
        IJ.log("cellpose segmentation with model : "+model);
        setSettings(cellposeTask, cellposeTempDir);
        try {
//              Save image in CellposeTempDir
            File t_imp_path = new File(cellposeTempDir, imagePlus.getShortTitle() + ".tif");
            FileSaver fs = new FileSaver(imagePlus);
            fs.saveAsTiff(t_imp_path.getAbsolutePath());

//              Prepare path for mask output
            File cellpose_imp_path = new File(cellposeTempDir, imagePlus.getShortTitle() + "_cp_masks.tif");
//              Prepare path for additional text file created by Cellpose
            File cellpose_outlines_path = new File(cellposeTempDir, imagePlus.getShortTitle() + "_cp_outlines.txt");

//            Show debug mode while launching to display the info written by cellpose on the stdout to the log window
            IJ.setDebugMode(true);
            cellposeTask.run();
            IJ.setDebugMode(false);

//              Open output mask file
            ImagePlus cellposeAllRois = IJ.openImage(cellpose_imp_path.toString());
//            Get Rois
            label2Roi(cellposeAllRois);
            cellposeMask = Detector.labeledImage(cellposeAllRois.getWidth(), cellposeAllRois.getHeight(), cellposeRoiManager.getRoisAsArray());
            cellposeMask.setTitle(imagePlus.getShortTitle() + "-cellpose");

//            Delete images and temp directory
            cellpose_imp_path.delete();
            cellpose_outlines_path.delete();
            cellposeTempDir.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set setting for cellpose launch
     * - diameter
     * - model with channel to use
     *
     * @param cellposeTask:   launcher of cellpose
     * @param cellposeTempDir : temporary directory that contains cellpose temporary files
     */
    private void setSettings(DefaultCellposeTask cellposeTask, File cellposeTempDir) {
        CellposeTaskSettings settings = new CellposeTaskSettings();
        settings.setFromPrefs(); /* get info on if to use GPU or CPU and other particularities*/
        System.out.println("cellpose model:"+model);
        switch (model) {
            case "nuclei":
                settings.setChannel1(nucleiChannel);
                //settings.setChannel2(-1) ;
                break;
            case "bact_omni":
                settings.setChannel1(cytoChannel);
                break;
            case "cyto":
            case "cyto2":
            case "cyto2_omni":
                System.out.println("cyto_channel:" + cytoChannel + ";nuclei_channel:" + nucleiChannel);
                settings.setChannel1(cytoChannel);
                settings.setChannel2(nucleiChannel);
                break;
            default:
                settings.setChannel1(cytoChannel);
                settings.setChannel2(nucleiChannel);
                break;
        }
        settings.setModel(model);
        settings.setDatasetDir(cellposeTempDir.toString());
        settings.setDiameter(minSizeNucleus);
        settings.setDiamThreshold(12.0); /*default value*/

        cellposeTask.setSettings(settings);
    }

    /**
     * Created temporary directory to give to cellpose
     *
     * @return temporary directory {@link File}
     */
    private File getCellposeTempDir() {
        String tempDir = IJ.getDirectory("Temp");
        File cellposeTempDir = new File(tempDir, "cellposeTemp");
        if (cellposeTempDir.exists()) {
            File[] contents = cellposeTempDir.listFiles();
            if (contents != null) for (File f : contents) {
                boolean delete = f.delete();
                if (!delete) IJ.error("Files could not be deleted from temp directory");
            }
        } else {
            boolean cellposeMkdir = cellposeTempDir.mkdir();
            if (!cellposeMkdir) {
                IJ.error("The temp directory could not be created");
            }
        }
        return cellposeTempDir;
    }

    /**
     * Based on <a href="https://github.com/BIOP/ijp-LaRoMe/blob/master/src/main/java/ch/epfl/biop/ij2command/Labels2Rois.java">...</a>
     * Simplified for only one frame, one slice and 1 channel
     * Can exclude on edges
     * Creates the RoiManager containing all particle Rois
     */
    public void label2Roi(ImagePlus cellposeIP) {
        if(cellposeIP==null) System.out.println("error cellposeIP is null!");
        ImageProcessor cellposeProc = cellposeIP.getProcessor();
        Wand wand = new Wand(cellposeProc);

//        Set RoiManager
        cellposeRoiManager = RoiManager.getRoiManager();
        cellposeRoiManager.reset();

//        Create range list
        int width = cellposeProc.getWidth();
        int height = cellposeProc.getHeight();

        int[] pixel_width = new int[width];
        int[] pixel_height = new int[height];

        IntStream.range(0, width - 1).forEach(val -> pixel_width[val] = val);
        IntStream.range(0, height - 1).forEach(val -> pixel_height[val] = val);

        /*
         * Will iterate through pixels, when getPixel > 0 ,
         * then use the magic wand to create a roi
         * finally set value to 0 and add to the roiManager
         */

        // will "erase" found ROI by setting them to 0
        cellposeProc.setColor(0);

        for (int y_coord : pixel_height) {
            for (int x_coord : pixel_width) {
                if (cellposeProc.getPixel(x_coord, y_coord) > 0.0) {
                    // use the magic wand at this coordinate
                    wand.autoOutline(x_coord, y_coord);

                    // if there is a region , then it has npoints
//                    There can be problems with very little ROIs, so threshold of 20 points
                    if (wand.npoints > 20) {
                        // get the Polygon, fill with 0 and add to the manager
                        Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);
                        roi.setPosition(cellposeIP.getCurrentSlice());
                        // ip.fill should use roi, otherwise make a rectangle that erases surrounding pixels
                        cellposeProc.fill(roi);
                        if (excludeOnEdges) {
                            Rectangle r = roi.getBounds();
                            if (r.x <= 1 || r.y <= 1 || r.x + r.width >= cellposeProc.getWidth() - 1 || r.y + r.height >= cellposeProc.getHeight() - 1) {
                                continue;
                            }
                        }
                        cellposeRoiManager.addRoi(roi);
                    }
                }
            }
        }
    }
}
