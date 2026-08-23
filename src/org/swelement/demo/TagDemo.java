package org.swelement.demo;

import org.swelement.ui.Button;
import org.swelement.ui.Tag;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TagDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Tag Demo - effect 三种效果、尺寸、可关闭（真实可点 CloseButton）");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 1. light 效果（默认，向后兼容）
            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            p1.setBorder(new TitledBorder("light 效果（浅底 + 深色文字，默认）"));
            p1.add(new Tag("Primary 主要", Tag.PRIMARY, false));
            p1.add(new Tag("Success 成功", Tag.SUCCESS, false));
            p1.add(new Tag("Warning 警告", Tag.WARNING, false));
            p1.add(new Tag("Danger 危险", Tag.DANGER, false));
            p1.add(new Tag("Info 信息", Tag.INFO, false));

            // 2. dark 效果（实色底白字）
            JPanel p1b = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            p1b.setBorder(new TitledBorder("dark 效果（实色底，Element 标准实心设计）"));
            for (int t = 0; t < 5; t++) {
                Tag tag = new Tag("dark-" + t, t, false);
                tag.setEffect(Tag.EFFECT_DARK);
                p1b.add(tag);
            }

            // 3. plain 效果（白底彩边）
            JPanel p1c = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            p1c.setBorder(new TitledBorder("plain 效果（白底 + 彩色边框）"));
            for (int t = 0; t < 5; t++) {
                Tag tag = new Tag("plain-" + t, t, false);
                tag.setEffect(Tag.EFFECT_PLAIN);
                p1c.add(tag);
            }

            // 4. 尺寸三档
            JPanel p1d = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            p1d.setBorder(new TitledBorder("尺寸三档（large / default / small）"));
            String[] names = {"large 大", "default 默认", "small 小"};
            int[] sizes = {Tag.SIZE_LARGE, Tag.SIZE_DEFAULT, Tag.SIZE_SMALL};
            for (int i = 0; i < 3; i++) {
                Tag tag = new Tag(names[i], Tag.PRIMARY, true);
                tag.setSize(sizes[i]);
                p1d.add(tag);
            }

            // 5. 可关闭标签区（CloseButton 点击关闭，动画后从容器移除）
            JPanel p2Wrap = new JPanel(new BorderLayout());
            p2Wrap.setBorder(new TitledBorder("可关闭标签（点击 × 观察宽度收缩动画，关闭后从面板移除）"));
            final JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            p2.setOpaque(true);
            p2.setBackground(Color.WHITE);
            List<String> initTags = java.util.Arrays.asList(
                    "🚀 Java", "⚛ React", "🎨 设计", "📊 数据可视化",
                    "🔧 DevOps", "🧪 测试", "☁️ 云计算", "🤖 AI/ML", "📱 移动端", "🌐 网络"
            );
            for (String s : initTags) {
                p2.add(makeClosableTag(s, p2.getComponentCount() % 5, p2));
            }
            p2Wrap.add(p2, BorderLayout.CENTER);

            // 6. 动态添加区
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            p3.setBorder(new TitledBorder("动态添加标签（点击按钮添加，类型循环）"));
            JTextField tf = new JTextField(16);
            tf.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            tf.setText("新标签");
            Button add = new Button("+ 添加可关闭标签", Button.PRIMARY, false);
            final int[] addIdx = {0};
            add.addActionListener(ev -> {
                String txt = tf.getText().trim();
                if (txt.isEmpty()) return;
                p2.add(makeClosableTag(txt, addIdx[0]++ % 5, p2));
                p2.revalidate();
            });
            Button clear = new Button("清空全部（带动画）", Button.WARNING, true);
            clear.addActionListener(ev -> {
                List<Component> tags = new ArrayList<Component>();
                for (Component c : p2.getComponents()) if (c instanceof Tag) tags.add(c);
                int delay = 0;
                for (Component c : tags) {
                    final Tag t = (Tag) c;
                    Timer timer = new Timer(delay, e -> t.close(() -> SwingUtilities.invokeLater(() -> {
                        p2.remove(t);
                        p2.revalidate();
                        p2.repaint();
                    })));
                    timer.setRepeats(false);
                    timer.start();
                    delay += 60;
                }
            });
            p3.add(new JLabel("标签文字:"));
            p3.add(tf);
            p3.add(add);
            p3.add(Box.createHorizontalStrut(20));
            p3.add(clear);

            root.add(p1);
            root.add(p1b);
            root.add(p1c);
            root.add(p1d);
            root.add(Box.createVerticalStrut(8));
            root.add(p2Wrap);
            root.add(Box.createVerticalStrut(8));
            root.add(p3);

            f.setContentPane(root);
            f.pack();
            f.setSize(Math.max(f.getWidth(), 860), f.getHeight());
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    /** 创建可关闭 Tag：× 点击即触发关闭动画并从父容器移除（组件内部 CloseButton，无坐标判断）。 */
    private static Tag makeClosableTag(String text, int type, final JPanel parent) {
        Tag t = new Tag(text, type, true);
        t.setOnClosed(() -> SwingUtilities.invokeLater(() -> {
            parent.remove(t);
            parent.revalidate();
            parent.repaint();
        }));
        return t;
    }
}
