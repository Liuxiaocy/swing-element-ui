# swing-element-ui 组件设计通用规范

日期：2026-08-23
状态：生效中
适用范围：本项目所有组件的新建、修改、重构

## 1. 可访问性与对比度（最高优先级）

### 1.1 文字对比度要求

**任何状态下，文字颜色与背景色的对比度必须满足 WCAG 2.1 AA 级（≥ 4.5:1）。**

适用状态包括但不限于：
- 默认、hover、active、focus
- disabled、loading
- plain、text、round、circle
- 尺寸变化（large/default/small）
- 任意组合状态

### 1.2 工具方法

项目已内置对比度工具，所有组件必须使用：

```java
// 断言对比度（java -ea 时生效，不满足抛 AssertionError）
ElementTheme.assertContrast(fgColor, bgColor, "component-state description");

// 计算相对亮度（WCAG 标准，范围 [0,1]）
float lum = ElementTheme.luminance(color);

// 自动选择对比度足够的文字色（WHITE / TEXT_MAIN #303133 / BLACK）
Color textColor = ElementTheme.pickTextColorForBg(bgColor);
```

### 1.3 对比度设计原则

1. **浅色背景配深色文字**：plain 模式、disabled 模式、text 按钮 hover 背景等浅色背景，文字必须使用深色版本（如 `#606266` 或各 type 的深色变体），禁止使用白色或浅色文字。
2. **深色背景配浅色文字**：实心彩色按钮等深色/彩色背景，可使用白色文字，但需验证对比度。
3. **半透明状态**：loading 等半透明状态，优先使用正常颜色降透明度，而非切换到低对比度的灰色。
4. **禁止"灰色文字+浅灰/浅蓝背景"组合**：disabled 状态禁止使用 `#C0C4CC` 文字配 `#A0CFFF`/`#F5F7FA` 背景（对比度仅 ~2:1）。

### 1.4 各 type 深色文字参考（plain 模式用）

在浅色背景上确保 ≥ 4.5:1 对比度的各 type 深色文字：

| type | 原色 | 深色文字 | 对应浅色背景 |
|---|---|---|---|
| DEFAULT | `#606266` | `#606266` | `#FFFFFF` |
| PRIMARY | `#409EFF` | `#1d6fb5` | `#ECF5FF` |
| SUCCESS | `#67C23A` | `#2d6b18` | `#F0F9EB` |
| WARNING | `#E6A23C` | `#955d12` | `#FDF6EC` |
| DANGER | `#F56C6C` | `#b83232` | `#FEF0F0` |
| INFO | `#909399` | `#606266` | `#F4F4F5` |

### 1.5 例外

实心彩色按钮（白色文字在彩色背景上）遵循 Element UI 标准设计，视觉清晰但严格 WCAG 对比度可能不足。此类需在组件设计文档中明确标注为例外，并确保 hover/active 状态对比度提升。

## 2. 颜色系统

### 2.1 主题色板

使用 `ElementTheme` 常量，禁止硬编码颜色值：

```java
ElementTheme.PRIMARY   // #409EFF
ElementTheme.SUCCESS   // #67C23A
ElementTheme.WARNING   // #E6A23C
ElementTheme.DANGER    // #F56C6C
ElementTheme.INFO      // #909399
ElementTheme.TEXT_MAIN    // #303133
ElementTheme.TEXT_REGULAR // #606266
ElementTheme.TEXT_PLACEHOLDER // #C0C4CC
ElementTheme.BORDER_BASE // #DCDFE6
ElementTheme.FILL_BLANK  // #FFFFFF
ElementTheme.FILL_BASE   // #F5F7FA
ElementTheme.RADIUS      // 4
ElementTheme.FONT        // Microsoft YaHei 14px
```

### 2.2 颜色插值

动画过渡使用 `ElementTheme.lerp()`：

```java
Color c = ElementTheme.lerp(colorA, colorB, progress); // progress 0~1
```

## 3. 动画系统

### 3.1 Animator 使用

- 所有动画使用 `Animator`（封装 `javax.swing.Timer`，16ms 帧间隔）
- 标准时长：hover 200ms，active 120ms，loading 800ms（循环）
- 缓动函数：交互用 `Easing::easeInOut`，循环加载用 `Easing::linear`
- 每个动画状态独立一个 Animator 实例

### 3.2 动画状态模式

交互组件统一使用 `0~1` float 进度字段：

```java
private float hover, active; // 0=未激活, 1=完全激活
private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
```

鼠标事件启停动画，`paintComponent` 按进度插值。

## 4. 自绘组件规范

### 4.1 基本要求

- 继承 `JComponent` 或 `JPanel`（按钮继承 `JButton`）
- 覆盖 `paintComponent(Graphics g)`，使用 `Graphics2D`
- 开启抗锯齿：`g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)`
- 关闭默认绘制：`setOpaque(false)`、`setContentAreaFilled(false)`、`setFocusPainted(false)`
- 使用 `Graphics2D.create()` 创建副本，操作完成后 `dispose()`

### 4.2 尺寸计算

- 覆盖 `getPreferredSize()`，根据内容（文字、图标、内边距）计算
- 内边距按尺寸档位变化，禁止硬编码固定宽高
- circle 模式返回正方形

## 5. 自检规范

### 5.1 必须包含 selfCheck

每个非平凡组件必须包含静态 `selfCheck()` 方法和 `main(String[] args)` 入口：

```java
static void selfCheck() {
    // 功能断言
    // 对比度断言（必须）
    System.out.println("Xxx self-check OK");
}
public static void main(String[] args) { selfCheck(); }
```

### 5.2 对比度断言（必须）

每个组件的 `selfCheck()` 必须包含所有状态的对比度断言：

```java
// 默认状态
ElementTheme.assertContrast(fg, bg, "Xxx default");
// hover 状态
ElementTheme.assertContrast(hoverFg, hoverBg, "Xxx hover");
// disabled 状态
ElementTheme.assertContrast(disabledFg, disabledBg, "Xxx disabled");
// plain 模式（如有）
// loading 状态（如有）
// text 模式（如有）
```

### 5.3 构建集成

- `build.bat` 编译后自动运行所有组件的自检
- 新增组件需在 `build.bat` 的 SOURCES 列表和自检列表中添加
- 自检失败则构建失败

## 6. 文档规范

### 6.1 设计文档

每个组件的新建或重大修改必须有设计文档：
- 路径：`docs/superpowers/specs/YYYY-MM-DD-<component>-design.md`
- 必须包含：目标、范围、API 设计、颜色方案（含对比度验证）、动画、自检方案
- 必须引用本文档的对比度要求

### 6.2 实施计划

复杂修改必须有实施计划：
- 路径：`docs/superpowers/plans/YYYY-MM-DD-<feature>-plan.md`
- 按任务分解，每个任务可独立测试和提交

## 7. 代码审查检查清单

代码审查时必须检查：

- [ ] 所有状态的文字对比度 ≥ 4.5:1（运行 `java -ea` 自检验证）
- [ ] 无硬编码颜色值（使用 ElementTheme 常量或定义在类顶部的常量数组）
- [ ] 动画使用 Animator，无 Thread.sleep 或手动 Timer
- [ ] selfCheck() 包含对比度断言
- [ ] paintComponent 使用 Graphics2D.create()/dispose()
- [ ] disabled 状态文字不是 `#C0C4CC` 浅灰（除非对比度验证通过）
- [ ] plain 模式文字不是白色（除非对比度验证通过）
- [ ] build.bat 已更新（新增文件时）
