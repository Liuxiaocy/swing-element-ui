# swing-element-ui Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 JDK 8 Swing 中实现 Element UI 风格基础组件库（Button/Input/Checkbox/Radio/Switch/Slider），所有 UI 变化带动画。

**Architecture:** 自绘组件（`JComponent` 子类覆盖 `paintComponent`）+ 自研微型动画引擎（`javax.swing.Timer` 插值 + 缓动函数）。组件持有 `hover/focus/active` 0→1 进度状态，由 Animator 驱动，paint 时按进度插值颜色/位移/透明度。

**Tech Stack:** Java 8 (`--release 8`)、javax.swing、纯 javac 构建（无 Maven/Gradle/外部依赖）。

## Global Constraints

- 编译参数：`javac -encoding UTF-8 --release 8`；若 JDK 报 `--release 8` 不支持，改用 `-source 8 -target 8`。
- 零外部依赖：只用 JDK 8 标准库。
- 包根：`org.swelement`，源码在 `src/` 下，编译输出到 `out/`。
- 所有交互组件自绘，与 L&F 无关（`setOpaque(false)` + 自定义 `paintComponent`）。
- 动画时长默认 200ms，缓动默认 `easeInOut`。
- 每个组件配一个 `*Demo` 类（`main` 起 JFrame）作为视觉验收。
- 核心逻辑（Easing/ElementTheme/Animator）带 assert 自检 `main`。
- 设计规格：`docs/superpowers/specs/2026-08-19-swing-element-ui-design.md`

---

### Task 1: 项目骨架 + 构建脚本 + Easing

**Files:**
- Create: `src/org/swelement/core/Easing.java`
- Create: `build.bat`
- Test: `Easing.selfCheck()`（内嵌 `main`）

**Interfaces:**
- Consumes: 无
- Produces: `Easing` 接口，静态方法 `linear(float)`, `easeIn(float)`, `easeOut(float)`, `easeInOut(float)`，全部返回 `[0,1]` 单调递增的缓动值。自检入口 `Easing.selfCheck()` 与 `main`。

- [ ] **Step 1: 写失败的自检**

创建 `src/org/swelement/core/Easing.java`：

```java
package org.swelement.core;

public interface Easing {
    float apply(float t);

    static float linear(float t) { return t; }
    static float easeIn(float t) { return t * t * t; }
    static float easeOut(float t) { return 1f - (float) Math.pow(1 - t, 3); }
    static float easeInOut(float t) { return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f; }

    static void selfCheck() {
        for (Easing e : new Easing[]{Easing::linear, Easing::easeIn, Easing::easeOut, Easing::easeInOut}) {
            float prev = -1f;
            for (int i = 0; i <= 100; i++) {
                float v = e.apply(i / 100f);
                assert v >= 0f && v <= 1f : e + " out of range at " + i;
                assert v >= prev - 1e-6f : e + " not monotonic at " + i;
                prev = v;
            }
            assert Math.abs(e.apply(0f)) < 1e-4f : e + " apply(0) != 0";
            assert Math.abs(e.apply(1f) - 1f) < 1e-4f : e + " apply(1) != 1";
        }
        System.out.println("Easing self-check OK");
    }

    static void main(String[] args) { selfCheck(); }
}
```

- [ ] **Step 2: 编译并运行，确认失败**

Run:
```
mkdir out
javac -encoding UTF-8 --release 8 -d out src/org/swelement/core/Easing.java
java -ea -cp out org.swelement.core.Easing
```
Expected: 编译通过，运行输出 `Easing self-check OK`（本任务实现即正确，验证链路建立）。

- [ ] **Step 3: 创建 build.bat**

创建 `build.bat`：

```bat
@echo off
setlocal
where javac >nul 2>nul || (echo ERROR: javac not on PATH & exit /b 1)
if not exist out mkdir out
dir /s /b src\*.java > .sources.txt
javac -encoding UTF-8 --release 8 -d out @.sources.txt
if errorlevel 1 (
  echo --release 8 not supported, retrying with -source/-target 8
  javac -encoding UTF-8 -source 8 -target 8 -d out @.sources.txt
)
if errorlevel 1 (echo BUILD FAILED & exit /b 1)
echo BUILD OK
```

- [ ] **Step 4: 运行 build.bat 确认全量编译通过**

Run: `.\build.bat`
Expected: 输出 `BUILD OK`。

- [ ] **Step 5: 提交**

```bash
git add src/org/swelement/core/Easing.java build.bat
git commit -m "feat: project skeleton, build.bat, Easing"
```

