package org.swelement.ui;

import org.swelement.framework.AstAbstractComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 步骤条 — Element UI Steps 的 Java 实现。
 * 横向步骤：完成(WARNING 勾)/进行中(PROIMARY)/等待(边框灰)三种状态。
 *
 * 用法：
 *   List<String> steps = Arrays.asList("填写信息", "确认订单", "支付", "完成");
 *   AstSteps s = new AstSteps(steps);
 *   s.setCurrent(2); // 当前在"支付"
 *   s.setStepClickListener(idx -> System.out.println("点击第 " + idx));
 *
 * 设计：圆形节点(28px) + 标签 + 连接线。
 * - 已完成节点：SUCCESS 填充 + 白色对勾
 * - 进行中节点：PRIMARY 填充 + 高对比度数字（pickTextColorForBg 自动选择），外加 PRIMARY 描边光环
 * - 等待节点：白底 BORDER_BASE 描边 + TEXT_REGULAR 数字
 * 连接线：已完成段 SUCCESS 填充，未完成段 BORDER_BASE。
 * 标签：已完成/进行中 TEXT_PRIMARY，等待 TEXT_REGULAR。
 */
public class AstSteps extends AstAbstractComponent {
    public enum Direction { HORIZONTAL, VERTICAL }

    /** 单步状态（Element Plus 对齐）。显式设置后覆盖由 {@code current} 推导出的状态。 */
    public enum Status { WAIT, PROCESS, FINISH, ERROR, SUCCESS }

    private final List<String> steps = new ArrayList<String>();
    private final List<Status> statuses = new ArrayList<Status>();          // 与 steps 等长，元素可为 null
    private final List<AstIcon.Type> icons = new ArrayList<AstIcon.Type>();  // 与 steps 等长，元素可为 null
    private int current = 0;          // 当前进行中的步骤索引
    private Direction direction = Direction.HORIZONTAL;
    private boolean simple = false;   // 简洁风格：节点退化为小圆点，不画数字/对勾/图标
    private Consumer<Integer> stepClickListener;

    private static final int NODE_D = 28;      // 节点直径
    private static final int NODE_GAP = 8;       // 节点与连线间距
    private static final int LABEL_GAP = 8;      // 节点与标签间距
    private static final int FONT_NODE = 14;      // 节点数字
    private static final int FONT_LABEL = 14;    // 标签
    private static final int LINE_W = 2;          // 连接线宽
    private static final int SIMPLE_DOT_D = 8;        // 简洁风格圆点直径
    private static final int SIMPLE_DOT_ACTIVE_D = 10; // 简洁风格进行中圆点直径
    private static final int SIMPLE_LINE_W = 1;       // 简洁风格连线宽
    private static final int SIMPLE_H = 24;           // 简洁风格横向高度

    public AstSteps(List<String> steps) {
        setSteps0(steps);
    }

    private void setSteps0(List<String> steps) {
        if (steps == null) throw new IllegalArgumentException("steps must not be null");
        if (steps.isEmpty()) throw new IllegalArgumentException("steps must not be empty");
        for (String s : steps) if (s == null) throw new IllegalArgumentException("step must not be null");
        this.steps.clear();
        this.steps.addAll(steps);
        // 步骤列表整体替换 → 显式状态与图标一并清空（长度与 steps 对齐）
        this.statuses.clear();
        this.icons.clear();
        for (int i = 0; i < steps.size(); i++) { statuses.add(null); icons.add(null); }
    }

    public void setSteps(List<String> steps) {
        setSteps0(steps);
        // 步骤列表整体替换 → 当前步进含义已变，重置为 0
        this.current = 0;
        revalidate(); repaint();
    }

    public void setCurrent(int idx) {
        if (idx < 0 || idx >= steps.size())
            throw new IndexOutOfBoundsException("current out of range: " + idx);
        this.current = idx;
        repaint();
    }

    public int getCurrent() { return current; }

    public void setDirection(Direction d) {
        if (d == null) throw new IllegalArgumentException("direction must not be null");
        this.direction = d;
        revalidate(); repaint();
    }

    public void setStepClickListener(Consumer<Integer> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.stepClickListener = l;
    }

    // ---------- P3.3：状态 / 图标 / 简洁风格 ----------

    /** 设置第 idx 步的显式状态；传 {@code null} 表示清空，回落到由 {@code current} 推导。 */
    public void setStepStatus(int idx, Status s) {
        checkIndex(idx);
        statuses.set(idx, s);
        repaint();
    }

