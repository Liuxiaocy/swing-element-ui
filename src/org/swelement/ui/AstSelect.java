package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.AnimatedPopup;
import org.swelement.core.Easing;
import org.swelement.framework.AstAbstractComponent;
import org.swelement.framework.AstInteractiveComponent;

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

public class AstSelect extends AstAbstractComponent implements FormValueProvider, FormInvalidMarker {
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
    private boolean popupShown, fieldFocus;
    // 可清空：hover 淡入手绘 ×（点击清空），隐藏时替换箭头 —— 不用 AstCloseButton（会与上下箭头重叠）
    private boolean hovering;
    private boolean invalid = false;

    // --- 尺寸档位（对齐 Element UI，与 AstInput 一致）---
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    private static final int[] TIER_HEIGHT = {40, 32, 28};
    private static final float[] TIER_FONT = {14f, 13f, 12f};
    /** 档位 → 多选标签尺寸：档位越高标签越大，小档位统一取 SMALL 以免撑破输入框高度。 */
    private static final int[] TIER_TAG = {AstTag.SIZE_DEFAULT, AstTag.SIZE_SMALL, AstTag.SIZE_SMALL};
    /** 多选已选项标签配色：INFO + light（浅灰底深灰字，与输入框白底协调且对比度达标）。 */
    private static final int TAG_TYPE = AstTag.INFO, TAG_EFFECT = AstTag.EFFECT_LIGHT;
    private int tier = SIZE_DEFAULT;

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("arrow", 200, Easing::easeInOut);
        anim.register("clear", 150, Easing::easeInOut);
    }

    public AstSelect(boolean multiple, boolean filterable) {
        this.multiple = multiple;
        this.filterable = filterable;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(220, 40));

        field = filterable ? new JTextField() : null;
        center.setOpaque(false);
        if (multiple) center.add(tagsPanel, BorderLayout.NORTH);
        if (filterable) {
            field.setOpaque(false);
            field.setBorder(new EmptyBorder(0, 12, 0, 0));
            field.setFont(theme().getFontBase());
            field.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { fieldFocus = true; repaint(); }
                public void focusLost(FocusEvent e) { fieldFocus = false; repaint(); }
            });
            center.add(field, BorderLayout.CENTER);
        } else {
            display.setOpaque(false);
            display.setBorder(new EmptyBorder(0, 12, 0, 0));
            display.setFont(theme().getFontBase());
            center.add(display, BorderLayout.CENTER);
        }
        applyTier();
        add(center, BorderLayout.CENTER);

        // 可清空：hover 淡入手绘 ×（点击清空），隐藏时替换箭头 —— 不用 AstCloseButton（避免与箭头重叠）
        JPanel east = new JPanel(new GridBagLayout());
        east.setOpaque(false);
        east.setBorder(new EmptyBorder(0, 4, 0, 8));
        add(east, BorderLayout.EAST);

        MouseAdapter hoverM = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                updateClear();
            }
            public void mouseExited(MouseEvent e)  {
                hovering = false;
                updateClear();
            }
        };
        addMouseListener(hoverM); display.addMouseListener(hoverM); tagsPanel.addMouseListener(hoverM); east.addMouseListener(hoverM);
        if (field != null) field.addMouseListener(hoverM);
        // 统一按下处理：命中 × 清空，否则展开下拉。所有区域经坐标转换后走同一 handlePress
        //（避免 AstCloseButton 与箭头重叠：事件不会被重复 toggle，也不会点不到）。
        installPress(this);
        installPress(east);
        installPress(display);
        installPress(tagsPanel);
        if (field != null) installPress(field);

        optionList.setOpaque(false);
        optionList.setLayout(new BoxLayout(optionList, BoxLayout.Y_AXIS));
        popup.getContent().add(optionList, BorderLayout.CENTER);
        popup.setDismissListener(() -> {
            if (popupShown) {
                popupShown = false;
                anim.go("arrow", anim.getProgress("arrow"), 0f);
                repaint();
            }
        });
    }

    /** 尺寸档位（对齐 Element UI）：高度 40/32/28，档位联动字体与清空按钮尺寸。 */
    public void setSize(int t) {
        if (t < SIZE_LARGE || t > SIZE_SMALL) throw new IllegalArgumentException("invalid size tier: " + t);
        this.tier = t;
        applyTier();
        revalidate();
        repaint();
    }

    private void applyTier() {
        if (display != null) display.setFont(theme().getFontBase().deriveFont(TIER_FONT[tier]));
        if (field != null) field.setFont(theme().getFontBase().deriveFont(TIER_FONT[tier]));
        // AstTag 按自身 size 档位取字号，改档位而非 setFont（setFont 对自绘标签无效）
        for (Component c : tagsPanel.getComponents()) {
            if (c instanceof AstTag) ((AstTag) c).setSize(TIER_TAG[tier]);
        }
    }

    /** 启用/禁用联动到多选标签：禁用态标签灰化且 × 不可点（由 AstTag.setEnabled 处理）。 */
    @Override
    public void setEnabled(boolean en) {
        super.setEnabled(en);
        if (tagsPanel != null) {
            for (Component c : tagsPanel.getComponents()) c.setEnabled(en);
            updateClear();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        // 多选带标签时自然高度已含标签行，取较大值避免裁剪；其余强制档位高度
        int h = TIER_HEIGHT[tier];
        if (multiple && tagsPanel.getComponentCount() > 0) h = Math.max(d.height, h);
        else d.height = h;
        return new Dimension(d.width, h);
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

    /** 当前多选标签（按已选顺序）。测试与自检用。 */
    List<AstTag> getTags() {
        List<AstTag> out = new ArrayList<>();
        for (Component c : tagsPanel.getComponents()) {
            if (c instanceof AstTag) out.add((AstTag) c);
        }
        return out;
    }

    static boolean matches(String label, String filter) {
        return label.toLowerCase().contains(filter.toLowerCase());
    }

    /** 清空单选选择（对应手绘 × 的点击行为，含下拉重建与重绘）。 */
    private void clearOnClick() {
        if (multiple) return;
        selected.clear();
        updateDisplay();
        rebuildList(null);
        repaint();
    }

    /** 命中测试：给定组件坐标，是否落在可清空 ×（区分于展开箭头）区域内。 */
    private boolean isClearHit(int x, int y) {
        if (multiple || selected.isEmpty() || !isEnabled()) return false;
        if (anim.getProgress("clear") < 0.5f) return false;
        float cx = getWidth() - 18f, cy = getHeight() / 2f;
        return Math.hypot(x - cx, y - cy) <= 7;
    }

    /** 统一的按下处理：命中 × 则清空，否则展开下拉。 */
    private void handlePress(int x, int y) {
        if (!isEnabled()) return;
        if (isClearHit(x, y)) clearOnClick();
        else togglePopup();
    }

    /** 给指定子区域安装统一按下监听：坐标转换到 AstSelect 后再走 handlePress。 */
    private void installPress(Component c) {
        c.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                Point p = (c == AstSelect.this) ? e.getPoint()
                        : SwingUtilities.convertPoint(c, e.getPoint(), AstSelect.this);
                handlePress(p.x, p.y);
            }
        });
    }

    private void updateClear() {
        float target = (!multiple && !selected.isEmpty() && hovering && isEnabled()) ? 1f : 0f;
        anim.get("clear").go(anim.getProgress("clear"), target, this::repaint);
    }

    private void togglePopup() {
        if (popupShown) {
            popup.setVisible(false);
            popupShown = false;
            anim.go("arrow", anim.getProgress("arrow"), 0f);
            repaint();
        } else {
            rebuildList(filterable ? field.getText() : null);
            popup.getContent().setPreferredSize(new Dimension(Math.max(180, getWidth()), popup.getContent().getPreferredSize().height));
            popup.show(this, 0, getHeight());
            popupShown = true;
            anim.go("arrow", anim.getProgress("arrow"), 1f);
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
                g.setFont(theme().getFontBase().deriveFont(Font.PLAIN, 12f));
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
            anim.go("arrow", anim.getProgress("arrow"), 0f);
        }
        updateDisplay();
        rebuildList(null);
        repaint();
    }

    private void updateDisplay() {
        tagsPanel.removeAll();
        if (multiple) {
            for (Option o : selected) tagsPanel.add(createTag(o));
        } else {
            display.setText(selected.isEmpty() ? "" : selected.get(0).label);
        }
        tagsPanel.revalidate();
        tagsPanel.repaint();
        updateClear();
    }

    /**
     * 构造多选已选项的标签（AstTag 可关闭）。
     *
     * <p>点击交互靠 AWT 事件重定向天然分层，无需额外命中测试：
     * <ul>
     *   <li>点 × → 事件落在 {@code AstCloseButton}（自带监听器）→ 只移除该项，不展开下拉；</li>
     *   <li>点标签主体 → AstTag 自身无鼠标监听器，事件上溯到 {@code tagsPanel} →
     *       {@link #handlePress} → 展开下拉。</li>
     * </ul>
     */
    private AstTag createTag(Option o) {
        AstTag tag = new AstTag(o.label, TAG_TYPE, true);
        tag.setEffect(TAG_EFFECT);
        tag.setSize(TIER_TAG[tier]);
        tag.setEnabled(isEnabled());
        tag.setOnClosed(() -> {
            selected.remove(o);
            updateDisplay();
            rebuildList(null);
            repaint();
        });
        return tag;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        float arrowAngle = anim.getProgress("arrow");
        boolean highlighted = popupShown || fieldFocus;
        Color border = isEnabled() ? (highlighted ? theme().getPrimary() : new Color(0xDCDFE6)) : new Color(0xE4E7ED);
        if (invalid) border = theme().getDanger();
        int radius = 8; // hardcoded for now (Element UI select has fixed 8px radius, slightly different from base)
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.setColor(isEnabled() ? Color.WHITE : theme().getFillBase());
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(highlighted ? 2f : 1f));
        g2.draw(shape);

        float clearP = anim.getProgress("clear");
        float ax = getWidth() - 18f, ay = getHeight() / 2f;
        if (clearP < 0.5f) {
            Graphics2D a2 = (Graphics2D) g2.create();
            a2.rotate(Math.PI * arrowAngle, ax, ay);
            a2.setColor(new Color(0xC0C4CC));
            a2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            a2.drawLine(Math.round(ax - 4), Math.round(ay - 1), Math.round(ax), Math.round(ay + 2));
            a2.drawLine(Math.round(ax + 4), Math.round(ay - 1), Math.round(ax), Math.round(ay + 2));
            a2.dispose();
        } else {
            // 手绘 ×：替代 AstCloseButton（避免与上下箭头重叠、遮挡点击），随 clear 进度淡入
            int alphaInt = Math.min(255, Math.round((clearP - 0.5f) * 2f * 255f));
            Graphics2D c2 = (Graphics2D) g2.create();
            c2.setColor(new Color(0x90, 0x93, 0x99, alphaInt));
            c2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            c2.drawLine(Math.round(ax - 3), Math.round(ay - 3), Math.round(ax + 3), Math.round(ay + 3));
            c2.drawLine(Math.round(ax + 3), Math.round(ay - 3), Math.round(ax - 3), Math.round(ay + 3));
            c2.dispose();
        }

        g2.dispose();
    }

    private class OptionRow extends AstInteractiveComponent {
        private final Option option;

        OptionRow(Option o) {
            this.option = o;
            setPreferredSize(new Dimension(Math.max(180, AstSelect.this.getWidth()), 32));
            if (option.disabled) setEnabled(false);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { if (!option.disabled) choose(option); }
            });
        }

        @Override
        protected boolean isToggleMode() {
            return false;
        }

        @Override
        protected void selfCheck() {
            // 内部类，自检由外部类负责
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            if (!option.disabled) {
                g2.setColor(lerp(Color.WHITE, new Color(0xF5F7FA), hoverProgress()));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            boolean isSel = selected.contains(option);
            g2.setFont(theme().getFontBase());
            g2.setColor(option.disabled ? new Color(0xC0C4CC)
                    : (isSel ? theme().getPrimary() : theme().getTextRegular()));
            FontMetrics fm = g2.getFontMetrics();
            String text = option.label + (multiple && isSel ? "  \u221a" : "");
            g2.drawString(text, 12, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    @Override
    protected void selfCheck() {
        assert matches("Apple", "app");
        assert matches("Apple", "APPLE");
        assert matches("苹果", "苹");
        assert !matches("Apple", "pear");
        assert !matches("", "a");

        // 对比度
        assertContrast(theme().getTextRegular(), Color.WHITE, "select text on white");

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

        // --- 多选：已选项用 AstTag 展示（P2.2）---
        final AstSelect mt = new AstSelect(true, false);
        mt.addOption(new AstSelect.Option("北京", "bj"));
        mt.addOption(new AstSelect.Option("上海", "sh"));
        mt.addOption(new AstSelect.Option("广州", "gz"));
        mt.setFormValue("bj,sh,gz");
        final Throwable[] err3 = {null};
        final int[] tagH = {-1, -1};
        final JFrame[] holder = {null};
        final boolean[] tagsDisabled = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame f = new JFrame();
                JPanel p = new JPanel(new BorderLayout());
                p.add(mt);
                f.add(p);
                f.pack();
                holder[0] = f;

                // 三个已选项 → 三个 AstTag，文案与顺序一致
                List<AstTag> tags = mt.getTags();
                assert tags.size() == 3 : "3 selected -> 3 AstTag, got " + tags.size();
                assert "北京".equals(tags.get(0).getText()) && "广州".equals(tags.get(2).getText())
                        : "tag order/label mismatch, got " + tags.get(0).getText() + "..." + tags.get(2).getText();
                // 可关闭：挂载后带 AstCloseButton 子组件
                assert tags.get(0).getComponentCount() == 1 && tags.get(0).getComponent(0) instanceof AstCloseButton
                        : "closable AstTag should own an AstCloseButton, count=" + tags.get(0).getComponentCount();

                // 档位联动：LARGE 档标签 > SMALL 档标签高度
                mt.setSize(AstSelect.SIZE_LARGE);
                tagH[0] = mt.getTags().get(0).getPreferredSize().height;
                mt.setSize(AstSelect.SIZE_SMALL);
                tagH[1] = mt.getTags().get(0).getPreferredSize().height;
                mt.setSize(AstSelect.SIZE_DEFAULT);

                // 禁用联动：Select 禁用 → 标签禁用（× 灰化不可点）
                mt.setEnabled(false);
                tagsDisabled[0] = !mt.getTags().get(0).isEnabled();
                mt.setEnabled(true);

                // 点第一个标签的 × → 关闭动画结束后只移除该项（不展开下拉、不影响其余项）
                Component cb = mt.getTags().get(0).getComponent(0);
                cb.dispatchEvent(new MouseEvent(cb, MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(), 0, cb.getWidth() / 2, cb.getHeight() / 2, 1, false));
            });
            Thread.sleep(400); // 等 ~200ms 关闭动画完成
        } catch (Throwable t) { err3[0] = t; }
        finally {
            // 断言失败也必须 dispose，否则 JVM 因残留非 daemon 的 AWT 线程挂死（掩盖真实错误）
            try { SwingUtilities.invokeAndWait(() -> { if (holder[0] != null) { holder[0].dispose(); holder[0] = null; } }); }
            catch (Throwable ignored) { /* 不掩盖原始断言错误 */ }
        }
        if (err3[0] != null) throw new RuntimeException(err3[0]);
        assert tagH[0] > tagH[1] : "LARGE tier tag taller than SMALL, got " + tagH[0] + " vs " + tagH[1];
        assert tagsDisabled[0] : "disabled select should disable its tags";
        assert "sh,gz".equals(mt.getFormValue())
                : "removing first tag leaves sh,gz, got " + mt.getFormValue();
        assert mt.getTags().size() == 2 : "2 tags remain after removing one, got " + mt.getTags().size();

        // 尺寸档位（手绘 × 清空方案：Select 自身清空用首绘 ×，不含 AstCloseButton 子组件）
        AstSelect sz = new AstSelect(false, false);
        assert !hasCloseButton(sz) : "Select uses hand-drawn ×, not an AstCloseButton";
        sz.setSize(AstSelect.SIZE_SMALL);
        assert sz.getPreferredSize().height == 28 : "Select SMALL height 28, got " + sz.getPreferredSize().height;
        sz.setSize(AstSelect.SIZE_LARGE);
        assert sz.getPreferredSize().height == 40 : "Select LARGE height 40, got " + sz.getPreferredSize().height;
        sz.setSize(AstSelect.SIZE_DEFAULT);
        assert sz.getPreferredSize().height == 32 : "Select DEFAULT height 32, got " + sz.getPreferredSize().height;
        boolean t2 = false;
        try { sz.setSize(9); } catch (IllegalArgumentException e) { t2 = true; }
        assert t2 : "Select invalid tier throws";

        System.out.println("AstSelect self-check OK");
    }

    /** 测试辅助：在手绘 × 命中区域派发按下事件（等价于点击清空）。 */
    private static void clearBtnClickForTest(AstSelect sel) {
        sel.handlePress(sel.getWidth() - 18, sel.getHeight() / 2);
    }

    /**
     * 结构性校验：Select 自身的清空用首绘 ×（避免与上下箭头重叠），
     * 故组件树里不应出现 AstCloseButton —— 但**多选已选项的 AstTag 内部各有一个**，
     * 所以本断言只对单选实例成立（调用方必须传单选 Select）。
     */
    private static boolean hasCloseButton(java.awt.Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof AstCloseButton) return true;
            if (c instanceof java.awt.Container && hasCloseButton((java.awt.Container) c)) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        new AstSelect(new String[]{"test"}).selfCheck();
    }
}
