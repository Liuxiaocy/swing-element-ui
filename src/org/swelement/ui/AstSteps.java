package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

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
 * - 已完成节点：WARNING 填充 + 白色对勾
 * - 进行中节点：PRIMARY 填充 + 白色数字，外加 PRIMARY 描边光环
 * - 等待节点：白底 BORDER_BASE 描边 + TEXT_PLACEHOLDER 数字
 * 连接线：已完成段 SUCCESS 填充，未完成段 BORDER_BASE。
 * 标签：已完成/进行中 TEXT_MAIN，等待 TEXT_PLACEHOLDER。
 */
public class AstSteps extends JComponent {
    public enum Direction { HORIZONTAL, VERTICAL }

    private final List<String> steps = new ArrayList<String>();
    private int current = 0;          // 当前进行中的步骤索引
    private Direction direction = Direction.HORIZONTAL;
    private Consumer<Integer> stepClickListener;

    private static final int NODE_D = 28;      // 节点直径
    private static final int NODE_GAP = 8;       // 节点与连线间距
    private static final int LABEL_GAP = 8;      // 节点与标签间距
    private static final int FONT_NODE = 14;      // 节点数字
    private static final int FONT_LABEL = 14;    // 标签
    private static final int LINE_W = 2;          // 连接线宽

    public AstSteps(List<String> steps) {
        setSteps0(steps);
        setOpaque(false);
    }

    private void setSteps0(List<String> steps) {
        if (steps == null) throw new IllegalArgumentException("steps must not be null");
        if (steps.isEmpty()) throw new IllegalArgumentException("steps must not be empty");
        for (String s : steps) if (s == null) throw new IllegalArgumentException("step must not be null");
        this.steps.clear();
        this.steps.addAll(steps);
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

    @Override public Dimension getPreferredSize() {
        if (direction == Direction.HORIZONTAL) {
            int n = steps.size();
            int w = n * NODE_D + (n - 1) * (60 + 2 * NODE_GAP); // 连线 60px
            return new Dimension(Math.max(w, n * (NODE_D + 80)), NODE_D + 24);
        } else {
            int n = steps.size();
            int h = n * NODE_D + (n - 1) * (28 + 2 * NODE_GAP);
            return new Dimension(200, h);
        }
    }
    @Override public Dimension getMinimumSize() { return new Dimension(NODE_D, NODE_D); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
                boolean done = i < current;
                g2.setColor(done ? ElementTheme.SUCCESS : ElementTheme.BORDER_BASE);
                g2.setStroke(new BasicStroke(LINE_W));
                g2.drawLine(x1, centerY, x2, centerY);
            }
        }
    }

    private void paintVertical(Graphics2D g2) {
        int n = steps.size();
        int cx = NODE_D / 2 + 4;
        int segH = 36;
        int startY = NODE_D / 2 + 4;
        for (int i = 0; i < n; i++) {
            int cy = startY + i * (NODE_D + segH);
            paintNodeAndLabel(g2, i, cx, cy, cx + NODE_D / 2 + LABEL_GAP, cy - NODE_D / 2 + 2, false);
            // 竖向连线
            if (i < n - 1) {
                int y1 = cy + NODE_D / 2 + NODE_GAP;
                int y2 = startY + (i + 1) * (NODE_D + segH) - NODE_D / 2 - NODE_GAP;
                boolean done = i < current;
                g2.setColor(done ? ElementTheme.SUCCESS : ElementTheme.BORDER_BASE);
                g2.setStroke(new BasicStroke(LINE_W));
                g2.drawLine(cx, y1, cx, y2);
            }
        }
    }

    private void paintNodeAndLabel(Graphics2D g2, int i, int cx, int cy, int lx, int ly, boolean labelCenter) {
        float r = NODE_D / 2f;
        boolean done = i < current;
        boolean active = i == current;
        // 光环（仅进行中）
        if (active) {
            g2.setColor(new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), 60));
            g2.fill(new Ellipse2D.Float(cx - r - 4, cy - r - 4, NODE_D + 8, NODE_D + 8));
        }
        // 节点圆
        if (done) {
            g2.setColor(ElementTheme.SUCCESS);
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, NODE_D, NODE_D));
        } else if (active) {
            g2.setColor(ElementTheme.PRIMARY);
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, NODE_D, NODE_D));
        } else {
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, NODE_D, NODE_D));
            g2.setColor(ElementTheme.BORDER_BASE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, NODE_D, NODE_D));
        }
        // 数字或对勾
        if (done) {
            // 对勾
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int s = NODE_D;
            int ox = cx - Math.round(s * 0.02f);
            g2.drawLine(ox - 5, cy, ox - 1, cy + 4);
            g2.drawLine(ox - 1, cy + 4, ox + 6, cy - 4);
        } else {
            // 等待态节点数字（白底圆内）：用 TEXT_REGULAR 保证对比度（TEXT_PLACEHOLDER 过淡）
            g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, (float) FONT_NODE));
            FontMetrics fm = g2.getFontMetrics();
            String num = String.valueOf(i + 1);
            int tx = cx - fm.stringWidth(num) / 2;
            int ty = cy - fm.getHeight() / 2 + fm.getAscent();
            g2.setColor(ElementTheme.TEXT_REGULAR);
            ElementTheme.assertContrast(ElementTheme.TEXT_REGULAR, Color.WHITE, "AstSteps waiting node");
            g2.drawString(num, tx, ty);
        }
        // 标签
        g2.setFont(ElementTheme.FONT.deriveFont(done || active ? Font.BOLD : Font.PLAIN, (float) FONT_LABEL));
        FontMetrics fmL = g2.getFontMetrics();
        String label = steps.get(i);
        Color labelCol = (done || active) ? ElementTheme.TEXT_MAIN : ElementTheme.TEXT_REGULAR;
        if (done || active) ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, Color.WHITE, "AstSteps active label");
        else ElementTheme.assertContrast(ElementTheme.TEXT_REGULAR, Color.WHITE, "AstSteps waiting label");
        g2.setColor(labelCol);
        if (labelCenter) {
            int tw = fmL.stringWidth(label);
            g2.drawString(label, lx + (NODE_D - tw) / 2 - (NODE_D / 2), ly);
        } else {
            g2.drawString(label, lx, ly + fmL.getAscent());
        }
    }

    // --- Self-check ---
    static void selfCheck() {
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
        System.out.println("AstSteps self-check OK");
    }

    private static void paintTo(JComponent c, int w, int h) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        gg.setColor(Color.WHITE); gg.fillRect(0, 0, w, h);
        try { c.paint(gg); } finally { gg.dispose(); }
    }

    public static void main(String[] args) { selfCheck(); }
}
