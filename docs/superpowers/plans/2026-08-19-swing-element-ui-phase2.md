# swing-element-ui Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Phase 1 基础上实现 8 个交互+状态组件：Select / Tabs / Pagination / Menu / Tag / Progress / Badge / Alert，全部 UI 变化带动画。

**Architecture:** 复用 Phase 1 的 `Animator`/`Easing`/`ElementTheme`。弹层（Select 下拉、Menu 子菜单）用 JPopupMenu 自绘的 `AnimatedPopup`（淡入+下滑动画）。组件全部自绘 JComponent，动画只在事件处理器中启动（**严禁在 paintComponent 里启动动画**——Phase 1 Slider 教训：会在 paint 循环中不断重置动画起点，导致永久重绘）。

**Tech Stack:** Java 8 (`--release 8` 或回退 `-source 8 -target 8`)、javax.swing、纯 javac（build.bat）、零外部依赖。

## Global Constraints

- 编译：`.\build.bat`（UTF-8，`--release 8`，输出 `out/`）。单个文件调试：`javac -encoding UTF-8 --release 8 -cp out -d out src\org\swelement\ui\X.java`
- 零外部依赖，只用 JDK 8 标准库
- 包：`org.swelement.core` / `org.swelement.ui` / `org.swelement.demo`
- 自检 main 必须 `public static void main`（java 启动器要求 public）
- 动画只从事件/方法调用中 `Animator.go()` 启动；paintComponent 只读状态
- 每个组件配 `*Demo`（main 起 JFrame）
- 设计规格：`docs/superpowers/specs/2026-08-19-swing-element-ui-phase2-design.md`
- 项目根：`D:\Program Files\code\swing-element-ui`（git main 分支，仓库根目录还有其他无关项目文件夹，禁止触碰）

---

### Task 1: AnimatedPopup（弹层容器）

**Files:**
- Create: `src/org/swelement/core/AnimatedPopup.java`
- Test: 通过 Task 2 (Select) 的 Demo 目视验收

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `AnimatedPopup extends JPopupMenu`，构造 `AnimatedPopup()`；方法 `JPanel getContent()`（往里面放内容）；重写 `public void show(Component, int, int)` 播放打开动画。

- [ ] **Step 1: 写 AnimatedPopup**

创建 `src/org/swelement/core/AnimatedPopup.java`：

```java
package org.swelement.core;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class AnimatedPopup extends JPopupMenu {
    private final Animator openAnim = new Animator(200, Easing::easeOut, v -> {
        alpha = v;
        content.setBorder(new EmptyBorder(Math.round(8 * (1 - v)), 0, 0, 0));
        repaint();
    });
    private float alpha;
    private final JPanel content;

    public AnimatedPopup() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int a = Math.round(255 * alpha);
                g2.setColor(new Color(255, 255, 255, a));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.setColor(new Color(228, 231, 237, a));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setLayout(new BorderLayout());
        add(content);
    }

    public JPanel getContent() { return content; }

    @Override
    public void show(Component invoker, int x, int y) {
        alpha = 0f;
        super.show(invoker, x, y);
        openAnim.go(0f, 1f);
    }
}
```

- [ ] **Step 2: 编译**

Run: `javac -encoding UTF-8 --release 8 -cp out -d out src\org\swelement\core\AnimatedPopup.java`
Expected: 无错误（含 Phase 1 既有类路径 out/）。

- [ ] **Step 3: 提交**

```bash
git add src/org/swelement/core/AnimatedPopup.java
git commit -m "feat: AnimatedPopup popup container with fade+slide-in"
```

---

### Task 2: Select（单选+多选+搜索+分组+可清空）+ Demo

**Files:**
- Create: `src/org/swelement/ui/Select.java`
- Create: `src/org/swelement/demo/SelectDemo.java`
- Test: `Select.selfCheck()`（public main）+ `SelectDemo` 视觉验收

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`, `AnimatedPopup`
- Produces: `Select extends JPanel`；内部类 `Select.Option(String label, Object value)` 与 `Select.Option(String label, Object value, String group, boolean disabled)`；构造 `Select(boolean multiple, boolean filterable)`；方法 `void addOption(Option)`、`java.util.List<Option> getSelected()`、`void clearSelection()`、`void setEnabled(boolean)`；静态 `boolean matches(String label, String filter)`（过滤匹配，自检用）。

- [ ] **Step 1: 写 Select**

创建 `src/org/swelement/ui/Select.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.AnimatedPopup;
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
import java.util.ArrayList;
import java.util.List;

public class Select extends JPanel {
    public static class Option {
        public final String label;
        public final Object value;
        public final String group;
        public final boolean disabled;

        public Option(String label, Object value) { this(label, value, null, false); }

        public Option(String label, Object value, String group, boolean disabled) {
            this.label = label;
            this.value = value;
            this.group = group;
            this.disabled = disabled;
        }
    }

    private final List<Option> options = new ArrayList<>();
    private final List<Option> selected = new ArrayList<>();
    private final JTextField field;
    private final JLabel display = new JLabel();
    private final JPanel tagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
    private final JPanel center = new JPanel(new BorderLayout());
    private final AnimatedPopup popup = new AnimatedPopup();
    private final JPanel optionList = new JPanel();
    private final boolean multiple, filterable;
    private final Animator arrowAnim = new Animator(200, Easing::easeInOut, v -> { arrowAngle = v; repaint(); });
    private float arrowAngle;
    private boolean popupShown, fieldFocus;

    public Select(boolean multiple, boolean filterable) {
        this.multiple = multiple;
        this.filterable = filterable;
        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(220, 40));

