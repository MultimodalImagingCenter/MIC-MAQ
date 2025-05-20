package fr.curie.micmaq.gui;

import fr.curie.micmaq.config.FieldOfViewProvider;
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
import java.util.ArrayList;

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
    private JSpinner cellposeCellproba_thresholdSpinner;
    private JRadioButton starDistRadioButton;
    private JPanel stardistPanel;
    private JComboBox stardistModelComboBox;
    private JTextField stardistModelPathTextField;
    private JButton stardistBrowseButton;
    private JSpinner stardistPercentileLowSpinner;
    private JSpinner stardistPercentileHighSpinner;
    private JSpinner stardistProbaThrSpinner;
    private JSpinner stardistNMSthrSpinner;
    private JPanel stardistLoadModelPanel;
    private JSpinner stardistScaleSpinner;
    private JCheckBox estimateCellsByNucleiCheckBox;
    private JSpinner spinnerExpansionRadius;
    private JPanel expansionPanel;
    private JRadioButton otherRadioButton;
    private JPanel otherPanel;
    private JTextArea textAreaMacroSegment;
    private JCheckBox resultIsInRoiManagerCheckBox;
    private JCheckBox resultIsAnInstanceCheckBox;

    private File cellposeModelPath;
    private File stardistModelPath;
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
        //IJ.log("segmentation GUI setupUI: " + Duration.between(start, end).toString());
        start = Instant.now();
        getPreferences();
        end = Instant.now();
        //IJ.log("segmentationGUI preference: " + Duration.between(start, end).toString());
        start = Instant.now();
        mainPanel.setBorder(BorderFactory.createTitledBorder("Segmentation parameters for " + type));
        cytoPanel.setVisible(false);

        starDistRadioButton.setVisible(true);
        otherRadioButton.setSelected(false);

        if (type.equals(NUCLEI)) {
            setMeasurementsButton.setText("set measurements for nuclei");
        } else {
            setMeasurementsButton.setText("set measurements for cells");
        }
        if (type.equals(CELLS)) {//segmentationChoicePanel.setVisible(false);
            thresholdingRadioButton.setSelected(false);
            thresholdingRadioButton.setVisible(false);
            starDistRadioButton.setSelected(false);
            starDistRadioButton.setVisible(false);
            cellposeRadioButton.setSelected(true);
            minSizeLabel.setText("minimum size of cells (area in pixels)");
            cellposeDiameterLabel.setText("diameter of cells (pixel)");
            setNucleiOnly(false);
        }
        if (type.equals(CELL_CYTO)) {
            cytoPanel.setVisible(true);
            //segmentationChoicePanel.setVisible(false);
            thresholdingRadioButton.setSelected(false);
            thresholdingRadioButton.setVisible(false);
            starDistRadioButton.setSelected(false);
            starDistRadioButton.setVisible(false);
            cellposeRadioButton.setSelected(true);
            minSizeLabel.setText("minimum size of cells (area in pixels)");
            cellposeDiameterLabel.setText("diameter of cells (pixel)");
            setNucleiOnly(false);
        }
        //IJ.log("segmentationGUI constr" + thresholdingRadioButton.isSelected() + ", " + cellposeRadioButton.isSelected());
        if (thresholdingRadioButton.isSelected()) {
            stardistPanel.setVisible(false);
            cellposePanel.setVisible(false);
            thresholdParamsPanel.setVisible(true);
            otherPanel.setVisible(false);
        } else if (cellposeRadioButton.isSelected()) {
            stardistPanel.setVisible(false);
            cellposePanel.setVisible(true);
            thresholdParamsPanel.setVisible(false);
            otherPanel.setVisible(false);
        } else if (starDistRadioButton.isSelected()) {
            stardistPanel.setVisible(true);
            cellposePanel.setVisible(false);
            thresholdParamsPanel.setVisible(false);
            otherPanel.setVisible(false);
        } else if (otherRadioButton.isSelected()) {
            stardistPanel.setVisible(false);
            cellposePanel.setVisible(false);
            thresholdParamsPanel.setVisible(false);
            otherPanel.setVisible(true);

        }
        String modelSelected = (String) cellposeModelCB.getSelectedItem();
        cellposeOwnModelPanel.setVisible(modelSelected.equals("own_model"));
        String stardistModelSelected = (String) stardistModelComboBox.getSelectedItem();
        stardistLoadModelPanel.setVisible(stardistModelSelected.equals("Model (.zip) from File"));

        if (isZStackCheckBox.isSelected()) {
            projectionPanel.setVisible(true);
        } else {
            projectionPanel.setVisible(false);
        }
        measures = new MeasureValue(true);
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
                stardistPanel.setVisible(starDistRadioButton.isSelected());
                otherPanel.setVisible(otherRadioButton.isSelected());
            }
        });
        cellposeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                thresholdParamsPanel.setVisible(thresholdingRadioButton.isSelected());
                cellposePanel.setVisible(cellposeRadioButton.isSelected());
                stardistPanel.setVisible(starDistRadioButton.isSelected());
                otherPanel.setVisible(otherRadioButton.isSelected());
            }
        });
        starDistRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                thresholdParamsPanel.setVisible(thresholdingRadioButton.isSelected());
                cellposePanel.setVisible(cellposeRadioButton.isSelected());
                stardistPanel.setVisible(starDistRadioButton.isSelected());
                otherPanel.setVisible(otherRadioButton.isSelected());
            }
        });

        otherRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                thresholdParamsPanel.setVisible(thresholdingRadioButton.isSelected());
                cellposePanel.setVisible(cellposeRadioButton.isSelected());
                stardistPanel.setVisible(starDistRadioButton.isSelected());
                otherPanel.setVisible(otherRadioButton.isSelected());
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
        stardistModelComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                String modelSelected = (String) stardistModelComboBox.getSelectedItem();
                stardistLoadModelPanel.setVisible(modelSelected.equals("Model (.zip) from File"));
            }
        });
        stardistBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stardistModelPath = CytoCellPanel.chooseCellposeModelFile(mainPanel);
                if (stardistModelPath != null) {
                    String path = stardistModelPath.getAbsolutePath();
                    if (path.split("\\\\").length > 2) {
                        String path_shorten = path.substring(path.substring(0, path.lastIndexOf("\\")).lastIndexOf("\\"));
                        stardistModelPathTextField.setText("..." + path_shorten);
                    } else if (path.split("/").length > 2) {
                        String path_shorten = path.substring(path.substring(0, path.lastIndexOf("/")).lastIndexOf("/"));
                        stardistModelPathTextField.setText("..." + path_shorten);
                    } else {
                        stardistModelPathTextField.setText(path);
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

        estimateCellsByNucleiCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                spinnerExpansionRadius.setEnabled(estimateCellsByNucleiCheckBox.isSelected());
            }
        });
    }

    public void setType(String type) {
        this.type = type;
        cytoPanel.setVisible(false);
        if (type.equals(CELLS)) {
            //segmentationChoicePanel.setVisible(false);
            thresholdingRadioButton.setSelected(false);
            thresholdingRadioButton.setVisible(false);
            starDistRadioButton.setSelected(false);
            starDistRadioButton.setVisible(false);
            cellposeRadioButton.setSelected(true);
            expansionPanel.setVisible(false);
            getPreferences();
        }
        if (type.equals(CELL_CYTO)) {
            cytoPanel.setVisible(true);
            //segmentationChoicePanel.setVisible(false);
            thresholdingRadioButton.setSelected(false);
            thresholdingRadioButton.setVisible(false);
            starDistRadioButton.setSelected(false);
            starDistRadioButton.setVisible(false);
            cellposeRadioButton.setSelected(true);
            expansionPanel.setVisible(false);
            getPreferences();
        }
        IJ.log("set Type " + type + ", " + thresholdingRadioButton.isSelected() + ", " + cellposeRadioButton.isSelected());
        thresholdParamsPanel.setVisible(thresholdingRadioButton.isSelected());
        cellposePanel.setVisible(cellposeRadioButton.isSelected());
        stardistPanel.setVisible(starDistRadioButton.isSelected());
    }

    public void setNucleiOnly(boolean onlynuclei) {
        expansionPanel.setVisible(onlynuclei);
        if (!onlynuclei) estimateCellsByNucleiCheckBox.setSelected(false);
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
        } else if (cellposeRadioButton.isSelected()) {
            result += "\nUse Cellpose:";
            result += "\n\tCellpose model: " + cellposeModelCB.getSelectedItem();
            if (cellposeModelCB.getSelectedIndex() == cellposeModelCB.getItemCount() - 1 && cellposeModelPath != null)
                result += "\n\tCellpose model path: " + cellposeModelPath;
            result += "\n\tMinimum diameter: " + (int) cellposeDiameterSpinner.getValue();
            result += "\n\tcellproba_threshold: " + cellposeCellproba_thresholdSpinner.getValue();
        } else if (starDistRadioButton.isSelected()) {
            result += "\nUse StarDist:";
            result += "\n\tStarDist model: " + stardistModelComboBox.getSelectedItem();
            if (stardistModelPath != null) result += "\n\tStarDist model path: " + stardistModelPath;
            result += "\n\tNormalization percentile low: " + stardistPercentileLowSpinner.getValue();
            result += "\n\tNormalization percentile high: " + stardistPercentileHighSpinner.getValue();
            result += "\n\tProbability threshold: " + stardistProbaThrSpinner.getValue();
            result += "\n\tOverlap threshold: " + stardistNMSthrSpinner.getValue();
            result += "\n\tscale image: " + stardistScaleSpinner.getValue();
        }
        result += "\nExclude on edges: " + (excludeOnEdgesCheckBox.isSelected() ? "yes" : "no");
        result += "\nFinal user validation: " + (userValidationCheckBox.isSelected() ? "yes" : "no");
        result += "\nsave Roi: " + (saveROIsCheckBox.isSelected() ? "yes" : "no");
        result += "\nsave mask: " + (saveSegmentationMaskCheckBox.isSelected() ? "yes" : "no");
        if (type.equals(CELL_CYTO)) {
            result += "\n\tmin size overlap nucleus/cell: " + minOverlapSpinner.getValue();
            result += "\n\tmin size of cytoplasm: " + minCytoSizeSpinner.getValue();
        }
        if (type.equals(NUCLEI)) {
            result += "\nexpand nuclei: " + ((estimateCellsByNucleiCheckBox.isSelected()) ? "yes" : "no");
            if (estimateCellsByNucleiCheckBox.isSelected())
                result += "\n\texpand radius: " + spinnerExpansionRadius.getValue();
        }

        return result;
    }

    public void setParameters(ArrayList<String> params) {
        boolean q = true;
        boolean m = false;
        String macro = "";
        useMacroCodeCheckBox.setSelected(false);
        isZStackCheckBox.setSelected(false);
        excludeOnEdgesCheckBox.setSelected(false);
        userValidationCheckBox.setSelected(false);
        saveROIsCheckBox.setSelected(true);
        saveSegmentationMaskCheckBox.setSelected(false);

        for (int i = 0; i < params.size(); i++) {
            //System.out.println("#segm: "+params.get(i));
            if (q) {
                if (params.get(i).startsWith("Measurements") || params.get(i).startsWith("Use ")) {
                    m = false;
                    //System.out.println("stop macro");
                }
                if (m) {
                    macro += params.get(i) + "\n";
                    //System.out.println("add text to macro " + params.get(i));
                    //System.out.println("macro becomes :\n" + macro);
                    macroTextArea.setText(macro);
                } else {

                    if (params.get(i).startsWith("Macro:")) {
                        m = true;
                        //System.out.println("start macro and activate in GUI");
                        useMacroCodeCheckBox.setSelected(true);
                        macroTextPanel.setVisible(true);
                    }

                    if (params.get(i).startsWith("Projection")) {
                        isZStackCheckBox.setSelected(true);
                        System.out.println("change projection");
                    }

                    if (params.get(i).startsWith("Use")) {
                        System.out.println("use segmentation :");
                        if (params.get(i).endsWith("Cellpose:")) {
                            System.out.println("Cellpose detected !");
                            cellposeRadioButton.setSelected(true);
                            int offset = 1;
                            String model = params.get(i + offset).split(": ")[1];
                            System.out.println("model to put = " + model);
                            int index = -1;
                            for (int ind = 0; ind < cellposeModelCB.getItemCount(); ind++) {
                                if (cellposeModelCB.getItemAt(ind).equals(model)) index = ind;
                            }
                            if (index >= 0) cellposeModelCB.setSelectedIndex(index);
                            offset++;
                            if (params.get(i + offset).startsWith("Cellpose model path: ")) {
                                String tmp = params.get(i + offset).split(": ")[1];
                                cellposeModelPath = new File(tmp);
                                cellposeModelPathTextField.setText(tmp);
                                offset++;
                            }
                            cellposeDiameterSpinner.setValue(new Integer(params.get(i + offset).split(": ")[1]));
                            //System.out.println("diameter: "+params.get(i+offset).split(": ")[1]);
                            offset++;
                            cellposeCellproba_thresholdSpinner.setValue(new Double(params.get(i + offset).split(": ")[1]));
                            //System.out.println("cellproba: "+params.get(i+offset).split(": ")[1]);

                        } else if (params.get(i).endsWith("StarDist:")) {
                            starDistRadioButton.setSelected(true);
                            int offset = 1;
                            String model = params.get(i + offset).split(": ")[1];
                            int index = -1;
                            for (int ind = 0; ind < stardistModelComboBox.getItemCount(); ind++) {
                                if (stardistModelComboBox.getItemAt(ind).equals(model)) index = ind;
                            }
                            if (index >= 0) stardistModelComboBox.setSelectedIndex(index);
                            offset++;
                            if (params.get(i + offset).startsWith("StarDist model path: ")) {
                                String tmp = params.get(i + offset).split(": ")[1];
                                stardistModelPath = new File(tmp);
                                stardistModelPathTextField.setText(tmp);
                                offset++;
                            }
                            stardistPercentileLowSpinner.setValue(new Double(params.get(i + offset).split(": ")[1]));
                            offset++;
                            stardistPercentileHighSpinner.setValue(new Double(params.get(i + offset).split(": ")[1]));
                            offset++;
                            stardistProbaThrSpinner.setValue(new Double(params.get(i + offset).split(": ")[1]));
                            offset++;
                            stardistNMSthrSpinner.setValue(new Double(params.get(i + offset).split(": ")[1]));
                            offset++;
                            stardistScaleSpinner.setValue(new Double(params.get(i + offset).split(": ")[1]));

                        } else {
                            thresholdingRadioButton.setSelected(true);
                            int offset = 1;
                            String model = params.get(i + offset).split(": ")[1];
                            int index = -1;
                            for (int ind = 0; ind < thresholdMethodsCB.getItemCount(); ind++) {
                                if (thresholdMethodsCB.getItemAt(ind).equals(model)) index = ind;
                            }
                            if (index >= 0) thresholdMethodsCB.setSelectedIndex(index);
                            offset++;
                            thresholdMinSizeSpinner.setValue(new Double(params.get(i + offset).split(": ")[1]));
                            offset++;
                            useWatershedCheckBox.setSelected(params.get(i + offset).endsWith("yes"));
                        }

                    }

                    if (params.get(i).startsWith("Exclude on edges:")) {
                        excludeOnEdgesCheckBox.setSelected(params.get(i).endsWith("yes"));
                    }
                    if (params.get(i).startsWith("Final user validation:")) {
                        userValidationCheckBox.setSelected(params.get(i).endsWith("yes"));
                    }
                    if (params.get(i).startsWith("save Roi:")) {
                        saveROIsCheckBox.setSelected(params.get(i).endsWith("yes"));
                    }
                    if (params.get(i).startsWith("save mask:")) {
                        saveSegmentationMaskCheckBox.setSelected(params.get(i).endsWith("yes"));
                    }

                    if (params.get(i).startsWith("min size overlap nucleus/cell:")) {
                        minOverlapSpinner.setValue(new Double(params.get(i).split(": ")[1]));
                    }
                    if (params.get(i).startsWith("min size of cytoplasm:")) {
                        minCytoSizeSpinner.setValue(new Double(params.get(i).split(": ")[1]));
                    }
                    if (params.get(i).startsWith("expand nuclei:")) {
                        estimateCellsByNucleiCheckBox.setSelected(params.get(i).endsWith("yes"));
                    }
                    if (params.get(i).startsWith("expand radius:")) {
                        spinnerExpansionRadius.setValue(new Integer(params.get(i).split(": ")[1]));
                    }
                }

            }
            if (params.get(i).startsWith("Quantification  Parameters")) q = false;

            spinnerExpansionRadius.setEnabled(estimateCellsByNucleiCheckBox.isSelected());
            mainPanel.repaint();
        }//end for
    }

    public SegmentationParameters getParameters() {
        SegmentationParameters params;
        if (thresholdingRadioButton.isSelected()) {
            params = SegmentationParameters.createThresholding((String) thresholdMethodsCB.getSelectedItem(), useWatershedCheckBox.isSelected());
        } else if (cellposeRadioButton.isSelected()) {
            params = SegmentationParameters.createCellpose((String) cellposeModelCB.getSelectedItem(),
                    (Integer) cellposeDiameterSpinner.getValue(),
                    (double) cellposeCellproba_thresholdSpinner.getValue());
            if (cellposeModelCB.getSelectedItem() == "own_model") {
                params.setPathToModel(cellposeModelPath);
                params.setCellposeModel(cellposeModelPath.getAbsolutePath());
            }
            IJ.log("set cellpose model to " + params.getCellposeModel());
        } else if (starDistRadioButton.isSelected()) {//StarDist
            boolean loadModel = (stardistModelComboBox.getSelectedItem() != "Model (.zip) from File");
            params = SegmentationParameters.createStarDist((String) stardistModelComboBox.getSelectedItem(),
                    (double) stardistPercentileLowSpinner.getValue(), (double) stardistPercentileHighSpinner.getValue(),
                    (double) stardistProbaThrSpinner.getValue(), (double) stardistNMSthrSpinner.getValue(),
                    (loadModel) ? stardistModelPath.getAbsolutePath() : null,
                    (double) stardistScaleSpinner.getValue());
            IJ.log("set stardist model to " + params.getStardistModel());
        } else {//other via macro
            IJ.log("segmentation via macro");
            params = SegmentationParameters.createMacroSegmentation(textAreaMacroSegment.getText(), resultIsInRoiManagerCheckBox.isSelected(), resultIsAnInstanceCheckBox.isSelected());
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

        if (this.type.equals(CELL_CYTO)) {
            params.setCytoplasmParameters((double) minOverlapSpinner.getValue(), (double) minCytoSizeSpinner.getValue());
        }

        if (this.type.equals(NUCLEI)) {
            if (estimateCellsByNucleiCheckBox.isSelected())
                params.setExpansionRadius((int) spinnerExpansionRadius.getValue());
        }
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
        int maxSlices = imageProvider.getFirstValidFieldOfView().getNSlices(channel);
        int firstSlice = (int) Prefs.get("MICMAQ.firstSlice" + type, 1);
        int lastSlice = (int) Prefs.get("MICMAQ.lastSlice" + type, maxSlices);
        ImageToAnalyze.assertSlices(maxSlices, firstSlice, lastSlice, sliceMinSpinner, sliceMaxSpinner);

//        Macro
        useMacroCodeCheckBox.setSelected(Prefs.get("MICMAQ.useMacro" + type, false));
        if (!useMacroCodeCheckBox.isSelected()) macroTextPanel.setVisible(false);
        macroTextArea.append(Prefs.get("MICMAQ.macro" + type, " "));

//        Segmentation
        int approach = (int) Prefs.get("MICMAQ.approach" + type, 2);
        if (approach == 1 && (type.equals(CELLS) || type.equals(CELL_CYTO))) approach = 2;
        switch (approach) {
            case 1:
            default:
                thresholdingRadioButton.setSelected(true);
                thresholdParamsPanel.setVisible(true);
                cellposePanel.setVisible(false);
                stardistPanel.setVisible(false);
                otherPanel.setVisible(false);
                break;
            case 2:
                cellposeRadioButton.setSelected(true);
                thresholdParamsPanel.setVisible(false);
                cellposePanel.setVisible(true);
                stardistPanel.setVisible(false);
                otherPanel.setVisible(false);
                break;
            case 3:
                starDistRadioButton.setSelected(true);
                thresholdParamsPanel.setVisible(false);
                cellposePanel.setVisible(false);
                stardistPanel.setVisible(true);
                otherPanel.setVisible(false);
                break;
            case 4:
                otherRadioButton.setSelected(true);
                thresholdParamsPanel.setVisible(false);
                cellposePanel.setVisible(false);
                stardistPanel.setVisible(false);
                otherPanel.setVisible(true);
                break;
        }
        //cellposeRadioButton.setSelected(Prefs.get("MICMAQ.useDeepLearning" + type, false));
        //if (cellposeRadioButton.isSelected()) thresholdParamsPanel.setVisible(false);
        //else cellposePanel.setVisible(false);
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
        cellposeCellproba_thresholdSpinner.setValue((double) Prefs.get("MICMAQ.cellposeCellproba_threshold" + type, 0.0));
        cellposeModelPath = new File(Prefs.get("MICMAQ.cellposeModelPath" + type, ""));
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
//        --> stardist
        stardistModelComboBox.setSelectedItem(Prefs.get("MICMAQ.stardistModel" + type, "Versatile (fluorescent nuclei)"));
        if (stardistModelComboBox.getSelectedItem() != "Model (.zip) from File") {
            stardistLoadModelPanel.setVisible(false);
        }
        stardistPercentileLowSpinner.setValue(Prefs.get("MICMAQ.stardistPercentLow" + type, 0.0));
        stardistPercentileHighSpinner.setValue(Prefs.get("MICMAQ.stardistPercentHigh" + type, 100.0));
        stardistProbaThrSpinner.setValue(Prefs.get("MICMAQ.stardistProbaThr" + type, 0.5));
        stardistNMSthrSpinner.setValue(Prefs.get("MICMAQ.stardistNMSThr" + type, 0.2));
        stardistScaleSpinner.setValue(Prefs.get("MICMAQ.stardistScale" + type, 1.0));
        stardistModelPath = new File(Prefs.get("MICMAQ.stardistModelPath" + type, ""));
        if (stardistModelPath != null) {
            String path = stardistModelPath.getAbsolutePath();
            if (path.split("\\\\").length > 2) {
                String path_shorten = path.substring(path.substring(0, path.lastIndexOf("\\")).lastIndexOf("\\"));
                stardistModelPathTextField.setText("..." + path_shorten);
            } else if (path.split("/").length > 2) {
                String path_shorten = path.substring(path.substring(0, path.lastIndexOf("/")).lastIndexOf("/"));
                stardistModelPathTextField.setText("..." + path_shorten);
            } else {
                stardistModelPathTextField.setText(path);
            }
        }
//        --> macro segmentation
        textAreaMacroSegment.setText(Prefs.get("MICMAQ.macroSegmentationCode" + type, ""));
        resultIsInRoiManagerCheckBox.setSelected(Prefs.get("MICMAQ.macroSegmentationRoi" + type, false));
        resultIsAnInstanceCheckBox.setSelected(Prefs.get("MICMAQ.macroSegmentationImage" + type, false));

//      --> cytoplasm
        minOverlapSpinner.setValue(Prefs.get("MICMAC.cytoplasmoverlap", 50.0));
        minCytoSizeSpinner.setValue(Prefs.get("MICMAC.cytoplasmminsize", 25.0));

        // expansion
        spinnerExpansionRadius.setValue((int) Prefs.get("MICMAQ.expansionRadius", 10));
        estimateCellsByNucleiCheckBox.setSelected(Prefs.get("MICMAQ.expansionCell", false));

        spinnerExpansionRadius.setEnabled(estimateCellsByNucleiCheckBox.isSelected());

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
        int approach = (thresholdingRadioButton.isSelected()) ? 1 : (cellposeRadioButton.isSelected()) ? 2 : (starDistRadioButton.isSelected()) ? 3 : 4;
        Prefs.set("MICMAQ.approach" + type, approach);
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
        Prefs.set("MICMAQ.cellposeCellproba_threshold" + type, (double) cellposeCellproba_thresholdSpinner.getValue());
        Prefs.set("MICMAQ.cellposeModelPath" + type, cellposeModelPath.getAbsolutePath());

//        --> stardist
        Prefs.set("MICMAQ.stardistModel" + type, (String) stardistModelComboBox.getSelectedItem());
        Prefs.set("MICMAQ.stardistPercentLow" + type, (double) stardistPercentileLowSpinner.getValue());
        Prefs.set("MICMAQ.stardistPercentHigh" + type, (double) stardistPercentileHighSpinner.getValue());
        Prefs.set("MICMAQ.stardistProbaThr" + type, (double) stardistProbaThrSpinner.getValue());
        Prefs.set("MICMAQ.stardistNMSThr" + type, (double) stardistNMSthrSpinner.getValue());
        Prefs.set("MICMAQ.stardistScale" + type, (double) stardistScaleSpinner.getValue());
        Prefs.set("MICMAQ.stardistModelPath" + type, stardistModelPath.getAbsolutePath());

//        --> macro segmentation
        Prefs.set("MICMAQ.macroSegmentationCode" + type, textAreaMacroSegment.getText());
        Prefs.set("MICMAQ.macroSegmentationRoi" + type, resultIsInRoiManagerCheckBox.isSelected());
        Prefs.set("MICMAQ.macroSegmentationImage" + type, resultIsAnInstanceCheckBox.isSelected());


//        --> cytoplasm
        Prefs.set("MICMAQ.cytoplasmoverlap", (double) minOverlapSpinner.getValue());
        Prefs.set("MICMAQ.cytoplasmminsize", (double) minCytoSizeSpinner.getValue());

        // expansion
        Prefs.set("MICMAQ.expansionRadius", (int) spinnerExpansionRadius.getValue());
        Prefs.set("MICMAQ.expansionCell", estimateCellsByNucleiCheckBox.isSelected());

    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
// List of methods for threshold
        thresholdMethodsCB = new JComboBox<>(AutoThresholder.getMethods());

        // minSize spinner
        thresholdMinSizeSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 0.0, Integer.MAX_VALUE, 10.0));
        cellposeDiameterSpinner = new JSpinner(new SpinnerNumberModel(200, 0, Integer.MAX_VALUE, 10));
        cellposeCellproba_thresholdSpinner = new JSpinner(new SpinnerNumberModel(0.0, -6.0, 6.0, 0.1));

