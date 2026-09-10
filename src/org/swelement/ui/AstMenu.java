package org.swelement.ui;

import org.swelement.core.AnimatedPopup;
import org.swelement.core.Easing;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class AstMenu extends AstInteractiveComponent {
    public static final int MODE_HORIZONTAL = 0;
    public static final int MODE_VERTICAL = 1;

    private static final int HEADER_H = 40;
    private static final int ROW_H = HEADER_H;          // 竖向模式行高，沿用 HEADER_H
    private static final int DEFAULT_SIDEBAR_W = 200;  // 竖向模式默认侧栏宽

    private class Entry {
        final String label;
        final Runnable action;
        final String[] subLabels;
        final Runnable[] subActions;
        final int index;

        Entry(String label, Runnable action, int idx) { this(label, action, null, null, idx); }

        Entry(String label, Runnable action, String[] subLabels, Runnable[] subActions, int idx) {
            this.label = label;
            this.action = action;
            this.subLabels = subLabels;
            this.subActions = subActions;
            this.index = idx;
        }

        boolean isSub() { return subLabels != null; }
    }

    private final List<Entry> entries = new ArrayList<>();
    private int active = -1;
    private int mode = MODE_HORIZONTAL;
    private int sidebarWidth = DEFAULT_SIDEBAR_W;
    private final AnimatedPopup subPopup = new AnimatedPopup();
    private final JPanel subList = new JPanel();

    public AstMenu() {
        setFont(UIManager.getFont("Label.font"));
        anim.register("indX", 250, Easing::easeInOut);
        anim.register("indW", 250, Easing::easeInOut);
        anim.register("indY", 250, Easing::easeInOut);   // 竖向模式指示器（左竖条）
        anim.register("indH", 250, Easing::easeInOut);
        subList.setOpaque(false);
        subList.setLayout(new BoxLayout(subList, BoxLayout.Y_AXIS));
        subPopup.getContent().add(subList, BorderLayout.CENTER);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                int idx = entryIndexAt(e.getX(), e.getY());
                if (idx < 0) return;
                onEntryClick(idx, entries.get(idx));
            }
            public void mouseExited(MouseEvent e) {
                for (int i = 0; i < entries.size(); i++) {
                    anim.go("hover_" + i, anim.getProgress("hover_" + i), 0f);
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                if (!isEnabled()) return;
                int idx = entryIndexAt(e.getX(), e.getY());
                for (int i = 0; i < entries.size(); i++) {
                    anim.go("hover_" + i, anim.getProgress("hover_" + i), i == idx ? 1f : 0f);
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (!isEnabled() || entries.isEmpty()) return;
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
        recomputePreferredSize();
    }

    public void addMenuItem(String label, Runnable action) {
        int idx = entries.size();
        entries.add(new Entry(label, action, idx));
        anim.register("hover_" + idx, 150, Easing::easeInOut);
        recomputePreferredSize();
        repaint();
    }

    public void addSubMenu(String label, String[] subLabels, Runnable[] subActions) {
        int idx = entries.size();
        entries.add(new Entry(label, null, subLabels, subActions, idx));
        anim.register("hover_" + idx, 150, Easing::easeInOut);
        recomputePreferredSize();
        repaint();
    }

    public void setActive(int index) { active = index; slideIndicator(); repaint(); }

    /** 方向键在条目间移动 active（clamp，不回绕）。包内可见，供 selfCheck 行为断言。 */
    void moveActive(int dir) {
        if (entries.isEmpty()) return;
        int n = entries.size();
        if (active < 0) { setActive(dir > 0 ? 0 : n - 1); return; }
        int next = active + dir;
        if (next < 0) next = 0;
        if (next >= n) next = n - 1;
        setActive(next);
    }

    /** 当前激活项索引。包内可见，供 selfCheck 行为断言。 */
    int getActiveIndex() { return active; }

    public void setMode(int m) {
        if (m != MODE_HORIZONTAL && m != MODE_VERTICAL) throw new IllegalArgumentException("invalid mode: " + m);
        this.mode = m;
        recomputePreferredSize();
        repaint();
    }
    public int getMode() { return mode; }
    public boolean isVertical() { return mode == MODE_VERTICAL; }

    public void setSidebarWidth(int w) {
        if (w <= 0) throw new IllegalArgumentException("sidebarWidth must be > 0");
        this.sidebarWidth = w;
        recomputePreferredSize();
        repaint();
    }

    /** 主轴上的条目尺寸：水平=宽度，垂直=行高。 */
    private int entryExtent(Entry en) {
        if (mode == MODE_VERTICAL) return ROW_H;
        return 24 + getFontMetrics(getFont()).stringWidth(en.label);
    }

    /** 命中测试：返回指针下的条目索引，越界/空白返回 -1。包内可见，供 selfCheck 行为断言。 */
    int entryIndexAt(int px, int py) {
        if (entries.isEmpty()) return -1;
        if (mode == MODE_VERTICAL) {
            if (px < 0 || px >= sidebarWidth) return -1;
            if (py < 0 || py >= entries.size() * ROW_H) return -1;
            return py / ROW_H;
        }
        if (py < 0 || py >= HEADER_H) return -1;
        int x = 0;
        for (int i = 0; i < entries.size(); i++) {
            int w = entryExtent(entries.get(i));
            if (x <= px && px < x + w) return i;
            x += w;
        }
        return -1;
    }

    private void recomputePreferredSize() {
        if (mode == MODE_VERTICAL) {
            setPreferredSize(new Dimension(sidebarWidth, entries.size() * ROW_H));
        } else {
            int total = 0;
            for (Entry en : entries) total += entryExtent(en);
            setPreferredSize(new Dimension(total, HEADER_H));
        }
    }

    private void onEntryClick(int i, Entry en) {
        setActive(i);
        if (!en.isSub()) {
            subPopup.setVisible(false);
            if (en.action != null) en.action.run();
            return;
        }
        subList.removeAll();
        for (int s = 0; s < en.subLabels.length; s++) {
            final Runnable a = en.subActions[s];
            SubMenuItem item = new SubMenuItem(en.subLabels[s], a);
            subList.add(item);
        }
        subList.revalidate();
        if (mode == MODE_VERTICAL) {
            int y = i * ROW_H;
            subPopup.getContent().setPreferredSize(new Dimension(160, subList.getPreferredSize().height));
            subPopup.show(this, getWidth(), y);          // 向右弹出
        } else {
            int x = 0;
            for (int k = 0; k < i; k++) x += entryExtent(entries.get(k));
            subPopup.getContent().setPreferredSize(new Dimension(140, subList.getPreferredSize().height));
            subPopup.show(this, x, HEADER_H);            // 向下弹出
        }
    }

    /** 子菜单项：内部组件，继承 AstInteractiveComponent */
    private class SubMenuItem extends AstInteractiveComponent {
        private final String text;
        private final Runnable action;

        SubMenuItem(String text, Runnable action) {
            this.text = text;
            this.action = action;
            anim.register("hover", 150, Easing::easeInOut);
            setPreferredSize(new Dimension(140, 32));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { if (isEnabled()) anim.go("hover", anim.getProgress("hover"), 1f); }
                public void mouseExited(MouseEvent e) { anim.go("hover", anim.getProgress("hover"), 0f); }
                public void mousePressed(MouseEvent e) {
                    if (!isEnabled()) return;
                    subPopup.setVisible(false);
                    if (action != null) action.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            float hover = anim.getProgress("hover");
            if (hover > 0) {
                g2.setColor(lerp(Color.WHITE, new Color(0xECF5FF), hover));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.setFont(getFont());
            g2.setColor(isEnabled() ? theme().getTextRegular() : new Color(0xC0C4CC));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 16, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }

        @Override
        protected void selfCheck() { }
    }

    private void slideIndicator() {
        int off = 0;
        for (int i = 0; i < entries.size(); i++) {
            int sz = entryExtent(entries.get(i));
            if (i == active) {
                if (mode == MODE_VERTICAL) {
                    anim.go("indY", anim.getProgress("indY"), off);
                    anim.go("indH", anim.getProgress("indH"), sz);
                } else {
                    anim.go("indX", anim.getProgress("indX"), off);
                    anim.go("indW", anim.getProgress("indW"), sz);
                }
                return;
            }
            off += sz;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        FontMetrics fm = g2.getFontMetrics(getFont());
        if (mode == MODE_VERTICAL) {
            int y = 0;
            for (int i = 0; i < entries.size(); i++) {
                Entry en = entries.get(i);
                float hover = anim.getProgress("hover_" + i);
                g2.setColor(hover > 0 ? lerp(Color.WHITE, new Color(0xECF5FF), hover) : Color.WHITE);
                g2.fillRect(0, y, sidebarWidth, ROW_H);
                g2.setColor(hover > 0.5f || i == active ? theme().getPrimary() : new Color(0x303133));
                g2.setFont(getFont());
                g2.drawString(en.label, 16, y + (ROW_H - fm.getHeight()) / 2f + fm.getAscent());
                y += ROW_H;
            }
            if (active >= 0) {
                g2.setColor(theme().getPrimary());
                g2.fillRect(0, Math.round(anim.getProgress("indY")), 2, Math.round(anim.getProgress("indH")));
            }
        } else {
            int x = 0;
            for (int i = 0; i < entries.size(); i++) {
                Entry en = entries.get(i);
                int w = entryExtent(en);
                float hover = anim.getProgress("hover_" + i);
                g2.setColor(hover > 0 ? lerp(Color.WHITE, new Color(0xECF5FF), hover) : Color.WHITE);
                g2.fillRect(x, 0, w, HEADER_H);
                g2.setColor(hover > 0.5f || i == active ? theme().getPrimary() : new Color(0x303133));
                g2.setFont(getFont());
                g2.drawString(en.label, x + 12, (HEADER_H - fm.getHeight()) / 2f + fm.getAscent());
                x += w;
            }
            if (active >= 0) {
                g2.setColor(theme().getPrimary());
                g2.fillRect(Math.round(anim.getProgress("indX")), HEADER_H - 2, Math.round(anim.getProgress("indW")), 2);
            }
        }
        g2.dispose();
    }

    /**
     * 菜单不是切换型控件：selected 状态对菜单无意义。
     * 覆写为 false，避免激活时翻转一个无人消费的状态字段。
     */
    @Override
    protected boolean isToggleMode() { return false; }

    @Override
    protected void selfCheck() {
        // 1. 基础构造
        AstMenu menu = this;
        assert menu.getPreferredSize().height == 40 : "default height 40, got " + menu.getPreferredSize().height;

        // 2. 添加菜单项
        final boolean[] clicked = {false};
        menu.addMenuItem("File", () -> clicked[0] = true);
        menu.addMenuItem("Edit", null);

        // 3. setActive
        menu.setActive(0);
        assert menu.getActiveIndex() == 0 : "active index 0, got " + menu.getActiveIndex();

        // 4. 渲染不抛异常
        menu.setSize(520, 40);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(520, 40, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { menu.paint(g); } finally { g.dispose(); }

        // 5. 子菜单
        String[] subLabels = {"New", "Open"};
        Runnable[] subActions = {null, null};
        menu.addSubMenu("Help", subLabels, subActions);

        // 6. 禁用态渲染
        menu.setEnabled(false);
        java.awt.image.BufferedImage img2 = new java.awt.image.BufferedImage(520, 40, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();
        try { menu.paint(g2); } finally { g2.dispose(); }
        menu.setEnabled(true);

        // 7. 对比度断言
        assertContrast(new Color(0x303133), Color.WHITE, "AstMenu text on white");

        // 8. 竖向模式
        AstMenu vmenu = new AstMenu();
        vmenu.setMode(AstMenu.MODE_VERTICAL);
        vmenu.addMenuItem("首页", null);
        vmenu.addMenuItem("新闻", null);
        vmenu.addMenuItem("设置", null);
        Dimension vp = vmenu.getPreferredSize();
        assert vp.width == DEFAULT_SIDEBAR_W : "vertical sidebar width " + DEFAULT_SIDEBAR_W + ", got " + vp.width;
        assert vp.height == 3 * ROW_H : "vertical height = 3*ROW_H, got " + vp.height;
        // 离屏绘制竖向不抛异常
        vmenu.setSize(DEFAULT_SIDEBAR_W, 3 * ROW_H);
        java.awt.image.BufferedImage vimg = new java.awt.image.BufferedImage(DEFAULT_SIDEBAR_W, 3 * ROW_H, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D vg = vimg.createGraphics();
        try { vmenu.paint(vg); } finally { vg.dispose(); }
        // 命中测试：第 i 行 / 越界
        assert vmenu.entryIndexAt(10, 40) == 1 : "vertical row 1 hit, got " + vmenu.entryIndexAt(10, 40);
        assert vmenu.entryIndexAt(10, 5) == 0 : "vertical row 0 hit, got " + vmenu.entryIndexAt(10, 5);
        assert vmenu.entryIndexAt(10, 200) == -1 : "vertical out of range -> -1";
        assert vmenu.entryIndexAt(300, 10) == -1 : "vertical x out of sidebar -> -1";
        // 方向键 clamp（不回绕）
        vmenu.setActive(0);
        vmenu.moveActive(1); assert vmenu.getActiveIndex() == 1 : "move down -> 1, got " + vmenu.getActiveIndex();
        vmenu.moveActive(1); vmenu.moveActive(1); assert vmenu.getActiveIndex() == 2 : "clamp at last 2, got " + vmenu.getActiveIndex();
        vmenu.moveActive(1); assert vmenu.getActiveIndex() == 2 : "no wrap past last, got " + vmenu.getActiveIndex();
        vmenu.setActive(2); vmenu.moveActive(-1); assert vmenu.getActiveIndex() == 1 : "move up -> 1, got " + vmenu.getActiveIndex();
        vmenu.moveActive(-1); vmenu.moveActive(-1); assert vmenu.getActiveIndex() == 0 : "clamp at 0, got " + vmenu.getActiveIndex();
        // setMode 非法值
        boolean threw = false;
        try { vmenu.setMode(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setMode(9) must throw";
        // sidebarWidth 非法值
        threw = false;
        try { vmenu.setSidebarWidth(0); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setSidebarWidth(0) must throw";

        assertKeyboardAccessible(this, "AstMenu");
        System.out.println("AstMenu self-check OK");
    }

    public static void main(String[] args) {
        new AstMenu().selfCheck();
    }
}
