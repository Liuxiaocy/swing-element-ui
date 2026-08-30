package org.swelement.demo;

import org.swelement.ui.AstButton;
import org.swelement.ui.AstContainer;
import org.swelement.ui.AstTabs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class AstContainerDemo {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { start(); }
        });
    }
    private static void start() {
        final JFrame f = new JFrame("AstContainer Demo - 布局容器");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        ctrl.setBorder(new TitledBorder("切换布局方向 & 显示项"));
        final JCheckBox hdr = new JCheckBox("Header", true);
        final JCheckBox asd = new JCheckBox("Aside", true);
        final JCheckBox ftr = new JCheckBox("Footer", true);
        final JComboBox<String> dir = new JComboBox<String>(new String[]{
                "HORIZONTAL (Aside 左 + Main 右)",
                "VERTICAL (Aside 上 + Main 下)"
        });
        AstButton apply = new AstButton("应用布局", 0 /* DEFAULT */, false);
        ctrl.add(dir); ctrl.add(hdr); ctrl.add(asd); ctrl.add(ftr); ctrl.add(apply);

        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setDividerLocation(110);
        split.setTopComponent(ctrl);

        apply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                AstContainer ac = new AstContainer(dir.getSelectedIndex() == 0 ? AstContainer.HORIZONTAL : AstContainer.VERTICAL);

                JPanel headerBox = new JPanel(new BorderLayout());
                JLabel title = new JLabel("  Admin Console — 后台管理系统");
                title.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
                title.setForeground(new Color(0x303133));
                headerBox.add(title, BorderLayout.WEST);
                JPanel hdrRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
                hdrRight.add(new AstButton("🔔 通知", AstButton.DEFAULT, false));
                hdrRight.add(new AstButton("👤 管理员", AstButton.PRIMARY, false));
                headerBox.add(hdrRight, BorderLayout.EAST);
                if (hdr.isSelected()) ac.setHeader(headerBox);

                DefaultListModel<String> lm = new DefaultListModel<String>();
                for (String s : new String[]{"📊 数据总览", "👥 用户管理", "📦 商品管理", "💳 订单管理", "⚙️ 系统设置"})
                    lm.addElement(s);
                JList<String> asideBox = new JList<String>(lm);
                asideBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
                asideBox.setFixedCellHeight(36);
                asideBox.setBorder(new EmptyBorder(8, 8, 8, 8));
                asideBox.setBackground(new Color(0xFAFAFA));
                if (asd.isSelected()) ac.setAside(asideBox);

                AstTabs mainTabs = new AstTabs(new String[]{"基本信息", "权限配置", "安全日志"}, 0);
                JLabel mainBody = new JLabel("<html><body style='color:#606266;font-size:12px;padding:16px 24px'>" +
                        "Main 主内容区：可放置表单、表格、卡片。AstContainer 默认 Main 四周 padding 16/20/16/20 像素。<br><br>" +
                        "✅ Header 64px，底部 1px BORDER_BASE 分割线<br>" +
                        "✅ Aside 在 HORIZONTAL 模式下宽 220px，右侧 1px 分割线<br>" +
                        "✅ Footer 48px，顶部 1px BORDER_BASE 分割线<br>" +
                        "✅ 通过顶部控件切换方向和显示项，观察布局变化</body></html>");
                JPanel mainCard = new JPanel(new BorderLayout());
                mainCard.add(mainTabs, BorderLayout.NORTH);
                mainCard.add(mainBody, BorderLayout.CENTER);
                ac.setMain(mainCard);

                JLabel footerBox = new JLabel("  © 2026 swing-element-ui · Layout demo", SwingConstants.LEFT);
                footerBox.setForeground(new Color(0x909399));
                if (ftr.isSelected()) ac.setFooter(footerBox);

                split.setBottomComponent(ac);
                split.setResizeWeight(1.0);
            }
        });

        apply.doClick();

        f.setContentPane(split);
        f.setSize(1000, 700);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
