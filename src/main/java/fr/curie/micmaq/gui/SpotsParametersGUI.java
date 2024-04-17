package fr.curie.micmaq.gui;

import fr.curie.micmaq.config.FieldOfViewProvider;
import fr.curie.micmaq.config.MeasureValue;
import fr.curie.micmaq.detectors.SpotDetector;
import fr.curie.micmaq.helpers.ImageToAnalyze;
import ij.Prefs;
import ij.measure.Measurements;
import ij.process.AutoThresholder;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

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
    private JToggleButton liveMaximaButton;
    private JToggleButton liveThresholdButton;

    protected int channel = -1;
    FieldOfViewProvider imageProvider;
    protected String proteinName;
    protected String[] thresholdMethods = null;

    SpotDetector spotDetectorLive;

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
                //darkBGCheckBox.setVisible(thresholdMethodCB.getSelectedIndex() == thresholdMethods.length - 1);
                if (spotDetectorLive != null) updateLiveThreshold();
            }
        });
        liveMaximaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean start = liveMaximaButton.getModel().isSelected();
                //liveButton.getModel().setPressed(start);
                if (start) {
                    spotDetectorLive = new SpotDetector(imageProvider.getImagePlus(imageProvider.getPreviewImage(), channel), proteinName);
                    if (isZStackCheckBox.isSelected())
                        spotDetectorLive.setzStackParameters((String) projectionMethodCB.getSelectedItem());
                    if(useMacroCodeCheckBox.isSelected()) spotDetectorLive.setPreprocessingMacro(macroArea.getText());
                    if(subtractBackgroundCheckBox.isSelected()) spotDetectorLive.setRollingBallSize((double)sbgSpinner.getValue());
                    spotDetectorLive.livePreviewFindMaxima((double) prominenceSpinner.getValue());
                } else {
                    if (spotDetectorLive != null) {
                        spotDetectorLive.endLivePreview();
                        spotDetectorLive = null;
                    }
                }
            }
        });
        prominenceSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (spotDetectorLive != null)
                    spotDetectorLive.livePreviewFindMaxima((double) prominenceSpinner.getValue());
            }
        });
        liveThresholdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean start = liveThresholdButton.getModel().isSelected();
                //liveButton.getModel().setPressed(start);
                if (start) {
                    spotDetectorLive = new SpotDetector(imageProvider.getImagePlus(imageProvider.getPreviewImage(), channel), proteinName);
                    updateLiveThreshold();
                } else {
                    if (spotDetectorLive != null) {
                        spotDetectorLive.endLivePreview();
                        spotDetectorLive = null;
                    }
                }
            }
        });
        userThresholdSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateLiveThreshold();
            }
        });
        minSizeSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateLiveThreshold();
            }
        });
        useWatershedCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateLiveThreshold();
            }
        });
        darkBGCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateLiveThreshold();
            }
        });
    }

    private void updateLiveThreshold() {
        if (spotDetectorLive == null) return;
        if (isZStackCheckBox.isSelected())
            spotDetectorLive.setzStackParameters((String) projectionMethodCB.getSelectedItem());
        if(useMacroCodeCheckBox.isSelected()) spotDetectorLive.setPreprocessingMacro(macroArea.getText());
        if(subtractBackgroundCheckBox.isSelected()) spotDetectorLive.setRollingBallSize((double)sbgSpinner.getValue());

        String thMethod = (String) thresholdMethodCB.getSelectedItem();
        double thValue = (thresholdMethodCB.getSelectedIndex() == thresholdMethodCB.getItemCount() - 1) ? (double) userThresholdSpinner.getValue() : Double.NaN;
        int minArea = (int) minSizeSpinner.getValue();
        boolean useWatershed = useWatershedCheckBox.isSelected();
        boolean dark = darkBGCheckBox.isSelected();
        spotDetectorLive.setSpotByThreshold(thMethod, dark ? thValue : -1.0E30D, dark ? 1.0E30D : thValue, minArea, useWatershed,dark, false);
        spotDetectorLive.livePreviewThreshold();
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
        liveThresholdButton.setEnabled(enabled);
    }

    private void setEnablingOfMaximaPanel(boolean enabled) {
        maximaPanel.setEnabled(enabled);
        prominenceLabel.setEnabled(enabled);
        prominenceSpinner.setEnabled(enabled);
        liveMaximaButton.setEnabled(enabled);
    }

    public MeasureValue getMeasure() {
        MeasureValue measureValue = new MeasureValue(false);
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

            measureValue.setSpotThreshold((String) (thresholdMethodCB.getSelectedItem()), minTh, maxTh, (Integer) minSizeSpinner.getValue(), useWatershedCheckBox.isSelected(),darkBGCheckBox.isSelected());
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
            result += "\nFind Spots by thresholding:";
            if (thresholdMethodCB.getSelectedIndex() == thresholdMethods.length - 1) {
                result += "\n\tUser defined threshold: " + userThresholdSpinner.getValue();
            } else {
                result += "\n\tAutomatic threshold method: " + thresholdMethodCB.getSelectedItem();
            }
            result += "\n\tDark background: " + darkBGCheckBox.isSelected();

            result += "\n\tMinimum spot diameter: " + (int) minSizeSpinner.getValue();
            result += "\n\tWatershed: " + (useWatershedCheckBox.isSelected() ? "yes" : "no");
        }

        return result;
    }

    public void setParameters(ArrayList<String> params) {
        boolean q = true;
        boolean m = false;
        String macro = "";
        findMaximaCheckBox.setSelected(false);
        thresholdCheckBox.setSelected(false);
        subtractBackgroundCheckBox.setSelected(false);
        isZStackCheckBox.setSelected(false);
        useMacroCodeCheckBox.setSelected(false);

        for (int i = 0; i < params.size(); i++) {
            //System.out.println("#spots: "+params.get(i));
            if (q) {
                if (params.get(i).startsWith("Measurements")) {
                    m = false;
                    //System.out.println("stop macro");
                }
                if (m) {
                    macro += params.get(i) + "\n";
                    //System.out.println("add text to macro " + params.get(i));
                    //System.out.println("macro becomes :\n" + macro);
                    macroArea.setText(macro);
                } else {

                    if (params.get(i).startsWith("Macro:")) {
                        m = true;
                        //System.out.println("start macro and activate in GUI");
                        useMacroCodeCheckBox.setSelected(true);
                        macroArea.setVisible(true);
                    }

                    if (params.get(i).startsWith("Projection")) {
                        isZStackCheckBox.setSelected(true);
                        //System.out.println("change projection");
                    }
                    if (params.get(i).startsWith("Subtract Background:")) {
                        subtractBackgroundCheckBox.setSelected(true);
                        sbgSpinner.setValue(new Double(params.get(i).split(": ")[1].split(" ")[1]));
                    }

                    if (params.get(i).endsWith("Local Maxima:")) {
                        //System.out.println("spots using local maxima");
                        findMaximaCheckBox.setSelected(true);
                        prominenceSpinner.setValue(new Double(params.get(i + 1).split(": ")[1]));
                    }
                    if (params.get(i).endsWith("thresholding:")) {
                        //System.out.println("spots using thresholds");
                        thresholdCheckBox.setSelected(true);
                        int offset = 1;
                        String model = params.get(i + offset).split(": ")[1];
                        //System.out.println("model to put = "+model);
                        int index = -1;
                        for (int ind = 0; ind < thresholdMethodCB.getItemCount(); ind++) {
                            if (thresholdMethodCB.getItemAt(ind).equals(model)) index = ind;
                        }
                        if (index >= 0) thresholdMethodCB.setSelectedIndex(index);
                        offset++;
                        if (params.get(i + offset).startsWith("User defined threshold:")) {
                            String tmp = params.get(i + offset).split(": ")[1];
                            userThresholdSpinner.setValue(new Double(params.get(i + offset).split(": ")[1]));
                            offset++;
                        }
                        darkBGCheckBox.setSelected(params.get(i + offset).endsWith("true"));
                        offset++;
                        minSizeSpinner.setValue(new Integer(params.get(i + offset).split(": ")[1]));
                        offset++;
                        useWatershedCheckBox.setSelected(params.get(i + offset).endsWith("yes"));
                    }
                }

            }
            if (params.get(i).startsWith("Quantification  Parameters")) q = false;

            mainPanel.repaint();
        }//end for
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
        maximaPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        spotDetectionPanel.add(maximaPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        maximaPanel.setBorder(BorderFactory.createTitledBorder(null, "Find maxima method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        prominenceLabel = new JLabel();
        prominenceLabel.setText("prominence");
        maximaPanel.add(prominenceLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maximaPanel.add(prominenceSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        liveMaximaButton = new JToggleButton();
        liveMaximaButton.setText("live testing");
        maximaPanel.add(liveMaximaButton, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel = new JPanel();
        thresholdPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 4, new Insets(0, 0, 0, 0), -1, -1));
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
        liveThresholdButton = new JToggleButton();
        liveThresholdButton.setText("live testing");
        thresholdPanel.add(liveThresholdButton, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        prominenceSpinner = new JSpinner(new SpinnerNumberModel(100.0, 0.0, Integer.MAX_VALUE, 1));
        sbgSpinner = new JSpinner(new SpinnerNumberModel(100.0, 0.0, Integer.MAX_VALUE, 1));
        String[] tmp = AutoThresholder.getMethods();
        thresholdMethods = new String[tmp.length + 1];
        System.arraycopy(tmp, 0, thresholdMethods, 0, tmp.length);
        thresholdMethods[thresholdMethods.length - 1] = "user_defined";
        //IJ.log("threshold methods:" + Arrays.toString(thresholdMethods));
        thresholdMethodCB = new JComboBox<>(thresholdMethods);
        userThresholdSpinner = new JSpinner(new SpinnerNumberModel(100.0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
    }
}
