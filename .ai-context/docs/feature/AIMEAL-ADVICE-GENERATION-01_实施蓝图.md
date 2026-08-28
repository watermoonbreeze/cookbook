# AIMEAL-ADVICE-GENERATION-01：健康建议与餐食生成代际一致性

> 状态：**CODE_COMPLETE / TURN=REVIEW**（CODE 已交付，等待旗舰逐项复核）
> 颗粒度：**L7**；理由：异步任务、UI 可见状态、健康建议的匿名数据边界与现有 generation 状态机相交。  
> 基线：`59d73112`（R8–R10 文档治理提交后，CODE 启动时 HEAD）
> 前置：`AIMEAL-RELIABILITY-01_研究与冻结前蓝图.md` §2 R2-A；不得与 R2-B/C/D 合并交付。

## 0. 目标与非目标

目标：修复 I8——用户已请求健康建议后编辑输入、变更日期、重置或关闭会话时，旧请求的成功/失败回包不得继续改变当前界面。每个建议请求必须归属于发起时的同一份 `generationId + autoGenPreview`。

非目标：不改变建议提示词、健康阈值、云端同意流程、存储模型、AI/规则解析选择、UI 文案或语音开关；不新增网络重试、取消按钮、持久化字段、数据库迁移或公开 API。

## 1. 已证实事实与决策

| 事实 | 证据 | 冻结结论 |
|---|---|---|
| 主餐食生成已经有 `generationId`、`generationJob` 与 `isCurrentGeneration()` 守卫。 | `AiMealInputViewModel.kt:439-563, 797-798` | 健康建议必须复用这条代际语义，不能自创第二套会话 ID。 |
| `confirmHealthAdvice()` 当前用裸 `viewModelScope.launch`；回写前未核验 generation/preview，`invalidateGenerationToInput()` 也不会取消它或清状态。 | 同文件 `321-353, 921-937` | I8 确为真实缺陷，不能以“主生成已取消”宣称关闭。 |
| 建议只属于确认页，关闭后应清除；同意弹窗已清楚说明它只在本次确认页展示。 | `AiMealInputSheet.kt:1062-1073, 1133-1140` | 输入失效时必须同时清 `healthAdviceConsentPending/loading/advice/error`。 |
| `AiMealInputViewModelStreamTest` 已提供内存 DB、可控协程主线程及 `CompletableDeferred` 测试模式。 | `AiMealInputViewModelStreamTest.kt:46-199, 496-588` | 测试采用 gate，不得使用 `Thread.sleep`、真实网络或轮询。 |

### 冻结决策

1. 新增私有 `healthAdviceJob: Job?`；它只拥有建议请求，不能占用或取消 `generationJob`。
2. 请求身份由调用时快照的 `generationId` 和 `preview` 对象引用共同构成；回写时二者都必须仍等于快照，且当前 phase 只能是 `PARTIAL_READY` 或 `PREVIEW_READY`。
3. 所有会话失效入口统一调用一个私有清理函数：取消 `healthAdviceJob`、置空引用，并清 `healthAdviceConsentPending/loading/advice/error`。`invalidateGenerationToInput()` 必须调用它；`cancelGeneration()`、保存开始与 `onCleared()` 若存在等价会话结束路径，也必须逐一复用它。
4. 再次点击“查看建议”时，仅在未 loading 时允许显示同意框；确认后仅允许一个 job。拒绝只关闭同意框，不清已显示的本次建议。
5. 取消不应写“生成失败”；仅当前、仍有效的失败才写入既有 `healthAdviceError`，并继续遵守最大 120 字限制。

## 2. 不变量（INV）

| ID | Owner | While | When | Do | Must not | Evidence |
|---|---|---|---|---|---|---|
| INV-ADV-01 | `confirmHealthAdvice` | 当前确认页有 preview | 用户同意生成 | 捕获 `generationId` 与 preview 身份，建立唯一 `healthAdviceJob` | 不以裸协程产生无身份回写 | T-ADV-01 |
| INV-ADV-02 | 会话失效收口 | 建议处于 pending/loading/已完成/失败任一态 | 编辑、改日期、reset、dismiss error、关闭会话或保存开始 | 取消 job 并清五个 healthAdvice 字段 | 不保留旧 advice、loading 或旧 error | T-ADV-02、T-ADV-03 |
| INV-ADV-03 | 建议 job 回写 | `complete()` 恢复后 | 当前 `generationId`、preview identity 或 phase 任一不匹配 | 直接 return，不更新任何 state | 不让迟到成功或失败污染新会话 | T-ADV-02、T-ADV-04 |
| INV-ADV-04 | 有效建议 job | 请求成功/失败 | 身份仍匹配且仍在预览阶段 | 仅更新 `loading=false` 与 advice 或既有截断错误 | 不修改 meal preview、健康档案、输入或持久化数据 | T-ADV-01、T-ADV-05 |
| INV-ADV-05 | 同意与展示 UI | 用户未确认或取消 | 取消同意 / 请求被取消 | 关闭 pending；取消不展示失败 | 不绕过云端同意闸门或改变匿名 payload | T-L1-03b 回归、T-ADV-06 |

