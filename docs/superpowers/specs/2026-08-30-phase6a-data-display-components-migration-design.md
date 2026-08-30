# Phase 6A：数据展示类组件迁移设计文档（第一批）

**日期：** 2026-08-30
**阶段：** Phase 6A（数据展示类 - 第一批）
**目标组件：** AstIcon、AstTimeline、AstCard、AstCalendar、AstCarousel

---

## 1. 概述

本阶段将 5 个数据展示类组件迁移到 `AstAbstractComponent` 新框架体系，替换旧的 `ElementTheme` 静态引用和 `Animator` 动画系统，改用 `theme()` 主题访问和 `AnimationManager` 动画管理器。

### 组件清单与复杂度

| 组件 | 行数 | 基类 | 动画数量 | 复杂度 | 迁移模式 |
|------|------|------|----------|--------|----------|
| AstIcon | ~1040 | JComponent | 1（Timer 旋转） | ★☆☆ | 组件类 → AstDisplayComponent |
| AstTimeline | 227 | JComponent | N（每项一个 hover 动画） | ★☆☆ | 组件类 → AstDisplayComponent |
| AstCard | 269 | JComponent | 1（hover 动画） | ★★☆ | 组件类 → AstDisplayComponent |
| AstCalendar | 349 | JComponent | 1（fade 动画） | ★★☆ | 组件类 → AstInteractiveComponent |
| AstCarousel | 254 | JComponent | 2（slide + arrow hover） | ★★☆ | 组件类 → AstInteractiveComponent |

---

## 2. 迁移模式

所有 5 个组件均为**组件类**（非静态工具类），直接继承 `AstAbstractComponent` 的子类：

- **AstIcon** → `AstDisplayComponent`（纯展示，无交互事件）
- **AstTimeline** → `AstDisplayComponent`（纯展示，hover 仅是视觉效果）
- **AstCard** → `AstDisplayComponent`（展示容器，hover 仅是视觉效果）
- **AstCalendar** → `AstInteractiveComponent`（有鼠标点击交互）
- **AstCarousel** → `AstInteractiveComponent`（有鼠标点击交互）

---

## 3. 各组件迁移要点

### 3.1 AstIcon

**当前状态：**
- `extends JComponent`
- 使用 `ElementTheme.PRIMARY/SUCCESS/WARNING/DANGER/INFO/TEXT_REGULAR` 等静态常量
- 使用 `javax.swing.Timer` 做 LOADING 图标旋转（非 `Animator`）
- `setOpaque(false)` 手动设置

**迁移要点：**
1. `extends AstDisplayComponent`
2. `ElementTheme.X` → `theme().getX()`
3. `ElementTheme.lerp(a, b, t)` → `lerp(a, b, t)`
4. `ElementTheme.assertContrast(fg, bg, where)` → `assertContrast(fg, bg, where)`
5. 删除 `setOpaque(false)`（基类构造器已处理）
6. `paintIcon` 静态方法内部的 `ElementTheme` 引用保持不变（静态方法无法访问实例 `theme()`）
7. **旋转动画**：当前使用 `Timer` + `spinPhase` 变量。由于 `AnimationManager` 管理的是从 from 到 to 的进度动画，而旋转是无限循环，**保留 Timer 方案**更合适（AnimationManager 不适合无限循环动画）
8. selfCheck：静态方法，内部的 `ElementTheme` 引用保持不变

**特殊处理：**
- `paintIcon` 是核心静态工具方法，被多处静态调用。由于静态方法无法访问实例 `theme()`，且图标绘制中使用的主题色（PRIMARY、SUCCESS、WARNING、DANGER、INFO）是状态色而非文本色，在所有主题中基本一致，**保留 ElementTheme 静态引用**。

---

### 3.2 AstTimeline

**当前状态：**
- `extends JComponent`
- 使用 `ElementTheme.BORDER_BASE/TEXT_MAIN/TEXT_REGULAR/FILL_BASE/PRIMARY/SUCCESS/WARNING/DANGER/INFO/RADIUS/FONT`
- 使用 `List<Animator> hoverAnims` 管理每项的 hover 动画（150ms easeInOut）
- `setOpaque(false)` 手动设置

