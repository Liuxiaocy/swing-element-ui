package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.function.Consumer;

/**
 * Calendar 日历 — 月份网格，可前后翻月、选中日期、高亮"今日"。
 *
 * 用法：
 *   AstCalendar cal = new AstCalendar();
 *   cal.setDateListener(date -> System.out.println(date[0]+"-"+(date[1]+1)+"-"+date[2]));
 *   frame.add(cal);
 *
 * 设计：
 *  - 顶部标题栏：左 ‹ / 右 › 切换月份，中间显示 "YYYY 年 MM 月"。
 *  - 星期表头：日 一 二 三 四 五 六，TEXT_REGULAR 13px。
 *  - 日期网格：6 行 × 7 列，每格 36×36，当日 PRIMARY 填充白字，选中 INFO 边框，
 *    非本月 TEXT_PLACEHOLDER，本月 TEXT_MAIN，hover FILL_BASE。
 *  - 切换月份时整面板淡入（alpha 动画 180ms easeOut）。
 *  - 对比度：日期文字白底断言；当日白字 PRIMARY 底按惯例跳过。
 */
public class AstCalendar extends JComponent {
    private int year, month; // month 0-11
    private int selDay = -1;
    private int hoverCell = -1;
    private float alpha = 1f;
    private final Animator fadeAnim = new Animator(180, new Easing() { public float apply(float t) { return Easing.easeOut(t); }},
        new Animator.Listener() { public void update(float v) { alpha = v; repaint(); }});
    private Consumer<int[]> dateListener; // [year, month0, day]

    private static final int CELL = 36;
    private static final int HEADER_H = 40;
    private static final int WEEK_H = 24;
    private static final String[] WEEK = { "日", "一", "二", "三", "四", "五", "六" };

