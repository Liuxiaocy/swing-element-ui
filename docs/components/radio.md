# Radio 单选框

一组备选项中进行单选。

## 基本用法

基础的单选框用法。

![基本用法](../screenshots/radio-default.png)

```java
import org.swelement.ui.AstRadio;

// 创建单选框
AstRadio radio1 = new AstRadio("选项A");
AstRadio radio2 = new AstRadio("选项B");
AstRadio radio3 = new AstRadio("选项C");
```

## 禁用状态

禁用状态的单选框。

![禁用状态](../screenshots/radio-disabled.png)

```java
import org.swelement.ui.AstRadio;

// 禁用单选框
AstRadio disabledRadio = new AstRadio("禁用选项");
disabledRadio.setEnabled(false);
```

## 按钮样式

将单选框渲染为 Element 风格的「分段按钮」（segmented）。选中项填满主色、文字为白色；
未选中为浅底 + 常规文字；相邻并排时形成连成一体的分段控件外观。

```java
import org.swelement.ui.AstRadio;

// 按钮样式：直角分段按钮，选中=主色填充+白字
AstRadio r1 = new AstRadio("上海");
AstRadio r2 = new AstRadio("北京");
AstRadio r3 = new AstRadio("广州");
r1.setButtonStyle(true);
r2.setButtonStyle(true);
r3.setButtonStyle(true);
r2.setSelected(true);   // 默认选中项（填满主色）

// 组互斥：同一 Group 内最多一个被选中
AstRadio.Group g = new AstRadio.Group();
g.add(r1); g.add(r2); g.add(r3);
```

## Radio 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| text | 单选框文字 | String | — | — |
| buttonStyle | 是否启用按钮样式（分段按钮） | boolean | `true` / `false` | `false` |

## Radio 方法

| 方法 | 说明 |
|------|------|
| `void setButtonStyle(boolean on)` | 启用/关闭按钮样式 |
| `boolean isButtonStyle()` | 是否处于按钮样式 |