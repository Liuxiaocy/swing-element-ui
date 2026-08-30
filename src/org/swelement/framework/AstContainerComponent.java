package org.swelement.framework;

import org.swelement.core.theme.Theme;

import java.awt.*;

/**
 * 容器组件基类。
 * <p>
 * 继承自 {@link AstAbstractComponent}，提供标准容器外观绘制能力：
 * <ul>
 *   <li>可配置的圆角半径（默认跟随主题，可手动覆盖）</li>
 *   <li>标准容器绘制方法（背景填充 + 边框描边）</li>
 * </ul>
 * <p>
 * 适用于 Card、Panel、Dialog 等具有容器外观的组件。
 */
public abstract class AstContainerComponent extends AstAbstractComponent {

    /** 圆角半径 */
    private int radius;
    /** 是否已手动设置圆角（手动设置后不再跟随主题变化） */
    private boolean radiusSetManually = false;

    /**
     * 初始化容器组件：从主题读取默认圆角半径。
     */
    @Override
    protected void initComponent() {
        super.initComponent();
        this.radius = theme().getRadiusBase();
    }

    /**
     * 设置圆角半径。
     * <p>
     * 手动设置后，圆角将不再随主题变化而自动更新。
     *
     * @param radius 圆角半径（像素）
     */
    public void setRadius(int radius) {
        this.radius = radius;
        this.radiusSetManually = true;
        repaint();
    }

    /**
     * 获取当前圆角半径。
     *
     * @return 圆角半径（像素）
     */
    public int getRadius() {
        return radius;
    }

    /**
     * 主题变更处理：未手动设置圆角时，自动跟随主题更新。
     *
     * @param oldTheme 旧主题
     * @param newTheme 新主题
     */
    @Override
    protected void onThemeUpdated(Theme oldTheme, Theme newTheme) {
        super.onThemeUpdated(oldTheme, newTheme);
        if (!radiusSetManually) {
            this.radius = newTheme.getRadiusBase();
        }
    }

    /**
     * 绘制标准容器外观（背景填充 + 边框描边）。
     * <p>
     * 子类的 paintComponent 中可调用此方法绘制容器基础外观，
     * 再在其上绘制自定义内容。
     * <p>
     * 使用主题的 fillBlank 作为背景色，borderBase 作为边框色，
     * 1px 实线描边。
     *
     * @param g2 Graphics2D 对象
     */
    protected void paintContainer(Graphics2D g2) {
        Theme t = theme();
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        // 背景填充
        g2.setColor(t.getFillBlank());
        fillRoundRect(g2, 0, 0, w, h, radius);
        // 边框描边
        g2.setColor(t.getBorderBase());
        g2.setStroke(new BasicStroke(1f));
        drawRoundRect(g2, 0, 0, w, h, radius);
    }
}
