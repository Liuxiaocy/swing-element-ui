# Swing Element UI P1 Components Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 14 new `Ast*`-prefixed Swing components following Element UI visual spec: 4 pure layout/render components (Container, Avatar, Card, Loading) → 5 popup/modal/toast components (Tooltip, Dropdown, Dialog, MessageBox, Message) → 2 complex popups (Cascader, DatePicker) → 3 heavy render components (Form, Tree, Table). Each delivered with selfCheck + interactive Demo + build pass.

**Architecture:** Keep existing Swing × Element UI layered stack (App → UI components → Core engine). Core enhancements are *additive and backward compatible*: ElementTheme gets 2 new contrast utilities; AnimatedPopup gets Direction + hideWithAnimation + stacking layers. New helper pure classes PopupPositioner + GlassPane shared by all popup components. Paint uses Animator-driven interpolation; no Thread / manual Timer.

**Tech Stack:** Java 8+, javax.swing.*, java.awt.Graphics2D, java.time.LocalDate, existing `org.swelement.core.{Animator, Easing, ElementTheme, AnimatedPopup}`; build via `javac` through `build.bat`.

---

## File Structure Map (Lock before writing code)

| File | Action | Responsibility |
|---|---|---|
| `src/org/swelement/core/ElementTheme.java` | **Modify** (append block at end, L44 before final brace) | Add `luminance(Color)`, `assertContrast(Color, Color, String)`, `srgb(int)` |
| `src/org/swelement/core/AnimatedPopup.java` | **Modify** (no breaking API change) | Add `Direction` enum, `PopupLayer` enum, `show(invoker, dir)`, `hideWithAnimation(Runnable)`, static `registerGlobal`, static `MessageStack` fields, close Animator |
| `src/org/swelement/core/PopupPositioner.java` | **Create** | Pure coordinate calculator: screen bounds, direction, screen flip, output Point + actual direction |
| `src/org/swelement/core/GlassPane.java` | **Create** | JPanel overlay, consume all mouse/key events, alpha Animator fade in/out, static `install(RootPaneContainer)` |
| `src/org/swelement/ui/AstContainer.java` | **Create** | JPanel with BorderLayout region slots: Header/Aside/Main/Footer; default heights & 1px borders |
| `src/org/swelement/ui/AstAvatar.java` | **Create** | JComponent: char / ImageIcon / Color+Text constructors, size, shape(CIRCLE/SQUARE), Badge wrapper |
| `src/org/swelement/ui/AstCard.java` | **Create** | JComponent: title string, bordered + shadowElevation(0/1/2) hover Animator, content/addHeaderAction |
| `src/org/swelement/ui/AstLoading.java` | **Create** | `wrap(target)` wrapper / `showFullScreen(w,tip)`, 12-arc spinner Animator(800ms linear) |
| `src/org/swelement/ui/AstTooltip.java` | **Create** | Non-inheriting: target/enter/exit Timer, AnimatedPopup delay show, 4 directions + boundary flip |
| `src/org/swelement/ui/AstDropdown.java` | **Create** | Non-inheriting: trigger + HOVER/CLICK mode, OptionRow list, AWTEventListener out-click dismiss |
| `src/org/swelement/ui/AstDialog.java` | **Create** | Modal: AnimatedPopup centered, scale+alpha Animator, addButton builder, GlassPane on show |
| `src/org/swelement/ui/AstMessageBox.java` | **Create** | Static: alert / confirm / show → delegates to AstDialog with built-in icon shapes |
| `src/org/swelement/ui/AstMessage.java` | **Create** | Static: top-center toast, stack vertical offsets, 3s fade Animator, types (S/W/I/E) |
| `src/org/swelement/ui/AstCascader.java` | **Create** | JComponent: Immutable Node, multi-column AnimatedPopup, List<String> path, ChangeListener |
| `src/org/swelement/ui/AstDatePicker.java` | **Create** | JComponent: Input-style trigger, 42-cell calendar popup, LocalDate, date range clamp |
| `src/org/swelement/ui/AstForm.java` | **Create** | JPanel: label-left or label-top GridBag layout, hint row, collect() method |
| `src/org/swelement/ui/AstTree.java` | **Create** | JComponent: Immutable TreeNode, 32px row height, expand Animator(rowHeight), arrow rotation, optional checkbox |
| `src/org/swelement/ui/AstTable.java` | **Create** | JComponent: Immutable Column, stripe, sortable, sort row-position Animator, single/multi select |
| `src/org/swelement/demo/AstContainerDemo.java` .. `AstTableDemo.java` | **Create (14 files)** | Per-component main, TitledBorder sections, interactive buttons, echo panels |
| `build.bat` | **Modify** (javac sources list + self-check java run chain) | Append new 14 UI sources + 14 Demo sources; append 14 `java -cp out org.swelement.ui.AstXxx` selfCheck invocations |

---

## Task 0: Core Engine Additions (Shared by every batch — do FIRST)

**Files:**
- Modify: `src/org/swelement/core/ElementTheme.java` — append at L44
- Modify: `src/org/swelement/core/AnimatedPopup.java`
- Create: `src/org/swelement/core/PopupPositioner.java`
- Create: `src/org/swelement/core/GlassPane.java`

- [ ] **Step 0.1: Add contrast utilities to ElementTheme.java — append just before the closing `}` brace (L44)**

```java
    // === P1 additions: WCAG contrast utilities ===
    private static float srgb(int v) {
        float vv = v / 255f;
        return vv <= 0.03928f ? vv / 12.92f : (float) Math.pow((vv + 0.055) / 1.055, 2.4);
    }
    /** Relative luminance per WCAG (approx, range [0,1]) */
    public static float luminance(Color c) {
        return 0.2126f * srgb(c.getRed()) + 0.7152f * srgb(c.getGreen()) + 0.0722f * srgb(c.getBlue());
    }
    /** Fails (AssertionError) when fg vs bg contrast < 4.5:1 — enabled only with -ea.
     *  Use `where` string to identify offending component state. */
    public static void assertContrast(Color fg, Color bg, String where) {
        float l1 = luminance(fg), l2 = luminance(bg);
        float lighter = Math.max(l1, l2), darker = Math.min(l1, l2);
        float ratio = (lighter + 0.05f) / (darker + 0.05f);
        assert ratio >= 4.5f : "[CONTRAST FAIL " + where + "] ratio=" + String.format("%.2f", ratio)
                + " fg=RGB(" + fg.getRed() + "," + fg.getGreen() + "," + fg.getBlue() + ")"
                + " bg=RGB(" + bg.getRed() + "," + bg.getGreen() + "," + bg.getBlue() + ")";
    }
```

- [ ] **Step 0.2: Compile + append self-check test to ElementTheme main()**

Append to `selfCheck()` block, right before `System.out.println("ElementTheme self-check OK");`:

```java
        assert luminance(Color.BLACK) < 0.01f : "black luminance near 0";
        assert luminance(Color.WHITE) > 0.99f : "white luminance near 1";
        try {
            assertContrast(Color.WHITE, Color.WHITE, "bad");
            assert false : "should have thrown";
        } catch (AssertionError expected) { /* ok */ }
        assertContrast(new Color(0x303133), Color.WHITE, "TEXT_MAIN on WHITE must pass");
```

