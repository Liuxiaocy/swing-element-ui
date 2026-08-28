# P4-C AstTable 类型增强设计（固定表头/冻结列/多级表头/选择/排序/筛选/展开/合计/合并/状态）

- 日期：2026-08-29
- 状态：待实现（spec 已评审，待 writing-plans 出实现计划）
- 关联：P4-A（5 个 Bug 修复，已合入 fdeacfe）；P4-D（AstIcon 重构）；P4-E（卡片模板）；P4-F（尺寸/标识/AstLoading/穿梭框）

## 1. 目标与范围

把 `AstTable` 从「单画布自绘、单层表头、裸数据」升级为具备 Element UI Table 全部常见展示/交互类型的组件，覆盖：

- C1 固定表头 + 纵向滚动
- C2 横向滚动 + 左/右冻结列
- C3 多级表头（跨列合并标题）
- C4 单选 / 多选
- C5 排序（升/降/无，表头三角指示）
- C6 筛选（列文本过滤）
- C7 展开行（行内子内容）
- C8 表尾合计行（按列聚合）
- C9 合并行/列 + 带状态表格（success/warning/danger/info 行样式）

**设计哲学**：保持现有项目「自绘（不混入 JTable）」的视觉一致性，所有新能力以**列模型 + 数据视图 + 分层渲染**方式叠加，原始公开 API 全部向后兼容。

### 非目标（YAGNI）

- 列宽拖拽 resize（仅在表头预留分隔线视觉，不在本轮实现交互）。
- 树形表格（与展开行不同，本轮不做层级缩进树）。
- 虚拟滚动（数据量未到需要虚拟化的程度，普通视口足够）。
- 单元格编辑（Element UI `editable` 不在本轮）。

## 2. 现状基线

`src/org/swelement/ui/AstTable.java`（约 497 行）当前实现：

- `public static final class Column { title, width, align }` 单层、无 children/fixed/sortable。
- 数据 `private final List<Object[]> rows` 直接持有。
- `paintComponent` 全量自绘：白底 + PRIMARY 表头 + 斑马纹 + hover 半透明 + 选中 PRIMARY 填充。
- 尺寸档位 `SIZE_LARGE/DEFAULT/SMALL`（表头 44/36/32、行高 40/32/28）——**保留**。
- 交互：hover、点击行选中（单选中），`rowClickListener`。
- 无滚动容器、无冻结列、无排序/筛选/多选/合计/合并。

`selfCheck()` 已覆盖 null 守卫、行/列计数、选择、尺寸档位、离屏绘制 + `dispatchEvent` 点击。

## 3. 架构总览

`AstTable` 由「单 JComponent」重构为「容器 + 三个视图 + 一个模型」：

```
AstTable (JPanel, 管理布局/滚动状态/事件)
 ├─ HeaderView   : 吸顶固定，多行多级表头，排序三角/筛选图标，选择列头
 ├─ BodyView     : 自管视口 (scrollX, scrollY)，绘制数据行 + 展开区；冻结列 clip 覆盖
 └─ FooterView   : 合计行，固定底部
AstTableModel    : 原始行 + 视图索引(排序/筛选) + 选择 + 展开集 + spanMap + 合计聚合
AstTableColumn   : 列模型（title/width/align/sortable/fixed/children）
```

渲染与交互都经过 `AstTableModel` 的「视图行」索引，保证排序/筛选/展开后的视觉与数据一致。

## 4. 列模型 `AstTableColumn`

```java
public class AstTableColumn {
    public enum Fixed { NONE, LEFT, RIGHT }
    public final String title;
    public final int width;
    public final Align align;          // 复用现有 Align
    public final boolean sortable;
    public final Fixed fixed;
    public final List<AstTableColumn> children; // 多级表头；叶子为空

    // 构造器：叶子列
    AstTableColumn(title, width)
    AstTableColumn(title, width, align)
    AstTableColumn(title, width, align, sortable)
    AstTableColumn(title, width, align, sortable, fixed)
    // 多级表头
    AstTableColumn(title, List<AstTableColumn> children)

    // 工具方法
    boolean isLeaf();
    List<AstTableColumn> getLeafColumns();   // 深度优先拍平
    int getDepth();                          // 该子树最大层级数
    int getLeafCount();
}
```

**向后兼容**：保留现有 `public static final class Column` 作为 `AstTableColumn` 的薄子类（继承 2 参/3 参构造器），保证文档示例与任何外部 `new AstTable.Column(...)` 调用不报错。`AstTable` 构造器同时接受 `AstTableColumn[]` 与 `Column[]`（后者经适配）。

## 5. 数据视图 `AstTableModel`

