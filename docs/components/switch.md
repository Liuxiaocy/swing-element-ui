# Switch 开关

表示两种相互对立的状态间的切换。

## 基本用法

基础的开关用法。

![基本用法](../screenshots/switch-default.png)

```java
import org.swelement.ui.AstSwitch;

// 创建开关
AstSwitch switch1 = new AstSwitch();
```

## 禁用状态

禁用状态的开关。

![禁用状态](../screenshots/switch-disabled.png)

```java
// 禁用开关
Switch disabledSwitch = new Switch();
disabledSwitch.setEnabled(false);
```

## Switch 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| — | — | — | — | — |

## Switch 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| isSelected | 获取开关状态 | — | boolean |
| setSelected | 设置开关状态 | boolean b | void |