**迁移要点：**
1. `extends AstDisplayComponent`
2. `ElementTheme.X` → `theme().getX()`
3. `ElementTheme.lerp(a, b, t)` → `lerp(a, b, t)`
4. `ElementTheme.assertContrast(fg, bg, where)` → `assertContrast(fg, bg, where)`
5. 删除 `setOpaque(false)`
6. **动画迁移**：
   - 当前每项一个 `Animator` 实例 → 改为使用 `AnimationManager` 注册命名动画
   - 由于 item 数量不固定，使用 `"hover_0"`, `"hover_1"`, ... 动态命名
   - 在 `initComponent()` 中注册动画（因 item 数量在构造时已知，可在构造器中注册）
   - `hoverAnims.get(i).stop(); hoverAnims.get(i).go(from, to)` → `anim.go("hover_" + i, from, to)`
   - `hoverStates.get(i)` → `anim.getProgress("hover_" + i)`
7. selfCheck：静态方法，保持结构

**动画注册方案：**
```java
@Override
protected void initComponent() {
    super.initComponent();
    for (int i = 0; i < items.size(); i++) {
        anim.register("hover_" + i, 150, Easing::easeInOut);
    }
}
```

---

### 3.3 AstCard

**当前状态：**
- `extends JComponent`
- 使用 `ElementTheme.BORDER_BASE/PRIMARY/TEXT_MAIN/RADIUS/FONT`
- 使用 `Animator hoverAnim`（150ms easeInOut）
- `setOpaque(false)` 手动设置
- `setLayout(null)` 手动布局
- 包含 `JPanel headerActions` 子组件

**迁移要点：**
1. `extends AstDisplayComponent`
2. `ElementTheme.X` → `theme().getX()`
3. `ElementTheme.lerp(a, b, t)` → `lerp(a, b, t)`
4. `ElementTheme.assertContrast(fg, bg, where)` → `assertContrast(fg, bg, where)`
5. 删除 `setOpaque(false)`
6. **动画迁移**：
   - `hoverAnim` → `anim.register("hover", 150, Easing::easeInOut)`
   - `hoverAnim.stop(); hoverAnim.go(hover, target)` → `anim.go("hover", anim.getProgress("hover"), target)`
   - `hover` 变量 → `anim.getProgress("hover")`
7. `headerActions` JPanel 保持不变（子容器，非主题/动画相关）
8. selfCheck：静态方法，保持结构

---

### 3.4 AstCalendar

**当前状态：**
- `extends JComponent`
- 使用 `ElementTheme.TEXT_MAIN/TEXT_REGULAR/TEXT_PLACEHOLDER/PRIMARY/INFO/FILL_BASE/FONT`
- 使用 `Animator fadeAnim`（180ms easeOut）做月份切换淡入
- `setOpaque(false)` 手动设置
- 有鼠标点击交互（选中日期、翻月/翻年）
- `hoverCell` 纯状态，无动画（直接 repaint）

**迁移要点：**
1. `extends AstInteractiveComponent`（有鼠标交互）
2. `ElementTheme.X` → `theme().getX()`
3. `ElementTheme.lerp(a, b, t)` → `lerp(a, b, t)`
4. `ElementTheme.assertContrast(fg, bg, where)` → `assertContrast(fg, bg, where)`
5. 删除 `setOpaque(false)`
6. **动画迁移**：
   - `fadeAnim` → `anim.register("fade", 180, Easing::easeOut)`
   - `fadeAnim.stop(); fadeAnim.go(from, to)` → `anim.go("fade", from, to)`
   - `alpha` 变量 → `anim.getProgress("fade")`
7. `hoverCell` 保持不变（无动画，纯状态变量）
8. `gridInfo()`、`daysInMonth()`、`cellAt()`、`navAt()`、`displayedDayForCell()` 等工具方法保持不变
9. selfCheck：静态方法，保持结构

---

### 3.5 AstCarousel

**当前状态：**
- `extends JComponent`
- 使用 `ElementTheme.PRIMARY/BORDER_BASE/FONT`
- 使用两个 `Animator`：
  - `slideAnim`（300ms easeInOut）— 幻灯片切换
  - `arrowAnim`（180ms easeInOut）— 箭头 hover
- `setOpaque(false)` 手动设置
- 有鼠标点击交互（左右箭头点击、prev/next/goTo）
- 使用 `Timer autoTimer` 做自动播放

