# AI记一餐：周期记 + NDJSON 流式 B3 会话实施蓝图

> 状态：`BLUEPRINT_READY`；B1/B2 已通过，B3 只可按本蓝图实施。
> 必读：`experience/12_多模型协作与实施蓝图规范.md`、`AI记一餐_周期记_NDJSON流式开发规范.md`、`AI记一餐_周期记_NDJSON流式改造落地方案.md` §7.9。
> 基线：`b37ace6f`（B1/B2 代码）与 `36e35689`（B1/B2 八审记录）。

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
