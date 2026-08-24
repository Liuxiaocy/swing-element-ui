package org.swelement.ui;

/** 表单错误态标记：让自绘字段组件能呈现校验错误视觉（红色边框）。 */
public interface FormInvalidMarker {
    void setInvalid(boolean invalid);
}
