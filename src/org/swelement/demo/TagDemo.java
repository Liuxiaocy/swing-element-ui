package org.swelement.demo;

import org.swelement.ui.Tag;

import javax.swing.*;
import java.awt.*;

public class TagDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Tag Demo");
            JPanel p = new JPanel(new FlowLayout(20, 20, 20));
            p.add(new Tag("默认", Tag.PRIMARY, false));
            p.add(new Tag("成功", Tag.SUCCESS, false));
            p.add(new Tag("警告", Tag.WARNING, false));
            p.add(new Tag("危险", Tag.DANGER, true));
            p.add(new Tag("信息", Tag.INFO, false));
            p.add(new Tag("可删除", Tag.DANGER, true));
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
