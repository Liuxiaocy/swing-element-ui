package org.swelement.demo;

import org.swelement.ui.Switch;

import javax.swing.*;
import java.awt.*;

public class SwitchDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Switch Demo");
            JPanel p = new JPanel(new FlowLayout(30, 30, 30));
            p.add(new Switch());
            Switch on = new Switch();
            on.setSelected(true);
            p.add(on);
            Switch d = new Switch();
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
