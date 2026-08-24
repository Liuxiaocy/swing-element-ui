# Tag 标记

用于标记和选择。

## 基本用法

不同类型的标记。

![基本用法](../screenshots/tag-default.png)

```java
import org.swelement.ui.AstTag;
import org.swelement.ui.Tag;

// 主要标记
AstTag primaryTag = new AstTag("标签一", AstTag.PRIMARY, false);

        // 成功标记
        AstTag successTag = new AstTag("标签二", AstTag.SUCCESS, false);

        // 警告标记
        AstTag warningTag = new AstTag("标签三", AstTag.WARNING, false);

        // 危险标记
        AstTag dangerTag = new AstTag("标签四", AstTag.DANGER, false);

        // 信息标记
        AstTag infoTag = new AstTag("标签五", AstTag.INFO, false);
```

## 可关闭标记

点击关闭按钮可以移除标记。

![可关闭标记](../screenshots/tag-closable.png)

```java
// 可关闭标记
Tag closableTag = new Tag("可关闭标签", Tag.PRIMARY, true);
closableTag.close(() -> {
    System.out.println("标签已关闭");
});
```

## Tag 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| text | 标记文字 | String | — | — |
| type | 标记类型 | int | PRIMARY / SUCCESS / WARNING / DANGER / INFO | PRIMARY |
| closable | 是否可关闭 | boolean | — | false |

## Tag 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| getText | 获取标记文字 | — | String |
| setText | 设置标记文字 | String t | void |
| close | 关闭标记 | Runnable onClosed | void |
