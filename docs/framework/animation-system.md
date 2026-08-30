# 动画系统

## 概述

框架的动画系统由三部分组成：

- **Easing** - 缓动函数接口与内置实现
- **Animator** - 单动画调度引擎（基于 Swing Timer）
- **AnimationManager** - 组件动画管理器（命名动画的集合，自动触发 repaint）

## Easing 缓动函数

`Easing` 是一个函数式接口，定义了 `float apply(float t)` 方法，输入进度 `t`（范围 [0, 1]），输出缓动后的值。

### 内置缓动

| 方法 | 公式 | 说明 | 适用场景 |
|------|------|------|---------|
| `Easing::linear` | `t` | 匀速 | 简单线性变化 |
| `Easing::easeIn` | `t³` | 先慢后快 | 元素出场 |
| `Easing::easeOut` | `1 - (1-t)³` | 先快后慢 | 元素入场 |
| `Easing::easeInOut` | 分段三次函数 | 两端慢中间快 | 双向平滑过渡（默认） |

### 自定义缓动

`Easing` 是函数式接口，可用 lambda 直接创建：

```java
// 弹性缓动示例
Easing bounce = t -> (float) (1 - Math.pow(1 - t, 4) * Math.cos(t * Math.PI * 4));

// 过冲缓动示例
Easing overshoot = t -> {
    float c1 = 1.70158f;
    float c3 = c1 + 1f;
    return 1f + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
};
```

## Animator 动画器

`Animator` 是底层动画引擎，基于 `javax.swing.Timer` 实现，所有更新回调在 EDT 上执行。

### 基本用法

```java
import org.swelement.core.Animator;
import org.swelement.core.Easing;

Animator anim = new Animator(200, Easing::easeInOut, new Animator.Listener() {
    @Override
    public void update(float value) {
        // value 在 [0, 1] 之间，经过缓动函数处理
        // 此处更新状态并触发重绘
        repaint();
    }
});

// 从 0 过渡到 1
anim.go(0f, 1f);

// 从当前值过渡到 0
anim.go(currentValue, 0f);

// 停止动画（不会触发完成回调）
anim.stop();
```

### 完成回调

```java
anim.go(0f, 1f, new Runnable() {
    @Override
    public void run() {
        // 动画完成后在 EDT 上执行
        // 如果动画被 stop() 中断，则不会触发
        System.out.println("动画完成");
    }
});
```

### 特性说明

- 使用 16ms 间隔的 Swing Timer（约 60fps），并启用 `coalesce` 合并回调
- 所有 `update` 回调和完成回调均在 EDT 上执行，线程安全
- 重复调用 `go()` 会取消之前的动画并清除之前的完成回调
- 调用 `stop()` 会停止动画且不会触发完成回调

## AnimationManager 动画管理器

### 为什么使用 AnimationManager

- **统一管理** 组件的所有动画，避免散落的 Animator 字段
- **命名引用** 通过字符串名称引用动画，代码更清晰
- **自动 repaint** 动画更新时自动触发所属组件的 `repaint()`
- **内置标准动画名称常量** 保持交互一致性

### 注册动画

```java
import org.swelement.core.AnimationManager;
import org.swelement.core.Easing;

// 在 initComponent() 中注册
anim.register("spin", 1000, Easing::linear);
anim.register("fade", 300, Easing::easeOut);
anim.register("expand", 250, Easing::easeInOut);
```

> 注意：`anim` 是 `AstAbstractComponent` 中的受保护字段，类型为 `AnimationManager`。

### 驱动动画

```java
// 驱动到 1（进入状态），从当前进度开始
anim.start("fade");

// 驱动到 0（退出状态），从当前进度开始
anim.stop("fade");

// 指定起始和结束值
anim.go("spin", 0f, 1f);

// 立即设置进度（无动画，停止当前动画）
// 值会自动钳制到 [0, 1] 范围
anim.setProgress("fade", 0.5f);
```

### 获取进度

```java
float fadeProgress = anim.getProgress("fade");
```

未注册的动画名称返回 `0f`。

### 查询动画是否存在

```java
boolean hasFade = anim.has("fade");
```

### 获取原始 Animator

```java
Animator spinAnim = anim.get("spin");
```

### 批量控制

```java
// 停止所有动画（进度值保持不变）
anim.stopAll();

// 销毁：停止所有动画并清空资源
// 组件 removeNotify 时自动调用
anim.dispose();
```

## 内置标准动画

交互组件基类（`AstInteractiveComponent`）在 `initComponent()` 中自动注册三个标准动画：

