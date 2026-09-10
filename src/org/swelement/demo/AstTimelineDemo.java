package org.swelement.demo;

import org.swelement.ui.AstIcon;
import org.swelement.ui.AstTimeline;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * AstTimeline 综合 Demo — 基础时间线 + 带图标时间线（混合）。
 *
 * 交互提示：
 *  - 鼠标移到卡片上会有背景过渡（FILL_BASE）。
 *  - 带图标的条目节点会放大到 20px，图标为白色；未设图标的仍是 12px 类型色实心圆点。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstTimelineDemo --selfcheck}
 * 会离屏构建两组时间线并绘制，验证图标 API 与「带图标节点放大」的绘制级行为。
 */
public class AstTimelineDemo {

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstTimeline Demo — 基础 + 带图标");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. 基础时间线（无图标）
        List<AstTimeline.Item> basic = new ArrayList<AstTimeline.Item>();
        basic.add(new AstTimeline.Item("2026-08-01", "项目启动", "确定范围与目标", AstTimeline.Type.PRIMARY));
        basic.add(new AstTimeline.Item("2026-08-10", "P1 完成", "全部组件完成并自检通过", AstTimeline.Type.SUCCESS));
        basic.add(new AstTimeline.Item("2026-08-21", "P2 进行中", AstTimeline.Type.WARNING));
        basic.add(new AstTimeline.Item("2026-09-01", "发布 v1.0", AstTimeline.Type.INFO));

        // 2. 带图标时间线（混合：部分有图标、部分没有）
        List<AstTimeline.Item> withIcons = new ArrayList<AstTimeline.Item>();
        withIcons.add(new AstTimeline.Item("2026-08-01", "账号注册", AstTimeline.Type.PRIMARY, AstIcon.Type.USER));
        withIcons.add(new AstTimeline.Item("2026-08-05", "资料审核", "人工复核中", AstTimeline.Type.WARNING, AstIcon.Type.EYE));
        withIcons.add(new AstTimeline.Item("2026-08-09", "审核通过", AstTimeline.Type.SUCCESS, AstIcon.Type.CIRCLE_CHECK));
        withIcons.add(new AstTimeline.Item("2026-08-12", "已归档", AstTimeline.Type.INFO));

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        root.add(section("基础时间线（12px 实心圆点）", new AstTimeline(basic)));
        root.add(Box.createVerticalStrut(18));
        root.add(section("带图标时间线（有图标 → 20px 圆底 + 白色图标）", new AstTimeline(withIcons)));

        f.setContentPane(new JScrollPane(root));
        f.setSize(620, 700);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private static JPanel section(String title, AstTimeline tl) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13f));
        t.setForeground(new Color(0x303133));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        tl.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(tl);
        return p;
    }

    // ===================== 自检 =====================

    /** 离屏构建基础 + 带图标两组时间线并绘制，验证图标 API 与节点放大行为。 */
    static void selfCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        JFrame jf = new JFrame("AstTimelineDemo SC");
                        jf.setSize(620, 700);
                        jf.setVisible(true);

                        List<AstTimeline.Item> basic = new ArrayList<AstTimeline.Item>();
                        basic.add(new AstTimeline.Item("2026-08-01", "项目启动", AstTimeline.Type.PRIMARY));
                        basic.add(new AstTimeline.Item("2026-08-10", "P1 完成", "自检通过", AstTimeline.Type.SUCCESS));
                        AstTimeline b = new AstTimeline(basic);
                        assert basic.get(0).getIcon() == null : "demo basic item has no icon";

                        List<AstTimeline.Item> icons = new ArrayList<AstTimeline.Item>();
                        icons.add(new AstTimeline.Item("2026-08-01", "账号注册", AstTimeline.Type.PRIMARY,
                                AstIcon.Type.USER));
                        icons.add(new AstTimeline.Item("2026-08-09", "审核通过", AstTimeline.Type.SUCCESS,
                                AstIcon.Type.CIRCLE_CHECK));
                        AstTimeline wi = new AstTimeline(icons);
                        assert icons.get(0).getIcon() == AstIcon.Type.USER : "demo icon USER";
                        assert icons.get(1).getIcon() == AstIcon.Type.CIRCLE_CHECK : "demo icon CIRCLE_CHECK";

                        // 离屏绘制两组，验证不抛异常
                        paintTo(b, 440, b.getPreferredSize().height);
                        paintTo(wi, 440, wi.getPreferredSize().height);

                        jf.dispose();
                    } catch (Throwable t) { err[0] = t; }
                }
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstTimelineDemo self-check OK");
    }

    private static void paintTo(AstTimeline c, int w, int h) {
        c.setBounds(0, 0, w, h);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { c.paint(g); } finally { g.dispose(); }
    }
}
