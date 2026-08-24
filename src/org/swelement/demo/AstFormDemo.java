package org.swelement.demo;

import org.swelement.core.ElementTheme;
import org.swelement.ui.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * AstForm 表单子体系 Demo — 注册表单。
 *
 * 演示能力：
 *   - 标签左定位（POS_LEFT）+ 自定义 labelWidth
 *   - 必填字段前的红色 * 标记（RequiredRule 自动附加）
 *   - FormValueProvider 取值契约：AstInput / AstSelect / AstDatePicker / AstInputNumber 统一取值
 *   - TypeRule(NUMBER/DATE) 与 EmailRule 等规则校验
 *   - 校验失败时错误文案淡入 + 控件红色边框 + 抖动动画
 *   - reset() 将各字段写回初始值并清除错误态
 */
public class AstFormDemo {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
        SwingUtilities.invokeLater(new Runnable() { public void run() { start(); } });
    }

    private static void start() {
        final JFrame f = new JFrame("AstForm Demo - 注册表单");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // --- 表单 ---
        final AstForm form = new AstForm();
        form.setLabelPosition(AstForm.POS_LEFT);
        form.setLabelWidth(80);

        final AstInput username = new AstInput("请输入用户名");
        form.addField("用户名", username, new AstForm.RequiredRule(), new AstForm.MinLengthRule(3));

        final AstInput email = new AstInput("请输入邮箱");
        form.addField("邮箱", email, new AstForm.RequiredRule(), new AstForm.EmailRule());

        final AstSelect role = new AstSelect(false, false);
        role.addOption(new AstSelect.Option("普通用户", "user"));
        role.addOption(new AstSelect.Option("管理员", "admin"));
        role.addOption(new AstSelect.Option("访客", "guest"));
        form.addField("角色", role, new AstForm.RequiredRule());

        final AstDatePicker birthday = new AstDatePicker();
        form.addField("生日", birthday, new AstForm.TypeRule(AstForm.TypeRule.Kind.DATE));

        final AstInputNumber quantity = new AstInputNumber(0, 100, 1, 1);
        form.addField("数量", quantity, new AstForm.TypeRule(AstForm.TypeRule.Kind.NUMBER));

        // --- 按钮区 ---
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        final JLabel echo = new JLabel("填写表单后点击「提交校验」。");
        echo.setForeground(new Color(0x606266));
        echo.setFont(echo.getFont().deriveFont(13f));

        AstButton submit = new AstButton("提交校验", AstButton.PRIMARY, false);
        submit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (form.validateForm()) {
                    echo.setForeground(ElementTheme.SUCCESS);
                    echo.setText("校验通过！用户名=" + username.getFormValue()
                            + " 角色=" + role.getFormValue()
                            + " 生日=" + birthday.getFormValue()
                            + " 数量=" + quantity.getFormValue());
                } else {
                    echo.setForeground(ElementTheme.DANGER);
                    echo.setText("校验未通过：" + form.getErrors().size() + " 个字段有误。");
                }
            }
        });

        AstButton reset = new AstButton("重置", AstButton.DEFAULT, false);
        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                form.reset();
                echo.setForeground(new Color(0x606266));
                echo.setText("已重置。");
            }
        });

        btnRow.add(submit);
        btnRow.add(reset);
        btnRow.add(echo);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.add(form, BorderLayout.CENTER);
        center.add(btnRow, BorderLayout.SOUTH);
        center.setBorder(new TitledBorder("注册信息"));

        root.add(center, BorderLayout.CENTER);
        f.add(root);
        f.setSize(540, 440);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
