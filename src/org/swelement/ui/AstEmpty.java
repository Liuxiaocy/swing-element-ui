package org.swelement.ui;

import org.swelement.core.theme.ThemeManager;
import org.swelement.framework.AstDisplayComponent;
import org.swelement.ui.AstButton;
import org.swelement.ui.AstIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 空状态 Empty — 用于数据为空、无权限、出错等场景的占位展示。
 *
 * 用法：
 *   AstEmpty empty = new AstEmpty("暂无数据");
 *   empty.setImage(customIcon);              // 自定义插图（优先级最高）
 *   empty.setIconType(AstIcon.SEARCH);       // 或指定 AstIcon 类型作为插图
 *   empty.setDescription("没有找到相关内容");
 *   empty.setAction(new AstButton("刷新", AstButton.PRIMARY, false)); // 可选操作按钮
 *
 * 设计：
 *  - 竖向布局：插图（自定义 Icon / AstIcon / 内置默认空插图）→ 描述文字 → 可选操作按钮。
 *  - 描述走主题 TEXT_REGULAR（WCAG AA），插图用中性灰，纯装饰不断言对比度。
 *  - 纯展示组件（AstDisplayComponent 子类），默认箭头光标。
 */
public class AstEmpty extends AstDisplayComponent {
    private Icon image;        // 自定义插图（最高优先）
    private int iconType = -1; // AstIcon 序数值（image 为空时绘制；-1 = 内置默认空插图）
    private int iconSize = 64;
    private String description = "暂无数据";
    private JComponent action;

    public AstEmpty() { }
    public AstEmpty(String description) { this.description = description; }

    public void setDescription(String d) {
        if (d == null) d = "";
        this.description = d; revalidate(); repaint();
    }
    public String getDescription() { return description; }

    /** 自定义插图（Icon）。设置后忽略 iconType。 */
    public void setImage(Icon img) { this.image = img; this.iconType = -1; revalidate(); repaint(); }

    /** 用 AstIcon 类型作为插图（覆盖自定义 image）。 */
    public void setIconType(int astIconType) { this.iconType = astIconType; this.image = null; revalidate(); repaint(); }

    public void setIconSize(int s) { this.iconSize = Math.max(24, Math.min(160, s)); revalidate(); repaint(); }

    /** 可选操作区（按钮/链接）。传 null 移除。 */
    public void setAction(JComponent a) {
        if (this.action != null) remove(this.action);
        this.action = a;
        if (a != null) { a.setAlignmentX(0.5f); add(a); }
        revalidate(); repaint();
    }
    public JComponent getAction() { return action; }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    private int imgW() { return image != null ? image.getIconWidth() : iconSize; }
    private int imgH() { return image != null ? image.getIconHeight() : iconSize; }

    @Override public Dimension getPreferredSize() {
        int w = Math.max(imgW(), 200);
        int descH = descLineCount() * 22 + 6;
        int h = 12 + imgH() + 10 + descH + 12;
        if (action != null) {
            Dimension ps = action.getPreferredSize();
            h += ps.height + 12;
            w = Math.max(w, ps.width + 24);
        }
        return new Dimension(w, h);
    }

    private int descLineCount() {
        if (description == null || description.isEmpty()) return 1;
        int avail = Math.max(120, getWidth() > 0 ? getWidth() - 24 : 216);
        int per = Math.max(8, avail / 15);
        return Math.max(1, (description.length() + per - 1) / per);
    }

    @Override public void doLayout() {
        if (action != null) {
            Dimension ps = action.getPreferredSize();
            int ax = (getWidth() - ps.width) / 2;
            int ay = getHeight() - 12 - ps.height;
            action.setBounds(ax, ay, ps.width, ps.height);
        }
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        int cx = getWidth() / 2;
        int top = 12;
        int iw = imgW(), ih = imgH();
        int ix = cx - iw / 2, iy = top;
        if (image != null) {
            image.paintIcon(this, g2, ix, iy);
        } else if (iconType >= 0 && iconType < AstIcon.Type.values().length) {
            AstIcon.paintIcon(g2, AstIcon.Type.values()[iconType], theme().getTextRegular(), Math.min(64, iconSize), 0f);
        } else {
            paintDefaultIllustration(g2, ix, iy, iw, ih);
        }
        // 描述
        g2.setColor(theme().getTextRegular());
        assertContrast(theme().getTextRegular(), Color.WHITE, "AstEmpty description");
        Font f = theme().getFontBase().deriveFont(13f);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics(f);
        int dy = iy + ih + 10;
        drawWrappedCenter(g2, description, cx, dy, getWidth() - 24, fm);
        g2.dispose();
    }

