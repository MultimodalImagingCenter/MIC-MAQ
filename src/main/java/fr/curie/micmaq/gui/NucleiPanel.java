package fr.curie.micmaq.gui;

import fr.curie.micmaq.detectors.NucleiDetector;
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Author : Camille RABIER
 * Date : 23/03/2022
 * GUI Class for
 * - defining parameters to use for nuclei
 */
public class NucleiPanel extends JPanel {
    //    GUI
    private JPanel mainPanel;
    private JButton previewButton;

    //    Choose file by extension
    private JPanel chooseFilePanel;
    private JScrollPane imageListScrolling;
    private JList<ImageToAnalyze> imageList;
    private JTextField imageEndingField;
    private JLabel imageEndingLabel;
    private JLabel errorImageEndingLabel;

    //    Preprocessing : Z-stack
    private JPanel zProjPanel;
    private JCheckBox isAZStackCheckBox;
    // Preprocessing : Z-stack general parameters
    private JPanel zStackParameters;
    private JLabel zProjMethodsLabel;
    private JComboBox<String> zProjMethodsCombo;
    private JCheckBox chooseSlicesToUseCheckBox;
    private JSpinner firstSliceSpinner;
    private JSpinner lastSliceSpinner;

    //    Preprocessing : Macro
    private JPanel macroPanel;
    private JScrollPane macroAreaScroll;
    private JCheckBox useAMacroCheckBox;
    private JTextArea macroArea;

    //    Choice of segmentation method
    private JPanel methodPanel;
    private JLabel segmentationMethodsLabel;
    private JRadioButton thresholdRadioButton;
    private JRadioButton cellPoseRadioButton;

    //    Thresholding parameters
    private JPanel thresholdingParametersPanel;
    private JLabel threshMethodsLabel;
    private JComboBox<String> threshMethodsCombo;
    private JLabel minSizeNucleusLabel;
    private JSpinner minSizeNucleusSpinner;
    //  Thresholding parameters : supplementary options
    private JCheckBox useWatershedCheckBox;
    private JCheckBox finalValidationCheckBox;

    //    CELLPOSE
    private JPanel cellPosePanel;
    private JComboBox<String> cellPoseModelCombo;

    private JSpinner cellPoseMinDiameterSpinner;
    private JLabel cellPoseMinDiameterLabel;
    private JLabel cellPoseModelLabel;
    //    CELLPOSE : OWN MODEL
    private JPanel ownModelPanel;
    private JLabel modelPathLabel;
    private JTextField modelPathField;
    private JButton modelBrowseButton;


    //    CELLPOSE & THRESHOLD
    private JPanel commonParameters;
    private JCheckBox excludeOnEdges;
    private JCheckBox showBinaryMaskCheckBox;
    private JCheckBox saveNucleiROIsCheckBox;
    private JCheckBox saveSegmentationMaskCheckBox;
    private JCheckBox showPreprocessingImageCheckBox;
    private JPanel optionsPanel;


    //    NON GUI
    private final boolean fromDirectory; /*True if image from directory*/
    private final ImageToAnalyze[] imagesNames;
    private final DefaultListModel<ImageToAnalyze> imageListModel = new DefaultListModel<>();
    private boolean filteredImages; /*true if there are filtered image*/
    private int measurements;
    private File cellposeModelPath;

//    CONSTRUCTOR

