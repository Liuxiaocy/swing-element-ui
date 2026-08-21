package org.swelement.demo;

import org.swelement.core.AnimatedPopup;
import org.swelement.core.ElementTheme;
import org.swelement.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 统一演示 AstTooltip / AstDropdown / AstDialog / AstMessageBox / AstMessage。
 * 共 6 个面板 Section。
 */
public class AstPopupDemo {
    private static final Component SPACER_16 = Box.createVerticalStrut(16);
    private static final Component SPACER_12 = Box.createVerticalStrut(12);
    private static JLabel echo; // bottom status label

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); }});
    }

    private static void start() {
        JFrame f = new JFrame("AstPopup Demo - Tooltip / Dropdown / Dialog / MessageBox / Message");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(16, 24, 16, 24));

        // Section 1: Tooltip
        JPanel s1 = section("Section 1: AstTooltip 四方向 × 两种主题 (鼠标悬停按钮 200ms 后显示)");
        JPanel btnGrid = new JPanel(new GridLayout(2, 4, 16, 16));
        btnGrid.setBorder(new EmptyBorder(12, 12, 12, 12));
        Object[][] buttons = new Object[][]{
            {"上方 DARK",   AnimatedPopup.Direction.ABOVE, AstTooltip.Effect.DARK},
            {"下方 DARK",   AnimatedPopup.Direction.BELOW, AstTooltip.Effect.DARK},
            {"左侧 DARK",   AnimatedPopup.Direction.LEFT,  AstTooltip.Effect.DARK},
            {"右侧 DARK",   AnimatedPopup.Direction.RIGHT, AstTooltip.Effect.DARK},
            {"上方 LIGHT",  AnimatedPopup.Direction.ABOVE, AstTooltip.Effect.LIGHT},
            {"下方 LIGHT",  AnimatedPopup.Direction.BELOW, AstTooltip.Effect.LIGHT},
            {"左侧 LIGHT",  AnimatedPopup.Direction.LEFT,  AstTooltip.Effect.LIGHT},
            {"右侧 LIGHT",  AnimatedPopup.Direction.RIGHT, AstTooltip.Effect.LIGHT},
        };
        for (Object[] row : buttons) {
            JButton b = new JButton((String) row[0]);
            b.setPreferredSize(new Dimension(160, 44));
            String tip = "这是一条示例文字，演示" + row[0] + "效果；较长文本会自动省略号截断";
            AstTooltip.attach(b, tip, (AnimatedPopup.Direction) row[1], (AstTooltip.Effect) row[2]);
            btnGrid.add(b);
        }
        s1.add(btnGrid, BorderLayout.CENTER);
        root.add(s1); root.add(SPACER_12);

        // Section 2: Dropdown — 3 columns
        JPanel s2 = section("Section 2: AstDropdown 下拉菜单 (三栏：基础 / 长列表滚动 / Right 方向 Action Echo)");
        JPanel cols = new JPanel(new GridLayout(1, 3, 20, 0)); cols.setBorder(new EmptyBorder(16, 16, 16, 16));

        // 2a Basic (below direction, 4 items)
        JPanel col1 = titled("基础下拉 (4 项, 下方)");
        final AstDropdown.Item[] basicItems = new AstDropdown.Item[]{
            new AstDropdown.Item("添加联系人", new ActionListener() { public void actionPerformed(ActionEvent e) { echo("Dropdown → 添加联系人"); }}),
            new AstDropdown.Item("导入数据",    new ActionListener() { public void actionPerformed(ActionEvent e) { echo("Dropdown → 导入数据"); }}),
            new AstDropdown.Item("导出 Excel",  new ActionListener() { public void actionPerformed(ActionEvent e) { echo("Dropdown → 导出 Excel"); }}),
            new AstDropdown.Item("删除分组",    new ActionListener() { public void actionPerformed(ActionEvent e) { echo("Dropdown → 删除分组"); }}),
        };
        AstDropdown dd1 = new AstDropdown("操作 ▸", basicItems);
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT)); row1.add(dd1);
        col1.add(row1, BorderLayout.NORTH);

        // 2b Long list (above direction, 12 items) — test scroll
        JPanel col2 = titled("长列表 (12 项, 上方, 自动滚动条)");
        AstDropdown.Item[] longItems = new AstDropdown.Item[12];
        for (int i = 0; i < 12; i++) {
            final int k = i + 1;
            longItems[i] = new AstDropdown.Item("历史记录 — 第 " + k + " 条",
                new ActionListener() { public void actionPerformed(ActionEvent e) { echo("长列表点击：第 " + k + " 条"); }});
        }
        AstDropdown dd2 = new AstDropdown("最近 ▴", longItems, AnimatedPopup.Direction.ABOVE);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT)); row2.add(dd2);
        col2.add(row2, BorderLayout.SOUTH);

        // 2c Right direction dropdown with 4 items + action echo
        JPanel col3 = titled("右侧方向下拉 (Action Echo 右面板)");
        final JTextArea echoArea = new JTextArea("点击菜单项后，这里记录动作日志\n");
        echoArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12)); echoArea.setEditable(false);
        echoArea.setForeground(ElementTheme.TEXT_REGULAR);
        echoArea.setOpaque(false);
        JScrollPane ep = new JScrollPane(echoArea); ep.setBorder(null);
        AstDropdown.Item[] rightItems = new AstDropdown.Item[]{
            new AstDropdown.Item("刷新缓存", new ActionListener() { public void actionPerformed(ActionEvent e) { log(echoArea, "[刷新缓存] 成功清除 1,248 条缓存条目"); }}),
            new AstDropdown.Item("强制登出", new ActionListener() { public void actionPerformed(ActionEvent e) { log(echoArea, "[强制登出] 所有活动会话已终止"); }}),
            new AstDropdown.Item("查看系统日志", new ActionListener() { public void actionPerformed(ActionEvent e) { log(echoArea, "[系统日志] 今天 42 条 WARN，2 条 ERROR"); }}),
            new AstDropdown.Item("重启服务",     new ActionListener() { public void actionPerformed(ActionEvent e) { log(echoArea, "[重启服务] 服务正在重启，预计 30s…"); }}),
        };
        AstDropdown dd3 = new AstDropdown("系统操作", rightItems, AnimatedPopup.Direction.RIGHT);
        JPanel topR = new JPanel(new FlowLayout(FlowLayout.LEFT)); topR.add(dd3);
        col3.add(topR, BorderLayout.NORTH);
        col3.add(ep, BorderLayout.CENTER);

        cols.add(col1); cols.add(col2); cols.add(col3);
        s2.add(cols, BorderLayout.CENTER);
        root.add(s2); root.add(SPACER_12);

        // Section 3: AstDialog — 2 buttons
        JPanel s3 = section("Section 3: AstDialog 模态对话框 (自定义内容 / 空内容)");
        JPanel s3row = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 16));
        final JFrame owner = f;
        JButton openForm = new JButton("打开表单对话框 (3 个 JTextField)");
        JButton openEmpty = new JButton("打开空内容对话框");
        s3row.add(openForm); s3row.add(openEmpty);
        s3.add(s3row, BorderLayout.NORTH);

        openForm.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            JPanel body = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(6, 0, 6, 8);
            gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            gbc.gridy = 0; body.add(new JLabel("姓名："), gbc);
            gbc.gridy = 1; body.add(new JLabel("邮箱："), gbc);
            gbc.gridy = 2; body.add(new JLabel("电话："), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.insets = new Insets(6, 0, 6, 0);
            final JTextField tf1 = new JTextField("", 24); tf1.setText("王小明");
            final JTextField tf2 = new JTextField("", 24); tf2.setText("ming.wang@company.com");
            final JTextField tf3 = new JTextField("", 24); tf3.setText("138-0000-0000");
            gbc.gridy = 0; body.add(tf1, gbc);
            gbc.gridy = 1; body.add(tf2, gbc);
            gbc.gridy = 2; body.add(tf3, gbc);
            AstDialog.show(owner, "编辑联系人信息", "保存", "取消", body, new AstDialog.ResultCallback() {
                public void onResult(int resultCode) {
                    if (resultCode == AstDialog.RESULT_OK) echo("Dialog → 已保存：姓名=" + tf1.getText() + " 邮箱=" + tf2.getText());
                    else echo("Dialog → 用户取消编辑");
                }
            });
        }});

        openEmpty.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            JPanel body = new JPanel(new BorderLayout());
            JLabel center = new JLabel("（自定义占位区：可嵌入任意内容）", JLabel.CENTER);
            center.setForeground(ElementTheme.TEXT_PLACEHOLDER);
            body.add(center, BorderLayout.CENTER);
            AstDialog.show(owner, "空对话框标题", body, new AstDialog.ResultCallback() {
                public void onResult(int resultCode) {
                    echo("Dialog → resultCode=" + resultCode + " (" + (resultCode == AstDialog.RESULT_OK ? "确定" : "取消") + ")");
                }
            });
        }});
        root.add(s3); root.add(SPACER_12);

        // Section 4: AstMessageBox
        JPanel s4 = section("Section 4: AstMessageBox — 5 种类型 + Confirm 对话回调");
        JPanel alertsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        alertsRow.setBorder(new EmptyBorder(8, 8, 8, 8));
        alertsRow.add(new JButtonMaker() {
            @Override public String label() { return "信息 INFO"; }
            @Override public Color bg() { return ElementTheme.PRIMARY; }
            @Override public void onClick() { AstMessageBox.alert(owner, AstMessageBox.MessageBoxType.INFO, "这是一条信息提示。"); }
        }.make());
        alertsRow.add(new JButtonMaker() {
            @Override public String label() { return "成功 SUCCESS"; }
            @Override public Color bg() { return ElementTheme.SUCCESS; }
            @Override public void onClick() { AstMessageBox.alert(owner, AstMessageBox.MessageBoxType.SUCCESS, "操作成功！你的订单已提交。"); }
        }.make());
        alertsRow.add(new JButtonMaker() {
            @Override public String label() { return "警告 WARNING"; }
            @Override public Color bg() { return ElementTheme.WARNING; }
            @Override public void onClick() { AstMessageBox.alert(owner, AstMessageBox.MessageBoxType.WARNING, "磁盘空间已使用 85%。请及时清理。"); }
        }.make());
        alertsRow.add(new JButtonMaker() {
            @Override public String label() { return "错误 ERROR"; }
            @Override public Color bg() { return ElementTheme.DANGER; }
            @Override public void onClick() { AstMessageBox.alert(owner, AstMessageBox.MessageBoxType.ERROR, "连接数据库失败：超时。检查网络或重试。"); }
        }.make());
        alertsRow.add(new JButtonMaker() {
            @Override public String label() { return "提问 QUESTION"; }
            @Override public Color bg() { return ElementTheme.PRIMARY; }
            @Override public void onClick() { AstMessageBox.alert(owner, AstMessageBox.MessageBoxType.QUESTION, "确定帮助？"); }
        }.make());
        s4.add(alertsRow, BorderLayout.NORTH);
        JPanel confirmRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        confirmRow.setBorder(new EmptyBorder(4, 8, 12, 8));
        final JLabel confirmLabel = new JLabel("等待操作", JLabel.LEFT);
        confirmLabel.setFont(confirmLabel.getFont().deriveFont(12f)); confirmLabel.setForeground(ElementTheme.TEXT_REGULAR);
        JButton confirmBtn = new JButton("Confirm — 确认删除？（QUESTION）"); confirmBtn.setForeground(Color.WHITE); confirmBtn.setBackground(ElementTheme.PRIMARY);
        JButton confirmType = new JButton("Confirm — 是否启用？（WARNING）"); confirmType.setForeground(Color.WHITE); confirmType.setBackground(ElementTheme.WARNING);
        confirmBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            AstMessageBox.confirm(owner, "您真的要删除选中的 3 条记录吗？此操作不可撤销。", new AstMessageBox.ConfirmCallback() {
                public void onConfirm() { confirmLabel.setText("✅ 确认：3 条记录已删除"); echo("MessageBox Confirm → 用户确认删除"); }
                public void onCancel()  { confirmLabel.setText("🚫 取消：用户取消操作");      echo("MessageBox Confirm → 用户取消"); }
            });
        }});
        confirmType.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            AstMessageBox.confirm(owner, AstMessageBox.MessageBoxType.WARNING, "启用此开关会开启 Beta 版新特性。是否继续？", new AstMessageBox.ConfirmCallback() {
                public void onConfirm() { confirmLabel.setText("✅ 启用：已开启 Beta 版特性"); echo("MessageBox Confirm → WARNING 确认开启 Beta"); }
                public void onCancel()  { confirmLabel.setText("🚫 取消：保持现有设置");       echo("MessageBox Confirm → WARNING 用户取消"); }
            });
        }});
        confirmRow.add(confirmBtn); confirmRow.add(confirmType); confirmRow.add(confirmLabel);
        s4.add(confirmRow, BorderLayout.CENTER);
        root.add(s4); root.add(SPACER_12);

        // Section 5: AstMessage toast
        JPanel s5 = section("Section 5: AstMessage — 4 种类型 Toast + 时长测试 (顶部居中堆叠)");
        JPanel s5row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        s5row.setBorder(new EmptyBorder(12, 12, 12, 12));
        ActionListener toast = new ActionListener() { public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if ("INFO".equals(cmd))    AstMessage.show(owner, AstMessage.MessageType.INFO,    "普通通知：任务已排入处理队列（3s 自动关闭）");
            if ("SUCCESS".equals(cmd)) AstMessage.show(owner, AstMessage.MessageType.SUCCESS, "保存成功！");
            if ("WARN".equals(cmd))    AstMessage.show(owner, AstMessage.MessageType.WARNING, "系统将于今晚 23:00 重启");
            if ("ERROR".equals(cmd))   AstMessage.show(owner, AstMessage.MessageType.ERROR,   "服务器返回 500 Internal Server Error");
            if ("500".equals(cmd))     AstMessage.show(owner, AstMessage.MessageType.WARNING, "快闪：我只显示 0.5s", 500);
            if ("6000".equals(cmd))    AstMessage.show(owner, AstMessage.MessageType.INFO, "长消息：我会显示 6 秒，请不要错过！", 6000);
            if ("BATCH".equals(cmd)) {
                AstMessage.show(owner, AstMessage.MessageType.INFO, "第一条");
                AstMessage.show(owner, AstMessage.MessageType.SUCCESS, "第二条 — 堆叠偏移 56px");
                AstMessage.show(owner, AstMessage.MessageType.WARNING, "第三条 — 测试三条一起");
            }
        }};
        Object[][] toastBtns = new Object[][]{
            {"INFO 信息", "INFO", ElementTheme.PRIMARY},
            {"SUCCESS 成功", "SUCCESS", ElementTheme.SUCCESS},
            {"WARNING 警告", "WARN", ElementTheme.WARNING},
            {"ERROR 错误", "ERROR", ElementTheme.DANGER},
            {"⏱ 快闪 0.5s", "500", new Color(0x909399)},
            {"⏳ 长时 6s", "6000", new Color(0x606266)},
            {"🧩 一次 3 条堆叠", "BATCH", new Color(0x1F2D3D)},
        };
        for (Object[] t : toastBtns) {
            JButton btn = new JButton((String) t[0]);
            btn.setForeground(Color.WHITE);
            btn.setBackground((Color) t[2]);
            btn.setFocusPainted(false); btn.setOpaque(true); btn.setBorderPainted(false);
            btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
            btn.setPreferredSize(new Dimension(160, 40));
            btn.setActionCommand((String) t[1]);
            btn.addActionListener(toast);
            s5row.add(btn);
        }
        s5.add(s5row, BorderLayout.CENTER);
        root.add(s5); root.add(SPACER_12);

        // Section 6: Combined — AstLoading + AstMessage + AstDialog + AstTooltip
        JPanel s6 = section("Section 6: 综合 — AstCard(进度) + AstLoading(WRAP) + AstMessage Toast + AstDialog 二次确认 + AstTooltip 悬停提示");
        JPanel s6row = new JPanel(new GridBagLayout()); s6row.setBorder(new EmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;

        // Left: AstCard "上传作业" with progress body wrapped in WRAP loader
        AstCard taskCard = new AstCard("上传作业");
        final Progress progress = new Progress(0);
        JLabel taskLabel = new JLabel("当前：未开始", JLabel.LEFT); taskLabel.setForeground(ElementTheme.TEXT_REGULAR);
        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(taskLabel); body.add(Box.createVerticalStrut(10)); body.add(progress);
        taskCard.setContent(body);
        final JButton startBtn = new JButton("▶ 开始上传");
        final JButton cancelBtn = new JButton("■ 取消任务"); cancelBtn.setEnabled(false);
        final AstLoading loader = new AstLoading(AstLoading.Mode.WRAP, taskCard);
        startBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            // Step 1: show loading + success toast
            AstMessage.show(owner, AstMessage.MessageType.INFO, "开始上传作业（综合示例）…", 900);
            loader.showLoading("上传初始化…");
            startBtn.setEnabled(false); cancelBtn.setEnabled(true);
            progress.setValue(0);
            // Step 2: timer advances progress; at 90% hide loader; 100% show confirm Dialog via MessageBox
            final Timer tm = new Timer(180, null); final int[] tick = {0};
            final Runnable cancelRef = new Runnable() { public void run() { tm.stop(); }};
            cancelBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e2) {
                cancelRef.run(); cancelBtn.setEnabled(false); startBtn.setEnabled(true); loader.hideLoading(); taskLabel.setText("当前：用户已取消");
                AstMessage.show(owner, AstMessage.MessageType.WARNING, "任务已取消", 900);
            }});
            tm.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) {
                tick[0]++;
                int p = (int) Math.min(99, Math.round(100 * (1 - Math.exp(-tick[0] / 5.0))));
                progress.setValue(p);
                taskLabel.setText("当前：已上传 " + p + "%，段次 " + tick[0]);
                if (p >= 90) loader.hideLoading();
                if (p >= 99) {
                    tm.stop();
                    cancelBtn.setEnabled(false);
                    // Step 3: MessageBox confirm "发布到预发环境？"
                    AstMessageBox.confirm(owner, "✅ 上传完成！是否立即发布到预发环境？", new AstMessageBox.ConfirmCallback() {
                        public void onConfirm() {
                            AstDialog.show(owner, "发布确认", "确认发布", "再想想", new JLabel("确认后将发布到环境 preprod-01，无法撤回。", JLabel.CENTER), new AstDialog.ResultCallback() {
                                public void onResult(int r) {
                                    if (r == AstDialog.RESULT_OK) {
                                        AstMessage.show(owner, AstMessage.MessageType.SUCCESS, "已发布到 preprod-01");
                                        taskLabel.setText("当前：✅ 已发布到 preprod-01");
                                        progress.setValue(100);
                                    } else {
                                        AstMessage.show(owner, AstMessage.MessageType.INFO, "已暂存本地，未发布");
                                        taskLabel.setText("当前：暂存本地（未发布）");
                                    }
                                    startBtn.setEnabled(true);
                                }
                            });
                        }
                        public void onCancel() {
                            AstMessage.show(owner, AstMessage.MessageType.INFO, "取消发布，暂存本地");
                            taskLabel.setText("当前：暂存本地（未发布）");
                            startBtn.setEnabled(true);
                        }
                    });
                }
            }});
            tm.start();
        }});
        // Attach tooltip to start button
        AstTooltip.attach(startBtn, "点击开始模拟指数分布上传任务（共 99 段）");
        AstTooltip.attach(cancelBtn, "点击停止当前任务，并重置进度条");
        JPanel taskPanel = new JPanel(new BorderLayout(8, 8));
        JPanel ctrl6 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        ctrl6.add(startBtn); ctrl6.add(cancelBtn);
        taskPanel.add(ctrl6, BorderLayout.NORTH);
        taskPanel.add(loader, BorderLayout.CENTER);
        s6row.add(taskPanel, gbc);
        s6.add(s6row, BorderLayout.CENTER);
        root.add(s6); root.add(SPACER_16);

        // Echo label (bottom)
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new TitledBorder("日志输出（所有动作最终都在下面打印一行）"));
        echo = new JLabel("（等待操作…）", JLabel.LEFT);
        echo.setForeground(ElementTheme.TEXT_REGULAR); echo.setFont(echo.getFont().deriveFont(12f));
        bottom.add(echo, BorderLayout.CENTER);
        root.add(bottom);

        f.setContentPane(new JScrollPane(root));
        f.pack(); f.setSize(Math.max(1200, f.getWidth()), Math.min(900, f.getHeight()));
        f.setLocationRelativeTo(null); f.setVisible(true);
    }

    // ------------------ helpers ------------------
    private static JPanel section(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(title));
        return p;
    }

    private static JPanel titled(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        return p;
    }

    private static void echo(String s) {
        if (echo != null) echo.setText("  " + s + "  @ " + new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date()));
        System.out.println("[ECHO] " + s);
    }

    private static void log(JTextArea area, String line) {
        area.append(line); area.append("\n");
        area.setCaretPosition(area.getDocument().getLength());
        echo("下拉动作日志 → " + line);
    }

    /** Helper: anonymous class factory to make themed borderless JButton with color background. */
    private static abstract class JButtonMaker {
        public abstract String label();
        public abstract Color bg();
        public abstract void onClick();
        public JButton make() {
            final JButton b = new JButton(label());
            Color fg = ElementTheme.pickTextColorForBg(bg());
            b.setForeground(fg); b.setBackground(bg());
            b.setFocusPainted(false); b.setOpaque(true); b.setBorderPainted(false);
            b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
            b.setPreferredSize(new Dimension(160, 40));
            b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { onClick(); }});
            return b;
        }
    }
}
