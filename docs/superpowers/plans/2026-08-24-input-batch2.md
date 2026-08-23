# Input 展现增强 + Select 可清空重写 实施计划（批次 2）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Input 增加尺寸档位、密码模式、前后缀图标，新增 TextArea 多行输入组件，并将 Select 的手绘 × 清空重写为 CloseButton 配方。

**Architecture:** 全部为 JDK8 Swing 自绘组件增强，不引入新依赖。Input 沿用「JPanel + 内嵌透明 JTextField + paintComponent 画边框/光晕」的既有配方；TextArea 复用同一绘制配方；Select 复用 Input 批次 1 的「east 面板 + CloseButton + clearVis 淡入」配方，消灭坐标命中测试。

**Tech Stack:** Java 8 Swing，javac 编译（build.bat），自检 = 各组件 `main()` 里的 `selfCheck()`，用 `java -ea` 断言驱动（TDD 的 RED/GREEN 即断言失败/通过）。

**Spec:** `docs/superpowers/specs/2026-08-24-input-batch2-design.md`

## Global Constraints

- JDK 8 语法（无 var、无 pattern matching、lambda 可用）。
- 文字颜色对背景对比度 ≥ 4.5:1（WCAG AA），图标默认色用 `0x606266`（≥7:1）。
- 图标全矢量自绘（AstIcon），不引入图片/字体资源。
- 事件响应用 `mousePressed`（不用 `mouseClicked`，快速点击修复后的项目规范）。
- `Animator.go()` 读起始值要读「状态」而非动画中间值（见 Input.updateClear 注释）。
- 编译验证命令（bash）：`cd "D:/Program Files/code/swing-element-ui" && javac -nowarn -encoding UTF-8 -d .workbuddy/probe/out $(find src -name '*.java')`
- 单组件自检：`java -ea -Djava.awt.headless=false -cp .workbuddy/probe/out <类名>`

---

### Task 1: AstIcon 补齐 EYE_OFF

**Files:**
- Modify: `src/org/swelement/ui/AstIcon.java`

**Interfaces:**
- Consumes: 现有 `AstIcon(int type, Color color, int size)` 构造器、`stroke()` 工具。
- Produces: `AstIcon.EYE_OFF`（int 常量 = 19），`ICON_COUNT == 20`。Task 3 密码切换依赖它。

- [ ] **Step 1: 写失败断言（RED）**

在 `AstIcon.selfCheck()` 的「Paint every icon type」块之后追加：

```java
        // EYE_OFF 绘制不抛异常且像素非空（密码切换依赖）
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            AstIcon eo = new AstIcon(EYE_OFF, ElementTheme.PRIMARY, 20);
            eo.setBounds(0, 0, 20, 20);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(20, 20, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            gg.setColor(Color.WHITE); gg.fillRect(0, 0, 20, 20);
            try { eo.paint(gg); } finally { gg.dispose(); }
            int nonWhite = 0;
            for (int x = 0; x < 20; x++) for (int y = 0; y < 20; y++) {
                int p = img.getRGB(x, y);
                if (((p >> 16) & 0xFF) < 200 || ((p >> 8) & 0xFF) < 200 || (p & 0xFF) < 200) nonWhite++;
            }
            assert nonWhite > 5 : "EYE_OFF should draw visible strokes, nonWhite=" + nonWhite;
        }}); } catch (Throwable t) { throw new RuntimeException(t); }
```

- [ ] **Step 2: 运行验证失败**

Run: `cd "D:/Program Files/code/swing-element-ui" && javac -nowarn -encoding UTF-8 -d .workbuddy/probe/out $(find src -name '*.java')`
Expected: 编译错误 `cannot find symbol: variable EYE_OFF`

- [ ] **Step 3: 实现**

常量区（`DELETE = 18;` 之后）：

```java
    public static final int EYE_OFF = 19;
    private static final int ICON_COUNT = 20;
```

（删除原来的 `private static final int ICON_COUNT = 19;`）

switch 增加（`case DELETE:` 之后）：

```java
            case EYE_OFF: drawEyeOff(g2, s); break;
```

绘制方法（`drawEye` 之后）：

```java
    private static void drawEyeOff(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float cy = cx(s);
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.1f, cy);
        p.curveTo(s * 0.3f, s * 0.2f, s * 0.7f, s * 0.2f, s * 0.9f, cy);
        p.curveTo(s * 0.7f, s * 0.8f, s * 0.3f, s * 0.8f, s * 0.1f, cy);
        g2.draw(p);
        // 斜杠贯穿（闭眼）
        g2.draw(new Line2D.Float(s * 0.14f, s * 0.84f, s * 0.86f, s * 0.16f));
    }
```

