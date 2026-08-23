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
import java.util.ArrayList;
import java.util.List;

/**
 * 公共可点击关闭按钮：矢量 × 符号 + hover 圆形底色淡入。
 * 所有可关闭组件（Tag/Alert/Input/AstDialog 等）统一使用，替代"自绘 × + 坐标命中测试"。
 * 对比度：默认色 0x606266 对白底 ≈6.1:1（≥4.5:1，WCAG AA 达标且有余量），hover 色 0x1d6fb5 为 primary 深变体（>= 4.5:1）。
 */
public class CloseButton extends JComponent {
    private final int size;
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
            public void mouseEntered(MouseEvent e) { if (interactive) hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); }
            public void mouseClicked(MouseEvent e) { fireClicked(); }
        });
    }

    public void addActionListener(ActionListener l) { listeners.add(l); }

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

    private void fireClicked() {
        if (!interactive) return;
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "close");
        for (ActionListener l : new ArrayList<ActionListener>(listeners)) l.actionPerformed(ev);
    }

    @Override
    public boolean contains(int x, int y) {
        return interactive ? super.contains(x, y) : false;
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(size, size); }

    @Override
    public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override
    protected void paintComponent(Graphics g) {
        int a = Math.round(255 * alpha);
        if (a <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (hover > 0) { // hover 圆形底色淡入（约 6% 黑）
            g2.setColor(new Color(0, 0, 0, Math.round(16 * hover * alpha)));
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
        }
        Color c = ElementTheme.lerp(color, hoverColor, hover);
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), a));
        float len = size * 0.4f; // × 半臂长
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        g2.setStroke(new BasicStroke(Math.max(1.4f, size / 14f)));
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

        System.out.println("CloseButton self-check OK");
    }

    public static void main(String[] args) { selfCheck(); }
}
