package org.swelement.demo;

import org.swelement.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * 高级组件综合 Demo — 集成 AstForm（表单校验）、AstTree（树形）、AstTable（表格）。
 *
 * 布局：
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │ 控制区：echo + 操作提示                                       │
 *   ├──────────────┬──────────────────────┬────────────────────────┤
 *   │ AstTree 树形  │ AstTable 表格(斑马纹) │ AstForm 表单(校验)      │
 *   │ 展开/折叠按钮  │ 选中行 echo          │ 提交校验 + 清空        │
 *   └──────────────┴──────────────────────┴────────────────────────┘
 *
 * 交互：每个组件均配按钮触发动画/行为，底部 echo 实时反馈。
 */
public class AstAdvancedDemo {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstAdvanced Demo - 表单/树形/表格综合");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // --- 顶部控制区 ---
        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        ctrl.setBorder(new TitledBorder("控制区（点击下方按钮观察动画与交互）"));
        final JLabel echo = new JLabel("就绪。请操作左侧树形、中间表格或右侧表单。");
        echo.setForeground(new Color(0x606266));
        echo.setFont(echo.getFont().deriveFont(13f));
        ctrl.add(echo);

        // --- 左：AstTree ---
        AstTree.TreeNode fileRoot = buildSampleTree();
        final AstTree tree = new AstTree(fileRoot);
        tree.setNodeClickListener(n -> echo.setText("树形：点击节点「" + n.label + "」"));
        tree.setNodeToggleListener(n -> echo.setText("树形：节点「" + n.label + "」" + (n.isExpanded() ? "已展开" : "已折叠")));

