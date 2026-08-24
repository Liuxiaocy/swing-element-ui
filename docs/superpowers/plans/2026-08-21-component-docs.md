# swing-element-ui 组件文档实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 14 个已实现组件编写仿 Element 官网风格的使用文档

**Architecture:** 每个组件独立 Markdown 文件，放在 `docs/components/` 目录，包含基本用法、样式截图、属性表格（五列）

**Tech Stack:** Markdown, Java 代码示例

## Global Constraints

- 文档语言：中文
- 文件格式：Markdown
- 属性表格：五列（参数、说明、类型、可选值、默认值）
- 截图引用：`../screenshots/{组件名}-{样式}.png`
- 代码示例：可直接运行的 Java 代码

---

## File Structure

```
docs/
  components/
    button.md
    input.md
    checkbox.md
    radio.md
    switch.md
    slider.md
    select.md
    tabs.md
    pagination.md
    menu.md
    tag.md
    progress.md
    badge.md
    alert.md
```

---

## Task 1: 创建目录结构

**Files:**
- Create: `docs/components/.gitkeep`
- Create: `docs/screenshots/.gitkeep`

**Interfaces:**
- Produces: 空目录结构

- [ ] **Step 1: 创建目录**

```bash
mkdir -p docs/components docs/screenshots
touch docs/components/.gitkeep docs/screenshots/.gitkeep
```

- [ ] **Step 2: 验证目录存在**

```bash
ls -la docs/
```

- [ ] **Step 3: 提交**

```bash
git add docs/components/.gitkeep docs/screenshots/.gitkeep
git commit -m "docs: 创建文档目录结构"
```

---

## Task 2: Button 按钮文档

**Files:**
- Create: `docs/components/button.md`

**Interfaces:**
- Produces: Button 组件使用文档

- [ ] **Step 1: 创建 button.md**

```markdown
# Button 按钮

常用的操作按钮。

## 基本用法

基础的按钮用法。

![基本用法](../screenshots/button-default.png)

```java
import org.swelement.ui.AstButton;

// 默认按钮
Button defaultBtn = new Button("默认按钮");

// 主要按钮
Button primaryBtn = new Button("主要按钮", Button.PRIMARY, false);

// 成功按钮
Button successBtn = new Button("成功按钮", Button.SUCCESS, false);

// 警告按钮
Button warningBtn = new Button("警告按钮", Button.WARNING, false);

// 危险按钮
Button dangerBtn = new Button("危险按钮", Button.DANGER, false);

// 信息按钮
Button infoBtn = new Button("信息按钮", Button.INFO, false);
```

## 朴素按钮

朴素风格的按钮。

![朴素按钮](../screenshots/button-plain.png)

```java
// 朴素主要按钮
Button plainPrimary = new Button("朴素 主要", Button.PRIMARY, true);

// 朴素成功按钮
Button plainSuccess = new Button("朴素 成功", Button.SUCCESS, true);

// 朴素警告按钮
Button plainWarning = new Button("朴素 警告", Button.WARNING, true);

// 朴素危险按钮
Button plainDanger = new Button("朴素 危险", Button.DANGER, true);

// 朴素信息按钮
Button plainInfo = new Button("朴素 信息", Button.INFO, true);
```

## 禁用状态

禁用状态的按钮。

![禁用状态](../screenshots/button-disabled.png)

```java
// 禁用主要按钮
Button disabledPrimary = new Button("禁用-主要", Button.PRIMARY, false);
disabledPrimary.setEnabled(false);

// 禁用朴素按钮
Button disabledPlain = new Button("禁用-朴素", Button.PRIMARY, true);
disabledPlain.setEnabled(false);

// 禁用默认按钮
Button disabledDefault = new Button("禁用-默认", Button.DEFAULT, false);
disabledDefault.setEnabled(false);
```

## Button 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| text | 按钮文字 | String | — | — |
| type | 按钮类型 | int | DEFAULT / PRIMARY / SUCCESS / WARNING / DANGER / INFO | DEFAULT |
| plain | 是否朴素按钮 | boolean | — | false |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/button.md
git commit -m "docs: 添加 Button 组件文档"
```

---

## Task 3: Input 输入框文档

**Files:**
- Create: `docs/components/input.md`

**Interfaces:**
- Produces: Input 组件使用文档

- [ ] **Step 1: 创建 input.md**

```markdown
# Input 输入框

输入框组件，支持占位符和可清空功能。

## 基本用法

基础的输入框用法。

![基本用法](../screenshots/input-default.png)

```java
import org.swelement.ui.AstInput;

