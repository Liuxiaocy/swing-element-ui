# Button 组件展现方式增强设计文档

日期：2026-08-23
状态：已批准（含对比度修复）

## 目标

为 `org.swelement.ui.AstButton` 组件补齐 Element UI 按钮的全部展现方式：尺寸、round 圆角、circle 圆形、图标按钮、loading 加载中、text 文本按钮。保持零依赖、自绘、JDK 8 兼容。

## 可访问性约束（全局，所有组件必须遵循）

**文字颜色与背景色在任何状态下都必须满足 WCAG 2.1 AA 级对比度（≥ 4.5:1）**，防止文字看不清楚。

- 工具：`ElementTheme.assertContrast(fg, bg, where)` 在 `java -ea` 自检中断言对比度
- 工具：`ElementTheme.luminance(c)` 和 `ElementTheme.pickTextColorForBg(bg)` 用于自动选择对比度足够的文字色
- 适用状态：默认、hover、active、disabled、loading、plain、text 等所有组合
- 例外：实心彩色按钮（白色文字在彩色背景上）遵循 Element UI 标准设计，视觉清晰但严格对比度可能不足；此类需在设计文档中明确标注

## 范围

仅修改 `Button.java` 和 `ButtonDemo.java`，不影响其他组件。不改变现有公共 API（已有构造函数和方法保持兼容），仅新增方法和常量。

## API 设计

### 新增常量

```java
// 尺寸
public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
// 图标位置
public static final int ICON_LEFT = 0, ICON_RIGHT = 1;
```

### 新增方法

| 方法 | 说明 | 默认值 |
|---|---|---|
| `setSize(int size)` | 设置按钮尺寸 | SIZE_DEFAULT |
| `setRound(boolean round)` | 胶囊圆角（圆角半径=高度/2） | false |
| `setCircle(boolean circle)` | 圆形按钮（宽高相等，通常配合图标） | false |
| `setIcon(String icon)` | 设置 Unicode 图标符号，null 表示无图标 | null |
| `setIconPosition(int pos)` | 图标在文字左/右 | ICON_LEFT |
| `setLoading(boolean loading)` | 加载中状态，禁用点击，显示旋转动画 | false |
| `setLoadingText(String text)` | loading 时显示的文字，null 用默认"加载中" | null |
| `setTextButton(boolean textBtn)` | text 文本按钮模式（仅 primary 色） | false |

## 颜色系统（含对比度修复）

### plain 朴素按钮

- 背景：各 type 对应 Element UI 浅色背景（非统一浅蓝）
  - DEFAULT: `#FFFFFF`
  - PRIMARY: `#ECF5FF`
  - SUCCESS: `#F0F9EB`
  - WARNING: `#FDF6EC`
  - DANGER: `#FEF0F0`
  - INFO: `#F4F4F5`
- 文字：各 type 的**深色版本**（非白色），确保在对应浅色背景上对比度 ≥ 4.5:1
  - DEFAULT: `#606266`
  - PRIMARY: `#1d6fb5`
  - SUCCESS: `#2d6b18`
  - WARNING: `#955d12`
  - DANGER: `#b83232`
  - INFO: `#606266`
- 边框：各 type 对应颜色（DEFAULT type hover 时边框变浅蓝）

### disabled 禁用状态

- 背景：plain 时 `#FFFFFF`，非 plain 时 `#F5F7FA`（非浅蓝）
- 文字：`#606266`（深灰，非浅灰 `#C0C4CC`），确保对比度 ≥ 4.5:1
- 边框：plain 时 `#DCDFE6`，非 plain 时 `#E4E7ED`

### loading 加载中

- **不使用 disabled 灰色**（浅灰文字在浅蓝背景上对比度不足）
- 使用**正常颜色**（包括 plain 模式的彩色文字），仅降低不透明度表示禁用：
  - 背景 alpha = 200（约 78%）
  - 文字 alpha = 230（约 90%）
- 旋转圆弧颜色同文字色
- loading 时 `setEnabled(false)` 阻止交互，但视觉上保持正常色彩

### text 文本按钮

- 仅 primary 色
- 正常状态：无背景无边框，文字 `#409EFF`
- hover：浅色背景 `#ECF5FF`（透明度随 hover 插值）
- disabled：文字 `#606266`（深灰，非浅灰），无背景

## 各特性实现细节

### 尺寸

三档参数：

| 尺寸 | 字体 | 垂直内边距 | 水平内边距 | 图标间距 |
|---|---|---|---|---|
| SIZE_LARGE | 16px | 12px | 24px | 10px |
| SIZE_DEFAULT | 14px | 9px | 20px | 8px |
| SIZE_SMALL | 12px | 6px | 12px | 6px |