        JPanel treePanel = new JPanel(new BorderLayout(0, 8));
        treePanel.setBorder(new TitledBorder("AstTree 树形控件"));
        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new Dimension(260, 360));
        treePanel.add(treeScroll, BorderLayout.CENTER);
        JPanel treeBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        AstButton expandAllBtn = new AstButton("全部展开", AstButton.DEFAULT, false);
        AstButton collapseAllBtn = new AstButton("全部折叠", AstButton.DEFAULT, false);
        expandAllBtn.addActionListener(e -> { tree.expandAll(); echo.setText("树形：已全部展开"); });
        collapseAllBtn.addActionListener(e -> { tree.collapseAll(); echo.setText("树形：已全部折叠"); });
        treeBtns.add(expandAllBtn); treeBtns.add(collapseAllBtn);
        treePanel.add(treeBtns, BorderLayout.SOUTH);

        // --- 中：AstTable ---
        AstTable.Column[] cols = {
            new AstTable.Column("姓名", 100, AstTable.Align.LEFT),
            new AstTable.Column("年龄", 60, AstTable.Align.CENTER),
            new AstTable.Column("部门", 120, AstTable.Align.LEFT),
            new AstTable.Column("状态", 70, AstTable.Align.CENTER),
        };
        final AstTable table = new AstTable(cols);
        table.addRow("张三", 28, "平台研发部", "在职");
        table.addRow("李四", 34, "产品设计部", "在职");
        table.addRow("王五", 22, "平台研发部", "试用期");
        table.addRow("赵六", 45, "运营部", "在职");
        table.addRow("孙七", 31, "产品设计部", "休假");
        table.setRowClickListener(row -> echo.setText("表格：选中第 " + (row + 1) + " 行「" + table.getValueAt(row, 0) + "」"));

        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.setBorder(new TitledBorder("AstTable 表格（斑马纹+列宽）"));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(420, 360));
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        JPanel tableBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        AstButton addRowBtn = new AstButton("➕ 添加行", AstButton.PRIMARY, false);
        AstButton clearBtn = new AstButton("清空", AstButton.DANGER, false);
        final int[] seq = {1};
        addRowBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = "新员工" + (seq[0]++);
                int age = 20 + (int)(Math.random() * 30);
                String[] depts = {"平台研发部", "产品设计部", "运营部", "市场部"};
                String dept = depts[(int)(Math.random() * depts.length)];
                String[] status = {"在职", "试用期", "休假"};
                String st = status[(int)(Math.random() * status.length)];
                table.addRow(name, age, dept, st);
                echo.setText("表格：新增行「" + name + "」，当前共 " + table.getRowCount() + " 行");
            }
        });
        clearBtn.addActionListener(e -> { table.clearRows(); echo.setText("表格：已清空所有行"); });
        tableBtns.add(addRowBtn); tableBtns.add(clearBtn);
        tablePanel.add(tableBtns, BorderLayout.SOUTH);

        // --- 右：AstForm ---
        final AstForm form = new AstForm();
        final JTextField nameField = new JTextField(16);
        final JTextField emailField = new JTextField(16);
        final JTextField phoneField = new JTextField(16);
        final JTextField noteField = new JTextField(16);
        form.addField("姓名", nameField, new AstForm.RequiredRule(), new AstForm.MinLengthRule(2));
        form.addField("邮箱", emailField, new AstForm.RequiredRule(), new AstForm.EmailRule());
        form.addField("手机", phoneField, new AstForm.RequiredRule(), new AstForm.PhoneRule());
        form.addField("备注", noteField, new AstForm.MaxLengthRule(20));

        JPanel formPanel = new JPanel(new BorderLayout(0, 8));
        formPanel.setBorder(new TitledBorder("AstForm 表单校验"));
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setPreferredSize(new Dimension(300, 360));
        formScroll.setBorder(null);
        formPanel.add(formScroll, BorderLayout.CENTER);
        JPanel formBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        AstButton submitBtn = new AstButton("提交校验", AstButton.PRIMARY, false);
        AstButton resetBtn = new AstButton("清空错误", AstButton.DEFAULT, false);
        AstButton fillBtn = new AstButton("填示例", AstButton.DEFAULT, false);
        submitBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean ok = form.validateForm();
                if (ok) {
                    echo.setText("表单：校验通过 ✓ 姓名=" + nameField.getText());
                } else {
                    Map<String, String> errs = form.getErrors();
                    echo.setText("表单：校验失败，" + errs.size() + " 项错误 — " + errs.values().iterator().next());
                }
            }
        });
        resetBtn.addActionListener(e -> { form.clearAllErrors(); echo.setText("表单：已清空错误提示"); });
        fillBtn.addActionListener(e -> {
            nameField.setText("李逍遥");
            emailField.setText("xiaoyao@example.com");
            phoneField.setText("13800138000");
            noteField.setText("示例备注");
            form.clearAllErrors();
            echo.setText("表单：已填入示例数据，可点击「提交校验」");
        });
        formBtns.add(submitBtn); formBtns.add(resetBtn); formBtns.add(fillBtn);
        formPanel.add(formBtns, BorderLayout.SOUTH);

        // --- 三列并排 ---
        JPanel cols2 = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 6, 0, 6);
        gbc.gridx = 0; gbc.weightx = 0.0; cols2.add(treePanel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; cols2.add(tablePanel, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0; cols2.add(formPanel, gbc);

        root.add(ctrl, BorderLayout.NORTH);
        root.add(cols2, BorderLayout.CENTER);
        f.setContentPane(new JScrollPane(root));
        f.pack();
        f.setSize(Math.max(f.getWidth(), 1100), Math.min(f.getHeight(), 720));
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    /** 构建示例树：项目目录结构 */
    private static AstTree.TreeNode buildSampleTree() {
        AstTree.TreeNode root = new AstTree.TreeNode("项目根目录");
        AstTree.TreeNode src = new AstTree.TreeNode("src");
        AstTree.TreeNode main = new AstTree.TreeNode("main");
        main.addChild(new AstTree.TreeNode("java"));
        main.addChild(new AstTree.TreeNode("resources"));
        src.addChild(main);
        AstTree.TreeNode test = new AstTree.TreeNode("test");
        test.addChild(new AstTree.TreeNode("java"));
        src.addChild(test);
        src.setExpanded(true);
        root.addChild(src);
        AstTree.TreeNode docs = new AstTree.TreeNode("docs");
        docs.addChild(new AstTree.TreeNode("design.md"));
        docs.addChild(new AstTree.TreeNode("api.md"));
        root.addChild(docs);
        AstTree.TreeNode conf = new AstTree.TreeNode("config");
        conf.addChild(new AstTree.TreeNode("application.yml"));
        conf.addChild(new AstTree.TreeNode("logback.xml"));
        root.addChild(conf);
        return root;
    }
}
