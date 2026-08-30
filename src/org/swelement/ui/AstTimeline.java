package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.framework.AstDisplayComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Timeline 时间线 — 垂直展示带有时间戳的事件序列。
 *
 * 用法：
 *   List<AstTimeline.Item> items = new ArrayList<>();
 *   items.add(new AstTimeline.Item("2026-08-01", "项目启动", AstTimeline.Type.PRIMARY));
 *   items.add(new AstTimeline.Item("2026-08-10", "P1 完成", AstTimeline.Type.SUCCESS));
 *   items.add(new AstTimeline.Item("2026-08-21", "P2 进行中", AstTimeline.Type.WARNING));
 *   AstTimeline tl = new AstTimeline(items);
 *   frame.add(tl);
 *
 * 设计：
 *  - 垂直布局，左侧 20px 处一条灰色竖线（BORDER_BASE，2px 宽）。
 *  - 每个 item：左侧时间戳节点（圆形，类型色填充），右侧内容卡片（白底 BORDER_BASE 边框圆角）。
 *  - 节点直径 12px，类型色：PRIMARY/SUCCESS/WARNING/DANGER/INFO。
 *  - 时间戳文字 TEXT_SECONDARY 13px，标题 TEXT_MAIN 粗体 14px，描述 TEXT_REGULAR 13px。
 *  - 卡片 hover 时背景过渡到 FILL_BASE（Animator 150ms easeInOut）。
 *  - 对比度：标题/描述/时间戳均白底，断言校验。
 */
public class AstTimeline extends AstDisplayComponent {
    public enum Type { PRIMARY, SUCCESS, WARNING, DANGER, INFO }

    public static final class Item {
        public final String timestamp;
        public final String title;
        public final String description;
        public final Type type;
        public Item(String timestamp, String title, Type type) { this(timestamp, title, null, type); }
        public Item(String timestamp, String title, String description, Type type) {
            if (timestamp == null) throw new IllegalArgumentException("timestamp must not be null");
            if (title == null) throw new IllegalArgumentException("title must not be null");
            if (type == null) throw new IllegalArgumentException("type must not be null");
            this.timestamp = timestamp; this.title = title; this.description = description; this.type = type;
        }
    }

    private final List<Item> items;
    private static final int NODE_X = 20;
    private static final int NODE_D = 12;
    private static final int LINE_X = NODE_X; // 竖线 x 居中于节点
    private static final int LINE_W = 2;
    private static final int CARD_X = 48;
    private static final int CARD_PAD = 12;
    private static final int ROW_H = 72;
    private static final int GAP = 16;

