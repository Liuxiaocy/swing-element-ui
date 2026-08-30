# 图标对象化改造实施计划（Icon Class Migration）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AstButton 及所有可使用图标的组件的图标从 `String` 改为图标类（`javax.swing.Icon`、`ImageIcon`、本框架 `AstIcon`）。让 `AstIcon` 实现 `javax.swing.Icon` 接口，使按钮能统一接收 AstIcon / ImageIcon / 任意 Icon。改造后自检 51 项全部通过。

**Design doc:** `docs/superpowers/specs/2026-08-30-icon-class-migration-design.md`（用户已批准）

**Architecture:** 以 `AstIcon` 为核心（实现 `javax.swing.Icon`），AstButton 字段改为 `Icon`，绘制时对 AstIcon 用按钮前景色重画线条以实现「图标颜色跟随按钮文字色」，对 ImageIcon/原生 Icon 直接 `paintIcon`。AstTree 展开箭头改用 AstIcon.CARET_DOWN/RIGHT。PaintingHelper 移除 String 版 `drawIcon`、新增 Icon 版。三个 demo（ButtonDemo/BadgeDemo/FrameworkDemo）改用 AstIcon。

**Tech Stack:** JDK 8, Swing, `javax.swing.Icon`, `javax.swing.ImageIcon`, AstIcon, AstInteractiveComponent

---

## 前置条件

- JDK 8 路径：`C:\Program Files\Java\jdk1.8.0_311\bin\javac.exe`
- 编译输出目录：`out/`
- 编译命令（单个文件需要依赖已编译到 out/）：`javac -encoding UTF-8 -d out -cp out src/<path>.java`
- 运行命令：`java -ea -cp out <fully.qualified.ClassName>`
- 全量编译+自检：`build.bat`（编译所有 src 到 out/ 并运行全部自检）
- 全套 51 项检查：`run-checks.bat`

**说明：** 由于组件间存在依赖（AstIcon 被 AstButton/AstTree 引用），建议每完成一个文件后用 `build.bat` 全量重编验证，避免增量编译遗漏。修改 demo 文件只需编译（无自检），但仍需 `build.bat` 整体通过。

---

### Task 1：AstIcon 实现 javax.swing.Icon 接口

**Files:**
- Modify: `src/org/swelement/ui/AstIcon.java`

**说明：** 只增不改，不破坏现有 54 图标与旧 int 常量 API。

- [ ] **Step 1: 修改类声明,追加 implements Icon**

```java
// 原：public class AstIcon extends AstDisplayComponent {
// 改：public class AstIcon extends AstDisplayComponent implements Icon {
```
`Icon` 来自 `import javax.swing.*;`（第 6 行已覆盖）。`Component` 来自 `import java.awt.*;`（第 7 行已覆盖），无需新增 import。

- [ ] **Step 2: 新增 getSpinPhase getter（放在 isSpinRunning 之后,约第 161 行附近）**

```java
public float getSpinPhase() {
    return spinPhase;
}
```

- [ ] **Step 3: 新增三个 Icon 接口方法（放在 paintComponent 之前,约第 165 行附近）**

```java
@Override
public int getIconWidth() {
    return size;
}

@Override
public int getIconHeight() {
    return size;
}

@Override
public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.translate(x, y);
    AstIcon.paintIcon(g2, type, color, size, spinPhase);
    g2.dispose();
}
```
注意：实例方法 `paintIcon(Component,Graphics,int,int)` 与静态方法 `paintIcon(Graphics2D,Type,Color,int,float)` 参数不同，不构成重载冲突；调用内部用 `AstIcon.` 前缀显式定向静态方法，避免歧义。

- [ ] **Step 4: 在 selfCheck 中追加 Icon 接口验证（放在 "spin" 测试块之后、最终 System.out.println 之前）**

```java
// Icon 接口验证
AstIcon iface = new AstIcon(Type.CHECK, ElementTheme.PRIMARY, 24);
assert iface instanceof Icon : "AstIcon must implement javax.swing.Icon";
assert iface.getIconWidth() == 24 : "icon width 24, got " + iface.getIconWidth();
assert iface.getIconHeight() == 24 : "icon height 24, got " + iface.getIconHeight();
java.awt.image.BufferedImage iimg = new java.awt.image.BufferedImage(24, 24, java.awt.image.BufferedImage.TYPE_INT_ARGB);
Graphics2D ig = iimg.createGraphics();
try {
    iface.paintIcon(null, ig, 0, 0);
} finally {
    ig.dispose();
}
```

