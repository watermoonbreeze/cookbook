# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 语言设置
  **必须使用中文**与用户对话

## 临时目录
  **除用户明确要求外处理解决问题需要创建的文件都必须放在temp/claude/这个临时目录下**

## 项目概述

Cookbook 是一款面向慢性病（三高、痛风等）患者的饮食规划 APP，核心价值是帮助用户解决"每天吃什么"的决策疲劳问题。基于 Kotlin Multiplatform (KMP) 跨平台架构，Android 端使用 Jetpack Compose，iOS 端使用 SwiftUI。

当前处于 **MVP 阶段**，聚焦三个核心功能：快速记录每餐、查看历史菜单、从历史中复用菜单。详细规划见 `docs/` 目录。

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

```bash
# 构建 Android 应用
./gradlew :androidApp:assembleDebug

# 构建 shared 模块
./gradlew :shared:build

# 运行共享模块通用测试
./gradlew :shared:allTests

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

## 上下文保护机制

- 在长对话中，每完成一个重要任务节点（如完成一次分析、解决一个问题、做出关键决策）后，主动将该阶段的关键信息（分析结论、决策依据、待办事项、关键代码路径等）保存到当前项目下的 `.claude/docs/context_memory/` 目录
- 按主题或任务命名文件，内容只存结论性信息，不存过程细节，确保脱离对话也能理解
- 单个文件控制在100行以内，超过时按子主题拆分为多个文件
- 当感知到对话较长（如已经超过多轮复杂交互）时，应加大保存频率
- 读取时按需加载当前任务相关的文件，不全量读取
- 新对话涉及相关主题时，顺便检查并清理过时内容
- 此目录存放项目相关的具体分析结果；跨项目通用的经验和模式仍存放在 auto memory 中
