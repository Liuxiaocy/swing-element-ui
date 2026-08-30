# Phase 7：简单组件清零迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 5 个简单组件（AstCloseButton、AstContainer、AstSlider、AstRate、AstMenu）迁移到 AstAbstractComponent 新框架体系，完成后自检总数达到 41 项全部通过。

**Architecture:** 每个组件独立迁移，遵循统一模式：替换基类、替换 ElementTheme 为 theme()、替换 Animator 为 AnimationManager、selfCheck 改为实例方法。AstMenu 因包含内部 Entry 类和子菜单项，需额外处理。

**Tech Stack:** JDK 8, Swing, AstAbstractComponent 框架（AstInteractiveComponent / AstDisplayComponent / AstContainerComponent）, AnimationManager, ElementTheme（仅用于 assertContrast 静态工具方法）

---

## 前置条件

- JDK 8 路径：`C:\Program Files\Java\jdk1.8.0_311\bin\javac.exe`
- 编译输出目录：`out/`
- 编译命令：`javac -encoding UTF-8 -d out -cp out src/<path>.java`
- 运行命令：`java -ea -cp out <fully.qualified.ClassName>`

---

### Task 1：迁移 AstCloseButton

**Files:**
- Modify: `src/org/swelement/ui/AstCloseButton.java`

**迁移模式：** `JComponent` → `AstInteractiveComponent`

- [ ] **Step 1: 修改 import**

```java
// 替换 import
// 删除：import org.swelement.core.Animator;
// 删除：import org.swelement.core.ElementTheme;
// 添加：import org.swelement.framework.AstInteractiveComponent;
```

- [ ] **Step 2: 修改类声明和字段**

```java
// 原：public class AstCloseButton extends JComponent {
// 改：public class AstCloseButton extends AstInteractiveComponent {

// 原：private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
// 改：在构造函数中注册动画，删除 hoverAnim 字段
// 在构造函数第一行添加：
// anim.register("hover", 150, Easing::easeInOut);
```

- [ ] **Step 3: 替换所有 hoverAnim 调用为 anim 调用**

```
hoverAnim.go(hover, 1f)  →  anim.go("hover", 1f)
hoverAnim.go(hover, 0f)  →  anim.go("hover", 0f)
```

- [ ] **Step 4: 替换 paintComponent 中的 hover 字段**

```java
// 在 paintComponent 开始处获取 hover 值：
// float hover = anim.getProgress("hover");
// 删除 private float hover; 字段
```

- [ ] **Step 5: 替换 ElementTheme 引用**

```
ElementTheme.TEXT_PLACEHOLDER  →  theme().getTextPlaceholder()
ElementTheme.lerp(a, b, t)     →  lerp(a, b, t)
```

- [ ] **Step 6: 替换 paintComponent 中的 Graphics2D 创建**

```java
// 原：Graphics2D g2 = (Graphics2D) g.create();
//     g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
// 改：Graphics2D g2 = createGraphics(g);
```

- [ ] **Step 7: 删除 setOpaque(false) 调用**（AstInteractiveComponent 构造函数已设置）

- [ ] **Step 8: 将 selfCheck 从静态方法改为实例方法**

```java
// 原：static void selfCheck() { ... }
// 改：
// @Override
// protected void selfCheck() { ... }

// 原：ElementTheme.assertContrast(...)
// 改：assertContrast(...)

// 原 main:
// public static void main(String[] args) { selfCheck(); }
// 改：
// public static void main(String[] args) {
//     new AstCloseButton().selfCheck();
// }
```

