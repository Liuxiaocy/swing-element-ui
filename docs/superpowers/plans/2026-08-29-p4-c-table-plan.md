# AstTable 类型增强（P4-C）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `AstTable` 升级为具备固定表头/冻结列/多级表头/单选多选/排序/筛选/展开行/合计行/合并单元格与状态行的 Element UI 风格表格，C1→C9 每批独立提交且 self-check 全绿。

**Architecture:** `AstTable` 由单 `JComponent` 重构为容器，内含 `HeaderView`(吸顶) + `BodyView`(自管视口 `scrollX/scrollY`) + `FooterView`(合计)；数据与视图抽离到 `AstTableModel`（原始行 + 视图索引 + 选择 + 展开集 + spanMap + 合计聚合）；列模型抽离到 `AstTableColumn`（含 `children` 多级与 `fixed` 冻结）。旧 `Column` 保留为 `AstTableColumn` 薄子类，原始公开 API 全部向后兼容。冻结列/多级表头用 clip 在固定 X 区重绘覆盖。

**Tech Stack:** Java 8 Swing（纯自绘，不混入 JTable）；`javac --release 8` 编译；`AstTable.selfCheck()` 离屏绘制 + `dispatchEvent` 作测试；`ElementTheme.assertContrast` 保证 WCAG 2.1 AA。

**Spec:** `docs/superpowers/specs/2026-08-29-p4-c-table-design.md`

## Global Constraints

- 编译命令：`javac -encoding UTF-8 --release 8 -d out`（本机 JDK 26 支持 `--release 8`）；自检 `java -ea -cp out org.swelement.ui.AstTable` 与 `...AstTableDemo`。
- 编译必须零错误（仅 JDK8 弃用警告可接受）；self-check 必须全绿。
- 尺寸档位 `SIZE_LARGE/DEFAULT/SMALL` 表头高 44/36/32、行高 40/32/28 **保留**，任何重构不得破坏。
- 向后兼容红线：`AstTable(Column[])`、`addRow(Object...)`、`setRows`、`clearRows`、`getValueAt`、`getRowCount`/`getColumnCount`、`getSelectedRow`/`setSelectedRow`、`setRowClickListener`、`setSize`、既有 `selfCheck` 断言**全部保持通过**。
- 对比度：选中行/状态行/斑马纹均须通过 `ElementTheme.assertContrast`（选中行可跳过但须在注释标注，沿用 AstCascader/AstTree 惯例）。
- 每批独立 TDD：先写失败 self-check 断言 → 运行确认失败 → 实现 → 运行全绿 → 提交。提交 message 形如 `feat(P4-c/Cn): ...`。

---

## File Structure

- **Create** `src/org/swelement/ui/AstTableColumn.java` — 列模型：`title/width/align/sortable/fixed/children`；`getLeafColumns()/getDepth()/getLeafCount()/isLeaf()`。C1 建立，C3 加 `children`。
- **Create** `src/org/swelement/ui/AstTableModel.java` — 数据视图：原始行 + 视图索引 + 选择 + 展开集 + spanMap + 合计聚合 + 排序/筛选。C1 建立骨架，后续批逐步加方法。
- **Modify** `src/org/swelement/ui/AstTable.java` — 重构为容器（BorderLayout: HeaderView NORTH / BodyView CENTER / FooterView SOUTH），委托 `AstTableModel`；保留 `Column` 薄子类与旧 API 转发；`selfCheck()` 逐步扩充。
- **Create** `src/org/swelement/demo/AstTableDemo.java` — 9 个 section 演示 C1–C9 + 综合示例；纳入 `build.bat`。
- **Modify** `build.bat` — SOURCES 增加 `AstTableColumn.java AstTableModel.java AstTableDemo.java`；自检段增加 `AstTableDemo` 调用。

每个视图（`HeaderView`/`BodyView`/`FooterView`）作为 `AstTable` 的内部类，避免跨文件耦合；只把 `AstTableColumn`、`AstTableModel` 拆成独立文件。

---

### Task 1: C1 基础重构 + 固定表头 + 纵向滚动

**Files:**
- Create: `src/org/swelement/ui/AstTableColumn.java`
- Create: `src/org/swelement/ui/AstTableModel.java`
- Modify: `src/org/swelement/ui/AstTable.java`（整体重构为容器 + 三个内部视图；保留 `Column` 子类与旧 API）
- Test: `src/org/swelement/ui/AstTable.java` 的 `selfCheck()`（同文件静态方法）

