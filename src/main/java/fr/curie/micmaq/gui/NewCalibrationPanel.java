package fr.curie.micmaq.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import fr.curie.micmaq.helpers.MeasureCalibration;
import ij.IJ;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Author : Camille RABIER
 * Date : 19/04/2022
 * GUI Class for
 * - adding of modifying calibration in calibration file
 */
public class NewCalibrationPanel extends JFrame {
    private JPanel mainPanel;
    private JButton validateButton;
    private JTable calibrationTable;
    private JTextField addNameField;
    private JTextField addValueField;
    private JTextField addUnitField;
    private JButton addButton;
    private JScrollPane calibrationTableScroll;
    private JPanel addCalibrationPanel;
    private DefaultTableModel tableModel;

    public NewCalibrationPanel(DefaultComboBoxModel<MeasureCalibration> calibrationValues) {
        $$$setupUI$$$();
//        ADD CALIBRATION
        addButton.addActionListener(e -> {
//            Verification that all fields are filled
            if (addNameField.getText().length() > 0 && addValueField.getText().length() > 0 && addUnitField.getText().length() > 0) {
//                Verification that the name does not already exist
                boolean nameAlreadyExists = false;
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    if (addNameField.getText().equals(tableModel.getValueAt(row, 0))) {
                        nameAlreadyExists = true;
                        IJ.error("The calibration name is not unique, it will not be added");
                    }
                }
//                Add to table model and empty the field
                if (!nameAlreadyExists) {
                    tableModel.addRow(new String[]{addNameField.getText(), addValueField.getText(), addUnitField.getText()});
                    addNameField.setText("");
                    addValueField.setText("");
                    addUnitField.setText("");
                }
            } else {
                IJ.error("All the fields need to be filled");
            }
        });
//        Rewrite file if the user validate the modifications
        validateButton.addActionListener(e -> {
            try {
                MeasureCalibration.createCalibrationFile(true);
            } catch (IOException ex) {
                IJ.error("The calibration file could not be found and could not be created." +
                        "It could be a problem of access rights of the ImageJ/Fiji preferences folder.");
                ex.printStackTrace();
            }
            ArrayList<String> nameArrayList = new ArrayList<>();
            calibrationValues.removeAllElements();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                String calibrationName = (String) tableModel.getValueAt(row, 0);
                if (calibrationName.length() > 0 && String.valueOf(tableModel.getValueAt(row, 0)).length() > 0 && ((String) tableModel.getValueAt(row, 2)).length() > 0) {
                    if (nameArrayList.contains(calibrationName)) {
                        IJ.error("There are multiple calibrations with te name " + calibrationName
                                + ". Only the first one will be written on the file.");
                    } else {
                        nameArrayList.add(calibrationName);
                        MeasureCalibration calibration = new MeasureCalibration((String) tableModel.getValueAt(row, 0), String.valueOf(tableModel.getValueAt(row, 1)), (String) tableModel.getValueAt(row, 2));
                        calibration.addCalibrationToFile();
                        calibrationValues.addElement(calibration);
                    }
                }
            }
            this.dispose();
        });
//        cancelButton.addActionListener(e -> this.dispose());
    }

    public void run() {
        setTitle("Add a calibration");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/IconPlugin.png")));
        setContentPane(this.mainPanel);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
//        try {
//            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//                System.out.println(info);
//                if ("Windows".equals(info.getName())) {
//                    UIManager.setLookAndFeel(info.getClassName());
//                    UIManager.getLookAndFeelDefaults().put("background", Color.DARK_GRAY);
//                }
//            }
//        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
//            e.printStackTrace();
//        }
        SwingUtilities.updateComponentTreeUI(mainPanel);
        pack();
        setVisible(true);
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
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Calibrations", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        calibrationTableScroll = new JScrollPane();
        mainPanel.add(calibrationTableScroll, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 1, false));
        calibrationTableScroll.setViewportView(calibrationTable);
        validateButton = new JButton();
        validateButton.setText("Validate modifications");
        mainPanel.add(validateButton, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addCalibrationPanel = new JPanel();
        addCalibrationPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(addCalibrationPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addCalibrationPanel.setBorder(BorderFactory.createTitledBorder(null, "Add a measureCalibration", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        addNameField = new JTextField();
        addCalibrationPanel.add(addNameField, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        addValueField = new JTextField();
        addCalibrationPanel.add(addValueField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        addUnitField = new JTextField();
        addUnitField.setText("");
        addCalibrationPanel.add(addUnitField, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        addButton = new JButton();
        addButton.setText("Add");
        addCalibrationPanel.add(addButton, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private void createUIComponents() {
        ArrayList<MeasureCalibration> measureCalibrations = MeasureCalibration.getCalibrationFromFile();
        String[] header = new String[]{"Name", "Value", "Unit"};
        Object[][] content = new Object[measureCalibrations.size()][header.length];
        for (int row = 0; row < measureCalibrations.size(); row++) {
            MeasureCalibration measureCalibration = measureCalibrations.get(row);
            content[row][0] = measureCalibration.getName();
            content[row][1] = measureCalibration.getPixelLength();
            content[row][2] = measureCalibration.getUnit();
        }
        tableModel = new DefaultTableModel(content, header);
        calibrationTable = new JTable(tableModel);
    }
}
