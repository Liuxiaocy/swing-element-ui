package org.swelement.demo;

import org.swelement.ui.AstTimePicker;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * AstTimePicker 综合 Demo — 基本（含秒列）+ 关闭秒列 + 区间模式 + 选择变化监听。
 *
 * 交互提示：
 *  - 点击触发框弹出「时 / 分 / 秒」三列可滚动面板，选择后点「确定」收起；再点触发框也可收起。
 *  - 区间模式先选起始、再选结束；结束面板中 ≤ 起始的备选项被级联置灰且不可点选。
 *  - 最后一组绑定 setTimeChangeListener，选择后状态行实时显示当前值。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstTimePickerDemo --selfcheck}
 * 验证 setTime / getTime / setRangeMode / setTimeRange 的取值与异常、showSeconds 列数、
 * 选择变化监听回调、尺寸合理与离屏绘制不抛错。
 * （弹层禁用级联等绘制级断言在 AstTimePicker.selfCheck 中，因其与组件同包可访问内部状态。）
 */
public class AstTimePickerDemo {

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstTimePicker Demo — 基本 / 关闭秒列 / 区间模式 / 监听");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. 基本用法（含秒列）
        AstTimePicker tp1 = new AstTimePicker();
        tp1.setTime(14, 30, 0);

        // 2. 关闭秒列（时:分）
        AstTimePicker tp2 = new AstTimePicker(false);
        tp2.setTime(9, 5, 0);

        // 3. 区间模式（固定时间范围）
        AstTimePicker tp3 = new AstTimePicker();
        tp3.setRangeMode(true);
        tp3.setTimeRange(9, 0, 0, 14, 30, 0);

        // 4. 选择变化监听
        AstTimePicker tp4 = new AstTimePicker();
        tp4.setTime(8, 0, 0);
        final JLabel status = new JLabel("当前时间：08:00:00");
        status.setForeground(new Color(0x909399));
        tp4.setTimeChangeListener(hms -> status.setText(
                String.format("当前时间：%02d:%02d:%02d", hms[0], hms[1], hms[2])));

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        root.add(section("基本用法（含秒列）", tp1));
        root.add(Box.createVerticalStrut(18));
        root.add(section("关闭秒列（时:分）", tp2));
        root.add(Box.createVerticalStrut(18));
        root.add(section("区间模式（结束侧级联置灰）", tp3));
        root.add(Box.createVerticalStrut(18));
        root.add(section("选择变化监听", tp4));
        root.add(Box.createVerticalStrut(8));
        root.add(status);

        f.setContentPane(new JScrollPane(root));
        f.setSize(520, 380);
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

