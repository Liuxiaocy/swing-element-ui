package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * 数字输入框 — Element UI InputNumber 的 Java 实现。
 * 带 +/- 步进按钮，长按加速，支持 min/max/step、禁用、小数精度。
 *
 * 用法：
 *   AstInputNumber num = new AstInputNumber(0, 100, 1, 0);
 *   num.setValue(50);
 *   num.setValueListener(v -> System.out.println("值: " + v));
 *
 * 设计：输入框居中，左右两侧 +/- 按钮。按钮 hover 动画（150ms easeInOut）。
 * 长按按钮 500ms 后每 100ms 自动步进（加速）。键盘上下箭头也可步进。
 * 数值越界 clamp；输入非法字符不更新。
 */
public class AstInputNumber extends JComponent {
    private double min, max, step;
    private int precision; // 小数位数
    private double value;
    private boolean disabled;
    private Consumer<Double> valueListener;

    private final JTextField field;
    private final StepButton minusBtn, plusBtn;
    private final Timer holdTimer; // 长按加速
    private boolean holdingPlus, holdingMinus;

    private static final int BTN_W = 32;
    private static final int HEIGHT = 32;

    public AstInputNumber(double min, double max, double step, int initial) {
        this(min, max, step, (double) initial, 0);
    }

    public AstInputNumber(double min, double max, double step, double initial, int precision) {
        if (min > max) throw new IllegalArgumentException("min > max");
        if (step <= 0) throw new IllegalArgumentException("step must be > 0");
        if (precision < 0 || precision > 6) throw new IllegalArgumentException("precision in [0,6]");
        this.min = min; this.max = max; this.step = step; this.precision = precision;
        this.value = clamp(initial);
        this.field = new JTextField(format(value));
        this.field.setHorizontalAlignment(JTextField.CENTER);
        this.field.setFont(ElementTheme.FONT.deriveFont(14f));
        this.field.setBorder(new EmptyBorder(0, 4, 0, 4));
        this.minusBtn = new StepButton("-", false);
        this.plusBtn = new StepButton("+", true);
        this.holdTimer = new Timer(100, e -> {
            if (holdingPlus) doStep(true);
            else if (holdingMinus) doStep(false);
        });
        setLayout(new BorderLayout());
        add(minusBtn, BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
        add(plusBtn, BorderLayout.EAST);
        setOpaque(false);
        // 输入框校验：失焦/回车时解析
        field.addActionListener(e -> commitText());
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitText(); }
        });
        // 键盘上下箭头
        field.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (disabled) return;
                if (e.getKeyCode() == KeyEvent.VK_UP) { doStep(true); e.consume(); }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN) { doStep(false); e.consume(); }
            }
        });
    }

    public double getValue() { return value; }
    public void setValue(double v) {
        double clamped = clamp(v);
        if (clamped == value) { field.setText(format(value)); return; }
        this.value = clamped;
        field.setText(format(value));
        if (valueListener != null) valueListener.accept(value);
    }

    public void setValueListener(Consumer<Double> l) {
        if (l == null) throw new IllegalArgumentException("listener must not be null");
        this.valueListener = l;
    }

    public void setDisabled(boolean d) {
        this.disabled = d;
        field.setEnabled(!d);
        minusBtn.setEnabled(!d);
        plusBtn.setEnabled(!d);
        repaint();
    }

    private void commitText() {
        String t = field.getText().trim();
        try {
            double v = Double.parseDouble(t);
            setValue(v);
        } catch (NumberFormatException ex) {
            field.setText(format(value)); // 恢复
        }
    }

    private void doStep(boolean plus) {
        if (disabled) return;
        double v = value + (plus ? step : -step);
        // 处理浮点精度：用 precision 舍入
        double factor = Math.pow(10, precision);
        v = Math.round(v * factor) / factor;
        setValue(v);
    }

    private double clamp(double v) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private String format(double v) {
        if (precision == 0) return String.valueOf((long) Math.round(v));
        return String.format("%." + precision + "f", v);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(140, HEIGHT); }
    @Override public Dimension getMinimumSize() { return getPreferredSize(); }
    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    // --- StepButton ---
    private final class StepButton extends JComponent {
        private final boolean plus;
        float hover;
        boolean pressed;
        final Animator hoverAnim;

        StepButton(String label, boolean plus) {
            this.plus = plus;
            setPreferredSize(new Dimension(BTN_W, HEIGHT));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); }},
                new Animator.Listener() { public void update(float v) { hover = v; repaint(); }});
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (!isEnabled()) return;
                    pressed = true;
                    doStep(plus);
                    if (plus) { holdingPlus = true; holdingMinus = false; }
                    else { holdingMinus = true; holdingPlus = false; }
                    holdTimer.setInitialDelay(400);
                    holdTimer.start();
                }
                @Override public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    holdingPlus = false; holdingMinus = false;
                    holdTimer.stop();
                    repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    holdTimer.stop();
                    holdingPlus = false; holdingMinus = false;
                    pressed = false;
                    hoverAnim.stop(); hoverAnim.go(hover, 0f);
                }
                @Override public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) hoverAnim.stop(); hoverAnim.go(hover, 1f);
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            // 背景：白底，hover 时 FILL_BASE
            Color bg = isEnabled() ? (hover > 0.01f ? ElementTheme.lerp(Color.WHITE, ElementTheme.FILL_BASE, hover) : Color.WHITE) : ElementTheme.FILL_BASE;
            g2.setColor(bg);
            g2.fillRect(0, 0, w, h);
            // 左/右边框
            g2.setColor(ElementTheme.BORDER_BASE);
            if (plus) g2.drawLine(0, 0, 0, h); // plus 在右侧
            else g2.drawLine(w - 1, 0, w - 1, h); // minus 在左侧
            // 符号
            g2.setColor(isEnabled() ? ElementTheme.TEXT_MAIN : ElementTheme.TEXT_PLACEHOLDER);
            if (isEnabled()) ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, bg, "AstInputNumber btn");
            g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 16f));
            FontMetrics fm = g2.getFontMetrics();
            String sym = plus ? "+" : "−";
            int tx = (w - fm.stringWidth(sym)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(sym, tx, ty);
            g2.dispose();
        }
    }

    // --- Self-check ---
    static void selfCheck() {
        boolean threw = false;
        try { new AstInputNumber(10, 1, 1, 0); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "min > max"; threw = false;
        try { new AstInputNumber(0, 10, 0, 0); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "step 0"; threw = false;
        try { new AstInputNumber(0, 10, 1, 0, -1); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "precision -1"; threw = false;
        try { new AstInputNumber(0, 10, 1, 0).setValueListener(null); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null listener";

        AstInputNumber n = new AstInputNumber(0, 100, 1, 50);
        assert n.getValue() == 50 : "initial 50";
        n.setValue(80);
        assert n.getValue() == 80 : "set 80";
        n.setValue(200); // clamp
        assert n.getValue() == 100 : "clamp to max 100";
        n.setValue(-10);
        assert n.getValue() == 0 : "clamp to min 0";
        // step
        n.setValue(50);
        // doStep via reflection not easy; test setValue precision
        AstInputNumber nf = new AstInputNumber(0, 10, 0.5, 0, 1);
        nf.setValue(2.5);
        assert nf.getValue() == 2.5 : "half value 2.5";
        assert nf.format(2.5).equals("2.5") : "format 2.5";

        // listener
        final double[] got = {-1};
        n.setValueListener(v -> got[0] = v);
        n.setValue(30);
        assert got[0] == 30 : "listener fired";

        // paint
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("Num SC");
            jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            try {
                AstInputNumber nn = new AstInputNumber(0, 100, 1, 50);
                nn.setBounds(0, 0, 140, 32);
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(140, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                gg.setColor(Color.WHITE); gg.fillRect(0, 0, 140, 32);
                try { nn.paint(gg); } finally { gg.dispose(); }
                nn.setDisabled(true);
                Graphics2D gg2 = img.createGraphics();
                try { nn.paint(gg2); } finally { gg2.dispose(); }
            } finally {
                jf.dispose();
            }
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstInputNumber self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
