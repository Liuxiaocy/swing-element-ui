package org.swelement.demo;

import org.swelement.ui.Input;

import javax.swing.*;
import java.awt.*;

public class InputDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Input Demo");
            JPanel p = new JPanel(new FlowLayout(30, 30, 30));
            Input a = new Input("请输入内容");
            a.setPreferredSize(new Dimension(220, 40));
            Input b = new Input("disabled");
            b.setPreferredSize(new Dimension(220, 40));
            b.setEnabled(false);
            p.add(a);
            p.add(b);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
