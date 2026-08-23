package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

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
 * 透明 JScrollPane 包 JTextArea（自动换行），复用 Input 的边框/聚焦光晕/占位符配方。
 */
public class TextArea extends JPanel {
    private final JTextArea area;
    private final JScrollPane scroll;
    private final Animator focusAnim = new Animator(200, Easing::easeInOut, v -> { focus = v; repaint(); });
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private float focus, hover;
    private boolean hasText;
    private final String placeholder;

    public TextArea(String placeholder, int rows, int columns) {
        this.placeholder = placeholder;
        setOpaque(false);
        setLayout(new BorderLayout());
        area = new JTextArea(rows, columns) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!hasText && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(ElementTheme.TEXT_PLACEHOLDER);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(placeholder, 0, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                    g2.dispose();
                }
            }
        };
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(ElementTheme.FONT);
        area.setForeground(ElementTheme.TEXT_MAIN);
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
            public void focusGained(FocusEvent e) { focusAnim.go(focus, 1f); }
            public void focusLost(FocusEvent e)   { focusAnim.go(focus, 0f); }
        });
        MouseAdapter m = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); }
        };
        addMouseListener(m);
        scroll.addMouseListener(m);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color border = ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, Math.max(focus, hover));
        if (!isEnabled()) border = new Color(0xE4E7ED);
        Color bg = isEnabled() ? ElementTheme.lerp(ElementTheme.FILL_BLANK, ElementTheme.FILL_BASE, hover) : ElementTheme.FILL_BASE;
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, ElementTheme.RADIUS * 2, ElementTheme.RADIUS * 2);
        g2.setColor(bg);
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(focus > 0 ? 2f : 1f));
        g2.draw(shape);
        if (focus > 0) {  // 聚焦光晕（同 Input）
            g2.setColor(new Color(64, 158, 255, Math.round(50 * focus)));
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

    static void selfCheck() {
        TextArea ta = new TextArea("请输入内容", 3, 20);
        assert ta.getText().isEmpty() : "initial empty";
        ta.setText("hello");
        assert "hello".equals(ta.getText()) : "setText works";
        TextArea tall = new TextArea("p", 8, 20);
        assert tall.getPreferredSize().height > ta.getPreferredSize().height
                : "rows drive height: " + tall.getPreferredSize().height + " vs " + ta.getPreferredSize().height;
        assert ta.getPreferredSize().height >= 60
                : "3-row taller than 60px, got " + ta.getPreferredSize().height;
        ta.setEnabled(false);
        assert !ta.isEnabled() : "panel disabled";
        assert !ta.area.isEnabled() : "area disabled";
        ta.setEnabled(true);
        assert ta.isEnabled() && ta.area.isEnabled() : "re-enabled";
        // 离屏绘制不抛异常（含占位符路径）
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(() -> {
            TextArea p = new TextArea("占位", 3, 20);
            p.setBounds(0, 0, 240, 80);
            p.doLayout();
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(240, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            try { p.paint(gg); } finally { gg.dispose(); }
        }); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("TextArea self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
