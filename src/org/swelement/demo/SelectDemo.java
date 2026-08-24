package org.swelement.demo;

import org.swelement.ui.AstButton;
import org.swelement.ui.AstSelect;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class SelectDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstSelect Demo - 下拉弹层淡入、选项选中高亮、禁用状态");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 基础选择：城市
            JPanel p1 = new JPanel(new GridBagLayout());
            p1.setBorder(new TitledBorder("基础用法（点击选择框观察下拉弹层淡入 + 旋转箭头 + 选项 hover 背景高亮）"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 12, 8, 12);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0; gbc.gridy = 0;
            p1.add(new JLabel("🏙  所在城市"), gbc);
            AstSelect city = new AstSelect(new String[]{"北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆", "苏州", "天津"});
            city.setPreferredSize(new Dimension(280, 40));
            city.setSelectedIndex(4); // 默认杭州
            gbc.gridx = 1; p1.add(city, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            p1.add(new JLabel("🎨 UI 风格"), gbc);
            AstSelect style = new AstSelect(new String[]{"Element UI", "Ant Design", "Material Design", "Bootstrap", "Flat UI"});
            style.setPreferredSize(new Dimension(280, 40));
            gbc.gridx = 1; p1.add(style, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            p1.add(new JLabel("🚫 禁用选择"), gbc);
            AstSelect dis = new AstSelect(new String[]{"选项 A", "选项 B", "选项 C"});
            dis.setPreferredSize(new Dimension(280, 40));
            dis.setEnabled(false);
            gbc.gridx = 1; p1.add(dis, gbc);

            // 长列表
            JPanel p2 = new JPanel(new GridBagLayout());
            p2.setBorder(new TitledBorder("长列表（会自动出现滚动条；可观察滚动中选中项的位置）"));
            gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 12, 8, 12);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0; gbc.gridy = 0;
            p2.add(new JLabel("👤 员工编号"), gbc);
            String[] many = new String[50];
            for (int i = 0; i < 50; i++) many[i] = "员工-" + String.format("%04d", i + 1) + " (" + (i % 5 == 0 ? "在职" : i % 7 == 0 ? "休假" : "出差") + ")";
            AstSelect emp = new AstSelect(many);
            emp.setPreferredSize(new Dimension(340, 40));
            emp.setSelectedIndex(12);
            gbc.gridx = 1; p2.add(emp, gbc);

            // 选中结果
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
            p3.setBorder(new TitledBorder("结果"));
            JLabel echo = new JLabel("（点击「查看选择」汇总所有选择结果）");
            echo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            echo.setForeground(new Color(0x606266));
            AstButton check = new AstButton("查看当前选择", AstButton.PRIMARY, false);
            check.addActionListener(e -> {
                Object cv = city.getSelectedValue();
                Object sv = style.getSelectedValue();
                Object ev = emp.getSelectedValue();
                String c = cv == null ? "(未选)" : cv.toString();
                String s = sv == null ? "(未选)" : sv.toString();
                String em = ev == null ? "(未选)" : ev.toString();
                echo.setText("<html>🏙 城市 = " + c + " ｜ 🎨 风格 = " + s + " ｜ 👤 员工 = " + em + "</html>");
            });
            AstButton reset = new AstButton("全部重置", AstButton.DEFAULT, true);
            reset.addActionListener(e -> {
                city.clearSelection();
                style.clearSelection();
                emp.clearSelection();
                echo.setText("（已重置所有下拉）");
            });
            p3.add(check);
            p3.add(reset);
            p3.add(echo);

            // 弹窗按钮：点击按钮弹出一个居中的 AstSelect 独立弹层
            JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
            p4.setBorder(new TitledBorder("以按钮触发：点按钮选中某城市"));
            String[] cs = {"北京", "上海", "广州", "深圳", "杭州", "成都", "武汉"};
            for (String c : cs) {
                AstButton b = new AstButton(c, AstButton.DEFAULT, true);
                b.addActionListener(e -> city.setSelectedValue(c));
                p4.add(b);
            }

            // 可清空
            JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
            p5.setBorder(new TitledBorder("可清空（单选有值时悬停，箭头变 ×，点击清除）"));
            AstSelect clearable = new AstSelect(new String[]{"北京", "上海", "广州", "深圳"});
            clearable.setPreferredSize(new Dimension(280, 40));
            clearable.setSelectedIndex(1);
            JLabel clearEcho = new JLabel("当前值：上海");
            clearEcho.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            clearEcho.setForeground(new Color(0x606266));
            AstButton showVal = new AstButton("查看当前值", AstButton.DEFAULT, true);
            showVal.addActionListener(e -> {
                Object v = clearable.getSelectedValue();
                clearEcho.setText("当前值：" + (v == null ? "(已清空)" : v.toString()));
            });
            p5.add(clearable); p5.add(showVal); p5.add(clearEcho);

            root.add(p1);
            root.add(Box.createVerticalStrut(8));
            root.add(p2);
            root.add(Box.createVerticalStrut(8));
            root.add(p3);
            root.add(Box.createVerticalStrut(8));
            root.add(p4);
            root.add(Box.createVerticalStrut(8));
            root.add(p5);

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
