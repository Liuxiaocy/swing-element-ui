package org.swelement.demo;

import org.swelement.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AstLoadingDemo {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstLoading Demo - 加载");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1200, 800); f.setLocationRelativeTo(null);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(16, 24, 16, 24));

        // ========= Demo Section A: WRAP mode over AstCard =========
        JPanel wrapPanel = new JPanel(new GridBagLayout());
        wrapPanel.setBorder(new TitledBorder("模式 A：WRAP 包裹模式（覆盖目标组件）"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.insets = new Insets(0, 14, 0, 14);

        // Card body that gets wrapped by AstLoading
        AstCard profileCard = new AstCard("用户信息面板");
        JPanel body1 = new JPanel(); body1.setLayout(new BoxLayout(body1, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("姓名：李四");  JLabel email = new JLabel("邮箱：lisi@example.com");
        JLabel dept = new JLabel("部门：基础架构部"); JLabel role = new JLabel("角色：SRE 工程师");
        JLabel joinedAt = new JLabel("入职：2021-03-15");
        for (JComponent row : new JComponent[]{name, email, dept, role, joinedAt}) {
            row.setForeground(new Color(0x606266));
            row.setFont(row.getFont().deriveFont(13f));
            body1.add(row); body1.add(Box.createVerticalStrut(4));
        }
        AstProgress perf = new AstProgress(66); perf.setBorder(new EmptyBorder(8, 0, 0, 0));
        body1.add(new JLabel("本月服务可用性")); body1.add(perf);
        profileCard.setContent(body1);
        final AstLoading wrap1 = new AstLoading(AstLoading.Mode.WRAP, profileCard);
        AstButton showWrap1 = new AstButton("显示加载中（WRAP）", AstButton.PRIMARY, false);
        AstButton hideWrap1 = new AstButton("隐藏加载", AstButton.DEFAULT, false);
        final JTextField wrapText1 = new JTextField("数据同步中，请稍候…", 26);
        JButton schedule = new JButton("⏱ 1.8 秒后自动关闭");
        // 控制按钮必须放在 wrap 之外：loading 遮罩会覆盖被包裹的整个卡片，
        // 若按钮留在卡片头部，遮罩出现后即被盖住无法再操作（模式A"未生效"的根因）。

        // === Demo Section B: WRAP mode over simple table/text area ===
        JPanel dataBody = new JPanel(new BorderLayout());
        String[] cols = {"ID", "商品", "单价", "库存"};
        Object[][] rows = new Object[10][4];
        for (int i = 0; i < 10; i++) {
            rows[i][0] = i+1;
            rows[i][1] = "商品 SKU-" + (1000 + i);
            rows[i][2] = "¥ " + (20 + (int)(Math.random()*300));
            rows[i][3] = (int)(Math.random()*200);
        }
        JTable table = new JTable(rows, cols);
        table.setRowHeight(26);
        dataBody.add(new JScrollPane(table), BorderLayout.CENTER);
        AstCard dataCard = new AstCard("商品库存数据");
        dataCard.setContent(dataBody);
        final AstLoading wrap2 = new AstLoading(AstLoading.Mode.WRAP, dataCard);
        AstButton showWrap2 = new AstButton("刷新数据", AstButton.PRIMARY, false);
        AstButton hideWrap2 = new AstButton("停止加载", AstButton.DEFAULT, false);
        final JLabel timeStampLbl = new JLabel("最近刷新：未刷新", JLabel.LEFT);
        timeStampLbl.setFont(timeStampLbl.getFont().deriveFont(12f));
        timeStampLbl.setForeground(new Color(0x909399));
        JPanel timeStuff = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8)); // 控制行在 wrap 外
        timeStuff.add(showWrap2); timeStuff.add(hideWrap2); timeStuff.add(timeStampLbl);

        // 控制行放在 wrap 之外（NORTH），遮罩出现时按钮仍可点击
        final JPanel leftCardWrap = new JPanel(new BorderLayout());
        leftCardWrap.add(wrap1, BorderLayout.CENTER);
        JPanel wrapCtrl1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        wrapCtrl1.add(showWrap1); wrapCtrl1.add(hideWrap1);
        wrapCtrl1.add(new JLabel("自定义 loading 文案：")); wrapCtrl1.add(wrapText1); wrapCtrl1.add(schedule);
        leftCardWrap.add(wrapCtrl1, BorderLayout.NORTH);

        final JPanel rightCardWrap = new JPanel(new BorderLayout()); rightCardWrap.add(wrap2, BorderLayout.CENTER);
        rightCardWrap.add(timeStuff, BorderLayout.SOUTH);

        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.5; wrapPanel.add(leftCardWrap, gbc);
        gbc.gridx = 1; gbc.weightx = 0.5; wrapPanel.add(rightCardWrap, gbc);

        // Wire Section A actions
        showWrap1.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            wrap1.showLoading(wrapText1.getText());
        }});
        hideWrap1.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            wrap1.hideLoading();
        }});
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        schedule.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            wrap1.showLoading(wrapText1.getText() + "（自动关闭）");
            new Timer(1800, new ActionListener() { public void actionPerformed(ActionEvent ev) {
                ((Timer) ev.getSource()).stop(); wrap1.hideLoading();
            }}).start();
        }});

        // Wire Section B actions
        showWrap2.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            wrap2.showLoading("刷新库存列表…");
            new Timer(1200, new ActionListener() { public void actionPerformed(ActionEvent ev) {
                ((Timer) ev.getSource()).stop();
                wrap2.hideLoading();
                timeStampLbl.setText("最近刷新：" + sdf.format(new Date()));
                // Simulate data update: re-randomize stock column
                for (int i = 0; i < table.getRowCount(); i++) table.setValueAt((int)(Math.random()*200), i, 3);
            }}).start();
        }});
        hideWrap2.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { wrap2.hideLoading(); }});

        // ========= Demo Section C: Fullscreen as GlassPane =========
        JPanel fsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        fsPanel.setBorder(new TitledBorder("模式 B：FULLSCREEN 全屏模式（作为 JFrame GlassPane）"));
        JButton openFs = new JButton("🔒 打开全屏加载（模拟提交表单 2s 后关闭）");
        JButton openFsCustom = new JButton("自定义文案全屏加载");
        final JTextField fsText = new JTextField("正在提交您的订单，请勿关闭窗口…", 36);
        final JButton cancelFs = new JButton("取消全屏加载");
        cancelFs.setEnabled(false);
        final AstLoading fsLoader = new AstLoading(AstLoading.Mode.FULLSCREEN, null);
        f.setGlassPane(fsLoader);
        openFs.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            fsLoader.showLoading("正在提交订单，请稍候…");
            cancelFs.setEnabled(true);
            new Timer(2000, new ActionListener() { public void actionPerformed(ActionEvent ev) {
                ((Timer) ev.getSource()).stop();
                fsLoader.hideLoading();
                cancelFs.setEnabled(false);
            }}).start();
        }});
        openFsCustom.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            fsLoader.showLoading(fsText.getText());
            cancelFs.setEnabled(true);
            // 全屏加载会冻结整个窗口（含本按钮），必须自带自动关闭，否则永久锁死
            new Timer(3500, new ActionListener() { public void actionPerformed(ActionEvent ev) {
                ((Timer) ev.getSource()).stop();
                fsLoader.hideLoading();
                cancelFs.setEnabled(false);
            }}).start();
        }});
        cancelFs.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            fsLoader.hideLoading(); cancelFs.setEnabled(false);
        }});
        fsPanel.add(openFs); fsPanel.add(openFsCustom); fsPanel.add(new JLabel("自定义全屏文案："));
        fsPanel.add(fsText); fsPanel.add(cancelFs);

        // ========= Demo Section D: AstProgress panel with loading =========
        JPanel progPanel = new JPanel(new BorderLayout(8, 8));
        progPanel.setBorder(new TitledBorder("综合：WRAP Loading + AstProgress 进度（启动后逐步推进到 100%）"));
        final AstProgress masterProg = new AstProgress(0);
        final JButton startProg = new JButton("▶ 开始模拟上传任务");
        final JButton cancelProg = new JButton("■ 取消任务"); cancelProg.setEnabled(false);
        final JLabel progressEcho = new JLabel("任务状态：待开始", JLabel.LEFT);
        progressEcho.setFont(progressEcho.getFont().deriveFont(12f));
        progressEcho.setForeground(new Color(0x909399));
        // 控制行放 wrap 外：loading 冻结期间按钮仍可点击（取消任务可用）
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topRow.add(startProg); topRow.add(cancelProg); topRow.add(progressEcho);
        progPanel.add(masterProg, BorderLayout.CENTER);
        final AstLoading progWrap = new AstLoading(AstLoading.Mode.WRAP, progPanel);
        startProg.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            startProg.setEnabled(false); cancelProg.setEnabled(true);
            masterProg.setValue(0);
            progressEcho.setText("任务状态：上传初始化中…");
            progWrap.showLoading("上传准备中…");
            new Timer(700, new ActionListener() {
                int ticks = 0;
                public void actionPerformed(ActionEvent ev) {
                    ticks++;
                    int pct = Math.min(99, (int)Math.round(100 * (1 - Math.exp(-ticks / 7.0))));
                    masterProg.setValue(pct);
                    if (pct >= 95) progWrap.hideLoading();
                    progressEcho.setText("任务状态：已完成 " + pct + "%（模拟：分段上传 第 " + ticks + " 段）");
                    if (pct >= 99 || !cancelProg.isEnabled()) {
                        ((Timer) ev.getSource()).stop();
                        if (cancelProg.isEnabled() /* still running, not canceled */) {
                            progressEcho.setText("任务状态：✅ 上传完成 (" + sdf.format(new Date()) + ")");
                            startProg.setEnabled(true); cancelProg.setEnabled(false);
                            progWrap.hideLoading();
                        }
                    }
                }
            }).start();
        }});
        cancelProg.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            cancelProg.setEnabled(false); startProg.setEnabled(true);
            progWrap.hideLoading();
            progressEcho.setText("任务状态：⏹ 已取消");
        }});

        // ========= Demo Section E: 自定义遮罩色 / 指示器尺寸 / 延迟显示 =========
        JPanel customPanel = new JPanel(new BorderLayout(8, 8));
        customPanel.setBorder(new TitledBorder("定制：setBgColor 遮罩色 · setSpinnerSize 指示器尺寸 · setDelay 延迟显示"));

        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 12));
        final AstLoading[] customLoaders = new AstLoading[3];
        final String[] customText = {"深色遮罩 + 大号指示器", "浅色遮罩 + 小号指示器", "延迟 800ms 显示（短请求不闪烁）"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            JLabel body = new JLabel(customText[i], JLabel.CENTER);
            body.setFont(body.getFont().deriveFont(13f));
            body.setPreferredSize(new Dimension(220, 120));
            body.setBorder(BorderFactory.createLineBorder(new Color(0xDCDFE6)));
            final AstLoading cl = new AstLoading(AstLoading.Mode.WRAP, body);
            customLoaders[i] = cl;
            cards.add(cl);
        }
        customLoaders[0].setBgColor(new Color(0x30, 0x31, 0x33, 0xB0)); // 深色半透明遮罩
        customLoaders[0].setSpinnerSize(AstLoading.DEFAULT_SPINNER_SIZE + 28);
        customLoaders[1].setBgColor(new Color(0xFF, 0xFF, 0xFF, 0xE6));
        customLoaders[1].setSpinnerSize(34);
        customLoaders[2].setDelay(800);

        JPanel customCtrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        JButton showAll = new JButton("▶ 全部显示");
        final JCheckBox delayCancel = new JCheckBox("800ms 内立即取消（验证不闪遮罩）", true);
        showAll.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < customLoaders.length; i++) {
                customLoaders[i].showLoading(customText[i]);
            }
            if (delayCancel.isSelected()) {
                // 延迟未到就取消：遮罩全程不出现
                new Timer(200, new ActionListener() { public void actionPerformed(ActionEvent ev) {
                    ((Timer) ev.getSource()).stop();
                    customLoaders[2].hideLoading();
                }}).start();
            }
            new Timer(3000, new ActionListener() { public void actionPerformed(ActionEvent ev) {
                ((Timer) ev.getSource()).stop();
                for (AstLoading cl : customLoaders) cl.hideLoading();
            }}).start();
        }});
        customCtrl.add(showAll);
        customCtrl.add(delayCancel);
        customPanel.add(customCtrl, BorderLayout.NORTH);
        customPanel.add(cards, BorderLayout.CENTER);

        // assemble root
        root.add(wrapPanel); root.add(Box.createVerticalStrut(12));
        root.add(fsPanel); root.add(Box.createVerticalStrut(12));
        root.add(customPanel); root.add(Box.createVerticalStrut(12));
        JPanel progWrapPanel = new JPanel(new BorderLayout());
        progWrapPanel.add(progWrap, BorderLayout.CENTER);
        progWrapPanel.add(topRow, BorderLayout.NORTH); // 控制行在 wrap 外
        root.add(progWrapPanel);
        f.setContentPane(new JScrollPane(root));
        f.setVisible(true);
    }
}
