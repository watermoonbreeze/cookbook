# AI记一餐：周期记 + NDJSON 流式 B3 会话实施蓝图

> 状态：`ACCEPTED` —— AF-ARCH-01/02 已关闭（`a7fdf074`，645 tests），ChatGPT 复核确认。AF-ARCH-03 在 B4 蓝图 §1 冻结。B4 蓝图已 `ACCEPTED`，B4 编码已开始。**🔴 架构模型复核检查点**：B4 编码完成后，B3+B4 全量由架构模型审核。
> 必读：`experience/12_多模型协作与实施蓝图规范.md`、`AI记一餐_周期记_NDJSON流式开发规范.md`、`AI记一餐_周期记_NDJSON流式改造落地方案.md` §7.9、本文 §11。
> 基线：`b37ace6f`（B1/B2 代码）与 `36e35689`（B1/B2 八审记录）；`d94e7d8f`（B3.4 代码基线）。

## 1. 目标、非目标与范围冻结

**目标：** 将 `AiMealInputViewModel` 从“一次 `complete()` 后整体解析”的旧编排改为“冻结 session -> 顺序收集 `AiRuntime.stream()` -> 共享 parser -> 只对合法草稿生成局部 preview -> 最终 preview/显式规则降级”的单一会话链。

**非目标：** B4 的快速记/周期记输入 UI、B5 的渐进视觉设计、数据库 schema、DI、HTTP transport、Prompt 协议、健康建议行为、语音行为、历史菜单 MERGE 规则。

| 文件 | 允许操作 |
|---|---|
| `shared/.../ai/meallog/MealStreamDraftMapper.kt`（新） | 唯一增加 `MealStreamDraft -> List<DayMealJson>` 的纯 mapper。 |
| `shared/.../ai/meallog/StreamingMealSession.kt`（新） | 唯一增加纯会话 reducer、跨模块 snapshot 与 segment 状态；不发网络、不写库。 |
| `shared/.../ai/meallog/*Test.kt`（新） | mapper/reducer 的纯单测。 |
| `androidApp/.../ui/ai/AiMealInputViewModel.kt` | 替换提交编排与主阶段；保存/健康建议既有契约只做必要编译适配。 |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | 只做新阶段到既有容器的编译适配；不得新增 B5 视觉和交互。 |
| `androidApp/.../ui/ai/*StreamTest.kt`（新） | ViewModel 的流事件、代际、取消和降级测试。 |
| `androidApp/build.gradle.kts` | **B3.1 唯一例外**：仅保留 `testImplementation(libs.sqldelight.sqlite.driver)` 与 `testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")`，只供 Android ViewModel 单测的内存 DB/受控 dispatcher；不得改动其他依赖、版本、插件或构建配置。 |
| `.ai-context/docs/...` | 更新 B3 台账、状态与真机项。 |

**禁止修改：** `AiRuntime`、`CloudAiRuntime`、`StreamTransport`、Koin/DI、`AiMealPrompt`、`StreamingMealParser`、B1/B2 已有测试、Repository、SQLDelight、Gradle、依赖版本。任何确有必要的例外先提交 `Q-B3`，不得先改代码。

## 2. 不变量

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-B3-01 | 用户点击发送 | 读取一次 UI 输入并冻结为 `StreamingMealRequest`；后续编辑创建下一 generation。 | 修改进行中 request/segments。 | T-B3-01、T-B3-06 |
| INV-B3-02 | session 有多个非空 segment | 按 `ordinal` 串行请求；前一 segment 的 Completed/Failed 后才开始下一段。 | 并行请求、跨段混写。 | T-B3-02 |
| INV-B3-03 | 收到 Delta | 仅由当前 generation 的 `StreamingMealSession` 喂给 `StreamingMealParser`；仅新增合法餐次/菜品时触发局部 preview。 | 原始文本进 UI state/数据库；对每个字符强制 preview。 | T-B3-03 |
| INV-B3-04 | Delta 已形成至少一份合法 `DayMealJson` | 阶段进入 `PARTIAL_READY`，保留生成中标记，允许得到只读局部 preview。 | 写库、提前 Completed、把 warning 当失败。 | T-B3-03 |
| INV-B3-05 | 全部 segment 已终态 | 有合法餐食则 `PREVIEW_READY`；无合法餐食则 `ERROR`。 | 自动规则降级、丢弃已合法前缀。 | T-B3-04、T-B3-05 |
| INV-B3-06 | 用户取消或 generation A 被 B 替代 | A 的 Job 被取消；A 后到 Delta/Failed/Completed 不更新 state；不产生终态。 | A 污染 B、对取消发 `Failed/Completed`。 | T-B3-06 |
| INV-B3-07 | 流失败且当前没有任何合法餐食 | 进入可见 `ERROR`，保留短诊断和原输入；规则解析只由用户显式触发。 | 静默调用 `RuleMealParser`。 | T-B3-05、T-B3-07 |
| INV-B3-08 | 用户确认保存 | 只可消费当前 `AutoGenPreview`，保留既有两次 MERGE 确认。 | 直接消费草稿、重复保存。 | T-B3-08 |

## 3. 固定类型、数据流和真相源

### 3.1 新类型

| 路径 | 类型与可见性 | 固定字段/职责 |
|---|---|---|
| `shared/.../MealStreamDraftMapper.kt` | `internal object MealStreamDraftMapper` | `toDayMealJson(draft, segments): List<DayMealJson>`；纯函数。按 `InputSegment.targetDate` 和 `ordinal` 排序，不生成 fallback ID。 |
| `shared/.../StreamingMealSession.kt` | `class StreamingMealSession` | 对 Android 模块公开的 B3 会话 API；构造参数为不可变 `StreamingMealRequest`；私有持有唯一 `StreamingMealParser`；接收当前 segment 的 `Delta/Completed/Failed/Cancelled`，输出不可变 snapshot。 |
| 同文件 | `enum class StreamSegmentState` | 固定为 `PENDING/STREAMING/COMPLETED/FAILED/CANCELLED`；仅 session 写入。 |
| 同文件 | `data class StreamingSessionSnapshot` | 对 Android 模块公开：`generationId`、`segmentStates: Map<String, StreamSegmentState>`、`draft`、`days`、`diagnostics`、`hasValidMeals`、`isTerminal`。不含原始响应。 |
| `AiMealInputViewModel.kt` | `AiMealPhase` 替换为 `INPUT/GENERATING/PARTIAL_READY/PREVIEW_READY/SAVING/DONE/ERROR` | 删除 `PARSING/PREVIEW`，不得保留兼容分支。 |
| 同文件 | `AiMealInputUiState` 新增会话只读字段 | `generationId: String?`、`segmentStates: Map<String, StreamSegmentState>`、`isGenerating: Boolean`；`autoGenPreview` 仍是唯一可保存对象。不能用 `Map<String, Any>` 或跨模块 internal 类型。 |
| 同文件 | `fun useRuleFallback()` | 唯一允许调用 `RuleMealParser` 的公开动作；仅当 phase=ERROR 且当前 generation 无合法餐食时执行，产物必须写 `parseSourceMessage="规则解析"`。B5 再接可见按钮，B3 不新增 UI。 |

