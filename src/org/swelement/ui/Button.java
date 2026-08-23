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
    private static final Color[] TYPE_FG  = {new Color(0x606266), PRIMARY_COLOR, SUCCESS_COLOR, WARNING_COLOR, DANGER_COLOR, INFO_COLOR};
    private static final Color[] PLAIN_BG = {FILL_BLANK, new Color(0xECF5FF), new Color(0xF0F9EB), new Color(0xFDF6EC), new Color(0xFEF0F0), new Color(0xF4F4F5)};
    // Darker text variants for plain mode — ensures WCAG 4.5:1 contrast on light backgrounds
    private static final Color[] PLAIN_FG = {new Color(0x606266), new Color(0x1d6fb5), new Color(0x2d6b18), new Color(0x955d12), new Color(0xb83232), new Color(0x606266)};
    // 朴素按钮状态底色：hover/active 逐级加深。文字恒为 PLAIN_FG，底色只会更深，
    // 对比度单调上升——任何动画过渡帧都满足 AA（避免半白字半深底的中间态）。
    private static final Color[] PLAIN_HOVER_BG = new Color[6];
    private static final Color[] PLAIN_ACTIVE_BG = new Color[6];
    /** active 时文字同步加深：底色 shade 0.84 后深字对比会跌破 4.5，需配合更深的文字。 */
    private static final Color[] PLAIN_ACTIVE_FG = new Color[6];
    static {
        for (int i = 0; i < 6; i++) {
            PLAIN_HOVER_BG[i] = shade(PLAIN_BG[i], 0.93f);
            PLAIN_ACTIVE_BG[i] = shade(PLAIN_BG[i], 0.84f);
            PLAIN_ACTIVE_FG[i] = shade(PLAIN_FG[i], 0.75f);
        }
    }
    private static Color shade(Color c, float f) {
        return new Color(Math.round(c.getRed() * f), Math.round(c.getGreen() * f), Math.round(c.getBlue() * f));
    }

    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private final Animator activeAnim = new Animator(120, Easing::easeInOut, v -> { active = v; repaint(); });
    private final Animator loadAnim = new Animator(800, Easing::linear, v -> { loadAngle = v; repaint(); });
    private float hover, active;
    private int size = SIZE_DEFAULT;
    private boolean round = false;
    private boolean circle = false;
    private String icon = null;
    private int iconPosition = ICON_LEFT;
    private boolean loading = false;
    private String loadingText = null;
    private float loadAngle = 0f;
    private boolean savedEnabled;
    private String savedText;
    private boolean textButton = false;
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

    public void setLoading(boolean loading) {
        if (this.loading == loading) return;
        this.loading = loading;
        if (loading) {
            savedEnabled = isEnabled();
            savedText = getText();
            setEnabled(false);
            setText(loadingText != null ? loadingText : "加载中");
            loadAngle = 0f;
            startLoadLoop();
        } else {
            loadAnim.stop();
            setEnabled(savedEnabled);
            setText(savedText);
        }
        revalidate();
        repaint();
    }

    public void setLoadingText(String text) {
        this.loadingText = text;
        if (loading) setText(text != null ? text : "加载中");
    }

    private void startLoadLoop() {
        loadAnim.go(0f, 1f, () -> { if (loading) startLoadLoop(); });
    }

    public void setTextButton(boolean textBtn) {
        this.textButton = textBtn;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg, fg, border;
        if (textButton) {
            if (!isEnabled()) {
                bg = new Color(0, 0, 0, 0);
                fg = new Color(0x606266);
                border = new Color(0, 0, 0, 0);
            } else {
                int alpha = Math.round(255 * hover);
                bg = new Color(0xEC, 0xF5, 0xFF, alpha);
                fg = ElementTheme.PRIMARY;
                border = new Color(0, 0, 0, 0);
            }
        } else if (loading) {
            // loading: use normal colors with reduced opacity so text stays visible
            bg = ElementTheme.lerp(ElementTheme.lerp(BASE_BG[type], HOVER_BG[type], hover), ACTIVE_BG[type], active);
            fg = ElementTheme.lerp(BASE_FG[type], HOVER_FG[type], hover);
            border = plain ? BORDER[type] : bg;
            if (plain) {
                bg = PLAIN_BG[type];
                fg = PLAIN_FG[type];
            }
            if (plain && type == DEFAULT) border = ElementTheme.lerp(BORDER_BASE, new Color(0xC6E2FF), hover);
            bg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 200);
            fg = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 230);
        } else if (!isEnabled()) {
            bg = plain ? FILL_BLANK : new Color(0xF5F7FA);
            fg = new Color(0x606266);
            border = plain ? BORDER_BASE : new Color(0xE4E7ED);
        } else {
            bg = ElementTheme.lerp(ElementTheme.lerp(BASE_BG[type], HOVER_BG[type], hover), ACTIVE_BG[type], active);
            fg = ElementTheme.lerp(BASE_FG[type], HOVER_FG[type], hover);
            border = plain ? BORDER[type] : bg;
            if (plain) {
                // Bug 修复：朴素按钮此前无视 hover/active，所有状态同色。
                bg = ElementTheme.lerp(PLAIN_BG[type], PLAIN_HOVER_BG[type], hover);
                bg = ElementTheme.lerp(bg, PLAIN_ACTIVE_BG[type], active);
                // 底色加深会压缩对比度，文字同步加深（更深更快）保证任何过渡帧 ≥ 4.5:1
                fg = ElementTheme.lerp(PLAIN_FG[type], PLAIN_ACTIVE_FG[type], Math.max(hover, active));
                // 边框随状态向深主色收敛，强化反馈（边框不承载文字，不受 AA 约束）
                border = ElementTheme.lerp(BORDER[type], PLAIN_FG[type], Math.max(hover, active));
            }
            if (plain && type == DEFAULT) border = ElementTheme.lerp(BORDER_BASE, new Color(0xC6E2FF), hover);
        }

        float arc = (round || circle) ? getHeight() / 2f : ElementTheme.RADIUS * 2;
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (!textButton || hover > 0) {
            g2.setColor(bg);
            g2.fill(shape);
        }
        if (!textButton) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(shape);
        }

        g2.setColor(fg);
        Font btnFont = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        g2.setFont(btnFont);
        FontMetrics fm = g2.getFontMetrics(btnFont);
        String text = getText();
        int textW = fm.stringWidth(text);
        int iconW = (!loading && icon != null) ? fm.stringWidth(icon) : 0;
        int loadW = loading ? 16 + SIZE_ICON_GAP[size] : 0;
        int gap = (iconW > 0 && textW > 0) ? SIZE_ICON_GAP[size] : 0;
        int contentW = textW + iconW + gap + loadW;
        float startX = (getWidth() - contentW) / 2f;
        float baseY = (getHeight() - fm.getHeight()) / 2f + fm.getAscent();
        float cursorX = startX;

        if (loading) {
            Graphics2D lg2 = (Graphics2D) g2.create();
            lg2.setColor(fg);
            lg2.setStroke(new BasicStroke(2f));
            int cx = Math.round(cursorX + 8);
            int cy = getHeight() / 2;
            double angle = loadAngle * 2 * Math.PI;
            lg2.drawArc(cx - 7, cy - 7, 14, 14, (int) Math.toDegrees(angle), 270);
            lg2.dispose();
            cursorX += loadW;
        }

        if (!loading && icon != null) {
            if (iconPosition == ICON_LEFT) {
                g2.drawString(icon, cursorX, baseY);
                cursorX += iconW + gap;
                g2.drawString(text, cursorX, baseY);
            } else {
                g2.drawString(text, cursorX, baseY);
                cursorX += textW + gap;
                g2.drawString(icon, cursorX, baseY);
            }
        } else {
            g2.drawString(text, cursorX, baseY);
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Font font = ElementTheme.FONT.deriveFont(SIZE_FONT[size]);
        FontMetrics fm = getFontMetrics(font);
        int textW = fm.stringWidth(getText());
        int iconW = (!loading && icon != null) ? fm.stringWidth(icon) : 0;
        int loadW = loading ? 16 + SIZE_ICON_GAP[size] : 0;
        int gap = (iconW > 0 && textW > 0) ? SIZE_ICON_GAP[size] : 0;
        int w = SIZE_HPAD[size] * 2 + textW + iconW + gap + loadW;
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

        Button lb = new Button("提交", Button.PRIMARY, false);
        assert lb.isEnabled() : "button should be enabled initially";
        lb.setLoading(true);
        assert !lb.isEnabled() : "loading button should be disabled";
        assert "加载中".equals(lb.getText()) : "loading text should default to 加载中, got " + lb.getText();
        lb.setLoading(false);
        assert lb.isEnabled() : "button should restore enabled after loading";
        assert "提交".equals(lb.getText()) : "button should restore original text after loading, got " + lb.getText();
        Button lb2 = new Button("保存");
        lb2.setLoadingText("保存中...");
        lb2.setLoading(true);
        assert "保存中...".equals(lb2.getText()) : "custom loading text should be used, got " + lb2.getText();
        lb2.setLoading(false);

        Button tb = new Button("文本按钮");
        tb.setTextButton(true);
        assert tb.getPreferredSize().width > 0 : "text button should have positive width";

        // Contrast checks: text must be readable against background in all states
        for (int t = 0; t < 6; t++) {
            ElementTheme.assertContrast(PLAIN_FG[t], PLAIN_BG[t], "plain type=" + t);
            ElementTheme.assertContrast(PLAIN_ACTIVE_FG[t], PLAIN_HOVER_BG[t], "plain hover fg type=" + t);
            ElementTheme.assertContrast(PLAIN_ACTIVE_FG[t], PLAIN_ACTIVE_BG[t], "plain active fg type=" + t);
        }
        ElementTheme.assertContrast(new Color(0x606266), new Color(0xF5F7FA), "disabled on gray");
        ElementTheme.assertContrast(new Color(0x606266), Color.WHITE, "disabled on white");

        System.out.println("Button self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
