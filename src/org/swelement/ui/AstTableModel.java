package org.swelement.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 表格数据视图（P4-C）。
 * 持有原始行 {@code raw} 与「视图行」索引 {@code view}（排序/筛选后的 raw 行映射）。
 * C1 仅含原始行/视图/单选；排序、筛选、展开、合计聚合、合并 span、状态行在后续批次扩展。
 */
public class AstTableModel {
    /** 选择模式。 */
    public enum SelectionMode { SINGLE, MULTIPLE }

    /** 排序方向（C5）。 */
    public enum SortDir { ASC, DESC, NONE }

    private final List<AstTableColumn> columns;
    private final List<Object[]> raw = new ArrayList<Object[]>();
    private List<Integer> view = new ArrayList<Integer>();

    // 排序（C5）
    private int sortLeaf = -1;
    private SortDir sortDir = SortDir.NONE;

    // 筛选（C6）
    private Predicate<Object[]> filter = null;

    // 展开行（C7）：以 raw 行记录展开状态
    private final Set<Integer> expanded = new HashSet<Integer>();

    // 合计行（C8）
    private final Map<Integer, Aggregator> summaries = new HashMap<Integer, Aggregator>();

    // 单选（C1）；多选集合在 C4 扩展
    private int selectedViewRow = -1;

    public AstTableModel(List<AstTableColumn> columns) {
        if (columns == null) throw new IllegalArgumentException("columns must not be null");
        for (AstTableColumn c : columns) if (c == null) throw new IllegalArgumentException("column must not be null");
        this.columns = new ArrayList<AstTableColumn>(columns);
    }

    // --- 列 ---
    public List<AstTableColumn> getColumns() { return columns; }
    public List<AstTableColumn> getLeafColumns() {
        List<AstTableColumn> out = new ArrayList<AstTableColumn>();
        for (AstTableColumn c : columns) out.addAll(c.getLeafColumns());
        return out;
    }
    public int leafCount() { return getLeafColumns().size(); }

    // --- 数据写入 ---
    public void addRow(Object... values) {
        if (values == null) throw new IllegalArgumentException("values must not be null");
        if (values.length != leafCount())
            throw new IllegalArgumentException("values length (" + values.length + ") must match leaf columns (" + leafCount() + ")");
        for (Object v : values) if (v == null) throw new IllegalArgumentException("value must not be null");
        Object[] copy = Arrays.copyOf(values, values.length);
        raw.add(copy);
        rebuildView();
    }

    public void setRows(List<Object[]> data) {
        if (data == null) throw new IllegalArgumentException("data must not be null");
        raw.clear();
        view.clear();
        for (Object[] row : data) {
            if (row == null) throw new IllegalArgumentException("row must not be null");
            if (row.length != leafCount())
                throw new IllegalArgumentException("row length must match leaf columns");
            raw.add(Arrays.copyOf(row, row.length));
        }
        rebuildView();
        selectedViewRow = -1;
    }

    public void clearRows() {
        raw.clear();
        view.clear();
        selectedViewRow = -1;
        selectedViewRows.clear();
    }

    // --- 视图查询 ---
    public int rawRowCount() { return raw.size(); }
    public int viewRowCount() { return view.size(); }
    public int rawRowOf(int v) {
        if (v < 0 || v >= view.size()) throw new IndexOutOfBoundsException("view row " + v);
        return view.get(v);
    }
    public Object getValueAtView(int v, int leafCol) {
        return raw.get(view.get(v))[leafCol];
    }

    // --- 排序 + 筛选（C5/C6）：view = 筛选(raw) 后排序 ---
    public int getSortLeaf() { return sortLeaf; }
    public SortDir getSortDir() { return sortDir; }
    public boolean getFilterActive() { return filter != null; }

    /** 按叶子列排序，重建 view（在筛选结果上排序）；NONE 还原为筛选序。切换会清空选择。 */
    public void sort(int leafCol, SortDir dir) {
        if (leafCol < 0 || leafCol >= leafCount()) throw new IndexOutOfBoundsException("leafCol " + leafCol);
        this.sortLeaf = (dir == SortDir.NONE) ? -1 : leafCol;
        this.sortDir = dir;
        rebuildView();
    }

