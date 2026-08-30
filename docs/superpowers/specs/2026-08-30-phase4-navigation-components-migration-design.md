# Phase 4 设计：导航类组件框架化迁移

日期：2026-08-30
分类：Migration（5 个组件，轻量迁移策略）
前置：Phase 3（输入类组件迁移，已完成）

## 总览

将 5 个导航类组件从"手动 ElementTheme + 手动 Animator + JComponent/JPanel 基类"迁移到 AstAbstractComponent 框架体系。

### 组件清单

| 组件 | 当前基类 | 迁移后基类 | Animator 数 | 内部类 | 复杂度 |
|------|---------|-----------|------------|--------|--------|
| AstSteps | JComponent | AstAbstractComponent | 0 | — | ★☆☆ |
| AstBreadcrumb | JComponent | AstAbstractComponent | 1 (hover) | — | ★★☆ |
| AstTabs | JComponent | AstAbstractComponent | 3 (indX/indW/content) | — | ★★☆ |
| AstPagination | JComponent | AstAbstractComponent | 1 (hover) | PageButton | ★★☆ |
| AstCollapse | JPanel | AstAbstractComponent | 2 (open/arrow) | CollapseItem | ★★★ |

### 迁移策略

延续 Phase 3 的轻量迁移模式：保持组件结构不变，仅替换基类、颜色系统、动画系统和绘制工具。

### 特殊注意

- **AstSteps** 无 Animator，只需颜色替换——最简单
- **AstTabs** 无 selfCheck，需新增
- **AstCollapse** 的 CollapseItem 是静态内部类，需要通过 `parent.theme()` 访问主题
- **AstPagination** 的 PageButton 可迁移到 AstInteractiveComponent 复用 hover 能力

---

## 各组件详细设计

### AstSteps（最简单，作为试点）

**当前结构**：
- extends JComponent
- 无 Animator（状态切换无过渡动画）
- paintComponent 绘制圆形节点 + 连接线 + 标签
- 使用 ElementTheme.SUCCESS/PRIMARY/BORDER_BASE/TEXT_MAIN/TEXT_REGULAR/TEXT_PLACEHOLDER/FONT
- ElementTheme.assertContrast

**迁移改动**：
1. `extends JComponent` → `extends AstAbstractComponent`
2. 删除 `setOpaque(false)`
3. 所有 `ElementTheme.xxx` → `theme().getXxx()`
4. `ElementTheme.assertContrast(...)` → `assertContrast(...)`
5. `paintComponent` 中 `(Graphics2D) g.create()` + `setRenderingHint(...)` → `createGraphics(g)`
6. `selfCheck` 从静态改为实例方法
7. `getPreferredSize` 中 `ElementTheme.FONT` → `theme().getFontBase()`

### AstBreadcrumb

**当前结构**：
- extends JComponent
- 1 个 hoverAnim（Animator 150ms）
- hoverAlpha 变量跟踪进度
- mouseMoved 确定鼠标在哪个 item 上
- paintComponent 绘制文字 + 分隔符 + hover 下划线
- getPreferredSize 和 itemIndexAt 也使用 ElementTheme.FONT

**迁移改动**：
1. `extends JComponent` → `extends AstAbstractComponent`
2. 删除 `hoverAnim` 字段和 `hoverAlpha` 变量
3. 添加 `initComponent()` 注册 "hover" 动画
4. `hoverAnim.go(...)` → `anim.go("hover", ...)`
5. 所有 `ElementTheme.xxx` → `theme().getXxx()`
6. `ElementTheme.lerp(...)` → `lerp(...)`
7. `paintComponent` 使用 `createGraphics(g)`
8. `getPreferredSize` 和 `itemIndexAt` 中 `ElementTheme.FONT` → `theme().getFontBase()`
9. `selfCheck` 迁移为实例方法

### AstTabs

**当前结构**：
- extends JComponent
- 3 个 Animator：indXAnim (250ms), indWAnim (250ms), contentAnim (200ms)
- indX/indW/contentAlpha 变量
- CardLayout 切换内容面板
- paintComponent 绘制标签文字 + 底部指示条
- **无 selfCheck** — 需新增

