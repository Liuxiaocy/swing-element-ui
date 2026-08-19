package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;

public class Progress extends JComponent {
    private final Animator fillAnim = new Animator(300, Easing::easeOut, v -> { shown = v; repaint(); });
    private float shown;
    private int value;
    private boolean showText = true;

    public Progress() {
        setOpaque(false);
        setPreferredSize(new Dimension(320, 20));
    }

    public void setValue(int v) {
        value = Math.max(0, Math.min(100, v));
        fillAnim.go(shown, value / 100f);
        repaint();
    }

    public void setShowText(boolean b) { showText = b; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int textW = showText ? 46 : 0;
        int trackW = getWidth() - textW;
        int y = (getHeight() - 6) / 2;
        g2.setColor(new Color(0xEBEEF5));
        g2.fillRoundRect(0, y, trackW, 6, 6, 6);
        int fillW = Math.round(trackW * shown);
        g2.setColor(ElementTheme.PRIMARY);
        g2.fillRoundRect(0, y, fillW, 6, 6, 6);
        if (showText) {
            g2.setColor(new Color(0x606266));
            FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT.deriveFont(12f));
            g2.setFont(ElementTheme.FONT.deriveFont(12f));
            g2.drawString(value + "%", trackW + 6, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        }
        g2.dispose();
    }
}
