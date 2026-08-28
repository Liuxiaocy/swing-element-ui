package org.swelement.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 表格数据视图（P4-C）。
 * 持有原始行 {@code raw} 与「视图行」索引 {@code view}（排序/筛选后的 raw 行映射）。
 * C1 仅含原始行/视图/单选；排序、筛选、展开、合计聚合、合并 span、状态行在后续批次扩展。
 */
public class AstTableModel {
    /** 选择模式。 */
    public enum SelectionMode { SINGLE, MULTIPLE }

    private final List<AstTableColumn> columns;
    private final List<Object[]> raw = new ArrayList<Object[]>();
    private List<Integer> view = new ArrayList<Integer>();

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
        view.add(raw.size() - 1);
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
            view.add(raw.size() - 1);
        }
        selectedViewRow = -1;
    }

    public void clearRows() {
        raw.clear();
        view.clear();
        selectedViewRow = -1;
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

    // --- 单选（C1；多选在 C4 覆盖）---
    public void setSelectedViewRow(int v) {
        if (v < -1 || v >= view.size()) throw new IndexOutOfBoundsException("view row " + v);
        this.selectedViewRow = v;
    }
    public int getSelectedViewRow() { return selectedViewRow; }
    public boolean isSelectedView(int v) { return v == selectedViewRow; }
    public java.util.Set<Integer> getSelectedViewRows() {
        java.util.Set<Integer> s = new java.util.HashSet<Integer>();
        if (selectedViewRow >= 0) s.add(selectedViewRow);
        return s;
    }
}
