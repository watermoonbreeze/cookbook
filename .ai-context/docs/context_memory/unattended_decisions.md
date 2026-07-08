# 无人值守决策记录

> 用户 2026-07-08 下班启动无人值守。指令：按待办顺序逐步执行；**暂无上架计划，P0 存储合规不做**；其他继续。
> 规则：深度执行；高风险/需真机 → 待确认队列不擅自做；每阶段 `[unattended]` 提交不 push；≤15 文件/阶段。

## 执行计划（安全优先，可编译/单测验证的工程任务先做）

| 顺序 | 任务 | 判定 | 处置 |
|---|---|---|---|
| 1 | P1 Shared IO Dispatcher | 低风险纯重构，build+test 可验 | ✅ 执行 |
| 2 | DI 平台化 `platformModule` | 低风险，build 可验 | ✅ 执行 |
| 3 | P3 KMP 日志 | 加依赖(Kermit)+集成，中等 | ✅ 执行(评估后) |
| 4 | P2 SQLDelight 写入 Diff | 改写库逻辑，涉数据，中高 | ⏸ 评估风险后定 |

## 待确认队列（高风险/需真机，未擅自执行）
- **首页搜索"跳到具体食材并高亮"**：跨底部 Tab 传参 + 高亮，Compose 跨 Tab 状态易出错，无真机难验证。
- **回归验证（删除一致性 / 图片持久化）**：本质是真机专项验证，无设备无法执行；仅可做代码级审阅。
- **iOS shared 编译能力**：启用 iOS target 需补多处 actual + framework 验证，改动大且无 iOS 环境验证。
- **selectionMode 完整两入口拆分**：高风险重构，picker 在菜品选食材关键流程，需真机回归。
- **AI S4 端侧模型(LiteRT-LM)**：大工程，占位已留，待专门规划。
- **P0 存储权限合规**：用户明确暂不做（无上架计划）。

## 决策/进展日志

- **阶段1 ✅ P1 IO Dispatcher**（commit `[unattended]`）：新增 `expect val ioDispatcher` + Android actual=Dispatchers.IO；8 repo + RecommendationDataSource 共 91 处 `Dispatchers.Default`→`ioDispatcher`；build+单测过。
- **阶段2 ⏸ DI 平台化 → 待确认**：shared 只有 koin.core（无 koin-android）。移平台注册进 shared androidMain 需加 koin-android 依赖 + 用 androidContext()，且 DI 解析错误是**运行时崩溃**、无设备编译验不出——无人值守不擅自做。
- **阶段3 代码审计**（删除一致性 + 图片持久化）：派 Explore 只读审计，按发现做安全修复。