    /**
     * @param imageNames : all images to analyze
     */
    public NucleiPanel(ImageToAnalyze[] imageNames, boolean fromDirectory) {
        this.fromDirectory = fromDirectory;
        $$$setupUI$$$();
        this.imagesNames = imageNames;
        chooseSlicesToUseCheckBox.setPreferredSize(new Dimension(-1, (int) firstSliceSpinner.getSize().getHeight()));
        getPreferences();
        //        List of images (filter)
        if (imagesNames != null) {
            imageListModel.removeElement(null);
            for (ImageToAnalyze ip : imagesNames) {
                imageListModel.addElement(ip);
            }
            filteredImages = ImageToAnalyze.filterModelByEnding((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
            imageList.setSelectedIndex(0);
        } else {
            IJ.error("No images (NucleiPanel");
        }

        imageEndingField.addActionListener(e -> {
            imageEndingField.setText(imageEndingField.getText().trim());
            filteredImages = ImageToAnalyze.filterModelByEnding((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
        });

//        ITEM LISTENERS : Add/Remove element of panel according to choice
        isAZStackCheckBox.addItemListener(e -> {
            zStackParameters.setVisible(e.getStateChange() == ItemEvent.SELECTED);
//            zProjMethodsLabel.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//            zProjMethodsCombo.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//            chooseSlicesToUseCheckBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        });
        chooseSlicesToUseCheckBox.addItemListener(e -> {
            //slicesPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            firstSliceSpinner.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            lastSliceSpinner.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            //mainPanel.revalidate();
        });
        useAMacroCheckBox.addItemListener(e -> macroPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        cellPoseRadioButton.addItemListener(e -> {
            cellPosePanel.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            thresholdingParametersPanel.setVisible(e.getStateChange() == ItemEvent.DESELECTED);
        });
        cellPoseModelCombo.addItemListener(e -> {
            String modelSelected = (String) cellPoseModelCombo.getSelectedItem();
            ownModelPanel.setVisible(modelSelected.equals("own_model"));
        });

        //        BUTTON TO CHOOSE CELLPOSE MODEL PATH
        modelBrowseButton.addActionListener(e -> {
            cellposeModelPath = CytoCellPanel.chooseCellposeModelFile(this.mainPanel);
            if (cellposeModelPath != null) {
                String path = cellposeModelPath.getAbsolutePath();
                if (path.split("\\\\").length > 2) {
                    String path_shorten = path.substring(path.substring(0, path.lastIndexOf("\\")).lastIndexOf("\\"));
                    modelPathField.setText("..." + path_shorten);
                } else if (path.split("/").length > 2) {
                    String path_shorten = path.substring(path.substring(0, path.lastIndexOf("/")).lastIndexOf("/"));
                    modelPathField.setText("..." + path_shorten);
                } else {
                    modelPathField.setText(path);
                }
            }
        });
//        PREVIEW : on the selected image
        previewButton.addActionListener(e -> {
            if (imageListModel.getSize() > 0) {
                if (!imageList.isSelectionEmpty()) {
                    ImageToAnalyze imageToPreview = imageList.getSelectedValue();
                    String nameExperiment = ImageToAnalyze.getNameExperiment(imageToPreview, imageEndingField);
                    NucleiDetector previewND = getNucleiDetector(imageToPreview, nameExperiment, true);
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            previewND.preview();
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

//GETTERS

    /**
     * @param imageToAnalyze : image to analyze
     * @param nameExperiment : experiment name associated to the image
     * @param isPreview      : true if preview button clicked
     * @return : instance of {@link NucleiDetector} : class that do the analysis
     */
    private NucleiDetector getNucleiDetector(ImageToAnalyze imageToAnalyze, String nameExperiment, boolean isPreview) {
        NucleiDetector nucleiDetector;
        if (isPreview) {
            nucleiDetector = new NucleiDetector(imageToAnalyze.getImagePlus(), nameExperiment, null, true);
        } else { /*no need to do measurements */
            nucleiDetector = new NucleiDetector(imageToAnalyze.getImagePlus(), nameExperiment, imageToAnalyze.getDirectory(), showPreprocessingImageCheckBox.isSelected());
            nucleiDetector.setMeasurements(measurements);
        }
        if (isAZStackCheckBox.isSelected()) {
            if (chooseSlicesToUseCheckBox.isSelected()) {
                nucleiDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()), (int) firstSliceSpinner.getValue(), (int) lastSliceSpinner.getValue());
            } else {
                nucleiDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
            }
        }
        if (useAMacroCheckBox.isSelected()) {
            nucleiDetector.setPreprocessingMacro(macroArea.getText());
        }
        if (cellPoseRadioButton.isSelected()) {
            String cellposeModel = cellPoseModelCombo.getSelectedItem() == "own_model" ? cellposeModelPath.getAbsolutePath() : (String) cellPoseModelCombo.getSelectedItem();
            nucleiDetector.setCellposeMethod((Integer) cellPoseMinDiameterSpinner.getValue(),
                    0, cellposeModel, excludeOnEdges.isSelected());
        } else {
            nucleiDetector.setThresholdMethod(threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()), (Double) minSizeNucleusSpinner.getValue(), useWatershedCheckBox.isSelected(), excludeOnEdges.isSelected());
        }
        nucleiDetector.setSegmentation(finalValidationCheckBox.isSelected(), isPreview || showBinaryMaskCheckBox.isSelected());
        if (saveSegmentationMaskCheckBox.isVisible()) {
            nucleiDetector.setSavings(saveSegmentationMaskCheckBox.isSelected(), saveNucleiROIsCheckBox.isSelected(), false);
        }
        return nucleiDetector;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    //      SETTERS
    public void setMeasurements(int measuresNuclei) {
        this.measurements = measuresNuclei;
    }

//    OTHER FUNCTIONS & METHODS

    /**
     * @param parametersToAdd : true if there is a need to add parameters to file
     * @return all {@link NucleiDetector} corresponding to filtered images
     */
    public ArrayList<NucleiDetector> getImages(boolean parametersToAdd) {
        ArrayList<NucleiDetector> nuclei = new ArrayList<>();
        ImageToAnalyze image;
        String nameExperiment;
        if (!filteredImages) {
            IJ.error("No images given for nuclei. Please verify the image ending corresponds to at least an image.");
            return null;
        } else {
            if (parametersToAdd) {
                addParametersToFile();
            }
            for (int i = 0; i < imageListModel.getSize(); i++) {
                image = imageListModel.getElementAt(i);
                nameExperiment = ImageToAnalyze.getNameExperiment(image, imageEndingField);
                nuclei.add(getNucleiDetector(image, nameExperiment, false));
            }
            return nuclei;
        }
    }

    /**
     * Add all parameters chosen in panel to file
     */
    private void addParametersToFile() {
        String directory = imageListModel.getElementAt(0).getDirectory();
        if (directory != null) {
            String parameterFilename = directory + "/Results/Parameters.txt";
            try {
                FileWriter fileWriter = new FileWriter(parameterFilename, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.append("\nNUCLEI PARAMETERS:");
                if (useAMacroCheckBox.isSelected()) {
                    bufferedWriter.append("\nMacro used:\n").append(macroArea.getText());
                }
                if (cellPoseRadioButton.isSelected()) {
                    bufferedWriter.append("\nUse of cellpose: ");
                    bufferedWriter.append("\nCellpose model: ").append(cellPoseModelCombo.getItemAt(cellPoseModelCombo.getSelectedIndex()));
                    bufferedWriter.append("\nCellpose cell diameter: ").append(String.valueOf(cellPoseMinDiameterSpinner.getValue()));
                    bufferedWriter.append("\nCellpose excludeOnEdges: ").append(excludeOnEdges.isSelected() ? "yes" : "no");
                } else {
                    bufferedWriter.append("\nUse of thresholding: ");
                    bufferedWriter.append("\nThreshold method: ").append(threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()));
                    bufferedWriter.append("\nMinimum cell diameter: ").append(String.valueOf(minSizeNucleusSpinner.getValue()));
                    bufferedWriter.append("\nThresholding excludeOnEdges: ").append(excludeOnEdges.isSelected() ? "yes" : "no");
                    bufferedWriter.append("\nThresholding watershed: ").append(useWatershedCheckBox.isSelected() ? "yes" : "no");
                    bufferedWriter.append("\nThresholding final validation: ").append(finalValidationCheckBox.isSelected() ? "yes" : "no");
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
        saveNucleiROIsCheckBox.setVisible(wantToSave);
        saveSegmentationMaskCheckBox.setVisible(wantToSave);
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
        mainPanel.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Detect nuclei", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        thresholdingParametersPanel = new JPanel();
        thresholdingParametersPanel.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(thresholdingParametersPanel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        thresholdingParametersPanel.setBorder(BorderFactory.createTitledBorder(null, "Thresholding parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        threshMethodsLabel = new JLabel();
        threshMethodsLabel.setText("Threshold method");
        thresholdingParametersPanel.add(threshMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minSizeNucleusLabel = new JLabel();
        minSizeNucleusLabel.setText("Minimum size of nuclei (pixels)");
        thresholdingParametersPanel.add(minSizeNucleusLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdingParametersPanel.add(minSizeNucleusSpinner, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdingParametersPanel.add(threshMethodsCombo, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useWatershedCheckBox = new JCheckBox();
        useWatershedCheckBox.setText("Use watershed");
        thresholdingParametersPanel.add(useWatershedCheckBox, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseFilePanel = new JPanel();
        chooseFilePanel.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(chooseFilePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        chooseFilePanel.setBorder(BorderFactory.createTitledBorder(null, "Choice of image", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        imageListScrolling = new JScrollPane();
        chooseFilePanel.add(imageListScrolling, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        imageList.setEnabled(true);
        imageListScrolling.setViewportView(imageList);
        imageEndingLabel = new JLabel();
        imageEndingLabel.setText("Text defining channel");
        chooseFilePanel.add(imageEndingLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        imageEndingField.setToolTipText("Enter the text defining the channel in the file name");
        chooseFilePanel.add(imageEndingField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        errorImageEndingLabel = new JLabel();
        errorImageEndingLabel.setText("No image corresponding to ending");
        chooseFilePanel.add(errorImageEndingLabel, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        methodPanel = new JPanel();
        methodPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(methodPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        methodPanel.setBorder(BorderFactory.createTitledBorder(null, "Method of segmentation", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        segmentationMethodsLabel = new JLabel();
        segmentationMethodsLabel.setText("Which method of segmentation do you want to use ?");
        methodPanel.add(segmentationMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdRadioButton = new JRadioButton();
        thresholdRadioButton.setSelected(true);
        thresholdRadioButton.setText("Threshold");
        methodPanel.add(thresholdRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseRadioButton = new JRadioButton();
        cellPoseRadioButton.setText("CellPose");
        methodPanel.add(cellPoseRadioButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zProjPanel = new JPanel();
        zProjPanel.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(zProjPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(487, 48), null, 0, false));
        zProjPanel.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        macroPanel = new JPanel();
        macroPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(macroPanel, new GridConstraints(1, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroAreaScroll = new JScrollPane();
        macroPanel.add(macroAreaScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroArea = new JTextArea();
        macroArea.setPreferredSize(new Dimension(1, 36));
        macroArea.setRows(2);
        macroAreaScroll.setViewportView(macroArea);
        final Spacer spacer1 = new Spacer();
        zProjPanel.add(spacer1, new GridConstraints(1, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(panel1, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, new Dimension(-1, 61), null, null, 0, false));
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(optionsPanel, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        zStackParameters = new JPanel();
        zStackParameters.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        optionsPanel.add(zStackParameters, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        zProjMethodsLabel = new JLabel();
        zProjMethodsLabel.setEnabled(true);
        zProjMethodsLabel.setText("Method of projection");
        zStackParameters.add(zProjMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zProjMethodsCombo = new JComboBox();
        zProjMethodsCombo.setEnabled(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Maximum projection");
        defaultComboBoxModel1.addElement("Standard Deviation projection");
        zProjMethodsCombo.setModel(defaultComboBoxModel1);
        zStackParameters.add(zProjMethodsCombo, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("Choose slices to use");
        zStackParameters.add(chooseSlicesToUseCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 28), null, null, 0, false));
        zStackParameters.add(firstSliceSpinner, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zStackParameters.add(lastSliceSpinner, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        zStackParameters.add(spacer2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        optionsPanel.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        isAZStackCheckBox = new JCheckBox();
        isAZStackCheckBox.setSelected(true);
        isAZStackCheckBox.setText("Is a Z-stack ?");
        panel1.add(isAZStackCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(panel2, new GridConstraints(1, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        useAMacroCheckBox = new JCheckBox();
        useAMacroCheckBox.setText("Use macro code");
        panel2.add(useAMacroCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showPreprocessingImageCheckBox = new JCheckBox();
        showPreprocessingImageCheckBox.setText("Show preprocessing image");
        showPreprocessingImageCheckBox.setToolTipText("Warning ! Process will pause after each set of images' measurement");
        panel2.add(showPreprocessingImageCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel2.add(spacer4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        cellPosePanel = new JPanel();
        cellPosePanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(cellPosePanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        cellPosePanel.setBorder(BorderFactory.createTitledBorder(null, "Deep learning parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        cellPosePanel.add(cellPoseMinDiameterSpinner, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseMinDiameterLabel = new JLabel();
        cellPoseMinDiameterLabel.setText("Minimum diameter of nuclei (pixel)");
        cellPosePanel.add(cellPoseMinDiameterLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseModelLabel = new JLabel();
        cellPoseModelLabel.setText("Model for Cellpose");
        cellPosePanel.add(cellPoseModelLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseModelCombo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("nuclei");
        defaultComboBoxModel2.addElement("cyto");
        defaultComboBoxModel2.addElement("cyto2");
        defaultComboBoxModel2.addElement("bact_omni");
        defaultComboBoxModel2.addElement("tissuenet");
        defaultComboBoxModel2.addElement("livecell");
        defaultComboBoxModel2.addElement("own_model");
        cellPoseModelCombo.setModel(defaultComboBoxModel2);
        cellPosePanel.add(cellPoseModelCombo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ownModelPanel = new JPanel();
        ownModelPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        cellPosePanel.add(ownModelPanel, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        modelPathLabel = new JLabel();
        modelPathLabel.setText("Path own model");
        ownModelPanel.add(modelPathLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        modelPathField = new JTextField();
        ownModelPanel.add(modelPathField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        modelBrowseButton = new JButton();
        modelBrowseButton.setText("Browse");
        ownModelPanel.add(modelBrowseButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previewButton = new JButton();
        previewButton.setText("Preview");
        mainPanel.add(previewButton, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        commonParameters = new JPanel();
        commonParameters.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(commonParameters, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        excludeOnEdges = new JCheckBox();
        excludeOnEdges.setSelected(true);
        excludeOnEdges.setText("Exclude on edges");
        commonParameters.add(excludeOnEdges, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showBinaryMaskCheckBox = new JCheckBox();
        showBinaryMaskCheckBox.setText("Show segmentation mask");
        showBinaryMaskCheckBox.setToolTipText("Warning ! Process will pause after each set of images' measurement");
        commonParameters.add(showBinaryMaskCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        finalValidationCheckBox = new JCheckBox();
        finalValidationCheckBox.setText("Final validation");
        commonParameters.add(finalValidationCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveNucleiROIsCheckBox = new JCheckBox();
        saveNucleiROIsCheckBox.setText("Save ROIs");
        commonParameters.add(saveNucleiROIsCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveSegmentationMaskCheckBox = new JCheckBox();
        saveSegmentationMaskCheckBox.setText("Save segmentation mask");
        commonParameters.add(saveSegmentationMaskCheckBox, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threshMethodsLabel.setLabelFor(threshMethodsCombo);
        minSizeNucleusLabel.setLabelFor(minSizeNucleusSpinner);
        cellPoseMinDiameterLabel.setLabelFor(cellPoseMinDiameterSpinner);
        cellPoseModelLabel.setLabelFor(cellPoseModelCombo);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(thresholdRadioButton);
        buttonGroup.add(cellPoseRadioButton);
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


// List of methods for threshold
        threshMethodsCombo = new JComboBox<>(AutoThresholder.getMethods());

// minSize spinner
        minSizeNucleusSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 0.0, Integer.MAX_VALUE, 10.0));
        cellPoseMinDiameterSpinner = new JSpinner(new SpinnerNumberModel(200, 0, Integer.MAX_VALUE, 10));

//  Text field to filter extension
        imageEndingField = new JTextField(15);
        firstSliceSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        lastSliceSpinner = new JSpinner(new SpinnerNumberModel(33, 0, 9999, 1));
    }

//    PREFS

    /**
     * Preset panel choices according to prefs
     */
    private void getPreferences() {
//        Name
        imageEndingField.setText(Prefs.get("MICMAQ.nucleusEnding", "_w31 DAPI 405"));
//        Projection
        isAZStackCheckBox.setSelected(Prefs.get("MICMAQ.zStackNucleus", true));
        zProjMethodsCombo.setSelectedItem(Prefs.get("MICMAQ.ProjMethodsNucleus", "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("MICMAQ.chooseSlicesNucleus", false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            //slicesPanel.setVisible(false);
            firstSliceSpinner.setVisible(false);
            lastSliceSpinner.setVisible(false);
        }
        int maxSlices = imagesNames[0].getImagePlus().getNSlices();
        int firstSlice = (int) Prefs.get("MICMAQ.firstSliceNucleus", 1);
        int lastSlice = (int) Prefs.get("MICMAQ.lastSliceNucleus", imagesNames[0].getImagePlus().getNSlices());
        ImageToAnalyze.assertSlices(maxSlices, firstSlice, lastSlice, firstSliceSpinner, lastSliceSpinner);
//        if ((Integer) firstSliceSpinner.getValue() > imagesNames[0].getImagePlus().getNSlices()) {
//            firstSliceSpinner.setValue(1);
//        }
//        if ((Integer) lastSliceSpinner.getValue() > imagesNames[0].getImagePlus().getNSlices()) {
//            lastSliceSpinner.setValue(1);
//        }
//        Macro
        useAMacroCheckBox.setSelected(Prefs.get("MICMAQ.useMacroNucleus", false));
        if (!useAMacroCheckBox.isSelected()) macroPanel.setVisible(false);
        macroArea.append(Prefs.get("MICMAQ.macroNucleus", " "));

//        Segmentation
        cellPoseRadioButton.setSelected(Prefs.get("MICMAQ.useDeepLearningNucleus", false));
        if (cellPoseRadioButton.isSelected()) thresholdingParametersPanel.setVisible(false);
        else cellPosePanel.setVisible(false);
        excludeOnEdges.setSelected(Prefs.get("MICMAQ.nucleusExcludeOnEdges", true));
        finalValidationCheckBox.setSelected(Prefs.get("MICMAQ.nucleiFinalValidation", false));
        saveSegmentationMaskCheckBox.setSelected(Prefs.get("MICMAQ.nucleusSaveMask", true));
        saveNucleiROIsCheckBox.setSelected(Prefs.get("MICMAQ.nucleusSaveROI", true));
//        --> threshold
        threshMethodsCombo.setSelectedItem(Prefs.get("MICMAQ.thresholdMethodNucleus", "Li"));
        minSizeNucleusSpinner.setValue(Prefs.get("MICMAQ.minSizeNucleus", 1000));
        useWatershedCheckBox.setSelected(Prefs.get("MICMAQ.nucleiUseWaterShed", false));
//        --> cellpose
        cellPoseModelCombo.setSelectedItem(Prefs.get("MICMAQ.cellposeNucleusMethods", "cyto2"));
        if (cellPoseModelCombo.getSelectedItem() != "own_model") {
            ownModelPanel.setVisible(false);
        }
        cellPoseMinDiameterSpinner.setValue((int) Prefs.get("MICMAQ.cellposeNucleusMinDiameter", 100));

//        Show images
        if (fromDirectory) {
            showPreprocessingImageCheckBox.setSelected(false);
            showPreprocessingImageCheckBox.setVisible(false);
            showBinaryMaskCheckBox.setSelected(false);
            showBinaryMaskCheckBox.setVisible(false);
        } else {
            showPreprocessingImageCheckBox.setSelected(Prefs.get("MICMAQ.nucleusShowPrepro", true));
            showBinaryMaskCheckBox.setSelected(Prefs.get("MICMAQ.nucleusShowMask", true));
        }
    }

    /**
     * set prefs in prefs file
     */
    public void setPreferences() {
//        Name
        Prefs.set("MICMAQ.nucleusEnding", imageEndingField.getText());
//        Projection
        Prefs.set("MICMAQ.zStackNucleus", isAZStackCheckBox.isSelected());
        if (isAZStackCheckBox.isSelected()) {
            Prefs.set("MICMAQ.ProjMethodsNucleus", zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
            Prefs.set("MICMAQ.chooseSlicesNucleus", chooseSlicesToUseCheckBox.isSelected());
            if (chooseSlicesToUseCheckBox.isSelected()) {
                Prefs.set("MICMAQ.firstSliceNucleus", (double) (int) firstSliceSpinner.getValue());
                Prefs.set("MICMAQ.lastSliceNucleus", (double) (int) lastSliceSpinner.getValue());
            }
        }

//        Macro
        Prefs.set("MICMAQ.useMacroNucleus", useAMacroCheckBox.isSelected());
        Prefs.set("MICMAQ.macroNucleus", macroArea.getText());

//        Segmentation
        Prefs.set("MICMAQ.useDeepLearningNucleus", cellPoseRadioButton.isSelected());
        Prefs.set("MICMAQ.nucleiFinalValidation", finalValidationCheckBox.isSelected());
        Prefs.set("MICMAQ.nucleusSaveROI", saveNucleiROIsCheckBox.isSelected());
        Prefs.set("MICMAQ.nucleusSaveMask", saveSegmentationMaskCheckBox.isSelected());

//        --> threshold
        Prefs.set("MICMAQ.thresholdMethodNucleus", threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()));
        Prefs.set("MICMAQ.minSizeNucleus", (double) minSizeNucleusSpinner.getValue());
        Prefs.set("MICMAQ.nucleiUseWaterShed", useWatershedCheckBox.isSelected());
        Prefs.set("MICMAQ.nucleusExcludeOnEdges", excludeOnEdges.isSelected());

//        --> cellpose
        Prefs.set("MICMAQ.cellposeNucleusMethods", cellPoseModelCombo.getItemAt(cellPoseModelCombo.getSelectedIndex()));
        Prefs.set("MICMAQ.cellposeNucleusMinDiameter", (double) (int) cellPoseMinDiameterSpinner.getValue());

//        Show images
        if (!fromDirectory) {
            Prefs.set("MICMAQ.nucleusShowPrepro", showPreprocessingImageCheckBox.isSelected());
            Prefs.set("MICMAQ.nucleusShowMask", showBinaryMaskCheckBox.isSelected());
        }

    }


}
