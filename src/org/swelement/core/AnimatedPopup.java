package org.swelement.core;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class AnimatedPopup extends JComponent {
    public enum Direction { ABOVE, BELOW, LEFT, RIGHT, TOP_CENTER, BOTTOM_RIGHT_CORNER }
    public enum PopupLayer { POPUP, TOOL, MODAL }
    private static final java.util.List<AnimatedPopup> globalStack =
            java.util.Collections.synchronizedList(new java.util.ArrayList<AnimatedPopup>());
    private static final java.util.IdentityHashMap<PopupLayer,Integer> layerZ =
            new java.util.IdentityHashMap<PopupLayer,Integer>() {{
                put(PopupLayer.POPUP, 0);
                put(PopupLayer.TOOL, 100);
                put(PopupLayer.MODAL, 200);
            }};

    private final Animator closeAnim = new Animator(180, new org.swelement.core.Easing() {
        public float apply(float t) { return org.swelement.core.Easing.easeIn(t); }
    }, new Animator.Listener() {
        public void update(float v) { alpha = 1f - v; repaint(); }
    });

    private final Animator openAnim;
    private float alpha;
    private final JPanel content;
    private Component invoker;
    private Runnable dismissListener;
    private PopupLayer assignedLayer = PopupLayer.POPUP;
    private boolean dismissOnOutsideClick = true;
    private final AWTEventListener awtDismissListener = new AWTEventListener() {
        public void eventDispatched(AWTEvent event) {
            if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                if (!dismissOnOutsideClick) return;
                MouseEvent me = (MouseEvent) event;
                Component src = me.getComponent();
                if (AnimatedPopup.this.getParent() == null) return;
                if (AnimatedPopup.this.isAncestorOf(src)) return;
                if (invoker != null && (src == invoker || (invoker instanceof Container && ((Container)invoker).isAncestorOf(src)))) return;
                hidePopup();
                if (dismissListener != null) dismissListener.run();
            }
        }
    };

    public AnimatedPopup() {
        setOpaque(false);
        setLayout(new BorderLayout());
        content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int a = Math.round(255 * alpha);
                g2.setColor(new Color(255, 255, 255, a));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.setColor(new Color(228, 231, 237, a));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setLayout(new BorderLayout());
        add(content);
        openAnim = new Animator(200, Easing::easeOut, v -> {
            alpha = v;
            content.setBorder(new EmptyBorder(Math.round(8 * (1 - v)), 0, 0, 0));
            repaint();
        });
        Toolkit.getDefaultToolkit().addAWTEventListener(awtDismissListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    public JPanel getContent() { return content; }

    public void setDismissListener(Runnable r) { dismissListener = r; }

    /**
     * 是否在弹层外部按下时自动关闭。默认 true。
     * Toast 一类靠计时器自行消失的浮层应设为 false，否则任何一次点击都会把它打掉，
     * 且绕过 hideWithAnimation 的簿记回调，导致调用方的开启列表出现僵尸条目。
     */
    public void setDismissOnOutsideClick(boolean b) { dismissOnOutsideClick = b; }

    /**
     * 幂等地挂回全局按下监听。
     * hidePopup 会摘掉监听，若不在每次展示时重挂，弹层第二次展示后就再也无法点击外部关闭。
     * 先摘再挂，避免重复注册导致回调多次触发。
     */
    private void ensureDismissListenerRegistered() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        tk.removeAWTEventListener(awtDismissListener);
        tk.addAWTEventListener(awtDismissListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    @Override
    public void setVisible(boolean v) {
        if (!v) hidePopup();
    }

    public void show(Component invoker, int x, int y) {
        this.invoker = invoker;
        hidePopup();
        Window w = SwingUtilities.getWindowAncestor(invoker);
        if (!(w instanceof RootPaneContainer)) return;
        JLayeredPane lp = ((RootPaneContainer) w).getLayeredPane();
        Point p = SwingUtilities.convertPoint(invoker, x, y, lp);
        Dimension size = getPreferredSize();
        setBounds(p.x, p.y, size.width, size.height);
        alpha = 0f;
        content.setBorder(new EmptyBorder(8, 0, 0, 0));
        lp.add(this, JLayeredPane.POPUP_LAYER, 0);
        lp.repaint(p.x, p.y, size.width, size.height);
        ensureDismissListenerRegistered();
        openAnim.go(0f, 1f);
    }

    private void hidePopup() {
        if (getParent() == null) return;
        Container parent = getParent();
        Rectangle r = getBounds();
        parent.remove(this);
        parent.repaint(r.x, r.y, r.width, r.height);
        openAnim.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(awtDismissListener);
    }

    public void show(Component invoker, Direction dir) {
        this.invoker = invoker;
        hidePopup();
        Window w = SwingUtilities.getWindowAncestor(invoker);
        if (!(w instanceof RootPaneContainer)) return;
        PopupPositioner pp = new PopupPositioner(getPreferredSize(),
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds());
        Rectangle inv = new Rectangle(invoker.getLocationOnScreen(), invoker.getSize());
        PopupPositioner.Result r = pp.calc(inv, dir);
        JLayeredPane lp = ((RootPaneContainer) w).getLayeredPane();
        Point p = new Point(r.location);
        SwingUtilities.convertPointFromScreen(p, lp);
        setBounds(p.x, p.y, getPreferredSize().width, getPreferredSize().height);
        alpha = 0f;
        if (content != null) content.setBorder(new javax.swing.border.EmptyBorder(8, 0, 0, 0));
        lp.add(this, JLayeredPane.POPUP_LAYER, layerZ.get(assignedLayer));
        lp.repaint(p.x, p.y, getWidth(), getHeight());
        ensureDismissListenerRegistered();
        if (openAnim != null) openAnim.go(0f, 1f);
    }

    public void hideWithAnimation(final Runnable afterHidden) {
        if (getParent() == null) { if (afterHidden != null) afterHidden.run(); return; }
        globalStack.remove(this);
        openAnim.stop();
        closeAnim.stop();
        alpha = 1f;
        closeAnim.go(0f, 1f);
        final Container parent = getParent();
        final Rectangle r = getBounds();
        Timer t = new Timer(185, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((Timer)e.getSource()).stop();
                parent.remove(AnimatedPopup.this);
                parent.repaint(r.x, r.y, r.width, r.height);
                Toolkit.getDefaultToolkit().removeAWTEventListener(awtDismissListener);
                if (afterHidden != null) afterHidden.run();
            }
        });
        t.setRepeats(false);
        t.start();
    }

    public static void registerGlobal(AnimatedPopup p, PopupLayer layer) {
        p.assignedLayer = layer;
        globalStack.add(p);
    }
}
