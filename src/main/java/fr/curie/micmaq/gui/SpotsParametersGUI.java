package fr.curie.micmaq.gui;

import fr.curie.micmaq.config.FieldOfViewProvider;
import fr.curie.micmaq.config.ImageProvider;
import fr.curie.micmaq.config.MeasureValue;
import fr.curie.micmaq.helpers.ImageToAnalyze;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.measure.Measurements;
import ij.process.AutoThresholder;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class SpotsParametersGUI {
    private JPanel mainPanel;
    private JPanel preprocessingPanel;
    private JCheckBox isZStackCheckBox;
    private JComboBox projectionMethodCB;
    private JPanel zstackPanel;
    private JCheckBox chooseSlicesToUseCheckBox;
    private JSpinner sliceMinSpinner;
    private JSpinner sliceMaxSpinner;
    private JCheckBox useMacroCodeCheckBox;
    private JPanel macroPanel;
    private JTextArea macroArea;
    private JCheckBox subtractBackgroundCheckBox;
    private JSpinner sbgSpinner;
    private JCheckBox findMaximaCheckBox;
    private JPanel maximaPanel;
    private JSpinner prominenceSpinner;
    private JCheckBox thresholdCheckBox;
    private JComboBox thresholdMethodCB;
    private JSpinner minSizeSpinner;
    private JCheckBox useWatershedCheckBox;
    private JPanel slicesPanel;
    private JPanel projectionPanel;
    private JPanel sbgPanel;
    private JPanel thresholdPanel;
    private JLabel thresholdLabel;
    private JLabel minSizeLabel;
    private JLabel prominenceLabel;
    private JPanel spotDetectionPanel;
    private JSpinner userThresholdSpinner;
    private JCheckBox darkBGCheckBox;

    protected int channel = -1;
    FieldOfViewProvider imageProvider;
    protected String proteinName;
    protected String[] thresholdMethods = null;

    public SpotsParametersGUI(FieldOfViewProvider imageProvider, int channel) {
        this.channel = channel;
        this.imageProvider = imageProvider;
        $$$setupUI$$$();
        mainPanel.setBorder(BorderFactory.createTitledBorder("Spot detection parameters for channel " + channel));
        getPreferences();

        projectionPanel.setVisible(isZStackCheckBox.isSelected());
        macroPanel.setVisible(useMacroCodeCheckBox.isSelected());
        //maximaPanel.setVisible(false);
        //thresholdPanel.setVisible(false);
        ((SpinnerNumberModel) minSizeSpinner.getModel()).setMinimum(0);

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
                macroPanel.setVisible(useMacroCodeCheckBox.isSelected());
            }
        });
        subtractBackgroundCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sbgPanel.setVisible(subtractBackgroundCheckBox.isSelected());
            }
        });
        findMaximaCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnablingOfMaximaPanel(findMaximaCheckBox.isSelected());
            }
        });
        thresholdCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnablingOfThresholdPanel(thresholdCheckBox.isSelected());
            }
        });

        thresholdMethodCB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userThresholdSpinner.setVisible(thresholdMethodCB.getSelectedIndex() == thresholdMethods.length - 1);
                darkBGCheckBox.setVisible(thresholdMethodCB.getSelectedIndex() == thresholdMethods.length - 1);
            }
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public int getChannel() {
        return channel;
    }

    public String getProteinName() {
        return proteinName;
    }

    public void setProteinName(String proteinName) {
        this.proteinName = proteinName;
    }

    //Enable panel according to checkbox
    private void setEnablingOfThresholdPanel(boolean enabled) {
        thresholdPanel.setEnabled(enabled);
        thresholdMethodCB.setEnabled(enabled);
        thresholdLabel.setEnabled(enabled);
        useWatershedCheckBox.setEnabled(enabled);
        minSizeLabel.setEnabled(enabled);
        minSizeSpinner.setEnabled(enabled);
        userThresholdSpinner.setEnabled(enabled);
        darkBGCheckBox.setEnabled(enabled);
    }

    private void setEnablingOfMaximaPanel(boolean enabled) {
        maximaPanel.setEnabled(enabled);
        prominenceLabel.setEnabled(enabled);
        prominenceSpinner.setEnabled(enabled);
    }

    public MeasureValue getMeasure() {
        MeasureValue measureValue = new MeasureValue();
        measureValue.setMeasure(Measurements.MEAN + Measurements.INTEGRATED_DENSITY);
        if (isZStackCheckBox.isSelected()) {
            measureValue.setProjection(projectionMethodCB.getSelectedIndex());
            if (chooseSlicesToUseCheckBox.isSelected()) {
                measureValue.setProjectionSliceMin((Integer) sliceMinSpinner.getValue());
                measureValue.setProjectionSliceMax((Integer) sliceMaxSpinner.getValue());
            }
        }
        if (useMacroCodeCheckBox.isSelected()) measureValue.setPreprocessMacro(macroArea.getText());

        if (subtractBackgroundCheckBox.isSelected()) measureValue.setSubtractBGRadius((Double) sbgSpinner.getValue());

        if (thresholdCheckBox.isSelected()) {
            double minTh = darkBGCheckBox.isSelected() ? (double) userThresholdSpinner.getValue() : -1.0E30D;
            double maxTh = darkBGCheckBox.isSelected() ? 1.0E30D : (double) userThresholdSpinner.getValue();

            measureValue.setSpotThreshold((String) (thresholdMethodCB.getSelectedItem()), minTh, maxTh, (Integer) minSizeSpinner.getValue(), useWatershedCheckBox.isSelected());
        }
        if (findMaximaCheckBox.isSelected()) {
            measureValue.setSpotFindMaxima((Double) prominenceSpinner.getValue());
        }
        setPreferences();
        return measureValue;
    }

    /**
     * Get preferences from ImageJ/Fiji file
     */
    private void getPreferences() {
        String id = "" + channel;
//        Z-projection
        isZStackCheckBox.setSelected(Prefs.get("MICMAQ.zStackSpot_" + id, true));
        projectionMethodCB.setSelectedItem(Prefs.get("MICMAQ.ProjMethodsSpot_" + id, "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("MICMAQ.chooseSlicesSpot_" + id, false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            sliceMinSpinner.setVisible(false);
            sliceMaxSpinner.setVisible(false);
        }
        //ImagePlus imptmp = imageProvider.getImagePlus(0, channel);
        //int maxSlices = imptmp.getNSlices();
        int maxSlices = imageProvider.getFieldOfView(0).getNSlices(channel);
        int firstSlice = (int) Prefs.get("MICMAQ.firstSliceSpot_" + id, 1);
        int lastSlice = (int) Prefs.get("MICMAQ.lastSliceSpot_" + id, maxSlices);
        ImageToAnalyze.assertSlices(maxSlices, firstSlice, lastSlice, sliceMinSpinner, sliceMaxSpinner);

//        Macro
        useMacroCodeCheckBox.setSelected(Prefs.get("MICMAQ.useMacroSpot_" + id, false));
        if (!useMacroCodeCheckBox.isSelected()) macroPanel.setVisible(false);
        macroArea.append(Prefs.get("MICMAQ.macroSpot_" + id, " "));

//        Rolling ball
        subtractBackgroundCheckBox.setSelected(Prefs.get("MICMAQ.rollingballCheckSpot_" + id, true));
        sbgSpinner.setValue(Prefs.get("MICMAQ.rollingballSizeSpot_" + id, 10));

//        Find maxima
        boolean maximaEnable = Prefs.get("MICMAQ.findMaximaSelectedSpot_" + id, true);
        findMaximaCheckBox.setSelected(maximaEnable);
        setEnablingOfMaximaPanel(maximaEnable);
        prominenceSpinner.setValue(Prefs.get("MICMAQ.prominence_" + id, 500));

//       Threshold
        boolean thresholdEnable = Prefs.get("MICMAQ.thresholdSelectedSpot_" + id, false);
        thresholdCheckBox.setSelected(thresholdEnable);
        setEnablingOfThresholdPanel(thresholdEnable);
        thresholdMethodCB.setSelectedItem(Prefs.get("MICMAQ.thresholdMethodSpot_" + id, "Li"));
        userThresholdSpinner.setVisible(thresholdMethodCB.getSelectedIndex() == thresholdMethods.length - 1);
        userThresholdSpinner.setValue((double) Prefs.get("MICMAQ.userthreshold_" + id, 10));
        darkBGCheckBox.setSelected(Prefs.get("MICMAQ_darkBG_" + id, true));
        minSizeSpinner.setValue((int) Prefs.get("MICMAQ.minSizeSpot_" + id, 10));
        useWatershedCheckBox.setSelected(Prefs.get("MICMAQ.useWatershedSpot_" + id, false));


    }

    public String getParametersAsString() {
        String result = "\nSPOT " + channel + " Parameters";
        result += "\nPreprocessing:";
        if (isZStackCheckBox.isSelected()) {
            result += "\n\tProjection: " + (String) projectionMethodCB.getSelectedItem();
            if (chooseSlicesToUseCheckBox.isSelected()) {
                result += "\n\tSlices " + (int) sliceMinSpinner.getValue() + "-" + sliceMaxSpinner.getValue();
            }
        }
        if (useMacroCodeCheckBox.isSelected()) {
            result += "\n\tMacro:\n" + macroArea.getText();
        }
        if (subtractBackgroundCheckBox.isSelected())
            result += "\n\tSubtract Background: radius " + sbgSpinner.getValue();

        if (findMaximaCheckBox.isSelected()) {
            result += "\nFind Spots by Local Maxima:";
            result += "\n\tProminence: " + prominenceSpinner.getValue();
        }
        if (thresholdCheckBox.isSelected()) {
            result += "\nFind Spots b thresholding:";
            if (thresholdMethodCB.getSelectedIndex() == thresholdMethods.length - 1) {
                result += "\n\tUser defined threshold: " + userThresholdSpinner.getValue();
                result += "\n\tDark background: " + darkBGCheckBox.isSelected();
            } else {
                result += "\n\tAutomatic threshold method: " + thresholdMethodCB.getSelectedItem();
            }

            result += "\n\tMinimum spot diameter: " + minSizeSpinner.getValue();
            result += "\n\tWatershed: " + (useWatershedCheckBox.isSelected() ? "yes" : "no");
        }

        return result;
    }

    /**
     * Set preferences for ImageJ/Fiji
     */
    public void setPreferences() {
        String id = "" + channel;
//        Z-projection
        Prefs.set("MICMAQ.zStackSpot_" + id, isZStackCheckBox.isSelected());
        if (isZStackCheckBox.isSelected()) {
            Prefs.set("MICMAQ.ProjMethodsSpot_" + id, (String) projectionMethodCB.getSelectedItem());
            Prefs.set("MICMAQ.chooseSlicesSpot_" + id, chooseSlicesToUseCheckBox.isSelected());
            if (chooseSlicesToUseCheckBox.isSelected()) {
                Prefs.set("MICMAQ.firstSliceSpot_" + id, (double) (int) sliceMinSpinner.getValue());
                Prefs.set("MICMAQ.lastSliceSpot_" + id, (double) (int) sliceMaxSpinner.getValue());
            }
        }

//        Macro
        Prefs.set("MICMAQ.useMacroSpot_" + id, useMacroCodeCheckBox.isSelected());
        Prefs.set("MICMAQ.macroSpot_" + id, macroArea.getText());

//        Rolling ball
        Prefs.set("MICMAQ.rollingballCheckSpot_" + id, subtractBackgroundCheckBox.isSelected());
        Prefs.set("MICMAQ.rollingballSizeSpot_" + id, (double) sbgSpinner.getValue());

//        Find maxima
        Prefs.set("MICMAQ.findMaximaSelectedSpot_" + id, findMaximaCheckBox.isSelected());
        Prefs.set("MICMAQ.prominence_" + id, (double) prominenceSpinner.getValue());

//        Threshold
        Prefs.set("MICMAQ.thresholdSelectedSpot_" + id, thresholdCheckBox.isSelected());
        Prefs.set("MICMAQ.thresholdMethodSpot_" + id, (String) thresholdMethodCB.getSelectedItem());
        Prefs.set("MICMAQ.userthreshold_" + id, (double) userThresholdSpinner.getValue());
        Prefs.set("MICMAQ.darkBG_" + id, darkBGCheckBox.isSelected());
        Prefs.set("MICMAQ.minSizeSpot_" + id, (int) minSizeSpinner.getValue());
        Prefs.set("MICMAQ.useWatershedSpot_" + id, useWatershedCheckBox.isSelected());


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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel = new JPanel();
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(mainPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(800, 600), null, 2, false));
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Spot detection parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        preprocessingPanel = new JPanel();
        preprocessingPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(preprocessingPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        preprocessingPanel.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        preprocessingPanel.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, new Dimension(-1, 61), null, null, 0, false));
        isZStackCheckBox = new JCheckBox();
        isZStackCheckBox.setText("is Z-stack?");
        panel2.add(isZStackCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTHWEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projectionPanel = new JPanel();
        projectionPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(projectionPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        zstackPanel = new JPanel();
        zstackPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        projectionPanel.add(zstackPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("method of projection");
        zstackPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("choose slices to use");
        zstackPanel.add(chooseSlicesToUseCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 28), null, null, 0, false));
        sliceMinSpinner = new JSpinner();
        zstackPanel.add(sliceMinSpinner, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sliceMaxSpinner = new JSpinner();
        zstackPanel.add(sliceMaxSpinner, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projectionMethodCB = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Maximum projection");
        defaultComboBoxModel1.addElement("Standard Deviation projection");
        projectionMethodCB.setModel(defaultComboBoxModel1);
        zstackPanel.add(projectionMethodCB, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        projectionPanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        preprocessingPanel.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        useMacroCodeCheckBox = new JCheckBox();
        useMacroCodeCheckBox.setText("use macro code");
        panel3.add(useMacroCodeCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTHWEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        macroPanel = new JPanel();
        macroPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(macroPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 2, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        macroPanel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroArea = new JTextArea();
        macroArea.setRows(2);
        scrollPane1.setViewportView(macroArea);
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        panel3.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 2, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        panel3.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        subtractBackgroundCheckBox = new JCheckBox();
        subtractBackgroundCheckBox.setText("subtract background");
        preprocessingPanel.add(subtractBackgroundCheckBox, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 40), null, null, 0, false));
        sbgPanel = new JPanel();
        sbgPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        preprocessingPanel.add(sbgPanel, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("ball diameter");
        sbgPanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sbgPanel.add(sbgSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotDetectionPanel = new JPanel();
        spotDetectionPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(spotDetectionPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        spotDetectionPanel.setBorder(BorderFactory.createTitledBorder(null, "Spots detection parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        findMaximaCheckBox = new JCheckBox();
        findMaximaCheckBox.setText("find maxima");
        spotDetectionPanel.add(findMaximaCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdCheckBox = new JCheckBox();
        thresholdCheckBox.setText("threshold");
        spotDetectionPanel.add(thresholdCheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maximaPanel = new JPanel();
        maximaPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        spotDetectionPanel.add(maximaPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        maximaPanel.setBorder(BorderFactory.createTitledBorder(null, "Find maxima method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        prominenceLabel = new JLabel();
        prominenceLabel.setText("prominence");
        maximaPanel.add(prominenceLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maximaPanel.add(prominenceSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel = new JPanel();
        thresholdPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        spotDetectionPanel.add(thresholdPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        thresholdPanel.setBorder(BorderFactory.createTitledBorder(null, "Threshold method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        thresholdLabel = new JLabel();
        thresholdLabel.setText("threshold method");
        thresholdPanel.add(thresholdLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel.add(thresholdMethodCB, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minSizeLabel = new JLabel();
        minSizeLabel.setText("minimum area (pixels)");
        thresholdPanel.add(minSizeLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minSizeSpinner = new JSpinner();
        thresholdPanel.add(minSizeSpinner, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel.add(userThresholdSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useWatershedCheckBox = new JCheckBox();
        useWatershedCheckBox.setText("use watershed");
        thresholdPanel.add(useWatershedCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        darkBGCheckBox = new JCheckBox();
        darkBGCheckBox.setSelected(true);
        darkBGCheckBox.setText("dark BG");
        thresholdPanel.add(darkBGCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        prominenceSpinner = new JSpinner(new SpinnerNumberModel(100.0, 0.0, Integer.MAX_VALUE, 1));
        sbgSpinner = new JSpinner(new SpinnerNumberModel(100.0, 0.0, Integer.MAX_VALUE, 1));
        String[] tmp = AutoThresholder.getMethods();
        thresholdMethods = new String[tmp.length + 1];
        System.arraycopy(tmp, 0, thresholdMethods, 0, tmp.length);
        thresholdMethods[thresholdMethods.length - 1] = "user_defined";
        IJ.log("threshold methods:" + Arrays.toString(thresholdMethods));
        thresholdMethodCB = new JComboBox<>(thresholdMethods);
        userThresholdSpinner = new JSpinner(new SpinnerNumberModel(100.0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
    }
}
