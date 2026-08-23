package org.swelement.ui;

import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Transfer 穿梭框 — 双列多选转移组件，左列为可选数据，右列为已选数据。
 *
 * 用法：
 *   List<AstTransfer.Item> data = new ArrayList<>();
 *   data.add(new AstTransfer.Item("1", "选项一"));
 *   data.add(new AstTransfer.Item("2", "选项二"));
 *   AstTransfer tf = new AstTransfer(data);
 *   tf.setChangeListener(selected -> System.out.println("已选 " + selected.size() + " 项"));
 *   frame.add(tf);
 *
 *   // 预设已选：
 *   tf.setSelectedKeys(Arrays.asList("2"));
 *
 * 设计：
 *  - 左面板：标题"列表" + 选项列表（JList，每项带勾选框），底部计数 X/Y。
 *  - 中间：垂直排列两个按钮"→"（移到右）、"←"（移到左）。
 *  - 右面板：标题"已选" + 已选项列表 + 底部计数。
 *  - 勾选框：自定义 CellRenderer 显示 □/☑ + label；选中项 PRIMARY 高亮。
 *  - 过滤：setFilterable(true) 后标题下方出现搜索框（简化：直接 JTextField + 过滤）。
 *  - 对比度：面板标题 TEXT_MAIN 白底；计数 TEXT_SECONDARY 白底；勾选框图标 TEXT_REGULAR。
 */
public class AstTransfer extends JComponent {
    public static final class Item {
        public final String key;
        public final String label;
        public Item(String key, String label) {
            if (key == null) throw new IllegalArgumentException("key must not be null");
            if (label == null) throw new IllegalArgumentException("label must not be null");
            this.key = key; this.label = label;
        }
    }

    private final java.util.LinkedHashMap<String, Item> allItems = new java.util.LinkedHashMap<String, Item>();
    private final java.util.LinkedHashSet<String> rightKeys = new java.util.LinkedHashSet<String>();
    // 临时勾选（待转移）
    private final java.util.LinkedHashSet<String> leftChecked = new java.util.LinkedHashSet<String>();
    private final java.util.LinkedHashSet<String> rightChecked = new java.util.LinkedHashSet<String>();
    private Consumer<List<Item>> changeListener;
    private boolean filterable = false;
    private String leftFilter = "";
    private String rightFilter = "";

    private JList<String> leftList, rightList;
    private DefaultListModel<String> leftModel, rightModel;
    private JLabel leftCount, rightCount;
    private JTextField leftSearch, rightSearch;