- [ ] **Step 4: 运行验证通过（GREEN）**

Run: 编译命令 + `java -ea -Djava.awt.headless=false -cp .workbuddy/probe/out org.swelement.ui.AstIcon`
Expected: `AstIcon self-check OK`

- [ ] **Step 5: Commit**

```bash
git add src/org/swelement/ui/AstIcon.java
git commit -m "feat(AstIcon): 补齐 EYE_OFF 闭眼图标（文档注释已声明但实现缺失）"
```

---

### Task 2: Input 尺寸档位

**Files:**
- Modify: `src/org/swelement/ui/Input.java`

**Interfaces:**
- Consumes: `CloseButton.setButtonSize(int)`（已存在）。
- Produces: `Input.SIZE_LARGE=0 / SIZE_DEFAULT=1 / SIZE_SMALL=2`，`setSize(int tier)`（与 Tag 同名 API），`getPreferredSize().height == {40,32,28}[tier]`。Task 7 demo 依赖。

- [ ] **Step 1: 写失败断言（RED）**

在 `Input.selfCheck()` 开头（`Input in = new Input("占位符");` 之前）插入：

```java
        Input df = new Input("默认");
        assert df.getPreferredSize().height == 32 : "DEFAULT height 32, got " + df.getPreferredSize().height;
        Input lg = new Input("大");
        lg.setSize(Input.SIZE_LARGE);
        assert lg.getPreferredSize().height == 40 : "LARGE height 40, got " + lg.getPreferredSize().height;
        Input sm = new Input("小");
        sm.setSize(Input.SIZE_SMALL);
        assert sm.getPreferredSize().height == 28 : "SMALL height 28, got " + sm.getPreferredSize().height;
        boolean threw = false;
        try { sm.setSize(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "invalid tier must throw";
```

- [ ] **Step 2: 运行验证失败**

Run: 编译命令 + `java -ea ... org.swelement.ui.Input`
Expected: 断言失败 `DEFAULT height 32, got ...`（当前无档位，preferred 高度由 field 决定，约 37~40）

- [ ] **Step 3: 实现**

类头部（`public class Input extends JPanel {` 之后、`private final JTextField field;` 之前）加常量与字段：

```java
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    private static final int[] TIER_HEIGHT = {40, 32, 28};
    private static final float[] TIER_FONT = {14f, 13f, 12f};
    private static final int[] TIER_VPAD = {10, 8, 4};
    private static final int[] TIER_HPAD = {16, 12, 8};
    private static final int[] TIER_CLEAR = {18, 16, 14};
    private int tier = SIZE_DEFAULT;
```

构造器末尾（`east.addMouseListener(m);` 之后）加：`applyTier();`，并新增方法（`updateClear()` 之前）：

```java
    /** 尺寸档位（对齐 Element UI）：高度 40/32/28，档位联动字体、内边距与清空按钮尺寸。 */
    public void setSize(int tier) {
        if (tier < SIZE_LARGE || tier > SIZE_SMALL)
            throw new IllegalArgumentException("invalid size tier: " + tier);
        this.tier = tier;
        applyTier();
    }

    private void applyTier() {
        field.setFont(ElementTheme.FONT.deriveFont(TIER_FONT[tier]));
        field.setBorder(BorderFactory.createEmptyBorder(TIER_VPAD[tier], TIER_HPAD[tier], TIER_VPAD[tier], 8));
        clearBtn.setButtonSize(TIER_CLEAR[tier]);
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = TIER_HEIGHT[tier];
        return d;
    }
```

同时把构造器里原有的 `field.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));` 删除（被 applyTier 取代），`field.setFont(ElementTheme.FONT);` 保留（applyTier 会覆盖）。

- [ ] **Step 4: 运行验证通过（GREEN）**

Run: 编译命令 + `java -ea ... org.swelement.ui.Input`
Expected: `Input self-check OK`（原有清空断言仍通过——clearBtn 尺寸变了但点击坐标 10,10 仍在 16px 按钮内）

- [ ] **Step 5: Commit**

```bash
git add src/org/swelement/ui/Input.java
git commit -m "feat(Input): 尺寸档位 large/default/small（高度 40/32/28，联动字体/内边距/清空按钮）"
```

---

### Task 3: Input 密码模式

**Files:**
- Modify: `src/org/swelement/ui/Input.java`

