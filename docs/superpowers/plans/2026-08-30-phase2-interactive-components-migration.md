# Phase 2: 基础交互组件迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 4 个基础交互组件（AstSwitch、AstRadio、AstCheckbox、AstButton）完全迁移到 AstInteractiveComponent 框架基类，支持运行时主题切换和完整自检

**Architecture:** 完全迁移方案：基类新增选中状态能力，4 个组件依次从 Swing 按钮基类迁移到 AstInteractiveComponent，替换 ElementTheme/Animator/手动 Graphics2D，补充完整自检

**Tech Stack:** Java Swing, JDK 8, 零外部依赖

---

## 前置任务：AstInteractiveComponent 添加选中状态能力

**Files:**
- Modify: `src/org/swelement/framework/AstInteractiveComponent.java`

### 新增 import

在文件顶部 import 区域添加：
```java
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.event.EventListenerList;
```

### 新增字段

在 `focused` 字段后添加：
```java
    /** 是否处于选中状态 */
    private boolean selected = false;
    /** sticky 行为：鼠标按下标记（按下后移出再释放仍切换） */
    private boolean pressStarted = false;
    /** 选中状态监听器列表 */
    private final EventListenerList itemListenerList = new EventListenerList();
```

### 注册 selected 动画

在 `initComponent()` 的动画注册部分（`anim.register(AnimationManager.FOCUS, ...)` 之后）添加：
```java
        anim.register(AnimationManager.SELECTED, 200, Easing::easeInOut);
```

注意：需要确认 AnimationManager 中是否有 SELECTED 常量。如果没有，使用字符串常量 `"selected"`。

### 修改鼠标监听：mousePressed

在 `mousePressed` 方法中，添加 `pressStarted = true;`：
```java
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                if (SwingUtilities.isLeftMouseButton(e)) {
                    pressing = true;
                    pressStarted = true;
                    anim.start(AnimationManager.ACTIVE);
                    onActiveChanged(true);
                    requestFocusInWindow();
                }
            }
```

### 修改鼠标监听：mouseReleased

在 `mouseReleased` 方法中，添加选中状态切换（sticky 行为）：
```java
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isEnabled()) return;
                if (pressing) {
                    pressing = false;
                    anim.stop(AnimationManager.ACTIVE);
                    onActiveChanged(false);
                }
                // sticky 行为：只要 pressStarted 为 true，释放时切换选中状态
                if (pressStarted) {
                    pressStarted = false;
                    setSelected(!selected);
                }
            }
```

### 修改鼠标监听：mouseExited

`mouseExited` 中不重置 `pressStarted`（sticky 行为的关键）。保持现有逻辑不变。

### 修改 setEnabled

在 `setEnabled` 方法中添加 selected 动画的重置：
```java
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            hovering = false;
            pressing = false;
            focused = false;
            pressStarted = false;
            anim.stop(AnimationManager.HOVER);
            anim.stop(AnimationManager.ACTIVE);
            anim.stop(AnimationManager.FOCUS);
            anim.stop(AnimationManager.SELECTED);
        }
    }
```

### 新增便捷进度方法

在 `focusProgress()` 后添加：
```java
    /**
     * 获取选中动画当前进度。
     *
     * @return 进度值 [0, 1]
     */
    protected float selectedProgress() {
        return anim.getProgress(AnimationManager.SELECTED);
    }
```

### 新增状态查询方法

在 `isFocusedFlag()` 后添加：
```java
    /**
     * 查询是否处于选中状态。
     *
     * @return true 表示选中
     */
    public boolean isSelected() {
        return selected;
    }
```

### 新增 setSelected 方法

在 `isSelected()` 后添加：
```java
    /**
     * 设置选中状态。
     * <p>
     * 状态变化时触发选中动画和 ItemListener 通知。
     *
     * @param selected true 选中，false 取消选中
     */
    public void setSelected(boolean selected) {
        if (this.selected == selected) return;
        boolean old = this.selected;
        this.selected = selected;
        anim.go(AnimationManager.SELECTED, old ? 1f : 0f, selected ? 1f : 0f);
        fireItemStateChanged(selected);
        onSelectedChanged(selected);
    }
```

### 新增 ItemListener 支持

