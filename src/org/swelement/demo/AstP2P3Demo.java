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
import java.util.ArrayList;
import java.util.List;

/**
 * P2/P3 组件综合 Demo — 集成 AstTimeline/AstCalendar/AstTimePicker/AstTransfer/AstCarousel/AstPopover/AstDrawer。
 *
 * 布局：
 *   ┌─────────────────────────────────────────────────────────────────┐
 *   │ 控制区：echo + TimePicker + Popover/Drawer 触发按钮                │
 *   ├──────────────┬───────────────────────┬──────────────────────────┤
 *   │ AstTimeline   │ AstCalendar 日历       │ AstTransfer 穿梭框        │
 *   │ 时间线        │ 上/下月 + 选中回显      │ 勾选 + 左右转移          │
 *   ├──────────────┴───────────────────────┴──────────────────────────┤
 *   │ AstCarousel 走马灯（autoplay 切换 + 前后按钮 + 指示点）            │
 *   └─────────────────────────────────────────────────────────────────┘
 *
 * 交互：每个组件均配按钮触发动画/行为，顶部 echo 实时反馈。
 */
public class AstP2P3Demo {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        final JFrame f = new JFrame("AstP2P3 Demo - 时间线/日历/时间/穿梭/走马灯/气泡/抽屉");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // --- 顶部控制区 ---
        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        ctrl.setBorder(new TitledBorder("控制区（TimePicker + Popover/Drawer 触发）"));
        final JLabel echo = new JLabel("就绪。请操作下方各组件观察动画与交互。");
        echo.setForeground(new Color(0x606266));
        echo.setFont(echo.getFont().deriveFont(13f));
        ctrl.add(echo);
        ctrl.add(Box.createHorizontalStrut(16));
        // TimePicker
        final AstTimePicker tp = new AstTimePicker(true);
        tp.setTime(9, 30, 0);
        tp.setTimeChangeListener(hms -> echo.setText("时间选择器：" + String.format("%02d:%02d:%02d", hms[0], hms[1], hms[2])));
        ctrl.add(new JLabel("时间："));
        ctrl.add(tp);
        ctrl.add(Box.createHorizontalStrut(16));
        // Popover 触发：富内容卡片
        JPanel popBody = new JPanel(new BorderLayout(0, 6));
        popBody.setOpaque(false);
        JLabel popInfo = new JLabel("<html>这是一个气泡卡片（Popover），<br>可承载任意富内容组件。</html>");
        popInfo.setForeground(ElementTheme.TEXT_REGULAR);
        popInfo.setFont(popInfo.getFont().deriveFont(13f));
        popBody.add(popInfo, BorderLayout.CENTER);
        final AstPopover popover = new AstPopover("提示标题", popBody, AnimatedPopup.Direction.BELOW, "Pop 气泡卡片");
        popover.setTrigger(AstPopover.Trigger.CLICK);
        ctrl.add(popover);
        ctrl.add(Box.createHorizontalStrut(16));
        // Drawer 触发按钮
        AstButton drawerBtn = new AstButton("打开抽屉", AstButton.PRIMARY, false);
        drawerBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            JPanel body = new JPanel(new GridLayout(0, 1, 8, 8));
            body.setBorder(new EmptyBorder(8, 8, 8, 8));
            for (int i = 1; i <= 4; i++) {
                JLabel row = new JLabel("抽屉内容项 " + i + " — 可放置任意设置/详情/表单组件");
                row.setForeground(ElementTheme.TEXT_REGULAR);
                row.setFont(row.getFont().deriveFont(13f));
                body.add(row);
            }
            AstDrawer.show(f, AstDrawer.Direction.RIGHT, "抽屉详情", body, 360, new Runnable() { public void run() {
                echo.setText("抽屉：已关闭");
            }});
            echo.setText("抽屉：已从右侧滑出");
        }});
        ctrl.add(drawerBtn);

        // --- 左：AstTimeline ---
        List<AstTimeline.Item> tlItems = new ArrayList<AstTimeline.Item>();
        tlItems.add(new AstTimeline.Item("2026-08-01", "项目启动", "仓库初始化与架构设计", AstTimeline.Type.PRIMARY));
        tlItems.add(new AstTimeline.Item("2026-08-10", "P1 完成", "基础组件与弹窗组件全链自检通过", AstTimeline.Type.SUCCESS));
        tlItems.add(new AstTimeline.Item("2026-08-21", "P2 进行中", "Popover/Drawer/TimePicker/Transfer 完成", AstTimeline.Type.WARNING));
        tlItems.add(new AstTimeline.Item("待定", "P3 规划", "Timeline/Calendar/Carousel 开发中", AstTimeline.Type.INFO));
        final AstTimeline timeline = new AstTimeline(tlItems);
        JPanel timelinePanel = new JPanel(new BorderLayout(0, 8));
        timelinePanel.setBorder(new TitledBorder("AstTimeline 时间线（hover 卡片过渡）"));
        JScrollPane tlScroll = new JScrollPane(timeline);
        tlScroll.setPreferredSize(new Dimension(360, 340));
        tlScroll.setBorder(null);
        timelinePanel.add(tlScroll, BorderLayout.CENTER);

        // --- 中：AstCalendar ---
        final AstCalendar cal = new AstCalendar();
        cal.setDateListener(date -> echo.setText("日历：选中 " + date[0] + "-" + (date[1] + 1) + "-" + date[2]));
        JPanel calPanel = new JPanel(new BorderLayout(0, 8));
        calPanel.setBorder(new TitledBorder("AstCalendar 日历（‹ › 翻月 + 选中日期）"));
        JPanel calBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        AstButton prevMonth = new AstButton("‹ 上月", AstButton.DEFAULT, false);
        AstButton nextMonth = new AstButton("下月 ›", AstButton.DEFAULT, false);
        AstButton todayBtn = new AstButton("回到今日", AstButton.PRIMARY, false);
        prevMonth.addActionListener(e -> { cal.prevMonth(); echo.setText("日历：上一月"); });
        nextMonth.addActionListener(e -> { cal.nextMonth(); echo.setText("日历：下一月"); });
        java.util.Calendar now = java.util.Calendar.getInstance();
        todayBtn.addActionListener(e -> { cal.setSelected(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH), now.get(java.util.Calendar.DAY_OF_MONTH)); echo.setText("日历：回到今日"); });
        calBtns.add(prevMonth); calBtns.add(nextMonth); calBtns.add(todayBtn);
        calPanel.add(cal, BorderLayout.CENTER);
        calPanel.add(calBtns, BorderLayout.SOUTH);

        // --- 右：AstTransfer ---
        List<AstTransfer.Item> tfData = new ArrayList<AstTransfer.Item>();
        String[] names = {"苹果", "香蕉", "橙子", "葡萄", "西瓜", "芒果", "草莓", "蓝莓"};
        for (int i = 0; i < names.length; i++) tfData.add(new AstTransfer.Item("k" + i, names[i]));
        final AstTransfer transfer = new AstTransfer(tfData);
        transfer.setFilterable(true);
        transfer.setSelectedKeys(java.util.Arrays.asList("k1"));
        transfer.setChangeListener(sel -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sel.size(); i++) { if (i > 0) sb.append("、"); sb.append(sel.get(i).label); }
            echo.setText("穿梭框：已选 " + sel.size() + " 项 — " + sb.toString());
        });
        JPanel transferPanel = new JPanel(new BorderLayout(0, 8));
        transferPanel.setBorder(new TitledBorder("AstTransfer 穿梭框（搜索 + 勾选转移）"));
        transferPanel.add(transfer, BorderLayout.CENTER);

        // --- 三列并排 ---
        JPanel row1 = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 6, 0, 6);
        gbc.gridx = 0; gbc.weightx = 0.0; row1.add(timelinePanel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.0; row1.add(calPanel, gbc);
        gbc.gridx = 2; gbc.weightx = 1.0; row1.add(transferPanel, gbc);

        // --- 底：AstCarousel ---
        List<AstCarousel.SlidePainter> slides = new ArrayList<AstCarousel.SlidePainter>();
        slides.add((g, w, h) -> {
            g.setColor(new Color(0x40, 0x9E, 0xFF)); g.fillRect(0, 0, w, h);
            g.setColor(Color.WHITE); g.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 28f));
            FontMetrics fm = g.getFontMetrics();
            String s = "第一张：蓝色幻灯片";
            g.drawString(s, (w - fm.stringWidth(s)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        });
        slides.add((g, w, h) -> {
            g.setColor(new Color(0x67, 0xC2, 0x3A)); g.fillRect(0, 0, w, h);
            g.setColor(Color.WHITE); g.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 28f));
            FontMetrics fm = g.getFontMetrics();
            String s = "第二张：绿色幻灯片";
            g.drawString(s, (w - fm.stringWidth(s)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        });
        slides.add((g, w, h) -> {
            g.setColor(new Color(0xE6, 0xA2, 0x3C)); g.fillRect(0, 0, w, h);
            g.setColor(Color.WHITE); g.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 28f));
            FontMetrics fm = g.getFontMetrics();
            String s = "第三张：橙色幻灯片";
            g.drawString(s, (w - fm.stringWidth(s)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        });
        final AstCarousel carousel = new AstCarousel(slides);
        JPanel carouselPanel = new JPanel(new BorderLayout(0, 8));
        carouselPanel.setBorder(new TitledBorder("AstCarousel 走马灯（hover 暂停 + ‹ › 切换 + 指示点）"));
        JPanel carBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        AstButton carPrev = new AstButton("‹ 上一张", AstButton.DEFAULT, false);
        AstButton carNext = new AstButton("下一张 ›", AstButton.DEFAULT, false);
        final AstButton autoplayBtn = new AstButton("开启自动播放", AstButton.PRIMARY, false);
        carPrev.addActionListener(e -> { carousel.prev(); echo.setText("走马灯：上一张，当前 " + (carousel.getCurrent() + 1) + "/" + carousel.getSlideCount()); });
        carNext.addActionListener(e -> { carousel.next(); echo.setText("走马灯：下一张，当前 " + (carousel.getCurrent() + 1) + "/" + carousel.getSlideCount()); });
        autoplayBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            boolean on = !carousel.isAutoplay();
            carousel.setAutoplay(on);
            autoplayBtn.setText(on ? "停止自动播放" : "开启自动播放");
            echo.setText("走马灯：自动播放已" + (on ? "开启（3s 循环）" : "停止"));
        }});
        carBtns.add(carPrev); carBtns.add(carNext); carBtns.add(autoplayBtn);
        carouselPanel.add(carousel, BorderLayout.CENTER);
        carouselPanel.add(carBtns, BorderLayout.SOUTH);

        root.add(ctrl, BorderLayout.NORTH);
        root.add(row1, BorderLayout.CENTER);
        root.add(carouselPanel, BorderLayout.SOUTH);
        f.setContentPane(new JScrollPane(root));
        f.pack();
        f.setSize(Math.max(f.getWidth(), 1180), Math.min(f.getHeight(), 860));
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
