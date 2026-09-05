import org.swelement.core.theme.ThemeManager;
import org.swelement.ui.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 离屏渲染组件截图，写入 docs/screenshots/，供组件文档引用。
 * 可重复执行；主题或组件更新后重跑即可刷新全部截图。
 * 用法：java -cp out DocScreenshotGen
 */
public class DocScreenshotGen {

    static final String DIR = "docs/screenshots";

    public static void main(String[] args) throws Exception {
        ThemeManager.ensureDefaultTheme();
        new File(DIR).mkdirs();

        // AstBadge
        AstBadge badge = new AstBadge();
        badge.setContent(new JButton("消息")); badge.setCount(12);
        render("badge-default.png", badge);
        AstBadge dot = new AstBadge();
        dot.setContent(new JButton("消息")); dot.setDot(true);
        render("badge-dot.png", dot);

        // AstButton
        render("button-default.png", new AstButton("默认按钮"));
        render("button-plain.png", new AstButton("朴素 主要", AstButton.PRIMARY, true));
        AstButton dis = new AstButton("禁用-默认", AstButton.DEFAULT, false); dis.setEnabled(false);
        render("button-disabled.png", dis);

        // AstCheckbox
        render("checkbox-default.png",
                new AstCheckbox("选项A"), new AstCheckbox("选项B"), new AstCheckbox("选项C"));
        AstCheckbox cd = new AstCheckbox("禁用选项"); cd.setEnabled(false);
        render("checkbox-disabled.png", cd);

        // AstInput
        render("input-default.png", new AstInput("请输入内容"));
        AstInput clr = new AstInput("请输入内容"); clr.setText("已输入文本");
        render("input-clearable.png", clr);
        AstInput din = new AstInput("请输入内容"); din.setEnabled(false);
        render("input-disabled.png", din);

        // AstMenu
        AstMenu menu = new AstMenu();
        menu.addMenuItem("首页", () -> {});
        menu.addMenuItem("新闻", () -> {});
        menu.addMenuItem("关于", () -> {});
        menu.setActive(0);
        render("menu-default.png", menu);
        menu.setSize(menu.getPreferredSize()); render("menu-sub.png", menu);

        // AstPagination
        render("pagination-default.png", new AstPagination(100, 10, 1));
        render("pagination-pagesize.png", new AstPagination(200, 20, 1));

        // AstProgress
        render("progress-default.png", new AstProgress(50));
        AstProgress pt = new AstProgress(60); pt.setShowText(true);
        render("progress-text.png", pt);

        // AstRadio
        render("radio-default.png", new AstRadio("选项A"), new AstRadio("选项B"));
        AstRadio rd = new AstRadio("禁用选项"); rd.setEnabled(false);
        render("radio-disabled.png", rd);

        // AstSelect
        render("select-default.png", new AstSelect(new String[]{"黄金糕", "双皮奶", "蚵仔煎"}));
        render("select-filterable.png", new AstSelect(false, true));
        render("select-multiple.png", new AstSelect(true, false));
        AstSelect grp = new AstSelect(false, false);
        grp.addOption(new AstSelect.Option("黄金糕", "gold", "热门城市", false));
        grp.addOption(new AstSelect.Option("北京", "beijing", "城市名", false));
        render("select-group.png", grp);

        // AstSlider
        render("slider-default.png", new AstSlider(0, 100, 50));
        AstSlider sd = new AstSlider(0, 100, 50); sd.setEnabled(false);
        render("slider-disabled.png", sd);

        // AstSwitch
        render("switch-default.png", new AstSwitch());
        AstSwitch sw = new AstSwitch(); sw.setEnabled(false);
        render("switch-disabled.png", sw);

        // AstTabs
        render("tabs-default.png",
                new AstTabs(new String[]{"用户管理", "配置管理", "角色管理"}, 0));

        // AstTag
        render("tag-default.png",
                new AstTag("标签一", AstTag.PRIMARY, false),
                new AstTag("标签二", AstTag.SUCCESS, false),
                new AstTag("标签三", AstTag.WARNING, false));
        AstTag tagc = new AstTag("可关闭标签", AstTag.PRIMARY, true); tagc.close(() -> {});
        render("tag-closable.png", tagc);

        // AstAlert
        JPanel alerts = new JPanel();
        alerts.setLayout(new BoxLayout(alerts, BoxLayout.Y_AXIS));
        alerts.add(new AstAlert(AstAlert.SUCCESS, "成功提示", null, false));
        alerts.add(new AstAlert(AstAlert.WARNING, "警告提示", null, false));
        alerts.add(new AstAlert(AstAlert.INFO, "信息提示", null, false));
        alerts.add(new AstAlert(AstAlert.ERROR, "错误提示", null, false));
        renderPanel("alert-default.png", alerts);
        renderPanel("alert-desc.png",
                new AstAlert(AstAlert.SUCCESS, "成功提示", "这是一段描述信息", false));
        renderPanel("alert-closable.png",
                new AstAlert(AstAlert.SUCCESS, "成功提示", "这是一段描述信息", true));

        System.out.println("DocScreenshotGen OK");
    }

    /** 多个组件横向排列渲染。 */
    static void render(String file, JComponent... comps) {
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        for (JComponent c : comps) p.add(c);
        renderPanel(file, p);
    }

    /** 直接渲染一个面板（用于纵向布局 / 单个大组件）。 */
    static void renderPanel(String file, JComponent root) {
        root.setBackground(Color.WHITE);
        root.setSize(root.getPreferredSize());
        root.doLayout();
        BufferedImage img = new BufferedImage(
                Math.max(1, root.getWidth()), Math.max(1, root.getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        root.paint(g);
        g.dispose();
        try {
            ImageIO.write(img, "png", new File(DIR, file));
        } catch (Exception e) {
            throw new RuntimeException("写入 " + file + " 失败", e);
        }
    }
}
