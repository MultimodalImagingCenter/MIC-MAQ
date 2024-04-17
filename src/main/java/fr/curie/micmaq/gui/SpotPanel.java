package fr.curie.micmaq.gui;

import fr.curie.micmaq.detectors.SpotDetector;
import fr.curie.micmaq.helpers.ImageToAnalyze;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import ij.IJ;
import ij.Prefs;
import ij.process.AutoThresholder;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Author : Camille RABIER
 * Date : 24/03/2022
 * GUI Class for
 * - defining parameters to use for spots
 */
public class SpotPanel {
    private final int id;
    private final boolean fromDirectory;
    //    GUI COMPONENTS
    private JPanel mainPanel;
    //    Image selection
    private JPanel chooseFilePanel;
    private JLabel spotChannelLabel;
    private JTextField spotChannelField;
    private JScrollPane imageListScroll;
    private JList<ImageToAnalyze> imageList;
    private JLabel imageEndingLabel;
    private JTextField imageEndingField;
    private JLabel errorImageEndingLabel;
    //    Preprocessing
//    ZStack
    private JPanel zStackParametersPanel;
    private JCheckBox isAZStackCheckBox;
    private JPanel zProjPanel;
    private JLabel zProjMethodsLabel;
    private JComboBox<String> zProjMethodsCombo;

    private JCheckBox chooseSlicesToUseCheckBox;
    private JSpinner firstSliceSpinner;
    private JSpinner lastSliceSpinner;
    //    Macro
    private JPanel macroPanel;
    private JCheckBox useAMacroCheckBox;
    private JTextArea macroArea;
    private JScrollPane macroAreaScroll;
    //    General parameters
    private JPanel generalParametersPanel;
    private JPanel choiceMethodPanel;
    private JButton previewButton;
    private JCheckBox useFindMaximaCheckBox;
    private JCheckBox useThresholdCheckBox;
    private JSpinner rollingBallSizeSpinner;
    private JCheckBox useRollingBallCheckBox;
    //    FindMaxima
    private JPanel maximaPanel;
    private JSpinner prominenceSpinner;
    private JLabel prominenceLabel;
    //    Threshold
    private JPanel thresholdPanel;
    private JComboBox<String> threshMethodsCombo;
    private JSpinner minSizeSpotSpinner;
    private JLabel minSizeSpotLabel;
    private JLabel threshMethodsLabel;
    private JPanel slicesPanel;
    private JCheckBox useWatershedCheckBox;

    //    SHOWING/SAVING
    private JCheckBox showImageCheckBox;
    private JCheckBox saveImageCheckBox;
    private JCheckBox saveROIsCheckBox;
    private JCheckBox showPreprocessingImageCheckBox;
    private JPanel saveAndShowPanel;
    private JPanel sbgParamPanel;

    //    NON GUI
    private final ImageToAnalyze[] imagesNames;
    private final DefaultListModel<ImageToAnalyze> imageListModel = new DefaultListModel<>();
    private boolean filteredImages;

