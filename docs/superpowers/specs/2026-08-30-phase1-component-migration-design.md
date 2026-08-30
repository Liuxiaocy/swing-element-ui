# Phase 1: 简单展示组件迁移设计

> 将 6 个简单展示/容器组件从旧的 ElementTheme + 手动 Animator 模式迁移到新框架基类体系

## 1. 背景与目标

### 1.1 现状

Phase 0 已完成框架基础设施建设，包括：

- **Theme 系统**：`Theme` 接口、`ThemeManager`、`ElementLightTheme`（支持运行时切换主题）
- **动画管理器**：`AnimationManager`（命名动画注册/驱动/获取进度）
- **自检框架**：`SelfCheckBase`（对比度断言等工具）
- **组件基类**：`AstAbstractComponent`、`AstInteractiveComponent`、`AstContainerComponent`、`AstDisplayComponent`
- **绘制工具**：`PaintingHelper`（圆角、发光、文字、颜色工具集）

现有 46 个 UI 组件仍使用旧模式：
- 继承 `JComponent` / `JButton`
- 通过 `ElementTheme` 静态常量获取颜色（运行时不可切换）
- 手动创建 `Animator` 实例管理动画（大量重复样板代码）
- 自检使用 `ElementTheme.assertContrast()`（部分组件缺失自检）

### 1.2 目标

将 6 个简单组件迁移到新框架体系，验证迁移模式，为后续 Phase 2-7 提供参考。

**迁移后预期收益**：
- 每组件减少约 30% 样板代码（动画声明、鼠标监听、Graphics2D 配置、颜色插值工具）
- 迁移后组件支持运行时主题切换
- 自检覆盖更完善

### 1.3 约束

- 保持零外部依赖，纯 JDK 8
- 允许破坏性 API 变更（不需要保持旧 public 方法签名）
- 保留 `ElementTheme` 旧类供未迁移组件使用，新旧共存
- 迁移后组件的 `selfCheck()` 使用 `SelfCheckBase` 工具，不再依赖 `ElementTheme.assertContrast()`

## 2. 迁移范围

| # | 组件 | 文件大小 | 新基类 | 分类理由 |
|---|------|---------|--------|---------|
| 1 | AstProgress | 2.6KB | AstDisplayComponent | 纯展示进度条，无交互 |
| 2 | AstDivider | 8.6KB | AstDisplayComponent | 分割线，无交互 |
| 3 | AstTag | 11.1KB | AstDisplayComponent | 标签，纯展示 |
| 4 | AstBadge | 15.8KB | AstDisplayComponent | 角标标记，纯展示 |
| 5 | AstAvatar | 9.2KB | AstDisplayComponent | 头像，无标准交互 |
| 6 | AstAlert | 9.9KB | AstContainerComponent | 容器型，含 close 按钮 |

AstAlert 使用 AstContainerComponent 而非 AstInteractiveComponent 的理由：AstAlert 的关闭交互通过内部子组件（close button）实现，Alert 本身不需要 hover/active/focus 标准动画。

## 3. 标准迁移改动清单

每个组件需完成以下 5 项改动：

### 3.1 继承链替换

- `extends JComponent` → `extends AstDisplayComponent`（或 `AstContainerComponent`）
- 删除不再需要的 import
- 删除旧的手动样式设置（如 `setOpaque(false)`、手动 RenderingHints 配置）

### 3.2 主题系统迁移

| 旧调用 | 新调用 |
|--------|--------|
| `ElementTheme.PRIMARY` | `theme().getPrimary()` |
| `ElementTheme.SUCCESS` | `theme().getSuccess()` |
| `ElementTheme.WARNING` | `theme().getWarning()` |
| `ElementTheme.DANGER` | `theme().getDanger()` |
| `ElementTheme.INFO` | `theme().getInfo()` |
| `ElementTheme.TEXT_MAIN` | `theme().getTextPrimary()` |
| `ElementTheme.TEXT_REGULAR` | `theme().getTextRegular()` |
| `ElementTheme.TEXT_SECONDARY` | `theme().getTextSecondary()` |
| `ElementTheme.TEXT_PLACEHOLDER` | `theme().getTextPlaceholder()` |
| `ElementTheme.TEXT_DISABLED` | `theme().getTextDisabled()` |
| `ElementTheme.BORDER_BASE` | `theme().getBorderBase()` |
| `ElementTheme.BORDER_LIGHT` | `theme().getBorderLight()` |
| `ElementTheme.FILL_BLANK` | `theme().getFillBlank()` |
| `ElementTheme.FILL_BASE` | `theme().getFillBase()` |
| `ElementTheme.FILL_LIGHT` | `theme().getFillLight()` |
| `ElementTheme.FONT` | `theme().getFontBase()` |
| `ElementTheme.FONT_SMALL` | `theme().getFontSmall()` |
| `ElementTheme.FONT_LARGE` | `theme().getFontLarge()` |
| `ElementTheme.RADIUS` | `theme().getRadiusBase()` |
| `ElementTheme.RADIUS_SMALL` | `theme().getRadiusSmall()` |
| `ElementTheme.RADIUS_LARGE` | `theme().getRadiusLarge()` |
| `ElementTheme.lerp(a, b, t)` | `lerp(a, b, t)`（基类自带方法） |

