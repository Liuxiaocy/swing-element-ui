# Swing Element UI 扩展组件设计文档 (P1 → P2 → P3)

> 生成日期：2026-08-21
> 策略：A（价值优先 · P1→P2→P3）
> 现有组件：14 项；待实现候选：21 项；其中 P1 共 14 项（新增组件名统一 `Ast*` 前缀避免与 Java/Javax 组件同名冲突）。

---

## 一、架构层设计

项目整体四层架构保持不变，核心引擎 **不做破坏性修改**，只新增增强能力和辅助类。

```
Application (Demos)                ← 14 个已完成 + 新增 Ast*Demo
  ↓
UI Components Layer                ← 14 done + 21 new (Ast*)
  ↓
Core Engine Layer                  ← Animator, Easing, ElementTheme(增强), AnimatedPopup(增强)
  ↓
Java Swing / AWT
```

### 1.1 现有核心引擎复用

| 核心类 | 复用的组件 | 复用方式 |
|---|---|---|
| **Animator**（Swing Timer 驱动，~15ms/tick） | 全部 Ast* | hover/focus/active/fade/translate 统一插值驱动；禁止引入自定义 `Timer` / `Thread.sleep` |
| **Easing**（linear, easeIn, easeOut, easeInOut） | Tooltip / Dialog / Message / Tree 等 | Tooltip 用 easeOut(220ms)；MessageBox 用 easeInOut(240ms)；Tree 展开高度插值用 easeInOut(240ms) |
| **ElementTheme**（色板, 字体, 圆角, lerp 工具） | 全部 Ast* | 颜色只使用 PRIMARY/SUCCESS/WARNING/DANGER/INFO + TEXT_\* + BORDER_BASE + FILL_BASE，新增色通过 `lerp(a,b,t)`；禁止硬编码新 `new Color(0x...)` 主色 |
| **AnimatedPopup**（JLayeredPane POPUP_LAYER 弹层） | AstTooltip, AstDropdown, AstMessage, AstCascader, AstDatePicker, AstDialog, AstMessageBox | 复用淡入+顶部偏移；新增 3 个向后兼容的可选能力（见 1.2） |

### 1.2 AnimatedPopup 增强（向后兼容，不破坏 Menu/Select）

现有 `show(Component invoker, int x, int y)` 签名保持不变；新增：

- **(a) 方向感知弹出** — 新增 `show(Component invoker, Direction dir)`，枚举值：`ABOVE / BELOW / LEFT / RIGHT / TOP_CENTER / BOTTOM_RIGHT_CORNER`。内部由新增的 `PopupPositioner` 纯计算类输出屏幕坐标，边界自动反折（右边空间不足自动切 LEFT）。
- **(b) 关闭动画钩子** — 新增 `hideWithAnimation(Runnable afterHidden)`。现状 `hidePopup()` 是瞬时 remove；关闭时 `Animator(180ms, easeIn)` 将 alpha 从 1 → 0 + 反向微位移，动画结束后再 `parent.remove(this)` 并回调 `afterHidden`。Tooltip exit、MessageBox confirm、Message timeout 都走这条。
- **(c) 堆叠层级管理** — 新增静态 `registerGlobal(AnimatedPopup p, Layer l)`；层级顺序 `MODAL(最顶) > TOOL > POPUP(默认)`。Modal Dialog 打开时自动激活 GlassPane 拦截下层鼠标事件。

### 1.3 新增 2 个核心辅助类（不耦合现有逻辑）

- **`PopupPositioner`** — 纯计算类，无 Swing 副作用。输入 invoker bounds + screen bounds + popup preferredSize + Direction；输出 `Point screenLocation` 和实际 Direction（边界反折后）。所有使用 AnimatedPopup 的组件统一走它，避免各自写坐标逻辑出现错位。
- **`GlassPane`** — `JPanel` 继承；半透明灰底 `new Color(0,0,0, ~80 alpha)`；`setActive(true)` 时吃掉所有 MouseEvent/KeyEvent（用 `AWTEventListener` 或玻璃面板自身 consume）。用于 Dialog / MessageBox 的模态遮罩。

### 1.4 Demo 层规范（沿用现成）

