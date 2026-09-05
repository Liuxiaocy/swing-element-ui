package org.swelement.ui;

import org.swelement.core.AnimatedPopup;
import org.swelement.core.Easing;
import org.swelement.core.theme.ThemeManager;
import org.swelement.framework.AstAbstractComponent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * 通知 Notification — 出现在页面四角的轻量通知（不阻断后台操作），常用于异步结果反馈。
 *
 * 用法：
 *   AstNotification.show(frame, AstNotification.NotificationType.SUCCESS, "成功", "保存成功");
 *   AstNotification.show(frame, AstNotification.NotificationType.WARNING, "警告", "请注意", 0, AstNotification.Position.BOTTOM_RIGHT, null);
 *
 * 特性：
 *  - 默认位置 TOP_RIGHT（右上角），支持 TOP_LEFT / BOTTOM_RIGHT / BOTTOM_LEFT。
 *  - 类型：INFO / SUCCESS / WARNING / ERROR（左侧圆点图标 + 卡片左侧强调色）。
 *  - 标题 + 描述；右上角 × 手动关闭；默认 4.5s 自动关闭（可在 show 中指定时长）。
 *  - 同位置多条自动向下/向上堆叠，关闭后其余自动补齐位置。
 *  - 复用 AnimatedPopup（TOOL 层，不响应外部点击关闭，避免误触刚弹出的通知）。
 */
public class AstNotification {
    public enum NotificationType { INFO, SUCCESS, WARNING, ERROR }
    public enum Position { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT }

    private static final IdentityHashMap<Container, List<Entry>> openPerOwner =
            new IdentityHashMap<Container, List<Entry>>();
    private static final int OFFSET = 16;       // 同位置相邻通知的间距
    private static final int MARGIN = 20;       // 距屏幕边缘
    private static final int DEFAULT_DURATION = 4500;

    static final class Entry {
        AnimatedPopup popup;
        NotificationCard card;
        Position pos;
        Container lp;
        Entry(AnimatedPopup p, NotificationCard c, Position pos, Container lp) {
            this.popup = p; this.card = c; this.pos = pos; this.lp = lp;
        }
    }

