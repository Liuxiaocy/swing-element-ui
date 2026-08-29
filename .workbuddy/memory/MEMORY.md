# swing-element-ui 项目长期约定

## 构建与运行环境

- **技术栈：Java 8 + Swing 自绘组件，目标 JDK 1.8。只允许 Java 8 原生 API，禁止 Java 9+ 特性**（用户明确要求；做不到就不做，不要引入高版本依赖或语法糖）。
- 本机 PATH 上的 `java`/`javac` 是 **JDK 26**，编译/运行必须显式使用
  `C:/Program Files/Java/jdk1.8.0_311/bin/javac.exe` 和 `.../java.exe`。
  `build.bat` 已固定优先取 JDK 1.8（变量 `JAVAC`/`JRUN`），高版本 JDK 才回退 `--release 8`。
- 编译必须**全量** `src/org/swelement/{core,ui,demo}`（组件间有跨包依赖，单文件编不过）。
- 跑 `javac` / `java` 需要 Bash 工具的 `dangerouslyDisableSandbox`。
- `cmd.exe` 无法从 PowerShell 调用 → build.bat 的验证要在 bash 里逐条执行。

## 代码风格与架构

- 组件命名两套：`Element*` / `Button` 等老组件，与 `Ast*` 新组件（Ast = 对齐 Element Plus 的自绘实现）。
- 每个组件自带 `selfCheck()` + `main()` 跑断言；`build.bat` 逐个 `java -ea -cp out <类>` 串联，
  目前共 34 项（33 组件 + `AstTableDemo --selfcheck`）。新增功能必须同步加自检。
- 主题色 / 动效统一走 `org.swelement.core`（`ElementTheme`、`Animator`、`Easing`）。
- 对比度要求 WCAG 2.1 AA，用 `ElementTheme.assertContrast` 断言。
- Java 8 语法限制：非 static 内部类不能声明 `static` 方法；`BiFunction` 要 3 个类型参数
  （只用一个入参时用 `Function`）。

## 表格（AstTable / AstTableColumn / AstTableModel）

- 结构：`AstTable` 容器 = `HeaderView`(NORTH) + `BodyView`(CENTER) + `FooterView`(SOUTH)。
- 渲染三段式 clip：左冻结 / 中列 / 右冻结，各自只画本带列（中列带过滤器 `PRED_NONE`，
  传 `null` 会导致视口宽于内容时冻结列被画两次）。
- 公开行索引统一为「视图行」语义（排序/筛选后的下标）。
- BodyView 两遍绘制：Pass A 铺整行背景，Pass B 按冻结带 clip 画内容（否则合并跨行会被下一行背景覆盖）。
- 槽位遍历布局（数据行 + 展开块），不能假设行高均匀。

## 验证方法（强制）

- **写完断言必须做反向验证**：临时回退修复，确认断言真的 exit=1。只断言辅助谓词/字段而不触碰真实绘制或刷新路径的断言是空转，必须升级为绘制级/行为级断言。
- 视觉类问题（边框、对齐、颜色）当前模型无法读图核验，只能靠像素采样断言 + 提示用户运行 demo 自查。

## Git 工作流

- 功能按批次独立提交（C1…C10 各一个 commit），中文 commit message，正文列实现要点。
- 快进合并到 main 用 `git branch -f main <分支>`，**不要 `git checkout main`**（沙箱会中断并损坏工作树）。
- 推送双远程（gitee + origin）用 wincred 凭据 + `http.sslVerify=false`，推完用 `ls-remote` 复核。
