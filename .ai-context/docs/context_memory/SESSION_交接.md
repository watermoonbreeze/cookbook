# 🔖 SESSION 交接入口

> 更新时间：**2026-08-06 19:15**
> 当前状态：**AF-ARCH-01 / AF-ARCH-02 已修复并推送**（`b8a90121`，623 tests 0 failures）；**AF-ARCH-03 待 B4 蓝图冻结决策**。B3 蓝图可从 BLOCKED 推进。
> 末位提交：`b8a90121`（AF-ARCH-01 + AF-ARCH-02 修复）

---

## 一、当前已完成

### B1 协议层（已通过八审 + 本次死代码清理 + AF-ARCH-01）
- `InputSegment`、`NdjsonEvents`、`StreamingMealParser`、`AiMealPrompt`、`LlmStreamEvent`
- B3.4 清理：`jsonFallbackDays`/`buildDraftFromJsonFallback` 死代码（-40行）、`DoneEvent`/`SegmentDraft.done` 死字段（-12行）
- **AF-ARCH-01 修复**：`processLine()` when 块加 `"done" -> {}`，静默消费段结束标记

### B2 Runtime（已通过八审 · 无改动）
- `CloudAiRequestConfig` + `StreamTransport/StreamCall` + `CloudAiRuntime.stream()`

### B3 会话层（四视角联合审查 → 全部修复 → 架构模型终审 → AF-ARCH-02 修复）

**B3.3 基线**：`449858e2`（AF-B3-R3-01~05 全部关闭）

**B3.4 修复提交**：`d94e7d8f`（8 文件 +188/-252 · 净删 64 行）

**AF-ARCH-02 修复提交**：`b8a90121`（StreamingMealSession 重构为按 segmentId 惰性 parser）

**首轮四视角审查**（Google 质量 + Google 架构 + Apple 质量 + Apple 架构）：

| 等级 | 数量 | 已修复 |
|------|:----:|:------:|
| 🔴 阻断 | 5 | ✅ 全部 |
| 🟡 建议 | 9 | ✅ 全部 |
| ⚪ 可选 | 9 | —（非阻塞） |
| B1/B2 死代码 | 2 | ✅ 全部 |

**架构模型终审**（google_architecture_engineer + apple_architect 双视角）：

| AF | 问题 | 状态 |
|---|---|:--:|
| **AF-ARCH-01** | `done` 事件落入 else 产"未知事件类型"警告 | ✅ 已修复 |
| **AF-ARCH-02** | 单 parser 服务全部 segments，B4 多段 fallback 永久失效 | ✅ 已修复 |
| **AF-ARCH-03** | `buildStreamingRequest` 单段/多段合一矛盾 | 🔧 待 B4 蓝图 |
| S1~S6 | 6 项建议 | 🔧 待 B4 蓝图处理 |

### 测试结果
```
Shared:  623 tests, 0 failures（新增 3 个：AF-ARCH-01 x1 + AF-ARCH-02 x2）
Android:  22 tests, 0 failures
Total:   645 tests, 0 failures
```

---

## 二、下一步（按顺序）

1. ~~找编码模型关闭 AF-ARCH-01、AF-ARCH-02~~ ✅ 已完成（`b8a90121`，623 tests 0 failures）
2. 起草 B4 实施蓝图时：把 AF-ARCH-03 的决策写入蓝图 §1/§不变量表；把 §11.3 的 S1~S6 逐条显式处理（接受方案或不采纳理由都要写）
3. 蓝图 §11.6 全部满足后，把 B3 蓝图状态由 `BLOCKED` 改 `ACCEPTED`，才可开始 B4 编码

AF-ARCH-01/02 已关闭，**可以开始起草 B4 蓝图**（AF-ARCH-03 的决策作为蓝图第 1 步冻结）。

---

## 三、先读清单（下个 session 接手时按序读）

1. `SESSION_交接.md`（本文件）
2. `.ai-context/PROJECT.md`
3. `feature/AI记一餐_周期记_NDJSON流式开发规范.md`
4. `feature/AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md`
5. `feature/AI记一餐_周期记_NDJSON流式改造落地方案.md`
6. `context_memory/2026-08-05_AI记一餐周期记NDJSON流式改造.md`（全流程台账）
7. `.ai-context/docs/功能路径索引.md`

## 四、B3 代码文件速查（当前状态）

| 文件 | 角色 | 状态 |
|------|------|:--:|
| `shared/.../ai/meallog/MealStreamDraftMapper.kt` | 纯 mapper | ✅ 清理 |
| `shared/.../ai/meallog/StreamingMealSession.kt` | reducer | ✅ AF-ARCH-02 已修复 |
| `shared/.../ai/meallog/StreamingMealParser.kt` | parser | ✅ AF-ARCH-01 已修复 |
| `shared/.../ai/meallog/NdjsonEvents.kt` | 事件类型 | ⚪ 架构审：NdjsonEvent 族死代码待清 |
| `shared/.../ai/meallog/AiMealPrompt.kt` | prompt | 🔧 架构审：AF-ARCH-03 待 B4 蓝图冻结决策 |
| `androidApp/.../ai/CloudAiRuntime.kt` | Runtime | ✅ 架构审通过 |
| `androidApp/.../ai/StreamTransport.kt` | transport | ✅ 架构审通过 |
| `androidApp/.../ai/CloudAiRequestConfig.kt` | config adapter | ✅ 架构审通过 |
| `androidApp/.../ui/ai/AiMealInputViewModel.kt` | ViewModel 会话链 | ✅ 修复 |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | UI | ✅ 修复 |
| `shared/.../test/.../StreamingMealSessionTest.kt` | Session 测试 | ✅ +3 新增 |
| `androidApp/.../test/.../AiMealInputViewModelStreamTest.kt` | ViewModel 测试 | ✅ |
| `shared/.../test/.../MealStreamDraftMapperTest.kt` | Mapper 测试 | ✅ |

---

## 五、关键红线（不变）

- `GENERATING`/`PARTIAL_READY` 期间绝不写库（I-01）
- 只有用户确认+二次 MERGE 后才写库（I-02）
- `finish_reason=length` 必须可见（I-03）
- 归属不完整事件不静默挂靠（I-04）
- 日期遵循 D-15（I-05）
- Release 不含原文/Key/响应（I-06）

## 六、B3 复审关键注意事项

1. **save 协程管理**：save 协程赋给 `generationJob`，`invalidateGenerationToInput` 会 cancel 它；`_state.update` 内 `phase==SAVING` 守卫防竞态覆盖
2. **健康摘要容错**：`buildHealthSafetyReport` 用 `runCatching`+`NonCancellable` 独立容错，失败降级默认报告
3. **Sheet 关闭守卫**：`PARTIAL_READY`/`PREVIEW_READY` 弹确认框；`SAVING` 阶段禁止关闭
4. **retrySave 并发防护**：入口 `phase!=ERROR` 门禁 + `generationJob?.cancel()` 防连点双写
5. **.copy() 粘性字段**：`invalidateGenerationToInput` 改用 `prev.copy()`，`inputMode`/`voiceState` 等自动继承
6. **stream 异常防护**：`collect{}` 外层 try-catch，非 Cancel 异常记录为段失败不静默丢失
7. **死代码已清**：旧同步路径（-140 行）、PENDING/cancel（-23 行）、jsonFallbackDays（-40 行）、DoneEvent（-12 行）
8. **AF-ARCH-02 parser 惰性创建**：`segmentParsers: LinkedHashMap`，`nextSegment()` 中 `getOrPut`，每个 parser 只持自己单个 segment，snapshot 合并