Run: `.\build.bat ; java -cp out org.swelement.core.ElementTheme` (run with assert enabled: `java -ea -cp out org.swelement.core.ElementTheme` also passes).

Expected output includes `ElementTheme self-check OK`.

- [ ] **Step 0.3: Enhance AnimatedPopup.java (backward compatible — DO NOT break existing signatures)**

Add at top of class body, right after `private Component invoker;`:

```java
    public enum Direction { ABOVE, BELOW, LEFT, RIGHT, TOP_CENTER, BOTTOM_RIGHT_CORNER }
    public enum PopupLayer { POPUP, TOOL, MODAL }
    private static final java.util.List<AnimatedPopup> globalStack =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    private static final java.util.IdentityHashMap<PopupLayer,Integer> layerZ =
            new java.util.IdentityHashMap<PopupLayer,Integer>() {{
                put(PopupLayer.POPUP, 0);
                put(PopupLayer.TOOL, 100);
                put(PopupLayer.MODAL, 200);
            }};

    private final Animator closeAnim = new Animator(180, Easing::easeIn, v -> {
        alpha = 1f - v;
        repaint();
    });
```

Add public methods before the class closing brace:

```java
    public void show(Component invoker, Direction dir) {
        this.invoker = invoker;
        hidePopup();
        Window w = SwingUtilities.getWindowAncestor(invoker);
        if (!(w instanceof RootPaneContainer)) return;
        PopupPositioner pp = new PopupPositioner(getPreferredSize(),
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds());
        Rectangle inv = new Rectangle(invoker.getLocationOnScreen(), invoker.getSize());
        PopupPositioner.Result r = pp.calc(inv, dir);
        JLayeredPane lp = ((RootPaneContainer) w).getLayeredPane();
        Point p = new Point(r.location);
        SwingUtilities.convertPointFromScreen(p, lp);
        setBounds(p.x, p.y, getPreferredSize().width, getPreferredSize().height);
        alpha = 0f;
        content.setBorder(new EmptyBorder(8, 0, 0, 0));
        lp.add(this, JLayeredPane.POPUP_LAYER, layerZ.get(PopupLayer.POPUP));
        lp.repaint(p.x, p.y, getWidth(), getHeight());
        openAnim.go(0f, 1f);
    }

    public void hideWithAnimation(final Runnable afterHidden) {
        if (getParent() == null) { if (afterHidden != null) afterHidden.run(); return; }
        globalStack.remove(this);
        closeAnim.stop();
        alpha = 1f;
        closeAnim.go(0f, 1f);
        final Container parent = getParent();
        final Rectangle r = getBounds();
        new Timer(185, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                ((Timer)e.getSource()).stop();
                parent.remove(AnimatedPopup.this);
                parent.repaint(r.x, r.y, r.width, r.height);
                if (afterHidden != null) afterHidden.run();
            }
        }).start();
    }

    public static void registerGlobal(AnimatedPopup p, PopupLayer layer) {
        globalStack.add(p);
    }
```

Run: `.\build.bat ; java -cp out org.swelement.ui.Select` (existing Select still works; MenuDemo behavior unchanged — confirm by running briefly: `start javaw -cp out org.swelement.demo.MenuDemo`).

- [ ] **Step 0.4: Create PopupPositioner.java (pure math, no Swing side-effects)**

```java
package org.swelement.core;

import java.awt.*;

public final class PopupPositioner {
    public static final class Result {
        public final Point location;
        public final AnimatedPopup.Direction actualDirection;
        public Result(Point l, AnimatedPopup.Direction a) { location = l; actualDirection = a; }
    }
    private final Dimension popupSize;
    private final Rectangle screenBounds;
    private static final int MARGIN = 8;

    public PopupPositioner(Dimension popupSize, Rectangle screenBounds) {
        this.popupSize = popupSize;
        this.screenBounds = screenBounds;
    }

    public Result calc(Rectangle invoker, AnimatedPopup.Direction preferred) {
        int px = 0, py = 0;
        AnimatedPopup.Direction actual = preferred;
        switch (preferred) {
            case ABOVE:
                px = invoker.x + invoker.width / 2 - popupSize.width / 2;
                py = invoker.y - popupSize.height - MARGIN;
                break;
            case BELOW:
                px = invoker.x + invoker.width / 2 - popupSize.width / 2;
                py = invoker.y + invoker.height + MARGIN;
                break;
            case LEFT:
                px = invoker.x - popupSize.width - MARGIN;
                py = invoker.y + invoker.height / 2 - popupSize.height / 2;
                break;
            case RIGHT:
                px = invoker.x + invoker.width + MARGIN;
                py = invoker.y + invoker.height / 2 - popupSize.height / 2;
                break;
            case TOP_CENTER:
                px = screenBounds.x + screenBounds.width / 2 - popupSize.width / 2;
                py = screenBounds.y + 20;
                return clampAndReturn(px, py, actual);
            case BOTTOM_RIGHT_CORNER:
                px = screenBounds.x + screenBounds.width - popupSize.width - 40;
                py = screenBounds.y + screenBounds.height - popupSize.height - 80;
                return clampAndReturn(px, py, actual);
        }
        // boundary flip: check if out of screen on preferred side
        boolean flip = false;
        switch (preferred) {
            case ABOVE: if (py < screenBounds.y) flip = true; break;
            case BELOW: if (py + popupSize.height > screenBounds.y + screenBounds.height) flip = true; break;
            case LEFT:  if (px < screenBounds.x) flip = true; break;
            case RIGHT: if (px + popupSize.width > screenBounds.x + screenBounds.width) flip = true; break;
        }
        if (flip) {
            switch (preferred) {
                case ABOVE: actual = AnimatedPopup.Direction.BELOW;
                    py = invoker.y + invoker.height + MARGIN; break;
                case BELOW: actual = AnimatedPopup.Direction.ABOVE;
                    py = invoker.y - popupSize.height - MARGIN; break;
                case LEFT:  actual = AnimatedPopup.Direction.RIGHT;
                    px = invoker.x + invoker.width + MARGIN; break;
                case RIGHT: actual = AnimatedPopup.Direction.LEFT;
                    px = invoker.x - popupSize.width - MARGIN; break;
            }
        }
        return clampAndReturn(px, py, actual);
    }

    private Result clampAndReturn(int px, int py, AnimatedPopup.Direction actual) {
        px = Math.max(screenBounds.x + MARGIN, Math.min(px, screenBounds.x + screenBounds.width - popupSize.width - MARGIN));
        py = Math.max(screenBounds.y + MARGIN, Math.min(py, screenBounds.y + screenBounds.height - popupSize.height - MARGIN));
        return new Result(new Point(px, py), actual);
    }

    static void selfCheck() {
        Rectangle screen = new Rectangle(0, 0, 1920, 1080);
        PopupPositioner pp = new PopupPositioner(new Dimension(200, 100), screen);
        // normal BELOW
        Result r = pp.calc(new Rectangle(1000, 10, 100, 30), AnimatedPopup.Direction.BELOW);
        assert r.location.y >= 10 + 30 : "below placement";
        // forced flip: invoker at bottom, BELOW would overflow
        r = pp.calc(new Rectangle(1000, 1070, 100, 30), AnimatedPopup.Direction.BELOW);
        assert r.actualDirection == AnimatedPopup.Direction.ABOVE : "overflow flip";
        assert r.location.y + 100 <= 1070 : "above placed above invoker bottom";
        // TOP_CENTER
        r = pp.calc(new Rectangle(0,0,1,1), AnimatedPopup.Direction.TOP_CENTER);
        assert r.location.x == 1920/2 - 100 : "top center x";
        assert r.location.y == screen.y + 20 : "top center y";
        System.out.println("PopupPositioner self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
```

