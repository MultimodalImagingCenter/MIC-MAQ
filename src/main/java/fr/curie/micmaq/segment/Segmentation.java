package fr.curie.micmaq.segment;

import fr.curie.micmaq.config.ImageProvider;
import fr.curie.micmaq.config.ImageSet;
import fr.curie.micmaq.detectors.CellposeLauncher;
import fr.curie.micmaq.detectors.NucleiDetector;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.plugin.LutLoader;
import ij.plugin.ZProjector;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.image.IndexColorModel;

public class Segmentation {
    SegmentationParameters params;
    Roi[] rois;
    RoiManager roiManager;
    boolean showBinaryImage=false;

    public Segmentation(SegmentationParameters params){
        this.params=params;
    }

    public Segmentation createSegmentation(){
        return new Segmentation(params);
    }

    public Roi[] getRois() {


        return rois;
    }

    public static Object[] segment(ImagePlus imp, SegmentationParameters params){
        Segmentation seg=new Segmentation(params);
        ImagePlus preprocessed=seg.preprocess(imp);
        ImagePlus mask=seg.segmentation(preprocessed);
        Object[] results=new Object[2];
        results[0]=mask;
        results[1]=seg.getRois();
        return results;
    }

    public ImagePlus segmentation(ImagePlus imp){
        if(params.getMethod()==SegmentationParameters.CELLPOSE) return cellposeSegmentation(imp);
        if(params.getMethod()==SegmentationParameters.THRESHOLDING) return thresholdingSegmentation(imp);
        return null;
    }

    /**
     * segmentation via cellpose
     * @param imp image to segment
     * @return instance segmentation mask
     */
    public ImagePlus cellposeSegmentation(ImagePlus imp){
        CellposeLauncher cellposeLauncher = new CellposeLauncher(imp, params.cellposeDiameter, params.cellposeCellproba_trheshold, params.cellposeModel, params.excludeOnEdge);
        cellposeLauncher.analysis();
        ImagePlus labeledImage = cellposeLauncher.getCellposeMask();
        renameImage(imp,labeledImage,"cellpose");
        roiManager = cellposeLauncher.getCellposeRoiManager();
        return labeledImage;
    }

    /**
     * segmentation via thresholding
     * @param imp image to segment
     * @return instance segmentation mask
     */
    public ImagePlus thresholdingSegmentation(ImagePlus imp){
        ImagePlus imageToParticleAnalyze=thresholding(imp);
        // Analyse particle
        roiManager =analyzeParticles(imageToParticleAnalyze);
        return labeledImage(imp,roiManager.getRoisAsArray());
    }

