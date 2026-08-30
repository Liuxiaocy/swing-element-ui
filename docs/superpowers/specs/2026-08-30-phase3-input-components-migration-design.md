# Phase 3 设计：输入类组件框架化迁移

日期：2026-08-30
分类：Migration（5 个组件，轻量迁移策略）
前置：Phase 2（基础交互组件迁移，已完成）

## 总览

将 5 个核心输入类组件从"手动 ElementTheme + 手动 Animator + JPanel/JComponent 基类"迁移到 AstAbstractComponent 框架体系，接入主题系统和动画管理器。

### 迁移策略：轻量迁移

保持组件当前结构不变（仍使用 JTextField/JTextArea 等原生 Swing 文本组件处理输入），但将颜色、动画、绘制工具统一到框架体系：

- 基类替换 → 接入主题系统、动画管理器、绘制工具
- ElementTheme.xxx → theme().getXxx()
- 手动 Animator → anim.register() / anim.getProgress()
- 手动 Graphics2D 设置 → createGraphics(g)
- 静态 selfCheck → 实例 selfCheck()

**为什么不深度重构为纯自绘？** 输入组件的核心价值是文本编辑能力（输入法、复制粘贴、选区、无障碍等），依赖原生 JTextField 是合理且必要的。深度重构 ROI 极低。

### 组件清单

| 组件 | 当前基类 | 迁移后基类 | 复杂度 |
|------|---------|-----------|--------|
| AstTextArea | JPanel | AstAbstractComponent | ★☆☆ |
| AstInput | JPanel | AstAbstractComponent | ★★☆ |
| AstInputNumber | JComponent | AstAbstractComponent | ★★☆ |
| AstSelect | JPanel | AstAbstractComponent | ★★★ |
| AstDatePicker | JComponent | AstAbstractComponent | ★★★ |

### 为什么用 AstAbstractComponent 而非 AstInteractiveComponent？

1. 输入组件是"容器 + 子组件"复合结构，鼠标事件需要转发给内部文本组件
2. AstInteractiveComponent 的 sticky toggle / selected 状态对输入组件无意义
3. 输入组件有自己的 focus 管理（焦点在内部文本组件上，而非容器本身）
4. hover 动画可以复用基类的 hover 机制

---

## 一、通用改造模式

### 1.1 基类替换

```java
// 旧
public class AstInput extends JPanel {

// 新
public class AstInput extends AstAbstractComponent {
```

删除构造函数中的 `setOpaque(false)`（基类已处理）。

保留 `setLayout(...)`、`add(...)`、`remove(...)` 等容器操作
（AstAbstractComponent extends JComponent extends Container，完全兼容）。

### 1.2 颜色映射

| ElementTheme 常量 | Theme 方法 |
|---|---|
| `ElementTheme.PRIMARY` | `theme().getPrimary()` |
| `ElementTheme.SUCCESS` | `theme().getSuccess()` |
| `ElementTheme.WARNING` | `theme().getWarning()` |
| `ElementTheme.DANGER` | `theme().getDanger()` |
| `ElementTheme.INFO` | `theme().getInfo()` |
| `ElementTheme.TEXT_MAIN` | `theme().getTextPrimary()` |
| `ElementTheme.TEXT_REGULAR` | `theme().getTextRegular()` |
| `ElementTheme.TEXT_SECONDARY` | `theme().getTextSecondary()` |
| `ElementTheme.TEXT_PLACEHOLDER` | `theme().getTextPlaceholder()` |
| `ElementTheme.TEXT_DISABLED` | `theme().getTextDisabled()` |
| `ElementTheme.BORDER_BASE` | `theme().getBorderBase()` |
| `ElementTheme.BORDER_LIGHT` | `theme().getBorderLight()` |
| `ElementTheme.FILL_BLANK` | `theme().getFillBlank()` |
| `ElementTheme.FILL_BASE` | `theme().getFillBase()` |
| `ElementTheme.FILL_LIGHT` | `theme().getFillLight()` |
| `ElementTheme.BG_PAGE` | `theme().getBgPage()` |
| `ElementTheme.FONT` | `theme().getFontBase()` |
| `ElementTheme.RADIUS` | `theme().getRadiusBase()` |
| `ElementTheme.lerp(a, b, t)` | `lerp(a, b, t)` |
| `ElementTheme.assertContrast(...)` | `assertContrast(...)` |

### 1.3 动画迁移模式

