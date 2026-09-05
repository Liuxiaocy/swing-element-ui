package org.swelement.core;

import org.swelement.core.theme.ThemeManager;

import java.awt.Color;
import java.awt.Font;

/**
 * 颜色与对比度工具类（含 WCAG 断言）。
 * <p>
 * <b>不</b>再承载静态颜色常量，所有外观色均应通过 {@link ThemeManager#getCurrent()} 取得，
 * 以支持运行时主题切换。本类仅保留：
 * <ul>
 *   <li>{@code primary/success/...} 一组便捷 getter，<b>委托给 ThemeManager</b>（不再持有固定值）</li>
 *   <li>{@link #lerp}/{@link}、{@link #luminance}/{@link}、{@link #assertContrast}、
 *       {@link #pickTextColorForBg} 等与具体主题无关的纯算法工具</li>
 * </ul>
 * <p>
 * 旧式 {@code ElementTheme.primary()} 等静态常量字段已移除。旧引用必须迁移为
 * {@code ElementTheme.primary()} 或更推荐的 {@code ThemeManager.getCurrent().getPrimary()}。
 *
 * @deprecated 直接使用 {@link ThemeManager#getCurrent()}；本类仅保留便捷 getter。
 */
public final class ElementTheme {

    private ElementTheme() {}

    // ==================== 委托给 ThemeManager 的便捷 getter ====================

    /** 主色（委托当前主题） */
    public static Color primary()   { return ThemeManager.getCurrent().getPrimary(); }
    /** 成功色 */
    public static Color success()   { return ThemeManager.getCurrent().getSuccess(); }
    /** 警告色 */
    public static Color warning()   { return ThemeManager.getCurrent().getWarning(); }
    /** 危险色 */
    public static Color danger()    { return ThemeManager.getCurrent().getDanger(); }
    /** 信息色 */
    public static Color info()      { return ThemeManager.getCurrent().getInfo(); }
    /** 主要文本色（Theme.getTextPrimary） */
    public static Color textPrimary() { return ThemeManager.getCurrent().getTextPrimary(); }
    /** 常规文本色 */
    public static Color textRegular() { return ThemeManager.getCurrent().getTextRegular(); }
    /** 占位文本色 */
    public static Color textPlaceholder() { return ThemeManager.getCurrent().getTextPlaceholder(); }
    /** 基础边框色 */
    public static Color borderBase() { return ThemeManager.getCurrent().getBorderBase(); }
    /** 空白填充色 */
    public static Color fillBlank() { return ThemeManager.getCurrent().getFillBlank(); }
    /** 基础填充色 */
    public static Color fillBase()  { return ThemeManager.getCurrent().getFillBase(); }
    /** 基础圆角半径 */
    public static int radius()      { return ThemeManager.getCurrent().getRadiusBase(); }
    /** 基础字号 */
    public static Font font()        { return ThemeManager.getCurrent().getFontBase(); }

    // ==================== 与主题无关的工具方法 ====================

