# Phase 3: 输入类组件迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 5 个输入类组件（AstTextArea、AstInput、AstInputNumber、AstSelect、AstDatePicker）从手动 ElementTheme + Animator + JPanel/JComponent 迁移到 AstAbstractComponent 框架体系，接入主题系统和动画管理器。

**Architecture:** 轻量迁移策略——保持组件内部结构不变（仍使用原生 JTextField/JTextArea 处理文本输入），仅替换基类、颜色系统、动画系统和绘制工具。

**Tech Stack:** Java Swing, JDK 8, 零外部依赖

---

## 通用迁移模式（所有 5 个组件共用）

### A. 基类替换
- `extends JPanel` 或 `extends JComponent` → `extends AstAbstractComponent`
- 删除构造函数中的 `setOpaque(false)`（基类已处理）
- 添加 `initComponent()` 方法，注册所需动画

### B. 颜色替换表
| ElementTheme | Theme 方法 |
|---|---|
| `ElementTheme.PRIMARY` | `theme().getPrimary()` |
| `ElementTheme.DANGER` | `theme().getDanger()` |
| `ElementTheme.INFO` | `theme().getInfo()` |
| `ElementTheme.TEXT_MAIN` | `theme().getTextPrimary()` |
| `ElementTheme.TEXT_REGULAR` | `theme().getTextRegular()` |
| `ElementTheme.TEXT_PLACEHOLDER` | `theme().getTextPlaceholder()` |
| `ElementTheme.TEXT_DISABLED` | `theme().getTextDisabled()` |
| `ElementTheme.BORDER_BASE` | `theme().getBorderBase()` |
| `ElementTheme.FILL_BLANK` | `theme().getFillBlank()` |
| `ElementTheme.FILL_BASE` | `theme().getFillBase()` |
| `ElementTheme.FILL_LIGHT` | `theme().getFillLight()` |
| `ElementTheme.FONT` | `theme().getFontBase()` |
| `ElementTheme.RADIUS` | `theme().getRadiusBase()` |
| `ElementTheme.lerp(a, b, t)` | `lerp(a, b, t)` |
| `ElementTheme.assertContrast(...)` | `assertContrast(...)` |

### C. 动画迁移模式
- 删除 `Animator` 字段和对应的 float 进度变量
- 在 `initComponent()` 中用 `anim.register(name, duration, easing)` 注册
- 动画驱动从 `xxxAnim.go(from, to)` 改为 `anim.go("name", anim.getProgress("name"), to)`
- 绘制时从 `anim.getProgress("name")` 获取进度
- focus 和 hover 的状态传播逻辑保持不变（因为输入组件复合结构需要特殊处理）

### D. 绘制工具
- `Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(...)` → `Graphics2D g2 = createGraphics(g);`

### E. 自检迁移
- `static void selfCheck()` → `@Override protected void selfCheck()`
- `public static void main(String[] args) { selfCheck(); }` → `public static void main(String[] args) { new AstXxx().selfCheck(); }`

---

## Task 1: 迁移 AstTextArea

**File:** `src/org/swelement/ui/AstTextArea.java`（135 行）

### 改动清单

1. **import 调整**：
   - 删除 `org.swelement.core.Animator`、`org.swelement.core.ElementTheme`
   - 添加 `org.swelement.framework.AstAbstractComponent`、`org.swelement.core.theme.Theme`
   - 保留 `javax.swing.*`、`java.awt.*`、`java.awt.event.*`、`java.awt.geom.RoundRectangle2D`、`javax.swing.border.EmptyBorder`、`javax.swing.event.DocumentListener`

2. **类声明**：`extends JPanel` → `extends AstAbstractComponent`

3. **删除字段**：
   ```java
   private final Animator focusAnim = new Animator(200, Easing::easeInOut, v -> { focus = v; repaint(); });
   private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
   private float focus, hover;
   ```

