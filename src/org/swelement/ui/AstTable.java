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
import java.util.Set;
import java.util.function.Consumer;

/**
 * 表格组件 — Element UI Table 的 Java 实现（P4-C 重构为分层容器）。
 *
 * 结构：AstTable(JPanel, BorderLayout) 内含
 *   - HeaderView(NORTH) 吸顶表头
 *   - BodyView(CENTER)  自管视口(scrollY)，绘制数据行
 *   - FooterView(SOUTH) 合计行（C8 启用）
 * 数据与视图抽离到 {@link AstTableModel}；列模型为 {@link AstTableColumn}。
 *
 * 用法：
 *   AstTableColumn[] cols = {
 *       new AstTableColumn("姓名", 120),
 *       new AstTableColumn("年龄", 80, AstTable.Align.CENTER),
 *       new AstTableColumn("地址", 200),
 *   };
 *   AstTable table = new AstTable(cols);
 *   table.addRow("张三", 28, "北京市朝阳区");
 *
 * 设计要点：表头行高 44/36/32、数据行高 40/32/28（尺寸档位 R1）。
 * 斑马纹、hover 高亮、点击选中均经 AstTableModel 视图行。
 */
public class AstTable extends JPanel {
    // --- Align（列对齐，供 AstTableColumn 复用）---
    public enum Align { LEFT, CENTER, RIGHT }

    public static final class Column extends AstTableColumn {
        public Column(String title, int width) { super(title, width); }
        public Column(String title, int width, Align align) { super(title, width, align); }
    }

    // --- Fields ---
    private final AstTableModel model;
    private final HeaderView headerView;
    private final BodyView bodyView;
    private final FooterView footerView;
    private Consumer<Integer> rowClickListener;
    private int tier = SIZE_DEFAULT;

    // --- 尺寸档位（对齐 Element UI）---
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    public static final AstTableModel.SelectionMode SELECTION_SINGLE = AstTableModel.SelectionMode.SINGLE;
    public static final AstTableModel.SelectionMode SELECTION_MULTIPLE = AstTableModel.SelectionMode.MULTIPLE;
    private static final int[] TIER_HEADER_H = {44, 36, 32};
    private static final int[] TIER_ROW_H = {40, 32, 28};
    private static final float[] TIER_FONT = {14f, 14f, 13f};
    private int headerH = 36;
    private int rowH = 32;
    private Font headerFont = ElementTheme.FONT.deriveFont(Font.BOLD, 14f);
    private Font cellFont = ElementTheme.FONT.deriveFont(14f);

    private static final int CELL_PAD_X = 12;
    /** 多选模式选择列宽度（始终冻结在左侧）。 */
    private static final int SELECT_COL_W = 44;
    private boolean multiSelect() { return model.getSelectionMode() == AstTableModel.SelectionMode.MULTIPLE; }
    private int selectColW() { return multiSelect() ? SELECT_COL_W : 0; }

    // --- Constructors ---
    public AstTable(AstTableColumn[] cols) {
        if (cols == null) throw new IllegalArgumentException("cols must not be null");
        if (cols.length == 0) throw new IllegalArgumentException("cols must have at least one column");
        this.model = new AstTableModel(Arrays.asList(cols));
        this.headerView = new HeaderView(); this.bodyView = new BodyView(); this.footerView = new FooterView();
        init();
    }
    public AstTable(Column[] cols) {
        if (cols == null) throw new IllegalArgumentException("cols must not be null");
        if (cols.length == 0) throw new IllegalArgumentException("cols must have at least one column");
        AstTableColumn[] a = new AstTableColumn[cols.length];
        for (int i = 0; i < cols.length; i++) a[i] = cols[i];
        this.model = new AstTableModel(Arrays.asList(a));
        this.headerView = new HeaderView(); this.bodyView = new BodyView(); this.footerView = new FooterView();
        init();
    }
    public AstTable(AstTableModel model) {
        if (model == null) throw new IllegalArgumentException("model must not be null");
        this.model = model;
        this.headerView = new HeaderView(); this.bodyView = new BodyView(); this.footerView = new FooterView();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setFocusable(true);
        applyTier();
        add(headerView, BorderLayout.NORTH);
        add(bodyView, BorderLayout.CENTER);
        add(footerView, BorderLayout.SOUTH);
    }

    public AstTableModel getModel() { return model; }
    public HeaderView getHeaderView() { return headerView; }
    public BodyView getBodyView() { return bodyView; }
    public FooterView getFooterView() { return footerView; }

