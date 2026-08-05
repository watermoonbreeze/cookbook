# AI记一餐：周期记 + NDJSON 流式开发规范

> 状态：架构与验收基线，供 DeepSeek 实施
> 日期：2026-08-05
> 关联：`AI记一餐_周期记_NDJSON流式改造落地方案.md`、`projectReview/21_AI与网络请求策略（专属）.md`、ADR D-15/D-16

## 1. 目标、边界与唯一事实源

### 1.1 交付目标

1. **快速记**：一段自然语言，最多 200 字，解析为可预览的餐食。
2. **周期记**：选择连续 1 至 7 天；每一天独立输入、最多 200 字；仅非空日期段发请求并在同一确认页合并展示。
3. **渐进体验**：发送后立即进入生成确认页。任一完整合法事件到达后立刻展示；网络结束后按日期、餐次、菜品稳定重排。
4. **输出兼容**：优先 NDJSON；同一新协议内兼容整体对象/数组 JSON。可提取至少一个合法菜名即形成待确认内容。

### 1.2 明确不做

- **没有旧版本兼容**：不保留旧 Prompt、旧解析器入口、旧状态机、旧数据迁移或双轨 UI。删除/替换仅限实施批次已覆盖并验证的旧实现。
- 不自动持久化 AI 结果；不持久化健康建议、原始输入、Prompt、完整响应或 Key。
- 不以菜名猜测覆盖模型已经明确给出的食材、调料和做法；本地只能规范化、校验、查重和补全缺失字段。
- 不在本任务做菜品/食材“自动添加成熟算法”的联网调研或重做推荐算法。

### 1.3 不变量

| ID | 不变量 | 可验证证据 |
|---|---|---|
| I-01 | `GENERATING` 与 `PARTIAL_READY` 期间绝不调用 `commitPreview` 或任何写库接口。 | ViewModel 单测/调用审查 |
| I-02 | 只有用户点击“确认记下”，并完成已有餐食的二次 MERGE 确认后，才允许写库。 | ViewModel/真机用例 |
| I-03 | `finish_reason=length` 必须展示“模型输出被截断”，完整已解析事件保留。 | Runtime + Parser 单测 |
| I-04 | 归属不完整的 NDJSON 事件不得静默挂到最近餐次或菜品。 | Parser 单测 |
| I-05 | 日期遵循：绝对日期 > 所选日期；星期按所选日期所在周；无日期用所选日期。 | `MealDateAnchorPolicyTest` |
| I-06 | Release 日志不得含原始饮食文本、完整 Prompt、完整模型响应或 Key。 | 日志审查/Release 真机 |
| I-07 | 同日期未保存会话误关重开保留；切换日期或保存成功后创建新会话。 | `AddDayFoodScreen` UI 回归 |

## 2. 输入、请求与会话契约

### 2.1 输入模型

| 项 | 快速记 | 周期记 |
|---|---|---|
| 日期范围 | 当前添加页所选日期 | 连续 1 至 7 天，默认所选日期所在周 |
| 输入单元 | 单一 `InputSegment` | 每个日期一个 `InputSegment` |
| 文本上限 | 200 Unicode 字符 | 每段 200 Unicode 字符 |
| 空白处理 | trim 后为空不可发送 | 空白段保留在编辑器但不请求、不生成 segment |
| `segment_id` | `quick-{targetDate}` | `week-{weekAnchor}-day{1..7}`，按星期一到星期日编号 |
| Token 上限 | 2048 | 每个非空日期段 4096；运行时安全上限 8192 |

`InputSegment` 是 UI、Prompt、运行时和解析器共享的不可变值：`segmentId`、`targetDate`、`inputText`、`ordinal`。发送后不得修改；编辑、切日期、重试都创建新的 generation。

### 2.2 generation 与取消

- 每次发送创建单调递增的 `generationId`；所有流事件、错误和完成信号都携带它。
- 新发送、切换日期、点击修改或 Sheet 永久销毁时取消旧 generation。晚到事件的 `generationId` 不匹配则直接丢弃，不能污染新会话。
- 同一日期误关重开不取消未保存 session；UI 仅重新订阅其状态。保存成功后清除 session，使同日下一次打开为空白 `INPUT`。
- 周期记按日期块单并发顺序请求。当前 segment 完成、失败或取消后才请求下一段；所有 segment 终态后才结束 generation，以减少重复、跨段污染和云端限流风险。