**Interfaces:**
- Consumes: 现有 `ElementTheme`（FILL_BASE/PRIMARY/BORDER_BASE/TEXT_MAIN/assertContrast）、`Align` 枚举、`Animator`/`Easing`（hover 动画沿用）。
- Produces:
  - `AstTableColumn(String title, int width)`、`AstTableColumn(String title, int width, Align align)`、`AstTableColumn.getLeafColumns(): List<AstTableColumn>`、`isLeaf(): boolean`、`getDepth(): int`。
  - `AstTableModel(List<AstTableColumn> cols)`；`addRow(Object...)`、`setRows(List<Object[]>)`、`clearRows()`、`rawRowCount()`、`viewRowCount()`、`getValueAtView(int v, int leafCol)`、`rawRowOf(int v)`、`getLeafColumns()`。
  - `AstTable(AstTableColumn[] cols)` 与 `AstTable(Column[] cols)`（适配）；`setModel(AstTableModel)`。
  - `BodyView` 暴露 `scrollY` 与 `setViewportHeight(int)`；`HeaderView` 高度 = `headerDepth * headerH`。

- [ ] **Step 1: 写失败的 self-check 断言（C1 相关）**

在 `AstTable.selfCheck()` 末尾追加（先让旧结构缺方法而编译/运行失败）：

```java
// --- C1: 列模型 + 模型 + 容器重构 ---
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

// 纵向滚动：ContentPane 限制高度，表头吸顶、body 可滚到最后一行
final int[] lastVisible = {-1};
final Throwable[] err1 = {null};
try { SwingUtilities.invokeAndWait(new Runnable(){ public void run(){
    JFrame jf = new JFrame("C1"); jf.setSize(500, 160); jf.setVisible(true);
    try {
        AstTable tt = new AstTable(new AstTableColumn[]{
            new AstTableColumn("姓名",120), new AstTableColumn("年龄",80), new AstTableColumn("地址",200)});
        for (int i=0;i<20;i++) tt.addRow("u"+i, i, "addr"+i);
        JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new BorderLayout());
        cp.add(tt, BorderLayout.CENTER); jf.validate();
        int headerTop = tt.getHeaderView().getY();           // 吸顶：始终 0
        int beforeScrollLast = tt.getBodyView().lastViewRowVisible();
        tt.getBodyView().scrollToRow(19);                    // 滚到最后
        tt.getBodyView().revalidate();
        int afterScrollLast = tt.getBodyView().lastViewRowVisible();
        assert headerTop == 0 : "C1 表头吸顶 Y=0";
        assert afterScrollLast > beforeScrollLast : "C1 滚动后可见末行后移";
        assert tt.getBodyView().lastViewRowVisible() == 19 : "C1 能滚到末行(19)";
    } finally { jf.dispose(); }
}}); } catch (Throwable t){ err1[0]=t; }
if (err1[0]!=null) throw new RuntimeException(err1[0]);
```

- [ ] **Step 2: 运行确认失败**

Run: `javac -encoding UTF-8 --release 8 -d out src/org/swelement/ui/AstTableColumn.java src/org/swelement/ui/AstTableModel.java src/org/swelement/ui/AstTable.java && java -ea -cp out org.swelement.ui.AstTable`
Expected: 编译失败（缺少 `AstTableColumn`/`AstTableModel`/`getHeaderView`/`getBodyView`/`scrollToRow`/`lastViewRowVisible`）→ 证明 RED。

- [ ] **Step 3: 写最小实现**

`AstTableColumn.java`（叶子列 + 拍平）：
```java
package org.swelement.ui;
public class AstTableColumn {
    public enum Fixed { NONE, LEFT, RIGHT }
    public final String title; public final int width; public final Align align;
    public final boolean sortable; public final Fixed fixed;
    public final java.util.List<AstTableColumn> children; // 多级表头，叶子为 null
    public AstTableColumn(String title, int width){ this(title,width,Align.LEFT,false,Fixed.NONE,null); }
    public AstTableColumn(String title, int width, Align align){ this(title,width,align,false,Fixed.NONE,null); }
    public AstTableColumn(String title, int width, Align align, boolean sortable, Fixed fixed, java.util.List<AstTableColumn> children){
        if(title==null) throw new IllegalArgumentException("title null");
        if(width<24) throw new IllegalArgumentException("width<24");
        this.title=title; this.width=width; this.align=align; this.sortable=sortable; this.fixed=fixed;
        this.children = children;
    }
    public boolean isLeaf(){ return children==null || children.isEmpty(); }
    public java.util.List<AstTableColumn> getLeafColumns(){
        java.util.List<AstTableColumn> out = new java.util.ArrayList<>();
        if(isLeaf()){ out.add(this); } else { for(AstTableColumn ch:children) out.addAll(ch.getLeafColumns()); }
        return out;
    }
    public int getDepth(){ if(isLeaf()) return 1; int d=0; for(AstTableColumn ch:children) d=Math.max(d,ch.getDepth()); return d+1; }
}
```

