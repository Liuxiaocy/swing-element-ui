package org.swelement.ui;

import org.swelement.framework.AstContainerComponent;

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
public class AstTransfer extends AstContainerComponent {
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
    /** 列表区空态文本的垂直居中基线偏移（渲染与断言共用同一布局算法）。 */
    private static final int EMPTY_HINT_FONT_SIZE = 13;
    private JLabel leftCount, rightCount;
    private JTextField leftSearch, rightSearch;

    // --- 尺寸档位（对齐 Element UI）---
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    private static final int[] TIER_ROW_H = {32, 28, 24};
    private static final float[] TIER_FONT = {13f, 13f, 12f};
    private int tier = SIZE_DEFAULT;
    private int rowH = 28;
    private Font cellFont;

    // --- F4: 自定义文案与空态 ---
    private String leftTitle = "列表";
    private String rightTitle = "已选";
    private String toRightText = "\u2192";
    private String toLeftText = "\u2190";
    private String emptyText = "无数据";
    private JLabel leftTitleLbl, rightTitleLbl;
    private AstButton toRightBtn, toLeftBtn;

    public AstTransfer(List<Item> items) {
        if (items == null) throw new IllegalArgumentException("items must not be null");
        for (Item it : items) {
            if (it == null) throw new IllegalArgumentException("item must not be null");
            allItems.put(it.key, it);
        }
        setFont(UIManager.getFont("Label.font"));
        cellFont = getFont().deriveFont(13f);
        applyTier();
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
        leftSearch = new JTextField();
        leftSearch.setFont(getFont().deriveFont(13f));
        leftSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme().getBorderBase()),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        leftSearch.setVisible(false);
        leftSearch.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            leftFilter = leftSearch.getText().trim().toLowerCase();
            refreshModels();
        }});
        // header + search 用 BoxLayout 纵向堆叠在 NORTH 区（BorderLayout 每个方位只能放一个组件）
        JPanel top = new JPanel(); top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS)); top.setOpaque(false);
        top.add(makeHeader(leftTitle, true));
        top.add(leftSearch);
        p.removeAll();
        p.add(top, BorderLayout.NORTH);
        leftModel = new DefaultListModel<String>();
        leftList = makeList(leftModel);
        leftList.setCellRenderer(new TransferCellRenderer(true));
        leftList.setFont(getFont().deriveFont(13f));
        leftList.setBackground(Color.WHITE);
        leftList.setSelectionBackground(theme().getFillBase());
        leftList.setSelectionForeground(theme().getTextPrimary());
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
        sp.setBorder(BorderFactory.createLineBorder(theme().getBorderBase()));
        p.add(sp, BorderLayout.CENTER);
        leftCount = new JLabel("0 / 0");
        leftCount.setFont(getFont().deriveFont(12f));
        leftCount.setForeground(theme().getTextRegular());
        leftCount.setBorder(new EmptyBorder(4, 4, 0, 0));
        p.add(leftCount, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(220, 240));
        JPanel top = new JPanel(); top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS)); top.setOpaque(false);
        top.add(makeHeader(rightTitle, false));
        rightSearch = new JTextField();
        rightSearch.setFont(getFont().deriveFont(13f));
        rightSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme().getBorderBase()),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        rightSearch.setVisible(false);
        rightSearch.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            rightFilter = rightSearch.getText().trim().toLowerCase();
            refreshModels();
        }});
        top.add(rightSearch);
        p.add(top, BorderLayout.NORTH);
        rightModel = new DefaultListModel<String>();
        rightList = makeList(rightModel);
        rightList.setCellRenderer(new TransferCellRenderer(false));
        rightList.setFont(getFont().deriveFont(13f));
        rightList.setBackground(Color.WHITE);
        rightList.setSelectionBackground(theme().getFillBase());
        rightList.setSelectionForeground(theme().getTextPrimary());
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
        sp.setBorder(BorderFactory.createLineBorder(theme().getBorderBase()));
        p.add(sp, BorderLayout.CENTER);
        rightCount = new JLabel("0 / 0");
        rightCount.setFont(getFont().deriveFont(12f));
        rightCount.setForeground(theme().getTextRegular());
        rightCount.setBorder(new EmptyBorder(4, 4, 0, 0));
        p.add(rightCount, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildCenterPanel() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false);
        p.add(Box.createVerticalGlue());
        toRightBtn = new AstButton(toRightText, AstButton.PRIMARY, false);
        sizeButton(toRightBtn, toRightText);
        toRightBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        toRightBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { moveRight(); }});
        p.add(toRightBtn);
        p.add(Box.createVerticalStrut(12));
        toLeftBtn = new AstButton(toLeftText, AstButton.DEFAULT, false);
        sizeButton(toLeftBtn, toLeftText);
        toLeftBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        toLeftBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { moveLeft(); }});
        p.add(toLeftBtn);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent makeHeader(String title, boolean left) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(getFont().deriveFont(Font.BOLD, 14f));
        lbl.setForeground(theme().getTextPrimary());
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        h.add(lbl, BorderLayout.WEST);
        if (left) leftTitleLbl = lbl; else rightTitleLbl = lbl;
        return h;
    }

    /** 列表为空时在列表区居中绘制空态提示。 */
    private JList<String> makeList(DefaultListModel<String> model) {
        return new JList<String>(model) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getModel().getSize() == 0 && !emptyText.isEmpty()) {
                    paintEmptyHint(g, this);
                }
            }
        };
    }

    private void paintEmptyHint(Graphics g, JList<?> list) {
        // 空态提示是功能性文本，用 TEXT_REGULAR（6.1:1）而非 TEXT_PLACEHOLDER（1.7:1，不达 WCAG AA）
        assertContrast(theme().getTextRegular(), Color.WHITE, "AstTransfer empty hint on white");
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(getFont().deriveFont((float) EMPTY_HINT_FONT_SIZE));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(theme().getTextRegular());
        g2.drawString(emptyText,
                (list.getWidth() - fm.stringWidth(emptyText)) / 2,
                list.getHeight() / 2 - fm.getHeight() / 2 + fm.getAscent());
        g2.dispose();
    }

    // --- F4 公开 API ---

    /** 自定义两侧标题（Element 的 titles）。 */
    public void setTitles(String left, String right) {
        if (left == null) throw new IllegalArgumentException("left title must not be null");
        if (right == null) throw new IllegalArgumentException("right title must not be null");
        this.leftTitle = left;
        this.rightTitle = right;
        if (leftTitleLbl != null) leftTitleLbl.setText(left);
        if (rightTitleLbl != null) rightTitleLbl.setText(right);
        revalidate();
        repaint();
    }

    /** 返回 {左标题, 右标题}。 */
    public String[] getTitles() { return new String[]{leftTitle, rightTitle}; }

    /** 自定义中间两个转移按钮的文案（Element 的 button-texts）。宽度随文案自适应，避免截断。 */
    public void setButtonTexts(String toRight, String toLeft) {
        if (toRight == null) throw new IllegalArgumentException("toRight text must not be null");
        if (toLeft == null) throw new IllegalArgumentException("toLeft text must not be null");
        this.toRightText = toRight;
        this.toLeftText = toLeft;
        if (toRightBtn != null) { toRightBtn.setText(toRight); sizeButton(toRightBtn, toRight); }
        if (toLeftBtn != null) { toLeftBtn.setText(toLeft); sizeButton(toLeftBtn, toLeft); }
        revalidate();
        repaint();
    }

    /** 返回 {右移按钮文案, 左移按钮文案}。 */
    public String[] getButtonTexts() { return new String[]{toRightText, toLeftText}; }

    /** 自定义列表为空时的提示文案，传空串则不显示空态。 */
    public void setEmptyText(String t) {
        if (t == null) throw new IllegalArgumentException("empty text must not be null");
        this.emptyText = t;
        repaint();
    }

    public String getEmptyText() { return emptyText; }

    /**
     * 同步按钮上限尺寸：AstButton 覆写了 getPreferredSize（按文案自算，外部 setPreferredSize 无效），
     * 而中间的 BoxLayout 按 maximumSize 夹紧，所以长文案必须同步放宽上限，否则会被压窄截断。
     */
    private void sizeButton(AstButton b, String text) {
        Dimension p = b.getPreferredSize();
        b.setMaximumSize(new Dimension(Math.max(48, p.width), p.height));
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

    @Override public Dimension getPreferredSize() {
        int h = 260 + (rowH - 28) * 8; // 基准 260，随档位行高线性缩放
        return new Dimension(520, h);
    }
    @Override public Dimension getMinimumSize() { return new Dimension(420, 200); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    // --- 尺寸档位（对齐 Element UI）---
    public void setSize(int tier) {
        if (tier < SIZE_LARGE || tier > SIZE_SMALL) throw new IllegalArgumentException("tier out of range: " + tier);
        this.tier = tier;
        applyTier();
        revalidate();
    }

    private void applyTier() {
        this.rowH = TIER_ROW_H[tier];
        this.cellFont = getFont().deriveFont(TIER_FONT[tier]);
        // JList 可视行数随档位调整（整体高度同步变化）
        if (leftList != null) { leftList.setVisibleRowCount(8 - tier); rightList.setVisibleRowCount(8 - tier); }
    }

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
                        g2.setColor(theme().getFillBase());
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    // 勾选框 □/☑
                    int bx = 10, by = (getHeight() - 14) / 2, bs = 14;
                    g2.setColor(checked ? theme().getPrimary() : theme().getBorderBase());
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawRoundRect(bx, by, bs, bs, 2, 2);
                    if (checked) {
                        g2.setColor(theme().getPrimary());
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(bx+3, by+7, bx+6, by+10);
                        g2.drawLine(bx+6, by+10, bx+11, by+4);
                    }
                    // 文字
                    Item it = allItems.get(key);
                    String label = it == null ? key : it.label;
                    g2.setColor(theme().getTextPrimary());
                    assertContrast(theme().getTextPrimary(), Color.WHITE, "AstTransfer cell text");
                    g2.setFont(cellFont);
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
            row.setPreferredSize(new Dimension(list.getVisibleRowCount() > 0 ? 200 : 200, rowH));
            return row;
        }
    }

    /** 离屏渲染整个穿梭框（递归布局后绘制）。 */
    private static java.awt.image.BufferedImage renderTransfer(AstTransfer t) {
        t.setBounds(0, 0, 520, 260);
        layoutAll(t);
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(520, 260, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { t.paint(g); } finally { g.dispose(); }
        return img;
    }

    /** 递归布局：doLayout 只作用一层，离屏渲染需要手动铺开整棵子树。 */
    private static void layoutAll(java.awt.Component c) {
        c.doLayout();
        if (c instanceof java.awt.Container) {
            for (java.awt.Component ch : ((java.awt.Container) c).getComponents()) layoutAll(ch);
        }
    }

    /** 统计区域内的深色像素（空态文本为 TEXT_REGULAR，白底上明显偏暗）。 */
    private static int countDark(java.awt.image.BufferedImage img, int x0, int y0, int x1, int y1) {
        int n = 0;
        for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
                int p = img.getRGB(x, y);
                int a = (p >>> 24) & 0xFF;
                if (a < 100) continue;
                int r = (p >>> 16) & 0xFF, g = (p >>> 8) & 0xFF, b = p & 0xFF;
                if (r < 200 && g < 200 && b < 200) n++;
            }
        }
        return n;
    }

    // --- Self-check ---
    @Override
    protected void selfCheck() {
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

        // 尺寸档位（R1）：整体高度随档位单调缩放，非法档位抛异常
        AstTransfer tf3 = new AstTransfer(data);
        tf3.setSize(AstTransfer.SIZE_LARGE);
        int hL = tf3.getPreferredSize().height;
        tf3.setSize(AstTransfer.SIZE_DEFAULT);
        int hD = tf3.getPreferredSize().height;
        tf3.setSize(AstTransfer.SIZE_SMALL);
        int hS = tf3.getPreferredSize().height;
        assert hL > hD && hD > hS : "AstTransfer 档位高度应单调递减 L>D>S, got " + hL + "," + hD + "," + hS;
        threw = false;
        try { tf3.setSize(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "AstTransfer 非法档位应抛异常";

        // --- F4: setTitles / setButtonTexts / 空态 ---
        AstTransfer tf4 = new AstTransfer(data);
        assert tf4.getTitles()[0].equals("列表") : "默认左标题";
        assert tf4.getTitles()[1].equals("已选") : "默认右标题";
        tf4.setTitles("待选列表", "已选列表");
        assert tf4.getTitles()[0].equals("待选列表") : "左标题";
        assert tf4.getTitles()[1].equals("已选列表") : "右标题";
        // 标题必须落到真实 JLabel，而不只是改了字段
        assert tf4.leftTitleLbl.getText().equals("待选列表") : "左标题未同步到 JLabel: " + tf4.leftTitleLbl.getText();
        assert tf4.rightTitleLbl.getText().equals("已选列表") : "右标题未同步到 JLabel: " + tf4.rightTitleLbl.getText();
        threw = false;
        try { tf4.setTitles(null, "x"); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null 左标题应抛异常";
        threw = false;
        try { tf4.setTitles("x", null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null 右标题应抛异常";

        assert tf4.getButtonTexts()[0].equals("\u2192") : "默认右移文案";
        assert tf4.getButtonTexts()[1].equals("\u2190") : "默认左移文案";
        int wArrow = tf4.toRightBtn.getMaximumSize().width;
        tf4.setButtonTexts("添加", "移除");
        assert tf4.toRightBtn.getText().equals("添加") : "右移文案未同步到按钮";
        assert tf4.toLeftBtn.getText().equals("移除") : "左移文案未同步到按钮";
        int wText = tf4.toRightBtn.getMaximumSize().width;
        assert wText > wArrow : "长文案按钮上限应变宽以免截断, got " + wArrow + " -> " + wText;
        tf4.setButtonTexts("全部添加到右侧", "全部移除到左侧");
        int wLong = tf4.toRightBtn.getMaximumSize().width;
        assert wLong > wText : "更长文案上限应更宽, got " + wText + " -> " + wLong;
        // 核心：上限不得小于 AstButton 自算的宽度，否则 BoxLayout 会夹紧导致文案截断
        assert wLong >= tf4.toRightBtn.getPreferredSize().width
            : "按钮上限 " + wLong + " 小于自算宽度 " + tf4.toRightBtn.getPreferredSize().width + "，文案会被截断";
        threw = false;
        try { tf4.setButtonTexts(null, "x"); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null 右移文案应抛异常";
        threw = false;
        try { tf4.setButtonTexts("x", null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null 左移文案应抛异常";

        // 绘制级：左列清空后应居中绘制空态「无数据」，置空后不再绘制
        AstTransfer tf5 = new AstTransfer(data);
        tf5.setEmptyText("无数据");
        assert tf5.getEmptyText().equals("无数据") : "空态文案";
        tf5.setSelectedKeys(java.util.Arrays.asList("1", "2", "3")); // 左列清空
        int withHint = countDark(renderTransfer(tf5), 20, 80, 190, 180);
        tf5.setEmptyText("");
        int noHint = countDark(renderTransfer(tf5), 20, 80, 190, 180);
        assert withHint > 30 : "空态应绘制「无数据」文本, darkPixels=" + withHint;
        assert noHint == 0 : "setEmptyText(\"\") 后不应再绘制空态, darkPixels=" + noHint;
        threw = false;
        try { tf5.setEmptyText(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null 空态文案应抛异常";

        System.out.println("AstTransfer self-check OK");
    }
    public static void main(String[] args) {
        new AstTransfer(new ArrayList<Item>()).selfCheck();
    }
}
