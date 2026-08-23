# 批次 1：CloseButton + 关闭逻辑重写 + Tag 展现增强 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新建公共 CloseButton 可点击组件，重写 Tag/Alert/Input/AstDialog 的关闭逻辑（删除坐标命中测试），并为 Tag 补齐 effect/尺寸展现。

**Architecture:** CloseButton 为自绘 JComponent（矢量 × + hover 动画），各组件以子组件方式组合放置，父组件动画通过 `setAlpha`/`setInteractive` 联动。Tag 从纯自绘重构为「自绘 + 子组件叠加（null 布局）」。

**Tech Stack:** Java 8 Swing，零外部依赖，自检用 `java -ea` 断言（无 JUnit）。

**Spec:** `docs/superpowers/specs/2026-08-23-close-button-batch1-design.md`

## Global Constraints

- JDK 8 兼容：不用 `var`、`List.of`、`java.util.function` 之外的 JDK8+ API 慎用（项目已用 lambda/方法引用，可用）
- 所有文字/× 符号颜色对比度 ≥ 4.5:1（WCAG AA），用 `ElementTheme.assertContrast` 断言；例外：dark effect 白字彩色底（Element 标准实心，需标注）
- 不改现有公共构造函数签名（`Tag(String,int,boolean)`、`Alert(int,String,String,boolean)`、`Input(String)`）
- 编译命令：`cmd //c build.bat`（在项目根目录，Git Bash 下）；单类自检：`java -ea -cp out org.swelement.ui.Xxx`
- 构建产物在 `out/`，源码 UTF-8

**Spec 与本计划的两处 reconcile（以对比度约束为准，AGENTS.md 最高优先级）：**
1. CloseButton 默认色从 spec 表格的 `0x909399`/`0x409EFF`（对白底约 3:1，不达标）改为 `0x606266`（≥7:1）/ hover `0x1d6fb5`（primary 深变体，≥4.5:1）——与 spec「selfCheck 用深灰变体验证」一致
2. spec 假设 AstMessageBox 复用 `makeCard`，实际 grep 确认**没有**（AstMessageBox 不引用 makeCard）；AstDialog 加 × 不影响 AstMessageBox，build.bat 中其 selfCheck 仍作为回归验证运行

---

### Task 1: CloseButton 公共组件

**Files:**
- Create: `src/org/swelement/ui/CloseButton.java`
- Modify: `build.bat`（SOURCES 列表 Checkbox 行后加 CloseButton 行）

**Interfaces:**
- Consumes: `Animator(int, Easing, Listener)`、`Easing::easeInOut`、`ElementTheme.lerp`
- Produces（后续任务全部依赖）: `new CloseButton()` / `new CloseButton(int size)`、`addActionListener(ActionListener)`、`setColor(Color)`、`setHoverColor(Color)`、`setAlpha(float)`、`setInteractive(boolean)`

- [ ] **Step 1: 创建含 selfCheck 的 CloseButton（selfCheck 即测试，先建骨架使断言可编译）**

