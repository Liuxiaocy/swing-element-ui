package org.swelement.demo;

import org.swelement.ui.AstButton;
import org.swelement.ui.AstIcon;

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

            int[] types = {AstButton.DEFAULT, AstButton.PRIMARY, AstButton.SUCCESS, AstButton.WARNING, AstButton.DANGER, AstButton.INFO};
            String[] labels = {"默认", "主要", "成功", "警告", "危险", "信息"};

            // ========== 尺寸 ==========
            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p1.setBorder(new TitledBorder("尺寸 Size"));
            AstButton bl = new AstButton("Large 大按钮", AstButton.PRIMARY, false);
            bl.setSize(AstButton.SIZE_LARGE);
            AstButton bd = new AstButton("Default 默认", AstButton.PRIMARY, false);
            AstButton bs = new AstButton("Small 小按钮", AstButton.PRIMARY, false);
            bs.setSize(AstButton.SIZE_SMALL);
            p1.add(bl); p1.add(bd); p1.add(bs);

            // ========== round 圆角 ==========
            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p2.setBorder(new TitledBorder("圆角 Round"));
            for (int i = 0; i < types.length; i++) {
                AstButton b = new AstButton(labels[i], types[i], false);
                b.setRound(true);
                p2.add(b);
            }

            // ========== circle 圆形 + 图标 ==========
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p3.setBorder(new TitledBorder("圆形 Circle（图标按钮）"));
            AstIcon.Type[] iconTypes = {
                AstIcon.Type.CHECK, AstIcon.Type.CLOSE, AstIcon.Type.STAR,
                AstIcon.Type.SETTING, AstIcon.Type.REFRESH, AstIcon.Type.STAR_FILLED
            };
            int[] ctypes = {AstButton.SUCCESS, AstButton.DANGER, AstButton.WARNING, AstButton.INFO, AstButton.PRIMARY, AstButton.DANGER};
            for (int i = 0; i < iconTypes.length; i++) {
                AstButton b = new AstButton("", ctypes[i], false);
                b.setIcon(new AstIcon(iconTypes[i], Color.WHITE, 16));
                b.setCircle(true);
                p3.add(b);
            }

            // ========== 图标位置 ==========
            JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p4.setBorder(new TitledBorder("图标 Icon（左/右）"));
            AstButton il = new AstButton("图标在左", AstButton.PRIMARY, false);
            il.setIcon(new AstIcon(AstIcon.Type.CHECK, Color.WHITE, 16));
            AstButton ir = new AstButton("图标在右", AstButton.PRIMARY, false);
            ir.setIcon(new AstIcon(AstIcon.Type.ARROW_RIGHT, Color.WHITE, 16));
            ir.setIconPosition(AstButton.ICON_RIGHT);
            p4.add(il); p4.add(ir);

            // ========== loading ==========
            JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p5.setBorder(new TitledBorder("加载中 Loading（点击触发，2秒后恢复）"));
            AstButton loadBtn = new AstButton("点击加载", AstButton.PRIMARY, false);
            loadBtn.addActionListener(e -> {
                loadBtn.setLoading(true);
                Timer t = new Timer(2000, ev -> loadBtn.setLoading(false));
                t.setRepeats(false);
                t.start();
            });
            AstButton loadBtn2 = new AstButton("保存", AstButton.SUCCESS, false);
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
            AstButton tb1 = new AstButton("文本按钮", AstButton.PRIMARY, false);
            tb1.setTextStyle(true);
            AstButton tb2 = new AstButton("禁用文本", AstButton.PRIMARY, false);
            tb2.setTextStyle(true);
            tb2.setEnabled(false);
            AstButton tb3 = new AstButton("圆角文本", AstButton.PRIMARY, false);
            tb3.setTextStyle(true);
            tb3.setRound(true);
            p6.add(tb1); p6.add(tb2); p6.add(tb3);

            // ========== 原有：6种类型 ==========
            JPanel p7 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p7.setBorder(new TitledBorder("按钮类型（原有）"));
            for (int i = 0; i < types.length; i++) p7.add(new AstButton(labels[i], types[i], false));

            // ========== 原有：朴素 + 禁用 ==========
            JPanel p8 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p8.setBorder(new TitledBorder("朴素 Plain + 禁用 Disabled（原有）"));
            p8.add(new AstButton("朴素主要", AstButton.PRIMARY, true));
            AstButton dis = new AstButton("禁用-主要", AstButton.PRIMARY, false);
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
