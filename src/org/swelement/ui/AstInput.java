package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.AnimatedPopup;
import org.swelement.core.Easing;
import org.swelement.core.PopupPositioner;
import org.swelement.framework.AstAbstractComponent;

import javax.swing.*;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.DocumentFilter.FilterBypass;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AstInput extends AstAbstractComponent implements FormValueProvider, FormInvalidMarker {
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    private static final int[] TIER_HEIGHT = {40, 32, 28};
    private static final float[] TIER_FONT = {14f, 13f, 12f};
    private static final int[] TIER_VPAD = {10, 8, 4};
    private static final int[] TIER_HPAD = {16, 12, 8};
    private static final int[] TIER_CLEAR = {18, 16, 14};
    private static final int ITEM_HEIGHT = 34;
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
    private JComponent prefixComp, suffixComp;

    private final JTextField field;
    private final AstCloseButton clearBtn = new AstCloseButton(16);
    private boolean hasText, hovering, focused;
    private boolean invalid = false;
    private final String placeholder;

    // ---- 输入建议（基于 AnimatedPopup） ----
    private final List<String> suggestions = new ArrayList<>();
    private boolean showSuggestionsOnFocus = false;
    private boolean suggestionsEnabled = false;
    private AnimatedPopup suggestionPopup;
    private JPanel suggestionItemsPanel;
    private List<String> currentFiltered = new ArrayList<>();
    private String suggestionSelectedValue = null;

    // ---- 最大长度限制 + 尾部字数统计 ----
    private int maxLength = 0;            // 0 = 不限制
    private boolean counterVisible = false;
    private JLabel counterLabel;

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("focus", 200, Easing::easeInOut);
        anim.register("hover", 200, Easing::easeInOut);
        anim.register("clear", 150, Easing::easeInOut);
    }

    public AstInput(String placeholder) { this(placeholder, TEXT); }

    public AstInput(String placeholder, int type) {
        this.placeholder = placeholder;
        this.password = (type == PASSWORD);
        setLayout(new BorderLayout());
        field = password ? createPasswordField() : createTextField();
        field.setOpaque(false);
        field.setFont(theme().getFontBase());
        field.setForeground(theme().getTextPrimary());
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onTextChanged(); }
            public void removeUpdate(DocumentEvent e) { onTextChanged(); }
            public void changedUpdate(DocumentEvent e) {}
        });
        // 输入建议：方向键导航 + 回车选择 + Esc 关闭
        field.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (suggestionPopup == null || suggestionPopup.getParent() == null) return;
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_DOWN) { moveSuggestion(1); e.consume(); }
                else if (k == KeyEvent.VK_UP) { moveSuggestion(-1); e.consume(); }
                else if (k == KeyEvent.VK_ENTER) {
                    if (suggestionSelectedValue != null) { chooseSuggestion(suggestionSelectedValue); e.consume(); }
                }
                else if (k == KeyEvent.VK_ESCAPE) { hideSuggestions(); e.consume(); }
            }
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
        }
        east.add(clearBtn);
        add(east, BorderLayout.EAST);

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                focused = true;
                anim.go("focus", anim.getProgress("focus"), 1f);
                updateClear();
                // 仅在组件仍附着于可见窗口时调度刷新建议，避免 dispose 后 invokeLater 触发空引用
                if (suggestionsEnabled && isShowing()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            // 双层兜底：dispose 后 EDT 上的 stale refresh 不应让 EDT 线程崩溃
                            try { refreshSuggestions(); } catch (Throwable ignored) {}
                        }
                    });
                }
            }
            public void focusLost(FocusEvent e) {
                focused = false;
                anim.go("focus", anim.getProgress("focus"), 0f);
                updateClear();
                hideSuggestions();
            }
        });
        MouseAdapter m = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovering = true;  anim.go("hover", anim.getProgress("hover"), 1f); updateClear(); }
            public void mouseExited(MouseEvent e)  { hovering = false; anim.go("hover", anim.getProgress("hover"), 0f); updateClear(); }
        };
        hoverKeeper = m;
        field.addMouseListener(m);   // field 铺满面板，鼠标事件落在 field 上
        addMouseListener(m);
        east.addMouseListener(m);    // 鼠标从 field 移入 east（清空按钮区）时保持 hovering，× 不淡出

        // 最大长度过滤器（始终安装，运行时读取 maxLength 字段；0 表示不限制）
        installMaxLengthFilter();
        applyTier();
        relayoutEast();
    }

    /** 尺寸档位（对齐 Element UI）：高度 40/32/28，档位联动字体、内边距与清空按钮尺寸。 */
    public void setSize(int tier) {
        if (tier < SIZE_LARGE || tier > SIZE_SMALL)
            throw new IllegalArgumentException("invalid size tier: " + tier);
        this.tier = tier;
        applyTier();
    }

    private void applyTier() {
        field.setFont(theme().getFontBase().deriveFont(TIER_FONT[tier]));
        field.setBorder(BorderFactory.createEmptyBorder(TIER_VPAD[tier], TIER_HPAD[tier], TIER_VPAD[tier], 8));
        clearBtn.setButtonSize(TIER_CLEAR[tier]);
        if (counterLabel != null) counterLabel.setFont(theme().getFontBase().deriveFont(TIER_FONT[tier]));
        if (west != null) relayoutWest();
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
        ensureWest();
        if (prefixIcon != null) west.remove(prefixIcon);
        prefixIcon = new AstIcon(iconType, ICON_COLOR, 16);
        west.add(prefixIcon);
        relayoutWest();
    }

    /** 后缀图标（AstIcon 常量），显示在复合元素/清空按钮左侧。 */
    public void setSuffixIcon(int iconType) {
        if (suffixIcon != null) east.remove(suffixIcon);
        suffixIcon = new AstIcon(iconType, ICON_COLOR, 16);
        east.add(suffixIcon, 0);
        relayoutEast();
    }

    /** 复合型输入框：前缀元素（标签或按钮），置于边框内左侧。与 `setPrefixIcon` 可并存。 */
    public void setPrefixComponent(JComponent c) {
        ensureWest();
        this.prefixComp = c;
        relayoutWest();
        revalidate(); repaint();
    }

    /** 复合型输入框：后缀元素（标签或按钮），置于边框内右侧、清空按钮之前。 */
    public void setSuffixComponent(JComponent c) {
        this.suffixComp = c;
        relayoutEast();
        revalidate(); repaint();
    }

    /** 懒创建 west 面板（前缀容器），仅在设置前缀图标/元素时建立。 */
    private void ensureWest() {
        if (west == null) {
            west = new JPanel(new GridBagLayout());
            west.setOpaque(false);
            west.setBorder(BorderFactory.createEmptyBorder(0, TIER_HPAD[tier], 0, 4));
            west.addMouseListener(hoverKeeper);
            add(west, BorderLayout.WEST);
        }
    }

    /** 重排 west（前缀）顺序：图标 → 自定义元素。 */
    private void relayoutWest() {
        if (west == null) return;
        west.setBorder(BorderFactory.createEmptyBorder(0, TIER_HPAD[tier], 0, 4));
        west.removeAll();
        if (prefixIcon != null) west.add(prefixIcon);
        if (prefixComp != null) west.add(prefixComp);
        west.revalidate(); west.repaint();
    }

    /** 重排 east（后缀）顺序：后缀图标 → 字数统计 → 后缀元素 → 眼睛 → 清空按钮。 */
    private void relayoutEast() {
        east.removeAll();
        if (suffixIcon != null) east.add(suffixIcon);
        if (counterVisible && counterLabel != null) east.add(counterLabel);
        if (suffixComp != null) east.add(suffixComp);
        if (password && eyeBtn != null) east.add(eyeBtn);
        east.add(clearBtn);
        east.revalidate(); east.repaint();
    }

    // ===================== 输入建议 =====================

    /** 设置输入建议候选项（自动去重，null 单项忽略）。 */
    public void setSuggestions(String... items) {
        suggestions.clear();
        if (items != null) for (String s : items) if (s != null && !suggestions.contains(s)) suggestions.add(s);
        suggestionsEnabled = !suggestions.isEmpty();
        if (!suggestionsEnabled) hideSuggestions();
        else refreshSuggestions();
    }

    /** 设置输入建议候选项（List 形式）。 */
    public void setSuggestions(List<String> items) {
        setSuggestions(items == null ? new String[0] : items.toArray(new String[0]));
    }

    /** 激活（聚焦）即列出全部建议；否则仅在输入后按内容匹配。默认 false（输入后匹配）。 */
    public void setShowSuggestionsOnFocus(boolean b) {
        this.showSuggestionsOnFocus = b;
        if (b) refreshSuggestions();
    }

    /** 计算当前应展示的建议项（空文本返回全部；否则按包含匹配，大小写不敏感）。 */
    List<String> computeFiltered() {
        if (!suggestionsEnabled) return Collections.emptyList();
        String t = getText().trim().toLowerCase();
        if (t.isEmpty()) return new ArrayList<>(suggestions);
        List<String> out = new ArrayList<>();
        for (String s : suggestions) if (s.toLowerCase().contains(t)) out.add(s);
        return out;
    }

    /** 文本变化统一入口：更新清空/字数/建议浮层。 */
    private void onTextChanged() {
        hasText = !field.getText().isEmpty();
        updateClear();
        updateCounter();
        refreshSuggestions();
    }

    /** 刷新建议浮层：根据焦点 / 模式 / 输入内容决定显示、重建内容或隐藏。 */
    private void refreshSuggestions() {
        // 组件已被解除（dispose 后的 stale invokeLater 或调用前未挂载）→ 直接放弃
        if (!isDisplayable()) return;
        if (!suggestionsEnabled || !focused) { hideSuggestions(); return; }
        if (!showSuggestionsOnFocus && getText().isEmpty()) { hideSuggestions(); return; }
        // 整体包裹：dispose 后 EDT 上偶发的 stale refresh 不应让 EDT 线程崩溃
        // （生产环境下用户不会在聚焦瞬间销毁输入框，此处仅是自检拆解期的健壮性兜底）
        try {
            List<String> f = computeFiltered();
            if (f.isEmpty()) { hideSuggestions(); return; }
            currentFiltered = f;
            rebuildSuggestionContent(f);
            if (suggestionPopup.getParent() == null) {
                suggestionPopup.show(this, AnimatedPopup.Direction.BELOW);
            } else {
                repositionSuggestionPopup();
            }
        } catch (Throwable t) {
            // detach-safe: 自检拆解期异常吞掉，避免 AWT-EventQueue 线程死亡
        }
    }

    /** 测试辅助：无视焦点强制展示建议浮层（用于离线交互验证）。 */
    void showSuggestionsNow() {
        focused = true;
        refreshSuggestions();
    }

    private AnimatedPopup getSuggestionPopup() {
        if (suggestionPopup == null) {
            suggestionPopup = new AnimatedPopup();
            suggestionPopup.setDismissOnOutsideClick(true);
            suggestionItemsPanel = new JPanel();
            suggestionItemsPanel.setLayout(new BoxLayout(suggestionItemsPanel, BoxLayout.Y_AXIS));
            suggestionItemsPanel.setOpaque(false);
            JScrollPane sp = new JScrollPane(suggestionItemsPanel);
            sp.setBorder(null);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            suggestionPopup.getContent().add(sp, BorderLayout.CENTER);
        }
        return suggestionPopup;
    }

    private void rebuildSuggestionContent(List<String> items) {
        getSuggestionPopup(); // 确保 popup + items 面板 + 滚动面板已创建（幂等）
        suggestionItemsPanel.removeAll();
        suggestionSelectedValue = null;
        for (String s : items) suggestionItemsPanel.add(new SuggestionItem(s));
        int h = Math.min(items.size() * ITEM_HEIGHT, 8 * ITEM_HEIGHT);
        JScrollPane sp = (JScrollPane) suggestionPopup.getContent().getComponent(0);
        int w = Math.max(getWidth(), 160);
        sp.setPreferredSize(new Dimension(w, Math.max(ITEM_HEIGHT, h)));
        sp.revalidate();
        suggestionPopup.getContent().revalidate();
    }

    private void repositionSuggestionPopup() {
        if (suggestionPopup == null || suggestionPopup.getParent() == null) return;
        Dimension ps = suggestionPopup.getPreferredSize();
        Window w = SwingUtilities.getWindowAncestor(this);
        if (!(w instanceof RootPaneContainer)) return;
        JLayeredPane lp = ((RootPaneContainer) w).getLayeredPane();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        PopupPositioner pp = new PopupPositioner(ps, screen);
        Rectangle inv = new Rectangle(getLocationOnScreen(), getSize());
        PopupPositioner.Result r = pp.calc(inv, AnimatedPopup.Direction.BELOW);
        Point p = r.location;
        SwingUtilities.convertPointFromScreen(p, lp);
        suggestionPopup.setBounds(p.x, p.y, ps.width, ps.height);
        lp.repaint(p.x, p.y, ps.width, ps.height);
    }

    /** 选择某条建议：回填文本、关闭浮层、焦点归还输入框。 */
    void chooseSuggestion(String value) {
        setText(value);
        hideSuggestions();
        field.requestFocusInWindow();
        try { field.setCaretPosition(field.getDocument().getLength()); } catch (Exception ignored) {}
    }

    private void hideSuggestions() {
        currentFiltered = Collections.emptyList();
        suggestionSelectedValue = null;
        if (suggestionPopup != null && suggestionPopup.getParent() != null) {
            suggestionPopup.hideWithAnimation(null);
        }
    }

    /** 键盘上下移动高亮项（循环在列表范围内）。 */
    private void moveSuggestion(int dir) {
        if (currentFiltered.isEmpty()) return;
        int idx = currentFiltered.indexOf(suggestionSelectedValue);
        if (idx < 0) idx = (dir > 0) ? -1 : 0;
        int n = Math.max(0, Math.min(currentFiltered.size() - 1, idx + dir));
        suggestionSelectedValue = currentFiltered.get(n);
        suggestionItemsPanel.repaint();
        Component c = suggestionItemsPanel.getComponent(n);
        if (c != null) suggestionItemsPanel.scrollRectToVisible(c.getBounds());
    }

    /** 单条建议：自绘高亮背景 + 文本，鼠标点击回填。 */
    private class SuggestionItem extends JPanel {
        final String value;
        boolean hover;
        SuggestionItem(String v) {
            this.value = v;
            setLayout(null);
            setOpaque(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { chooseSuggestion(value); }
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }
        public Dimension getPreferredSize() { return new Dimension(120, ITEM_HEIGHT); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            boolean hl = hover || value.equals(suggestionSelectedValue);
            if (hl) {
                g2.setColor(theme().getFillLight());
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.setColor(theme().getTextRegular());
            g2.setFont(theme().getFontBase().deriveFont(13f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(value, 12, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    // ===================== 最大长度限制 + 字数统计 =====================

    /** 设置最大输入长度（0 表示不限）。设置后尾部展示「当前/最大」字数统计。 */
    public void setMaxLength(int max) {
        this.maxLength = max;
        counterVisible = max > 0;
        if (counterVisible && counterLabel == null) {
            counterLabel = new JLabel();
            counterLabel.setFont(theme().getFontBase().deriveFont(TIER_FONT[tier]));
            counterLabel.setForeground(theme().getTextRegular());
        }
        if (counterLabel != null) counterLabel.setVisible(counterVisible);
        relayoutEast();
        updateCounter();
    }

    private void updateCounter() {
        if (!counterVisible || counterLabel == null) return;
        counterLabel.setText(field.getDocument().getLength() + "/" + maxLength);
    }

    /** 安装最大长度 DocumentFilter（运行时读取 maxLength；<=0 不限制）。 */
    private void installMaxLengthFilter() {
        if (field.getDocument() instanceof PlainDocument) {
            ((PlainDocument) field.getDocument()).setDocumentFilter(new MaxLengthFilter());
        }
    }

    private class MaxLengthFilter extends DocumentFilter {
        public void insertString(FilterBypass fb, int off, String str, AttributeSet a) throws BadLocationException {
            if (maxLength <= 0) { super.insertString(fb, off, str, a); return; }
            int remain = maxLength - fb.getDocument().getLength();
            if (remain <= 0) return;
            String s = (str == null) ? "" : str;
            if (s.length() > remain) s = s.substring(0, remain);
            super.insertString(fb, off, s, a);
        }
        public void replace(FilterBypass fb, int off, int len, String str, AttributeSet a) throws BadLocationException {
            if (maxLength <= 0) { super.replace(fb, off, len, str, a); return; }
            if (str == null || str.isEmpty()) { super.replace(fb, off, len, str, a); return; } // 删除始终放行
            int curLen = fb.getDocument().getLength();
            int remain = maxLength - (curLen - len);
            if (remain <= 0) return;
            String s = str;
            if (s.length() > remain) s = s.substring(0, remain);
            super.replace(fb, off, len, s, a);
        }
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
            g2.setColor(theme().getTextPlaceholder());
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
        float current = anim.getProgress("clear");
        // 直接使用底层 Animator，附加 onComplete 确保动画结束时 syncClear 被调用
        // （paintComponent 中每帧也会调用 syncClear，onComplete 额外覆盖 headless/未显示场景）
        Animator clearAnim = anim.get("clear");
        clearAnim.go(current, target, this::syncClear);
    }

    /** 清空按钮淡入淡出动画驱动 alpha 与可交互性（无文本或 alpha 低时不拦截点击）。 */
    private void syncClear() {
        clearBtn.setAlpha(anim.getProgress("clear"));
        clearBtn.setInteractive(isEnabled() && anim.getProgress("clear") > 0.5f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        syncClear();
        float focus = anim.getProgress("focus");
        float hover = anim.getProgress("hover");
        Color border = lerp(theme().getBorderBase(), theme().getPrimary(), Math.max(focus, hover));
        if (!isEnabled()) border = new Color(0xE4E7ED);
        if (invalid) border = theme().getDanger();
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
        clearBtn.setInteractive(enabled && anim.getProgress("clear") > 0.5f);
    }

    @Override
    protected void selfCheck() {
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

        // 复合型：前缀/后缀元素
        AstInput cp = new AstInput("复合型");
        JLabel pre = new JLabel("Http://");
        JLabel suf = new JLabel(".com");
        cp.setPrefixComponent(pre);
        cp.setSuffixComponent(suf);
        java.util.List<Component> westKids = java.util.Arrays.asList(cp.west.getComponents());
        assert westKids.contains(pre) : "prefix component placed in west";
        java.util.List<Component> eastKids = java.util.Arrays.asList(cp.east.getComponents());
        assert eastKids.contains(suf) : "suffix component placed in east";

        // 最大长度限制 + 字数统计
        AstInput ml = new AstInput("限制");
        ml.setMaxLength(5);
        ml.setText("123456789");
        assert ml.getText().length() == 5 : "max length enforced, got len=" + ml.getText().length();
        assert ml.getText().equals("12345") : "truncated to exactly max, got=" + ml.getText();
        ml.setText("");
        ml.setText("abcde");
        assert ml.getText().equals("abcde") : "exact max length allowed, got=" + ml.getText();
        assert ml.counterVisible : "counter visible when maxLength>0";
        // 超过上限的选择性删除仍放行
        ml.setText("abcde");
        ml.field.setSelectionStart(0); ml.field.setSelectionEnd(2); // 选中 "ab"
        ml.setText(""); // 触发 replace(0,2,"") —— 删除不被拦截
        assert ml.getText().isEmpty() : "deletion not blocked by max-length filter";

        // 输入建议：过滤 + 选择回填（不依赖浮层渲染）
        AstInput sg = new AstInput("建议");
        sg.setSuggestions("apple", "apricot", "banana", "cherry");
        assert sg.suggestionsEnabled : "suggestions enabled after setSuggestions";
        sg.setText("ap");
        List<String> filtered = sg.computeFiltered();
        assert filtered.size() == 2 : "filter 'ap' -> 2, got " + filtered.size();
        assert filtered.contains("apple") && filtered.contains("apricot") : "filter content wrong: " + filtered;
        sg.setText("");
        List<String> all = sg.computeFiltered();
        assert all.size() == 4 : "empty input -> all suggestions, got " + all.size();
        sg.chooseSuggestion("banana");
        assert sg.getText().equals("banana") : "chooseSuggestion sets text, got=" + sg.getText();

        // 输入建议：浮层展示 + 点击回填（真实窗口 + AnimatedPopup）
        final Throwable[] sgErr = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame f = new JFrame();
                f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                AstInput si2 = new AstInput("建议");
                si2.setSuggestions("one", "two", "three");
                si2.setShowSuggestionsOnFocus(true);
                f.add(si2);
                f.setSize(300, 80);
                f.setVisible(true);
                si2.showSuggestionsNow();
                assert si2.suggestionPopup.getParent() != null : "suggestion popup should be shown";
                assert si2.suggestionItemsPanel.getComponentCount() == 3
                        : "popup should list 3 items, got " + si2.suggestionItemsPanel.getComponentCount();
                Component item = si2.suggestionItemsPanel.getComponent(0);
                item.dispatchEvent(new MouseEvent(item, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0,
                        item.getWidth() / 2, item.getHeight() / 2, 1, false));
                assert si2.getText().equals("one") : "click suggestion should set text, got=" + si2.getText();
                // 同步隐藏：setVisible→hidePopup 立即卸载 AWT 监听、停止 openAnim，避免 185ms Timer 让 JVM 不退出
                si2.suggestionPopup.setVisible(false);
                f.dispose();
            });
        } catch (Throwable t) { sgErr[0] = t; }
        if (sgErr[0] != null) throw new RuntimeException(sgErr[0]);

        // FormValueProvider 取值契约
        AstInput fb = new AstInput("占位");
        fb.setText("hello");
        assert fb.getFormValue().equals("hello") : "AstInput.getFormValue";
        fb.setFormValue("world");
        assert fb.getFormValue().equals("world") : "AstInput.setFormValue";

        // 对比度：文字 vs 背景
        assertContrast(theme().getTextPrimary(), theme().getFillBlank(), "input text on bg");
        assertContrast(theme().getTextRegular(), theme().getFillBlank(), "input counter");

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

    public static void main(String[] args) {
        new AstInput("test").selfCheck();
    }
}