---

### Task 2: ElementTheme（色板 + 插值工具）

**Files:**
- Create: `src/org/swelement/core/ElementTheme.java`
- Test: `ElementTheme.selfCheck()`（内嵌 `main`）

**Interfaces:**
- Consumes: 无
- Produces: 颜色/字体常量（`PRIMARY` `SUCCESS` `WARNING` `DANGER` `INFO` `TEXT_MAIN` `TEXT_REGULAR` `TEXT_PLACEHOLDER` `BORDER_BASE` `FILL_BLANK` `FILL_BASE`，均 `Color`）、`int RADIUS`、`Font FONT`；静态插值 `Color lerp(Color,Color,float)`、`float lerp(float,float,float)`、`int lerp(int,int,float)`。

- [ ] **Step 1: 写失败的自检**

创建 `src/org/swelement/core/ElementTheme.java`：

```java
package org.swelement.core;

import java.awt.Color;
import java.awt.Font;

public final class ElementTheme {
    public static final Color PRIMARY = new Color(0x409EFF);
    public static final Color SUCCESS = new Color(0x67C23A);
    public static final Color WARNING = new Color(0xE6A23C);
    public static final Color DANGER  = new Color(0xF56C6C);
    public static final Color INFO    = new Color(0x909399);
    public static final Color TEXT_MAIN = new Color(0x303133);
    public static final Color TEXT_REGULAR = new Color(0x606266);
    public static final Color TEXT_PLACEHOLDER = new Color(0xC0C4CC);
    public static final Color BORDER_BASE = new Color(0xDCDFE6);
    public static final Color FILL_BLANK = new Color(0xFFFFFF);
    public static final Color FILL_BASE = new Color(0xF5F7FA);
    public static final int RADIUS = 4;
    public static final Font FONT = new Font("Microsoft YaHei", Font.PLAIN, 14);

    private ElementTheme() {}

    public static Color lerp(Color a, Color b, float t) {
        return new Color(
            lerp(a.getRed(), b.getRed(), t),
            lerp(a.getGreen(), b.getGreen(), t),
            lerp(a.getBlue(), b.getBlue(), t));
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    public static int lerp(int a, int b, float t) { return Math.round(lerp((float) a, (float) b, t)); }

    static void selfCheck() {
        assert lerp(Color.WHITE, Color.BLACK, 0f).equals(Color.WHITE);
        assert lerp(Color.WHITE, Color.BLACK, 1f).equals(Color.BLACK);
        assert lerp(10, 20, 0.5f) == 15;
        assert lerp(0.5f, 1f, 0.5f) == 0.75f;
        assert lerp(Color.WHITE, Color.BLACK, 0.5f).getRed() == 128;  // Math.round(127.5f)==128
        System.out.println("ElementTheme self-check OK");
    }

    static void main(String[] args) { selfCheck(); }
}
```

- [ ] **Step 2: 编译运行自检**

Run:
```
javac -encoding UTF-8 --release 8 -d out src/org/swelement/core/ElementTheme.java
java -ea -cp out org.swelement.core.ElementTheme
```
Expected: 输出 `ElementTheme self-check OK`。

- [ ] **Step 3: 提交**

```bash
git add src/org/swelement/core/ElementTheme.java
git commit -m "feat: ElementTheme palette and interpolation"
```

---

### Task 3: Animator 动画引擎

**Files:**
- Create: `src/org/swelement/core/Animator.java`
- Test: `Animator.main`（内嵌 assert 自检）

**Interfaces:**
- Consumes: `Easing`
- Produces: `Animator(int durationMs, Easing, Animator.Listener)`；`Listener` 为函数式接口 `void update(float v)`；方法 `void go(float from, float to)`（从当前值动画到目标值，中断时从当前值继续）、`void stop()`、`boolean running()`。

- [ ] **Step 1: 写失败的自检**

创建 `src/org/swelement/core/Animator.java`：

