package fr.curie.micmaq.gui;


import fr.curie.micmaq.helpers.ImageToAnalyze;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

/**
 * Author : Camille RABIER
 * Date : 15/03/2022
 * GUI Class for
 * - choosing to use open images or images from repertory
 * - choose repertory or image to use
 */
public class OpenImages extends JFrame implements PlugIn {
    //    GUI :
    private JPanel mainPanel;
    //    --> CHOOSE PANEL TO GET IMAGES
    private JLabel preTextChoiceImage;

    private JPanel radioButtonChoicePanel;
    private JRadioButton useFileImagesRadioButton;
    private JRadioButton useOpenImagesRadioButton;

    private JPanel originImages;

    //    --> GET IMAGES FROM OPENED IMAGES
    private JPanel openImages;
    //    List of opened image
    private JScrollPane windowListScroll;
    private JList<ImageToAnalyze> windowList;
    //    Display or remove image
    private JButton displayImageButton;
    private JButton removeButton;

    //    --> GET IMAGES FROM DIRECTORY
    private JPanel directoryImages;
    //    Choose directory
    private JButton chooseDirectoryButton;
    private JTextArea chosenDirectory;
    private JScrollPane imageListScroll;
    //    Extension of images to consider
    private JLabel extensionLabel;
    private JComboBox<String> extension;
    private JTextField otherExtensionField;
    //  List of image in directory
    //private JList<String> imageList;
    private JList<String> directoryImageList;

    //    --> VALIDATE IMAGES TO USE
    private JPanel validationPanel;
    private JButton OKButton;
    private JButton cancelButton;

    //    NON GUI
    boolean useDirectory;
    private File directory; /*directory containing the images to use*/
    private ImageToAnalyze[] ipList; /*list of image to use*/
    private final DefaultListModel<ImageToAnalyze> openImagesModel = new DefaultListModel<>(); /*list of opened images*/
    //private final DefaultListModel<String> fileModel = new DefaultListModel<>(); /*list of image in directory*/
    private final DefaultListModel<String> directoryFileModel = new DefaultListModel<>();


