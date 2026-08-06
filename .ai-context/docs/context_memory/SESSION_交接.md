# 🔖 SESSION 交接入口

> 更新时间：**2026-08-06 18:30**
> 当前状态：**架构模型终审已完成**（google_architecture_engineer + apple_architect 双视角审查 B1+B2+B3 全量代码，逐条代码验证）；发现 **3 项 🔴 阻断（AF-ARCH-01~03）+ 6 项 🟡 建议（S1~S6）**，结论 **BLOCKED——须先关闭 AF-ARCH-01~03 才能起草 B4 实施蓝图**。完整审核结论见 `feature/AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md` §11；B1 侧交叉记录见 `feature/AI记一餐_周期记_NDJSON流式改造落地方案.md` §7.10。
> 末位提交：`d94e7d8f`（代码未改动，本次为纯审核，仅改文档）

---

## 一、当前已完成

### B1 协议层（已通过八审 + 本次死代码清理）
- `InputSegment`、`NdjsonEvents`、`StreamingMealParser`、`AiMealPrompt`、`LlmStreamEvent`
- 本次清理：`jsonFallbackDays`/`buildDraftFromJsonFallback` 死代码（-40行）、`DoneEvent`/`SegmentDraft.done` 死字段（-12行）

### B2 Runtime（已通过八审 · 无改动）
- `CloudAiRequestConfig` + `StreamTransport/StreamCall` + `CloudAiRuntime.stream()`

### B3 会话层（四视角联合审查 → 全部修复 → 终审通过）

**B3.3 基线**：`449858e2`（AF-B3-R3-01~05 全部关闭）

**B3.4 修复提交**：`d94e7d8f`（本次 · 8 文件 +188/-252 · 净删 64 行）

**首轮四视角审查**（Google 质量 + Google 架构 + Apple 质量 + Apple 架构 + 我）：

| 等级 | 数量 | 已修复 |
|------|:----:|:------:|
| 🔴 阻断 | 5 | ✅ 全部 |
| 🟡 建议 | 9 | ✅ 全部 |
| ⚪ 可选 | 9 | —（非阻塞） |
| B1/B2 死代码 | 2 | ✅ 全部 |

**阻断修复清单**：
| 修复 | 文件 | 内容 |
|------|------|------|
| R4-01 | ViewModel | `confirmSave` save 协程 assign generationJob + CancellationException 重抛 + `phase==SAVING` 守卫 |
| R4-02 | ViewModel | `buildHealthSafetyReport` 独立 `runCatching`，失败降级默认报告，预览不连坐 |
| R4-03 | Sheet+VM | Sheet 关闭未保存守卫（PARTIAL_READY/PREVIEW_READY 弹确认框；SAVING 禁止关闭） |
| R4-04 | Sheet+VM | 保存失败后"重试保存"按钮 + `retrySave()` + phase 门禁 + 连点并发防护 |
| R4-05 | Sheet+VM | ERROR 态传入 `parseWarnings` 诊断 + ErrorPhase 展示诊断卡片（最多 3 条） |

**建议修复清单**：
| 修复 | 内容 |
|------|------|
| R4-06 | `invalidateGenerationToInput` 改用 `prev.copy()` 保留粘性字段 |
| R4-07 | `stream().collect{}` 外层 try-catch（非 Cancel 异常记录为段失败） |
| R4-08 | 删除旧解析路径死代码（`parseToDayMealJsonList`/`parseWithAi`/`ParsedDays`/`weekdayChinese`·-140 行） |
| R4-09 | 删除 `PENDING` 枚举 + `cancel()` 方法 + `cancelled` 字段 + `CANCELLED` |
| R4-10 | 删除 Mapper `knownSegmentIds` 恒真检查；删除 `snapshot()` 冗余 `.toList()` |
| R4-11 | 删除 jsonFallbackDays 死代码（Parser · -40 行） |
| R4-12 | 删除 `DoneEvent`/`SegmentDraft.done`/`MutableSegmentDraft.done`/`handleDoneEvent` |
| R4-13 | `retrySave` 连点并发：`phase!=ERROR` 门禁 + `generationJob?.cancel()` |
| R4-14 | SAVING 阶段 Sheet 关闭守卫 |

**终审结论**（Google 质量 + Apple 质量）：
- Google 质量终审：**可交付，无阻断**。1 条 🟡=NdjsonLine.summary 死字段（已顺手删）
- Apple 质量终审：**通过**（retrySave 并发 + SAVING 守卫 2 条终审发现已即修）

### 测试结果
```
Shared:  620 tests, 0 failures
Android:  22 tests, 0 failures
Total:   642 tests, 0 failures
```

---

## 二、✅ 架构模型审核已完成 → ⚠️ 下个 session 主任务：修复 AF-ARCH-01~03

### 本次（2026-08-06）已完成

由 `google_architecture_engineer` + `apple_architect` 双视角并行审查 B1+B2+B3 全部代码（优先 B3），每条结论均由架构模型逐行读代码验证后收敛，写入 `feature/AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md` §11。**未改动任何生产代码，纯审核。**