- [ ] **Step 5: 编译并验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstIcon.java
java -ea -cp out org.swelement.ui.AstIcon
```
Expected: `AstIcon self-check OK (54 icons)`

---

### Task 2：AstButton 改用 Icon 字段

**Files:**
- Modify: `src/org/swelement/ui/AstButton.java`

**说明：** 同包 `org.swelement.ui`，AstIcon 无需 import。`Icon`/`ImageIcon` 由 `import javax.swing.*;` 覆盖。颜色跟随仅在图标为 AstIcon 时用按钮前景色重画。

- [ ] **Step 1: 字段 String → Icon（第 25 行）**

```java
// 原：private String icon;
// 改：private Icon icon;
```

- [ ] **Step 2: setIcon 改签名，新增 getIcon（第 131-135 行）**

```java
public void setIcon(Icon icon) {
    this.icon = icon;
    revalidate();
    repaint();
}

public Icon getIcon() {
    return icon;
}
```

- [ ] **Step 3: 在绘制区新增私有辅助方法 paintButtonIcon（放在 paintComponent 之后、getPreferredSize 之前）**

```java
private void paintButtonIcon(Graphics2D g2, Icon ic, Color c, int x, int y) {
    if (ic instanceof AstIcon) {
        AstIcon ai = (AstIcon) ic;
        // 注意：必须先用临时 Graphics2D 平移到 (x,y)，否则静态绘制器会画在当前原点 (0,0)
        Graphics2D tx = (Graphics2D) g2.create();
        tx.translate(x, y);
        AstIcon.paintIcon(tx, ai.getTypeEnum(), c, ai.getSizeValue(), ai.getSpinPhase());
        tx.dispose();
    } else {
        ic.paintIcon(this, g2, x, y);
    }
}
```

- [ ] **Step 4: 修改 paintComponent 的图标宽度与绘制（第 386、406-418 行）**

位置 1 — 宽度计算（第 386 行）：
```java
// 原：int iconW = (!loading && icon != null) ? fm.stringWidth(icon) : 0;
// 改：int iconW = (!loading && icon != null) ? icon.getIconWidth() : 0;
```

位置 2 — 绘制块（第 406-418 行）替换为：
```java
if (!loading && icon != null) {
    int iy = (getHeight() - icon.getIconHeight()) / 2;
    if (iconPosition == ICON_LEFT) {
        paintButtonIcon(g2, icon, fg, Math.round(cursorX), iy);
        cursorX += iconW + gap;
        g2.drawString(displayText, cursorX, baseY);
    } else {
        g2.drawString(displayText, cursorX, baseY);
        cursorX += textW + gap;
        paintButtonIcon(g2, icon, fg, Math.round(cursorX), iy);
    }
} else {
    g2.drawString(displayText, cursorX, baseY);
}
```

- [ ] **Step 5: 修改 getPreferredSize 的图标宽度（第 430 行）**

```java
// 原：int iconW = (!loading && icon != null) ? fm.stringWidth(icon) : 0;
// 改：int iconW = (!loading && icon != null) ? icon.getIconWidth() : 0;
```

- [ ] **Step 6: 修改 selfCheck 中的图标测试（第 461-470 行）,替换为 AstIcon + 原生 ImageIcon + 渲染不抛异常**

```java
Color iconColor = theme().getPrimary();
AstButton ib = new AstButton("");
ib.setIcon(new AstIcon(AstIcon.Type.CHECK, iconColor, 16));
assert ib.getPreferredSize().width > 0 : "icon-only button should have positive width";
AstButton ib2 = new AstButton("确定");
ib2.setIcon(new AstIcon(AstIcon.Type.CHECK, iconColor, 16));
assert ib2.getPreferredSize().width > new AstButton("确定").getPreferredSize().width
        : "button with icon should be wider than text-only";
ib2.setIconPosition(AstButton.ICON_RIGHT);
assert ib2.getPreferredSize().width > new AstButton("确定").getPreferredSize().width
        : "icon-right button should also be wider";

