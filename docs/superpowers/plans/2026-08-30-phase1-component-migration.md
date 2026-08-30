# Phase 1: 简单展示组件迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 6 个简单展示/容器组件从旧 ElementTheme + 手动 Animator 模式迁移到新框架基类体系

**Architecture:** 原地迁移：每个组件修改继承链为新框架基类（AstDisplayComponent/AstContainerComponent），替换 ElementTheme 静态调用为 theme() 方法调用，替换手动 Animator 为 AnimationManager 命名动画，迁移自检到 SelfCheckBase 工具方法。

**Tech Stack:** Java Swing, JDK 8, 零外部依赖

---

## 前置任务：为 AstAbstractComponent 添加自检工具方法

**Files:**
- Modify: `src/org/swelement/framework/AstAbstractComponent.java`

现有 `AstAbstractComponent` 缺少 `assertContrast`、`luminance`、`contrastRatio`、`pickTextColorForBg` 方法。迁移后的组件需要这些方法做自检，需先补充。

- [ ] **Step 1: 在 AstAbstractComponent 的自检区域前添加工具方法**

在 `radius()` 方法之后、`// ==================== 生命周期 ====================` 之前插入：

```java
    // ==================== 自检工具 ====================

    /**
     * 计算 WCAG 相对亮度。
     *
     * @param c 颜色
     * @return 相对亮度值 [0, 1]
     */
    protected float luminance(Color c) {
        return 0.2126f * srgb(c.getRed())
                + 0.7152f * srgb(c.getGreen())
                + 0.0722f * srgb(c.getBlue());
    }

    /**
     * 计算两个颜色的对比度。
     *
     * @param a 颜色 a
     * @param b 颜色 b
     * @return 对比度值
     */
    protected float contrastRatio(Color a, Color b) {
        float l1 = luminance(a);
        float l2 = luminance(b);
        float lighter = Math.max(l1, l2);
        float darker = Math.min(l1, l2);
        return (lighter + 0.05f) / (darker + 0.05f);
    }

    /**
     * 对比度断言（WCAG AA 4.5:1）。
     *
     * @param fg    前景色
     * @param bg    背景色
     * @param where 位置描述
     */
    protected void assertContrast(Color fg, Color bg, String where) {
        assertContrast(fg, bg, where, 4.5f);
    }

    /**
     * 指定最小比例的对比度断言。
     *
     * @param fg       前景色
     * @param bg       背景色
     * @param where    位置描述
     * @param minRatio 最小对比度比例
     */
    protected void assertContrast(Color fg, Color bg, String where, float minRatio) {
        float ratio = contrastRatio(fg, bg);
        assert ratio >= minRatio : "[CONTRAST FAIL " + where + "] ratio="
                + String.format("%.2f", ratio) + " (need >= " + String.format("%.2f", minRatio) + ")"
                + " fg=RGB(" + fg.getRed() + "," + fg.getGreen() + "," + fg.getBlue() + ")"
                + " bg=RGB(" + bg.getRed() + "," + bg.getGreen() + "," + bg.getBlue() + ")";
    }

    /**
     * 根据背景色自动选择文字颜色（白/黑/深灰）。
     *
     * @param bg 背景色
     * @return 最合适的文字色
     */
    protected Color pickTextColorForBg(Color bg) {
        float lumBg = luminance(bg);
        float rW = contrastRatio(Color.WHITE, bg);
        float rB = contrastRatio(Color.BLACK, bg);
        if (rW >= 4.5f && rW >= rB) return Color.WHITE;
        if (rB >= 4.5f) return Color.BLACK;
        return rW >= rB ? Color.WHITE : Color.BLACK;
    }

    /**
     * sRGB 线性化转换。
     *
     * @param v 8 位颜色分量值 [0, 255]
     * @return 线性化后的分量值
     */
    private static float srgb(int v) {
        float vv = v / 255f;
        return vv <= 0.03928f ? vv / 12.92f : (float) Math.pow((vv + 0.055) / 1.055, 2.4);
    }
```

- [ ] **Step 2: 编译验证**

```bash
javac -encoding UTF-8 --release 8 -d out src/org/swelement/framework/AstAbstractComponent.java src/org/swelement/core/AnimationManager.java src/org/swelement/core/theme/Theme.java src/org/swelement/core/theme/ThemeManager.java src/org/swelement/core/theme/ElementLightTheme.java
```
Expected: 编译成功（可能有 deprecation 警告，忽略）