        field = filterable ? new JTextField() : null;
        center.setOpaque(false);
        if (multiple) center.add(tagsPanel, BorderLayout.NORTH);
        if (filterable) {
            field.setOpaque(false);
            field.setBorder(new EmptyBorder(0, 12, 0, 0));
            field.setFont(ElementTheme.FONT);
            field.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { fieldFocus = true; repaint(); }
                public void focusLost(FocusEvent e) { fieldFocus = false; repaint(); }
            });
            center.add(field, BorderLayout.CENTER);
        } else {
            display.setOpaque(false);
            display.setBorder(new EmptyBorder(0, 12, 0, 0));
            display.setFont(ElementTheme.FONT);
            center.add(display, BorderLayout.CENTER);
        }
        add(center, BorderLayout.CENTER);

        optionList.setOpaque(false);
        optionList.setLayout(new BoxLayout(optionList, BoxLayout.Y_AXIS));
        popup.getContent().add(optionList, BorderLayout.CENTER);

        MouseAdapter click = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled()) return;
                if (!multiple && !selected.isEmpty() && e.getX() > getWidth() - 46 && e.getX() < getWidth() - 28) {
                    selected.clear();
                    updateDisplay();
                    repaint();
                    return;
                }
                togglePopup();
            }
        };
        addMouseListener(click);
        display.addMouseListener(click);
        tagsPanel.addMouseListener(click);
        if (field != null) field.addMouseListener(click);
    }

    public void addOption(Option o) { options.add(o); }

    public List<Option> getSelected() { return new ArrayList<>(selected); }

    public void clearSelection() { selected.clear(); updateDisplay(); }

    static boolean matches(String label, String filter) {
        return label.toLowerCase().contains(filter.toLowerCase());
    }

    private void togglePopup() {
        if (popupShown) {
            popup.setVisible(false);
            popupShown = false;
            arrowAnim.go(arrowAngle, 0f);
            repaint();
        } else {
            rebuildList(filterable ? field.getText() : null);
            popup.getContent().setPreferredSize(new Dimension(Math.max(180, getWidth()), popup.getContent().getPreferredSize().height));
            popup.show(this, 0, getHeight());
            popupShown = true;
            arrowAnim.go(arrowAngle, 1f);
            repaint();
        }
    }

    private void rebuildList(String filter) {
        optionList.removeAll();
        String lastGroup = null;
        for (Option o : options) {
            if (filter != null && !filter.isEmpty() && !matches(o.label, filter)) continue;
            if (o.group != null && !o.group.equals(lastGroup)) {
                lastGroup = o.group;
                JLabel g = new JLabel(o.group);
                g.setForeground(new Color(0x909399));
                g.setFont(ElementTheme.FONT.deriveFont(Font.PLAIN, 12f));
                g.setBorder(new EmptyBorder(6, 12, 4, 0));
                optionList.add(g);
            }
            optionList.add(new OptionRow(o));
        }
        if (optionList.getComponentCount() == 0) {
            JLabel empty = new JLabel("无匹配数据");
            empty.setForeground(new Color(0x909399));
            empty.setBorder(new EmptyBorder(10, 12, 10, 0));
            optionList.add(empty);
        }
        optionList.revalidate();
        optionList.repaint();
    }

    private void choose(Option o) {
        if (multiple) {
            if (selected.contains(o)) selected.remove(o);
            else selected.add(o);
        } else {
            selected.clear();
            selected.add(o);
            if (filterable) field.setText(o.label);
            popup.setVisible(false);
            popupShown = false;
            arrowAnim.go(arrowAngle, 0f);
        }
        updateDisplay();
        rebuildList(null);
        repaint();
    }

    private void updateDisplay() {
        tagsPanel.removeAll();
        if (multiple) {
            for (Option o : selected) {
                JLabel chip = new JLabel(o.label + "  ×");
                chip.setOpaque(true);
                chip.setBackground(new Color(0xF4F4F5));
                chip.setForeground(new Color(0x606266));
                chip.setFont(ElementTheme.FONT.deriveFont(Font.PLAIN, 12f));
                chip.setBorder(new EmptyBorder(2, 8, 2, 8));
                chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                chip.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) { selected.remove(o); updateDisplay(); rebuildList(null); }
                });
                tagsPanel.add(chip);
            }
        } else {
            display.setText(selected.isEmpty() ? "" : selected.get(0).label);
        }
        tagsPanel.revalidate();
        tagsPanel.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean highlighted = popupShown || fieldFocus;
        Color border = isEnabled() ? (highlighted ? ElementTheme.PRIMARY : new Color(0xDCDFE6)) : new Color(0xE4E7ED);
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        g2.setColor(isEnabled() ? Color.WHITE : ElementTheme.FILL_BASE);
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(highlighted ? 2f : 1f));
        g2.draw(shape);

        float ax = getWidth() - 18f, ay = getHeight() / 2f;
        Graphics2D a2 = (Graphics2D) g2.create();
        a2.rotate(Math.PI * arrowAngle, ax, ay);
        a2.setColor(new Color(0xC0C4CC));
        a2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        a2.drawLine(Math.round(ax - 4), Math.round(ay - 1), Math.round(ax), Math.round(ay + 2));
        a2.drawLine(Math.round(ax + 4), Math.round(ay - 1), Math.round(ax), Math.round(ay + 2));
        a2.dispose();

        if (!multiple && !selected.isEmpty()) {  // 可清空 ×
            g2.setColor(new Color(0xC0C4CC));
            FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT);
            g2.drawString("\u00d7", getWidth() - 38 - fm.stringWidth("\u00d7") / 2, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        }
        g2.dispose();
    }

    private class OptionRow extends JPanel {
        private final Option option;
        private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
        private float hover;

        OptionRow(Option o) {
            this.option = o;
            setOpaque(false);
            setPreferredSize(new Dimension(Math.max(180, Select.this.getWidth()), 32));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { if (option.disabled) return; hoverAnim.go(hover, 1f); }
                public void mouseExited(MouseEvent e) { hoverAnim.go(hover, 0f); }
                public void mouseClicked(MouseEvent e) { if (!option.disabled) choose(option); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!option.disabled) {
                g2.setColor(ElementTheme.lerp(Color.WHITE, new Color(0xF5F7FA), hover));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            boolean isSel = selected.contains(option);
            g2.setColor(option.disabled ? new Color(0xC0C4CC)
                    : (isSel ? ElementTheme.PRIMARY : ElementTheme.TEXT_REGULAR));
            FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT);
            String text = option.label + (multiple && isSel ? "  \u221a" : "");
            g2.drawString(text, 12, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    public static void selfCheck() {
        assert matches("Apple", "app");
        assert matches("Apple", "APPLE");
        assert matches("苹果", "苹");
        assert !matches("Apple", "pear");
        assert !matches("", "a");
        System.out.println("AstSelect self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
```

- [ ] **Step 2: 写 SelectDemo**

创建 `src/org/swelement/demo/SelectDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.AstSelect;
import org.swelement.ui.Select;
import org.swelement.ui.AstSelect.Option;

import javax.swing.*;
import java.awt.*;

public class SelectDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstSelect Demo");
            JPanel p = new JPanel(new FlowLayout(40, 40, 40));

            AstSelect single = new AstSelect(false, false);
            single.addOption(new Option("北京", 1));
            single.addOption(new Option("上海", 2));
            single.addOption(new Option("广州", 3));
            p.add(single);

            AstSelect groups = new AstSelect(false, false);
            groups.addOption(new Option("苹果", 1, "水果", false));
            groups.addOption(new Option("香蕉", 2, "水果", false));
            groups.addOption(new Option("白菜", 3, "蔬菜", false));
            groups.addOption(new Option("萝卜", 4, "蔬菜", false));
            p.add(groups);

            AstSelect multi = new AstSelect(true, false);
            multi.addOption(new Option("Red", 1));
            multi.addOption(new Option("Green", 2));
            multi.addOption(new Option("Blue", 3));
            p.add(multi);

            AstSelect search = new AstSelect(true, true);
            for (int i = 1; i <= 10; i++) search.addOption(new Option("选项 " + i, i));
            p.add(search);

            AstSelect disabled = new AstSelect(false, false);
            disabled.addOption(new Option("禁用项", 1));
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

- [ ] **Step 3: 编译 + 自检 + 启动验收**

Run:
```
.\build.bat
java -ea -cp out org.swelement.ui.AstSelect
```
Expected: `Select self-check OK`。
然后 `java -cp out org.swelement.demo.SelectDemo`，用 Start-Process 启动 3 秒、stderr 为空（窗口本身由人工目视验收）。

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/ui/AstSelect.java src/org/swelement/demo/SelectDemo.java
git commit -m "feat: Select with multiple/filterable/groups + demo"
```

---

### Task 3: Tabs + Demo

**Files:**
- Create: `src/org/swelement/ui/Tabs.java`
- Create: `src/org/swelement/demo/TabsDemo.java`
- Test: `TabsDemo` 视觉验收

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Tabs extends JComponent`；方法 `void addTab(String title, JComponent panel)`、`int getSelectedIndex()`、`void setSelectedIndex(int)`（越界忽略）。

- [ ] **Step 1: 写 Tabs**

创建 `src/org/swelement/ui/Tabs.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Tabs extends JComponent {
    private static final int HEADER_H = 40;

    private final List<String> titles = new ArrayList<>();
    private final Animator indXAnim = new Animator(250, Easing::easeInOut, v -> { indX = v; repaint(); });
    private final Animator indWAnim = new Animator(250, Easing::easeInOut, v -> { indW = v; repaint(); });
    private final Animator contentAnim = new Animator(200, Easing::easeInOut, v -> { contentAlpha = v; repaint(); });
    private float indX, indW, contentAlpha = 1f;
    private int selected = 0;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards) {
        @Override
        protected void paintComponent(Graphics g) {
            ((Graphics2D) g).setComposite(AlphaComposite.SrcOver.derive(contentAlpha));
            super.paintComponent(g);
        }
    };

    public Tabs() {
        setOpaque(false);
        setLayout(new BorderLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new EmptyBorder(HEADER_H, 0, 0, 0));
        add(cardPanel, BorderLayout.CENTER);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getY() > HEADER_H) return;
                int[] xs = tabPositions();
                for (int i = 0; i < titles.size(); i++) {
                    if (e.getX() >= xs[i] && e.getX() < xs[i] + 24 + getFontMetrics(ElementTheme.FONT).stringWidth(titles.get(i))) {
                        setSelectedIndex(i);
                        return;
                    }
                }
            }
        });
    }

    public void addTab(String title, JComponent panel) {
        titles.add(title);
        cardPanel.add(panel, String.valueOf(titles.size() - 1));
        if (titles.size() == 1) cards.show(cardPanel, "0");
        repaint();
    }

    public int getSelectedIndex() { return selected; }

    public void setSelectedIndex(int i) {
        if (i < 0 || i >= titles.size() || i == selected) return;
        selected = i;
        cards.show(cardPanel, String.valueOf(i));
        contentAnim.go(0f, 1f);
        slideIndicator();
        repaint();
    }

    private int[] tabPositions() {
        FontMetrics fm = getFontMetrics(ElementTheme.FONT);
        int[] xs = new int[titles.size()];
        int x = 0;
        for (int i = 0; i < titles.size(); i++) {
            xs[i] = x;
            x += 24 + fm.stringWidth(titles.get(i));
        }
        return xs;
    }

    private void slideIndicator() {
        FontMetrics fm = getFontMetrics(ElementTheme.FONT);
        int x = 0;
        for (int i = 0; i < titles.size(); i++) {
            int w = 24 + fm.stringWidth(titles.get(i));
            if (i == selected) {
                indXAnim.go(indX, x);
                indWAnim.go(indW, w);
                return;
            }
            x += w;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT);
        int[] xs = tabPositions();
        for (int i = 0; i < titles.size(); i++) {
            g2.setColor(i == selected ? ElementTheme.PRIMARY : new Color(0x303133));
            g2.setFont(ElementTheme.FONT);
            g2.drawString(titles.get(i), xs[i] + 12, (HEADER_H - fm.getHeight()) / 2f + fm.getAscent());
        }
        if (indX == 0f && indW == 0f && !titles.isEmpty()) {
            indX = xs[selected];
            indW = 24 + fm.stringWidth(titles.get(selected));
        }
        g2.setColor(ElementTheme.PRIMARY);
        g2.fillRect(Math.round(indX), HEADER_H - 2, Math.round(indW), 2);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(480, 240); }
}
```

- [ ] **Step 2: 写 TabsDemo**

创建 `src/org/swelement/demo/TabsDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.AstTabs;
import org.swelement.ui.Tabs;

import javax.swing.*;
import java.awt.*;

public class TabsDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstTabs Demo");
            JPanel p = new JPanel(new FlowLayout(40, 40, 40));
            AstTabs tabs = new AstTabs();
            for (int i = 1; i <= 4; i++) {
                JLabel l = new JLabel("面板 " + i, SwingConstants.CENTER);
                l.setFont(new Font("Microsoft YaHei", Font.PLAIN, 24));
                tabs.addTab("标签 " + i, l);
            }
            p.add(tabs);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 3: 编译 + 启动验收**

Run: `.\build.bat`；`java -cp out org.swelement.demo.TabsDemo` 启动 3 秒、stderr 为空。

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/ui/AstTabs.java src/org/swelement/demo/TabsDemo.java
git commit -m "feat: Tabs with sliding indicator + content fade + demo"
```

---

### Task 4: Pagination + Demo

**Files:**
- Create: `src/org/swelement/ui/Pagination.java`
- Create: `src/org/swelement/demo/PaginationDemo.java`
- Test: `Pagination.selfCheck()`（public main）+ Demo 视觉验收

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Pagination extends JComponent`；构造 `Pagination()`；方法 `void setTotal(int)`、`void setPageSize(int)`、`int getCurrentPage()`、`void setCurrentPage(int)`（钳制 1..总页数）、`void addPageChangeListener(java.util.function.IntConsumer)`；静态 `java.util.List<Integer> pageWindow(int current, int pages)`（-1 表示省略号）。

- [ ] **Step 1: 写 Pagination**

创建 `src/org/swelement/ui/Pagination.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class Pagination extends JComponent {
    private int total, pageSize = 10, current = 1;
    private final List<IntConsumer> listeners = new ArrayList<>();
    private final JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
    private final JTextField jumper = new JTextField(3);

    public Pagination() {
        setOpaque(false);
        setLayout(new BorderLayout());
        row.setOpaque(false);
        add(row, BorderLayout.CENTER);
        jumper.setFont(ElementTheme.FONT.deriveFont(12f));
        jumper.setBorder(new EmptyBorder(2, 4, 2, 4));
        jumper.addActionListener(this::onJump);
    }

    public void setTotal(int t) { total = Math.max(0, t); rebuild(); }

    public void setPageSize(int s) { pageSize = Math.max(1, s); rebuild(); }

    public int getCurrentPage() { return current; }

    public void setCurrentPage(int v) {
        int pages = pages();
        current = Math.max(1, Math.min(pages, v));
        rebuild();
        for (IntConsumer l : listeners) l.accept(current);
    }

    public void addPageChangeListener(IntConsumer l) { listeners.add(l); }

    private int pages() { return total == 0 ? 1 : (total + pageSize - 1) / pageSize; }

    static List<Integer> pageWindow(int cur, int pages) {
        List<Integer> out = new ArrayList<>();
        for (int p = 1; p <= pages; p++) {
            if (p == 1 || p == pages || Math.abs(p - cur) <= 2) out.add(p);
            else if (out.isEmpty() || out.get(out.size() - 1) != -1) out.add(-1);
        }
        return out;
    }

    private void rebuild() {
        row.removeAll();
        row.add(pageButton("\u2039", current > 1 ? current - 1 : -1));     // ‹
        for (int p : pageWindow(current, pages())) {
            if (p == -1) {
                JLabel dots = new JLabel("…");
                dots.setForeground(new Color(0x909399));
                row.add(dots);
            } else {
                row.add(pageButton(String.valueOf(p), p));
            }
        }
        row.add(pageButton("\u203a", current < pages() ? current + 1 : -1)); // ›
        row.add(new JLabel("共 " + total + " 条"));
        JLabel go = new JLabel("前往");
        go.setForeground(new Color(0x606266));
        row.add(go);
        row.add(jumper);
        JLabel page = new JLabel("页");
        page.setForeground(new Color(0x606266));
        row.add(page);
        row.revalidate();
        row.repaint();
    }

    private JComponent pageButton(String text, int page) {
        JLabel b = new JLabel(text) {
            private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
            private float hover;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = page == current && page > 0;
                if (active || hover > 0) {
                    g2.setColor(active ? ElementTheme.PRIMARY : ElementTheme.lerp(Color.WHITE, new Color(0xF5F7FA), hover));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                }
                g2.setColor(active ? Color.WHITE : (page > 0 ? new Color(0x606266) : new Color(0xC0C4CC)));
                FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT.deriveFont(12f));
                g2.setFont(ElementTheme.FONT.deriveFont(12f));
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                g2.dispose();
            }
        };
        b.setOpaque(false);
        b.setPreferredSize(new Dimension(page > 0 ? 28 : 24, 28));
        b.setFont(ElementTheme.FONT.deriveFont(12f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (page > 0) {
            b.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { b.repaint(); }
                public void mouseExited(MouseEvent e) { b.repaint(); }
                public void mouseClicked(MouseEvent e) { setCurrentPage(page); }
            });
        }
        return b;
    }

    private void onJump(ActionEvent e) {
        try {
            int p = Integer.parseInt(jumper.getText().trim());
            setCurrentPage(p);
            jumper.setText("");
        } catch (NumberFormatException ignore) { }
    }

    public static void selfCheck() {
        assert pageWindow(1, 10).equals(java.util.Arrays.asList(1, 2, 3, -1, 10));
        assert pageWindow(5, 10).equals(java.util.Arrays.asList(1, -1, 3, 4, 5, 6, 7, -1, 10));
        assert pageWindow(9, 10).equals(java.util.Arrays.asList(1, -1, 7, 8, 9, 10));
        assert pageWindow(1, 1).equals(java.util.Arrays.asList(1));
        assert pageWindow(1, 3).equals(java.util.Arrays.asList(1, 2, 3));
        System.out.println("AstPagination self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
```

> 注：`pageButton` 内的匿名 JLabel 持有一个 hover 字段并在 mouseEntered/Exited 里只是 repaint（hover 由 paintComponent 里的匿名 Animator 回调驱动——回调里 `hover = v; repaint();` 是匿名内部类的字段，与 mouseEntered 无关）。hover 动画启动点缺失：修复见 Step 2。

- [ ] **Step 2: 修正 pageButton hover 动画启动点**

`pageButton` 中鼠标监听器需要真正驱动动画。将 mouseEntered/Exited 改为：

```java
        if (page > 0) {
            b.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { startHover(b, 1f); }
                public void mouseExited(MouseEvent e) { startHover(b, 0f); }
                public void mouseClicked(MouseEvent e) { setCurrentPage(page); }
            });
        }
```

并在 `AstPagination` 类中新增静态辅助（从组件上取 animator 有难度，改为让匿名类直接持引用——将 Step 1 的匿名 JLabel 改为普通内部类 `PageButton extends JLabel`，构造 `PageButton(String text, int page)`，hover 动画字段与监听全在类内部，鼠标监听直接 `hoverAnim.go(hover, ...)`）。即：**删除 Step 1 的 pageButton 匿名类版本，改用以下 PageButton 内部类**：

```java
    private class PageButton extends JLabel {
        private final int page;
        private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
        private float hover;

        PageButton(String text, int page) {
            super(text);
            this.page = page;
            setOpaque(false);
            setPreferredSize(new Dimension(page > 0 ? 28 : 24, 28));
            setFont(ElementTheme.FONT.deriveFont(12f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (page > 0) {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hoverAnim.go(hover, 1f); }
                    public void mouseExited(MouseEvent e) { hoverAnim.go(hover, 0f); }
                    public void mouseClicked(MouseEvent e) { setCurrentPage(page); }
                });
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean active = page == current && page > 0;
            if (active || hover > 0) {
                g2.setColor(active ? ElementTheme.PRIMARY : ElementTheme.lerp(Color.WHITE, new Color(0xF5F7FA), hover));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            }
            g2.setColor(active ? Color.WHITE : (page > 0 ? new Color(0x606266) : new Color(0xC0C4CC)));
            FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT.deriveFont(12f));
            g2.setFont(ElementTheme.FONT.deriveFont(12f));
            g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }
```

将 `rebuild()` 中的 `row.add(pageButton("\u2039", ...))` / `pageButton(String.valueOf(p), p)` / `pageButton("\u203a", ...)` 全部替换为 `row.add(new PageButton(text, page))`，并删除 Step 1 的 `pageButton` 方法（返回 JComponent 的版本）。

- [ ] **Step 3: 写 PaginationDemo**

创建 `src/org/swelement/demo/PaginationDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.AstPagination;

import javax.swing.*;
import java.awt.*;

public class PaginationDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstPagination Demo");
            JPanel p = new JPanel(new FlowLayout(40, 40, 40));
            AstPagination pg = new AstPagination();
            pg.setTotal(256);
            JLabel info = new JLabel("当前页: 1");
            pg.addPageChangeListener(v -> info.setText("当前页: " + v));
            p.add(pg);
            p.add(info);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 4: 编译 + 自检 + 启动验收**

Run: `.\build.bat`；`java -ea -cp out org.swelement.ui.Pagination` → `Pagination self-check OK`；Demo 启动 3 秒 stderr 为空。

- [ ] **Step 5: 提交**

```bash
git add src/org/swelement/ui/AstPagination.java src/org/swelement/demo/PaginationDemo.java
git commit -m "feat: Pagination with page window + hover + demo"
```

---

### Task 5: Menu + Demo

**Files:**
- Create: `src/org/swelement/ui/Menu.java`
- Create: `src/org/swelement/demo/MenuDemo.java`
- Test: `MenuDemo` 视觉验收

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`, `AnimatedPopup`
- Produces: `Menu extends JComponent`；方法 `void addMenuItem(String label, Runnable action)`、`void addSubMenu(String label, String[] subLabels, Runnable[] actions)`（两数组等长）、`void setActive(int index)`。

- [ ] **Step 1: 写 Menu**

创建 `src/org/swelement/ui/Menu.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.AnimatedPopup;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Menu extends JComponent {
    private static final int HEADER_H = 40;

    private static class Entry {
        final String label;
        final Runnable action;
        final String[] subLabels;
        final Runnable[] subActions;
        final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
        float hover;

        Entry(String label, Runnable action) { this(label, action, null, null); }

        Entry(String label, Runnable action, String[] subLabels, Runnable[] subActions) {
            this.label = label;
            this.action = action;
            this.subLabels = subLabels;
            this.subActions = subActions;
        }

        boolean isSub() { return subLabels != null; }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Animator indXAnim = new Animator(250, Easing::easeInOut, v -> { indX = v; repaint(); });
    private final Animator indWAnim = new Animator(250, Easing::easeInOut, v -> { indW = v; repaint(); });
    private float indX, indW;
    private int active = -1;
    private final AnimatedPopup subPopup = new AnimatedPopup();
    private final JPanel subList = new JPanel();

    public Menu() {
        setOpaque(false);
        setPreferredSize(new Dimension(520, HEADER_H));
        subList.setOpaque(false);
        subList.setLayout(new BoxLayout(subList, BoxLayout.Y_AXIS));
        subPopup.getContent().add(subList, BorderLayout.CENTER);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getY() > HEADER_H) return;
                int x = 0;
                for (int i = 0; i < entries.size(); i++) {
                    Entry en = entries.get(i);
                    int w = entryWidth(en);
                    if (e.getX() >= x && e.getX() < x + w) {
                        onEntryClick(i, en);
                        return;
                    }
                    x += w;
                }
            }
        });
    }

    public void addMenuItem(String label, Runnable action) { entries.add(new Entry(label, action)); repaint(); }

    public void addSubMenu(String label, String[] subLabels, Runnable[] subActions) {
        entries.add(new Entry(label, null, subLabels, subActions));
        repaint();
    }

    public void setActive(int index) { active = index; slideIndicator(); repaint(); }

    private int entryWidth(Entry en) {
        return 24 + getFontMetrics(ElementTheme.FONT).stringWidth(en.label);
    }

    private void onEntryClick(int i, Entry en) {
        setActive(i);
        if (!en.isSub()) {
            if (en.action != null) en.action.run();
            return;
        }
        subList.removeAll();
        for (int s = 0; s < en.subLabels.length; s++) {
            final Runnable a = en.subActions[s];
            JLabel item = new JLabel(en.subLabels[s]) {
                private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
                private float hover;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (hover > 0) {
                        g2.setColor(ElementTheme.lerp(Color.WHITE, new Color(0xECF5FF), hover));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    g2.setColor(ElementTheme.TEXT_REGULAR);
                    FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT);
                    g2.drawString(getText(), 16, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                    g2.dispose();
                }
            };
            item.setOpaque(false);
            item.setPreferredSize(new Dimension(140, 32));
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final int fi = s;
            item.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { item.repaint(); }
                public void mouseExited(MouseEvent e) { item.repaint(); }
                public void mouseClicked(MouseEvent e) {
                    subPopup.setVisible(false);
                    if (a != null) a.run();
                }
            });
            subList.add(item);
        }
        subList.revalidate();
        int x = 0;
        for (int k = 0; k < i; k++) x += entryWidth(entries.get(k));
        subPopup.getContent().setPreferredSize(new Dimension(140, subList.getPreferredSize().height));
        subPopup.show(this, x, HEADER_H);
    }

    private void slideIndicator() {
        int x = 0;
        for (int i = 0; i < entries.size(); i++) {
            int w = entryWidth(entries.get(i));
            if (i == active) {
                indXAnim.go(indX, x);
                indWAnim.go(indW, w);
                return;
            }
            x += w;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT);
        int x = 0;
        for (Entry en : entries) {
            int w = entryWidth(en);
            en.hoverAnim.go(en.hover, 0f);   // 见下方注：此调用应在鼠标事件中，此处仅占位由 Step 2 修正
            g2.setColor(en.hover > 0 ? ElementTheme.lerp(Color.WHITE, new Color(0xECF5FF), en.hover) : Color.WHITE);
            g2.fillRect(x, 0, w, HEADER_H);
            g2.setColor(en.hover > 0.5f || entries.indexOf(en) == active ? ElementTheme.PRIMARY : new Color(0x303133));
            g2.setFont(ElementTheme.FONT);
            g2.drawString(en.label, x + 12, (HEADER_H - fm.getHeight()) / 2f + fm.getAscent());
            x += w;
        }
        if (indX == 0f && indW == 0f && active >= 0) slideIndicator();
        if (active >= 0) {
            g2.setColor(ElementTheme.PRIMARY);
            g2.fillRect(Math.round(indX), HEADER_H - 2, Math.round(indW), 2);
        }
        g2.dispose();
    }
}
```

> 注：`paintComponent` 里调用 `en.hoverAnim.go(...)` 违反全局约束（动画禁止在 paint 中启动）。Step 2 修正：删除该行，hover 动画改由鼠标事件驱动（Menu 需要跟踪鼠标所在条目）。

- [ ] **Step 2: 修正 hover 动画驱动方式**

删除 `paintComponent` 中的 `en.hoverAnim.go(en.hover, 0f);` 行（以及 `// 见下方注` 注释）。在 `AstMenu` 构造器中增加鼠标移动/进出跟踪：