沿用 [AlertDemo](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/demo/AlertDemo.java) / [BadgeDemo](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/demo/BadgeDemo.java) 的风格：
- 每个 Ast* 组件对应一个 `Ast*Demo` 可独立启动
- `TitledBorder` 分区（功能展示区、交互控制区、状态回显区）
- 关键动画可通过显式"弹出 / 切换 / 排序"按钮观察，不依赖偶然鼠标移过

---

## 二、组件层设计（P1 共 14 项，统一 Ast* 前缀）

### 命名规则

- 新增组件：`AstContainer / AstAvatar / AstCard / AstLoading / AstTooltip / AstDropdown / AstDialog / AstMessageBox / AstMessage / AstCascader / AstDatePicker / AstForm / AstTree / AstTable`
- 对应 Demo：`AstContainerDemo / AstAvatarDemo / ... / AstTableDemo`
- **现有 14 个组件（Alert / Button / Badge / Checkbox / Input / Menu / Pagination / Progress / Radio / Select / Slider / Switch / Tabs / Tag）保持不变**，不做破坏性重命名。

---

### 批次 1：纯绘制 / 纯布局（无 AnimatedPopup 依赖，可首批交付）

#### 2.1 AstContainer — 布局容器

```java
public class AstContainer extends JPanel {
    public static final int VERTICAL = 0, HORIZONTAL = 1;
    public AstContainer(int direction);
    public void setHeader(JComponent h);       // 顶栏（可选）：默认 64px，FILL_BASE 底 + 1px 下边框
    public void setAside(JComponent aside);    // 侧边栏（仅 HORIZONTAL 生效）：默认 220px，1px 右边框
    public void setMain(JComponent main);      // 主区（必填）
    public void setFooter(JComponent f);       // 底栏（可选）：默认 48px
}
```
实现：JPanel + BorderLayout 子分区；无动画。

#### 2.2 AstAvatar — 头像

```java
public class AstAvatar extends JComponent {
    public static final int CIRCLE = 0, SQUARE = 1;
    public AstAvatar(char c, int size, int shape);
    public AstAvatar(ImageIcon icon, int size, int shape);
    public AstAvatar(Color bg, String text, int size, int shape);
    public void setBadgeCount(int n);         // 内部包装现有 AstBadge 组件
    public void setBadgeDot(boolean dot);
}
```
默认 size=40，可选 32/40/64。size=CIRCLE 用 `fillOval`，SQUARE 用 RoundRectangle2D。

#### 2.3 AstCard — 卡片容器

```java
public class AstCard extends JComponent {
    public AstCard(String title);
    public AstCard(String title, boolean bordered, boolean shadowHover);
    public void setContent(JComponent content);
    public void addHeaderAction(JComponent btn);
    public void setShadowElevation(int level);    // 0 无, 1 静态, 2 hover抬升
}
```
hover 态：`Animator(150ms easeInOut)` 插值边框颜色 `BORDER_BASE → PRIMARY` + 外描边宽度。
`isOptimizedDrawingEnabled()=false`（card 边框画在子组件之外）。

#### 2.4 AstLoading — 加载指示

```java
public class AstLoading {
    // 局部：把 target 包进半透明覆盖层 + spinner
    public static AstLoading wrap(JComponent target);
    public void show();     // alpha 0→1 fade-in + spinner 开始旋转
    public void hide();     // fade-out
    // 全局：全屏 GlassPane
    public static void showFullScreen(Window w, String tipText);
    public static void hideFullScreen(Window w);
}
```
spinner：12 段圆弧循环递减 alpha；`Animator(800ms linear)` 每 tick 旋转偏移。局部 Loading 使用 JLayeredPane 层叠原 target，不破坏 target 原有布局。

---

### 批次 2：依赖 AnimatedPopup（Popup / Dialog / Toast 系）

需依赖架构层的 AnimatedPopup 三项增强。

#### 2.5 AstTooltip — 工具提示

