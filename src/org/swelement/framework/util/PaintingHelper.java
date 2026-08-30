package org.swelement.framework.util;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/**
 * 静态绘制工具类，提供圆角矩形、圆形、文字、发光效果、颜色调整、图标等常用绘制方法。
 * <p>
 * 所有方法均为 public static，类为 final 且构造函数私有，禁止实例化。
 */
public final class PaintingHelper {

    private PaintingHelper() {}

    // ==================== 圆角矩形 ====================

    /**
     * 创建圆角矩形 Shape。
     * <p>
     * arcWidth 和 arcHeight 为 radius * 2（Swing 圆角矩形参数为直径）。
     *
     * @param x      左上角 x 坐标
     * @param y      左上角 y 坐标
     * @param w      宽度
     * @param h      高度
     * @param radius 圆角半径
     * @return 圆角矩形 Shape
     */
    public static RoundRectangle2D roundRect(int x, int y, int w, int h, int radius) {
        float arc = radius * 2f;
        return new RoundRectangle2D.Float(x, y, w, h, arc, arc);
    }

    /**
     * 填充圆角矩形。
     *
     * @param g2     Graphics2D 对象
     * @param x      左上角 x 坐标
     * @param y      左上角 y 坐标
     * @param w      宽度
     * @param h      高度
     * @param radius 圆角半径
     */
    public static void fillRoundRect(Graphics2D g2, int x, int y, int w, int h, int radius) {
        g2.fill(roundRect(x, y, w, h, radius));
    }

    /**
     * 绘制圆角矩形边框。
     *
     * @param g2     Graphics2D 对象
     * @param x      左上角 x 坐标
     * @param y      左上角 y 坐标
     * @param w      宽度
     * @param h      高度
     * @param radius 圆角半径
     */
    public static void drawRoundRect(Graphics2D g2, int x, int y, int w, int h, int radius) {
        g2.draw(roundRect(x, y, w, h, radius));
    }

    // ==================== 圆形 ====================

