# 企业级就绪度评审与优化路线图

> 评审日期：2026-09-04
> 评审范围：48 个 UI 组件、12 个核心类、25 个 Demo、56 篇文档
> 评审视角：交付企业应用团队长期使用与二次开发
> 结论：**组件层完成度高，但"可用性基础设施"欠账较多，直接交付企业团队会在第一周就踩坑。**

---

## 一、总体判断

| 维度 | 评分 | 判断 |
|------|------|------|
| 组件覆盖与绘制质量 | 80 | 好。48 个组件覆盖 Element UI 主体，自绘质量与对比度断言扎实 |
| 再开发扩展性 | 55 | 中。基类抽象成立，但常量体系漂移、主题不能运行时替换 |
| 原生 Swing 适配 | 45 | 偏差。能塞进 JPanel，但键盘、L&F、弹层、无障碍四处硬伤 |
| 文档体系 | 75 | 中上。组件文档已全面改用 Ast* API，新增文档-代码一致性自检与 32 张截图；仍非 48 篇全量 |
| 工程化与发版 | 90 | 好。已具备 pom.xml / LICENSE / 版本号 1.0.0 / 首个 tag v1.0.0，可作为 Maven 依赖引入 |

**一句话结论**：M1 五个 P0 阻塞项（键盘、弹层跟随、文档一致性、工程化发版、主题统一）已全部解决，库已达到"可交付企业团队长期使用与二次开发"的状态。剩余为 M2/M3 的组件缺口（Table 分页/编辑、Upload 等）与长期可维护性工作（常量体系、JUnit、无障碍、HiDPI）。

---

## 二、P0 阻塞项（不解决不能交付）

### P0-1 键盘完全不可达 —— 企业录入场景直接不可用

> ✅ **状态：已解决（2026-09-04 / M1）** —— `AstInteractiveComponent` 基类统一焦点与键绑定，浮层 Esc 关闭归还焦点；各交互组件 `selfCheck()` 含可聚焦/键绑定断言。**注：按视觉优先决策，不绘制焦点环**（见修复方案第 5 条的修订）。见 CHANGELOG.md `[1.0.0]`。

**证据**
- `grep -rl "setFocusable(true)" src/` → 仅 4 个文件（AstButton、AstRate、AstTable、AstTree）
- `grep -rn "InputMap\|ActionMap" src/` → 全库仅 2 处，都在 `AstDrawer.java:111`
- `AstButton.java:50` 调了 `setFocusable(true)`，但全类无任何键绑定，动作只在 `mouseReleased` 触发
- `AstCheckbox` / `AstRadio` / `AstSwitch` / `AstSlider` / `AstSelect` / `AstDatePicker` 均不可 Tab 聚焦

**影响**：企业应用的表单录入岗（财务、仓储、客服）重度依赖纯键盘操作。当前状态下，用户每填一个字段都必须摸鼠标，录入效率断崖式下降，且无法满足信息无障碍验收要求。

**修复方案**
1. 在 `AstInteractiveComponent` 基类中统一 `setFocusable(true)`，而不是各子类零散设置。
2. 基类统一注册键绑定（Java 8 写法，不用 lambdas 也可）：
   ```java
   getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "activate");
   getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate");
   getActionMap().put("activate", new AbstractAction() {
       public void actionPerformed(ActionEvent e) { doClick(); }
   });
   ```
