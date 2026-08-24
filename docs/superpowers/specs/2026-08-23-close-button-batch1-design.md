# 批次 1：CloseButton 公共组件 + 关闭逻辑重写 + Tag 展现增强设计文档

日期：2026-08-23
状态：待批准

## 目标

1. 新建公共可点击关闭按钮组件 `AstCloseButton`，替代所有「自绘 × + 点击坐标命中测试」的关闭实现
2. 重写四个组件的关闭逻辑：Tag、Alert、Input（清空）、AstDialog（新增 × 关闭）
3. Tag 补齐 Element UI 展现方式：effect（dark/light/plain）、尺寸档位
4. 三个无 selfCheck 的组件（Tag/Alert/Input）补齐自检，含 WCAG 对比度断言

保持零依赖、自绘、JDK 8 兼容。

## 可访问性约束（全局）

文字与 × 符号颜色在任何状态下必须满足 WCAG 2.1 AA（≥ 4.5:1），工具 `ElementTheme.assertContrast`。
例外：dark effect 实心彩色背景上的白色文字遵循 Element UI 标准设计（同实心彩色按钮），需在文档标注。

## 一、CloseButton 公共组件

新建 `src/org/swelement/ui/CloseButton.java`，继承 `JComponent`。

### API

| 方法 | 说明 | 默认值 |
|---|---|---|
| `CloseButton()` | 构造，默认 24×24 | |
| `CloseButton(int size)` | 构造，指定边长（正方形） | |
| `addActionListener(ActionListener l)` | 点击触发（消费后自动移除一次性回调不采用，支持多监听） | |
| `setColor(Color c)` | × 符号默认颜色 | `0x909399` |
| `setHoverColor(Color c)` | hover 时 × 符号颜色 | `0x409EFF` |
| `setAlpha(float a)` | 整体透明度 0~1，供父组件动画驱动 | 1f |
| `setInteractive(boolean b)` | false 时不响应点击（alpha 淡出期间） | true |

### 行为细节

- **绘制**：两条 45° 线段构成 ×（线宽 1.6f，长度随边长缩放，约为边长的 40%），抗锯齿
- **hover**：圆形背景 `rgba(0,0,0,0.06)` 淡入（150ms easeInOut，Animator），× 颜色从 `color` 插值到 `hoverColor`；离开淡出
- **点击**：MouseListener 的 `mouseClicked`（在按钮边界内即触发，无需坐标计算——Swings 事件分发天然保证）触发所有 `ActionListener`
- **alpha 联动**：`setAlpha(0)` 时调用方应同时 `setInteractive(false)`；`paintComponent` 按 alpha 绘制；`setInteractive(false)` 时 `contains(x,y)` 返回 false，不拦截父组件鼠标事件
- **无 hover 动画时的绘制开销**：每次 repaint 仅重绘自身小区域

### 关键实现点

```java
@Override public boolean contains(int x, int y) {
    return interactive ? super.contains(x, y) : false;
}
@Override public Dimension getPreferredSize() { return new Dimension(size, size); }
@Override public Dimension getMinimumSize() { return getPreferredSize(); }
```

## 二、Tag 重构 + 展现增强

### 现状问题

- 关闭靠 `e.getX() > xw` 坐标判断，× 画在文字后缀 `"  ×"` 中，命中区不精确
- 只有 light 效果，无 dark/plain，无尺寸档位

### 布局重构（核心变化）

Tag 从纯自绘改为「自绘背景/边框/文字 + 右侧叠加 CloseButton 子组件」：

- `setLayout(null)`，`addNotify` 时若 closable 则 `add(closeButton)`
- 重写 `doLayout()`：CloseButton 定位在右侧，垂直居中，x = 宽度 - closeButton 宽 - 右内边距
- `getPreferredSize()`：宽度 = 左内边距 + 文字宽 + 间距 + （closable ? closeButton 宽 : 0）+ 右内边距；高度按尺寸档位
- 文字绘制区域右边界给 CloseButton 让位
- 删除 `addNotify` 中的坐标命中 MouseListener 和文字后缀 ×；CloseButton 的 ActionListener 调 `close(() -> {})`
- 收缩动画 `closeAnim` 保持不变

### effect 三种效果（对齐 Element UI Tag）

| effect | 背景 | 文字 | 边框 |
|---|---|---|---|
| `EFFECT_DARK` | 各 type 实色（PRIMARY/SUCCESS/WARNING/DANGER/INFO） | 白色（Element 标准，对比度例外标注） | 同背景色 |
| `EFFECT_LIGHT` | 各 type 浅色（现有 BG 数组） | 各 type **深色变体**（同按钮 PLAIN_FG 取值，保证 ≥4.5:1） | 各 type 浅边框（现有 BORDER 数组） |
| `EFFECT_PLAIN` | 白色 | 各 type 深色变体 | 各 type 实色边框 |

### 尺寸档位

| 尺寸 | 字体 | 垂直内边距 | 水平内边距 | CloseButton 边长 |
|---|---|---|---|---|
| SIZE_LARGE | 14px | 8 | 16 | 20 |
| SIZE_DEFAULT | 12px | 4 | 10 | 18 |
| SIZE_SMALL | 12px | 2 | 8 | 16 |

### 新增 API

