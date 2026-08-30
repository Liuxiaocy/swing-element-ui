package org.swelement.ui;

import org.swelement.core.Easing;
import org.swelement.framework.AstDisplayComponent;

import javax.swing.*;
import java.awt.*;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

public class AstProgress extends AstDisplayComponent {
    private final EventListenerList listenerList = new EventListenerList();
    private int value;
    private boolean showText = true;

    @Override
    protected void initComponent() {
        super.initComponent();
        anim.register("fill", 300, Easing::easeOut);
    }

    public AstProgress() {
        setPreferredSize(new Dimension(320, 20));
    }

    public AstProgress(int initialValue) {
        this();
        setValue(initialValue);
    }

    public int getValue() { return value; }

    public void setValue(int v) {
        int old = value;
        value = Math.max(0, Math.min(100, v));
        anim.go("fill", anim.getProgress("fill"), value / 100f);
        repaint();
        if (old != value) fireStateChanged();
    }

    public void setShowText(boolean b) { showText = b; repaint(); }

    public boolean isShowText() { return showText; }

    public void addChangeListener(ChangeListener l) { listenerList.add(ChangeListener.class, l); }

    public void removeChangeListener(ChangeListener l) { listenerList.remove(ChangeListener.class, l); }

    private void fireStateChanged() {
        ChangeListener[] ls = listenerList.getListeners(ChangeListener.class);
        if (ls.length == 0) return;
        javax.swing.event.ChangeEvent ev = new javax.swing.event.ChangeEvent(this);
        for (ChangeListener l : ls) l.stateChanged(ev);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        float shown = anim.getProgress("fill");
        int textW = showText ? 46 : 0;
        int trackW = getWidth() - textW;
        int y = (getHeight() - 6) / 2;
        g2.setColor(new Color(0xEBEEF5));
        g2.fillRoundRect(0, y, trackW, 6, 6, 6);
        int fillW = Math.round(trackW * shown);
        g2.setColor(theme().getPrimary());
        g2.fillRoundRect(0, y, fillW, 6, 6, 6);
        if (showText) {
            g2.setColor(new Color(0x606266));
            Font f = theme().getFontBase().deriveFont(12f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(value + "%", trackW + 6, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        }
        g2.dispose();
    }

    @Override
    protected void selfCheck() {
        AstProgress p = new AstProgress();
        assert p.getPreferredSize().width == 320 : "default width";
        assert p.getPreferredSize().height == 20 : "default height";
        p.setValue(50);
        assert p.getValue() == 50 : "setValue";
        p.setValue(200);
        assert p.getValue() == 100 : "setValue clamps to 100";
        p.setValue(-10);
        assert p.getValue() == 0 : "setValue clamps to 0";
        assertContrast(new Color(0x606266), new Color(0xEBEEF5), "progress text on track");
        System.out.println("AstProgress self-check OK");
    }

    public static void main(String[] args) {
        new AstProgress().selfCheck();
    }
}
