# swing-element-ui 组件文档设计

日期：2026-08-21
状态：已批准

## 目标

为 swing-element-ui 的全部 14 个已实现组件编写使用文档，仿照 Element UI 官网风格，包含基本用法、不同样式截图、属性信息。

## 文档格式

- 格式：Markdown 文件
- 语言：中文
- 结构：每个组件独立一个 `.md` 文件

## 目录结构

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
  screenshots/
    button-default.png
    button-types.png
    ...
```

## 组件清单

| 组件 | 文件名 | 类名 | 截图前缀 |
|------|--------|------|----------|
| Button | button.md | Button | button- |
| Input | input.md | Input | input- |
| Checkbox | checkbox.md | Checkbox | checkbox- |
| Radio | radio.md | Radio | radio- |
| Switch | switch.md | Switch | switch- |
| Slider | slider.md | Slider | slider- |
| Select | select.md | Select | select- |
| Tabs | tabs.md | Tabs | tabs- |
| Pagination | pagination.md | Pagination | pagination- |
| Menu | menu.md | Menu | menu- |
| Tag | tag.md | Tag | tag- |
| Progress | progress.md | Progress | progress- |
| Badge | badge.md | Badge | badge- |
| Alert | alert.md | Alert | alert- |

## 每个组件文档结构

```markdown
# {组件名} {中文名}

{一句话描述}

## 基本用法

{描述}

![基本用法](../screenshots/{前缀}default.png)

```java
{代码示例}
```

## {样式2名称}

{描述}

![{样式2名称}](../screenshots/{前缀}{样式2}.png)

```java
{代码示例}
```

## {组件名} 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| {param} | {说明} | {type} | {可选值} | {默认值} |

## {组件名} 事件

| 事件名 | 说明 | 回调参数 |
|--------|------|----------|
| {event} | {说明} | {参数} |
```

## 属性表格五列说明

1. **参数**：属性名称
2. **说明**：属性用途描述
3. **类型**：Java 类型（String / int / boolean / float 等）
4. **可选值**：枚举值或范围（用 / 分隔，如 `DEFAULT / PRIMARY / SUCCESS`）
5. **默认值**：默认值（用 — 表示必填）

## 截图规范

- 目录：`docs/screenshots/`
- 命名：`{组件名}-{样式}.png`
- 尺寸：建议 800x600 或自适应
- 来源：运行对应 Demo 类后手动截取

## 各组件文档内容规划

### Button 按钮
- 基本用法：6 种类型按钮
- 朴素按钮：plain 模式
- 禁用状态：disabled
- 属性：text, type, plain

### Input 输入框
- 基本用法：placeholder
- 可清空：clearable
- 禁用状态
- 属性：placeholder, text

### Checkbox 多选框
- 基本用法：单个多选框
- 禁用状态
- 属性：text

### Radio 单选框
- 基本用法：单个单选框
- 禁用状态
- 属性：text

### Switch 开关
- 基本用法：开/关切换
- 禁用状态
- 属性：—

### Slider 滑块
- 基本用法：拖动选择值
- 禁用状态
- 属性：min, max, value

### Select 选择器
- 基本用法：单选下拉
- 多选模式：multiple
- 可搜索：filterable
- 分组：group
- 属性：multiple, filterable

### Tabs 标签页
- 基本用法：切换标签
- 属性：titles, initialIndex

### Pagination 分页
- 基本用法：分页导航
- 属性：total, pageSize, currentPage

### Menu 导航菜单
- 基本用法：垂直菜单
- 属性：—

### Tag 标记
- 基本用法：5 种类型
- 可关闭标签
- 属性：text, type, closable

### Progress 进度条
- 基本用法：线性进度条
- 显示文字
- 属性：value, showText

### Badge 角标
- 基本用法：数字角标
- 点状角标
- 属性：count, dot

### Alert 提示
- 基本用法：4 种类型
- 可关闭提示
- 带描述信息
- 属性：type, title, desc, closable

## 验证

- 每个文档包含完整代码示例
- 属性表格五列完整
- 截图路径正确引用
- 代码示例可直接运行
