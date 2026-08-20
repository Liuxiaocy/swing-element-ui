# P1 Popup Components Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 5 Element UI Popup components for Swing: Tooltip (提示框), Dropdown (下拉菜单), Dialog (对话框), MessageBox (消息弹窗), and Message (全局消息/Toast). All use Ast* prefix, reuse core AnimatedPopup + PopupPositioner + GlassPane engines, meet WCAG contrast ≥4.5:1, with Animator-driven fade/opening transitions, full self-check coverage, and a single AstPopupDemo interactive frame exercising all 5.

**Architecture:** Each popup component composes AnimatedPopup as its pop layer container + registers as its content. AstDialog/AstMessageBox additionally use GlassPane for modality. AstMessage uses JLayeredPane.POPUP_LAYER with TOP_CENTER positioning for global toasts. All paintComponent methods use Graphics2D (antialiased + stroke pure), round rect 8px, ElementTheme tokens, isOptimizedDrawingEnabled=false, assertContrast before text draw. Hover/action animators 150ms easeInOut; open fades 220ms easeOut; tooltips use 200ms entry delay via javax.swing.Timer.

**Tech Stack:** Java 8, javax.swing (JComponent/JPanel/JLabel/Timer), java.awt (Graphics2D/Shape/BasicStroke/Insets/Rectangle/Point/FontMetrics/Color), core (Animator/Easing/ElementTheme/AnimatedPopup/PopupPositioner/GlassPane).

---

## File Structure (before coding — locked)

| File | Role |
|------|------|
| `src/org/swelement/ui/AstTooltip.java` | NEW: Component decorator — attach tooltip text + direction to any JComponent; singleton global Tooltip glass-popup via AnimatedPopup |
| `src/org/swelement/ui/AstDropdown.java` | NEW: Toggle/menu popup — button invoker + list of items (String label + action listener) |
| `src/org/swelement/ui/AstDialog.java` | NEW: Modal dialog — title, body content, OK/Cancel footer buttons, GlassPane backdrop |
| `src/org/swelement/ui/AstMessageBox.java` | NEW: Preset dialog variants (INFO/SUCCESS/WARNING/ERROR/QUESTION + icon + confirm/cancel callbacks, wrapping AstDialog) |
| `src/org/swelement/ui/AstMessage.java` | NEW: Global toast (1-liner) — success/info/warning/error, TOP_CENTER (default) or BOTTOM_RIGHT_CORNER, 3s auto close with fade out |
| `src/org/swelement/demo/AstPopupDemo.java` | NEW: Single unified demo for all 5 popup components (6 sections: Tooltip buttons, Dropdown menu, Dialog actions, MessageBox 5 types, Message toast buttons, combined interactive check) |
| `build.bat` | MODIFY: append 6 source files to `SET SOURCES` list + append 5 self-check blocks at tail |

---

## Common Pattern (all popup components)

1. **Constructor public surface**: include null checks → throw `IllegalArgumentException` with descriptive text; never allow silent invalid states.
2. **Theme & contrasts**: Use `ElementTheme.TEXT_MAIN / TEXT_REGULAR / TEXT_PLACEHOLDER / PRIMARY / SUCCESS / WARNING / DANGER / BORDER_BASE / RADIUS / FONT` constants only; never ad-hoc magic hex unless 1-time `new Color(r,g,b,a)` transient composites (like translucent PRIMARY rings). All text paints preceded by `ElementTheme.assertContrast(fg, bg, "AstXxx.reason")`.
3. **Paint order** (for custom components): `g2.create()` → set rendering hints → `Insets in = getInsets()`; use x=in.left, y=in.top; w=getWidth()-in.left-in.right; h=getHeight()-in.top-in.bottom; round rect drawRect using floats ±0.5 for 1px alignment. `g2.dispose()` end.
4. **Animator callbacks**: For any `Animator.go(from,to,onComplete)` (3-arg form), ensure order is `stop(); animator.go(current, target, callback);` so in-flight animations don't overwrite state.
5. **isOptimizedDrawingEnabled**: Override to return false on all custom components that paint after subcomponents.
6. **Self-check main**: Each Ast* class has `public static void main(String[] args) { selfCheck(); }`. selfCheck wraps Swing ops in `SwingUtilities.invokeAndWait(Runnable)`, paints to BufferedImage, asserts critical pixels alpha≥100 for visible content, exercises constructor null-guards via try/catch with `threw = true` flag. Prints `AstXxx self-check OK`.

