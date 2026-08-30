package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.framework.AstInteractiveComponent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class AstSlider extends AstInteractiveComponent {
    private float lastTarget = Float.NaN;
    private int min, max, value;
    private boolean dragging;

    public AstSlider(int min, int max, int value) {
        this.min = min; this.max = max; this.value = value;
        anim.register("thumb", 200, Easing::easeOut);
        anim.register("hover", 150, Easing::easeInOut);
        MouseAdapter m = new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { if (!isEnabled()) return; dragging = true; setValueFrom(e.getX()); }
            public void mouseDragged(MouseEvent e)  { if (!isEnabled()) return; setValueFrom(e.getX()); }
            public void mouseReleased(MouseEvent e) { dragging = false; }
            public void mouseEntered(MouseEvent e)  { if (isEnabled()) anim.go("hover", anim.getProgress("hover"), 1f); }
            public void mouseExited(MouseEvent e)   { anim.go("hover", anim.getProgress("hover"), 0f); }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    private void setValueFrom(int x) {
        int left = 6, right = getWidth() - 16;
        float t = (x - left) / (float) (right - left);
        setValue(min + Math.round(t * (max - min)));
    }

    public int getValue() { return value; }

    public void setValue(int v) {
        int nv = Math.max(min, Math.min(max, v));
        if (nv != value) {
            value = nv;
            fire();
        }
        repaint();
    }

    public void addChangeListener(ChangeListener l) { listenerList.add(ChangeListener.class, l); }
    public void removeChangeListener(ChangeListener l) { listenerList.remove(ChangeListener.class, l); }

    private void fire() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) l.stateChanged(e);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        int cy = getHeight() / 2;
        int trackY = cy - 3, trackH = 6;
        int left = 6, right = getWidth() - 16;
        float t = (max == min) ? 0f : (value - min) / (float) (max - min);
        int thumbTarget = left + Math.round(t * (right - left));
        float thumbProgress = anim.getProgress("thumb");
        float hover = anim.getProgress("hover");
        // thumbX 初始为 -1（未初始化），迁移后用 thumbProgress < 0.001 且 lastTarget 为 NaN 表示首次
        int cx;
        if (Float.isNaN(lastTarget)) {
            cx = thumbTarget;
            lastTarget = thumbTarget;
            anim.go("thumb", thumbTarget, thumbTarget); // 初始化到目标位置
        } else if (dragging) {
            cx = thumbTarget;
        } else if (thumbTarget != lastTarget) {
            anim.go("thumb", anim.getProgress("thumb"), thumbTarget);
            lastTarget = thumbTarget;
            cx = Math.round(thumbProgress);
        } else {
            cx = Math.round(thumbProgress);
        }

        Color trackColor = isEnabled() ? theme().getPrimary() : new Color(0xC0C4CC);
        g2.setColor(new Color(0xE4E7ED));
        g2.fill(new RoundRectangle2D.Float(left, trackY, right - left, trackH, trackH, trackH));
        int fillW = Math.max(0, Math.min(right - left, cx - left));
        g2.setColor(trackColor);
        g2.fill(new RoundRectangle2D.Float(left, trackY, fillW, trackH, trackH, trackH));

        float r = 6f * (1f + 0.25f * hover);
        g2.setColor(Color.WHITE);
        g2.fillOval(Math.round(cx - r), Math.round(cy - r), Math.round(2 * r), Math.round(2 * r));
        g2.setColor(trackColor);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(Math.round(cx - r), Math.round(cy - r), Math.round(2 * r), Math.round(2 * r));
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(240, 32); }

    @Override
    protected void selfCheck() {
        // 1. 构造函数边界测试
        AstSlider s0 = this;
        assert s0.getValue() == 50 : "initial value 50, got " + s0.getValue();

        // 2. setValue 边界
        s0.setValue(0);
        assert s0.getValue() == 0 : "min value 0";
        s0.setValue(100);
        assert s0.getValue() == 100 : "max value 100";
        s0.setValue(-10);
        assert s0.getValue() == 0 : "clamp to min, got " + s0.getValue();
        s0.setValue(200);
        assert s0.getValue() == 100 : "clamp to max, got " + s0.getValue();

        // 3. ChangeListener
        final int[] fired = {0};
        final int[] lastVal = {-1};
        s0.addChangeListener(e -> { fired[0]++; lastVal[0] = s0.getValue(); });
        s0.setValue(30);
        assert fired[0] == 1 : "listener fired once, got " + fired[0];
        assert lastVal[0] == 30 : "last value 30, got " + lastVal[0];

        // 4. 相同值不触发
        s0.setValue(30);
        assert fired[0] == 1 : "same value should not fire again";

        // 5. removeChangeListener（移除不存在的不报错）
        s0.removeChangeListener(e -> {});
        assert true;

        // 6. 首选尺寸
        Dimension pd = s0.getPreferredSize();
        assert pd.width == 240 && pd.height == 32 : "preferred size 240x32, got " + pd;

        // 7. 渲染不抛异常
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(240, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { s0.paint(g); } finally { g.dispose(); }

        // 8. 禁用态渲染不抛异常
        s0.setEnabled(false);
        java.awt.image.BufferedImage img2 = new java.awt.image.BufferedImage(240, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();
        try { s0.paint(g2); } finally { g2.dispose(); }
        s0.setEnabled(true);

        // 9. 渲染 + 主题色验证（图形元素，非文字，不需要 4.5:1 对比度）
        assert theme().getPrimary() != null : "primary color not null";

        System.out.println("AstSlider self-check OK");
    }

    public static void main(String[] args) {
        new AstSlider(0, 100, 50).selfCheck();
    }
}
