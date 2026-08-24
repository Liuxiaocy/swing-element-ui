package org.swelement.core;

import java.awt.Color;
import java.awt.Font;

public final class ElementTheme {
    public static final Color PRIMARY = new Color(0x409EFF);
    public static final Color SUCCESS = new Color(0x67C23A);
    public static final Color WARNING = new Color(0xE6A23C);
    public static final Color DANGER  = new Color(0xF56C6C);
    public static final Color INFO    = new Color(0x909399);
    public static final Color TEXT_MAIN = new Color(0x303133);
    public static final Color TEXT_REGULAR = new Color(0x606266);
    public static final Color TEXT_PLACEHOLDER = new Color(0xC0C4CC);
    public static final Color BORDER_BASE = new Color(0xDCDFE6);
    public static final Color FILL_BLANK = new Color(0xFFFFFF);
    public static final Color FILL_BASE = new Color(0xF5F7FA);
    public static final int RADIUS = 4;
    public static final Font FONT = new Font("Microsoft YaHei", Font.PLAIN, 14);

    private ElementTheme() {}

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
        assertContrast(new Color(0x303133), Color.WHITE, "TEXT_MAIN on WHITE must pass");
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
        float rT = contrastRatio(luminance(TEXT_MAIN), lumBg);
        float rB = contrastRatio(luminance(Color.BLACK), lumBg);
        if (rW >= 4.5f && rW >= rT && rW >= rB) return Color.WHITE;
        if (rT >= 4.5f && rT >= rB) return TEXT_MAIN;
        if (rB >= 4.5f) return Color.BLACK;
        // None meets threshold → pick highest contrast
        if (rW >= rT && rW >= rB) return Color.WHITE;
        if (rT >= rB) return TEXT_MAIN;
        return Color.BLACK;
    }
}