**Interfaces:**
- Consumes: `AstIcon.EYE / EYE_OFF`（Task 1）。
- Produces: `Input.TEXT=0 / PASSWORD=1`，构造器 `new Input(String placeholder, int type)`（原单参构造器保留）。Task 7 demo 依赖。

- [ ] **Step 1: 写失败断言（RED）**

在 `Input.selfCheck()` 的档位断言之后追加：

```java
        Input pw = new Input("请输入密码", Input.PASSWORD);
        pw.setText("secret123");
        assert "secret123".equals(pw.getText()) : "password getText";
        final JPasswordField pf = (JPasswordField) findTextComponent(pw);
        assert pf.getEchoChar() != 0 : "masked by default, echo=" + pf.getEchoChar();
        final Throwable[] pwErr = {null};
        try {
            SwingUtilities.invokeAndWait(() -> eyeClickForTest(pw));
            assert pf.getEchoChar() == 0 : "eye toggle should show plaintext";
            SwingUtilities.invokeAndWait(() -> eyeClickForTest(pw));
            assert pf.getEchoChar() != 0 : "eye toggle should mask again";
        } catch (Throwable t) { pwErr[0] = t; }
        if (pwErr[0] != null) throw new RuntimeException(pwErr[0]);
```

并在 `selfCheck()` 之外加两个测试辅助（`clearBtnClickForTest` 之后）：

```java
    /** 测试辅助：找到 Input 内的文本组件（JTextField 或 JPasswordField）。 */
    private static JTextComponent findTextComponent(Input in) {
        for (Component c : in.getComponents())
            if (c instanceof JTextComponent) return (JTextComponent) c;
        throw new AssertionError("text component not found in Input");
    }

    /** 测试辅助：向密码框的眼睛按钮派发按下事件。 */
    private static void eyeClickForTest(Input in) {
        for (Component c : in.getComponents()) {
            if (c instanceof JPanel) {
                for (Component cc : ((JPanel) c).getComponents()) {
                    if (cc instanceof JPanel) {
                        boolean hasIcon = false;
                        for (Component ccc : ((JPanel) cc).getComponents()) if (ccc instanceof AstIcon) hasIcon = true;
                        if (hasIcon) {
                            cc.dispatchEvent(new java.awt.event.MouseEvent(cc, java.awt.event.MouseEvent.MOUSE_PRESSED,
                                    System.currentTimeMillis(), 0, 8, 8, 1, false));
                            return;
                        }
                    }
                }
            }
        }
        throw new AssertionError("eye button not found in Input");
    }
```

需要 `import javax.swing.text.JTextComponent;`。

- [ ] **Step 2: 运行验证失败**

Run: 编译命令
Expected: 编译错误 `cannot find symbol: constructor Input(String,int)`

- [ ] **Step 3: 实现**

(a) 类头加常量与字段：

```java
    public static final int TEXT = 0, PASSWORD = 1;
    private static final Color ICON_COLOR = new Color(0x606266);   // ≥7:1
    private static final Color ICON_HOVER = new Color(0x303133);   // hover 加深，对比单调上升
    private final boolean password;
    private JPanel eyeBtn;
    private AstIcon eyeIcon;
    private boolean pwVisible = false;
```

(b) 构造器改为双参 + 委托（替换原 `public Input(String placeholder) {` 整段开头）：

```java
    public Input(String placeholder) { this(placeholder, TEXT); }

    public Input(String placeholder, int type) {
        this.placeholder = placeholder;
        this.password = (type == PASSWORD);
        setOpaque(false);
        setLayout(new BorderLayout());
        field = password ? createPasswordField() : createTextField();
        field.setOpaque(false);
        ...（后续原样：DocumentListener、add(field, CENTER)、clearBtn、east、focus、mouse）
```

其中 field 的创建抽成两个工厂方法（放在构造器之后）：

```java
    private JTextField createTextField() {
        return new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintPlaceholder(g, this);
            }
        };
    }

    private JPasswordField createPasswordField() {
        JPasswordField pf = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintPlaceholder(g, this);
            }
        };
        pf.setEchoChar('\u25cf'); // ●
        return pf;
    }

    private void paintPlaceholder(Graphics g, JTextComponent c) {
        if (!hasText && !c.isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(ElementTheme.TEXT_PLACEHOLDER);
            g2.setFont(c.getFont());
            FontMetrics fm = g2.getFontMetrics();
            Insets ins = c.getBorder() != null ? c.getBorder().getBorderInsets(c) : new Insets(0, 0, 0, 0);
            g2.drawString(placeholder, ins.left, (c.getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }
```