---

### Task 1: 迁移 AstProgress

**Files:**
- Modify: `src/org/swelement/ui/AstProgress.java`（73 行）

AstProgress 是最简单的迁移目标：无自检（需补充）、1 个 Animator、3 处 ElementTheme 引用。

- [ ] **Step 1: 修改类声明和 import**

将第 3-5 行的 import 替换：
```java
// 删除这些 import:
import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
// 替换为:
import org.swelement.core.Easing;
import org.swelement.framework.AstDisplayComponent;
import org.swelement.core.theme.ThemeManager;
```

将第 13 行类声明改为：
```java
public class AstProgress extends AstDisplayComponent {
```

- [ ] **Step 2: 修改字段和构造函数**

删除第 14 行的手动 Animator 字段：
```java
// 删除:
private final Animator fillAnim = new Animator(300, Easing::easeOut, v -> { shown = v; repaint(); });
```
替换为（保留 shown 字段，在 initComponent 中注册动画）：
```java
// shown 字段保留，动画进度直接通过 anim.getProgress 获取
```

修改构造函数（第 20-25 行），在构造函数中注册动画并移除手动 setOpaque：
```java
public AstProgress() {
    setPreferredSize(new Dimension(320, 20));
}

public AstProgress(int initialValue) {
    this();
    setValue(initialValue);
}
```

添加 initComponent 方法（注册命名动画）：
```java
@Override
protected void initComponent() {
    super.initComponent();
    anim.register("fill", 300, Easing::easeOut);
}
```

- [ ] **Step 3: 修改 setValue 方法**

将第 35 行的 `fillAnim.go(shown, value / 100f);` 替换为：
```java
anim.go("fill", shown, value / 100f);
```

添加 shown 进度获取（anim 回调自动更新 shown，需在 register 时处理）：
实际上 AnimationManager 的 go 方法会自动触发 repaint，不需要手动回调更新 shown。改为在 paintComponent 中直接使用 `anim.getProgress("fill")` 获取进度。

删除 `shown` 字段，在 paintComponent 中直接使用 `float shown = anim.getProgress("fill");`。

但 setValue 需要记住 value。所以保留 `value` 字段，删除 `shown` 字段：
```java
private int value;
private boolean showText = true;
```

setValue 方法改为：
```java
public void setValue(int value) {
    this.value = Math.max(0, Math.min(100, value));
    anim.go("fill", anim.getProgress("fill"), value / 100f);
    fireStateChanged();
}
```

- [ ] **Step 4: 修改 paintComponent**

将第 50-70 行替换为：
```java
@Override
protected void paintComponent(Graphics g) {
    Graphics2D g2 = createGraphics(g);
    float shown = anim.getProgress("fill");
    int textW = showText ? 46 : 0;
    int trackW = getWidth() - textW;
    int y = (getHeight() - 6) / 2;
    g2.setColor(new Color(0xEBEEF5));
    g2.fillRoundRect(0, y, trackW, 6, 6, 6);
    int fillW = Math.round(trackW * shown);
    g2.setColor(theme().getPrimary());
    g2.fillRoundRect(0, y, fillW, 6, 6, 6);
    if (showText) {
        g2.setColor(new Color(0x606266));
        Font f = theme().getFontBase().deriveFont(12f);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(value + "%", trackW + 6, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
    }
    g2.dispose();
}
```

- [ ] **Step 5: 补充 selfCheck 方法**

在类末尾添加：
```java
@Override
protected void selfCheck() {
    AstProgress p = new AstProgress();
    assert p.getPreferredSize().width == 320 : "default width";
    assert p.getPreferredSize().height == 20 : "default height";
    p.setValue(50);
    assert p.getValue() == 50 : "setValue";
    p.setValue(200);
    assert p.getValue() == 100 : "setValue clamps to 100";
    p.setValue(-10);
    assert p.getValue() == 0 : "setValue clamps to 0";
    p.setShowText(false);
    assert !p.isShowText() : "setShowText false";
    p.setShowText(true);
    assert p.isShowText() : "setShowText true";
    // 对比度：文字色与背景色
    assertContrast(new Color(0x606266), new Color(0xEBEEF5), "progress text on track");
    System.out.println("AstProgress self-check OK");
}

public static void main(String[] args) {
    AstProgress p = new AstProgress();
    p.selfCheck();
}
```

