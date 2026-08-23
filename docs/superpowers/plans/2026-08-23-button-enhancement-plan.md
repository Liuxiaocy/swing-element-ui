# Button 组件展现方式增强 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Button 组件补齐尺寸、round、circle、图标、loading、text 六种 Element UI 展现方式，保持零依赖、自绘、JDK 8 兼容。

**Architecture:** 在现有 Button.java 基础上新增字段和 setter 方法，扩展 paintComponent 绘制逻辑和 getPreferredSize 尺寸计算。loading 旋转动画通过新增 Animator 驱动角度自绘圆弧。所有新特性正交组合，不破坏现有 API。

**Tech Stack:** Java 8, Java Swing (JComponent/Graphics2D), 自研 Animator/Easing/ElementTheme, 零外部依赖

**Spec:** `docs/superpowers/specs/2026-08-23-button-enhancement-design.md`

## Global Constraints

- JDK 8，编译参数 `--release 8`
- 零外部依赖，纯 JDK API
- 全部自绘（覆盖 paintComponent），与 L&F 无关
- 动画使用自研 Animator（javax.swing.Timer 封装）
- 不改变现有公共构造函数签名：`Button(String text)` 和 `Button(String text, int type, boolean plain)`
- 构建通过 `build.bat`，自检通过 `java -ea -cp out org.swelement.ui.Button`
- 代码文件 UTF-8 编码

---

## 文件结构

| 文件 | 操作 | 职责 |
|---|---|---|
| `src/org/swelement/ui/Button.java` | 修改 | 组件实现（新增字段、方法、绘制逻辑、自检） |
| `src/org/swelement/demo/ButtonDemo.java` | 修改 | 演示所有新特性 |

`build.bat` 已包含这两个文件，无需修改。

---

### Task 1: 尺寸系统 + selfCheck 骨架

**Files:**
- Modify: `src/org/swelement/ui/Button.java`

**Interfaces:**
- Produces: `Button.SIZE_LARGE`, `Button.SIZE_DEFAULT`, `Button.SIZE_SMALL` 常量；`Button.setSize(int size)` 方法；`Button.selfCheck()` 静态方法；`Button.main(String[])` 入口

- [ ] **Step 1: 在 Button.java 中添加尺寸常量和参数表，以及 selfCheck 骨架（先写测试断言）**

在 `public class Button extends JButton {` 行之后、现有 `public static final int DEFAULT...` 常量之后，添加：

```java
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    public static final int ICON_LEFT = 0, ICON_RIGHT = 1;

    private static final float[] SIZE_FONT = {16f, 14f, 12f};
    private static final int[] SIZE_VPAD = {12, 9, 6};
    private static final int[] SIZE_HPAD = {24, 20, 12};
    private static final int[] SIZE_ICON_GAP = {10, 8, 6};
```

在现有 `private final int type;` 字段之前添加尺寸字段：

```java
    private int size = SIZE_DEFAULT;
```

在类的末尾（最后一个 `}` 之前）添加 selfCheck 和 main：

```java
    static void selfCheck() {
        Button b = new Button("测试");
        b.setSize(Button.SIZE_LARGE);
        assert b.getPreferredSize().height > new Button("测试").getPreferredSize().height
                : "SIZE_LARGE should be taller than SIZE_DEFAULT";
        b.setSize(Button.SIZE_SMALL);
        assert b.getPreferredSize().height < new Button("测试").getPreferredSize().height
                : "SIZE_SMALL should be shorter than SIZE_DEFAULT";
        System.out.println("Button self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
```

- [ ] **Step 2: 编译并运行自检，确认失败（setSize 方法不存在）**

Run:
```
cd /d "D:\Program Files\code\swing-element-ui"
build.bat
```
Expected: BUILD FAILED，编译错误 `cannot find symbol method setSize(int)`

- [ ] **Step 3: 实现 setSize 方法并修改 getPreferredSize 使用尺寸参数**

