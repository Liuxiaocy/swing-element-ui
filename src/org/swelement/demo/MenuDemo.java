package org.swelement.demo;

import org.swelement.ui.Menu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MenuDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Menu Demo - 体验导航菜单与子菜单下拉动画");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel(new BorderLayout());

            // 顶部菜单
            Menu menu = new Menu();
            final JTextArea log = new JTextArea();
            log.setEditable(false);
            log.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            log.setBorder(new EmptyBorder(8, 8, 8, 8));
            log.append("【操作日志】\n");

            // 记录日志的辅助
            java.util.function.Consumer<String> addLog = s -> {
                log.append("● " + s + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            };

            menu.addMenuItem("🏠 首页", () -> {
                addLog.accept("点击菜单：首页");
                menu.setActive(0);
            });
            menu.addMenuItem("📰 新闻", () -> {
                addLog.accept("点击菜单：新闻");
                menu.setActive(1);
            });
            menu.addMenuItem("🛒 商城", () -> {
                addLog.accept("点击菜单：商城");
                menu.setActive(2);
            });
            menu.addSubMenu("📚 文档中心",
                    new String[]{"📖 快速入门", "📘 开发者指南", "📕 API 手册", "───", "💡 常见问题"},
                    new Runnable[]{
                            () -> addLog.accept("子菜单：快速入门"),
                            () -> addLog.accept("子菜单：开发者指南"),
                            () -> addLog.accept("子菜单：API 手册"),
                            null, // 分隔符占位
                            () -> addLog.accept("子菜单：常见问题")
                    });
            menu.addSubMenu("⚙️ 设置",
                    new String[]{"👤 账户信息", "🔐 安全设置", "🎨 主题外观", "🔔 通知偏好", "───", "🚪 退出登录"},
                    new Runnable[]{
                            () -> addLog.accept("子菜单：账户信息"),
                            () -> addLog.accept("子菜单：安全设置"),
                            () -> addLog.accept("子菜单：主题外观"),
                            () -> addLog.accept("子菜单：通知偏好"),
                            null,
                            () -> addLog.accept("子菜单：退出登录")
                    });
            menu.addSubMenu("ℹ️ 关于",
                    new String[]{"项目介绍", "团队成员", "联系方式", "版本更新"},
                    new Runnable[]{
                            () -> addLog.accept("关于 → 项目介绍：Swing Element UI v1.0"),
                            () -> addLog.accept("关于 → 团队成员"),
                            () -> addLog.accept("关于 → 联系方式"),
                            () -> addLog.accept("关于 → 版本更新")
                    });
            menu.setActive(0);

            JScrollPane scroll = new JScrollPane(log,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(BorderFactory.createTitledBorder("菜单操作日志（点击上方菜单项 / 子菜单项）"));
            scroll.setPreferredSize(new Dimension(640, 260));

            JLabel hint = new JLabel("💡 提示：鼠标悬停导航项观察 hover 变色；点击含 ▾ 的项查看子菜单下拉淡入", SwingConstants.CENTER);
            hint.setForeground(new Color(0x909399));
            hint.setBorder(new EmptyBorder(8, 0, 8, 0));

            root.add(menu, BorderLayout.NORTH);
            root.add(hint, BorderLayout.SOUTH);
            root.add(scroll, BorderLayout.CENTER);

            f.setContentPane(root);
            f.setSize(720, 400);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
