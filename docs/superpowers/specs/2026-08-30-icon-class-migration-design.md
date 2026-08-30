# 图标对象化改造设计文档（Icon 类替代 String）

日期：2026-08-30
状态：待批准

## 目标

将项目中使用 `String` 字符充当图标的地方统一改造为图标类：`javax.swing.Icon`、`ImageIcon`、以及本框架的 `AstIcon`。核心是让 `AstButton` 的图标从 `String` 改为 `Icon`，并让 `AstIcon` 真正实现 `javax.swing.Icon` 接口，从而三个图标类型可被统一接受。同时清理 `AstTree`、`PaintingHelper` 及所有 demo/自检中的 String 图标用法。

## 背景与现状

| 位置 | 当前用法 | 问题 |
|---|---|---|
| `AstButton` | `private String icon` + `setIcon(String)` + `g2.drawString(icon,...)` | 把图标当文本字符渲染 |
| `AstTree` | `String icon = expanded ? "▼" : "▶"` | 展开箭头用字符 |
| `PaintingHelper.drawIcon(g2, String iconChar,...)` | String 字符 | 工具方法参数为字符串 |
| `ButtonDemo` / `BadgeDemo` / `FrameworkDemo` | `setIcon("\u2713")`、`"🔔"` 等 | demo 用字符串 |
| `AstAvatar` | `ImageIcon icon` | ✅ 已是图标类（参考） |
| `AstInput` | `AstIcon prefixIcon/suffixIcon` | ✅ 已用 AstIcon（参考） |

关键约束：`AstIcon extends AstDisplayComponent`（JComponent），**尚未实现 `javax.swing.Icon`**，因此当前无法直接作为 `Icon` 传入 `AstButton`。

## 关键决策（已与需求方确认）

1. **AstIcon 实现 `javax.swing.Icon` 接口** —— 补齐 `paintIcon(Component, Graphics, int, int)`、`getIconWidth()`、`getIconHeight()`，复用现有静态绘制器。这是让 `AstButton.setIcon(Icon)` 统一接受三类图标的关键。
2. **移除 `setIcon(String)`** —— 彻底落实"图标不应该是 String"。同步更新 demo 与自检。
3. **全量改造** —— 除 AstButton 外，一并处理 `AstTree` 展开箭头、`PaintingHelper.drawIcon`、以及 `ButtonDemo`/`BadgeDemo`/`FrameworkDemo` 与 `AstButton.selfCheck`。
4. **按钮内嵌 `AstIcon` 时图标颜色跟随按钮文字色**（随 hover / active / disabled 变化），与 Element UI 一致；`ImageIcon` 保留自身像素。

## API 变更

### `AstIcon`（src/org/swelement/ui/AstIcon.java）

类声明增加 `implements Icon`：

```java
public class AstIcon extends AstDisplayComponent implements Icon
```

新增（满足 `javax.swing.Icon` 接口，复用现有静态绘制器 `paintIcon(Graphics2D, Type, Color, int, float)`）：

```java
@Override
public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.translate(x, y);
    paintIcon(g2, type, color, size, spinPhase);
    g2.dispose();
}

@Override
public int getIconWidth() { return size; }

@Override
public int getIconHeight() { return size; }
```

新增 getter（供外部嵌入绘制复用旋转相位）：`public float getSpinPhase() { return spinPhase; }`

重载说明：现有静态 `paintIcon(Graphics2D, Type, Color, int, float)` 与新增实例 `paintIcon(Component, Graphics, int, int)` 签名不同，不冲突。

尺寸约束不变：`size ∈ [8, 64]`。

### `AstButton`（src/org/swelement/ui/AstButton.java）