- [ ] **Step 9: 编译并验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstCloseButton.java
java -ea -cp out org.swelement.ui.AstCloseButton
```
Expected: `AstCloseButton self-check OK`

---

### Task 2：迁移 AstContainer

**Files:**
- Modify: `src/org/swelement/ui/AstContainer.java`

**迁移模式：** `JPanel` → `AstContainerComponent`

- [ ] **Step 1: 修改 import**

```java
// 删除：import org.swelement.core.ElementTheme;
// 添加：import org.swelement.framework.AstContainerComponent;
```

- [ ] **Step 2: 修改类声明**

```java
// 原：public class AstContainer extends JPanel {
// 改：public class AstContainer extends AstContainerComponent {
```

- [ ] **Step 3: 替换 ElementTheme 引用**

```
ElementTheme.FILL_BLANK    →  theme().getFillBlank()
ElementTheme.FILL_BASE     →  theme().getFillBase()
ElementTheme.BORDER_BASE   →  theme().getBorderBase()
```

- [ ] **Step 4: 删除 setOpaque(true) 中的 setOpaque**（AstContainerComponent 默认 opaque true）
  - 注意：内部的 north/south/west/center/centerStack 是普通 JPanel，它们的 `setOpaque(false)` 保留不变

- [ ] **Step 5: 将 selfCheck 从静态方法改为实例方法**

```java
// 原：static void selfCheck() { ... }
// 改：
// @Override
// protected void selfCheck() { ... }

// 原 main:
// public static void main(String[] args) { selfCheck(); }
// 改：
// public static void main(String[] args) {
//     new AstContainer(HORIZONTAL).selfCheck();
// }
```

- [ ] **Step 6: 编译并验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstContainer.java
java -ea -cp out org.swelement.ui.AstContainer
```
Expected: `AstContainer self-check OK`

---

### Task 3：迁移 AstSlider

**Files:**
- Modify: `src/org/swelement/ui/AstSlider.java`

**迁移模式：** `JComponent` → `AstInteractiveComponent`
**特殊：** 当前没有 selfCheck，需要新增

- [ ] **Step 1: 修改 import**

```java
// 删除：import org.swelement.core.Animator;
// 删除：import org.swelement.core.ElementTheme;
// 添加：import org.swelement.framework.AstInteractiveComponent;
```

- [ ] **Step 2: 修改类声明和字段**

```java
// 原：public class AstSlider extends JComponent {
// 改：public class AstSlider extends AstInteractiveComponent {

// 删除 thumbAnim 和 hoverAnim 两个 Animator 字段
// 删除 thumbX 和 hover 字段（改为从 anim 获取）
// 保留 lastTarget 字段
```

- [ ] **Step 3: 在构造函数中注册动画**

```java
// 在构造函数第一行添加：
// anim.register("thumb", 200, Easing::easeOut);
// anim.register("hover", 150, Easing::easeInOut);
```

- [ ] **Step 4: 替换动画调用**

```
thumbAnim.go(thumbX, thumbTarget)  →  anim.go("thumb", thumbTarget)
hoverAnim.go(hover, 1f)            →  anim.go("hover", 1f)
hoverAnim.go(hover, 0f)            →  anim.go("hover", 0f)
```

- [ ] **Step 5: 在 paintComponent 中获取动画进度**

```java
// 在 paintComponent 开始处添加：
// float thumbX = anim.getProgress("thumb");
// float hover = anim.getProgress("hover");
```

- [ ] **Step 6: 替换 ElementTheme 引用**

```
ElementTheme.PRIMARY  →  theme().getPrimary()
```

- [ ] **Step 7: 替换 paintComponent 中的 Graphics2D 创建**

```java
// 原：Graphics2D g2 = (Graphics2D) g.create();
//     g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
// 改：Graphics2D g2 = createGraphics(g);
```

- [ ] **Step 8: 删除 setOpaque(false) 调用**

- [ ] **Step 9: 新增 selfCheck 实例方法**