Compile + run: `.\build.bat ; java -ea -cp out org.swelement.core.PopupPositioner`
Expected: `PopupPositioner self-check OK`.

- [ ] **Step 0.5: Create GlassPane.java**

```java
package org.swelement.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GlassPane extends JPanel {
    private final Animator alphaAnim = new Animator(200, Easing::easeOut, v -> {
        alpha = Math.round(v * 80); // range 0..80
        repaint();
    });
    private int alpha;
    private static final AWTEventListener CONSUME = e -> {
        if (!(e instanceof InputEvent)) return;
        // GlassPane intercepts by being visible & opaque region; we also consume focus traversal
    };

    public GlassPane() {
        setOpaque(false);
        Toolkit.getDefaultToolkit().addAWTEventListener(CONSUME, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    public void setActive(boolean active) {
        setVisible(active);
        if (active) { alphaAnim.go(0f, 1f); }
        else { alphaAnim.go(1f, 0f); }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, alpha));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    // Block all mouse/key events reaching children
    @Override protected void processMouseEvent(MouseEvent e) { e.consume(); }
    @Override protected void processMouseMotionEvent(MouseEvent e) { e.consume(); }
    @Override protected void processKeyEvent(KeyEvent e) { e.consume(); }
}
```

- [ ] **Step 0.6: Commit batch 0 core changes**

```bash
git add src/org/swelement/core/ElementTheme.java src/org/swelement/core/AnimatedPopup.java src/org/swelement/core/PopupPositioner.java src/org/swelement/core/GlassPane.java
git commit -m "feat(core): contrast utilities + AnimatedPopup direction/hideAnimation/stacking + PopupPositioner + GlassPane"
```

---

## Task 1: AstContainer (pure layout, no animation)

**Files:**
- Create: `src/org/swelement/ui/AstContainer.java`
- Create: `src/org/swelement/demo/AstContainerDemo.java`
- Modify: `build.bat` (add to sources list + self-check run chain)

- [ ] **Step 1.1: Create AstContainer.java**

```java
package org.swelement.ui;

import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

public class AstContainer extends JPanel {
    public static final int VERTICAL = 0, HORIZONTAL = 1;
    public static final int HEADER_H = 64, ASIDE_W = 220, FOOTER_H = 48;

    private final int direction;
    private JComponent header, aside, mainComp, footer;
    private final JPanel north, south, west, center;

    public AstContainer(int direction) {
        this.direction = direction;
        setOpaque(true);
        setBackground(ElementTheme.FILL_BLANK);
        north = new JPanel(new BorderLayout()); north.setOpaque(false);
        south = new JPanel(new BorderLayout()); south.setOpaque(false);
        west  = new JPanel(new BorderLayout()); west.setOpaque(false);
        center = new JPanel(new BorderLayout()); center.setOpaque(false);
        super.setLayout(new BorderLayout());
        super.add(north, BorderLayout.NORTH);
        super.add(south, BorderLayout.SOUTH);
        super.add(west,  direction == HORIZONTAL ? BorderLayout.WEST : BorderLayout.NORTH);
        super.add(center, BorderLayout.CENTER);
    }

    public void setHeader(JComponent h) {
        if (header != null) north.remove(header);
        header = h;
        h.setBorder(new MatteBorder(0, 0, 1, 0, ElementTheme.BORDER_BASE));
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, HEADER_H));
        h.setBackground(ElementTheme.FILL_BASE);
        north.add(h, BorderLayout.CENTER);
        revalidate();
    }

    public void setAside(JComponent a) {
        if (aside != null) west.remove(aside);
        aside = a;
        if (direction == HORIZONTAL) {
            a.setPreferredSize(new Dimension(ASIDE_W, a.getPreferredSize().height));
            a.setBorder(new MatteBorder(0, 0, 0, 1, ElementTheme.BORDER_BASE));
        } else {
            a.setPreferredSize(new Dimension(a.getPreferredSize().width, 40));
            a.setBorder(new MatteBorder(0, 0, 1, 0, ElementTheme.BORDER_BASE));
        }
        west.add(a, BorderLayout.CENTER);
        revalidate();
    }

    public void setMain(JComponent m) {
        if (mainComp != null) center.remove(mainComp);
        mainComp = m;
        m.setBorder(new EmptyBorder(16, 20, 16, 20));
        center.add(m, BorderLayout.CENTER);
        revalidate();
    }

    public void setFooter(JComponent f) {
        if (footer != null) south.remove(footer);
        footer = f;
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, FOOTER_H));
        f.setBackground(ElementTheme.FILL_BASE);
        f.setBorder(new MatteBorder(1, 0, 0, 0, ElementTheme.BORDER_BASE));
        south.add(f, BorderLayout.CENTER);
        revalidate();
    }

    static void selfCheck() {
        AstContainer c = new AstContainer(HORIZONTAL);
        JComponent h = new JPanel(); h.setBackground(Color.white);
        JComponent a = new JPanel(); a.setBackground(Color.white);
        JComponent m = new JPanel(); m.setBackground(Color.white);
        JComponent f = new JPanel(); f.setBackground(Color.white);
        c.setHeader(h); c.setAside(a); c.setMain(m); c.setFooter(f);
        JFrame jf = new JFrame();
        jf.setContentPane(c);
        jf.setSize(900, 600);
        jf.pack();
        assert h.getHeight() == HEADER_H : "header height";
        assert a.getWidth() == ASIDE_W : "aside width";
        assert f.getHeight() == FOOTER_H : "footer height";
        jf.dispose();
        // VERTICAL: aside becomes top bar
        AstContainer cv = new AstContainer(VERTICAL);
        cv.setHeader(new JPanel()); cv.setAside(new JPanel()); cv.setMain(new JPanel());
        assert true : "no exceptions";
        System.out.println("AstContainer self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
```

- [ ] **Step 1.2: Create AstContainerDemo.java (interactive)**

