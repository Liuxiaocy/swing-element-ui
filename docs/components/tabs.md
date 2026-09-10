# Tabs 标签页

分隔内容上有关联但属于不同类别的数据集合。

## 基本用法

基础的标签页用法。

![基本用法](../screenshots/tabs-default.png)

```java
import org.swelement.ui.AstTabs;
import javax.swing.JPanel;
import javax.swing.JLabel;

// 创建标签页
AstTabs tabs = new AstTabs(new String[]{"用户管理", "配置管理", "角色管理", "定时任务补偿"}, 0);

// 添加内容面板
JPanel panel1 = new JPanel();
panel1.add(new JLabel("用户管理内容"));
tabs.addTab("用户管理", panel1);

// 监听标签切换
tabs.addChangeListener(e -> {
    int index = tabs.getSelectedIndex();
    String title = tabs.getSelectedTitle();
    System.out.println("切换到: " + title);
});
```

## 竖向模式（侧栏）

通过 `setMode(AstTabs.MODE_VERTICAL)` 将标签页切换为竖向侧栏：标签自上而下排列在左侧，选中项左侧显示主色竖条，内容面板显示在右侧。可用 ↑ / ↓ 方向键切换标签（水平模式为 ← / →）。侧栏宽度默认 200，可用 `setSidebarWidth` 调整。

```java
import org.swelement.ui.AstTabs;
import javax.swing.JPanel;
import javax.swing.JLabel;

// 创建标签页并切换为竖向侧栏
AstTabs tabs = new AstTabs(new String[]{"概览", "订单", "商品"}, 0);
tabs.setMode(AstTabs.MODE_VERTICAL);   // 切换为竖向侧栏
tabs.setSidebarWidth(200);             // 可选：调整侧栏宽度（默认 200）

JPanel panel1 = new JPanel();
panel1.add(new JLabel("概览内容"));
tabs.addTab("概览", panel1);
```

## Tabs 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| tabTitles | 标签标题数组 | String[] | — | — |
| initialIndex | 初始选中索引 | int | — | 0 |
| mode | 标签排列方向 | int | horizontal（0）/ vertical（1） | horizontal（0） |
| sidebarWidth | 竖向模式侧栏宽度（仅竖向生效） | int | > 0 | 200 |

## Tabs 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| addTab | 添加标签页 | String title, JComponent panel | void |
| getSelectedIndex | 获取当前选中索引 | — | int |
| setSelectedIndex | 设置选中索引 | int i | void |
| getSelectedTitle | 获取当前选中标题 | — | String |
| addChangeListener | 添加切换监听器 | ChangeListener l | void |
| setMode | 设置排列方向（非法值抛 IllegalArgumentException） | int m（`MODE_HORIZONTAL` / `MODE_VERTICAL`） | void |
| getMode | 获取当前排列方向 | — | int |
| isVertical | 是否为竖向（侧栏）模式 | — | boolean |
| setSidebarWidth | 设置竖向侧栏宽度（≤0 抛 IllegalArgumentException） | int w | void |