    public AstCalendar() {
        Calendar now = Calendar.getInstance();
        this.year = now.get(Calendar.YEAR);
        this.month = now.get(Calendar.MONTH);
        setOpaque(false);
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                int cell = cellAt(e.getX(), e.getY());
                if (cell != hoverCell) {
                    hoverCell = cell;
                    repaint();
                }
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseExited(java.awt.event.MouseEvent e) { hoverCell = -1; repaint(); }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int cell = cellAt(e.getX(), e.getY());
                if (cell < 0) return;
                int[] grid = gridInfo();
                int firstWeekday = grid[0];
                int daysInMonth = grid[1];
                int day = cell - firstWeekday + 1;
                if (day >= 1 && day <= daysInMonth) {
                    selDay = day;
                    repaint();
                    if (dateListener != null) dateListener.accept(new int[]{ year, month, day });
                }
            }
        });
    }

    public void setDateListener(Consumer<int[]> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.dateListener = l;
    }

    public void setSelected(int y, int m0, int d) {
        if (m0 < 0 || m0 > 11) throw new IllegalArgumentException("month out of range: " + m0);
        if (d < 1 || d > 31) throw new IllegalArgumentException("day out of range: " + d);
        boolean monthChanged = (y != year || m0 != month);
        this.year = y; this.month = m0; this.selDay = d;
        if (monthChanged) { alpha = 0f; fadeAnim.stop(); fadeAnim.go(0f, 1f); }
        else repaint();
    }

    public int[] getSelected() {
        return selDay > 0 ? new int[]{ year, month, selDay } : null;
    }

    public void prevMonth() {
        if (month == 0) { month = 11; year--; } else month--;
        selDay = -1;
        alpha = 0f; fadeAnim.stop(); fadeAnim.go(0f, 1f);
    }

    public void nextMonth() {
        if (month == 11) { month = 0; year++; } else month++;
        selDay = -1;
        alpha = 0f; fadeAnim.stop(); fadeAnim.go(0f, 1f);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(7 * CELL + 16, HEADER_H + WEEK_H + 6 * CELL + 16); }
    @Override public Dimension getMinimumSize() { return new Dimension(7 * CELL, HEADER_H + WEEK_H + 6 * CELL); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    private int[] gridInfo() {
        Calendar cal = new GregorianCalendar(year, month, 1);
        int firstWeekday = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        return new int[]{ firstWeekday, daysInMonth };
    }

    private int cellAt(int x, int y) {
        int ox = 8, oy = HEADER_H + WEEK_H + 8;
        int cx = (x - ox) / CELL;
        int cy = (y - oy) / CELL;
        if (cx < 0 || cx > 6 || cy < 0 || cy > 5) return -1;
        return cy * 7 + cx;
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int w = getWidth();
        // 整面板 alpha 应用：用 ALPHAComposite
        java.awt.Composite oldComp = g2.getComposite();
        g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(Math.max(0.15f, alpha)));
        // 标题栏
        String title = String.format("%d 年 %02d 月", year, month + 1);
        g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(ElementTheme.TEXT_MAIN);
        ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, getBackground0(), "AstCalendar title");
        int titleX = (w - fm.stringWidth(title)) / 2;
        g2.drawString(title, titleX, 26);
        // 左右箭头按钮
        drawNavButton(g2, 12, 6, "‹", hoverCell == -2);
        drawNavButton(g2, w - 36, 6, "›", hoverCell == -3);
        // 星期表头
        int ox = 8, oy = HEADER_H + 8;
        g2.setFont(ElementTheme.FONT.deriveFont(13f));
        fm = g2.getFontMetrics();
        g2.setColor(ElementTheme.TEXT_REGULAR);
        for (int i = 0; i < 7; i++) {
            String s = WEEK[i];
            int sx = ox + i * CELL + (CELL - fm.stringWidth(s)) / 2;
            g2.drawString(s, sx, oy + fm.getAscent() + 4);
        }
        oy += WEEK_H;
        // 日期网格
        int[] grid = gridInfo();
        int firstWeekday = grid[0], daysInMonth = grid[1];
        Calendar today = Calendar.getInstance();
        int todayY = today.get(Calendar.YEAR), todayM = today.get(Calendar.MONTH), todayD = today.get(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < 42; i++) {
            int col = i % 7, row = i / 7;
            int x = ox + col * CELL, y = oy + row * CELL;
            int day = i - firstWeekday + 1;
            boolean inMonth = (day >= 1 && day <= daysInMonth);
            boolean isToday = (year == todayY && month == todayM && day == todayD);
            boolean isSel = (day == selDay);
            boolean isHover = (i == hoverCell);
            Color fg;
            Color bg = null;
            if (isToday) { bg = ElementTheme.PRIMARY; fg = Color.WHITE; }
            else if (isSel) { fg = ElementTheme.PRIMARY; bg = new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), 30); }
            else if (isHover && inMonth) { bg = ElementTheme.FILL_BASE; fg = ElementTheme.TEXT_MAIN; }
            else if (!inMonth) { fg = ElementTheme.TEXT_PLACEHOLDER; }
            else { fg = ElementTheme.TEXT_MAIN; }
            if (!isToday && inMonth) {
                ElementTheme.assertContrast(fg, bg != null ? bg : getBackground0(), "AstCalendar day " + day);
            }
            if (bg != null) {
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(x+1, y+1, CELL-2, CELL-2, 6, 6));
            }
            g2.setColor(fg);
            g2.setFont(ElementTheme.FONT.deriveFont(13f));
            FontMetrics fm2 = g2.getFontMetrics();
            String s = inMonth ? String.valueOf(day) : "";
            if (s.length() > 0) {
                int sx = x + (CELL - fm2.stringWidth(s)) / 2;
                int sy = y + (CELL - fm2.getHeight()) / 2 + fm2.getAscent();
                g2.drawString(s, sx, sy);
            }
        }
        g2.setComposite(oldComp);
        g2.dispose();
    }

    private Color getBackground0() { return Color.WHITE; }

    private void drawNavButton(Graphics2D g2, int x, int y, String s, boolean hover) {
        RoundRectangle2D r = new RoundRectangle2D.Float(x, y, 24, 24, 6, 6);
        g2.setColor(hover ? ElementTheme.FILL_BASE : new Color(0, 0, 0, 0));
        if (hover) g2.fill(r);
        g2.setColor(ElementTheme.TEXT_REGULAR);
        ElementTheme.assertContrast(ElementTheme.TEXT_REGULAR, Color.WHITE, "AstCalendar nav");
        g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 18f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, x + (24 - fm.stringWidth(s)) / 2, y + (24 - fm.getHeight()) / 2 + fm.getAscent());
    }

    static void selfCheck() {
        boolean threw = false;
        try { new AstCalendar().setDateListener(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null listener must throw"; threw = false;
        try { new AstCalendar().setSelected(2026, -1, 1); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "month<0 must throw"; threw = false;
        try { new AstCalendar().setSelected(2026, 12, 1); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "month>11 must throw"; threw = false;
        try { new AstCalendar().setSelected(2026, 0, 0); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "day<1 must throw"; threw = false;
        try { new AstCalendar().setSelected(2026, 0, 32); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "day>31 must throw";

        AstCalendar cal = new AstCalendar();
        // 网格信息校验：2026-08-01 是周六 → firstWeekday=6
        cal.year = 2026; cal.month = 7; // August
        int[] grid = cal.gridInfo();
        assert grid[0] == 6 : "2026-08-01 firstWeekday 应为 6（周六），实际=" + grid[0];
        assert grid[1] == 31 : "2026-08 共 31 天，实际=" + grid[1];

        cal.setSelected(2026, 7, 15);
        int[] sel = cal.getSelected();
        assert sel != null && sel[0] == 2026 && sel[1] == 7 && sel[2] == 15 : "getSelected roundtrip";

        // prevMonth / nextMonth
        cal.year = 2026; cal.month = 0; cal.selDay = 5;
        cal.prevMonth();
        assert cal.year == 2025 && cal.month == 11 : "prevMonth 跨年";
        cal.nextMonth();
        assert cal.year == 2026 && cal.month == 0 : "nextMonth 跨年";
        cal.month = 11; cal.nextMonth();
        assert cal.year == 2027 && cal.month == 0 : "nextMonth 跨年";

        final int[] fired = {0};
        final AstCalendar cal2 = new AstCalendar();
        cal2.setDateListener(new java.util.function.Consumer<int[]>() { public void accept(int[] d) { fired[0]++; }});

        // 离屏绘制校验对比度断言
        cal2.year = 2026; cal2.month = 7; cal2.alpha = 1f;
        cal2.setSize(cal2.getPreferredSize());
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                cal2.getWidth(), cal2.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { cal2.paint(gg); } finally { gg.dispose(); }
        // 扫描整图确认有非透明像素被绘制
        boolean anyPainted = false;
        outer:
        for (int y = 0; y < img.getHeight(); y += 4) {
            for (int x = 0; x < img.getWidth(); x += 4) {
                if (((img.getRGB(x, y) >>> 24) & 0xFF) > 100) { anyPainted = true; break outer; }
            }
        }
        assert anyPainted : "calendar 应绘制出非透明像素";
        System.out.println("AstCalendar self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
