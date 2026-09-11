package org.swelement.demo;

import org.swelement.core.ElementTheme;
import org.swelement.ui.AstCheckbox;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * AstCheckbox 综合 Demo — 普通多选框 + 按钮样式（segmented）+ 禁用状态。
 *
 * 交互提示：
 *  - 同一组多选框可独立勾选（多选）。
 *  - 按钮样式下选中项填满主色、文字白色，相邻并排时形成 Element 风格的分段控件。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstCheckboxDemo --selfcheck}
 * 离屏构建按钮样式多选框并绘制，验证 setButtonStyle API、按钮态尺寸与绘制不抛错。
 * （选中=主色填充的绘制级断言在 AstCheckbox.selfCheck 中，因其与组件同包可驱动 fill 动画到终态。）
 */
public class AstCheckboxDemo {

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstCheckbox Demo — 普通 / 按钮样式 / 禁用");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. 普通多选框（可独立勾选）
        AstCheckbox c1 = new AstCheckbox("选项 A");
        AstCheckbox c2 = new AstCheckbox("选项 B");
        AstCheckbox c3 = new AstCheckbox("选项 C");
        c1.setSelected(true);

        JPanel pNormal = new JPanel();
        pNormal.setLayout(new BoxLayout(pNormal, BoxLayout.Y_AXIS));
        pNormal.setOpaque(false);
        pNormal.add(c1); pNormal.add(Box.createVerticalStrut(4));
        pNormal.add(c2); pNormal.add(Box.createVerticalStrut(4));
        pNormal.add(c3);

        // 2. 按钮样式（并排成 segmented，可多选）
        AstCheckbox b1 = new AstCheckbox("苹果");
        AstCheckbox b2 = new AstCheckbox("香蕉");
        AstCheckbox b3 = new AstCheckbox("橙子");
        b1.setButtonStyle(true); b2.setButtonStyle(true); b3.setButtonStyle(true);
        b1.setSelected(true); b3.setSelected(true);
        JPanel pSeg = new JPanel();
        pSeg.setLayout(new BoxLayout(pSeg, BoxLayout.X_AXIS));
        pSeg.setOpaque(false);
        pSeg.add(b1); pSeg.add(b2); pSeg.add(b3);

        // 3. 禁用（按钮样式）
        AstCheckbox dis = new AstCheckbox("不可用");
        dis.setButtonStyle(true);
        dis.setEnabled(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        root.add(section("普通多选框（勾选框 + 文字）", pNormal));
        root.add(Box.createVerticalStrut(18));
        root.add(section("按钮样式（segmented，选中=主色填充，可多选）", pSeg));
        root.add(Box.createVerticalStrut(18));
        root.add(section("禁用状态", dis));

        f.setContentPane(new JScrollPane(root));
        f.setSize(560, 320);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private static JPanel section(String title, JComponent c) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13f));
        t.setForeground(new Color(0x303133));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(c);
        return p;
    }

    // ===================== 自检 =====================

    /** 离屏构建按钮样式多选框并绘制，验证 setButtonStyle API、尺寸与绘制不抛错。 */
    static void selfCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        JFrame jf = new JFrame("AstCheckboxDemo SC");
                        jf.setSize(560, 320);
                        jf.setVisible(true);

                        // API / 默认态
                        AstCheckbox bc = new AstCheckbox("选项");
                        assert !bc.isButtonStyle() : "demo default not button style";
                        bc.setButtonStyle(true);
                        assert bc.isButtonStyle() : "demo setButtonStyle true";
                        bc.setButtonStyle(false);
                        assert !bc.isButtonStyle() : "demo setButtonStyle false";
                        bc.setButtonStyle(true);

                        // 按钮态尺寸：height=32，width=文字宽+40
                        FontMetrics fm = bc.getFontMetrics(ElementTheme.font());
                        int expW = fm.stringWidth("选项") + 40;
                        assert bc.getPreferredSize().height == 32
                                : "demo button height, got " + bc.getPreferredSize().height;
                        assert bc.getPreferredSize().width == expW
                                : "demo button width, got " + bc.getPreferredSize().width;

                        // 多选独立：可同时勾选多个
                        AstCheckbox x = new AstCheckbox("X"); x.setButtonStyle(true);
                        AstCheckbox y = new AstCheckbox("Y"); y.setButtonStyle(true);
                        x.setSelected(true); y.setSelected(true);
                        assert x.isSelected() && y.isSelected() : "demo multi-select both on";

                        // 绘制级：未选中态背景应为浅底（非主色），且离屏绘制不抛错
                        bc.setSelected(false);
                        int bw = bc.getPreferredSize().width, bh = 32;
                        bc.setSize(bw, bh);
                        BufferedImage img = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D gg = img.createGraphics();
                        try { bc.paint(gg); } finally { gg.dispose(); }
                        int px = img.getRGB(8, bh / 2);
                        int alpha = (px >>> 24) & 0xFF;
                        int primRgb = ElementTheme.primary().getRGB() & 0xFFFFFF;
                        assert alpha == 255 : "demo button bg opaque, alpha=" + alpha;
                        assert (px & 0xFFFFFF) != primRgb
                                : "demo unselected button must NOT be primary-filled, got "
                                + Integer.toHexString(px & 0xFFFFFF);

                        // 选中态离屏绘制不抛错（选中=主色填充的绘制级断言见 AstCheckbox.selfCheck）
                        bc.setSelected(true);
                        paintTo(bc, bw, bh);

                        jf.dispose();
                    } catch (Throwable t) { err[0] = t; }
                }
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstCheckboxDemo self-check OK");
    }

    private static void paintTo(AstCheckbox c, int w, int h) {
        c.setBounds(0, 0, w, h);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { c.paint(g); } finally { g.dispose(); }
    }
}
