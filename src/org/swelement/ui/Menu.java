package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.AnimatedPopup;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Menu extends JComponent {
    private static final int HEADER_H = 40;

    private class Entry {
        final String label;
        final Runnable action;
        final String[] subLabels;
        final Runnable[] subActions;
        final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
        float hover;

        Entry(String label, Runnable action) { this(label, action, null, null); }

        Entry(String label, Runnable action, String[] subLabels, Runnable[] subActions) {
            this.label = label;
            this.action = action;
            this.subLabels = subLabels;
            this.subActions = subActions;
        }

        boolean isSub() { return subLabels != null; }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Animator indXAnim = new Animator(250, Easing::easeInOut, v -> { indX = v; repaint(); });
    private final Animator indWAnim = new Animator(250, Easing::easeInOut, v -> { indW = v; repaint(); });
    private float indX, indW;
    private int active = -1;
    private final AnimatedPopup subPopup = new AnimatedPopup();
    private final JPanel subList = new JPanel();

    public Menu() {
        setOpaque(false);
        setPreferredSize(new Dimension(520, HEADER_H));
        subList.setOpaque(false);
        subList.setLayout(new BoxLayout(subList, BoxLayout.Y_AXIS));
        subPopup.getContent().add(subList, BorderLayout.CENTER);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
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
                for (Entry en : entries) en.hoverAnim.go(en.hover, 0f);
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                if (e.getY() > HEADER_H) return;
                int x = 0;
                for (Entry en : entries) {
                    int w = entryWidth(en);
                    boolean over = e.getX() >= x && e.getX() < x + w;
                    en.hoverAnim.go(en.hover, over ? 1f : 0f);
                    x += w;
                }
            }
        });
    }

    public void addMenuItem(String label, Runnable action) { entries.add(new Entry(label, action)); repaint(); }

    public void addSubMenu(String label, String[] subLabels, Runnable[] subActions) {
        entries.add(new Entry(label, null, subLabels, subActions));
        repaint();
    }

    public void setActive(int index) { active = index; slideIndicator(); repaint(); }

    private int entryWidth(Entry en) {
        return 24 + getFontMetrics(ElementTheme.FONT).stringWidth(en.label);
    }

    private void onEntryClick(int i, Entry en) {
        setActive(i);
        if (!en.isSub()) {
            if (en.action != null) en.action.run();
            return;
        }
        subList.removeAll();
        for (int s = 0; s < en.subLabels.length; s++) {
            final Runnable a = en.subActions[s];
            JLabel item = new JLabel(en.subLabels[s]) {
                private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
                private float hover;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (hover > 0) {
                        g2.setColor(ElementTheme.lerp(Color.WHITE, new Color(0xECF5FF), hover));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    g2.setColor(ElementTheme.TEXT_REGULAR);
                    FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT);
                    g2.drawString(getText(), 16, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                    g2.dispose();
                }
            };
            item.setOpaque(false);
            item.setPreferredSize(new Dimension(140, 32));
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final int fi = s;
            item.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { item.repaint(); }
                public void mouseExited(MouseEvent e) { item.repaint(); }
                public void mouseClicked(MouseEvent e) {
                    subPopup.setVisible(false);
                    if (a != null) a.run();
                }
            });
            subList.add(item);
        }
        subList.revalidate();
        int x = 0;
        for (int k = 0; k < i; k++) x += entryWidth(entries.get(k));
        subPopup.getContent().setPreferredSize(new Dimension(140, subList.getPreferredSize().height));
        subPopup.show(this, x, HEADER_H);
    }

    private void slideIndicator() {
        int x = 0;
        for (int i = 0; i < entries.size(); i++) {
            int w = entryWidth(entries.get(i));
            if (i == active) {
                indXAnim.go(indX, x);
                indWAnim.go(indW, w);
                return;
            }
            x += w;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT);
        int x = 0;
        for (Entry en : entries) {
            int w = entryWidth(en);
            g2.setColor(en.hover > 0 ? ElementTheme.lerp(Color.WHITE, new Color(0xECF5FF), en.hover) : Color.WHITE);
            g2.fillRect(x, 0, w, HEADER_H);
            g2.setColor(en.hover > 0.5f || entries.indexOf(en) == active ? ElementTheme.PRIMARY : new Color(0x303133));
            g2.setFont(ElementTheme.FONT);
            g2.drawString(en.label, x + 12, (HEADER_H - fm.getHeight()) / 2f + fm.getAscent());
            x += w;
        }
        if (indX == 0f && indW == 0f && active >= 0) slideIndicator();
        if (active >= 0) {
            g2.setColor(ElementTheme.PRIMARY);
            g2.fillRect(Math.round(indX), HEADER_H - 2, Math.round(indW), 2);
        }
        g2.dispose();
    }
}