`StreamingMealSession` 是 B3 的唯一 reducer；ViewModel 不得自己拼 NDJSON、维护第二份 parser 草稿或判断日期归属。它不认识 `ViewModel`、Compose、Repository 或 `AutoGenPreview`。

### 3.2 mapper 的唯一映射规则

1. 只遍历 `MealStreamDraft.segments` 中存在、且在 `StreamingMealRequest.segments` 精确命中的 `segmentId`；其他项只留诊断，绝不生成 `DayMealJson`。
2. `MealDraftNode` 仅在至少包含一条非空 `DishDraftNode.name` 时转换。`date` 使用 parser 已校验/锚定后的 `MealDraftNode.date`，不得再用设备当前日期或最近 segment 覆盖。
3. 一个 `MealDraftNode` 转为一个 `MealJson`；菜品转为 `MealDishRefJson(dish = DishJson(...), ref = null)`；食材转为 `DishIngredientJson(food = FoodJson(name = ...), ref = null)`。缺失用量使用 Unified schema 已有默认值，不新增猜测规则。
4. `cookingSteps` 按 `order` 升序、未给 order 的项保持输入顺序；`seasonings` 作为 `is_main=false` 的 `DishIngredientJson` 追加，不能丢失或作为主料。
5. `DayMealJson.raw_input` 只使用匹配 `InputSegment.inputText`；`parse_method="ai"`；不得写 raw response、健康建议或诊断原文。
6. 同日期的餐次在 mapper 内按 `meal_id` 合并；同一 `meal_id` 不重复生成。排序为日期升序、餐次按 `breakfast/lunch/dinner/snack`、菜品按 `dish_id`。

### 3.3 完整数据流

`UiState.inputText + targetDate` 在 `submit()` 时冻结 -> B3 仅构造一个 quick segment（`segmentId="quick-{targetDate}"`、`ordinal=0`、`weekAnchor=targetDate 所在周一`、`generationId="meal-{递增序号}"`） -> `StreamingMealSession` -> 对每个 nonBlank segment 调 `AiMealPrompt.buildStreamingRequest(listOf(segment))` -> `AiRuntime.stream` -> session/parser -> mapper 的 `days` -> `MultiDayRecorder.previewAll(days, targetDate)` -> `AutoGenPreview` -> 用户确认 -> `commitPreview`。

B4 将替换“仅构造 quick segment”的输入构造步骤为周期 segments；不得修改 session、mapper 或 Runtime 合同。

## 4. 状态机与时序

| 来源 | 事件 | 目标 | 原子结果 |
|---|---|---|---|
| INPUT/ERROR/DONE | `submit` 且输入非空 | GENERATING | 创建递增 generation、冻结 request、清空旧 preview/result/诊断。 |
| GENERATING | 当前段 Delta 后 `hasValidMeals=false` | GENERATING | 只更新进度/诊断，不调用 preview。 |
| GENERATING | 当前段 Delta 后 `hasValidMeals=true` | PARTIAL_READY | 调一次 `previewAll`；成功后写当前 preview，`isGenerating=true`。 |
| PARTIAL_READY | 后续合法 Delta | PARTIAL_READY | 可重算局部 preview；相同 `days` 不重复调用。 |
| GENERATING/PARTIAL_READY | 当前段 Completed | 同状态或下一个段 GENERATING | session finish 当前段；如有下一段才开始下一请求。 |
| GENERATING/PARTIAL_READY | 所有段完成且有合法餐食 | PREVIEW_READY | 最终重算一次 preview，`isGenerating=false`，汇总诊断。 |
| GENERATING/PARTIAL_READY | 所有段完成且无合法餐食 | ERROR | 短诊断；不调用规则 parser。 |
| GENERATING/PARTIAL_READY | 当前段 Failed 且无合法餐食、无下一段 | ERROR | 短错误码/原因；不降级。 |
| GENERATING/PARTIAL_READY | 当前段 Failed 且有合法餐食 | PARTIAL_READY 或 PREVIEW_READY | 保留已合法 preview 与失败诊断；不丢前缀。 |
| GENERATING/PARTIAL_READY | cancel/reset/new submit/date change | INPUT 或新的 GENERATING | 取消 Job；旧 generation 后续事件全部丢弃且无终态。 |
| PREVIEW_READY/PARTIAL_READY | `confirmSave` | SAVING -> DONE/ERROR | 沿用现有 commit/MERGE 合同。 |
| ERROR | `useRuleFallback` | PREVIEW_READY 或 ERROR | 仅此事件可调 `RuleMealParser`；成功后明确标记规则来源。 |

实现时使用一个 `generationJob: Job?` 私有字段；每次开始新 generation 前先 `cancel()` 旧 Job，再替换。每次 collector 回写 state 前先精确比较 `state.generationId == session.generationId`。不使用全局 Job、`GlobalScope`、延迟 sleep 或跨 ViewModel 的取消器。

## 5. 逐文件机械实施脚本

1. 新建 `MealStreamDraftMapper` 与纯测试，按 3.2 映射；先通过 mapper 的日期、餐次、dish、seasoning、未知 segment 五类测试。
2. 新建 `StreamingMealSession` 与纯测试。它只编排 parser/mapper 与 segment 状态，不持有 `AiRuntime`，不启动协程。
3. 在 ViewModel 中替换 `AiMealPhase`、增加 generation 私有 Job/序号和 3.3 的 quick request 构造；删除 `submit()` 对 `parseToDayMealJsonList()` 的调用，不删除旧非流式辅助函数，直到 B6 清理批次再处理。
4. ViewModel 每段只收集一次 `aiRuntime.stream(AiMealPrompt.buildStreamingRequest(listOf(segment)))`；按第 4 节逐事件调用 session，且仅消费当前 generation 回调。
5. 将 session snapshot 中的 `days` 交给 `previewAll`；preview 成功后只写 `autoGenPreview`、warnings、会话状态。不得写库。
6. 在 `AiMealInputSheet` 将旧 `PARSING` 映射到 `GENERATING`，旧 `PREVIEW` 映射到 `PARTIAL_READY/PREVIEW_READY` 的现有容器；不得加入新按钮、文案、布局或 B4/B5 状态展示。
7. 增加测试后才更新 B3 台账和真机清单。任何 `Q-B3` 先停在本步骤，不能跳到下一步。

## 6. 测试矩阵