    /** 获取第 idx 步的显式状态；未设置返回 {@code null}。 */
    public Status getStepStatus(int idx) {
        checkIndex(idx);
        return statuses.get(idx);
    }

    /** 设置第 idx 步的图标；传 {@code null} 还原为序号/对勾。图标优先于默认内容绘制。 */
    public void setStepIcon(int idx, AstIcon.Type t) {
        checkIndex(idx);
        icons.set(idx, t);
        revalidate();
        repaint();
    }

    /** 获取第 idx 步的图标；未设置返回 {@code null}。 */
    public AstIcon.Type getStepIcon(int idx) {
        checkIndex(idx);
        return icons.get(idx);
    }

    /** 简洁风格：节点退化为小圆点、连线为细线，不画数字/对勾/图标。 */
    public void setSimple(boolean b) {
        this.simple = b;
        revalidate();
        repaint();
    }
    public boolean isSimple() { return simple; }

    /**
     * 解析第 idx 步的最终状态：显式 status 优先（覆盖 current 推导），
     * 否则 i&lt;current→FINISH、i==current→PROCESS、i&gt;current→WAIT。
     * 包内可见，供 selfCheck 行为断言。
     */
    Status resolveStatus(int idx) {
        Status s = statuses.get(idx);
        if (s != null) return s;
        if (idx < current) return Status.FINISH;
        if (idx == current) return Status.PROCESS;
        return Status.WAIT;
    }

    private void checkIndex(int idx) {
        if (idx < 0 || idx >= steps.size())
            throw new IndexOutOfBoundsException("step index out of range: " + idx);
    }

    @Override public Dimension getPreferredSize() {
        int n = steps.size();
        if (direction == Direction.HORIZONTAL) {
            int w = n * NODE_D + (n - 1) * (60 + 2 * NODE_GAP); // 连线 60px
            return new Dimension(Math.max(w, n * (NODE_D + 80)), simple ? SIMPLE_H : NODE_D + 24);
        } else {
            int node = simple ? SIMPLE_DOT_D : NODE_D;
            int seg = simple ? 20 : 28;
            int h = n * node + (n - 1) * (seg + 2 * NODE_GAP);
            return new Dimension(200, h);
        }
    }
    @Override public Dimension getMinimumSize() { return new Dimension(NODE_D, NODE_D); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        if (direction == Direction.HORIZONTAL) paintHorizontal(g2);
        else paintVertical(g2);
        g2.dispose();
    }

    private void paintHorizontal(Graphics2D g2) {
        int w = getWidth();
        int n = steps.size();
        int segW = (n > 1) ? (w - n * NODE_D) / (n - 1) : 0;
        int centerY = NODE_D / 2 + 4;
        for (int i = 0; i < n; i++) {
            int cx = i * (NODE_D + segW) + NODE_D / 2;
            paintNodeAndLabel(g2, i, cx, centerY, cx - NODE_D / 2, centerY + NODE_D / 2 + LABEL_GAP, true);
            // 连接线（到下一节点）
            if (i < n - 1) {
                int x1 = cx + NODE_D / 2 + NODE_GAP;
                int x2 = (i + 1) * (NODE_D + segW) + NODE_D / 2 - NODE_D / 2 - NODE_GAP;
                Status st = resolveStatus(i);
                boolean done = st == Status.FINISH || st == Status.SUCCESS;
                g2.setColor(done ? theme().getSuccess() : theme().getBorderBase());
                g2.setStroke(new BasicStroke(simple ? SIMPLE_LINE_W : LINE_W));
                g2.drawLine(x1, centerY, x2, centerY);
            }
        }
    }

    private void paintVertical(Graphics2D g2) {
        int n = steps.size();
        int cx = NODE_D / 2 + 4;
        int segH = simple ? 24 : 36;
        int startY = NODE_D / 2 + 4;
        for (int i = 0; i < n; i++) {
            int cy = startY + i * (NODE_D + segH);
            paintNodeAndLabel(g2, i, cx, cy, cx + NODE_D / 2 + LABEL_GAP, cy - NODE_D / 2 + 2, false);
            // 竖向连线
            if (i < n - 1) {
                int y1 = cy + NODE_D / 2 + NODE_GAP;
                int y2 = startY + (i + 1) * (NODE_D + segH) - NODE_D / 2 - NODE_GAP;
                Status st = resolveStatus(i);
                boolean done = st == Status.FINISH || st == Status.SUCCESS;
                g2.setColor(done ? theme().getSuccess() : theme().getBorderBase());
                g2.setStroke(new BasicStroke(simple ? SIMPLE_LINE_W : LINE_W));
                g2.drawLine(cx, y1, cx, y2);
            }
        }
    }

