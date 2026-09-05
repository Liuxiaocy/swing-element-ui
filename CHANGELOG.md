# Changelog

本文件记录 swing-element-ui 的版本发布与重大变更。版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)（MAJOR.MINOR.PATCH）。

---

## [1.0.0] — 2026-09-04（首个企业可交付版 / M1）

首个达到"可交付企业团队长期使用与二次开发"状态的版本。补齐了 5 个 P0 阻塞项。

### 新增（M1 可用性基础设施）
- **P0-1 键盘可达性**：`AstInteractiveComponent` 基类统一 `setFocusable(true)` 并注册 Space/Enter 键绑定；浮层统一 Esc 关闭并归还焦点；各交互组件 `selfCheck()` 断言可聚焦与键绑定存在。**按视觉优先决策不绘制焦点环**（焦点环在大面积组件上会破坏显示效果；焦点状态仍驱动 FOCUS 动画，子类可通过 `focusProgress()` 取用）。
- **P0-2 弹层跟随**：`AnimatedPopup` 新增宿主 `ComponentListener` / `HierarchyListener` 与祖先 `JViewport` 的 `ChangeListener`，滚动、宿主移动、窗口缩放时自动关闭浮层并卸载监听器（无泄漏）。
- **P0-5 主题体系统一**：保留 `ThemeManager` 运行时体系，`ElementTheme` 降级为默认值提供者，全局引用切到 `ThemeManager.getCurrent()`，换肤全量生效。

### 文档与工程化
- **P0-3 文档-代码一致性**：14 篇组件文档全面改用 `Ast*` API；新增 `tools/DocSnippetCheck.java` 抽取文档中的 Java 示例编译校验 + 命令/链接存在性校验，并接入 `build.bat` / `run-checks.bat`（永久防止再次漂移）；新增 `tools/DocScreenshotGen.java` 生成 32 张文档截图；`README.md` 补全 43 组件总览与全部 Demo。
- **P0-4 工程化与发版**：新增 `pom.xml`（Java 8，`maven-compiler-plugin` release=8，附带 sources/javadoc jar）；新增 `LICENSE`（Apache-2.0）；确定版本号 `1.0.0` 并打首个 git tag `v1.0.0`；`.gitignore` 增加 `.idea/` 并清理已提交的 IDE 配置。

### 已知限制（M1 仍存，见评估文档 M2/M3）
- 不支持 LookAndFeel 切换、无障碍（AccessibleContext）、JTable/JTree CellRenderer 内嵌。
- HiDPI 未实测；无 JUnit 数据层单测；常量体系（`AstSize`/`AstType`）尚未统一（计划在 Breaking Change 中处理）。
- 企业刚需缺口：Table 分页/单元格编辑、Upload、日期范围选择、表单跨字段/异步校验等（见 M2/M3 路线图）。