`AstTableModel.java`（骨架：原始行 + 视图索引，C1 仅纵向滚动/选择无关）：
```java
package org.swelement.ui;
import java.util.*;
public class AstTableModel {
    private final List<AstTableColumn> columns;
    private final List<Object[]> raw = new ArrayList<>();
    private List<Integer> view = new ArrayList<>();
    public AstTableModel(List<AstTableColumn> cols){ this.columns=cols; }
    public List<AstTableColumn> getLeafColumns(){ List<AstTableColumn> out=new ArrayList<>(); for(AstTableColumn c:columns) out.addAll(c.getLeafColumns()); return out; }
    public int leafCount(){ return getLeafColumns().size(); }
    public void addRow(Object... v){ Object[] copy=Arrays.copyOf(v,v.length); raw.add(copy); view.add(raw.size()-1); }
    public void setRows(List<Object[]> data){ raw.clear(); view.clear(); for(Object[] r:data){ raw.add(Arrays.copyOf(r,r.length)); view.add(raw.size()-1);} }
    public void clearRows(){ raw.clear(); view.clear(); }
    public int rawRowCount(){ return raw.size(); }
    public int viewRowCount(){ return view.size(); }
    public int rawRowOf(int v){ return view.get(v); }
    public Object getValueAtView(int v, int leafCol){ return raw.get(view.get(v))[leafCol]; }
}
```

`AstTable.java` 重构为容器（关键结构，C1 只实现纵向滚动；横向/冻结留字段）：
```java
public class AstTable extends JPanel {
    public static final class Column extends AstTableColumn { // 向后兼容薄子类
        public Column(String t,int w){ super(t,w); } public Column(String t,int w,Align a){ super(t,w,a); }
    }
    private final AstTableModel model;
    private final HeaderView headerView; private final BodyView bodyView; private final FooterView footerView;
    // 尺寸档位沿用 TIER_HEADER_H/ROW_H/FONT
    public AstTable(AstTableColumn[] cols){ this(new AstTableModel(Arrays.asList(cols))); }
    public AstTable(Column[] cols){ this(Arrays.stream(cols).map(c->(AstTableColumn)c).toArray(AstTableColumn[]::new)); }
    public AstTable(AstTableModel m){ this.model=m; setLayout(new BorderLayout());
        headerView=new HeaderView(); bodyView=new BodyView(); footerView=new FooterView();
        add(headerView, BorderLayout.NORTH); add(bodyView, BorderLayout.CENTER); add(footerView, BorderLayout.SOUTUTH/*SOUTH*/);
        applyTier(); }
    public HeaderView getHeaderView(){ return headerView; }
    public BodyView getBodyView(){ return bodyView; }
    // 旧 API 转发到 model
    public int getColumnCount(){ return model.leafCount(); }
    public int getRowCount(){ return model.viewRowCount(); }
    public Object getValueAt(int r,int c){ return model.getValueAtView(r,c); }
    public void addRow(Object... v){ model.addRow(v); revalidate(); repaint(); }
    // BodyView 提供 scrollToRow / lastViewRowVisible / scrollY
}
```
`BodyView extends JComponent`：`paintComponent` 按 `headerH + scrollY` 偏移绘制 `model.viewRowCount()` 行；`scrollToRow(int v)` 设 `scrollY = v*rowH - viewportH/2` 并 clamp；`lastViewRowVisible()` 返回 `scrollY/rowH + viewportH/rowH` 经 clamp；`getPreferredSize` 返回内容总高（header+rows）使外部可限制高度触发滚动；`HeaderView` 固定高 = `headerDepth*headerH`，不随 body 滚动。鼠标滚轮 → `bodyView.scrollY += delta` 并 repaint。

- [ ] **Step 4: 运行确认全绿**

Run: 同上编译 + `java -ea -cp out org.swelement.ui.AstTable`
Expected: PASS（打印 `AstTable self-check OK`），含旧断言（null 守卫/尺寸档位/点击）与新增 C1 断言。

- [ ] **Step 5: 提交**

```bash
git add src/org/swelement/ui/AstTableColumn.java src/org/swelement/ui/AstTableModel.java src/org/swelement/ui/AstTable.java
git commit -m "feat(P4-c/C1): 重构为列模型+数据视图+分层渲染，固定表头+纵向滚动"
```

---

### Task 2: C2 横向滚动 + 左/右冻结列

**Files:**
- Modify: `src/org/swelement/ui/AstTableColumn.java`（加 `fixed` 构造器——若 Task1 已含则跳过）
- Modify: `src/org/swelement/ui/AstTable.java`（`BodyView`/`HeaderView` 加 `scrollX` 与 clip 重绘；`frozenLeftW`/`frozenRightW`）
- Test: `AstTable.selfCheck()` 追加 C2 段

**Interfaces:**
- Consumes: `AstTableColumn.Fixed`；`model.getLeafColumns()` 顺序与宽度。
- Produces: `AstTableColumn(String, int, Align, boolean sortable, Fixed fixed, List children)`；`BodyView.scrollX`、`frozenLeftW()`、`frozenRightW()`、`isFrozenLeft(int leaf)`/`isFrozenRight(int leaf)`；`BodyView.paintComponent` 三段式 clip。

