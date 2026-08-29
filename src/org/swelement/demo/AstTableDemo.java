package org.swelement.demo;

import org.swelement.ui.AstButton;
import org.swelement.ui.AstTable;
import org.swelement.ui.AstTableColumn;
import org.swelement.ui.AstTableModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * AstTable 综合 Demo — 演示 P4-C 的 9 类表格能力 + 综合示例。
 *
 * 标签页：
 *   C1 固定表头 + 纵向滚动     C2 左/右冻结列 + 横向滚动
 *   C3 多级表头               C4 单选 / 多选
 *   C5 排序（升/降/无）        C6 筛选（列文本过滤）
 *   C7 展开行                 C8 表尾合计行
 *   C9 合并行/列 + 状态行      综合示例
 *
 * 交互提示：
 *   - 滚轮纵向滚动；Shift+滚轮横向滚动。
 *   - 可排序列点表头循环「无 → 升 → 降 → 无」，可筛选列点表头最右漏斗输入文本。
 *   - 设置了展开内容的表格，双击行展开/收起。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstTableDemo --selfcheck}
 * 会离屏构建并绘制全部标签页，验证不抛异常。
 */
public class AstTableDemo {

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstTable Demo — 9 类表格能力 + 综合示例");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("C1 固定表头/滚动", new JScrollPane(section1()));
        tabs.addTab("C2 冻结列", new JScrollPane(section2()));
        tabs.addTab("C3 多级表头", new JScrollPane(section3()));
        tabs.addTab("C4 单选/多选", new JScrollPane(section4()));
        tabs.addTab("C5 排序", new JScrollPane(section5()));
        tabs.addTab("C6 筛选", new JScrollPane(section6()));
        tabs.addTab("C7 展开行", new JScrollPane(section7()));
        tabs.addTab("C8 合计行", new JScrollPane(section8()));
        tabs.addTab("C9 合并/状态", new JScrollPane(section9()));
        tabs.addTab("综合示例", new JScrollPane(sectionAll()));

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10, 12, 10, 12));
        root.add(tabs, BorderLayout.CENTER);
        f.setContentPane(root);
        f.setSize(960, 680);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    // ============ C1 固定表头 + 纵向滚动 ============
    private static JPanel section1() {
        AstTable t = new AstTable(new AstTableColumn[]{
            new AstTableColumn("姓名", 110),
            new AstTableColumn("工号", 90, AstTable.Align.CENTER),
            new AstTableColumn("部门", 150),
            new AstTableColumn("入职日期", 130, AstTable.Align.CENTER),
        });
        String[] depts = {"平台研发部", "产品设计部", "运营部", "市场部"};
        for (int i = 1; i <= 20; i++)
            t.addRow("员工" + i, 1000 + i, depts[i % depts.length], "2023-0" + (i % 9 + 1) + "-15");
        // 固定高度：表头吸顶，body 内部滚动（不套 JScrollPane）
        t.setPreferredSize(new Dimension(482, 200));
        final JLabel info = tipLabel("滚轮在表格内滚动，表头保持不动。");
        t.setRowClickListener(row -> info.setText("点击第 " + (row + 1) + " 行："
            + t.getValueAt(row, 0) + " · 工号 " + t.getValueAt(row, 1) + " · " + t.getValueAt(row, 2)));
        return section("C1 固定表头 + 纵向滚动 — 20 行数据、视口仅约 165px；"
            + "表头始终吸顶，滚轮在表格内纵向滚动。", t, info);
    }

    // ============ C2 左/右冻结列 + 横向滚动 ============
    private static JPanel section2() {
        AstTableColumn name = new AstTableColumn("姓名", 100, AstTable.Align.LEFT, false, AstTableColumn.Fixed.LEFT, null);
        AstTableColumn dept = new AstTableColumn("部门", 160);
        AstTableColumn addr = new AstTableColumn("办公地址", 220);
        AstTableColumn tel = new AstTableColumn("手机号", 150, AstTable.Align.CENTER);
        AstTableColumn op = new AstTableColumn("操作", 110, AstTable.Align.CENTER, false, AstTableColumn.Fixed.RIGHT, null);
        AstTable t = new AstTable(new AstTableColumn[]{name, dept, addr, tel, op});
        String[] depts = {"平台研发部", "产品设计部", "运营部"};
        for (int i = 1; i <= 10; i++)
            t.addRow("员工" + i, depts[i % depts.length],
                "上海市浦东新区张江高科技园区博云路 " + i + " 号", "138001380" + (i % 10), "编辑 删除");
        t.setPreferredSize(new Dimension(430, 210)); // 窄于总宽(740) → 可横向滚动
        return section("C2 左/右冻结列 — 总宽 740px 而视口仅 430px：底部出现<b>横向滚动条</b>（可直接拖拽滑块），"
            + "也可 <b>Shift+滚轮</b> 滚动；「姓名」列左冻结、「操作」列右冻结，滚动时始终固定可见。", t);
    }

    // ============ C3 多级表头 ============
    private static JPanel section3() {
        AstTableColumn city = new AstTableColumn("城市", 110);
        AstTableColumn street = new AstTableColumn("街道", 190);
        AstTableColumn addr = new AstTableColumn("地址", Arrays.asList(city, street));
        AstTable t = new AstTable(new AstTableColumn[]{
            new AstTableColumn("姓名", 100), addr, new AstTableColumn("年龄", 80, AstTable.Align.CENTER)});
        t.addRow("张三", "上海市", "浦东新区张江路 100 号", 28);
        t.addRow("李四", "北京市", "海淀区中关村大街 1 号", 34);
        t.addRow("王五", "广州市", "天河区珠江新城 8 号", 22);
        return section("C3 多级表头 — 父列「地址」跨「城市 / 街道」两列居中显示，"
            + "表头占两行高度，叶子顺序仍为 姓名 → 城市 → 街道 → 年龄。", t);
    }

    // ============ C4 单选 / 多选 ============
    private static JPanel section4() {
        AstTable t = new AstTable(new AstTableColumn[]{
            new AstTableColumn("姓名", 110),
            new AstTableColumn("部门", 160),
            new AstTableColumn("年龄", 90, AstTable.Align.CENTER),
        });
        addEmployees(t, 6);
        final JLabel info = tipLabel("当前：单选 — 点击行选中（再点同一行取消）");
        t.setRowClickListener(row -> info.setText(
            (t.getModel().getSelectionMode() == AstTableModel.SelectionMode.MULTIPLE ? "多选" : "单选")
            + "：点击第 " + (row + 1) + " 行「" + t.getValueAt(row, 0) + "」，当前已选 "
            + t.getModel().getSelectedViewRows().size() + " 行"));

        final AstButton modeBtn = new AstButton("切换为多选", AstButton.PRIMARY, false);
        modeBtn.addActionListener(e -> {
            boolean multi = t.getModel().getSelectionMode() == AstTableModel.SelectionMode.MULTIPLE;
            t.setSelectionMode(multi ? AstTable.SELECTION_SINGLE : AstTable.SELECTION_MULTIPLE);
            modeBtn.setText(multi ? "切换为多选" : "切换为单选");
            info.setText("已切换为" + (multi ? "单选" : "多选") + " — 多选时首列为复选框，点表头复选框可全选");
            t.repaint();
        });
        AstButton allBtn = new AstButton("全选 / 清空", AstButton.DEFAULT, false);
        allBtn.addActionListener(e -> { t.getModel().toggleSelectAll(); t.repaint(); });
        AstButton readBtn = new AstButton("读取选中行", AstButton.INFO, false);
        readBtn.addActionListener(e -> info.setText("已选视图行：" + t.getModel().getSelectedViewRows()));
        return section("C4 单选 / 多选 — 多选模式首列插入冻结复选框列，表头复选框全选；点击整行即切换选中。",
            t, info, buttonRow(modeBtn, allBtn, readBtn));
    }

    // ============ C5 排序 ============
    private static JPanel section5() {
        AstTableColumn age = new AstTableColumn("年龄", 90, AstTable.Align.CENTER, true, AstTableColumn.Fixed.NONE, false, null);
        AstTableColumn name = new AstTableColumn("姓名", 110, AstTable.Align.LEFT, true, AstTableColumn.Fixed.NONE, false, null);
        AstTable t = new AstTable(new AstTableColumn[]{name, age, new AstTableColumn("部门", 160)});
        t.addRow("王五", 22, "运营部");
        t.addRow("张三", 34, "平台研发部");
        t.addRow("李四", 28, "产品设计部");
        t.addRow("赵六", 45, "市场部");
        final JLabel info = tipLabel("点击「姓名 / 年龄」表头循环切换：无 → 升序 ▲ → 降序 ▼ → 无");
        AstButton ascBtn = new AstButton("年龄升序", AstButton.PRIMARY, false);
        AstButton descBtn = new AstButton("年龄降序", AstButton.DEFAULT, false);
        AstButton noneBtn = new AstButton("取消排序", AstButton.DEFAULT, false);
        ascBtn.addActionListener(e -> { t.setSort(1, AstTableModel.SortDir.ASC); info.setText("年龄升序：22, 28, 34, 45"); });
        descBtn.addActionListener(e -> { t.setSort(1, AstTableModel.SortDir.DESC); info.setText("年龄降序：45, 34, 28, 22"); });
        noneBtn.addActionListener(e -> { t.setSort(1, AstTableModel.SortDir.NONE); info.setText("已取消排序，恢复原始行序"); });
        return section("C5 排序 — 可排序列（姓名/年龄）表头右侧画方向箭头，点击循环切换；"
            + "数值按大小、文本按字典序比较。", t, info, buttonRow(ascBtn, descBtn, noneBtn));
    }

    // ============ C6 筛选 ============
    private static JPanel section6() {
        AstTableColumn city = new AstTableColumn("城市", 130, AstTable.Align.LEFT, false, AstTableColumn.Fixed.NONE, true, null);
        AstTable t = new AstTable(new AstTableColumn[]{
            new AstTableColumn("姓名", 110), city, new AstTableColumn("人数", 90, AstTable.Align.CENTER)});
        t.addRow("张三", "上海市", 120);
        t.addRow("李四", "北京市", 200);
        t.addRow("王五", "广州市", 150);
        t.addRow("赵六", "上海市", 90);
        final JTextField qf = new JTextField(10);
        final JLabel info = tipLabel("点击「城市」列头最右侧的漏斗图标可弹出输入框，回车即筛选。");
        AstButton applyBtn = new AstButton("按城市筛选", AstButton.PRIMARY, false);
        applyBtn.addActionListener(e -> {
            String q = qf.getText().trim();
            t.setFilter(1, q);
            info.setText(q.isEmpty() ? "筛选条件为空，已显示全部 " + t.getRowCount() + " 行"
                : "筛选城市含「" + q + "」，剩余 " + t.getRowCount() + " 行");
        });
        AstButton clearBtn = new AstButton("清空筛选", AstButton.DEFAULT, false);
        clearBtn.addActionListener(e -> { qf.setText(""); t.clearFilter(); info.setText("已清空筛选，恢复 " + t.getRowCount() +  " 行"); });
        return section("C6 筛选 — 可筛选列（城市）表头最右画漏斗图标；筛选后视图行重建，合计/排序均基于筛选结果。",
            t, info, buttonRow(labeled("关键词：", qf), applyBtn, clearBtn));
    }

    // ============ C7 展开行 ============
    private static JPanel section7() {
        AstTable t = new AstTable(new AstTableColumn[]{
            new AstTableColumn("姓名", 110),
            new AstTableColumn("部门", 170),
            new AstTableColumn("年龄", 90, AstTable.Align.CENTER),
        });
        final String[] names = {"张三", "李四", "王五", "赵六", "孙七"};
        final String[] depts = {"平台研发部", "产品设计部", "运营部", "市场部", "平台研发部"};
        final int[] ages = {28, 34, 22, 45, 31};
        for (int i = 0; i < names.length; i++) t.addRow(names[i], depts[i], ages[i]);
        t.setRowExpandText(raw -> "第 " + (raw + 1) + " 行详情 — " + names[raw] + " · " + depts[raw]
            + " · 年龄 " + ages[raw] + " · 工位 A-" + (100 + raw) + " · 直属上级：部门负责人");
        final JLabel info = tipLabel("点击第一列行首的 ▶ 按钮（或双击行）展开/收起详情；展开区块整行宽、浅色底。");
        AstButton expandBtn = new AstButton("展开第 1 行", AstButton.PRIMARY, false);
        expandBtn.addActionListener(e -> { t.getModel().toggleExpanded(0); t.revalidate(); t.repaint(); });
        AstButton collapseBtn = new AstButton("全部收起", AstButton.DEFAULT, false);
        collapseBtn.addActionListener(e -> {
            for (int i = 0; i < t.getModel().rawRowCount(); i++) if (t.getModel().isExpanded(i)) t.getModel().toggleExpanded(i);
            t.revalidate(); t.repaint();
        });
        return section("C7 展开行 — 每行第一列行首有 ▶/▼ 展开按钮，点击即切换（也可双击行）；"
            + "展开时该行下方插入 80px 详情区块，滚动高度与视图行数同步扩展。", t, info, buttonRow(expandBtn, collapseBtn));
    }

    // ============ C8 表尾合计行 ============
    private static JPanel section8() {
        AstTable t = new AstTable(new AstTableColumn[]{
            new AstTableColumn("姓名", 110),
            new AstTableColumn("部门", 170),
            new AstTableColumn("月薪", 110, AstTable.Align.RIGHT),
        });
        t.addRow("张三", "平台研发部", 18000);
        t.addRow("李四", "产品设计部", 15000);
        t.addRow("王五", "运营部", 12000);
        final JLabel info = tipLabel("表尾合计行：未注册列留空，首列显示「合计」标签。");
        AstButton sumBtn = new AstButton("月薪求和", AstButton.PRIMARY, false);
        sumBtn.addActionListener(e -> {
            t.setSummary(2, null); // null → 默认求和
            info.setText("月薪合计 = " + t.getModel().getSummary(2) + " 元");
        });
        AstButton avgBtn = new AstButton("月薪平均", AstButton.SUCCESS, false);
        avgBtn.addActionListener(e -> { t.setSummary(2, AstTableModel.AVG); info.setText("月薪平均 = " + t.getModel().getSummary(2)); });
        AstButton hideBtn = new AstButton("隐藏合计行", AstButton.DEFAULT, false);
        hideBtn.addActionListener(e -> { t.clearSummary(); info.setText("已隐藏表尾合计行"); });
        return section("C8 表尾合计行 — 注册聚合器后 FooterView 自动显示；"
            + "合计基于<b>当前筛选后的行</b>，筛选变化会实时重算。", t, info, buttonRow(sumBtn, avgBtn, hideBtn));
    }

    // ============ C9 合并行/列 + 状态行 ============
    private static JPanel section9() {
        AstTable t = new AstTable(new AstTableColumn[]{
            new AstTableColumn("区域", 100, AstTable.Align.CENTER),
            new AstTableColumn("城市", 130),
            new AstTableColumn("销售额(万)", 120, AstTable.Align.RIGHT),
        });
        t.addRow("华东", "上海市", 120);
        t.addRow("华东", "杭州市", 90);
        t.addRow("华南", "广州市", 80);
        t.addRow("华南", "深圳市", 110);
        // 「区域」列纵向合并：华东跨 2 行、华南跨 2 行
        t.setSpan(0, 0, 2, 1);
        t.setSpan(2, 0, 2, 1);
        // 状态行：整行着色
        t.setRowStatus(1, AstTableModel.Status.SUCCESS);
        t.setRowStatus(3, AstTableModel.Status.WARNING);
        final JLabel info = tipLabel("「区域」列已纵向合并（华东/华南各跨 2 行）；第 2 行成功绿、第 4 行警告橙。");
        AstButton addDangerBtn = new AstButton("标记第 3 行为危险", AstButton.DANGER, false);
        addDangerBtn.addActionListener(e -> { t.setRowStatus(2, AstTableModel.Status.DANGER); info.setText("第 3 行已标记为「危险」红色底"); });
        AstButton mergeCityBtn = new AstButton("合并城市列首两格", AstButton.DEFAULT, false);
        mergeCityBtn.addActionListener(e -> { t.setSpan(0, 1, 1, 2); info.setText("城市列第 1 行已横向合并 2 列"); });
        return section("C9 合并行/列 + 带状态表格 — 锚点格按 rowspan/colspan 扩展、被覆盖格跳过；"
            + "状态行用主题色的浅色底 + 主文本色（满足 WCAG 对比度）。", t, info, buttonRow(addDangerBtn, mergeCityBtn));
    }

    // ============ 综合示例 ============
    private static JPanel sectionAll() {
        AstTableColumn name = new AstTableColumn("姓名", 100, AstTable.Align.LEFT, false, AstTableColumn.Fixed.LEFT, null);
        AstTableColumn city = new AstTableColumn("城市", 110, AstTable.Align.LEFT, false, AstTableColumn.Fixed.NONE, true, null);
        AstTableColumn street = new AstTableColumn("街道", 180);
        AstTableColumn addr = new AstTableColumn("地址", Arrays.asList(city, street));
        AstTableColumn age = new AstTableColumn("年龄", 90, AstTable.Align.CENTER, true, AstTableColumn.Fixed.NONE, false, null);
        AstTableColumn salary = new AstTableColumn("月薪", 110, AstTable.Align.RIGHT, false, AstTableColumn.Fixed.NONE, false, null);
        AstTable t = new AstTable(new AstTableColumn[]{name, addr, age, salary});
        t.addRow("张三", "上海市", "浦东新区张江路 100 号", 28, 18000);
        t.addRow("李四", "北京市", "海淀区中关村大街 1 号", 34, 22000);
        t.addRow("王五", "广州市", "天河区珠江新城 8 号", 22, 12000);
        t.addRow("赵六", "上海市", "徐汇区漕溪北路 25 号", 45, 26000);
        t.setSelectionMode(AstTable.SELECTION_MULTIPLE);   // C4
        t.setSummary(3, null);                             // C8：叶子列 3 = 月薪
        t.setRowExpandText(raw -> "综合示例 · 第 " + (raw + 1) + " 行：支持冻结列 / 多级表头 / 多选 / 排序 / 筛选 / 展开 / 合计");
        t.setRowStatus(2, AstTableModel.Status.INFO);      // C9

        final JLabel info = tipLabel("已启用：左冻结姓名 + 多级表头 + 多选 + 年龄排序 + 城市筛选 + 行首 ▶ 展开 + 月薪合计 + 状态行。");
        t.setRowClickListener(row -> info.setText("点击第 " + (row + 1) + " 行「" + t.getValueAt(row, 0)
            + "」，已选 " + t.getModel().getSelectedViewRows().size() + " 行，月薪合计 " + t.getModel().getSummary(3)));
        AstButton ascBtn = new AstButton("年龄升序", AstButton.PRIMARY, false);
        ascBtn.addActionListener(e -> { t.setSort(2, AstTableModel.SortDir.ASC); info.setText("按年龄升序（叶子列 2）；合计仍为 " + t.getModel().getSummary(3)); });
        AstButton filterBtn = new AstButton("只看上海", AstButton.SUCCESS, false);
        filterBtn.addActionListener(e -> { t.setFilter(1, "上海"); info.setText("筛选城市含「上海」，剩余 " + t.getRowCount() + " 行，合计 " + t.getModel().getSummary(3)); });
        AstButton clearBtn = new AstButton("重置", AstButton.DEFAULT, false);
        clearBtn.addActionListener(e -> {
            t.clearFilter(); t.setSort(2, AstTableModel.SortDir.NONE);
            info.setText("已重置筛选与排序，共 " + t.getRowCount() + " 行，合计 " + t.getModel().getSummary(3));
        });
        return section("综合示例 — 一张表同时叠加 C1~C9 全部能力：冻结列、多级表头、多选、排序、筛选、展开行、合计行、状态行。",
            t, info, buttonRow(ascBtn, filterBtn, clearBtn));
    }

    // ===================== 工具方法 =====================

    private static void addEmployees(AstTable t, int n) {
        String[] names = {"张三", "李四", "王五", "赵六", "孙七", "周八", "吴九", "郑十"};
        String[] depts = {"平台研发部", "产品设计部", "运营部", "市场部"};
        for (int i = 0; i < n; i++)
            t.addRow(names[i % names.length] + (i >= names.length ? (i / names.length + 1) : ""),
                depts[i % depts.length], 22 + (i * 3) % 20);
    }

    private static JLabel tipLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(12f));
        l.setForeground(new Color(0x606266));
        return l;
    }

    private static JPanel buttonRow(JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (JComponent c : items) p.add(c);
        return p;
    }

    private static JPanel labeled(String text, JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.add(new JLabel(text));
        p.add(c);
        return p;
    }

    /** 构造一个标签页主体：说明文字 + 表格（保持自然高度） + 可选控件行。 */
    private static JPanel section(String tip, JComponent table, JComponent... extras) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel l = new JLabel("<html><body style='width:640px'>" + tip + "</body></html>");
        l.setFont(l.getFont().deriveFont(12f));
        l.setBorder(new EmptyBorder(0, 0, 4, 0));
        p.add(l, BorderLayout.NORTH);

        // FlowLayout 保持表格自身 preferred 尺寸；BorderLayout 会拉伸宽度，
        // 使冻结列示例的视口宽于内容，反而看不到横向滚动条与冻结效果。
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.add(table);
        p.add(holder, BorderLayout.CENTER);

        if (extras.length > 0) {
            JPanel south = new JPanel();
            south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
            for (JComponent c : extras) { c.setAlignmentX(Component.LEFT_ALIGNMENT); south.add(c); south.add(Box.createVerticalStrut(6)); }
            p.add(south, BorderLayout.SOUTH);
        }
        return p;
    }

    // ===================== 自检 =====================

    private static JPanel[] allSections() {
        return new JPanel[]{section1(), section2(), section3(), section4(), section5(),
            section6(), section7(), section8(), section9(), sectionAll()};
    }

    /** 离屏构建并绘制全部标签页，验证各能力组合不抛异常。 */
    static void selfCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JFrame jf = new JFrame("AstTableDemo SC");
                    jf.setSize(960, 700);
                    jf.setVisible(true);
                    try {
                        JPanel[] sections = allSections();
                        for (int i = 0; i < sections.length; i++) {
                            JPanel p = sections[i];
                            jf.setContentPane(new JScrollPane(p));
                            jf.validate();
                            Dimension d = p.getPreferredSize();
                            int w = Math.max(1, d.width), h = Math.max(1, d.height);
                            p.setSize(w, h);
                            p.doLayout();
                            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g = img.createGraphics();
                            try { p.paint(g); } finally { g.dispose(); }
                        }
                        assert sections.length == 10 : "AstTableDemo 应有 10 个标签页";
                    } finally {
                        jf.dispose();
                    }
                }
            });
        } catch (Throwable t) {
            err[0] = t;
        }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstTableDemo self-check OK");
    }
}
