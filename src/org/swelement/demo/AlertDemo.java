package org.swelement.demo;

import org.swelement.ui.Alert;
import org.swelement.ui.Button;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AlertDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Alert Demo - 点击按钮体验弹出/关闭动画");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // 主容器：上方按钮区 + 下方 Alert 展示区
            JPanel root = new JPanel(new BorderLayout(0, 16));
            root.setBorder(new EmptyBorder(20, 20, 20, 20));

            // ========== 按钮区：点击弹出对应类型 Alert ==========
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            btnPanel.setBorder(BorderFactory.createTitledBorder("点击按钮弹出 Alert（观察淡入动画）"));

            // Alert 动态展示区（可垂直滚动）
            final JPanel alertArea = new JPanel();
            alertArea.setLayout(new BoxLayout(alertArea, BoxLayout.Y_AXIS));
            JScrollPane scroll = new JScrollPane(alertArea,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(BorderFactory.createTitledBorder("Alert 展示区（观察关闭时收缩动画）"));
            scroll.setPreferredSize(new Dimension(600, 360));

            // 插入 Alert 的辅助方法
            java.util.function.Consumer<Alert> addAlert = a -> {
                alertArea.add(Box.createVerticalStrut(8), 0);
                alertArea.add(a, 0);
                alertArea.revalidate();
                SwingUtilities.invokeLater(() -> {
                    JViewport vp = scroll.getViewport();
                    vp.setViewPosition(new Point(0, 0));
                });
            };

            // 4 个类型弹出按钮（含描述+可关闭）
            Button btnSuccess = new Button("弹出 Success", Button.SUCCESS, false);
            btnSuccess.addActionListener(e -> {
                Alert a = new Alert(Alert.SUCCESS, "操作成功",
                        "数据已保存，您可以在「记录」中查看本次操作结果。", true);
                a.addPropertyChangeListener("ancestor", null); // placeholder
                // 监听关闭动画完成后移除
                a.putClientProperty("removeAfterClose", addAlert);
                addAlert.accept(a);
            });

            Button btnWarning = new Button("弹出 Warning", Button.WARNING, false);
            btnWarning.addActionListener(e -> {
                Alert a = new Alert(Alert.WARNING, "存储空间不足",
                        "当前磁盘仅剩 1.2GB，建议及时清理以免影响程序运行。", true);
                addAlert.accept(a);
            });

            Button btnInfo = new Button("弹出 Info", Button.INFO, false);
            btnInfo.addActionListener(e -> {
                Alert a = new Alert(Alert.INFO, "温馨提示",
                        "本次更新包含性能优化与 bug 修复，建议重启应用生效。", true);
                addAlert.accept(a);
            });

            Button btnError = new Button("弹出 Error", Button.DANGER, false);
            btnError.addActionListener(e -> {
                Alert a = new Alert(Alert.ERROR, "网络请求失败",
                        "无法连接到服务器 (超时)，请检查网络连接后重试。", true);
                addAlert.accept(a);
            });

            // 精简版（无描述）弹出按钮
            Button btnSimple = new Button("精简版 Info（无描述、不可关）", Button.INFO, true);
            btnSimple.addActionListener(e -> {
                Alert a = new Alert(Alert.INFO, "这是一条精简消息，无描述、不可关闭", null, false);
                addAlert.accept(a);
            });

            // 清空按钮
            Button btnClear = new Button("清空所有 Alert", Button.DEFAULT, true);
            btnClear.addActionListener(e -> {
                for (Component c : alertArea.getComponents()) {
                    if (c instanceof Alert) {
                        Alert alert = (Alert) c;
                        alert.close(() -> {});
                    }
                }
                Timer t = new Timer(300, ev -> {
                    alertArea.removeAll();
                    alertArea.revalidate();
                    alertArea.repaint();
                });
                t.setRepeats(false);
                t.start();
            });

            btnPanel.add(btnSuccess);
            btnPanel.add(btnWarning);
            btnPanel.add(btnInfo);
            btnPanel.add(btnError);
            btnPanel.add(Box.createHorizontalStrut(20));
            btnPanel.add(btnSimple);
            btnPanel.add(Box.createHorizontalStrut(20));
            btnPanel.add(btnClear);

            root.add(btnPanel, BorderLayout.NORTH);
            root.add(scroll, BorderLayout.CENTER);

            // 默认预置 4 个静态 Alert 展示外观
            alertArea.add(new Alert(Alert.SUCCESS, "成功提示（静态展示）",
                    "这是一条成功提示信息，带描述且可关闭。点击右上角 × 查看收缩动画。", true));
            alertArea.add(Box.createVerticalStrut(8));
            alertArea.add(new Alert(Alert.WARNING, "警告提示（静态展示）",
                    "这是一条警告提示信息，带描述且可关闭。", true));
            alertArea.add(Box.createVerticalStrut(8));
            alertArea.add(new Alert(Alert.INFO, "消息提示（精简版）", null, false));
            alertArea.add(Box.createVerticalStrut(8));
            alertArea.add(new Alert(Alert.ERROR, "错误提示（静态展示）",
                    "这是一条错误提示信息，带描述且可关闭。", true));

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