- [ ] **Step 1: 写失败断言（C2）**

```java
// C2 冻结列：左冻结姓名列随横滚 X 不动；中列随动
final Throwable[] err2={null};
try { SwingUtilities.invokeAndWait(new Runnable(){ public void run(){
    JFrame jf=new JFrame("C2"); jf.setSize(360,200); jf.setVisible(true);
    try {
        AstTableColumn name=new AstTableColumn("姓名",100,Align.LEFT,false,AstTableColumn.Fixed.LEFT,null);
        AstTableColumn age =new AstTableColumn("年龄",90,Align.CENTER);
        AstTableColumn addr=new AstTableColumn("地址",260,Align.LEFT);
        AstTableColumn tag =new AstTableColumn("标签",260,Align.LEFT);
        AstTable tt=new AstTable(new AstTableColumn[]{name,age,addr,tag});
        for(int i=0;i<10;i++) tt.addRow("n"+i,i,"a-long-address-"+i,"t"+i);
        JPanel cp=(JPanel)jf.getContentPane(); cp.setLayout(new BorderLayout()); cp.add(tt,BorderLayout.CENTER); jf.validate();
        int leafW = tt.getBodyView().frozenLeftW();
        assert leafW==100 : "C2 左冻结宽=100";
        int xBefore = tt.getBodyView().leafXOnScreen(1); // 年龄列左缘屏幕X
        tt.getBodyView().scrollX = 120; tt.getBodyView().repaint();
        int xNameAfter = tt.getBodyView().leafXOnScreen(0); // 姓名(冻结)屏幕X
        int xAgeAfter = tt.getBodyView().leafXOnScreen(1);  // 年龄(中)屏幕X
        assert xNameAfter == 0 : "C2 冻结列横滚后仍贴左(0)";
        assert xAgeAfter < xBefore : "C2 中列横滚后左移";
        assert leafW + (tt.getBodyView().frozenRightW()) < tt.getWidth() : "C2 冻结宽小于视口";
    } finally { jf.dispose(); }
}}); } catch(Throwable t){ err2[0]=t; }
if(err2[0]!=null) throw new RuntimeException(err2[0]);
```

- [ ] **Step 2: 运行确认失败**（缺 `Fixed` 构造/`frozenLeftW`/`leafXOnScreen`/`scrollX`）。

- [ ] **Step 3: 实现** `BodyView.paintComponent` 三段式：

```java
// 1) 中列：clip 到 [frozenLeftW, W-frozenRightW]，偏移 (-scrollX,-scrollY)
g2.clipRect(frozenLeftW, 0, W-frozenLeftW-frozenRightW, H);
paintRows(g2, -scrollX, -scrollY);
g2.setClip(null);
// 2) 左冻结：clip [0,frozenLeftW]，只画 fixed==LEFT 叶子，scrollX=0
if(frozenLeftW>0){ g2.clipRect(0,0,frozenLeftW,H); paintRows(g2, 0, -scrollY, c->c.fixed==LEFT); g2.setClip(null);
    g2.drawLine(frozenLeftW,0,frozenLeftW,H); } // 边界阴影
// 3) 右冻结：clip [W-frozenRightW,W]，画 fixed==RIGHT，X 对齐 totalW-frozenRightW-scrollX
if(frozenRightW>0){ g2.clipRect(W-frozenRightW,0,frozenRightW,H); paintRows(g2, totalLeafW-frozenRightW-scrollX, -scrollY, c->c.fixed==RIGHT); g2.setClip(null);
    g2.drawLine(W-frozenRightW,0,W-frozenRightW,H); }
```
`paintRows(g2, offX, offY, Predicate<AstTableColumn> colFilter)` 仅画 `colFilter` 通过的叶子列，`leafX(leaf)=累计宽度+offX`。`HeaderView` 用同样三段式（横向偏移仅 `scrollX`）。`scrollX` 由 Shift+滚轮或拖拽更新并 clamp 到 `[0, max(0,totalLeafW-W)]`。`frozenLeftW/frozenRightW` 由叶子列 `fixed` 累加。

- [ ] **Step 4: 运行全绿**（编译+`java -ea`）。
- [ ] **Step 5: 提交** `git commit -m "feat(P4-c/C2): 横向滚动 + 左/右冻结列(clip 重绘)"`

---

### Task 3: C3 多级表头

**Files:**
- Modify: `src/org/swelement/ui/AstTable.java`（`HeaderView` 多行递归绘制；`headerDepth` 用 `model.columns` 最大 `getDepth()`）
- Test: `selfCheck()` 追加 C3

**Interfaces:**
- Consumes: `AstTableColumn.children`/`getLeafColumns()`/`getDepth()`。
- Produces: `AstTableColumn(String title, List<AstTableColumn> children)`；`HeaderView` 行数 = `headerDepth`；父列跨子列宽度居中、底部分隔线。

