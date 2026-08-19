package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class Pagination extends JComponent {
    private int total, pageSize = 10, current = 1;
    private final List<IntConsumer> listeners = new ArrayList<>();
    private final JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
    private final JTextField jumper = new JTextField(3);

    public Pagination() {
        setOpaque(false);
        setLayout(new BorderLayout());
        row.setOpaque(false);
        add(row, BorderLayout.CENTER);
        jumper.setFont(ElementTheme.FONT.deriveFont(12f));
        jumper.setBorder(new EmptyBorder(2, 4, 2, 4));
        jumper.addActionListener(this::onJump);
    }

    public void setTotal(int t) { total = Math.max(0, t); rebuild(); }

    public void setPageSize(int s) { pageSize = Math.max(1, s); rebuild(); }

    public int getCurrentPage() { return current; }

    public void setCurrentPage(int v) {
        int pages = pages();
        current = Math.max(1, Math.min(pages, v));
        rebuild();
        for (IntConsumer l : listeners) l.accept(current);
    }

    public void addPageChangeListener(IntConsumer l) { listeners.add(l); }

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

    private class PageButton extends JLabel {
        private final int page;
        private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
        private float hover;

        PageButton(String text, int page) {
            super(text);
            this.page = page;
            setOpaque(false);
            setPreferredSize(new Dimension(page > 0 ? 28 : 24, 28));
            setFont(ElementTheme.FONT.deriveFont(12f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (page > 0) {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { if (!isEnabled()) return; hoverAnim.go(hover, 1f); }
                    public void mouseExited(MouseEvent e) { hoverAnim.go(hover, 0f); }
                    public void mouseClicked(MouseEvent e) { if (!isEnabled()) return; setCurrentPage(page); }
                });
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean active = page == current && page > 0;
            if (active || hover > 0) {
                g2.setColor(active ? ElementTheme.PRIMARY : ElementTheme.lerp(Color.WHITE, new Color(0xF5F7FA), hover));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            }
            g2.setColor(active ? Color.WHITE : (page > 0 ? new Color(0x606266) : new Color(0xC0C4CC)));
            FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT.deriveFont(12f));
            g2.setFont(ElementTheme.FONT.deriveFont(12f));
            g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
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

    public static void selfCheck() {
        assert pageWindow(1, 10).equals(java.util.Arrays.asList(1, 2, 3, -1, 10));
        assert pageWindow(5, 10).equals(java.util.Arrays.asList(1, -1, 3, 4, 5, 6, 7, -1, 10));
        assert pageWindow(9, 10).equals(java.util.Arrays.asList(1, -1, 7, 8, 9, 10));
        assert pageWindow(1, 1).equals(java.util.Arrays.asList(1));
        assert pageWindow(1, 3).equals(java.util.Arrays.asList(1, 2, 3));
        System.out.println("Pagination self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}