package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.AnimatedPopup;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.core.PopupPositioner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * 全局消息 Toast（不拦截后台操作）。
 * 默认：顶部居中（TOP_CENTER），3s 自动关闭，淡出动画 + 下滑进入。
 *   AstMessage.show(frame, MessageType.SUCCESS, "保存成功");
 *   AstMessage.show(frame, MessageType.INFO, "自定义时长", 800);
 */
public class AstMessage {
    public enum MessageType { INFO, SUCCESS, WARNING, ERROR }

    // Track open toasts per owner → next Y position offset
    private static final IdentityHashMap<Container, List<AnimatedPopup>> openPerOwner =
            new IdentityHashMap<Container, List<AnimatedPopup>>();
    private static final int TOAST_OFFSET = 56; // vertical spacing per stacked toast
    private static final int DEFAULT_DURATION = 3000;

    public static void show(Window owner, MessageType type, String text) {
        show(owner, type, text, DEFAULT_DURATION);
    }

    public static void show(final Window owner, final MessageType type, final String text, int durationMs) {
        if (owner == null) throw new IllegalArgumentException("owner must not be null");
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (text == null) throw new IllegalArgumentException("text must not be null");
        if (durationMs <= 0) durationMs = DEFAULT_DURATION;
        if (durationMs < 500) durationMs = 500;
        if (!(owner instanceof RootPaneContainer)) throw new IllegalArgumentException("owner must be RootPaneContainer");
        if (!SwingUtilities.isEventDispatchThread()) {
            final int dur = durationMs;
            SwingUtilities.invokeLater(new Runnable() { public void run() { show(owner, type, text, dur); }});
            return;
        }
        final RootPaneContainer rpc = (RootPaneContainer) owner;
        final ToastCard card = new ToastCard(type, text);
        final AnimatedPopup popup = new AnimatedPopup();
        // Toast 靠计时器自行消失，不参与「外部点击关闭」，否则触发按钮的 MOUSE_PRESSED
        // 会立刻打掉刚弹出的 Toast（旧根因：多次点击只剩最后一个 Toast 且位置持续下移）。
        popup.setDismissOnOutsideClick(false);
        AnimatedPopup.registerGlobal(popup, AnimatedPopup.PopupLayer.TOOL);
        popup.getContent().setLayout(new BorderLayout());
        popup.getContent().add(card, BorderLayout.CENTER);
        popup.setPreferredSize(card.getPreferredSize());
        // Compute Y offset: existing open count × 56
        Container key = rpc.getLayeredPane();
        List<AnimatedPopup> open = openPerOwner.get(key);
        if (open == null) { open = new ArrayList<AnimatedPopup>(); openPerOwner.put(key, open); }
        int idx = open.size();
        open.add(popup);
        // Positioning: PopupPositioner TOP_CENTER; then add +TOAST_OFFSET × idx
        PopupPositioner pp = new PopupPositioner(card.getPreferredSize(),
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds());
        Rectangle invokerBounds = new Rectangle(owner.getLocationOnScreen(), owner.getSize());
        PopupPositioner.Result r = pp.calc(invokerBounds, AnimatedPopup.Direction.TOP_CENTER);
        Point screenPt = new Point(r.location.x, r.location.y + idx * TOAST_OFFSET);
        JLayeredPane lp = rpc.getLayeredPane();
        Point lpPt = new Point(screenPt);
        SwingUtilities.convertPointFromScreen(lpPt, lp);
        popup.setBounds(lpPt.x, lpPt.y, card.getPreferredSize().width, card.getPreferredSize().height);
        lp.add(popup, JLayeredPane.POPUP_LAYER);
        card.startIn();
        // Auto close timer
        Timer closeT = new Timer(durationMs, null);
        closeT.setRepeats(false);
        final List<AnimatedPopup> openList = open;
        final int finalDur = durationMs;
        final ActionListener doHide = new ActionListener() { public void actionPerformed(ActionEvent e) {
            closeT.stop();
            popup.hideWithAnimation(new Runnable() { public void run() {
                openList.remove(popup);
                // Reposition remaining toasts upward
                repositionUpward(lp, openList);
            }});
        }};
        closeT.addActionListener(doHide);
        closeT.start();
    }

