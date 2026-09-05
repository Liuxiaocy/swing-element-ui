# Input 输入框

输入框组件，支持占位符、可清空、密码切换、前后缀图标、前后缀复合元素、最大长度与字数统计，以及输入建议（autocomplete）。

## 基本用法

基础的输入框用法。

```java
import org.swelement.ui.AstInput;

// 带占位符的输入框
AstInput input = new AstInput("请输入内容");
```

## 可清空

输入框右侧显示清空按钮。

```java
import org.swelement.ui.AstInput;

// 输入内容后，鼠标悬停显示清空按钮
AstInput clearableInput = new AstInput("请输入内容");
```

## 密码框

`AstInput(TEXT)` 第二个参数传 `AstInput.PASSWORD` 即为密码框，自带眼睛按钮可切换明文。

```java
import org.swelement.ui.AstInput;

AstInput pwd = new AstInput("请输入密码", AstInput.PASSWORD);
```

## 前后缀图标

`setPrefixIcon(int)` / `setSuffixIcon(int)` 接收 `AstIcon.Type` 常量。
重复设置会替换、不叠加。

```java
import org.swelement.ui.AstIcon;
import org.swelement.ui.AstInput;

AstInput search = new AstInput("搜索");
search.setPrefixIcon(AstIcon.SEARCH);

AstInput config = new AstInput("");
config.setSuffixIcon(AstIcon.SETTING);
```

## 前后缀复合元素

`setPrefixComponent(JComponent)` / `setSuffixComponent(JComponent)` 接受任意 Swing 组件作为前缀/后缀，常用于：
URL 前缀（`Http://`）、单位后缀（`.com`）、表单校验图标、操作按钮等。

```java
import javax.swing.JLabel;
import org.swelement.ui.AstInput;

AstInput url = new AstInput("example");
url.setPrefixComponent(new JLabel("Http://"));
url.setSuffixComponent(new JLabel(".com"));
```

## 最大长度与字数统计

`setMaxLength(int)` 一旦传入正整数，立即启用 DocumentFilter 截断超长输入，
同时在右侧渲染 `当前长度/上限` 字数标签（`TEXT_REGULAR` 配色，对比度达标）。

```java
import org.swelement.ui.AstInput;

AstInput tweet = new AstInput("说点什么");
tweet.setMaxLength(140);
```

`setMaxLength(0)` 解除限制并隐藏字数。

## 输入建议（autocomplete）

通过 `setSuggestions(String...)` 注册候选项，浮层基于 `AnimatedPopup`：
- `setShowSuggestionsOnFocus(true)` 聚焦即展开全部候选；否则仅在用户键入后展示
- 输入文本与候选项**不区分大小写**做包含匹配
- 上下方向键 / 鼠标点击候选 → 回填文本
- 点击浮层外部自动收起

```java
import org.swelement.ui.AstInput;

AstInput fruit = new AstInput("输入水果名");
fruit.setSuggestions("apple", "apricot", "banana", "cherry");
fruit.setShowSuggestionsOnFocus(true);  // 聚焦即展示全部
// 或保留默认：仅在键入时按子串过滤
```

也可传入 List：

```java
import java.util.Arrays;
import org.swelement.ui.AstInput;

AstInput tag = new AstInput("添加标签");
tag.setSuggestions(Arrays.asList("java", "javascript", "python", "rust"));
```

## 禁用状态

```java
import org.swelement.ui.AstInput;

AstInput disabledInput = new AstInput("请输入内容");
disabledInput.setEnabled(false);
```

## Input 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| placeholder | 占位符文本 | String | — | — |
| tier | 尺寸档位（构造后 `setSize`） | int | `SIZE_LARGE`=0 / `SIZE_DEFAULT`=1 / `SIZE_SMALL`=2 | `SIZE_DEFAULT` |
| type | 输入框类型 | int | `TEXT`=0 / `PASSWORD`=1 | `TEXT` |

## Input 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| getText | 获取输入框文本 | — | String |
| setText | 设置输入框文本 | String t | void |
| setPrefixIcon | 设置前缀图标（`AstIcon.Type`） | int iconType | void |
| setSuffixIcon | 设置后缀图标（`AstIcon.Type`） | int iconType | void |
| setPrefixComponent | 设置前缀复合元素 | JComponent c | void |
| setSuffixComponent | 设置后缀复合元素 | JComponent c | void |
| setMaxLength | 设置最大长度（>0 启用 DocumentFilter 与字数统计；0 解除） | int max | void |
| setSuggestions | 设置输入建议候选项（数组） | String... items | void |
| setSuggestions | 设置输入建议候选项（列表） | List&lt;String&gt; items | void |
| setShowSuggestionsOnFocus | 聚焦是否立即展开全部候选 | boolean | void |
| setSize | 切换尺寸档位 | int tier | void |
| setEnabled | 启用/禁用 | boolean | void |