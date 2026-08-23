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
    private final CloseButton clearBtn = new CloseButton(16);
    private final Animator focusAnim = new Animator(200, Easing::easeInOut, v -> { focus = v; repaint(); });
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private final Animator clearAnim = new Animator(150, Easing::easeInOut, v -> { clearVis = v; syncClear(); repaint(); });
    private float focus, hover, clearVis;
    private boolean hasText, hovering, focused;
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
        field.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));
        field.setFont(ElementTheme.FONT);
        field.setForeground(ElementTheme.TEXT_MAIN);
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { hasText = !field.getText().isEmpty(); updateClear(); }
            public void removeUpdate(DocumentEvent e) { hasText = !field.getText().isEmpty(); updateClear(); }
            public void changedUpdate(DocumentEvent e) {}
        });
        add(field, BorderLayout.CENTER);
        clearBtn.addActionListener(e -> { setText(""); field.requestFocus(); });
        clearBtn.setAlpha(0f);
        clearBtn.setInteractive(false);
        JPanel east = new JPanel(new GridBagLayout()); // 居中放置，避免 BorderLayout.EAST 拉伸高度
        east.setOpaque(false);
        // 右留 8px、左留 4px，使清空按钮与输入框右边缘及文字均保持间距，不顶边
        east.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 8));
        east.add(clearBtn);
        add(east, BorderLayout.EAST);

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { focused = true;  focusAnim.go(focus, 1f); updateClear(); }
            public void focusLost(FocusEvent e)   { focused = false; focusAnim.go(focus, 0f); updateClear(); }
        });
        MouseAdapter m = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovering = true;  hoverAnim.go(hover, 1f); updateClear(); }
            public void mouseExited(MouseEvent e)  { hovering = false; hoverAnim.go(hover, 0f); updateClear(); }
        };
        field.addMouseListener(m);   // field 铺满面板，鼠标事件落在 field 上
        addMouseListener(m);
        east.addMouseListener(m);    // 鼠标从 field 移入 east（清空按钮区）时保持 hovering，× 不淡出
    }

    private void updateClear() {
        // 目标值取 hover/focus 的「状态」而非动画中间值：Animator.go() 会同步回调 update(from)，
        // 若读 focus/hover 浮点数，事件发生瞬间它们仍是动画起始值（0），清空按钮永远淡不进来。
        float target = hasText && (hovering || focused) ? 1f : 0f;
        clearAnim.go(clearVis, target);
    }

    /** 清空按钮淡入淡出动画驱动 alpha 与可交互性（无文本或 alpha 低时不拦截点击）。 */
    private void syncClear() {
        clearBtn.setAlpha(clearVis);
        clearBtn.setInteractive(isEnabled() && clearVis > 0.5f);
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
        g2.dispose();
    }

    public String getText() { return field.getText(); }
    public void setText(String t) { field.setText(t); }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        field.setEnabled(enabled);
        clearBtn.setInteractive(enabled && clearVis > 0.5f);
    }

    static void selfCheck() {
        Input in = new Input("占位符");
        assert in.getText().isEmpty() : "initial text empty";
        in.setText("hello");
        assert "hello".equals(in.getText()) : "setText works";
        // hover 触发清空按钮淡入 → 可交互 → 点击清空（Animator 走 EDT）
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                in.setSize(260, 40);
                in.doLayout();
                in.dispatchEvent(new java.awt.event.MouseEvent(in, java.awt.event.MouseEvent.MOUSE_ENTERED,
                        System.currentTimeMillis(), 0, 10, 10, 0, false));
            });
            Thread.sleep(300);
            SwingUtilities.invokeAndWait(() -> clearBtnClickForTest(in));
            Thread.sleep(50);
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        assert in.getText().isEmpty() : "clear button click should clear text, got: " + in.getText();
        System.out.println("Input self-check OK");
    }

    /** 测试辅助：向 Input 内的 CloseButton 派发点击事件（同包访问私有字段）。 */
    private static void clearBtnClickForTest(Input in) {
        for (Component c : in.getComponents()) {
            if (c instanceof JPanel) {
                for (Component cc : ((JPanel) c).getComponents()) {
                    if (cc instanceof CloseButton) {
                        cc.dispatchEvent(new java.awt.event.MouseEvent(cc, java.awt.event.MouseEvent.MOUSE_CLICKED,
                                System.currentTimeMillis(), 0, 10, 10, 1, false));
                        return;
                    }
                }
            }
        }
        throw new AssertionError("CloseButton not found in Input");
    }

    public static void main(String[] args) { selfCheck(); }
}
