package org.swelement.ui;

import org.swelement.core.AnimatedPopup;
import org.swelement.core.ElementTheme;
import org.swelement.core.PopupPositioner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * Tooltip 提示。通过 attach 静态方法为 JComponent 绑定文本和方向/主题。
 * 共享全局单例 AnimatedPopup（避免每次 hover 创建新 HeavyWeight Window）。
 * 用法：
 *   AstTooltip.attach(btn, "点此保存");
 *   AstTooltip.attach(btn, "编辑内容", AnimatedPopup.Direction.RIGHT, AstTooltip.Effect.LIGHT);
 *   AstTooltip.detach(btn);
 */
public class AstTooltip {
    public enum Effect { DARK, LIGHT }

    private static final HashMap<JComponent, Attached> attached = new HashMap<JComponent, Attached>();
    private static final IdentityHashMap<JComponent, MouseAdapter> mouseAdapters = new IdentityHashMap<JComponent, MouseAdapter>();
    private static final IdentityHashMap<JComponent, Timer> pendingTimers = new IdentityHashMap<JComponent, Timer>();
    private static final AnimatedPopup sharedPopup;
    private static final JPanel balloon;
    private static String currentText;
    private static Effect currentEffect;
    private static JComponent currentInvoker;

    static {
        sharedPopup = new AnimatedPopup();
        balloon = new JPanel() {
            @Override public boolean isOptimizedDrawingEnabled() { return false; }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                Effect eff = currentEffect;
                Color bg; Color fg; Color borderC;
                if (eff == Effect.DARK) {
                    bg = new Color(0x30, 0x31, 0x33, 0xFF);
                    fg = Color.WHITE;
                    borderC = null;
                } else {
                    bg = Color.WHITE;
                    fg = ElementTheme.TEXT_MAIN;
                    borderC = ElementTheme.BORDER_BASE;
                }
                ElementTheme.assertContrast(fg, bg, "AstTooltip balloon text");
                int r = ElementTheme.RADIUS;
                RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1.5f, getHeight()-1.5f, r, r);
                g2.setColor(bg); g2.fill(rect);
                if (borderC != null) {
                    g2.setColor(borderC);
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(rect);
                }
                // Centered single-line text at x=12, y=height/2 baseline
                g2.setColor(fg);
                g2.setFont(ElementTheme.FONT.deriveFont(14f));
                FontMetrics fm = g2.getFontMetrics();
                int baseY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                String t = currentText == null ? "" : currentText;
                if (fm.stringWidth(t) > getWidth() - 24) {
                    // ellipsize
                    String ellipsis = "\u2026";
                    int ellW = fm.stringWidth(ellipsis);
                    while (t.length() > 0 && fm.stringWidth(t) + ellW > getWidth() - 24) {
                        t = t.substring(0, t.length() - 1);
                    }
                    t = t + ellipsis;
                }
                g2.drawString(t, 12, baseY);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() {
                Font f = ElementTheme.FONT.deriveFont(14f);
                FontMetrics fm = getFontMetrics(f);
                int w = 24 + fm.stringWidth(currentText == null ? "" : currentText);
                int h = fm.getHeight() + 12;
                w = Math.min(360, Math.max(48, w));
                return new Dimension(w, h);
            }
            @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        };
        balloon.setOpaque(false);
        sharedPopup.getContent().setLayout(new BorderLayout());
        sharedPopup.getContent().add(balloon, BorderLayout.CENTER);
        AnimatedPopup.registerGlobal(sharedPopup, AnimatedPopup.PopupLayer.TOOL);
    }

    private static final class Attached {
        final String text;
        final AnimatedPopup.Direction dir;
        final Effect effect;
        Attached(String t, AnimatedPopup.Direction d, Effect e) { text=t; dir=d; effect=e; }
    }

    private AstTooltip() {}

    public static void attach(JComponent target, String text) {
        attach(target, text, AnimatedPopup.Direction.BELOW, Effect.DARK);
    }
    public static void attach(JComponent target, String text, AnimatedPopup.Direction dir) {
        attach(target, text, dir, Effect.DARK);
    }
    public static void attach(JComponent target, String text, AnimatedPopup.Direction dir, Effect effect) {
        if (target == null) throw new IllegalArgumentException("target JComponent must not be null");
        if (text == null) throw new IllegalArgumentException("tooltip text must not be null");
        if (dir == null) throw new IllegalArgumentException("direction must not be null");
        if (effect == null) throw new IllegalArgumentException("effect must not be null");
        // Detach any previous for this target first
        detach(target);
        attached.put(target, new Attached(text, dir, effect));
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                scheduleShow(target);
            }
            @Override public void mouseExited(MouseEvent e) {
                cancelPendingAndHide(target);
            }
            @Override public void mousePressed(MouseEvent e) {
                cancelPendingAndHide(target);
            }
        };
        target.addMouseListener(ma);
        mouseAdapters.put(target, ma);
    }

    public static void detach(JComponent target) {
        if (target == null) throw new IllegalArgumentException("target JComponent must not be null");
        MouseAdapter ma = mouseAdapters.remove(target);
        if (ma != null) target.removeMouseListener(ma);
        Timer t = pendingTimers.remove(target);
        if (t != null) t.stop();
        attached.remove(target);
        if (currentInvoker == target) {
            sharedPopup.hideWithAnimation(null);
            currentInvoker = null;
        }
    }

    private static void scheduleShow(final JComponent target) {
        cancelPendingTimer(target);
        Timer tm = new Timer(200, null);
        tm.setRepeats(false);
        final Attached at = attached.get(target);
        if (at == null) return;
        tm.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            pendingTimers.remove(target);
            showPopupFor(target, at);
        }});
        pendingTimers.put(target, tm);
        tm.start();
    }

    private static void cancelPendingTimer(JComponent target) {
        Timer old = pendingTimers.remove(target);
        if (old != null) old.stop();
    }

    private static void cancelPendingAndHide(JComponent target) {
        cancelPendingTimer(target);
        if (currentInvoker == target) {
            sharedPopup.hideWithAnimation(null);
            currentInvoker = null;
        }
    }

    private static void showPopupFor(JComponent target, Attached at) {
        if (!target.isShowing()) return;
        currentInvoker = target;
        currentText = at.text;
        currentEffect = at.effect;
        balloon.setSize(balloon.getPreferredSize());
        sharedPopup.setPreferredSize(balloon.getPreferredSize());
        sharedPopup.show(target, at.dir);
    }

    private static Component findChildByName(Container c, String namePart, int idx) {
        if (c == null) return null;
        java.util.ArrayList<Component> out = new java.util.ArrayList<Component>();
        java.util.Queue<Container> q = new java.util.LinkedList<Container>(); q.add(c);
        while (!q.isEmpty()) {
            Container cur = q.poll();
            for (int i = 0; i < cur.getComponentCount(); i++) {
                Component ch = cur.getComponent(i);
                if (ch.getClass().getName().contains(namePart)) out.add(ch);
                if (ch instanceof Container) q.add((Container) ch);
            }
        }
        return idx < out.size() ? out.get(idx) : null;
    }

    static void selfCheck() {
        boolean threw = false;
        try { AstTooltip.attach(null, "x"); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null target must throw IAE"; threw = false;
        try { AstTooltip.attach(new JLabel(), null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null text must throw IAE"; threw = false;
        try { AstTooltip.attach(new JLabel(), "x", null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null dir must throw IAE"; threw = false;
        try { AstTooltip.attach(new JLabel(), "x", AnimatedPopup.Direction.ABOVE, null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null effect must throw IAE";
        try { AstTooltip.detach(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw;

        final Throwable[] err = {null};
        final JFrame[] jfHolder = {null};
        final JButton[] btnHolder = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                jfHolder[0] = new JFrame("Tooltip self-check");
                jfHolder[0].setSize(800, 600);
                jfHolder[0].setVisible(true);
                JButton b = new JButton("HOVER ME");
                b.setBounds(200, 200, 120, 36);
                jfHolder[0].getContentPane().setLayout(null);
                jfHolder[0].getContentPane().add(b);
                btnHolder[0] = b;
                AstTooltip.attach(b, "Hi tooltip!", AnimatedPopup.Direction.BELOW, AstTooltip.Effect.DARK);
                b.dispatchEvent(new MouseEvent(b, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 2, 2, 0, false));
            }});
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);

        try { Thread.sleep(260); } catch (InterruptedException ignore) {}

        final boolean[] foundHolder = {false};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                JLayeredPane lp = jfHolder[0].getLayeredPane();
                for (int i = 0; i < lp.getComponentCount(); i++) {
                    Component c = lp.getComponent(i);
                    if (c instanceof AnimatedPopup) { foundHolder[0] = true; break; }
                }
                assert foundHolder[0] : "tooltip popup not added after 200ms delay";
                JButton b = btnHolder[0];
                b.dispatchEvent(new MouseEvent(b, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 2, 2, 0, false));
                AstTooltip.detach(b);
                currentText = "DARK effect 对比度测试"; currentEffect = Effect.DARK;
                balloon.setSize(balloon.getPreferredSize());
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(Math.max(1, balloon.getWidth()), Math.max(1, balloon.getHeight()), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                try { balloon.paint(gg); } finally { gg.dispose(); }
                int darkPx = img.getRGB(10, Math.max(1, balloon.getHeight()/2));
                int darkAlpha = (darkPx >>> 24) & 0xFF;
                assert darkAlpha > 120 : "dark balloon painted; alpha="+darkAlpha;
                currentText = "LIGHT effect 对比度测试"; currentEffect = Effect.LIGHT;
                balloon.setSize(balloon.getPreferredSize());
                img = new java.awt.image.BufferedImage(Math.max(1, balloon.getWidth()), Math.max(1, balloon.getHeight()), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                gg = img.createGraphics();
                try { balloon.paint(gg); } finally { gg.dispose(); }
                int lightPx = img.getRGB(10, Math.max(1, balloon.getHeight()/2));
                int lightAlpha = (lightPx >>> 24) & 0xFF;
                assert lightAlpha > 120 : "light balloon painted; alpha="+lightAlpha;
                jfHolder[0].dispose();
            }});
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstTooltip self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
