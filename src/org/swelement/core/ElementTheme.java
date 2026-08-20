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
    private static float luminance(Color c) {
        return 0.2126f * srgb(c.getRed()) + 0.7152f * srgb(c.getGreen()) + 0.0722f * srgb(c.getBlue());
    }
    /** Fails (AssertionError) when fg vs bg contrast < 4.5:1 — enabled only with -ea.
     *  Use `where` string to identify offending component state. */
    public static void assertContrast(Color fg, Color bg, String where) {
        float l1 = luminance(fg), l2 = luminance(bg);
        float lighter = Math.max(l1, l2), darker = Math.min(l1, l2);
        float ratio = (lighter + 0.05f) / (darker + 0.05f);
        assert ratio >= 4.5f : "[CONTRAST FAIL " + where + "] ratio=" + String.format("%.2f", ratio)
                + " fg=RGB(" + fg.getRed() + "," + fg.getGreen() + "," + fg.getBlue() + ")"
                + " bg=RGB(" + bg.getRed() + "," + bg.getGreen() + "," + bg.getBlue() + ")";
    }
}
