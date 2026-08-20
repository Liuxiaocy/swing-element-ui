package org.swelement.demo;

import org.swelement.ui.Button;
import org.swelement.ui.Input;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class InputDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Input Demo - 占位符、focus 光晕、清空按钮、禁用状态");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 基本输入组
            JPanel p1 = new JPanel(new GridBagLayout());
            p1.setBorder(new TitledBorder("基础用法（点击输入框观察 focus 光晕 + 边框加粗；鼠标悬停观察清空按钮淡入）"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 10, 8, 10);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0; gbc.gridy = 0;
            p1.add(new JLabel("用户名:"), gbc);
            Input name = new Input("请输入用户名");
            name.setPreferredSize(new Dimension(260, 40));
            gbc.gridx = 1; p1.add(name, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            p1.add(new JLabel("邮箱:"), gbc);
            Input email = new Input("请输入邮箱地址，例如 name@example.com");
            email.setPreferredSize(new Dimension(360, 40));
            gbc.gridx = 1; p1.add(email, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            p1.add(new JLabel("搜索:"), gbc);
            Input search = new Input("请输入搜索关键词，支持中英文...");
            search.setPreferredSize(new Dimension(300, 40));
            gbc.gridx = 1; p1.add(search, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            p1.add(new JLabel("禁用:"), gbc);
            Input dis = new Input("disabled 此输入框不可编辑");
            dis.setPreferredSize(new Dimension(260, 40));
            dis.setEnabled(false);
            gbc.gridx = 1; p1.add(dis, gbc);

            // 实时回显 + 交互按钮
            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
            p2.setBorder(new TitledBorder("值读取交互"));
            Input target = new Input("在左侧输入框内编辑任意内容");
            target.setPreferredSize(new Dimension(280, 40));
            JLabel echo = new JLabel("（值将显示在这里）");
            echo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            echo.setForeground(new Color(0x606266));
            JButton readBtn = new JButton("读取值 →");
            readBtn.addActionListener(e -> {
                String v = target.getText();
                echo.setText(v.isEmpty() ? "（空）" : v);
            });
            JButton clearBtn = new JButton("清空");
            clearBtn.addActionListener(e -> target.setText(""));
            JButton fillBtn = new JButton("填入 \"Hello Swing\"");
            fillBtn.addActionListener(e -> target.setText("Hello Swing"));
            JButton toggleEnable = new JButton("切换启用/禁用");
            toggleEnable.addActionListener(e -> target.setEnabled(!target.isEnabled()));

            p2.add(target);
            p2.add(readBtn);
            p2.add(echo);
            p2.add(clearBtn);
            p2.add(fillBtn);
            p2.add(toggleEnable);

            root.add(p1);
            root.add(Box.createVerticalStrut(8));
            root.add(p2);

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