// 原生 ImageIcon（Icon 接口路径）
java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
Graphics2D ig = bi.createGraphics();
ig.setColor(java.awt.Color.RED);
ig.fillRect(0, 0, 16, 16);
ig.dispose();
ImageIcon nativeIcon = new ImageIcon(bi);
AstButton nb = new AstButton("原生");
nb.setIcon(nativeIcon);
assert nb.getPreferredSize().width > 0 : "native image icon button positive width";

// 渲染均不抛异常（AstIcon 颜色跟随路径 + ImageIcon 路径）
java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(200, 60, java.awt.image.BufferedImage.TYPE_INT_ARGB);
Graphics2D g = img.createGraphics();
try {
    ib.paint(g);   // AstIcon 图标
    nb.paint(g);   // ImageIcon 图标
} finally {
    g.dispose();
}
```

- [ ] **Step 7: 编译并验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstIcon.java src/org/swelement/ui/AstButton.java
java -ea -cp out org.swelement.ui.AstButton
```
Expected: `Button self-check OK`

---

### Task 3：AstTree 展开箭头改用 AstIcon

**Files:**
- Modify: `src/org/swelement/ui/AstTree.java`

**说明：** AstTree 与 AstIcon 同属 `org.swelement.ui`，无需 import。`EXPANDER_W = 16` 在 AstIcon size 合法区间 [8,64] 内。`textColor` 在绘制块中已有作用域。

- [ ] **Step 1: 修改展开箭头绘制（第 279-288 行）**

```java
// 原：
// g2.setColor(textColor);
// g2.setFont(nodeFont.deriveFont(Font.PLAIN));
// FontMetrics fmE = g2.getFontMetrics();
// String icon = row.node.isExpanded() ? "▼" : "▶";
// int ix = expX + (EXPANDER_W - fmE.stringWidth(icon)) / 2;
// int iy = y + (rowH - fmE.getHeight()) / 2 + fmE.getAscent();
// g2.drawString(icon, ix, iy);
//
// 改：
AstIcon caret = row.node.isExpanded()
        ? new AstIcon(AstIcon.Type.CARET_DOWN, textColor, EXPANDER_W)
        : new AstIcon(AstIcon.Type.CARET_RIGHT, textColor, EXPANDER_W);
int ix = expX + (EXPANDER_W - caret.getIconWidth()) / 2;
int iy = y + (rowH - caret.getIconHeight()) / 2;
caret.paintIcon(this, g2, ix, iy);
```

- [ ] **Step 2: 编译并验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstIcon.java src/org/swelement/ui/AstTree.java
java -ea -cp out org.swelement.ui.AstTree
```
Expected: `AstTree self-check OK`

---

### Task 4：PaintingHelper 新增 drawIcon(Icon) 重载、移除 String 版

**Files:**
- Modify: `src/org/swelement/framework/util/PaintingHelper.java`

**说明：** 经 grep 确认 `drawIcon(Graphics2D,String,int,int,int,Color)` 仅被 FrameworkDemo 调用（本计划 Task 7 会一并改掉）。自检中仅注释引用，不实际调用，删改不影响自检。

- [ ] **Step 1: 替换 drawIcon String 版为 Icon 版（第 233-249 行）**

```java
public static void drawIcon(Graphics2D g2, Icon icon, int x, int y) {
    if (icon == null) return;
    icon.paintIcon(null, g2, x, y);
}
```

- [ ] **Step 2: 更新方法上方的 javadoc（第 222-232 行）**

```java
/**
 * 绘制一个 Icon（原生 ImageIcon / 任意 javax.swing.Icon / AstIcon）。
 *
 * @param g2    Graphics2D 对象
 * @param icon  图标对象
 * @param x     图标左上角 x 坐标
 * @param y     图标左上角 y 坐标
 */
```

- [ ] **Step 3: 更新自检中的对应注释（第 360-361 行）**

```java
// === drawIcon 基本验证 ===
// 通过编译保证方法存在，运行时需要 Graphics2D
```

- [ ] **Step 4: 编译并验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/framework/util/PaintingHelper.java
java -ea -cp out org.swelement.framework.util.PaintingHelper
```
Expected: `PaintingHelper self-check OK`

---

### Task 5：ButtonDemo 改用 AstIcon

**Files:**
- Modify: `src/org/swelement/demo/ButtonDemo.java`

