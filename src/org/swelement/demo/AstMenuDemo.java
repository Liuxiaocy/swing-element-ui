package org.swelement.demo;

import org.swelement.ui.AstMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * AstMenu 综合 Demo — 水平顶栏模式 + 竖向侧栏模式。
 *
 * 交互提示：
 *  - 点击菜单项触发其 action；带子菜单的项点击后弹出子菜单（水平向下、竖向向右）。
 *  - 竖向模式下可用 ↑ / ↓ 方向键在条目间移动激活项（水平模式为 ← / →）。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstMenuDemo --selfcheck}
 * 会离屏构建水平 + 竖向两个菜单并绘制，验证布局（竖向侧栏宽 200、3 行高 120）与渲染不抛异常。
 */
public class AstMenuDemo {

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstMenu Demo — 水平顶栏 + 竖向侧栏");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 水平顶栏
        AstMenu h = new AstMenu();
        h.addMenuItem("首页", () -> System.out.println("点击：首页"));
        h.addMenuItem("产品", () -> System.out.println("点击：产品"));
        h.addSubMenu("文档",
            new String[]{"快速入门", "开发者指南", "API 手册"},
            new Runnable[]{
                () -> System.out.println("点击：快速入门"),
                () -> System.out.println("点击：开发者指南"),
                () -> System.out.println("点击：API 手册")});
        h.addMenuItem("关于", () -> System.out.println("点击：关于"));
        h.setActive(0);

        // 竖向侧栏
        AstMenu v = new AstMenu();
        v.setMode(AstMenu.MODE_VERTICAL);
        v.setSidebarWidth(200);
        v.addMenuItem("控制台", () -> System.out.println("点击：控制台"));
        v.addMenuItem("用户管理", () -> System.out.println("点击：用户管理"));
        v.addSubMenu("系统设置",
            new String[]{"权限配置", "操作日志", "数据备份"},
            new Runnable[]{
                () -> System.out.println("点击：权限配置"),
                () -> System.out.println("点击：操作日志"),
                () -> System.out.println("点击：数据备份")});
        v.addMenuItem("退出", () -> System.out.println("点击：退出"));
        v.setActive(0);

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.add(v, BorderLayout.NORTH);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xDCdfe6)));

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        root.add(h, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.add(sidebar, BorderLayout.WEST);
        JLabel hint = new JLabel("<html><body style='width:360px'>主内容区：点击菜单项 / 子菜单查看交互；"
            + "竖向菜单可用 <b>↑ / ↓</b> 方向键切换激活项（水平模式为 ← / →）。</body></html>");
        hint.setFont(hint.getFont().deriveFont(13f));
        hint.setForeground(new Color(0x606266));
        body.add(hint, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        f.setContentPane(root);
        f.setSize(760, 480);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    // ===================== 自检 =====================

    /** 离屏构建水平 + 竖向两个菜单并绘制，验证布局与渲染不抛异常。 */
    static void selfCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        JFrame jf = new JFrame("AstMenuDemo SC");
                        jf.setSize(760, 480);
                        jf.setVisible(true);

                        AstMenu h = new AstMenu();
                        h.addMenuItem("首页", null);
                        h.addMenuItem("产品", null);
                        h.addSubMenu("文档", new String[]{"入门", "指南"}, new Runnable[]{null, null});
                        h.setActive(0);

                        AstMenu v = new AstMenu();
                        v.setMode(AstMenu.MODE_VERTICAL);
                        v.setSidebarWidth(200);
                        v.addMenuItem("控制台", null);
                        v.addMenuItem("用户管理", null);
                        v.addMenuItem("退出", null);

                        // 竖向布局断言：侧栏宽 200、3 行高 120
                        Dimension vp = v.getPreferredSize();
                        assert vp.width == 200 : "demo vertical sidebar width 200, got " + vp.width;
                        assert vp.height == 3 * 40 : "demo vertical height 3*40, got " + vp.height;

                        // 离屏绘制水平 + 竖向，验证不抛异常
                        BufferedImage hi = new BufferedImage(400, 40, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g1 = hi.createGraphics();
                        try { h.paint(g1); } finally { g1.dispose(); }
                        BufferedImage vi = new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = vi.createGraphics();
                        try { v.paint(g2); } finally { g2.dispose(); }

                        jf.dispose();
                    } catch (Throwable t) { err[0] = t; }
                }
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstMenuDemo self-check OK");
    }
}