### 2.3 Prompt 要求

`AiMealPrompt` 只负责纯函数式构建，不能访问 UI、Repository、系统时间或日志。

- 系统提示要求：只输出 NDJSON，禁止 Markdown、解释文本和空行；每行仅一个 JSON 对象。
- 日期提示同时给出该 `InputSegment.targetDate`、该周范围和日期锚点规则；模型不得把设备日期当锚点。
- Prompt 必须要求输出 `done`，并要求每个 `meal`/`dish`/`ingredient` 具备下节定义的归属键。
- `response_format=json_object` 不适用于 NDJSON 请求，流式餐食请求不得开启它；整体 JSON fallback 请求可按模型能力开启。
- 禁止将完整 Prompt 或用户原文写入 Release 日志。

## 3. Runtime 与网络协议

### 3.1 分层边界

| 层 | 责任 | 禁止事项 |
|---|---|---|
| `shared: ai` | 流式抽象、请求/响应值对象、协议 JSON 编解码 | Android HTTP、UI 状态、数据库 |
| `androidApp: ai` | DeepSeek/OpenAI 兼容 HTTP/SSE、超时、重试、脱敏运行日志 | 解析餐食语义、写库 |
| `shared: ai/meallog` | Prompt、行缓冲、NDJSON/整体 JSON 规范化、归属校验、日期锚定 | 网络、Compose、Repository |
| `androidApp: ui/ai` | generation 会话、预览构建、用户确认、渲染状态 | 协议字符串拼接、直接 SQL |

### 3.2 新 API 表面

在 `AiRuntime` 增加流式能力；保留 `complete` 仅给非流式功能或 fallback 使用。

| 类型/方法 | 合同 |
|---|---|
| `stream(request)` | 返回冷 `Flow<LlmStreamEvent>`；收集才发网络；取消收集必须取消 HTTP。 |
| `LlmStreamEvent.Delta` | 仅包含模型正文增量文本；空增量不发给上层。 |
| `LlmStreamEvent.Completed` | 包含 `finishReason` 与累计正文长度；无 finish reason 记为 `unknown`。 |
| `LlmStreamEvent.Failed` | 仅携带可展示的简短原因、HTTP 状态/错误码和是否可重试；不带完整响应。 |
| `complete` fallback | 不支持 SSE 的运行时将完整正文按一个 `Delta` 再发 `Completed`，因此业务层只有一条处理链。 |

`CloudAiRuntime` 负责把 DeepSeek SSE 的 `data:` 帧解析为 `Delta`；`[DONE]` 只转 `Completed`。网络层不得假设一帧等于一行 NDJSON，也不得把 SSE metadata 当模型正文。

### 3.3 网络规则

- 连接超时 15 秒，单 segment 读取超时 60 秒；仅对未收到任一正文事件的可重试网络失败重试 1 次。
- 已收到任何正文后失败，不自动重试，保留已解析事件和失败尾部，避免重复菜品。
- Runtime 传递服务端 `finish_reason`；`stop` 为正常结束，`length` 为截断，其他值显示为异常结束。
- 运行日志只可记录：generation/segment 哈希、模型名、HTTP 状态、耗时、输入与输出长度、finish reason、错误码。Debug 原始片段只能驻留当前会话并二次点击查看。

## 4. NDJSON 事件与解析规范

### 4.1 传输与行缓冲

1. `StreamingMealParser` 接收 `Delta` 字符串并追加缓冲。
2. 仅以 `\n` 分出完整行；移除行尾 `\r` 和空白，空行忽略。
3. 每一完整行用 `kotlinx.serialization` 解析为 JSON 对象；禁止正则拼接或猜测半行。
4. `Completed` 时，缓冲中仍未闭合的内容记录为“尾部不完整”，不解析、不写入预览。
5. 若全程没有任何合法 NDJSON 事件，才以累计正文尝试整体对象 JSON、整体数组 JSON；规范化后仍须经过同一归属校验。

