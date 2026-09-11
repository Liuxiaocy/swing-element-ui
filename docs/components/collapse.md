# Collapse 折叠面板

可折叠的内容区域，支持同时展开多个，或手风琴模式（同时仅一个展开）。

## 基本用法

`AstCollapse()` 默认非手风琴，可同时展开多项。

```java
import org.swelement.ui.AstCollapse;
import javax.swing.JLabel;

AstCollapse c = new AstCollapse();
c.addItem("标题一", new JLabel("内容一"));
c.addItem("标题二", new JLabel("内容二"));

// 展开 / 折叠 / 切换
c.expand(0);
c.collapse(1);
c.toggle(1);
```

## 手风琴模式

`new AstCollapse(true)` 为手风琴模式：展开新面板会自动折叠其他已展开面板。

```java
import org.swelement.ui.AstCollapse;
import javax.swing.JLabel;

AstCollapse c = new AstCollapse(true);
c.addItem("标题一", new JLabel("内容一"));
c.addItem("标题二", new JLabel("内容二"));
c.expand(0);
c.expand(1);   // 标题一自动折叠
```

## 监听变化

`setChangeListener` 接收 `Consumer<int[]>`，回调参数为当前所有展开面板的索引数组。

```java
import org.swelement.ui.AstCollapse;
import javax.swing.JLabel;
import java.util.Arrays;

AstCollapse c = new AstCollapse();
c.addItem("标题一", new JLabel("内容一"));
c.addItem("标题二", new JLabel("内容二"));
c.setChangeListener(openIndices -> System.out.println("当前展开：" + Arrays.toString(openIndices)));
```

## Collapse 属性

| 参数 | 说明 | 类型 | 可选值 | 默认值 |
|------|------|------|--------|--------|
| accordion | 手风琴模式（同时仅一个展开） | boolean | true / false | false |
| items | 面板集合（addItem 追加） | — | — | — |

## Collapse 方法

| 方法名 | 说明 | 参数 | 返回值 |
|--------|------|------|--------|
| addItem | 追加面板（title / content 不可为 null） | String title, JComponent content | void |
| setAccordion | 设置手风琴模式 | boolean a | void |
| isAccordion | 是否手风琴模式 | — | boolean |
| expand | 展开指定面板（越界抛 IndexOutOfBoundsException） | int idx | void |
| collapse | 折叠指定面板（越界抛 IndexOutOfBoundsException） | int idx | void |
| toggle | 切换指定面板（越界抛 IndexOutOfBoundsException） | int idx | void |
| isOpen | 指定面板是否展开 | int idx | boolean |
| getItemCount | 面板数量 | — | int |
| getOpenIndices | 当前展开的面板索引数组 | — | int[] |
| setChangeListener | 设置变化监听（null 抛 IllegalArgumentException） | Consumer\<int[]\> l | void |