```java
        addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                if (e.getY() > HEADER_H) return;
                int x = 0;
                for (Entry en : entries) {
                    int w = entryWidth(en);
                    boolean over = e.getX() >= x && e.getX() < x + w;
                    en.hoverAnim.go(en.hover, over ? 1f : 0f);
                    x += w;
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                for (Entry en : entries) en.hoverAnim.go(en.hover, 0f);
            }
            public void mouseClicked(MouseEvent e) { /* 保留原点击逻辑 */ }
        });
```

即：原点击 MouseAdapter 保留并补充 `mouseExited`，新增 motion listener 负责逐条目 hover 过渡。

- [ ] **Step 3: 写 MenuDemo**

创建 `src/org/swelement/demo/MenuDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.AstMenu;

import javax.swing.*;
import java.awt.*;

public class MenuDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstMenu Demo");
            JPanel p = new JPanel(new BorderLayout());
            AstMenu menu = new AstMenu();
            menu.addMenuItem("首页", () -> System.out.println("home"));
            menu.addMenuItem("新闻", () -> System.out.println("news"));
            menu.addSubMenu("关于", new String[]{"项目", "团队", "联系方式"},
                    new Runnable[]{() -> System.out.println("project"), () -> System.out.println("team"), () -> System.out.println("contact")});
            p.add(menu, BorderLayout.NORTH);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.setSize(640, 200);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 4: 编译 + 启动验收**

Run: `.\build.bat`；`java -cp out org.swelement.demo.MenuDemo` 启动 3 秒 stderr 为空。

- [ ] **Step 5: 提交**

```bash
git add src/org/swelement/ui/AstMenu.java src/org/swelement/demo/MenuDemo.java
git commit -m "feat: horizontal Menu with animated underline + submenu popup + demo"
```

---

### Task 6: Tag + Demo

**Files:**
- Create: `src/org/swelement/ui/Tag.java`
- Create: `src/org/swelement/demo/TagDemo.java`
- Test: `TagDemo` 视觉验收

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Tag extends JComponent`；类型常量 `PRIMARY=0 SUCCESS=1 WARNING=2 DANGER=3 INFO=4`；构造 `Tag(String text, int type, boolean closable)`；方法 `void close(Runnable onClosed)`（动画结束后回调）、`void setText(String)`。