    public static Color lerp(Color a, Color b, float t) {
        return new Color(
            lerp(a.getRed(), b.getRed(), t),
            lerp(a.getGreen(), b.getGreen(), t),
            lerp(a.getBlue(), b.getBlue(), t));
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    public static int lerp(int a, int b, float t) { return Math.round(lerp((float) a, (float) b, t)); }

    static void selfCheck() {
        assert lerp(Color.WHITE, Color.BLACK, 0f).equals(Color.WHITE);
        assert lerp(Color.WHITE, Color.BLACK, 1f).equals(Color.BLACK);
        assert lerp(10, 20, 0.5f) == 15;
        assert lerp(0.5f, 1f, 0.5f) == 0.75f;
        assert lerp(Color.WHITE, Color.BLACK, 0.5f).getRed() == 128;  // Math.round(127.5f)==128
        assert luminance(Color.BLACK) < 0.01f : "black luminance near 0";
        assert luminance(Color.WHITE) > 0.99f : "white luminance near 1";
        try {
            assertContrast(Color.WHITE, Color.WHITE, "bad");
            assert false : "should have thrown";
        } catch (AssertionError expected) { /* ok */ }
        assertContrast(textPrimary(), fillBlank(), "textPrimary on fillBlank must pass");
        // 行为级断言：便捷 getter 必须跟随 ThemeManager.setCurrent() 实时切换
        org.swelement.core.theme.Theme original =
                org.swelement.core.theme.ThemeManager.getCurrent();
        Color originalPrimary = primary();
        try {
            // 构造一个不同主色的主题，注册并切换
            org.swelement.core.theme.Theme brand = new org.swelement.core.theme.Theme() {
                public String getName() { return "probe-brand"; }
                public Color getPrimary() { return new Color(0xFF0000); }
                public Color getSuccess() { return original.getSuccess(); }
                public Color getWarning() { return original.getWarning(); }
                public Color getDanger()  { return original.getDanger(); }
                public Color getInfo()    { return original.getInfo(); }
                public Color getTextPrimary()    { return original.getTextPrimary(); }
                public Color getTextRegular()    { return original.getTextRegular(); }
                public Color getTextSecondary()  { return original.getTextSecondary(); }
                public Color getTextPlaceholder(){ return original.getTextPlaceholder(); }
                public Color getTextDisabled()   { return original.getTextDisabled(); }
                public Color getBorderBase()     { return original.getBorderBase(); }
                public Color getBorderLight()    { return original.getBorderLight(); }
                public Color getBorderLighter()  { return original.getBorderLighter(); }
                public Color getFillBlank()      { return original.getFillBlank(); }
                public Color getFillBase()       { return original.getFillBase(); }
                public Color getFillLight()      { return original.getFillLight(); }
                public int  getRadiusSmall()     { return original.getRadiusSmall(); }
                public int  getRadiusBase()      { return original.getRadiusBase(); }
                public int  getRadiusLarge()     { return original.getRadiusLarge(); }
                public Font getFontSmall()       { return original.getFontSmall(); }
                public Font getFontBase()        { return original.getFontBase(); }
                public Font getFontLarge()       { return original.getFontLarge(); }
                public Color getColor(String k)  { return null; }
                public Font  getFont(String k)   { return null; }
                public int   getSize(String k)   { return -1; }
            };
            org.swelement.core.theme.ThemeManager.registerTheme(brand);
            org.swelement.core.theme.ThemeManager.setCurrent("probe-brand");
            assert primary().equals(new Color(0xFF0000))
                    : "primary() 应跟随 ThemeManager.setCurrent() 切换，实际=" + primary();
            assert primary() != originalPrimary
                    : "primary() 未跟随主题切换，仍为旧值";
        } finally {
            // 恢复现场：切回原主题并反注册探测主题
            org.swelement.core.theme.ThemeManager.setCurrent(original.getName());
        }
        System.out.println("ElementTheme self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }

    // === P1 additions: WCAG contrast utilities ===
    private static float srgb(int v) {
        float vv = v / 255f;
        return vv <= 0.03928f ? vv / 12.92f : (float) Math.pow((vv + 0.055) / 1.055, 2.4);
    }
    /** Relative luminance per WCAG (approx, range [0,1]) */
    public static float luminance(Color c) {
        return 0.2126f * srgb(c.getRed()) + 0.7152f * srgb(c.getGreen()) + 0.0722f * srgb(c.getBlue());
    }
    /** Fails (AssertionError) when fg vs bg contrast < 4.5:1 (body-text AA) — enabled only with -ea.
     *  Use `where` string to identify offending component state. */
    public static void assertContrast(Color fg, Color bg, String where) {
        assertContrast(fg, bg, where, 4.5f);
    }

    /** Like {@link #assertContrast(Color,Color,String)} but with a caller-supplied minimum ratio.
     *  Use 3.0f for non-text UI elements / graphical markers (WCAG 1.4.11 "non-text contrast"),
     *  e.g. a required-field asterisk or an accent border whose brand color cannot reach 4.5:1. */
    public static void assertContrast(Color fg, Color bg, String where, float minRatio) {
        float l1 = luminance(fg), l2 = luminance(bg);
        float lighter = Math.max(l1, l2), darker = Math.min(l1, l2);
        float ratio = (lighter + 0.05f) / (darker + 0.05f);
        assert ratio >= minRatio : "[CONTRAST FAIL " + where + "] ratio=" + String.format("%.2f", ratio)
                + " (need >= " + String.format("%.2f", minRatio) + ")"
                + " fg=RGB(" + fg.getRed() + "," + fg.getGreen() + "," + fg.getBlue() + ")"
                + " bg=RGB(" + bg.getRed() + "," + bg.getGreen() + "," + bg.getBlue() + ")";
    }

    private static float contrastRatio(float lum1, float lum2) {
        float lighter = Math.max(lum1, lum2), darker = Math.min(lum1, lum2);
        return (lighter + 0.05f) / (darker + 0.05f);
    }

    /** Returns WHITE/TEXT_MAIN/BLACK — first that meets WCAG 4.5:1 on bg; tie-break prefers highest contrast. */
    public static Color pickTextColorForBg(Color bg) {
        float lumBg = luminance(bg);
        float rW = contrastRatio(luminance(Color.WHITE), lumBg);
        float rT = contrastRatio(luminance(textPrimary()), lumBg);
        float rB = contrastRatio(luminance(Color.BLACK), lumBg);
        if (rW >= 4.5f && rW >= rT && rW >= rB) return Color.WHITE;
        if (rT >= 4.5f && rT >= rB) return textPrimary();
        if (rB >= 4.5f) return Color.BLACK;
        // None meets threshold → pick highest contrast
        if (rW >= rT && rW >= rB) return Color.WHITE;
        if (rT >= rB) return textPrimary();
        return Color.BLACK;
    }
}
