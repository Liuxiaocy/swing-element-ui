package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AstBadge extends JComponent {
    private final Animator popAnim = new Animator(200, Easing::easeOut, v -> { scale = v; repaint(); });
    private float scale = 1f;
    private int count;
    private boolean dot;
    private JComponent content;

    /** 尺寸档位（与 AstInput/AstPagination 等一致：0=large 1=default 2=small）。 */
    public static final int SIZE_LARGE = 0;
    public static final int SIZE_DEFAULT = 1;
    public static final int SIZE_SMALL = 2;

    private static final int[] TIER_BADGE_H = {20, 18, 16};
    private static final int[] TIER_DOT = {12, 10, 8};
    private static final float[] TIER_FONT = {13f, 12f, 11f};
    private static final int PAD = 12;

    /** 徽标类型，对应 Element 标准 5 色。 */
    public enum Type {
        PRIMARY, SUCCESS, WARNING, DANGER, INFO
    }

    private int tier = SIZE_DEFAULT;
    private int badgeH = TIER_BADGE_H[SIZE_DEFAULT];
    private int dotSize = TIER_DOT[SIZE_DEFAULT];
    private Type type = Type.DANGER;
    private int max = 99;
    private boolean hidden;

    /** 透明覆盖层，负责绘制角标。作为 index 0 子组件，绘制顺序在 content 之后（最上层）。 */
    private final JComponent overlay = new JComponent() {
        {
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            paintBadge(g);
        }
        @Override
        public boolean contains(int x, int y) {
            return false; // 鼠标事件穿透到下层 content
        }
    };

    public AstBadge() {
        setOpaque(false);
        setLayout(new FillLayout());
        setFont(ElementTheme.FONT.deriveFont(Font.BOLD, TIER_FONT[SIZE_DEFAULT]));
        setBorder(new EmptyBorder(PAD, 0, 0, PAD));
        add(overlay, 0); // index 0 = 最后绘制 = 最上层
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    public void setContent(JComponent c) {
        if (content != null) remove(content);
        content = c;
        add(content); // 追加到末尾，index > overlay，先绘制（底层）
        revalidate();
    }

    public void setCount(int c) {
        count = c;
        scale = 0.6f;
        popAnim.go(scale, 1f);
        repaint();
    }

    public void setDot(boolean b) { dot = b; repaint(); }

    /** 设置徽标配色。白字彩底为 Element 标准实心样式（对比度例外，与 AstTag 实心态一致）。 */
    public void setType(Type t) {
        if (t == null) throw new IllegalArgumentException("type must not be null");
        type = t;
        repaint();
    }

    public Type getType() { return type; }

    /** 设置封顶值：count 超过 max 时显示 {@code max+}。 */
    public void setMax(int m) {
        if (m < 1) throw new IllegalArgumentException("max must be >= 1: " + m);
        max = m;
        repaint();
    }

    public int getMax() { return max; }

    /** 隐藏徽标（仍保留 count/dot 状态，切回即可恢复）。 */
    public void setHidden(boolean h) { hidden = h; repaint(); }

    public boolean isHidden() { return hidden; }

    public void setSize(int t) {
        if (t < SIZE_LARGE || t > SIZE_SMALL) throw new IllegalArgumentException("size tier: " + t);
        tier = t;
        badgeH = TIER_BADGE_H[t];
        dotSize = TIER_DOT[t];
        setFont(ElementTheme.FONT.deriveFont(Font.BOLD, TIER_FONT[t]));
        repaint();
    }

    public int getSizeTier() { return tier; }

    /** 角标文本：count 未超过 max 时原样显示，超过则显示 {@code max+}。 */
    static String badgeText(int count, int max) {
        return count > max ? (max + "+") : String.valueOf(count);
    }

    private static Color colorOf(Type t) {
        switch (t) {
            case PRIMARY: return ElementTheme.PRIMARY;
            case SUCCESS: return ElementTheme.SUCCESS;
            case WARNING: return ElementTheme.WARNING;
            case DANGER:  return ElementTheme.DANGER;
            case INFO:    return ElementTheme.INFO;
            default: throw new AssertionError("unhandled badge type " + t);
        }
    }

    private void paintBadge(Graphics g) {
        if (hidden) return;
        if (count <= 0 && !dot) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Font badgeFont = ElementTheme.FONT.deriveFont(Font.BOLD, TIER_FONT[tier]);
        g2.setFont(badgeFont);
        FontMetrics fm = g2.getFontMetrics();
        String text = badgeText(count, max);
        int textW = dot ? 0 : fm.stringWidth(text);

        int badgeW = dot ? dotSize : (count > max ? textW + 10 : badgeH);

        // 角标中心定位在 content 的右上角顶点；宽文本（如 "999+"）左移以避免右侧被裁剪
        int cx = getWidth() - Math.max(PAD, badgeW / 2 + 1);
        int cy = PAD;

        float s = 0.6f + 0.4f * scale;
        g2.translate(cx, cy);
        g2.scale(s, s);
        g2.translate(-cx, -cy);

        g2.setColor(colorOf(type));
        if (dot) {
            g2.fillOval(cx - dotSize / 2, cy - dotSize / 2, dotSize, dotSize);
        } else if (count <= max) {
            g2.fillOval(cx - badgeH / 2, cy - badgeH / 2, badgeH, badgeH);
        } else {
            g2.fillRoundRect(cx - badgeW / 2, cy - badgeH / 2, badgeW, badgeH, badgeH / 2, badgeH / 2);
        }

        if (!dot) {
            g2.setColor(Color.WHITE);
            g2.drawString(text, cx - textW / 2f, cy - fm.getHeight() / 2f + fm.getAscent());
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Insets ins = getInsets();
        if (content == null) return new Dimension(48 + ins.left + ins.right, 48 + ins.top + ins.bottom);
        Dimension d = content.getPreferredSize();
        return new Dimension(d.width + ins.left + ins.right, d.height + ins.top + ins.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /** 自定义布局：content 填充 insets 内区域，overlay 填充整个 AstBadge（含 padding）以绘制角标。 */
    private class FillLayout implements LayoutManager {
        public void addLayoutComponent(String name, Component comp) {}
        public void removeLayoutComponent(Component comp) {}

        public Dimension preferredLayoutSize(Container parent) {
            return getPreferredSize();
        }
        public Dimension minimumLayoutSize(Container parent) {
            return getMinimumSize();
        }

        public void layoutContainer(Container parent) {
            Insets ins = parent.getInsets();
            int cw = parent.getWidth() - ins.left - ins.right;
            int ch = parent.getHeight() - ins.top - ins.bottom;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponent(i);
                if (c == overlay) {
                    c.setBounds(0, 0, parent.getWidth(), parent.getHeight());
                } else {
                    c.setBounds(ins.left, ins.top, cw, ch);
                }
            }
        }
    }

    // --- Self-check ---
    public static void selfCheck() {
        // 档位基础：默认档、字体联动、非法档位抛异常
        AstBadge b = new AstBadge();
        assert b.getSizeTier() == SIZE_DEFAULT : "default tier";
        assert b.getFont().getSize2D() == 12f : "default font=" + b.getFont().getSize2D();
        boolean threw = false;
        try { b.setSize(5); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "invalid tier must throw";
        b.setSize(SIZE_LARGE);
        assert b.getSizeTier() == SIZE_LARGE : "large tier";
        assert b.getFont().getSize2D() == 13f : "large font=" + b.getFont().getSize2D();
        b.setSize(SIZE_SMALL);
        assert b.getFont().getSize2D() == 11f : "small font=" + b.getFont().getSize2D();

        // 绘制级：dot 模式红点直径随档位变化（12 / 8，允许 ±1 抗锯齿误差）
        int dLarge = dotDiameterPx(SIZE_LARGE);
        int dSmall = dotDiameterPx(SIZE_SMALL);
        assert Math.abs(dLarge - 12) <= 1 : "large dot ~12, got " + dLarge;
        assert Math.abs(dSmall - 8) <= 1 : "small dot ~8, got " + dSmall;
        assert dLarge > dSmall : "large dot must be bigger";

        // 绘制级：count 模式圆角标高度随档位变化（20 / 16）
        int hLarge = countBadgeHeightPx(SIZE_LARGE);
        int hSmall = countBadgeHeightPx(SIZE_SMALL);
        assert Math.abs(hLarge - 20) <= 1 : "large count badge h ~20, got " + hLarge;
        assert Math.abs(hSmall - 16) <= 1 : "small count badge h ~16, got " + hSmall;

        // --- F2: max / hidden / type ---

        // max 溢出文本规则（含边界：count == max 不显示加号）
        assert badgeText(0, 99).equals("0") : "count 0";
        assert badgeText(99, 99).equals("99") : "count == max keeps plain number";
        assert badgeText(100, 99).equals("99+") : "over max → 99+";
        assert badgeText(1000, 999).equals("999+") : "over max → 999+";
        assert badgeText(11, 10).equals("10+") : "custom max";
        AstBadge bm = new AstBadge();
        assert bm.getMax() == 99 : "default max is 99 (Element 兼容)";
        boolean threwMax = false;
        try { bm.setMax(0); } catch (IllegalArgumentException e) { threwMax = true; }
        assert threwMax : "max < 1 must throw";

        // 绘制级：max 文本越长角标越宽，且宽文本不会被右边界裁剪
        int wNarrow = countBadgeBox(1000, 9)[2];    // "9+"
        int wWide = countBadgeBox(1000, 999)[2];    // "999+"
        assert wWide > wNarrow + 6 : "'999+' must be wider than '9+', got " + wNarrow + " vs " + wWide;
        int[] wideBox = countBadgeBox(1000, 999);
        assert wideBox[0] + wideBox[2] <= 159 : "wide badge must not be clipped at right edge, box="
            + wideBox[0] + "+" + wideBox[2];

        // 绘制级：hidden 切换后角标不落笔，取消隐藏后恢复
        AstBadge bh = new AstBadge();
        bh.setContent(new JLabel("X"));
        bh.setCount(5);
        bh.scale = 1f;
        int shown = countColor(render(bh), 0xF56C6C);
        assert shown > 20 : "count badge should paint red, got " + shown;
        bh.setHidden(true);
        assert bh.isHidden() : "isHidden";
        int hiddenPx = countColor(render(bh), 0xF56C6C);
        assert hiddenPx == 0 : "hidden badge must paint nothing, got " + hiddenPx;
        bh.setHidden(false);
        assert countColor(render(bh), 0xF56C6C) > 20 : "badge visible again after unhide";

        // 绘制级：5 种 type 各自只落笔 Element 语义色（dot 模式纯色圆，无白字干扰）
        AstBadge bt = new AstBadge();
        assert bt.getType() == Type.DANGER : "default type is DANGER (Element 兼容)";
        boolean threwType = false;
        try { bt.setType(null); } catch (IllegalArgumentException e) { threwType = true; }
        assert threwType : "null type must throw";
        for (Type t : Type.values()) {
            assert colorOf(t).getRGB() == (0xFF000000 | expectRgb(t)) : "colorOf(" + t + ") must match ElementTheme";
            java.awt.image.BufferedImage img = renderDot(t);
            int own = countColor(img, expectRgb(t));
            assert own > 20 : "type " + t + " should paint its own color, got " + own;
            for (Type o : Type.values()) {
                if (o == t) continue;
                int other = countColor(img, expectRgb(o));
                assert other == 0 : "type " + t + " must not paint " + o + " color, got " + other;
            }
        }
        System.out.println("AstBadge self-check OK");
    }

    private static int expectRgb(Type t) {
        switch (t) {
            case PRIMARY: return 0x409EFF;
            case SUCCESS: return 0x67C23A;
            case WARNING: return 0xE6A23C;
            case DANGER:  return 0xF56C6C;
            case INFO:    return 0x909399;
            default: throw new AssertionError(t);
        }
    }

    /** 离屏渲染 dot 角标，统计红色（0xF56C6C）像素外接框宽度作为直径。 */
    private static int dotDiameterPx(int tier) {
        AstBadge b = new AstBadge();
        b.setSize(tier);
        b.setDot(true);
        b.setContent(new JLabel("X"));
        return redBox(render(b))[2];
    }

    /** 离屏渲染 count 角标，统计红色像素外接框高度。 */
    private static int countBadgeHeightPx(int tier) {
        AstBadge b = new AstBadge();
        b.setSize(tier);
        b.setCount(5);
        b.scale = 1f; // 跳过弹出动画，确定态渲染
        b.setContent(new JLabel("X"));
        return redBox(render(b))[3];
    }

    /** 离屏渲染 count 角标，返回红色像素外接框 {x, y, w, h}。 */
    private static int[] countBadgeBox(int count, int max) {
        AstBadge b = new AstBadge();
        b.setMax(max);
        b.setCount(count);
        b.scale = 1f;
        b.setContent(new JLabel("X"));
        return redBox(render(b, 160, 60));
    }

    private static java.awt.image.BufferedImage renderDot(Type t) {
        AstBadge b = new AstBadge();
        b.setSize(SIZE_LARGE); // 大档圆点更大，取样像素更多
        b.setType(t);
        b.setDot(true);
        b.setContent(new JLabel("X"));
        return render(b);
    }

    private static java.awt.image.BufferedImage render(AstBadge b) {
        return render(b, 120, 60);
    }

    private static java.awt.image.BufferedImage render(AstBadge b, int w, int h) {
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            b.setBounds(0, 0, w, h);
            b.doLayout();
            b.paint(g);
        } finally {
            g.dispose();
            b.popAnim.stop(); // 停掉 setCount 启动的弹出动画，否则 Timer 会让自检 JVM 无法退出
        }
        return img;
    }

    /** 红色像素外接框 {minX, minY, w, h}；宽松红色判定以包含抗锯齿半透明边缘。 */
    private static int[] redBox(java.awt.image.BufferedImage img) {
        int minX = Integer.MAX_VALUE, maxX = -1, minY = Integer.MAX_VALUE, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int a = (rgb >>> 24) & 0xFF, r = (rgb >>> 16) & 0xFF, g = (rgb >>> 8) & 0xFF;
                if (a > 0 && r > g + 30) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) throw new AssertionError("badge not painted");
        return new int[]{minX, minY, maxX - minX + 1, maxY - minY + 1};
    }

    /** 统计与给定 RGB 精确匹配（容差 6）的不透明像素数量，用于验证角标实心色。 */
    private static int countColor(java.awt.image.BufferedImage img, int rgb) {
        int tr = (rgb >>> 16) & 0xFF, tg = (rgb >>> 8) & 0xFF, tb = rgb & 0xFF;
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                int a = (p >>> 24) & 0xFF;
                if (a < 250) continue; // 跳过抗锯齿边缘
                int r = (p >>> 16) & 0xFF, g = (p >>> 8) & 0xFF, b = p & 0xFF;
                if (Math.abs(r - tr) <= 6 && Math.abs(g - tg) <= 6 && Math.abs(b - tb) <= 6) n++;
            }
        }
        return n;
    }

    public static void main(String[] args) { selfCheck(); }
}
