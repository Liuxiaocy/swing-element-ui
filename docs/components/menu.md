# Menu 导航菜单

为网站提供导航功能的菜单。

## 基本用法

默认是水平顶栏导航菜单。

![基本用法](../screenshots/menu-default.png)

```java
import org.swelement.ui.AstMenu;

// 创建导航菜单（默认水平顶栏）
AstMenu menu = new AstMenu();

// 添加菜单项
menu.addMenuItem("首页", () -> {
    System.out.println("点击首页");
});

menu.addMenuItem("新闻", () -> {
    System.out.println("点击新闻");
});

// 设置当前激活项
menu.setActive(0);
```

## 竖向模式（侧栏）

通过 `setMode(AstMenu.MODE_VERTICAL)` 将菜单切换为竖向侧栏：条目自上而下排列、激活项左侧显示主色竖条；点击带子菜单的项时子菜单向**右侧**弹出。可用 ↑ / ↓ 方向键在条目间移动激活项。侧栏宽度默认 200，可用 `setSidebarWidth` 调整。

```java
import org.swelement.ui.AstMenu;

AstMenu menu = new AstMenu();
menu.setMode(AstMenu.MODE_VERTICAL);      // 切换为竖向侧栏
menu.setSidebarWidth(200);               // 可选：调整侧栏宽度
menu.addMenuItem("首页", () -> System.out.println("首页"));
menu.addMenuItem("新闻", () -> System.out.println("新闻"));
menu.setActive(0);
```

## 子菜单

包含下拉子菜单的导航菜单。

![子菜单](../screenshots/menu-sub.png)

```java
import org.swelement.ui.AstMenu;

AstMenu menu = new AstMenu();

// 添加带子菜单的导航项
menu.addSubMenu("文档中心",
    new String[]{"快速入门", "开发者指南", "API 手册", "常见问题"},
    new Runnable[]{
        () -> System.out.println("快速入门"),
        () -> System.out.println("开发者指南"),
        () -> System.out.println("API 手册"),
        () -> System.out.println("常见问题")
    });

menu.setActive(0);
```

## 禁用状态

禁用状态的菜单。

```java
import org.swelement.ui.AstMenu;

AstMenu menu = new AstMenu();
menu.setEnabled(false);

menu.addMenuItem("首页", () -> {
    System.out.println("点击首页");
});
```

## Menu 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| mode | 菜单朝向 | int | MODE_HORIZONTAL / MODE_VERTICAL | MODE_HORIZONTAL |
| sidebarWidth | 竖向模式侧栏宽度 | int | — | 200 |

## Menu 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| addMenuItem | 添加菜单项 | String label, Runnable action | void |
| addSubMenu | 添加带子菜单的项 | String label, String[] subLabels, Runnable[] subActions | void |
| setActive | 设置当前激活项 | int index | void |
| setMode | 设置菜单朝向（水平 / 竖向） | int mode | void |
| isVertical | 是否为竖向模式 | — | boolean |
| setSidebarWidth | 设置竖向侧栏宽度 | int w | void |