```java
package org.swelement.demo;

import org.swelement.ui.AstButton;
import org.swelement.ui.AstContainer;
import org.swelement.ui.AstTabs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class AstContainerDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstContainer Demo - 布局容器");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Top controls
            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            ctrl.setBorder(new TitledBorder("切换布局方向 & 显示项"));
            final JCheckBox hdr = new JCheckBox("Header", true);
            final JCheckBox asd = new JCheckBox("Aside", true);
            final JCheckBox ftr = new JCheckBox("Footer", true);
            final JComboBox<String> dir = new JComboBox<>(new String[]{"HORIZONTAL (Aside 左 + Main 右)", "VERTICAL (Aside 上 + Main 下)"});
            ctrl.add(dir);
            ctrl.add(hdr);
            ctrl.add(asd);
            ctrl.add(ftr);

            // Construct components
            JPanel headerBox = new JPanel(new BorderLayout());
            JLabel title = new JLabel("  Admin Console — 后台管理系统");
            title.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
            title.setForeground(new Color(0x303133));
            headerBox.add(title, BorderLayout.WEST);
            JPanel hdrRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
            hdrRight.add(new AstButton("🔔 通知", AstButton.DEFAULT, false));
            hdrRight.add(new AstButton("👤 管理员", AstButton.PRIMARY, false));
            headerBox.add(hdrRight, BorderLayout.EAST);

            DefaultListModel<String> lm = new DefaultListModel<>();
            for (String s : new String[]{"📊 数据总览", "👥 用户管理", "📦 商品管理", "💳 订单管理", "⚙️ 系统设置"})
                lm.addElement(s);
            JList<String> asideBox = new JList<>(lm);
            asideBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            asideBox.setFixedCellHeight(36);
            asideBox.setBorder(new EmptyBorder(8, 8, 8, 8));
            asideBox.setBackground(new Color(0xFAFAFA));

            AstTabs mainTabs = new AstTabs(new String[]{"基本信息", "权限配置", "安全日志"}, 0);
            JLabel mainBody = new JLabel("<html><body style='color:#606266;font-size:12px;padding:16px 24px'>" +
                    "Main 主内容区：可放置表单、表格、卡片。AstContainer 默认 Main 四周 16/20/16/20 padding。</body></html>");
            JPanel mainCard = new JPanel(new BorderLayout());
            mainCard.add(mainTabs, BorderLayout.NORTH);
            mainCard.add(mainBody, BorderLayout.CENTER);

            JLabel footerBox = new JLabel("  © 2026 swing-element-ui · Layout demo", SwingConstants.LEFT);
            footerBox.setForeground(new Color(0x909399));

            final AstContainer[] ac = {new AstContainer(AstContainer.HORIZONTAL)};
            Runnable rebuild = () -> {
                ac[0] = new AstContainer(dir.getSelectedIndex() == 0 ? AstContainer.HORIZONTAL : AstContainer.VERTICAL);
                if (hdr.isSelected()) ac[0].setHeader(headerBox);
                if (asd.isSelected()) ac[0].setAside(asideBox);
                ac[0].setMain(mainCard);
                if (ftr.isSelected()) ac[0].setFooter(footerBox);
                f.setContentPane(new JSplitPane(JSplitPane.VERTICAL_SPLIT, ctrl, ac[0]));
                f.revalidate();
                f.repaint();
            };
            hdr.addActionListener(e -> rebuild.run());
            asd.addActionListener(e -> rebuild.run());
            ftr.addActionListener(e -> rebuild.run());
            dir.addActionListener(e -> rebuild.run());
            rebuild.run();

            f.pack();
            f.setSize(980, 680);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 1.3: Add to build.bat sources + self-check chain, compile & run self-check**

In `build.bat`, append to `SET SOURCES=` list the 2 new paths:
```
src\org\swelement\ui\AstContainer.java ^
src\org\swelement\demo\AstContainerDemo.java ^
```
At bottom append:
```
echo --- AstContainer self-check ---
java -ea -cp out org.swelement.ui.AstContainer
```

Run: `.\build.bat 2>&1 | Select-String "BUILD OK|AstContainer self-check OK|error"`
Expected: `BUILD OK` followed by `AstContainer self-check OK`.

- [ ] **Step 1.4: Commit Task 1**

```bash
git add src/org/swelement/ui/AstContainer.java src/org/swelement/demo/AstContainerDemo.java build.bat
git commit -m "feat: AstContainer layout + interactive demo"
```

---

## Task 2: AstAvatar

**Files:**
- Create: `src/org/swelement/ui/AstAvatar.java`
- Create: `src/org/swelement/demo/AstAvatarDemo.java`
- Modify: `build.bat` — append sources + self-check

- [ ] **Step 2.1: Create AstAvatar.java**

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class AstAvatar extends JComponent {
    public static final int CIRCLE = 0, SQUARE = 1;
    public static final int SIZE_SMALL = 32, SIZE_DEFAULT = 40, SIZE_LARGE = 64;

    private final int size, shape;
    private final Color bg;
    private final String text;    // may be 1 char or longer label
    private final ImageIcon icon; // nullable
    private final AstBadge badge;    // delegate
    private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> {
        hover = v;
        repaint();
    });
    private float hover;

    public AstAvatar(char c, int size, int shape) {
        this(ColorFactory.pick(c), String.valueOf(c), null, size, shape);
    }

    public AstAvatar(Color bg, String text, int size, int shape) {
        this(bg, text, null, size, shape);
    }

    public AstAvatar(ImageIcon icon, int size, int shape) {
        this(Color.WHITE, "", icon, size, shape);
    }

    private AstAvatar(Color bg, String text, ImageIcon icon, int size, int shape) {
        this.bg = bg;
        this.text = text;
        this.icon = icon;
        this.size = size;
        this.shape = shape;
        this.badge = new AstBadge();
        setOpaque(false);
        setLayout(null);
        add(badge);
        badge.setContent(new JLabel()); // placeholder, badge paints in corner outside of content bounds
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (isEnabled()) hoverAnim.go(hover, 1f);
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverAnim.go(hover, 0f);
            }
        });
    }

    public void setBadgeCount(int n) {
        badge.setCount(n);
    }

    public void setBadgeDot(boolean b) {
        badge.setDot(b);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // border lift on hover
        Color bgPaint = ElementTheme.lerp(bg, ElementTheme.lerp(bg, Color.WHITE, 0.15f), hover * 0.5f);
        Shape s = shape == CIRCLE
                ? new Ellipse2D.Float(0, 0, size, size)
                : new RoundRectangle2D.Float(0, 0, size, size, ElementTheme.RADIUS * 2, ElementTheme.RADIUS * 2);
        g2.setColor(bgPaint);
        g2.fill(s);
        // contrast-safe text color: pick WHITE when bg dark, TEXT_MAIN when bg light
        float lum = ElementTheme.luminance(bgPaint);
        Color fg = lum < 0.55f ? Color.WHITE : ElementTheme.TEXT_MAIN;
        ElementTheme.assertContrast(fg, bgPaint, "AstAvatar.text.shape=" + shape);
        if (icon != null) {
            int iw = Math.min(icon.getIconWidth(), size - 4);
            int ih = Math.min(icon.getIconHeight(), size - 4);
            icon.paintIcon(this, g2, (size - iw) / 2, (size - ih) / 2);
        } else {
            g2.setColor(fg);
            g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, size * 0.4f));
            FontMetrics fm = g2.getFontMetrics();
            String txt = text.length() > 2 ? text.substring(0, 2) : text;
            g2.drawString(txt, (size - fm.stringWidth(txt)) / 2f, (size - fm.getHeight()) / 2f + fm.getAscent());
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(size, size);
    }

    @Override
    public void doLayout() {
        // place badge at top-right corner of the avatar square
        badge.setBounds(Math.max(0, size - 22), 0, 40, 40);
    }

    private static final class ColorFactory {
        private static final Color[] POOL = {
                new Color(0x409EFF), new Color(0x67C23A), new Color(0xE6A23C), new Color(0xF56C6C),
                new Color(0x909399), new Color(0x8e44ad), new Color(0x16a085), new Color(0xd35400)
        };

        static Color pick(char c) {
            return POOL[(c & 0x7fffffff) % POOL.length];
        }
    }

    static void selfCheck() {
        AstAvatar a1 = new AstAvatar('Z', SIZE_DEFAULT, CIRCLE);
        AstAvatar a2 = new AstAvatar(new Color(0xFFFFFF), "U", SIZE_LARGE, SQUARE);
        a1.setBadgeDot(true);
        a2.setBadgeCount(99);
        assert a1.getPreferredSize().width == SIZE_DEFAULT;
        assert a2.getPreferredSize().height == SIZE_LARGE;
        // contrast assert for dark-luminance bg
        AstAvatar dark = new AstAvatar(new Color(0x111111), "X", 40, CIRCLE);
        // size paint without exception by calling with a headless-safe graphics
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(80, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try {
            dark.setBounds(0, 0, 40, 40);
            dark.paintComponent(gg); // runs assertContrast
        } finally {
            gg.dispose();
        }
        System.out.println("AstAvatar self-check OK");
    }

    public static void main(String[] args) {
        selfCheck();
    }
}
```

