package org.swelement.ui;

import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.*;

/**
 * 图标组件 — Element UI Icon 风格的自绘图标库（无图片文件依赖）。
 * 所有图标用 Graphics2D 路径/线条绘制，可指定颜色和尺寸。
 * <p>
 * 用法（新枚举 API）：
 * AstIcon check = new AstIcon(AstIcon.Type.CHECK, ElementTheme.SUCCESS, 16);
 * AstIcon loading = new AstIcon(AstIcon.Type.LOADING, ElementTheme.PRIMARY, 16);
 * loading.setSpinEnabled(true);
 * <p>
 * 零组件复用（任意 JComponent 内部直接绘制）：
 * AstIcon.paintIcon(g, Type.CARET_DOWN, ElementTheme.TEXT_REGULAR, 12, 0f);
 * <p>
 * 图标清单（54 个，0..1 归一化坐标 ×size）：
 * CHECK, CLOSE, ARROW_UP/DOWN/LEFT/RIGHT, PLUS, MINUS, SEARCH, INFO, SUCCESS,
 * WARNING, ERROR, SETTING, USER, EYE, REFRESH, EDIT, DELETE, EYE_OFF,
 * CALENDAR, CLOCK, STAR, STAR_FILLED, BELL, MESSAGE, MORE, MENU, LINK, LOCATION,
 * PHONE, CAMERA, COLLECTION, UPLOAD, DOWNLOAD, LOCK, UNLOCK, SORT, FILTER_FILLED,
 * FULL_SCREEN, COPY, SHARE, PRINT, CIRCLE_CHECK, CIRCLE_CLOSE, CIRCLE_WARNING,
 * CIRCLE_INFO, QUESTION, LOADING, CARET_UP/DOWN/LEFT/RIGHT, DELETE_FILLED。
 * stroke 线宽随 size 缩放，端点圆角；LOADING 可配合 spin 相位旋转。
 */
public class AstIcon extends JComponent {
    // --- Icon type enum (新增 API；序号即旧 int 常量值) ---
    public enum Type {
        CHECK, CLOSE, ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT,
        PLUS, MINUS, SEARCH, INFO, SUCCESS, WARNING, ERROR, SETTING,
        USER, EYE, REFRESH, EDIT, DELETE, EYE_OFF,
        CALENDAR, CLOCK, STAR, STAR_FILLED, BELL, MESSAGE, MORE, MENU,
        LINK, LOCATION, PHONE, CAMERA, COLLECTION, UPLOAD, DOWNLOAD,
        LOCK, UNLOCK, SORT, FILTER_FILLED, FULL_SCREEN, COPY, SHARE, PRINT,
        CIRCLE_CHECK, CIRCLE_CLOSE, CIRCLE_WARNING, CIRCLE_INFO, QUESTION,
        LOADING, CARET_UP, CARET_DOWN, CARET_LEFT, CARET_RIGHT, DELETE_FILLED
    }

    private static final Type[] TYPES = Type.values();

    // --- 旧 int 常量（值 = 枚举序号，向后兼容；自检锁定一致性） ---
    public static final int CHECK = Type.CHECK.ordinal();
    public static final int CLOSE = Type.CLOSE.ordinal();
    public static final int ARROW_UP = Type.ARROW_UP.ordinal();
    public static final int ARROW_DOWN = Type.ARROW_DOWN.ordinal();
    public static final int ARROW_LEFT = Type.ARROW_LEFT.ordinal();
    public static final int ARROW_RIGHT = Type.ARROW_RIGHT.ordinal();
    public static final int PLUS = Type.PLUS.ordinal();
    public static final int MINUS = Type.MINUS.ordinal();
    public static final int SEARCH = Type.SEARCH.ordinal();
    public static final int INFO = Type.INFO.ordinal();
    public static final int SUCCESS = Type.SUCCESS.ordinal();
    public static final int WARNING = Type.WARNING.ordinal();
    public static final int ERROR = Type.ERROR.ordinal();
    public static final int SETTING = Type.SETTING.ordinal();
    public static final int USER = Type.USER.ordinal();
    public static final int EYE = Type.EYE.ordinal();
    public static final int REFRESH = Type.REFRESH.ordinal();
    public static final int EDIT = Type.EDIT.ordinal();
    public static final int DELETE = Type.DELETE.ordinal();
    public static final int EYE_OFF = Type.EYE_OFF.ordinal();

    private Type type;
    private Color color;
    private int size;
    private float spinPhase = 0f;
    private Timer spinTimer;

    public AstIcon(int type) {
        this(type, ElementTheme.TEXT_REGULAR, 16);
    }

    public AstIcon(int type, Color color, int size) {
        this(typeOf(type), color, size);
    }

    public AstIcon(Type type) {
        this(type, ElementTheme.TEXT_REGULAR, 16);
    }

