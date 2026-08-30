package org.swelement.core;

import java.awt.Color;

/**
 * 自检基类，提供对比度断言等通用工具方法。
 * <p>
 * 所有组件的自检类可继承此类，复用亮度计算、对比度断言、
 * 浮点近似断言、尺寸断言等通用功能。
 */
public abstract class SelfCheckBase {

    /**
     * 对比度断言（默认 4.5:1 AA 级）。
     * 仅在 -ea 开启时生效。
     *
     * @param fg    前景色
     * @param bg    背景色
     * @param where 位置描述，用于失败时的错误信息
     */
    protected void assertContrast(Color fg, Color bg, String where) {
        assertContrast(fg, bg, where, 4.5f);
    }

    /**
     * 指定最小比例的对比度断言。
     * 仅在 -ea 开启时生效。
     *
     * @param fg       前景色
     * @param bg       背景色
     * @param where    位置描述，用于失败时的错误信息
     * @param minRatio 最小对比度比例（非文本用 3.0f）
     */
    protected void assertContrast(Color fg, Color bg, String where, float minRatio) {
        float l1 = luminance(fg);
        float l2 = luminance(bg);
        float lighter = Math.max(l1, l2);
        float darker = Math.min(l1, l2);
        float ratio = (lighter + 0.05f) / (darker + 0.05f);
        assert ratio >= minRatio : "[CONTRAST FAIL " + where + "] ratio=" + String.format("%.2f", ratio)
                + " (need >= " + String.format("%.2f", minRatio) + ")"
                + " fg=RGB(" + fg.getRed() + "," + fg.getGreen() + "," + fg.getBlue() + ")"
                + " bg=RGB(" + bg.getRed() + "," + bg.getGreen() + "," + bg.getBlue() + ")";
    }

    /**
     * 浮点近似断言。
     * 仅在 -ea 开启时生效。
     *
     * @param expected 期望值
     * @param actual   实际值
     * @param epsilon  允许的误差范围
     * @param msg      失败时的描述信息
     */
    protected void assertApprox(float expected, float actual, float epsilon, String msg) {
        assert Math.abs(expected - actual) <= epsilon : msg
                + " (expected=" + expected + ", actual=" + actual + ", epsilon=" + epsilon + ")";
    }

    /**
     * 尺寸断言。
     * 仅在 -ea 开启时生效。
     *
     * @param expected 期望值
     * @param actual   实际值
     * @param msg      失败时的描述信息
     */
    protected void assertDimension(int expected, int actual, String msg) {
        assert expected == actual : msg + " (expected=" + expected + ", actual=" + actual + ")";
    }

    /**
     * 计算颜色的 WCAG 相对亮度。
     *
     * @param c 颜色
     * @return 相对亮度值，范围 [0, 1]
     */
    protected float luminance(Color c) {
        return 0.2126f * srgb(c.getRed())
                + 0.7152f * srgb(c.getGreen())
                + 0.0722f * srgb(c.getBlue());
    }

    /**
     * 计算两个颜色的对比度。
     *
     * @param a 颜色 a
     * @param b 颜色 b
     * @return 对比度值
     */
    protected float contrastRatio(Color a, Color b) {
        float l1 = luminance(a);
        float l2 = luminance(b);
        float lighter = Math.max(l1, l2);
        float darker = Math.min(l1, l2);
        return (lighter + 0.05f) / (darker + 0.05f);
    }

    /**
     * 颜色线性插值。
     *
     * @param a 起始颜色
     * @param b 结束颜色
     * @param t 插值参数 [0, 1]
     * @return 插值后的颜色
     */
    protected Color lerp(Color a, Color b, float t) {
        return new Color(
                lerp(a.getRed(), b.getRed(), t),
                lerp(a.getGreen(), b.getGreen(), t),
                lerp(a.getBlue(), b.getBlue(), t));
    }

    /**
     * 整数线性插值。
     *
     * @param a 起始值
     * @param b 结束值
     * @param t 插值参数
     * @return 插值结果
     */
    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    /**
     * sRGB 线性化转换。
     *
     * @param v 8 位颜色分量值 [0, 255]
     * @return 线性化后的分量值
     */
    private static float srgb(int v) {
        float vv = v / 255f;
        return vv <= 0.03928f ? vv / 12.92f : (float) Math.pow((vv + 0.055) / 1.055, 2.4);
    }

    // === 自检 ===

    /**
     * 自检方法，验证 SelfCheckBase 的各项功能。
     * 仅在 -ea 开启时生效。
     */
    static void selfCheck() {
        // 使用内部子类进行测试
        SelfCheckBase checker = new SelfCheckBase() {};

        // 黑白亮度值正确
        assert checker.luminance(Color.BLACK) < 0.01f : "black luminance should be near 0";
        assert checker.luminance(Color.WHITE) > 0.99f : "white luminance should be near 1";

        // 黑白对比度 > 20:1
        float bwRatio = checker.contrastRatio(Color.BLACK, Color.WHITE);
        assert bwRatio > 20f : "black-white contrast should be > 20:1, got " + bwRatio;

        // TEXT_REGULAR on WHITE 通过 AA
        Color TEXT_REGULAR = new Color(0x606266);
        checker.assertContrast(TEXT_REGULAR, Color.WHITE, "TEXT_REGULAR on WHITE");

        // lerp 颜色插值正确
        assert checker.lerp(Color.WHITE, Color.BLACK, 0f).equals(Color.WHITE) : "lerp at 0 should be white";
        assert checker.lerp(Color.WHITE, Color.BLACK, 1f).equals(Color.BLACK) : "lerp at 1 should be black";
        Color mid = checker.lerp(Color.WHITE, Color.BLACK, 0.5f);
        assert mid.getRed() == 128 : "lerp at 0.5 red should be 128, got " + mid.getRed();
        assert mid.getGreen() == 128 : "lerp at 0.5 green should be 128";
        assert mid.getBlue() == 128 : "lerp at 0.5 blue should be 128";

        // assertApprox 正常工作
        checker.assertApprox(1.0f, 1.0f, 0.001f, "exact match");
        checker.assertApprox(1.0f, 1.0005f, 0.001f, "within epsilon");
        try {
            checker.assertApprox(1.0f, 2.0f, 0.001f, "should fail");
            assert false : "assertApprox should have thrown";
        } catch (AssertionError expected) {
            // ok
        }

        // assertDimension 正常工作
        checker.assertDimension(100, 100, "exact dimension");
        try {
            checker.assertDimension(100, 200, "should fail");
            assert false : "assertDimension should have thrown";
        } catch (AssertionError expected) {
            // ok
        }

        // 对比度断言失败测试
        try {
            checker.assertContrast(Color.WHITE, Color.WHITE, "same color");
            assert false : "assertContrast should have thrown for same color";
        } catch (AssertionError expected) {
            // ok
        }

        System.out.println("SelfCheckBase self-check OK");
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
