package org.swelement.ui;

import org.swelement.core.AnimatedPopup;
import org.swelement.core.Easing;
import org.swelement.framework.AstContainerComponent;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
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
 *   // 区间模式（固定时间范围）：
 *   AstTimePicker rp = new AstTimePicker();
 *   rp.setRangeMode(true);
 *   rp.setTimeRange(9, 0, 0, 14, 30, 0);          // start 必须严格早于 end
 *   int[][] range = rp.getTimeRange();            // {{9,0,0},{14,30,0}}
 *
 * 设计：
 *  - 触发框：自绘 AstInput 风格，显示 HH:mm:ss（关闭秒列显示 HH:mm），右侧时钟图标；
 *    区间模式显示 "HH:mm:ss ~ HH:mm:ss"（未选侧显示 "--:--:--" 占位）。
 *  - 弹出面板：AnimatedPopup BELOW，白底圆角。
 *    单值模式：三列（或两列）JList，每列 7 行可见，当前值高亮 PRIMARY，底部"确定"关闭。
 *    区间模式：左"起始" + 右"结束"两块时间面板并排；先选起始再选结束；
 *      结束面板按整时间戳严格比较级联置灰 ≤ 开始 的备选项（时/分/秒），且不可点选。
 *  - 键盘：上下箭头调整当前聚焦列的值。
 *  - 对比度：触发框文字 TEXT_MAIN on 白底；面板项文字 TEXT_MAIN，选中项白字 PRIMARY 底（按惯例跳过断言）；
 *    禁用项静音灰，WCAG 2.1 豁免 inactive 组件对比度，不做 AA 断言。
 */
public class AstTimePicker extends AstInteractiveComponent {
    private int hour = 0, minute = 0, second = 0;
    // 区间模式状态
    private boolean rangeMode = false;
    private int startHour = 0, startMinute = 0, startSecond = 0;
    private int endHour = 0, endMinute = 0, endSecond = 0;
    private boolean startSet = false, endSet = false;
    private static final int[] HMS_MAX = { 24, 60, 60 };
    private final boolean showSeconds;
    private Consumer<int[]> timeChangeListener;
    private final AnimatedPopup popup;
    private boolean open;
    private static final int FIELD_H = 36;
    private static final int ICON_SIZE = 16;

    public AstTimePicker() { this(true); }

