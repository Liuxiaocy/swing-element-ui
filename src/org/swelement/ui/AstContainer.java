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
    private final JPanel centerStack;

    public AstContainer(int direction) {
        this.direction = direction;
        setOpaque(true);
        setBackground(ElementTheme.FILL_BLANK);
        north = new JPanel(new BorderLayout()); north.setOpaque(false);
        south = new JPanel(new BorderLayout()); south.setOpaque(false);
        west  = new JPanel(new BorderLayout()); west.setOpaque(false);
        center = new JPanel(new BorderLayout()); center.setOpaque(false);
        centerStack = new JPanel(new BorderLayout()); centerStack.setOpaque(false);
        super.setLayout(new BorderLayout());
        super.add(north, BorderLayout.NORTH);
        super.add(south, BorderLayout.SOUTH);
        super.add(centerStack, BorderLayout.CENTER);
        if (direction == HORIZONTAL) {
            centerStack.add(west,   BorderLayout.WEST);
            centerStack.add(center, BorderLayout.CENTER);
        } else {
            centerStack.add(west,   BorderLayout.NORTH);
            centerStack.add(center, BorderLayout.CENTER);
        }
    }

    public void setHeader(JComponent h) {
        if (h == null) throw new IllegalArgumentException("header must not be null");
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
        if (a == null) throw new IllegalArgumentException("aside must not be null");
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
        if (m == null) throw new IllegalArgumentException("main must not be null");
        if (mainComp != null) center.remove(mainComp);
        mainComp = m;
        m.setBorder(new EmptyBorder(16, 20, 16, 20));
        center.add(m, BorderLayout.CENTER);
        revalidate();
    }

    public void setFooter(JComponent f) {
        if (f == null) throw new IllegalArgumentException("footer must not be null");
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
        final AstContainer c = new AstContainer(HORIZONTAL);
        final JPanel h = new JPanel(); h.setBackground(Color.white);
        final JPanel a = new JPanel(); a.setBackground(Color.white);
        final JPanel m = new JPanel(); m.setBackground(Color.white);
        final JPanel f = new JPanel(); f.setBackground(Color.white);
        c.setHeader(h); c.setAside(a); c.setMain(m); c.setFooter(f);
        final int[] dims = new int[4];
        final boolean[] err = {false};
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                JFrame jf = new JFrame();
                jf.setContentPane(c);
                jf.setSize(900, 600);
                jf.pack();
                dims[0] = h.getHeight();
                dims[1] = a.getWidth();
                dims[2] = f.getHeight();
                dims[3] = c.getComponentCount();
                jf.dispose();
            }});
        } catch (Exception e) { err[0] = true; }
        assert !err[0] : "EDT invokeAndWait failed";
        assert dims[0] == HEADER_H : "header height, got "+dims[0]+" expected "+HEADER_H;
        assert dims[1] == ASIDE_W : "aside width, got "+dims[1]+" expected "+ASIDE_W;
        assert dims[2] == FOOTER_H : "footer height, got "+dims[2]+" expected "+FOOTER_H;
        assert dims[3] == 3 : "top-level should have 3 regions (north/south/centerStack)";

        final boolean[] vertOk = {false};
        final int[] vertHeights = new int[2];
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                AstContainer cv = new AstContainer(VERTICAL);
                JPanel hv = new JPanel(); hv.setBackground(Color.white);
                JPanel av = new JPanel(); av.setBackground(Color.WHITE);
                JPanel mv = new JPanel(); mv.setBackground(Color.WHITE);
                JPanel fv = new JPanel(); fv.setBackground(Color.WHITE);
                cv.setHeader(hv); cv.setAside(av); cv.setMain(mv); cv.setFooter(fv);
                JFrame jfv = new JFrame();
                jfv.setContentPane(cv);
                jfv.setSize(900, 600);
                jfv.pack();
                vertHeights[0] = hv.getHeight();
                vertHeights[1] = av.getHeight();
                vertOk[0] = (cv.isAncestorOf(hv) && cv.isAncestorOf(av) && cv.isAncestorOf(mv) && cv.isAncestorOf(fv));
                jfv.dispose();
                assert cv.getDirection() == VERTICAL : "direction";
            }});
        } catch (Exception e) { err[0] = true; }
        assert !err[0] : "EDT invokeAndWait failed for VERTICAL";
        assert vertOk[0] : "VERTICAL: header+aside+main+footer must ALL be descendants of AstContainer (header was orphaned by overlap bug)";
        assert vertHeights[0] == HEADER_H : "VERTICAL header height, got "+vertHeights[0];
        assert vertHeights[1] >= 40 : "VERTICAL aside height >= 40, got "+vertHeights[1];
        System.out.println("AstContainer self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
