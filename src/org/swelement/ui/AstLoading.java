package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;

/**
 * AstLoading 组件：
 *  - Mode WRAP：包裹任意 JComponent，调用 showLoading(text)/hideLoading() 显示覆盖层
 *  - Mode FULLSCREEN：作为 JFrame 的 GlassPane，全屏锁定 + 遮罩
 *  - Spinner：12 段弧线旋转指示器（Element UI 经典环形进度）
 *  - Text：可选文字提示（"加载中…"），支持动态更新
 *  - Fade Animator：显隐 220ms opacity 过渡
 *
 * 使用方式：
 *   // wrap mode
 *   AstLoading loader = new AstLoading(Mode.WRAP, myTargetComp);
 *   loader.showLoading("数据加载中…");
 *   // later
 *   loader.hideLoading();
 *
 *   // fullscreen mode
 *   AstLoading loader = new AstLoading(Mode.FULLSCREEN, null);
 *   myFrame.setGlassPane(loader);
 *   loader.showLoading("正在提交请求…");  // calls setVisible(true)
 *   loader.hideLoading();  // calls setVisible(false)
 */
public class AstLoading extends JComponent {
    public enum Mode { WRAP, FULLSCREEN }

    private final Mode mode;
    private final JComponent target; // wrap mode target; FULLSCREEN uses null

    // Animation: rotation angle in radians (0..2π), loop driven by Swing Timer
    private final Timer spinTimer;
    private float angle;  // radians
    private static final int SPIN_INTERVAL = 30;  // ~33 fps
    private static final float SPIN_SPEED = 0.42f; // radians / tick → ~2 spins/sec

    // Fade animator for overlay opacity
    private float overlay = 0f; // 0 = hidden, 1 = fully visible
    private final Animator fadeAnim = new Animator(220, new Easing() { public float apply(float t) { return Easing.easeOut(t); } },
        new Animator.Listener() { public void update(float v) { overlay = v; setVisible(overlay > 0.01f && visible); repaint(); } });

    private boolean visible = false;
    private String text = "";

    // --- F3: 可定制遮罩 / 指示器 / 延迟显示 ---
    /** 默认遮罩底色：FULLSCREEN 半透明浅灰，WRAP 纯白。 */
    private static final Color MASK_FULLSCREEN = new Color(0xE9, 0xEB, 0xEF, 0xA0);
    private static final Color MASK_WRAP = Color.WHITE;
    /** 默认指示器外径（半径 28 + 线长 12 → 外径 68），与 Element UI 环形进度一致。 */
    public static final int DEFAULT_SPINNER_SIZE = 68;

    private Color bgColor;                       // null = 用模式默认色
    private int spinnerSize = DEFAULT_SPINNER_SIZE;
    private int showDelay;                       // 0 = 立即显示
    private Timer delayTimer;                     // 延迟显示计时器（非重复）
    private boolean pendingShow;                  // 已 showLoading 但延迟未到（尚未淡入）

    /**
     * 冻结层：loading 激活期间拦截鼠标/键盘事件。
     * FULLSCREEN：消费窗口内一切鼠标与键盘事件（用户明确要求"冻结屏幕，防止用户操作"）。
     * WRAP：仅消费落在 wrap 边界内的鼠标事件，遮罩区域不可交互。
     * 常驻注册 + isFreezing() 快速短路，无需管理注册/注销时序。
     */
    private final java.awt.event.AWTEventListener freezeListener = new java.awt.event.AWTEventListener() {
        public void eventDispatched(AWTEvent ev) {
            if (!isFreezing()) return;
            if (ev instanceof java.awt.event.MouseEvent) {
                if (mode == Mode.FULLSCREEN) { ((java.awt.event.MouseEvent) ev).consume(); return; }
                Component src = ((java.awt.event.MouseEvent) ev).getComponent();
                if (src == null || !isShowing()) return;
                java.awt.Point p = SwingUtilities.convertPoint(
                        src, ((java.awt.event.MouseEvent) ev).getPoint(), AstLoading.this);
                if (p.x >= 0 && p.y >= 0 && p.x < getWidth() && p.y < getHeight()) {
                    ((java.awt.event.MouseEvent) ev).consume();
                }
            } else if (ev instanceof java.awt.event.KeyEvent && mode == Mode.FULLSCREEN) {
                ((java.awt.event.KeyEvent) ev).consume();
            }
        }
    };

