# 自定义组件开发指南

## 选择基类

开发自定义组件的第一步是选择合适的基类。框架提供四层继承结构：

| 基类 | 适用场景 | 内置能力 |
|------|---------|---------|
| **AstInteractiveComponent** | 可交互组件（按钮、开关、复选框等） | hover/active/focus 动画 + 鼠标/焦点监听 + 手型光标 |
| **AstContainerComponent** | 容器型组件（卡片、面板、标签页等） | 圆角管理 + 标准容器绘制 + 主题绑定 |
| **AstDisplayComponent** | 纯展示组件（标签、角标、进度条等） | 最小开销，默认箭头光标，无内置交互 |
| **AstAbstractComponent** | 特殊需求，以上都不满足 | 主题绑定 + 动画管理 + 绘制辅助 + 自检 |

### 继承关系

```
JComponent
    └── AstAbstractComponent  (主题 + 动画 + 绘制辅助 + 自检)
            ├── AstInteractiveComponent  (hover/active/focus 交互)
            ├── AstContainerComponent    (容器外观 + 圆角)
            └── AstDisplayComponent      (纯展示，最小开销)
```

## 开发步骤

### Step 1: 继承基类

根据组件特性选择合适的基类：

```java
public class MyWidget extends AstInteractiveComponent {
}
```

### Step 2: 重写 initComponent()

注册自定义动画、设置属性、创建子组件。**必须调用 `super.initComponent()`**：

```java
import org.swelement.core.Easing;
import java.awt.Dimension;

@Override
protected void initComponent() {
    super.initComponent(); // 必须调用！否则基类的初始化逻辑不会执行

    // 注册自定义动画
    anim.register("fill", 300, Easing::easeOut);
    anim.register("spin", 800, Easing::linear);

    // 设置默认属性
    setPreferredSize(new Dimension(100, 40));
}
```

### Step 3: 重写 paintComponent()

使用基类提供的绘制辅助方法。**必须使用 `createGraphics(g)` 创建副本，并在结束时调用 `g2.dispose()`**：

```java
import org.swelement.core.theme.Theme;
import java.awt.*;

@Override
protected void paintComponent(Graphics g) {
    Graphics2D g2 = createGraphics(g); // 自动抗锯齿
    Theme t = theme();

    // 获取动画进度
    float hover = hoverProgress();
    float fill = anim.getProgress("fill");

    // 使用基类绘制方法
    Color bg = lerp(t.getFillBlank(), t.getPrimary(), hover);
    g2.setColor(bg);
    fillRoundRect(g2, 0, 0, getWidth() - 1, getHeight() - 1, radius());

    // 文字绘制
    g2.setColor(t.getTextPrimary());
    g2.setFont(t.getFontBase());
    drawCenteredText(g2, getText(), 0, getWidth(),
        getHeight() / 2f + g2.getFontMetrics().getAscent() / 2f - g2.getFontMetrics().getDescent());

    g2.dispose(); // 必须释放资源！
}
```

### Step 4: 重写 getPreferredSize()

返回组件的首选尺寸：

```java
@Override
public Dimension getPreferredSize() {
    FontMetrics fm = getFontMetrics(theme().getFontBase());
    int w = fm.stringWidth(getText()) + 32;
    int h = 36;
    return new Dimension(w, h);
}
```

### Step 5: 实现 selfCheck()

每个组件必须实现自检方法，覆盖尺寸计算、状态切换、对比度、边界条件等：

```java
import org.swelement.core.SelfCheckBase;
import java.awt.Color;

@Override
protected void selfCheck() {
    MyWidget w = new MyWidget();

    // 尺寸测试
    assert w.getPreferredSize().width > 0 : "width should be positive";

    // 对比度测试
    Theme t = theme();
    assertContrast(t.getTextPrimary(), t.getFillBlank(), "text on bg");
    assertContrast(Color.WHITE, t.getPrimary(), "white on primary");
}
```

> 注意：`assertContrast` 等断言方法来自 `SelfCheckBase`。需要在自检类中继承使用，或直接调用 `SelfCheckBase` 的实例方法。实际使用时，组件的 `selfCheck()` 可以创建一个 `SelfCheckBase` 子类实例来使用这些工具方法。

## 完整示例：自定义开关

以下是一个完整的自定义开关组件示例：