```java
public static final int EFFECT_DARK = 0, EFFECT_LIGHT = 1, EFFECT_PLAIN = 2;
public static final int SIZE_LARGE = 0, SIZE_DEFAULT = 1, SIZE_SMALL = 2;
public void setEffect(int effect);   // 构造后可切换，默认 EFFECT_LIGHT（向后兼容）
public void setSize(int size);       // 默认 SIZE_DEFAULT
```

现有构造函数 `Tag(String, int, boolean)` 签名不变。

### 对比度断言

light/plain 各 type：文字深色变体 vs 对应背景 ≥ 4.5:1；× 符号颜色 vs 背景同断言。dark 效果白字彩色底为例外（标注「Element 标准实心」）。

## 三、Alert 关闭重写

- 删除 `mouseClicked` 中 `e.getX() > getWidth() - 28` 的坐标判断和 `paintComponent` 中的 × 绘制
- `AstAlert` 增加 CloseButton 子组件（`setLayout(null)`，`doLayout` 定位右上角：x = 宽-16-按钮宽，y = 垂直居中于标题行）
- **alpha 联动**：`inAnim`/`outAnim` 回调中同步 `closeButton.setAlpha(inP * (1 - outP))`，alpha < 0.5 时 `setInteractive(false)`，否则 true
- 其余绘制逻辑不变

## 四、Input 清空重写

- 删除 `mouseClicked` 中 `e.getX() > getWidth() - 30` 坐标判断和 `paintComponent` 中的 × 淡入绘制
- CloseButton 放入 Input 面板右侧：field 仍占 `BorderLayout.CENTER`，CloseButton 加 `BorderLayout.EAST`（field 右侧 padding 30 已预留空间）
- **alpha 联动**：`clearAnim` 回调中 `clearButton.setAlpha(clearVis)` + `setInteractive(clearVis > 0.5f)`
- CloseButton 点击：`setText("")` + `field.requestFocus()`，× 颜色用 `0x909399`（对白色背景对比度 3.9:1 → 改用 `0x606266` 深灰变体，≥7:1），hover 色用 PRIMARY 深变体 `0x1d6fb5`
- field 上的 MouseAdapter 仅保留 hover 动画用途，删除 mouseClicked

批次 1 不动 Input 其他展现方式（尺寸/密码/前后缀图标归批次 2）。

## 五、AstDialog 新增 × 关闭

- 标题栏右上角放置 CloseButton（24×24），垂直居中，x = 宽 - 24 - 16
- 点击触发 `finish(RESULT_CANCEL)`（与取消按钮等价）
- `makeCard` 是 public 且被 AstMessageBox 复用——MessageBox 卡片同样获得 × 关闭，行为是 RESULT_CANCEL，需验证 AstMessageBox 现有 selfCheck 不受影响
- 标题栏 JPanel 改为可容纳子组件：`setLayout(null)` + `doLayout` 定位 CloseButton，标题文字绘制保持不变

## 六、selfCheck 新增

### Tag.selfCheck()（新增，`java -ea -cp out org.swelement.ui.Tag`）

- effect 切换后 preferredSize 有效
- closable Tag 宽度 > 不可关闭 Tag 宽度
- CloseButton 子组件存在且位于右侧
- light/plain 各 type 对比度断言（文字深色变体 vs 背景）
- 尺寸三档高度递减断言

### Alert.selfCheck()（新增）

- closable 时 CloseButton 子组件存在
- 非 closable 时无子组件
- close() 动画完成后 onClosed 回调触发（复用现有 closeAnim 逻辑断言）

### Input.selfCheck()（新增）

- setText 后 hasText 变化、clearVis 目标切换
- CloseButton 点击清空文本（通过 doClick 触发 ActionListener 路径验证）
- 清空后 field 获得焦点请求不抛异常

### CloseButton.selfCheck()（新增）

- preferredSize 正方形
- addActionListener 点击触发（doClick / dispatchEvent 模拟）
- setInteractive(false) 后 contains 返回 false
- × 符号默认色 vs 白色背景对比度 ≥ 4.5:1（用深灰变体验证）

## 七、Demo 更新

- `TagDemo`：新增 effect 三种效果行、尺寸三档行、可关闭标签（点 × 收缩动画）
- `AlertDemo`：closable 示例改为真实可点的 CloseButton（视觉差异说明：hover 有圆形底色）
- `InputDemo`：清空按钮 hover 效果展示
- `AstAdvancedDemo`（或实际展示 AstDialog 的 Demo）：对话框右上角 × 关闭演示

## 八、实施顺序（供计划文档细化）

1. CloseButton 组件 + selfCheck（TDD：先写断言）
2. Tag 重构（关闭逻辑 + effect + 尺寸）+ selfCheck + Demo
3. Alert 关闭重写 + selfCheck + Demo
4. Input 清空重写 + selfCheck + Demo
5. AstDialog × 关闭 + 验证 AstMessageBox selfCheck
6. 全量编译 + 全部相关 selfCheck 运行 + Demo 冒烟

## 明确不做

- 不改 Tag/Alert/Input/AstDialog 现有公共构造函数签名
- Input 的尺寸/密码切换/前后缀图标/textarea 归批次 2
- Checkbox/Radio 的 bordered 归批次 3
- Tabs closable 标签页（复用 CloseButton）归批次 4
- 不引入图标字体或图片资源，× 仍为矢量线段绘制
- AstDrawer 已是可点击组件方式，不动
