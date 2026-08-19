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
        System.out.println("ElementTheme self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
