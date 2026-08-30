package org.swelement.framework;

import org.swelement.core.AnimationManager;
import org.swelement.core.theme.Theme;
import org.swelement.core.theme.ThemeManager;

import javax.swing.JComponent;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Swing Element UI 组件顶层基类。
 * <p>
 * 所有组件均继承自此类，提供以下基础能力：
 * <ul>
 *   <li>动画管理器（AnimationManager）的持有与生命周期管理</li>
 *   <li>主题系统的自动接入与变更监听</li>
 *   <li>抗锯齿 Graphics2D 创建</li>
 *   <li>颜色插值、圆角矩形绘制、居中文本绘制等通用绘制辅助</li>
 *   <li>组件销毁时的资源清理（主题监听、动画资源）</li>
 * </ul>
 * <p>
 * 子类必须实现 {@link #selfCheck()} 自检方法。
 */
public abstract class AstAbstractComponent extends JComponent
        implements ThemeManager.ThemeChangeListener {

    /** 动画管理器，管理本组件的所有命名动画 */
    protected final AnimationManager anim;

    /**
     * 构造方法：初始化动画管理器、设置不透明、注册主题监听、调用初始化钩子。
     */
    protected AstAbstractComponent() {
        this.anim = new AnimationManager(this);
        setOpaque(false);
        ThemeManager.ensureDefaultTheme();
        ThemeManager.addThemeChangeListener(this);
        initComponent();
    }

    // ==================== 主题 ====================

    /**
     * 获取当前主题。
     *
     * @return 当前主题实例
     */
    protected Theme theme() {
        return ThemeManager.getCurrent();
    }

    /**
     * 主题变更钩子，子类可重写以响应主题变化。
     * 默认行为为重绘组件。
     *
     * @param oldTheme 旧主题
     * @param newTheme 新主题
     */
    protected void onThemeUpdated(Theme oldTheme, Theme newTheme) {
        repaint();
    }

    /**
     * 主题变更回调（ThemeChangeListener 接口实现）。
     * final 方法，禁止重写；子类应重写 {@link #onThemeUpdated(Theme, Theme)}。
     *
     * @param oldTheme 旧主题
     * @param newTheme 新主题
     */
    @Override
    public final void onThemeChanged(Theme oldTheme, Theme newTheme) {
        onThemeUpdated(oldTheme, newTheme);
    }

    // ==================== 初始化钩子 ====================

    /**
     * 组件初始化钩子，在构造函数末尾调用。
     * 子类可重写以执行初始化逻辑（如注册动画、安装监听器等）。
     */
    protected void initComponent() { }

    // ==================== 绘制辅助 ====================

    /**
     * 创建带抗锯齿设置的 Graphics2D 副本。
     * <p>
     * 设置以下 RenderingHint：
     * <ul>
     *   <li>KEY_ANTIALIASING → VALUE_ANTIALIAS_ON（几何抗锯齿）</li>
     *   <li>KEY_TEXT_ANTIALIASING → VALUE_TEXT_ANTIALIAS_ON（文字抗锯齿）</li>
     *   <li>KEY_STROKE_CONTROL → VALUE_STROKE_PURE（描边精度控制）</li>
     * </ul>
     * <p>
     * 调用者使用完毕后必须调用 dispose() 释放资源。
     *
     * @param g 原始 Graphics 对象
     * @return 配置好抗锯齿的 Graphics2D 副本
     */
    protected Graphics2D createGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /**
     * 颜色插值（RGBA 四通道线性插值）。
     * <p>
     * 对红、绿、蓝、alpha 四个通道分别进行线性插值，返回新的 Color 对象。
     *
     * @param a 起始颜色
     * @param b 结束颜色
     * @param t 插值因子 [0, 1]，0 返回 a，1 返回 b
     * @return 插值后的颜色
     */
    protected Color lerp(Color a, Color b, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * clamped + 0.5f);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * clamped + 0.5f);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * clamped + 0.5f);
        int alpha = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * clamped + 0.5f);
        return new Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, bl)),
                Math.max(0, Math.min(255, alpha))
        );
    }

    /**
     * 填充圆角矩形。
     * <p>
     * 使用 RoundRectangle2D.Float 绘制，arcWidth 和 arcHeight 均为 radius * 2
     * （Swing 圆角矩形参数为直径，传入 radius 语义更直观）。
     *
     * @param g2     Graphics2D 对象
     * @param x      左上角 x 坐标
     * @param y      左上角 y 坐标
     * @param w      宽度
     * @param h      高度
     * @param radius 圆角半径
     */
    protected void fillRoundRect(Graphics2D g2, int x, int y, int w, int h, int radius) {
        float arc = radius * 2f;
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
    }

    /**
     * 绘制圆角矩形边框。
     * <p>
     * 使用 RoundRectangle2D.Float 绘制，arcWidth 和 arcHeight 均为 radius * 2
     * （Swing 圆角矩形参数为直径，传入 radius 语义更直观）。
     *
     * @param g2     Graphics2D 对象
     * @param x      左上角 x 坐标
     * @param y      左上角 y 坐标
     * @param w      宽度
     * @param h      高度
     * @param radius 圆角半径
     */
    protected void drawRoundRect(Graphics2D g2, int x, int y, int w, int h, int radius) {
        float arc = radius * 2f;
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
    }

    /**
     * 水平居中文本绘制。
     * <p>
     * 以 (x, baselineY) 为参考点，在指定宽度内水平居中绘制文本。
     * 文本基线位于 baselineY。
     *
     * @param g2        Graphics2D 对象
     * @param text      要绘制的文本
     * @param x         区域左边界 x 坐标
     * @param width     区域宽度
     * @param baselineY 文本基线 y 坐标
     */
    protected void drawCenteredText(Graphics2D g2, String text, int x, int width, float baselineY) {
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        float textX = x + (width - textWidth) / 2f;
        g2.drawString(text, textX, baselineY);
    }

    /**
     * 获取主题基础圆角半径。
     *
     * @return 基础圆角半径（像素）
     */
    protected int radius() {
        return theme().getRadiusBase();
    }

    // ==================== 自检工具 ====================

    /**
     * 计算 WCAG 相对亮度。
     *
     * @param c 颜色
     * @return 相对亮度值 [0, 1]
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
     * 对比度断言（WCAG AA 4.5:1）。
     *
     * @param fg    前景色
     * @param bg    背景色
     * @param where 位置描述
     */
    protected void assertContrast(Color fg, Color bg, String where) {
        assertContrast(fg, bg, where, 4.5f);
    }

    /**
     * 指定最小比例的对比度断言。
     *
     * @param fg       前景色
     * @param bg       背景色
     * @param where    位置描述
     * @param minRatio 最小对比度比例
     */
    protected void assertContrast(Color fg, Color bg, String where, float minRatio) {
        float ratio = contrastRatio(fg, bg);
        assert ratio >= minRatio : "[CONTRAST FAIL " + where + "] ratio="
                + String.format("%.2f", ratio) + " (need >= " + String.format("%.2f", minRatio) + ")"
                + " fg=RGB(" + fg.getRed() + "," + fg.getGreen() + "," + fg.getBlue() + ")"
                + " bg=RGB(" + bg.getRed() + "," + bg.getGreen() + "," + bg.getBlue() + ")";
    }

    /**
     * 根据背景色自动选择文字颜色（白/黑）。
     *
     * @param bg 背景色
     * @return 最合适的文字色
     */
    protected Color pickTextColorForBg(Color bg) {
        float rW = contrastRatio(Color.WHITE, bg);
        float rB = contrastRatio(Color.BLACK, bg);
        if (rW >= 4.5f && rW >= rB) return Color.WHITE;
        if (rB >= 4.5f) return Color.BLACK;
        return rW >= rB ? Color.WHITE : Color.BLACK;
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

    // ==================== 生命周期 ====================

    /**
     * 组件移除通知：清理主题监听器和动画资源。
     */
    @Override
    public void removeNotify() {
        super.removeNotify();
        ThemeManager.removeThemeChangeListener(this);
        anim.dispose();
    }

    // ==================== 自检 ====================

    /**
     * 自检方法，每个具体组件必须实现。
     * <p>
     * 用于验证组件在各种状态下的正确性（如对比度、布局等）。
     * 仅在 -ea（开启断言）时生效。
     */
    protected abstract void selfCheck();
}
