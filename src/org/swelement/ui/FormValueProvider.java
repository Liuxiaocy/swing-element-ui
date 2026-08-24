package org.swelement.ui;

/** 表单取值契约：让自绘字段组件能被 AstForm 统一取值与回填。 */
public interface FormValueProvider {
    /** 返回组件当前值的字符串表示（密码模式返回明文）。 */
    String getFormValue();

    /** 用字符串写回组件值（用于表单 reset）。 */
    void setFormValue(String value);
}
