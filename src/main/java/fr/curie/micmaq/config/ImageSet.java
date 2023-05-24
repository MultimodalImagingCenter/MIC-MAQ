package fr.curie.micmaq.config;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

import java.io.IOException;

public class ImageSet {
    String path;
    String directory;
    int serieNb;
    int nbAvailableChannels;
    private ImportProcess process;
    private ImagePlus fullChannels;
    String fieldname;


    public ImageSet(String path, int serieNb) {
        this.path = path;
        this.serieNb = serieNb;
        String args = "location[local machine] windowless=true groupFiles=true id=[" + path + "]";
        ImporterOptions options = null;
        try {
            options = new ImporterOptions();
            options.parseArg(args);
            options.setId(path);
            process = new ImportProcess(options);
            process.execute();
            if (serieNb >= process.getSeriesCount())
                throw new IOException("number of available series in file is not compatible with specification");


            for(int s=0;s<process.getSeriesCount();s++){
                process.getOptions().setSeriesOn(s,s==serieNb);
            }
            process.execute();
            nbAvailableChannels = process.getCCount(serieNb);
            fieldname= process.getSeriesLabel(serieNb);
            //System.out.println(options);
            //IJ.showMessage("Nseries " + process.getSeriesCount() + " channels:" + process.getCCount(serieNb));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public ImagePlus getImage(int channel){
        if(fullChannels==null) {
            try {
                ImagePlusReader reader = new ImagePlusReader(process);
                fullChannels = reader.openImagePlus()[0];
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return reduce(channel);
    }

    private ImagePlus reduce(int channel) {
        ImagePlus imp2 = IJ.createImage(fullChannels.getTitle(), fullChannels.getBitDepth() + "-bit",
                fullChannels.getWidth(), fullChannels.getHeight(), fullChannels.getNSlices() * fullChannels.getNFrames());
        ((ImagePlus)imp2).setDimensions(1, fullChannels.getNSlices(), fullChannels.getNFrames());
        ((ImagePlus)imp2).setOpenAsHyperStack(true);
        int channels = 1;
        int slices = imp2.getNSlices();
        int frames = imp2.getNFrames();
        int c = channel;
        int z1 = fullChannels.getSlice();
        int t1 = fullChannels.getFrame();
        int n = channels * slices * frames;
        ImageStack stack = fullChannels.getStack();
        ImageStack stack2 = imp2.getStack();


        fullChannels.setPositionWithoutUpdate(c, 1, 1);
        ImageProcessor ip = fullChannels.getProcessor();
        double min = ip.getMin();
        double max = ip.getMax();

        for(int z = 1; z <= slices; ++z) {
            if (slices == 1) {
                z = z1;
            }

            for(int t = 1; t <= frames; ++t) {
                if (frames == 1) {
                    t = t1;
                }

                int n1 = fullChannels.getStackIndex(c, z, t);
                ip = stack.getProcessor(n1);
                String label = stack.getSliceLabel(n1);
                int n2 = imp2.getStackIndex(c, z, t);
                if (stack2.getPixels(n2) != null) {
                    stack2.getProcessor(n2).insert(ip, 0, 0);
                } else {
                    stack2.setPixels(ip.getPixels(), n2);
                }

                stack2.setSliceLabel(label, n2);
            }
        }
        imp2.getProcessor().setMinAndMax(min, max);


        imp2.resetStack();
        imp2.setPosition(1, 1, 1);
        return imp2;

    }

    public String getPath() {
        return path;
    }

    public int getSerieNb() {
        return serieNb;
    }

    public int getNbAvailableChannels() {
        return nbAvailableChannels;
    }

    public ImagePlus getFullChannels() {
        return fullChannels;
    }

    public void freeImagePlus(){
        fullChannels=null;
    }

    public String getFieldname() {
        return fieldname;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public static void main(String[] args){
        String path="C:\\Users\\messaoudi\\Downloads\\drive-download-20221121T083445Z-001\\MFGTMP_221110120001_C09f00d0.C01";
        ImageSet img=new ImageSet(path,3);
        img.getImage(2).show();
    }
}
