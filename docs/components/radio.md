# Radio 单选框

一组备选项中进行单选。

## 基本用法

基础的单选框用法。

![基本用法](../screenshots/radio-default.png)

```java
import org.swelement.ui.Radio;

// 创建单选框
Radio radio1 = new Radio("选项A");
Radio radio2 = new Radio("选项B");
Radio radio3 = new Radio("选项C");
```

## 禁用状态

禁用状态的单选框。

![禁用状态](../screenshots/radio-disabled.png)

```java
// 禁用单选框
Radio disabledRadio = new Radio("禁用选项");
disabledRadio.setEnabled(false);
```

## Radio 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| text | 单选框文字 | String | — | — |