```java
@Override
protected void selfCheck() {
    // 1. 构造函数边界测试
    AstSlider s0 = new AstSlider(0, 100, 50);
    assert s0.getValue() == 50 : "initial value 50";

    // 2. setValue 边界
    s0.setValue(0);
    assert s0.getValue() == 0 : "min value 0";
    s0.setValue(100);
    assert s0.getValue() == 100 : "max value 100";
    s0.setValue(-10);
    assert s0.getValue() == 0 : "clamp to min";
    s0.setValue(200);
    assert s0.getValue() == 100 : "clamp to max";

    // 3. ChangeListener
    final int[] fired = {0};
    final int[] lastVal = {-1};
    s0.addChangeListener(e -> { fired[0]++; lastVal[0] = s0.getValue(); });
    s0.setValue(30);
    assert fired[0] == 1 : "listener fired once, got " + fired[0];
    assert lastVal[0] == 30 : "last value 30, got " + lastVal[0];

    // 4. 相同值不触发
    s0.setValue(30);
    assert fired[0] == 1 : "same value should not fire again";

    // 5. removeChangeListener
    s0.removeChangeListener(e -> {}); // 移除不存在的不报错
    assert true;

    // 6. 首选尺寸
    Dimension pd = s0.getPreferredSize();
    assert pd.width == 240 && pd.height == 32 : "preferred size 240x32, got " + pd;

    // 7. 渲染不抛异常
    java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(240, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    try { s0.paint(g); } finally { g.dispose(); }

    // 8. 禁用态渲染不抛异常
    s0.setEnabled(false);
    java.awt.image.BufferedImage img2 = new java.awt.image.BufferedImage(240, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = img2.createGraphics();
    try { s0.paint(g2); } finally { g2.dispose(); }
    s0.setEnabled(true);

    // 9. 对比度断言
    assertContrast(theme().getPrimary(), Color.WHITE, "AstSlider primary track on white");

    System.out.println("AstSlider self-check OK");
}
```

- [ ] **Step 10: 添加 main 方法**

```java
public static void main(String[] args) {
    new AstSlider(0, 100, 50).selfCheck();
}
```

- [ ] **Step 11: 编译并验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstSlider.java
java -ea -cp out org.swelement.ui.AstSlider
```
Expected: `AstSlider self-check OK`

---

### Task 4：迁移 AstRate

**Files:**
- Modify: `src/org/swelement/ui/AstRate.java`

**迁移模式：** `JComponent` → `AstInteractiveComponent`

- [ ] **Step 1: 修改 import**

```java
// 删除：import org.swelement.core.Animator;
// 删除：import org.swelement.core.ElementTheme;
// 添加：import org.swelement.framework.AstInteractiveComponent;
```

- [ ] **Step 2: 修改类声明和字段**

```java
// 原：public class AstRate extends JComponent {
// 改：public class AstRate extends AstInteractiveComponent {

// 删除 hoverAnim 字段
// 删除 hoverScale 字段（改为从 anim 获取）
```

- [ ] **Step 3: 在构造函数中注册动画**

```java
// 在构造函数中 setOpaque 之前添加：
// anim.register("hover", 150, Easing::easeOut);
```

- [ ] **Step 4: 替换动画调用**

```
hoverAnim.stop(); hoverAnim.go(hoverScale, 1.12f)  →  anim.stop("hover"); anim.go("hover", 1.12f)
hoverAnim.stop(); hoverAnim.go(hoverScale, 1f)     →  anim.stop("hover"); anim.go("hover", 1f)
```

- [ ] **Step 5: 在 paintComponent 中获取动画进度**

```java
// 在 paintComponent 开始处添加：
// float hoverScale = anim.getProgress("hover");
// 注意：hoverScale 默认 0，但语义上是 1f（未hover时正常大小）
// 需要调整：anim.register("hover", 150, Easing::easeOut) 初始值 0
// 然后 hoverScale 的计算方式改为：1f + 0.12f * anim.getProgress("hover")
// 这样 0 → 1.0, 1 → 1.12
```

修正：AnimationManager 的进度是 0→1，所以需要重新映射：
```java
float hoverAnim = anim.getProgress("hover");
float scale = 1f + 0.12f * hoverAnim; // 未hover=1.0, hover=1.12
// 替换 isHovered 判断中的 hoverScale 为 scale
```

- [ ] **Step 6: 替换 ElementTheme 引用**

```
ElementTheme.WARNING       →  theme().getWarning()
ElementTheme.BORDER_BASE   →  theme().getBorderBase()
```

- [ ] **Step 7: 替换 paintComponent 中的 Graphics2D 创建**

```java
// 原：Graphics2D g2 = (Graphics2D) g.create();
//     g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
// 改：Graphics2D g2 = createGraphics(g);
```

- [ ] **Step 8: 删除 setOpaque(false) 调用**

- [ ] **Step 9: 将 selfCheck 从静态方法改为实例方法**

```java
// 原：static void selfCheck() { ... }
// 改：
// @Override
// protected void selfCheck() { ... }

