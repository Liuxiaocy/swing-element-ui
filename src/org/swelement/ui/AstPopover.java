package org.swelement.ui;

import org.swelement.core.AnimatedPopup;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Popover 气泡卡片 — 点击触发器（或 hover）后弹出一个带标题的卡片浮层。
 * 与 Tooltip 的区别：Popover 容纳富内容（任意 JComponent），且默认点击触发、不随鼠标移开消失。
 *
 * 用法：
 *   AstPopover pop = new AstPopover("标题", bodyPanel, AnimatedPopup.Direction.BELOW);
 *   pop.setTriggerText("点我");
 *   frame.add(pop);
 *
 *   // 或将一个既有按钮作为触发器：
 *   AstPopover pop = AstPopover.wrap(myButton, "标题", bodyPanel, AnimatedPopup.Direction.RIGHT);
 *
 * 设计：
 *  - 触发器：默认使用 Button（PRIMARY 风格），可替换为任意 JComponent（wrap 静态方法）。
 *  - 内容卡片：白底、BORDER_BASE 边框、RADIUS 圆角，标题栏 36px（TEXT_MAIN 粗体，白底），
 *    下接 1px 分隔线，body 区域 padding 16。
 *  - 触发方式：CLICK（默认）/ HOVER。HOVER 模式参考 Tooltip 延时 200ms 显示，离开后 120ms 隐藏。
 *  - 关闭：再次点击触发器、点击卡片外部（AnimatedPopup 内置 AWT dismiss）、或调用 hidePopover()。
 */
public class AstPopover extends JComponent {
    public enum Trigger { CLICK, HOVER }

    private String title;
    private JComponent body;
    private final AnimatedPopup.Direction dir;
    private final AnimatedPopup popup;
    private Trigger trigger = Trigger.CLICK;
    private boolean open;
    private JComponent invokerComp;      // 真正的触发组件（按钮或 wrap 进来的 JComponent）
    private Timer hoverShowTimer;
    private Timer hoverHideTimer;
    private static final int HOVER_SHOW_DELAY = 200;
    private static final int HOVER_HIDE_DELAY = 120;

    public AstPopover(String title, JComponent body, AnimatedPopup.Direction dir) {
        this(title, body, dir, null);
    }

    public AstPopover(String title, JComponent body, AnimatedPopup.Direction dir, String triggerText) {
        if (title == null) throw new IllegalArgumentException("title must not be null");
        if (body == null) throw new IllegalArgumentException("body must not be null");
        if (dir == null) throw new IllegalArgumentException("direction must not be null");
        this.title = title;
        this.body = body;
        this.dir = dir;
        this.popup = new AnimatedPopup();
        popup.setDismissListener(new Runnable() { public void run() { open = false; }});
        AnimatedPopup.registerGlobal(popup, AnimatedPopup.PopupLayer.POPUP);
        Button invokerBtn = new Button(triggerText == null ? "Popover" : triggerText, Button.DEFAULT, false);
        this.invokerComp = invokerBtn;
        installInvoker(invokerBtn, true);
        setLayout(new BorderLayout());
        add(invokerBtn, BorderLayout.CENTER);
        setOpaque(false);
    }

    /** 使用既有 JComponent 作为触发器（hover/click 由本组件接管监听）。 */
    public static AstPopover wrap(JComponent invoker, String title, JComponent body, AnimatedPopup.Direction dir) {
        if (invoker == null) throw new IllegalArgumentException("invoker must not be null");
        AstPopover p = new AstPopover(title, body, dir);
        // 移除默认按钮，替换为传入的 invoker
        p.remove(p.invokerComp);
        p.invokerComp = invoker;
        p.installInvoker(invoker, false);
        p.add(invoker, BorderLayout.CENTER);
        return p;
    }