在构造函数之后、`paintComponent` 之前添加：

```java
    public void setSize(int size) {
        this.size = size;
        revalidate();
        repaint();
    }
```

将现有 `getPreferredSize` 方法（当前返回 `new Dimension(fm.stringWidth(getText()) + 40, fm.getHeight() + 18)`）替换为：

```java
    @Override
    public Dimension getPreferredSize() {
        Font font = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        FontMetrics fm = getFontMetrics(font);
        int w = SIZE_HPAD[size] * 2 + fm.stringWidth(getText());
        int h = SIZE_VPAD[size] * 2 + fm.getHeight();
        return new Dimension(w, h);
    }
```

同时修改构造函数中的 `setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));` — 由于现在 getPreferredSize 已控制尺寸，border 不再影响布局，保留即可。但 `setFont(ElementTheme.FONT)` 需改为根据 size 动态设置，在构造函数中保留默认字体即可，paintComponent 中使用 SIZE_FONT 派生字体。

- [ ] **Step 4: 修改 paintComponent 使用尺寸对应的字体**

在 `paintComponent` 中，将绘制文字部分的字体从 `getFont()` 改为尺寸字体。找到现有代码：

```java
        g2.setColor(fg);
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
```

替换为：

```java
        g2.setColor(fg);
        Font btnFont = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        g2.setFont(btnFont);
        FontMetrics fm = g2.getFontMetrics(btnFont);
        String text = getText();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
```

- [ ] **Step 5: 编译并运行自检，确认通过**

Run:
```
cd /d "D:\Program Files\code\swing-element-ui"
build.bat
java -ea -cp out org.swelement.ui.Button
```
Expected: `Button self-check OK`，且 build.bat 中的其他自检也通过

- [ ] **Step 6: 提交**

```bash
git add src/org/swelement/ui/Button.java
git commit -m "feat(button): add size system (large/default/small) with self-check"
```

---

### Task 2: round 和 circle 圆角

**Files:**
- Modify: `src/org/swelement/ui/Button.java`

**Interfaces:**
- Consumes: `Button.SIZE_*` 常量和尺寸参数表（Task 1）
- Produces: `Button.setRound(boolean round)`, `Button.setCircle(boolean circle)` 方法

- [ ] **Step 1: 在 selfCheck 中追加 round/circle 断言**

在 `selfCheck()` 方法中、`System.out.println("Button self-check OK");` 之前添加：

```java
        Button rc = new Button("圆");
        rc.setCircle(true);
        Dimension pd = rc.getPreferredSize();
        assert pd.width == pd.height : "circle button preferredSize must be square, got " + pd.width + "x" + pd.height;
        rc.setRound(true);
        assert pd.width == pd.height : "round+circle still square";
```

- [ ] **Step 2: 编译确认失败（setCircle/setRound 不存在）**

Run: `build.bat`
Expected: BUILD FAILED，`cannot find symbol method setCircle(boolean)` / `setRound(boolean)`

- [ ] **Step 3: 添加 round/circle 字段和 setter 方法**

在 `private int size = SIZE_DEFAULT;` 之后添加：

```java
    private boolean round = false;
    private boolean circle = false;
```

在 `setSize` 方法之后添加：

```java
    public void setRound(boolean round) {
        this.round = round;
        repaint();
    }

    public void setCircle(boolean circle) {
        this.circle = circle;
        revalidate();
        repaint();
    }
```

- [ ] **Step 4: 修改 getPreferredSize 支持 circle 正方形**

将 Task 1 中的 `getPreferredSize` 末尾：

```java
        return new Dimension(w, h);
```

替换为：

```java
        if (circle) {
            int s = Math.max(w, h);
            return new Dimension(s, s);
        }
        return new Dimension(w, h);
```

- [ ] **Step 5: 修改 paintComponent 圆角计算**

找到现有代码：

