# Progress 进度条

用于展示操作进度，告知用户当前状态和预期。

## 基本用法

线性进度条。

![基本用法](../screenshots/progress-default.png)

```java
import org.swelement.ui.AstProgress;

// 创建进度条
AstProgress progress = new AstProgress(50);

// 动态更新进度
progress.

        setValue(75);
```

## 显示文字

进度条右侧显示百分比文字。

![显示文字](../screenshots/progress-text.png)

```java
// 创建显示文字的进度条
Progress progressWithText = new Progress(60);
progressWithText.setShowText(true);
```

## Progress 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| initialValue | 初始进度值 | int | 0-100 | 0 |
| showText | 是否显示文字 | boolean | — | true |

## Progress 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| getValue | 获取当前进度 | — | int |
| setValue | 设置当前进度 | int v | void |
| setShowText | 设置是否显示文字 | boolean b | void |
| addChangeListener | 添加进度变化监听器 | ChangeListener l | void |