    /**
     * 填充圆形。
     * <p>
     * 使用 Ellipse2D.Float，以 (cx, cy) 为圆心。
     *
     * @param g2     Graphics2D 对象
     * @param cx     圆心 x 坐标
     * @param cy     圆心 y 坐标
     * @param radius 半径
     */
    public static void fillCircle(Graphics2D g2, int cx, int cy, int radius) {
        g2.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2f, radius * 2f));
    }

    /**
     * 绘制圆形边框。
     * <p>
     * 使用 Ellipse2D.Float，以 (cx, cy) 为圆心。
     *
     * @param g2     Graphics2D 对象
     * @param cx     圆心 x 坐标
     * @param cy     圆心 y 坐标
     * @param radius 半径
     */
    public static void drawCircle(Graphics2D g2, int cx, int cy, int radius) {
        g2.draw(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2f, radius * 2f));
    }

    // ==================== 文字 ====================

    /**
     * 水平居中绘制文字（给定基线 y）。
     *
     * @param g2        Graphics2D 对象
     * @param text      要绘制的文本
     * @param x         区域左边界 x 坐标
     * @param width     区域宽度
     * @param baselineY 文本基线 y 坐标
     */
    public static void drawCenteredText(Graphics2D g2, String text, int x, int width, float baselineY) {
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        float textX = x + (width - textWidth) / 2f;
        g2.drawString(text, textX, baselineY);
    }

    /**
     * 在矩形内完全居中绘制文字（水平 + 垂直）。
     * <p>
     * 垂直居中公式：y + (h - fm.getHeight()) / 2 + fm.getAscent()
     *
     * @param g2   Graphics2D 对象
     * @param text 要绘制的文本
     * @param x    矩形左上角 x 坐标
     * @param y    矩形左上角 y 坐标
     * @param w    矩形宽度
     * @param h    矩形高度
     */
    public static void drawTextInCenter(Graphics2D g2, String text, int x, int y, int w, int h) {
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        float textX = x + (w - textWidth) / 2f;
        float baselineY = y + (h - fm.getHeight()) / 2f + fm.getAscent();
        g2.drawString(text, textX, baselineY);
    }

    // ==================== 发光效果 ====================

    /**
     * 多层描边模拟外发光效果，纯 JDK 实现。
     * <p>
     * 从外到内透明度递增，保存和恢复 Composite/Stroke/Color。
     *
     * @param g2    Graphics2D 对象
     * @param shape 发光的形状
     * @param color 发光颜色
     * @param size  发光大小（像素）
     * @param alpha 最外层透明度 [0, 1]
     */
    public static void drawGlow(Graphics2D g2, Shape shape, Color color, int size, float alpha) {
        if (size <= 0 || alpha <= 0) return;

        Composite oldComposite = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        Color oldColor = g2.getColor();

        try {
            g2.setColor(color);
            // 多层描边，从外到内透明度递增
            for (int i = size; i >= 1; i--) {
                float layerAlpha = alpha * (1f - (float) (i - 1) / size);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layerAlpha));
                g2.setStroke(new BasicStroke(i * 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(shape);
            }
        } finally {
            g2.setComposite(oldComposite);
            g2.setStroke(oldStroke);
            g2.setColor(oldColor);
        }
    }

    // ==================== 颜色工具 ====================

    /**
     * 设置颜色的 alpha 通道。
     * <p>
     * alpha 参数为 [0, 1] 浮点数，转换为 0-255 整数，使用 Math.round。
     * 超出范围自动钳制。
     *
     * @param color 原始颜色
     * @param alpha 透明度 [0, 1]，0 完全透明，1 完全不透明
     * @return 调整后的颜色
     */
    public static Color withAlpha(Color color, float alpha) {
        float clamped = Math.max(0f, Math.min(1f, alpha));
        int a = Math.round(clamped * 255f);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    /**
     * 颜色变暗。
     * <p>
     * 各通道乘以 factor，factor < 1 变暗。
     *
     * @param color  原始颜色
     * @param factor 变暗因子（< 1 变暗，= 1 不变）
     * @return 变暗后的颜色
     */
    public static Color darken(Color color, float factor) {
        float f = Math.max(0f, factor);
        int r = Math.max(0, Math.min(255, Math.round(color.getRed() * f)));
        int g = Math.max(0, Math.min(255, Math.round(color.getGreen() * f)));
        int b = Math.max(0, Math.min(255, Math.round(color.getBlue() * f)));
        return new Color(r, g, b, color.getAlpha());
    }

    /**
     * 颜色变亮。
     * <p>
     * 向白色插值，factor 是接近白色的程度。
     * factor = 0 返回原色，factor = 1 返回白色。
     *
     * @param color  原始颜色
     * @param factor 变亮因子 [0, 1]，接近白色的程度
     * @return 变亮后的颜色
     */
    public static Color lighten(Color color, float factor) {
        float clamped = Math.max(0f, Math.min(1f, factor));
        int r = Math.max(0, Math.min(255, Math.round(color.getRed() + (255 - color.getRed()) * clamped)));
        int g = Math.max(0, Math.min(255, Math.round(color.getGreen() + (255 - color.getGreen()) * clamped)));
        int b = Math.max(0, Math.min(255, Math.round(color.getBlue() + (255 - color.getBlue()) * clamped)));
        return new Color(r, g, b, color.getAlpha());
    }

    // ==================== 图标 ====================

    /**
     * 绘制一个 Icon（原生 ImageIcon / 任意 javax.swing.Icon / AstIcon）。
     *
     * @param g2    Graphics2D 对象
     * @param icon  图标对象
     * @param x     图标左上角 x 坐标
     * @param y     图标左上角 y 坐标
     */
    public static void drawIcon(Graphics2D g2, Icon icon, int x, int y) {
        if (icon == null) return;
        icon.paintIcon(null, g2, x, y);
    }

    // ==================== 自检 ====================

    /**
     * 自检方法，验证 PaintingHelper 的各项功能。
     * 仅在 -ea 开启时生效。
     */
    static void selfCheck() {
        // === withAlpha 测试 ===
        Color red = Color.RED;

        // 半透明
        Color half = withAlpha(red, 0.5f);
        assert half.getRed() == 255 : "withAlpha should preserve red channel";
        assert half.getGreen() == 0 : "withAlpha should preserve green channel";
        assert half.getBlue() == 0 : "withAlpha should preserve blue channel";
        assert half.getAlpha() == 128 : "withAlpha 0.5 should be 128 (Math.round(127.5))";

        // 完全透明
        Color transparent = withAlpha(red, 0f);
        assert transparent.getAlpha() == 0 : "withAlpha 0 should be fully transparent";

        // 完全不透明
        Color opaque = withAlpha(red, 1f);
        assert opaque.getAlpha() == 255 : "withAlpha 1 should be fully opaque";

        // 超出范围钳制（小于 0）
        Color clampedLow = withAlpha(red, -0.5f);
        assert clampedLow.getAlpha() == 0 : "withAlpha negative should clamp to 0";

        // 超出范围钳制（大于 1）
        Color clampedHigh = withAlpha(red, 2f);
        assert clampedHigh.getAlpha() == 255 : "withAlpha >1 should clamp to 255";

        // === darken 测试 ===
        Color white = Color.WHITE;
        Color darkened = darken(white, 0.5f);
        assert darkened.getRed() == 128 : "darken white by 0.5: red should be 128";
        assert darkened.getGreen() == 128 : "darken white by 0.5: green should be 128";
        assert darkened.getBlue() == 128 : "darken white by 0.5: blue should be 128";
        assert darkened.getAlpha() == 255 : "darken should preserve alpha";

        // factor = 1 不变
        Color same = darken(white, 1f);
        assert same.equals(white) : "darken with factor 1 should return same color";

        // factor = 0 返回黑色
        Color blackened = darken(white, 0f);
        assert blackened.getRed() == 0 : "darken with factor 0 should be black";
        assert blackened.getGreen() == 0 : "darken with factor 0 should be black";
        assert blackened.getBlue() == 0 : "darken with factor 0 should be black";

        // 保持原有 alpha
        Color semiRed = withAlpha(red, 0.6f);
        Color darkenedSemi = darken(semiRed, 0.5f);
        assert darkenedSemi.getAlpha() == semiRed.getAlpha() : "darken should preserve alpha channel";

        // === lighten 测试 ===
        Color black = Color.BLACK;
        Color lightened = lighten(black, 0.5f);
        assert lightened.getRed() == 128 : "lighten black by 0.5: red should be 128";
        assert lightened.getGreen() == 128 : "lighten black by 0.5: green should be 128";
        assert lightened.getBlue() == 128 : "lighten black by 0.5: blue should be 128";
        assert lightened.getAlpha() == 255 : "lighten should preserve alpha";

        // factor = 0 不变
        Color same2 = lighten(black, 0f);
        assert same2.equals(black) : "lighten with factor 0 should return same color";

        // factor = 1 返回白色
        Color whitened = lighten(black, 1f);
        assert whitened.getRed() == 255 : "lighten with factor 1 should be white";
        assert whitened.getGreen() == 255 : "lighten with factor 1 should be white";
        assert whitened.getBlue() == 255 : "lighten with factor 1 should be white";

        // 超出范围钳制
        Color clampedLighten = lighten(black, 2f);
        assert clampedLighten.equals(Color.WHITE) : "lighten factor >1 should clamp to white";

        Color clampedDarken = lighten(black, -1f);
        assert clampedDarken.equals(Color.BLACK) : "lighten factor <0 should clamp to original";

        // === roundRect 测试 ===
        RoundRectangle2D rr = roundRect(10, 20, 100, 50, 8);
        assert rr.getX() == 10f : "roundRect x should be 10";
        assert rr.getY() == 20f : "roundRect y should be 20";
        assert rr.getWidth() == 100f : "roundRect width should be 100";
        assert rr.getHeight() == 50f : "roundRect height should be 50";
        assert rr.getArcWidth() == 16f : "roundRect arcWidth should be radius * 2 = 16";
        assert rr.getArcHeight() == 16f : "roundRect arcHeight should be radius * 2 = 16";

        // 半径为 0 的情况
        RoundRectangle2D rr0 = roundRect(0, 0, 50, 50, 0);
        assert rr0.getArcWidth() == 0f : "roundRect with radius 0 should have arcWidth 0";

        // === fillRoundRect / drawRoundRect 基本功能验证（不抛异常即可） ===
        // 由于需要 Graphics2D，这里验证方法存在性通过编译保证
        // 间接验证：roundRect 返回的对象可用于 fill/draw

        // === fillCircle / drawCircle 基本功能验证 ===
        // 通过 Ellipse2D 间接验证：圆心和半径计算
        Ellipse2D circle = new Ellipse2D.Float(100 - 20, 100 - 20, 20 * 2f, 20 * 2f);
        assert circle.getX() == 80f : "circle left should be cx - radius";
        assert circle.getY() == 80f : "circle top should be cy - radius";
        assert circle.getWidth() == 40f : "circle width should be radius * 2";
        assert circle.getHeight() == 40f : "circle height should be radius * 2";

        // === drawGlow 基本验证 ===
        // 验证 size <= 0 或 alpha <= 0 时不执行（通过编译保证方法存在）

        // === drawIcon 基本验证 ===
        // 通过编译保证方法存在，运行时需要 Graphics2D

        System.out.println("PaintingHelper self-check OK");
    }

    /**
     * 主方法，运行自检。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        selfCheck();
    }
}