// 原 main:
// public static void main(String[] args) { selfCheck(); }
// 改：
// public static void main(String[] args) {
//     new AstRate().selfCheck();
// }
```

- [ ] **Step 10: 编译并验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstRate.java
java -ea -cp out org.swelement.ui.AstRate
```
Expected: `AstRate self-check OK`

---

### Task 5：迁移 AstMenu

**Files:**
- Modify: `src/org/swelement/ui/AstMenu.java`

**迁移模式：** `JComponent` → `AstInteractiveComponent`
**特殊：** 内部 Entry 类、子菜单项、多个动画、AnimatedPopup

- [ ] **Step 1: 修改 import**

```java
// 删除：import org.swelement.core.Animator;
// 删除：import org.swelement.core.ElementTheme;
// 保留：import org.swelement.core.AnimatedPopup;（弹出层暂不迁移）
// 添加：import org.swelement.framework.AstInteractiveComponent;
```

- [ ] **Step 2: 修改类声明**

```java
// 原：public class AstMenu extends JComponent {
// 改：public class AstMenu extends AstInteractiveComponent {
```

- [ ] **Step 3: 修改 Entry 内部类 - 移除 Animator 字段**

Entry 类不再持有自己的 Animator，改为用索引从外部 anim 获取进度。
但 Entry.hover 字段仍需保留（用于当前值），动画驱动方式改为 AstMenu 统一管理。

更简单的方案：Entry 保留 hover 字段，动画通过 AstMenu 的 AnimationManager 按索引注册。

```java
// 在 Entry 中删除 hoverAnim 字段，保留 hover 字段
// Entry 的 hover 动画由 AstMenu 统一通过 anim.register("hover_" + index, ...) 管理
```

- [ ] **Step 4: 修改 AstMenu 顶部的动画字段**

```java
// 删除：indXAnim, indWAnim 两个 Animator 字段
// 删除：indX, indW 字段（改为从 anim 获取）
// 保留：subPopup, subList（弹出层暂不迁移）
```

- [ ] **Step 5: 在构造函数中注册 indicator 动画**

```java
// 在构造函数开始添加：
// anim.register("indX", 250, Easing::easeInOut);
// anim.register("indW", 250, Easing::easeInOut);
```

- [ ] **Step 6: 替换 indicator 动画调用**

```
indXAnim.go(indX, x)  →  anim.go("indX", x)
indWAnim.go(indW, w)  →  anim.go("indW", w)
```

- [ ] **Step 7: 管理 Entry 的 hover 动画**

Entry hover 动画动态注册。当 addMenuItem / addSubMenu 时注册对应的动画。

```java
// 在 addMenuItem 和 addSubMenu 中，entries.add 后添加：
// anim.register("hover_" + (entries.size() - 1), 150, Easing::easeInOut);
```

替换 mouseMoved 和 mouseExited 中的 hoverAnim 调用：
```
en.hoverAnim.go(en.hover, over ? 1f : 0f)
→
int idx = entries.indexOf(en);
anim.go("hover_" + idx, over ? 1f : 0f);
```

