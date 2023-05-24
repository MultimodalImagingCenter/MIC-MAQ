package fr.curie.micmaq.helpers;

import fr.curie.micmaq.config.FieldOfView;
import ij.IJ;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterMetadata;
import loci.plugins.in.ImporterOptions;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.EventObject;
import java.util.Vector;
//from  w  w w  .  j a  v a2s. c o  m
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

public class testTree {
    static JTree tree;

    public static void main(String[] args){
        String path="C:\\Users\\messaoudi\\Desktop\\test MIC-MAQ\\lif\\SK28_NT_P-ATM-gH2AX_IF87.lif";
        String arg = "location[local machine] windowless=true groupFiles=true id=[" + path + "]";
        try {
            ImporterOptions options = new ImporterOptions();
            options.parseArg(arg);
            options.setId(path);

            ImportProcess process = new ImportProcess(options);
            process.execute();
            int nSeries = process.getSeriesCount();
            ImporterMetadata md=process.getOriginalMetadata();
            IMetadata imd=process.getOMEMetadata();
            System.out.println(path + " Nseries " + nSeries + " channels:" + process.getCCount(0));
            System.out.println("nb experiment (metadata)="+imd.getExperimentCount());
            System.out.println("nb dataset (metadata)="+imd.getDatasetCount());
            System.out.println("nb image (metadata)="+imd.getImageCount());
            System.out.println("nb experiment (metadata)="+imd.getExperimentCount());
            System.out.println("nb image (metadata)="+imd.getImageCount());
            for(int i=0;i<imd.getImageCount();i++){
                System.out.println("nb planes (metadata)="+imd.getPlaneCount(i));
                int nc=imd.getChannelCount(i);
                System.out.println("nb channel (metadata)="+nc);
                for(int c=0;c<nc;c++){
                    System.out.println("#"+c+" channel name (metadata)="+imd.getChannelName(i,c));
                }
            }


        } catch (FormatException e){

        }  catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main2(String args[]) {
        JFrame frame = new JFrame();
        CheckBoxNode accessibilityOptions[] = { new CheckBoxNode("A", false),
                new CheckBoxNode("B", true) };
        CheckBoxNode browsingOptions[] = { new CheckBoxNode("C", true),
                new CheckBoxNode("D", true), new CheckBoxNode("E", true),
                new CheckBoxNode("F", false) };
        Vector accessVector = new NamedVector("G", accessibilityOptions);
        Vector browseVector = new NamedVector("H", browsingOptions);
        Object rootNodes[] = { accessVector, browseVector };
        Vector rootVector = new NamedVector("Root", rootNodes);
        tree = new JTree(rootVector);

        CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
        tree.setCellRenderer(renderer);

        tree.setCellEditor(new CheckBoxNodeEditor(tree));
        tree.setEditable(true);

        JScrollPane scrollPane = new JScrollPane(tree);
        frame.getContentPane().add(scrollPane, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        JButton button = new JButton("new node");
        buttonPanel.add(button);
        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        button.addActionListener(e -> {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("New node");
            root.add(newNode);
            model.reload();
        });

        frame.setSize(300, 450);
        frame.setVisible(true);
    }
}

class CheckBoxNodeRenderer implements TreeCellRenderer {
    JCheckBox leafRenderer = new JCheckBox();
    DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();
    Color selectionBorderColor, selectionForeground, selectionBackground,
            textForeground, textBackground;

    protected JCheckBox getLeafRenderer() {
        return leafRenderer;
    }

    public CheckBoxNodeRenderer() {
        Font fontValue;
        fontValue = UIManager.getFont("Tree.font");
        if (fontValue != null) {
            leafRenderer.setFont(fontValue);
        }
        Boolean booleanValue = (Boolean) UIManager
                .get("Tree.drawsFocusBorderAroundIcon");
        leafRenderer.setFocusPainted((booleanValue != null)
                && (booleanValue.booleanValue()));

        selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
        selectionForeground = UIManager.getColor("Tree.selectionForeground");
        selectionBackground = UIManager.getColor("Tree.selectionBackground");
        textForeground = UIManager.getColor("Tree.textForeground");
        textBackground = UIManager.getColor("Tree.textBackground");
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {

        Component returnValue;
        if (leaf) {

            String stringValue = tree.convertValueToText(value, selected, expanded,
                    leaf, row, false);
            leafRenderer.setText(stringValue);
            leafRenderer.setSelected(false);

            leafRenderer.setEnabled(tree.isEnabled());

            if (selected) {
                leafRenderer.setForeground(selectionForeground);
                leafRenderer.setBackground(selectionBackground);
            } else {
                leafRenderer.setForeground(textForeground);
                leafRenderer.setBackground(textBackground);
            }

            if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof CheckBoxNode) {
                    CheckBoxNode node = (CheckBoxNode) userObject;
                    leafRenderer.setText(node.getText());
                    leafRenderer.setSelected(node.isSelected());
                }
            }
            returnValue = leafRenderer;
        } else {
            returnValue = nonLeafRenderer.getTreeCellRendererComponent(tree, value,
                    selected, expanded, leaf, row, hasFocus);
        }
        return returnValue;
    }
}

class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {

    CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();

    ChangeEvent changeEvent = null;

    JTree tree;

    public CheckBoxNodeEditor(JTree tree) {
        this.tree = tree;
    }

    public Object getCellEditorValue() {
        JCheckBox checkbox = renderer.getLeafRenderer();
        CheckBoxNode checkBoxNode = new CheckBoxNode(checkbox.getText(),
                checkbox.isSelected());
        return checkBoxNode;
    }
    public boolean isCellEditable(EventObject event) {
        /*boolean returnValue = false;
        if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;
            TreePath path = tree.getPathForLocation(mouseEvent.getX(),
                    mouseEvent.getY());
            if (path != null) {
                Object node = path.getLastPathComponent();
                if ((node != null) && (node instanceof DefaultMutableTreeNode)) {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                    Object userObject = treeNode.getUserObject();
                    returnValue = ((treeNode.isLeaf()) && (userObject instanceof CheckBoxNode));
                }
            }
        }
        return returnValue;*/
        return super.isCellEditable(event);
    }

    public Component getTreeCellEditorComponent(JTree tree, Object value,
                                                boolean selected, boolean expanded, boolean leaf, int row) {

        Component editor = renderer.getTreeCellRendererComponent(tree, value, true,
                expanded, leaf, row, true);
        ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                if (stopCellEditing()) {
                    fireEditingStopped();
                }
            }
        };
        if (editor instanceof JCheckBox) {
            ((JCheckBox) editor).addItemListener(itemListener);
        }
        return editor;
    }
}
class CheckBoxNode {
    String text;
    boolean selected;
    public CheckBoxNode(String text, boolean selected) {
        this.text = text;
        this.selected = selected;
    }
    public boolean isSelected() {
        return selected;
    }
    public void setSelected(boolean newValue) {
        selected = newValue;
    }
    public String getText() {
        return text;
    }
    public void setText(String newValue) {
        text = newValue;
    }
    public String toString() {
        return getClass().getName() + "[" + text + "/" + selected + "]";
    }
}
class NamedVector extends Vector {
    String name;
    public NamedVector(String name) {
        this.name = name;
    }
    public NamedVector(String name, Object elements[]) {
        this.name = name;
        for (int i = 0, n = elements.length; i < n; i++) {
            add(elements[i]);
        }
    }
    public String toString() {
        return "[" + name + "]";
    }
}