```java
public class AstTableModel {
    private final List<AstTableColumn> columns;
    private final List<Object[]> raw;          // 原始数据（addRow/setRows 写入）
    private List<Integer> view;               // 视图行 -> raw 行 的索引
    private int selectedViewRow = -1;          // 单选
    private Set<Integer> selectedViewRows;     // 多选
    private final Set<Integer> expanded = ...;// 已展开的原始行
    private final Map<String,Integer> span = ...; // "r,c" -> rowspan*100+colspan
    private final Map<Integer, Aggregator> summary; // col -> 聚合器

    // 排序/筛选（返回新视图，不复制数据）
    void sort(int leafCol, SortDir dir);       // SortDir: ASC/DESC/NONE
    void filter(Predicate<Object[]> p);        // 重建 view
    void clearFilter();

    // 选择
    void setSelectionMode(SINGLE|MULTIPLE);
    void setSelectedViewRow(int v);
    void toggleSelectedViewRow(int v);         // 多选
    void toggleSelectAll();

    // 展开
    void toggleExpanded(int rawRow);

    // 合计
    void setSummary(int leafCol, Aggregator agg); // 默认数值列 sum
    Object getSummary(int leafCol);

    // 合并
    void setSpan(int rawRow, int leafCol, int rowspan, int colspan);

    // 查询
    int viewRowCount();
    Object getValueAtView(int v, int leafCol); // 经 view 映射
    int rawRowOf(int v);
}
```

`AstTable` 持有 `AstTableModel`，所有绘制/点击都走 `model.getValueAtView(...)` 与 `model.viewRowCount()`。原始公开方法 `addRow/setRows/clearRows/getValueAt/getRowCount/getColumnCount/getSelectedRow/setSelectedRow/setRowClickListener` 保留，内部转发到 model（单选默认走 `selectedViewRow`）。

## 6. 渲染分层与滚动

### 6.1 布局
`AstTable` 用 BorderLayout：`HeaderView`(NORTH，固定高度=层级数×表头行高)、`BodyView`(CENTER，占剩余)、`FooterView`(SOUTH，固定高度=合计行高，仅当配置了 summary 时显示)。

### 6.2 滚动（自管视口）
`BodyView` 内部维护 `scrollX, scrollY`：
- 鼠标滚轮 → `scrollY`（纵向）；`Shift+滚轮` 或 表体拖拽 → `scrollX`（横向）。
- 纵向可滚动区间 `[0, max(0, contentH - viewportH)]`；横向 `[0, max(0, totalLeafW - viewportW)]`。
- 越界 clamp。

### 6.3 冻结列（C2 关键）
绘制顺序（BodyView 与 HeaderView 同构）：
1. **主体**：clip 到中部可视区 `[frozenLeftW, W-frozenRightW]`，在 `(-scrollX, -scrollY)` 偏移下绘制全部叶子列。
2. **左冻结**：clip 到 `[0, frozenLeftW]`，以 `scrollY` 纵向偏移、`scrollX=0` 重绘 `fixed==LEFT` 的叶子列（天然吸左）。
3. **右冻结**：clip 到 `[W-frozenRightW, W]`，重绘 `fixed==RIGHT` 叶子列（X 以 `totalLeafW - frozenRightW - scrollX` 对齐，实现吸右）。
4. 在冻结边界画 1px 阴影/分隔线区隔。

`frozenLeftW` = 左冻结叶子列宽度和；`frozenRightW` 同理。`frozenLeftW + frozenRightW < viewportW` 时才有意义，否则退化为普通滚动。

### 6.4 多级表头（C3）
- 顶层按 `children` 递归：父列横跨其子孙叶子列宽度之和，标题居中、底部画分隔线；层级 = `max(getDepth())`。
- 叶子列位于最底行，承载排序三角/筛选图标/选择列复选框。
- 列 X 偏移由 `getLeafColumns()` 顺序累加宽度得出，父列起止 X 由其首尾叶子列推导。

### 6.5 合计行（C8）
`FooterView` 固定底部，逐叶子列绘制 `model.getSummary(leafCol)`（数值列默认 `sum`，文本列默认空或「合计」标签，可由 `setSummary` 自定义）。冻结列处理与 BodyView 一致。

### 6.6 合并单元格（C9）
`paintCells` 改为查 `span` map：若 `(r,c)` 是某合并的「被覆盖格」则跳过；若是锚点格，则把绘制矩形扩展到 `rowspan×colspan` 所占区域（需跳过跨过的行/列高度——合并行假设同行高，跨列按列宽）。遮挡判定：`(r+dr, c+dc)` 均标记 covered，绘制锚点时一次性覆盖。

### 6.7 带状态表格（C9）
`AstTableModel` 增加 `rowStatus: Map<Integer rawRow, Status>`，`Status ∈ {DEFAULT, SUCCESS, WARNING, DANGER, INFO}`。绘制整行背景用对应浅色（如 SUCCESS=FILL_SUCCESS），文字保持 `TEXT_MAIN` 以保证 WCAG AA；选中/状态叠加时优先状态底色、选中用左侧 3px PRIMARY 描边，避免对比度冲突。

