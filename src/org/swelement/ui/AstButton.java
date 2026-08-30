package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.core.theme.Theme;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import javax.swing.event.EventListenerList;

public class AstButton extends AstInteractiveComponent {
    public static final int DEFAULT = 0, PRIMARY = 1, SUCCESS = 2, WARNING = 3, DANGER = 4, INFO = 5;
    public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
    public static final int ICON_LEFT = 0, ICON_RIGHT = 1;

    private static final float[] SIZE_FONT = {16f, 14f, 12f};
    private static final int[] SIZE_VPAD = {12, 9, 6};
    private static final int[] SIZE_HPAD = {24, 20, 12};
    private static final int[] SIZE_ICON_GAP = {10, 8, 6};

    private String text;
    private Icon icon;
    private int iconPosition = ICON_LEFT;
    private int type;
    private int size = SIZE_DEFAULT;
    private boolean plain;
    private boolean textStyle;
    private boolean round = false;
    private boolean circle = false;
    private boolean loading = false;
    private String loadingText = null;
    private boolean savedEnabled;
    private String savedText;
    private final EventListenerList actionListenerList = new EventListenerList();

    // ==================== 构造方法 ====================

    public AstButton(String text) {
        this(text, DEFAULT, false);
    }

    public AstButton(String text, int type, boolean plain) {
        super();
        this.text = text;
        this.type = type;
        this.plain = plain;
        setFocusable(true);
    }

