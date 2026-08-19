package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;

import javax.swing.*;
import java.awt.*;

public class Badge extends JComponent {
    private final Animator popAnim = new Animator(200, Easing::easeOut, v -> { scale = v; repaint(); });
    private float scale = 1f;
    private int count;
    private boolean dot;
    private JComponent content;

    public Badge() {
        setOpaque(false);
        setLayout(new BorderLayout());
    }

    public void setContent(JComponent c) {
        if (content != null) remove(content);
        content = c;
        add(content, BorderLayout.CENTER);
        revalidate();
    }

    public void setCount(int c) {
        count = c;
        scale = 0.6f;
        popAnim.go(scale, 1f);
        repaint();
    }

    public void setDot(boolean b) { dot = b; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        if (count <= 0 && !dot) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = getWidth() - 9;
        int cy = 9;
        float s = 0.6f + 0.4f * scale;
        g2.translate(cx, cy);
        g2.scale(s, s);
        g2.translate(-cx, -cy);
        int size = dot ? 10 : 18;
        g2.setColor(new Color(0xF56C6C));
        g2.fillOval(cx - size / 2, cy - size / 2, size, size);
        if (!dot && count > 99) {
            size = 24;
            g2.setColor(new Color(0xF56C6C));
            g2.fillRoundRect(cx - size / 2, cy - size / 2, size, size, 8, 8);
        }
        if (!dot) {
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics(getFont());
            String text = count > 99 ? "99+" : String.valueOf(count);
            g2.drawString(text, cx - fm.stringWidth(text) / 2f, cy - fm.getHeight() / 2f + fm.getAscent());
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return content != null ? content.getPreferredSize() : new Dimension(48, 48);
    }
}
