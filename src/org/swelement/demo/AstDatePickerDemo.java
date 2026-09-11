package org.swelement.demo;

import org.swelement.ui.AstDatePicker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;

/**
 * AstDatePicker 综合 Demo — 基本（已选日期）+ 空值/占位符 + 区间模式 + 尺寸档位 + 选择变化监听。
 *
 * 交互提示：
 *  - 点击触发框弹出日历，点某天即选中并自动收起；表头左右两侧可翻月。
 *  - 区间模式左右并排两块日历（左=起始月、右=起始月+1），点击-点击选取起止
 *    （end<start 自动交换，两者都选后再点开启新一轮），区间内部浅主色底纹。
 *  - 最后一组绑定 setDateChangeListener，选中后状态行实时显示日期。
 *
 * 自检：{@code java -ea -cp out org.swelement.demo.AstDatePickerDemo --selfcheck}
 * 验证 setDate/getDate、区间 setDateRange/getDateRange（含自动交换）、FormValue 往返、
 * 尺寸档位高度单调、各类非法参数异常、弹层开合 + 点击某天选中并回调 + 区间弹层含两块日历、
 * 离屏绘制不抛错。
 * （区间高亮 / 端点判定等绘制级断言在 AstDatePicker.selfCheck 中，因其与组件同包可访问内部 seam。）
 */
public class AstDatePickerDemo {

    // 日历面板首选尺寸（7*36+16 × 40+28+32*6+16）——用于在弹层子树里定位日历面板
    private static final int CAL_W = 268;
    private static final int CAL_H = 276;

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstDatePicker Demo — 基本 / 占位符 / 区间 / 尺寸档位 / 监听");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. 基本用法（已选日期）
        AstDatePicker dp1 = new AstDatePicker(LocalDate.of(2026, 8, 21));

        // 2. 空值 + 自定义占位符
        AstDatePicker dp2 = new AstDatePicker();
        dp2.setPlaceholder("请选择日期");

