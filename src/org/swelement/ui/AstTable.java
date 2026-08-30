package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.framework.AstContainerComponent;
import org.swelement.framework.AstDisplayComponent;
import org.swelement.framework.AstInteractiveComponent;

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
public class AstTable extends AstContainerComponent {
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
    private final java.util.Map<Integer, String> filterQueries = new java.util.HashMap<Integer, String>();
    private java.util.function.Function<Integer, String> expandTextFn = null;
    private java.util.function.Function<Integer, JComponent> expandRenderer = null;

    // --- 尺寸档位（对齐 Element UI）---
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    public static final AstTableModel.SelectionMode SELECTION_SINGLE = AstTableModel.SelectionMode.SINGLE;
    public static final AstTableModel.SelectionMode SELECTION_MULTIPLE = AstTableModel.SelectionMode.MULTIPLE;
    private static final int[] TIER_HEADER_H = {44, 36, 32};
    private static final int[] TIER_ROW_H = {40, 32, 28};
    private static final float[] TIER_FONT = {14f, 14f, 13f};
    private int headerH = 36;
    private int rowH = 32;
    private Font headerFont;
    private Font cellFont;

    private static final int CELL_PAD_X = 12;
    /** 多选模式选择列宽度（始终冻结在左侧）。 */
    private static final int SELECT_COL_W = 44;
    /** 展开行区块高度（C7）。 */
    private static final int EXPAND_H = 80;
    /** 展开行按钮宽度（第一列行首三角）。 */
    private static final int EXPAND_BTN_W = 16;
    /** 横向滚动条高度。 */
    private static final int HSCROLL_H = 8;
    /** 三段式 clip 的列过滤器：每个冻结带只画自己那一类列，避免同一列被重复绘制。 */
    private static final java.util.function.Predicate<AstTableColumn> PRED_LEFT =
        c -> c.fixed == AstTableColumn.Fixed.LEFT;
    private static final java.util.function.Predicate<AstTableColumn> PRED_NONE =
        c -> c.fixed == AstTableColumn.Fixed.NONE;
    private static final java.util.function.Predicate<AstTableColumn> PRED_RIGHT =
        c -> c.fixed == AstTableColumn.Fixed.RIGHT;
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
        this.headerFont = theme().getFontBase().deriveFont(Font.BOLD, TIER_FONT[tier]);
        this.cellFont = theme().getFontBase().deriveFont(TIER_FONT[tier]);
        headerView.applyTier(headerH, headerFont);
        bodyView.applyTier(rowH, cellFont);
        footerView.applyTier(rowH, cellFont);
    }
    public int getHeaderHeight() { return headerH; }
    public int getRowHeight() { return rowH; }
    public int getTier() { return tier; }

    // --- Public API（向后兼容，转发到 model / bodyView）---
    public void addRow(Object... values) { model.addRow(values); revalidate(); repaint(); }
    public void setRows(List<Object[]> data) { model.setRows(data); revalidate(); repaint(); }
    public void clearRows() { model.clearRows(); revalidate(); repaint(); }
    public int getRowCount() { return model.viewRowCount() + model.expandedCount(); }
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
    /**
     * 按叶子列排序（升/降/无）；切换会清空选择。
     * 必须走本方法（而非直接 model.sort）以保证整表重绘 —— 只 repaint HeaderView 时数据行不会刷新。
     */
    public void setSort(int leafCol, AstTableModel.SortDir dir) {
        if (dir == null) throw new IllegalArgumentException("dir must not be null");
        sortApplyCount++;
        model.sort(leafCol, dir); revalidate(); repaint();
    }
    /** 经 {@link #setSort} 应用的排序次数（selfCheck 用于锁定「点击表头必须整表刷新」）。 */
    int sortApplyCount = 0;
    /** 按叶子列文本子串筛选；空查询清空该列筛选。 */
    public void setFilter(int leafCol, String query) {
        if (leafCol < 0 || leafCol >= model.leafCount()) throw new IndexOutOfBoundsException("leafCol " + leafCol);
        if (query == null || query.isEmpty()) { filterQueries.remove(leafCol); model.clearFilter(); }
        else {
            final String q = query; final int col = leafCol;
            filterQueries.put(leafCol, q);
            model.filter(r -> String.valueOf(r[col]).contains(q));
        }
        revalidate(); repaint();
    }
    public void clearFilter() { filterQueries.clear(); model.clearFilter(); revalidate(); repaint(); }

    /** 设置展开行文本渲染（按 raw 行返回字符串）。 */
    public void setRowExpandText(java.util.function.Function<Integer, String> fn) {
        if (fn == null) throw new IllegalArgumentException("fn must not be null");
        this.expandTextFn = fn; this.expandRenderer = null; revalidate(); repaint();
    }
    /** 设置展开行组件渲染（按 raw 行返回 JComponent）。 */
    @SuppressWarnings("unchecked")
    public void setRowExpandRenderer(java.util.function.Function<Integer, ? extends JComponent> fn) {
        if (fn == null) throw new IllegalArgumentException("fn must not be null");
        this.expandRenderer = (java.util.function.Function<Integer, JComponent>) fn;
        this.expandTextFn = null; revalidate(); repaint();
    }
    /** 注册表尾合计聚合器（null 表示默认求和）。有合计时 FooterView 可见。 */
    public void setSummary(int leafCol, AstTableModel.Aggregator agg) {
        if (leafCol < 0 || leafCol >= model.leafCount()) throw new IndexOutOfBoundsException("leafCol " + leafCol);
        model.setSummary(leafCol, agg);
        footerView.setVisible(model.hasSummary());
        revalidate(); repaint();
    }
    /** 清除全部合计并隐藏 FooterView。 */
    public void clearSummary() {
        model.clearSummary();
        footerView.setVisible(false);
        revalidate(); repaint();
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
        int h = headerH + (model.viewRowCount() * rowH + model.expandedCount() * EXPAND_H) + footerView.getPreferredHeight() + 2;
        return new Dimension(getTotalWidth() + selectColW() + 2, h);
    }
    @Override public Dimension getMinimumSize() { return new Dimension(getTotalWidth() + selectColW() + 2, headerH + rowH); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(theme().getBorderBase());
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        g2.dispose();
    }

    // ===================== HeaderView =====================
    public class HeaderView extends AstInteractiveComponent {
        private int hH = 36; private Font hf;
        HeaderView() {
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (multiSelect()) {
                        int scw = selectColW();
                        if (e.getX() >= 0 && e.getX() < scw && e.getY() < getPreferredHeight()) {
                            model.toggleSelectAll(); repaint(); return;
                        }
                    }
                    // 排序/筛选：点击叶子列表头
                    int leaf = headerLeafAt(e.getX());
                    if (leaf >= 0) {
                        AstTableColumn col = model.getLeafColumns().get(leaf);
                        int sx = bodyView.scrollX;
                        int lx = (col.fixed == AstTableColumn.Fixed.LEFT) ? leafX(leaf) : leafX(leaf) - sx;
                        if (col.filterable && e.getX() >= lx + col.width - 16 && e.getY() < getPreferredHeight()) {
                            openFilterPopup(leaf, lx); return;
                        }
                        if (col.sortable && e.getY() < getPreferredHeight()) {
                            AstTableModel.SortDir cur = model.getSortDir();
                            AstTableModel.SortDir next;
                            if (model.getSortLeaf() != leaf || cur == AstTableModel.SortDir.NONE) next = AstTableModel.SortDir.ASC;
                            else if (cur == AstTableModel.SortDir.ASC) next = AstTableModel.SortDir.DESC;
                            else next = AstTableModel.SortDir.NONE;
                            setSort(leaf, next); // 含 revalidate + 整表 repaint（只 repaint 表头不会刷新数据行）
                        }
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
        /** 点击漏斗：弹出内联文本输入框，回车即按该列文本子串筛选。 */
        private void openFilterPopup(int leaf, int lx) {
            JPopupMenu pm = new JPopupMenu();
            final JTextField tf = new JTextField(filterQueries.getOrDefault(leaf, ""), 12);
            tf.addActionListener(ae -> { setFilter(leaf, tf.getText().trim()); pm.setVisible(false); });
            pm.add(tf);
            pm.show(this, Math.max(0, lx), getHeight());
            tf.requestFocusInWindow();
        }
        void applyTier(int h, Font f) { this.hH = h; this.hf = f; }
        int getDepth() { int d = 1; for (AstTableColumn c : model.getColumns()) d = Math.max(d, c.getDepth()); return d; }
        int getPreferredHeight() { return getDepth() * hH; }
        @Override public Dimension getPreferredSize() { return new Dimension(getTotalWidth(), getPreferredHeight()); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            int w = getWidth(), hh = getPreferredHeight(), depth = getDepth();
            Color sep = lerp(theme().getPrimary(), Color.BLACK, 0.15f);
            g2.setColor(theme().getPrimary());
            g2.fillRect(0, 0, w, hh);
            g2.setColor(sep);
            g2.drawLine(0, hh - 1, w, hh - 1);
            int scw = selectColW(), flw = frozenLeftW(), frw = frozenRightW(), tlw = totalLeafW();
            int leftBand = scw + flw;
            int sx = bodyView.scrollX;
            // 选择列（多选模式）：全选复选框，始终冻结左侧
            if (scw > 0) {
                g2.clipRect(0, 0, scw, hh);
                boolean all = model.viewRowCount() > 0 && model.getSelectedViewRows().size() == model.viewRowCount();
                drawCheckbox(g2, scw / 2, hh / 2, all);
                g2.setClip(null);
                g2.setColor(sep); g2.drawLine(scw, 0, scw, hh);
            }
            // 三个冻结带：各只画本带的列，避免同一列被重复绘制（视口宽于内容时会重复）
            drawHeaderBand(g2, hh, depth, leftBand, w - frw, -sx, AstTableColumn.Fixed.NONE);
            drawHeaderBand(g2, hh, depth, 0, leftBand, scw, AstTableColumn.Fixed.LEFT);
            drawHeaderBand(g2, hh, depth, w - frw, w, w - tlw - scw, AstTableColumn.Fixed.RIGHT);
            if (leftBand > 0) { g2.setColor(sep); g2.drawLine(leftBand, 0, leftBand, hh); }
            if (frw > 0) { g2.setColor(sep); g2.drawLine(w - frw, 0, w - frw, hh); }
            g2.dispose();
        }
        /** 在 [x0,x1) 带内绘制表头（只画 fixed==which 的叶子列）。 */
        private void drawHeaderBand(Graphics2D g2, int hh, int depth, int x0, int x1, int xOffset, AstTableColumn.Fixed which) {
            if (x1 - x0 <= 0) return;
            g2.clipRect(x0, 0, x1 - x0, hh);
            g2.setFont(hf);
            FontMetrics fm = g2.getFontMetrics();
            paintHeaderGroups(g2, fm, model.getColumns(), xOffset, 0, depth, which);
            g2.setClip(null);
        }
        /**
         * 递归绘制多级表头：只绘制 fixed==which 的叶子；
         * 父列标题跟随其第一个叶子的带，避免跨带重复。
         */
        private void paintHeaderGroups(Graphics2D g2, FontMetrics fm, List<AstTableColumn> cols, int xOffset, int level, int depth, AstTableColumn.Fixed which) {
            int x = xOffset;
            for (AstTableColumn col : cols) {
                if (col.isLeaf()) {
                    if (col.fixed == which) drawHeaderCell(g2, fm, col, x, col.width, level, depth);
                } else {
                    if (col.getLeafColumns().get(0).fixed == which)
                        drawHeaderCell(g2, fm, col, x, col.width, level, depth);
                    paintHeaderGroups(g2, fm, col.children, x, level + 1, depth, which);
                }
                x += col.width;
            }
        }
        /** 表头单元格：叶子列纵向跨到最底层，父列只占当前层。 */
        private void drawHeaderCell(Graphics2D g2, FontMetrics fm, AstTableColumn col, int x, int groupW, int level, int depth) {
            int rows = col.isLeaf() ? Math.max(1, depth - level) : 1;
            int cellH = rows * hH, top = level * hH, bottom = top + cellH;
            Color sep = lerp(theme().getPrimary(), Color.BLACK, 0.15f);
            g2.setColor(Color.WHITE);
            String text = clipText(g2, col.title, groupW - 2 * CELL_PAD_X);
            int tx = x + (groupW - fm.stringWidth(text)) / 2;
            int ty = top + (cellH - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
            int ayc = top + cellH / 2;
            // 可排序叶子：右侧方向箭头（若有筛选漏斗则左移避让）
            if (col.isLeaf() && col.sortable) {
                int li = model.getLeafColumns().indexOf(col);
                AstTableModel.SortDir d = (li == model.getSortLeaf()) ? model.getSortDir() : AstTableModel.SortDir.NONE;
                int ax = x + groupW - (col.filterable ? 28 : 16);
                g2.setColor(Color.WHITE);
                if (d == AstTableModel.SortDir.ASC) { g2.drawLine(ax - 4, ayc + 3, ax, ayc - 3); g2.drawLine(ax, ayc - 3, ax + 4, ayc + 3); }
                else if (d == AstTableModel.SortDir.DESC) { g2.drawLine(ax - 4, ayc - 3, ax, ayc + 3); g2.drawLine(ax, ayc + 3, ax + 4, ayc - 3); }
                else { g2.drawLine(ax - 5, ayc - 2, ax - 1, ayc + 2); g2.drawLine(ax - 1, ayc + 2, ax + 3, ayc - 2); }
            }
            // 可筛选叶子：最右漏斗图标
            if (col.isLeaf() && col.filterable) {
                int fx = x + groupW - 11;
                g2.setColor(Color.WHITE);
                g2.drawLine(fx - 4, ayc - 4, fx + 4, ayc - 4);
                g2.drawLine(fx - 4, ayc - 4, fx, ayc + 2);
                g2.drawLine(fx + 4, ayc - 4, fx, ayc + 2);
                g2.drawLine(fx, ayc + 2, fx, ayc + 5);
            }
            // 单元格底边框：叶子列画在最底层，保证表头底部一行边框连续
            g2.setColor(sep);
            g2.drawLine(x, bottom - 1, x + groupW, bottom - 1);
            // 叶子列右侧竖分隔（跨整个单元格高度）
            if (col.isLeaf() && (x + col.width) < getWidth()) {
                g2.setColor(lerp(theme().getPrimary(), Color.BLACK, 0.1f));
                g2.drawLine(x + col.width - 1, top + 4, x + col.width - 1, bottom - 4);
            }
        }
        /** 叶子列当前左缘 X（自然坐标，不含横滚）。 */
        int leafX(int leaf) {
            List<AstTableColumn> leaves = model.getLeafColumns();
            int x = selectColW();
            for (int i = 0; i < leaf && i < leaves.size(); i++) x += leaves.get(i).width;
            return x;
        }
        @Override protected void selfCheck() { }
    }

    // ===================== BodyView =====================
    public class BodyView extends AstInteractiveComponent {
        private int rH = 32; private Font cf;
        int scrollX = 0;          // 横向滚动（包可见，供 HeaderView 对齐 + selfCheck）
        private int scrollY = 0;
        private int hoverRow = -1;
        /** 被合并单元格覆盖的格（C9），每次绘制前重算。 */
        private java.util.Set<Long> coveredCells = java.util.Collections.emptySet();
        /** 横向滚动条拖拽状态。 */
        private boolean hScrollDrag = false;
        /** 非空时记录每次 drawCells 实际落笔的 (视图行, 叶子列)；仅 selfCheck 使用。 */
        java.util.List<int[]> paintLog = null;

        @Override
        protected void initComponent() {
            super.initComponent();
            anim.register("hover", 150, Easing::easeInOut);
        }

        BodyView() {
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hoverRow = -1; anim.go("hover", anim.getProgress("hover"), 0f);
                }
                @Override public void mousePressed(MouseEvent e) {
                    // 1) 底部横向滚动条：开始拖拽
                    if (hScrollHit(e.getPoint())) {
                        hScrollDrag = true;
                        setScrollXFromThumb(e.getX());
                        return;
                    }
                    int idx = viewRowAtPoint(e.getPoint());
                    if (idx < 0 || idx >= model.viewRowCount()) return;
                    // 2) 第一列行首展开按钮
                    if (expandable()) {
                        int bx = leafXOnScreen(0);
                        if (e.getX() >= bx && e.getX() <= bx + EXPAND_BTN_W) {
                            model.toggleExpanded(model.rawRowOf(idx));
                            AstTable.this.revalidate(); AstTable.this.repaint();
                            return;
                        }
                    }
                    // 3) 选中
                    if (model.getSelectionMode() == AstTableModel.SelectionMode.MULTIPLE) model.toggleSelectedViewRow(idx);
                    else model.setSelectedViewRow(idx);
                    repaint();
                    fireRowClick(idx);
                }
                @Override public void mouseReleased(MouseEvent e) { hScrollDrag = false; }
                @Override public void mouseClicked(MouseEvent e) {
                    // 双击行切换展开（C7）；未设置展开内容时忽略
                    if (e.getClickCount() != 2) return;
                    if (expandTextFn == null && expandRenderer == null) return;
                    int idx = viewRowAtPoint(e.getPoint());
                    if (idx < 0 || idx >= model.viewRowCount()) return;
                    model.toggleExpanded(model.rawRowOf(idx));
                    AstTable.this.revalidate(); AstTable.this.repaint();
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int idx = viewRowAtPoint(e.getPoint());
                    if (idx != hoverRow) {
                        hoverRow = idx;
                        anim.go("hover", anim.getProgress("hover"), idx >= 0 ? 1f : 0f);
                    }
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (hScrollDrag) setScrollXFromThumb(e.getX());
                }
            });
            addMouseWheelListener(new MouseWheelListener() {
                @Override public void mouseWheelMoved(MouseWheelEvent e) {
                    int d = e.getWheelRotation() * rH;
                    if (e.isShiftDown()) {
                        int max = maxScrollX();
                        scrollX = Math.max(0, Math.min(scrollX + d, max));
                        AstTable.this.repaint(); // 横滚需同步重绘表头
                    } else {
                        int max = maxScrollY();
                        scrollY = Math.max(0, Math.min(scrollY + d, max));
                        repaint();
                    }
                }
            });
        }
        void applyTier(int h, Font f) { this.rH = h; this.cf = f; }

        int viewportW() { return getWidth(); }
        /** 是否有横向滚动条（内容宽于视口）。 */
        boolean hasHScroll() { return maxScrollX() > 0; }
        /** 数据行可用高度（排除底部横向滚动条）。 */
        int viewportH() { return getHeight() - (hasHScroll() ? HSCROLL_H : 0); }
        int maxScrollY() {
            int total = 0;
            for (int v = 0; v < model.viewRowCount(); v++) { total += rH; if (model.isExpandedView(v)) total += EXPAND_H; }
            return Math.max(0, total - viewportH());
        }
        int maxScrollX() { return Math.max(0, totalLeafW() + selectColW() - viewportW()); }

        // --- 横向滚动条（自绘，底部 HSCROLL_H 区域）---
        private int hScrollTrackW() { return Math.max(1, getWidth() - 2); }
        private int hScrollThumbW() {
            int contentW = totalLeafW() + selectColW();
            if (contentW <= 0) return hScrollTrackW();
            int tw = (int) ((double) viewportW() / contentW * hScrollTrackW());
            return Math.max(24, Math.min(tw, hScrollTrackW()));
        }
        private int hScrollThumbX() {
            int max = maxScrollX(), tw = hScrollThumbW(), usable = hScrollTrackW() - tw;
            if (max <= 0 || usable <= 0) return 1;
            return 1 + (int) Math.round((double) scrollX / max * usable);
        }
        private boolean hScrollHit(Point p) { return hasHScroll() && p.y >= getHeight() - HSCROLL_H; }
        /** 按滑块中心对齐鼠标位置换算 scrollX，并同步重绘表头。 */
        private void setScrollXFromThumb(int mouseX) {
            int max = maxScrollX(), tw = hScrollThumbW(), usable = hScrollTrackW() - tw;
            int rel = mouseX - 1 - tw / 2;
            scrollX = usable <= 0 ? 0 : Math.max(0, Math.min(max, (int) Math.round((double) rel / usable * max)));
            AstTable.this.repaint();
        }
        private void paintHScrollBar(Graphics2D g2) {
            int w = getWidth(), y = getHeight() - HSCROLL_H;
            g2.setColor(theme().getFillBase());
            g2.fillRect(0, y, w, HSCROLL_H);
            g2.setColor(lerp(theme().getBorderBase(), Color.WHITE, 0.4f));
            g2.drawLine(0, y, w, y);
            g2.setColor(lerp(theme().getTextPlaceholder(), Color.WHITE, 0.25f));
            g2.fillRoundRect(hScrollThumbX(), y + 1, hScrollThumbW(), HSCROLL_H - 3, 4, 4);
        }
        void scrollToRow(int v) {
            int yy = 0;
            for (int i = 0; i < v && i < model.viewRowCount(); i++) { yy += rH; if (model.isExpandedView(i)) yy += EXPAND_H; }
            scrollY = Math.max(0, Math.min(yy - (viewportH() - rH) / 2, maxScrollY()));
            repaint();
        }
        int lastViewRowVisible() {
            if (model.viewRowCount() == 0) return -1;
            int contentY = scrollY + viewportH(), yy = 0, last = -1;
            for (int v = 0; v < model.viewRowCount(); v++) {
                if (yy + rH <= contentY) last = v;
                yy += rH;
                if (model.isExpandedView(v)) yy += EXPAND_H;
            }
            return last;
        }
        private int viewRowAtPoint(Point p) {
            if (p.y < 0) return -1;
            int contentY = p.y + scrollY, yy = 0;
            for (int v = 0; v < model.viewRowCount(); v++) {
                if (contentY >= yy && contentY < yy + rH) return v;
                yy += rH;
                if (model.isExpandedView(v)) {
                    if (contentY >= yy && contentY < yy + EXPAND_H) return -1; // 展开区不可选
                    yy += EXPAND_H;
                }
            }
            return -1;
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
            int total = 0;
            for (int v = 0; v < model.viewRowCount(); v++) { total += rH; if (model.isExpandedView(v)) total += EXPAND_H; }
            return new Dimension(getTotalWidth() + selectColW(), total);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            int h = viewportH(); // 数据行可用高度（底部留给横向滚动条）
            List<AstTableColumn> leaves = model.getLeafColumns();
            computeCovered(leaves.size());
            // Pass A：行背景（整行宽）。先铺满再画内容，合并单元格跨行才不会被后画的行覆盖。
            int y = -scrollY;
            for (int v = 0; v < model.viewRowCount(); v++) {
                if (y + rH > 0 && y < h) paintRowBg(g2, v, y);
                y += rH;
                if (model.isExpandedView(v)) {
                    if (y + EXPAND_H > 0 && y < h) paintExpandRow(g2, v, y);
                    y += EXPAND_H;
                }
            }
            // Pass B：单元格内容（按左/中/右冻结带 clip 绘制）
            y = -scrollY;
            for (int v = 0; v < model.viewRowCount(); v++) {
                if (y + rH > 0 && y < h) paintRowCells(g2, v, y, leaves);
                y += rH;
                if (model.isExpandedView(v)) y += EXPAND_H;
            }
            if (hasHScroll()) paintHScrollBar(g2);
            g2.dispose();
        }
        /** 行背景：选中 / 状态 / 斑马 / hover（C9 状态行）。 */
        private void paintRowBg(Graphics2D g2, int v, int y) {
            boolean selected = model.isSelectedView(v);
            float hoverAlpha = anim.getProgress("hover");
            boolean isHovered = (v == hoverRow) && hoverAlpha > 0.01f;
            boolean zebra = (v % 2 == 1);
            AstTableModel.Status st = model.getRowStatus(model.rawRowOf(v));
            Color bg = Color.WHITE;
            if (selected) bg = theme().getPrimary();
            else if (st != AstTableModel.Status.DEFAULT) {
                bg = lerp(statusColor(st), Color.WHITE, 0.85f);
                assertContrast(theme().getTextPrimary(), bg, "AstTable status row");
            }
            else if (zebra) {
                bg = theme().getFillBase();
                assertContrast(theme().getTextPrimary(), theme().getFillBase(), "AstTable zebra row");
            }
            else assertContrast(theme().getTextPrimary(), Color.WHITE, "AstTable white row");
            g2.setColor(bg); g2.fillRect(0, y, getWidth(), rH);
            if (selected) return;
            if (isHovered) {
                int a = Math.round(18 * hoverAlpha);
                g2.setColor(new Color(theme().getPrimary().getRed(), theme().getPrimary().getGreen(), theme().getPrimary().getBlue(), a));
                g2.fillRect(0, y, getWidth(), rH);
            }
            if (st == AstTableModel.Status.DEFAULT && !zebra) {
                g2.setColor(lerp(theme().getBorderBase(), Color.WHITE, 0.5f));
                g2.drawLine(0, y + rH - 1, getWidth(), y + rH - 1);
            }
        }
        /** 单元格内容：选择列复选框 + 左/中/右冻结带（含横滚偏移）。 */
        private void paintRowCells(Graphics2D g2, int v, int y, List<AstTableColumn> leaves) {
            int w = getWidth();
            int scw = selectColW(), flw = frozenLeftW(), frw = frozenRightW(), tlw = totalLeafW();
            int leftBand = scw + flw;
            // 合并单元格可跨行，clip 高度需按最大 rowspan 放宽，否则被截断
            int spanH = rowSpanRows(v, leaves.size()) * rH;
            boolean selected = model.isSelectedView(v);
            float hoverAlpha = anim.getProgress("hover");
            boolean isHovered = (v == hoverRow) && hoverAlpha > 0.01f;
            Color textColor = theme().getTextPrimary();
            if (selected) textColor = Color.WHITE;
            else if (isHovered) textColor = lerp(theme().getTextPrimary(), theme().getPrimary(), hoverAlpha * 0.7f);
            if (leftBand > 0) {
                g2.clipRect(0, y, leftBand, spanH);
                drawCells(g2, v, y, w, leaves, scw, PRED_LEFT, textColor);
                g2.setClip(null); g2.drawLine(leftBand, y, leftBand, y + rH);
            }
            if (scw > 0) {
                g2.clipRect(0, y, scw, rH);
                drawCheckbox(g2, scw / 2, y + rH / 2, selected);
                g2.setClip(null); g2.drawLine(scw, y, scw, y + rH);
            }
            if (w - leftBand - frw > 0) {
                g2.clipRect(leftBand, y, w - leftBand - frw, spanH);
                // 只画未冻结列（原为 null 会画全部，视口宽于内容时冻结列会被重复绘制）
                drawCells(g2, v, y, w, leaves, -scrollX, PRED_NONE, textColor);
                g2.setClip(null);
            }
            if (frw > 0) {
                g2.clipRect(w - frw, y, frw, spanH);
                drawCells(g2, v, y, w, leaves, w - tlw - scw, PRED_RIGHT, textColor);
                g2.setClip(null); g2.drawLine(w - frw, y, w - frw, y + rH);
            }
        }
        /** 绘制一行内通过 filter 的叶子列文本；被合并覆盖的格跳过，锚点格按 rowspan/colspan 扩展。 */
        private void drawCells(Graphics2D g2, int v, int y, int w, List<AstTableColumn> leaves, int xOffset,
                               java.util.function.Predicate<AstTableColumn> filter, Color textColor) {
            g2.setColor(textColor); g2.setFont(cf);
            FontMetrics fm = g2.getFontMetrics();
            int raw = model.rawRowOf(v);
            int x = xOffset;
            for (int c = 0; c < leaves.size(); c++) {
                AstTableColumn col = leaves.get(c);
                if ((filter == null || filter.test(col)) && !coveredCells.contains(cellKey(v, c))) {
                    if (paintLog != null) paintLog.add(new int[]{v, c});
                    int[] sp = model.getSpan(raw, c);
                    int cs = Math.max(1, sp[1]), rs = Math.max(1, sp[0]);
                    int mw = col.width;
                    for (int k = 1; k < cs && c + k < leaves.size(); k++) mw += leaves.get(c + k).width;
                    int mh = rs * rH;
                    // 第一列行首：展开/收起三角按钮
                    boolean drawExpandBtn = expandable() && c == 0;
                    if (drawExpandBtn) drawExpandButton(g2, x + 2, y + rH / 2, model.isExpandedView(v), textColor);
                    int indent = drawExpandBtn ? EXPAND_BTN_W : 0;
                    String text = clipText(g2, String.valueOf(model.getValueAtView(v, c)), mw - 2 * CELL_PAD_X - indent);
                    int tx = alignX(x + indent, mw - indent, fm.stringWidth(text), col.align, CELL_PAD_X);
                    int ty = y + (mh - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(text, tx, ty);
                    if (x + mw < w) {
                        Color saved = g2.getColor();
                        g2.setColor(theme().getBorderBase());
                        g2.drawLine(x + mw - 1, y + 4, x + mw - 1, y + mh - 4);
                        g2.setColor(saved);
                    }
                }
                x += col.width;
            }
        }
        /** 重算被合并单元格覆盖的格集合（视图行 × 叶子列）。 */
        private void computeCovered(int leafCount) {
            coveredCells = new java.util.HashSet<Long>();
            int rows = model.viewRowCount();
            for (int v = 0; v < rows; v++) {
                int raw = model.rawRowOf(v);
                for (int c = 0; c < leafCount; c++) {
                    int[] sp = model.getSpan(raw, c);
                    if (sp[0] <= 1 && sp[1] <= 1) continue;
                    int rs = Math.min(sp[0], rows - v), cs = Math.min(sp[1], leafCount - c);
                    for (int dr = 0; dr < rs; dr++)
                        for (int dc = 0; dc < cs; dc++)
                            if (dr > 0 || dc > 0) coveredCells.add(cellKey(v + dr, c + dc));
                }
            }
        }
        /** 该行最大 rowspan（决定合并单元格 clip 高度）。 */
        private int rowSpanRows(int v, int leafCount) {
            int raw = model.rawRowOf(v), max = 1;
            for (int c = 0; c < leafCount; c++) {
                int rs = model.getSpan(raw, c)[0];
                if (rs > max) max = rs;
            }
            return max;
        }
        private long cellKey(int v, int c) { return ((long) v << 16) | c; }
        /** 绘制展开行区块（C7），整行宽、浅色底。 */
        private void paintExpandRow(Graphics2D g2, int v, int y) {
            int w = getWidth();
            g2.setColor(theme().getFillBase());
            g2.fillRect(0, y, w, EXPAND_H);
            g2.drawLine(0, y + EXPAND_H - 1, w, y + EXPAND_H - 1);
            int raw = model.rawRowOf(v);
            if (expandRenderer != null) {
                JComponent c = expandRenderer.apply(raw);
                if (c != null) {
                    int cw = w - 2 * CELL_PAD_X;
                    c.setSize(cw, EXPAND_H - 8);
                    Graphics g0 = g2.create(CELL_PAD_X, y + 4, cw, EXPAND_H - 8);
                    c.paint(g0); g0.dispose();
                    return;
                }
            }
            if (expandTextFn != null) {
                String text = expandTextFn.apply(raw);
                if (text != null) {
                    g2.setColor(theme().getTextPrimary());
                    g2.setFont(cf);
                    FontMetrics fm = g2.getFontMetrics();
                    int ty = y + (EXPAND_H - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(clipText(g2, text, w - 2 * CELL_PAD_X), CELL_PAD_X, ty);
                }
            }
        }
        @Override protected void selfCheck() { }
    }

    // ===================== FooterView（C8 合计行）=====================
    public class FooterView extends AstDisplayComponent {
        private int fH = 32; private Font cf;
        FooterView() { setVisible(false); }
        void applyTier(int h, Font f) { this.fH = h; this.cf = f; }
        int getPreferredHeight() { return model.hasSummary() ? fH : 0; }
        @Override public Dimension getPreferredSize() { return new Dimension(getTotalWidth() + selectColW(), getPreferredHeight()); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(Color.WHITE); g2.fillRect(0, 0, w, h);
            g2.setColor(theme().getBorderBase()); g2.drawLine(0, 0, w, 0);
            g2.setFont(cf);
            FontMetrics fm = g2.getFontMetrics();
            List<AstTableColumn> leaves = model.getLeafColumns();
            int scw = selectColW(), flw = frozenLeftW(), frw = frozenRightW(), tlw = totalLeafW();
            int leftBand = scw + flw;
            if (leftBand > 0) {
                g2.clipRect(0, 0, leftBand, h);
                drawSummaryCells(g2, fm, leaves, scw, c -> c.fixed == AstTableColumn.Fixed.LEFT, h);
                g2.setClip(null); g2.drawLine(leftBand, 0, leftBand, h);
            }
            if (w - leftBand - frw > 0) {
                g2.clipRect(leftBand, 0, w - leftBand - frw, h);
                drawSummaryCells(g2, fm, leaves, -bodyView.scrollX, null, h);
                g2.setClip(null);
            }
            if (frw > 0) {
                g2.clipRect(w - frw, 0, frw, h);
                drawSummaryCells(g2, fm, leaves, w - tlw - scw, c -> c.fixed == AstTableColumn.Fixed.RIGHT, h);
                g2.setClip(null); g2.drawLine(w - frw, 0, w - frw, h);
            }
            g2.dispose();
        }
        private void drawSummaryCells(Graphics2D g2, FontMetrics fm, List<AstTableColumn> leaves, int xOffset,
                                      java.util.function.Predicate<AstTableColumn> filter, int h) {
            g2.setColor(theme().getTextPrimary());
            int x = xOffset;
            for (int c = 0; c < leaves.size(); c++) {
                AstTableColumn col = leaves.get(c);
                if (filter == null || filter.test(col)) {
                    String text = summaryText(c);
                    int tx = alignX(x, col.width, fm.stringWidth(text), col.align, CELL_PAD_X);
                    int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(clipText(g2, text, col.width - 2 * CELL_PAD_X), tx, ty);
                }
                x += col.width;
            }
        }
        /** 未注册合计的首列显示「合计」标签，其余未注册列留空。 */
        String summaryText(int c) {
            if (!model.hasSummary(c)) return c == 0 ? "合计" : "";
            Object v = model.getSummary(c);
            return v == null ? "" : String.valueOf(v);
        }
        @Override protected void selfCheck() { }
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
    /** 是否配置了展开内容（决定行首是否显示展开按钮）。 */
    private boolean expandable() { return expandTextFn != null || expandRenderer != null; }
    /** 绘制展开/收起三角：展开为 ▼，收起为 ▶。 */
    private void drawExpandButton(Graphics2D g2, int x, int cy, boolean expanded, Color color) {
        g2.setColor(color);
        if (expanded) {
            g2.drawLine(x, cy - 3, x + 8, cy - 3);
            g2.drawLine(x, cy - 3, x + 4, cy + 3);
            g2.drawLine(x + 8, cy - 3, x + 4, cy + 3);
        } else {
            g2.drawLine(x, cy - 4, x + 6, cy);
            g2.drawLine(x + 6, cy, x, cy + 4);
            g2.drawLine(x, cy - 4, x, cy + 4);
        }
    }
    /** 行状态 → 主题色（C9）。 */
    private Color statusColor(AstTableModel.Status st) {
        switch (st) {
            case SUCCESS: return theme().getSuccess();
            case WARNING: return theme().getWarning();
            case DANGER:  return theme().getDanger();
            case INFO:    return theme().getInfo();
            default:      return theme().getPrimary();
        }
    }
    /** 合并单元格（C9）：锚点 (rawRow, leafCol) 向下跨 rowspan 行、向右跨 colspan 列。 */
    public void setSpan(int rawRow, int leafCol, int rowspan, int colspan) {
        model.setSpan(rawRow, leafCol, rowspan, colspan); revalidate(); repaint();
    }
    /** 设置行状态（C9），影响整行底色。 */
    public void setRowStatus(int rawRow, AstTableModel.Status st) {
        if (st == null) throw new IllegalArgumentException("status must not be null");
        model.setRowStatus(rawRow, st); repaint();
    }
    /** 绘制一个 16×16 复选框（白底 + PRIMARY 勾选）。供选择列复用。 */
    private void drawCheckbox(Graphics2D g2, int cx, int cy, boolean checked) {
        int s = 16, x = cx - s / 2, y = cy - s / 2;
        g2.setColor(Color.WHITE); g2.fillRect(x, y, s, s);
        g2.setColor(theme().getBorderBase()); g2.drawRect(x, y, s, s);
        if (checked) {
            g2.setColor(theme().getPrimary());
            g2.drawLine(x + 3, cy, x + 6, y + s - 4);
            g2.drawLine(x + 6, y + s - 4, x + s - 3, y + 3);
        }
    }

    // --- Self-check ---
    @Override
    protected void selfCheck() {
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

        // --- C6: 筛选（列文本过滤）---
        AstTable t6 = new AstTable(new AstTableColumn[]{
            new AstTableColumn("城市", 160), new AstTableColumn("人数", 80)});
        t6.addRow("北京市", 100); t6.addRow("上海市", 200); t6.addRow("广州市", 150);
        t6.getModel().filter(r -> "上海市".equals(r[0]));
        assert t6.getRowCount() == 1 : "C6 过滤后1行";
        assert "上海市".equals(t6.getValueAt(0, 0)) : "C6 剩上海";
        t6.getModel().clearFilter();
        assert t6.getRowCount() == 3 : "C6 清空还原3行";
        // 筛选 + 排序组合：按人数降序后仅剩上海市(200)
        t6.getModel().sort(1, AstTableModel.SortDir.DESC);
        t6.getModel().filter(r -> (Integer) r[1] >= 200);
        assert t6.getRowCount() == 1 && "上海市".equals(t6.getValueAt(0, 0)) : "C6 筛选+排序组合";
        t6.getModel().clearFilter();
        assert t6.getRowCount() == 3 : "C6 组合清空还原3行";

        // --- C7: 展开行 ---
        AstTable t7 = new AstTable(new AstTableColumn[]{
            new AstTableColumn("姓名", 120), new AstTableColumn("详情", 200)});
        t7.addRow("张", "..."); t7.addRow("李", "...");
        final int base = t7.getRowCount();
        t7.setRowExpandText(r -> "展开内容#" + r);
        t7.getModel().toggleExpanded(0);
        assert t7.getRowCount() == base + 1 : "C7 展开后视图行+1(" + (base + 1) + ")";
        t7.getModel().toggleExpanded(0);
        assert t7.getRowCount() == base : "C7 收起还原";
        // 展开后离屏绘制不抛异常（含展开区块）
        t7.getModel().toggleExpanded(0);
        java.awt.image.BufferedImage img7 = new java.awt.image.BufferedImage(t7.getPreferredSize().width, 160, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg7 = img7.createGraphics();
        try { t7.paint(gg7); } finally { gg7.dispose(); }

        // --- C8: 表尾合计行 ---
        AstTable t8 = new AstTable(new AstTableColumn[]{
            new AstTableColumn("姓名", 120), new AstTableColumn("年龄", 80)});
        t8.addRow("张", 22); t8.addRow("李", 34); t8.addRow("王", 28);
        assert !t8.getFooterView().isVisible() : "C8 无合计时 footer 隐藏";
        t8.setSummary(1, null); // null → 默认求和
        assert (Integer) t8.getModel().getSummary(1) == 84 : "C8 合计=84";
        assert t8.getFooterView().isVisible() : "C8 有合计时 footer 可见";
        assert t8.getFooterView().getPreferredHeight() > 0 : "C8 footer 高度>0";
        // 合计随筛选变化：仅留一行时合计=34
        t8.getModel().filter(r -> Integer.valueOf(34).equals(r[1]));
        assert (Integer) t8.getModel().getSummary(1) == 34 : "C8 合计随筛选变化=34";
        t8.getModel().clearFilter();
        assert (Integer) t8.getModel().getSummary(1) == 84 : "C8 清空筛选还原=84";
        // 离屏绘制不抛异常（含合计行）
        java.awt.image.BufferedImage img8 = new java.awt.image.BufferedImage(t8.getPreferredSize().width, 160, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg8 = img8.createGraphics();
        try { t8.paint(gg8); } finally { gg8.dispose(); }
        t8.clearSummary();
        assert !t8.getFooterView().isVisible() : "C8 clearSummary 后隐藏";

        // --- C9: 合并行/列 + 带状态表格 ---
        AstTable t9 = new AstTable(new AstTableColumn[]{
            new AstTableColumn("A", 100), new AstTableColumn("B", 100)});
        t9.addRow("x", "1"); t9.addRow("x", "2"); t9.addRow("y", "3");
        t9.getModel().setSpan(0, 0, 2, 1); // A 列第 0、1 行合并
        int[] sp = t9.getModel().getSpan(0, 0);
        assert sp[0] == 2 && sp[1] == 1 : "C9 合并 2x1";
        assert Arrays.equals(t9.getModel().getSpan(0, 1), new int[]{1, 1}) : "C9 非合并默认 1x1";
        t9.getModel().setRowStatus(2, AstTableModel.Status.SUCCESS);
        assert t9.getModel().getRowStatus(2) == AstTableModel.Status.SUCCESS : "C9 状态行";
        assert t9.getModel().getRowStatus(0) == AstTableModel.Status.DEFAULT : "C9 未设置状态为 DEFAULT";
        // 合并单元格 + 状态行离屏绘制不抛断言
        java.awt.image.BufferedImage img9 = new java.awt.image.BufferedImage(220, 120, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg9 = img9.createGraphics();
        try { t9.paint(gg9); } finally { gg9.dispose(); }
        // 非法 span 应抛异常
        threw = false;
        try { t9.getModel().setSpan(0, 0, 0, 1); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "C9 span<1 应抛异常";
        threw = false;
        try { t9.getModel().setRowStatus(0, null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "C9 null status 应抛异常";

        // --- Bugfix 回归：7 个表格问题 ---
        final Throwable[] errB = {null};
        try { SwingUtilities.invokeAndWait(new Runnable(){ public void run(){
            JFrame jf = new JFrame("AstTable bugfix"); jf.setSize(700, 300); jf.setVisible(true);
            try {
                JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new BorderLayout());

                // (1) 排序：点击表头应真正刷新数据行序
                AstTable ts = new AstTable(new AstTableColumn[]{
                    new AstTableColumn("姓名", 120),
                    new AstTableColumn("年龄", 100, Align.CENTER, true, AstTableColumn.Fixed.NONE, false, null)});
                ts.addRow("王", 22); ts.addRow("张", 34); ts.addRow("李", 28);
                cp.add(ts, BorderLayout.CENTER); jf.validate();
                int ageX = ts.getHeaderView().leafX(1) + 40;
                int sac = ts.sortApplyCount;
                ts.getHeaderView().dispatchEvent(new MouseEvent(ts.getHeaderView(), MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), 0, ageX, ts.getHeaderHeight() / 2, 1, false));
                // 关键：必须走 AstTable.setSort（整表重绘）；若只 repaint 表头，数据行不会刷新
                assert ts.sortApplyCount == sac + 1 : "bugfix1 点击表头应触发 AstTable.setSort（整表刷新）";
                assert ts.getModel().getSortDir() == AstTableModel.SortDir.ASC : "bugfix1 点击表头应置 ASC";
                assert (Integer) ts.getValueAt(0, 1) == 22 : "bugfix1 升序首行年龄=22, got " + ts.getValueAt(0, 1);
                ts.getHeaderView().dispatchEvent(new MouseEvent(ts.getHeaderView(), MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), 0, ageX, ts.getHeaderHeight() / 2, 1, false));
                assert (Integer) ts.getValueAt(0, 1) == 34 : "bugfix1 再点应为 DESC 首行=34, got " + ts.getValueAt(0, 1);

                // (2) 冻结列：中列过滤器不得包含冻结列（否则视口宽于内容时重复绘制）
                AstTableColumn opCol = new AstTableColumn("操作", 110, Align.CENTER, false, AstTableColumn.Fixed.RIGHT, null);
                AstTableColumn nmCol = new AstTableColumn("姓名", 100, Align.LEFT, false, AstTableColumn.Fixed.LEFT, null);
                assert !PRED_NONE.test(opCol) && !PRED_NONE.test(nmCol) : "bugfix2 中列过滤器不得匹配冻结列";
                assert PRED_LEFT.test(nmCol) && PRED_RIGHT.test(opCol) : "bugfix2 冻结列应分别匹配左/右过滤器";
                assert !PRED_LEFT.test(opCol) && !PRED_RIGHT.test(nmCol) : "bugfix2 冻结过滤器应互斥";
                // 绘制级验证：视口宽于内容时，冻结列每行只能被画 1 次（不能在中列带里再画一次）
                cp.removeAll();
                AstTable tf = new AstTable(new AstTableColumn[]{
                    nmCol, new AstTableColumn("年龄", 80), opCol});
                tf.addRow("张三", 28, "编辑"); tf.addRow("李四", 34, "编辑");
                cp.add(tf, BorderLayout.CENTER); jf.setSize(900, 200); jf.validate();
                assert tf.getBodyView().getWidth() > tf.totalLeafW() + tf.selectColW()
                    : "bugfix2 前提：视口应宽于内容, got " + tf.getBodyView().getWidth() + "/" + (tf.totalLeafW() + tf.selectColW());
                java.awt.image.BufferedImage fimg = new java.awt.image.BufferedImage(
                    Math.max(1, tf.getBodyView().getWidth()), Math.max(1, tf.getBodyView().getHeight()),
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D fg = fimg.createGraphics();
                tf.getBodyView().paintLog = new java.util.ArrayList<int[]>();
                try { tf.getBodyView().paint(fg); } finally { fg.dispose(); }
                int opHits = 0, nmHits = 0;
                for (int[] e : tf.getBodyView().paintLog) {
                    if (e[0] != 0) continue;
                    if (e[1] == 2) opHits++;
                    if (e[1] == 0) nmHits++;
                }
                tf.getBodyView().paintLog = null;
                assert opHits == 1 : "bugfix2 右冻结列「操作」每行应只绘制 1 次, got " + opHits;
                assert nmHits == 1 : "bugfix2 左冻结列「姓名」每行应只绘制 1 次, got " + nmHits;

                // (3) 横向滚动条：内容宽于视口时出现，且行区高度排除滚动条
                cp.removeAll();
                AstTable tb = new AstTable(new AstTableColumn[]{
                    new AstTableColumn("A", 200), new AstTableColumn("B", 200), new AstTableColumn("C", 200)});
                for (int i = 0; i < 3; i++) tb.addRow("a" + i, i, "c" + i);
                cp.add(tb, BorderLayout.CENTER); jf.setSize(300, 200); jf.validate();
                assert tb.getBodyView().hasHScroll() : "bugfix3 内容宽于视口时应有横向滚动条";
                assert tb.getBodyView().viewportH() == tb.getBodyView().getHeight() - HSCROLL_H
                    : "bugfix3 行区高度应排除滚动条, got " + tb.getBodyView().viewportH() + "/" + tb.getBodyView().getHeight();
                int sxBefore = tb.getBodyView().scrollX;
                int barY = tb.getBodyView().getHeight() - 4;
                tb.getBodyView().dispatchEvent(new MouseEvent(tb.getBodyView(), MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), 0, 260, barY, 1, false));
                assert tb.getBodyView().scrollX > sxBefore : "bugfix3 点击滚动条右侧应增大 scrollX";

                // (4) 多级表头：未分多级的叶子列跨多行，其底部应有边框
                cp.removeAll();
                AstTableColumn mc = new AstTableColumn("城市", 120), mst = new AstTableColumn("街道", 160);
                AstTable tm = new AstTable(new AstTableColumn[]{
                    new AstTableColumn("姓名", 100), new AstTableColumn("地址", Arrays.asList(mc, mst)),
                    new AstTableColumn("年龄", 80)});
                tm.addRow("张三", "上海", "浦东", 28);
                cp.add(tm, BorderLayout.CENTER); jf.setSize(700, 240); jf.validate();
                int mh = tm.getHeaderView().getPreferredHeight();
                java.awt.image.BufferedImage mimg = new java.awt.image.BufferedImage(
                    Math.max(1, tm.getHeaderView().getWidth()), mh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D mg = mimg.createGraphics();
                try { tm.getHeaderView().paint(mg); } finally { mg.dispose(); }
                // 「姓名」是未分多级的叶子列，应纵向跨 2 行 → level0/level1 的分界处不应有横线。
                // 采样点取列左缘内侧（避开居中文字的抗锯齿像素）。
                int pr = theme().getPrimary().getRed(), pg = theme().getPrimary().getGreen(), pb = theme().getPrimary().getBlue();
                int midY = tm.getHeaderHeight() - 1;
                int bpx = mimg.getRGB(tm.getHeaderView().leafX(0) + 8, midY);
                boolean isBg = ((bpx >> 16) & 0xFF) == pr && ((bpx >> 8) & 0xFF) == pg && (bpx & 0xFF) == pb;
                assert isBg : "bugfix4 多级表头：未分多级的叶子列应跨多行，层级分界处不应有分隔线";

                // (5) 多选全选后，表头文字仍应可见（白色像素）
                cp.removeAll();
                AstTable tk = new AstTable(new AstTableColumn[]{
                    new AstTableColumn("姓名", 140), new AstTableColumn("部门", 160)});
                for (int i = 0; i < 3; i++) tk.addRow("u" + i, "d" + i);
                tk.setSelectionMode(AstTable.SELECTION_MULTIPLE);
                cp.add(tk, BorderLayout.CENTER); jf.setSize(500, 240); jf.validate();
                tk.getModel().toggleSelectAll();
                assert tk.getModel().getSelectedViewRows().size() == 3 : "bugfix5 全选 3 行";
                int kh = tk.getHeaderView().getPreferredHeight();
                java.awt.image.BufferedImage kimg = new java.awt.image.BufferedImage(
                    Math.max(1, tk.getHeaderView().getWidth()), kh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D kg = kimg.createGraphics();
                try { tk.getHeaderView().paint(kg); } finally { kg.dispose(); }
                boolean hasWhite = false;
                for (int yy = 0; yy < kimg.getHeight() && !hasWhite; yy++)
                    for (int xx = SELECT_COL_W + 2; xx < kimg.getWidth(); xx++) {
                        int rgb = kimg.getRGB(xx, yy);
                        if (((rgb >> 16) & 0xFF) > 230 && ((rgb >> 8) & 0xFF) > 230 && (rgb & 0xFF) > 230) { hasWhite = true; break; }
                    }
                assert hasWhite : "bugfix5 全选后表头文字应仍为白色可见（原被 drawCheckbox 的颜色覆盖）";

                // (6) 展开按钮：点击第一列行首切换展开，且不触发选中
                cp.removeAll();
                AstTable te = new AstTable(new AstTableColumn[]{
                    new AstTableColumn("姓名", 120), new AstTableColumn("详情", 160)});
                te.addRow("张", "x"); te.addRow("李", "y");
                te.setRowExpandText(r -> "详情#" + r);
                cp.add(te, BorderLayout.CENTER); jf.setSize(500, 240); jf.validate();
                int reh = te.getRowHeight(), baseRows = te.getRowCount();
                int btnX = te.getBodyView().leafXOnScreen(0) + 6;
                te.getBodyView().dispatchEvent(new MouseEvent(te.getBodyView(), MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), 0, btnX, reh / 2, 1, false));
                assert te.getRowCount() == baseRows + 1 : "bugfix6 点击行首展开按钮应 +1 行, got " + te.getRowCount();
                assert te.getSelectedRow() == -1 : "bugfix6 点击展开按钮不应选中该行";
                // 展开后整表离屏绘制不抛异常（含滚动条/展开区）
                java.awt.image.BufferedImage eimg = new java.awt.image.BufferedImage(
                    te.getPreferredSize().width, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D eg = eimg.createGraphics();
                try { te.paint(eg); } finally { eg.dispose(); }
            } finally { jf.dispose(); }
        }}); } catch (Throwable t){ errB[0] = t; }
        if (errB[0] != null) throw new RuntimeException(errB[0]);

        System.out.println("AstTable self-check OK");
    }

    public static void main(String[] args) {
        AstTableColumn[] cols = { new AstTableColumn("test", 100) };
        new AstTable(cols).selfCheck();
    }
}