    private void paintNodeAndLabel(Graphics2D g2, int i, int cx, int cy, int lx, int ly, boolean labelCenter) {
        Status st = resolveStatus(i);
        boolean waiting = st == Status.WAIT;
        if (simple) {
            paintSimpleDot(g2, st, cx, cy);
        } else {
            paintFullNode(g2, i, st, cx, cy);
        }
        // 标签
        g2.setFont(theme().getFontBase().deriveFont(waiting ? Font.PLAIN : Font.BOLD, (float) FONT_LABEL));
        FontMetrics fmL = g2.getFontMetrics();
        String label = steps.get(i);
        Color labelCol = waiting ? theme().getTextRegular() : theme().getTextPrimary();
        if (waiting) assertContrast(theme().getTextRegular(), Color.WHITE, "AstSteps waiting label");
        else assertContrast(theme().getTextPrimary(), Color.WHITE, "AstSteps active label");
        g2.setColor(labelCol);
        if (labelCenter) {
            int tw = fmL.stringWidth(label);
            g2.drawString(label, lx + (NODE_D - tw) / 2 - (NODE_D / 2), ly);
        } else {
            g2.drawString(label, lx, ly + fmL.getAscent());
        }
    }

    /** 简洁风格节点：彩色小圆点，不画数字/对勾/图标。 */
    private void paintSimpleDot(Graphics2D g2, Status st, int cx, int cy) {
        int d = (st == Status.PROCESS) ? SIMPLE_DOT_ACTIVE_D : SIMPLE_DOT_D;
        Color c;
        switch (st) {
            case FINISH: case SUCCESS: c = theme().getSuccess(); break;
            case PROCESS:              c = theme().getPrimary(); break;
            case ERROR:                c = theme().getDanger();  break;
            default:                   c = theme().getBorderBase(); break;
        }
        g2.setColor(c);
        g2.fill(new Ellipse2D.Float(cx - d / 2f, cy - d / 2f, d, d));
    }