**结论：BLOCKED。** 3 项 🔴 阻断 + 6 项 🟡 建议（S1~S6，需 B4 蓝图起草时逐条显式处理）+ 一批 ⚪ 死代码建议。详见蓝图 §11.2~§11.4，此处只列须做的事：

| AF | 一句话 | 改动范围 |
|---|---|---|
| **AF-ARCH-01** | `done` 事件协议契约缺口：prompt 要求每段输出 `done`，parser 未处理，落入 else 产出"未知事件类型「done」"警告，**现在每次成功生成都会展示给用户**（当前生产缺陷，非仅 B4 前瞻）。 | 仅 `StreamingMealParser.kt`（B1）+ 单测，加一个 `"done" -> {}` 分支。 |
| **AF-ARCH-02** | `StreamingMealSession` 构造时建**唯一**parser 服务全部 segments，`tryWholeJsonFallback` 却要求 `segments.size==1` 才回退——B3 恒 1 段被掩盖，B4 多段会致 fallback 永久失效、截断标记跨段互相覆盖、fallback 日期错锚。 | 仅 `StreamingMealSession.kt`（B3）+ 单测，parser 改按 segmentId 惰性创建（不引入接口，只改构造时机）。 |
| **AF-ARCH-03** | `AiMealPrompt.buildStreamingRequest` 同时支持"单段"与"多段合一请求"两条矛盾路径，当前仅靠调用方隐式选择维持一致——这是 **B4 蓝图必须先冻结的决策**（推荐"N 请求×1 段"，与 AF-ARCH-02 一致），非独立修复批次。 | 归入 B4 蓝图第 1 步，届时需把 `AiMealPrompt.kt` 纳入 B4 allowlist。 |

**下一步（按顺序）**：
1. 找编码模型按蓝图 §11.2 表格逐项关闭 AF-ARCH-01、AF-ARCH-02（各自独立小批次，allowlist 见表格，附新增单测证据）。
2. 两项关闭并复验（`:shared:testDebugUnitTest` 0 失败）后，起草 B4 实施蓝图时：把 AF-ARCH-03 的决策写入蓝图 §1/§不变量表；把 §11.3 的 S1~S6 逐条显式处理（接受方案或不采纳理由都要写）。
3. 蓝图 §11.6 全部满足后，把 B3 蓝图状态由 `BLOCKED` 改 `ACCEPTED`，才可开始 B4 编码。

**不要**：跳过 AF-ARCH-01/02 直接写 B4 蓝图（AF-ARCH-02 不修，B4 的多段 fallback/截断从设计上就是错的，返工成本远高于现在改）。

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
| `shared/.../ai/meallog/StreamingMealSession.kt` | reducer | 🔧 架构审待修 AF-ARCH-02 |
| `shared/.../ai/meallog/StreamingMealParser.kt` | parser | 🔧 架构审待修 AF-ARCH-01 |
| `shared/.../ai/meallog/NdjsonEvents.kt` | 事件类型 | ⚪ 架构审：`NdjsonEvent` 族死代码待清 |
| `shared/.../ai/meallog/AiMealPrompt.kt` | prompt | 🔧 架构审：AF-ARCH-03 待 B4 蓝图冻结决策 |
| `androidApp/.../ai/CloudAiRuntime.kt` | Runtime | ✅ 架构审通过（⚪ postOnce 重复，不阻塞） |
| `androidApp/.../ai/StreamTransport.kt` | transport | ✅ 架构审通过 |
| `androidApp/.../ai/CloudAiRequestConfig.kt` | config adapter | ✅ 架构审通过 |
| `androidApp/.../ui/ai/AiMealInputViewModel.kt` | ViewModel 会话链 | ✅ 修复 |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | UI | ✅ 修复 |
| `shared/.../test/.../StreamingMealSessionTest.kt` | Session 测试 | ✅ |
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

## 六、B3 复审关键注意事项（B3.4 修复后更新）

1. **save 协程管理**：save 协程赋给 `generationJob`，`invalidateGenerationToInput` 会 cancel 它；`_state.update` 内 `phase==SAVING` 守卫防竞态覆盖
2. **健康摘要容错**：`buildHealthSafetyReport` 用 `runCatching`+`NonCancellable` 独立容错，失败降级默认报告
3. **Sheet 关闭守卫**：`PARTIAL_READY`/`PREVIEW_READY` 弹确认框；`SAVING` 阶段禁止关闭
4. **retrySave 并发防护**：入口 `phase!=ERROR` 门禁 + `generationJob?.cancel()` 防连点双写
5. **.copy() 粘性字段**：`invalidateGenerationToInput` 改用 `prev.copy()`，`inputMode`/`voiceState` 等自动继承
6. **stream 异常防护**：`collect{}` 外层 try-catch，非 Cancel 异常记录为段失败不静默丢失
7. **死代码已清**：旧同步路径（-140 行）、PENDING/cancel（-23 行）、jsonFallbackDays（-40 行）、DoneEvent（-12 行）