```java
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, ElementTheme.RADIUS * 2, ElementTheme.RADIUS * 2);
```

替换为：

```java
        float arc = (round || circle) ? getHeight() / 2f : ElementTheme.RADIUS * 2;
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
```

- [ ] **Step 6: 编译并运行自检确认通过**

Run:
```
build.bat
java -ea -cp out org.swelement.ui.Button
```
Expected: `Button self-check OK`

- [ ] **Step 7: 提交**

```bash
git add src/org/swelement/ui/Button.java
git commit -m "feat(button): add round and circle corner modes"
```

---

### Task 3: 图标按钮

**Files:**
- Modify: `src/org/swelement/ui/Button.java`

**Interfaces:**
- Consumes: `Button.SIZE_ICON_GAP` 参数表（Task 1），`Button.ICON_LEFT/ICON_RIGHT` 常量（Task 1）
- Produces: `Button.setIcon(String icon)`, `Button.setIconPosition(int pos)` 方法

- [ ] **Step 1: 在 selfCheck 中追加图标断言**

在 selfCheck 的 circle 断言之后、println 之前添加：

```java
        Button ib = new Button("");
        ib.setIcon("\u2713");
        assert ib.getPreferredSize().width > 0 : "icon-only button should have positive width";
        Button ib2 = new Button("确定");
        ib2.setIcon("\u2713");
        assert ib2.getPreferredSize().width > new Button("确定").getPreferredSize().width
                : "button with icon should be wider than text-only";
        ib2.setIconPosition(Button.ICON_RIGHT);
        assert ib2.getPreferredSize().width > new Button("确定").getPreferredSize().width
                : "icon-right button should also be wider";
```

- [ ] **Step 2: 编译确认失败**

Run: `build.bat`
Expected: BUILD FAILED，`cannot find symbol method setIcon(String)` / `setIconPosition(int)`

- [ ] **Step 3: 添加图标字段和 setter 方法**

在 `private boolean circle = false;` 之后添加：

```java
    private String icon = null;
    private int iconPosition = ICON_LEFT;
```

在 `setCircle` 方法之后添加：

```java
    public void setIcon(String icon) {
        this.icon = icon;
        revalidate();
        repaint();
    }

    public void setIconPosition(int pos) {
        this.iconPosition = pos;
        repaint();
    }
```

- [ ] **Step 4: 修改 getPreferredSize 计算图标宽度**

在 `getPreferredSize` 中，找到：

```java
        int w = SIZE_HPAD[size] * 2 + fm.stringWidth(getText());
```

替换为：

```java
        int textW = fm.stringWidth(getText());
        int iconW = (icon != null) ? fm.stringWidth(icon) : 0;
        int gap = (iconW > 0 && textW > 0) ? SIZE_ICON_GAP[size] : 0;
        int w = SIZE_HPAD[size] * 2 + textW + iconW + gap;
```

- [ ] **Step 5: 修改 paintComponent 绘制图标**

在 paintComponent 中，找到文字绘制部分（Task 1 修改后的）：

```java
        g2.setColor(fg);
        Font btnFont = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        g2.setFont(btnFont);
        FontMetrics fm = g2.getFontMetrics(btnFont);
        String text = getText();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
```

替换为：

```java
        g2.setColor(fg);
        Font btnFont = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        g2.setFont(btnFont);
        FontMetrics fm = g2.getFontMetrics(btnFont);
        String text = getText();
        int textW = fm.stringWidth(text);
        int iconW = (icon != null) ? fm.stringWidth(icon) : 0;
        int gap = (iconW > 0 && textW > 0) ? SIZE_ICON_GAP[size] : 0;
        int contentW = textW + iconW + gap;
        float startX = (getWidth() - contentW) / 2f;
        float baseY = (getHeight() - fm.getHeight()) / 2f + fm.getAscent();
        if (icon != null) {
            if (iconPosition == ICON_LEFT) {
                g2.drawString(icon, startX, baseY);
                g2.drawString(text, startX + iconW + gap, baseY);
            } else {
                g2.drawString(text, startX, baseY);
                g2.drawString(icon, startX + textW + gap, baseY);
            }
        } else {
            g2.drawString(text, startX, baseY);
        }
```