### 4.2 事件定义

所有事件都有 `type`、`segment_id`。`segment_id` 必须与本次已发送 `InputSegment` 精确匹配。

| `type` | 必需字段 | 可选字段 | 行为 |
|---|---|---|---|
| `meal` | `segment_id`,`meal_id`,`date`,`slot` | `time`,`note` | 创建/更新餐次父节点；`meal_id={date}|{slot}`。 |
| `dish` | `segment_id`,`meal_id`,`dish_id`,`name` | `cooking_method`,`quantity`,`unit`,`eaten_ratio`,`note` | 创建菜品；`dish_id={meal_id}|d{正整数}`。 |
| `ingredient` | `segment_id`,`meal_id`,`dish_id`,`name` | `role`,`food_group`,`quantity`,`unit`,`nutrients`,`is_main` | 加入菜品的食材集合。 |
| `seasoning` | `segment_id`,`meal_id`,`dish_id`,`name` | `quantity`,`unit`,`nutrients` | 加入非主料调料集合。 |
| `cooking_step` | `segment_id`,`meal_id`,`dish_id`,`text` | `order` | 仅展示，按 `order` 稳定排序。 |
| `warning` | `segment_id`,`message` | `meal_id`,`dish_id` | 加入对应节点或 segment 诊断。 |
| `advice` | `segment_id`,`message` | `meal_id` | 仅会话展示，不进入 recorder。 |
| `done` | `segment_id` | `summary` | 标记该 segment 模型业务完成；网络仍以 `Completed` 为最终态。 |

未知 `type`、未知字段或缺 `segment_id` 都进诊断且不中断其他行。事件正文绝不能直接进入 UI；必须转换为内部 `MealStreamDraft` 后再渲染。

### 4.3 归属校验与容错

- `meal`：`date` 必须是有效 ISO 日期，`slot` 仅能为 `breakfast/lunch/dinner/snack`。`meal_id` 与 `date|slot` 不符即拒绝该事件。
- `dish`：父 `meal_id` 不存在时，只有同事件可验证的 `date + slot` 能精确生成该父餐次才可补建；否则标记“未归属菜品”。
- `ingredient/seasoning/cooking_step`：父 `dish_id` 不存在时，仅在 `dish_name + meal_id` 精确且唯一命中时可补挂并增加 warning；其余为“未归属明细”。
- 同一 `dish_id` 归到不同 `meal_id` 时，先到的合法归属保留，后到的冲突事件拒绝并诊断。
- 同键重复事件按字段合并：菜品/餐次以最新非空字段覆盖；食材和调料按规范名去重并合并信息，不能重复展示。
- 任何孤儿、冲突、无效日期或半行都不能成为 `DayMealJson`，更不能写库。

### 4.4 日期处理

事件转 `DayMealJson` 后统一调用 `MealDateAnchorPolicy`；不允许流式路径绕过它。周期记中某 segment 的输出日期必须匹配该段目标日期，除非用户该段原文包含绝对日期；星期描述按所选日期所在周映射。偏离时保留诊断并采用策略修正后的日期，不得按“最近一条事件”猜测。

## 5. ViewModel、预览与 UI 状态机

### 5.1 唯一状态机

`INPUT → GENERATING → PARTIAL_READY → PREVIEW_READY → SAVING → DONE`

- `INPUT`：可编辑模式、日期段和文本；快速记/周期记切换保留各自草稿。
- `GENERATING`：已跳转确认页骨架；没有合法菜品时确认按钮禁用；可查看进度、取消或返回修改。
- `PARTIAL_READY`：已有至少一个通过归属校验的菜品；保留生成中标识，允许查看和“确认记下当前内容”。
- `PREVIEW_READY`：所有 segment 已终态；按日期/餐次/菜品排序，集中显示 warning、截断或失败尾部。
- `SAVING`：冻结 generation 和预览；仅允许等待结果，不可重复确认。
- `DONE`：通知添加餐食页刷新并清空本次 session。
- `ERROR`：仅在无任何合法菜品且无法/未选择规则解析时进入；保留原输入和诊断，返回 `INPUT` 可修改重发。