```java
public class AstTooltip {
    public static final int ABOVE=0, BELOW=1, LEFT=2, RIGHT=3;
    public AstTooltip(JComponent target, String text, int direction);
    public AstTooltip(JComponent target, JComponent customContent, int direction);
    public void setDelay(int showDelayMs, int hideDelayMs);   // 默认 300ms show / 200ms hide
    public void setAlways(boolean always);                    // 鼠标悬停内容区是否保持
}
```
实现：非继承式。`target.addMouseListener` 在 enter/exit 时调度 `Timer.schedule`（showDelay 内再次 exit 可 cancel，避免抖动）。弹出内容默认 1 行或 2 行文字 + padding。

#### 2.6 AstDropdown — 通用下拉

```java
public class AstDropdown {
    public static final int HOVER=0, CLICK=1;
    public AstDropdown(JComponent trigger, int triggerMode);
    public void addItem(String label, Runnable onClick);
    public void addSeparator();
    public void setPlacement(int direction);     // BELOW / ABOVE / LEFT / RIGHT
}
```
触发：CLICK 模式复用现有 Menu 的 `AWTEventListener` 点外关闭；HOVER 模式同 Tooltip 的延迟。内容显示为 `OptionRow` 风格列表（与现有 Select/Menu 对齐）。

#### 2.7 AstDialog — 对话框

```java
public class AstDialog {
    public AstDialog(String title, JComponent body);
    public void addButton(String text, int type, Runnable onClick);  // type = Button.PRIMARY/SUCCESS/...
    public void setWidth(int w);                         // 默认 520
    public void setClosable(boolean closable);           // 右上角 ×
    public void show(Window parent);
    public void hide();
}
```
打开：`Animator(240ms easeOut)` scale 0.95→1.0 + alpha 0→1；关闭相反。show() 时 GlassPane.setActive(true) 拦截下层。

#### 2.8 AstMessageBox — 全局静态

```java
public final class AstMessageBox {
    public static final int ICON_INFO=0, ICON_SUCCESS=1, ICON_WARNING=2, ICON_ERROR=3, ICON_QUESTION=4;
    public static void alert(Window p, String msg, Runnable onOk);
    public static void confirm(Window p, String msg, Runnable onOk, Runnable onCancel);
    public static void show(Window p, String title, int iconType,
                            JComponent body, Object[] actions);
}
```
内部实例化 AstDialog + 预填图标；`actions`：每个元素为 `String` 或 `Object[]{String label, Integer type, Runnable handler}`。

#### 2.9 AstMessage — 顶部飘条 / Toast

```java
public final class AstMessage {
    public static final int SUCCESS=0, WARNING=1, INFO=2, ERROR=3;
    public static void show(Window w, int type, String text);     // 3 秒自动消失
    public static void show(Window w, int type, String text, int durationMs, boolean showCloseBtn);
}
```
定位：顶部居中 top=20px；多个时由静态 `MessageStack` 管理 y 偏移堆叠（间隔 12px + shadow 不重叠）。type 决定图标色：INFO=PRIMARY, SUCCESS=GREEN, WARNING=ORANGE, ERROR=RED。fade-in 180ms → 停顿 duration → fade-out 220ms。

---

### 批次 3：复杂 Popup + 数据模型

#### 2.10 AstCascader — 级联选择

```java
public class AstCascader extends JComponent {
    public static class Node {
        public final String value, label;
        public final List<Node> children;
        public Node(String label, String value, Node... children);
    }
    public AstCascader(List<Node> options);
    public List<String> getSelectedPath();
    public void addChangeListener(javax.swing.event.ChangeListener l);
    public void setPlaceholder(String ph);
}
```
结构：选择区（Input 风格显示路径）+ AnimatedPopup 内 2~4 列并排滚动面板。选中父节点后立刻在右侧列展开子节点。选叶子节点 → 关闭 Popup → 文本区显示斜杠拼接路径。

#### 2.11 AstDatePicker — 日期选择

```java
public class AstDatePicker extends JComponent {
    public AstDatePicker();
    public AstDatePicker(java.time.LocalDate initial);
    public java.time.LocalDate getDate();
    public void setDate(java.time.LocalDate d);
    public void addChangeListener(javax.swing.event.ChangeListener l);
    public void setRange(java.time.LocalDate min, java.time.LocalDate max);
}
```
输入框（复用 Input 外观）+ 日历 Popup（6×7 表格 + 月份切换箭头）。选中 → fade-out Popup → Input 填入 `yyyy-MM-dd`。日期对象统一 `java.time.LocalDate`，不引入 `java.util.Date`。