- [ ] **Step 6: 编译并运行自检确认通过**

Run:
```
build.bat
java -ea -cp out org.swelement.ui.Button
```
Expected: `Button self-check OK`

- [ ] **Step 7: 提交**

```bash
git add src/org/swelement/ui/Button.java
git commit -m "feat(button): add icon button support (Unicode icon, left/right position)"
```

---

### Task 4: loading 加载中

**Files:**
- Modify: `src/org/swelement/ui/Button.java`

**Interfaces:**
- Consumes: `Easing::linear`（已存在），`Animator`（已存在），尺寸参数表
- Produces: `Button.setLoading(boolean loading)`, `Button.setLoadingText(String text)` 方法

- [ ] **Step 1: 在 selfCheck 中追加 loading 断言**

在 selfCheck 的图标断言之后、println 之前添加：

```java
        Button lb = new Button("提交", Button.PRIMARY, false);
        assert lb.isEnabled() : "button should be enabled initially";
        lb.setLoading(true);
        assert !lb.isEnabled() : "loading button should be disabled";
        assert "加载中".equals(lb.getText()) : "loading text should default to 加载中, got " + lb.getText();
        lb.setLoading(false);
        assert lb.isEnabled() : "button should restore enabled after loading";
        assert "提交".equals(lb.getText()) : "button should restore original text after loading, got " + lb.getText();
        Button lb2 = new Button("保存");
        lb2.setLoadingText("保存中...");
        lb2.setLoading(true);
        assert "保存中...".equals(lb2.getText()) : "custom loading text should be used, got " + lb2.getText();
        lb2.setLoading(false);
```

- [ ] **Step 2: 编译确认失败**

Run: `build.bat`
Expected: BUILD FAILED，`cannot find symbol method setLoading(boolean)` / `setLoadingText(String)`

- [ ] **Step 3: 添加 loading 字段、动画和 setter 方法**

在 `private int iconPosition = ICON_LEFT;` 之后添加：

```java
    private boolean loading = false;
    private String loadingText = null;
    private float loadAngle = 0f;
    private boolean savedEnabled;
    private String savedText;

    private final Animator loadAnim = new Animator(800, Easing::linear, v -> { loadAngle = v; repaint(); });
```

在 `setIconPosition` 方法之后添加：

```java
    public void setLoading(boolean loading) {
        if (this.loading == loading) return;
        this.loading = loading;
        if (loading) {
            savedEnabled = isEnabled();
            savedText = getText();
            setEnabled(false);
            setText(loadingText != null ? loadingText : "加载中");
            loadAngle = 0f;
            startLoadLoop();
        } else {
            loadAnim.stop();
            setEnabled(savedEnabled);
            setText(savedText);
        }
        revalidate();
        repaint();
    }

    public void setLoadingText(String text) {
        this.loadingText = text;
        if (loading) setText(text != null ? text : "加载中");
    }

    private void startLoadLoop() {
        loadAnim.go(0f, 1f, () -> { if (loading) startLoadLoop(); });
    }
```

- [ ] **Step 4: 修改 getPreferredSize 考虑 loading 状态**

在 `getPreferredSize` 中，找到：

```java
        int textW = fm.stringWidth(getText());
        int iconW = (icon != null) ? fm.stringWidth(icon) : 0;
        int gap = (iconW > 0 && textW > 0) ? SIZE_ICON_GAP[size] : 0;
        int w = SIZE_HPAD[size] * 2 + textW + iconW + gap;
```

替换为：

