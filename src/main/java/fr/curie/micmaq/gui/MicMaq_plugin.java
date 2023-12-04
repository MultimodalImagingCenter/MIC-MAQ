package fr.curie.micmaq.gui;

import com.jgoodies.common.collect.ArrayListModel;
import fr.curie.micmaq.config.*;
import fr.curie.micmaq.detectors.Experiment;
import fr.curie.micmaq.helpers.ImageToAnalyze;
import fr.curie.micmaq.helpers.MeasureCalibration;
import fr.curie.micmaq.segment.SegmentationParameters;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.WindowOrganizer;
import ij.plugin.frame.RoiManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MicMaq_plugin extends JFrame implements PlugIn {
    private JTabbedPane TabsPanel;
    private JPanel LogoPanel;
    private JPanel ConfigurationPanel;
    private JComboBox<MeasureCalibration> calibrationCombo;
    private JButton addNewCalibrationButton;
    private JButton launchButton;
    private JButton cancelButton;
    private JSpinner nrProteinSpinner;
    private JButton browseButton;
    private JButton previewButton;
    private JTextArea chosenDirectoryTextArea;
    private JPanel rootPane;
    private JTextField filePatternTextField;
    private JPanel channelsDisplay;
    private JSpinner PreviewSpinner;
    private JLabel numberOfImageLabel;
    private JButton showImagesButton;
    private JButton reorganiseDataButton;
    private JTree imagesTree;
    private JPanel globalPanel;
    private JScrollPane TreeJScroll;
    private JScrollPane channelScroll;
    private JButton previewCurrentStepButton;


    FieldOfViewProvider provider;
    ArrayList<ChannelPanel> channelPanels;
    int correctNbChannels;
    SegmentationParametersGUI nucleiParam;
    SegmentationParametersGUI cellParam;
    ArrayList<SpotsParametersGUI> spotPanels;
    QuantificationParametersGUI quantifPanel;

    ResultsTable cellResults;
    ResultsTable nucleusResults;
    String workingDirectory;

    boolean resized = false;
    int sizeflag = 0;

    ArrayList<String> patterns = null;
    String filePattern = null;
    String path_shorten = null;

    public MicMaq_plugin() {
        try {
            boolean nimbusFound = false;
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                System.out.println(info.getName());
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    System.out.println("set Look and feel to Nimbus");
                    nimbusFound = true;
                    //break;
                }
            }
            if (!nimbusFound) {
                System.out.println("set Look and feel to System: " + UIManager.getSystemLookAndFeelClassName());
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }
        Locale.setDefault(Locale.ENGLISH);

        $$$setupUI$$$();


        ((JSpinner.DefaultEditor) PreviewSpinner.getEditor()).getTextField().setColumns(4);
        ((SpinnerNumberModel) PreviewSpinner.getModel()).setMinimum(0);

        addNewCalibrationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewCalibrationPanel calib = new NewCalibrationPanel((DefaultComboBoxModel<MeasureCalibration>) calibrationCombo.getModel());
                calib.run();
            }
        });
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                File directory = chooseDirectory(rootPane);
                //provider = new ImageProvider(directory.getAbsolutePath(), filePatternTextField.getText().trim());
                if (directory != null) {
                    provider = new FieldOfViewProvider(directory.getAbsolutePath());
                    provider.parseDirectory(filePatternTextField.getText());
                    ((ImagesTree) imagesTree).setFieldOfViewProvider(provider);
                    ((SpinnerNumberModel) PreviewSpinner.getModel()).setValue(0);
                    ((SpinnerNumberModel) PreviewSpinner.getModel()).setMaximum(provider.getNbFielOfView());
                    String path = directory.getAbsolutePath();
                    workingDirectory = path;
//                Shorten path to show only the 2 last subdirectories
                    if (path.split("\\\\").length > 2) {
                        path_shorten = path.substring(path.substring(0, path.lastIndexOf("\\")).lastIndexOf("\\"));
                        chosenDirectoryTextArea.setText("..." + path_shorten);
                    } else if (path.split("/").length > 2) {
                        String path_shorten = path.substring(path.substring(0, path.lastIndexOf("/")).lastIndexOf("/"));
                        chosenDirectoryTextArea.setText("..." + path_shorten);
                    } else {
                        chosenDirectoryTextArea.setText(path);
                    }

                }
                updateChannels();
                channelDefinitionChange(true);
                checkCalibrationFromImages();
                if (!resized) {
                    pack();
                    resized = false;
                }
            }
        });

        channelsDisplay.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("channel")) {
                    System.out.println("change channel definition value=" + evt.getNewValue());
                    channelDefinitionChange(((Long) evt.getNewValue()).intValue() == 2);
                }
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //stop running experiments

                //close window
                dispose();
            }
        });
        previewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> previewAction()).start();
            }
        });
        previewCurrentStepButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> previewStep()).start();
            }
        });
        launchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //save parameters
                createParametersFile();
                //run on all images
                new Thread(() -> runAllExperiments()).start();
                Prefs.savePreferences();
            }
        });
        filePatternTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (provider != null) {
                    filePattern = filePatternTextField.getText();
                    provider.parseDirectory(filePattern);
                    updateChannels();
                    channelDefinitionChange(true);
                    ((ImagesTree) imagesTree).updateTree();
                    if (!resized) {
                        pack();
                        resized = false;
                    }
                }
            }
        });
        showImagesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayPreviewImages();
            }
        });
        reorganiseDataButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RedefineChannelsDialog dialog = new RedefineChannelsDialog();
                dialog.pack();
                dialog.setVisible(true);
                patterns = dialog.getChannelPatterns();
                if (patterns == null) return;
                provider.reorganiseFiles(filePatternTextField.getText(), patterns);
                ((ImagesTree) imagesTree).updateTree();
                updateChannels();
                channelDefinitionChange(true);
                if (!resized) {
                    pack();
                    resized = false;
                }
            }
        });

        TabsPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                previewCurrentStepButton.setEnabled(false);
                JPanel current = (JPanel) TabsPanel.getSelectedComponent();
                if (nucleiParam != null && current == nucleiParam.getMainPanel()) {
                    previewCurrentStepButton.setEnabled(true);
                } else if (cellParam != null && current == cellParam.getMainPanel()) {
                    previewCurrentStepButton.setEnabled(true);
                } else if (spotPanels != null && spotPanels.size() > 0) {
                    for (SpotsParametersGUI sp : spotPanels) {
                        if (sp != null && current == sp.getMainPanel()) {
                            previewCurrentStepButton.setEnabled(true);
                        }
                    }
                }
            }
        });


    }

    public int getNucleiChannel() {
        for (int i = 0; i < channelPanels.size(); i++) {
            ChannelPanel cp = channelPanels.get(i);
            if (cp.isNuclei()) {
                return i + 1;
            }
        }
        return -1;
    }

    public int getCellChannel() {
        for (int i = 0; i < channelPanels.size(); i++) {
            ChannelPanel cp = channelPanels.get(i);
            if (cp.isCell()) {
                return i + 1;
            }
        }
        return -1;
    }

    protected void channelDefinitionChange(boolean autoSelectPanel) {
        //TODO afficher les tabulations comme il faut!
        System.out.println("should do something FIRE from channelPanel");
        int countN = 0;
        int countC = 0;
        int countQ = 0;
        int countQbefore = 0;
        ArrayList<String> names = new ArrayList<>(channelPanels.size());
        //count each categories
        for (int i = 0; i < channelPanels.size(); i++) {
            ChannelPanel cp = channelPanels.get(i);
            if (cp.isNuclei()) {
                countN++;
            }
            if (cp.isCell()) {
                countC++;
            }
            if (cp.isSpot()) {
                countQ++;
            }
            if (spotPanels != null && spotPanels.get(i) != null) countQbefore++;
            names.add(cp.getProteinName());
        }
        //remove things
        if (countN == 0) {
            provider.setNucleiChannel(-1);
            nucleiParam = null;
        }
        if (countC == 0) {
            provider.setCellChannel(-1);
            cellParam = null;
        }
        //if (spotPanels != null && spotPanels.size() != channelPanels.size()) spotPanels = null;
        if (countQ < 0) spotPanels = null;
        //add Tabs
        int activePanel = -1;
        int count = 0;
        for (int i = 0; i < channelPanels.size(); i++) {
            ChannelPanel cp = channelPanels.get(i);
            if (cp.isNuclei()) {
                provider.setNucleiChannel(i + 1);
                if (nucleiParam == null) {
                    nucleiParam = new SegmentationParametersGUI(provider, SegmentationParametersGUI.NUCLEI);
                    activePanel = i;
                }
                if(cellParam!=null){
                    cellParam.setType(SegmentationParametersGUI.CELL_CYTO);
                }
                count++;
                System.out.println("a nuclei param should display");
            }
            if (cp.isCell()) {
                provider.setCellChannel(i + 1);
                if (cellParam == null) {
                    cellParam = new SegmentationParametersGUI(provider,
                            (countN > 0) ? SegmentationParametersGUI.CELL_CYTO : SegmentationParametersGUI.CELLS);
                    activePanel = i;
                }else{
                    if(nucleiParam!=null){
                        cellParam.setType(SegmentationParametersGUI.CELL_CYTO);
                    }else{
                        cellParam.setType(SegmentationParametersGUI.CELLS);
                    }
                }
                count++;
                System.out.println("a cell param should display");
            }
            if (cp.isSpot()) {
                if (spotPanels != null)
                    System.out.println("#" + i + " is spot : " + spotPanels + "(" + spotPanels.size() + ") should be " + channelPanels.size());
                else {
                    spotPanels = new ArrayListModel<>(channelPanels.size());
                    for (int j = 0; j < channelPanels.size(); j++) spotPanels.add(null);
                }
                if (spotPanels.get(i) == null) {
                    SpotsParametersGUI tmp = new SpotsParametersGUI(provider, i + 1);
                    tmp.setProteinName(cp.getProteinName());
                    spotPanels.set(i, tmp);
                    activePanel = i;
                }
            } else {
                if (spotPanels != null) {
                    spotPanels.set(i, null);
                }
            }
        }
        if (quantifPanel == null){
            quantifPanel = new QuantificationParametersGUI(provider);
        }else{

            provider.setChannelsUserName(names);
            quantifPanel.updateComboCheckbox();
        }
        updateTabsPanel();
        if (autoSelectPanel && activePanel >= 0) {
            if (activePanel + 1 == provider.getNucleiChannel()) {
                TabsPanel.setSelectedIndex(1);
            } else if (activePanel + 1 == provider.getCellChannel()) {
                TabsPanel.setSelectedIndex((provider.getNucleiChannel() >= 0) ? 2 : 1);
            } else {
                TabsPanel.setSelectedIndex(TabsPanel.getTabCount() - 1);
            }

        }
    }

    protected void updateTabsPanel() {
        int nbtabs = TabsPanel.getTabCount();
        int nbSpots = 0;
        if (spotPanels != null) for (SpotsParametersGUI sp : spotPanels) if (sp != null) nbSpots++;
        int neededTabs = 1 + ((nucleiParam != null) ? 1 : 0) + ((cellParam != null) ? 1 : 0) + nbSpots;
        System.out.println("needed tabs=" + neededTabs + " (current=" + nbtabs + ")");
        for (int t = 1; t < nbtabs; t++) TabsPanel.removeTabAt(1);
        System.out.println("cleaned tabs : " + TabsPanel.getTabCount());
        //if (neededTabs != nbtabs) {
        if (nucleiParam != null) TabsPanel.add("Nuclei segmentation parameters", nucleiParam.getMainPanel());
        if (cellParam != null) TabsPanel.add("Cell segmentation parameters", cellParam.getMainPanel());
        TabsPanel.add("Quantification parameters", quantifPanel.getRootPanel());
        if (spotPanels != null) {
            for (SpotsParametersGUI sp : spotPanels) {
                if (sp != null) {
                    TabsPanel.add("Spot detection parameters for channel " + (sp.getChannel()) + " _ " + sp.getProteinName(), sp.getMainPanel());
                }
            }
        }
        //}
        //pack();
        nbtabs = TabsPanel.getTabCount();
        nbSpots = 0;
        if (spotPanels != null) for (SpotsParametersGUI sp : spotPanels) if (sp != null) nbSpots++;
        neededTabs = 1 + ((nucleiParam != null) ? 1 : 0) + ((cellParam != null) ? 1 : 0) + nbSpots;
        System.out.println("after needed tabs=" + neededTabs + " (current=" + nbtabs + ")");
        if (!resized) {
            pack();
            resized = false;
        }
    }

    protected ChannelPanel getChannelPanel(int index) {
        return channelPanels.get(index);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Window Resize Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a flag to track window resize
        int[] isResizedByUser = new int[1];

        // Add a component to display the flag's status
        JLabel flagLabel = new JLabel("Window is not resized");
        frame.add(flagLabel, BorderLayout.SOUTH);

        // Add a component to occupy space
        JPanel contentPane = new JPanel();
        frame.add(contentPane);

        // Add a ComponentListener to detect window resize
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                isResizedByUser[0]++;
                if (isResizedByUser[0] > 10) {
                    // Set the flag to true when the window is resized
                    flagLabel.setText("Window is resized by user");
                }
            }
        });


        frame.pack();
        frame.setVisible(true);
    }

    public void run(String s) {
        setTitle("MIC-MAQ");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/IconPlugin.png")));
        setContentPane(this.rootPane);
        pack();
        setVisible(true);
        resized = false;

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                sizeflag++;
                if (sizeflag > 10) resized = true;

            }
        });


    }

    protected void updateChannels() {
        if (provider == null) return;
        if (provider.getNbFielOfView() == 0) {
            IJ.error("no Images found !");
            channelsDisplay.removeAll();
            return;
        } else {
            numberOfImageLabel.setText(provider.getNbFielOfView() + " images found");
        }
        correctNbChannels = 0;
        if (provider.getDifferentNumberOfChannels() > 1) {
            GenericDialog gd = new GenericDialog("information needed!");
            gd.addMessage("images do not share the same number of channels.");
            gd.addMessage("please provide the correct number of channels to look for.");
            String[] possibilities = new String[provider.getDifferentNumberOfChannels()];
            for (int i = 0; i < provider.getDifferentNumberOfChannels(); i++)
                possibilities[i] = "images with " + provider.getNumberOfChannel(i) + " channels (" + provider.getNbImgsPerChanNb(i) + ")";
            gd.addChoice("select ", possibilities, possibilities[0]);
            gd.showDialog();
            correctNbChannels = provider.getNumberOfChannel(gd.getNextChoiceIndex());
            ArrayList<FieldOfView> fovs = provider.getAllFields();
            for (FieldOfView fov : fovs) {
                if (fov.getNbAvailableChannels() != correctNbChannels) fov.setUsed(false);
            }
            if (imagesTree instanceof ImagesTree) ((ImagesTree) imagesTree).updateTree();
        } else {
            correctNbChannels = provider.getNumberOfChannel(0);
        }
        channelsDisplay.removeAll();
        if (channelPanels == null) channelPanels = new ArrayList<>();
        channelPanels.clear();
        for (int i = 0; i < correctNbChannels; i++) {
            ChannelPanel cp = new ChannelPanel(i, channelsDisplay);
            String tmpstr = provider.getFieldOfView(((ImagesTree) imagesTree).getFirstIndex()).getChannelUserName(i + 1);
            if (tmpstr == null) {
                tmpstr = provider.getFieldOfView(((ImagesTree) imagesTree).getFirstIndex()).getChannelNameInFile(i + 1);
            }
            cp.setProteinName(tmpstr);
            channelPanels.add(cp);
            channelsDisplay.add(cp.getPanel());
        }
        //channelsDisplay.setPreferredSize(new Dimension(channelsDisplay.getComponent(0).getWidth(), channelsDisplay.getComponent(0).getHeight() * correctNbChannels));
        channelScroll.setPreferredSize(new Dimension(channelsDisplay.getComponent(0).getWidth(), 600));
        quantifPanel = new QuantificationParametersGUI(provider);
        if (!resized) {
            pack();
            resized = false;
        }

    }

    protected void checkCalibrationFromImages() {
        ArrayList<MeasureCalibration> imgsCalib = provider.checkCalibration();
        if (imgsCalib.size() == 0) return;
        MeasureCalibration retainedCalib = null;
        if (imgsCalib.size() == 1) retainedCalib = imgsCalib.get(0);
        else {
            GenericDialog gd = new GenericDialog("information needed!");
            gd.addMessage("images do not share the same calibration.");
            gd.addMessage("please provide the correct calibration to look for.");
            String[] possibilities = new String[imgsCalib.size()];
            for (int i = 0; i < imgsCalib.size(); i++)
                possibilities[i] = "images with calibration " + imgsCalib.get(i);
            gd.addChoice("select ", possibilities, possibilities[0]);
            gd.showDialog();
            retainedCalib = imgsCalib.get(gd.getNextChoiceIndex());
            ArrayList<FieldOfView> fovs = provider.getAllFields();
            for (FieldOfView fov : fovs) {
                if (fov.getPixelSize(1) != retainedCalib.getPixelLength()) fov.setUsed(false);
            }
            if (imagesTree instanceof ImagesTree) ((ImagesTree) imagesTree).updateTree();

        }
        addCalibrationToComboBox(retainedCalib);

    }


    public static File chooseDirectory(Component parent) {
        //            Create JFileChooser to get directory
        JFileChooser directoryChooser = new JFileChooser(IJ.getDirectory("current"));

        /*UIManager.put("FileChooser.openButtonText", "Open");
        UIManager.put("FileChooser.cancelButtonText", "Cancel");
        UIManager.put("FileChooser.saveButtonText", "Save");
        UIManager.put("FileChooser.cancelButtonToolTipText", "Cancel selection");
        UIManager.put("FileChooser.saveButtonToolTipText", "Save selected file");
        UIManager.put("FileChooser.openButtonToolTipText", "Open selected file");

        UIManager.put("FileChooser.lookInLabelText", "Find in:");
        UIManager.put("FileChooser.fileNameLabelText", "File name:");
        UIManager.put("FileChooser.filesOfTypeLabelText", "File type:");
        UIManager.put("FileChooser.upFolderToolTipText", "Up one level");
        UIManager.put("FileChooser.homeFolderToolTipText", "Home");
        UIManager.put("FileChooser.newFolderToolTipText", "Create new folder");
        UIManager.put("FileChooser.listViewButtonToolTipText", "List");*/
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

//            If directory approved by user
        if (directoryChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File directory = directoryChooser.getSelectedFile();
            if (!directory.exists()) {
                IJ.error("The directory does not exists");
            } else if (!directory.isDirectory()) {
                IJ.error("It needs to be a directory not a file");
            }
            OpenDialog.setDefaultDirectory(directory.getAbsolutePath());
            return directory;
        }
        return null;
    }

    private void addCalibrationToComboBox(MeasureCalibration newCalibration) {
        DefaultComboBoxModel<MeasureCalibration> calibrationDefaultComboBoxModel = (DefaultComboBoxModel<MeasureCalibration>) calibrationCombo.getModel();
        if (calibrationDefaultComboBoxModel.getElementAt(0).getName().startsWith("From image"))
            calibrationDefaultComboBoxModel.removeElementAt(0);
        calibrationDefaultComboBoxModel.insertElementAt(newCalibration, 0);//
        calibrationCombo.setSelectedIndex(0);
    }

    private void setCalibrationComboBox() {
//        Parse calibration file
        MeasureCalibration[] calibrationsArray = MeasureCalibration.getCalibrationArrayFromFile();
//        Add calibrations found to ComboBox
        DefaultComboBoxModel<MeasureCalibration> calibrationDefaultComboBoxModel = new DefaultComboBoxModel<>(calibrationsArray);
        calibrationCombo = new JComboBox<>(calibrationDefaultComboBoxModel);

    }

    private void displayPreviewImages() {
        if (provider != null && provider.getNbFielOfView() > 0) {
            ((ImagesTree) imagesTree).validateSelections();
            int countNuclei = 0;
            int countCell = 0;
            for (ChannelPanel cp : channelPanels) {
                if (cp.isNuclei()) countNuclei++;
                if (cp.isCell()) countCell++;
            }
            if (countNuclei > 1 || countCell > 1) {
                IJ.error("channels distribution error", "for segmentation there can be only one nuclei channel and one cell channel");
                return;
            }
            int index = (Integer) PreviewSpinner.getValue();
            FieldOfView imgs = provider.getFieldOfView(index);
            while (imgs.getNbAvailableChannels() != correctNbChannels)
                imgs = provider.getFieldOfView(++index);

            int q = 0;
            for (int c = 1; c <= imgs.getNbAvailableChannels(); c++) {
                ChannelPanel pan = getChannelPanel(c - 1);
                if (pan.isUsed()) {
                    String title = "C" + c + "_";
                    title += pan.getProteinName() + "_";
                    if (pan.isNuclei()) title += "Nuclei_";
                    if (pan.isCell()) title += "Cell_";
                    if (pan.isQuantification()) title += "Q" + (++q) + "_";
                    if (pan.isSpot()) title += "Spots_";
                    title += imgs.getFieldname();
                    ImagePlus imp = imgs.getImagePlus(c);
                    imp.setTitle(title);
                    imp.show();
                }
            }
        }
    }


    private void previewAction() {
        if (provider != null && provider.getNbFielOfView() > 0) {
            int countNuclei = 0;
            int countCell = 0;
            for (ChannelPanel cp : channelPanels) {
                if (cp.isNuclei()) countNuclei++;
                if (cp.isCell()) countCell++;
            }
            if (countNuclei > 1 || countCell > 1) {
                IJ.error("channels distribution error", "for segmentation there can be only one nuclei channel and one cell channel");
                return;
            }
            ProgressWindowK2000 progress = new ProgressWindowK2000("Preview step");
            progress.setLocationRelativeTo(rootPane);
            progress.setAlwaysOnTop(true);

            int index = (Integer) PreviewSpinner.getValue();
            FieldOfView imgs = provider.getFieldOfView(index);

            Experiment exp = createExperiment(index, true);
            IJ.log("#############################");
            IJ.log("##        preview          ##");
            IJ.log("#############################");

            WindowManager.setWindow(WindowManager.getWindow("Log"));

            exp.run();
            if (cellResults != null) cellResults.show("cells/spots");
            if (nucleusResults != null) nucleusResults.show("nuclei correspondence");
            progress.dispose();
            new WindowOrganizer().run("tile");
            WindowManager.getWindow("cells/spots").toFront();
            RoiManager.getInstance().toFront();
        }
    }

    private void previewStep() {
        ProgressWindowK2000 progress = new ProgressWindowK2000("Preview step");
        progress.setLocationRelativeTo(rootPane);
        progress.setAlwaysOnTop(true);

        int index = (Integer) PreviewSpinner.getValue();
        FieldOfView imgs = provider.getFieldOfView(index);
        Experiment exp = createExperiment(index, true);

        JPanel current = (JPanel) TabsPanel.getSelectedComponent();
        if (nucleiParam != null && current == nucleiParam.getMainPanel()) {
            IJ.log("preview nuclei segmentation");
            exp.previewNucleiSegmentation();
        } else if (cellParam != null && current == cellParam.getMainPanel()) {
            IJ.log("preview cell segmentation");
            exp.previewCellSegmentation();
        } else if (spotPanels != null && spotPanels.size() > 0) {
            for (int i = 0; i < spotPanels.size(); i++) {
                SpotsParametersGUI sp = spotPanels.get(i);
                if (sp != null && current == sp.getMainPanel()) {
                    IJ.log("preview spot detection " + i + " (" + sp.getChannel() + ")");
                    exp.previewSpotDetection(sp.getChannel());
                }
            }
        }
        progress.dispose();
        new WindowOrganizer().run("tile");
        //WindowManager.getWindow("Log").toFront();
        RoiManager.getInstance().toFront();
    }

    public void runAllExperiments() {
        ((ImagesTree) imagesTree).validateSelections();
        Instant dateBegin = Instant.now();
        if (cellResults != null) cellResults = null;
        if (nucleusResults != null) nucleusResults = null;
        if (!checkParameters()) return;
        ProgressMonitor progress = new ProgressMonitor(this, "computing for all images",
                "", -1, provider.getNbFielOfView() * 100);
        progress.setMillisToDecideToPopup(1);
        progress.setMillisToPopup(1);
        int countNbExp = 0;
        for (int index = 0; index < provider.getNbFielOfView(); index++) {
            if (provider.getFieldOfView(index).isUsed()) countNbExp++;
        }
        for (int index = 0; index < provider.getNbFielOfView(); index++) {

            IJ.log("#############################");
            IJ.log("##      run experiment " + IJ.pad(index + 1, 3) + "           ##");
            IJ.log("#############################");
            IJ.log("selected for computation : " + provider.getFieldOfView(index).isUsed());
            IJ.showStatus("running experiment " + index + " (" + provider.getNbFielOfView() + ")");
            if (provider.getFieldOfView(index).isUsed()) {
                String msg = "working on " + (index + 1) + "/" + provider.getNbFielOfView();
                if (index > 0) {
                    Instant dateTmp = Instant.now();
                    long duration = Duration.between(dateBegin, dateTmp).toMillis();
                    long remain = (duration / (index)) * (provider.getNbFielOfView() - (index));
                    String remainString = String.format("%02d min %02d sec",
                            TimeUnit.MILLISECONDS.toMinutes(remain),
                            TimeUnit.MILLISECONDS.toSeconds(remain) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remain))
                    );
                    msg += (", " + remainString + " remaining");
                }
                progress.setNote(msg);
                progress.setProgress(index * 100);
                Experiment exp = createExperiment(index, false);
                exp.run();
                if (progress.isCanceled()) {
                    exp.interruptProcess();
                    IJ.log("process canceled");
                    return;
                }
                progress.setProgress((index + 1) * 100);
                IJ.showProgress(index + 1, provider.getNbFielOfView());
            } else {
                IJ.log("User removed this field of view from analysis\nnothing done!");
            }
        }
        if (cellResults != null) {
            cellResults.deleteRow(cellResults.size() - 1);
            cellResults.show("Results");
            cellResults.save(workingDirectory + "/results/Results.xls");
        }
        if (nucleusResults != null) {
            nucleusResults.deleteRow(nucleusResults.size() - 1);
            nucleusResults.show("Cells-Nuclei Association");
            nucleusResults.save(workingDirectory + "/results/Cells-Nuclei-Association.xls");
        }

        Instant dateEnd = Instant.now();
        long duration = Duration.between(dateBegin, dateEnd).toMillis();
        IJ.log("Analysis is done. It took " + duration / 1000 + " seconds");
        progress.close();
    }

    public Experiment createExperiment(int index, boolean preview) {
        IJ.log("create experiment");
        FieldOfView imgs = provider.getFieldOfView(index);
        ExperimentSettings settings = new ExperimentSettings(imgs);
        boolean nucleus = false;
        boolean cell = false;
        for (int i = 0; i < channelPanels.size(); i++) {
            ChannelPanel cp = channelPanels.get(i);
            if (cp.isUsed() && cp.isNuclei()) {
                System.out.println("nuclei channel: " + (i + 1));
                SegmentationParameters params = nucleiParam.getParameters();
                params.getMeasurements().setName("_C" + (i + 1) + "_" + cp.getProteinName());
                MeasureValue tmp = quantifPanel.getMeasuresSegmentation(i+1);
                params.getMeasurements().setMeasure(tmp.getMeasure());
                params.setPreprocessMacroQuantif(quantifPanel.getMacro(i+1));
                settings.setSegmentationNuclei(i + 1, params);
                if (!params.isZproject() && tmp.isZproject()) {
                    params.setProjection(tmp.getProjectionMethod(), tmp.getProjectionSliceMin(), tmp.getProjectionSliceMax());
                }
                nucleus = true;
            } else if (cp.isUsed() && cp.isCell()) {
                System.out.println("cell channel: " + (i + 1));
                SegmentationParameters params = cellParam.getParameters();
                MeasureValue tmp = quantifPanel.getMeasuresSegmentation(i+1);
                params.getMeasurements().setMeasure(tmp.getMeasure());
                params.getMeasurements().setName("_C" + (i + 1) + "_" + cp.getProteinName());
                params.setPreprocessMacroQuantif(quantifPanel.getMacro(i+1));
                settings.setSegmentationCell(i + 1, params);
                if (!params.isZproject() && tmp.isZproject()) {
                    params.setProjection(tmp.getProjectionMethod(), tmp.getProjectionSliceMin(), tmp.getProjectionSliceMax());
                }
                cell = true;
            } else if (cp.isUsed()) {
                System.out.println("quantification channel: " + (i + 1));
                IJ.log("quantification channel: " + (i + 1) + " is spot " + cp.isSpot());
                //IJ.log("number of slices: " + imgs.getNSlices(i + 1));
                MeasureValue measureValue = new MeasureValue(false);
                if (cp.isSpot()) {
                    SpotsParametersGUI sp = spotPanels.get(i);
                    if (sp != null) {
                        measureValue = sp.getMeasure();
                        MeasureValue tmp = quantifPanel.getMeasuresSegmentation(i+1);
                        if (!measureValue.isZproject() && tmp.isZproject()) {
                            measureValue.setProjection(tmp.getProjectionMethod());
                            measureValue.setProjectionSliceMin(tmp.getProjectionSliceMin());
                            measureValue.setProjectionSliceMax(tmp.getProjectionSliceMax());
                        }
                    }
                } else {
                    //TODO check that MAX projection is what should be done by default
                    if (imgs.getNSlices(i + 1) > 1) {
                        MeasureValue tmp = quantifPanel.getMeasuresQuantif(i+1);
                        measureValue.setProjection(tmp.getProjectionMethod());
                        measureValue.setProjectionSliceMin(tmp.getProjectionSliceMin());
                        measureValue.setProjectionSliceMax(tmp.getProjectionSliceMax());
                        IJ.log("C" + (i + 1) + " quantification without spots: 3D -> set projection to the one defined in quantification");
                    }
                }
                measureValue.setMeasure(quantifPanel.getMeasuresQuantif(i+1).getMeasure());
                measureValue.setName("C" + (i + 1) + "_" + cp.getProteinName());
                measureValue.setPreprocessMacroQuantif(quantifPanel.getMacro(i+1));
                settings.setQuantification(i + 1, measureValue);
                //IJ.log("create exp: spot macro:" + measureValue.getPreprocessMacro());
                //IJ.log("create exp: spot quantif macro:" + measureValue.getPreprocessMacroQuantif());
            }
        }

        settings.setCalibration((MeasureCalibration) calibrationCombo.getSelectedItem());
        IJ.log("using calibration " + calibrationCombo.getSelectedItem());
        if (cellResults == null) cellResults = new ResultsTable();
        if (nucleus && cell && nucleusResults == null) nucleusResults = new ResultsTable();
        Experiment exp = settings.createExperiment(workingDirectory, imgs, cellResults, nucleusResults, preview);
        IJ.log("create experiment finished");
        return exp;
    }

    private boolean checkParameters() {
        boolean warningN = false;
        boolean warningC = false;
        boolean warningS = false;
        boolean warningQ = false;
        String warningMessage = "";
        FieldOfView imgs = provider.getFieldOfView(((ImagesTree) imagesTree).getFirstIndex());
        for (int i = 0; i < channelPanels.size(); i++) {
            ChannelPanel cp = channelPanels.get(i);
            if (cp.isUsed() && cp.isNuclei()) {
                System.out.println("nuclei channel: " + (i + 1));
                SegmentationParameters params = nucleiParam.getParameters();
                warningN = (imgs.getNSlices(i + 1) > 1) && (!params.isZproject());
                if (warningN) {
                    warningMessage += "C" + (i + 1) + " projection for nuclei is not defined, using the one from quantification\n";
                }
            } else if (cp.isUsed() && cp.isCell()) {
                SegmentationParameters params = cellParam.getParameters();
                MeasureValue tmp = quantifPanel.getMeasuresSegmentation(i+1);
                warningC = (imgs.getNSlices(i + 1) > 1) && (!params.isZproject());
                if (warningC)
                    warningMessage += "C" + (i + 1) + " projection for cell is not defined, using the one from quantification\n";
            } else if (cp.isUsed()) {
                if (cp.isSpot()) {
                    SpotsParametersGUI sp = spotPanels.get(i);
                    if (sp != null) {
                        MeasureValue measureValue = sp.getMeasure();
                        //MeasureValue tmp = quantifPanel.getMeasuresSegmentation();
                        warningS = (imgs.getNSlices(i + 1) > 1) && (!measureValue.isZproject());
                        if (warningN)
                            warningMessage += "C" + (i + 1) + " projection for spot is not defined, using the one from quantification\n";
                        if (!measureValue.isSpotThreshold() && !measureValue.isSpotFindMaxima()) {
                            warningS = true;
                            warningMessage += "C" + (i + 1) + " is defined as spot but no spot detection method is selected\n";
                        }
                    }
                }
            }
            MeasureValue tmp = quantifPanel.getMeasuresQuantif(i+1);
            if (((imgs.getNSlices(i + 1) > 1) && (!tmp.isZproject()))) {
                warningQ = true;
                warningMessage += "C" + (i + 1) + " projection for quantification is not defined while using stacks\n";
            }
        }

        if (warningC || warningN || warningQ || warningS) {
            GenericDialog genericDialog = new GenericDialog("warning");
            genericDialog.addMessage(warningMessage);
            genericDialog.showDialog();
            return genericDialog.wasOKed();
        }
        return true;
    }

    private void createParametersFile() {
        if (!new File(workingDirectory).exists()) {
            ImageToAnalyze.createResultsDirectory(workingDirectory);
        }
        File resultdircheck = new File(workingDirectory + "/Results/");
        if (!resultdircheck.exists()) resultdircheck.mkdirs();
        String parameterFilename = workingDirectory + "/Results/Parameters.txt";
        File parametersFile = new File(parameterFilename);
        try {
            if (parametersFile.createNewFile()) {
                IJ.log("Creation of parameters file in results directory");
            }
            FileWriter fileWriter = new FileWriter(parametersFile.getAbsoluteFile(), false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.append("The configuration is :");

            bufferedWriter.append("\nFILES: ");
            String tmp = "\n\tDirectory : "+workingDirectory;
            tmp += (filePattern!=null)? "\n\tFilter by image filename pattern: "+filePattern : "";
            if(patterns!=null){
                tmp+="\n\tRearrange image file into channels with patterns:";
                for(int i=0;i<patterns.size();i++){
                    tmp+="\n\t\tCHANNEL "+(i+1)+": "+patterns.get(i);
                }
            }
            bufferedWriter.append(tmp);

            bufferedWriter.append("\n\nCALIBRATION: " + calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex()).toString());

            for (int i = 0; i < channelPanels.size(); i++) {
                ChannelPanel cp = channelPanels.get(i);
                tmp = "\n\nCHANNEL " + (i + 1) + " (" + channelPanels.get(i).getProteinName() + "):" + (cp.isUsed() ? " used" : " NOT USED");
                if (cp.isUsed() && cp.isNuclei()) {
                    tmp += nucleiParam.getParametersAsString();
                    tmp += quantifPanel.getParametersAsString(true, i+1);
                } else if (cp.isUsed() && cp.isCell()) {
                    tmp += cellParam.getParametersAsString();
                    tmp += quantifPanel.getParametersAsString(true, i+1);
                } else if (cp.isUsed()) {
                    tmp += "\nQUANTIFICATION";
                    if (cp.isSpot()) {
                        SpotsParametersGUI sp = spotPanels.get(i);
                        if (sp != null) {
                            tmp += sp.getParametersAsString();
                        }
                    }
                    tmp += quantifPanel.getParametersAsString(false, i+1);
                }
                bufferedWriter.append(tmp);
            }

            bufferedWriter.close();
        } catch (IOException e) {
            IJ.log("The parametersFile could not be created in " + workingDirectory);
            e.printStackTrace();
        }

    }

    private void createUIComponents() {
        nrProteinSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
        setCalibrationComboBox();
        channelsDisplay = new JPanel();
        channelsDisplay.setLayout(new GridLayout(0, 1));
        //channelsDisplay.setLayout(new BoxLayout(channelsDisplay, BoxLayout.Y_AXIS));
        //channelsDisplay.setPreferredSize(new Dimension(800, 600));
        imagesTree = new ImagesTree(null);

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
        panel1.setBackground(new Color(-16777216));
        rootPane = new JPanel();
        rootPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPane.setBackground(new Color(-16777216));
        panel1.add(rootPane, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(800, 700), null, 0, false));
        LogoPanel = new JPanel();
        LogoPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        LogoPanel.setBackground(new Color(-16777216));
        rootPane.add(LogoPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setBackground(new Color(-16777216));
        label1.setHorizontalAlignment(0);
        label1.setIcon(new ImageIcon(getClass().getResource("/logo_bandeau.png")));
        label1.setText("");
        LogoPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        TabsPanel = new JTabbedPane();
        rootPane.add(TabsPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(800, 400), null, 0, false));
        ConfigurationPanel = new JPanel();
        ConfigurationPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        TabsPanel.addTab("Configuration", ConfigurationPanel);
        ConfigurationPanel.setBorder(BorderFactory.createTitledBorder(null, "configuration", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        globalPanel = new JPanel();
        globalPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        ConfigurationPanel.add(globalPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        globalPanel.setBorder(BorderFactory.createTitledBorder(null, "data", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        globalPanel.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 2, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        browseButton = new JButton();
        browseButton.setText("browse");
        panel2.add(browseButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numberOfImageLabel = new JLabel();
        numberOfImageLabel.setText("no files!");
        panel2.add(numberOfImageLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reorganiseDataButton = new JButton();
        reorganiseDataButton.setText("rearrange data");
        panel2.add(reorganiseDataButton, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("select files with pattern");
        panel3.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        filePatternTextField = new JTextField();
        filePatternTextField.setColumns(30);
        filePatternTextField.setText("");
        panel3.add(filePatternTextField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("directory");
        panel4.add(label3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chosenDirectoryTextArea = new JTextArea();
        chosenDirectoryTextArea.setColumns(50);
        chosenDirectoryTextArea.setEditable(false);
        chosenDirectoryTextArea.setEnabled(true);
        chosenDirectoryTextArea.setText("No directory chosen");
        panel4.add(chosenDirectoryTextArea, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTH, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 1, false));
        final JSplitPane splitPane1 = new JSplitPane();
        globalPanel.add(splitPane1, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
        TreeJScroll = new JScrollPane();
        splitPane1.setLeftComponent(TreeJScroll);
        TreeJScroll.setViewportView(imagesTree);
        channelScroll = new JScrollPane();
        splitPane1.setRightComponent(channelScroll);
        channelScroll.setViewportView(channelsDisplay);
        channelsDisplay.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "channels", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, new Color(-4473925)));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        ConfigurationPanel.add(panel5, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(null, "calibration", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label4 = new JLabel();
        label4.setText("calibration");
        panel5.add(label4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addNewCalibrationButton = new JButton();
        addNewCalibrationButton.setText("add new calibration");
        panel5.add(addNewCalibrationButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel5.add(calibrationCombo, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel6.setBackground(new Color(-16777216));
        rootPane.add(panel6, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        panel7.setBorder(BorderFactory.createTitledBorder(null, "preview", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label5 = new JLabel();
        label5.setText("preview on images set");
        panel7.add(label5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        PreviewSpinner = new JSpinner();
        panel7.add(PreviewSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previewButton = new JButton();
        previewButton.setText("preview all steps");
        panel7.add(previewButton, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showImagesButton = new JButton();
        showImagesButton.setText("show images");
        panel7.add(showImagesButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previewCurrentStepButton = new JButton();
        previewCurrentStepButton.setEnabled(false);
        previewCurrentStepButton.setText("preview current step");
        panel7.add(previewCurrentStepButton, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel6.add(cancelButton, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        launchButton = new JButton();
        launchButton.setText("Launch");
        panel6.add(launchButton, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }


}
