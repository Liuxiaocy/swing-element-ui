# 主题系统

## 概述

框架的主题系统基于 `Theme` 接口和 `ThemeManager`，支持：

- 运行时切换主题
- 自定义主题（完全实现或基于默认主题修改）
- 主题变更自动通知，组件自动重绘
- 组件级主题变量扩展（通过 `getColor` / `getFont` / `getSize` 扩展方法）

## 使用主题

### 获取当前主题

```java
import org.swelement.core.theme.Theme;
import org.swelement.core.theme.ThemeManager;

Theme theme = ThemeManager.getCurrent();
Color primary = theme.getPrimary();
Font baseFont = theme.getFontBase();
int radius = theme.getRadiusBase();
```

> 注意：调用 `getCurrent()` 前必须至少注册过一个主题，否则会抛出 `IllegalStateException`。框架基类 `AstAbstractComponent` 会在构造时自动调用 `ensureDefaultTheme()` 确保默认主题可用。

### 切换主题

```java
ThemeManager.setCurrent("element-light");
```

切换主题后，所有注册了主题监听的组件会自动收到通知并重绘。

> 注意：如果切换到与当前同名的主题，不会触发变更通知。

### 注册主题

```java
Theme myCustomTheme = new MyCustomTheme();
ThemeManager.registerTheme(myCustomTheme);
```

第一个注册的主题会成为当前默认主题。

### 获取可用主题列表

```java
import java.util.List;

List<String> themeNames = ThemeManager.getAvailableThemes();
```

返回按注册顺序排列的主题名称列表。

### 确保默认主题

```java
ThemeManager.ensureDefaultTheme();
```

如果当前没有注册任何主题，自动注册 `ElementLightTheme` 作为默认主题。框架基类已自动调用此方法，通常不需要手动调用。

### 主题变更监听

```java
ThemeManager.addThemeChangeListener(new ThemeManager.ThemeChangeListener() {
    @Override
    public void onThemeChanged(Theme oldTheme, Theme newTheme) {
        // 处理主题变更
        System.out.println("主题从 " + oldTheme.getName() + " 切换到 " + newTheme.getName());
    }
});
```

移除监听器：

```java
ThemeManager.removeThemeChangeListener(listener);
```

> 监听器异常隔离：单个监听器抛出异常不会影响其他监听器的执行。

## 内置主题

### Element Light（默认）

Element UI 标准亮色主题，主题名称为 `element-light`。

#### 语义色

| 名称 | 色值 | 说明 |
|------|------|------|
| Primary | `#409EFF` | 主色 |
| Success | `#67C23A` | 成功色 |
| Warning | `#E6A23C` | 警告色 |
| Danger | `#F56C6C` | 危险/错误色 |
| Info | `#909399` | 信息色 |

#### 文字色

| 名称 | 色值 | 说明 |
|------|------|------|
| Text Primary | `#303133` | 主要文字（最深） |
| Text Regular | `#606266` | 常规文字 |
| Text Secondary | `#909399` | 次要文字 |
| Text Placeholder | `#C0C4CC` | 占位符文字 |
| Text Disabled | `#C0C4CC` | 禁用文字 |

#### 边框色

| 名称 | 色值 | 说明 |
|------|------|------|
| Border Base | `#DCDFE6` | 基础边框 |
| Border Light | `#E4E7ED` | 浅边框 |
| Border Lighter | `#EBEEF5` | 更浅边框 |

#### 填充色

| 名称 | 色值 | 说明 |
|------|------|------|
| Fill Blank | `#FFFFFF` | 纯白 |
| Fill Base | `#F5F7FA` | 浅灰底 |
| Fill Light | `#FAFAFA` | 更浅填充 |

#### 圆角

| 名称 | 值 | 说明 |
|------|-----|------|
| Radius Small | 2px | 小圆角 |
| Radius Base | 4px | 基础圆角 |
| Radius Large | 8px | 大圆角 |

#### 字体

| 名称 | 字体 | 大小 | 样式 |
|------|------|------|------|
| Font Small | Microsoft YaHei | 12px | PLAIN |
| Font Base | Microsoft YaHei | 14px | PLAIN |
| Font Large | Microsoft YaHei | 16px | PLAIN |

## 自定义主题

### 方式一：完全自定义（实现 Theme 接口）

适用于需要完全控制所有主题变量的场景。

