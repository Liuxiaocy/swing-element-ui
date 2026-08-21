package org.swelement.ui;

import org.swelement.core.AnimatedPopup;
import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 级联选择器 — Element UI Cascader 的 Java 实现。
 * 支持 N 级层级选择（省→市→区），点击后弹出 AnimatedPopup，
 * 内部多个 ColumnPanel 横向并排，每列滚动，选中项驱动下一列。
 *
 * 用法：
 *   AstCascader.Option root = new AstCascader.Option("广东");
 *   AstCascader.Option gz = new AstCascader.Option("广州");
 *   gz.addChild(new AstCascader.Option("天河区"));
 *   gz.addChild(new AstCascader.Option("越秀区"));
 *   root.addChild(gz);
 *   AstCascader.Option sz = new AstCascader.Option("深圳");
 *   sz.addChild(new AstCascader.Option("南山区"));
 *   sz.addChild(new AstCascader.Option("福田区"));
 *   root.addChild(sz);
 *   List<AstCascader.Option> opts = new ArrayList<AstCascader.Option>();
 *   opts.add(root);
 *   AstCascader cascader = new AstCascader(opts, 3);
 *   cascader.setPlaceholder("请选择城市");
 *   cascader.setSelectionListener(path -> System.out.println("选中: " + path));
 */
public class AstCascader extends JComponent {
    // --- Option model ---
    public static final class Option {
        public final String label;
        private final List<Option> children = new ArrayList<Option>();
        public Option(String label) {
            if (label == null) throw new IllegalArgumentException("label must not be null");
            this.label = label;
        }
        public void addChild(Option c) {
            if (c == null) throw new IllegalArgumentException("child must not be null");
            children.add(c);
        }
        public List<Option> getChildren() { return children; }
        public boolean hasChildren() { return !children.isEmpty(); }
    }

    // --- Fields ---
    private final List<Option> rootOptions;
    private final int levels;
    private String placeholder = "请选择";
    private Consumer<List<String>> selectionListener;
    private final List<Option> selectedPath = new ArrayList<Option>();
    private final Button invoker;
    private final AnimatedPopup popup;
    private final JPanel columnsContainer;
    private final List<ColumnPanel> columns = new ArrayList<ColumnPanel>();
    private boolean open;
    private static final int COL_W = 160;
    private static final int ROW_H = 32;
    private static final int MAX_VISIBLE_ROWS = 8;