4. **添加 initComponent**：
   ```java
   @Override
   protected void initComponent() {
       super.initComponent();
       anim.register("focus", 200, Easing::easeInOut);
       anim.register("hover", 200, Easing::easeInOut);
   }
   ```

5. **构造函数**：
   - 删除 `setOpaque(false)`
   - FocusListener 中 `focusAnim.go(focus, 1f)` → `anim.go("focus", anim.getProgress("focus"), 1f)`
   - FocusListener 中 `focusAnim.go(focus, 0f)` → `anim.go("focus", anim.getProgress("focus"), 0f)`
   - MouseAdapter 中 `hoverAnim.go(hover, 1f)` → `anim.go("hover", anim.getProgress("hover"), 1f)`
   - MouseAdapter 中 `hoverAnim.go(hover, 0f)` → `anim.go("hover", anim.getProgress("hover"), 0f)`

6. **paintComponent**：
   - 使用 `createGraphics(g)`
   - `ElementTheme.lerp(...)` → `lerp(...)`
   - `ElementTheme.BORDER_BASE` → `theme().getBorderBase()`
   - `ElementTheme.PRIMARY` → `theme().getPrimary()`
   - `ElementTheme.FILL_BLANK` → `theme().getFillBlank()`
   - `ElementTheme.FILL_BASE` → `theme().getFillBase()`
   - `ElementTheme.RADIUS` → `theme().getRadiusBase()`
   - `ElementTheme.TEXT_PLACEHOLDER` → `theme().getTextPlaceholder()`
   - `ElementTheme.FONT` → `theme().getFontBase()`
   - `focus` → `anim.getProgress("focus")`
   - `hover` → `anim.getProgress("hover")`
   - `new Color(0xE4E7ED)` → 保留（禁用边框色，无直接主题对应）

7. **selfCheck 迁移**：
   - `static void selfCheck()` → `@Override protected void selfCheck()`
   - `main` 方法中 `selfCheck()` → `new AstTextArea("p", 3, 20).selfCheck()`
   - 添加对比度断言（文字 vs 背景）

### 验证
- [ ] 编译通过
- [ ] 自检通过：`java -ea -cp out org.swelement.ui.AstTextArea`

---

## Task 2: 迁移 AstInput

**File:** `src/org/swelement/ui/AstInput.java`（366 行）

### 改动清单

1. **import 调整**：
   - 删除 `org.swelement.core.Animator`、`org.swelement.core.ElementTheme`
   - 添加 `org.swelement.framework.AstAbstractComponent`
   - 保留其他 import

2. **类声明**：`extends JPanel` → `extends AstAbstractComponent`

3. **删除字段**：
   ```java
   private final Animator focusAnim = new Animator(200, Easing::easeInOut, v -> { focus = v; repaint(); });
   private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
   private final Animator clearAnim = new Animator(150, Easing::easeInOut, v -> { clearVis = v; syncClear(); repaint(); });
   private float focus, hover, clearVis;
   ```
   保留 `hasText, hovering, focused` 等 boolean 状态变量（用于逻辑判断）。

   注意：`clearVis` 变量仍然需要，因为 `syncClear()` 中要读取它来设置 clearBtn 的 alpha 和 interactive 状态。但它从 anim 获取。
   实际上，`clearAnim` 的 update 回调做了三件事：更新 clearVis、调用 syncClear()、调用 repaint()。
   迁移后，动画由 AnimationManager 驱动，每次更新都会触发 repaint()。但 syncClear() 需要在每次动画更新时调用。
   
   方案：在 paintComponent 中调用 syncClear()？不好，会频繁调用。
   更好的方案：保留 clearVis 变量，但由 anim 驱动。AnimationManager 的动画更新会触发 repaint，但不会调用自定义回调。
   
   等等——AnimationManager 的实现是怎样的？让我检查一下。
   
   从之前的代码看，AnimationManager 内部持有 Animator，Animator 有 update listener 会调用 repaint。
   所以 `anim.register()` 注册的动画在更新时会自动触发 owner 的 repaint()。
   
   但 clearAnim 的 update 还需要调用 syncClear()。这个需求如何满足？
   
   方案 A：在 paintComponent 开头调用 syncClear()。每次绘制前同步一次 clear 按钮状态。虽然调用频繁，但逻辑简单且开销很小。
   
   方案 B：保留一个单独的 Animator 给 clear 用，不走 anim 管理器。
   
   方案 C：给 AnimationManager 添加 listener 支持。
   
   建议用方案 A，最简单且开销可忽略。
   
   所以 `clearVis` 变量改为从 `anim.getProgress("clear")` 读取，syncClear() 在 paintComponent 开头调用。

   等等，syncClear() 调用 `clearBtn.setAlpha(clearVis)` 和 `clearBtn.setInteractive(...)`。
   如果在 paintComponent 中调用，每次重绘都设置，这没问题。
   
   但是 `updateClear()` 中调用 `clearAnim.go(clearVis, target)` 来启动动画。
   迁移后改为 `anim.go("clear", anim.getProgress("clear"), target)`。
   
   所以结论是：
   - 删除 `clearAnim` 字段
   - `clearVis` 变量删除，改为 `anim.getProgress("clear")`
   - `updateClear()` 中 `clearAnim.go(clearVis, target)` → `anim.go("clear", anim.getProgress("clear"), target)`
   - `syncClear()` 中参数从 `clearVis` 改为 `anim.getProgress("clear")`
   - 在 `paintComponent` 开头调用 `syncClear()`（替代原来的 Animator update 回调）