// 带占位符的输入框
Input input = new Input("请输入内容");
```

## 可清空

输入框右侧显示清空按钮。

![可清空](../screenshots/input-clearable.png)

```java
// 输入内容后，鼠标悬停显示清空按钮
Input clearableInput = new Input("请输入内容");
```

## 禁用状态

禁用状态的输入框。

![禁用状态](../screenshots/input-disabled.png)

```java
// 禁用输入框
Input disabledInput = new Input("请输入内容");
disabledInput.setEnabled(false);
```

## Input 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| placeholder | 占位符文本 | String | — | — |

## Input 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| getText | 获取输入框文本 | — | String |
| setText | 设置输入框文本 | String t | void |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/input.md
git commit -m "docs: 添加 Input 组件文档"
```

---

## Task 4: Checkbox 多选框文档

**Files:**
- Create: `docs/components/checkbox.md`

**Interfaces:**
- Produces: Checkbox 组件使用文档

- [ ] **Step 1: 创建 checkbox.md**

```markdown
# Checkbox 多选框

一组备选项中进行多选。

## 基本用法

基础的多选框用法。

![基本用法](../screenshots/checkbox-default.png)

```java
import org.swelement.ui.AstCheckbox;

// 创建多选框
Checkbox checkbox1 = new Checkbox("选项A");
Checkbox checkbox2 = new Checkbox("选项B");
Checkbox checkbox3 = new Checkbox("选项C");
```

## 禁用状态

禁用状态的多选框。

![禁用状态](../screenshots/checkbox-disabled.png)

```java
// 禁用多选框
Checkbox disabledCheckbox = new Checkbox("禁用选项");
disabledCheckbox.setEnabled(false);
```

## Checkbox 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| text | 多选框文字 | String | — | — |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/checkbox.md
git commit -m "docs: 添加 Checkbox 组件文档"
```

---

## Task 5: Radio 单选框文档

**Files:**
- Create: `docs/components/radio.md`

**Interfaces:**
- Produces: Radio 组件使用文档

- [ ] **Step 1: 创建 radio.md**

```markdown
# Radio 单选框

一组备选项中进行单选。

## 基本用法

基础的单选框用法。

![基本用法](../screenshots/radio-default.png)

```java
import org.swelement.ui.AstRadio;

// 创建单选框
Radio radio1 = new Radio("选项A");
Radio radio2 = new Radio("选项B");
Radio radio3 = new Radio("选项C");
```

## 禁用状态

禁用状态的单选框。

![禁用状态](../screenshots/radio-disabled.png)

```java
// 禁用单选框
Radio disabledRadio = new Radio("禁用选项");
disabledRadio.setEnabled(false);
```

## Radio 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| text | 单选框文字 | String | — | — |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/radio.md
git commit -m "docs: 添加 Radio 组件文档"
```

---

## Task 6: Switch 开关文档

**Files:**
- Create: `docs/components/switch.md`

**Interfaces:**
- Produces: Switch 组件使用文档

- [ ] **Step 1: 创建 switch.md**

```markdown
# Switch 开关

表示两种相互对立的状态间的切换。

## 基本用法

基础的开关用法。

![基本用法](../screenshots/switch-default.png)

```java
import org.swelement.ui.AstSwitch;

// 创建开关
Switch switch1 = new Switch();
```

## 禁用状态

禁用状态的开关。

![禁用状态](../screenshots/switch-disabled.png)

```java
// 禁用开关
Switch disabledSwitch = new Switch();
disabledSwitch.setEnabled(false);
```

## Switch 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| — | — | — | — | — |

## Switch 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| isSelected | 获取开关状态 | — | boolean |
| setSelected | 设置开关状态 | boolean b | void |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/switch.md
git commit -m "docs: 添加 Switch 组件文档"
```

---

## Task 7: Slider 滑块文档

**Files:**
- Create: `docs/components/slider.md`

**Interfaces:**
- Produces: Slider 组件使用文档

- [ ] **Step 1: 创建 slider.md**

```markdown
# Slider 滑块

通过拖动选择一个数值范围。

## 基本用法

基础的滑块用法。

![基本用法](../screenshots/slider-default.png)

```java
import org.swelement.ui.AstSlider;

// 创建滑块（最小值0，最大值100，初始值50）
Slider slider = new Slider(0, 100, 50);
```

## 禁用状态

禁用状态的滑块。

![禁用状态](../screenshots/slider-disabled.png)

```java
// 禁用滑块
Slider disabledSlider = new Slider(0, 100, 50);
disabledSlider.setEnabled(false);
```

## Slider 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| min | 最小值 | int | — | — |
| max | 最大值 | int | — | — |
| value | 初始值 | int | — | — |

## Slider 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| getValue | 获取当前值 | — | int |
| setValue | 设置当前值 | int v | void |
| addChangeListener | 添加值变化监听器 | ChangeListener l | void |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/slider.md
git commit -m "docs: 添加 Slider 组件文档"
```

---

## Task 8: Select 选择器文档

**Files:**
- Create: `docs/components/select.md`