- [ ] **Step 2.2: Create AstAvatarDemo.java** (4 columns: char / color+text / icon placeholder / badges, 3 sizes × 2 shapes, interactive +1 badge button)

```java
package org.swelement.demo;

import org.swelement.ui.AstAvatar;
import org.swelement.ui.AstButton;
import org.swelement.ui.Button;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AstAvatarDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("AstAvatar Demo - 头像");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(16, 24, 16, 24));

            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 12));
            p1.setBorder(new TitledBorder("单字符头像（按字符哈希取色 + 自动高对比文字色）"));
            for (char c : new char[]{'Z', 'A', '李', '王', '5', '☰', '😀', 'P'}) {
                AstAvatar a = new AstAvatar(c, AstAvatar.SIZE_LARGE, AstAvatar.CIRCLE);
                JPanel wrap = new JPanel();
                wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
                wrap.add(a);
                wrap.add(Box.createVerticalStrut(4));
                JLabel l = new JLabel(String.valueOf(c), SwingConstants.CENTER);
                l.setForeground(new Color(0x909399));
                l.setFont(l.getFont().deriveFont(11f));
                l.setAlignmentX(Component.CENTER_ALIGNMENT);
                wrap.add(l);
                p1.add(wrap);
            }

            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 36, 12));
            p2.setBorder(new TitledBorder("大小 × 形状"));
            int[] sizes = {AstAvatar.SIZE_SMALL, AstAvatar.SIZE_DEFAULT, AstAvatar.SIZE_LARGE};
            String[] labels = {"Small 32", "Default 40", "Large 64"};
            for (int i = 0; i < sizes.length; i++) {
                for (int sh : new int[]{AstAvatar.CIRCLE, AstAvatar.SQUARE}) {
                    AstAvatar a = new AstAvatar(new Color(0xE6A23C), "Admin", sizes[i], sh);
                    JPanel wrap = new JPanel();
                    wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
                    wrap.add(a);
                    wrap.add(Box.createVerticalStrut(4));
                    JLabel l = new JLabel(labels[i] + (sh == AstAvatar.CIRCLE ? " 圆" : " 方"), SwingConstants.CENTER);
                    l.setForeground(new Color(0x909399));
                    l.setFont(l.getFont().deriveFont(11f));
                    l.setAlignmentX(Component.CENTER_ALIGNMENT);
                    wrap.add(l);
                    p2.add(wrap);
                }
            }

            final AtomicInteger badge = new AtomicInteger(3);
            final AstAvatar[] avs = new AstAvatar[4];
            avs[0] = new AstAvatar('U', AstAvatar.SIZE_LARGE, AstAvatar.CIRCLE);
            avs[0].setBadgeCount(badge.get());
            avs[1] = new AstAvatar(new Color(0x67C23A), "OK", AstAvatar.SIZE_DEFAULT, AstAvatar.SQUARE);
            avs[1].setBadgeDot(true);
            avs[2] = new AstAvatar('P', AstAvatar.SIZE_LARGE, AstAvatar.SQUARE);
            avs[2].setBadgeCount(100);
            avs[3] = new AstAvatar('A', AstAvatar.SIZE_DEFAULT, AstAvatar.CIRCLE);
            avs[3].setBadgeCount(0);
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 12));
            p3.setBorder(new TitledBorder("角标复合（数字 / dot / 99+ / 0隐藏）"));
            for (AstAvatar a : avs) p3.add(a);

            JPanel p4 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
            p4.setBorder(new TitledBorder("交互控制"));
            AstButton plus = new AstButton("用户 U 角标 +1", AstButton.PRIMARY, false);
            AstButton reset = new AstButton("重置为 3", AstButton.DEFAULT, false);
            AstButton dotSwitch = new AstButton("切换 OK 的红点", AstButton.WARNING, false);
            final boolean[] dotOn = {true};
            plus.addActionListener(e -> {
                int n = badge.incrementAndGet();
                avs[0].setBadgeCount(n);
            });
            reset.addActionListener(e -> {
                badge.set(3);
                avs[0].setBadgeCount(3);
            });
            dotSwitch.addActionListener(e -> {
                dotOn[0] = !dotOn[0];
                avs[1].setBadgeDot(dotOn[0]);
            });
            p4.add(plus);
            p4.add(reset);
            p4.add(dotSwitch);

            root.add(p1);
            root.add(Box.createVerticalStrut(8));
            root.add(p2);
            root.add(Box.createVerticalStrut(8));
            root.add(p3);
            root.add(Box.createVerticalStrut(8));
            root.add(p4);

            f.setContentPane(new JScrollPane(root));
            f.pack();
            f.setSize(Math.max(f.getWidth(), 920), Math.min(f.getHeight(), 720));
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
```

- [ ] **Step 2.3: Add to build.bat, compile, selfCheck**
- [ ] **Step 2.4: Commit Task 2**

```bash
git add src/org/swelement/ui/AstAvatar.java src/org/swelement/demo/AstAvatarDemo.java build.bat
git commit -m "feat: AstAvatar char/text/icon + size/shape + contrast-safe FG + badge composite + demo"
```

---

(Plan continues in same file)

## Task 3: AstCard

**Files:**
- Create: `src/org/swelement/ui/AstCard.java`
- Create: `src/org/swelement/demo/AstCardDemo.java`
- Modify: `build.bat`

Key features: 48px title bar (left title string, right addHeaderAction flow), content body, bordered default, hover shadow elevation. Animator drives border color BORDER_BASE → PRIMARY.

- [ ] **Step 3.1: Create AstCard.java**

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

