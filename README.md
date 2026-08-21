# swing-element-ui

Element UI 风格的 Java Swing 组件库（JDK 8，零依赖）。

## 构建

```
.\build.bat        # 编译到 out/
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
java -cp out org.swelement.demo.PaginationDemo
java -cp out org.swelement.demo.MenuDemo
java -cp out org.swelement.demo.TagDemo
java -cp out org.swelement.demo.ProgressDemo
java -cp out org.swelement.demo.BadgeDemo
java -cp out org.swelement.demo.AlertDemo
```

## 核心自检

```
java -ea -cp out org.swelement.core.Easing
java -ea -cp out org.swelement.core.ElementTheme
java -ea -cp out org.swelement.core.Animator
java -ea -cp out org.swelement.ui.Select
java -ea -cp out org.swelement.ui.Pagination
```

## 设计

见 `docs/superpowers/specs/2026-08-19-swing-element-ui-design.md`

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