在 `onSelectedChanged` 钩子方法之后添加：
```java
    /**
     * 添加选中状态变更监听器。
     *
     * @param l 监听器
     */
    public void addItemListener(ItemListener l) {
        itemListenerList.add(ItemListener.class, l);
    }

    /**
     * 移除选中状态变更监听器。
     *
     * @param l 监听器
     */
    public void removeItemListener(ItemListener l) {
        itemListenerList.remove(ItemListener.class, l);
    }

    /**
     * 触发选中状态变更事件。
     *
     * @param selected 当前选中状态
     */
    protected void fireItemStateChanged(boolean selected) {
        ItemEvent e = new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                selected ? ItemEvent.SELECTED : ItemEvent.DESELECTED);
        for (ItemListener l : itemListenerList.getListeners(ItemListener.class)) {
            l.itemStateChanged(e);
        }
    }

    /**
     * 选中状态变更钩子，子类可重写。
     *
     * @param selected true 表示选中，false 表示取消选中
     */
    protected void onSelectedChanged(boolean selected) {}
```

注意：`onSelectedChanged` 钩子应该放在状态变更钩子区域（和 onHoverChanged/onActiveChanged/onFocusChanged 在一起）。

### 验证步骤

- [ ] 编译：`javac -encoding UTF-8 --release 8 -d out src/org/swelement/framework/AstInteractiveComponent.java src/org/swelement/framework/AstAbstractComponent.java src/org/swelement/core/AnimationManager.java src/org/swelement/core/Easing.java src/org/swelement/core/theme/Theme.java src/org/swelement/core/theme/ThemeManager.java src/org/swelement/core/theme/ElementLightTheme.java src/org/swelement/core/Animator.java src/org/swelement/core/SelfCheckBase.java`
- [ ] 检查 AnimationManager 中是否有 SELECTED 常量，如果没有则使用字符串 `"selected"`

---

## Task 1: 迁移 AstSwitch

**Files:**
- Modify: `src/org/swelement/ui/AstSwitch.java`（46 行）

### 1. 修改 import

删除：`import org.swelement.core.Animator;`、`import org.swelement.core.ElementTheme;`、`import org.swelement.core.StickyToggleModel;`
添加：`import org.swelement.framework.AstInteractiveComponent;`
保留：`import org.swelement.core.Easing;`、`import javax.swing.*;`、`import java.awt.*;`、`import java.awt.geom.RoundRectangle2D;`

### 2. 修改类声明

从：`public class AstSwitch extends JToggleButton {`
改为：`public class AstSwitch extends AstInteractiveComponent {`

### 3. 删除字段

删除：
```java
private final Animator slideAnim = new Animator(300, Easing::easeInOut, v -> { slide = v; repaint(); });
private float slide = 0f;
```

### 4. 修改 initComponent

添加 initComponent 方法，注册 slide 动画：
```java
@Override
protected void initComponent() {
    super.initComponent();
    anim.register("slide", 300, Easing::easeInOut);
}
```

### 5. 修改构造函数

当前构造函数（约 16-22 行）：
```java
public AstSwitch() {
    setModel(new StickyToggleModel());
    setOpaque(false);
    setFocusPainted(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    addItemListener(e -> slideAnim.go(slide, isSelected() ? 1f : 0f));
}
```

改为（删除 StickyToggleModel、setOpaque、setFocusPainted、setCursor、addItemListener，因为基类已处理）：
```java
public AstSwitch() {
}
```

添加 onSelectedChanged 钩子来驱动 slide 动画：
```java
@Override
protected void onSelectedChanged(boolean selected) {
    super.onSelectedChanged(selected);
    anim.go("slide", anim.getProgress("slide"), selected ? 1f : 0f);
}
```

### 6. 修改 paintComponent

当前：
```java
@Override
protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int w = getWidth(), h = getHeight();
    g2.setColor(ElementTheme.lerp(new Color(0xDCDFE6), ElementTheme.PRIMARY, slide));
    g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, h - 1, h - 1));
    int knob = h - 6;
    int x = Math.round(2 + slide * (w - knob - 4));
    g2.setColor(Color.WHITE);
    g2.fillOval(x, 3, knob, knob);
    g2.dispose();
}
```

改为：
```java
@Override
protected void paintComponent(Graphics g) {
    Graphics2D g2 = createGraphics(g);
    float slide = anim.getProgress("slide");
    int w = getWidth() - 1, h = getHeight() - 1;
    g2.setColor(lerp(new Color(0xDCDFE6), theme().getPrimary(), slide));
    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
    int knob = h - 5;
    int x = Math.round(2 + slide * (w - knob - 3));
    g2.setColor(Color.WHITE);
    g2.fillOval(x, 3, knob, knob);
    g2.dispose();
}
```

