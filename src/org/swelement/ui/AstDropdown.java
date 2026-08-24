package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.AnimatedPopup;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * 下拉菜单组件 — 包装一个按钮作为 invoker，点击展开/收起菜单项。
 * 支持 BELOW/ABOVE/LEFT/RIGHT 四个方向（默认 BELOW）。
 * 菜单项支持 hover 高亮（Animator），8 项以内显示为纯列表；≥9 项自动包裹 JScrollPane。
 * 用法：
 *   AstDropdown.Item[] items = new AstDropdown.Item[]{
 *       new AstDropdown.Item("添加", e -> addAction()),
 *       new AstDropdown.Item("删除", e -> deleteAction()),
 *       new AstDropdown.Item("导出", null),
 *   };
 *   AstDropdown dd = new AstDropdown("操作菜单", items);
 *   frame.add(dd);
 */
public class AstDropdown extends JComponent {
    public static final class Item {
        public final String label;
        public final ActionListener action;
        public Item(String label, ActionListener action) {
            if (label == null) throw new IllegalArgumentException("label must not be null");
            this.label = label; this.action = action;
        }
    }

    private final AstButton invoker;
    private final AnimatedPopup popup;
    private final AnimatedPopup.Direction dir;
    private final Item[] items;
    private final JPanel scrollView; // viewport (Box Y layout of ItemRows)
    private final ArrayList<ItemRow> rows;
    private boolean open;
    private static final int ROW_H = 36;
    private static final int MAX_VISIBLE = 8;

    public AstDropdown(String invokerLabel, Item[] items) { this(invokerLabel, items, AnimatedPopup.Direction.BELOW); }

    public AstDropdown(String invokerLabel, Item[] items, AnimatedPopup.Direction dir) {
        if (invokerLabel == null) throw new IllegalArgumentException("invokerLabel must not be null");
        if (items == null) throw new IllegalArgumentException("items array must not be null");
        if (items.length == 0) throw new IllegalArgumentException("items array must have at least one entry");
        if (dir == null) throw new IllegalArgumentException("direction must not be null");
        for (Item it : items) if (it == null) throw new IllegalArgumentException("item must not be null");
        this.items = items.clone(); // defensive copy
        this.dir = dir;
        this.invoker = new AstButton(invokerLabel + "  ▾", AstButton.PRIMARY, false);
        this.popup = new AnimatedPopup();
        popup.setDismissListener(new Runnable() { public void run() { open = false; }});
        this.scrollView = new JPanel();
        scrollView.setLayout(new BoxLayout(scrollView, BoxLayout.Y_AXIS));
        scrollView.setOpaque(false);
        this.rows = new ArrayList<ItemRow>(items.length);
        for (Item it : items) {
            ItemRow row = new ItemRow(it);
            rows.add(row);
            scrollView.add(row);
            // separator: thin BORDER_BASE line between rows
            if (rows.size() < items.length) scrollView.add(new Separator());
        }
        AnimatedPopup.registerGlobal(popup, AnimatedPopup.PopupLayer.POPUP);
        // Invoker click = toggle
        final ActionListener toggleL = new ActionListener() { public void actionPerformed(ActionEvent e) { toggle(); }};
        this.invoker.addActionListener(toggleL);
        // Layout: invoker fills AstDropdown's size; simple BorderLayout; AstDropdown preferred size = button's pref
        setLayout(new BorderLayout());
        add(invoker, BorderLayout.CENTER);
        setOpaque(false);
    }

    public void showDropdown() {
        if (open) return;
        open = true;
        updatePopupPrefSize();
        popup.show(this, dir);
    }

    public void hideDropdown() {
        if (!open) return;
        open = false;
        popup.hideWithAnimation(null);
    }

    public void toggle() { if (open) hideDropdown(); else showDropdown(); }

    public boolean isOpen() { return open; }

    public void setInvokerText(String s) {
        if (s == null) throw new IllegalArgumentException("invoker text must not be null");
        invoker.setText(s + "  ▾");
    }

    public String getInvokerText() {
        String raw = invoker.getText();
        int idx = raw.lastIndexOf("  ▾");
        return idx == -1 ? raw : raw.substring(0, idx);
    }

    @Override public Dimension getPreferredSize() { return invoker.getPreferredSize(); }
    @Override public Dimension getMinimumSize() { return invoker.getMinimumSize(); }

