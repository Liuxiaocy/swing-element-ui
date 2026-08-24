package org.swelement.demo;

import org.swelement.ui.AstAvatar;
import org.swelement.ui.AstButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicInteger;

public class AstAvatarDemo {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }
    private static void start() {
        JFrame f = new JFrame("AstAvatar Demo - 头像");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 12));
        p1.setBorder(new TitledBorder("单字符头像（字符哈希选色 + 对比度自动选字色）"));
        for (char c : new char[]{'Z','A','李','王','5','☰','花','P','S','U'}) {
            AstAvatar a = new AstAvatar(c, AstAvatar.SIZE_LARGE, AstAvatar.CIRCLE);
            JPanel wrap = new JPanel(); wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
            wrap.add(a); wrap.add(Box.createVerticalStrut(4));
            JLabel l = new JLabel(String.valueOf(c), SwingConstants.CENTER);
            l.setForeground(new Color(0x909399));
            l.setFont(l.getFont().deriveFont(11f));
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            wrap.add(l);
            p1.add(wrap);
        }

        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 36, 12));
        p2.setBorder(new TitledBorder("大小 × 形状"));
        int[] sizes = {AstAvatar.SIZE_SMALL, AstAvatar.SIZE_DEFAULT, AstAvatar.SIZE_LARGE};
        String[] labels = {"Small 32", "Default 40", "Large 64"};
        Color col1 = new Color(0xE6A23C);
        Color col2 = new Color(0x67C23A);
        Color col3 = new Color(0xF56C6C);
        Color[] cols = {col1, col2, col3};
        String[] names = {"Admin", "系统", "管"};
        for (int i = 0; i < sizes.length; i++) {
            for (int sh : new int[]{AstAvatar.CIRCLE, AstAvatar.SQUARE}) {
                AstAvatar a = new AstAvatar(cols[i], names[i], sizes[i], sh);
                JPanel wrap = new JPanel(); wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
                wrap.add(a); wrap.add(Box.createVerticalStrut(4));
                JLabel l = new JLabel(labels[i] + (sh==AstAvatar.CIRCLE?" 圆":" 方"), SwingConstants.CENTER);
                l.setForeground(new Color(0x909399)); l.setFont(l.getFont().deriveFont(11f));
                l.setAlignmentX(Component.CENTER_ALIGNMENT); wrap.add(l);
                p2.add(wrap);
            }
        }

        final AtomicInteger badge = new AtomicInteger(3);
        final AstAvatar[] avs = new AstAvatar[4];
        avs[0] = new AstAvatar('U', AstAvatar.SIZE_LARGE, AstAvatar.CIRCLE); avs[0].setBadgeCount(badge.get());
        avs[1] = new AstAvatar(col2, "OK", AstAvatar.SIZE_DEFAULT, AstAvatar.SQUARE); avs[1].setBadgeDot(true);
        avs[2] = new AstAvatar('P', AstAvatar.SIZE_LARGE, AstAvatar.SQUARE); avs[2].setBadgeCount(100);
        avs[3] = new AstAvatar('A', AstAvatar.SIZE_DEFAULT, AstAvatar.CIRCLE); avs[3].setBadgeCount(0);
        JPanel p3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 12));
        p3.setBorder(new TitledBorder("角标复合（数字 / dot / 99+ / 0隐藏）"));
        for (AstAvatar a : avs) p3.add(a);

        JPanel p4 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        p4.setBorder(new TitledBorder("交互控制（点击以下按钮观察角标动画）"));
        final JLabel echo = new JLabel("当前 U 角标 = 3");
        echo.setFont(echo.getFont().deriveFont(Font.BOLD));
        echo.setForeground(new Color(0x303133));
        AstButton plus = new AstButton("用户 U 角标 +1", AstButton.PRIMARY, false);
        AstButton minus = new AstButton("用户 U 角标 -1", AstButton.DEFAULT, false);
        AstButton reset = new AstButton("重置为 3", AstButton.DEFAULT, false);
        final boolean[] dotOn = {true};
        AstButton dotSwitch = new AstButton("切换 OK 的红点", AstButton.WARNING, false);
        final boolean[] showBig = {true};
        AstButton bigToggle = new AstButton("切换 P 的大角标 99+ / 0", AstButton.DANGER, false);

        ActionListener updater = new ActionListener() { public void actionPerformed(ActionEvent e) {
            int cur = badge.get();
            if (e.getSource() == plus) { cur = badge.incrementAndGet(); avs[0].setBadgeCount(cur); }
            else if (e.getSource() == minus) { int v = Math.max(0, badge.decrementAndGet()); badge.set(v); avs[0].setBadgeCount(v); cur = v; }
            else if (e.getSource() == reset) { badge.set(3); cur = 3; avs[0].setBadgeCount(3); }
            else if (e.getSource() == dotSwitch) { dotOn[0] = !dotOn[0]; avs[1].setBadgeDot(dotOn[0]); }
            else if (e.getSource() == bigToggle) { showBig[0] = !showBig[0]; avs[2].setBadgeCount(showBig[0] ? 100 : 0); }
            echo.setText("当前 U 角标 = " + badge.get());
        }};
        plus.addActionListener(updater); minus.addActionListener(updater); reset.addActionListener(updater);
        dotSwitch.addActionListener(updater); bigToggle.addActionListener(updater);
        p4.add(plus); p4.add(minus); p4.add(reset); p4.add(dotSwitch); p4.add(bigToggle);
        JPanel echoRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        echoRow.setBorder(new EmptyBorder(0,0,8,0));
        echoRow.add(echo);

        root.add(p1); root.add(Box.createVerticalStrut(8));
        root.add(p2); root.add(Box.createVerticalStrut(8));
        root.add(p3); root.add(Box.createVerticalStrut(8));
        root.add(echoRow);
        root.add(p4);

        f.setContentPane(new JScrollPane(root));
        f.pack(); f.setSize(Math.max(f.getWidth(), 1080), Math.min(f.getHeight(), 800));
        f.setLocationRelativeTo(null); f.setVisible(true);
    }
}
