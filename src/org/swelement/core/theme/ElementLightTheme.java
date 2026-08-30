package org.swelement.core.theme;

import java.awt.Color;
import java.awt.Font;

public class ElementLightTheme implements Theme {
    // ==================== Constants ====================
    // Semantic colors
    private static final Color PRIMARY = new Color(0x409EFF);
    private static final Color SUCCESS = new Color(0x67C23A);
    private static final Color WARNING = new Color(0xE6A23C);
    private static final Color DANGER = new Color(0xF56C6C);
    private static final Color INFO = new Color(0x909399);

    // 文字色
    private static final Color TEXT_PRIMARY = new Color(0x303133);
    private static final Color TEXT_REGULAR = new Color(0x606266);
    private static final Color TEXT_SECONDARY = new Color(0x909399);
    private static final Color TEXT_PLACEHOLDER = new Color(0xC0C4CC);
    private static final Color TEXT_DISABLED = new Color(0xC0C4CC);

    // 边框色
    private static final Color BORDER_BASE = new Color(0xDCDFE6);
    private static final Color BORDER_LIGHT = new Color(0xE4E7ED);
    private static final Color BORDER_LIGHTER = new Color(0xEBEEF5);

    // 填充色
    private static final Color FILL_BLANK = new Color(0xFFFFFF);
    private static final Color FILL_BASE = new Color(0xF5F7FA);
    private static final Color FILL_LIGHT = new Color(0xFAFAFA);

    // 圆角
    private static final int RADIUS_SMALL = 2;
    private static final int RADIUS_BASE = 4;
    private static final int RADIUS_LARGE = 8;

    // 字体
    private static final Font FONT_SMALL = new Font("Microsoft YaHei", Font.PLAIN, 12);
    private static final Font FONT_BASE = new Font("Microsoft YaHei", Font.PLAIN, 14);
    private static final Font FONT_LARGE = new Font("Microsoft YaHei", Font.PLAIN, 16);

    public String getName() {
        return "element-light";
    }

    // 语义色
    public Color getPrimary() { return PRIMARY; }
    public Color getSuccess() { return SUCCESS; }
    public Color getWarning() { return WARNING; }
    public Color getDanger() { return DANGER; }
    public Color getInfo() { return INFO; }

    // 文字色
    public Color getTextPrimary() { return TEXT_PRIMARY; }
    public Color getTextRegular() { return TEXT_REGULAR; }
    public Color getTextSecondary() { return TEXT_SECONDARY; }
    public Color getTextPlaceholder() { return TEXT_PLACEHOLDER; }
    public Color getTextDisabled() { return TEXT_DISABLED; }

    // 边框色
    public Color getBorderBase() { return BORDER_BASE; }
    public Color getBorderLight() { return BORDER_LIGHT; }
    public Color getBorderLighter() { return BORDER_LIGHTER; }

    // 填充色
    public Color getFillBlank() { return FILL_BLANK; }
    public Color getFillBase() { return FILL_BASE; }
    public Color getFillLight() { return FILL_LIGHT; }

    // 圆角
    public int getRadiusSmall() { return RADIUS_SMALL; }
    public int getRadiusBase() { return RADIUS_BASE; }
    public int getRadiusLarge() { return RADIUS_LARGE; }

    // 字体
    public Font getFontSmall() { return FONT_SMALL; }
    public Font getFontBase() { return FONT_BASE; }
    public Font getFontLarge() { return FONT_LARGE; }

    // 扩展方法 — 未命中返回 null / -1
    public Color getColor(String key) {
        if (key == null) return null;
        if ("primary".equals(key)) return PRIMARY;
        if ("success".equals(key)) return SUCCESS;
        if ("warning".equals(key)) return WARNING;
        if ("danger".equals(key)) return DANGER;
        if ("info".equals(key)) return INFO;
        if ("textPrimary".equals(key)) return TEXT_PRIMARY;
        if ("textRegular".equals(key)) return TEXT_REGULAR;
        if ("textSecondary".equals(key)) return TEXT_SECONDARY;
        if ("textPlaceholder".equals(key)) return TEXT_PLACEHOLDER;
        if ("textDisabled".equals(key)) return TEXT_DISABLED;
        if ("borderBase".equals(key)) return BORDER_BASE;
        if ("borderLight".equals(key)) return BORDER_LIGHT;
        if ("borderLighter".equals(key)) return BORDER_LIGHTER;
        if ("fillBlank".equals(key)) return FILL_BLANK;
        if ("fillBase".equals(key)) return FILL_BASE;
        if ("fillLight".equals(key)) return FILL_LIGHT;
        return null;
    }

    public Font getFont(String key) {
        if (key == null) return null;
        if ("small".equals(key)) return FONT_SMALL;
        if ("base".equals(key)) return FONT_BASE;
        if ("large".equals(key)) return FONT_LARGE;
        return null;
    }

