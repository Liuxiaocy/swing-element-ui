package org.swelement.demo;

import org.swelement.ui.AstButton;
import org.swelement.ui.AstSlider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class SliderDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstSlider Demo - 拖拽、值变化平滑过渡、hover 放大手柄");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 基础滑块：音量
            JPanel p1 = new JPanel(new GridBagLayout());
            p1.setBorder(new TitledBorder("基础使用（鼠标悬停：手柄放大；点击轨道或拖拽：观察填充过渡动画）"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 12, 8, 12);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0; gbc.gridy = 0;
            p1.add(new JLabel("🔊 音量"), gbc);
            AstSlider vol = new AstSlider(0, 100, 40);
            vol.setPreferredSize(new Dimension(380, 32));
            JLabel volLbl = new JLabel("40 %");
            volLbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            volLbl.setPreferredSize(new Dimension(60, 24));
            vol.addChangeListener(e -> volLbl.setText(vol.getValue() + " %"));
            gbc.gridx = 1; p1.add(vol, gbc);
            gbc.gridx = 2; p1.add(volLbl, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            p1.add(new JLabel("🌞 亮度"), gbc);
            AstSlider bright = new AstSlider(0, 100, 75);
            bright.setPreferredSize(new Dimension(380, 32));
            JLabel brLbl = new JLabel("75 %");
            brLbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            brLbl.setPreferredSize(new Dimension(60, 24));
            bright.addChangeListener(e -> brLbl.setText(bright.getValue() + " %"));
            gbc.gridx = 1; p1.add(bright, gbc);
            gbc.gridx = 2; p1.add(brLbl, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            p1.add(new JLabel("💾 进度"), gbc);
            AstSlider dis = new AstSlider(0, 100, 30);
            dis.setPreferredSize(new Dimension(380, 32));
            dis.setEnabled(false);
            gbc.gridx = 1; p1.add(dis, gbc);
            JLabel disLbl = new JLabel("(禁用)");
            disLbl.setForeground(new Color(0x909399));
            gbc.gridx = 2; p1.add(disLbl, gbc);

            // 大范围值：温度 -20 ~ 40
            JPanel p2 = new JPanel(new GridBagLayout());
            p2.setBorder(new TitledBorder("非 0-100 范围（温度：-20℃ ~ 40℃，点击「随机温度」体验滑块动画定位）"));
            gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 12, 8, 12);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0; gbc.gridy = 0;
            p2.add(new JLabel("🌡  空调温度"), gbc);
            AstSlider temp = new AstSlider(-20, 40, 25);
            temp.setPreferredSize(new Dimension(440, 32));
            JLabel tempLbl = new JLabel("25 ℃");
            tempLbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            tempLbl.setForeground(new Color(0xF56C6C));
            temp.addChangeListener(e -> {
                int v = temp.getValue();
                tempLbl.setText(v + " ℃ " + (v <= 0 ? "❄️" : v >= 30 ? "🔥" : "🌿"));
            });
            gbc.gridx = 1; p2.add(temp, gbc);
            gbc.gridx = 2; p2.add(tempLbl, gbc);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            AstButton randTemp = new AstButton("随机温度", AstButton.WARNING, false);
            randTemp.addActionListener(e -> temp.setValue(-20 + (int) (Math.random() * 61)));
            AstButton cold = new AstButton("制冷 18℃", AstButton.PRIMARY, true);
            cold.addActionListener(e -> temp.setValue(18));
            AstButton warm = new AstButton("制热 28℃", AstButton.DANGER, true);
            warm.addActionListener(e -> temp.setValue(28));
            AstButton volMax = new AstButton("音量 max", AstButton.SUCCESS, true);
            volMax.addActionListener(e -> vol.setValue(100));
            btnRow.add(randTemp);
            btnRow.add(cold);
            btnRow.add(warm);
            btnRow.add(Box.createHorizontalStrut(15));
            btnRow.add(volMax);

            // 自动动画演示：进度条循环 0→100→0
            final int[] dir = {1};
            final int[] cur = {0};
            final AstSlider demoSlider = new AstSlider(0, 100, 0);
            demoSlider.setPreferredSize(new Dimension(420, 32));
            JLabel demoLbl = new JLabel("0 %");
            demoLbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            demoSlider.addChangeListener(e -> demoLbl.setText(demoSlider.getValue() + " %"));
            new Timer(50, e -> {
                cur[0] += dir[0];
                if (cur[0] >= 100) { cur[0] = 100; dir[0] = -1; }
                if (cur[0] <= 0) { cur[0] = 0; dir[0] = 1; }
                demoSlider.setValue(cur[0]);
            }).start();

            JPanel auto = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
            auto.setBorder(new TitledBorder("自动动画演示（0 ⇄ 100 循环，可观察平滑填充动画）"));
            auto.add(demoSlider);
            auto.add(demoLbl);

            root.add(p1);
            root.add(Box.createVerticalStrut(8));
            root.add(p2);
            root.add(Box.createVerticalStrut(4));
            root.add(btnRow);
            root.add(Box.createVerticalStrut(8));
            root.add(auto);

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
