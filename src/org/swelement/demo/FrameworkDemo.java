package org.swelement.demo;

import org.swelement.framework.AstInteractiveComponent;
import org.swelement.framework.util.PaintingHelper;
import org.swelement.core.theme.Theme;
import org.swelement.core.theme.ThemeManager;
import org.swelement.core.theme.ElementLightTheme;
import org.swelement.ui.AstIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * 框架核心能力演示 Demo。
 * <p>
 * 展示内容：
 * <ul>
 *   <li>主题系统（动态切换主题）</li>
 *   <li>动画管理器（hover/active/focus 自动动画）</li>
 *   <li>基类绘制辅助（圆角、插值、文字居中）</li>
 *   <li>交互组件基类的自动事件处理</li>
 * </ul>
 */
public class FrameworkDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 确保默认主题已注册
            ThemeManager.ensureDefaultTheme();
            // 注册自定义紫色主题
            ThemeManager.registerTheme(new PurpleTheme());

            JFrame f = new JFrame("Swing Element UI - Framework Demo 框架特性演示");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(20, 24, 24, 24));
            root.setBackground(Color.WHITE);

            // ========== 标题 ==========
            JLabel title = new JLabel("Framework 框架特性演示");
            title.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
            title.setForeground(new Color(0x303133));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            root.add(title);
            root.add(Box.createVerticalStrut(4));

            JLabel subtitle = new JLabel("展示主题系统、动画管理器、绘制工具类、交互基类等核心能力");
            subtitle.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            subtitle.setForeground(new Color(0x909399));
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            root.add(subtitle);
            root.add(Box.createVerticalStrut(20));

            // ========== 主题切换区 ==========
            JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            themePanel.setBorder(new TitledBorder("主题系统 Theme System"));
            themePanel.setBackground(Color.WHITE);
            themePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel themeLabel = new JLabel("选择主题：");
            themeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            themeLabel.setForeground(new Color(0x606266));

            JComboBox<String> themeCombo = new JComboBox<String>();
            themeCombo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            themeCombo.setPreferredSize(new Dimension(200, 32));
            for (String name : ThemeManager.getAvailableThemes()) {
                // 显示友好名称
                if ("element-light".equals(name)) {
                    themeCombo.addItem("Element Light");
                } else if ("purple-indigo".equals(name)) {
                    themeCombo.addItem("Purple Indigo (自定义)");
                } else {
                    themeCombo.addItem(name);
                }
            }
            themeCombo.setSelectedIndex(0);

            // 当前主题信息标签
            final JLabel themeInfo = new JLabel("当前主题：Element Light");
            themeInfo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            themeInfo.setForeground(new Color(0x67C23A));

            themeCombo.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String displayName = (String) e.getItem();
                    String themeName;
                    if (displayName.startsWith("Element Light")) {
                        themeName = "element-light";
                    } else if (displayName.startsWith("Purple Indigo")) {
                        themeName = "purple-indigo";
                    } else {
                        themeName = displayName;
                    }
                    ThemeManager.setCurrent(themeName);
                    Theme t = ThemeManager.getCurrent();
                    themeInfo.setText("当前主题：" + displayName);
                    themeInfo.setForeground(t.getSuccess());
                }
            });

            themePanel.add(themeLabel);
            themePanel.add(themeCombo);
            themePanel.add(themeInfo);
            root.add(themePanel);
            root.add(Box.createVerticalStrut(16));

            // ========== 交互按钮演示区 ==========
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 16));
            buttonPanel.setBorder(new TitledBorder("动画管理器 Animation Manager（hover / active / focus）"));
            buttonPanel.setBackground(Color.WHITE);
            buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // 创建演示按钮
            DemoButton demoBtn1 = new DemoButton("悬停查看效果", DemoButton.STYLE_PRIMARY);
            demoBtn1.setPreferredSize(new Dimension(160, 44));

            DemoButton demoBtn2 = new DemoButton("点击按下效果", DemoButton.STYLE_SUCCESS);
            demoBtn2.setPreferredSize(new Dimension(160, 44));

            DemoButton demoBtn3 = new DemoButton("Tab 聚焦效果", DemoButton.STYLE_WARNING);
            demoBtn3.setPreferredSize(new Dimension(160, 44));

            buttonPanel.add(demoBtn1);
            buttonPanel.add(demoBtn2);
            buttonPanel.add(demoBtn3);
            root.add(buttonPanel);
            root.add(Box.createVerticalStrut(16));

            // ========== 绘制工具类演示区 ==========
            JPanel paintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 16));
            paintPanel.setBorder(new TitledBorder("绘制工具类 PaintingHelper（圆角 / 发光 / 居中文本 / 图标）"));
            paintPanel.setBackground(Color.WHITE);
            paintPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // 圆角矩形演示
            JPanel roundRectDemo = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    Theme t = ThemeManager.getCurrent();
                    // 大圆角矩形
                    g2.setColor(t.getPrimary());
                    PaintingHelper.fillRoundRect(g2, 10, 10, 80, 60, 12);

                    // 小圆角矩形
                    g2.setColor(t.getSuccess());
                    PaintingHelper.fillRoundRect(g2, 100, 10, 80, 60, 4);

                    // 描边圆角矩形
                    g2.setColor(t.getWarning());
                    g2.setStroke(new BasicStroke(2f));
                    PaintingHelper.drawRoundRect(g2, 190, 10, 80, 60, 20);

                    // 居中文本
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
                    PaintingHelper.drawTextInCenter(g2, "圆角", 10, 10, 80, 60);

                    g2.setColor(Color.WHITE);
                    PaintingHelper.drawTextInCenter(g2, "小角", 100, 10, 80, 60);

                    g2.setColor(t.getWarning());
                    PaintingHelper.drawTextInCenter(g2, "描边", 190, 10, 80, 60);

                    g2.dispose();
                }
            };
            roundRectDemo.setPreferredSize(new Dimension(290, 80));
            roundRectDemo.setBackground(Color.WHITE);

            // 发光效果演示
            JPanel glowDemo = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    Theme t = ThemeManager.getCurrent();

                    // 发光圆形
                    Shape circle = new java.awt.geom.Ellipse2D.Float(40, 15, 50, 50);
                    PaintingHelper.drawGlow(g2, circle, t.getPrimary(), 12, 0.4f);
                    g2.setColor(t.getPrimary());
                    g2.fill(circle);

                    // 发光圆角矩形
                    Shape rr = PaintingHelper.roundRect(120, 20, 90, 40, 10);
                    PaintingHelper.drawGlow(g2, rr, t.getSuccess(), 10, 0.35f);
                    g2.setColor(t.getSuccess());
                    g2.fill(rr);

                    // 文字
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
                    PaintingHelper.drawTextInCenter(g2, "发光", 120, 20, 90, 40);

                    g2.dispose();
                }
            };
            glowDemo.setPreferredSize(new Dimension(230, 80));
            glowDemo.setBackground(Color.WHITE);

            // 图标演示
            JPanel iconDemo = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    Theme t = ThemeManager.getCurrent();

                    // 各种图表（AstIcon 自绘）
                    AstIcon.Type[] iconTypes = {
                        AstIcon.Type.STAR_FILLED, AstIcon.Type.CHECK, AstIcon.Type.CLOSE,
                        AstIcon.Type.SETTING, AstIcon.Type.CIRCLE_INFO
                    };
                    Color[] colors = {t.getWarning(), t.getSuccess(), t.getDanger(), t.getInfo(), t.getDanger()};

                    for (int i = 0; i < iconTypes.length; i++) {
                        int cx = 25 + i * 36;
                        int cy = 28;
                        // 圆形背景
                        PaintingHelper.fillCircle(g2, cx, cy, 14);
                        g2.setColor(PaintingHelper.lighten(colors[i], 0.85f));
                        PaintingHelper.fillCircle(g2, cx, cy, 14);
                        // 图标
                        PaintingHelper.drawIcon(g2, new AstIcon(iconTypes[i], colors[i], 16), cx - 8, cy - 8);
                    }

                    g2.dispose();
                }
            };
            iconDemo.setPreferredSize(new Dimension(210, 60));
            iconDemo.setBackground(Color.WHITE);

            paintPanel.add(roundRectDemo);
            paintPanel.add(glowDemo);
            paintPanel.add(iconDemo);
            root.add(paintPanel);
            root.add(Box.createVerticalStrut(16));

            // ========== 主题响应演示区 ==========
            JPanel themeRespPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
            themeRespPanel.setBorder(new TitledBorder("主题响应 onThemeUpdated（切换主题后自动更新）"));
            themeRespPanel.setBackground(Color.WHITE);
            themeRespPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // 主题色卡片
            ThemeColorCard primaryCard = new ThemeColorCard("Primary", ThemeColorCard.TYPE_PRIMARY);
            primaryCard.setPreferredSize(new Dimension(100, 80));

            ThemeColorCard successCard = new ThemeColorCard("Success", ThemeColorCard.TYPE_SUCCESS);
            successCard.setPreferredSize(new Dimension(100, 80));

            ThemeColorCard warningCard = new ThemeColorCard("Warning", ThemeColorCard.TYPE_WARNING);
            warningCard.setPreferredSize(new Dimension(100, 80));

            ThemeColorCard dangerCard = new ThemeColorCard("Danger", ThemeColorCard.TYPE_DANGER);
            dangerCard.setPreferredSize(new Dimension(100, 80));

            ThemeColorCard infoCard = new ThemeColorCard("Info", ThemeColorCard.TYPE_INFO);
            infoCard.setPreferredSize(new Dimension(100, 80));

            themeRespPanel.add(primaryCard);
            themeRespPanel.add(successCard);
            themeRespPanel.add(warningCard);
            themeRespPanel.add(dangerCard);
            themeRespPanel.add(infoCard);
            root.add(themeRespPanel);
            root.add(Box.createVerticalStrut(10));

            // ========== 底部说明 ==========
            JLabel tip = new JLabel("提示：鼠标悬停按钮查看 hover 动画，点击按钮查看 active 动画，Tab 键切换焦点查看 focus 动画");
            tip.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            tip.setForeground(new Color(0x909399));
            tip.setAlignmentX(Component.LEFT_ALIGNMENT);
            root.add(tip);

            f.setContentPane(root);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    // ==================== 自定义紫色主题 ====================

    /**
     * 自定义靛蓝紫色主题。
     * <p>
     * primary = 0x6366F1 (靛蓝)
     * success = 0x10B981 (翠绿)
     * 其他颜色基于主色调整或保持类似色调。
     */
    static class PurpleTheme implements Theme {
        private static final Color PRIMARY = new Color(0x6366F1);
        private static final Color SUCCESS = new Color(0x10B981);
        private static final Color WARNING = new Color(0xF59E0B);
        private static final Color DANGER = new Color(0xEF4444);
        private static final Color INFO = new Color(0x6366F1);

        private static final Color TEXT_PRIMARY = new Color(0x1F2937);
        private static final Color TEXT_REGULAR = new Color(0x4B5563);
        private static final Color TEXT_SECONDARY = new Color(0x6B7280);
        private static final Color TEXT_PLACEHOLDER = new Color(0x9CA3AF);
        private static final Color TEXT_DISABLED = new Color(0xD1D5DB);

        private static final Color BORDER_BASE = new Color(0xE5E7EB);
        private static final Color BORDER_LIGHT = new Color(0xF3F4F6);
        private static final Color BORDER_LIGHTER = new Color(0xF9FAFB);

        private static final Color FILL_BLANK = new Color(0xFFFFFF);
        private static final Color FILL_BASE = new Color(0xF9FAFB);
        private static final Color FILL_LIGHT = new Color(0xF3F4F6);

        private static final int RADIUS_SMALL = 3;
        private static final int RADIUS_BASE = 6;
        private static final int RADIUS_LARGE = 10;

        private static final Font FONT_SMALL = new Font("Microsoft YaHei", Font.PLAIN, 12);
        private static final Font FONT_BASE = new Font("Microsoft YaHei", Font.PLAIN, 14);
        private static final Font FONT_LARGE = new Font("Microsoft YaHei", Font.PLAIN, 16);

        public String getName() {
            return "purple-indigo";
        }

        public Color getPrimary() { return PRIMARY; }
        public Color getSuccess() { return SUCCESS; }
        public Color getWarning() { return WARNING; }
        public Color getDanger() { return DANGER; }
        public Color getInfo() { return INFO; }

        public Color getTextPrimary() { return TEXT_PRIMARY; }
        public Color getTextRegular() { return TEXT_REGULAR; }
        public Color getTextSecondary() { return TEXT_SECONDARY; }
        public Color getTextPlaceholder() { return TEXT_PLACEHOLDER; }
        public Color getTextDisabled() { return TEXT_DISABLED; }

        public Color getBorderBase() { return BORDER_BASE; }
        public Color getBorderLight() { return BORDER_LIGHT; }
        public Color getBorderLighter() { return BORDER_LIGHTER; }

        public Color getFillBlank() { return FILL_BLANK; }
        public Color getFillBase() { return FILL_BASE; }
        public Color getFillLight() { return FILL_LIGHT; }

        public int getRadiusSmall() { return RADIUS_SMALL; }
        public int getRadiusBase() { return RADIUS_BASE; }
        public int getRadiusLarge() { return RADIUS_LARGE; }

        public Font getFontSmall() { return FONT_SMALL; }
        public Font getFontBase() { return FONT_BASE; }
        public Font getFontLarge() { return FONT_LARGE; }

        public Color getColor(String key) {
            if (key == null) return null;
            if ("primary".equals(key)) return PRIMARY;
            if ("success".equals(key)) return SUCCESS;
            if ("warning".equals(key)) return WARNING;
            if ("danger".equals(key)) return DANGER;
            if ("info".equals(key)) return INFO;
            if ("textPrimary".equals(key)) return TEXT_PRIMARY;
            if ("textRegular".equals(key)) return TEXT_REGULAR;
            if ("textSecondary".equals(key)) return TEXT_SECONDARY;
            if ("textPlaceholder".equals(key)) return TEXT_PLACEHOLDER;
            if ("textDisabled".equals(key)) return TEXT_DISABLED;
            if ("borderBase".equals(key)) return BORDER_BASE;
            if ("borderLight".equals(key)) return BORDER_LIGHT;
            if ("borderLighter".equals(key)) return BORDER_LIGHTER;
            if ("fillBlank".equals(key)) return FILL_BLANK;
            if ("fillBase".equals(key)) return FILL_BASE;
            if ("fillLight".equals(key)) return FILL_LIGHT;
            return null;
        }

        public Font getFont(String key) {
            if (key == null) return null;
            if ("small".equals(key)) return FONT_SMALL;
            if ("base".equals(key)) return FONT_BASE;
            if ("large".equals(key)) return FONT_LARGE;
            return null;
        }

        public int getSize(String key) {
            if (key == null) return -1;
            if ("radiusSmall".equals(key)) return RADIUS_SMALL;
            if ("radiusBase".equals(key)) return RADIUS_BASE;
            if ("radiusLarge".equals(key)) return RADIUS_LARGE;
            return -1;
        }
    }

    // ==================== 演示按钮组件 ====================

    /**
     * 演示用按钮组件，继承自 AstInteractiveComponent。
     * <p>
     * 展示三种动画效果：
     * <ul>
     *   <li>hover：颜色变亮 + 轻微放大</li>
     *   <li>active：颜色变暗 + 轻微缩小</li>
     *   <li>focus：外发光效果</li>
     * </ul>
     */
    static class DemoButton extends AstInteractiveComponent {
        static final int STYLE_PRIMARY = 0;
        static final int STYLE_SUCCESS = 1;
        static final int STYLE_WARNING = 2;
        static final int STYLE_DANGER = 3;
        static final int STYLE_INFO = 4;

        private final int style;
        private String text;

        DemoButton(String text, int style) {
            this.text = text;
            this.style = style;
            // 设置初始尺寸
            setPreferredSize(new Dimension(140, 40));
        }

        private Color getBaseColor() {
            Theme t = theme();
            switch (style) {
                case STYLE_SUCCESS: return t.getSuccess();
                case STYLE_WARNING: return t.getWarning();
                case STYLE_DANGER: return t.getDanger();
                case STYLE_INFO: return t.getInfo();
                case STYLE_PRIMARY:
                default: return t.getPrimary();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int radius = theme().getRadiusBase();

            Color baseColor = getBaseColor();
            float hoverP = hoverProgress();
            float activeP = activeProgress();
            float focusP = focusProgress();

            // 计算最终颜色：hover 变亮，active 变暗
            Color hoverColor = PaintingHelper.lighten(baseColor, 0.15f * hoverP);
            Color finalColor = PaintingHelper.darken(hoverColor, 1f - 0.15f * activeP);

            // 计算缩放偏移：active 时略微内缩
            int inset = Math.round(2f * activeP);
            int bx = inset;
            int by = inset;
            int bw = w - inset * 2;
            int bh = h - inset * 2;

            // focus 发光效果
            if (focusP > 0.01f) {
                Shape focusShape = PaintingHelper.roundRect(bx + 2, by + 2, bw - 4, bh - 4, radius);
                PaintingHelper.drawGlow(g2, focusShape, baseColor,
                        Math.round(8f * focusP), 0.5f * focusP);
            }

            // 填充按钮主体
            g2.setColor(finalColor);
            PaintingHelper.fillRoundRect(g2, bx, by, bw, bh, radius);

            // 绘制文字
            g2.setColor(Color.WHITE);
            g2.setFont(theme().getFontBase());
            PaintingHelper.drawTextInCenter(g2, text, bx, by, bw, bh);

            g2.dispose();
        }

        @Override
        protected void selfCheck() {
            // 组件自检（Demo 组件简化处理）
        }
    }

    // ==================== 主题色卡片组件 ====================

    /**
     * 主题色卡片，演示 onThemeUpdated 钩子。
     * <p>
     * 切换主题后自动更新显示的颜色。
     */
    static class ThemeColorCard extends AstInteractiveComponent {
        static final int TYPE_PRIMARY = 0;
        static final int TYPE_SUCCESS = 1;
        static final int TYPE_WARNING = 2;
        static final int TYPE_DANGER = 3;
        static final int TYPE_INFO = 4;

        private final int type;
        private final String label;

        ThemeColorCard(String label, int type) {
            this.label = label;
            this.type = type;
            setPreferredSize(new Dimension(100, 80));
        }

        private Color getColor() {
            Theme t = theme();
            switch (type) {
                case TYPE_SUCCESS: return t.getSuccess();
                case TYPE_WARNING: return t.getWarning();
                case TYPE_DANGER: return t.getDanger();
                case TYPE_INFO: return t.getInfo();
                case TYPE_PRIMARY:
                default: return t.getPrimary();
            }
        }

        @Override
        protected void onThemeUpdated(Theme oldTheme, Theme newTheme) {
            super.onThemeUpdated(oldTheme, newTheme);
            // 主题变更时自动重绘（父类已调用 repaint）
            // 这里可以添加额外的主题更新逻辑
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int radius = theme().getRadiusLarge();

            Color color = getColor();
            float hoverP = hoverProgress();

            // hover 时的发光效果
            if (hoverP > 0.01f) {
                Shape cardShape = PaintingHelper.roundRect(2, 2, w - 4, h - 4, radius);
                PaintingHelper.drawGlow(g2, cardShape, color,
                        Math.round(10f * hoverP), 0.4f * hoverP);
            }

            // 卡片背景
            g2.setColor(color);
            PaintingHelper.fillRoundRect(g2, 4, 4, w - 8, h - 8, radius);

            // 标签文字
            g2.setColor(Color.WHITE);
            g2.setFont(theme().getFontBase());
            PaintingHelper.drawTextInCenter(g2, label, 4, 4, w - 8, h - 8);

            // hex 值
            String hex = String.format("#%06X", color.getRGB() & 0xFFFFFF);
            g2.setFont(theme().getFontSmall());
            g2.setColor(PaintingHelper.withAlpha(Color.WHITE, 0.8f));
            FontMetrics fm = g2.getFontMetrics();
            float hexY = h - 10 - fm.getDescent();
            PaintingHelper.drawCenteredText(g2, hex, 4, w - 8, hexY);

            g2.dispose();
        }

        @Override
        protected void selfCheck() {
            // 组件自检（Demo 组件简化处理）
        }
    }
}
