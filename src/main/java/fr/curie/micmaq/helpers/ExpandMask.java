package fr.curie.micmaq.helpers;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
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

    /**
     * 3D variant: find best positive neighbor in an ellipsoidal neighborhood around (x,y,z)
     * using separate radii for XY and Z. z is the 1-based index of the slice in the ImageStack.
     */
    static protected double getBestValue(ImageStack stack, int x, int y, int z, int radiusXY, int radiusZ){
        ImageProcessor ipz = stack.getProcessor(z);
        double center = ipz.getf(x, y);
        if (center > 0) return center;

        if (radiusXY < 0) radiusXY = 0;
        if (radiusZ < 0) radiusZ = 0;

        int w = stack.getWidth();
        int h = stack.getHeight();
        int n = stack.getSize();

        double bestVal = center;
        double bestDist = Double.MAX_VALUE; // normalized squared distance

        double rxy2 = (double) radiusXY * (double) radiusXY;
        double rz2 = (double) radiusZ * (double) radiusZ;

        int zMin = Math.max(1, z - radiusZ);
        int zMax = Math.min(n, z + radiusZ);
        for (int kk = zMin; kk <= zMax; kk++) {
            int dz = kk - z;
            double dz2 = dz * dz;
            ImageProcessor ipk = stack.getProcessor(kk);

            for (int j = y - radiusXY; j <= y + radiusXY; j++) {
                for (int i = x - radiusXY; i <= x + radiusXY; i++) {
                    double tmp = ipk.getValue(i, j); // returns 0 outside bounds
                    if (tmp <= 0) continue;

                    int dx = x - i;
                    int dy = y - j;
                    double dxy2 = dx * dx + dy * dy;

                    // Compute normalized ellipsoidal distance: (dx^2+dy^2)/rxy^2 + dz^2/rz^2
                    double partXY = (radiusXY > 0) ? (dxy2 / rxy2) : (dxy2 == 0 ? 0.0 : Double.POSITIVE_INFINITY);
                    double partZ  = (radiusZ  > 0) ? (dz2  / rz2)  : (dz  == 0 ? 0.0 : Double.POSITIVE_INFINITY);
                    double norm = partXY + partZ;

                    if (norm <= 1.0 && norm < bestDist) {
                        bestDist = norm;
                        bestVal = tmp;
                    }
                }
            }
        }
        return bestVal;
    }

    /**
     * 3D expandsMask: expand a mask over an ImageStack using an ellipsoidal neighborhood
     * defined by radiusXY (in-plane) and radiusZ (through slices). For each voxel (x,y,z),
     * if its value is > 0 it is kept; otherwise the closest positive value within the
     * ellipsoidal neighborhood is propagated.
     */
    public static ImageStack expandsMask(final ImageStack stack, final int radiusXY, final int radiusZ) {
        final int w = stack.getWidth();
        final int h = stack.getHeight();
        final int n = stack.getSize();

        final ImageProcessor[] outProcessors = new ImageProcessor[n];

        ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ArrayList<Future> futures = new ArrayList<>();

        for (int z = 1; z <= n; z++) {
            final int zz = z;
            futures.add(exec.submit(new Thread() {
                @Override
                public void run() {
                    ImageProcessor out = stack.getProcessor(zz).createProcessor(w, h);
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            double v = getBestValue(stack, x, y, zz, radiusXY, radiusZ);
                            out.putPixelValue(x, y, v);
                        }
                    }
                    outProcessors[zz - 1] = out;
                }
            }));
        }

        for (Future f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        exec.shutdown();

        ImageStack result = new ImageStack(w, h, stack.getColorModel());
        for (int z = 1; z <= n; z++) {
            String label = stack.getSliceLabel(z);
            result.addSlice(label, outProcessors[z - 1]);
        }
        return result;
    }

}
