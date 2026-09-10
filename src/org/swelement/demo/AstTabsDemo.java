package org.swelement.demo;

import org.swelement.ui.AstTabs;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * AstTabs 综合 Demo — 水平顶栏标签 + 竖向侧栏标签。
 *
 * 交互提示：
 *  - 点击标签切换内容面板（CardLayout）。
 *  - 竖向模式下可用 ↑ / ↓ 方向键在标签间移动选中项（水平模式为 ← / →）。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstTabsDemo --selfcheck}
 * 会离屏构建水平 + 竖向两个标签页并绘制，验证布局（竖向侧栏宽 200+内容宽、3 行高）
 * 与渲染不抛异常。
 */
public class AstTabsDemo {

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstTabs Demo — 水平顶栏 + 竖向侧栏");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 水平顶栏标签（no-arg 构造 + addTab：避免构造器的空面板与真实内容面板重复）
        AstTabs h = new AstTabs();
        h.addTab("用户管理", panel("用户管理：增删改查与批量导入导出。"));
        h.addTab("角色权限", panel("角色权限：RBAC 模型与数据范围。"));
        h.addTab("操作日志", panel("操作日志：审计与追踪。"));

        // 竖向侧栏标签
        AstTabs v = new AstTabs();
        v.setMode(AstTabs.MODE_VERTICAL);
        v.setSidebarWidth(200);
        v.addTab("概览", panel("概览：核心指标卡片。"));
        v.addTab("订单", panel("订单：状态流转与退款。"));
        v.addTab("商品", panel("商品：SPU/SKU 与库存。"));

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.add(v, BorderLayout.NORTH);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xDCdfe6)));

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        root.add(h, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.add(sidebar, BorderLayout.WEST);
        JLabel hint = new JLabel("<html><body style='width:360px'>主内容区：点击标签切换内容；"
            + "竖向标签可用 <b>↑ / ↓</b> 方向键切换选中项（水平模式为 ← / →）。</body></html>");
        hint.setFont(hint.getFont().deriveFont(13f));
        hint.setForeground(new Color(0x606266));
        body.add(hint, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        f.setContentPane(root);
        f.setSize(760, 480);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private static JPanel panel(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel("<html><body style='padding:16px'>" + text + "</body></html>");
        l.setForeground(new Color(0x606266));
        p.add(l, BorderLayout.NORTH);
        return p;
    }

    // ===================== 自检 =====================

    /** 离屏构建水平 + 竖向两个标签页并绘制，验证布局与渲染不抛异常。 */
    static void selfCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        JFrame jf = new JFrame("AstTabsDemo SC");
                        jf.setSize(760, 480);
                        jf.setVisible(true);

                        AstTabs h = new AstTabs();
                        h.addTab("A", new JPanel());
                        h.addTab("B", new JPanel());
                        h.addTab("C", new JPanel());

                        AstTabs v = new AstTabs();
                        v.setMode(AstTabs.MODE_VERTICAL);
                        v.setSidebarWidth(200);
                        v.addTab("概览", new JPanel());
                        v.addTab("订单", new JPanel());
                        v.addTab("商品", new JPanel());

                        // 竖向布局断言：侧栏宽 200 + 内容宽 280、3 行高（取 max(120,200)=200）
                        Dimension vp = v.getPreferredSize();
                        assert vp.width == 200 + 280 : "demo vertical width = 200+280, got " + vp.width;
                        assert vp.height == Math.max(3 * 40, 200) : "demo vertical height, got " + vp.height;

                        // 离屏绘制水平 + 竖向，验证不抛异常
                        BufferedImage hi = new BufferedImage(400, 240, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g1 = hi.createGraphics();
                        try { h.paint(g1); } finally { g1.dispose(); }
                        BufferedImage vi = new BufferedImage(480, 200, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = vi.createGraphics();
                        try { v.paint(g2); } finally { g2.dispose(); }

                        jf.dispose();
                    } catch (Throwable t) { err[0] = t; }
                }
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstTabsDemo self-check OK");
    }
}
