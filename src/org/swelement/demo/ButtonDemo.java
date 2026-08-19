package org.swelement.demo;

import org.swelement.ui.Button;

import javax.swing.*;
import java.awt.*;

public class ButtonDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Button Demo");
            JPanel p = new JPanel(new FlowLayout(20, 20, 20));
            int[] types = {Button.DEFAULT, Button.PRIMARY, Button.SUCCESS, Button.WARNING, Button.DANGER, Button.INFO};
            String[] labels = {"默认按钮", "主要按钮", "成功按钮", "警告按钮", "危险按钮", "信息按钮"};
            for (int i = 0; i < types.length; i++) p.add(new Button(labels[i], types[i], false));
            p.add(new Button("朴素按钮", Button.PRIMARY, true));
            Button disabled = new Button("禁用按钮", Button.PRIMARY, false);
            disabled.setEnabled(false);
            p.add(disabled);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}