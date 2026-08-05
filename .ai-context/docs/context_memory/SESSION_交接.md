# 🔖 SESSION 交接入口

> 更新时间：**2026-08-06 03:46**
> 当前状态：AI 记一餐大改 B1/B2 八审通过；B3 三轮复审修复完成（`d4384040`），待架构模型定向复审；B4 未开始。

---

## 一、当前已完成

### B1 协议层（已通过）
- `InputSegment`、`NdjsonEvents`、`StreamingMealParser`（归属校验+fallback+合并）、`AiMealPrompt`（NDJSON prompt）、`LlmStreamEvent`
- Parser 测试 27 条

### B2 Runtime（已通过）
- `CloudAiRequestConfig` + `StreamTransport/StreamCall`（每请求独立 call）
- `CloudAiRuntime.stream()`（callbackFlow + AtomicReference + 四终态）
- `HttpUrlStreamCall`（取消标记+三处检查+安全 IO 包装+HTTP 重试分类）
- `StreamTransportException`（安全异常，不含 body）
- Runtime 测试 13 条

### B3 会话层（三轮修复完成，待定向复审）
提交链：`d45e9aa7` → `ada6748f` → `38cae283` → `d4384040`

核心交付：
- `MealStreamDraftMapper`（纯 mapper，按 ordinal 遍历+同日合并+去重，7 tests）
- `StreamingMealSession`（reducer，ordinal 排序+终态门控+STREAM_ENDED_WITHOUT_TERMINAL，8 tests）
- `AiMealInputViewModel` 会话链：
  - `AiMealPhase` 7 态（INPUT/GENERATING/PARTIAL_READY/PREVIEW_READY/SAVING/DONE/ERROR）
  - `generationJob` + `generationCounter` + `isCurrentGeneration` 谓词
  - `invalidateGenerationToInput`（原子取消+清空）
  - `handleSessionSnapshot`（Delta→snapshot+preview→generation 复查→写入）
  - `AiMealSessionPort` 三方法 seam（preview/commit/parseRule）
  - `buildHealthSafetyReport` 保持基线（直接 healthSummaryLabels，无 try-catch）
  - `useRuleFallback`（纳入 generationJob+generation 复查+CancellationException 重抛）
  - `confirmSave`（PARTIAL/PREVIEW_READY 才可保存+局部 frozenPreview+原子清+port.commit）
- ViewModel 测试 9 条（含 T-B3-06a preview gate 竞态、T-B3-08 两轮 preview、T-B3-08b ERROR+preview fallback 拒绝）
- Sheet 编译适配：GENERATING→ParsingPhase，PARTIAL/PREVIEW_READY→PreviewPhase

### 全部测试：约 500 条，0 失败
- B3 专项：session 8 + viewmodel 9 + mapper 7 + parser 27 + glm 12 + runtime 13 = 76
- 其余：已有 shared 全量回归（约 420+）
- `delay` = 0，`healthSummaryProvider` 残留 = 0

---

## 二、下个 session 的主任务

### 任务：B3 待定向复审 → 按新反馈修复 → 通过后启动 B4

> **架构模型复审未通过**（在当前 session 结束前行审核发现新问题）。下个 session 首先检查是否有新的复审反馈提交（look for `docs: *B3*review*` 类 commit 或 `落地方案.md`/`B3蓝图.md` 的 §11/§7.10 新增章节），然后按修复蓝图执行。
>
> 实施基线：`d4384040`（B3.3 三轮复审）
> B1/B2 代码基线：`b37ace6f`（八审通过）
> **B4 不可开始**，直到 B3 复审通过。

### 先读清单

1. `.ai-context/PROJECT.md`
2. `feature/AI记一餐_周期记_NDJSON流式开发规范.md`（分批状态表）
3. `feature/AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md`（B3 唯一执行文件，含 §8 B3.1、§9 B3.2、§10 B3.3，检查是否有新 §11）
4. `feature/AI记一餐_周期记_NDJSON流式改造落地方案.md`（检查 §7.10/§7.11 新复审反馈）
5. `context_memory/2026-08-05_AI记一餐周期记NDJSON流式改造.md`（全流程台账）
6. `.ai-context/docs/功能路径索引.md`（AI快捷输入记餐行，确认当前项）

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
| **`androidApp/.../ui/ai/AiMealInputViewModel.kt`** | **B3 可改** |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | B3 可改（编译适配） |
| `shared/.../test/.../StreamingMealSessionTest.kt` | B3 可改 |
| `androidApp/.../test/.../AiMealInputViewModelStreamTest.kt` | B3 可改 |
| `shared/.../test/.../MealStreamDraftMapperTest.kt` | B3 可改 |

---

## 三、关键红线（不变）

- `GENERATING`/`PARTIAL_READY` 期间绝不写库（I-01）
- 只有用户确认+二次 MERGE 后才写库（I-02）
- `finish_reason=length` 必须可见，完整已解析内容保留（I-03）
- 归属不完整事件不静默挂靠（I-04）
- 日期遵循 D-15（I-05）
- Release 不含原文/Key/响应（I-06）
- 整体 JSON fallback 仅单 segment（L-04）
- D-15 修正日期贯穿 key/date/meal_id（L-05）
- 取消不是失败终态，不发 Failed/Completed（L-03）
- 错误只含安全码/状态，不含 body（L-06）

---

## 四、B3 复审关键注意事项（三轮经验教训）

1. **generation 复查**：任何挂起点（`port.preview`/`parseRule`/`buildHealthSafetyReport`）恢复后必须 `isCurrentGeneration(id)`，false 直接 return
2. **健康摘要基线**：`buildHealthSafetyReport` 直接调 `healthSummaryLabels()`，无 seam/provider/try-catch；用 `NonCancellable` 包裹避免保存取消 job 时 IO fatal
3. **port 严格三方法**：`preview/commit/parseRule`，不得新增第四方法
4. **测试受控驱动**：Channel/CompletableDeferred/StateFlow.first，无 delay/sleep/轮询/pipe；gate port 单次计数不调 super
5. **GatedPreviewPort 计数**：必须单次 `previewCount++`，不能先增计数再调 super 导致二次递增
6. **信道 close**：gate 测试需要 channel 送事件，gate 前不 close（否则流结束 advance 太早）

---

## 五、提交前状态

当前待复审提交：`d4384040 fix: B3.3 三轮复审 — AF-B3-R3-01~05`

既有无关文件勿混入：
- `temp/claude/chatlog.md`
- `temp/build*.txt`、`temp/err.txt`、`temp/g.txt`、`temp/r3err.txt`
- `temp/test_output.txt`
- `temp/codex/`