    /** 校验 API 取值/异常、尺寸、弹层列数、监听回调与离屏绘制。 */
    static void selfCheck() {
        // 1) 非 GUI：单值 API 与非法取值
        AstTimePicker tp = new AstTimePicker();
        tp.setTime(14, 30, 0);
        int[] t = tp.getTime();
        assert t[0] == 14 && t[1] == 30 && t[2] == 0 : "getTime roundtrip";

        boolean threw = false;
        try { tp.setTime(24, 0, 0); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "hour>23 must throw";
        threw = false;
        try { tp.setTime(-1, 0, 0); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "hour<0 must throw";
        threw = false;
        try { tp.setTime(0, 60, 0); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "minute>59 must throw";
        threw = false;
        try { tp.setTime(0, 0, 60); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "second>59 must throw";
        threw = false;
        try { tp.setTimeChangeListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null listener must throw";

        // 2) 区间模式 API 与异常
        AstTimePicker rp = new AstTimePicker();
        assert !rp.isRangeMode() : "默认非区间模式";
        rp.setRangeMode(true);
        assert rp.isRangeMode() : "setRangeMode(true) 后 isRangeMode=true";
        rp.setTimeRange(9, 0, 0, 14, 30, 0);
        int[][] rg = rp.getTimeRange();
        assert rg[0][0] == 9 && rg[0][1] == 0 && rg[0][2] == 0
            && rg[1][0] == 14 && rg[1][1] == 30 && rg[1][2] == 0 : "getTimeRange roundtrip";
        threw = false;
        try { rp.setTimeRange(14, 30, 0, 9, 0, 0); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "end<=start must throw";
        threw = false;
        try { rp.getTime(); } catch (IllegalStateException e) { threw = true; }
        assert threw : "区间模式 getTime 必须抛 IllegalStateException";
        threw = false;
        try { new AstTimePicker().getTimeRange(); } catch (IllegalStateException e) { threw = true; }
        assert threw : "非区间模式 getTimeRange 必须抛 IllegalStateException";

        // 3) 尺寸：触发框 180x36
        assert tp.getPreferredSize().width == 180 && tp.getPreferredSize().height == 36
            : "preferred 180x36, got " + tp.getPreferredSize();

        // 4) 弹层列数（含秒列 3 / 关闭秒列 2）+ 监听回调
        runGuiCheck(true, 3, true);
        runGuiCheck(false, 2, false);
        // 等弹层收起动画（185ms 定时器）走完，避免残留 Swing Timer 拖住 JVM 退出
        sleep(260);

        // 5) 离屏绘制不抛错
        AstTimePicker pv = new AstTimePicker();
        pv.setTime(8, 8, 8);
        pv.setSize(180, 36);
        BufferedImage img = new BufferedImage(180, 36, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { pv.paint(gg); } finally { gg.dispose(); }

        System.out.println("AstTimePickerDemo self-check OK");
    }

    /** 弹出面板断言 JList 列数；testListener 为真时驱动时列选择并断言监听回调。 */
    private static void runGuiCheck(final boolean showSeconds, final int expectCols, final boolean testListener) {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                JFrame jf = null;
                try {
                    jf = new JFrame("AstTimePickerDemo SC");
                    jf.setSize(640, 420);
                    jf.setVisible(true);
                    jf.getContentPane().setLayout(new FlowLayout());
                    final AstTimePicker picker = new AstTimePicker(showSeconds);
                    picker.setTime(10, 0, 0);
                    final int[] fired = {-1};
                    if (testListener) picker.setTimeChangeListener(hms -> fired[0] = hms[0]);
                    jf.getContentPane().add(picker);
                    jf.pack();
                    picker.showPicker();
                    sleep(80);
                    int cols = countLists(jf.getLayeredPane());
                    assert cols == expectCols
                        : "showSeconds=" + showSeconds + " 应含 " + expectCols + " 个 JList，实际=" + cols;
                    if (testListener) {
                        JList<?> hourList = findListByModelSize(jf.getLayeredPane(), 24);
                        assert hourList != null : "应找到时列（模型 24 项）";
                        hourList.setSelectedIndex(15);
                        sleep(40);
                        assert fired[0] == 15 : "监听回调应收到 15，实际=" + fired[0];
                    }
                    picker.hidePicker();
                    sleep(40);
                    assert !picker.isOpen() : "hidePicker 后 open=false";
                } catch (Throwable ex) { err[0] = ex; }
                finally { if (jf != null) jf.dispose(); }
            }});
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
    }

    /** 统计容器子树内 JList 数量（时/分/秒三列 → 3；关闭秒列 → 2）。 */
    private static int countLists(Container root) {
        int n = 0;
        java.util.Queue<Container> q = new java.util.LinkedList<Container>();
        q.add(root);
        while (!q.isEmpty()) {
            Container cur = q.poll();
            for (int i = 0; i < cur.getComponentCount(); i++) {
                Component ch = cur.getComponent(i);
                if (ch instanceof JList) n++;
                if (ch instanceof Container) q.add((Container) ch);
            }
        }
        return n;
    }

    /** 找到模型项数等于 size 的第一个 JList（时列 24 项、分/秒列 60 项，据此定位时列）。 */
    private static JList<?> findListByModelSize(Container root, int size) {
        java.util.Queue<Container> q = new java.util.LinkedList<Container>();
        q.add(root);
        while (!q.isEmpty()) {
            Container cur = q.poll();
            for (int i = 0; i < cur.getComponentCount(); i++) {
                Component ch = cur.getComponent(i);
                if (ch instanceof JList) {
                    JList<?> l = (JList<?>) ch;
                    if (l.getModel() != null && l.getModel().getSize() == size) return l;
                }
                if (ch instanceof Container) q.add((Container) ch);
            }
        }
        return null;
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (Throwable ignore) {} }
}
