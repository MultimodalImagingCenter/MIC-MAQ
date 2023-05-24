package fr.curie.micmaq.helpers;

import ij.IJ;
import ij.Prefs;

import java.io.*;
import java.util.ArrayList;

import static ij.IJ.d2s;

/**
 * Author : Camille RABIER
 * Date : 08/08/2022
 * Class for
 * - defining calibration and parse calibration file
 */
public class MeasureCalibration {
    private final String name; /*name of calibration*/
    private final double pixelLength; /*length of 1 pixel in real life*/
    private final String unit; /*unit of length the measures should be in*/

//    CONSTRUCTORS
    /**
     * Constructor mainly used when parsing file
     * @param name : label to identify calibration
     * @param pixelLength : length of pixel
     * @param unit : unit of calibration
     */
    public MeasureCalibration(String name, String pixelLength, String unit) {
//        SET NAME
        this.name = name;
//        SET VALUE
        /*The values are considered as given as length, so they need to be converted to areas by multiplicating them*/
        double value_tmp;
        try {
            value_tmp = Double.parseDouble(pixelLength);
        }catch (NumberFormatException e){ /*If the value is not a number*/
            IJ.error("The value is not a number, please correct the file.");
            value_tmp=1;
        }
        this.pixelLength = value_tmp;

//        SET UNIT
        this.unit = unit;
    }

    /**
     * Constructor for default value of no calibration
     */
    public MeasureCalibration(){
//        SET NAME
        this.name = "No calibration";
//        SET VALUE
        this.pixelLength =1;
//        SET UNIT
        this.unit = "pixel";
    }

//      GETTER
    public String getName() {
    return name;
}
    public double getPixelLength(){
        return pixelLength;
    }
    public double getPixelArea() {
        return pixelLength*pixelLength;
    }
    public String getUnit() {
        return unit;
    }

//      METHODS/FUNCTIONS
    /**
     * Parse IJ_Calibration.txt file found in Prefs directory
     * If no file found, creates one with default value
     * @return ArrayList of all calibrations found in file
     */
    public static ArrayList<MeasureCalibration> getCalibrationFromFile(){
        String calibration_filename = Prefs.getPrefsDir()+"/IJ_Calibration.txt"; /*get localisation of file*/
        ArrayList<MeasureCalibration> measureCalibrations = new ArrayList<>(); /*init ArrayList*/
        try{
            BufferedReader reader = new BufferedReader(new FileReader(calibration_filename));
            String currentLine;
//            Reads each line til no more
            while ((currentLine=reader.readLine())!=null){
                if (!currentLine.startsWith("(")){/*Headings line contains ()*/
                    String[] calibration_values = currentLine.split(";");/*The three infos are separated by ;*/
                    if (calibration_values.length==3){
                        measureCalibrations.add(new MeasureCalibration(calibration_values[0],calibration_values[1],calibration_values[2]));
                    } else {/*If more or less infos display error*/
                        IJ.error("The calibration information need to be separated by ';' " +
                                "and there should only be the name, value and unit");
                    }
                }

            }
            reader.close();
        } catch (FileNotFoundException e) { /*If calibration file does not exist, creates one*/
            try {
                createCalibrationFile(false);
                return getCalibrationFromFile(); /* With file existing, it can be parsed */
            } catch (IOException ex) { /*If the file can not be created, display error message*/
                IJ.error(calibration_filename+" could not be found and could not be created." +
                        "It could be a problem of access rights of the ImageJ/Fiji preferences folder.");
                ex.printStackTrace();
            }
        } catch (IOException e) { /*If the file can not be read, display error message*/
            IJ.error("Could not read the file, it can be due to rights of access");
            e.printStackTrace();
        }
        return measureCalibrations;
    }

    /**
     * From a created Calibration instance, adds it to file
     */
    public void addCalibrationToFile(){
        String calibration_filename = Prefs.getPrefsDir()+"/IJ_Calibration.txt";
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(calibration_filename,true));
            output.newLine();
            output.append(name).append(";").append(String.valueOf(pixelLength)).append(";").append(unit);
            IJ.log("Added new calibration : name: "+ name +" value: " + pixelLength +" unit: "+ unit);
            output.close();
        } catch (IOException e) { /*If file can not be written in, display error message*/
            IJ.error("Could not add new calibration to the file, please verify rights of access \n" + e.getMessage());
        }
    }

    /**
     * Convert getCalibrationFromFile ArrayList to array
     * @return array of Calibrations contained in Calibration file
     */
    public static MeasureCalibration[] getCalibrationArrayFromFile(){
        ArrayList<MeasureCalibration> calibrationsArrayList = getCalibrationFromFile();
        MeasureCalibration[] calibrationsArray = new MeasureCalibration[calibrationsArrayList.size()];
        for (int i = 0; i < calibrationsArrayList.size(); i++) {
            calibrationsArray[i]=calibrationsArrayList.get(i);
        }
        return calibrationsArray;
    }

    /**
     * Creates Helpers.Calibration file in ImageJ prefs directory
     * @param alreadyExists : true if file already exists (so no creation, just replace)
     * @throws IOException : if file can not be created or written to, getCalibrationFromFile display error message
     */
    public static void createCalibrationFile(boolean alreadyExists) throws IOException {
        String calibration_filename = Prefs.getPrefsDir()+"/IJ_Calibration.txt";
        File calibration_file = new File(calibration_filename);
        if (!alreadyExists){
            if (calibration_file.createNewFile()){
                IJ.log("Creation of calibration file in "+ Prefs.getPrefsDir());
            }else {
                IJ.error("File already exists, but file was not found previously.");
                return;
            }
        }
        FileWriter fileWriter = new FileWriter(calibration_file.getAbsoluteFile());
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.append("(Name;Value;Measurement unit)");/*Write headings*/
        if (!alreadyExists) bufferedWriter.append("\nNo calibration;1.0;pix");/*Write default calibration*/
        /*if file already exists, the NewCalibrationPanel will already have the default value in memory*/
        bufferedWriter.close();
    }

    /**
     * From name of calibration, found the Calibration object corresponding
     * Used for the JComboBox
     * @param measureCalibrations : array of Calibrations objects
     * @param nameToFind : name of calibration to find
     * @return Helpers.Calibration corresponding to name
     */
    public static MeasureCalibration findCalibrationFromName(MeasureCalibration[] measureCalibrations, String nameToFind){
        MeasureCalibration toReturn = null;
        for (MeasureCalibration c: measureCalibrations
             ) {
            if(nameToFind.equals(c.getName())&& toReturn==null){
                toReturn=c;
            } else if (nameToFind.equals(c.getName())&&toReturn!=null){
                IJ.error("Multiple calibration with same name");
            }
        }
        if (toReturn == null){
            IJ.error("No calibration was found");
        }
        return toReturn;
    }


    @Override
    public String toString() {
        /*convert micro in greek letter */
        String unitMod = unit.startsWith("um")?"\u03BCm":unit;
        return name+"(x"+ d2s(pixelLength,3) +" "+ unitMod+")";
    }
}