3. 组件按职责补方向键：Slider 左右调值、Tabs 左右切页、Menu 上下导航、Table 上下选行。
4. 所有浮层（Select / DatePicker / Dropdown / Popover / Dialog / Drawer）统一绑定 `Esc` 关闭，焦点关闭后归还给触发组件。
5. **画焦点环**：当前自绘组件获得焦点后没有任何视觉反馈，用户不知道焦点在哪。需在 `paintComponent` 里根据 `isFocusOwner()` 绘制 focus ring，颜色用 `ElementTheme.PRIMARY` 半透明描边。
   > ⚠️ **决策修订（2026-09-05）**：本条**已实现后按视觉优先决策撤回**。焦点环是贴合组件外框的 2px 描边，在 `AstCalendar` / `AstCarousel` / `AstTree` / `AstMenu` 等大面积组件上会明显破坏显示效果。
   > 现方案：**只保留键盘可达性本身**（可聚焦、Space/Enter 激活、Esc 关闭并归还焦点、焦点状态仍驱动 FOCUS 动画供子类取用），**不绘制任何焦点环**。已从 `AstInteractiveComponent` 移除 `paintFocusRing` / `focusRingRect` / `isFocusVisible` 及 13 处调用。
   > 后续若需要焦点可见性，建议改用不侵占布局的轻量提示（如输入框聚焦时边框变色），而不是组件外描边。
6. 补自检：每个交互组件的 `selfCheck()` 断言 `isFocusable() == true` 且 `getInputMap(WHEN_FOCUSED).get(VK_SPACE) != null`。

> 按项目惯例，写完断言必须做反向验证（临时回退确认断言真的 exit=1），否则断言空转。

---

### P0-2 弹层不跟随滚动与窗口缩放

> ✅ **状态：已解决（2026-09-04 / M1）** —— `AnimatedPopup` 挂宿主 `ComponentListener`/`HierarchyListener` 与祖先 `JViewport` 的 `ChangeListener`，滚动/移动/缩放时自动关闭浮层并卸载监听器（无泄漏）；`selfCheck()` 含反向验证断言。见 CHANGELOG.md `[1.0.0]`。

**证据**
- `core/AnimatedPopup.java` 只挂了 `AWTEventListener`（`:37`、`:77`）做"点击外部关闭"
- 全库 `WindowListener` / `ComponentListener` / `HierarchyListener` 零使用（仅 `AstDrawer.java:100` 监听自身 glassPane 尺寸）
- 浮层挂在窗口 `JLayeredPane` 的 `POPUP_LAYER`（`AnimatedPopup.java:118`），坐标一次性算定
- `core/PopupPositioner.java:140` 用整屏 bounds 做翻转判断，而非宿主窗口 bounds

**影响**：只要把表单放进 `JScrollPane`（企业后台标配），滚动一下，Select 下拉框就停在原地与输入框分离；窗口拖拽缩放同理。这是用户第一眼就能发现、且会直接判定"这库不成熟"的问题。

**修复方案**
1. `AnimatedPopup` 构造时对宿主组件挂 `HierarchyListener` + 宿主的 `ComponentListener`（监听 move/resize）+ 祖先 `JScrollPane` 的 viewport `ChangeListener`。
2. 任一事件触发时重算屏幕坐标（复用 `SwingUtilities.convertPointToScreen`），或简单起见**直接关闭浮层**（Element UI 本身在滚动时也是关闭的，交互可接受）。
3. `PopupPositioner` 的边界判断改用宿主窗口 `getBounds()` 而非屏幕 bounds，避免跨屏/任务栏遮挡。
4. 补自检：把 Select 放进 JScrollPane，程序化滚动 viewport，断言浮层坐标已更新或已关闭。

---

### P0-3 七篇文档引用已被删除的类

> ✅ **状态：已解决（2026-09-04 / M1）** —— 14 篇组件文档全面改用 `Ast*` API；新增 `tools/DocSnippetCheck.java`（文档示例编译 + 命令/链接校验）并接入 `build.bat`/`run-checks.bat` 作为永久防漂移自检；`tools/DocScreenshotGen.java` 生成 32 张文档截图；`README.md` 补全 43 组件总览与全部 Demo。见 CHANGELOG.md `[1.0.0]`。

**证据**
- `src/org/swelement/ui/` 下非 `Ast` 开头的文件只剩 `FormInvalidMarker.java` 和 `FormValueProvider.java`
- 但 `docs/components/` 中仍在 import：`Button` `Input` `Select` `Badge` `Checkbox` `Pagination` `Tag`
- README 同样列的是老 demo 名