## 3. 固定 allowlist

```allowlist
allow:
androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputViewModel.kt | 仅新增私有 healthAdviceJob/清理与身份谓词；将现有健康建议启动、回写和会话失效入口接入本蓝图 INV
androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputViewModelStreamTest.kt | 新增 T-ADV-01~05 的确定性竞态测试及本文件需要的测试 fake
.ai-context/docs/feature/AIMEAL-ADVICE-GENERATION-01_实施蓝图.md | 勾选交付、写入实际命令和审查结论
.ai-context/docs/feature/AIMEAL-RELIABILITY-01_研究与冻结前蓝图.md | 仅将 R2-A 状态和实际 commit/验证证据回写
.ai-context/docs/projectReview/10_后续执行路线图与蓝图库.md | 仅将 R2-A 状态回写为 CODE_COMPLETE/ARCH_ACCEPTED（实际发生后）
.ai-context/docs/context_memory/BLUEPRINT_STATE.md | 按握手协议登记/推进本批状态
.ai-context/docs/context_memory/SESSION_交接.md | 仅更新本批交接摘要
forbidden:
androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputSheet.kt | 不改 UI、文案、同意弹窗或语音开关
shared/ | 不改 prompt、runtime、协议、数据模型、schema 或 seed
androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/ai/AiMealConsentGateIntegrationTest.kt | 既有同意门禁回归基线，不得修改
```

## 4. Luna 实施脚本

### STEP-ADV-1：建立唯一建议任务与失效收口

在 `AiMealInputViewModel` 的 `generationJob` 邻近区域新增 `private var healthAdviceJob: Job? = null`；新增私有 `clearHealthAdviceForInvalidatedSession()`：先 `cancel()`、置空 job，再单次 `_state.update` 清除 `healthAdviceConsentPending`、`healthAdviceLoading`、`healthAdvice`、`healthAdviceError`。

把该函数接入每个会使 `generationId` 或 `autoGenPreview` 失效的既有入口，至少包括 `invalidateGenerationToInput()` 与 save 开始。若 `cancelGeneration()`/Sheet 关闭存在独立路径，先在代码搜索并同样接入；不得复制一段字段清理代码。

完成形态：任何上述入口返回后，`healthAdviceLoading == false`、其余四项为 false/null，且旧 job 已取消。

### STEP-ADV-2：为请求捕获身份并安全回写

在 `confirmHealthAdvice()`：

1. 读取 preview 后同时读取非空 `generationId`；任一为空直接 return。
2. 设置 pending=false、loading=true、error=null 后，将请求赋给 `healthAdviceJob`。
3. 在 job 内完成 `healthSummaryLabels()` 与 `aiRuntime.complete()`；回写前以一个私有谓词核验：当前 generation ID 相同、当前 `autoGenPreview === capturedPreview`、phase 仍为两个预览态之一。
4. 谓词失败或 `CancellationException` 时不写 state；其他异常沿既有 `Result` 路径变为当前会话的人话错误。成功/失败回写后把 `healthAdviceJob` 清为 null，但不得误清后来创建的另一个 job（只能在仍为本 job/身份有效时清理）。

完成形态：有效请求成功只写本次 advice；有效失败只写既有错误；任何失效请求零回写。

### STEP-ADV-3：确定性测试

在既有 `AiMealInputViewModelStreamTest` 使用一个 `AiRuntime.complete()` 被 `CompletableDeferred` 阻塞的 fake，先驱动到 `PREVIEW_READY`，再调用 advice 操作。

| 测试 | 驱动与精确断言 |
|---|---|
| T-ADV-01 | 有效请求释放成功回包：loading 先为 true，后为 false；advice 是新文本；error 为 null；`complete` 仅一次。 |
| T-ADV-02 | A 建议进入 gate → `setInputText()` → 释放 A 成功：保持 INPUT、无 preview、advice/error 为 null、loading=false。 |
| T-ADV-03 | A 建议进入 gate → `setTargetDate()` 与 `reset()` 分别覆盖：均不留下建议状态；至少一个用例验证取消后的 completion 无回写。 |
| T-ADV-04 | A 建议进入 gate → 重新 submit 得到 B preview → 释放 A 失败：B 的 generation/preview 仍在，A error 不出现。 |
| T-ADV-05 | 有效失败回包：loading=false、advice=null、error 为期望短文案；不改 phase/preview。 |
| T-ADV-06 | 既有 `T-L1-03b` 原样运行：未同意云端时没有 complete 调用、无 advice。 |

