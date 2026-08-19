package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Alert extends JComponent {
    public static final int SUCCESS = 0, WARNING = 1, INFO = 2, ERROR = 3;

    private static final Color[] COLORS = {ElementTheme.SUCCESS, ElementTheme.WARNING, ElementTheme.INFO, ElementTheme.DANGER};
    private static final Color[] BG = {new Color(0xF0F9EB), new Color(0xFDF6EC), new Color(0xF4F4F5), new Color(0xFEF0F0)};
    private static final String[] ICONS = {"\u221a", "!", "i", "\u00d7"};

    private float inP = 0f, outP;
    private Runnable onClosed;
    private int origW, origH;

    private final Animator inAnim = new Animator(300, Easing::easeOut, v -> { inP = v; repaint(); });
    private final Animator outAnim = new Animator(250, Easing::easeIn, v -> {
        outP = v;
        int h = Math.max(1, Math.round(origH * (1 - v)));
        setPreferredSize(new Dimension(origW, h));
        revalidate();
        if (v >= 1f && onClosed != null) {
            Runnable r = onClosed;
            onClosed = null;
            r.run();
        }
        repaint();
    });
    private final int type;
    private final String title, desc;
    private final boolean closable;

    public Alert(int type, String title, String desc, boolean closable) {
        this.type = type;
        this.title = title;
        this.desc = desc;
        this.closable = closable;
        setOpaque(false);
        setPreferredSize(new Dimension(360, desc == null ? 40 : 56));
        if (closable) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (!isEnabled()) return;
                    if (e.getX() > getWidth() - 28) close(() -> {});
                }
            });
        }
        inAnim.go(0f, 1f);
    }

    public void close(Runnable onClosed) {
        this.onClosed = onClosed;
        origW = getWidth();
        origH = getHeight();
        outAnim.go(0f, 1f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int a = Math.round(255 * inP * (1 - outP));
        if (a <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(BG[type].getRed(), BG[type].getGreen(), BG[type].getBlue(), a));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(COLORS[type].getRed(), COLORS[type].getGreen(), COLORS[type].getBlue(), a));
        g2.fillRect(0, 0, 4, getHeight());
        g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(ICONS[type], 16, (desc == null ? getHeight() : 22));
        g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD));
        FontMetrics tfm = g2.getFontMetrics();
        g2.drawString(title, 40, (desc == null ? getHeight() : 22) - tfm.getAscent() / 2f + tfm.getAscent() / 2f);
        if (desc != null) {
            g2.setFont(ElementTheme.FONT);
            g2.setColor(new Color(0x606266));
            g2.drawString(desc, 40, 42);
        }
        if (closable) {
            g2.setFont(ElementTheme.FONT.deriveFont(14f));
            FontMetrics xfm = g2.getFontMetrics();
            g2.setColor(new Color(0xC0C4CC));
            g2.drawString("\u00d7", getWidth() - 24 - xfm.stringWidth("\u00d7") / 2, (getHeight() + 8) / 2f);
        }
        g2.dispose();
    }
}