    /** 应用行级筛选谓词，重建 view（在筛选结果上排序）。传入 null 视为清空。 */
    public void filter(Predicate<Object[]> p) {
        if (p == null) { clearFilter(); return; }
        this.filter = p;
        rebuildView();
    }
    /** 清空筛选，保留排序。 */
    public void clearFilter() {
        this.filter = null;
        rebuildView();
    }

    // --- 展开行（C7）---
    public void toggleExpanded(int rawRow) {
        if (rawRow < 0 || rawRow >= raw.size()) return;
        if (expanded.contains(rawRow)) expanded.remove(rawRow); else expanded.add(rawRow);
    }
    public boolean isExpanded(int rawRow) { return expanded.contains(rawRow); }
    /** 视图行 v 是否展开（v 为筛选/排序后的视图索引）。 */
    public boolean isExpandedView(int v) {
        return v >= 0 && v < view.size() && expanded.contains(view.get(v));
    }
    /** 当前展开的视图行数量（用于布局高度）。 */
    public int expandedCount() {
        int c = 0;
        for (Integer v : view) if (expanded.contains(v)) c++;
        return c;
    }

    // --- 合计行（C8）---
    /** 列聚合器：对（当前视图行, 叶子列）求值。 */
    public interface Aggregator { Object apply(List<Object[]> rows, int leafCol); }

    /** 数值求和；无数值时返回空串。 */
    public static final Aggregator SUM = new Aggregator() {
        public Object apply(List<Object[]> rows, int leafCol) {
            double s = 0; boolean any = false;
            for (Object[] r : rows) {
                Object v = r[leafCol];
                if (v instanceof Number) { s += ((Number) v).doubleValue(); any = true; }
            }
            if (!any) return "";
            if (s == Math.rint(s) && Math.abs(s) < 1e15) return Integer.valueOf((int) s);
            return Double.valueOf(s);
        }
    };
    /** 数值平均；无数值时返回空串。 */
    public static final Aggregator AVG = new Aggregator() {
        public Object apply(List<Object[]> rows, int leafCol) {
            double s = 0; int n = 0;
            for (Object[] r : rows) {
                Object v = r[leafCol];
                if (v instanceof Number) { s += ((Number) v).doubleValue(); n++; }
            }
            if (n == 0) return "";
            return Double.valueOf(s / n);
        }
    };
    /** 行数统计。 */
    public static final Aggregator COUNT = new Aggregator() {
        public Object apply(List<Object[]> rows, int leafCol) { return Integer.valueOf(rows.size()); }
    };

    /** 注册列聚合器；agg 为 null 时默认 SUM。 */
    public void setSummary(int leafCol, Aggregator agg) {
        if (leafCol < 0 || leafCol >= leafCount()) throw new IndexOutOfBoundsException("leafCol " + leafCol);
        summaries.put(leafCol, agg == null ? SUM : agg);
    }
    public void clearSummary() { summaries.clear(); }
    public boolean hasSummary() { return !summaries.isEmpty(); }
    public boolean hasSummary(int leafCol) { return summaries.containsKey(leafCol); }
    /** 对当前视图行（含筛选结果）求值；未注册列返回空串。 */
    public Object getSummary(int leafCol) {
        Aggregator a = summaries.get(leafCol);
        if (a == null) return "";
        List<Object[]> rows = new ArrayList<Object[]>();
        for (Integer ri : view) rows.add(raw.get(ri));
        return a.apply(rows, leafCol);
    }

    // --- 合并单元格 + 行状态（C9）---
    public enum Status { DEFAULT, SUCCESS, WARNING, DANGER, INFO }

    // 合并 span：key(rawRow, leafCol) → {rowspan, colspan}
    private final Map<Integer, int[]> spans = new HashMap<Integer, int[]>();
    // 行状态：key rawRow → Status
    private final Map<Integer, Status> rowStatus = new HashMap<Integer, Status>();