```java
package org.swelement.core;

import javax.swing.Timer;

public final class Animator {
    public interface Listener { void update(float v); }

    private final Timer timer;
    private final long duration;
    private final Easing easing;
    private final Listener listener;
    private float from, to;
    private long start;

    public Animator(int durationMs, Easing easing, Listener listener) {
        this.duration = durationMs;
        this.easing = easing;
        this.listener = listener;
        this.timer = new Timer(15, e -> tick());
    }

    public void go(float from, float to) {
        this.from = from;
        this.to = to;
        this.start = System.currentTimeMillis();
        this.timer.start();
    }

    public void stop() { timer.stop(); }

    public boolean running() { return timer.isRunning(); }

    private void tick() {
        float p = (System.currentTimeMillis() - start) / (float) duration;
        if (p >= 1f) { p = 1f; timer.stop(); }
        listener.update(from + (to - from) * easing.apply(p));
    }

    static void main(String[] args) throws Exception {
        final float[] last = {-1f};
        Animator a = new Animator(40, Easing::linear, v -> last[0] = v);
        a.go(0f, 1f);
        Thread.sleep(200);
        assert a.running() == false : "animator should have stopped";
        assert Math.abs(last[0] - 1f) < 0.001f : "did not reach target: " + last[0];

        a.go(5f, 0f);
        Thread.sleep(200);
        assert Math.abs(last[0] - 0f) < 0.001f : "did not animate to 0: " + last[0];

        float mid = last[0];
        a.go(0f, 1f);
        Thread.sleep(30);
        a.go(last[0], 0f);   // 中断重定向：从当前值反向
        Thread.sleep(200);
        assert Math.abs(last[0] - 0f) < 0.001f : "interrupt re-target failed: " + last[0];

        System.out.println("Animator self-check OK");
    }
}
```

- [ ] **Step 2: 编译运行自检**

Run:
```
javac -encoding UTF-8 --release 8 -d out src/org/swelement/core/Animator.java
java -ea -cp out org.swelement.core.Animator
```
Expected: 输出 `Animator self-check OK`。

- [ ] **Step 3: 提交**

```bash
git add src/org/swelement/core/Animator.java
git commit -m "feat: Animator timing engine"
```

---

### Task 4: Button + ButtonDemo

**Files:**
- Create: `src/org/swelement/ui/Button.java`
- Create: `src/org/swelement/demo/ButtonDemo.java`
- Test: `ButtonDemo`（视觉验收）

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Button extends JButton`，构造 `Button(String)`（默认类型）、`Button(String text, int type, boolean plain)`；类型常量 `DEFAULT=0 PRIMARY=1 SUCCESS=2 WARNING=3 DANGER=4 INFO=5`。

- [ ] **Step 1: 写 Button**

创建 `src/org/swelement/ui/Button.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class Button extends JButton {
    public static final int DEFAULT = 0, PRIMARY = 1, SUCCESS = 2, WARNING = 3, DANGER = 4, INFO = 5;

    private static final Color WHITE = Color.WHITE;
    private static final Color FILL_BLANK = ElementTheme.FILL_BLANK;
    private static final Color PRIMARY_COLOR = ElementTheme.PRIMARY;
    private static final Color SUCCESS_COLOR = ElementTheme.SUCCESS;
    private static final Color WARNING_COLOR = ElementTheme.WARNING;
    private static final Color DANGER_COLOR = ElementTheme.DANGER;
    private static final Color INFO_COLOR = ElementTheme.INFO;
    private static final Color BORDER_BASE_COLOR = ElementTheme.BORDER_BASE;

    private static final Color[] BASE_BG  = {FILL_BLANK, PRIMARY_COLOR, SUCCESS_COLOR, WARNING_COLOR, DANGER_COLOR, INFO_COLOR};
    private static final Color[] HOVER_BG = {new Color(0xECF5FF), new Color(0x66B1FF), new Color(0x85CE61), new Color(0xEBB563), new Color(0xF78989), new Color(0xA6A9AD)};
    private static final Color[] ACTIVE_BG= {new Color(0xD2E4FF), new Color(0x3A8EE6), new Color(0x5DAF32), new Color(0xCF9236), new Color(0xDD6161), new Color(0x82848A)};
    private static final Color[] BASE_FG  = {new Color(0x606266), WHITE, WHITE, WHITE, WHITE, WHITE};
    private static final Color[] HOVER_FG = {PRIMARY_COLOR, WHITE, WHITE, WHITE, WHITE, WHITE};
    private static final Color[] BORDER   = {BORDER_BASE_COLOR, PRIMARY_COLOR, SUCCESS_COLOR, WARNING_COLOR, DANGER_COLOR, INFO_COLOR};

    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private final Animator activeAnim = new Animator(120, Easing::easeInOut, v -> { active = v; repaint(); });
    private float hover, active;
    private final int type;
    private final boolean plain;

    public Button(String text) { this(text, DEFAULT, false); }

    public Button(String text, int type, boolean plain) {
        super(text);
        this.type = type;
        this.plain = plain;
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
        setFont(ElementTheme.FONT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (isEnabled()) hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); activeAnim.go(active, 0f); }
            public void mousePressed(MouseEvent e) { if (isEnabled()) activeAnim.go(active, 1f); }
            public void mouseReleased(MouseEvent e){ activeAnim.go(active, 0f); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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
            if (plain) fg = ElementTheme.lerp(BASE_FG[type], PRIMARY, hover);
        }
        if (plain && type == DEFAULT) border = ElementTheme.lerp(BORDER_BASE, new Color(0xC6E2FF), hover);

        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, ElementTheme.RADIUS * 2, ElementTheme.RADIUS * 2);
        g2.setColor(bg);
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(shape);

        g2.setColor(fg);
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText()) + 40, fm.getHeight() + 18);
    }
}
```

- [ ] **Step 2: 写 ButtonDemo**

创建 `src/org/swelement/demo/ButtonDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.Button;