```java
import org.swelement.core.theme.Theme;
import java.awt.Color;
import java.awt.Font;

public class MyTheme implements Theme {

    @Override
    public String getName() {
        return "my-theme";
    }

    // 语义色
    @Override
    public Color getPrimary() { return new Color(0x6366F1); }
    @Override
    public Color getSuccess() { return new Color(0x10B981); }
    @Override
    public Color getWarning() { return new Color(0xF59E0B); }
    @Override
    public Color getDanger() { return new Color(0xEF4444); }
    @Override
    public Color getInfo() { return new Color(0x6B7280); }

    // 文字色
    @Override
    public Color getTextPrimary() { return new Color(0x111827); }
    @Override
    public Color getTextRegular() { return new Color(0x374151); }
    @Override
    public Color getTextSecondary() { return new Color(0x6B7280); }
    @Override
    public Color getTextPlaceholder() { return new Color(0x9CA3AF); }
    @Override
    public Color getTextDisabled() { return new Color(0xD1D5DB); }

    // 边框色
    @Override
    public Color getBorderBase() { return new Color(0xE5E7EB); }
    @Override
    public Color getBorderLight() { return new Color(0xF3F4F6); }
    @Override
    public Color getBorderLighter() { return new Color(0xF9FAFB); }

    // 填充色
    @Override
    public Color getFillBlank() { return Color.WHITE; }
    @Override
    public Color getFillBase() { return new Color(0xF9FAFB); }
    @Override
    public Color getFillLight() { return new Color(0xF3F4F6); }

    // 圆角
    @Override
    public int getRadiusSmall() { return 2; }
    @Override
    public int getRadiusBase() { return 6; }
    @Override
    public int getRadiusLarge() { return 12; }

    // 字体
    @Override
    public Font getFontSmall() { return new Font("Dialog", Font.PLAIN, 12); }
    @Override
    public Font getFontBase() { return new Font("Dialog", Font.PLAIN, 14); }
    @Override
    public Font getFontLarge() { return new Font("Dialog", Font.PLAIN, 16); }

    // 扩展方法 — 未命中返回 null / -1
    @Override
    public Color getColor(String key) { return null; }
    @Override
    public Font getFont(String key) { return null; }
    @Override
    public int getSize(String key) { return -1; }
}
```

### 方式二：扩展 ElementLightTheme（推荐用于小幅定制）

继承 `ElementLightTheme` 并重写需要修改的方法，其余继承默认值。

```java
import org.swelement.core.theme.ElementLightTheme;
import java.awt.Color;

public class IndigoTheme extends ElementLightTheme {

    @Override
    public String getName() {
        return "indigo";
    }

    @Override
    public Color getPrimary() {
        return new Color(0x6366F1);
    }

    @Override
    public int getRadiusBase() {
        return 8; // 更大的圆角
    }
}
```

注册并使用：

```java
ThemeManager.registerTheme(new IndigoTheme());
ThemeManager.setCurrent("indigo");
```

## Theme API 参考

### 语义色

| 方法 | 说明 |
|------|------|
| `getPrimary()` | 主色 |
| `getSuccess()` | 成功色 |
| `getWarning()` | 警告色 |
| `getDanger()` | 危险/错误色 |
| `getInfo()` | 信息色 |

### 文字色

| 方法 | 说明 |
|------|------|
| `getTextPrimary()` | 主要文字（最深） |
| `getTextRegular()` | 常规文字 |
| `getTextSecondary()` | 次要文字 |
| `getTextPlaceholder()` | 占位符文字 |
| `getTextDisabled()` | 禁用文字 |

### 边框色

| 方法 | 说明 |
|------|------|
| `getBorderBase()` | 基础边框 |
| `getBorderLight()` | 浅边框 |
| `getBorderLighter()` | 更浅边框 |

### 填充色

| 方法 | 说明 |
|------|------|
| `getFillBlank()` | 纯白 |
| `getFillBase()` | 浅灰底 |
| `getFillLight()` | 更浅填充 |

### 尺寸（圆角）

| 方法 | 说明 |
|------|------|
| `getRadiusSmall()` | 小圆角 |
| `getRadiusBase()` | 基础圆角 |
| `getRadiusLarge()` | 大圆角 |

### 字体

| 方法 | 说明 |
|------|------|
| `getFontSmall()` | 小字体 |
| `getFontBase()` | 基础字体 |
| `getFontLarge()` | 大字体 |

### 扩展方法

| 方法 | 说明 | 未命中返回 |
|------|------|-----------|
| `getColor(String key)` | 按键名获取颜色 | `null` |
| `getFont(String key)` | 按键名获取字体 | `null` |
| `getSize(String key)` | 按键名获取尺寸 | `-1` |

ElementLightTheme 内置的扩展键名：

- 颜色：`primary`、`success`、`warning`、`danger`、`info`、`textPrimary`、`textRegular`、`textSecondary`、`textPlaceholder`、`textDisabled`、`borderBase`、`borderLight`、`borderLighter`、`fillBlank`、`fillBase`、`fillLight`
- 字体：`small`、`base`、`large`
- 尺寸：`radiusSmall`、`radiusBase`、`radiusLarge`

## 框架组件中的主题

所有框架基类自动处理主题：

- `AstAbstractComponent` 自动注册主题监听
- 主题变更时自动调用 `repaint()`
- 子类可重写 `onThemeUpdated(Theme oldTheme, Theme newTheme)` 处理特殊逻辑
- 通过 `theme()` 方法获取当前主题
- 组件移除时（`removeNotify`）自动注销主题监听，无内存泄漏

```java
@Override
protected void onThemeUpdated(Theme oldTheme, Theme newTheme) {
    super.onThemeUpdated(oldTheme, newTheme); // 必须调用，否则不会自动重绘
    // 自定义主题变更逻辑
    updateMyCustomProperty(newTheme);
}
```

## 最佳实践

1. **始终从主题获取颜色**，不要硬编码颜色值
2. **使用语义色**（`getPrimary()` / `getSuccess()` 等）而不是具体颜色名，便于主题切换
3. **自定义主题建议继承 ElementLightTheme**，只修改需要变化的部分，减少维护成本
4. **主题切换在 EDT 中执行**，确保线程安全
5. **组件内不要缓存主题引用**，每次绘制都通过 `theme()` 获取最新主题
6. **使用扩展方法传递自定义变量**，通过 `getColor` / `getFont` / `getSize` 扩展主题变量而不修改接口
