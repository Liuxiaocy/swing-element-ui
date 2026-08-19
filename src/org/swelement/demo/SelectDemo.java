package org.swelement.demo;

import org.swelement.ui.Select;
import org.swelement.ui.Select.Option;

import javax.swing.*;
import java.awt.*;

public class SelectDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Select Demo");
            JPanel p = new JPanel(new FlowLayout(40, 40, 40));

            Select single = new Select(false, false);
            single.addOption(new Option("北京", 1));
            single.addOption(new Option("上海", 2));
            single.addOption(new Option("广州", 3));
            p.add(single);

            Select groups = new Select(false, false);
            groups.addOption(new Option("苹果", 1, "水果", false));
            groups.addOption(new Option("香蕉", 2, "水果", false));
            groups.addOption(new Option("白菜", 3, "蔬菜", false));
            groups.addOption(new Option("萝卜", 4, "蔬菜", false));
            p.add(groups);

            Select multi = new Select(true, false);
            multi.addOption(new Option("Red", 1));
            multi.addOption(new Option("Green", 2));
            multi.addOption(new Option("Blue", 3));
            p.add(multi);

            Select search = new Select(true, true);
            for (int i = 1; i <= 10; i++) search.addOption(new Option("选项 " + i, i));
            p.add(search);

            Select disabled = new Select(false, false);
            disabled.addOption(new Option("禁用项", 1));
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