完整实现 `src/org/swelement/ui/CloseButton.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 公共可点击关闭按钮：矢量 × 符号 + hover 圆形底色淡入。
 * 所有可关闭组件（Tag/Alert/Input/AstDialog 等）统一使用，替代"自绘 × + 坐标命中测试"。
 * 对比度：默认色 0x606266 对白底 >= 7:1（WCAG AA），hover 色 0x1d6fb5 为 primary 深变体（>= 4.5:1）。
 */
public class CloseButton extends JComponent {
    private final int size;
    private Color color = new Color(0x606266);
    private Color hoverColor = new Color(0x1d6fb5);
    private float hover;
    private float alpha = 1f;
    private boolean interactive = true;
    private final List<ActionListener> listeners = new ArrayList<ActionListener>();
    private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });

    public CloseButton() { this(24); }

    public CloseButton(int size) {
        this.size = size;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (interactive) hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); }
            public void mouseClicked(MouseEvent e) { fireClicked(); }
        });
    }

    public void addActionListener(ActionListener l) { listeners.add(l); }

    /** × 符号默认颜色。 */
    public void setColor(Color c) { this.color = c; repaint(); }

    /** hover 时 × 符号颜色。 */
    public void setHoverColor(Color c) { this.hoverColor = c; repaint(); }

    /** 整体透明度 0~1，供父组件淡入淡出动画驱动。 */
    public void setAlpha(float a) {
        this.alpha = Math.max(0f, Math.min(1f, a));
        repaint();
    }

    /** false 时不响应点击且不拦截父组件鼠标事件（contains 返回 false）。 */
    public void setInteractive(boolean b) {
        this.interactive = b;
        if (!b) hoverAnim.go(hover, 0f);
        repaint();
    }

    public boolean isInteractive() { return interactive; }

    private void fireClicked() {
        if (!interactive) return;
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "close");
        for (ActionListener l : new ArrayList<ActionListener>(listeners)) l.actionPerformed(ev);
    }

    @Override
    public boolean contains(int x, int y) {
        return interactive ? super.contains(x, y) : false;
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(size, size); }

    @Override
    public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override
    protected void paintComponent(Graphics g) {
        int a = Math.round(255 * alpha);
        if (a <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (hover > 0) { // hover 圆形底色淡入（约 6% 黑）
            g2.setColor(new Color(0, 0, 0, Math.round(16 * hover * alpha)));
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
        }
        Color c = ElementTheme.lerp(color, hoverColor, hover);
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), a));
        float len = size * 0.4f; // × 半臂长
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        g2.setStroke(new BasicStroke(Math.max(1.4f, size / 14f)));
        g2.drawLine(Math.round(cx - len), Math.round(cy - len), Math.round(cx + len), Math.round(cy + len));
        g2.drawLine(Math.round(cx - len), Math.round(cy + len), Math.round(cx + len), Math.round(cy - len));
        g2.dispose();
    }

    static void selfCheck() {
        CloseButton cb = new CloseButton();
        Dimension pd = cb.getPreferredSize();
        assert pd.width == 24 && pd.height == 24 : "default 24x24, got " + pd;
        CloseButton cb2 = new CloseButton(18);
        assert cb2.getPreferredSize().width == 18 : "custom size 18";

        // 点击触发监听
        final int[] fired = {0};
        cb.addActionListener(e -> fired[0]++);
        cb.addMouseListener(new MouseAdapter() {});
        cb.setSize(24, 24);
        cb.dispatchEvent(new MouseEvent(cb, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 12, 12, 1, false));
        assert fired[0] == 1 : "click should fire listener, fired=" + fired[0];

        // setInteractive(false)：contains false + 点击不触发
        cb.setInteractive(false);
        assert !cb.contains(12, 12) : "non-interactive contains must be false";
        cb.dispatchEvent(new MouseEvent(cb, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 12, 12, 1, false));
        assert fired[0] == 1 : "non-interactive click must not fire";
        cb.setInteractive(true);
        assert cb.contains(12, 12) : "interactive contains true";

        // setAlpha 边界
        cb.setAlpha(2f); assert true; // 不抛异常即通过（内部 clamp）
        cb.setAlpha(-1f);

        // 对比度：默认色与 hover 色对白底（浅色场景）达标
        ElementTheme.assertContrast(new Color(0x606266), Color.WHITE, "CloseButton default on white");
        ElementTheme.assertContrast(new Color(0x1d6fb5), Color.WHITE, "CloseButton hover on white");

        System.out.println("CloseButton self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
```

- [ ] **Step 2: build.bat 加入源文件**

在 `build.bat` SOURCES 中 `src\org\swelement\ui\Checkbox.java ^` 行后插入：

```
src\org\swelement\ui\CloseButton.java ^
```

- [ ] **Step 3: 编译并运行自检**

Run: `cd "D:\Program Files\code\swing-element-ui" && cmd //c build.bat 2>&1 | tail -5`
Expected: `BUILD OK`（build.bat 后续 selfCheck 仍全绿）
Run: `java -ea -cp out org.swelement.ui.CloseButton`
Expected: `CloseButton self-check OK`

- [ ] **Step 4: Commit**

```bash
git add src/org/swelement/ui/CloseButton.java build.bat
git commit -m "feat: add CloseButton shared clickable close component with hover animation and WCAG-compliant colors"
```

---

### Task 2: Tag 重构 — CloseButton 关闭 + effect + 尺寸

**Files:**
- Modify: `src/org/swelement/ui/Tag.java`（全文重写）
- Modify: `src/org/swelement/demo/TagDemo.java`（删除坐标命中 hack，新增 effect/尺寸展示）

**Interfaces:**
- Consumes: Task 1 的 CloseButton 全部 API
- Produces: `Tag(String, int, boolean)`（不变）、`setEffect(int)`（EFFECT_DARK/LIGHT/PLAIN）、`setSize(int)`（SIZE_LARGE/DEFAULT/SMALL）、`setOnClosed(Runnable)`（× 点击后的回调，替代 Demo 中的坐标监听）、`close(Runnable)`（保留）

- [ ] **Step 1: 重写 Tag.java**

全文替换为：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;

public class Tag extends JComponent {
    public static final int PRIMARY = 0, SUCCESS = 1, WARNING = 2, DANGER = 3, INFO = 4;
    public static final int EFFECT_DARK = 0, EFFECT_LIGHT = 1, EFFECT_PLAIN = 2;
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;

    private static final float[] SIZE_FONT = {14f, 12f, 12f};
    private static final int[] SIZE_VPAD = {8, 4, 2};
    private static final int[] SIZE_HPAD = {16, 10, 8};
    private static final int[] CLOSE_SIZE = {20, 18, 16};
    private static final int CLOSE_GAP = 4;
    private static final int CLOSE_RIGHT = 6;

