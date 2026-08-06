# 🔖 SESSION 交接入口

> 更新时间：**2026-08-06 08:30**
> 当前状态：B3.3 补漏修复完成（`449858e2`）；AF-B3-R3-01~05 全部真正关闭，三条非缓存命令当次通过；待架构模型定向复审；B4 未开始。

---

## 一、当前已完成

### B1 协议层（已通过）
- `InputSegment`、`NdjsonEvents`、`StreamingMealParser`、`AiMealPrompt`、`LlmStreamEvent`
- Parser 测试 27 条

### B2 Runtime（已通过）
- `CloudAiRequestConfig` + `StreamTransport/StreamCall` + `CloudAiRuntime.stream()`
- `HttpUrlStreamCall`（取消标记+三处检查+安全 IO 包装+HTTP 重试分类）
- Runtime 测试 13 条

### B3 会话层（AF-B3-R3-01~05 全部关闭）

提交链：`d45e9aa7` → `ada6748f` → `38cae283` → `d4384040` → **`449858e2`**

核心交付：
- `MealStreamDraftMapper`（纯 mapper，ordinal 遍历+同日合并+去重）
- `StreamingMealSession`（reducer，ordinal 排序+终态门控+STREAM_ENDED_WITHOUT_TERMINAL）
- `AiMealInputViewModel` 会话链：
  - `AiMealPhase` 7 态
  - `generationJob` + `generationCounter` + `isCurrentGeneration` 谓词
  - `useRuleFallback` 纳入 generationJob + 四点 generation 复查（**R3-01 补漏：空结果分支加 isCurrentGeneration**）
  - `buildHealthSafetyReport` 恢复基线（无 healthSummaryProvider，无 try-catch）
  - `confirmSave`（frozenPreview + 原子清会话 + port.commit）
  - `dismissError` 走 invalidateGenerationToInput
- ViewModel 测试 9 条（T-B3-06 A→B gate、T-B3-06a preview 编辑、T-B3-08 两轮 preview 保存+hasExisting、T-B3-08b ERROR+preview fallback 拒绝）
- Session 测试 8 条（T-B3-02 Failed 门控独立 session2、T-B3-04 前缀保留）
- Mapper 测试 7 条

### 全部测试：约 500 条，0 失败
- 三条非缓存命令当次全部通过（2026-08-06 08:30）

### AF-B3-R3-01~05 关闭映射

| AF | 状态 | 证据 |
|----|------|------|
| R3-01 | ✅ | generationJob + fallbackGenerationId + 四点 isCurrentGeneration（含补漏）+ CancellationException 重抛 + dismissError→invalidate |
| R3-02 | ✅ | 无 healthSummaryProvider；buildHealthSafetyReport 直接 healthSummaryLabels；port 严格三方法 |
| R3-03 | ✅ | T-B3-06 gate A→B、T-B3-08 两轮+hasExisting+保存P、T-B3-08b ERROR+preview fallback拒绝、GatedPreviewPort 单次计数 |
| R3-04 | ✅ | T-B3-02 Completed+Failed 双门控独立 session |
| R3-05 | ✅ | 三条非缓存命令串行当次成功 |

---

## 二、下个 session 的主任务

### 任务：B3 定向复审 → 通过后启动 B4

> **B3 代码已全部完成**，等待架构模型对 `449858e2` 做定向复审。
> 实施基线：`449858e2`（B3.3 补漏）
> B1/B2 代码基线：`b37ace6f`（八审通过）
> **B4 不可开始**，直到 B3 复审通过。

### 先读清单

1. `.ai-context/PROJECT.md`
2. `feature/AI记一餐_周期记_NDJSON流式开发规范.md`
3. `feature/AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md`（§8~§10）
4. `feature/AI记一餐_周期记_NDJSON流式改造落地方案.md`
5. `context_memory/2026-08-05_AI记一餐周期记NDJSON流式改造.md`（全流程台账含 B3.3 补漏）
6. `.ai-context/docs/功能路径索引.md`

### 当前 B3 代码文件速查

| 文件 | 角色 |
|------|------|
| `shared/.../ai/meallog/MealStreamDraftMapper.kt` | 纯 mapper，不许改 |
| `shared/.../ai/meallog/StreamingMealSession.kt` | reducer，不许改 |
| `shared/.../ai/meallog/StreamingMealParser.kt` | parser，不许改 |
| `shared/.../ai/meallog/NdjsonEvents.kt` | 事件类型，不许改 |
| `androidApp/.../ai/CloudAiRuntime.kt` | Runtime，不许改 |
| `androidApp/.../ai/StreamTransport.kt` | transport，不许改 |
| `androidApp/.../ai/CloudAiRequestConfig.kt` | config adapter，不许改 |
| `androidApp/.../ui/ai/AiMealInputViewModel.kt` | B3 可改 |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | B3 可改（编译适配） |
| `shared/.../test/.../StreamingMealSessionTest.kt` | B3 可改 |
| `androidApp/.../test/.../AiMealInputViewModelStreamTest.kt` | B3 可改 |
| `shared/.../test/.../MealStreamDraftMapperTest.kt` | B3 可改 |

---

## 三、关键红线（不变）

- `GENERATING`/`PARTIAL_READY` 期间绝不写库（I-01）
- 只有用户确认+二次 MERGE 后才写库（I-02）
- `finish_reason=length` 必须可见（I-03）
- 归属不完整事件不静默挂靠（I-04）
- 日期遵循 D-15（I-05）
- Release 不含原文/Key/响应（I-06）

---

## 四、B3 复审关键注意事项

1. **generation 复查**——任何挂起点恢复后必须 `isCurrentGeneration(id)`，false 直接 return
2. **健康摘要基线**——`buildHealthSafetyReport` 直接调 `healthSummaryLabels()`，无 seam/try-catch；用 `NonCancellable` 包裹
3. **port 严格三方法**——`preview/commit/parseRule`
4. **useRuleFallback 四点 isCurrentGeneration**——parseRule 后 / preview 后 / **空结果写 ERROR 前** / 异常写 ERROR 前（本次补漏）
5. **confirmSave 局部 frozenPreview**——先冻结再取消 generation
6. **dismissError 走 invalidate**——不保留仅改 phase 的分支
