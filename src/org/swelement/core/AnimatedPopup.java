package org.swelement.core;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class AnimatedPopup extends JPopupMenu {
    private final Animator openAnim;
    private float alpha;
    private final JPanel content;

    public AnimatedPopup() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int a = Math.round(255 * alpha);
                g2.setColor(new Color(255, 255, 255, a));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.setColor(new Color(228, 231, 237, a));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setLayout(new BorderLayout());
        add(content);
        openAnim = new Animator(200, Easing::easeOut, v -> {
            alpha = v;
            content.setBorder(new EmptyBorder(Math.round(8 * (1 - v)), 0, 0, 0));
            repaint();
        });
    }

    public JPanel getContent() { return content; }

    @Override
    public void show(Component invoker, int x, int y) {
        alpha = 0f;
        super.show(invoker, x, y);
        openAnim.go(0f, 1f);
    }
}