---

### 批次 4：重型渲染组件

#### 2.12 AstForm — 表单容器

```java
public class AstForm extends JPanel {
    public static final int LABEL_LEFT = 0, LABEL_TOP = 1;
    public AstForm(int labelPosition, int labelWidth);
    public void addItem(String label, JComponent field);
    public void addItem(String label, JComponent field, String hintText);
    public Map<String, String> collect();     // 对实现 FormField 接口的字段 collect
}
```
布局：LABEL_LEFT 用 GridBag 两列对齐；LABEL_TOP 用 GridBag 一列 label 在上一行 field。hintText 用 `deriveFont(12f) + INFO 灰` + 下边距。

#### 2.13 AstTree — 树形控件

```java
public class AstTree extends JComponent {
    public static class TreeNode {
        public final String id, label;
        public final boolean leaf;
        public final List<TreeNode> children;
        public TreeNode(String id, String label, boolean leaf, TreeNode... children);
    }
    public AstTree(List<TreeNode> roots);
    public void setOnSelect(java.util.function.Consumer<TreeNode> onSelect);
    public void setOnExpand(java.util.function.BiConsumer<TreeNode, Boolean> onExpand);
    public TreeNode getSelected();
    public void setShowCheckbox(boolean show);
}
```
不继承 JTree，纯 List 结构。`Animator(220ms easeInOut)` 驱动展开折叠行的行高插值（从 0 → childCount×32px），平滑展开。缩进：24px/层。箭头 `▸` 展开时 Animator 旋转 90°。

#### 2.14 AstTable — 数据表格

```java
public class AstTable extends JComponent {
    public static class Column {
        public final String key, title;
        public final int width;
        public final boolean sortable;
    }
    public AstTable(List<Column> columns, List<Map<String, Object>> rows);
    public void setStripe(boolean stripe);
    public void setSelectable(boolean singleSelect);
    public int[] getSelectedRows();
    public void addSelectionListener(java.util.function.Consumer<int[]> l);
    public void setBordered(boolean bordered);
    public void sortBy(String columnKey, boolean asc);
}
```
不继承 JTable，纯 List。表头高 48px，sortable 列点击切换 ↑/↓，`Animator(300ms easeOut)` 驱动每行的目标 y 位置与当前 y 插值重绘，实现排序行移动动画。斑马纹：偶数 FILL_BASE / 奇数白。选中行 PRIMARY 半透明蓝底。

---

## 三、统一规范

### 3.1 动画规范

| 类型 | 时长 | Easing | 应用场景 |
|---|---|---|---|
| Hover 渐变 (bg/fg/border) | 150-200ms | easeInOut | AstCard/AstAvatar hover |
| Active/Press | 100-120ms | easeInOut | AstDropdown trigger 按压 |
| Popup 弹入 (fade + y=8→0) | 220ms | easeOut | AstTooltip/AstDropdown/AstCascader/AstDatePicker |
| Popup 弹出 | 180ms | easeIn | 同上关闭 |
| Modal 弹入 (scale+fade) | 240ms | easeOut | AstDialog/AstMessageBox |
| Modal 弹出 | 200ms | easeIn | 同上关闭 |
| Toast fade-in / 停顿 / fade-out | 180 / duration / 220 | easeOut / easeIn | AstMessage |
| Tree 行高展开/折叠 | 200-260ms | easeInOut | AstTree |
| Table 排序行位移 | 300ms | easeOut | AstTable |
| Loading spinner 旋转 | 800ms loop | linear | AstLoading |

约束：每个组件 Animator 实例数 ≤ 3；`paintComponent` 内不得 new Animator；必须走 listener → `repaint()`，不能直接 `setBounds` 引起连锁 revalidate。

### 3.2 主题规范

- 所有新色通过 `ElementTheme.lerp(a,b,t)` 派生，不新增硬编码色板。
- `ElementTheme.RADIUS`（组件）/ `RADIUS*2`（卡片/弹层）；头像圆形无圆角。
- `ElementTheme.FONT` 基础 14px；标题 `deriveFont(BOLD, 16f)`；caption / hint `deriveFont(12f)`。