import javax.swing.*;
import java.awt.*;

public class ButtonDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Button Demo");
            JPanel p = new JPanel(new FlowLayout(20, 20, 20));
            int[] types = {Button.DEFAULT, Button.PRIMARY, Button.SUCCESS, Button.WARNING, Button.DANGER, Button.INFO};
            String[] labels = {"默认按钮", "主要按钮", "成功按钮", "警告按钮", "危险按钮", "信息按钮"};
            for (int i = 0; i < types.length; i++) p.add(new Button(labels[i], types[i], false));
            p.add(new Button("朴素按钮", Button.PRIMARY, true));
            Button disabled = new Button("禁用按钮", Button.PRIMARY, false);
            disabled.setEnabled(false);
            p.add(disabled);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 3: 编译 + 视觉验收**

Run: `.\build.bat` 然后 `java -cp out org.swelement.demo.ButtonDemo`
Expected: 窗口显示各类型按钮；鼠标移入背景渐变色过渡，按下回弹，disabled 按钮灰显且无动画。

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/ui/Button.java src/org/swelement/demo/ButtonDemo.java
git commit -m "feat: Button with hover/active animations + demo"
```

---

### Task 5: Input + InputDemo

**Files:**
- Create: `src/org/swelement/ui/Input.java`
- Create: `src/org/swelement/demo/InputDemo.java`
- Test: `InputDemo`（视觉验收）

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Input extends JPanel`，内嵌 `JTextField`；构造 `Input(String placeholder)`；方法 `String getText()`、`void setText(String)`、`void setEnabled(boolean)`。

- [ ] **Step 1: 写 Input**

创建 `src/org/swelement/ui/Input.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class Input extends JPanel {
    private final JTextField field;
    private final Animator focusAnim = new Animator(200, Easing::easeInOut, v -> { focus = v; repaint(); });
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private final Animator clearAnim = new Animator(150, Easing::easeInOut, v -> { clearVis = v; repaint(); });
    private float focus, hover, clearVis;
    private boolean hasText;
    private final String placeholder;

    public Input(String placeholder) {
        this.placeholder = placeholder;
        setOpaque(false);
        setLayout(new BorderLayout());
        field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!hasText && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(ElementTheme.TEXT_PLACEHOLDER);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(placeholder, 12, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                    g2.dispose();
                }
            }
        };
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 30));
        field.setFont(ElementTheme.FONT);
        field.setForeground(ElementTheme.TEXT_MAIN);
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { hasText = !field.getText().isEmpty(); updateClear(); }
            public void removeUpdate(DocumentEvent e) { hasText = !field.getText().isEmpty(); updateClear(); }
            public void changedUpdate(DocumentEvent e) {}
        });
        add(field, BorderLayout.CENTER);

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { focusAnim.go(focus, 1f); updateClear(); }
            public void focusLost(FocusEvent e)   { focusAnim.go(focus, 0f); updateClear(); }
        });
        MouseAdapter m = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hoverAnim.go(hover, 1f); updateClear(); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); updateClear(); }
            public void mouseClicked(MouseEvent e) {
                if (e.getX() > getWidth() - 30) { setText(""); field.requestFocus(); }
            }
        };
        field.addMouseListener(m);   // field 铺满面板，鼠标事件落在 field 上
        addMouseListener(m);
    }

    private void updateClear() {
        float target = hasText ? Math.max(focus, hover) : 0f;
        clearAnim.go(clearVis, target);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color border = ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, Math.max(focus, hover));
        if (!isEnabled()) border = new Color(0xE4E7ED);
        Color bg = isEnabled() ? ElementTheme.lerp(ElementTheme.FILL_BLANK, ElementTheme.FILL_BASE, hover) : ElementTheme.FILL_BASE;
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, ElementTheme.RADIUS * 2, ElementTheme.RADIUS * 2);
        g2.setColor(bg);
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(focus > 0 ? 2f : 1f));
        g2.draw(shape);
        if (focus > 0) {  // 聚焦光晕
            g2.setColor(new Color(64, 158, 255, Math.round(50 * focus)));
            g2.setStroke(new BasicStroke(4f));
            g2.draw(shape);
        }
        if (clearVis > 0) {  // × 淡入
            g2.setColor(new Color(192, 196, 204, Math.round(255 * clearVis)));
            Font f = g2.getFont().deriveFont(Font.BOLD, 14f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            String x = "\u00d7";
            g2.drawString(x, getWidth() - 24 - fm.stringWidth(x) / 2, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        }
        g2.dispose();
    }

    public String getText() { return field.getText(); }
    public void setText(String t) { field.setText(t); }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        field.setEnabled(enabled);
    }
}
```