| ID | 前置与刺激 | 精确断言 | 夹具约束 |
|---|---|---|---|
| T-B3-01 | quick 输入后再编辑文本。 | request 的 `inputText/targetDate/generationId` 保持首次快照；编辑不改进行中 session。 | fake runtime 只延迟事件，不检查 ViewModel 私有实现。 |
| T-B3-02 | 两个 segment；第一段 Completed 后第二段才允许发事件。 | `new stream` 调用顺序为 s1,s2；第二 call 在 s1 完成前为 0。 | 用 channel/latch 控制完成，禁止 sleep。 |
| T-B3-03 | meal/dish Delta 分两次到达。 | 无合法 dish 前无 `previewAll`；合法 dish 后 phase=PARTIAL_READY、preview 一次、无写库。 | parser 使用真实 B1 实现；fake 只提供 Delta。 |
| T-B3-04 | 第一段合法，第二段失败或 `length`。 | 合法前缀保留；最终为 PREVIEW_READY 或有局部 preview 的可见状态；诊断包含失败/截断。 | fake Failed 只模拟 Runtime 事件。 |
| T-B3-05 | 全流 Failed 或全为非法事件。 | phase=ERROR；`RuleMealParser` 调用数为 0；无 preview/commit。 | 明确 spy rule parser seam；不得以返回空列表伪造。 |
| T-B3-06 | generation A 阻塞，开始 B 或取消 A，随后释放 A 的 Delta/Completed/Failed。 | A 后到事件不改变 B；A 无 Failed/Completed；A Job 已取消。 | 用受控 flow + latch；不得直接调用 reducer 的“取消结果”。 |
| T-B3-07 | ERROR 后直接调用 `useRuleFallback()`。 | 仅此显式动作调用一次 RuleMealParser，结果标记规则来源；未调用时为 0。 | B3 不创建 UI；B5 只绑定该既有动作。 |
| T-B3-08 | PARTIAL_READY/PREVIEW_READY 有 AutoGenPreview 且 existing meal。 | 第一次确认仅置 MERGE confirmed；第二次才 commit 一次；草稿不可直接 commit。 | recorder fake 记录输入对象和调用次数。 |
| T-B3-09 | mapper 包含第二日期、seasoning、乱序 steps、未知 segment。 | 精确断言 DayMealJson 日期、meal slot、dish name、is_main=false、step 顺序、未知项不生成 day。 | 纯 shared 单测，无 Runtime/UI。 |

## 7. 交付与放行

编码模型交付时必须给出：allowlist diff、T-B3-01~09 对照表、当前 commit 的 shared test/Android ViewModel 定向 test/Android Debug build 输出、未完成真机项和全部 Q-B3。任一 T 未通过、无当前证据、出现未授权文件或自行补的 UI 行为，B3 一律不放行 B4。

## 8. B3.1 首轮复审修复蓝图（唯一执行依据）

> 复审基线：`d45e9aa7`。本节覆盖前文与下列事项冲突的旧描述；其余范围、类型和禁止项不变。编码模型只做本节列出的机械修改，不得更改 B1/B2、DI、Repository、数据库或 B4/B5 UI。

### 8.1 固定测试 seam

| ID | Owner | When | Input | Do | Must not | Evidence |
|---|---|---|---|---|---|---|
| B3.1-PORT-01 | 编码模型 | 修改 ViewModel 前 | 现有 `MultiDayRecorder`、`RuleMealParser`、`IngredientRepository` | 只在 `AiMealInputViewModel.kt` 新增 `internal data class RuleFallbackResult(val days: List<DayMealJson>, val warning: String?)`、`internal interface AiMealSessionPort { suspend fun preview(days: List<DayMealJson>, targetDate: LocalDate): AutoGenPreview; suspend fun commit(preview: AutoGenPreview): AutoGenResult; suspend fun parseRule(input: String, targetDate: LocalDate): RuleFallbackResult }` 与 `internal fun replaceSessionPortForTest(port: AiMealSessionPort)`；默认 port 必须逐字复用现有 `previewAll`、`commitPreview`、`allActiveNames -> RuleMealParser.parse -> MealDateAnchorPolicy.apply` 链路。 | 不改 `MultiDayRecorder`/`RuleMealParser`，不进 Koin，不改公开 ViewModel 构造器，不新增生产 fallback。 | T-B3-03/05/07/08 通过 port 精确记录 preview、commit、规则解析调用。 |
| B3.1-PORT-02 | 编码模型 | 修改 Gradle | 现有 `d45e9aa7` 依赖 diff | 保留 §1 写明的两个 testImplementation；测试统一用 `runTest`、受控 dispatcher、`Channel`/`CompletableDeferred` 或 `StateFlow.first`。 | 不再添加任何依赖；不得使用 `delay`、轮询、真实网络、随机端口。 | `rg -n "delay\\(|Thread.sleep" androidApp/src/test/java/.../AiMealInputViewModelStreamTest.kt` 无匹配。 |

### 8.2 AF 闭环：唯一最小修复

| AF | 违反项与定位 | 固定修复 | 必须新增/替换的证据 | 禁止扩大范围 |
|---|---|---|---|---|
| AF-B3-01 | INV-B3-03/04；`AiMealInputViewModel.kt` 的 `Delta` 分支只 `onDelta`，未调用 snapshot/preview。 | 每个 `Delta` 先 `session.onDelta(segmentId, text)`，再立即 `handleSessionSnapshot(session, generationId, isFinal=false)`；沿用 `lastPreviewDays` 去重。 | T-B3-03：受控 flow 先送 meal，断言 `preview=0`；再送 dish，断言 `PARTIAL_READY`、`preview=1`、`commit=0`，随后不得发送 Completed 前进入 `PREVIEW_READY`。 | 不新增 phase、buffer 或 parser。 |
| AF-B3-02 | INV-B3-02/05；`StreamingMealSession.nextSegment()` 未排序、未要求前段终态；ViewModel 在无 terminal 的 flow 返回后仍推进。 | `orderedSegments = request.nonBlankSegments.sortedBy(InputSegment::ordinal)`；`nextSegment()` 仅在尚未开始，或当前状态为 `COMPLETED/FAILED` 时返回下一段，`CANCELLED` 永远返回 null。每段 `collect` 返回后，若当前状态仍为 `STREAMING`，调用 `session.onFailed(segmentId, "STREAM_ENDED_WITHOUT_TERMINAL")` 后才取下一段。 | T-B3-02：输入声明顺序为 ordinal=1/0；第一次必须取 ordinal=0；其 `Completed/Failed` 前再次 `nextSegment()` 为 null；终态后才为下一段。另测无 terminal 的 flow：记录 Failed 诊断，才允许下一段，且不提前 `PREVIEW_READY`。 | 不让 session 持有 Runtime/Job，不改 B1 parser。 |
| AF-B3-03 | INV-B3-01/06；`setInputText()` 留旧 generation，`setTargetDate()` 不取消，且 preview 使用可变 UI 日期。 | 新增唯一私有 `invalidateGenerationToInput(nextInput, nextDate)`：先 `generationJob?.cancel()`，再原子清空 `generationId/segmentStates/autoGenPreview/lastPreviewDays` 并令 `isGenerating=false, phase=INPUT`；`setInputText`、`appendText`、`onVoiceResult`、reset、日期变更均调用它。`handleSessionSnapshot` 的 preview date 取 session/request 冻结日期，不得读可变 UI date。 | T-B3-01/T-B3-06：spy 捕获首次 request 的 input/date/generation；编辑或改日期后释放 A 的 Delta/Completed/Failed，断言状态仍是 INPUT/new generation、A Job cancelled、无 preview/commit。 | 不为取消写 ERROR/Completed，不新增全局 Job。 |
| AF-B3-04 | §5 第 6 步；`GENERATING` 被交给 `PreviewPhase`，其 preview=null 时直接 return。 | `AiMealInputSheet` 固定为 `GENERATING -> ParsingPhase()`；仅 `PARTIAL_READY/PREVIEW_READY -> PreviewPhase()`。 | Android Debug build；新增最小 Compose/状态分派断言，或在验收台账记录人工检查：生成初期显示既有解析容器，合法 dish 后显示既有预览。 | 不新增按钮、文案、布局、B5 交互。 |
| AF-B3-05 | INV-B3-07/08；`confirmSave()` 未限制 phase，`useRuleFallback()` 未排除已有 preview。 | `confirmSave()` 只接受 `PARTIAL_READY/PREVIEW_READY`，第一次 MERGE 只置确认；第二次先把当前 `AutoGenPreview` 存入局部不可变值，再 `generationJob.cancel()` 并原子设 `generationId=null,isGenerating=false,phase=SAVING`，最后只对该局部值调用 `port.commit`；SAVING/DONE/ERROR 的重复调用直接 return。`useRuleFallback()` 同时要求 `phase=ERROR && autoGenPreview==null`。 | T-B3-08：existing preview 第一次只置 `mergeConfirmed`，第二次 `commit(currentAutoGenPreview)` 恰一次，第三次 0 次；保存开始后释放旧 Delta/Completed 仍保持 SAVING/DONE；另测带 preview 的 ERROR 调用 fallback 为 0。 | 不改既有 MERGE 文案或写库语义。 |
| AF-B3-06 | §3.2 第 6 条；`MealStreamDraftMapper.kt` 对同 `meal_id` 直接赋值覆盖，且未按 segment ordinal 驱动。 | 先按 request segments 的 ordinal 遍历已知 `segmentId`；同 date+mealId 合并 dish map（key=`dishId`），再统一转换为一个 `MealJson`，dish 排序仍按 `dishId`。`raw_input` 取该 day 第一个有合法餐次的 ordinal 最小 segment。 | T-B3-09 增加两个已知 segment 同 date+mealId、不同 dish：只产一个 meal 且两 dish 都保留；另断言乱序 map 不改变结果和 raw_input。 | 不生成 fallback ID，不改日期锚定。 |
| AF-B3-07 | §1 allowlist、§6/7；依赖在未等待 Q-B3 时已加入，且测试用 `delay` 与弱断言。 | 依赖例外已由本文 §1/§8.1 追认，编码模型不得再变更 Gradle；删除全部 `delay` 测试路径，按 T-B3-01~09 重建因果证据。 | 交付时逐项列 T-B3-01~09 的测试名、精确断言、当前命令与结果；不能再用“70 tests 0 failures”代替。 | 不新增第三方 mock 框架、接口或测试端口。 |

