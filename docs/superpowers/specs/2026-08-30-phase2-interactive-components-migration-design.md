# Phase 2: 基础交互组件迁移设计

> 将 4 个基础交互组件从旧 Swing 基类 + ElementTheme + 手动 Animator 模式迁移到新框架 AstInteractiveComponent 体系

## 1. 背景与目标

### 1.1 现状

Phase 1 已完成 6 个展示/容器组件的迁移，验证了展示型组件的迁移模式。Phase 2 迁移 4 个基础交互组件：

| 组件 | 当前继承 | 行数 | Animator 数 | 复杂度 |
|------|---------|------|------------|--------|
| AstSwitch | JToggleButton | 46 | 1 | 低 |
| AstRadio | JRadioButton | 68 | 3 | 中低 |
| AstCheckbox | JCheckBox | 82 | 3 | 中 |
| AstButton | JButton | 336 | 3 | 高 |

共同特点：
- 继承 Swing 标准按钮类（JButton/JToggleButton 等）
- 使用 StickyToggleModel（除 AstButton 外）管理选中状态
- 手动创建 Animator 管理交互动画
- 使用 ElementTheme 静态常量获取颜色
- 部分组件缺少 selfCheck

### 1.2 目标

1. 将 4 个组件完全迁移到 `AstInteractiveComponent` 基类
2. 在 `AstInteractiveComponent` 中内置选中状态能力（替代 StickyToggleModel）
3. 所有组件补充完整的 selfCheck（含对比度断言）
4. 迁移后组件支持运行时主题切换

### 1.3 约束

- 完全迁移到 AstInteractiveComponent（不保留 Swing 按钮基类）
- 选中状态能力内置到 AstInteractiveComponent 基类
- 允许破坏性 API 变更
- 全部补充完整自检
- 保持零外部依赖，JDK 8 兼容

## 2. 基类改造：AstInteractiveComponent 选中状态

### 2.1 新增字段

```java
private boolean selected = false;
private boolean pressStarted = false; // sticky 行为：鼠标按下时的状态记录
private final EventListenerList listenerList = new EventListenerList();
```

### 2.2 新增动画

在 `initComponent()` 中注册：
```java
anim.register("selected", 200, Easing::easeInOut);
```

### 2.3 新增方法

```java
public boolean isSelected() { return selected; }

public void setSelected(boolean selected) {
    if (this.selected == selected) return;
    boolean old = this.selected;
    this.selected = selected;
    anim.go("selected", old ? 1f : 0f, selected ? 1f : 0f);
    fireItemStateChanged(selected);
    onSelectedChanged(selected);
}

public void addItemListener(ItemListener l) { listenerList.add(ItemListener.class, l); }
public void removeItemListener(ItemListener l) { listenerList.remove(ItemListener.class, l); }

protected void onSelectedChanged(boolean selected) { } // 钩子

protected void fireItemStateChanged(boolean selected) {
    ItemEvent e = new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
            selected ? ItemEvent.SELECTED : ItemEvent.DESELECTED);
    for (ItemListener l : listenerList.getListeners(ItemListener.class)) {
        l.itemStateChanged(e);
    }
}
```

### 2.4 Sticky 行为

StickyToggleModel 的核心特性：鼠标按下后即使指针移出组件边界，释放时仍会切换状态。

实现方式：
- `mousePressed`: `pressStarted = true;`
- `mouseReleased`: 如果 `pressStarted` 为 true 且组件仍启用，则切换选中状态；`pressStarted = false;`
- `mouseExited`: 不重置 `pressStarted`（这就是 sticky 的关键）
- `mouseDragged`: 不处理

这样即使鼠标移出组件，只要不释放按钮，再移回来释放仍然会切换。

注意：`mouseClicked` 不再用于状态切换，改用 `mousePressed` + `mouseReleased` 组合以支持 sticky。

### 2.5 便捷方法

```java
protected float selectedProgress() { return anim.getProgress("selected"); }
```

## 3. 组件迁移设计

### 3.1 AstSwitch（最简单，验证模式）

**继承链**：`JToggleButton` → `AstInteractiveComponent`

**动画迁移**：
- 删除 `slideAnim` 字段
- 注册：`anim.register("slide", 300, Easing::easeInOut)`
- 选中状态变化时：`anim.go("slide", current, selected ? 1f : 0f)`
- 基类的 selected 动画不需要（slide 已经是视觉过渡）

**绘制**：
- 轨道颜色：`lerp(new Color(0xDCDFE6), theme().getPrimary(), anim.getProgress("slide"))`
- 滑块位置：`2 + slide * (getWidth() - knobSize - 4)`
- 使用 `createGraphics(g)`

**自检补充**：
- 尺寸断言（44x22）
- 选中/未选中状态切换
- 轨道颜色对比度
- 滑块位置正确性

### 3.2 AstRadio

