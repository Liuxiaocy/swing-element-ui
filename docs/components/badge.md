# Badge 角标

按钮和图标上的状态标识。

## 基本用法

数字角标。

![基本用法](../screenshots/badge-default.png)

```java
import org.swelement.ui.AstBadge;
import org.swelement.ui.Badge;

// 创建角标
AstBadge badge = new AstBadge();
badge.

        setContent(new JButton("消息"));
        badge.

        setCount(12);
```

## 点状角标

只显示一个小红点，不显示具体数字。

![点状角标](../screenshots/badge-dot.png)

```java
// 创建点状角标
Badge dotBadge = new Badge();
dotBadge.setContent(new JButton("消息"));
dotBadge.setDot(true);
```

## Badge 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| count | 角标数字 | int | — | 0 |
| dot | 是否显示点状角标 | boolean | — | false |

## Badge 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| setContent | 设置角标内容组件 | JComponent c | void |
| setCount | 设置角标数字 | int c | void |
| setDot | 设置是否显示点状角标 | boolean b | void |