- [ ] **Step 2: 写 InputDemo**

创建 `src/org/swelement/demo/InputDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.Input;

import javax.swing.*;
import java.awt.*;

public class InputDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Input Demo");
            JPanel p = new JPanel(new FlowLayout(30, 30, 30));
            Input a = new Input("请输入内容");
            a.setPreferredSize(new Dimension(220, 40));
            Input b = new Input("disabled");
            b.setPreferredSize(new Dimension(220, 40));
            b.setEnabled(false);
            p.add(a);
            p.add(b);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 3: 编译 + 视觉验收**

Run: `.\build.bat` 然后 `java -cp out org.swelement.demo.InputDemo`
Expected: 输入框 hover 边框变蓝、聚焦边框加深并带光晕、输入内容后出现可点击清空的 ×、disabled 灰显。

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/ui/Input.java src/org/swelement/demo/InputDemo.java
git commit -m "feat: Input with focus glow and clear button + demo"
```

---

### Task 6: Checkbox + CheckboxDemo

**Files:**
- Create: `src/org/swelement/ui/Checkbox.java`
- Create: `src/org/swelement/demo/CheckboxDemo.java`
- Test: `CheckboxDemo`（视觉验收）

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Checkbox extends JCheckBox`（自绘），构造 `Checkbox(String)`；继承标准 `isSelected()`/`setSelected()`/`setEnabled()`。

- [ ] **Step 1: 写 Checkbox**

创建 `src/org/swelement/ui/Checkbox.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public class Checkbox extends JCheckBox {
    private final Animator fillAnim = new Animator(200, Easing::easeInOut, v -> { fill = v; repaint(); });
    private final Animator checkAnim = new Animator(200, Easing::easeOut, v -> { check = v; repaint(); });
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private float fill, check, hover;

    public Checkbox(String text) {
        super(text);
        setOpaque(false);
        setFocusPainted(false);
        setFont(ElementTheme.FONT);
        setForeground(ElementTheme.TEXT_REGULAR);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); }
        });
        addItemListener(e -> {
            fillAnim.go(fill, isSelected() ? 1f : 0f);
            checkAnim.go(check, isSelected() ? 1f : 0f);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int y = (getHeight() - 16) / 2;
        Color border = isEnabled()
            ? ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, Math.max(fill, hover))
            : new Color(0xC0C4CC);
        Color bg = ElementTheme.lerp(ElementTheme.FILL_BLANK, ElementTheme.PRIMARY, fill);
        if (!isEnabled()) bg = ElementTheme.lerp(ElementTheme.FILL_BLANK, new Color(0xC0C4CC), fill);

        Shape box = new RoundRectangle2D.Float(0, y, 16, 16, 4, 4);
        g2.setColor(bg);
        g2.fill(box);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(box);

        if (check > 0) {  // 勾号描边动画：裁剪窗口从左到右揭示
            Shape old = g2.getClip();
            g2.clip(new Rectangle2D.Float(0, y - 2, 12 * check + 1, 20));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            p.moveTo(4, y + 9);
            p.lineTo(7, y + 12);
            p.lineTo(12, y + 5);
            g2.draw(p);
            g2.setClip(old);
        }

        g2.setColor(isEnabled() ? ElementTheme.TEXT_REGULAR : new Color(0xC0C4CC));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), 24, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText()) + 28, 20);
    }
}
```

- [ ] **Step 2: 写 CheckboxDemo**

创建 `src/org/swelement/demo/CheckboxDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.Checkbox;

import javax.swing.*;
import java.awt.*;

