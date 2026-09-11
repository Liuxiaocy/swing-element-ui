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
- [x] P2.1 `AstInput`：① 输入建议（激活即列 / 输入后匹配，基于 `AnimatedPopup`）② 最大长度限制 + 尾部字数统计 ③ 复合型（前缀/后缀元素：标签或按钮）；`selfCheck`（过滤/选择/弹层显示+点击/最大长度截断/删除不被拦截/前后缀布局）；`docs/components/input.md` 已更新覆盖新 API。
- [x] P2.2 `AstSelect`：基础多选用 `AstTag` 展示已选项 —— 原手绘 `JLabel`（`"label  ×"` 文本拼 ×）换成真正的 `AstTag`（INFO + light，可关闭）。点按分层靠 AWT 事件重定向天然实现，无额外命中测试：点 × → 落在 `AstCloseButton`（自带监听器）→ 只移除该项不展开下拉；点标签主体 → AstTag 自身无鼠标监听器，事件上溯 `tagsPanel` → 展开下拉。档位联动 `{DEFAULT, SMALL, SMALL}`、`setEnabled` 联动标签禁用。顺带修 `AstTag` 缺陷：挂载前被禁用的标签，`addNotify()` 才创建的 × 漏掉禁用态（禁用多选会出现可点的 ×）。`selfCheck` 新增 6 项（标签数/文案顺序/可关闭结构/档位高度递减/禁用联动/点 × 后 formValue 与剩余标签数）；`SelectDemo` 新增「多选」面板（含禁用多选）；`docs/components/select.md` 补标签交互表 + 方法表补 `setSize`/`setEnabled`/`getFormValue`/`setFormValue`。
- [x] P2.3 `AstTimePicker`：固定时间范围（选开始时间后，结束时间备选项按范围禁用/置灰）—— `setRangeMode(true)` 区间模式；弹层左右并排起始/结束两块面板，结束侧按整时间戳严格比较级联置灰 ≤ 开始的备选项且不可点选；`setTimeRange`/`getTimeRange` 取值；禁用项静音灰渲染（WCAG 豁免不做 AA 断言）。`docs/components/timepicker.md` 随本项补完（覆盖 P5.2 timepicker 文档缺口）。selfCheck 新增区间 round-trip / 顺序异常 / 弹层 6 JList+2 TimePanel / 结束侧禁用项不可选断言；单模式与区间弹层块均补 `finally` dispose 兜底。
- [x] P2.4 `AstDatePicker`：日期范围选择（起止两个面板/联动高亮）—— `setRangeMode(true)` 区间模式；弹层左右并排两块日历（左=起始月，右=起始月+1）各自独立翻月；点击-点击选取（end<start 自动交换、再点开启新一轮）；联动高亮三档（端点实心 PRIMARY 白字跳过 AA / 区间内部 `lerp(WHITE,PRIMARY,0.10)` 底纹做 AA / 今日仅描边）；start 选定后 hover 预览 `[start,hover]`；`setDateRange`/`getDateRange`（非区间抛 `IllegalStateException`，null 抛 `IllegalArgumentException`）；`getFormValue` 区间形如 `start~end`；禁用/其他月静音灰豁免对比度。selfCheck 新增区间 round-trip / 自动交换 / form value / 点击-点击+第三轮重置 / `inRange`·`isEndpoint` 行为断言 / 弹层含 2 个 CalendarPanel / 左面板离屏绘制；单模式与区间弹层均补 `finally` dispose 兜底。`docs/components/datepicker.md` 随本项补完（覆盖 P5.2 datepicker 文档缺口）。

