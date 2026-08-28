package org.swelement.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * 表格列模型（P4-C）。
 * 叶子列承载实际数据；父列通过 {@code children} 描述多级表头（跨子列宽度合并标题）。
 * 支持冻结列（{@code fixed}）与排序（{@code sortable}）。
 *
 * 向后兼容：{@link AstTable.Column} 为本类的薄子类，保留旧 2 参/3 参构造器。
 */
public class AstTableColumn {
    public enum Fixed { NONE, LEFT, RIGHT }

    public final String title;
    public final int width;
    public final AstTable.Align align;
    public final boolean sortable;
    public final Fixed fixed;
    /** 多级表头子列；叶子列为 null。 */
    public final List<AstTableColumn> children;

    public AstTableColumn(String title, int width) {
        this(title, width, AstTable.Align.LEFT, false, Fixed.NONE, null);
    }
    public AstTableColumn(String title, int width, AstTable.Align align) {
        this(title, width, align, false, Fixed.NONE, null);
    }
    public AstTableColumn(String title, int width, AstTable.Align align, boolean sortable, Fixed fixed, List<AstTableColumn> children) {
        if (title == null) throw new IllegalArgumentException("title must not be null");
        if (width < 24) throw new IllegalArgumentException("width must be >= 24");
        if (align == null) throw new IllegalArgumentException("align must not be null");
        if (fixed == null) throw new IllegalArgumentException("fixed must not be null");
        this.title = title;
        this.width = width;
        this.align = align;
        this.sortable = sortable;
        this.fixed = fixed;
        this.children = children;
    }
    /** 多级表头构造器：父列横跨其子列宽度之和。 */
    public AstTableColumn(String title, List<AstTableColumn> children) {
        this(title, sumWidth(children), AstTable.Align.LEFT, false, Fixed.NONE, children);
    }

    private static int sumWidth(List<AstTableColumn> children) {
        int w = 0;
        for (AstTableColumn c : children) w += c.width;
        return w;
    }

    public boolean isLeaf() { return children == null || children.isEmpty(); }

    /** 深度优先拍平为叶子列列表（保持视觉从左到右顺序）。 */
    public List<AstTableColumn> getLeafColumns() {
        List<AstTableColumn> out = new ArrayList<AstTableColumn>();
        if (isLeaf()) out.add(this);
        else for (AstTableColumn ch : children) out.addAll(ch.getLeafColumns());
        return out;
    }

    /** 该子树最大层级数（叶子=1）。 */
    public int getDepth() {
        if (isLeaf()) return 1;
        int d = 0;
        for (AstTableColumn ch : children) d = Math.max(d, ch.getDepth());
        return d + 1;
    }

    public int getLeafCount() { return getLeafColumns().size(); }
}
