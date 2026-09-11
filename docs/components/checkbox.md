# Checkbox 多选框

一组备选项中进行多选。

## 基本用法

基础的多选框用法。

![基本用法](../screenshots/checkbox-default.png)

```java
import org.swelement.ui.AstCheckbox;

// 创建多选框
AstCheckbox checkbox1 = new AstCheckbox("选项A");
AstCheckbox checkbox2 = new AstCheckbox("选项B");
AstCheckbox checkbox3 = new AstCheckbox("选项C");
```

## 禁用状态

禁用状态的多选框。

![禁用状态](../screenshots/checkbox-disabled.png)

```java
import org.swelement.ui.AstCheckbox;

// 禁用多选框
AstCheckbox disabledCheckbox = new AstCheckbox("禁用选项");
disabledCheckbox.setEnabled(false);
```

## 按钮样式

将多选框渲染为 Element 风格的「分段按钮」（segmented）。选中项填满主色、文字为白色；
未选中为浅底 + 常规文字；相邻并排时形成连成一体的分段控件外观。与勾选框不同，按钮样式下
不绘制独立勾选框——整个按钮即为选中指示器，且组内各项可独立多选。

```java
import org.swelement.ui.AstCheckbox;

// 按钮样式：直角分段按钮，选中=主色填充+白字（可多选）
AstCheckbox b1 = new AstCheckbox("苹果");
AstCheckbox b2 = new AstCheckbox("香蕉");
AstCheckbox b3 = new AstCheckbox("橙子");
b1.setButtonStyle(true);
b2.setButtonStyle(true);
b3.setButtonStyle(true);
b1.setSelected(true);
b3.setSelected(true);   // 多个可同时选中
```

## Checkbox 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| text | 多选框文字 | String | — | — |
| buttonStyle | 是否启用按钮样式（分段按钮） | boolean | `true` / `false` | `false` |

## Checkbox 方法

| 方法 | 说明 |
|------|------|
| `void setButtonStyle(boolean on)` | 启用/关闭按钮样式 |
| `boolean isButtonStyle()` | 是否处于按钮样式 |
