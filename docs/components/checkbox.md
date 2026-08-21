# Checkbox 多选框

一组备选项中进行多选。

## 基本用法

基础的多选框用法。

![基本用法](../screenshots/checkbox-default.png)

```java
import org.swelement.ui.Checkbox;

// 创建多选框
Checkbox checkbox1 = new Checkbox("选项A");
Checkbox checkbox2 = new Checkbox("选项B");
Checkbox checkbox3 = new Checkbox("选项C");
```

## 禁用状态

禁用状态的多选框。

![禁用状态](../screenshots/checkbox-disabled.png)

```java
// 禁用多选框
Checkbox disabledCheckbox = new Checkbox("禁用选项");
disabledCheckbox.setEnabled(false);
```

## Checkbox 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| text | 多选框文字 | String | — | — |
