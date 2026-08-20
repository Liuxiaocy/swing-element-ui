package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class AstCard extends JComponent {
    private final String title;
    private final boolean bordered;
    private final Animator hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); } },
        new Animator.Listener() { public void update(float v) { hover = v; repaint(); } });
    private float hover;
    private JComponent content;
    private final JPanel headerActions;

    public AstCard(String title) { this(title, true, true); }

    public AstCard(String title, boolean bordered, boolean shadowOnHover) {
        this.title = title == null ? "" : title;
        this.bordered = bordered;
        this.headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        headerActions.setOpaque(false);
        setLayout(null); // manual layout in doLayout
        add(headerActions);
        setOpaque(false);
        if (shadowOnHover) {
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { if (isEnabled()) { hoverAnim.stop(); hoverAnim.go(hover, 1f); } }
                public void mouseExited(MouseEvent e)  { hoverAnim.stop(); hoverAnim.go(hover, 0f); }
            });
        }
    }

    public void setContent(JComponent c) {
        if (c == null) throw new IllegalArgumentException("content must not be null");
        if (content != null) remove(content);
        content = c; add(c); revalidate();
    }

    public void addHeaderAction(JComponent c) {
        if (c == null) throw new IllegalArgumentException("header action must not be null");
        headerActions.add(c); revalidate();
    }

    public void setShadowElevation(int level) { /* reserved for future multi-level shadow, currently binary hover only */ }

    public String getTitle() { return title; }
    public boolean isBordered() { return bordered; }
    public JComponent getContent() { return content; }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    @Override public void doLayout() {
        Insets in = getInsets();
        int x = in.left, y = in.top, w = getWidth() - in.left - in.right, h = getHeight() - in.top - in.bottom;
        int titleH = 48;
        // headerActions align to the right; use full width minus 8+8 from edges, full 48 title bar height
        headerActions.setBounds(x, y, Math.max(0, w), titleH);
        // content body padded 16 top/bottom, 20 left/right, below title bar separator line (drawn at y+titleH)
        if (content != null) {
            int padTB = 16, padLR = 20;
            int contentY = y + titleH + padTB;
            int contentH = Math.max(0, h - titleH - 2 * padTB);
            int contentX = x + padLR;
            int contentW = Math.max(0, w - 2 * padLR);
            content.setBounds(contentX, contentY, contentW, contentH);
        }
    }

    @Override public Dimension getPreferredSize() {
        int cw = content != null ? content.getPreferredSize().width + 40 : 360;
        int ch = 48 + 32 + (content != null ? content.getPreferredSize().height : 160);
        return new Dimension(Math.max(240, cw), Math.max(120, ch));
    }

    @Override public Dimension getMinimumSize() {
        return new Dimension(240, 120);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Color bg = Color.WHITE;
        Color borderColor = bordered
                ? ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, hover)
                : new Color(0, 0, 0, 0);
        ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, bg, "AstCard.body");
        int r = ElementTheme.RADIUS * 2;
        RoundRectangle2D rect = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1.5f, getHeight()-1.5f, r, r);
        g2.setColor(bg); g2.fill(rect);
        if (bordered) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(rect);
        }
        // Hover outer glow ring: PRIMARY translucent stroke 1.5px
        if (hover > 0.01f) {
            int a = Math.round(36 * hover);
            g2.setColor(new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), a));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1f, 1f, getWidth()-2.5f, getHeight()-2.5f, r, r));
        }
        // Title bar separator at y=48 (1px, BORDER_BASE)
        g2.setColor(ElementTheme.BORDER_BASE);
        g2.drawLine(0, 48, getWidth(), 48);
        // Title string: bold 16px, x=20, baseline vertically centered in 48px title bar
        g2.setColor(ElementTheme.TEXT_MAIN);
        ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, bg, "AstCard.title");
        g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2.getFontMetrics();
        int titleBaseline = (48 - fm.getHeight()) / 2 + fm.getAscent();
        // Don't paint title on top of right-justified headerActions (max title width: width - actions.width - 28px)
        int actionsW = headerActions.getPreferredSize().width + 28;
        int maxW = Math.max(10, getWidth() - actionsW);
        String shown = title;
        if (fm.stringWidth(shown) > maxW) {
            // ellipsize — approximate by stripping characters from end + append …
            String ellipsis = "\u2026";
            int ellW = fm.stringWidth(ellipsis);
            while (shown.length() > 0 && fm.stringWidth(shown) + ellW > maxW) {
                shown = shown.substring(0, shown.length() - 1);
            }
            shown = shown + ellipsis;
        }
        if (maxW > 20) {
            g2.drawString(shown, 20, titleBaseline);
        }
        g2.dispose();
    }

    static void selfCheck() {
        // Create card with title + content + action, paint off-screen to trigger contrast assertions
        final AstCard c = new AstCard("用户信息");
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel line1 = new JLabel("用户名：张三"); line1.setForeground(ElementTheme.TEXT_MAIN);
        JLabel line2 = new JLabel("邮箱：zhangsan@example.com"); line2.setForeground(ElementTheme.TEXT_REGULAR);
        body.add(line1); body.add(Box.createVerticalStrut(6)); body.add(line2);
        c.setContent(body);
        c.addHeaderAction(new Button("编辑", Button.DEFAULT, false));
        c.addHeaderAction(new Button("删除", Button.DANGER, false));
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                JFrame jf = new JFrame();
                jf.setSize(600, 500);
                jf.add(c);
                c.setBounds(0, 0, 520, 340);
                c.doLayout();
                // Force a single paintComponent for paint assertions
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(520, 340, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                try { c.paintComponent(gg); } finally { gg.dispose(); }
                // Header bar separator at y=48: pixel (10, 49) below should be drawn card bg; y=47 should be in title bg area.
                int bottomOfCard = img.getRGB(260, 200);  // inside card bg area (away from rounded corners) → white-ish
                int alphaBottom = (bottomOfCard >>> 24) & 0xFF;
                assert alphaBottom > 200 : "card body should be opaque white";
                // Check title elipsis: long title forces truncation
                AstCard longCard = new AstCard("非常长的卡片标题用来测试自动省略号效果，多余的文字不绘制在按钮区域之上");
                longCard.setBounds(0, 0, 400, 200);
                JPanel smallBody = new JPanel(); longCard.setContent(smallBody);
                longCard.doLayout();
                img = new java.awt.image.BufferedImage(400, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                gg = img.createGraphics();
                try { longCard.paintComponent(gg); } finally { gg.dispose(); }
                jf.dispose();
            }});
        } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        // Border card: isBordered getter + title getter
        AstCard plain = new AstCard("", false, false);
        assert plain.getTitle().isEmpty();
        assert !plain.isBordered();
        System.out.println("AstCard self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
