package org.swelement.demo;

import org.swelement.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AstCardDemo {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstCard Demo - 卡片");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(16, 24, 16, 24));

        // Control panel
        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        ctrl.setBorder(new TitledBorder("控制区"));
        final JCheckBox hoverOn = new JCheckBox("Hover 高亮（鼠标移入体验动画）", true);
        final JCheckBox borderOn = new JCheckBox("边框（重新生成）", true);
        final JButton addCardBtn = new JButton("➕ 新增一张卡片");
        ctrl.add(hoverOn); ctrl.add(borderOn); ctrl.add(addCardBtn);
        final JLabel echo = new JLabel(" ");
        echo.setForeground(new Color(0x909399));
        echo.setFont(echo.getFont().deriveFont(12f));
        ctrl.add(echo);

        // Horizontal row: 3 cards side-by-side
        JPanel row = new JPanel(new GridBagLayout());
        row.setBorder(new TitledBorder("样式 1：普通卡片（Header Action + 正文内容）"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.insets = new Insets(0, 12, 0, 12);
        // Card 1: User profile — avatar in top-left corner of body, info rows + 2 actions
        AstCard c1 = new AstCard("用户信息", true, hoverOn.isSelected());
        JPanel body1 = new JPanel(new GridBagLayout());
        GridBagConstraints bc1 = new GridBagConstraints();
        bc1.anchor = GridBagConstraints.WEST; bc1.insets = new Insets(2, 0, 2, 0); bc1.fill = GridBagConstraints.HORIZONTAL;
        AstAvatar av = new AstAvatar('Z', AstAvatar.SIZE_LARGE, AstAvatar.CIRCLE);
        av.setBadgeDot(true);
        bc1.gridx = 0; bc1.gridy = 0; bc1.gridheight = 3; bc1.weightx = 0; bc1.fill = GridBagConstraints.NONE; bc1.insets = new Insets(0, 0, 0, 16);
        body1.add(av, bc1);
        bc1.gridheight = 1; bc1.gridx = 1; bc1.weightx = 1.0; bc1.insets = new Insets(4, 0, 4, 0);
        bc1.gridy = 0; body1.add(newFieldLabel("姓名："), bc1); bc1.gridx = 2; body1.add(new JLabel("张三"), bc1);
        bc1.gridy = 1; bc1.gridx = 1; body1.add(newFieldLabel("邮箱："), bc1); bc1.gridx = 2; body1.add(new JLabel("zhangsan@example.com"), bc1);
        bc1.gridy = 2; bc1.gridx = 1; body1.add(newFieldLabel("部门："), bc1); bc1.gridx = 2; body1.add(new JLabel("平台研发部"), bc1);
        c1.setContent(body1);
        AstButton editBtn = new AstButton("编辑", AstButton.DEFAULT, false);
        AstButton delBtn = new AstButton("删除", AstButton.DANGER, false);
        final ActionListener echoAction = new ActionListener() { public void actionPerformed(ActionEvent e) {
            Object s = e.getSource();
            if (s == editBtn) echo.setText("执行：编辑用户信息");
            else if (s == delBtn) echo.setText("执行：删除用户");
            else if (s instanceof AstButton) echo.setText("执行：" + ((AstButton)s).getText());
        }};
        editBtn.addActionListener(echoAction); delBtn.addActionListener(echoAction);
        c1.addHeaderAction(delBtn); c1.addHeaderAction(editBtn);
        gbc.gridx = 0;
        row.add(wrap(c1), gbc);

        // Card 2: AstProgress stats
        AstCard c2 = new AstCard("项目进度", true, hoverOn.isSelected());
        JPanel body2 = new JPanel(); body2.setLayout(new BoxLayout(body2, BoxLayout.Y_AXIS));
        AstProgress p1 = new AstProgress(75); AstProgress p2 = new AstProgress(42); AstProgress p3 = new AstProgress(88);
        for (Object[] pair : new Object[][]{ {"需求开发", p1}, {"接口联调", p2}, {"上线部署", p3} }) {
            JPanel line = new JPanel(new BorderLayout(12, 4));
            JLabel lbl = new JLabel((String) pair[0]);
            lbl.setFont(lbl.getFont().deriveFont(12f)); lbl.setForeground(new Color(0x606266));
            line.add(lbl, BorderLayout.WEST);
            line.add((JComponent) pair[1], BorderLayout.CENTER);
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            body2.add(line); body2.add(Box.createVerticalStrut(10));
        }
        c2.setContent(body2);
        AstButton more2 = new AstButton("查看详情", AstButton.DEFAULT, false); more2.addActionListener(echoAction);
        c2.addHeaderAction(more2);
        gbc.gridx = 1;
        row.add(wrap(c2), gbc);

        // Card 3: Plain no-border, simple text card
        AstCard c3 = new AstCard("无标题卡片（自定义）", false, hoverOn.isSelected());
        JTextArea ta = new JTextArea(
                "这是一个没有边框的卡片样式，适合与其他容器组合使用。\n\n" +
                "Element 规范指出：\n" +
                " - Shadow-on-hover 在 bordered=false 时也可生效\n" +
                " - 卡片内容区可嵌入任意 JComponent\n" +
                " - 卡片高度跟随内容自适应");
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        ta.setOpaque(false);
        ta.setForeground(new Color(0x606266));
        c3.setContent(ta);
        AstButton apply3 = new AstButton("应用", AstButton.PRIMARY, false); apply3.addActionListener(echoAction);
        c3.addHeaderAction(apply3);
        gbc.gridx = 2;
        row.add(wrap(c3), gbc);

        // Control: regenerate row on hover / border toggle
        ActionListener rebuild = new ActionListener() { public void actionPerformed(ActionEvent e) {
            // Swap out the 3 cards with a fresh row using the same construction but re-specify hoverOn/borderOn values directly
            boolean h = hoverOn.isSelected(), b = borderOn.isSelected();
            row.removeAll();
            AstCard nc1 = new AstCard("用户信息", b, h); nc1.setContent(body1); nc1.addHeaderAction(delBtn); nc1.addHeaderAction(editBtn);
            AstCard nc2 = new AstCard("项目进度", b, h); nc2.setContent(body2); nc2.addHeaderAction(more2);
            AstCard nc3 = new AstCard("无标题卡片（自定义）", b, h); nc3.setContent(ta); nc3.addHeaderAction(apply3);
            GridBagConstraints gbc2 = new GridBagConstraints();
            gbc2.gridy = 0; gbc2.fill = GridBagConstraints.BOTH; gbc2.weightx = 1.0; gbc2.weighty = 1.0; gbc2.insets = new Insets(0,12,0,12);
            gbc2.gridx = 0; row.add(wrap(nc1), gbc2);
            gbc2.gridx = 1; row.add(wrap(nc2), gbc2);
            gbc2.gridx = 2; row.add(wrap(nc3), gbc2);
            row.revalidate(); row.repaint();
        }};
        hoverOn.addActionListener(rebuild); borderOn.addActionListener(rebuild);

        // Add-new-card button
        final JPanel dynamicRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        dynamicRow.setBorder(new TitledBorder("样式 2：动态卡片列表（点击上方 ➕ 新增一张卡片追加到下面）"));
        addCardBtn.addActionListener(new ActionListener() {
            int seq = 1;
            public void actionPerformed(ActionEvent e) {
                AstCard card = new AstCard("动态卡片 #" + (seq++), true, hoverOn.isSelected());
                JPanel inner = new JPanel(new BorderLayout());
                JLabel tip = new JLabel("  这是第 " + (seq-1) + " 张新增的卡片。鼠标悬停可观察 hover 边框高亮动画。", SwingConstants.LEFT);
                tip.setForeground(new Color(0x909399)); tip.setFont(tip.getFont().deriveFont(12f));
                inner.add(tip, BorderLayout.CENTER);
                AstProgress prog = new AstProgress((int)(Math.random() * 80) + 20);
                inner.add(prog, BorderLayout.SOUTH);
                card.setContent(inner);
                AstButton close = new AstButton("关闭卡片", AstButton.WARNING, false);
                close.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ev) {
                    dynamicRow.remove(card.getParent() == null ? card : card.getParent());
                    dynamicRow.revalidate(); dynamicRow.repaint();
                    echo.setText("已关闭 1 张卡片，当前动态列表剩余：" + (dynamicRow.getComponentCount()));
                }});
                card.addHeaderAction(close);
                JPanel wrap = new JPanel(new BorderLayout()); wrap.add(card, BorderLayout.CENTER);
                wrap.setPreferredSize(new Dimension(320, 220));
                dynamicRow.add(wrap);
                dynamicRow.revalidate(); dynamicRow.repaint();
                echo.setText("已新增 " + (seq-1) + " 张动态卡片");
            }
        });

        root.add(ctrl); root.add(Box.createVerticalStrut(12));
        root.add(row);  root.add(Box.createVerticalStrut(12));
        root.add(dynamicRow);
        f.setContentPane(new JScrollPane(root));
        f.pack(); f.setSize(Math.max(f.getWidth(), 1200), Math.min(f.getHeight(), 800));
        f.setLocationRelativeTo(null); f.setVisible(true);
    }

    private static JLabel newFieldLabel(String s) {
        JLabel l = new JLabel(s);
        l.setFont(l.getFont().deriveFont(12f));
        l.setForeground(new Color(0x909399));
        return l;
    }

    private static JPanel wrap(AstCard card) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(0, 0, 0, 0));
        p.add(card, BorderLayout.CENTER);
        return p;
    }
}
