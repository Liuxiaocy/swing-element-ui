package org.swelement.ui;

import org.swelement.core.AnimatedPopup;
import org.swelement.core.Easing;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class AstMenu extends AstInteractiveComponent {
    private static final int HEADER_H = 40;

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
    private final AnimatedPopup subPopup = new AnimatedPopup();
    private final JPanel subList = new JPanel();

    public AstMenu() {
        setPreferredSize(new Dimension(520, HEADER_H));
        setFont(UIManager.getFont("Label.font"));
        anim.register("indX", 250, Easing::easeInOut);
        anim.register("indW", 250, Easing::easeInOut);
        subList.setOpaque(false);
        subList.setLayout(new BoxLayout(subList, BoxLayout.Y_AXIS));
        subPopup.getContent().add(subList, BorderLayout.CENTER);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                if (e.getY() > HEADER_H) return;
                int x = 0;
                for (int i = 0; i < entries.size(); i++) {
                    Entry en = entries.get(i);
                    int w = entryWidth(en);
                    if (e.getX() >= x && e.getX() < x + w) {
                        onEntryClick(i, en);
                        return;
                    }
                    x += w;
                }
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
                if (e.getY() > HEADER_H) return;
                int x = 0;
                for (int i = 0; i < entries.size(); i++) {
                    Entry en = entries.get(i);
                    int w = entryWidth(en);
                    boolean over = e.getX() >= x && e.getX() < x + w;
                    anim.go("hover_" + i, anim.getProgress("hover_" + i), over ? 1f : 0f);
                    x += w;
                }
            }
        });
    }

    public void addMenuItem(String label, Runnable action) {
        int idx = entries.size();
        entries.add(new Entry(label, action, idx));
        anim.register("hover_" + idx, 150, Easing::easeInOut);
        repaint();
    }

    public void addSubMenu(String label, String[] subLabels, Runnable[] subActions) {
        int idx = entries.size();
        entries.add(new Entry(label, null, subLabels, subActions, idx));
        anim.register("hover_" + idx, 150, Easing::easeInOut);
        repaint();
    }

    public void setActive(int index) { active = index; slideIndicator(); repaint(); }

    private int entryWidth(Entry en) {
        return 24 + getFontMetrics(getFont()).stringWidth(en.label);
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
        int x = 0;
        for (int k = 0; k < i; k++) x += entryWidth(entries.get(k));
        subPopup.getContent().setPreferredSize(new Dimension(140, subList.getPreferredSize().height));
        subPopup.show(this, x, HEADER_H);
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
        int x = 0;
        for (int i = 0; i < entries.size(); i++) {
            int w = entryWidth(entries.get(i));
            if (i == active) {
                anim.go("indX", anim.getProgress("indX"), x);
                anim.go("indW", anim.getProgress("indW"), w);
                return;
            }
            x += w;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        FontMetrics fm = g2.getFontMetrics(getFont());
        float indX = anim.getProgress("indX");
        float indW = anim.getProgress("indW");
        int x = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry en = entries.get(i);
            int w = entryWidth(en);
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
            g2.fillRect(Math.round(indX), HEADER_H - 2, Math.round(indW), 2);
        }
        g2.dispose();
    }

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
        assert true; // 不抛异常即通过

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

        System.out.println("AstMenu self-check OK");
    }

    public static void main(String[] args) {
        new AstMenu().selfCheck();
    }
}