4. **添加 initComponent**：
   ```java
   @Override
   protected void initComponent() {
       super.initComponent();
       anim.register("focus", 200, Easing::easeInOut);
       anim.register("hover", 200, Easing::easeInOut);
       anim.register("clear", 150, Easing::easeInOut);
   }
   ```

5. **构造函数**：
   - 删除 `setOpaque(false)`
   - `ElementTheme.FONT` → `theme().getFontBase()`
   - `ElementTheme.TEXT_MAIN` → `theme().getTextPrimary()`
   - FocusListener 中 focusAnim.go → anim.go("focus", ...)
   - hoverAnim.go → anim.go("hover", ...)
   - ICON_COLOR 和 ICON_HOVER 保留为常量（图标颜色，非主题色）

6. **applyTier**：
   - `ElementTheme.FONT.deriveFont(...)` → `theme().getFontBase().deriveFont(...)`

7. **paintPlaceholder**：
   - `ElementTheme.TEXT_PLACEHOLDER` → `theme().getTextPlaceholder()`

8. **updateClear**：
   - `clearAnim.go(clearVis, target)` → `anim.go("clear", anim.getProgress("clear"), target)`

9. **syncClear**：
   - `clearBtn.setAlpha(clearVis)` → `clearBtn.setAlpha(anim.getProgress("clear"))`
   - `clearBtn.setInteractive(isEnabled() && clearVis > 0.5f)` → `clearBtn.setInteractive(isEnabled() && anim.getProgress("clear") > 0.5f)`

10. **paintComponent**：
    - 开头添加 `syncClear();`（替代 Animator update 回调）
    - `createGraphics(g)` 替换手动设置
    - 所有 ElementTheme 颜色替换
    - `ElementTheme.lerp(...)` → `lerp(...)`
    - `focus` → `anim.getProgress("focus")`
    - `hover` → `anim.getProgress("hover")`
    - `ElementTheme.RADIUS * 2` → `theme().getRadiusBase() * 2`
    - `ElementTheme.DANGER` → `theme().getDanger()`
    - `new Color(64, 158, 255, Math.round(50 * focus))` → `new Color(theme().getPrimary().getRed(), theme().getPrimary().getGreen(), theme().getPrimary().getBlue(), Math.round(50 * anim.getProgress("focus")))`
      或者更简单：直接写死 `new Color(64, 158, 255, ...)`，因为光晕颜色就是主色半透明
      
      更好的方式：使用主题主色：
      ```java
      Color primary = theme().getPrimary();
      g2.setColor(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), Math.round(50 * anim.getProgress("focus"))));
      ```

