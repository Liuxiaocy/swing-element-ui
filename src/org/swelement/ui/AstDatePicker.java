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
 * 用法：
 *   AstDatePicker dp = new AstDatePicker();
 *   dp.setDate(LocalDate.of(2026, 8, 21));
 *   dp.setDateChangeListener(date -> System.out.println("选中: " + date));
 */
public class AstDatePicker extends AstAbstractComponent implements FormValueProvider, FormInvalidMarker {
    private LocalDate selectedDate;
    private LocalDate viewMonth;    // 当前显示的月份 (1st of month)
    private String placeholder = "选择日期";
    private Consumer<LocalDate> dateChangeListener;
    private final AstButton invoker;
    private final AnimatedPopup popup;
    private final CalendarPanel calendarPanel;
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
        this.viewMonth = (initial == null ? LocalDate.now() : initial).withDayOfMonth(1);
        this.invoker = new AstButton((initial == null ? placeholder : formatDate(initial)), AstButton.DEFAULT, false);
        invoker.setIcon(new AstIcon(AstIcon.Type.CALENDAR));
        this.popup = new AnimatedPopup();
        popup.setDismissListener(new Runnable() { public void run() { open = false; }});
        this.calendarPanel = new CalendarPanel();
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

    public void setDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("date must not be null");
        this.selectedDate = date;
        this.viewMonth = date.withDayOfMonth(1);
        updateInvokerText();
        if (calendarPanel != null) calendarPanel.repaint();
    }

    public LocalDate getDate() { return selectedDate; }

    public void clear() {
        this.selectedDate = null;
        updateInvokerText();
        if (calendarPanel != null) calendarPanel.repaint();
    }

    @Override public String getFormValue() {
        return getDate() == null ? "" : getDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
    }
    @Override public void setFormValue(String v) {
        if (v == null || v.isEmpty()) { clear(); return; }
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
        calendarPanel.updateView();
        Container cc = popup.getContent();
        cc.removeAll();
        cc.setLayout(new BorderLayout());
        cc.add(calendarPanel, BorderLayout.CENTER);
        popup.setPreferredSize(new Dimension(CAL_W, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16));
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
        if (selectedDate != null) invoker.setText(formatDate(selectedDate));
        else invoker.setText(placeholder);
    }

    private static String formatDate(LocalDate d) {
        return String.format("%04d-%02d-%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    @Override public Dimension getPreferredSize() { return invoker.getPreferredSize(); }
    @Override public Dimension getMinimumSize() { return invoker.getMinimumSize(); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    // --- Calendar Panel ---
    private final class CalendarPanel extends JPanel {
        CalendarPanel() {
            setOpaque(false);
            setLayout(new BorderLayout());
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    handleClick(e.getX(), e.getY());
                }
            });
        }

        void updateView() { repaint(); }

        @Override public Dimension getPreferredSize() {
            return new Dimension(CAL_W, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int W = getWidth(), H = getHeight();
            // Card background: white with border
            Color bg = Color.WHITE;
            Color borderC = AstDatePicker.this.theme().getBorderBase();
            int r = AstDatePicker.this.theme().getRadiusBase() * 2;
            RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, W-1.5f, H-1.5f, r, r);
            g2.setColor(bg); g2.fill(rect);
            g2.setColor(borderC); g2.setStroke(new BasicStroke(1f)); g2.draw(rect);

            // Header: year-month centered + left arrow + right arrow
            int headerY = 0;
            String headerText = viewMonth.getYear() + " 年 " + viewMonth.getMonthValue() + " 月";
            g2.setColor(AstDatePicker.this.theme().getTextPrimary());
            AstDatePicker.this.assertContrast(AstDatePicker.this.theme().getTextPrimary(), Color.WHITE, "AstDatePicker header");
            g2.setFont(AstDatePicker.this.theme().getFontBase().deriveFont(Font.BOLD, 15f));
            FontMetrics hfm = g2.getFontMetrics();
            int hx = (W - hfm.stringWidth(headerText)) / 2;
            int hy = headerY + (HEADER_H - hfm.getHeight()) / 2 + hfm.getAscent();
            g2.drawString(headerText, hx, hy);

            // Left/Right arrows: draw clickable regions
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

            // Day grid: 6 rows × 7 cols (42 cells, covering 6 weeks)
            LocalDate firstOfMonth = viewMonth.withDayOfMonth(1);
            // Java DayOfWeek: MONDAY=1..SUNDAY=7; our grid starts Sunday
            int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // Sunday=0
            LocalDate gridStart = firstOfMonth.minusDays(firstDayOfWeek);
            LocalDate today = LocalDate.now();

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
                    boolean isSelected = cellDate.equals(selectedDate);

                    if (isSelected) {
                        // Selected: PRIMARY bg + white text (Element UI standard, skip assertContrast — same as Button)
                        g2.setColor(AstDatePicker.this.theme().getPrimary());
                        g2.fill(new RoundRectangle2D.Float(cellX + 2, cellY + 2, CELL_W - 4, CELL_H - 4, 4, 4));
                        g2.setColor(Color.WHITE);
                    } else if (isToday) {
                        // Today: PRIMARY ring border + PRIMARY text
                        g2.setColor(Color.WHITE);
                        g2.fill(new RoundRectangle2D.Float(cellX + 2, cellY + 2, CELL_W - 4, CELL_H - 4, 4, 4));
                        g2.setColor(AstDatePicker.this.theme().getPrimary());
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.draw(new RoundRectangle2D.Float(cellX + 2.5f, cellY + 2.5f, CELL_W - 5, CELL_H - 5, 4, 4));
                        g2.setColor(AstDatePicker.this.theme().getPrimary());
                    } else if (isCurrentMonth) {
                        // Normal day: TEXT_MAIN on WHITE
                        g2.setColor(Color.WHITE);
                        g2.fill(new RoundRectangle2D.Float(cellX + 2, cellY + 2, CELL_W - 4, CELL_H - 4, 4, 4));
                        g2.setColor(AstDatePicker.this.theme().getTextPrimary());
                        AstDatePicker.this.assertContrast(AstDatePicker.this.theme().getTextPrimary(), Color.WHITE, "AstDatePicker normal day");
                    } else {
                        // Other month: TEXT_PLACEHOLDER on WHITE (light gray, skip assertContrast)
                        g2.setColor(Color.WHITE);
                        g2.fill(new RoundRectangle2D.Float(cellX + 2, cellY + 2, CELL_W - 4, CELL_H - 4, 4, 4));
                        g2.setColor(AstDatePicker.this.theme().getTextPlaceholder());
                    }
                    // Draw day number centered
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

        /**
         * 命中检测：必须是纯判定，不能有任何副作用。
         * Swing 在鼠标移动/进入时通过 findComponentAt → contains 逐层探测，
         * 若在此执行业务逻辑，会令鼠标"移入"即触发动作（见 handleClick）。
         */
        @Override public boolean contains(int x, int y) {
            return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
        }

        /** 鼠标真正点击后处理：选中日期 / 翻月 / 今天。移入不会触达此处。 */
        private void handleClick(int x, int y) {
            int W = getWidth();
            // Header arrows
            if (y < HEADER_H) {
                if (x < 40) { // Left arrow
                    viewMonth = viewMonth.minusMonths(1);
                    repaint();
                    return;
                }
                if (x > W - 40) { // Right arrow
                    viewMonth = viewMonth.plusMonths(1);
                    repaint();
                    return;
                }
                return; // header click, no action
            }
            // Day grid
            int wky = HEADER_H;
            if (y >= wky + WEEKDAY_H && y < wky + WEEKDAY_H + 6 * CELL_H) {
                int col = (x - 8) / CELL_W;
                int row = (y - wky - WEEKDAY_H) / CELL_H;
                if (col >= 0 && col < 7 && row >= 0 && row < 6) {
                    LocalDate firstOfMonth = viewMonth.withDayOfMonth(1);
                    int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
                    LocalDate gridStartDate = firstOfMonth.minusDays(firstDayOfWeek);
                    LocalDate clicked = gridStartDate.plusDays(row * 7 + col);
                    // 选中点击的日期
                    selectedDate = clicked;
                    if (clicked.getMonth() != viewMonth.getMonth() || clicked.getYear() != viewMonth.getYear()) {
                        viewMonth = clicked.withDayOfMonth(1); // navigate to clicked month
                    }
                    updateInvokerText();
                    if (dateChangeListener != null) dateChangeListener.accept(clicked);
                    repaint();
                    hideDatePicker();
                    return;
                }
            }
            // Footer "今天" click
            int footY = wky + WEEKDAY_H + 6 * CELL_H + 4;
            if (y >= footY && y < footY + 24) {
                selectedDate = LocalDate.now();
                viewMonth = selectedDate.withDayOfMonth(1);
                updateInvokerText();
                if (dateChangeListener != null) dateChangeListener.accept(selectedDate);
                repaint();
                hideDatePicker();
            }
        }
    }

    // --- Self-check ---
    @Override
    protected void selfCheck() {
        // Constructor: null initial → empty (no selection) picker, not an error
        AstDatePicker empty = new AstDatePicker(null);
        assert empty.getFormValue().isEmpty() : "null initial → empty picker";
        // setDate null guard
        boolean threw = false;
        AstDatePicker dp0 = new AstDatePicker();
        threw = false;
        try { dp0.setDate(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null setDate";
        // setPlaceholder null guard
        threw = false;
        try { dp0.setPlaceholder(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null placeholder";
        // setDateChangeListener null guard
        threw = false;
        try { dp0.setDateChangeListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null listener";

        // Functional test
        final Throwable[] err = {null};
        final LocalDate[] picked = new LocalDate[1];
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("DatePicker SC"); jf.setSize(800, 600); jf.setVisible(true);
            LocalDate init = LocalDate.of(2026, 8, 21);
            AstDatePicker dp = new AstDatePicker(init);
            dp.setDateChangeListener(date -> picked[0] = date);
            JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new FlowLayout());
            cp.add(dp); jf.pack();
            assert dp.getDate().equals(init) : "initial date";
            // Open picker
            dp.showDatePicker();
            assert dp.isOpen() : "picker open";
            // Find popup
            JLayeredPane lp = jf.getLayeredPane();
            AnimatedPopup popup = null;
            for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { popup = (AnimatedPopup) lp.getComponent(i); break; }
            assert popup != null : "popup found";
            // Paint calendar off-screen to trigger assertContrast for normal day
            Component calPanel = null;
            for (int i = 0; i < popup.getComponentCount(); i++) {
                Component c = popup.getComponent(i);
                if (c instanceof JPanel) { calPanel = c; break; }
            }
            // Try getContent children
            if (calPanel == null) {
                Container cc = popup.getContent();
                for (int i = 0; i < cc.getComponentCount(); i++) if (cc.getComponent(i) instanceof JPanel) { calPanel = cc.getComponent(i); break; }
            }
            assert calPanel != null : "calendar panel found";
            calPanel.setSize(CAL_W, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(CAL_W, HEADER_H + WEEKDAY_H + CELL_H * 6 + 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            try { calPanel.paint(gg); } finally { gg.dispose(); }
            // Verify header text rendered (check pixel in header area is not transparent)
            int headerPx = img.getRGB(CAL_W / 2, 20);
            int ha = (headerPx >>> 24) & 0xFF;
            assert ha > 100 : "header rendered; alpha=" + ha;
            // Verify a day cell area rendered (row 2, col 3 = mid-week, should be in current month for Aug 2026)
            // Aug 1 2026 is Saturday → firstDayOfWeek = 6 (Saturday=6 in our Sunday=0 system)
            // gridStart = Aug 1 - 6 days = Jul 26
            // row=2 col=3 → idx=17 → Jul 26 + 17 = Aug 12 → current month → TEXT_MAIN on WHITE
            int dayPx = img.getRGB(8 + 3 * CELL_W + CELL_W / 2, HEADER_H + WEEKDAY_H + 2 * CELL_H + CELL_H / 2);
            int da = (dayPx >>> 24) & 0xFF;
            assert da > 100 : "day cell rendered; alpha=" + da;
            // Regression: 移入(contains)不得选中；点击(handleClick)才选中
            // row=2 col=3 对应 2026-08-12（网格起点 Jul 26 + 17 天）
            LocalDate beforeRe = dp.getDate();
            boolean hitRe = dp.calendarPanel.contains(134, 148);
            assert hitRe : "day cell hit";
            assert dp.getDate().equals(beforeRe) : "hover(contains) must NOT select; got " + dp.getDate();
            dp.calendarPanel.handleClick(134, 148);
            assert dp.getDate().equals(LocalDate.of(2026, 8, 12)) : "click(handleClick) should select 2026-08-12; got " + dp.getDate();
            // Close
            dp.hideDatePicker();
            jf.dispose();
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        // FormValueProvider 取值契约
        AstDatePicker dpTest = new AstDatePicker();
        assert dpTest.getFormValue().isEmpty() : "DatePicker empty when no date";
        dpTest.setDate(LocalDate.of(2026, 8, 24));
        assert dpTest.getFormValue().equals("2026-08-24") : "DatePicker getFormValue, got " + dpTest.getFormValue();
        dpTest.setFormValue("");
        assert dpTest.getFormValue().isEmpty() : "DatePicker clear";

        // 尺寸档位（R1）：触发框高度随档位单调变化（由 invoker AstButton 档位驱动）
        AstDatePicker dpSz = new AstDatePicker(LocalDate.of(2026, 8, 24));
        int hDef = dpSz.getPreferredSize().height;
        dpSz.setSize(AstDatePicker.SIZE_LARGE);
        int hL = dpSz.getPreferredSize().height;
        dpSz.setSize(AstDatePicker.SIZE_SMALL);
        int hS = dpSz.getPreferredSize().height;
        assert hL > hDef && hDef > hS : "DatePicker tier height monotonic LARGE>DEFAULT>SMALL, got " + hL + "/" + hDef + "/" + hS;
        boolean tD = false;
        try { dpSz.setSize(9); } catch (IllegalArgumentException e) { tD = true; }
        assert tD : "DatePicker invalid tier throws";

        System.out.println("AstDatePicker self-check OK");
    }
    public static void main(String[] args) {
        new AstDatePicker().selfCheck();
    }
}
