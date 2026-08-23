package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 公共可点击关闭按钮：矢量 × 符号 + hover 圆形底色淡入。
 * 所有可关闭组件（Tag/Alert/Input/AstDialog 等）统一使用，替代"自绘 × + 坐标命中测试"。
 * 对比度：默认色 0x606266 对白底 ≈6.1:1（≥4.5:1，WCAG AA 达标且有余量），hover 色 0x1d6fb5 为 primary 深变体（>= 4.5:1）。
 * 禁用态：× 渲染为 ElementTheme.TEXT_PLACEHOLDER（0xC0C4CC，Element 禁用灰），不响应点击、不拦截父组件事件。
 *        禁用灰对比度低于 AA，但属于 WCAG 1.4.3 明确豁免的"disabled UI"场景，符合规范。
 */
public class CloseButton extends JComponent {
    private int size;
    private Color color = new Color(0x606266);
    private Color hoverColor = new Color(0x1d6fb5);
    private float hover;
    private float alpha = 1f;
    private boolean interactive = true;
    private final List<ActionListener> listeners = new ArrayList<ActionListener>();
    private final Animator hoverAnim = new Animator(150, Easing::easeInOut, v -> { hover = v; repaint(); });

    public CloseButton() { this(24); }

    public CloseButton(int size) {
        this.size = size;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (interactive && isEnabled()) hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); }
            public void mouseClicked(MouseEvent e) { fireClicked(); }
        });
    }

    public void addActionListener(ActionListener l) { listeners.add(l); }

    /** 移除指定关闭监听。 */
    public void removeActionListener(ActionListener l) { listeners.remove(l); }

    /** 移除全部关闭监听。 */
    public void removeAllActionListeners() { listeners.clear(); }

    /** 当前注册的全部关闭监听（Swing 惯例，便于自检）。 */
    public ActionListener[] getActionListeners() { return listeners.toArray(new ActionListener[0]); }

    /** 设定唯一关闭回调：清空既有监听后注册一个（批次 2 的 Tabs/Select 清空按钮常用）。 */
    public void setOnClose(Runnable r) {
        removeAllActionListeners();
        if (r != null) addActionListener(e -> r.run());
    }

    /** × 符号默认颜色。 */
    public void setColor(Color c) { this.color = c; repaint(); }

    /** hover 时 × 符号颜色。 */
    public void setHoverColor(Color c) { this.hoverColor = c; repaint(); }

    /** 整体透明度 0~1，供父组件淡入淡出动画驱动。 */
    public void setAlpha(float a) {
        this.alpha = Math.max(0f, Math.min(1f, a));
        repaint();
    }

    /** false 时不响应点击且不拦截父组件鼠标事件（contains 返回 false）。 */
    public void setInteractive(boolean b) {
        this.interactive = b;
        if (!b) hoverAnim.go(hover, 0f);
        repaint();
    }

    public boolean isInteractive() { return interactive; }

    /** 运行时调整设计尺寸（首选/最小尺寸），并触发重布局。布局即使拉伸渲染也以实际尺寸为准。 */
    public void setButtonSize(int s) {
        if (s <= 0) return;
        this.size = s;
        revalidate();
        repaint();
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        if (!b) hoverAnim.go(hover, 0f); // 禁用时取消 hover 高亮
        repaint();
    }

    private void fireClicked() {
        if (!interactive || !isEnabled()) return;
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "close");
        for (ActionListener l : new ArrayList<ActionListener>(listeners)) l.actionPerformed(ev);
    }

    @Override
    public boolean contains(int x, int y) {
        return interactive && isEnabled() ? super.contains(x, y) : false;
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(size, size); }

    @Override
    public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override
    protected void paintComponent(Graphics g) {
        int a = Math.round(255 * alpha);
        if (a <= 0) return;
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean enabled = isEnabled();
        if (enabled && hover > 0) { // hover 圆形底色淡入（约 6% 黑），禁用态不画
            g2.setColor(new Color(0, 0, 0, Math.round(16 * hover * alpha)));
            g2.fillOval(0, 0, w - 1, h - 1);
        }
        // × 颜色：启用态在 color↔hoverColor 间插值；禁用态用 Element 禁用灰（TEXT_PLACEHOLDER）
        Color c = enabled ? ElementTheme.lerp(color, hoverColor, hover) : ElementTheme.TEXT_PLACEHOLDER;
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), a));
        // 字形以实际尺寸为准，绘制与布局解耦（父组件拉伸也不变形/不偏心）
        int eff = Math.min(w, h);
        float len = eff * 0.34f; // × 半臂长，约占盒子 1/3，留白克制
        float cx = w / 2f, cy = h / 2f;
        g2.setStroke(new BasicStroke(Math.max(1.2f, eff / 16f)));
        g2.drawLine(Math.round(cx - len), Math.round(cy - len), Math.round(cx + len), Math.round(cy + len));
        g2.drawLine(Math.round(cx - len), Math.round(cy + len), Math.round(cx + len), Math.round(cy - len));
        g2.dispose();
    }

    static void selfCheck() {
        CloseButton cb = new CloseButton();
        Dimension pd = cb.getPreferredSize();
        assert pd.width == 24 && pd.height == 24 : "default 24x24, got " + pd;
        CloseButton cb2 = new CloseButton(18);
        assert cb2.getPreferredSize().width == 18 : "custom size 18";

        // 点击触发监听
        final int[] fired = {0};
        cb.addActionListener(e -> fired[0]++);
        cb.addMouseListener(new MouseAdapter() {});
        cb.setSize(24, 24);
        cb.dispatchEvent(new MouseEvent(cb, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 12, 12, 1, false));
        assert fired[0] == 1 : "click should fire listener, fired=" + fired[0];

        // setInteractive(false)：contains false + 点击不触发
        cb.setInteractive(false);
        assert !cb.contains(12, 12) : "non-interactive contains must be false";
        cb.dispatchEvent(new MouseEvent(cb, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 12, 12, 1, false));
        assert fired[0] == 1 : "non-interactive click must not fire";
        cb.setInteractive(true);
        assert cb.contains(12, 12) : "interactive contains true";

        // setAlpha 边界
        cb.setAlpha(2f); assert true; // 不抛异常即通过（内部 clamp）
        cb.setAlpha(-1f);

        // 对比度：默认色与 hover 色对白底（浅色场景）达标
        ElementTheme.assertContrast(new Color(0x606266), Color.WHITE, "CloseButton default on white");
        ElementTheme.assertContrast(new Color(0x1d6fb5), Color.WHITE, "CloseButton hover on white");

        // --- 新增：setButtonSize / 移除监听 / setOnClose / 禁用灰化 ---
        CloseButton sz = new CloseButton(24);
        sz.setButtonSize(18);
        assert sz.getPreferredSize().width == 18 : "setButtonSize(18) updates preferred size";

        final int[] f2 = {0};
        CloseButton rl = new CloseButton();
        rl.setSize(24, 24);
        ActionListener a1 = e -> f2[0]++;
        ActionListener a2 = e -> f2[0]++;
        rl.addActionListener(a1);
        rl.addActionListener(a2);
        assert rl.getActionListeners().length == 2 : "two listeners registered, got " + rl.getActionListeners().length;
        rl.removeActionListener(a1);
        assert rl.getActionListeners().length == 1 : "after remove one, got " + rl.getActionListeners().length;
        rl.dispatchEvent(new MouseEvent(rl, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 12, 12, 1, false));
        assert f2[0] == 1 : "only remaining listener fires, fired=" + f2[0];

        final int[] f3 = {0};
        rl.setOnClose(() -> f3[0]++);
        assert rl.getActionListeners().length == 1 : "setOnClose replaces listeners, got " + rl.getActionListeners().length;
        rl.dispatchEvent(new MouseEvent(rl, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 12, 12, 1, false));
        assert f3[0] == 1 : "setOnClose callback fires, fired=" + f3[0];

        // 禁用灰化：contains false + 点击不触发 + 中心像素为禁用灰
        final int[] f4 = {0};
        CloseButton dis = new CloseButton();
        dis.setSize(24, 24);
        dis.addActionListener(e -> f4[0]++);
        dis.setEnabled(false);
        assert !dis.contains(12, 12) : "disabled contains must be false";
        dis.dispatchEvent(new MouseEvent(dis, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 12, 12, 1, false));
        assert f4[0] == 0 : "disabled click must not fire";
        dis.setEnabled(true);
        assert dis.contains(12, 12) : "re-enabled contains true";
        // 禁用态绘制（离屏）不抛异常，且 × 中心为禁用灰 ~0xC0C4CC
        BufferedImage bi = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gx = bi.createGraphics();
        dis.setEnabled(false);
        dis.setAlpha(1f);
        dis.paint(gx);
        gx.dispose();
        int px = bi.getRGB(12, 12);
        int r = (px >> 16) & 0xFF, gg = (px >> 8) & 0xFF, bb = px & 0xFF;
        assert Math.abs(r - 0xC0) <= 10 && Math.abs(gg - 0xC4) <= 10 && Math.abs(bb - 0xCC) <= 10
                : "disabled × should be gray ~0xC0C4CC, got rgb=" + r + "," + gg + "," + bb;

        // 拉伸渲染（布局解耦）不抛异常
        CloseButton stretch = new CloseButton(16);
        stretch.setSize(40, 40);
        Graphics2D sg = bi.createGraphics();
        stretch.paint(sg);
        sg.dispose();

        System.out.println("CloseButton self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
