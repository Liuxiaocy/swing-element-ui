# TimePicker 时间选择器

当用户需要输入时间（时/分/秒）时使用，点击触发框弹出三列可滚动选择面板。

## 基本用法

默认包含「秒」列，显示 `HH:mm:ss`；传入 `false` 可关闭秒列，只选 `HH:mm`。

```java
import org.swelement.ui.AstTimePicker;

// 创建时间选择器（默认含秒列）
AstTimePicker tp = new AstTimePicker();
tp.setTime(14, 30, 0);
int[] t = tp.getTime();                       // {14, 30, 0}

// 关闭秒列，只选 时:分（setTime 仍需传秒，仅影响显示与弹层列数）
AstTimePicker tp2 = new AstTimePicker(false);
tp2.setTime(9, 5, 0);

// 选择变化监听
tp.setTimeChangeListener(hms -> System.out.println(hms[0] + ":" + hms[1] + ":" + hms[2]));
```

非法取值会抛 `IllegalArgumentException`：`setTime(24,0,0)`、`setTime(-1,0,0)`、`setTime(0,60,0)`、`setTime(0,0,60)` 均非法。

## 区间模式（固定时间范围）

开启区间模式后，触发框显示 `start ~ end`，弹层左右并排两块面板（起始 / 结束）：先选起始时间，再选结束时间。

**结束时间必须严格晚于开始时间**；一旦起始选定，结束面板中 ≤ 起始的备选项会按整时间戳级联置灰（时/分/秒三列联动）且不可点选，从交互上杜绝「结束早于开始」。

```java
import org.swelement.ui.AstTimePicker;

AstTimePicker rp = new AstTimePicker();
rp.setRangeMode(true);                        // 开启区间模式
rp.setTimeRange(9, 0, 0, 14, 30, 0);         // 结束必须严格晚于开始，否则抛 IllegalArgumentException
int[][] range = rp.getTimeRange();            // {{9,0,0}, {14,30,0}}
boolean isRange = rp.isRangeMode();
```

区间模式下 `getTime()` 不可用（抛 `IllegalStateException`），请改用 `getTimeRange()`；非区间模式下调用 `getTimeRange()` 同样抛 `IllegalStateException`。

## 禁用项说明

结束面板中被置灰的项使用静音灰渲染，并在选择时直接拦截（不会更新已选值）。受 WCAG 2.1 对 inactive 组件的对比度豁免，禁用项不做 AA 对比度断言，其余文字状态（触发框文字、面板项文字）仍满足 AA。

## TimePicker 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| showSeconds | 是否显示秒列 | boolean | — | true |
| rangeMode | 是否区间模式 | boolean | — | false |

## TimePicker 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| setTime | 设置时间（非区间模式） | int h, int m, int s | void |
| getTime | 获取时间（非区间模式；区间模式抛 `IllegalStateException`） | — | int[3] |
| setTimeChangeListener | 设置选择变化监听 | Consumer\<int[]\> | void |
| setRangeMode | 开启 / 关闭区间模式 | boolean on | void |
| isRangeMode | 是否处于区间模式 | — | boolean |
| setTimeRange | 设置区间起止（end 必须严格晚于 start，否则抛 `IllegalArgumentException`） | int sh,int sm,int ss,int eh,int em,int es | void |
| getTimeRange | 获取区间起止（非区间模式抛 `IllegalStateException`） | — | int[2][3] |
| showPicker / hidePicker / toggle | 弹出 / 收起 / 切换面板 | — | void |
| isOpen | 面板是否展开 | — | boolean |
