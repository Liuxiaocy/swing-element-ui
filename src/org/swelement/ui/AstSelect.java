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

public class AstSelect extends JPanel implements FormValueProvider, FormInvalidMarker {
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
    // 可清空 ×（复用 AstInput 批次 1 的 east 面板配方，替代手绘 × + 坐标命中）
    private final AstCloseButton clearBtn = new AstCloseButton(16);
    private final Animator clearAnim = new Animator(150, Easing::easeInOut, v -> { clearVis = v; syncClear(); repaint(); });
    private float clearVis;
    private boolean hovering;
    private boolean invalid = false;

    public AstSelect(boolean multiple, boolean filterable) {
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

        // 可清空 ×（复用 AstInput 批次 1 的 east 面板配方，替代手绘 × + 坐标命中测试）
        clearBtn.setAlpha(0f);
        clearBtn.setInteractive(false);
        clearBtn.setOnClose(() -> {
            if (multiple) return;
            selected.clear();
            updateDisplay();
            rebuildList(null);
            repaint();
        });
        JPanel east = new JPanel(new GridBagLayout());
        east.setOpaque(false);
        east.setBorder(new EmptyBorder(0, 4, 0, 8));
        east.add(clearBtn);
        add(east, BorderLayout.EAST);

        MouseAdapter hoverM = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovering = true;  updateClear(); }
            public void mouseExited(MouseEvent e)  { hovering = false; updateClear(); }
        };
        addMouseListener(hoverM); display.addMouseListener(hoverM); tagsPanel.addMouseListener(hoverM); east.addMouseListener(hoverM);
        if (field != null) field.addMouseListener(hoverM);
        east.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { if (!isEnabled()) return; togglePopup(); }
        });

        optionList.setOpaque(false);
        optionList.setLayout(new BoxLayout(optionList, BoxLayout.Y_AXIS));
        popup.getContent().add(optionList, BorderLayout.CENTER);
        popup.setDismissListener(() -> {
            if (popupShown) {
                popupShown = false;
                arrowAnim.go(arrowAngle, 0f);
                repaint();
            }
        });

        MouseAdapter click = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                togglePopup();
            }
        };
        addMouseListener(click);
        display.addMouseListener(click);
        tagsPanel.addMouseListener(click);
        if (field != null) field.addMouseListener(click);
    }

    /** Convenience constructor: single-select non-filterable with given labels (value = label). */
    public AstSelect(String[] labels) {
        this(false, false);
        for (String s : labels) addOption(new Option(s, s));
    }

    /** AstSelect a single option by its value (Object.equals). For single-select mode. */
    public void setSelectedValue(Object value) {
        selected.clear();
        if (value != null) {
            for (Option o : options) {
                if (value.equals(o.value)) { selected.add(o); break; }
            }
        }
        updateDisplay();
    }

    /** Returns the first selected option's value (for single-select mode), or null. */
    public Object getSelectedValue() {
        return selected.isEmpty() ? null : selected.get(0).value;
    }

    /** AstSelect option by index. */
    public void setSelectedIndex(int i) {
        selected.clear();
        if (i >= 0 && i < options.size()) selected.add(options.get(i));
        updateDisplay();
    }

    public int getSelectedIndex() {
        if (selected.isEmpty()) return -1;
        return options.indexOf(selected.get(0));
    }

    public void addOption(Option o) { options.add(o); }

    public List<Option> getSelected() { return new ArrayList<>(selected); }

    public void clearSelection() { selected.clear(); updateDisplay(); }

    public List<Option> getOptions() { return new ArrayList<>(options); }

    @Override public String getFormValue() {
        if (multiple) {
            StringBuilder sb = new StringBuilder();
            for (Option o : selected) { if (sb.length() > 0) sb.append(","); sb.append(o.value); }
            return sb.toString();
        }
        Object v = getSelectedValue();
        return v == null ? "" : String.valueOf(v);
    }
    @Override public void setFormValue(String v) {
        selected.clear();
        if (v != null && !v.isEmpty()) {
            for (String tok : v.split(",")) {
                for (Option o : options) if (String.valueOf(o.value).equals(tok.trim())) { selected.add(o); break; }
            }
        }
        updateDisplay();
    }
    @Override public void setInvalid(boolean inv) { this.invalid = inv; repaint(); }

    static boolean matches(String label, String filter) {
        return label.toLowerCase().contains(filter.toLowerCase());
    }

    private void updateClear() {
        float target = (!multiple && !selected.isEmpty() && hovering && isEnabled()) ? 1f : 0f;
        clearAnim.go(clearVis, target);
    }

    private void syncClear() {
        clearBtn.setAlpha(clearVis);
        clearBtn.setInteractive(clearVis > 0.5f);
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
                    public void mousePressed(MouseEvent e) { if (!isEnabled()) return; selected.remove(o); updateDisplay(); rebuildList(null); }
                });
                tagsPanel.add(chip);
            }
        } else {
            display.setText(selected.isEmpty() ? "" : selected.get(0).label);
        }
        tagsPanel.revalidate();
        tagsPanel.repaint();
        updateClear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean highlighted = popupShown || fieldFocus;
        Color border = isEnabled() ? (highlighted ? ElementTheme.PRIMARY : new Color(0xDCDFE6)) : new Color(0xE4E7ED);
        if (invalid) border = ElementTheme.DANGER;
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        g2.setColor(isEnabled() ? Color.WHITE : ElementTheme.FILL_BASE);
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(highlighted ? 2f : 1f));
        g2.draw(shape);

        if (clearVis < 0.5f) {  // × 淡入过半即隐藏箭头（Element「× 替换箭头」）
            float ax = getWidth() - 18f, ay = getHeight() / 2f;
            Graphics2D a2 = (Graphics2D) g2.create();
            a2.rotate(Math.PI * arrowAngle, ax, ay);
            a2.setColor(new Color(0xC0C4CC));
            a2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            a2.drawLine(Math.round(ax - 4), Math.round(ay - 1), Math.round(ax), Math.round(ay + 2));
            a2.drawLine(Math.round(ax + 4), Math.round(ay - 1), Math.round(ax), Math.round(ay + 2));
            a2.dispose();
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
            setPreferredSize(new Dimension(Math.max(180, AstSelect.this.getWidth()), 32));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { if (option.disabled) return; hoverAnim.go(hover, 1f); }
                public void mouseExited(MouseEvent e) { hoverAnim.go(hover, 0f); }
                public void mousePressed(MouseEvent e) { if (!option.disabled) choose(option); }
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
            g2.setFont(ElementTheme.FONT);
            g2.setColor(option.disabled ? new Color(0xC0C4CC)
                    : (isSel ? ElementTheme.PRIMARY : ElementTheme.TEXT_REGULAR));
            FontMetrics fm = g2.getFontMetrics();
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

        // 可清空：hover 淡入 ×，点击清空选择（复用 AstInput 的测试配方）
        final AstSelect sel = new AstSelect(new String[]{"北京", "上海", "广州"});
        sel.setSelectedIndex(1);
        assert "上海".equals(sel.getSelectedValue());
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                sel.setSize(280, 40);
                sel.doLayout();
                sel.dispatchEvent(new java.awt.event.MouseEvent(sel, java.awt.event.MouseEvent.MOUSE_ENTERED,
                        System.currentTimeMillis(), 0, 10, 10, 0, false));
            });
            Thread.sleep(300);
            SwingUtilities.invokeAndWait(() -> clearBtnClickForTest(sel));
            Thread.sleep(50);
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        assert sel.getSelectedValue() == null : "clear should empty selection, got " + sel.getSelectedValue();

        // FormValueProvider 取值契约（单选 + 多选）
        AstSelect ss = new AstSelect(new String[]{"a", "b", "c"});
        ss.setSelectedValue("a");
        assert "a".equals(ss.getFormValue()) : "AstSelect single getFormValue, got " + ss.getFormValue();
        AstSelect ms = new AstSelect(true, false);
        ms.addOption(new AstSelect.Option("A", "a"));
        ms.addOption(new AstSelect.Option("B", "b"));
        ms.addOption(new AstSelect.Option("C", "c"));
        ms.setFormValue("a,c");
        assert "a,c".equals(ms.getFormValue()) : "AstSelect multi getFormValue, got " + ms.getFormValue();
        ms.setFormValue("");
        assert ms.getFormValue().isEmpty() : "AstSelect multi empty after setFormValue('')";

        System.out.println("AstSelect self-check OK");
    }

    /** 测试辅助：向 AstSelect 内的 AstCloseButton 派发按下事件。 */
    private static void clearBtnClickForTest(AstSelect sel) {
        for (Component c : sel.getComponents()) {
            if (c instanceof JPanel) {
                for (Component cc : ((JPanel) c).getComponents()) {
                    if (cc instanceof AstCloseButton) {
                        cc.dispatchEvent(new java.awt.event.MouseEvent(cc, java.awt.event.MouseEvent.MOUSE_PRESSED,
                                System.currentTimeMillis(), 0, 8, 8, 1, false));
                        return;
                    }
                }
            }
        }
        throw new AssertionError("AstCloseButton not found in AstSelect");
    }

    public static void main(String[] args) { selfCheck(); }
}