## 阶段 3 — 导航 / 步骤 / 时间线增强
- [x] P3.1 `AstMenu`：侧栏竖向菜单模式 —— `setMode(int)`（`MODE_HORIZONTAL`/`MODE_VERTICAL` 常量）+ `getMode`/`isVertical`/`setSidebarWidth`（默认 200）；布局改为模式感知（水平左→右底条、竖向上→下左竖条指示，entryExtent 按朝向返回宽/行高）；鼠标命中 x/y 分支统一走 `entryIndexAt`；子菜单水平向下、竖向向**右**弹出；方向键 ↑/↓（竖向）/←/→（水平）移动 active 并 clamp 不回绕；`selfCheck` 新增竖向 preferredSize(200,n*40)、离屏绘制、行命中、方向键 clamp、`setMode`/`setSidebarWidth` 非法值断言。新增 `AstMenuDemo`（水平+竖向展示 + `--selfcheck`），接入 `build.bat`/`run-checks.bat` 与 README Demo 列表。menu.md 补竖向示例与属性/方法表。
- [x] P3.2 `AstTabs`：侧栏竖向标签模式 —— `setMode(int)`（`MODE_HORIZONTAL`/`MODE_VERTICAL` 常量）+ `getMode`/`isVertical`/`setSidebarWidth`（默认 200，`≤0` 抛 `IllegalArgumentException`）；内容区 inset 模式感知（水平 `EmptyBorder(HEADER_H,0,0,0)`、竖向 `EmptyBorder(0,sidebarWidth,0,0)`，竖向内容移到右侧）；命中测试统一走 `tabIndexAt(px,py)`（水平按 x 遍历 y≤HEADER_H、竖向按行 `py/ROW_H` 且 x≤sidebarWidth）；指示条竖向走新增的 `indY/indH` 通道（左侧 2px PRIMARY 竖条），水平保持 `indX/indW` 底部横条；`getPreferredSize` 竖向 `(sidebarWidth+280, max(n*ROW_H,200))`；方向键 ↑/↓（竖向）/←/→（水平）移动 selected 并 clamp 不回绕（走 `moveActive`，`setFocusable(true)` 无焦点环）。`selfCheck` 新增竖向 preferredSize、行命中/越界、方向键 clamp、可聚焦、非法参数断言；反向验证（`tabIndexAt` 竖向分支改 `return -1`）得到真实 `AssertionError: vertical row1 hit, got -1`，还原后 exit=0。新增 `AstTabsDemo`（水平+竖向展示 + `--selfcheck`；修掉「构造器建空面板 + 再 addTab」导致标签翻倍的坑），接入 `build.bat`/`run-checks.bat`/README。tabs.md 补竖向示例与 `mode`/`sidebarWidth` 属性、`setMode`/`getMode`/`isVertical`/`setSidebarWidth` 方法。
- [x] P3.3 `AstSteps`：横向步骤条 / 含状态步骤条 / 带图标步骤条 / 简洁风格步骤条 —— 新增 `Status{WAIT,PROCESS,FINISH,ERROR,SUCCESS}` 枚举与 `setStepStatus(idx,s)`/`getStepStatus`（传 null 清空）；`resolveStatus(i)` 显式 status **覆盖** `current` 推导（未设则 `i<current→FINISH`、`i==current→PROCESS`、`i>current→WAIT`）；`setStepIcon(idx, AstIcon.Type)` 用 `AstIcon.paintIcon` 画图标替代序号/对勾（图标优先）；`setSimple(boolean)` 简洁风格：节点退化为彩色小圆点（PROCESS 略大）、连线 1px 细线、不画数字/对勾/图标，横竖双向生效且 `getPreferredSize` 变紧凑（横向 24 vs 52）。渲染重构为状态驱动（`paintFullNode`/`paintSimpleDot`），ERROR 用 `theme().getDanger()` 填充+白叉，连线是否"已完成"改用 `resolveStatus(i)` 判定以尊重 status 覆盖。`selfCheck` 新增：五态推导与覆盖、清空回落、越界抛异常、图标读写、simple 高度更小、五态×横竖离屏绘制 + 图标 + 简洁风格绘制；反向验证（让 `resolveStatus` 忽略显式 status）得真实 `AssertionError: explicit ERROR must override derived FINISH`，还原后 exit=0。新增 `AstStepsDemo`（基础/含状态/带图标/简洁四组 + `--selfcheck`），接入 `build.bat`/`run-checks.bat`（60 项）/README；新建 `docs/components/steps.md`（覆盖 P5.2 steps 文档缺口）。
- [x] P3.4 `AstTimeline`：带图标的时间线（`Item` 支持 `AstIcon.Type`）—— `Item` 新增 `public final AstIcon.Type icon` 字段 + `getIcon()`，新增两个带图标构造器 `Item(ts,title,type,icon)` 与 `Item(ts,title,desc,type,icon)`；原 3/4 参构造器内部传 `icon=null`，**不破坏任何现有调用**（字段保持 final 不可变）。渲染：设了图标的节点放大到 `ICON_NODE_D=20`（类型色圆底 + 内部 16px 白色图标，复用 `AstIcon.paintIcon` + `g2.create()/translate`），未设图标仍为 12px 实心圆点（混合列表按项各自判定）。白色图标属「白字彩底实心态」，按惯例不做 AA 断言。自检新增：四种构造器 icon 读写与默认值、**绘制级**像素断言（距节点中心水平 8px 处——在 12px 圆点外、20px 图标圆内，带图标项应为类型色不透明、无图标项不应被类型色填充）；反向验证（节点渲染忽略 icon）得真实 `AssertionError: icon node opaque, alpha=0`，还原后 exit=0。新增 `AstTimelineDemo`（基础 + 混合带图标两组 + `--selfcheck`），接入 `build.bat`/`run-checks.bat`（61 项）/README；新建 `docs/components/timeline.md`（覆盖 P5.2 timeline 文档缺口）。

