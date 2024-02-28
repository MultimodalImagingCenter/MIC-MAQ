package fr.curie.micmaq.helpers;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.*;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.stream.IntStream;

public class CombineTiles implements PlugInFilter {
    ImagePlus imp;
    int sizex = 1024;
    int sizey = 1024;
    int sizez = 1;
    int overlap = 30;
    boolean instanceMask = true;
    boolean excludeOnEdges = true;
    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp=imp;
        if(!askParameters()) return DONE;

        return DOES_ALL;
    }

    public boolean askParameters(){
        GenericDialog gd= new GenericDialog("combine tiles");
        gd.addNumericField("final_image_width", sizex);
        gd.addNumericField("final_image_height", sizey);
        gd.addNumericField("final_image_depth (not used yet)", sizez);
        gd.addNumericField("overlap between tiles", overlap);
        gd.addCheckbox("instance segmentation mask",instanceMask);

        gd.showDialog();
        if(gd.wasCanceled())return false;

        sizex=(int)gd.getNextNumber();
        sizey=(int)gd.getNextNumber();
        sizez=(int)gd.getNextNumber();
        overlap=(int)gd.getNextNumber();
        instanceMask=gd.getNextBoolean();
        return true;
    }

    public void setParameters(int sizex, int sizey, int sizez, int overlap, boolean instanceMask){
        this.sizex=sizex;
        this.sizey=sizey;
        this.sizez=sizez;
        this.overlap=overlap;
        this.instanceMask=instanceMask;
    }

    @Override
    public void run(ImageProcessor ip) {
        if(imp.getNChannels()==1) {
            new ImagePlus(imp.getShortTitle() + "_tiles", combineTileImage(imp.getStack())).show();
            return;
        }
        combineTileChannels(imp).show();

    }

    ImagePlus combineTileChannels(ImagePlus imp){
        ImagePlus[] channels=new ImagePlus[imp.getNChannels()];
        for(int c=0;c<channels.length;c++){
            imp.setC(c+1);
            ImageStack tmp=new ImageStack(imp.getWidth(), imp.getHeight());
            for(int i=0;i<imp.getNSlices();i++){
                imp.setPosition(c+1,i+1,1);
                tmp.addSlice("", imp.getProcessor());
            }
            channels[c]=new ImagePlus(imp.getShortTitle() + "_tiles", combineTileImage(tmp));
            channels[c].show();
        }
        ImagePlus composite = RGBStackMerge.mergeChannels(channels, true);
        return composite;
    }

    ImageProcessor combineTileImage(ImageStack is){
        int step=is.getWidth()-overlap;
        ImageProcessor result=is.getProcessor(1).createProcessor(sizex,sizey);
        int index=1;
        for(int y=0;y<result.getHeight()-overlap;y+=step){
            for(int x=0;x< result.getWidth()-overlap;x+=step){
                result.copyBits(is.getProcessor(index),x,y, Blitter.MAX);
                if(instanceMask)label2Roi(new ImagePlus("",is.getProcessor(index)),x,y);
                index++;
            }
        }
        return result;
    }

    public void label2Roi(ImagePlus cellposeIP, int xoffset, int yoffset) {
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
        checkDuplicates(cellposeRoiManager);
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
}
