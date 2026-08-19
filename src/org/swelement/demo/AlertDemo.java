package org.swelement.demo;

import org.swelement.ui.Alert;

import javax.swing.*;
import java.awt.*;

public class AlertDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Alert Demo");
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            p.add(new Alert(Alert.SUCCESS, "成功提示", "这是一条成功提示信息", true));
            p.add(Box.createVerticalStrut(10));
            p.add(new Alert(Alert.WARNING, "警告提示", "这是一条警告提示信息", true));
            p.add(Box.createVerticalStrut(10));
            p.add(new Alert(Alert.INFO, "消息提示", null, false));
            p.add(Box.createVerticalStrut(10));
            p.add(new Alert(Alert.ERROR, "错误提示", "这是一条错误提示信息", true));
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}