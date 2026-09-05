package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.GlassPane;
import org.swelement.framework.AstContainerComponent;
import org.swelement.framework.AstDisplayComponent;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Drawer 抽屉 — 从屏幕一侧（左/右/上/下）滑出的模态面板，常用于详情、设置、表单等。
 *
 * 用法：
 *   AstDrawer.show(frame, AstDrawer.Direction.RIGHT, "详情", bodyPanel, 360, new Runnable() { public void run() {
 *       // 关闭后回调（可选）
 *   }});
 *
 * 设计：
 *  - 基于 GlassPane 全屏遮罩 + 一张抽屉卡片（白底、BORDER_BASE 边框，方向侧圆角）。
 *  - 标题栏 48px：TEXT_MAIN 粗体 16，BORDER_BASE 下分隔线，右上角 ×（AstIcon CLOSE）。
 *  - body 区域 padding 24，可滚动（>视口高度自动 JScrollPane）。
 *  - 滑入/滑出动画：AnimationManager easeInOut 240ms，translateX/translateY 从全宽偏移到 0。
 *  - 关闭：点 ×、点击遮罩外区域、Esc、或调用 close()。
 *  - 宽度/高度可配置：水平方向传 width（默认 360，钳制 [240, ownerW*0.6]）；
 *    垂直方向传 height（默认 280，钳制 [160, ownerH*0.6]）。
 */
public class AstDrawer {
    public enum Direction { LEFT, RIGHT, TOP, BOTTOM }
    private static final String GP_KEY = AstDrawer.class.getName() + ".gp";
    private static final String CARD_KEY = AstDrawer.class.getName() + ".card";

    public static void show(Window owner, Direction dir, String title, JComponent body) {
        show(owner, dir, title, body, -1, null);
    }

