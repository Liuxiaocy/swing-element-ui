# Swing Element UI 框架化设计规格

> 版本：v2.0.0  
> 日期：2026-08-30  
> 状态：设计评审中

---

## 目录

1. [项目背景与目标](#1-项目背景与目标)
2. [总体架构设计](#2-总体架构设计)
3. [核心模块：主题系统](#3-核心模块主题系统)
4. [核心模块：动画系统](#4-核心模块动画系统)
5. [核心模块：自检框架](#5-核心模块自检框架)
6. [组件基类体系](#6-组件基类体系)
7. [绘制辅助工具](#7-绘制辅助工具)
8. [组件迁移策略](#8-组件迁移策略)
9. [扩展性设计](#9-扩展性设计)
10. [质量保证体系](#10-质量保证体系)
11. [目录结构重构](#11-目录结构重构)
12. [文档体系](#12-文档体系)
13. [构建与发布](#13-构建与发布)
14. [风险与权衡](#14-风险与权衡)

---

## 1. 项目背景与目标

### 1.1 背景

Swing Element UI 项目已完成 40+ 个 Element UI 风格的 Swing 组件开发，具备完整的动画引擎和主题色板。但当前架构存在以下问题：

1. **样板代码重复**：每个组件独立编写动画初始化、鼠标监听、颜色插值逻辑，约 30-40% 代码为重复模式
2. **主题不可切换**：`ElementTheme` 为静态常量类，运行时无法换肤
3. **缺乏统一规范**：组件各自实现，API 风格、事件处理、绘制模式不完全一致
4. **扩展门槛高**：新增组件需理解全部底层细节，缺乏框架层抽象
5. **目录结构扁平**：所有组件平铺在 `ui/` 目录下，数量增长后难以管理

### 1.2 目标

| 目标 | 衡量标准 |
|------|---------|
| **易用性** | 新增组件代码量减少 50%+，30 分钟内可完成一个简单组件开发 |
| **可扩展性** | 支持自定义主题、自定义组件、组件装饰器三层扩展机制 |
| **易维护性** | 统一基类 + 模块化目录 + 结构化自检，维护成本降低 40% |
| **零依赖** | 保持纯 JDK 8 标准库，不引入任何外部依赖 |
| **向后兼容** | 现有组件 API 尽量保持不变，迁移成本可控 |

### 1.3 设计原则

1. **约定优于配置**：合理的默认值，常用功能开箱即用
2. **渐进式采用**：可逐步迁移，新旧组件可共存
3. **单一职责**：每层只做一件事，边界清晰
4. **面向接口编程**：核心能力通过接口定义，实现可替换
5. **可测试性**：纯逻辑与 UI 分离，核心逻辑可单元测试

---

## 2. 总体架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                   Application / Demo Layer                  │
│              (用户业务代码 / 组件演示)                        │
├─────────────────────────────────────────────────────────────┤
│                    UI Components Layer                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ AstButton│ │ AstInput │ │ AstTable │ │ AstDialog    │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │
│  (40+ 组件，按功能分类组织)                                   │
├─────────────────────────────────────────────────────────────┤
│                   Framework Base Layer                       │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              AstAbstractComponent (顶层基类)            │ │
│  │  主题绑定 | 动画管理 | 绘制辅助 | 生命周期 | 自检        │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌──────────────────────┐ ┌──────────────────────────────┐ │
│  │ AstInteractiveComp.  │ │ AstContainerComponent        │ │
│  │  hover/active/focus  │ │  布局管理 | 子组件代理       │ │
│  └──────────────────────┘ └──────────────────────────────┘ │
│  ┌──────────────────────┐ ┌──────────────────────────────┐ │
│  │ AstDisplayComponent  │ │ PaintingHelper / Utils       │ │
│  │  纯展示型基类        │ │  绘制工具 | 通用工具          │ │
│  └──────────────────────┘ └──────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    Core Engine Layer                         │
│  ┌───────────────┐  ┌───────────────┐  ┌────────────────┐ │
│  │ Theme System  │  │ Animation Eng │  │ Popup System   │ │
│  │  Theme 接口   │  │  Animator     │  │ AnimatedPopup  │ │
│  │  ThemeManager │  │  Easing       │  │  PopupPositioner│ │
│  │  ElementTheme │  │  AnimationMgr │  │  GlassPane     │ │
│  └───────────────┘  └───────────────┘  └────────────────┘ │
│  ┌───────────────┐                                           │
│  │ SelfCheck Frm │                                           │
│  │  SelfCheckBase│                                           │
│  └───────────────┘                                           │
├─────────────────────────────────────────────────────────────┤
│                  Java Swing / AWT (JDK 8)                   │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块依赖关系

```
                    ┌──────────────┐
                    │    Theme     │  (接口，无依赖)
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │ ThemeManager │  (依赖 Theme)
                    └──────┬───────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
     ┌─────────┐    ┌───────────┐    ┌─────────────┐
     │ Easing  │    │ Animator  │    │ SelfCheckBase│
     └────┬────┘    └─────┬─────┘    └─────────────┘
          │               │
          └───────┬───────┘
                  ▼
         ┌─────────────────┐
         │ AnimationManager│
         └────────┬────────┘
                  │
         ┌────────▼────────┐
         │AstAbstractComp. │
         └────────┬────────┘
     ┌────────────┼────────────┐
     ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│Interact. │ │Container │ │ Display  │
└─────┬────┘ └─────┬────┘ └─────┬────┘
      │            │            │
      └────────────┼────────────┘
                   ▼
         ┌─────────────────┐
         │  UI Components  │  (40+ 具体组件)
         └─────────────────┘
```

**依赖方向严格单向**：`ui → framework → core → JDK`，无循环依赖。

---

## 3. 核心模块：主题系统

### 3.1 Theme 接口

```java
package org.swelement.core.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * 主题接口，定义组件绘制所需的全部视觉变量。
 * 所有颜色、尺寸、字体均通过主题获取，禁止硬编码。
 */
public interface Theme {

    /** 主题名称，用于 ThemeManager 注册和查找 */
    String getName();

    // ==================== 语义色 ====================

    Color getPrimary();      // 主色
    Color getSuccess();      // 成功色
    Color getWarning();      // 警告色
    Color getDanger();       // 危险/错误色
    Color getInfo();         // 信息色

    // ==================== 文字色 ====================

    Color getTextPrimary();      // 主要文字
    Color getTextRegular();      // 常规文字
    Color getTextSecondary();    // 次要文字
    Color getTextPlaceholder();  // 占位符文字
    Color getTextDisabled();     // 禁用文字

    // ==================== 边框色 ====================

    Color getBorderBase();     // 基础边框
    Color getBorderLight();    // 浅边框
    Color getBorderLighter();  // 更浅边框

    // ==================== 填充色 ====================

    Color getFillBlank();   // 纯白填充
    Color getFillBase();    // 基础填充（浅灰）
    Color getFillLight();   // 更浅填充

    // ==================== 圆角 ====================

    int getRadiusSmall();   // 小圆角 2px
    int getRadiusBase();    // 基础圆角 4px
    int getRadiusLarge();   // 大圆角 8px

    // ==================== 字体 ====================

    Font getFontSmall();    // 小字体 12px
    Font getFontBase();     // 基础字体 14px
    Font getFontLarge();    // 大字体 16px

    // ==================== 扩展色 ====================

    /**
     * 根据键名获取颜色，支持组件级自定义主题变量。
     * 内置键名未命中时返回 null，由调用方处理降级。
     */
    Color getColor(String key);

    /**
     * 根据键名获取字体，支持扩展。
     */
    Font getFont(String key);

    /**
     * 根据键名获取尺寸，支持扩展。
     */
    int getSize(String key);
}
```

### 3.2 ThemeManager

```java
package org.swelement.core.theme;

import java.util.List;

/**
 * 主题管理器，全局单例。
 * 负责主题注册、切换、变更通知。
 */
public final class ThemeManager {

    private ThemeManager() {}

    /**
     * 注册主题。同名主题会覆盖。
     */
    public static void registerTheme(Theme theme) { ... }

    /**
     * 获取当前主题。
     * 默认返回 ElementLightTheme。
     */
    public static Theme getCurrent() { ... }

    /**
     * 切换主题，触发所有监听器通知。
     * @param themeName 主题名称
     * @throws IllegalArgumentException 主题不存在时抛出
     */
    public static void setCurrent(String themeName) { ... }

    /**
     * 获取所有已注册主题名称列表。
     */
    public static List<String> getAvailableThemes() { ... }

    /**
     * 添加主题变更监听器。
     */
    public static void addThemeChangeListener(ThemeChangeListener listener) { ... }

    /**
     * 移除主题变更监听器。
     */
    public static void removeThemeChangeListener(ThemeChangeListener listener) { ... }

    /**
     * 主题变更监听器接口。
     */
    public interface ThemeChangeListener {
        void onThemeChanged(Theme oldTheme, Theme newTheme);
    }
}
```

### 3.3 ElementLightTheme（默认主题）

```java
package org.swelement.core.theme;

/**
 * Element UI 亮色主题，完全复刻 v1 版本的 ElementTheme 色板。
 * 作为框架默认主题。
 */
public class ElementLightTheme implements Theme {

    // 所有颜色、尺寸、字体常量定义...
    // 与现有 ElementTheme 常量值保持一致

    /**
     * 主题构建器，方便基于默认主题快速定制。
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Color primary;
        private Color success;
        private Color warning;
        private Color danger;
        private Color info;
        private int radiusBase;
        private Font fontBase;
        // ... 更多可定制字段

        public Builder primary(Color color) { ... }
        public Builder success(Color color) { ... }
        public Builder warning(Color color) { ... }
        public Builder danger(Color color) { ... }
        public Builder info(Color color) { ... }
        public Builder radiusBase(int radius) { ... }
        public Builder fontBase(Font font) { ... }

        public ElementLightTheme build() { ... }
    }
}
```

### 3.4 主题变更传播机制

1. **ThemeManager** 持有全局监听器列表
2. **AstAbstractComponent** 构造时自动注册监听器
3. 主题切换时，基类调用 `onThemeUpdated()` 钩子并触发 `repaint()`
4. 子类可重写 `onThemeUpdated()` 处理特殊逻辑（如重新计算尺寸）

---

## 4. 核心模块：动画系统

### 4.1 Animator（现有升级）

保持现有 API 不变，增加少量优化：

```java
package org.swelement.core.animation;

public class Animator {

    public Animator(int durationMs, Easing easing, Listener listener) { ... }

    /** 启动动画，从 from 到 to */
    public void go(float from, float to) { ... }

    /** 启动动画，完成后触发回调 */
    public void go(float from, float to, Runnable onComplete) { ... }

    /** 停止动画，onComplete 不触发 */
    public void stop() { ... }

    /** 是否正在运行 */
    public boolean isRunning() { ... }

    /** 获取当前值 */
    public float getCurrentValue() { ... }

    /** 动画帧回调接口 */
    public interface Listener {
        void update(float value);
    }
}
```

### 4.2 Easing（现有保持）

现有接口和实现保持不变，作为函数式接口使用。

### 4.3 AnimationManager（新增）

```java
package org.swelement.core.animation;

import javax.swing.JComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * 动画管理器，统一管理组件的所有动画状态。
 * 提供命名动画注册、获取、驱动能力。
 */
public class AnimationManager {

    // ==================== 内置标准动画名称 ====================

    public static final String HOVER = "hover";
    public static final String FOCUS = "focus";
    public static final String ACTIVE = "active";
    public static final String PRESS = "press";
    public static final String OPEN = "open";
    public static final String CLOSE = "close";

    private final JComponent owner;
    private final Map<String, Animator> animations = new HashMap<>();
    private final Map<String, Float> progress = new HashMap<>();

    public AnimationManager(JComponent owner) {
        this.owner = owner;
    }

    // ==================== 注册与获取 ====================

    /**
     * 注册一个命名动画。
     * @param name 动画名称
     * @param durationMs 动画时长（毫秒）
     * @param easing 缓动函数
     */
    public Animator register(String name, int durationMs, Easing easing) {
        Animator anim = new Animator(durationMs, easing, v -> {
            progress.put(name, v);
            owner.repaint();
        });
        animations.put(name, anim);
        progress.put(name, 0f);
        return anim;
    }

    /**
     * 获取指定名称的动画器。
     */
    public Animator get(String name) {
        return animations.get(name);
    }

    /**
     * 获取指定动画的当前进度值 [0, 1]。
     */
    public float getProgress(String name) {
        Float v = progress.get(name);
        return v != null ? v : 0f;
    }

    /**
     * 检查动画是否已注册。
     */
    public boolean has(String name) {
        return animations.containsKey(name);
    }

    // ==================== 动画驱动 ====================

    /**
     * 驱动动画到 1（进入状态）。
     */
    public void start(String name) {
        Animator anim = animations.get(name);
        if (anim != null) {
            float current = getProgress(name);
            anim.go(current, 1f);
        }
    }

    /**
     * 驱动动画到 0（退出状态）。
     */
    public void stop(String name) {
        Animator anim = animations.get(name);
        if (anim != null) {
            float current = getProgress(name);
            anim.go(current, 0f);
        }
    }

    /**
     * 驱动动画从 from 到 to。
     */
    public void go(String name, float from, float to) {
        Animator anim = animations.get(name);
        if (anim != null) {
            anim.go(from, to);
        }
    }

    /**
     * 立即设置动画进度值（不带动画，直接跳转）。
     */
    public void setProgress(String name, float value) {
        progress.put(name, value);
        Animator anim = animations.get(name);
        if (anim != null) {
            anim.stop();
        }
        owner.repaint();
    }

    /**
     * 停止所有动画。
     */
    public void stopAll() {
        for (Animator anim : animations.values()) {
            anim.stop();
        }
    }

    /**
     * 销毁，停止所有动画并清理资源。
     */
    public void dispose() {
        stopAll();
        animations.clear();
        progress.clear();
    }
}
```

### 4.4 内置标准动画配置

| 动画名 | 时长 | 缓动 | 触发时机 |
|--------|------|------|---------|
| `hover` | 200ms | easeInOut | 鼠标进入/离开 |
| `active` | 120ms | easeInOut | 鼠标按下/释放 |
| `focus` | 200ms | easeInOut | 获得/失去焦点 |

---

## 5. 核心模块：自检框架

### 5.1 SelfCheckBase

```java
package org.swelement.core.check;

import java.awt.Color;

/**
 * 自检基类，提供通用断言工具。
 * 所有核心类和组件的 selfCheck() 均可继承或使用此类工具。
 */
public abstract class SelfCheckBase {

    // ==================== 对比度断言 ====================

    /**
     * 断言前景色与背景色满足 WCAG 2.1 AA 级对比度（≥ 4.5:1）。
     * 仅在 -ea 开启时生效。
     */
    protected void assertContrast(Color fg, Color bg, String where) {
        assertContrast(fg, bg, where, 4.5f);
    }

    /**
     * 断言对比度，可指定最小比例。
     * 非文本元素使用 3.0f（WCAG 1.4.11）。
     */
    protected void assertContrast(Color fg, Color bg, String where, float minRatio) {
        float ratio = contrastRatio(fg, bg);
        assert ratio >= minRatio : "[CONTRAST FAIL " + where + "] ratio="
                + String.format("%.2f", ratio) + " (need >= " + String.format("%.2f", minRatio) + ")"
                + " fg=RGB(" + fg.getRed() + "," + fg.getGreen() + "," + fg.getBlue() + ")"
                + " bg=RGB(" + bg.getRed() + "," + bg.getGreen() + "," + bg.getBlue() + ")";
    }

    // ==================== 浮点断言 ====================

    protected void assertApprox(float expected, float actual, float epsilon, String msg) {
        assert Math.abs(expected - actual) <= epsilon : msg
                + " expected=" + expected + " actual=" + actual + " epsilon=" + epsilon;
    }

    // ==================== 尺寸断言 ====================

    protected void assertDimension(int expected, int actual, String msg) {
        assert expected == actual : msg + " expected=" + expected + " actual=" + actual;
    }

    // ==================== 工具方法 ====================

    /** 计算相对亮度（WCAG 定义） */
    protected float luminance(Color c) { ... }

    /** 计算对比度 */
    protected float contrastRatio(Color a, Color b) { ... }

    /** 线性插值颜色 */
    protected Color lerp(Color a, Color b, float t) { ... }
}
```

---

## 6. 组件基类体系

### 6.1 继承层级总览

```
JComponent (Swing 原生)
    │
    └── AstAbstractComponent (framework 层)
            │
            ├── AstInteractiveComponent  (交互型组件基类)
            │       ├── AstButton
            │       ├── AstCheckbox
            │       ├── AstRadio
            │       ├── AstSwitch
            │       ├── AstSlider
            │       ├── AstInput (部分交互)
            │       └── ...
            │
            ├── AstContainerComponent   (容器型组件基类)
            │       ├── AstCard
            │       ├── AstTabs
            │       ├── AstForm
            │       └── ...
            │
            └── AstDisplayComponent     (展示型组件基类)
                    ├── AstTag
                    ├── AstBadge
                    ├── AstProgress
                    ├── AstAlert
                    ├── AstAvatar
                    ├── AstDivider
                    └── ...
```

### 6.2 AstAbstractComponent — 顶层基类

```java
package org.swelement.framework;

import org.swelement.core.animation.AnimationManager;
import org.swelement.core.theme.Theme;
import org.swelement.core.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Swing Element UI 组件顶层抽象基类。
 * 提供主题绑定、动画管理、绘制辅助、生命周期等通用能力。
 */
public abstract class AstAbstractComponent extends JComponent
        implements ThemeManager.ThemeChangeListener {

    // ==================== 动画管理 ====================

    /** 动画管理器，子类通过 anim 注册和驱动动画 */
    protected final AnimationManager anim = new AnimationManager(this);

    // ==================== 主题 ====================

    /**
     * 获取当前主题。
     * 优先使用组件级主题（未来扩展），否则使用全局主题。
     */
    protected Theme theme() {
        return ThemeManager.getCurrent();
    }

    /**
     * 主题变更钩子，子类可重写处理主题切换逻辑。
     * 默认触发重绘。
     */
    protected void onThemeUpdated(Theme oldTheme, Theme newTheme) {
        repaint();
    }

    @Override
    public final void onThemeChanged(Theme oldTheme, Theme newTheme) {
        onThemeUpdated(oldTheme, newTheme);
    }

    // ==================== 构造与初始化 ====================

    protected AstAbstractComponent() {
        setOpaque(false);
        ThemeManager.addThemeChangeListener(this);
        initComponent();
    }

    /**
     * 组件初始化钩子。
     * 子类在此进行动画注册、属性设置、子组件创建等初始化工作。
     */
    protected void initComponent() {
        // 默认空实现，子类按需重写
    }

    // ==================== 绘制辅助方法 ====================

    /**
     * 创建配置好抗锯齿的 Graphics2D。
     * 使用完毕后必须调用 g2.dispose()。
     */
    protected Graphics2D createGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /**
     * 颜色插值（主题色便捷方法）。
     */
    protected Color lerp(Color a, Color b, float t) {
        if (t <= 0f) return a;
        if (t >= 1f) return b;
        return new Color(
                lerp(a.getRed(), b.getRed(), t),
                lerp(a.getGreen(), b.getGreen(), t),
                lerp(a.getBlue(), b.getBlue(), t),
                lerp(a.getAlpha(), b.getAlpha(), t));
    }

    private int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    /**
     * 绘制圆角矩形背景。
     */
    protected void fillRoundRect(Graphics2D g2, int x, int y, int w, int h, int radius) {
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, radius * 2, radius * 2));
    }

    /**
     * 绘制圆角矩形边框。
     */
    protected void drawRoundRect(Graphics2D g2, int x, int y, int w, int h, int radius) {
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, radius * 2, radius * 2));
    }

    /**
     * 绘制水平居中文本（基线 y 由调用方控制）。
     */
    protected void drawCenteredText(Graphics2D g2, String text, int x, int width, float baselineY) {
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);
        g2.drawString(text, x + (width - textW) / 2f, baselineY);
    }

    // ==================== 尺寸辅助 ====================

    /**
     * 获取基础圆角（从主题读取）。
     */
    protected int radius() {
        return theme().getRadiusBase();
    }

    // ==================== 生命周期 ====================

    @Override
    public void removeNotify() {
        super.removeNotify();
        ThemeManager.removeThemeChangeListener(this);
        anim.dispose();
    }

    // ==================== 自检 ====================

    /**
     * 组件自检方法。
     * 每个组件必须实现，覆盖：尺寸计算、状态切换、对比度、边界条件。
     */
    protected abstract void selfCheck();
}
```

### 6.3 AstInteractiveComponent — 交互组件基类

```java
package org.swelement.framework;

import org.swelement.core.animation.AnimationManager;
import org.swelement.core.theme.Theme;

import java.awt.*;
import java.awt.event.*;

/**
 * 可交互组件基类。
 * 自动管理 hover / active / focus 三种标准交互状态及其动画。
 * 按钮、开关、复选框、单选框等可交互组件继承此类。
 */
public abstract class AstInteractiveComponent extends AstAbstractComponent {

    // ==================== 状态字段 ====================

    private boolean hovering = false;
    private boolean pressing = false;
    private boolean focused = false;

    // ==================== 构造 ====================

    protected AstInteractiveComponent() {
        super();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        // 注册标准交互动画
        anim.register(AnimationManager.HOVER, 200, Easing::easeInOut);
        anim.register(AnimationManager.ACTIVE, 120, Easing::easeInOut);
        anim.register(AnimationManager.FOCUS, 200, Easing::easeInOut);
        // 安装事件监听
        installInteractionListeners();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ==================== 事件监听 ====================

    private void installInteractionListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    hovering = true;
                    anim.start(AnimationManager.HOVER);
                    onHoverChanged(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                pressing = false;
                anim.stop(AnimationManager.HOVER);
                anim.stop(AnimationManager.ACTIVE);
                onHoverChanged(false);
                onActiveChanged(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                    pressing = true;
                    anim.start(AnimationManager.ACTIVE);
                    onActiveChanged(true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressing) {
                    pressing = false;
                    anim.stop(AnimationManager.ACTIVE);
                    onActiveChanged(false);
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (isEnabled()) {
                    focused = true;
                    anim.start(AnimationManager.FOCUS);
                    onFocusChanged(true);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                anim.stop(AnimationManager.FOCUS);
                onFocusChanged(false);
            }
        });
    }

    // ==================== 便捷方法 ====================

    /** 获取 hover 动画进度 [0, 1] */
    protected float hoverProgress() {
        return anim.getProgress(AnimationManager.HOVER);
    }

    /** 获取 active（按下）动画进度 [0, 1] */
    protected float activeProgress() {
        return anim.getProgress(AnimationManager.ACTIVE);
    }

    /** 获取 focus 动画进度 [0, 1] */
    protected float focusProgress() {
        return anim.getProgress(AnimationManager.FOCUS);
    }

    /** 是否处于 hover 状态 */
    protected boolean isHovering() {
        return hovering;
    }

    /** 是否处于按下状态 */
    protected boolean isPressing() {
        return pressing;
    }

    /** 是否处于焦点状态 */
    protected boolean isFocusedFlag() {
        return focused;
    }

    // ==================== 状态变更钩子 ====================

    /** Hover 状态变更，子类按需重写 */
    protected void onHoverChanged(boolean hovering) {}

    /** 按下状态变更，子类按需重写 */
    protected void onActiveChanged(boolean active) {}

    /** 焦点状态变更，子类按需重写 */
    protected void onFocusChanged(boolean focused) {}

    // ==================== 禁用态联动 ====================

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            // 禁用时清除所有交互状态
            hovering = false;
            pressing = false;
            focused = false;
            anim.stop(AnimationManager.HOVER);
            anim.stop(AnimationManager.ACTIVE);
            anim.stop(AnimationManager.FOCUS);
        }
    }
}
```

### 6.4 AstContainerComponent — 容器组件基类

```java
package org.swelement.framework;

import java.awt.*;

/**
 * 容器型组件基类。
 * 适用于包含子组件的复合组件，如 Card、Tabs、Form 等。
 * 提供子组件管理、布局辅助、边框绘制等能力。
 */
public abstract class AstContainerComponent extends AstAbstractComponent {

    private int radius;

    protected AstContainerComponent() {
        super();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        this.radius = theme().getRadiusBase();
    }

    /** 设置圆角半径 */
    public void setRadius(int radius) {
        this.radius = radius;
        repaint();
    }

    /** 获取圆角半径 */
    public int getRadius() {
        return radius;
    }

    @Override
    protected void onThemeUpdated(Theme oldTheme, Theme newTheme) {
        super.onThemeUpdated(oldTheme, newTheme);
        // 主题切换时，如果未手动设置过圆角，更新为新主题的默认值
        this.radius = newTheme.getRadiusBase();
    }

    /**
     * 绘制容器边框和背景（由 paintComponent 调用）。
     * 子类可重写自定义样式。
     */
    protected void paintContainer(Graphics2D g2) {
        Theme t = theme();
        int w = getWidth() - 1;
        int h = getHeight() - 1;

        // 背景
        g2.setColor(t.getFillBlank());
        fillRoundRect(g2, 0, 0, w, h, radius);

        // 边框
        g2.setColor(t.getBorderBase());
        g2.setStroke(new BasicStroke(1f));
        drawRoundRect(g2, 0, 0, w, h, radius);
    }
}
```

### 6.5 AstDisplayComponent — 展示组件基类

```java
package org.swelement.framework;

/**
 * 展示型组件基类。
 * 适用于纯展示、无交互（或弱交互）的组件，如 Tag、Badge、Progress 等。
 * 不自动注册 hover/active/focus 动画，按需手动注册。
 */
public abstract class AstDisplayComponent extends AstAbstractComponent {

    protected AstDisplayComponent() {
        super();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        // 展示组件默认不响应鼠标，光标为默认
        setCursor(Cursor.getDefaultCursor());
    }
}
```

---

## 7. 绘制辅助工具

### 7.1 PaintingHelper

```java
package org.swelement.framework.util;

import java.awt.*;
import java.awt.geom.*;

/**
 * 绘制辅助工具类，提供常用绘制方法。
 * 所有方法为静态无状态，可直接调用。
 */
public final class PaintingHelper {

    private PaintingHelper() {}

    // ==================== 圆角矩形 ====================

    /** 创建圆角矩形 Shape */
    public static RoundRectangle2D roundRect(int x, int y, int w, int h, int radius) {
        return new RoundRectangle2D.Float(x, y, w, h, radius * 2, radius * 2);
    }

    /** 填充圆角矩形 */
    public static void fillRoundRect(Graphics2D g2, int x, int y, int w, int h, int radius) {
        g2.fill(roundRect(x, y, w, h, radius));
    }

    /** 绘制圆角矩形边框 */
    public static void drawRoundRect(Graphics2D g2, int x, int y, int w, int h, int radius) {
        g2.draw(roundRect(x, y, w, h, radius));
    }

    // ==================== 圆形 ====================

    /** 填充圆形 */
    public static void fillCircle(Graphics2D g2, int cx, int cy, int radius) {
        g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    /** 绘制圆形边框 */
    public static void drawCircle(Graphics2D g2, int cx, int cy, int radius) {
        g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    // ==================== 文字绘制 ====================

    /**
     * 在指定区域内水平居中文本绘制。
     * @param baselineY 文字基线 y 坐标
     */
    public static void drawCenteredText(Graphics2D g2, String text, int x, int width, float baselineY) {
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);
        g2.drawString(text, x + (width - textW) / 2f, baselineY);
    }

    /**
     * 在指定矩形内垂直水平居中绘制单行文本。
     */
    public static void drawTextInCenter(Graphics2D g2, String text, int x, int y, int w, int h) {
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);
        float textX = x + (w - textW) / 2f;
        float textY = y + (h - fm.getHeight()) / 2f + fm.getAscent();
        g2.drawString(text, textX, textY);
    }

    // ==================== 发光效果 ====================

    /**
     * 为形状绘制外发光效果。
     * 通过多层描边模拟光晕（纯 JDK 实现，不依赖模糊滤镜）。
     */
    public static void drawGlow(Graphics2D g2, Shape shape, Color color, int size, float alpha) {
        Composite oldComposite = g2.getComposite();
        Stroke oldStroke = g2.getStroke();

        for (int i = size; i >= 1; i--) {
            float layerAlpha = alpha * (1f - (float) i / size) * 0.5f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layerAlpha));
            g2.setStroke(new BasicStroke(i * 2f));
            g2.setColor(color);
            g2.draw(shape);
        }

        g2.setComposite(oldComposite);
        g2.setStroke(oldStroke);
    }

    // ==================== 颜色工具 ====================

    /**
     * 调整颜色透明度。
     */
    public static Color withAlpha(Color color, float alpha) {
        int a = Math.round(255 * Math.max(0, Math.min(1, alpha)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    /**
     * 颜色加深（乘以 factor，factor < 1）。
     */
    public static Color darken(Color color, float factor) {
        return new Color(
                Math.round(color.getRed() * factor),
                Math.round(color.getGreen() * factor),
                Math.round(color.getBlue() * factor),
                color.getAlpha());
    }

    /**
     * 颜色变亮（线性向白色插值，factor 为接近白色的程度）。
     */
    public static Color lighten(Color color, float factor) {
        return new Color(
                Math.round(color.getRed() + (255 - color.getRed()) * factor),
                Math.round(color.getGreen() + (255 - color.getGreen()) * factor),
                Math.round(color.getBlue() + (255 - color.getBlue()) * factor),
                color.getAlpha());
    }

    // ==================== 图标绘制 ====================

    /**
     * 绘制字符图标（使用字体字符作为图标）。
     */
    public static void drawIcon(Graphics2D g2, String iconChar, int x, int y, int size, Color color) {
        Font oldFont = g2.getFont();
        Color oldColor = g2.getColor();

        g2.setFont(oldFont.deriveFont((float) size));
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(iconChar, x, y + fm.getAscent());

        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }
}
```

---

## 8. 组件迁移策略

### 8.1 迁移原则

1. **渐进式**：分阶段迁移，新旧组件可共存
2. **API 兼容**：尽量保持组件 public API 不变，内部实现重构
3. **验证优先**：每迁移一个组件，确保自检和 Demo 均通过
4. **回退安全**：每个阶段可独立回退，不影响其他组件

### 8.2 迁移阶段规划

| 阶段 | 内容 | 涉及组件 | 预计工作量 |
|------|------|---------|-----------|
| **Phase 0** | 框架基础设施搭建 | Theme、ThemeManager、AnimationManager、基类体系、PaintingHelper、自检框架 | 核心 |
| **Phase 1** | 简单展示组件迁移（验证框架） | AstTag、AstBadge、AstProgress、AstAlert、AstAvatar、AstDivider | 6 个 |
| **Phase 2** | 基础交互组件迁移 | AstButton、AstCheckbox、AstRadio、AstSwitch | 4 个 |
| **Phase 3** | 输入类组件迁移 | AstInput、AstInputNumber、AstTextArea | 3 个 |
| **Phase 4** | 选择/导航组件迁移 | AstSelect、AstTabs、AstMenu、AstPagination、AstSlider | 5 个 |
| **Phase 5** | 容器/布局组件迁移 | AstCard、AstContainer、AstCollapse、AstSteps | 4 个 |
| **Phase 6** | 反馈/弹层组件迁移 | AstDialog、AstDrawer、AstMessage、AstMessageBox、AstTooltip、AstPopover、AstLoading | 7 个 |
| **Phase 7** | 数据/高级组件迁移 | AstTable、AstTree、AstDatePicker、AstTimePicker、AstCalendar、AstCascader、AstTransfer、AstCarousel、AstRate、AstTimeline、AstBreadcrumb、AstDropdown | 12 个 |

**总计：约 41 个组件**

### 8.3 迁移模板（以 AstButton 为例）

**迁移前（v1 模式）：**
```java
public class AstButton extends JButton {
    public static final int DEFAULT = 0, PRIMARY = 1, ...;
    private static final Color[] BASE_BG = {...};
    private static final Color[] HOVER_BG = {...};

    private final Animator hoverAnim = new Animator(200, Easing::easeInOut, v -> { hover = v; repaint(); });
    private final Animator activeAnim = new Animator(120, Easing::easeInOut, v -> { active = v; repaint(); });
    private float hover, active;

    public AstButton(String text, int type, boolean plain) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (isEnabled()) hoverAnim.go(hover, 1f); }
            public void mouseExited(MouseEvent e)  { hoverAnim.go(hover, 0f); ... }
            public void mousePressed(MouseEvent e) { if (isEnabled()) activeAnim.go(active, 1f); }
            public void mouseReleased(MouseEvent e){ activeAnim.go(active, 0f); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(...);
        // 大量颜色插值和绘制逻辑...
        g2.dispose();
    }
}
```

**迁移后（v2 模式）：**
```java
public class AstButton extends AstInteractiveComponent {
    public static final int DEFAULT = 0, PRIMARY = 1, ...;

    private int type;
    private boolean plain;

    @Override
    protected void initComponent() {
        super.initComponent();
        // 按钮特有初始化
        this.type = DEFAULT;
        this.plain = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        Theme t = theme();

        // 从主题获取基础色
        Color baseBg = getTypeBg(t, type);
        Color hoverBg = getTypeHoverBg(t, type);
        Color activeBg = getTypeActiveBg(t, type);

        // 使用基类动画进度
        Color bg = lerp(lerp(baseBg, hoverBg, hoverProgress()), activeBg, activeProgress());
        Color fg = getTypeFg(t, type);

        // 使用基类绘制辅助
        int arc = radius() * 2;
        fillRoundRect(g2, 0, 0, getWidth() - 1, getHeight() - 1, arc);
        // ...
        g2.dispose();
    }
}
```

**减少的代码量：**
- 动画声明：减少 ~8 行
- 鼠标监听：减少 ~15 行
- Graphics2D 配置：减少 ~3 行
- 颜色插值工具：减少 ~5 行
- **总计减少约 30% 样板代码**

---

## 9. 扩展性设计

### 9.1 扩展层级

```
┌─────────────────────────────────────────┐
│          Layer 3: 组件装饰器            │  ← 运行时增强
│  (边框、校验状态、加载遮罩、角标等)     │
├─────────────────────────────────────────┤
│          Layer 2: 自定义组件            │  ← 功能扩展
│  (继承基类，开发业务组件)               │
├─────────────────────────────────────────┤
│          Layer 1: 自定义主题            │  ← 视觉定制
│  (颜色、字体、圆角等视觉变量)           │
└─────────────────────────────────────────┘
```

### 9.2 Layer 1: 自定义主题

**方式一：基于默认主题快速修改（Builder 模式）**
```java
Theme indigoTheme = ElementLightTheme.builder()
    .primary(new Color(0x6366F1))       // 主色改为靛蓝
    .success(new Color(0x10B981))       // 成功色改为翠绿
    .warning(new Color(0xF59E0B))       // 警告色改为琥珀
    .danger(new Color(0xEF4444))        // 危险色改为亮红
    .radiusBase(6)                       // 圆角改为 6px
    .fontBase(new Font("SansSerif", Font.PLAIN, 14))
    .build();

ThemeManager.registerTheme(indigoTheme);
ThemeManager.setCurrent("Element Light"); // 切换
```

**方式二：完全自定义主题（实现 Theme 接口）**
```java
public class DarkTheme implements Theme {
    @Override public String getName() { return "Dark"; }
    @Override public Color getPrimary() { return new Color(0x409EFF); }
    @Override public Color getTextPrimary() { return new Color(0xE5EAF3); }
    // ... 实现所有方法
}
```

**方式三：扩展组件级主题变量**
```java
// 在自定义主题中添加组件特有颜色
@Override
public Color getColor(String key) {
    switch (key) {
        case "button.textHover": return new Color(0x...);
        case "input.focusGlow": return new Color(0x...);
        default: return null; // 返回 null 表示使用默认逻辑
    }
}
```

### 9.3 Layer 2: 自定义组件

**简单组件开发步骤：**

1. 选择合适的基类（交互型 / 容器型 / 展示型）
2. 重写 `initComponent()` 注册动画和初始化
3. 重写 `paintComponent()` 实现绘制
4. 重写 `getPreferredSize()` 返回首选尺寸
5. 实现 `selfCheck()` 编写自检
6. 添加 Demo 类验证

**完整示例：自定义评分组件骨架**
```java
public class AstRate extends AstInteractiveComponent {

    private int max = 5;
    private int value = 0;

    @Override
    protected void initComponent() {
        super.initComponent();
        // 注册自定义动画
        anim.register("fill", 300, Easing::easeOut);
    }

    public void setValue(int value) {
        this.value = Math.max(0, Math.min(max, value));
        anim.go("fill", 0f, 1f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        Theme t = theme();
        // 绘制星星...
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(max * 24, 24);
    }

    @Override
    protected void selfCheck() {
        // 自检逻辑...
    }
}
```

### 9.4 Layer 3: 组件装饰器

```java
/**
 * 组件装饰器接口，用于为现有组件动态添加视觉或行为增强。
 */
public interface ComponentDecorator {
    void decorate(JComponent component);
    void undecorate(JComponent component);
}

/**
 * 示例：校验状态装饰器
 */
public class ValidationDecorator implements ComponentDecorator {

    public enum State { SUCCESS, WARNING, ERROR }

    private final State state;
    private final String message;

    public static void decorate(JComponent component, State state, String message) {
        new ValidationDecorator(state, message).decorate(component);
    }

    @Override
    public void decorate(JComponent component) {
        // 为组件边框添加颜色标识
        // 在组件下方添加错误提示文字
        // 支持 Tooltip 展示详情
    }

    @Override
    public void undecorate(JComponent component) {
        // 移除装饰
    }
}
```

---

## 10. 质量保证体系

### 10.1 自检规范

**每个组件必须实现 `selfCheck()`，覆盖以下方面：**

| 检查类别 | 检查内容 | 示例 |
|---------|---------|------|
| **尺寸计算** | 不同尺寸档位、文字长度、有无图标 | 大号按钮比默认高 |
| **状态切换** | 默认/hover/active/disabled/loading | loading 时按钮禁用 |
| **对比度** | 所有状态下文字与背景的 WCAG AA | plain 模式文字与底色 ≥ 4.5:1 |
| **边界条件** | 空文本、极值、异常输入 | 0 页时分页组件不崩溃 |
| **功能逻辑** | 核心算法、事件回调 | 分页窗口生成算法正确 |

### 10.2 对比度自动检查机制

**基类提供统一的颜色收集接口：**

```java
// 在 AstAbstractComponent 中
protected void collectStateColors(ColorCollector collector) {
    // 子类重写，添加各状态的前景/背景色对
}

// ColorCollector 工具类
public class ColorCollector {
    public void add(String stateName, Color foreground, Color background);
    public void add(String stateName, Color foreground, Color background, float minRatio);
}
```

**自检时自动遍历检查：**
```java
// 基类自检方法中统一调用
protected void checkContrastAll() {
    ColorCollector collector = new ColorCollector();
    collectStateColors(collector);
    collector.assertAllPass();
}
```

### 10.3 构建验证流程

```
build.bat
    │
    ├── 步骤 1：编译所有 .java 文件
    │       └── 失败 → BUILD FAILED，输出编译错误
    │
    ├── 步骤 2：运行核心自检
    │       ├── java -ea org.swelement.core.theme.ThemeManager
    │       ├── java -ea org.swelement.core.animation.Animator
    │       ├── java -ea org.swelement.core.animation.Easing
    │       └── ...
    │
    ├── 步骤 3：运行框架自检
    │       ├── java -ea org.swelement.framework.AstAbstractComponent
    │       └── ...
    │
    ├── 步骤 4：运行组件自检
    │       ├── java -ea org.swelement.ui.form.AstButton
    │       ├── java -ea org.swelement.ui.form.AstInput
    │       └── ... (所有组件)
    │
    └── 全部通过 → BUILD & VERIFY OK
```

**批量验证脚本：** `run-checks.bat`

---

## 11. 目录结构重构

### 11.1 重构后目录结构

```
swing-element-ui/
├── src/
│   └── org/swelement/
│       ├── core/                              # 核心引擎层
│       │   ├── theme/                         # 主题系统
│       │   │   ├── Theme.java
│       │   │   ├── ThemeManager.java
│       │   │   └── ElementLightTheme.java
│       │   ├── animation/                     # 动画引擎
│       │   │   ├── Animator.java
│       │   │   ├── Easing.java
│       │   │   └── AnimationManager.java
│       │   ├── popup/                         # 弹层系统
│       │   │   ├── AnimatedPopup.java
│       │   │   ├── PopupPositioner.java
│       │   │   └── GlassPane.java
│       │   └── check/                         # 自检框架
│       │       └── SelfCheckBase.java
│       │
│       ├── framework/                         # 框架基类层 (新增)
│       │   ├── AstAbstractComponent.java
│       │   ├── AstInteractiveComponent.java
│       │   ├── AstContainerComponent.java
│       │   ├── AstDisplayComponent.java
│       │   └── util/
│       │       ├── PaintingHelper.java
│       │       └── ComponentUtils.java
│       │
│       ├── ui/                                # UI 组件层 (按功能分类)
│       │   ├── form/                          # 表单组件
│       │   │   ├── AstButton.java
│       │   │   ├── AstInput.java
│       │   │   ├── AstInputNumber.java
│       │   │   ├── AstCheckbox.java
│       │   │   ├── AstRadio.java
│       │   │   ├── AstSwitch.java
│       │   │   ├── AstSelect.java
│       │   │   ├── AstCascader.java
│       │   │   ├── AstDatePicker.java
│       │   │   ├── AstTimePicker.java
│       │   │   └── AstRate.java
│       │   ├── data/                          # 数据展示
│       │   │   ├── AstTable.java
│       │   │   ├── AstTableColumn.java
│       │   │   ├── AstTableModel.java
│       │   │   ├── AstPagination.java
│       │   │   ├── AstTree.java
│       │   │   └── AstTransfer.java
│       │   ├── navigation/                    # 导航组件
│       │   │   ├── AstMenu.java
│       │   │   ├── AstTabs.java
│       │   │   ├── AstBreadcrumb.java
│       │   │   ├── AstDropdown.java
│       │   │   └── AstSteps.java
│       │   ├── feedback/                      # 反馈组件
│       │   │   ├── AstAlert.java
│       │   │   ├── AstDialog.java
│       │   │   ├── AstDrawer.java
│       │   │   ├── AstMessage.java
│       │   │   ├── AstMessageBox.java
│       │   │   ├── AstLoading.java
│       │   │   ├── AstProgress.java
│       │   │   └── AstPopover.java
│       │   ├── display/                       # 展示组件
│       │   │   ├── AstTag.java
│       │   │   ├── AstBadge.java
│       │   │   ├── Avatar.java
│       │   │   ├── AstCard.java
│       │   │   ├── AstDivider.java
│       │   │   ├── AstIcon.java
│       │   │   ├── AstTimeline.java
│       │   │   └── AstCarousel.java
│       │   └── layout/                        # 布局组件
│       │       ├── AstContainer.java
│       │       ├── AstCollapse.java
│       │       └── AstCalendar.java
│       │
│       └── demo/                              # 演示层
│           ├── FrameworkDemo.java             # 框架特性总览
│           ├── ThemeDemo.java                 # 主题切换演示
│           ├── form/
│           │   └── ...
│           └── ...
│
├── docs/
│   ├── framework/                  # 框架文档 (新增)
│   │   ├── README.md               # 框架概览 + 快速开始
│   │   ├── getting-started.md      # 入门指南
│   │   ├── architecture.md         # 架构设计
│   │   ├── theme-system.md         # 主题系统使用指南
│   │   ├── animation-system.md     # 动画系统使用指南
│   │   ├── custom-component.md     # 自定义组件开发指南
│   │   ├── migration-guide.md      # 从 v1 迁移指南
│   │   └── api/                    # API 参考
│   │       ├── AstAbstractComponent.md
│   │       ├── AstInteractiveComponent.md
│   │       ├── Theme.md
│   │       ├── ThemeManager.md
│   │       └── AnimationManager.md
│   ├── components/                 # 组件文档 (现有保持)
│   └── superpowers/
│       ├── specs/
│       └── plans/
│
├── build.bat                       # 编译脚本
├── run-checks.bat                  # 自检脚本 (新增)
└── README.md
```

### 11.2 迁移顺序

目录重构与组件迁移同步进行：
1. 先创建 `core/` 子模块和 `framework/` 目录
2. 逐步将组件从 `ui/` 根目录移入分类子目录
3. 同步更新 `build.bat` 编译路径
4. 最后清理空目录

---

## 12. 文档体系

### 12.1 文档分层

| 层级 | 文档 | 目标读者 | 更新频率 |
|------|------|---------|---------|
| **入门层** | README.md | 所有人 | 低 |
| | getting-started.md | 新手 | 低 |
| **指南层** | theme-system.md | 所有开发 | 中 |
| | animation-system.md | 所有开发 | 中 |
| | custom-component.md | 组件开发者 | 中 |
| | migration-guide.md | 现有用户 | 低 |
| **参考层** | API 文档 | 所有开发 | 高 |
| | 组件文档 | 所有开发 | 高 |
| **设计层** | architecture.md | 架构师 | 低 |
| | 设计规格文档 | 核心开发者 | 低 |

### 12.2 组件文档模板

每个组件文档统一包含以下章节：

```markdown
# 组件名

## 基础用法
最简代码示例 + 效果说明

## 组件类型
所有类型/变体的展示和代码

## 尺寸
大/中/小尺寸示例

## 状态
默认 / hover / active / disabled / loading / plain / text 等

## 自定义主题
组件级主题变量说明

## API 参考
### Props / 属性
所有 public setter/getter 列表

### Events / 事件
所有事件监听器列表

### Methods / 方法
所有 public 方法列表

## 无障碍支持
- 键盘操作
- 对比度
- 屏幕阅读器
```

---

## 13. 构建与发布

### 13.1 构建脚本

**保持零依赖，使用纯 javac 编译，升级构建脚本：**

```
build.bat
├── 查找 javac（支持 JDK 8+）
├── 收集源文件列表（遍历 src 下所有 .java）
├── 编译（--release 8，UTF-8 编码）
├── 输出编译结果
└── 可选：运行自检
```

**新增 `build-jar.bat`：** 编译后打包为 JAR 文件

### 13.2 版本号规范

采用语义化版本号 `MAJOR.MINOR.PATCH`：
- **MAJOR**：不兼容的 API 变更（v2 框架化即为此类）
- **MINOR**：新增功能，向下兼容
- **PATCH**：Bug 修复，向下兼容

### 13.3 发布产物

| 产物 | 说明 |
|------|------|
| `swing-element-ui-2.0.0.jar` | 核心库 JAR |
| `swing-element-ui-2.0.0-sources.jar` | 源码 JAR |
| `swing-element-ui-2.0.0-javadoc.jar` | Javadoc JAR |

---

## 14. 风险与权衡

### 14.1 风险评估

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| 迁移周期长 | 高 | 中 | 分阶段迁移，每阶段独立验证 |
| 迁移引入 Bug | 高 | 中 | 严格自检 + Demo 验证 + 逐步迁移 |
| 性能下降 | 中 | 低 | 基类方法尽量精简，避免每层叠加开销 |
| 学习成本 | 中 | 中 | 完善文档 + 代码示例 + 渐进式采用 |
| 向后兼容性 | 高 | 低 | 保持组件 public API 不变，内部重构 |

### 14.2 关键权衡

| 决策 | 优点 | 缺点 | 选择理由 |
|------|------|------|---------|
| 继承基类 vs 组合 | 代码复用率高、API 统一 | 灵活性受限 | 对 Swing 组件库来说，继承是更自然的模式 |
| 动态主题 vs 静态常量 | 支持运行时换肤、扩展性强 | 轻微性能开销 | 框架级必备能力，开销可忽略 |
| 零依赖 vs Maven | 极简、无构建门槛 | 依赖管理、发布不便 | 保持项目原有定位 |
| 全量迁移 vs 渐进 | 一步到位、架构干净 | 风险高、周期长 | 渐进式更安全，可随时暂停 |

### 14.3 性能考量

1. **主题查找**：ThemeManager.getCurrent() 为简单变量读取，开销可忽略
2. **动画管理**：AnimationManager 使用 HashMap，查找 O(1)
3. **绘制辅助**：PaintingHelper 为静态方法，无额外对象创建
4. **监听器数量**：基类自动注册的监听器数量控制在 3 个以内（鼠标、焦点、主题）
5. **主题变更**：全局通知，组件按需重绘，不强制重新布局

---

## 附录 A：API 变化对照（v1 → v2）

### 保持不变的 API
- 所有组件的构造函数签名
- 所有组件的 public 方法（getText/setText/setEnabled 等）
- 组件类型常量（DEFAULT/PRIMARY/SUCCESS 等）
- 尺寸常量（SIZE_LARGE/SIZE_DEFAULT/SIZE_SMALL）

### 新增的 API
- `ThemeManager` 类（主题管理）
- `Theme` 接口（主题定义）
- `AnimationManager` 类（动画管理）
- 基类 `AstAbstractComponent` / `AstInteractiveComponent` 等
- `PaintingHelper` 工具类

### 内部变化（不影响使用）
- 组件内部不再直接引用 `ElementTheme` 静态常量
- 组件内部动画通过 `anim` 管理器统一管理
- 组件继承体系从 Swing 原生类改为框架基类

---

## 附录 B：自检清单

### 框架级自检
- [ ] ThemeManager 注册/切换/通知 正常
- [ ] AnimationManager 注册/驱动/停止 正常
- [ ] 基类主题变更自动重绘
- [ ] 基类交互事件正确驱动动画
- [ ] PaintingHelper 所有方法正确

### 组件级自检（每个组件必查）
- [ ] 默认状态对比度 ≥ 4.5:1
- [ ] Hover 状态对比度 ≥ 4.5:1
- [ ] Active 状态对比度 ≥ 4.5:1
- [ ] Disabled 状态对比度 ≥ 4.5:1
- [ ] Plain 模式对比度 ≥ 4.5:1
- [ ] 尺寸计算正确（各尺寸档位）
- [ ] 状态切换正常（各状态间过渡）
- [ ] 边界条件处理（空值、极值）
- [ ] 禁用态联动正确
