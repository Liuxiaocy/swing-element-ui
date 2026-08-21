package org.swelement.ui;

import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * 图标组件 — Element UI Icon 风格的自绘图标库（无图片文件依赖）。
 * 所有图标用 Graphics2D 路径/线条绘制，可指定颜色和尺寸。
 *
 * 用法：
 *   AstIcon check = new AstIcon(AstIcon.CHECK, ElementTheme.SUCCESS, 16);
 *   AstIcon close = new AstIcon(AstIcon.CLOSE, ElementTheme.DANGER, 16);
 *   AstIcon arrow = new AstIcon(AstIcon.ARROW_DOWN, ElementTheme.TEXT_REGULAR, 14);
 *
 * 图标清单：CHECK, CLOSE, ARROW_UP/DOWN/LEFT/RIGHT, PLUS, MINUS, SEARCH,
 * INFO, SUCCESS, WARNING, ERROR, SETTING, USER, EYE, EYE_OFF, REFRESH, EDIT, DELETE。
 * 全部按 16/20px 网格设计，stroke 线宽 2px，端点圆角。
 */
public class AstIcon extends JComponent {
    // --- Icon type constants ---
    public static final int CHECK = 0;
    public static final int CLOSE = 1;
    public static final int ARROW_UP = 2;
    public static final int ARROW_DOWN = 3;
    public static final int ARROW_LEFT = 4;
    public static final int ARROW_RIGHT = 5;
    public static final int PLUS = 6;
    public static final int MINUS = 7;
    public static final int SEARCH = 8;
    public static final int INFO = 9;
    public static final int SUCCESS = 10;
    public static final int WARNING = 11;
    public static final int ERROR = 12;
    public static final int SETTING = 13;
    public static final int USER = 14;
    public static final int EYE = 15;
    public static final int REFRESH = 16;
    public static final int EDIT = 17;
    public static final int DELETE = 18;
    private static final int ICON_COUNT = 19;

    private int type;
    private Color color;
    private int size;

    public AstIcon(int type) { this(type, ElementTheme.TEXT_REGULAR, 16); }

    public AstIcon(int type, Color color, int size) {
        if (type < 0 || type >= ICON_COUNT)
            throw new IllegalArgumentException("invalid icon type: " + type);
        if (color == null) throw new IllegalArgumentException("color must not be null");
        if (size < 8 || size > 64) throw new IllegalArgumentException("size must be in [8,64]");
        this.type = type;
        this.color = color;
        this.size = size;
        setOpaque(false);
    }

    public int getType() { return type; }
    public Color getColor() { return color; }
    public int getSizeValue() { return size; }
    public void setType(int t) {
        if (t < 0 || t >= ICON_COUNT) throw new IllegalArgumentException("invalid icon type: " + t);
        this.type = t; repaint();
    }
    public void setColor(Color c) {
        if (c == null) throw new IllegalArgumentException("color must not be null");
        this.color = c; repaint();
    }
    public void setSizeValue(int s) {
        if (s < 8 || s > 64) throw new IllegalArgumentException("size must be in [8,64]");
        this.size = s; revalidate(); repaint();
    }

