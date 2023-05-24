package fr.curie.micmaq.gui;

import fr.curie.micmaq.config.FieldOfView;
import fr.curie.micmaq.config.FieldOfViewProvider;
import fr.curie.micmaq.config.ImageProvider;
import fr.curie.micmaq.config.MeasureValue;
import fr.curie.micmaq.helpers.ImageToAnalyze;
import fr.curie.micmaq.segment.SegmentationParameters;
import ij.IJ;
import ij.Prefs;
import ij.measure.Measurements;
import ij.process.AutoThresholder;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.time.Duration;
import java.time.Instant;

public class SegmentationParametersGUI {
    private JButton setMeasurementsButton;
    private JCheckBox isZStackCheckBox;
    private JComboBox projectionMethodCB;
    private JCheckBox chooseSlicesToUseCheckBox;
    private JSpinner sliceMaxSpinner;
    private JSpinner sliceMinSpinner;
    private JCheckBox useMacroCodeCheckBox;
    private JTextArea macroTextArea;
    private JPanel projectionPanel;
    private JPanel macroTextPanel;
    private JRadioButton cellposeRadioButton;
    private JRadioButton thresholdingRadioButton;
    private JPanel thresholdParamsPanel;
    private JPanel segmentationChoicePanel;
    private JComboBox thresholdMethodsCB;
    private JSpinner thresholdMinSizeSpinner;
    private JCheckBox useWatershedCheckBox;
    private JComboBox cellposeModelCB;
    private JSpinner cellposeDiameterSpinner;
    private JTextField cellposeModelPathTextField;
    private JButton browseButton;
    private JCheckBox excludeOnEdgesCheckBox;
    private JCheckBox userValidationCheckBox;
    private JCheckBox saveROIsCheckBox;
    private JCheckBox saveSegmentationMaskCheckBox;
    private JPanel cellposePanel;
    private JPanel cellposeOwnModelPanel;
    private JPanel mainPanel;
    private JSpinner minOverlapSpinner;
    private JSpinner minCytoSizeSpinner;
    private JPanel cytoPanel;
    private JLabel cellposeDiameterLabel;
    private JLabel minSizeLabel;
    private JPanel measurementsPanel;

    private File cellposeModelPath;
    protected FieldOfViewProvider imageProvider;
    public static String NUCLEI = "nuclei";
    public static String CELLS = "cells";
    public static String CELL_CYTO = "cells_cyto";
    protected String type;
    protected MeasureValue measures;


