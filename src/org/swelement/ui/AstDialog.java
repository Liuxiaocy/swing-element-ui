package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.core.GlassPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 模态对话框 — 基于 GlassPane 全屏遮罩 + 居中圆角卡片。
 * 用法：
 *   AstDialog.show(frame, "保存确认", bodyContentPanel, new AstDialog.ResultCallback() {
 *       public void onResult(int resultCode) {
 *           if (resultCode == AstDialog.RESULT_OK) save();
 *           else cancel();
 *       }
 *   });
 */
public class AstDialog {
    public static final int RESULT_OK = 1;
    public static final int RESULT_CANCEL = 2;
    private static final String GP_KEY = AstDialog.class.getName() + ".gp";
    private static final String CARD_KEY = AstDialog.class.getName() + ".card";
    public interface ResultCallback { void onResult(int resultCode); }

    public static void show(Window owner, String title, JComponent body, final ResultCallback cb) {
        show(owner, title, "确定", "取消", body, cb);
    }

    public static void show(final Window owner, final String title, final String okText, final String cancelText,
                            final JComponent body, final ResultCallback cb) {
        if (owner == null) throw new IllegalArgumentException("owner must not be null");
        if (title == null) throw new IllegalArgumentException("title must not be null");
        if (okText == null) throw new IllegalArgumentException("okText must not be null");
        if (cancelText == null) throw new IllegalArgumentException("cancelText must not be null");
        if (!(owner instanceof RootPaneContainer)) throw new IllegalArgumentException("owner must be a RootPaneContainer (JFrame/JDialog/...)");
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                show(owner, title, okText, cancelText, body, cb);
            }});
            return;
        }
        final RootPaneContainer rpc = (RootPaneContainer) owner;
        // Get or install GlassPane
        GlassPane tempGp = (GlassPane) rpc.getRootPane().getClientProperty(GP_KEY);
        if (tempGp == null || rpc.getGlassPane() != tempGp) {
            tempGp = GlassPane.install(rpc);
            rpc.getRootPane().putClientProperty(GP_KEY, tempGp);
        }
        final GlassPane gp = tempGp;
        final JPanel[] cardHolder = new JPanel[1];
        final JPanel card = makeCard(title, okText, cancelText, body, cb, new Runnable() { public void run() {
            // Called once card fade exits; remove from glass pane + deactivate modal glass
            gp.setActive(false);
            gp.remove(cardHolder[0]);
            rpc.getRootPane().putClientProperty(CARD_KEY, null);
            gp.repaint();
        }});
        cardHolder[0] = card;
        rpc.getRootPane().putClientProperty(CARD_KEY, card);
        gp.removeAll(); // just to be safe — only one card at a time; removes any lingering previous
        gp.setLayout(null); // absolute positioning: card centered by onShow animator
        gp.add(card);
        // Activate glass pane AFTER adding card so that the first paint sees children
        gp.setActive(true);
        // Position card centered inside glass pane
        Dimension rootSize = gp.getSize();
        if (rootSize.width <= 0 || rootSize.height <= 0) { // fallback: owner's size
            rootSize = owner.getSize();
        }
        card.setSize(card.getPreferredSize());
        int cx = Math.max(0, (rootSize.width - card.getWidth()) / 2);
        int cy = Math.max(0, (rootSize.height - card.getHeight()) / 2);
        card.setLocation(cx, cy);
        // Trigger fade-in of card by starting its cardAnimator via public startFadeIn method
        DialogCardPanel dcp = (DialogCardPanel) card;
        dcp.startFadeIn();
    }

    /** Package/public helper used by AstMessageBox. */
    public static JPanel makeCard(String title, String okText, String cancelText, JComponent body, final ResultCallback cb, final Runnable onClosed) {
        return new DialogCardPanel(title, okText, cancelText, body, cb, onClosed);
    }

    // --- Inner classes -----------------------------------------------------

    public static final class DialogCardPanel extends JPanel {
        private final Animator fade; // 0 hidden → 1 fully shown
        float cardAlpha;
        private final String title;
        final JComponent body;
        final Runnable onClosed;
        final ResultCallback resultCallback;
        private final String okText, cancelText;

        DialogCardPanel(final String title, final String okText, final String cancelText, JComponent body, final ResultCallback cb, final Runnable onClosed) {
            this.title = title;
            this.okText = okText;
            this.cancelText = cancelText;
            this.body = body == null ? new JPanel() : body;
            this.resultCallback = cb;
            this.onClosed = onClosed;
            this.setOpaque(false);
            this.cardAlpha = 0f;
            this.fade = new Animator(220, new Easing() { public float apply(float t) { return Easing.easeOut(t); }},
                new Animator.Listener() { public void update(float v) { cardAlpha = v; repaint(); }});
            buildLayout();
        }

        void startFadeIn() {
            fade.stop();
            fade.go(cardAlpha, 1f);
        }

        void startFadeOut(final Runnable after) {
            fade.stop();
            fade.go(cardAlpha, 0f, new Runnable() { public void run() {
                if (after != null) after.run();
            }});
        }

        private void buildLayout() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(0, 0, 0, 0));
            // Title: NORTH, height 48, bold 16, separator 1px at bottom, left padding 24, right padding 24
            final JPanel titleBar = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // Paint the 48px top section of the card background (rounded corners handled by outer card)
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(ElementTheme.BORDER_BASE);
                    g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                    g2.setColor(ElementTheme.TEXT_MAIN);
                    ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstDialog title");
                    g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 16f));
                    FontMetrics fm = g2.getFontMetrics();
                    int baseY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(title, 24, baseY);
                    g2.dispose();
                }
                @Override public Dimension getPreferredSize() { return new Dimension(480, 48); }
                @Override public Dimension getMinimumSize() { return new Dimension(200, 48); }
                @Override public boolean isOptimizedDrawingEnabled() { return false; }
            };
            titleBar.setOpaque(false);
            add(titleBar, BorderLayout.NORTH);

            // Body: CENTER, padding 24 top/bottom, 24 left/right
            final JPanel bodyWrap = new JPanel(new BorderLayout());
            bodyWrap.setBorder(new EmptyBorder(24, 24, 24, 24));
            bodyWrap.setOpaque(false);
            bodyWrap.add(this.body, BorderLayout.CENTER);
            add(bodyWrap, BorderLayout.CENTER);

            // Footer: SOUTH, height 64, right-aligned buttons, separator line at top, 24px right pad
            final JPanel footer = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(ElementTheme.BORDER_BASE);
                    g2.drawLine(0, 0, getWidth(), 0);
                    g2.dispose();
                }
                @Override public boolean isOptimizedDrawingEnabled() { return false; }
                @Override public Dimension getPreferredSize() { return new Dimension(480, 64); }
                @Override public Dimension getMinimumSize() { return new Dimension(200, 64); }
            };
            footer.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 16));
            footer.setOpaque(false);
            Button cancelBtn = new Button(cancelText, Button.DEFAULT, false);
            Button okBtn = new Button(okText, Button.PRIMARY, false);
            cancelBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { finish(RESULT_CANCEL); }});
            okBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { finish(RESULT_OK); }});
            footer.add(cancelBtn); footer.add(okBtn);
            add(footer, BorderLayout.SOUTH);
        }

        private void finish(int resultCode) {
            if (resultCallback != null) {
                try { resultCallback.onResult(resultCode); } catch (Throwable ignore) { /* swallow — never leak up to dialog internals */ }
            }
            startFadeOut(new Runnable() { public void run() {
                if (onClosed != null) {
                    try { onClosed.run(); } catch (Throwable ignore) {}
                }
            }});
        }

        @Override public Dimension getPreferredSize() {
            Dimension bp = body.getPreferredSize();
            int w = Math.max(320, Math.max(480, bp.width + 48 + 40));
            int minBody = 100;
            int h = 48 + 64 + 24 + 24 + Math.max(minBody, bp.height);
            w = Math.min(w, 720);
            h = Math.min(h, 640);
            return new Dimension(w, h);
        }
        @Override public Dimension getMinimumSize() { return new Dimension(320, 260); }

        @Override public boolean isOptimizedDrawingEnabled() { return false; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int a = Math.min(255, Math.max(0, Math.round(255f * cardAlpha)));
            Color cardBg = new Color(0xFF, 0xFF, 0xFF, a);
            Color borderC = new Color(ElementTheme.BORDER_BASE.getRed(), ElementTheme.BORDER_BASE.getGreen(), ElementTheme.BORDER_BASE.getBlue(), a);
            ElementTheme.assertContrast(ElementTheme.TEXT_REGULAR, Color.WHITE, "AstDialog body on card bg");
            int r = ElementTheme.RADIUS * 2;
            RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1.5f, getHeight()-1.5f, r, r);
            g2.setColor(cardBg);
            g2.fill(rect);
            g2.setColor(borderC);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(rect);
            g2.dispose();
        }
    }

    // --- Helpers used by self-check and MessageBox sub-components ---
    private static Component findChildByText(Container c, String text) {
        if (c == null || text == null) return null;
        Queue<Container> q = new LinkedList<Container>(); q.add(c);
        while (!q.isEmpty()) {
            Container cur = q.poll();
            for (int i = 0; i < cur.getComponentCount(); i++) {
                Component ch = cur.getComponent(i);
                if (ch instanceof JLabel) {
                    if (text.equals(((JLabel) ch).getText())) return ch;
                } else if (ch instanceof Button) {
                    if (text.equals(((Button) ch).getText())) return ch;
                } else if (ch instanceof AbstractButton) {
                    if (text.equals(((AbstractButton) ch).getText())) return ch;
                }
                if (ch instanceof Container) q.add((Container) ch);
            }
        }
        return null;
    }

    private static Component firstPanelChild(Container c) {
        if (c == null) return null;
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component ch = c.getComponent(i);
            if (ch instanceof JPanel && ((JPanel) ch).getComponentCount() > 0) return ch;
        }
        return null;
    }

    private static void clickComponent(Component c) {
        if (c == null) return;
        if (c instanceof AbstractButton) {
            ((AbstractButton) c).doClick(50);
            try { Thread.sleep(70); } catch (Throwable ignore) {}
            return;
        }
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, Math.min(10, c.getWidth()/2), Math.min(10, c.getHeight()/2), 1, false));
        try { Thread.sleep(15); } catch (Throwable ignore) {}
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, Math.min(10, c.getWidth()/2), Math.min(10, c.getHeight()/2), 1, false));
        try { Thread.sleep(15); } catch (Throwable ignore) {}
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, Math.min(10, c.getWidth()/2), Math.min(10, c.getHeight()/2), 1, false));
        try { Thread.sleep(15); } catch (Throwable ignore) {}
    }

    static void selfCheck() {
        // Constructor argument validation: null owner → IAE, null title → IAE, owner not RPC → IAE
        boolean threw = false;
        try { AstDialog.show(null, "T", new JPanel(), null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null owner"; threw = false;
        try { AstDialog.show(new JFrame(), null, new JPanel(), null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null title"; threw = false;
        try { AstDialog.show(new Window((Frame)null) { }, "T", new JPanel(), null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "non-RPC owner"; threw = false;

        final Throwable[] err = {null};
        final int[] res = {0};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("Dialog self-check"); jf.setSize(800, 600); jf.setVisible(true);
            JPanel body = new JPanel(new BorderLayout());
            JLabel info = new JLabel("<html>内容区<br>多行文字信息<br>第三行</html>", JLabel.CENTER);
            info.setFont(info.getFont().deriveFont(13f)); info.setForeground(ElementTheme.TEXT_REGULAR);
            body.add(info, BorderLayout.CENTER);
            AstDialog.show(jf, "对话框标题", "保存", "取消", body, new AstDialog.ResultCallback() {
                public void onResult(int resultCode) { res[0] = resultCode; }
            });
            try { Thread.sleep(260); } catch (InterruptedException ignore) {}
            Component gp = jf.getGlassPane();
            assert gp != null && gp.isVisible() : "glass pane visible";
            Container c = (Container) gp;
            Component card = firstPanelChild(c);
            assert card != null : "card child found";
            Component ok = findChildByText((Container) card, "保存");
            Component cancel = findChildByText((Container) card, "取消");
            assert ok != null : "保存 button present"; assert cancel != null : "取消 button present";
            clickComponent(cancel);
            try { Thread.sleep(300); } catch (InterruptedException ignore) {}
            assert res[0] == RESULT_CANCEL : "canceled → result should be RESULT_CANCEL="+RESULT_CANCEL+" actual="+res[0];

            // Second call: default overload — default OK/Cancel buttons with "确定"/"取消" label
            AstDialog.show(jf, "T2", body, new ResultCallback() { public void onResult(int resultCode) { res[0] = resultCode; }});
            try { Thread.sleep(260); } catch (InterruptedException ignore) {}
            gp = jf.getGlassPane(); c = (Container) gp;
            card = firstPanelChild(c);
            Component ok2 = findChildByText((Container) card, "确定");
            assert ok2 != null : "确定 button present";
            clickComponent(ok2);
            try { Thread.sleep(300); } catch (InterruptedException ignore) {}
            assert res[0] == RESULT_OK : "result OK expected=" + RESULT_OK + " actual="+res[0];
            jf.dispose();
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        // makeCard public: test offscreen paint
        JPanel card = AstDialog.makeCard("X", "A", "B", new JLabel("body"), null, null);
        card.setSize(480, 240);
        ((DialogCardPanel)card).cardAlpha = 1f; // skip fade animation directly to 1
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(480, 240, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { card.paint(gg); } finally { gg.dispose(); }
        int px = img.getRGB(40, 120);
        int a = (px >>> 24) & 0xFF;
        assert a > 120 : "card bg painted opaque alpha="+a;
        System.out.println("AstDialog self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
