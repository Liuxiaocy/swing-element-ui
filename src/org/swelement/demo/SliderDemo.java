package org.swelement.demo;

import org.swelement.ui.Slider;

import javax.swing.*;
import java.awt.*;

public class SliderDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Slider Demo");
            JPanel p = new JPanel(new FlowLayout(30, 30, 30));
            Slider s = new Slider(0, 100, 40);
            JLabel label = new JLabel("40");
            label.setPreferredSize(new Dimension(40, 24));
            s.addChangeListener(e -> label.setText(String.valueOf(s.getValue())));
            p.add(s);
            p.add(label);
            Slider d = new Slider(0, 100, 30);
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