（原匿名 JTextField 里手绘 placeholder 的整段代码删除，`ins.left` 取代硬编码的 x=12，随档位内边距联动。）

(c) east 面板段，密码模式加眼睛按钮（`east.add(clearBtn);` 之前）：

```java
        if (password) {
            eyeIcon = new AstIcon(AstIcon.EYE, ICON_COLOR, 16);
            eyeBtn = new JPanel(new GridBagLayout());
            eyeBtn.setOpaque(false);
            eyeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            eyeBtn.add(eyeIcon);
            eyeBtn.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    pwVisible = !pwVisible;
                    ((JPasswordField) field).setEchoChar(pwVisible ? (char) 0 : '\u25cf');
                    eyeIcon.setType(pwVisible ? AstIcon.EYE_OFF : AstIcon.EYE);
                }
                public void mouseEntered(MouseEvent e) { eyeIcon.setColor(ICON_HOVER); }
                public void mouseExited(MouseEvent e)  { eyeIcon.setColor(ICON_COLOR); }
            });
            east.add(eyeBtn);
        }
```

`east.addMouseListener(m);` 保持（鼠标移入眼睛区维持 hovering，× 不淡出）。

(d) `getText()` 改为：

```java
    public String getText() {
        return password ? new String(((JPasswordField) field).getPassword()) : field.getText();
    }
```

- [ ] **Step 4: 运行验证通过（GREEN）**

Run: 编译命令 + `java -ea ... org.swelement.ui.Input`
Expected: `Input self-check OK`

- [ ] **Step 5: Commit**

```bash
git add src/org/swelement/ui/Input.java
git commit -m "feat(Input): 密码模式（JPasswordField + 眼睛切换明文/掩码，EYE/EYE_OFF）"
```

---

### Task 4: Input 前后缀图标

**Files:**
- Modify: `src/org/swelement/ui/Input.java`

**Interfaces:**
- Consumes: `AstIcon` 常量（SEARCH/USER/SETTING 等）。
- Produces: `setPrefixIcon(int iconType)`、`setSuffixIcon(int iconType)`。Task 7 demo 依赖。

- [ ] **Step 1: 写失败断言（RED）**

`Input.selfCheck()` 末尾（`System.out.println("Input self-check OK");` 之前）追加：

```java
        Input pi = new Input("搜索");
        pi.setPrefixIcon(AstIcon.SEARCH);
        assert countAstIcons(pi) == 1 : "prefix icon added, count=" + countAstIcons(pi);
        Input si = new Input("");
        si.setText("x");
        si.setSuffixIcon(AstIcon.SETTING);
        assert countAstIcons(si) == 1 : "suffix icon added, count=" + countAstIcons(si);
        // 重复设置不叠加
        pi.setPrefixIcon(AstIcon.USER);
        assert countAstIcons(pi) == 1 : "prefix icon replaced, count=" + countAstIcons(pi);
```

辅助方法（`eyeClickForTest` 之后）：

```java
    /** 测试辅助：统计 Input 子树中的 AstIcon 数量。 */
    private static int countAstIcons(Container c) {
        int n = 0;
        for (Component cc : c.getComponents()) {
            if (cc instanceof AstIcon) n++;
            if (cc instanceof Container) n += countAstIcons((Container) cc);
        }
        return n;
    }
```

- [ ] **Step 2: 运行验证失败**

Run: 编译命令
Expected: 编译错误 `cannot find symbol: method setPrefixIcon(int)`

- [ ] **Step 3: 实现**

(a) 把构造器里局部变量 `JPanel east` 提升为字段：类头加 `private final JPanel east;`，构造器里改 `east = new JPanel(new GridBagLayout());`（`east.setOpaque(false)` 等行不变，去掉类型声明）。鼠标适配器同样提升为字段：`private final MouseAdapter hoverKeeper` 改为在构造器里 `hoverKeeper = new MouseAdapter() {...}` 赋值（原局部 `m` 改名 `hoverKeeper`，`field.addMouseListener(hoverKeeper)` 等四处引用同步改名）。

(b) 类头加字段：

```java
    private JPanel west;
    private AstIcon prefixIcon, suffixIcon;
```

(c) 新增公开方法（`setSize(int tier)` 之后）：