### 8.3 B3.1 放行判定

1. 上表 AF-B3-01~07 全部关闭，且所有 T-B3-01~09 各有一个可直接定位的自动化用例。
2. 运行 `scripts\\build-cli.bat :shared:testDebugUnitTest`、`scripts\\build-cli.bat :androidApp:testDebugUnitTest --tests com.sxdbsm.cookbook.android.ui.ai.AiMealInputViewModelStreamTest`、`scripts\\build-cli.bat :androidApp:assembleDebug`；提交本次输出，不使用 UP-TO-DATE 或历史 XML 作为唯一证据。
3. 更新 B3 台账为“复审中”，列出 AF 关闭映射、allowlist diff 和唯一真机清单的 B3 状态；本轮不新增 B4/B5 真机项。
4. 任一 AF、T、命令或 allowlist 不满足，结论只能是 `BLOCKED`，不得开始 B4。

## 9. B3.2 二轮复审修复蓝图（唯一执行依据）

> 复审基线：`ada6748f`。本节覆盖 §8 的冲突内容；仅允许修改 `AiMealInputViewModel.kt`、`StreamingMealSessionTest.kt`、`AiMealInputViewModelStreamTest.kt`、`MealStreamDraftMapperTest.kt` 与 B3 台账/开发规范。**不得改 Gradle、Mapper、Session 生产代码、UI、B1/B2、DI、Repository 或数据库。**

| AF | 违反项与定位 | 固定修复 | 必须新增/替换的证据 | 禁止扩大范围 |
|---|---|---|---|---|
| AF-B3-R2-01 | INV-B3-06；`handleSessionSnapshot()` 的 `port.preview()` 与健康摘要是挂起点，恢复后和 `catch(Exception)` 回写前没有再次比对 generation；取消会被当成 ERROR。 | 定义唯一私有谓词 `isCurrentGeneration(generationId) = _state.value.generationId == generationId`。`preview()` 返回后、恢复原 ViewModel 私有健康摘要后、以及普通异常写 ERROR 前都先调用该谓词；false 立即 return。单独 `catch (CancellationException) { throw e }`，排在 `catch (Exception)` 前。 | T-B3-06a：spy port 的 `preview` 通过 `CompletableDeferred` 进入、`withContext(NonCancellable)` 等待释放；A 已进入 preview 时执行编辑/提交 B/确认保存之一，再释放 A。断言 INPUT/B/SAVING-DONE 保持不变，旧 A 不得写 phase、preview、segmentStates 或 ERROR。 | 不新增 Job、Mutex、Flow 或状态；不把取消转换为诊断/失败。 |
| AF-B3-R2-02 | §8.1 B3.1-PORT-01；`AiMealSessionPort` 多出 `healthReport`，并把既有健康摘要移入 port 且吞异常，超出冻结三方法和 B3 非目标。 | `AiMealSessionPort` 严格只保留 `preview/commit/parseRule`；删除 `healthReport` 方法、DefaultSessionPort 中的健康仓储字段/实现与测试 fake 实现。将健康摘要代码恢复为 ViewModel 原有私有 `buildHealthSafetyReport(preview)`，其既有行为不改。 | `rg -n "healthReport" AiMealInputViewModel.kt AiMealInputViewModelStreamTest.kt` 无匹配；既有 T-B3-03/05/07/08 仅通过三方法 port 继续通过。 | 不新增健康建议测试、接口或异常降级。 |
| AF-B3-R2-03 | INV-B3-02/05、T-B3-02、T-B3-04；现有 reducer 测试没有乱序 ordinal、终态前二次 `nextSegment()==null`，且无“合法前缀+后段失败”用例。 | **只扩展 `StreamingMealSessionTest`。** T-B3-02 使用声明列表 `[ordinal=1, ordinal=0]`：首取必须 ordinal=0；在其 `Completed` 和 `Failed` 前分别断言再次取段为 null；标 Failed 后才取 ordinal=1。另以 first 合法 meal/dish→Completed、second Failed 构造 T-B3-04，断言 final snapshot `hasValidMeals=true`、`isTerminal=true`、days 保留 first dish、diagnostics 含 second failure。 | 上述两个测试直接标注 `T-B3-02` / `T-B3-04`；不以 quick ViewModel 单段用例替代。 | 不修改 Session/Parser/Mapper 生产代码，不新增多段 ViewModel 输入。 |
| AF-B3-R2-04 | T-B3-01/05/06/08；当前 VM 用例未记录 request 冻结字段、未覆盖日期失效/preview 挂起竞态，未证明保存后旧工作不能改写；T-B3-05 未断言 commit=0。 | **只扩展 `AiMealInputViewModelStreamTest`。** Runtime spy 记录每个 `LlmRequest`；T-B3-01 断言第一请求的 user 含原始 input 与冻结 targetDate，且提交后 UI generation 为 `meal-1`；随后 `setTargetDate` 与编辑均使旧 generation 无效。T-B3-05 增加 `commitCount=0`。T-B3-06 用 R2-01 的 preview gate 覆盖 A→B；T-B3-08 用同一 gate 覆盖保存开始后释放旧 A，断言不离开 SAVING/DONE，且 `commit` 收到的对象与确认时 `autoGenPreview` 是同一实例。ERROR 但人为保留 preview 时 `useRuleFallback()` 调用数必须为 0。 | 所有异步边界只用 `runTest`、`StateFlow.first`、`Channel`、`CompletableDeferred`、`NonCancellable` gate；无 `delay`/轮询。 | 不修改生产 API 或加入 mock 框架。 |
| AF-B3-R2-05 | T-B3-09 与 §8.3 当前证据；raw_input 测试两段输入相同，无法证明最小 ordinal 归属；二轮本地非缓存 Android 定向命令未取得可采信输出。 | `MealStreamDraftMapperTest` 的同日同 meal 测试改为 ordinal=1 的输入文本 `late` 与 ordinal=0 的 `early`，声明顺序仍逆序，精确断言 `raw_input="early"`。在无并行 Gradle 任务时重新运行 §8.3 三条命令，保留本次控制台结果。 | T-B3-09 的 raw_input 直接断言；`shared:testDebugUnitTest`、Android ViewModel 指定测试、`androidApp:assembleDebug` 全部本次成功，且 test task 不是唯一 `UP-TO-DATE` 证据。 | 不清理/修改用户 `temp/`，不把 XML/commit message 当证据。 |