## 阶段 4 — 表单控件增强
- [x] P4.1 `AstRadio` 按钮样式（`setButtonStyle(true)`）
- [x] P4.2 `AstCheckbox` 按钮样式（`setButtonStyle(true)`）

## 阶段 5 — 补齐 Demo 与文档
- [x] P5.1 补齐 Demo（共 8 个，已全部完成；Menu/Tabs/Input/Select/Radio/Checkbox/Tag 已有 Demo 不重复）
  - [x] `AstEmptyDemo`（已存在，build.bat 接入）
  - [x] `AstNotificationDemo`（已存在，build.bat 接入）
  - [x] `StepsDemo`（已存在，run-checks.bat 接入）
  - [x] `TimelineDemo`（已存在，run-checks.bat 接入）
  - [x] `BreadcrumbDemo`（本批新建 + breadcrumb.md + 接入 build/run-checks/README；run-checks 现 61 步）
  - [x] `CollapseDemo`（本批新建 + collapse.md + 接入 build/run-checks/README；run-checks 现 62 步）
  - [x] `TimePickerDemo`（本批新建；timepicker.md 已存在 + 接入 build/run-checks/README；run-checks 现 63 步）
  - [x] `DatePickerDemo`（本批新建；datepicker.md 已存在 + 接入 build/run-checks/README；run-checks 现 64 步）
- [x] P5.2 补文档页（语句型示例必须可编译，无截图则不写图片链接）
  - [x] `empty` `notification` `steps` `timeline` `timepicker` `datepicker`（各阶段已建）
  - [x] `breadcrumb`（本批新建）
  - [x] `collapse`（本批新建）
- [x] P5.3 全部条目接入 `build.bat` + `run-checks.bat`；`README.md` Demo 列表同步
  - [x] `BreadcrumbDemo` 已接入 build/run-checks/README
  - [x] `CollapseDemo` 已接入 build/run-checks/README
  - [x] `TimePickerDemo` 已接入 build/run-checks/README
  - [x] `DatePickerDemo` 已接入 build/run-checks/README
  - [x] `AstEmptyDemo`/`AstNotificationDemo`/`AstIconDemo`/`AstTableDemo` 已补入 run-checks.bat（原仅 build.bat）；run-checks 现 68 步（67 自检 + 文档一致性）

## 阶段 6 — 全量验证 ✅ 完成（2026-09-11）
- [x] 全量 self-check（数量随新增自增）+ `DocSnippetCheck` 全绿；`out/` 编译零错误。
  - run-checks 共 **68 步**（67 自检 + 文档一致性）**全部 PASS**；全量 JDK 8 编译 RC=0；`DocSnippetCheck OK（28 篇文档）`。
  - 沙箱无法跑 `run-checks.bat`（禁 `cmd.exe`、无 `timeout`）→ 新增 `tools/run-checks.py` 等价运行器
    （解析 bat 逐条执行、每条 120s 超时兜底，支持 `--limit N` 冒烟）。

