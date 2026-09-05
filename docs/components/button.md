# Button 按钮

常用的操作按钮。

## 基本用法

基础的按钮用法。

![基本用法](../screenshots/button-default.png)

```java
import org.swelement.ui.AstButton;

// 默认按钮
AstButton defaultBtn = new AstButton("默认按钮");

// 主要按钮
AstButton primaryBtn = new AstButton("主要按钮", AstButton.PRIMARY, false);

// 成功按钮
AstButton successBtn = new AstButton("成功按钮", AstButton.SUCCESS, false);

// 警告按钮
AstButton warningBtn = new AstButton("警告按钮", AstButton.WARNING, false);

// 危险按钮
AstButton dangerBtn = new AstButton("危险按钮", AstButton.DANGER, false);

// 信息按钮
AstButton infoBtn = new AstButton("信息按钮", AstButton.INFO, false);
```

## 朴素按钮

朴素风格的按钮。

![朴素按钮](../screenshots/button-plain.png)

```java
import org.swelement.ui.AstButton;

// 朴素主要按钮
AstButton plainPrimary = new AstButton("朴素 主要", AstButton.PRIMARY, true);

// 朴素成功按钮
AstButton plainSuccess = new AstButton("朴素 成功", AstButton.SUCCESS, true);

// 朴素警告按钮
AstButton plainWarning = new AstButton("朴素 警告", AstButton.WARNING, true);

// 朴素危险按钮
AstButton plainDanger = new AstButton("朴素 危险", AstButton.DANGER, true);

// 朴素信息按钮
AstButton plainInfo = new AstButton("朴素 信息", AstButton.INFO, true);
```

## 禁用状态

禁用状态的按钮。

![禁用状态](../screenshots/button-disabled.png)

```java
import org.swelement.ui.AstButton;

// 禁用主要按钮
AstButton disabledPrimary = new AstButton("禁用-主要", AstButton.PRIMARY, false);
disabledPrimary.setEnabled(false);

// 禁用朴素按钮
AstButton disabledPlain = new AstButton("禁用-朴素", AstButton.PRIMARY, true);
disabledPlain.setEnabled(false);

// 禁用默认按钮
AstButton disabledDefault = new AstButton("禁用-默认", AstButton.DEFAULT, false);
disabledDefault.setEnabled(false);
```

## Button 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| text | 按钮文字 | String | — | — |
| type | 按钮类型 | int | DEFAULT / PRIMARY / SUCCESS / WARNING / DANGER / INFO | DEFAULT |
| plain | 是否朴素按钮 | boolean | — | false |
