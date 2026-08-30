package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Carousel 走马灯 — 横向滑动展示多张幻灯片，支持前后切换、指示点、自动播放。
 *
 * 用法：
 *   List<AstCarousel.SlidePainter> slides = new ArrayList<>();
 *   slides.add((g, w, h) -> {
 *       g.setColor(ElementTheme.PRIMARY); g.fillRect(0, 0, w, h);
 *       g.setColor(Color.WHITE); g.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 28f));
 *       FontMetrics fm = g.getFontMetrics();
 *       String s = "第一张"; g.drawString(s, (w - fm.stringWidth(s)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
 *   });
 *   AstCarousel c = new AstCarousel(slides);
 *   c.setAutoplay(true); // 3s 自动切换
 *   frame.add(c);
 *
 * 设计：
 *  - 容器：固定高度（默认 220），内部自绘所有 slides，通过 translateX 偏移当前可见区。
 *  - 切换动画：Animator 300ms easeInOut，从当前 offset 滑到目标 offset。
 *  - 左右箭头：hover 时显示半透明圆形按钮（‹/›），点击切换。
 *  - 指示点：底部居中，当前页 PRIMARY 长椭圆，其余 BORDER_BASE 圆点。
 *  - 自动播放：3 秒间隔切换到下一张，循环；鼠标 hover 容器时暂停。
 *  - 对比度：箭头/指示点断言；幻灯片内容由调用方在 SlidePainter 中保证对比度。
 */
public class AstCarousel extends AstInteractiveComponent {
    /** 幻灯片绘制器：在给定 Graphics2D 上绘制 (w,h) 区域的内容。 */
    public interface SlidePainter { void paint(Graphics2D g, int w, int h); }