    /**
     * preprocessing as defined in params
     * @param imp image to work on
     * @return preprossed image
     */
    public ImagePlus preprocess(ImagePlus imp){
        if (imp!=null){
            IJ.run("Options...","iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
            ImagePlus imageToReturn = (params.isZproject())?projection(imp):imp.duplicate();
            ImagePlus temp;
//      MACRO : apply custom commands of user
            if (params.getPreprocessMacro()!=null){
                imageToReturn.show();
                IJ.selectWindow(imageToReturn.getID());
                IJ.runMacro("setBatchMode(true);"+params.getPreprocessMacro()+"setBatchMode(false);"); /*accelerates the treatment by displaying only the last image*/
                temp = WindowManager.getCurrentImage();
                imageToReturn = temp.duplicate();
                temp.changes=false;
                temp.close();
            }
            return imageToReturn;
        }else return null;
    }

    /**
     * If the original image is a stack, projects it in one image for further analysis
     * The projections proposed are maximum projection or standard deviation projection
     */
    private ImagePlus projection(ImagePlus image) {
        if (image.getNSlices()>1){
            String projMet=(params.getProjectionMethod()==SegmentationParameters.PROJECTION_MAX)?"max":"sd";
            int zStackFirstSlice=Math.max(params.getProjectionSliceMin(),1);
            int zStackLastSlice=Math.min(params.getProjectionSliceMin(),image.getNSlices());
            ImagePlus result=ZProjector.run(image, projMet, zStackFirstSlice, zStackLastSlice);

            renameImage(image,result,"projection");
            return result;
        }
        return image;
    }

    /**
     * Renames image with original image prefix (without extension)
     * + modification identifier suffix (+if present, the extension)
     * @param imageToRename : ImagePlus that will be renamed
     * @param toAdd : suffix to add
     */
    public void renameImage(ImagePlus originaImage, ImagePlus imageToRename, String toAdd){
        FileInfo fi=originaImage.getFileInfo();
        if(fi==null) fi=originaImage.getOriginalFileInfo();
        if(fi==null){
            imageToRename.setTitle(toAdd);
            return;
        }
        String nameImage=fi.fileName;
        int lastPoint = nameImage.lastIndexOf("."); /*get position of last point*/
        String getTitleWOExtension;
        String extension;
        if (lastPoint != -1){ /*a point is present in the string*/
            getTitleWOExtension = nameImage.substring(0,lastPoint);
            extension = nameImage.substring(lastPoint);
            imageToRename.setTitle(getTitleWOExtension +"_"+toAdd+extension);
        }else { /*no extension to add*/
            imageToRename.setTitle(nameImage +"_"+toAdd);
        }
    }

    /**
     * If useThreshold, prepare threshold image
     * @param imagePlus : image to segment
     */
    public ImagePlus thresholding(ImagePlus imagePlus){
//        OBTAIN BINARY MASK OF THRESHOLD IMAGE
        ImagePlus mask_IP = getThresholdMask(imagePlus);

        if (showBinaryImage){
            mask_IP.show();
        }

        if (params.isThresholdingWatershed()){
            ImagePlus watershed_mask = getWatershed(mask_IP.getProcessor());
            renameImage(imagePlus,watershed_mask,"watershed");
            if (showBinaryImage){
                watershed_mask.show();
            }
            return watershed_mask;
        }else {
            return mask_IP;
        }
    }

    /**
     * Creates a binary image to differentiate the objects to analyze
     * @param image : image to threshold
     * @return binary image
     */
    public ImagePlus getThresholdMask(ImagePlus image) {
//        GET IMAGE TO THRESHOLD
        ImagePlus threshold_IP= image.duplicate();
        ImageProcessor threshold_proc = threshold_IP.getProcessor(); /*get processor*/

//        DEFINE THRESHOLD VALUES THROUGH METHOD GIVEN
        threshold_proc.setAutoThreshold(params.thresholdMethod+" dark");
//        GET BINARY MASK OF THRESHOLD IMAGE THROUGH PARTICLE ANALYZER
//        Set options
        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager==null){
            roiManager=new RoiManager();
        }else {
            roiManager.reset();
        }
        int analyzer_option = /*ParticleAnalyzer.SHOW_MASKS+*/ParticleAnalyzer.ADD_TO_MANAGER;
        if (params.excludeOnEdge) analyzer_option+= ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
//        Analyze
        ParticleAnalyzer.setRoiManager(roiManager);
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(analyzer_option,0,null,params.minSize,Integer.MAX_VALUE);
        particleAnalyzer.analyze(threshold_IP);
        rois = roiManager.getRoisAsArray();
//        Get binary mask output and renames it
        ImagePlus mask_IP= binaryImage(image,rois);
        renameImage(image, mask_IP,"binary_mask");
        return mask_IP;
    }

    /**
     * Uses the ROIs detected to create binary image (with same dimension as image)
     * @param image original image for size
     * @return Binary image
     */
    private ImagePlus binaryImage(ImagePlus image, Roi[] rois){
        ImagePlus binaryMask = NewImage.createByteImage("binaryMask",image.getWidth(),image.getHeight(),1,NewImage.FILL_BLACK);
        for (Roi roi : rois) {
            binaryMask.getProcessor().setColor(255);
            binaryMask.getProcessor().fill(roi);
        }
        return binaryMask;
    }

    /**
     *
     * @param binary_threshold_proc : processor of mask, already ByteProcessor
     * @return ImagePlus corresponding to watershed binary image
     */
    public ImagePlus getWatershed(ImageProcessor binary_threshold_proc) {
        EDM edm = new EDM();
        edm.toWatershed(binary_threshold_proc);
        return new ImagePlus("_watershed",binary_threshold_proc);
    }