    public SegmentationParametersGUI(FieldOfViewProvider imageProvider, String type) {
        this.imageProvider = imageProvider;
        this.type = type;
        Instant start = Instant.now();
        $$$setupUI$$$();
        measurementsPanel.setVisible(false);
        Instant end = Instant.now();
        IJ.log("segmentation GUI setupUI: " + Duration.between(start, end).toString());
        start = Instant.now();
        getPreferences();
        end = Instant.now();
        IJ.log("segmentationGUI preference: " + Duration.between(start, end).toString());
        start = Instant.now();
        mainPanel.setBorder(BorderFactory.createTitledBorder("Segmentation parameters for " + type));
        cytoPanel.setVisible(false);
        if (type.equals(NUCLEI)) {
            setMeasurementsButton.setText("set measurements for nuclei");
        } else {
            setMeasurementsButton.setText("set measurements for cells");
        }
        if (type.equals(CELLS)) {
            segmentationChoicePanel.setVisible(false);
            cellposeRadioButton.setSelected(true);
            thresholdingRadioButton.setSelected(false);
            minSizeLabel.setText("minimum size of cells (area in pixels)");
            cellposeDiameterLabel.setText("diameter of cells (pixel)");
        }
        if (type.equals(CELL_CYTO)) {
            cytoPanel.setVisible(true);
            segmentationChoicePanel.setVisible(false);
            cellposeRadioButton.setSelected(true);
            thresholdingRadioButton.setSelected(false);
            minSizeLabel.setText("minimum size of cells (area in pixels)");
            cellposeDiameterLabel.setText("diameter of cells (pixel)");
        }
        if (thresholdingRadioButton.isSelected()) {
            cellposePanel.setVisible(false);
            thresholdParamsPanel.setVisible(true);
        } else {
            cellposePanel.setVisible(true);
            thresholdParamsPanel.setVisible(false);
        }
        String modelSelected = (String) cellposeModelCB.getSelectedItem();
        cellposeOwnModelPanel.setVisible(modelSelected.equals("own_model"));
        if (isZStackCheckBox.isSelected()) {
            projectionPanel.setVisible(true);
        } else {
            projectionPanel.setVisible(false);
        }
        measures = new MeasureValue();
        int measurements = (int) Prefs.get("MICMAQ.Measurements_" + type, (Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY));
        measures.setMeasure(measurements); /*Default measurements*/


        isZStackCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                projectionPanel.setVisible(isZStackCheckBox.isSelected());
            }
        });
        chooseSlicesToUseCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sliceMinSpinner.setVisible(chooseSlicesToUseCheckBox.isSelected());
                sliceMaxSpinner.setVisible(chooseSlicesToUseCheckBox.isSelected());
            }
        });
        thresholdingRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                thresholdParamsPanel.setVisible(thresholdingRadioButton.isSelected());
                cellposePanel.setVisible(cellposeRadioButton.isSelected());
            }
        });
        cellposeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                thresholdParamsPanel.setVisible(thresholdingRadioButton.isSelected());
                cellposePanel.setVisible(cellposeRadioButton.isSelected());
            }
        });
        cellposeModelCB.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                String modelSelected = (String) cellposeModelCB.getSelectedItem();
                cellposeOwnModelPanel.setVisible(modelSelected.equals("own_model"));
            }
        });
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cellposeModelPath = CytoCellPanel.chooseCellposeModelFile(mainPanel);
                if (cellposeModelPath != null) {
                    String path = cellposeModelPath.getAbsolutePath();
                    if (path.split("\\\\").length > 2) {
                        String path_shorten = path.substring(path.substring(0, path.lastIndexOf("\\")).lastIndexOf("\\"));
                        cellposeModelPathTextField.setText("..." + path_shorten);
                    } else if (path.split("/").length > 2) {
                        String path_shorten = path.substring(path.substring(0, path.lastIndexOf("/")).lastIndexOf("/"));
                        cellposeModelPathTextField.setText("..." + path_shorten);
                    } else {
                        cellposeModelPathTextField.setText(path);
                    }
                }
            }
        });
        useMacroCodeCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                macroTextPanel.setVisible(useMacroCodeCheckBox.isSelected());
            }
        });
        setMeasurementsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Measures tmp = new Measures("Nuclei", measures);
                tmp.run();
            }
        });
        end = Instant.now();
        IJ.log("segmentationGUI : add listener  " + Duration.between(start, end).toString());
    }

    public void setType(String type) {
        this.type = type;
        cytoPanel.setVisible(false);
        if (type.equals(CELLS)) {
            segmentationChoicePanel.setVisible(false);
            cellposeRadioButton.setSelected(true);
            thresholdingRadioButton.setSelected(false);
            getPreferences();
        }
        if (type.equals(CELL_CYTO)) {
            cytoPanel.setVisible(true);
            segmentationChoicePanel.setVisible(false);
            cellposeRadioButton.setSelected(true);
            thresholdingRadioButton.setSelected(false);
            getPreferences();
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public String getParametersAsString() {
        String result = "\nSEGMENTATION " + type;
        result += "\nPreprocessing:";
        if (isZStackCheckBox.isSelected()) {
            result += "\n\tProjection: " + (String) projectionMethodCB.getSelectedItem();
            if (chooseSlicesToUseCheckBox.isSelected()) {
                result += "\n\tSlices " + (int) sliceMinSpinner.getValue() + "-" + sliceMaxSpinner.getValue();
            }
        }
        if (useMacroCodeCheckBox.isSelected()) {
            result += "\n\tMacro:\n" + macroTextArea.getText();
        }
        if (thresholdingRadioButton.isSelected()) {
            result += "\nUse thresholding:";
            result += "\n\tAutomatic threshold method: " + thresholdMethodsCB.getSelectedItem();
            result += "\n\tMinimum " + type + " diameter: " + thresholdMinSizeSpinner.getValue();
            result += "\n\tWatershed: " + (useWatershedCheckBox.isSelected() ? "yes" : "no");
        } else {
            result += "\nUse Cellpose:";
            result += "\n\tCellpose model: " + cellposeModelCB.getSelectedItem();
            if (cellposeModelPath != null) result += "\n\tCellpose model path: " + cellposeModelPath;
            result += "\n\tMinimum diameter: " + cellposeDiameterSpinner.getValue();
        }
        result += "\n\tExclude on edges: " + (excludeOnEdgesCheckBox.isSelected() ? "yes" : "no");
        result += "\n\tFinal user validation: " + (userValidationCheckBox.isSelected() ? "yes" : "no");

        return result;
    }

    public SegmentationParameters getParameters() {
        SegmentationParameters params;
        if (thresholdingRadioButton.isSelected()) {
            params = SegmentationParameters.createThresholding((String) thresholdMethodsCB.getSelectedItem(), useWatershedCheckBox.isSelected());
        } else {
            params = SegmentationParameters.createCellpose((String) cellposeModelCB.getSelectedItem(), (Integer) cellposeDiameterSpinner.getValue());
            if (cellposeModelPath != null) {
                params.setPathToModel(cellposeModelPath);
                params.setCellposeModel(cellposeModelPath.getAbsolutePath());
            }
            IJ.log("set cellpose model to " + params.getCellposeModel());
        }
        if (isZStackCheckBox.isSelected()) {
            params.setProjection(projectionMethodCB.getSelectedIndex());
            if (chooseSlicesToUseCheckBox.isSelected()) {
                params.setProjectionSliceMin((Integer) sliceMinSpinner.getValue());
                params.setProjectionSliceMax((Integer) sliceMaxSpinner.getValue());
            }
        }
        if (useMacroCodeCheckBox.isSelected()) params.setPreprocessMacro(macroTextArea.getText());
        params.setMinSize((Double) thresholdMinSizeSpinner.getValue());
        params.setExcludeOnEdge(excludeOnEdgesCheckBox.isSelected());
        params.setUserValidation(userValidationCheckBox.isSelected());
        params.setSaveROIs(saveROIsCheckBox.isSelected());
        params.setSaveMasks(saveSegmentationMaskCheckBox.isSelected());
        params.setMeasurements(measures);
        setPreferences();
        return params;
    }

    /**
     * Preset panel choices according to prefs
     */
    private void getPreferences() {
//        Projection
        isZStackCheckBox.setSelected(Prefs.get("MICMAQ.zStack" + type, true));
        projectionMethodCB.setSelectedItem(Prefs.get("MICMAQ.ProjMethods" + type, "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("MICMAQ.chooseSlices" + type, false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            //slicesPanel.setVisible(false);
            sliceMinSpinner.setVisible(false);
            sliceMaxSpinner.setVisible(false);
        }
        int channel = (type.equals(NUCLEI)) ? imageProvider.getNucleiChannel() : imageProvider.getCellChannel();


        //int maxSlices = imageProvider.getImagePlus(0, imageProvider.getCellChannel()).getNSlices();
        int maxSlices = imageProvider.getFieldOfView(0).getNSlices(channel);
        int firstSlice = (int) Prefs.get("MICMAQ.firstSlice" + type, 1);
        int lastSlice = (int) Prefs.get("MICMAQ.lastSlice" + type, maxSlices);
        ImageToAnalyze.assertSlices(maxSlices, firstSlice, lastSlice, sliceMinSpinner, sliceMaxSpinner);

//        Macro
        useMacroCodeCheckBox.setSelected(Prefs.get("MICMAQ.useMacro" + type, false));
        if (!useMacroCodeCheckBox.isSelected()) macroTextPanel.setVisible(false);
        macroTextArea.append(Prefs.get("MICMAQ.macro" + type, " "));

//        Segmentation
        cellposeRadioButton.setSelected(Prefs.get("MICMAQ.useDeepLearning" + type, false));
        if (cellposeRadioButton.isSelected()) thresholdParamsPanel.setVisible(false);
        else cellposePanel.setVisible(false);
        excludeOnEdgesCheckBox.setSelected(Prefs.get("MICMAQ.ExcludeOnEdges" + type, true));
        userValidationCheckBox.setSelected(Prefs.get("MICMAQ.userValidation" + type, false));
        saveSegmentationMaskCheckBox.setSelected(Prefs.get("MICMAQ.SaveMask" + type, true));
        saveROIsCheckBox.setSelected(Prefs.get("MICMAQ.SaveROI" + type, true));
//        --> threshold
        thresholdMethodsCB.setSelectedItem(Prefs.get("MICMAQ.thresholdMethod" + type, "Li"));
        thresholdMinSizeSpinner.setValue(Prefs.get("MICMAQ.minSize" + type, 1000));
        useWatershedCheckBox.setSelected(Prefs.get("MICMAQ.UseWaterShed" + type, false));
//        --> cellpose
        cellposeModelCB.setSelectedItem(Prefs.get("MICMAQ.cellposeMethods" + type, "cyto2"));
        if (cellposeModelCB.getSelectedItem() != "own_model") {
            cellposeOwnModelPanel.setVisible(false);
        }
        cellposeDiameterSpinner.setValue((int) Prefs.get("MICMAQ.cellposeDiameter" + type, 100));

    }

    /**
     * set prefs in prefs file
     */
    public void setPreferences() {
//        Projection
        Prefs.set("MICMAQ.zStack" + type, isZStackCheckBox.isSelected());
        if (isZStackCheckBox.isSelected()) {
            Prefs.set("MICMAQ.ProjMethods" + type, (String) projectionMethodCB.getSelectedItem());
            Prefs.set("MICMAQ.chooseSlices" + type, chooseSlicesToUseCheckBox.isSelected());
            if (chooseSlicesToUseCheckBox.isSelected()) {
                Prefs.set("MICMAQ.firstSlice" + type, (double) (int) sliceMinSpinner.getValue());
                Prefs.set("MICMAQ.lastSlice" + type, (double) (int) sliceMaxSpinner.getValue());
            }
        }

//        Macro
        Prefs.set("MICMAQ.useMacro" + type, useMacroCodeCheckBox.isSelected());
        Prefs.set("MICMAQ.macro" + type, macroTextArea.getText());

//        Segmentation
        Prefs.set("MICMAQ.useDeepLearning" + type, cellposeRadioButton.isSelected());
        Prefs.set("MICMAQ.userValidation" + type, userValidationCheckBox.isSelected());
        Prefs.set("MICMAQ.SaveROI" + type, saveROIsCheckBox.isSelected());
        Prefs.set("MICMAQ.SaveMask" + type, saveSegmentationMaskCheckBox.isSelected());

//        --> threshold
        Prefs.set("MICMAQ.thresholdMethod" + type, (String) thresholdMethodsCB.getSelectedItem());
        Prefs.set("MICMAQ.minSize" + type, (double) thresholdMinSizeSpinner.getValue());
        Prefs.set("MICMAQ.UseWaterShed" + type, useWatershedCheckBox.isSelected());
        Prefs.set("MICMAQ.ExcludeOnEdges" + type, excludeOnEdgesCheckBox.isSelected());

//        --> cellpose
        Prefs.set("MICMAQ.cellposeMethods" + type, (String) cellposeModelCB.getSelectedItem());
        Prefs.set("MICMAQ.cellposeDiameter" + type, (int) cellposeDiameterSpinner.getValue());


    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
// List of methods for threshold
        thresholdMethodsCB = new JComboBox<>(AutoThresholder.getMethods());

        // minSize spinner
        thresholdMinSizeSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 0.0, Integer.MAX_VALUE, 10.0));
        cellposeDiameterSpinner = new JSpinner(new SpinnerNumberModel(200, 0, Integer.MAX_VALUE, 10));

//  Text field to filter extension
        sliceMinSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        sliceMaxSpinner = new JSpinner(new SpinnerNumberModel(33, 0, 9999, 1));

        //        min overlap of nuclei and cell
        minOverlapSpinner = new JSpinner(new SpinnerNumberModel(50, 0.0, 100.0, 10));
//        minimal size of cytoplasm compared to cell
        minCytoSizeSpinner = new JSpinner(new SpinnerNumberModel(25, 0.0, 100.0, 10));

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
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Segmentation parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        useMacroCodeCheckBox = new JCheckBox();
        useMacroCodeCheckBox.setText("use macro code");
        panel2.add(useMacroCodeCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        panel2.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        panel1.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroTextPanel = new JPanel();
        macroTextPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(macroTextPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        macroTextPanel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroTextArea = new JTextArea();
        macroTextArea.setRows(2);
        scrollPane1.setViewportView(macroTextArea);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, new Dimension(-1, 61), null, null, 0, false));
        isZStackCheckBox = new JCheckBox();
        isZStackCheckBox.setText("is Z-stack?");
        panel3.add(isZStackCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTHWEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projectionPanel = new JPanel();
        projectionPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(projectionPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        projectionPanel.add(panel4, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("method of projection");
        panel4.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("choose slices to use");
        panel4.add(chooseSlicesToUseCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 28), null, null, 0, false));
        panel4.add(sliceMaxSpinner, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel4.add(sliceMinSpinner, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        panel4.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        projectionMethodCB = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Maximum projection");
        defaultComboBoxModel1.addElement("Standard deviation projection");
        projectionMethodCB.setModel(defaultComboBoxModel1);
        panel4.add(projectionMethodCB, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer4 = new com.intellij.uiDesigner.core.Spacer();
        projectionPanel.add(spacer4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        segmentationChoicePanel = new JPanel();
        segmentationChoicePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(segmentationChoicePanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        segmentationChoicePanel.setBorder(BorderFactory.createTitledBorder(null, "Segmentation method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        cellposeRadioButton = new JRadioButton();
        cellposeRadioButton.setText("Cellpose");
        segmentationChoicePanel.add(cellposeRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdingRadioButton = new JRadioButton();
        thresholdingRadioButton.setSelected(true);
        thresholdingRadioButton.setText("Thresholding");
        segmentationChoicePanel.add(thresholdingRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Which method of segmentation to use?");
        segmentationChoicePanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer5 = new com.intellij.uiDesigner.core.Spacer();
        segmentationChoicePanel.add(spacer5, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        thresholdParamsPanel = new JPanel();
        thresholdParamsPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(thresholdParamsPanel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        thresholdParamsPanel.setBorder(BorderFactory.createTitledBorder(null, "Thresholding parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("thresholding method");
        thresholdParamsPanel.add(label3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdParamsPanel.add(thresholdMethodsCB, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minSizeLabel = new JLabel();
        minSizeLabel.setText("minimum size of nuclei (area in pixels)");
        thresholdParamsPanel.add(minSizeLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdParamsPanel.add(thresholdMinSizeSpinner, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useWatershedCheckBox = new JCheckBox();
        useWatershedCheckBox.setText("use watershed");
        thresholdParamsPanel.add(useWatershedCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellposePanel = new JPanel();
        cellposePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(cellposePanel, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        cellposePanel.setBorder(BorderFactory.createTitledBorder(null, "Cellpose parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label4 = new JLabel();
        label4.setText("model for Cellpose");
        cellposePanel.add(label4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellposeModelCB = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("nuclei");
        defaultComboBoxModel2.addElement("cyto");
        defaultComboBoxModel2.addElement("cyto2");
        defaultComboBoxModel2.addElement("bact_omni");
        defaultComboBoxModel2.addElement("tissuenet");
        defaultComboBoxModel2.addElement("livecell");
        defaultComboBoxModel2.addElement("own_model");
        cellposeModelCB.setModel(defaultComboBoxModel2);
        cellposePanel.add(cellposeModelCB, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellposeDiameterLabel = new JLabel();
        cellposeDiameterLabel.setText("diameter");
        cellposePanel.add(cellposeDiameterLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellposePanel.add(cellposeDiameterSpinner, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellposeOwnModelPanel = new JPanel();
        cellposeOwnModelPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        cellposePanel.add(cellposeOwnModelPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("path to own model");
        cellposeOwnModelPanel.add(label5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellposeModelPathTextField = new JTextField();
        cellposeOwnModelPanel.add(cellposeModelPathTextField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        browseButton = new JButton();
        browseButton.setText("Browse");
        cellposeOwnModelPanel.add(browseButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel5, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        excludeOnEdgesCheckBox = new JCheckBox();
        excludeOnEdgesCheckBox.setText("exclude on edges");
        panel5.add(excludeOnEdgesCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        userValidationCheckBox = new JCheckBox();
        userValidationCheckBox.setText("user validation");
        panel5.add(userValidationCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveROIsCheckBox = new JCheckBox();
        saveROIsCheckBox.setText("save ROIs");
        panel5.add(saveROIsCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveSegmentationMaskCheckBox = new JCheckBox();
        saveSegmentationMaskCheckBox.setText("save segmentation mask");
        panel5.add(saveSegmentationMaskCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer6 = new com.intellij.uiDesigner.core.Spacer();
        panel5.add(spacer6, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        cytoPanel = new JPanel();
        cytoPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(cytoPanel, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cytoPanel.setBorder(BorderFactory.createTitledBorder(null, "Cytoplasm extraction parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label6 = new JLabel();
        label6.setText("minimal overlap of nucleus with cell (% of nucleus)");
        cytoPanel.add(label6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cytoPanel.add(minOverlapSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Minimal size of cytoplasm (% of cell)");
        cytoPanel.add(label7, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cytoPanel.add(minCytoSizeSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        measurementsPanel = new JPanel();
        measurementsPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(measurementsPanel, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        measurementsPanel.setBorder(BorderFactory.createTitledBorder(null, "Measurements", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        setMeasurementsButton = new JButton();
        setMeasurementsButton.setText("set measurements");
        measurementsPanel.add(setMeasurementsButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(cellposeRadioButton);
        buttonGroup.add(thresholdingRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
