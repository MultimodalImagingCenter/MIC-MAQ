package fr.curie.micmaq.helpers;

import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
/**
 * Author : Camille RABIER
 * Date : 22/08/2022
 * Class to facilitate management of ImagePlus
 * - For images from directory creates the ImagePlus instances at the necessary time
 * - If directory set, creates Results directory
 * - facilitates name to display
 */
public class ImageToAnalyze {
    private ImagePlus imagePlus;
    private String directory; /*directory to save results them*/
    private final String imageName;
    private boolean hideID=false;

//    CONSTRUCTORS

    /**
     * Constructor for image from directory
     *
     * @param directory : directory containing image
     * @param imageName : name of image file
     */
    public ImageToAnalyze(String directory, String imageName) {
        this.setDirectory(directory);
        this.imageName = imageName;
    }

    /**
     * Constructor for image opened in ImageJ
     *
     * @param imagePlus : opened image
     */
    public ImageToAnalyze(ImagePlus imagePlus) {
        this.imagePlus = imagePlus;
        this.imagePlus.setCalibration(null);/*remove calibration*/
        this.imageName = imagePlus.getTitle();
        directory = null;
    }


//    GETTERS

    /**
     * If ImagePlus does not exist (image from directory), creates the instance
     *
     * @return ImagePlus instance
     */
    public ImagePlus getImagePlus() {
        if (imagePlus == null) {
            this.imagePlus = IJ.openImage(directory + "/" + imageName);
            this.imagePlus.setCalibration(null); /*remove calibration*/
        }
        ImagePlus toReturn = imagePlus.duplicate();
        toReturn.setTitle(imageName);
        return toReturn;
    }

    /**
     *
     * @return ID of image
     */
    public int getID(){
       return imagePlus.getID();
    }

    /**
     * @return directory to save results
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * @return name of image
     */
    public String getImageName() {
        return imageName;
    }


//    SETTERS

    /**
     * Unify path separators and create results directory
     * @param directory : path of directory
     */
    public void setDirectory(String directory) {
//        UNIFY PATH SEPARATOR
        if (directory.contains("\\")) {/*unify the directory delimiter*/
            directory = directory.replace("\\", "/");
        }
//        REMOVE EXTRA SEPARATOR IF NECESSARY
        if (directory.endsWith("/")) {
            directory = directory.substring(0, directory.length() - 1);
        }
        this.directory = directory;
//        CREATE RESULTS DIRECTORY
        if (!new File(directory + "/Results/Images").exists() ||!new File(directory + "/Results/ROI").exists()){
            createResultsDirectory(directory);
        }
    }

    public void setHideID(boolean hide){
        this.hideID = hide;
    }


    //    FUNCTIONS/METHODS
    /**
     * @return String with either name of image (image from directory) or image name with ID (if opened image)
     */
    @Override
    public String toString() {
        if (directory != null || hideID) {
            return imageName;
        } else { /*For openImages, we want the ID to be displayed in case there are multiple images opened with same name*/
            //return imageName + "#" + imagePlus.getID();
            return imageName;
        }
    }

