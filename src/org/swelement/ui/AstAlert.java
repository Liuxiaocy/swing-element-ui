package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;

public class AstAlert extends JComponent {
    public static final int SUCCESS = 0, WARNING = 1, INFO = 2, ERROR = 3;

    private static final Color[] COLORS = {ElementTheme.SUCCESS, ElementTheme.WARNING, ElementTheme.INFO, ElementTheme.DANGER};
    private static final Color[] BG = {new Color(0xF0F9EB), new Color(0xFDF6EC), new Color(0xF4F4F5), new Color(0xFEF0F0)};
    private static final String[] ICONS = {"\u221a", "!", "i", "\u00d7"};

    private float inP = 0f, outP;
    private Runnable onClosed;
    private int origW, origH;

    private final Animator inAnim = new Animator(300, Easing::easeOut, v -> { inP = v; repaint(); syncClose(); });
    private final Animator outAnim = new Animator(250, Easing::easeIn, v -> {
        outP = v;
        int h = Math.max(1, Math.round(origH * (1 - v)));
        setPreferredSize(new Dimension(origW, h));
        revalidate();
        if (v >= 1f && onClosed != null) {
            Runnable r = onClosed;
            onClosed = null;
            r.run();
        }
        repaint();
        syncClose();
    });
    private final int type;
    private final String title, desc;
    private final boolean closable;
    private AstCloseButton closeBtn;

    public AstAlert(int type, String title, String desc, boolean closable) {
        this.type = type;
        this.title = title;
        this.desc = desc;
        this.closable = closable;
        setOpaque(false);
        setPreferredSize(new Dimension(360, desc == null ? 40 : 56));
        setLayout(null); // AstCloseButton 绝对定位
        if (closable) {
            closeBtn = new AstCloseButton(20);
            closeBtn.addActionListener(e -> close(() -> {}));
            add(closeBtn);
        }
        inAnim.go(0f, 1f);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (closeBtn != null) {
            closeBtn.setBounds(getWidth() - 16 - 20, (getHeight() - 20) / 2, 20, 20);
        }
    }

    /** 淡入淡出动画驱动 AstCloseButton 的 alpha 与可交互性。 */
    private void syncClose() {
        if (closeBtn == null) return;
        float a = inP * (1 - outP);
        closeBtn.setAlpha(a);
        closeBtn.setInteractive(a > 0.5f && isEnabled());
    }

    public void close(Runnable onClosed) {
        this.onClosed = onClosed;
        origW = getWidth();
        origH = getHeight();
        outAnim.go(0f, 1f);
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        if (closeBtn != null) closeBtn.setEnabled(b); // 禁用态：关闭 × 灰化且不可点
        syncClose(); // 无动画 tick 时也要刷新 × 的可交互性（如淡入完成后才启用）
    }

    @Override
    protected void paintComponent(Graphics g) {
        int a = Math.round(255 * inP * (1 - outP));
        if (a <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(BG[type].getRed(), BG[type].getGreen(), BG[type].getBlue(), a));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(COLORS[type].getRed(), COLORS[type].getGreen(), COLORS[type].getBlue(), a));
        g2.fillRect(0, 0, 4, getHeight());

        if (desc == null) {
            // 精简模式（高40）：图标与标题垂直居中
            g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 16f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(ICONS[type], 16, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD));
            FontMetrics tfm = g2.getFontMetrics();
            g2.drawString(title, 40, (getHeight() - tfm.getHeight()) / 2f + tfm.getAscent());
        } else {
            // 完整模式（高56）：标题上、描述下
            g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 16f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(ICONS[type], 16, 22 - fm.getHeight() / 2f + fm.getAscent() - 2);
            g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD));
            FontMetrics tfm = g2.getFontMetrics();
            g2.drawString(title, 40, 22 - tfm.getHeight() / 2f + tfm.getAscent() - 2);
            g2.setFont(ElementTheme.FONT);
            FontMetrics dfm = g2.getFontMetrics();
            Color descColor = new Color(0x606266);
            g2.setColor(new Color(descColor.getRed(), descColor.getGreen(), descColor.getBlue(), a));
            g2.drawString(desc, 40, 40 - dfm.getHeight() / 2f + dfm.getAscent() - 2);
        }
        g2.dispose();
    }

    static void selfCheck() {
        AstAlert a = new AstAlert(AstAlert.INFO, "标题", "描述文字", true);
        assert a.getComponentCount() == 1 && a.getComponent(0) instanceof AstCloseButton
                : "closable alert has AstCloseButton child, count=" + a.getComponentCount();
        AstAlert b = new AstAlert(AstAlert.INFO, "标题", null, false);
        assert b.getComponentCount() == 0 : "non-closable alert has no child";
        // close() 动画完成后回调触发（Animator 走 EDT）
        final Throwable[] err = {null};
        final boolean[] closed = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                a.setSize(360, 56);
                a.close(() -> closed[0] = true);
            });
            Thread.sleep(400);
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        assert closed[0] : "onClosed callback should fire after close animation";
        System.out.println("AstAlert self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}