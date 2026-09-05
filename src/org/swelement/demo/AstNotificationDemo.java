package org.swelement.demo;

import org.swelement.core.theme.ThemeManager;
import org.swelement.ui.AstButton;
import org.swelement.ui.AstNotification;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class AstNotificationDemo {
    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstNotification Demo - 四角位置 / 四种类型 / 手动关闭");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
            root.setBackground(Color.WHITE);
            root.add(button("成功（右上角，默认）", () ->
                    AstNotification.show(f, AstNotification.NotificationType.SUCCESS, "成功", "数据已保存")));
            root.add(Box.createVerticalStrut(10));
            root.add(button("警告（左下角）", () ->
                    AstNotification.show(f, AstNotification.NotificationType.WARNING, "警告", "表单存在未填写项", 0, AstNotification.Position.BOTTOM_LEFT, null)));
            root.add(Box.createVerticalStrut(10));
            root.add(button("错误（左上角）", () ->
                    AstNotification.show(f, AstNotification.NotificationType.ERROR, "错误", "提交失败，请重试", 0, AstNotification.Position.TOP_LEFT, null)));
            root.add(Box.createVerticalStrut(10));
            root.add(button("信息（右下角，常驻 6s）", () ->
                    AstNotification.show(f, AstNotification.NotificationType.INFO, "提示", "这是一条较长的通知内容，用于演示描述换行", 6000, AstNotification.Position.BOTTOM_RIGHT, null)));
            f.setContentPane(root);
            f.pack();
            f.setSize(440, 360);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    static AstButton button(String label, Runnable action) {
        AstButton b = new AstButton(label, AstButton.PRIMARY, false);
        b.addActionListener(e -> action.run());
        return b;
    }

    static void selfCheck() {
        ThemeManager.ensureDefaultTheme();
        // 验证 demo 面板构建不抛异常（组件自身的 show / 堆叠 / 离屏绘制由 build.bat 的组件自检覆盖）
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(button("成功", () -> { }));
        panel.add(button("警告", () -> { }));
        panel.setSize(panel.getPreferredSize());
        BufferedImage img = new BufferedImage(Math.max(1, panel.getWidth()), Math.max(1, panel.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { panel.paint(g); } finally { g.dispose(); }
        System.out.println("AstNotificationDemo self-check OK");
    }
}