```java
    /** 前缀图标（AstIcon 常量）。静态装饰，不可点。 */
    public void setPrefixIcon(int iconType) {
        if (west == null) {
            west = new JPanel(new GridBagLayout());
            west.setOpaque(false);
            west.setBorder(BorderFactory.createEmptyBorder(0, TIER_HPAD[tier], 0, 4));
            west.addMouseListener(hoverKeeper);
            add(west, BorderLayout.WEST);
        }
        if (prefixIcon != null) west.remove(prefixIcon);
        prefixIcon = new AstIcon(iconType, ICON_COLOR, 16);
        west.add(prefixIcon);
        revalidate(); repaint();
    }

    /** 后缀图标（AstIcon 常量），显示在清空按钮左侧。 */
    public void setSuffixIcon(int iconType) {
        if (suffixIcon != null) east.remove(suffixIcon);
        suffixIcon = new AstIcon(iconType, ICON_COLOR, 16);
        east.add(suffixIcon, 0);
        revalidate(); repaint();
    }
```

注意：密码模式的眼睛按钮也在 east 里，`east.add(suffixIcon, 0)` 使 suffix 在最前，视觉顺序为 [suffix][eye][clear]。

- [ ] **Step 4: 运行验证通过（GREEN）**

Run: 编译命令 + `java -ea ... org.swelement.ui.Input`
Expected: `Input self-check OK`

- [ ] **Step 5: Commit**

```bash
git add src/org/swelement/ui/Input.java
git commit -m "feat(Input): 前后缀图标 setPrefixIcon/setSuffixIcon（复用 AstIcon，0x606266 达标对比度）"
```

---

### Task 5: TextArea 独立组件

**Files:**
- Create: `src/org/swelement/ui/TextArea.java`
- Modify: `build.bat`（SOURCES 列表 + 自检块）

**Interfaces:**
- Consumes: `Animator`、`Easing`、`ElementTheme`（Input 同款边框/光晕配方）。
- Produces: `new TextArea(String placeholder, int rows, int columns)`，`getText()/setText(String)/setEnabled(boolean)`。Task 7 demo 与 build.bat 依赖。

- [ ] **Step 1: 写组件与失败断言（RED——文件不存在即失败态）**

新建 `src/org/swelement/ui/TextArea.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * 多行文本输入 — Element UI textarea 移植。
 * 透明 JScrollPane 包 JTextArea（自动换行），复用 Input 的边框/聚焦光晕/占位符配方。
 */
public class TextArea extends JPanel {
    private final JTextArea area;
    private final JScrollPane scroll;
    private final Animator focusAnim = new Animator(200, Easing::easeInOut, v -> { focus = v; repaint(); });
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private float focus, hover;
    private boolean hasText;
    private final String placeholder;

    public TextArea(String placeholder, int rows, int columns) {
        this.placeholder = placeholder;
        setOpaque(false);
        setLayout(new BorderLayout());
        area = new JTextArea(rows, columns) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!hasText && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(ElementTheme.TEXT_PLACEHOLDER);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(placeholder, 0, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                    g2.dispose();
                }
            }
        };
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(ElementTheme.FONT);
        area.setForeground(ElementTheme.TEXT_MAIN);
        area.setBorder(new EmptyBorder(8, 12, 8, 8));
        area.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { hasText = !area.getText().isEmpty(); repaint(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { hasText = !area.getText().isEmpty(); repaint(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
        scroll = new JScrollPane(area);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        add(scroll, BorderLayout.CENTER);

        area.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { focusAnim.go(focus, 1f); }
            public void focusLost(FocusEvent e)   { focusAnim.go(focus, 0f); }
        });
        MouseAdapter m = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); }
        };
        addMouseListener(m);
        scroll.addMouseListener(m);
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
        if (focus > 0) {
            g2.setColor(new Color(64, 158, 255, Math.round(50 * focus)));
            g2.setStroke(new BasicStroke(4f));
            g2.draw(shape);
        }
        g2.dispose();
    }

    public String getText() { return area.getText(); }
    public void setText(String t) { area.setText(t); }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        area.setEnabled(enabled);
    }

    static void selfCheck() {
        TextArea ta = new TextArea("请输入内容", 3, 20);
        assert ta.getText().isEmpty();
        ta.setText("hello");
        assert "hello".equals(ta.getText());
        TextArea tall = new TextArea("p", 8, 20);
        assert tall.getPreferredSize().height > ta.getPreferredSize().height
                : "rows drive height: " + tall.getPreferredSize().height + " vs " + ta.getPreferredSize().height;
        assert ta.getPreferredSize().height >= 3 * 20
                : "3-row taller than 3 single lines, got " + ta.getPreferredSize().height;
        ta.setEnabled(false);
        assert !ta.isEnabled() && !ta JTextArea_enabled(ta);
        ta.setEnabled(true);
        // 离屏绘制不抛异常（含占位符路径）
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(() -> {
            TextArea p = new TextArea("占位", 3, 20);
            p.setBounds(0, 0, 240, 80);
            p.doLayout();
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(240, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            try { p.paint(gg); } finally { gg.dispose(); }
        }); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("TextArea self-check OK");
    }

    private static boolean JTextArea_enabled(TextArea ta) { return ta.area.isEnabled(); }

    public static void main(String[] args) { selfCheck(); }
}
```