```java
        int textW = fm.stringWidth(getText());
        int iconW = (!loading && icon != null) ? fm.stringWidth(icon) : 0;
        int loadW = loading ? 16 + SIZE_ICON_GAP[size] : 0;
        int gap = (iconW > 0 && textW > 0) ? SIZE_ICON_GAP[size] : 0;
        int w = SIZE_HPAD[size] * 2 + textW + iconW + gap + loadW;
```

- [ ] **Step 5: 修改 paintComponent 绘制 loading 旋转圆弧并调整内容布局**

在 paintComponent 中，将 Task 3 修改后的文字/图标绘制部分替换。找到：

```java
        g2.setColor(fg);
        Font btnFont = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        g2.setFont(btnFont);
        FontMetrics fm = g2.getFontMetrics(btnFont);
        String text = getText();
        int textW = fm.stringWidth(text);
        int iconW = (icon != null) ? fm.stringWidth(icon) : 0;
        int gap = (iconW > 0 && textW > 0) ? SIZE_ICON_GAP[size] : 0;
        int contentW = textW + iconW + gap;
        float startX = (getWidth() - contentW) / 2f;
        float baseY = (getHeight() - fm.getHeight()) / 2f + fm.getAscent();
        if (icon != null) {
            if (iconPosition == ICON_LEFT) {
                g2.drawString(icon, startX, baseY);
                g2.drawString(text, startX + iconW + gap, baseY);
            } else {
                g2.drawString(text, startX, baseY);
                g2.drawString(icon, startX + textW + gap, baseY);
            }
        } else {
            g2.drawString(text, startX, baseY);
        }
```

替换为：

```java
        g2.setColor(fg);
        Font btnFont = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        g2.setFont(btnFont);
        FontMetrics fm = g2.getFontMetrics(btnFont);
        String text = getText();
        int textW = fm.stringWidth(text);
        int iconW = (!loading && icon != null) ? fm.stringWidth(icon) : 0;
        int loadW = loading ? 16 + SIZE_ICON_GAP[size] : 0;
        int gap = (iconW > 0 && textW > 0) ? SIZE_ICON_GAP[size] : 0;
        int contentW = textW + iconW + gap + loadW;
        float startX = (getWidth() - contentW) / 2f;
        float baseY = (getHeight() - fm.getHeight()) / 2f + fm.getAscent();
        float cursorX = startX;

        if (loading) {
            // 绘制旋转圆弧
            Graphics2D lg2 = (Graphics2D) g2.create();
            lg2.setColor(fg);
            lg2.setStroke(new BasicStroke(2f));
            int cx = Math.round(cursorX + 8);
            int cy = getHeight() / 2;
            double angle = loadAngle * 2 * Math.PI;
            lg2.drawArc(cx - 7, cy - 7, 14, 14, (int) Math.toDegrees(angle), 270);
            lg2.dispose();
            cursorX += loadW;
        }

        if (!loading && icon != null) {
            if (iconPosition == ICON_LEFT) {
                g2.drawString(icon, cursorX, baseY);
                cursorX += iconW + gap;
                g2.drawString(text, cursorX, baseY);
            } else {
                g2.drawString(text, cursorX, baseY);
                cursorX += textW + gap;
                g2.drawString(icon, cursorX, baseY);
            }
        } else {
            g2.drawString(text, cursorX, baseY);
        }
```

- [ ] **Step 6: 编译并运行自检确认通过**

Run:
```
build.bat
java -ea -cp out org.swelement.ui.Button
```
Expected: `Button self-check OK`

- [ ] **Step 7: 提交**

```bash
git add src/org/swelement/ui/Button.java
git commit -m "feat(button): add loading state with spinning arc animation"
```

---

### Task 5: text 文本按钮

**Files:**
- Modify: `src/org/swelement/ui/Button.java`

**Interfaces:**
- Consumes: 现有 hover 动画、颜色系统
- Produces: `Button.setTextButton(boolean textBtn)` 方法

- [ ] **Step 1: 在 selfCheck 中追加 text 按钮断言**

