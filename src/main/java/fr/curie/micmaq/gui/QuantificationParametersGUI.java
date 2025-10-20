package fr.curie.micmaq.gui;

import fr.curie.micmaq.config.FieldOfView;
import fr.curie.micmaq.config.FieldOfViewProvider;
import fr.curie.micmaq.config.MeasureValue;
import fr.curie.micmaq.helpers.ImageToAnalyze;
import ij.Prefs;
import ij.measure.Measurements;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class QuantificationParametersGUI {
    private JButton setMeasurementsButton;
    private JCheckBox useMacroCodeCheckBox;
    private JPanel macroTextPanel;
    private JTextArea macroTextArea;
    private JCheckBox isZStackCheckBox;
    private JPanel projectionPanel;
    private JCheckBox chooseSlicesToUseCheckBox;
    private JSpinner sliceMaxSpinner;
    private JSpinner sliceMinSpinner;
    private JComboBox projectionMethodCB;
    private JCheckBox meanGrayValueCheckBox;
    private JCheckBox integratedDensityCheckBox;
    private JCheckBox medianGrayValueCheckBox;
    private JCheckBox minAndMaxGrayCheckBox;
    private JCheckBox modalGrayValueCheckBox;
    private JCheckBox standardDeviationCheckBox;
    private JCheckBox skewnessCheckBox;
    private JCheckBox kurtosisCheckBox;
    private JPanel rootPanel;
    private JCheckBox areaCheckBox;
    private JCheckBox perimeterCheckBox;
    private JCheckBox boundingRectangleCheckBox;
    private JCheckBox centroidCheckBox;
    private JCheckBox centerOfMassCheckBox;
    private JCheckBox areaFractionCheckBox;
    private JCheckBox fitEllipseCheckBox;
    private JCheckBox shapeDescriptorCheckBox;
    private JCheckBox feretSDiametersCheckBox;
    private JComboBox comboBoxPreProc;
    private JComboBox comboBoxMeasure;
    private JCheckBox summaryTableCheckBox;
    private JCheckBox countOnlyPositiveCellsCheckBox;

    protected FieldOfViewProvider provider;
    protected MeasureValue measuresQuantif;
    protected MeasureValue measuresSegmentation;
    private static final String QUANTIFICATION = "Quantif";
    private static final String SEGMENTATION = "Segment";
    String type = "Quantif";

    ComboCheckBox preprocComboCheck;
    ComboCheckBox measureComboCheck;


    public QuantificationParametersGUI(FieldOfViewProvider imageProvider) {
        provider = imageProvider;
        measuresQuantif = new MeasureValue(false);
        measuresSegmentation = new MeasureValue(true);
        $$$setupUI$$$();
        getPreferences();
        type = QUANTIFICATION;
        int measurements = (int) Prefs.get("MICMAQ.Measurements_" + type, (Measurements.MEAN + Measurements.INTEGRATED_DENSITY));
        measuresQuantif.setMeasure(measurements); /*Default measurements*/

        type = SEGMENTATION;
        int measurements2 = (int) Prefs.get("MICMAQ.Measurements_" + type, (Measurements.MEAN + Measurements.INTEGRATED_DENSITY + Measurements.AREA));
        measuresSegmentation.setMeasure(measurements2); /*Default measurements*/

        System.out.println("#### QuantificationparameterGUI creation : " + measuresQuantif.getMeasure());
        System.out.println("#### QuantificationparameterGUI creation : " + measuresSegmentation.getMeasure());

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
        useMacroCodeCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                macroTextPanel.setVisible(useMacroCodeCheckBox.isSelected());
            }
        });

        medianGrayValueCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        standardDeviationCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        minAndMaxGrayCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        modalGrayValueCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        skewnessCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        kurtosisCheckBox.addActionListener(e -> {
            updateMeasures();
        });

        areaCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        perimeterCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        boundingRectangleCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        centroidCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        centerOfMassCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        areaFractionCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        fitEllipseCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        shapeDescriptorCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        feretSDiametersCheckBox.addActionListener(e -> {
            updateMeasures();
        });
        summaryTableCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countOnlyPositiveCellsCheckBox.setEnabled(summaryTableCheckBox.isSelected());
            }
        });
    }

    public MeasureValue getMeasuresQuantif(int channel) {
        setPreferences();

        MeasureValue measure = (measureComboCheck.isSelected(channel)) ? measuresQuantif : new MeasureValue(false);
        if (preprocComboCheck.isSelected(channel)) {
            if (isZStackCheckBox.isSelected()) {
                measure.setProjection(projectionMethodCB.getSelectedIndex());
                if (chooseSlicesToUseCheckBox.isSelected()) {
                    measure.setProjectionSliceMin((Integer) sliceMinSpinner.getValue());
                    measure.setProjectionSliceMax((Integer) sliceMaxSpinner.getValue());
                }
                if (useMacroCodeCheckBox.isSelected()) measure.setPreprocessMacro(macroTextArea.getText());
            }
        }
        measure.setSummary(summaryTableCheckBox.isSelected());
        return measure;
    }

    public MeasureValue getMeasuresSegmentation(int channel) {
        setPreferences();
        MeasureValue measure = (measureComboCheck.isSelected(channel)) ? measuresSegmentation : new MeasureValue(true);
        if (preprocComboCheck.isSelected(channel)) {
            if (isZStackCheckBox.isSelected()) {
                measure.setProjection(projectionMethodCB.getSelectedIndex());
                if (chooseSlicesToUseCheckBox.isSelected()) {
                    measure.setProjectionSliceMin((Integer) sliceMinSpinner.getValue());
                    measure.setProjectionSliceMax((Integer) sliceMaxSpinner.getValue());
                }
                if (useMacroCodeCheckBox.isSelected()) measure.setPreprocessMacro(macroTextArea.getText());
            }
        }
        measure.setSummary(summaryTableCheckBox.isSelected());
        System.out.println("####### quantificationParameterGUI getmeasuresegmentation : " + measure.getMeasure());
        System.out.println("####### quantificationParameterGUI getmeasuresegmentation measureSegmentation : " + measuresSegmentation.getMeasure());
        return measure;

    }

    public void resetMeasures() {
        medianGrayValueCheckBox.setSelected(false);
        standardDeviationCheckBox.setSelected(false);
        minAndMaxGrayCheckBox.setSelected(false);
        skewnessCheckBox.setSelected(false);
        modalGrayValueCheckBox.setSelected(false);
        kurtosisCheckBox.setSelected(false);
        perimeterCheckBox.setSelected(false);
        boundingRectangleCheckBox.setSelected(false);
        centroidCheckBox.setSelected(false);
        centerOfMassCheckBox.setSelected(false);
        areaFractionCheckBox.setSelected(false);
        fitEllipseCheckBox.setSelected(false);
        shapeDescriptorCheckBox.setSelected(false);
        feretSDiametersCheckBox.setSelected(false);

        isZStackCheckBox.setSelected(false);
        useMacroCodeCheckBox.setSelected(false);
        summaryTableCheckBox.setSelected(false);
    }

    public void setMeasures(int channel, ArrayList<String> params) {
        boolean q = false;
        boolean m = false;
        String macro = "";
        boolean includeMeasure = false;
        boolean includeProcess = false;

        for (int i = 0; i < params.size(); i++) {
            System.out.println(params.get(i));
            if (q) {
                if (params.get(i).startsWith("Measurements")) {
                    m = false;
                    System.out.println("stop macro");
                }
                if (m) {
                    macro += params.get(i) + "\n";
                    //System.out.println("add text to macro "+params.get(i));
                    //System.out.println("macro becomes :\n"+macro);
                    macroTextArea.setText(macro);
                } else {

                    if (params.get(i).startsWith("Macro:")) {
                        m = true;
                        //System.out.println("start macro and activate in GUI");
                        useMacroCodeCheckBox.setSelected(true);
                        macroTextPanel.setVisible(true);
                        includeProcess = true;
                    }

                    if (params.get(i).startsWith("Projection")) {
                        isZStackCheckBox.setSelected(true);
                        //System.out.println("change projection");
                        includeProcess = true;
                    }

                    if (params.get(i).startsWith("median")) {
                        medianGrayValueCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("std_dev")) {
                        standardDeviationCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("min_max")) {
                        minAndMaxGrayCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("skewness")) {
                        skewnessCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("mode")) {
                        modalGrayValueCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("kurtosis")) {
                        kurtosisCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("perimeter")) {
                        perimeterCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("bound_rectangle")) {
                        boundingRectangleCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("centroid")) {
                        centroidCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("center_of_mass")) {
                        centerOfMassCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("area_fraction")) {
                        areaFractionCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("fit_ellipse")) {
                        fitEllipseCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("shape_descriptor")) {
                        shapeDescriptorCheckBox.setSelected(true);
                        includeMeasure = true;
                    }
                    if (params.get(i).startsWith("Feret's_diameter")) {
                        feretSDiametersCheckBox.setSelected(true);
                        includeMeasure = true;
                    }

                }

            }
            if (params.get(i).startsWith("Quantification  Parameters")) q = true;
        }//end for
        if (!includeMeasure) {
            measureComboCheck.setSelected(channel, false);
            measureComboCheck.setSelected(0, false); // unselect all
        }
        if (!includeProcess) {
            preprocComboCheck.setSelected(channel, false);
            preprocComboCheck.setSelected(0, false);//unselect All
        }
    }


    public String getMacro(int channel) {
        if (useMacroCodeCheckBox.isSelected() && preprocComboCheck.isSelected(channel)) return macroTextArea.getText();
        return null;
    }

    protected void updateMeasures() {
        type = QUANTIFICATION;
        int measurementsValue = Measurements.MEAN + Measurements.INTEGRATED_DENSITY;
        if (medianGrayValueCheckBox.isSelected()) {
            measurementsValue += Measurements.MEDIAN;
        }
        if (standardDeviationCheckBox.isSelected()) {
            measurementsValue += Measurements.STD_DEV;
        }
        if (minAndMaxGrayCheckBox.isSelected()) {
            measurementsValue += Measurements.MIN_MAX;
        }
        if (skewnessCheckBox.isSelected()) {
            measurementsValue += Measurements.SKEWNESS;
        }
        if (modalGrayValueCheckBox.isSelected()) {
            measurementsValue += Measurements.MODE;
        }
        if (kurtosisCheckBox.isSelected()) {
            measurementsValue += Measurements.KURTOSIS;
        }
        Prefs.set("MICMAQ.Measurements_" + type, measurementsValue);
        if (measuresQuantif != null) measuresQuantif.setMeasure(measurementsValue);

        type = SEGMENTATION;
        measurementsValue += Measurements.AREA;
        if (perimeterCheckBox.isSelected()) measurementsValue += Measurements.PERIMETER;
        if (boundingRectangleCheckBox.isSelected()) measurementsValue += Measurements.RECT;
        if (centroidCheckBox.isSelected()) measurementsValue += Measurements.CENTROID;
        if (centerOfMassCheckBox.isSelected()) measurementsValue += Measurements.CENTER_OF_MASS;
        if (areaFractionCheckBox.isSelected()) measurementsValue += Measurements.AREA_FRACTION;
        if (fitEllipseCheckBox.isSelected()) measurementsValue += Measurements.ELLIPSE;
        if (shapeDescriptorCheckBox.isSelected()) measurementsValue += Measurements.SHAPE_DESCRIPTORS;
        if (feretSDiametersCheckBox.isSelected()) measurementsValue += Measurements.FERET;

        Prefs.set("MICMAQ.Measurements_" + type, measurementsValue);
        if (measuresSegmentation != null) measuresSegmentation.setMeasure(measurementsValue);
        Prefs.savePreferences();
    }

    private void getPreferences() {
//        Projection
        isZStackCheckBox.setSelected(Prefs.get("MICMAQ.zStack", true));
        projectionMethodCB.setSelectedItem(Prefs.get("MICMAQ.ProjMethods", "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("MICMAQ.chooseSlices", false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            //slicesPanel.setVisible(false);
            sliceMinSpinner.setVisible(false);
            sliceMaxSpinner.setVisible(false);
        }
        int maxSlices = 1;
        FieldOfView fov = provider.getFirstValidFieldOfView();
        for (int i = 1; i <= fov.getNbAvailableChannels(); i++) {
            maxSlices = Math.max(maxSlices, fov.getNSlices(i));
        }


        int firstSlice = (int) Prefs.get("MICMAQ.firstSlice", 1);
        int lastSlice = (int) Prefs.get("MICMAQ.lastSlice", maxSlices);
        ImageToAnalyze.assertSlices(maxSlices, firstSlice, lastSlice, sliceMinSpinner, sliceMaxSpinner);

        type = QUANTIFICATION;
//        Macro
        useMacroCodeCheckBox.setSelected(Prefs.get("MICMAQ.useMacro" + type, false));
        if (!useMacroCodeCheckBox.isSelected()) macroTextPanel.setVisible(false);
        macroTextArea.append(Prefs.get("MICMAQ.macro" + type, " "));

        int measurements = (int) Prefs.get("MICMAQ.Measurements_" + type, (Measurements.MEAN + Measurements.INTEGRATED_DENSITY));
        standardDeviationCheckBox.setSelected((measurements & Measurements.STD_DEV) != 0);
        minAndMaxGrayCheckBox.setSelected((measurements & Measurements.MIN_MAX) != 0);
        skewnessCheckBox.setSelected((measurements & Measurements.SKEWNESS) != 0);
        medianGrayValueCheckBox.setSelected((measurements & Measurements.MEDIAN) != 0);
        kurtosisCheckBox.setSelected((measurements & Measurements.KURTOSIS) != 0);
        modalGrayValueCheckBox.setSelected((measurements & Measurements.MODE) != 0);

        type = SEGMENTATION;
        measurements = (int) Prefs.get("MICMAQ.Measurements_" + type, (Measurements.MEAN + Measurements.INTEGRATED_DENSITY));
        perimeterCheckBox.setSelected((measurements & Measurements.PERIMETER) != 0);
        boundingRectangleCheckBox.setSelected((measurements & Measurements.RECT) != 0);
        centroidCheckBox.setSelected((measurements & Measurements.CENTROID) != 0);
        centerOfMassCheckBox.setSelected((measurements & Measurements.CENTER_OF_MASS) != 0);
        areaFractionCheckBox.setSelected((measurements & Measurements.AREA_FRACTION) != 0);
        fitEllipseCheckBox.setSelected((measurements & Measurements.ELLIPSE) != 0);
        shapeDescriptorCheckBox.setSelected((measurements & Measurements.SHAPE_DESCRIPTORS) != 0);
        feretSDiametersCheckBox.setSelected((measurements & Measurements.FERET) != 0);

        summaryTableCheckBox.setSelected(Prefs.get("MICMAQ.summary", false));
        countOnlyPositiveCellsCheckBox.setEnabled(summaryTableCheckBox.isSelected());
        countOnlyPositiveCellsCheckBox.setSelected(Prefs.get("MICMAQ.summaryPositive", false));

    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    /**
     * set prefs in prefs file
     */
    public void setPreferences() {
        type = QUANTIFICATION;
//        Projection
        Prefs.set("MICMAQ.zStack_" + type, isZStackCheckBox.isSelected());
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
        Prefs.set("MICMAQ.summary", summaryTableCheckBox.isSelected());
        Prefs.set("MICMAQ.summaryPositive", countOnlyPositiveCellsCheckBox.isSelected());
//      the checkbox is automaticaly saved in update measures

    }

    public String getParametersAsString(boolean morphological, int channel) {
        String result = "\nQuantification  Parameters";
        result += "\nPreprocessing:";
        if (preprocComboCheck.isSelected(channel)) {
            if (isZStackCheckBox.isSelected()) {
                result += "\n\tProjection: " + (String) projectionMethodCB.getSelectedItem();
                if (chooseSlicesToUseCheckBox.isSelected()) {
                    result += "\n\tSlices " + (int) sliceMinSpinner.getValue() + "-" + sliceMaxSpinner.getValue();
                }
            }
            if (useMacroCodeCheckBox.isSelected()) {
                result += "\n\tMacro:\n" + macroTextArea.getText();
            }
        } else {
            result += "\n\tNone";
        }
        result += "\nMeasurements (intensity based):";
        result += "\n\tmean\n\tintegrated density";
        if (measureComboCheck.isSelected(channel)) {
            if (medianGrayValueCheckBox.isSelected()) result += "\n\tmedian";
            if (standardDeviationCheckBox.isSelected()) result += "\n\tstd_dev";
            if (minAndMaxGrayCheckBox.isSelected()) result += "\n\tmin_max";
            if (skewnessCheckBox.isSelected()) result += "\n\tskewness";
            if (modalGrayValueCheckBox.isSelected()) result += "\n\tmode";
            if (kurtosisCheckBox.isSelected()) result += "\n\tkurtosis";
        }
        if (morphological) {
            result += "\nMeasurements (morphological):";
            result += "\n\tarea";
            if (measureComboCheck.isSelected(channel)) {
                if (perimeterCheckBox.isSelected()) result += "\n\tperimeter";
                if (boundingRectangleCheckBox.isSelected()) result += "\n\tbound_rectangle";
                if (centroidCheckBox.isSelected()) result += "\n\tcentroid";
                if (centerOfMassCheckBox.isSelected()) result += "\n\tcenter_of_mass";
                if (areaFractionCheckBox.isSelected()) result += "\n\tarea_fraction";
                if (fitEllipseCheckBox.isSelected()) result += "\n\tfit_ellipse";
                if (shapeDescriptorCheckBox.isSelected()) result += "\n\tshape_descriptor";
                if (feretSDiametersCheckBox.isSelected()) result += "\n\tFeret's_diameter";
            }
        }
        return result;
    }

    public boolean getSummary() {
        return summaryTableCheckBox.isSelected();
    }

    public boolean getCountOnlyPositiveCells() {
        return countOnlyPositiveCellsCheckBox.isSelected();
    }

    public void updateComboCheckbox() {
        preprocComboCheck.updateTexts(provider);
        measureComboCheck.updateTexts(provider);
    }

    private void createUIComponents() {

//  Text field to filter extension
        sliceMinSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        sliceMaxSpinner = new JSpinner(new SpinnerNumberModel(33, 0, 9999, 1));

        preprocComboCheck = new ComboCheckBox(provider);
        comboBoxPreProc = preprocComboCheck.getComboBox();
        measureComboCheck = new ComboCheckBox(provider);
        comboBoxMeasure = measureComboCheck.getComboBox();

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
        rootPanel = new JPanel();
        rootPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(null, "Measurements", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 3, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder(null, "intensity based", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        meanGrayValueCheckBox = new JCheckBox();
        meanGrayValueCheckBox.setEnabled(false);
        meanGrayValueCheckBox.setSelected(true);
        meanGrayValueCheckBox.setText("mean gray value");
        panel2.add(meanGrayValueCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        medianGrayValueCheckBox = new JCheckBox();
        medianGrayValueCheckBox.setText("median gray value");
        panel2.add(medianGrayValueCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        integratedDensityCheckBox = new JCheckBox();
        integratedDensityCheckBox.setEnabled(false);
        integratedDensityCheckBox.setSelected(true);
        integratedDensityCheckBox.setText("integrated density");
        panel2.add(integratedDensityCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minAndMaxGrayCheckBox = new JCheckBox();
        minAndMaxGrayCheckBox.setText("min and max gray value");
        panel2.add(minAndMaxGrayCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        modalGrayValueCheckBox = new JCheckBox();
        modalGrayValueCheckBox.setText("modal gray value");
        panel2.add(modalGrayValueCheckBox, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        standardDeviationCheckBox = new JCheckBox();
        standardDeviationCheckBox.setText("standard deviation");
        panel2.add(standardDeviationCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        skewnessCheckBox = new JCheckBox();
        skewnessCheckBox.setText("skewness");
        panel2.add(skewnessCheckBox, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kurtosisCheckBox = new JCheckBox();
        kurtosisCheckBox.setText("kurtosis");
        panel2.add(kurtosisCheckBox, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(null, "morphological", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        areaCheckBox = new JCheckBox();
        areaCheckBox.setEnabled(false);
        areaCheckBox.setSelected(true);
        areaCheckBox.setText("area");
        panel3.add(areaCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        perimeterCheckBox = new JCheckBox();
        perimeterCheckBox.setText("perimeter");
        panel3.add(perimeterCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        boundingRectangleCheckBox = new JCheckBox();
        boundingRectangleCheckBox.setText("bounding rectangle");
        panel3.add(boundingRectangleCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        centroidCheckBox = new JCheckBox();
        centroidCheckBox.setText("centroid");
        panel3.add(centroidCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        centerOfMassCheckBox = new JCheckBox();
        centerOfMassCheckBox.setText("center of mass");
        panel3.add(centerOfMassCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        areaFractionCheckBox = new JCheckBox();
        areaFractionCheckBox.setText("area fraction");
        panel3.add(areaFractionCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fitEllipseCheckBox = new JCheckBox();
        fitEllipseCheckBox.setText("fit ellipse");
        panel3.add(fitEllipseCheckBox, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        shapeDescriptorCheckBox = new JCheckBox();
        shapeDescriptorCheckBox.setText("shape descriptor");
        panel3.add(shapeDescriptorCheckBox, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        feretSDiametersCheckBox = new JCheckBox();
        feretSDiametersCheckBox.setText("Ferret's diameters");
        panel3.add(feretSDiametersCheckBox, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("additional measurements applied on ");
        panel4.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel4.add(comboBoxMeasure, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        useMacroCodeCheckBox = new JCheckBox();
        useMacroCodeCheckBox.setText("use macro code");
        panel6.add(useMacroCodeCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        panel6.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        panel5.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroTextPanel = new JPanel();
        macroTextPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(macroTextPanel, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        macroTextPanel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroTextArea = new JTextArea();
        macroTextArea.setRows(2);
        scrollPane1.setViewportView(macroTextArea);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel7, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, new Dimension(-1, 61), null, null, 0, false));
        isZStackCheckBox = new JCheckBox();
        isZStackCheckBox.setText("is Z-stack?");
        panel7.add(isZStackCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTHWEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projectionPanel = new JPanel();
        projectionPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(projectionPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        projectionPanel.add(panel8, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("method of projection");
        panel8.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("choose slices to use");
        panel8.add(chooseSlicesToUseCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 28), null, null, 0, false));
        panel8.add(sliceMaxSpinner, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel8.add(sliceMinSpinner, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        panel8.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        projectionMethodCB = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Maximum projection");
        defaultComboBoxModel1.addElement("Standard deviation projection");
        defaultComboBoxModel1.addElement("Sum Slices");
        projectionMethodCB.setModel(defaultComboBoxModel1);
        panel8.add(projectionMethodCB, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer4 = new com.intellij.uiDesigner.core.Spacer();
        projectionPanel.add(spacer4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel9, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("apply quantification preprocessing on");
        panel9.add(label3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel9.add(comboBoxPreProc, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel10, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel10.setBorder(BorderFactory.createTitledBorder(null, "summary", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        summaryTableCheckBox = new JCheckBox();
        summaryTableCheckBox.setText("create summary table");
        panel10.add(summaryTableCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer5 = new com.intellij.uiDesigner.core.Spacer();
        panel10.add(spacer5, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        countOnlyPositiveCellsCheckBox = new JCheckBox();
        countOnlyPositiveCellsCheckBox.setEnabled(false);
        countOnlyPositiveCellsCheckBox.setText("count only positive cells to average spots");
        panel10.add(countOnlyPositiveCellsCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
