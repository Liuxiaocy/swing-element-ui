package org.swelement.demo;

import org.swelement.ui.AstBreadcrumb;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * AstBreadcrumb 综合 Demo — 默认分隔符 + 自定义分隔符 + 点击交互。
 *
 * 交互提示：
 *  - 最后一段为当前页，不可点击；其余段点击触发 setItemClickListener。
 *  - hover 时文字变主色并出现下划线。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstBreadcrumbDemo --selfcheck}
 * 离屏构建面包屑并绘制，验证 setItems / setSeparator / setItemClickListener API、尺寸、
 * 点击行为（首段可点、末段不可点）与绘制不抛错。
 * （选中=主色填充的绘制级断言在 AstBreadcrumb.selfCheck 中，因其与组件同包可驱动 fill 动画到终态。）
 */
public class AstBreadcrumbDemo {

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstBreadcrumb Demo — 默认分隔符 / 自定义分隔符 / 交互");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. 默认分隔符 "/"
        AstBreadcrumb b1 = new AstBreadcrumb(Arrays.asList("首页", "用户管理", "详情"));

        // 2. 自定义分隔符 ">"
        AstBreadcrumb b2 = new AstBreadcrumb(Arrays.asList("控制台", "系统设置", "成员管理"));
        b2.setSeparator(">");

        // 3. 交互（点击更新状态）
        AstBreadcrumb b3 = new AstBreadcrumb(Arrays.asList("商品", "评价", "晒单"));
        final JLabel status = new JLabel("点击任意可点段查看跳转");
        status.setForeground(new Color(0x909399));
        b3.setItemClickListener(idx -> status.setText("跳转到第 " + idx + " 段：" + b3.getItems().get(idx)));

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        root.add(section("默认分隔符（/）", b1));
        root.add(Box.createVerticalStrut(18));
        root.add(section("自定义分隔符（>）", b2));
        root.add(Box.createVerticalStrut(18));
        root.add(section("点击交互", b3));
        root.add(Box.createVerticalStrut(8));
        root.add(status);

        f.setContentPane(new JScrollPane(root));
        f.setSize(560, 300);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private static JPanel section(String title, JComponent c) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13f));
        t.setForeground(new Color(0x303133));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(c);
        return p;
    }

    // ===================== 自检 =====================

    /** 离屏构建面包屑并绘制，验证 API、尺寸、点击行为与绘制不抛错。 */
    static void selfCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        JFrame jf = new JFrame("AstBreadcrumbDemo SC");
                        jf.setSize(560, 200);
                        jf.setVisible(true);

                        AstBreadcrumb br = new AstBreadcrumb(Arrays.asList("首页", "用户管理", "详情"));
                        assert br.getItems().size() == 3 : "3 items";
                        br.setItems(Arrays.asList("a", "b"));
                        assert br.getItems().size() == 2 : "2 after setItems";

                        boolean threw = false;
                        try { br.setSeparator(null); } catch (IllegalArgumentException e) { threw = true; }
                        assert threw : "null separator throws";
                        threw = false;
                        try { br.setItemClickListener(null); } catch (IllegalArgumentException e) { threw = true; }
                        assert threw : "null listener throws";

                        // 尺寸：height=24（ROW_H），width>0
                        assert br.getPreferredSize().height == 24
                                : "breadcrumb height 24, got " + br.getPreferredSize().height;
                        assert br.getPreferredSize().width > 0 : "breadcrumb width > 0";

                        // 点击行为：首段可点，末段不可点
                        final int[] clicked = {-99};
                        br.setItemClickListener(idx -> clicked[0] = idx);
                        jf.getContentPane().setLayout(new FlowLayout());
                        jf.getContentPane().add(br);
                        jf.pack();
                        br.dispatchEvent(new MouseEvent(br, MouseEvent.MOUSE_PRESSED,
                                System.currentTimeMillis(), 0, 10, 10, 1, false));
                        try { Thread.sleep(30); } catch (Throwable ignore) {}
                        assert clicked[0] == 0 : "clicked item 0, got " + clicked[0];
                        clicked[0] = -99;
                        int lastX = br.getPreferredSize().width - 30;
                        br.dispatchEvent(new MouseEvent(br, MouseEvent.MOUSE_PRESSED,
                                System.currentTimeMillis(), 0, lastX, 10, 1, false));
                        try { Thread.sleep(30); } catch (Throwable ignore) {}
                        assert clicked[0] == -99 : "last item not clickable, got " + clicked[0];

                        // 绘制不抛错
                        int w = br.getPreferredSize().width, h = 24;
                        br.setSize(w, h);
                        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D gg = img.createGraphics();
                        try { br.paint(gg); } finally { gg.dispose(); }

                        jf.dispose();
                    } catch (Throwable t) { err[0] = t; }
                }
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstBreadcrumbDemo self-check OK");
    }
}