---

### Task 1: AstTooltip

**Files:**
- Create: `src/org/swelement/ui/AstTooltip.java`
- Modify: `build.bat` (sources list + self-check block)

#### Design

API:
```java
public class AstTooltip {
    public enum Effect { DARK, LIGHT }  // Element UI 两种主题（DARK 默认）
    // Attach a static tooltip to a JComponent. When user hovers 200ms, popup appears in `dir` direction.
    public static void attach(JComponent target, String text);
    public static void attach(JComponent target, String text, AnimatedPopup.Direction dir);
    public static void attach(JComponent target, String text, AnimatedPopup.Direction dir, Effect effect);
    // Detach any tooltip attached to target.
    public static void detach(JComponent target);
}
```

Implementation:
- Use a **single shared** `AnimatedPopup` instance (singleton popup). Content: dark/light rounded balloon + 12px padding + centered text. 14px font.
- Register mouse listeners on attach: `mouseEntered → start javax.swing.Timer(200ms, show popup)`; `mouseExited / mousePressed → cancel Timer + hideWithAnimation`.
- Popup direction: param or BELOW default. Use `popup.show(invoker, dir)` to compute position.
- DARK effect: bg = new Color(0x30,0x31,0x33, alpha); text = Color.WHITE; border = none (no card border). LIGHT effect: bg = Color.WHITE; border = BORDER_BASE; text = TEXT_MAIN. Call assertContrast for each effect on each paint.
- Content size: `textWidth fm.stringWidth + 24; height = fm.getHeight() + 12`. Pref size uses max 320 width (wrap is optional; for simplicity support single-line).

Self-check:
```java
static void selfCheck() {
    // Attach → tooltip registered; null target → IAE, null text → IAE
    boolean threw = false;
    try { AstTooltip.attach(null, "x"); } catch (IllegalArgumentException iae) { threw = true; }
    assert threw : "target null"; threw = false;
    try { AstTooltip.attach(new JLabel(), null); } catch (IllegalArgumentException iae) { threw = true; }
    assert threw : "text null";
    // Use invokeAndWait: attach to 2 buttons, simulate ENTER timer trigger with fake e → paint offscreen
    final Throwable[] err = {null};
    try {
        SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame(); jf.setSize(800, 600); jf.setVisible(true);
            JButton b = new JButton("HOVER ME"); b.setBounds(200, 200, 120, 36);
            jf.getContentPane().setLayout(null); jf.getContentPane().add(b);
            AstTooltip.attach(b, "Hi tooltip!", AnimatedPopup.Direction.BELOW, AstTooltip.Effect.DARK);
            // Trigger attach's internal logic by posting a synthetic mouseEntered event
            b.dispatchEvent(new MouseEvent(b, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 2, 2, 0, false));
            // Wait for show timer to fire (>200ms) — 260ms sleep
            try { Thread.sleep(260); } catch (InterruptedException ignore) {}
            // Paint glass pane: access layered pane of jf
            JLayeredPane lp = jf.getLayeredPane();
            // Ensure popup added: search AnimatedPopup descendants (3 levels deep)
            boolean found = false;
            for (int i = 0; i < lp.getComponentCount(); i++) {
                Component c = lp.getComponent(i);
                if (c instanceof AnimatedPopup) { found = true; break; }
            }
            assert found : "tooltip popup should be added to layered pane after 200ms delay";
            AstTooltip.detach(b);
            jf.dispose();
        }});
    } catch (Throwable t) { err[0] = t; }
    if (err[0] != null) throw new RuntimeException(err[0]);
    System.out.println("AstTooltip self-check OK");
}
```

#### Steps
- [ ] **Step 1**: Write AstTooltip per design
- [ ] **Step 2**: Append `AstTooltip.java` to build sources; append self-check block
- [ ] **Step 3**: Build: `.\build.bat` → BUILD OK
- [ ] **Step 4**: `java -ea -cp out org.swelement.ui.AstTooltip` → OK
- [ ] **Step 5**: Git commit: `git add src/org/swelement/ui/AstTooltip.java build.bat; git commit -m "feat: AstTooltip dark/light attach with 200ms hover delay, 4 directions"`

