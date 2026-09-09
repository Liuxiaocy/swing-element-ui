# Select 选择器

当选项过多时，使用下拉菜单展示并选择内容。

## 基本用法

基础的单选下拉框。

![基本用法](../screenshots/select-default.png)

```java
import org.swelement.ui.AstSelect;

// 创建单选下拉框
AstSelect select = new AstSelect(new String[]{"黄金糕", "双皮奶", "蚵仔煎", "龙须面"});

// 获取选中值
Object value = select.getSelectedValue();
```

## 多选模式

支持多选的下拉框。**已选项以 `AstTag` 标签展示**（INFO 类型 + light 效果），标签尺寸随 Select 档位联动。

![多选模式](../screenshots/select-multiple.png)

```java
import org.swelement.ui.AstSelect;

// 创建多选下拉框
AstSelect multiSelect = new AstSelect(true, false);
multiSelect.addOption(new AstSelect.Option("黄金糕", "gold"));
multiSelect.addOption(new AstSelect.Option("双皮奶", "milk"));
multiSelect.addOption(new AstSelect.Option("蚵仔煎", "oyster"));

// 多选取值：逗号连接；批量回填同样用逗号分隔的字符串
multiSelect.setFormValue("gold,milk");
String value = multiSelect.getFormValue();        // "gold,milk"
// 注意：java.awt.* 会遮蔽 java.util.List，故此处写全限定名
java.util.List<AstSelect.Option> picked = multiSelect.getSelected();
```

### 标签交互

点按位置决定行为，无需额外配置：

| 点按区域 | 行为 |
|----------|------|
| 标签上的 `×` | 只移除该选项，不展开下拉（带 200ms 收起动画） |
| 标签其他区域 | 展开 / 收起下拉 |

Select 被 `setEnabled(false)` 时，已选项标签同步灰化且 `×` 不可点。

```java
// 多选 + 可搜索：输入过滤，勾选后落成标签
AstSelect stack = new AstSelect(true, true);
for (String s : new String[]{"Java", "Swing", "Maven"}) {
    stack.addOption(new AstSelect.Option(s, s.toLowerCase()));
}
stack.setSize(AstSelect.SIZE_LARGE);   // LARGE 档用更大的标签
```

## 可搜索

支持输入搜索的下拉框。

![可搜索](../screenshots/select-filterable.png)

```java
import org.swelement.ui.AstSelect;

// 创建可搜索下拉框
AstSelect filterSelect = new AstSelect(false, true);
filterSelect.addOption(new AstSelect.Option("黄金糕", "gold"));
filterSelect.addOption(new AstSelect.Option("双皮奶", "milk"));
filterSelect.addOption(new AstSelect.Option("蚵仔煎", "oyster"));
```

## 分组

选项分组展示。

![分组](../screenshots/select-group.png)

```java
import org.swelement.ui.AstSelect;

// 创建分组下拉框
AstSelect groupSelect = new AstSelect(false, false);
groupSelect.addOption(new AstSelect.Option("黄金糕", "gold", "热门城市", false));
groupSelect.addOption(new AstSelect.Option("双皮奶", "milk", "热门城市", false));
groupSelect.addOption(new AstSelect.Option("北京", "beijing", "城市名", false));
groupSelect.addOption(new AstSelect.Option("上海", "shanghai", "城市名", false));
```

## Select 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| multiple | 是否多选 | boolean | — | false |
| filterable | 是否可搜索 | boolean | — | false |

## AstSelect.Option 属性

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
| setSize | 尺寸档位（联动输入框高度与多选标签尺寸） | int t（SIZE_LARGE / SIZE_DEFAULT / SIZE_SMALL） | void |
| setEnabled | 启用 / 禁用（多选时联动已选标签与其 `×`） | boolean en | void |
| getFormValue | 取值：多选为逗号连接串，单选为单个值 | — | String |
| setFormValue | 回填：多选传逗号分隔串，单选传单个值 | String v | void |
