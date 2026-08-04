# AGENTS.md

本文件为 Codex 在本仓库中工作时提供指导。Claude Code 使用根目录 `CLAUDE.md`。

## 公共 AI 上下文目录（必读）

本项目为 Claude Code / Codex 双模式开发，**公共规范、经验、功能文档、上下文记忆统一存放在 `.ai-context/`**（说明见 `.ai-context/README.md`）：

- **通用强制规则**：`.ai-context/rules/通用规则.md` —— 任务编排门禁、任务前快照、工程一致性、单元测试、AI 注释、构建环境等，**每次任务开始前遵守**
- **经验手册**：`.ai-context/docs/experience/`（索引 `INDEX.md`，工程统一规范见 `09_工程统一规范.md`）
- **功能/方案文档**：`.ai-context/docs/feature/`
- **上下文记忆**：`.ai-context/docs/context_memory/`（双端共写共读，任务快照与阶段结论都写这里）

## 双模式配置目录

| 用途 | 位置 |
|---|---|
| 公共规则/经验/功能文档/上下文记忆/公共 hook | `.ai-context/`（唯一来源） |
| Claude Code 入口与专属配置 | `CLAUDE.md`、`.claude/`（settings.json、agents/、hook 薄包装） |
| Codex 入口与专属配置 | `AGENTS.md`、`.codex/`（settings.json、agents/、hook 薄包装） |
| Claude 临时目录 | `temp/claude/` |
| Codex 临时目录 | `temp/codex/` |

原则（2026-07-03 起）：公共内容只维护 `.ai-context/` 一份，两侧入口都引用它；`.claude/` 与 `.codex/` 只保留各自工具必须放在原生目录的内容。旧的"两边各自维护等价副本"做法已废弃。

## 语言设置

**必须使用中文**与用户对话。

## 临时目录

除用户明确要求外，处理问题时需要创建的临时文件放在 `temp/codex/`。

## 真机验证清单维护

- 唯一清单位于 `.ai-context/docs/feature/真机待验证清单_<yyyyMMddHHmm>.md`。
- 有新的真机验证项时，**只更新这同一份文档**，再将文件名时间戳改为当次最新时间；不得复制或新建另一份验证清单。
- 交付时告知用户最新文件名，方便按时间戳定位。

## 项目概述

Cookbook 是一款面向慢性病（三高、痛风等）患者的饮食规划 APP，核心价值是帮助用户解决"每天吃什么"的决策疲劳问题。项目基于 Kotlin Multiplatform (KMP) 跨平台架构，Android 端使用 Jetpack Compose，iOS 端使用 SwiftUI。

MVP 三大核心功能（快速记录每餐、查看历史菜单、复用菜单）已完成，当前处于**功能扩展与打磨阶段**（食材体系、厨房小助手、搜索等已落地）。详细规划见 `docs/` 目录和 `.ai-context/docs/feature/`。

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

统一使用 CLI 构建脚本（显式 JDK 17，原理与换机说明见 `.ai-context/rules/通用规则.md` 第八节）：

```bash
# 构建 Android 应用（Windows）
scripts\build-cli.bat :androidApp:assembleDebug

# 运行 shared Android 单元测试（当前工程未注册 :shared:allTests）
scripts\build-cli.bat :shared:testDebugUnitTest

# 构建 shared 模块
scripts\build-cli.bat :shared:build

# 清理构建产物
scripts\build-cli.bat clean
```

macOS/Linux 使用 `./scripts/build-cli.sh <任务>`。直接 `./gradlew` 依赖全局 `org.gradle.java.home=jdk-17`，可用但不作为标准入口。IDE（AS Hedgehog）当前打开本项目会报模块实体错误，不作为构建路径。

## 规划文档

- `docs/菜谱功能.md` — 原始需求描述
- `docs/产品规划方案.md` — 完整产品规划（MVP → 一期 → 二期）
- `docs/MVP开发规划.md` — MVP 详细开发任务、数据模型、页面设计
- `docs/技术栈与主题风格.md` — 技术选型与 Material3 主题配色规范
- `.ai-context/docs/feature/` — 实施方案（MVP 实施、数据库设计、食材体系重构、端侧 AI、UI 控件命名清单等）

## 工程一致性要求

- **任务编排强制门禁**、**任务前上下文快照**、**单元测试纳入流程**、**AI 注释规范**等通用规则统一见 `.ai-context/rules/通用规则.md`，Codex 必须遵守；其中任务编排流程定义读 `~/.codex/memories/workflow_auto_orchestration.md`，标准/深度任务按流程用 Codex 子代理映射 DEV 角色参与，不得直接由主线程跳过分派。
- 开发前必须按需读取 `.ai-context/docs/experience/09_工程统一规范.md`，保证 KMP 分层、平台 UI 风格、数据库和代码风格一致。
- 修改数据库、主题、架构边界或公共组件时，同步更新对应 `.ai-context/docs/feature/` 文档。

## 自定义命令兼容

Codex 不保证自动注册 Claude Code 风格自定义 slash command。用户输入 `/myinit`、`/zongjie`、`/fansi` 时，按全局 Codex 命令档案执行：

- `/myinit`：读取 `~/.codex/commands/myinit.md`
- `/zongjie`：读取 `~/.codex/commands/zongjie.md`
- `/fansi`：读取 `~/.codex/commands/fansi.md`

执行这些命令产出的项目级公共内容（经验、记忆、文档）写入 `.ai-context/`；仅 Codex 自身可识别的配置写 `.codex/`。
