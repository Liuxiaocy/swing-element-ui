package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.framework.AstAbstractComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 折叠面板 — Element UI Collapse 的 Java 实现。
 * 多个面板，点击标题展开/折叠内容；支持手风琴模式（同时仅一个展开）。
 *
 * 用法：
 *   AstCollapse c = new AstCollapse(true); // 手风琴
 *   c.addItem("标题一", createContentA());
 *   c.addItem("标题二", createContentB());
 *   c.setChangeListener((idx, open) -> System.out.println(idx + " " + (open?"open":"close")));
 *
 * 设计：每个面板标题栏（38px）+ 内容区。标题栏：白底 + 底部 1px 分隔线，
 * 右侧 ▶/▼ 箭头，展开时旋转 90°（AnimationManager 220ms easeInOut）。
 * 内容区高度动画：折叠 0→contentH（easeInOut），内容 alpha 同步。
 * 手风琴模式：展开某项时自动折叠其他项。
 */
public class AstCollapse extends AstAbstractComponent {
    private final List<CollapseItem> items = new ArrayList<CollapseItem>();
    private boolean accordion;
    private Consumer<int[]> changeListener; // 当前展开项索引数组

    public AstCollapse() { this(false); }
    public AstCollapse(boolean accordion) {
        this.accordion = accordion;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void setAccordion(boolean a) { this.accordion = a; }
    public boolean isAccordion() { return accordion; }

    public void addItem(String title, JComponent content) {
        if (title == null) throw new IllegalArgumentException("title must not be null");
        if (content == null) throw new IllegalArgumentException("content must not be null");
        CollapseItem item = new CollapseItem(this, items.size(), title, content);
        items.add(item);
        add(item);
        add(Box.createVerticalStrut(0)); // 间距由 item 内分隔线处理
        revalidate(); repaint();
    }

    public void expand(int idx) {
        if (idx < 0 || idx >= items.size()) throw new IndexOutOfBoundsException("idx " + idx);
        if (accordion) {
            for (int i = 0; i < items.size(); i++) {
                if (i == idx) items.get(i).setOpen(true);
                else items.get(i).setOpen(false);
            }
        } else {
            items.get(idx).setOpen(true);
        }
        fireChange();
    }

    public void collapse(int idx) {
        if (idx < 0 || idx >= items.size()) throw new IndexOutOfBoundsException("idx " + idx);
        items.get(idx).setOpen(false);
        fireChange();
    }

    public void toggle(int idx) {
        if (idx < 0 || idx >= items.size()) throw new IndexOutOfBoundsException("idx " + idx);
        CollapseItem it = items.get(idx);
        boolean willOpen = !it.isOpen;
        if (accordion && willOpen) {
            for (int i = 0; i < items.size(); i++) {
                if (i == idx) items.get(i).setOpen(true);
                else items.get(i).setOpen(false);
            }
        } else {
            it.setOpen(willOpen);
        }
        fireChange();
    }

    public boolean isOpen(int idx) {
        if (idx < 0 || idx >= items.size()) throw new IndexOutOfBoundsException("idx " + idx);
        return items.get(idx).isOpen;
    }

    public int getItemCount() { return items.size(); }

    public int[] getOpenIndices() {
        java.util.List<Integer> open = new java.util.ArrayList<Integer>();
        for (int i = 0; i < items.size(); i++) if (items.get(i).isOpen) open.add(i);
        int[] arr = new int[open.size()];
        for (int i = 0; i < open.size(); i++) arr[i] = open.get(i);
        return arr;
    }

    public void setChangeListener(Consumer<int[]> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.changeListener = l;
    }

    void fireChange() {
        if (changeListener != null) changeListener.accept(getOpenIndices());
    }

    // --- CollapseItem ---
    private static final class CollapseItem extends AstAbstractComponent {
        private final AstCollapse parent;
        private final int index;
        private final String title;
        private final JComponent content;
        private boolean isOpen;

        private static final int HEADER_H = 38;
        private int contentH = 100; // 内容首选高度

        @Override
        protected void initComponent() {
            super.initComponent();
            anim.register("open", 220, Easing::easeInOut);
            anim.register("arrow", 220, Easing::easeInOut);
        }

        @Override
        protected void selfCheck() {
            // CollapseItem 的自检由 AstCollapse.selfCheck() 覆盖
        }

        CollapseItem(AstCollapse parent, int index, String title, JComponent content) {
            this.parent = parent;
            this.index = index;
            this.title = title;
            this.content = content;
            setLayout(new BorderLayout());
            // 内容高度
            contentH = Math.max(content.getPreferredSize().height, 40);
            // 标题栏作为 NORTH
            JPanel header = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = createGraphics(g);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    // 底部分隔线（除展开状态顶部有内容外）
                    g2.setColor(theme().getBorderBase());
                    g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                    g2.dispose();
                }
            };
            header.setPreferredSize(new Dimension(Short.MAX_VALUE, HEADER_H));
            header.setMaximumSize(new Dimension(Short.MAX_VALUE, HEADER_H));
            header.setOpaque(false);
            JLabel lbl = new JLabel(title);
            lbl.setFont(theme().getFontBase().deriveFont(15f));
            lbl.setForeground(theme().getTextPrimary());
            assertContrast(theme().getTextPrimary(), Color.WHITE, "AstCollapse header title");
            lbl.setBorder(new EmptyBorder(0, 16, 0, 0));
            header.add(lbl, BorderLayout.CENTER);
            // 箭头图标面板（自绘）
            JPanel arrowPanel = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = createGraphics(g);
                    g2.setColor(theme().getTextRegular());
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int aw = 8, ah = 8;
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    float angle = (float) (Math.PI / 2 * anim.getProgress("arrow")); // 0→90°
                    // ▶ 形状旋转 angle
                    Path2D p = new Path2D.Float();
                    double s = 4.5;
                    p.moveTo(cx + s, cy);
                    p.lineTo(cx - s, cy - s);
                    p.lineTo(cx - s, cy + s);
                    p.closePath();
                    java.awt.geom.AffineTransform at = java.awt.geom.AffineTransform.getRotateInstance(angle, cx, cy);
                    p.transform(at);
                    g2.fill(p);
                    g2.dispose();
                }
            };
            arrowPanel.setPreferredSize(new Dimension(28, HEADER_H));
            arrowPanel.setOpaque(false);
            arrowPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            arrowPanel.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { parent.toggle(index); }
            });
            header.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { parent.toggle(index); }
            });
            header.add(arrowPanel, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);
            // 内容区
            JPanel contentWrap = new JPanel(new BorderLayout()) {
                @Override public Dimension getPreferredSize() {
                    int h = Math.round(contentH * anim.getProgress("open"));
                    return new Dimension(Short.MAX_VALUE, h);
                }
                @Override public Dimension getMaximumSize() { return getPreferredSize(); }
                @Override public boolean isOptimizedDrawingEnabled() { return false; }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = createGraphics(g);
                    float p = anim.getProgress("open");
                    // 内容背景
                    g2.setColor(theme().getFillBase());
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    // 内容 alpha
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, p))));
                    g2.setColor(theme().getFillBase());
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    // 动画进行中时重新布局以更新内容高度
                    if (p > 0f && p < 1f) {
                        CollapseItem.this.revalidate();
                    }
                }
            };
            contentWrap.setOpaque(false);
            contentWrap.add(content, BorderLayout.CENTER);
            add(contentWrap, BorderLayout.CENTER);
            setMaximumSize(new Dimension(Short.MAX_VALUE, HEADER_H + contentH));
        }

        void setOpen(boolean open) {
            this.isOpen = open;
            anim.go("open", anim.getProgress("open"), open ? 1f : 0f);
            anim.go("arrow", anim.getProgress("arrow"), open ? 1f : 0f);
        }
    }

    // --- Self-check ---
    @Override
    protected void selfCheck() {
        boolean threw = false;
        AstCollapse c0 = new AstCollapse();
        try { c0.addItem(null, new JPanel()); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null title"; threw = false;
        try { c0.addItem("x", null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null content"; threw = false;
        try { c0.expand(0); } catch (IndexOutOfBoundsException e) { threw = true; }
        assert threw : "expand OOB"; threw = false;
        try { c0.collapse(0); } catch (IndexOutOfBoundsException e) { threw = true; }
        assert threw : "collapse OOB"; threw = false;
        try { c0.toggle(0); } catch (IndexOutOfBoundsException e) { threw = true; }
        assert threw : "toggle OOB"; threw = false;
        try { c0.isOpen(0); } catch (IndexOutOfBoundsException e) { threw = true; }
        assert threw : "isOpen OOB"; threw = false;
        try { c0.setChangeListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null listener";

        AstCollapse c = new AstCollapse(false);
        c.addItem("面板一", makeContent("内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一"));
        c.addItem("面板二", makeContent("内容二"));
        c.addItem("面板三", makeContent("内容三"));
        assert c.getItemCount() == 3 : "3 items";
        assert !c.isOpen(0) : "0 closed default";
        assert !c.isOpen(1) : "1 closed default";
        assert c.getOpenIndices().length == 0 : "no open default";
        // toggle
        c.toggle(1);
        assert c.isOpen(1) : "1 open after toggle";
        assert !c.isOpen(0) : "0 still closed";
        assert c.getOpenIndices().length == 1 : "1 open";
        assert c.getOpenIndices()[0] == 1 : "open idx 1";
        // toggle again → close
        c.toggle(1);
        assert !c.isOpen(1) : "1 closed after toggle again";
        // expand + accordion off → multiple open
        c.expand(0); c.expand(2);
        assert c.isOpen(0) && c.isOpen(2) : "both open (non-accordion)";
        assert c.getOpenIndices().length == 2 : "2 open";
        // collapse
        c.collapse(0);
        assert !c.isOpen(0) : "0 closed";

        // accordion mode
        AstCollapse ca = new AstCollapse(true);
        ca.addItem("A", makeContent("aaa"));
        ca.addItem("B", makeContent("bbb"));
        ca.addItem("C", makeContent("ccc"));
        ca.expand(0);
        assert ca.isOpen(0) : "A open";
        ca.expand(1); // accordion → A closes
        assert ca.isOpen(1) : "B open";
        assert !ca.isOpen(0) : "A closed (accordion)";
        assert ca.getOpenIndices().length == 1 : "accordion 1 open";

        // listener
        final int[] lastCount = {-1};
        ca.setChangeListener(arr -> lastCount[0] = arr.length);
        ca.toggle(2);
        try { Thread.sleep(50); } catch (Throwable ignore) {}
        assert ca.isOpen(2) : "C open";
        assert !ca.isOpen(1) : "B closed";
        assert lastCount[0] == 1 : "listener fired";

        // 对比度
        assertContrast(theme().getTextPrimary(), Color.WHITE, "collapse header title on white");

        // Paint test on EDT
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("Collapse SC");
            jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            try {
                AstCollapse cp = new AstCollapse(true);
                cp.addItem("面板一", makeContent("内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一"));
                cp.addItem("面板二", makeContent("内容二"));
                jf.getContentPane().setLayout(new BorderLayout());
                jf.getContentPane().add(cp, BorderLayout.NORTH);
                jf.pack();
                cp.expand(0);
                try { Thread.sleep(60); } catch (Throwable ignore) {}
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(300, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                gg.setColor(Color.WHITE); gg.fillRect(0, 0, 300, 200);
                try { cp.paint(gg); } finally { gg.dispose(); }
            } finally {
                jf.dispose();
            }
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstCollapse self-check OK");
    }

    private JComponent makeContent(String text) {
        JLabel l = new JLabel(text);
        l.setFont(theme().getFontBase().deriveFont(14f));
        l.setBorder(new EmptyBorder(16, 16, 16, 16));
        return l;
    }

    public static void main(String[] args) {
        new AstCollapse().selfCheck();
    }
}