- [ ] **Step 1: 写失败断言（C3）**

```java
// C3 多级表头：层级数、叶子拍平顺序与偏移
AstTableColumn name=new AstTableColumn("姓名",100);
AstTableColumn city=new AstTableColumn("城市",120);
AstTableColumn street=new AstTableColumn("街道",160);
AstTableColumn addr=new AstTableColumn("地址",Arrays.asList(city,street)); // 两级
AstTableColumn age=new AstTableColumn("年龄",80);
AstTable t3=new AstTable(new AstTableColumn[]{name,addr,age});
assert t3.getHeaderView().getDepth()==2 : "C3 层级=2";
List<AstTableColumn> leaves=t3.getModel().getLeafColumns();
assert leaves.size()==4 : "C3 叶子=4(姓名/城市/街道/年龄)";
assert leaves.get(1)==city && leaves.get(2)==street : "C3 叶子顺序 城市→街道";
assert t3.getHeaderView().leafX(1)==100 : "C3 城市列X=100(姓名宽后)";
assert t3.getHeaderView().leafX(2)==220 : "C3 街道X=220";
```

- [ ] **Step 2: 运行失败**（缺 `children` 构造/`getDepth`/`leafX`）。
- [ ] **Step 3: 实现** `HeaderView.paintComponent`：递归 `paintGroup(col, x, depth)`——若叶子，于底行画标题；若父列，`groupW=Σ子叶子宽`，于该行居中画标题并 `drawLine` 跨 `x..x+groupW` 底边；`headerH = depth*rowH`。`leafX(leaf)` 由 `getLeafColumns()` 前缀宽度和。`headerView` 高度随 `depth` 变化，`BodyView` 顶部起始 Y = `headerH`。
- [ ] **Step 4: 运行全绿。**
- [ ] **Step 5: 提交** `git commit -m "feat(P4-c/C3): 多级表头(children 跨列合并)"`

---

### Task 4: C4 单选 / 多选

**Files:**
- Modify: `src/org/swelement/ui/AstTableModel.java`（`setSelectionMode`/`selectedViewRow`/`selectedViewRows`/`toggleSelectedViewRow`/`toggleSelectAll`）
- Modify: `src/org/swelement/ui/AstTable.java`（`BodyView` 画选择列复选框；点击行改选；多选模式加首列选择列；`rowClickListener` 改传视图行）
- Test: `selfCheck()` 追加 C4

**Interfaces:**
- Consumes: `model.viewRowCount()`。
- Produces: `AstTableModel.setSelectionMode(SINGLE|MULTIPLE)`；
  `model.getSelectedViewRow()`、`model.isSelectedView(int v)`、`model.getSelectedViewRows():Set<Integer>`；
  `AstTable.setSelectionMode(...)`；`AstTable.getSelectedRow()` 返回 `model.rawRowOf(selectedViewRow)`（保持旧语义"数据行"）或视图行——**spec 约定 `rowClickListener` 传视图行**，但 `getSelectedRow` 维持旧"数据行"语义（向后兼容），文档注明。

- [ ] **Step 1: 写失败断言（C4）**

```java
// C4 单选/多选
final Throwable[] err4={null};
try { SwingUtilities.invokeAndWait(new Runnable(){ public void run(){
    JFrame jf=new JFrame("C4"); jf.setSize(420,200); jf.setVisible(true);
    try {
        AstTable t4=new AstTable(new AstTableColumn[]{new AstTableColumn("姓名",120),new AstTableColumn("年龄",80)});
        for(int i=0;i<5;i++) t4.addRow("u"+i,i);
        t4.setSelectionMode(AstTable.SELECTION_MULTIPLE);
        JPanel cp=(JPanel)jf.getContentPane(); cp.setLayout(new BorderLayout()); cp.add(t4,BorderLayout.CENTER); jf.validate();
        int y2=t4.getHeaderHeight()+2*t4.getRowHeight()+t4.getRowHeight()/2;
        t4.dispatchEvent(new MouseEvent(t4,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,30,y2,1,false));
        assert t4.getModel().isSelectedView(2) : "C4 多选点中行2";
        int y3=t4.getHeaderHeight()+3*t4.getRowHeight()+t4.getRowHeight()/2;
        t4.dispatchEvent(new MouseEvent(t4,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,30,y3,1,false));
        assert t4.getModel().isSelectedView(2)&&t4.getModel().isSelectedView(3) : "C4 多选累加";
        t4.setSelectionMode(AstTable.SELECTION_SINGLE);
        int y1=t4.getHeaderHeight()+1*t4.getRowHeight()+t4.getRowHeight()/2;
        t4.dispatchEvent(new MouseEvent(t4,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,30,y1,1,false));
        assert t4.getModel().getSelectedViewRow()==1 && t4.getModel().getSelectedViewRows().size()==1 : "C4 单选互斥";
    } finally { jf.dispose(); }
}}); } catch(Throwable t){ err4[0]=t; }
if(err4[0]!=null) throw new RuntimeException(err4[0]);
```

