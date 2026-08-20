package org.swelement.demo;

import org.swelement.ui.Button;
import org.swelement.ui.Tabs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class TabsDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Tabs Demo - 下划线滑动动画、hover 颜色过渡、切换内容");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 20, 24));

            // Tab 1：4个页面
            Tabs tabs = new Tabs(new String[]{"📋 基本信息", "⚙️ 系统设置", "🔐 安全中心", "📜 操作日志"}, 0);
            // 为每个 tab 创建对应内容面板
            JPanel contentHolder = new JPanel(new CardLayout());
            JPanel[] contents = {makeInfo(), makeSettings(), makeSecurity(), makeLog()};
            String[] keys = {"T0", "T1", "T2", "T3"};
            for (int i = 0; i < 4; i++) contentHolder.add(contents[i], keys[i]);
            ((CardLayout) contentHolder.getLayout()).show(contentHolder, keys[0]);
            tabs.addChangeListener(e -> {
                int idx = tabs.getSelectedIndex();
                if (idx >= 0 && idx < keys.length)
                    ((CardLayout) contentHolder.getLayout()).show(contentHolder, keys[idx]);
            });

            JPanel view = new JPanel();
            view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));
            view.setBorder(new TitledBorder("4 个 Tab（切换 Tab 观察底部蓝色下划线平滑滑动至目标位置）"));
            view.add(tabs);
            view.add(Box.createVerticalStrut(10));
            view.add(contentHolder);

            // Tab 2：3个简单 tab + 控制按钮
            Tabs tabs2 = new Tabs(new String[]{"Tab A", "Tab B", "Tab C"}, 1);
            JLabel tab2Echo = new JLabel("当前：Tab B");
            tab2Echo.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            tab2Echo.setForeground(new Color(0x409EFF));
            tabs2.addChangeListener(e -> tab2Echo.setText("当前：" + tabs2.getSelectedTitle()));

            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            ctrl.setBorder(new TitledBorder("外部按钮控制 Tab 切换（观察下划线从当前位置动画滑到目标位置）"));
            Button goto0 = new Button("切到第 1 个", Button.DEFAULT, true);
            Button goto1 = new Button("切到第 2 个", Button.DEFAULT, true);
            Button goto2 = new Button("切到第 3 个", Button.DEFAULT, true);
            Button prev = new Button("◀ 上一个", Button.WARNING, true);
            Button next = new Button("下一个 ▶", Button.SUCCESS, true);
            goto0.addActionListener(e -> tabs.setSelectedIndex(0));
            goto1.addActionListener(e -> tabs.setSelectedIndex(1));
            goto2.addActionListener(e -> tabs.setSelectedIndex(2));
            prev.addActionListener(e -> {
                int i = tabs.getSelectedIndex() - 1;
                if (i < 0) i = 3;
                tabs.setSelectedIndex(i);
            });
            next.addActionListener(e -> {
                int i = tabs.getSelectedIndex() + 1;
                if (i > 3) i = 0;
                tabs.setSelectedIndex(i);
            });
            ctrl.add(goto0); ctrl.add(goto1); ctrl.add(goto2);
            ctrl.add(Box.createHorizontalStrut(20));
            ctrl.add(prev); ctrl.add(next);

            JPanel miniView = new JPanel();
            miniView.setLayout(new BoxLayout(miniView, BoxLayout.Y_AXIS));
            miniView.setBorder(new TitledBorder("3 个迷你 Tab + 当前选中值回显"));
            miniView.add(tabs2);
            miniView.add(Box.createVerticalStrut(8));
            JPanel echoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            echoRow.add(new JLabel("选中状态："));
            echoRow.add(tab2Echo);
            miniView.add(echoRow);

            root.add(view);
            root.add(Box.createVerticalStrut(10));
            root.add(ctrl);
            root.add(Box.createVerticalStrut(8));
            root.add(miniView);

            f.setContentPane(root);
            f.pack();
            f.setSize(Math.max(f.getWidth(), 900), f.getHeight());
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    private static JPanel makeInfo() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(10, 16, 10, 16));
        p.setBackground(new Color(0xFFFFFF));
        p.setOpaque(true);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 18);
        gbc.anchor = GridBagConstraints.WEST;
        Object[][] rows = {
                {"用户名", "XiaoMing"},
                {"邮箱", "xiaoming@example.com"},
                {"手机号", "138****8888"},
                {"注册时间", "2023-05-18"},
                {"所属部门", "研发中心 / 前端组"}
        };
        for (int i = 0; i < rows.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            JLabel k = new JLabel(rows[i][0] + "：");
            k.setForeground(new Color(0x909399));
            p.add(k, gbc);
            gbc.gridx = 1;
            JLabel v = new JLabel(String.valueOf(rows[i][1]));
            v.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
            p.add(v, gbc);
        }
        return p;
    }

    private static JPanel makeSettings() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        p.setBorder(new EmptyBorder(10, 16, 10, 16));
        p.setBackground(Color.WHITE);
        p.setOpaque(true);
        String[] labels = {"🌞 明亮模式", "🌙 夜间模式", "🎨 跟随系统", "🔊 声音开启", "🔕 静音模式"};
        for (String s : labels) {
            JCheckBox cb = new JCheckBox(s);
            cb.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            p.add(cb);
        }
        return p;
    }

    private static JPanel makeSecurity() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(10, 16, 10, 16));
        p.setBackground(Color.WHITE);
        p.setOpaque(true);
        String[] items = {
                "🔑 修改登录密码 — 上次修改：30 天前  ⚠️",
                "📱 绑定手机号 — 已绑定：138****8888  ✅",
                "📧 绑定邮箱 — 已绑定：xiaoming@example.com  ✅",
                "🛡  两步验证 — 未开启  ❗",
                "🖥  登录设备管理 — 当前在线 2 台设备"
        };
        for (String s : items) {
            JLabel l = new JLabel("•  " + s);
            l.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            l.setBorder(new EmptyBorder(4, 0, 4, 0));
            p.add(l);
        }
        return p;
    }

    private static JPanel makeLog() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 16, 10, 16));
        p.setBackground(Color.WHITE);
        p.setOpaque(true);
        String[] cols = {"时间", "操作", "IP", "状态"};
        Object[][] data = {
                {"2024-05-10 10:22", "登录系统", "192.168.1.105", "成功"},
                {"2024-05-10 11:07", "修改资料", "192.168.1.105", "成功"},
                {"2024-05-10 14:33", "上传文件", "192.168.1.105", "成功"},
                {"2024-05-09 20:11", "登录失败（密码错误）", "221.12.8.45", "失败"},
                {"2024-05-09 09:45", "登录系统", "114.88.22.7", "成功"}
        };
        JTable t = new JTable(data, cols);
        t.setRowHeight(26);
        t.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }
}