**焦点动画**（几乎所有输入组件都有）：
```java
// 旧
private final Animator focusAnim = new Animator(200, Easing::easeInOut, v -> { focus = v; repaint(); });
// + 手动 FocusListener 驱动

// 新
// 基类已内置 focus 动画和焦点监听，直接用 focusProgress()
float focus = focusProgress();
```

但注意：输入组件的焦点在内部 JTextField 上，而非容器本身。基类的 focus 动画是监听容器自身的 FocusEvent。因此需要：
- 当内部文本组件获得/失去焦点时，手动驱动容器的 focus 动画
- 或者将内部文本组件的焦点事件转发给基类

方案：在内部文本组件的 FocusListener 中调用 `anim.go(AnimationManager.FOCUS, ...)`。

**悬停动画**（几乎所有输入组件都有）：
```java
// 旧
private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
// + 手动 MouseListener 驱动

// 新
// 基类已内置 hover 动画和鼠标监听，直接用 hoverProgress()
float hover = hoverProgress();
```

注意：输入组件的鼠标事件主要落在内部文本组件上，需要确保 hover 状态正确传播。方案：保留内部组件的 mouse listener，转发 hover 状态。或者更简单——在容器的 mouse listener 中驱动（因为容器是父组件，鼠标进入子组件也算进入容器）。

实际上 Swing 的 `mouseEntered`/`mouseExited` 事件是按组件边界触发的，鼠标从容器进入子组件时会触发容器的 `mouseExited`。这会导致 hover 闪烁。

解决方案：使用 `MouseMotionListener` 跟踪鼠标位置，或者使用 AWTEventListener 全局监听。但更简单的方式是——保留现有的 hover 传播机制（给内部子组件都加 mouse listener），但将动画从手动 Animator 改为基类 anim。

**结论**：hover 和 focus 的状态判断逻辑保持不变（因为输入组件的复合结构需要特殊处理），但将动画的存储和驱动从手动 Animator 改为 `anim.register()`。

### 1.4 绘制工具迁移

```java
// 旧
Graphics2D g2 = (Graphics2D) g.create();
g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

// 新
Graphics2D g2 = createGraphics(g);
```

### 1.5 自检迁移

```java
// 旧
static void selfCheck() { ... }
public static void main(String[] args) { selfCheck(); }

// 新
@Override
protected void selfCheck() { ... }
public static void main(String[] args) { new AstXxx().selfCheck(); }
```

---

## 二、各组件详细设计

### 2.1 AstTextArea（最简单，作为试点）

**当前结构**：
- extends JPanel
- 内含 JTextArea + JScrollPane
- 手动 focusAnim + hoverAnim
- 手动边框绘制（圆角、focus 光晕）
- 占位符绘制

**迁移改动**：
1. `extends JPanel` → `extends AstAbstractComponent`
2. 删除 `focusAnim`、`hoverAnim` 字段和 `focus`、`hover` 变量
3. 构造函数中删除 `setOpaque(false)`
4. `paintComponent` 中：
   - `createGraphics(g)` 替换手动设置
   - `ElementTheme.BORDER_BASE` → `theme().getBorderBase()`
   - `ElementTheme.PRIMARY` → `theme().getPrimary()`
   - `ElementTheme.FILL_BLANK` → `theme().getFillBlank()`
   - `ElementTheme.FILL_BASE` → `theme().getFillBase()`
   - `ElementTheme.RADIUS` → `theme().getRadiusBase()`
   - `ElementTheme.lerp(...)` → `lerp(...)`
   - `ElementTheme.TEXT_PLACEHOLDER` → `theme().getTextPlaceholder()`
   - `ElementTheme.FONT` → `theme().getFontBase()`
   - `focus` → `anim.getProgress("focus")`
   - `hover` → `anim.getProgress("hover")`
5. FocusListener 中：
   - `focusAnim.go(focus, 1f)` → `anim.go("focus", anim.getProgress("focus"), 1f)`
   - `focusAnim.go(focus, 0f)` → `anim.go("focus", anim.getProgress("focus"), 0f)`
6. MouseListener 中：
   - `hoverAnim.go(hover, 1f)` → `anim.go("hover", anim.getProgress("hover"), 1f)`
   - `hoverAnim.go(hover, 0f)` → `anim.go("hover", anim.getProgress("hover"), 0f)`
7. `initComponent()` 注册 "focus" 和 "hover" 动画
   - 注意：基类 AstInteractiveComponent 自动注册 hover/focus，但 AstAbstractComponent 不注册。需要手动注册。

等等——AstAbstractComponent 是否注册 hover/focus 动画？让我确认。

