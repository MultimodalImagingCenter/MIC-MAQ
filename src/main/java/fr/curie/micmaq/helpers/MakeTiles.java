package fr.curie.micmaq.helpers;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class MakeTiles implements PlugInFilter {
    ImagePlus imp;
    int sizexy = 1024;
    int sizez = 1;
    int overlap = 30;
    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp=imp;
        if(!askParameters()) return DONE;
        return DOES_ALL;
    }

    public void setParameters(int sizexy, int sizez, int overlap){
        this.sizexy=sizexy;
        this.sizez=sizez;
        this.overlap=overlap;
    }


    public boolean askParameters(){
        GenericDialog gd= new GenericDialog("combine tiles");
        gd.addNumericField("tile size (square)", sizexy);
        gd.addNumericField("final image depth (not used yet)", sizez);
        gd.addNumericField("overlap between tiles", overlap);

        gd.showDialog();
        if(gd.wasCanceled())return false;

        sizexy=(int)gd.getNextNumber();
        sizez=(int)gd.getNextNumber();
        overlap=(int)gd.getNextNumber();
        return true;
    }

    @Override
    public void run(ImageProcessor ip) {
        if(imp.getNChannels()==1 && imp.getNSlices()==1) {
            new ImagePlus(imp.getShortTitle() + "_tiles", tileImage(ip)).show();
        }
        else if(imp.getNSlices()==1){
            tileChannels(imp).show();
        }
    }

    ImagePlus tileChannels(ImagePlus imp){
        ImagePlus[] channels=new ImagePlus[imp.getNChannels()];
        for(int c=0;c<channels.length;c++){
            imp.setC(c+1);
            channels[c]=new ImagePlus(imp.getShortTitle() + "_tiles", tileImage(imp.getChannelProcessor()));
            channels[c].show();
        }
        ImagePlus composite = RGBStackMerge.mergeChannels(channels, true);
        return composite;
    }

    ImageStack tileImage(ImageProcessor ip){
        int step=sizexy-overlap;
        ImageStack is=new ImageStack(sizexy,sizexy);
        for(int y=0;y<ip.getHeight()-overlap;y+=step){
            for(int x=0;x<ip.getWidth()-overlap;x+=step){
                ip.setRoi(x,y,sizexy,sizexy);
                ImageProcessor crop= ip.crop();
                is.addSlice("",crop);
            }
        }
        return is;
    }
}

