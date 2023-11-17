package fr.curie.micmaq.config;

import ij.IJ;
import loci.formats.FormatException;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Vector;

public class ImagesTree extends JTree {
    FieldOfViewProvider provider;
    private ArrayList<CheckBoxNode> nodes;
    int firstIndex=0;
    boolean toggleAll=true;

    KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            super.keyTyped(e);
            //System.out.println("key event ");
            System.out.println(getRowCount());
            if (e.getKeyChar() == KeyEvent.VK_SPACE) {
                System.out.println("key space");
                int row=getSelectionRows()[0];
                System.out.println("current node "+ row);
                if(e.getModifiers() == 0 && row>0){
                    DefaultMutableTreeNode child=(DefaultMutableTreeNode) getLastSelectedPathComponent();
                    //System.out.println("name="+ child.toString());
                    ((CheckBoxNode)child.getUserObject()).toggleSelection();
                    //System.out.println("after toggle name="+ child.toString());
                }else if (e.getModifiers() == KeyEvent.SHIFT_MASK) {
                    System.out.println("shift key");
                    toggleAll=!toggleAll;
                    for(int i=1;i<getRowCount();i++){
                        DefaultMutableTreeNode child=(DefaultMutableTreeNode) getPathForRow(i).getLastPathComponent();
                        System.out.println("name="+ child.toString());
                        ((CheckBoxNode)child.getUserObject()).setSelected(toggleAll);
                    }
                }
                repaint();
            }
        }
    };


    public ImagesTree(FieldOfViewProvider provider){
        super(new DefaultTreeModel(new DefaultMutableTreeNode("root")));
        this.provider=provider;
        createTree();
        addKeyListener(keyListener);
        setRootVisible(true);
    }

    /*public void createTree(){
        if(provider==null) return;
        DefaultMutableTreeNode root=(DefaultMutableTreeNode)getModel().getRoot();
        for(int i=0;i<provider.getNbFielOfView();i++) {
            FieldOfView fov=provider.getFieldOfView(i);
            DefaultMutableTreeNode child=new CheckBoxNode(fov);
            for(int c=1;c<=fov.getNbAvailableChannels();c++){
                child.add(new DefaultMutableTreeNode(fov.getChannelName(c)));
            }
            root.add(child);
        }
        ((DefaultTreeModel)getModel()).nodeStructureChanged(root);
        setCellRenderer(new CheckBoxNodeRenderer());
        setCellEditor(new CheckBoxNodeEditor(this));
    }*/

    public void createTree(){
        if(provider==null) return;
        firstIndex=-1;
        //IJ.log("########## create Tree ############");
        nodes = new ArrayList<>();
        DefaultMutableTreeNode root=(DefaultMutableTreeNode)getModel().getRoot();
        ArrayList<FieldOfView> fovs=provider.getAllFields();
        for(int i=0;i<fovs.size();i++){
            FieldOfView fov=fovs.get(i);
            CheckBoxNode tmp=new CheckBoxNode("#"+i+"_"+fov.getFieldname(),fov.isUsed());
            if(firstIndex<0 && fov.isUsed()) firstIndex=i;
            //IJ.log(fov.getFieldname()+ " is selected: "+fov.isUsed() + "/" +tmp.isSelected());
            nodes.add(tmp);
            DefaultMutableTreeNode child=new DefaultMutableTreeNode(fov.getFieldname());
            for(int c=1;c<=fov.getNbAvailableChannels();c++){
                child.add(new DefaultMutableTreeNode(fov.getChannelName(c)));
            }
            child.setUserObject(tmp);

            root.add(child);
            //root.add(new DefaultMutableTreeNode(fov.getFieldname()));
        }
        ((DefaultTreeModel)getModel()).reload();
        CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();


        setCellRenderer(renderer);
        setCellEditor(new CheckBoxNodeEditor(this));
        setEditable(true);

        //repaint();
    }

    public void validateSelections(){
        DefaultMutableTreeNode root=(DefaultMutableTreeNode)getModel().getRoot();
        IJ.log("tree children "+root.getChildCount());
        for(int i=0;i< root.getChildCount();i++){
            DefaultMutableTreeNode child=(DefaultMutableTreeNode)root.getChildAt(i);
            Object userObject = child.getUserObject();
            CheckBoxNode node = (CheckBoxNode) userObject;
            provider.getFieldOfView(i).setUsed(node.isSelected());
            IJ.log("field #"+i+" "+provider.getFieldOfView(i).getFieldname()+ " is "+provider.getFieldOfView(i).isUsed());
        }
    }



    public void updateTree(){
        DefaultMutableTreeNode root=(DefaultMutableTreeNode)getModel().getRoot();
        root.removeAllChildren();
        createTree();
        //repaint();
    }

    public void setFieldOfViewProvider(FieldOfViewProvider provider) {
        this.provider = provider;
        updateTree();
    }

    public int getFirstIndex() {
        return firstIndex;
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
            if (!leaf&& value!=tree.getModel().getRoot()) {

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
                        //IJ.log("instance of checkbox node "+node.getText()+": selected "+node.isSelected());
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
                    System.out.println("tree item change");
                    if (stopCellEditing()) {
                        fireEditingStopped();
                    }
                }
            };

            if (editor instanceof JCheckBox) {
                JCheckBox jc= (JCheckBox) editor;
                if(jc.getItemListeners().length==0) {
                    System.out.println("add custom listener");
                    jc.addItemListener(itemListener);
                }else{
                    System.out.println("no listener needed");
                }
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

        public void toggleSelection(){
            selected=!selected;
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
}
