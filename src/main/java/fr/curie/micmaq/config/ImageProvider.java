package fr.curie.micmaq.config;

import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

public class ImageProvider {
    String directory;
    int nbFiles;
    int nbImages;
    ArrayList<ImageSet> imageSets;
    ArrayList<Integer> differentNumberOfChannels;
    ArrayList<Integer> nbImgsPerChanNb;
    String filePattern;
    int nucleiChannel=-1;
    int cellChannel=-1;


    public ImageProvider(String directory, String filePattern){
        this.filePattern = filePattern.toLowerCase();
        this.directory=directory;
        parseDirectory();
    }
    public ImageProvider(String directory){
        this(directory,"");
    }

    public void parseDirectory(){
        imageSets=new ArrayList<>();
        File dir=new File(directory);
        FilenameFilter filter=new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return filePattern.isEmpty()||name.contains(filePattern);
            }
        };
        //System.out.println("filter " +fileExtension+" filter "+filter);
        File[] files=dir.listFiles(filter);
        //System.out.println("nb files "+files.length);
        nbFiles=files.length;
        for(File f:files){
            if(!f.isDirectory()) {
                String path = f.getAbsolutePath();
                String args = "location[local machine] windowless=true groupFiles=true id=[" + path + "]";
                try {
                    ImporterOptions options = new ImporterOptions();
                    options.parseArg(args);
                    options.setId(path);

                    ImportProcess process = new ImportProcess(options);
                    process.execute();
                    int nSeries = process.getSeriesCount();
                    //IJ.showMessage(path + " Nseries " + nSeries + " channels:" + process.getCCount(0));
                    for (int i = 0; i < nSeries; i++) {
                        ImageSet is=new ImageSet(path, i);
                        is.setDirectory(directory);
                        imageSets.add(is);
                    }

                } catch (FormatException e){

                }  catch(IOException e) {
                    e.printStackTrace();
                    nbFiles--;
                }
            }else {
                nbFiles--;
            }
        }
        nbImages=imageSets.size();
        System.out.println("total number of files: "+nbFiles);
        System.out.println("total number of images: "+nbImages);
        if(nbImages==0) return;
        checkChannels();


    }

    public void checkChannels(){
        for(ImageSet set:imageSets){
            if(differentNumberOfChannels==null) differentNumberOfChannels=new ArrayList<>();
            int channels=set.getNbAvailableChannels();
            boolean found=false;
            for(Integer i:differentNumberOfChannels) if(i==channels) found=true;
            if(found==false) differentNumberOfChannels.add(channels);
        }
        if(differentNumberOfChannels.size()>1) System.out.println("different number of channels found between images");
        for(int chan=0; chan<differentNumberOfChannels.size();chan++) {
            int channels=differentNumberOfChannels.get(chan);
            System.out.println("number of channels:"+channels);
            int count=0;
            for(ImageSet set:imageSets){
                if(set.getNbAvailableChannels()==channels) count++;
            }
            if(nbImgsPerChanNb==null) nbImgsPerChanNb=new ArrayList<>();
            nbImgsPerChanNb.add(count);
            System.out.println(differentNumberOfChannels.get(chan)+ " channels for "+count+" images");
        }
    }

    /**
     * get the total number of files in the directory
     * @return number of files in directory
     */
    public int getNbFiles() {
        return nbFiles;
    }

    /**
     * ge the number of imageSet in the provider
     * @return
     */
    public int getNbImages() {
        return nbImages;
    }

    public  int getDifferentNumberOfChannels() {
        return differentNumberOfChannels.size();
    }

    public int getNumberOfChannel(int index){
        return differentNumberOfChannels.get(index);
    }

    public int getNbImgsPerChanNb(int index) {
        return nbImgsPerChanNb.get(index);
    }

    public ImageSet getImageSet(int index){
        return imageSets.get(index);
    }

    public ImagePlus getImagePlus(int index, int channel){
        return imageSets.get(index).getImage(channel);
    }

    public void freeImagePlus(int index){
        imageSets.get(index).freeImagePlus();
    }

    public int getNucleiChannel() {
        return nucleiChannel;
    }

    public void setNucleiChannel(int nucleiChannel) {
        this.nucleiChannel = nucleiChannel;
    }

    public int getCellChannel() {
        return cellChannel;
    }

    public void setCellChannel(int cellChannel) {
        this.cellChannel = cellChannel;
    }

    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }

    public void addToImageSet(ImageSet imgset){
        if(imageSets==null) imageSets=new ArrayList<>();
        imageSets.add(imgset);
        nbImages=imageSets.size();
    }

    public static void main(String[] args){
        String path="C:\\Users\\messaoudi\\Downloads\\drive-download-20221121T083445Z-001\\";
        ImageProvider provider=new ImageProvider(path,"");

    }

}