需要添加 getValue、setShowText、isShowText 公开方法（如果不存在）：
```java
public int getValue() { return value; }
public void setShowText(boolean showText) { this.showText = showText; repaint(); }
public boolean isShowText() { return showText; }
```

- [ ] **Step 6: 编译和自检验证**

```bash
javac -encoding UTF-8 --release 8 -d out src/org/swelement/ui/AstProgress.java src/org/swelement/framework/AstDisplayComponent.java src/org/swelement/framework/AstAbstractComponent.java src/org/swelement/core/AnimationManager.java src/org/swelement/core/Easing.java src/org/swelement/core/theme/Theme.java src/org/swelement/core/theme/ThemeManager.java src/org/swelement/core/theme/ElementLightTheme.java
java -ea -cp out org.swelement.ui.AstProgress
```
Expected: 输出 "AstProgress self-check OK"

---

### Task 2: 迁移 AstDivider

**Files:**
- Modify: `src/org/swelement/ui/AstDivider.java`（190 行）

AstDivider 无 Animator，纯绘制组件。有自检方法。

- [ ] **Step 1: 修改类声明和 import**

删除 `import org.swelement.core.ElementTheme;`，添加：
```java
import org.swelement.framework.AstDisplayComponent;
```

将第 21 行类声明改为：
```java
public class AstDivider extends AstDisplayComponent {
```

- [ ] **Step 2: 修改字段默认值**

第 29-30 行，将 ElementTheme 静态引用改为 null（在 initComponent 中从主题获取）：
```java
private Color lineColor;
private Color textColor;
```

- [ ] **Step 3: 添加 initComponent 初始化颜色**

```java
@Override
protected void initComponent() {
    super.initComponent();
    lineColor = theme().getBorderBase();
    textColor = theme().getTextRegular();
}
```

- [ ] **Step 4: 修改构造函数**

删除构造函数中的 `setOpaque(false)`（基类已处理）。保留参数校验逻辑。

- [ ] **Step 5: 修改 paintComponent**

第 77 行的手动 Graphics2D 配置替换为 `createGraphics(g)`：
```java
Graphics2D g2 = createGraphics(g);
```

第 84 行 `ElementTheme.FONT.deriveFont(14f)` 替换为：
```java
g2.setFont(theme().getFontBase().deriveFont(14f));
```

第 111 行 `ElementTheme.assertContrast(textColor, Color.WHITE, ...)` 替换为：
```java
assertContrast(textColor, Color.WHITE, "AstDivider text");
```

- [ ] **Step 6: 修改 selfCheck**

将 `ElementTheme.assertContrast(...)` 替换为 `assertContrast(...)`（实例方法）。
将静态 `selfCheck()` 改为实例方法 `protected void selfCheck()`。
保留所有现有断言逻辑。

- [ ] **Step 7: 修改 main 方法**

```java
public static void main(String[] args) {
    new AstDivider(HORIZONTAL).selfCheck();
}
```

- [ ] **Step 8: 编译和自检验证**

```bash
javac -encoding UTF-8 --release 8 -d out src/org/swelement/ui/AstDivider.java src/org/swelement/framework/AstDisplayComponent.java src/org/swelement/framework/AstAbstractComponent.java src/org/swelement/core/AnimationManager.java src/org/swelement/core/theme/Theme.java src/org/swelement/core/theme/ThemeManager.java src/org/swelement/core/theme/ElementLightTheme.java
java -ea -cp out org.swelement.ui.AstDivider
```
Expected: 输出 "AstDivider self-check OK"

---

### Task 3: 迁移 AstTag

**Files:**
- Modify: `src/org/swelement/ui/AstTag.java`（250 行）

AstTag 有手动 Animator（closeAnim）、多状态颜色数组、完善的 selfCheck。

- [ ] **Step 1: 修改类声明和 import**

删除 `import org.swelement.core.Animator;`、`import org.swelement.core.ElementTheme;`，添加：
```java
import org.swelement.framework.AstDisplayComponent;
```
保留 `import org.swelement.core.Easing;`。

