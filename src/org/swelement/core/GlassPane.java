package org.swelement.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GlassPane extends JPanel {
    private final Animator.Listener fadeListener = new Animator.Listener() {
        public void update(float v) {
            alpha = fadeDirectionOut ? Math.round((1f - v) * startAlpha) : Math.round(v * 80);
            repaint();
        }
    };
    private final Animator alphaAnim = new Animator(200, new org.swelement.core.Easing() {
        public float apply(float t) { return org.swelement.core.Easing.easeOut(t); }
    }, fadeListener);
    private int startAlpha = 0;
    private boolean fadeDirectionOut = false;
    private int alpha;

    public GlassPane() {
        setOpaque(false);
    }

    public void setActive(boolean active) {
        alphaAnim.stop();
        fadeDirectionOut = !active;
        if (active) {
            setVisible(true);
            requestFocusInWindow();
            startAlpha = 0;
            alpha = 0;
            alphaAnim.go(0f, 1f);
        } else {
            startAlpha = Math.max(1, alpha);
            alpha = startAlpha;
            alphaAnim.go(0f, 1f);
            Timer t = new Timer(220, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    ((Timer)e.getSource()).stop();
                    setVisible(false);
                }
            });
            t.setRepeats(false);
            t.start();
        }
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
        // Test setActive(false) fade-out: after anim timer, alpha should be small / invisible
        gp.setActive(false);
        // sleep 300ms to allow Timer(220ms) + anim(200ms) complete
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        img = new java.awt.image.BufferedImage(200, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        gg = img.createGraphics();
        try {
            gp.setSize(200, 200);
            gp.paintComponent(gg);
        } finally { gg.dispose(); }
        // After fade-out, either panel is not visible OR alpha value is small (<=20)
        int after = img.getRGB(10,10);
        int aa = (after >>> 24) & 0xFF;
        assert !gp.isVisible() || aa <= 20 : "after setActive(false) alpha should be small or not visible, was visible="+gp.isVisible()+" alpha="+aa;
        jf.dispose();
        System.out.println("GlassPane self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