### 9.1 放行

仅当 AF-B3-R2-01~05 全部关闭、T-B3-01~09 均有直接测试、§8.3 三命令均有本次成功输出时，B3 才可标为 `ACCEPTED` 并开始 B4。否则保持 `BLOCKED`。

## 10. B3.3 三轮复审修复蓝图（唯一执行依据）

> 复审基线：`38cae283`。本节覆盖此前文字中与下列事项冲突的内容。
> 唯一允许改动：`androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputViewModel.kt`、`androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputViewModelStreamTest.kt`、`shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/ai/meallog/StreamingMealSessionTest.kt`、B3 台账/开发规范。不得改 shared 生产代码、UI、port 接口、DI、Repository、数据库、Gradle、B1/B2 或其他生产文件。

| ID | Owner | When | Input | Do | Must not | Evidence |
|---|---|---|---|---|---|---|
| AF-B3-R3-01 | 编码模型 | `useRuleFallback()` 从 ERROR 启动至 `parseRule/preview` 任一挂起点恢复后，或用户调用 `dismissError()`、编辑、改日期、reset、submit 时 | 调用开始时的 `generationId`、`inputText`、`targetDate` 快照；既有 `generationJob` | 仅当 `state.phase==ERROR && state.autoGenPreview==null && state.generationId!=null` 时启动。把该 coroutine 赋给既有 `generationJob`；捕获 `fallbackGenerationId`。`parseRule()` 返回后、`preview()` 返回后、规则空结果写 ERROR 前、普通异常写 ERROR 前均调用 `isCurrentGeneration(fallbackGenerationId)`；false 立即静默 return。单独 `catch (CancellationException) { throw e }` 位于普通 `catch` 前。`dismissError()` 必须改为调用既有 `invalidateGenerationToInput(current.inputText, current.targetDate)`，不保留仅改 phase 的分支。 | 不新增 Job 字段、Mutex、Flow、port 方法、fallback 状态或 UI；不把取消转为 ERROR/诊断；不改规则解析、日期锚定或提交语义。 | 新增 `T-B3-07a`：受控 port 的 `parseRule` 进入 `CompletableDeferred` gate 后，调用 `setInputText`；释放 gate 后断言 `phase=INPUT`、`generationId=null`、`autoGenPreview=null`、`previewCount=0`，且没有 ERROR。再以同一 gate 覆盖 `dismissError()`；两例均无 `delay`/轮询。 |
| AF-B3-R3-02 | 编码模型 | `handleSessionSnapshot()` 的 preview 成功路径计算健康摘要时 | 已返回的 `preview` 与当前 `generationId` | 删除 `healthSummaryProvider` 字段和测试中的赋值。将 `buildHealthSafetyReport(preview)` 恢复为基线 `d45e9aa7` 的私有函数体：直接调用 `healthSummaryLabels()`，无 provider、无 try/catch 降级。仍在当前 `try` 内先取得 `safetyReport`，其恢复后立刻调用 `isCurrentGeneration(generationId)`；仅 current 时一次性写入既有 preview 状态与 `healthSafetyReport=safetyReport`。 | 不新增健康建议/健康摘要接口、测试 seam、后台 launch、异常吞没或新的 UI 行为；不修改 health repository。 | `rg -n "healthSummaryProvider" AiMealInputViewModel.kt AiMealInputViewModelStreamTest.kt` 无匹配；`AiMealSessionPort` 仍严格只含 `preview/commit/parseRule`。既有 T-B3-03/05/07/08 继续使用三方法 port；Android 定向测试通过。 |
| AF-B3-R3-03 | 编码模型 | 替换 T-B3-06/T-B3-08，并补 ERROR+已有 preview 的 fallback 拒绝证据时 | 既有 `RecordingRuntime`、`SpySessionPort`、`Channel`、`CompletableDeferred`、`NonCancellable` | 仅修改 `AiMealInputViewModelStreamTest`。替换 T-B3-06：A 先送合法 meal/dish 并进入 `preview` gate；A 挂起期间调用 `submit()` 启动 B，B 完成到 `PREVIEW_READY`；释放 A 后断言 B 的 `generationId`、phase、preview、segmentStates 不变，且无 ERROR。替换 T-B3-08：port 第一轮 preview 立即返回 `hasExisting=true` 的实例 P；第一次 `confirmSave()` 仅确认 merge；随后发送 A 的 `Completed`，使 final preview 进入第二轮 gate；第二次 `confirmSave()` 在该 gate 挂起时提交 P 并进入 SAVING/DONE；释放旧 final preview 后断言仍为 SAVING/DONE、`commitCount==1` 且 `lastCommittedPreview === P`。新增“首个 PARTIAL preview 成功、final preview 抛异常”的 fake，使状态为 `ERROR` 但仍保留 preview；调用 `useRuleFallback()` 后精确断言 `parseRuleCount==0`。gate port 每次 preview 只能计数一次。 | 不新增生产 seam/mock 框架；不保留旧的“流已完全结束后才保存”用例作为 T-B3-08；不得用编辑替代 A→B；不得用 sleep、delay、轮询或私有 reducer 直调。 | `T-B3-06`、`T-B3-08` 测试名保留且各自包含上述 gate 断言；ERROR+preview 拒绝 fallback 有独立断言；`rg -n "delay\\(|Thread.sleep" androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputViewModelStreamTest.kt` 无匹配。 |
| AF-B3-R3-04 | 编码模型 | 验证 `nextSegment()` 的 FAILED 门控时 | 逆序声明的两个 InputSegment `[ordinal=1, ordinal=0]` 与新的 `StreamingMealSession` | 仅扩展 `StreamingMealSessionTest` 的 T-B3-02，新增独立 session：首取 ordinal=0；其状态仍为 STREAMING 时 `nextSegment()==null`；对首段调用 `onFailed` 后，下一次才返回 ordinal=1。保留已有 Completed 门控断言和乱序声明。 | 不修改 `StreamingMealSession`、mapper、parser 或 ViewModel；不以第二段失败替代“首段失败才推进”的断言。 | T-B3-02 同时精确覆盖 Completed 与 Failed 两条推进前置条件；shared 定向测试通过。 |
| AF-B3-R3-05 | 编码模型 | AF-B3-R3-01~04 全部完成后申请验收时 | 无并行 Gradle 任务的终端；当前 commit | 按 §10.2 顺序串行执行三条命令，并以实际结果更新台账。命令超时、无输出、失败、仅历史 XML、仅 `UP-TO-DATE` 或旧 temp 日志均记为无证据。 | 不并行 Gradle；不编辑/清理用户 `temp/`；不预填“0 failures”或“BUILD SUCCESSFUL”。 | 三条当次命令均成功，且当前输出可定位到本 commit；否则 B3 保持 BLOCKED。 |

