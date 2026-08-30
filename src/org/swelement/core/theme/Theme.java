package org.swelement.core.theme;

import java.awt.Color;
import java.awt.Font;

public interface Theme {
    String getName();

    // 语义色
    Color getPrimary();
    Color getSuccess();
    Color getWarning();
    Color getDanger();
    Color getInfo();

    // 文字色
    Color getTextPrimary();
    Color getTextRegular();
    Color getTextSecondary();
    Color getTextPlaceholder();
    Color getTextDisabled();

    // 边框色
    Color getBorderBase();
    Color getBorderLight();
    Color getBorderLighter();

    // 填充色
    Color getFillBlank();
    Color getFillBase();
    Color getFillLight();

    // 圆角
    int getRadiusSmall();
    int getRadiusBase();
    int getRadiusLarge();

    // 字体
    Font getFontSmall();
    Font getFontBase();
    Font getFontLarge();

    // 扩展方法
    Color getColor(String key);     // 未命中返回 null
    Font getFont(String key);      // 未命中返回 null
    int getSize(String key);       // 未命中返回 -1
}