---

### Task 2: AstDropdown

**Files:**
- Create: `src/org/swelement/ui/AstDropdown.java`
- Modify: `build.bat`

#### Design

API:
```java
public class AstDropdown extends JComponent {
    public static class Item { public final String label; public final ActionListener action; public Item(String l, ActionListener a) { label=l; action=a; } }
    // Custom-styled type PRIMARY button invoker. Default direction = BELOW.
    public AstDropdown(String invokerLabel, Item[] items);
    public AstDropdown(String invokerLabel, Item[] items, AnimatedPopup.Direction dir);
    // Toggle programmatic
    public void showDropdown();
    public void hideDropdown();
    public boolean isOpen();
    // Button text update
    public void setInvokerText(String s);
    public String getInvokerText();
}
```

Implementation notes:
- Layout: a single `Button` (use existing org.swelement.ui.Button PRIMARY style with chevron `▾` suffix appended) as invoker. On click toggles AnimatedPopup containing items list. Items list: vertical `BoxLayout(Y)` with `ItemRow` components. Each ItemRow: 36px height, padding left=14px, paint full-width BORDER_BASE divider between rows; hover highlights background with `new Color(PRIMARY.r, PRIMARY.g, PRIMARY.b, 18 alpha)` and text set to PRIMARY (use float hover state + Animator 150ms easeInOut). Mouse click: actionPerformed on ItemRow row fires item.action + hideDropdown().
- Popup minimum width = invoker width (min 140px), max width = 360px. Per row: 36px; up to 8 rows visible; 9+ rows add scroll via JScrollPane.
- Add `AnimatedPopup.registerGlobal(popup, PopupLayer.POPUP)` for z-order.
- paintComponent for ItemRow: draw bg rounded inside 0-radius rect (flat card → no round per row; outer popup has round). assertContrast TEXT_MAIN vs WHITE (idle) vs hover-bg (PRIMARY tint).
- Invoker Button click: `popup.getParent() == null ? showDropdown() : hideDropdown()`; use dismissListener on popup to keep isOpen() synchronized.

Self-check:
```java
static void selfCheck() {
    // Empty item array → IAE; null label → IAE; null items → IAE
    boolean threw = false;
    try { new AstDropdown("x", new AstDropdown.Item[0]); } catch (IllegalArgumentException e) { threw = true; }
    assert threw; threw = false;
    try { new AstDropdown(null, new Item[]{ new Item("a",null) }); } catch (IllegalArgumentException e) { threw = true; }
    assert threw;
    final Throwable[] err = {null};
    try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
        JFrame jf = new JFrame(); jf.setSize(600, 500); jf.setVisible(true);
        final int[] fireCount = {0};
        AstDropdown dd = new AstDropdown("菜单", new Item[]{
            new Item("操作1", new ActionListener() { public void actionPerformed(ActionEvent e) { fireCount[0]++; }}),
            new Item("操作2", null)
        });
        JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new FlowLayout()); cp.add(dd); jf.pack();
        // Programmatically showDropdown()
        dd.showDropdown();
        assert dd.isOpen() : "after showDropdown isOpen";
        // Click item 0 via synthetic mouse click on first ItemRow descendant within AnimatedPopup
        JLayeredPane lp = jf.getLayeredPane();
        AnimatedPopup popup = null;
        for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { popup = (AnimatedPopup) lp.getComponent(i); break; }
        assert popup != null;
        Component row = findChildByClass(popup, "AstDropdown$ItemRow", 0);
        row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, row.getWidth()/2, row.getHeight()/2, 1, false));
        try { Thread.sleep(20); } catch (Throwable ignore) {}
        row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, row.getWidth()/2, row.getHeight()/2, 1, false));
        // Swing processes on EDT; allow fireCount[0] to increment via listeners
        try { Thread.sleep(40); } catch (Throwable ignore) {}
        assert fireCount[0] == 1 : "item click fired listener; actual="+fireCount[0];
        // After action, popup hidden
        assert !dd.isOpen() : "post click dropdown closed";
        // Offscreen paint ItemRow to test assertContrast
        JPanel jp = new JPanel(); jp.setSize(240, 36);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(240, 36, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        // Create an ItemRow manually via reflection or via a child capture
        AstDropdown dd2 = new AstDropdown("T", new Item[]{ new Item("abc", null) });
        dd2.showDropdown();
        AnimatedPopup p2 = null;
        JFrame jf2 = new JFrame(); jf2.setContentPane(new JPanel()); jf2.getContentPane().add(dd2);
        jf2.pack(); jf2.setVisible(true);
        lp = jf2.getLayeredPane();
        for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { p2 = (AnimatedPopup) lp.getComponent(i); break; }
        Component row2 = findChildByClass(p2, "AstDropdown$ItemRow", 0);
        row2.setBounds(0, 0, 240, 36);
        Graphics2D gg = img.createGraphics();
        try { row2.paint(gg); } finally { gg.dispose(); }
        int px = img.getRGB(10, 18); int alphaPx = (px >>> 24) & 0xFF;
        assert alphaPx > 100 : "row painted opaque enough";
        dd2.hideDropdown(); jf.dispose(); jf2.dispose();
    }}); } catch (Throwable t) { err[0] = t; }
    if (err[0] != null) throw new RuntimeException(err[0]);
    System.out.println("AstDropdown self-check OK");
}
```

