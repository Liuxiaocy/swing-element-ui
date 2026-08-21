package org.swelement.ui;

import org.swelement.core.Animator;
import org.swelement.core.Easing;
import org.swelement.core.ElementTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * 表单组件 — 支持字段校验规则和错误提示动画。
 *
 * 用法：
 *   AstForm form = new AstForm();
 *   JTextField nameField = new JTextField(20);
 *   form.addField("姓名", nameField, new AstForm.RequiredRule());
 *   JTextField emailField = new JTextField(20);
 *   form.addField("邮箱", emailField, new AstForm.RequiredRule(), new AstForm.EmailRule());
 *   if (form.validateForm()) { System.out.println("校验通过"); }
 *
 * 注意：因 JPanel 继承自 java.awt.Container，已存在 void validate() 方法，
 * 故表单校验方法命名为 validateForm() 以避免返回类型冲突。
 */
public class AstForm extends JPanel {
    // --- Validation rules ---
    public interface ValidationRule {
        String validate(String value, String fieldLabel);
        // Return null if valid, error message string if invalid
    }

    public static class RequiredRule implements ValidationRule {
        public String validate(String value, String fieldLabel) {
            if (value == null || value.trim().isEmpty()) return fieldLabel + "不能为空";
            return null;
        }
    }

    public static class MinLengthRule implements ValidationRule {
        private final int min;
        public MinLengthRule(int min) { this.min = min; }
        public String validate(String value, String fieldLabel) {
            if (value == null) return fieldLabel + "不能为空";
            if (value.length() < min) return fieldLabel + "至少" + min + "个字符";
            return null;
        }
    }

    public static class MaxLengthRule implements ValidationRule {
        private final int max;
        public MaxLengthRule(int max) { this.max = max; }
        public String validate(String value, String fieldLabel) {
            if (value != null && value.length() > max) return fieldLabel + "不超过" + max + "个字符";
            return null;
        }
    }

    public static class RegexRule implements ValidationRule {
        private final java.util.regex.Pattern pattern;
        private final String message;
        public RegexRule(String regex, String message) {
            this.pattern = java.util.regex.Pattern.compile(regex);
            this.message = message;
        }
        public String validate(String value, String fieldLabel) {
            if (value == null || value.trim().isEmpty()) return null; // Let RequiredRule handle empty
            if (!pattern.matcher(value).matches()) return message;
            return null;
        }
    }

    public static class EmailRule extends RegexRule {
        public EmailRule() { super("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", "邮箱格式不正确"); }
    }

    public static class PhoneRule extends RegexRule {
        public PhoneRule() { super("^1[3-9]\\d{9}$", "手机号格式不正确"); }
    }

    // --- Form fields ---
    private final List<FormField> fields = new ArrayList<FormField>();
    private final Map<String, String> errors = new LinkedHashMap<String, String>();