**说明：** 仅需编译（无自检），但要确保不引用已删除的 `setIcon(String)`。

- [ ] **Step 1: 新增 import（第 3 行后）**

```java
import org.swelement.ui.AstIcon;
```

- [ ] **Step 2: 圆形图标数组 String → AstIcon.Type（第 45 行）**

```java
// 原：String[] icons = {"\u2713", "\u2717", "\u2605", "\u2699", "\u21bb", "\u2764"};
// 改：
AstIcon.Type[] iconTypes = {
    AstIcon.Type.CHECK, AstIcon.Type.CLOSE, AstIcon.Type.STAR_FILLED,
    AstIcon.Type.SETTING, AstIcon.Type.REFRESH, AstIcon.Type.DELETE_FILLED
};
```

- [ ] **Step 3: 修改循环（第 47-52 行）**

```java
// 原：for (int i = 0; i < icons.length; i++) {
//         AstButton b = new AstButton("", ctypes[i], false);
//         b.setIcon(icons[i]);
// 改：
for (int i = 0; i < iconTypes.length; i++) {
    AstButton b = new AstButton("", ctypes[i], false);
    b.setIcon(new AstIcon(iconTypes[i], Color.WHITE, 16));
```

- [ ] **Step 4: 修改左侧/右侧图标按钮（第 58、60 行）**

```java
// 原：il.setIcon("\u2713");
// 改：il.setIcon(new AstIcon(AstIcon.Type.CHECK, Color.WHITE, 16));
//
// 原：ir.setIcon("\u2192");
// 改：ir.setIcon(new AstIcon(AstIcon.Type.ARROW_RIGHT, Color.WHITE, 16));
```

- [ ] **Step 5: 编译验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstIcon.java src/org/swelement/ui/AstButton.java src/org/swelement/demo/ButtonDemo.java
```
Expected: 编译无错误（demo 无 main 自检，仅确认编译通过）。

---

### Task 6：BadgeDemo 图标改为 AstIcon.BELL

**Files:**
- Modify: `src/org/swelement/demo/BadgeDemo.java`

- [ ] **Step 1: 新增 import**

```java
import org.swelement.ui.AstIcon;
```

- [ ] **Step 2: 修改 iconBox 的绘制（第 88-92 行）**

```java
// 原：
// g2.setColor(Color.WHITE);
// g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
// FontMetrics fm = g2.getFontMetrics();
// String icon = "🔔";
// g2.drawString(icon, (getWidth() - fm.stringWidth(icon)) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
//
// 改：
AstIcon bell = new AstIcon(AstIcon.Type.BELL, Color.WHITE, 22);
int bx = (getWidth() - bell.getIconWidth()) / 2;
int by = (getHeight() - bell.getIconHeight()) / 2;
bell.paintIcon(this, g2, bx, by);
```
注：`this` 为匿名 JPanel（Component），匿名类可直接引用外部同名 AstIcon（需确保局部变量名不与外层冲突，用 `bell` 避开）。

- [ ] **Step 3: 编译验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstIcon.java src/org/swelement/demo/BadgeDemo.java
```
Expected: 编译无错误。

---

### Task 7：FrameworkDemo 改用 AstIcon（并移除对 String drawIcon 的调用）

**Files:**
- Modify: `src/org/swelement/demo/FrameworkDemo.java`

**说明：** 这是 `drawIcon(String)` 的唯一调用方，须一并替换，否则 Task 4 删除后编译失败。

- [ ] **Step 1: 新增 import（第 4 行后）**

```java
import org.swelement.ui.AstIcon;
```

- [ ] **Step 2: 修改图标数组与绘制循环（第 225-237 行）**

