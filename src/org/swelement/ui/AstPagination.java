package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.framework.AstAbstractComponent;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class AstPagination extends AstAbstractComponent {
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    /** 按钮宽高 / 字号随档位。 */
    private static final int[] TIER_BTN_H = {32, 28, 24};
    private static final int[] TIER_BTN_W = {32, 28, 24};
    private static final float[] TIER_FONT = {14f, 12f, 12f};
    private int tier = SIZE_DEFAULT;

    private int total, pageSize = 10, current = 1;
    private final List<IntConsumer> listeners = new ArrayList<>();
    private final javax.swing.event.EventListenerList swingListeners = new javax.swing.event.EventListenerList();
    private final JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
    private final JTextField jumper = new JTextField(3);

    public AstPagination() {
        setLayout(new BorderLayout());
        row.setOpaque(false);
        add(row, BorderLayout.CENTER);
        jumper.setFont(theme().getFontBase().deriveFont(TIER_FONT[tier]));
        jumper.setBorder(new EmptyBorder(2, 4, 2, 4));
        jumper.addActionListener(this::onJump);
    }

    /** 尺寸档位（Element UI size：large/default/small），按钮与跳页框联动。 */
    public void setSize(int t) {
        if (t < SIZE_LARGE || t > SIZE_SMALL) throw new IllegalArgumentException("tier out of range: " + t);
        tier = t;
        jumper.setFont(theme().getFontBase().deriveFont(TIER_FONT[tier]));
        rebuild();
    }

    public int getSizeTier() { return tier; }

    public AstPagination(int totalCount, int pageSize, int initialPage) {
        this();
        this.total = Math.max(0, totalCount);
        this.pageSize = Math.max(1, pageSize);
        this.current = 1;
        rebuild();
        setCurrentPage(initialPage);
    }

    public int getTotalCount() { return total; }

    public int getPageSize() { return pageSize; }

    public int getTotalPages() { return pages(); }

    public void setTotal(int t) { total = Math.max(0, t); rebuild(); }

    public void setPageSize(int s) { pageSize = Math.max(1, s); rebuild(); }

    public int getCurrentPage() { return current; }

    public void setCurrentPage(int v) {
        int pages = pages();
        current = Math.max(1, Math.min(pages, v));
        rebuild();
        for (IntConsumer l : listeners) l.accept(current);
        ChangeListener[] ls = swingListeners.getListeners(ChangeListener.class);
        if (ls.length > 0) {
            javax.swing.event.ChangeEvent ev = new javax.swing.event.ChangeEvent(this);
            for (ChangeListener l : ls) l.stateChanged(ev);
        }
    }

    public void addPageChangeListener(IntConsumer l) { listeners.add(l); }

    public void addChangeListener(ChangeListener l) { swingListeners.add(ChangeListener.class, l); }

    public void removeChangeListener(ChangeListener l) { swingListeners.remove(ChangeListener.class, l); }

    private int pages() { return total == 0 ? 1 : (total + pageSize - 1) / pageSize; }

    static List<Integer> pageWindow(int cur, int pages) {
        List<Integer> out = new ArrayList<>();
        for (int p = 1; p <= pages; p++) {
            if (p == 1 || p == pages || Math.abs(p - cur) <= 2) out.add(p);
            else if (out.isEmpty() || out.get(out.size() - 1) != -1) out.add(-1);
        }
        return out;
    }

    private void rebuild() {
        row.removeAll();
        row.add(new PageButton("\u2039", current > 1 ? current - 1 : -1));     // ‹
        for (int p : pageWindow(current, pages())) {
            if (p == -1) {
                JLabel dots = new JLabel("…");
                dots.setForeground(new Color(0x909399));
                row.add(dots);
            } else {
                row.add(new PageButton(String.valueOf(p), p));
            }
        }
        row.add(new PageButton("\u203a", current < pages() ? current + 1 : -1)); // ›
        row.add(new JLabel("共 " + total + " 条"));
        JLabel go = new JLabel("前往");
        go.setForeground(new Color(0x606266));
        row.add(go);
        row.add(jumper);
        JLabel page = new JLabel("页");
        page.setForeground(new Color(0x606266));
        row.add(page);
        row.revalidate();
        row.repaint();
    }

    private class PageButton extends AstInteractiveComponent {
        private final int page;
        private final String text;

        PageButton(String text, int page) {
            this.text = text;
            this.page = page;
            setPreferredSize(new Dimension(page > 0 ? TIER_BTN_W[tier] : TIER_BTN_W[tier] - 4, TIER_BTN_H[tier]));
            setFont(theme().getFontBase().deriveFont(TIER_FONT[tier]));
            if (page > 0) {
                addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) { if (!isEnabled()) return; setCurrentPage(page); }
                });
            }
        }

        @Override
        protected boolean isToggleMode() { return false; }

        @Override
        protected void selfCheck() {}

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            float hover = hoverProgress();
            boolean active = page == current && page > 0;
            if (active || hover > 0) {
                g2.setColor(active ? theme().getPrimary() : lerp(Color.WHITE, new Color(0xF5F7FA), hover));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            }
            g2.setFont(theme().getFontBase().deriveFont(TIER_FONT[tier]));
            g2.setColor(active ? Color.WHITE : (page > 0 ? new Color(0x606266) : new Color(0xC0C4CC)));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    private void onJump(ActionEvent e) {
        try {
            int p = Integer.parseInt(jumper.getText().trim());
            setCurrentPage(p);
            jumper.setText("");
        } catch (NumberFormatException ignore) { }
    }

    @Override
    protected void selfCheck() {
        assert pageWindow(1, 10).equals(java.util.Arrays.asList(1, 2, 3, -1, 10));
        assert pageWindow(5, 10).equals(java.util.Arrays.asList(1, -1, 3, 4, 5, 6, 7, -1, 10));
        assert pageWindow(9, 10).equals(java.util.Arrays.asList(1, -1, 7, 8, 9, 10));
        assert pageWindow(1, 1).equals(java.util.Arrays.asList(1));
        assert pageWindow(1, 3).equals(java.util.Arrays.asList(1, 2, 3));

        // --- 尺寸档位：分页按钮高度与跳页框字体随档位联动（32/28/24） ---
        AstPagination pg = new AstPagination(100, 10, 1);
        assert pg.getSizeTier() == SIZE_DEFAULT : "default tier";
        assert firstPageButton(pg).getPreferredSize().height == 28
            : "default btn h=" + firstPageButton(pg).getPreferredSize().height;
        pg.setSize(SIZE_LARGE);
        assert pg.jumper.getFont().getSize2D() == 14f : "large jumper font=" + pg.jumper.getFont().getSize2D();
        assert firstPageButton(pg).getPreferredSize().height == 32
            : "large btn h=" + firstPageButton(pg).getPreferredSize().height;
        pg.setSize(SIZE_SMALL);
        assert pg.jumper.getFont().getSize2D() == 12f : "small jumper font=" + pg.jumper.getFont().getSize2D();
        assert firstPageButton(pg).getPreferredSize().height == 24
            : "small btn h=" + firstPageButton(pg).getPreferredSize().height;
        boolean threw = false;
        try { pg.setSize(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "invalid tier must throw";

        // 对比度
        assertContrast(new Color(0x606266), Color.WHITE, "pagination btn text on white");

        System.out.println("AstPagination self-check OK");
    }

    private static PageButton firstPageButton(AstPagination pg) {
        for (Component c : pg.row.getComponents()) {
            if (c instanceof PageButton) return (PageButton) c;
        }
        throw new AssertionError("no PageButton found");
    }

    public static void main(String[] args) {
        new AstPagination(100, 10, 1).selfCheck();
    }
}
