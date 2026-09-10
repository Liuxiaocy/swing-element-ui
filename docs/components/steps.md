# Steps 步骤条

引导用户按照流程完成任务的分步导航条。

## 基本用法（横向）

基础的横向步骤条。未显式设置状态时，节点状态由 `current` 推导：`i < current` 已完成、`i == current` 进行中、`i > current` 等待。

```java
import org.swelement.ui.AstSteps;
import java.util.Arrays;

// 创建步骤条
AstSteps steps = new AstSteps(Arrays.asList("填写信息", "确认订单", "支付", "完成"));

// 设置当前进行中的步骤
steps.setCurrent(2);

// 监听步骤点击
steps.setStepClickListener(idx -> System.out.println("点击第 " + (idx + 1) + " 步"));
```

## 竖向步骤条

```java
import org.swelement.ui.AstSteps;
import java.util.Arrays;

AstSteps steps = new AstSteps(Arrays.asList("提交申请", "部门审批", "财务复核"));
steps.setDirection(AstSteps.Direction.VERTICAL);
steps.setCurrent(1);
```

## 含状态步骤条

每步可设置显式状态 `Status`：`WAIT` / `PROCESS` / `FINISH` / `ERROR` / `SUCCESS`。
**显式设置的 status 会覆盖由 `current` 推导出的状态**（例如某步虽在 `current` 之前，标 `ERROR` 就画红色叉）；传 `null` 可清空并回落到推导。

```java
import org.swelement.ui.AstSteps;
import java.util.Arrays;

AstSteps steps = new AstSteps(Arrays.asList("提交申请", "部门审批", "财务复核", "归档"));
steps.setCurrent(2);
steps.setStepStatus(0, AstSteps.Status.SUCCESS);
steps.setStepStatus(1, AstSteps.Status.ERROR);   // 覆盖"已完成"推导，显示红色叉
steps.setStepStatus(2, AstSteps.Status.PROCESS);
steps.setStepStatus(1, null);                    // 清空，回落到 current 推导
```

## 带图标步骤条

设置图标后，节点内绘制图标而不是序号 / 对勾；传 `null` 可还原。

```java
import org.swelement.ui.AstIcon;
import org.swelement.ui.AstSteps;
import java.util.Arrays;

AstSteps steps = new AstSteps(Arrays.asList("注册账号", "完善资料", "上传证件", "完成"));
steps.setCurrent(1);
steps.setStepIcon(0, AstIcon.Type.USER);
steps.setStepIcon(1, AstIcon.Type.EDIT);
steps.setStepIcon(2, AstIcon.Type.UPLOAD);
steps.setStepIcon(3, AstIcon.Type.CHECK);
```

## 简洁风格步骤条

`setSimple(true)` 后节点退化为彩色小圆点、连线为细线，不再绘制数字 / 对勾 / 图标，整体更紧凑。横向与竖向均生效。

```java
import org.swelement.ui.AstSteps;
import java.util.Arrays;

AstSteps steps = new AstSteps(Arrays.asList("待处理", "处理中", "已完结"));
steps.setCurrent(1);
steps.setSimple(true);
```

## Steps 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| steps | 步骤标题列表（不可为空，元素不可为 null） | List\<String\> | — | — |
| current | 当前进行中的步骤索引 | int | 0 ~ steps.size()-1 | 0 |
| direction | 排列方向 | Direction | HORIZONTAL / VERTICAL | HORIZONTAL |
| simple | 简洁风格（小圆点） | boolean | true / false | false |
| status（逐步骤） | 单步显式状态，覆盖 current 推导 | Status | WAIT / PROCESS / FINISH / ERROR / SUCCESS | 未设置（回落推导） |

## Steps 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| setSteps | 替换步骤列表（重置 current，并清空已设的状态/图标） | List\<String\> steps | void |
| setCurrent | 设置当前步骤（越界抛 IndexOutOfBoundsException） | int idx | void |
| getCurrent | 获取当前步骤索引 | — | int |
| setDirection | 设置排列方向（null 抛 IllegalArgumentException） | Direction d | void |
| setStepClickListener | 设置步骤点击监听（null 抛 IllegalArgumentException） | Consumer\<Integer\> l | void |
| setStepStatus | 设置某步显式状态（传 null 清空；越界抛 IndexOutOfBoundsException） | int idx, Status s | void |
| getStepStatus | 获取某步显式状态（未设置返回 null） | int idx | Status |
| setStepIcon | 设置某步图标（传 null 还原；越界抛 IndexOutOfBoundsException） | int idx, AstIcon.Type t | void |
| getStepIcon | 获取某步图标（未设置返回 null） | int idx | AstIcon.Type |
| setSimple | 设置简洁风格 | boolean b | void |
| isSimple | 是否为简洁风格 | — | boolean |
