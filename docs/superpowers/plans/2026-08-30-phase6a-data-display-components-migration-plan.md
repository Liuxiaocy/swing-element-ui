# Phase 6A：数据展示类组件迁移实施计划（第一批）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 5 个数据展示类组件（AstIcon、AstTimeline、AstCard、AstCalendar、AstCarousel）迁移到 AstAbstractComponent 新框架，替换 ElementTheme 静态引用和 Animator 动画系统。

**Architecture:** 组件类直接继承 AstAbstractComponent 的子类（AstDisplayComponent 或 AstInteractiveComponent），将 ElementTheme.X 替换为 theme().getX()，将 Animator 实例替换为 AnimationManager 的命名动画。静态 selfCheck 方法保持不变。

**Tech Stack:** Java 8, Swing, AstAbstractComponent framework, AnimationManager, Theme system

---

## 文件结构

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/org/swelement/ui/AstIcon.java` | 修改 | extends AstDisplayComponent，主题引用替换 |
| `src/org/swelement/ui/AstTimeline.java` | 修改 | extends AstDisplayComponent，动画 + 主题替换 |
| `src/org/swelement/ui/AstCard.java` | 修改 | extends AstDisplayComponent，动画 + 主题替换 |
| `src/org/swelement/ui/AstCalendar.java` | 修改 | extends AstInteractiveComponent，动画 + 主题替换 |
| `src/org/swelement/ui/AstCarousel.java` | 修改 | extends AstInteractiveComponent，动画 + 主题替换 |
| `run-checks.bat` | 修改 | 新增 5 项自检，总数 29 → 34 |

---

### Task 1: 迁移 AstIcon

**Files:**
- Modify: `src/org/swelement/ui/AstIcon.java`

**说明：** AstIcon 是纯绘制组件，无 Animator 动画（用 Timer 做旋转），大部分绘制在静态 `paintIcon` 方法中。实例方法中替换主题引用，静态方法保留 ElementTheme。

- [ ] **Step 1: 修改类声明和 import**

将第 1-7 行的 import 和类声明替换为：

```java
package org.swelement.ui;