- [ ] **Step 2: 运行失败**（缺选择模式/方法）。
- [ ] **Step 3: 实现** `AstTableModel` 加 `selectionMode` 字段、`selectedViewRow`/`selectedViewRows`；`toggleSelectedViewRow` 多选增删、单选置位；`toggleSelectAll` 全选/清空。多选模式 `HeaderView` 首列画全选复选框、`BodyView` 首列画行复选框；点击复选框或行触发选择；`getSelectedRow()` 经 `rawRowOf` 映射回数据行以保持旧语义。
- [ ] **Step 4: 运行全绿。**
- [ ] **Step 5: 提交** `git commit -m "feat(P4-c/C4): 单选/多选(选择列复选框)"`

---

### Task 5: C5 排序

**Files:**
- Modify: `src/org/swelement/ui/AstTableModel.java`（`sort(int leafCol, SortDir)`/`SortDir`/`getSortDir`/`getSortLeaf`）
- Modify: `src/org/swelement/ui/AstTable.java`（`HeaderView` 在 `sortable` 叶子画三角；点击循环 NONE→ASC→DESC→NONE）
- Test: `selfCheck()` 追加 C5

**Interfaces:**
- Consumes: `model.getLeafColumns()`、`getValueAtView`。
- Produces: `enum SortDir{ASC,DESC,NONE}`；`model.sort(int leafCol, SortDir)`（按 `raw` 值比较重建 `view`）；`model.getSortLeaf()`/`getSortDir()`。

- [ ] **Step 1: 写失败断言（C5）**

```java
// C5 排序：年龄升序后 view 行序 0,1,2 对应年龄 22,28,34
AstTable t5=new AstTable(new AstTableColumn[]{new AstTableColumn("姓名",120),new AstTableColumn("年龄",80,Align.CENTER,true,Fixed.NONE,null)});
t5.addRow("王",22); t5.addRow("张",34); t5.addRow("李",28);
t5.getModel().sort(1, AstTableModel.SortDir.ASC);
assert (Integer)t5.getValueAt(0,1)==22 && (Integer)t5.getValueAt(1,1)==28 && (Integer)t5.getValueAt(2,1)==34 : "C5 升序";
t5.getModel().sort(1, AstTableModel.SortDir.DESC);
assert (Integer)t5.getValueAt(0,1)==34 && (Integer)t5.getValueAt(2,1)==22 : "C5 降序";
t5.getModel().sort(1, AstTableModel.SortDir.NONE);
assert (Integer)t5.getValueAt(0,1)==22 : "C5 NONE 还原原始序";
```

- [ ] **Step 2: 运行失败**（缺 `SortDir`/`sort`）。
- [ ] **Step 3: 实现** `model.sort` 用 `Comparator` 对 `view` 重排（数值按 `Number`，文本按 `String`）；非 `sortable` 列点击忽略。`HeaderView` 对激活列画 ▲/▼。
- [ ] **Step 4: 运行全绿。**
- [ ] **Step 5: 提交** `git commit -m "feat(P4-c/C5): 排序(升/降/无 + 表头三角)"`

---

### Task 6: C6 筛选

**Files:**
- Modify: `src/org/swelement/ui/AstTableModel.java`（`filter(Predicate)`/`clearFilter()`/`getFilterActive()`）
- Modify: `src/org/swelement/ui/AstTable.java`（`HeaderView` 筛选项画图标；点击弹简易文本输入，输入即 `filter` 含子串）
- Test: `selfCheck()` 追加 C6

**Interfaces:**
- Consumes: `model.raw`/`viewRowCount`。
- Produces: `model.filter(Predicate<Object[]>)`（重建 `view` 仅含匹配行）；`model.clearFilter()`；文本过滤默认 `row -> Arrays.stream(row).anyMatch(v -> String.valueOf(v).contains(q))`。

- [ ] **Step 1: 写失败断言（C6）**

```java
// C6 筛选：仅保留含 "上海" 的行
AstTable t6=new AstTable(new AstTableColumn[]{new AstTableColumn("城市",160),new AstTableColumn("人数",80)});
t6.addRow("北京市",100); t6.addRow("上海市",200); t6.addRow("广州市",150);
t6.getModel().filter(r -> "上海市".equals(r[0]));
assert t6.getRowCount()==1 : "C6 过滤后1行";
assert "上海市".equals(t6.getValueAt(0,0)) : "C6 剩上海";
t6.getModel().clearFilter();
assert t6.getRowCount()==3 : "C6 清空还原3行";
```