11. **selfCheck 迁移**：
    - `static void selfCheck()` → `@Override protected void selfCheck()`
    - `main` 中 `selfCheck()` → `new AstInput("test").selfCheck()`
    - 注意：selfCheck 中引用了私有字段（如 `clearBtn`、`eyeBtn`），静态方法可以访问同包的私有字段，但实例方法也可以访问自己的私有字段。
    - 测试辅助方法（`clearBtnClickForTest`、`findTextComponent`、`eyeClickForTest`、`countAstIcons`）保持为 private static 或改为 private 实例方法。
    - 由于这些方法在 selfCheck 中被调用，且 selfCheck 现在是实例方法，这些辅助方法也可以改为实例方法。
    - 但为了最小改动，保持它们为 private static 即可（静态方法可以访问同类的私有成员）。

### 验证
- [ ] 编译通过
- [ ] 自检通过：`java -ea -cp out org.swelement.ui.AstInput`

---

## Task 3: 迁移 AstInputNumber

**File:** `src/org/swelement/ui/AstInputNumber.java`（318 行）

### 改动清单

1. **import 调整**：
   - 删除 `org.swelement.core.Animator`、`org.swelement.core.ElementTheme`
   - 添加 `org.swelement.framework.AstAbstractComponent`
   - 保留其他 import

2. **类声明**：`extends JComponent` → `extends AstAbstractComponent`

3. **StepButton 内部类**：
   - StepButton 也从手动 Animator 迁移
   - 方案：StepButton extends AstInteractiveComponent，复用 hover/active 能力
   - 但 StepButton 是内部类，且有特殊的绘制（左/右边框、+/- 符号）
   - 迁移后：
     - `extends JComponent` → `extends AstInteractiveComponent`
     - 删除 `hoverAnim` 字段和 `hover` 变量
     - 删除 `pressed` 变量（基类有 `isPressing()`）
     - 鼠标监听中 hover 相关删除（基类处理）
     - 但 pressed 和长按逻辑需要保留
     - paintComponent 中使用 `hoverProgress()` 获取 hover 进度

4. **StepButton 构造函数**：
   - 删除 `setOpaque(false)`、`setCursor(...)`（基类已处理）
   - 删除 `hoverAnim` 相关代码
   - 保留 pressed 状态管理、holdTimer 逻辑（这些是 StepButton 特有的）
   - mousePressed 中的 `pressed = true` 保留（基类也会设置 pressing，但我们需要在 StepButton 中跟踪自己的状态）
   - 实际上，基类的 `isPressing()` 可以替代 `pressed` 变量
   - mouseEntered/mouseExited 的 hover 驱动删除（基类处理）
   - 但 mouseExited 中还有 `holdTimer.stop()`、`holdingPlus = false` 等逻辑，需要保留
   - 重写 `onActiveChanged(boolean active)` 来处理按下状态变化？
   - 或者更简单：保留鼠标监听，但只处理业务逻辑（长按、步进），hover 动画由基类处理

   建议方案：保留 StepButton 的 MouseListener，但移除 hoverAnim 相关的行。hover 进度从 `hoverProgress()` 获取。

5. **StepButton.paintComponent**：
   - `createGraphics(g)` 替换手动设置
   - `hover` → `hoverProgress()`
   - `ElementTheme.lerp(...)` → `lerp(...)`
   - `ElementTheme.FILL_BASE` → `theme().getFillBase()`
   - `ElementTheme.BORDER_BASE` → `theme().getBorderBase()`
   - `ElementTheme.TEXT_MAIN` → `theme().getTextPrimary()`
   - `ElementTheme.TEXT_PLACEHOLDER` → `theme().getTextPlaceholder()`
   - `ElementTheme.FONT` → `theme().getFontBase()`
   - `ElementTheme.assertContrast(...)` → `assertContrast(...)`