    /** 标准节点：圆底 +（图标 / 对勾 / 叉 / 序号）。 */
    private void paintFullNode(Graphics2D g2, int i, Status st, int cx, int cy) {
        float r = NODE_D / 2f;
        // 光环（仅进行中）
        if (st == Status.PROCESS) {
            Color primary = theme().getPrimary();
            g2.setColor(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 60));
            g2.fill(new Ellipse2D.Float(cx - r - 4, cy - r - 4, NODE_D + 8, NODE_D + 8));
        }
        // 节点圆
        if (st == Status.FINISH || st == Status.SUCCESS) {
            g2.setColor(theme().getSuccess());
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, NODE_D, NODE_D));
        } else if (st == Status.PROCESS) {
            g2.setColor(theme().getPrimary());
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, NODE_D, NODE_D));
        } else if (st == Status.ERROR) {
            g2.setColor(theme().getDanger());
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, NODE_D, NODE_D));
        } else {
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, NODE_D, NODE_D));
            g2.setColor(theme().getBorderBase());
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, NODE_D, NODE_D));
        }
        // 内容：图标优先，其次按状态画对勾 / 叉 / 序号
        AstIcon.Type ic = icons.get(i);
        if (ic != null) {
            int size = 16;
            Graphics2D ig = (Graphics2D) g2.create();
            ig.translate(cx - size / 2, cy - size / 2);
            AstIcon.paintIcon(ig, ic, st == Status.WAIT ? theme().getTextRegular() : Color.WHITE, size, 0f);
            ig.dispose();
            return;
        }
        if (st == Status.FINISH || st == Status.SUCCESS) {
            // 对勾
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int s = NODE_D;
            int ox = cx - Math.round(s * 0.02f);
            g2.drawLine(ox - 5, cy, ox - 1, cy + 4);
            g2.drawLine(ox - 1, cy + 4, ox + 6, cy - 4);
        } else if (st == Status.ERROR) {
            // 叉
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
            g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
        } else if (st == Status.PROCESS) {
            // 进行中节点数字：在主色背景上使用高对比度文字
            Color numColor = pickTextColorForBg(theme().getPrimary());
            g2.setFont(theme().getFontBase().deriveFont(Font.BOLD, (float) FONT_NODE));
            FontMetrics fm = g2.getFontMetrics();
            String num = String.valueOf(i + 1);
            int tx = cx - fm.stringWidth(num) / 2;
            int ty = cy - fm.getHeight() / 2 + fm.getAscent();
            g2.setColor(numColor);
            assertContrast(numColor, theme().getPrimary(), "AstSteps active node");
            g2.drawString(num, tx, ty);
        } else {
            // 等待态节点数字（白底圆内）：用 TEXT_REGULAR 保证对比度（TEXT_PLACEHOLDER 过淡）
            g2.setFont(theme().getFontBase().deriveFont(Font.BOLD, (float) FONT_NODE));
            FontMetrics fm = g2.getFontMetrics();
            String num = String.valueOf(i + 1);
            int tx = cx - fm.stringWidth(num) / 2;
            int ty = cy - fm.getHeight() / 2 + fm.getAscent();
            g2.setColor(theme().getTextRegular());
            assertContrast(theme().getTextRegular(), Color.WHITE, "AstSteps waiting node");
            g2.drawString(num, tx, ty);
        }
    }

    // --- Self-check ---
    @Override
    protected void selfCheck() {
        boolean threw = false;
        try { new AstSteps(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null steps"; threw = false;
        try { new AstSteps(new ArrayList<String>()); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "empty steps"; threw = false;
        try { new AstSteps(Arrays.asList("a", null)); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null step"; threw = false;
        try { new AstSteps(Arrays.asList("a")).setCurrent(1); } catch (IndexOutOfBoundsException e) { threw = true; }
        assert threw : "current OOB"; threw = false;
        try { new AstSteps(Arrays.asList("a")).setDirection(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null direction"; threw = false;
        try { new AstSteps(Arrays.asList("a")).setStepClickListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null listener";

        AstSteps s = new AstSteps(Arrays.asList("填写", "确认", "支付", "完成"));
        assert s.getCurrent() == 0 : "current 0 default";
        s.setCurrent(2);
        assert s.getCurrent() == 2 : "current 2";
        try { s.setCurrent(4); assert false; } catch (IndexOutOfBoundsException e) {}
        // setSteps resets current if needed
        s.setSteps(Arrays.asList("x", "y"));
        assert s.getCurrent() == 0 : "current reset";

        // 对比度：进行中节点文字 vs 主色背景
        assertContrast(pickTextColorForBg(theme().getPrimary()), theme().getPrimary(), "Steps active node text on primary");
        // 对比度：等待节点文字 vs 白色背景
        assertContrast(theme().getTextRegular(), Color.WHITE, "Steps waiting text on white");

        // paint both directions
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            AstSteps sh = new AstSteps(Arrays.asList("填写信息", "确认订单", "支付", "完成"));
            sh.setCurrent(2);
            sh.setBounds(0, 0, 400, 40);
            paintTo(sh, 400, 40);
            AstSteps sv = new AstSteps(Arrays.asList("步骤一", "步骤二", "步骤三"));
            sv.setDirection(Direction.VERTICAL);
            sv.setCurrent(1);
            sv.setBounds(0, 0, 200, 200);
            paintTo(sv, 200, 200);
            // current at 0 and last
            AstSteps s0 = new AstSteps(Arrays.asList("a", "b"));
            s0.setCurrent(0); s0.setBounds(0,0,200,40); paintTo(s0, 200, 40);
            s0.setCurrent(1); paintTo(s0, 200, 40);
            AstSteps sLast = new AstSteps(Arrays.asList("a", "b", "c"));
            sLast.setCurrent(2); sLast.setBounds(0,0,300,40); paintTo(sLast, 300, 40);
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);

        // ===== P3.3：状态 / 图标 / 简洁风格 =====
        AstSteps st = new AstSteps(Arrays.asList("步骤一", "步骤二", "步骤三", "步骤四"));
        st.setCurrent(2);
        // 未设显式状态时按 current 推导
        assert st.resolveStatus(0) == Status.FINISH : "derived FINISH, got " + st.resolveStatus(0);
        assert st.resolveStatus(2) == Status.PROCESS : "derived PROCESS, got " + st.resolveStatus(2);
        assert st.resolveStatus(3) == Status.WAIT : "derived WAIT, got " + st.resolveStatus(3);
        // 显式 status 覆盖 current 推导
        st.setStepStatus(0, Status.ERROR);
        assert st.resolveStatus(0) == Status.ERROR : "explicit ERROR must override derived FINISH";
        st.setStepStatus(3, Status.SUCCESS);
        assert st.resolveStatus(3) == Status.SUCCESS : "explicit SUCCESS must override derived WAIT";
        // 清空后回落到推导
        st.setStepStatus(0, null);
        assert st.resolveStatus(0) == Status.FINISH : "cleared status falls back to derived";
        assert st.getStepStatus(0) == null : "cleared status is null";
        threw = false;
        try { st.setStepStatus(9, Status.WAIT); } catch (IndexOutOfBoundsException e) { threw = true; }
        assert threw : "setStepStatus OOB";
        threw = false;
        try { st.setStepStatus(-1, Status.WAIT); } catch (IndexOutOfBoundsException e) { threw = true; }
        assert threw : "setStepStatus negative";

        // 图标
        AstSteps si = new AstSteps(Arrays.asList("a", "b"));
        si.setStepIcon(0, AstIcon.Type.USER);
        assert si.getStepIcon(0) == AstIcon.Type.USER : "icon set";
        si.setStepIcon(0, null);
        assert si.getStepIcon(0) == null : "icon cleared";
        threw = false;
        try { si.setStepIcon(5, AstIcon.Type.USER); } catch (IndexOutOfBoundsException e) { threw = true; }
        assert threw : "setStepIcon OOB";

        // 简洁风格尺寸
        AstSteps ss = new AstSteps(Arrays.asList("a", "b", "c"));
        assert !ss.isSimple() : "default not simple";
        int hNormal = ss.getPreferredSize().height;
        ss.setSimple(true);
        assert ss.isSimple() : "simple on";
        int hSimple = ss.getPreferredSize().height;
        assert hSimple < hNormal : "simple height smaller, got " + hSimple + " vs " + hNormal;

        // 五种状态 + 图标 + 简洁风格 的离屏绘制（横 / 竖 两套）
        final Throwable[] err2 = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            Status[] all = {Status.WAIT, Status.PROCESS, Status.FINISH, Status.ERROR, Status.SUCCESS};
            for (Status s : all) {
                AstSteps t = new AstSteps(Arrays.asList("a", "b", "c"));
                t.setCurrent(1);
                for (int i = 0; i < 3; i++) t.setStepStatus(i, s);
                t.setBounds(0, 0, 400, 60);
                paintTo(t, 400, 60);
                AstSteps tv = new AstSteps(Arrays.asList("a", "b", "c"));
                tv.setDirection(Direction.VERTICAL);
                for (int i = 0; i < 3; i++) tv.setStepStatus(i, s);
                tv.setBounds(0, 0, 200, 200);
                paintTo(tv, 200, 200);
            }
            // 图标渲染
            AstSteps ti = new AstSteps(Arrays.asList("a", "b"));
            ti.setStepIcon(0, AstIcon.Type.USER);
            ti.setStepIcon(1, AstIcon.Type.EDIT);
            ti.setBounds(0, 0, 300, 60);
            paintTo(ti, 300, 60);
            // 简洁风格（横 + 竖）
            AstSteps ts = new AstSteps(Arrays.asList("a", "b", "c"));
            ts.setSimple(true);
            ts.setCurrent(1);
            ts.setBounds(0, 0, 400, 24);
            paintTo(ts, 400, 24);
            AstSteps tsv = new AstSteps(Arrays.asList("a", "b", "c"));
            tsv.setDirection(Direction.VERTICAL);
            tsv.setSimple(true);
            tsv.setBounds(0, 0, 200, 150);
            paintTo(tsv, 200, 150);
            // 简洁 + 状态 + 图标（简洁下图标不画，节点退化为圆点）
            AstSteps tsi = new AstSteps(Arrays.asList("a", "b"));
            tsi.setSimple(true);
            tsi.setStepStatus(0, Status.ERROR);
            tsi.setStepIcon(1, AstIcon.Type.STAR);
            tsi.setBounds(0, 0, 300, 24);
            paintTo(tsi, 300, 24);
        }}); } catch (Throwable t) { err2[0] = t; }
        if (err2[0] != null) throw new RuntimeException(err2[0]);

        System.out.println("AstSteps self-check OK");
    }

    private static void paintTo(JComponent c, int w, int h) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        gg.setColor(Color.WHITE); gg.fillRect(0, 0, w, h);
        try { c.paint(gg); } finally { gg.dispose(); }
    }

    public static void main(String[] args) {
        new AstSteps(java.util.Arrays.asList("a", "b")).selfCheck();
    }
}
