package org.swelement.demo;

import org.swelement.ui.Progress;

import javax.swing.*;
import java.awt.*;

public class ProgressDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Progress Demo");
            JPanel p = new JPanel(new GridLayout(4, 1, 10, 10));
            p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            Progress a = new Progress();
            p.add(a);
            Progress b = new Progress();
            b.setShowText(false);
            p.add(b);
            Progress c = new Progress();
            p.add(c);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            int[] counter = {0};
            new Timer(60, e -> {
                counter[0]++;
                a.setValue(counter[0] % 101);
                b.setValue((counter[0] + 30) % 100);
                c.setValue((int) (Math.random() * 100));
            }).start();
        });
    }
}