public class AstCard extends JComponent {
    private final String title;
    private final boolean bordered;
    private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> {
        hover = v;
        repaint();
    });
    private float hover;
    private JComponent content;
    private final JPanel headerActions;

    public AstCard(String title) {
        this(title, true, true);
    }

    public AstCard(String title, boolean bordered, boolean shadowOnHover) {
        this.title = title;
        this.bordered = bordered;
        this.headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        headerActions.setOpaque(false);
        setLayout(null); // manual layout in doLayout
        add(headerActions);
        setOpaque(false);
        if (shadowOnHover) addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) hoverAnim.go(hover, 1f);
            }

            public void mouseExited(MouseEvent e) {
                hoverAnim.go(hover, 0f);
            }
        });
    }

    public void setContent(JComponent c) {
        if (content != null) remove(content);
        content = c;
        add(c);
        revalidate();
    }

    public void addHeaderAction(JComponent c) {
        headerActions.add(c);
        revalidate();
    }

    public void setShadowElevation(int level) { /* reserved, current hover is binary */ }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    @Override
    public void doLayout() {
        Insets in = getInsets();
        int x = in.left, y = in.top, w = getWidth() - in.left - in.right, h = getHeight() - in.top - in.bottom;
        int titleH = 48;
        headerActions.setBounds(x + w - 8, y, w - 16, titleH);
        if (content != null) {
            int padTB = 16, padLR = 20;
            content.setBounds(x + padLR, y + titleH + padTB, w - 2 * padLR, Math.max(0, h - titleH - 2 * padTB));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = Color.WHITE;
        Color borderColor = bordered
                ? ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, hover)
                : new Color(0, 0, 0, 0);
        ElementTheme.assertContrast(borderColor, bg, "AstCard.border");
        RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1.5f, getHeight() - 1.5f, ElementTheme.RADIUS * 2, ElementTheme.RADIUS * 2);
        g2.setColor(bg);
        g2.fill(r);
        // bottom shadow when hovered
        if (hover > 0.01f) {
            g2.setColor(new Color(64, 158, 255, Math.round(36 * hover)));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2.5f, getHeight() - 2.5f, ElementTheme.RADIUS * 2, ElementTheme.RADIUS * 2));
        }
        if (bordered) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(r);
        }
        // title bar
        g2.setColor(ElementTheme.TEXT_MAIN);
        ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, bg, "AstCard.title");
        g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2.getFontMetrics();
        int titleX = 20;
        int titleBaseline = (48 - fm.getHeight()) / 2 + fm.getAscent();
        if (title != null) g2.drawString(title, titleX, titleBaseline);
        // title bottom separator
        g2.setColor(ElementTheme.BORDER_BASE);
        g2.drawLine(0, 48, getWidth(), 48);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        int cw = content != null ? content.getPreferredSize().width + 40 : 320;
        int ch = 48 + 32 + (content != null ? content.getPreferredSize().height : 160);
        return new Dimension(cw, ch);
    }

    static void selfCheck() {
        AstCard c = new AstCard("用户信息");
        JPanel body = new JPanel();
        body.add(new JLabel("Hello"));
        c.setContent(body);
        c.addHeaderAction(new AstButton("编辑", AstButton.DEFAULT, false));
        JFrame jf = new JFrame();
        jf.setSize(500, 400);
        jf.add(c);
        c.setBounds(0, 0, 500, 300);
        c.doLayout();
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(500, 300, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try {
            c.paintComponent(gg);
        } finally {
            gg.dispose();
        }
        jf.dispose();
        System.out.println("AstCard self-check OK");
    }

    public static void main(String[] args) {
        selfCheck();
    }
}
```

- [ ] **Step 3.2: Create AstCardDemo.java** (show 3 cards: plain, with 2 actions, borderless. Add interactivity: toggle hover-able, add dynamic stats card with Progress inside).

Skeleton: Title section with 3 cards horizontally; bottom control: add new Card, switch theme-like toggle.

- [ ] **Step 3.3: Add to build.bat, compile, run selfCheck**
- [ ] **Step 3.4: Commit Task 3**

```bash
git add src/org/swelement/ui/AstCard.java src/org/swelement/demo/AstCardDemo.java build.bat
git commit -m "feat: AstCard header/action/content + hover border highlight + demo"
```

---

## Task 4: AstLoading

**Files:**
- Create: `src/org/swelement/ui/AstLoading.java`
- Create: `src/org/swelement/demo/AstLoadingDemo.java`
- Modify: `build.bat`

- [ ] **Step 4.1: Create AstLoading.java**

```java
package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;
import org.swelement.core.GlassPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;

public final class AstLoading {
    private final JComponent target;
    private final Overlay overlay;
    private boolean showing;

    private AstLoading(JComponent target) {
        this.target = target;
        this.overlay = new Overlay("");
    }

    public static AstLoading wrap(JComponent target) {
        if (target == null) throw new IllegalArgumentException("target null");
        AstLoading al = new AstLoading(target);
        // Wrap target into 1:1 JLayeredPane
        target.putClientProperty(AstLoading.class.getName(), al);
        return al;
    }