    // --- 尺寸档位 ---
    public void setSize(int tier) {
        if (tier < SIZE_LARGE || tier > SIZE_SMALL) throw new IllegalArgumentException("tier out of range: " + tier);
        this.tier = tier;
        applyTier();
        revalidate(); repaint();
    }
    private void applyTier() {
        this.headerH = TIER_HEADER_H[tier];
        this.rowH = TIER_ROW_H[tier];
        this.headerFont = ElementTheme.FONT.deriveFont(Font.BOLD, TIER_FONT[tier]);
        this.cellFont = ElementTheme.FONT.deriveFont(TIER_FONT[tier]);
        headerView.applyTier(headerH, headerFont);
        bodyView.applyTier(rowH, cellFont);
    }
    public int getHeaderHeight() { return headerH; }
    public int getRowHeight() { return rowH; }
    public int getTier() { return tier; }

    // --- Public API（向后兼容，转发到 model / bodyView）---
    public void addRow(Object... values) { model.addRow(values); revalidate(); repaint(); }
    public void setRows(List<Object[]> data) { model.setRows(data); revalidate(); repaint(); }
    public void clearRows() { model.clearRows(); revalidate(); repaint(); }
    public int getRowCount() { return model.viewRowCount(); }
    public int getColumnCount() { return model.leafCount(); }
    public Object getValueAt(int row, int col) { return model.getValueAtView(row, col); }
    public int getSelectedRow() { return model.getSelectedViewRow(); }
    public void setSelectedRow(int row) {
        if (row < -1 || row >= model.viewRowCount()) throw new IndexOutOfBoundsException("row " + row);
        model.setSelectedViewRow(row); repaint();
    }
    /** 设置选择模式（单选/多选）；切换会清空当前选择。 */
    public void setSelectionMode(AstTableModel.SelectionMode m) {
        if (m == null) throw new IllegalArgumentException("mode must not be null");
        model.setSelectionMode(m); revalidate(); repaint();
    }
    /** 按叶子列排序（升/降/无）；切换会清空选择。 */
    public void setSort(int leafCol, AstTableModel.SortDir dir) {
        if (dir == null) throw new IllegalArgumentException("dir must not be null");
        model.sort(leafCol, dir); revalidate(); repaint();
    }
    public void setRowClickListener(Consumer<Integer> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.rowClickListener = l;
    }
    public int getTotalWidth() {
        int w = 0;
        for (AstTableColumn c : model.getLeafColumns()) w += c.width;
        return w;
    }
    // 冻结列宽度（供 HeaderView/BodyView 三段式 clip 共用）
    int totalLeafW() {
        int w = 0;
        for (AstTableColumn c : model.getLeafColumns()) w += c.width;
        return w;
    }
    int frozenLeftW() {
        int w = 0;
        for (AstTableColumn c : model.getLeafColumns()) if (c.fixed == AstTableColumn.Fixed.LEFT) w += c.width;
        return w;
    }
    int frozenRightW() {
        int w = 0;
        for (AstTableColumn c : model.getLeafColumns()) if (c.fixed == AstTableColumn.Fixed.RIGHT) w += c.width;
        return w;
    }
    void fireRowClick(int viewRow) { if (rowClickListener != null) rowClickListener.accept(viewRow); }

    // --- Layout / paint（容器画白底+外边框，视图画内容）---
    @Override public Dimension getPreferredSize() {
        int h = headerH + model.viewRowCount() * rowH + footerView.getPreferredHeight() + 2;
        return new Dimension(getTotalWidth() + selectColW() + 2, h);
    }
    @Override public Dimension getMinimumSize() { return new Dimension(getTotalWidth() + selectColW() + 2, headerH + rowH); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(ElementTheme.BORDER_BASE);
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        g2.dispose();
    }