    private void installInvoker(final JComponent c, boolean isButton) {
        // CLICK 模式
        if (isButton && c instanceof Button) {
            ((Button) c).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (trigger == Trigger.CLICK) toggle();
                }
            });
        } else {
            c.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (trigger == Trigger.CLICK) toggle();
                }
            });
        }
        // HOVER 模式（无论是否是 Button，都加 mouseEntered/Exited）
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (trigger != Trigger.HOVER) return;
                cancelHoverHide();
                scheduleHoverShow();
            }
            @Override public void mouseExited(MouseEvent e) {
                if (trigger != Trigger.HOVER) return;
                cancelHoverShow();
                scheduleHoverHide();
            }
        });
    }

    public void setTrigger(Trigger t) {
        if (t == null) throw new IllegalArgumentException("trigger must not be null");
        this.trigger = t;
    }

    public Trigger getTrigger() { return trigger; }

    public void setTitle(String t) {
        if (t == null) throw new IllegalArgumentException("title must not be null");
        this.title = t;
        if (open) updatePopupContent();
    }

    public String getTitle() { return title; }

    public void setBody(JComponent b) {
        if (b == null) throw new IllegalArgumentException("body must not be null");
        this.body = b;
        if (open) updatePopupContent();
    }

    public JComponent getBody() { return body; }

    public void showPopover() {
        if (open) return;
        open = true;
        updatePopupContent();
        popup.show(this, dir);
    }

    public void hidePopover() {
        if (!open) return;
        open = false;
        popup.hideWithAnimation(null);
    }

    public void toggle() { if (open) hidePopover(); else showPopover(); }

    public boolean isOpen() { return open; }

    @Override public Dimension getPreferredSize() { return invokerComp.getPreferredSize(); }
    @Override public Dimension getMinimumSize() { return invokerComp.getMinimumSize(); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    private void updatePopupContent() {
        Container cc = popup.getContent();
        cc.removeAll();
        CardPanel card = new CardPanel(title, body);
        cc.add(card, BorderLayout.CENTER);
        popup.setPreferredSize(card.getPreferredSize());
    }

    private void scheduleHoverShow() {
        cancelHoverShow();
        hoverShowTimer = new Timer(HOVER_SHOW_DELAY, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hoverShowTimer = null;
                if (!open) showPopover();
            }
        });
        hoverShowTimer.setRepeats(false);
        hoverShowTimer.start();
    }

    private void cancelHoverShow() {
        if (hoverShowTimer != null) { hoverShowTimer.stop(); hoverShowTimer = null; }
    }

    private void scheduleHoverHide() {
        cancelHoverHide();
        hoverHideTimer = new Timer(HOVER_HIDE_DELAY, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hoverHideTimer = null;
                if (open) hidePopover();
            }
        });
        hoverHideTimer.setRepeats(false);
        hoverHideTimer.start();
    }

    private void cancelHoverHide() {
        if (hoverHideTimer != null) { hoverHideTimer.stop(); hoverHideTimer = null; }
    }

    // --- 卡片面板：标题 + 分隔线 + body ---
    private static final class CardPanel extends JPanel {
        private final String title;
        private final JComponent body;

        CardPanel(String title, JComponent body) {
            this.title = title;
            this.body = body;
            setOpaque(false);
            setLayout(new BorderLayout());
            add(new TitleBar(title), BorderLayout.NORTH);
            JPanel bodyWrap = new JPanel(new BorderLayout());
            bodyWrap.setBorder(new EmptyBorder(16, 16, 16, 16));
            bodyWrap.setOpaque(false);
            bodyWrap.add(body, BorderLayout.CENTER);
            add(bodyWrap, BorderLayout.CENTER);
        }

        @Override public Dimension getPreferredSize() {
            Dimension bp = body.getPreferredSize();
            int w = Math.max(240, bp.width + 32 + 4);
            int h = 36 + bp.height + 32;
            return new Dimension(Math.min(w, 520), Math.min(h, 480));
        }
        @Override public Dimension getMinimumSize() { return new Dimension(240, 96); }
        @Override public boolean isOptimizedDrawingEnabled() { return false; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int r = ElementTheme.RADIUS * 2;
            RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1.5f, getHeight()-1.5f, r, r);
            g2.setColor(Color.WHITE);
            g2.fill(rect);
            g2.setColor(ElementTheme.BORDER_BASE);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(rect);
            g2.dispose();
        }

        private static final class TitleBar extends JPanel {
            private final String title;
            TitleBar(String title) { this.title = title; setOpaque(false); }
            @Override public Dimension getPreferredSize() { return new Dimension(240, 36); }
            @Override public Dimension getMinimumSize() { return new Dimension(120, 36); }
            @Override public boolean isOptimizedDrawingEnabled() { return false; }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight()-1);
                g2.setColor(ElementTheme.BORDER_BASE);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.setColor(ElementTheme.TEXT_MAIN);
                ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstPopover title");
                g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 14f));
                FontMetrics fm = g2.getFontMetrics();
                int baseY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                String t = title; int maxW = getWidth() - 24;
                if (fm.stringWidth(t) > maxW) {
                    String ell = "\u2026";
                    while (t.length() > 0 && fm.stringWidth(t) + fm.stringWidth(ell) > maxW) t = t.substring(0, t.length()-1);
                    t = t + ell;
                }
                g2.drawString(t, 16, baseY);
                g2.dispose();
            }
        }
    }

    static void selfCheck() {
        boolean threw = false;
        try { new AstPopover(null, new JPanel(), AnimatedPopup.Direction.BELOW); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null title must throw"; threw = false;
        try { new AstPopover("x", null, AnimatedPopup.Direction.BELOW); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null body must throw"; threw = false;
        try { new AstPopover("x", new JPanel(), null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null dir must throw"; threw = false;
        try { new AstPopover("x", new JPanel(), AnimatedPopup.Direction.BELOW).setTrigger(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null trigger must throw"; threw = false;
        try { AstPopover.wrap(null, "x", new JPanel(), AnimatedPopup.Direction.BELOW); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "wrap null invoker must throw"; threw = false;
        try { new AstPopover("x", new JPanel(), AnimatedPopup.Direction.BELOW).setTitle(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "setTitle null must throw"; threw = false;
        try { new AstPopover("x", new JPanel(), AnimatedPopup.Direction.BELOW).setBody(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "setBody null must throw";

        // Title/body 修改后 isOpen 时刷新
        AstPopover p0 = new AstPopover("标题", new JPanel(), AnimatedPopup.Direction.BELOW);
        assert "标题".equals(p0.getTitle());
        p0.setTitle("新标题"); p0.setBody(new JLabel("内容"));
        assert "新标题".equals(p0.getTitle());

        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                JFrame jf = new JFrame("AstPopover SC"); jf.setSize(800, 600); jf.setVisible(true);
                JLabel info = new JLabel("卡片正文"); info.setForeground(ElementTheme.TEXT_REGULAR);
                AstPopover pop = new AstPopover("提示标题", info, AnimatedPopup.Direction.BELOW, "点我");
                JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new FlowLayout()); cp.add(pop); jf.pack();
                pop.showPopover();
                assert pop.isOpen() : "showPopover 应打开";
                try { Thread.sleep(60); } catch (Throwable ignore) {}
                JLayeredPane lp = jf.getLayeredPane();
                AnimatedPopup found = null;
                for (int i = 0; i < lp.getComponentCount(); i++) {
                    Component c = lp.getComponent(i);
                    if (c instanceof AnimatedPopup) { found = (AnimatedPopup) c; break; }
                }
                assert found != null : "popup 已挂载到 layered pane";
                // 卡片应含标题"提示标题"
                boolean hasTitle = false;
                if (found != null) {
                    java.util.Queue<Container> q = new java.util.LinkedList<Container>(); q.add(found);
                    while (!q.isEmpty() && !hasTitle) {
                        Container cur = q.poll();
                        for (int i = 0; i < cur.getComponentCount(); i++) {
                            Component ch = cur.getComponent(i);
                            if (ch instanceof CardPanel) hasTitle = true;
                            if (ch instanceof Container) q.add((Container) ch);
                        }
                    }
                }
                assert hasTitle : "popup 内含 CardPanel";
                // 再次调用 showPopover 不重复打开
                pop.showPopover();
                int popups = 0;
                for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) popups++;
                assert popups == 1 : "重复 showPopover 不应创建第二个 popup，实际=" + popups;
                // 关闭
                pop.hidePopover();
                try { Thread.sleep(200); } catch (Throwable ignore) {}
                assert !pop.isOpen() : "hidePopover 后 open=false";
                // toggle 测试
                pop.toggle();
                assert pop.isOpen();
                pop.toggle();
                assert !pop.isOpen();
                jf.dispose();
            }});
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);

        // 离屏绘制 CardPanel 校验对比度断言
        JLabel info = new JLabel("正文"); info.setForeground(ElementTheme.TEXT_REGULAR);
        CardPanel card = new CardPanel("标题", info);
        card.setSize(card.getPreferredSize());
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                Math.max(1, card.getWidth()), Math.max(1, card.getHeight()), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { card.paint(gg); } finally { gg.dispose(); }
        int px = img.getRGB(10, 10); int a = (px >>> 24) & 0xFF;
        assert a > 120 : "CardPanel 绘制不透明 alpha=" + a;
        System.out.println("AstPopover self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