将第 11 行类声明改为：
```java
public class AstTag extends AstDisplayComponent {
```

- [ ] **Step 2: 修改静态颜色数组**

第 25 行 `DARK_BG` 数组改为实例方法获取（因为 ElementTheme 静态常量→theme() 实例方法）：
```java
// 删除 DARK_BG 静态数组，改为方法获取:
private Color darkBg(int type) {
    Theme t = theme();
    switch (type) {
        case PRIMARY: return t.getPrimary();
        case SUCCESS: return t.getSuccess();
        case WARNING: return t.getWarning();
        case DANGER: return t.getDanger();
        default: return t.getInfo();
    }
}
```

需要添加 `import org.swelement.core.theme.Theme;`。

- [ ] **Step 3: 替换手动 Animator**

删除第 35-45 行的 `closeAnim` 字段和手动 Animator。
添加 initComponent 注册动画：
```java
@Override
protected void initComponent() {
    super.initComponent();
    anim.register("close", 200, Easing::easeInOut);
}
```

第 95 行 `closeAnim.go(0f, 1f);` 替换为：
```java
origW = getPreferredSize().width;
origH = getPreferredSize().height;
anim.go("close", 0f, 1f);
```

需要在 doLayout 或 paintComponent 中根据 `anim.getProgress("close")` 更新 preferredSize。

- [ ] **Step 4: 修改 paintComponent**

第 135 行手动 Graphics2D 配置替换为 `createGraphics(g)`。
第 153、167 行 `ElementTheme.FONT` 替换为 `theme().getFontBase()`。
`DARK_BG[type]` 替换为 `darkBg(type)`。

- [ ] **Step 5: 修改 closeBtn 设置**

第 107 行 `closeBtn.setHoverColor(ElementTheme.TEXT_MAIN)` 替换为：
```java
closeBtn.setHoverColor(theme().getTextPrimary());
```

- [ ] **Step 6: 修改 selfCheck**

将 `ElementTheme.assertContrast(...)` 替换为 `assertContrast(...)`。
将 `ElementTheme.TEXT_MAIN` 替换为 `theme().getTextPrimary()` 或 `ThemeManager.getCurrent().getTextPrimary()`。
将静态 `selfCheck()` 改为实例方法 `protected void selfCheck()`。

- [ ] **Step 7: 修改 main 方法**

```java
public static void main(String[] args) {
    new AstTag("test", PRIMARY, false).selfCheck();
}
```

- [ ] **Step 8: 编译和自检验证**

```bash
javac -encoding UTF-8 --release 8 -d out src/org/swelement/ui/AstTag.java src/org/swelement/ui/AstCloseButton.java src/org/swelement/framework/AstDisplayComponent.java src/org/swelement/framework/AstAbstractComponent.java src/org/swelement/core/AnimationManager.java src/org/swelement/core/Easing.java src/org/swelement/core/theme/Theme.java src/org/swelement/core/theme/ThemeManager.java src/org/swelement/core/theme/ElementLightTheme.java src/org/swelement/core/ElementTheme.java src/org/swelement/core/StickyToggleModel.java src/org/swelement/core/Animator.java src/org/swelement/core/AnimatedPopup.java src/org/swelement/core/GlassPane.java src/org/swelement/core/PopupPositioner.java
java -ea -cp out org.swelement.ui.AstTag
```
Expected: 输出 "AstTag self-check OK"

---

### Task 4: 迁移 AstBadge

**Files:**
- Modify: `src/org/swelement/ui/AstBadge.java`（400 行）

AstBadge 有手动 Animator（popAnim）、overlay 子组件、完善的 selfCheck。

- [ ] **Step 1: 修改类声明和 import**

删除 `import org.swelement.core.Animator;`、`import org.swelement.core.ElementTheme;`，添加：
```java
import org.swelement.framework.AstDisplayComponent;
import org.swelement.core.theme.Theme;
```
保留 `import org.swelement.core.Easing;`。

将第 11 行类声明改为：
```java
public class AstBadge extends AstDisplayComponent {
```

- [ ] **Step 2: 替换手动 Animator**

删除第 12 行 `popAnim` 字段。删除 `scale` 字段（改用 anim.getProgress）。
添加 initComponent：
```java
@Override
protected void initComponent() {
    super.initComponent();
    anim.register("pop", 200, Easing::easeOut);
}
```

