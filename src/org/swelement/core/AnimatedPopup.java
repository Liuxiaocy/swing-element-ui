package org.swelement.core;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class AnimatedPopup extends JComponent {
    private final Animator openAnim;
    private float alpha;
    private final JPanel content;
    private Component invoker;
    private Runnable dismissListener;

    public AnimatedPopup() {
        setOpaque(false);
        setLayout(new BorderLayout());
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
        Toolkit.getDefaultToolkit().addAWTEventListener(this::onAwtEvent, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void onAwtEvent(AWTEvent e) {
        if (e.getID() != MouseEvent.MOUSE_PRESSED || !isShowing()) return;
        MouseEvent me = (MouseEvent) e;
        if (invoker != null && invoker.isShowing()) {
            Point iv = invoker.getLocationOnScreen();
            if (me.getXOnScreen() >= iv.x && me.getXOnScreen() < iv.x + invoker.getWidth()
                    && me.getYOnScreen() >= iv.y && me.getYOnScreen() < iv.y + invoker.getHeight()) return;
        }
        Point pp = getLocationOnScreen();
        if (me.getXOnScreen() >= pp.x && me.getXOnScreen() < pp.x + getWidth()
                && me.getYOnScreen() >= pp.y && me.getYOnScreen() < pp.y + getHeight()) return;
        hidePopup();
        if (dismissListener != null) dismissListener.run();
    }

    public JPanel getContent() { return content; }

    public void setDismissListener(Runnable r) { dismissListener = r; }

    @Override
    public void setVisible(boolean v) {
        if (!v) hidePopup();
    }

    public void show(Component invoker, int x, int y) {
        this.invoker = invoker;
        hidePopup();
        Window w = SwingUtilities.getWindowAncestor(invoker);
        if (!(w instanceof RootPaneContainer)) return;
        JLayeredPane lp = ((RootPaneContainer) w).getLayeredPane();
        Point p = SwingUtilities.convertPoint(invoker, x, y, lp);
        Dimension size = getPreferredSize();
        setBounds(p.x, p.y, size.width, size.height);
        alpha = 0f;
        content.setBorder(new EmptyBorder(8, 0, 0, 0));
        lp.add(this, JLayeredPane.POPUP_LAYER, 0);
        lp.repaint(p.x, p.y, size.width, size.height);
        openAnim.go(0f, 1f);
    }

    private void hidePopup() {
        if (getParent() == null) return;
        Container parent = getParent();
        Rectangle r = getBounds();
        parent.remove(this);
        parent.repaint(r.x, r.y, r.width, r.height);
        openAnim.stop();
    }
}
