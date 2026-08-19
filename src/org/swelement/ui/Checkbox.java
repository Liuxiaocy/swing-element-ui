package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public class Checkbox extends JCheckBox {
    private final Animator fillAnim = new Animator(200, Easing::easeInOut, v -> { fill = v; repaint(); });
    private final Animator checkAnim = new Animator(200, Easing::easeOut, v -> { check = v; repaint(); });
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private float fill, check, hover;

    public Checkbox(String text) {
        super(text);
        setOpaque(false);
        setFocusPainted(false);
        setFont(ElementTheme.FONT);
        setForeground(ElementTheme.TEXT_REGULAR);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); }
        });
        addItemListener(e -> {
            fillAnim.go(fill, isSelected() ? 1f : 0f);
            checkAnim.go(check, isSelected() ? 1f : 0f);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int y = (getHeight() - 16) / 2;
        Color border = isEnabled()
            ? ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, Math.max(fill, hover))
            : new Color(0xC0C4CC);
        Color bg = ElementTheme.lerp(ElementTheme.FILL_BLANK, ElementTheme.PRIMARY, fill);
        if (!isEnabled()) bg = ElementTheme.lerp(ElementTheme.FILL_BLANK, new Color(0xC0C4CC), fill);

        Shape box = new RoundRectangle2D.Float(0, y, 16, 16, 4, 4);
        g2.setColor(bg);
        g2.fill(box);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(box);

        if (check > 0) {  // 勾号描边动画：裁剪窗口从左到右揭示
            Shape old = g2.getClip();
            g2.clip(new Rectangle2D.Float(0, y - 2, 12 * check + 1, 20));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            p.moveTo(4, y + 9);
            p.lineTo(7, y + 12);
            p.lineTo(12, y + 5);
            g2.draw(p);
            g2.setClip(old);
        }

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
