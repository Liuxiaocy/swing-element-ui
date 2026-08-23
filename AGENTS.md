# Superpowers 开发方法论

本项目已安装 [Superpowers](https://github.com/obra/superpowers) 软件开发方法论技能集。

## 核心规则

在开始任何开发任务之前，**必须先调用 `using-superpowers` 技能**，它会指导你在合适的时机调用对应的方法论技能。

## 组件设计规范（所有组件必须遵循）

详细规范见 `docs/superpowers/specs/component-design-guidelines.md`。

### 可访问性与对比度（最高优先级）

**任何状态下，文字颜色与背景色的对比度必须满足 WCAG 2.1 AA 级（≥ 4.5:1）。**

- 工具：`ElementTheme.assertContrast(fg, bg, where)` 在 `java -ea` 自检中断言
- 适用状态：默认、hover、active、disabled、loading、plain、text 等所有组合
- 禁止：浅色背景配白色/浅色文字（如 plain 模式白字白底、disabled 浅灰字浅蓝底）
- 每个组件的 `selfCheck()` 必须包含所有状态的对比度断言

## 工作流

1. **brainstorming** — 写代码前先澄清需求，通过提问细化设计，分段展示设计方案供确认
2. **writing-plans** — 设计确认后，拆分成 2-5 分钟的小任务，每个任务包含精确文件路径、完整代码和验证步骤
3. **subagent-driven-development** 或 **executing-plans** — 按计划执行，每个任务派遣独立子 agent 并经过两阶段审查
4. **test-driven-development** — 严格 RED-GREEN-REFACTOR：先写失败测试，再写最小代码通过，最后重构
5. **requesting-code-review** — 任务间进行代码审查，按严重程度报告问题，严重问题阻塞进度
6. **verification-before-completion** — 声称完成前必须运行验证，证据先行
7. **finishing-a-development-branch** — 任务完成后验证测试，决定合并/PR/保留/丢弃

## 调试

遇到 bug 时使用 **systematic-debugging** 技能：四阶段根因定位法（定位→分析→假设→修复），而非猜测式调试。

## 技能优先级

当多个技能适用时，流程技能优先（设定方法），然后是实现技能。
- "构建 X" → 先 brainstorming，再实现技能
- "修复 bug" → 先 systematic-debugging，再领域技能

## 例外

用户明确指示跳过某个流程时可以跳过，但需确认用户理解后果。
