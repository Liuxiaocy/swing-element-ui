# P4-D/E/F 组件体系精修设计（AstIcon 重构 / 卡片模板 / 尺寸·标识·Loading·穿梭框）

- 日期：2026-08-29
- 状态：待评审
- 关联：P4-C（表格类型增强，已合入 e2c2516）；本批为 P4 收尾
- 约束：**Java 8 原生**（仅 JDK 1.8 API，无第三方依赖），WCAG 2.1 AA，全量自检

## 1. 目标与范围

补齐 Element UI 体系下三类欠账：

- **P4-D AstIcon 重构**：int 常量 → 枚举 API，图标库 20 → 40+，抽出可复用的静态绘制器。
- **P4-E 卡片模板**：AstCard 阴影三态对齐 Element，并提供常用卡片版式模板 demo。
- **P4-F 精修**：尺寸档位补全、AstBadge 溢出/隐藏/配色、AstLoading 外观定制、AstTransfer 标题与空态。

## 2. P4-D AstIcon 重构

### 现状
`AstIcon(int type, Color, int size)`，20 个 `public static final int` 常量，Graphics2D 自绘
（16/20 网格、stroke 2、圆角端点）。被 AstInput、AstDrawer、InputDemo 引用。

### 设计
1. **枚举化**：新增 `public enum Type { CHECK, CLOSE, ... }`；旧 int 常量保留且值 = 枚举序号
   （向后兼容断言锁定），旧构造器委托新枚举构造器。
2. **图标库扩到 40+**（Element UI 高频，全部自绘）：
   - 通用：CALENDAR、CLOCK、STAR、STAR_FILLED、BELL、MESSAGE、MORE(⋯)、MENU、
     LINK、LOCATION、PHONE、CAMERA、COLLECTION(收藏)、DELETE_FILLED
   - 操作：UPLOAD、DOWNLOAD、LOCK、UNLOCK、SORT、FILTER_FILLED(实心漏斗)、
     FULL_SCREEN、COPY、SHARE、PRINT
   - 状态：CIRCLE_CHECK、CIRCLE_CLOSE、CIRCLE_WARNING、CIRCLE_INFO(带圈状态)、
     QUESTION、LOADING(旋转环)
   - 方向补充：CARET_UP/DOWN/LEFT/RIGHT(实心小三角，表格排序用)
3. **静态绘制器**：`public static void paintIcon(Graphics2D g, Type t, Color c, int size, float spin)`，
   任意组件可零实例调用（AstTable 表头箭头等可切换复用）。
4. **旋转动画**：`Type.LOADING` + `setSpinEnabled(true)` 用现有 `Animator`/`Timer` 旋转；
   spin 参数为弧度相位。
5. **自检**：全部 Type 落笔断言（每类型至少 1 个非背景像素）；旧 int 常量与枚举序号一致性断言；
  参数校验断言（size 越界仍抛）。
6. **demo**：新增 `AstIconDemo`——40+ 图标网格（4 列流式）、悬停高亮 + 名称 tooltip、
   三种尺寸/三色变体段、旋转 LOADING 段。

## 3. P4-E 卡片模板

### 现状
`AstCard(title, bordered, shadowOnHover)`：boolean 阴影只有「悬停/无」两态。

### 设计
1. **阴影三态**：`public enum Shadow { ALWAYS, HOVER, NEVER }` + `setShadow(Shadow)`；
   旧布尔构造器保留（shadowOnHover=true→HOVER，false→NEVER，兼容映射断言）。
2. **无头卡片**：`title` 为空字符串时不绘制 header 区域，内容占满（现有行为确认 + 断言锁定）。
3. **卡片模板**（demo 层，不新建组件类）：
   - 图片卡片（AstAvatar 大图 + 标题 + 描述 + 操作按钮）
   - 列表卡片（AstCard + 行列表内容）
   - 数字统计卡片（大数字 + 标签 + 趋势 Tag）
   - 阴影三态对比（同内容三卡并排）
   - 带头部操作卡片（addHeaderAction 已有，组合展示）
4. **自检**：Shadow 三态断言（HOVER 时 hover 动画前后阴影 alpha 变化）、无头卡片高度断言。

## 4. P4-F 尺寸 / 标识 / Loading / 穿梭框

### F1 尺寸档位补全（R1 配方，P3 已定）
- `AstPagination`：字号/按钮尺寸随 tier（40/32/28 语义 → 按钮高 32/28/24）。
- `AstRate`：星星尺寸随 tier（28/22/18 绘制半径联动）。
- `AstForm`：行间距与内部控件 tier 联动（`setSizeTier` 下发到字段）。
- `AstBadge`：dot 直径与数字字号随 tier。
- 各补 `selfCheck` 档位断言（preferredSize 随 tier 变化）。

### F2 AstBadge
- `max` 溢出：`count > max` 显示 `max+`（Element 行为，默认无 max）。
- `hidden`：`setHidden(true)` 整个 badge 不占位不绘制。
- `type`：`Type{PRIMARY, SUCCESS, WARNING, DANGER, INFO}` 配色（默认 PRIMARY）。
- WCAG：数字白字/底色对比度断言（5 色全测）。

### F3 AstLoading
- `setBgColor(Color)`：遮罩背景色（默认半透明白/黑按主题）。
- `setSpinnerSize(int)`：环半径联动。
- `setDelay(int ms)`：showLoading 后延迟显示（防闪烁），hideLoading 立即。
- 自检：delay 用假 Timer 断言（不真等）；bgColor 落笔像素断言。

### F4 AstTransfer
- `setTitles(String left, String right)`：面板标题（默认「列表 1/列表 2」）。
- `setButtonTexts(String toRight, String toLeft)`。
- 空状态：无数据时列表区居中灰字「无数据」。
- 自检：titles 传递断言 + 空态绘制断言。

## 5. 批次与提交

| 批 | 内容 | 提交 |
|----|------|------|
| D | AstIcon 枚举化 + 40 图标 + paintIcon + spin + AstIconDemo + build.bat 注册 | 1 commit |
| E | AstCard Shadow 三态 + 无头卡片 + 模板 demo | 1 commit |
| F1-F4 | 4 个小提交（Pagination/Rate/Form/Badge 档位；Badge 增强；Loading；Transfer） | 4 commit |

每批：编译（原生 JDK 8）→ 反向验证新断言 → 全量 34+ 项 self-check → 提交。

## 6. 明确不做

- 图标全量对齐 Element 300+（只做 40+ 高频）；矢量字体图标。
- 卡片拖拽排序、瀑布流布局。
- Loading SVG 路径多样化、服务指令式 v-loading 指令语义。
- 穿梭框拖拽、右面板初始数据、自定义 item 渲染插槽。
