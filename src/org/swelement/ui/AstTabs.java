package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.framework.AstAbstractComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class AstTabs extends AstAbstractComponent {
    private static final int HEADER_H = 40;

    private final List<String> titles = new ArrayList<String>();
    private final EventListenerList listenerList = new EventListenerList();
    private float indXFrom, indXTo, indWFrom, indWTo;
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

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("indX", 250, Easing::easeInOut);
        anim.register("indW", 250, Easing::easeInOut);
        anim.register("content", 200, Easing::easeInOut);
        anim.setProgress("content", 1f);
    }

    public AstTabs() {
        setLayout(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setOpaque(false);
        cardPanel.setBorder(new EmptyBorder(HEADER_H, 0, 0, 0));
        add(cardPanel, BorderLayout.CENTER);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                if (e.getY() > HEADER_H) return;
                int[] xs = tabPositions();
                for (int i = 0; i < titles.size(); i++) {
                    if (e.getX() >= xs[i] && e.getX() < xs[i] + 24 + getFontMetrics(theme().getFontBase()).stringWidth(titles.get(i))) {
                        setSelectedIndex(i);
                        return;
                    }
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

    private void slideIndicator() {
        FontMetrics fm = getFontMetrics(theme().getFontBase());
        int x = 0;
        for (int i = 0; i < titles.size(); i++) {
            int w = 24 + fm.stringWidth(titles.get(i));
            if (i == selected) {
                float curX = indXFrom + (indXTo - indXFrom) * anim.getProgress("indX");
                float curW = indWFrom + (indWTo - indWFrom) * anim.getProgress("indW");
                indXFrom = curX;
                indXTo = x;
                indWFrom = curW;
                indWTo = w;
                anim.go("indX", 0f, 1f);
                anim.go("indW", 0f, 1f);
                return;
            }
            x += w;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        float indX = indXFrom + (indXTo - indXFrom) * anim.getProgress("indX");
        float indW = indWFrom + (indWTo - indWFrom) * anim.getProgress("indW");
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setFont(theme().getFontBase());
        FontMetrics fm = g2.getFontMetrics();
        int[] xs = tabPositions();
        for (int i = 0; i < titles.size(); i++) {
            g2.setColor(i == selected ? theme().getPrimary() : theme().getTextPrimary());
            assertContrast(theme().getTextPrimary(), Color.WHITE, "AstTabs unselected text on white");
            g2.drawString(titles.get(i), xs[i] + 12, (HEADER_H - fm.getHeight()) / 2f + fm.getAscent());
        }
        if (!indicatorInit && !titles.isEmpty()) {
            indicatorInit = true;
            indXFrom = xs[selected];
            indXTo = xs[selected];
            indWFrom = 24 + fm.stringWidth(titles.get(selected));
            indWTo = indWFrom;
        }
        g2.setColor(theme().getPrimary());
        g2.fillRect(Math.round(indX), HEADER_H - 2, Math.round(indW), 2);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(480, 240); }

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

        System.out.println("AstTabs self-check OK");
    }

    public static void main(String[] args) {
        new AstTabs().selfCheck();
    }
}
