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
            JFrame f = new JFrame("Input Demo - 档位、密码、图标、文本域、清空、禁用");
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
            name.setColumns(16);
            gbc.gridx = 1; p1.add(name, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            p1.add(new JLabel("邮箱:"), gbc);
            Input email = new Input("请输入邮箱地址，例如 name@example.com");
            email.setColumns(24);
            gbc.gridx = 1; p1.add(email, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            p1.add(new JLabel("搜索:"), gbc);
            Input search = new Input("请输入搜索关键词，支持中英文...");
            search.setColumns(20);
            gbc.gridx = 1; p1.add(search, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            p1.add(new JLabel("禁用:"), gbc);
            Input dis = new Input("disabled 此输入框不可编辑");
            dis.setColumns(16);
            dis.setEnabled(false);
            gbc.gridx = 1; p1.add(dis, gbc);

            // 实时回显 + 交互按钮
            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
            p2.setBorder(new TitledBorder("值读取交互"));
            Input target = new Input("在左侧输入框内编辑任意内容");
            target.setColumns(18);
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

            // 尺寸档位
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            p3.setBorder(new TitledBorder("尺寸档位（large 40 / default 32 / small 28）"));
            Input iL = new Input("大型输入框"); iL.setSize(Input.SIZE_LARGE); iL.setColumns(12);
            Input iD = new Input("默认输入框"); iD.setColumns(12);
            Input iS = new Input("小型输入框"); iS.setSize(Input.SIZE_SMALL); iS.setColumns(12);
            p3.add(iL); p3.add(iD); p3.add(iS);

            // 密码 + 图标
            JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            p4.setBorder(new TitledBorder("密码框（眼睛切换明文）与前后缀图标"));
            Input pw = new Input("请输入密码", Input.PASSWORD); pw.setColumns(14);
            Input pfx = new Input("搜索关键词"); pfx.setPrefixIcon(org.swelement.ui.AstIcon.SEARCH); pfx.setColumns(14);
            Input sfx = new Input("带后缀图标"); sfx.setSuffixIcon(org.swelement.ui.AstIcon.SETTING); sfx.setColumns(14);
            p4.add(pw); p4.add(pfx); p4.add(sfx);

            // 文本域
            JPanel p5 = new JPanel(new BorderLayout(8, 8));
            p5.setBorder(new TitledBorder("文本域 TextArea（自动换行，纵向滚动按需出现）"));
            org.swelement.ui.TextArea ta = new org.swelement.ui.TextArea("请输入多行备注内容…", 4, 32);
            p5.add(ta, BorderLayout.CENTER);

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
