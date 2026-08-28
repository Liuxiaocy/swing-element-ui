package org.swelement.demo;

import org.swelement.ui.AstAlert;
import org.swelement.ui.AstButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AlertDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstAlert Demo - 点击按钮体验弹出/关闭动画");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // 主容器：上方按钮区 + 下方 AstAlert 展示区
            JPanel root = new JPanel(new BorderLayout(0, 16));
            root.setBorder(new EmptyBorder(20, 20, 20, 20));

            // ========== 按钮区：点击弹出对应类型 AstAlert ==========
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            btnPanel.setBorder(BorderFactory.createTitledBorder("点击按钮弹出 AstAlert（观察淡入动画 / 长文本自动换行截断 / 关闭后移除）"));

            // AstAlert 动态展示区（可垂直滚动）
            final JPanel alertArea = new JPanel();
            alertArea.setLayout(new BoxLayout(alertArea, BoxLayout.Y_AXIS));
            JScrollPane scroll = new JScrollPane(alertArea,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(BorderFactory.createTitledBorder("AstAlert 展示区（观察关闭时收缩+移除动画）"));
            scroll.setPreferredSize(new Dimension(600, 360));

            // 插入 AstAlert 的辅助方法（仅负责挂载与滚动到顶）
            java.util.function.Consumer<AstAlert> addAlert = a -> {
                alertArea.add(Box.createVerticalStrut(8), 0);
                alertArea.add(a, 0);
                alertArea.revalidate();
                SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(new Point(0, 0)));
            };

            // 4 个类型弹出按钮（含描述+可关闭）：close 后 AlertX 自行从展示区移除
            AstButton btnSuccess = new AstButton("弹出 Success（长描述·演示换行）", AstButton.SUCCESS, false);
            btnSuccess.addActionListener(e -> addAlert.accept(new AlertX(AstAlert.SUCCESS, "操作成功",
                    "数据已保存，您可以在「记录」中查看本次操作结果。这是一段会被自动换行并在超过两行时以省略号截断的较长描述文字，用于验证固定高度与文字处理是否符合预期。", true, alertArea)));

            AstButton btnWarning = new AstButton("弹出 Warning", AstButton.WARNING, false);
            btnWarning.addActionListener(e -> addAlert.accept(new AlertX(AstAlert.WARNING, "存储空间不足",
                    "当前磁盘仅剩 1.2GB，建议及时清理以免影响程序运行。", true, alertArea)));

            AstButton btnInfo = new AstButton("弹出 Info", AstButton.INFO, false);
            btnInfo.addActionListener(e -> addAlert.accept(new AlertX(AstAlert.INFO, "温馨提示",
                    "本次更新包含性能优化与 bug 修复，建议重启应用生效。", true, alertArea)));

            AstButton btnError = new AstButton("弹出 Error", AstButton.DANGER, false);
            btnError.addActionListener(e -> addAlert.accept(new AlertX(AstAlert.ERROR, "网络请求失败",
                    "无法连接到服务器 (超时)，请检查网络连接后重试。", true, alertArea)));

            // 精简版（无描述）弹出按钮
            AstButton btnSimple = new AstButton("精简版 Info（无描述、不可关）", AstButton.INFO, true);
            btnSimple.addActionListener(e -> addAlert.accept(new AlertX(AstAlert.INFO, "这是一条精简消息，无描述、不可关闭", null, false, alertArea)));

            // 清空按钮
            AstButton btnClear = new AstButton("清空所有 AstAlert", AstButton.DEFAULT, true);
            btnClear.addActionListener(e -> {
                for (Component c : alertArea.getComponents()) {
                    if (c instanceof AstAlert) ((AstAlert) c).close(() -> {});
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

            // 默认预置 4 个静态 AstAlert 展示外观
            alertArea.add(new AlertX(AstAlert.SUCCESS, "成功提示（静态展示）",
                    "这是一条成功提示信息，带描述且可关闭。点击右上角 × 查看收缩+移除动画。", true, alertArea));
            alertArea.add(Box.createVerticalStrut(8));
            alertArea.add(new AlertX(AstAlert.WARNING, "警告提示（静态展示）",
                    "这是一条警告提示信息，带描述且可关闭。", true, alertArea));
            alertArea.add(Box.createVerticalStrut(8));
            alertArea.add(new AlertX(AstAlert.INFO, "消息提示（精简版）", null, false, alertArea));
            alertArea.add(Box.createVerticalStrut(8));
            alertArea.add(new AlertX(AstAlert.ERROR, "错误提示（静态展示）",
                    "这是一条错误提示信息，带描述且可关闭。", true, alertArea));

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    /** 关闭时自动从展示区移除自身的 AstAlert 子类，用于演示关闭动画后的清理。 */
    static class AlertX extends AstAlert {
        private final JPanel area;
        AlertX(int type, String title, String desc, boolean closable, JPanel area) {
            super(type, title, desc, closable);
            this.area = area;
        }
        @Override public void close(Runnable onClosed) {
            super.close(new Runnable() {
                public void run() {
                    if (area != null) { area.remove(AlertX.this); area.revalidate(); area.repaint(); }
                    if (onClosed != null) onClosed.run();
                }
            });
        }
    }
}