```java
import org.swelement.framework.AstInteractiveComponent;
import org.swelement.core.Easing;
import org.swelement.core.theme.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MySwitch extends AstInteractiveComponent {

    private boolean on = false;
    private static final int SWITCH_WIDTH = 44;
    private static final int SWITCH_HEIGHT = 22;
    private static final int KNOB_SIZE = 18;

    @Override
    protected void initComponent() {
        super.initComponent();

        // 注册滑块动画
        anim.register("slide", 300, Easing::easeInOut);

        // 设置首选尺寸
        setPreferredSize(new Dimension(SWITCH_WIDTH, SWITCH_HEIGHT));

        // 添加点击事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isEnabled()) {
                    setOn(!on);
                }
            }
        });
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean newOn) {
        if (this.on != newOn) {
            this.on = newOn;
            float from = anim.getProgress("slide");
            float to = newOn ? 1f : 0f;
            anim.go("slide", from, to);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        Theme t = theme();

        float slide = anim.getProgress("slide");
        int w = getWidth() - 1;
        int h = getHeight() - 1;

        // 轨道背景
        Color trackColor;
        if (!isEnabled()) {
            trackColor = t.getFillBase();
        } else {
            trackColor = lerp(t.getBorderBase(), t.getPrimary(), slide);
        }
        g2.setColor(trackColor);
        fillRoundRect(g2, 0, 0, w, h, h / 2);

        // 旋钮
        int knobLeft = Math.round(2 + slide * (w - KNOB_SIZE - 4));
        int knobTop = (h - KNOB_SIZE) / 2;
        g2.setColor(Color.WHITE);
        g2.fillOval(knobLeft, knobTop, KNOB_SIZE, KNOB_SIZE);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(SWITCH_WIDTH, SWITCH_HEIGHT);
    }

    @Override
    protected void selfCheck() {
        MySwitch sw = new MySwitch();

        // 尺寸测试
        Dimension pref = sw.getPreferredSize();
        assert pref.width == SWITCH_WIDTH : "width should match";
        assert pref.height == SWITCH_HEIGHT : "height should match";

        // 初始状态
        assert !sw.isOn() : "should be off by default";

        // 状态切换
        sw.setOn(true);
        assert sw.isOn() : "should be on after setOn(true)";

        // 对比度测试
        Theme t = sw.theme();
        // 开启状态：白色旋钮在主色轨道上
        assertContrastHelper(Color.WHITE, t.getPrimary(), "knob on primary track");
        // 关闭状态：白色旋钮在边框色轨道上
        assertContrastHelper(Color.WHITE, t.getBorderBase(), "knob on border track");
    }

    // 辅助方法：使用 SelfCheckBase 进行对比度断言
    private void assertContrastHelper(Color fg, Color bg, String where) {
        new SelfCheckBase() {}.assertContrast(fg, bg, where);
    }
}
```

## 基类 API 速查

### AstAbstractComponent

所有组件基类的顶层抽象类，提供核心基础设施。

#### 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `anim` | `AnimationManager` | 动画管理器实例 |

#### 主题相关

| 方法 | 说明 |
|------|------|
| `theme()` | 获取当前主题实例 |
| `onThemeUpdated(oldTheme, newTheme)` | 主题变更钩子，默认调用 repaint() |

#### 生命周期

| 方法 | 说明 |
|------|------|
| `initComponent()` | 初始化钩子，在构造函数末尾调用 |
| `removeNotify()` | 组件移除时清理主题监听和动画资源（已自动实现） |
| `selfCheck()` | 自检方法（抽象，必须实现） |

#### 绘制辅助

| 方法 | 说明 |
|------|------|
| `createGraphics(g)` | 创建带抗锯齿的 Graphics2D 副本（几何+文字+描边） |
| `lerp(a, b, t)` | RGBA 四通道颜色线性插值，自动钳制 t 到 [0,1] |
| `fillRoundRect(g2, x, y, w, h, radius)` | 填充圆角矩形 |
| `drawRoundRect(g2, x, y, w, h, radius)` | 绘制圆角矩形边框 |
| `drawCenteredText(g2, text, x, width, baselineY)` | 水平居中文本绘制 |
| `radius()` | 获取主题基础圆角半径 |

### AstInteractiveComponent

交互组件基类，继承自 `AstAbstractComponent`。

#### 动画进度

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `hoverProgress()` | float | hover 动画进度 [0, 1] |
| `activeProgress()` | float | active 动画进度 [0, 1] |
| `focusProgress()` | float | focus 动画进度 [0, 1] |

#### 状态查询

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `isHovering()` | boolean | 是否处于悬停状态 |
| `isPressing()` | boolean | 是否处于按下状态 |
| `isFocusedFlag()` | boolean | 是否处于焦点状态（避免与 isFocused 冲突） |

#### 状态变更钩子

| 方法 | 说明 |
|------|------|
| `onHoverChanged(hovering)` | hover 状态变更钩子 |
| `onActiveChanged(active)` | active 状态变更钩子 |
| `onFocusChanged(focused)` | focus 状态变更钩子 |

#### 其他

| 方法 | 说明 |
|------|------|
| `setEnabled(enabled)` | 禁用时重置所有交互状态和动画 |

### AstContainerComponent

容器组件基类，继承自 `AstAbstractComponent`。

| 方法 | 说明 |
|------|------|
| `setRadius(radius)` | 设置圆角半径（手动设置后不再跟随主题） |
| `getRadius()` | 获取当前圆角半径 |
| `paintContainer(g2)` | 绘制标准容器外观（fillBlank 背景 + borderBase 边框） |

### AstDisplayComponent

展示组件基类，继承自 `AstAbstractComponent`。最小开销，默认箭头光标，无内置交互。

## PaintingHelper 绘制工具

`org.swelement.framework.util.PaintingHelper` 提供静态绘制工具方法，可在任何地方使用。