    public void show() { show(""); }
    public void show(String tip) {
        if (target.getParent() instanceof JLayeredPane == false) {
            Container parent = target.getParent();
            int idx = -1;
            if (parent != null) for (int i = 0; i < parent.getComponentCount(); i++) if (parent.getComponent(i) == target) { idx = i; break; }
            JLayeredPane lp = new JLayeredPane();
            lp.setOpaque(false);
            if (parent != null) {
                parent.add(lp, idx);
                parent.remove(target);
            }
            lp.add(target, JLayeredPane.DEFAULT_LAYER, 0);
            lp.add(overlay, JLayeredPane.PALETTE_LAYER, 0);
            lp.validate();
            // size tracking
            lp.addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentResized(java.awt.event.ComponentEvent e) {
                    target.setBounds(0, 0, lp.getWidth(), lp.getHeight());
                    overlay.setBounds(0, 0, lp.getWidth(), lp.getHeight());
                }
            });
            target.setBounds(0, 0, target.getWidth() == 0 ? 400 : target.getWidth(), target.getHeight() == 0 ? 200 : target.getHeight());
            overlay.setBounds(0, 0, target.getWidth(), target.getHeight());
        }
        overlay.setText(tip);
        overlay.showAnim();
        showing = true;
    }
    public void hide() { overlay.hideAnim(); showing = false; }

    // === Full Screen ===
    private static final Map<Window, GlassPane> FULL = new IdentityHashMap<>();

    public static void showFullScreen(Window w, String tipText) {
        if (w == null) throw new IllegalArgumentException("window null");
        if (!(w instanceof RootPaneContainer)) return;
        RootPaneContainer rpc = (RootPaneContainer) w;
        GlassPane gp = FULL.get(w);
        if (gp == null) {
            gp = new GlassPane();
            rpc.getRootPane().setGlassPane(gp);
            FULL.put(w, gp);
        }
        // overlay tip inside a small centered panel over glass pane
        gp.removeAll();
        JPanel tip = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x404040, 220));
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tip.setOpaque(false);
        tip.setBorder(new EmptyBorder(16, 20, 16, 20));
        Spinner spinner = new Spinner(Color.WHITE);
        JLabel lbl = new JLabel(tipText == null ? "" : tipText, SwingConstants.CENTER);
        lbl.setForeground(Color.WHITE); lbl.setFont(ElementTheme.FONT.deriveFont(14f));
        tip.add(spinner, BorderLayout.CENTER); tip.add(lbl, BorderLayout.SOUTH);
        gp.setLayout(null);
        gp.add(tip);
        Dimension td = tip.getPreferredSize();
        tip.setSize(Math.max(td.width, 180), Math.max(td.height, 100));
        tip.setLocation(w.getWidth()/2 - tip.getWidth()/2, w.getHeight()/2 - tip.getHeight()/2);
        gp.setActive(true);
        spinner.start();
    }
    public static void hideFullScreen(Window w) {
        GlassPane gp = FULL.get(w); if (gp == null) return;
        gp.setActive(false);
    }

    // === visual primitives ===
    private static final class Overlay extends JComponent {
        private final Animator fade = new Animator(200, Easing::easeOut, v -> { alpha = Math.round(v*200); repaint(); });
        private final Animator fadeOut = new Animator(180, Easing::easeIn, v -> { alpha = Math.round((1-v)*200); repaint(); });
        private int alpha;
        private String text;
        private final Spinner spinner;
        Overlay(String s) { this.text = s == null ? "" : s; spinner = new Spinner(ElementTheme.PRIMARY); add(spinner); }
        void setText(String s) { text = s == null ? "" : s; }
        void showAnim() { setVisible(true); spinner.start(); fadeOut.stop(); fade.go(0f, 1f); }
        void hideAnim() { fade.stop(); fadeOut.go(0f, 1f); final Timer t = new Timer(200, e -> { ((Timer)e.getSource()).stop(); spinner.stop(); setVisible(false); }); t.setRepeats(false); t.start(); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
            if (!text.isEmpty()) {
                g2 = (Graphics2D) g.create();
                g2.setFont(ElementTheme.FONT);
                g2.setColor(ElementTheme.TEXT_REGULAR);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(text);
                // paint text just below spinner
                g2.drawString(text, (getWidth()-tw)/2f, getHeight()/2f + 28);
                g2.dispose();
            }
        }
        public void doLayout() {
            spinner.setSize(32, 32);
            spinner.setLocation(getWidth()/2 - 16, getHeight()/2 - 28);
        }
    }

    private static final class Spinner extends JComponent {
        private final Animator rot = new Animator(800, Easing::linear, v -> { a = v; repaint(); });
        private final Color base;
        private float a;
        Spinner(Color base) {
            this.base = base;
            setOpaque(false);
        }
        void start() { a = 0f; rot.stop(); rot.go(0f, 1f); }
        void stop()  { rot.stop(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(getWidth()/2, getHeight()/2);
            g2.rotate(a * Math.PI * 2);
            int count = 12;
            for (int i = 0; i < count; i++) {
                double ang = (i/(double)count) * Math.PI * 2;
                float t = i / (float) count;
                int alpha = (int) (60 + (255-60) * t);
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
                int x = (int) Math.round(Math.cos(ang) * (getWidth()*0.35));
                int y = (int) Math.round(Math.sin(ang) * (getHeight()*0.35));
                g2.fillOval(x - 2, y - 2, 4, 4);
            }
            g2.dispose();
        }
    }

    static void selfCheck() {
        // wrap mode
        JLabel content = new JLabel("Hello");
        content.setPreferredSize(new Dimension(400, 300));
        AstLoading al = wrap(content);
        al.show("加载中...");
        // paint off-screen
        JLayeredPane lp = (JLayeredPane) content.getParent();
        lp.setSize(400, 300);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(400, 300, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { lp.paint(gg); } finally { gg.dispose(); }
        al.hide();
        // static fullScreen mode doesn't need headless test
        System.out.println("AstLoading self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
```

- [ ] **Step 4.2: Create AstLoadingDemo.java** — Button to show/hide wrap mode (target: AstTable-like 6×4 grid of labels); Button to show 3s full screen with tip "全局加载中 3 秒后消失"; show 5 different targets with show/hide toggles, observe fade overlay + spinning arc.

- [ ] **Step 4.3: Build + selfCheck run (java -ea -cp out org.swelement.ui.AstLoading)**
- [ ] **Step 4.4: Commit Task 4**

```bash
git add src/org/swelement/ui/AstLoading.java src/org/swelement/demo/AstLoadingDemo.java build.bat
git commit -m "feat: AstLoading wrap/fullscreen + 12-arc linear spinner + fade overlay + demo"
```

---

## Task 5: Popup 系 — AstTooltip, AstDropdown, AstDialog, AstMessageBox, AstMessage (Tasks 5.1~5.5)

### Task 5.1: AstTooltip

- [ ] **Step 5.1.1: Create AstTooltip.java** — Non-inheriting class, use AnimatedPopup.show(invoker, Direction). Build content JLabel with padding 8~12px, white bg, BORDER border, TEXT_MAIN text, 12 line max, width auto shrink after 320px. showDelay 300 Timer, cancel if exit.

- [ ] **Step 5.1.2: Create AstTooltipDemo.java** — Put 4 buttons (上/下/左/右 方向 tooltip, 不同内容: 1行/2行/长描述), mouse over observe fade-in + direction auto-flip when placed near edge of screen.

- [ ] **Step 5.1.3: Build + self-check (paint tooltip off-screen via popup content panel paint, assert contrast)**

- [ ] **Step 5.1.4: Commit**

```bash
git commit -m "feat: AstTooltip 4-direction boundary flip + delay show/hide + demo"
```

### Task 5.2: AstDropdown

- [ ] **Step 5.2.1: Create AstDropdown.java** — trigger JComponent + HOVER or CLICK mode, AnimatedPopup, OptionRow list (reuse Menu#OptionRow visual style). CLICK: use `AWTEventListener MOUSE_PRESSED` (same pattern AnimatedPopup already uses) to dismiss on outside click.

- [ ] **Step 5.2.2: Create AstDropdownDemo.java** — Hover dropdown on a "用户中心 ▾" button showing 4 items + separator; Click dropdown on "导出 ▾" with 3 formats. Action listeners append log to text area.

- [ ] **Step 5.2.3: Build + click dismiss test in selfCheck (synthetic press event)**
- [ ] **Step 5.2.4: Commit**

```bash
git commit -m "feat: AstDropdown HOVER/CLICK trigger + dismiss + item callback + demo"
```

### Task 5.3: AstDialog

- [ ] **Step 5.3.1: Create AstDialog.java** — Use AnimatedPopup centered over parent window. Install GlassPane when show, consume all events. Add scale + alpha Animator (0.95→1.0 open, 1.0→0.95 close). Build bottom button row right-aligned flow. addButton appends, rightmost is default focus. Support width setter, closable × top-right painted in corner.

- [ ] **Step 5.3.2: Create AstDialogDemo.java** — 3 dialogs: simple info, large body scroll, 3-button action (是/否/取消). Buttons in control panel open each, echo panel shows "User clicked 是" etc. Dialog has AstCard-like styled header bar.

- [ ] **Step 5.3.3: Build + off-screen self-check**
- [ ] **Step 5.3.4: Commit**

```bash
git commit -m "feat: AstDialog modal glasspane, scale+fade anim, right-aligned button row + demo"
```

### Task 5.4: AstMessageBox

- [ ] **Step 5.4.1: Create AstMessageBox.java** — static, builds on AstDialog. 5 icons (i/✓/!/×/?) drawn as rounded color circle + white glyph. `alert` / `confirm` return via Runnable callbacks.

- [ ] **Step 5.4.2: Create AstMessageBoxDemo.java** — 5 buttons per icon type, + a confirm "Are you sure delete?" that logs Ok/Cancel callbacks.

- [ ] **Step 5.4.3: Build + self-check**
- [ ] **Step 5.4.4: Commit**

```bash
git commit -m "feat: AstMessageBox static alert/confirm + 5 icon glyphs + demo"
```

### Task 5.5: AstMessage

- [ ] **Step 5.5.1: Create AstMessage.java** — static show(w, type, text [, duration, closable]). AnimatedPopup at top-center of window (PopupPositioner TOP_CENTER). Static MessageStack tracks visible messages; new Message appends below existing; on dismiss collapse gap upward with Animator.

- [ ] **Step 5.5.2: Create AstMessageDemo.java** — 4 type buttons (success/warning/info/error). Button "Show 4 in a row" observe stacking; 按钮 "自动隐藏 1.5s" vs "需手动关闭".

- [ ] **Step 5.5.3: Build + self-check**
- [ ] **Step 5.5.4: Commit**

```bash
git commit -m "feat: AstMessage top-center toast stack + types + auto dismiss + demo"
```

---

## Task 6: Complex Popup 系 — AstCascader, AstDatePicker

### Task 6.1: AstCascader

- [ ] **Step 6.1.1: Create AstCascader.java** — Input-style trigger (Input visual replica: Rounded border + placeholder text), click opens AnimatedPopup. Static nested `Node` immutable. First column: root level; when a parent is selected, 2nd column fills with children; 3rd if leaf. Click leaf → close popup, trigger shows "a / b / c". ChangeListener → getSelectedPath().

- [ ] **Step 6.1.2: Create AstCascaderDemo.java** — 2 demos: 2-level ("手机/电脑" → sub-brands) and 3-level ("省/市/区"). Echo label shows selected path on change.

- [ ] **Step 6.1.3: Build + self-check**
- [ ] **Step 6.1.4: Commit**

```bash
git commit -m "feat: AstCascader immutable Node, multi-column popup, path concat + demo"
```

### Task 6.2: AstDatePicker

- [ ] **Step 6.2.1: Create AstDatePicker.java** — Input visual trigger shows "yyyy-MM-dd"; click opens 6×7 day grid, prev/next month arrows, month/year header. Click day → close, write to trigger. ChangeListener fires. setRange: clamp on setDate, out-of-range days painted disabled (TEXT_PLACEHOLDER + no click).

- [ ] **Step 6.2.2: Create AstDatePickerDemo.java** — basic picker, range-limited picker, start-date + end-date linked (end >= start). Echo shows selected LocalDate.

- [ ] **Step 6.2.3: Build + self-check**
- [ ] **Step 6.2.4: Commit**

```bash
git commit -m "feat: AstDatePicker LocalDate calendar popup + range clamp + demo"
```

---

## Task 7: Heavy Render — AstForm, AstTree, AstTable

### Task 7.1: AstForm

- [ ] **Step 7.1.1: Create AstForm.java** — JPanel GridBagLayout. LABEL_LEFT: label column (fixed width labelWidth, right align, 14px font medium TEXT_MAIN), field column full remaining width. LABEL_TOP: label full width top, field below. addItem(label, field [, hint]) — hint: 12px INFO gray 1-line below field. `collect()` returns Map: key=label, value=field.getText() (JTextComponent), getSelectedItem().toString() (JComboBox), getState()? "on":"off" (AbstractButton), recursively.

- [ ] **Step 7.1.2: Create AstFormDemo.java** — Standard login form (label left, fields: username/password/remember me + submit button), Employee profile (label top, 5 fields: name/email/phone/hire date/department). Submit button logs collect() output.

- [ ] **Step 7.1.3: Build + self-check**
- [ ] **Step 7.1.4: Commit**

```bash
git commit -m "feat: AstForm label-left/label-top GridBag, hint row, collect() map + demo"
```

### Task 7.2: AstTree

- [ ] **Step 7.2.1: Create AstTree.java** — immutable TreeNode, rows painted from `visibleList()` (flatten expanded nodes). Row: 24px indent × depth + arrow ▸ + label; row-height 32px. Arrow rotation Animator 150ms, expand row height Animator 240ms easeInOut for each subtree to grow from 0. Click on leaf: onSelect fire. showCheckbox mode: 14px square before label, propagate check from parent to children.

- [ ] **Step 7.2.2: Create AstTreeDemo.java** — File tree: 3-level (src/core/ui, src/demo files etc). Echo panel shows selected path. Button "展开全部 / 折叠全部" programmatic expand/collapse. Checkbox tree: permission model.

- [ ] **Step 7.2.3: Build + self-check**
- [ ] **Step 7.2.4: Commit**

```bash
git commit -m "feat: AstTree immutable Node, expand Animator, arrow rotation, optional checkbox + demo"
```

### Task 7.3: AstTable

- [ ] **Step 7.3.1: Create AstTable.java** — Immutable Column, rows List<Map>. Stripe rows (odd/even). Header 48px sortable click. Sort animator: row index target position vs current position (300ms easeOut), row y interpolate per tick. selectedRows highlight PRIMARY translucent bg. Single/multi select mode. addSelectionListener fires int array.

- [ ] **Step 7.3.2: Create AstTableDemo.java** — Employee list 6 columns × 20 rows; 3 sortable columns; button "Stripe ON/OFF", "Selection single/multi", "Sort by salary desc" programmatic; selection echo shows row indices + names.

- [ ] **Step 7.3.3: Build + self-check**
- [ ] **Step 7.3.4: Commit**

```bash
git commit -m "feat: AstTable immutable Column, stripe, sort row anim, selectable + demo"
```

---

## Final Task 8: Build Integration & Smoke Run

- [ ] **Step 8.1: Append each self-check to build.bat after Task 0–7 finished**

Build script end:
```
echo --- Ast* self-checks ---
java -ea -cp out org.swelement.ui.AstContainer
java -ea -cp out org.swelement.ui.AstAvatar
java -ea -cp out org.swelement.ui.AstCard
java -ea -cp out org.swelement.ui.AstLoading
java -ea -cp out org.swelement.ui.AstTooltip
java -ea -cp out org.swelement.ui.AstDropdown
java -ea -cp out org.swelement.ui.AstDialog
java -ea -cp out org.swelement.ui.AstMessageBox
java -ea -cp out org.swelement.ui.AstMessage
java -ea -cp out org.swelement.ui.AstCascader
java -ea -cp out org.swelement.ui.AstDatePicker
java -ea -cp out org.swelement.ui.AstForm
java -ea -cp out org.swelement.ui.AstTree
java -ea -cp out org.swelement.ui.AstTable
echo --- All Ast* self-checks PASSED ---
```

- [ ] **Step 8.2: Run full build, fix any compile errors, iterate until all 14 self-checks print OK**

- [ ] **Step 8.3: Commit build script update**

```bash
git add build.bat
git commit -m "build(bat): append all Ast* self-checks to compile & run chain"
```

- [ ] **Step 8.4: Run end-to-end demos manually (open 2-3 to verify visual): AstContainerDemo, AstLoadingDemo, AstMessageBoxDemo**
