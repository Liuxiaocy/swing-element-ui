# M2 组件建设路线图（progress ledger）

> 需求来源：2026-09-05 用户批量提出的新组件 + 现有组件增强 + 补齐 Demo/文档。
> 工作约定（沿用 P0 阶段）：每个组件 `extends` 合适基类、实现 `selfCheck()`、提供 `main` 入口；
> 增强/新增后必须 JDK 1.8 全量编译 + 自检通过；新组件/新 Demo 同步接入 `build.bat` 与 `run-checks.bat`；
> 新组件补 `docs/components/*.md`，并在 `README.md` 的 Demo 列表登记。
> 复用优先级：弹层 → `AnimatedPopup`；选中 chip → `AstTag`；通知 → 复用 `AstMessage` 的 `ToastCard` + `GlassPane`；图标 → `AstIcon.Type`。

## 阶段 1 — 全新组件（零现有依赖，最独立）✅ 完成（2026-09-05）
- [x] P1.1 `AstEmpty` 空状态：`AstDisplayComponent` 子类；支持自定义 Icon / AstIcon 类型 / 内置默认插图 + 描述 + 可选操作按钮（`setAction`）；`selfCheck`（离屏绘制 + 默认插图像素断言）+ `AstEmptyDemo --selfcheck` + `docs/components/empty.md`（含 3 张截图）。
- [x] P1.2 `AstNotification` 通知：复用 `AstMessage`/`AnimatedPopup`（TOOL 层，不响应外部点击）；四角位置（TOP/BOTTOM × RIGHT/LEFT）、四类型、标题+描述、× 手动关闭、默认 4.5s 自动关闭、同位置堆叠与补位；`preview()` 供截图；`selfCheck`（堆叠/离屏绘制）+ `AstNotificationDemo --selfcheck` + `docs/components/notification.md`（含 2 张截图）。
- 已接入 `build.bat`（2 项组件自检 + 2 项 demo --selfcheck）与 `run-checks.bat`（2 项组件自检，总数 51→53 组件、文档 52→54）；README Demo 列表已登记。

## 阶段 2 — 选择器类增强
- [ ] P2.1 `AstInput`：① 输入建议（激活即列 / 输入后匹配，基于 `AnimatedPopup`）② 最大长度限制 + 尾部字数统计 ③ 复合型（前缀/后缀元素：标签或按钮）
- [ ] P2.2 `AstSelect`：基础多选用 `AstTag` 展示已选项
- [ ] P2.3 `AstTimePicker`：固定时间范围（选开始时间后，结束时间备选项按范围禁用/置灰）
- [ ] P2.4 `AstDatePicker`：日期范围选择（起止两个面板/联动高亮）

## 阶段 3 — 导航 / 步骤 / 时间线增强
- [ ] P3.1 `AstMenu`：侧栏竖向菜单模式（`setMode(VERTICAL)` 或独立 `AstSideMenu`）
- [ ] P3.2 `AstTabs`：侧栏竖向标签模式
- [ ] P3.3 `AstSteps`：横向步骤条 / 含状态步骤条 / 带图标步骤条 / 简洁风格步骤条
- [ ] P3.4 `AstTimeline`：带图标的时间线（`Item` 支持 `AstIcon.Type`）

## 阶段 4 — 表单控件增强
- [ ] P4.1 `AstRadio` 按钮样式（`setButtonStyle(true)`）
- [ ] P4.2 `AstCheckbox` 按钮样式（`setButtonStyle(true)`）

## 阶段 5 — 补齐 Demo 与文档
- [ ] P5.1 新建 8 个 Demo：`AstEmptyDemo` `AstNotificationDemo` `BreadcrumbDemo` `StepsDemo` `CollapseDemo` `TimelineDemo` `TimePickerDemo` `DatePickerDemo`（Menu/Tabs/Input/Select/Radio/Checkbox/Tag 已有 Demo，不重复）
- [ ] P5.2 补文档页：`empty` `notification` `breadcrumb` `steps` `collapse` `timeline` `timepicker` `datepicker`（语句型示例必须可编译）
- [ ] P5.3 全部条目接入 `build.bat` + `run-checks.bat`；`README.md` Demo 列表同步

## 阶段 6 — 全量验证
- [ ] 全量 self-check（数量随新增自增）+ `DocSnippetCheck` 全绿；`out/` 编译零错误

## 备注
- 真实缺口：新组件 `AstEmpty`/`AstNotification`；缺 Demo 的 8 个；缺文档页的 8 个（见 P5.2）。
- `AstBreadcrumb`/`AstTabs`/`AstSteps`/`AstCollapse`/`AstTimeline`/`AstInput`/`AstSelect`/`AstTimePicker`/`AstDatePicker`/`AstMenu`/`AstRadio`/`AstCheckbox` 均**已存在且已有 selfCheck**，本次是增强。
- `AstMessage` 已存在（Toast 式通知），`AstNotification` 在其上封装更丰富的 API（位置/手动关闭/多实例堆叠）。
