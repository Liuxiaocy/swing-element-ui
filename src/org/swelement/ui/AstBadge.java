package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AstBadge extends JComponent {
    private final Animator popAnim = new Animator(200, Easing::easeOut, v -> { scale = v; repaint(); });
    private float scale = 1f;
    private int count;
    private boolean dot;
    private JComponent content;

    private static final int BADGE_H = 18;
    private static final int DOT_SIZE = 10;
    private static final int PAD = 12;

    /** 透明覆盖层，负责绘制角标。作为 index 0 子组件，绘制顺序在 content 之后（最上层）。 */
    private final JComponent overlay = new JComponent() {
        {
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            paintBadge(g);
        }
        @Override
        public boolean contains(int x, int y) {
            return false; // 鼠标事件穿透到下层 content
        }
    };

    public AstBadge() {
        setOpaque(false);
        setLayout(new FillLayout());
        setFont(ElementTheme.FONT.deriveFont(Font.BOLD, 12f));
        setBorder(new EmptyBorder(PAD, 0, 0, PAD));
        add(overlay, 0); // index 0 = 最后绘制 = 最上层
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    public void setContent(JComponent c) {
        if (content != null) remove(content);
        content = c;
        add(content); // 追加到末尾，index > overlay，先绘制（底层）
        revalidate();
    }

    public void setCount(int c) {
        count = c;
        scale = 0.6f;
        popAnim.go(scale, 1f);
        repaint();
    }

    public void setDot(boolean b) { dot = b; repaint(); }

    private void paintBadge(Graphics g) {
        if (count <= 0 && !dot) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Font badgeFont = ElementTheme.FONT.deriveFont(Font.BOLD, 12f);
        g2.setFont(badgeFont);
        FontMetrics fm = g2.getFontMetrics();
        String text = count > 99 ? "99+" : String.valueOf(count);
        int textW = dot ? 0 : fm.stringWidth(text);

        int badgeW = dot ? DOT_SIZE : (count > 99 ? textW + 10 : BADGE_H);
        int badgeH = dot ? DOT_SIZE : BADGE_H;

        // 角标中心定位在 content 的右上角顶点
        int cx = getWidth() - PAD;
        int cy = PAD;

        float s = 0.6f + 0.4f * scale;
        g2.translate(cx, cy);
        g2.scale(s, s);
        g2.translate(-cx, -cy);

        g2.setColor(new Color(0xF56C6C));
        if (dot) {
            g2.fillOval(cx - DOT_SIZE / 2, cy - DOT_SIZE / 2, DOT_SIZE, DOT_SIZE);
        } else if (count <= 99) {
            g2.fillOval(cx - BADGE_H / 2, cy - BADGE_H / 2, BADGE_H, BADGE_H);
        } else {
            g2.fillRoundRect(cx - badgeW / 2, cy - badgeH / 2, badgeW, badgeH, badgeH / 2, badgeH / 2);
        }

        if (!dot) {
            g2.setColor(Color.WHITE);
            g2.drawString(text, cx - textW / 2f, cy - fm.getHeight() / 2f + fm.getAscent());
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Insets ins = getInsets();
        if (content == null) return new Dimension(48 + ins.left + ins.right, 48 + ins.top + ins.bottom);
        Dimension d = content.getPreferredSize();
        return new Dimension(d.width + ins.left + ins.right, d.height + ins.top + ins.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /** 自定义布局：content 填充 insets 内区域，overlay 填充整个 AstBadge（含 padding）以绘制角标。 */
    private class FillLayout implements LayoutManager {
        public void addLayoutComponent(String name, Component comp) {}
        public void removeLayoutComponent(Component comp) {}

        public Dimension preferredLayoutSize(Container parent) {
            return getPreferredSize();
        }
        public Dimension minimumLayoutSize(Container parent) {
            return getMinimumSize();
        }

        public void layoutContainer(Container parent) {
            Insets ins = parent.getInsets();
            int cw = parent.getWidth() - ins.left - ins.right;
            int ch = parent.getHeight() - ins.top - ins.bottom;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponent(i);
                if (c == overlay) {
                    c.setBounds(0, 0, parent.getWidth(), parent.getHeight());
                } else {
                    c.setBounds(ins.left, ins.top, cw, ch);
                }
            }
        }
    }
}
