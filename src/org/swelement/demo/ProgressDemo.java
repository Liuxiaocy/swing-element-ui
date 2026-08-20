package org.swelement.demo;

import org.swelement.ui.Button;
import org.swelement.ui.Progress;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ProgressDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Progress Demo - 进度条填充动画、首屏加载动画");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 多行进度条展示（Element UI 统一为蓝色主题色）
            JPanel p1 = new JPanel();
            p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
            p1.setBorder(new TitledBorder("不同进度值展示（打开窗口即观察「填充从 0 动画过渡到目标值」）"));
            p1.setOpaque(true);
            p1.setBackground(Color.WHITE);
            int[] vals = {10, 25, 45, 60, 78, 92, 100};
            Progress[] bars = new Progress[vals.length];
            for (int i = 0; i < vals.length; i++) {
                final int fi = i;
                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setBorder(new EmptyBorder(4, 12, 4, 12));
                row.setOpaque(true);
                row.setBackground(Color.WHITE);
                JLabel lbl = new JLabel("任务 " + (i + 1));
                lbl.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
                lbl.setPreferredSize(new Dimension(70, 22));
                Progress p = new Progress();
                p.setPreferredSize(new Dimension(460, 22));
                final JLabel vLbl = new JLabel("0 %");
                vLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                vLbl.setPreferredSize(new Dimension(54, 22));
                vLbl.setHorizontalAlignment(JLabel.RIGHT);
                p.addChangeListener(e -> vLbl.setText(p.getValue() + " %"));
                bars[i] = p;
                row.add(lbl, BorderLayout.WEST);
                row.add(p, BorderLayout.CENTER);
                row.add(vLbl, BorderLayout.EAST);
                p1.add(row);
                // 延迟设置值以启动动画
                Timer set = new Timer(120 + i * 80, e -> p.setValue(vals[fi]));
                set.setRepeats(false);
                set.start();
            }

            // 主控制区：单一主导进度条 + 按钮控制
            Progress main = new Progress(0);
            main.setPreferredSize(new Dimension(600, 24));
            JLabel mLbl = new JLabel("0 %");
            mLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            mLbl.setForeground(new Color(0x409EFF));
            main.addChangeListener(e -> mLbl.setText(main.getValue() + " %"));

            JPanel p2 = new JPanel(new BorderLayout(10, 0));
            p2.setBorder(new TitledBorder("上传进度（点击按钮观察填充动画）"));
            JPanel inner = new JPanel(new BorderLayout(10, 0));
            inner.setOpaque(true);
            inner.setBackground(Color.WHITE);
            inner.setBorder(new EmptyBorder(10, 12, 10, 12));
            JLabel prefix = new JLabel("🚀 上传：");
            prefix.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            inner.add(prefix, BorderLayout.WEST);
            inner.add(main, BorderLayout.CENTER);
            inner.add(mLbl, BorderLayout.EAST);
            p2.add(inner, BorderLayout.CENTER);

            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            p3.setBorder(new TitledBorder("操作按钮"));
            Button to0 = new Button("0%", Button.DEFAULT, true);
            Button to30 = new Button("30%", Button.DEFAULT, true);
            Button to60 = new Button("60%", Button.DEFAULT, true);
            Button to90 = new Button("90%", Button.PRIMARY, true);
            Button to100 = new Button("完成 100%", Button.SUCCESS, false);
            Button animStart = new Button("▶ 自动模拟上传", Button.WARNING, false);
            Button animStop = new Button("⏸ 停止自动", Button.DANGER, true);
            Button reloadAll = new Button("🔄 重置上方面板并播放动画", Button.DEFAULT, true);

            final Timer[] uploadTimer = {null};
            to0.addActionListener(e -> main.setValue(0));
            to30.addActionListener(e -> main.setValue(30));
            to60.addActionListener(e -> main.setValue(60));
            to90.addActionListener(e -> main.setValue(90));
            to100.addActionListener(e -> main.setValue(100));
            animStart.addActionListener(e -> {
                if (uploadTimer[0] != null && uploadTimer[0].isRunning()) return;
                uploadTimer[0] = new Timer(180, ev -> {
                    int v = main.getValue();
                    if (v < 99) main.setValue(v + 1);
                });
                uploadTimer[0].start();
            });
            animStop.addActionListener(e -> {
                if (uploadTimer[0] != null) uploadTimer[0].stop();
            });
            reloadAll.addActionListener(e -> {
                for (int i = 0; i < bars.length; i++) {
                    final int fi = i;
                    bars[i].setValue(0);
                    Timer set = new Timer(200 + fi * 80, ev -> bars[fi].setValue(vals[fi]));
                    set.setRepeats(false);
                    set.start();
                }
            });

            p3.add(to0); p3.add(to30); p3.add(to60); p3.add(to90); p3.add(to100);
            p3.add(Box.createHorizontalStrut(15));
            p3.add(animStart); p3.add(animStop);
            p3.add(Box.createHorizontalStrut(15));
            p3.add(reloadAll);

            // 0→100 单次演示
            Progress once = new Progress(0);
            once.setPreferredSize(new Dimension(600, 20));
            JLabel oLbl = new JLabel("0 %");
            oLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            oLbl.setForeground(new Color(0x67C23A));
            once.addChangeListener(e -> oLbl.setText(once.getValue() + " %"));
            JPanel onceRow = new JPanel(new BorderLayout(10, 0));
            onceRow.setBorder(new EmptyBorder(10, 12, 10, 12));
            onceRow.setOpaque(true);
            onceRow.setBackground(Color.WHITE);
            onceRow.add(new JLabel("演示：重置 0 → 100"), BorderLayout.WEST);
            onceRow.add(once, BorderLayout.CENTER);
            onceRow.add(oLbl, BorderLayout.EAST);
            Button onceBtn = new Button("🔄 重置并播放 0→100 动画", Button.PRIMARY, false);
            onceBtn.addActionListener(e -> {
                once.setValue(0);
                new Timer(25, new java.awt.event.ActionListener() {
                    int v = 0;
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent ev) {
                        v += 1;
                        once.setValue(v);
                        if (v >= 100) ((Timer) ev.getSource()).stop();
                    }
                }).start();
            });

            JPanel p4 = new JPanel();
            p4.setLayout(new BoxLayout(p4, BoxLayout.Y_AXIS));
            p4.setBorder(new TitledBorder("单次动画演示（每 25ms +1%，观察动画流畅度）"));
            p4.setOpaque(true);
            p4.setBackground(Color.WHITE);
            p4.add(onceRow);
            JPanel bWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            bWrap.add(onceBtn);
            p4.add(bWrap);

            root.add(p1);
            root.add(Box.createVerticalStrut(8));
            root.add(p2);
            root.add(Box.createVerticalStrut(4));
            root.add(p3);
            root.add(Box.createVerticalStrut(8));
            root.add(p4);

            f.setContentPane(root);
            f.pack();
            f.setSize(Math.max(f.getWidth(), 880), f.getHeight());
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
