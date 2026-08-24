package org.swelement.demo;

import org.swelement.ui.AstBadge;
import org.swelement.ui.AstButton;
import org.swelement.ui.AstInput;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class BadgeDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstBadge Demo - 徽标角标、红点、多组件类型");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // ========== 第一组：Button 上角标 ==========
            AstBadge b1 = new AstBadge();
            b1.setContent(new AstButton("消息", AstButton.DEFAULT, false));
            b1.setCount(8);

            AstBadge b2 = new AstBadge();
            b2.setContent(new AstButton("评论", AstButton.DEFAULT, false));
            b2.setCount(100);

            AstBadge b3 = new AstBadge();
            b3.setContent(new AstButton("通知", AstButton.PRIMARY, false));
            b3.setDot(true);

            AstBadge b4 = new AstBadge();
            b4.setContent(new AstButton("待办", AstButton.WARNING, false));
            b4.setCount(3);

            AstBadge b5 = new AstBadge();
            b5.setContent(new AstButton("用户中心", AstButton.SUCCESS, false));
            b5.setCount(999);

            JPanel show1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
            show1.setBorder(new TitledBorder("Button 组件角标（数字变化时观察缩放弹出动画）"));
            show1.add(wrapBadgeWithLabel(b1, "数字 8"));
            show1.add(wrapBadgeWithLabel(b2, ">99 圆角 99+"));
            show1.add(wrapBadgeWithLabel(b3, "红点 dot"));
            show1.add(wrapBadgeWithLabel(b4, "小数 3"));
            show1.add(wrapBadgeWithLabel(b5, "三位 999"));

            // ========== 第二组：其他类型组件角标 ==========
            // AstInput 输入框角标
            AstBadge onInput = new AstBadge();
            AstInput input = new AstInput("搜索关键词...");
            input.setColumns(14);
            onInput.setContent(input);
            onInput.setCount(5);

            // JLabel（头像/图标）角标
            AstBadge onAvatar = new AstBadge();
            JLabel avatar = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0x409EFF));
                    g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("👤", (getWidth() - fm.stringWidth("👤")) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                    g2.dispose();
                }
            };
            avatar.setPreferredSize(new Dimension(56, 56));
            onAvatar.setContent(avatar);
            onAvatar.setDot(true);

            // JPanel（色块图标）角标
            AstBadge onIcon = new AstBadge();
            JPanel iconBox = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xF56C6C));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
                    FontMetrics fm = g2.getFontMetrics();
                    String icon = "🔔";
                    g2.drawString(icon, (getWidth() - fm.stringWidth(icon)) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                    g2.dispose();
                }
            };
            iconBox.setPreferredSize(new Dimension(56, 56));
            onIcon.setContent(iconBox);
            onIcon.setCount(12);

            // AstCheckbox 角标（带勾选状态指示）
            AstBadge onCheckbox = new AstBadge();
            JCheckBox cb = new JCheckBox("新功能上线");
            cb.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            cb.setPreferredSize(new Dimension(140, 28));
            onCheckbox.setContent(cb);
            onCheckbox.setDot(true);

            // JLabel（纯文字标签）角标
            AstBadge onLabel = new AstBadge();
            JLabel textLabel = new JLabel("📋 待审文件") {
                @Override
                public void paintComponent(Graphics g) {
                    g.setColor(new Color(0x909399));
                    g.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
                    super.paintComponent(g);
                }
            };
            textLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            textLabel.setForeground(new Color(0x303133));
            textLabel.setPreferredSize(new Dimension(120, 28));
            onLabel.setContent(textLabel);
            onLabel.setCount(23);

            JPanel show2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 14));
            show2.setBorder(new TitledBorder("不同组件类型角标（角标均显示在组件右上角外部，不遮挡内容）"));
            show2.add(wrapBadgeWithLabel(onInput, "AstInput 输入框"));
            show2.add(wrapBadgeWithLabel(onAvatar, "JLabel 头像 dot"));
            show2.add(wrapBadgeWithLabel(onIcon, "JPanel 图标"));
            show2.add(wrapBadgeWithLabel(onCheckbox, "AstCheckbox 复选框"));
            show2.add(wrapBadgeWithLabel(onLabel, "JLabel 文字标签"));

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

            gbc.gridx = 0; gbc.gridy = 5;
            ctrl.add(new JLabel("图标 count:"), gbc);
            final JSpinner sp6 = new JSpinner(new SpinnerNumberModel(12, 0, 9999, 1));
            sp6.addChangeListener(e -> onIcon.setCount(((Number) sp6.getValue()).intValue()));
            gbc.gridx = 1; ctrl.add(sp6, gbc);

            gbc.gridx = 0; gbc.gridy = 6;
            ctrl.add(new JLabel("头像 dot:"), gbc);
            final JCheckBox dotCb2 = new JCheckBox("显示红点", true);
            dotCb2.addActionListener(e -> onAvatar.setDot(dotCb2.isSelected()));
            gbc.gridx = 1; ctrl.add(dotCb2, gbc);

            gbc.gridx = 0; gbc.gridy = 7;
            ctrl.add(new JLabel("标签 count:"), gbc);
            final JSpinner sp7 = new JSpinner(new SpinnerNumberModel(23, 0, 9999, 1));
            sp7.addChangeListener(e -> onLabel.setCount(((Number) sp7.getValue()).intValue()));
            gbc.gridx = 1; ctrl.add(sp7, gbc);

            // 批量 +1 按钮
            gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
            AstButton plusBtn = new AstButton("全部 count +1 (看弹出动画)", AstButton.PRIMARY, false);
            plusBtn.addActionListener(e -> {
                sp1.setValue((int) sp1.getValue() + 1);
                sp2.setValue((int) sp2.getValue() + 1);
                sp4.setValue((int) sp4.getValue() + 1);
                sp5.setValue((int) sp5.getValue() + 1);
                sp6.setValue((int) sp6.getValue() + 1);
                sp7.setValue((int) sp7.getValue() + 1);
            });
            ctrl.add(plusBtn, gbc);

            // 自动递增演示
            final int[] n = {8};
            new Timer(1500, e -> {
                n[0]++;
                b1.setCount(n[0]);
                if (n[0] % 3 == 0) b3.setDot(n[0] % 6 == 0);
            }).start();

            root.add(show1);
            root.add(Box.createVerticalStrut(10));
            root.add(show2);
            root.add(Box.createVerticalStrut(10));
            root.add(ctrl);

            f.setContentPane(new JScrollPane(root));
            f.pack();
            f.setSize(Math.max(f.getWidth(), 920), Math.min(f.getHeight(), 750));
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    private static JComponent wrapBadgeWithLabel(AstBadge b, String desc) {
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