    public OpenImages() {
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
        $$$setupUI$$$();
//        Look for open images to see if the choice for the opened images' panel is necessary
        if (WindowManager.getImageCount() == 0) { /*No open images*/
            /*No need for the radiobutton*/
            useOpenImagesRadioButton.setVisible(false);
            useFileImagesRadioButton.setVisible(false);
            /*Display panel to choose from directory and hide the other*/
            useDirectory = true;
            openImages.setVisible(false);
            pack();
        } else {/*if opened images, by default the directory panel is hidden*/
            directoryImages.setVisible(false);
            useDirectory = false;
            openImagesModel.removeElement(null); /*remove temporary element*/
            for (int id_Image : WindowManager.getIDList()) { /*add all opened image to list*/
                ImageToAnalyze image = new ImageToAnalyze(WindowManager.getImage(id_Image));
                openImagesModel.addElement(image);
            }
            pack();
        }

//        SET UP PANELS
        useFileImagesRadioButton.addItemListener(e -> {
            openImages.setVisible(e.getStateChange() == ItemEvent.DESELECTED);
            directoryImages.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            useDirectory = (e.getStateChange() == ItemEvent.SELECTED);
        });

//      DIRECTORY PANEL
        //        Set up extension fields
        otherExtensionField.setVisible(false);
        otherExtensionField.setText(Prefs.get("MICMAQ.ChoiceExtension", ".TIF"));
        extension.setSelectedItem(Prefs.get("MICMAQ.ChoiceExtension", ".TIF"));
        extension.addItemListener(e -> {
            String extensionSelected = (String) extension.getSelectedItem();
            if (extensionSelected.equals("Other")) { /*If extension wanted not in Jlist*/
                otherExtensionField.setVisible(true);
                otherExtensionField.setText(Prefs.get("MICMAQ.ChoiceExtensionCustom", ".TIF"));
            } else {
                otherExtensionField.setText(extensionSelected);
                otherExtensionField.setVisible(false);
            }
            pack();
//            If files from directory have already been filtered, refilter
            if (ipList != null && ipList.length > 0) {
                getFilesFromDirectory();
            }
        });
        otherExtensionField.addActionListener(e -> {
            if (ipList != null && ipList.length > 0) getFilesFromDirectory();
        });

        chooseDirectoryButton.addActionListener(e -> {
            directory = chooseDirectory(OpenImages.this);
            if (directory != null) {
                String path = directory.getAbsolutePath();
//                Shorten path to show only the 2 last subdirectories
                if (path.split("\\\\").length > 2) {
                    String path_shorten = path.substring(path.substring(0, path.lastIndexOf("\\")).lastIndexOf("\\"));
                    chosenDirectory.setText("..." + path_shorten);
                } else if (path.split("/").length > 2) {
                    String path_shorten = path.substring(path.substring(0, path.lastIndexOf("/")).lastIndexOf("/"));
                    chosenDirectory.setText("..." + path_shorten);
                } else {
                    chosenDirectory.setText(path);
                }

//              Get images from the directory
                getFilesFromDirectory();
            }
        });


//        OPEN IMAGE PANEL
//        Display selected image
        displayImageButton.addActionListener(e -> {
            if (windowList.getModel().getSize() > 0) {
                int selectedImageIndex = windowList.getSelectedValue().getID();
                IJ.selectWindow(selectedImageIndex);
            }
        });
//        Remove selected image from list
        removeButton.addActionListener(e -> {
            int selectedIndex = windowList.getSelectedIndex();
            if (selectedIndex != -1) openImagesModel.remove(windowList.getSelectedIndex());
            if (windowList.getModel().getSize() > 0) windowList.setSelectedIndex(0);
        });

//        VALIDATION PANEL
        OKButton.addActionListener(e -> {
            if (useDirectory) { /*Images used afterwards are directory images*/
                if (directory == null) {/*No directory given*/
                    IJ.error("No directory chosen.");
                } else {
                    if (ipList == null) {
                        IJ.error("No image to analyze.");
                    } else {
                        Prefs.set("MICMAQ.ChoiceExtension", extension.getItemAt(extension.getSelectedIndex()));
                        Prefs.set("MICMAQ.ChoiceExtensionCustom", otherExtensionField.getText());
                    }
                }
            } else { /*Images used afterwards are opened images*/
                if (windowList.getModel().getSize() == 0) {
                    IJ.error("There are no image to analyze.");
                } else {
//                    Add images to image list that will be analyzed
                    ipList = new ImageToAnalyze[openImagesModel.getSize()];
                    for (int i = 0; i < openImagesModel.getSize(); i++) {
                        ipList[i] = openImagesModel.getElementAt(i);
                    }

                }
            }
            if (ipList != null) { /*Launch next step of the plugin*/
                IJ.log("There are " + ipList.length + " images");
                PluginCellProt analyzer = new PluginCellProt(ipList, useDirectory);
                analyzer.run(null);
                this.setVisible(false);
            }
        });
        cancelButton.addActionListener(e -> dispose());
    }