第 78 行 `popAnim.go(scale, 1f);` 替换为：
```java
anim.go("pop", anim.getProgress("pop"), 1f);
```

第 359 行 `b.popAnim.stop();` 替换为：
```java
b.anim.stop("pop");
```

paintBadge 中 `scale` 变量改为 `float scale = anim.getProgress("pop");`。

- [ ] **Step 3: 修改 colorOf 方法**

第 123-129 行，将 `ElementTheme.PRIMARY` 等替换为 `theme().getPrimary()` 等：
```java
private Color colorOf(Type type) {
    Theme t = theme();
    switch (type) {
        case PRIMARY: return t.getPrimary();
        case SUCCESS: return t.getSuccess();
        case WARNING: return t.getWarning();
        case DANGER: return t.getDanger();
        default: return t.getInfo();
    }
}
```

- [ ] **Step 4: 修改构造函数和 setFont**

第 58 行 `setFont(ElementTheme.FONT.deriveFont(...))` 替换为：
```java
setFont(theme().getFontBase().deriveFont(Font.BOLD, TIER_FONT[SIZE_DEFAULT]));
```

第 112 行同理替换。

- [ ] **Step 5: 修改 paintBadge**

第 138 行手动 Graphics2D 配置替换为 `createGraphics(g)`。
第 139 行 `ElementTheme.FONT` 替换为 `theme().getFontBase()`。

- [ ] **Step 6: 修改 selfCheck**

将 `ElementTheme.PRIMARY` 等引用替换为 `ThemeManager.getCurrent().getPrimary()` 等。
将静态 `selfCheck()` 改为实例方法 `protected void selfCheck()`。
保留所有现有断言逻辑，注意 `expectRgb` 方法中引用的 ElementTheme 颜色值需改用 ThemeManager。

- [ ] **Step 7: 修改 main 方法**

```java
public static void main(String[] args) {
    new AstBadge().selfCheck();
}
```

- [ ] **Step 8: 编译和自检验证**

```bash
javac -encoding UTF-8 --release 8 -d out src/org/swelement/ui/AstBadge.java src/org/swelement/framework/AstDisplayComponent.java src/org/swelement/framework/AstAbstractComponent.java src/org/swelement/core/AnimationManager.java src/org/swelement/core/Easing.java src/org/swelement/core/theme/Theme.java src/org/swelement/core/theme/ThemeManager.java src/org/swelement/core/theme/ElementLightTheme.java
java -ea -cp out org.swelement.ui.AstBadge
```
Expected: 输出 "AstBadge self-check OK"

---

### Task 5: 迁移 AstAvatar

**Files:**
- Modify: `src/org/swelement/ui/AstAvatar.java`（179 行）

AstAvatar 有手动 Animator（hoverAnim）、鼠标监听、使用 ElementTheme.lerp/luminance/pickTextColorForBg。

- [ ] **Step 1: 修改类声明和 import**

删除 `import org.swelement.core.Animator;`、`import org.swelement.core.ElementTheme;`，添加：
```java
import org.swelement.framework.AstDisplayComponent;
import org.swelement.core.theme.Theme;
```
保留 `import org.swelement.core.Easing;` 和鼠标/几何 import。

将第 14 行类声明改为：
```java
public class AstAvatar extends AstDisplayComponent {
```

- [ ] **Step 2: 替换手动 Animator 和鼠标监听**

删除第 26-31 行的 `hoverAnim` 和 `hover` 字段。
添加 initComponent 注册动画和鼠标监听：
```java
@Override
protected void initComponent() {
    super.initComponent();
    anim.register("hover", 150, Easing::easeInOut);
    addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            if (isEnabled()) anim.go("hover", anim.getProgress("hover"), 1f);
        }
        public void mouseExited(MouseEvent e) {
            anim.go("hover", anim.getProgress("hover"), 0f);
        }
    });
}
```

注意：Easing.easeInOut 是方法引用语法，需要确认 Easing 类的方法签名。如果 Easing.apply 是实例方法，使用 `Easing::easeInOut`。

- [ ] **Step 3: 修改构造函数**

删除构造函数中的 `setOpaque(false)`（基类已处理）和手动 `addMouseListener`（移到 initComponent）。
保留其余构造逻辑。

