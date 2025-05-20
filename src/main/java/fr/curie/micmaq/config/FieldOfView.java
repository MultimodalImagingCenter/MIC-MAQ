package fr.curie.micmaq.config;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import ome.units.quantity.Length;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class FieldOfView {
    ArrayList<ImportProcess> channelsImagePlus;
    ArrayList<Integer> originalChannelNb;
    String fieldname;
    boolean used=true;
    int serieNb=0;

    ArrayList<String> channelUserName;

    public FieldOfView(){
        channelsImagePlus=new ArrayList<ImportProcess>();
        originalChannelNb=new ArrayList<>();
        channelUserName = new ArrayList<>();
    }

    /**
     * add a channel for field of view
     * @param path path to file of image
     * @param serieNb serie nb corresponding to field of view in image given as path
     * @param channelNb channel nb corresponding to field of view in image given as path
     */
    public void addChannel(String path, int serieNb, int channelNb, String userName){
        String args = "location[local machine] windowless=true groupFiles=true id=[" + path + "]";
        ImporterOptions options = null;
        this.serieNb=serieNb;
        try {
            options = new ImporterOptions();
            options.parseArg(args);
            options.setId(path);
            ImportProcess process = new ImportProcess(options);
            process.execute();
            if (serieNb >= process.getSeriesCount())
                throw new IOException("number of available series in file is not compatible with specification");


            for(int s=0;s<process.getSeriesCount();s++){
                process.getOptions().setSeriesOn(s,s==serieNb);
            }
            process.execute();
            fieldname= process.getIdName()+"#"+process.getSeriesLabel(serieNb);
            fieldname=fieldname.replaceAll("[\\\\/:,;*?\"<>|]","_");
            IJ.log("idname: "+process.getIdName()+"\nserieslabel: "+process.getSeriesLabel(serieNb));
            IJ.log("fieldname: "+fieldname);
            channelsImagePlus.add(process);
            originalChannelNb.add(channelNb);
            channelUserName.add(userName);
            //IJ.log("add channel : "+path+"   serieNb:"+serieNb+"    channel:"+channelNb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addChannel(ImportProcess importProcess, int serieNb, int channelNb){
        this.serieNb=serieNb;
        try {

            fieldname= importProcess.getIdName()+"#"+importProcess.getOMEMetadata().getImageName(serieNb);
            fieldname=fieldname.replaceAll("[\\\\/:*?\"<>|]","_");
            IJ.log("idname: "+importProcess.getIdName()+"\nserieslabel: "+importProcess.getSeriesLabel(serieNb));
            IJ.log("fieldname: "+fieldname);
            IJ.log("getImageName(serieNb)"+importProcess.getOMEMetadata().getImageName(serieNb));
            //IJ.log("getImageName(channelNb)"+importProcess.getOMEMetadata().getImageName(channelNb-1));
            channelsImagePlus.add(importProcess);
            originalChannelNb.add(channelNb);
            //IJ.log("add channel : "+path+"   serieNb:"+serieNb+"    channel:"+channelNb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public void addAllChannels(String path, int serieNb){
        IJ.log("add all channels for "+path+" : "+serieNb);
        String args = "location[local machine] windowless=true groupFiles=true id=[" + path + "]";
        ImporterOptions options = null;
        try {
            options = new ImporterOptions();
            options.parseArg(args);
            options.setId(path);
            ImportProcess process = new ImportProcess(options);
            process.execute();
            if (serieNb >= process.getSeriesCount())
                throw new IOException("number of available series in file is not compatible with specification");
            //for(int s=0;s<process.getSeriesCount();s++){
            //    process.getOptions().setSeriesOn(s,s==serieNb);
            //}
            //process.execute();
            //int nbChannels= process.getCCount(serieNb);
            int nbChannels=process.getOMEMetadata().getChannelCount(serieNb);
            IJ.log("number of channels: "+nbChannels);
            for(int c=1;c<=nbChannels;c++){
                Instant start=Instant.now();
                addChannel(process,serieNb,c);
                Instant end = Instant.now();
                long duration= Duration.between(start, end).toMillis();
                IJ.log("FoV adding channel "+c+" : "+duration+"ms");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param channel channel number in range [1,Nchannels]
     * @return
     */
    public ImagePlus getImagePlus(int channel){
        //IJ.log("FoV getImage "+channel);
        try {
            ImportProcess process=getImportProcess(channel);
            process.execute();
            ImagePlusReader reader = new ImagePlusReader(process);
            ImagePlus tmp = reader.openImagePlus()[0];
            //tmp.show();
            //IJ.log("original channel "+originalChannelNb.get(channel-1));
            //IJ.log("nb channels: "+tmp.getNChannels());
            ImagePlus chanImg=reduce(tmp,originalChannelNb.get(channel-1));
            String title=chanImg.getTitle();
            title=title.replaceAll("[\\\\/:,;*?\"<>|]","_");
            //IJ.log(process.getOMEMetadata().getImageName(originalChannelNb.get(channel-1)));
            chanImg.setTitle(title);
            return chanImg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private ImportProcess getImportProcess(int channel) throws FormatException,IOException {
        ImportProcess result= channelsImagePlus.get(channel-1);
        for(int s=0;s<result.getSeriesCount();s++){
            result.getOptions().setSeriesOn(s,s==serieNb);
        }
        return result;
    }

    private ImagePlus reduce(ImagePlus fullChannels, int channel) {
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
    public int getNbAvailableChannels() {
        return channelsImagePlus.size();
    }

    public String getFieldname() {
        return fieldname;
    }

    public void setFieldname(String fieldname) {
        this.fieldname = fieldname;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    /**
     *
     * @param channel in range [1..nchannels]
     * @return
     */
    public int getNSlices(int channel){
        //System.out.println("fov getNslices : channel "+channel);
        System.out.println("fov getNslices :  "+channelsImagePlus.get(channel-1).getImageReader().getSizeZ());
        System.out.println("fov getNslices (OME) :  "+channelsImagePlus.get(channel-1).getOMEMetadata().getPlaneCount(0));
        return channelsImagePlus.get(channel-1).getImageReader().getSizeZ();
    }

    /**
     *
     * @param channel in range [1..nchannels]
     * @return
     */
    public String getChannelName(int channel){
        String file=channelsImagePlus.get(channel-1).getImageReader().getCurrentFile();
        String result=file.substring(file.lastIndexOf(File.separator)+1);
        if(channel<channelsImagePlus.size()) {
            return result + "#" + channelsImagePlus.get(channel - 1).getSeriesLabel(serieNb) + "#" + (originalChannelNb.get(channel - 1) + 1) + "_#_" + getChannelNameInFile(channel);
        }
        return result;
    }

    public double getPixelSize(int channel){
        Length tmp=channelsImagePlus.get(channel-1).getOMEMetadata().getPixelsPhysicalSizeX(0);
        if(tmp!=null) return tmp.value().doubleValue();
        else return -1;
    }

    public String getCalibrationUnit(int channel){
        Length tmp=channelsImagePlus.get(channel-1).getOMEMetadata().getPixelsPhysicalSizeX(0);
        if (tmp!=null) return tmp.unit().getSymbol();
        else return null;
    }

    public String getChannelNameInFile(int channel){
//        int nbchan=channelsImagePlus.get(channel-1).getOMEMetadata().getChannelCount(0);
//        System.out.println("FoV getChannelName:"+channel+" nb channels:"+nbchan);
//        for(int c=0;c<nbchan;c++){
//            System.out.println("#"+c+" channel name (metadata)="+channelsImagePlus.get(channel-1).getOMEMetadata().getChannelName(0,c));
//        }
        System.out.println("FoV getChannelName : channel="+channel+"   original="+originalChannelNb.get(channel-1));
        String tmp=null;
        try{
            tmp=channelsImagePlus.get(channel-1).getOMEMetadata().getChannelName(0,originalChannelNb.get(channel-1)-1);
        }catch (Exception e){
            e.printStackTrace();
        }
        //System.out.println("returned name "+tmp);
        if(tmp!=null) return tmp.replaceAll("/","_");
        return tmp;
    }

    public String getChannelUserName(int channel) {
        channel-=1;
        String tmpstr=null;
        if( channel>=0 && channel<channelUserName.size())
            tmpstr = channelUserName.get(channel);
        if (tmpstr == null) {
            tmpstr = getChannelNameInFile(channel+1);
        }
        return tmpstr;
    }

    public void setChannelsUserName(ArrayList<String> names){
        channelUserName=names;
    }

}
