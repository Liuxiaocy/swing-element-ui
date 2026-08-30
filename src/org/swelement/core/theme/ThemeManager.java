package org.swelement.core.theme;

import java.util.*;
import java.awt.Color;
import java.awt.Font;

public final class ThemeManager {
    private static Map<String, Theme> themes = new LinkedHashMap<String, Theme>();
    private static List<ThemeChangeListener> listeners = new ArrayList<ThemeChangeListener>();
    private static Theme current;

    private ThemeManager() {}

    /** Register a theme. The first registered theme becomes the default current theme. */
    public static void registerTheme(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        themes.put(theme.getName(), theme);
        if (current == null) {
            current = theme;
        }
    }

    /** Get the current theme. @throws IllegalStateException if no theme registered */
    public static Theme getCurrent() {
        if (current == null) {
            throw new IllegalStateException("no theme registered");
        }
        return current;
    }

    /** Switch to a different theme by name. Notifies all listeners. @throws IllegalArgumentException if not found */
    public static void setCurrent(String themeName) {
        if (themeName == null) {
            throw new IllegalArgumentException("themeName must not be null");
        }
        Theme target = themes.get(themeName);
        if (target == null) {
            throw new IllegalArgumentException("theme not found: " + themeName);
        }
        if (current != null && current.getName().equals(themeName)) {
            return; // 同名不触发通知
        }
        Theme old = current;
        current = target;
        fireThemeChanged(old, target);
    }

    /** Get list of all registered theme names (in registration order). */
    public static List<String> getAvailableThemes() {
        return new ArrayList<String>(themes.keySet());
    }