- [ ] **Step 4: 修改 paintComponent**

第 66-67 行手动 Graphics2D 配置替换为 `createGraphics(g)`。
第 68 行 `ElementTheme.lerp(...)` 替换为 `lerp(...)`（基类自带方法）。
第 71 行 `ElementTheme.RADIUS` 替换为 `radius()` 或 `theme().getRadiusBase()`。
第 80 行 `ElementTheme.PRIMARY` 替换为 `theme().getPrimary()`。
第 84 行 `ElementTheme.pickTextColorForBg(bgPaint)` 替换为 `pickTextColorForBg(bgPaint)`。
第 85 行 `ElementTheme.assertContrast(...)` 替换为 `assertContrast(...)`。
第 85 行 `ElementTheme.luminance(bgPaint)` 替换为 `luminance(bgPaint)`。
第 92 行 `ElementTheme.FONT` 替换为 `theme().getFontBase()`。

- [ ] **Step 5: 修改 selfCheck**

将所有 `ElementTheme.assertContrast(...)` 替换为 `assertContrast(...)`。
将 `ElementTheme.luminance(...)` 替换为 `luminance(...)`。
将静态 `selfCheck()` 改为实例方法 `protected void selfCheck()`。

- [ ] **Step 6: 修改 main 方法**

```java
public static void main(String[] args) {
    new AstAvatar('A', SIZE_DEFAULT, CIRCLE).selfCheck();
}
```

- [ ] **Step 7: 编译和自检验证**

```bash
javac -encoding UTF-8 --release 8 -d out src/org/swelement/ui/AstAvatar.java src/org/swelement/ui/AstBadge.java src/org/swelement/framework/AstDisplayComponent.java src/org/swelement/framework/AstAbstractComponent.java src/org/swelement/core/AnimationManager.java src/org/swelement/core/Easing.java src/org/swelement/core/theme/Theme.java src/org/swelement/core/theme/ThemeManager.java src/org/swelement/core/theme/ElementLightTheme.java
java -ea -cp out org.swelement.ui.AstAvatar
```
Expected: 输出 "AstAvatar self-check OK"

---

### Task 6: 迁移 AstAlert

**Files:**
- Modify: `src/org/swelement/ui/AstAlert.java`（217 行）

AstAlert 是容器型组件，含两个 Animator（inAnim/outAnim）、AstCloseButton 子组件、多类型。

- [ ] **Step 1: 修改类声明和 import**

删除 `import org.swelement.core.Animator;`、`import org.swelement.core.ElementTheme;`，添加：
```java
import org.swelement.framework.AstContainerComponent;
import org.swelement.core.theme.Theme;
```
保留 `import org.swelement.core.Easing;`。

将第 10 行类声明改为：
```java
public class AstAlert extends AstContainerComponent {
```

- [ ] **Step 2: 修改静态颜色数组**

第 13 行 `COLORS` 数组引用 ElementTheme 静态常量，改为方法获取：
```java
// 删除静态 COLORS 数组，改为方法:
private Color typeColor(int t) {
    Theme theme = theme();
    switch (t) {
        case SUCCESS: return theme.getSuccess();
        case WARNING: return theme.getWarning();
        case INFO: return theme.getInfo();
        default: return theme.getDanger();
    }
}
```

BG 和 ICONS 数组保持不变（它们是硬编码 RGB 值，不依赖 ElementTheme）。

- [ ] **Step 3: 替换手动 Animator**

删除第 27-28 行的 `inAnim` 和 `outAnim` 字段。
删除 `inP` 和 `outP` 字段（改用 anim.getProgress）。

添加 initComponent：
```java
@Override
protected void initComponent() {
    super.initComponent();
    anim.register("in", 300, Easing::easeOut);
    anim.register("out", 250, Easing::easeIn);
}
```

第 59 行 `inAnim.go(0f, 1f);` 替换为：
```java
anim.go("in", 0f, 1f);
```

第 94 行 `outAnim.go(0f, 1f);` 替换为：
```java
origW = getPreferredSize().width;
origH = getPreferredSize().height;
anim.go("out", 0f, 1f);
```

out 动画回调中需要更新 preferredSize。由于 AnimationManager 的 go 方法自动触发 repaint，但 out 动画还需要调整 preferredSize。需要在动画回调中处理。