- [ ] **Step 2: 运行验证**

Run: 编译命令 + `java -ea ... org.swelement.ui.TextArea`
Expected: `TextArea self-check OK`（若断言失败按输出调整 rows/height 阈值——JTextArea 行高随字体，3 行约 60+px）

- [ ] **Step 3: 注册进 build.bat**

SOURCES 列表 `src\org\swelement\ui\Input.java ^` 之后加一行：

```
src\org\swelement\ui\TextArea.java ^
```

自检块（Input 自检之后）加：

```bat
echo --- TextArea self-check ---
java -ea -cp out org.swelement.ui.TextArea
if %ERRORLEVEL% NEQ 0 ( echo TextArea self-check FAILED & exit /b 1 )
```

- [ ] **Step 4: Commit**

```bash
git add src/org/swelement/ui/TextArea.java build.bat
git commit -m "feat(TextArea): 多行文本输入组件（复用 Input 边框/光晕/占位符配方，lineWrap）"
```

---

### Task 6: Select 可清空重写

**Files:**
- Modify: `src/org/swelement/ui/Select.java`

**Interfaces:**
- Consumes: `CloseButton`（setAlpha/setInteractive/setOnClose/setButtonSize）。
- Produces: Select 单选有值且 hover 时 × 淡入并替换箭头，点击清空。行为 API 不变。

- [ ] **Step 1: 写失败断言（RED）**

`Select.selfCheck()` 的 matches 断言之后追加：

```java
        // 可清空：hover 淡入 ×，点击清空选择（复用 Input 的测试配方）
        final Select sel = new Select(new String[]{"北京", "上海", "广州"});
        sel.setSelectedIndex(1);
        assert "上海".equals(sel.getSelectedValue());
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                sel.setSize(280, 40);
                sel.doLayout();
                sel.dispatchEvent(new java.awt.event.MouseEvent(sel, java.awt.event.MouseEvent.MOUSE_ENTERED,
                        System.currentTimeMillis(), 0, 10, 10, 0, false));
            });
            Thread.sleep(300);
            SwingUtilities.invokeAndWait(() -> clearBtnClickForTest(sel));
            Thread.sleep(50);
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        assert sel.getSelectedValue() == null : "clear should empty selection, got " + sel.getSelectedValue();
```

辅助（selfCheck 之外）：

```java
    /** 测试辅助：向 Select 内的 CloseButton 派发按下事件。 */
    private static void clearBtnClickForTest(Select sel) {
        for (Component c : sel.getComponents()) {
            if (c instanceof JPanel) {
                for (Component cc : ((JPanel) c).getComponents()) {
                    if (cc instanceof CloseButton) {
                        cc.dispatchEvent(new java.awt.event.MouseEvent(cc, java.awt.event.MouseEvent.MOUSE_PRESSED,
                                System.currentTimeMillis(), 0, 8, 8, 1, false));
                        return;
                    }
                }
            }
        }
        throw new AssertionError("CloseButton not found in Select");
    }
```

- [ ] **Step 2: 运行验证失败**

Run: 编译命令 + `java -ea ... org.swelement.ui.Select`
Expected: `AssertionError: CloseButton not found in Select`（当前是手绘 ×，无 CloseButton）

- [ ] **Step 3: 实现**

(a) 类头加字段：

```java
    private final CloseButton clearBtn = new CloseButton(16);
    private final Animator clearAnim = new Animator(150, Easing::easeInOut, v -> { clearVis = v; syncClear(); repaint(); });
    private float clearVis;
    private boolean hovering;
```

(b) 构造器 `add(center, BorderLayout.CENTER);` 之后加：

