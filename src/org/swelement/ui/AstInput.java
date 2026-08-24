package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class AstInput extends JPanel implements FormValueProvider, FormInvalidMarker {
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    private static final int[] TIER_HEIGHT = {40, 32, 28};
    private static final float[] TIER_FONT = {14f, 13f, 12f};
    private static final int[] TIER_VPAD = {10, 8, 4};
    private static final int[] TIER_HPAD = {16, 12, 8};
    private static final int[] TIER_CLEAR = {18, 16, 14};
    private int tier = SIZE_DEFAULT;

    public static final int TEXT = 0, PASSWORD = 1;
    private static final Color ICON_COLOR = new Color(0x606266);   // ≥7:1
    private static final Color ICON_HOVER = new Color(0x303133);   // hover 加深，对比单调上升
    private final boolean password;
    private JPanel eyeBtn;
    private AstIcon eyeIcon;
    private boolean pwVisible = false;
    private final JPanel east;
    private final MouseAdapter hoverKeeper;
    private JPanel west;
    private AstIcon prefixIcon, suffixIcon;

    private final JTextField field;
    private final AstCloseButton clearBtn = new AstCloseButton(16);
    private final Animator focusAnim = new Animator(200, Easing::easeInOut, v -> { focus = v; repaint(); });
    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private final Animator clearAnim = new Animator(150, Easing::easeInOut, v -> { clearVis = v; syncClear(); repaint(); });
    private float focus, hover, clearVis;
    private boolean hasText, hovering, focused;
    private boolean invalid = false;
    private final String placeholder;

    public AstInput(String placeholder) { this(placeholder, TEXT); }

    public AstInput(String placeholder, int type) {
        this.placeholder = placeholder;
        this.password = (type == PASSWORD);
        setOpaque(false);
        setLayout(new BorderLayout());
        field = password ? createPasswordField() : createTextField();
        field.setOpaque(false);
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
        JPanel eastLocal = new JPanel(new GridBagLayout()); // 居中放置，避免 BorderLayout.EAST 拉伸高度
        east = eastLocal;
        east.setOpaque(false);
        // 右留 8px、左留 4px，使清空按钮与输入框右边缘及文字均保持间距，不顶边
        east.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 8));
        if (password) {
            eyeIcon = new AstIcon(AstIcon.EYE, ICON_COLOR, 16);
            eyeBtn = new JPanel(new GridBagLayout());
            eyeBtn.setOpaque(false);
            eyeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            eyeBtn.add(eyeIcon);
            eyeBtn.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    pwVisible = !pwVisible;
                    ((JPasswordField) field).setEchoChar(pwVisible ? (char) 0 : '\u25cf');
                    eyeIcon.setType(pwVisible ? AstIcon.EYE_OFF : AstIcon.EYE);
                }
                public void mouseEntered(MouseEvent e) { eyeIcon.setColor(ICON_HOVER); }
                public void mouseExited(MouseEvent e)  { eyeIcon.setColor(ICON_COLOR); }
            });
            east.add(eyeBtn);
        }
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
        hoverKeeper = m;
        field.addMouseListener(m);   // field 铺满面板，鼠标事件落在 field 上
        addMouseListener(m);
        east.addMouseListener(m);    // 鼠标从 field 移入 east（清空按钮区）时保持 hovering，× 不淡出
        applyTier();
    }

    /** 尺寸档位（对齐 Element UI）：高度 40/32/28，档位联动字体、内边距与清空按钮尺寸。 */
    public void setSize(int tier) {
        if (tier < SIZE_LARGE || tier > SIZE_SMALL)
            throw new IllegalArgumentException("invalid size tier: " + tier);
        this.tier = tier;
        applyTier();
    }

    private void applyTier() {
        field.setFont(ElementTheme.FONT.deriveFont(TIER_FONT[tier]));
        field.setBorder(BorderFactory.createEmptyBorder(TIER_VPAD[tier], TIER_HPAD[tier], TIER_VPAD[tier], 8));
        clearBtn.setButtonSize(TIER_CLEAR[tier]);
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = TIER_HEIGHT[tier];
        return d;
    }

    /** 前缀图标（AstIcon 常量）。静态装饰，不可点。 */
    public void setPrefixIcon(int iconType) {
        if (west == null) {
            west = new JPanel(new GridBagLayout());
            west.setOpaque(false);
            west.setBorder(BorderFactory.createEmptyBorder(0, TIER_HPAD[tier], 0, 4));
            west.addMouseListener(hoverKeeper);
            add(west, BorderLayout.WEST);
        }
        if (prefixIcon != null) west.remove(prefixIcon);
        prefixIcon = new AstIcon(iconType, ICON_COLOR, 16);
        west.add(prefixIcon);
        revalidate(); repaint();
    }

    /** 后缀图标（AstIcon 常量），显示在清空按钮左侧。 */
    public void setSuffixIcon(int iconType) {
        if (suffixIcon != null) east.remove(suffixIcon);
        suffixIcon = new AstIcon(iconType, ICON_COLOR, 16);
        east.add(suffixIcon, 0);
        revalidate(); repaint();
    }

    private JTextField createTextField() {
        return new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintPlaceholder(g, this);
            }
        };
    }

    private JPasswordField createPasswordField() {
        JPasswordField pf = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintPlaceholder(g, this);
            }
        };
        pf.setEchoChar('\u25cf'); // ●
        return pf;
    }

    /** 占位符绘制：x 取边框左内边距，随尺寸档位联动。 */
    private void paintPlaceholder(Graphics g, JTextComponent c) {
        if (!hasText && !c.isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(ElementTheme.TEXT_PLACEHOLDER);
            g2.setFont(c.getFont());
            FontMetrics fm = g2.getFontMetrics();
            Insets ins = c.getBorder() != null ? c.getBorder().getBorderInsets(c) : new Insets(0, 0, 0, 0);
            g2.drawString(placeholder, ins.left, (c.getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
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
        if (invalid) border = ElementTheme.DANGER;
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

    public String getText() {
        return password ? new String(((JPasswordField) field).getPassword()) : field.getText();
    }

    /** 列数（透传给内嵌文本框，决定首选宽度；高度由尺寸档位决定）。 */
    public void setColumns(int columns) { field.setColumns(columns); revalidate(); }
    public void setText(String t) { field.setText(t); }

    @Override public String getFormValue() { return getText(); }
    @Override public void setFormValue(String v) { setText(v == null ? "" : v); }
    @Override public void setInvalid(boolean inv) { this.invalid = inv; repaint(); }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        field.setEnabled(enabled);
        clearBtn.setEnabled(enabled); // 禁用态：清空按钮灰化且不可点
        clearBtn.setInteractive(enabled && clearVis > 0.5f);
    }

    static void selfCheck() {
        AstInput df = new AstInput("默认");
        assert df.getPreferredSize().height == 32 : "DEFAULT height 32, got " + df.getPreferredSize().height;
        AstInput lg = new AstInput("大");
        lg.setSize(AstInput.SIZE_LARGE);
        assert lg.getPreferredSize().height == 40 : "LARGE height 40, got " + lg.getPreferredSize().height;
        AstInput sm = new AstInput("小");
        sm.setSize(AstInput.SIZE_SMALL);
        assert sm.getPreferredSize().height == 28 : "SMALL height 28, got " + sm.getPreferredSize().height;
        boolean threw = false;
        try { sm.setSize(9); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "invalid tier must throw";

        // 密码模式：默认掩码，眼睛切换明文/掩码
        AstInput pw = new AstInput("请输入密码", AstInput.PASSWORD);
        pw.setText("secret123");
        assert "secret123".equals(pw.getText()) : "password getText";
        final JPasswordField pf = (JPasswordField) findTextComponent(pw);
        assert pf.getEchoChar() != 0 : "masked by default, echo=" + pf.getEchoChar();
        final Throwable[] pwErr = {null};
        try {
            SwingUtilities.invokeAndWait(() -> eyeClickForTest(pw));
            assert pf.getEchoChar() == 0 : "eye toggle should show plaintext";
            SwingUtilities.invokeAndWait(() -> eyeClickForTest(pw));
            assert pf.getEchoChar() != 0 : "eye toggle should mask again";
        } catch (Throwable t) { pwErr[0] = t; }
        if (pwErr[0] != null) throw new RuntimeException(pwErr[0]);

        AstInput in = new AstInput("占位符");
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

        // 前后缀图标
        AstInput pi = new AstInput("搜索");
        pi.setPrefixIcon(AstIcon.SEARCH);
        assert countAstIcons(pi) == 1 : "prefix icon added, count=" + countAstIcons(pi);
        AstInput si = new AstInput("");
        si.setText("x");
        si.setSuffixIcon(AstIcon.SETTING);
        assert countAstIcons(si) == 1 : "suffix icon added, count=" + countAstIcons(si);
        // 重复设置不叠加
        pi.setPrefixIcon(AstIcon.USER);
        assert countAstIcons(pi) == 1 : "prefix icon replaced, count=" + countAstIcons(pi);

        // FormValueProvider 取值契约
        AstInput fb = new AstInput("占位");
        fb.setText("hello");
        assert fb.getFormValue().equals("hello") : "AstInput.getFormValue";
        fb.setFormValue("world");
        assert fb.getFormValue().equals("world") : "AstInput.setFormValue";

        System.out.println("AstInput self-check OK");
    }

    /** 测试辅助：向 AstInput 内的 AstCloseButton 派发点击事件（同包访问私有字段）。 */
    private static void clearBtnClickForTest(AstInput in) {
        for (Component c : in.getComponents()) {
            if (c instanceof JPanel) {
                for (Component cc : ((JPanel) c).getComponents()) {
                    if (cc instanceof AstCloseButton) {
                        cc.dispatchEvent(new java.awt.event.MouseEvent(cc, java.awt.event.MouseEvent.MOUSE_PRESSED,
                                System.currentTimeMillis(), 0, 10, 10, 1, false));
                        return;
                    }
                }
            }
        }
        throw new AssertionError("AstCloseButton not found in AstInput");
    }

    /** 测试辅助：找到 AstInput 内的文本组件（JTextField 或 JPasswordField）。 */
    private static JTextComponent findTextComponent(AstInput in) {
        for (Component c : in.getComponents())
            if (c instanceof JTextComponent) return (JTextComponent) c;
        throw new AssertionError("text component not found in AstInput");
    }

    /** 测试辅助：向密码框的眼睛按钮派发按下事件。 */
    private static void eyeClickForTest(AstInput in) {
        for (Component c : in.getComponents()) {
            if (c instanceof JPanel) {
                for (Component cc : ((JPanel) c).getComponents()) {
                    if (cc instanceof JPanel) {
                        boolean hasIcon = false;
                        for (Component ccc : ((JPanel) cc).getComponents()) if (ccc instanceof AstIcon) hasIcon = true;
                        if (hasIcon) {
                            cc.dispatchEvent(new java.awt.event.MouseEvent(cc, java.awt.event.MouseEvent.MOUSE_PRESSED,
                                    System.currentTimeMillis(), 0, 8, 8, 1, false));
                            return;
                        }
                    }
                }
            }
        }
        throw new AssertionError("eye button not found in AstInput");
    }

    /** 测试辅助：统计 AstInput 子树中的 AstIcon 数量。 */
    private static int countAstIcons(Container c) {
        int n = 0;
        for (Component cc : c.getComponents()) {
            if (cc instanceof AstIcon) n++;
            if (cc instanceof Container) n += countAstIcons((Container) cc);
        }
        return n;
    }

    public static void main(String[] args) { selfCheck(); }
}
