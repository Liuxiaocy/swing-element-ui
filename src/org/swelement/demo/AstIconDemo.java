package org.swelement.demo;

import org.swelement.core.ElementTheme;
import org.swelement.ui.AstIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * AstIcon 演示 — 54 个自绘图标全览 + 尺寸/颜色变体 + 旋转动画。
 * selfcheck：离屏绘制全部图标并断言非空（与 AstIcon.selfCheck 互补，走 demo 入口）。
 */
public class AstIconDemo {
    private static final int CELL = 72;

    public static void main(String[] args) {
        if (args.length > 0 && "--selfcheck".equals(args[0])) { selfCheck(); return; }
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        JFrame f = new JFrame("AstIcon Demo - 图标库（54 个，纯 Graphics2D 自绘）");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(16, 24, 16, 24));

        // ---- 段 1：图标全览网格（4 列 × 名称标签）----
        JPanel grid = new JPanel(new GridLayout(0, 4, 6, 6));
        grid.setBorder(new TitledBorder("全部图标（悬停高亮，名称见提示）"));
        for (AstIcon.Type t : AstIcon.Type.values()) {
            grid.add(newIconCell(t, ElementTheme.TEXT_REGULAR, 20));
        }
        JScrollPane sp = new JScrollPane(grid);
        sp.setPreferredSize(new Dimension(4 * CELL + 30, 420));
        root.add(sp);

        // ---- 段 2：尺寸变体 ----
        JPanel sizes = new JPanel(new FlowLayout(FlowLayout.LEFT, 28, 10));
        sizes.setBorder(new TitledBorder("尺寸变体（CHECK）"));
        for (int sz : new int[]{16, 20, 24, 32, 48}) {
            JPanel one = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
            one.setOpaque(false);
            one.add(new AstIcon(AstIcon.Type.CHECK, ElementTheme.SUCCESS, sz));
            JLabel lb = new JLabel(sz + "px");
            lb.setForeground(ElementTheme.TEXT_REGULAR);
            one.add(lb);
            sizes.add(one);
        }
        root.add(sizes);

        // ---- 段 3：颜色变体 ----
        JPanel colors = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 10));
        colors.setBorder(new TitledBorder("颜色变体（STAR_FILLED）"));
        Color[] palette = {ElementTheme.PRIMARY, ElementTheme.SUCCESS, ElementTheme.WARNING,
            ElementTheme.DANGER, ElementTheme.INFO, ElementTheme.TEXT_REGULAR};
        for (Color c : palette)
            colors.add(newIconCell(AstIcon.Type.STAR_FILLED, c, 24));
        root.add(colors);

        // ---- 段 4：旋转动画（LOADING 自旋）----
        JPanel spin = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 10));
        spin.setBorder(new TitledBorder("旋转动画（LOADING 自旋 + REFRESH 对照）"));
        for (AstIcon.Type t : new AstIcon.Type[]{AstIcon.Type.LOADING, AstIcon.Type.LOADING, AstIcon.Type.LOADING}) {
            // 三个不同尺寸的 LOADING 同时自旋
            spin.add(newIconCell(t, ElementTheme.PRIMARY, 16 + (t.ordinal() % 3) * 8));
        }
        AstIcon ld = new AstIcon(AstIcon.Type.LOADING, ElementTheme.PRIMARY, 32);
        ld.setSpinEnabled(true);
        spin.add(ld);
        AstIcon rf = new AstIcon(AstIcon.Type.REFRESH, ElementTheme.PRIMARY, 24);
        rf.setSpinEnabled(true); // 演示任意图标皆可旋转
        spin.add(rf);
        root.add(spin);

        f.setContentPane(new JScrollPane(root));
        f.setSize(640, 760);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    /** 单个图标格：图标 + 悬停高亮背景 + tooltip 名称。 */
    private static JComponent newIconCell(final AstIcon.Type t, Color c, int size) {
        final JPanel cell = new JPanel(new GridBagLayout());
        cell.setPreferredSize(new Dimension(CELL, CELL));
        cell.setBackground(Color.WHITE);
        cell.setToolTipText(t.name());
        final AstIcon ic = new AstIcon(t, c, size);
        cell.add(ic);
        cell.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                cell.setBackground(new Color(0xECF5FF));
                ic.setColor(ElementTheme.PRIMARY);
            }
            @Override public void mouseExited(MouseEvent e) {
                cell.setBackground(Color.WHITE);
                ic.setColor(c);
            }
        });
        return cell;
    }

    // --- self-check ---
    private static void selfCheck() {
        final Throwable[] err = {null};
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    // 1) 全部图标经组件路径（paintComponent）离屏绘制非空
                    for (AstIcon.Type t : AstIcon.Type.values()) {
                        AstIcon ic = new AstIcon(t, ElementTheme.PRIMARY, 24);
                        ic.setBounds(0, 0, 24, 24);
                        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D gg = img.createGraphics();
                        gg.setColor(Color.WHITE);
                        gg.fillRect(0, 0, 24, 24);
                        try { ic.paint(gg); } finally { gg.dispose(); }
                        int nonWhite = 0;
                        for (int x = 0; x < 24; x++)
                            for (int y = 0; y < 24; y++) {
                                int p = img.getRGB(x, y);
                                if (((p >> 16) & 0xFF) < 200 || ((p >> 8) & 0xFF) < 200 || (p & 0xFF) < 200) nonWhite++;
                            }
                        assert nonWhite > 3 : "demo: icon " + t + " empty, nonWhite=" + nonWhite;
                    }
                    // 2) 尺寸变体生效
                    AstIcon a = new AstIcon(AstIcon.Type.CHECK, ElementTheme.PRIMARY, 16);
                    assert a.getPreferredSize().width == 16 : "pref size 16";
                    a.setSizeValue(48);
                    assert a.getPreferredSize().width == 48 : "pref size 48";
                    // 3) 静态绘制器参数校验
                    boolean threw = false;
                    try {
                        AstIcon.paintIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB).createGraphics(),
                            null, Color.BLACK, 16, 0f);
                    } catch (IllegalArgumentException e) { threw = true; }
                    assert threw : "paintIcon null type should throw";
                }
            });
        } catch (Throwable t) {
            err[0] = t;
        }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstIconDemo self-check OK");
    }
}
