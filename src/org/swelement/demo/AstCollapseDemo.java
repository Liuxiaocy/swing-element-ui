package org.swelement.demo;

import org.swelement.ui.AstCollapse;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * AstCollapse 综合 Demo — 基础折叠面板 + 手风琴模式 + 点击交互。
 *
 * 交互提示：
 *  - 点击标题栏展开/折叠对应面板；箭头随展开状态旋转。
 *  - 手风琴模式下同时仅一个面板展开，展开新面板会自动折叠其他面板。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstCollapseDemo --selfcheck}
 * 离屏构建折叠面板并绘制，验证 addItem / expand / collapse / toggle / accordion API、
 * 越界与 null 参数异常、变化监听与绘制不抛错。
 * （展开/折叠的动画与绘制断言在 AstCollapse.selfCheck 中，因其与组件同包可驱动动画通道。）
 */
public class AstCollapseDemo {

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static JComponent makeContent(String text) {
        JLabel l = new JLabel(text);
        l.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return l;
    }

    private static void start() {
        JFrame f = new JFrame("AstCollapse Demo — 基础 / 手风琴 / 交互");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. 基础折叠面板（非手风琴）
        AstCollapse c1 = new AstCollapse(false);
        c1.addItem("标题一", makeContent("内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一"));
        c1.addItem("标题二", makeContent("内容二"));
        c1.addItem("标题三", makeContent("内容三"));
        c1.expand(0);

        // 2. 手风琴模式
        AstCollapse c2 = new AstCollapse(true);
        c2.addItem("标题一", makeContent("内容一"));
        c2.addItem("标题二", makeContent("内容二"));
        c2.addItem("标题三", makeContent("内容三"));
        c2.expand(0);

        // 3. 交互（变化监听更新状态）
        AstCollapse c3 = new AstCollapse(false);
        c3.addItem("面板一", makeContent("内容一"));
        c3.addItem("面板二", makeContent("内容二"));
        final JLabel status = new JLabel("点击标题栏展开/折叠，查看当前展开项");
        status.setForeground(new Color(0x909399));
        c3.setChangeListener(open -> status.setText("当前展开：" + java.util.Arrays.toString(open)));

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        root.add(section("基础折叠面板（可同时展开多项）", c1));
        root.add(Box.createVerticalStrut(18));
        root.add(section("手风琴模式（同时仅一个展开）", c2));
        root.add(Box.createVerticalStrut(18));
        root.add(section("点击交互", c3));
        root.add(Box.createVerticalStrut(8));
        root.add(status);

        f.setContentPane(new JScrollPane(root));
        f.setSize(520, 420);
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

    /** 离屏构建折叠面板并绘制，验证 API、越界/null 异常、手风琴与绘制不抛错。 */
    static void selfCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        AstCollapse c = new AstCollapse(false);
                        boolean threw = false;
                        try { c.addItem(null, new JPanel()); } catch (IllegalArgumentException e) { threw = true; }
                        assert threw : "null title throws"; threw = false;
                        try { c.addItem("x", null); } catch (IllegalArgumentException e) { threw = true; }
                        assert threw : "null content throws"; threw = false;
                        try { c.expand(0); } catch (IndexOutOfBoundsException e) { threw = true; }
                        assert threw : "expand OOB throws"; threw = false;
                        try { c.setChangeListener(null); } catch (IllegalArgumentException e) { threw = true; }
                        assert threw : "null listener throws";

                        c.addItem("面板一", new JLabel("内容一"));
                        c.addItem("面板二", new JLabel("内容二"));
                        c.addItem("面板三", new JLabel("内容三"));
                        assert c.getItemCount() == 3 : "3 items";
                        assert c.getOpenIndices().length == 0 : "all closed default";
                        assert !c.isOpen(0) : "0 closed default";

                        // toggle
                        c.toggle(1);
                        assert c.isOpen(1) : "1 open after toggle";
                        assert c.getOpenIndices().length == 1 : "1 open";
                        c.toggle(1);
                        assert !c.isOpen(1) : "1 closed after toggle again";

                        // expand multiple (non-accordion)
                        c.expand(0); c.expand(2);
                        assert c.isOpen(0) && c.isOpen(2) : "both open (non-accordion)";
                        assert c.getOpenIndices().length == 2 : "2 open";

                        // accordion
                        AstCollapse ca = new AstCollapse(true);
                        ca.addItem("A", new JLabel("aaa"));
                        ca.addItem("B", new JLabel("bbb"));
                        ca.expand(0);
                        assert ca.isOpen(0) : "A open";
                        ca.expand(1);
                        assert ca.isOpen(1) && !ca.isOpen(0) : "accordion: B open, A closed";
                        assert ca.getOpenIndices().length == 1 : "accordion 1 open";

                        // 绘制不抛错
                        AstCollapse cp = new AstCollapse(true);
                        cp.addItem("面板一", new JLabel("内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一内容一"));
                        cp.addItem("面板二", new JLabel("内容二"));
                        JFrame jf = new JFrame("AstCollapseDemo SC");
                        jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        jf.getContentPane().setLayout(new BorderLayout());
                        jf.getContentPane().add(cp, BorderLayout.NORTH);
                        jf.pack();
                        cp.expand(0);
                        try { Thread.sleep(60); } catch (Throwable ignore) {}
                        BufferedImage img = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D gg = img.createGraphics();
                        gg.setColor(Color.WHITE); gg.fillRect(0, 0, 300, 200);
                        try { cp.paint(gg); } finally { gg.dispose(); }
                        jf.dispose();
                    } catch (Throwable t) { err[0] = t; }
                }
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstCollapseDemo self-check OK");
    }
}