- [ ] **Step 2: 运行失败**（缺 `filter`）。
- [ ] **Step 3: 实现** `model.filter` 重建 `view`；`HeaderView` 对可过滤列画漏斗图标，点击经 `AstPopover`/内联输入框触发 `filter`；本轮回文本过滤，复杂条件留 `filter(Predicate)` 接口。
- [ ] **Step 4: 运行全绿。**
- [ ] **Step 5: 提交** `git commit -m "feat(P4-c/C6): 筛选(列文本过滤)"`

---

### Task 7: C7 展开行

**Files:**
- Modify: `src/org/swelement/ui/AstTableModel.java`（`expanded` 集、`toggleExpanded(int rawRow)`、`isExpanded(int rawRow)`、`viewRowCount()` 含展开块）
- Modify: `src/org/swelement/ui/AstTable.java`（`BodyView` 在展开行下插入展开区；行首展开箭头；`setRowExpandRenderer(BiFunction<Integer,? extends JComponent>)` 或简单 `setRowExpandText(Function)`）
- Test: `selfCheck()` 追加 C7

**Interfaces:**
- Consumes: `model.viewRowOf`/`rawRowOf`。
- Produces: `model.toggleExpanded(int rawRow)`；`model.isExpanded(int rawRow)`；`AstTable.setRowExpandRenderer(...)`；展开态视图行高 = `rowH + expandH`。

- [ ] **Step 1: 写失败断言（C7）**

```java
// C7 展开行：展开后视图行数 +1，收起还原
AstTable t7=new AstTable(new AstTableColumn[]{new AstTableColumn("姓名",120),new AstTableColumn("详情",200)});
t7.addRow("张","..."); t7.addRow("李","...");
final int base=t7.getRowCount();
t7.setRowExpandText(r -> "展开内容#"+r);
t7.getModel().toggleExpanded(0);
assert t7.getRowCount()==base+1 : "C7 展开后视图行+1("+(base+1)+")";
t7.getModel().toggleExpanded(0);
assert t7.getRowCount()==base : "C7 收起还原";
```

- [ ] **Step 2: 运行失败**（缺 `toggleExpanded`/`setRowExpandText`）。
- [ ] **Step 3: 实现** `model.viewRowCount` = `rawView + expandedCount`；`BodyView` 绘制时遇展开行，在其下追加展开区（`expandRenderer` 产出高度）；点击行首 ▶/▼ 触发 `toggleExpanded` 并重算布局。
- [ ] **Step 4: 运行全绿。**
- [ ] **Step 5: 提交** `git commit -m "feat(P4-c/C7): 展开行(行内子内容)"`

---

### Task 8: C8 表尾合计行

**Files:**
- Modify: `src/org/swelement/ui/AstTableModel.java`（`setSummary(int leafCol, Aggregator)`/`getSummary(int leafCol)`；默认数值列 `sum`）
- Modify: `src/org/swelement/ui/AstTable.java`（`FooterView` 固定底部；仅当 `summary` 非空显示；冻结列对齐同 BodyView）
- Test: `selfCheck()` 追加 C8

**Interfaces:**
- Consumes: `model.raw`/`getLeafColumns`。
- Produces: `model.setSummary(int leafCol, Aggregator)`（`Aggregator{ Object apply(List<Object[]> rows); }`，默认 `sum`）；`model.getSummary(int leafCol)`；`AstTable.setSummary(int leafCol, Aggregator)`。

- [ ] **Step 1: 写失败断言（C8）**

```java
// C8 合计：年龄列求和 = 22+34+28=84
AstTable t8=new AstTable(new AstTableColumn[]{new AstTableColumn("姓名",120),new AstTableColumn("年龄",80)});
t8.addRow("张",22); t8.addRow("李",34); t8.addRow("王",28);
t8.setSummary(1, null); // 默认 sum
assert (Integer)t8.getModel().getSummary(1)==84 : "C8 合计=84";
assert t8.getFooterView().isVisible() : "C8 有合计时 footer 可见";
```

- [ ] **Step 2: 运行失败**（缺 `setSummary`/`getSummary`/`FooterView`）。
- [ ] **Step 3: 实现** `model.setSummary` 注册聚合器；`getSummary` 遍历 `raw` 累加数值（非数值列返回空或「合计」标签）。`FooterView` 底部固定，按 `getLeafColumns()` 逐列绘制 `getSummary`，冻结列 clip 对齐同 BodyView。
- [ ] **Step 4: 运行全绿。**
- [ ] **Step 5: 提交** `git commit -m "feat(P4-c/C8): 表尾合计行(按列聚合)"`

---

### Task 9: C9 合并行/列 + 带状态表格

**Files:**
- Modify: `src/org/swelement/ui/AstTableModel.java`（`setSpan(int rawRow,int leafCol,int rowspan,int colspan)`/`getSpan`；`rowStatus` map/`setRowStatus`/`getRowStatus`）
- Modify: `src/org/swelement/ui/AstTable.java`（`BodyView.paintCells` 查 span 跳过被覆盖格、锚点跨区；`rowStatus` 行底色）
- Test: `selfCheck()` 追加 C9