- [ ] **Step 8: 在 paintComponent 中获取动画进度**

```java
// 在 paintComponent 开始处获取：
// float indX = anim.getProgress("indX");
// float indW = anim.getProgress("indW");

// 循环中获取每个 entry 的 hover：
// float hover = anim.getProgress("hover_" + i);
// 用 hover 替换 en.hover
```

- [ ] **Step 9: 替换 ElementTheme 引用**

```
ElementTheme.PRIMARY       →  theme().getPrimary()
ElementTheme.TEXT_REGULAR  →  theme().getTextRegular()
ElementTheme.FONT          →  getFont()  // 用组件自身字体
ElementTheme.lerp(a, b, t) →  lerp(a, b, t)
```

- [ ] **Step 10: 替换 entryWidth 中的字体引用**

```java
// 原：return 24 + getFontMetrics(ElementTheme.FONT).stringWidth(en.label);
// 改：return 24 + getFontMetrics(getFont()).stringWidth(en.label);
```

- [ ] **Step 11: 子菜单项迁移**

子菜单项当前是匿名 JLabel 内部类，每个有自己的 Animator 和 ElementTheme 引用。
提取为内部静态类 SubMenuItem extends AstInteractiveComponent 太重了。

简化方案：子菜单项保留 JLabel，但替换 ElementTheme 引用。由于子菜单在 AnimatedPopup 内部，且 AnimatedPopup 暂不迁移，子菜单项可以暂时保留 Animator（因为它不在主组件树中，无法共享 AstMenu 的 AnimationManager）。

```java
// 子菜单项中的 ElementTheme 替换：
// ElementTheme.FONT → 用 new JLabel 的默认字体
// ElementTheme.TEXT_REGULAR → 保持 0x303133（直接写颜色值）
// ElementTheme.lerp → 暂时保留 ElementTheme.lerp 静态调用
```

实际上更好的做法：子菜单项也改成 AstInteractiveComponent 内部类。让它继承 AstInteractiveComponent，这样可以有自己的 theme() 和 anim。

```java
// 提取为内部类：
// private class SubMenuItem extends AstInteractiveComponent {
//     private final String text;
//     private final Runnable action;
//     SubMenuItem(String text, Runnable action) {
//         this.text = text;
//         this.action = action;
//         anim.register("hover", 150, Easing::easeInOut);
//         setPreferredSize(new Dimension(140, 32));
//         setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//         addMouseListener(new MouseAdapter() {
//             public void mouseEntered(MouseEvent e) { if (isEnabled()) anim.go("hover", 1f); }
//             public void mouseExited(MouseEvent e) { anim.go("hover", 0f); }
//             public void mousePressed(MouseEvent e) {
//                 if (!isEnabled()) return;
//                 subPopup.setVisible(false);
//                 if (action != null) action.run();
//             }
//         });
//     }
//     @Override protected void paintComponent(Graphics g) {
//         Graphics2D g2 = createGraphics(g);
//         float hover = anim.getProgress("hover");
//         if (hover > 0) {
//             g2.setColor(lerp(Color.WHITE, new Color(0xECF5FF), hover));
//             g2.fillRect(0, 0, getWidth(), getHeight());
//         }
//         g2.setFont(getFont());
//         g2.setColor(isEnabled() ? theme().getTextRegular() : new Color(0xC0C4CC));
//         FontMetrics fm = g2.getFontMetrics();
//         g2.drawString(text, 16, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
//         g2.dispose();
//     }
//     @Override protected void selfCheck() { }
// }
```

- [ ] **Step 12: 替换 paintComponent 中的 Graphics2D 创建**

```java
// 原：Graphics2D g2 = (Graphics2D) g.create();
//     g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
// 改：Graphics2D g2 = createGraphics(g);
```

- [ ] **Step 13: 删除 setOpaque(false) 调用**

- [ ] **Step 14: 新增 selfCheck 实例方法**

