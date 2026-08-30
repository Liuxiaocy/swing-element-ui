# 入门指南

## 环境要求

- JDK 8 或更高版本
- 零外部依赖（纯 JDK 标准库）

## 获取框架

当前框架以源码形式提供，需要自行编译。以下是编译方式：

### 方式一：命令行编译

```bash
# 进入项目根目录
cd swing-element-ui

# 创建输出目录
mkdir out

# 编译所有源码
javac -d out -sourcepath src src/org/swelement/core/*.java src/org/swelement/core/theme/*.java src/org/swelement/framework/*.java src/org/swelement/framework/util/*.java
```

### 方式二：IDE 导入

使用 IntelliJ IDEA 或 Eclipse 等 IDE：

1. 打开项目，将 `src` 目录标记为源码根目录
2. 确保 JDK 版本设置为 1.8 或更高
3. 编译项目

## 第一个组件

让我们创建一个继承 `AstInteractiveComponent` 的自定义按钮，体验框架的核心能力。

### 完整代码示例

```java
import org.swelement.framework.AstInteractiveComponent;
import org.swelement.core.theme.Theme;
import javax.swing.*;
import java.awt.*;

public class MyFirstButton extends AstInteractiveComponent {

    private String text = "Click Me";

    @Override
    protected void paintComponent(Graphics g) {
        // 1. 创建带抗锯齿的 Graphics2D（必须）
        Graphics2D g2 = createGraphics(g);
        Theme t = theme();

        // 2. 获取动画进度，用于颜色插值
        float hover = hoverProgress();
        float active = activeProgress();

        // 3. 计算背景色：默认 → hover → active 三级插值
        Color baseColor = isEnabled() ? t.getFillBlank() : t.getFillBase();
        Color hoverColor = isEnabled() ? t.getPrimary() : t.getFillBase();
        Color activeColor = isEnabled() ? darkenForDemo(t.getPrimary()) : t.getFillBase();

        Color bg = lerp(lerp(baseColor, hoverColor, hover), activeColor, active);

        // 4. 计算文字颜色
        Color fg;
        if (!isEnabled()) {
            fg = t.getTextDisabled();
        } else if (hover > 0.5f) {
            fg = Color.WHITE;
        } else {
            fg = t.getTextRegular();
        }

        // 5. 绘制圆角背景
        g2.setColor(bg);
        fillRoundRect(g2, 0, 0, getWidth() - 1, getHeight() - 1, radius());

        // 6. 绘制居中文本
        g2.setColor(fg);
        g2.setFont(t.getFontBase());
        FontMetrics fm = g2.getFontMetrics();
        float baselineY = getHeight() / 2f + fm.getAscent() / 2f - fm.getDescent();
        drawCenteredText(g2, text, 0, getWidth(), baselineY);

        // 7. 释放 Graphics2D 资源（必须）
        g2.dispose();
    }

    private Color darkenForDemo(Color c) {
        // 简单变暗示例，实际项目中建议使用 PaintingHelper.darken()
        int r = Math.max(0, c.getRed() - 30);
        int g = Math.max(0, c.getGreen() - 30);
        int b = Math.max(0, c.getBlue() - 30);
        return new Color(r, g, b, c.getAlpha());
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, 36);
    }

    public String getText() { return text; }
    public void setText(String text) {
        this.text = text;
        repaint();
    }

    @Override
    protected void selfCheck() {
        // 自检逻辑，详见自定义组件开发指南
    }
}
```

### 运行组件

```java
import javax.swing.*;

public class Demo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("My First Button");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            MyFirstButton button = new MyFirstButton();
            frame.add(button);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
```

## 运行 Demo

项目中包含框架演示程序，编译后可运行：

```bash
# 编译后运行
java -cp out org.swelement.demo.FrameworkDemo
```

## 常用概念速查表

| 概念 | 说明 | 位置 |
|------|------|------|
| `theme()` | 获取当前主题实例 | AstAbstractComponent |
| `anim` | AnimationManager 实例，管理所有命名动画 | AstAbstractComponent |
| `createGraphics(g)` | 创建带抗锯齿的 Graphics2D 副本 | AstAbstractComponent |
| `lerp(a, b, t)` | RGBA 四通道颜色线性插值 | AstAbstractComponent |
| `fillRoundRect(g2,x,y,w,h,r)` | 填充圆角矩形 | AstAbstractComponent |
| `drawRoundRect(g2,x,y,w,h,r)` | 绘制圆角矩形边框 | AstAbstractComponent |
| `drawCenteredText(g2,text,x,w,baselineY)` | 水平居中文本绘制 | AstAbstractComponent |
| `radius()` | 获取主题基础圆角半径 | AstAbstractComponent |
| `hoverProgress()` | hover 动画进度 [0, 1] | AstInteractiveComponent |
| `activeProgress()` | active 动画进度 [0, 1] | AstInteractiveComponent |
| `focusProgress()` | focus 动画进度 [0, 1] | AstInteractiveComponent |
| `isHovering()` | 是否处于悬停状态 | AstInteractiveComponent |
| `isPressing()` | 是否处于按下状态 | AstInteractiveComponent |
| `isFocusedFlag()` | 是否处于焦点状态 | AstInteractiveComponent |
| `paintContainer(g2)` | 绘制标准容器外观（背景+边框） | AstContainerComponent |
