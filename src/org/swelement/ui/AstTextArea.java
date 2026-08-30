package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.framework.AstAbstractComponent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * 多行文本输入 — Element UI textarea 移植。
 * 透明 JScrollPane 包 JTextArea（自动换行），复用 AstInput 的边框/聚焦光晕/占位符配方。
 */
public class AstTextArea extends AstAbstractComponent {
    private final JTextArea area;
    private final JScrollPane scroll;
    private boolean hasText;
    private final String placeholder;

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("focus", 200, Easing::easeInOut);
        anim.register("hover", 200, Easing::easeInOut);
    }

    public AstTextArea(String placeholder, int rows, int columns) {
        this.placeholder = placeholder;
        setLayout(new BorderLayout());
        area = new JTextArea(rows, columns) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!hasText && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(theme().getTextPlaceholder());
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(placeholder, 0, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                    g2.dispose();
                }
            }
        };
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(theme().getFontBase());
        area.setForeground(theme().getTextPrimary());
        area.setBorder(new EmptyBorder(8, 12, 8, 8));
        area.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { hasText = !area.getText().isEmpty(); repaint(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { hasText = !area.getText().isEmpty(); repaint(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
        scroll = new JScrollPane(area);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        add(scroll, BorderLayout.CENTER);

        area.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { anim.go("focus", anim.getProgress("focus"), 1f); }
            public void focusLost(FocusEvent e)   { anim.go("focus", anim.getProgress("focus"), 0f); }
        });
        MouseAdapter m = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { anim.go("hover", anim.getProgress("hover"), 1f); }
            public void mouseExited(MouseEvent e)  { anim.go("hover", anim.getProgress("hover"), 0f); }
        };
        addMouseListener(m);
        scroll.addMouseListener(m);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        float focus = anim.getProgress("focus");
        float hover = anim.getProgress("hover");
        Color border = lerp(theme().getBorderBase(), theme().getPrimary(), Math.max(focus, hover));
        if (!isEnabled()) border = new Color(0xE4E7ED);
        Color bg = isEnabled() ? lerp(theme().getFillBlank(), theme().getFillBase(), hover) : theme().getFillBase();
        int radius = theme().getRadiusBase() * 2;
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.setColor(bg);
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(focus > 0 ? 2f : 1f));
        g2.draw(shape);
        if (focus > 0) {
            Color primary = theme().getPrimary();
            g2.setColor(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), Math.round(50 * focus)));
            g2.setStroke(new BasicStroke(4f));
            g2.draw(shape);
        }
        g2.dispose();
    }

    public String getText() { return area.getText(); }
    public void setText(String t) { area.setText(t); }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        area.setEnabled(enabled);
    }

    @Override
    protected void selfCheck() {
        AstTextArea ta = new AstTextArea("请输入内容", 3, 20);
        assert ta.getText().isEmpty() : "initial empty";
        ta.setText("hello");
        assert "hello".equals(ta.getText()) : "setText works";
        AstTextArea tall = new AstTextArea("p", 8, 20);
        assert tall.getPreferredSize().height > ta.getPreferredSize().height
                : "rows drive height: " + tall.getPreferredSize().height + " vs " + ta.getPreferredSize().height;
        assert ta.getPreferredSize().height >= 60
                : "3-row taller than 60px, got " + ta.getPreferredSize().height;
        ta.setEnabled(false);
        assert !ta.isEnabled() : "panel disabled";
        assert !ta.area.isEnabled() : "area disabled";
        ta.setEnabled(true);
        assert ta.isEnabled() && ta.area.isEnabled() : "re-enabled";
        // 对比度：文字 vs 背景
        assertContrast(theme().getTextPrimary(), theme().getFillBlank(), "textarea text on bg");
        // 离屏绘制不抛异常（含占位符路径）
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(() -> {
            AstTextArea p = new AstTextArea("占位", 3, 20);
            p.setBounds(0, 0, 240, 80);
            p.doLayout();
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(240, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            try { p.paint(gg); } finally { gg.dispose(); }
        }); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstTextArea self-check OK");
    }

    public static void main(String[] args) {
        new AstTextArea("test", 3, 20).selfCheck();
    }
}
