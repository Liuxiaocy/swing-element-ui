package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.theme.Theme;
import org.swelement.framework.AstDisplayComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class AstTag extends AstDisplayComponent {
    public static final int PRIMARY = 0, SUCCESS = 1, WARNING = 2, DANGER = 3, INFO = 4;
    public static final int EFFECT_DARK = 0, EFFECT_LIGHT = 1, EFFECT_PLAIN = 2;
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;

    private static final float[] SIZE_FONT = {14f, 12f, 12f};
    private static final int[] SIZE_VPAD = {8, 4, 2};
    private static final int[] SIZE_HPAD = {16, 10, 8};
    private static final int[] CLOSE_SIZE = {16, 14, 12};
    private static final int CLOSE_GAP = 4;
    private static final int CLOSE_RIGHT = 6;

    private static final Color[] LIGHT_BG = {new Color(0xECF5FF), new Color(0xF0F9EB), new Color(0xFDF6EC), new Color(0xFEF0F0), new Color(0xF4F4F5)};
    private static final Color[] LIGHT_BORDER = {new Color(0xD9ECFF), new Color(0xE1F3D8), new Color(0xFAECD8), new Color(0xFDE2E2), new Color(0xE9E9EB)};
    // 深色文字变体，浅色/白底上对比度 >= 4.5:1（取值同 Button PLAIN_FG）
    private static final Color[] DEEP_FG = {new Color(0x1d6fb5), new Color(0x2d6b18), new Color(0x955d12), new Color(0xb83232), new Color(0x606266)};

    private Runnable onClosed;
    private int origW, origH;
    private int effect = EFFECT_LIGHT;
    private int size = SIZE_DEFAULT;
    private AstCloseButton closeBtn;

    private final int type;
    private final boolean closable;
    private String text;

    public AstTag(String text, int type, boolean closable) {
        this.text = text;
        this.type = type;
        this.closable = closable;
        setLayout(null); // AstCloseButton 绝对定位，doLayout 摆放
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("close", 200, Easing::easeInOut);
    }

    /** 深色背景色（effect=DARK），从主题获取语义色。 */
    private Color darkBg(int type) {
        Theme t = theme();
        switch (type) {
            case PRIMARY: return t.getPrimary();
            case SUCCESS: return t.getSuccess();
            case WARNING: return t.getWarning();
            case DANGER: return t.getDanger();
            default: return t.getInfo();
        }
    }

    public void setEffect(int effect) {
        this.effect = effect;
        updateCloseColors();
        repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (closeBtn != null) closeBtn.setEnabled(enabled); // 禁用态：关闭 × 灰化且不可点
    }

    public void setSize(int size) {
        this.size = size;
        revalidate();
        repaint();
    }

    /** × 点击关闭动画完成后的回调（由 Demo 用于从容器移除）。 */
    public void setOnClosed(Runnable r) { this.onClosed = r; }

    public void setText(String t) {
        text = t;
        revalidate();
        repaint();
    }

    public String getText() { return text; }

    public void close(Runnable onClosed) {
        this.onClosed = onClosed;
        origW = getPreferredSize().width;
        origH = getPreferredSize().height;
        if (closeBtn != null) {
            closeBtn.setInteractive(false);
            closeBtn.setVisible(false);
        }
        anim.get("close").go(0f, 1f, () -> {
            if (this.onClosed != null) {
                Runnable r = this.onClosed;
                this.onClosed = null;
                r.run();
            }
        });
    }

    private void updateCloseColors() {
        if (closeBtn == null) return;
        if (effect == EFFECT_DARK) {
            // 白色 × 在彩色实底上 —— Element 标准实心设计，对比度为例外（见 spec 标注）
            closeBtn.setColor(Color.WHITE);
            closeBtn.setHoverColor(Color.WHITE);
        } else {
            // light: 深色变体 × 对应浅色底；plain: 深色变体 × 白底。hover 统一 TEXT_PRIMARY
            closeBtn.setColor(DEEP_FG[type]);
            closeBtn.setHoverColor(theme().getTextPrimary());
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (closable && closeBtn == null) {
            closeBtn = new AstCloseButton(CLOSE_SIZE[size]);
            closeBtn.addActionListener(e -> close(onClosed != null ? onClosed : (Runnable) () -> {}));
            add(closeBtn);
            updateCloseColors();
            revalidate();
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (closeBtn != null && closeBtn.isVisible()) {
            int s = CLOSE_SIZE[size];
            closeBtn.setBounds(getWidth() - CLOSE_RIGHT - s, (getHeight() - s) / 2, s, s);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        // 关闭动画：根据进度收缩 preferredSize
        float closeProgress = anim.getProgress("close");
        if (closeProgress > 0f && origW > 0) {
            float w = origW * (1 - closeProgress);
            setPreferredSize(new Dimension(Math.max(1, Math.round(w)), origH));
            revalidate();
        }
        Color bg, fg, border;
        switch (effect) {
            case EFFECT_DARK:
                bg = darkBg(type); fg = Color.WHITE; border = darkBg(type); // 白字彩底：Element 标准实心，对比度例外
                break;
            case EFFECT_PLAIN:
                bg = Color.WHITE; fg = DEEP_FG[type]; border = darkBg(type);
                break;
            default: // EFFECT_LIGHT（默认，向后兼容）
                bg = LIGHT_BG[type]; fg = DEEP_FG[type]; border = LIGHT_BORDER[type];
                break;
        }
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
        g2.setColor(fg);
        Font f = theme().getFontBase().deriveFont(SIZE_FONT[size]);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics(f);
        int rightInset = closable ? CLOSE_GAP + CLOSE_SIZE[size] + CLOSE_RIGHT : SIZE_HPAD[size];
        Shape oldClip = g2.getClip();
        g2.clipRect(0, 0, getWidth() - rightInset, getHeight()); // 文字不与 AstCloseButton 重叠
        g2.drawString(text, SIZE_HPAD[size], (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g2.setClip(oldClip);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) return super.getPreferredSize();
        Font f = theme().getFontBase().deriveFont(SIZE_FONT[size]);
        FontMetrics fm = getFontMetrics(f);
        int w = SIZE_HPAD[size] + fm.stringWidth(text)
                + (closable ? CLOSE_GAP + CLOSE_SIZE[size] + CLOSE_RIGHT : SIZE_HPAD[size]);
        int h = Math.max(SIZE_VPAD[size] * 2 + fm.getHeight(), CLOSE_SIZE[size] + 8);
        return new Dimension(w, h);
    }

    @Override
    protected void selfCheck() {
        // 对比度：light 与 plain 各 type 深色文字变体 vs 对应背景
        for (int t = 0; t < 5; t++) {
            assertContrast(DEEP_FG[t], LIGHT_BG[t], "tag light type=" + t);
            assertContrast(DEEP_FG[t], Color.WHITE, "tag plain type=" + t);
            assertContrast(theme().getTextPrimary(), LIGHT_BG[t], "tag hover-x on light type=" + t);
        }
        // 显式 setPreferredSize 必须被尊重（close 收缩动画依赖此：Animator 通过 setPreferredSize 驱动）
        AstTag shrink = new AstTag("标签", AstTag.PRIMARY, false);
        shrink.setPreferredSize(new Dimension(1, 26));
        assert shrink.getPreferredSize().equals(new Dimension(1, 26))
                : "explicitly-set preferred size must be honored by getPreferredSize";
        // 可关闭 AstTag 更宽（为 AstCloseButton 预留）
        AstTag plain = new AstTag("标签", AstTag.PRIMARY, false);
        AstTag closable = new AstTag("标签", AstTag.PRIMARY, true);
        assert closable.getPreferredSize().width > plain.getPreferredSize().width
                : "closable tag must reserve width for close button";
        // effect 切换不抛异常
        closable.setEffect(AstTag.EFFECT_DARK);
        closable.setEffect(AstTag.EFFECT_PLAIN);
        closable.setEffect(AstTag.EFFECT_LIGHT);
        // 尺寸三档高度递减
        AstTag l = new AstTag("尺寸", AstTag.INFO, false); l.setSize(AstTag.SIZE_LARGE);
        AstTag d = new AstTag("尺寸", AstTag.INFO, false);
        AstTag s = new AstTag("尺寸", AstTag.INFO, false); s.setSize(AstTag.SIZE_SMALL);
        assert l.getPreferredSize().height > d.getPreferredSize().height : "large > default height";
        assert d.getPreferredSize().height > s.getPreferredSize().height : "default > small height";
        // 加入窗口后 AstCloseButton 子组件存在且位于右侧
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame f = new JFrame();
                JPanel p = new JPanel();
                AstTag c = new AstTag("可关闭", AstTag.SUCCESS, true);
                p.add(c);
                f.add(p);
                f.pack();
                assert c.getComponentCount() == 1 && c.getComponent(0) instanceof AstCloseButton
                        : "close button child present, count=" + c.getComponentCount();
                Component cb = c.getComponent(0);
                assert cb.getX() + cb.getWidth() <= c.getWidth() && cb.getX() > c.getWidth() / 2
                        : "close button on right side, x=" + cb.getX();
                // 禁用 AstTag → 关闭按钮灰化且不可点（setEnabled 联动）
                c.setEnabled(false);
                assert !cb.contains(cb.getWidth() / 2, cb.getHeight() / 2) : "disabled AstTag → close button not clickable";
                c.setEnabled(true);
                f.dispose();
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        // 点击 AstCloseButton 触发关闭动画并回调 onClosed（primary close 路径）
        // 注意：基类 removeNotify 会 dispose 动画管理器，故需在动画完成后再 dispose 窗口
        final Throwable[] err2 = {null};
        final boolean[] closed = {false};
        final JFrame[] holder = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame f = new JFrame();
                holder[0] = f;
                JPanel p = new JPanel();
                AstTag c = new AstTag("可关闭", AstTag.SUCCESS, true);
                c.setOnClosed(() -> closed[0] = true);
                p.add(c);
                f.add(p);
                f.pack();
                Component cb = c.getComponent(0);
                cb.dispatchEvent(new MouseEvent(cb, MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(), 0, cb.getWidth() / 2, cb.getHeight() / 2, 1, false));
            });
            Thread.sleep(400); // 等待 ~200ms 关闭动画完成
            if (holder[0] != null) holder[0].dispose();
        } catch (Throwable t) { err2[0] = t; }
        if (err2[0] != null) throw new RuntimeException(err2[0]);
        assert closed[0] : "clicking close button should fire onClosed after close animation";
        System.out.println("AstTag self-check OK");
    }

    public static void main(String[] args) {
        new AstTag("test", PRIMARY, false).selfCheck();
    }
}