    public AstTransfer(List<Item> items) {
        if (items == null) throw new IllegalArgumentException("items must not be null");
        for (Item it : items) {
            if (it == null) throw new IllegalArgumentException("item must not be null");
            allItems.put(it.key, it);
        }
        setOpaque(false);
        setLayout(new BorderLayout(8, 0));
        add(buildLeftPanel(), BorderLayout.WEST);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);
        refreshModels();
    }

    public void setChangeListener(Consumer<List<Item>> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.changeListener = l;
    }

    public void setFilterable(boolean f) {
        this.filterable = f;
        leftSearch.setVisible(f);
        rightSearch.setVisible(f);
        revalidate(); repaint();
    }

    public boolean isFilterable() { return filterable; }

    public void setSelectedKeys(java.util.Collection<String> keys) {
        if (keys == null) throw new IllegalArgumentException("keys must not be null");
        rightKeys.clear();
        for (String k : keys) {
            if (k != null && allItems.containsKey(k)) rightKeys.add(k);
        }
        leftChecked.clear(); rightChecked.clear();
        refreshModels();
        fireChange();
    }

    public List<Item> getSelected() {
        List<Item> out = new ArrayList<Item>();
        for (String k : rightKeys) out.add(allItems.get(k));
        return out;
    }

    public List<String> getSelectedKeys() {
        List<String> out = new ArrayList<String>();
        out.addAll(rightKeys);
        return out;
    }

    private JComponent buildLeftPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(220, 240));
        p.setBorder(new EmptyBorder(0, 0, 0, 0));
        p.add(makeHeader("列表"), BorderLayout.NORTH);
        leftSearch = new JTextField();
        leftSearch.setFont(ElementTheme.FONT.deriveFont(13f));
        leftSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ElementTheme.BORDER_BASE),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        leftSearch.setVisible(false);
        leftSearch.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            leftFilter = leftSearch.getText().trim().toLowerCase();
            refreshModels();
        }});
        p.add(leftSearch, BorderLayout.NORTH); // 第二个 NORTH 会替换 header，改用中间
        // 修正：用 BoxLayout NORTH 区域堆叠 header+search
        JPanel top = new JPanel(); top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS)); top.setOpaque(false);
        top.add(makeHeader("列表"));
        top.add(leftSearch);
        p.removeAll();
        p.add(top, BorderLayout.NORTH);
        leftModel = new DefaultListModel<String>();
        leftList = new JList<String>(leftModel);
        leftList.setCellRenderer(new TransferCellRenderer(true));
        leftList.setFont(ElementTheme.FONT.deriveFont(13f));
        leftList.setBackground(Color.WHITE);
        leftList.setSelectionBackground(ElementTheme.FILL_BASE);
        leftList.setSelectionForeground(ElementTheme.TEXT_MAIN);
        leftList.setVisibleRowCount(8);
        leftList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                int idx = leftList.locationToIndex(e.getPoint());
                if (idx < 0) return;
                String key = leftModel.get(idx);
                toggleSet(leftChecked, key);
                leftList.repaint();
            }
        });
        JScrollPane sp = new JScrollPane(leftList);
        sp.setBorder(BorderFactory.createLineBorder(ElementTheme.BORDER_BASE));
        p.add(sp, BorderLayout.CENTER);
        leftCount = new JLabel("0 / 0");
        leftCount.setFont(ElementTheme.FONT.deriveFont(12f));
        leftCount.setForeground(ElementTheme.TEXT_REGULAR);
        leftCount.setBorder(new EmptyBorder(4, 4, 0, 0));
        p.add(leftCount, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(220, 240));
        JPanel top = new JPanel(); top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS)); top.setOpaque(false);
        top.add(makeHeader("已选"));
        rightSearch = new JTextField();
        rightSearch.setFont(ElementTheme.FONT.deriveFont(13f));
        rightSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ElementTheme.BORDER_BASE),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        rightSearch.setVisible(false);
        rightSearch.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            rightFilter = rightSearch.getText().trim().toLowerCase();
            refreshModels();
        }});
        top.add(rightSearch);
        p.add(top, BorderLayout.NORTH);
        rightModel = new DefaultListModel<String>();
        rightList = new JList<String>(rightModel);
        rightList.setCellRenderer(new TransferCellRenderer(false));
        rightList.setFont(ElementTheme.FONT.deriveFont(13f));
        rightList.setBackground(Color.WHITE);
        rightList.setSelectionBackground(ElementTheme.FILL_BASE);
        rightList.setSelectionForeground(ElementTheme.TEXT_MAIN);
        rightList.setVisibleRowCount(8);
        rightList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                int idx = rightList.locationToIndex(e.getPoint());
                if (idx < 0) return;
                String key = rightModel.get(idx);
                toggleSet(rightChecked, key);
                rightList.repaint();
            }
        });
        JScrollPane sp = new JScrollPane(rightList);
        sp.setBorder(BorderFactory.createLineBorder(ElementTheme.BORDER_BASE));
        p.add(sp, BorderLayout.CENTER);
        rightCount = new JLabel("0 / 0");
        rightCount.setFont(ElementTheme.FONT.deriveFont(12f));
        rightCount.setForeground(ElementTheme.TEXT_REGULAR);
        rightCount.setBorder(new EmptyBorder(4, 4, 0, 0));
        p.add(rightCount, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildCenterPanel() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false);
        p.add(Box.createVerticalGlue());
        Button toRight = new Button("\u2192", Button.PRIMARY, false);
        toRight.setPreferredSize(new Dimension(48, 32));
        toRight.setMaximumSize(new Dimension(48, 32));
        toRight.setAlignmentX(Component.CENTER_ALIGNMENT);
        toRight.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { moveRight(); }});
        p.add(toRight);
        p.add(Box.createVerticalStrut(12));
        Button toLeft = new Button("\u2190", Button.DEFAULT, false);
        toLeft.setPreferredSize(new Dimension(48, 32));
        toLeft.setMaximumSize(new Dimension(48, 32));
        toLeft.setAlignmentX(Component.CENTER_ALIGNMENT);
        toLeft.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { moveLeft(); }});
        p.add(toLeft);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent makeHeader(String title) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 14f));
        lbl.setForeground(ElementTheme.TEXT_MAIN);
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        h.add(lbl, BorderLayout.WEST);
        return h;
    }

    private void moveRight() {
        if (leftChecked.isEmpty()) return;
        for (String k : leftChecked) rightKeys.add(k);
        leftChecked.clear();
        refreshModels();
        fireChange();
    }

    private void moveLeft() {
        if (rightChecked.isEmpty()) return;
        for (String k : rightChecked) rightKeys.remove(k);
        rightChecked.clear();
        refreshModels();
        fireChange();
    }

    private void toggleSet(java.util.Set<String> set, String key) {
        if (set.contains(key)) set.remove(key); else set.add(key);
    }

    private void refreshModels() {
        leftModel.clear(); rightModel.clear();
        int leftTotal = 0, rightTotal = 0;
        for (Item it : allItems.values()) {
            if (rightKeys.contains(it.key)) {
                if (rightFilter.isEmpty() || it.label.toLowerCase().contains(rightFilter)) {
                    rightModel.addElement(it.key);
                    rightTotal++;
                }
            } else {
                if (leftFilter.isEmpty() || it.label.toLowerCase().contains(leftFilter)) {
                    leftModel.addElement(it.key);
                    leftTotal++;
                }
            }
        }
        leftCount.setText(leftChecked.size() + " / " + leftTotal);
        rightCount.setText(rightChecked.size() + " / " + rightTotal);
    }

    private void fireChange() {
        if (changeListener != null) changeListener.accept(getSelected());
    }

    @Override public Dimension getPreferredSize() { return new Dimension(520, 260); }
    @Override public Dimension getMinimumSize() { return new Dimension(420, 200); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    // --- CellRenderer：自绘勾选框 + label ---
    private class TransferCellRenderer implements ListCellRenderer<String> {
        private final boolean leftSide;
        TransferCellRenderer(boolean left) { this.leftSide = left; }

        @Override public java.awt.Component getListCellRendererComponent(JList<? extends String> list, String key, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel row = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean checked = (leftSide && leftChecked.contains(key)) || (!leftSide && rightChecked.contains(key));
                    // 始终填充白底（保证文字对比度断言成立 + 行不透明）
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    if (isSelected) {
                        g2.setColor(ElementTheme.FILL_BASE);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    // 勾选框 □/☑
                    int bx = 10, by = (getHeight() - 14) / 2, bs = 14;
                    g2.setColor(checked ? ElementTheme.PRIMARY : ElementTheme.BORDER_BASE);
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawRoundRect(bx, by, bs, bs, 2, 2);
                    if (checked) {
                        g2.setColor(ElementTheme.PRIMARY);
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(bx+3, by+7, bx+6, by+10);
                        g2.drawLine(bx+6, by+10, bx+11, by+4);
                    }
                    // 文字
                    Item it = allItems.get(key);
                    String label = it == null ? key : it.label;
                    g2.setColor(ElementTheme.TEXT_MAIN);
                    ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstTransfer cell text");
                    g2.setFont(ElementTheme.FONT.deriveFont(13f));
                    FontMetrics fm = g2.getFontMetrics();
                    int baseY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    String t = label; int maxW = getWidth() - 32;
                    if (fm.stringWidth(t) > maxW) {
                        String ell = "\u2026";
                        while (t.length() > 0 && fm.stringWidth(t) + fm.stringWidth(ell) > maxW) t = t.substring(0, t.length()-1);
                        t = t + ell;
                    }
                    g2.drawString(t, 30, baseY);
                    g2.dispose();
                }
            };
            row.setOpaque(false);
            row.setPreferredSize(new Dimension(list.getVisibleRowCount() > 0 ? 200 : 200, 28));
            return row;
        }
    }

    static void selfCheck() {
        boolean threw = false;
        try { new AstTransfer(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null items must throw"; threw = false;
        try { new AstTransfer(new ArrayList<Item>() {{ add(null); }}); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null item must throw"; threw = false;
        try { new AstTransfer(new ArrayList<Item>()).setChangeListener(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null listener must throw"; threw = false;
        try { new AstTransfer(new ArrayList<Item>()).setSelectedKeys(null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null keys must throw";

        List<Item> data = new ArrayList<Item>();
        data.add(new Item("1", "苹果"));
        data.add(new Item("2", "香蕉"));
        data.add(new Item("3", "橙子"));
        AstTransfer tf = new AstTransfer(data);
        assert tf.getSelected().size() == 0 : "初始已选为空";
        tf.setSelectedKeys(java.util.Arrays.asList("2"));
        assert tf.getSelectedKeys().equals(java.util.Arrays.asList("2")) : "预设已选 2";
        // 无效 key 被忽略
        tf.setSelectedKeys(java.util.Arrays.asList("99"));
        assert tf.getSelected().size() == 0 : "无效 key 被忽略";

        final int[] fired = {0};
        tf.setChangeListener(new Consumer<List<Item>>() { public void accept(List<Item> sel) { fired[0]++; }});
        // 左面板勾选 1 和 3 → 移到右
        tf.leftChecked.add("1"); tf.leftChecked.add("3");
        tf.moveRight();
        assert tf.getSelectedKeys().size() == 2 : "moveRight 后 2 项";
        assert tf.getSelectedKeys().contains("1") && tf.getSelectedKeys().contains("3");
        assert fired[0] == 1 : "changeListener 触发一次 fired=" + fired[0];
        // 右面板勾选 1 → 移到左
        tf.rightChecked.add("1");
        tf.moveLeft();
        assert !tf.getSelectedKeys().contains("1") : "moveLeft 后 1 不在右";
        assert tf.getSelectedKeys().contains("3");
        assert fired[0] == 2 : "changeListener 第二次 fired=" + fired[0];

        // filterable 开关
        tf.setFilterable(true);
        assert tf.isFilterable();

        // EDT + 渲染校验
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                try {
                    JFrame jf = new JFrame("AstTransfer SC"); jf.setSize(800, 600); jf.setVisible(true);
                    List<Item> d2 = new ArrayList<Item>();
                    for (int i = 0; i < 5; i++) d2.add(new Item("k"+i, "项"+i));
                    AstTransfer t2 = new AstTransfer(d2);
                    JPanel cp = (JPanel) jf.getContentPane(); cp.setLayout(new FlowLayout());
                    cp.add(t2); jf.pack();
                    assert t2.getPreferredSize().width > 0;
                    // 离屏绘制 CellRenderer
                    JList<String> dummy = new JList<String>(new String[]{"k0"});
                    TransferCellRenderer r = t2.new TransferCellRenderer(true);
                    java.awt.Component c = r.getListCellRendererComponent(dummy, "k0", 0, false, false);
                    c.setSize(200, 28);
                    java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(200, 28, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    Graphics2D gg = img.createGraphics();
                    try { c.paint(gg); } finally { gg.dispose(); }
                    int px = img.getRGB(2, 10); int a = (px >>> 24) & 0xFF;
                    assert a > 100 : "cell rendered alpha=" + a;
                    jf.dispose();
                } catch (Throwable e) { err[0] = e; }
            }});
        } catch (Throwable e) { err[0] = e; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstTransfer self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
