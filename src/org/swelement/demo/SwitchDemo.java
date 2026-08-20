package org.swelement.demo;

import org.swelement.ui.Button;
import org.swelement.ui.Switch;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemListener;

public class SwitchDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Switch Demo - 开关滑动 + 底色渐变过渡动画");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 基础：多行功能开关
            JPanel p1 = new JPanel();
            p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
            p1.setBorder(new TitledBorder("功能开关（点击开关观察旋钮 22→40px 滑动 + 颜色渐变；下方文字也会实时更新状态）"));

            // 用方法而非局部 BiFunction lambda 以便 Java 8 编译
            p1.add(makeRow("🔔 接收消息通知", true));
            p1.add(makeRow("🌙 夜间模式", false));
            p1.add(makeRow("🔊 声音提醒", true));
            p1.add(makeRow("📱 自动登录", false));
            p1.add(makeRow("🕶  隐私模式（匿名浏览）", false));

            // 禁用展示
            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 0));
            p2.setBorder(new TitledBorder("禁用状态（不可点击，底色/旋钮呈灰色）"));
            Switch off = new Switch();
            off.setEnabled(false);
            Switch on = new Switch();
            on.setSelected(true);
            on.setEnabled(false);
            p2.add(wrapLabeled(off, "禁用-关"));
            p2.add(wrapLabeled(on, "禁用-开"));

            // 集中控制：全部开关一次性切换
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            p3.setBorder(new TitledBorder("外部按钮控制"));
            Button toggleAll = new Button("全部开/关 切换一次", Button.PRIMARY, false);
            final boolean[] allState = {false};
            toggleAll.addActionListener(e -> {
                allState[0] = !allState[0];
                applyToAllSwitches(p1, allState[0]);
            });
            Button openAll = new Button("全部开启", Button.SUCCESS, true);
            openAll.addActionListener(e -> applyToAllSwitches(p1, true));
            Button closeAll = new Button("全部关闭", Button.DANGER, true);
            closeAll.addActionListener(e -> applyToAllSwitches(p1, false));
            p3.add(toggleAll);
            p3.add(openAll);
            p3.add(closeAll);

            root.add(p1);
            root.add(Box.createVerticalStrut(8));
            root.add(p2);
            root.add(Box.createVerticalStrut(8));
            root.add(p3);

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    /** 创建一行：左侧文字 + 右侧 Switch。文字实时显示开关状态。 */
    private static JComponent makeRow(String baseLabel, boolean init) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel left = new JLabel(baseLabel + "  ——  " + (init ? "✅ 已开启" : "⛔ 已关闭"));
        left.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        row.add(left, BorderLayout.WEST);
        Switch sw = new Switch();
        sw.setSelected(init);
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrap.add(sw);
        row.add(wrap, BorderLayout.EAST);
        // 状态变更监听
        ItemListener listener = e -> {
            String s = sw.isSelected() ? "✅ 已开启" : "⛔ 已关闭";
            left.setText(baseLabel + "  ——  " + s);
        };
        sw.addItemListener(listener);
        // 便于外部遍历查找（putClientProperty 记录引用）
        sw.putClientProperty("switch.row.label", left);
        sw.putClientProperty("switch.base.label", baseLabel);
        return row;
    }

    /** 递归查找 p1 中的所有 Switch 并统一设置选中值 */
    private static void applyToAllSwitches(Container root, boolean selected) {
        for (Component c : root.getComponents()) {
            if (c instanceof Switch) {
                Switch s = (Switch) c;
                s.setSelected(selected);
                // 手动触发一次文字刷新（ItemListener only fires on user click by default）
                JLabel label = (JLabel) s.getClientProperty("switch.row.label");
                String base = (String) s.getClientProperty("switch.base.label");
                if (label != null && base != null) {
                    label.setText(base + "  ——  " + (selected ? "✅ 已开启" : "⛔ 已关闭"));
                }
            } else if (c instanceof Container) {
                applyToAllSwitches((Container) c, selected);
            }
        }
    }

    private static JComponent wrapLabeled(Switch sw, String label) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(sw);
        p.add(Box.createVerticalStrut(4));
        JLabel l = new JLabel(label);
        l.setForeground(new Color(0x909399));
        l.setFont(l.getFont().deriveFont(11f));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(l);
        return p;
    }
}
