# AI记一餐：周期记 + NDJSON流式改造落地方案

> 日期：2026-08-05  
> 状态：B1/B2 已实现但复审不通过，待按“七、验收问题反馈”修复；B3 不得开始。
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
