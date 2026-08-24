# Alert 提示

用于展示重要的提示信息。

## 基本用法

4 种类型的提示。

![基本用法](../screenshots/alert-default.png)

```java
import org.swelement.ui.AstAlert;

// 成功提示
AstAlert successAlert = new AstAlert(AstAlert.SUCCESS, "成功提示", null, false);

        // 警告提示
        AstAlert warningAlert = new AstAlert(AstAlert.WARNING, "警告提示", null, false);

        // 信息提示
        AstAlert infoAlert = new AstAlert(AstAlert.INFO, "信息提示", null, false);

        // 错误提示
        AstAlert errorAlert = new AstAlert(AstAlert.ERROR, "错误提示", null, false);
```

## 带描述信息

包含标题和描述的完整提示。

![带描述信息](../screenshots/alert-desc.png)

```java
// 带描述的成功提示
Alert successWithDesc = new Alert(Alert.SUCCESS, "成功提示", "这是一段描述信息", false);

// 带描述的警告提示
Alert warningWithDesc = new Alert(Alert.WARNING, "警告提示", "这是一段描述信息", false);
```

## 可关闭提示

点击关闭按钮可以关闭提示。

![可关闭提示](../screenshots/alert-closable.png)

```java
// 可关闭的提示
Alert closableAlert = new Alert(Alert.SUCCESS, "成功提示", "这是一段描述信息", true);
closableAlert.close(() -> {
    System.out.println("提示已关闭");
});
```

## Alert 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| type | 提示类型 | int | SUCCESS / WARNING / INFO / ERROR | SUCCESS |
| title | 标题 | String | — | — |
| desc | 描述信息 | String | — | null |
| closable | 是否可关闭 | boolean | — | false |

## Alert 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| close | 关闭提示 | Runnable onClosed | void |
