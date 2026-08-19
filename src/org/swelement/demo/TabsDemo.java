package org.swelement.demo;

import org.swelement.ui.Tabs;

import javax.swing.*;
import java.awt.*;

public class TabsDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Tabs Demo");
            JPanel p = new JPanel(new FlowLayout(40, 40, 40));
            Tabs tabs = new Tabs();
            for (int i = 1; i <= 4; i++) {
                JLabel l = new JLabel("面板 " + i, SwingConstants.CENTER);
                l.setFont(new Font("Microsoft YaHei", Font.PLAIN, 24));
                tabs.addTab("标签 " + i, l);
            }
            p.add(tabs);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
