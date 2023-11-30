package fr.curie.micmaq.gui;

import fr.curie.micmaq.config.FieldOfViewProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class ComboCheckBox {

    Store[] stores;
    JPanel panel;
    JComboBox combo;
    private class Store{
        String name;
        Boolean state;

        public Store(String name, Boolean state) {
            this.name = name;
            this.state = state;
        }
    }

    class ComboCheckboxRenderer implements ListCellRenderer{
        JCheckBox checkbox;
        public ComboCheckboxRenderer(){
            checkbox = new JCheckBox();
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Store store=(Store)value;
            checkbox.setText(store.name);
            checkbox.setSelected(store.state);
            return checkbox;
        }
    }

    public ComboCheckBox(String[] texts){
        init(texts);
    }

    public ComboCheckBox(FieldOfViewProvider provider){
        int nb= provider.getNumberOfChannel(0);
        String[] names= new String[nb+1];
        names[0]="all";
        for(int i=0;i<nb;i++){
            names[i+1]="C"+(i+1)+"_"+provider.getFieldOfView(0).getChannelUserName(i+1);
        }
        init(names);
    }

    public void init(String[] texts){
        stores=new Store[texts.length];
        for(int i=0;i<texts.length;i++){
            stores[i]=new Store(texts[i],true);
        }
        combo = new JComboBox(stores);
        combo.setRenderer(new ComboCheckboxRenderer());
        combo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                int index=cb.getSelectedIndex();
                Store st=(Store) cb.getSelectedItem();
                st.state=!st.state;
                ComboCheckboxRenderer render = (ComboCheckboxRenderer) cb.getRenderer();
                render.checkbox.setSelected(st.state);
                if(index==0 && st.state){
                    for(int i=1;i<stores.length;i++){
                        stores[i].state=st.state;
                    }
                }else if(index!=0 && !st.state){
                    stores[0].state=false;
                }
            }
        });
        panel=new JPanel();
        panel.add(combo);
    }

    public JPanel getContent(){
        return panel;
    }
    public JComboBox getComboBox(){
        return combo;
    }

    public boolean isSelected(int index){
        return stores[index].state;
    }

    public static void main(String[] args){
        String[] texts= {"C1","C2","C3"};
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel tmp=new JPanel();
        ComboCheckBox combo= new ComboCheckBox(texts);
        tmp.add(combo.getContent());
        JButton but=new JButton("OK");
        but.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(int i=0;i<texts.length;i++){
                    System.out.println(texts[i]+" : "+combo.isSelected(i));
                }
            }
        });
        tmp.add(but);
        f.getContentPane().add(tmp);
        f.setSize(300,160);
        f.setLocation(200,200);
        f.setVisible(true);
    }
}