import org.swelement.core.ElementTheme;
import org.swelement.framework.AstDisplayComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.*;
```

将 `public class AstIcon extends JComponent` 改为 `public class AstIcon extends AstDisplayComponent`

- [ ] **Step 2: 修改构造器，删除 setOpaque(false)**

找到两个构造器中的 `setOpaque(false);` 行，全部删除（基类已处理）。

位置：构造器 `AstIcon(Type type, Color color, int size)` 末尾，约第 93 行。

- [ ] **Step 3: 替换实例 paintComponent 中的主题引用**

`paintComponent` 方法（约第 174 行）本身不直接使用 ElementTheme，它调用静态 `paintIcon`。无需修改。

- [ ] **Step 4: 验证 selfCheck 静态方法无需修改**

selfCheck 是静态方法，内部使用 `ElementTheme` 常量保持不变。确认 `main` 方法也保持不变。

- [ ] **Step 5: 编译并运行自检**

```bash
javac -encoding UTF-8 -d out src/org/swelement/ui/AstIcon.java
java -ea -cp out org.swelement.ui.AstIcon
```

Expected: `AstIcon self-check OK (54 icons)`

---

### Task 2: 迁移 AstTimeline

**Files:**
- Modify: `src/org/swelement/ui/AstTimeline.java`

**说明：** AstTimeline 有 N 个 hover 动画（每项一个），需迁移到 AnimationManager 动态命名动画。

- [ ] **Step 1: 修改 import 和类声明**

将第 1-11 行替换为：

```java
package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.framework.AstDisplayComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
```

将 `public class AstTimeline extends JComponent` 改为 `public class AstTimeline extends AstDisplayComponent`

- [ ] **Step 2: 删除 Animator 相关字段，添加 initComponent**

删除字段：
```java
private final List<Animator> hoverAnims = new ArrayList<Animator>();
```

保留 `hoverStates` 列表（仍用于存储当前值，但改为从 anim 读取）。

**实际方案：** 由于每项动画用动态命名 `"hover_"+i`，`hoverStates` 列表可以删除，改为直接通过 `anim.getProgress("hover_"+i)` 读取。

修改构造器：
- 删除 `import org.swelement.core.Animator;`
- 删除 `hoverAnims` 相关的循环初始化代码
- 删除 `setOpaque(false);`

添加 `initComponent` 方法：
```java
@Override
protected void initComponent() {
    super.initComponent();
    for (int i = 0; i < items.size(); i++) {
        anim.register("hover_" + i, 150, Easing::easeInOut);
    }
}
```

**注意：** `items` 是 final 字段，在构造器中初始化，在 `initComponent` 中可安全访问（initComponent 在父类构造器中调用，而父类构造器在子类字段初始化后、子类构造器体之前调用？不，Java 中父类构造器先于子类字段初始化执行。）

**修正方案：** 由于 `items` 在构造器参数传入后才赋值，而 `initComponent()` 在 `super()` 调用时执行（此时 items 尚未初始化），动画注册需要移到构造器中进行。

构造器末尾添加：
```java
for (int i = 0; i < this.items.size(); i++) {
    anim.register("hover_" + i, 150, Easing::easeInOut);
}
```

删除 `initComponent` 覆写。

- [ ] **Step 3: 替换 mouseMoved 中的动画调用**

将：
```java
hoverAnims.get(i).stop();
hoverAnims.get(i).go(hoverStates.get(i), target);
```

替换为：
```java
anim.go("hover_" + i, anim.getProgress("hover_" + i), target);
```

将 `hoverStates.get(i)` 替换为 `anim.getProgress("hover_" + i)`。

mouseExited 中同理替换。

- [ ] **Step 4: 替换 paintComponent 中的 hover 读取**

将 `float hover = hoverStates.get(i);` 替换为：
```java
float hover = anim.getProgress("hover_" + i);
```

- [ ] **Step 5: 替换所有 ElementTheme.X 引用**

在 `typeColor` 方法和 `paintComponent` 中：
- `ElementTheme.PRIMARY` → `theme().getPrimary()`
- `ElementTheme.SUCCESS` → `theme().getSuccess()`
- `ElementTheme.WARNING` → `theme().getWarning()`
- `ElementTheme.DANGER` → `theme().getDanger()`
- `ElementTheme.INFO` → `theme().getInfo()`
- `ElementTheme.BORDER_BASE` → `theme().getBorderBase()`
- `ElementTheme.TEXT_MAIN` → `theme().getTextMain()`
- `ElementTheme.TEXT_REGULAR` → `theme().getTextRegular()`
- `ElementTheme.FILL_BASE` → `theme().getFillBase()`
- `ElementTheme.RADIUS` → `theme().getRadiusBase()`
- `ElementTheme.FONT` → `theme().getFontBase()`
- `ElementTheme.lerp(Color.WHITE, ElementTheme.FILL_BASE, hover)` → `lerp(Color.WHITE, theme().getFillBase(), hover)`
- `ElementTheme.assertContrast(...)` → `assertContrast(...)`

- [ ] **Step 6: 删除 hoverStates 列表及相关初始化**

删除 `private final List<Float> hoverStates = new ArrayList<Float>();`
删除构造器中 `hoverStates.add(0f);` 行

- [ ] **Step 7: 编译并运行自检**

```bash
javac -encoding UTF-8 -d out src/org/swelement/ui/AstTimeline.java
java -ea -cp out org.swelement.ui.AstTimeline
```

Expected: `AstTimeline self-check OK`

---

### Task 3: 迁移 AstCard

**Files:**
- Modify: `src/org/swelement/ui/AstCard.java`

**说明：** AstCard 有 1 个 hover 动画，继承 AstDisplayComponent。

- [ ] **Step 1: 修改 import 和类声明**

将第 1-12 行替换为：

```java
package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.framework.AstDisplayComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
```

将 `public class AstCard extends JComponent` 改为 `public class AstCard extends AstDisplayComponent`

- [ ] **Step 2: 添加 initComponent，替换 hoverAnim**

删除字段：
```java
private final Animator hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); } },
    new Animator.Listener() { public void update(float v) { hover = v; repaint(); }});
