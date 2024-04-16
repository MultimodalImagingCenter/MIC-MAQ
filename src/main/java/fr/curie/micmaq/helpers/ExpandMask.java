package fr.curie.micmaq.helpers;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExpandMask implements PlugInFilter {
    int radius=5;
    @Override
    public int setup(String arg, ImagePlus imp) {
        radius=(int) IJ.getNumber("Expand mask by radius ",radius);
        return IJ.setupDialog(imp,DOES_ALL);
    }

    @Override
    public void run(ImageProcessor ip) {
        ImageProcessor result = ip.createProcessor(ip.getWidth(),ip.getHeight());
        for(int y=0;y<ip.getHeight();y++){
            for(int x=0;x<ip.getWidth();x++){
                result.putPixelValue(x,y,getBestValue(ip,x,y,radius));
            }
        }
        ip.copyBits(result,0,0, Blitter.COPY);
    }
    static public ImageProcessor expandsMask(final ImageProcessor ip, final int radius){
        ImageProcessor result = ip.createProcessor(ip.getWidth(),ip.getHeight());
        ExecutorService exec= Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ArrayList<Future> futures=new ArrayList<>();
        for(int y=0;y<ip.getHeight();y++){
            final int yy=y;
            futures.add(exec.submit(new Thread(){
                @Override
                public void run() {
                    for(int x=0;x<ip.getWidth();x++){
                        result.putPixelValue(x,yy,getBestValue(ip,x,yy,radius));
                    }
                }
            }));

        }
        for(Future f: futures) {
            try{
                f.get();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    static protected double getBestValue(ImageProcessor ip, int x, int y, int radius){
        double val=ip.getf(x,y);
        if(val>0) return val;
        int radius2=radius*radius;
        double dmin = Double.MAX_VALUE;
        for(int j = y - radius; j<= y+radius;j++){
            for(int i = x - radius; i<= x+radius;i++){
                double tmp=ip.getValue(i,j);
                double d=(x-i)*(x-i)+(y-j)*(y-j);
                if(tmp>0 && d<dmin && d<radius2){
                    dmin=d;
                    val=tmp;
                }
            }

        }
        return val;
    }
}