### 7. 修改 getPreferredSize

从：`return new Dimension(44, 22);`（保持不变）

### 8. 添加 selfCheck

```java
@Override
protected void selfCheck() {
    AstSwitch s = new AstSwitch();
    assert s.getPreferredSize().width == 44 : "default width";
    assert s.getPreferredSize().height == 22 : "default height";
    assert !s.isSelected() : "default not selected";
    s.setSelected(true);
    assert s.isSelected() : "setSelected true";
    s.setSelected(false);
    assert !s.isSelected() : "setSelected false";
    // 对比度：滑块白字 vs 轨道选中色（PRIMARY）
    assertContrast(Color.WHITE, theme().getPrimary(), "switch knob on track");
    System.out.println("AstSwitch self-check OK");
}

public static void main(String[] args) {
    new AstSwitch().selfCheck();
}
```

### 验证

- [ ] 编译：`javac -encoding UTF-8 --release 8 -d out src/org/swelement/ui/AstSwitch.java src/org/swelement/framework/AstInteractiveComponent.java src/org/swelement/framework/AstAbstractComponent.java src/org/swelement/core/AnimationManager.java src/org/swelement/core/Easing.java src/org/swelement/core/theme/Theme.java src/org/swelement/core/theme/ThemeManager.java src/org/swelement/core/theme/ElementLightTheme.java src/org/swelement/core/Animator.java src/org/swelement/core/SelfCheckBase.java`
- [ ] 自检：`java -ea -cp out org.swelement.ui.AstSwitch`
- [ ] Expected: "AstSwitch self-check OK"

---

## Task 2: 迁移 AstRadio

**Files:**
- Modify: `src/org/swelement/ui/AstRadio.java`（68 行）

### 1. 修改 import

删除：`import org.swelement.core.Animator;`、`import org.swelement.core.ElementTheme;`、`import org.swelement.core.StickyToggleModel;`
添加：`import org.swelement.framework.AstInteractiveComponent;`
保留：`import org.swelement.core.Easing;`、`import javax.swing.*;`、`import java.awt.*;`、`import java.awt.event.MouseAdapter;`、`import java.awt.event.MouseEvent;`

### 2. 修改类声明

从：`public class AstRadio extends JRadioButton {`
改为：`public class AstRadio extends AstInteractiveComponent {`

### 3. 删除字段

删除：
```java
private final Animator dotAnim = new Animator(200, Easing::easeOut, v -> { dot = v; repaint(); });
private final Animator borderAnim = new Animator(200, Easing::easeInOut, v -> { border = v; repaint(); });
private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
private float dot = 0;
private float border = 0;
private float hover = 0;
```

注意：hover 动画由基类处理，用 `hoverProgress()` 获取。dot 和 border 是自定义动画，需要注册到 anim。

### 4. 添加 initComponent

```java
@Override
protected void initComponent() {
    super.initComponent();
    anim.register("dot", 200, Easing::easeOut);
    anim.register("border", 200, Easing::easeInOut);
}
```

### 5. 修改构造函数

当前（约 19-35 行）：
```java
public AstRadio(String text) {
    super(text);
    setModel(new StickyToggleModel());
    setOpaque(false);
    setFocusPainted(false);
    setFont(ElementTheme.FONT);
    setForeground(ElementTheme.TEXT_REGULAR);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) { hoverAnim.go(hover, 1f); }
        public void mouseExited(MouseEvent e) { hoverAnim.go(hover, 0f); }
    });
    addItemListener(e -> {
        borderAnim.go(border, isSelected() ? 1f : 0f);
        dotAnim.go(dot, isSelected() ? 1f : 0f);
    });
}
```

改为：
```java
private final String text;

public AstRadio(String text) {
    this.text = text;
}
```

添加 onSelectedChanged 钩子：
```java
@Override
protected void onSelectedChanged(boolean selected) {
    super.onSelectedChanged(selected);
    anim.go("border", anim.getProgress("border"), selected ? 1f : 0f);
    anim.go("dot", anim.getProgress("dot"), selected ? 1f : 0f);
}
```

### 6. 修改 paintComponent

