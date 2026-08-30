# Swing Element UI Framework

> Element UI 风格的 Java Swing 组件框架（JDK 8，零依赖）

## 快速开始

30 秒上手：引入、初始化、创建第一个组件

```java
import org.swelement.framework.AstInteractiveComponent;
import org.swelement.core.theme.Theme;
import javax.swing.*;
import java.awt.*;

public class HelloButton extends AstInteractiveComponent {
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = createGraphics(g);
        Theme t = theme();

        Color bg = lerp(t.getFillBlank(), t.getPrimary(), hoverProgress());
        Color fg = hoverProgress() > 0.5f ? Color.WHITE : t.getTextRegular();

        g2.setColor(bg);
        fillRoundRect(g2, 0, 0, getWidth() - 1, getHeight() - 1, radius());

        g2.setColor(fg);
        g2.setFont(t.getFontBase());
        drawCenteredText(g2, "Hello", 0, getWidth(), getHeight() / 2f + g2.getFontMetrics().getAscent() / 2f);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, 36);
    }

    @Override
    protected void selfCheck() { }
}
```

## 核心特性

- **动态主题系统** - 运行时切换主题，支持自定义主题，组件自动响应主题变更
- **动画管理器** - 统一管理组件动画，内置 hover/active/focus 标准动画
- **组件基类体系** - 四层继承结构，大幅减少样板代码
- **绘制工具集** - 圆角、发光、颜色工具等常用绘制方法
- **自检框架** - WCAG 对比度断言，保证可访问性
- **零依赖** - 纯 JDK 8 标准库，无需任何外部依赖

## 架构总览

```
┌─────────────────────────────────────────────────────┐
│                  业务组件层                           │
│  (Button / Switch / Input / Card / ...)             │
├─────────────────────────────────────────────────────┤
│                  框架基类层                           │
│  AstInteractiveComponent                             │
│  AstContainerComponent                               │
│  AstDisplayComponent                                 │
│  AstAbstractComponent  ← 主题绑定 + 动画 + 绘制辅助   │
├─────────────────────────────────────────────────────┤
│                  核心能力层                           │
│  Theme / ThemeManager  (主题系统)                    │
│  Animator / Easing / AnimationManager  (动画引擎)   │
│  SelfCheckBase  (自检框架)                           │
│  PaintingHelper  (绘制工具)                          │
└─────────────────────────────────────────────────────┘
```

## 模块一览

| 模块 | 包路径 | 说明 |
|------|--------|------|
| 主题系统 | `org.swelement.core.theme` | Theme 接口、ThemeManager、ElementLightTheme |
| 动画引擎 | `org.swelement.core` | Animator、Easing、AnimationManager |
| 自检框架 | `org.swelement.core` | SelfCheckBase（对比度断言、尺寸断言等） |
| 组件基类 | `org.swelement.framework` | AstAbstractComponent 等 4 个基类 |
| 绘制工具 | `org.swelement.framework.util` | PaintingHelper（圆角、发光、颜色工具等） |

## 下一步

- [入门指南](getting-started.md) - 环境搭建、第一个组件、常用概念速查
- [主题系统](theme-system.md) - Theme API、自定义主题、主题切换
- [动画系统](animation-system.md) - Easing 缓动、Animator、AnimationManager
- [自定义组件开发](custom-component.md) - 基类选择、开发步骤、完整示例、绘制规范
