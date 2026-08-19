package org.swelement.demo;

import org.swelement.ui.Pagination;

import javax.swing.*;
import java.awt.*;

public class PaginationDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Pagination Demo");
            JPanel p = new JPanel(new FlowLayout(40, 40, 40));
            Pagination pg = new Pagination();
            pg.setTotal(256);
            JLabel info = new JLabel("当前页: 1");
            pg.addPageChangeListener(v -> info.setText("当前页: " + v));
            p.add(pg);
            p.add(info);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}