    /** Add a theme change listener. Safe to call with null (ignored). */
    public static void addThemeChangeListener(ThemeChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /** Remove a theme change listener. Safe to call with null (ignored). */
    public static void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    /** Ensure default theme is registered. Registers ElementLightTheme if no themes exist yet. */
    public static void ensureDefaultTheme() {
        if (themes.isEmpty()) {
            registerTheme(new ElementLightTheme());
        }
    }

    private static void fireThemeChanged(Theme oldTheme, Theme newTheme) {
        // 遍历监听器副本，单个监听器异常不影响其他
        List<ThemeChangeListener> snapshot = new ArrayList<ThemeChangeListener>(listeners);
        for (ThemeChangeListener l : snapshot) {
            try {
                l.onThemeChanged(oldTheme, newTheme);
            } catch (RuntimeException e) {
                // 单个监听器异常不影响其他
            }
        }
    }

    /** Listener interface for theme change events. */
    public interface ThemeChangeListener {
        void onThemeChanged(Theme oldTheme, Theme newTheme);
    }

    // ==================== 自检 ====================

    static void selfCheck() {
        // 保存现场
        Map<String, Theme> savedThemes = new LinkedHashMap<String, Theme>(themes);
        Theme savedCurrent = current;
        List<ThemeChangeListener> savedListeners = new ArrayList<ThemeChangeListener>(listeners);

        try {
            // 重置状态用于测试
            themes.clear();
            current = null;
            listeners.clear();

            // 测试1: null 主题抛 IllegalArgumentException
            try {
                registerTheme(null);
                assert false : "registerTheme(null) should throw";
            } catch (IllegalArgumentException expected) { /* ok */ }

            // 测试2: 注册第一个主题成为 current
            Theme themeA = new MockTheme("theme-a");
            registerTheme(themeA);
            assert getCurrent() == themeA : "first registered should be current";

            // 测试3: 注册第二个不改变 current
            Theme themeB = new MockTheme("theme-b");
            registerTheme(themeB);
            assert getCurrent() == themeA : "second register should not change current";

            // 测试4: getAvailableThemes 返回正确顺序
            List<String> available = getAvailableThemes();
            assert available.size() == 2 : "should have 2 themes";
            assert "theme-a".equals(available.get(0)) : "first should be theme-a";
            assert "theme-b".equals(available.get(1)) : "second should be theme-b";

            // 测试5: setCurrent 切换并触发监听器
            final Theme[] oldRef = new Theme[1];
            final Theme[] newRef = new Theme[1];
            final int[] count = new int[1];
            ThemeChangeListener listener = new ThemeChangeListener() {
                public void onThemeChanged(Theme oldTheme, Theme newTheme) {
                    oldRef[0] = oldTheme;
                    newRef[0] = newTheme;
                    count[0]++;
                }
            };
            addThemeChangeListener(listener);

            setCurrent("theme-b");
            assert getCurrent() == themeB : "current should be theme-b";
            assert count[0] == 1 : "listener should be called once";
            assert oldRef[0] == themeA : "old theme should be theme-a";
            assert newRef[0] == themeB : "new theme should be theme-b";

            // 测试6: 切换到相同主题不触发通知
            count[0] = 0;
            setCurrent("theme-b");
            assert count[0] == 0 : "same theme should not trigger listener";

            // 测试7: 移除监听器后不再触发
            removeThemeChangeListener(listener);
            count[0] = 0;
            setCurrent("theme-a");
            assert count[0] == 0 : "removed listener should not be called";

            // 测试8: 未知主题抛 IllegalArgumentException
            try {
                setCurrent("nonexistent");
                assert false : "setCurrent(nonexistent) should throw";
            } catch (IllegalArgumentException expected) { /* ok */ }

            // 测试9: null 主题名抛 IllegalArgumentException
            try {
                setCurrent(null);
                assert false : "setCurrent(null) should throw";
            } catch (IllegalArgumentException expected) { /* ok */ }

            // 测试10: 监听器异常不影响其他监听器
            ThemeChangeListener badListener = new ThemeChangeListener() {
                public void onThemeChanged(Theme oldTheme, Theme newTheme) {
                    throw new RuntimeException("bad listener");
                }
            };
            ThemeChangeListener goodListener = new ThemeChangeListener() {
                public void onThemeChanged(Theme oldTheme, Theme newTheme) {
                    count[0]++;
                }
            };
            count[0] = 0;
            addThemeChangeListener(badListener);
            addThemeChangeListener(goodListener);
            setCurrent("theme-b");
            assert count[0] == 1 : "good listener should still be called after bad one throws";
            removeThemeChangeListener(badListener);
            removeThemeChangeListener(goodListener);

            // 测试11: ensureDefaultTheme 注册默认主题
            themes.clear();
            current = null;
            ensureDefaultTheme();
            assert getCurrent() != null : "ensureDefaultTheme should register a theme";
            assert "element-light".equals(getCurrent().getName()) : "default theme name should be element-light";
            // 再次调用不重复注册
            int sizeBefore = themes.size();
            ensureDefaultTheme();
            assert themes.size() == sizeBefore : "ensureDefaultTheme should not register again";

            System.out.println("ThemeManager self-check OK");
        } finally {
            // 恢复现场
            themes.clear();
            themes.putAll(savedThemes);
            current = savedCurrent;
            listeners.clear();
            listeners.addAll(savedListeners);
        }
    }

    public static void main(String[] args) {
        selfCheck();
    }

    // 自检用的 Mock 主题
    private static class MockTheme implements Theme {
        private final String name;

        MockTheme(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public Color getPrimary() { return Color.BLUE; }
        public Color getSuccess() { return Color.GREEN; }
        public Color getWarning() { return Color.YELLOW; }
        public Color getDanger() { return Color.RED; }
        public Color getInfo() { return Color.GRAY; }
        public Color getTextPrimary() { return Color.BLACK; }
        public Color getTextRegular() { return Color.DARK_GRAY; }
        public Color getTextSecondary() { return Color.GRAY; }
        public Color getTextPlaceholder() { return Color.LIGHT_GRAY; }
        public Color getTextDisabled() { return Color.LIGHT_GRAY; }
        public Color getBorderBase() { return Color.GRAY; }
        public Color getBorderLight() { return Color.LIGHT_GRAY; }
        public Color getBorderLighter() { return Color.WHITE; }
        public Color getFillBlank() { return Color.WHITE; }
        public Color getFillBase() { return Color.LIGHT_GRAY; }
        public Color getFillLight() { return Color.WHITE; }
        public int getRadiusSmall() { return 2; }
        public int getRadiusBase() { return 4; }
        public int getRadiusLarge() { return 8; }
        public Font getFontSmall() { return new Font("Dialog", Font.PLAIN, 12); }
        public Font getFontBase() { return new Font("Dialog", Font.PLAIN, 14); }
        public Font getFontLarge() { return new Font("Dialog", Font.PLAIN, 16); }
        public Color getColor(String key) { return null; }
        public Font getFont(String key) { return null; }
        public int getSize(String key) { return -1; }
    }
}
