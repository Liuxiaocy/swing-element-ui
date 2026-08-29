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
    /** 阴影模式（Element UI Card 的 shadow 属性）。 */
    public enum Shadow { ALWAYS, HOVER, NEVER }

    private final String title;
    private final boolean bordered;
    private Shadow shadow = Shadow.HOVER;
    private final Animator hoverAnim = new Animator(150, new Easing() { public float apply(float t) { return Easing.easeInOut(t); } },
        new Animator.Listener() { public void update(float v) { hover = v; repaint(); } });
    private float hover;
    private JComponent content;
    private final JPanel headerActions;

    public AstCard(String title) { this(title, true, true); }

    /** 兼容旧构造器：shadowOnHover=true → HOVER，false → NEVER。 */
    public AstCard(String title, boolean bordered, boolean shadowOnHover) {
        this(title, bordered, shadowOnHover ? Shadow.HOVER : Shadow.NEVER);
    }

    public AstCard(String title, boolean bordered, Shadow shadow) {
        this.title = title == null ? "" : title;
        this.bordered = bordered;
        this.shadow = shadow == null ? Shadow.HOVER : shadow;
        this.headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        headerActions.setOpaque(false);
        setLayout(null); // manual layout in doLayout
        add(headerActions);
        setOpaque(false);
        if (this.shadow == Shadow.HOVER) {
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { if (isEnabled()) { hoverAnim.stop(); hoverAnim.go(hover, 1f); } }
                public void mouseExited(MouseEvent e)  { if (isEnabled()) { hoverAnim.stop(); hoverAnim.go(hover, 0f); } }
            });
        }
    }

    /** 阴影模式可运行时切换（HOVER 模式自动注册/复用悬停监听）。 */
    public void setShadow(Shadow s) {
        if (s == null) throw new IllegalArgumentException("shadow must not be null");
        this.shadow = s;
        if (s != Shadow.HOVER && hover > 0.01f) { hoverAnim.stop(); hover = 0f; }
        repaint();
    }

    public Shadow getShadow() { return shadow; }

    /** 无头卡片：title 为空时不绘制标题栏，内容区顶置。 */
    public boolean isHeadless() { return title.isEmpty(); }

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
        int titleH = isHeadless() ? 0 : 48;
        // headerActions align to the right; use full width minus 8+8 from edges, full 48 title bar height
        headerActions.setBounds(x, y, Math.max(0, w), titleH);
        headerActions.setVisible(titleH > 0);
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
        int titleH = isHeadless() ? 0 : 48;
        int cw = content != null ? content.getPreferredSize().width + 40 : 360;
        int ch = titleH + 32 + (content != null ? content.getPreferredSize().height : 160);
        return new Dimension(Math.max(240, cw), Math.max(120, ch));
    }

    @Override public Dimension getMinimumSize() {
        return new Dimension(240, 120);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Insets in = getInsets();
        int x = in.left, y = in.top;
        int w = getWidth() - in.left - in.right;
        int h = getHeight() - in.top - in.bottom;
        w = Math.max(0, w); h = Math.max(0, h);
        Color bg = Color.WHITE;
        Color borderColor = bordered
                ? ElementTheme.lerp(ElementTheme.BORDER_BASE, ElementTheme.PRIMARY, hover)
                : new Color(0, 0, 0, 0);
        ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, bg, "AstCard.body");
        int r = ElementTheme.RADIUS * 2;
        // 阴影：ALWAYS 恒定 / HOVER 随悬停插值 / NEVER 无（画在卡片底、向下偏移 3px 露出下缘）
        float shadowFactor = shadow == Shadow.ALWAYS ? 1f : shadow == Shadow.HOVER ? hover : 0f;
        if (shadowFactor > 0.01f && w > 2 && h > 2) {
            int sa = Math.round(50 * shadowFactor);
            g2.setColor(new Color(0x30, 0x31, 0x33, sa));
            g2.fill(new RoundRectangle2D.Float(x, y + 3f, w - 1f, h, r, r));
        }
        RoundRectangle2D rect = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1.5f, h - 1.5f, r, r);
        g2.setColor(bg); g2.fill(rect);
        if (bordered) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(rect);
        }
        // Hover outer glow ring: PRIMARY translucent stroke 1.5px
        if (hover > 0.01f && w > 2 && h > 2) {
            int a = Math.round(36 * hover);
            g2.setColor(new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), a));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(x + 1f, y + 1f, w - 2.5f, h - 2.5f, r, r));
        }
        // 无头卡片：不绘制标题栏（分隔线 + 标题文字）
        if (isHeadless()) { g2.dispose(); return; }
        // Title bar separator at y+48 (1px, BORDER_BASE)
        g2.setColor(ElementTheme.BORDER_BASE);
        g2.drawLine(x, y + 48, x + w, y + 48);
        // Title string: bold 16px, x+20, baseline vertically centered in 48px title bar
        g2.setColor(ElementTheme.TEXT_MAIN);
        ElementTheme.assertContrast(ElementTheme.TEXT_MAIN, bg, "AstCard.title");
        g2.setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2.getFontMetrics();
        int titleBaseline = y + (48 - fm.getHeight()) / 2 + fm.getAscent();
        // Don't paint title on top of right-justified headerActions (max title width: width - actions.width - 28px)
        int actionsW = headerActions.getPreferredSize().width + 28;
        int availableW = Math.max(10, w - actionsW);
        String shown = title;
        if (availableW > 20 && fm.stringWidth(shown) > availableW) {
            String ellipsis = "\u2026";
            int ellW = fm.stringWidth(ellipsis);
            while (shown.length() > 0 && fm.stringWidth(shown) + ellW > availableW) {
                shown = shown.substring(0, shown.length() - 1);
            }
            shown = shown + ellipsis;
        }
        if (availableW > 20) {
            g2.drawString(shown, x + 20, titleBaseline);
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
        c.addHeaderAction(new AstButton("编辑", AstButton.DEFAULT, false));
        c.addHeaderAction(new AstButton("删除", AstButton.DANGER, false));
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

        // ---- P4-E：阴影三态 / 无头卡片 / 兼容映射 ----
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                // 兼容映射：旧布尔构造器 true→HOVER，false→NEVER
                assert new AstCard("a", true, true).getShadow() == Shadow.HOVER : "compat true→HOVER";
                assert new AstCard("a", true, false).getShadow() == Shadow.NEVER : "compat false→NEVER";
                // 阴影像素断言：卡片右下缘下方 2px 处，ALWAYS 有阴影灰像素、NEVER 无
                AstCard sa = new AstCard("阴影", true, Shadow.ALWAYS);
                AstCard sn = new AstCard("无影", true, Shadow.NEVER);
                sa.setBounds(0, 0, 300, 200); sn.setBounds(0, 0, 300, 200);
                JPanel bb = new JPanel(); sa.setContent(bb); sn.setContent(bb);
                sa.doLayout(); sn.doLayout();
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(300, 210, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                try { sa.paintComponent(gg); } finally { gg.dispose(); }
                // 阴影画在 (x, y+3, w-1, h)，卡片本体覆盖其上；采样卡片底边正下方 2px（避开边框描边与圆角）：
                int px = img.getRGB(250, 201);
                assert ((px >>> 24) & 0xFF) > 0 : "ALWAYS 阴影应产生可见投影, got " + Integer.toHexString(px);
                img = new java.awt.image.BufferedImage(300, 210, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                gg = img.createGraphics();
                try { sn.paintComponent(gg); } finally { gg.dispose(); }
                px = img.getRGB(250, 201);
                assert ((px >>> 24) & 0xFF) == 0 : "NEVER 不应有投影, got " + Integer.toHexString(px);
                // setShadow 参数校验
                boolean threw = false;
                try { sn.setShadow(null); } catch (IllegalArgumentException e) { threw = true; }
                assert threw : "setShadow(null) should throw";
                // 无头卡片：高度更小（无 48 标题栏），且绘制不含分隔线（y=48 处应为白）
                JPanel tall = new JPanel();
                tall.setPreferredSize(new java.awt.Dimension(200, 200));
                AstCard hl = new AstCard("", true, Shadow.NEVER);
                hl.setContent(tall);
                int pH = hl.getPreferredSize().height;
                AstCard tt = new AstCard("有头", true, Shadow.NEVER);
                tt.setContent(tall);
                assert pH == tt.getPreferredSize().height - 48 : "无头卡片高度应少 48, got " + pH + " vs " + tt.getPreferredSize().height;
                assert hl.isHeadless() && !tt.isHeadless();
                hl.setBounds(0, 0, 300, 200); hl.doLayout();
                img = new java.awt.image.BufferedImage(300, 210, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                gg = img.createGraphics();
                try { hl.paintComponent(gg); } finally { gg.dispose(); }
                int line = img.getRGB(150, 48);
                assert ((line >> 16) & 0xFF) > 240 && ((line >> 8) & 0xFF) > 240 && (line & 0xFF) > 240
                    : "无头卡片 y=48 处不应有分隔线, got " + Integer.toHexString(line);
            }});
        } catch (Throwable t) { throw new RuntimeException(t); }
        System.out.println("AstCard self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