    private static void repositionUpward(Container layeredPane, List<AnimatedPopup> open) {
        // For popup index i, its y should be (base TOP_CENTER y) + i*TOAST_OFFSET
        // Use first popup's X and the current Y base (compute the position as if it were index 0)
        if (open.isEmpty()) return;
        if (layeredPane == null || !layeredPane.isShowing()) return;
        try {
            Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            int x0, y0;
            Dimension size = open.get(0).getSize();
            int baseX = screen.x + screen.width / 2 - size.width / 2;
            int baseY = screen.y + 20;
            x0 = baseX; y0 = baseY;
            for (int i = 0; i < open.size(); i++) {
                AnimatedPopup p = open.get(i);
                Point lpP = new Point(x0, y0 + i * TOAST_OFFSET);
                SwingUtilities.convertPointFromScreen(lpP, layeredPane);
                p.setLocation(lpP.x, lpP.y);
            }
        } catch (Throwable ignore) { }
    }

    // ---------- Internal ToastCard paint component ----------
    static final class ToastCard extends JPanel {
        final MessageType type;
        final String text;
        final Animator inAnim; // 0 → 1 : slide-down + fade
        float progress;
        ToastCard(MessageType t, String s) {
            type = t; text = s;
            setOpaque(false);
            inAnim = new Animator(220, new Easing() { public float apply(float f) { return Easing.easeOut(f); }},
                new Animator.Listener() { public void update(float v) { progress = v; repaint(); }});
        }
        void startIn() { inAnim.stop(); inAnim.go(0f, 1f); }

        @Override public Dimension getPreferredSize() {
            Font f = ElementTheme.FONT.deriveFont(14f);
            FontMetrics fm = getFontMetrics(f);
            int textW = fm.stringWidth(text);
            int w = 32 + 12 + 8 + textW + 32; // icon 32x32 + 12 icon pad + 8 gap + textW + 32 right pad
            w = Math.max(280, Math.min(640, w));
            return new Dimension(w, 48);
        }
        @Override public Dimension getMinimumSize() { return new Dimension(280, 48); }