- [ ] **Step 1: 写 Tag**

创建 `src/org/swelement/ui/Tag.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Tag extends JComponent {
    public static final int PRIMARY = 0, SUCCESS = 1, WARNING = 2, DANGER = 3, INFO = 4;

    private static final Color[] BG = {new Color(0xECF5FF), new Color(0xF0F9EB), new Color(0xFDF6EC), new Color(0xFEF0F0), new Color(0xF4F4F5)};
    private static final Color[] FG = {ElementTheme.PRIMARY, ElementTheme.SUCCESS, ElementTheme.WARNING, ElementTheme.DANGER, ElementTheme.INFO};
    private static final Color[] BORDER = {new Color(0xD9ECFF), new Color(0xE1F3D8), new Color(0xFAECD8), new Color(0xFDE2E2), new Color(0xE9E9EB)};

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
    private Runnable onClosed;
    private int origW, origH;
    private final int type;
    private final boolean closable;
    private String text;

    public Tag(String text, int type, boolean closable) {
        this.text = text;
        this.type = type;
        this.closable = closable;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setText(String t) { text = t; repaint(); }

    public void close(Runnable onClosed) {
        this.onClosed = onClosed;
        origW = getWidth();
        origH = getHeight();
        closeAnim.go(0f, 1f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(BG[type]);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
        g2.setColor(BORDER[type]);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
        g2.setColor(FG[type]);
        FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT.deriveFont(12f));
        g2.setFont(ElementTheme.FONT.deriveFont(12f));
        String suffix = closable ? "  ×" : "";
        g2.drawString(text + suffix, 8, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(ElementTheme.FONT.deriveFont(12f));
        return new Dimension(16 + fm.stringWidth(text + (closable ? "  ×" : "")), fm.getHeight() + 8);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (closable) {
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    FontMetrics fm = getFontMetrics(ElementTheme.FONT.deriveFont(12f));
                    int xw = 16 + fm.stringWidth(text);
                    if (e.getX() > xw) close(() -> {});
                }
            });
        }
    }
}
```

