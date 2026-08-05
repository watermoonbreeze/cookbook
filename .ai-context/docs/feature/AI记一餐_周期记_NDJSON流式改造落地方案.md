# AI记一餐：周期记 + NDJSON流式改造落地方案

> 日期：2026-08-05  
> 状态：已拍板，待下个干净 session 实施  
> 目标：解决一周餐食整体 JSON 被截断、整体 schema 失败导致有效内容不可用、确认页等待时间长的问题。
> 全景图挂钩：`projectReview/21_AI与网络请求策略（专属）.md` §“周期记 + NDJSON 流式解析”；`projectReview/08_决策记录.md` D-16；`projectReview/05_诊断地图.md` AI 长输入条目；`.ai-context/docs/功能路径索引.md` AI快捷输入记餐行。

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

## 七、验收红线

- AI 返回非扁平但可提取菜名时，必须规范化进入确认页。
- `finish_reason=length` 必须可见，且不丢弃已完整解析内容。
- 日期锚点仍按 D-15：绝对日期 > 所选日期；星期 = 所选日期所在周；无日期 = 所选日期。
- 流式生成中不能写库；只有用户确认才写库。
- 健康建议不能持久化。
- Release 不能输出完整 prompt、原始饮食文本、完整模型响应或 Key。
