package fr.curie.micmaq.detectors;

import ch.epfl.biop.wrappers.cellpose.CellposeTaskSettings;
import ch.epfl.biop.wrappers.cellpose.DefaultCellposeTask;
import fr.curie.micmaq.helpers.CombineTiles;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.io.FileSaver;
import ij.plugin.RGBStackMerge;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
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
    private final double cellproba_threshold;
    private final String model;
    private final int nucleiChannel;
    private final int cytoChannel;
    private final boolean excludeOnEdges;
    private ImagePlus cellposeMask;
    private RoiManager cellposeRoiManager;

    public static int tileSize=-1;
    public static int tileOverlap=-1;
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
    public CellposeLauncher(ImagePlus imagePlus, int minSizeNucleus, double cellproba_threshold,String model, int cytoChannel, int nucleiChannel, boolean excludeOnEdges) {
        this.imagePlus = imagePlus;
        this.minSizeNucleus = minSizeNucleus;
        this.cellproba_threshold=cellproba_threshold;
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
    public CellposeLauncher(ImagePlus imagePlus, int minSizeNucleus, double cellproba_threshold, String model, boolean excludeOnEdges) {
        this(imagePlus, minSizeNucleus, cellproba_threshold, model, 0, 0, excludeOnEdges);
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
        cellposeMask=runCellpose();
        cellposeRoiManager=label2Roi(cellposeMask);
        cellposeMask = Detector.labeledImage(cellposeMask.getWidth(), cellposeMask.getHeight(), cellposeRoiManager.getRoisAsArray());
        cellposeMask.setTitle(imagePlus.getShortTitle() + "-cellpose");
    }

    /**
     * Launching of cellpose resulting with only a binary mask
     */
    public void analysisWithoutRois() {
        cellposeMask=runCellpose();
    }


    public ImagePlus runCellpose(){
        RoiManager.getRoiManager().reset();
        if(tileSize<0 || (tileSize>=imagePlus.getWidth()&& tileSize>= imagePlus.getHeight())){
            return runCellposeImage(imagePlus);
        }else{
            return runCellposeTiled(imagePlus);
        }
    }

    public ImagePlus runCellposeImage(ImagePlus input){
        DefaultCellposeTask cellposeTask = new DefaultCellposeTask();
        File cellposeTempDir = getCellposeTempDir();
        setSettings(cellposeTask, cellposeTempDir);
        try {
//              Save image in CellposeTempDir
            File t_imp_path = new File(cellposeTempDir, input.getShortTitle() + ".tif");
            FileSaver fs = new FileSaver(input);
            fs.saveAsTiff(t_imp_path.getAbsolutePath());

//              Prepare path for mask output
            File cellpose_imp_path = new File(cellposeTempDir, input.getShortTitle() + "_cp_masks.tif");
//              Prepare path for additional text file created by Cellpose
            File cellpose_outlines_path = new File(cellposeTempDir, input.getShortTitle() + "_cp_outlines.txt");

//            Show debug mode while launching to display the info written by cellpose on the stdout to the log window
            IJ.setDebugMode(true);
            cellposeTask.run();
            IJ.setDebugMode(false);

//              Open output mask file
            ImagePlus cellposeMask = IJ.openImage(cellpose_imp_path.toString());
//

//            Delete images and temp directory
            cellpose_imp_path.delete();
            cellpose_outlines_path.delete();
            cellposeTempDir.delete();
            return cellposeMask;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public ImagePlus runCellposeTiled(ImagePlus input){
        ImagePlus tiles=null;
        if(input.getNChannels()>1){
            tiles=tileChannels(input);
        }else{
            tiles=tileImage(input.getProcessor());
        }
        ImageStack masks=new ImageStack(tileSize, tileSize);
        //tiles.show();

        for(int slice=1;slice<=tiles.getNSlices();slice++){
            IJ.showStatus("run cellpose on tile #"+slice+" / "+tiles.getNSlices());
            if(input.getNChannels()==1){
                tiles.setSlice(slice);
                ImageProcessor tmp= tiles.getProcessor();

                masks.addSlice(runCellposeImage(new ImagePlus("tmp"+slice,tmp)).getProcessor());
            }else{
                tiles.setPosition(1,slice,1);
                ImageProcessor c1=tiles.getProcessor().duplicate();
                tiles.setPosition(2,slice,1);
                ImageProcessor c2=tiles.getProcessor().duplicate();
                ImagePlus composite = RGBStackMerge.mergeChannels(new ImagePlus[]{new ImagePlus("c1",c1), new ImagePlus("c2",c2)}, true);
                masks.addSlice(runCellposeImage(composite).getProcessor());
            }
            IJ.showProgress(slice,tiles.getNSlices());
        }
        IJ.showStatus("combine masks");
        //new ImagePlus("tiles masks",masks).show();

        ImagePlus result= new ImagePlus(input.getTitle()+"TiledCellpose_mask",combineTileImage(masks));
        return result;
    }


    ImagePlus tileChannels(ImagePlus imp){
        ImagePlus[] channels=new ImagePlus[imp.getNChannels()];
        for(int c=0;c<channels.length;c++){
            imp.setC(c+1);
            channels[c]=tileImage(imp.getChannelProcessor());
            //channels[c].show();
        }
        ImagePlus composite = RGBStackMerge.mergeChannels(channels, true);
        return composite;
    }

    ImagePlus tileImage(ImageProcessor ip){
        int step=tileSize-tileOverlap;
        ImageStack is=new ImageStack(tileSize,tileSize);
        for(int y=0;y<ip.getHeight()-tileOverlap;y+=step){
            for(int x=0;x<ip.getWidth()-tileOverlap;x+=step){
                ip.setRoi(x,y,tileSize,tileSize);
                ImageProcessor crop= ip.crop();
                is.addSlice("",crop);
            }
        }
        return new ImagePlus("tiles",is);
    }

    ImageProcessor combineTileImage(ImageStack is){
        int step=is.getWidth()-tileOverlap;
        ImageProcessor result=is.getProcessor(1).createProcessor(imagePlus.getWidth(),imagePlus.getHeight());
        int index=1;
        for(int y=0;y<result.getHeight()-tileOverlap;y+=step){
            for(int x=0;x< result.getWidth()-tileOverlap;x+=step){
                ImageProcessor tmp=is.getProcessor(1).createProcessor(imagePlus.getWidth(),imagePlus.getHeight());
                tmp.copyBits(is.getProcessor(index),x,y, Blitter.MAX);
                label2Roi(new ImagePlus("",is.getProcessor(index)),x,y);
                index++;
            }
        }
        checkDuplicates(RoiManager.getRoiManager());
        rois2Labels(result);
        return result;
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
            case "cyto2_cp3":
            case "cyto3":
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
        settings.setCellProbTh(cellproba_threshold);
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
    public RoiManager label2Roi(ImagePlus cellposeIP) {
        return label2Roi(cellposeIP,0,0);
    }

    public RoiManager label2Roi(ImagePlus cellposeIP, int xoffset, int yoffset) {
        if(cellposeIP==null) System.out.println("error cellposeIP is null!");
        ImageProcessor cellposeProc = cellposeIP.getProcessor().duplicate();
        Wand wand = new Wand(cellposeProc);

//        Set RoiManager
        RoiManager cellposeRoiManager = RoiManager.getRoiManager();
        //cellposeRoiManager.reset();

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
                        Rectangle r = roi.getBounds();

                        roi.setLocation(xoffset+r.x,yoffset+r.y);
                        cellposeRoiManager.addRoi(roi);
                    }
                }
            }
        }
        return cellposeRoiManager;
    }


    public void checkDuplicates(RoiManager roiManager){
        int nRois= roiManager.getCount();
        IJ.log("check duplicate : starting nb rois="+nRois);
        for (int i=nRois-1;i>0;i--){
            boolean toRemove=false;
            for(int j= i-1;j>=0;j--){
                Roi r1=roiManager.getRoi(i);
                Roi r2=roiManager.getRoi(j);
                Point[] points=r1.getContainedPoints();
                double areaR1=r1.getStatistics().pixelCount;
                double areaR2=r2.getStatistics().pixelCount;
                int count=0;
                for(Point p:points){
                    if(r2.contains((int)p.getX(),(int)p.getY())) count++;
                }
                double IoU=((double)count)/(areaR1+areaR2-(double)count);
                if(IoU>0.1) {
                    ShapeRoi s1=new ShapeRoi(r1);
                    ShapeRoi s2=new ShapeRoi(r2);
                    s1.or(s2);
                    roiManager.setRoi(s1,j);
                    toRemove=true;
                }
            }
            if (toRemove) {
                IJ.log("remove roi #"+i);
                roiManager.deselect();
                roiManager.select(i);
                roiManager.runCommand("delete");
            }
        }
        IJ.log("check duplicate : end nb rois="+roiManager.getCount());

    }

    public ImageProcessor rois2Labels(ImageProcessor result){
        RoiManager rm=RoiManager.getRoiManager();
        for (int r=0;r<rm.getCount();r++){
            Roi roi=rm.getRoi(r);
            result.setColor(r+1);
            result.fill(roi);
        }
        return result;
    }


}