- [ ] **Step 2: 写 TagDemo**

创建 `src/org/swelement/demo/TagDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.AstTag;
import org.swelement.ui.Tag;

import javax.swing.*;
import java.awt.*;

public class TagDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstTag Demo");
            JPanel p = new JPanel(new FlowLayout(20, 20, 20));
            AstTag t1 = new AstTag("标签一", AstTag.DEFAULT_SAFE, false);
            p.add(new AstTag("默认", AstTag.PRIMARY, false));
            p.add(new AstTag("成功", AstTag.SUCCESS, false));
            p.add(new AstTag("警告", AstTag.WARNING, false));
            p.add(new AstTag("危险", AstTag.DANGER, true));
            p.add(new AstTag("信息", AstTag.INFO, false));
            p.add(new AstTag("可删除", AstTag.DANGER, true));
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

> 注：Step 2 的 demo 里删掉 `Tag.DEFAULT_SAFE` 那行（该常量不存在——Tag 没有 DEFAULT 类型，5 个类型即 PRIMARY/SUCCESS/WARNING/DANGER/INFO）。

- [ ] **Step 3: 编译 + 启动验收**

Run: `.\build.bat`；`java -cp out org.swelement.demo.TagDemo` 启动 3 秒 stderr 为空。点击"可删除"标签的 × 应看到收缩动画。

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/ui/AstTag.java src/org/swelement/demo/TagDemo.java
git commit -m "feat: Tag with types and shrink-close animation + demo"
```

