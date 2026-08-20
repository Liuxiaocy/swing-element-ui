package org.swelement.demo;

import org.swelement.ui.Button;
import org.swelement.ui.Pagination;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class PaginationDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Pagination Demo - 页码跳转、hover/active 高亮、首末页淡入");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // 主分页：100 条 / 每页 8 → 13 页
            JPanel p1 = new JPanel();
            p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
            p1.setBorder(new TitledBorder("主分页（共 100 条 / 每页 8 条 → 共 13 页。点击页码观察当前页高亮；点击前后箭头翻页）"));

            JLabel info = new JLabel();
            info.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            info.setForeground(new Color(0x606266));
            info.setBorder(new EmptyBorder(4, 16, 8, 16));

            Pagination pg = new Pagination(100, 8, 1);
            JPanel pgWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            pgWrap.setBorder(new EmptyBorder(6, 16, 10, 16));
            pgWrap.add(pg);

            // 数据展示区（模拟页数据）
            final JTextArea dataArea = new JTextArea(12, 72);
            dataArea.setEditable(false);
            dataArea.setFont(new Font("Consolas", Font.PLAIN, 13));
            dataArea.setBorder(new EmptyBorder(10, 14, 10, 14));
            JScrollPane sp = new JScrollPane(dataArea);
            sp.setBorder(new EmptyBorder(6, 16, 6, 16));
            JPanel dWrap = new JPanel(new BorderLayout());
            dWrap.add(sp, BorderLayout.CENTER);
            dWrap.setBorder(new EmptyBorder(2, 4, 2, 4));

            pg.addChangeListener(e -> {
                int cp = pg.getCurrentPage();
                int tPage = pg.getTotalPages();
                int ps = pg.getPageSize();
                int from = (cp - 1) * ps + 1;
                int to = Math.min(cp * ps, pg.getTotalCount());
                info.setText("第 " + cp + " / " + tPage + " 页，显示第 " + from + " - " + to + " 条（共 " + pg.getTotalCount() + " 条）");
                // 渲染模拟数据
                StringBuilder sb = new StringBuilder();
                sb.append("+-------+----------------------+---------------------+--------------------+\n");
                sb.append("| ID    | 标题                 | 创建者              | 创建时间           |\n");
                sb.append("+-------+----------------------+---------------------+--------------------+\n");
                String[] users = {"Alice", "Bob", "Charlie", "Diana", "Eric", "Fiona", "George", "Hanna"};
                for (int i = from; i <= to; i++) {
                    String u = users[(i - 1) % users.length];
                    String title = "示例数据项 #" + String.format("%04d", i);
                    String time = "2024-" + String.format("%02d", ((i - 1) % 12) + 1) + "-" + String.format("%02d", ((i - 1) % 28) + 1)
                            + " " + String.format("%02d", (i % 24)) + ":" + String.format("%02d", (i * 3 % 60));
                    sb.append(String.format("| %-6d| %-21s| %-20s| %-19s|%n", i, title, u, time));
                }
                sb.append("+-------+----------------------+---------------------+--------------------+\n");
                dataArea.setText(sb.toString());
                dataArea.setCaretPosition(0);
            });
            // 触发一次初始渲染
            pg.setCurrentPage(1);

            // 控制按钮：跳转指定页 / 每页条数
            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
            ctrl.setBorder(new TitledBorder("外部交互控制"));
            JLabel toLbl = new JLabel("跳转到第");
            JSpinner spPage = new JSpinner(new SpinnerNumberModel(1, 1, pg.getTotalPages(), 1));
            spPage.setPreferredSize(new Dimension(70, 30));
            Button go = new Button("跳转", Button.PRIMARY, false);
            go.addActionListener(e -> pg.setCurrentPage((Integer) spPage.getValue()));

            JLabel szLbl = new JLabel("每页条数:");
            String[] sizes = {"3", "5", "8", "10", "15", "20", "50"};
            JComboBox<String> sizeBox = new JComboBox<>(sizes);
            sizeBox.setSelectedItem("8");
            sizeBox.addActionListener(e -> {
                int ps = Integer.parseInt((String) sizeBox.getSelectedItem());
                pg.setPageSize(ps);
                // 更新 spinner 最大页
                spPage.setModel(new SpinnerNumberModel(1, 1, pg.getTotalPages(), 1));
            });

            Button firstB = new Button("⏮ 首页", Button.DEFAULT, true);
            Button lastB = new Button("末页 ⏭", Button.DEFAULT, true);
            Button prevB = new Button("◀ 上一页", Button.WARNING, true);
            Button nextB = new Button("下一页 ▶", Button.SUCCESS, true);
            firstB.addActionListener(e -> pg.setCurrentPage(1));
            lastB.addActionListener(e -> pg.setCurrentPage(pg.getTotalPages()));
            prevB.addActionListener(e -> { int c = pg.getCurrentPage(); if (c > 1) pg.setCurrentPage(c - 1); });
            nextB.addActionListener(e -> { int c = pg.getCurrentPage(); if (c < pg.getTotalPages()) pg.setCurrentPage(c + 1); });

            ctrl.add(toLbl); ctrl.add(spPage); ctrl.add(go);
            ctrl.add(Box.createHorizontalStrut(20));
            ctrl.add(szLbl); ctrl.add(sizeBox);
            ctrl.add(Box.createHorizontalStrut(20));
            ctrl.add(firstB); ctrl.add(prevB); ctrl.add(nextB); ctrl.add(lastB);

            // 小数据量示例
            JPanel small = new JPanel();
            small.setLayout(new BoxLayout(small, BoxLayout.Y_AXIS));
            small.setBorder(new TitledBorder("小数据分页（共 12 条 / 每页 3 条 → 共 4 页，展示前后箭头禁用态）"));
            Pagination spg = new Pagination(12, 3, 1);
            JLabel sgLbl = new JLabel();
            sgLbl.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            sgLbl.setForeground(new Color(0x606266));
            sgLbl.setBorder(new EmptyBorder(4, 16, 8, 16));
            spg.addChangeListener(e -> sgLbl.setText("当前第 " + spg.getCurrentPage() + " 页，共 " + spg.getTotalPages() + " 页（总 " + spg.getTotalCount() + " 条，每页 " + spg.getPageSize() + " 条）"));
            spg.setCurrentPage(1);
            JPanel sw = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            sw.setBorder(new EmptyBorder(6, 16, 10, 16));
            sw.add(spg);
            small.add(sgLbl); small.add(sw);

            p1.add(info);
            p1.add(pgWrap);
            p1.add(dWrap);

            root.add(p1);
            root.add(Box.createVerticalStrut(4));
            root.add(ctrl);
            root.add(Box.createVerticalStrut(8));
            root.add(small);

            f.setContentPane(root);
            f.pack();
            f.setSize(Math.max(f.getWidth(), 960), f.getHeight());
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
