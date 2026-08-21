package org.swelement.ui;

import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;

/**
 * 分割线组件 — Element UI Divider 的 Java 实现。
 * 支持水平/垂直方向、带文字（左/中/右对齐）、虚线模式。
 *
 * 用法：
 *   AstDivider h = new AstDivider(AstDivider.HORIZONTAL, "标题", AstDivider.ALIGN_LEFT);
 *   AstDivider v = new AstDivider(AstDivider.VERTICAL);
 *   v.setDashed(true);
 *
 * 设计：水平方向画一条 BORDER_BASE 细线，文字打断线并带左右内边距；
 * 垂直方向占满高度画竖线。文字色 TEXT_REGULAR，线色 BORDER_BASE，
 * 对比度由 assertContrast 校验（仅水平带文字模式）。
 */
public class AstDivider extends JComponent {
    public static final int HORIZONTAL = 0, VERTICAL = 1;
    public static final int ALIGN_LEFT = 0, ALIGN_CENTER = 1, ALIGN_RIGHT = 2;

    private final int direction;
    private String text;
    private int align;
    private boolean dashed;
    private Color lineColor = ElementTheme.BORDER_BASE;
    private Color textColor = ElementTheme.TEXT_REGULAR;

    public AstDivider(int direction) { this(direction, null, ALIGN_CENTER); }

    public AstDivider(int direction, String text) { this(direction, text, ALIGN_CENTER); }

    public AstDivider(int direction, String text, int align) {
        if (direction != HORIZONTAL && direction != VERTICAL)
            throw new IllegalArgumentException("direction must be HORIZONTAL or VERTICAL");
        if (align != ALIGN_LEFT && align != ALIGN_CENTER && align != ALIGN_RIGHT)
            throw new IllegalArgumentException("align must be ALIGN_LEFT/CENTER/RIGHT");
        this.direction = direction;
        this.text = text;
        this.align = align;
        setOpaque(false);
    }

    public void setText(String t) { this.text = t; repaint(); }
    public void setAlign(int a) {
        if (a != ALIGN_LEFT && a != ALIGN_CENTER && a != ALIGN_RIGHT)
            throw new IllegalArgumentException("align must be ALIGN_LEFT/CENTER/RIGHT");
        this.align = a; repaint();
    }
    public void setDashed(boolean d) { this.dashed = d; repaint(); }
    public void setLineColor(Color c) {
        if (c == null) throw new IllegalArgumentException("lineColor must not be null");
        this.lineColor = c; repaint();
    }
    public void setTextColor(Color c) {
        if (c == null) throw new IllegalArgumentException("textColor must not be null");
        this.textColor = c; repaint();
    }

    @Override public Dimension getPreferredSize() {
        if (direction == HORIZONTAL) {
            int h = (text == null || text.isEmpty()) ? 1 : 24;
            return new Dimension(100, h);
        } else {
            int w = 1;
            return new Dimension(w, 100);
        }
    }

    @Override public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(lineColor);
        if (direction == HORIZONTAL) {
            if (text == null || text.isEmpty()) {
                drawLine(g2, 0, h / 2, w, h / 2);
            } else {
                g2.setFont(ElementTheme.FONT.deriveFont(14f));
                FontMetrics fm = g2.getFontMetrics();
                String clipped = clipText(g2, text, w - 32);
                int tw = fm.stringWidth(clipped);
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                // 文字背景填充（白色，避免线条穿过文字）
                g2.setColor(getBackground() == null ? Color.WHITE : getBackground());
                // 若背景未知，用 Component 不透明色；此处用白色（Divider 通常用于白底）
                g2.setColor(Color.WHITE);
                int padX = 20;
                int textX;
                if (align == ALIGN_LEFT) {
                    textX = padX;
                    g2.setColor(Color.WHITE); g2.fillRect(textX - 4, 0, tw + 8, h);
                    g2.setColor(lineColor); drawLine(g2, textX + tw + 4, h / 2, w, h / 2);
                } else if (align == ALIGN_RIGHT) {
                    textX = w - padX - tw;
                    g2.setColor(Color.WHITE); g2.fillRect(textX - 4, 0, tw + 8, h);
                    g2.setColor(lineColor); drawLine(g2, 0, h / 2, textX - 4, h / 2);
                } else {
                    textX = (w - tw) / 2;
                    g2.setColor(Color.WHITE); g2.fillRect(textX - 4, 0, tw + 8, h);
                    g2.setColor(lineColor);
                    drawLine(g2, 0, h / 2, textX - 4, h / 2);
                    drawLine(g2, textX + tw + 4, h / 2, w, h / 2);
                }
                // 文字
                ElementTheme.assertContrast(textColor, Color.WHITE, "AstDivider text");
                g2.setColor(textColor);
                g2.drawString(clipped, textX, ty);
            }
        } else {
            // VERTICAL
            drawLine(g2, w / 2, 0, w / 2, h);
        }
        g2.dispose();
    }

    private void drawLine(Graphics2D g2, int x1, int y1, int x2, int y2) {
        if (dashed) {
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f));
            g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(old);
        } else {
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private static String clipText(Graphics2D g2, String text, int maxW) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxW) return text;
        String ell = "\u2026";
        int ellW = fm.stringWidth(ell);
        if (maxW <= ellW) return ell;
        String t = text;
        while (t.length() > 0 && fm.stringWidth(t) + ellW > maxW) t = t.substring(0, t.length() - 1);
        return t + ell;
    }

    // --- Self-check ---
    static void selfCheck() {
        boolean threw = false;
        try { new AstDivider(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "bad direction"; threw = false;
        try { new AstDivider(HORIZONTAL, "x", 9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "bad align"; threw = false;
        try { new AstDivider(HORIZONTAL).setLineColor(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null lineColor"; threw = false;
        try { new AstDivider(HORIZONTAL).setTextColor(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null textColor"; threw = false;
        try { new AstDivider(HORIZONTAL).setAlign(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "bad setAlign";

        // Paint tests on EDT
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            AstDivider h0 = new AstDivider(HORIZONTAL);
            h0.setBounds(0, 0, 200, 1);
            paintTo(h0, 200, 1);
            AstDivider h1 = new AstDivider(HORIZONTAL, "标题文字", ALIGN_CENTER);
            h1.setBounds(0, 0, 200, 24);
            paintTo(h1, 200, 24);
            AstDivider h2 = new AstDivider(HORIZONTAL, "左对齐", ALIGN_LEFT);
            h2.setBounds(0, 0, 200, 24); paintTo(h2, 200, 24);
            AstDivider h3 = new AstDivider(HORIZONTAL, "右对齐", ALIGN_RIGHT);
            h3.setBounds(0, 0, 200, 24); paintTo(h3, 200, 24);
            AstDivider h4 = new AstDivider(HORIZONTAL, "很长很长的标题文字内容测试省略号功能");
            h4.setBounds(0, 0, 100, 24); paintTo(h4, 100, 24);
            h4.setDashed(true); paintTo(h4, 100, 24);
            AstDivider v0 = new AstDivider(VERTICAL);
            v0.setBounds(0, 0, 1, 200); paintTo(v0, 1, 200);
            v0.setDashed(true); paintTo(v0, 1, 200);
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstDivider self-check OK");
    }

    private static void paintTo(JComponent c, int w, int h) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        gg.setColor(Color.WHITE); gg.fillRect(0, 0, w, h);
        try { c.paint(gg); } finally { gg.dispose(); }
    }

    public static void main(String[] args) { selfCheck(); }
}