当前（约 38-61 行）：
```java
@Override
protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int cy = getHeight() / 2;
    int r = 8;
    int cx = r + 2;
    Color borderColor = ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, Math.max(border, hover));
    g2.setColor(ElementTheme.FILL_BLANK);
    g2.fillOval(cx - r, cy - r, r * 2, r * 2);
    g2.setColor(borderColor);
    g2.setStroke(new BasicStroke(1f));
    g2.drawOval(cx - r, cy - r, r * 2, r * 2);
    if (dot > 0.01f) {
        int innerR = (int) (4 * Math.sqrt(dot));
        g2.setColor(ElementTheme.PRIMARY);
        g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
    }
    g2.setColor(ElementTheme.TEXT_REGULAR);
    g2.setFont(getFont());
    FontMetrics fm = g2.getFontMetrics();
    g2.drawString(getText(), cx + r + 8, cy + fm.getAscent() / 2 - fm.getDescent());
    g2.dispose();
}
```

改为：
```java
@Override
protected void paintComponent(Graphics g) {
    Graphics2D g2 = createGraphics(g);
    float dot = anim.getProgress("dot");
    float border = anim.getProgress("border");
    float hover = hoverProgress();
    int cy = getHeight() / 2;
    int r = 8;
    int cx = r + 2;
    Color borderColor = lerp(theme().getBorderBase(), theme().getPrimary(), Math.max(border, hover));
    g2.setColor(theme().getFillBlank());
    g2.fillOval(cx - r, cy - r, r * 2, r * 2);
    g2.setColor(borderColor);
    g2.setStroke(new BasicStroke(1f));
    g2.drawOval(cx - r, cy - r, r * 2, r * 2);
    if (dot > 0.01f) {
        int innerR = (int) (4 * Math.sqrt(dot));
        g2.setColor(theme().getPrimary());
        g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
    }
    g2.setColor(theme().getTextRegular());
    g2.setFont(theme().getFontBase());
    FontMetrics fm = g2.getFontMetrics();
    g2.drawString(text, cx + r + 8, cy + fm.getAscent() / 2 - fm.getDescent());
    g2.dispose();
}
```

### 7. 修改 getPreferredSize

当前可能使用 super.getPreferredSize() + 偏移。改为手动计算：
```java
@Override
public Dimension getPreferredSize() {
    FontMetrics fm = getFontMetrics(theme().getFontBase());
    int w = 20 + 8 + fm.stringWidth(text) + 8;
    int h = 28;
    return new Dimension(w, h);
}
```

### 8. 添加 selfCheck

```java
@Override
protected void selfCheck() {
    AstRadio r = new AstRadio("选项");
    assert r.getPreferredSize().height == 28 : "default height";
    assert !r.isSelected() : "default not selected";
    r.setSelected(true);
    assert r.isSelected() : "setSelected true";
    r.setSelected(false);
    assert !r.isSelected() : "setSelected false";
    // 对比度
    assertContrast(theme().getTextRegular(), theme().getFillBlank(), "radio text on bg");
    System.out.println("AstRadio self-check OK");
}

public static void main(String[] args) {
    new AstRadio("test").selfCheck();
}
```

### 验证

- [ ] 编译（依赖同上 + AstRadio.java）
- [ ] 自检：`java -ea -cp out org.swelement.ui.AstRadio`
- [ ] Expected: "AstRadio self-check OK"

---

## Task 3: 迁移 AstCheckbox

**Files:**
- Modify: `src/org/swelement/ui/AstCheckbox.java`（82 行）

### 迁移模式

与 AstRadio 类似，但有以下差异：
- 形状是圆角矩形（非圆形）
- 有勾号 clip 揭示动画（fill + check 两个动画）
- 勾号用 Path2D 绘制 + clip 裁剪
- hover 用基类 hover 动画

### 关键改动点

1. **类声明**：`JCheckBox` → `AstInteractiveComponent`
2. **删除字段**：`fillAnim`、`checkAnim`、`hoverAnim`、`fill`、`check`、`hover`
3. **initComponent**：注册 `fill`（200ms easeInOut）和 `check`（200ms easeOut）动画
4. **构造函数**：删除 StickyToggleModel、setOpaque、setFocusPainted、setCursor、addMouseListener、addItemListener
5. **onSelectedChanged**：驱动 fill 和 check 动画
6. **paintComponent**：使用 `createGraphics(g)`、`theme().getXxx()`、`lerp()`、`hoverProgress()`
7. **getPreferredSize**：手动计算（图标 16 + 文字 + gap）
8. **selfCheck**：补充完整自检

### 验证

