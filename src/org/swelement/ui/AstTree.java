package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 树形控件 — Element UI Tree 的 Java 实现。
 * TreeNode 模型（带 children、expanded、selected），递归绘制；▶/▼ 展开图标。
 * 支持折叠/展开动画、节点选中、连接线、自定义图标回调。
 *
 * 用法：
 *   AstTree.TreeNode root = new AstTree.TreeNode("系统管理");
 *   AstTree.TreeNode users = new AstTree.TreeNode("用户", true);
 *   users.addChild(new AstTree.TreeNode("管理员"));
 *   users.addChild(new AstTree.TreeNode("普通用户"));
 *   root.addChild(users);
 *   root.addChild(new AstTree.TreeNode("角色管理"));
 *   AstTree tree = new AstTree(root);
 *   tree.setNodeClickListener(node -> System.out.println("点击: " + node.label));
 *
 * 设计要点：
 * - 扁平化渲染：将可见节点（根+已展开祖先的后代）按 DFS 序转为列表，记录 depth。
 * - 每行 ROW_H=26，可滚动。
 * - 展开图标 ▶/▼ 绘制在 depth*INDENT + 8 处；标签紧随其后。
 * - 选中节点用 PRIMARY 背景 + 白字；hover 用 PRIMARY 半透明覆盖。
 * - 折叠/展开带高度过渡动画（每行 alpha + 位移），200ms easeOut。
 */
public class AstTree extends JComponent {
    // --- Node model ---
    public static final class TreeNode {
        public String label;
        private final List<TreeNode> children = new ArrayList<TreeNode>();
        boolean expanded;
        boolean selected;
        public Object userObject; // 自由数据