**影响**：新手照文档抄第一段代码就编译不过，直接劝退。这是投入产出比最高的修复项——纯文本替换，几小时搞定。

**修复方案**
1. 全量替换 7 篇文档中的 `org.swelement.ui.Button` → `AstButton` 等，正文示例同步改。
2. 用脚本核对：抽取文档中所有 `org.swelement.ui.X`，逐个校验文件是否存在，把这条做成 `run-checks.bat` 的一项自检（**文档-代码一致性自检**，能永久防止再次漂移）。
3. README 组件表从 14 行补全到 46 行，补上缺失的 10 个 demo。

---

### P0-4 无法作为依赖引入

> ✅ **状态：已解决（2026-09-04 / M1）** —— 新增 `pom.xml`（Java 8，附 sources/javadoc jar）、`LICENSE`（Apache-2.0）、版本号 `1.0.0` 与首个 git tag `v1.0.0`；`.gitignore` 增加 `.idea/` 并清理已提交 IDE 配置。见 CHANGELOG.md `[1.0.0]`。

**证据**：根目录无 `pom.xml` / `build.gradle`、无 `LICENSE`、`git tag` 数量为 0、`.idea/` 已被提交（6 个文件）。

**影响**：企业团队无法做版本管理、依赖升级、合规审查（法务必问 LICENSE）。只能"拷贝源码进工程"，后续升级等于重新合并，长期必然分叉。

**修复方案**（保持 Java 8，不需要改任何源码）
1. 新增 `pom.xml`：`maven-compiler-plugin` 设 `source/target = 1.8`，打包 `swelement-ui-{version}.jar`，附带 sources jar 与 javadoc jar。
2. 保留 `build.bat` 作为无 Maven 环境的兜底，但让二者产出同一目录结构。
3. 补 `LICENSE`（若希望最大商业化友好度，推荐 Apache-2.0 或 MIT）。
4. 确定版本号语义（如 `0.1.0` → `1.0.0` 为首个企业可交付版），打第一个 git tag。
5. `.gitignore` 增加 `.idea/`，并 `git rm -r --cached .idea` 清理已提交的 IDE 配置。

---

### P0-5 双主题系统并存，换肤只生效一半

> ✅ **状态：已解决（2026-09-04 / M1）** —— 保留 `ThemeManager` 运行时体系，`ElementTheme` 降级为默认值提供者，全局引用切到 `ThemeManager.getCurrent()`，换肤全量生效。见 CHANGELOG.md `[1.0.0]`。

**证据**
- `ElementTheme` 是 `public static final` 常量类（`core/ElementTheme.java:7-19`），不可实例化
- 另有 `Theme` 接口 + `ThemeManager` + `ElementLightTheme`（`ThemeManager.java:68` 自动注册），两套颜色值硬编码相同
- `AstIcon:76`、`AstCarousel:232`、`AstCard:183`、`AstTable`、`AstTree`、`AstCalendar`、`AstTimeline` 直接引用 `ElementTheme.PRIMARY` 静态量，**不响应** `ThemeManager.setCurrent()`
- `Theme.getColor/getFont/getSize`（`Theme.java:44-46`）除 `ElementLightTheme.selfCheck` 自测外无任何调用 → 死代码

**影响**：企业客户第一件事就是换品牌色。当前要么改源码重新编译（等于 fork），要么调 `ThemeManager.setCurrent()` 后发现一半组件没变。这是二次开发的头号痛点。

**修复方案**
1. 二选一，不要两套并存。推荐**保留 ThemeManager 运行时体系**，把 `ElementTheme` 降级为"默认值提供者"：静态常量改为从 `ThemeManager.getCurrent()` 惰性读取，或干脆标记 `@Deprecated`。
2. 全局替换所有 `ElementTheme.XXX` 直接引用为 `ThemeManager.getCurrent().color("xxx")`。这一步是纯机械替换，但必须配合自检。
3. 补一个 `BrandTheme` 示例 + 自检：注册自定义主题后，遍历所有组件实例断言取到的主色已变化。
4. 删除 `Theme.getColor/getFont/getSize` 死代码，或真正实现它。
5. `ThemeManager.setCurrent` 的 `repaint()`（`ThemeManager.java:74-83`）必须确保在 EDT 执行。

