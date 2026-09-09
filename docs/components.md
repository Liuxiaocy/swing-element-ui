# Swing Element UI 组件库技术文档

## 目录

1. [项目概览](#1-项目概览)
2. [目录结构](#2-目录结构)
3. [模块依赖关系](#3-模块依赖关系)
4. [核心模块（core）](#4-核心模块core)
5. [框架模块（framework）](#5-框架模块framework)
6. [主题系统](#6-主题系统)
7. [UI 组件详解](#7-ui-组件详解)
8. [构建与运行](#8-构建与运行)

---

## 1. 项目概览

**Swing Element UI** 是一个将 Vue.js 的 Element UI 设计语言移植到 Java Swing 的桌面端组件库。采用纯矢量自绘方式实现所有组件的外观，不依赖任何外部库（除 JDK 自带的 `FlatLaf` 作为可选的 Swing 增强），支持运行时主题切换、动画过渡、WCAG 2.1 AA 级对比度合规。

**技术栈：** Java 8+ / Swing / FlatLaf（可选）

**入口类：** `org.swelement.demo.AstThemeDemo`（`src/org/swelement/demo/AstThemeDemo.java`）

---

## 2. 目录结构

```
swing-element-ui/
├── src/
│   └── org/swelement/
│       ├── core/                         # 基础设施层
│       │   ├── AnimatedPopup.java        # 弹出层管理（HoverPopover/Toast 共用）
│       │   ├── AnimationManager.java     # 命名动画状态管理
│       │   ├── Animator.java             # 底层时间驱动动画引擎
│       │   ├── Easing.java               # 缓动函数（linear/easeIn/easeOut/easeInOut）
│       │   ├── ElementTheme.java         # 颜色工具类 + WCAG 对比度断言
│       │   ├── GlassPane.java            # 全屏 GlassPane 遮罩
│       │   ├── PopupPositioner.java      # Popup 定位器
│       │   ├── SelfCheckBase.java        # 自检基类
│       │   ├── StickyToggleModel.java    # Sticky 切换模型
│       │   └── theme/
│       │       ├── Theme.java            # 主题接口（语义色/文字色/边框/圆角/字体）
│       │       ├── ThemeManager.java     # 主题注册/切换/监听
│       │       └── ElementLightTheme.java# 默认亮色主题
│       ├── framework/                    # 组件框架层
│       │   ├── AstAbstractComponent.java # 所有组件的顶层基类
│       │   ├── AstDisplayComponent.java  # 纯展示组件基类
│       │   ├── AstInteractiveComponent.java # 交互组件基类（hover/active/focus/selected）
│       │   ├── AstContainerComponent.java   # 容器组件基类
│       │   └── util/                     # 框架工具类
│       ├── ui/                           # UI 组件层（30+ 组件）
│       │   ├── AstButton.java            # 按钮
│       │   ├── AstInput.java             # 输入框
│       │   ├── AstSelect.java            # 下拉选择器
│       │   ├── AstCheckbox.java          # 复选框
│       │   ├── AstRadio.java             # 单选按钮
│       │   ├── AstSwitch.java            # 开关
│       │   ├── AstSlider.java            # 滑块
│       │   ├── AstRate.java              # 评分
│       │   ├── AstProgress.java          # 进度条
│       │   ├── AstTabs.java              # 标签页
│       │   ├── AstTag.java               # 标签
│       │   ├── AstTree.java              # 树形控件
│       │   ├── AstTable.java             # 表格
│       │   ├── AstForm.java              # 表单
│       │   ├── AstDialog.java            # 对话框
│       │   ├── AstDrawer.java            # 抽屉
│       │   ├── AstPopover.java           # 气泡卡片
│       │   ├── AstTooltip.java           # 提示
│       │   ├── AstMessage.java           # 全局消息 Toast
│       │   ├── AstNotification.java      # 通知
│       │   ├── AstMessageBox.java        # 消息盒子
│       │   ├── AstLoading.java           # 加载
│       │   ├── AstAlert.java             # 警告提示
│       │   ├── AstBreadcrumb.java        # 面包屑
│       │   ├── AstDropdown.java          # 下拉菜单
│       │   ├── AstMenu.java              # 导航菜单
│       │   ├── AstPagination.java        # 分页
│       │   ├── AstCarousel.java          # 走马灯
│       │   ├── AstCollapse.java          # 折叠面板
│       │   ├── AstCascader.java          # 级联选择器
│       │   ├── AstTimePicker.java        # 时间选择器
│       │   ├── AstCalendar.java          # 日历
│       │   ├── AstTimeline.java          # 时间线
│       │   ├── AstTransfer.java          # 穿梭框
│       │   ├── AstCard.java              # 卡片
│       │   ├── AstAvatar.java            # 头像
│       │   ├── AstBadge.java             # 角标
│       │   ├── AstDivider.java           # 分割线
│       │   ├── AstIcon.java              # 图标（54 种矢量图标）
│       │   ├── AstTextArea.java          # 多行文本框
│       │   ├── AstCloseButton.java       # 公共关闭按钮
│       │   ├── AstColorFactory.java      # 颜色工厂
│       │   └── ...                       # 更多组件
│       └── demo/
│           └── AstThemeDemo.java         # 演示入口
├── docs/
│   └── component-design-guidelines.md    # 组件设计规范
└── lib/
    └── flatlaf-3.6.jar                   # FlatLaf 依赖（可选）
```

---

## 3. 模块依赖关系

```
core ← framework ← ui ← demo
 ↑
 theme（core/theme/）
```

- **core**：不依赖任何上层，提供动画、弹出层、主题管理、WCAG 断言等基础设施
- **framework**：依赖 core，提供三种组件基类（Abstract / Display / Interactive / Container）
- **ui**：依赖 framework，实现所有具体 UI 组件
- **demo**：依赖 ui，提供演示界面

---

## 4. 核心模块（core）

### 4.1 Theme（主题接口）

`org.swelement.core.theme.Theme` — 主题的抽象接口，定义了全部语义色、文字色、边框色、填充色、圆角半径和字体。

```java
public interface Theme {
    String getName();
    // 语义色
    Color getPrimary();    // 默认 #409EFF（蓝）
    Color getSuccess();    // 默认 #67C23A（绿）
    Color getWarning();    // 默认 #E6A23C（黄）
    Color getDanger();     // 默认 #F56C6C（红）
    Color getInfo();       // 默认 #909399（灰）
    // 文字色
    Color getTextPrimary();      // #303133
    Color getTextRegular();      // #606266
    Color getTextSecondary();    // #909399
    Color getTextPlaceholder();  // #C0C4CC
    Color getTextDisabled();     // #C0C4CC
    // 边框色
    Color getBorderBase();   // #DCDFE6
    Color getBorderLight();  // #E4E7ED
    Color getBorderLighter();// #EBEEF5
    // 填充色
    Color getFillBlank();   // #FFFFFF
    Color getFillBase();    // #F5F7FA
    Color getFillLight();   // #F5F7FA
    // 圆角
    int getRadiusSmall();   // 2
    int getRadiusBase();    // 4
    int getRadiusLarge();   // 8
    // 字体
    Font getFontSmall();
    Font getFontBase();     // 14px SansSerif
    Font getFontLarge();
}
```

### 4.2 ThemeManager（主题管理器）

`org.swelement.core.theme.ThemeManager` — 全局单例，负责主题的注册、切换和变更通知。

```java
// 注册主题（第一个注册的自动成为当前主题）
ThemeManager.registerTheme(new ElementLightTheme());

// 获取当前主题
Theme t = ThemeManager.getCurrent();

// 切换主题（触发所有监听器更新）
ThemeManager.setCurrent("dark");

// 监听主题变更
ThemeManager.addThemeChangeListener((oldTheme, newTheme) -> {
    // 所有组件自动重绘
});

// 获取已注册主题列表
List<String> names = ThemeManager.getAvailableThemes();
```

### 4.3 ElementTheme（颜色工具类）

`org.swelement.core.ElementTheme` — 便捷颜色 getter（委托 ThemeManager）+ 颜色算法工具。

```java
// 便捷 getter（推荐直接用 ThemeManager.getCurrent()）
Color primary = ElementTheme.primary();
Color text = ElementTheme.textRegular();

// 颜色插值
Color mid = ElementTheme.lerp(Color.RED, Color.BLUE, 0.5f);

// 亮度计算
float lum = ElementTheme.luminance(someColor);

// WCAG 对比度断言（运行时检查，仅 assert 开启时生效）
ElementTheme.assertContrast(fg, bgColor, "组件名+状态");

// 选择文字颜色（基于背景亮度自动选黑/白）
Color textOnBg = ElementTheme.pickTextColorForBg(bgColor);
```

### 4.4 AnimationManager（动画管理器）

`org.swelement.core.AnimationManager` — 每个组件持有一个实例，管理命名动画。

```java
// 注册动画（名称 + 持续时间ms + 缓动函数）
anim.register("hover", 200, Easing::easeInOut);
anim.register("fill", 300, Easing::easeOut);

// 启动动画（从当前值到目标值）
anim.go("hover", 0f, 1f);   // 从 0 → 1
anim.go("fill", anim.getProgress("fill"), 0f); // 当前值 → 0

// 读取当前进度（0.0 ~ 1.0）
float p = anim.getProgress("hover");

// 直接设置进度
anim.setProgress("fill", 1f);

// 停止动画
anim.stop("hover");
```

### 4.5 Animator（底层动画引擎）

`org.swelement.core.Animator` — 基于 Swing Timer 的时间驱动动画，支持重复/非重复/脉冲模式。

```java
Animator a = new Animator(parentComponent);
a.setDuration(200);
a.setInterpolator(Easing::easeInOut);
a.addTarget(new Animator.Target() {
    public void update(float fraction) { /* fraction: 0→1 */ }
});
a.start();
```

### 4.6 Easing（缓动函数）

`org.swelement.core.Easing` — 静态函数集合。

```java
Easing::linear       // 线性
Easing::easeIn       // 慢→快
Easing::easeOut      // 快→慢
Easing::easeInOut    // 慢→快→慢
Easing::bounceOut    // 弹跳
Easing::elasticOut   // 弹性
```

### 4.7 AnimatedPopup（弹出层）

`org.swelement.core.AnimatedPopup` — 管理弹出窗口（ToolTip / Toast / Popover 等共用），支持全局单例复用避免频繁创建 HeavyWeight Window。

```java
AnimatedPopup popup = new AnimatedPopup();
popup.getContent().add(someComponent);
popup.setPreferredSize(new Dimension(200, 100));

// 全局注册（自动管理生命周期）
AnimatedPopup.registerGlobal(popup, AnimatedPopup.PopupLayer.POPUP);

// 定位
PopupPositioner.showAbove(popup, targetComponent);
PopupPositioner.showBelow(popup, targetComponent);
PopupPositioner.showAt(popup, x, y);

// 关闭监听
popup.setDismissListener(() -> { /* on close */ });
```

---

## 5. 框架模块（framework）

### 5.1 组件继承体系

```
JComponent
  └── AstAbstractComponent           ← 所有组件的根
        ├── AstDisplayComponent      ← 纯展示组件（Badge, Tag, Progress, Icon...）
        ├── AstInteractiveComponent  ← 交互组件（Button, Checkbox, Radio, Slider...）
        │                              自动管理 hover/active/focus/selected 状态动画
        └── AstContainerComponent    ← 容器组件（Form, Table, Container, Alert...）
```

### 5.2 AstAbstractComponent

所有组件的顶层基类，提供：

- `AnimationManager anim` — 命名动画管理
- `theme()` — 获取当前主题
- `onThemeUpdated(old, new)` — 主题变更回调（子类重写）
- `createGraphics(g)` — 创建抗锯齿 Graphics2D
- `lerp(a, b, t)` — 颜色/数值插值
- `drawRoundRect(g2, x, y, w, h, r)` — 绘制圆角矩形
- `drawCenteredText(g2, text, w, h)` — 居中文本绘制
- `assertContrast(fg, bg, where)` — WCAG 对比度断言
- `selfCheck()` — 自检方法（子类必须实现）

### 5.3 AstInteractiveComponent

交互组件基类，自动管理：

| 状态 | 动画名 | 说明 |
|------|--------|------|
| hover | `hover` | 鼠标悬停（200ms easeInOut） |
| active | `active` | 鼠标按下（120ms easeInOut） |
| focus | `focus` | 键盘聚焦（200ms easeInOut） |
| selected | `selected` | 选中状态切换（200ms easeInOut） |

```java
// 读取状态进度
float h = hoverProgress();  // 0.0 ~ 1.0
float a = activeProgress();
float f = focusProgress();

// 键盘支持
// Space / Enter 触发激活
// Tab 切换焦点
```

### 5.4 AstDisplayComponent

纯展示组件基类，默认光标为箭头，不注册交互动画。

### 5.5 AstContainerComponent

容器组件基类，支持子组件管理。

---

## 6. 主题系统

### 6.1 默认主题

`ElementLightTheme` — Element UI 标准亮色主题，所有颜色值与 Vue Element UI 一致。

### 6.2 运行时切换

```java
// 所有组件自动响应主题变更（通过 ThemeChangeListener）
ThemeManager.setCurrent("dark"); // 切换到暗色主题
```

### 6.3 对比度保证

每个组件的 `selfCheck()` 方法中包含 WCAG 2.1 AA 级对比度断言（≥4.5:1），确保所有状态（默认、hover、disabled、选中等）的文字与背景对比度合规。

---

## 7. UI 组件详解

### 7.1 AstButton — 按钮

**类型：** `AstInteractiveComponent`

Element UI Button 的 Swing 实现。支持 5 种类型、3 种尺寸、plain 模式、disabled 状态、圆形按钮。

```java
// 基本用法
AstButton btn = new AstButton("确定");

// 类型按钮
AstButton primary = new AstButton("主要", AstButton.Type.PRIMARY);
AstButton success = new AstButton("成功", AstButton.Type.SUCCESS);
AstButton warning = new AstButton("警告", AstButton.Type.WARNING);
AstButton danger  = new AstButton("危险", AstButton.Type.DANGER);
AstButton info    = new AstButton("信息", AstButton.Type.INFO);

// plain 模式（浅色背景）
AstButton plain = new AstButton("主要按钮", AstButton.Type.PRIMARY, AstButton.PLAIN);

// 尺寸
AstButton large  = new AstButton("大型", AstButton.SIZE_LARGE);
AstButton small  = new AstButton("小型", AstButton.SIZE_SMALL);

// 圆形按钮
AstButton round = new AstButton("圆角", AstButton.Type.PRIMARY, AstButton.ROUND);

// 禁用
btn.setEnabled(false);

// 点击监听
btn.addActionListener(e -> System.out.println("clicked"));
```

**属性：**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| text | String | "" | 按钮文字 |
| type | Type | DEFAULT | PRIMARY/SUCCESS/WARNING/DANGER/INFO/DEFAULT |
| size | int | SIZE_DEFAULT | SIZE_LARGE / SIZE_DEFAULT / SIZE_SMALL |
| plain | boolean | false | 是否为 plain 模式 |
| round | boolean | false | 是否为圆形 |

---

### 7.2 AstInput — 输入框

**类型：** `AstInteractiveComponent`

Element UI Input 的 Swing 实现，支持普通输入、密码模式、可清空。

```java
// 普通输入框
AstInput input = new AstInput("请输入内容");
input.setColumns(20);

// 密码输入框
AstInput password = new AstInput("请输入密码");
password.setEchoChar('*');

// 可清空
AstInput clearable = new AstInput("可清空");
clearable.setClearable(true);

// 尺寸
AstInput large = new AstInput("大号", AstInput.SIZE_LARGE);
AstInput small = new AstInput("小号", AstInput.SIZE_SMALL);

// 获取/设置值
String val = input.getValue();
input.setText("默认值");

// 焦点/失焦监听
input.addFocusListener(new FocusAdapter() {
    public void focusGained(FocusEvent e) { /* 获焦 */ }
    public void focusLost(FocusEvent e) { /* 失焦 */ }
});

// 禁用
input.setEnabled(false);

// 占位符
input = new AstInput("请输入用户名");
```

**属性：**

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| placeholder | String | "" | 占位文字 |
| clearable | boolean | false | 是否可清空 |
| size | int | SIZE_DEFAULT | SIZE_LARGE / SIZE_DEFAULT / SIZE_SMALL |

---

### 7.3 AstTextArea — 多行文本框

**类型：** `AstAbstractComponent`

多行文本输入，透明 JScrollPane 包 JTextArea，复用 AstInput 的边框/聚焦光晕/占位符。

```java
AstTextArea area = new AstTextArea("请输入详细描述", 5, 30);
area.setPreferredSize(new Dimension(300, 120));

// 获取内容
String text = area.getText();

// 设置内容
area.setText("默认内容");
```

---

### 7.4 AstSelect — 下拉选择器

**类型：** `AstAbstractComponent` + `FormValueProvider` + `FormInvalidMarker`

Element UI Select 的 Swing 实现，支持单选/多选、可搜索、可清空、分组。

```java
// 单选
AstSelect select = new AstSelect(false, false);
select.addOption(new AstSelect.Option("选项一", "opt1"));
select.addOption(new AstSelect.Option("选项二", "opt2"));
select.setSelectionListener(sel -> {
    if (!sel.isEmpty()) System.out.println("选中: " + sel.get(0).label);
});

// 多选
AstSelect multi = new AstSelect(true, false);
multi.addOption(new AstSelect.Option("北京", "bj"));
multi.addOption(new AstSelect.Option("上海", "sh"));
multi.setSelectionListener(sel -> System.out.println("已选 " + sel.size() + " 项"));

// 可搜索
AstSelect filterable = new AstSelect(false, true);
filterable.setPlaceholder("搜索选项...");

// 尺寸
AstSelect large = new AstSelect(false, false);
large.setSize(AstSelect.SIZE_LARGE);

// 设置选项
select.setOptions(Arrays.asList(
    new AstSelect.Option("北京", "bj"),
    new AstSelect.Option("上海", "sh", "城市", false)
));

// 清空选择
select.clearSelection();
```

**Option 类：**

```java
public static class Option {
    public final String label;      // 显示文字
    public final Object value;      // 选项值
    public final String group;      // 分组名（可选）
    public final boolean disabled;  // 是否禁用
}
```

---

### 7.5 AstCheckbox — 复选框

**类型：** `AstInteractiveComponent`

Element UI Checkbox 的 Swing 实现，带动画的勾选效果。

```java
AstCheckbox cb = new AstCheckbox("同意协议");
cb.setSelected(true);

// 监听选中变化
cb.addItemListener(e -> {
    System.out.println("选中: " + cb.isSelected());
});

// 禁用
cb.setEnabled(false);
```

---

### 7.6 AstRadio — 单选按钮

**类型：** `AstInteractiveComponent`

Element UI Radio 的 Swing 实现，支持分组。

```java
// 单个
AstRadio r1 = new AstRadio("选项一");
AstRadio r2 = new AstRadio("选项二");

// 分组（同一时间只有一个被选中）
AstRadio.Group group = new AstRadio.Group();
group.add(r1);
group.add(r2);

// 监听选中变化
r1.addItemListener(e -> System.out.println("r1 选中: " + r1.isSelected()));
```

---

### 7.7 AstSwitch — 开关

**类型：** `AstInteractiveComponent`

Element UI Switch 的 Swing 实现，滑块动画切换。

```java
AstSwitch sw = new AstSwitch();
sw.setSelected(true);

sw.addItemListener(e -> System.out.println("开关: " + (sw.isSelected() ? "开" : "关")));

// 尺寸固定 44x22
```

---

### 7.8 AstSlider — 滑块

**类型：** `AstInteractiveComponent`

Element UI Slider 的 Swing 实现，支持拖动和键盘操作。

```java
AstSlider slider = new AstSlider(0, 100, 50);
slider.addChangeListener(e -> System.out.println("值: " + slider.getValue()));

// 设置范围
slider.setMin(0);
slider.setMax(200);
slider.setValue(100);
```

---

### 7.9 AstRate — 评分

**类型：** `AstInteractiveComponent`

Element UI Rate 的 Swing 实现，支持半星、hover 预览、只读模式。

```java
AstRate rate = new AstRate(5, true); // 5星，允许半星
rate.setValue(3.5f);
rate.setValueListener(v -> System.out.println("评分: " + v));

// 只读
rate.setReadOnly(true);

// 自定义星星大小
rate.setStarSize(32);
```

---

### 7.10 AstProgress — 进度条

**类型：** `AstDisplayComponent`

Element UI Progress 的 Swing 实现，水平进度条带百分比文字。

```java
AstProgress bar = new AstProgress(60);
bar.setValue(75);
bar.setShowText(true);

// 监听变化
bar.addChangeListener(e -> System.out.println("进度: " + bar.getValue() + "%"));
```

---

### 7.11 AstTabs — 标签页

**类型：** `AstAbstractComponent`

Element UI Tabs 的 Swing 实现，CardLayout 切换，带滑动指示条动画。

```java
AstTabs tabs = new AstTabs();
tabs.addTab("用户管理", createUserPanel());
tabs.addTab("系统设置", createSettingsPanel());
tabs.setSelectedIndex(0);

// 监听切换
tabs.addChangeListener(e -> {
    System.out.println("切换到: " + tabs.getSelectedIndex());
});
```

---

### 7.12 AstTag — 标签

**类型：** `AstInteractiveComponent`

Element UI Tag 的 Swing 实现，支持多种类型、plain 模式、可关闭。

```java
AstTag tag = new AstTag("标签一");
tag.setType(AstTag.Type.SUCCESS);

// plain 模式
AstTag plain = new AstTag("标签", AstTag.Type.WARNING, AstTag.EFFECT_PLAIN);

// 可关闭
AstTag closable = new AstTag("可关闭", AstTag.Type.DANGER);
closable.setClosable(true);
closable.setOnClose(() -> System.out.println("关闭"));

// 尺寸
AstTag large = new AstTag("大标签", AstTag.SIZE_DEFAULT);
```

**类型枚举：** `PRIMARY / SUCCESS / WARNING / DANGER / INFO`

---

### 7.13 AstTree — 树形控件

**类型：** `AstInteractiveComponent`

Element UI Tree 的 Java 实现，支持折叠/展开动画、节点选中、连接线。

```java
AstTree.TreeNode root = new AstTree.TreeNode("系统管理");
AstTree.TreeNode users = new AstTree.TreeNode("用户", true);
users.addChild(new AstTree.TreeNode("管理员"));
users.addChild(new AstTree.TreeNode("普通用户"));
root.addChild(users);
root.addChild(new AstTree.TreeNode("角色管理"));

AstTree tree = new AstTree(root);
tree.setNodeClickListener(node -> System.out.println("点击: " + node.label));

// 设置根节点
tree.setRoot(root);
```

**TreeNode 模型：**

```java
public static final class TreeNode {
    public String label;
    public Object userObject;  // 自由数据
    void addChild(TreeNode c);
    List<TreeNode> getChildren();
    boolean hasChildren();
    void setExpanded(boolean e);
    void setSelected(boolean s);
}
```

---

### 7.14 AstTable — 表格

**类型：** `AstContainerComponent`

Element UI Table 的 Java 实现，支持列对齐、斑马纹、hover 高亮、行选中。

```java
AstTable.Column[] cols = {
    new AstTable.Column("姓名", 120),
    new AstTable.Column("年龄", 80, AstTable.Align.CENTER),
    new AstTable.Column("地址", 200),
};
AstTable table = new AstTable(cols);
table.addRow("张三", 28, "北京市朝阳区");
table.addRow("李四", 32, "上海市浦东新区");

// 行点击监听
table.setRowClickListener(row -> System.out.println("点击行: " + row));

// 启用斑马纹
table.setStripe(true);

// 启用 hover 高亮
table.setHighlightOnHover(true);

// 尺寸
table.setSize(AstTable.SIZE_DEFAULT);
```

---

### 7.15 AstForm — 表单

**类型：** `AstContainerComponent`

表单组件，支持字段校验规则和错误提示动画。

```java
AstForm form = new AstForm();

// 添加字段（标签 + 组件 + 校验规则）
JTextField nameField = new JTextField(20);
form.addField("姓名", nameField, new AstForm.RequiredRule());

JTextField emailField = new JTextField(20);
form.addField("邮箱", emailField, new AstForm.RequiredRule(), new AstForm.EmailRule());

// 校验
if (form.validateForm()) {
    System.out.println("校验通过");
}

// 标签位置
form.setLabelPosition(AstForm.POS_LEFT);  // LEFT / TOP / RIGHT

// 标签宽度
form.setLabelWidth(120);
```

**内置校验规则：**

| 规则 | 说明 |
|------|------|
| `RequiredRule` | 必填 |
| `MinLengthRule(min)` | 最小长度 |
| `MaxLengthRule(max)` | 最大长度 |
| `RegexRule(regex, msg)` | 正则匹配 |
| `EmailRule` | 邮箱格式 |

---

### 7.16 AstDialog — 对话框

**类型：** 静态方法调用

Element UI Dialog 的 Swing 实现，阻塞式对话框。

```java
// 简单 alert
AstDialog.show(frame, "提示", "确定", "", bodyPanel, callback);

// confirm
AstDialog.show(frame, "确认", "确定", "取消", bodyPanel, new AstDialog.ResultCallback() {
    public void onResult(int resultCode) {
        if (resultCode == AstDialog.RESULT_OK) {
            // 用户点击确定
        } else {
            // 用户点击取消
        }
    }
});
```

---

### 7.17 AstDrawer — 抽屉

**类型：** 静态方法调用

Element UI Drawer 的 Swing 实现，从屏幕边缘滑入。

```java
AstDrawer.show(frame, "标题", contentPanel, AstDrawer.Position.RIGHT);
```

---

### 7.18 AstPopover — 气泡卡片

**类型：** `AstAbstractComponent`

Element UI Popover 的 Java 实现，点击/hover 触发弹出带标题的卡片浮层。

```java
// 基本用法
AstPopover pop = new AstPopover("标题", bodyPanel, AnimatedPopup.Direction.BELOW);
pop.setTriggerText("点我");
frame.add(pop);

// 包装已有组件
AstPopover pop2 = AstPopover.wrap(myButton, "提示", infoPanel, AnimatedPopup.Direction.RIGHT);

// 触发方式
pop.setTrigger(AstPopover.Trigger.CLICK);  // CLICK / HOVER

// 方向
pop.setDirection(AnimatedPopup.Direction.BELOW);

// 手动控制
pop.showPopover();
pop.hidePopover();
```

---

### 7.19 AstTooltip — 提示

**类型：** 静态方法调用

Element UI Tooltip 的 Java 实现，hover 显示提示文字，共享全局 AnimatedPopup。

```java
// 基本用法
AstTooltip.attach(btn, "点此保存");

// 自定义方向和主题
AstTooltip.attach(btn, "编辑内容", AnimatedPopup.Direction.RIGHT, AstTooltip.Effect.LIGHT);

// 移除
AstTooltip.detach(btn);
```

**Effect 枚举：** `DARK`（深色背景白字）/ `LIGHT`（白底深字）

---

### 7.20 AstMessage — 全局消息 Toast

**类型：** 静态方法调用

不阻断后台操作的轻量 Toast，顶部居中，3s 自动关闭，支持堆叠。

```java
AstMessage.show(frame, AstMessage.MessageType.SUCCESS, "保存成功");
AstMessage.show(frame, AstMessage.MessageType.INFO, "自定义时长", 800);
AstMessage.show(frame, AstMessage.MessageType.WARNING, "注意");
AstMessage.show(frame, AstMessage.MessageType.ERROR, "操作失败");
```

**MessageType 枚举：** `INFO / SUCCESS / WARNING / ERROR`

---

### 7.21 AstNotification — 通知

**类型：** 静态方法调用

出现在页面四角的轻量通知，支持标题+描述，4.5s 自动关闭，多条堆叠。

```java
AstNotification.show(frame,
    AstNotification.NotificationType.SUCCESS,
    "操作成功",
    "数据已保存"
);

// 自定义时长和位置
AstNotification.show(frame,
    AstNotification.NotificationType.WARNING,
    "警告",
    "请注意",
    6000,
    AstNotification.Position.BOTTOM_RIGHT,
    null  // Runnable onClosed
);
```

**NotificationType 枚举：** `INFO / SUCCESS / WARNING / ERROR`

**Position 枚举：** `TOP_RIGHT / TOP_LEFT / BOTTOM_RIGHT / BOTTOM_LEFT`

---

### 7.22 AstMessageBox — 消息盒子

**类型：** 静态方法调用

简化版对话框，5 种类型图标，alert/confirm 两种风格。

```java
// alert（单个确定按钮）
AstMessageBox.alert(frame, AstMessageBox.MessageBoxType.ERROR, "出错了");

// confirm（确定 + 取消）
AstMessageBox.confirm(frame, AstMessageBox.MessageBoxType.QUESTION, "确定删除？",
    new AstMessageBox.ConfirmCallback() {
        public void onConfirm() { /* 确定 */ }
        public void onCancel() { /* 取消 */ }
    }
);
```

**MessageBoxType 枚举：** `INFO / SUCCESS / WARNING / ERROR / QUESTION`

---

### 7.23 AstLoading — 加载

**类型：** `AstAbstractComponent`

Element UI Loading 的 Swing 实现，支持包裹模式和全屏模式，12 段弧线旋转指示器。

```java
// 包裹模式
AstLoading loader = new AstLoading(AstLoading.Mode.WRAP, myTargetComp);
loader.showLoading("数据加载中…");
// later
loader.hideLoading();

// 全屏模式
AstLoading loader = new AstLoading(AstLoading.Mode.FULLSCREEN, null);
myFrame.setGlassPane(loader);
loader.showLoading("正在提交请求…");
loader.hideLoading();

// 自定义遮罩颜色
loader.setBgColor(new Color(255, 255, 255, 160));

// 自定义 spinner 大小
loader.setSpinnerSize(80);

// 延迟显示（避免闪烁）
loader.setShowDelay(300); // 300ms 后才显示
```

---

### 7.24 AstAlert — 警告提示

**类型：** `AstContainerComponent`

Element UI Alert 的 Swing 实现，支持 4 种类型、可关闭。

```java
AstAlert alert = new AstAlert(AstAlert.SUCCESS, "操作成功", "数据已保存", true);
alert.setOnClosed(() -> System.out.println("已关闭"));

// 无描述
AstAlert simple = new AstAlert(AstAlert.WARNING, "请注意", null, false);
```

**类型常量：** `SUCCESS / WARNING / INFO / ERROR`

---

### 7.25 AstBreadcrumb — 面包屑

**类型：** `AstAbstractComponent`

Element UI Breadcrumb 的 Java 实现，路径式导航，分隔符可配。

```java
AstBreadcrumb bc = new AstBreadcrumb(Arrays.asList("首页", "用户管理", "详情"));
bc.setSeparator("/");
bc.setItemClickListener(idx -> System.out.println("点击第 " + idx + " 段"));
```

---

### 7.26 AstDropdown — 下拉菜单

**类型：** `AstAbstractComponent`

Element UI Dropdown 的 Java 实现。

```java
// 用法参见源码
```

---

### 7.27 AstMenu — 导航菜单

**类型：** `AstInteractiveComponent`

Element UI Menu 的 Java 实现，支持子菜单。

```java
AstMenu menu = new AstMenu();
menu.addItem("首页", () -> System.out.println("首页"));
menu.addItem("用户管理", () -> System.out.println("用户管理"),
    new String[]{"管理员", "普通用户"},
    new Runnable[]{() -> System.out.println("管理员"), () -> System.out.println("普通用户")}
);
menu.setActiveIndex(0);
```

---

### 7.28 AstPagination — 分页

**类型：** `AstAbstractComponent`

Element UI Pagination 的 Java 实现，支持跳页、尺寸档位。

```java
AstPagination p = new AstPagination(200, 10, 1);
p.addPageListener(page -> System.out.println("第 " + page + " 页"));

// 尺寸
p.setSize(AstPagination.SIZE_DEFAULT);
```

---

### 7.29 AstCarousel — 走马灯

**类型：** `AstInteractiveComponent`

Element UI Carousel 的 Java 实现，横向滑动展示多张幻灯片。

```java
List<AstCarousel.SlidePainter> slides = new ArrayList<>();
slides.add((g, w, h) -> {
    g.setColor(ElementTheme.primary());
    g.fillRect(0, 0, w, h);
    g.setColor(Color.WHITE);
    g.drawString("第一张", w/2 - 30, h/2);
});
AstCarousel c = new AstCarousel(slides);
c.setAutoplay(true);
```

---

### 7.30 AstCollapse — 折叠面板

**类型：** `AstAbstractComponent`

Element UI Collapse 的 Java 实现，支持手风琴模式。

```java
AstCollapse c = new AstCollapse(true); // 手风琴
c.addItem("标题一", createContentA());
c.addItem("标题二", createContentB());
c.setChangeListener((idx, open) -> System.out.println(idx + " " + (open ? "open" : "close")));
```

---

### 7.31 AstCascader — 级联选择器

**类型：** `AstContainerComponent` + `FormValueProvider`

Element UI Cascader 的 Java 实现，支持 N 级层级选择。

```java
AstCascader.Option root = new AstCascader.Option("广东");
AstCascader.Option gz = new AstCascader.Option("广州");
gz.addChild(new AstCascader.Option("天河区"));
gz.addChild(new AstCascader.Option("越秀区"));
root.addChild(gz);

List<AstCascader.Option> opts = new ArrayList<>();
opts.add(root);
AstCascader cascader = new AstCascader(opts, 3);
cascader.setPlaceholder("请选择城市");
cascader.setSelectionListener(path -> System.out.println("选中: " + path));
```

---

### 7.32 AstTimePicker — 时间选择器

**类型：** `AstInteractiveComponent`

Element UI TimePicker 的 Swing 实现，三列（时/分/秒）可滚动选择面板。

```java
AstTimePicker tp = new AstTimePicker();
tp.setTime(14, 30, 0);
tp.setTimeChangeListener(hms -> System.out.println(hms[0]+":"+hms[1]+":"+hms[2]));

// 关闭秒列
AstTimePicker tp2 = new AstTimePicker(false);
```

---

### 7.33 AstCalendar — 日历

**类型：** `AstInteractiveComponent`

Element UI Calendar 的 Java 实现，月份网格，可前后翻月、选中日期。

```java
AstCalendar cal = new AstCalendar();
cal.setDateListener(date -> System.out.println(date[0]+"-"+(date[1]+1)+"-"+date[2]));
```

---

### 7.34 AstTimeline — 时间线

**类型：** `AstDisplayComponent`

Element UI Timeline 的 Java 实现，垂直展示带有时间戳的事件序列。

```java
List<AstTimeline.Item> items = new ArrayList<>();
items.add(new AstTimeline.Item("2026-08-01", "项目启动", AstTimeline.Type.PRIMARY));
items.add(new AstTimeline.Item("2026-08-10", "P1 完成", AstTimeline.Type.SUCCESS));
AstTimeline tl = new AstTimeline(items);
```

**Type 枚举：** `PRIMARY / SUCCESS / WARNING / DANGER / INFO`

---

### 7.35 AstTransfer — 穿梭框

**类型：** `AstContainerComponent`

Element UI Transfer 的 Java 实现，双列多选转移组件。

```java
List<AstTransfer.Item> data = new ArrayList<>();
data.add(new AstTransfer.Item("1", "选项一"));
data.add(new AstTransfer.Item("2", "选项二"));
AstTransfer tf = new AstTransfer(data);
tf.setChangeListener(selected -> System.out.println("已选 " + selected.size() + " 项"));

// 预设已选
tf.setSelectedKeys(Arrays.asList("2"));

// 可搜索
tf.setFilterable(true);
```

---

### 7.36 AstCard — 卡片

**类型：** `AstDisplayComponent`

Element UI Card 的 Java 实现，支持标题、边框、阴影。

```java
AstCard card = new AstCard("卡片标题");
card.setContent(contentPanel);

// 阴影模式
card.setShadow(AstCard.Shadow.ALWAYS);   // 始终阴影
card.setShadow(AstCard.Shadow.HOVER);    // hover 时阴影
card.setShadow(AstCard.Shadow.NEVER);    // 无阴影

// 无边框
AstCard borderless = new AstCard("无边框", false, AstCard.Shadow.NEVER);
```

**Shadow 枚举：** `ALWAYS / HOVER / NEVER`

---

### 7.37 AstAvatar — 头像

**类型：** `AstDisplayComponent`

Element UI Avatar 的 Java 实现，支持圆形/方形、文字/图标、角标。

```java
// 文字头像
AstAvatar avatar = new AstAvatar('A', AstAvatar.SIZE_DEFAULT, AstAvatar.CIRCLE);

// 图标头像
AstAvatar icon = new AstAvatar(myIcon, AstAvatar.SIZE_LARGE, AstAvatar.CIRCLE);

// 形状
AstAvatar.SQUARE  // 方形
AstAvatar.CIRCLE  // 圆形

// 尺寸
AstAvatar.SIZE_SMALL  // 32px
AstAvatar.SIZE_DEFAULT // 40px
AstAvatar.SIZE_LARGE  // 64px

// 角标（通过 getBadge() 访问）
avatar.getBadge().setDot(true);
```

---

### 7.38 AstBadge — 角标

**类型：** `AstDisplayComponent`

Element UI Badge 的 Java 实现，支持数字/圆点、多种类型、尺寸档位。

```java
AstBadge badge = new AstBadge();
badge.setContent(someComponent);
badge.setCount(5);

// 圆点模式
badge.setDot(true);

// 类型
badge.setType(AstBadge.Type.DANGER);

// 尺寸
badge.setSizeTier(AstBadge.SIZE_DEFAULT);

// 最大值
badge.setMax(99); // 超过 99 显示 99+

// 隐藏
badge.setHidden(true);
```

---

### 7.39 AstDivider — 分割线

**类型：** `AstDisplayComponent`

Element UI Divider 的 Java 实现，支持水平/垂直、带文字、虚线。

```java
// 水平分割线
AstDivider h = new AstDivider(AstDivider.HORIZONTAL, "标题", AstDivider.ALIGN_LEFT);

// 垂直分割线
AstDivider v = new AstDivider(AstDivider.VERTICAL);

// 虚线
v.setDashed(true);

// 自定义颜色
h.setLineColor(Color.RED);
h.setTextColor(Color.BLUE);
```

**常量：** `HORIZONTAL / VERTICAL / ALIGN_LEFT / ALIGN_CENTER / ALIGN_RIGHT`

---

### 7.40 AstIcon — 图标

**类型：** `AstDisplayComponent`

Element UI Icon 的 Java 实现，54 种矢量图标，纯 Graphics2D 绘制，无图片依赖。

```java
// 组件方式
AstIcon check = new AstIcon(AstIcon.Type.CHECK, ElementTheme.success(), 16);
AstIcon loading = new AstIcon(AstIcon.Type.LOADING, ElementTheme.primary(), 16);
loading.setSpinEnabled(true);

// 零组件复用（任意 JComponent 内直接绘制）
AstIcon.paintIcon(g, AstIcon.Type.CARET_DOWN, ElementTheme.textRegular(), 12, 0f);
```

**图标清单（54 个）：**

`CHECK, CLOSE, ARROW_UP/DOWN/LEFT/RIGHT, PLUS, MINUS, SEARCH, INFO, SUCCESS, WARNING, ERROR, SETTING, USER, EYE, REFRESH, EDIT, DELETE, EYE_OFF, CALENDAR, CLOCK, STAR, STAR_FILLED, BELL, MESSAGE, MORE, MENU, LINK, LOCATION, PHONE, CAMERA, COLLECTION, UPLOAD, DOWNLOAD, LOCK, UNLOCK, SORT, FILTER_FILLED, FULL_SCREEN, COPY, SHARE, PRINT, CIRCLE_CHECK, CIRCLE_CLOSE, CIRCLE_WARNING, CIRCLE_INFO, QUESTION, LOADING, CARET_UP/DOWN/LEFT/RIGHT, DELETE_FILLED`

---

### 7.41 AstCloseButton — 公共关闭按钮

**类型：** `AstInteractiveComponent`

所有可关闭组件（AstTag / AstAlert / AstInput / AstDialog 等）统一使用的矢量 × 关闭按钮。

```java
AstCloseButton close = new AstCloseButton(24);
close.addActionListener(e -> System.out.println("关闭"));

// 设定唯一回调
close.setOnClose(() -> doSomething());

// 禁用态
close.setEnabled(false);
```

---

## 8. 构建与运行

### 8.1 编译

```bash
# 编译所有源码
javac -d out -cp "lib/flatlaf-3.6.jar" $(find src -name "*.java")
```

### 8.2 运行演示

```bash
java -cp "out:lib/flatlaf-3.6.jar" org.swelement.demo.AstThemeDemo
```

### 8.3 运行自检

```bash
# 启用 assert 运行自检（所有组件的 selfCheck() 会验证对比度、尺寸、功能）
java -ea -cp "out:lib/flatlaf-3.6.jar" org.swelement.demo.AstThemeDemo
```

---

## 附录：通用 API 模式

### 获取/设置选中状态

所有交互组件共享：

```java
component.isSelected()     // 获取选中状态
component.setSelected(b)   // 设置选中状态
```

### 获取/设置值

```java
input.getValue()           // AstInput
select.getSelected()       // AstSelect（返回 List<Option>）
slider.getValue()          // AstSlider（返回 int）
rate.getValue()            // AstRate（返回 float）
progress.getValue()        // AstProgress（返回 int）
timePicker.getTime()       // AstTimePicker（返回 int[] {h,m,s}）
```

### 监听器模式

```java
// ActionListener（按钮、关闭等）
component.addActionListener(e -> { ... });

// ChangeListener（滑块、进度条、标签页）
component.addChangeListener(e -> { ... });

// ItemListener（复选框、开关）
component.addItemListener(e -> { ... });

// Consumer<T>（自定义监听）
select.setSelectionListener(sel -> { ... });
rate.setValueListener(v -> { ... });
tree.setNodeClickListener(node -> { ... });
```

### 尺寸档位

大部分组件支持 Element UI 标准三档尺寸：

```java
SIZE_LARGE  = 0  // 大号
SIZE_DEFAULT = 1 // 默认
SIZE_SMALL  = 2  // 小号
```

设置方式：

```java
component.setSize(AstXxx.SIZE_LARGE);
```
