# AI记一餐：周期记 + NDJSON流式改造落地方案

> 日期：2026-08-05  
> 状态：B1/B2 已完成八审并通过；B3 可按开发规范开始实施。
> 目标：解决一周餐食整体 JSON 被截断、整体 schema 失败导致有效内容不可用、确认页等待时间长的问题。
> 全景图挂钩：`projectReview/21_AI与网络请求策略（专属）.md` §“周期记 + NDJSON 流式解析”；`projectReview/08_决策记录.md` D-16；`projectReview/05_诊断地图.md` AI 长输入条目；`.ai-context/docs/功能路径索引.md` AI快捷输入记餐行。
> 实施基线：`AI记一餐_周期记_NDJSON流式开发规范.md`（接口契约、状态机、分批与验收门禁）。

## 一、最终目标

- 快速记：用户直接输入，最大 200 字。
- 周期记：用户选择周/日期段，按天输入，每天最大 200 字。
- 云端请求：按日期块分段发送，max token 按场景放大，至少覆盖一周餐食解析。
- 输出协议：优先 NDJSON/JSONL，每行一个可独立校验事件；整体 JSON 作为兼容输入。
- 界面：点击发送后立即进入生成确认页，按流式事件实时展开餐食；合法内容可查看、可确认记下。
- 保存：只保存已解析且合法的餐食/菜品/食材；健康建议仍只展示，不持久化。

## 二、非范围

- 不做旧版本兼容；当前所有都是新版本。
- 不把健康建议落库、不备份、不埋点。
- 不让本地正则覆盖 AI 显式食材/调料/做法语义；本地只规范化、查重、校验和兜底。
- 不做菜品/食材自动添加成熟算法调研的落地；该项先联网调研另开任务。

## 三、协议设计

### 3.1 NDJSON 事件

每行一个 JSON 对象，示例：

```jsonl
{"type":"meal","segment_id":"week-2026-08-17-day1","meal_id":"2026-08-17|breakfast","date":"2026-08-17","slot":"breakfast","time":"08:00"}
{"type":"dish","segment_id":"week-2026-08-17-day1","meal_id":"2026-08-17|breakfast","dish_id":"2026-08-17|breakfast|d1","name":"鸡蛋饼","cooking_method":"煎"}
{"type":"ingredient","segment_id":"week-2026-08-17-day1","meal_id":"2026-08-17|breakfast","dish_id":"2026-08-17|breakfast|d1","name":"鸡蛋","role":"主料","food_group":"蛋类","nutrients":["蛋白质"]}
{"type":"ingredient","segment_id":"week-2026-08-17-day1","meal_id":"2026-08-17|breakfast","dish_id":"2026-08-17|breakfast|d1","name":"面粉","role":"主料","food_group":"谷薯类","nutrients":["碳水化合物"]}
{"type":"warning","segment_id":"week-2026-08-17-day1","meal_id":"2026-08-17|breakfast","message":"未识别具体份量，按默认份量预估"}
{"type":"done","segment_id":"week-2026-08-17-day1"}
```

硬门槛：至少能提取一个合法菜名。日期、餐次、时间、食材、调料、做法、营养大类和营养元素都尽量填，不强制。

### 3.2 事件关联键与归属校验

NDJSON 不能只靠行顺序关联，必须用稳定父子键保证“同一天同一餐”的菜品和食材不会被打散。

硬规则：

- `segment_id`：请求分段键，标识该事件来自哪一天/哪段输入。周期记按天分段时，格式建议为 `week-{anchorDate}-day{index}`。本地用它校验“本段输出不能越界写到其他段”，除非用户原文明确给了绝对日期。
- `meal_id`：餐次父键，格式建议为 `{date}|{slot}`。所有 `dish/ingredient/warning` 事件必须携带 `meal_id`；本地以 `meal_id` 聚合餐次，不以“上一行 meal”作为唯一依据。
- `dish_id`：菜品父键，格式建议为 `{meal_id}|d{index}`。所有 `ingredient/seasoning/cooking_step` 事件必须携带 `dish_id`。
- `meal` 事件必须包含 `date + slot + meal_id`；`dish` 事件必须包含 `meal_id + dish_id + name`；`ingredient` 事件必须包含 `dish_id + name`。
- 行顺序只作展示优化和容错参考，不能作为最终归属真相源。

归属校验：

- `dish.meal_id` 找不到父餐次时：若同事件有合法 `date+slot`，本地可创建对应 `meal` 并记录 warning；否则该菜进入“待确认/未归属”，不能静默挂到最近餐次。
- `ingredient.dish_id` 找不到父菜品时：若能通过同事件的 `dish_name + meal_id` 精确命中唯一菜品，可容错挂接并记录 warning；否则该食材进入该段诊断，不能静默挂到最近菜品。
- 同一 `dish_id` 出现在多个 `meal_id` 下：视为协议冲突，保留先到合法归属，后续冲突事件进诊断。
- 同一 `segment_id` 输出的 `date` 若偏离该段锚点，必须经过 `MealDateAnchorPolicy` 复核；无绝对日期时不允许模型把周一早餐改到周二晚餐。

### 3.3 兼容整体 JSON

解析顺序：

1. NDJSON/JSONL 按行解析。
2. 失败后尝试整体对象/数组 JSON。
3. 将对象/数组规范化为与 NDJSON 同一套内部事件。
4. 只要能提取合法菜名，就进入确认页并显示警告；不得因“不是扁平格式”直接拒绝。

整体 JSON 兼容时也必须先生成 `segment_id/meal_id/dish_id` 再进入同一归属校验流程，不能绕过父子键校验。

### 3.4 截断处理

- `finish_reason=length` 必须显示明确诊断：“模型输出被截断”。
- 截断前已完整解析的事件保留为可确认内容。
- 半行、半对象、未闭合尾部只进入诊断，不写库。

## 四、UI 状态机

```text
INPUT
  ├─ 快速记：单输入框 ≤200
  └─ 周期记：周/日期段 + 每天输入 ≤200
      ↓ 发送
GENERATING
  ├─ 立即进入确认页骨架
  ├─ 流式追加：日期 → 餐次 → 菜品 → 食材/建议/警告
  ├─ 可查看诊断/原始片段
  └─ 当前合法内容可确认
      ↓
PREVIEW_READY / PARTIAL_READY
      ↓ 确认记下
SAVING → DONE
```

界面兼容要求：

- 生成中也能滚动。
- 底部 CTA 固定可达或整页可滚动到。
- 支持“最终重排”：流式临时卡片可在完整 JSON/事件结束后重新归组排序。
- 支持失败尾部：已解析内容保留，失败原因进结果详情。
- 支持误关重开：同一日期未保存会话保留；切换日期或保存成功后清空。

## 五、分批实施顺序

### 批 1：协议与 max token

- `AiMealPrompt`：按快速记/周期记生成不同 prompt；要求 NDJSON 优先。
- `CloudAiRuntime`：按场景设置更大 max token；暴露/保留 `finish_reason`。
- `AiMealParser`：新增 NDJSON 事件解析入口；整体 JSON 兼容适配。
- 测试：NDJSON、多对象、数组、半截断、非扁平 JSON 均覆盖。

### 批 2：流式 Runtime 抽象

- `AiRuntime` 增加流式能力或新建 `StreamingAiRuntime`，保持非流式 fallback。
- DeepSeek 等云端模型优先接流式响应；不支持流式时走整体响应再模拟事件输出。
- 日志：Debug 可输出 AI 调查排查原始片段；Release 只保留关键状态、长度、finish_reason、错误码。

### 批 3：ViewModel 渐进状态

- `AiMealInputViewModel` 增加 `GENERATING/PARTIAL_READY` 或等价状态。
- 维护 `generatedEvents`、`previewDraft`、`diagnostic`。
- 每个完整事件到达后更新确认页。
- 已合法内容可确认；保存时只提交当前合法 preview。

### 批 4：输入 UI 改造

- `AiMealInputSheet` 增加“快速记 / 周期记”输入方式。
- 快速记 200 字限制。
- 周期记按周/日期段组织，每日 200 字限制。
- 发送后直接跳生成确认页。

### 批 5：确认页流式展示