禁止：测试不得以 `delay` 猜时序；不得调用真实云端；不得把 `AiMealConsentGateIntegrationTest` 改成适配新实现。

### STEP-ADV-4：交付与状态

1. 运行 §5 全部命令，并记录退出码、测试名称与提交 SHA。
2. `BLUEPRINT_STATE` 仅可由 CODE 写 `CODE_COMPLETE / TURN=REVIEW`；不得自填 `ARCH_ACCEPTED`。
3. 由旗舰审查者逐 INV、allowlist、测试矩阵验收；发现反例即登记 AF 并回到 CODE，不得以“测试绿”替代审查。

## 5. 验证合同

```powershell
scripts\build-cli.bat :androidApp:testDebugUnitTest --tests "com.sxdbsm.cookbook.android.ui.ai.AiMealInputViewModelStreamTest"
scripts\build-cli.bat :androidApp:testDebugUnitTest --tests "com.sxdbsm.cookbook.android.ui.ai.AiMealConsentGateIntegrationTest"
scripts\build-cli.bat :androidApp:testDebugUnitTest
scripts\build-cli.bat :androidApp:assembleDebug
python .ai-context/tools/blueprint_check.py --allowlist ".ai-context/docs/feature/AIMEAL-ADVICE-GENERATION-01_实施蓝图.md" --range <BASE>..<HEAD>
git diff --check
```

人工/真机项（统一归 R10，状态必须为 `PENDING_DEVICE_VERIFICATION`）：生成建议后编辑输入；生成建议后切换日期；请求中关闭 sheet；拒绝同意；网络失败后重试。验证重点是无旧建议闪现、无卡住 loading、无内部错误代号，且确认页之外不保留建议。

## 6. 独立挑战记录（冻结前）

| 挑战 | 反例 | 结论 |
|---|---|---|
| 仅 cancel job 是否足够？ | 非协作实现可能在取消后仍返回。 | 否；必须有 generation+preview+phase 三重回写谓词。 |
| 只比 generationId 是否足够？ | 同一 generation 的 preview 可能被后续部分结果替换。 | 否；额外比较 preview 对象身份。 |
| advice job 能否复用 generationJob？ | 编辑取消建议会错误取消主生成；主生成完成又会覆盖 advice slot。 | 否；两类工作使用独立 job，但共用 generation 身份。 |
| 能否在失效时只隐藏 advice？ | loading/error 仍可能污染随后新会话。 | 否；必须原子清五个字段。 |
| 是否应同时启用语音或改变建议文案？ | 会扩大为 R2-C/R2-D，无法证明因果。 | 否；明确禁止跨批。 |

## 7. 完成判定

CODE 完成不等于验收。只有 allowlist、五个新增竞态测试、两组回归、Android 全量单测、Debug 构建和 diff 检查均通过，且旗舰审查确认所有 INV 后，ARCH 才可把本批改为 `ARCH_ACCEPTED`。真机项始终后置到 R10。

最后更新：2026-08-28。

## 8. CODE 交付证据（待旗舰 REVIEW）

- STEP-ADV-1~2：`AiMealInputViewModel` 新增独立 `healthAdviceJob`；输入/日期/reset/保存/关闭均取消并清除建议状态；回写同时校验 generation、preview 对象身份和预览阶段。
- STEP-ADV-3：新增 `T-ADV-01~05`，用 `CompletableDeferred` 与 `NonCancellable` 受控模拟迟到回包；未使用 sleep、真实网络或轮询。
- T-ADV 定向：`scripts\build-cli.bat :androidApp:testDebugUnitTest --tests "com.sxdbsm.cookbook.android.ui.ai.AiMealInputViewModelStreamTest"` 通过。
- 同意门禁：`scripts\build-cli.bat :androidApp:testDebugUnitTest --tests "com.sxdbsm.cookbook.android.ui.ai.AiMealConsentGateIntegrationTest"` 通过。
- 全量与构建：`:androidApp:testDebugUnitTest`、`:androidApp:assembleDebug` 均通过。构建输出含既有第三方 `common-9.9.9-runtime.jar` D8 stack-map 警告，未造成失败。
- 尚待：以实际提交 SHA 运行 allowlist 检查、`feature_sync_check --range` 与旗舰 REVIEW；设备项仍归 R10，未创建或关闭真机结论。
