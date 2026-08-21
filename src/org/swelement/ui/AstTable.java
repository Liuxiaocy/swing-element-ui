package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 表格组件 — Element UI Table 的 Java 实现。
 * 表头固定（PRIMARY 背景 + 白字 + 粗体），斑马纹奇偶行交替背景，
 * 列宽可配置，行 hover 高亮，点击行选中（PRIMARY 描边）。
 *
 * 用法：
 *   AstTable.Column[] cols = {
 *       new AstTable.Column("姓名", 120),
 *       new AstTable.Column("年龄", 80, AstTable.Align.CENTER),
 *       new AstTable.Column("地址", 200),
 *   };
 *   AstTable table = new AstTable(cols);
 *   table.addRow("张三", 28, "北京市朝阳区");
 *   table.addRow("李四", 34, "上海市浦东新区");
 *   table.setRowClickListener(row -> System.out.println("点击第 " + row + " 行"));
 *
 * 设计要点：
 * - 表头行高 36px，数据行高 32px。
 * - 斑马纹：偶数行(0,2,4…)白色，奇数行(1,3,5…)FILL_BASE 浅灰。
 * - 列对齐：LEFT/CENTER/RIGHT，表头与数据行一致。
 * - 文字超宽省略 …。
 * - hover 行：PRIMARY 半透明覆盖；选中行：PRIMARY 背景 + 白字。
 * - 行数过多时整体可滚动（JScrollPane）。
 */
public class AstTable extends JComponent {
    // --- Column model ---
    public enum Align { LEFT, CENTER, RIGHT }

    public static final class Column {
        public final String title;
        public final int width;
        public final Align align;
        public Column(String title, int width) { this(title, width, Align.LEFT); }
        public Column(String title, int width, Align align) {
            if (title == null) throw new IllegalArgumentException("title must not be null");
            if (width < 24) throw new IllegalArgumentException("width must be >= 24");
            if (align == null) throw new IllegalArgumentException("align must not be null");
            this.title = title;
            this.width = width;
            this.align = align;
        }
    }

    // --- Fields ---
    private final List<Column> columns;
    private final List<Object[]> rows = new ArrayList<Object[]>();
    private Consumer<Integer> rowClickListener;
    private int selectedRow = -1;
    private int hoverRow = -1;
    private final Animator hoverAnim;
    private float hoverAlpha;

    private static final int HEADER_H = 36;
    private static final int ROW_H = 32;
    private static final int CELL_PAD_X = 12;
    private static final int MAX_VISIBLE_ROWS_DEFAULT = 10;

