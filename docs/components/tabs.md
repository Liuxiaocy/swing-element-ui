# Tabs 标签页

分隔内容上有关联但属于不同类别的数据集合。

## 基本用法

基础的标签页用法。

![基本用法](../screenshots/tabs-default.png)

```java
import org.swelement.ui.AstTabs;

// 创建标签页
AstTabs tabs = new AstTabs(new String[]{"用户管理", "配置管理", "角色管理", "定时任务补偿"}, 0);

        // 添加内容面板
        JPanel panel1 = new JPanel();
panel1.

        add(new JLabel("用户管理内容"));
        tabs.

        addTab("用户管理",panel1);

// 监听标签切换
tabs.

        addChangeListener(e ->{
        int index = tabs.getSelectedIndex();
        String title = tabs.getSelectedTitle();
    System.out.

        println("切换到: "+title);
});
```

## Tabs 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| tabTitles | 标签标题数组 | String[] | — | — |
| initialIndex | 初始选中索引 | int | — | 0 |

## Tabs 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| addTab | 添加标签页 | String title, JComponent panel | void |
| getSelectedIndex | 获取当前选中索引 | — | int |
| setSelectedIndex | 设置选中索引 | int i | void |
| getSelectedTitle | 获取当前选中标题 | — | String |
| addChangeListener | 添加切换监听器 | ChangeListener l | void |