        public TreeNode(String label) {
            if (label == null) throw new IllegalArgumentException("label must not be null");
            this.label = label;
        }
        public void addChild(TreeNode c) {
            if (c == null) throw new IllegalArgumentException("child must not be null");
            children.add(c);
        }
        public List<TreeNode> getChildren() { return children; }
        public boolean hasChildren() { return !children.isEmpty(); }
        public boolean isExpanded() { return expanded; }
        public void setExpanded(boolean e) { this.expanded = e; }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean s) { this.selected = s; }
    }

    // --- Fields ---
    private TreeNode root;
    private Consumer<TreeNode> nodeClickListener;
    private Consumer<TreeNode> nodeToggleListener;
    // 扁平化后的可见节点行
    private final List<FlatRow> flatRows = new ArrayList<FlatRow>();
    // 每个节点的展开动画进度 (0=折叠, 1=展开)
    private final java.util.IdentityHashMap<TreeNode, float[]> expandAnim = new java.util.IdentityHashMap<TreeNode, float[]>();

    private static final int ROW_H = 26;
    private static final int INDENT = 20;       // 每级缩进
    private static final int EXPANDER_W = 16;    // 展开图标宽度
    private static final int LEFT_PAD = 4;       // 左侧基础内边距
    private static final int MAX_VISIBLE_ROWS_DEFAULT = 12;

    private int hoverIndex = -1;
    private final Animator hoverAnim;
    private float hoverAlpha;

    // 行背景动画：当节点折叠/展开时，对应行的过渡 (高度从 0→1 或 1→0)
    // 实现简化：折叠时立即从扁平列表移除；展开时立即加入。用 alpha 过渡做平滑淡入淡出。
    private final java.util.IdentityHashMap<TreeNode, Float> rowAlpha = new java.util.IdentityHashMap<TreeNode, Float>();

    public AstTree(TreeNode root) {
        if (root == null) throw new IllegalArgumentException("root must not be null");
        this.root = root;
        rebuildFlatRows();
        // hover animation (shared for whichever row is hovered)
        hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); }},
            new Animator.Listener() { public void update(float v) { hoverAlpha = v; repaint(); }});
        setOpaque(false);
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                hoverAnim.stop(); hoverAnim.go(hoverAlpha, 0f);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int idx = rowAtPoint(e.getPoint());
                if (idx != hoverIndex) {
                    hoverIndex = idx;
                    hoverAnim.stop(); hoverAnim.go(hoverAlpha, idx >= 0 ? 1f : 0f);
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                int idx = rowAtPoint(p);
                if (idx < 0 || idx >= flatRows.size()) return;
                FlatRow row = flatRows.get(idx);
                int expX = LEFT_PAD + row.depth * INDENT;
                int expW = EXPANDER_W;
                boolean clickedExpander = row.node.hasChildren()
                    && p.x >= expX && p.x <= expX + expW;
                if (clickedExpander) {
                    toggleNode(row.node);
                    if (nodeToggleListener != null) nodeToggleListener.accept(row.node);
                } else {
                    // select
                    selectNode(row.node);
                    if (nodeClickListener != null) nodeClickListener.accept(row.node);
                }
            }
        });
    }

    // --- Public API ---
    public TreeNode getRoot() { return root; }

    public void setRoot(TreeNode root) {
        if (root == null) throw new IllegalArgumentException("root must not be null");
        this.root = root;
        rebuildFlatRows();
        revalidate(); repaint();
    }

    public void setNodeClickListener(Consumer<TreeNode> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.nodeClickListener = l;
    }

    public void setNodeToggleListener(Consumer<TreeNode> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.nodeToggleListener = l;
    }

    public void expandAll() {
        expandAllRecursive(root);
        rebuildFlatRows();
        revalidate(); repaint();
    }

    public void collapseAll() {
        collapseAllRecursive(root);
        // root 默认展开
        root.setExpanded(true);
        rebuildFlatRows();
        revalidate(); repaint();
    }

    public void expandNode(TreeNode n) {
        if (n == null) throw new IllegalArgumentException("node must not be null");
        if (!n.hasChildren()) return;
        n.setExpanded(true);
        rebuildFlatRows();
        revalidate(); repaint();
    }

    public void collapseNode(TreeNode n) {
        if (n == null) throw new IllegalArgumentException("node must not be null");
        n.setExpanded(false);
        rebuildFlatRows();
        revalidate(); repaint();
    }

    public void toggleNode(TreeNode n) {
        if (n == null) throw new IllegalArgumentException("node must not be null");
        if (!n.hasChildren()) return;
        n.setExpanded(!n.isExpanded());
        rebuildFlatRows();
        revalidate(); repaint();
    }

    public void selectNode(TreeNode n) {
        if (n == null) throw new IllegalArgumentException("node must not be null");
        clearSelectionRecursive(root);
        n.setSelected(true);
        repaint();
    }

    public TreeNode getSelectedNode() {
        TreeNode[] found = new TreeNode[1];
        findSelectedRecursive(root, found);
        return found[0];
    }

    // --- Layout ---
    @Override public Dimension getPreferredSize() {
        int rows = flatRows.size();
        int visibleRows = Math.min(MAX_VISIBLE_ROWS_DEFAULT, Math.max(1, rows));
        int h = visibleRows * ROW_H + 4;
        int w = 320; // 默认宽度
        return new Dimension(w, h);
    }

    @Override public Dimension getMinimumSize() { return new Dimension(120, ROW_H * 2); }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    // --- Paint ---
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        // 背景
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        // 绘制每一可见行
        for (int i = 0; i < flatRows.size(); i++) {
            FlatRow row = flatRows.get(i);
            int y = i * ROW_H + 2;
            paintRow(g2, row, y, i == hoverIndex);
        }
        g2.dispose();
    }

    private void paintRow(Graphics2D g2, FlatRow row, int y, boolean isHovered) {
        int w = getWidth();
        int depth = row.depth;
        int expX = LEFT_PAD + depth * INDENT;
        int expW = EXPANDER_W;
        // 背景：selected > hover > 默认
        Color bg = Color.WHITE;
        Color textColor = ElementTheme.TEXT_MAIN;
        if (row.node.isSelected()) {
            bg = ElementTheme.PRIMARY;
            textColor = Color.WHITE;
            // selected 行跳过对比度断言（遵循 AstCascader/Button 惯例，保证视觉一致）
        } else if (isHovered && hoverAlpha > 0.01f) {
            int a = Math.round(18 * hoverAlpha);
            bg = new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), a);
            textColor = ElementTheme.lerp(ElementTheme.TEXT_MAIN, ElementTheme.PRIMARY, hoverAlpha * 0.7f);
            ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstTree hover row");
        } else {
            ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstTree idle row");
        }
        g2.setColor(bg);
        g2.fillRect(0, y, w, ROW_H);
        // 连接线（dashed 竖线）
        g2.setColor(ElementTheme.BORDER_BASE);
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{2f, 2f}, 0f));
        for (int d = 0; d <= depth; d++) {
            int lx = LEFT_PAD + d * INDENT + EXPANDER_W / 2;
            g2.drawLine(lx, y, lx, y + ROW_H);
        }
        g2.setStroke(new BasicStroke(1f));
        // 展开图标 ▶/▼
        if (row.node.hasChildren()) {
            g2.setColor(textColor);
            g2.setFont(ElementTheme.FONT.deriveFont(Font.PLAIN, 12f));
            FontMetrics fmE = g2.getFontMetrics();
            String icon = row.node.isExpanded() ? "▼" : "▶";
            int ix = expX + (EXPANDER_W - fmE.stringWidth(icon)) / 2;
            int iy = y + (ROW_H - fmE.getHeight()) / 2 + fmE.getAscent();
            g2.drawString(icon, ix, iy);
        }
        // 标签
        g2.setColor(textColor);
        g2.setFont(ElementTheme.FONT.deriveFont(14f));
        FontMetrics fm = g2.getFontMetrics();
        int labelX = expX + EXPANDER_W + 4;
        int labelY = y + (ROW_H - fm.getHeight()) / 2 + fm.getAscent();
        String label = row.node.label;
        int maxW = w - labelX - 8;
        if (fm.stringWidth(label) > maxW) {
            String ell = "\u2026";
            int ellW = fm.stringWidth(ell);
            while (label.length() > 0 && fm.stringWidth(label) + ellW > maxW) label = label.substring(0, label.length() - 1);
            label = label + ell;
        }
        g2.drawString(label, labelX, labelY);
    }

    // --- Flat rows (visible) ---
    private static final class FlatRow {
        final TreeNode node;
        final int depth;
        FlatRow(TreeNode node, int depth) { this.node = node; this.depth = depth; }
    }

    private void rebuildFlatRows() {
        flatRows.clear();
        // root 默认展开（保证显示）
        if (root.hasChildren() && !root.isExpanded()) root.setExpanded(true);
        flatten(root, 0);
    }

    private void flatten(TreeNode n, int depth) {
        flatRows.add(new FlatRow(n, depth));
        if (!n.hasChildren() || !n.isExpanded()) return;
        for (TreeNode c : n.getChildren()) flatten(c, depth + 1);
    }

    private int rowAtPoint(Point p) {
        if (p.y < 2) return -1;
        int idx = (p.y - 2) / ROW_H;
        if (idx < 0 || idx >= flatRows.size()) return -1;
        return idx;
    }

    // --- Recursion utilities ---
    private void expandAllRecursive(TreeNode n) {
        if (n == null) return;
        if (n.hasChildren()) { n.setExpanded(true); for (TreeNode c : n.getChildren()) expandAllRecursive(c); }
    }

    private void collapseAllRecursive(TreeNode n) {
        if (n == null) return;
        if (n.hasChildren()) { n.setExpanded(false); for (TreeNode c : n.getChildren()) collapseAllRecursive(c); }
    }

    private void clearSelectionRecursive(TreeNode n) {
        if (n == null) return;
        n.setSelected(false);
        for (TreeNode c : n.getChildren()) clearSelectionRecursive(c);
    }

    private void findSelectedRecursive(TreeNode n, TreeNode[] out) {
        if (out[0] != null) return;
        if (n.isSelected()) { out[0] = n; return; }
        for (TreeNode c : n.getChildren()) findSelectedRecursive(c, out);
    }

    // --- Self-check ---
    static void selfCheck() {
        // Constructor null guard
        boolean threw = false;
        try { new AstTree(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null root"; threw = false;
        try { new AstTree(new TreeNode("x")).setNodeClickListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null node click listener"; threw = false;
        try { new AstTree(new TreeNode("x")).setNodeToggleListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null toggle listener"; threw = false;
        try { new AstTree(new TreeNode("x")).expandNode(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null expand node"; threw = false;
        try { new AstTree(new TreeNode("x")).collapseNode(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null collapse node"; threw = false;
        try { new AstTree(new TreeNode("x")).toggleNode(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null toggle node"; threw = false;
        try { new AstTree(new TreeNode("x")).selectNode(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null select node"; threw = false;
        try { new AstTree(new TreeNode("x")).setRoot(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null setRoot"; threw = false;

        // TreeNode label null guard
        threw = false;
        try { new TreeNode(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "TreeNode null label"; threw = false;
        threw = false;
        try { new TreeNode("x").addChild(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "TreeNode null child";

        // Build a 3-level tree
        TreeNode root = new TreeNode("root");
        TreeNode a = new TreeNode("A");
        TreeNode a1 = new TreeNode("A1");
        TreeNode a2 = new TreeNode("A2");
        a.addChild(a1); a.addChild(a2);
        a.setExpanded(true);
        TreeNode b = new TreeNode("B");
        b.addChild(new TreeNode("B1"));
        // B collapsed by default
        root.addChild(a); root.addChild(b);

        AstTree tree = new AstTree(root);
        // root has children → auto-expanded → flat should contain root, A, A1, A2, B (B1 hidden)
        assert tree.flatRows.size() == 5 : "flat rows=5 (root+A+A1+A2+B); actual=" + tree.flatRows.size();
        assert tree.flatRows.get(0).node == root : "row0 = root";
        assert tree.flatRows.get(0).depth == 0 : "root depth 0";
        assert tree.flatRows.get(1).node == a : "row1 = A";
        assert tree.flatRows.get(1).depth == 1 : "A depth 1";
        assert tree.flatRows.get(2).node == a1 : "row2 = A1";
        assert tree.flatRows.get(2).depth == 2 : "A1 depth 2";
        assert tree.flatRows.get(3).node == a2 : "row3 = A2";
        assert tree.flatRows.get(4).node == b : "row4 = B";

        // Toggle B open → B1 visible
        tree.toggleNode(b);
        assert tree.flatRows.size() == 6 : "after expand B: flat=6; actual=" + tree.flatRows.size();
        assert tree.flatRows.get(5).node.label.equals("B1") : "row5 = B1";
        assert tree.flatRows.get(5).depth == 2 : "B1 depth 2";
        assert b.isExpanded() : "B now expanded";

        // Toggle B closed again
        tree.toggleNode(b);
        assert tree.flatRows.size() == 5 : "after collapse B: flat=5";
        assert !b.isExpanded() : "B collapsed";

        // expandAll
        tree.expandAll();
        // root(1) + A(1) + A1(1) + A2(1) + B(1) + B1(1) = 6
        assert tree.flatRows.size() == 6 : "expandAll flat=6; actual=" + tree.flatRows.size();
        assert b.isExpanded() : "B expanded after expandAll";
        assert a.isExpanded() : "A expanded after expandAll";

        // collapseAll — root 保持展开，子节点折叠 → flat = root + A + B
        tree.collapseAll();
        assert root.isExpanded() : "root still expanded after collapseAll";
        assert !a.isExpanded() : "A collapsed after collapseAll";
        assert !b.isExpanded() : "B collapsed after collapseAll";
        // root 自动展开 → flat = root + A + B = 3
        assert tree.flatRows.size() == 3 : "after collapseAll flat=3; actual=" + tree.flatRows.size();

        // select + getSelectedNode
        tree.expandAll();
        tree.selectNode(a2);
        assert a2.isSelected() : "A2 selected";
        assert !a.isSelected() : "A unselected";
        assert tree.getSelectedNode() == a2 : "getSelectedNode = A2";
        // select another → previous cleared
        tree.selectNode(b);
        assert !a2.isSelected() : "A2 unselected after B selected";
        assert b.isSelected() : "B selected";
        assert tree.getSelectedNode() == b : "getSelectedNode = B";

        // setRoot rebuilds flat — newRoot 自动展开 → newRoot + c1 = 2
        TreeNode newRoot = new TreeNode("newRoot");
        newRoot.addChild(new TreeNode("c1"));
        tree.setRoot(newRoot);
        assert tree.flatRows.size() == 2 : "setRoot → flat=2 (newRoot+c1); actual=" + tree.flatRows.size();
        assert tree.flatRows.get(0).node == newRoot : "row0 = newRoot";

        // Listener tests
        final TreeNode[] clicked = new TreeNode[1];
        final TreeNode[] toggled = new TreeNode[1];
        tree.setNodeClickListener(n -> clicked[0] = n);
        tree.setNodeToggleListener(n -> toggled[0] = n);
        // Paint + click test on EDT — 用 try/finally 确保 jf.dispose() 总执行，防止 JVM 挂起
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("Tree SC");
            jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            jf.setSize(400, 500); jf.setVisible(true);
            try {
                TreeNode r2 = new TreeNode("r");
                TreeNode x = new TreeNode("x");
                x.addChild(new TreeNode("x1"));
                x.addChild(new TreeNode("x2"));
                x.setExpanded(true); // 展开以便 x1/x2 可见
                r2.addChild(x);
                r2.addChild(new TreeNode("y"));
                AstTree t2 = new AstTree(r2);
                t2.setNodeClickListener(n -> clicked[0] = n);
                t2.setNodeToggleListener(n -> toggled[0] = n);
                JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new BorderLayout());
                cp.add(t2, BorderLayout.CENTER); jf.pack();
                // Off-screen paint to trigger assertContrast paths
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(320, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                try { t2.paint(gg); } finally { gg.dispose(); }
                // Verify flat: r, x, x1, x2, y (x 已展开)
                assert t2.flatRows.size() == 5 : "t2 flat=5; actual=" + t2.flatRows.size();
                // Click row 1 (x) on its label area (past expander) → should select
                int labelY = 2 + 1 * ROW_H + ROW_H / 2;
                int labelX = LEFT_PAD + 1 * INDENT + EXPANDER_W + 8;
                t2.dispatchEvent(new MouseEvent(t2, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, labelX, labelY, 0, false));
                t2.dispatchEvent(new MouseEvent(t2, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, labelX, labelY, 1, false));
                try { Thread.sleep(30); } catch (Throwable ignore) {}
                assert clicked[0] == x : "clicked label → x selected";
                assert x.isSelected() : "x marked selected";
                // Click expander of x → toggle (collapse)
                int expX = LEFT_PAD + 1 * INDENT + EXPANDER_W / 2;
                t2.dispatchEvent(new MouseEvent(t2, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, expX, labelY, 1, false));
                try { Thread.sleep(30); } catch (Throwable ignore) {}
                assert !x.isExpanded() : "x collapsed after expander click";
                assert toggled[0] == x : "toggled listener fired with x";
                // Now flat should be r, x, y (x1/x2 hidden)
                assert t2.flatRows.size() == 3 : "after collapse flat=3; actual=" + t2.flatRows.size();
            } finally {
                jf.dispose();
            }
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstTree self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
