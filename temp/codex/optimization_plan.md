# Cookbook 项目代码优化与 KMP 最佳实践方案

**任务背景**：对现有代码进行审核，基于 Kotlin Multiplatform (KMP) 最佳实践提出优化建议。
**当前阶段**：MVP 开发中

---

## 1. 协程调度器规范化 (Coroutine Dispatchers)
*   **问题**：Shared 层目前硬编码使用 `Dispatchers.Default` 处理所有异步任务（包括 DB IO）。
*   **目标**：在 Android 端利用 `Dispatchers.IO` 提高 IO 效率，同时保持跨平台兼容性。
*   **实施建议**：
    1.  在 `commonMain` 定义 `expect val ioDispatcher: CoroutineDispatcher`。
    2.  在 `androidMain` 实现 `actual val ioDispatcher = Dispatchers.IO`。
    3.  在 `iosMain` 实现 `actual val ioDispatcher = Dispatchers.Default` (或特定 Native 调度器)。
    4.  替换 `DishRepository` 等类中的 `Dispatchers.Default`。

## 2. 存储权限与 Google Play 合规性
*   **问题**：`MainActivity` 申请了 `MANAGE_EXTERNAL_STORAGE`。
*   **风险**：该权限在应用商店审核中极难通过。
*   **优化路径**：
    *   **方案 A (推荐)**：迁移数据至 `Context.getExternalFilesDir()` 或 `Context.getFilesDir()`，无需运行时权限。
    *   **方案 B**：如需外部备份，使用 `Storage Access Framework (SAF)` 调用系统选择器进行单文件导出。

## 3. KMP 原生日志系统
*   **问题**：`AppLogger` 仅限 Android，`commonMain` 缺乏调试日志。
*   **目标**：引入支持双端的日志库。
*   **库推荐**：`Napier` 或 `Kermit`。
*   **操作**：替换 `commonMain` 中的 `println` 或空缺，统一日志输出。

## 4. 数据持久化性能 (SQLDelight)
*   **问题**：标签和食材采用“全量替换（删除再插入）”策略。
*   **优化**：
    *   实现简单的 `diff` 逻辑，仅更新变化的项。
    *   减少数据库触发器和 UI 监听器的无效刷新频率。

## 5. 依赖注入 (Koin) 模块解耦
*   **目标**：将平台相关实现（如驱动工厂）移入各平台 module。
*   **操作**：
    *   定义 `expect fun platformModule(): Module`。
    *   在 `androidMain` 返回包含 `DatabaseDriverFactory` 实现的 module。
    *   在 `SharedModule.kt` 中通过 `includes(platformModule())` 整合。

## 6. ViewModel 逻辑复用 (长期)
*   **建议**：随着逻辑变复杂，考虑将 `HomeViewModel` 等移至 `shared/commonMain`。
*   **技术栈**：使用 `androidx.lifecycle:lifecycle-viewmodel:2.8.0+`（现已官方支持 KMP）。

---
**后续行动**：可根据此文档逐项建立分任务进行处理。
