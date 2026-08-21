# Select 选择器

当选项过多时，使用下拉菜单展示并选择内容。

## 基本用法

基础的单选下拉框。

![基本用法](../screenshots/select-default.png)

```java
import org.swelement.ui.Select;

// 创建单选下拉框
Select select = new Select(new String[]{"黄金糕", "双皮奶", "蚵仔煎", "龙须面"});

// 获取选中值
Object value = select.getSelectedValue();
```

## 多选模式

支持多选的下拉框。

![多选模式](../screenshots/select-multiple.png)

```java
// 创建多选下拉框
Select multiSelect = new Select(true, false);
multiSelect.addOption(new Select.Option("黄金糕", "gold"));
multiSelect.addOption(new Select.Option("双皮奶", "milk"));
multiSelect.addOption(new Select.Option("蚵仔煎", "oyster"));
```

## 可搜索

支持输入搜索的下拉框。

![可搜索](../screenshots/select-filterable.png)

```java
// 创建可搜索下拉框
Select filterSelect = new Select(false, true);
filterSelect.addOption(new Select.Option("黄金糕", "gold"));
filterSelect.addOption(new Select.Option("双皮奶", "milk"));
filterSelect.addOption(new Select.Option("蚵仔煎", "oyster"));
```

## 分组

选项分组展示。

![分组](../screenshots/select-group.png)

```java
// 创建分组下拉框
Select groupSelect = new Select(false, false);
groupSelect.addOption(new Select.Option("黄金糕", "gold", "热门城市", false));
groupSelect.addOption(new Select.Option("双皮奶", "milk", "热门城市", false));
groupSelect.addOption(new Select.Option("北京", "beijing", "城市名", false));
groupSelect.addOption(new Select.Option("上海", "shanghai", "城市名", false));
```

## Select 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| multiple | 是否多选 | boolean | — | false |
| filterable | 是否可搜索 | boolean | — | false |

## Select.Option 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| label | 选项显示文字 | String | — | — |
| value | 选项值 | Object | — | — |
| group | 分组名称 | String | — | null |
| disabled | 是否禁用 | boolean | — | false |

## Select 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| addOption | 添加选项 | Option o | void |
| getSelectedValue | 获取选中值（单选） | — | Object |
| setSelectedValue | 设置选中值（单选） | Object value | void |
| getSelectedIndex | 获取选中索引 | — | int |
| setSelectedIndex | 设置选中索引 | int i | void |
| getSelected | 获取所有选中项 | — | List\<Option\> |
| clearSelection | 清空选中 | — | void |
| getOptions | 获取所有选项 | — | List\<Option\> |