---

### Task 7: Progress + Demo

**Files:**
- Create: `src/org/swelement/ui/Progress.java`
- Create: `src/org/swelement/demo/ProgressDemo.java`
- Test: `ProgressDemo` 视觉验收

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Progress extends JComponent`；构造 `Progress()`；方法 `void setValue(int)`（0-100 钳制）、`void setShowText(boolean)`。

- [ ] **Step 1: 写 Progress**

创建 `src/org/swelement/ui/Progress.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;

public class Progress extends JComponent {
    private final Animator fillAnim = new Animator(300, Easing::easeOut, v -> { shown = v; repaint(); });
    private float shown;
    private int value;
    private boolean showText = true;

    public Progress() {
        setOpaque(false);
        setPreferredSize(new Dimension(320, 20));
    }

    public void setValue(int v) {
        value = Math.max(0, Math.min(100, v));
        fillAnim.go(shown, value / 100f);
        repaint();
    }

    public void setShowText(boolean b) { showText = b; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int textW = showText ? 46 : 0;
        int trackW = getWidth() - textW;
        int y = (getHeight() - 6) / 2;
        g2.setColor(new Color(0xEBEEF5));
        g2.fillRoundRect(0, y, trackW, 6, 6, 6);
        int fillW = Math.round(trackW * shown);
        g2.setColor(ElementTheme.PRIMARY);
        g2.fillRoundRect(0, y, fillW, 6, 6, 6);
        if (showText) {
            g2.setColor(new Color(0x606266));
            FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT.deriveFont(12f));
            g2.setFont(ElementTheme.FONT.deriveFont(12f));
            g2.drawString(value + "%", trackW + 6, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        }
        g2.dispose();
    }
}
```

- [ ] **Step 2: 写 ProgressDemo**

创建 `src/org/swelement/demo/ProgressDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.AstProgress;
import org.swelement.ui.Progress;

import javax.swing.*;
import java.awt.*;

public class ProgressDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstProgress Demo");
            JPanel p = new JPanel(new GridLayout(4, 1, 10, 10));
            p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            AstProgress a = new AstProgress();
            p.add(a);
            AstProgress b = new AstProgress();
            b.setShowText(false);
            p.add(b);
            AstProgress c = new AstProgress();
            p.add(c);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            new Timer(60, e -> {
                int v = a.getValue();
                a.setValue(v + 1);
                b.setValue((v + 30) % 100);
                c.setValue((int) (Math.random() * 100));
            }).start();
        });
    }
}
```

> 注：`AstProgress` 没有 `getValue()` —— Step 2 里 a 的递增改用计数器变量。修正为：

```java
            int[] counter = {0};
            new Timer(60, e -> {
                counter[0]++;
                a.setValue(counter[0] % 101);
                b.setValue((counter[0] + 30) % 100);
                c.setValue((int) (Math.random() * 100));
            }).start();