**迁移改动**：
1. `extends JComponent` → `extends AstAbstractComponent`
2. 删除 3 个 Animator 和 3 个 float 变量
3. 添加 `initComponent()` 注册 "indX", "indW", "content" 动画
4. `indXAnim.go(...)` → `anim.go("indX", ...)`
5. `contentAnim.go(0f, 1f)` → `anim.go("content", anim.getProgress("content"), 1f)`（注意：contentAnim 从 0 开始，需要特殊处理）
6. `paintComponent` 中使用 `anim.getProgress("indX")` 等
7. `ElementTheme.FONT` → `theme().getFontBase()`
8. `ElementTheme.PRIMARY` → `theme().getPrimary()`
9. 新增 `selfCheck()` 方法
10. cardPanel 的 paintComponent 中使用 `anim.getProgress("content")`

### AstPagination

**当前结构**：
- extends JComponent
- 内部类 PageButton extends JLabel，有自己的 hoverAnim
- PageButton.paintComponent 绘制背景 + 文字
- 三档尺寸

**迁移改动**：
1. `extends JComponent` → `extends AstAbstractComponent`
2. 外部类：`ElementTheme.FONT` → `theme().getFontBase()`
3. PageButton 内部类：
   - `extends JLabel` → `extends AstInteractiveComponent`（复用 hover 动画）
   - 删除 `hoverAnim` 和 `hover` 变量
   - 删除 mouseEntered/mouseExited 的 hover 驱动（基类处理）
   - `paintComponent` 使用 `createGraphics(g)` + `hoverProgress()` + `theme().getXxx()`
   - `ElementTheme.lerp(...)` → `lerp(...)`
4. `selfCheck` 迁移为实例方法

### AstCollapse

**当前结构**：
- extends JPanel
- 内部类 CollapseItem extends JPanel（静态嵌套类）
- CollapseItem 有 openAnim (220ms) 和 arrowAnim (220ms)
- CollapseItem 内有 3 个匿名 JPanel 子类（header, arrowPanel, contentWrap），各自重写 paintComponent
- 手风琴模式

**迁移改动**：
1. `extends JPanel` → `extends AstAbstractComponent`
2. 外部类：删除 `setOpaque(false)`
3. CollapseItem：
   - `extends JPanel` → `extends AstAbstractComponent`
   - 删除 `openAnim`, `arrowAnim` 字段和 `openProgress`, `arrowRot` 变量
   - 添加 `initComponent()` 注册 "open" 和 "arrow" 动画
   - `openAnim.go(...)` → `anim.go("open", ...)`
   - `arrowAnim.go(...)` → `anim.go("arrow", ...)`
4. CollapseItem 内的匿名 JPanel 子类：
   - header 的 paintComponent：`ElementTheme.BORDER_BASE` → `AstCollapse.this.theme().getBorderBase()`
     （等等，CollapseItem 是静态类，不能直接访问 AstCollapse.this。但它有 `parent` 字段引用 AstCollapse。所以用 `parent.theme().getBorderBase()`）
   - arrowPanel 的 paintComponent：`ElementTheme.TEXT_REGULAR` → `parent.theme().getTextRegular()`
   - contentWrap 的 paintComponent：`ElementTheme.FILL_BASE` → `parent.theme().getFillBase()`
   - `ElementTheme.FONT` → `parent.theme().getFontBase()`
   - `ElementTheme.TEXT_MAIN` → `parent.theme().getTextPrimary()`
   - `ElementTheme.assertContrast(...)` → `parent.assertContrast(...)`
5. `selfCheck` 迁移为实例方法

---

## 验证策略

每个组件迁移后：
1. JDK 8 编译通过
2. selfCheck() 断言全部通过
3. 对比度断言通过（WCAG AA）
4. 添加到 run-checks.bat（19 → 24 项）

## 涉及文件

**修改**：
- `src/org/swelement/ui/AstSteps.java`
- `src/org/swelement/ui/AstBreadcrumb.java`
- `src/org/swelement/ui/AstTabs.java`
- `src/org/swelement/ui/AstPagination.java`
- `src/org/swelement/ui/AstCollapse.java`
- `run-checks.bat`（添加 5 个组件的自检）