| 名称 | 常量 | 时长 | 缓动 | 触发时机 |
|------|------|------|------|---------|
| hover | `AnimationManager.HOVER` | 200ms | easeInOut | 鼠标进入/离开 |
| active | `AnimationManager.ACTIVE` | 120ms | easeInOut | 鼠标按下/释放 |
| focus | `AnimationManager.FOCUS` | 200ms | easeInOut | 获得/失去焦点 |

### 使用方式

在 `paintComponent` 中直接使用便捷方法获取进度：

```java
@Override
protected void paintComponent(Graphics g) {
    Graphics2D g2 = createGraphics(g);
    Theme t = theme();

    float hover = hoverProgress();
    float active = activeProgress();
    float focus = focusProgress();

    // 颜色插值：默认 → hover → active 三级过渡
    Color bg = lerp(
        lerp(t.getFillBlank(), t.getPrimary(), hover),
        darkenColor(t.getPrimary()),
        active
    );

    g2.setColor(bg);
    fillRoundRect(g2, 0, 0, getWidth() - 1, getHeight() - 1, radius());

    g2.dispose();
}
```

### 便捷方法一览

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `hoverProgress()` | float | 获取 hover 动画进度 [0, 1] |
| `activeProgress()` | float | 获取 active 动画进度 [0, 1] |
| `focusProgress()` | float | 获取 focus 动画进度 [0, 1] |
| `isHovering()` | boolean | 是否处于 hover 状态 |
| `isPressing()` | boolean | 是否处于按下状态 |
| `isFocusedFlag()` | boolean | 是否处于焦点状态 |

> `isFocusedFlag()` 方法名避免与 JComponent 的 `isFocused()` 冲突。

## 状态钩子

交互组件基类提供状态变更钩子，子类可重写以响应状态变化：

```java
@Override
protected void onHoverChanged(boolean hovering) {
    // hover 状态变化时执行
    // hovering=true 表示进入悬停，false 表示离开
}

@Override
protected void onActiveChanged(boolean active) {
    // 按下状态变化时执行
    // active=true 表示按下，false 表示释放
}

@Override
protected void onFocusChanged(boolean focused) {
    // 焦点状态变化时执行
    // focused=true 表示获得焦点，false 表示失去焦点
}
```

> 状态钩子在状态切换时立即调用，不等待动画完成。

### 禁用态处理

当组件被禁用时（`setEnabled(false)`），交互组件基类会自动：

- 重置所有交互状态（hovering / pressing / focused 设为 false）
- 停止所有标准动画（hover / active / focus）
- 确保组件回到初始视觉状态

## 绘制中的颜色插值

使用基类的 `lerp()` 方法进行颜色插值，支持 RGBA 四通道过渡：

```java
// 基础 → hover → active 三级插值
Color bg = lerp(
    lerp(baseColor, hoverColor, hoverProgress()),
    activeColor,
    activeProgress()
);

// 带透明度的颜色过渡也支持
Color translucent = lerp(
    withAlpha(baseColor, 0f),
    withAlpha(baseColor, 1f),
    fadeProgress
);
```

> 提示：`lerp()` 会自动将 `t` 钳制到 [0, 1] 范围，无需手动处理边界。

## 性能说明

- 每个 `AnimationManager` 内部管理多个 `Animator`
- 每个 `Animator` 持有一个 Swing Timer（16ms 间隔，约 60fps）
- Swing EDT 会合并 repaint 请求，实际重绘次数远少于帧数
- 组件 `removeNotify` 时自动 `dispose` 所有动画，无内存泄漏
- 动画更新回调中应只做状态更新和触发 repaint，避免计算密集型操作

## 标准动画名称常量

`AnimationManager` 提供以下常量供使用：

| 常量 | 值 | 说明 |
|------|-----|------|
| `HOVER` | `"hover"` | 悬停动画 |
| `FOCUS` | `"focus"` | 聚焦动画 |
| `ACTIVE` | `"active"` | 激活动画 |
| `PRESS` | `"press"` | 按下动画 |
| `OPEN` | `"open"` | 打开动画 |
| `CLOSE` | `"close"` | 关闭动画 |

## 最佳实践

1. **使用 AnimationManager** 而不是直接创建 Animator，统一管理更方便
2. **优先使用内置标准动画**（hover / active / focus），保持交互一致性
3. **动画时长遵循规范**：hover 200ms、active 120ms、focus 200ms
4. **颜色插值使用 lerp()** 方法，支持透明度过渡
5. **不要在动画回调中做重计算**，只更新状态变量和触发 repaint
6. **禁用组件时考虑动画状态**，确保视觉正确回退
7. **自定义动画使用有意义的名称**，提高代码可读性