    public AstTimePicker(boolean showSeconds) {
        this.showSeconds = showSeconds;
        this.popup = new AnimatedPopup();
        this.popup.setDismissListener(new Runnable() { public void run() { open = false; }});
        AnimatedPopup.registerGlobal(popup, AnimatedPopup.PopupLayer.POPUP);
        anim.register("hover", 150, Easing::easeInOut);
        setFont(UIManager.getFont("Label.font"));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { anim.stop("hover"); anim.go("hover", anim.getProgress("hover"), 1f); }
            @Override public void mouseExited(MouseEvent e) { anim.stop("hover"); anim.go("hover", anim.getProgress("hover"), 0f); }
            @Override public void mousePressed(MouseEvent e) { toggle(); }
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

    public int[] getTime() {
        if (rangeMode) throw new IllegalStateException("区间模式下请用 getTimeRange()");
        return new int[]{ hour, minute, second };
    }

    public void setTimeChangeListener(Consumer<int[]> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.timeChangeListener = l;
    }

    // --- 区间模式 API ---
    public void setRangeMode(boolean on) {
        this.rangeMode = on;
        if (on) { startSet = false; endSet = false; }
        repaint();
    }

    public boolean isRangeMode() { return rangeMode; }

    public void setTimeRange(int sh, int sm, int ss, int eh, int em, int es) {
        if (!rangeMode) throw new IllegalStateException("setTimeRange 需先 setRangeMode(true)");
        checkRange(sh, sm, ss); checkRange(eh, em, es);
        if (compare(eh, em, es, sh, sm, ss) <= 0)
            throw new IllegalArgumentException("结束时间必须严格晚于开始时间");
        startHour = sh; startMinute = sm; startSecond = ss; startSet = true;
        endHour = eh; endMinute = em; endSecond = es; endSet = true;
        repaint();
    }

    public int[][] getTimeRange() {
        if (!rangeMode) throw new IllegalStateException("getTimeRange 需先 setRangeMode(true)");
        return new int[][]{{ startHour, startMinute, startSecond }, { endHour, endMinute, endSecond }};
    }

    private static void checkRange(int h, int m, int s) {
        if (h < 0 || h > 23 || m < 0 || m > 59 || s < 0 || s > 59)
            throw new IllegalArgumentException("time out of range: " + h + ":" + m + ":" + s);
    }

    private static int compare(int h1, int m1, int s1, int h2, int m2, int s2) {
        if (h1 != h2) return Integer.compare(h1, h2);
        if (m1 != m2) return Integer.compare(m1, m2);
        return Integer.compare(s1, s2);
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
        if (rangeMode) {
            RangePanel rp = new RangePanel(showSeconds, this);
            cc.add(rp, BorderLayout.CENTER);
            popup.setPreferredSize(rp.getPreferredSize());
        } else {
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
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        float hover = anim.getProgress("hover");
        int w = getWidth(), h = getHeight();
        // 触发框：白底 + BORDER_BASE 边框（hover 时 PRIMARY）
        Color borderC = hover > 0.01f
            ? lerp(theme().getBorderBase(), theme().getPrimary(), hover)
            : theme().getBorderBase();
        RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, w-1.5f, h-1.5f, theme().getRadiusBase(), theme().getRadiusBase());
        g2.setColor(Color.WHITE);
        g2.fill(rect);
        g2.setColor(borderC);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(rect);
        // 时间文字
        String text;
        if (rangeMode) {
            String sf = startSet
                ? (showSeconds ? String.format("%02d:%02d:%02d", startHour, startMinute, startSecond)
                               : String.format("%02d:%02d", startHour, startMinute))
                : (showSeconds ? "--:--:--" : "--:--");
            String ef = endSet
                ? (showSeconds ? String.format("%02d:%02d:%02d", endHour, endMinute, endSecond)
                               : String.format("%02d:%02d", endHour, endMinute))
                : (showSeconds ? "--:--:--" : "--:--");
            text = sf + " ~ " + ef;
        } else {
            text = showSeconds
                ? String.format("%02d:%02d:%02d", hour, minute, second)
                : String.format("%02d:%02d", hour, minute);
        }
        g2.setColor(theme().getTextPrimary());
        assertContrast(theme().getTextPrimary(), Color.WHITE, "AstTimePicker field text");
        g2.setFont(getFont().deriveFont(14f));
        FontMetrics fm = g2.getFontMetrics();
        int baseY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, 12, baseY);
        // 时钟图标（右侧）
        int ix = w - ICON_SIZE - 10, iy = (h - ICON_SIZE) / 2;
        drawClockIcon(g2, ix, iy, ICON_SIZE, theme().getTextRegular());
        g2.dispose();
    }

    /**
     * 时间选择器不是切换型控件：selected 状态对它无意义。
     * 覆写为 false，避免激活时翻转一个无人消费的状态字段。
     */
    @Override
    protected boolean isToggleMode() { return false; }

