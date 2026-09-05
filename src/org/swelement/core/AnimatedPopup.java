package org.swelement.core;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
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

    /**
     * 宿主/祖先滚动/缩放监听。
     * <p>
     * 浮层是一组相对宿主的绝对坐标，一旦宿主被移出原位、宿主所在 JScrollPane 滚动、
     * 宿主祖先容器发生改变，浮层就与宿主脱钩 — 企业表单只要塞进 JScrollPane 一滚就出问题。
     * 与 Element Plus 行为一致：发生上述事件时直接关闭弹层，而非重新计算。
     */
    private ComponentListener invokerMoveListener;
    private ChangeListener viewportChangeListener;
    private HierarchyListener invokerHierarchyListener;
    /** 这些监听挂在哪些对象上，hidePopup 时按引用精确摘除，避免误摘宿主原有监听 */
    private JViewport watchedViewport;
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
        installContextWatchers();
        openAnim.go(0f, 1f);
    }

    private void hidePopup() {
        if (getParent() == null) return;
        uninstallContextWatchers();
        Container parent = getParent();
        Rectangle r = getBounds();
        parent.remove(this);
        parent.repaint(r.x, r.y, r.width, r.height);
        openAnim.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(awtDismissListener);
    }

    /**
     * 挂上"宿主移动/缩放"和"祖先 JScrollPane 滚动"监听。
     * <p>
     * 触发时一律关闭弹层（与 Element Plus 行为一致），不做复杂重定位 —
     * 重定位在多层滚动/窗口缩放下极易出现抖动，且方向键导航时体验割裂。
     */
    private void installContextWatchers() {
        if (invoker == null) return;
        invokerMoveListener = new ComponentListener() {
            public void componentResized(ComponentEvent e) { dismissForContextChange(); }
            public void componentMoved(ComponentEvent e)   { dismissForContextChange(); }
            public void componentShown(ComponentEvent e)   { }
            public void componentHidden(ComponentEvent e)   { dismissForContextChange(); }
        };
        invoker.addComponentListener(invokerMoveListener);

        // 沿祖先链找到最近的 JScrollPane，挂 viewport ChangeListener
        JScrollPane sp = findAncestorScrollPane(invoker);
        if (sp != null) {
            JViewport vp = sp.getViewport();
            if (vp != null) {
                watchedViewport = vp;
                viewportChangeListener = new ChangeListener() {
                    public void stateChanged(javax.swing.event.ChangeEvent e) {
                        dismissForContextChange();
                    }
                };
                vp.addChangeListener(viewportChangeListener);
            }
        }

        // 祖先容器变化（如被重新布局）也可能拖动弹层
        invokerHierarchyListener = new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                long flags = HierarchyEvent.PARENT_CHANGED | HierarchyEvent.ANCESTOR_MOVED
                        | HierarchyEvent.ANCESTOR_RESIZED;
                if ((e.getChangeFlags() & flags) != 0) {
                    // 祖先变了，原 viewport 可能失效，不重挂监听，直接关闭
                    dismissForContextChange();
                }
            }
        };
        invoker.addHierarchyListener(invokerHierarchyListener);
    }

    private void dismissForContextChange() {
        if (getParent() == null) return;
        hidePopup();
        if (dismissListener != null) dismissListener.run();
    }

    private void uninstallContextWatchers() {
        if (invoker != null) {
            if (invokerMoveListener != null) {
                invoker.removeComponentListener(invokerMoveListener);
                invokerMoveListener = null;
            }
            if (invokerHierarchyListener != null) {
                invoker.removeHierarchyListener(invokerHierarchyListener);
                invokerHierarchyListener = null;
            }
        }
        if (watchedViewport != null && viewportChangeListener != null) {
            watchedViewport.removeChangeListener(viewportChangeListener);
            watchedViewport = null;
            viewportChangeListener = null;
        }
    }

    private static JScrollPane findAncestorScrollPane(Component c) {
        Container p = c.getParent();
        while (p != null) {
            if (p instanceof JScrollPane) return (JScrollPane) p;
            p = p.getParent();
        }
        return null;
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
        installContextWatchers();
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
                uninstallContextWatchers();
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

    // ---- self-check：弹层跟随上下文 —— 滚动 / 宿主移动 / 窗口缩放时自动关闭，且监听器无泄漏 ----
    static void selfCheck() {
        final JFrame[] frameHolder = {null};
        final JButton[] invokerHolder = {null};
        final AnimatedPopup[] popupHolder = {null};
        final JViewport[] vpHolder = {null};
        final boolean[] dismissed = {false};
        // 监听器基线：{invoker 的 ComponentListener, invoker 的 HierarchyListener, viewport 的 ChangeListener}
        final int[] baseline = {0, 0, 0};

        // Phase 1: 搭 UI + 首次 show
        onEDT(new Runnable() { public void run() {
            JFrame frame = new JFrame("AnimatedPopup self-check");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(300, 300);
            JPanel big = new JPanel(null);
            big.setPreferredSize(new Dimension(800, 800));
            JButton invoker = new JButton("invoker");
            invoker.setBounds(50, 50, 100, 30);
            big.add(invoker);
            JScrollPane scroll = new JScrollPane(big);
            frame.add(scroll, BorderLayout.CENTER);

            final AnimatedPopup popup = new AnimatedPopup();
            popup.getContent().add(new JLabel("content"));
            popup.setSize(120, 80);
            popup.setDismissListener(new Runnable() { public void run() { dismissed[0] = true; } });
            registerGlobal(popup, PopupLayer.POPUP);

            frame.setVisible(true);
            JViewport vp = scroll.getViewport();
            baseline[0] = invoker.getComponentListeners().length;
            baseline[1] = invoker.getHierarchyListeners().length;
            baseline[2] = vp.getChangeListeners().length;
            popup.show(invoker, 0, invoker.getHeight());
            assert popup.getParent() != null : "popup 未挂到 layered pane";

            frameHolder[0] = frame; invokerHolder[0] = invoker;
            popupHolder[0] = popup; vpHolder[0] = vp;
        }});

        // Phase 2: viewport 滚动（ChangeEvent 同步派发，本阶段内即可断言）
        onEDT(new Runnable() { public void run() {
            vpHolder[0].setViewPosition(new Point(0, 100));
            assert dismissed[0] : "viewport 滚动后弹层未自动关闭";
            assert popupHolder[0].getParent() == null : "viewport 滚动后弹层仍挂在 layered pane";
            assertNoWatcherLeak(invokerHolder[0], vpHolder[0], baseline, "viewport 滚动");
        }});

        // Phase 3: 再次 show（首次关闭时监听器应已卸载，这里重装一套全新的）
        onEDT(new Runnable() { public void run() {
            dismissed[0] = false;
            JButton invoker = invokerHolder[0];
            popupHolder[0].show(invoker, 0, invoker.getHeight());
            assert popupHolder[0].getParent() != null : "二次 show 未挂到 layered pane";
        }});

        // Phase 4: 移动宿主。COMPONENT_MOVED 经 EventQueue.postEvent 异步派发，
        // 本阶段只发事件不断言 —— 在 EDT 上同步等待注定看不到结果。
        onEDT(new Runnable() { public void run() {
            invokerHolder[0].setLocation(70, 70);
        }});

        // Phase 5: 上一阶段 post 的事件已先于本 runnable 派发完毕
        onEDT(new Runnable() { public void run() {
            assert dismissed[0] : "宿主移动后弹层未自动关闭";
            assert popupHolder[0].getParent() == null : "宿主移动后弹层仍挂在 layered pane";
            assertNoWatcherLeak(invokerHolder[0], vpHolder[0], baseline, "宿主移动");
        }});

        // Phase 6: 三次 show + 缩放窗口（ANCESTOR_RESIZED 层级事件同为异步 post）
        onEDT(new Runnable() { public void run() {
            dismissed[0] = false;
            JButton invoker = invokerHolder[0];
            popupHolder[0].show(invoker, 0, invoker.getHeight());
            frameHolder[0].setSize(400, 350);
        }});

        // Phase 7: 断言窗口缩放也触发了关闭，然后清理
        onEDT(new Runnable() { public void run() {
            assert dismissed[0] : "窗口缩放后弹层未自动关闭";
            assert popupHolder[0].getParent() == null : "窗口缩放后弹层仍挂在 layered pane";
            assertNoWatcherLeak(invokerHolder[0], vpHolder[0], baseline, "窗口缩放");
            AnimatedPopup p = popupHolder[0];
            p.openAnim.stop();
            p.closeAnim.stop();
            frameHolder[0].dispose();
        }});

        globalStack.remove(popupHolder[0]);
        System.out.println("AnimatedPopup self-check OK");
    }

    private static void assertNoWatcherLeak(JButton invoker, JViewport vp, int[] baseline, String where) {
        assert invoker.getComponentListeners().length == baseline[0]
                : where + "关闭后 ComponentListener 未卸载（泄漏）";
        assert invoker.getHierarchyListeners().length == baseline[1]
                : where + "关闭后 HierarchyListener 未卸载（泄漏）";
        assert vp.getChangeListeners().length == baseline[2]
                : where + "关闭后 viewport ChangeListener 未卸载（泄漏）";
    }

    private static void onEDT(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException(cause);
        }
    }

    public static void main(String[] args) { selfCheck(); }
}