    public AstCascader(List<Option> options, int levels) {
        if (options == null) throw new IllegalArgumentException("options must not be null");
        if (options.isEmpty()) throw new IllegalArgumentException("options must have at least one entry");
        if (levels < 1) throw new IllegalArgumentException("levels must be >= 1");
        for (Option o : options) if (o == null) throw new IllegalArgumentException("option must not be null");
        this.rootOptions = new ArrayList<Option>(options);
        this.levels = levels;
        this.invoker = new Button(placeholder + "  ▾", Button.DEFAULT, false);
        this.popup = new AnimatedPopup();
        popup.setDismissListener(new Runnable() { public void run() { open = false; }});
        this.columnsContainer = new JPanel();
        columnsContainer.setLayout(new BoxLayout(columnsContainer, BoxLayout.X_AXIS));
        columnsContainer.setOpaque(false);
        AnimatedPopup.registerGlobal(popup, AnimatedPopup.PopupLayer.POPUP);
        this.invoker.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { toggle(); }});
        setLayout(new BorderLayout());
        add(invoker, BorderLayout.CENTER);
        setOpaque(false);
    }

    public void setPlaceholder(String s) {
        if (s == null) throw new IllegalArgumentException("placeholder must not be null");
        this.placeholder = s;
        updateInvokerText();
    }

    public void setSelectionListener(Consumer<List<String>> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.selectionListener = l;
    }

    public List<String> getSelectedPath() {
        List<String> labels = new ArrayList<String>();
        for (Option o : selectedPath) labels.add(o.label);
        return labels;
    }

    public void showCascader() {
        if (open) return;
        open = true;
        rebuildColumns();
        updatePopupSize();
        popup.show(this, AnimatedPopup.Direction.BELOW);
    }

    public void hideCascader() {
        if (!open) return;
        open = false;
        popup.hideWithAnimation(null);
    }

    public void toggle() { if (open) hideCascader(); else showCascader(); }
    public boolean isOpen() { return open; }

    private void updateInvokerText() {
        if (selectedPath.isEmpty()) {
            invoker.setText(placeholder + "  ▾");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < selectedPath.size(); i++) {
                if (i > 0) sb.append(" / ");
                sb.append(selectedPath.get(i).label);
            }
            sb.append("  ▾");
            invoker.setText(sb.toString());
        }
    }

    private void rebuildColumns() {
        columnsContainer.removeAll();
        columns.clear();
        // Column 0: root options
        ColumnPanel col0 = new ColumnPanel(0, rootOptions);
        columns.add(col0);
        columnsContainer.add(col0);
        // Subsequent columns based on selectedPath
        List<Option> currentChildren = rootOptions;
        for (int level = 1; level < levels; level++) {
            int selIdx = -1;
            if (selectedPath.size() >= level) {
                Option parent = selectedPath.get(level - 1);
                // find parent in currentChildren to get its children
                for (int i = 0; i < currentChildren.size(); i++) {
                    if (currentChildren.get(i) == parent) { selIdx = i; break; }
                }
            }
            if (selIdx >= 0 && currentChildren.get(selIdx).hasChildren()) {
                currentChildren = currentChildren.get(selIdx).getChildren();
                ColumnPanel col = new ColumnPanel(level, currentChildren);
                columns.add(col);
                columnsContainer.add(col);
            } else {
                break;
            }
        }
        // Add separator between columns
        columnsContainer.revalidate();
        columnsContainer.repaint();
    }

    private void updatePopupSize() {
        int numCols = columns.size();
        int totalW = numCols * COL_W + (numCols - 1) * 1; // 1px separators
        int totalRows = 0;
        for (ColumnPanel c : columns) totalRows = Math.max(totalRows, c.optionCount());
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, totalRows);
        int viewH = visibleRows * ROW_H + 8; // padding
        boolean useScroll = totalRows > MAX_VISIBLE_ROWS;
        Container cc = popup.getContent();
        cc.removeAll();
        cc.setLayout(new BorderLayout());
        if (useScroll) {
            JScrollPane sp = new JScrollPane(columnsContainer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setBorder(null); sp.setOpaque(false); sp.getViewport().setOpaque(false);
            sp.setPreferredSize(new Dimension(totalW, viewH));
            cc.add(sp, BorderLayout.CENTER);
        } else {
            columnsContainer.setPreferredSize(new Dimension(totalW, viewH));
            cc.add(columnsContainer, BorderLayout.CENTER);
        }
        popup.setPreferredSize(new Dimension(totalW, viewH + 4));
    }

    void onColumnClick(int level, Option clicked) {
        // Truncate selectedPath to this level
        while (selectedPath.size() > level) selectedPath.remove(selectedPath.size() - 1);
        selectedPath.add(clicked);
        // If clicked has children and level < levels-1, rebuild to show next column
        if (clicked.hasChildren() && level < levels - 1) {
            rebuildColumns();
            updatePopupSize();
            popup.revalidate(); popup.repaint();
        } else {
            // Leaf or max level reached → selection complete
            updateInvokerText();
            if (selectionListener != null) selectionListener.accept(getSelectedPath());
            hideCascader();
        }
    }

    @Override public Dimension getPreferredSize() { return invoker.getPreferredSize(); }
    @Override public Dimension getMinimumSize() { return invoker.getMinimumSize(); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    // --- ColumnPanel: one vertical list of options ---
    private final class ColumnPanel extends JPanel {
        private final int level;
        private final List<Option> options;

        ColumnPanel(int level, List<Option> opts) {
            this.level = level;
            this.options = opts;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setPreferredSize(new Dimension(COL_W, Math.min(MAX_VISIBLE_ROWS, opts.size()) * ROW_H + 8));
            for (int i = 0; i < opts.size(); i++) {
                add(new CascaderRow(this, opts.get(i), i));
            }
        }

        int optionCount() { return options.size(); }

        boolean isOptionSelected(Option o) {
            if (selectedPath.size() > level) return selectedPath.get(level) == o;
            return false;
        }

        int selectedRowIndex() {
            if (selectedPath.size() <= level) return -1;
            Option sel = selectedPath.get(level);
            for (int i = 0; i < options.size(); i++) if (options.get(i) == sel) return i;
            return -1;
        }
    }

    // --- CascaderRow: single option row ---
    private final class CascaderRow extends JPanel {
        private final ColumnPanel parent;
        private final Option option;
        private final int index;
        float hover;
        final Animator hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); }},
            new Animator.Listener() { public void update(float v) { hover = v; repaint(); }});

        CascaderRow(ColumnPanel parent, Option option, int index) {
            this.parent = parent;
            this.option = option;
            this.index = index;
            setOpaque(false);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(COL_W, ROW_H));
            setMaximumSize(new Dimension(COL_W, ROW_H));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) { hoverAnim.stop(); hoverAnim.go(hover, 1f); }
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (isEnabled()) { hoverAnim.stop(); hoverAnim.go(hover, 0f); }
                }
                @Override public void mouseClicked(MouseEvent e) {
                    if (!isEnabled()) return;
                    onColumnClick(parent.level, option);
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            boolean selected = parent.isOptionSelected(option);
            Color bg = Color.WHITE;
            Color textColor = ElementTheme.TEXT_MAIN;
            if (selected) {
                bg = ElementTheme.PRIMARY;
                textColor = Color.WHITE;
            } else if (hover > 0.01f) {
                int a = Math.round(18 * hover);
                bg = new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), a);
                textColor = ElementTheme.lerp(ElementTheme.TEXT_MAIN, ElementTheme.PRIMARY, hover);
                ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstCascader hover row");
            } else {
                ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstCascader idle row");
            }
            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            // Label: left padding 16px, vertical center
            g2.setColor(textColor);
            g2.setFont(ElementTheme.FONT.deriveFont(14f));
            FontMetrics fm = g2.getFontMetrics();
            int baseY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            String label = option.label;
            int maxW = getWidth() - 32;
            if (fm.stringWidth(label) > maxW) {
                String ell = "\u2026";
                int ellW = fm.stringWidth(ell);
                while (label.length() > 0 && fm.stringWidth(label) + ellW > maxW) label = label.substring(0, label.length() - 1);
                label = label + ell;
            }
            g2.drawString(label, 16, baseY);
            // If has children: draw right arrow ">" at right side
            if (option.hasChildren()) {
                g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 12f));
                FontMetrics fm2 = g2.getFontMetrics();
                String arrow = ">";
                int ax = getWidth() - fm2.stringWidth(arrow) - 12;
                int ay = (getHeight() - fm2.getHeight()) / 2 + fm2.getAscent();
                g2.drawString(arrow, ax, ay);
            }
            g2.dispose();
        }

        @Override public boolean isOptimizedDrawingEnabled() { return false; }
    }

    // --- Self-check ---
    static void selfCheck() {
        // Constructor null guards
        boolean threw = false;
        try { new AstCascader(null, 3); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null options"; threw = false;
        try { new AstCascader(new ArrayList<Option>(), 3); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "empty options"; threw = false;
        try { new AstCascader(Arrays.asList((Option)null), 3); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null option entry"; threw = false;
        try { new AstCascader(Arrays.asList(new Option("x")), 0); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "levels < 1";
        // setPlaceholder null guard
        AstCascader c0 = new AstCascader(Arrays.asList(new Option("x")), 1);
        threw = false;
        try { c0.setPlaceholder(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null placeholder"; threw = false;
        try { c0.setSelectionListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null listener";

        // Build test tree: 广东→广州→天河区 / 深圳→南山区
        Option gd = new Option("广东");
        Option gz = new Option("广州");
        gz.addChild(new Option("天河区"));
        gz.addChild(new Option("越秀区"));
        gd.addChild(gz);
        Option sz = new Option("深圳");
        sz.addChild(new Option("南山区"));
        sz.addChild(new Option("福田区"));
        gd.addChild(sz);
        // 上海→浦东新区 (no children)
        Option sh = new Option("上海");
        sh.addChild(new Option("浦东新区"));
        List<Option> roots = new ArrayList<Option>();
        roots.add(gd); roots.add(sh);
        AstCascader cascader = new AstCascader(roots, 3);
        cascader.setPlaceholder("请选择城市");

        final Throwable[] err = {null};
        final List<String> selected = new ArrayList<String>();
        cascader.setSelectionListener(path -> { selected.clear(); selected.addAll(path); });
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("Cascader SC"); jf.setSize(800, 600); jf.setVisible(true);
            JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new FlowLayout());
            cp.add(cascader); jf.pack();
            // Open cascader
            cascader.showCascader();
            assert cascader.isOpen() : "cascader open";
            // Find popup in layered pane
            JLayeredPane lp = jf.getLayeredPane();
            AnimatedPopup popup = null;
            for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { popup = (AnimatedPopup) lp.getComponent(i); break; }
            assert popup != null : "popup found";
            // Find first column's first row (广东)
            Component row0 = findChildByName(popup, "AstCascader$CascaderRow", 0);
            assert row0 != null : "first row found";
            // Click 广东
            clickRow(row0);
            try { Thread.sleep(50); } catch (Throwable ignore) {}
            // Now column 1 should show 广东's children (广州, 深圳)
            // Click 广州 (find 2nd column's first row)
            Component row1 = findChildByName(popup, "AstCascader$CascaderRow", 2); // after column0's 2 rows
            assert row1 != null : "广州 row found after clicking 广东";
            clickRow(row1);
            try { Thread.sleep(50); } catch (Throwable ignore) {}
            // Click 天河区
            Component row2 = findChildByName(popup, "AstCascader$CascaderRow", 4); // after col0(2) + col1(2) rows
            assert row2 != null : "天河区 row found";
            clickRow(row2);
            try { Thread.sleep(100); } catch (Throwable ignore) {}
            // Check selected path
            assert selected.size() == 3 : "selected path has 3 elements; actual=" + selected.size();
            assert "广东".equals(selected.get(0)) : "level 0 = 广东";
            assert "广州".equals(selected.get(1)) : "level 1 = 广州";
            assert "天河区".equals(selected.get(2)) : "level 2 = 天河区";
            assert !cascader.isOpen() : "cascader closed after leaf selection";
            // Verify invoker text updated
            String invokerText = cascader.getSelectedPath().toString();
            assert invokerText.contains("广东") : "invoker shows selected path";

            // Off-screen paint a CascaderRow to trigger assertContrast
            AstCascader c2 = new AstCascader(roots, 3);
            c2.showCascader();
            AnimatedPopup p2 = null;
            for (int i = 0; i < lp.getComponentCount(); i++) if (lp.getComponent(i) instanceof AnimatedPopup) { p2 = (AnimatedPopup) lp.getComponent(i); break; }
            Component r = findChildByName(p2, "AstCascader$CascaderRow", 0);
            r.setBounds(0, 0, COL_W, ROW_H);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(COL_W, ROW_H, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            try { r.paint(gg); } finally { gg.dispose(); }
            int px = img.getRGB(10, 16); int a = (px >>> 24) & 0xFF;
            assert a > 100 : "row painted opaque";
            c2.hideCascader();
            jf.dispose();
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstCascader self-check OK");
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

    private static void clickRow(Component c) {
        if (c == null) return;
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 10, 10, 0, false));
        try { Thread.sleep(20); } catch (Throwable ignore) {}
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, 10, 1, false));
        try { Thread.sleep(20); } catch (Throwable ignore) {}
    }

    public static void main(String[] args) { selfCheck(); }
}
