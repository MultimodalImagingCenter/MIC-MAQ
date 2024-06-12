package fr.curie.micmaq.config;

import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterMetadata;
import loci.plugins.in.ImporterOptions;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class FieldOfViewProvider {
    String directory;
    int nucleiChannel = -1;
    int cellChannel = -1;

    int previewImage = 0;
    int validIndex=0;

    double completion=-10;

    ArrayList<FieldOfView> fields;
    ArrayList<Integer> differentNumberOfChannels;
    ArrayList<Integer> nbImgsPerChanNb;

    /**
     * constructor
     *
     * @param directory working directory
     */
    public FieldOfViewProvider(String directory) {
        this.directory = directory;
    }

    public void addFieldOfView(FieldOfView fov) {
        fields.add(fov);
    }

    public FieldOfView getFieldOfView(int index) {
        return fields.get(index);
    }

    public FieldOfView getFirstValidFieldOfView(){
        return fields.get(validIndex);
    }

    public ArrayList<FieldOfView> getAllFields() {
        return fields;
    }

    public int getNbFielOfView() {
        return fields.size();
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

    public int getPreviewImage() {
        return previewImage;
    }

    public void setPreviewImage(int previewImage) {
        this.previewImage = previewImage;
    }

    public void parseDirectory(final String filePattern) {
        IJ.log("parse directory " + directory);
        completion=0;
        Instant dateTotalBegin = Instant.now();
        fields = new ArrayList<>();
        File dir = new File(directory);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return filePattern == null || filePattern.isEmpty() || name.contains(filePattern);
            }
        };
        //System.out.println("filter " +fileExtension+" filter "+filter);
        File[] files = dir.listFiles(filter);
        if (files == null || files.length == 0) return;
        IJ.log("nb files "+files.length);
        for (int f=0;f<files.length;f++){
            File file =files[f];
            if(completion<0) return;
            if (!file.isDirectory()) {
                System.out.println("########################################");
                String path = file.getAbsolutePath();
                IJ.showStatus("parsing " + path);
                String args = "location[local machine] windowless=true groupFiles=true id=[" + path + "]";
                try {

                    Instant dateBegin = Instant.now();
                    ImporterOptions options = new ImporterOptions();
                    options.parseArg(args);
                    options.setId(path);

                    ImportProcess process = new ImportProcess(options);
                    process.execute();

                    Instant dateEnd = Instant.now();
                    long duration = Duration.between(dateBegin, dateEnd).toMillis();
                    IJ.log("first read of file" + file + ". It took " + duration / 1000.0 + " seconds");

                    dateBegin = Instant.now();
                    int nSeries = process.getSeriesCount();
                    ImporterMetadata md = process.getOriginalMetadata();
                    IMetadata imd = process.getOMEMetadata();
                    System.out.println("\t"+path + " Nseries " + nSeries + " channels:" + process.getCCount(0));
                    System.out.println("\t"+"nb dataset (metadata)=" + imd.getDatasetCount());
                    System.out.println("\t"+"nb experiment (metadata)=" + imd.getExperimentCount());
                    System.out.println("\t"+"nb image (metadata)=" + imd.getImageCount());
                    dateEnd = Instant.now();
                    duration = Duration.between(dateBegin, dateEnd).toMillis();
                    IJ.log("\t"+"get infos from metadata " + duration / 1000.0 + " seconds");
                    for (int i = 0; i < imd.getImageCount(); i++) {
                        System.out.println("\t\t"+"nb channel (metadata)=" + imd.getChannelCount(i));
                    }
                    IJ.log("\t"+path + " Nseries " + nSeries + " channels:" + process.getCCount(0));
                    for (int i = 0; i < nSeries; i++) {
                        IJ.showStatus("adding file " + (i + 1) + "/" + nSeries);
                        IJ.log("\t"+"adding field of view " + i);
                        Instant dateBegin2 = Instant.now();
                        FieldOfView fov = new FieldOfView();
                        fov.addAllChannels(path, i);
                        fields.add(fov);
                        Instant dateEnd2 = Instant.now();
                        long duration2 = Duration.between(dateBegin2, dateEnd2).toMillis();
                        IJ.log("\t"+"adding field " + i + ":    " + duration2 / 1000.0 + " seconds");
                    }

                } catch (FormatException e) {

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
            }
            completion=(f+1.0)/ files.length;
            System.out.flush();
        }
        completion=1.0;
        IJ.log("total number of field of view: " + fields.size());
        if (fields.size() == 0) return;
        Instant dateTotalEnd = Instant.now();
        long duration = Duration.between(dateTotalBegin, dateTotalEnd).toMillis();
        IJ.log("reading directory. It took " + duration / 1000.0 + " seconds");

        checkChannels();
    }

    public void checkChannels() {
        IJ.log("check channels");
        differentNumberOfChannels = new ArrayList<>();
        for (FieldOfView fov : fields) {
            int channels = fov.getNbAvailableChannels();
            boolean found = false;
            for (Integer i : differentNumberOfChannels) if (i == channels) found = true;
            if (found == false) differentNumberOfChannels.add(channels);
        }
        if (differentNumberOfChannels.size() > 1) IJ.log("different number of channels found between images");
        for (int chan = 0; chan < differentNumberOfChannels.size(); chan++) {
            int channels = differentNumberOfChannels.get(chan);
            IJ.log("number of channels:" + channels);
            int count = 0;
            for (FieldOfView set : fields) {
                if (set.getNbAvailableChannels() == channels) count++;
            }
            if (nbImgsPerChanNb == null) nbImgsPerChanNb = new ArrayList<>();
            nbImgsPerChanNb.add(count);
            IJ.log(differentNumberOfChannels.get(chan) + " channels for " + count + " images");
        }
    }

    public ArrayList<MeasureCalibration> checkCalibration() {
        IJ.log("check calibration");
        ArrayList<MeasureCalibration> differentNumberOfCalibration = new ArrayList<>();
        for (FieldOfView fov : fields) {
            double calib = fov.getPixelSize(1);
            if (calib > 0) {
                boolean found = false;
                for (MeasureCalibration i : differentNumberOfCalibration) found = (i.getPixelLength() == calib);
                if (!found)
                    differentNumberOfCalibration.add(new MeasureCalibration("From image ", "" + calib, fov.getCalibrationUnit(1)));
            }
        }
        if (differentNumberOfCalibration.size() > 1) IJ.log("different number of calibrations found between images");
        for (int chan = 0; chan < differentNumberOfCalibration.size(); chan++) {
            MeasureCalibration calib = differentNumberOfCalibration.get(chan);
            IJ.log("number of calibrations:" + calib);
            int count = 0;
            for (FieldOfView set : fields) {
                if (set.getPixelSize(1) == calib.getPixelLength()) count++;
            }
            ArrayList<Integer> nbImgsPerCalib = new ArrayList<>();
            nbImgsPerCalib.add(count);
            IJ.log(differentNumberOfCalibration.get(chan) + " calibrations for " + count + " images");
        }

        return differentNumberOfCalibration;
    }

    public void reorganiseFiles(final String filePattern, final ArrayList<String> patterns) {
        IJ.log("reorganise files " + directory);
        fields = new ArrayList<>();
        File dir = new File(directory);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(filePattern) && name.contains(patterns.get(0));
            }
        };
        //System.out.println("filter " +fileExtension+" filter "+filter);
        File[] files = dir.listFiles(filter);
        IJ.log("nb files "+files.length);
        for (int f=0;f<files.length;f++){
            File file =files[f];
            if(completion<0) return;
            if (!file.isDirectory()) {
                String path = file.getAbsolutePath();
                IJ.log("############   opening"+file.getName());
                String args = "location[local machine] windowless=true groupFiles=true id=[" + path + "]";
                try {
                    ImporterOptions options = new ImporterOptions();
                    options.parseArg(args);
                    options.setId(path);

                    ImportProcess process = new ImportProcess(options);
                    process.execute();
                    int nSeries = process.getSeriesCount();
                    IJ.log(path + " Nseries " + nSeries + " channels:" + process.getCCount(0));
                    for (int i = 0; i < nSeries; i++) {
                        IJ.log("\tadding field of view " + i);
                        FieldOfView fov = new FieldOfView();
                        fov.addChannel(path, i, 1, patterns.get(0));
                        for (int c = 1; c < patterns.size(); c++) {
                            String name = file.getName();
                            IJ.log("\t\toriginal name " + name);
                            String f2 = name.replaceAll(patterns.get(0), patterns.get(c));
                            IJ.log("\t\tafter pattern replacement --> " + f2);
                            IJ.log("\t\tpath " + dir + File.separator + f2);
                            fov.addChannel(dir + File.separator + f2, i, 1, patterns.get(c));
                        }
                        fov.setFieldname(file.getName().replaceAll(patterns.get(0), ""));
                        fields.add(fov);
                    }

                } catch (FormatException e) {

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
            }
            completion=(f+1.0)/ files.length;
        }
        IJ.log("total number of field of view: " + fields.size());
        checkChannels();
    }

    public ImagePlus getImagePlus(int index, int channel) {
        return fields.get(index).getImagePlus(channel);
    }

    public int getDifferentNumberOfChannels() {
        return differentNumberOfChannels.size();
    }

    public int getNumberOfChannel(int index) {
        return differentNumberOfChannels.get(index);
    }

    public int getNbImgsPerChanNb(int index) {
        return nbImgsPerChanNb.get(index);
    }

    public void setChannelsUserName(ArrayList<String> names) {
        for (FieldOfView fov : fields) fov.setChannelsUserName(names);
    }

    public int getFirstValidIndex() {
        return validIndex;
    }

    public void setFirstValidIndex(int validIndex) {
        this.validIndex = validIndex;
    }

    public double getCompletion() {
        return completion;
    }

    public void cancelLoading() {
        this.completion = -1;
    }
}