### 10.1 固定测试夹具

1. `GatedPreviewPort` 必须把创建 preview 对象与计数放在一个 override 中；禁止先递增再调用会再次递增的 `super.preview()`。
2. `T-B3-07a` 的 fake 只能阻塞 `parseRule`，并用既有三方法 port 记录 `previewCount`；测试不得直接构造或回写 ViewModel state。
3. T-B3-08 的第二轮 preview 由 A 的 `Completed` 触发 `isFinal=true`；即使 `lastPreviewDays` 相同，现有 `!isFinal` 去重条件也必须让 final preview 真实进入 gate。测试中不得用额外 dish 或已经完成的流替代此时序。

### 10.2 放行与证据

1. AF-B3-R3-01~05 全部关闭，且 T-B3-01~09 仍各有直接测试，才可提交验收。
2. 在**没有其他 Gradle 任务**时按此顺序运行并保留本次输出：
   `scripts\\build-cli.bat :shared:testDebugUnitTest --rerun-tasks`；
   `scripts\\build-cli.bat :androidApp:testDebugUnitTest --tests com.sxdbsm.cookbook.android.ui.ai.AiMealInputViewModelStreamTest --rerun-tasks`；
   `scripts\\build-cli.bat :androidApp:assembleDebug --rerun-tasks`。
3. 本轮 `:androidApp:testDebugUnitTest --tests ... --rerun-tasks` 在 124.1 秒无输出后超时，且无 test-results，因此不是通过证据。三条命令均当次成功后才可把 B3 标为 `ACCEPTED`；否则一律 `BLOCKED`，不得开始 B4。

> B3.3 与随后的 B3.4（四视角联合复审，5 阻断 + 9 建议全部修复，两轮终审通过，提交 `d94e7d8f`，642 测试 0 失败）已完成，正确性/并发/边界维度关闭。但 B4（多段周期记）开工前，架构维度尚未审查——见 §11。

## 11. 架构模型终审（B1+B2+B3 全量 · B4 前置门禁）

> 复审基线：`d94e7d8f`。审查人：`google_architecture_engineer` + `apple_architect` 双视角独立审查，逐条结论由架构模型（我）代码验证后收敛；覆盖范围 = B1 协议层 + B2 Runtime + B3 会话层全部生产代码，优先 B3。目的：B3.4 的四轮 AF 修复与两轮质量终审均聚焦"正确性/并发/边界"，**从未有人从"能否撑住 B4 多段周期记"的架构角度审查**，此为 SESSION_交接.md §2 要求的审核，也是 `12_多模型协作与实施蓝图规范.md` §6"架构冻结门禁"在起草 B4 蓝图前的强制自检。

### 11.1 结论

**AF-ARCH-01/02 已关闭（`a7fdf074`），AF-ARCH-03 待 B4 蓝图冻结。ChatGPT 架构复核（2026-08-06）：有条件通过，允许进入 B4 蓝图阶段。**

原终审发现 3 项 🔴 阻断（AF-ARCH-01~03，均已逐行代码验证，非推测），其中 1 项是**当前已在生产发生的用户可见缺陷**（不是"B4 才会暴露"），另 2 项是"B3 因恒定单段而被掩盖、B4 一旦多段立即踩空"的架构缺口。三项改动集中、定位精确，不需要推翻 B1/B2/B3 已有设计。

**修复状态**：AF-ARCH-01（`done` 静默消费）和 AF-ARCH-02（按 segmentId 惰性 parser）已在 `a7fdf074` 修复，Shared 623 + Android 22 = 645 tests 0 failures，ChatGPT 独立复核确认关闭。AF-ARCH-03（请求段数策略）必须在 B4 蓝图第一步冻结。另有一批 🟡 建议须在 B4 蓝图起草时逐条显式处理（接受或拒绝都要写明理由，不能沉默跳过），以及一批 ⚪ 死代码/技术债建议顺手清理。

### 11.2 🔴 阻断（AF-ARCH，须先关闭再起草 B4 蓝图）

| AF | 层/文件 | 问题与验证证据 | 影响 | 修复方向 | Allowlist | 验收证据 |
|---|---|---|---|---|---|---|
| **AF-ARCH-01** | B1 · `AiMealPrompt.kt:39,52,126` × `StreamingMealParser.kt:122-134` × `AiMealInputViewModel.kt:409` × `AiMealInputSheet.kt:755-769` | Prompt 明确要求模型每段结束输出 `{"type":"done",...}`；`StreamingMealParser` 的 `when(parsed.type)` 无 `"done"` 分支，落入 `else` 追加 `WARNING 未知事件类型「done」，已忽略`；该诊断经 `snap.diagnostics.map{it.message}` 直接写入 `parseWarnings`，`PreviewPhase` 原样渲染"请确认以下解析提示"卡片给用户。三处代码逐行读取确认，链路成立。 | **当前生产缺陷，非仅 B4 前瞻**：现在每一次成功的 AI 记一餐都会在预览页多出一条对用户无意义的技术性噪音。四轮 AF 修复 + 两轮质量终审均未捕获，因为审查聚焦并发/状态机正确性，没有人逐字追踪"prompt 承诺的事件 → parser 是否消费 → 诊断是否 user-facing"这条链路。B4 段数 ×7，噪音同比放大。 | `StreamingMealParser` 增加 `"done" -> {}`（可选：标记 per-segment `receivedDone` 供 B4 判断段落收尾，但不得产出诊断）。 | 仅 `StreamingMealParser.kt`（B1）+ 对应单测；不改协议/Prompt/其他文件。 | ✅ **已关闭**（`a7fdf074`）。新增单测验证 `done` 不产生 warning；ChatGPT 复核确认关闭。 |
| **AF-ARCH-02** | B3 · `StreamingMealSession.kt:39-43,87` × `StreamingMealParser.kt:474-478` | `StreamingMealSession` 构造时创建**唯一** parser、`segments=request.segments` 全量；每段 `onCompleted` 都对同一 parser 调 `finish()`；而 `tryWholeJsonFallback` 要求 `segments.size==1` 才回退。B3 恒为 1 段，缺陷被掩盖；测试固定单段，无法暴露。逐行读取确认。 | B4 一旦 >1 段：①整体 JSON fallback 对所有段永久失效（parser 看到的 `segments.size` 恒等于段总数而非 1）；② `hasAnyNdjsonEvent`/`finishReason`/`isLengthTruncated` 是 parser 全局字段，被每段 `finish()` 反复覆盖 —— 截断标记被后段抹掉、一旦某段出现过 NDJSON 就永久跳过其余段的 fallback；③ `fallbackDate` 恒为第一段日期，多天 fallback 场景日期错锚。 | `StreamingMealSession` 把 parser 从"构造时单例"改为"按 segmentId 惰性创建"（工厂/Map，每段一个独立实例），`onCompleted` 只 finish 该段自己的实例；`snapshot()` 合并各段 draft/diagnostics。**不引入 parser 接口/依赖注入**——只有一个实现，注入是过度设计；问题是构造的时机/作用域，不是耦合方式。 | 仅 `StreamingMealSession.kt`（B3 已授权文件）+ `StreamingMealSessionTest.kt`；不改 `StreamingMealParser.kt`、不改 B1 协议、不改 VM/UI。 | ✅ **已关闭**（`a7fdf074`）。`segmentParsers: LinkedHashMap<String, StreamingMealParser>` + `getOrPut` 惰性创建；ChatGPT 复核确认关闭。**下列边界检查须携带到 B4 蓝图：** |

