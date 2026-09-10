package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.framework.AstAbstractComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class AstTabs extends AstAbstractComponent {
    public static final int MODE_HORIZONTAL = 0;
    public static final int MODE_VERTICAL = 1;

    private static final int HEADER_H = 40;
    private static final int ROW_H = 40;               // 竖向模式每行高
    private static final int DEFAULT_SIDEBAR_W = 200;  // 竖向模式默认侧栏宽
    private static final int CONTENT_W = 280;          // 竖向模式预留内容区宽

    private final List<String> titles = new ArrayList<String>();
    private final EventListenerList listenerList = new EventListenerList();
    private float indXFrom, indXTo, indWFrom, indWTo;
    private float indYFrom, indYTo, indHFrom, indHTo;
    private boolean indicatorInit;
    private int selected = 0;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards) {
        @Override
        protected void paintComponent(Graphics g) {
            float contentAlpha = anim.getProgress("content");
            ((Graphics2D) g).setComposite(AlphaComposite.SrcOver.derive(contentAlpha));
            super.paintComponent(g);
        }
    };

    private int mode = MODE_HORIZONTAL;
    private int sidebarWidth = DEFAULT_SIDEBAR_W;

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("indX", 250, Easing::easeInOut);
        anim.register("indW", 250, Easing::easeInOut);
        anim.register("indY", 250, Easing::easeInOut);   // 竖向模式指示器（左侧竖条）
        anim.register("indH", 250, Easing::easeInOut);
        anim.register("content", 200, Easing::easeInOut);
        anim.setProgress("content", 1f);
    }

    public AstTabs() {
        setLayout(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setOpaque(false);
        applyModeBorders();
        add(cardPanel, BorderLayout.CENTER);
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                int idx = tabIndexAt(e.getX(), e.getY());
                if (idx >= 0) setSelectedIndex(idx);
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (!isEnabled() || titles.isEmpty()) return;
                int k = e.getKeyCode();
                if (mode == MODE_VERTICAL) {
                    if (k == KeyEvent.VK_DOWN) { moveActive(1); e.consume(); }
                    else if (k == KeyEvent.VK_UP) { moveActive(-1); e.consume(); }
                } else {
                    if (k == KeyEvent.VK_RIGHT) { moveActive(1); e.consume(); }
                    else if (k == KeyEvent.VK_LEFT) { moveActive(-1); e.consume(); }
                }
            }
        });
    }

    /** Convenience constructor: build tabs with titles + empty content panels. */
    public AstTabs(String[] tabTitles, int initialIndex) {
        this();
        for (String t : tabTitles) {
            JPanel p = new JPanel(new BorderLayout());
            p.setOpaque(false);
            addTab(t, p);
        }
        if (initialIndex >= 0 && initialIndex < tabTitles.length) {
            selected = initialIndex;
            if (initialIndex > 0) {
                // cards.show has been done by addTab for index 0 only, so switch if needed
                cards.show(cardPanel, String.valueOf(initialIndex));
            }
        }
    }

    public void addTab(String title, JComponent panel) {
        titles.add(title);
        cardPanel.add(panel, String.valueOf(titles.size() - 1));
        if (titles.size() == 1) cards.show(cardPanel, "0");
        repaint();
    }

    public int getSelectedIndex() { return selected; }

    public String getSelectedTitle() {
        return (selected >= 0 && selected < titles.size()) ? titles.get(selected) : null;
    }

    public void addChangeListener(ChangeListener l) { listenerList.add(ChangeListener.class, l); }

    public void removeChangeListener(ChangeListener l) { listenerList.remove(ChangeListener.class, l); }

    private void fireStateChanged() {
        ChangeListener[] ls = listenerList.getListeners(ChangeListener.class);
        if (ls.length == 0) return;
        javax.swing.event.ChangeEvent ev = new javax.swing.event.ChangeEvent(this);
        for (ChangeListener l : ls) l.stateChanged(ev);
    }

    public void setSelectedIndex(int i) {
        if (i < 0 || i >= titles.size() || i == selected) return;
        selected = i;
        cards.show(cardPanel, String.valueOf(i));
        anim.go("content", 0f, 1f);
        slideIndicator();
        repaint();
        fireStateChanged();
    }

    private int[] tabPositions() {
        FontMetrics fm = getFontMetrics(theme().getFontBase());
        int[] xs = new int[titles.size()];
        int x = 0;
        for (int i = 0; i < titles.size(); i++) {
            xs[i] = x;
            x += 24 + fm.stringWidth(titles.get(i));
        }
        return xs;
    }

    /** 命中测试：返回指针下的标签索引，越界/空白返回 -1。包内可见，供 selfCheck 行为断言。 */
    int tabIndexAt(int px, int py) {
        if (titles.isEmpty()) return -1;
        if (mode == MODE_VERTICAL) {
            if (px < 0 || px >= sidebarWidth) return -1;
            if (py < 0 || py >= titles.size() * ROW_H) return -1;
            return py / ROW_H;
        }
        if (py < 0 || py >= HEADER_H) return -1;
        int x = 0;
        FontMetrics fm = getFontMetrics(theme().getFontBase());
        for (int i = 0; i < titles.size(); i++) {
            int w = 24 + fm.stringWidth(titles.get(i));
            if (px >= x && px < x + w) return i;
            x += w;
        }
        return -1;
    }

    /** 方向键在标签间移动 selected（clamp，不回绕）。包内可见，供 selfCheck 行为断言。 */
    void moveActive(int dir) {
        if (titles.isEmpty()) return;
        int n = titles.size();
        int next = selected + dir;
        if (next < 0) next = 0;
        if (next >= n) next = n - 1;
        setSelectedIndex(next);
    }

    /** 当前选中索引。包内可见，供 selfCheck 行为断言。 */
    int getActiveIndex() { return selected; }

    public void setMode(int m) {
        if (m != MODE_HORIZONTAL && m != MODE_VERTICAL) throw new IllegalArgumentException("invalid mode: " + m);
        this.mode = m;
        applyModeBorders();
        revalidate();
        repaint();
    }
    public int getMode() { return mode; }
    public boolean isVertical() { return mode == MODE_VERTICAL; }

    public void setSidebarWidth(int w) {
        if (w <= 0) throw new IllegalArgumentException("sidebarWidth must be > 0");
        this.sidebarWidth = w;
        if (mode == MODE_VERTICAL) applyModeBorders();
        revalidate();
        repaint();
    }

    private void applyModeBorders() {
        if (mode == MODE_VERTICAL) {
            cardPanel.setBorder(new EmptyBorder(0, sidebarWidth, 0, 0));
        } else {
            cardPanel.setBorder(new EmptyBorder(HEADER_H, 0, 0, 0));
        }
    }

    private void slideIndicator() {
        FontMetrics fm = getFontMetrics(theme().getFontBase());
        if (mode == MODE_VERTICAL) {
            int y = 0;
            for (int i = 0; i < titles.size(); i++) {
                int h = ROW_H;
                if (i == selected) {
                    indYFrom = indYFrom + (indYTo - indYFrom) * anim.getProgress("indY");
                    indHFrom = indHFrom + (indHTo - indHFrom) * anim.getProgress("indH");
                    indYTo = y;
                    indHTo = h;
                    anim.go("indY", 0f, 1f);
                    anim.go("indH", 0f, 1f);
                    return;
                }
                y += h;
            }
        } else {
            int x = 0;
            for (int i = 0; i < titles.size(); i++) {
                int w = 24 + fm.stringWidth(titles.get(i));
                if (i == selected) {
                    indXFrom = indXFrom + (indXTo - indXFrom) * anim.getProgress("indX");
                    indWFrom = indWFrom + (indWTo - indWFrom) * anim.getProgress("indW");
                    indXTo = x;
                    indWTo = w;
                    anim.go("indX", 0f, 1f);
                    anim.go("indW", 0f, 1f);
                    return;
                }
                x += w;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        float indX = indXFrom + (indXTo - indXFrom) * anim.getProgress("indX");
        float indW = indWFrom + (indWTo - indWFrom) * anim.getProgress("indW");
        float indY = indYFrom + (indYTo - indYFrom) * anim.getProgress("indY");
        float indH = indHFrom + (indHTo - indHFrom) * anim.getProgress("indH");
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setFont(theme().getFontBase());
        FontMetrics fm = g2.getFontMetrics();
        int[] xs = tabPositions();
        for (int i = 0; i < titles.size(); i++) {
            g2.setColor(i == selected ? theme().getPrimary() : theme().getTextPrimary());
            assertContrast(theme().getTextPrimary(), Color.WHITE, "AstTabs unselected text on white");
            if (mode == MODE_VERTICAL) {
                g2.drawString(titles.get(i), 12, i * ROW_H + (ROW_H - fm.getHeight()) / 2f + fm.getAscent());
            } else {
                g2.drawString(titles.get(i), xs[i] + 12, (HEADER_H - fm.getHeight()) / 2f + fm.getAscent());
            }
        }
        if (!indicatorInit && !titles.isEmpty()) {
            indicatorInit = true;
            indXFrom = xs[selected];
            indXTo = xs[selected];
            indWFrom = 24 + fm.stringWidth(titles.get(selected));
            indWTo = indWFrom;
            indYFrom = selected * ROW_H;
            indYTo = selected * ROW_H;
            indHFrom = ROW_H;
            indHTo = ROW_H;
        }
        g2.setColor(theme().getPrimary());
        if (mode == MODE_VERTICAL) {
            g2.fillRect(0, Math.round(indY), 2, Math.round(indH));
        } else {
            g2.fillRect(Math.round(indX), HEADER_H - 2, Math.round(indW), 2);
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        if (mode == MODE_VERTICAL) {
            int h = Math.max(titles.size() * ROW_H, 200);
            return new Dimension(sidebarWidth + CONTENT_W, h);
        }
        return new Dimension(480, 240);
    }

    // --- Self-check ---

    @Override
    protected void selfCheck() {
        // Basic tab operations
        AstTabs tabs = new AstTabs(new String[]{"Tab1", "Tab2", "Tab3"}, 0);
        assert tabs.getSelectedIndex() == 0 : "initial index 0";
        assert "Tab1".equals(tabs.getSelectedTitle()) : "initial title";

        tabs.setSelectedIndex(1);
        assert tabs.getSelectedIndex() == 1 : "switched to 1";
        assert "Tab2".equals(tabs.getSelectedTitle()) : "title 2";

        tabs.setSelectedIndex(2);
        assert tabs.getSelectedIndex() == 2 : "switched to 2";

        // Invalid index ignored
        tabs.setSelectedIndex(-1);
        assert tabs.getSelectedIndex() == 2 : "invalid -1 ignored";
        tabs.setSelectedIndex(99);
        assert tabs.getSelectedIndex() == 2 : "invalid 99 ignored";

        // Same index ignored
        tabs.setSelectedIndex(2);
        assert tabs.getSelectedIndex() == 2 : "same index ignored";

        // Add tab after creation
        tabs.addTab("Tab4", new JPanel());
        assert tabs.getSelectedIndex() == 2 : "addTab doesn't change selection";

        // Change listener
        final int[] changed = {-1};
        tabs.addChangeListener(new ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                changed[0] = tabs.getSelectedIndex();
            }
        });
        tabs.setSelectedIndex(0);
        assert changed[0] == 0 : "listener fired";

        // Paint test on EDT
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            AstTabs t = new AstTabs(new String[]{"A", "B"}, 0);
            t.setBounds(0, 0, 400, 240);
            t.setSelectedIndex(1);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(400, 240, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            gg.setColor(Color.WHITE); gg.fillRect(0, 0, 400, 240);
            try { t.paint(gg); } finally { gg.dispose(); }
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);

        // Contrast: unselected tab text on white background
        assertContrast(theme().getTextPrimary(), Color.WHITE, "AstTabs unselected text on white");

        // --- 竖向模式（与 AstMenu 同一 setMode 形状）---
        AstTabs vtabs = new AstTabs(new String[]{"Tab1", "Tab2", "Tab3"}, 0);
        vtabs.setMode(AstTabs.MODE_VERTICAL);
        Dimension vp = vtabs.getPreferredSize();
        assert vp.width == DEFAULT_SIDEBAR_W + CONTENT_W : "vertical width = sidebarWidth+CONTENT_W, got " + vp.width;
        assert vp.height == Math.max(3 * ROW_H, 200) : "vertical height, got " + vp.height;

        // 命中测试
        assert vtabs.tabIndexAt(10, 40) == 1 : "vertical row1 hit, got " + vtabs.tabIndexAt(10, 40);
        assert vtabs.tabIndexAt(10, 5) == 0 : "vertical row0 hit, got " + vtabs.tabIndexAt(10, 5);
        assert vtabs.tabIndexAt(10, 200) == -1 : "vertical out of range -> -1";
        assert vtabs.tabIndexAt(300, 10) == -1 : "vertical x out of sidebar -> -1";

        // 方向键 clamp（不回绕）
        vtabs.setSelectedIndex(0);
        vtabs.moveActive(1); assert vtabs.getActiveIndex() == 1 : "move down -> 1, got " + vtabs.getActiveIndex();
        vtabs.moveActive(1); vtabs.moveActive(1); assert vtabs.getActiveIndex() == 2 : "clamp at last, got " + vtabs.getActiveIndex();
        vtabs.moveActive(1); assert vtabs.getActiveIndex() == 2 : "no wrap past last, got " + vtabs.getActiveIndex();
        vtabs.setSelectedIndex(2); vtabs.moveActive(-1); assert vtabs.getActiveIndex() == 1 : "move up -> 1, got " + vtabs.getActiveIndex();
        vtabs.moveActive(-1); vtabs.moveActive(-1); assert vtabs.getActiveIndex() == 0 : "clamp at 0, got " + vtabs.getActiveIndex();

        // 键盘可达性：组件可聚焦（AstTabs 非切换型，无 Space/Enter 绑定，故只验证可聚焦 + 方向键逻辑）
        assert vtabs.isFocusable() : "AstTabs must be focusable for keyboard nav";

        // 离屏绘制竖向不抛异常
        final Throwable[] verr = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            AstTabs t = new AstTabs(new String[]{"A", "B", "C"}, 0);
            t.setMode(AstTabs.MODE_VERTICAL);
            t.setBounds(0, 0, 480, 200);
            t.setSelectedIndex(1);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(480, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            gg.setColor(Color.WHITE); gg.fillRect(0, 0, 480, 200);
            try { t.paint(gg); } finally { gg.dispose(); }
        }}); } catch (Throwable t) { verr[0] = t; }
        if (verr[0] != null) throw new RuntimeException(verr[0]);

        // 非法参数
        boolean threw = false;
        try { vtabs.setMode(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setMode(9) must throw";
        threw = false;
        try { vtabs.setSidebarWidth(0); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setSidebarWidth(0) must throw";

        System.out.println("AstTabs self-check OK");
    }

    public static void main(String[] args) {
        new AstTabs().selfCheck();
    }
}
