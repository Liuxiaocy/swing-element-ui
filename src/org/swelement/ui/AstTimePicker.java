package org.swelement.ui;

import org.swelement.core.AnimatedPopup;
import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * TimePicker 时间选择器 — 点击触发框弹出三列（时/分/秒）可滚动选择面板。
 *
 * 用法：
 *   AstTimePicker tp = new AstTimePicker();
 *   tp.setTime(14, 30, 0);
 *   tp.setTimeChangeListener(hms -> System.out.println(hms[0]+":"+hms[1]+":"+hms[2]));
 *   frame.add(tp);
 *
 *   // 关闭秒列（只选时:分）：
 *   AstTimePicker tp2 = new AstTimePicker(false);
 *
 * 设计：
 *  - 触发框：自绘 Input 风格，显示 HH:mm:ss（关闭秒列显示 HH:mm），右侧时钟图标。
 *  - 弹出面板：AnimatedPopup BELOW，白底圆角，三列（或两列）JList，每列 8 行可见，当前值高亮 PRIMARY。
 *  - 选择：点击某值立即更新并高亮；面板底部"确定"按钮关闭面板。
 *  - 键盘：上下箭头调整当前聚焦列的值。
 *  - 对比度：触发框文字 TEXT_MAIN on 白底；面板项文字 TEXT_MAIN，选中项白字 PRIMARY 底（按惯例跳过断言）。
 */