    public static void show(Window owner, NotificationType type, String title, String message) {
        show(owner, type, title, message, DEFAULT_DURATION, Position.TOP_RIGHT, null);
    }
    public static void show(Window owner, NotificationType type, String title, String message, int durationMs) {
        show(owner, type, title, message, durationMs, Position.TOP_RIGHT, null);
    }
    public static void show(Window owner, NotificationType type, String title, String message, int durationMs, Position pos) {
        show(owner, type, title, message, durationMs, pos, null);
    }
    public static void show(final Window owner, final NotificationType type, final String title, final String message,
                            final int durationMs, final Position pos, final Runnable onClose) {
        if (owner == null) throw new IllegalArgumentException("owner must not be null");
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (!(owner instanceof RootPaneContainer)) throw new IllegalArgumentException("owner must be RootPaneContainer");
        final int dur = durationMs <= 0 ? DEFAULT_DURATION : Math.max(500, durationMs);
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                show(owner, type, title, message, dur, pos, onClose);
            }});
            return;
        }
        final RootPaneContainer rpc = (RootPaneContainer) owner;
        final NotificationCard card = new NotificationCard(type, pos, title, message);
        final AnimatedPopup popup = new AnimatedPopup();
        popup.setDismissOnOutsideClick(false); // 避免触发按钮的 MOUSE_PRESSED 立刻打掉刚弹出的通知
        AnimatedPopup.registerGlobal(popup, AnimatedPopup.PopupLayer.TOOL);
        popup.getContent().setLayout(new BorderLayout());
        popup.getContent().add(card, BorderLayout.CENTER);
        popup.setPreferredSize(card.getPreferredSize());
        card.onClose = onClose;

        final JLayeredPane lp = rpc.getLayeredPane();
        List<Entry> open = openPerOwner.get(lp);
        if (open == null) { open = new ArrayList<Entry>(); openPerOwner.put(lp, open); }
        final List<Entry> openList = open;
        final int idx = sideCount(openList, pos);
        openList.add(new Entry(popup, card, pos, lp));

        Rectangle screen = screenOf(owner);
        Dimension size = card.getPreferredSize();
        Point pt = cornerPoint(screen, size, pos, idx);
        Point lpPt = new Point(pt);
        SwingUtilities.convertPointFromScreen(lpPt, lp);
        popup.setBounds(lpPt.x, lpPt.y, size.width, size.height);
        lp.add(popup, JLayeredPane.POPUP_LAYER);
        lp.moveToFront(popup);
        card.startIn();

        final Timer closeT = new Timer(dur, null);
        closeT.setRepeats(false);
        final ActionListener doHide = new ActionListener() { public void actionPerformed(ActionEvent e) {
            closeT.stop();
            hide(card, openList);
        }};
        closeT.addActionListener(doHide);
        closeT.start();
    }

    /** 供文档截图/预览使用：返回一个已渲染（完全显示态）的通知卡片，不挂载到任何窗口。 */
    public static JComponent preview(NotificationType type, String title, String message) {
        ThemeManager.ensureDefaultTheme();
        NotificationCard card = new NotificationCard(type, Position.TOP_RIGHT, title, message);
        card.setInProgress(1f);
        card.setSize(card.getPreferredSize());
        return card;
    }

    /** × 手动关闭时调用：从对应 owner 的列表中移除并播放退出动画。 */
    static void hideCard(NotificationCard card) {
        for (List<Entry> list : openPerOwner.values()) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).card == card) {
                    hideFrom(list, i);
                    return;
                }
            }
        }
    }

    private static void hideFrom(List<Entry> list, int i) {
        final Entry en = list.remove(i);
        if (en.card.onClose != null) { try { en.card.onClose.run(); } catch (Throwable ignore) { } }
        en.popup.hideWithAnimation(new Runnable() { public void run() { reposition(en.lp, list); } });
    }

    static void hide(NotificationCard card, List<Entry> openList) {
        for (int i = 0; i < openList.size(); i++) {
            if (openList.get(i).card == card) { hideFrom(openList, i); return; }
        }
    }

    /** 关闭后同位置其余通知重新贴边/补位。 */
    private static void reposition(Container lp, List<Entry> list) {
        if (lp == null || !lp.isShowing()) return;
        try {
            Window w = SwingUtilities.getWindowAncestor(lp);
            Rectangle screen = screenOf(w);
            int topIdx = 0, botIdx = 0;
            for (Entry en : list) {
                boolean top = en.pos == Position.TOP_RIGHT || en.pos == Position.TOP_LEFT;
                int idx = top ? topIdx++ : botIdx++;
                Point pt = cornerPoint(screen, en.popup.getSize(), en.pos, idx);
                Point lpPt = new Point(pt);
                SwingUtilities.convertPointFromScreen(lpPt, lp);
                en.popup.setLocation(lpPt.x, lpPt.y);
            }
        } catch (Throwable ignore) { }
    }

    private static int sideCount(List<Entry> list, Position pos) {
        int n = 0;
        boolean top = pos == Position.TOP_RIGHT || pos == Position.TOP_LEFT;
        for (Entry e : list) {
            boolean et = e.pos == Position.TOP_RIGHT || e.pos == Position.TOP_LEFT;
            if (et == top) n++;
        }
        return n;
    }

    private static Point cornerPoint(Rectangle screen, Dimension size, Position pos, int idx) {
        int x = (pos == Position.TOP_LEFT || pos == Position.BOTTOM_LEFT)
                ? screen.x + MARGIN
                : screen.x + screen.width - MARGIN - size.width;
        int y;
        if (pos == Position.TOP_RIGHT || pos == Position.TOP_LEFT) {
            y = screen.y + MARGIN + idx * (size.height + OFFSET);
        } else {
            y = screen.y + screen.height - MARGIN - size.height - idx * (size.height + OFFSET);
        }
        return new Point(x, y);
    }

    private static Rectangle screenOf(Window owner) {
        GraphicsConfiguration gc = owner.getGraphicsConfiguration();
        if (gc != null) return gc.getBounds();
        return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    }

    // ---------------- NotificationCard ----------------
    static final class NotificationCard extends AstAbstractComponent {
        final NotificationType type;
        final Position pos;
        final String title;
        final String message;
        Runnable onClose;
        private boolean hoverClose;
        private Rectangle closeRect;

        NotificationCard(NotificationType t, Position pos, String title, String message) {
            this.type = t; this.pos = pos; this.title = title; this.message = message;
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            anim.register("in", 260, Easing::easeOut);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (closeRect != null && closeRect.contains(e.getPoint())) hideCard(NotificationCard.this);
                }
                @Override public void mouseMoved(MouseEvent e) {
                    boolean h = closeRect != null && closeRect.contains(e.getPoint());
                    if (h != hoverClose) { hoverClose = h; repaint(); }
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (hoverClose) { hoverClose = false; repaint(); }
                }
            });
        }

        void startIn() { anim.go("in", 0f, 1f); }
        void setInProgress(float v) { anim.setProgress("in", v); }
        float getInProgress() { return anim.getProgress("in"); }

        @Override public Dimension getPreferredSize() {
            int w = 340;
            int h = 16 + 22; // padding + title line
            if (message != null && !message.isEmpty()) {
                int lines = (message.length() + 27) / 28;
                h += 8 + lines * 18;
            }
            h += 16;
            return new Dimension(w, h);
        }
        @Override public Dimension getMinimumSize() { return new Dimension(280, 64); }
        @Override public boolean isOptimizedDrawingEnabled() { return false; }

        @Override protected void selfCheck() { }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            float p = anim.getProgress("in");
            int a = Math.min(255, Math.max(0, Math.round(255f * p)));
            int slide = (int) (16f * (1f - p));
            int dy = (pos == Position.TOP_RIGHT || pos == Position.TOP_LEFT) ? -slide : slide;
            g2.translate(0, dy);

            Color bg = new Color(0xFF, 0xFF, 0xFF, a);
            Color borderBase = theme().getBorderBase();
            Color border = new Color(borderBase.getRed(), borderBase.getGreen(), borderBase.getBlue(), a);
            int r = theme().getRadiusBase();
            RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1.5f, getHeight() - 1.5f, r, r);
            g2.setColor(bg); g2.fill(rect);
            g2.setColor(border); g2.setStroke(new BasicStroke(1f)); g2.draw(rect);

            // 左侧类型圆点图标
            int iconX = 16, iconY = 16, iconS = 20;
            AstIcon.paintIcon(g2, iconTypeFor(type), colorFor(type), iconS, 0f);

            // 标题
            assertContrast(theme().getTextPrimary(), Color.WHITE, "AstNotification title");
            g2.setColor(new Color(theme().getTextPrimary().getRed(), theme().getTextPrimary().getGreen(), theme().getTextPrimary().getBlue(), a));
            Font tf = theme().getFontBase().deriveFont(Font.BOLD, 15f);
            g2.setFont(tf);
            FontMetrics tfm = g2.getFontMetrics(tf);
            int tx = iconX + iconS + 8;
            int ty = 16 + tfm.getAscent();
            g2.drawString(title != null ? title : "", tx, ty);

            // 描述
            if (message != null && !message.isEmpty()) {
                assertContrast(theme().getTextRegular(), Color.WHITE, "AstNotification message");
                g2.setColor(new Color(theme().getTextRegular().getRed(), theme().getTextRegular().getGreen(), theme().getTextRegular().getBlue(), a));
                Font mf = theme().getFontBase().deriveFont(13f);
                g2.setFont(mf);
                FontMetrics mfm = g2.getFontMetrics(mf);
                int my = ty + 8 + mfm.getAscent();
                int avail = getWidth() - tx - 12;
                int per = Math.max(8, avail / 14);
                int i = 0, line = 0;
                while (i < message.length() && line < 3) {
                    int end = Math.min(message.length(), i + per);
                    g2.drawString(message.substring(i, end), tx, my + line * 18);
                    i = end; line++;
                }
            }

            // 关闭 ×
            int cw = 20, cxr = getWidth() - 12 - cw, cyr = 10;
            closeRect = new Rectangle(cxr, cyr, cw, cw);
            if (hoverClose) {
                g2.setColor(theme().getFillBase());
                g2.fillOval(cxr + 2, cyr + 2, cw - 4, cw - 4);
            }
            g2.setColor(theme().getTextRegular());
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int m = cxr + 6, mx = cxr + cw - 6;
            g2.drawLine(m, cyr + 6, mx, cyr + cw - 6);
            g2.drawLine(m, cyr + cw - 6, mx, cyr + 6);
            g2.dispose();
        }

        Color colorFor(NotificationType t) {
            switch (t) {
                case SUCCESS: return theme().getSuccess();
                case WARNING: return theme().getWarning();
                case ERROR:   return theme().getDanger();
                case INFO:
                default:      return theme().getPrimary();
            }
        }
        AstIcon.Type iconTypeFor(NotificationType t) {
            switch (t) {
                case SUCCESS: return AstIcon.Type.CIRCLE_CHECK;
                case WARNING: return AstIcon.Type.CIRCLE_WARNING;
                case ERROR:   return AstIcon.Type.CIRCLE_CLOSE;
                case INFO:
                default:      return AstIcon.Type.CIRCLE_INFO;
            }
        }
    }

    // ---------------- self-check ----------------
    static void selfCheck() {
        ThemeManager.ensureDefaultTheme();
        boolean threw = false;
        try { AstNotification.show(null, NotificationType.INFO, "t", "m"); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null owner"; threw = false;
        try { AstNotification.show(new JFrame(), null, "t", "m"); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null type"; threw = false;
        try { AstNotification.show(new Window((Frame) null) { }, NotificationType.INFO, "t", "m"); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "non-RPC owner";

        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            try {
                JFrame jf = new JFrame("Notification SC"); jf.setSize(800, 600); jf.setVisible(true);
                AstNotification.show(jf, NotificationType.SUCCESS, "成功", "数据已保存");
                Thread.sleep(320);
                JLayeredPane lp = jf.getLayeredPane();
                AnimatedPopup popup = null;
                for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { popup = (AnimatedPopup) lp.getComponent(i); break; }
                assert popup != null : "notification popup added";
                // 强制完全显示状态后离屏绘制
                Container content = popup.getContent();
                NotificationCard card = null;
                for (int ci = 0; ci < content.getComponentCount(); ci++) if (content.getComponent(ci) instanceof NotificationCard) { card = (NotificationCard) content.getComponent(ci); break; }
                assert card != null : "notification card mounted";
                card.setInProgress(1f);
                popup.setBounds(0, 0, card.getPreferredSize().width, card.getPreferredSize().height);
                BufferedImage img = new BufferedImage(card.getPreferredSize().width, card.getPreferredSize().height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                try { popup.paint(gg); } finally { gg.dispose(); }
                int px = img.getRGB(card.getPreferredSize().width / 2, card.getPreferredSize().height / 2);
                assert (px >>> 24 & 0xFF) > 120 : "notification bg opaque; alpha=" + (px >>> 24 & 0xFF);
                // 第二条：验证堆叠数 ≥ 2
                AstNotification.show(jf, NotificationType.INFO, "提示", "第二条通知", 800);
                Thread.sleep(320);
                int count = 0;
                for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) count++;
                assert count >= 2 : "at least 2 notifications after 2nd show; count=" + count;
                jf.dispose();
            } catch (Throwable t) { err[0] = t; }
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstNotification self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
