package org.swelement.demo;

import org.swelement.ui.AstIcon;
import org.swelement.ui.AstSteps;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * AstSteps 综合 Demo — 横向基础 / 含状态 / 带图标 / 简洁风格。
 *
 * 交互提示：
 *  - 「横向基础」支持点击步骤（setStepClickListener）。
 *  - 显式 setStepStatus 会覆盖由 setCurrent 推导出的状态（如已完成步骤标 ERROR）。
 *  - 「简洁风格」节点退化为彩色小圆点，不画数字/对勾/图标。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstStepsDemo --selfcheck}
 * 会离屏构建四种步骤条并绘制，验证状态/图标 API、简洁风格尺寸更小与渲染不抛异常。
 */
public class AstStepsDemo {

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstSteps Demo — 基础 / 含状态 / 带图标 / 简洁风格");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. 横向基础（可点击）
        AstSteps basic = new AstSteps(Arrays.asList("填写信息", "确认订单", "支付", "完成"));
        basic.setCurrent(2);
        basic.setStepClickListener(idx -> System.out.println("点击步骤: " + (idx + 1)));

        // 2. 含状态（显式 status 覆盖 current 推导）
        AstSteps status = new AstSteps(Arrays.asList("提交申请", "部门审批", "财务复核", "归档"));
        status.setCurrent(2);
        status.setStepStatus(0, AstSteps.Status.SUCCESS);
        status.setStepStatus(1, AstSteps.Status.ERROR);   // 覆盖"已完成"推导，显示红色叉
        status.setStepStatus(2, AstSteps.Status.PROCESS);

        // 3. 带图标
        AstSteps icon = new AstSteps(Arrays.asList("注册账号", "完善资料", "上传证件", "完成"));
        icon.setCurrent(1);
        icon.setStepIcon(0, AstIcon.Type.USER);
        icon.setStepIcon(1, AstIcon.Type.EDIT);
        icon.setStepIcon(2, AstIcon.Type.UPLOAD);
        icon.setStepIcon(3, AstIcon.Type.CHECK);

        // 4. 简洁风格
        AstSteps simple = new AstSteps(Arrays.asList("待处理", "处理中", "已完结"));
        simple.setCurrent(1);
        simple.setSimple(true);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        root.add(section("横向基础（点击步骤）", basic));
        root.add(Box.createVerticalStrut(14));
        root.add(section("含状态（显式 status 覆盖 current：第 2 步 ERROR）", status));
        root.add(Box.createVerticalStrut(14));
        root.add(section("带图标", icon));
        root.add(Box.createVerticalStrut(14));
        root.add(section("简洁风格（小圆点）", simple));

        f.setContentPane(new JScrollPane(root));
        f.setSize(760, 520);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private static JPanel section(String title, AstSteps steps) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13f));
        t.setForeground(new Color(0x303133));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        steps.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(steps);
        return p;
    }

    // ===================== 自检 =====================

    /** 离屏构建四种步骤条并绘制，验证 API 与渲染不抛异常。 */
    static void selfCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        JFrame jf = new JFrame("AstStepsDemo SC");
                        jf.setSize(760, 520);
                        jf.setVisible(true);

                        AstSteps basic = new AstSteps(Arrays.asList("填写信息", "确认订单", "支付", "完成"));
                        basic.setCurrent(2);
                        assert basic.getCurrent() == 2 : "demo current 2, got " + basic.getCurrent();

                        AstSteps status = new AstSteps(Arrays.asList("a", "b", "c", "d"));
                        status.setCurrent(2);
                        status.setStepStatus(1, AstSteps.Status.ERROR);
                        status.setStepStatus(0, AstSteps.Status.SUCCESS);
                        assert status.getStepStatus(1) == AstSteps.Status.ERROR : "demo status ERROR";
                        assert status.getStepStatus(0) == AstSteps.Status.SUCCESS : "demo status SUCCESS";

                        AstSteps icon = new AstSteps(Arrays.asList("a", "b", "c", "d"));
                        icon.setStepIcon(0, AstIcon.Type.USER);
                        icon.setStepIcon(3, AstIcon.Type.CHECK);
                        assert icon.getStepIcon(0) == AstIcon.Type.USER : "demo icon USER";
                        assert icon.getStepIcon(1) == null : "demo icon unset is null";

                        AstSteps simple = new AstSteps(Arrays.asList("a", "b", "c"));
                        simple.setSimple(true);
                        assert simple.isSimple() : "demo simple on";
                        // 简洁风格更紧凑
                        int hSimple = simple.getPreferredSize().height;
                        int hNormal = new AstSteps(Arrays.asList("a", "b", "c")).getPreferredSize().height;
                        assert hSimple < hNormal : "demo simple height " + hSimple + " < normal " + hNormal;

                        // 离屏绘制四种，验证不抛异常
                        paintTo(basic, 480, 60);
                        paintTo(status, 480, 60);
                        paintTo(icon, 480, 60);
                        paintTo(simple, 480, 24);

                        jf.dispose();
                    } catch (Throwable t) { err[0] = t; }
                }
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstStepsDemo self-check OK");
    }

    private static void paintTo(AstSteps c, int w, int h) {
        c.setBounds(0, 0, w, h);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        try { c.paint(g); } finally { g.dispose(); }
    }
}