    /** 内置默认空插图：一个轻灰圆角「盒子」+ 一条盖线，纯装饰。 */
    private void paintDefaultIllustration(Graphics2D g2, int x, int y, int w, int h) {
        Color line = new Color(0xC0, 0xC4, 0xCC);
        g2.setColor(line);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int pad = (int) (w * 0.18);
        int bx = x + pad, by = y + pad, bw = w - pad * 2, bh = h - pad * 2;
        g2.drawRoundRect(bx, by, bw, bh, 10, 10);
        // 盖线
        g2.drawLine(bx + bw * 2 / 10, by + bh / 2, bx + bw * 8 / 10, by + bh / 2);
        // 一个小圆点（装饰）
        g2.fillOval(x + w / 2 - 4, by + bh * 3 / 4, 8, 8);
    }

    /** 居中按宽度折行绘制描述文字。 */
    private void drawWrappedCenter(Graphics2D g2, String text, int cx, int top, int maxW, FontMetrics fm) {
        if (text == null || text.isEmpty()) return;
        int per = Math.max(8, maxW / 15);
        java.util.List<String> lines = new java.util.ArrayList<String>();
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(text.length(), i + per);
            lines.add(text.substring(i, end));
            i = end;
        }
        int ly = top + fm.getAscent();
        for (String ln : lines) {
            int lw = fm.stringWidth(ln);
            g2.drawString(ln, cx - lw / 2, ly);
            ly += 22;
        }
    }

    // --- self-check ---
    @Override
    protected void selfCheck() {
        ThemeManager.ensureDefaultTheme();
        boolean threw = false;
        try { AstEmpty e = new AstEmpty(null); } catch (Throwable t) { threw = true; }
        assert !threw : "null description must be tolerated";

        AstEmpty e = new AstEmpty("暂无数据");
        e.setDescription("没有找到相关内容");
        e.setIconType(AstIcon.INFO);
        e.setAction(new AstButton("刷新", AstButton.PRIMARY, false));
        e.setSize(e.getPreferredSize());
        e.doLayout();
        BufferedImage img = new BufferedImage(Math.max(1, e.getWidth()), Math.max(1, e.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { e.paint(gg); } finally { gg.dispose(); }
        int px = img.getRGB(Math.min(e.getWidth() - 1, e.getWidth() / 2), Math.min(e.getHeight() - 1, e.getHeight() - 20));
        int a = (px >>> 24) & 0xFF;
        assert a > 120 : "AstEmpty 绘制不透明 alpha=" + a;

        // 默认插图路径（无 image / 无 iconType）
        AstEmpty e2 = new AstEmpty();
        e2.setSize(e2.getPreferredSize());
        BufferedImage img2 = new BufferedImage(Math.max(1, e2.getWidth()), Math.max(1, e2.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg2 = img2.createGraphics();
        try { e2.paint(gg2); } finally { gg2.dispose(); }
        // 默认插图绘在顶部插图带（y∈[12, 12+iconSize]），扫描该带确认确有像素被画出
        int ih = 64; // 默认 iconSize
        boolean drew = false;
        for (int yy = 12; yy < 12 + ih && !drew; yy++) {
            for (int xx = 0; xx < img2.getWidth(); xx++) {
                if ((img2.getRGB(xx, yy) >>> 24 & 0xFF) > 0) { drew = true; break; }
            }
        }
        assert drew : "AstEmpty 默认插图应绘制出像素";

        System.out.println("AstEmpty self-check OK");
    }
    public static void main(String[] args) { new AstEmpty().selfCheck(); }
}
