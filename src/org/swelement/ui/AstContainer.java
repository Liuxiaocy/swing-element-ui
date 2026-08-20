package org.swelement.ui;

import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

public class AstContainer extends JPanel {
    public static final int VERTICAL = 0, HORIZONTAL = 1;
    public static final int HEADER_H = 64, ASIDE_W = 220, FOOTER_H = 48;

    private final int direction;
    private JComponent header, aside, mainComp, footer;
    private final JPanel north, south, west, center;

    public AstContainer(int direction) {
        this.direction = direction;
        setOpaque(true);
        setBackground(ElementTheme.FILL_BLANK);
        north = new JPanel(new BorderLayout()); north.setOpaque(false);
        south = new JPanel(new BorderLayout()); south.setOpaque(false);
        west  = new JPanel(new BorderLayout()); west.setOpaque(false);
        center = new JPanel(new BorderLayout()); center.setOpaque(false);
        super.setLayout(new BorderLayout());
        super.add(north, BorderLayout.NORTH);
        super.add(south, BorderLayout.SOUTH);
        if (direction == HORIZONTAL) {
            super.add(west,  BorderLayout.WEST);
        } else {
            super.add(west,  BorderLayout.NORTH);
        }
        super.add(center, BorderLayout.CENTER);
    }

    public void setHeader(JComponent h) {
        if (header != null) north.remove(header);
        header = h;
        h.setBorder(new MatteBorder(0, 0, 1, 0, ElementTheme.BORDER_BASE));
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, HEADER_H));
        if (h.getBackground() == null || ElementTheme.FILL_BLANK.equals(h.getBackground()) || Color.WHITE.equals(h.getBackground())) {
            h.setBackground(ElementTheme.FILL_BASE);
        }
        if (h.isOpaque() == false) h.setOpaque(true);
        north.add(h, BorderLayout.CENTER);
        revalidate();
    }

    public void setAside(JComponent a) {
        if (aside != null) west.remove(aside);
        aside = a;
        if (direction == HORIZONTAL) {
            a.setPreferredSize(new Dimension(ASIDE_W, Math.max(a.getPreferredSize().height, 400)));
            a.setBorder(new MatteBorder(0, 0, 0, 1, ElementTheme.BORDER_BASE));
        } else {
            a.setPreferredSize(new Dimension(Math.max(a.getPreferredSize().width, 400), 40));
            a.setBorder(new MatteBorder(0, 0, 1, 0, ElementTheme.BORDER_BASE));
        }
        if (a.isOpaque() == false) a.setOpaque(true);
        if (a.getBackground() == null || ElementTheme.FILL_BLANK.equals(a.getBackground()) || Color.WHITE.equals(a.getBackground())) {
            a.setBackground(ElementTheme.FILL_BASE);
        }
        west.add(a, BorderLayout.CENTER);
        revalidate();
    }

    public void setMain(JComponent m) {
        if (mainComp != null) center.remove(mainComp);
        mainComp = m;
        m.setBorder(new EmptyBorder(16, 20, 16, 20));
        center.add(m, BorderLayout.CENTER);
        revalidate();
    }

    public void setFooter(JComponent f) {
        if (footer != null) south.remove(footer);
        footer = f;
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, FOOTER_H));
        if (f.isOpaque() == false) f.setOpaque(true);
        if (f.getBackground() == null || ElementTheme.FILL_BLANK.equals(f.getBackground()) || Color.WHITE.equals(f.getBackground())) {
            f.setBackground(ElementTheme.FILL_BASE);
        }
        f.setBorder(new MatteBorder(1, 0, 0, 0, ElementTheme.BORDER_BASE));
        south.add(f, BorderLayout.CENTER);
        revalidate();
    }

    public int getDirection() { return direction; }

    static void selfCheck() {
        AstContainer c = new AstContainer(HORIZONTAL);
        JPanel h = new JPanel(); h.setBackground(Color.white);
        JPanel a = new JPanel(); a.setBackground(Color.white);
        JPanel m = new JPanel(); m.setBackground(Color.white);
        JPanel f = new JPanel(); f.setBackground(Color.white);
        c.setHeader(h); c.setAside(a); c.setMain(m); c.setFooter(f);
        JFrame jf = new JFrame();
        jf.setContentPane(c);
        jf.setSize(900, 600);
        jf.pack();
        assert h.getHeight() == HEADER_H : "header height, got "+h.getHeight()+" expected "+HEADER_H;
        assert a.getWidth() == ASIDE_W : "aside width, got "+a.getWidth()+" expected "+ASIDE_W;
        assert f.getHeight() == FOOTER_H : "footer height, got "+f.getHeight()+" expected "+FOOTER_H;
        assert c.getComponentCount() == 4 : "4 regions should be added";
        jf.dispose();
        AstContainer cv = new AstContainer(VERTICAL);
        JPanel hv = new JPanel(); JPanel av = new JPanel(); JPanel mv = new JPanel();
        cv.setHeader(hv); cv.setAside(av); cv.setMain(mv);
        assert cv.getDirection() == VERTICAL;
        System.out.println("AstContainer self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
