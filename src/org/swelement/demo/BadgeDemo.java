package org.swelement.demo;

import org.swelement.ui.Badge;

import javax.swing.*;
import java.awt.*;

public class BadgeDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Badge Demo");
            JPanel p = new JPanel(new FlowLayout(60, 40, 40));
            Badge b1 = new Badge();
            b1.setContent(new JButton("消息"));
            b1.setCount(8);
            p.add(b1);
            Badge b2 = new Badge();
            b2.setContent(new JButton("评论"));
            b2.setCount(100);
            p.add(b2);
            Badge b3 = new Badge();
            b3.setContent(new JButton("通知"));
            b3.setDot(true);
            p.add(b3);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            int[] n = {8};
            new Timer(1200, e -> {
                n[0]++;
                b1.setCount(n[0]);
                if (n[0] % 3 == 0) b3.setDot(n[0] % 6 == 0);
            }).start();
        });
    }
}
