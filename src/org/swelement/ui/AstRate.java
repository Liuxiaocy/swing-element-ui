package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

/**
 * 评分组件 — Element UI Rate 的 Java 实现。
 * 支持星星个数、半星、hover 预览、只读模式、清除。
 *
 * 用法：
 *   AstRate rate = new AstRate(5, true); // 5星，允许半星
 *   rate.setValue(3.5f);
 *   rate.setValueListener(v -> System.out.println("评分: " + v));
 *
 * 设计：5 边星形自绘；已选星用 WARNING 金色填充，未选为 BORDER_BASE；
 * hover 时实时预览（不改变 value，仅视觉），离开恢复。点击设置 value。
 * 半星：左半填充 + 右半边框。动画：hover 缩放 1.0→1.15（150ms easeOut）。
 */
public class AstRate extends JComponent {
    private int max = 5;
    private float value;
    private boolean allowHalf;
    private boolean readOnly;
    private Consumer<Float> valueListener;
    private int starSize = 24;
    private int gap = 4;

    private float hoverValue = -1f; // -1 = no hover
    private final Animator hoverAnim;
    private float hoverScale = 1f;

    public AstRate() { this(5, false); }
    public AstRate(int max, boolean allowHalf) { this(max, allowHalf, 0f); }
    public AstRate(int max, boolean allowHalf, float initialValue) {
        if (max < 1 || max > 20) throw new IllegalArgumentException("max must be in [1,20]");
        if (initialValue < 0 || initialValue > max)
            throw new IllegalArgumentException("initialValue out of range");
        if (allowHalf && initialValue * 2 != Math.floor(initialValue * 2))
            throw new IllegalArgumentException("half value must be multiple of 0.5");
        if (!allowHalf && initialValue != Math.floor(initialValue))
            throw new IllegalArgumentException("non-half value must be integer");
        this.max = max;
        this.allowHalf = allowHalf;
        this.value = initialValue;
        this.hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeOut(t); }},
            new Animator.Listener() { public void update(float v) { hoverScale = v; repaint(); }});
        setOpaque(false);
        setFocusable(true);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                if (readOnly) return;
                float hv = valueAt(e.getPoint());
                if (hv != hoverValue) {
                    hoverValue = hv;
                    hoverAnim.stop(); hoverAnim.go(hoverScale, 1.12f);
                    repaint();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                if (readOnly) return;
                hoverValue = -1f;
                hoverAnim.stop(); hoverAnim.go(hoverScale, 1f);
                repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (readOnly) return;
                float v = valueAt(e.getPoint());
                if (v == value && v > 0) {
                    // 同值再次点击 → 清除
                    setValue(0f);
                } else {
                    setValue(v);
                }
            }
        });
    }

    public float getValue() { return value; }

    public void setValue(float v) {
        if (v < 0 || v > max) throw new IllegalArgumentException("value out of range");
        if (allowHalf && v * 2 != Math.floor(v * 2))
            throw new IllegalArgumentException("half value must be multiple of 0.5");
        if (!allowHalf && v != Math.floor(v))
            throw new IllegalArgumentException("non-half value must be integer");
        this.value = v;
        repaint();
        if (valueListener != null) valueListener.accept(v);
    }

    public void setValueListener(Consumer<Float> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.valueListener = l;
    }

    public void setReadOnly(boolean ro) { this.readOnly = ro; repaint(); }
    public void setStarSize(int s) {
        if (s < 12 || s > 48) throw new IllegalArgumentException("starSize must be in [12,48]");
        this.starSize = s; revalidate(); repaint();
    }

    @Override public Dimension getPreferredSize() {
        int w = max * starSize + (max - 1) * gap + 4;
        return new Dimension(w, starSize + 4);
    }
    @Override public Dimension getMinimumSize() { return getPreferredSize(); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float displayValue = (hoverValue >= 0 && !readOnly) ? hoverValue : value;
        for (int i = 0; i < max; i++) {
            float cx = 2 + i * (starSize + gap) + starSize / 2f;
            float cy = 2 + starSize / 2f;
            float starVal = i + 1; // this star represents full value up to i+1
            float filled;
            if (displayValue >= starVal) filled = 1f;
            else if (displayValue >= starVal - 0.5f) filled = 0.5f;
            else filled = 0f;
            boolean isHovered = (hoverValue >= 0 && !readOnly)
                && hoverValue >= starVal - 0.5f && hoverValue < starVal + 0.5f
                || (hoverValue >= starVal && i == (int) hoverValue - 1);
            float scale = (isHovered && hoverScale > 1f) ? hoverScale : 1f;
            drawStar(g2, cx, cy, starSize / 2f, filled, scale);
        }
        g2.dispose();
    }

    private void drawStar(Graphics2D g2, float cx, float cy, float r, float filled, float scale) {
        Graphics2D g = (Graphics2D) g2.create();
        if (scale != 1f) {
            g.translate(cx, cy);
            g.scale(scale, scale);
            g.translate(-cx, -cy);
        }
        Path2D star = starPath(cx, cy, r);
        Color fillCol = (filled > 0f) ? ElementTheme.WARNING : Color.WHITE;
        Color borderCol = (filled > 0f) ? ElementTheme.WARNING : ElementTheme.BORDER_BASE;
        // 背景：白底圆（避免穿透）
        g.setColor(Color.WHITE);
        g.fill(star);
        if (filled == 0.5f) {
            // 半星：左半填充
            g.setColor(ElementTheme.WARNING);
            g.setClip(new Rectangle2D.Float(cx - r, cy - r, r, r * 2));
            g.fill(star);
            g.setClip(null);
            // 边框全部
            g.setColor(borderCol);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(star);
        } else if (filled == 1f) {
            g.setColor(fillCol);
            g.fill(star);
            // 全填充无边框或细边框同色
            g.setColor(borderCol);
            g.setStroke(new BasicStroke(0.5f));
            g.draw(star);
        } else {
            // 空星：边框
            g.setColor(borderCol);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(star);
        }
        g.dispose();
    }

    private static Path2D starPath(float cx, float cy, float r) {
        Path2D p = new Path2D.Float();
        double outer = r;
        double inner = r * 0.4;
        for (int i = 0; i < 10; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / 5;
            double rr = (i % 2 == 0) ? outer : inner;
            float x = cx + (float) Math.cos(ang) * (float) rr;
            float y = cy + (float) Math.sin(ang) * (float) rr;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    private float valueAt(Point p) {
        int idx = (p.x - 2) / (starSize + gap);
        if (idx < 0 || idx >= max) return 0f;
        int within = (p.x - 2) - idx * (starSize + gap);
        if (allowHalf && within < starSize / 2) return idx + 0.5f;
        return idx + 1f;
    }

    // --- Self-check ---
    static void selfCheck() {
        boolean threw = false;
        try { new AstRate(0, false); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "max 0"; threw = false;
        try { new AstRate(21, false); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "max 21"; threw = false;
        try { new AstRate(5, false, 6f); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "initialValue > max"; threw = false;
        try { new AstRate(5, false, 1.5f); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "non-integer on non-half"; threw = false;
        try { new AstRate(5, true, 1.3f); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "non-0.5 multiple on half"; threw = false;
        AstRate r0 = new AstRate(5, false);
        threw = false;
        try { r0.setValue(6f); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setValue > max"; threw = false;
        try { r0.setValue(1.5f); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setValue half on non-half"; threw = false;
        try { r0.setValueListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null listener"; threw = false;
        try { r0.setStarSize(5); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "starSize too small";

        AstRate r = new AstRate(5, true, 2.5f);
        assert r.getValue() == 2.5f : "value 2.5";
        r.setValue(4f);
        assert r.getValue() == 4f : "value 4";
        // half values
        AstRate rh = new AstRate(5, true);
        rh.setValue(3.5f);
        assert rh.getValue() == 3.5f : "half value 3.5";
        // integer only
        AstRate ri = new AstRate(5, false);
        ri.setValue(3f);
        assert ri.getValue() == 3f : "integer value 3";

        // listener
        final float[] got = {-1f};
        r.setValueListener(v -> got[0] = v);
        r.setValue(1f);
        assert got[0] == 1f : "listener fired";

        // valueAt test
        AstRate r2 = new AstRate(5, true, 0);
        assert r2.valueAt(new Point(2 + 0 * (24 + 4) + 5, 10)) == 0.5f : "first star left half → 0.5";
        assert r2.valueAt(new Point(2 + 0 * (24 + 4) + 18, 10)) == 1f : "first star right half → 1";
        assert r2.valueAt(new Point(2 + 2 * (24 + 4) + 5, 10)) == 2.5f : "third star left → 2.5";
        assert r2.valueAt(new Point(2 + 4 * (24 + 4) + 18, 10)) == 5f : "last star right → 5";

        // Paint + EDT click test
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("Rate SC");
            jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            try {
                AstRate rt = new AstRate(5, true, 0);
                final float[] clicked = {-1f};
                rt.setValueListener(v -> clicked[0] = v);
                jf.getContentPane().setLayout(new FlowLayout());
                jf.getContentPane().add(rt); jf.pack();
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(rt.getPreferredSize().width, 28, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                gg.setColor(Color.WHITE); gg.fillRect(0, 0, img.getWidth(), img.getHeight());
                try { rt.paint(gg); } finally { gg.dispose(); }
                // Click 3rd star right half → value 3
                int x = 2 + 2 * (24 + 4) + 18;
                rt.dispatchEvent(new MouseEvent(rt, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, 10, 0, false));
                rt.dispatchEvent(new MouseEvent(rt, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, 10, 1, false));
                try { Thread.sleep(30); } catch (Throwable ignore) {}
                assert clicked[0] == 3f : "clicked → 3; actual=" + clicked[0];
                // Click same again → clear
                rt.dispatchEvent(new MouseEvent(rt, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, 10, 1, false));
                try { Thread.sleep(30); } catch (Throwable ignore) {}
                assert clicked[0] == 0f : "clicked again → 0; actual=" + clicked[0];
                // Half click
                int xh = 2 + 1 * (24 + 4) + 5;
                rt.dispatchEvent(new MouseEvent(rt, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, xh, 10, 1, false));
                try { Thread.sleep(30); } catch (Throwable ignore) {}
                assert clicked[0] == 1.5f : "half click → 1.5; actual=" + clicked[0];
                // readOnly: click does nothing
                rt.setReadOnly(true);
                rt.setValue(2f);
                rt.dispatchEvent(new MouseEvent(rt, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, 10, 1, false));
                try { Thread.sleep(30); } catch (Throwable ignore) {}
                assert rt.getValue() == 2f : "readOnly preserves value";
            } finally {
                jf.dispose();
            }
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstRate self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
