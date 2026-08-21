package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 面包屑导航 — Element UI Breadcrumb 的 Java 实现。
 * 路径式导航，分隔符为 "/"，每段可点击（最后一段为当前页不可点）。
 *
 * 用法：
 *   AstBreadcrumb bc = new AstBreadcrumb(Arrays.asList("首页", "用户管理", "详情"));
 *   bc.setSeparator("/");
 *   bc.setItemClickListener(idx -> System.out.println("点击第 " + idx + " 段"));
 *
 * 设计：水平排列，每段文字 TEXT_MAIN，hover 时变 PRIMARY 并带上划线动画；
 * 最后一段 TEXT_PLACEHOLDER（不可点）。分隔符 TEXT_PLACEHOLDER。
 * 文字超宽省略 …。
 */
public class AstBreadcrumb extends JComponent {
    private final List<String> items = new ArrayList<String>();
    private String separator = "/";
    private Consumer<Integer> itemClickListener;
    private int hoverIndex = -1;
    private final Animator hoverAnim;
    private float hoverAlpha;

    private static final int FONT_SIZE = 14;
    private static final int ITEM_PAD = 8;       // 文字左右内边距
    private static final int SEP_PAD = 6;        // 分隔符左右间距
    private static final int ROW_H = 24;

    public AstBreadcrumb() { this(new ArrayList<String>()); }
    public AstBreadcrumb(List<String> items) { setItems0(items);
        hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); }},
            new Animator.Listener() { public void update(float v) { hoverAlpha = v; repaint(); }});
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                hoverIndex = -1; hoverAnim.stop(); hoverAnim.go(hoverAlpha, 0f);
            }
            @Override public void mouseClicked(MouseEvent e) {
                int idx = itemIndexAt(e.getPoint());
                if (idx < 0 || idx >= items.size() - 1) return; // 末段不可点
                if (itemClickListener != null) itemClickListener.accept(idx);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int idx = itemIndexAt(e.getPoint());
                if (idx != hoverIndex) {
                    hoverIndex = idx;
                    // 末段不 hover 高亮
                    if (idx == items.size() - 1) idx = -1;
                    hoverAnim.stop(); hoverAnim.go(hoverAlpha, idx >= 0 ? 1f : 0f);
                }
            }
        });
    }

    private void setItems0(List<String> items) {
        if (items == null) throw new IllegalArgumentException("items must not be null");
        for (String s : items) if (s == null) throw new IllegalArgumentException("item must not be null");
        this.items.clear();
        this.items.addAll(items);
    }

    public void setItems(List<String> items) {
        setItems0(items);
        hoverIndex = -1;
        revalidate(); repaint();
    }

    public void setSeparator(String s) {
        if (s == null) throw new IllegalArgumentException("separator must not be null");
        this.separator = s;
        revalidate(); repaint();
    }

    public void setItemClickListener(Consumer<Integer> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.itemClickListener = l;
    }

    public List<String> getItems() { return new ArrayList<String>(items); }

    @Override public Dimension getPreferredSize() {
        Graphics g = getGraphics();
        FontMetrics fm = (g == null) ? null : g.getFontMetrics(ElementTheme.FONT.deriveFont((float) FONT_SIZE));
        int w = 0;
        for (int i = 0; i < items.size(); i++) {
            int itemW = (fm == null) ? items.get(i).length() * 14 + 2 * ITEM_PAD : fm.stringWidth(items.get(i)) + 2 * ITEM_PAD;
            w += itemW;
            if (i < items.size() - 1) {
                int sepW = (fm == null) ? separator.length() * 8 + 2 * SEP_PAD : fm.stringWidth(separator) + 2 * SEP_PAD;
                w += sepW;
            }
        }
        return new Dimension(Math.max(w, 40), ROW_H);
    }
    @Override public Dimension getMinimumSize() { return new Dimension(40, ROW_H); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(ElementTheme.FONT.deriveFont((float) FONT_SIZE));
        FontMetrics fm = g2.getFontMetrics();
        int x = 0;
        int y = (ROW_H - fm.getHeight()) / 2 + fm.getAscent();
        for (int i = 0; i < items.size(); i++) {
            boolean isLast = (i == items.size() - 1);
            boolean isHovered = (i == hoverIndex && !isLast && hoverAlpha > 0.01f);
            Color textColor;
            if (isLast) {
                textColor = ElementTheme.TEXT_PLACEHOLDER;
            } else if (isHovered) {
                textColor = ElementTheme.lerp(ElementTheme.TEXT_MAIN, ElementTheme.PRIMARY, hoverAlpha);
                ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstBreadcrumb idle item");
            } else {
                textColor = ElementTheme.TEXT_MAIN;
                ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstBreadcrumb idle item");
            }
            String label = items.get(i);
            int maxItemW = 200;
            String clipped = clip(fm, label, maxItemW);
            g2.setColor(textColor);
            g2.drawString(clipped, x + ITEM_PAD, y);
            // hover 下划线
            if (isHovered) {
                int tw = fm.stringWidth(clipped);
                int uy = y + 2;
                Color under = new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), Math.round(255 * hoverAlpha));
                g2.setColor(under);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(x + ITEM_PAD, uy, x + ITEM_PAD + tw, uy);
            }
            x += fm.stringWidth(clipped) + 2 * ITEM_PAD;
            // separator
            if (!isLast) {
                g2.setColor(ElementTheme.TEXT_PLACEHOLDER);
                g2.drawString(separator, x + SEP_PAD, y);
                x += fm.stringWidth(separator) + 2 * SEP_PAD;
            }
        }
        g2.dispose();
    }

    private int itemIndexAt(Point p) {
        Graphics g = getGraphics();
        if (g == null) return -1;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setFont(ElementTheme.FONT.deriveFont((float) FONT_SIZE));
        FontMetrics fm = g2.getFontMetrics();
        int x = 0;
        int idx = -1;
        for (int i = 0; i < items.size(); i++) {
            String clipped = clip(fm, items.get(i), 200);
            int itemW = fm.stringWidth(clipped) + 2 * ITEM_PAD;
            int segEnd = x + itemW;
            if (p.x >= x && p.x < segEnd && p.y >= 0 && p.y < ROW_H) { idx = i; break; }
            x = segEnd;
            if (i < items.size() - 1) x += fm.stringWidth(separator) + 2 * SEP_PAD;
        }
        g2.dispose();
        return idx;
    }

    private static String clip(FontMetrics fm, String text, int maxW) {
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
        try { new AstBreadcrumb(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null items"; threw = false;
        try { new AstBreadcrumb(Arrays.asList("a", null)); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null item"; threw = false;
        try { new AstBreadcrumb().setSeparator(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null separator"; threw = false;
        try { new AstBreadcrumb().setItemClickListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null listener";

        AstBreadcrumb bc = new AstBreadcrumb(Arrays.asList("首页", "用户管理", "详情"));
        assert bc.getItems().size() == 3 : "3 items";
        bc.setItems(Arrays.asList("a", "b"));
        assert bc.getItems().size() == 2 : "2 after setItems";

        // listener + paint + click on EDT
        final int[] clicked = {-99};
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("BC SC");
            jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            try {
                AstBreadcrumb b = new AstBreadcrumb(Arrays.asList("首页", "用户管理", "详情"));
                b.setItemClickListener(idx -> clicked[0] = idx);
                jf.getContentPane().setLayout(new FlowLayout());
                jf.getContentPane().add(b); jf.pack();
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(b.getPreferredSize().width, 24, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                gg.setColor(Color.WHITE); gg.fillRect(0, 0, img.getWidth(), img.getHeight());
                try { b.paint(gg); } finally { gg.dispose(); }
                // Click item 0 (首页)
                b.dispatchEvent(new MouseEvent(b, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, 10, 1, false));
                try { Thread.sleep(30); } catch (Throwable ignore) {}
                assert clicked[0] == 0 : "clicked item 0; actual=" + clicked[0];
                // Click last item (详情) → not clickable
                clicked[0] = -99;
                int lastX = b.getPreferredSize().width - 30;
                b.dispatchEvent(new MouseEvent(b, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, lastX, 10, 1, false));
                try { Thread.sleep(30); } catch (Throwable ignore) {}
                assert clicked[0] == -99 : "last item not clickable; actual=" + clicked[0];
            } finally {
                jf.dispose();
            }
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstBreadcrumb self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