    // ===================== HeaderView =====================
    public class HeaderView extends JComponent {
        private int hH = 36; private Font hf;
        HeaderView() {
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (multiSelect()) {
                        int scw = selectColW();
                        if (e.getX() >= 0 && e.getX() < scw && e.getY() < getPreferredHeight()) {
                            model.toggleSelectAll(); repaint(); return;
                        }
                    }
                    // 排序：点击 sortable 叶子列循环 NONE→ASC→DESC→NONE
                    int leaf = headerLeafAt(e.getX());
                    if (leaf >= 0 && model.getLeafColumns().get(leaf).sortable && e.getY() < getPreferredHeight()) {
                        AstTableModel.SortDir cur = model.getSortDir();
                        AstTableModel.SortDir next;
                        if (model.getSortLeaf() != leaf || cur == AstTableModel.SortDir.NONE) next = AstTableModel.SortDir.ASC;
                        else if (cur == AstTableModel.SortDir.ASC) next = AstTableModel.SortDir.DESC;
                        else next = AstTableModel.SortDir.NONE;
                        model.sort(leaf, next);
                        repaint();
                    }
                }
            });
        }
        /** 命中测试：表头局部 x → 叶子列索引（含选择列偏移/横滚/左冻结）。 */
        int headerLeafAt(int x) {
            int sx = bodyView.scrollX;
            List<AstTableColumn> leaves = model.getLeafColumns();
            for (int i = 0; i < leaves.size(); i++) {
                AstTableColumn.Fixed f = leaves.get(i).fixed;
                int lx = (f == AstTableColumn.Fixed.LEFT) ? leafX(i) : leafX(i) - sx;
                if (x >= lx && x < lx + leaves.get(i).width) return i;
            }
            return -1;
        }
        void applyTier(int h, Font f) { this.hH = h; this.hf = f; }
        int getDepth() { int d = 1; for (AstTableColumn c : model.getColumns()) d = Math.max(d, c.getDepth()); return d; }
        int getPreferredHeight() { return getDepth() * hH; }
        @Override public Dimension getPreferredSize() { return new Dimension(getTotalWidth(), getPreferredHeight()); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), hh = getPreferredHeight();
            g2.setColor(ElementTheme.PRIMARY);
            g2.fillRect(0, 0, w, hh);
            g2.setColor(ElementTheme.lerp(ElementTheme.PRIMARY, Color.BLACK, 0.15f));
            g2.drawLine(0, hh - 1, w, hh - 1);
            g2.setColor(Color.WHITE); g2.setFont(hf);
            FontMetrics fm = g2.getFontMetrics();
            int scw = selectColW(), flw = frozenLeftW(), frw = frozenRightW(), tlw = totalLeafW();
            int leftBand = scw + flw;
            int sx = bodyView.scrollX;
            // 选择列（多选模式）：全选复选框，始终冻结左侧
            if (scw > 0) {
                g2.clipRect(0, 0, scw, hh);
                boolean all = model.viewRowCount() > 0 && model.getSelectedViewRows().size() == model.viewRowCount();
                drawCheckbox(g2, scw / 2, hh / 2, all);
                g2.setClip(null);
                g2.drawLine(scw, 0, scw, hh);
            }
            if (w - leftBand - frw > 0) { g2.clipRect(leftBand, 0, w - leftBand - frw, hh); paintHeaderGroups(g2, fm, model.getColumns(), -sx, 0); g2.setClip(null); }
            if (leftBand > 0) { g2.clipRect(0, 0, leftBand, hh); paintHeaderGroups(g2, fm, model.getColumns(), scw, 0); g2.setClip(null); g2.drawLine(leftBand, 0, leftBand, hh); }
            if (frw > 0) { g2.clipRect(w - frw, 0, frw, hh); paintHeaderGroups(g2, fm, model.getColumns(), w - tlw - scw, 0); g2.setClip(null); g2.drawLine(w - frw, 0, w - frw, hh); }
            g2.dispose();
        }
        /** 递归绘制多级表头：父列跨其子列宽度居中，叶子列位于其层级行。 */
        private void paintHeaderGroups(Graphics2D g2, FontMetrics fm, List<AstTableColumn> cols, int xOffset, int level) {
            int x = xOffset;
            for (AstTableColumn col : cols) {
                drawHeaderCell(g2, fm, col, x, col.width, level);
                if (col.isLeaf()) x += col.width;
                else { paintHeaderGroups(g2, fm, col.children, x, level + 1); x += col.width; }
            }
        }
        private void drawHeaderCell(Graphics2D g2, FontMetrics fm, AstTableColumn col, int x, int groupW, int level) {
            String text = clipText(g2, col.title, groupW - 2 * CELL_PAD_X);
            int tx = x + (groupW - fm.stringWidth(text)) / 2;
            int ty = level * hH + (hH - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
            // 可排序叶子：右侧画方向箭头
            if (col.isLeaf() && col.sortable) {
                int li = model.getLeafColumns().indexOf(col);
                AstTableModel.SortDir d = (li == model.getSortLeaf()) ? model.getSortDir() : AstTableModel.SortDir.NONE;
                int ax = x + groupW - CELL_PAD_X - 4, ayc = level * hH + hH / 2;
                g2.setColor(Color.WHITE);
                if (d == AstTableModel.SortDir.ASC) { g2.drawLine(ax - 4, ayc + 3, ax, ayc - 3); g2.drawLine(ax, ayc - 3, ax + 4, ayc + 3); }
                else if (d == AstTableModel.SortDir.DESC) { g2.drawLine(ax - 4, ayc - 3, ax, ayc + 3); g2.drawLine(ax, ayc + 3, ax + 4, ayc - 3); }
                else { g2.drawLine(ax - 5, ayc - 2, ax - 1, ayc + 2); g2.drawLine(ax - 1, ayc + 2, ax + 3, ayc - 2); }
            }
            // 该行底部分隔线
            g2.drawLine(x, level * hH + hH - 1, x + groupW, level * hH + hH - 1);
            // 叶子列右侧竖分隔
            if (col.isLeaf() && (x + col.width) < getWidth()) {
                Color saved = g2.getColor();
                g2.setColor(ElementTheme.lerp(ElementTheme.PRIMARY, Color.BLACK, 0.1f));
                g2.drawLine(x + col.width - 1, level * hH + 4, x + col.width - 1, level * hH + hH - 4);
                g2.setColor(saved);
            }
        }
        /** 叶子列当前左缘 X（自然坐标，不含横滚）。 */
        int leafX(int leaf) {
            List<AstTableColumn> leaves = model.getLeafColumns();
            int x = selectColW();
            for (int i = 0; i < leaf && i < leaves.size(); i++) x += leaves.get(i).width;
            return x;
        }
    }

    // ===================== BodyView =====================
    public class BodyView extends JComponent {
        private int rH = 32; private Font cf;
        int scrollX = 0;          // 横向滚动（包可见，供 HeaderView 对齐 + selfCheck）
        private int scrollY = 0;
        private int hoverRow = -1;
        private float hoverAlpha = 0f;
        private final Animator hoverAnim;

        BodyView() {
            setOpaque(false);
            hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); } },
                new Animator.Listener() { public void update(float v) { hoverAlpha = v; repaint(); }});
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hoverRow = -1; hoverAnim.stop(); hoverAnim.go(hoverAlpha, 0f);
                }
                @Override public void mousePressed(MouseEvent e) {
                    int idx = viewRowAtPoint(e.getPoint());
                    if (idx < 0 || idx >= model.viewRowCount()) return;
                    if (model.getSelectionMode() == AstTableModel.SelectionMode.MULTIPLE) model.toggleSelectedViewRow(idx);
                    else model.setSelectedViewRow(idx);
                    repaint();
                    fireRowClick(idx);
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int idx = viewRowAtPoint(e.getPoint());
                    if (idx != hoverRow) {
                        hoverRow = idx;
                        hoverAnim.stop(); hoverAnim.go(hoverAlpha, idx >= 0 ? 1f : 0f);
                    }
                }
            });
            addMouseWheelListener(new MouseWheelListener() {
                @Override public void mouseWheelMoved(MouseWheelEvent e) {
                    int d = e.getWheelRotation() * rH;
                    if (e.isShiftDown()) {
                        int max = maxScrollX();
                        scrollX = Math.max(0, Math.min(scrollX + d, max));
                    } else {
                        int max = maxScrollY();
                        scrollY = Math.max(0, Math.min(scrollY + d, max));
                    }
                    repaint();
                }
            });
        }
        void applyTier(int h, Font f) { this.rH = h; this.cf = f; }

        int viewportH() { return getHeight(); }
        int viewportW() { return getWidth(); }
        int maxScrollY() { return Math.max(0, model.viewRowCount() * rH - viewportH()); }
        int maxScrollX() { return Math.max(0, totalLeafW() + selectColW() - viewportW()); }
        void scrollToRow(int v) {
            int target = v * rH - (viewportH() - rH) / 2;
            scrollY = Math.max(0, Math.min(target, maxScrollY()));
            repaint();
        }
        int lastViewRowVisible() {
            if (model.viewRowCount() == 0) return -1;
            int last = (scrollY + viewportH() - 1) / rH;
            return Math.min(last, model.viewRowCount() - 1);
        }
        private int viewRowAtPoint(Point p) {
            if (p.y < 0) return -1;
            int v = (p.y + scrollY) / rH;
            if (v < 0 || v >= model.viewRowCount()) return -1;
            return v;
        }
        private int leafNaturalX(int leaf) {
            List<AstTableColumn> leaves = model.getLeafColumns();
            int x = selectColW();
            for (int i = 0; i < leaf && i < leaves.size(); i++) x += leaves.get(i).width;
            return x;
        }
        /** 叶子列当前屏幕左缘 X（含冻结/横滚）。 */
        int leafXOnScreen(int leaf) {
            List<AstTableColumn> leaves = model.getLeafColumns();
            if (leaf < 0 || leaf >= leaves.size()) return -1;
            int nx = leafNaturalX(leaf);
            AstTableColumn.Fixed f = leaves.get(leaf).fixed;
            if (f == AstTableColumn.Fixed.LEFT) return nx;
            if (f == AstTableColumn.Fixed.RIGHT) return nx + (viewportW() - totalLeafW() - selectColW());
            return nx - scrollX;
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(getTotalWidth(), model.viewRowCount() * rH);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int w = getWidth(), h = getHeight();
            List<AstTableColumn> leaves = model.getLeafColumns();
            int scw = selectColW(), flw = frozenLeftW(), frw = frozenRightW(), tlw = totalLeafW();
            int leftBand = scw + flw;
            // 2) 左冻结（含选择列右侧区域）：先画，使选择列覆盖其上
            if (leftBand > 0) {
                g2.clipRect(0, 0, leftBand, h);
                for (int v = 0; v < model.viewRowCount(); v++) {
                    int yTop = v * rH - scrollY;
                    if (yTop > h || yTop + rH < 0) continue;
                    paintRow(g2, v, yTop, w, leaves, scw, c -> c.fixed == AstTableColumn.Fixed.LEFT);
                }
                g2.setClip(null);
                g2.drawLine(leftBand, 0, leftBand, h);
            }
            // 选择列（多选模式）：覆盖在左冻结区最左侧，绘制行复选框
            if (scw > 0) {
                g2.clipRect(0, 0, scw, h);
                for (int v = 0; v < model.viewRowCount(); v++) {
                    int yTop = v * rH - scrollY;
                    if (yTop > h || yTop + rH < 0) continue;
                    boolean sel = model.isSelectedView(v);
                    Color bg = sel ? ElementTheme.PRIMARY : (v % 2 == 1 ? ElementTheme.FILL_BASE : Color.WHITE);
                    g2.setColor(bg); g2.fillRect(0, yTop, scw, rH);
                    drawCheckbox(g2, scw / 2, yTop + rH / 2, sel);
                }
                g2.setClip(null);
                g2.drawLine(scw, 0, scw, h);
            }
            // 1) 中列
            if (w - leftBand - frw > 0) {
                g2.clipRect(leftBand, 0, w - leftBand - frw, h);
                for (int v = 0; v < model.viewRowCount(); v++) {
                    int yTop = v * rH - scrollY;
                    if (yTop > h || yTop + rH < 0) continue;
                    paintRow(g2, v, yTop, w, leaves, -scrollX, null);
                }
                g2.setClip(null);
            }
            // 3) 右冻结
            if (frw > 0) {
                g2.clipRect(w - frw, 0, frw, h);
                for (int v = 0; v < model.viewRowCount(); v++) {
                    int yTop = v * rH - scrollY;
                    if (yTop > h || yTop + rH < 0) continue;
                    paintRow(g2, v, yTop, w, leaves, w - tlw - scw, c -> c.fixed == AstTableColumn.Fixed.RIGHT);
                }
                g2.setClip(null);
                g2.drawLine(w - frw, 0, w - frw, h);
            }
            g2.dispose();
        }
        private void paintRow(Graphics2D g2, int v, int y, int w, List<AstTableColumn> leaves, int xOffset, java.util.function.Predicate<AstTableColumn> filter) {
            boolean selected = model.isSelectedView(v);
            boolean isHovered = (v == hoverRow) && hoverAlpha > 0.01f;
            boolean zebra = (v % 2 == 1);
            Color bg = Color.WHITE, textColor = ElementTheme.TEXT_MAIN;
            if (selected) { bg = ElementTheme.PRIMARY; textColor = Color.WHITE; }
            else {
                if (zebra) { bg = ElementTheme.FILL_BASE; ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, ElementTheme.FILL_BASE, "AstTable zebra row"); }
                else { ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstTable white row"); }
                if (isHovered) textColor = ElementTheme.lerp(ElementTheme.TEXT_MAIN, ElementTheme.PRIMARY, hoverAlpha * 0.7f);
            }
            g2.setColor(bg); g2.fillRect(0, y, w, rH);
            if (!selected && isHovered) {
                int a = Math.round(18 * hoverAlpha);
                g2.setColor(new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), a));
                g2.fillRect(0, y, w, rH);
            }
            g2.setColor(textColor); g2.setFont(cf);
            FontMetrics fm = g2.getFontMetrics();
            int x = xOffset;
            for (int c = 0; c < leaves.size(); c++) {
                AstTableColumn col = leaves.get(c);
                if (filter == null || filter.test(col)) {
                    String text = clipText(g2, String.valueOf(model.getValueAtView(v, c)), col.width - 2 * CELL_PAD_X);
                    int tx = alignX(x, col.width, fm.stringWidth(text), col.align, CELL_PAD_X);
                    int ty = y + (rH - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(text, tx, ty);
                    if (x + col.width < w) {
                        Color saved = g2.getColor();
                        g2.setColor(ElementTheme.BORDER_BASE);
                        g2.drawLine(x + col.width - 1, y + 4, x + col.width - 1, y + rH - 4);
                        g2.setColor(saved);
                    }
                }
                x += col.width;
            }
            if (!zebra && !selected) {
                g2.setColor(ElementTheme.lerp(ElementTheme.BORDER_BASE, Color.WHITE, 0.5f));
                g2.drawLine(0, y + rH - 1, w, y + rH - 1);
            }
        }
    }

    // ===================== FooterView（C8 启用）=====================
    public class FooterView extends JComponent {
        int getPreferredHeight() { return 0; }
        @Override public Dimension getPreferredSize() { return new Dimension(getTotalWidth(), getPreferredHeight()); }
        @Override protected void paintComponent(Graphics g) { /* C8 实现合计绘制 */ }
    }

    // --- 文本/对齐工具 ---
    private static int alignX(int cellX, int cellW, int textW, Align align, int pad) {
        if (align == Align.CENTER) return cellX + (cellW - textW) / 2;
        if (align == Align.RIGHT) return cellX + cellW - textW - pad;
        return cellX + pad;
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
    /** 绘制一个 16×16 复选框（白底 + PRIMARY 勾选）。供选择列复用。 */
    private void drawCheckbox(Graphics2D g2, int cx, int cy, boolean checked) {
        int s = 16, x = cx - s / 2, y = cy - s / 2;
        g2.setColor(Color.WHITE); g2.fillRect(x, y, s, s);
        g2.setColor(ElementTheme.BORDER_BASE); g2.drawRect(x, y, s, s);
        if (checked) {
            g2.setColor(ElementTheme.PRIMARY);
            g2.drawLine(x + 3, cy, x + 6, y + s - 4);
            g2.drawLine(x + 6, y + s - 4, x + s - 3, y + 3);
        }
    }

    // --- Self-check ---
    static void selfCheck() {
        // Constructor null guards
        boolean threw = false;
        try { new AstTable((Column[]) null); } catch (IllegalArgumentException e) { threw = true; }
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

        // Build a table (旧 API 兼容)
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

        table.setSelectedRow(1);
        assert table.getSelectedRow() == 1 : "selected row 1";
        table.setSelectedRow(-1);
        assert table.getSelectedRow() == -1 : "cleared selection";

        table.clearRows();
        assert table.getRowCount() == 0 : "0 rows after clear";

        List<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{"a", 1, "x"});
        data.add(new Object[]{"b", 2, "y"});
        table.setRows(data);
        assert table.getRowCount() == 2 : "2 rows after setRows";
        assert "b".equals(table.getValueAt(1, 0)) : "row1 col0 = b";
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

        // Listener test on EDT（点击派发到 BodyView，坐标为 body 局部）
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
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(t2.getPreferredSize().width, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                try { t2.paint(gg); } finally { gg.dispose(); }
                int rh = t2.getRowHeight();
                int clickY = 2 * rh + rh / 2; // body 局部：第 2 行(李四)中心
                int clickX = 10;
                t2.getBodyView().dispatchEvent(new MouseEvent(t2.getBodyView(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, clickX, clickY, 0, false));
                t2.getBodyView().dispatchEvent(new MouseEvent(t2.getBodyView(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, clickX, clickY, 1, false));
                try { Thread.sleep(30); } catch (Throwable ignore) {}
                assert clicked[0] == 2 : "clicked row 2; actual=" + clicked[0];
                assert t2.getSelectedRow() == 2 : "selected row 2";
                // 点击表头区域（body 局部 y<0）→ 不改变选择
                t2.getBodyView().dispatchEvent(new MouseEvent(t2.getBodyView(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 10, -5, 1, false));
                try { Thread.sleep(20); } catch (Throwable ignore) {}
                assert t2.getSelectedRow() == 2 : "header click does not change selection";
                // 点击末行之后 → 不改变选择
                int belowY = 4 * rh + rh / 2;
                t2.getBodyView().dispatchEvent(new MouseEvent(t2.getBodyView(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 10, belowY, 1, false));
                try { Thread.sleep(20); } catch (Throwable ignore) {}
                assert t2.getSelectedRow() == 2 : "below-rows click does not change selection";
            } finally {
                jf.dispose();
            }
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);

        // 尺寸档位（R1）
        AstTable tb3 = new AstTable(new Column[]{ new Column("姓名", 120), new Column("年龄", 80) });
        tb3.setSize(AstTable.SIZE_LARGE);
        int hHL = tb3.getHeaderHeight(), rHL = tb3.getRowHeight();
        tb3.setSize(AstTable.SIZE_DEFAULT);
        int hHD = tb3.getHeaderHeight(), rHD = tb3.getRowHeight();
        tb3.setSize(AstTable.SIZE_SMALL);
        int hHS = tb3.getHeaderHeight(), rHS = tb3.getRowHeight();
        assert hHL > hHD && hHD > hHS : "AstTable 档位表头高度应单调递减, got " + hHL + "," + hHD + "," + hHS;
        assert rHL > rHD && rHD > rHS : "AstTable 档位行高应单调递减, got " + rHL + "," + rHD + "," + rHS;
        threw = false;
        try { tb3.setSize(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "AstTable 非法档位应抛异常";

        // --- C1: 列模型 + 模型 + 容器重构 + 纵向滚动 ---
        AstTableColumn c0 = new AstTableColumn("姓名", 120, Align.LEFT);
        AstTableColumn c1 = new AstTableColumn("年龄", 80, Align.CENTER);
        AstTableColumn c2 = new AstTableColumn("地址", 200, Align.LEFT);
        AstTable t1 = new AstTable(new AstTableColumn[]{c0, c1, c2});
        assert t1.getColumnCount() == 3 : "C1 列数=3";
        t1.addRow("张三", 28, "北京市");
        t1.addRow("李四", 34, "上海市");
        assert t1.getRowCount() == 2 : "C1 行数=2";
        assert "张三".equals(t1.getValueAt(0, 0)) : "C1 getValueAt 兼容";
        assert Integer.valueOf(34).equals(t1.getValueAt(1, 1)) : "C1 数值列";

        final int[] lastVisibleArr = {-1};
        final Throwable[] err1 = {null};
        try { SwingUtilities.invokeAndWait(new Runnable(){ public void run(){
            JFrame jf = new JFrame("C1"); jf.setSize(500, 160); jf.setVisible(true);
            try {
                AstTable tt = new AstTable(new AstTableColumn[]{
                    new AstTableColumn("姓名",120), new AstTableColumn("年龄",80), new AstTableColumn("地址",200)});
                for (int i=0;i<20;i++) tt.addRow("u"+i, i, "addr"+i);
                JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new BorderLayout());
                cp.add(tt, BorderLayout.CENTER); jf.validate();
                int headerTop = tt.getHeaderView().getY();
                int beforeScrollLast = tt.getBodyView().lastViewRowVisible();
                tt.getBodyView().scrollToRow(19);
                int afterScrollLast = tt.getBodyView().lastViewRowVisible();
                assert headerTop == 0 : "C1 表头吸顶 Y=0";
                assert afterScrollLast > beforeScrollLast : "C1 滚动后可见末行后移";
                assert tt.getBodyView().lastViewRowVisible() == 19 : "C1 能滚到末行(19)";
            } finally { jf.dispose(); }
        }}); } catch (Throwable t){ err1[0]=t; }
        if (err1[0]!=null) throw new RuntimeException(err1[0]);

        // --- C2: 横向滚动 + 左冻结列 ---
        final int[] c2res = {0,0,0,0};
        final Throwable[] err2 = {null};
        try { SwingUtilities.invokeAndWait(new Runnable(){ public void run(){
            JFrame jf = new JFrame("C2"); jf.setSize(360, 200); jf.setVisible(true);
            try {
                AstTableColumn name = new AstTableColumn("姓名",100,Align.LEFT,false,AstTableColumn.Fixed.LEFT,null);
                AstTableColumn age  = new AstTableColumn("年龄",90,Align.CENTER);
                AstTableColumn addr = new AstTableColumn("地址",260,Align.LEFT);
                AstTableColumn tag  = new AstTableColumn("标签",260,Align.LEFT);
                AstTable tt = new AstTable(new AstTableColumn[]{name,age,addr,tag});
                for (int i=0;i<10;i++) tt.addRow("n"+i,i,"a-long-address-"+i,"t"+i);
                JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new BorderLayout());
                cp.add(tt, BorderLayout.CENTER); jf.validate();
                int leafW = tt.frozenLeftW();
                c2res[0] = leafW;
                int xBefore = tt.getBodyView().leafXOnScreen(1); // 年龄(中列)
                tt.getBodyView().scrollX = 120; tt.getBodyView().repaint();
                int xNameAfter = tt.getBodyView().leafXOnScreen(0); // 姓名(冻结)
                int xAgeAfter  = tt.getBodyView().leafXOnScreen(1);  // 年龄(中列)
                c2res[1] = xNameAfter; c2res[2] = xAgeAfter; c2res[3] = xBefore;
                assert leafW == 100 : "C2 左冻结宽=100, got "+leafW;
                assert xNameAfter == 0 : "C2 冻结列横滚后仍贴左(0), got "+xNameAfter;
                assert xAgeAfter < xBefore : "C2 中列横滚后左移, before="+xBefore+" after="+xAgeAfter;
                assert leafW + tt.frozenRightW() < tt.getWidth() : "C2 冻结宽小于视口";
            } finally { jf.dispose(); }
        }}); } catch (Throwable t){ err2[0]=t; }
        if (err2[0]!=null) throw new RuntimeException(err2[0]);

        // --- C3: 多级表头 ---
        AstTableColumn c3name = new AstTableColumn("姓名", 100);
        AstTableColumn c3city = new AstTableColumn("城市", 120);
        AstTableColumn c3street = new AstTableColumn("街道", 160);
        AstTableColumn c3addr = new AstTableColumn("地址", Arrays.asList(c3city, c3street));
        AstTableColumn c3age = new AstTableColumn("年龄", 80);
        AstTable t3 = new AstTable(new AstTableColumn[]{c3name, c3addr, c3age});
        assert t3.getHeaderView().getDepth() == 2 : "C3 层级=2";
        List<AstTableColumn> c3leaves = t3.getModel().getLeafColumns();
        assert c3leaves.size() == 4 : "C3 叶子=4";
        assert c3leaves.get(1) == c3city && c3leaves.get(2) == c3street : "C3 叶子顺序 城市→街道";
        assert t3.getHeaderView().leafX(1) == 100 : "C3 城市列X=100, got " + t3.getHeaderView().leafX(1);
        assert t3.getHeaderView().leafX(2) == 220 : "C3 街道X=220, got " + t3.getHeaderView().leafX(2);

        // --- C4: 单选 / 多选 ---
        final Throwable[] err4 = {null};
        try { SwingUtilities.invokeAndWait(new Runnable(){ public void run(){
            JFrame jf = new JFrame("C4"); jf.setSize(420, 200); jf.setVisible(true);
            try {
                AstTable t4 = new AstTable(new AstTableColumn[]{
                    new AstTableColumn("姓名", 120), new AstTableColumn("年龄", 80)});
                for (int i = 0; i < 5; i++) t4.addRow("u" + i, i);
                t4.setSelectionMode(AstTable.SELECTION_MULTIPLE);
                JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new BorderLayout());
                cp.add(t4, BorderLayout.CENTER); jf.validate();
                int rh = t4.getRowHeight();
                int y2 = 2 * rh + rh / 2; // body 局部：第2行中心
                t4.getBodyView().dispatchEvent(new MouseEvent(t4.getBodyView(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 30, y2, 1, false));
                assert t4.getModel().isSelectedView(2) : "C4 多选点中行2";
                int y3 = 3 * rh + rh / 2;
                t4.getBodyView().dispatchEvent(new MouseEvent(t4.getBodyView(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 30, y3, 1, false));
                assert t4.getModel().isSelectedView(2) && t4.getModel().isSelectedView(3) : "C4 多选累加";
                t4.setSelectionMode(AstTable.SELECTION_SINGLE);
                int y1 = 1 * rh + rh / 2;
                t4.getBodyView().dispatchEvent(new MouseEvent(t4.getBodyView(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 30, y1, 1, false));
                assert t4.getModel().getSelectedViewRow() == 1 && t4.getModel().getSelectedViewRows().size() == 1 : "C4 单选互斥";
                // 离屏绘制不抛异常（含选择列）
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(t4.getPreferredSize().width, 160, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                try { t4.paint(gg); } finally { gg.dispose(); }
            } finally { jf.dispose(); }
        }}); } catch (Throwable t){ err4[0] = t; }
        if (err4[0] != null) throw new RuntimeException(err4[0]);

        // --- C5: 排序（升/降/无 + 表头三角）---
        AstTable t5 = new AstTable(new AstTableColumn[]{
            new AstTableColumn("姓名", 120),
            new AstTableColumn("年龄", 80, Align.CENTER, true, AstTableColumn.Fixed.NONE, null)});
        t5.addRow("王", 22); t5.addRow("张", 34); t5.addRow("李", 28);
        t5.getModel().sort(1, AstTableModel.SortDir.ASC);
        assert (Integer) t5.getValueAt(0, 1) == 22 && (Integer) t5.getValueAt(1, 1) == 28 && (Integer) t5.getValueAt(2, 1) == 34 : "C5 升序 22,28,34";
        t5.getModel().sort(1, AstTableModel.SortDir.DESC);
        assert (Integer) t5.getValueAt(0, 1) == 34 && (Integer) t5.getValueAt(2, 1) == 22 : "C5 降序 34,22";
        t5.getModel().sort(1, AstTableModel.SortDir.NONE);
        assert (Integer) t5.getValueAt(0, 1) == 22 : "C5 NONE 还原原始序";
        // 离屏绘制不抛异常（含排序箭头）
        java.awt.image.BufferedImage img5 = new java.awt.image.BufferedImage(t5.getPreferredSize().width, 90, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg5 = img5.createGraphics();
        try { t5.paint(gg5); } finally { gg5.dispose(); }

        System.out.println("AstTable self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
