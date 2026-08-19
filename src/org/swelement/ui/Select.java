package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.AnimatedPopup;
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
import java.util.ArrayList;
import java.util.List;

public class Select extends JPanel {
    public static class Option {
        public final String label;
        public final Object value;
        public final String group;
        public final boolean disabled;

        public Option(String label, Object value) { this(label, value, null, false); }

        public Option(String label, Object value, String group, boolean disabled) {
            this.label = label;
            this.value = value;
            this.group = group;
            this.disabled = disabled;
        }
    }

    private final List<Option> options = new ArrayList<>();
    private final List<Option> selected = new ArrayList<>();
    private final JTextField field;
    private final JLabel display = new JLabel();
    private final JPanel tagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
    private final JPanel center = new JPanel(new BorderLayout());
    private final AnimatedPopup popup = new AnimatedPopup();
    private final JPanel optionList = new JPanel();
    private final boolean multiple, filterable;
    private final Animator arrowAnim = new Animator(200, Easing::easeInOut, v -> { arrowAngle = v; repaint(); });
    private float arrowAngle;
    private boolean popupShown, fieldFocus;

    public Select(boolean multiple, boolean filterable) {
        this.multiple = multiple;
        this.filterable = filterable;
        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(220, 40));

        field = filterable ? new JTextField() : null;
        center.setOpaque(false);
        if (multiple) center.add(tagsPanel, BorderLayout.NORTH);
        if (filterable) {
            field.setOpaque(false);
            field.setBorder(new EmptyBorder(0, 12, 0, 0));
            field.setFont(ElementTheme.FONT);
            field.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { fieldFocus = true; repaint(); }
                public void focusLost(FocusEvent e) { fieldFocus = false; repaint(); }
            });
            center.add(field, BorderLayout.CENTER);
        } else {
            display.setOpaque(false);
            display.setBorder(new EmptyBorder(0, 12, 0, 0));
            display.setFont(ElementTheme.FONT);
            center.add(display, BorderLayout.CENTER);
        }
        add(center, BorderLayout.CENTER);

        optionList.setOpaque(false);
        optionList.setLayout(new BoxLayout(optionList, BoxLayout.Y_AXIS));
        popup.getContent().add(optionList, BorderLayout.CENTER);

        MouseAdapter click = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled()) return;
                if (!multiple && !selected.isEmpty() && e.getX() > getWidth() - 46 && e.getX() < getWidth() - 28) {
                    selected.clear();
                    updateDisplay();
                    repaint();
                    return;
                }
                togglePopup();
            }
        };
        addMouseListener(click);
        display.addMouseListener(click);
        tagsPanel.addMouseListener(click);
        if (field != null) field.addMouseListener(click);
    }

    public void addOption(Option o) { options.add(o); }

    public List<Option> getSelected() { return new ArrayList<>(selected); }

    public void clearSelection() { selected.clear(); updateDisplay(); }

    static boolean matches(String label, String filter) {
        return label.toLowerCase().contains(filter.toLowerCase());
    }

    private void togglePopup() {
        if (popupShown) {
            popup.setVisible(false);
            popupShown = false;
            arrowAnim.go(arrowAngle, 0f);
            repaint();
        } else {
            rebuildList(filterable ? field.getText() : null);
            popup.getContent().setPreferredSize(new Dimension(Math.max(180, getWidth()), popup.getContent().getPreferredSize().height));
            popup.show(this, 0, getHeight());
            popupShown = true;
            arrowAnim.go(arrowAngle, 1f);
            repaint();
        }
    }

    private void rebuildList(String filter) {
        optionList.removeAll();
        String lastGroup = null;
        for (Option o : options) {
            if (filter != null && !filter.isEmpty() && !matches(o.label, filter)) continue;
            if (o.group != null && !o.group.equals(lastGroup)) {
                lastGroup = o.group;
                JLabel g = new JLabel(o.group);
                g.setForeground(new Color(0x909399));
                g.setFont(ElementTheme.FONT.deriveFont(Font.PLAIN, 12f));
                g.setBorder(new EmptyBorder(6, 12, 4, 0));
                optionList.add(g);
            }
            optionList.add(new OptionRow(o));
        }
        if (optionList.getComponentCount() == 0) {
            JLabel empty = new JLabel("无匹配数据");
            empty.setForeground(new Color(0x909399));
            empty.setBorder(new EmptyBorder(10, 12, 10, 0));
            optionList.add(empty);
        }
        optionList.revalidate();
        optionList.repaint();
    }

    private void choose(Option o) {
        if (multiple) {
            if (selected.contains(o)) selected.remove(o);
            else selected.add(o);
        } else {
            selected.clear();
            selected.add(o);
            if (filterable) field.setText(o.label);
            popup.setVisible(false);
            popupShown = false;
            arrowAnim.go(arrowAngle, 0f);
        }
        updateDisplay();
        rebuildList(null);
        repaint();
    }

    private void updateDisplay() {
        tagsPanel.removeAll();
        if (multiple) {
            for (Option o : selected) {
                JLabel chip = new JLabel(o.label + "  ×");
                chip.setOpaque(true);
                chip.setBackground(new Color(0xF4F4F5));
                chip.setForeground(new Color(0x606266));
                chip.setFont(ElementTheme.FONT.deriveFont(Font.PLAIN, 12f));
                chip.setBorder(new EmptyBorder(2, 8, 2, 8));
                chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                chip.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) { if (!isEnabled()) return; selected.remove(o); updateDisplay(); rebuildList(null); }
                });
                tagsPanel.add(chip);
            }
        } else {
            display.setText(selected.isEmpty() ? "" : selected.get(0).label);
        }
        tagsPanel.revalidate();
        tagsPanel.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean highlighted = popupShown || fieldFocus;
        Color border = isEnabled() ? (highlighted ? ElementTheme.PRIMARY : new Color(0xDCDFE6)) : new Color(0xE4E7ED);
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        g2.setColor(isEnabled() ? Color.WHITE : ElementTheme.FILL_BASE);
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(highlighted ? 2f : 1f));
        g2.draw(shape);

        float ax = getWidth() - 18f, ay = getHeight() / 2f;
        Graphics2D a2 = (Graphics2D) g2.create();
        a2.rotate(Math.PI * arrowAngle, ax, ay);
        a2.setColor(new Color(0xC0C4CC));
        a2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        a2.drawLine(Math.round(ax - 4), Math.round(ay - 1), Math.round(ax), Math.round(ay + 2));
        a2.drawLine(Math.round(ax + 4), Math.round(ay - 1), Math.round(ax), Math.round(ay + 2));
        a2.dispose();

        if (!multiple && !selected.isEmpty()) {  // 可清空 ×
            g2.setColor(new Color(0xC0C4CC));
            FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT);
            g2.drawString("\u00d7", getWidth() - 38 - fm.stringWidth("\u00d7") / 2, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        }
        g2.dispose();
    }

    private class OptionRow extends JPanel {
        private final Option option;
        private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });
        private float hover;

        OptionRow(Option o) {
            this.option = o;
            setOpaque(false);
            setPreferredSize(new Dimension(Math.max(180, Select.this.getWidth()), 32));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { if (option.disabled) return; hoverAnim.go(hover, 1f); }
                public void mouseExited(MouseEvent e) { hoverAnim.go(hover, 0f); }
                public void mouseClicked(MouseEvent e) { if (!option.disabled) choose(option); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!option.disabled) {
                g2.setColor(ElementTheme.lerp(Color.WHITE, new Color(0xF5F7FA), hover));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            boolean isSel = selected.contains(option);
            g2.setColor(option.disabled ? new Color(0xC0C4CC)
                    : (isSel ? ElementTheme.PRIMARY : ElementTheme.TEXT_REGULAR));
            FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT);
            String text = option.label + (multiple && isSel ? "  \u221a" : "");
            g2.drawString(text, 12, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    public static void selfCheck() {
        assert matches("Apple", "app");
        assert matches("Apple", "APPLE");
        assert matches("苹果", "苹");
        assert !matches("Apple", "pear");
        assert !matches("", "a");
        System.out.println("Select self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
