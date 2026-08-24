package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class AstTabs extends JComponent {
    private static final int HEADER_H = 40;

    private final List<String> titles = new ArrayList<>();
    private final Animator indXAnim = new Animator(250, Easing::easeInOut, v -> { indX = v; repaint(); });
    private final Animator indWAnim = new Animator(250, Easing::easeInOut, v -> { indW = v; repaint(); });
    private final Animator contentAnim = new Animator(200, Easing::easeInOut, v -> { contentAlpha = v; repaint(); });
    private final EventListenerList listenerList = new EventListenerList();
    private float indX, indW, contentAlpha = 1f;
    private int selected = 0;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards) {
        @Override
        protected void paintComponent(Graphics g) {
            ((Graphics2D) g).setComposite(AlphaComposite.SrcOver.derive(contentAlpha));
            super.paintComponent(g);
        }
    };

    public AstTabs() {
        setOpaque(false);
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
                    if (e.getX() >= xs[i] && e.getX() < xs[i] + 24 + getFontMetrics(ElementTheme.FONT).stringWidth(titles.get(i))) {
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
        contentAnim.go(0f, 1f);
        slideIndicator();
        repaint();
        fireStateChanged();
    }

    private int[] tabPositions() {
        FontMetrics fm = getFontMetrics(ElementTheme.FONT);
        int[] xs = new int[titles.size()];
        int x = 0;
        for (int i = 0; i < titles.size(); i++) {
            xs[i] = x;
            x += 24 + fm.stringWidth(titles.get(i));
        }
        return xs;
    }

    private void slideIndicator() {
        FontMetrics fm = getFontMetrics(ElementTheme.FONT);
        int x = 0;
        for (int i = 0; i < titles.size(); i++) {
            int w = 24 + fm.stringWidth(titles.get(i));
            if (i == selected) {
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
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT);
        int[] xs = tabPositions();
        for (int i = 0; i < titles.size(); i++) {
            g2.setColor(i == selected ? ElementTheme.PRIMARY : new Color(0x303133));
            g2.setFont(ElementTheme.FONT);
            g2.drawString(titles.get(i), xs[i] + 12, (HEADER_H - fm.getHeight()) / 2f + fm.getAscent());
        }
        if (indX == 0f && indW == 0f && !titles.isEmpty()) {
            indX = xs[selected];
            indW = 24 + fm.stringWidth(titles.get(selected));
        }
        g2.setColor(ElementTheme.PRIMARY);
        g2.fillRect(Math.round(indX), HEADER_H - 2, Math.round(indW), 2);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(480, 240); }
}