    private static final Color[] LIGHT_BG = {new Color(0xECF5FF), new Color(0xF0F9EB), new Color(0xFDF6EC), new Color(0xFEF0F0), new Color(0xF4F4F5)};
    private static final Color[] LIGHT_BORDER = {new Color(0xD9ECFF), new Color(0xE1F3D8), new Color(0xFAECD8), new Color(0xFDE2E2), new Color(0xE9E9EB)};
    private static final Color[] DARK_BG = {ElementTheme.PRIMARY, ElementTheme.SUCCESS, ElementTheme.WARNING, ElementTheme.DANGER, ElementTheme.INFO};
    // 深色文字变体，浅色/白底上对比度 >= 4.5:1（取值同 Button PLAIN_FG）
    private static final Color[] DEEP_FG = {new Color(0x1d6fb5), new Color(0x2d6b18), new Color(0x955d12), new Color(0xb83232), new Color(0x606266)};

    private Runnable onClosed;
    private int origW, origH;
    private int effect = EFFECT_LIGHT;
    private int size = SIZE_DEFAULT;
    private CloseButton closeBtn;

    private final Animator closeAnim = new Animator(200, Easing::easeInOut, v -> {
        float w = origW * (1 - v);
        setPreferredSize(new Dimension(Math.max(1, Math.round(w)), origH));
        revalidate();
        if (v >= 1f && onClosed != null) {
            Runnable r = onClosed;
            onClosed = null;
            r.run();
        }
        repaint();
    });
    private final int type;
    private final boolean closable;
    private String text;

    public Tag(String text, int type, boolean closable) {
        this.text = text;
        this.type = type;
        this.closable = closable;
        setOpaque(false);
        setLayout(null); // CloseButton 绝对定位，doLayout 摆放
    }

    public void setEffect(int effect) {
        this.effect = effect;
        updateCloseColors();
        repaint();
    }

    public void setSize(int size) {
        this.size = size;
        revalidate();
        repaint();
    }

    /** × 点击关闭动画完成后的回调（由 Demo 用于从容器移除）。 */
    public void setOnClosed(Runnable r) { this.onClosed = r; }

    public void setText(String t) {
        text = t;
        revalidate();
        repaint();
    }

    public String getText() { return text; }

    public void close(Runnable onClosed) {
        this.onClosed = onClosed;
        origW = getWidth();
        origH = getHeight();
        if (closeBtn != null) {
            closeBtn.setInteractive(false);
            closeBtn.setVisible(false);
        }
        closeAnim.go(0f, 1f);
    }