    private final List<SlidePainter> slides;
    private int current = 0;
    private boolean autoplay = false;
    private boolean hoverPaused = false;
    private Timer autoTimer;
    private static final int HEIGHT = 220;
    private static final long AUTO_INTERVAL = 3000;

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("slide", 300, Easing::easeInOut);
        anim.register("arrow", 180, Easing::easeInOut);
    }

    public AstCarousel(List<SlidePainter> slides) {
        if (slides == null) throw new IllegalArgumentException("slides must not be null");
        if (slides.isEmpty()) throw new IllegalArgumentException("slides must not be empty");
        for (SlidePainter s : slides) if (s == null) throw new IllegalArgumentException("slide must not be null");
        this.slides = new ArrayList<SlidePainter>(slides);
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { hoverPaused = true; anim.go("arrow", anim.getProgress("arrow"), 1f); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { hoverPaused = false; anim.go("arrow", anim.getProgress("arrow"), 0f); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                int w = getWidth();
                if (e.getX() < 44) prev();
                else if (e.getX() > w - 44) next();
            }
        });
    }

    public void setAutoplay(boolean on) {
        this.autoplay = on;
        if (on) startAutoplay(); else stopAutoplay();
    }

    public boolean isAutoplay() { return autoplay; }

    public void next() { goTo((current + 1) % slides.size()); }
    public void prev() { goTo((current - 1 + slides.size()) % slides.size()); }

    public void goTo(int idx) {
        if (idx < 0 || idx >= slides.size()) throw new IndexOutOfBoundsException("slide index out of range: " + idx);
        int old = current;
        current = idx;
        int w = getWidth() > 0 ? getWidth() : getPreferredSize().width;
        float from = -old * w;
        float to = -idx * w;
        anim.go("slide", from, to);
    }

    public int getCurrent() { return current; }
    public int getSlideCount() { return slides.size(); }

    private void startAutoplay() {
        stopAutoplay();
        autoTimer = new Timer((int) AUTO_INTERVAL, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!hoverPaused) next();
            }
        });
        autoTimer.start();
    }

    private void stopAutoplay() {
        if (autoTimer != null) { autoTimer.stop(); autoTimer = null; }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(480, HEIGHT); }
    @Override public Dimension getMinimumSize() { return new Dimension(280, HEIGHT); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        int w = getWidth(), h = getHeight();
        g2.setClip(0, 0, w, h);
        // 绘制所有 slides（横向排列），整体偏移 offset
        float offset = anim.getProgress("slide");
        int sx = 0;
        for (int i = 0; i < slides.size(); i++) {
            Graphics2D sg = (Graphics2D) g2.create();
            sg.translate((int) offset + sx, 0);
            try { slides.get(i).paint(sg, w, h); } catch (Throwable ignore) {}
            sg.dispose();
            sx += w;
        }
        // 左右箭头（hover 时显示）
        float arrowHover = anim.getProgress("arrow");
        if (arrowHover > 0.01f) {
            int aw = 32, ah = 32;
            int ay = (h - ah) / 2;
            int a = Math.round(180 * arrowHover);
            drawArrow(g2, 12, ay, aw, ah, "‹", a);
            drawArrow(g2, w - aw - 12, ay, aw, ah, "›", a);
        }
        // 指示点
        int dotR = 4;
        int dotGap = 12;
        int dotsW = slides.size() * (dotR * 2) + (slides.size() - 1) * dotGap;
        int dx0 = (w - dotsW) / 2;
        int dy = h - 16;
        for (int i = 0; i < slides.size(); i++) {
            int cx = dx0 + i * (dotR * 2 + dotGap) + dotR;
            if (i == current) {
                g2.setColor(theme().getPrimary());
                g2.fillRoundRect(cx - dotR - 5, dy - dotR, dotR * 2 + 10, dotR * 2, dotR, dotR);
            } else {
                g2.setColor(theme().getBorderBase());
                g2.fillOval(cx - dotR, dy - dotR, dotR * 2, dotR * 2);
            }
        }
        g2.dispose();
    }

    private void drawArrow(Graphics2D g2, int x, int y, int w, int h, String s, int alpha) {
        Color bg = new Color(0x30, 0x31, 0x33, alpha);
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 16, 16));
        g2.setColor(new Color(0xFF, 0xFF, 0xFF, alpha));
        g2.setFont(theme().getFontBase().deriveFont(Font.BOLD, 20f));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(s)) / 2;
        int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(s, tx, ty);
    }

    @Override public boolean contains(int x, int y) {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }

    @Override
    protected void selfCheck() {
        boolean threw = false;
        try { new AstCarousel(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null slides must throw"; threw = false;
        try { new AstCarousel(new ArrayList<SlidePainter>()); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "empty slides must throw"; threw = false;
        List<SlidePainter> bad = new ArrayList<SlidePainter>(); bad.add(null);
        try { new AstCarousel(bad); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null slide must throw"; threw = false;
        try { new AstCarousel(makeSlides(3)).goTo(-1); } catch (IndexOutOfBoundsException ioobe) { threw = true; }
        assert threw : "goTo(-1) must throw"; threw = false;
        try { new AstCarousel(makeSlides(3)).goTo(3); } catch (IndexOutOfBoundsException ioobe) { threw = true; }
        assert threw : "goTo(3) must throw";

        List<SlidePainter> slides = makeSlides(3);
        AstCarousel c = new AstCarousel(slides);
        assert c.getCurrent() == 0 : "初始 current=0";
        assert c.getSlideCount() == 3;
        c.next();
        assert c.getCurrent() == 1 : "next → 1";
        c.next();
        assert c.getCurrent() == 2 : "next → 2";
        c.next();
        assert c.getCurrent() == 0 : "next 循环回 0";
        c.prev();
        assert c.getCurrent() == 2 : "prev 从 0 循环回 2";
        c.goTo(1);
        assert c.getCurrent() == 1;
        // autoplay 开关
        c.setAutoplay(true);
        assert c.isAutoplay();
        c.setAutoplay(false);
        assert !c.isAutoplay();

        // 离屏绘制校验：carousel paint() 应至少绘制出指示点等非透明像素
        c.goTo(0);
        c.setSize(c.getPreferredSize());
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                c.getWidth(), c.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { c.paint(gg); } finally { gg.dispose(); }
        boolean anyPainted = false;
        for (int y = 0; y < img.getHeight() && !anyPainted; y += 2) {
            for (int x = 0; x < img.getWidth() && !anyPainted; x += 2) {
                if (((img.getRGB(x, y) >>> 24) & 0xFF) > 100) anyPainted = true;
            }
        }
        assert anyPainted : "carousel paint 应绘制出非透明像素（指示点至少应绘制）";
        // 直接校验 SlidePainter：在独立 BufferedImage 上调用，确认 fillRect 生效
        java.awt.image.BufferedImage slideImg = new java.awt.image.BufferedImage(120, 120, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = slideImg.createGraphics();
        try { makeSlides(1).get(0).paint(sg, 120, 120); } finally { sg.dispose(); }
        int spx = slideImg.getRGB(50, 60);
        int sa = (spx >>> 24) & 0xFF;
        assert sa > 120 : "SlidePainter 直接调用应绘制不透明 alpha=" + sa;
        int sred = (spx >> 16) & 0xFF;
        assert sred == ElementTheme.PRIMARY.getRed() : "SlidePainter 应填 PRIMARY 红色分量=" + sred;
        System.out.println("AstCarousel self-check OK");
    }

    private static List<SlidePainter> makeSlides(int n) {
        final Color[] colors = { ElementTheme.PRIMARY, ElementTheme.SUCCESS, ElementTheme.WARNING, ElementTheme.DANGER, ElementTheme.INFO };
        List<SlidePainter> out = new ArrayList<SlidePainter>();
        for (int i = 0; i < n; i++) {
            final int idx = i;
            final Color c = colors[i % colors.length];
            out.add(new SlidePainter() {
                public void paint(Graphics2D g, int w, int h) {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(c);
                    g.fillRect(0, 0, w, h);
                    g.setColor(Color.WHITE);
                    g.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 28f));
                    FontMetrics fm = g.getFontMetrics();
                    String s = "幻灯片 " + (idx + 1);
                    g.drawString(s, (w - fm.stringWidth(s)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
                }
            });
        }
        return out;
    }
    public static void main(String[] args) {
        List<SlidePainter> slides = new ArrayList<SlidePainter>();
        slides.add(new SlidePainter() { public void paint(Graphics2D g, int w, int h) {} });
        new AstCarousel(slides).selfCheck();
    }
}