    private void updatePopupPrefSize() {
        int invokerW = Math.max(140, getWidth());
        invokerW = Math.min(360, invokerW);
        int totalRows = rows.size();
        // add separators: totalRows - 1 of 1px each
        int separators = Math.max(0, totalRows - 1);
        int viewH = totalRows * ROW_H + separators;
        boolean useScroll = totalRows > MAX_VISIBLE;
        int visibleH = useScroll ? (MAX_VISIBLE * ROW_H + (MAX_VISIBLE-1)) : viewH;
        // popup content: if scroll wrap scrollView in JScrollPane; if not, add scrollView directly.
        Container cc = popup.getContent();
        cc.removeAll();
        if (useScroll) {
            JScrollPane sp = new JScrollPane(scrollView, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setBorder(null); sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setPreferredSize(new Dimension(invokerW, visibleH));
            cc.add(sp, BorderLayout.CENTER);
        } else {
            scrollView.setPreferredSize(new Dimension(invokerW, visibleH));
            cc.add(scrollView, BorderLayout.CENTER);
        }
        popup.setPreferredSize(new Dimension(invokerW, visibleH + 4));
    }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    private final class Separator extends JComponent {
        Separator() { setOpaque(false); }
        @Override public Dimension getPreferredSize() { return new Dimension(10, 1); }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 1); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(ElementTheme.BORDER_BASE);
            g2.drawLine(12, 0, Math.max(13, getWidth()-12), 0);
            g2.dispose();
        }
    }

    private final class ItemRow extends JPanel {
        final Item item;
        float hover;
        final Animator hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); }},
            new Animator.Listener() { public void update(float v) { hover = v; repaint(); }});

        ItemRow(final Item item) {
            this.item = item;
            setOpaque(false);
            setLayout(new BorderLayout());
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) { hoverAnim.stop(); hoverAnim.go(hover, 1f); }
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (isEnabled()) { hoverAnim.stop(); hoverAnim.go(hover, 0f); }
                }
                @Override public void mousePressed(MouseEvent e) {
                    if (!isEnabled()) return;
                    if (!open) return; // 已关闭/关闭动画中的陈旧行不再触发（快速连点只生效一次）
                    if (item.action != null) item.action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, item.label));
                    // Clicked an item → close dropdown
                    hideDropdown();
                }
            });
            // Label: left-padding via EmptyBorder
            JLabel lbl = new JLabel(item.label);
            lbl.setFont(ElementTheme.FONT.deriveFont(14f));
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            add(lbl, BorderLayout.CENTER);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(Math.max(140, super.getPreferredSize().width), ROW_H); }
        @Override public Dimension getMinimumSize() { return new Dimension(100, ROW_H); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = Color.WHITE;
            Color textColor = ElementTheme.TEXT_MAIN;
            if (hover > 0.01f) {
                // Primary tinted background, alpha based on hover
                int a = Math.round(18 * hover);
                bg = new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), a);
                // Blend text color toward PRIMARY by hover
                textColor = ElementTheme.lerp(ElementTheme.TEXT_MAIN, ElementTheme.PRIMARY, hover);
            }
            ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstDropdown.ItemRow idle");
            ElementTheme.assertContrast(textColor, Color.WHITE, "AstDropdown.ItemRow hover-text");
            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            // Update JLabel foreground so text matches hover lerp
            if (getComponentCount() > 0 && getComponent(0) instanceof JLabel) {
                ((JLabel) getComponent(0)).setForeground(textColor);
            }
            g2.dispose();
        }

        @Override public boolean isOptimizedDrawingEnabled() { return false; }
    }

    private static Component findChildByName(Container c, String namePart, int idx) {
        if (c == null) return null;
        java.util.ArrayList<Component> out = new java.util.ArrayList<Component>();
        java.util.Queue<Container> q = new java.util.LinkedList<Container>(); q.add(c);
        while (!q.isEmpty()) {
            Container cur = q.poll();
            for (int i = 0; i < cur.getComponentCount(); i++) {
                Component ch = cur.getComponent(i);
                if (ch.getClass().getName().contains(namePart)) out.add(ch);
                if (ch instanceof Container) q.add((Container) ch);
            }
        }
        return idx < out.size() ? out.get(idx) : null;
    }

    private static Component findChildByText(Container c, String text) {
        if (c == null || text == null) return null;
        java.util.Queue<Container> q = new java.util.LinkedList<Container>(); q.add(c);
        while (!q.isEmpty()) {
            Container cur = q.poll();
            for (int i = 0; i < cur.getComponentCount(); i++) {
                Component ch = cur.getComponent(i);
                if (ch instanceof JLabel) {
                    if (text.equals(((JLabel) ch).getText())) return ch;
                } else if (ch instanceof AstButton) {
                    if (text.equals(((AstButton) ch).getText())) return ch;
                } else if (ch instanceof AbstractButton) {
                    if (text.equals(((AbstractButton) ch).getText())) return ch;
                }
                if (ch instanceof Container) q.add((Container) ch);
            }
        }
        return null;
    }

    static void selfCheck() {
        // Constructor null guards
        boolean threw = false;
        try { new AstDropdown(null, new Item[]{ new Item("a", null) }); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null invokerLabel must throw"; threw = false;
        try { new AstDropdown("x", null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null items array must throw"; threw = false;
        try { new AstDropdown("x", new Item[0]); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "empty items array must throw"; threw = false;
        try { new AstDropdown("x", new Item[]{ new Item("a", null) }, null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null dir must throw"; threw = false;
        try { new AstDropdown("x", new Item[]{ null }); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null item must throw";
        // setInvokerText/getInvokerText roundtrip
        AstDropdown dd0 = new AstDropdown("初始", new Item[]{ new Item("i", null) });
        assert "初始".equals(dd0.getInvokerText()) : "getInvokerText incorrect";
        dd0.setInvokerText("新名称");
        assert "新名称".equals(dd0.getInvokerText()) : "setInvokerText incorrect";

        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("AstDropdown SC"); jf.setSize(600, 500); jf.setVisible(true);
            final int[] fireCount = {0};
            AstDropdown dd = new AstDropdown("菜单", new Item[]{
                new Item("操作1", new ActionListener() { public void actionPerformed(ActionEvent e) { fireCount[0]++; }}),
                new Item("操作2", null)
            });
            JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new FlowLayout()); cp.add(dd); jf.pack();
            dd.showDropdown();
            assert dd.isOpen() : "showDropdown should open";
            JLayeredPane lp = jf.getLayeredPane();
            AnimatedPopup popup = null;
            for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { popup = (AnimatedPopup) lp.getComponent(i); break; }
            assert popup != null : "popup found after showDropdown";
            Component row = findChildByName(popup, "AstDropdown$ItemRow", 0);
            assert row != null : "row found";
            row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, row.getWidth()/2, row.getHeight()/2, 1, false));
            try { Thread.sleep(20); } catch (Throwable ignore) {}
            row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, row.getWidth()/2, row.getHeight()/2, 1, false));
            try { Thread.sleep(40); } catch (Throwable ignore) {}
            assert fireCount[0] == 1 : "action fired once; actual="+fireCount[0];
            assert !dd.isOpen() : "after click dropdown closes";

            // Long scrollable dropdown: 10 items → popup uses JScrollPane scrollView child
            Item[] many = new Item[10];
            for (int i = 0; i < many.length; i++) many[i] = new Item("Item-"+i, null);
            AstDropdown ddLong = new AstDropdown("Long", many);
            JFrame jf2 = new JFrame(); jf2.setSize(600, 500); jf2.getContentPane().setLayout(new FlowLayout());
            jf2.getContentPane().add(ddLong); jf2.pack(); jf2.setVisible(true);
            ddLong.showDropdown();
            lp = jf2.getLayeredPane();
            AnimatedPopup popup2 = null;
            for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { popup2 = (AnimatedPopup) lp.getComponent(i); break; }
            assert popup2 != null : "long popup found";
            // popup2.content → first component should be JScrollPane (scroll > MAX_VISIBLE rows)
            boolean hasScrollPane = findChildByName(popup2, "JScrollPane", 0) != null;
            assert hasScrollPane : "10-item dropdown should use JScrollPane";

            // Off-screen paint an ItemRow to trigger assertContrast
            AstDropdown shortDd = new AstDropdown("T", new Item[]{ new Item("abcdefg", null) });
            JPanel fakeView = new JPanel(); fakeView.setLayout(new BoxLayout(fakeView, BoxLayout.Y_AXIS));
            ItemRow r = shortDd.new ItemRow(new Item("Short Label", null));
            r.setBounds(0, 0, 240, ROW_H);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(240, ROW_H, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            try { r.paint(gg); } finally { gg.dispose(); }
            int px = img.getRGB(10, 18); int alphaPx = (px >>> 24) & 0xFF;
            assert alphaPx > 100 : "row painted opaque enough";
            // hover row paint — force hover via manual animator go
            r.hoverAnim.stop(); r.hoverAnim.go(0, 1f);
            // run internal timer by waiting for anim frames
            try { Thread.sleep(220); } catch (Throwable ignore) {}
            img = new java.awt.image.BufferedImage(240, ROW_H, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            gg = img.createGraphics();
            try { r.paint(gg); } finally { gg.dispose(); }
            int hoverPx = img.getRGB(120, 18); // center
            int hp = (hoverPx >>> 24) & 0xFF;
            assert hp > 100 : "hover row still opaque";
            dd.hideDropdown();
            ddLong.hideDropdown();
            jf.dispose(); jf2.dispose();
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstDropdown self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
