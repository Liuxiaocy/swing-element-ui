# swing-element-ui

Element UI 风格的 Java Swing 组件库（JDK 8，零依赖）。

## 构建

```
.\build.bat        # 编译到 out/
```

## 运行 Demo

```
java -cp out org.swelement.demo.ButtonDemo
java -cp out org.swelement.demo.InputDemo
java -cp out org.swelement.demo.CheckboxDemo
java -cp out org.swelement.demo.RadioDemo
java -cp out org.swelement.demo.SwitchDemo
java -cp out org.swelement.demo.SliderDemo
```

## 核心自检

```
java -ea -cp out org.swelement.core.Easing
java -ea -cp out org.swelement.core.ElementTheme
java -ea -cp out org.swelement.core.Animator
```

## 设计

见 `docs/superpowers/specs/2026-08-19-swing-element-ui-design.md`