### 圆角矩形

| 方法 | 说明 |
|------|------|
| `roundRect(x, y, w, h, radius)` | 创建圆角矩形 Shape |
| `fillRoundRect(g2, x, y, w, h, radius)` | 填充圆角矩形 |
| `drawRoundRect(g2, x, y, w, h, radius)` | 绘制圆角矩形边框 |

### 圆形

| 方法 | 说明 |
|------|------|
| `fillCircle(g2, cx, cy, radius)` | 填充圆形（cx, cy 为圆心） |
| `drawCircle(g2, cx, cy, radius)` | 绘制圆形边框 |

### 文字

| 方法 | 说明 |
|------|------|
| `drawCenteredText(g2, text, x, width, baselineY)` | 水平居中文本（指定基线） |
| `drawTextInCenter(g2, text, x, y, w, h)` | 矩形内完全居中（水平+垂直） |

### 发光效果

| 方法 | 说明 |
|------|------|
| `drawGlow(g2, shape, color, size, alpha)` | 多层描边模拟外发光 |

### 颜色工具

| 方法 | 说明 |
|------|------|
| `withAlpha(color, alpha)` | 设置颜色的 alpha 通道 [0, 1] |
| `darken(color, factor)` | 颜色变暗（各通道乘以 factor） |
| `lighten(color, factor)` | 颜色变亮（向白色插值） |

### 图标（字符型）

| 方法 | 说明 |
|------|------|
| `drawIcon(g2, iconChar, x, y, size, color)` | 绘制字符型图标 |

## 绘制规范

1. **总是调用 `createGraphics(g)`** 创建 Graphics2D 副本，确保抗锯齿和状态隔离
2. **总是调用 `g2.dispose()`** 释放资源，放在方法末尾或 finally 块中
3. **颜色从 `theme()` 获取**，不硬编码颜色值
4. **圆角使用 `radius()`** 或从主题获取，保持视觉一致性
5. **文字颜色对比度 ≥ 4.5:1**（WCAG AA 级），所有状态都需满足
6. **绘制坐标考虑边框**：宽度和高度通常使用 `getWidth() - 1` 和 `getHeight() - 1`
7. **文字基线计算**：垂直居中时使用 `y + (h - fm.getHeight()) / 2 + fm.getAscent()`

## 自检规范

每个组件的 `selfCheck()` 必须覆盖以下类别：

| 类别 | 检查内容 |
|------|---------|
| 尺寸 | 不同尺寸档位、文字长度、有无图标 |
| 状态 | 默认 / hover / active / disabled / loading |
| 对比度 | 所有状态下文字与背景 ≥ 4.5:1 |
| 边界 | 空文本、极值、异常输入 |
| 功能 | 核心算法、事件回调、状态转换 |

### 自检工具方法（SelfCheckBase）

| 方法 | 说明 |
|------|------|
| `assertContrast(fg, bg, where)` | 对比度断言（默认 4.5:1 AA 级） |
| `assertContrast(fg, bg, where, minRatio)` | 指定比例的对比度断言 |
| `assertApprox(expected, actual, epsilon, msg)` | 浮点近似断言 |
| `assertDimension(expected, actual, msg)` | 尺寸断言 |
| `luminance(color)` | 计算 WCAG 相对亮度 |
| `contrastRatio(a, b)` | 计算两个颜色的对比度 |
| `lerp(a, b, t)` | 颜色线性插值（自检用） |

### 运行自检

使用 `-ea` 参数启用断言：

```bash
java -ea -cp out org.swelement.demo.SelfCheckRunner
```

## 常见问题

**Q: 组件的鼠标事件不生效？**

确保组件设置了 `setPreferredSize()` 且 `isEnabled()` 为 `true`。交互组件默认启用鼠标响应，但需要组件有明确的尺寸才能接收鼠标事件。

**Q: 主题切换后组件没有更新？**

检查是否缓存了主题引用。应该每次绘制都调用 `theme()` 获取最新主题，而不是在构造函数中保存主题引用。

**Q: 自定义动画不流畅？**

确保动画回调只做状态更新和 `repaint()`，不要做计算密集型操作。Swing 会合并 repaint 请求，但计算量大时仍可能掉帧。

**Q: 如何禁用组件的动画？**

可以调用 `anim.stopAll()` 停止所有动画，或使用 `anim.setProgress(name, value)` 直接设置最终值（无过渡）。

**Q: 组件被移除后还有内存泄漏？**

`AstAbstractComponent` 的 `removeNotify()` 会自动清理主题监听和动画资源。如果自定义了额外的资源，应重写 `removeNotify()` 并调用 `super.removeNotify()`。

**Q: 如何在组件中添加自定义主题变量？**

通过主题的扩展方法 `getColor(String key)`、`getFont(String key)`、`getSize(String key)` 获取自定义变量。自定义主题时可重写这些方法返回额外的变量值。

**Q: 容器组件如何自定义圆角？**

调用 `setRadius(int)` 手动设置圆角，设置后圆角将不再随主题变化。如果需要恢复跟随主题，目前需要重新创建组件或通过自定义子类实现。