    private void updateCloseColors() {
        if (closeBtn == null) return;
        if (effect == EFFECT_DARK) {
            // 白色 × 在彩色实底上 —— Element 标准实心设计，对比度为例外（见 spec 标注）
            closeBtn.setColor(Color.WHITE);
            closeBtn.setHoverColor(Color.WHITE);
        } else {
            // light: 深色变体 × 对应浅色底；plain: 深色变体 × 白底。hover 统一 TEXT_MAIN
            closeBtn.setColor(DEEP_FG[type]);
            closeBtn.setHoverColor(ElementTheme.TEXT_MAIN);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (closable && closeBtn == null) {
            closeBtn = new CloseButton(CLOSE_SIZE[size]);
            closeBtn.addActionListener(e -> close(onClosed != null ? onClosed : (Runnable) () -> {}));
            add(closeBtn);
            updateCloseColors();
            revalidate();
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (closeBtn != null && closeBtn.isVisible()) {
            int s = CLOSE_SIZE[size];
            closeBtn.setBounds(getWidth() - CLOSE_RIGHT - s, (getHeight() - s) / 2, s, s);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg, fg, border;
        switch (effect) {
            case EFFECT_DARK:
                bg = DARK_BG[type]; fg = Color.WHITE; border = DARK_BG[type]; // 白字彩底：Element 标准实心，对比度例外
                break;
            case EFFECT_PLAIN:
                bg = Color.WHITE; fg = DEEP_FG[type]; border = DARK_BG[type];
                break;
            default: // EFFECT_LIGHT（默认，向后兼容）
                bg = LIGHT_BG[type]; fg = DEEP_FG[type]; border = LIGHT_BORDER[type];
                break;
        }
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
        g2.setColor(fg);
        Font f = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics(f);
        int rightInset = closable ? CLOSE_GAP + CLOSE_SIZE[size] + CLOSE_RIGHT : SIZE_HPAD[size];
        Shape oldClip = g2.getClip();
        g2.clipRect(0, 0, getWidth() - rightInset, getHeight()); // 文字不与 CloseButton 重叠
        g2.drawString(text, SIZE_HPAD[size], (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g2.setClip(oldClip);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Font f = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        FontMetrics fm = getFontMetrics(f);
        int w = SIZE_HPAD[size] + fm.stringWidth(text)
                + (closable ? CLOSE_GAP + CLOSE_SIZE[size] + CLOSE_RIGHT : SIZE_HPAD[size]);
        int h = Math.max(SIZE_VPAD[size] * 2 + fm.getHeight(), CLOSE_SIZE[size] + 8);
        return new Dimension(w, h);
    }

    static void selfCheck() {
        // 对比度：light 与 plain 各 type 深色文字变体 vs 对应背景
        for (int t = 0; t < 5; t++) {
            ElementTheme.assertContrast(DEEP_FG[t], LIGHT_BG[t], "tag light type=" + t);
            ElementTheme.assertContrast(DEEP_FG[t], Color.WHITE, "tag plain type=" + t);
        }
        // 可关闭 Tag 更宽（为 CloseButton 预留）
        Tag plain = new Tag("标签", Tag.PRIMARY, false);
        Tag closable = new Tag("标签", Tag.PRIMARY, true);
        assert closable.getPreferredSize().width > plain.getPreferredSize().width
                : "closable tag must reserve width for close button";
        // effect 切换不抛异常
        closable.setEffect(Tag.EFFECT_DARK);
        closable.setEffect(Tag.EFFECT_PLAIN);
        closable.setEffect(Tag.EFFECT_LIGHT);
        // 尺寸三档高度递减
        Tag l = new Tag("尺寸", Tag.INFO, false); l.setSize(Tag.SIZE_LARGE);
        Tag d = new Tag("尺寸", Tag.INFO, false);
        Tag s = new Tag("尺寸", Tag.INFO, false); s.setSize(Tag.SIZE_SMALL);
        assert l.getPreferredSize().height > d.getPreferredSize().height : "large > default height";
        assert d.getPreferredSize().height > s.getPreferredSize().height : "default > small height";
        // 加入窗口后 CloseButton 子组件存在且位于右侧
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame f = new JFrame();
                JPanel p = new JPanel();
                Tag c = new Tag("可关闭", Tag.SUCCESS, true);
                p.add(c);
                f.add(p);
                f.pack();
                assert c.getComponentCount() == 1 && c.getComponent(0) instanceof CloseButton
                        : "close button child present, count=" + c.getComponentCount();
                Component cb = c.getComponent(0);
                assert cb.getX() + cb.getWidth() <= c.getWidth() && cb.getX() > c.getWidth() / 2
                        : "close button on right side, x=" + cb.getX();
                f.dispose();
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("Tag self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
```

- [ ] **Step 2: 编译 + 运行 Tag 自检**

Run: `cd "D:\Program Files\code\swing-element-ui" && cmd //c build.bat 2>&1 | grep -E "BUILD|FAILED" && java -ea -cp out org.swelement.ui.Tag`
Expected: `BUILD OK`、`Tag self-check OK`（若 TagDemo 未同步更新会编译失败，本步连同 Step 3 的 TagDemo 一起改后验证）

- [ ] **Step 3: 重写 TagDemo.java（删除坐标命中 hack）**

全文替换为：

```java
package org.swelement.demo;

import org.swelement.ui.Button;
import org.swelement.ui.Tag;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TagDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Tag Demo - effect 三种效果、尺寸、可关闭（真实可点 CloseButton）");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 1. light 效果（默认，向后兼容）
            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            p1.setBorder(new TitledBorder("light 效果（浅底 + 深色文字，默认）"));
            p1.add(new Tag("Primary 主要", Tag.PRIMARY, false));
            p1.add(new Tag("Success 成功", Tag.SUCCESS, false));
            p1.add(new Tag("Warning 警告", Tag.WARNING, false));
            p1.add(new Tag("Danger 危险", Tag.DANGER, false));
            p1.add(new Tag("Info 信息", Tag.INFO, false));

            // 2. dark 效果（实色底白字）
            JPanel p1b = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            p1b.setBorder(new TitledBorder("dark 效果（实色底，Element 标准实心设计）"));
            for (int t = 0; t < 5; t++) {
                Tag tag = new Tag("dark-" + t, t, false);
                tag.setEffect(Tag.EFFECT_DARK);
                p1b.add(tag);
            }

            // 3. plain 效果（白底彩边）
            JPanel p1c = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            p1c.setBorder(new TitledBorder("plain 效果（白底 + 彩色边框）"));
            for (int t = 0; t < 5; t++) {
                Tag tag = new Tag("plain-" + t, t, false);
                tag.setEffect(Tag.EFFECT_PLAIN);
                p1c.add(tag);
            }

            // 4. 尺寸三档
            JPanel p1d = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            p1d.setBorder(new TitledBorder("尺寸三档（large / default / small）"));
            String[] names = {"large 大", "default 默认", "small 小"};
            int[] sizes = {Tag.SIZE_LARGE, Tag.SIZE_DEFAULT, Tag.SIZE_SMALL};
            for (int i = 0; i < 3; i++) {
                Tag tag = new Tag(names[i], Tag.PRIMARY, true);
                tag.setSize(sizes[i]);
                p1d.add(tag);
            }

            // 5. 可关闭标签区（CloseButton 点击关闭，动画后从容器移除）
            JPanel p2Wrap = new JPanel(new BorderLayout());
            p2Wrap.setBorder(new TitledBorder("可关闭标签（点击 × 观察宽度收缩动画，关闭后从面板移除）"));
            final JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            p2.setOpaque(true);
            p2.setBackground(Color.WHITE);
            List<String> initTags = java.util.Arrays.asList(
                    "🚀 Java", "⚛ React", "🎨 设计", "📊 数据可视化",
                    "🔧 DevOps", "🧪 测试", "☁️ 云计算", "🤖 AI/ML", "📱 移动端", "🌐 网络"
            );
            for (String s : initTags) {
                p2.add(makeClosableTag(s, p2.getComponentCount() % 5, p2));
            }
            p2Wrap.add(p2, BorderLayout.CENTER);

            // 6. 动态添加区
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            p3.setBorder(new TitledBorder("动态添加标签（点击按钮添加，类型循环）"));
            JTextField tf = new JTextField(16);
            tf.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            tf.setText("新标签");
            Button add = new Button("+ 添加可关闭标签", Button.PRIMARY, false);
            final int[] addIdx = {0};
            add.addActionListener(ev -> {
                String txt = tf.getText().trim();
                if (txt.isEmpty()) return;
                p2.add(makeClosableTag(txt, addIdx[0]++ % 5, p2));
                p2.revalidate();
            });
            Button clear = new Button("清空全部（带动画）", Button.WARNING, true);
            clear.addActionListener(ev -> {
                List<Component> tags = new ArrayList<Component>();
                for (Component c : p2.getComponents()) if (c instanceof Tag) tags.add(c);
                int delay = 0;
                for (Component c : tags) {
                    final Tag t = (Tag) c;
                    Timer timer = new Timer(delay, e -> t.close(() -> SwingUtilities.invokeLater(() -> {
                        p2.remove(t);
                        p2.revalidate();
                        p2.repaint();
                    })));
                    timer.setRepeats(false);
                    timer.start();
                    delay += 60;
                }
            });
            p3.add(new JLabel("标签文字:"));
            p3.add(tf);
            p3.add(add);
            p3.add(Box.createHorizontalStrut(20));
            p3.add(clear);

            root.add(p1);
            root.add(p1b);
            root.add(p1c);
            root.add(p1d);
            root.add(Box.createVerticalStrut(8));
            root.add(p2Wrap);
            root.add(Box.createVerticalStrut(8));
            root.add(p3);

            f.setContentPane(root);
            f.pack();
            f.setSize(Math.max(f.getWidth(), 860), f.getHeight());
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    /** 创建可关闭 Tag：× 点击即触发关闭动画并从父容器移除（组件内部 CloseButton，无坐标判断）。 */
    private static Tag makeClosableTag(String text, int type, final JPanel parent) {
        Tag t = new Tag(text, type, true);
        t.setOnClosed(() -> SwingUtilities.invokeLater(() -> {
            parent.remove(t);
            parent.revalidate();
            parent.repaint();
        }));
        return t;
    }
}
```

- [ ] **Step 4: 编译全量 + Tag 自检 + Demo 冒烟**

Run: `cd "D:\Program Files\code\swing-element-ui" && cmd //c build.bat 2>&1 | grep -E "BUILD|FAILED" && java -ea -cp out org.swelement.ui.Tag`
Expected: `BUILD OK`、`Tag self-check OK`
Run: `java -cp out org.swelement.demo.TagDemo`（人工确认窗口展示 effect/尺寸/可关闭，× 点击有收缩动画后关闭，Ctrl+C 退出）

- [ ] **Step 5: Commit**

```bash
git add src/org/swelement/ui/Tag.java src/org/swelement/demo/TagDemo.java
git commit -m "feat: Tag closable via CloseButton child component + effect (dark/light/plain) + size variants, removes coordinate hit-testing"
```

---

### Task 3: Alert 关闭重写

**Files:**
- Modify: `src/org/swelement/ui/Alert.java`

**Interfaces:**
- Consumes: Task 1 的 CloseButton（`new CloseButton(24)`、`setAlpha`、`setInteractive`、`addActionListener`）
- Produces: `Alert(int, String, String, boolean)` 不变；`close(Runnable)` 不变

- [ ] **Step 1: 修改 Alert.java**

变更点（其余代码保持不变）：

1. 删除构造函数中的 `setCursor(...)` + `addMouseListener(...)` 坐标命中块，替换为：

```java
        setLayout(null); // CloseButton 绝对定位
        if (closable) {
            closeBtn = new CloseButton(24);
            closeBtn.addActionListener(e -> close(() -> {}));
            add(closeBtn);
        }
```

2. 新增字段与 doLayout（放在 `private final boolean closable;` 之后）：

```java
    private CloseButton closeBtn;
```

```java
    @Override
    public void doLayout() {
        super.doLayout();
        if (closeBtn != null) {
            closeBtn.setBounds(getWidth() - 16 - 24, (getHeight() - 24) / 2, 24, 24);
        }
    }
```

3. 两个 Animator 回调末尾加 `syncClose();`（inAnim 与 outAnim 的 listener update 内），并新增方法：

```java
    /** 淡入淡出动画驱动 CloseButton 的 alpha 与可交互性。 */
    private void syncClose() {
        if (closeBtn == null) return;
        float a = inP * (1 - outP);
        closeBtn.setAlpha(a);
        closeBtn.setInteractive(a > 0.5f);
    }
```

4. `paintComponent` 删除 `if (closable) { ... }` 的 × 绘制块（约 99-106 行）。

5. 类末尾新增 selfCheck：

```java
    static void selfCheck() {
        Alert a = new Alert(Alert.INFO, "标题", "描述文字", true);
        assert a.getComponentCount() == 1 && a.getComponent(0) instanceof CloseButton
                : "closable alert has CloseButton child, count=" + a.getComponentCount();
        Alert b = new Alert(Alert.INFO, "标题", null, false);
        assert b.getComponentCount() == 0 : "non-closable alert has no child";
        // close() 动画完成后回调触发（Animator 走 EDT）
        final Throwable[] err = {null};
        final boolean[] closed = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                a.setSize(360, 56);
                a.close(() -> closed[0] = true);
            });
            Thread.sleep(400);
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        assert closed[0] : "onClosed callback should fire after close animation";
        System.out.println("Alert self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
```

- [ ] **Step 2: 编译 + 自检**

Run: `cd "D:\Program Files\code\swing-element-ui" && cmd //c build.bat 2>&1 | grep -E "BUILD|FAILED" && java -ea -cp out org.swelement.ui.Alert`
Expected: `BUILD OK`、`Alert self-check OK`

- [ ] **Step 3: Demo 冒烟（AlertDemo 无需改动，验证行为不回归）**

Run: `java -cp out org.swelement.demo.AlertDemo`（人工确认：静态 Alert 右上角 × hover 有圆形底色，点击触发收缩动画）
AlertDemo 代码本身不改——原坐标判断在 Alert 内部，已随组件重写删除。

- [ ] **Step 4: Commit**

```bash
git add src/org/swelement/ui/Alert.java
git commit -m "refactor: Alert close via CloseButton child with alpha-linked interactivity, removes coordinate hit-testing"
```

---

### Task 4: Input 清空重写

**Files:**
- Modify: `src/org/swelement/ui/Input.java`

**Interfaces:**
- Consumes: Task 1 的 CloseButton（`new CloseButton(20)`）
- Produces: `Input(String)` 不变；`getText()`/`setText()`/`setEnabled()` 不变

- [ ] **Step 1: 修改 Input.java**

变更点（其余保持不变）：

1. 新增字段：

```java
    private final CloseButton clearBtn = new CloseButton(20);
```

2. 构造函数中：
   - `field.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 30));` 改为 `BorderFactory.createEmptyBorder(8, 12, 8, 8);`（右侧空间改由 CloseButton 占位）
   - `add(field, BorderLayout.CENTER);` 后插入：

```java
        clearBtn.addActionListener(e -> { setText(""); field.requestFocus(); });
        clearBtn.setAlpha(0f);
        clearBtn.setInteractive(false);
        JPanel east = new JPanel(new GridBagLayout()); // 居中放置，避免 BorderLayout.EAST 拉伸高度
        east.setOpaque(false);
        east.add(clearBtn);
        add(east, BorderLayout.EAST);
```

   - MouseAdapter `m` 删除 `mouseClicked` 方法，仅保留 mouseEntered/mouseExited

3. `clearAnim` 的 listener 改为：

```java
    private final Animator clearAnim = new Animator(150, Easing::easeInOut, v -> { clearVis = v; syncClear(); repaint(); });
```

并新增：

```java
    /** 清空按钮淡入淡出动画驱动 alpha 与可交互性（无文本或 alpha 低时不拦截点击）。 */
    private void syncClear() {
        clearBtn.setAlpha(clearVis);
        clearBtn.setInteractive(clearVis > 0.5f);
    }
```

4. `paintComponent` 删除 `if (clearVis > 0) { ... }` 的 × 绘制块（约 92-99 行）

5. `setEnabled` 中追加 `clearBtn.setInteractive(enabled && clearVis > 0.5f);`

6. 类末尾新增 selfCheck：

```java
    static void selfCheck() {
        Input in = new Input("占位符");
        assert in.getText().isEmpty() : "initial text empty";
        in.setText("hello");
        assert "hello".equals(in.getText()) : "setText works";
        // hover 触发清空按钮淡入 → 可交互 → 点击清空（Animator 走 EDT）
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                in.setSize(260, 40);
                in.doLayout();
                in.dispatchEvent(new java.awt.event.MouseEvent(in, java.awt.event.MouseEvent.MOUSE_ENTERED,
                        System.currentTimeMillis(), 0, 10, 10, 0, false));
            });
            Thread.sleep(300);
            SwingUtilities.invokeAndWait(() -> clearBtnClickForTest(in));
            Thread.sleep(50);
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        assert in.getText().isEmpty() : "clear button click should clear text, got: " + in.getText();
        System.out.println("Input self-check OK");
    }

    /** 测试辅助：向 Input 内的 CloseButton 派发点击事件（同包访问私有字段）。 */
    private static void clearBtnClickForTest(Input in) {
        for (Component c : in.getComponents()) {
            if (c instanceof JPanel) {
                for (Component cc : ((JPanel) c).getComponents()) {
                    if (cc instanceof CloseButton) {
                        cc.dispatchEvent(new java.awt.event.MouseEvent(cc, java.awt.event.MouseEvent.MOUSE_CLICKED,
                                System.currentTimeMillis(), 0, 10, 10, 1, false));
                        return;
                    }
                }
            }
        }
        throw new AssertionError("CloseButton not found in Input");
    }

    public static void main(String[] args) { selfCheck(); }
```

- [ ] **Step 2: 编译 + 自检**

Run: `cd "D:\Program Files\code\swing-element-ui" && cmd //c build.bat 2>&1 | grep -E "BUILD|FAILED" && java -ea -cp out org.swelement.ui.Input`
Expected: `BUILD OK`、`Input self-check OK`

- [ ] **Step 3: Demo 冒烟（InputDemo 无需改动）**

Run: `java -cp out org.swelement.demo.InputDemo`（人工确认：输入文字 + 悬停/聚焦后 × 淡入，hover × 有圆形底色，点击清空且光标回到输入框）

- [ ] **Step 4: Commit**

```bash
git add src/org/swelement/ui/Input.java
git commit -m "refactor: Input clear button via CloseButton child with alpha-linked interactivity, removes coordinate hit-testing"
```

---

### Task 5: AstDialog 标题栏新增 × 关闭

**Files:**
- Modify: `src/org/swelement/ui/AstDialog.java`

**Interfaces:**
- Consumes: Task 1 的 CloseButton（`new CloseButton(24)`）
- Produces: `DialogCardPanel` 标题栏右上角 ×，行为等同 `finish(RESULT_CANCEL)`；`makeCard` 签名不变

- [ ] **Step 1: 修改 DialogCardPanel.buildLayout()**

在 `final JPanel titleBar = new JPanel() {...}` 定义之前插入：

```java
            final CloseButton closeX = new CloseButton(24);
            closeX.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { finish(RESULT_CANCEL); }});
```

titleBar 匿名类内新增 doLayout 覆写（与其他覆写并列）：

```java
                @Override public void doLayout() {
                    super.doLayout();
                    closeX.setBounds(getWidth() - 24 - 16, (getHeight() - 24) / 2, 24, 24);
                }
```

titleBar 构造区（`titleBar.setOpaque(false);` 后）加：

```java
            titleBar.setLayout(null); // closeX 绝对定位
            titleBar.add(closeX);
```

- [ ] **Step 2: 扩展 AstDialog.selfCheck 断言 × 存在且点击生效**

在 `selfCheck()` 中第一次 `AstDialog.show(jf, "对话框标题", ...)` 块内、`assert cancel != null : "取消 button present";` 之后插入：

```java
            Component closeX = null;
            Queue<Container> bfs = new LinkedList<Container>(); bfs.add((Container) card);
            while (!bfs.isEmpty() && closeX == null) {
                Container cur = bfs.poll();
                for (int i = 0; i < cur.getComponentCount(); i++) {
                    Component ch = cur.getComponent(i);
                    if (ch instanceof CloseButton) { closeX = ch; break; }
                    if (ch instanceof Container) bfs.add((Container) ch);
                }
            }
            assert closeX != null : "close × present in dialog title bar";
```

第二次 show（`"T2"`）块内，将 `clickComponent(ok2);` 前插入对 × 的验证（点击 × → RESULT_CANCEL）：

```java
            Component closeX2 = null;
            Queue<Container> bfs2 = new LinkedList<Container>(); bfs2.add((Container) card);
            while (!bfs2.isEmpty() && closeX2 == null) {
                Container cur = bfs2.poll();
                for (int i = 0; i < cur.getComponentCount(); i++) {
                    Component ch = cur.getComponent(i);
                    if (ch instanceof CloseButton) { closeX2 = ch; break; }
                    if (ch instanceof Container) bfs2.add((Container) ch);
                }
            }
            assert closeX2 != null : "close × present (second dialog)";
            clickComponent(closeX2);
            try { Thread.sleep(300); } catch (InterruptedException ignore) {}
            assert res[0] == RESULT_CANCEL : "× click should cancel, actual=" + res[0];
            // 第三次：重新打开验证 ok 按钮仍正常
            AstDialog.show(jf, "T3", body, new ResultCallback() { public void onResult(int resultCode) { res[0] = resultCode; }});
            try { Thread.sleep(260); } catch (InterruptedException ignore) {}
            card = firstPanelChild((Container) jf.getGlassPane());
            Component ok3 = findChildByText((Container) card, "确定");
            clickComponent(ok3);
            try { Thread.sleep(300); } catch (InterruptedException ignore) {}
            assert res[0] == RESULT_OK : "OK still works after × cancel, actual=" + res[0];
```

（原 `clickComponent(ok2)` 段的 OK 断言由上述 T3 段替代，删除原 ok2 点击与断言，保留 findChildByText(ok2) 存在性断言。）

注意：`makeCard` 的 offscreen paint 测试（`card.setSize(480, 240)`）中卡片不含 CloseButton 布局问题——DialogCardPanel 构造即添加 closeX 到 titleBar，`doLayout` 在 setSize 后由 `card.paint(gg)` 前的 `validate()` 链触发；若 paint 时 doLayout 未执行，closeX bounds 为 0，paint 仍安全（alpha=1，绘制在 0,0 不可见区域不影响断言像素点）。无需改动该段。

- [ ] **Step 3: 编译 + AstDialog/AstMessageBox 自检**

Run: `cd "D:\Program Files\code\swing-element-ui" && cmd //c build.bat 2>&1 | grep -E "BUILD|FAILED|self-check"`
Expected: `BUILD OK`、`AstDialog self-check OK`、`AstMessageBox self-check OK`（AstMessageBox 不调用 makeCard，已确认无影响；作为回归仍跑）

- [ ] **Step 4: Demo 冒烟**

Run: `java -cp out org.swelement.demo.AstAdvancedDemo`（人工确认：打开对话框，标题栏右上角 × hover 有圆形底色，点击关闭对话框且回调走取消分支）

- [ ] **Step 5: Commit**

```bash
git add src/org/swelement/ui/AstDialog.java
git commit -m "feat: AstDialog title bar close × via CloseButton, behaves as RESULT_CANCEL"
```

---

### Task 6: build.bat 集成自检 + 全量验证

**Files:**
- Modify: `build.bat`（追加 4 个自检运行）

**Interfaces:**
- Consumes: Task 1-5 的 selfCheck
- Produces: build.bat 全量校验覆盖新组件

- [ ] **Step 1: build.bat 追加自检**

在 `echo --- AstContainer self-check ---` 段之前插入：

```bat
echo --- CloseButton self-check ---
java -ea -cp out org.swelement.ui.CloseButton
if %ERRORLEVEL% NEQ 0 ( echo CloseButton self-check FAILED & exit /b 1 )

echo --- Tag self-check ---
java -ea -cp out org.swelement.ui.Tag
if %ERRORLEVEL% NEQ 0 ( echo Tag self-check FAILED & exit /b 1 )

echo --- Alert self-check ---
java -ea -cp out org.swelement.ui.Alert
if %ERRORLEVEL% NEQ 0 ( echo Alert self-check FAILED & exit /b 1 )

echo --- Input self-check ---
java -ea -cp out org.swelement.ui.Input
if %ERRORLEVEL% NEQ 0 ( echo Input self-check FAILED & exit /b 1 )
```

- [ ] **Step 2: 全量构建 + 全部自检**

Run: `cd "D:\Program Files\code\swing-element-ui" && cmd //c build.bat 2>&1 | tail -40`
Expected: `BUILD OK` + 所有 `self-check OK`（含原有 Ast* 系列，无 FAILED）

- [ ] **Step 3: 确认坐标命中测试已全部清除**

Run（应无输出）: `grep -rn "e\.getX() > " src/org/swelement/ui/Tag.java src/org/swelement/ui/Alert.java src/org/swelement/ui/Input.java`
Expected: 无匹配（三处坐标判断全部删除；AstDrawer 原本即为组件方式，不在清除范围）

- [ ] **Step 4: Commit**

```bash
git add build.bat
git commit -m "chore: add CloseButton/Tag/Alert/Input self-checks to build.bat"
```

---

## Self-Review 记录

- Spec 覆盖：CloseButton（Task 1）、Tag effect/尺寸/关闭（Task 2）、Alert（Task 3）、Input（Task 4）、AstDialog ×（Task 5）、selfCheck 与 build.bat 集成（Task 1-6）、Demo 更新（Task 2-5 内）——全覆盖
- 两处 spec 偏差已在头部 reconcile 注明（CloseButton 默认色取对比度达标深色变体；AstMessageBox 实际不调用 makeCard）
- TagDemo 中删除的坐标命中监听逻辑由 `setOnClosed` API 替代，类型签名一致（Task 2 Produces 与 Demo 使用一致）
- Input 的 clearVis 语义保留（hover/focus 且有文本时显示），仅交互载体从坐标判断改为 CloseButton
