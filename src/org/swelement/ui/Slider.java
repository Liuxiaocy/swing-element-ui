package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class Slider extends JComponent {
    private final Animator thumbAnim = new Animator(200, Easing::easeOut, v -> { thumbX = v; repaint(); });
    private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
    private float thumbX = -1f, hover;   // thumbX 为像素位置，-1 表示未初始化
    private int min, max, value;
    private boolean dragging;

    public Slider(int min, int max, int value) {
        this.min = min; this.max = max; this.value = value;
        setOpaque(false);
        setPreferredSize(new Dimension(240, 32));
        MouseAdapter m = new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { dragging = true; setValueFrom(e.getX()); }
            public void mouseDragged(MouseEvent e)  { setValueFrom(e.getX()); }
            public void mouseReleased(MouseEvent e) { dragging = false; }
            public void mouseEntered(MouseEvent e)  { hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)   { hoverAnim.go(hover, 0f); }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    private void setValueFrom(int x) {
        int left = 6, right = getWidth() - 16;
        float t = (x - left) / (float) (right - left);
        setValue(min + Math.round(t * (max - min)));
    }

    public int getValue() { return value; }

    public void setValue(int v) {
        int nv = Math.max(min, Math.min(max, v));
        if (nv != value) {
            value = nv;
            fire();
        }
        repaint();
    }

    public void addChangeListener(ChangeListener l) { listenerList.add(ChangeListener.class, l); }
    public void removeChangeListener(ChangeListener l) { listenerList.remove(ChangeListener.class, l); }

    private void fire() {
        for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) l.stateChanged(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cy = getHeight() / 2;
        int trackY = cy - 3, trackH = 6;
        int left = 6, right = getWidth() - 16;
        float t = (max == min) ? 0f : (value - min) / (float) (max - min);
        int thumbTarget = left + Math.round(t * (right - left));
        if (thumbX < 0) thumbX = thumbTarget;
        if (dragging) thumbX = thumbTarget; else thumbAnim.go(thumbX, thumbTarget);
        int cx = Math.round(thumbX);

        Color trackColor = isEnabled() ? ElementTheme.PRIMARY : new Color(0xC0C4CC);
        g2.setColor(new Color(0xE4E7ED));
        g2.fill(new RoundRectangle2D.Float(left, trackY, right - left, trackH, trackH, trackH));
        int fillW = Math.max(0, Math.min(right - left, cx - left));
        g2.setColor(trackColor);
        g2.fill(new RoundRectangle2D.Float(left, trackY, fillW, trackH, trackH, trackH));

        float r = 6f * (1f + 0.25f * hover);
        g2.setColor(Color.WHITE);
        g2.fillOval(Math.round(cx - r), Math.round(cy - r), Math.round(2 * r), Math.round(2 * r));
        g2.setColor(trackColor);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(Math.round(cx - r), Math.round(cy - r), Math.round(2 * r), Math.round(2 * r));
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(240, 32); }
}