在 selfCheck 的 loading 断言之后、println 之前添加：

```java
        Button tb = new Button("文本按钮");
        tb.setTextButton(true);
        assert tb.getPreferredSize().width > 0 : "text button should have positive width";
        // text 按钮不抛异常即通过（视觉效果需 Demo 目视）
```

- [ ] **Step 2: 编译确认失败**

Run: `build.bat`
Expected: BUILD FAILED，`cannot find symbol method setTextButton(boolean)`

- [ ] **Step 3: 添加 textButton 字段和 setter**

在 `private String savedText;` 之后（loading 字段区域末尾）添加：

```java
    private boolean textButton = false;
```

在 `setLoadingText` 方法之后添加：

```java
    public void setTextButton(boolean textBtn) {
        this.textButton = textBtn;
        repaint();
    }
```

- [ ] **Step 4: 修改 paintComponent 颜色计算和绘制逻辑支持 text 模式**

找到 paintComponent 中的颜色计算部分：

```java
        Color bg, fg, border;
        if (!isEnabled()) {
            bg = plain ? FILL_BLANK : new Color(0xA0CFFF);
            fg = new Color(0xC0C4CC);
            border = plain ? BORDER_BASE : bg;
        } else {
            bg = ElementTheme.lerp(ElementTheme.lerp(BASE_BG[type], HOVER_BG[type], hover), ACTIVE_BG[type], active);
            fg = ElementTheme.lerp(BASE_FG[type], HOVER_FG[type], hover);
            border = plain ? BORDER[type] : bg;
            if (plain) bg = ElementTheme.lerp(FILL_BLANK, new Color(0xECF5FF), hover);
            if (plain) fg = ElementTheme.lerp(BASE_FG[type], PRIMARY_COLOR, hover);
        }
        if (plain && type == DEFAULT) border = ElementTheme.lerp(BORDER_BASE, new Color(0xC6E2FF), hover);
```

替换为：

```java
        Color bg, fg, border;
        if (textButton) {
            // text 按钮：仅 primary 色，无背景无边框，hover 时浅色背景
            if (!isEnabled()) {
                bg = new Color(0, 0, 0, 0);
                fg = new Color(0xC0C4CC);
                border = new Color(0, 0, 0, 0);
            } else {
                int alpha = Math.round(255 * hover);
                bg = new Color(0xEC, 0xF5, 0xFF, alpha);
                fg = ElementTheme.PRIMARY;
                border = new Color(0, 0, 0, 0);
            }
        } else if (!isEnabled()) {
            bg = plain ? FILL_BLANK : new Color(0xA0CFFF);
            fg = new Color(0xC0C4CC);
            border = plain ? BORDER_BASE : bg;
        } else {
            bg = ElementTheme.lerp(ElementTheme.lerp(BASE_BG[type], HOVER_BG[type], hover), ACTIVE_BG[type], active);
            fg = ElementTheme.lerp(BASE_FG[type], HOVER_FG[type], hover);
            border = plain ? BORDER[type] : bg;
            if (plain) bg = ElementTheme.lerp(FILL_BLANK, new Color(0xECF5FF), hover);
            if (plain) fg = ElementTheme.lerp(BASE_FG[type], PRIMARY_COLOR, hover);
            if (plain && type == DEFAULT) border = ElementTheme.lerp(BORDER_BASE, new Color(0xC6E2FF), hover);
        }
```

然后找到背景和边框绘制部分：

```java
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.setColor(bg);
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(shape);
```

替换为：

```java
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (!textButton || hover > 0) {
            g2.setColor(bg);
            g2.fill(shape);
        }
        if (!textButton) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(shape);
        }
```

- [ ] **Step 5: 编译并运行自检确认通过**

Run:
```
build.bat
java -ea -cp out org.swelement.ui.Button
```
Expected: `Button self-check OK`

- [ ] **Step 6: 提交**