**Interfaces:**
- Consumes: `model.getLeafColumns()`/`getValueAtView`。
- Produces: `model.setSpan(int raw,int leaf,int rowspan,int colspan)`；`model.getSpan(int raw,int leaf)`→`int[]{rowspan,colspan}`（默认 `{1,1}`）；`model.setRowStatus(int raw, Status)`/`getRowStatus`；`enum Status{DEFAULT,SUCCESS,WARNING,DANGER,INFO}`。

- [ ] **Step 1: 写失败断言（C9）**

```java
// C9 合并：A1 跨 2 行 1 列；状态行底色
AstTable t9=new AstTable(new AstTableColumn[]{new AstTableColumn("A",100),new AstTableColumn("B",100)});
t9.addRow("x","1"); t9.addRow("x","2"); t9.addRow("y","3");
t9.getModel().setSpan(0,0,2,1); // A 列 0,1 行合并
int[] sp=t9.getModel().getSpan(0,0);
assert sp[0]==2 && sp[1]==1 : "C9 合并 2x1";
assert Arrays.equals(t9.getModel().getSpan(0,1), new int[]{1,1}) : "C9 非合并默认 1x1";
t9.getModel().setRowStatus(2, AstTableModel.Status.SUCCESS);
assert t9.getModel().getRowStatus(2)==AstTableModel.Status.SUCCESS : "C9 状态行";
// 离屏绘制不抛断言
BufferedImage img=new BufferedImage(220,120,BufferedImage.TYPE_INT_ARGB);
Graphics2D gg=img.createGraphics(); try{ t9.paint(gg);} finally{ gg.dispose();}
```

- [ ] **Step 2: 运行失败**（缺 `setSpan`/`getSpan`/`Status`/`setRowStatus`）。
- [ ] **Step 3: 实现** `paintCells`：先标记 `(r+dr,c+dc)` 为 covered；绘制锚点格时矩形扩展 `rowspan*rowH × Σcolspan宽`；被覆盖格跳过。状态行：`BODY_BG` 用 `FILL_SUCCESS/WARNING/DANGER/INFO` 浅色，文字 `TEXT_MAIN`（满足 contrast），选中用左 3px PRIMARY 描边。合并行假设同 `rowH`。
- [ ] **Step 4: 运行全绿。**
- [ ] **Step 5: 提交** `git commit -m "feat(P4-c/C9): 合并行/列 + 带状态表格"`

---

### Task 10: AstTableDemo 综合演示 + build.bat 注册

**Files:**
- Create: `src/org/swelement/demo/AstTableDemo.java`
- Modify: `build.bat`

**Interfaces:**
- Consumes: 全部 C1–C9 API。
- Produces: 可运行 demo（9 section + 综合）；`main` 触发各 section 构建。

- [ ] **Step 1: 写 AstTableDemo** 9 个 `JPanel` section 分别展示 C1 固定表头+滚动、C2 冻结列、C3 多级表头、C4 选择、C5 排序、C6 筛选、C7 展开、C8 合计、C9 合并+状态；`main` 用 `JTabbedPane` 组织并 `setVisible`。
- [ ] **Step 2: 注册 build.bat** SOURCES 加 `AstTableColumn.java AstTableModel.java AstTableDemo.java`；自检段加 `java -ea -cp out org.swelement.demo.AstTableDemo`（或仅编译校验）。
- [ ] **Step 3: 全量编译 + 全部 self-check 运行确认通过（含 AstTable + AstTableDemo）。
- [ ] **Step 4: 提交** `git commit -m "feat(P4-c): AstTableDemo 综合演示 + build.bat 注册"`

---

## Self-Review（已执行）

1. **Spec 覆盖**：C1（容器/模型/吸顶滚动）✅ Task1；C2 冻结列 ✅ Task2；C3 多级表头 ✅ Task3；C4 单选多选 ✅ Task4；C5 排序 ✅ Task5；C6 筛选 ✅ Task6；C7 展开 ✅ Task7；C8 合计 ✅ Task8；C9 合并+状态 ✅ Task9；demo/build ✅ Task10。向后兼容（Column 子类、旧 API、尺寸档位、self-check 回归）✅ Task1 强制。
2. **占位符扫描**：无 TBD/TODO；每步含具体断言或签名/算法；未写「类似 Task N」。
3. **类型一致**：`AstTableColumn.Fixed`、`AstTableModel.SortDir`/`Status`/`setSpan`/`getSpan`/`toggleExpanded`/`setSummary`/`getSummary` 在定义任务与引用任务名称一致；`getLeafColumns()`/`getDepth()`/`getValueAtView` 跨任务统一。
4. **风险项**：冻结列+多级表头+横滚 clip 叠加（Task2/3 打地基）；合并放最后（Task9）；行高非均匀在 Task7/9 通过「行高表/展开区高度」处理，未假设 `(y-headerH)/rowH` 全局均匀。
