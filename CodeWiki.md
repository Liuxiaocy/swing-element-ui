# Swing-Element-UI Code Wiki

> Element UI 风格的 Java Swing 组件库（JDK 8，零依赖）

---

## 目录

1. [项目概述](#1-项目概述)
2. [整体架构](#2-整体架构)
3. [目录结构](#3-目录结构)
4. [核心模块详解](#4-核心模块详解)
5. [UI 组件详解](#5-ui-组件详解)
6. [Demo 模块](#6-demo-模块)
7. [依赖关系](#7-依赖关系)
8. [构建与运行](#8-构建与运行)
9. [设计模式与架构特点](#9-设计模式与架构特点)
10. [扩展开发指南](#10-扩展开发指南)

---

## 1. 项目概述

### 1.1 项目简介

**swing-element-ui** 是一个在 Java Swing 平台上复刻 Vue.js 生态中知名 UI 框架 **Element UI** 视觉风格与交互体验的组件库。项目采用纯 JDK 8 API 实现，**零外部依赖**，所有组件均通过自定义绘制（Custom Painting）实现，不依赖任何第三方 Look & Feel。

### 1.2 核心特性

| 特性 | 说明 |
|------|------|
| **Element UI 视觉风格** | 完整还原 Element UI 色板（主色 `#409EFF`、成功 `#67C23A` 等）、圆角、间距规范 |
| **自研动画引擎** | 基于 `javax.swing.Timer` 的轻量 Animator，支持多种缓动函数，全部 UI 状态切换均有过渡动画 |
| **零依赖** | 仅使用 JDK 标准库，编译产物可直接运行 |
| **组件自绘** | 所有组件继承 JComponent/JPanel，覆盖 `paintComponent` 实现完全自定义绘制 |
| **内置自检** | 核心类提供 `assert` 自检 main 方法，可通过 `-ea` 参数运行验证 |

### 1.3 技术栈

- **语言**: Java
- **最低版本**: JDK 8（编译参数 `--release 8`）
- **GUI 框架**: Java Swing（`javax.swing.*` + `java.awt.*`）
- **构建工具**: 纯 `javac` 命令（Windows 下通过 `build.bat` 脚本）

### 1.4 阶段规划

项目采用分阶段迭代交付：

| 阶段 | 状态 | 内容 |
|------|------|------|
| **Phase 1** | ✅ 已完成 | 动画引擎 + 主题 + 基础表单组件（Button/Input/Checkbox/Radio/Switch/Slider） |
| **Phase 2** | ✅ 已完成 | 交互+状态组件（Select/Tabs/Pagination/Menu/Tag/Progress/Badge/Alert） |
| **Phase 3** | ⏳ 待开发 | Dialog/Tooltip/Notification/DatePicker/Table/Tree 等 |

---

## 2. 整体架构

### 2.1 分层架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        Application Layer                    │
│                    (用户业务代码 / Demo)                     │
├─────────────────────────────────────────────────────────────┤
│                      UI Components Layer                     │
│  ┌────────┐ ┌───────┐ ┌────────┐ ┌───────┐ ┌────────────┐  │
│  │ Button │ │ Input │ │ Select │ │ Tabs  │ │ Pagination │  │
│  └────────┘ └───────┘ └────────┘ └───────┘ └────────────┘  │
│  ┌────────┐ ┌───────┐ ┌────────┐ ┌───────┐ ┌────────────┐  │
│  │ Checkbox│ │ Radio │ │ Switch │ │Slider │ │    Menu    │  │
│  └────────┘ └───────┘ └────────┘ └───────┘ └────────────┘  │
│  ┌────────┐ ┌──────────┐ ┌───────┐ ┌──────────────────┐    │
│  │  Tag   │ │ Progress │ │ Badge │ │      Alert       │    │
│  └────────┘ └──────────┘ └───────┘ └──────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│                        Core Engine Layer                     │
│  ┌─────────────────┐  ┌──────────┐  ┌──────────────────┐   │
│  │  ElementTheme   │  │  Easing  │  │    Animator      │   │
│  │  (色板+插值)    │  │ (缓动)   │  │ (动画调度器)     │   │
│  └─────────────────┘  └──────────┘  └──────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              AnimatedPopup (弹层容器)                 │   │
│  └──────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│              Java Swing / AWT (JDK 标准库)                   │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心设计思想

1. **动画驱动状态**：每个交互组件持有 `hover` / `focus` / `active` 等 0→1 浮点进度值，由 `Animator` 驱动，`paintComponent` 按进度插值颜色、位移、透明度等视觉属性。

2. **完全自绘**：不使用 Swing 默认组件绘制（`setOpaque(false)` + `setContentAreaFilled(false)`），全部视觉元素在 `paintComponent` 中用 `Graphics2D` 绘制。

3. **插值过渡**：颜色、尺寸、位置等视觉属性的变化不直接切换，而是通过 `ElementTheme.lerp()` 系列方法在动画帧间线性插值。

4. **弹层复用**：`AnimatedPopup` 作为通用弹层容器，被 `AstSelect`、`AstMenu` 等需要下拉弹出的组件复用。

---

## 3. 目录结构

```
swing-element-ui/
├── src/
│   └── org/swelement/
│       ├── core/                          # 核心引擎模块
│       │   ├── ElementTheme.java          # Element UI 色板常量 + 插值工具
│       │   ├── Easing.java                # 缓动函数接口与内置实现
│       │   ├── Animator.java              # 动画调度引擎
│       │   └── AnimatedPopup.java         # 带动画的弹层容器
│       ├── ui/                            # UI 组件模块
│       │   ├── Button.java                # 按钮
│       │   ├── Input.java                 # 文本输入框
│       │   ├── Checkbox.java              # 复选框
│       │   ├── Radio.java                 # 单选按钮
│       │   ├── Switch.java                # 开关
│       │   ├── Slider.java                # 滑块
│       │   ├── Select.java                # 下拉选择
│       │   ├── Tabs.java                  # 标签页
│       │   ├── Pagination.java            # 分页
│       │   ├── Menu.java                  # 导航菜单
│       │   ├── Tag.java                   # 标签
│       │   ├── Progress.java              # 进度条
│       │   ├── Badge.java                 # 徽标
│       │   └── Alert.java                 # 警告提示
│       └── demo/                          # 演示示例模块
│           ├── ButtonDemo.java
│           ├── InputDemo.java
│           ├── CheckboxDemo.java
│           ├── RadioDemo.java
│           ├── SwitchDemo.java
│           ├── SliderDemo.java
│           ├── SelectDemo.java
│           ├── TabsDemo.java
│           ├── PaginationDemo.java
│           ├── MenuDemo.java
│           ├── TagDemo.java
│           ├── ProgressDemo.java
│           ├── BadgeDemo.java
│           └── AlertDemo.java
├── out/                                   # 编译输出目录（.class 文件）
├── docs/superpowers/                      # 设计文档
│   ├── plans/                             # 实施计划
│   └── specs/                             # 设计规格
│       ├── 2026-08-19-swing-element-ui-design.md
│       └── 2026-08-19-swing-element-ui-phase2-design.md
├── build.bat                              # Windows 编译脚本
├── .sources.txt                           # 编译源文件列表（自动生成）
├── README.md                              # 项目说明
└── CodeWiki.md                            # 本文档
```

---

## 4. 核心模块详解

### 4.1 ElementTheme — 主题与色板

**文件**: [ElementTheme.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/core/ElementTheme.java)

#### 4.1.1 职责

- 集中定义 Element UI 标准色板常量
- 提供颜色、整型、浮点型的线性插值（lerp）工具
- 统一圆角半径和字体配置

#### 4.1.2 色板常量

| 常量名 | 十六进制值 | 用途 |
|--------|-----------|------|
| `PRIMARY` | `#409EFF` | 主题主色（蓝） |
| `SUCCESS` | `#67C23A` | 成功状态（绿） |
| `WARNING` | `#E6A23C` | 警告状态（橙） |
| `DANGER` | `#F56C6C` | 危险/错误（红） |
| `INFO` | `#909399` | 信息（灰） |
| `TEXT_MAIN` | `#303133` | 主要文字 |
| `TEXT_REGULAR` | `#606266` | 常规文字 |
| `TEXT_PLACEHOLDER` | `#C0C4CC` | 占位符文字 |
| `BORDER_BASE` | `#DCDFE6` | 基础边框 |
| `FILL_BLANK` | `#FFFFFF` | 空白填充（白） |
| `FILL_BASE` | `#F5F7FA` | 基础填充背景 |
| `RADIUS` | `4` | 圆角半径（px） |
| `FONT` | `Microsoft YaHei 14pt PLAIN` | 基础字体 |

#### 4.1.3 关键方法

| 方法签名 | 说明 |
|----------|------|
| `lerp(Color a, Color b, float t)` | 在两个颜色间按 `t∈[0,1]` 做 RGB 通道线性插值 |
| `lerp(float a, float b, float t)` | 浮点插值 |
| `lerp(int a, int b, float t)` | 整型插值（四舍五入） |
| `selfCheck()` | 内置断言自检 |
| `main(String[] args)` | 入口：运行 `java -ea -cp out org.swelement.core.ElementTheme` |

#### 4.1.4 使用示例

```java
Color hover = ElementTheme.lerp(ElementTheme.FILL_BLANK, ElementTheme.PRIMARY, hoverProgress);
```

---

### 4.2 Easing — 缓动函数

**文件**: [Easing.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/core/Easing.java)

#### 4.2.1 职责

为动画引擎提供标准的缓动（Easing）函数，将线性进度 `t∈[0,1]` 映射为非线性进度，实现「快入慢出」等自然运动感。

#### 4.2.2 接口定义

```java
public interface Easing {
    float apply(float t);   // 输入 0..1，输出 0..1
}
```

`Easing` 是一个 `@FunctionalInterface`，可用 lambda 或方法引用直接构造。

#### 4.2.3 内置实现（静态方法）

| 方法 | 公式 | 效果 | 适用场景 |
|------|------|------|----------|
| `linear(t)` | `t` | 匀速 | 简单线性变化 |
| `easeIn(t)` | `t³` | 先慢后快（加速入场） | 元素出场 |
| `easeOut(t)` | `1 - (1-t)³` | 先快后慢（减速出场） | 元素入场（默认） |
| `easeInOut(t)` | 分段三次 | 两端慢中间快 | 双向平滑过渡 |

所有实现均满足：
- `apply(0) = 0`，`apply(1) = 1`
- 输出值域 `[0, 1]`
- 单调非递减

#### 4.2.4 自检

`Easing.selfCheck()` 对 4 种缓动遍历 101 个采样点断言值域与单调性。

---

### 4.3 Animator — 动画调度引擎

**文件**: [Animator.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/core/Animator.java)

#### 4.3.1 职责

封装 `javax.swing.Timer`，以约 60fps（帧间隔 15ms）的频率驱动动画帧，按指定缓动函数将 `[from, to]` 区间内的当前值回调给监听器。

#### 4.3.2 类结构

```
Animator
├── Listener 接口        void update(float v)  —— 帧回调
├── 字段
│   ├── timer: Timer              Swing 定时器（15ms 间隔）
│   ├── duration: long            动画总时长（ms）
│   ├── easing: Easing            缓动函数
│   ├── listener: Listener        回调
│   ├── from, to: float           起止值
│   └── start: long               起始时间戳
├── 方法
│   ├── go(from, to)              启动动画（可中途重定向）
│   ├── stop()                    立即停止
│   ├── running()                 是否运行中
│   └── tick()                    (private) 帧处理
└── main()                        自检
```

#### 4.3.3 关键行为

1. **帧计算**：每 15ms 触发一次 `tick()`，按真实经过时间计算进度 `p`，避免 Timer 累积误差：
   ```
   p = min(1, (now - start) / duration)
   当前值 = from + (to - from) * easing.apply(p)
   ```

2. **中途重定向**：在动画运行期间再次调用 `go()` 会重置 `start` 时间戳，并将当前值作为新的 `from`，实现无缝衔接的方向反转或目标变更。

3. **自动停止**：当 `p ≥ 1` 时自动调用 `timer.stop()`，最后一帧回调精确为 `to` 值。

#### 4.3.4 典型用法

```java
Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> {
    hover = v;       // 更新组件状态字段
    repaint();       // 触发重绘
});

// 鼠标进入时：从当前值过渡到 1
mouseEntered: hoverAnim.go(hover, 1f);
// 鼠标离开时：从当前值过渡到 0
mouseExited:  hoverAnim.go(hover, 0f);
```

#### 4.3.5 性能说明

- 单个 `Animator` 持有一个 `Timer` 实例；每个交互组件通常持有 2~3 个 Animator（hover/focus/active）
- 当多个动画同时运行时，Swing 事件调度线程（EDT）会合并重绘请求，实际 repaint 次数远少于 Timer 触发次数

---

### 4.4 AnimatedPopup — 动画弹层容器

**文件**: [AnimatedPopup.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/core/AnimatedPopup.java)

#### 4.4.1 职责

提供通用的下拉弹层容器，具备：
- 半透明圆角白底的 Element UI 风格外观
- 淡入 + 下滑入场动画（200ms easeOut）
- 点击弹层/触发源外部区域自动关闭
- 被 `AstSelect`、`AstMenu` 等组件复用

#### 4.4.2 继承关系

`AnimatedPopup` 继承自 `JComponent`，并非 `JPopupMenu`。弹层通过添加到 `JLayeredPane.POPUP_LAYER` 实现窗口内浮层效果，避免了重量级弹出窗口的重绘闪烁问题。

#### 4.4.3 关键方法

| 方法 | 说明 |
|------|------|
| `getContent()` | 返回内部 `JPanel`，调用方在此添加内容组件 |
| `setDismissListener(Runnable)` | 注册弹层被外部点击关闭时的回调 |
| `show(Component invoker, int x, int y)` | 在指定触发源相对坐标处显示弹层，启动淡入动画 |
| `hidePopup()` | (private) 立即关闭并移除弹层 |
| `onAwtEvent(AWTEvent)` | (private) 全局 AWT 鼠标事件监听，判断点击外部关闭 |

#### 4.4.4 动画细节

入场时同时进行两个插值：
- **透明度 alpha**：`0 → 1`（内容面板和边框颜色按 alpha 调整）
- **顶部内边距**：`EmptyBorder(8,0,0,0) → (0,0,0,0)`，实现内容向下滑动进入的视觉

---

## 5. UI 组件详解

### 5.1 设计通则

所有 UI 组件遵循以下通用模式：

1. **继承 Swing 基类**：按语义选择 `JButton` / `JPanel` / `JComponent` 等
2. **关闭默认绘制**：`setOpaque(false)` + `setContentAreaFilled(false)` + `setFocusPainted(false)`
3. **动画状态字段**：`float hover / focus / active` 等，范围 `[0, 1]`
4. **Animator 数组**：每个状态对应一个 Animator 实例
5. **鼠标监听**：`MouseAdapter` 中 `mouseEntered/Exited/Pressed/Released` 启停动画
6. **自定义 paintComponent**：用 `Graphics2D` + `ElementTheme.lerp()` 按状态插值绘制

---

### 5.2 Button — 按钮

**文件**: [Button.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Button.java)

#### 5.2.1 继承

`extends JButton`

#### 5.2.2 类型常量

| 常量 | 值 | 样式 |
|------|----|------|
| `DEFAULT` | 0 | 白底灰边默认按钮 |
| `PRIMARY` | 1 | 蓝底白字主按钮 |
| `SUCCESS` | 2 | 绿底成功 |
| `WARNING` | 3 | 橙底警告 |
| `DANGER` | 4 | 红底危险 |
| `INFO` | 5 | 灰底信息 |

#### 5.2.3 构造函数

```java
Button(String text)                           // 默认类型，非朴素
Button(String text, int type, boolean plain)  // 指定类型与朴素模式
```

#### 5.2.4 动画驱动

持有 2 个 Animator：

| Animator | 时长 | 缓动 | 驱动 |
|----------|------|------|------|
| `hoverAnim` | 200ms | easeInOut | 鼠标进出时 `0↔1` |
| `activeAnim` | 120ms | easeInOut | 鼠标按压时 `0↔1` |

绘制时颜色插值顺序：`BASE → lerp(HOVER) → lerp(ACTIVE)`，实现三级状态平滑过渡。

#### 5.2.5 朴素模式（plain=true）

朴素按钮背景为白色，边框与文字使用主题色；hover 时背景淡化为 `#ECF5FF`（浅蓝），文字过渡为蓝色。

---

### 5.3 Input — 文本输入框

**文件**: [Input.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Input.java)

#### 5.3.1 继承

`extends JPanel`（外层容器，自绘边框 + 内部 JTextField）

#### 5.3.2 构造函数

```java
Input(String placeholder)
```

#### 5.3.3 结构

```
Input (JPanel, BorderLayout)
└── JTextField field  —— 实际文本编辑区（透明无边框）
```

#### 5.3.4 动画状态

| Animator | 功能 |
|----------|------|
| `focusAnim` | focus 时边框变 PRIMARY + 加粗 2px + 外发光（4px 半透明描边） |
| `hoverAnim` | hover 时边框微变 + 底色过渡到 FILL_BASE |
| `clearAnim` | 有文本且 (focus 或 hover) 时，× 清除按钮淡入 |

#### 5.3.5 关键方法

| 方法 | 说明 |
|------|------|
| `getText()` | 获取文本 |
| `setText(String)` | 设置文本 |
| `setEnabled(boolean)` | 联动内部 field 的启用状态 |

---

### 5.4 Checkbox — 复选框

**文件**: [Checkbox.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Checkbox.java)

#### 5.4.1 继承

`extends JCheckBox`

#### 5.4.2 动画效果

- **填充动画**（fillAnim）：选中时方框背景从白过渡到 PRIMARY 蓝
- **打勾动画**（checkAnim）：勾选时通过 `clip` 裁剪区域从左向右扩展，实现勾号「描边揭示」效果
- **边框过渡**（hoverAnim）：hover 时边框淡化为主题色

---

### 5.5 Radio — 单选按钮

**文件**: [Radio.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Radio.java)

#### 5.5.1 继承

`extends JRadioButton`

#### 5.5.2 动画效果

- **外圈边框**（borderAnim）：选中时边框从灰过渡到 PRIMARY
- **内点缩放**（dotAnim）：选中时内部圆点半径从 0 动画放大到 4px（使用 `sqrt(dot)` 非线性映射获得自然感）
- **hover 边框**（hoverAnim）

---

### 5.6 Switch — 开关

**文件**: [Switch.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Switch.java)

#### 5.6.1 继承

`extends JToggleButton`

#### 5.6.2 尺寸

固定 `44×22px`，轨道高 22px，球形旋钮 18px。

#### 5.6.3 动画

单一 `slideAnim`（300ms easeInOut）同时驱动：
- 底色：`#DCDFE6 → PRIMARY`（轨道颜色插值）
- 旋钮位置：`x=2 → x=24`（线性插值）

---

### 5.7 Slider — 滑块

**文件**: [Slider.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Slider.java)

#### 5.7.1 继承

`extends JComponent`

#### 5.7.2 构造函数

```java
Slider(int min, int max, int value)
```

#### 5.7.3 交互

| 操作 | 行为 |
|------|------|
| 鼠标点击轨道 | 直接跳转 + 带动画过渡 |
| 拖拽 | 跟随鼠标，无动画（`dragging` 标志跳过 Animator） |
| Hover | 圆形手柄放大 125%（2px 扩边） |

#### 5.7.4 事件

```java
void addChangeListener(ChangeListener l)   // 值变化时触发
void removeChangeListener(ChangeListener l)
```

#### 5.7.5 关键参数

- 轨道高 6px，圆角 6px
- 填充色 PRIMARY，背景色 `#E4E7ED`
- 手柄默认半径 6px，hover 时 7.5px

---

### 5.8 Select — 下拉选择

**文件**: [Select.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Select.java)

#### 5.8.1 继承

`extends JPanel`（本项目中最复杂的组件）

#### 5.8.2 构造函数

```java
Select(boolean multiple, boolean filterable)
```

| 参数 | 说明 |
|------|------|
| `multiple` | true=多选（顶部 tag 芯片），false=单选 |
| `filterable` | true=内嵌搜索框过滤选项，false=只读展示 |

#### 5.8.3 内部类 `Select.Option`

```java
public static class Option {
    String label;      // 显示文本
    Object value;      // 绑定值
    String group;      // 分组名（可 null）
    boolean disabled;  // 是否禁用
}
```

#### 5.8.4 内部结构

```
Select (JPanel, BorderLayout)
├── center (JPanel)
│   ├── tagsPanel (FlowLayout)    // 多选时的 tag 芯片行
│   ├── field (JTextField)        // filterable=true 时的搜索框
│   └── display (JLabel)          // filterable=false 时的选中展示
└── AnimatedPopup popup
    └── optionList (BoxLayout.Y)
        ├── JLabel (分组标题)
        ├── OptionRow (选项行) × N
        └── JLabel (无匹配提示)
```

#### 5.8.5 动画

| Animator | 功能 |
|----------|------|
| `arrowAnim` | 下拉箭头 0→180° 旋转（使用 `Graphics2D.rotate(Math.PI * arrowAngle)`） |
| `popup` 内部动画 | 由 AnimatedPopup 提供的淡入+下滑 |
| 内部 `OptionRow.hoverAnim` | 选项 hover 时背景渐变 |

#### 5.8.6 公开 API

| 方法 | 说明 |
|------|------|
| `addOption(Option o)` | 添加选项 |
| `getSelected()` | 返回选中 Option 列表（拷贝） |
| `clearSelection()` | 清空选中 |
| `matches(String label, String filter)` | (static) 过滤匹配规则，包含断言自检 |

---

### 5.9 Tabs — 标签页

**文件**: [Tabs.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Tabs.java)

#### 5.9.1 继承

`extends JComponent`

#### 5.9.2 结构

```
Tabs (JComponent, BorderLayout)
├── (paintComponent 自绘 40px 头部 + 下划线指示条)
└── cardPanel (JPanel, CardLayout)
    └── 用户添加的页面 × N
```

#### 5.9.3 动画

| Animator | 功能 |
|----------|------|
| `indXAnim` | 指示条 X 坐标滑动（250ms） |
| `indWAnim` | 指示条宽度过渡（250ms） |
| `contentAnim` | 内容切换淡入（200ms，通过 AlphaComposite 合成） |

#### 5.9.4 API

```java
void addTab(String title, JComponent panel)
int getSelectedIndex()
void setSelectedIndex(int i)
```

---

### 5.10 Pagination — 分页

**文件**: [Pagination.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Pagination.java)

#### 5.10.1 继承

`extends JComponent`

#### 5.10.2 核心算法 `pageWindow()`

生成页码窗口（含省略号 `-1` 标记），规则：
- 始终显示第 1 页和最后 1 页
- 显示当前页前后各 2 页
- 中间不连续的区域插入省略号

示例：
```
cur=5, pages=10 → [1, -1, 3, 4, 5, 6, 7, -1, 10]
cur=1, pages=10 → [1, 2, 3, -1, 10]
```

#### 5.10.3 内部组件

- 上一页/下一页按钮（`‹` / `›`）
- 页码按钮（内部类 `PageButton`，带 hover 渐变动画）
- 省略号标签
- 总数文本 `共 N 条`
- 跳转输入框（JTextField，回车跳转）

#### 5.10.4 API

| 方法 | 说明 |
|------|------|
| `setTotal(int t)` | 设置数据总数 |
| `setPageSize(int s)` | 设置每页条数 |
| `getCurrentPage()` | 获取当前页（1-based） |
| `setCurrentPage(int v)` | 跳转指定页（自动钳制） |
| `addPageChangeListener(IntConsumer)` | 页码变化回调 |

---

### 5.11 Menu — 导航菜单

**文件**: [Menu.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Menu.java)

#### 5.11.1 继承

`extends JComponent`

#### 5.11.2 内部类 `Menu.Entry`

封装菜单项数据：
```
Entry
├── label: String
├── action: Runnable           // 点击回调（无子菜单时）
├── subLabels: String[]        // 子菜单标签
├── subActions: Runnable[]     // 子菜单回调
└── hoverAnim: Animator        // hover 背景过渡
```

#### 5.11.3 动画

- **指示条滑动**：`indXAnim` + `indWAnim`（同 Tabs 模式）
- **菜单项 hover**：每个 Entry 自有 `hoverAnim`
- **子菜单弹出**：通过 `AnimatedPopup` 淡入

#### 5.11.4 API

```java
void addMenuItem(String label, Runnable action)
void addSubMenu(String label, String[] subLabels, Runnable[] subActions)
void setActive(int index)
```

---

### 5.12 Tag — 标签

**文件**: [Tag.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Tag.java)

#### 5.12.1 类型常量

`PRIMARY(0)` / `SUCCESS(1)` / `WARNING(2)` / `DANGER(3)` / `INFO(4)`

每种类型对应 BG / FG / BORDER 三色数组。

#### 5.12.2 关闭动画

`closeAnim`（200ms easeInOut）驱动：
- 宽度从原始宽度线性收缩到 1px（`setPreferredSize`）
- 动画完成（`v>=1f`）后调用 `onClosed` 回调，通知父容器移除

#### 5.12.3 API

```java
Tag(String text, int type, boolean closable)
void setText(String t)
void close(Runnable onClosed)
```

---

### 5.13 Progress — 进度条

**文件**: [Progress.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Progress.java)

#### 5.13.1 动画

`fillAnim`（300ms easeOut）驱动填充宽度动画：调用 `setValue(v)` 时不直接跳变，而是从当前 `shown` 进度平滑过渡到新值 `v/100`。

#### 5.13.2 API

```java
void setValue(int v)        // 0-100 钳制
void setShowText(boolean b) // 是否在右侧显示百分比文字
```

---

### 5.14 Badge — 徽标

**文件**: [Badge.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Badge.java)

#### 5.14.1 功能

为任意子组件（通常是按钮/头像）右上角添加数字角标或红点指示。

#### 5.14.2 动画

每次调用 `setCount(c)` 时，`popAnim`（200ms easeOut）驱动缩放：`0.6 → 1.0`，通过 `Graphics2D.scale(s,s)` 实现「弹出」效果。

#### 5.14.3 规则

- `count > 99`：显示 `99+`，角标形状改为圆角矩形（24px 宽）
- `dot = true`：仅显示 10px 红点，不显示数字
- `count <= 0 && !dot`：跳过绘制（完全透明）

---

### 5.15 Alert — 警告提示

**文件**: [Alert.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Alert.java)

#### 5.15.1 类型常量

`SUCCESS(0)` / `WARNING(1)` / `INFO(2)` / `ERROR(3)`

对应 4 组颜色：左侧竖条色 + 背景色 + 图标字符（`√ ! i ×`）。

#### 5.15.2 动画

| Animator | 时机 | 功能 |
|----------|------|------|
| `inAnim` | 构造时自动播放 | 淡入下滑（300ms easeOut，alpha 0→1） |
| `outAnim` | 调用 `close()` | 高度收缩到 0（250ms easeIn），完成后回调 `onClosed` |

#### 5.15.3 构造

```java
Alert(int type, String title, String desc, boolean closable)
```

`desc=null` 时为精简样式（高 40px），否则为带描述的完整样式（高 56px）。

---

## 6. Demo 模块

**目录**: [demo/](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/demo/)

每个 UI 组件对应一个 `*Demo.java`，均包含 `main()` 方法，用于启动独立 JFrame 展示组件效果。

### Demo 启动命令对照表

| Demo 类 | 功能展示 |
|---------|----------|
| `ButtonDemo` | 6 种按钮类型 + 朴素模式 + 禁用态 |
| `InputDemo` | 占位符 + 清空按钮 + focus 光晕 |
| `CheckboxDemo` | 勾选动画 + hover |
| `RadioDemo` | 单选按钮组 + 圆点缩放动画 |
| `SwitchDemo` | 开关滑动动画 |
| `SliderDemo` | 滑块拖拽 + 值变化动画 |
| `SelectDemo` | 单选 + 分组 + 多选 tag + 过滤搜索 |
| `TabsDemo` | 标签页切换 + 下划线滑动 |
| `PaginationDemo` | 分页 + 省略号 + 跳转 |
| `MenuDemo` | 水平菜单 + 子菜单下拉 |
| `TagDemo` | 5 种 tag 类型 + 关闭动画 |
| `ProgressDemo` | 进度条填充动画 |
| `BadgeDemo` | 数字角标 + 红点模式 |
| `AlertDemo` | 4 种 Alert 类型 + 关闭动画 |

### Demo 通用模板

```java
public class XxxDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {   // 在 EDT 中创建 UI
            JFrame f = new JFrame("Xxx Demo");
            JPanel p = new JPanel(new FlowLayout(20, 20, 20));
            // 添加测试组件...
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);   // 居中
            f.setVisible(true);
        });
    }
}
```

> **注意**：所有 UI 创建和操作必须在 Swing Event Dispatch Thread（EDT）中执行，通过 `SwingUtilities.invokeLater()` 确保线程安全。

---

## 7. 依赖关系

### 7.1 外部依赖

**零外部依赖**。项目仅使用 JDK 标准库：
- `java.awt.*` — AWT 图形基础
- `javax.swing.*` — Swing 组件框架
- `java.awt.geom.*` — 2D 几何图形（RoundRectangle2D、Path2D 等）
- `java.awt.event.*` — 事件监听

### 7.2 内部模块依赖图

```
                    ┌──────────────┐
                    │ ElementTheme │
                    └──────┬───────┘
                           │ 被所有类引用
            ┌──────────────┼──────────────┐
            ▼              ▼              ▼
     ┌──────────┐   ┌──────────┐   ┌─────────────┐
     │  Easing  │   │ Animator │   │ AnimatedPopup│
     └────┬─────┘   └────┬─────┘   └──────┬──────┘
          │              │                │
          └───────┬──────┘                │
                  ▼                       ▼
         ┌─────────────────┐      ┌────────────────┐
         │  UI Components  │      │ Select, Menu   │
         │ (14 个组件)     │      │ (含弹层组件)   │
         └─────────────────┘      └────────────────┘
                  │
                  ▼
         ┌─────────────────┐
         │  Demo Classes   │
         │ (14 个演示)     │
         └─────────────────┘
```

### 7.3 模块依赖详情

| 模块 | 依赖 | 说明 |
|------|------|------|
| **core.ElementTheme** | 无 | 最底层常量与工具 |
| **core.Easing** | 无 | 独立数学函数接口 |
| **core.Animator** | Easing, javax.swing.Timer | 调度引擎 |
| **core.AnimatedPopup** | Animator, Easing, ElementTheme | 弹层容器 |
| **ui.Button** | Animator, Easing, ElementTheme | |
| **ui.Input** | Animator, Easing, ElementTheme | |
| **ui.Checkbox** | Animator, Easing, ElementTheme | |
| **ui.Radio** | Animator, Easing, ElementTheme | |
| **ui.Switch** | Animator, Easing, ElementTheme | |
| **ui.Slider** | Animator, Easing, ElementTheme | |
| **ui.Select** | Animator, AnimatedPopup, Easing, ElementTheme | 依赖 AnimatedPopup |
| **ui.Tabs** | Animator, Easing, ElementTheme | |
| **ui.Pagination** | Animator, Easing, ElementTheme | |
| **ui.Menu** | Animator, AnimatedPopup, Easing, ElementTheme | 依赖 AnimatedPopup |
| **ui.Tag** | Animator, Easing, ElementTheme | |
| **ui.Progress** | Animator, Easing, ElementTheme | |
| **ui.Badge** | Animator, Easing | 不依赖 ElementTheme |
| **ui.Alert** | Animator, Easing, ElementTheme | |
| **demo.\*.java** | 对应 ui.* 类 | 演示入口 |

### 7.4 循环依赖检查

**无循环依赖**。依赖方向严格为：
`demo → ui → core → JDK`

---

## 8. 构建与运行

### 8.1 环境要求

- **JDK**: 8 或更高（推荐 8，脚本默认尝试 `--release 8`）
- **操作系统**: Windows（`build.bat`），Linux/macOS 可手动执行 javac 命令
- **可选路径**: 脚本优先尝试 `C:\Program Files\Java\jdk1.8.0_311\bin\javac.exe`，否则用 PATH 中的 javac

### 8.2 编译

#### Windows 一键编译

```cmd
cd d:\Program Files\code\swing-element-ui
.\build.bat
```

脚本行为：
1. 查找 `javac` 可执行文件
2. 遍历 `src\*.java` 生成 `.sources.txt` 文件列表
3. 尝试 `javac -encoding UTF-8 --release 8 -d out @.sources.txt`
4. 若 JDK 不支持 `--release 8`，回退为 `-source 8 -target 8`
5. 输出 `BUILD OK` 或 `BUILD FAILED`

#### 手动编译（通用）

```bash
# 收集源文件列表
find src -name "*.java" > .sources.txt
# 编译
javac -encoding UTF-8 -d out @.sources.txt
```

### 8.3 运行 Demo

编译后 class 文件位于 `out/` 目录下。通过 `java -cp out` 指定类路径运行：

```cmd
java -cp out org.swelement.demo.ButtonDemo
java -cp out org.swelement.demo.InputDemo
java -cp out org.swelement.demo.CheckboxDemo
java -cp out org.swelement.demo.RadioDemo
java -cp out org.swelement.demo.SwitchDemo
java -cp out org.swelement.demo.SliderDemo
java -cp out org.swelement.demo.SelectDemo
java -cp out org.swelement.demo.TabsDemo
java -cp out org.swelement.demo.PaginationDemo
java -cp out org.swelement.demo.MenuDemo
java -cp out org.swelement.demo.TagDemo
java -cp out org.swelement.demo.ProgressDemo
java -cp out org.swelement.demo.BadgeDemo
java -cp out org.swelement.demo.AlertDemo
```

### 8.4 运行核心自检（断言验证）

使用 `-ea` 开启 assert，对核心模块的逻辑正确性进行验证：

```cmd
java -ea -cp out org.swelement.core.Easing          # 验证缓动函数值域/单调性
java -ea -cp out org.swelement.core.ElementTheme    # 验证插值正确性
java -ea -cp out org.swelement.core.Animator        # 验证动画启停与重定向
java -ea -cp out org.swelement.ui.AstSelect            # 验证过滤匹配逻辑
java -ea -cp out org.swelement.ui.AstPagination        # 验证分页窗口算法
```

全部通过时应输出 `xxx self-check OK`。

---

## 9. 设计模式与架构特点

### 9.1 设计模式应用

| 模式 | 应用位置 | 说明 |
|------|----------|------|
| **观察者模式** | `Slider.addChangeListener()`、`Pagination.addPageChangeListener()`、Swing 原生事件体系 | 多播事件监听 |
| **策略模式** | `Easing` 接口 + 多实现 | 缓动算法可互换 |
| **组合模式** | `AnimatedPopup` 作为容器、`Tabs.cardPanel` 卡片面板 | 容器-内容嵌套 |
| **装饰模式** | `ElementTheme.lerp()` 包装基础颜色 | 动态插值增强 |
| **命令模式** | `Menu.Entry.action: Runnable`、`Button.addActionListener` | 动作封装为对象 |
| **模板方法** | 所有组件覆盖 `paintComponent` | Swing 标准框架 |

### 9.2 架构亮点

1. **动画引擎解耦**：`Animator` 完全通用，不耦合具体组件，任何浮点数属性均可驱动
2. **自绘一致性**：所有组件绕过 Swing 默认 UI 委托（`ComponentUI`），视觉风格 100% 可控
3. **EDT 友好**：所有动画帧回调都在 Swing Timer 的 EDT 线程中执行，天然线程安全
4. **可测试性**：纯逻辑模块（Easing、插值、分页算法）内置 assert 自检 main，无需测试框架
5. **极小表面积**：核心 API 极简（3 个构造/方法即可用），降低学习成本

### 9.3 权衡与取舍

| 决策 | 优点 | 缺点 |
|------|------|------|
| 每个 Animator 独立 Timer | 逻辑简单，各自独立 | 多组件时 Timer 实例较多（可优化为全局单 Timer） |
| 继承 Swing 基类（JButton 等） | 保留原有的 Model/Listener 语义 | 仍携带未使用的默认 L&F 字段 |
| 浮点数进度存储 | 支持中途重定向、无状态跳变 | 每帧触发 repaint，需依赖 Swing 合并 |
| 不使用 ComponentUI | 代码自包含，无需 L&F 注册 | 无法通过 UIManager 全局换肤 |

---

## 10. 扩展开发指南

### 10.1 新增 UI 组件模板

以新增 `MyWidget` 为例：

#### Step 1: 创建文件

在 `src/org/swelement/ui/` 下新建 `MyWidget.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MyWidget extends JComponent {

    // 1) 动画状态字段
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> {
        hover = v;
        repaint();
    });
    private float hover;

    public MyWidget() {
        setOpaque(false);  // 2) 关闭默认绘制
        setPreferredSize(new Dimension(100, 40));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 3) 鼠标事件驱动动画
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) hoverAnim.go(hover, 1f);
            }
            public void mouseExited(MouseEvent e) {
                hoverAnim.go(hover, 0f);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        // 4) 按 hover 进度插值颜色
        Color bg = ElementTheme.lerp(ElementTheme.FILL_BLANK,
                                     ElementTheme.PRIMARY, hover);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                         ElementTheme.RADIUS * 2, ElementTheme.RADIUS * 2);
        g2.dispose();
    }
}
```

#### Step 2: 添加 Demo

在 `src/org/swelement/demo/` 下创建 `MyWidgetDemo.java`（参考现有模板）。

#### Step 3: 注册到构建

无需修改，`.sources.txt` 由 `build.bat` 自动遍历生成。运行 `.\build.bat` 重新编译即可。

### 10.2 自定义缓动函数

`Easing` 是函数式接口，可直接用 lambda 传入自定义曲线：

```java
// 弹性缓动（示例）
Animator bounceAnim = new Animator(400, t -> {
    return (float) (1 - Math.pow(1 - t, 4) * Math.cos(t * Math.PI * 4));
}, v -> { ... });
```

### 10.3 自定义主题色

直接修改 `ElementTheme.java` 中的常量：

```java
public static final Color PRIMARY = new Color(0x6366F1);  // 改为紫蓝主色
```

所有组件会自动采用新值（无需修改组件代码）。

### 10.4 打包为 JAR

编译完成后可手动打包：

```cmd
cd out
jar cvf ../swing-element-ui.jar org
cd ..
```

使用方引入 `swing-element-ui.jar` 到 classpath 即可。

---

## 附录：文件索引

| 类别 | 文件路径 |
|------|---------|
| **核心引擎** | [ElementTheme.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/core/ElementTheme.java) |
| | [Easing.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/core/Easing.java) |
| | [Animator.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/core/Animator.java) |
| | [AnimatedPopup.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/core/AnimatedPopup.java) |
| **UI 组件** | [Button.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Button.java) |
| | [Input.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Input.java) |
| | [Checkbox.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Checkbox.java) |
| | [Radio.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Radio.java) |
| | [Switch.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Switch.java) |
| | [Slider.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Slider.java) |
| | [Select.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Select.java) |
| | [Tabs.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Tabs.java) |
| | [Pagination.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Pagination.java) |
| | [Menu.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Menu.java) |
| | [Tag.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Tag.java) |
| | [Progress.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Progress.java) |
| | [Badge.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Badge.java) |
| | [Alert.java](file:///d:/Program%20Files/code/swing-element-ui/src/org/swelement/ui/Alert.java) |
| **设计文档** | [Phase 1 设计](file:///d:/Program%20Files/code/swing-element-ui/docs/superpowers/specs/2026-08-19-swing-element-ui-design.md) |
| | [Phase 2 设计](file:///d:/Program%20Files/code/swing-element-ui/docs/superpowers/specs/2026-08-19-swing-element-ui-phase2-design.md) |
| **构建脚本** | [build.bat](file:///d:/Program%20Files/code/swing-element-ui/build.bat) |
| **项目说明** | [README.md](file:///d:/Program%20Files/code/swing-element-ui/README.md) |
