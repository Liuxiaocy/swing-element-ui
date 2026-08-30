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

    public AstCheckbox(String text) {
        super();
        this.text = text;
    }

    public String getText() { return text; }

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

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(theme().getFontBase());
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
        System.out.println("AstCheckbox self-check OK");
    }

    public static void main(String[] args) {
        new AstCheckbox("test").selfCheck();
    }
}