```java
        // 可清空 ×（复用 Input 批次 1 的 east 面板配方，替代手绘 × + 坐标命中）
        clearBtn.setAlpha(0f);
        clearBtn.setInteractive(false);
        clearBtn.setOnClose(() -> {
            if (multiple) return;
            selected.clear();
            updateDisplay();
            rebuildList(null);
            repaint();
        });
        JPanel east = new JPanel(new GridBagLayout());
        east.setOpaque(false);
        east.setBorder(new EmptyBorder(0, 4, 0, 8));
        east.add(clearBtn);
        add(east, BorderLayout.EAST);

        MouseAdapter hoverM = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovering = true;  updateClear(); }
            public void mouseExited(MouseEvent e)  { hovering = false; updateClear(); }
        };
        addMouseListener(hoverM); display.addMouseListener(hoverM); tagsPanel.addMouseListener(hoverM); east.addMouseListener(hoverM);
        if (field != null) field.addMouseListener(hoverM);
        east.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { if (!isEnabled()) return; togglePopup(); }
        });
```

(c) 原 `click` 适配器删掉坐标分支：

```java
        MouseAdapter click = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                togglePopup();
            }
        };
```

(d) 新增私有方法（`togglePopup()` 之前）：

```java
    private void updateClear() {
        float target = (!multiple && !selected.isEmpty() && hovering && isEnabled()) ? 1f : 0f;
        clearAnim.go(clearVis, target);
    }

    private void syncClear() {
        clearBtn.setAlpha(clearVis);
        clearBtn.setInteractive(clearVis > 0.5f);
    }
```

(e) `updateDisplay()` 末尾（`tagsPanel.repaint();` 之后）加 `updateClear();`。

(f) `paintComponent`：删除 `if (!multiple && !selected.isEmpty())` 的手绘 × 整块；箭头绘制块包进 `if (clearVis < 0.5f) { ... }`（× 淡入过半即隐藏箭头，对齐 Element「× 替换箭头」）：

```java
        if (clearVis < 0.5f) {
            float ax = getWidth() - 18f, ay = getHeight() / 2f;
            Graphics2D a2 = (Graphics2D) g2.create();
            a2.rotate(Math.PI * arrowAngle, ax, ay);
            a2.setColor(new Color(0xC0C4CC));
            a2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            a2.drawLine(Math.round(ax - 4), Math.round(ay - 1), Math.round(ax), Math.round(ay + 2));
            a2.drawLine(Math.round(ax + 4), Math.round(ay - 1), Math.round(ax), Math.round(ay + 2));
            a2.dispose();
        }
```

- [ ] **Step 4: 运行验证通过（GREEN）**

Run: 编译命令 + `java -ea ... org.swelement.ui.Select`
Expected: `Select self-check OK`

- [ ] **Step 5: Commit**

```bash
git add src/org/swelement/ui/Select.java
git commit -m "refactor(Select): 可清空 × 重写为 CloseButton 配方（消灭手绘 × + 坐标命中测试残留）"
```

---

### Task 7: Demo 更新 + 全量回归

**Files:**
- Modify: `src/org/swelement/demo/InputDemo.java`
- Modify: `src/org/swelement/demo/SelectDemo.java`
- Modify: `src/org/swelement/demo/BadgeDemo.java:54`（Input 手动高度）

**Interfaces:**
- Consumes: Task 2/3/4 的 Input API、Task 5 的 TextArea、Task 6 的 Select 行为。

- [ ] **Step 1: InputDemo 更新**

- 四处 `setPreferredSize(new Dimension(W, 40))` 全部删除，改 `name.setColumns(16)` / `email.setColumns(24)` / `search.setColumns(20)` / `dis.setColumns(16)` / `target.setColumns(18)`（宽度由列数决定，高度由档位决定）。
- 新增三个分区（放在 p2 之后、`root.add(p1)` 之前构建）：

```java
            // 尺寸档位
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            p3.setBorder(new TitledBorder("尺寸档位（large 40 / default 32 / small 28）"));
            Input iL = new Input("大型输入框"); iL.setSize(Input.SIZE_LARGE); iL.setColumns(12);
            Input iD = new Input("默认输入框"); iD.setColumns(12);
            Input iS = new Input("小型输入框"); iS.setSize(Input.SIZE_SMALL); iS.setColumns(12);
            p3.add(iL); p3.add(iD); p3.add(iS);

            // 密码 + 图标
            JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            p4.setBorder(new TitledBorder("密码框（眼睛切换明文）与前后缀图标"));
            Input pw = new Input("请输入密码", Input.PASSWORD); pw.setColumns(14);
            Input pfx = new Input("搜索关键词"); pfx.setPrefixIcon(org.swelement.ui.AstIcon.SEARCH); pfx.setColumns(14);
            Input sfx = new Input("带后缀图标"); sfx.setSuffixIcon(org.swelement.ui.AstIcon.SETTING); sfx.setColumns(14);
            p4.add(pw); p4.add(pfx); p4.add(sfx);

            // 文本域
            JPanel p5 = new JPanel(new BorderLayout(8, 8));
            p5.setBorder(new TitledBorder("文本域 TextArea（自动换行，纵向滚动按需出现）"));
            org.swelement.ui.TextArea ta = new org.swelement.ui.TextArea("请输入多行备注内容…", 4, 32);
            p5.add(ta, BorderLayout.CENTER);
```

