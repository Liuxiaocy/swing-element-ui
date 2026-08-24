package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.core.StickyToggleModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AstRadio extends JRadioButton {
    private final Animator dotAnim = new Animator(200, Easing::easeOut, v -> { dot = v; repaint(); });
    private final Animator borderAnim = new Animator(200, Easing::easeInOut, v -> { border = v; repaint(); });
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private float dot, border, hover;

    public AstRadio(String text) {
        super(text);
        setModel(new StickyToggleModel()); // 快速点击时指针移出边界仍能完成翻转
        setOpaque(false);
        setFocusPainted(false);
        setFont(ElementTheme.FONT);
        setForeground(ElementTheme.TEXT_REGULAR);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (isEnabled()) hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); }
        });
        addItemListener(e -> {
            borderAnim.go(border, isSelected() ? 1f : 0f);
            dotAnim.go(dot, isSelected() ? 1f : 0f);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int y = (getHeight() - 16) / 2;
        int cx = 8, cy = y + 8;

        Color borderColor = isEnabled()
            ? ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, Math.max(border, hover))
            : new Color(0xC0C4CC);
        g2.setColor(ElementTheme.FILL_BLANK);
        g2.fillOval(cx - 8, cy - 8, 16, 16);
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(cx - 8, cy - 8, 16, 16);

        float r = 4f * (float) Math.sqrt(dot);
        g2.setColor(isEnabled() ? ElementTheme.PRIMARY : new Color(0xC0C4CC));
        g2.fillOval((int) (cx - r), (int) (cy - r), (int) (2 * r), (int) (2 * r));

        g2.setColor(isEnabled() ? ElementTheme.TEXT_REGULAR : new Color(0xC0C4CC));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), 24, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText()) + 28, 20);
    }
}