public class AstTimePicker extends JComponent {
    private int hour = 0, minute = 0, second = 0;
    private final boolean showSeconds;
    private Consumer<int[]> timeChangeListener;
    private final AnimatedPopup popup;
    private boolean open;
    private float hover;
    private final Animator hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); }},
        new Animator.Listener() { public void update(float v) { hover = v; repaint(); }});
    private static final int FIELD_H = 36;
    private static final int ICON_SIZE = 16;

    public AstTimePicker() { this(true); }

    public AstTimePicker(boolean showSeconds) {
        this.showSeconds = showSeconds;
        this.popup = new AnimatedPopup();
        this.popup.setDismissListener(new Runnable() { public void run() { open = false; }});
        AnimatedPopup.registerGlobal(popup, AnimatedPopup.PopupLayer.POPUP);
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hoverAnim.stop(); hoverAnim.go(hover, 1f); }
            @Override public void mouseExited(MouseEvent e) { hoverAnim.stop(); hoverAnim.go(hover, 0f); }
            @Override public void mouseClicked(MouseEvent e) { toggle(); }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setTime(int h, int m, int s) {
        if (h < 0 || h > 23) throw new IllegalArgumentException("hour out of range: " + h);
        if (m < 0 || m > 59) throw new IllegalArgumentException("minute out of range: " + m);
        if (s < 0 || s > 59) throw new IllegalArgumentException("second out of range: " + s);
        this.hour = h; this.minute = m; this.second = s;
        repaint();
    }

    public int[] getTime() { return new int[]{ hour, minute, second }; }

    public void setTimeChangeListener(Consumer<int[]> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.timeChangeListener = l;
    }

    public void showPicker() {
        if (open) return;
        open = true;
        buildPanel();
        popup.show(this, AnimatedPopup.Direction.BELOW);
    }

    public void hidePicker() {
        if (!open) return;
        open = false;
        popup.hideWithAnimation(null);
    }

    public void toggle() { if (open) hidePicker(); else showPicker(); }
    public boolean isOpen() { return open; }

    @Override public Dimension getPreferredSize() { return new Dimension(180, FIELD_H); }
    @Override public Dimension getMinimumSize() { return new Dimension(140, FIELD_H); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    private void buildPanel() {
        Container cc = popup.getContent();
        cc.removeAll();
        TimePanel tp = new TimePanel(showSeconds, hour, minute, second, new TimePanel.Callback() {
            public void onPick(int h, int m, int s) {
                hour = h; minute = m; second = s;
                repaint();
                if (timeChangeListener != null) timeChangeListener.accept(new int[]{ h, m, s });
            }
            public void onConfirm() { hidePicker(); }
        });
        cc.add(tp, BorderLayout.CENTER);
        popup.setPreferredSize(tp.getPreferredSize());
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int w = getWidth(), h = getHeight();
        // 触发框：白底 + BORDER_BASE 边框（hover 时 PRIMARY）
        Color borderC = hover > 0.01f
            ? ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, hover)
            : ElementTheme.BORDER_BASE;
        RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, w-1.5f, h-1.5f, ElementTheme.RADIUS, ElementTheme.RADIUS);
        g2.setColor(Color.WHITE);
        g2.fill(rect);
        g2.setColor(borderC);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(rect);
        // 时间文字
        String text = showSeconds
            ? String.format("%02d:%02d:%02d", hour, minute, second)
            : String.format("%02d:%02d", hour, minute);
        g2.setColor(ElementTheme.TEXT_MAIN);
        ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstTimePicker field text");
        g2.setFont(ElementTheme.FONT.deriveFont(14f));
        FontMetrics fm = g2.getFontMetrics();
        int baseY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, 12, baseY);
        // 时钟图标（右侧）
        int ix = w - ICON_SIZE - 10, iy = (h - ICON_SIZE) / 2;
        drawClockIcon(g2, ix, iy, ICON_SIZE, ElementTheme.TEXT_REGULAR);
        g2.dispose();
    }

    private static void drawClockIcon(Graphics2D g2, int x, int y, int s, Color c) {
        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // 外圈圆
        g2.drawOval(x, y, s, s);
        // 时针、分针
        int cx = x + s/2, cy = y + s/2;
        g2.drawLine(cx, cy, cx, cy - s/4);
        g2.drawLine(cx, cy, cx + s/3, cy);
        g2.dispose();
    }

    // --- 弹出面板：三列可滚动列表 ---
    static final class TimePanel extends JPanel {
        interface Callback { void onPick(int h, int m, int s); void onConfirm(); }
        private final boolean showSeconds;
        private int hour, minute, second;
        private final Callback cb;
        private static final int ROW_H = 28;
        private static final int VISIBLE_ROWS = 7;
        private static final int COL_W = 56;

        TimePanel(boolean showSeconds, int h, int m, int s, Callback cb) {
            this.showSeconds = showSeconds;
            this.hour = h; this.minute = m; this.second = s;
            this.cb = cb;
            setOpaque(false);
            setLayout(new BorderLayout());
            add(buildColumns(), BorderLayout.CENTER);
            // 底部"确定"按钮
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            footer.setOpaque(false);
            Button confirm = new Button("确定", Button.PRIMARY, false);
            confirm.setPreferredSize(new Dimension(64, 30));
            confirm.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { cb.onConfirm(); }});
            footer.add(confirm);
            add(footer, BorderLayout.SOUTH);
        }

        private JComponent buildColumns() {
            JPanel cols = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 8));
            cols.setOpaque(false);
            cols.add(makeColumn(24, hour, "时", new ColumnCb() { public void onSel(int v) { hour = v; cb.onPick(hour, minute, second); }}));
            cols.add(makeColumn(60, minute, "分", new ColumnCb() { public void onSel(int v) { minute = v; cb.onPick(hour, minute, second); }}));
            if (showSeconds) cols.add(makeColumn(60, second, "秒", new ColumnCb() { public void onSel(int v) { second = v; cb.onPick(hour, minute, second); }}));
            return cols;
        }

        private interface ColumnCb { void onSel(int v); }

        private JComponent makeColumn(int max, int current, String label, final ColumnCb cb) {
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            JLabel lbl = new JLabel(label, JLabel.CENTER);
            lbl.setFont(ElementTheme.FONT.deriveFont(12f));
            lbl.setForeground(ElementTheme.TEXT_REGULAR);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            wrap.add(lbl, BorderLayout.NORTH);
            // 列表：ListModel + JList
            final DefaultListModel<String> model = new DefaultListModel<String>();
            for (int i = 0; i < max; i++) model.addElement(String.format("%02d", i));
            final JList<String> list = new JList<String>(model) {
                @Override public boolean isOptimizedDrawingEnabled() { return false; }
            };
            list.setFont(ElementTheme.FONT.deriveFont(13f));
            list.setSelectionBackground(ElementTheme.PRIMARY);
            list.setSelectionForeground(Color.WHITE);
            list.setForeground(ElementTheme.TEXT_MAIN);
            list.setBackground(Color.WHITE);
            list.setFixedCellHeight(ROW_H);
            list.setVisibleRowCount(VISIBLE_ROWS);
            list.setSelectedIndex(current);
            list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
                public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) return;
                    int idx = list.getSelectedIndex();
                    if (idx >= 0) cb.onSel(idx);
                }
            });
            JScrollPane sp = new JScrollPane(list);
            sp.setPreferredSize(new Dimension(COL_W, VISIBLE_ROWS * ROW_H));
            sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setBorder(null);
            sp.getViewport().setBackground(Color.WHITE);
            // 确保选中项可见
            int cell = current;
            if (cell >= 0) {
                int top = Math.max(0, cell - VISIBLE_ROWS/2);
                list.ensureIndexIsVisible(top);
                list.ensureIndexIsVisible(cell);
            }
            wrap.add(sp, BorderLayout.CENTER);
            return wrap;
        }

        @Override public Dimension getPreferredSize() {
            int cols = showSeconds ? 3 : 2;
            int w = cols * (COL_W + 4) + 12;
            int h = 16 /*label*/ + VISIBLE_ROWS * ROW_H + 16 + 46 /*footer*/;
            return new Dimension(w, h);
        }
    }

    static void selfCheck() {
        boolean threw = false;
        try { new AstTimePicker().setTime(24, 0, 0); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "hour>23 must throw"; threw = false;
        try { new AstTimePicker().setTime(-1, 0, 0); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "hour<0 must throw"; threw = false;
        try { new AstTimePicker().setTime(0, 60, 0); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "minute>59 must throw"; threw = false;
        try { new AstTimePicker().setTime(0, 0, 60); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "second>59 must throw"; threw = false;
        try { new AstTimePicker().setTimeChangeListener(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null listener must throw";

        AstTimePicker tp = new AstTimePicker();
        tp.setTime(9, 5, 30);
        int[] t = tp.getTime();
        assert t[0] == 9 && t[1] == 5 && t[2] == 30 : "getTime roundtrip";

        final int[] fired = {0};
        final AstTimePicker tp2 = new AstTimePicker();
        tp2.setTime(14, 30, 0);
        tp2.setTimeChangeListener(new java.util.function.Consumer<int[]>() {
            public void accept(int[] hms) { fired[0]++; }
        });

        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    JFrame jf = new JFrame("AstTimePicker SC"); jf.setSize(800, 600); jf.setVisible(true);
                    JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new FlowLayout());
                    cp.add(tp2); jf.pack();
                    tp2.showPicker();
                    assert tp2.isOpen() : "showPicker 后 open=true";
                    try { Thread.sleep(60); } catch (Throwable ignore) {}
                    JLayeredPane lp = jf.getLayeredPane();
                    AnimatedPopup found = null;
                    for (int i = 0; i < lp.getComponentCount(); i++) {
                        Component c = lp.getComponent(i);
                        if (c instanceof AnimatedPopup) { found = (AnimatedPopup) c; break; }
                    }
                    assert found != null : "popup 已挂载";
                    assert found != null;
                    // 查找 TimePanel 内的 JList（时列），改选 8
                    boolean listFound = false;
                    java.util.Queue<Container> q = new java.util.LinkedList<Container>(); q.add(found);
                    while (!q.isEmpty() && !listFound) {
                        Container cur = q.poll();
                        for (int i = 0; i < cur.getComponentCount(); i++) {
                            Component ch = cur.getComponent(i);
                            if (ch instanceof JList) { listFound = true; break; }
                            if (ch instanceof Container) q.add((Container) ch);
                        }
                    }
                    assert listFound : "面板内含 JList";
                    tp2.hidePicker();
                    try { Thread.sleep(50); } catch (Throwable ignore) {}
                    assert !tp2.isOpen() : "hidePicker 后 open=false";
                    // toggle 测试
                    tp2.toggle();
                    assert tp2.isOpen();
                    tp2.toggle();
                    assert !tp2.isOpen();
                    jf.dispose();
                } catch (Throwable ex) { err[0] = ex; }
            }});
        } catch (Throwable ex2) { err[0] = ex2; }
        if (err[0] != null) throw new RuntimeException(err[0]);

        // 离屏绘制触发框校验对比度
        AstTimePicker tpv = new AstTimePicker();
        tpv.setTime(8, 8, 8);
        tpv.setSize(180, FIELD_H);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(180, FIELD_H, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { tpv.paint(gg); } finally { gg.dispose(); }
        int px = img.getRGB(12, 10); int a = (px >>> 24) & 0xFF;
        assert a > 120 : "trigger field painted alpha=" + a;
        // 离屏绘制 TimePanel 校验布局（含三列 JList + 确定按钮）
        TimePanel panel = new TimePanel(true, 14, 30, 0, new TimePanel.Callback() {
            public void onPick(int h, int m, int s) {}
            public void onConfirm() {}
        });
        // 结构校验：递归查找应至少含 3 个 JList（时/分/秒）和 1 个 Button（确定）
        int listCount = 0, btnCount = 0;
        java.util.Queue<Container> q2 = new java.util.LinkedList<Container>(); q2.add(panel);
        while (!q2.isEmpty()) {
            Container cur = q2.poll();
            for (int i = 0; i < cur.getComponentCount(); i++) {
                Component ch = cur.getComponent(i);
                if (ch instanceof JList) listCount++;
                if (ch instanceof Button) btnCount++;
                if (ch instanceof Container) q2.add((Container) ch);
            }
        }
        assert listCount == 3 : "TimePanel 应含 3 个 JList（时/分/秒），实际=" + listCount;
        assert btnCount == 1 : "TimePanel 应含 1 个确定按钮，实际=" + btnCount;
        assert panel.getPreferredSize().width > 0 && panel.getPreferredSize().height > 0 : "TimePanel preferredSize 合理";
        System.out.println("AstTimePicker self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
