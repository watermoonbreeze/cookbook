# 优化计划评估结论

时间：2026-06-25。[AI生成]

## 评估对象

- `temp/codex/optimization_plan.md`

## 结论分级

### 必须优先处理

- 存储权限与 Google Play 合规：当前 AndroidManifest 仍声明 `MANAGE_EXTERNAL_STORAGE`，且 MainActivity/CookbookStorage 依赖 `/sdcard/cookbook` 公共目录授权。若后续上架或分发给非测试用户，应迁移到 app 专属目录，并用 SAF 做导出/备份。

### 建议近期处理

- 协程调度器规范化：shared 层大量 Repository 使用 `Dispatchers.Default` 执行 SQLDelight IO，建议新增 KMP `ioDispatcher` expect/actual 后替换。

### 可排期处理

- KMP 原生日志：当前日志主要在 Android App 层，commonMain 暂无统一日志能力。若 iOS 或 shared 排查需求增加，再引入 Napier/Kermit。
- SQLDelight 全量替换策略：当前适合 MVP，食材详情/分类变多后可对高频编辑链路做 diff 更新。
- Koin 模块解耦：当前 `DatabaseDriverFactory` 由平台注册、sharedModule 依赖 get，结构可用；不是阻塞问题。

### 暂不建议处理

- ViewModel 移到 shared：当前 Android UI 仍快速变化，迁移 shared ViewModel 会提高复杂度。建议等 Android 功能稳定、iOS 要同步实现时再做。

## 推荐执行顺序

1. 存储目录合规迁移。
2. shared IO dispatcher。
3. 食材/菜品高频保存 diff 优化。
4. KMP 日志。
5. DI 模块整理。
6. shared ViewModel 长期化。
