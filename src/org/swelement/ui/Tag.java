package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class Tag extends JComponent {
    public static final int PRIMARY = 0, SUCCESS = 1, WARNING = 2, DANGER = 3, INFO = 4;
    public static final int EFFECT_DARK = 0, EFFECT_LIGHT = 1, EFFECT_PLAIN = 2;
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;

    private static final float[] SIZE_FONT = {14f, 12f, 12f};
    private static final int[] SIZE_VPAD = {8, 4, 2};
    private static final int[] SIZE_HPAD = {16, 10, 8};
    private static final int[] CLOSE_SIZE = {20, 18, 16};
    private static final int CLOSE_GAP = 4;
    private static final int CLOSE_RIGHT = 6;

    private static final Color[] LIGHT_BG = {new Color(0xECF5FF), new Color(0xF0F9EB), new Color(0xFDF6EC), new Color(0xFEF0F0), new Color(0xF4F4F5)};
    private static final Color[] LIGHT_BORDER = {new Color(0xD9ECFF), new Color(0xE1F3D8), new Color(0xFAECD8), new Color(0xFDE2E2), new Color(0xE9E9EB)};
    private static final Color[] DARK_BG = {ElementTheme.PRIMARY, ElementTheme.SUCCESS, ElementTheme.WARNING, ElementTheme.DANGER, ElementTheme.INFO};
    // 深色文字变体，浅色/白底上对比度 >= 4.5:1（取值同 Button PLAIN_FG）
    private static final Color[] DEEP_FG = {new Color(0x1d6fb5), new Color(0x2d6b18), new Color(0x955d12), new Color(0xb83232), new Color(0x606266)};

    private Runnable onClosed;
    private int origW, origH;
    private int effect = EFFECT_LIGHT;
    private int size = SIZE_DEFAULT;
    private CloseButton closeBtn;

    private final Animator closeAnim = new Animator(200, Easing::easeInOut, v -> {
        float w = origW * (1 - v);
        setPreferredSize(new Dimension(Math.max(1, Math.round(w)), origH));
        revalidate();
        if (v >= 1f && onClosed != null) {
            Runnable r = onClosed;
            onClosed = null;
            r.run();
        }
        repaint();
    });
    private final int type;
    private final boolean closable;
    private String text;

    public Tag(String text, int type, boolean closable) {
        this.text = text;
        this.type = type;
        this.closable = closable;
        setOpaque(false);
        setLayout(null); // CloseButton 绝对定位，doLayout 摆放
    }

    public void setEffect(int effect) {
        this.effect = effect;
        updateCloseColors();
        repaint();
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
        origW = getWidth();
        origH = getHeight();
        if (closeBtn != null) {
            closeBtn.setInteractive(false);
            closeBtn.setVisible(false);
        }
        closeAnim.go(0f, 1f);
    }

    private void updateCloseColors() {
        if (closeBtn == null) return;
        if (effect == EFFECT_DARK) {
            // 白色 × 在彩色实底上 —— Element 标准实心设计，对比度为例外（见 spec 标注）
            closeBtn.setColor(Color.WHITE);
            closeBtn.setHoverColor(Color.WHITE);
        } else {
            // light: 深色变体 × 对应浅色底；plain: 深色变体 × 白底。hover 统一 TEXT_MAIN
            closeBtn.setColor(DEEP_FG[type]);
            closeBtn.setHoverColor(ElementTheme.TEXT_MAIN);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (closable && closeBtn == null) {
            closeBtn = new CloseButton(CLOSE_SIZE[size]);
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
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg, fg, border;
        switch (effect) {
            case EFFECT_DARK:
                bg = DARK_BG[type]; fg = Color.WHITE; border = DARK_BG[type]; // 白字彩底：Element 标准实心，对比度例外
                break;
            case EFFECT_PLAIN:
                bg = Color.WHITE; fg = DEEP_FG[type]; border = DARK_BG[type];
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
        Font f = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics(f);
        int rightInset = closable ? CLOSE_GAP + CLOSE_SIZE[size] + CLOSE_RIGHT : SIZE_HPAD[size];
        Shape oldClip = g2.getClip();
        g2.clipRect(0, 0, getWidth() - rightInset, getHeight()); // 文字不与 CloseButton 重叠
        g2.drawString(text, SIZE_HPAD[size], (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g2.setClip(oldClip);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) return super.getPreferredSize();
        Font f = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        FontMetrics fm = getFontMetrics(f);
        int w = SIZE_HPAD[size] + fm.stringWidth(text)
                + (closable ? CLOSE_GAP + CLOSE_SIZE[size] + CLOSE_RIGHT : SIZE_HPAD[size]);
        int h = Math.max(SIZE_VPAD[size] * 2 + fm.getHeight(), CLOSE_SIZE[size] + 8);
        return new Dimension(w, h);
    }

    static void selfCheck() {
        // 对比度：light 与 plain 各 type 深色文字变体 vs 对应背景
        for (int t = 0; t < 5; t++) {
            ElementTheme.assertContrast(DEEP_FG[t], LIGHT_BG[t], "tag light type=" + t);
            ElementTheme.assertContrast(DEEP_FG[t], Color.WHITE, "tag plain type=" + t);
            ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, LIGHT_BG[t], "tag hover-x on light type=" + t);
        }
        // 显式 setPreferredSize 必须被尊重（close 收缩动画依赖此：Animator 通过 setPreferredSize 驱动）
        Tag shrink = new Tag("标签", Tag.PRIMARY, false);
        shrink.setPreferredSize(new Dimension(1, 26));
        assert shrink.getPreferredSize().equals(new Dimension(1, 26))
                : "explicitly-set preferred size must be honored by getPreferredSize";
        // 可关闭 Tag 更宽（为 CloseButton 预留）
        Tag plain = new Tag("标签", Tag.PRIMARY, false);
        Tag closable = new Tag("标签", Tag.PRIMARY, true);
        assert closable.getPreferredSize().width > plain.getPreferredSize().width
                : "closable tag must reserve width for close button";
        // effect 切换不抛异常
        closable.setEffect(Tag.EFFECT_DARK);
        closable.setEffect(Tag.EFFECT_PLAIN);
        closable.setEffect(Tag.EFFECT_LIGHT);
        // 尺寸三档高度递减
        Tag l = new Tag("尺寸", Tag.INFO, false); l.setSize(Tag.SIZE_LARGE);
        Tag d = new Tag("尺寸", Tag.INFO, false);
        Tag s = new Tag("尺寸", Tag.INFO, false); s.setSize(Tag.SIZE_SMALL);
        assert l.getPreferredSize().height > d.getPreferredSize().height : "large > default height";
        assert d.getPreferredSize().height > s.getPreferredSize().height : "default > small height";
        // 加入窗口后 CloseButton 子组件存在且位于右侧
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame f = new JFrame();
                JPanel p = new JPanel();
                Tag c = new Tag("可关闭", Tag.SUCCESS, true);
                p.add(c);
                f.add(p);
                f.pack();
                assert c.getComponentCount() == 1 && c.getComponent(0) instanceof CloseButton
                        : "close button child present, count=" + c.getComponentCount();
                Component cb = c.getComponent(0);
                assert cb.getX() + cb.getWidth() <= c.getWidth() && cb.getX() > c.getWidth() / 2
                        : "close button on right side, x=" + cb.getX();
                f.dispose();
            });
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        // 点击 CloseButton 触发关闭动画并回调 onClosed（primary close 路径）
        final Throwable[] err2 = {null};
        final boolean[] closed = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame f = new JFrame();
                JPanel p = new JPanel();
                Tag c = new Tag("可关闭", Tag.SUCCESS, true);
                c.setOnClosed(() -> closed[0] = true);
                p.add(c);
                f.add(p);
                f.pack();
                Component cb = c.getComponent(0);
                cb.dispatchEvent(new MouseEvent(cb, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, cb.getWidth() / 2, cb.getHeight() / 2, 1, false));
                f.dispose();
            });
            Thread.sleep(400); // 等待 ~200ms 关闭动画完成
        } catch (Throwable t) { err2[0] = t; }
        if (err2[0] != null) throw new RuntimeException(err2[0]);
        assert closed[0] : "clicking close button should fire onClosed after close animation";
        System.out.println("Tag self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