6. **外框相关**：
   - AstInputNumber 本身没有外框绘制（边框由 StepButton 绘制左右边线，中间是 JTextField）
   - 所以主类的 paintComponent 可能不存在，或者很简单
   - 检查后发现：AstInputNumber 没有重写 paintComponent，它是纯组合式组件
   - setInvalid 使用 `BorderFactory.createLineBorder(ElementTheme.DANGER, 1)` → `BorderFactory.createLineBorder(theme().getDanger(), 1)`

7. **applyTier**：
   - `ElementTheme.FONT.deriveFont(...)` → `theme().getFontBase().deriveFont(...)`

8. **selfCheck 迁移**：
   - `static void selfCheck()` → `@Override protected void selfCheck()`
   - `main` 中 `selfCheck()` → `new AstInputNumber(0, 100, 1, 50).selfCheck()`

### 验证
- [ ] 编译通过
- [ ] 自检通过：`java -ea -cp out org.swelement.ui.AstInputNumber`

---

## Task 4: 迁移 AstSelect

**File:** `src/org/swelement/ui/AstSelect.java`（460 行）

### 改动清单

1. **import 调整**：
   - 删除 `org.swelement.core.Animator`、`org.swelement.core.ElementTheme`
   - 添加 `org.swelement.framework.AstAbstractComponent`
   - 保留其他 import

2. **类声明**：`extends JPanel` → `extends AstAbstractComponent`

3. **删除字段**：
   ```java
   private final Animator arrowAnim = new Animator(200, Easing::easeInOut, v -> { arrowAngle = v; repaint(); });
   private final Animator clearAnim = new Animator(150, Easing::easeInOut, v -> { clearVis = v; syncClear(); repaint(); });
   private float arrowAngle;
   private float clearVis;
   ```
   arrowAngle 和 clearVis 改为从 anim 获取进度。

4. **添加 initComponent**：
   ```java
   @Override
   protected void initComponent() {
       super.initComponent();
       anim.register("arrow", 200, Easing::easeInOut);
       anim.register("clear", 150, Easing::easeInOut);
   }
   ```

5. **构造函数**：
   - 删除 `setOpaque(false)`
   - `ElementTheme.FONT` → `theme().getFontBase()`
   - `arrowAnim.go(arrowAngle, 0f)` → `anim.go("arrow", anim.getProgress("arrow"), 0f)`
   - `arrowAnim.go(arrowAngle, 1f)` → `anim.go("arrow", anim.getProgress("arrow"), 1f)`
   - `clearAnim.go(clearVis, target)` → `anim.go("clear", anim.getProgress("clear"), target)`
   - hover 状态变量 `hovering` 保留（逻辑判断需要）
   - 各种颜色常量（`0x909399`、`0xF4F4F5`、`0x606266` 等）保留为硬编码（这些是次要颜色，非主题核心色）

6. **applyTier**：
   - `ElementTheme.FONT.deriveFont(...)` → `theme().getFontBase().deriveFont(...)`

7. **updateClear**：
   - `clearAnim.go(clearVis, target)` → `anim.go("clear", anim.getProgress("clear"), target)`

8. **syncClear**：
   - `clearBtn.setAlpha(clearVis)` → `clearBtn.setAlpha(anim.getProgress("clear"))`
   - `clearBtn.setInteractive(clearVis > 0.5f)` → `clearBtn.setInteractive(anim.getProgress("clear") > 0.5f)`

9. **togglePopup**：
   - `arrowAnim.go(...)` → `anim.go("arrow", ...)`

10. **choose**：
    - `arrowAnim.go(...)` → `anim.go("arrow", ...)`

11. **rebuildList**：
    - `ElementTheme.FONT` → `theme().getFontBase()`
    - `new Color(0x909399)` 保留（分组标签颜色）

12. **updateDisplay**：
    - `ElementTheme.FONT.deriveFont(...)` → `theme().getFontBase().deriveFont(...)`
    - chip 颜色（`0xF4F4F5`、`0x606266`）保留