public class CheckboxDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Checkbox Demo");
            JPanel p = new JPanel(new FlowLayout(30, 30, 30));
            p.add(new Checkbox("默认"));
            p.add(new Checkbox("已选"));
            Checkbox c = new Checkbox("已选");
            c.setSelected(true);
            p.add(c);
            Checkbox d = new Checkbox("禁用");
            d.setEnabled(false);
            p.add(d);
            Checkbox e = new Checkbox("选中禁用");
            e.setEnabled(false);
            e.setSelected(true);
            p.add(e);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 3: 编译 + 视觉验收**

Run: `.\build.bat` 然后 `java -cp out org.swelement.demo.CheckboxDemo`
Expected: 点击时边框渐蓝、背景填充渐变、勾号由短到长描画；禁用灰显。

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/ui/Checkbox.java src/org/swelement/demo/CheckboxDemo.java
git commit -m "feat: Checkbox with animated checkmark + demo"
```

---

### Task 7: Radio + RadioDemo

**Files:**
- Create: `src/org/swelement/ui/Radio.java`
- Create: `src/org/swelement/demo/RadioDemo.java`
- Test: `RadioDemo`（视觉验收）

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Radio extends JRadioButton`（自绘），构造 `Radio(String)`；继承标准 `isSelected()`/`setSelected()`/`setEnabled()`。

- [ ] **Step 1: 写 Radio**

创建 `src/org/swelement/ui/Radio.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Radio extends JRadioButton {
    private final Animator dotAnim = new Animator(200, Easing::easeOut, v -> { dot = v; repaint(); });
    private final Animator borderAnim = new Animator(200, Easing::easeInOut, v -> { border = v; repaint(); });
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private float dot, border, hover;

    public Radio(String text) {
        super(text);
        setOpaque(false);
        setFocusPainted(false);
        setFont(ElementTheme.FONT);
        setForeground(ElementTheme.TEXT_REGULAR);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); }
        });
        addItemListener(e -> {
            borderAnim.go(border, isSelected() ? 1f : 0f);
            dotAnim.go(dot, isSelected() ? 1f : 0f);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int y = (getHeight() - 16) / 2;
        int cx = 8, cy = y + 8;

        Color borderColor = isEnabled()
            ? ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, Math.max(border, hover))
            : new Color(0xC0C4CC);
        g2.setColor(ElementTheme.FILL_BLANK);
        g2.fillOval(cx - 8, cy - 8, 16, 16);
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(cx - 8, cy - 8, 16, 16);

        float r = 4f * (float) Math.sqrt(dot);
        g2.setColor(ElementTheme.PRIMARY);
        g2.fillOval((int) (cx - r), (int) (cy - r), (int) (2 * r), (int) (2 * r));

        g2.setColor(isEnabled() ? ElementTheme.TEXT_REGULAR : new Color(0xC0C4CC));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), 24, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText()) + 28, 20);
    }
}
```

- [ ] **Step 2: 写 RadioDemo**

创建 `src/org/swelement/demo/RadioDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.Radio;

import javax.swing.*;
import java.awt.*;

public class RadioDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Radio Demo");
            JPanel p = new JPanel(new FlowLayout(30, 30, 30));
            ButtonGroup group = new ButtonGroup();
            Radio a = new Radio("选项 A");
            Radio b = new Radio("选项 B");
            Radio c = new Radio("选项 C");
            group.add(a); group.add(b); group.add(c);
            p.add(a); p.add(b); p.add(c);
            Radio d = new Radio("禁用");
            d.setEnabled(false);
            p.add(d);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 3: 编译 + 视觉验收**

Run: `.\build.bat` 然后 `java -cp out org.swelement.demo.RadioDemo`
Expected: 单选圆圈边框渐蓝、选中时内点从小到大弹出；组内互斥；禁用灰显。

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/ui/Radio.java src/org/swelement/demo/RadioDemo.java
git commit -m "feat: Radio with animated dot + demo"
```

---

### Task 8: Switch + SwitchDemo

