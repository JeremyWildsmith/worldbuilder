package io.github.jevaengine.builder.worldbuilder.ui;

import java.awt.Component;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.event.*;

@SuppressWarnings("serial")
public class JCheckBoxList<T extends Comparable<T>> extends JList<JCheckBoxList.Datum<T>> {
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public JCheckBoxList() {
        setCellRenderer(new CellRenderer());
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    int index = locationToIndex(e.getPoint());
                    if (index != -1) {
                        JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index).checkBox;
                        checkbox.setSelected(!checkbox.isSelected());
                        repaint();
                    }
                }
            }
        });
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public JCheckBoxList(ListModel<Datum<T>> model){
        this();
        setModel(model);
    }

    protected class CellRenderer<T extends Comparable<T>> implements ListCellRenderer<Datum<T>> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Datum<T>> jList, Datum<T> value, int i, boolean isSelected, boolean b1) {
            JCheckBox checkbox = value.checkBox;

            //Drawing checkbox, change the appearance here
            checkbox.setBackground(isSelected ? getSelectionBackground()
                    : getBackground());
            checkbox.setForeground(isSelected ? getSelectionForeground()
                    : getForeground());
            checkbox.setEnabled(isEnabled());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected ? UIManager
                    .getBorder("List.focusCellHighlightBorder") : noFocusBorder);
            return checkbox;
        }
    }

    public static class Datum<T extends Comparable<T>> implements Comparable<Datum<T>> {
        private JCheckBox checkBox;
        public T info;

        public Datum(T info, boolean checked) {
            this.checkBox = new JCheckBox();
            this.checkBox.setLabel(info.toString());
            this.info = info;

            this.checkBox.setSelected(checked);
        }

        @Override
        public int compareTo(Datum<T> tDatum) {
            return info.compareTo(tDatum.info);
        }

        public boolean isChecked() {
            return checkBox.isSelected();
        }
    }
}