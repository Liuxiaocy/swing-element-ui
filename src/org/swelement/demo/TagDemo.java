package org.swelement.demo;

import org.swelement.ui.Button;
import org.swelement.ui.Tag;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class TagDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Tag Demo - 标签类型、颜色、可关闭动画（宽度收缩）");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 5 种颜色展示
            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            p1.setBorder(new TitledBorder("5 种标签色（不可关闭）"));
            p1.add(new Tag("Primary 主要", Tag.PRIMARY, false));
            p1.add(new Tag("Success 成功", Tag.SUCCESS, false));
            p1.add(new Tag("Warning 警告", Tag.WARNING, false));
            p1.add(new Tag("Danger 危险", Tag.DANGER, false));
            p1.add(new Tag("Info 信息", Tag.INFO, false));

            // 可关闭标签区（观察关闭动画）
            JPanel p2Wrap = new JPanel(new BorderLayout());
            p2Wrap.setBorder(new TitledBorder("可关闭标签（点击标签右侧 ×，观察宽度收缩至 0 的动画；关闭后会从面板移除）"));
            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            p2.setOpaque(true);
            p2.setBackground(Color.WHITE);
            // 初始一些 tag
            List<String> initTags = Arrays.asList(
                    "🚀 Java", "⚛ React", "🎨 设计", "📊 数据可视化",
                    "🔧 DevOps", "🧪 测试", "☁️ 云计算", "🤖 AI/ML", "📱 移动端", "🌐 网络"
            );
            int[] idx = {0};
            for (String s : initTags) {
                int type = idx[0]++ % 5;
                Tag t = new Tag(s, type, true);
                // 关闭后从父容器移除
                t.putClientProperty("parent", p2);
                p2.add(t);
            }
            p2Wrap.add(p2, BorderLayout.CENTER);
            // 给每个 Tag 挂关闭回调（注意需要用 MouseListener 监听 × 区域）
            // 由于 Tag.close(onClosed) 暴露为 API，这里通过 AWT 事件代理监听点击
            for (Component c : p2.getComponents()) {
                if (c instanceof Tag) {
                    final Tag tag = (Tag) c;
                    final JPanel parent = p2;
                    tag.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            if (!tag.isEnabled()) return;
                            // 判断是否点在 × 区域
                            FontMetrics fm = tag.getFontMetrics(org.swelement.core.ElementTheme.FONT.deriveFont(12f));
                            int xw = 16 + fm.stringWidth(tag.getText());
                            if (e.getX() > xw && e.getX() < tag.getWidth()) {
                                tag.close(() -> SwingUtilities.invokeLater(() -> {
                                    parent.remove(tag);
                                    parent.revalidate();
                                    parent.repaint();
                                }));
                            }
                        }
                    });
                }
            }

            // 动态添加区
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
                int type = addIdx[0]++ % 5;
                Tag t = new Tag(txt, type, true);
                final JPanel parent = p2;
                t.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (!t.isEnabled()) return;
                        FontMetrics fm = t.getFontMetrics(org.swelement.core.ElementTheme.FONT.deriveFont(12f));
                        int xw = 16 + fm.stringWidth(t.getText());
                        if (e.getX() > xw && e.getX() < t.getWidth()) {
                            t.close(() -> SwingUtilities.invokeLater(() -> {
                                parent.remove(t);
                                parent.revalidate();
                                parent.repaint();
                            }));
                        }
                    }
                });
                p2.add(t);
                p2.revalidate();
            });
            Button clear = new Button("清空全部（带动画）", Button.WARNING, true);
            clear.addActionListener(ev -> {
                Component[] cs = p2.getComponents();
                int delay = 0;
                for (Component c : cs) {
                    if (c instanceof Tag) {
                        final Tag t = (Tag) c;
                        final int d = delay;
                        Timer timer = new Timer(delay, e -> {
                            t.close(() -> SwingUtilities.invokeLater(() -> {
                                p2.remove(t);
                                p2.revalidate();
                                p2.repaint();
                            }));
                        });
                        timer.setRepeats(false);
                        timer.start();
                        delay += 60;
                    }
                }
            });
            p3.add(new JLabel("标签文字:"));
            p3.add(tf);
            p3.add(add);
            p3.add(Box.createHorizontalStrut(20));
            p3.add(clear);

            root.add(p1);
            root.add(Box.createVerticalStrut(8));
            root.add(p2Wrap);
            root.add(Box.createVerticalStrut(8));
            root.add(p3);

            f.setContentPane(root);
            f.pack();
            f.setSize(Math.max(f.getWidth(), 820), f.getHeight());
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