注意：AnimationManager.register 的回调签名是自动触发 repaint。如果需要在动画过程中做额外操作（如调整 preferredSize），需要在 paintComponent 或 doLayout 中根据 anim.getProgress("out") 计算。

修改 syncClose 方法中引用 outAnim/inAnim 的地方，改为 anim.getProgress("in") 和 anim.getProgress("out")。

- [ ] **Step 4: 修改构造函数**

删除 `setOpaque(false)`（基类已处理）。
保留其余逻辑。将 `inAnim.go(0f, 1f)` 改为 `anim.go("in", 0f, 1f)`。

- [ ] **Step 5: 修改 paintComponent**

第 109 行手动 Graphics2D 配置替换为 `createGraphics(g)`。
`inP` 替换为 `anim.getProgress("in")`。
`outP` 替换为 `anim.getProgress("out")`。
`COLORS[type]` 替换为 `typeColor(type)`。
所有 `ElementTheme.FONT` 替换为 `theme().getFontBase()`。

- [ ] **Step 6: 修改 selfCheck**

将静态 `selfCheck()` 改为实例方法 `protected void selfCheck()`。
保留所有现有断言逻辑。
修改 main 方法：
```java
public static void main(String[] args) {
    new AstAlert(INFO, "title", "desc", true).selfCheck();
}
```

- [ ] **Step 7: 编译和自检验证**

```bash
javac -encoding UTF-8 --release 8 -d out src/org/swelement/ui/AstAlert.java src/org/swelement/ui/AstCloseButton.java src/org/swelement/framework/AstContainerComponent.java src/org/swelement/framework/AstAbstractComponent.java src/org/swelement/core/AnimationManager.java src/org/swelement/core/Easing.java src/org/swelement/core/theme/Theme.java src/org/swelement/core/theme/ThemeManager.java src/org/swelement/core/theme/ElementLightTheme.java src/org/swelement/core/ElementTheme.java src/org/swelement/core/StickyToggleModel.java src/org/swelement/core/Animator.java src/org/swelement/core/AnimatedPopup.java src/org/swelement/core/GlassPane.java src/org/swelement/core/PopupPositioner.java
java -ea -cp out org.swelement.ui.AstAlert
```
Expected: 输出 "AstAlert self-check OK"

---

## 最终验证

- [ ] **Step 1: 编译所有迁移后的组件**

```bash
javac -encoding UTF-8 --release 8 -d out src/org/swelement/ui/AstProgress.java src/org/swelement/ui/AstDivider.java src/org/swelement/ui/AstTag.java src/org/swelement/ui/AstBadge.java src/org/swelement/ui/AstAvatar.java src/org/swelement/ui/AstAlert.java src/org/swelement/framework/AstAbstractComponent.java src/org/swelement/framework/AstInteractiveComponent.java src/org/swelement/framework/AstContainerComponent.java src/org/swelement/framework/AstDisplayComponent.java src/org/swelement/framework/util/PaintingHelper.java src/org/swelement/core/AnimationManager.java src/org/swelement/core/SelfCheckBase.java src/org/swelement/core/Easing.java src/org/swelement/core/Animator.java src/org/swelement/core/ElementTheme.java src/org/swelement/core/StickyToggleModel.java src/org/swelement/core/AnimatedPopup.java src/org/swelement/core/GlassPane.java src/org/swelement/core/PopupPositioner.java src/org/swelement/core/theme/Theme.java src/org/swelement/core/theme/ThemeManager.java src/org/swelement/core/theme/ElementLightTheme.java
```
Expected: 编译成功

- [ ] **Step 2: 运行 run-checks.bat 验证全部自检**

```bash
run-checks.bat
```
Expected: ALL CHECKS PASSED

- [ ] **Step 3: 单独运行 6 个迁移组件的自检**

```bash
java -ea -cp out org.swelement.ui.AstProgress
java -ea -cp out org.swelement.ui.AstDivider
java -ea -cp out org.swelement.ui.AstTag
java -ea -cp out org.swelement.ui.AstBadge
java -ea -cp out org.swelement.ui.AstAvatar
java -ea -cp out org.swelement.ui.AstAlert
```
Expected: 每个输出 "XXX self-check OK"