## 7. 交互

- **hover**：现存 `hoverRow` 改为「视图行」，半透明覆盖。
- **选中**：点击数据行 → 单选置 `selectedViewRow` / 多选 `toggleSelectedViewRow`；`rowClickListener` 回调改为传「视图行」并在文档注明映射。
- **排序**：点击 `sortable` 叶子列表头 → 在 NONE→ASC→DESC→NONE 间循环，`model.sort`。
- **筛选**：表头筛选图标点击 → 弹文本输入（复用 `AstInput`/`AstPopover` 或简化内联输入框），输入即 `model.filter` 含子串；本轮回文本过滤，复杂条件弹层留接口。
- **展开**：行首展开箭头（当有 `expandRenderer` 时显示）→ `toggleExpanded`，BodyView 在该行下插入展开区（高度由 `expandRenderer` 决定）。

## 8. C1→C9 子批实现要点

每批独立 TDD（RED 断言 → 实现 → GREEN → 提交），不跨批合入。

| 批 | 目标 | 关键新增 API | self-check 重点断言 |
|---|---|---|---|
| C1 | 表头吸顶 + 纵向滚动 | `BodyView` 视口、`scrollY`、滚轮监听 | 首屏表头可见、滚动后表头 Y 不变、最后一行可达 |
| C2 | 横向滚动 + 冻结列 | `AstTableColumn.fixed`、`frozenLeftW/RightW`、clip 重绘 | 冻结列 X 随横滚不变、中列随动、边界分隔线 |
| C3 | 多级表头 | `children`、`getLeafColumns/getDepth` | 层级数、叶子拍平偏移、父列跨宽居中 |
| C4 | 单选/多选 | `setSelectionMode`、选择列复选框 `toggleSelectAll` | 单选互斥、多选集合增删、全选 |
| C5 | 排序 | `sortable`、`sort(dir)`、三角 | 视图行顺序随排序变化、NONE 还原 |
| C6 | 筛选 | `filter(predicate)`、表头图标 | 过滤后视图行数、清空还原 |
| C7 | 展开行 | `setRowExpandRenderer`、`toggleExpanded` | 展开态插入区高度、收起还原 |
| C8 | 合计行 | `setSummary(col, agg)`、`FooterView` | 合计值正确、冻结列对齐、无 summary 不显示 |
| C9 | 合并/状态 | `setSpan`、`rowStatus`、覆盖格跳过 | 锚点跨区绘制、被覆盖格不绘、状态底色 |

**向后兼容红线**：C1 起 `AstTable(Column[] cols)`、`addRow`、`setRows`、`getValueAt`、`getSelectedRow/setSelectedRow`、`setRowClickListener`、`setSize`、尺寸档位、`selfCheck` 既有断言全部保持通过。

## 9. 测试策略

- 沿用现有 `AstTable.selfCheck()` 离屏 `BufferedImage` 绘制 + `SwingUtilities.invokeAndWait` + `dispatchEvent` 模式。
- 每批在 `selfCheck()` 内新增针对性断言（见上表），并在末尾 `System.out.println("AstTable self-check OK")`。
- 新建 `src/org/swelement/demo/AstTableDemo.java`：9 个 section 分别演示 C1–C9，外加综合示例；纳入 `build.bat` 的 SOURCES 与自检注册。
- 编译：`javac -encoding UTF-8 --release 8 -d out`（本机用系统 `javac`，JDK 26 支持 `--release 8`）；自检 `java -ea -cp out org.swelement.ui.AstTable` 与 `...AstTableDemo`。
- 全绿标准：编译零错误（仅 JDK8 弃用警告可接受）、self-check 全绿、demo 可运行展示。

## 10. 验收标准

- [ ] C1–C9 全部实现并各自独立提交，每批 self-check 全绿。
- [ ] 原始公开 API 与尺寸档位无回归。
- [ ] `AstTableDemo` 覆盖 9 类用法，可运行。
- [ ] 编译 `--release 8` 零错误。
- [ ] 对比度遵循 WCAG 2.1 AA（选中/状态/斑马纹均满足 `ElementTheme.assertContrast`）。

## 11. 风险与缓解

- **冻结列 + 多级表头 + 横滚的 clip 叠加**（最复杂）：放 C2/C3 先打地基，单测 clip 矩形坐标；C9 合并放最后以免互相干扰坐标计算。
- **滚动与合并/展开高度耦合**：视图行高在 C7/C9 变为非均匀，`BodyView` 行 Y 改为由「行高表」累加（均匀默认 rowH，展开区/合并行特殊高度），避免 `(y-headerH)/rowH` 假设。
- **性能**：冻结列每帧重绘全部行，数据量适中（≤数百行）可接受；超限再做脏矩形优化（不在本轮）。
