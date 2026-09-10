package org.swelement.ui;

import org.swelement.core.AnimatedPopup;
import org.swelement.framework.AstAbstractComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * 日期选择器 — Element UI DatePicker 的 Java 实现。
 * 点击输入框弹出日历卡片，支持月份切换、今日高亮、选中日期。
 *
 * 单值用法：
 *   AstDatePicker dp = new AstDatePicker();
 *   dp.setDate(LocalDate.of(2026, 8, 21));
 *   dp.setDateChangeListener(date -> System.out.println("选中: " + date));
 *
 * 区间模式（日期范围选择）：
 *   AstDatePicker rp = new AstDatePicker();
 *   rp.setRangeMode(true);
 *   rp.setDateRange(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 20));  // end<start 自动交换
 *   LocalDate[] range = rp.getDateRange();                                 // {start, end}
 *
 * 设计：
 *  - 触发框：AstButton 风格，显示 YYYY-MM-DD（区间模式显示 "start ~ end"，未选侧显示 placeholder）。
 *  - 弹层：单值模式一块日历面板；区间模式左右并排两块（左=起始月，右=起始月+1），各自独立翻月。
 *  - 区间选取：点击-点击——第一次点设 start，第二次点设 end（end<start 自动交换），两者都选后再点开启新一轮。
 *    联动高亮：start/end 实心 PRIMARY 底白字；两者之间的日期浅 PRIMARY 底纹（TEXT_PRIMARY 文字，做 AA 断言）；
 *    未选 end 时悬浮某天预览 [start, hover] 区间。禁用项/其他月份用静音灰（豁免对比度）。
 */
public class AstDatePicker extends AstAbstractComponent implements FormValueProvider, FormInvalidMarker {
    private LocalDate selectedDate;
    // 区间模式状态
    private boolean rangeMode = false;
    private LocalDate startDate, endDate;
    private boolean startSelected = false, endSelected = false;
    private LocalDate hoverDate = null;

    private String placeholder = "选择日期";
    private Consumer<LocalDate> dateChangeListener;
    private final AstButton invoker;
    private final AnimatedPopup popup;
    private final CalendarPanel calendarPanel;   // 单值模式面板
    private CalendarPanel leftPanel, rightPanel; // 区间模式左右面板（在 RangePanel 内创建）
    private boolean open;
    private boolean invalid = false;

    // --- 尺寸档位（对齐 Element UI，与 AstInput 一致）---
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    private int tier = SIZE_DEFAULT;

    private static final String[] WEEKDAYS = {"日", "一", "二", "三", "四", "五", "六"};
    private static final int CELL_W = 36;
    private static final int CELL_H = 32;
    private static final int CAL_W = CELL_W * 7 + 16; // 7 cols + padding
    private static final int HEADER_H = 40;
    private static final int WEEKDAY_H = 28;

    /** 默认构造为一个未选中的空日期选择器（表单场景下不应默认填充为今天）。 */
    public AstDatePicker() { this(null); }

