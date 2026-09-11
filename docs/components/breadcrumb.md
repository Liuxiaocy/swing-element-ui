# Breadcrumb 面包屑

显示当前页面的路径，并支持回到任意上级页面。

## 基本用法

最后一段为「当前页」，不可点击；其余段点击触发 `setItemClickListener`。分隔符默认 `/`。

```java
import org.swelement.ui.AstBreadcrumb;
import java.util.Arrays;

// 创建面包屑（元素不可为 null）
AstBreadcrumb bc = new AstBreadcrumb(Arrays.asList("首页", "用户管理", "详情"));

// 设置分隔符（默认 "/"）
bc.setSeparator("/");

// 监听某一段点击（末段不触发）
bc.setItemClickListener(idx -> System.out.println("跳转到第 " + idx + " 段"));
```

## 自定义分隔符

```java
import org.swelement.ui.AstBreadcrumb;
import java.util.Arrays;

AstBreadcrumb bc = new AstBreadcrumb(Arrays.asList("控制台", "系统设置", "成员管理"));
bc.setSeparator(">");
```

## 替换路径

```java
import org.swelement.ui.AstBreadcrumb;
import java.util.Arrays;

AstBreadcrumb bc = new AstBreadcrumb(Arrays.asList("首页", "详情"));
bc.setItems(Arrays.asList("首页", "订单", "详情"));   // 返回副本，原列表不受影响
```

## Breadcrumb 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| items | 路径段列表（不可为 null，元素不可为 null） | List\<String\> | — | — |
| separator | 段间分隔符 | String | — | "/" |

## Breadcrumb 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| setItems | 替换路径段列表（元素不可为 null） | List\<String\> items | void |
| getItems | 获取路径段列表（返回副本） | — | List\<String\> |
| setSeparator | 设置分隔符（null 抛 IllegalArgumentException） | String s | void |
| setItemClickListener | 设置段点击监听（null 抛 IllegalArgumentException；末段不触发） | Consumer\<Integer\> l | void |
