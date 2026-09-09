# 全组件使用文档设计

日期：2026-08-28
状态：已批准

## 目标

为 swing-element-ui 的全部 50 个组件编写完整的使用文档，仿照 Element UI 官网中文文档风格，合并为单一大文件。

## 文档格式

- 格式：Markdown 单文件 `docs/components.md`
- 语言：中文
- 风格：仿 Element UI 官网中文文档

## 文档大纲

```
# swing-element-ui 组件文档

## 安装与快速开始
  - 环境要求（JDK 8+）
  - 构建方式（build.bat）
  - 快速上手示例

## 基础组件
  Button 按钮 | Layout 布局 | Icon 图标 | CloseButton 关闭按钮

## 表单组件
  Input 输入框 | InputNumber 数字输入框 | Select 选择器 | Cascader 级联选择 |
  Switch 开关 | Checkbox 多选框 | Radio 单选框 | Rate 评分 |
  Slider 滑块 | TextArea 多行输入 | Form 表单

## 数据展示
  Table 表格 | Tag 标记 | Progress 进度条 | Tree 树形控件 |
  Pagination 分页 | Badge 角标 | Avatar 头像 | Calendar 日历 |
  Carousel 走马灯 | Collapse 折叠面板 | Timeline 时间线 | Card 卡片

## 导航组件
  NavMenu 导航菜单 | Tabs 标签页 | Breadcrumb 面包屑 | Dropdown 下拉菜单 |
  Steps 步骤条

## 反馈组件
  Alert 提示 | Dialog 对话框 | Tooltip 文字提示 | Popover 弹出框 |
  Message 消息 | MessageBox 弹框 | Notification 通知 | Loading 加载 |
  Drawer 抽屉 | Empty 空状态

## 布局容器
  Container 容器 | Divider 分割线
```

## 每个组件的标准格式

```markdown
## ComponentName 中文名

一句话简介。

### 基本用法

说明文字。

\```java
// 代码示例
\```

### 不同变体（如适用）

#### 变体1名称
说明 + 代码示例

### ComponentName 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|

### ComponentName 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|

### ComponentName 事件

| 事件名 | 说明 | 回调参数 |
|--------|------|----------|
```

## 组件清单（50 个）

### 基础组件（4 个）
- AstButton: 按钮
- AstContainer: 布局容器
- AstIcon: 图标
- AstCloseButton: 关闭按钮

### 表单组件（11 个）
- AstInput: 输入框
- AstInputNumber: 数字输入框
- AstSelect: 选择器
- AstCascader: 级联选择
- AstSwitch: 开关
- AstCheckbox: 多选框
- AstRadio: 单选框
- AstRate: 评分
- AstSlider: 滑块
- AstTextArea: 多行输入
- AstForm: 表单

### 数据展示组件（12 个）
- AstTable: 表格
- AstTag: 标记
- AstProgress: 进度条
- AstTree: 树形控件
- AstPagination: 分页
- AstBadge: 角标
- AstAvatar: 头像
- AstCalendar: 日历
- AstCarousel: 走马灯
- AstCollapse: 折叠面板
- AstTimeline: 时间线
- AstCard: 卡片

### 导航组件（5 个）
- AstMenu: 导航菜单
- AstTabs: 标签页
- AstBreadcrumb: 面包屑
- AstDropdown: 下拉菜单
- AstSteps: 步骤条

### 反馈组件（10 个）
- AstAlert: 提示
- AstDialog: 对话框
- AstTooltip: 文字提示
- AstPopover: 弹出框
- AstMessage: 消息
- AstMessageBox: 弹框
- AstNotification: 通知
- AstLoading: 加载
- AstDrawer: 抽屉
- AstEmpty: 空状态

### 布局组件（2 个）
- AstContainer: 布局容器
- AstDivider: 分割线

### 其他组件（6 个）
- AstDatePicker: 日期选择
- AstTimePicker: 时间选择
- AstTransfer: 穿梭框
- AstTableColumn: 表格列定义
- AstTableModel: 表格数据模型
- FormValueProvider / FormInvalidMarker: 表单接口

## 数据来源

- 已有独立文档的组件：从 `docs/components/*.md` 合并
- 无独立文档的组件：从源码 `src/org/swelement/ui/Ast*.java` 提取 API
- Demo 文件：`src/org/swelement/demo/*.java` 提供使用示例

## 验证

- 所有代码示例可编译运行
- 属性/方法/事件表格与源码一致
- 文档结构与 Element UI 官网风格一致