    public static File chooseDirectory(Component parent) {
        //            Create JFileChooser to get directory
        JFileChooser directoryChooser = new JFileChooser(IJ.getDirectory("current"));

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

    /**
     * Get images in directory
     * Filter according to extension
     */
    private void getFilesFromDirectory() {
//        Get extension
        String extension = otherExtensionField.getText().toLowerCase(Locale.ROOT);
//        Filter files according to extension
        FileFilter fileFilter = file -> !file.isDirectory()
                && (file.getName().toLowerCase(Locale.ROOT).endsWith(extension));
        File[] images = directory.listFiles(fileFilter);

        if (images != null) {
//        If there are image corresponding to filter
            if (images.length == 0) {
                //
                if (!directoryFileModel.contains("No file")) {
                    directoryFileModel.removeAllElements();
                    directoryFileModel.addElement("No file");
                    IJ.error("No file corresponding to the extension chosen has been found");
                }
            } else {
                ipList = new ImageToAnalyze[images.length];
//                fileModel.removeAllElements();
                directoryFileModel.removeAllElements();
                for (int i = 0; i < images.length; i++) {
                    ipList[i] = new ImageToAnalyze(directory.getAbsolutePath(), images[i].getName());
//                    fileModel.addElement(images[i].getName());
                    directoryFileModel.addElement(images[i].getName());
                }


            }
        }
    }

    @Override
    public void run(String s) {
        createUIComponents();
        setTitle("MIC-MAQ");

        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/IconPlugin.png")));
        setContentPane(this.mainPanel);
        setPreferredSize(new Dimension(500, 500));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private void createUIComponents() {
        if (openImagesModel.getSize() == 0) {
            openImagesModel.addElement(null);
        }
        windowList = new JList<>(openImagesModel);
//        if (!fileModel.contains("No file")) {
//            fileModel.addElement("No file");
//        }
//        imageList = new JList<>(fileModel);
        windowList.setSelectedIndex(0);

        preTextChoiceImage = new JLabel();
        if (WindowManager.getImageCount() > 0) {
            preTextChoiceImage.setText("There are open images, do you want to use them ?");
        } else {
            preTextChoiceImage.setText("There are no open images, please choose a directory");
            useDirectory = true;
        }

        if (!directoryFileModel.contains("No file!")) {
            directoryFileModel.addElement("No file!");
        }
        directoryImageList = new JList<>(directoryFileModel);
    }

    /**
     * Tests
     *
     * @param args : none
     */
    public static void main(String[] args) {
        ImagePlus[] imagesToAnalyze = new ImagePlus[3];
        imagesToAnalyze[0] = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        imagesToAnalyze[1] = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        imagesToAnalyze[2] = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w21 FITC.TIF");
        for (ImagePlus images : imagesToAnalyze) {
            images.show();
        }
        OpenImages openImages = new OpenImages();
        openImages.run(null);
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
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Images to use", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        mainPanel.add(preTextChoiceImage, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        radioButtonChoicePanel = new JPanel();
        radioButtonChoicePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(radioButtonChoicePanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        useOpenImagesRadioButton = new JRadioButton();
        useOpenImagesRadioButton.setSelected(true);
        useOpenImagesRadioButton.setText("Yes");
        radioButtonChoicePanel.add(useOpenImagesRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useFileImagesRadioButton = new JRadioButton();
        useFileImagesRadioButton.setText("No");
        radioButtonChoicePanel.add(useFileImagesRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        validationPanel = new JPanel();
        validationPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(validationPanel, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        validationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        OKButton = new JButton();
        OKButton.setText("OK");
        OKButton.setToolTipText("Sets ok");
        validationPanel.add(OKButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        validationPanel.add(cancelButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        originImages = new JPanel();
        originImages.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(originImages, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        openImages = new JPanel();
        openImages.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        originImages.add(openImages, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        displayImageButton = new JButton();
        displayImageButton.setText("Display image");
        openImages.add(displayImageButton, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        removeButton = new JButton();
        removeButton.setText("Remove");
        openImages.add(removeButton, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        windowListScroll = new JScrollPane();
        openImages.add(windowListScroll, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        windowList.setSelectionMode(0);
        windowListScroll.setViewportView(windowList);
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        openImages.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        directoryImages = new JPanel();
        directoryImages.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 3, new Insets(0, 0, 0, 0), -1, -1));
        originImages.add(directoryImages, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        extensionLabel = new JLabel();
        extensionLabel.setText("Extension of files");
        directoryImages.add(extensionLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        extension = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement(".TIF");
        defaultComboBoxModel1.addElement(".STK");
        defaultComboBoxModel1.addElement("Other");
        extension.setModel(defaultComboBoxModel1);
        extension.setToolTipText("is a combo box");
        directoryImages.add(extension, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseDirectoryButton = new JButton();
        chooseDirectoryButton.setText("Choose directory");
        directoryImages.add(chooseDirectoryButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chosenDirectory = new JTextArea();
        chosenDirectory.setLineWrap(true);
        chosenDirectory.setText("No directory choosen");
        chosenDirectory.setWrapStyleWord(true);
        directoryImages.add(chosenDirectory, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        imageListScroll = new JScrollPane();
        directoryImages.add(imageListScroll, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageListScroll.setViewportView(directoryImageList);
        otherExtensionField = new JTextField();
        otherExtensionField.setText("");
        directoryImages.add(otherExtensionField, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        directoryImages.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        directoryImages.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer4 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer4, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(useOpenImagesRadioButton);
        buttonGroup.add(useFileImagesRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