**迁移要点：**
1. `extends AstInteractiveComponent`（有鼠标交互）
2. `ElementTheme.X` → `theme().getX()`
3. `ElementTheme.lerp(a, b, t)` → `lerp(a, b, t)`
4. `ElementTheme.assertContrast(fg, bg, where)` → `assertContrast(fg, bg, where)`
5. 删除 `setOpaque(false)`
6. **动画迁移**：
   - `slideAnim` → `anim.register("slide", 300, Easing::easeInOut)`
   - `arrowAnim` → `anim.register("arrow", 180, Easing::easeInOut)`
   - `slideAnim.stop(); slideAnim.go(from, to)` → `anim.go("slide", from, to)`
   - `arrowAnim.stop(); arrowAnim.go(from, to)` → `anim.go("arrow", from, to)`
   - `offset` 变量 → `anim.getProgress("slide")`
   - `arrowHover` 变量 → `anim.getProgress("arrow")`
7. `autoTimer` 保持不变（`javax.swing.Timer`，非 Animator 体系）
8. selfCheck：静态方法，保持结构

---

## 4. API 替换对照表

| 旧 API | 新 API | 适用组件 |
|--------|--------|----------|
| `ElementTheme.PRIMARY` | `theme().getPrimary()` | 全部 |
| `ElementTheme.SUCCESS` | `theme().getSuccess()` | AstIcon, AstTimeline, AstCarousel |
| `ElementTheme.WARNING` | `theme().getWarning()` | AstIcon, AstTimeline, AstCalendar |
| `ElementTheme.DANGER` | `theme().getDanger()` | AstIcon, AstTimeline |
| `ElementTheme.INFO` | `theme().getInfo()` | AstIcon, AstTimeline, AstCalendar |
| `ElementTheme.TEXT_MAIN` | `theme().getTextMain()` | AstCard, AstTimeline, AstCalendar |
| `ElementTheme.TEXT_REGULAR` | `theme().getTextRegular()` | AstTimeline, AstCalendar |
| `ElementTheme.TEXT_PLACEHOLDER` | `theme().getTextPlaceholder()` | AstCalendar |
| `ElementTheme.BORDER_BASE` | `theme().getBorderBase()` | AstCard, AstTimeline, AstCarousel |
| `ElementTheme.FILL_BASE` | `theme().getFillBase()` | AstTimeline, AstCalendar |
| `ElementTheme.RADIUS` | `theme().getRadius()` | AstCard, AstTimeline |
| `ElementTheme.FONT` | `theme().getFont()` | 全部 |
| `ElementTheme.lerp(a, b, t)` | `lerp(a, b, t)` | AstCard, AstTimeline |
| `ElementTheme.assertContrast(fg, bg, where)` | `assertContrast(fg, bg, where)` | 全部 |
| `new Animator(dur, easing, listener)` | `anim.register(name, dur, easing)` | 全部有动画的 |
| `anim.stop(); anim.go(from, to)` | `anim.go(name, from, to)` | 全部有动画的 |
| `setOpaque(false)` | 删除（基类处理） | 全部 |

---

## 5. 自检迁移策略

所有 5 个组件的 selfCheck 均为**静态方法**，保持静态调用方式：

- 静态方法中使用 `ElementTheme` 的断言和主题色引用保持不变（静态上下文无法访问实例 `theme()`）
- 自检中创建的组件实例会自动触发实例化时的主题初始化
- 离屏绘制验证对比度断言的方式保持不变

---

## 6. 风险与注意事项

1. **AstTimeline 动态命名动画**：item 数量不固定，需在构造器中循环注册 `"hover_0"` 到 `"hover_N"` 动画。
2. **AstIcon 静态 paintIcon 方法**：保留 `ElementTheme` 静态引用，不迁移到 `theme()`。这是合理的，因为图标颜色主要是状态色（PRIMARY/SUCCESS 等），在各主题中保持一致。
3. **AstCalendar 的 alpha 最小值**：当前代码有 `Math.max(0.15f, alpha)`，迁移后改为 `Math.max(0.15f, anim.getProgress("fade"))`。
4. **AstCarousel 的 slideAnim offset**：当前 `offset` 是像素值（负数），AnimationManager 的 go/Progress 会直接使用这个值，不需要归一化。

---

## 7. 验证标准

迁移完成后，以下条件必须全部满足：

- [ ] 所有 5 个组件编译通过（JDK 8）
- [ ] AstIcon self-check 通过
- [ ] AstTimeline self-check 通过
- [ ] AstCard self-check 通过
- [ ] AstCalendar self-check 通过
- [ ] AstCarousel self-check 通过
- [ ] run-checks.bat 更新总数（29 → 34）
- [ ] 全部 34 项自检通过
