# 批次 2 设计：Input 展现增强 + Select 可清空重写

日期：2026-08-24
前置：批次 1（`d9a4212` 尺寸收敛、`cd5b531` 三项增强）
用户决策：TextArea 独立组件类；图标用内置矢量枚举 + AstIcon 复用。

## 1. AstIcon 补齐 EYE_OFF

新增 `EYE_OFF = 19`（眼睛轮廓 + 斜杠，风格同现有 stroke 图标），修正文档注释与实现不符（注释声称有 EYE_OFF，常量与绘制均无）。密码切换的睁/闭眼直接复用 `EYE`/`EYE_OFF`。

## 2. Input 尺寸档位（对齐 Element UI）

`setSize(SIZE_LARGE / SIZE_DEFAULT / SIZE_SMALL)`，模式同 Tag：

| 档位 | 高度 | 字体 | 内边距 | 清空按钮 |
|---|---|---|---|---|
| LARGE | 40 | 14px | 10/16 | 18 |
| DEFAULT | 32 | 13px | 8/12 | 16 |
| SMALL | 28 | 12px | 4/8 | 14 |

按 Element 标准走（DEFAULT=32）。demo 手动 `setPreferredSize(…,40)` 全部移除改档位。

## 3. 密码模式

`new Input(placeholder, PASSWORD)`：内部换 `JPasswordField`；east 面板加眼睛按钮（AstIcon EYE/EYE_OFF，`mousePressed` 切换 echoChar，hover 变色 + 手型光标）。清空按钮逻辑不变，两者在 east 并排。

## 4. 前后缀图标

`setPrefixIcon(int)` / `setSuffixIcon(int)`（取 AstIcon 常量）。WEST 面板镜像 east 配方（透明 + GridBagLayout 居中 + 留白 8/4），静态不可点。图标默认色 `0x606266`（≥7:1）。suffix 图标与清空按钮在 east 并排（图标在前）。

## 5. TextArea 独立组件

`TextArea(String placeholder, int rows, int columns)`：透明 JScrollPane（无边界线）包 JTextArea（lineWrap），复用 Input 的边框/聚焦光晕/占位符绘制配方，`getText/setText/setEnabled`。尺寸由 rows×columns 决定，纵向滚动按需出现。不做 autosize。

## 6. Select 可清空重写（消灭最后的坐标命中残留）

删除 `paintComponent` 手绘 × 和 `mousePressed` 里的 `getX() > getWidth()-46` 坐标判断；east 面板放 CloseButton(16)，复用 Input 的 clearVis/hover 淡入配方；单选有值且 hover 时 × 淡入，同时箭头隐藏（clearVis ≥ 0.5 时不画箭头，对齐 Element「× 替换箭头」行为），点击清空选择。

## 验证

每个组件 selfCheck 扩展（尺寸档位断言、密码切换 echoChar、图标组件存在性、Select 清空事件路径）；全量编译 + 31 自检回归 + InputDemo/SelectDemo 更新。

## 明确不做

textarea autosize、图标任意 Icon 重载、Select 多选清空、Input maxlength。
