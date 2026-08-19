package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Tag extends JComponent {
    public static final int PRIMARY = 0, SUCCESS = 1, WARNING = 2, DANGER = 3, INFO = 4;

    private static final Color[] BG = {new Color(0xECF5FF), new Color(0xF0F9EB), new Color(0xFDF6EC), new Color(0xFEF0F0), new Color(0xF4F4F5)};
    private static final Color[] FG = {ElementTheme.PRIMARY, ElementTheme.SUCCESS, ElementTheme.WARNING, ElementTheme.DANGER, ElementTheme.INFO};
    private static final Color[] BORDER = {new Color(0xD9ECFF), new Color(0xE1F3D8), new Color(0xFAECD8), new Color(0xFDE2E2), new Color(0xE9E9EB)};

    private Runnable onClosed;
    private int origW, origH;

    private final Animator closeAnim = new Animator(200, Easing::easeInOut, v -> {
        float w = origW * (1 - v);
        setPreferredSize(new Dimension(Math.max(1, Math.round(w)), origH));
        revalidate();
        if (v >= 1f && onClosed != null) {
            Runnable r = onClosed;
            onClosed = null;
            r.run();
        }
        repaint();
    });
    private final int type;
    private final boolean closable;
    private String text;

    public Tag(String text, int type, boolean closable) {
        this.text = text;
        this.type = type;
        this.closable = closable;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setText(String t) { text = t; repaint(); }

    public void close(Runnable onClosed) {
        this.onClosed = onClosed;
        origW = getWidth();
        origH = getHeight();
        closeAnim.go(0f, 1f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(BG[type]);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
        g2.setColor(BORDER[type]);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
        g2.setColor(FG[type]);
        FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT.deriveFont(12f));
        g2.setFont(ElementTheme.FONT.deriveFont(12f));
        String suffix = closable ? "  ×" : "";
        g2.drawString(text + suffix, 8, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(ElementTheme.FONT.deriveFont(12f));
        return new Dimension(16 + fm.stringWidth(text + (closable ? "  ×" : "")), fm.getHeight() + 8);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (closable) {
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (!isEnabled()) return;
                    FontMetrics fm = getFontMetrics(ElementTheme.FONT.deriveFont(12f));
                    int xw = 16 + fm.stringWidth(text);
                    if (e.getX() > xw) close(() -> {});
                }
            });
        }
    }
}