        // 3. 区间模式（固定起止范围）
        AstDatePicker dp3 = new AstDatePicker();
        dp3.setRangeMode(true);
        dp3.setDateRange(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 20));

        // 4. 尺寸档位（大 / 默认 / 小）
        JPanel sizes = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        sizes.setOpaque(false);
        AstDatePicker big = new AstDatePicker(LocalDate.of(2026, 8, 21));
        big.setSize(AstDatePicker.SIZE_LARGE);
        AstDatePicker mid = new AstDatePicker(LocalDate.of(2026, 8, 21));
        AstDatePicker small = new AstDatePicker(LocalDate.of(2026, 8, 21));
        small.setSize(AstDatePicker.SIZE_SMALL);
        sizes.add(big); sizes.add(mid); sizes.add(small);

        // 5. 选择变化监听
        AstDatePicker dp4 = new AstDatePicker();
        final JLabel status = new JLabel("未选择日期");
        status.setForeground(new Color(0x909399));
        dp4.setDateChangeListener(d -> status.setText("选中：" + d));

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        root.add(section("基本用法（已选日期）", dp1));
        root.add(Box.createVerticalStrut(18));
        root.add(section("空值 + 自定义占位符", dp2));
        root.add(Box.createVerticalStrut(18));
        root.add(section("区间模式（起止范围）", dp3));
        root.add(Box.createVerticalStrut(18));
        root.add(section("尺寸档位（大 / 默认 / 小）", sizes));
        root.add(Box.createVerticalStrut(18));
        root.add(section("选择变化监听", dp4));
        root.add(Box.createVerticalStrut(8));
        root.add(status);

        f.setContentPane(new JScrollPane(root));
        f.setSize(600, 430);
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

    /** 校验单值/区间 API、异常、FormValue、尺寸档位、弹层开合与点击选日、离屏绘制。 */
    static void selfCheck() {
        // 1) 单值 API 与异常
        AstDatePicker dp = new AstDatePicker(LocalDate.of(2026, 8, 21));
        assert dp.getDate().equals(LocalDate.of(2026, 8, 21)) : "getDate roundtrip";
        assert !dp.isRangeMode() : "默认非区间模式";

        boolean threw = false;
        try { dp.setDate(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setDate(null) must throw IAE";
        threw = false;
        try { dp.setPlaceholder(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setPlaceholder(null) must throw IAE";
        threw = false;
        try { dp.setDateChangeListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setDateChangeListener(null) must throw IAE";
        threw = false;
        try { dp.setSize(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setSize(9) must throw IAE";

        // 空值构造：未选中，FormValue 为空
        AstDatePicker empty = new AstDatePicker();
        assert empty.getDate() == null : "空选择器 getDate 为 null";
        assert empty.getFormValue().isEmpty() : "空选择器 getFormValue 为空";
        empty.setInvalid(true);
        empty.setInvalid(false); // 仅校验不抛错

        // 2) 尺寸档位高度单调 LARGE > DEFAULT > SMALL
        AstDatePicker sz = new AstDatePicker(LocalDate.of(2026, 8, 21));
        int hDef = sz.getPreferredSize().height;
        sz.setSize(AstDatePicker.SIZE_LARGE);
        int hL = sz.getPreferredSize().height;
        sz.setSize(AstDatePicker.SIZE_SMALL);
        int hS = sz.getPreferredSize().height;
        assert hL > hDef && hDef > hS
            : "档位高度应 LARGE>DEFAULT>SMALL, got " + hL + "/" + hDef + "/" + hS;

        // 3) FormValue 往返（单值）
        AstDatePicker fv = new AstDatePicker();
        fv.setDate(LocalDate.of(2026, 8, 24));
        assert fv.getFormValue().equals("2026-08-24") : "单值 getFormValue, got " + fv.getFormValue();
        fv.setFormValue("");
        assert fv.getFormValue().isEmpty() : "setFormValue(\"\") 清空";

        // 4) 区间 API 与异常
        AstDatePicker rp = new AstDatePicker();
        rp.setRangeMode(true);
        assert rp.isRangeMode() : "setRangeMode(true) 后 isRangeMode=true";
        rp.setDateRange(LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 5)); // end<start → 自动交换
        LocalDate[] rng = rp.getDateRange();
        assert rng[0].equals(LocalDate.of(2026, 8, 5)) && rng[1].equals(LocalDate.of(2026, 8, 20))
            : "end<start 应自动交换, got " + rng[0] + "~" + rng[1];
        assert rp.getFormValue().equals("2026-08-05~2026-08-20")
            : "区间 getFormValue, got " + rp.getFormValue();
        rp.setFormValue("2026-08-01~2026-08-10");
        LocalDate[] rng2 = rp.getDateRange();
        assert rng2[0].equals(LocalDate.of(2026, 8, 1)) && rng2[1].equals(LocalDate.of(2026, 8, 10))
            : "区间 setFormValue roundtrip";
        threw = false;
        try { rp.setDateRange(null, LocalDate.now()); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "setDateRange(null,..) must throw IAE";
        threw = false;
        try { rp.getDate(); } catch (IllegalStateException e) { threw = true; }
        assert threw : "区间模式 getDate 必须抛 ISE";
        threw = false;
        try { new AstDatePicker().getDateRange(); } catch (IllegalStateException e) { threw = true; }
        assert threw : "非区间模式 getDateRange 必须抛 ISE";
        threw = false;
        try { new AstDatePicker().setDateRange(LocalDate.now(), LocalDate.now()); } catch (IllegalStateException e) { threw = true; }
        assert threw : "非区间模式 setDateRange 必须抛 ISE";

        // 5) 弹层 GUI：单值点击选日 + 监听回调 + 自动收起；区间弹层含两块日历
        runSingleGuiCheck();
        runRangeGuiCheck();
        sleep(260); // 等收起动画定时器走完，避免残留 Swing Timer 拖住 JVM 退出

        // 6) 离屏绘制不抛错
        AstDatePicker pv = new AstDatePicker(LocalDate.of(2026, 8, 21));
        Dimension pd = pv.getPreferredSize();
        pv.setSize(pd);
        BufferedImage img = new BufferedImage(Math.max(1, pd.width), Math.max(1, pd.height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { pv.paint(gg); } finally { gg.dispose(); }

        System.out.println("AstDatePickerDemo self-check OK");
    }

    /** 单值弹层：弹出后点击某天，断言选中 2026-08-12、监听回调与自动收起。 */
    private static void runSingleGuiCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                JFrame jf = null;
                try {
                    jf = new JFrame("AstDatePickerDemo SC - single");
                    jf.setSize(760, 560);
                    jf.setVisible(true);
                    final LocalDate[] picked = {null};
                    AstDatePicker dp = new AstDatePicker(LocalDate.of(2026, 8, 21));
                    dp.setDateChangeListener(d -> picked[0] = d);
                    jf.getContentPane().setLayout(new FlowLayout());
                    jf.getContentPane().add(dp);
                    jf.pack();
                    dp.showDatePicker();
                    sleep(80);
                    assert dp.isOpen() : "showDatePicker 后 open=true";
                    JPanel cal = findFirstCalendarPanel(jf.getLayeredPane());
                    assert cal != null : "弹层内应含日历面板(" + CAL_W + "x" + CAL_H + ")";
                    cal.setSize(CAL_W, CAL_H);
                    // 网格第 3 行第 4 列 → 2026-08 视图下的 2026-08-12
                    cal.dispatchEvent(new MouseEvent(cal, MouseEvent.MOUSE_CLICKED,
                            System.currentTimeMillis(), 0, 134, 148, 1, false));
                    sleep(40);
                    assert dp.getDate() != null && dp.getDate().equals(LocalDate.of(2026, 8, 12))
                        : "点击应选中 2026-08-12，实际=" + dp.getDate();
                    assert picked[0] != null && picked[0].equals(LocalDate.of(2026, 8, 12))
                        : "监听回调应收到 2026-08-12，实际=" + picked[0];
                    assert !dp.isOpen() : "单值选中后应自动收起";
                } catch (Throwable ex) { err[0] = ex; }
                finally { if (jf != null) jf.dispose(); }
            }});
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
    }

    /** 区间弹层：断言弹出后含恰好两块日历面板（左起始月 / 右起始月+1）。 */
    private static void runRangeGuiCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                JFrame jf = null;
                try {
                    jf = new JFrame("AstDatePickerDemo SC - range");
                    jf.setSize(900, 620);
                    jf.setVisible(true);
                    AstDatePicker rp = new AstDatePicker();
                    rp.setRangeMode(true);
                    jf.getContentPane().setLayout(new FlowLayout());
                    jf.getContentPane().add(rp);
                    jf.pack();
                    rp.showDatePicker();
                    sleep(80);
                    assert rp.isOpen() : "区间 showDatePicker 后 open=true";
                    int cals = countCalendarPanels(jf.getLayeredPane());
                    assert cals == 2 : "区间弹层应含 2 块日历面板，实际=" + cals;
                    rp.hideDatePicker();
                    sleep(40);
                    assert !rp.isOpen() : "区间 hideDatePicker 后 open=false";
                } catch (Throwable ex) { err[0] = ex; }
                finally { if (jf != null) jf.dispose(); }
            }});
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
    }

    // --- 日历面板定位辅助 ---
    // CalendarPanel 是 AstDatePicker 的私有内部类，Demo 与组件不同包、无法直接引用其类型，
    // 故按类名后缀识别（运行时类名 org.swelement.ui.AstDatePicker$CalendarPanel）。

    private static boolean isCalendarPanel(Component c) {
        return c != null && c.getClass().getName().endsWith("CalendarPanel");
    }

    private static JPanel findFirstCalendarPanel(Container root) {
        java.util.Queue<Container> q = new java.util.LinkedList<Container>();
        q.add(root);
        while (!q.isEmpty()) {
            Container cur = q.poll();
            for (int i = 0; i < cur.getComponentCount(); i++) {
                Component ch = cur.getComponent(i);
                if (isCalendarPanel(ch)) return (JPanel) ch;
                if (ch instanceof Container) q.add((Container) ch);
            }
        }
        return null;
    }

    private static int countCalendarPanels(Container root) {
        int n = 0;
        java.util.Queue<Container> q = new java.util.LinkedList<Container>();
        q.add(root);
        while (!q.isEmpty()) {
            Container cur = q.poll();
            for (int i = 0; i < cur.getComponentCount(); i++) {
                Component ch = cur.getComponent(i);
                if (isCalendarPanel(ch)) n++;
                if (ch instanceof Container) q.add((Container) ch);
            }
        }
        return n;
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (Throwable ignore) {} }
}