//  Text field to filter extension
        sliceMinSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        sliceMaxSpinner = new JSpinner(new SpinnerNumberModel(33, 0, 9999, 1));

        //        min overlap of nuclei and cell
        minOverlapSpinner = new JSpinner(new SpinnerNumberModel(50, 0.0, 100.0, 10));
//        minimal size of cytoplasm compared to cell
        minCytoSizeSpinner = new JSpinner(new SpinnerNumberModel(25, 0.0, 100.0, 10));

        //stardist spinners
        stardistPercentileLowSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 0.1));
        stardistPercentileHighSpinner = new JSpinner(new SpinnerNumberModel(100, 0.0, 100.0, 0.1));
        stardistProbaThrSpinner = new JSpinner(new SpinnerNumberModel(0.5, 0.0, 1.0, 0.01));
        stardistNMSthrSpinner = new JSpinner(new SpinnerNumberModel(0.3, 0.0, 1.0, 0.01));
        stardistScaleSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.01, 100.0, 0.1));

        //expansion spinner
        spinnerExpansionRadius = new JSpinner(new SpinnerNumberModel(10, 0, 9999, 1));

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
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(10, 1, new Insets(0, 0, 0, 0), -1, -1));
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
        defaultComboBoxModel1.addElement("Sum Slices");
        projectionMethodCB.setModel(defaultComboBoxModel1);
        panel4.add(projectionMethodCB, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer4 = new com.intellij.uiDesigner.core.Spacer();
        projectionPanel.add(spacer4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        segmentationChoicePanel = new JPanel();
        segmentationChoicePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(segmentationChoicePanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        segmentationChoicePanel.setBorder(BorderFactory.createTitledBorder(null, "Segmentation method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        cellposeRadioButton = new JRadioButton();
        cellposeRadioButton.setSelected(true);
        cellposeRadioButton.setText("Cellpose");
        segmentationChoicePanel.add(cellposeRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdingRadioButton = new JRadioButton();
        thresholdingRadioButton.setSelected(false);
        thresholdingRadioButton.setText("Thresholding");
        segmentationChoicePanel.add(thresholdingRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Which method of segmentation to use?");
        segmentationChoicePanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer5 = new com.intellij.uiDesigner.core.Spacer();
        segmentationChoicePanel.add(spacer5, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        starDistRadioButton = new JRadioButton();
        starDistRadioButton.setText("StarDist");
        segmentationChoicePanel.add(starDistRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        otherRadioButton = new JRadioButton();
        otherRadioButton.setText("Other");
        segmentationChoicePanel.add(otherRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        cellposePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(cellposePanel, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        cellposePanel.setBorder(BorderFactory.createTitledBorder(null, "Cellpose parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label4 = new JLabel();
        label4.setText("model for Cellpose");
        cellposePanel.add(label4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellposeModelCB = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("cpsam");
        defaultComboBoxModel2.addElement("nuclei");
        defaultComboBoxModel2.addElement("cyto");
        defaultComboBoxModel2.addElement("cyto2");
        defaultComboBoxModel2.addElement("cyto2_cp3");
        defaultComboBoxModel2.addElement("cyto3");
        defaultComboBoxModel2.addElement("tissuenet");
        defaultComboBoxModel2.addElement("tissuenet_cp3");
        defaultComboBoxModel2.addElement("livecell");
        defaultComboBoxModel2.addElement("livecell_cp3");
        defaultComboBoxModel2.addElement("yeast_BF_cp3");
        defaultComboBoxModel2.addElement("yeast_PhC_cp3");
        defaultComboBoxModel2.addElement("bact_phase_cp3");
        defaultComboBoxModel2.addElement("bact_fluor_cp3");
        defaultComboBoxModel2.addElement("deepbacs_cp3");
        defaultComboBoxModel2.addElement("own_model");
        cellposeModelCB.setModel(defaultComboBoxModel2);
        cellposePanel.add(cellposeModelCB, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellposeDiameterLabel = new JLabel();
        cellposeDiameterLabel.setText("diameter (pixels)");
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
        final JLabel label6 = new JLabel();
        label6.setText("cellprob_threshold");
        cellposePanel.add(label6, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellposePanel.add(cellposeCellproba_thresholdSpinner, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel5, new com.intellij.uiDesigner.core.GridConstraints(8, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
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
        mainPanel.add(cytoPanel, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cytoPanel.setBorder(BorderFactory.createTitledBorder(null, "Cytoplasm extraction parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label7 = new JLabel();
        label7.setText("minimal overlap of nucleus with cell (% of nucleus)");
        cytoPanel.add(label7, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cytoPanel.add(minOverlapSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Minimal size of cytoplasm (% of cell)");
        cytoPanel.add(label8, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cytoPanel.add(minCytoSizeSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        measurementsPanel = new JPanel();
        measurementsPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(measurementsPanel, new com.intellij.uiDesigner.core.GridConstraints(9, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        measurementsPanel.setBorder(BorderFactory.createTitledBorder(null, "Measurements", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        setMeasurementsButton = new JButton();
        setMeasurementsButton.setText("set measurements");
        measurementsPanel.add(setMeasurementsButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stardistPanel = new JPanel();
        stardistPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(stardistPanel, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        stardistPanel.setBorder(BorderFactory.createTitledBorder(null, "StarDist parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label9 = new JLabel();
        label9.setText("model for StarDist");
        stardistPanel.add(label9, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stardistModelComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        defaultComboBoxModel3.addElement("Versatile (fluorescent nuclei)");
        defaultComboBoxModel3.addElement("Versatile (H&E nuclei)");
        defaultComboBoxModel3.addElement("DSB 2018 (from StarDist 2D paper)");
        defaultComboBoxModel3.addElement("Model (.zip) from File");
        stardistModelComboBox.setModel(defaultComboBoxModel3);
        stardistPanel.add(stardistModelComboBox, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("normalization percentile low");
        stardistPanel.add(label10, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stardistPanel.add(stardistPercentileLowSpinner, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("percentile high");
        stardistPanel.add(label11, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stardistPanel.add(stardistPercentileHighSpinner, new com.intellij.uiDesigner.core.GridConstraints(2, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("probability threshold");
        stardistPanel.add(label12, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stardistPanel.add(stardistProbaThrSpinner, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("overlap threshold");
        stardistPanel.add(label13, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stardistPanel.add(stardistNMSthrSpinner, new com.intellij.uiDesigner.core.GridConstraints(3, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stardistLoadModelPanel = new JPanel();
        stardistLoadModelPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        stardistPanel.add(stardistLoadModelPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("path to own model");
        stardistLoadModelPanel.add(label14, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stardistModelPathTextField = new JTextField();
        stardistLoadModelPanel.add(stardistModelPathTextField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        stardistBrowseButton = new JButton();
        stardistBrowseButton.setText("Browse");
        stardistLoadModelPanel.add(stardistBrowseButton, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        label15.setText("image scale");
        stardistPanel.add(label15, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stardistPanel.add(stardistScaleSpinner, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        expansionPanel = new JPanel();
        expansionPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(expansionPanel, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        expansionPanel.setBorder(BorderFactory.createTitledBorder(null, "expansion to cell", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        estimateCellsByNucleiCheckBox = new JCheckBox();
        estimateCellsByNucleiCheckBox.setText("estimate cells by nuclei expansion : ");
        expansionPanel.add(estimateCellsByNucleiCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spinnerExpansionRadius.setEnabled(false);
        expansionPanel.add(spinnerExpansionRadius, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        label16.setText("radius (pixels)");
        expansionPanel.add(label16, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        otherPanel = new JPanel();
        otherPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(otherPanel, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        otherPanel.setBorder(BorderFactory.createTitledBorder(null, "Other", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label17 = new JLabel();
        label17.setText("macro code");
        otherPanel.add(label17, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resultIsInRoiManagerCheckBox = new JCheckBox();
        resultIsInRoiManagerCheckBox.setText("result is in RoiManager");
        otherPanel.add(resultIsInRoiManagerCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resultIsAnInstanceCheckBox = new JCheckBox();
        resultIsAnInstanceCheckBox.setText("result is an instance segmentation mask");
        otherPanel.add(resultIsAnInstanceCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        otherPanel.add(scrollPane2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textAreaMacroSegment = new JTextArea();
        scrollPane2.setViewportView(textAreaMacroSegment);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(cellposeRadioButton);
        buttonGroup.add(thresholdingRadioButton);
        buttonGroup.add(starDistRadioButton);
        buttonGroup.add(otherRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
