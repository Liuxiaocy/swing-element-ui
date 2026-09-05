package org.swelement.demo;

import org.swelement.core.theme.ThemeManager;
import org.swelement.ui.AstButton;
import org.swelement.ui.AstEmpty;
import org.swelement.ui.AstIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class AstEmptyDemo {
    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstEmpty Demo - 默认插图 / AstIcon 插图 / 带操作按钮");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(buildPanel());
            f.pack();
            f.setSize(Math.max(f.getWidth(), 760), Math.max(f.getHeight(), 420));
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    static JComponent buildPanel() {
        JPanel root = new JPanel(new GridLayout(1, 3, 16, 16));
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.setBackground(Color.WHITE);

        JPanel p1 = new JPanel(new BorderLayout());
        p1.setBorder(new TitledBorder("默认插图"));
        p1.setBackground(Color.WHITE);
        p1.add(new AstEmpty("暂无数据"), BorderLayout.CENTER);

        JPanel p2 = new JPanel(new BorderLayout());
        p2.setBorder(new TitledBorder("AstIcon 插图 + 描述"));
        p2.setBackground(Color.WHITE);
        AstEmpty e2 = new AstEmpty("没有找到相关内容");
        e2.setIconType(AstIcon.SEARCH);
        e2.setIconSize(72);
        p2.add(e2, BorderLayout.CENTER);

        JPanel p3 = new JPanel(new BorderLayout());
        p3.setBorder(new TitledBorder("带操作按钮"));
        p3.setBackground(Color.WHITE);
        AstEmpty e3 = new AstEmpty("网络异常，加载失败");
        e3.setIconType(AstIcon.WARNING);
        e3.setAction(new AstButton("重试", AstButton.PRIMARY, false));
        p3.add(e3, BorderLayout.CENTER);

        root.add(p1); root.add(p2); root.add(p3);
        return root;
    }

    static void selfCheck() {
        ThemeManager.ensureDefaultTheme();
        JComponent panel = buildPanel();
        panel.setSize(panel.getPreferredSize());
        BufferedImage img = new BufferedImage(Math.max(1, panel.getWidth()), Math.max(1, panel.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { panel.paint(g); } finally { g.dispose(); }
        System.out.println("AstEmptyDemo self-check OK");
    }
}