Note: `findChildByClass(Container parent, String classNameFragment, int idx)` helper method: DFS walk, match class name, return idx-th match. If not found return null. Use reflection-free by `c.getClass().getName().contains(classNameFragment)`.

#### Steps
- [ ] **Step 1**: Write AstDropdown
- [ ] **Step 2**: Append source to build.bat + self-check block (after AstTooltip block)
- [ ] **Step 3**: `.\build.bat` → BUILD OK
- [ ] **Step 4**: `java -ea -cp out org.swelement.ui.AstDropdown` → OK + regression run AstTooltip OK
- [ ] **Step 5**: Git commit `git add src/org/swelement/ui/AstDropdown.java build.bat; git commit -m "feat: AstDropdown invoker, item rows with hover, scroll, click actions"`

---

### Task 3: AstDialog

**Files:**
- Create: `src/org/swelement/ui/AstDialog.java`
- Modify: `build.bat`

#### Design

API:
```java
public class AstDialog {
    public static final int RESULT_OK = 1;
    public static final int RESULT_CANCEL = 2;
    public interface ResultCallback { void onResult(int resultCode); }

    // Show a modal dialog centered within target Frame's root pane.
    // width default 480, height auto by content but ≥ 200, ≤ 600
    public static void show(java.awt.Window owner, String title, JComponent body, final ResultCallback cb);
    public static void show(java.awt.Window owner, String title, String okText, String cancelText, JComponent body, final ResultCallback cb);
    // Returns the custom card panel body used (useful for assertions)
    static JPanel makeCard(String title, String okText, String cancelText, JComponent body, final ResultCallback cb, final Runnable onClosed);
}
```

