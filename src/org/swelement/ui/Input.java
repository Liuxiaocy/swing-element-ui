package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class Input extends JPanel {
    private final JTextField field;
    private final Animator focusAnim = new Animator(200, Easing::easeInOut, v -> { focus = v; repaint(); });
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private final Animator clearAnim = new Animator(150, Easing::easeInOut, v -> { clearVis = v; repaint(); });
    private float focus, hover, clearVis;
    private boolean hasText;
    private final String placeholder;

    public Input(String placeholder) {
        this.placeholder = placeholder;
        setOpaque(false);
        setLayout(new BorderLayout());
        field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!hasText && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(ElementTheme.TEXT_PLACEHOLDER);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(placeholder, 12, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                    g2.dispose();
                }
            }
        };
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 30));
        field.setFont(ElementTheme.FONT);
        field.setForeground(ElementTheme.TEXT_MAIN);
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { hasText = !field.getText().isEmpty(); updateClear(); }
            public void removeUpdate(DocumentEvent e) { hasText = !field.getText().isEmpty(); updateClear(); }
            public void changedUpdate(DocumentEvent e) {}
        });
        add(field, BorderLayout.CENTER);

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { focusAnim.go(focus, 1f); updateClear(); }
            public void focusLost(FocusEvent e)   { focusAnim.go(focus, 0f); updateClear(); }
        });
        MouseAdapter m = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hoverAnim.go(hover, 1f); updateClear(); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); updateClear(); }
            public void mouseClicked(MouseEvent e) {
                if (e.getX() > getWidth() - 30) { setText(""); field.requestFocus(); }
            }
        };
        field.addMouseListener(m);   // field 铺满面板，鼠标事件落在 field 上
        addMouseListener(m);
    }

    private void updateClear() {
        float target = hasText ? Math.max(focus, hover) : 0f;
        clearAnim.go(clearVis, target);
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
        if (focus > 0) {  // 聚焦光晕
            g2.setColor(new Color(64, 158, 255, Math.round(50 * focus)));
            g2.setStroke(new BasicStroke(4f));
            g2.draw(shape);
        }
        if (clearVis > 0) {  // × 淡入
            g2.setColor(new Color(192, 196, 204, Math.round(255 * clearVis)));
            Font f = g2.getFont().deriveFont(Font.BOLD, 14f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            String x = "\u00d7";
            g2.drawString(x, getWidth() - 24 - fm.stringWidth(x) / 2, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        }
        g2.dispose();
    }

    public String getText() { return field.getText(); }
    public void setText(String t) { field.setText(t); }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        field.setEnabled(enabled);
    }
}
