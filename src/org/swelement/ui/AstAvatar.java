package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.theme.Theme;
import org.swelement.framework.AstDisplayComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class AstAvatar extends AstDisplayComponent {
    public static final int CIRCLE = 0, SQUARE = 1;
    public static final int SIZE_SMALL = 32, SIZE_DEFAULT = 40, SIZE_LARGE = 64;
    /** 角标外扩边距：组件边界比头像大一圈，给右上角角标完整的绘制空间，
     *  否则角标上半部超出组件 bounds 被 Swing 裁剪（只显示下半圆）。 */
    public static final int BADGE_PAD = 12;

    private final int size, shape;
    private final Color bg;
    private final String text;
    private final ImageIcon icon;
    private final AstBadge badge;

    public AstAvatar(char c, int size, int shape) {
        this(ColorFactory.pick(c), String.valueOf(c), null, size, shape);
    }
    public AstAvatar(Color bg, String text, int size, int shape) {
        this(bg, text, null, size, shape);
    }
    public AstAvatar(ImageIcon icon, int size, int shape) {
        this(Color.WHITE, "", icon, size, shape);
    }
    private AstAvatar(Color bg, String text, ImageIcon icon, int size, int shape) {
        this.bg = bg; this.text = text == null ? "" : text; this.icon = icon;
        this.size = size; this.shape = shape;
        this.badge = new AstBadge();
        setLayout(null);
        add(badge);
        JLabel ph = new JLabel();
        ph.setPreferredSize(new Dimension(Math.max(1,size-28), Math.max(1,size-28)));
        badge.setContent(ph);
        badge.setDot(false);
        badge.setCount(0);
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("hover", 150, Easing::easeInOut);
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    anim.go("hover", anim.getProgress("hover"), 1f);
                }
            }
            public void mouseExited(MouseEvent e) {
                anim.go("hover", anim.getProgress("hover"), 0f);
            }
        });
    }

    public void setBadgeCount(int n)    { badge.setCount(n); }
    public void setBadgeDot(boolean b)  { badge.setDot(b); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        g2.translate(BADGE_PAD, BADGE_PAD);
        float hover = anim.getProgress("hover");
        Color bgLift = lerp(bg, lerp(bg, Color.WHITE, 0.15f), hover * 0.5f);
        Color bgPaint = new Color(bgLift.getRGB());
        Shape s;
        int rad = radius() * 2;
        if (shape == CIRCLE) {
            s = new Ellipse2D.Float(0.5f, 0.5f, size - 1f, size - 1f);
        } else {
            s = new RoundRectangle2D.Float(0.5f, 0.5f, size - 1.5f, size - 1.5f, rad, rad);
        }
        g2.setColor(bgPaint);
        g2.fill(s);
        if (hover > 0.01f) {
            Color primary = theme().getPrimary();
            g2.setColor(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), Math.round(80 * hover)));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(s);
        }
        Color fg = pickTextColorForBg(bgPaint);
        assertContrast(fg, bgPaint, "AstAvatar shape=" + shape + " lum=" + String.format("%.2f", luminance(bgPaint)));
        if (icon != null) {
            int iw = Math.min(icon.getIconWidth(), Math.max(4, size - 8));
            int ih = Math.min(icon.getIconHeight(), Math.max(4, size - 8));
            icon.paintIcon(this, g2, (size - iw) / 2, (size - ih) / 2);
        } else if (text.length() > 0) {
            g2.setColor(fg);
            Font f = theme().getFontBase().deriveFont(Font.BOLD, Math.max(10f, size * 0.4f));
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics(f);
            String txt = text.length() > 2 ? text.substring(0, 2) : text;
            float x = (size - fm.stringWidth(txt)) / 2f;
            float y = (size - fm.getHeight()) / 2f + fm.getAscent();
            g2.drawString(txt, x, y);
        }
        g2.dispose();
    }

    @Override public Dimension getPreferredSize() { return new Dimension(size + 2*BADGE_PAD, size + 2*BADGE_PAD); }
    @Override public Dimension getMinimumSize()   { return new Dimension(size + 2*BADGE_PAD, size + 2*BADGE_PAD); }
    @Override public Dimension getMaximumSize()   { return new Dimension(size + 2*BADGE_PAD, size + 2*BADGE_PAD); }

    @Override public void doLayout() {
        // AstBadge 内容 insets (top=12, left=12, bottom=0, right=0)，paintBadge 圆心 = (w-12, 12)（badge 坐标系）。
        // 目标：圆心落在头像右上角内侧 (BADGE_PAD + size - 12, BADGE_PAD + 12)（组件坐标系）。
        // 由 bx + w - 12 = BADGE_PAD + size - 12 且 by + 12 = BADGE_PAD + 12 解得：
        badge.setBounds(0, BADGE_PAD, size + BADGE_PAD, size + BADGE_PAD);
        // 离屏/无显示场景下 Swing 不会自动 validate，联动布局 badge 内部
        // （FillLayout 需执行一次才能给 overlay 正确 bounds，否则角标画在 0 尺寸区域）
        badge.doLayout();
    }

    private static final class ColorFactory {
        private static final Color[] POOL = {
                new Color(0x409EFF), new Color(0x67C23A), new Color(0xE6A23C), new Color(0xF56C6C),
                new Color(0x909399), new Color(0x8e44ad), new Color(0x16a085), new Color(0xd35400)
        };
        static Color pick(char c) { return POOL[(c & 0x7fffffff) % POOL.length]; }
    }

    @Override protected void selfCheck() {
        AstAvatar dark = new AstAvatar(new Color(0x111111), "X", 40, CIRCLE);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(80, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        try {
            dark.setBounds(0, 0, 40, 40);
            dark.doLayout();
            dark.paintComponent(gg);
        } finally { gg.dispose(); }
        AstAvatar light = new AstAvatar(new Color(0xE8F2FE), "AB", SIZE_DEFAULT, SQUARE);
        img = new java.awt.image.BufferedImage(80, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        gg = img.createGraphics();
        try {
            light.setBounds(0, 0, SIZE_DEFAULT, SIZE_DEFAULT);
            light.doLayout();
            light.paintComponent(gg);
        } finally { gg.dispose(); }
        AstAvatar a1 = new AstAvatar('Z', SIZE_DEFAULT, CIRCLE);
        AstAvatar a2 = new AstAvatar(new Color(0xFFFFFF), "U", SIZE_LARGE, SQUARE);
        a1.setBadgeDot(true); a2.setBadgeCount(99);
        assert a1.getPreferredSize().width == SIZE_DEFAULT + 2*BADGE_PAD;
        assert a2.getPreferredSize().height == SIZE_LARGE + 2*BADGE_PAD;
        // 回归：角标上半部必须完整绘制（旧缺陷：badge 圆心 y<0 被组件 bounds 裁掉上半圆）。
        // a2 头像白底、角标红底：采样圆心上方 8px 处应为角标红色而非透明白底。
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(
                a2.getPreferredSize().width, a2.getPreferredSize().height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg2 = bi.createGraphics();
        try { a2.setSize(a2.getPreferredSize()); a2.doLayout(); a2.paint(bg2); } finally { bg2.dispose(); }
        int cx = BADGE_PAD + SIZE_LARGE - 12, cy = BADGE_PAD + 12;
        // 距圆心 4px（而非 8px）：setCount 触发的 pop 动画可能停在 0.6 缩放（半径 5.4px），
        // 离屏绘制不走 EDT 泵，采样点必须落在缩放后的圆内。旧缺陷下圆心在 y=0，y=20 必在圆外。
        int px = bi.getRGB(cx, cy - 4);
        int pr = (px >>> 16) & 0xFF, pg = (px >>> 8) & 0xFF, pa = (px >>> 24) & 0xFF;
        assert pa > 200 && pr > 180 && pg < 160 : "badge upper half visible; got a="+pa+" r="+pr+" g="+pg;
        boolean caughtContrast = false;
        try {
            AstAvatar bad = new AstAvatar(new Color(0x888888), "A", 40, CIRCLE) {
                @Override protected void paintComponent(Graphics g) {
                    Color same = new Color(0x888888);
                    assertContrast(same, same, "AstAvatar.sameColorTest");
                }
            };
            bad.paintComponent(null);
        } catch (AssertionError expected) {
            caughtContrast = true;
        }
        assert caughtContrast : "same-fg-bg contrast should throw via assertContrast";
        for (int sz : new int[]{SIZE_SMALL, SIZE_DEFAULT, SIZE_LARGE}) {
            AstAvatar ch = new AstAvatar('P', sz, CIRCLE);
            assert ch.getPreferredSize().width == sz + 2*BADGE_PAD;
        }
        // 停掉 setBadgeCount 触发的 badge pop 动画 Timer，否则自检 JVM 无法退出
        a2.removeNotify();
        System.out.println("AstAvatar self-check OK");
    }
    public static void main(String[] args) {
        new AstAvatar('A', SIZE_DEFAULT, CIRCLE).selfCheck();
    }
}
