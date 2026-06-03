# AGENTS.md

本文件为 Codex 在本仓库中工作时提供指导。Claude Code 使用根目录 `CLAUDE.md`；两边配置相互独立，不互相读取，但核心项目规范应保持等价。

## 语言设置

**必须使用中文**与用户对话。

## 临时目录

除用户明确要求外，处理问题时需要创建的临时文件放在 `temp/codex/`。如任务需要兼容 Claude Code，可同步写入 `temp/claude/` 或在结果中说明只使用了 Codex 临时目录。

## 双模式配置目录

| 用途 | Claude Code | Codex |
|---|---|---|
| 项目主入口 | `CLAUDE.md` | `AGENTS.md` |
| 项目配置目录 | `.claude/` | `.codex/` |
| 上下文记忆 | `.claude/docs/context_memory/` | `.codex/docs/context_memory/` |
| 经验手册 | `.claude/docs/experience/` | `.codex/docs/experience/` |
| 功能文档 | `.claude/docs/feature/` | `.codex/docs/feature/` |
| Hook 脚本档案 | `.claude/hooks/` | `.codex/hooks/` |
| Agent 档案 | `.claude/agents/` | `.codex/agents/` |

原则：Codex 工作时只依赖 `AGENTS.md` 和 `.codex/`；Claude Code 工作时只依赖 `CLAUDE.md` 和 `.claude/`。当需要双模式一致时，将同一条规则按两边各自格式分别维护，而不是让任一侧读取另一侧配置。

## 项目概述

Cookbook 是一款面向慢性病（三高、痛风等）患者的饮食规划 APP，核心价值是帮助用户解决“每天吃什么”的决策疲劳问题。项目基于 Kotlin Multiplatform (KMP) 跨平台架构，Android 端使用 Jetpack Compose，iOS 端使用 SwiftUI。

当前处于 **MVP 阶段**，聚焦三个核心功能：快速记录每餐、查看历史菜单、从历史中复用菜单。详细规划见 `docs/` 目录和 `.codex/docs/feature/`。

## 技术栈

- **Kotlin**: 1.9.20，**AGP**: 8.2.2
- **Android UI**: Jetpack Compose 1.5.4 + Material3 1.1.2
- **数据库**: SQLDelight（跨平台 SQLite），**依赖注入**: Koin
- **构建工具**: Gradle (Kotlin DSL)，版本目录 (`gradle/libs.versions.toml`)
- **包名**: `com.sxdbsm.cookbook`（shared），`com.sxdbsm.cookbook.android`（Android App）
- **最低 Android SDK**: 21，**目标/编译 SDK**: 34，**JVM Target**: 1.8
- **Maven 仓库**: 优先使用阿里云镜像

## 架构

采用 Clean Architecture 简化版（UI / Domain / Data 三层），UI 与业务逻辑按模块分离：

- `:shared` — 跨平台共享模块（`commonMain`/`androidMain`/`iosMain`），存放 Domain 层（Model、UseCase）和 Data 层（Repository、SQLDelight）。通过 `expect/actual` 适配各平台，iOS 端编译为静态 framework。
- `:androidApp` — Android 应用模块，依赖 `:shared`，仅负责 UI 层（Compose 页面、ViewModel、Theme、Navigation）。
- `iosApp/` — iOS 应用工程（Xcode/SwiftUI），调用 shared framework。

## 常用命令

```bash
# 构建 Android 应用
./gradlew :androidApp:assembleDebug

# 构建 shared 模块
./gradlew :shared:build

# 运行 shared Android 单元测试（当前工程未注册 :shared:allTests）
./gradlew :shared:testDebugUnitTest

# 仅运行 Android 单元测试
./gradlew :shared:testDebugUnitTest

# 清理构建产物
./gradlew clean
```

## 规划文档

- `docs/菜谱功能.md` — 原始需求描述
- `docs/产品规划方案.md` — 完整产品规划（MVP → 一期 → 二期）
- `docs/MVP开发规划.md` — MVP 详细开发任务、数据模型、页面设计
- `docs/技术栈与主题风格.md` — 技术选型与 Material3 主题配色规范
- `.codex/docs/feature/MVP实施方案.md` — MVP 实施方案
- `.codex/docs/feature/数据库设计方案.md` — 数据库设计方案
- `.codex/docs/feature/界面探讨.md` — 界面方案和交互讨论
- `.codex/docs/experience/09_工程统一规范.md` — KMP 架构、Android/iOS UI、数据库、代码风格与开发流程规范

## 工程一致性要求

- **任务编排强制门禁**：[AI修改] 只要用户下达的是一个任务（包括开发、修复、优化、调研、文档、配置、审核等），开始执行前必须先走 `~/.codex/memories/workflow_auto_orchestration.md` 的阶段0评估定级；必须先告诉用户本次采用的模式/级别、原因、是否需要智能体分派。不得在未声明任务编排结果的情况下直接自行处理。
- 开发类任务必须执行 Codex 自动任务编排流程：先读取 `~/.codex/memories/workflow_auto_orchestration.md`，完成阶段0评估定级并输出智能体分派表；标准/深度任务必须按流程用 Codex 子代理映射 DEV 角色参与，不得直接由主线程跳过分派。
- 开发前必须按需读取 `.codex/docs/experience/09_工程统一规范.md`，保证 KMP 分层、平台 UI 风格、数据库和代码风格一致。
- Android UI 采用 Material Design 3 与既有 Theme/ExtendedColors；iOS UI 采用 SwiftUI 与 iOS 原生交互风格。
- 新增跨平台业务能力优先进入 `shared`，平台模块只承接各自 UI 与平台适配。
- 修改数据库、主题、架构边界或公共组件时，同步更新对应 `.codex/docs/` 文档。

## 上下文保护机制

- 只要用户下达的是会执行命令、修改文件或产生项目决策的任务，阶段0任务编排声明之后、阶段1分析或实质操作之前，必须先保存“任务前上下文快照”到 `.codex/docs/context_memory/`。
- 任务前上下文快照至少记录：用户最新需求、任务模式/级别、计划分派角色、当前已知项目状态、预计涉及文件/模块、主要风险和待验证项。
- 纯问答且不执行命令、不修改文件、不产生项目决策时，可以不写快照，但需要在回复中明确说明原因。
- 在长对话中，每完成一个重要任务节点后，主动将阶段性结论保存到 `.codex/docs/context_memory/`。
- 双模式一致性任务中，如用户明确要求两边都可接续使用，则将同一份关键摘要按 Claude Code 目录规则另存到 `.claude/docs/context_memory/`；Codex 不读取 `.claude` 作为自身上下文来源。
- 文件按主题或任务命名，只存结论、决策、待办、关键路径，不存冗长过程。
- 单个文件控制在 100 行以内，超过时按子主题拆分。
- 读取时按需加载当前任务相关文件，不全量读取。

## 自定义命令兼容

Codex 不保证自动注册 Claude Code 风格自定义 slash command。用户输入 `/myinit`、`/zongjie`、`/fansi` 时，按全局 Codex 命令档案执行：

- `/myinit`：读取 `~/.codex/commands/myinit.md`
- `/zongjie`：读取 `~/.codex/commands/zongjie.md`
- `/fansi`：读取 `~/.codex/commands/fansi.md`

执行这些命令时保持双模式独立：Codex 侧写 `.codex` / `AGENTS.md`；如果用户要求 Claude Code 也具备同等规则，再按 Claude Code 格式写入 `.claude` / `CLAUDE.md`。