组装段相应改为 `root.add(p1); root.add(Box.createVerticalStrut(8)); root.add(p2); root.add(Box.createVerticalStrut(8)); root.add(p3); root.add(Box.createVerticalStrut(8)); root.add(p4); root.add(Box.createVerticalStrut(8)); root.add(p5);`，窗口标题改为 `"Input Demo - 档位、密码、图标、文本域、清空、禁用"`。

- [ ] **Step 2: SelectDemo 更新（可清空演示区）**

p4 之后新增 p5 并加入组装（`root.add(p4);` 之后 `root.add(Box.createVerticalStrut(8)); root.add(p5);`）：

```java
            // 可清空
            JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
            p5.setBorder(new TitledBorder("可清空（单选有值时悬停，箭头变 ×，点击清除）"));
            Select clearable = new Select(new String[]{"北京", "上海", "广州", "深圳"});
            clearable.setPreferredSize(new Dimension(280, 40));
            clearable.setSelectedIndex(1);
            JLabel clearEcho = new JLabel("当前值：上海");
            clearEcho.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            clearEcho.setForeground(new Color(0x606266));
            Button showVal = new Button("查看当前值", Button.DEFAULT, true);
            showVal.addActionListener(e -> {
                Object v = clearable.getSelectedValue();
                clearEcho.setText("当前值：" + (v == null ? "(已清空)" : v.toString()));
            });
            p5.add(clearable); p5.add(showVal); p5.add(clearEcho);
```

- [ ] **Step 3: BadgeDemo 的 Input 手动高度**

读 `src/org/swelement/demo/BadgeDemo.java` 第 50~60 行，把 `input.setPreferredSize(new Dimension(220, 40));` 改为 `input.setColumns(16);`（高度交给档位）。

- [ ] **Step 4: 全量编译 + 全部自检回归**

Run（bash）:

```bash
cd "D:/Program Files/code/swing-element-ui" && javac -nowarn -encoding UTF-8 -d .workbuddy/probe/out $(find src -name '*.java') && for c in org.swelement.ui.AstIcon org.swelement.ui.Input org.swelement.ui.TextArea org.swelement.ui.Select org.swelement.ui.CloseButton org.swelement.ui.Tag org.swelement.ui.Alert; do echo "== $c"; java -ea -Djava.awt.headless=false -cp .workbuddy/probe/out $c 2>&1 | tail -1; done
```

再跑其余全部自检（build.bat 里的 31 个清单逐一 `java -ea -cp .workbuddy/probe/out <类>`）。
Expected: 全部输出 `self-check OK`。

- [ ] **Step 5: Commit**

```bash
git add src/org/swelement/demo/InputDemo.java src/org/swelement/demo/SelectDemo.java src/org/swelement/demo/BadgeDemo.java build.bat
git commit -m "demo: Input 档位/密码/图标/文本域与 Select 可清空演示；移除手动高度"
```

---

## Self-Review 结论

- 覆盖检查：spec 六节 → Task 1(EYE_OFF)、2(档位)、3(密码)、4(图标)、5(TextArea)、6(Select 重写)、7(demo+build.bat+回归) 一一对应。✓
- 占位符扫描：无 TBD/TODO；所有代码块完整。✓
- 类型一致性：`setSize(int tier)` 与 Tag 同名；CloseButton 用 `setButtonSize/setAlpha/setInteractive/setOnClose`，均与现有 API 一致。✓
- TextArea selfCheck 中 `assert !ta.isEnabled() && !ta JTextArea_enabled(ta);` 一行有语法笔误风险——执行时写成两条断言：`assert !ta.isEnabled() : "panel disabled"; assert !ta.area.isEnabled() : "area disabled";`（area 需改为可访问或保留辅助方法）。
