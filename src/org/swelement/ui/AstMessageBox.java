package org.swelement.ui;

import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 消息盒子（简化版对话框）—— 5 种类型图标，alert/confirm 两种风格：
 *   AstMessageBox.alert(frame, MessageBoxType.ERROR, "出错了");       // 单个"确定"按钮
 *   AstMessageBox.confirm(frame, MessageBoxType.QUESTION, "确定删除？", new ConfirmCallback(){ ... });
 */
public class AstMessageBox {
    public enum MessageBoxType { INFO, SUCCESS, WARNING, ERROR, QUESTION }

    public interface ConfirmCallback {
        void onConfirm();
        default void onCancel() { }
    }

    public static void alert(Window owner, String message) { alert(owner, MessageBoxType.INFO, message); }

    public static void alert(final Window owner, final MessageBoxType type, final String message) {
        if (owner == null) throw new IllegalArgumentException("owner must not be null");
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (message == null) throw new IllegalArgumentException("message must not be null");
        final JComponent body = buildBody(type, message);
        AstDialog.show(owner, titleFor(type), "确定", "", body, new AstDialog.ResultCallback() {
            public void onResult(int resultCode) { /* alert ignores result */ }
        });
    }

    public static void confirm(Window owner, String message, ConfirmCallback cb) {
        confirm(owner, MessageBoxType.QUESTION, message, cb);
    }

    public static void confirm(final Window owner, final MessageBoxType type, final String message, final ConfirmCallback cb) {
        if (owner == null) throw new IllegalArgumentException("owner must not be null");
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (message == null) throw new IllegalArgumentException("message must not be null");
        if (cb == null) throw new IllegalArgumentException("callback must not be null");
        final JComponent body = buildBody(type, message);
        AstDialog.show(owner, titleFor(type), "确定", "取消", body, new AstDialog.ResultCallback() {
            public void onResult(int resultCode) {
                if (resultCode == AstDialog.RESULT_OK) cb.onConfirm();
                else cb.onCancel();
            }
        });
    }

    private static String titleFor(MessageBoxType t) {
        switch (t) {
            case INFO: return "信息";
            case SUCCESS: return "成功";
            case WARNING: return "警告";
            case ERROR: return "错误";
            case QUESTION: return "确认";
        }
        return "消息";
    }

    /** Package-public helper used by self-check to test each icon paint. */
    public static JPanel makeIconPanel(MessageBoxType t) { return new IconPanel(t); }

    private static JComponent buildBody(MessageBoxType type, String message) {
        JPanel wrap = new JPanel(new BorderLayout(24, 8));
        wrap.setOpaque(false);
        JPanel icon = makeIconPanel(type);
        icon.setPreferredSize(new Dimension(64, 80));
        icon.setMinimumSize(new Dimension(64, 80));
        wrap.add(icon, BorderLayout.WEST);
        JLabel msg = new JLabel("<html><div style='width:360px;'>" + escapeHtml(message) + "</div></html>", JLabel.LEFT);
        msg.setFont(ElementTheme.FONT.deriveFont(14f));
        msg.setForeground(ElementTheme.TEXT_REGULAR);
        wrap.add(msg, BorderLayout.CENTER);
        return wrap;
    }