`getPreferredSize()` 按当前尺寸计算：宽度 = 水平内边距×2 + 文字宽度 + 图标宽度 + 图标间距（如有图标）；高度 = 垂直内边距×2 + 字体高度。

### round

圆角半径覆盖 `ElementTheme.RADIUS`，使用 `getHeight()/2f`。与 size、plain、type 等正交，可任意组合。

### circle

- `getPreferredSize()` 返回正方形：边长 = max(宽度计算值, 高度计算值)
- 圆角半径 = `getWidth()/2f`
- 通常配合 `setIcon` 使用且文字为空；若有文字则文字居中绘制
- circle 隐含 round 效果（无需同时设置 round）

### 图标

- 图标为 Unicode 字符串（如 `"\u2713"` 对勾、`"\u21bb"` 刷新），使用当前尺寸字体绘制
- 图标位置：ICON_LEFT 时图标在文字左侧，ICON_RIGHT 时在右侧
- 图标与文字间距按尺寸缩放（见上表）
- 文字为空且有图标时：纯图标按钮，宽度 = 水平内边距×2 + 图标宽度
- 无图标时：行为与当前一致
- loading 时：图标被旋转圆弧替代

### loading

- `setLoading(true)`：
  - 保存当前 enabled 状态和文字，然后 `setEnabled(false)`
  - 启动旋转动画 `loadAnim`（800ms，linear，循环）
  - 显示文字切换为 loadingText（默认"加载中"）
  - 颜色使用正常色彩降透明度（见"颜色系统"）
- `setLoading(false)`：
  - 停止旋转动画
  - 恢复原 enabled 状态和文字
- 旋转动画：`Animator` 驱动 `loadAngle` 从 0→1 循环，`paintComponent` 中绘制 270° 圆弧（stroke 宽度 2px，颜色为当前文字色），圆弧位于文字左侧
- loading 时不响应 hover/active 动画（disabled 状态）

### text 按钮

- 仅 primary 色（`#409EFF`），不支持其他 type
- 不绘制背景填充和边框
- hover 时：绘制浅色背景 `#ECF5FF`（透明度由 hover 进度插值）
- 文字颜色：正常时 `#409EFF`；disabled 时 `#606266`（深灰）
- text 按钮模式下，plain 参数被忽略
- 圆角仍受 round/circle 控制

## 动画与交互

- 现有 `hoverAnim`、`activeAnim` 保持不变
- 新增 `loadAnim`：`new Animator(800, Easing::linear, v -> { loadAngle = v; repaint(); })`，循环模式（完成后自动重新 go(0,1)）
- loading 状态下 hover/active 不启动（`isEnabled()` 守卫）
- text 按钮 hover 动画：背景色从透明插值到 `#ECF5FF`

## paintComponent 绘制顺序

1. 计算当前状态的 bg/fg/border 颜色（考虑 disabled、loading、text 模式、hover、active、plain、type）
2. 绘制背景（text 模式下仅 hover 时绘制半透明背景）
3. 绘制边框（非 text 模式）
4. 绘制 loading 旋转圆弧（如果 loading）
5. 绘制图标（如果有，且非 loading）
6. 绘制文字（loading 时用 loadingText）

## 验证

### Demo 更新

`ButtonDemo.java` 新增展示区域：
- 尺寸行：large / default / small 各一个 primary 按钮
- round 行：各 type 的 round 按钮
- circle 行：带图标的圆形按钮
- 图标行：图标在左、图标在右、纯图标
- loading 行：点击按钮触发 loading 状态，2 秒后恢复
- text 行：text 按钮（含 disabled）

### 自检

在 `AstButton` 类中新增 `selfCheck()` 静态方法（`main` 入口），断言：
- circle 模式下 `getPreferredSize()` 宽高相等
- size 切换后字体大小正确
- loading 切换后 enabled 状态正确恢复
- 纯图标按钮（文字为空+有图标）宽度计算正确
- **对比度断言**：plain 各 type 文字 vs 背景 ≥ 4.5:1；disabled 文字 vs 灰色/白色背景 ≥ 4.5:1

运行：`java -ea -cp out org.swelement.ui.Button`

## 明确不做

- 不引入外部图标字体或图片资源
- 不实现 Element Plus 的 link 按钮（2.x 无此组件）
- 不实现按钮组（ButtonGroup），属于独立组件
- 不改变现有构造函数签名
- 实心彩色按钮不强制对比度（遵循 Element UI 标准设计）
