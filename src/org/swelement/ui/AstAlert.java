package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.theme.Theme;
import org.swelement.framework.AstContainerComponent;

import javax.swing.*;
import java.awt.*;

public class AstAlert extends AstContainerComponent {
    public static final int SUCCESS = 0, WARNING = 1, INFO = 2, ERROR = 3;

    private static final Color[] BG = {new Color(0xF0F9EB), new Color(0xFDF6EC), new Color(0xF4F4F5), new Color(0xFEF0F0)};
    private static final String[] ICONS = {"\u221a", "!", "i", "\u00d7"};

    private static final int PAD = 12;
    private static final int TITLE_LINE_H = 20;
    private static final int DESC_LINE_H = 18;
    private static final int LEFT_PAD = 40;
    private static final int RIGHT_PAD = 16;

    private Runnable onClosed;
    private int origW, origH;
    private final int type;
    private final String title, desc;
    private final boolean closable;
    private AstCloseButton closeBtn;

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("in", 300, Easing::easeOut);
        anim.register("out", 250, Easing::easeIn);
    }

    private Color typeColor(int t) {
        Theme theme = theme();
        switch (t) {
            case SUCCESS: return theme.getSuccess();
            case WARNING: return theme.getWarning();
            case INFO: return theme.getInfo();
            default: return theme.getDanger();
        }
    }

    public AstAlert(int type, String title, String desc, boolean closable) {
        this.type = type;
        this.title = title;
        this.desc = desc;
        this.closable = closable;
        setPreferredSize(new Dimension(360, desc == null ? 40 : 56));
        setLayout(null); // AstCloseButton 绝对定位
        if (closable) {
            closeBtn = new AstCloseButton(20);
            closeBtn.addActionListener(e -> close(() -> {}));
            add(closeBtn);
        }
        anim.go("in", 0f, 1f);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (closeBtn != null) {
            int boxH = Math.min(getHeight(), computeFixedH(getWidth()));
            closeBtn.setBounds(getWidth() - 16 - 20, (boxH - 20) / 2, 20, 20);
        }
    }

    /** 固定高度（内容派生，不随父容器拉伸）：无描述 40；有描述按换行行数（≤2 行）。 */
    private int computeFixedH(int availW) {
        if (desc == null) return 40;
        int descW = Math.max(20, availW - LEFT_PAD - RIGHT_PAD);
        int lines = wrapLines(getFontMetrics(theme().getFontBase().deriveFont(13f)), desc, descW, 2).length;
        return Math.max(40, PAD + TITLE_LINE_H + 4 + lines * DESC_LINE_H + PAD);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(360, computeFixedH(360)); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, computeFixedH(360)); }

    /** 淡入淡出动画驱动 AstCloseButton 的 alpha 与可交互性。 */
    private void syncClose() {
        if (closeBtn == null) return;
        float a = anim.getProgress("in") * (1 - anim.getProgress("out"));
        closeBtn.setAlpha(a);
        closeBtn.setInteractive(a > 0.5f && isEnabled());
    }

    public void close(Runnable onClosed) {
        this.onClosed = onClosed;
        origW = getPreferredSize().width;
        origH = getPreferredSize().height;
        anim.get("out").go(0f, 1f, () -> {
            if (this.onClosed != null) {
                Runnable r = this.onClosed;
                this.onClosed = null;
                r.run();
            }
        });
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        if (closeBtn != null) closeBtn.setEnabled(b); // 禁用态：关闭 × 灰化且不可点
        syncClose(); // 无动画 tick 时也要刷新 × 的可交互性（如淡入完成后才启用）
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 关闭动画期间缩小 preferredSize（复刻原始 outAnim 回调行为）
        float outProgress = anim.getProgress("out");
        if (outProgress > 0f && origH > 0) {
            int h = Math.max(1, Math.round(origH * (1 - outProgress)));
            setPreferredSize(new Dimension(origW, h));
            revalidate();
        }
        syncClose();
        int a = Math.round(255 * anim.getProgress("in") * (1 - outProgress));
        if (a <= 0) return;
        Graphics2D g2 = createGraphics(g);
        int w = getWidth();
        int boxH = Math.min(getHeight(), computeFixedH(w));
        g2.setColor(new Color(BG[type].getRed(), BG[type].getGreen(), BG[type].getBlue(), a));
        g2.fillRect(0, 0, w, boxH);
        Color tc = typeColor(type);
        g2.setColor(new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), a));
        g2.fillRect(0, 0, 4, boxH);

        int availW = Math.max(20, w - LEFT_PAD - RIGHT_PAD);
        Font baseFont = theme().getFontBase();
        if (desc == null) {
            // 精简模式（高40）：图标与标题垂直居中
            g2.setFont(baseFont.deriveFont(Font.BOLD, 16f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(ICONS[type], 16, (boxH - fm.getHeight()) / 2f + fm.getAscent());
            g2.setFont(baseFont.deriveFont(Font.BOLD));
            FontMetrics tfm = g2.getFontMetrics();
            g2.drawString(truncate(tfm, title, availW), LEFT_PAD, (boxH - tfm.getHeight()) / 2f + tfm.getAscent());
        } else {
            // 完整模式：标题上、描述下（描述最多 2 行，超出省略号截断）
            g2.setFont(baseFont.deriveFont(Font.BOLD, 16f));
            FontMetrics tfm = g2.getFontMetrics();
            g2.drawString(ICONS[type], 16, PAD + (TITLE_LINE_H - tfm.getHeight()) / 2f + tfm.getAscent());
            g2.drawString(truncate(tfm, title, availW), LEFT_PAD, PAD + (TITLE_LINE_H - tfm.getHeight()) / 2f + tfm.getAscent());
            g2.setFont(baseFont.deriveFont(13f));
            FontMetrics dfm = g2.getFontMetrics();
            Color descColor = new Color(0x606266);
            g2.setColor(new Color(descColor.getRed(), descColor.getGreen(), descColor.getBlue(), a));
            String[] lines = wrapLines(dfm, desc, availW, 2);
            int y = PAD + TITLE_LINE_H + 4;
            for (String ln : lines) {
                g2.drawString(ln, LEFT_PAD, y + dfm.getAscent());
                y += DESC_LINE_H;
            }
        }
        g2.dispose();
    }

    /** 按宽度折行（CJK 友好），最多 maxLines 行；超出时末行以省略号截断。 */
    private static String[] wrapLines(FontMetrics fm, String text, int maxW, int maxLines) {
        if (text == null || text.isEmpty()) return new String[]{""};
        java.util.List<String> raw = new java.util.ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') { raw.add(cur.toString()); cur.setLength(0); continue; }
            String t = cur.toString() + ch;
            if (fm.stringWidth(t) > maxW && cur.length() > 0) { raw.add(cur.toString()); cur = new StringBuilder(String.valueOf(ch)); }
            else cur.append(ch);
        }
        if (cur.length() > 0) raw.add(cur.toString());
        if (raw.size() <= maxLines) return raw.toArray(new String[0]);
        String[] out = new String[maxLines];
        for (int i = 0; i < maxLines - 1; i++) out[i] = raw.get(i);
        StringBuilder sb = new StringBuilder(raw.get(maxLines - 1));
        for (int i = maxLines; i < raw.size(); i++) sb.append(raw.get(i));
        out[maxLines - 1] = truncate(fm, sb.toString(), maxW);
        return out;
    }

    private static String truncate(FontMetrics fm, String s, int maxW) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= maxW) return s;
        String ell = "\u2026";
        int ellW = fm.stringWidth(ell);
        while (s.length() > 0 && fm.stringWidth(s) + ellW > maxW) s = s.substring(0, s.length() - 1);
        return s + ell;
    }

    @Override
    protected void selfCheck() {
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

        // 长描述 wrap + 截断：固定高度=2 行，绘制不抛异常且盒体不透明
        final AstAlert longA = new AstAlert(AstAlert.INFO, "标题",
                "这是一段非常长的描述文字，用于测试自动换行与省略号截断功能是否正常生效，内容应当被限制在最多两行并以省略号结尾。", true);
        assert longA.getPreferredSize().height >= 84 : "长描述应为 2 行固定高度(≥84)，实际=" + longA.getPreferredSize().height;
        final Throwable[] err2 = {null};
        try {
            Thread.sleep(350); // 等待入场动画完成，确保绘制时 alpha > 0
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                longA.setSize(longA.getPreferredSize());
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(longA.getWidth(), longA.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                try { longA.paint(gg); } finally { gg.dispose(); }
                int aa = (img.getRGB(20, longA.getHeight() / 2) >>> 24) & 0xFF;
                assert aa > 120 : "long desc alert box painted, alpha=" + aa;
            }});
        } catch (Throwable t) { err2[0] = t; }
        if (err2[0] != null) throw new RuntimeException(err2[0]);

        System.out.println("AstAlert self-check OK");
    }

    public static void main(String[] args) {
        new AstAlert(INFO, "title", "desc", true).selfCheck();
    }
}