---

## 三、原生 Swing 适配：能用什么、不能用什么

这是企业团队最关心的"能不能跟我现有代码混用"。逐条回答：

| 场景 | 结论 | 说明与注意 |
|------|------|-----------|
| 放进 JPanel / 任意 LayoutManager | ✅ 可用 | 继承 JComponent 且 `setOpaque(false)`（`AstAbstractComponent.java:36`），主流组件都重写了 `getPreferredSize` |
| 放进 JScrollPane | ⚠️ 有条件 | 容器本身没问题，但**内部浮层会错位**（见 P0-2） |
| 作为 JTable / JTree 的 CellRenderer | ❌ 不建议 | 自绘组件持有动画与焦点状态，renderer 每次绘制都复用同实例重建，会掉帧且鼠标事件不生效。表格内需要交互控件时，应改用 `CellEditor` 或走 `AstTable` 自带的列渲染扩展点 |
| 与原生 JButton/JTextField 混排 | ✅ 可用 | 视觉上不统一（原生走 L&F，Ast 走自绘），见下条 |
| 切换 LookAndFeel（FlatLaf 等） | ❌ 无效 | 组件完全不读 UIManager，也不重写 `updateUI()`。混排界面会一半变主题色一半不变，视觉割裂 |
| 标准事件监听 | ⚠️ 半套 | 基础件用 `addActionListener`/`addItemListener`；但 Collapse/DatePicker/Select/Tree/Table/Transfer 各自用 `setXxxListener(Consumer<...>)`，需学两套 |
| 数据绑定（PropertyChange） | ❌ 不支持 | 全库无 `firePropertyChange`。想做 MVVM 式绑定只能靠上面的 Consumer 回调 |
| 键盘导航 | ❌ 不可用 | 见 P0-1 |
| 屏幕阅读器 / 无障碍 | ❌ 不支持 | 无 `AccessibleContext` 覆写，Java Access Bridge 读到的是空白组件 |
| HiDPI 缩放（125%/150%） | ⚠️ 需实测 | 整数像素 + `RoundRectangle2D` 绘制，未做 DPI 适配。现代笔记本需人工验证 |
| EDT 线程安全 | ✅ 合规 | `Animator`/`GlassPane`/`AnimatedPopup`/`AstCarousel` 均用 `javax.swing.Timer`（在 EDT 跑），`AstDialog` 显式 `invokeLater`。仅 `ThemeManager.setCurrent` 需注意调用线程 |
| Java 8 纯度 | ✅ 达标 | 未发现 `var` / `List.of` / `Map.of` 等 Java 9+ API |

**给使用方的三条硬约束建议**（写进文档）：
1. Ast 组件**只用于整页新写的模块**，不要塞进 JTable/JTree 的渲染器。
2. 若项目已用 FlatLaf 之类现代 L&F，建议**全站统一用 Ast 组件**，避免半原生半自绘的视觉割裂；或接受 Ast 组件不跟随 L&F。
3. 表单类页面必须等 P0-1 修完再上，否则键盘用户无法工作。

---

## 四、再开发（扩展性）优化

### 4.1 常量体系漂移 —— 必须统一

这是二次开发最容易踩的隐藏坑：