```java
@Override
protected void selfCheck() {
    // 1. 基础构造
    AstMenu menu = new AstMenu();
    assert menu.getPreferredSize().height == 40 : "default height 40";

    // 2. 添加菜单项
    final boolean[] clicked = {false};
    menu.addMenuItem("File", () -> clicked[0] = true);
    menu.addMenuItem("Edit", null);

    // 3. setActive
    menu.setActive(0);
    assert true; // 不抛异常即通过

    // 4. 渲染不抛异常
    menu.setSize(520, 40);
    java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(520, 40, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    try { menu.paint(g); } finally { g.dispose(); }

    // 5. 子菜单
    String[] subLabels = {"New", "Open"};
    Runnable[] subActions = {null, null};
    menu.addSubMenu("Help", subLabels, subActions);

    // 6. 禁用态渲染
    menu.setEnabled(false);
    java.awt.image.BufferedImage img2 = new java.awt.image.BufferedImage(520, 40, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = img2.createGraphics();
    try { menu.paint(g2); } finally { g2.dispose(); }
    menu.setEnabled(true);

    // 7. 对比度断言
    assertContrast(theme().getPrimary(), Color.WHITE, "AstMenu active indicator on white");
    assertContrast(new Color(0x303133), Color.WHITE, "AstMenu text on white");

    System.out.println("AstMenu self-check OK");
}
```

- [ ] **Step 15: 添加 main 方法**

```java
public static void main(String[] args) {
    new AstMenu().selfCheck();
}
```

- [ ] **Step 16: 编译并验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstMenu.java
java -ea -cp out org.swelement.ui.AstMenu
```
Expected: `AstMenu self-check OK`

---

### Task 6：最终验证 + 更新脚本

**Files:**
- Modify: `run-checks.bat`

- [ ] **Step 1: 更新 run-checks.bat，添加 5 个新组件**

在现有 36 项之后添加：
```
echo [37/41] Checking AstCloseButton...
"%JRUN%" -ea -cp out org.swelement.ui.AstCloseButton || set /a FAILED+=1
set /a TOTAL+=1

echo [38/41] Checking AstContainer...
"%JRUN%" -ea -cp out org.swelement.ui.AstContainer || set /a FAILED+=1
set /a TOTAL+=1

echo [39/41] Checking AstSlider...
"%JRUN%" -ea -cp out org.swelement.ui.AstSlider || set /a FAILED+=1
set /a TOTAL+=1

echo [40/41] Checking AstRate...
"%JRUN%" -ea -cp out org.swelement.ui.AstRate || set /a FAILED+=1
set /a TOTAL+=1

echo [41/41] Checking AstMenu...
"%JRUN%" -ea -cp out org.swelement.ui.AstMenu || set /a FAILED+=1
set /a TOTAL+=1
```

同时把所有 `[N/36]` 改为 `[N/41]`。

- [ ] **Step 2: 运行全部检查**

```
run-checks.bat
```
Expected: `ALL 41 CHECKS PASSED`

---

## 注意事项

1. **AnimationManager 注册时机**：必须在构造函数中尽早注册（第一行），因为 `anim` 在 `AstAbstractComponent` 构造函数中初始化。
2. **动画进度初始值**：AnimationManager 的 `getProgress()` 在未 go() 时返回初始值 0。如果组件需要不同初始值（如 hover 默认 0 是正确的，thumb 需要初始化为目标位置），需在 paintComponent 中处理。
3. **内部类 selfCheck**：继承 AstAbstractComponent 的内部类也需要实现 `selfCheck()` 抽象方法，提供空实现即可。
4. **AstSlider 的 thumb 动画**：thumbX 初始为 -1，表示未初始化。首次 paint 时设置为目标位置。迁移后需要在 paintComponent 中用 `anim.getProgress("thumb")`，如果为 0 且 thumbX < 0，则设置初始位置。