```

（替换 `int v = a.getValue(); ...` 三行。）

- [ ] **Step 3: 编译 + 启动验收**

Run: `.\build.bat`；`java -cp out org.swelement.demo.ProgressDemo` 启动 3 秒 stderr 为空（窗口内有 Timer 持续跑进度动画）。

- [ ] **Step 4: 提交**

```bash
git add src/org/swelement/ui/AstProgress.java src/org/swelement/demo/ProgressDemo.java
git commit -m "feat: Progress bar with animated fill + demo"
```

---

### Task 8: Badge + Demo

**Files:**
- Create: `src/org/swelement/ui/Badge.java`
- Create: `src/org/swelement/demo/BadgeDemo.java`
- Test: `BadgeDemo` 视觉验收

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Badge extends JComponent`；构造 `Badge()`（用 `setLayout(new BorderLayout())` 放入子组件，子组件占满内容区）；方法 `void setCount(int)`（0 隐藏）、`void setDot(boolean)`、`void setContent(JComponent)`。

- [ ] **Step 1: 写 Badge**

创建 `src/org/swelement/ui/Badge.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;

import javax.swing.*;
import java.awt.*;

public class Badge extends JComponent {
    private final Animator popAnim = new Animitor(200, Easing::easeOut, v -> { scale = v; repaint(); });
    private float scale = 1f;
    private int count;
    private boolean dot;
    private JComponent content;

    public Badge() {
        setOpaque(false);
        setLayout(new BorderLayout());
    }

    public void setContent(JComponent c) {
        if (content != null) remove(content);
        content = c;
        add(content, BorderLayout.CENTER);
        revalidate();
    }

    public void setCount(int c) {
        count = c;
        scale = 0.6f;
        popAnim.go(scale, 1f);
        repaint();
    }

    public void setDot(boolean b) { dot = b; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        if (count <= 0 && !dot) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = dot ? 10 : 18;
        int cx = getWidth() - size / 2;
        int cy = size / 2;
        g2.setColor(new Color(0xF56C6C));
        g2.fillOval(cx - size / 2, cy - size / 2, size, size);
        if (!dot && count > 99) {
            size = 24;
            g2.setColor(new Color(0xF56C6C));
            g2.fillRoundRect(cx - size / 2, cy - size / 2, size, size, 8, 8);
        }
        if (!dot) {
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics(getFont());
            String text = count > 99 ? "99+" : String.valueOf(count);
            g2.drawString(text, cx - fm.stringWidth(text) / 2f, cy - fm.getHeight() / 2f + fm.getAscent());
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return content != null ? content.getPreferredSize() : new Dimension(48, 48);
    }
}
```

> 注：Step 1 有一处拼写错误：`Animitor` → `Animator`；且 `scale` 字段声明但实际未用于绘制（修正见 Step 2）。

- [ ] **Step 2: 修正拼写并让 scale 生效**

1. `new Animitor(...)` 改为 `new Animator(...)`。
2. 在 `paintComponent` 中应用缩放弹出效果：绘制角标前加

```java
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = getWidth() - 9;
        int cy = 9;
        float s = 0.6f + 0.4f * scale;
        g2.translate(cx, cy);
        g2.scale(s, s);
        g2.translate(-cx, -cy);
        int size = dot ? 10 : 18;
        g2.setColor(new Color(0xF56C6C));
        g2.fillOval(cx - size / 2, cy - size / 2, size, size);
```

（替换原 paintComponent 的 cx/cy/size/fillOval 部分，圆角 99+ 分支保持，文本绘制保持。）

- [ ] **Step 3: 写 BadgeDemo**

创建 `src/org/swelement/demo/BadgeDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.AstBadge;
import org.swelement.ui.Badge;

import javax.swing.*;
import java.awt.*;

public class BadgeDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstBadge Demo");
            JPanel p = new JPanel(new FlowLayout(60, 40, 40));
            AstBadge b1 = new AstBadge();
            b1.setContent(new JButton("消息"));
            b1.setCount(8);
            p.add(b1);
            AstBadge b2 = new AstBadge();
            b2.setContent(new JButton("评论"));
            b2.setCount(100);
            p.add(b2);
            AstBadge b3 = new AstBadge();
            b3.setContent(new JButton("通知"));
            b3.setDot(true);
            p.add(b3);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            int[] n = {8};
            new Timer(1200, e -> {
                n[0]++;
                b1.setCount(n[0]);
                if (n[0] % 3 == 0) b3.setDot(n[0] % 6 == 0);
            }).start();
        });
    }
}
```

- [ ] **Step 4: 编译 + 启动验收**

Run: `.\build.bat`；`java -cp out org.swelement.demo.BadgeDemo` 启动 3 秒 stderr 为空（数字周期变化带缩放弹出）。

- [ ] **Step 5: 提交**

```bash
git add src/org/swelement/ui/AstBadge.java src/org/swelement/demo/BadgeDemo.java
git commit -m "feat: Badge with pop animation + demo"
```

---

### Task 9: Alert + Demo

**Files:**
- Create: `src/org/swelement/ui/Alert.java`
- Create: `src/org/swelement/demo/AlertDemo.java`
- Test: `AlertDemo` 视觉验收

**Interfaces:**
- Consumes: `Animator`, `Easing`, `ElementTheme`
- Produces: `Alert extends JComponent`；类型常量 `SUCCESS=0 WARNING=1 INFO=2 ERROR=3`；构造 `Alert(int type, String title, String desc, boolean closable)`；方法 `void close(Runnable onClosed)`。

- [ ] **Step 1: 写 Alert**