## 备注
- 真实缺口：新组件 `AstEmpty`/`AstNotification`；缺 Demo 的 8 个；缺文档页的 8 个（见 P5.2）。
- `AstBreadcrumb`/`AstTabs`/`AstSteps`/`AstCollapse`/`AstTimeline`/`AstInput`/`AstSelect`/`AstTimePicker`/`AstDatePicker`/`AstMenu`/`AstRadio`/`AstCheckbox` 均**已存在且已有 selfCheck**，本次是增强。
- `AstMessage` 已存在（Toast 式通知），`AstNotification` 在其上封装更丰富的 API（位置/手动关闭/多实例堆叠）。
- 缺陷修复（自检挂死）：`AnimatedPopup.hideWithAnimation` 的 185ms 隐藏定时器只持局部引用、无法取消，且 `hidePopup()` 只停 `openAnim` 不停 `closeAnim`；运行中的 Swing Timer 会让 AWT 事件线程（非 daemon）一直存活 → 自检 JVM 不退出，批量自检偶发 `rc=124` 超时挂死（`AstTooltip` 首次暴露）。修法：新增 `hideTimer` 字段统一管理，`hidePopup()` 里统一 stop 掉 `hideTimer`/`openAnim`/`closeAnim`（顺带修掉「先 `hideWithAnimation` 再 `show` 时，上轮残留定时器把新弹层摘掉」这个既有 bug）；`AstTooltip.selfCheck` 收尾补 `sharedPopup.setVisible(false)` 并断言弹层已摘除。反向验证（去掉收尾调用）得到真实的断言失败 + 挂死（exit=124），还原后 3/3 通过，全量 59 项全绿。
- P4.1 `AstRadio` 按钮样式：新增 `setButtonStyle(boolean)` / `isButtonStyle()` + `fill` 动画通道；按钮态渲染为直角 1px 边框分段按钮（选中=主色填充+白字实心态，未选中=浅底+常规文字，禁用=静音灰）；`getPreferredSize` 按钮态 `height=32 / width=文字宽+40`。`selfCheck` 新增按钮态尺寸 + **绘制级像素断言**（离屏绘制选中态、强制 `fill=1`、采样背景中心须为 `PRIMARY` 不透明；反向验证屏蔽 primary 填充得真实 `AssertionError: button selected bg must be PRIMARY, got ffffff`，还原 exit=0）。新增 `AstRadioDemo`（普通/按钮样式/禁用三组 + `--selfcheck`，Group 互斥组合）；**注意 Demo 在另一包无法访问组件 `protected anim`，其 selfCheck 只做 API/尺寸/Group/绘制不抛错，绘制级像素断言留在组件 selfCheck**。接入 `build.bat`/`run-checks.bat`（59→60 步，含 DocSnippetCheck 校验 26 篇文档）、README Demo 列表、radio.md 补「按钮样式」小节 + 属性/方法表。全量 60 步全绿。
- P4.2 `AstCheckbox` 按钮样式：复用 P4.1 的 Bounded 设计（选中=主色填充+白字实心态、直角 1px 边框分段按钮、禁用=静音灰、按钮态 `height=32 / width=文字宽+40`）；`fill` 通道在 `AstCheckbox` 已存在，直接驱动。按钮态**不画勾选框**（与 Radio 一致，纯文字分段按钮）。`selfCheck` 新增按钮态 API/尺寸 + **绘制级像素断言**（强制 `fill=1`、采样背景中心须为 `PRIMARY` 不透明；反向验证屏蔽 primary 填充得真实 `AssertionError: checkbox button selected bg must be PRIMARY, got ffffff`，还原 exit=0）+ 未选中态 AA（白字彩底按惯例豁免）。新增 `AstCheckboxDemo`（普通/按钮样式/禁用三组 + `--selfcheck`，校验**多选**——X 与 Y 可同时选中）；Demo selfCheck 同样只做 API/尺寸/多选/绘制不抛错（绘制级像素断言留在组件 selfCheck）。接入 `build.bat`/`run-checks.bat`（含 `AstCheckboxDemo`）、README Demo 列表改指 `AstCheckboxDemo`、checkbox.md 补「按钮样式」小节（强调多选、不画勾选框）+ 属性/方法表。全量 60 步全绿、DocSnippetCheck 通过。