```java
// 原：
// String[] icons = {"\u2605", "\u2713", "\u2717", "\u2699", "\u2764"};
// Color[] colors = {t.getWarning(), t.getSuccess(), t.getDanger(), t.getInfo(), t.getDanger()};
// for (int i = 0; i < icons.length; i++) {
//     int cx = 25 + i * 36;
//     int cy = 28;
//     PaintingHelper.fillCircle(g2, cx, cy, 14);
//     g2.setColor(PaintingHelper.lighten(colors[i], 0.85f));
//     PaintingHelper.fillCircle(g2, cx, cy, 14);
//     PaintingHelper.drawIcon(g2, icons[i], cx, cy - 10, 16, colors[i]);
// }
//
// 改：
AstIcon.Type[] iconTypes = {
    AstIcon.Type.STAR_FILLED, AstIcon.Type.CHECK, AstIcon.Type.CLOSE,
    AstIcon.Type.SETTING, AstIcon.Type.CIRCLE_INFO
};
Color[] colors = {t.getWarning(), t.getSuccess(), t.getDanger(), t.getInfo(), t.getDanger()};
for (int i = 0; i < iconTypes.length; i++) {
    int cx = 25 + i * 36;
    int cy = 28;
    PaintingHelper.fillCircle(g2, cx, cy, 14);
    g2.setColor(PaintingHelper.lighten(colors[i], 0.85f));
    PaintingHelper.fillCircle(g2, cx, cy, 14);
    PaintingHelper.drawIcon(g2, new AstIcon(iconTypes[i], colors[i], 16), cx - 8, cy - 8);
}
```

- [ ] **Step 3: 编译验证**

```
javac -encoding UTF-8 -d out -cp out src/org/swelement/ui/AstIcon.java src/org/swelement/framework/util/PaintingHelper.java src/org/swelement/demo/FrameworkDemo.java
```
Expected: 编译无错误。

---

### Task 8：全量验证（删除 out、重编译、51 项通过）

**Files:**
- 无需改代码，仅验证。`run-checks.bat` 已含 AstButton(14)、PaintingHelper(7)、AstIcon(30)、AstTree(39)，无需改动。

- [ ] **Step 1: 删除陈旧 out/ 目录**

```
if exist out rmdir /s /q out
```
（在项目根目录执行）

- [ ] **Step 2: 全量编译 + 全部自检**

```
build.bat
```
Expected: `BUILD OK` 且所有自检 `self-check OK`（无 FAILED）。

- [ ] **Step 3: 运行自带示例的自检（AstIconDemo 等）**

```
build.bat
```
已包含 AstIconDemo self-check（第 276 行）。

- [ ] **Step 4: 运行全套 51 项检查**

```
run-checks.bat
```
Expected: `ALL 51 CHECKS PASSED`

- [ ] **Step 5: 以 powershell 直接运行验证（规避 cmd /c 限制）**

```
& '.\run-checks.bat'
```
Expected: `ALL 51 CHECKS PASSED`

---

## 明确不做（保持设计文档范围）

以下现有用例属于文本/省略号/导航字形，非按钮图标，本次不改（如需可作后续跟进）：

- `AstAlert.ICONS = {"\u221a", "!", "i", "\u00d7"}`（告警文本图标）
- `AstTransfer.toRightText/toLeftText`（→/← 作按钮文案）
- `AstPagination`（`‹`/`›` 翻页字形）、`AstDatePicker`（`‹`/`›` 导航字形）
- 各组件里的省略号 `"\u2026"`（纯文本）

---

## 注意事项

1. **AstIcon 静态绘制器复用**：AstButton 的颜色跟随通过 `AstIcon.paintIcon(g2, type, fg, size, spin)` 实现，`fg` 随 hover/active/disabled 变化，因此图标颜色自动跟随按钮文字色。
2. **AstIcon 自身 color 在按钮内被忽略**：凡 `icon instanceof AstIcon`，按钮都会用 `fg` 重画，故 ButtonDemo 中 AstIcon 的颜色占位符（如 `Color.WHITE`）会被覆盖，可任意指定。
3. **ImageIcon 走自身像素**：非 AstIcon 的 `Icon`（如 `ImageIcon`）保留自身像素，`icon.paintIcon(this, g2, x, y)` 直接绘制。
4. **实例 paintIcon 必须 translate**：AstIcon 的 `Icon.paintIcon(Component,Graphics,int,int)` 需先 `g2.translate(x, y)` 再调用静态绘制器，否则坐标偏移。
5. **iconY 仅在 icon 非空分支计算**：`getHeight() - icon.getIconHeight()` 要放到 `icon != null` 的分支内求值，避免 icon 为 null 时 NPE。
6. **编译顺序**：由于 AstButton/AstTree/FrameworkDemo 依赖 AstIcon，先编译 AstIcon 再编译依赖方；流程末尾用 `build.bat` 全量重编兜底。