**Interfaces:**
- Produces: Select 组件使用文档

- [ ] **Step 1: 创建 select.md**

```markdown
# Select 选择器

当选项过多时，使用下拉菜单展示并选择内容。

## 基本用法

基础的单选下拉框。

![基本用法](../screenshots/select-default.png)

```java
import org.swelement.ui.AstSelect;

// 创建单选下拉框
Select select = new Select(new String[]{"黄金糕", "双皮奶", "蚵仔煎", "龙须面"});

// 监听选择变化
select.addChangeListener(e -> {
    Object value = select.getSelectedValue();
    System.out.println("选中: " + value);
});
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
| getSelected | 获取所有选中项 | — | List<Option> |
| clearSelection | 清空选中 | — | void |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/select.md
git commit -m "docs: 添加 Select 组件文档"
```

---

## Task 9: Tabs 标签页文档

**Files:**
- Create: `docs/components/tabs.md`

**Interfaces:**
- Produces: Tabs 组件使用文档

- [ ] **Step 1: 创建 tabs.md**

```markdown
# Tabs 标签页

分隔内容上有关联但属于不同类别的数据集合。

## 基本用法

基础的标签页用法。

![基本用法](../screenshots/tabs-default.png)

```java
import org.swelement.ui.AstTabs;

// 创建标签页
Tabs tabs = new Tabs(new String[]{"用户管理", "配置管理", "角色管理", "定时任务补偿"}, 0);

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
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/tabs.md
git commit -m "docs: 添加 Tabs 组件文档"
```

---

## Task 10: Pagination 分页文档

**Files:**
- Create: `docs/components/pagination.md`

**Interfaces:**
- Produces: Pagination 组件使用文档

- [ ] **Step 1: 创建 pagination.md**

```markdown
# Pagination 分页

当数据量过多时，使用分页分解数据。

## 基本用法

基础的分页用法。

![基本用法](../screenshots/pagination-default.png)

```java
import org.swelement.ui.AstPagination;

// 创建分页组件（总数据量100，每页10条，初始第1页）
Pagination pagination = new Pagination(100, 10, 1);

// 监听页码变化
pagination.addPageChangeListener(page -> {
    System.out.println("当前页: " + page);
});
```

## 自定义每页数量

修改每页显示的数据量。

![自定义每页数量](../screenshots/pagination-pagesize.png)

```java
// 创建分页组件（总数据量200，每页20条）
Pagination pagination = new Pagination(200, 20, 1);

// 动态修改每页数量
pagination.setPageSize(50);
```

## Pagination 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| totalCount | 总数据量 | int | — | 0 |
| pageSize | 每页数量 | int | — | 10 |
| initialPage | 初始页码 | int | — | 1 |

## Pagination 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| getTotalCount | 获取总数据量 | — | int |
| setTotal | 设置总数据量 | int t | void |
| getPageSize | 获取每页数量 | — | int |
| setPageSize | 设置每页数量 | int s | void |
| getCurrentPage | 获取当前页码 | — | int |
| setCurrentPage | 设置当前页码 | int v | void |
| getTotalPages | 获取总页数 | — | int |
| addPageChangeListener | 添加页码变化监听器 | IntConsumer l | void |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/pagination.md
git commit -m "docs: 添加 Pagination 组件文档"
```

---

## Task 11: Menu 导航菜单文档

**Files:**
- Create: `docs/components/menu.md`

**Interfaces:**
- Produces: Menu 组件使用文档

- [ ] **Step 1: 创建 menu.md**

```markdown
# Menu 导航菜单

为网站提供导航功能的菜单。

## 基本用法

基础的垂直导航菜单。

![基本用法](../screenshots/menu-default.png)

```java
import org.swelement.ui.AstMenu;

// 创建导航菜单
Menu menu = new Menu();
```

## Menu 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| — | — | — | — | — |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/menu.md
git commit -m "docs: 添加 Menu 组件文档"
```

---

## Task 12: Tag 标记文档

**Files:**
- Create: `docs/components/tag.md`

**Interfaces:**
- Produces: Tag 组件使用文档

- [ ] **Step 1: 创建 tag.md**

```markdown
# Tag 标记

用于标记和选择。

## 基本用法

不同类型的标记。

![基本用法](../screenshots/tag-default.png)

```java
import org.swelement.ui.AstTag;

// 主要标记
Tag primaryTag = new Tag("标签一", Tag.PRIMARY, false);

// 成功标记
Tag successTag = new Tag("标签二", Tag.SUCCESS, false);

// 警告标记
Tag warningTag = new Tag("标签三", Tag.WARNING, false);

// 危险标记
Tag dangerTag = new Tag("标签四", Tag.DANGER, false);