    public void setSpan(int rawRow, int leafCol, int rowspan, int colspan) {
        if (rawRow < 0) throw new IndexOutOfBoundsException("rawRow " + rawRow);
        if (leafCol < 0 || leafCol >= leafCount()) throw new IndexOutOfBoundsException("leafCol " + leafCol);
        if (rowspan < 1 || colspan < 1) throw new IllegalArgumentException("span must be >= 1");
        spans.put(spanKey(rawRow, leafCol), new int[]{rowspan, colspan});
    }
    /** 返回 {rowspan, colspan}；未合并为 {1,1}（返回副本）。 */
    public int[] getSpan(int rawRow, int leafCol) {
        int[] sp = spans.get(spanKey(rawRow, leafCol));
        return sp == null ? new int[]{1, 1} : new int[]{sp[0], sp[1]};
    }
    public void clearSpans() { spans.clear(); }

    public void setRowStatus(int rawRow, Status s) {
        if (s == null) throw new IllegalArgumentException("status must not be null");
        if (rawRow < 0) throw new IndexOutOfBoundsException("rawRow " + rawRow);
        rowStatus.put(rawRow, s);
    }
    public Status getRowStatus(int rawRow) {
        Status s = rowStatus.get(rawRow);
        return s == null ? Status.DEFAULT : s;
    }
    private static int spanKey(int rawRow, int leafCol) { return (rawRow << 16) | leafCol; }

    /** 依据当前 filter + sort 重建视图索引；清空选择。 */
    private void rebuildView() {
        List<Integer> base = new ArrayList<Integer>();
        for (int i = 0; i < raw.size(); i++) {
            if (filter == null || filter.test(raw.get(i))) base.add(i);
        }
        if (sortDir != SortDir.NONE && sortLeaf >= 0) {
            final int col = sortLeaf;
            Collections.sort(base, new Comparator<Integer>() {
                public int compare(Integer a, Integer b) {
                    int c = compareValues(raw.get(a)[col], raw.get(b)[col]);
                    return sortDir == SortDir.ASC ? c : -c;
                }
            });
        }
        view = base;
        selectedViewRow = -1;
        selectedViewRows.clear();
    }
    private static int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Number && b instanceof Number)
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    // --- 选择（C1 单选 + C4 多选）---
    private SelectionMode selectionMode = SelectionMode.SINGLE;
    private final java.util.Set<Integer> selectedViewRows = new java.util.HashSet<Integer>();

    public void setSelectionMode(SelectionMode m) {
        this.selectionMode = m;
        selectedViewRow = -1;
        selectedViewRows.clear();
    }
    public SelectionMode getSelectionMode() { return selectionMode; }

    public void setSelectedViewRow(int v) {
        if (v < -1 || v >= view.size()) throw new IndexOutOfBoundsException("view row " + v);
        this.selectedViewRow = v;
        selectedViewRows.clear();
        if (v >= 0) selectedViewRows.add(v);
    }
    public int getSelectedViewRow() {
        if (selectionMode == SelectionMode.MULTIPLE) return selectedViewRows.isEmpty() ? -1 : selectedViewRows.iterator().next();
        return selectedViewRow;
    }
    public boolean isSelectedView(int v) {
        return selectionMode == SelectionMode.MULTIPLE ? selectedViewRows.contains(v) : v == selectedViewRow;
    }
    public void toggleSelectedViewRow(int v) {
        if (v < 0 || v >= view.size()) return;
        if (selectionMode == SelectionMode.SINGLE) selectedViewRow = (v == selectedViewRow) ? -1 : v;
        else if (selectedViewRows.contains(v)) selectedViewRows.remove(v);
        else selectedViewRows.add(v);
    }
    public void toggleSelectAll() {
        if (selectionMode != SelectionMode.MULTIPLE) return;
        if (selectedViewRows.size() == view.size()) selectedViewRows.clear();
        else for (int i = 0; i < view.size(); i++) selectedViewRows.add(i);
    }
    public java.util.Set<Integer> getSelectedViewRows() {
        java.util.Set<Integer> s = new java.util.HashSet<Integer>(selectedViewRows);
        if (selectionMode != SelectionMode.MULTIPLE && selectedViewRow >= 0) s.add(selectedViewRow);
        return s;
    }
}
