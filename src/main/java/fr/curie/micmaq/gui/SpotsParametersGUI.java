package fr.curie.micmaq.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import fr.curie.micmaq.config.FieldOfViewProvider;
import fr.curie.micmaq.config.MeasureValue;
import fr.curie.micmaq.detectors.SpotDetector;
import fr.curie.micmaq.helpers.ImageToAnalyze;
import ij.IJ;
import ij.ImagePlus;
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
    private JCheckBox showHideCheckBox;
    private JCheckBox spotiflowCheckBox;
    private JComboBox spotiflowModelComboBox;
    private JTextField spotiflowPathTextField;
    private JSpinner spotiflowThresholdSpinner1;
    private JPanel spotiflowPanel;
    private JCheckBox showHideCheckBox1;
    private JToggleButton liveSpotiflowButton;
    private JSpinner spotiflowScaleSpinner;
    private JSpinner spotiflowMinDistSpinner1;
    private JSpinner spotiflowSubPixRadiusSpinner1;

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
                    if (useMacroCodeCheckBox.isSelected()) spotDetectorLive.setPreprocessingMacro(macroArea.getText());
                    if (subtractBackgroundCheckBox.isSelected())
                        spotDetectorLive.setRollingBallSize((double) sbgSpinner.getValue());
                    updateLiveMaxima();
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
                updateLiveMaxima();
            }
        });
        showHideCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (spotDetectorLive != null) {
                    spotDetectorLive.showHideToggle(showHideCheckBox.isSelected());
                    //spotDetectorLive.livePreviewFindMaxima((double) prominenceSpinner.getValue(), showHideCheckBox.isSelected());
                }
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

        spotiflowCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnablingOfSpotiFlowPanel(spotiflowCheckBox.isSelected());
            }
        });


        liveSpotiflowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IJ.log("live spotiflow");
                boolean start = liveSpotiflowButton.getModel().isSelected();
                if (start) {
                    if (!isSpotiflowModelValid()) {
                        IJ.error("Spotiflow model not valid", "please check if you are using a 2D or 3D model and if your image is a 2D or 3D image ");
                        return;
                    }
                    IJ.log("start live spotiflow");
                    ProgressMonitor pm = new ProgressMonitor(null, "Live spotiFlow", "Please wait...", 0, 100);
                    pm.setMillisToDecideToPopup(1);
                    pm.setMillisToPopup(1);
                    IJ.showStatus("update detection by spotiflow");
                    spotDetectorLive = new SpotDetector(imageProvider.getImagePlus(imageProvider.getPreviewImage(), channel), proteinName);
                    if (isZStackCheckBox.isSelected())
                        spotDetectorLive.setzStackParameters((String) projectionMethodCB.getSelectedItem());
                    if (useMacroCodeCheckBox.isSelected()) spotDetectorLive.setPreprocessingMacro(macroArea.getText());
                    if (subtractBackgroundCheckBox.isSelected())
                        spotDetectorLive.setRollingBallSize((double) sbgSpinner.getValue());
                    IJ.log("spotDetectorLive created should now run preview");
                    pm.setProgress(1);
                    String model = spotiflowModelComboBox.getSelectedItem().toString();
                    boolean pretrained = true;
                    if (spotiflowModelComboBox.getSelectedIndex() == spotiflowModelComboBox.getItemCount() - 1) {
                        pretrained = false;
                        model = spotiflowPathTextField.getText();
                    }
                    spotDetectorLive.livePreviewSpotiflow(model, pretrained, (double) spotiflowThresholdSpinner1.getValue(), (double) spotiflowScaleSpinner.getValue(), (int) spotiflowMinDistSpinner1.getValue(), (int) spotiflowSubPixRadiusSpinner1.getValue(), showHideCheckBox1.isSelected());
                    pm.setProgress(100);
                    pm.close();
                } else {
                    IJ.log("end live spotiflow");
                    if (spotDetectorLive != null) {
                        spotDetectorLive.endLivePreview();
                        spotDetectorLive = null;
                    }
                }

            }
        });
        showHideCheckBox1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (spotDetectorLive != null) spotDetectorLive.showHideToggle(showHideCheckBox1.isSelected());
                //updateLiveSpotiflow();
            }
        });
        spotiflowThresholdSpinner1.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateLiveSpotiflow();
            }
        });
        spotiflowScaleSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateLiveSpotiflow();
            }
        });
        spotiflowMinDistSpinner1.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateLiveSpotiflow();
            }
        });
        spotiflowSubPixRadiusSpinner1.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateLiveSpotiflow();
            }
        });
    }

    private void updateLiveMaxima() {
        if (spotDetectorLive == null) return;
        spotDetectorLive.livePreviewFindMaxima((double) prominenceSpinner.getValue(), showHideCheckBox.isSelected());
    }


    private void updateLiveThreshold() {
        if (spotDetectorLive == null) return;
        if (isZStackCheckBox.isSelected())
            spotDetectorLive.setzStackParameters((String) projectionMethodCB.getSelectedItem());
        if (useMacroCodeCheckBox.isSelected()) spotDetectorLive.setPreprocessingMacro(macroArea.getText());
        if (subtractBackgroundCheckBox.isSelected())
            spotDetectorLive.setRollingBallSize((double) sbgSpinner.getValue());

        String thMethod = (String) thresholdMethodCB.getSelectedItem();
        double thValue = (thresholdMethodCB.getSelectedIndex() == thresholdMethodCB.getItemCount() - 1) ? (double) userThresholdSpinner.getValue() : Double.NaN;
        int minArea = (int) minSizeSpinner.getValue();
        boolean useWatershed = useWatershedCheckBox.isSelected();
        boolean dark = darkBGCheckBox.isSelected();
        spotDetectorLive.setSpotByThreshold(thMethod, dark ? thValue : -1.0E30D, dark ? 1.0E30D : thValue, minArea, useWatershed, dark, false);
        spotDetectorLive.livePreviewThreshold();
    }

    public void updateLiveSpotiflow() {
        if (spotDetectorLive != null) {
            IJ.showStatus("update detection by spotiflow");
            String model = spotiflowModelComboBox.getSelectedItem().toString();
            boolean pretrained = true;
            if (spotiflowModelComboBox.getSelectedIndex() == spotiflowModelComboBox.getItemCount() - 1) {
                pretrained = false;
                model = spotiflowPathTextField.getText();
            }
            spotDetectorLive.livePreviewSpotiflow(model, pretrained, (double) spotiflowThresholdSpinner1.getValue(), (double) spotiflowScaleSpinner.getValue(), (int) spotiflowMinDistSpinner1.getValue(), (int) spotiflowSubPixRadiusSpinner1.getValue(), showHideCheckBox1.isSelected());
        }
    }

    public boolean isSpotiflowModelValid() {
        if (!spotiflowCheckBox.isSelected()) return true;
        if (spotiflowModelComboBox.getSelectedIndex() < spotiflowModelComboBox.getItemCount() - 1) {
            IJ.log("spotiflow model is " + (spotiflowModelComboBox.getSelectedItem().toString()) + " is 3D processing " + is3DProcessing());
            if (spotiflowModelComboBox.getSelectedItem().toString().endsWith("3d")) {
                if (is3DProcessing()) return true;
                else return false;
            } else {
                if (is3DProcessing()) return false;
                else return true;
            }
        }
        return true;
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

    private void setEnablingOfSpotiFlowPanel(boolean enabled) {
        spotiflowPanel.setEnabled(enabled);
        spotiflowModelComboBox.setEnabled(enabled);
        spotiflowThresholdSpinner1.setEnabled(enabled);
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

            measureValue.setSpotThreshold((String) (thresholdMethodCB.getSelectedItem()), minTh, maxTh, (Integer) minSizeSpinner.getValue(), useWatershedCheckBox.isSelected(), darkBGCheckBox.isSelected());
        }
        if (findMaximaCheckBox.isSelected()) {
            if (is3DProcessing()) {
                IJ.log("3D processing, maxima detection will be done on each slice");
                measureValue.setSpotFindMaxima3D(0, 1.5, 1.5, (double) prominenceSpinner.getValue());
            } else {
                measureValue.setSpotFindMaxima((Double) prominenceSpinner.getValue());
            }
        }
        if (spotiflowCheckBox.isSelected()) {
            String model = spotiflowModelComboBox.getSelectedItem().toString();
            boolean pretrained = true;
            if (spotiflowModelComboBox.getSelectedIndex() == spotiflowModelComboBox.getItemCount() - 1) {
                pretrained = false;
                model = spotiflowPathTextField.getText();
            }
            measureValue.setSpotFindSpotiflow(model, pretrained, (Double) spotiflowThresholdSpinner1.getValue());
            if ((double) spotiflowScaleSpinner.getValue() != 1.0)
                measureValue.setSpotiflowScale((Double) spotiflowScaleSpinner.getValue());
            if ((int) spotiflowMinDistSpinner1.getValue() > 0)
                measureValue.setSpotiflowMinDistance((Integer) spotiflowMinDistSpinner1.getValue());
            if ((int) spotiflowSubPixRadiusSpinner1.getValue() > 0)
                measureValue.setSpotiflowSubpixRadius((Integer) spotiflowSubPixRadiusSpinner1.getValue());
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
        int maxSlices = imageProvider.getFirstValidFieldOfView().getNSlices(channel);
        int firstSlice = (int) Prefs.get("MICMAQ.firstSliceSpot_" + id, 1);
        int lastSlice = (int) Prefs.get("MICMAQ.lastSliceSpot_" + id, maxSlices);
        ImageToAnalyze.assertSlices(maxSlices, firstSlice, lastSlice, sliceMinSpinner, sliceMaxSpinner);

//        Macro
        useMacroCodeCheckBox.setSelected(Prefs.get("MICMAQ.useMacroSpot_" + id, false));
        if (!useMacroCodeCheckBox.isSelected()) macroPanel.setVisible(false);
        macroArea.setText(Prefs.get("MICMAQ.macroSpot_" + id, " "));

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
        //spotiflow
        boolean spotiflowEnable = Prefs.get("MICMAQ.spotiflowSelectedSpot_" + id, false);
        spotiflowCheckBox.setSelected(spotiflowEnable);
        setEnablingOfSpotiFlowPanel(spotiflowEnable);
        spotiflowModelComboBox.setSelectedItem(Prefs.get("MICMAQ.spotiflowModelSpot_" + id, "general"));
        spotiflowThresholdSpinner1.setValue((double) Prefs.get("MICMAQ.spotiflowThresholdSpot_" + id, 0.5));
        spotiflowScaleSpinner.setValue((double) Prefs.get("MICMAQ.spotiflowScaleSpot_", 1.0));
        spotiflowMinDistSpinner1.setValue((int) Prefs.get("MICMAQ.spotiflowMinDistSpot_", 0));
        spotiflowSubPixRadiusSpinner1.setValue((int) Prefs.get("MICMAQ.spotiflowSubPixRadiusSpot_", 0));


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
            result += "\n\tMacro:\n" + macroArea.getText() + "\n\tend Macro";
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
        if (spotiflowCheckBox.isSelected()) {
            result += "\nFind Spots by spotiflow:";
            result += "\n\tSpotiflow model: " + spotiflowModelComboBox.getSelectedItem();
            result += "\n\tSpotiflow threshold: " + spotiflowThresholdSpinner1.getValue();
            if ((double) spotiflowScaleSpinner.getValue() != 1.0)
                result += "\n\tSpotiflow scale: " + spotiflowScaleSpinner.getValue();
            if ((int) spotiflowMinDistSpinner1.getValue() > 0)
                result += "\n\tSpotiflow min distance: " + spotiflowMinDistSpinner1.getValue();
            if ((int) spotiflowSubPixRadiusSpinner1.getValue() > 0)
                result += "\n\tSpotiflow subpix radius: " + spotiflowSubPixRadiusSpinner1.getValue();
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
        isZStackCheckBox.setSelected(true);
        isZStackCheckBox.doClick();
        useMacroCodeCheckBox.setSelected(true);
        useMacroCodeCheckBox.doClick();

        for (int i = 0; i < params.size(); i++) {
            //System.out.println("#spots: "+params.get(i));
            if (q) {
                if (params.get(i).startsWith("Measurements")) {
                    m = false;
                    //System.out.println("stop macro");
                }
                if (m) {
                    if (params.get(i).startsWith("end Macro")) {
                        m = false;
                    } else {
                        macro += params.get(i) + "\n";
                        //System.out.println("add text to macro " + params.get(i));
                        //System.out.println("macro becomes :\n" + macro);
                        macroArea.setText(macro);
                        macroArea.setVisible(true);
                    }
                } else {

                    if (params.get(i).startsWith("Macro:")) {
                        m = true;
                        //System.out.println("start macro and activate in GUI");
                        useMacroCodeCheckBox.setSelected(false);
                        useMacroCodeCheckBox.doClick();
                        macroArea.setVisible(true);
                    }

                    if (params.get(i).startsWith("Projection")) {
                        isZStackCheckBox.setSelected(false);
                        isZStackCheckBox.doClick();
                        String model = params.get(i).split(": ")[1];
                        projectionMethodCB.setSelectedItem(model);
                        //System.out.println("change projection");
                    }
                    if (params.get(i).startsWith("Subtract Background:")) {
                        subtractBackgroundCheckBox.setSelected(false);
                        subtractBackgroundCheckBox.doClick();
                        sbgSpinner.setValue(Double.parseDouble(params.get(i).split(": ")[1].split(" ")[1]));
                    }

                    if (params.get(i).endsWith("Local Maxima:")) {
                        //System.out.println("spots using local maxima");
                        findMaximaCheckBox.setSelected(false);
                        findMaximaCheckBox.doClick();
                        prominenceSpinner.setValue(Double.parseDouble(params.get(i + 1).split(": ")[1]));
                    }
                    if (params.get(i).endsWith("thresholding:")) {
                        //System.out.println("spots using thresholds");
                        thresholdCheckBox.setSelected(false);
                        thresholdCheckBox.doClick();
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
                            userThresholdSpinner.setValue(Double.parseDouble(params.get(i + offset).split(": ")[1]));
                            offset++;
                        }
                        darkBGCheckBox.setSelected(params.get(i + offset).endsWith("true"));
                        offset++;
                        minSizeSpinner.setValue(Integer.parseInt(params.get(i + offset).split(": ")[1]));
                        offset++;
                        useWatershedCheckBox.setSelected(params.get(i + offset).endsWith("yes"));
                    }
                    if (params.get(i).startsWith("Spotiflow")) {
                        spotiflowCheckBox.setSelected(false);
                        spotiflowCheckBox.doClick();
                        String model = params.get(i + 1).split(": ")[1];
                        spotiflowModelComboBox.setSelectedItem(model);
                        spotiflowThresholdSpinner1.setValue(Double.parseDouble(params.get(i + 2).split(": ")[1]));
                        spotiflowScaleSpinner.setValue(Double.parseDouble(params.get(i + 3).split(": ")[1]));
                        spotiflowMinDistSpinner1.setValue(Integer.parseInt(params.get(i + 4).split(": ")[1]));
                        spotiflowSubPixRadiusSpinner1.setValue(Integer.parseInt(params.get(i + 5).split(": ")[1]));
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

        Prefs.set("MICMAQ.spotiflowSelectedSpot_" + id, spotiflowCheckBox.isSelected());
        Prefs.set("MICMAQ.spotiflowModelSpot_" + id, (String) spotiflowModelComboBox.getSelectedItem());
        Prefs.set("MICMAQ.spotiflowThresholdSpot_" + id, (double) spotiflowThresholdSpinner1.getValue());
        Prefs.set("MICMAQ.spotiflowScaleSpot_" + id, (double) spotiflowScaleSpinner.getValue());
        Prefs.set("MICMAQ.spotiflowMinDistSpot_" + id, (int) spotiflowMinDistSpinner1.getValue());
        Prefs.set("MICMAQ.spotiflowSubpixRadiusSpot_" + id, (int) spotiflowSubPixRadiusSpinner1.getValue());

    }

    public boolean is3DProcessing() {
        ImagePlus imptmp = imageProvider.getImagePlus(imageProvider.getPreviewImage(), channel);
        if (imptmp.getNSlices() == 1) return false;
        if (isZStackCheckBox.isSelected()) return false;
        return true;
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
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(mainPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(800, 600), null, 2, false));
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Spot detection parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        preprocessingPanel = new JPanel();
        preprocessingPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(preprocessingPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        preprocessingPanel.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        preprocessingPanel.add(panel2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, new Dimension(-1, 61), null, null, 0, false));
        isZStackCheckBox = new JCheckBox();
        isZStackCheckBox.setText("is Z-stack?");
        panel2.add(isZStackCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projectionPanel = new JPanel();
        projectionPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(projectionPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        zstackPanel = new JPanel();
        zstackPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        projectionPanel.add(zstackPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("method of projection");
        zstackPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("choose slices to use");
        zstackPanel.add(chooseSlicesToUseCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 28), null, null, 0, false));
        sliceMinSpinner = new JSpinner();
        zstackPanel.add(sliceMinSpinner, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sliceMaxSpinner = new JSpinner();
        zstackPanel.add(sliceMaxSpinner, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projectionMethodCB = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Maximum projection");
        defaultComboBoxModel1.addElement("Standard Deviation projection");
        defaultComboBoxModel1.addElement("Sum Slices");
        defaultComboBoxModel1.addElement("Best Focal slice");
        defaultComboBoxModel1.addElement("Maximum of 3 best Focal slices");
        projectionMethodCB.setModel(defaultComboBoxModel1);
        zstackPanel.add(projectionMethodCB, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        projectionPanel.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        preprocessingPanel.add(panel3, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        useMacroCodeCheckBox = new JCheckBox();
        useMacroCodeCheckBox.setText("use macro code");
        panel3.add(useMacroCodeCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        macroPanel = new JPanel();
        macroPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(macroPanel, new GridConstraints(0, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        macroPanel.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroArea = new JTextArea();
        macroArea.setRows(2);
        scrollPane1.setViewportView(macroArea);
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel3.add(spacer3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        subtractBackgroundCheckBox = new JCheckBox();
        subtractBackgroundCheckBox.setText("subtract background");
        preprocessingPanel.add(subtractBackgroundCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 40), null, null, 0, false));
        sbgPanel = new JPanel();
        sbgPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        preprocessingPanel.add(sbgPanel, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("ball diameter");
        sbgPanel.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sbgPanel.add(sbgSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotDetectionPanel = new JPanel();
        spotDetectionPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(spotDetectionPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        spotDetectionPanel.setBorder(BorderFactory.createTitledBorder(null, "Spots detection parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        findMaximaCheckBox = new JCheckBox();
        findMaximaCheckBox.setText("find maxima");
        spotDetectionPanel.add(findMaximaCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdCheckBox = new JCheckBox();
        thresholdCheckBox.setText("threshold");
        spotDetectionPanel.add(thresholdCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maximaPanel = new JPanel();
        maximaPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        spotDetectionPanel.add(maximaPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        maximaPanel.setBorder(BorderFactory.createTitledBorder(null, "Find maxima method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        prominenceLabel = new JLabel();
        prominenceLabel.setText("prominence");
        maximaPanel.add(prominenceLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maximaPanel.add(prominenceSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        liveMaximaButton = new JToggleButton();
        liveMaximaButton.setText("live testing");
        maximaPanel.add(liveMaximaButton, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showHideCheckBox = new JCheckBox();
        showHideCheckBox.setSelected(true);
        showHideCheckBox.setText("show/hide");
        maximaPanel.add(showHideCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel = new JPanel();
        thresholdPanel.setLayout(new GridLayoutManager(3, 4, new Insets(0, 0, 0, 0), -1, -1));
        spotDetectionPanel.add(thresholdPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        thresholdPanel.setBorder(BorderFactory.createTitledBorder(null, "Threshold method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        thresholdLabel = new JLabel();
        thresholdLabel.setText("threshold method");
        thresholdPanel.add(thresholdLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel.add(thresholdMethodCB, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minSizeLabel = new JLabel();
        minSizeLabel.setText("minimum area (pixels)");
        thresholdPanel.add(minSizeLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minSizeSpinner = new JSpinner();
        thresholdPanel.add(minSizeSpinner, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel.add(userThresholdSpinner, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useWatershedCheckBox = new JCheckBox();
        useWatershedCheckBox.setText("use watershed");
        thresholdPanel.add(useWatershedCheckBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        darkBGCheckBox = new JCheckBox();
        darkBGCheckBox.setSelected(true);
        darkBGCheckBox.setText("dark BG");
        thresholdPanel.add(darkBGCheckBox, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        liveThresholdButton = new JToggleButton();
        liveThresholdButton.setText("live testing");
        thresholdPanel.add(liveThresholdButton, new GridConstraints(2, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotiflowCheckBox = new JCheckBox();
        spotiflowCheckBox.setText("spotiflow");
        spotDetectionPanel.add(spotiflowCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotiflowPanel = new JPanel();
        spotiflowPanel.setLayout(new GridLayoutManager(6, 3, new Insets(0, 0, 0, 0), -1, -1));
        spotiflowPanel.setEnabled(false);
        spotDetectionPanel.add(spotiflowPanel, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        spotiflowPanel.setBorder(BorderFactory.createTitledBorder(null, "Spotiflow method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("model");
        spotiflowPanel.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotiflowModelComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("general");
        defaultComboBoxModel2.addElement("hybiss");
        defaultComboBoxModel2.addElement("synth_complex");
        defaultComboBoxModel2.addElement("fluo_live");
        defaultComboBoxModel2.addElement("synth_3d");
        defaultComboBoxModel2.addElement("smfish_3d");
        defaultComboBoxModel2.addElement("own_model");
        spotiflowModelComboBox.setModel(defaultComboBoxModel2);
        spotiflowPanel.add(spotiflowModelComboBox, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("path to own model");
        spotiflowPanel.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotiflowPathTextField = new JTextField();
        spotiflowPanel.add(spotiflowPathTextField, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("probability threshold");
        spotiflowPanel.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotiflowPanel.add(spotiflowThresholdSpinner1, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showHideCheckBox1 = new JCheckBox();
        showHideCheckBox1.setSelected(true);
        showHideCheckBox1.setText("show/hide");
        spotiflowPanel.add(showHideCheckBox1, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        liveSpotiflowButton = new JToggleButton();
        liveSpotiflowButton.setText("live testing");
        spotiflowPanel.add(liveSpotiflowButton, new GridConstraints(3, 2, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("scale factor");
        spotiflowPanel.add(label6, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotiflowPanel.add(spotiflowScaleSpinner, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("min dist");
        spotiflowPanel.add(label7, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotiflowPanel.add(spotiflowMinDistSpinner1, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("subPixel radius");
        spotiflowPanel.add(label8, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotiflowPanel.add(spotiflowSubPixRadiusSpinner1, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

        spotiflowThresholdSpinner1 = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 1.0, 0.01));
        spotiflowScaleSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 100, 0.01));
        spotiflowMinDistSpinner1 = new JSpinner(new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1));
        spotiflowSubPixRadiusSpinner1 = new JSpinner(new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1));
    }
}
