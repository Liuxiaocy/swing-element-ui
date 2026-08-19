package org.swelement.demo;

import org.swelement.ui.Radio;

import javax.swing.*;
import java.awt.*;

public class RadioDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Radio Demo");
            JPanel p = new JPanel(new FlowLayout(30, 30, 30));
            ButtonGroup group = new ButtonGroup();
            Radio a = new Radio("选项 A");
            Radio b = new Radio("选项 B");
            Radio c = new Radio("选项 C");
            group.add(a); group.add(b); group.add(c);
            p.add(a); p.add(b); p.add(c);
            Radio d = new Radio("禁用");
            d.setEnabled(false);
            p.add(d);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}