- 字段：`private String icon` → `private Icon icon`
- 方法：`setIcon(String)` → `setIcon(Icon icon)`，新增 `getIcon()`
- `getPreferredSize()`：图标宽度改用 `icon.getIconWidth()`（替换 `fm.stringWidth(icon)`）
- `paintComponent()` 绘制部分：
  - 图标宽度改用 `icon.getIconWidth()`
  - **垂直居中**：文本视觉中心为 `getHeight()/2`，故 `iconY = (getHeight() - icon.getIconHeight()) / 2`
  - 绘制委托 `icon.paintIcon(this, g2, x, iconY)`
  - **颜色跟随**：当 `icon instanceof AstIcon` 时，用按钮当前前景色 `fg` 重画线条
    ```java
    if (icon instanceof AstIcon) {
        AstIcon ai = (AstIcon) icon;
        AstIcon.paintIcon(g2, ai.getTypeEnum(), fg, ai.getSizeValue(), ai.getSpinPhase());
    } else {
        icon.paintIcon(this, g2, x, iconY);
    }
    ```
    AstIcon 的 INFO / SUCCESS / WARNING / ERROR 状态图标底色固定走主题色（`drawInfoCircle` 等），此处 `fg` 仅作用于线条颜色，安全。

### `AstTree`（src/org/swelement/ui/AstTree.java）

第 284-287 行展开箭头由 String 改为 `AstIcon.CARET_DOWN` / `AstIcon.CARET_RIGHT` 绘制：

```java
if (row.node.hasChildren()) {
    AstIcon caret = row.node.isExpanded()
        ? new AstIcon(AstIcon.Type.CARET_DOWN, textColor, EXPANDER_W)
        : new AstIcon(AstIcon.Type.CARET_RIGHT, textColor, EXPANDER_W);
    int ix = expX + (EXPANDER_W - caret.getIconWidth()) / 2;
    int iy = y + (rowH - caret.getIconHeight()) / 2;
    caret.paintIcon(this, g2, ix, iy);
}
```

`EXPANDER_W = 16`（在 `[8,64]` 范围内，满足 AstIcon 构造约束）。

### `PaintingHelper`（src/org/swelement/framework/util/PaintingHelper.java）

新增 `Icon` 重载，委托 `Icon.paintIcon(null, g2, x, y)`：

```java
public static void drawIcon(Graphics2D g2, Icon icon, int x, int y) {
    if (icon == null) return;
    icon.paintIcon(null, g2, x, y);
}
```

移除 String 版 `drawIcon(Graphics2D, String, int, int, int, Color)`（改由 AstIcon 承担）。

### demo 与自检

- `ButtonDemo`：`String[] icons` → `AstIcon.Type[]`；`b.setIcon(new AstIcon(...))`
- `BadgeDemo`：`"🔔"` 字符 → `AstIcon.Type.BELL` 绘制
- `FrameworkDemo`：`PaintingHelper.drawIcon(g2, String,...)` → `PaintingHelper.drawIcon(g2, new AstIcon(...), ...)`
- `AstButton.selfCheck`：`setIcon("\u2713")` → `setIcon(new AstIcon(AstIcon.Type.CHECK, 颜色, 16))`

## 可访问性与对比度（最高优先级）

- AstIcon 作为按钮图标时，跟随按钮前景色 `fg`，与文字同色，天然满足对比度（文字色已由 AstButton 各状态保证 ≥ 4.5:1）。
- 彩色实心按钮（PRIMARY/SUCCESS/WARNING/DANGER）白色线条图标属于 Element UI 标准视觉，遵循既有设计例外（与现有实心按钮一致）。
- 不引入新的浅色背景配浅色文字组合。

## 验证

1. 删除 `out/`（清陈旧字节码），全量重新编译。
2. 运行 `run-checks.bat`，确认全部自检通过（含 `AstButton`、`AstIcon`、`AstTree`、`PaintingHelper`、demo 自检）。
3. 手动运行 `ButtonDemo` 目视验证：图标按钮 circle、图标左/右、颜色随按钮状态变化。

## 明确不做

- 不引入外部图标字体、图片资源（AstIcon 保持纯 Graphics2D 自绘）。
- 不改变现有 `AstButton` 构造方法签名（`AstButton(String)`、`AstButton(String,int,boolean)`）。
- 不为 `AstIcon` 新增更多图标类型（沿用现有 54 枚举）。
- `AstButton` 不保留 `setIcon(String)` 兼容重载（已确认移除）。