**继承链**：`JRadioButton` → `AstInteractiveComponent`

**动画迁移**：
- 删除 `dotAnim`/`borderAnim`/`hoverAnim` 字段
- 注册：`anim.register("dot", 200, Easing::easeOut)`、`anim.register("border", 200, Easing::easeInOut)`
- hover 使用基类的 hover 动画（`hoverProgress()`）
- 选中状态变化时驱动 dot 和 border 动画

**绘制**：
- 边框颜色：`lerp(theme().getBorderBase(), theme().getPrimary(), Math.max(selectedProgress(), hoverProgress()))`
- 内点半径：`4 * (float) Math.sqrt(anim.getProgress("dot"))`
- 文字绘制：偏移 24px
- 使用 `createGraphics(g)`

**自检补充**：
- 尺寸断言
- 选中/未选中状态
- hover 状态
- 对比度（文字与背景）

### 3.3 AstCheckbox

**继承链**：`JCheckBox` → `AstInteractiveComponent`

**动画迁移**：
- 删除 `fillAnim`/`checkAnim`/`hoverAnim` 字段
- 注册：`anim.register("fill", 200, Easing::easeInOut)`、`anim.register("check", 200, Easing::easeOut)`
- hover 使用基类的 hover 动画
- 选中状态变化时驱动 fill 和 check 动画

**绘制**：
- 边框颜色：`lerp(theme().getBorderBase(), theme().getPrimary(), Math.max(anim.getProgress("fill"), hoverProgress()))`
- 背景：`lerp(theme().getFillBlank(), theme().getPrimary(), anim.getProgress("fill"))`
- 勾号：使用 clip 裁剪揭示动画（从左到右，宽度 = 14 * checkProgress）
- 使用 `createGraphics(g)`

**自检补充**：
- 尺寸断言
- 选中/未选中状态
- 勾号动画进度
- 对比度

### 3.4 AstButton（最复杂）

**继承链**：`JButton` → `AstInteractiveComponent`

**颜色数组迁移**：
- 删除所有静态颜色数组（`BASE_BG`、`HOVER_BG`、`ACTIVE_BG`、`BASE_FG`、`HOVER_FG`、`BORDER`、`TYPE_FG`、`PLAIN_BG`、`PLAIN_FG`、`PLAIN_HOVER_BG`、`PLAIN_ACTIVE_BG`、`PLAIN_ACTIVE_FG`）
- 替换为实例方法，从 `theme()` 获取颜色后计算衍生色

**动画迁移**：
- hover/active 使用基类标准动画（`hoverProgress()` / `activeProgress()`）
- loading 动画：`anim.register("load", 800, Easing::linear)`，通过递归 `anim.get("load").go(0f, 1f, () -> { if (loading) startLoadLoop(); })` 实现无限循环

**绘制**：
- 4 种模式：textButton / loading / disabled / normal
- normal 模式：plain 和非 plain 分支
- 颜色插值：使用基类 `lerp()` 方法
- 使用 `createGraphics(g)`
- 文字 + 图标位置计算保持不变

**自检迁移**：
- 已有 selfCheck，将 `ElementTheme.assertContrast(...)` 替换为 `assertContrast(...)`
- 静态 → 实例方法

## 4. 迁移顺序

| 批次 | 组件 | 预估工作量 | 理由 |
|------|------|-----------|------|
| 前置 | AstInteractiveComponent 选中状态 | 中 | 基类能力，3 个组件依赖 |
| 1 | AstSwitch | 小 | 最简单，验证基类选中状态 + 滑动动画模式 |
| 2 | AstRadio | 中低 | 圆形单选，内点 sqrt 缩放，2 个自定义动画 |
| 3 | AstCheckbox | 中 | 勾号 clip 揭示动画，3 个自定义动画 |
| 4 | AstButton | 高 | 最复杂，6 type × 多属性组合，loading 循环动画 |

## 5. 验证策略

- 每个组件迁移后：编译 + selfCheck
- 全部完成后：run-checks.bat 全量验证
- 子代理驱动方式：每个批次派遣独立子 agent，两阶段审查

## 6. API 变更说明

由于是完全迁移，以下 API 发生变化：

- 不再继承 JButton/JToggleButton 等，失去 Swing 按钮模型的全部能力
- 新增 `isSelected()` / `setSelected()` / `addItemListener()` / `removeItemListener()`（基类提供）
- 新增 `selectedProgress()`（选中动画进度）
- AstButton 不再支持 ActionListener（需改用 MouseListener 或添加支持）
- 文字通过 `setText()` / `getText()` 仍可用（JComponent 不提供，需自己实现）

**注意**：AstButton 的 `setText`/`getText`/`addActionListener` 等 JButton 特有方法，迁移后需要自行实现或放弃。本设计假设保留核心方法（setText/getText/addActionListener）。
