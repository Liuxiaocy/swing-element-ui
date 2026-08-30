# Phase 5 设计：反馈/弹出类组件框架化迁移

日期：2026-08-30
分类：Migration（5 个组件）
前置：Phase 4（导航类组件迁移，已完成）

## 总览

将 5 个反馈/弹出类组件迁移到框架体系。

### 组件清单

| 组件 | 类型 | 当前基类 | Animator | 内部类 | 复杂度 |
|------|------|---------|----------|--------|--------|
| AstLoading | JComponent | JComponent | fadeAnim + spinTimer | — | ★★☆ |
| AstTooltip | 静态工具类 | — (无基类) | 无 | balloon (匿名 JPanel) | ★★☆ |
| AstPopover | JComponent | JComponent | 无 (AnimatedPopup 处理) | CardPanel, TitleBar | ★★☆ |
| AstMessage | 静态工具类 | — (无基类) | inAnim (ToastCard) | ToastCard (静态) | ★★☆ |
| AstDialog | 静态工具类 | — (无基类) | fade (DialogCardPanel) | DialogCardPanel (公开静态) | ★★★ |

### 迁移策略

两种模式：

**模式 A：组件类（AstLoading、AstPopover）**
- `extends JComponent` → `extends AstAbstractComponent`
- Animator → anim.register() / anim.go() / anim.getProgress()
- ElementTheme → theme().getXxx()
- selfCheck 从静态改为实例

**模式 B：静态工具类（AstTooltip、AstMessage、AstDialog）**
- 外层类保持不变（静态工具类，不是组件）
- 内部 JPanel 子类 → extends AstAbstractComponent（获得 theme()/anim/lerp/assertContrast）
- ElementTheme → theme().getXxx()
- selfCheck 保持静态（工具类的自检不需要实例化）
- 内部类的 anim 用于替换手动 Animator

### onComplete 回调处理

多个组件使用 `Animator.go(from, to, onComplete)` 模式。迁移方案：
通过 `anim.get(name)` 获取底层 Animator，使用其 `go(from, to, onComplete)` 方法。
与 Phase 3 的 AstInput/AstSelect 一致。

---

## 各组件详细设计

### AstLoading

- `extends JComponent` → `extends AstAbstractComponent`
- `fadeAnim` → `anim.register("fade", 220, Easing::easeOut)`
- `overlay` 变量 → `anim.getProgress("fade")`
- onComplete：beginShow 中的 target.setVisible(false) 和 hideLoading 中的 setVisible(false)
- `spinTimer` 保留（是 Swing Timer，不是 Animator）
- ElementTheme.FONT/RADIUS/BORDER_BASE/PRIMARY/TEXT_REGULAR → theme().getXxx()
- ElementTheme.assertContrast → assertContrast
- selfCheck → 实例方法

### AstTooltip

- 外层保持静态工具类
- `balloon` 匿名 JPanel → 匿名 AstAbstractComponent
- ElementTheme.TEXT_MAIN/BORDER_BASE/RADIUS/FONT → theme().getXxx()
- ElementTheme.assertContrast → assertContrast
- selfCheck 保持静态

### AstPopover

- `extends JComponent` → `extends AstAbstractComponent`
- CardPanel (静态, extends JPanel → extends AstAbstractComponent)
- TitleBar (静态, extends JPanel → extends AstAbstractComponent)
- ElementTheme.RADIUS/BORDER_BASE/TEXT_MAIN/FONT → theme().getXxx()
- ElementTheme.assertContrast → assertContrast
- selfCheck → 实例方法

### AstMessage

- 外层保持静态工具类
- ToastCard (静态, extends JPanel → extends AstAbstractComponent)
- `inAnim` → `anim.register("in", 220, Easing::easeOut)`
- `progress` → `anim.getProgress("in")`
- ElementTheme.FONT/BORDER_BASE/RADIUS/PRIMARY/SUCCESS/WARNING/DANGER/TEXT_MAIN/TEXT_REGULAR → theme().getXxx()
- ElementTheme.assertContrast/pickTextColorForBg → assertContrast/直接调用
- selfCheck 保持静态

### AstDialog

- 外层保持静态工具类
- DialogCardPanel (公开静态, extends JPanel → extends AstAbstractComponent)
- `fade` → `anim.register("fade", 220, Easing::easeOut)`
- `cardAlpha` → `anim.getProgress("fade")`
- onComplete：startFadeOut 中的 after callback
- 匿名 JPanel 子类 (titleBar, footer) 中的 ElementTheme → DialogCardPanel.this.theme().getXxx()
- ElementTheme.assertContrast → DialogCardPanel.this.assertContrast()
- selfCheck 保持静态

---

## 验证策略

- JDK 8 编译通过
- selfCheck 全部通过
- run-checks.bat 从 24 → 29 项
