# Slider 滑块

通过拖动选择一个数值范围。

## 基本用法

基础的滑块用法。

![基本用法](../screenshots/slider-default.png)

```java
import org.swelement.ui.AstSlider;

// 创建滑块（最小值0，最大值100，初始值50）
AstSlider slider = new AstSlider(0, 100, 50);
```

## 禁用状态

禁用状态的滑块。

![禁用状态](../screenshots/slider-disabled.png)

```java
// 禁用滑块
Slider disabledSlider = new Slider(0, 100, 50);
disabledSlider.setEnabled(false);
```

## Slider 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| min | 最小值 | int | — | — |
| max | 最大值 | int | — | — |
| value | 初始值 | int | — | — |

## Slider 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| getValue | 获取当前值 | — | int |
| setValue | 设置当前值 | int v | void |
| addChangeListener | 添加值变化监听器 | ChangeListener l | void |