// 信息标记
Tag infoTag = new Tag("标签五", Tag.INFO, false);
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
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/tag.md
git commit -m "docs: 添加 Tag 组件文档"
```

---

## Task 13: Progress 进度条文档

**Files:**
- Create: `docs/components/progress.md`

**Interfaces:**
- Produces: Progress 组件使用文档

- [ ] **Step 1: 创建 progress.md**

```markdown
# Progress 进度条

用于展示操作进度，告知用户当前状态和预期。

## 基本用法

线性进度条。

![基本用法](../screenshots/progress-default.png)

```java
import org.swelement.ui.AstProgress;

// 创建进度条
Progress progress = new Progress(50);

// 动态更新进度
progress.setValue(75);
```

## 显示文字

进度条右侧显示百分比文字。

![显示文字](../screenshots/progress-text.png)

```java
// 创建显示文字的进度条
Progress progressWithText = new Progress(60);
progressWithText.setShowText(true);
```

## Progress 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| initialValue | 初始进度值 | int | 0-100 | 0 |
| showText | 是否显示文字 | boolean | — | true |

## Progress 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| getValue | 获取当前进度 | — | int |
| setValue | 设置当前进度 | int v | void |
| setShowText | 设置是否显示文字 | boolean b | void |
| addChangeListener | 添加进度变化监听器 | ChangeListener l | void |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/progress.md
git commit -m "docs: 添加 Progress 组件文档"
```

---

## Task 14: Badge 角标文档

**Files:**
- Create: `docs/components/badge.md`

**Interfaces:**
- Produces: Badge 组件使用文档

- [ ] **Step 1: 创建 badge.md**

```markdown
# Badge 角标

按钮和图标上的状态标识。

## 基本用法

数字角标。

![基本用法](../screenshots/badge-default.png)

```java
import org.swelement.ui.AstBadge;

// 创建角标
Badge badge = new Badge();
badge.setContent(new JButton("消息"));
badge.setCount(12);
```

## 点状角标

只显示一个小红点，不显示具体数字。

![点状角标](../screenshots/badge-dot.png)

```java
// 创建点状角标
Badge dotBadge = new Badge();
dotBadge.setContent(new JButton("消息"));
dotBadge.setDot(true);
```

## Badge 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| count | 角标数字 | int | — | 0 |
| dot | 是否显示点状角标 | boolean | — | false |

## Badge 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| setContent | 设置角标内容组件 | JComponent c | void |
| setCount | 设置角标数字 | int c | void |
| setDot | 设置是否显示点状角标 | boolean b | void |
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/badge.md
git commit -m "docs: 添加 Badge 组件文档"
```

---

## Task 15: Alert 提示文档

**Files:**
- Create: `docs/components/alert.md`

**Interfaces:**
- Produces: Alert 组件使用文档

- [ ] **Step 1: 创建 alert.md**

```markdown
# Alert 提示

用于展示重要的提示信息。

## 基本用法

4 种类型的提示。

![基本用法](../screenshots/alert-default.png)

```java
import org.swelement.ui.AstAlert;

// 成功提示
Alert successAlert = new Alert(Alert.SUCCESS, "成功提示", null, false);

// 警告提示
Alert warningAlert = new Alert(Alert.WARNING, "警告提示", null, false);

// 信息提示
Alert infoAlert = new Alert(Alert.INFO, "信息提示", null, false);

// 错误提示
Alert errorAlert = new Alert(Alert.ERROR, "错误提示", null, false);
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
```

- [ ] **Step 2: 提交**

```bash
git add docs/components/alert.md
git commit -m "docs: 添加 Alert 组件文档"
```

---

## Task 16: 更新 README 文档索引

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: 所有组件文档

- [ ] **Step 1: 在 README.md 中添加文档索引**

在 README.md 末尾添加：

```markdown
## 组件文档

| 组件 | 文档 |
|------|------|
| Button 按钮 | [文档](docs/components/button.md) |
| Input 输入框 | [文档](docs/components/input.md) |
| Checkbox 多选框 | [文档](docs/components/checkbox.md) |
| Radio 单选框 | [文档](docs/components/radio.md) |
| Switch 开关 | [文档](docs/components/switch.md) |
| Slider 滑块 | [文档](docs/components/slider.md) |
| Select 选择器 | [文档](docs/components/select.md) |
| Tabs 标签页 | [文档](docs/components/tabs.md) |
| Pagination 分页 | [文档](docs/components/pagination.md) |
| Menu 导航菜单 | [文档](docs/components/menu.md) |
| Tag 标记 | [文档](docs/components/tag.md) |
| Progress 进度条 | [文档](docs/components/progress.md) |
| Badge 角标 | [文档](docs/components/badge.md) |
| Alert 提示 | [文档](docs/components/alert.md) |
```

- [ ] **Step 2: 提交**

```bash
git add README.md
git commit -m "docs: 添加组件文档索引"
```
