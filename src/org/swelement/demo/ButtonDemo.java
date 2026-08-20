package org.swelement.demo;

import org.swelement.ui.Button;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ButtonDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Button Demo - 体验按钮类型、朴素、禁用与过渡动画");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(16, 20, 20, 20));

            // ========== 6 种类型 ==========
            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p1.setBorder(new TitledBorder("按钮类型（悬停 / 按下观察颜色与动画）"));
            int[] types = {Button.DEFAULT, Button.PRIMARY, Button.SUCCESS, Button.WARNING, Button.DANGER, Button.INFO};
            String[] labels = {"默认 Default", "主要 Primary", "成功 Success", "警告 Warning", "危险 Danger", "信息 Info"};
            for (int i = 0; i < types.length; i++) p1.add(new Button(labels[i], types[i], false));

            // ========== 朴素模式 ==========
            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p2.setBorder(new TitledBorder("朴素模式 Plain"));
            p2.add(new Button("朴素 主要", Button.PRIMARY, true));
            p2.add(new Button("朴素 成功", Button.SUCCESS, true));
            p2.add(new Button("朴素 警告", Button.WARNING, true));
            p2.add(new Button("朴素 危险", Button.DANGER, true));
            p2.add(new Button("朴素 信息", Button.INFO, true));

            // ========== 禁用状态 ==========
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p3.setBorder(new TitledBorder("禁用状态 Disabled"));
            Button d1 = new Button("禁用-主要", Button.PRIMARY, false);
            d1.setEnabled(false);
            Button d2 = new Button("禁用-朴素", Button.PRIMARY, true);
            d2.setEnabled(false);
            Button d3 = new Button("禁用-默认", Button.DEFAULT, false);
            d3.setEnabled(false);
            p3.add(d1); p3.add(d2); p3.add(d3);

            // ========== 点击计数（交互） ==========
            JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
            p4.setBorder(new TitledBorder("点击交互测试 - 计数"));
            final int[] count = {0};
            JLabel counter = new JLabel("已点击 0 次");
            counter.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            Button clickBtn = new Button("点我 +1", Button.PRIMARY, false);
            clickBtn.addActionListener(e -> {
                count[0]++;
                counter.setText("已点击 " + count[0] + " 次");
            });
            JButton toggle = new JButton("切换上面按钮 启用/禁用");
            toggle.addActionListener(e -> clickBtn.setEnabled(!clickBtn.isEnabled()));
            p4.add(clickBtn);
            p4.add(counter);
            p4.add(Box.createHorizontalStrut(20));
            p4.add(toggle);

            root.add(p1);
            root.add(Box.createVerticalStrut(6));
            root.add(p2);
            root.add(Box.createVerticalStrut(6));
            root.add(p3);
            root.add(Box.createVerticalStrut(6));
            root.add(p4);

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
