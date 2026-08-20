package org.swelement.demo;

import org.swelement.ui.Badge;
import org.swelement.ui.Button;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class BadgeDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Badge Demo - 徽标角标、红点、数字缩放弹出动画");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 预先创建几个 Badge 实例方便后面控制
            Badge b1 = new Badge();
            b1.setContent(makeBoxButton("消息", 90, 36));
            b1.setCount(8);

            Badge b2 = new Badge();
            b2.setContent(makeBoxButton("评论", 90, 36));
            b2.setCount(100);

            Badge b3 = new Badge();
            b3.setContent(makeBoxButton("通知", 90, 36));
            b3.setDot(true);

            Badge b4 = new Badge();
            b4.setContent(makeBoxButton("待办", 90, 36));
            b4.setCount(3);

            Badge b5 = new Badge();
            b5.setContent(makeBoxButton("用户头像", 90, 36));
            b5.setCount(999);   // 三位数字：展示圆角宽度自适应

            // ========== 展示区 ==========
            JPanel show = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
            show.setBorder(new TitledBorder("角标展示（数字变化时观察缩放弹出动画）"));
            show.add(wrapBadgeWithLabel(b1, "数字"));
            show.add(wrapBadgeWithLabel(b2, ">99 圆角"));
            show.add(wrapBadgeWithLabel(b3, "红点 dot"));
            show.add(wrapBadgeWithLabel(b4, "小数"));
            show.add(wrapBadgeWithLabel(b5, "三位 999"));

            // ========== 交互控制区 ==========
            JPanel ctrl = new JPanel(new GridBagLayout());
            ctrl.setBorder(new TitledBorder("手动更新角标"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 6, 4, 6);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0; gbc.gridy = 0;
            ctrl.add(new JLabel("消息 count:"), gbc);
            final JSpinner sp1 = new JSpinner(new SpinnerNumberModel(8, 0, 9999, 1));
            sp1.addChangeListener(e -> b1.setCount(((Number) sp1.getValue()).intValue()));
            gbc.gridx = 1; ctrl.add(sp1, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            ctrl.add(new JLabel("评论 count:"), gbc);
            final JSpinner sp2 = new JSpinner(new SpinnerNumberModel(100, 0, 9999, 1));
            sp2.addChangeListener(e -> b2.setCount(((Number) sp2.getValue()).intValue()));
            gbc.gridx = 1; ctrl.add(sp2, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            ctrl.add(new JLabel("通知 dot:"), gbc);
            final JCheckBox dotCb = new JCheckBox("显示红点", true);
            dotCb.addActionListener(e -> b3.setDot(dotCb.isSelected()));
            gbc.gridx = 1; ctrl.add(dotCb, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            ctrl.add(new JLabel("待办 count:"), gbc);
            final JSpinner sp4 = new JSpinner(new SpinnerNumberModel(3, 0, 9999, 1));
            sp4.addChangeListener(e -> b4.setCount(((Number) sp4.getValue()).intValue()));
            gbc.gridx = 1; ctrl.add(sp4, gbc);

            gbc.gridx = 0; gbc.gridy = 4;
            ctrl.add(new JLabel("用户 count:"), gbc);
            final JSpinner sp5 = new JSpinner(new SpinnerNumberModel(999, 0, 9999, 1));
            sp5.addChangeListener(e -> b5.setCount(((Number) sp5.getValue()).intValue()));
            gbc.gridx = 1; ctrl.add(sp5, gbc);

            // 批量 +1 按钮（观察缩放动画）
            gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
            Button plusBtn = new Button("全部 count +1 (看弹出动画)", Button.PRIMARY, false);
            plusBtn.addActionListener(e -> {
                sp1.setValue((int) sp1.getValue() + 1);
                sp2.setValue((int) sp2.getValue() + 1);
                sp4.setValue((int) sp4.getValue() + 1);
                sp5.setValue((int) sp5.getValue() + 1);
            });
            ctrl.add(plusBtn, gbc);

            // 自动递增演示（最右下角的计数）
            final int[] n = {8};
            new Timer(1200, e -> {
                n[0]++;
                b1.setCount(n[0]);
                if (n[0] % 3 == 0) b3.setDot(n[0] % 6 == 0);
            }).start();

            root.add(show);
            root.add(Box.createVerticalStrut(10));
            root.add(ctrl);

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    // 辅助：把 Badge 放中心的 Box 容器按钮
    private static JComponent makeBoxButton(String text, int w, int h) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(w, h));
        return b;
    }

    // 辅助：Badge + 下方说明标签
    private static JComponent wrapBadgeWithLabel(Badge b, String desc) {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(b);
        wrap.add(Box.createVerticalStrut(4));
        JLabel l = new JLabel(desc, SwingConstants.CENTER);
        l.setForeground(new Color(0x909399));
        l.setFont(l.getFont().deriveFont(11f));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrap.add(l);
        return wrap;
    }
}