    public AstTimeline(List<Item> items) {
        if (items == null) throw new IllegalArgumentException("items must not be null");
        if (items.isEmpty()) throw new IllegalArgumentException("items must not be empty");
        for (Item it : items) if (it == null) throw new IllegalArgumentException("item must not be null");
        this.items = new ArrayList<Item>(items);
        for (int i = 0; i < this.items.size(); i++) {
            anim.register("hover_" + i, 150, Easing::easeInOut);
        }
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                int y = e.getY();
                int hovered = -1;
                for (int i = 0; i < AstTimeline.this.items.size(); i++) {
                    int top = i * (ROW_H + GAP);
                    if (y >= top && y < top + ROW_H) { hovered = i; break; }
                }
                for (int i = 0; i < AstTimeline.this.items.size(); i++) {
                    float target = (i == hovered) ? 1f : 0f;
                    if (Math.abs(anim.getProgress("hover_" + i) - target) > 0.01f) {
                        anim.go("hover_" + i, anim.getProgress("hover_" + i), target);
                    }
                }
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                for (int i = 0; i < items.size(); i++) {
                    anim.go("hover_" + i, anim.getProgress("hover_" + i), 0f);
                }
            }
        });
    }

    private Color typeColor(Type t) {
        switch (t) {
            case PRIMARY: return theme().getPrimary();
            case SUCCESS: return theme().getSuccess();
            case WARNING: return theme().getWarning();
            case DANGER: return theme().getDanger();
            case INFO: default: return theme().getInfo();
        }
    }

    @Override public Dimension getPreferredSize() {
        int n = items.size();
        int h = n * ROW_H + (n - 1) * GAP;
        return new Dimension(440, h);
    }
    @Override public Dimension getMinimumSize() { return new Dimension(320, ROW_H); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        int n = items.size();
        int totalH = n * ROW_H + (n - 1) * GAP;
        // 竖线
        g2.setColor(theme().getBorderBase());
        g2.setStroke(new BasicStroke(LINE_W));
        g2.drawLine(LINE_X, 0, LINE_X, totalH);
        // 每项
        for (int i = 0; i < n; i++) {
            Item it = items.get(i);
            int top = i * (ROW_H + GAP);
            float hover = anim.getProgress("hover_" + i);
            // 节点
            Color tc = typeColor(it.type);
            Ellipse2D node = new Ellipse2D.Float(NODE_X - NODE_D/2f, top + ROW_H/2f - NODE_D/2f, NODE_D, NODE_D);
            g2.setColor(tc);
            g2.fill(node);
            g2.setColor(new Color(0xFF, 0xFF, 0xFF, 220));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(node);
            // 卡片
            int cardY = top;
            int cardH = ROW_H;
            int cardW = getWidth() - CARD_X - 4;
            Color bg = lerp(Color.WHITE, theme().getFillBase(), hover);
            g2.setColor(bg);
            g2.fillRoundRect(CARD_X, cardY, cardW, cardH, theme().getRadiusBase(), theme().getRadiusBase());
            g2.setColor(theme().getBorderBase());
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(CARD_X, cardY, cardW, cardH, theme().getRadiusBase(), theme().getRadiusBase());
            // 文字
            g2.setFont(theme().getFontBase().deriveFont(Font.BOLD, 14f));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(theme().getTextPrimary());
            assertContrast(theme().getTextPrimary(), bg, "AstTimeline title");
            g2.drawString(ellipsize(g2, fm, it.title, cardW - 2 * CARD_PAD), CARD_X + CARD_PAD, cardY + 22);
            // 时间戳
            g2.setFont(theme().getFontBase().deriveFont(12f));
            fm = g2.getFontMetrics();
            g2.setColor(theme().getTextRegular());
            assertContrast(theme().getTextRegular(), bg, "AstTimeline timestamp");
            g2.drawString(ellipsize(g2, fm, it.timestamp, cardW - 2 * CARD_PAD), CARD_X + CARD_PAD, cardY + ROW_H - 12);
            // 描述（如有）
            if (it.description != null) {
                g2.setFont(theme().getFontBase().deriveFont(13f));
                fm = g2.getFontMetrics();
                g2.setColor(theme().getTextRegular());
                String desc = ellipsize(g2, fm, it.description, cardW - 2 * CARD_PAD);
                int descX = CARD_X + CARD_PAD;
                int descY = cardY + 22 + fm.getHeight() + 2;
                if (descY < cardY + ROW_H - 18) {
                    g2.drawString(desc, descX, descY);
                }
            }
        }
        g2.dispose();
    }

    private static String ellipsize(Graphics2D g2, FontMetrics fm, String s, int maxW) {
        if (fm.stringWidth(s) <= maxW) return s;
        String ell = "\u2026";
        while (s.length() > 0 && fm.stringWidth(s) + fm.stringWidth(ell) > maxW) s = s.substring(0, s.length()-1);
        return s + ell;
    }

    @Override
    protected void selfCheck() {
        boolean threw = false;
        try { new AstTimeline(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null items must throw"; threw = false;
        try { new AstTimeline(new ArrayList<Item>()); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "empty items must throw"; threw = false;
        List<Item> bad = new ArrayList<Item>(); bad.add(null);
        try { new AstTimeline(bad); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null item must throw"; threw = false;
        try { new Item(null, "x", Type.PRIMARY); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null timestamp must throw"; threw = false;
        try { new Item("t", null, Type.PRIMARY); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null title must throw"; threw = false;
        try { new Item("t", "x", null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null type must throw";

        List<Item> items = new ArrayList<Item>();
        items.add(new Item("2026-08-01", "项目启动", Type.PRIMARY));
        items.add(new Item("2026-08-10", "P1 完成", "全部组件完成并自检通过", Type.SUCCESS));
        items.add(new Item("2026-08-21", "P2 进行中", Type.WARNING));
        AstTimeline tl = new AstTimeline(items);
        assert tl.getPreferredSize().height == 3 * 72 + 2 * 16 : "preferredHeight";
        assert tl.getPreferredSize().width == 440;

        // 离屏绘制校验对比度断言
        tl.setSize(440, tl.getPreferredSize().height);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                440, tl.getPreferredSize().height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { tl.paint(gg); } finally { gg.dispose(); }
        // 节点区域像素应有颜色（PRIMARY 蓝色）
        int nodePx = img.getRGB(20, 36);
        int a = (nodePx >>> 24) & 0xFF;
        assert a > 120 : "节点绘制不透明 alpha=" + a;
        // 卡片区像素应不透明
        int cardPx = img.getRGB(60, 10);
        int ca = (cardPx >>> 24) & 0xFF;
        assert ca > 120 : "卡片绘制不透明 alpha=" + ca;
        System.out.println("AstTimeline self-check OK");
    }
    public static void main(String[] args) {
        List<Item> items = new ArrayList<Item>();
        items.add(new Item("2026-08-01", "test", Type.PRIMARY));
        new AstTimeline(items).selfCheck();
    }
}