13. **paintComponent**：
    - 开头添加 `syncClear();`（替代 clearAnim 的 update 回调）
    - `createGraphics(g)` 替换手动设置
    - `ElementTheme.PRIMARY` → `theme().getPrimary()`
    - `ElementTheme.DANGER` → `theme().getDanger()`
    - `ElementTheme.FILL_BASE` → `theme().getFillBase()`
    - `new Color(0xDCDFE6)` 保留（默认边框）
    - `new Color(0xE4E7ED)` 保留（禁用边框）
    - `arrowAngle` → `anim.getProgress("arrow")`
    - `clearVis` → `anim.getProgress("clear")`
    - `new Color(0xC0C4CC)` 保留（箭头颜色）

14. **OptionRow 内部类**：
    - OptionRow 有自己的 hoverAnim，也需要迁移
    - OptionRow 继承 JPanel，比较轻量
    - 方案：OptionRow 也迁移到 AstInteractiveComponent，复用 hover 能力
    - 但 OptionRow 是内部类，且在弹出列表中
    - 简化方案：OptionRow 的 hoverAnim 也改为用 anim 管理器
    - 但 OptionRow 继承 JPanel → 改为继承 AstInteractiveComponent 会带来额外 overhead
    - 建议：OptionRow 保持为 JPanel，只替换颜色引用（ElementTheme → theme()）
    - 等等，OptionRow 是内部类，访问不到外部类的 theme() 吗？可以，因为它是非静态内部类，可以访问外部类的方法
    - 但 OptionRow 自己有 Animator，迁移需要每个 OptionRow 都有 AnimationManager 实例，开销较大
    - 折中：OptionRow 保留手动 Animator（内部类、生命周期短、数量不多），只替换颜色引用
    - 实际上，为了保持一致性，还是应该迁移。让 OptionRow extends AstInteractiveComponent。
    
    建议：OptionRow extends AstInteractiveComponent，删除 hoverAnim，用 hoverProgress()。

15. **OptionRow.paintComponent**：
    - `createGraphics(g)` 替换手动设置
    - `ElementTheme.lerp(...)` → `lerp(...)`
    - `ElementTheme.FONT` → `theme().getFontBase()`
    - `ElementTheme.PRIMARY` → `theme().getPrimary()`
    - `ElementTheme.TEXT_REGULAR` → `theme().getTextRegular()`
    - `new Color(0xF5F7FA)` 保留（hover 背景色）
    - `new Color(0xC0C4CC)` 保留（禁用文字色）
    - `hover` → `hoverProgress()`

16. **selfCheck 迁移**：
    - `public static void selfCheck()` → `@Override protected void selfCheck()`
    - `main` 中 `selfCheck()` → `new AstSelect(new String[]{"test"}).selfCheck()`
    - 测试辅助方法保持 static

### 验证
- [ ] 编译通过
- [ ] 自检通过：`java -ea -cp out org.swelement.ui.AstSelect`

---

## Task 5: 迁移 AstDatePicker

**File:** `src/org/swelement/ui/AstDatePicker.java`

### 改动清单

先完整读取文件再制定详细计划。核心改动点：

1. **import 调整**：删除 ElementTheme，添加 AstAbstractComponent
2. **类声明**：`extends JComponent` → `extends AstAbstractComponent`
3. **颜色替换**：所有 ElementTheme.xxx → theme().getXxx()
4. **CalendarPanel 内部类**：颜色也替换
5. **selfCheck 迁移**：静态 → 实例
6. AstDatePicker 使用 AstButton 作为 invoker，AstButton 已迁移，天然兼容

### 验证
- [ ] 编译通过
- [ ] 自检通过：`java -ea -cp out org.swelement.ui.AstDatePicker`

---

## 最终验证

- [ ] 编译所有 5 个组件 + 依赖
- [ ] 更新 run-checks.bat（14 → 19 项）
- [ ] 更新 build.bat（添加 5 个组件到 SOURCES 和自检流程）
- [ ] 运行全部 19 项自检，全部通过