    public SpotPanel(ImageToAnalyze[] ipList, int id, boolean fromDirectory) {
        this.fromDirectory = fromDirectory;
        $$$setupUI$$$();
//        this.measureCalibration = measureCalibration;
        imagesNames = ipList;
        this.id = id;
        //        GET PREFERENCES
        getPreferences();


        //        List of images
        if (imagesNames != null) {
            imageListModel.removeElement(null);
            for (ImageToAnalyze imagePlusDisplay : imagesNames) {
                imageListModel.addElement(imagePlusDisplay);
            }
            filteredImages = ImageToAnalyze.filterModelByEnding((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
            imageList.setSelectedIndex(0);
        } else {
            IJ.error("No images (SpotPanel" + id + ")");
        }
        imageEndingField.addActionListener(e -> {
            imageEndingField.setText(imageEndingField.getText().trim());
            filteredImages = ImageToAnalyze.filterModelByEnding((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
        });
//        ITEM LISTENERS
        isAZStackCheckBox.addItemListener(e -> zStackParametersPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        useAMacroCheckBox.addItemListener(e -> macroPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        chooseSlicesToUseCheckBox.addItemListener(e -> slicesPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        useFindMaximaCheckBox.addItemListener(e -> setEnablingOfMaximaPanel(e.getStateChange() == ItemEvent.SELECTED));
        useThresholdCheckBox.addItemListener(e -> setEnablingOfThresholdPanel(e.getStateChange() == ItemEvent.SELECTED));
        useRollingBallCheckBox.addItemListener(e -> sbgParamPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
//        PREVIEW
        previewButton.addActionListener(e -> {
            if (imageListModel.getSize() > 0) {
                if (!imageList.isSelectionEmpty()) {
                    ImageToAnalyze imageToPreview = imageList.getSelectedValue();
//                    String nameExperiment = imageToPreview.getImagePlus().getTitle().split(imageEndingField.getText())[0];
                    String nameExperiment = ImageToAnalyze.getNameExperiment(imageToPreview, imageEndingField);
                    String spotName = spotChannelField.getText();
                    SpotDetector previewPD = getSpotDetector(imageToPreview, spotName, nameExperiment, true);
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            previewPD.preview();
                            return null;
                        }
                    };
                    worker.execute();
                } else {
                    IJ.error("Please select an image name.");
                }
            } else {
                IJ.error("There is no image to be used to do a preview.");
            }
        });

    }


//    GETTERS

    /**
     * @return all {@link SpotDetector} corresponding to filtered images
     */
    private SpotDetector getSpotDetector(ImageToAnalyze firstImage, String spotName, String nameExperiment, boolean isPreview) {
        SpotDetector spotDetector;
        if (isPreview) {
            spotDetector = new SpotDetector(firstImage.getImagePlus(), spotName, nameExperiment, null, true);
        } else {
            spotDetector = new SpotDetector(firstImage.getImagePlus(), spotName, nameExperiment, firstImage.getDirectory(), showPreprocessingImageCheckBox.isSelected());
        }
        if (isAZStackCheckBox.isSelected()) {
            if (chooseSlicesToUseCheckBox.isSelected()) {
                spotDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()), (int) firstSliceSpinner.getValue(), (int) lastSliceSpinner.getValue());
            } else {
                spotDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
            }
        }
        if (useAMacroCheckBox.isSelected()) {
            spotDetector.setPreprocessingMacro(macroArea.getText());
        }
        if (useRollingBallCheckBox.isSelected()) {
            spotDetector.setRollingBallSize((double) rollingBallSizeSpinner.getValue());
        }
        spotDetector.setSaving(saveImageCheckBox.isSelected(), saveROIsCheckBox.isSelected());
        if (useFindMaximaCheckBox.isSelected()) {
            spotDetector.setSpotByFindMaxima((double) prominenceSpinner.getValue(), isPreview || showImageCheckBox.isSelected());
        }
        if (useThresholdCheckBox.isSelected()) {
            spotDetector.setSpotByThreshold(threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()), (double) minSizeSpotSpinner.getValue(), useWatershedCheckBox.isSelected(), true,isPreview || showImageCheckBox.isSelected());
        }

        return spotDetector;
    }

    /**
     * @return all {@link SpotDetector} corresponding to filtered images
     */
    public ArrayList<SpotDetector> getImages() {
        ArrayList<SpotDetector> selectedSpots = new ArrayList<>();
        ImageToAnalyze image;
        String nameExperiment;
        String spotName = spotChannelField.getText();
        /*if no image selected*/
        if (!filteredImages) {
            IJ.error("No images given for spot " + (id + 1) + ". Please verify the image ending corresponds to at least an image.");
            return null;
        } else {
            for (int i = 0; i < imageListModel.getSize(); i++) {
                image = imageListModel.getElementAt(i);
                nameExperiment = ImageToAnalyze.getNameExperiment(image, imageEndingField);
                selectedSpots.add(getSpotDetector(image, spotName, nameExperiment, false));
            }
            return selectedSpots;
        }
    }
//    OTHER FUNCTIONS/METHODS
    //PREFERENCES

    /**
     * Get preferences from ImageJ/Fiji file
     */
    private void getPreferences() {
//        Name spot
        spotChannelField.setText(Prefs.get("MICMAQ.spotName_" + id, "CY5"));
        imageEndingField.setText(Prefs.get("MICMAQ.spotEnding_" + id, "_w11 CY5"));

//        Z-projection
        isAZStackCheckBox.setSelected(Prefs.get("MICMAQ.zStackSpot_" + id, true));
        zProjMethodsCombo.setSelectedItem(Prefs.get("MICMAQ.ProjMethodsSpot_" + id, "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("MICMAQ.chooseSlicesSpot_" + id, false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            slicesPanel.setVisible(false);
        }
        int maxSlices = imagesNames[0].getImagePlus().getNSlices();
        int firstSlice = (int) Prefs.get("MICMAQ.firstSliceSpot_" + id, 1);
        int lastSlice = (int) Prefs.get("MICMAQ.lastSliceSpot_" + id, imagesNames[0].getImagePlus().getNSlices());
        ImageToAnalyze.assertSlices(maxSlices, firstSlice, lastSlice, firstSliceSpinner, lastSliceSpinner);

//        Macro
        useAMacroCheckBox.setSelected(Prefs.get("MICMAQ.useMacroSpot_" + id, false));
        if (!useAMacroCheckBox.isSelected()) macroPanel.setVisible(false);
        macroArea.append(Prefs.get("MICMAQ.macroSpot_" + id, " "));

//        Rolling ball
        useRollingBallCheckBox.setSelected(Prefs.get("MICMAQ.rollingballCheckSpot_" + id, true));
        rollingBallSizeSpinner.setValue(Prefs.get("MICMAQ.rollingballSizeSpot_" + id, 10));

//        Find maxima
        boolean maximaEnable = Prefs.get("MICMAQ.findMaximaSelectedSpot_" + id, true);
        useFindMaximaCheckBox.setSelected(maximaEnable);
        setEnablingOfMaximaPanel(maximaEnable);
        prominenceSpinner.setValue(Prefs.get("MICMAQ.prominence_" + id, 500));

//       Threshold
        boolean thresholdEnable = Prefs.get("MICMAQ.thresholdSelectedSpot_" + id, false);
        useThresholdCheckBox.setSelected(thresholdEnable);
        setEnablingOfThresholdPanel(thresholdEnable);
        threshMethodsCombo.setSelectedItem(Prefs.get("MICMAQ.thresholdMethodSpot_" + id, "Li"));
        minSizeSpotSpinner.setValue(Prefs.get("MICMAQ.minSizeSpot_" + id, 10));
        useWatershedCheckBox.setSelected(Prefs.get("MICMAQ.useWatershedSpot_" + id, false));


//        Show and save images ?
        saveROIsCheckBox.setSelected(Prefs.get("MICMAQ.saveROISpot_" + id, true));
        saveROIsCheckBox.setSelected(Prefs.get("MICMAQ.saveROISpot_" + id, true));

        if (fromDirectory) {
            showPreprocessingImageCheckBox.setSelected(false);
            showPreprocessingImageCheckBox.setVisible(false);
            showImageCheckBox.setVisible(false);
            showImageCheckBox.setSelected(false);
        } else {
            showPreprocessingImageCheckBox.setSelected(Prefs.get("MICMAQ.showPreproSpot_" + id, true));
            showImageCheckBox.setSelected(Prefs.get("MICMAQ.showImageSpot_" + id, true));
        }
    }


    /**
     * Set preferences for ImageJ/Fiji
     */
    public void setPreferences() {
//        Name spot
        Prefs.set("MICMAQ.spotName_" + id, spotChannelField.getText());
        Prefs.set("MICMAQ.spotEnding_" + id, imageEndingField.getText());

//        Z-projection
        Prefs.set("MICMAQ.zStackSpot_" + id, isAZStackCheckBox.isSelected());
        if (isAZStackCheckBox.isSelected()) {
            Prefs.set("MICMAQ.ProjMethodsSpot_" + id, zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
            Prefs.set("MICMAQ.chooseSlicesSpot_" + id, chooseSlicesToUseCheckBox.isSelected());
            if (chooseSlicesToUseCheckBox.isSelected()) {
                Prefs.set("MICMAQ.firstSliceSpot_" + id, (double) (int) firstSliceSpinner.getValue());
                Prefs.set("MICMAQ.lastSliceSpot_" + id, (double) (int) lastSliceSpinner.getValue());
            }
        }

//        Macro
        Prefs.set("MICMAQ.useMacroSpot_" + id, useAMacroCheckBox.isSelected());
        Prefs.set("MICMAQ.macroSpot_" + id, macroArea.getText());

//        Rolling ball
        Prefs.set("MICMAQ.rollingballCheckSpot_" + id, useRollingBallCheckBox.isSelected());
        Prefs.set("MICMAQ.rollingballSizeSpot_" + id, (double) rollingBallSizeSpinner.getValue());

//        Find maxima
        Prefs.set("MICMAQ.findMaximaSelectedSpot_" + id, useFindMaximaCheckBox.isSelected());
        Prefs.set("MICMAQ.prominence_" + id, (double) prominenceSpinner.getValue());

//        Threshold
        Prefs.set("MICMAQ.thresholdSelectedSpot_" + id, useThresholdCheckBox.isSelected());
        Prefs.set("MICMAQ.thresholdMethodSpot_" + id, threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()));
        Prefs.set("MICMAQ.minSizeSpot_" + id, (double) minSizeSpotSpinner.getValue());
        Prefs.set("MICMAQ.useWatershedSpot_" + id, useWatershedCheckBox.isSelected());

//        Show and save images ?
        Prefs.set("MICMAQ.saveROISpot_" + id, saveROIsCheckBox.isSelected());
        Prefs.set("MICMAQ.saveMaskSpot_" + id, saveImageCheckBox.isSelected());

        if (!fromDirectory) {
            Prefs.set("MICMAQ.showPreproSpot_" + id, showPreprocessingImageCheckBox.isSelected());
            Prefs.set("MICMAQ.showImageSpot_" + id, showImageCheckBox.isSelected());
        }

    }

    /**
     * Add all parameters chosen in panel to file
     */
    public void addParametersToFile() {
        String directory = imageListModel.getElementAt(0).getDirectory();
        if (directory != null) {
            String parameterFilename = directory + "/Results/Parameters.txt";
            try {
                FileWriter fileWriter = new FileWriter(parameterFilename, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.append("\nSPOT ").append(spotChannelField.getText()).append(" Parameters");
                if (useFindMaximaCheckBox.isSelected()) {
                    bufferedWriter.append("\nFind by maxima parameters:");
                    bufferedWriter.append("\nProminence :").append(String.valueOf(prominenceSpinner.getValue()));
                }
                if (useThresholdCheckBox.isSelected()) {
                    bufferedWriter.append("\nThresholding parameters : ");
                    bufferedWriter.append("\nThreshold method :").append(threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()));
                    bufferedWriter.append("\nMinimum spot diameter :").append(String.valueOf(minSizeSpotSpinner.getValue()));
                    bufferedWriter.append("\nThresholding watershed ").append(useWatershedCheckBox.isSelected() ? "yes" : "no");
                }
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                IJ.log("The parameters could not be written.");
            }
        }
    }
//    GUI

    /**
     * Show or not saving results checkbox
     *
     * @param wantToSave : true if there is a need to show savings checkbox
     */
    public void saveResultsCheckbox(boolean wantToSave) {
        saveImageCheckBox.setVisible(wantToSave);
        saveROIsCheckBox.setVisible(wantToSave);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    //Enable panel according to checkbox
    private void setEnablingOfThresholdPanel(boolean enabled) {
        thresholdPanel.setEnabled(enabled);
        threshMethodsCombo.setEnabled(enabled);
        threshMethodsLabel.setEnabled(enabled);
        useWatershedCheckBox.setEnabled(enabled);
        threshMethodsCombo.setEnabled(enabled);
        minSizeSpotSpinner.setEnabled(enabled);
        minSizeSpotLabel.setEnabled(enabled);
        if (enabled) {
            saveAndShowPanel.setEnabled(true);
            showImageCheckBox.setEnabled(true);
            saveImageCheckBox.setEnabled(true);
            saveROIsCheckBox.setEnabled(true);
        } else if (!useFindMaximaCheckBox.isSelected()) {
            saveAndShowPanel.setEnabled(false);
            showImageCheckBox.setEnabled(false);
            saveImageCheckBox.setEnabled(false);
            saveROIsCheckBox.setEnabled(false);
        }
    }

    private void setEnablingOfMaximaPanel(boolean enabled) {
        maximaPanel.setEnabled(enabled);
        prominenceLabel.setEnabled(enabled);
        prominenceSpinner.setEnabled(enabled);
        if (enabled) {
            saveAndShowPanel.setEnabled(true);
            showImageCheckBox.setEnabled(true);
            saveImageCheckBox.setEnabled(true);
            saveROIsCheckBox.setEnabled(true);
        } else if (!useThresholdCheckBox.isSelected()) {
            saveAndShowPanel.setEnabled(false);
            showImageCheckBox.setEnabled(false);
            saveImageCheckBox.setEnabled(false);
            saveROIsCheckBox.setEnabled(false);
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Quantify signal", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        chooseFilePanel = new JPanel();
        chooseFilePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(chooseFilePanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        chooseFilePanel.setBorder(BorderFactory.createTitledBorder(null, "Choice of image", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        imageListScroll = new JScrollPane();
        chooseFilePanel.add(imageListScroll, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageList.setEnabled(true);
        imageList.setSelectionMode(0);
        imageListScroll.setViewportView(imageList);
        imageEndingLabel = new JLabel();
        imageEndingLabel.setText("Text defining channel");
        chooseFilePanel.add(imageEndingLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        imageEndingField.setToolTipText("Enter the text defining the channel in the file name");
        chooseFilePanel.add(imageEndingField, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        errorImageEndingLabel = new JLabel();
        errorImageEndingLabel.setText("No image corresponding to ending");
        chooseFilePanel.add(errorImageEndingLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        chooseFilePanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        zProjPanel = new JPanel();
        zProjPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(zProjPanel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        zProjPanel.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        useRollingBallCheckBox = new JCheckBox();
        useRollingBallCheckBox.setText("Substract background");
        zProjPanel.add(useRollingBallCheckBox, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 40), null, null, 0, false));
        sbgParamPanel = new JPanel();
        sbgParamPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(sbgParamPanel, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Size of rolling ball");
        sbgParamPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sbgParamPanel.add(rollingBallSizeSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showPreprocessingImageCheckBox = new JCheckBox();
        showPreprocessingImageCheckBox.setEnabled(true);
        showPreprocessingImageCheckBox.setText("Show preprocessing image");
        showPreprocessingImageCheckBox.setToolTipText("Warning ! Process will pause after each set of images' measurement");
        zProjPanel.add(showPreprocessingImageCheckBox, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, new Dimension(-1, 61), null, null, 0, false));
        isAZStackCheckBox = new JCheckBox();
        isAZStackCheckBox.setSelected(true);
        isAZStackCheckBox.setText("Is a Z-stack ?");
        panel1.add(isAZStackCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTHWEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        zStackParametersPanel = new JPanel();
        zStackParametersPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(zStackParametersPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        zProjMethodsLabel = new JLabel();
        zProjMethodsLabel.setText("Method of projection");
        zStackParametersPanel.add(zProjMethodsLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zProjMethodsCombo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Maximum projection");
        defaultComboBoxModel1.addElement("Standard Deviation projection");
        zProjMethodsCombo.setModel(defaultComboBoxModel1);
        zStackParametersPanel.add(zProjMethodsCombo, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        slicesPanel = new JPanel();
        slicesPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        zStackParametersPanel.add(slicesPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        slicesPanel.add(firstSliceSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        slicesPanel.add(lastSliceSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("Choose slices to use");
        zStackParametersPanel.add(chooseSlicesToUseCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 28), null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        panel2.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 100), null, null, 0, false));
        useAMacroCheckBox = new JCheckBox();
        useAMacroCheckBox.setText("Use macro code");
        panel3.add(useAMacroCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTHWEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        macroPanel = new JPanel();
        macroPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(macroPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroAreaScroll = new JScrollPane();
        macroPanel.add(macroAreaScroll, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroArea = new JTextArea();
        macroArea.setRows(2);
        macroAreaScroll.setViewportView(macroArea);
        generalParametersPanel = new JPanel();
        generalParametersPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(generalParametersPanel, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        previewButton = new JButton();
        previewButton.setText("Preview");
        generalParametersPanel.add(previewButton, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        choiceMethodPanel = new JPanel();
        choiceMethodPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        generalParametersPanel.add(choiceMethodPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        choiceMethodPanel.setBorder(BorderFactory.createTitledBorder(null, "Spot detection method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        maximaPanel = new JPanel();
        maximaPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        choiceMethodPanel.add(maximaPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        maximaPanel.setBorder(BorderFactory.createTitledBorder(null, "Find maxima method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        prominenceLabel = new JLabel();
        prominenceLabel.setText("Prominence");
        maximaPanel.add(prominenceLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maximaPanel.add(prominenceSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel = new JPanel();
        thresholdPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        choiceMethodPanel.add(thresholdPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        thresholdPanel.setBorder(BorderFactory.createTitledBorder(null, "Threshold method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        minSizeSpotLabel = new JLabel();
        minSizeSpotLabel.setText("Minimum size of spot");
        thresholdPanel.add(minSizeSpotLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel.add(minSizeSpotSpinner, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threshMethodsLabel = new JLabel();
        threshMethodsLabel.setText("Threshold Method");
        thresholdPanel.add(threshMethodsLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        threshMethodsCombo.setModel(defaultComboBoxModel2);
        thresholdPanel.add(threshMethodsCombo, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        useWatershedCheckBox = new JCheckBox();
        useWatershedCheckBox.setText("Use watershed");
        thresholdPanel.add(useWatershedCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useFindMaximaCheckBox = new JCheckBox();
        useFindMaximaCheckBox.setSelected(true);
        useFindMaximaCheckBox.setText("Find maxima");
        choiceMethodPanel.add(useFindMaximaCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useThresholdCheckBox = new JCheckBox();
        useThresholdCheckBox.setSelected(true);
        useThresholdCheckBox.setText("Threshold");
        choiceMethodPanel.add(useThresholdCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveAndShowPanel = new JPanel();
        saveAndShowPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        choiceMethodPanel.add(saveAndShowPanel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        saveROIsCheckBox = new JCheckBox();
        saveROIsCheckBox.setText("Save ROIs");
        saveAndShowPanel.add(saveROIsCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showImageCheckBox = new JCheckBox();
        showImageCheckBox.setText("Show image(s)");
        showImageCheckBox.setToolTipText("Warning ! Process will pause after each set of images' measurement");
        saveAndShowPanel.add(showImageCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveImageCheckBox = new JCheckBox();
        saveImageCheckBox.setText("Save image(s)");
        saveAndShowPanel.add(saveImageCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotChannelField = new JTextField();
        spotChannelField.setText("");
        mainPanel.add(spotChannelField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        spotChannelLabel = new JLabel();
        spotChannelLabel.setText("Name of protein");
        mainPanel.add(spotChannelLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private void createUIComponents() {
        imageListModel.addElement(null);
        imageList = new JList<>(imageListModel);
        imageList.setVisibleRowCount(4);

// List of methods for thresholdPanel
        threshMethodsCombo = new JComboBox<>(AutoThresholder.getMethods());

//  Textfield to filter extension
        imageEndingField = new JTextField(15);
//        minSizeThr spinner
        minSizeSpotSpinner = new JSpinner(new SpinnerNumberModel(10, 0.0, Integer.MAX_VALUE, 1));

//        prominence spinner
        prominenceSpinner = new JSpinner(new SpinnerNumberModel(500.0, 0.0, Integer.MAX_VALUE, 1));
//rolling ball size
        rollingBallSizeSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, Integer.MAX_VALUE, 1.0));
        firstSliceSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        lastSliceSpinner = new JSpinner(new SpinnerNumberModel(33, 0, 9999, 1));
    }


}