**AF-ARCH-02 关闭确认 · B4 携带边界检查表（ChatGPT 复核）：**

| # | 检查项 | 判断 | B4 蓝图落点 |
|---| ------ | ---- | ----------- |
| 1 | 每个 parser 构造时只传入一个 `InputSegment` | 必须 | B4 蓝图 INV 表 |
| 2 | delta 只进入当前 segment 的 parser | 必须 | B4 蓝图 INV 表 |
| 3 | `finish_reason=length` 只标记当前 parser | 必须 | B4 蓝图 INV 表 |
| 4 | parser 内的残余 buffer 不跨 segment | 必须 | B4 蓝图 INV 表 |
| 5 | snapshot 合并顺序与原 segments 顺序一致 | 必须 | B4 蓝图 §数据流 |
| 6 | 多次调用 snapshot 不会重复累计结果 | 必须 | B4 蓝图 INV 表 |
| 7 | `segmentId` 重复时有明确行为（同一 session 内 segmentId 必须唯一，建议 fail-fast） | B4 前冻结 | B4 蓝图 §不变量 |
| **AF-ARCH-03** | B1 API 面 × B4 决策缺口 · `AiMealPrompt.kt:93-127` × `AiMealInputViewModel.kt:306` | `buildStreamingRequest(segments: List<InputSegment>)` 同时实现"单段"与"多段合一请求"两条 prompt 拼装分支（多段分支要求模型"逐段输出 NDJSON"、maxTokens 按段数缩放到 8192）；当前唯一调用点固定传 `listOf(segment)`，多段分支是不可达代码，仅靠调用方隐式选择维持一致性。逐行读取确认两分支并存。 | 不是代码缺陷，是**决策缺口**：B4 编码者面对两条现成路径，选错一条会让 AF-ARCH-02 的按段 parser 修复失效（多段合一请求下一次响应混着多天 NDJSON，无法简单按段惰性建 parser），也会让周期记的核心诉求（按段进度/失败隔离/单段重试）落空。 | **B4 蓝图开工前必须显式冻结为"N 次独立请求 × 每次 1 段"**（与 AF-ARCH-02 一致，支持按段重试/进度/失败隔离），并将 `buildStreamingRequest` 签名收窄为单段，删除多段合一分支与 maxTokens 缩放逻辑，使错误调用方式编译期不可用。 | 归为 **B4 蓝图第 1 步的强制不变量**，非独立修复批次；会触及 `AiMealPrompt.kt`，需 B4 蓝图显式将该文件纳入允许修改范围。 | B4 蓝图的 INV 表与 allowlist 需显式记录本决策及理由。 |

### 11.3 🟡 建议（B4 蓝图起草时须逐条显式处理，接受/拒绝均要写明理由）

| # | 定位 | 问题 | 建议 |
|---|---|---|---|
| S1 | `AiMealInputViewModel.kt:546-566`（`confirmSave`）× B4 多段场景 | `PARTIAL_READY` 允许保存；保存时 `generationJob?.cancel()` 会连带取消仍在 STREAMING 的段。B3 单段无感知；B4 若用户在第 3/7 天预览时点保存，后续天的输入会被**静默丢弃**，违反项目"透明准则"（改数据/放弃用户输入至少 T1 事后留痕）。 | B4 蓝图需显式定策略：`isTerminal` 前禁止保存，或保存前明确告知"仅保存已完成 N 天，其余 M 天将放弃"，结果需回报"已存 X 天/放弃 Y 天"。 |
| S2 | `AiMealInputViewModel.kt:309-325`（Delta → `handleSessionSnapshot` → `sessionPort.preview`） | 每个 Delta 都可能触发一次 `previewAll`（内部 `AutoGenContext.load` 全字典加载），流式期间"相同 days 不重复"的去重条件很难命中；B4 段数 × 已完成天数会放大该开销，且在 `callbackFlow` 的 `collect` 内同步执行会拖慢 SSE 读取（用户感知"AI 越写越慢"）。 | B4 蓝图需定 preview 触发时机：改"段终态 + final + 防抖/节流"，Delta 路径只更新 `segmentStates` 等纯 UI 进度，不触发 DB 相关的 preview。 |
| S3 | `StreamingMealSession.kt:20-28`（`StreamingSessionSnapshot`） | `segmentStates: Map<String, StreamSegmentState>` + 拍平的 `diagnostics` 无法表达"第几天/哪个日期失败、原因是什么"，B4 UI（渲染"第 3/7 天失败"）需要更结构化的表达；`draft` 字段把 parser 内部草稿整体暴露过模块边界，仅测试引用。 | B4 蓝图设计 snapshot 时补 segment→date/ordinal/error 的结构化字段与 `pendingCount`（呼应 S1）；`draft` 可收窄为仅测试可见或删除。 |
| S4 | `AiMealInputViewModel.kt:79-83`（`AiMealSessionPort`） | `preview(days, targetDate)` 的 `targetDate` 在多天场景语义模糊（下游 `MultiDayRecorder` 参数名是 `today`，三处命名指同一概念）；`commit(preview)` 无法按天局部提交（B4 若要局部重试/局部保存，只能在 VM 里对 `AutoGenPreview` 做领域手术）；`parseRule(input, targetDate)` 只接单串，表达不了按天降级。 | B4 蓝图冻结新签名（如 `commit(preview, dayFilter: Set<String>? = null)`），过滤逻辑下沉 `MultiDayRecorder`，不在 VM 里拆解领域模型。 |
| S5 | `AiMealInputViewModel.kt:150-155`（`sessionPort` 为 `var` + `internal fun replaceSessionPortForTest`） | 生产类为测试暴露可变注入点，构造完成后仍可被改写，是不必要的并发面。 | 改为构造参数默认值注入（`private val sessionPort: AiMealSessionPort = DefaultSessionPort(...)`），测试直接构造传参，删 `replaceSessionPortForTest`。 |
| S6 | `AiMealInputViewModel.kt`（644 行，9 项职责并列：输入/语音状态机/generation 编排/snapshot 映射/保存/规则降级/健康摘要/健康建议云端调用/周起始日算法） | 两位架构师独立一致结论：**现在不建议整体拆分 VM**（`submit()` 本就要为 B4 重写，先拆是白工；本项目有表单 VM 拆分引入 hydration/数据丢失的踩坑史，收益不抵风险）。但两处应"下沉"而非"拆分"：①周起始日算法 + segment 构造 → shared `InputSegmentFactory`（B4 本就要写"按周生成 7 个 segmentId"的工厂，现在就该定归属，且可纯单测覆盖周一边界/跨年周）；②健康摘要脱敏逻辑（"绝不发送姓名/ID"红线）目前只在 androidApp、无 shared 单测覆盖，应下沉 `HealthContextSummarizer` 并配单测。 | B4 蓝图把这两处下沉列为 §5 实施脚本的前置步骤，不做其余拆分。 |