private float hover;
```

添加 initComponent：
```java
@Override
protected void initComponent() {
    super.initComponent();
    anim.register("hover", 150, Easing::easeInOut);
}
```

- [ ] **Step 3: 替换构造器中的 hoverAnim 调用**

将 `hoverAnim.stop(); hoverAnim.go(hover, 1f);` 替换为：
```java
anim.go("hover", anim.getProgress("hover"), 1f);
```

将 `hoverAnim.stop(); hoverAnim.go(hover, 0f);` 替换为：
```java
anim.go("hover", anim.getProgress("hover"), 0f);
```

删除 `setOpaque(false);`

- [ ] **Step 4: 替换 setShadow 中的 hoverAnim 调用**

将 `hoverAnim.stop(); hover = 0f;` 替换为：
```java
anim.setProgress("hover", 0f);
```

需要确认 AnimationManager 是否有 setProgress 方法。**如果没有**，使用 `anim.go("hover", 0f, 0f);` 来重置。

- [ ] **Step 5: 替换 paintComponent 中的 hover 变量**

将所有 `hover` 变量替换为 `anim.getProgress("hover")`

- [ ] **Step 6: 替换所有 ElementTheme.X 引用**

- `ElementTheme.BORDER_BASE` → `theme().getBorderBase()`
- `ElementTheme.PRIMARY` → `theme().getPrimary()`
- `ElementTheme.TEXT_MAIN` → `theme().getTextMain()`
- `ElementTheme.RADIUS` → `theme().getRadiusBase()`
- `ElementTheme.FONT` → `theme().getFontBase()`
- `ElementTheme.lerp(...)` → `lerp(...)`
- `ElementTheme.assertContrast(...)` → `assertContrast(...)`

- [ ] **Step 7: 编译并运行自检**

```bash
javac -encoding UTF-8 -d out src/org/swelement/ui/AstCard.java
java -ea -cp out org.swelement.ui.AstCard
```

Expected: `AstCard self-check OK`

---

### Task 4: 迁移 AstCalendar

**Files:**
- Modify: `src/org/swelement/ui/AstCalendar.java`

**说明：** AstCalendar 有 1 个 fade 动画，有鼠标交互，继承 AstInteractiveComponent。

- [ ] **Step 1: 修改 import 和类声明**

将第 1-13 行替换为：

```java
package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.function.Consumer;
```

将 `public class AstCalendar extends JComponent` 改为 `public class AstCalendar extends AstInteractiveComponent`

- [ ] **Step 2: 添加 initComponent，替换 fadeAnim**

删除字段：
```java
private final Animator fadeAnim = new Animator(180, new Easing() { public float apply(float t) { return Easing.easeOut(t); }},
    new Animator.Listener() { public void update(float v) { alpha = v; repaint(); }});
```

添加 initComponent：
```java
@Override
protected void initComponent() {
    super.initComponent();
    anim.register("fade", 180, Easing::easeOut);
    anim.setProgress("fade", 1f);
}
```

删除 `private float alpha = 1f;` 字段（改为从 anim 读取）。

**注意：** 初始 alpha 应为 1f。在 initComponent 中注册后设置初始进度。

- [ ] **Step 3: 替换构造器**

删除 `setOpaque(false);`

- [ ] **Step 4: 替换所有 fadeAnim 调用**

搜索所有 `fadeAnim.stop(); fadeAnim.go(0f, 1f);` 替换为：
```java
anim.go("fade", 0f, 1f);
```

出现位置：
- `setSelected` 方法（约第 109 行）
- `prevMonth` 方法（约第 120 行）
- `nextMonth` 方法（约第 126 行）
- `prevYear` 方法（约第 170 行）
- `nextYear` 方法（约第 175 行）

- [ ] **Step 5: 替换 paintComponent 中的 alpha 变量**

将 `alpha` 替换为 `anim.getProgress("fade")`

位置：`g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(Math.max(0.15f, alpha)));`

- [ ] **Step 6: 替换所有 ElementTheme.X 引用**

- `ElementTheme.TEXT_MAIN` → `theme().getTextMain()`
- `ElementTheme.TEXT_REGULAR` → `theme().getTextRegular()`
- `ElementTheme.TEXT_PLACEHOLDER` → `theme().getTextPlaceholder()`
- `ElementTheme.PRIMARY` → `theme().getPrimary()`
- `ElementTheme.INFO` → `theme().getInfo()`
- `ElementTheme.FILL_BASE` → `theme().getFillBase()`
- `ElementTheme.FONT` → `theme().getFontBase()`
- `ElementTheme.assertContrast(...)` → `assertContrast(...)`

- [ ] **Step 7: 编译并运行自检**

```bash
javac -encoding UTF-8 -d out src/org/swelement/ui/AstCalendar.java
java -ea -cp out org.swelement.ui.AstCalendar
```

Expected: `AstCalendar self-check OK`

---

### Task 5: 迁移 AstCarousel

**Files:**
- Modify: `src/org/swelement/ui/AstCarousel.java`

**说明：** AstCarousel 有 2 个动画（slide + arrow），有鼠标交互，继承 AstInteractiveComponent。

- [ ] **Step 1: 修改 import 和类声明**

将第 1-12 行替换为：

```java
package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
```

将 `public class AstCarousel extends JComponent` 改为 `public class AstCarousel extends AstInteractiveComponent`

- [ ] **Step 2: 添加 initComponent，替换两个 Animator**

删除字段：
```java
private final Animator slideAnim;
private float arrowHover = 0f;
private final Animator arrowAnim = new Animator(180, new Easing() { public float apply(float t) { return Easing.easeInOut(t); }},
    new Animator.Listener() { public void update(float v) { arrowHover = v; repaint(); }});