| 问题 | 证据 | 修复 |
|------|------|------|
| `setSize(int)` 语义冲突 | `AstAvatar.java:16` 是像素值（32/40/64），其余 12 组件是档位（0/1/2） | Avatar 改为 `setSizePx(int)`，或统一走档位 + 查表 |
| type 常量序号错位 | `AstButton.java:15` 是 `DEFAULT=0,PRIMARY=1..INFO=5`；`AstTag.java:12` 是 `PRIMARY=0..INFO=4` | 抽 `AstType` 共享枚举，全库替换 |
| 命名不一致 | `AstCloseButton.setButtonSize(int)`（:83）、`AstIcon.setSizeValue(int)`（:134） | 统一为 `setSize(int)` |
| 类型不统一 | `AstBadge.setType(Type)` 用枚举，`AstButton`/`AstTag`/`AstIcon` 用 int | 统一用 `AstType` 枚举，int 版保留为 `@Deprecated` 过渡 |

**建议**：新建 `org.swelement.core.AstSize` 和 `org.swelement.core.AstType` 两个共享枚举，全库替换。这是 Breaking Change，正好配合 1.0.0 发版一次做完。

### 4.2 重复代码收敛

- **尺寸档位越界校验**复制粘贴于 12+ 组件（Badge:112、Cascader:96、DatePicker:101、Form:127、Input:114、InputNumber:86、Pagination:43、Rate:115、Select:131、Table:131、Transfer:388、Tree:220）→ 上提到基类 `setSize(int)` final 方法。
- **圆角矩形绘制两份实现**：`AstAbstractComponent.fillRoundRect/drawRoundRect`（:146-167）与 `PaintingHelper.fillRoundRect/drawRoundRect`（:47-63）完全相同 → 删一留一。
- **type→颜色 5-case switch 重复**：`AstButton.typeColor/baseBg`（:202-224）与 `AstTag.darkBg`（:52-60）→ 抽到 `AstType` 枚举自带颜色属性。

### 4.3 数据模型层

- `AstTableModel` 是真实模型（排序/筛选/选择/合计/合并/行状态），但**缺 `TableModelListener` 增量通知、缺分页、缺单元格编辑、缺懒加载钩子**。企业接数据库分页必须自己改。
- `AstTree` 用自有 `AstTree.TreeNode`（:39），非 Swing 标准 `TreeModel`/`TreeNode`，且 `setRoot` 整体重建（:140），无法增量更新、无法与 JTree 互通。
- `FormValueProvider` 接口（AstInput/Select/InputNumber 实现）——**自定义字段未实现该接口时，表单校验静默失败**，不报错。这是很危险的静默 bug，建议改为校验时抛异常或打警告。

**建议**：优先实现 `TableModelListener` + 分页接口，这是企业表格的刚需入口。

### 4.4 测试体系

- 现有 44 个组件有 `selfCheck()` + `main()` 跑 `-ea` 断言，但**不是单元测试框架**，无 CI 集成。
- `AstTableModel`、`AstTableColumn`、`FormInvalidMarker`、`FormValueProvider` **无 main → 核心数据逻辑（排序/筛选/合计）零自测**，这是风险最高的地方。
- 无视觉回归：自绘组件的视觉变更只能靠人眼。

**建议**：
1. 优先给 `AstTableModel` 补 selfCheck（排序稳定性、筛选组合、合计正确性）。
2. 引入 JUnit 4（Java 8 兼容）做数据层单测，`selfCheck()` 保留为组件级冒烟。
3. 视觉回归：离屏渲染到 `BufferedImage` → 采样关键点像素 → 与基线比对。项目已在用像素采样断言，扩成基线文件比对即可。

---

## 五、组件缺口（企业刚需）

| 缺口 | 优先级 | 说明 |
|------|--------|------|
| Upload 上传 | P0 | 企业表单必备，当前完全缺失 |
| Table 分页 | P0 | `AstTable` 只有排序+列筛选+合计+合并，无分页 |
| Table 单元格编辑 | P0 | 后台管理标配 |
| Table 虚拟滚动 | P1 | 万行数据需验证性能，当前全量绘制 |
| 日期范围选择（daterange） | P1 | 只有单体 DatePicker/TimePicker，报表查询必需 |
| 表单校验增强 | P1 | 现仅 Required/MinLen/MaxLen/Email/Phone/Type 6 种（`AstForm.java:42-91`），缺数值范围、日期比较、**跨字段校验**（确认密码）、异步远程校验、i18n 消息 |
| Transfer/Tree 增量更新 | P2 | 见 4.3 |