实际上，hover 和 focus 是 AstInteractiveComponent 注册的。AstAbstractComponent 只注册基础的动画管理器，不注册具体动画。

所以对于继承 AstAbstractComponent 的输入组件：
- 需要手动 `anim.register("focus", 200, Easing::easeInOut)`
- 需要手动 `anim.register("hover", 200, Easing::easeInOut)`
- 需要手动驱动这些动画（通过内部组件的事件监听）

8. selfCheck 从静态改为实例方法

### 2.2 AstInput

**当前结构**：
- extends JPanel
- 内含 JTextField/JPasswordField + 清空按钮 + 前后缀图标 + 眼睛按钮
- 手动 focusAnim + hoverAnim + clearAnim
- 手动边框绘制（圆角、focus 光晕、invalid 红色）
- 三档尺寸（LARGE/DEFAULT/SMALL）

**迁移改动**：
1. `extends JPanel` → `extends AstAbstractComponent`
2. 删除三个 Animator 字段和对应 float 变量
3. 构造函数中删除 `setOpaque(false)`
4. `initComponent()` 注册 "focus"、"hover"、"clear" 动画
5. 所有 ElementTheme 颜色替换为 theme().getXxx()
6. focus/hover/clear 动画驱动改为 anim.go()
7. `paintComponent` 中用 createGraphics(g) 和 lerp()
8. `ElementTheme.assertContrast` → `assertContrast`（如果有）
9. selfCheck 从静态改为实例方法

注意：AstInput 的 hoverKeeper 机制（鼠标在 field/east/west 之间移动时保持 hover）需要保留——因为 Swing 的 mouseEntered/mouseExited 在父子组件间切换时会触发 exited。所以 hover 状态不能直接用基类的 hover 机制，需要保持现有的手动状态管理，但动画存储改为 anim。

### 2.3 AstInputNumber

**当前结构**：
- extends JComponent
- 内含 JTextField + 两个 StepButton（内部类）
- StepButton 有自己的 hoverAnim
- 长按加速 Timer
- 三档尺寸

**迁移改动**：
1. `extends JComponent` → `extends AstAbstractComponent`
2. 外框绘制的 ElementTheme 颜色替换
3. StepButton 内部类：
   - 可以让 StepButton extends AstInteractiveComponent，复用 hover/active 能力
   - 或者保留 StepButton 独立，只是把 Animator 替换掉
   - 建议：StepButton 继承 AstInteractiveComponent，代码更简洁
4. setInvalid 中的 ElementTheme.DANGER → theme().getDanger()
5. StepButton.paintComponent 中的 ElementTheme.assertContrast → assertContrast
6. selfCheck 从静态改为实例方法

### 2.4 AstSelect

**当前结构**：
- extends JPanel
- 内含显示区 + 下拉弹出 + 选项列表
- arrowAnim + clearAnim + 手动 hover 状态
- 单选/多选模式
- 可清空、可过滤
- 三档尺寸

**迁移改动**：
1. `extends JPanel` → `extends AstAbstractComponent`
2. 删除 arrowAnim、clearAnim → 注册到 anim
3. hover 状态管理保留手动（原因同 AstInput），但动画改为 anim
4. 所有 ElementTheme 颜色替换
5. createGraphics(g) 替换手动设置
6. selfCheck 迁移

### 2.5 AstDatePicker

**当前结构**：
- extends JComponent
- 内含 AstButton（已迁移）+ AnimatedPopup + CalendarPanel
- 尺寸档位

**迁移改动**：
1. `extends JComponent` → `extends AstAbstractComponent`
2. 所有 ElementTheme 颜色替换为 theme().getXxx()
3. CalendarPanel 内部类中的颜色也需要替换
4. selfCheck 迁移
5. 注意：AstDatePicker 使用 AstButton 作为 invoker，而 AstButton 已经迁移到框架，所以这部分天然兼容

---

## 三、验证策略

每个组件迁移后运行：
1. 编译通过
2. selfCheck() 断言全部通过
3. 对比度断言通过（WCAG AA）
4. 添加到 run-checks.bat

---

## 四、涉及文件

**修改**：
- `src/org/swelement/ui/AstTextArea.java`
- `src/org/swelement/ui/AstInput.java`
- `src/org/swelement/ui/AstInputNumber.java`
- `src/org/swelement/ui/AstSelect.java`
- `src/org/swelement/ui/AstDatePicker.java`
- `run-checks.bat`（添加 5 个组件的自检）
- `build.bat`（添加 5 个组件到 SOURCES 列表和自检流程）
