package org.swelement.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GlassPane extends JPanel {
    private final Animator alphaAnim = new Animator(200, new org.swelement.core.Easing() {
        public float apply(float t) { return org.swelement.core.Easing.easeOut(t); }
    }, new Animator.Listener() {
        public void update(float v) { alpha = Math.round(v * 80); repaint(); }
    });
    private int alpha;

    public GlassPane() {
        setOpaque(false);
    }

    public void setActive(boolean active) {
        setVisible(active);
        alphaAnim.stop();
        if (active) { alpha = 0; alphaAnim.go(0f, 1f); requestFocusInWindow(); }
        else { alpha = 80; alphaAnim.go(0f, 1f); }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, Math.max(0, Math.min(alpha, 255))));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    @Override protected void processMouseEvent(MouseEvent e) { e.consume(); super.processMouseEvent(e); }
    @Override protected void processMouseMotionEvent(MouseEvent e) { e.consume(); super.processMouseMotionEvent(e); }
    @Override protected void processKeyEvent(KeyEvent e) { e.consume(); super.processKeyEvent(e); }

    public static GlassPane install(RootPaneContainer rpc) {
        GlassPane gp = new GlassPane();
        rpc.getRootPane().setGlassPane(gp);
        return gp;
    }

    static void selfCheck() {
        JFrame jf = new JFrame();
        GlassPane gp = install(jf);
        gp.setActive(true);
        try { Thread.sleep(60); } catch (InterruptedException e) { }
        // paint off-screen
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(200, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try {
            gp.setSize(200, 200);
            gp.paintComponent(gg);
        } finally { gg.dispose(); }
        int topLeft = img.getRGB(10, 10);
        int a = (topLeft >>> 24) & 0xFF;
        assert a >= 40 : "active glass alpha should be positive after anim setup, was="+a;
        gp.setActive(false);
        jf.dispose();
        System.out.println("GlassPane self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
