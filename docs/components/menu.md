# Menu 导航菜单

为网站提供导航功能的菜单。

## 基本用法

基础的垂直导航菜单。

![基本用法](../screenshots/menu-default.png)

```java
import org.swelement.ui.AstMenu;

// 创建导航菜单
AstMenu menu = new AstMenu();

// 添加菜单项
menu.

        addMenuItem("首页",() ->{
        System.out.

        println("点击首页");
});

        menu.

        addMenuItem("新闻",() ->{
        System.out.

        println("点击新闻");
});

// 设置当前激活项
        menu.

        setActive(0);
```

## 子菜单

包含下拉子菜单的导航菜单。

![子菜单](../screenshots/menu-sub.png)

```java
Menu menu = new Menu();

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
Menu menu = new Menu();
menu.setEnabled(false);

menu.addMenuItem("首页", () -> {
    System.out.println("点击首页");
});
```

## Menu 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| — | — | — | — | — |
