package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public class AstCheckbox extends AstInteractiveComponent {
    private final String text;
    /** 按钮样式开关：true 时渲染为直角分段按钮（选中=主色填充+白字），false 为默认勾选框。 */
    private boolean buttonStyle = false;

    public AstCheckbox(String text) {
        super();
        this.text = text;
    }

    public String getText() { return text; }

    /**
     * 设置按钮样式（el-checkbox-button 风格）：直角分段按钮，选中=主色填充+白字，
     * 未选中=浅底+常规文字，相邻并排时形成 segmented 外观。按钮态不绘制勾选框/对勾。
     *
     * @param on true 启用按钮样式，false 恢复默认勾选框
     */
    public void setButtonStyle(boolean on) {
        if (this.buttonStyle == on) return;
        this.buttonStyle = on;
        revalidate();
        repaint();
    }

    /** @return 是否处于按钮样式。 */
    public boolean isButtonStyle() { return buttonStyle; }

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("fill", 200, Easing::easeInOut);
        anim.register("check", 200, Easing::easeOut);
    }

    @Override
    protected void onSelectedChanged(boolean selected) {
        anim.go("fill", anim.getProgress("fill"), selected ? 1f : 0f);
        anim.go("check", anim.getProgress("check"), selected ? 1f : 0f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (buttonStyle) { paintButtonStyle(g); return; }
        Graphics2D g2 = createGraphics(g);
        g2.setFont(theme().getFontBase());
        int y = (getHeight() - 16) / 2;
        float fill = anim.getProgress("fill");
        float check = anim.getProgress("check");
        float hover = hoverProgress();
        Color border = isEnabled()
            ? lerp(theme().getBorderBase(), theme().getPrimary(), Math.max(fill, hover))
            : new Color(0xC0C4CC);
        Color bg = lerp(theme().getFillBlank(), theme().getPrimary(), fill);
        if (!isEnabled()) bg = lerp(theme().getFillBlank(), new Color(0xC0C4CC), fill);

        Shape box = new RoundRectangle2D.Float(0, y, 16, 16, 4, 4);
        g2.setColor(bg);
        g2.fill(box);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(box);

        if (check > 0) {
            Shape old = g2.getClip();
            g2.clip(new Rectangle2D.Float(0, y - 2, 12 * check + 1, 20));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            p.moveTo(4, y + 9);
            p.lineTo(7, y + 12);
            p.lineTo(12, y + 5);
            g2.draw(p);
            g2.setClip(old);
        }

        g2.setColor(isEnabled() ? theme().getTextRegular() : new Color(0xC0C4CC));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, 24, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g2.dispose();
    }

    /**
     * 按钮样式绘制：直角矩形，1px 边框；选中=主色填充+白字，未选中=浅底+常规文字；
     * 禁用=静音灰（WCAG 豁免）。背景随 fill 动画在浅底↔主色间平滑过渡（复用现有 fill 通道）。
     * 不绘制勾选框/对勾——整个按钮即为选中指示器。
     */
    private void paintButtonStyle(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        int w = getWidth(), h = getHeight();
        float fill = anim.getProgress("fill");
        float hover = hoverProgress();
        boolean enabled = isEnabled();
        Color primary = theme().getPrimary();
        Color borderBase = theme().getBorderBase();
        Color fillBlank = theme().getFillBlank();
        Color muted = new Color(0xC0C4CC);

        Color bg, border, fg;
        if (!enabled) {
            bg = fillBlank; border = muted; fg = muted;
        } else {
            // 选中态白字彩底（实心态，按惯例不做 AA 断言）；fill 进度驱动背景与文字色平滑过渡
            bg = lerp(fillBlank, primary, fill);
            fg = lerp(theme().getTextRegular(), Color.WHITE, fill);
            border = lerp(borderBase, primary, Math.max(fill, hover));
        }

        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);
        g2.setColor(border);
        // 直角 1px 边框（四边各 1px 实色条，保证清晰锐利）
        g2.fillRect(0, 0, w, 1);
        g2.fillRect(0, h - 1, w, 1);
        g2.fillRect(0, 0, 1, h);
        g2.fillRect(w - 1, 0, 1, h);

        g2.setFont(theme().getFontBase());
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(text)) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(fg);
        g2.drawString(text, tx, ty);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(theme().getFontBase());
        if (buttonStyle) {
            // 按钮态：height=32，width=文字宽+40（左右各 20 padding，对齐 Element 默认）
            int w = fm.stringWidth(text) + 40;
            return new Dimension(w, 32);
        }
        int w = 16 + 8 + fm.stringWidth(text) + 8;
        int h = 28;
        return new Dimension(w, h);
    }

    @Override
    protected void selfCheck() {
        AstCheckbox c = new AstCheckbox("选项");
        assert c.getPreferredSize().height == 28 : "default height, got " + c.getPreferredSize().height;
        assert !c.isSelected() : "default not selected";
        c.setSelected(true);
        assert c.isSelected() : "setSelected true";
        c.setSelected(false);
        assert !c.isSelected() : "setSelected false";
        assertContrast(theme().getTextRegular(), theme().getFillBlank(), "checkbox text on fill");
        assertContrast(Color.WHITE, theme().getPrimary(), "checkbox check on fill (graphic)", 2.5f);

        // ===== P4.2：按钮样式（setButtonStyle）=====
        AstCheckbox bc = new AstCheckbox("选项");
        assert !bc.isButtonStyle() : "default not button style";
        bc.setButtonStyle(true);
        assert bc.isButtonStyle() : "setButtonStyle true";
        bc.setButtonStyle(false);
        assert !bc.isButtonStyle() : "setButtonStyle false";
        bc.setButtonStyle(true);

        // 按钮态尺寸：height=32，width=文字宽+40
        FontMetrics fm = bc.getFontMetrics(theme().getFontBase());
        int expW = fm.stringWidth("选项") + 40;
        assert bc.getPreferredSize().height == 32 : "button height, got " + bc.getPreferredSize().height;
        assert bc.getPreferredSize().width == expW : "button width, got " + bc.getPreferredSize().width;

        // 按钮态未选中：文字仍对浅底 AA（选中白字彩底按惯例豁免）
        assertContrast(theme().getTextRegular(), theme().getFillBlank(), "checkbox button text on fill");

        // 绘制级断言：选中态强制 fill=1，离屏绘制后背景中心像素应为 PRIMARY 不透明
        bc.setSelected(true);
        bc.anim.setProgress("fill", 1f);
        int bw = bc.getPreferredSize().width, bh = 32;
        bc.setSize(bw, bh);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                bw, bh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { bc.paint(gg); } finally { gg.dispose(); }
        int px = img.getRGB(8, bh / 2);   // 远离 0.5px 边框与居中文字，取纯背景点
        int alpha = (px >>> 24) & 0xFF;
        int primRgb = theme().getPrimary().getRGB() & 0xFFFFFF;
        assert alpha == 255 : "checkbox button selected bg opaque, alpha=" + alpha;
        assert (px & 0xFFFFFF) == primRgb
                : "checkbox button selected bg must be PRIMARY, got " + Integer.toHexString(px & 0xFFFFFF);

        // 离屏绘制未选中态不抛错
        bc.setSelected(false);
        bc.anim.setProgress("fill", 0f);
        Graphics2D gx = img.createGraphics();
        try { bc.paint(gx); } finally { gx.dispose(); }

        // 清理动画定时器，避免自检 JVM 因非 daemon 的 AWT/EDT 不退出而挂死
        bc.anim.stopAll();
        c.anim.stopAll();

        assertKeyboardAccessible(this, "AstCheckbox");
        anim.stopAll();
        System.out.println("AstCheckbox self-check OK");
    }

    public static void main(String[] args) {
        new AstCheckbox("test").selfCheck();
    }
}