```bash
git add src/org/swelement/ui/Button.java
git commit -m "feat(button): add text button mode (primary only, hover background)"
```

---

### Task 6: Demo 更新

**Files:**
- Modify: `src/org/swelement/demo/ButtonDemo.java`

**Interfaces:**
- Consumes: 所有 Task 1-5 新增的 Button 方法

- [ ] **Step 1: 重写 ButtonDemo.java 展示所有新特性**

将整个 `ButtonDemo.java` 内容替换为：

```java
package org.swelement.demo;

import org.swelement.ui.Button;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ButtonDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Button Demo - 尺寸/圆角/圆形/图标/加载/文本按钮");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(16, 20, 20, 20));

            // ========== 尺寸 ==========
            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p1.setBorder(new TitledBorder("尺寸 Size"));
            Button bl = new Button("Large 大按钮", Button.PRIMARY, false);
            bl.setSize(Button.SIZE_LARGE);
            Button bd = new Button("Default 默认", Button.PRIMARY, false);
            Button bs = new Button("Small 小按钮", Button.PRIMARY, false);
            bs.setSize(Button.SIZE_SMALL);
            p1.add(bl); p1.add(bd); p1.add(bs);

            // ========== round 圆角 ==========
            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p2.setBorder(new TitledBorder("圆角 Round"));
            int[] types = {Button.DEFAULT, Button.PRIMARY, Button.SUCCESS, Button.WARNING, Button.DANGER, Button.INFO};
            String[] labels = {"默认", "主要", "成功", "警告", "危险", "信息"};
            for (int i = 0; i < types.length; i++) {
                Button b = new Button(labels[i], types[i], false);
                b.setRound(true);
                p2.add(b);
            }

            // ========== circle 圆形 + 图标 ==========
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p3.setBorder(new TitledBorder("圆形 Circle（图标按钮）"));
            String[] icons = {"\u2713", "\u2717", "\u2605", "\u2699", "\u21bb", "\u2764"};
            int[] ctypes = {Button.SUCCESS, Button.DANGER, Button.WARNING, Button.INFO, Button.PRIMARY, Button.DANGER};
            for (int i = 0; i < icons.length; i++) {
                Button b = new Button("", ctypes[i], false);
                b.setIcon(icons[i]);
                b.setCircle(true);
                p3.add(b);
            }

            // ========== 图标位置 ==========
            JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p4.setBorder(new TitledBorder("图标 Icon（左/右）"));
            Button il = new Button("图标在左", Button.PRIMARY, false);
            il.setIcon("\u2713");
            Button ir = new Button("图标在右", Button.PRIMARY, false);
            ir.setIcon("\u2192");
            ir.setIconPosition(Button.ICON_RIGHT);
            p4.add(il); p4.add(ir);

            // ========== loading ==========
            JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p5.setBorder(new TitledBorder("加载中 Loading（点击触发，2秒后恢复）"));
            Button loadBtn = new Button("点击加载", Button.PRIMARY, false);
            loadBtn.addActionListener(e -> {
                loadBtn.setLoading(true);
                Timer t = new Timer(2000, ev -> loadBtn.setLoading(false));
                t.setRepeats(false);
                t.start();
            });
            Button loadBtn2 = new Button("保存", Button.SUCCESS, false);
            loadBtn2.setLoadingText("保存中...");
            loadBtn2.addActionListener(e -> {
                loadBtn2.setLoading(true);
                Timer t = new Timer(2000, ev -> loadBtn2.setLoading(false));
                t.setRepeats(false);
                t.start();
            });
            p5.add(loadBtn); p5.add(loadBtn2);

            // ========== text 文本按钮 ==========
            JPanel p6 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p6.setBorder(new TitledBorder("文本按钮 Text"));
            Button tb1 = new Button("文本按钮", Button.PRIMARY, false);
            tb1.setTextButton(true);
            Button tb2 = new Button("禁用文本", Button.PRIMARY, false);
            tb2.setTextButton(true);
            tb2.setEnabled(false);
            Button tb3 = new Button("圆角文本", Button.PRIMARY, false);
            tb3.setTextButton(true);
            tb3.setRound(true);
            p6.add(tb1); p6.add(tb2); p6.add(tb3);

            // ========== 原有：6种类型 ==========
            JPanel p7 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p7.setBorder(new TitledBorder("按钮类型（原有）"));
            for (int i = 0; i < types.length; i++) p7.add(new Button(labels[i], types[i], false));

            // ========== 原有：朴素 + 禁用 ==========
            JPanel p8 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p8.setBorder(new TitledBorder("朴素 Plain + 禁用 Disabled（原有）"));
            p8.add(new Button("朴素主要", Button.PRIMARY, true));
            Button dis = new Button("禁用-主要", Button.PRIMARY, false);
            dis.setEnabled(false);
            p8.add(dis);

            root.add(p1);
            root.add(Box.createVerticalStrut(6));
            root.add(p2);
            root.add(Box.createVerticalStrut(6));
            root.add(p3);
            root.add(Box.createVerticalStrut(6));
            root.add(p4);
            root.add(Box.createVerticalStrut(6));
            root.add(p5);
            root.add(Box.createVerticalStrut(6));
            root.add(p6);
            root.add(Box.createVerticalStrut(6));
            root.add(p7);
            root.add(Box.createVerticalStrut(6));
            root.add(p8);

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 2: 编译确认通过**

Run: `build.bat`
Expected: BUILD OK，所有自检通过

- [ ] **Step 3: 运行 Demo 目视验证（人工确认）**

Run: `java -cp out org.swelement.demo.ButtonDemo`
Expected: 窗口显示 8 个区域，逐一验证：
- 尺寸：large 最高、small 最矮
- round：胶囊形圆角
- circle：正方形圆形图标按钮
- 图标：图标在左/在右
- loading：点击后按钮禁用、显示旋转圆弧和"加载中"/"保存中..."，2秒后恢复
- text：无背景无边框，hover 时浅蓝背景
- 原有类型/朴素/禁用：行为不变

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/demo/ButtonDemo.java
git commit -m "demo(button): update ButtonDemo to showcase all new button variants"
```