### 11.4 ⚪ 可选 / 死代码技术债（不阻塞 B4，建议下一次维护批次顺手清）

- `NdjsonEvents.kt` 的 `NdjsonEvent` sealed 族（7 个子类，约 75 行）：已用 grep 确认全仓零引用（`hasAnyNdjsonEvent` 是同名近似的独立布尔字段，非同一符号）；`parser` 实际直接用 `NdjsonLine` + 字符串路由。文件头注释仍称"内部用 NdjsonEvent sealed 族做强类型处理"，与实现相反，会误导 B4 阅读者，建议删除或据此真正落地。
- `StreamingMealParser.kt:24` 构造参数 `generationId` 类体内零使用，建议删除。
- `AiMealPrompt.kt:58-85,139-172` 的 `FLAT_SYSTEM_PROMPT`/`buildRequest()`/`@Deprecated SYSTEM_PROMPT` 生产零调用；注释"保留供整体 JSON 回退使用"与事实不符（fallback 只重新解析已收到的原文，不会重新发送该 prompt），建议删除或改正注释。
- `AiMealInputViewModel.kt` 的 `confirmSave()`/`retrySave()` 保存协程体重复约 18 行，可提取共享私有函数。
- `AiRuntime.kt` 的 `LlmStreamEvent.Failed.httpStatus`/`Completed.totalChars` 从未被赋值或消费。
- `StreamingMealSession.kt:60 nextSegment()` 有副作用（推进游标 + 置 STREAMING）但命名像纯 getter，建议改名（如 `beginNextSegment()`）或补文档。
- B2 `CloudAiRuntime.kt:75-187`：`stream()` 走 `StreamTransport` seam（可测/可取消/错误分类），`postOnce`（供 `complete()`/健康建议调用）是类内私有裸 `HttpURLConnection`，无取消语义、与 `stream()` 的重试循环逐字重复。`confirmHealthAdvice` 走的正是这条路径，用户关闭 Sheet 时该请求不会被取消。建议顺手把 `postOnce` 收进 transport、重试循环抽公共函数，但不阻塞 B4（B4 只用 `stream`）。

### 11.5 审查通过项（供 B4 蓝图沿用，无需重新论证）

- 依赖方向单向：`shared/ai/meallog` 零 android 导入；UI 层未见反向导入 shared 内部可变类型（除 §11.4 提到的 `draft` 字段外）；`AiMealInputSheet.kt` 未注入任何 Repository，符合项目红线。
- `StreamingMealSession` 不认识 ViewModel/Compose/Repository/AiRuntime，B3 分层本身干净，不需要推倒重来。
- `StreamingMealSession` 直接 `new StreamingMealParser()` 的耦合方式本身**不是问题**（YAGNI：只有一个实现，不存在替换需求）——真正要改的是 AF-ARCH-02 的构造时机/作用域，而非引入接口。
- `AiMealSessionPort` 三方法的抽象定位（隔离 `MultiDayRecorder` 便于测试）合理，非过度设计；命名达意、无 mode 布尔硬编码。
- B2（`CloudAiRuntime`/`StreamTransport`/`CloudAiRequestConfig`）整体无过度设计残留，`connectionFactory` 等测试缝有明确消费者，属合理克制。
- `AiMealInputSheet.kt` 对 ViewModel 的消费符合 UDF 准则（`collectAsStateWithLifecycle` + `vm.xxx()` 上抛事件，无本地业务态）。

### 11.6 放行判定

**原判定**：仅当 AF-ARCH-01~03 全部关闭（含新增测试证据、`:shared:testDebugUnitTest` 与既有 Android 定向测试保持 0 失败）、且 B4 蓝图起草时已把 §11.3 的 S1~S6 逐条显式处理（写明接受方案或不采纳理由），才可将本文档状态由 `BLOCKED` 改为 `ACCEPTED` 并开始起草 B4 实施蓝图。

**当前状态**：
- AF-ARCH-01 ✅ 已关闭（`a7fdf074`，ChatGPT 复核确认）
- AF-ARCH-02 ✅ 已关闭（`a7fdf074`，ChatGPT 复核确认，7 项边界检查携带到 B4 蓝图）
- AF-ARCH-03 🔧 待 B4 蓝图第一步冻结
- S1~S6 🔧 待 B4 蓝图逐条显式处理
- 测试：Shared 623 + Android 22 = 645 tests 0 failures ✅

**ChatGPT 复核结论（2026-08-06）**：有条件通过，允许进入 B4 蓝图阶段。B4 编码前须完成 4 项门禁（见 §11.7）。

### 11.7 ChatGPT 架构复核（2026-08-06 · 独立第三方审核）

> 复核范围：AF-ARCH-01/02 修复提交 `a7fdf074` + 本文 §11 架构模型终审
> 总体结论：**有条件通过，允许进入 B4 蓝图阶段**

**逐项判断**：

| AF | 结论 | 依据 |
|----| :--: | ---- |
| AF-ARCH-01 | ✅ 关闭 | `done` 静默消费是最小且正确的处理。附加验证全部通过：不产生 warning、未知事件仍报警、不破坏已解析数据、不额外生成内容。 |
| AF-ARCH-02 | ✅ 关闭 | `LinkedHashMap<String, StreamingMealParser>` + `getOrPut` 惰性创建解决了原三个架构风险。7 项边界检查携带到 B4 蓝图（见 AF-ARCH-02 条目下表）。 |
| AF-ARCH-03 | 🔴 未关闭 | 必须在 B4 蓝图第一步冻结为"N 请求 × 1 段"，收窄 `buildStreamingRequest` 签名。 |

**B4 编码前必须完成的 4 项门禁**：

| # | 门禁 | 状态 |
|---| ---- | :--: |
| 1 | B4 蓝图第一步冻结 AF-ARCH-03：N 次独立请求 × 每次 1 段 | 🔧 待 B4 蓝图 |
| 2 | 原审核 S1~S6 逐条写入蓝图，注明采纳方案或不采纳原因 | 🔧 待 B4 蓝图 |
| 3 | 确认 645 项测试结果运行在完整 SHA `a7fdf074ca9c437cc1c4acfe3ffb622bfd325331` 上 | ✅ 已确认 |
| 4 | AF-ARCH-02 边界检查表（7 项）写入 B4 蓝图不变量 | 🔧 待 B4 蓝图 |

**B4 蓝图起草时的强制清单**（除上述 4 项外）：
- 把 `segmentId` 唯一性写为 B4 不变量（建议 fail-fast，不依赖隐式约定）
- 把 snapshot 合并顺序写入 B4 数据流
- SESSION_交接.md 的版本元数据已修正（`b8a90121` → `a7fdf074`）