```

添加 initComponent：
```java
@Override
protected void initComponent() {
    super.initComponent();
    anim.register("slide", 300, Easing::easeInOut);
    anim.register("arrow", 180, Easing::easeInOut);
}
```

删除 `private float offset;` 字段（改为从 anim 读取）。

- [ ] **Step 3: 修改构造器**

删除构造器中的 `slideAnim = new Animator(...)` 初始化行。
删除 `setOpaque(false);`

- [ ] **Step 4: 替换 goTo 中的 slideAnim 调用**

将：
```java
slideAnim.stop();
slideAnim.go(from, to);
```

替换为：
```java
anim.go("slide", from, to);
```

- [ ] **Step 5: 替换 paintComponent 中的 offset 和 arrowHover**

将 `offset` 替换为 `anim.getProgress("slide")`
将 `arrowHover` 替换为 `anim.getProgress("arrow")`

- [ ] **Step 6: 替换 mouse listener 中的 arrowAnim 调用**

将：
```java
arrowAnim.stop(); arrowAnim.go(arrowHover, 1f);
```

替换为：
```java
anim.go("arrow", anim.getProgress("arrow"), 1f);
```

将：
```java
arrowAnim.stop(); arrowAnim.go(arrowHover, 0f);
```

替换为：
```java
anim.go("arrow", anim.getProgress("arrow"), 0f);
```

- [ ] **Step 7: 替换所有 ElementTheme.X 引用**

- `ElementTheme.PRIMARY` → `theme().getPrimary()`
- `ElementTheme.BORDER_BASE` → `theme().getBorderBase()`
- `ElementTheme.FONT` → `theme().getFontBase()`

- [ ] **Step 8: 编译并运行自检**

```bash
javac -encoding UTF-8 -d out src/org/swelement/ui/AstCarousel.java
java -ea -cp out org.swelement.ui.AstCarousel
```

Expected: `AstCarousel self-check OK`

---

### Task 6: 最终验证 + 更新脚本

**Files:**
- Modify: `run-checks.bat`

- [ ] **Step 1: 更新 run-checks.bat**

在第 24 项 AstCollapse 之后，添加 5 项新自检：

第 25 项 AstLoading 之前已有... 等等，让我重新排序。

实际插入位置：在 AstCollapse（第 24 项）之后，AstLoading（第 25 项）之前？不对。

按字母顺序，5 个新组件应该插入的位置：
- AstCalendar → 在 AstButton 之后、AstCard 之前？不，按字母顺序：
  - AstBadge
  - AstButton
  - AstCalendar (新增)
  - AstCard (新增)
  - ...

但 run-checks.bat 当前顺序不是字母序，是按 Phase 顺序排列的。在最后（AstDialog 之后）添加新项即可。

在 `[29/29] Checking AstDialog...` 之后添加：

```
echo [30/34] Checking AstIcon...
"%JRUN%" -ea -cp out org.swelement.ui.AstIcon || set /a FAILED+=1
set /a TOTAL+=1

echo [31/34] Checking AstTimeline...
"%JRUN%" -ea -cp out org.swelement.ui.AstTimeline || set /a FAILED+=1
set /a TOTAL+=1

echo [32/34] Checking AstCard...
"%JRUN%" -ea -cp out org.swelement.ui.AstCard || set /a FAILED+=1
set /a TOTAL+=1

echo [33/34] Checking AstCalendar...
"%JRUN%" -ea -cp out org.swelement.ui.AstCalendar || set /a FAILED+=1
set /a TOTAL+=1

echo [34/34] Checking AstCarousel...
"%JRUN%" -ea -cp out org.swelement.ui.AstCarousel || set /a FAILED+=1
set /a TOTAL+=1
```

同时更新所有 `/29]` 为 `/34]`，以及底部的统计信息。

- [ ] **Step 2: 完整重新编译所有源文件**

编译所有 86 个源文件（排除 4 个有问题的 demo）。

- [ ] **Step 3: 运行全部 34 项自检**

Expected: `ALL 34 CHECKS PASSED`

---

## 风险提示

1. **AstTimeline 的 initComponent 时机问题**：`items` 在构造器参数中传入，`initComponent()` 在父类构造时调用（此时 items 尚未初始化）。因此动画注册必须在构造器中进行，不能用 initComponent。
2. **AnimationManager 的 setProgress 方法**：如果该方法不存在，需改用 `anim.go(name, value, value)` 方式重置进度。
3. **AstCalendar 的 alpha 初始值**：默认应为 1f（完全不透明），需在注册后设置。
