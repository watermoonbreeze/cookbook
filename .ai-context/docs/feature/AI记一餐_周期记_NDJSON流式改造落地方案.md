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
