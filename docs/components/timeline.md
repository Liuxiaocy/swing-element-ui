# Timeline 时间线

可视化地呈现时间流信息。

## 基本用法

垂直展示带有时间戳的事件序列。未设置图标时，节点为 12px 类型色实心圆点。

```java
import org.swelement.ui.AstTimeline;
import java.util.ArrayList;
import java.util.List;

List<AstTimeline.Item> items = new ArrayList<AstTimeline.Item>();
items.add(new AstTimeline.Item("2026-08-01", "项目启动", "确定范围与目标", AstTimeline.Type.PRIMARY));
items.add(new AstTimeline.Item("2026-08-10", "P1 完成", "全部组件完成并自检通过", AstTimeline.Type.SUCCESS));
items.add(new AstTimeline.Item("2026-08-21", "P2 进行中", AstTimeline.Type.WARNING));

AstTimeline timeline = new AstTimeline(items);
```

## 带图标的时间线

给 `Item` 传入 `AstIcon.Type` 后，节点会**放大到 20px**：类型色圆底 + 内部 16px 白色图标。
未设置图标的条目仍是 12px 实心圆点，两者可在同一条时间线里混排。

```java
import org.swelement.ui.AstIcon;
import org.swelement.ui.AstTimeline;
import java.util.ArrayList;
import java.util.List;

List<AstTimeline.Item> items = new ArrayList<AstTimeline.Item>();
// 带图标（无描述）
items.add(new AstTimeline.Item("2026-08-01", "账号注册", AstTimeline.Type.PRIMARY, AstIcon.Type.USER));
// 带图标 + 描述
items.add(new AstTimeline.Item("2026-08-05", "资料审核", "人工复核中", AstTimeline.Type.WARNING, AstIcon.Type.EYE));
// 不带图标：仍是 12px 圆点
items.add(new AstTimeline.Item("2026-08-12", "已归档", AstTimeline.Type.INFO));

AstTimeline timeline = new AstTimeline(items);
```

## Timeline 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| items | 时间线条目列表（不可为空，元素不可为 null） | List\<Item\> | — | — |

## Timeline.Item 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| timestamp | 时间戳（不可为 null） | String | — | — |
| title | 标题（不可为 null） | String | — | — |
| description | 描述，可为 null | String | — | null |
| type | 节点类型色（不可为 null） | Type | PRIMARY / SUCCESS / WARNING / DANGER / INFO | — |
| icon | 节点图标；设了会放大节点并画白色图标 | AstIcon.Type | 任意 AstIcon.Type | null（画实心圆点） |

## Timeline.Item 构造器

| 构造器 | 说明 |
|--------|------|
| Item(timestamp, title, type) | 无描述、无图标 |
| Item(timestamp, title, description, type) | 有描述、无图标 |
| Item(timestamp, title, type, icon) | 无描述、带图标 |
| Item(timestamp, title, description, type, icon) | 完整（icon 可为 null） |

## Timeline 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| getIcon | 获取该条目的图标（未设置返回 null） | — | AstIcon.Type |