### 3.3 动画系统迁移

| 旧模式 | 新模式 |
|--------|--------|
| `new Animator(duration, easing, callback)` | `anim.register(name, duration, easing)` |
| `hoverAnim.go(from, to)` | `anim.go(name, from, to)` |
| `hoverAnim.stop()` | `anim.stop(name)` |
| 手动浮点变量 + 回调 | `anim.getProgress(name)` 直接获取 |
| 手动鼠标监听安装 | 基类自动安装（仅 AstInteractiveComponent） |

### 3.4 绘制辅助迁移

| 旧模式 | 新模式 |
|--------|--------|
| `(Graphics2D) g` + 手动 RenderingHints | `createGraphics(g)` |
| 手动 `g2.fillRoundRect(...)` 或 `RoundRectangle2D` | `fillRoundRect(g2, x, y, w, h, r)` |
| 手动 `g2.drawRoundRect(...)` | `drawRoundRect(g2, x, y, w, h, r)` |

### 3.5 自检迁移

- `ElementTheme.assertContrast(fg, bg, where)` → `assertContrast(fg, bg, where)`（SelfCheckBase 提供）
- 静态 `public static void selfCheck()` → 实例方法 `protected void selfCheck()`（由基类调用）
- 补充缺失的自检（如 AstProgress 当前无 selfCheck）
- 自检中颜色从 `ElementTheme.XXX` 改为 `ThemeManager.getCurrent().getXXX()` 或 `theme()`

### 3.6 不迁移的部分

- 组件特定的业务逻辑（AstAlert 的关闭行为、AstBadge 的 sup 脚标定位等）
- 组件特有的自定义动画（如 AstProgress 的 fillAnim 保留为自定义命名动画 `"fill"`）
- 组件特有的尺寸计算逻辑

## 4. 迁移顺序

按复杂度从低到高，先验证模式再处理复杂组件：

| 批次 | 组件 | 预计改动量 | 理由 |
|------|------|-----------|------|
| Batch 1 | AstProgress | 最小 | 无自检需补充，仅 1 个动画，代码最短 |
| Batch 2 | AstDivider | 小 | 无动画无交互，纯绘制 |
| Batch 3 | AstTag | 中 | 有多状态颜色数组，自检完善 |
| Batch 4 | AstBadge | 中 | 有 sup 脚标定位逻辑，较复杂 |
| Batch 5 | AstAvatar | 中 | 有形状/尺寸/状态逻辑 |
| Batch 6 | AstAlert | 最大 | 容器型，含关闭交互、多类型、多状态 |

## 5. 验证策略

### 5.1 单组件验证

每个组件迁移后立即：
1. 编译（`javac --release 8`）
2. 运行 `selfCheck()`（`java -ea -cp out org.swelement.ui.AstXxx`）

### 5.2 全量验证

全部 6 个组件迁移完成后：
1. 运行 `run-checks.bat` 统一验证
2. 编译对应的 Demo 文件，确保引用兼容

### 5.3 子代理执行

- 按子代理驱动方式执行
- 每个批次派遣独立子 agent
- 每个子 agent 完成后返回编译和自检结果
- 两阶段审查：子 agent 自检 + 主 agent 验证

## 6. 迁移后预期效果

### 代码量变化

以 AstProgress 为例（当前最简单，2.6KB）：

| 改动类别 | 删除行数（估） | 新增行数（估） |
|----------|--------------|--------------|
| 手动 Graphics2D 配置 | 3-5 | 1（createGraphics） |
| 手动 Animator 声明 | 5-8 | 2（register + getProgress） |
| ElementTheme 引用 | 多处 | 多处（theme().xxx） |
| 自检补充 | 0 | 10-15（新增 selfCheck） |

净效果：删除样板代码多于新增的自检代码，总体行数持平或略减，但代码质量显著提升。

### 功能增强

- 迁移后组件支持运行时主题切换（ThemeManager.setCurrent()）
- 自检覆盖更完善
- 代码结构更清晰，减少手动管理
