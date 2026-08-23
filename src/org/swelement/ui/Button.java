package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class Button extends JButton {
    public static final int DEFAULT = 0, PRIMARY = 1, SUCCESS = 2, WARNING = 3, DANGER = 4, INFO = 5;
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    public static final int ICON_LEFT = 0, ICON_RIGHT = 1;

    private static final float[] SIZE_FONT = {16f, 14f, 12f};
    private static final int[] SIZE_VPAD = {12, 9, 6};
    private static final int[] SIZE_HPAD = {24, 20, 12};
    private static final int[] SIZE_ICON_GAP = {10, 8, 6};

    private static final Color WHITE = Color.WHITE;
    private static final Color FILL_BLANK = ElementTheme.FILL_BLANK;
    private static final Color PRIMARY_COLOR = ElementTheme.PRIMARY;
    private static final Color SUCCESS_COLOR = ElementTheme.SUCCESS;
    private static final Color WARNING_COLOR = ElementTheme.WARNING;
    private static final Color DANGER_COLOR  = ElementTheme.DANGER;
    private static final Color INFO_COLOR    = ElementTheme.INFO;
    private static final Color BORDER_BASE = ElementTheme.BORDER_BASE;

    private static final Color[] BASE_BG  = {FILL_BLANK, PRIMARY_COLOR, SUCCESS_COLOR, WARNING_COLOR, DANGER_COLOR, INFO_COLOR};
    private static final Color[] HOVER_BG = {new Color(0xECF5FF), new Color(0x66B1FF), new Color(0x85CE61), new Color(0xEBB563), new Color(0xF78989), new Color(0xA6A9AD)};
    private static final Color[] ACTIVE_BG= {new Color(0xD2E4FF), new Color(0x3A8EE6), new Color(0x5DAF32), new Color(0xCF9236), new Color(0xDD6161), new Color(0x82848A)};
    private static final Color[] BASE_FG  = {new Color(0x606266), WHITE, WHITE, WHITE, WHITE, WHITE};
    private static final Color[] HOVER_FG = {PRIMARY_COLOR, WHITE, WHITE, WHITE, WHITE, WHITE};
    private static final Color[] BORDER   = {BORDER_BASE, PRIMARY_COLOR, SUCCESS_COLOR, WARNING_COLOR, DANGER_COLOR, INFO_COLOR};

    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private final Animator activeAnim = new Animator(120, Easing::easeInOut, v -> { active = v; repaint(); });
    private float hover, active;
    private int size = SIZE_DEFAULT;
    private boolean round = false;
    private boolean circle = false;
    private String icon = null;
    private int iconPosition = ICON_LEFT;
    private final int type;
    private final boolean plain;

    public Button(String text) { this(text, DEFAULT, false); }

    public Button(String text, int type, boolean plain) {
        super(text);
        this.type = type;
        this.plain = plain;
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
        setFont(ElementTheme.FONT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (isEnabled()) hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); activeAnim.go(active, 0f); }
            public void mousePressed(MouseEvent e) { if (isEnabled()) activeAnim.go(active, 1f); }
            public void mouseReleased(MouseEvent e){ activeAnim.go(active, 0f); }
        });
    }

    public void setSize(int size) {
        this.size = size;
        revalidate();
        repaint();
    }

    public void setRound(boolean round) {
        this.round = round;
        repaint();
    }

    public void setCircle(boolean circle) {
        this.circle = circle;
        revalidate();
        repaint();
    }

    public void setIcon(String icon) {
        this.icon = icon;
        revalidate();
        repaint();
    }

    public void setIconPosition(int pos) {
        this.iconPosition = pos;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg, fg, border;
        if (!isEnabled()) {
            bg = plain ? FILL_BLANK : new Color(0xA0CFFF);
            fg = new Color(0xC0C4CC);
            border = plain ? BORDER_BASE : bg;
        } else {
            bg = ElementTheme.lerp(ElementTheme.lerp(BASE_BG[type], HOVER_BG[type], hover), ACTIVE_BG[type], active);
            fg = ElementTheme.lerp(BASE_FG[type], HOVER_FG[type], hover);
            border = plain ? BORDER[type] : bg;
            if (plain) bg = ElementTheme.lerp(FILL_BLANK, new Color(0xECF5FF), hover);
            if (plain) fg = ElementTheme.lerp(BASE_FG[type], PRIMARY_COLOR, hover);
        }
        if (plain && type == DEFAULT) border = ElementTheme.lerp(BORDER_BASE, new Color(0xC6E2FF), hover);

        float arc = (round || circle) ? getHeight() / 2f : ElementTheme.RADIUS * 2;
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.setColor(bg);
        g2.fill(shape);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(shape);

        g2.setColor(fg);
        Font btnFont = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        g2.setFont(btnFont);
        FontMetrics fm = g2.getFontMetrics(btnFont);
        String text = getText();
        int textW = fm.stringWidth(text);
        int iconW = (icon != null) ? fm.stringWidth(icon) : 0;
        int gap = (iconW > 0 && textW > 0) ? SIZE_ICON_GAP[size] : 0;
        int contentW = textW + iconW + gap;
        float startX = (getWidth() - contentW) / 2f;
        float baseY = (getHeight() - fm.getHeight()) / 2f + fm.getAscent();
        if (icon != null) {
            if (iconPosition == ICON_LEFT) {
                g2.drawString(icon, startX, baseY);
                g2.drawString(text, startX + iconW + gap, baseY);
            } else {
                g2.drawString(text, startX, baseY);
                g2.drawString(icon, startX + textW + gap, baseY);
            }
        } else {
            g2.drawString(text, startX, baseY);
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Font font = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        FontMetrics fm = getFontMetrics(font);
        int textW = fm.stringWidth(getText());
        int iconW = (icon != null) ? fm.stringWidth(icon) : 0;
        int gap = (iconW > 0 && textW > 0) ? SIZE_ICON_GAP[size] : 0;
        int w = SIZE_HPAD[size] * 2 + textW + iconW + gap;
        int h = SIZE_VPAD[size] * 2 + fm.getHeight();
        if (circle) {
            int s = Math.max(w, h);
            return new Dimension(s, s);
        }
        return new Dimension(w, h);
    }

    static void selfCheck() {
        Button b = new Button("测试");
        b.setSize(Button.SIZE_LARGE);
        assert b.getPreferredSize().height > new Button("测试").getPreferredSize().height
                : "SIZE_LARGE should be taller than SIZE_DEFAULT";
        b.setSize(Button.SIZE_SMALL);
        assert b.getPreferredSize().height < new Button("测试").getPreferredSize().height
                : "SIZE_SMALL should be shorter than SIZE_DEFAULT";

        Button rc = new Button("圆");
        rc.setCircle(true);
        Dimension pd = rc.getPreferredSize();
        assert pd.width == pd.height : "circle button preferredSize must be square, got " + pd.width + "x" + pd.height;
        rc.setRound(true);
        assert pd.width == pd.height : "round+circle still square";

        Button ib = new Button("");
        ib.setIcon("\u2713");
        assert ib.getPreferredSize().width > 0 : "icon-only button should have positive width";
        Button ib2 = new Button("确定");
        ib2.setIcon("\u2713");
        assert ib2.getPreferredSize().width > new Button("确定").getPreferredSize().width
                : "button with icon should be wider than text-only";
        ib2.setIconPosition(Button.ICON_RIGHT);
        assert ib2.getPreferredSize().width > new Button("确定").getPreferredSize().width
                : "icon-right button should also be wider";

        System.out.println("Button self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