### 3.3 文字/背景对比度强制规范（WCAG AA：正文对比度 ≥ 4.5:1）

**根原则：固定背景明度 → 反推文字色，不允许各自独立调。**

- **浅底 (背景 L ≥ 55% 或 灰度均值 ≥ 140)**：文字用 TEXT_MAIN / TEXT_REGULAR / TEXT_PLACEHOLDER 深色系；禁止白字。
- **深底 (背景 L < 55%)**：文字用 WHITE 或淡色变体；禁止深字。
- **彩色填充背景 (PRIMARY/SUCCESS/WARNING/DANGER/INFO)**：强制白字。
- **plain/浅彩底**：文字用对应主题色（PRIMARY 等），禁止"浅紫字 + 浅紫底"。

**ElementTheme 新增工具（开发期断言，release 不启断言无运行时负担）：**
```java
public static float luminance(Color c);    // WCAG 近似相对亮度
public static void assertContrast(Color fg, Color bg, String where);  // ratio<4.5 → throw AssertionError
```
用法：每个组件 `paintComponent` 入口对关键文字/背景对调用一次；`selfCheck()` 对所有 state×type 组合全覆盖。

### 3.4 事件与数据模型

- **事件优先用 Swing 标准 listener**：`ChangeListener` / `ActionListener`；简单回调接受 `Consumer<T> / BiConsumer<T1,T2> / Runnable` 函数式参数；不引入自定义 listener 接口类爆炸。
- **数据模型不可变**：Node/Column/Rows 用 public final 字段 + 构造赋值，无 setter；修改需整体重新 `setXxx(newData)`，触发重绘。
- **日期统一 `java.time.LocalDate`**；字符串格式统一 `yyyy-MM-dd`。

### 3.5 错误处理 + Self-Check

Fail-fast 入口校验抛 `IllegalArgumentException`（构造/setXxx）：
- AstDatePicker `setRange(min, max)` min.after(max) → 抛；`setDate(d)` range 外静默 clamp。
- AstTable 列 key 重复 → 抛；`sortBy(key)` key 不存在 → 静默。
- AstTree 循环引用检测 → 抛。
- AstContainer `setMain(null)` → 抛；AstLoading `wrap(null)` → 抛。

每个组件末尾提供 `static void selfCheck()` 纯逻辑断言 + `public static void main(String[] args) { selfCheck(); }`。`build.bat` 末尾追加执行链路，与现有 Pagination/Select 相同。

---

## 四、实施顺序（P1 4 批次 × 交付节奏）

| 批次 | 组件 | 依赖 | 交付后可演示 |
|---|---|---|---|
| ① | AstContainer, AstAvatar, AstCard, AstLoading | 无 | 独立 Demo 4 个 + 已完成组件拼装页面 |
| ② | AstTooltip, AstDropdown, AstDialog, AstMessageBox, AstMessage | 架构层 AnimatedPopup 增强 + PopupPositioner + GlassPane | Tooltip 悬浮、Dialog 模态、Toast 堆叠 |
| ③ | AstCascader, AstDatePicker | AnimatedPopup 方向 + 边界反折 | 省市区级联 / 日期选择 |
| ④ | AstForm, AstTree, AstTable | 无特殊 | 表单收集、树形展开、表格排序动效 |

完成 P1 后进入 P2（7 项：AstInputNumber/AstTimePicker/AstCollapse/AstTransfer/AstSteps/AstBreadcrumb/AstPopover/AstDrawer/AstRate）+ P3（5 项：AstDivider/AstCarousel/AstTimeline/AstCalendar/AstIcon）。

---

## 五、P2 / P3 组件清单（后续设计文档独立拆分）

- **P2**：AstInputNumber, AstTimePicker, AstCollapse, AstTransfer, AstSteps, AstBreadcrumb, AstPopover, AstDrawer, AstRate
- **P3**：AstDivider, AstCarousel, AstTimeline, AstCalendar, AstIcon
- **明确不实现（Web 特有 / Swing 有原生更好替代 / 性价比低）**：ColorPicker, InfiniteScroll, Skeleton, Affix, PageHeader, Result, Empty, Upload（上传通常由业务实现，不强行模仿 Web API）