---

## 六、文档体系优化

| 项 | 现状 | 建议 |
|----|------|------|
| 组件文档 | 15/48，其中 7 篇死链 | 修死链 + 补齐 33 篇（可用模板批量生成骨架，再逐个补属性表） |
| README | 只列 14 组件、15 demo，无快速开始 | 重写：30 秒最小示例 + 完整组件矩阵 + 混入原生 Swing 的指引 + FAQ |
| CodeWiki.md | 42KB，Phase 3 组件已实现却仍写"待开发" | 加最后更新日期，或改为脚本半自动生成 |
| Javadoc | 200 块 / 908 public 成员 ≈ 22% | 优先给 46 个组件的 public 构造与方法补齐，企业靠 IDE 补全看注释 |
| Demo | 25 个独立 demo，无总览入口 | **新增 `ShowcaseDemo`**：左侧导航 + 右侧预览，覆盖全部组件。这是企业选型的决定性演示 |
| CHANGELOG | 无 | 必补，1.0.0 起开始记录 |
| CONTRIBUTING | 无 | 必补，含 selfCheck 编写规范、对比度断言要求、命名约定 |
| 迁移指南 | 无 | 补「从原生 Swing 迁移」章节（组件对照表 + 常见坑） |
| 已知限制 | 无 | 明确写出：不支持 L&F、不支持无障碍、不建议用于 CellRenderer、HiDPI 未验证 |
| 截图 | `docs/screenshots/` 空目录，14 篇全是死链 | 补截图或删引用 |

---

## 七、分阶段路线图

### M1 · 打通可用性（约 1-2 周）—— 不完成不建议交付
1. P0-1 键盘可达性（基类统一 focusable + InputMap/ActionMap + Esc 关闭 + 自检；**不绘制焦点环**）
2. P0-2 弹层跟随滚动/缩放（HierarchyListener + ComponentListener + 重定位或关闭）
3. P0-3 文档死链修复 + 文档-代码一致性自检
4. P0-4 `pom.xml` + LICENSE + 版本号 + 首个 tag + 清理 `.idea/`
5. P0-5 主题体系统一（ElementTheme 降级，全量走 ThemeManager）

### M2 · 补齐企业刚需（约 2-3 周）
6. `AstTable` 分页 + 单元格编辑
7. Upload 组件
8. `AstForm` 校验增强（数值范围、日期比较、跨字段、异步）
9. `ShowcaseDemo` 全组件总览（选型演示）
10. README 重写 + 补齐组件文档 + 已知限制章节
11. `AstTableModel` 补 selfCheck

### M3 · 长期可维护（约 3-4 周）
12. 常量体系统一（`AstSize` / `AstType` 枚举）——配合 1.0.0 Breaking Change
13. Javadoc 补齐至 80%
14. 无障碍 `AccessibleContext` 基础支持
15. HiDPI 实测与适配
16. 动画期脏矩形重绘优化（当前每 16ms 全组件 repaint）
17. 引入 JUnit 做数据层单测 + 视觉回归基线
18. CHANGELOG / CONTRIBUTING / 迁移指南

---

## 八、给决策者的建议

如果这个项目要在**近一个月内**投入企业项目：优先做 M1 全部 + M2 的 `ShowcaseDemo` 和 `AstTable` 分页，其余可以边用边补。

如果要做成**长期维护的内部 UI 标准**：M1→M2→M3 全流程走完，尤其是 M3 的常量体系统一（12）和测试体系（17）——不做好这两项，二次开发的人每加一个组件就会复制粘贴一遍旧代码的坏味道，半年后库会腐化。

**当前最不该做的一件事**：在 M1 完成前继续加新组件。每加一个组件，就等于多一个要回头补键盘支持、补主题接入、补文档的对象。