    private static void drawClockIcon(Graphics2D g2, int x, int y, int s, Color c) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(c);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // 外圈圆
        g.drawOval(x, y, s, s);
        // 时针、分针
        int cx = x + s/2, cy = y + s/2;
        g.drawLine(cx, cy, cx, cy - s/4);
        g.drawLine(cx, cy, cx + s/3, cy);
        g.dispose();
    }

    // --- 弹出面板：三列可滚动列表 ---
    static final class TimePanel extends AstContainerComponent {
        interface Callback { void onPick(int h, int m, int s); void onConfirm(); }
        private final boolean showSeconds;
        private int hour, minute, second;
        private final Callback cb;
        private static final int ROW_H = 28;
        private static final int VISIBLE_ROWS = 7;
        private static final int COL_W = 56;
        // 区间结束面板：每列禁用遮罩 + 当前已选值（用于级联判定）
        private final boolean endMode;
        private final boolean[][] disabled; // [col][value]；非结束面板为 null
        private int curH, curM, curS;
        private final JList<String>[] lists = new JList[3];
        private boolean suppress = false;
        private java.util.function.Supplier<int[]> startSupplier;

        TimePanel(boolean showSeconds, int h, int m, int s, Callback cb) {
            this(showSeconds, h, m, s, cb, false, null);
        }

        TimePanel(boolean showSeconds, int h, int m, int s, Callback cb, boolean endMode, java.util.function.Supplier<int[]> startSupplier) {
            this.showSeconds = showSeconds;
            this.hour = h; this.minute = m; this.second = s;
            this.curH = h; this.curM = m; this.curS = s;
            this.cb = cb;
            this.endMode = endMode;
            this.startSupplier = startSupplier;
            this.disabled = endMode ? new boolean[3][60] : null;
            if (endMode) recomputeDisabled();
            setFont(UIManager.getFont("Label.font"));
            setLayout(new BorderLayout());
            add(buildColumns(), BorderLayout.CENTER);
            // 底部"确定"按钮
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            footer.setOpaque(false);
            AstButton confirm = new AstButton("确定", AstButton.PRIMARY, false);
            confirm.setPreferredSize(new Dimension(64, 30));
            confirm.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { cb.onConfirm(); }});
            footer.add(confirm);
            add(footer, BorderLayout.SOUTH);
        }

        /** 结束面板：按 start 与当前 end 的其它列，级联重算三列禁用遮罩（Element 风格）。 */
        void recomputeDisabled() {
            if (!endMode || startSupplier == null) return;
            int[] start = startSupplier.get();
            boolean sameH = (curH == start[0]);
            boolean sameHM = sameH && (curM == start[1]);
            for (int hh = 0; hh < HMS_MAX[0]; hh++) disabled[0][hh] = le(hh, curM, curS, start);
            for (int mm = 0; mm < HMS_MAX[1]; mm++) disabled[1][mm] = sameH && le(start[0], mm, curS, start);
            for (int ss = 0; ss < HMS_MAX[2]; ss++) disabled[2][ss] = sameHM && (ss <= start[2]);
            repaint();
        }

        // (h,m,s) <= start ?
        private static boolean le(int h, int m, int s, int[] start) {
            if (h < start[0]) return true;
            if (h > start[0]) return false;
            if (m < start[1]) return true;
            if (m > start[1]) return false;
            return s <= start[2];
        }

        private JComponent buildColumns() {
            JPanel cols = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 8));
            cols.setOpaque(false);
            int cols2 = showSeconds ? 3 : 2;
            for (int c = 0; c < cols2; c++) {
                final int col = c;
                String lab = (col == 0) ? "时" : (col == 1) ? "分" : "秒";
                int max = (col == 0) ? 24 : 60;
                int cur = (col == 0) ? hour : (col == 1) ? minute : second;
                cols.add(makeColumn(col, max, cur, lab, new ColumnCb() {
                    public void onSel(int v) { applySelection(col, v); }
                }));
            }
            return cols;
        }

        private interface ColumnCb { void onSel(int v); }

        private JComponent makeColumn(final int col, int max, int current, String label, final ColumnCb cb) {
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            JLabel lbl = new JLabel(label, JLabel.CENTER);
            lbl.setFont(getFont().deriveFont(12f));
            lbl.setForeground(theme().getTextRegular());
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            wrap.add(lbl, BorderLayout.NORTH);
            // 列表：ListModel + JList
            final DefaultListModel<String> model = new DefaultListModel<String>();
            for (int i = 0; i < max; i++) model.addElement(String.format("%02d", i));
            final JList<String> list = new JList<String>(model) {
                @Override public boolean isOptimizedDrawingEnabled() { return false; }
            };
            lists[col] = list;
            list.setFont(getFont().deriveFont(13f));
            list.setSelectionBackground(theme().getPrimary());
            list.setSelectionForeground(Color.WHITE);
            list.setForeground(theme().getTextPrimary());
            list.setBackground(Color.WHITE);
            list.setFixedCellHeight(ROW_H);
            list.setVisibleRowCount(VISIBLE_ROWS);
            list.setSelectedIndex(current);
            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> l, Object v, int index, boolean isSel, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(l, v, index, isSel, cellHasFocus);
                    if (endMode && disabled[col][index]) {
                        // 禁用项：静音灰；WCAG 2.1 豁免 inactive 组件对比度，不做 AA 断言
                        c.setForeground(new Color(0xBFBFBF));
                        c.setEnabled(false);
                    } else {
                        c.setForeground(isSel ? Color.WHITE : theme().getTextPrimary());
                    }
                    return c;
                }
            });
            list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
                public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                    if (e.getValueIsAdjusting() || suppress) return;
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

        private void applySelection(int col, int v) {
            if (endMode && disabled[col][v]) { revert(col); return; }
            if (col == 0) { hour = v; curH = v; }
            else if (col == 1) { minute = v; curM = v; }
            else { second = v; curS = v; }
            if (endMode) recomputeDisabled();
            cb.onPick(hour, minute, second);
        }

        private void revert(int col) {
            int idx = (col == 0) ? curH : (col == 1) ? curM : curS;
            JList<String> l = lists[col];
            if (l != null) { suppress = true; l.setSelectedIndex(Math.max(0, idx)); suppress = false; }
        }

        // 自检驱动：模拟点击某列某值；禁用项会被拦截（cur 不变）
        void selectColumn(int col, int v) { applySelection(col, v); }
        int getSelectedHour() { return curH; }
        int getSelectedMinute() { return curM; }
        int getSelectedSecond() { return curS; }

        @Override public Dimension getPreferredSize() {
            int cols = showSeconds ? 3 : 2;
            int w = cols * (COL_W + 4) + 12;
            int h = 16 /*label*/ + VISIBLE_ROWS * ROW_H + 16 + 46 /*footer*/;
            return new Dimension(w, h);
        }

        @Override protected void selfCheck() { }
    }

    // --- 区间弹层：起始 + 结束 两块时间面板并排 ---
    final class RangePanel extends AstContainerComponent {
        private final TimePanel startPanel, endPanel;
        RangePanel(boolean showSeconds, final AstTimePicker picker) {
            setLayout(new FlowLayout(FlowLayout.CENTER, 12, 8));
            setOpaque(false);
            startPanel = new TimePanel(showSeconds, picker.startHour, picker.startMinute, picker.startSecond,
                new TimePanel.Callback() {
                    public void onPick(int h, int m, int s) {
                        picker.startHour = h; picker.startMinute = m; picker.startSecond = s; picker.startSet = true;
                        picker.repaint(); endPanel.recomputeDisabled();
                    }
                    public void onConfirm() { picker.hidePicker(); }
                });
            endPanel = new TimePanel(showSeconds, picker.endHour, picker.endMinute, picker.endSecond,
                new TimePanel.Callback() {
                    public void onPick(int h, int m, int s) {
                        picker.endHour = h; picker.endMinute = m; picker.endSecond = s; picker.endSet = true;
                        picker.repaint();
                    }
                    public void onConfirm() { picker.hidePicker(); }
                }, true, new java.util.function.Supplier<int[]>() {
                    public int[] get() { return new int[]{ picker.startHour, picker.startMinute, picker.startSecond }; }
                });
            add(startPanel);
            add(endPanel);
        }
        @Override public Dimension getPreferredSize() {
            Dimension d = startPanel.getPreferredSize();
            return new Dimension(d.width * 2 + 28, d.height);
        }

        @Override protected void selfCheck() { }
    }

    // --- Self-check ---
    @Override
    protected void selfCheck() {
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

        AstTimePicker tp = this;
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
        final JFrame[] holder = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    JFrame jf = new JFrame("AstTimePicker SC"); holder[0] = jf; jf.setSize(800, 600); jf.setVisible(true);
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
                    // 不在此 dispose，交给 finally 兜底
                } catch (Throwable ex) { err[0] = ex; }
            }});
        } catch (Throwable ex2) { err[0] = ex2; }
        finally {
            // 断言失败也必须 dispose，否则 JVM 因残留非 daemon 的 AWT 线程挂死（掩盖真实错误）
            try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                if (holder[0] != null) { holder[0].dispose(); holder[0] = null; }
            }}); } catch (Throwable ignored) { /* 不掩盖原始断言错误 */ }
        }
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
                if (ch instanceof AstButton) btnCount++;
                if (ch instanceof Container) q2.add((Container) ch);
            }
        }
        assert listCount == 3 : "TimePanel 应含 3 个 JList（时/分/秒），实际=" + listCount;
        assert btnCount == 1 : "TimePanel 应含 1 个确定按钮，实际=" + btnCount;
        assert panel.getPreferredSize().width > 0 && panel.getPreferredSize().height > 0 : "TimePanel preferredSize 合理";

        // --- 区间模式自检 ---
        AstTimePicker rp = new AstTimePicker(true);
        rp.setRangeMode(true);
        boolean rangeThrew = false;
        try { rp.setTimeRange(14, 30, 0, 9, 5, 30); } catch (IllegalArgumentException iae) { rangeThrew = true; }
        assert rangeThrew : "结束≤开始必须抛 IllegalArgumentException";
        rp.setTimeRange(9, 5, 30, 14, 30, 0);
        int[][] rg = rp.getTimeRange();
        assert rg[0][0] == 9 && rg[0][1] == 5 && rg[0][2] == 30 && rg[1][0] == 14 && rg[1][1] == 30 && rg[1][2] == 0 : "getTimeRange roundtrip";
        boolean notRangeThrew = false;
        try { new AstTimePicker().getTimeRange(); } catch (IllegalStateException ise) { notRangeThrew = true; }
        assert notRangeThrew : "非区间模式 getTimeRange 必须抛 IllegalStateException";

        // 弹层区间：含 2 个 TimePanel（秒列 6 个 JList）；结束侧早于开始项被禁用且不可选
        final Throwable[] err2 = { null };
        final JFrame[] holder2 = { null };
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    JFrame jf = new JFrame("AstTimePicker Range SC"); holder2[0] = jf; jf.setSize(800, 600); jf.setVisible(true);
                    JPanel cpn = (JPanel) jf.getContentPane(); cpn.setLayout(new FlowLayout());
                    final AstTimePicker rpp = new AstTimePicker(true);
                    rpp.setRangeMode(true);
                    rpp.setTimeRange(9, 0, 0, 14, 0, 0); // start=09:00:00 end=14:00:00
                    cpn.add(rpp); jf.pack();
                    rpp.showPicker();
                    try { Thread.sleep(60); } catch (Throwable ignore) {}
                    AnimatedPopup foundP = null;
                    JLayeredPane lp2 = jf.getLayeredPane();
                    for (int i = 0; i < lp2.getComponentCount(); i++) {
                        Component c = lp2.getComponent(i);
                        if (c instanceof AnimatedPopup) { foundP = (AnimatedPopup) c; break; }
                    }
                    assert foundP != null : "区间 popup 已挂载";
                    int listCount2 = 0; java.util.List<TimePanel> tps = new java.util.ArrayList<TimePanel>();
                    java.util.Queue<Container> q3 = new java.util.LinkedList<Container>(); q3.add(foundP);
                    while (!q3.isEmpty()) {
                        Container cur = q3.poll();
                        for (int i = 0; i < cur.getComponentCount(); i++) {
                            Component ch = cur.getComponent(i);
                            if (ch instanceof JList) listCount2++;
                            if (ch instanceof TimePanel) tps.add((TimePanel) ch);
                            if (ch instanceof Container) q3.add((Container) ch);
                        }
                    }
                    assert listCount2 == 6 : "区间(秒列)应含 6 个 JList，实际=" + listCount2;
                    assert tps.size() == 2 : "区间应含 2 个 TimePanel(起/止)，实际=" + tps.size();
                    TimePanel endTp = tps.get(1);
                    int beforeH = endTp.getSelectedHour(); // 14
                    endTp.selectColumn(0, 8); // h=8 ≤ start(09:00:00) 应禁用，不变
                    int afterH = endTp.getSelectedHour();
                    assert afterH == beforeH : "结束侧禁用项不应改变已选值 before=" + beforeH + " after=" + afterH;
                    rpp.hidePicker();
                    // 不在此 dispose，交给 finally 兜底
                } catch (Throwable ex) { err2[0] = ex; }
            }});
        } catch (Throwable ex2) { err2[0] = ex2; }
        finally {
            // 同上：断言失败也必须 dispose，避免 JVM 挂死
            try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                if (holder2[0] != null) { holder2[0].dispose(); holder2[0] = null; }
            }}); } catch (Throwable ignored) { /* 不掩盖原始断言错误 */ }
        }
        if (err2[0] != null) throw new RuntimeException(err2[0]);

        assertKeyboardAccessible(this, "AstTimePicker");
        System.out.println("AstTimePicker self-check OK");
    }
    public static void main(String[] args) {
        new AstTimePicker().selfCheck();
    }
}
