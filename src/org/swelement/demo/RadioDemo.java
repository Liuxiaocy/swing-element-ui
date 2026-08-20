package org.swelement.demo;

import org.swelement.ui.Button;
import org.swelement.ui.Radio;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class RadioDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Radio Demo - 单选圆点缩放动画、禁用状态");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 基础单选：支付方式
            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 8));
            p1.setBorder(new TitledBorder("支付方式（单选 - 观察内点缩放出现动画）"));
            ButtonGroup pay = new ButtonGroup();
            Radio a = new Radio("💳 信用卡");
            Radio b = new Radio("🧧 支付宝");
            Radio c = new Radio("💚 微信支付");
            Radio d = new Radio("🚫 禁用选项");
            d.setEnabled(false);
            pay.add(a); pay.add(b); pay.add(c); pay.add(d);
            b.setSelected(true);
            p1.add(a); p1.add(b); p1.add(c); p1.add(d);

            // 第二组：配送方式 + 选项对应价格
            JPanel p2 = new JPanel(new GridBagLayout());
            p2.setBorder(new TitledBorder("配送方式（切换选择查看下方价格更新）"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(4, 14, 4, 24);
            ButtonGroup ship = new ButtonGroup();
            String[][] opts = {
                    {"🚀 当天达（3小时内）", "¥ 20.00"},
                    {"📦 次日达（次日上午）", "¥ 10.00"},
                    {"📮 普通快递（2-3天）", "¥ 5.00"},
                    {"🎁 到店自提", "免费"}
            };
            Radio[] radios = new Radio[opts.length];
            for (int i = 0; i < opts.length; i++) {
                radios[i] = new Radio(opts[i][0]);
                ship.add(radios[i]);
                gbc.gridx = 0; gbc.gridy = i;
                p2.add(radios[i], gbc);
                JLabel price = new JLabel(opts[i][1]);
                price.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
                price.setForeground(new Color(0xF56C6C));
                gbc.gridx = 1;
                p2.add(price, gbc);
            }
            radios[1].setSelected(true);

            // 提交结果
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
            p3.setBorder(new TitledBorder("结果"));
            JLabel payRes = new JLabel("（点击「提交选择」查看结果）");
            payRes.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            payRes.setForeground(new Color(0x606266));
            Button submit = new Button("提交选择", Button.PRIMARY, false);
            submit.addActionListener(e -> {
                String payText = a.isSelected() ? a.getText() : b.isSelected() ? b.getText() : c.isSelected() ? c.getText() : "未选";
                String shipText = "未选";
                for (int i = 0; i < radios.length; i++) if (radios[i].isSelected()) shipText = opts[i][0];
                payRes.setText("<html>✅ 支付：" + payText + " ｜ 🚚 配送：" + shipText + "</html>");
            });
            Button reset = new Button("重置", Button.DEFAULT, true);
            reset.addActionListener(e -> {
                pay.clearSelection();
                ship.clearSelection();
                payRes.setText("（已重置，请重新选择）");
            });
            p3.add(submit);
            p3.add(reset);
            p3.add(payRes);

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
