# swing-element-ui 设计文档

日期：2026-08-19
状态：已批准（待实现）

## 目标

在 Java Swing（JDK 8）中复刻 Element UI 组件库的外观与交互动画。组件 UI 变化（hover、focus、选中、值变化、显隐）均使用动画过渡。

## 范围与阶段

Element UI 有 60+ 组件，按阶段分批交付，每阶段独立可运行、可验收：

- **Phase 1（本设计文档覆盖）**：动画引擎 + 主题 + 基础表单组件
  Button / Input / Checkbox / Radio / Switch / Slider
- **Phase 2（后续）**：Select / Table / Tree / Pagination / Tabs / Menu 等
- **Phase 3（后续）**：Dialog / Tooltip / Notification / DatePicker 等

## 技术约束

- JDK 8，编译参数 `--release 8`
- 构建：纯 `javac`，不引入 Maven/Gradle
- 动画：自研微型引擎（`javax.swing.Timer` + 缓动函数），零外部依赖
- 全部组件自绘（继承 JComponent/JPanel，覆盖 `paintComponent`），与 L&F 无关

## 目录结构

```
swing-element-ui/
  src/org/swelement/
    core/
      Animator.java        动画引擎
      Easing.java          缓动函数
      ElementTheme.java    Element UI 色板常量 + 插值工具
    ui/
      Button.java  Input.java  Checkbox.java  Radio.java  Switch.java  Slider.java
    demo/
      ButtonDemo.java  InputDemo.java  CheckboxDemo.java
      RadioDemo.java  SwitchDemo.java  SliderDemo.java
  build.bat             Windows 一键编译
  build.sh              （可选）Linux/macOS 编译脚本
```

## 核心引擎

### Animator
- 封装 `javax.swing.Timer`，帧间隔 15ms（约 60fps）
- 字段：`from`、`to`、`duration`（默认 200ms）、`Easing`、进度回调 `onUpdate(float p)`
- 支持：中途反向（反转 start/end）、重复、完成后自动停止
- 单个组件内多个 Animator 实例分别驱动不同状态（hover/focus/active）

### Easing
- 枚举或静态方法：`linear`、`easeIn`、`easeOut`、`easeInOut`（三次缓动），几行数学

### ElementTheme
- Element UI 色板常量：
  - 主色 `#409EFF`，success `#67C23A`，warning `#E6A23C`，danger `#F56C6C`，info `#909399`
  - 文字：`#303133` / `#606266` / `#909399` / `#C0C4CC`
  - 边框：`#DCDFE6` / `#E4E7ED` / `#EBEEF5` / `#F2F6FC`
  - 背景：`#FFFFFF` / `#F5F7FA`
  - 圆角：4px；字体：Microsoft YaHei（回退 Dialog）
- 插值工具：`lerpColor`（RGB 插值）、`lerpInt`、`lerpFloat`

## 组件通用模式

每个交互组件持有 `hover / focus / active` 三个 0→1 的进度状态（float 字段），由 Animator 驱动，`paintComponent` 按进度插值颜色、位移、透明度、描边宽度。鼠标事件监听器负责启停动画。

## Phase 1 组件与动画行为

| 组件 | 动画行为 |
|------|----------|
| Button | hover 背景/文字色渐变；按下回弹；disabled 变灰；支持 type（default/primary/success/warning/danger/info）、plain、圆角 |
| Input | focus 边框色过渡 + 光晕；可清空时 × 按钮淡入；占位符；disabled |
| Checkbox / Radio | 打勾动画（勾号描边渐进）、背景填充渐变、边框过渡 |
| Switch | knob 滑动 + 底色渐变（开/关） |
| Slider | 已选轨道填充比例动画、thumb 拖拽、hover 时 thumb 放大过渡 |

## 验证

- 每个组件配一个 `*Demo` 类（`main` 方法起 JFrame 展示）
- 非平凡逻辑（Easing/插值）内置一个 assert 自检 `main`，断言缓动单调性、插值正确性
- 编译后用 `java -cp out org.swelement.demo.ButtonDemo` 等逐个目视验收

## 错误处理与可访问性

- 组件自绘不依赖外部资源；disabled 状态全程生效，不触发交互
- 保留 Swing 默认焦点遍历；焦点可视（focus 动画即视觉反馈）
- 无 I/O、无外部依赖，错误处理面小

## 明确省略（后续阶段补充）

- 表单校验、Loading、组件间的组合联动（如 Select 下拉弹层）
- 复杂组件（Table/Tree/DatePicker/Dialog 等）到 Phase 2/3 再做