    public AstForm() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
    }

    public void addField(String label, JComponent input, ValidationRule... rules) {
        if (label == null) throw new IllegalArgumentException("label must not be null");
        if (input == null) throw new IllegalArgumentException("input must not be null");
        FormField field = new FormField(label, input, rules);
        fields.add(field);
        add(field);
        add(Box.createVerticalStrut(8));
    }

    public boolean validateForm() {
        errors.clear();
        boolean allValid = true;
        for (FormField f : fields) {
            String value = extractValue(f.input);
            String firstError = null;
            for (ValidationRule rule : f.rules) {
                String err = rule.validate(value, f.label);
                if (err != null) { firstError = err; break; }
            }
            if (firstError != null) {
                errors.put(f.label, firstError);
                f.showError(firstError);
                allValid = false;
            } else {
                f.clearError();
            }
        }
        return allValid;
    }

    public Map<String, String> getErrors() { return new LinkedHashMap<String, String>(errors); }

    public void clearAllErrors() {
        errors.clear();
        for (FormField f : fields) f.clearError();
    }

    private String extractValue(JComponent input) {
        if (input instanceof JTextField) return ((JTextField) input).getText();
        if (input instanceof JTextArea) return ((JTextArea) input).getText();
        if (input instanceof JComboBox) {
            Object sel = ((JComboBox<?>) input).getSelectedItem();
            return sel == null ? "" : sel.toString();
        }
        return "";
    }

    // --- FormField: one row with label + input + error ---
    private final class FormField extends JPanel {
        final String label;
        final JComponent input;
        final ValidationRule[] rules;
        final JLabel errorLabel;
        final Animator errorAnim;
        float errorAlpha;

        FormField(String label, JComponent input, ValidationRule[] rules) {
            this.label = label;
            this.input = input;
            this.rules = rules == null ? new ValidationRule[0] : rules;
            setLayout(new BorderLayout(12, 4));
            setOpaque(false);
            // Label: 100px width, right-aligned
            JLabel lbl = new JLabel(label + "：", JLabel.RIGHT);
            lbl.setPreferredSize(new Dimension(100, 28));
            lbl.setFont(ElementTheme.FONT.deriveFont(14f));
            lbl.setForeground(ElementTheme.TEXT_REGULAR);
            ElementTheme.assertContrast(ElementTheme.TEXT_REGULAR, Color.WHITE, "AstForm label");
            add(lbl, BorderLayout.WEST);
            // Input: CENTER
            add(input, BorderLayout.CENTER);
            // Error label: SOUTH, hidden initially
            errorLabel = new JLabel(" ");
            errorLabel.setFont(ElementTheme.FONT.deriveFont(12f));
            errorLabel.setForeground(ElementTheme.DANGER);
            errorLabel.setPreferredSize(new Dimension(0, 0)); // collapsed
            errorLabel.setVisible(false);
            add(errorLabel, BorderLayout.SOUTH);
            // Error fade-in animator
            errorAnim = new Animator(200, new Easing() { public float apply(float t) { return Easing.easeOut(t); }},
                new Animator.Listener() { public void update(float v) {
                    errorAlpha = v;
                    errorLabel.setPreferredSize(new Dimension(0, Math.round(20 * v)));
                    errorLabel.setForeground(new Color(ElementTheme.DANGER.getRed(), ElementTheme.DANGER.getGreen(), ElementTheme.DANGER.getBlue(), Math.round(255 * v)));
                    revalidate(); repaint();
                }});
        }

        void showError(String msg) {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
            errorAnim.stop();
            errorAnim.go(errorAlpha, 1f);
            // Also set input border to DANGER
            if (input instanceof JTextField) {
                ((JTextField) input).setBorder(BorderFactory.createLineBorder(ElementTheme.DANGER, 1));
            }
        }

        void clearError() {
            errorAnim.stop();
            errorAnim.go(errorAlpha, 0f);
            if (input instanceof JTextField) {
                ((JTextField) input).setBorder(BorderFactory.createLineBorder(ElementTheme.BORDER_BASE, 1));
            }
        }
    }

    // --- Self-check ---
    static void selfCheck() {
        // Null guards
        boolean threw = false;
        AstForm f0 = new AstForm();
        try { f0.addField(null, new JTextField(), new RequiredRule()); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null label"; threw = false;
        try { f0.addField("x", null, new RequiredRule()); } catch (IllegalArgumentException e) { threw = true; }
        assert threw : "null input";

        // Rule tests
        RequiredRule rr = new RequiredRule();
        assert rr.validate(null, "姓名") != null : "required null fails";
        assert rr.validate("", "姓名") != null : "required empty fails";
        assert rr.validate("  ", "姓名") != null : "required blank fails";
        assert rr.validate("abc", "姓名") == null : "required non-empty passes";

        MinLengthRule ml = new MinLengthRule(3);
        assert ml.validate("ab", "f") != null : "min length 3 fails for 2 chars";
        assert ml.validate("abc", "f") == null : "min length 3 passes for 3 chars";

        MaxLengthRule xl = new MaxLengthRule(5);
        assert xl.validate("abcdef", "f") != null : "max length 5 fails for 6 chars";
        assert xl.validate("abcde", "f") == null : "max length 5 passes for 5 chars";

        EmailRule er = new EmailRule();
        assert er.validate("not-an-email", "f") != null : "invalid email fails";
        assert er.validate("test@example.com", "f") == null : "valid email passes";
        assert er.validate("", "f") == null : "empty email passes (let Required handle)";

        PhoneRule pr = new PhoneRule();
        assert pr.validate("12345", "f") != null : "invalid phone fails";
        assert pr.validate("13800138000", "f") == null : "valid phone passes";

        // Form validate() test
        final Throwable[] err = {null};
        try { SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            JFrame jf = new JFrame("Form SC"); jf.setSize(600, 500); jf.setVisible(true);
            AstForm form = new AstForm();
            JTextField nameField = new JTextField("", 20);
            form.addField("姓名", nameField, new RequiredRule(), new MinLengthRule(2));
            JTextField emailField = new JTextField("bad-email", 20);
            form.addField("邮箱", emailField, new RequiredRule(), new EmailRule());
            JTextField phoneField = new JTextField("13800138000", 20);
            form.addField("手机", phoneField, new RequiredRule(), new PhoneRule());
            jf.getContentPane().setLayout(new BorderLayout()); jf.getContentPane().add(form, BorderLayout.CENTER);
            jf.pack();
            // Validate — should fail (name empty, email invalid)
            boolean ok = form.validateForm();
            assert !ok : "validate fails with empty name + bad email";
            Map<String, String> errs = form.getErrors();
            assert errs.size() == 2 : "2 errors expected; actual=" + errs.size();
            assert errs.containsKey("姓名") : "has name error";
            assert errs.containsKey("邮箱") : "has email error";
            assert !errs.containsKey("手机") : "phone should pass";
            // Fix name + email → validate again
            nameField.setText("张三");
            emailField.setText("zhangsan@example.com");
            boolean ok2 = form.validateForm();
            assert ok2 : "validate passes after fixes";
            assert form.getErrors().isEmpty() : "no errors after fix";
            // Off-screen paint form to trigger assertContrast for label
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(400, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = img.createGraphics();
            try { form.paint(gg); } finally { gg.dispose(); }
            jf.dispose();
        }}); } catch (Throwable t) { err[0] = t; }
        if (err[0] != null) throw new RuntimeException(err[0]);
        System.out.println("AstForm self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