    // ==================== 初始化 ====================

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("load", 800, Easing::linear);
    }

    // ==================== 模式 ====================

    @Override
    protected boolean isToggleMode() {
        return false;
    }

    @Override
    protected void onActionPerformed() {
        super.onActionPerformed();
        if (!isEnabled() || loading) return;
        fireActionPerformed();
    }

    // ==================== 属性访问 ====================

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        revalidate();
        repaint();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
        repaint();
    }

    public void setSize(int size) {
        this.size = size;
        revalidate();
        repaint();
    }

    public boolean isPlain() {
        return plain;
    }

    public void setPlain(boolean plain) {
        this.plain = plain;
        repaint();
    }

    public boolean isTextStyle() {
        return textStyle;
    }

    public void setTextStyle(boolean textStyle) {
        this.textStyle = textStyle;
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

    public void setIcon(Icon icon) {
        this.icon = icon;
        revalidate();
        repaint();
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIconPosition(int pos) {
        this.iconPosition = pos;
        repaint();
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        if (this.loading == loading) return;
        this.loading = loading;
        if (loading) {
            savedEnabled = isEnabled();
            savedText = text;
            setEnabled(false);
            text = loadingText != null ? loadingText : "加载中";
            anim.setProgress("load", 0f);
            anim.go("load", 0f, 1f);
        } else {
            anim.stop("load");
            setEnabled(savedEnabled);
            text = savedText;
        }
        revalidate();
        repaint();
    }

    public void setLoadingText(String text) {
        this.loadingText = text;
        if (loading) {
            this.text = text != null ? text : "加载中";
            repaint();
        }
    }

    // ==================== ActionListener 支持 ====================

    public void addActionListener(ActionListener l) {
        actionListenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        actionListenerList.remove(ActionListener.class, l);
    }

    protected void fireActionPerformed() {
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, text != null ? text : "");
        for (ActionListener l : actionListenerList.getListeners(ActionListener.class)) {
            l.actionPerformed(e);
        }
    }

    /** 以编程方式触发点击，等效于用户点击。 */
    public void doClick() {
        if (!isEnabled() || loading) return;
        fireActionPerformed();
    }

    // ==================== 颜色计算方法 ====================

    private Color typeColor(int type) {
        Theme t = theme();
        switch (type) {
            case PRIMARY: return t.getPrimary();
            case SUCCESS: return t.getSuccess();
            case WARNING: return t.getWarning();
            case DANGER: return t.getDanger();
            case INFO: return t.getInfo();
            default: return t.getTextRegular(); // DEFAULT
        }
    }

    private Color baseBg(int type) {
        Theme t = theme();
        switch (type) {
            case PRIMARY: return t.getPrimary();
            case SUCCESS: return t.getSuccess();
            case WARNING: return t.getWarning();
            case DANGER: return t.getDanger();
            case INFO: return t.getInfo();
            default: return t.getFillBlank(); // DEFAULT
        }
    }

    private Color hoverBg(int type) {
        if (type == DEFAULT) {
            return mixWithWhite(theme().getPrimary(), 0.9f);
        }
        return mixWithWhite(baseBg(type), 0.2f);
    }

    private Color activeBg(int type) {
        if (type == DEFAULT) {
            return mixWithWhite(theme().getPrimary(), 0.8f);
        }
        return shade(baseBg(type), 0.9f);
    }

    private Color baseFg(int type) {
        if (type == DEFAULT) {
            return theme().getTextRegular();
        }
        return Color.WHITE;
    }

    private Color hoverFg(int type) {
        if (type == DEFAULT) {
            return theme().getPrimary();
        }
        return Color.WHITE;
    }

    private Color borderColor(int type) {
        Theme t = theme();
        switch (type) {
            case PRIMARY: return t.getPrimary();
            case SUCCESS: return t.getSuccess();
            case WARNING: return t.getWarning();
            case DANGER: return t.getDanger();
            case INFO: return t.getInfo();
            default: return t.getBorderBase(); // DEFAULT
        }
    }

    private Color plainBg(int type) {
        if (type == DEFAULT) {
            return theme().getFillBlank();
        }
        return mixWithWhite(typeColor(type), 0.9f);
    }

    private Color plainFg(int type) {
        if (type == DEFAULT || type == INFO) {
            return theme().getTextRegular();
        }
        return shade(typeColor(type), 0.65f);
    }

    private Color plainHoverBg(int type) {
        return shade(plainBg(type), 0.93f);
    }

    private Color plainActiveBg(int type) {
        return shade(plainBg(type), 0.84f);
    }

    private Color plainActiveFg(int type) {
        return shade(plainFg(type), 0.75f);
    }

    /** RGB 通道按比例缩放（变暗）。 */
    private static Color shade(Color c, float f) {
        return new Color(
                Math.round(c.getRed() * f),
                Math.round(c.getGreen() * f),
                Math.round(c.getBlue() * f));
    }

    /** 与白色混合。amount 为白色占比 [0,1]。 */
    private static Color mixWithWhite(Color c, float amount) {
        float baseAmt = 1f - amount;
        return new Color(
                Math.round(c.getRed() * baseAmt + 255 * amount),
                Math.round(c.getGreen() * baseAmt + 255 * amount),
                Math.round(c.getBlue() * baseAmt + 255 * amount));
    }

    // ==================== 绘制 ====================

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);

        // loading 动画循环
        if (loading) {
            float p = anim.getProgress("load");
            if (p >= 0.99f) {
                anim.setProgress("load", 0f);
                anim.go("load", 0f, 1f);
            }
        }

        float hover = hoverProgress();
        float active = activeProgress();

        Color bg, fg, border;
        if (textStyle) {
            if (!isEnabled()) {
                bg = new Color(0, 0, 0, 0);
                fg = theme().getTextRegular();
                border = new Color(0, 0, 0, 0);
            } else {
                int alpha = Math.round(255 * hover);
                bg = new Color(0xEC, 0xF5, 0xFF, alpha);
                fg = theme().getPrimary();
                border = new Color(0, 0, 0, 0);
            }
        } else if (loading) {
            // loading: use normal colors with reduced opacity so text stays visible
            bg = lerp(lerp(baseBg(type), hoverBg(type), hover), activeBg(type), active);
            fg = lerp(baseFg(type), hoverFg(type), hover);
            border = plain ? borderColor(type) : bg;
            if (plain) {
                bg = plainBg(type);
                fg = plainFg(type);
            }
            if (plain && type == DEFAULT) {
                border = lerp(theme().getBorderBase(), new Color(0xC6E2FF), hover);
            }
            bg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 200);
            fg = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 230);
        } else if (!isEnabled()) {
            bg = plain ? theme().getFillBlank() : new Color(0xF5F7FA);
            fg = theme().getTextRegular();
            border = plain ? theme().getBorderBase() : new Color(0xE4E7ED);
        } else {
            bg = lerp(lerp(baseBg(type), hoverBg(type), hover), activeBg(type), active);
            fg = lerp(baseFg(type), hoverFg(type), hover);
            border = plain ? borderColor(type) : bg;
            if (plain) {
                bg = lerp(plainBg(type), plainHoverBg(type), hover);
                bg = lerp(bg, plainActiveBg(type), active);
                fg = lerp(plainFg(type), plainActiveFg(type), Math.max(hover, active));
                border = lerp(borderColor(type), plainFg(type), Math.max(hover, active));
            }
            if (plain && type == DEFAULT) {
                border = lerp(theme().getBorderBase(), new Color(0xC6E2FF), hover);
            }
        }

        float arc = (round || circle) ? getHeight() / 2f : theme().getRadiusBase() * 2f;
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (!textStyle || hover > 0) {
            g2.setColor(bg);
            g2.fill(shape);
        }
        if (!textStyle) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(shape);
        }

        g2.setColor(fg);
        Font btnFont = theme().getFontBase().deriveFont(SIZE_FONT[size]);
        g2.setFont(btnFont);
        FontMetrics fm = g2.getFontMetrics(btnFont);
        String displayText = text != null ? text : "";
        int textW = fm.stringWidth(displayText);
        int iconW = (!loading && icon != null) ? icon.getIconWidth() : 0;
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
            double angle = anim.getProgress("load") * 2 * Math.PI;
            lg2.drawArc(cx - 7, cy - 7, 14, 14, (int) Math.toDegrees(angle), 270);
            lg2.dispose();
            cursorX += loadW;
        }

        if (!loading && icon != null) {
            int iy = (getHeight() - icon.getIconHeight()) / 2;
            if (iconPosition == ICON_LEFT) {
                paintButtonIcon(g2, icon, fg, Math.round(cursorX), iy);
                cursorX += iconW + gap;
                g2.drawString(displayText, cursorX, baseY);
            } else {
                g2.drawString(displayText, cursorX, baseY);
                cursorX += textW + gap;
                paintButtonIcon(g2, icon, fg, Math.round(cursorX), iy);
            }
        } else {
            g2.drawString(displayText, cursorX, baseY);
        }
        g2.dispose();
    }

    private void paintButtonIcon(Graphics2D g2, Icon ic, Color c, int x, int y) {
        if (ic instanceof AstIcon) {
            AstIcon ai = (AstIcon) ic;
            AstIcon.paintIcon(g2, ai.getTypeEnum(), c, ai.getSizeValue(), ai.getSpinPhase());
        } else {
            ic.paintIcon(this, g2, x, y);
        }
    }

    // ==================== 布局 ====================

    @Override
    public Dimension getPreferredSize() {
        Font font = theme().getFontBase().deriveFont(SIZE_FONT[size]);
        FontMetrics fm = getFontMetrics(font);
        String displayText = text != null ? text : "";
        int textW = fm.stringWidth(displayText);
        int iconW = (!loading && icon != null) ? icon.getIconWidth() : 0;
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

    // ==================== 自检 ====================

    @Override
    protected void selfCheck() {
        AstButton b = new AstButton("测试");
        b.setSize(AstButton.SIZE_LARGE);
        assert b.getPreferredSize().height > new AstButton("测试").getPreferredSize().height
                : "SIZE_LARGE should be taller than SIZE_DEFAULT";
        b.setSize(AstButton.SIZE_SMALL);
        assert b.getPreferredSize().height < new AstButton("测试").getPreferredSize().height
                : "SIZE_SMALL should be shorter than SIZE_DEFAULT";

        AstButton rc = new AstButton("圆");
        rc.setCircle(true);
        Dimension pd = rc.getPreferredSize();
        assert pd.width == pd.height : "circle button preferredSize must be square, got " + pd.width + "x" + pd.height;
        rc.setRound(true);
        assert pd.width == pd.height : "round+circle still square";

        Color iconColor = theme().getPrimary();
        AstButton ib = new AstButton("");
        ib.setIcon(new AstIcon(AstIcon.Type.CHECK, iconColor, 16));
        assert ib.getPreferredSize().width > 0 : "icon-only button should have positive width";
        AstButton ib2 = new AstButton("确定");
        ib2.setIcon(new AstIcon(AstIcon.Type.CHECK, iconColor, 16));
        assert ib2.getPreferredSize().width > new AstButton("确定").getPreferredSize().width
                : "button with icon should be wider than text-only";
        ib2.setIconPosition(AstButton.ICON_RIGHT);
        assert ib2.getPreferredSize().width > new AstButton("确定").getPreferredSize().width
                : "icon-right button should also be wider";

        // 原生 ImageIcon（Icon 接口路径）
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig = bi.createGraphics();
        ig.setColor(java.awt.Color.RED);
        ig.fillRect(0, 0, 16, 16);
        ig.dispose();
        ImageIcon nativeIcon = new ImageIcon(bi);
        AstButton nb = new AstButton("原生");
        nb.setIcon(nativeIcon);
        assert nb.getPreferredSize().width > 0 : "native image icon button positive width";

        // 渲染均不抛异常（AstIcon 颜色跟随路径 + ImageIcon 路径）
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(200, 60, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            ib.paint(g);   // AstIcon 图标
            nb.paint(g);   // ImageIcon 图标
        } finally {
            g.dispose();
        }

        AstButton lb = new AstButton("提交", AstButton.PRIMARY, false);
        assert lb.isEnabled() : "button should be enabled initially";
        lb.setLoading(true);
        assert !lb.isEnabled() : "loading button should be disabled";
        assert "加载中".equals(lb.getText()) : "loading text should default to 加载中, got " + lb.getText();
        lb.setLoading(false);
        assert lb.isEnabled() : "button should restore enabled after loading";
        assert "提交".equals(lb.getText()) : "button should restore original text after loading, got " + lb.getText();
        AstButton lb2 = new AstButton("保存");
        lb2.setLoadingText("保存中...");
        lb2.setLoading(true);
        assert "保存中...".equals(lb2.getText()) : "custom loading text should be used, got " + lb2.getText();
        lb2.setLoading(false);

        AstButton tb = new AstButton("文本按钮");
        tb.setTextStyle(true);
        assert tb.getPreferredSize().width > 0 : "text button should have positive width";

        // Contrast checks: text must be readable against background in all states
        for (int t = 0; t < 6; t++) {
            assertContrast(plainFg(t), plainBg(t), "plain type=" + t);
            assertContrast(plainActiveFg(t), plainHoverBg(t), "plain hover fg type=" + t);
            assertContrast(plainActiveFg(t), plainActiveBg(t), "plain active fg type=" + t);
        }
        assertContrast(new Color(0x606266), new Color(0xF5F7FA), "disabled on gray");
        assertContrast(new Color(0x606266), Color.WHITE, "disabled on white");

        // ActionListener test
        final boolean[] fired = {false};
        AstButton ab = new AstButton("动作");
        ab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fired[0] = true;
            }
        });
        ab.onActionPerformed();
        assert fired[0] : "ActionListener should fire onActionPerformed";

        System.out.println("Button self-check OK");
    }

    public static void main(String[] args) {
        new AstButton("测试").selfCheck();
    }
}
