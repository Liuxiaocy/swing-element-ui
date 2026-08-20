package org.swelement.demo;

import org.swelement.ui.Button;
import org.swelement.ui.Checkbox;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CheckboxDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Checkbox Demo - 勾选动画、打勾描边揭示、禁用状态");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 基础展示
            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 8));
            p1.setBorder(new TitledBorder("基础展示（勾选观察方框填充 + 勾号动画揭示）"));
            p1.add(new Checkbox("默认（未勾选）"));
            Checkbox cSel = new Checkbox("已勾选");
            cSel.setSelected(true);
            p1.add(cSel);
            Checkbox cDis = new Checkbox("禁用未勾选");
            cDis.setEnabled(false);
            p1.add(cDis);
            Checkbox cSelDis = new Checkbox("禁用已勾选");
            cSelDis.setEnabled(false);
            cSelDis.setSelected(true);
            p1.add(cSelDis);

            // 兴趣标签多选组
            JPanel p2 = new JPanel(new GridBagLayout());
            p2.setBorder(new TitledBorder("兴趣标签 - 多选组（勾选后点击「查看结果」）"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(4, 12, 4, 24);
            String[] hobbies = {"🏃 运动健身", "📚 阅读", "🎵 音乐", "🎮 游戏", "✈️ 旅行", "🎨 绘画", "🍳 烹饪", "📷 摄影"};
            List<Checkbox> boxes = new ArrayList<>();
            for (int i = 0; i < hobbies.length; i++) {
                Checkbox cb = new Checkbox(hobbies[i]);
                boxes.add(cb);
                gbc.gridx = i % 4;
                gbc.gridy = i / 4;
                p2.add(cb, gbc);
            }

            // 控制区
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            p3.setBorder(new TitledBorder("操作区"));
            final JLabel result = new JLabel("（点击下方按钮观察勾选结果）");
            result.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            result.setForeground(new Color(0x606266));

            Button selAll = new Button("全选", Button.DEFAULT, true);
            selAll.addActionListener(e -> boxes.forEach(cb -> cb.setSelected(true)));
            Button clearAll = new Button("全不选", Button.DEFAULT, true);
            clearAll.addActionListener(e -> boxes.forEach(cb -> cb.setSelected(false)));
            Button invert = new Button("反选", Button.DEFAULT, true);
            invert.addActionListener(e -> boxes.forEach(cb -> cb.setSelected(!cb.isSelected())));
            Button view = new Button("查看结果", Button.PRIMARY, false);
            view.addActionListener(e -> {
                StringBuilder sb = new StringBuilder("已选: ");
                for (Checkbox cb : boxes) if (cb.isSelected()) sb.append(cb.getText()).append("，");
                String s = sb.toString();
                result.setText(s.length() > 5 ? s.substring(0, s.length() - 1) : "（未选择任何项）");
            });

            p3.add(selAll);
            p3.add(clearAll);
            p3.add(invert);
            p3.add(Box.createHorizontalStrut(15));
            p3.add(view);
            p3.add(Box.createHorizontalStrut(10));
            p3.add(result);

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
}