    @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
    @Override public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(color);
        float s = (float) size;
        switch (type) {
            case CHECK: drawCheck(g2, s); break;
            case CLOSE: drawClose(g2, s); break;
            case ARROW_UP: drawArrow(g2, s, 0); break;
            case ARROW_DOWN: drawArrow(g2, s, 1); break;
            case ARROW_LEFT: drawArrow(g2, s, 2); break;
            case ARROW_RIGHT: drawArrow(g2, s, 3); break;
            case PLUS: drawPlusMinus(g2, s, true); break;
            case MINUS: drawPlusMinus(g2, s, false); break;
            case SEARCH: drawSearch(g2, s); break;
            case INFO: drawInfoCircle(g2, s, ElementTheme.PRIMARY); break;
            case SUCCESS: drawInfoCircle(g2, s, ElementTheme.SUCCESS); drawCheckWhite(g2, s); break;
            case WARNING: drawTriangle(g2, s, ElementTheme.WARNING); break;
            case ERROR: drawInfoCircle(g2, s, ElementTheme.DANGER); drawXWhite(g2, s); break;
            case SETTING: drawSetting(g2, s); break;
            case USER: drawUser(g2, s); break;
            case EYE: drawEye(g2, s); break;
            case REFRESH: drawRefresh(g2, s); break;
            case EDIT: drawEdit(g2, s); break;
            case DELETE: drawDelete(g2, s); break;
            default: break;
        }
        g2.dispose();
    }

    private static void stroke(Graphics2D g2, float w) {
        g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    private static void drawCheck(Graphics2D g2, float s) {
        stroke(g2, s * 0.125f);
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.18f, s * 0.5f);
        p.lineTo(s * 0.42f, s * 0.74f);
        p.lineTo(s * 0.82f, s * 0.26f);
        g2.draw(p);
    }

    private static void drawClose(Graphics2D g2, float s) {
        stroke(g2, s * 0.125f);
        float m = s * 0.25f, mx = s * 0.75f;
        g2.draw(new Line2D.Float(m, m, mx, mx));
        g2.draw(new Line2D.Float(m, mx, mx, m));
    }

    private static void drawArrow(Graphics2D g2, float s, int dir) {
        stroke(g2, s * 0.1f);
        Path2D p = new Path2D.Float();
        // dir: 0=up,1=down,2=left,3=right
        float cx = s * 0.5f;
        if (dir == 0) { p.moveTo(cx, s * 0.2f); p.lineTo(s * 0.2f, s * 0.55f); p.lineTo(s * 0.8f, s * 0.55f); g2.draw(p);
            g2.draw(new Line2D.Float(cx, s * 0.2f, cx, s * 0.8f)); }
        else if (dir == 1) { p.moveTo(cx, s * 0.8f); p.lineTo(s * 0.2f, s * 0.45f); p.lineTo(s * 0.8f, s * 0.45f); g2.draw(p);
            g2.draw(new Line2D.Float(cx, s * 0.2f, cx, s * 0.8f)); }
        else if (dir == 2) { p.moveTo(s * 0.2f, cx); p.lineTo(s * 0.55f, s * 0.2f); p.lineTo(s * 0.55f, s * 0.8f); g2.draw(p);
            g2.draw(new Line2D.Float(s * 0.2f, cx, s * 0.8f, cx)); }
        else { p.moveTo(s * 0.8f, cx); p.lineTo(s * 0.45f, s * 0.2f); p.lineTo(s * 0.45f, s * 0.8f); g2.draw(p);
            g2.draw(new Line2D.Float(s * 0.2f, cx, s * 0.8f, cx)); }
    }

    private static void drawPlusMinus(Graphics2D g2, float s, boolean plus) {
        stroke(g2, s * 0.1f);
        float m = s * 0.2f, mx = s * 0.8f, cy = s * 0.5f;
        g2.draw(new Line2D.Float(m, cy, mx, cy));
        if (plus) g2.draw(new Line2D.Float(cx(s), m, cx(s), mx));
    }
    private static float cx(float s) { return s * 0.5f; }

    private static void drawSearch(Graphics2D g2, float s) {
        stroke(g2, s * 0.1f);
        float r = s * 0.28f;
        float cx = s * 0.42f, cy = s * 0.42f;
        g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        g2.draw(new Line2D.Float(cx + r * 0.7f, cy + r * 0.7f, s * 0.82f, s * 0.82f));
    }

    private static void drawInfoCircle(Graphics2D g2, float s, Color bg) {
        Color save = g2.getColor();
        g2.setColor(bg);
        g2.fill(new Ellipse2D.Float(s * 0.06f, s * 0.06f, s * 0.88f, s * 0.88f));
        g2.setColor(Color.WHITE);
        // "i" dot + stem
        float w = s * 0.1f;
        g2.fill(new RoundRectangle2D.Float(cx(s) - w / 2, s * 0.24f, w, s * 0.18f, w, w));
        g2.fill(new RoundRectangle2D.Float(cx(s) - w / 2, s * 0.5f, w, s * 0.26f, w, w));
        g2.setColor(save);
    }

    private static void drawCheckWhite(Graphics2D g2, float s) {
        g2.setColor(Color.WHITE);
        stroke(g2, s * 0.12f);
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.3f, s * 0.52f);
        p.lineTo(s * 0.45f, s * 0.66f);
        p.lineTo(s * 0.72f, s * 0.34f);
        g2.draw(p);
    }

    private static void drawXWhite(Graphics2D g2, float s) {
        g2.setColor(Color.WHITE);
        stroke(g2, s * 0.12f);
        g2.draw(new Line2D.Float(s * 0.34f, s * 0.34f, s * 0.66f, s * 0.66f));
        g2.draw(new Line2D.Float(s * 0.34f, s * 0.66f, s * 0.66f, s * 0.34f));
    }

    private static void drawTriangle(Graphics2D g2, float s, Color bg) {
        Color save = g2.getColor();
        g2.setColor(bg);
        Path2D p = new Path2D.Float();
        p.moveTo(cx(s), s * 0.1f);
        p.lineTo(s * 0.92f, s * 0.84f);
        p.lineTo(s * 0.08f, s * 0.84f);
        p.closePath();
        g2.fill(p);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(cx(s) - s * 0.05f, s * 0.34f, s * 0.1f, s * 0.26f, s * 0.1f, s * 0.1f));
        g2.fill(new Ellipse2D.Float(cx(s) - s * 0.05f, s * 0.66f, s * 0.1f, s * 0.1f));
        g2.setColor(save);
    }

    private static void drawSetting(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float cx = cx(s), cy = cx(s);
        float rOut = s * 0.38f, rIn = s * 0.16f;
        int teeth = 8;
        Path2D p = new Path2D.Float();
        for (int i = 0; i < teeth * 2; i++) {
            double ang = (Math.PI * i) / teeth;
            float r = (i % 2 == 0) ? rOut : rOut * 0.78f;
            float x = cx + (float) Math.cos(ang) * r;
            float y = cy + (float) Math.sin(ang) * r;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        g2.draw(p);
        g2.draw(new Ellipse2D.Float(cx - rIn, cy - rIn, rIn * 2, rIn * 2));
    }

    private static void drawUser(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float cx = cx(s);
        // head
        float hr = s * 0.16f;
        g2.draw(new Ellipse2D.Float(cx - hr, s * 0.18f, hr * 2, hr * 2));
        // shoulders
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.18f, s * 0.82f);
        p.curveTo(s * 0.22f, s * 0.5f, s * 0.78f, s * 0.5f, s * 0.82f, s * 0.82f);
        g2.draw(p);
    }

    private static void drawEye(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float cy = cx(s);
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.1f, cy);
        p.curveTo(s * 0.3f, s * 0.2f, s * 0.7f, s * 0.2f, s * 0.9f, cy);
        p.curveTo(s * 0.7f, s * 0.8f, s * 0.3f, s * 0.8f, s * 0.1f, cy);
        g2.draw(p);
        float pr = s * 0.1f;
        g2.draw(new Ellipse2D.Float(cy - pr, cy - pr, pr * 2, pr * 2));
    }

    private static void drawRefresh(Graphics2D g2, float s) {
        stroke(g2, s * 0.1f);
        float cx = cx(s), cy = cx(s);
        double r = s * 0.32f;
        // arc 270°
        Arc2D arc = new Arc2D.Float(cx - (float)r, cy - (float)r, (float)(2*r), (float)(2*r), 30, 270, Arc2D.OPEN);
        g2.draw(arc);
        // arrow head at end
        Path2D ah = new Path2D.Float();
        float ex = cx + (float)(r * Math.cos(Math.toRadians(30 + 270)));
        float ey = cy + (float)(r * Math.sin(Math.toRadians(30 + 270)));
        ah.moveTo(ex, ey);
        ah.lineTo(ex - s * 0.12f, ey - s * 0.04f);
        ah.moveTo(ex, ey);
        ah.lineTo(ex + s * 0.04f, ey + s * 0.12f);
        g2.draw(ah);
    }

    private static void drawEdit(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        // pencil: diagonal
        g2.draw(new Line2D.Float(s * 0.2f, s * 0.8f, s * 0.66f, s * 0.34f));
        g2.draw(new Line2D.Float(s * 0.66f, s * 0.34f, s * 0.78f, s * 0.22f));
        g2.draw(new Line2D.Float(s * 0.2f, s * 0.8f, s * 0.08f, s * 0.92f));
        g2.draw(new Line2D.Float(s * 0.64f, s * 0.36f, s * 0.76f, s * 0.24f));
    }

    private static void drawDelete(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float cx = cx(s);
        // lid
        g2.draw(new Line2D.Float(s * 0.2f, s * 0.24f, s * 0.8f, s * 0.24f));
        // handle
        g2.draw(new Line2D.Float(s * 0.4f, s * 0.24f, s * 0.4f, s * 0.16f));
        g2.draw(new Line2D.Float(s * 0.6f, s * 0.24f, s * 0.6f, s * 0.16f));
        g2.draw(new Line2D.Float(s * 0.4f, s * 0.16f, s * 0.6f, s * 0.16f));
        // body sides
        g2.draw(new Line2D.Float(s * 0.28f, s * 0.24f, s * 0.32f, s * 0.84f));
        g2.draw(new Line2D.Float(s * 0.72f, s * 0.24f, s * 0.68f, s * 0.84f));
        g2.draw(new Line2D.Float(s * 0.32f, s * 0.84f, s * 0.68f, s * 0.84f));
        // inner lines
        g2.draw(new Line2D.Float(s * 0.42f, s * 0.36f, s * 0.42f, s * 0.72f));
        g2.draw(new Line2D.Float(s * 0.58f, s * 0.36f, s * 0.58f, s * 0.72f));
    }

    // --- Self-check ---
    static void selfCheck() {
        boolean threw = false;
        try { new AstIcon(-1); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "bad type"; threw = false;
        try { new AstIcon(ICON_COUNT); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "type out of range"; threw = false;
        try { new AstIcon(CHECK, null, 16); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null color"; threw = false;
        try { new AstIcon(CHECK, Color.BLACK, 4); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "size too small"; threw = false;
        try { new AstIcon(CHECK, Color.BLACK, 99); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "size too large"; threw = false;
        try { new AstIcon(CHECK).setType(99); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setType bad"; threw = false;
        try { new AstIcon(CHECK).setColor(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setColor null"; threw = false;
        try { new AstIcon(CHECK).setSizeValue(4); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setSizeValue too small";

        // Paint every icon type to catch draw exceptions
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            for (int t = 0; t < ICON_COUNT; t++) {
                AstIcon ic = new AstIcon(t, ElementTheme.PRIMARY, 20);
                ic.setBounds(0, 0, 20, 20);
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(20, 20, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                gg.setColor(Color.WHITE); gg.fillRect(0, 0, 20, 20);
                try { ic.paint(gg); } finally { gg.dispose(); }
            }
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstIcon self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
