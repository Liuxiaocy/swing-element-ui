package org.swelement.demo;

import org.swelement.ui.Checkbox;

import javax.swing.*;
import java.awt.*;

public class CheckboxDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Checkbox Demo");
            JPanel p = new JPanel(new FlowLayout(30, 30, 30));
            p.add(new Checkbox("默认"));
            p.add(new Checkbox("已选"));
            Checkbox c = new Checkbox("已选");
            c.setSelected(true);
            p.add(c);
            Checkbox d = new Checkbox("禁用");
            d.setEnabled(false);
            p.add(d);
            Checkbox e = new Checkbox("选中禁用");
            e.setEnabled(false);
            e.setSelected(true);
            p.add(e);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
