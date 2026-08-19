# swing-element-ui Phase 2 设计文档

日期：2026-08-19
状态：已批准（待实现）
前置：Phase 1 已完成（`docs/superpowers/specs/2026-08-19-swing-element-ui-design.md`，核心引擎 Animator/Easing/ElementTheme 与 6 个基础组件）

## 目标

在 Phase 1 基础上实现交互+状态组件：Select / Tabs / Pagination / Menu / Tag / Progress / Badge / Alert。所有 UI 变化沿用动画引擎。

## 范围

| 组件 | 行为 | 动画 |
|------|------|------|
| Select | 单选+多选+filterable 搜索+可清空+禁用+分组选项 | 箭头旋转 180°、下拉淡入+下滑、选项 hover 渐变色、多选 tag 出现/移除 |
| Tabs | 默认下划线样式，点击切换面板 | 指示条在标签间滑动（x/width 插值）、内容淡入 |
| Pagination | 页码+上下页+跳转输入+总数 | hover 过渡、当前页指示条滑动 |
| Menu | 水平导航 + 子菜单下拉 | 激活项下划线滑动、子菜单淡入 |
| Tag | 类型色、可关闭 | 关闭时收缩+淡出 |
| Progress | 线型进度条（可带文字%） | 值变化填充动画 |
| Badge | 红点/数字角标 | 数字变化缩放弹出 |
| Alert | 4 类型、标题/描述、可关闭 | 出现淡入、关闭高度收缩 |

## 技术约束

- JDK 8，`javac --release 8`（回退 `-source 8 -target 8`），纯 javac 构建，零外部依赖
- 动画：Phase 1 的 `Animator` / `Easing` / `ElementTheme`
- 弹层：**JPopupMenu 自绘**（原生定位/外部点击关闭/焦点管理），内容面板半透明，淡入+下滑动画

## 目录结构

```
src/org/swelement/
  core/
    + AnimatedPopup.java   弹层容器（JPopupMenu 子类，淡入+下滑）
  ui/
    + Select.java  Tabs.java  Pagination.java  Menu.java
    + Tag.java  Progress.java  Badge.java  Alert.java
  demo/
    + SelectDemo.java  TabsDemo.java  PaginationDemo.java  MenuDemo.java
    + TagDemo.java  ProgressDemo.java  BadgeDemo.java  AlertDemo.java
```

## 核心新增：AnimatedPopup

- 继承 `JPopupMenu`：`setOpaque(false)`，自绘圆角白底（Element 下拉样式：白底、`#E4E7ED` 边框、4px 圆角、阴影）
- `show(Component invoker)` 时用 Animator（200ms easeOut）驱动 `alpha 0→1` + `offsetY 8→0`，内容面板按 alpha 绘制、按 offset 偏移
- 关闭时反向动画（可选直接关闭，Phase 2 先做直接关闭 + 打开动画）

## 组件规格

### Select（最复杂）
- 继承 JPanel，布局：触发框（自绘，样式同 Input）+ 箭头指示（旋转动画）+ 下拉 AnimatedPopup
- 触发框：filterable 时内嵌 JTextField（可输入过滤），否则只读展示
- 选项模型：内部 `SelectOption {label, value, group, disabled}`；分组渲染灰标签行
- 单选：点击选项选中、高亮当前；多选：选项前勾选框 + 顶部 tags（可逐个关闭）
- filterable：输入时按 label 包含过滤，无匹配显示"无匹配数据"
- 可清空：有值时显示 ×（复用 Input 的 × 思路）
- 逻辑自检：过滤匹配、分组排序、选中/清除状态机

### Tabs
- 继承 JComponent；`addTab(String, JComponent)`；内部标签头自绘
- 指示条：2px 高、PRIMARY 色，位置 x/宽度按 Animator 插值滑动
- 激活标签文字 PRIMARY、其余 `#303133`，hover 过渡
- 内容面板：CardLayout 切换 + 切换时内容淡入（alpha 动画）

### Pagination
- 继承 JComponent；`setTotal(int)`、`setPageSize(int)`、`getCurrentPage()`、`setCurrentPage(int)`（钳制 1..pageCount）
- 布局：上/下页按钮 + 页码（当前页前后各 2 个 + 省略号）+ 跳转输入 + "共 N 条"
- 页码钳制逻辑、省略号计算逻辑加 assert 自检
- 动画：当前页指示（浅色底）滑动过渡、hover 过渡

### Menu
- 继承 JComponent；水平排列菜单项；`addMenuItem(String, Runnable)` 普通项，`addSubMenu(String, MenuItem...)` 子菜单
- 激活项下划线滑动（复用 Tabs 指示条思路）；子菜单项 hover 背景过渡
- 子菜单用 AnimatedPopup 弹出

### Tag
- 继承 JComponent；类型色（primary/success/warning/danger/info）+ 边框浅色 + 圆角 4px；`setClosable(boolean)`
- 关闭：Animator 驱动宽度收缩+alpha 归零后触发 `onClosed` 回调（父容器移除）

### Progress
- 继承 JComponent；`setValue(int)` 0-100 钳制；填充宽度按 Animator 动画（easeOut）
- 样式：6px 高圆角轨道 `#EBEEF5`，填充按类型色（默认 PRIMARY）；`setShowText(true)` 右侧显示百分比

### Badge
- 继承 JComponent；`setCount(int)`（0 隐藏）；内容区放子组件（setLayout 持有）
- 数字变化：缩放弹出动画（scale 0.6→1）；仅红点模式 `setDot(true)`

### Alert
- 继承 JComponent；类型色（success/warning/info/error）左边框 4px + 淡背景 + 图标字符（✓/!/i/×）+ 标题/描述 + 关闭按钮
- 出现：淡入+下滑；关闭：高度收缩至 0 + 透明度归零，完成后回调

## 验证

- 8 个组件各配 `*Demo`（main 起 JFrame），逐个目视验收
- 非平凡逻辑 assert 自检 main：
  - `Select`：过滤匹配、选中/清除状态
  - `Pagination`：页码钳制、省略号窗口计算
- 全量回归：`build.bat` + 3 个 core 自检 + 全部 14 个 Demo 可启动（Phase 1 的 6 个不回归损坏）

## 明确省略（后续阶段补充）

- Select 远程搜索、级联、Tag 的 max-length 等扩展配置
- Table / Tree / DatePicker / Dialog / Notification / Tooltip / Rate 等到 Phase 2 后续或 Phase 3
- 弹层整体缩放动画（JPopupMenu 限制，用淡入+下滑替代）