- [ ] 编译通过
- [ ] 自检通过：`java -ea -cp out org.swelement.ui.AstCheckbox`

---

## Task 4: 迁移 AstButton

**Files:**
- Modify: `src/org/swelement/ui/AstButton.java`（336 行）

这是最复杂的迁移。核心改动：

### 1. 类声明 + import

`extends JButton` → `extends AstInteractiveComponent`
删除 `import org.swelement.core.Animator;`、`import org.swelement.core.ElementTheme;`
添加 `import org.swelement.framework.AstInteractiveComponent;`、`import org.swelement.core.theme.Theme;`

### 2. 静态颜色数组 → 实例方法

所有静态颜色数组（`BASE_BG`、`HOVER_BG`、`ACTIVE_BG`、`BASE_FG`、`HOVER_FG`、`BORDER`、`TYPE_FG`、`PLAIN_BG`、`PLAIN_FG`、`PLAIN_HOVER_BG`、`PLAIN_ACTIVE_BG`、`PLAIN_ACTIVE_FG`）改为实例方法，从 `theme()` 获取颜色后计算。

例如：
```java
private Color baseBg(int type) {
    Theme t = theme();
    switch (type) {
        case PRIMARY: return t.getPrimary();
        case SUCCESS: return t.getSuccess();
        // ...
        default: return t.getFillBlank(); // DEFAULT type
    }
}
```

### 3. 动画迁移

- hover/active → 基类标准动画（`hoverProgress()` / `activeProgress()`）
- loadAnim → `anim.register("load", 800, Easing::linear)` + 递归循环

### 4. 构造函数

删除 `setContentAreaFilled(false)`、`setFocusPainted(false)`、`setBorderPainted(false)`、`setBorder(null)` 等 JButton 特有调用。
删除手动 `addMouseListener`（基类已处理）。
保留：type/plain/text 等字段初始化、`setFont(...)`、`setCursor`（基类已设手型，可能不需要）。

### 5. paintComponent

- 使用 `createGraphics(g)`
- 颜色插值用 `lerp()`
- ElementTheme 颜色替换为 `theme().getXxx()`
- 所有静态颜色数组引用改为实例方法调用
- loading 动画用 `anim.getProgress("load")`

### 6. 文字与图标

AstButton 原有 `setText/getText`（继承自 JButton），迁移后需要自己实现：
```java
private String text;
public String getText() { return text; }
public void setText(String text) { this.text = text; repaint(); }
```

同理，`setIcon`/`setIconPosition` 等保留。

### 7. addActionListener

原 JButton 自带 ActionListener 支持。迁移后需要添加：
```java
private final EventListenerList actionListenerList = new EventListenerList();
public void addActionListener(ActionListener l) { actionListenerList.add(ActionListener.class, l); }
public void removeActionListener(ActionListener l) { actionListenerList.remove(ActionListener.class, l); }
protected void fireActionPerformed() {
    ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, text);
    for (ActionListener l : actionListenerList.getListeners(ActionListener.class)) {
        l.actionPerformed(e);
    }
}
```

在 mouseReleased 时（且 pressStarted 为 true、在组件内释放）触发 fireActionPerformed。

注意：基类的 mouseReleased 已经处理了 sticky 选中切换。按钮不需要选中状态切换，而是需要 ActionEvent。所以 AstButton 应该重写 mouseReleased 行为，或者使用 onActiveChanged 钩子。

更好的方式：AstButton 不使用基类的 selected 状态（按钮是临时激活，不是选中态）。而是重写 mouseReleased 来触发 ActionEvent。

实际上，AstButton 的交互模式是：按下 → active 动画 → 释放 → 触发 ActionEvent。没有 selected 状态。所以：

- AstButton 不使用基类的 selected 状态和 sticky 行为
- 重写 mouseReleased 以触发 ActionEvent
- 或者在 onActiveChanged(false) 中判断是否触发

最简单的方式：保留基类的鼠标监听，但在 mouseReleased 时额外触发 action。

### 8. selfCheck 迁移

将静态 selfCheck 改为实例方法，替换 `ElementTheme.assertContrast(...)` 为 `assertContrast(...)`。

### 验证

- [ ] 编译通过
- [ ] 自检通过：`java -ea -cp out org.swelement.ui.AstButton`

---

## 最终验证

- [ ] 编译所有迁移组件
- [ ] 运行 run-checks.bat
- [ ] 单独运行每个组件的 selfCheck
- [ ] 确认 ALL CHECKS PASSED