    /** 遮罩仍显著（含淡出过程）且逻辑上处于 loading 时视为冻结。 */
    private boolean isFreezing() { return visible || overlay > 0.3f; }

    public AstLoading(Mode mode, JComponent target) {
        if (mode == null) throw new IllegalArgumentException("mode must not be null");
        this.mode = mode;
        this.target = target;
        setOpaque(false);
        // Initially not visible; caller shows via showLoading
        setVisible(false);
        if (mode == Mode.WRAP) {
            if (target == null) throw new IllegalArgumentException("target must not be null in WRAP mode");
            setLayout(new BorderLayout());
            add(target, BorderLayout.CENTER);
        } else {
            setLayout(null);
        }
        spinTimer = new Timer(SPIN_INTERVAL, new ActionListener() { public void actionPerformed(ActionEvent e) {
            if (overlay > 0.01f) {
                angle = (angle + SPIN_SPEED) % (2f * (float) Math.PI);
                repaint();
            }
        }});
        spinTimer.setRepeats(true);
        spinTimer.setCoalesce(true);
        Toolkit.getDefaultToolkit().addAWTEventListener(freezeListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    public Mode getMode() { return mode; }
    public JComponent getTarget() { return target; }
    public String getText() { return text; }
    public boolean isLoadingVisible() { return visible; }
    public float getOverlayAlpha() { return overlay; }

    /** 遮罩底色（含 alpha）。传 null 恢复模式默认；调用 setBgColor 后以自定义色为准。 */
    public void setBgColor(Color c) { bgColor = c; repaint(); }

    /** 返回实际生效的遮罩底色（未自定义时为模式默认色）。 */
    public Color getBgColor() {
        return bgColor != null ? bgColor : (mode == Mode.FULLSCREEN ? MASK_FULLSCREEN : MASK_WRAP);
    }

    /** 环形指示器外径（px）。弧线长度按默认比例缩放，最小 16。 */
    public void setSpinnerSize(int px) {
        if (px < 16) throw new IllegalArgumentException("spinner size must be >= 16: " + px);
        spinnerSize = px;
        repaint();
    }

    public int getSpinnerSize() { return spinnerSize; }

    /** 延迟显示毫秒数：请求在 delay 内结束时完全不显示遮罩，避免短请求闪烁。 */
    public void setDelay(int ms) {
        if (ms < 0) throw new IllegalArgumentException("delay must be >= 0: " + ms);
        showDelay = ms;
    }

    public int getDelay() { return showDelay; }

    public void showLoading(String loadingText) {
        if (loadingText == null) loadingText = "";
        this.text = loadingText;
        this.visible = true;
        cancelDelay();
        if (showDelay > 0) {
            // 延迟期内不落笔；到期仍 visible 才开始淡入
            pendingShow = true;
            delayTimer = new Timer(showDelay, new ActionListener() { public void actionPerformed(ActionEvent e) {
                ((Timer) e.getSource()).stop();
                delayTimer = null;
                if (!visible) { pendingShow = false; return; }
                pendingShow = false;
                beginShow();
            }});
            delayTimer.setRepeats(false);
            delayTimer.start();
            return;
        }
        beginShow();
    }

    private void cancelDelay() {
        if (delayTimer != null) { delayTimer.stop(); delayTimer = null; }
        pendingShow = false;
    }

    private void beginShow() {
        // ensure parent sizing in wrap mode (target size + overlay same size)
        if (mode == Mode.WRAP) {
            setSize(getParent() != null ? getParent().getSize() : getPreferredSize());
        }
        fadeAnim.stop();
        if (mode == Mode.WRAP) {
            // 淡入完成后藏起 target：不透明子组件（如 AstProgress）的 paintImmediately
            // 会绕过 paintChildren 里的 overlay 直接上屏，动画透过遮罩闪烁的根因。
            // 遮罩此时已完全不透明，视觉无差别，但 target 不再产生 dirty region。
            fadeAnim.go(overlay, 1f, new Runnable() { public void run() {
                if (visible && target != null && target.isVisible()) {
                    target.setVisible(false);
                    repaint();
                }
            }});
        } else {
            fadeAnim.go(overlay, 1f);
        }
        if (!spinTimer.isRunning()) spinTimer.start();
        // For FULLSCREEN GlassPane usage: we become visible
        if (mode == Mode.FULLSCREEN) {
            setVisible(true);
        }
        // For WRAP mode: setVisible to true (overlay + target both inside self)
        if (mode == Mode.WRAP) {
            setVisible(true);
        }
        requestRepaintAll();
    }

    public void hideLoading() {
        cancelDelay();
        this.visible = false;
        if (mode == Mode.WRAP && target != null && !target.isVisible()) {
            // 恢复 target：遮罩尚在（将淡出），先画回来再渐隐，无闪空帧。
            // BorderLayout 对不可见组件不布局，恢复后需重排。
            target.setVisible(true);
            revalidate();
            doLayout();
        }
        if (spinTimer.isRunning()) spinTimer.stop();
        fadeAnim.stop();
        fadeAnim.go(overlay, 0f, new Runnable() { public void run() {
            if (mode == Mode.FULLSCREEN) {
                setVisible(false);
            }
            requestRepaintAll();
        }});
    }

    private void requestRepaintAll() {
        repaint();
        if (getParent() != null) getParent().repaint();
    }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }

    @Override public Dimension getPreferredSize() {
        if (mode == Mode.WRAP && target != null) return target.getPreferredSize();
        return new Dimension(420, 280);
    }

    @Override public Dimension getMinimumSize() {
        if (mode == Mode.WRAP && target != null) return target.getMinimumSize();
        return new Dimension(160, 140);
    }

    @Override protected void paintChildren(Graphics g) {
        // Wrap mode: paint children (target JComponent) normally FIRST
        if (mode == Mode.WRAP) {
            super.paintChildren(g);
        }
        // Then paint overlay OVER the target / glass pane background
        if (overlay > 0.01f) {
            paintOverlay(g);
        }
    }

    @Override protected void paintComponent(Graphics g) {
        if (mode == Mode.FULLSCREEN) {
            // Fullscreen: paint background mask in paintComponent too (for when no children exist)
            if (overlay > 0.01f) {
                paintOverlayBgOnly(g);
            }
        }
        // Wrap mode: component itself has no background; target is painted in paintChildren
    }

    /** 铺遮罩底色：自定义 setBgColor 优先，否则用模式默认色，alpha 再乘淡入进度。 */
    private void paintMask(Graphics2D g2, int w, int h) {
        Color base = getBgColor();
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(),
                Math.round(base.getAlpha() * overlay)));
        g2.fillRect(0, 0, w, h);
    }

    private void paintOverlayBgOnly(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintMask(g2, getWidth(), getHeight());
        g2.dispose();
    }

    private void paintOverlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int W = getWidth(), H = getHeight();

        // Background mask
        paintMask(g2, W, H);

        // Compute centered 360×360 card for spinner + text (Element UI look)
        int cardW = 220, cardH = 160;
        if (!text.isEmpty()) {
            FontMetrics fm = g2.getFontMetrics(ElementTheme.FONT.deriveFont(14f));
            int tw = Math.min(200, fm.stringWidth(text) + 24);
            cardW = Math.max(220, tw + 40);
        }
        int cardX = (W - cardW) / 2;
        int cardY = (H - cardH) / 2;
        // Wrap mode: transparent rounded light card, not visible separately from bg — skip the card rectangle
        // Fullscreen mode: draw subtle rounded card
        if (mode == Mode.FULLSCREEN) {
            int aCard = Math.round(0xFF * overlay);
            g2.setColor(new Color(0xFF, 0xFF, 0xFF, aCard));
            RoundRectangle2D card = new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, ElementTheme.RADIUS*2, ElementTheme.RADIUS*2);
            g2.fill(card);
            g2.setColor(new Color(ElementTheme.BORDER_BASE.getRed(), ElementTheme.BORDER_BASE.getGreen(), ElementTheme.BORDER_BASE.getBlue(), Math.round(0xFF * overlay)));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(card);
            ElementTheme.assertContrast(ElementTheme.TEXT_REGULAR, Color.WHITE, "AstLoading fullscreen card text on white bg");
        }

        // Spinner: 12-segment rotating arcs (Element UI ring indicator)
        int cx = W / 2;
        int cyBase = (mode == Mode.FULLSCREEN) ? cardY + 56 : H / 2 - (text.isEmpty() ? 0 : 14);
        float scale = spinnerSize / (float) DEFAULT_SPINNER_SIZE;
        int lineLen = Math.max(4, Math.round(spinnerSize * 12f / DEFAULT_SPINNER_SIZE));
        int r = Math.max(lineLen / 2 + 1, spinnerSize / 2 - lineLen / 2); // 半径按外径反推，含线长补偿
        float strokeW = Math.max(2f, 4.5f * scale); // 弧线粗细同步缩放，小尺寸不至于糊成一团
        float spinOffset = angle;

        for (int i = 0; i < 12; i++) {
            float baseT = (float) i / 12f;  // 0..1 going around
            float theta = spinOffset + baseT * 2f * (float) Math.PI;
            // Tail fades: segments ahead of rotation direction are brighter
            float fadeSeg = ((baseT + (spinOffset / (2f * (float) Math.PI))) % 1f);
            // fadeSeg 0 = brightest at leading edge, 1 = dimmest at trailing
            float alphaSeg = 0.12f + 0.88f * (1f - fadeSeg);
            int a = Math.round((0xFF * alphaSeg) * overlay);
            if (a < 2) continue;
            // segment color = PRIMARY (lighter via alpha)
            Color segC = new Color(ElementTheme.PRIMARY.getRed(), ElementTheme.PRIMARY.getGreen(), ElementTheme.PRIMARY.getBlue(), Math.min(255, a));
            g2.setColor(segC);
            g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double cos = Math.cos(theta), sin = Math.sin(theta);
            int x1 = (int) Math.round(cx + (r - lineLen/2f) * cos);
            int y1 = (int) Math.round(cyBase + (r - lineLen/2f) * sin);
            int x2 = (int) Math.round(cx + (r + lineLen/2f) * cos);
            int y2 = (int) Math.round(cyBase + (r + lineLen/2f) * sin);
            g2.drawLine(x1, y1, x2, y2);
        }

        // Text below spinner
        if (!text.isEmpty()) {
            int aText = Math.round(0xFF * 0.85f * overlay);
            Color tc = new Color(ElementTheme.TEXT_REGULAR.getRed(), ElementTheme.TEXT_REGULAR.getGreen(), ElementTheme.TEXT_REGULAR.getBlue(), Math.min(255, Math.max(0, aText)));
            g2.setColor(tc);
            Font f = ElementTheme.FONT.deriveFont(14f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int tb = cyBase + r + 22 + fm.getAscent();
            int tw = fm.stringWidth(text);
            int tx = cx - tw / 2;
            g2.drawString(text, tx, tb);
        }
        g2.dispose();
    }

    /** 离屏渲染遮罩（强制 overlay=1 且停掉淡入动画），用于绘制级断言。 */
    private static java.awt.image.BufferedImage renderOverlay(AstLoading l, int w, int h) {
        l.setSize(w, h);
        l.doLayout();
        l.fadeAnim.stop();
        l.overlay = 1f;
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { l.paint(g); } finally { g.dispose(); }
        return img;
    }

    /** 色相像素外接框宽度：hue 0=蓝（PRIMARY 指示器），1=红。 */
    private static int hueBoxWidth(java.awt.image.BufferedImage img, int hue) {
        int minX = Integer.MAX_VALUE, maxX = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (!isHue(img.getRGB(x, y), hue)) continue;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
            }
        }
        return maxX < 0 ? 0 : (maxX - minX + 1);
    }

    /** 色相像素总数，用于判断弧线粗细/长度的整体变化。 */
    private static int countHue(java.awt.image.BufferedImage img, int hue) {
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (isHue(img.getRGB(x, y), hue)) n++;
            }
        }
        return n;
    }

    private static boolean isHue(int p, int hue) {
        int a = (p >>> 24) & 0xFF;
        if (a < 40) return false;
        int r = (p >>> 16) & 0xFF, g = (p >>> 8) & 0xFF, b = p & 0xFF;
        return hue == 1 ? (r > g + 40 && r > b + 40) : (b > r + 40 && b > g + 20);
    }

    static void selfCheck() {
        // Constructor null checks (can run outside EDT — pure argument validation)
        boolean threw = false;
        try { new AstLoading(Mode.WRAP, null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "WRAP mode null target should throw";
        threw = false;
        try { new AstLoading(null, null); } catch (IllegalArgumentException iae) { threw = true; }
        assert threw : "null mode should throw";

        final boolean[] checks = {false, false, false, false, false, false};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                // WRAP mode: wrap a JLabel
                JLabel lbl = new JLabel("target content");
                lbl.setFont(new Font("Dialog", Font.PLAIN, 14));
                AstLoading wrap = new AstLoading(Mode.WRAP, lbl);
                checks[0] = (wrap.getMode() == Mode.WRAP);
                checks[1] = (wrap.getTarget() == lbl);
                checks[2] = !wrap.isLoadingVisible();

                // FULLSCREEN mode: no target, null allowed
                AstLoading fs = new AstLoading(Mode.FULLSCREEN, null);
                checks[3] = (fs.getMode() == Mode.FULLSCREEN);
                checks[4] = (fs.getTarget() == null);
                checks[5] = fs.getText().isEmpty();

                // Manually set sizes (no layout manager interference)
                wrap.setSize(640, 480);
                wrap.doLayout();
                wrap.showLoading("加载中…");
                // Bypass fade animation: force overlay=1, stop anim timer
                wrap.fadeAnim.stop();
                wrap.overlay = 1f;
                // Offscreen paint — translate graphics to simulate (0,0) origin
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(640, 480, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = img.createGraphics();
                gg.setClip(0, 0, 640, 480);
                try {
                    // Ensure paintChildren uses our 640x480: temporarily force size via Graphics clip + size
                    wrap.setSize(640, 480);
                    wrap.paintChildren(gg);
                } finally { gg.dispose(); }
                int topLeft = img.getRGB(10, 10);
                int tlA = (topLeft >>> 24) & 0xFF;
                assert tlA > 100 : "wrap overlay alpha should be opaqueish after showLoading, got " + tlA
                    + " (W=" + wrap.getWidth() + " H=" + wrap.getHeight() + " overlay=" + wrap.overlay + ")";
                wrap.hideLoading();
                wrap.fadeAnim.stop();
                wrap.overlay = 0f;

                // FULLSCREEN glass pane mode paint offscreen
                fs.setSize(640, 480);
                fs.showLoading("全屏加载");
                fs.fadeAnim.stop();
                fs.overlay = 1f;
                img = new java.awt.image.BufferedImage(640, 480, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                gg = img.createGraphics();
                gg.setClip(0, 0, 640, 480);
                try {
                    fs.setSize(640, 480);
                    fs.paintComponent(gg);
                    fs.paintChildren(gg);
                } finally { gg.dispose(); }
                int bgPx = img.getRGB(320, 240); int bgA = (bgPx >>> 24) & 0xFF;
                assert bgA > 120 : "fullscreen overlay bg mask should render opaque after showLoading, got " + bgA
                    + " (W=" + fs.getWidth() + " H=" + fs.getHeight() + " overlay=" + fs.overlay + ")";
                assert "全屏加载".equals(fs.getText());
                fs.hideLoading();
                fs.fadeAnim.stop();
                fs.overlay = 0f;

                // Additional WRAP API tests on EDT
                wrap.showLoading(null);
                assert "".equals(wrap.getText()) : "null text should default to empty string";
                wrap.hideLoading();
                wrap.fadeAnim.stop();
                assert wrap.getPreferredSize() != null;
                assert wrap.getMinimumSize() != null;

                // --- F3: setBgColor（绘制级）---
                assert wrap.getBgColor().equals(Color.WHITE) : "WRAP default mask is white, got " + wrap.getBgColor();
                java.awt.image.BufferedImage plain = renderOverlay(wrap, 320, 240);
                int plainPx = plain.getRGB(6, 6);
                assert ((plainPx >>> 16) & 0xFF) > 240 && ((plainPx >>> 8) & 0xFF) > 240 && (plainPx & 0xFF) > 240
                    : "default WRAP mask should be white at (6,6), got " + Integer.toHexString(plainPx);
                wrap.setBgColor(new Color(255, 0, 0));
                java.awt.image.BufferedImage red = renderOverlay(wrap, 320, 240);
                int redPx = red.getRGB(6, 6);
                assert ((redPx >>> 16) & 0xFF) > 240 && ((redPx >>> 8) & 0xFF) < 20 && (redPx & 0xFF) < 20
                    : "custom bg color must paint red mask at (6,6), got " + Integer.toHexString(redPx);
                wrap.setBgColor(null);
                assert wrap.getBgColor().equals(Color.WHITE) : "setBgColor(null) restores mode default";
                boolean threwSize = false;
                try { wrap.setSpinnerSize(8); } catch (IllegalArgumentException iae) { threwSize = true; }
                assert threwSize : "spinner size < 16 must throw";

                // --- F3: setSpinnerSize（绘制级：外径随设置变化）---
                wrap.setBgColor(Color.WHITE);
                wrap.setSpinnerSize(DEFAULT_SPINNER_SIZE);
                java.awt.image.BufferedImage bigImg = renderOverlay(wrap, 320, 240);
                wrap.setSpinnerSize(34);
                java.awt.image.BufferedImage smallImg = renderOverlay(wrap, 320, 240);
                int wBig = hueBoxWidth(bigImg, 0);
                int wSmall = hueBoxWidth(smallImg, 0);
                int pxBig = countHue(bigImg, 0);
                int pxSmall = countHue(smallImg, 0);
                assert wBig > 10 : "default spinner should paint visible blue arcs, got " + wBig;
                assert wBig > wSmall + 15 : "spinner 68 must be much wider than 34, got " + wBig + " vs " + wSmall;
                // 半径之外：线段长度与描边粗细也必须随之缩小（否则小尺寸只是变矮、弧线仍又长又粗）
                assert pxBig > pxSmall * 2.2f
                    : "spinner 68 must paint far more arc pixels than 34, got " + pxBig + " vs " + pxSmall;
                wrap.setSpinnerSize(DEFAULT_SPINNER_SIZE);
            }});
        } catch (Throwable t) { throw new RuntimeException(t); }

        // --- F3: setDelay（需跨线程等待真实 Timer）---
        final AstLoading[] dref = new AstLoading[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                dref[0] = new AstLoading(Mode.WRAP, new JLabel("t"));
                dref[0].setDelay(300);
                dref[0].showLoading("延迟显示");
            }});
            Thread.sleep(120);
            final float[] mid = new float[1];
            SwingUtilities.invokeAndWait(new Runnable() { public void run() { mid[0] = dref[0].getOverlayAlpha(); }});
            assert mid[0] == 0f : "before delay elapses overlay must stay 0, got " + mid[0];
            Thread.sleep(500);
            final float[] after = new float[1];
            SwingUtilities.invokeAndWait(new Runnable() { public void run() { after[0] = dref[0].getOverlayAlpha(); }});
            assert after[0] > 0.05f : "after delay elapses overlay must fade in, got " + after[0];

            // 延迟期内 hideLoading → 全程不落笔（防短请求闪烁）
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                dref[0].hideLoading();
                dref[0].fadeAnim.stop();
                dref[0].overlay = 0f;
                dref[0].setDelay(300);
                dref[0].showLoading("延迟显示2");
            }});
            Thread.sleep(120);
            SwingUtilities.invokeAndWait(new Runnable() { public void run() { dref[0].hideLoading(); }});
            Thread.sleep(500);
            final float[] cancelled = new float[1];
            SwingUtilities.invokeAndWait(new Runnable() { public void run() { cancelled[0] = dref[0].getOverlayAlpha(); }});
            assert cancelled[0] == 0f : "cancel within delay must never fade in, got " + cancelled[0];
            boolean threwDelay = false;
            try { dref[0].setDelay(-1); } catch (IllegalArgumentException iae) { threwDelay = true; }
            assert threwDelay : "negative delay must throw";
        } catch (Throwable t) { throw new RuntimeException(t); }
        assert checks[0] : "wrap mode != WRAP";
        assert checks[1] : "wrap target mismatch";
        assert checks[2] : "wrap should not be loading visible initially";
        assert checks[3] : "fs mode != FULLSCREEN";
        assert checks[4] : "fs target should be null";
        assert checks[5] : "fs initial text should be empty";
        System.out.println("AstLoading self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
