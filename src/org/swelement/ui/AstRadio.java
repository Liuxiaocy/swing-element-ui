package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.framework.AstInteractiveComponent;

import java.awt.*;

public class AstRadio extends AstInteractiveComponent {
    private final String text;
    /** 按钮样式开关：true 时渲染为直角分段按钮（选中=主色填充+白字），false 为默认圆形单选。 */
    private boolean buttonStyle = false;

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("dot", 200, Easing::easeOut);
        anim.register("border", 200, Easing::easeInOut);
        // 按钮态背景填充动画（未选中=浅底，选中=主色），与圆形态 dot/border 共存互不干扰
        anim.register("fill", 200, Easing::easeInOut);
    }

    @Override
    protected void onSelectedChanged(boolean selected) {
        super.onSelectedChanged(selected);
        anim.go("border", anim.getProgress("border"), selected ? 1f : 0f);
        anim.go("dot", anim.getProgress("dot"), selected ? 1f : 0f);
        anim.go("fill", anim.getProgress("fill"), selected ? 1f : 0f);
        if (selectionChangeHook != null) selectionChangeHook.run();
    }

    /**
     * 设置按钮样式（el-radio-button 风格）：直角分段按钮，选中=主色填充+白字，
     * 未选中=浅底+常规文字，相邻并排时形成 segmented 外观。
     *
     * @param on true 启用按钮样式，false 恢复默认圆形单选
     */
    public void setButtonStyle(boolean on) {
        if (this.buttonStyle == on) return;
        this.buttonStyle = on;
        revalidate();
        repaint();
    }

    /** @return 是否处于按钮样式。 */
    public boolean isButtonStyle() { return buttonStyle; }

    public AstRadio(String text) {
        this.text = text;
    }

    public String getText() { return text; }

    /**
     * 单选按钮分组：组内同一时间只有一个被选中。
     */
    public static class Group {
        private final java.util.List<AstRadio> radios = new java.util.ArrayList<AstRadio>();
        private AstRadio selected;

        public void add(final AstRadio radio) {
            if (radio == null) return;
            radios.add(radio);
            if (radio.isSelected()) {
                if (selected != null) selected.setSelected(false);
                selected = radio;
            }
            // 监听选中变化：通过包装 onSelectedChanged 实现
            final Group group = this;
            final Runnable prevHook = radio.selectionChangeHook;
            radio.selectionChangeHook = new Runnable() {
                public void run() {
                    if (prevHook != null) prevHook.run();
                    group.onRadioSelected(radio);
                }
            };
        }

        void onRadioSelected(AstRadio radio) {
            if (radio.isSelected() && selected != radio) {
                AstRadio old = selected;
                selected = radio;
                if (old != null) old.setSelected(false);
            }
        }

        public AstRadio getSelected() { return selected; }

        public void clearSelection() {
            if (selected != null) { selected.setSelected(false); selected = null; }
        }
    }

    /** 内部使用：选中变化钩子，供 Group 等外部机制监听。 */
    Runnable selectionChangeHook;

    @Override
    protected void paintComponent(Graphics g) {
        if (buttonStyle) { paintButtonStyle(g); return; }
        Graphics2D g2 = createGraphics(g);
        float dot = anim.getProgress("dot");
        float border = anim.getProgress("border");
        float hover = hoverProgress();
        int cy = getHeight() / 2;
        int r = 8;
        int cx = r + 2;
        Color borderColor = lerp(theme().getBorderBase(), theme().getPrimary(), Math.max(border, hover));
        g2.setColor(theme().getFillBlank());
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
        if (dot > 0.01f) {
            int innerR = (int) (4 * Math.sqrt(dot));
            g2.setColor(theme().getPrimary());
            g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
        }
        g2.setColor(theme().getTextRegular());
        g2.setFont(theme().getFontBase());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx + r + 8, cy + fm.getAscent() / 2 - fm.getDescent());
        g2.dispose();
    }

    /**
     * 按钮样式绘制：直角矩形，1px 边框；选中=主色填充+白字，未选中=浅底+常规文字；
     * 禁用=静音灰（WCAG 豁免）。背景随 fill 动画在浅底↔主色间平滑过渡。
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
        int w = 20 + 8 + fm.stringWidth(text) + 8;
        int h = 28;
        return new Dimension(w, h);
    }

    @Override
    protected void selfCheck() {
        AstRadio r = new AstRadio("选项");
        assert r.getPreferredSize().height == 28 : "default height, got " + r.getPreferredSize().height;
        assert !r.isSelected() : "default not selected";
        r.setSelected(true);
        assert r.isSelected() : "setSelected true";
        r.setSelected(false);
        assert !r.isSelected() : "setSelected false";
        // 对比度：文字 vs 背景（填充色）
        assertContrast(theme().getTextRegular(), theme().getFillBlank(), "radio text on fill");
        assertKeyboardAccessible(this, "AstRadio");

        // ===== P4.1：按钮样式（setButtonStyle）=====
        AstRadio br = new AstRadio("选项");
        assert !br.isButtonStyle() : "default not button style";
        br.setButtonStyle(true);
        assert br.isButtonStyle() : "setButtonStyle true";
        br.setButtonStyle(false);
        assert !br.isButtonStyle() : "setButtonStyle false";
        br.setButtonStyle(true);

        // 按钮态尺寸：height=32，width=文字宽+40
        FontMetrics fm = br.getFontMetrics(theme().getFontBase());
        int expW = fm.stringWidth("选项") + 40;
        assert br.getPreferredSize().height == 32 : "button height, got " + br.getPreferredSize().height;
        assert br.getPreferredSize().width == expW : "button width, got " + br.getPreferredSize().width;

        // 按钮态未选中：文字仍对浅底 AA（选中白字彩底按惯例豁免）
        assertContrast(theme().getTextRegular(), theme().getFillBlank(), "radio button text on fill");

        // 绘制级断言：选中态强制 fill=1，离屏绘制后背景中心像素应为 PRIMARY 不透明
        br.setSelected(true);
        br.anim.setProgress("fill", 1f);
        int bw = br.getPreferredSize().width, bh = 32;
        br.setSize(bw, bh);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                bw, bh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try { br.paint(gg); } finally { gg.dispose(); }
        int px = img.getRGB(8, bh / 2);   // 远离 0.5px 边框与居中文字，取纯背景点
        int alpha = (px >>> 24) & 0xFF;
        int primRgb = theme().getPrimary().getRGB() & 0xFFFFFF;
        assert alpha == 255 : "button selected bg opaque, alpha=" + alpha;
        assert (px & 0xFFFFFF) == primRgb
                : "button selected bg must be PRIMARY, got " + Integer.toHexString(px & 0xFFFFFF);

        // 离屏绘制未选中态不抛错
        br.setSelected(false);
        br.anim.setProgress("fill", 0f);
        Graphics2D gx = img.createGraphics();
        try { br.paint(gx); } finally { gx.dispose(); }

        // 清理动画定时器，避免自检 JVM 因非 daemon 的 AWT/EDT 不退出而挂死
        br.anim.stopAll();
        r.anim.stopAll();
        anim.stopAll();

        System.out.println("AstRadio self-check OK");
    }

    public static void main(String[] args) {
        new AstRadio("test").selfCheck();
    }
}