        @Override public boolean isOptimizedDrawingEnabled() { return false; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            // Toast card: white bg with 8px corner; light 1px BORDER_BASE; left accent stripe of type color 4px wide
            int a = Math.min(255, Math.max(0, Math.round(255 * progress)));
            Color bg = new Color(0xFF, 0xFF, 0xFF, a);
            Color border = new Color(ElementTheme.BORDER_BASE.getRed(), ElementTheme.BORDER_BASE.getGreen(), ElementTheme.BORDER_BASE.getBlue(), a);
            ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstMessage toast text");
            int r = ElementTheme.RADIUS;
            RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1.5f, getHeight()-1.5f, r, r);
            g2.setColor(bg); g2.fill(rect);
            g2.setColor(border); g2.setStroke(new BasicStroke(1f)); g2.draw(rect);
            // Left accent stripe 4px: type color, alpha = a
            Color accent = colorFor(type);
            Color accentA = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), a);
            g2.setColor(accentA);
            g2.fillRect(0, 0, 4, getHeight());
            // Badge icon 24x24, left=16, vertical center
            int iconX = 20, iconY = (getHeight() - 24) / 2;
            Color iconBg = accentA;
            Color fg = ElementTheme.pickTextColorForBg(accent);
            ElementTheme.assertContrast(fg, accent, "AstMessage toast badge "+type);
            Ellipse2D circ = new Ellipse2D.Float(iconX, iconY, 24, 24);
            g2.setColor(iconBg); g2.fill(circ);
            String glyph = glyphFor(type);
            Font f = ElementTheme.FONT.deriveFont(Font.BOLD, 14f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics(f);
            int gx = iconX + 12 - fm.stringWidth(glyph)/2;
            int gy = iconY + (24 - fm.getHeight())/2 + fm.getAscent();
            g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), a));
            g2.drawString(glyph, gx, gy);
            // Text right of icon, at x = 20 + 24 + 8 = 52
            int tx = 52;
            Color tc = new Color(ElementTheme.TEXT_MAIN.getRed(), ElementTheme.TEXT_MAIN.getGreen(), ElementTheme.TEXT_MAIN.getBlue(), a);
            g2.setColor(tc);
            Font tf = ElementTheme.FONT.deriveFont(14f);
            g2.setFont(tf);
            FontMetrics tm = g2.getFontMetrics(tf);
            int ty = (getHeight() - tm.getHeight())/2 + tm.getAscent();
            String shown = text;
            int maxW = Math.max(20, getWidth() - tx - 20);
            if (tm.stringWidth(shown) > maxW) {
                String ell = "\u2026"; int ellW = tm.stringWidth(ell);
                while (shown.length() > 0 && tm.stringWidth(shown) + ellW > maxW) shown = shown.substring(0, shown.length()-1);
                shown = shown + ell;
            }
            g2.drawString(shown, tx, ty);
            g2.dispose();
        }

        static Color colorFor(MessageType t) {
            switch (t) {
                case INFO: return ElementTheme.PRIMARY;
                case SUCCESS: return ElementTheme.SUCCESS;
                case WARNING: return ElementTheme.WARNING;
                case ERROR: return ElementTheme.DANGER;
            }
            return ElementTheme.PRIMARY;
        }
        static String glyphFor(MessageType t) {
            switch (t) {
                case INFO: return "i";
                case SUCCESS: return "√";
                case WARNING: return "!";
                case ERROR: return "×";
            }
            return "i";
        }
    }

    static void selfCheck() {
        // Null guards
        boolean threw = false;
        try { AstMessage.show(null, MessageType.INFO, "x"); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw; threw = false;
        try { AstMessage.show(new JFrame(), null, "x"); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw; threw = false;
        try { AstMessage.show(new JFrame(), MessageType.INFO, null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw; threw = false;
        try { AstMessage.show(new Window((Frame)null) { }, MessageType.INFO, "x"); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "non-RPC";

        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("Toast SC"); jf.setSize(800, 600); jf.setVisible(true);
            AstMessage.show(jf, MessageType.SUCCESS, "保存成功");
            try { Thread.sleep(320); } catch (InterruptedException ignore) {}
            JLayeredPane lp = jf.getLayeredPane();
            AnimatedPopup popup = null;
            for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { popup = (AnimatedPopup) lp.getComponent(i); break; }
            assert popup != null : "toast popup added";
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(480, 48, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            popup.setBounds(0, 0, 480, 48);
            // Force toast card to fully visible state (bypass animation timing)
            Container content = popup.getContent();
            for (int ci = 0; ci < content.getComponentCount(); ci++) {
                Component cc = content.getComponent(ci);
                if (cc instanceof ToastCard) { ((ToastCard) cc).progress = 1f; }
            }
            Graphics2D gg = img.createGraphics();
            try { popup.paint(gg); } finally { gg.dispose(); }
            int px = img.getRGB(40, 24); int a = (px >>> 24) & 0xFF;
            assert a >= 120 : "toast bg opaque enough; alpha="+a;
            // Show 2nd toast — verify count ≥ 2 and y-offset diff ≥ 40
            AstMessage.show(jf, MessageType.INFO, "第二条信息", 800);
            try { Thread.sleep(320); } catch (InterruptedException ignore) {}
            int count = 0; int[] ys = new int[2]; int yi = 0;
            for (int i = 0; i < lp.getComponentCount(); i++) {
                Component c = lp.getComponent(i);
                if (c instanceof AnimatedPopup && yi < 2) { ys[yi++] = c.getY(); count++; }
            }
            assert count >= 2 : "at least 2 toasts after 2nd show";
            int diff = Math.abs(ys[0] - ys[1]);
            assert diff >= 40 : "toasts y-offset by ≥40px; diff="+diff;
            jf.dispose();
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstMessage self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
