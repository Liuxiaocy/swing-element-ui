# swing-element-ui

Element UI 风格的 Java Swing 组件库（JDK 8，零依赖）。

## 构建

```
.\build.bat        # 编译到 out/ 并跑全部组件自检
.\run-checks.bat   # 仅跑全部组件自检（含文档-代码一致性校验）
```

## 运行 Demo

```
java -cp out org.swelement.demo.ButtonDemo
java -cp out org.swelement.demo.InputDemo
java -cp out org.swelement.demo.CheckboxDemo
java -cp out org.swelement.demo.RadioDemo
java -cp out org.swelement.demo.SwitchDemo
java -cp out org.swelement.demo.SliderDemo
java -cp out org.swelement.demo.SelectDemo
java -cp out org.swelement.demo.TabsDemo
java -cp out org.swelement.demo.AstTabsDemo
java -cp out org.swelement.demo.AstStepsDemo
java -cp out org.swelement.demo.AstTimelineDemo
java -cp out org.swelement.demo.AstRadioDemo
java -cp out org.swelement.demo.AstCheckboxDemo
java -cp out org.swelement.demo.PaginationDemo
java -cp out org.swelement.demo.AstMenuDemo
java -cp out org.swelement.demo.TagDemo
java -cp out org.swelement.demo.ProgressDemo
java -cp out org.swelement.demo.BadgeDemo
java -cp out org.swelement.demo.AlertDemo
java -cp out org.swelement.demo.AstEmptyDemo
java -cp out org.swelement.demo.AstNotificationDemo
java -cp out org.swelement.demo.AstAvatarDemo
java -cp out org.swelement.demo.AstCardDemo
java -cp out org.swelement.demo.AstContainerDemo
java -cp out org.swelement.demo.AstFormDemo
java -cp out org.swelement.demo.AstIconDemo
java -cp out org.swelement.demo.AstLoadingDemo
java -cp out org.swelement.demo.AstPopupDemo
java -cp out org.swelement.demo.AstTableDemo
java -cp out org.swelement.demo.AstAdvancedDemo
java -cp out org.swelement.demo.FrameworkDemo
```

## 核心自检

```
java -ea -cp out org.swelement.core.Easing
java -ea -cp out org.swelement.core.ElementTheme
java -ea -cp out org.swelement.core.AnimationManager
java -ea -cp out org.swelement.core.theme.ThemeManager
java -ea -cp out org.swelement.ui.AstSelect
java -ea -cp out org.swelement.ui.AstPagination
```

## 设计

见 `docs/superpowers/specs/2026-08-19-swing-element-ui-design.md`

## 组件总览

> 文档列为组件说明页（`docs/components/` 下），Demo 列为可运行的演示类。
> 未列出的组件已在 `AstAdvancedDemo` / `FrameworkDemo` 中展示。

| 组件 | 文档 | Demo |
|------|------|------|
| Alert 提示 | [文档](docs/components/alert.md) | AlertDemo |
| Avatar 头像 | — | AstAvatarDemo |
| Badge 角标 | [文档](docs/components/badge.md) | BadgeDemo |
| Button 按钮 | [文档](docs/components/button.md) | ButtonDemo |
| Calendar 日历 | — | — |
| Card 卡片 | — | AstCardDemo |
| Carousel 走马灯 | — | — |
| Cascader 级联选择 | — | — |
| Checkbox 多选框 | [文档](docs/components/checkbox.md) | AstCheckboxDemo |
| CloseButton 关闭按钮 | — | — |
| Collapse 折叠面板 | — | — |
| Container 布局容器 | — | AstContainerDemo |
| DatePicker 日期选择 | — | — |
| Dialog 对话框 | — | — |
| Divider 分割线 | — | — |
| Drawer 抽屉 | — | — |
| Dropdown 下拉菜单 | — | — |
| Form 表单 | — | AstFormDemo |
| Icon 图标 | — | AstIconDemo |
| Input 输入框 | [文档](docs/components/input.md) | InputDemo |
| InputNumber 数字输入 | — | — |
| Loading 加载 | — | AstLoadingDemo |
| Menu 导航菜单 | [文档](docs/components/menu.md) | AstMenuDemo |
| Message 消息 | — | — |
| MessageBox 弹框 | — | — |
| Pagination 分页 | [文档](docs/components/pagination.md) | PaginationDemo |
| Popover 弹出框 | — | AstPopupDemo |
| Progress 进度条 | [文档](docs/components/progress.md) | ProgressDemo |
| Radio 单选框 | [文档](docs/components/radio.md) | AstRadioDemo |
| Rate 评分 | — | — |
| Select 选择器 | [文档](docs/components/select.md) | SelectDemo |
| Slider 滑块 | [文档](docs/components/slider.md) | SliderDemo |
| Steps 步骤条 | [文档](docs/components/steps.md) | AstStepsDemo |
| Switch 开关 | [文档](docs/components/switch.md) | SwitchDemo |
| Table 表格 | — | AstTableDemo |
| Tabs 标签页 | [文档](docs/components/tabs.md) | AstTabsDemo |
| Tag 标记 | [文档](docs/components/tag.md) | TagDemo |
| TextArea 多行输入 | — | — |
| TimePicker 时间选择 | — | — |
| Timeline 时间线 | [文档](docs/components/timeline.md) | AstTimelineDemo |
| Tooltip 文字提示 | — | AstPopupDemo |
| Transfer 穿梭框 | — | — |
| Tree 树形控件 | — | — |

## 文档-代码一致性

`run-checks.bat` 内含 `DocSnippetCheck`：抽取文档中的 ```java 代码块并对照 `out/` 真正编译，
校验 markdown 链接、命令引用真实存在，从机制上防止文档与代码再次漂移。
截图由 `tools/DocScreenshotGen.java` 离屏渲染生成，主题更新后重跑即可刷新。
