package org.swelement.demo;

import org.swelement.ui.AstRadio;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * AstRadio 综合 Demo — 普通单选框 + 按钮样式（segmented）+ 禁用状态。
 *
 * 交互提示：
 *  - 同一 Group 内最多一个被选中（互斥）。
 *  - 按钮样式下选中项填满主色、文字白色，相邻并排时形成 Element 风格的分段控件。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstRadioDemo --selfcheck}
 * 离屏构建按钮样式单选并绘制，验证 setButtonStyle API、按钮态尺寸、Group 互斥与绘制不抛错。
 * （选中=主色填充的绘制级断言在 AstRadio.selfCheck 中，因其与组件同包可驱动 fill 动画到终态。）
 */
public class AstRadioDemo {

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstRadio Demo — 普通 / 按钮样式 / 禁用");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. 普通单选框（互斥组）
        AstRadio r1 = new AstRadio("选项 A");
        AstRadio r2 = new AstRadio("选项 B");
        AstRadio r3 = new AstRadio("选项 C");
        r1.setSelected(true);
        AstRadio.Group normal = new AstRadio.Group();
        normal.add(r1); normal.add(r2); normal.add(r3);

        JPanel pNormal = new JPanel();
        pNormal.setLayout(new BoxLayout(pNormal, BoxLayout.Y_AXIS));
        pNormal.setOpaque(false);
        pNormal.add(r1); pNormal.add(Box.createVerticalStrut(4));
        pNormal.add(r2); pNormal.add(Box.createVerticalStrut(4));
        pNormal.add(r3);

        // 2. 按钮样式（并排成 segmented）
        AstRadio b1 = new AstRadio("上海");
        AstRadio b2 = new AstRadio("北京");
        AstRadio b3 = new AstRadio("广州");
        b1.setButtonStyle(true); b2.setButtonStyle(true); b3.setButtonStyle(true);
        b2.setSelected(true);
        AstRadio.Group seg = new AstRadio.Group();
        seg.add(b1); seg.add(b2); seg.add(b3);

        JPanel pSeg = new JPanel();
        pSeg.setLayout(new BoxLayout(pSeg, BoxLayout.X_AXIS));
        pSeg.setOpaque(false);
        pSeg.add(b1); pSeg.add(b2); pSeg.add(b3);

        // 3. 禁用（按钮样式）
        AstRadio dis = new AstRadio("不可用");
        dis.setButtonStyle(true);
        dis.setEnabled(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        root.add(section("普通单选框（圆形 + 文字）", pNormal));
        root.add(Box.createVerticalStrut(18));
        root.add(section("按钮样式（segmented，选中=主色填充）", pSeg));
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

    /** 离屏构建按钮样式单选并绘制，验证 setButtonStyle API、尺寸、Group 互斥与绘制不抛错。 */
    static void selfCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        JFrame jf = new JFrame("AstRadioDemo SC");
                        jf.setSize(560, 320);
                        jf.setVisible(true);

                        // API / 默认态
                        AstRadio br = new AstRadio("选项");
                        assert !br.isButtonStyle() : "demo default not button style";
                        br.setButtonStyle(true);
                        assert br.isButtonStyle() : "demo setButtonStyle true";
                        br.setButtonStyle(false);
                        assert !br.isButtonStyle() : "demo setButtonStyle false";
                        br.setButtonStyle(true);

                        // 按钮态尺寸：height=32，width=文字宽+40
                        FontMetrics fm = br.getFontMetrics(org.swelement.core.ElementTheme.font());
                        int expW = fm.stringWidth("选项") + 40;
                        assert br.getPreferredSize().height == 32
                                : "demo button height, got " + br.getPreferredSize().height;
                        assert br.getPreferredSize().width == expW
                                : "demo button width, got " + br.getPreferredSize().width;

                        // Group 互斥 + 按钮样式组合
                        AstRadio x = new AstRadio("X"); x.setButtonStyle(true);
                        AstRadio y = new AstRadio("Y"); y.setButtonStyle(true);
                        x.setSelected(true);
                        AstRadio.Group g = new AstRadio.Group();
                        g.add(x); g.add(y);
                        assert g.getSelected() == x : "demo group initial selected";
                        y.setSelected(true);
                        assert !x.isSelected() && y.isSelected() : "demo group exclusivity";

                        // 绘制级：未选中态背景应为浅底（非主色），且离屏绘制不抛错
                        br.setSelected(false);
                        int bw = br.getPreferredSize().width, bh = 32;
                        br.setSize(bw, bh);
                        BufferedImage img = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D gg = img.createGraphics();
                        try { br.paint(gg); } finally { gg.dispose(); }
                        int px = img.getRGB(8, bh / 2);
                        int alpha = (px >>> 24) & 0xFF;
                        int primRgb = org.swelement.core.ElementTheme.primary().getRGB() & 0xFFFFFF;
                        assert alpha == 255 : "demo button bg opaque, alpha=" + alpha;
                        assert (px & 0xFFFFFF) != primRgb
                                : "demo unselected button must NOT be primary-filled, got "
                                + Integer.toHexString(px & 0xFFFFFF);

                        // 选中态离屏绘制不抛错（选中=主色填充的绘制级断言见 AstRadio.selfCheck）
                        br.setSelected(true);
                        paintTo(br, bw, bh);

                        jf.dispose();
                    } catch (Throwable t) { err[0] = t; }
                }
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstRadioDemo self-check OK");
    }

    private static void paintTo(AstRadio c, int w, int h) {
        c.setBounds(0, 0, w, h);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { c.paint(g); } finally { g.dispose(); }
    }
}