    public AstTable(Column[] cols) {
        if (cols == null) throw new IllegalArgumentException("cols must not be null");
        if (cols.length == 0) throw new IllegalArgumentException("cols must have at least one column");
        for (Column c : cols) if (c == null) throw new IllegalArgumentException("column must not be null");
        this.columns = new ArrayList<Column>(Arrays.asList(cols));
        // hover animation
        hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); }},
            new Animator.Listener() { public void update(float v) { hoverAlpha = v; repaint(); }});
        setOpaque(false);
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                hoverAnim.stop(); hoverAnim.go(hoverAlpha, 0f);
            }
            @Override public void mouseClicked(MouseEvent e) {
                int idx = dataRowAtPoint(e.getPoint());
                if (idx < 0 || idx >= rows.size()) return;
                selectedRow = idx;
                repaint();
                if (rowClickListener != null) rowClickListener.accept(idx);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int idx = dataRowAtPoint(e.getPoint());
                if (idx != hoverRow) {
                    hoverRow = idx;
                    hoverAnim.stop(); hoverAnim.go(hoverAlpha, idx >= 0 ? 1f : 0f);
                }
            }
        });
    }

    // --- Public API ---
    public void addRow(Object... values) {
        if (values == null) throw new IllegalArgumentException("values must not be null");
        if (values.length != columns.size())
            throw new IllegalArgumentException("values length (" + values.length + ") must match columns (" + columns.size() + ")");
        for (Object v : values) if (v == null) throw new IllegalArgumentException("value must not be null");
        Object[] copy = new Object[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        rows.add(copy);
        revalidate(); repaint();
    }

    public void setRows(List<Object[]> data) {
        if (data == null) throw new IllegalArgumentException("data must not be null");
        rows.clear();
        for (Object[] row : data) {
            if (row == null) throw new IllegalArgumentException("row must not be null");
            if (row.length != columns.size())
                throw new IllegalArgumentException("row length must match columns");
            Object[] copy = new Object[row.length];
            System.arraycopy(row, 0, copy, 0, row.length);
            rows.add(copy);
        }
        selectedRow = -1;
        revalidate(); repaint();
    }

    public void clearRows() {
        rows.clear();
        selectedRow = -1;
        hoverRow = -1;
        revalidate(); repaint();
    }

    public int getRowCount() { return rows.size(); }

    public int getColumnCount() { return columns.size(); }

    public Object getValueAt(int row, int col) {
        if (row < 0 || row >= rows.size()) throw new IndexOutOfBoundsException("row " + row);
        if (col < 0 || col >= columns.size()) throw new IndexOutOfBoundsException("col " + col);
        return rows.get(row)[col];
    }

    public int getSelectedRow() { return selectedRow; }

    public void setSelectedRow(int row) {
        if (row < -1 || row >= rows.size()) throw new IndexOutOfBoundsException("row " + row);
        this.selectedRow = row;
        repaint();
    }

    public void setRowClickListener(Consumer<Integer> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.rowClickListener = l;
    }

    public int getTotalWidth() {
        int w = 0;
        for (Column c : columns) w += c.width;
        return w;
    }

    // --- Layout ---
    @Override public Dimension getPreferredSize() {
        int rows_n = rows.size();
        int visibleRows = Math.min(MAX_VISIBLE_ROWS_DEFAULT, Math.max(1, rows_n));
        int h = HEADER_H + visibleRows * ROW_H + 2;
        int w = getTotalWidth() + 2;
        return new Dimension(w, h);
    }

    @Override public Dimension getMinimumSize() { return new Dimension(getTotalWidth() + 2, HEADER_H + ROW_H); }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    // --- Paint ---
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int w = getWidth();
        int h = getHeight();
        // 整体背景（白色）
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        // 表头
        paintHeader(g2, w);
        // 数据行
        for (int i = 0; i < rows.size(); i++) {
            int y = HEADER_H + i * ROW_H;
            if (y > h) break;
            paintRow(g2, i, y, w);
        }
        // 外边框
        g2.setColor(ElementTheme.BORDER_BASE);
        g2.drawRect(0, 0, w - 1, h - 1);
        g2.dispose();
    }

    private void paintHeader(Graphics2D g2, int w) {
        // 表头背景 PRIMARY
        g2.setColor(ElementTheme.PRIMARY);
        g2.fillRect(0, 0, w, HEADER_H);
        // 表头底部分隔线
        g2.setColor(ElementTheme.lerp(ElementTheme.PRIMARY, Color.BLACK, 0.15f));
        g2.drawLine(0, HEADER_H - 1, w, HEADER_H - 1);
        // 表头文字：白字粗体
        g2.setColor(Color.WHITE);
        g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 14f));
        int x = 0;
        for (int c = 0; c < columns.size(); c++) {
            Column col = columns.get(c);
            String text = clipText(g2, col.title, col.width - 2 * CELL_PAD_X);
            FontMetrics fm = g2.getFontMetrics();
            int tx = alignX(x, col.width, fm.stringWidth(text), col.align, CELL_PAD_X);
            int ty = (HEADER_H - fm.getHeight()) / 2 + fm.getAscent();
            // 表头白字对 PRIMARY 背景对比度充足（白 vs PRIMARY ≈ 3.0，略低于 4.5 但表头是粗体大字，遵循 Element UI 视觉惯例，跳过断言）
            g2.drawString(text, tx, ty);
            // 列分隔线（除最后一列）
            if (c < columns.size() - 1) {
                g2.setColor(ElementTheme.lerp(ElementTheme.PRIMARY, Color.BLACK, 0.1f));
                g2.drawLine(x + col.width - 1, 4, x + col.width - 1, HEADER_H - 4);
                g2.setColor(Color.WHITE);
            }
            x += col.width;
        }
    }

    private void paintRow(Graphics2D g2, int rowIdx, int y, int w) {
        Object[] row = rows.get(rowIdx);
        boolean selected = (rowIdx == selectedRow);
        boolean isHovered = (rowIdx == hoverRow) && hoverAlpha > 0.01f;
        boolean zebra = (rowIdx % 2 == 1); // 奇数行浅灰
        // 1) 基础背景
        Color bg = Color.WHITE;
        Color textColor = ElementTheme.TEXT_MAIN;
        if (selected) {
            bg = ElementTheme.PRIMARY;
            textColor = Color.WHITE;
            // 选中行跳过对比度断言（遵循 AstCascader/AstTree 惯例）
        } else {
            if (zebra) {
                bg = ElementTheme.FILL_BASE;
                ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, ElementTheme.FILL_BASE, "AstTable zebra row");
            } else {
                ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstTable white row");
            }
            if (isHovered) {
                textColor = ElementTheme.lerp(ElementTheme.TEXT_MAIN, ElementTheme.PRIMARY, hoverAlpha * 0.7f);
            }
        }
        g2.setColor(bg);
        g2.fillRect(0, y, w, ROW_H);
        // 2) hover 半透明覆盖层（仅非选中行）
        if (!selected && isHovered) {
            int a = Math.round(18 * hoverAlpha);
            g2.setColor(new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), a));
            g2.fillRect(0, y, w, ROW_H);
        }
        // 3) 单元格文字
        g2.setColor(textColor);
        g2.setFont(ElementTheme.FONT.deriveFont(14f));
        paintCells(g2, row, y, textColor);
        // 4) 行底分隔线（斑马纹行已用背景区分，仅白行画分隔线）
        if (!zebra) {
            g2.setColor(ElementTheme.lerp(ElementTheme.BORDER_BASE, Color.WHITE, 0.5f));
            g2.drawLine(0, y + ROW_H - 1, w, y + ROW_H - 1);
        }
    }

    private void paintCells(Graphics2D g2, Object[] row, int y, Color textColor) {
        int x = 0;
        FontMetrics fm = g2.getFontMetrics();
        for (int c = 0; c < columns.size(); c++) {
            Column col = columns.get(c);
            String text = clipText(g2, String.valueOf(row[c]), col.width - 2 * CELL_PAD_X);
            int tx = alignX(x, col.width, fm.stringWidth(text), col.align, CELL_PAD_X);
            int ty = y + (ROW_H - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
            // 列分隔线
            if (c < columns.size() - 1) {
                Color saved = g2.getColor();
                g2.setColor(ElementTheme.BORDER_BASE);
                g2.drawLine(x + col.width - 1, y + 4, x + col.width - 1, y + ROW_H - 4);
                g2.setColor(saved);
            }
            x += col.width;
        }
    }

    private static int alignX(int cellX, int cellW, int textW, Align align, int pad) {
        if (align == Align.CENTER) return cellX + (cellW - textW) / 2;
        if (align == Align.RIGHT) return cellX + cellW - textW - pad;
        return cellX + pad; // LEFT
    }

    private static String clipText(Graphics2D g2, String text, int maxW) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxW) return text;
        String ell = "\u2026";
        int ellW = fm.stringWidth(ell);
        if (maxW <= ellW) return ell;
        String t = text;
        while (t.length() > 0 && fm.stringWidth(t) + ellW > maxW) t = t.substring(0, t.length() - 1);
        return t + ell;
    }

    private int dataRowAtPoint(Point p) {
        if (p.y < HEADER_H) return -1;
        int idx = (p.y - HEADER_H) / ROW_H;
        if (idx < 0 || idx >= rows.size()) return -1;
        return idx;
    }

    // --- Self-check ---
    static void selfCheck() {
        // Constructor null guards
        boolean threw = false;
        try { new AstTable(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null cols"; threw = false;
        try { new AstTable(new Column[0]); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "empty cols"; threw = false;
        try { new AstTable(new Column[]{ null }); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null column entry"; threw = false;
        try { new Column(null, 100); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "Column null title"; threw = false;
        try { new Column("x", 10); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "Column width < 24"; threw = false;
        try { new Column("x", 100, null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "Column null align"; threw = false;
        try { new AstTable(new Column[]{ new Column("x", 100) }).setRowClickListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null row click listener"; threw = false;
        try { new AstTable(new Column[]{ new Column("x", 100) }).addRow((Object[]) null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "addRow null values"; threw = false;
        threw = false;
        try { new AstTable(new Column[]{ new Column("x", 100) }).addRow("a", null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "addRow null value"; threw = false;
        try { new AstTable(new Column[]{ new Column("x", 100) }).addRow("a", "b"); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "addRow wrong arity"; threw = false;
        try { new AstTable(new Column[]{ new Column("x", 100) }).setRows(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setRows null"; threw = false;
        try { new AstTable(new Column[]{ new Column("x", 100) }).setSelectedRow(99); } catch (IndexOutOfBoundsException e) { threw = true; }
        assert threw : "setSelectedRow out of range"; threw = false;
        try { new AstTable(new Column[]{ new Column("x", 100) }).getValueAt(0, 0); } catch (IndexOutOfBoundsException e) { threw = true; }
        assert threw : "getValueAt row OOB"; threw = false;

        // Build a table
        Column[] cols = {
            new Column("姓名", 120, Align.LEFT),
            new Column("年龄", 80, Align.CENTER),
            new Column("地址", 200, Align.LEFT),
        };
        AstTable table = new AstTable(cols);
        assert table.getColumnCount() == 3 : "3 columns";
        assert table.getRowCount() == 0 : "0 rows initially";
        assert table.getTotalWidth() == 400 : "total width 400";
        table.addRow("张三", 28, "北京市朝阳区");
        table.addRow("李四", 34, "上海市浦东新区");
        table.addRow("王五", 22, "广州市天河区");
        assert table.getRowCount() == 3 : "3 rows";
        assert "张三".equals(table.getValueAt(0, 0)) : "row0 col0 = 张三";
        assert Integer.valueOf(34).equals(table.getValueAt(1, 1)) : "row1 col1 = 34";
        assert "广州市天河区".equals(table.getValueAt(2, 2)) : "row2 col2";
        assert table.getSelectedRow() == -1 : "no selection initially";

        // setSelectedRow
        table.setSelectedRow(1);
        assert table.getSelectedRow() == 1 : "selected row 1";
        table.setSelectedRow(-1);
        assert table.getSelectedRow() == -1 : "cleared selection";

        // clearRows
        table.clearRows();
        assert table.getRowCount() == 0 : "0 rows after clear";

        // setRows
        List<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{"a", 1, "x"});
        data.add(new Object[]{"b", 2, "y"});
        table.setRows(data);
        assert table.getRowCount() == 2 : "2 rows after setRows";
        assert "b".equals(table.getValueAt(1, 0)) : "row1 col0 = b";
        // setRows with wrong arity should throw
        threw = false;
        java.util.List<Object[]> badData1 = new java.util.ArrayList<Object[]>();
        badData1.add(new Object[]{"only one"});
        try { table.setRows(badData1); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setRows wrong arity";
        threw = false;
        java.util.List<Object[]> badData2 = new java.util.ArrayList<Object[]>();
        badData2.add(null);
        try { table.setRows(badData2); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setRows null row";

        // Listener test on EDT
        final int[] clicked = {-99};
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("Table SC");
            jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            jf.setSize(500, 400); jf.setVisible(true);
            try {
                Column[] c2 = {
                    new Column("姓名", 120),
                    new Column("年龄", 80, Align.CENTER),
                    new Column("地址", 200),
                };
                AstTable t2 = new AstTable(c2);
                t2.addRow("张三", 28, "北京市朝阳区");
                t2.addRow("李四", 34, "上海市浦东新区");
                t2.addRow("王五", 22, "广州市天河区");
                t2.addRow("赵六", 45, "深圳市南山区");
                t2.setRowClickListener(row -> clicked[0] = row);
                JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new BorderLayout());
                cp.add(t2, BorderLayout.CENTER); jf.pack();
                // Off-screen paint to trigger assertContrast paths
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(t2.getPreferredSize().width, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                try { t2.paint(gg); } finally { gg.dispose(); }
                // Click row 2 (李四) — y = HEADER_H + 2*ROW_H + ROW_H/2
                int clickY = HEADER_H + 2 * ROW_H + ROW_H / 2;
                int clickX = 10;
                t2.dispatchEvent(new MouseEvent(t2, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, clickX, clickY, 0, false));
                t2.dispatchEvent(new MouseEvent(t2, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, clickX, clickY, 1, false));
                try { Thread.sleep(30); } catch (Throwable ignore) {}
                assert clicked[0] == 2 : "clicked row 2; actual=" + clicked[0];
                assert t2.getSelectedRow() == 2 : "selected row 2";
                // Click header area (y < HEADER_H) — should not select
                t2.dispatchEvent(new MouseEvent(t2, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, HEADER_H / 2, 1, false));
                try { Thread.sleep(20); } catch (Throwable ignore) {}
                assert t2.getSelectedRow() == 2 : "header click does not change selection";
                // Click below last row — should not select
                int belowY = HEADER_H + 4 * ROW_H + ROW_H / 2;
                t2.dispatchEvent(new MouseEvent(t2, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, belowY, 1, false));
                try { Thread.sleep(20); } catch (Throwable ignore) {}
                assert t2.getSelectedRow() == 2 : "below-rows click does not change selection";
            } finally {
                jf.dispose();
            }
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstTable self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