创建 `src/org/swelement/ui/Alert.java`：

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Alert extends JComponent {
    public static final int SUCCESS = 0, WARNING = 1, INFO = 2, ERROR = 3;

    private static final Color[] COLORS = {ElementTheme.SUCCESS, ElementTheme.WARNING, ElementTheme.INFO, ElementTheme.DANGER};
    private static final Color[] BG = {new Color(0xF0F9EB), new Color(0xFDF6EC), new Color(0xF4F4F5), new Color(0xFEF0F0)};
    private static final String[] ICONS = {"\u221a", "!", "i", "\u00d7"};

    private final Animator inAnim = new Animator(300, Easing::easeOut, v -> { inP = v; repaint(); });
    private final Animator outAnim = new Animator(250, Easing::easeIn, v -> {
        outP = v;
        int h = Math.max(1, Math.round(origH * (1 - v)));
        setPreferredSize(new Dimension(origW, h));
        revalidate();
        if (v >= 1f && onClosed != null) {
            Runnable r = onClosed;
            onClosed = null;
            r.run();
        }
        repaint();
    });
    private float inP = 1f, outP;
    private Runnable onClosed;
    private int origW, origH;
    private final int type;
    private final String title, desc;
    private final boolean closable;

    public Alert(int type, String title, String desc, boolean closable) {
        this.type = type;
        this.title = title;
        this.desc = desc;
        this.closable = closable;
        setOpaque(false);
        setPreferredSize(new Dimension(360, desc == null ? 40 : 56));
        if (closable) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getX() > getWidth() - 28) close(() -> {});
                }
            });
        }
    }

    public void close(Runnable onClosed) {
        this.onClosed = onClosed;
        origW = getWidth();
        origH = getHeight();
        outAnim.go(0f, 1f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int a = Math.round(255 * inP * (1 - outP));
        if (a <= 0) return;
        g2.setColor(new Color(BG[type].getRed(), BG[type].getGreen(), BG[type].getBlue(), a));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(COLORS[type].getRed(), COLORS[type].getGreen(), COLORS[type].getBlue(), a));
        g2.fillRect(0, 0, 4, getHeight());
        g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(ICONS[type], 16, (desc == null ? getHeight() : 22));
        g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD));
        FontMetrics tfm = g2.getFontMetrics();
        g2.drawString(title, 40, (desc == null ? getHeight() : 22) - tfm.getAscent() / 2f + tfm.getAscent() / 2f);
        if (desc != null) {
            g2.setFont(ElementTheme.FONT);
            g2.setColor(new Color(0x606266));
            g2.drawString(desc, 40, 42);
        }
        if (closable) {
            g2.setFont(ElementTheme.FONT.deriveFont(14f));
            FontMetrics xfm = g2.getFontMetrics();
            g2.setColor(new Color(0xC0C4CC));
            g2.drawString("\u00d7", getWidth() - 24 - xfm.stringWidth("\u00d7") / 2, (getHeight() + 8) / 2f);
        }
        g2.dispose();
    }
}
```

> 注：`inP` 动画从未被触发（构造后直接 1f）。Step 2 修正：构造后立即 `inAnim.go(0f, 1f)` 让出现时淡入（若不需要出现动画可忽略此注，但规格要求"出现淡入"）。

- [ ] **Step 2: 触发出现动画**

在 `AstAlert` 构造器末尾（`setPreferredSize` 之后）加一行：

```java
        inAnim.go(0f, 1f);
```

（使 `inP` 从 0 动画到 1，出现淡入。）

- [ ] **Step 3: 写 AlertDemo**

创建 `src/org/swelement/demo/AlertDemo.java`：

```java
package org.swelement.demo;

import org.swelement.ui.AstAlert;

import javax.swing.*;

public class AlertDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstAlert Demo");
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            p.add(new AstAlert(AstAlert.SUCCESS, "成功提示", "这是一条成功提示信息", true));
            p.add(Box.createVerticalStrut(10));
            p.add(new AstAlert(AstAlert.WARNING, "警告提示", "这是一条警告提示信息", true));
            p.add(Box.createVerticalStrut(10));
            p.add(new AstAlert(AstAlert.INFO, "消息提示", null, false));
            p.add(Box.createVerticalStrut(10));
            p.add(new AstAlert(AstAlert.ERROR, "错误提示", "这是一条错误提示信息", true));
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 4: 编译 + 启动验收**

Run: `.\build.bat`；`java -cp out org.swelement.demo.AlertDemo` 启动 3 秒 stderr 为空。点击右上角 × 观察高度收缩。

- [ ] **Step 5: 提交**

```bash
git add src/org/swelement/ui/AstAlert.java src/org/swelement/demo/AlertDemo.java
git commit -m "feat: Alert with types and collapse-close animation + demo"
```

---

### Task 10: 全量回归 + README 更新

**Files:**
- Modify: `README.md`（追加 8 个新 Demo 命令）
- Test: 全量回归

- [ ] **Step 1: 全量编译 + 全部自检 + 全部 Demo 可启动**

Run: `.\build.bat`，然后依次运行（自检必须输出 OK）：
```
java -ea -cp out org.swelement.core.Easing
java -ea -cp out org.swelement.core.ElementTheme
java -ea -cp out org.swelement.core.Animator
java -ea -cp out org.swelement.ui.AstSelect
java -ea -cp out org.swelement.ui.AstPagination
```
随后 14 个 Demo（6 个 Phase 1 + 8 个 Phase 2）各启动 3 秒、stderr 为空：ButtonDemo, InputDemo, CheckboxDemo, RadioDemo, SwitchDemo, SliderDemo, SelectDemo, TabsDemo, PaginationDemo, MenuDemo, TagDemo, ProgressDemo, BadgeDemo, AlertDemo。

- [ ] **Step 2: 更新 README**

在 `README.md` 的 `## 运行 Demo` 段落后追加：

```markdown
java -cp out org.swelement.demo.SelectDemo
java -cp out org.swelement.demo.TabsDemo
java -cp out org.swelement.demo.PaginationDemo
java -cp out org.swelement.demo.MenuDemo
java -cp out org.swelement.demo.TagDemo
java -cp out org.swelement.demo.ProgressDemo
java -cp out org.swelement.demo.BadgeDemo
java -cp out org.swelement.demo.AlertDemo
```

并在 `## 核心自检` 段追加：

```markdown
java -ea -cp out org.swelement.ui.AstSelect
java -ea -cp out org.swelement.ui.AstPagination
```

- [ ] **Step 3: 提交**

```bash
git add README.md
git commit -m "docs: README phase 2 demos and self-checks"
```

---

## Self-Review

**1. Spec coverage:**
- AnimatedPopup ✓ Task 1；Select（多选+搜索+分组+可清空）✓ Task 2；Tabs ✓ Task 3；Pagination ✓ Task 4；Menu ✓ Task 5；Tag ✓ Task 6；Progress ✓ Task 7；Badge ✓ Task 8；Alert ✓ Task 9；回归+README ✓ Task 10
- 动画要求逐项落实：箭头旋转/下拉淡入下滑/选项 hover/多选 chip ✓ Task 2；指示条滑动+内容淡入 ✓ Task 3；hover 过渡+当前页高亮 ✓ Task 4；下划线滑动+子菜单淡入 ✓ Task 5；Tag 收缩淡出 ✓ Task 6；进度填充动画 ✓ Task 7；数字缩放弹出 ✓ Task 8；Alert 淡入+收缩 ✓ Task 9
- 自检 main（public）✓ Task 2/4

**2. Placeholder scan:** 无 TBD。Task 2/4/5/6/8/9 各含一处"修正步骤"，全部给出具体替换代码，无占位。

**3. Type consistency:** `Animator(int, Easing, Listener)`、`go(float, float)`、`ElementTheme.lerp` 三重重载、`AnimatedPopup.getContent()` 在 Task 2/5 中用法一致；`Select.Option` 构造签名在 Demo 中一致；`AstTag` 类型常量与 Demo 一致；`AstProgress`/`AstBadge`/`AstAlert` 构造与 Demo 一致。Task 8 修正 `Animitor`→`Animator`（拼写），Task 9 修正 `inP` 未触发（构造后启动动画），Task 6 修正 Demo 引用了不存在的 `Tag.DEFAULT_SAFE`。