    /**
     * Analyze the binary image to detect particles (here spots or nuclei)
     * It scans the image/selection until it finds the edge on an object(different color)
     * Then it outlines the object using the wand tool and measures it.
     * It fills the found object to make it invisible, so it is not detected another time.
     * The scan ends when the end of image or selection is reached
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
        int analyzer_option = ParticleAnalyzer.SHOW_OVERLAY_MASKS+ParticleAnalyzer.ADD_TO_MANAGER;
        if (params.isExcludeOnEdge()) analyzer_option+= ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
        ParticleAnalyzer particleAnalyzer=new ParticleAnalyzer(analyzer_option,0,null,params.getMinSize(),Integer.MAX_VALUE);

//        Analyze
        ImageProcessor threshold_proc = threshold_IP.getProcessor();
        threshold_proc.setAutoThreshold("Default dark");
//        threshold_proc.setThreshold(128,255); /*Needs to set threshold for the binary image*/
        particleAnalyzer.analyze(threshold_IP); /*adds particles found to RoiManager and add overlay (see options)*/
        return roiManager;
    }

    /**
     * Uses the ROIs found in getThresholdMask (thresholdRois)
     * @param rois : region of all the objects segmented
     * @return ImagePlus with an intensity per object
     */
    public ImagePlus labeledImage(ImagePlus image, Roi[] rois){
        ImagePlus ip = labeledImage(image.getWidth(),image.getHeight(),rois);
        String nameObject=(params.method==SegmentationParameters.CELLPOSE)? "Cellpose_"+params.cellposeModel : "Threshold_"+params.getThresholdMethod();
        renameImage(image,ip, nameObject +"_labeled_mask");
        return ip;
    }

    /**
     * Do the same as the previous labeledImage, but without renaming the image and in static.
     * @param imageHeight : height of segmented image
     * @param imageWidth : width of segmented image
     * @param rois : : region of all the objects segmented
     * @return image with an intensity per object
     */
    public static ImagePlus labeledImage(int imageWidth,int imageHeight, Roi[] rois){
        ImagePlus imagePlus = NewImage.createShortImage("labeledImage",imageWidth,imageHeight,1,NewImage.FILL_BLACK);
        for (int i = 0; i < rois.length; i++) {
            imagePlus.getProcessor().setColor(i+1);
            imagePlus.getProcessor().fill(rois[i]);
        }
        return imagePlus;
    }


    public static void main(String[] args){
        String path="C:\\Users\\messaoudi\\Downloads\\drive-download-20221121T083445Z-001\\";
        ImageProvider provider=new ImageProvider(path,"");

        ImageSet imageSet = provider.getImageSet(0);
        SegmentationParameters parameters=SegmentationParameters.createThresholding("Li", false);
        parameters.setExcludeOnEdge(true);
        //parameters.setMinSize(10);
        //parameters.setPreprocessMacro("run(\"Gaussian Blur...\", \"sigma=2\");");

        Segmentation nucleiSeg=new Segmentation(parameters);
        ImagePlus ori=imageSet.getImage(1);
        ori.show();
        /*ImagePlus prepro=nucleiSeg.preprocess(ori);
        prepro.show();
        ImagePlus mask=nucleiSeg.segmentation(prepro);
        IndexColorModel cm= LutLoader.getLut("3-3-2 RGB");
        mask.getProcessor().resetMinAndMax();
        mask.getProcessor().setColorModel(cm);

        ImageStatistics stats=mask.getStatistics();
        System.out.println("min="+stats.min);
        System.out.println("max="+stats.max);
        System.out.println("mean="+stats.mean);
        System.out.println("stddev="+stats.stdDev);


        mask.show();
        Object[] result=Segmentation.segment(ori,parameters);
        ImagePlus mask2=(ImagePlus) result[0];
        mask2.getProcessor().resetMinAndMax();
        mask2.getProcessor().setColorModel(cm);
        mask2.show();


        Roi[] rois=(Roi[])result[1];
        System.out.println("nb nuclei found: "+rois.length);
        ImagePlus channel=imageSet.getImage(2);
        channel.setRoi(rois[0]);
        channel.show();*/

        NucleiDetector nd=new NucleiDetector(ori,ori.getTitle()+"test",path+"results\\",true);

    }
}