禁止 `PARSING` 与 `PREVIEW` 两套并存；现有状态需直接迁移到上述唯一新状态，无旧状态兼容分支。

### 5.2 ViewModel 规则

- 维护 `generationId`、不可变 `segments`、`segmentStatus`、`eventBuffer`、`draftPreview`、`diagnostics`、`finishReasons`；不要把原始响应放入可持久化 state 或 `SavedStateHandle`。
- 每收到一条完整合法事件：更新 `eventBuffer` → 归组 `draftPreview` → 调用只读 `recorder.previewAll` 生成当前合法预览 → 更新 UI。不得为每个字符或无效事件触发预览。
- `previewAll` 的输入仅为当前合法完整餐食；出现失败/截断时仍保留已有结果。
- “确认记下当前内容”只传当前 `AutoGenPreview`；已有餐食遵循既有两次 MERGE 确认，不因流式而降低门槛。
- AI 无有效结果时，显示失败原因和“改用规则解析”明确操作；只有用户点击该操作才运行 `RuleMealParser`，结果必须显示“规则解析”。

### 5.3 UI 规则

- 输入页用现有 `SegmentedControl` 提供“快速记 / 周期记”；周期记采用连续日期列表，每项显示日期、星期、字符计数和多行输入框。
- 每个输入框即时限制 200 字，并显示 `当前/200`；粘贴内容超过限制时截取并给出一次可见提示。
- 发送后直接显示确认页骨架：日期段进度、增量餐次/菜品、warning、建议、失败尾部；生成中页面必须可滚动。
- 生成中关闭 Sheet 不弹出“已保存”提示；同日期重开恢复生成或预览状态。切换添加页日期时，新 key 必须使旧 session 失效。
- 诊断只显示阶段、短原因、响应长度、finish reason；Debug 才可经二次点击临时查看原始片段。健康建议始终标注“仅供参考，非医嘱”。

## 6. 数据、隐私与日志

- `MultiDayRecorder.previewAll` 是唯一预览构建入口，`commitPreview` 是唯一持久化入口；流事件不允许直接操作 Repository。
- 写入范围仅限用户确认后的餐食、菜品、食材及必要关联；AI 建立的实体继续遵循 `source=ai` 与既有查重规则。
- `advice`、原始输入、Prompt、完整流正文、未归属事件、诊断原文只存在内存会话；重输、取消、保存成功、日期切换或进程死亡后不可恢复。
- Debug 原始日志必须受现有 debug gate 保护；Release 仅记录脱敏指标，CI/审查必须检查相关日志调用。

## 7. DeepSeek 编码规范

1. 先完成 shared 的值对象和纯解析测试，再接 Android HTTP，再接 ViewModel/UI；不得从 UI 直接拼 JSON 或解析 SSE。
2. 新增业务类型放在所属层：协议值对象在 `shared/ai`，餐食事件/校验器在 `shared/ai/meallog`，Android HTTP 在 `androidApp/ai`，状态和 Compose 在 `androidApp/ui/ai`。
3. 每个公共类型/状态转移必须有中文 KDoc，解释业务约束；新增或实质修改代码按项目规则标注 `[AI生成]` / `[AI修改]`，不写重复代码含义的注释。
4. 不使用 `Any`、Map 或 JSON 字符串在层间传递；边界全部使用明确 data class/sealed class。解析必须用 `kotlinx.serialization`。
5. 不吞异常：网络、协议、归属校验和预览失败都转换为带阶段的诊断；UI 不显示堆栈、Key、完整原文或服务端 body。
6. 任何并发修改 `StateFlow` 必须带 generation 校验；所有可取消网络任务遵循协程取消，不启动脱离 `viewModelScope` 的后台线程。
7. 单个批次必须同时提交实现、相关单测、文档证据；不要做无关格式化、依赖升级、数据库迁移或 Git 重置。

## 8. 分批交付与验收流程