---

## Self-Review

**1. Spec coverage:**
- 尺寸（large/default/small）→ Task 1 ✓
- round 圆角 → Task 2 ✓
- circle 圆形 → Task 2 ✓
- 图标按钮（Unicode、左/右位置）→ Task 3 ✓
- loading（旋转圆弧、默认"加载中"、自定义文字、状态恢复）→ Task 4 ✓
- text 文本按钮（仅 primary、hover 背景）→ Task 5 ✓
- 图标间距按尺寸缩放 → Task 1 参数表 + Task 3/4 使用 ✓
- selfCheck 自检 → 各任务逐步追加 ✓
- Demo 更新 → Task 6 ✓
- 不破坏现有 API → 所有新增均为新方法/字段 ✓

**2. Placeholder scan:** 无 TBD/TODO/模糊描述，所有代码步骤均有完整代码块。

**3. Type consistency:**
- `setSize(int)` 使用 `SIZE_LARGE/SIZE_DEFAULT/SIZE_SMALL` 常量，一致
- `setIconPosition(int)` 使用 `ICON_LEFT/ICON_RIGHT` 常量，一致
- `SIZE_FONT/SIZE_VPAD/SIZE_HPAD/SIZE_ICON_GAP` 数组索引与尺寸常量一一对应，一致
- loading 字段 `savedEnabled/savedText` 在 setLoading 中读写，一致

**4. 构建兼容性:** `build.bat` 已包含 Button.java 和 ButtonDemo.java，无需修改。新增的 `java -ea -cp out org.swelement.ui.Button` 自检需手动运行验证。
