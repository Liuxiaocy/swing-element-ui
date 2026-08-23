package org.swelement.demo;

import org.swelement.ui.Button;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ButtonDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Button Demo - 尺寸/圆角/圆形/图标/加载/文本按钮");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(16, 20, 20, 20));

            int[] types = {Button.DEFAULT, Button.PRIMARY, Button.SUCCESS, Button.WARNING, Button.DANGER, Button.INFO};
            String[] labels = {"默认", "主要", "成功", "警告", "危险", "信息"};

            // ========== 尺寸 ==========
            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p1.setBorder(new TitledBorder("尺寸 Size"));
            Button bl = new Button("Large 大按钮", Button.PRIMARY, false);
            bl.setSize(Button.SIZE_LARGE);
            Button bd = new Button("Default 默认", Button.PRIMARY, false);
            Button bs = new Button("Small 小按钮", Button.PRIMARY, false);
            bs.setSize(Button.SIZE_SMALL);
            p1.add(bl); p1.add(bd); p1.add(bs);

            // ========== round 圆角 ==========
            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p2.setBorder(new TitledBorder("圆角 Round"));
            for (int i = 0; i < types.length; i++) {
                Button b = new Button(labels[i], types[i], false);
                b.setRound(true);
                p2.add(b);
            }

            // ========== circle 圆形 + 图标 ==========
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p3.setBorder(new TitledBorder("圆形 Circle（图标按钮）"));
            String[] icons = {"\u2713", "\u2717", "\u2605", "\u2699", "\u21bb", "\u2764"};
            int[] ctypes = {Button.SUCCESS, Button.DANGER, Button.WARNING, Button.INFO, Button.PRIMARY, Button.DANGER};
            for (int i = 0; i < icons.length; i++) {
                Button b = new Button("", ctypes[i], false);
                b.setIcon(icons[i]);
                b.setCircle(true);
                p3.add(b);
            }

            // ========== 图标位置 ==========
            JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p4.setBorder(new TitledBorder("图标 Icon（左/右）"));
            Button il = new Button("图标在左", Button.PRIMARY, false);
            il.setIcon("\u2713");
            Button ir = new Button("图标在右", Button.PRIMARY, false);
            ir.setIcon("\u2192");
            ir.setIconPosition(Button.ICON_RIGHT);
            p4.add(il); p4.add(ir);

            // ========== loading ==========
            JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p5.setBorder(new TitledBorder("加载中 Loading（点击触发，2秒后恢复）"));
            Button loadBtn = new Button("点击加载", Button.PRIMARY, false);
            loadBtn.addActionListener(e -> {
                loadBtn.setLoading(true);
                Timer t = new Timer(2000, ev -> loadBtn.setLoading(false));
                t.setRepeats(false);
                t.start();
            });
            Button loadBtn2 = new Button("保存", Button.SUCCESS, false);
            loadBtn2.setLoadingText("保存中...");
            loadBtn2.addActionListener(e -> {
                loadBtn2.setLoading(true);
                Timer t = new Timer(2000, ev -> loadBtn2.setLoading(false));
                t.setRepeats(false);
                t.start();
            });
            p5.add(loadBtn); p5.add(loadBtn2);

            // ========== text 文本按钮 ==========
            JPanel p6 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p6.setBorder(new TitledBorder("文本按钮 Text"));
            Button tb1 = new Button("文本按钮", Button.PRIMARY, false);
            tb1.setTextButton(true);
            Button tb2 = new Button("禁用文本", Button.PRIMARY, false);
            tb2.setTextButton(true);
            tb2.setEnabled(false);
            Button tb3 = new Button("圆角文本", Button.PRIMARY, false);
            tb3.setTextButton(true);
            tb3.setRound(true);
            p6.add(tb1); p6.add(tb2); p6.add(tb3);

            // ========== 原有：6种类型 ==========
            JPanel p7 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p7.setBorder(new TitledBorder("按钮类型（原有）"));
            for (int i = 0; i < types.length; i++) p7.add(new Button(labels[i], types[i], false));

            // ========== 原有：朴素 + 禁用 ==========
            JPanel p8 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p8.setBorder(new TitledBorder("朴素 Plain + 禁用 Disabled（原有）"));
            p8.add(new Button("朴素主要", Button.PRIMARY, true));
            Button dis = new Button("禁用-主要", Button.PRIMARY, false);
            dis.setEnabled(false);
            p8.add(dis);

            root.add(p1);
            root.add(Box.createVerticalStrut(6));
            root.add(p2);
            root.add(Box.createVerticalStrut(6));
            root.add(p3);
            root.add(Box.createVerticalStrut(6));
            root.add(p4);
            root.add(Box.createVerticalStrut(6));
            root.add(p5);
            root.add(Box.createVerticalStrut(6));
            root.add(p6);
            root.add(Box.createVerticalStrut(6));
            root.add(p7);
            root.add(Box.createVerticalStrut(6));
            root.add(p8);

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