    /**
     * @param initial 初始选中日期；传 null 表示未选中（空），常用于表单字段。
     */
    public AstDatePicker(LocalDate initial) {
        this.selectedDate = initial;
        this.invoker = new AstButton((initial == null ? placeholder : formatDate(initial)), AstButton.DEFAULT, false);
        invoker.setIcon(new AstIcon(AstIcon.Type.CALENDAR));
        this.popup = new AnimatedPopup();
        popup.setDismissListener(new Runnable() { public void run() { open = false; }});
        this.calendarPanel = new CalendarPanel(initial);
        AnimatedPopup.registerGlobal(popup, AnimatedPopup.PopupLayer.POPUP);
        this.invoker.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { toggle(); }});
        setLayout(new BorderLayout());
        add(invoker, BorderLayout.CENTER);
        applyTier();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
    }

    // ===================== 区间模式 API =====================
    public void setRangeMode(boolean on) {
        this.rangeMode = on;
        if (on) { startSelected = false; endSelected = false; startDate = null; endDate = null; hoverDate = null; }
        updateInvokerText();
        repaintPanels();
    }

    public boolean isRangeMode() { return rangeMode; }

    public void setDateRange(LocalDate start, LocalDate end) {
        if (!rangeMode) throw new IllegalStateException("setDateRange 需先 setRangeMode(true)");
        if (start == null || end == null) throw new IllegalArgumentException("start/end must not be null");
        LocalDate s = start, e = end;
        if (s.isAfter(e)) { LocalDate t = s; s = e; e = t; } // 自动交换保证 start<=end
        this.startDate = s; this.endDate = e; startSelected = true; endSelected = true; hoverDate = null;
        updateInvokerText();
        repaintPanels();
    }

    public LocalDate[] getDateRange() {
        if (!rangeMode) throw new IllegalStateException("getDateRange 需先 setRangeMode(true)");
        return new LocalDate[]{ startDate, endDate };
    }

    /** 行为级测试 seam：d 是否落在 [start,end] 区间内（含端点由 isEndpoint 单独判断）。 */
    boolean inRange(LocalDate d) {
        if (!rangeMode || !startSelected) return false;
        LocalDate s = startDate;
        LocalDate e = endSelected ? endDate : hoverDate;
        if (e == null) return false;
        LocalDate lo = s.isBefore(e) ? s : e;
        LocalDate hi = s.isAfter(e) ? s : e;
        return !d.isBefore(lo) && !d.isAfter(hi);
    }

    /** 行为级测试 seam：d 是否为区间端点（start 或 end）。 */
    boolean isEndpoint(LocalDate d) {
        if (!rangeMode) return false;
        if (startSelected && d.equals(startDate)) return true;
        if (endSelected && d.equals(endDate)) return true;
        return false;
    }

    // ===================== 单值 API =====================
    public void setDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("date must not be null");
        this.selectedDate = date;
        this.calendarPanel.viewMonth = date.withDayOfMonth(1);
        updateInvokerText();
        repaintPanels();
    }

    public LocalDate getDate() {
        if (rangeMode) throw new IllegalStateException("区间模式下请用 getDateRange()");
        return selectedDate;
    }

    public void clear() {
        if (rangeMode) { startSelected = false; endSelected = false; startDate = null; endDate = null; hoverDate = null; }
        else { this.selectedDate = null; }
        updateInvokerText();
        repaintPanels();
    }

    @Override public String getFormValue() {
        if (rangeMode) {
            if (!startSelected) return "";
            if (!endSelected) return formatDate(startDate) + "~";
            return formatDate(startDate) + "~" + formatDate(endDate);
        }
        return selectedDate == null ? "" : selectedDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
    }
    @Override public void setFormValue(String v) {
        if (v == null || v.isEmpty()) { clear(); return; }
        if (rangeMode) {
            String[] parts = v.split("~");
            try {
                LocalDate s = LocalDate.parse(parts[0]);
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    LocalDate e = LocalDate.parse(parts[1]);
                    setDateRange(s, e);
                } else {
                    startDate = s; startSelected = true; endSelected = false; endDate = null; hoverDate = null;
                    updateInvokerText(); repaintPanels();
                }
            } catch (Exception e) { clear(); }
            return;
        }
        try { setDate(LocalDate.parse(v)); } catch (Exception e) { clear(); }
    }
    @Override public void setInvalid(boolean inv) {
        this.invalid = inv;
        setBorder(inv ? BorderFactory.createLineBorder(theme().getDanger(), 1) : null);
        repaint();
    }

    /** 尺寸档位（对齐 Element UI）：触发框高度由 invoker(AstButton) 档位驱动，避免裁剪。 */
    public void setSize(int t) {
        if (t < SIZE_LARGE || t > SIZE_SMALL) throw new IllegalArgumentException("invalid size tier: " + t);
        this.tier = t;
        applyTier();
        revalidate();
        repaint();
    }

    private void applyTier() {
        invoker.setSize(tier);
    }

    public void setPlaceholder(String s) {
        if (s == null) throw new IllegalArgumentException("placeholder must not be null");
        this.placeholder = s;
        updateInvokerText();
    }

    public void setDateChangeListener(Consumer<LocalDate> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.dateChangeListener = l;
    }

    public void showDatePicker() {
        if (open) return;
        open = true;
        Container cc = popup.getContent();
        cc.removeAll();
        cc.setLayout(new BorderLayout());
        if (rangeMode) {
            RangePanel rp = new RangePanel();
            cc.add(rp, BorderLayout.CENTER);
            popup.setPreferredSize(new Dimension(CAL_W * 2 + 28, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16 + 8));
        } else {
            calendarPanel.updateView();
            cc.add(calendarPanel, BorderLayout.CENTER);
            popup.setPreferredSize(new Dimension(CAL_W, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16));
        }
        popup.show(this, AnimatedPopup.Direction.BELOW);
    }

    public void hideDatePicker() {
        if (!open) return;
        open = false;
        popup.hideWithAnimation(null);
    }

    public void toggle() { if (open) hideDatePicker(); else showDatePicker(); }
    public boolean isOpen() { return open; }

    private void updateInvokerText() {
        if (rangeMode) {
            String s = startSelected ? formatDate(startDate) : placeholder;
            String e = endSelected ? formatDate(endDate) : placeholder;
            invoker.setText(s + " ~ " + e);
        } else {
            invoker.setText(selectedDate != null ? formatDate(selectedDate) : placeholder);
        }
    }

    private static String formatDate(LocalDate d) {
        return String.format("%04d-%02d-%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    private LocalDate rangeStartMonth() {
        LocalDate base = startSelected ? startDate : (selectedDate != null ? selectedDate : LocalDate.now());
        return base.withDayOfMonth(1);
    }

    private void repaintPanels() {
        if (calendarPanel != null) calendarPanel.repaint();
        if (leftPanel != null) leftPanel.repaint();
        if (rightPanel != null) rightPanel.repaint();
    }

    private void repaintRangePanels() {
        if (leftPanel != null) leftPanel.repaint();
        if (rightPanel != null) rightPanel.repaint();
    }

    /** 点击-点击选取：单值模式直接设 selectedDate；区间模式按 start→end→新一轮 推进。 */
    void onDayClick(LocalDate clicked, CalendarPanel src) {
        if (!rangeMode) {
            selectedDate = clicked;
            calendarPanel.viewMonth = clicked.withDayOfMonth(1);
            updateInvokerText();
            if (dateChangeListener != null) dateChangeListener.accept(clicked);
            repaintPanels();
            hideDatePicker();
            return;
        }
        if (!startSelected) {
            startDate = clicked; startSelected = true; endSelected = false; endDate = null; hoverDate = null;
        } else if (!endSelected) {
            endDate = clicked; endSelected = true;
            if (startDate.isAfter(endDate)) { LocalDate t = startDate; startDate = endDate; endDate = t; } // 自动交换
            if (dateChangeListener != null) dateChangeListener.accept(startDate);
        } else {
            startDate = clicked; startSelected = true; endSelected = false; endDate = null; hoverDate = null;
        }
        updateInvokerText();
        repaintPanels();
        if (rangeMode && startSelected && endSelected) hideDatePicker();
    }

    @Override public Dimension getPreferredSize() { return invoker.getPreferredSize(); }
    @Override public Dimension getMinimumSize() { return invoker.getMinimumSize(); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    // --- Calendar Panel ---
    private final class CalendarPanel extends JPanel {
        LocalDate viewMonth;
        CalendarPanel(LocalDate initialMonth) {
            this.viewMonth = (initialMonth == null ? LocalDate.now() : initialMonth).withDayOfMonth(1);
            setOpaque(false);
            setLayout(new BorderLayout());
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { handleClick(e.getX(), e.getY()); }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    if (!AstDatePicker.this.rangeMode || !AstDatePicker.this.startSelected || AstDatePicker.this.endSelected) return;
                    LocalDate d = dateAt(e.getX(), e.getY());
                    if (d != null && !d.equals(AstDatePicker.this.hoverDate)) {
                        AstDatePicker.this.hoverDate = d;
                        AstDatePicker.this.repaintRangePanels();
                    }
                }
            });
        }

        void updateView() { repaint(); }

        @Override public Dimension getPreferredSize() {
            return new Dimension(CAL_W, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16);
        }

        private LocalDate dateAt(int x, int y) {
            if (y < HEADER_H) return null;
            int wky = HEADER_H;
            if (y >= wky + WEEKDAY_H && y < wky + WEEKDAY_H + 6 * CELL_H) {
                int col = (x - 8) / CELL_W;
                int row = (y - wky - WEEKDAY_H) / CELL_H;
                if (col >= 0 && col < 7 && row >= 0 && row < 6) {
                    LocalDate first = viewMonth.withDayOfMonth(1);
                    int firstDayOfWeek = first.getDayOfWeek().getValue() % 7; // Sunday=0
                    LocalDate gridStart = first.minusDays(firstDayOfWeek);
                    return gridStart.plusDays(row * 7 + col);
                }
            }
            return null;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int W = getWidth(), H = getHeight();
            Color bg = Color.WHITE;
            Color borderC = AstDatePicker.this.theme().getBorderBase();
            int r = AstDatePicker.this.theme().getRadiusBase() * 2;
            RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, W-1.5f, H-1.5f, r, r);
            g2.setColor(bg); g2.fill(rect);
            g2.setColor(borderC); g2.setStroke(new BasicStroke(1f)); g2.draw(rect);

            // Header
            int headerY = 0;
            String headerText = viewMonth.getYear() + " 年 " + viewMonth.getMonthValue() + " 月";
            g2.setColor(AstDatePicker.this.theme().getTextPrimary());
            AstDatePicker.this.assertContrast(AstDatePicker.this.theme().getTextPrimary(), Color.WHITE, "AstDatePicker header");
            g2.setFont(AstDatePicker.this.theme().getFontBase().deriveFont(Font.BOLD, 15f));
            FontMetrics hfm = g2.getFontMetrics();
            int hx = (W - hfm.stringWidth(headerText)) / 2;
            int hy = headerY + (HEADER_H - hfm.getHeight()) / 2 + hfm.getAscent();
            g2.drawString(headerText, hx, hy);

            g2.setFont(AstDatePicker.this.theme().getFontBase().deriveFont(Font.BOLD, 16f));
            FontMetrics afm = g2.getFontMetrics();
            g2.setColor(AstDatePicker.this.theme().getTextRegular());
            g2.drawString("‹", 16, headerY + (HEADER_H - afm.getHeight()) / 2 + afm.getAscent());
            g2.drawString("›", W - 16 - afm.stringWidth("›"), headerY + (HEADER_H - afm.getHeight()) / 2 + afm.getAscent());

            // Weekday row
            int wky = HEADER_H;
            g2.setFont(AstDatePicker.this.theme().getFontBase().deriveFont(Font.PLAIN, 12f));
            FontMetrics wfm = g2.getFontMetrics();
            for (int i = 0; i < 7; i++) {
                int cx = 8 + i * CELL_W + (CELL_W - wfm.stringWidth(WEEKDAYS[i])) / 2;
                int cy = wky + (WEEKDAY_H - wfm.getHeight()) / 2 + wfm.getAscent();
                g2.setColor(AstDatePicker.this.theme().getTextPlaceholder());
                g2.drawString(WEEKDAYS[i], cx, cy);
            }

            // Day grid
            LocalDate firstOfMonth = viewMonth.withDayOfMonth(1);
            int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
            LocalDate gridStart = firstOfMonth.minusDays(firstDayOfWeek);
            LocalDate today = LocalDate.now();

            // 区间高亮边界（含未选 end 时的 hover 预览）
            LocalDate rs = AstDatePicker.this.startSelected ? AstDatePicker.this.startDate : null;
            LocalDate re = AstDatePicker.this.rangeMode ? (AstDatePicker.this.endSelected ? AstDatePicker.this.endDate : AstDatePicker.this.hoverDate) : null;
            LocalDate bandLo = null, bandHi = null;
            if (AstDatePicker.this.rangeMode && rs != null && re != null) {
                bandLo = rs.isBefore(re) ? rs : re;
                bandHi = rs.isAfter(re) ? rs : re;
            }
            Color bandTint = lerp(Color.WHITE, AstDatePicker.this.theme().getPrimary(), 0.10f);

            g2.setFont(AstDatePicker.this.theme().getFontBase().deriveFont(Font.PLAIN, 14f));
            FontMetrics dfm = g2.getFontMetrics();
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    int idx = row * 7 + col;
                    LocalDate cellDate = gridStart.plusDays(idx);
                    int cellX = 8 + col * CELL_W;
                    int cellY = wky + WEEKDAY_H + row * CELL_H;
                    boolean isCurrentMonth = cellDate.getMonth() == viewMonth.getMonth() && cellDate.getYear() == viewMonth.getYear();
                    boolean isToday = cellDate.equals(today);
                    boolean isStart = AstDatePicker.this.rangeMode && AstDatePicker.this.startSelected && cellDate.equals(AstDatePicker.this.startDate);
                    boolean isEnd = AstDatePicker.this.rangeMode && AstDatePicker.this.endSelected && cellDate.equals(AstDatePicker.this.endDate);
                    boolean inBand = bandLo != null && !cellDate.isBefore(bandLo) && !cellDate.isAfter(bandHi);

                    if (isStart || isEnd) {
                        // 端点：实心 PRIMARY 底 + 白字（Element 标准，跳过对比度断言）
                        g2.setColor(AstDatePicker.this.theme().getPrimary());
                        g2.fill(new RoundRectangle2D.Float(cellX + 2, cellY + 2, CELL_W - 4, CELL_H - 4, 4, 4));
                        g2.setColor(Color.WHITE);
                    } else if (inBand) {
                        // 区间内部：浅 PRIMARY 底纹 + TEXT_PRIMARY（做 AA 断言，底纹近白应满足）
                        g2.setColor(bandTint);
                        g2.fill(new RoundRectangle2D.Float(cellX + 2, cellY + 2, CELL_W - 4, CELL_H - 4, 4, 4));
                        g2.setColor(AstDatePicker.this.theme().getTextPrimary());
                        AstDatePicker.this.assertContrast(AstDatePicker.this.theme().getTextPrimary(), bandTint, "AstDatePicker range band");
                    } else if (isToday) {
                        g2.setColor(Color.WHITE);
                        g2.fill(new RoundRectangle2D.Float(cellX + 2, cellY + 2, CELL_W - 4, CELL_H - 4, 4, 4));
                        g2.setColor(AstDatePicker.this.theme().getPrimary());
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.draw(new RoundRectangle2D.Float(cellX + 2.5f, cellY + 2.5f, CELL_W - 5, CELL_H - 5, 4, 4));
                        g2.setColor(AstDatePicker.this.theme().getPrimary());
                    } else if (isCurrentMonth) {
                        g2.setColor(Color.WHITE);
                        g2.fill(new RoundRectangle2D.Float(cellX + 2, cellY + 2, CELL_W - 4, CELL_H - 4, 4, 4));
                        g2.setColor(AstDatePicker.this.theme().getTextPrimary());
                        AstDatePicker.this.assertContrast(AstDatePicker.this.theme().getTextPrimary(), Color.WHITE, "AstDatePicker normal day");
                    } else {
                        g2.setColor(Color.WHITE);
                        g2.fill(new RoundRectangle2D.Float(cellX + 2, cellY + 2, CELL_W - 4, CELL_H - 4, 4, 4));
                        g2.setColor(AstDatePicker.this.theme().getTextPlaceholder());
                    }
                    String dayStr = String.valueOf(cellDate.getDayOfMonth());
                    int dx = cellX + (CELL_W - dfm.stringWidth(dayStr)) / 2;
                    int dy = cellY + (CELL_H - dfm.getHeight()) / 2 + dfm.getAscent();
                    g2.drawString(dayStr, dx, dy);
                }
            }

            // Footer: "今天" button area
            int footY = wky + WEEKDAY_H + 6 * CELL_H + 4;
            g2.setColor(AstDatePicker.this.theme().getTextRegular());
            g2.setFont(AstDatePicker.this.theme().getFontBase().deriveFont(Font.PLAIN, 13f));
            FontMetrics ffm = g2.getFontMetrics();
            String footText = "点击今天: " + formatDate(today);
            int fx = (W - ffm.stringWidth(footText)) / 2;
            int fy = footY + ffm.getAscent();
            g2.drawString(footText, fx, fy);

            g2.dispose();
        }

        @Override public boolean isOptimizedDrawingEnabled() { return false; }

        @Override public boolean contains(int x, int y) {
            return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
        }

        private void handleClick(int x, int y) {
            int W = getWidth();
            if (y < HEADER_H) {
                if (x < 40) { viewMonth = viewMonth.minusMonths(1); repaint(); return; }
                if (x > W - 40) { viewMonth = viewMonth.plusMonths(1); repaint(); return; }
                return;
            }
            LocalDate d = dateAt(x, y);
            if (d != null) { AstDatePicker.this.onDayClick(d, this); return; }
            int wky = HEADER_H;
            int footY = wky + WEEKDAY_H + 6 * CELL_H + 4;
            if (y >= footY && y < footY + 24) { AstDatePicker.this.onDayClick(LocalDate.now(), this); }
        }
    }

    // --- 区间弹层：左右两块日历面板 ---
    private final class RangePanel extends JPanel {
        RangePanel() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 12, 8));
            setOpaque(false);
            leftPanel = new CalendarPanel(rangeStartMonth());
            rightPanel = new CalendarPanel(rangeStartMonth().plusMonths(1));
            add(leftPanel);
            add(rightPanel);
        }
        @Override public Dimension getPreferredSize() {
            return new Dimension(CAL_W * 2 + 28, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16 + 8);
        }
    }

    // --- Self-check ---
    @Override
    protected void selfCheck() {
        // Constructor: null initial → empty (no selection) picker, not an error
        AstDatePicker empty = new AstDatePicker(null);
        assert empty.getFormValue().isEmpty() : "null initial → empty picker";
        boolean threw = false;
        AstDatePicker dp0 = new AstDatePicker();
        try { dp0.setDate(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null setDate";
        threw = false;
        try { dp0.setPlaceholder(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null placeholder";
        threw = false;
        try { dp0.setDateChangeListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null listener";

        // ===== 单值模式功能测试 =====
        final Throwable[] err = {null};
        final LocalDate[] picked = new LocalDate[1];
        final JFrame[] holder = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    JFrame jf = new JFrame("DatePicker SC"); holder[0] = jf; jf.setSize(800, 600); jf.setVisible(true);
                    LocalDate init = LocalDate.of(2026, 8, 21);
                    AstDatePicker dp = new AstDatePicker(init);
                    dp.setDateChangeListener(date -> picked[0] = date);
                    JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new FlowLayout());
                    cp.add(dp); jf.pack();
                    assert dp.getDate().equals(init) : "initial date";
                    dp.showDatePicker();
                    assert dp.isOpen() : "picker open";
                    JLayeredPane lp = jf.getLayeredPane();
                    AnimatedPopup popup = null;
                    for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { popup = (AnimatedPopup) lp.getComponent(i); break; }
                    assert popup != null : "popup found";
                    Component calPanel = null;
                    for (int i = 0; i < popup.getComponentCount(); i++) {
                        Component c = popup.getComponent(i);
                        if (c instanceof JPanel) { calPanel = c; break; }
                    }
                    if (calPanel == null) {
                        Container cc = popup.getContent();
                        for (int i = 0; i < cc.getComponentCount(); i++) if (cc.getComponent(i) instanceof JPanel) { calPanel = cc.getComponent(i); break; }
                    }
                    assert calPanel != null : "calendar panel found";
                    calPanel.setSize(CAL_W, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16);
                    java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(CAL_W, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    Graphics2D gg = img.createGraphics();
                    try { calPanel.paint(gg); } finally { gg.dispose(); }
                    int headerPx = img.getRGB(CAL_W / 2, 20);
                    int ha = (headerPx >>> 24) & 0xFF;
                    assert ha > 100 : "header rendered; alpha=" + ha;
                    int dayPx = img.getRGB(8 + 3 * CELL_W + CELL_W / 2, HEADER_H + WEEKDAY_H + 2 * CELL_H + CELL_H / 2);
                    int da = (dayPx >>> 24) & 0xFF;
                    assert da > 100 : "day cell rendered; alpha=" + da;
                    // 移入(contains)不得选中；点击(handleClick)才选中
                    LocalDate beforeRe = dp.getDate();
                    boolean hitRe = dp.calendarPanel.contains(134, 148);
                    assert hitRe : "day cell hit";
                    assert dp.getDate().equals(beforeRe) : "hover(contains) must NOT select; got " + dp.getDate();
                    dp.calendarPanel.handleClick(134, 148);
                    assert dp.getDate().equals(LocalDate.of(2026, 8, 12)) : "click(handleClick) should select 2026-08-12; got " + dp.getDate();
                    dp.hideDatePicker();
                } catch (Throwable t) { err[0] = t; }
            }});
        } catch (Throwable t2) { err[0] = t2; }
        finally {
            try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                if (holder[0] != null) { holder[0].dispose(); holder[0] = null; }
            }}); } catch (Throwable ignored) {}
        }
        if (err[0] != null) throw new RuntimeException(err[0]);

        // FormValueProvider 取值契约（单值）
        AstDatePicker dpTest = new AstDatePicker();
        assert dpTest.getFormValue().isEmpty() : "DatePicker empty when no date";
        dpTest.setDate(LocalDate.of(2026, 8, 24));
        assert dpTest.getFormValue().equals("2026-08-24") : "DatePicker getFormValue, got " + dpTest.getFormValue();
        dpTest.setFormValue("");
        assert dpTest.getFormValue().isEmpty() : "DatePicker clear";

        // 尺寸档位（LARGE>DEFAULT>SMALL）
        AstDatePicker dpSz = new AstDatePicker(LocalDate.of(2026, 8, 24));
        int hDef = dpSz.getPreferredSize().height;
        dpSz.setSize(AstDatePicker.SIZE_LARGE);
        int hL = dpSz.getPreferredSize().height;
        dpSz.setSize(AstDatePicker.SIZE_SMALL);
        int hS = dpSz.getPreferredSize().height;
        assert hL > hDef && hDef > hS : "DatePicker tier height monotonic LARGE>DEFAULT>SMALL, got " + hL + "/" + hDef + "/" + hS;
        threw = false;
        try { dpSz.setSize(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "DatePicker invalid tier throws";

        // ===== 区间模式测试 =====
        AstDatePicker rdp = new AstDatePicker();
        rdp.setRangeMode(true);
        threw = false;
        try { rdp.setDateRange(null, LocalDate.now()); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "区间 setDateRange(null,...) 必须抛 IllegalArgumentException";
        rdp.setDateRange(LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 5)); // end<start → 自动交换
        LocalDate[] rng = rdp.getDateRange();
        assert rng[0].equals(LocalDate.of(2026, 8, 5)) && rng[1].equals(LocalDate.of(2026, 8, 20)) : "区间 end<start 自动交换, got " + rng[0] + "~" + rng[1];
        // 区间 form value
        assert rdp.getFormValue().equals("2026-08-05~2026-08-20") : "区间 getFormValue, got " + rdp.getFormValue();
        rdp.setFormValue("2026-08-01~2026-08-10");
        LocalDate[] rng2 = rdp.getDateRange();
        assert rng2[0].equals(LocalDate.of(2026, 8, 1)) && rng2[1].equals(LocalDate.of(2026, 8, 10)) : "区间 setFormValue roundtrip";
        // 非区间模式 getDateRange 抛异常
        threw = false;
        try { new AstDatePicker().getDateRange(); } catch (IllegalStateException e) { threw = true; }
        assert threw : "非区间模式 getDateRange 必须抛 IllegalStateException";
        // 行为级：点击-点击 + 区间/端点判定
        AstDatePicker cpk = new AstDatePicker();
        cpk.setRangeMode(true);
        cpk.onDayClick(LocalDate.of(2026, 8, 5), null);
        assert cpk.startSelected && cpk.getStartDate().equals(LocalDate.of(2026, 8, 5)) && !cpk.endSelected : "第一次点击设 start";
        cpk.onDayClick(LocalDate.of(2026, 8, 20), null);
        assert cpk.endSelected && cpk.getStartDate().equals(LocalDate.of(2026, 8, 5)) && cpk.getEndDate().equals(LocalDate.of(2026, 8, 20)) : "第二次点击设 end";
        assert cpk.inRange(LocalDate.of(2026, 8, 12)) : "区间内日期应 inRange";
        assert !cpk.inRange(LocalDate.of(2026, 8, 1)) : "区间前日期不应 inRange";
        assert cpk.isEndpoint(LocalDate.of(2026, 8, 5)) && cpk.isEndpoint(LocalDate.of(2026, 8, 20)) : "端点判定";
        // 第三次点击开启新一轮
        cpk.onDayClick(LocalDate.of(2026, 9, 1), null);
        assert cpk.startSelected && !cpk.endSelected && cpk.getStartDate().equals(LocalDate.of(2026, 9, 1)) : "第三轮重置 start";

        // 区间弹层：含 2 个 CalendarPanel
        final Throwable[] err3 = {null};
        final JFrame[] holder3 = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    JFrame jf = new JFrame("DatePicker Range SC"); holder3[0] = jf; jf.setSize(900, 600); jf.setVisible(true);
                    AstDatePicker rpp = new AstDatePicker();
                    rpp.setRangeMode(true);
                    rpp.setDateRange(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 20));
                    JPanel cpn = (JPanel) jf.getContentPane(); cpn.setLayout(new FlowLayout());
                    cpn.add(rpp); jf.pack();
                    rpp.showDatePicker();
                    try { Thread.sleep(60); } catch (Throwable ignore) {}
                    JLayeredPane lp3 = jf.getLayeredPane();
                    AnimatedPopup popup3 = null;
                    for (int i = 0; i < lp3.getComponentCount(); i++) if (lp3.getComponent(i) instanceof AnimatedPopup) { popup3 = (AnimatedPopup) lp3.getComponent(i); break; }
                    assert popup3 != null : "区间 popup 已挂载";
                    int calCount = 0;
                    java.util.Queue<Container> q = new java.util.LinkedList<Container>(); q.add(popup3);
                    while (!q.isEmpty()) {
                        Container cur = q.poll();
                        for (int i = 0; i < cur.getComponentCount(); i++) {
                            Component ch = cur.getComponent(i);
                            if (ch instanceof CalendarPanel) calCount++;
                            if (ch instanceof Container) q.add((Container) ch);
                        }
                    }
                    assert calCount == 2 : "区间弹层应含 2 个 CalendarPanel，实际=" + calCount;
                    // 离屏绘制左面板（含区间高亮），确认不抛异常且表头已绘制
                    rpp.leftPanel.setSize(CAL_W, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16);
                    java.awt.image.BufferedImage img3 = new java.awt.image.BufferedImage(CAL_W, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    Graphics2D gg3 = img3.createGraphics();
                    try { rpp.leftPanel.paint(gg3); } finally { gg3.dispose(); }
                    int ph = (img3.getRGB(CAL_W / 2, 20) >>> 24) & 0xFF;
                    assert ph > 100 : "区间左面板表头已绘制; alpha=" + ph;
                    rpp.hideDatePicker();
                } catch (Throwable t) { err3[0] = t; }
            }});
        } catch (Throwable t2) { err3[0] = t2; }
        finally {
            try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                if (holder3[0] != null) { holder3[0].dispose(); holder3[0] = null; }
            }}); } catch (Throwable ignored) {}
        }
        if (err3[0] != null) throw new RuntimeException(err3[0]);

        System.out.println("AstDatePicker self-check OK");
    }

    // 供 selfCheck 访问端点/区间判定（包内可见）
    LocalDate getStartDate() { return startDate; }
    LocalDate getEndDate() { return endDate; }

    public static void main(String[] args) {
        new AstDatePicker().selfCheck();
    }
}
