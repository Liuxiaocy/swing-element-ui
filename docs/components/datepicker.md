# DatePicker 日期选择器

当用户需要输入日期时使用，点击触发框弹出日历卡片，支持月份切换、今日高亮、选中日期；开启区间模式后可选择一段日期范围。

## 基本用法

默认创建一个**未选中**的空选择器（表单场景下不应默认预填为今天）。`setDate` 设置日期，`getDate` 取回，`setDateChangeListener` 监听选择变化。

```java
import org.swelement.ui.AstDatePicker;
import java.time.LocalDate;

// 创建日期选择器（默认空，不预填今天）
AstDatePicker dp = new AstDatePicker();
dp.setDate(LocalDate.of(2026, 8, 21));
LocalDate d = dp.getDate();                       // 2026-08-21

// 选择变化监听
dp.setDateChangeListener(date -> System.out.println("选中: " + date));

// 表单值存取（ISO 格式 YYYY-MM-DD）
String formStr = dp.getFormValue();               // "2026-08-21"
dp.setFormValue("2026-09-01");

// 清空
dp.clear();
```

非法取值会抛 `IllegalArgumentException`：`setDate(null)` 非法。

## 区间模式（日期范围选择）

开启区间模式后，触发框显示 `start ~ end`（未选侧显示 placeholder），弹层左右并排两块日历面板（左=起始月，右=起始月+1），各自可独立翻月。

**选取为「点击-点击」式**：第一次点击设 start，第二次点击设 end（若 end 早于 start 会自动交换保证 start≤end），两者都选后再点开启新一轮。

```java
import org.swelement.ui.AstDatePicker;
import java.time.LocalDate;

AstDatePicker rp = new AstDatePicker();
rp.setRangeMode(true);                            // 开启区间模式
rp.setDateRange(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 20));  // end<start 自动交换
LocalDate[] range = rp.getDateRange();            // {2026-08-05, 2026-08-20}
boolean isRange = rp.isRangeMode();

// 区间表单值形如 "start~end"，仅选了 start 时为 "start~"
String rv = rp.getFormValue();                    // "2026-08-05~2026-08-20"
rp.setFormValue("2026-08-01~2026-08-10");
```

区间模式下 `getDate()` 不可用（抛 `IllegalStateException`），请改用 `getDateRange()`；非区间模式下调用 `getDateRange()` 同样抛 `IllegalStateException`。`setDateRange` 传入 null 抛 `IllegalArgumentException`。

## 联动高亮说明

开启区间模式并选定 start 后，鼠标在日历上移动会**预览** `[start, hover]` 区间；选定两端后高亮固定。高亮分三档：

- **端点**（start / end）：实心 PRIMARY 底 + 白字（Element 标准配色，跳过 AA 对比度断言）。
- **区间内部**：浅 PRIMARY 底纹 `lerp(WHITE, PRIMARY, 0.10)` + 深色文字（`TEXT_PRIMARY`，做 WCAG 2.1 AA 断言）。
- **今日**：仅描一圈 PRIMARY 边框，不填充。

其他月份日期与 disable 态使用静音灰（WCAG inactive 豁免，不做 AA 断言）。

## AstDatePicker 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| rangeMode | 是否区间模式 | boolean | — | false |
| placeholder | 触发框空态文字 | String | — | "选择日期" |
| size tier | 尺寸档位 | int | SIZE_LARGE / SIZE_DEFAULT / SIZE_SMALL | SIZE_DEFAULT |

## AstDatePicker 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| setDate | 设置日期（非区间模式；null 抛 `IllegalArgumentException`） | LocalDate date | void |
| getDate | 获取日期（非区间模式；区间模式抛 `IllegalStateException`） | — | LocalDate |
| setDateChangeListener | 设置选择变化监听 | Consumer\<LocalDate\> | void |
| clear | 清空选择（单值清 selectedDate；区间清 start/end） | — | void |
| setRangeMode | 开启 / 关闭区间模式 | boolean on | void |
| isRangeMode | 是否处于区间模式 | — | boolean |
| setDateRange | 设置区间起止（end<start 自动交换；null 抛 `IllegalArgumentException`；非区间抛 `IllegalStateException`） | LocalDate start, LocalDate end | void |
| getDateRange | 获取区间起止（非区间模式抛 `IllegalStateException`） | — | LocalDate[2] |
| setPlaceholder | 设置空态文字 | String s | void |
| setSize | 设置尺寸档位（0/1/2 = LARGE/DEFAULT/SMALL） | int t | void |
| showDatePicker / hideDatePicker / toggle | 弹出 / 收起 / 切换面板 | — | void |
| isOpen | 面板是否展开 | — | boolean |
| getFormValue / setFormValue | 表单值存取（区间形如 `start~end`） | String v | String / void |