    public int getSize(String key) {
        if (key == null) return -1;
        if ("radiusSmall".equals(key)) return RADIUS_SMALL;
        if ("radiusBase".equals(key)) return RADIUS_BASE;
        if ("radiusLarge".equals(key)) return RADIUS_LARGE;
        return -1;
    }

    // ==================== 自检 ====================

    static void selfCheck() {
        ElementLightTheme theme = new ElementLightTheme();

        // 主题名称
        assert "element-light".equals(theme.getName()) : "theme name should be element-light";

        // 关键颜色 RGB 值 — 与 ElementTheme 常量对比
        assert theme.getPrimary().getRGB() == new Color(0x409EFF).getRGB() : "PRIMARY mismatch";
        assert theme.getSuccess().getRGB() == new Color(0x67C23A).getRGB() : "SUCCESS mismatch";
        assert theme.getWarning().getRGB() == new Color(0xE6A23C).getRGB() : "WARNING mismatch";
        assert theme.getDanger().getRGB() == new Color(0xF56C6C).getRGB() : "DANGER mismatch";
        assert theme.getInfo().getRGB() == new Color(0x909399).getRGB() : "INFO mismatch";

        assert theme.getTextPrimary().getRGB() == new Color(0x303133).getRGB() : "TEXT_PRIMARY mismatch";
        assert theme.getTextRegular().getRGB() == new Color(0x606266).getRGB() : "TEXT_REGULAR mismatch";
        assert theme.getTextSecondary().getRGB() == new Color(0x909399).getRGB() : "TEXT_SECONDARY mismatch";
        assert theme.getTextPlaceholder().getRGB() == new Color(0xC0C4CC).getRGB() : "TEXT_PLACEHOLDER mismatch";
        assert theme.getTextDisabled().getRGB() == new Color(0xC0C4CC).getRGB() : "TEXT_DISABLED mismatch";

        assert theme.getBorderBase().getRGB() == new Color(0xDCDFE6).getRGB() : "BORDER_BASE mismatch";
        assert theme.getBorderLight().getRGB() == new Color(0xE4E7ED).getRGB() : "BORDER_LIGHT mismatch";
        assert theme.getBorderLighter().getRGB() == new Color(0xEBEEF5).getRGB() : "BORDER_LIGHTER mismatch";

        assert theme.getFillBlank().getRGB() == Color.WHITE.getRGB() : "FILL_BLANK should be white";
        assert theme.getFillBase().getRGB() == new Color(0xF5F7FA).getRGB() : "FILL_BASE mismatch";
        assert theme.getFillLight().getRGB() == new Color(0xFAFAFA).getRGB() : "FILL_LIGHT mismatch";

        // 圆角尺寸
        assert theme.getRadiusSmall() == 2 : "radiusSmall should be 2";
        assert theme.getRadiusBase() == 4 : "radiusBase should be 4";
        assert theme.getRadiusLarge() == 8 : "radiusLarge should be 8";

        // 字体
        assert "Microsoft YaHei".equals(theme.getFontSmall().getName()) : "fontSmall name mismatch";
        assert theme.getFontSmall().getSize() == 12 : "fontSmall size should be 12";
        assert theme.getFontSmall().getStyle() == Font.PLAIN : "fontSmall style should be PLAIN";

        assert "Microsoft YaHei".equals(theme.getFontBase().getName()) : "fontBase name mismatch";
        assert theme.getFontBase().getSize() == 14 : "fontBase size should be 14";
        assert theme.getFontBase().getStyle() == Font.PLAIN : "fontBase style should be PLAIN";

        assert "Microsoft YaHei".equals(theme.getFontLarge().getName()) : "fontLarge name mismatch";
        assert theme.getFontLarge().getSize() == 16 : "fontLarge size should be 16";
        assert theme.getFontLarge().getStyle() == Font.PLAIN : "fontLarge style should be PLAIN";

        // 扩展方法 — 命中
        assert theme.getColor("primary") != null : "getColor(primary) should hit";
        assert theme.getColor("primary").equals(theme.getPrimary()) : "getColor(primary) should equal getPrimary";
        assert theme.getFont("base") != null : "getFont(base) should hit";
        assert theme.getFont("base").equals(theme.getFontBase()) : "getFont(base) should equal getFontBase";
        assert theme.getSize("radiusBase") == 4 : "getSize(radiusBase) should be 4";

        // 扩展方法 — 未命中返回 null / -1
        assert theme.getColor("nonexistent") == null : "getColor(nonexistent) should return null";
        assert theme.getColor(null) == null : "getColor(null) should return null";
        assert theme.getFont("nonexistent") == null : "getFont(nonexistent) should return null";
        assert theme.getFont(null) == null : "getFont(null) should return null";
        assert theme.getSize("nonexistent") == -1 : "getSize(nonexistent) should return -1";
        assert theme.getSize(null) == -1 : "getSize(null) should return -1";

        System.out.println("ElementLightTheme self-check OK");
    }

    public static void main(String[] args) {
        selfCheck();
    }
}
