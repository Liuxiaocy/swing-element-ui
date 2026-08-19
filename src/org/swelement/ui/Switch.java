package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Switch extends JToggleButton {
    private final Animator slideAnim = new Animator(300, Easing::easeInOut, v -> { slide = v; repaint(); });
    private float slide;

    public Switch() {
        setOpaque(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addItemListener(e -> slideAnim.go(slide, isSelected() ? 1f : 0f));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = 44, h = 22;
        int y = (getHeight() - h) / 2;

        Color track = isEnabled()
            ? ElementTheme.lerp(new Color(0xDCDFE6), ElementTheme.PRIMARY, slide)
            : new Color(0xE4E7ED);
        g2.setColor(track);
        g2.fill(new RoundRectangle2D.Float(0, y, w, h, h, h));

        int knob = 18;
        int x = Math.round(2 + slide * (w - knob - 4));
        g2.setColor(Color.WHITE);
        g2.fillOval(x, y + 2, knob, knob);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(44, 22); }
}
