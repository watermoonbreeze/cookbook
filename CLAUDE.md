# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 公共 AI 上下文目录（必读）

本项目为 Claude Code / Codex 双模式开发，**公共规范、经验、功能文档、上下文记忆统一存放在 `.ai-context/`**（说明见 `.ai-context/README.md`）：

- **通用强制规则**：`.ai-context/rules/通用规则.md` —— 任务编排门禁、任务前快照、工程一致性、单元测试、AI 注释、构建环境等，**每次任务开始前遵守**
- **经验手册**：`.ai-context/docs/experience/`（索引 `INDEX.md`，工程统一规范见 `09_工程统一规范.md`）
- **功能/方案文档**：`.ai-context/docs/feature/`
- **上下文记忆**：`.ai-context/docs/context_memory/`（双端共写共读，任务快照与阶段结论都写这里）

`.claude/` 只保留 Claude Code 专属内容（settings.json、agents/、hook 薄包装）；公共内容一律放 `.ai-context/`，不再双份维护。

## 语言设置

**必须使用中文**与用户对话。

## 临时目录

除用户明确要求外，处理问题需要创建的临时文件放在 `temp/claude/`。

## 项目概述

Cookbook 是一款面向慢性病（三高、痛风等）患者的饮食规划 APP，核心价值是帮助用户解决"每天吃什么"的决策疲劳问题。基于 Kotlin Multiplatform (KMP) 跨平台架构，Android 端使用 Jetpack Compose，iOS 端使用 SwiftUI。

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

- **`:shared`** — 跨平台共享模块（`commonMain`/`androidMain`/`iosMain`），存放 Domain 层（Model、UseCase）和 Data 层（Repository、SQLDelight）。通过 `expect/actual` 适配各平台，iOS 端编译为静态 framework。
- **`:androidApp`** — Android 应用模块，依赖 `:shared`，仅负责 UI 层（Compose 页面、ViewModel、Theme、Navigation）。
- **`iosApp/`** — iOS 应用工程（Xcode/SwiftUI），调用 shared framework。

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