    /**
     * Create result directory (with subdirectories Images and ROI)
     * @param directory : path of directory
     */
    public static void createResultsDirectory(String directory) {
        Path path = Paths.get(new File(directory).getAbsolutePath() + "/Results/Images");
        Path path2 = Paths.get(new File(directory).getAbsolutePath() + "/Results/ROI");
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            IJ.error("Failed to create results directory" + ex.getMessage());
        }
        try {
            Files.createDirectories(path2);
        } catch (IOException ex) {
            IJ.error("Failed to create results directory" + ex.getMessage());
        }
    }

    /**
     * Remove extension of imageName
     * @param imageName : name of image
     * @return name of image without extension, if extension exists
     */
    public static String nameWithoutExtension(String imageName) {
        int lastPoint = imageName.lastIndexOf(".");
        if (lastPoint != -1) {
            return imageName.substring(0, lastPoint);
        } else {
            return imageName;
        }
    }

    /**
     * Return name of image without channel specific information
     * @param imageToAnalyze : image
     * @return name of image without the ending entered by the user
     */
    public static String getNameExperiment(ImageToAnalyze imageToAnalyze, JTextField imageEndingField){
        if (imageEndingField.getText().length() == 0) {
            return imageToAnalyze.getImageName();
        } else {
            return ImageToAnalyze.nameWithoutExtension(imageToAnalyze.getImageName().split(imageEndingField.getText())[0] + imageToAnalyze.getImageName().split(imageEndingField.getText())[1]);
        }
    }

    /**
     * Filter list of image by name ending and if necessary display error message in panel
     * Iterates on all names in the model and if the name does not end with the label given by user,
     * it is removed from the model
     * @param filteredImageList : JList model
     * @param endingFilter : String that the images names should end by
     * @param allImageList : all the images that have to be considered
     * @param errorImageEndingLabel : JLabel that is displayed in case of empty model
     * @return true if there are images in the filteredImageList.
     */
    public static boolean filterModelByEnding(DefaultListModel<ImageToAnalyze> filteredImageList, String endingFilter, ImageToAnalyze[] allImageList, JLabel errorImageEndingLabel) {
        for (ImageToAnalyze image : allImageList) {
            String title = image.getImageName();
            String titleWoExt = ImageToAnalyze.nameWithoutExtension(title);
//          Assert if image title ends by filter

//            if (!title.endsWith(endingFilter) && !titleWoExt.endsWith(endingFilter)) { /*the title does not end by the filter*/
            if (!title.contains(endingFilter) && !titleWoExt.contains(endingFilter)) { /*the title does not end by the filter*/
                if (filteredImageList.contains(image)) { /*if model contains the image, removes it*/
                    filteredImageList.removeElement(image);
                }
            } else { /*the title ends by the filter*/
                if (!filteredImageList.contains(image)) { /*if the model does not contain the image, adds it*/
                    filteredImageList.addElement(image);
                }
            }
        }
        if (filteredImageList.isEmpty()) {
            /*if no image corresponds to the filter, display all images names and an error*/
            for (ImageToAnalyze imagePlusDisplay : allImageList) {
                filteredImageList.addElement(imagePlusDisplay);
            }
            errorImageEndingLabel.setText("No image corresponding to ending.");
            errorImageEndingLabel.setForeground(Color.RED);
            return false; /*there are no images corresponding to the label*/
        } else {
            errorImageEndingLabel.setText(filteredImageList.size()+" image(s) corresponding to ending.");
            errorImageEndingLabel.setForeground(Color.decode("#00833D"));
            return true; /*there are images corresponding to the label*/
        }
    }

    /**
     * Assert that the prefs save int are in the possible slices number
     * @param maxSlices maximal number of slices in image
     * @param firstSlice : firstSlice in prefs
     * @param lastSlice : lastSlice in prefs
     * @param firstSliceSpinner :spinner for first Slice
     * @param lastSliceSpinner : spinner for last slice
     */
    public static void assertSlices(int maxSlices, int firstSlice, int lastSlice, JSpinner firstSliceSpinner, JSpinner lastSliceSpinner) {
        if(firstSlice<=maxSlices){
            firstSliceSpinner.setModel(new SpinnerNumberModel(firstSlice,1,maxSlices,1));
        }else {
            firstSliceSpinner.setModel(new SpinnerNumberModel(1,1,maxSlices,1));
        }
        if (lastSlice<=maxSlices){
            lastSliceSpinner.setModel(new SpinnerNumberModel(lastSlice,1,maxSlices,1));
        }else {/*prefs saved for image with more slices than actual images*/
            lastSliceSpinner.setModel(new SpinnerNumberModel(maxSlices,1,maxSlices,1));
        }
    }

}
