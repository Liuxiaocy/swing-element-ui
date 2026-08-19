package org.swelement.demo;

import org.swelement.ui.Menu;

import javax.swing.*;
import java.awt.*;

public class MenuDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Menu Demo");
            JPanel p = new JPanel(new BorderLayout());
            Menu menu = new Menu();
            menu.addMenuItem("首页", () -> System.out.println("home"));
            menu.addMenuItem("新闻", () -> System.out.println("news"));
            menu.addSubMenu("关于", new String[]{"项目", "团队", "联系方式"},
                    new Runnable[]{() -> System.out.println("project"), () -> System.out.println("team"), () -> System.out.println("contact")});
            p.add(menu, BorderLayout.NORTH);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.setSize(640, 200);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}