    public static void show(final Window owner, final Direction dir, final String title, final JComponent body,
                            final int size, final Runnable onClosed) {
        if (owner == null) throw new IllegalArgumentException("owner must not be null");
        if (dir == null) throw new IllegalArgumentException("direction must not be null");
        if (title == null) throw new IllegalArgumentException("title must not be null");
        if (body == null) throw new IllegalArgumentException("body must not be null");
        if (!(owner instanceof RootPaneContainer)) throw new IllegalArgumentException("owner must be a RootPaneContainer");
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                show(owner, dir, title, body, size, onClosed);
            }});
            return;
        }
        final RootPaneContainer rpc = (RootPaneContainer) owner;
        GlassPane tempGp = (GlassPane) rpc.getRootPane().getClientProperty(GP_KEY);
        if (tempGp == null || rpc.getGlassPane() != tempGp) {
            tempGp = GlassPane.install(rpc);
            rpc.getRootPane().putClientProperty(GP_KEY, tempGp);
        }
        final GlassPane gp = tempGp;
        // 关闭既有抽屉：立即移除旧的（无滑出动画，避免时序竞态），触发其 onClosed，再开新抽屉
        Object existing = rpc.getRootPane().getClientProperty(CARD_KEY);
        if (existing instanceof DrawerPanel) {
            final DrawerPanel prev = (DrawerPanel) existing;
            prev.stopSlide();
            if (prev.maskCloser != null) { gp.removeMouseListener(prev.maskCloser); prev.maskCloser = null; }
            gp.remove(prev);
            gp.setActive(false);
            rpc.getRootPane().putClientProperty(CARD_KEY, null);
            gp.repaint();
            if (prev.userOnClosed != null) { try { prev.userOnClosed.run(); } catch (Throwable ignore) {} }
        }
        openNew(gp, rpc, owner, dir, title, body, size, onClosed);
    }

    private static void openNew(final GlassPane gp, final RootPaneContainer rpc, Window owner, final Direction dir,
                               final String title, final JComponent body, final int size, final Runnable onClosed) {
        final DrawerPanel[] holder = new DrawerPanel[1];
        final Runnable closeHandler = new Runnable() { public void run() {
            final DrawerPanel card = holder[0];
            if (card == null) return;
            card.startSlideOut(new Runnable() { public void run() {
                gp.remove(card);
                if (card.maskCloser != null) { gp.removeMouseListener(card.maskCloser); card.maskCloser = null; }
                gp.setActive(false);
                rpc.getRootPane().putClientProperty(CARD_KEY, null);
                gp.repaint();
                if (onClosed != null) { try { onClosed.run(); } catch (Throwable ignore) {} }
            }});
        }};
        final DrawerPanel card = new DrawerPanel(dir, title, body, size, owner.getSize(), closeHandler, onClosed);
        holder[0] = card;
        // 点击遮罩（卡片之外的半透明黑区）关闭抽屉。
        // 关键：GlassPane 自身默认没有任何鼠标监听器，AWT 的 Component.eventEnabled() 会判定
        // 该组件未启用鼠标事件从而直接丢弃，所以不挂监听器时点击遮罩毫无反应（卡片内的 × 有效
        // 是因为它挂在卡片上，卡片自带监听器）。
        final MouseAdapter maskCloser = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                // 只有落在抽屉卡片之外的点击才算点遮罩；卡片内部交给卡片自己处理
                if (!card.getBounds().contains(e.getPoint())) card.close();
            }
        };
        card.maskCloser = maskCloser;
        gp.addMouseListener(maskCloser);
        rpc.getRootPane().putClientProperty(CARD_KEY, card);
        gp.removeAll();
        gp.setLayout(null);
        gp.add(card);
        gp.setActive(true);
        // 关键：先 setSize/Location 再启动滑入动画
        gp.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                card.placeIn(gp.getSize());
                card.validate();
                gp.removeComponentListener(this);
            }
        });
        card.placeIn(gp.getSize());
        gp.validate(); // 首次打开：确保 body 子组件完成布局（否则第二次才显示）
        card.startSlideIn();
        // Esc 关闭
        gp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "ast-drawer-close");
        gp.getActionMap().put("ast-drawer-close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { card.close(); }
        });
    }

    // --- DrawerPanel ---
    static final class DrawerPanel extends AstContainerComponent {
        private final Direction dir;
        private final String title;
        private final JComponent body;
        private final int size;
        private final Dimension ownerSize;
        private final Runnable onClose;
        final Runnable userOnClosed;
        private final Animator slide;
        private float progress;   // 0 = 完全隐藏（在屏外），1 = 完全显示
        private int offsetX, offsetY;
        private int placedX, placedY, placedW, placedH;
        /**
         * 挂在宿主 GlassPane 上、用于「点击遮罩关闭抽屉」的监听器。
         * <p>
         * GlassPane 是复用的（按 root pane 缓存在 {@link #GP_KEY}），因此抽屉关闭时必须把它卸载，
         * 否则旧监听器会残留：下一次打开抽屉时，点击遮罩会触发一个已被移除的卡片去 close()，
         * 表现为重复触发 onClosed 回调。
         */
        MouseListener maskCloser;

        DrawerPanel(Direction dir, String title, JComponent body, int size, Dimension ownerSize, Runnable onClose, Runnable userOnClosed) {
            this.dir = dir;
            this.title = title;
            this.body = body;
            this.size = size;
            this.ownerSize = ownerSize == null ? new Dimension(800, 600) : ownerSize;
            this.onClose = onClose;
            this.userOnClosed = userOnClosed;
            slide = new Animator(240, new Easing() { public float apply(float t) { return Easing.easeInOut(t); }},
                new Animator.Listener() { public void update(float v) { progress = v; repositionOffscreen(); repaint(); }});
            buildLayout();
        }

        void placeIn(Dimension glassSize) {
            boolean horiz = (dir == Direction.LEFT || dir == Direction.RIGHT);
            int dim = size > 0 ? size : (horiz ? 360 : 280);
            if (horiz) {
                placedW = Math.max(240, Math.min(dim, Math.round(glassSize.width * 0.6f)));
                placedH = glassSize.height;
            } else {
                placedW = glassSize.width;
                placedH = Math.max(160, Math.min(dim, Math.round(glassSize.height * 0.6f)));
            }
            switch (dir) {
                case LEFT:   placedX = 0; placedY = 0; break;
                case RIGHT:  placedX = glassSize.width - placedW; placedY = 0; break;
                case TOP:    placedX = 0; placedY = 0; break;
                case BOTTOM: placedX = 0; placedY = glassSize.height - placedH; break;
            }
            setBounds(placedX, placedY, placedW, placedH);
            repositionOffscreen();
        }

        private void repositionOffscreen() {
            // progress 0 → 全部在屏外；1 → 在 placedX/placedY
            int dx = 0, dy = 0;
            switch (dir) {
                case LEFT:   dx = -(int) (placedW * (1f - progress)); break;
                case RIGHT:  dx =  (int) (placedW * (1f - progress)); break;
                case TOP:    dy = -(int) (placedH * (1f - progress)); break;
                case BOTTOM: dy =  (int) (placedH * (1f - progress)); break;
            }
            offsetX = dx; offsetY = dy;
            setLocation(placedX + offsetX, placedY + offsetY);
        }

        void startSlideIn() {
            slide.stop();
            progress = 0f;
            slide.go(0f, 1f);
        }

        void startSlideOut(final Runnable after) {
            slide.stop();
            slide.go(progress, 0f, new Runnable() { public void run() { if (after != null) after.run(); }});
        }

        void stopSlide() { slide.stop(); }

        void close() {
            if (onClose != null) onClose.run();
        }

        private void buildLayout() {
            setLayout(new BorderLayout());
            AstDisplayComponent titleBar = new AstDisplayComponent() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = createGraphics(g);
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, getWidth(), getHeight()-1);
                    g2.setColor(theme().getBorderBase());
                    g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                    g2.setColor(theme().getTextPrimary());
                    assertContrast(theme().getTextPrimary(), Color.WHITE, "AstDrawer title");
                    g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
                    FontMetrics fm = g2.getFontMetrics();
                    int baseY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    String t = title; int maxW = getWidth() - 56;
                    if (fm.stringWidth(t) > maxW) {
                        String ell = "\u2026";
                        while (t.length() > 0 && fm.stringWidth(t) + fm.stringWidth(ell) > maxW) t = t.substring(0, t.length()-1);
                        t = t + ell;
                    }
                    g2.drawString(t, 24, baseY);
                    g2.dispose();
                }
                @Override public Dimension getPreferredSize() { return new Dimension(120, 48); }
                @Override public Dimension getMinimumSize() { return new Dimension(80, 48); }
                @Override public boolean isOptimizedDrawingEnabled() { return false; }
                @Override protected void selfCheck() { }
            };
            titleBar.setLayout(new BorderLayout());
            // 关闭按钮：自绘 36x36 区域，hover 变 FILL_BASE
            AstInteractiveComponent closeBtn = new AstInteractiveComponent() {
                {
                    anim.register("hover", 150, Easing::easeInOut);
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                            anim.stop("hover"); anim.go("hover", anim.getProgress("hover"), 1f);
                        }
                        @Override public void mouseExited(java.awt.event.MouseEvent e) {
                            anim.stop("hover"); anim.go("hover", anim.getProgress("hover"), 0f);
                        }
                        @Override public void mousePressed(java.awt.event.MouseEvent e) { close(); }
                    });
                }
                @Override public Dimension getPreferredSize() { return new Dimension(36, 36); }
                @Override public Dimension getMinimumSize() { return new Dimension(36, 36); }
                @Override public boolean isOptimizedDrawingEnabled() { return false; }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = createGraphics(g);
                    float hover = anim.getProgress("hover");
                    if (hover > 0.01f) {
                        int a = Math.round(255 * hover);
                        Color fb = theme().getFillBase();
                        g2.setColor(new Color(fb.getRed(), fb.getGreen(), fb.getBlue(), a));
                        g2.fillOval(0, 0, 35, 35);
                    }
                    // 自绘 ×（两条对角线，stroke 圆角）
                    g2.setColor(theme().getTextRegular());
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    float m = 11, mx = 25;
                    g2.draw(new Line2D.Float(m, m, mx, mx));
                    g2.draw(new Line2D.Float(m, mx, mx, m));
                    g2.dispose();
                }
                @Override protected void selfCheck() { }
            };
            titleBar.add(closeBtn, BorderLayout.EAST);
            JPanel leftFill = new JPanel(); leftFill.setOpaque(false); leftFill.setPreferredSize(new Dimension(6, 36));
            titleBar.add(leftFill, BorderLayout.WEST);
            add(titleBar, BorderLayout.NORTH);

            // body 区域：自动滚动
            JScrollPane sp = new JScrollPane(body, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setBorder(null); sp.setOpaque(false); sp.getViewport().setOpaque(false);
            sp.setBorder(new EmptyBorder(0, 0, 0, 0));
            JPanel bodyWrap = new JPanel(new BorderLayout());
            bodyWrap.setBorder(new EmptyBorder(24, 24, 24, 24));
            bodyWrap.setOpaque(false);
            bodyWrap.add(sp, BorderLayout.CENTER);
            add(bodyWrap, BorderLayout.CENTER);
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(placedW > 0 ? placedW : 360, placedH > 0 ? placedH : 480);
        }
        @Override public Dimension getMinimumSize() { return new Dimension(240, 120); }
        @Override public boolean isOptimizedDrawingEnabled() { return false; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            float progress = this.progress;
            int a = Math.min(255, Math.max(0, Math.round(255f * progress)));
            Color bg = new Color(0xFF, 0xFF, 0xFF, a);
            // 仅在朝向屏幕外的一侧画圆角；朝向屏幕内的一侧直角
            int r = theme().getRadiusBase() * 2;
            // 简化处理：左右方向时左侧/右侧 2 个圆角，上下方向时顶/底 2 个圆角
            RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1.5f, getHeight()-1.5f, r, r);
            g2.setColor(bg);
            g2.fill(rect);
            Color bb = theme().getBorderBase();
            g2.setColor(new Color(bb.getRed(), bb.getGreen(), bb.getBlue(), a));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(rect);
            g2.dispose();
        }

        @Override protected void selfCheck() { }
    }

    private static JLabel findLabel(Container c, String text) {
        if (c == null) return null;
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component ch = c.getComponent(i);
            if (ch instanceof JLabel && text.equals(((JLabel) ch).getText())) return (JLabel) ch;
            if (ch instanceof Container) { JLabel r = findLabel((Container) ch, text); if (r != null) return r; }
        }
        return null;
    }

    /**
     * 构造一个 MOUSE_PRESSED 事件，供 self-check 直接派发到 GlassPane 模拟「点击遮罩/卡片」。
     * <p>
     * 注意：坐标相对事件源（GlassPane）计算，与 {@code DrawerPanel.getBounds()}（相对其父 GlassPane）处于同一坐标系，
     * 因此可以在自检里直接断言「落点是否在卡片 bounds 内」。
     */
    private static MouseEvent mousePress(Component c, int x, int y) {
        return new MouseEvent(c, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                0, x, y, 1, false, MouseEvent.BUTTON1);
    }

    // --- self-check ---
    static void selfCheck() {
        org.swelement.core.theme.ThemeManager.ensureDefaultTheme();
        boolean threw = false;
        try { AstDrawer.show(null, Direction.RIGHT, "x", new JPanel()); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null owner"; threw = false;
        try { AstDrawer.show(new JFrame(), null, "x", new JPanel()); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null dir"; threw = false;
        try { AstDrawer.show(new JFrame(), Direction.RIGHT, null, new JPanel()); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null title"; threw = false;
        try { AstDrawer.show(new JFrame(), Direction.RIGHT, "x", null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null body"; threw = false;
        try { AstDrawer.show(new Window((Frame)null) {}, Direction.RIGHT, "x", new JPanel()); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "non-RPC owner";

        final Throwable[] err = {null};
        final boolean[] closed = {false};
        final JFrame[] jfHolder = {null};
        final DrawerPanel[] cardHolder = {null};
        try {
            // 1) EDT：创建窗体 + 显示抽屉
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    jfHolder[0] = new JFrame("AstDrawer SC"); jfHolder[0].setSize(800, 600); jfHolder[0].setVisible(true);
                    JLabel info = new JLabel("抽屉正文"); info.setForeground(org.swelement.core.theme.ThemeManager.getCurrent().getTextRegular());
                    AstDrawer.show(jfHolder[0], Direction.RIGHT, "详情标题", info, 360, new Runnable() { public void run() { closed[0] = true; }});
                } catch (Throwable t) { err[0] = t; }
            }});
            if (err[0] == null) Thread.sleep(200); // 主线程 sleep，EDT Timer 可 tick
            // 2) EDT：断言 glass pane + 找到 card + 点击遮罩关闭
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    Component gp = jfHolder[0].getGlassPane();
                    assert gp != null && gp.isVisible() : "glass pane visible";
                    Container c = (Container) gp;
                    for (int i = 0; i < c.getComponentCount(); i++) {
                        Component ch = c.getComponent(i);
                        if (ch instanceof DrawerPanel) { cardHolder[0] = (DrawerPanel) ch; break; }
                    }
                    assert cardHolder[0] != null : "drawer card 已挂载到 glass pane";
                    // 首次打开：body 子组件应已被 validate，可见且有非零尺寸
                    JLabel bodyLbl = findLabel(cardHolder[0], "抽屉正文");
                    assert bodyLbl != null : "body 标签应已挂载到抽屉";
                    assert bodyLbl.getWidth() > 0 && bodyLbl.getHeight() > 0 : "首次打开 body 应已完成布局（宽高>0），实际="
                            + bodyLbl.getWidth() + "x" + bodyLbl.getHeight();
                    // 点击遮罩关闭（模态抽屉的基本行为）
                    java.awt.Rectangle cb = cardHolder[0].getBounds();
                    int inX = cb.x + cb.width / 2, inY = cb.y + cb.height / 2;
                    int maskX = Math.max(1, cb.x / 2), maskY = Math.max(1, cb.y + cb.height / 2);
                    assert !cb.contains(maskX, maskY) : "遮罩测试点必须落在卡片之外，实际 card=" + cb;
                    // 2a) 负向：点击卡片内部不应关闭
                    gp.dispatchEvent(mousePress(gp, inX, inY));
                    boolean stillOpen = false;
                    for (int i = 0; i < c.getComponentCount(); i++) {
                        if (c.getComponent(i) instanceof DrawerPanel) { stillOpen = true; break; }
                    }
                    assert stillOpen : "点击抽屉卡片内部不应关闭抽屉";
                    // 2b) 正向：点击遮罩（半透明黑区）应关闭
                    gp.dispatchEvent(mousePress(gp, maskX, maskY));
                } catch (Throwable t) { err[0] = t; }
            }});
            if (err[0] == null) Thread.sleep(280); // 等待 slide-out 动画完成（240ms）
            if (err[0] == null) assert closed[0] : "点击遮罩应关闭抽屉并触发 onClosed 回调";
            // 2c) 重新打开，继续验证 close() 路径未被破坏
            closed[0] = false;
            cardHolder[0] = null;
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    JLabel info2 = new JLabel("抽屉正文2");
                    AstDrawer.show(jfHolder[0], Direction.RIGHT, "详情标题", info2, 360,
                            new Runnable() { public void run() { closed[0] = true; }});
                } catch (Throwable t) { err[0] = t; }
            }});
            if (err[0] == null) Thread.sleep(200);
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    Container c = (Container) jfHolder[0].getGlassPane();
                    for (int i = 0; i < c.getComponentCount(); i++) {
                        Component ch = c.getComponent(i);
                        if (ch instanceof DrawerPanel) { cardHolder[0] = (DrawerPanel) ch; break; }
                    }
                    assert cardHolder[0] != null : "重新打开的 drawer card 应挂载到 glass pane";
                    cardHolder[0].close(); // 触发 slide out → onClosed
                } catch (Throwable t) { err[0] = t; }
            }});
            if (err[0] == null) Thread.sleep(280); // 等待 slide-out 动画完成（240ms）
            if (err[0] == null) assert closed[0] : "onClosed 回调执行";
            // 3) 替换测试：先开 LEFT（带 onClosed），再用 TOP 替换 → LEFT 的 onClosed 应触发
            closed[0] = false;
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    AstDrawer.show(jfHolder[0], Direction.LEFT, "左侧", new JLabel("左"), 280, new Runnable() { public void run() { closed[0] = true; }});
                } catch (Throwable t) { err[0] = t; }
            }});
            if (err[0] == null) Thread.sleep(60);
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    AstDrawer.show(jfHolder[0], Direction.TOP, "上", new JLabel("上"), 200, null);
                } catch (Throwable t) { err[0] = t; }
            }});
            // 替换路径同步触发旧抽屉 onClosed，无需等动画
            if (err[0] == null) assert closed[0] : "旧抽屉关闭后回调";
            SwingUtilities.invokeAndWait(new Runnable() { public void run() { jfHolder[0].dispose(); }});
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);

        // 离屏绘制 DrawerPanel 校验对比度断言
        DrawerPanel card = new DrawerPanel(Direction.RIGHT, "标题", new JLabel("body"), 360, new Dimension(800, 600), null, null);
        card.placeIn(new Dimension(800, 600));
        card.progress = 1f;
        card.setSize(card.placedW, card.placedH);
        card.setLocation(card.placedX, card.placedY);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                Math.max(1, card.placedW), Math.max(1, card.placedH), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { card.paint(gg); } finally { gg.dispose(); }
        int px = img.getRGB(10, 10); int a = (px >>> 24) & 0xFF;
        assert a > 120 : "DrawerPanel 绘制不透明 alpha=" + a;
        System.out.println("AstDrawer self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