| 批次 | 实施范围 | 完成门槛 |
|---|---|---|
| B1 协议 ⚠️ 三审未通过 | `InputSegment`、NDJSON 事件、行缓冲、整体 JSON 规范化、归属/日期校验、Prompt token/字段 | Shared 单测通过 | `f98c3a50` 修复了多菜编号和 d0；AF-12 仍绕过 D-15 日期锚定，未通过。 |
| B2 Runtime ⚠️ 三审未通过 | `AiRuntime.stream`、DeepSeek SSE、finish reason、取消保障 | Runtime 单测 + 全量 0 失败 | `f98c3a50` 修复了终态主路径；AF-10 取消/端到端测试、AF-11 Release 日志红线未通过。 |
| B3 会话 | 新状态机、generation、事件缓冲、局部 `previewAll`、确认/规则降级边界 | ViewModel 单测证明 I-01/I-02/I-03/I-04 |
| B4 输入 UI | 快速记/周期记、日期段、200 字限制、恢复和清空规则 | Compose/人工验证；无保存副作用 |
| B5 确认 UI | 渐进卡片、进度、截断/失败诊断、最终重排、部分确认 | 真机能看到增量与失败尾部 |
| B6 收尾 | 文档、唯一真机清单、构建、单测、审查 | 全部自动验证和人工清单完成 |

每批固定流程：实现 → 对应单测 → `scripts\\build-cli.bat :shared:testDebugUnitTest`（shared 改动时）→ `scripts\\build-cli.bat :androidApp:assembleDebug`（Android 改动时）→ 自审代码与日志 → 更新本文/全景图/真机清单。任何阻断问题必须在进入下一批前修复。

## 9. 必须自动化的用例

| ID | 用例 | 预期 |
|---|---|---|
| T-01 | 正常 NDJSON：meal→dish→ingredient | 增量归组成一餐一菜一食材。 |
| T-02 | dish 先到但携带合法 date/slot | 只补建精确父餐次并产生 warning。 |
| T-03 | ingredient 缺失/冲突 `dish_id` | 不挂到最近菜，进入诊断。 |
| T-04 | 同 `dish_id` 跨 `meal_id` | 保留先到合法归属，拒绝冲突。 |
| T-05 | SSE chunk 在一行 JSON 中间断开 | 仅完整换行后解析。 |
| T-06 | 完成时半行/未闭合 JSON | 已完成事件保留，尾部仅诊断。 |
| T-07 | 整体对象及数组 JSON | 均规范化后进入相同归属校验。 |
| T-08 | `finish_reason=length` | 有可见截断 warning，合法前缀可确认。 |
| T-09 | 无绝对日期、星期、绝对日期 | 分别覆盖 D-15 三条日期优先级。 |
| T-10 | generation A 取消后 A 晚到事件 | A 事件不改变 generation B 的预览。 |
| T-11 | PARTIAL_READY 点击确认 | 仅保存当时合法 preview，一次且仅一次。 |
| T-12 | AI 无有效内容后选择规则解析 | 只有显式点击后调用规则解析，来源标注正确。 |

## 10. 真机验收与交付证据

在唯一文件 `真机待验证清单_<最新时间戳>.md` 中维护以下项目，不得创建第二份清单：

1. DeepSeek 周期记：连续 7 天、每天 200 字以内，最终日期、餐次、菜品均正确归组。
2. 网络正常时确认页先出现骨架，再逐步显示餐食；生成中可滚动、可关闭重开。
3. 模拟 `length`：可见截断提示，截断前完整菜品可确认，半行不出现。
4. 返回非扁平对象/数组 JSON：能提取菜名则可预览并有兼容提示。
5. 缺 `meal_id`/`dish_id`：无错挂，诊断可见且不写库。
6. D-15 日期锚点：绝对日期、星期、无日期三类均正确。
7. 同日误关重开保留；切日期清空；保存成功后同日重开为空。
8. 已有同餐次记录：仍需两次 MERGE 确认，原记录保留。
9. Debug/Release 日志分别核验：Debug 原始片段仅会话可看；Release 不含敏感内容。

交付前必须提供：涉及文件清单、B1-B6 的通过证据、构建和单测结果、未完成真机项及原因。未满足 I-01 至 I-07 任一项，不得标记完成。
