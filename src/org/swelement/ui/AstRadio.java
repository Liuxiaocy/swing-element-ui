package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.framework.AstInteractiveComponent;

import java.awt.*;

public class AstRadio extends AstInteractiveComponent {
    private final String text;

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("dot", 200, Easing::easeOut);
        anim.register("border", 200, Easing::easeInOut);
    }

    @Override
    protected void onSelectedChanged(boolean selected) {
        super.onSelectedChanged(selected);
        anim.go("border", anim.getProgress("border"), selected ? 1f : 0f);
        anim.go("dot", anim.getProgress("dot"), selected ? 1f : 0f);
        if (selectionChangeHook != null) selectionChangeHook.run();
    }

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

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(theme().getFontBase());
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
        System.out.println("AstRadio self-check OK");
    }

    public static void main(String[] args) {
        new AstRadio("test").selfCheck();
    }
}