    public AstIcon(Type type, Color color, int size) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (color == null) throw new IllegalArgumentException("color must not be null");
        if (size < 8 || size > 64) throw new IllegalArgumentException("size must be in [8,64]");
        this.type = type;
        this.color = color;
        this.size = size;
        setOpaque(false);
    }

    private static Type typeOf(int t) {
        if (t < 0 || t >= TYPES.length)
            throw new IllegalArgumentException("invalid icon type: " + t);
        return TYPES[t];
    }

    public int getType() {
        return type.ordinal();
    }

    public Type getTypeEnum() {
        return type;
    }

    public Color getColor() {
        return color;
    }

    public int getSizeValue() {
        return size;
    }

    public void setType(int t) {
        setType(typeOf(t));
    }

    public void setType(Type t) {
        if (t == null) throw new IllegalArgumentException("type must not be null");
        this.type = t;
        repaint();
    }

    public void setColor(Color c) {
        if (c == null) throw new IllegalArgumentException("color must not be null");
        this.color = c;
        repaint();
    }

    public void setSizeValue(int s) {
        if (s < 8 || s > 64) throw new IllegalArgumentException("size must be in [8,64]");
        this.size = s;
        revalidate();
        repaint();
    }

    /** LOADING 等图标的旋转动画（默认关）。 */
    public void setSpinEnabled(boolean on) {
        if (on) {
            if (spinTimer == null) {
                spinTimer = new Timer(40, new ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        spinPhase += (float) (Math.PI / 8);
                        if (spinPhase > Math.PI * 2) spinPhase -= Math.PI * 2;
                        repaint();
                    }
                });
            }
            if (!spinTimer.isRunning()) spinTimer.start();
        } else if (spinTimer != null) {
            spinTimer.stop();
        }
    }

    public boolean isSpinRunning() {
        return spinTimer != null && spinTimer.isRunning();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        paintIcon((Graphics2D) g, type, color, size, spinPhase);
    }

    /**
     * 静态绘制器 — 任意组件可零实例调用。
     *
     * @param g    目标画布（调用方坐标原点即图标左上角）
     * @param t    图标类型
     * @param c    前景色（INFO/SUCCESS/WARNING/ERROR 状态图标底色固定走主题色）
     * @param size 边长 [8,64]
     * @param spin 旋转相位（弧度，绕图标中心；LOADING 自旋用，通常传 0）
     */
    public static void paintIcon(Graphics2D g, Type t, Color c, int size, float spin) {
        if (g == null) throw new IllegalArgumentException("graphics must not be null");
        if (t == null) throw new IllegalArgumentException("type must not be null");
        if (c == null) throw new IllegalArgumentException("color must not be null");
        if (size < 8 || size > 64) throw new IllegalArgumentException("size must be in [8,64]");
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        if (spin != 0f) {
            double half = size / 2.0;
            g2.rotate(spin, half, half);
        }
        g2.setColor(c);
        draw(g2, t, (float) size);
        g2.dispose();
    }

    private static void draw(Graphics2D g2, Type t, float s) {
        switch (t) {
            case CHECK: drawCheck(g2, s); break;
            case CLOSE: drawClose(g2, s); break;
            case ARROW_UP: drawArrow(g2, s, 0); break;
            case ARROW_DOWN: drawArrow(g2, s, 1); break;
            case ARROW_LEFT: drawArrow(g2, s, 2); break;
            case ARROW_RIGHT: drawArrow(g2, s, 3); break;
            case PLUS: drawPlusMinus(g2, s, true); break;
            case MINUS: drawPlusMinus(g2, s, false); break;
            case SEARCH: drawSearch(g2, s); break;
            case INFO:
                drawInfoCircle(g2, s, ElementTheme.PRIMARY);
                drawInfo(g2, s);
                break;
            case SUCCESS:
                drawInfoCircle(g2, s, ElementTheme.SUCCESS);
                drawCheckWhite(g2, s);
                break;
            case WARNING: drawTriangle(g2, s, ElementTheme.WARNING); break;
            case ERROR:
                drawInfoCircle(g2, s, ElementTheme.DANGER);
                drawXWhite(g2, s);
                break;
            case SETTING: drawSetting(g2, s); break;
            case USER: drawUser(g2, s); break;
            case EYE: drawEye(g2, s); break;
            case REFRESH: drawRefresh(g2, s); break;
            case EDIT: drawEdit(g2, s); break;
            case DELETE: drawDelete(g2, s); break;
            case EYE_OFF: drawEyeOff(g2, s); break;
            case CALENDAR: drawCalendar(g2, s); break;
            case CLOCK: drawClock(g2, s); break;
            case STAR: drawStar(g2, s, false); break;
            case STAR_FILLED: drawStar(g2, s, true); break;
            case BELL: drawBell(g2, s); break;
            case MESSAGE: drawMessage(g2, s); break;
            case MORE: drawMore(g2, s); break;
            case MENU: drawMenu(g2, s); break;
            case LINK: drawLink(g2, s); break;
            case LOCATION: drawLocation(g2, s); break;
            case PHONE: drawPhone(g2, s); break;
            case CAMERA: drawCamera(g2, s); break;
            case COLLECTION: drawCollection(g2, s); break;
            case UPLOAD: drawUploadDownload(g2, s, true); break;
            case DOWNLOAD: drawUploadDownload(g2, s, false); break;
            case LOCK: drawLock(g2, s, false); break;
            case UNLOCK: drawLock(g2, s, true); break;
            case SORT: drawSort(g2, s); break;
            case FILTER_FILLED: drawFilterFilled(g2, s); break;
            case FULL_SCREEN: drawFullScreen(g2, s); break;
            case COPY: drawCopy(g2, s); break;
            case SHARE: drawShare(g2, s); break;
            case PRINT: drawPrint(g2, s); break;
            case CIRCLE_CHECK: drawCircleGlyph(g2, s, 0); break;
            case CIRCLE_CLOSE: drawCircleGlyph(g2, s, 1); break;
            case CIRCLE_WARNING: drawCircleGlyph(g2, s, 2); break;
            case CIRCLE_INFO: drawCircleGlyph(g2, s, 3); break;
            case QUESTION: drawQuestion(g2, s); break;
            case LOADING: drawLoading(g2, s); break;
            case CARET_UP: drawCaret(g2, s, 0); break;
            case CARET_DOWN: drawCaret(g2, s, 1); break;
            case CARET_LEFT: drawCaret(g2, s, 2); break;
            case CARET_RIGHT: drawCaret(g2, s, 3); break;
            case DELETE_FILLED: drawDeleteFilled(g2, s); break;
            default: break;
        }
    }

    // ============ 原有 20 图标 ============

    private static void stroke(Graphics2D g2, float w) {
        g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    private static void drawCheck(Graphics2D g2, float s) {
        stroke(g2, s * 0.125f);
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.18f, s * 0.5f);
        p.lineTo(s * 0.42f, s * 0.74f);
        p.lineTo(s * 0.82f, s * 0.26f);
        g2.draw(p);
    }

    private static void drawClose(Graphics2D g2, float s) {
        stroke(g2, s * 0.125f);
        float m = s * 0.25f, mx = s * 0.75f;
        g2.draw(new Line2D.Float(m, m, mx, mx));
        g2.draw(new Line2D.Float(m, mx, mx, m));
    }

    private static void drawArrow(Graphics2D g2, float s, int dir) {
        stroke(g2, s * 0.1f);
        Path2D p = new Path2D.Float();
        // dir: 0=up,1=down,2=left,3=right
        float cx = s * 0.5f;
        if (dir == 0) {
            p.moveTo(cx, s * 0.2f);
            p.lineTo(s * 0.2f, s * 0.55f);
            p.moveTo(cx, s * 0.2f);
            p.lineTo(s * 0.8f, s * 0.55f);
            g2.draw(p);
            g2.draw(new Line2D.Float(cx, s * 0.2f, cx, s * 0.8f));
        } else if (dir == 1) {
            p.moveTo(cx, s * 0.8f);
            p.lineTo(s * 0.2f, s * 0.45f);
            p.moveTo(cx, s * 0.8f);
            p.lineTo(s * 0.8f, s * 0.45f);
            g2.draw(p);
            g2.draw(new Line2D.Float(cx, s * 0.2f, cx, s * 0.8f));
        } else if (dir == 2) {
            p.moveTo(s * 0.2f, cx);
            p.lineTo(s * 0.55f, s * 0.2f);
            p.moveTo(s * 0.2f, cx);
            p.lineTo(s * 0.55f, s * 0.8f);
            g2.draw(p);
            g2.draw(new Line2D.Float(s * 0.2f, cx, s * 0.8f, cx));
        } else {
            p.moveTo(s * 0.8f, cx);
            p.lineTo(s * 0.45f, s * 0.2f);
            p.moveTo(s * 0.8f, cx);
            p.lineTo(s * 0.45f, s * 0.8f);
            g2.draw(p);
            g2.draw(new Line2D.Float(s * 0.2f, cx, s * 0.8f, cx));
        }
    }

    private static void drawPlusMinus(Graphics2D g2, float s, boolean plus) {
        stroke(g2, s * 0.1f);
        float m = s * 0.2f, mx = s * 0.8f, cy = s * 0.5f;
        g2.draw(new Line2D.Float(m, cy, mx, cy));
        if (plus) g2.draw(new Line2D.Float(cx(s), m, cx(s), mx));
    }

    private static float cx(float s) {
        return s * 0.5f;
    }

    private static void drawSearch(Graphics2D g2, float s) {
        stroke(g2, s * 0.1f);
        float r = s * 0.28f;
        float cx = s * 0.42f, cy = s * 0.42f;
        g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        g2.draw(new Line2D.Float(cx + r * 0.7f, cy + r * 0.7f, s * 0.82f, s * 0.82f));
    }

    private static void drawInfo(Graphics2D g2, float s) {
        Color save = g2.getColor();
        // "i" dot + stem
        g2.setColor(Color.WHITE);
        float w = s * 0.1f;
        g2.fill(new RoundRectangle2D.Float(cx(s) - w / 2, s * 0.24f, w, s * 0.18f, w, w));
        g2.fill(new RoundRectangle2D.Float(cx(s) - w / 2, s * 0.5f, w, s * 0.26f, w, w));
        g2.setColor(save);
    }

    private static void drawInfoCircle(Graphics2D g2, float s, Color bg) {
        g2.setColor(bg);
        g2.fill(new Ellipse2D.Float(s * 0.06f, s * 0.06f, s * 0.88f, s * 0.88f));
    }

    private static void drawCheckWhite(Graphics2D g2, float s) {
        g2.setColor(Color.WHITE);
        stroke(g2, s * 0.12f);
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.3f, s * 0.52f);
        p.lineTo(s * 0.45f, s * 0.66f);
        p.lineTo(s * 0.72f, s * 0.34f);
        g2.draw(p);
    }

    private static void drawXWhite(Graphics2D g2, float s) {
        g2.setColor(Color.WHITE);
        stroke(g2, s * 0.12f);
        g2.draw(new Line2D.Float(s * 0.34f, s * 0.34f, s * 0.66f, s * 0.66f));
        g2.draw(new Line2D.Float(s * 0.34f, s * 0.66f, s * 0.66f, s * 0.34f));
    }

    private static void drawTriangle(Graphics2D g2, float s, Color bg) {
        Color save = g2.getColor();
        g2.setColor(bg);
        Path2D p = new Path2D.Float();
        p.moveTo(cx(s), s * 0.1f);
        p.lineTo(s * 0.92f, s * 0.84f);
        p.lineTo(s * 0.08f, s * 0.84f);
        p.closePath();
        g2.fill(p);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(cx(s) - s * 0.05f, s * 0.34f, s * 0.1f, s * 0.26f, s * 0.1f, s * 0.1f));
        g2.fill(new Ellipse2D.Float(cx(s) - s * 0.05f, s * 0.66f, s * 0.1f, s * 0.1f));
        g2.setColor(save);
    }

    private static void drawSetting(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float cx = cx(s), cy = cx(s);
        float rOut = s * 0.38f, rIn = s * 0.16f;
        int teeth = 8;
        Path2D p = new Path2D.Float();
        for (int i = 0; i < teeth * 2; i++) {
            double ang = (Math.PI * i) / teeth;
            float r = (i % 2 == 0) ? rOut : rOut * 0.78f;
            float x = cx + (float) Math.cos(ang) * r;
            float y = cy + (float) Math.sin(ang) * r;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.closePath();
        g2.draw(p);
        g2.draw(new Ellipse2D.Float(cx - rIn, cy - rIn, rIn * 2, rIn * 2));
    }

    private static void drawUser(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float cx = cx(s);
        // head
        float hr = s * 0.16f;
        g2.draw(new Ellipse2D.Float(cx - hr, s * 0.18f, hr * 2, hr * 2));
        // shoulders
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.18f, s * 0.82f);
        p.curveTo(s * 0.22f, s * 0.5f, s * 0.78f, s * 0.5f, s * 0.82f, s * 0.82f);
        g2.draw(p);
    }

    private static void drawEye(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float cy = cx(s);
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.1f, cy);
        p.curveTo(s * 0.3f, s * 0.2f, s * 0.7f, s * 0.2f, s * 0.9f, cy);
        p.curveTo(s * 0.7f, s * 0.8f, s * 0.3f, s * 0.8f, s * 0.1f, cy);
        g2.draw(p);
        float pr = s * 0.1f;
        g2.draw(new Ellipse2D.Float(cy - pr, cy - pr, pr * 2, pr * 2));
    }

    private static void drawEyeOff(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float cy = cx(s);
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.1f, cy);
        p.curveTo(s * 0.3f, s * 0.2f, s * 0.7f, s * 0.2f, s * 0.9f, cy);
        p.curveTo(s * 0.7f, s * 0.8f, s * 0.3f, s * 0.8f, s * 0.1f, cy);
        g2.draw(p);
        // 斜杠贯穿（闭眼）
        g2.draw(new Line2D.Float(s * 0.14f, s * 0.84f, s * 0.86f, s * 0.16f));
    }

    private static void drawRefresh(Graphics2D g2, float s) {
        stroke(g2, s * 0.1f);
        float cx = cx(s), cy = cx(s);
        double r = s * 0.32f;
        // arc 270°
        Arc2D arc = new Arc2D.Float(cx - (float) r, cy - (float) r, (float) (2 * r), (float) (2 * r), 30, 270, Arc2D.OPEN);
        g2.draw(arc);
        // arrow head at end
        Path2D ah = new Path2D.Float();
        float ex = cx + (float) (r * Math.cos(Math.toRadians(60 + 270)));
        float ey = cy + (float) (r * Math.sin(Math.toRadians(60 + 270)));
        ah.moveTo(ex, ey);
        ah.lineTo(ex - s * 0.02f, ey - s * 0.44f);
        ah.moveTo(ex, ey);
        ah.lineTo(ex - s * 0.24f, ey + s * 0.12f);
        g2.draw(ah);
    }

    private static void drawEdit(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        // pencil: diagonal
        g2.draw(new Line2D.Float(s * 0.18f, s * 0.86f, s * 0.16f, s * 0.66f));
        g2.draw(new Line2D.Float(s * 0.16f, s * 0.66f, s * 0.49f, s * 0.14f));
        g2.draw(new Line2D.Float(s * 0.49f, s * 0.14f, s * 0.72f, s * 0.27f));
        g2.draw(new Line2D.Float(s * 0.72f, s * 0.27f, s * 0.38f, s * 0.78f));
        g2.draw(new Line2D.Float(s * 0.38f, s * 0.78f, s * 0.18f, s * 0.86f));

        g2.draw(new Line2D.Float(s * 0.41f, s * 0.26f, s * 0.63f, s * 0.39f));
        g2.draw(new Line2D.Float(s * 0.48f, s * 0.87f, s * 0.89f, s * 0.87f));
    }

    private static void drawDelete(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float cx = cx(s);
        // lid
        g2.draw(new Line2D.Float(s * 0.2f, s * 0.24f, s * 0.8f, s * 0.24f));
        // handle
        g2.draw(new Line2D.Float(s * 0.4f, s * 0.24f, s * 0.4f, s * 0.16f));
        g2.draw(new Line2D.Float(s * 0.6f, s * 0.24f, s * 0.6f, s * 0.16f));
        g2.draw(new Line2D.Float(s * 0.4f, s * 0.16f, s * 0.6f, s * 0.16f));
        // body sides
        g2.draw(new Line2D.Float(s * 0.28f, s * 0.24f, s * 0.32f, s * 0.84f));
        g2.draw(new Line2D.Float(s * 0.72f, s * 0.24f, s * 0.68f, s * 0.84f));
        g2.draw(new Line2D.Float(s * 0.32f, s * 0.84f, s * 0.68f, s * 0.84f));
        // inner lines
        g2.draw(new Line2D.Float(s * 0.42f, s * 0.36f, s * 0.42f, s * 0.72f));
        g2.draw(new Line2D.Float(s * 0.58f, s * 0.36f, s * 0.58f, s * 0.72f));
    }

    // ============ 新增 34 图标（0..1 归一化坐标） ============

    private static void drawCalendar(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        // 外框
        g2.draw(new RoundRectangle2D.Float(s * 0.1f, s * 0.16f, s * 0.8f, s * 0.74f, s * 0.08f, s * 0.08f));
        // 顶部两个挂环
        g2.draw(new Line2D.Float(s * 0.3f, s * 0.08f, s * 0.3f, s * 0.22f));
        g2.draw(new Line2D.Float(s * 0.7f, s * 0.08f, s * 0.7f, s * 0.22f));
        // 表头分隔线
        g2.draw(new Line2D.Float(s * 0.1f, s * 0.36f, s * 0.9f, s * 0.36f));
        // 日期点
        float d = s * 0.07f;
        g2.fill(new Ellipse2D.Float(s * 0.3f - d / 2, s * 0.52f - d / 2, d, d));
        g2.fill(new Ellipse2D.Float(s * 0.5f - d / 2, s * 0.52f - d / 2, d, d));
        g2.fill(new Ellipse2D.Float(s * 0.3f - d / 2, s * 0.7f - d / 2, d, d));
        g2.fill(new Ellipse2D.Float(s * 0.5f - d / 2, s * 0.7f - d / 2, d, d));
    }

    private static void drawClock(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        float r = s * 0.38f;
        g2.draw(new Ellipse2D.Float(cx(s) - r, cx(s) - r, r * 2, r * 2));
        // 指针
        g2.draw(new Line2D.Float(cx(s), cx(s), cx(s), s * 0.28f));
        g2.draw(new Line2D.Float(cx(s), cx(s), s * 0.66f, cx(s) + s * 0.06f));
    }

    private static void drawStar(Graphics2D g2, float s, boolean filled) {
        Path2D p = new Path2D.Float();
        float cx = cx(s), cy = cx(s);
        float outer = s * 0.4f, inner = s * 0.17f;
        for (int i = 0; i < 10; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / 5;
            float r = (i % 2 == 0) ? outer : inner;
            float x = cx + (float) Math.cos(ang) * r;
            float y = cy + (float) Math.sin(ang) * r;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.closePath();
        if (filled) g2.fill(p);
        else {
            stroke(g2, s * 0.08f);
            g2.draw(p);
        }
    }

    private static void drawBell(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        // 钟身
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.2f, s * 0.72f);
        p.curveTo(s * 0.22f, s * 0.3f, s * 0.34f, s * 0.16f, s * 0.5f, s * 0.16f);
        p.curveTo(s * 0.66f, s * 0.16f, s * 0.78f, s * 0.3f, s * 0.8f, s * 0.72f);
        p.closePath();
        g2.draw(p);
        // 底沿 + 铃锤
        g2.draw(new Line2D.Float(s * 0.12f, s * 0.72f, s * 0.88f, s * 0.72f));
        g2.draw(new Arc2D.Float(cx(s) - s * 0.08f, s * 0.72f, s * 0.16f, s * 0.14f, 0, 180, Arc2D.OPEN));
    }

    private static void drawMessage(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        // 气泡 + 尾巴
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.2f, s * 0.2f);
        p.lineTo(s * 0.8f, s * 0.2f);
        p.curveTo(s * 0.88f, s * 0.2f, s * 0.88f, s * 0.26f, s * 0.88f, s * 0.32f);
        p.lineTo(s * 0.88f, s * 0.56f);
        p.curveTo(s * 0.88f, s * 0.62f, s * 0.88f, s * 0.68f, s * 0.8f, s * 0.68f);
        p.lineTo(s * 0.42f, s * 0.68f);
        p.lineTo(s * 0.26f, s * 0.84f);
        p.lineTo(s * 0.26f, s * 0.68f);
        p.lineTo(s * 0.2f, s * 0.68f);
        p.curveTo(s * 0.12f, s * 0.68f, s * 0.12f, s * 0.62f, s * 0.12f, s * 0.56f);
        p.lineTo(s * 0.12f, s * 0.32f);
        p.curveTo(s * 0.12f, s * 0.26f, s * 0.12f, s * 0.2f, s * 0.2f, s * 0.2f);
        p.closePath();
        g2.draw(p);
        // 三点
        float d = s * 0.06f;
        for (int i = 0; i < 3; i++)
            g2.fill(new Ellipse2D.Float(s * 0.3f + i * s * 0.2f - d / 2, s * 0.44f - d / 2, d, d));
    }

    private static void drawMore(Graphics2D g2, float s) {
        float d = s * 0.12f;
        for (int i = 0; i < 3; i++)
            g2.fill(new Ellipse2D.Float(cx(s) + (i - 1) * s * 0.24f - d / 2, cx(s) - d / 2, d, d));
    }

    private static void drawMenu(Graphics2D g2, float s) {
        stroke(g2, s * 0.1f);
        g2.draw(new Line2D.Float(s * 0.14f, s * 0.28f, s * 0.86f, s * 0.28f));
        g2.draw(new Line2D.Float(s * 0.14f, s * 0.5f, s * 0.86f, s * 0.5f));
        g2.draw(new Line2D.Float(s * 0.14f, s * 0.72f, s * 0.86f, s * 0.72f));
    }

    private static void drawLink(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        // 两个斜向链环
        Path2D ring = new Path2D.Float();
        ring.moveTo(s * 0.58f, s * 0.18f);
        ring.lineTo(s * 0.82f, s * 0.42f);
        ring.curveTo(s * 0.94f, s * 0.54f, s * 0.78f, s * 0.7f, s * 0.66f, s * 0.58f);
        ring.lineTo(s * 0.42f, s * 0.34f);
        ring.curveTo(s * 0.3f, s * 0.22f, s * 0.46f, s * 0.06f, s * 0.58f, s * 0.18f);
        ring.closePath();
        g2.draw(ring);
        Path2D ring2 = new Path2D.Float();
        ring2.moveTo(s * 0.42f, s * 0.82f);
        ring2.lineTo(s * 0.18f, s * 0.58f);
        ring2.curveTo(s * 0.06f, s * 0.46f, s * 0.22f, s * 0.3f, s * 0.34f, s * 0.42f);
        ring2.lineTo(s * 0.58f, s * 0.66f);
        ring2.curveTo(s * 0.7f, s * 0.78f, s * 0.54f, s * 0.94f, s * 0.42f, s * 0.82f);
        ring2.closePath();
        g2.draw(ring2);
    }

    private static void drawLocation(Graphics2D g2, float s) {
        // 大头针：圆头 + 尖尾
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.5f, s * 0.92f);
        p.curveTo(s * 0.16f, s * 0.58f, s * 0.14f, s * 0.4f, s * 0.14f, s * 0.34f);
        p.curveTo(s * 0.14f, s * 0.16f, s * 0.3f, s * 0.06f, s * 0.5f, s * 0.06f);
        p.curveTo(s * 0.7f, s * 0.06f, s * 0.86f, s * 0.16f, s * 0.86f, s * 0.34f);
        p.curveTo(s * 0.86f, s * 0.4f, s * 0.84f, s * 0.58f, s * 0.5f, s * 0.92f);
        p.closePath();
        g2.fill(p);
        // 中心挖孔（透明，适配任意底色）
        punch(g2, new Ellipse2D.Float(cx(s) - s * 0.1f, s * 0.32f, s * 0.2f, s * 0.2f));
    }

    /** 用 Clear 组合模式挖透明孔（任意底色下均正确）。 */
    private static void punch(Graphics2D g2, Shape hole) {
        Composite save = g2.getComposite();
        g2.setComposite(AlphaComposite.Clear);
        g2.fill(hole);
        g2.setComposite(save);
    }

    private static void drawPhone(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        // 话筒
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.2f, s * 0.32f);
        p.curveTo(s * 0.2f, s * 0.2f, s * 0.32f, s * 0.12f, s * 0.4f, s * 0.2f);
        p.lineTo(s * 0.48f, s * 0.32f);
        p.curveTo(s * 0.54f, s * 0.38f, s * 0.5f, s * 0.46f, s * 0.44f, s * 0.52f);
        p.curveTo(s * 0.5f, s * 0.62f, s * 0.6f, s * 0.72f, s * 0.72f, s * 0.78f);
        p.curveTo(s * 0.78f, s * 0.72f, s * 0.86f, s * 0.68f, s * 0.9f, s * 0.76f);
        p.lineTo(s * 0.94f, s * 0.86f);
        p.curveTo(s * 0.9f, s * 0.94f, s * 0.76f, s * 0.92f, s * 0.62f, s * 0.82f);
        p.curveTo(s * 0.44f, s * 0.68f, s * 0.28f, s * 0.5f, s * 0.2f, s * 0.32f);
        p.closePath();
        g2.draw(p);
    }

    private static void drawCamera(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        // 机身
        g2.draw(new RoundRectangle2D.Float(s * 0.1f, s * 0.28f, s * 0.8f, s * 0.6f, s * 0.08f, s * 0.08f));
        // 顶部取景窗
        g2.draw(new RoundRectangle2D.Float(s * 0.36f, s * 0.16f, s * 0.28f, s * 0.12f, s * 0.04f, s * 0.04f));
        // 镜头
        float r = s * 0.16f;
        g2.draw(new Ellipse2D.Float(cx(s) - r, s * 0.58f - r, r * 2, r * 2));
    }

    private static void drawCollection(Graphics2D g2, float s) {
        // 书签（收藏）
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.24f, s * 0.1f);
        p.lineTo(s * 0.76f, s * 0.1f);
        p.lineTo(s * 0.76f, s * 0.92f);
        p.lineTo(s * 0.5f, s * 0.68f);
        p.lineTo(s * 0.24f, s * 0.92f);
        p.closePath();
        g2.fill(p);
    }

    private static void drawUploadDownload(Graphics2D g2, float s, boolean up) {
        stroke(g2, s * 0.1f);
        // 箭头
        Path2D a = new Path2D.Float();
        if (up) {
            a.moveTo(cx(s), s * 0.12f);
            a.lineTo(s * 0.28f, s * 0.42f);
            a.moveTo(cx(s), s * 0.12f);
            a.lineTo(s * 0.72f, s * 0.42f);
            g2.draw(a);
            g2.draw(new Line2D.Float(cx(s), s * 0.12f, cx(s), s * 0.62f));
        } else {
            a.moveTo(cx(s), s * 0.62f);
            a.lineTo(s * 0.28f, s * 0.32f);
            a.moveTo(cx(s), s * 0.62f);
            a.lineTo(s * 0.72f, s * 0.32f);
            g2.draw(a);
            g2.draw(new Line2D.Float(cx(s), s * 0.12f, cx(s), s * 0.62f));
        }
        // 托盘
        g2.draw(new Line2D.Float(s * 0.16f, s * 0.78f, s * 0.34f, s * 0.78f));
        g2.draw(new Line2D.Float(s * 0.66f, s * 0.78f, s * 0.84f, s * 0.78f));
    }

    private static void drawLock(Graphics2D g2, float s, boolean open) {
        stroke(g2, s * 0.09f);
        // 锁体
        g2.draw(new RoundRectangle2D.Float(s * 0.22f, s * 0.44f, s * 0.56f, s * 0.44f, s * 0.06f, s * 0.06f));
        // 锁梁
        if (open) {
            g2.draw(new Arc2D.Float(s * 0.54f, s * 0.1f, s * 0.24f, s * 0.4f, 0, 180, Arc2D.OPEN));
            g2.draw(new Line2D.Float(s * 0.54f, s * 0.3f, s * 0.54f, s * 0.44f));
            g2.draw(new Line2D.Float(s * 0.78f, s * 0.3f, s * 0.78f, s * 0.44f));
        } else {
            g2.draw(new Arc2D.Float(s * 0.32f, s * 0.1f, s * 0.36f, s * 0.4f, 0, 180, Arc2D.OPEN));
            g2.draw(new Line2D.Float(s * 0.32f, s * 0.3f, s * 0.32f, s * 0.44f));
            g2.draw(new Line2D.Float(s * 0.68f, s * 0.3f, s * 0.68f, s * 0.44f));
        }
        // 锁孔
        float d = s * 0.07f;
        g2.fill(new Ellipse2D.Float(cx(s) - d / 2, s * 0.56f, d, d));
        g2.draw(new Line2D.Float(cx(s), s * 0.62f, cx(s), s * 0.72f));
    }

    private static void drawSort(Graphics2D g2, float s) {
        stroke(g2, s * 0.08f);
        // 左侧两横线
        g2.draw(new Line2D.Float(s * 0.08f, s * 0.3f, s * 0.5f, s * 0.3f));
        g2.draw(new Line2D.Float(s * 0.08f, s * 0.62f, s * 0.36f, s * 0.62f));
        // 右侧上下三角
        Path2D up = new Path2D.Float();
        up.moveTo(s * 0.74f, s * 0.08f);
        up.lineTo(s * 0.62f, s * 0.26f);
        up.lineTo(s * 0.86f, s * 0.26f);
        up.closePath();
        g2.fill(up);
        Path2D down = new Path2D.Float();
        down.moveTo(s * 0.74f, s * 0.92f);
        down.lineTo(s * 0.62f, s * 0.74f);
        down.lineTo(s * 0.86f, s * 0.74f);
        down.closePath();
        g2.fill(down);
    }

    private static void drawFilterFilled(Graphics2D g2, float s) {
        // 实心漏斗
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.08f, s * 0.14f);
        p.lineTo(s * 0.92f, s * 0.14f);
        p.lineTo(s * 0.62f, s * 0.5f);
        p.lineTo(s * 0.62f, s * 0.9f);
        p.lineTo(s * 0.38f, s * 0.78f);
        p.lineTo(s * 0.38f, s * 0.5f);
        p.closePath();
        g2.fill(p);
    }

    private static void drawFullScreen(Graphics2D g2, float s) {
        stroke(g2, s * 0.1f);
        float m = s * 0.1f, mx = s * 0.9f, arm = s * 0.22f;
        // 左上
        g2.draw(new Line2D.Float(m, m + arm, m, m));
        g2.draw(new Line2D.Float(m, m, m + arm, m));
        // 右上
        g2.draw(new Line2D.Float(mx - arm, m, mx, m));
        g2.draw(new Line2D.Float(mx, m, mx, m + arm));
        // 左下
        g2.draw(new Line2D.Float(m, mx - arm, m, mx));
        g2.draw(new Line2D.Float(m, mx, m + arm, mx));
        // 右下
        g2.draw(new Line2D.Float(mx - arm, mx, mx, mx));
        g2.draw(new Line2D.Float(mx, mx - arm, mx, mx));
    }

    private static void drawCopy(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        // 后卡
        g2.draw(new RoundRectangle2D.Float(s * 0.3f, s * 0.08f, s * 0.6f, s * 0.6f, s * 0.06f, s * 0.06f));
        // 前卡
        g2.draw(new RoundRectangle2D.Float(s * 0.1f, s * 0.32f, s * 0.6f, s * 0.6f, s * 0.06f, s * 0.06f));
        // 前卡内容线
        g2.draw(new Line2D.Float(s * 0.22f, s * 0.5f, s * 0.56f, s * 0.5f));
        g2.draw(new Line2D.Float(s * 0.22f, s * 0.64f, s * 0.56f, s * 0.64f));
    }

    private static void drawShare(Graphics2D g2, float s) {
        stroke(g2, s * 0.07f);
        float r = s * 0.11f;
        float[][] nodes = {{s * 0.5f, s * 0.14f}, {s * 0.16f, s * 0.7f}, {s * 0.84f, s * 0.7f}};
        // 连线
        g2.draw(new Line2D.Float(nodes[0][0], nodes[0][1] + r, nodes[1][0] + r * 0.5f, nodes[1][1] - r));
        g2.draw(new Line2D.Float(nodes[0][0], nodes[0][1] + r, nodes[2][0] - r * 0.5f, nodes[2][1] - r));
        // 节点
        for (float[] n : nodes)
            g2.fill(new Ellipse2D.Float(n[0] - r, n[1] - r, r * 2, r * 2));
    }

    private static void drawPrint(Graphics2D g2, float s) {
        stroke(g2, s * 0.09f);
        // 出纸
        g2.draw(new RoundRectangle2D.Float(s * 0.24f, s * 0.08f, s * 0.52f, s * 0.24f, s * 0.04f, s * 0.04f));
        // 机身
        g2.draw(new RoundRectangle2D.Float(s * 0.1f, s * 0.3f, s * 0.8f, s * 0.34f, s * 0.06f, s * 0.06f));
        // 进纸
        g2.draw(new RoundRectangle2D.Float(s * 0.26f, s * 0.64f, s * 0.48f, s * 0.28f, s * 0.04f, s * 0.04f));
        // 按钮点
        float d = s * 0.07f;
        g2.fill(new Ellipse2D.Float(s * 0.76f - d / 2, s * 0.42f - d / 2, d, d));
    }

    /** 带圆圈的状态图标。glyph: 0=√ 1=× 2=! 3=i（前景色线条版）。 */
    private static void drawCircleGlyph(Graphics2D g2, float s, int glyph) {
        stroke(g2, s * 0.08f);
        float r = s * 0.4f;
        g2.draw(new Ellipse2D.Float(cx(s) - r, cx(s) - r, r * 2, r * 2));
        stroke(g2, s * 0.1f);
        if (glyph == 0) { // √
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.34f, s * 0.52f);
            p.lineTo(s * 0.46f, s * 0.64f);
            p.lineTo(s * 0.68f, s * 0.36f);
            g2.draw(p);
        } else if (glyph == 1) { // ×
            g2.draw(new Line2D.Float(s * 0.36f, s * 0.36f, s * 0.64f, s * 0.64f));
            g2.draw(new Line2D.Float(s * 0.36f, s * 0.64f, s * 0.64f, s * 0.36f));
        } else if (glyph == 2) { // !
            g2.draw(new Line2D.Float(cx(s), s * 0.32f, cx(s), s * 0.56f));
            float d = s * 0.09f;
            g2.fill(new Ellipse2D.Float(cx(s) - d / 2, s * 0.62f, d, d));
        } else { // i
            g2.draw(new Line2D.Float(cx(s), s * 0.44f, cx(s), s * 0.66f));
            float d = s * 0.09f;
            g2.fill(new Ellipse2D.Float(cx(s) - d / 2, s * 0.28f, d, d));
        }
    }

    private static void drawQuestion(Graphics2D g2, float s) {
        stroke(g2, s * 0.08f);
        float r = s * 0.4f;
        g2.draw(new Ellipse2D.Float(cx(s) - r, cx(s) - r, r * 2, r * 2));
        // 问号钩
        Path2D p = new Path2D.Float();
        p.moveTo(s * 0.38f, s * 0.42f);
        p.curveTo(s * 0.38f, s * 0.26f, s * 0.62f, s * 0.26f, s * 0.62f, s * 0.42f);
        p.curveTo(s * 0.62f, s * 0.54f, s * 0.5f, s * 0.52f, s * 0.5f, s * 0.62f);
        stroke(g2, s * 0.1f);
        g2.draw(p);
        float d = s * 0.09f;
        g2.fill(new Ellipse2D.Float(cx(s) - d / 2, s * 0.68f, d, d));
    }

    private static void drawLoading(Graphics2D g2, float s) {
        stroke(g2, s * 0.1f);
        float r = s * 0.36f;
        // 270° 开口环（配合 spin 相位旋转）
        g2.draw(new Arc2D.Float(cx(s) - r, cx(s) - r, r * 2, r * 2, 0, 270, Arc2D.OPEN));
    }

    /** 实心小三角（表格排序指示等）。dir: 0=up 1=down 2=left 3=right。 */
    private static void drawCaret(Graphics2D g2, float s, int dir) {
        Path2D p = new Path2D.Float();
        float cx = cx(s), cy = cx(s), w = s * 0.3f, h = s * 0.22f;
        if (dir == 0) {
            p.moveTo(cx, cy - h);
            p.lineTo(cx - w, cy + h);
            p.lineTo(cx + w, cy + h);
        } else if (dir == 1) {
            p.moveTo(cx, cy + h);
            p.lineTo(cx - w, cy - h);
            p.lineTo(cx + w, cy - h);
        } else if (dir == 2) {
            p.moveTo(cx - h, cy);
            p.lineTo(cx + h, cy - w);
            p.lineTo(cx + h, cy + w);
        } else {
            p.moveTo(cx + h, cy);
            p.lineTo(cx - h, cy - w);
            p.lineTo(cx - h, cy + w);
        }
        p.closePath();
        g2.fill(p);
    }

    private static void drawDeleteFilled(Graphics2D g2, float s) {
        // 实心垃圾桶
        Path2D body = new Path2D.Float();
        body.moveTo(s * 0.28f, s * 0.26f);
        body.lineTo(s * 0.72f, s * 0.26f);
        body.lineTo(s * 0.68f, s * 0.9f);
        body.lineTo(s * 0.32f, s * 0.9f);
        body.closePath();
        g2.fill(body);
        // 盖
        g2.fill(new RoundRectangle2D.Float(s * 0.2f, s * 0.14f, s * 0.6f, s * 0.1f, s * 0.05f, s * 0.05f));
        g2.fill(new RoundRectangle2D.Float(s * 0.42f, s * 0.06f, s * 0.16f, s * 0.1f, s * 0.04f, s * 0.04f));
        // 挖空竖条（透明）
        punch(g2, new RoundRectangle2D.Float(s * 0.42f, s * 0.38f, s * 0.06f, s * 0.4f, s * 0.03f, s * 0.03f));
        punch(g2, new RoundRectangle2D.Float(s * 0.54f, s * 0.38f, s * 0.06f, s * 0.4f, s * 0.03f, s * 0.03f));
    }

    // --- Self-check ---
    static void selfCheck() {
        boolean threw = false;
        try {
            new AstIcon(-1);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assert threw : "bad type";
        threw = false;
        try {
            new AstIcon(TYPES.length);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assert threw : "type out of range";
        threw = false;
        try {
            new AstIcon(CHECK, null, 16);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assert threw : "null color";
        threw = false;
        try {
            new AstIcon(CHECK, Color.BLACK, 4);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assert threw : "size too small";
        threw = false;
        try {
            new AstIcon(CHECK, Color.BLACK, 99);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assert threw : "size too large";
        threw = false;
        try {
            new AstIcon(CHECK).setType(99);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assert threw : "setType bad";
        threw = false;
        try {
            new AstIcon(CHECK).setColor(null);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assert threw : "setColor null";
        threw = false;
        try {
            new AstIcon(CHECK).setSizeValue(4);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assert threw : "setSizeValue too small";
        threw = false;
        try {
            new AstIcon(null);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assert threw : "null enum type";
        threw = false;
        try {
            AstIcon.paintIcon(null, Type.CHECK, Color.BLACK, 16, 0f);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assert threw : "paintIcon null graphics";

        // 旧 int 常量与枚举序号一致（向后兼容锁定）
        assert CHECK == 0 && CLOSE == 1 && ARROW_UP == 2 && ARROW_DOWN == 3
            && ARROW_LEFT == 4 && ARROW_RIGHT == 5 && PLUS == 6 && MINUS == 7
            && SEARCH == 8 && INFO == 9 && SUCCESS == 10 && WARNING == 11
            && ERROR == 12 && SETTING == 13 && USER == 14 && EYE == 15
            && REFRESH == 16 && EDIT == 17 && DELETE == 18 && EYE_OFF == 19
            : "legacy int constants must keep original values";
        // 旧 int 构造器与枚举构造器等价
        assert new AstIcon(CLOSE).getTypeEnum() == Type.CLOSE : "int ctor must map to enum";
        assert new AstIcon(Type.LOADING).getType() == Type.LOADING.ordinal() : "enum ctor ordinal";
        assert TYPES.length == 54 : "icon count, got " + TYPES.length;

        // 每个图标绘制非空（对比白底，非白像素 > 3）
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    for (Type t : TYPES) {
                        java.awt.image.BufferedImage img =
                            new java.awt.image.BufferedImage(24, 24, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        Graphics2D gg = img.createGraphics();
                        gg.setColor(Color.WHITE);
                        gg.fillRect(0, 0, 24, 24);
                        try {
                            paintIcon(gg, t, ElementTheme.PRIMARY, 24, 0f);
                        } finally {
                            gg.dispose();
                        }
                        int nonWhite = 0;
                        for (int x = 0; x < 24; x++)
                            for (int y = 0; y < 24; y++) {
                                int p = img.getRGB(x, y);
                                if (((p >> 16) & 0xFF) < 200 || ((p >> 8) & 0xFF) < 200 || (p & 0xFF) < 200) nonWhite++;
                            }
                        assert nonWhite > 3 : "icon " + t + " should draw visible pixels, nonWhite=" + nonWhite;
                    }
                }
            });
        } catch (Throwable t) {
            err[0] = t;
        }
        if (err[0] != null) throw new RuntimeException(err[0]);

        // spin：启用后相位推进（LOADING 旋转）
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    AstIcon ld = new AstIcon(Type.LOADING, ElementTheme.PRIMARY, 20);
                    ld.setBounds(0, 0, 20, 20);
                    assert !ld.isSpinRunning() : "spin default off";
                    ld.setSpinEnabled(true);
                    assert ld.isSpinRunning() : "spin should start";
                    ld.setSpinEnabled(false);
                    assert !ld.isSpinRunning() : "spin should stop";
                }
            });
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        System.out.println("AstIcon self-check OK (" + TYPES.length + " icons)");
    }

    public static void main(String[] args) {
        selfCheck();
    }
}