- 餐食卡支持增量菜品、增量食材、警告、建议。
- 详情弹窗原始返回可滚动。
- “查看建议”继续只展示，不持久化。

### 批 6：验收与真机

- 更新唯一真机待验证清单。
- 跑 `scripts\build-cli.bat :androidApp:assembleDebug`。
- 必测 DeepSeek 一周菜单、截断、非扁平 JSON、日期锚点、同日误关重开、切日期清空。

## 六、关键文件

- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/AiMealPrompt.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/AiMealParser.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/MealDateAnchorPolicy.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/AiRuntime*.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ai/CloudAiRuntime.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputViewModel.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputSheet.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal/AddDayFoodScreen.kt`

## 七、验收问题反馈（2026-08-05，B1/B2 复审）

> 结论：**不通过，禁止进入 B3。** 2026-08-05 已复审提交 `fe253f70`（B1）与 `e078649e`（B2）。项目可编译，但实现未满足流式与归属契约；本节列出的项必须在同一修复批次完成代码、单测和构建证据后，再申请定向复审。

| ID | 阻断问题与定位 | 最小修复要求 | 必须新增的自动化证据 |
|---|---|---|---|
| AF-01 | **Runtime 不是逐帧流式且 Flow 不结束。** `CloudAiRuntime.stream()` 调用 `streamOnce()` 累积完全部正文后才发送一个 `Delta`，因此确认页不能随 SSE 到达渐进展示；成功、失败分支随后 `awaitClose {}` 而未 `close()`，普通 `collect` 无法自然结束。无服务端 `finish_reason` 时还错误补为 `stop`，违反“未知为 `unknown`”。 | 将 SSE 读取改为“每个非空 `delta.content` 立即发一个 `LlmStreamEvent.Delta`”；`[DONE]` 或正常 EOF 后只发一次 `Completed` 并关闭 Flow；无 `finish_reason` 传 `unknown`。`complete` 的模拟流同样只能在确知语义时用 `stop`，否则用 `unknown`。 | 用可控 SSE 输入验证：两个 data 帧产生两个按序 Delta，且 Delta 在流结束前可被收集；`[DONE]` 后只出现一次 Completed，`collect/toList` 正常返回；无 finish reason 时为 `unknown`；空 delta 不上抛。 |
| AF-02 | **取消与“收到正文后不重试”未成立。** 当前 `hasAnyContent` 仅在 `streamOnce()` 正常返回后赋值；读取中已经收到正文再抛异常时仍会被误判为“无内容”并重试。连接对象局部且 `awaitClose` 没有取消/断开正在阻塞的 HTTP 读取，不满足取消收集必须取消 HTTP。 | 将“已收到正文”的状态与连接/输入流生命周期暴露给流控制层；首个正文后发生网络错误必须保留此前 Delta、发一次 Failed、不得第二次请求。`awaitClose`/协程取消必须关闭输入流并 `disconnect()`，不得遗留脱离收集者的读取任务。 | 覆盖：首个 Delta 后 IOException 仅请求一次且 Failed 的 `retryable=false`；首帧前 IOException 才允许重试一次；取消收集后连接被关闭且不再发事件；失败流也自然结束。 |
| AF-03 | **B1 归属校验会污染草稿。** `StreamingMealParser.processLine()` 对未知 `segment_id` 仅记 warning 后仍继续创建该 segment；`meal` 的非法 `slot` 仅 warning 后仍接收；`dish_id={meal_id}|d{正整数}` 未校验，`dish` 补建父节点时也未验证 `meal_id/date/slot/dish_id` 的一致关系。这违反“精确匹配、非法归属不可进入预览”。 | 未知 segment、非法 slot、非法 meal_id/dish_id、父键不一致均只产生诊断并拒绝事件，不得创建 segment/meal/dish。父餐次补建仅允许同一 dish 事件的合法 `date + slot` 与 `meal_id`、`dish_id` 同时精确一致；否则进入未归属诊断。 | 覆盖未知 segment 不出现在 `draft.segments`；四个非法 slot 均以外值被拒绝；错误 dish_id/错误 meal_id 不创建节点；合法 dish 先到补建仍可通过。 |
| AF-04 | **整体 JSON fallback 绕过新协议归属链。** `tryWholeJsonFallback()` 直接把旧 `FlatMealJson/MultiDayJson` 转成 `fallback-day{n}` 草稿，既不映射已发送 `segment_id`，也未走相同的父子键、日期锚点和冲突校验，违背本方案 §3.3 第 3-4 步及规范 §4.1/§4.4。 | 整体对象/数组 JSON 必须先规范化为本次 `InputSegment` 的 NDJSON 等价内部事件，生成稳定 `segment_id/meal_id/dish_id`，再复用同一个归属校验入口；转换后的日期必须走 `MealDateAnchorPolicy`。不得产生不在本次请求内的 `fallback-*` segment。 | 对象与数组各覆盖：结果仅归入已发送 segment；无效日期/冲突父键被拒绝；日期锚点规则生效；可提取合法菜名时仍形成可预览草稿并有兼容诊断。 |
| AF-05 | **同键合并与细节容错不完整。** 同一 `dish_id` 的后到事件会整节点替换，而非“最新非空字段覆盖”；ingredient/seasoning 未按规范名去重合并；规范要求的 `dish_name + meal_id` 唯一补挂路径没有协议字段和实现。 | 为 dish、ingredient、seasoning 定义明确的规范化键与字段合并规则；增加 `dish_name`（或在已确认的等价字段中明确来源）并只在同餐唯一命中时补挂，同时写 warning。不能确定归属的一律诊断，不得猜测。 | 覆盖 dish 同键部分更新保留原有字段与子项；重复食材/调料合并不重复展示；唯一命中补挂成功并有 warning，多候选/零候选被拒绝。 |

**修复交付要求：**

1. 先完成 AF-01 至 AF-05 及其测试，再运行 `scripts\\build-cli.bat :shared:testDebugUnitTest` 和 `scripts\\build-cli.bat :androidApp:assembleDebug`，提交实际输出摘要。
2. 不得借修复保留新的旧协议入口、旧状态机或双轨 UI；B3 只基于修正后的 `AiRuntime.stream + StreamingMealParser` 实现。
3. 此次复审中，缓存态 shared 测试和 Android Debug 构建均显示通过；强制重跑 shared 测试在 120 秒命令上限内超时，**不得将其视为新的测试通过证据**。修复后须在可完成的环境中给出非缓存测试结果。

### 7.1 二审未关闭项（2026-08-05，提交 `4a0a61c9`）

> 结论：**仍不通过，B3 继续阻断。** 本次修复已改善未知 segment、非法 slot、逐帧发送和同键合并，但以下问题仍违反既有 AF 合同或缺少必需证据。

| ID | 未关闭问题与定位 | 最小修复要求 | 必须新增的自动化证据 |
|---|---|---|---|
| AF-06 | **AF-01/AF-02 的异常终态和取消仍错误。** `CloudAiRuntime.stream()` 在首个 Delta 之后发生 `IOException/Exception` 时仅 `break`，循环后仍发送 `Completed`（`CloudAiRuntime.kt` 155-180），把异常伪装成正常结束；`job.join()` 先等待阻塞读取结束（183），取消收集时不能依赖稍后的 `awaitClose` 来及时 `disconnect()`。本提交也没有 `CloudAiRuntime` 行为测试。 | 首帧后异常必须保留已发 Delta，再发送一次 `Failed(retryable=false)` 并关闭，绝不能发送 Completed。删除 producer 内阻塞 `job.join()`；由 `awaitClose` 立即取消 job、关闭输入流并断开当前连接，连接引用须能跨线程安全读取。为 HTTP/SSE 读取抽出可替换的 transport 或使用本地测试服务，禁止只测 `GlmProtocol`。 | 验证两个 SSE data 帧先后可见且完成后 collect 返回；首帧后 IOException = Delta + Failed、无 Completed、请求次数为 1；首帧前 IOException 才重试 1 次；取消收集可在阻塞读取期间断开连接且不再出事件。 |
| AF-07 | **AF-04 的整体 JSON 规范化仍会丢菜或跨段错挂。** Flat 路径对同日期同餐次的每个 item 固定生成 `dish_id=...|d1`（`StreamingMealParser.kt` 523-539），后项覆盖/合并前项；找不到目标日期段时 `findSegmentForDate()` 回退到第一个 segment（587-592），却保留原日期，仍会跨段污染，且没有调用 `MealDateAnchorPolicy`。 | 对每个 `(segment_id,date,slot)` 维护递增菜品序号，确保同餐多菜得到不同 `dish_id`。没有精确目标 segment 时不得任意映射到第一段；必须按该段原文和 `MealDateAnchorPolicy` 修正/拒绝并记录诊断。整体对象与数组都走同一规则。 | Flat 同日同餐两道菜必须保留两条不同 dish；周期记返回不属于任何输入段的日期不得写进任一段；绝对日期、星期、无日期三类 fallback 均证明 D-15 生效。 |
| AF-08 | **严格协议与容错入口未完整闭环。** `dish_id` 正则接受 `d0`（196），不满足正整数；虽已新增 `dish_name` 字段，但 NDJSON Prompt 没有要求 ingredient/seasoning 在缺 `dish_id` 时携带它，唯一补挂路径无法由模型稳定触发。 | `dish_id` 只接受 `d1` 及以上；更新 NDJSON Prompt，明确“缺/错 dish_id 时必须同时提供 dish_name，且仅用于唯一补挂”，并保持无唯一命中即拒绝。 | 覆盖 `d0`、`d00`、跨 meal 的 dish_id 均拒绝；Prompt 断言包含 `dish_name` 容错字段和约束；唯一/零/多候选补挂三种结果均覆盖。 |
| AF-09 | **交付证据与 Token 台账缺失。** 提交新增的根因文档未按第八节模板记录本批模型版本、调用次数、实际 Token（或“不可取得”）、测试命令与结果；`temp/test_output.txt` 还保留旧的 18 测试中 2 失败记录，不能作为本提交成功证据。 | 在同批 context-memory 快照或提交说明写完整台账；临时输出只能辅助，正式证据必须是针对当前 commit 的测试报告。无法取得平台 Token 时逐字段写“不可取得”，不得估算。 | 提供当前 commit 的非缓存 shared 测试、Android 构建输出摘要，以及完整台账。 |

### 7.2 DeepSeek 修复执行单（只做 AF-06 至 AF-09）

**范围边界：** 只允许修改 `CloudAiRuntime` 及其最小可测试 transport seam、`StreamingMealParser`、`NdjsonEvents`、`AiMealPrompt`、对应单测/测试配置和本方案指定的台账。禁止开始 B3、修改 ViewModel/UI/数据库、保留第二套流式实现或引入无关依赖升级。

#### 步骤 1：先修 AF-06，Runtime 终态只能四选一

`callbackFlow` 内启动 IO 子 job 后不得 `job.join()` 阻塞 producer；立即注册 `awaitClose`，在取消时取消 job、关闭当前输入流并 `disconnect()` 当前连接。当前活动连接的引用必须线程安全，且每次重试都替换/清理旧连接。

| 场景 | 允许事件序列 | 重试 | 最终动作 |
|---|---|---|---|
| `[DONE]` 或正常 EOF | `Delta* → Completed` | 不重试 | `close()` 一次 |
| 第一个 Delta 前的可重试 IO 失败 | 第一次不发终态；第二次失败后 `Failed(retryable=true)` | 最多 1 次 | `close()` 一次 |
| 已发至少一个 Delta 后 IO/解析失败 | `Delta* → Failed(retryable=false)` | 禁止 | `close()` 一次；**不得发 Completed** |
| 收集者取消 | 不再发送终态 | 禁止 | 立即取消 job、关闭流、断开 HTTP |

缺 API Key 也只能发 `Failed(retryable=false) → close()`。无服务端 finish reason 的正常结束必须是 `Completed(finishReason="unknown")`。

为 Runtime 增加可替换的本地 transport/连接工厂，避免真实联网测试；不要把餐食语义放入 Runtime。新增独立 Runtime 单测，证明逐帧、自然结束、首帧前重试、首帧后失败、取消断连五类行为。若 Android App 模块尚无单测 source set/dependency，只补最小必要测试配置并实际运行注册后的测试任务；`assembleDebug` 不能替代 Runtime 单测。

#### 步骤 2：修 AF-07，整体 JSON 必须不丢菜、不跨段

1. 在整体 JSON 规范化时，按 `(segment_id, date, slot)` 为 dish 分配 `d1..dN`，同餐每个原始菜品必须得到不同 `dish_id`。
2. 仅当日期精确命中已发送 `InputSegment.targetDate` 时才映射该 segment；不能命中时，先按该 segment 原文调用 `MealDateAnchorPolicy` 得到修正日期，仍不能得到唯一归属则拒绝并诊断。禁止回退到 `segments.first()`。
3. Flat 对象和 MultiDay 数组都必须走上述映射与同一个 `processLine()` 校验入口。
4. 新增测试：同日同餐两菜均保留；周期记返回输入段外日期不污染任何段；绝对日期、星期、无日期三类 fallback 都满足 D-15。

#### 步骤 3：修 AF-08，协议、Prompt、解析三处同步

1. `dish_id` 数字部分仅接受 `[1-9][0-9]*`；`d0`、`d00`、跨 `meal_id` 均拒绝。
2. 在 `NDJSON_SYSTEM_PROMPT` 的 `ingredient`/`seasoning` 事件定义和规则中加入 `dish_name`：只在 `dish_id` 缺失或无效时提供，解析器只允许同 `meal_id` 下唯一菜名命中时补挂；零/多候选一律诊断拒绝。
3. 新增 Prompt 断言及上述所有正负解析用例；不以手写测试数据绕过 Prompt 契约。

#### 步骤 4：完成 AF-09 台账与复审包

1. 在既有 `.ai-context/docs/context_memory/2026-08-05_AI记一餐周期记NDJSON流式改造.md` 追加本批台账，不新建并行交接入口。
2. 按第 8.2 节填写模型/version、调用次数、input/cached-input/output/reasoning/total-billed Token；平台不显示的字段写“不可取得”，禁止估算。
3. 提交当前 commit 的非缓存 shared 测试和 Android 构建摘要；Runtime 新增测试所在模块也必须给出实际测试命令与结果。临时文件 `temp/*.txt` 不作为正式证据，也不提交。
4. 提交前执行 `git diff --check`，提交内容仅包含本步骤的代码、测试和必要 `.ai-context` 文档；提交说明列出关闭的 AF 编号。

**再次申请复审的最小材料：** commit ID、AF-06~09 对照表、测试文件清单、逐条命令/结果、台账路径。任何一项缺失都不进入 B3。

### 7.3 三审未关闭项（2026-08-05，提交 `f98c3a50`）

> 结论：**仍不通过，B3 继续阻断。** AF-08 的 `d0` 拒绝和 Prompt 补充、AF-09 的台账格式可接受；但 AF-06 与 AF-07 尚未真正关闭，并发现 I-06 日志红线违规。不得在本轮将 B1/B2 标记为“通过”。

| ID | 未关闭问题与定位 | 最小修复要求 | 必须新增的自动化证据 |
|---|---|---|---|
| AF-10 | **AF-06 取消保障仍未实现，Runtime 测试没有测 Runtime。** `HttpUrlStreamTransport.execute()` 将 `HttpURLConnection` 局部封装；`CloudAiRuntime.awaitClose` 仅 `job.cancel()`，无法在 `BufferedReader.readLine()` 阻塞时主动关闭连接/输入流。新增 `CloudAiRuntimeStreamTest` 只测试 `readSseStream` 和 transport 构造，未创建 `CloudAiRuntime`、未注入 fake transport，也未覆盖 Delta→Failed、重试或取消。 | transport 必须显式提供当前执行的取消/关闭能力（例如 `cancelActive()` 或可关闭的执行句柄）；`awaitClose` 必须调用它并等待资源释放。`CloudAiRuntime.stream()` 通过 fake/blocking transport 做端到端单测，不得再以 reader 辅助函数测试代替。 | 正常：两 Delta 后仅一个 Completed 且 collect 返回；首 Delta 后异常：Delta→Failed(false)，无 Completed、请求 1 次；首帧前 IOException：仅重试 1 次；阻塞 transport：取消 collect 后 transport 收到取消、HTTP/输入流关闭、后续不发事件。 |
| AF-11 | **I-06 隐私红线：Release 日志和 UI 错误可泄漏服务端响应。** `HttpUrlStreamTransport` 将 `errorBody.take(200)` 拼进 `IOException`；`CloudAiRuntime` 再把 `e.message` 写入 `AppLogger.w` 并传给 `Failed`。`AppLogger.w` 在非 debug 包也写 logcat 与本地文件，因此服务端 body 可能落入 Release 日志。 | HTTP 失败只保留状态码、稳定错误码和安全短文案；不得把 error body 放入 Throwable message、`Failed.message` 或任何 `i/w/e` 日志。原始 body 若确有 Debug 排查价值，只能受现有 debug gate 保护并留在当次会话，不写 Release 文件日志。 | 伪造包含饮食文本/Key 样式字符串的 HTTP 错误 body，断言 `Failed` 与 Release 可记录字段均不包含该字符串；状态码和安全错误码仍可用于诊断。 |
| AF-12 | **AF-07 仍绕过 D-15 日期锚定。** `findSegmentForDateOrReject()` 对不等于 `InputSegment.targetDate` 的日期直接拒绝，未依据该段 `inputText` 调用 `MealDateAnchorPolicy`。这把“用户原文含绝对日期时绝对日期优先”错误地变成拒绝，且没有星期/无日期的 fallback 覆盖。 | 整体 JSON 规范化前，针对每个候选结果和其来源 `InputSegment.inputText` 调用 `MealDateAnchorPolicy`；绝对日期、星期、无日期分别按 D-15 修正后再归属。仅在策略后仍不能唯一归属时拒绝并诊断；禁止以 `targetDate` 精确相等替代策略。 | 对整体对象和数组各覆盖：原文绝对日期优先、星期映射到所选周、无日期使用所选日期；每项证明不会跨 segment 污染且保留诊断/修正信息。 |

**DeepSeek 三审修复范围：** 只处理 AF-10 至 AF-12、必要的既有测试与台账修订；更新本节和开发规范的批次状态。完成后提交“当前 commit 的”shared、androidApp Runtime 测试和 Android 构建证据，再申请复审。继续禁止 B3。

### 7.4 四审未关闭项（2026-08-05，提交 `62347448`）

> 结论：**仍不通过，B3 继续阻断。** 默认 transport 已增加 `cancelActive()`，HTTP 错误 body 已移出 Release 消息；但自动化证据并未验证 Runtime 合同，日期锚定实现也会错归周期段。不得将 B1/B2 标记为通过。

| ID | 未关闭问题与定位 | 最小修复要求 | 必须新增的自动化证据 |
|---|---|---|---|
| AF-13 | **AF-10 的 Runtime 测试仍是伪覆盖。** `CloudAiRuntimeStreamTest` 未构造 `CloudAiRuntime`、未注入 fake `StreamTransport`、未 collect Flow；所谓取消测试创建未使用的 pipe/writer，最终断言 `cancelled || !cancelled` 恒为真。为测试而将 `AiRuntimeConfig` 改为 `open` 也没有实际使用。 | 用真实 `CloudAiRuntime.stream()` 加 fake config/transport 做端到端测试；删除恒真断言、未使用的 pipe/thread 和无效测试代码。若 `AiRuntimeConfig.open` 不再是实际测试所需，恢复原有封装；若保留，测试必须真正使用替代配置。 | 正常、首帧前重试、首帧后 `Delta→Failed(false)`、取消阻塞 transport 四类均通过 `collect/toList` 或带超时的 Job 验证；取消用例断言 `cancelActive()` 被调用一次、阻塞执行被解除且没有终态/晚到事件。 |
| AF-14 | **AF-12 的日期锚定仍会错段且未使用修正日期。** `findSegmentForDateWithPolicy()` 遍历每个 segment 时，只要修正日期落在整个周期范围就立刻返回当前（通常第一个）segment；其返回值只有 segmentId，调用方继续用原始 `dateStr` 建 `meal_id`，没有使用 `MealDateAnchorPolicy` 的修正日期。`sourceInput` 始终传 `null`，新增测试也没有星期场景，绝对日期测试不校验最终 date/meal/segment。 | 将策略结果建模为 `ResolvedSegment(segmentId, correctedDate)`；合成 meal/dish 必须使用 `correctedDate`。周期记通过 `segments.first { it.targetDate == correctedDate }` 精确选段，禁止“落在范围内就取当前段”。快速记/用户原文含绝对日期时，明确保留来源 segment 与绝对日期的映射规则并以策略结果判断。删除未使用的 `sourceInput`，或传入真实来源输入。 | 周期两段：第二天结果必须只进入第二段；整体对象/数组各验证绝对日期、星期、无日期三条 D-15 规则，断言 `SegmentDraft` key、`MealDraftNode.date`、`meal_id` 均为预期值；禁止仅断言“有诊断或有 segment”。 |

**DeepSeek 四审修复范围：** 只处理 AF-13 与 AF-14、删除无效测试/无必要测试开放面、补相应测试和当前 commit 的执行证据。完成前继续禁止 B3。

### 7.5 AF-13/14 锁定实现蓝图（2026-08-05，替代开放式修复）

> 本节是 AF-13/14 唯一实施依据，优先级高于 7.4 中可能引发自由发挥的措辞。DeepSeek **不得**自行新增第二套 transport、日期归属或测试抽象；只按以下固定类型和顺序实现。除列出的文件外，不改 UI、ViewModel、shared 数据库、Prompt 内容、依赖版本或 Git 历史。完成前 B3 继续禁止开始。

#### 7.5.1 目标与不可变约束

| ID | 不可变约束 | 验收判定 |
|---|---|---|
| L-01 | 一个 `CloudAiRuntime.stream()` 调用拥有一个独立的、可取消的 HTTP call；不同流之间不共享 `activeConnection`。 | 两个 call 的取消不会互相断开；实现中 `HttpUrlStreamTransport` 不再保存跨请求连接状态。 |
| L-02 | Runtime 的测试依赖必须可伪造，生产偏好配置不可为了测试而 `open`。 | `AiRuntimeConfig` 恢复为 final；测试只构造 fake config provider 和 fake transport。 |
| L-03 | `callbackFlow` 在正常、失败、取消三种路径均只结束一次；取消不是失败终态。 | 取消后 fake call 收到一次 `cancel()`，collector 没有 `Completed`、`Failed` 或晚到 `Delta`。 |
| L-04 | 整体 JSON fallback 没有可证明来源时宁可拒绝，绝不猜测跨日期/跨 segment 归属。 | 多 segment fallback 仅产生诊断、不产生 `SegmentDraft`；周期记按“一段一请求一 parser”调用。 |
| L-05 | D-15 的策略输出必须同时决定预览段归属和实际餐食日期。 | `SegmentDraft` key、`MealDraftNode.date`、`meal_id` 都来自同一个 `correctedDate`。 |
| L-06 | Release 可见错误/日志只含稳定错误码、HTTP 状态和安全短文案。 | 服务端 body、异常 message、输入文本、Key 均不进入 `Failed.message` 或 `i/w/e` 日志。 |

#### 7.5.2 AF-13 固定抽象：配置、call 与 transport

**文件责任固定如下。** 现有 `CloudAiRuntime.kt` 可以保留 SSE 读取工具，但把下列类型放在同一 `ai` package；不要引入 Repository、ViewModel、`Any`、Map 或新的 DI 框架。

| 文件 | 必须负责 | 禁止负责 |
|---|---|---|
| `androidApp/.../ai/CloudAiRequestConfig.kt`（新建） | 将生产 `AiRuntimeConfig` 适配为 Runtime 所需的 model/key 查询。 | HTTP、SSE、偏好存储细节以外的业务逻辑。 |
| `androidApp/.../ai/StreamTransport.kt`（新建） | `StreamTransport`、每请求 `StreamCall`、HTTP 实现、只暴露安全 transport 异常。 | `AiRuntime` 事件、UI 状态、全局可取消连接。 |
| `androidApp/.../ai/CloudAiRuntime.kt` | 请求组装、重试、`AiStreamEvent` 发射、以当前 call 取消 `callbackFlow`。 | 连接字段、原始错误 body、测试专用分支。 |
| `androidApp/.../di/AndroidModule.kt`（现有实际 Koin 文件） | 仅将生产 adapter 注入 `CloudAiRuntime`。 | 测试 fake 或业务决策。 |
| `androidApp/src/test/.../ai/CloudAiRuntimeStreamTest.kt` | `CloudAiRuntime.stream()` 端到端合同测试和确定性 fake。 | pipe、真实网络、sleep 轮询、恒真断言。 |

**类型与签名锁定。** 名称、职责和可见性不得自行替换；允许因现有包名做必要 import 调整。

```kotlin
internal interface CloudAiRequestConfig {
    suspend fun selectedModel(): CloudModel
    suspend fun apiKeyForSelectedModel(): String
}

internal class PreferenceCloudAiRequestConfig(
    private val delegate: AiRuntimeConfig
) : CloudAiRequestConfig

internal interface StreamTransport {
    fun newCall(request: StreamHttpRequest): StreamCall
}

internal interface StreamCall {
    suspend fun execute(onDelta: suspend (String) -> Unit): SseStreamResult
    fun cancel()
}

internal data class StreamHttpRequest(
    val endpoint: String,
    val apiKey: String,
    val body: String
)
```

`StreamHttpRequest` 只承载已经组装完成的 HTTP 参数。`CloudAiRequestConfig` 的生产 adapter 直接委托现有 `AiRuntimeConfig`；**恢复并保持 `AiRuntimeConfig` 为 final，删除为测试加入的 `open`。** `CloudAiRuntime` 构造函数固定接收 `CloudAiRequestConfig` 与 `StreamTransport`；现有 `complete/chat` 如仍需要 model/key，同样只经 `CloudAiRequestConfig` 读取。Koin 中创建 `PreferenceCloudAiRequestConfig(get())` 后传入 Runtime。不得在测试中构造、继承或 mock `AiRuntimeConfig`。

`HttpUrlStreamTransport.newCall()` 每次返回一个新的私有 `HttpUrlStreamCall`。只有该 call 内部允许有：

```kotlin
@Volatile private var connection: HttpURLConnection? = null
```

其固定行为为：`execute()` 建连后赋值，完成/异常的 `finally` 中清空并 `disconnect()`；`cancel()` 只对本 call 当前连接执行 `disconnect()`，可重复调用但没有全局副作用。删除旧的 `StreamTransport.cancelActive()`、删除 `HttpUrlStreamTransport.activeConnection`，也删除所有调用点。禁止以 singleton/transport 字段保存“当前请求”。

HTTP 非 2xx 时 transport 只能抛出包含 `httpStatus`、稳定 `code`、安全文案的 `StreamTransportException`；原始 `errorBody` 不得放入 Throwable message。若保留 debug 排障，必须使用既有 debug gate 且只记录长度/哈希，不记录正文。`CloudAiRuntime` 捕获未知异常时也只转换为固定错误码与安全文案，日志仅记录异常类型和错误码，不能拼接 `e.message`。

#### 7.5.3 AF-13 固定运行顺序与终态规则

`CloudAiRuntime.stream()` 只可按以下顺序组织；可抽成私有函数，但不得改变顺序或引入第二个 producer/job：

1. 从 `CloudAiRequestConfig` 读取 model/key；空 key 直接发一个 `Failed(retryable=false)` 再关闭，不创建 call。
2. 创建 `AtomicReference<StreamCall?> activeCall`；每次重试都 `transport.newCall(request)`，再将该 call 写入 reference。不得重用前一次 call。
3. 执行 call 时，`onDelta` 只在当前 coroutine 未取消时发 `Delta`；首个非空 Delta 后设置 `hasDelta=true`。
4. 正常返回时，仅发一个 `Completed(finishReason)`，再关闭 channel。
5. 首帧前的可重试 transport 失败可按既有上限重试；首帧后失败或不可重试失败仅发一个 `Failed(retryable=false)`，再关闭 channel。重试前清空 reference；每个 attempt 的 `finally` 用 compare-and-set 清除自己的 call，不能清掉后续 attempt。
6. `CancellationException` 必须重新抛出，不转换为 `Failed`，不记录 warning/error 日志。
7. `awaitClose` 固定执行 `activeCall.getAndSet(null)?.cancel()` 后取消执行 job。取消发生后禁止补发任何终态或 Delta。

禁止在 `awaitClose` 外调用无归属的 transport 取消方法；禁止 catch-all 后继续向已经关闭的 channel 发送事件；禁止将“用户取消”表示为 HTTP 失败。

#### 7.5.4 AF-13 固定测试夹具和用例

测试不连接网络、不使用 `PipedInputStream`、线程、`Thread.sleep` 或“只验证 fake 自己”的断言。测试文件中必须在同一 package 实现：

```kotlin
private class FakeRequestConfig(...) : CloudAiRequestConfig
private class ScriptedStreamTransport(...) : StreamTransport
private class ScriptedStreamCall(...) : StreamCall
```

`ScriptedStreamCall` 只支持四种预定义 script：`Complete(deltas, finishReason)`、`FailBeforeDelta`、`DeltaThenFail(deltas)`、`BlockUntilCancelled`。其 `cancel()` 必须可观察地计数并解除 `BlockUntilCancelled` 的挂起；解除后若 runtime 已取消，`execute()` 不得自行制造 Delta。测试通过 `runBlocking + withTimeout`（或工程已存在的 coroutine test 调度器）真正 collect `CloudAiRuntime.stream()`，并至少覆盖下表。断言要对事件序列、次数和 call 状态作精确比较，不能只断言“非空”。

| 用例 | script | 必须断言 |
|---|---|---|
| R-01 正常 | `Complete(["A", "B"], "stop")` | 事件严格为 `Delta(A), Delta(B), Completed(stop)`；无 Failed；只创建一个 call。 |
| R-02 首帧前重试 | 第一次 `FailBeforeDelta`，第二次 `Complete(["A"], "stop")` | 创建两个不同 call；最终仅 `Delta(A), Completed(stop)`；没有第一次失败事件。 |
| R-03 首帧后失败 | `DeltaThenFail(["A"])` | 严格为 `Delta(A), Failed(retryable=false)`；没有 Completed；不创建第二个 call。 |
| R-04 取消阻塞 call | `BlockUntilCancelled`，collector 启动并确认 call 已进入执行后取消 job | `cancelCount == 1`；阻塞已解除；collector 正常结束且事件为空；取消后没有晚到事件/终态。 |
| R-05 脱敏失败 | 伪造含饮食文本和 Key 样式字串的失败 | `Failed.message`、可记录的错误字段均不含原串；包含稳定错误码或 HTTP 状态。 |

R-04 的同步点必须来自 fake 的 `CompletableDeferred`/latch（例如 `entered.await()`），而不是时间猜测。原先 `cancelled || !cancelled`、未使用 pipe、未 collect 的测试代码必须删除。

#### 7.5.5 AF-14 固定归属模型：单来源 fallback，不做跨段猜测

这次修复不再尝试以“日期落在整个周期范围”推断来源。周期记架构已经规定**每个 `InputSegment` 串行发起一个独立请求，并为该请求创建一个 parser**；因此整体 JSON fallback 的唯一安全来源就是当前 parser 所属 segment。

`StreamingMealParser` 可以继续保存 `segments: List<InputSegment>` 以兼容 NDJSON 的显式 `segment_id` 校验，但 `tryWholeJsonFallback()` 固定遵循：

1. `segments.size != 1`：追加 `whole_json_fallback_requires_single_segment` 诊断并返回；不得创建任何 `SegmentDraft`、meal、dish，也不得按 range/first/last/最近日期匹配。
2. `segments.size == 1`：该唯一 `ownerSegment` 是 fallback 的来源，调用 `MealDateAnchorPolicy.apply(ownerSegment.inputText, ownerSegment.targetDate, listOf(rawDay))`。
3. 从策略返回的唯一 `DayMealJson` 得到 `correctedDate`：有 `date` 时解析该 date；否则以 `ownerSegment.targetDate + date_offset` 物化。日期无效则诊断并拒绝该 day。
4. 创建 `ResolvedFallbackDay(segmentId = ownerSegment.segmentId, correctedDate = correctedDate, day = correctedDay)`；该值对象是 fallback 后续组装唯一输入。
5. `SegmentDraft` 的 key 使用 `ownerSegment.segmentId`；`MealDraftNode.date`、`meal_id = "${correctedDate}|${slot}"` 以及其下 dish/ingredient 父键全部使用同一个 `correctedDate`。禁止继续读取原始 `dateStr`。

在 parser 内新增如下纯值对象/私有纯函数即可，**不**新增跨 segment resolver，**不**保留 `findSegmentForDateWithPolicy()`，也不保留从未真实传入的 `sourceInput`：

```kotlin
private data class ResolvedFallbackDay(
    val segmentId: String,
    val correctedDate: LocalDate,
    val day: DayMealJson
)

private fun resolveWholeJsonFallbackDay(
    ownerSegment: InputSegment,
    rawDay: DayMealJson
): ResolvedFallbackDay?
```

绝对日期场景中 `segmentId` 仍是发起请求的来源 segment，而 meal 的 `date/meal_id` 是用户绝对日期；二者不同是合法且刻意的。星期场景按 `MealDateAnchorPolicy` 映射到 owner 目标日期所在周；无日期场景落到 owner 的目标日期。NDJSON 路径仍按事件中的显式 `segment_id` 做精确校验，不能借 fallback 放宽。

#### 7.5.6 AF-14 固定测试矩阵

删除现有“有诊断或有 segment 即通过”一类弱断言。所有 fallback 测试必须同时断言：`SegmentDraft` key、`MealDraftNode.date`、`meal_id`；整体对象和数组各跑一遍。固定夹具为两个周期 segment（例如 `s-1=2026-08-05`、`s-2=2026-08-06`），但整体 fallback 每次只向 parser 传单个 owner segment。

| 用例 | owner 输入 / 原始 day | 必须断言 |
|---|---|---|
| D-01 绝对日期，对象 | `s-1` 输入含“8月10日”，day.date=`2026-08-10` | key=`s-1`，meal date/meal_id 均为 `2026-08-10`。 |
| D-02 绝对日期，数组 | 同 D-01 的数组外壳 | 与 D-01 完全相同。 |
| D-03 星期，对象 | `s-1` 的所选周输入含星期描述，day 以 weekday/date_offset 表示 | key=`s-1`，date/meal_id 为该周正确星期，非模型原始错误日期。 |
| D-04 星期，数组 | 同 D-03 的数组外壳 | 与 D-03 完全相同。 |
| D-05 无日期，对象 | `s-2` 输入无绝对日期/星期，day.date 给出其他日期或为空 | key=`s-2`，date/meal_id 均为 `2026-08-06`。 |
| D-06 无日期，数组 | 同 D-05 的数组外壳 | 与 D-05 完全相同。 |
| D-07 多段 fallback 拒绝 | parser 传入 `[s-1, s-2]`，返回完整对象或数组 | 没有 preview 节点；有 `whole_json_fallback_requires_single_segment` 诊断；绝不落入 `s-1` 或 `s-2`。 |
| D-08 NDJSON 第二段归属 | 显式 `segment_id=s-2` 的合法事件 | 只进入 `s-2`；这是对“第二天不能落入第一段”的独立证明。 |

#### 7.5.7 DeepSeek 交付清单（逐项照做）

1. 先按 7.5.2 完成抽象收敛，删除 `AiRuntimeConfig.open`、`cancelActive()` 和错误的范围归属函数；不要先补 UI 或只补测试。
2. 再按 7.5.3 写 Runtime，按 7.5.5 写 fallback；所有生产代码只调用锁定接口。
3. 最后按 7.5.4 与 7.5.6 完成测试；测试失败时修生产实现，不降低断言、不改测试为“有任意事件即可”。
4. 执行当前 commit 的 `scripts\\build-cli.bat :shared:testDebugUnitTest`、Runtime 所在模块的精确 test task、`scripts\\build-cli.bat :androidApp:assembleDebug`；提交命令、通过/失败、耗时和测试类名。超时/缓存结果必须如实标记，不能写为通过。
5. 更新本节 AF-13/14 状态和 `.ai-context/docs/context_memory/2026-08-05_AI记一餐周期记NDJSON流式改造.md` 的实际 Token/调用台账；未取得字段写“不可取得”。
6. 提交范围仅限 AF-13/14 的生产代码、测试和上述证据文档。提交说明格式：`fix: close AF-13 AF-14 runtime contract and fallback date anchoring`。提交后提供 commit ID、R-01~R-05/D-01~D-08 对照表和命令输出摘要，再申请定向复审。

### 7.6 五审未关闭项（2026-08-05，提交 `35a18c8e`）

> 结论：**不通过，B3 继续阻断。** `35a18c8e` 正确引入 `CloudAiRequestConfig`、每请求 `StreamCall` 并将 fallback 收敛为单来源，说明 §7.5 的抽象方向成立；但取消路径仍把取消当普通异常处理，真实 IO 失败不走首帧重试，测试又以弱断言和删除既有回归用例替代了验收证据。本节是 AF-15~18 唯一修复依据。

| ID | 阻断问题与定位 | 固定修复要求 | 必须新增/恢复的证据 |
|---|---|---|---|
| AF-15 | **取消合同未兑现且存在建连竞态。** `CloudAiRuntime.stream()` 的 `catch (e: Exception)` 位于取消异常之后，`CancellationException` 会被转换为 `Failed`；Delta 的 `send()` 被取消时也会落入该分支。`HttpUrlStreamCall.cancel()` 在 `connection` 尚未赋值时只是空操作，随后 `execute()` 仍可建连、写请求并读流。定位：`CloudAiRuntime.kt` 107~133、`StreamTransport.kt` 80~114。 | `CloudAiRuntime` 必须在所有失败 catch 前 `catch (e: CancellationException) { throw e }`；Delta 回调发射前检查协程活跃状态。`HttpUrlStreamCall` 新增每 call 私有取消标记，`cancel()` 先置位再 disconnect；`execute()` 在连接赋值后、写 body 前、读响应前检查该标记，已取消则抛 `CancellationException`，不得访问网络。保留 `awaitClose` 的“call.cancel → job.cancel”顺序。 | 新增“**cancel 在 execute 建连前到达**” fake call，用可观察的 `bodyWritten`/`responseRead` 断言均为 false；R-04 收集真实 Flow 到外部列表，取消后断言列表为空、无终态、`cancelCount==1`、job 已结束。 |
| AF-16 | **真实网络 IO 错误不会重试。** `HttpUrlStreamCall.execute()` 只将非 2xx 转为 `StreamTransportException`；超时、连接重置、读失败等 `IOException` 被 Runtime 的 generic catch 直接 `Failed(STREAM_ERROR)`，绕过“首帧前可重试”。定位：`StreamTransport.kt` 83~109、`CloudAiRuntime.kt` 116~132。 | transport 统一把非取消的网络 `IOException` 转为安全 `StreamTransportException`；该异常必须有 nullable `httpStatus`、稳定 `code` 和可判定 retryable，Throwable message 不含原始 body/输入/Key。Runtime 仅根据该安全类型和 `hasDelta` 判定重试；无 HTTP 状态时 UI 文案显示 `STREAM_IO_ERROR`，不得拼 `HTTP null`。 | R-02 改为第一 call 抛“安全 IO 失败”（无 HTTP 状态），第二 call 成功；精确断言创建两个不同 call、无首轮 Failed。再加首帧前连续两次 IO 失败，只产生一个安全 Failed，且不含原始异常字符串。 |
| AF-17 | **R-01~R-05 仍非充分行为证据。** 测试没有记录 `newCall()` 次数，R-01/R-02/R-03 的“一个/两个/不重试”只在名称中；R-04 在取消后直接返回字面量 `emptyList()`，没有检查 collector 实际收到的事件；R-05 只伪造了已安全的 `StreamTransportException`，没有证明 Runtime 会剥离原始异常内容。定位：`CloudAiRuntimeStreamTest.kt` 32~42、107~125、143~181。 | `ScriptedStreamTransport` 固定保存 `createdCalls: MutableList<ScriptedStreamCall>`；R-01 精确断言 1，R-02 精确断言 2 且实例不同，R-03 精确断言 1。R-04 只能断言真实 collector 写入的外部事件列表，禁止返回固定空列表。R-05 必须让 fake call 抛包含饮食文本与 Key 样式字串的 generic `IOException`，断言 `Failed` 与 Release 可写字段均不含原串。 | R-01~R-05 全部保留；每条测试断言事件完整顺序、call 数量和终态次数。禁止 `isEmpty()` 来自字面量、禁止只断言某一类事件的 filter 结果。 |
| AF-18 | **AF-14 的日期证据不完整，并删除了既有 B1 回归。** D-03/D-04 只断言有 segment，未断言星期映射后的 `date/meal_id`；D-02/D-05/D-06 没有同时检查 `MealDraftNode.date`；无日期用例使用缺失 date，不能证明模型给出错误日期会被覆盖。与此同时 `StreamingMealParserTest` 从原有约 30 条缩减为 16 条，删除 T-03/T-04/T-06/T-07、归属隔离、调料/步骤、唯一 `dish_name` 补挂等既有风险用例。定位：`StreamingMealParserTest.kt` 85~168 及本提交对该文件的删除。 | D-01~D-06 每条同时断言 segment key、`MealDraftNode.date`、`meal_id`。D-03/D-04 的 target 为 2026-08-05（周三），原始模型 date=2026-08-10 时结果必须为 2026-08-05；D-05/D-06 同样传错误原始 date=2026-08-10，结果必须为 owner target 2026-08-06。恢复所有因本批替换而删除、且仍适用于新单来源 fallback 的既有回归测试；若某测试语义因“多段 fallback 必拒绝”改变，只更新该断言并保留测试目的。 | D-01~D-08 按 §7.5.6 的精确字段断言；恢复后 `StreamingMealParserTest` 必须覆盖 T-01~T-08、AF-03/04/05/07/08 的正反路径，不得以减少测试数换取绿灯。提交复审时给出“恢复/变更的旧用例 → 新测试名”映射表。 |

**DeepSeek 五审实施顺序（禁止自行变更）：**

1. 先完成 AF-15：为每 call 加取消标记和三处取消检查；Runtime 显式重抛 `CancellationException`。此步骤不改日期代码。
2. 完成 AF-16：将非取消 `IOException` 统一安全包装，调整 Runtime 的安全失败格式与首帧重试；不得把原始 exception message 放回日志/UI。
3. 重建 `ScriptedStreamTransport` 夹具后一次完成 AF-17 的 R-01~R-05。先让这些测试能证明错误实现会失败，再修改生产代码；测试不使用真实网络、pipe、sleep 或固定返回值。
4. 完成 AF-18：先恢复被删除的测试，再加严 D-01~D-06；只在测试证明确有错误时改 parser，保持 §7.5 的单来源模型不变。
5. 运行当前 commit 的 shared test、`CloudAiRuntimeStreamTest` 精确 task 和 Android Debug 构建；同时提供测试 XML 中的测试数/失败数与实际命令输出。任一项缺失或超时不能标记通过。

**五审申请材料：** commit ID；AF-15~18 对照表；R-01~R-05、D-01~D-08、恢复回归的测试名与结果；三条构建/测试命令的非缓存输出摘要；本批 Token 台账。未满足前 B3 继续禁止开始。

### 7.7 六审未关闭项（2026-08-05，提交 `5100ac33`）

> 结论：**B1 通过；B2 不通过，B3 继续阻断。** `5100ac33` 已完成 AF-15~18：parser 27 条回归和 D-01~D-08 的字段断言恢复，取消前检查、IO 安全错误类型与 Runtime 测试夹具均有实质改进。剩余问题都在生产 HTTP transport 的取消/重试边界，本节 AF-19/20 是唯一修复依据。

| ID | 阻断问题与定位 | 固定修复要求 | 必须新增的证据 |
|---|---|---|---|
| AF-19 | **用户取消造成的 `IOException` 仍会被包装为可重试网络失败。** `HttpUrlStreamCall.cancel()` 先置 `cancelled=true` 再 `disconnect()` 是正确的，但阻塞在 output/response/SSE read 时 `disconnect()` 常以 `IOException` 返回；当前 `catch (e: IOException)` 未先检查 `cancelled`，会抛 `StreamTransportException(STREAM_IO_ERROR)`。Runtime 因 job 已取消通常不会把它展示出来，但仍可能记录 `AppLogger.w`，并且 transport 合同本身错误地将用户取消定义为网络失败。定位：`StreamTransport.kt` 124~129。 | 在 `catch (e: IOException)` 的第一行检查 `cancelled`：为 true 时抛 `CancellationException`；仅未取消的 IO 才安全包装为 `StreamTransportException`。保持“取消异常不记录 warning/error、不重试、不发终态”。不得依赖 Runtime 的 job 取消来掩盖 transport 的错误分类。 | 新增 `DisconnectDuringRead` fake：进入阻塞 read 后由 `cancel()` 释放并抛原始 `IOException`。取消 collector 后断言只创建一个 call、collector 无事件、没有 Failed/Completed；并以可观察 logger sink 或 transport 单测证明该路径转为 `CancellationException`，非 `STREAM_IO_ERROR`。 |
| AF-20 | **HTTP 失败全部标为 retryable，且终态 `retryable` 忽略安全类型。** `HttpUrlStreamCall` 对任何非 2xx 都传 `retryable=true`，会让 400/401/403 等确定性客户端错误无意义重试；Runtime 终态又使用 `retryable = !hasDelta`，即使 `e.retryable=false` 也会向 UI 声称可重试。定位：`StreamTransport.kt` 116~129、`CloudAiRuntime.kt` 123~130。 | 固定 HTTP 重试分类：仅 `408`、`429`、`500..599` 为 retryable；其余 4xx 为 false。Runtime 的 `Failed.retryable` 必须为 `!hasDelta && e.retryable`。状态码、稳定码、UI 文案仍只使用安全字段；不改变首帧成功、首帧 IO 重试与首帧后失败规则。 | 新增 R-02c：首帧 HTTP 400 → 仅一个 call、一个 `Failed("HTTP 400 STREAM_HTTP_ERROR", retryable=false)`、无 Completed。新增 R-02d：首帧 HTTP 503 → 两个不同 call 后可成功；验证重试分类不是“所有 HTTP 都重试”。 |

**DeepSeek 六审实施顺序：**

1. 只改 `StreamTransport.kt`：先在 IOException catch 前按取消标记重抛 `CancellationException`，再加入固定 HTTP retryability 函数；不改 parser、DI、Prompt、UI 或数据库。
2. 只改 `CloudAiRuntime.kt` 的 terminal `retryable` 赋值，改为 `!hasDelta && e.retryable`；不新增第二套错误类型。
3. 只扩展 `CloudAiRuntimeStreamTest.kt`：加入 `DisconnectDuringRead`、R-02c、R-02d。禁止删减已经通过的 R/D/parser 回归。
4. 运行当前 commit 的 Runtime 精确测试、shared 全量测试和 Android Debug 构建；提交 XML 中测试数/失败数及命令输出。AF-19/20 均通过后才可申请 B2 定向复审。

### 7.8 七审未关闭项（2026-08-05，提交 `07b3f499`）

> 结论：**B2 生产逻辑符合 AF-19/20，但验收不通过，B3 继续阻断。** 代码已在 IO catch 前检查 `cancelled`，HTTP 分类和 terminal retryable 也正确；但 AF-19 新 fake 在 `cancelled=true` 时自行抛出 `CancellationException`，没有让真实 `HttpUrlStreamCall` 进入其 `catch(IOException)`。现有真实 call 测试也仅覆盖“execute 前已取消”，不是 read 被 disconnect 打断。本节 AF-21 是唯一收尾项。

| ID | 阻断问题与定位 | 固定修复要求 | 必须新增的证据 |
|---|---|---|
| AF-21 | **AF-19 的关键生产分支未被测试。** `DisconnectDuringRead` fake 在取消后直接抛 `CancellationException`，因此 Runtime 测试不会执行 `HttpUrlStreamCall.execute()` 的 `catch (e: IOException) { if (cancelled) ... }`；`HttpUrlStreamCall cancel后execute` 又在请求启动前取消。定位：`CloudAiRuntimeStreamTest.kt` 96~102、333~346；生产分支：`StreamTransport.kt` 127~133。 | 仅为可测试性增加一个**internal、默认生产实现不变**的 connection factory 构造参数：`HttpUrlStreamCall(request, connectionFactory = { URL(it).openConnection() as HttpURLConnection })`。`HttpUrlStreamTransport.newCall()` 保持无参默认 factory。不得将 factory 加到 Koin、公开 API 或 Runtime。测试用 `BlockingHttpURLConnection` 注入该 factory：成功提供 200/SSE input；input read 通过 latch 通知“已阻塞”，`disconnect()` 释放 latch 后 input 必须抛原始 `IOException("socket closed")`。 | `HttpUrlStreamCall` 直接测试：启动 `execute()` → 等待 read 已阻塞 → `cancel()` → 结果**必须**为 `CancellationException`，不是 `StreamTransportException`；同时断言 output 写过一次、response read 已进入。保留现有 Runtime 取消测试，证明该异常向上不产生 Delta/Failed/Completed。 |

**AF-21 固定实现骨架：**

```kotlin
internal class HttpUrlStreamCall(
    private val request: StreamHttpRequest,
    private val connectionFactory: (String) -> HttpURLConnection = {
        URL(it).openConnection() as HttpURLConnection
    },
) : StreamCall
```

生产代码将原 `URL(request.endpoint).openConnection()` 替换为 `connectionFactory(request.endpoint)`，其余网络行为不改。测试内的 `BlockingHttpURLConnection` 只实现本次用到的 `outputStream`、`responseCode`、`inputStream`、`disconnect()` 以及 `HttpURLConnection` 抽象方法；read 用 `CountDownLatch` 阻塞，`disconnect()` 后由 read 明确抛 `IOException`。禁止使用端口 `1`、真实公网、sleep、随机端口或 fake 自行抛 `CancellationException` 来替代该证据。

完成后运行 Runtime 精确测试、shared 全量测试和 Android Debug 构建；提交当前 XML 测试数与命令输出。AF-21 通过即 B2 通过，允许申请 B3 开始前的最终定向复审。

### 7.9 八审结论（2026-08-05，提交 `b37ace6f`）

> 结论：**通过，B1/B2 放行，允许开始 B3。** AF-21 已按 §7.8 的固定骨架关闭，没有引入第二套 transport 或扩大生产 API。

> B3 唯一实施依据：`AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md`。该蓝图按长期协作规范冻结类型、真相源、状态机、时序、allowlist 和 T-B3-01~09；编码模型不得以本节“允许开始 B3”自行推导实现。

| 验收项 | 复审结果 | 可核查证据 |
|---|---|---|
| production seam | 通过 | `HttpUrlStreamCall` 仅新增 internal 默认 `connectionFactory`；`HttpUrlStreamTransport`、Koin 与 Runtime 的生产入口不变。 |
| 阻塞读链路 | 通过 | `BlockingHttpURLConnection` 提供 200/SSE input；首行后 `BlockingSseInputStream` 以 latch 阻塞真实 `read()`。 |
| 取消分类 | 通过 | 测试依次执行 `execute()`、等待 read 阻塞、`cancel()`、`disconnect()` 释放、input 抛原始 `IOException`；生产 `catch(IOException)` 据 `cancelled` 抛出 `CancellationException`，而非 `StreamTransportException`。 |
| 请求已实际进入 | 通过 | AF-21 断言 body 已写入、response code 已读取；并保留既有 Runtime 取消事件验证。 |

本次复核实际执行 `scripts\\build-cli.bat :androidApp:testDebugUnitTest --tests "com.sxdbsm.cookbook.android.ai.CloudAiRuntimeStreamTest"`（成功）及 `scripts\\build-cli.bat :androidApp:assembleDebug`（成功）。AF-21 仅改 Android transport 与其测试，B1 shared parser 的既有 27 条回归未改动；B3 仍必须遵守开发规范的 I-01 至 I-07，不得顺带改写已通过的 B1/B2 契约。

## 八、B1/B2 质量评分与 Token 记账

### 8.1 本次质量评分

> 复审对象：`fe253f70`（B1）+ `e078649e`（B2）。得分：**52/100，不可进入下一批。** 分数用于定位改进，不替代 AF-01 至 AF-05 的逐项验收。

| 维度 | 分值 | 得分 | 依据 |
|---|---:|---:|---|
| 架构与分层 | 20 | 16 | 类型和模块位置基本正确；Runtime/Parser 边界清楚。 |
| 协议与数据正确性 | 25 | 10 | `segment_id`、slot、父子键及整体 JSON fallback 未严格遵循归属契约。 |
| 流式与协程可靠性 | 20 | 5 | 未逐帧发 Delta、Flow 不自然结束、取消与首帧后失败重试错误。 |
| 测试与可验证性 | 20 | 12 | 已有 parser/SSE 协议测试，但缺少真实流生命周期、取消、越段和 fallback 同链测试。 |
| 可维护性与文档 | 15 | 9 | KDoc 和类型较完整；实现与验收基线有脱节。 |

### 8.2 修复批次必须附带的 Token 台账

每个 DeepSeek 修复批次的提交说明或同批 `.ai-context/docs/context_memory/` 快照必须附带下表。**只能填写模型平台实际显示的数值；平台未提供则写“不可取得”，严禁按代码行数、字符数或主观估计代填。**

| 字段 | 必填内容 |
|---|---|
| 范围 | 本次关闭的 AF 编号、起止 commit、模型和模型版本。 |
| 调用量 | 调用次数；输入 Token、缓存输入 Token、输出 Token、推理 Token、总计费 Token。平台没有的字段写“不可取得”。 |
| 交付 | 修改文件、代码/测试增删行、实际运行的测试和构建命令、每项结果。 |
| 质量 | 首轮复审结果（通过/不通过）；未关闭 AF；若返工，记录返工调用次数和返工 Token。 |
| 时间 | 开始/结束时间及人工介入次数，仅作效率辅助指标。 |

格式示例：

```text
模型：DeepSeek <version>
范围：AF-01, AF-02；<baseCommit>..HEAD
调用：3 次；input=<平台值>；cached_input=<平台值/不可取得>；output=<平台值>；reasoning=<平台值/不可取得>；total_billed=<平台值/不可取得>
交付：<files>；tests=<命令 + 结果>；build=<命令 + 结果>
质量：首轮复审=<通过/不通过>；未关闭=<AF 列表或无>；返工=<次数/Token 或不可取得>
时间：<开始>..<结束>；人工介入=<次数>
```

### 8.3 多模型模式的比较口径

本轮没有“由架构模型独立实现 B1/B2”的同条件样本，且平台未暴露双方实际 Token，因此**目前不能判断是否节省 Token**。当前可确认的价值是：架构复审已在 B3 前阻断 5 类基础缺陷，避免错误扩散到 UI 和写库链路。

后续以一个完整修复批次为单位记录并比较：

1. `总计费 Token / 已关闭 AF 数`。
2. `总计费 Token / 最终通过批次数`。
3. 首轮复审通过率。
4. `返工 Token / 实现 Token`。

只有同等范围、同等验收门槛下，才可将“DeepSeek 实现 + 架构复审”的总 Token 与单模型实现进行比较；代码行数仅是交付规模，不能当作 Token 或成本。

## 九、验收红线

- AI 返回非扁平但可提取菜名时，必须规范化进入确认页。
- `finish_reason=length` 必须可见，且不丢弃已完整解析内容。
- 日期锚点仍按 D-15：绝对日期 > 所选日期；星期 = 所选日期所在周；无日期 = 所选日期。
- 流式生成中不能写库；只有用户确认才写库。
- 健康建议不能持久化。
- Release 不能输出完整 prompt、原始饮食文本、完整模型响应或 Key。