    private static String escapeHtml(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    // --- Icon panel: circle 48x48 centered, glyph drawn centered inside ---
    static final class IconPanel extends JPanel {
        final MessageBoxType type;
        IconPanel(MessageBoxType t) { type = t; setOpaque(false); }

        @Override public Dimension getPreferredSize() { return new Dimension(64, 80); }
        @Override public Dimension getMinimumSize() { return new Dimension(64, 80); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            Color bg = colorFor(type);
            Color fg = ElementTheme.pickTextColorForBg(bg);
            ElementTheme.assertContrast(fg, bg, "AstMessageBox icon "+type);
            int cx = getWidth() / 2;
            int topY = (getHeight() - 48) / 2;
            Ellipse2D circ = new Ellipse2D.Float(cx - 24, topY, 48, 48);
            g2.setColor(bg); g2.fill(circ);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(circ);
            String glyph = glyphFor(type);
            Font f = ElementTheme.FONT.deriveFont(Font.BOLD, 28f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int baseY = topY + (48 - fm.getHeight()) / 2 + fm.getAscent();
            int textX = cx - fm.stringWidth(glyph) / 2;
            g2.setColor(fg);
            g2.drawString(glyph, textX, baseY);
            g2.dispose();
        }

        static Color colorFor(MessageBoxType t) {
            switch (t) {
                case INFO: case QUESTION: return ElementTheme.PRIMARY;
                case SUCCESS: return ElementTheme.SUCCESS;
                case WARNING: return ElementTheme.WARNING;
                case ERROR: return ElementTheme.DANGER;
            }
            return ElementTheme.PRIMARY;
        }
        static String glyphFor(MessageBoxType t) {
            switch (t) {
                case INFO: return "i";
                case SUCCESS: return "√";
                case WARNING: return "!";
                case ERROR: return "×";
                case QUESTION: return "?";
            }
            return "i";
        }
    }

    // --- Helpers for self-check (same as AstDialog helpers; duplicated here for independent self-check main) ---
    private static Component findChildByText(Container c, String text) {
        if (c == null || text == null) return null;
        Queue<Container> q = new LinkedList<Container>(); q.add(c);
        while (!q.isEmpty()) {
            Container cur = q.poll();
            for (int i = 0; i < cur.getComponentCount(); i++) {
                Component ch = cur.getComponent(i);
                if (ch instanceof JLabel) { if (text.equals(((JLabel) ch).getText())) return ch; }
                else if (ch instanceof Button) { if (text.equals(((Button) ch).getText())) return ch; }
                else if (ch instanceof AbstractButton) { if (text.equals(((AbstractButton) ch).getText())) return ch; }
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
        if (c instanceof AbstractButton) { ((AbstractButton) c).doClick(); return; }
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, Math.min(10, c.getWidth()/2), Math.min(10, c.getHeight()/2), 1, false));
        try { Thread.sleep(15); } catch (Throwable ignore) {}
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, Math.min(10, c.getWidth()/2), Math.min(10, c.getHeight()/2), 1, false));
        try { Thread.sleep(15); } catch (Throwable ignore) {}
    }

    static void selfCheck() {
        boolean threw = false;
        try { AstMessageBox.alert(null, AstMessageBox.MessageBoxType.INFO, "x"); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null owner alert"; threw = false;
        try { AstMessageBox.alert(new JFrame(), null, "x"); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null type alert"; threw = false;
        try { AstMessageBox.alert(new JFrame(), AstMessageBox.MessageBoxType.INFO, null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null msg alert"; threw = false;
        try { AstMessageBox.confirm(new JFrame(), "y", null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null cb confirm";
        // Now test actual dialogs
        final Throwable[] err = {null};
        final int[] cb = {0, 0};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("MsgBox self-check"); jf.setSize(800, 600); jf.setVisible(true);
            // Alert ERROR → click 确定
            AstMessageBox.alert(jf, AstMessageBox.MessageBoxType.ERROR, "发生错误");
            try { Thread.sleep(260); } catch (InterruptedException ignore) {}
            Component gp = jf.getGlassPane(); assert gp != null && gp.isVisible();
            Component card = firstPanelChild((Container) gp);
            Component ok = findChildByText((Container) card, "确定");
            clickComponent(ok);
            try { Thread.sleep(300); } catch (InterruptedException ignore) {}
            // Confirm QUESTION → click 取消
            AstMessageBox.confirm(jf, AstMessageBox.MessageBoxType.QUESTION, "确认删除？", new ConfirmCallback() {
                public void onConfirm() { cb[0]++; }
                public void onCancel() { cb[1]++; }
            });
            try { Thread.sleep(260); } catch (InterruptedException ignore) {}
            card = firstPanelChild((Container) jf.getGlassPane());
            Component cancel = findChildByText((Container) card, "取消");
            clickComponent(cancel);
            try { Thread.sleep(300); } catch (InterruptedException ignore) {}
            assert cb[1] == 1 : "onCancel should fire once";
            // Confirm INFO default type → click 确定 → onConfirm fires
            AstMessageBox.confirm(jf, "ok?", new ConfirmCallback() { public void onConfirm() { cb[0]++; }});
            try { Thread.sleep(260); } catch (InterruptedException ignore) {}
            card = firstPanelChild((Container) jf.getGlassPane());
            Component ok2 = findChildByText((Container) card, "确定");
            clickComponent(ok2);
            try { Thread.sleep(300); } catch (InterruptedException ignore) {}
            assert cb[0] == 1 : "onConfirm should fire once; actual="+cb[0];

            // Off-screen paint icon panels for all 5 types
            for (MessageBoxType t : MessageBoxType.values()) {
                JPanel icon = AstMessageBox.makeIconPanel(t);
                icon.setSize(64, 80); icon.doLayout();
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                try { icon.paint(gg); } finally { gg.dispose(); }
                int center = img.getRGB(32, 40);
                int ca = (center >>> 24) & 0xFF;
                assert ca >= 120 : "icon bg opaque enough for type="+t+"; alpha="+ca;
            }
            jf.dispose();
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstMessageBox self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