Implementation:
- Install GlassPane on owner RootPaneContainer via `GlassPane.install(owner)`. If already a GlassPane present from prior show, reuse with new active state (don't double-install — cache via client property `clientProperty(AstDialog.class.getName() + ".gp")`).
- Body is CENTER; NORTH title bar 48px bold 16px TEXT_MAIN; SOUTH footer 64px with right-aligned buttons: [Cancel (DEFAULT, left button)] [OK (PRIMARY, right button)]. OK button triggers cb.onResult(RESULT_OK) + close. Cancel → RESULT_CANCEL + close.
- Use `AnimatedPopup` container with Direction.TOP_CENTER positioned? No — use a custom dialog card JPanel centered in GlassPane. GlassPane overlay = true (setActive true) on show.
- Use `Animator fadeAnim` 220ms for card entrance + card exit.
- On close callback: card fades out (Animator 220ms easeIn on alpha), then GlassPane.setActive(false), then remove from owner.
- Contrast: assertContrast title TEXT_MAIN on card white bg; assert body TEXT_REGULAR on white bg; OK button PRIMARY bg + WHITE text contrast via ElementTheme.pickTextColorForBg.
- Painting: Card content = RoundRectangle2D Float corner RADIUS*2. Title separator at y=48. Footer separator at height - 64. Full assertContrast for all text.

Self-check:
```java
static void selfCheck() {
    final Throwable[] err = {null};
    final int[] res = {0};
    try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
        JFrame jf = new JFrame(); jf.setSize(800, 600); jf.setVisible(true);
        JPanel body = new JPanel(new BorderLayout());
        JLabel info = new JLabel("<html>内容区<br>多行文字信息<br>第三行</html>", JLabel.CENTER);
        info.setFont(info.getFont().deriveFont(13f)); info.setForeground(ElementTheme.TEXT_REGULAR);
        body.add(info, BorderLayout.CENTER);
        AstDialog.show(jf, "对话框标题", "保存", "取消", body, new AstDialog.ResultCallback() {
            public void onResult(int resultCode) { res[0] = resultCode; }
        });
        // Wait for glass pane + dialog card added to glass pane. Search glass pane children.
        try { Thread.sleep(260); } catch (InterruptedException ignore) {}
        Component gp = jf.getGlassPane();
        assert gp != null;
        assert gp.isVisible() : "glass pane visible while modal";
        // Find card JPanel — first child of glassPane at depth.
        Container c = (Container) gp;
        Component card = null;
        for (int i = 0; i < c.getComponentCount(); i++) {
            if (c.getComponent(i) instanceof JPanel && ((JPanel)c.getComponent(i)).getComponentCount() > 0) {
                card = c.getComponent(i); break;
            }
        }
        assert card != null : "dialog card panel must exist as child of glass pane";
        // Click OK button — traverse card to find a Button with text "保存" via findChildByClass filter by label string equals
        Component ok = findChildByText(card, "保存");
        Component cancel = findChildByText(card, "取消");
        assert ok != null; assert cancel != null;
        cancel.dispatchEvent(new MouseEvent(cancel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 10, 10, 1, false));
        try { Thread.sleep(20); } catch (Throwable ignore) {}
        cancel.dispatchEvent(new MouseEvent(cancel, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 10, 10, 1, false));
        try { Thread.sleep(280); } catch (InterruptedException ignore) {}
        assert res[0] == RESULT_CANCEL : "result should be cancel=" + RESULT_CANCEL + " actual=" + res[0];
        // Show second time → click OK → assert RESULT_OK
        AstDialog.show(jf, "T2", body, new AstDialog.ResultCallback() { public void onResult(int resultCode) { res[0] = resultCode; }});
        try { Thread.sleep(260); } catch (InterruptedException ignore) {}
        card = null; gp = jf.getGlassPane(); c = (Container) gp;
        for (int i = 0; i < c.getComponentCount(); i++) if (c.getComponent(i) instanceof JPanel && c.getComponent(i).getComponentCount()>0) { card = c.getComponent(i); break; }
        Component ok2 = findChildByText(card, "确定");  // default okText
        ok2.dispatchEvent(new MouseEvent(ok2, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 10, 10, 1, false));
        try { Thread.sleep(20); } catch (Throwable ignore) {}
        ok2.dispatchEvent(new MouseEvent(ok2, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 10, 10, 1, false));
        try { Thread.sleep(300); } catch (InterruptedException ignore) {}
        assert res[0] == RESULT_OK : "result should be OK=" + RESULT_OK + " actual="+res[0];
        jf.dispose();
    }}); } catch (Throwable t) { err[0] = t; }
    if (err[0] != null) throw new RuntimeException(err[0]);
    // makeCard static access test: paint card off-screen
    JPanel card = AstDialog.makeCard("X", "A", "B", new JLabel("body"), null, null);
    card.setSize(480, 240);
    java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(480, 240, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    Graphics2D gg = img.createGraphics();
    try { card.paint(gg); } finally { gg.dispose(); }
    int px = img.getRGB(40, 120);
    int a = (px >>> 24) & 0xFF;
    assert a > 120 : "card bg should be rendered opaque";
    System.out.println("AstDialog self-check OK");
}
```
Helper `findChildByText(Component root, String text)`: DFS, return first Button/JLabel with text equal.

#### Steps
- [ ] **Step 1**: Write AstDialog
- [ ] **Step 2**: Append source + self-check block to build.bat
- [ ] **Step 3**: `.\build.bat` → BUILD OK
- [ ] **Step 4**: `java -ea -cp out org.swelement.ui.AstDialog` → OK
- [ ] **Step 5**: Git commit `git add src/org/swelement/ui/AstDialog.java build.bat; git commit -m "feat: AstDialog modal card with glass pane, 2-button footer, fade transitions"`

---

### Task 4: AstMessageBox + AstMessage

**Files:**
- Create: `src/org/swelement/ui/AstMessageBox.java`
- Create: `src/org/swelement/ui/AstMessage.java`
- Modify: `build.bat`

#### Design A — AstMessageBox (uses AstDialog)

API:
```java
public class AstMessageBox {
    public enum MessageBoxType { INFO, SUCCESS, WARNING, ERROR, QUESTION }
    public interface ConfirmCallback { void onConfirm(); default void onCancel() {} }

    // show(message) — INFO, OK only
    public static void alert(Window owner, String message);
    // show with type — OK only; icon per type
    public static void alert(Window owner, MessageBoxType type, String message);
    // confirm with YES + NO; callback onConfirm or onCancel
    public static void confirm(Window owner, String message, final ConfirmCallback cb);
    public static void confirm(Window owner, MessageBoxType type, String message, final ConfirmCallback cb);
    // Icon color per type: INFO PRIMARY, SUCCESS SUCCESS green, WARNING WARNING yellow, ERROR DANGER red, QUESTION PRIMARY
    // Draw icon with Graphics2D: rounded shape (circular badge 48×48) with Unicode glyph or simple shape (INFO = "i"; SUCCESS = "√"; WARNING="!"; ERROR="×"; QUESTION="?")
}
```

Implementation:
- `alert(owner, type, msg)` internally creates `makeCard(title, msg)` with icon panel on left (width 80, centered icon), message label on right (BorderLayout CENTER) — passes to AstDialog.show.
- `confirm` uses OK button = "确定", Cancel = "取消"; maps RESULT_OK → onConfirm, RESULT_CANCEL → onCancel.
- Icon paint: 48px circle, background color = type-themed (PRIMARY/SUCCESS/WARNING/DANGER/PRIMARY); foreground color = `pickTextColorForBg(bg)`; glyph font = FONT deriveFont BOLD 28px; drawString centered within 48×48 circle. assertContrast glyph-on-circle.

Self-check:
```java
static void selfCheck() {
    // all 5 types + confirm flow
    final Throwable[] err = {null};
    final int[] cb = {0, 0}; // index 0=confirm count, 1=cancel count
    try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
        JFrame jf = new JFrame(); jf.setSize(800, 600); jf.setVisible(true);
        AstMessageBox.alert(jf, AstMessageBox.MessageBoxType.ERROR, "发生错误");
        try { Thread.sleep(260); } catch (InterruptedException ignore) {}
        Component gp = jf.getGlassPane();
        assert gp.isVisible();
        // Click OK (确定)
        Component card = firstPanelChild((Container) gp);
        Component ok = findChildByText(card, "确定");
        clickComponent(ok);
        try { Thread.sleep(300); } catch (InterruptedException ignore) {}
        // Confirm dialog → first click cancel → onCancel
        AstMessageBox.confirm(jf, AstMessageBox.MessageBoxType.QUESTION, "确认删除？", new ConfirmCallback() {
            public void onConfirm() { cb[0]++; }
            public void onCancel() { cb[1]++; }
        });
        try { Thread.sleep(260); } catch (InterruptedException ignore) {}
        card = firstPanelChild((Container) jf.getGlassPane());
        Component cancel = findChildByText(card, "取消");
        clickComponent(cancel);
        try { Thread.sleep(300); } catch (InterruptedException ignore) {}
        assert cb[1] == 1 : "onCancel should fire once";
        // Confirm → click OK
        AstMessageBox.confirm(jf, "ok?", new ConfirmCallback() { public void onConfirm() { cb[0]++; }});
        try { Thread.sleep(260); } catch (InterruptedException ignore) {}
        card = firstPanelChild((Container) jf.getGlassPane());
        Component ok2 = findChildByText(card, "确定");
        clickComponent(ok2);
        try { Thread.sleep(300); } catch (InterruptedException ignore) {}
        assert cb[0] == 1 : "onConfirm should fire once";
        // Off-screen paint icon component (JPanel) for each type to assert contrast
        for (MessageBoxType t : MessageBoxType.values()) {
            JPanel icon = AstMessageBox.makeIconPanel(t); // public or package-private helper
            icon.setSize(80, 64); icon.doLayout();
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(80, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics(); try { icon.paint(gg); } finally { gg.dispose(); }
            int center = img.getRGB(40, 32);
            int ca = (center >>> 24) & 0xFF;
            assert ca >= 120 : "icon bg must be opaque for type="+t;
        }
        jf.dispose();
    }}); } catch (Throwable t) { err[0] = t; }
    if (err[0] != null) throw new RuntimeException(err[0]);
    System.out.println("AstMessageBox self-check OK");
}
```

Helper `clickComponent`: dispatches PRESSED+RELEASED.

#### Design B — AstMessage (toast)

API:
```java
public class AstMessage {
    public enum MessageType { INFO, SUCCESS, WARNING, ERROR }

    // Centered-top, auto close after 3 seconds (default)
    public static void show(Window owner, MessageType type, String text);
    // Custom duration in ms (minimum 500ms); durations ≤0 treated as 3000
    public static void show(Window owner, MessageType type, String text, int durationMs);
}
```

Implementation:
- Creates AnimatedPopup with direction TOP_CENTER. Content: JPanel card, width = fm.stringWidth(text)+120 (icon 32, padding 32 each side + 8 icon-text gap). Height 48. Icon 32×20 badge glyph on left (same glyph as MessageBox): INFO="i", SUCCESS="√", WARNING="!", ERROR="×"; background color circular badge. Text: 14px TEXT_MAIN, vertical center. assertContrast.
- Popup Layer: `AnimatedPopup.registerGlobal(popup, PopupLayer.TOOL)`. Uses PopupPositioner Direction.TOP_CENTER.
- Duration: `new javax.swing.Timer(durationMs, hideWithAnimation callback)` — on action: timer.stop() + popup.hideWithAnimation(null).
- Multiple toasts allowed; each subsequent adds below previous if another is already visible (y-offset by 56px per open message).

Self-check:
```java
static void selfCheck() {
    // Null owner → IAE; null text → IAE; null type → IAE
    boolean threw = false;
    try { AstMessage.show(null, MessageType.INFO, "x"); } catch (IllegalArgumentException iae) { threw = true; }
    assert threw; threw = false;
    try { AstMessage.show(new JFrame(), null, "x"); } catch (IllegalArgumentException iae) { threw = true; }
    assert threw; threw = false;
    try { AstMessage.show(new JFrame(), MessageType.INFO, null); } catch (IllegalArgumentException iae) { threw = true; }
    assert threw;
    final Throwable[] err = {null};
    try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
        JFrame jf = new JFrame(); jf.setSize(800, 600); jf.setVisible(true);
        AstMessage.show(jf, MessageType.SUCCESS, "保存成功");
        try { Thread.sleep(320); } catch (InterruptedException ignore) {}
        // Verify layered pane contains ≥1 AnimatedPopup with TOOL layer content — paint & get alpha
        JLayeredPane lp = jf.getLayeredPane();
        AnimatedPopup popup = null;
        for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { popup = (AnimatedPopup) lp.getComponent(i); break; }
        assert popup != null : "toast popup added";
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(480, 48, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        popup.setBounds(0,0,480,48); try { popup.paint(gg); } finally { gg.dispose(); }
        int px = img.getRGB(40, 24); int a = (px >>> 24) & 0xFF;
        assert a >= 120 : "toast bg opaque enough";
        // Show 2nd → verify 2 popups; y positions offset by ≥40
        AstMessage.show(jf, MessageType.INFO, "第二条信息", 800);
        try { Thread.sleep(320); } catch (InterruptedException ignore) {}
        int count = 0;
        int[] ys = new int[2]; int yi = 0;
        for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { count++; if (yi<2) ys[yi++] = lp.getComponent(i).getY(); }
        assert count >= 2 : "at least 2 toasts visible after 2nd show";
        int diff = Math.abs(ys[0] - ys[1]);
        assert diff >= 40 : "toasts offset vertically by 40+px";
        jf.dispose();
    }}); } catch (Throwable t) { err[0] = t; }
    if (err[0] != null) throw new RuntimeException(err[0]);
    System.out.println("AstMessage self-check OK");
}
```

#### Steps
- [ ] **Step 1**: Write AstMessageBox
- [ ] **Step 2**: Write AstMessage
- [ ] **Step 3**: Append 2 sources + 2 self-check blocks to build.bat
- [ ] **Step 4**: `.\build.bat` → BUILD OK
- [ ] **Step 5**: Run self-checks AstMessage, AstMessageBox — both OK
- [ ] **Step 6**: Regression — AstTooltip/AstDropdown/AstDialog all OK
- [ ] **Step 7**: Git commit `git add src/org/swelement/ui/AstMessageBox.java src/org/swelement/ui/AstMessage.java build.bat; git commit -m "feat: AstMessageBox 5 types + confirm callbacks; AstMessage toast center-top with offset + auto close"`

---

### Task 5: AstPopupDemo + build.bat finalize

**Files:**
- Create: `src/org/swelement/demo/AstPopupDemo.java`
- Finalize: `build.bat`

#### Demo layout

```
Root: BoxLayout Y (JScrollPane)
├─ 控制栏: (仅说明文字)
├─ Section 1: AstTooltip (卡片 GridLayout 2 rows × 4 cols) → 8 buttons each with 4 directions × 2 effects: (ABOVE/DARK, BELOW/DARK, LEFT/DARK, RIGHT/DARK, ABOVE/LIGHT, BELOW/LIGHT, LEFT/LIGHT, RIGHT/LIGHT)
├─ Section 2: AstDropdown (3 columns):
│  ├─ 左 Col1: Basic dropdown (BELOW) — 4 menu items
│  ├─ 中 Col2: Above direction dropdown with 10 items → test scroll
│  └─ 右 Col3: Right direction dropdown with ActionListener echo panel
├─ Section 3: AstDialog — 2 buttons: 打开自定义对话框 (with form-like body of 3 JTextFields for 姓名/邮箱/电话) + 打开空内容对话框
├─ Section 4: AstMessageBox — 5 alert buttons (INFO/SUCCESS/WARNING/ERROR/QUESTION OK-only) + 2 confirm buttons (CONFIRM YES/NO) with callback echo
├─ Section 5: AstMessage — 4 type buttons (INFO/SUCCESS/WARNING/ERROR) + 2 duration tests (500ms quick / 6s long)
└─ Section 6: 综合区 - AstCard with Progress wrapped in AstLoading WRAP, click按钮会先用 AstMessage toast → 再 AstDialog 确认 → AstTooltip 按钮提示
```

Add a `JLabel echo` line at bottom-right showing last callback fired.

**Requirements:**
- InvokeLater wrap start()
- Every button/action that invokes a popup should include at least 1 echo print (System.out.println or echo label) to demonstrate callbacks fire correctly
- Timer delays all javax.swing.Timer, no Thread.sleep on EDT (test in self-checks can use Thread.sleep but those are only invoked by self-check main which runs off EDT typically)

**Steps**
- [ ] **Step 1**: Write AstPopupDemo.java
- [ ] **Step 2**: Add AstPopupDemo.java to build sources list
- [ ] **Step 3**: `.\build.bat` → BUILD OK
- [ ] **Step 4**: Regression all 5 self-checks pass
- [ ] **Step 5**: Git commit `git add src/org/swelement/demo/AstPopupDemo.java build.bat; git commit -m "feat: AstPopupDemo with tooltip/dropdown/dialog/messagebox/message 6 sections"`

---

### Finalization Checklist (run by implementer after all 5 tasks complete)

- [ ] All 5 self-checks pass when run one-by-one
- [ ] `.\build.bat` full chain → BUILD OK
- [ ] `git log --oneline -6` shows 5 commits
- [ ] No placeholder TODO/TBD left in new files: `Grep TODO src/org/swelement/ui/Ast*.java` → 0 matches excluding existing
- [ ] Confirm Ast prefix on all new classes
- [ ] All contrast assertions present where text is painted