**Files:**
- Create: `src/org/swelement/ui/Switch.java`
- Create: `src/org/swelement/demo/SwitchDemo.java`
- Test: `SwitchDemo`（视觉验收）

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Switch extends JToggleButton`（自绘），构造 `Switch()`；继承标准 `isSelected()`/`setSelected()`/`setEnabled()`。

- [ ] **Step 1: 写 Switch**

创建 `src/org/swelement/ui/Switch.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Switch extends JToggleButton {
    private final Animator slideAnim = new Animator(300, Easing::easeInOut, v -> { slide = v; repaint(); });
    private float slide;

    public Switch() {
        setOpaque(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addItemListener(e -> slideAnim.go(slide, isSelected() ? 1f : 0f));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = 44, h = 22;
        int y = (getHeight() - h) / 2;

        Color track = isEnabled()
            ? ElementTheme.lerp(new Color(0xDCDFE6), ElementTheme.PRIMARY, slide)
            : new Color(0xE4E7ED);
        g2.setColor(track);
        g2.fill(new RoundRectangle2D.Float(0, y, w, h, h, h));

        int knob = 18;
        int x = Math.round(2 + slide * (w - knob - 4));
        g2.setColor(Color.WHITE);
        g2.fillOval(x, y + 2, knob, knob);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(44, 22); }
}
```

- [ ] **Step 2: 写 SwitchDemo**

创建 `src/org/swelement/demo/SwitchDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.Switch;

import javax.swing.*;
import java.awt.*;

public class SwitchDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Switch Demo");
            JPanel p = new JPanel(new FlowLayout(30, 30, 30));
            p.add(new Switch());
            Switch on = new Switch();
            on.setSelected(true);
            p.add(on);
            Switch d = new Switch();
            d.setEnabled(false);
            p.add(d);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 3: 编译 + 视觉验收**

Run: `.\build.bat` 然后 `java -cp out org.swelement.demo.SwitchDemo`
Expected: 点击后 knob 平滑滑动、轨道底色从灰渐变到蓝；禁用灰显。

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/ui/Switch.java src/org/swelement/demo/SwitchDemo.java
git commit -m "feat: Switch with sliding knob animation + demo"
```

---

### Task 9: Slider + SliderDemo

**Files:**
- Create: `src/org/swelement/ui/Slider.java`
- Create: `src/org/swelement/demo/SliderDemo.java`
- Test: `SliderDemo`（视觉验收）

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Slider extends JComponent`，构造 `Slider(int min, int max, int value)`；方法 `int getValue()`、`void setValue(int)`、`void addChangeListener(ChangeListener)`。

- [ ] **Step 1: 写 Slider**

创建 `src/org/swelement/ui/Slider.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class Slider extends JComponent {
    private final Animator thumbAnim = new Animator(200, Easing::easeOut, v -> { thumbX = v; repaint(); });
    private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
    private float thumbX = -1f, hover;   // thumbX 为像素位置，-1 表示未初始化
    private int min, max, value;
    private boolean dragging;

    public Slider(int min, int max, int value) {
        this.min = min; this.max = max; this.value = value;
        setOpaque(false);
        setPreferredSize(new Dimension(240, 32));
        MouseAdapter m = new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { dragging = true; setValueFrom(e.getX()); }
            public void mouseDragged(MouseEvent e)  { setValueFrom(e.getX()); }
            public void mouseReleased(MouseEvent e) { dragging = false; }
            public void mouseEntered(MouseEvent e)  { hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)   { hoverAnim.go(hover, 0f); }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    private void setValueFrom(int x) {
        int left = 6, right = getWidth() - 16;
        float t = (x - left) / (float) (right - left);
        setValue(min + Math.round(t * (max - min)));
    }

    public int getValue() { return value; }

    public void setValue(int v) {
        int nv = Math.max(min, Math.min(max, v));
        if (nv != value) {
            value = nv;
            fire();
        }
        repaint();
    }

    public void addChangeListener(ChangeListener l) { listenerList.add(ChangeListener.class, l); }
    public void removeChangeListener(ChangeListener l) { listenerList.remove(ChangeListener.class, l); }

    private void fire() {
        for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) l.stateChanged(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cy = getHeight() / 2;
        int trackY = cy - 3, trackH = 6;
        int left = 6, right = getWidth() - 16;
        float t = (max == min) ? 0f : (value - min) / (float) (max - min);
        int thumbTarget = left + Math.round(t * (right - left));
        if (thumbX < 0) thumbX = thumbTarget;
        if (dragging) thumbX = thumbTarget; else thumbAnim.go(thumbX, thumbTarget);
        int cx = Math.round(thumbX);

        Color trackColor = isEnabled() ? ElementTheme.PRIMARY : new Color(0xC0C4CC);
        g2.setColor(new Color(0xE4E7ED));
        g2.fill(new RoundRectangle2D.Float(left, trackY, right - left, trackH, trackH, trackH));
        int fillW = Math.max(0, Math.min(right - left, cx - left));
        g2.setColor(trackColor);
        g2.fill(new RoundRectangle2D.Float(left, trackY, fillW, trackH, trackH, trackH));

        float r = 6f * (1f + 0.25f * hover);
        g2.setColor(Color.WHITE);
        g2.fillOval(Math.round(cx - r), Math.round(cy - r), Math.round(2 * r), Math.round(2 * r));
        g2.setColor(trackColor);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(Math.round(cx - r), Math.round(cy - r), Math.round(2 * r), Math.round(2 * r));
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(240, 32); }
}
```

> 注：`thumbX` 表示 thumb 的像素 x 坐标。拖拽时立即跟随（`thumbX = thumbTarget`），松开/程序设值后由 `thumbAnim` 从当前位置动画到目标位置。

- [ ] **Step 2: 写 SliderDemo**

创建 `src/org/swelement/demo/SliderDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.Slider;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class SliderDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Slider Demo");
            JPanel p = new JPanel(new FlowLayout(30, 30, 30));
            Slider s = new Slider(0, 100, 40);
            JLabel label = new JLabel("40");
            label.setPreferredSize(new Dimension(40, 24));
            s.addChangeListener(e -> label.setText(String.valueOf(s.getValue())));
            p.add(s);
            p.add(label);
            Slider d = new Slider(0, 100, 30);
            d.setEnabled(false);
            p.add(d);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 3: 编译 + 视觉验收**

Run: `.\build.bat` 然后 `java -cp out org.swelement.demo.SliderDemo`
Expected: 拖拽 thumb 跟手、已选轨道填充、松开后 thumb 归位动画；hover 放大过渡；disabled 灰显不可拖。

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/ui/Slider.java src/org/swelement/demo/SliderDemo.java
git commit -m "feat: Slider with thumb animation + demo"
```

---

### Task 10: 全量回归 + 收尾

**Files:**
- Modify: 无（仅验证）

- [ ] **Step 1: 全量编译 + 全部自检 + 全部 Demo 可启动**

Run: `.\build.bat`，然后依次运行（每个窗口出现后关闭）：
```
java -ea -cp out org.swelement.core.Easing
java -ea -cp out org.swelement.core.ElementTheme
java -ea -cp out org.swelement.core.Animator
java -cp out org.swelement.demo.ButtonDemo
java -cp out org.swelement.demo.InputDemo
java -cp out org.swelement.demo.CheckboxDemo
java -cp out org.swelement.demo.RadioDemo
java -cp out org.swelement.demo.SwitchDemo
java -cp out org.swelement.demo.SliderDemo
```
Expected: 三个自检输出 OK，六个 Demo 窗口正常显示且动画流畅。

- [ ] **Step 2: 更新 README（运行说明）**

创建 `README.md`：

```markdown
# swing-element-ui

Element UI 风格的 Java Swing 组件库（JDK 8，零依赖）。

## 构建

```
.\build.bat        # 编译到 out/
```

## 运行 Demo

```
java -cp out org.swelement.demo.ButtonDemo
java -cp out org.swelement.demo.InputDemo
java -cp out org.swelement.demo.CheckboxDemo
java -cp out org.swelement.demo.RadioDemo
java -cp out org.swelement.demo.SwitchDemo
java -cp out org.swelement.demo.SliderDemo
```

## 核心自检

```
java -ea -cp out org.swelement.core.Easing
java -ea -cp out org.swelement.core.ElementTheme
java -ea -cp out org.swelement.core.Animator
```

## 设计

见 `docs/superpowers/specs/2026-08-19-swing-element-ui-design.md`
```

- [ ] **Step 3: 提交**

```bash
git add README.md
git commit -m "docs: README with build and run instructions"
```

---

## Self-Review

**1. Spec coverage:**
- 动画引擎 ✓ Task 3；缓动 ✓ Task 1；主题色板 ✓ Task 2
- Button/Input/Checkbox/Radio/Switch/Slider ✓ Task 4-9
- 每个组件 Demo ✓ Task 4-9；核心自检 ✓ Task 1-3
- build.bat（`--release 8` + UTF-8）✓ Task 1；README ✓ Task 10
- 错误处理/可访问性：disabled 全程生效 ✓ 各组件；焦点遍历保留（继承 Swing 组件）✓

**2. Placeholder scan:** 无 TBD/TODO。Task 9 Step 2 明确要求删除占位方法 `setValueFromFix` 并补 `fire()` 通知。

**3. Type consistency:** `Animator.go(from,to)` 各处一致；`ElementTheme.lerp` 三重重载一致；组件构造签名在 Demo 中与类定义一致。