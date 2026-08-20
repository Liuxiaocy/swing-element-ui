package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class AstAvatar extends JComponent {
    public static final int CIRCLE = 0, SQUARE = 1;
    public static final int SIZE_SMALL = 32, SIZE_DEFAULT = 40, SIZE_LARGE = 64;

    private final int size, shape;
    private final Color bg;
    private final String text;
    private final ImageIcon icon;
    private final Badge badge;
    private final Animator hoverAnim = new Animator(150, new Easing() {
        public float apply(float t) { return Easing.easeInOut(t); }
    }, new Animator.Listener() {
        public void update(float v) { hover = v; repaint(); }
    });
    private float hover;

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
        this.badge = new Badge();
        setOpaque(false);
        setLayout(null);
        add(badge);
        JLabel ph = new JLabel();
        ph.setPreferredSize(new Dimension(Math.max(1,size-28), Math.max(1,size-28)));
        badge.setContent(ph);
        badge.setDot(false);
        badge.setCount(0);
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (isEnabled()) { hoverAnim.stop(); hoverAnim.go(hover, 1f); } }
            public void mouseExited(MouseEvent e)  { hoverAnim.stop(); hoverAnim.go(hover, 0f); }
        });
    }

    public void setBadgeCount(int n)    { badge.setCount(n); }
    public void setBadgeDot(boolean b)  { badge.setDot(b); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Color bgLift = ElementTheme.lerp(bg, ElementTheme.lerp(bg, Color.WHITE, 0.15f), hover * 0.5f);
        Color bgPaint = new Color(bgLift.getRGB());
        Shape s;
        int rad = ElementTheme.RADIUS * 2;
        if (shape == CIRCLE) {
            s = new Ellipse2D.Float(0.5f, 0.5f, size - 1f, size - 1f);
        } else {
            s = new RoundRectangle2D.Float(0.5f, 0.5f, size - 1.5f, size - 1.5f, rad, rad);
        }
        g2.setColor(bgPaint);
        g2.fill(s);
        if (hover > 0.01f) {
            g2.setColor(new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), Math.round(80*hover)));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(s);
        }
        Color fg = ElementTheme.pickTextColorForBg(bgPaint);
        ElementTheme.assertContrast(fg, bgPaint, "AstAvatar shape="+shape+" lum="+String.format("%.2f", ElementTheme.luminance(bgPaint)));
        if (icon != null) {
            int iw = Math.min(icon.getIconWidth(), Math.max(4, size - 8));
            int ih = Math.min(icon.getIconHeight(), Math.max(4, size - 8));
            icon.paintIcon(this, g2, (size - iw) / 2, (size - ih) / 2);
        } else if (text.length() > 0) {
            g2.setColor(fg);
            Font f = ElementTheme.FONT.deriveFont(Font.BOLD, Math.max(10f, size * 0.4f));
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics(f);
            String txt = text.length() > 2 ? text.substring(0, 2) : text;
            float x = (size - fm.stringWidth(txt)) / 2f;
            float y = (size - fm.getHeight()) / 2f + fm.getAscent();
            g2.drawString(txt, x, y);
        }
        g2.dispose();
    }

    @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
    @Override public Dimension getMinimumSize()   { return new Dimension(size, size); }
    @Override public Dimension getMaximumSize()   { return new Dimension(size, size); }

    @Override public void doLayout() {
        // Badge EmptyBorder insets are (top=12, left=12, bottom=0, right=0).
        // Position badge so its content-inside-insets covers avatar 0..size bounding box
        // → center of badge paint = (size-12, 12) in badge coords, which lands at (size-12-12+12, 12-12+12) = (size-12, 12) in avatar coords.
        final int PAD = 12;
        badge.setBounds(-PAD, -PAD, size + PAD, size + PAD);
    }

    private static final class ColorFactory {
        private static final Color[] POOL = {
                new Color(0x409EFF), new Color(0x67C23A), new Color(0xE6A23C), new Color(0xF56C6C),
                new Color(0x909399), new Color(0x8e44ad), new Color(0x16a085), new Color(0xd35400)
        };
        static Color pick(char c) { return POOL[(c & 0x7fffffff) % POOL.length]; }
    }

    static void selfCheck() {
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
        assert a1.getPreferredSize().width == SIZE_DEFAULT;
        assert a2.getPreferredSize().height == SIZE_LARGE;
        boolean caughtContrast = false;
        try {
            AstAvatar bad = new AstAvatar(new Color(0x888888), "A", 40, CIRCLE) {
                @Override protected void paintComponent(Graphics g) {
                    Color same = new Color(0x888888);
                    ElementTheme.assertContrast(same, same, "AstAvatar.sameColorTest");
                }
            };
            bad.paintComponent(null);
        } catch (AssertionError expected) {
            caughtContrast = true;
        }
        assert caughtContrast : "same-fg-bg contrast should throw via assertContrast";
        for (int sz : new int[]{SIZE_SMALL, SIZE_DEFAULT, SIZE_LARGE}) {
            AstAvatar ch = new AstAvatar('P', sz, CIRCLE);
            assert ch.getPreferredSize().width == sz;
        }
        System.out.println("AstAvatar self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
