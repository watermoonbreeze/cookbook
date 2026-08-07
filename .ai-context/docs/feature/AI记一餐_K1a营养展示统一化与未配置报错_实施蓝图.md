# AI记一餐 K1a：营养展示统一化 + AI 未配置诚实报错 实施蓝图

> **🔴 2026-08-07 追记：本文档"AI 未配置诚实报错"（原 §1/§3/§4/§7 的 CFG 部分）的设计已被用户当面否决并重新设计，且已直接实施完毕（跳过 CODE 独立角色，由 ARCH 本人实现+自测）。K1a 营养展示统一化部分则仍是原设计、尚未实施、`TURN=CODE` 继续有效。下面 CFG 相关章节保留作历史记录（说明"为什么最初设计是错的"），但不再是当前行为的真相源——真相源是代码本身 + 本节摘要。**
>
> **用户纠正的原因**：原设计把"未配置 AI"当成一个需要拦下来的错误态（同步短路 + 报错 UI + "去设置"引导）。但产品实际预期是：**规则解析是 AI 的兜底，不是被 AI 挡住的替代模式**——① 标题旁应始终显示当前引擎（"AI · 模型名" 或 "规则解析"）；② 配置了 AI 时优先调用 AI，AI 某段失败要显示失败原因（复用既有 `CloudAiRuntime` 的诚实错误消息，不用新造）、然后**自动**（非手动点击）回退到规则解析，确认页要说明"这是 AI 失败后规则解析的结果"。
>
> **实际最终设计**（已实现，非本文档原方案）：
> - `AiMealInputViewModel` 新增 `configReady`（是否有真实可用云端 AI，判据 `activeType()==CLOUD && currentCloudApiKey().isNotBlank()`）+ `refreshEngineStatus()`（由 `AiMealInputSheet` 的 `LaunchedEffect(Unit)` 驱动，覆盖"配置Key后返回"场景）+ `engineLabel`（标题旁始终可见的"AI·模型名"/"规则解析"徽标）。
> - 每个 segment 在 AI 侧失败（`LlmStreamEvent.Failed`/流异常/流意外结束/未配置直接跳过 AI）时，**自动**触发 `attemptRuleFallback()` 调用 `sessionPort.parseRule()`，用**该段自身**的解析结果补进 `mergeDays()` 产出的合并结果；只有 AI 和规则**都**没解析出内容才是真正的 ERROR。
> - 确认页复用既有 `parseSourceMessage` 字段披露"本次结果：规则解析（AI 解析失败：<原因>）"或"部分内容由规则解析补充"。
> - 独立 Google 质量审查（两轮）发现并修复 5 处真实阻断（详见下方"实施审查记录"）：`mergeDays` 用 segmentId 而非日期字符串匹配（避免 AI 声明"昨天"这类不同日期被误判丢弃）、`isStreaming` 状态守卫（避免已成功段被误判未终态）、内部哨兵文案人性化（不泄露 `AI_NOT_CONFIGURED` 等内部代号给用户）、`RuleFallbackResult.warning` 不再丢失、周期记非首段规则兜底日期锚点修正；二轮复核还额外发现并补上"AI 正常 Completed 但没解析出任何菜"这条路径此前完全没有自动兜底的覆盖缺口。
> - 涉及文件：`androidApp/.../ui/ai/AiMealInputViewModel.kt`、`AiMealInputSheet.kt`、`shared/.../ai/meallog/StreamingMealSession.kt`（新增 `daysForSegment`/`isStreaming`）、对应测试 `AiMealInputViewModelStreamTest.kt`（新增/重写 T-CFG-01~06、T-B3-05/05b/07、T-B3-02）。
> - 验证：`:androidApp:testDebugUnitTest`（含本文件全部新/改测试）、`:shared:testDebugUnitTest`、`:androidApp:assembleDebug` 均 0 failures / BUILD SUCCESSFUL。
>
> K1a 营养展示统一化部分（§1.3 In Scope 第 1-3 条）**仍未实施**，蓝图设计不变，`BLUEPRINT_STATE.md` 的 `TURN=CODE` 仍指向这部分。

> 状态：`ACCEPTED`（GC-37 独立挑战已完成，13 项中 6 项 CONFIRMED-ISSUE 已就地修订，详见 §10；**注：本轮挑战针对的是已被否决的原 CFG 设计，不代表实际实施方案已过挑战**——实际实施方案经过了两轮独立 Google 质量审查，见上方追记）
> **颗粒度：L7**（项目基线；GC 条款清单见 `experience/12_多模型协作与实施蓝图规范.md` §12）。**§0.1 是入口——先读它，逐行对照落点章节。**
> 起草日期：2026-08-07（AI记一餐 B4+B5+B6 批次 ACCEPTED 后，用户装机真机验证期间由 ARCH@主力机 起草）；同日经独立 opus 挑战 agent（GC-37）攻击后修订定稿。
> 前置：`AI快捷记一餐_进阶_架构方案.md`（K1a 的原始产品设计，2026-08-01，📄待拍板未编码；本蓝图是其 P2-1 子集的可执行细化，K1b/K1c 处置见 §1.3）
> 依赖检查：K1b 强依赖 L1 合规闸门（`待办索引.md` 确认 L1 仍 ⬜ 未做）→ **K1b 继续不在范围**；K1c(weekday 推算) 的 `weekdayToIso` 已在流式主路径生产使用（`MealDateAnchorPolicy.kt:35-36` ← `StreamingMealParser.kt:510`），`weekdayToDateOffset`/`parseWeekdayHint` 只在孤儿路径 `useRuleFallback()`（B5 后无 UI 调用点）可达 → **本批仍不需要动，但不等同"三个函数都在生产路径覆盖"，修正表述**。

---

### §0.1 颗粒度勾销表（GRANULARITY = L7）

| GC | 条款一句话 | 本蓝图落点 | 状态 |
|---|---|---|:--:|
| GC-01 | 每个行为分支写成"条件→唯一动作→禁止动作"，对歧义词 grep 零命中 | §3 不变量表 | 满足 |
| GC-02 | allowlist：文件×允许操作×禁止操作 + 显式禁改清单 | §6 | 满足 |
| GC-03 | 上一批延后项归宿表 | — | N/A：上一批（B4+B5+B6）已全部关闭且 ACCEPTED，无延后项 |
| GC-04 | 每条 INV 具备 ID/条件/必须结果/禁止结果/证据五列 | §3 | 满足 |
| GC-05 | INV↔T 双向映射表 | §8.2 | 满足 |
| GC-06 | 放行条件写出命令原文 | §7 末尾"验收命令" | 满足 |
| GC-07 | 测试夹具职责边界表 | §8.1 | 满足 |
| GC-08 | 交付台账含真机清单文件名+编号区间 | §9（交付时登记，本蓝图先声明编号前缀 `E-K1A-*`） | 满足 |
| GC-09 | 列出本批不得失败的既有回归套件全名 | §6 末尾"回归基线锁定" | 满足 |
| GC-10 | 逐字段真相源表 | §4.1 | 满足 |
| GC-11 | 新增/重命名与既有字段语义重叠字段时给旧字段全部写入点清单 | §4.2（`DishPreview.estimatedKcal`→`nutrition` 全写入点） | 满足 |
| GC-12 | UI 判据与业务判据同源表 | §4.3（`errorKind` 判据） | 满足 |
| GC-13 | fallback 先转换为主路径类型再复用主路径校验入口 | — | N/A：本批无 fallback 路径新增（未接线自动规则兜底，见 §1.3 弃置项） |
| GC-14~16 | 对象生命周期表 / 可变持有物传递形态 / 搬迁历史注释清单 | — | N/A：本批不涉及系统资源持有对象（麦克风/Stream/Job），不搬迁既有代码块 |
| GC-17~19 | 逐项状态 List&lt;Status&gt; / 索引空间标注 / 过滤链画出 | — | N/A：本批无"列表逐项状态由数据层产出"场景（营养字段是值对象非状态机） |
| GC-20 | 自动副作用清单表 | — | N/A：本批无新增静默截断/丢弃/降级行为（NOT_CONFIGURED 分支是**主动告知**，非静默副作用，已入 §3 INV 而非"需要另外告知的副作用"） |
| GC-21 | INV 写"提示/告知"字样必须有 STEP 落点 | §7 STEP-CFG-4（CTA 按钮的代码落点） | 满足 |
| GC-22 | 每条可见副作用配 T-ID/真机项编号 | §8.2 | 满足 |
| GC-23 | 实施脚本每个最小动作独立编号 STEP，含文件/定位/动作/完成形态 | §7 | 满足 |
| GC-24 | 交付台账含 STEP 勾销表，Evidence 只能引用真实存在的测试/commit | §9（交付时填，本蓝图预留表头） | 满足 |
| GC-25 | STEP 完成形态是字面量时写出目标字面量原文+grep判据 | §7 每条 STEP 末尾 | 满足 |
| GC-26 | 冻结值修订记录表 | — | N/A：本批不改任何已冻结阈值/常量（复用 `SODIUM_HIGH_PER_DISH_MG` 等既有值，未新增/修改冻结值） |
| GC-27 | 编辑即失效收口函数（`invalidateGenerationToInput`）核对表 | — | N/A：本批新增入口是 `submit()` 开头的只读前置检查，不是"编辑草稿"类入口，不产生"编辑未收口"风险；`submit()` 本身早已是既有唯一发送入口，未新增第二个发送入口 |
| GC-28 | 构造时创建、后续多次迭代复用对象是否按基数分片 | — | N/A：本批新增的批量查询（`nutritionRepo.dishNutrition(reuseIds)`）是**一次性**调用，非跨迭代复用的累积态对象 |
| GC-29 | 多来源写入同一聚合目标必须声明合并/覆盖 | §4.2（REUSE 菜品 `nutrition` 字段的唯一写入者是 `previewAll()` 后处理步骤，`DishAutoGenerator.preview()` 对 REUSE 分支写 `null` 占位，无并发写入同一字段） | 满足 |
| GC-30 | 状态转移驱动完整副作用链，非只做字面量最小实现 | §3 INV-CFG-01/02（NOT_CONFIGURED 分支要求同时清空 `parseWarnings`/不产出 `segmentStates`，非只改 `errorMessage`） | 满足 |
| GC-31 | 挂起点清单 + 恢复后重新校验 generation 身份 | — | N/A（**GC-37 挑战后修订机制，结论不变**）：`refreshConfigReady()` 的挂起点由 `AiMealInputSheet` 的 `LaunchedEffect(Unit)` 触发，写入的 `configReady` 与任何 `generationId` 无关联（不是流式 session 状态），恢复后直接覆盖赋值即为正确结果，不存在"更新的 generation 已开始、旧挂起点仍写入过期数据"的竞态类别；`submit()` 内读取是同步属性读 |
| GC-32 | 高频异步事件的节流/去重策略 | — | N/A：本批不新增高频异步事件（`refreshConfigReady()` 只在 Sheet 每次进入组合时触发一次，非逐 Delta 级） |
| GC-33 | 禁止为测试暴露新的可变全局注入点 | — | N/A：本批不新增 `var xxx` + `replaceXxxForTest` 模式；既有 `sessionPort`/`replaceSessionPortForTest` 是历史遗留，非本批引入，不在本批修复范围 |
| GC-34 | 复核注释/KDoc 与实现一致性 | §7 每条 STEP 要求同步改注释 | 满足 |
| GC-35 | 协议事件枚举与处理分支逐项对照，未处理枚举禁止直接透传用户 | — | N/A：本批不新增网络协议/NDJSON 事件类型，`errorKind` 是纯 UI 内部状态，非协议层枚举 |
| GC-36 | 数据层 List&lt;Status&gt; 前先列真实状态空间，核对值域覆盖 | §3 脚注（`errorKind` 二值范围的边界论证，明确不是"假装穷尽了 AI 失败原因空间"） | 满足 |
| GC-37 | 蓝图冻结前存在独立挑战台账 | §10 | 满足（独立 opus 挑战 agent，14 项，6 项 CONFIRMED-ISSUE 全部采纳修订，4 项 MINOR-NIT 采纳，4 项 CONFIRMED-FINE 无需改动） |

任一条"未满足"须先处理。当前无"未满足"项；GC-37 待独立挑战通过后由 ARCH 补齐、蓝图状态方可从 `BLUEPRINT_READY` 转 `ACCEPTED`。

---

## §1 目标与范围

### 1.1 一句话价值

把"AI 快捷记一餐"预览确认页的营养展示做到"CREATE 和 REUSE 两类菜都有真实营养数据、口径统一"，同时把"未配置 AI 时的报错"从误导性文案改成诚实告知+可操作引导。

### 1.2 触发来源

- **K1a**（`待办_功能算法.md` 🔄）：AI 预览确认页展示营养素和热量，K1 Phase2 剩余项。
- **用户报告 Bug**（2026-08-07，本蓝图起草会话）：AI 快捷记未配置 AI 时点发送，提示"没能识别出菜品，试试更具体的描述？"——该文案在"未配置"场景下是误导的（实际原因是 Key 未填，不是描述不够具体）。

### 1.3 In Scope / Out of Scope

**In Scope**：
1. `DishAutoGenerator.preview()` 的 CREATE 分支：营养计算从"手写 kcal-only 累加公式"改为复用 `NutritionCalculator.dishNutrition()`（单一真相源，产出全部宏量素）。
2. `MultiDayRecorder.previewAll()`：REUSE 菜品（AI 识别为库内已有菜，当前 `ingredients=[]`/`estimatedKcal=null` 完全不显营养）新增批量营养回填。
3. `AiMealInputSheet.kt` 的 `MealPreviewCard`：热量展示从手写文案换成直接复用既有 `DishNutritionLine` 组件（同时补上蛋白/脂肪/碳水/钠提示，这些当前完全没有）。
4. `AiMealInputViewModel.submit()`：新增"AI 未配置"前置检测，短路为诚实错误态，不发起任何网络请求。
5. `AiMealInputSheet.kt` 的 `ErrorPhase`：按"未配置" vs "其他失败"分支，未配置时给"去设置"CTA，路由到既有 `AiSettingsScreen`。

**Out of Scope（本批不做，理由见括号）**：
- **K1b 健康评价**（L1 合规闸门未完成，架构方案 §3 明确"K1b 门禁：强依赖 L1，L1 未落地则不启用"，继续冻结）。
- **K1c weekday 推算**（已实现，代码已在生产路径上，无需动）。
- **每餐/每天营养合计**（原始 K1a 描述是"预览确认页展示营养素和热量"，架构方案 §3 数据流图写的是"每菜/每餐"，但每餐合计需要新 UI 组件而非纯复用，为控制本批范围只做菜品级——**已有权威设计** `DishNutritionLine` 是菜品级组件，餐级合计留作 fast-follow）。
- **食材级 REUSE 精度**（`IngredientAutoGenerator.preview()` 对已入库食材仍用 `NutritionGuesser.guess()` 近似值而非该食材在库内的真实 `IngredientNutrition`，是本批引入前就存在的既有精度限制，修复需要新的批量食材营养查询，属于独立 fast-follow，见 §11 弃置项）。
- **规则兜底解析按钮接线**（`useRuleFallback()` 已存在但 B5 之后 UI 无调用点，是与本 bug 相邻但独立的既有缺口，不在本次"诚实报错"范围内——本批只解决"报错文案是否诚实"，不解决"失败后有没有本地兜底路径"）。

---

## §2 现状与差距

| # | 现状 | 证据（file:line） | 差距/影响 |
|---|---|---|---|
| D1 | CREATE 菜品营养计算是独立手写公式（只算 kcal），与 `NutritionCalculator`/`DishNutritionLine` 口径不同源 | `shared/.../domain/autogen/DishAutoGenerator.kt:86-91`（`kcalPer100 * ip.quantity / 100.0` 手工 fold） | 违反"单一真相源"红线；只有热量，无蛋白/脂肪/碳水/钠 |
| D2 | REUSE 菜品（AI 识别为库内已有菜）完全不显营养 | `shared/.../domain/autogen/DishAutoGenerator.kt:52-61`（`ingredients = emptyList()`, `estimatedKcal = null`） | K1a 对"识别到已有菜"这条主路径完全未兑现（这恰恰是最常见的场景——家常菜大概率已在库里） |
| D3 | `MealPreviewCard` 只显热量（受开关），无宏量/钠提示，且用手写重复逻辑而非复用 `DishNutritionLine` | `androidApp/.../ui/ai/AiMealInputSheet.kt:1158-1173` | 与 `DayMealCardView`（首页/食历）视觉和信息量不一致，用户体验割裂 |
| D4 | 未配置 AI 时，`submit()` 无前置检测，未配置与"AI 解析失败"共用同一句文案，真实原因（"XX API Key 未配置"）只沉在次要折叠区 | `AiMealInputViewModel.kt:362`(submit 无检测)、`:490-503`(唯一 ERROR 分支)、`:494`(误导文案)、`:499`(真实原因降级进 parseWarnings)；`androidApp/.../ui/ai/AiSettingsScreen.kt`(设置页已存在但 Sheet 内无跳转入口) | 用户看到"试试更具体的描述"会去改措辞而不是去配置 Key，白费一次尝试；构造已注入的 `config: AiRuntimeConfig`（`:154`）全文件零调用，`isModelReady()` 是现成能力（已被 `AiRecommendViewModel`/`AiPlanViewModel` 同款方式使用） |
| D5（记录不处理） | 食材级 REUSE（命中库内已有食材）仍用 `NutritionGuesser.guess()` 近似值而非该食材真实 `IngredientNutrition` | `shared/.../domain/autogen/IngredientAutoGenerator.kt:68-69` | 本批统一菜品级计算源后，此项残留精度限制未同步解决（见 §1.3 弃置理由），CREATE 菜品若含 REUSE 食材，展示的宏量素仍是"估算值"而非"实际库内值" |

---

## §3 不变量

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-K1A-01 | CREATE 菜品（`resolution==CREATE`, 至少 1 个食材）preview 时 | 用 `NutritionCalculator.dishNutrition(ingredients.map{it.toNutritionInput()})` 计算，产出含蛋白/脂肪/碳水/钠/纤维等全部字段的 `DishNutrition`（**不做 `hasData` 过滤，无论是否有数据都产出非空对象**——见 INV-K1A-04） | 另起独立公式手算任何单一营养素（含热量） | T-K1A-01 |
| INV-K1A-02 | REUSE 菜品（`resolution==REUSE`, `existingId!=null`）preview 完成后 | `MultiDayRecorder.previewAll()` 用**一次批量**查询（`nutritionRepo.dishNutrition(reuseIds)`）填充其 `nutrition` 字段 | 对每个 REUSE 菜品逐个单独查询（N+1） | T-K1A-02, T-K1A-03 |
| INV-K1A-03 | `MealPreviewCard` 渲染每道菜的营养行 | 唯一入口 `DishNutritionLine(dishPreview.nutrition?.toDishNutritionUi())` | 保留旧的手写 kcal 文案分支（重复实现导致口径漂移风险） | T-K1A-04（UI 层，走真机） |
| INV-K1A-04（**GC-37 挑战后修订**） | 菜品营养展示时 | 区分两种"无数据"：①菜名为空（`DishAutoGenerator.preview()` 的早退分支，从未尝试计算）→ `nutrition==null` → `DishNutritionLine(null)` 静默不渲染（该分支本就不产出可见菜行，符合现状）；②尝试计算但食材全无营养数据（`nutrition!=null && nutrition.hasData==false`，CREATE 与 REUSE 两路径都可能落入，REUSE 因 `dishNutrition()` 用 `associateWith` 保证每个请求 id 都有非空条目而**必然**是这一类而非①类）→ 显"营养待完善" | ①类和②类混淆（`.takeIf{it.hasData}` 会把②错误地拍扁成①，导致该菜整行不显示任何文字，而非显"营养待完善"——这正是 GC-37 挑战 #2 抓到的回归） | T-K1A-01（覆盖①②两分支） |
| INV-K1A-05（**GC-37 挑战新增，挑战 #6**） | CREATE 菜品的食材中，只要有一个 `IngredientPreview.nutrition.source` 不是 `NutritionGuessSource.Match`（即 Group 均值估算或完全未匹配） | `DishAutoGenerator.preview()` 对 `NutritionCalculator.dishNutrition(...)` 的结果强制 `.copy(estimated = true)`，使 `DishNutrition.complete` 为 `false`，最终 `DishNutritionUi.estimated` 为 `true`（UI 显"（估算）"尾注） | 因固定传入 `NutritionInput(unitGrams = 1.0)`（非空）导致 `resolveGrams()` 内部分支永远走不到 est=true 分支，从而让"（估算）"标记对含猜测营养值的菜永久消失（营养数据诚实性红线：估算值必须标注，不能看起来像权威数值） | T-K1A-01（新增断言：含 Group 均值食材的 CREATE 菜品 → `nutrition.estimated==true`） |
| INV-CFG-01（**GC-37 挑战后修订**） | 用户点击发送（`submit()` 调用时）且 `configReady==false`（`configReady` 的判定见下方"配置预取"说明，**不等价于** `AiRuntimeConfig.isModelReady()==false`——见 GC-36 脚注修订） | 立即（同步、不经过任何 suspend/网络调用）把状态置为 `phase=ERROR, errorKind=NOT_CONFIGURED, errorMessage="还没有配置 AI，先去设置里填一下", parseWarnings=emptyList()`；不构造 `StreamingMealSession`；不调用 `aiRuntime.stream()` | 发起任何一次 LLM 请求后才判定失败；把"未配置"和"解析失败"用同一句文案表达；把 MOCK/ON_DEVICE 运行时误判为"未配置"（见 GC-36 脚注修订） | T-CFG-01 |
| INV-CFG-02 | `configReady==true`（含所有非"CLOUD+Key 为空"的情形：CLOUD 已填 Key / MOCK / ON_DEVICE） | `submit()` 行为与本蓝图之前完全一致（既有 GENERIC 错误路径、既有 PARTIAL_READY/PREVIEW_READY 流程不变），**且无需修改任何既有测试文件**（见 §4.4"配置预取"说明的测试友好性论证） | 引入任何新的中间态影响既有回归；要求已有测试夹具改动才能保持绿 | T-CFG-02（既有 `AiMealInputViewModelStreamTest` 9/9、`GenerationProgressTest` 4/4 保持绿，零改动） |
| INV-CFG-03 | `state.errorKind==NOT_CONFIGURED` 时 `ErrorPhase` 渲染 | 主按钮显"去设置"，点击触发 `onNavigateToAiSettings` 回调路由到 `AiSettingsScreen`；次要按钮保留"重新输入" | 显示原有的"重试保存"按钮（NOT_CONFIGURED 场景下必然 `autoGenPreview==null`，该按钮本就不该出现，但仍需保证分支正确，不能因加新分支破坏既有 `autoGenPreview!=null` 判断） | 真机 E-K1A-CFG-01 |
| INV-CFG-04（**GC-37 挑战新增，挑战 #1**） | `phase` 转为 `ERROR` 的任一时刻（`submit()` 短路 或 `handleSessionSnapshot()` 既有 `:490-503` 分支） | 该次转移必须**显式**写 `errorKind`（短路写 `NOT_CONFIGURED`，`:490-503` 分支新增一行写 `GENERIC`）——`errorKind` 不允许"沿用上一次 `.copy()` 前的旧值" | 用户在 NOT_CONFIGURED 态配置 Key 后重试、真解析失败，`ErrorPhase` 仍显"去设置"CTA（`errorKind` 停留在陈旧的 `NOT_CONFIGURED`） | T-CFG-03 |
| INV-CFG-05（**GC-37 挑战新增，挑战 #3**） | 用户从 `ErrorPhase` 的"去设置"CTA 跳转 `AiSettingsScreen` 配置 Key 后返回本 Sheet | `configReady` 必须被重新求值（不是维持进入 Sheet 那一刻的陈旧值），使得配置后立即可以正常发送 | "去设置"CTA 变成死路：配置完 Key 回来仍提示"还没有配置 AI" | 真机 E-K1A-CFG-02 |

**GC-36 脚注（`errorKind` 值域边界论证，GC-37 挑战 #5 后修订）**：`AiMealErrorKind{GENERIC, NOT_CONFIGURED}` 只有两个值，**刻意不**试图穷举"AI 为什么失败"的完整原因空间（超时/401/429/限流/文本过于模糊等，仍全部落在既有 `GENERIC` 分支，行为不变）。**挑战发现原判据 `!config.isModelReady()` 会把 MOCK（用户主动选择的"规则推荐·离线可用"）和 ON_DEVICE 一并误判为"未配置"，而这两者根本不该走这条新分支**——`isModelReady()==false` 描述的是"三种运行时里有没有一种能真正调用模型"，值域比"CLOUD 选中但 Key 为空"更宽，直接拿来做 NOT_CONFIGURED 判据本身就是 GC-36 想防的"表面合规、值域不对齐"。修订后的判据收窄为 `config.activeType() == AiRuntimeType.CLOUD && config.currentCloudApiKey().isBlank()`（见 §4.4），这才是**真正**"客户端在发起任何网络请求之前就能确定性判断、且值域精确对应'未配置'这一用户可理解概念"的分支；MOCK/ON_DEVICE 用户完全不受本批影响，继续走原有 `submit()` 路径。若未来要细分 `GENERIC` 内部（如区分超时/限流），应在那批蓝图里对该子空间单独做值域覆盖论证，本批不预支这个设计。

---

## §4 接口契约

### 4.1 逐字段真相源表

| 字段 | 唯一写入者 | 读取方 | 终局形态 |
|---|---|---|---|
| `DishPreview.nutrition: DishNutrition?` | CREATE：`DishAutoGenerator.preview()`（一次性构造）；REUSE：`MultiDayRecorder.previewAll()` 后处理（一次性 `.copy()`，非逐步累积写） | `AiMealInputSheet.kt` `MealPreviewCard` | 替换（删除 `estimatedKcal`，见 §4.2） |
| `AiMealInputUiState.errorKind: AiMealErrorKind` | `AiMealInputViewModel`：`submit()` 短路分支写 `NOT_CONFIGURED`；`handleSessionSnapshot()` 既有 `:490-503` 分支**新增一行显式写** `GENERIC`（**GC-37 挑战 #1 修订：不得省略这一行**——`.copy()` 会沿用上一次的 `errorKind`，若不显式写，用户"未配置→去设置→回来重试→真解析失败"时 `errorKind` 会停留在陈旧的 `NOT_CONFIGURED`，`ErrorPhase` 显示错误的 CTA，见 INV-CFG-04） | `AiMealInputSheet.kt` `ErrorPhase` | 新增字段，默认 `GENERIC`，`phase` 转 `ERROR` 的两处写入点都必须显式赋值 |
| `AiMealInputViewModel.configReady: Boolean`（private，默认 `true`） | 唯一写入者是 `refreshConfigReady()`（public suspend-launch 包装，**GC-37 挑战 #3/#4 修订**：不再由 `init{}` 单次挂起写入，见下方"配置预取"说明） | `submit()` 同步读一次 | 新增私有属性，不对外暴露；写入时机改为"每次 Sheet 进入可交互态"而非"VM 构造时一次性" |

### 4.2 GC-11：`DishPreview.estimatedKcal` → `nutrition` 全部写入点迁移清单

旧字段 `estimatedKcal: Double?` 的全部写入点（grep 已核对，**共 3 处**生产代码——原稿"仅 2 处"是计数笔误，下表本就列了 3 行，此处订正为与表一致）：

| 写入点 | 处置 |
|---|---|
| `DishAutoGenerator.kt:46`（空菜名分支，写 `null`） | 改写 `nutrition = null`（真正"从未尝试计算"的场景，见 INV-K1A-04 的①类） |
| `DishAutoGenerator.kt:59`（REUSE 分支，写 `null`） | 改写 `nutrition = null`（占位，`previewAll()` 后处理会用 `.copy()` 填充真实值——填充后必为非空 `DishNutrition`，即使 `hasData=false`，因为 `NutritionRepository.dishNutrition()` 用 `associateWith` 保证每个请求 id 都有条目） |
| `DishAutoGenerator.kt:99`（CREATE 分支，写手算 fold 结果） | 改写为 `NutritionCalculator.dishNutrition(...)` 的结果，**不做 `hasData` 过滤**（见 §7 STEP-K1A-1.2，GC-37 挑战 #2 修订——过滤会把"算了但没数据"错误拍扁成"没算"，导致 `DishNutritionLine(null)` 静默不渲染而非显"营养待完善"） |

旧字段的全部读取点（grep 已核对，仅 1 处生产代码）：

| 读取点 | 处置 |
|---|---|
| `AiMealInputSheet.kt:1110`（`val kcal = dishPreview.estimatedKcal`）+ `:1159-1173`（渲染逻辑） | 整块替换为 `DishNutritionLine(dishPreview.nutrition?.toDishNutritionUi())`（见 §7 STEP-K1A-3.1） |

**终局裁决**：**替换**（删除 `estimatedKcal`，不并存）。理由：字段语义完全被 `nutrition: DishNutrition?` 覆盖（`DishNutrition.hasData`/`.totals.energyKcal` 可推出旧字段的全部信息），并存会产生"两个真相源"风险。

### 4.3 GC-12：UI 判据与业务判据同源表

| UI 表现 | 判据来源 |
|---|---|
| `ErrorPhase` 显示"去设置"CTA | `state.errorKind == AiMealErrorKind.NOT_CONFIGURED`（与 `submit()` 内写入该字段用的是同一次 `refreshConfigReady()` 结果，无第二套判断逻辑） |
| `MealPreviewCard` 显"营养待完善" | `dishPreview.nutrition?.hasData != true`（与 `DishNutritionLine` 内部判据完全一致，因为直接调用同一组件，非各自实现） |

### 4.4 新增/变更函数签名

```kotlin
// shared/.../domain/autogen/DishAutoGenerator.kt（新增 private 扩展函数）
private fun IngredientPreview.toNutritionInput(): NutritionInput = NutritionInput(
    quantity = quantity,       // GC-37 挑战 #8 修订措辞：quantity 在 preview 与 commit 两条路径下都被当作克数使用
                                // （IngredientAutoGenerator.preview() 固定 unitId=ctx.gramUnitId，DishAutoGenerator.commit()
                                // 原样传 unitId 落库）——这是"preview/commit 口径一致"的既有事实，不代表 AI 给出的原始
                                // unit（如"个/勺"）真的做过克重换算；SemanticIngredient.unit 目前被整层丢弃未读（见 §12 D5 关联项）。
    unitGrams = 1.0,
    nutrition = nutrition.values?.let { v ->
        IngredientNutrition(
            ingredientId = existingId ?: 0L, // 占位；NutritionCalculator.dishNutrition() 不读取该字段
            energyKcal = v.energyKcal, proteinG = v.proteinG, fatG = v.fatG, carbG = v.carbG,
            fiberG = v.fiberG, sodiumMg = v.sodiumMg, potassiumMg = v.potassiumMg,
            calciumMg = v.calciumMg, gi = v.gi, purineMg = v.purineMg,
        )
    },
)
```

```kotlin
// shared/.../domain/autogen/AutoGenModels.kt（DishPreview 字段变更）
data class DishPreview(
    val inputName: String,
    val resolution: ResolveKind,
    val existingId: Long?,
    val ingredients: List<IngredientPreview>,
    val source: String,
    val nutrition: DishNutrition?,   // 原 estimatedKcal: Double?
    val eatenRatio: Double? = null,
)
```

```kotlin
// shared/.../ai/meallog/MultiDayRecorder.kt（previewAll 新增后处理，GC-37 挑战 #7 已验证 nutritionRepo 字段与批量签名均真实存在）
suspend fun previewAll(days: List<DayMealJson>, today: LocalDate): AutoGenPreview = withContext(ioDispatcher) {
    val autoGenContext = AutoGenContext.load(db, aliasResolver)
    val preview = buildDayGen().preview(days.map { toSemanticDay(it) }, today, autoGenContext)
    val reuseIds = preview.days.asSequence()
        .flatMap { it.meals }.flatMap { it.dishes }
        .filter { it.resolution == ResolveKind.REUSE }
        .mapNotNull { it.existingId }
        .distinct().toList()
    if (reuseIds.isEmpty()) return@withContext preview
    val nutritionById = nutritionRepo.dishNutrition(reuseIds) // 一次批量查询，非循环
    preview.copy(days = preview.days.map { day ->
        day.copy(meals = day.meals.map { meal ->
            meal.copy(dishes = meal.dishes.map { dish ->
                if (dish.resolution == ResolveKind.REUSE && dish.existingId != null)
                    dish.copy(nutrition = nutritionById[dish.existingId]) else dish
            })
        })
    })
}
```

```kotlin
// androidApp/.../ui/ai/AiMealInputViewModel.kt
enum class AiMealErrorKind { GENERIC, NOT_CONFIGURED }

// AiMealInputUiState 新增字段：
val errorKind: AiMealErrorKind = AiMealErrorKind.GENERIC,

// VM 新增私有属性 + 公开刷新函数（GC-37 挑战 #3/#4/#5 修订：不用 init{} 一次性挂起，改为显式刷新函数）：
private var configReady: Boolean = true // 乐观默认

/** 刷新"是否已配置 AI"标记。由 UI 侧在 Sheet 每次进入可交互态时调用（见 AiMealInputSheet 的 LaunchedEffect），
 *  覆盖两个场景：①Sheet 首次打开前完成一次判定；②用户从"去设置"CTA 跳转配置 Key 后返回本 Sheet，Sheet 所在的
 *  AddDayFoodScreen 会随导航离开/返回而整体离开/重新进入组合（Compose Navigation 标准行为，rememberSaveable 的
 *  aiSheetOpen 跨此过程保留），使 LaunchedEffect(Unit) 重新触发，从而拿到最新配置状态。
 *  判据刻意收窄为"CLOUD 且 Key 为空"，不用 AiRuntimeConfig.isModelReady()（GC-36 脚注修订，挑战 #5）：
 *  isModelReady()==false 也覆盖 MOCK/ON_DEVICE，这两种是用户主动选择的离线模式，不该被诊断成"未配置"。 */
suspend fun refreshConfigReady() {
    configReady = !(config.activeType() == AiRuntimeType.CLOUD && config.currentCloudApiKey().isBlank())
}

// submit() 开头短路（不变，见下方）；handleSessionSnapshot() 既有 :490-503 分支新增一行（GC-37 挑战 #1）：
// _state.update { it.copy(phase = ERROR, errorKind = AiMealErrorKind.GENERIC, errorMessage = "...", ...) }

// submit() 开头新增短路：
fun submit() {
    if (!configReady) {
        _state.update {
            it.copy(
                phase = AiMealPhase.ERROR,
                errorKind = AiMealErrorKind.NOT_CONFIGURED,
                errorMessage = "还没有配置 AI，先去设置里填一下",
                parseWarnings = emptyList(),
            )
        }
        return
    }
    val state = _state.value
    // ... 原有逻辑不变
}
```

```kotlin
// androidApp/.../ui/ai/AiMealInputSheet.kt
@Composable
fun AiMealInputSheet(
    vm: AiMealInputViewModel,
    onDismiss: () -> Unit,
    onSaved: (AiMealInputUiState) -> Unit = {},
    onNavigateToAiSettings: () -> Unit = {}, // 新增
) {
    // 新增：每次本 Sheet 进入组合（含"从 AI 设置页返回后重新进入"）都刷新一次 configReady（GC-37 挑战 #3）。
    // 放在既有 :127 的 snackbarEvent LaunchedEffect 旁边，同一模式。
    LaunchedEffect(Unit) { vm.refreshConfigReady() }
    /* ... 其余不变；ErrorPhase(vm, state, onNavigateToAiSettings) 透传 */
}
```

---

## §5 UI 设计

复用范围内，无新交互范式，Apple-UX 门禁豁免理由：本批全部是"复用已确立组件"（`DishNutritionLine` 零改直接复用）+ "纯文案/CTA 路由"（NOT_CONFIGURED 分支复用既有 `ErrorPhase` 布局骨架，只替换按钮文案与 onClick 目标），符合 CLAUDE.md 门禁豁免条件"复用已确立 §九 模式的同类小改"。

- `MealPreviewCard` 第二行由手写热量文案 → `DishNutritionLine`（三宏量色点 + 热量 + 钠提示，与首页/食历/AI推荐视觉统一）。
- `ErrorPhase`：`errorKind==NOT_CONFIGURED` 时，主按钮文案"去设置"（原"重新输入"降为次要 `TextButton`，与既有"有预览时"分支的主/次布局规则一致，不新增布局形态）。

文案"还没有配置 AI，先去设置里填一下"符合项目文案准则：说人话、鼓励非责备、无责怪语气；不含 emoji。

---

## §6 文件改动清单 + Allowlist

| 文件 | 允许操作 | 禁止操作 |
|---|---|---|
| `shared/.../domain/autogen/DishAutoGenerator.kt` | 改 CREATE 分支营养计算、REUSE/空菜名分支字段名；新增 private converter 扩展函数 | 改 `commit()` 逻辑、REUSE 分支的"不重算"决策本身 |
| `shared/.../domain/autogen/AutoGenModels.kt` | `DishPreview.estimatedKcal`→`nutrition` 字段替换 | 改 `IngredientPreview`/`MealPreview`/`DayPreview`/`AutoGenPreview` 其他字段 |
| `shared/.../ai/meallog/MultiDayRecorder.kt` | `previewAll()` 内新增批量回填后处理 | 改 `commitPreview()`/`recordAll()`/`buildDayGen()` |
| `androidApp/.../ui/ai/AiMealInputViewModel.kt` | 新增 `configReady`/`refreshConfigReady()`/`submit()` 开头短路；`AiMealInputUiState` 新增 `errorKind` 字段；**在 `handleSessionSnapshot()` 既有 `:490-503` 分支的 `.copy(...)` 里新增恰好一行 `errorKind = AiMealErrorKind.GENERIC`**（GC-37 挑战 #1，见 §4.1；除这一行外该分支其余逻辑不得动） | 改 `AiMealPhase` 枚举本身；改 `handleSessionSnapshot()` 除新增那一行外的任何逻辑；引入 `init{}` 块做配置预取（GC-37 挑战 #3/#4 已否决该方案） |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | `MealPreviewCard` 热量渲染替换；`ErrorPhase` 加 `errorKind` 分支+CTA；顶层签名加 `onNavigateToAiSettings`；新增 `LaunchedEffect(Unit) { vm.refreshConfigReady() }`（GC-37 挑战 #3） | 改 `PreviewPhase`/`GeneratingPhase` 其他渲染逻辑 |
| `androidApp/.../ui/addmeal/AddDayFoodScreen.kt` | 新增 `onOpenAiSettings` 参数并透传 | 改其他现有回调 |
| `androidApp/.../ui/nav/MainScaffold.kt` | `AddDayFoodScreen(...)` 调用点加一个具名参数 | 改其他路由 |
| 新建 `shared/.../domain/autogen/DishAutoGeneratorTest.kt` | 新建（此前无此测试文件） | — |
| 新建/追加 `androidApp/.../ui/ai/AiMealInputViewModelConfigTest.kt`（或追加进既有 Stream 测试文件） | 新增 T-CFG-01~03 | 不得删除/弱化既有 `AiMealInputViewModelStreamTest`/`GenerationProgressTest` 断言；**不得修改这两个既有测试文件的任何一行**（GC-37 挑战 #4 修订后的 `refreshConfigReady()` 设计使 `configReady` 默认 `true` 且只在显式调用时才改变，既有测试从不调用它，天然保持通过，无需再像原稿设想的那样"给测试夹具塞 Key"） |

**显式禁改文件清单**（本批touching 风险高、需要明确排除以防"顺手改进"扩大范围）：
- `shared/.../ai/meallog/StreamingMealSession.kt`、`GenerationProgress.kt`、`SegmentProgressBar.kt`（B4+B5+B6 刚验收关闭，不容许本批触碰）
- `shared/.../ai/meallog/AiMealPrompt.kt`、`UnifiedMealSchema.kt`（K1b 专用，本批不涉及 prompt/schema）
- `shared/.../ai/meallog/HealthContextBuilder.kt`（K1b 专用，保持零调用现状）
- `shared/.../domain/NutritionGuesser.kt`（§1.3 已声明的既有精度限制，本批不修复其内部逻辑）
- `androidApp/.../ui/component/DishNutritionLine.kt`（零改直接复用，禁止调整其展示阈值/文案）
- `shared/.../ai/MemberDishHealthUseCase.kt`、`IngredientCrowdCare.kt`、`HealthRuleEngine.kt`（K1b/推荐相关，本批不碰）
- `shared/.../ai/AiRuntimeConfig.kt`（`activeType()`/`currentCloudApiKey()` 均是现成公开能力，只读复用；**不使用**也**不改动** `isModelReady()`——GC-37 挑战 #5 后判据改用前两者自行组合，理由见 GC-36 脚注）

**回归基线锁定**（GC-09，本批完成后必须仍为绿）：
- `:shared:testDebugUnitTest`（全量，0 failures）
- `:androidApp:testDebugUnitTest --tests "*.AiMealInputViewModelStreamTest"`（9/9）
- `:androidApp:testDebugUnitTest --tests "*.GenerationProgressTest"`（4/4）
- `:androidApp:assembleDebug`

---

## §7 分阶段实施步骤

### 批 K1A-1：菜品营养计算统一（CREATE 分支）

**STEP-K1A-1.1**：`shared/.../domain/autogen/DishAutoGenerator.kt` — 新增 §4.4 给出的 `IngredientPreview.toNutritionInput()` 扩展函数（文件内 private，紧邻 `preview()` 之后）。
完成形态：`grep "fun IngredientPreview.toNutritionInput" DishAutoGenerator.kt` 命中 1 处。

**STEP-K1A-1.2**（**GC-37 挑战 #2/#6 修订**）：同文件 `preview()` 的 CREATE 分支（原 :85-101），删除 :86-91 手算 fold，改为：
```kotlin
val rawNutrition = NutritionCalculator.dishNutrition(ingredientPreviews.map { it.toNutritionInput() })
// 挑战 #6：unitGrams 固定非空导致 resolveGrams() 永不走 est=true 分支，NutritionCalculator 自身的 estimated
// 标记对"营养值是猜的"这件事完全不敏感——必须由本层显式补上：只要有一个食材不是高置信度 Match，就标估算。
val anyGuessed = ingredientPreviews.any { it.nutrition.source !is NutritionGuessSource.Match }
val nutrition = rawNutrition.copy(estimated = rawNutrition.estimated || anyGuessed)
// 挑战 #2：不做 .takeIf{it.hasData} 过滤——"算了但没数据"(hasData=false) 必须仍是非空 DishNutrition，
// 交给 DishNutritionLine 自己渲染"营养待完善"；过滤成 null 会让该菜整行不显示任何文字（见 INV-K1A-04）。
```
`DishPreview(..., nutrition = nutrition, ...)` 替换原 `estimatedKcal = estimatedKcal`。
完成形态：`grep "estimatedKcal = ingredientPreviews.fold" DishAutoGenerator.kt` 零命中；`grep "NutritionCalculator.dishNutrition(ingredientPreviews" DishAutoGenerator.kt` 命中 1 处；`grep "anyGuessed" DishAutoGenerator.kt` 命中 ≥1 处；`grep "\.takeIf { it\.hasData }" DishAutoGenerator.kt` 零命中（确认过滤未被引入）。

**STEP-K1A-1.3**：`AutoGenModels.kt` — `DishPreview.estimatedKcal: Double?` 改 `nutrition: DishNutrition?`，加 import `com.sxdbsm.cookbook.domain.model.DishNutrition`。`DishAutoGenerator.kt:46,59` 两处占位分支同步改 `nutrition = null`。
完成形态：`grep -r "estimatedKcal" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/` 零命中。

### 批 K1A-2：REUSE 菜品批量回填

**STEP-K1A-2.1**：`MultiDayRecorder.kt` `previewAll()` 按 §4.4 给出的完整实现替换。
完成形态：`grep "nutritionRepo.dishNutrition" MultiDayRecorder.kt` 命中 1 处，且不在任何 `forEach`/`map` 循环体内部（用 `grep -B3 "nutritionRepo.dishNutrition"` 人工核对调用位置在循环外）。

### 批 K1A-3：UI 展示切换

**STEP-K1A-3.1**：`AiMealInputSheet.kt` `MealPreviewCard`（:1108-1174），删除 :1110 `val kcal = ...` 与 :1158-1173 手写渲染块，替换为：
```kotlin
DishNutritionLine(dishPreview.nutrition?.toDishNutritionUi())
```
先 grep 确认 `calorieOn`（:1067）在函数内是否还有其他用途；若替换后再无引用，一并删除该行避免死变量。
完成形态：`grep "整份约 \\\${kcal" AiMealInputSheet.kt` 零命中；`grep "DishNutritionLine(dishPreview.nutrition" AiMealInputSheet.kt` 命中 1 处。

### 批 K1A-CFG：AI 未配置诚实报错

**STEP-K1A-CFG-1**：`AiMealInputViewModel.kt` 顶部新增 `enum class AiMealErrorKind { GENERIC, NOT_CONFIGURED }`；`AiMealInputUiState` 新增 `val errorKind: AiMealErrorKind = AiMealErrorKind.GENERIC`。
完成形态：`grep "enum class AiMealErrorKind" AiMealInputViewModel.kt` 命中 1 处。

**STEP-K1A-CFG-2**（**GC-37 挑战 #3/#4/#5 修订，取代原"init{} 一次性预取"方案**）：同文件新增 `private var configReady: Boolean = true` + §4.4 给出的公开 `suspend fun refreshConfigReady()`（判据 `activeType()==CLOUD && currentCloudApiKey().isBlank()`，**不用** `isModelReady()`）。**不新增 `init{}` 块**。
完成形态：`grep "private var configReady" AiMealInputViewModel.kt` 命中 1 处；`grep "suspend fun refreshConfigReady" AiMealInputViewModel.kt` 命中 1 处；`grep "^\s*init {" AiMealInputViewModel.kt` 零命中（确认未引入 init 块）；`grep "config.isModelReady()" AiMealInputViewModel.kt` 零命中（确认判据用的是收窄后的表达式，非 `isModelReady()`）。

**STEP-K1A-CFG-3**：`submit()` 方法体最开头插入 §4.4 给出的短路分支（在任何 segments 构造之前）；`handleSessionSnapshot()` 既有 `:490-503` 分支的 `.copy(...)` 内新增一行 `errorKind = AiMealErrorKind.GENERIC`（GC-37 挑战 #1，见 §4.1/§6）。
完成形态：`grep "还没有配置 AI，先去设置里填一下" AiMealInputViewModel.kt` 命中 1 处；`grep -A2 "fun submit()" AiMealInputViewModel.kt` 第一行非空语句是 `if (!configReady)`；`grep "errorKind = AiMealErrorKind.GENERIC" AiMealInputViewModel.kt` 命中 1 处。

**STEP-K1A-CFG-7**（**GC-37 挑战 #3 新增**）：`AiMealInputSheet.kt` 顶层 composable 内新增 `LaunchedEffect(Unit) { vm.refreshConfigReady() }`（放在既有 `:127` 附近的 `snackbarEvent` `LaunchedEffect` 旁）。
完成形态：`grep "vm.refreshConfigReady()" AiMealInputSheet.kt` 命中 1 处。

**STEP-K1A-CFG-4**：`AiMealInputSheet.kt` 顶层签名加 `onNavigateToAiSettings: () -> Unit = {}`；调用 `ErrorPhase` 处透传；`ErrorPhase` 签名加同名参数，函数体按 `state.errorKind` 分支：
```kotlin
if (state.errorKind == AiMealErrorKind.NOT_CONFIGURED) {
    Button(onClick = onNavigateToAiSettings, shape = RoundedCornerShape(12.dp)) { Text("去设置") }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = { vm.dismissError() }) { Text("重新输入") }
} else {
    // 原有 :1255-1273 分支（autoGenPreview!=null 走"重试保存"，否则走"重新输入"）不变
}
```
完成形态：`grep "onNavigateToAiSettings" AiMealInputSheet.kt` 命中 ≥3 处；`grep "\"去设置\"" AiMealInputSheet.kt` 命中 1 处。

**STEP-K1A-CFG-5**：`AddDayFoodScreen.kt` 签名加 `onOpenAiSettings: () -> Unit = {}`；:597 `AiMealInputSheet(...)` 调用加 `onNavigateToAiSettings = onOpenAiSettings`。
完成形态：`grep "onOpenAiSettings" AddDayFoodScreen.kt` 命中 ≥2 处。

**STEP-K1A-CFG-6**：`MainScaffold.kt` :405 附近 `AddDayFoodScreen(...)` 调用加 `onOpenAiSettings = { nav.navigate(Routes.AI_SETTINGS) }`。
完成形态：`grep "onOpenAiSettings = { nav.navigate(Routes.AI_SETTINGS)" MainScaffold.kt` 命中 1 处。

### 批 K1A-T：测试

**STEP-K1A-T-1**：新建 `shared/.../domain/autogen/DishAutoGeneratorTest.kt`，覆盖 T-K1A-01（见 §8.2）。

**STEP-K1A-T-2**：`MultiDayRecorder` 相关测试（新建或追加进既有 shared 集成测试）覆盖 T-K1A-02/03（REUSE 批量回填 + 零 N+1）。

**STEP-K1A-T-3**：`AiMealInputViewModel` 测试（新建 `AiMealInputViewModelConfigTest.kt`；**不追加进/不修改** `AiMealInputViewModelStreamTest.kt`，见 §6 allowlist）覆盖 T-CFG-01/02/03。

**验收命令**（当次输出需贴进交付台账，GC-06）：
```
scripts\build-cli.bat :shared:testDebugUnitTest
scripts\build-cli.bat :androidApp:testDebugUnitTest
scripts\build-cli.bat :androidApp:assembleDebug
```

---

## §8 测试矩阵

### 8.1 测试夹具职责边界（GC-07）

| 夹具 | 职责 | 禁止 |
|---|---|---|
| shared 测试用 `FakeNutritionRepository`（若新建） | 记录 `dishNutrition(ids)` 被调用的次数与参数，返回预设 `Map<Long,DishNutrition>` | 直接返回业务终态判断结果；不得内部做"调用超过1次就返回不同值"这类隐藏逻辑掩盖 N+1 |
| androidApp 测试用现有 `SpySessionPort`/fake `AiRuntime` | 记录 `stream()`/`complete()` 是否被调用（用于 T-CFG-01 断言"零调用"） | — |

### 8.2 INV↔T 双向映射表

| INV | T-ID | 断言要点 |
|---|---|---|
| INV-K1A-01 | T-K1A-01a | CREATE 菜品含 2 个有营养数据、`source=Match` 的食材 → `preview().nutrition!!.totals` 的蛋白/脂肪/碳水均 >0，且与手工用 `NutritionCalculator.dishNutrition()` 独立计算的期望值一致；`nutrition!!.estimated == false`（全 Match，无需估算标记） |
| INV-K1A-04 | T-K1A-01b | CREATE 菜品全部食材无营养数据 → `nutrition != null && nutrition.hasData == false`（**不是** `null`——挑战 #2 修订，见 INV-K1A-04） |
| INV-K1A-04 | T-K1A-01c | 菜名为空 → `nutrition == null`（早退分支，从未尝试计算，与①类一致） |
| INV-K1A-05 | T-K1A-01d | CREATE 菜品含 1 个 `source=Group`（大类均值猜测）食材 → `nutrition!!.estimated == true`（挑战 #6，验证"估算"标记未因 `unitGrams` 固定非空而失效） |
| INV-K1A-02 | T-K1A-02 | 一天预览含 1 个 REUSE 菜品（`existingId=5`）→ `previewAll()` 返回的对应 `DishPreview.nutrition` 等于 fake repo 为 id=5 返回的值 |
| INV-K1A-02 | T-K1A-03（**GC-37 挑战 #11 修订**） | 一天预览含 3 个 REUSE 菜品 → 对**单次 `MultiDayRecorder.previewAll()` 调用**，fake `NutritionRepository.dishNutrition()` 被调用**恰好 1 次**，入参包含全部 3 个 id（断言范围是 `previewAll()` 单次调用内部，不是整个 VM/generation 生命周期——`previewAll()` 本身会在一次 generation 中被多次触发，每次触发各自应只查 1 次，测试只需覆盖单次调用的行为） |
| INV-CFG-01 | T-CFG-01 | 构造 `AiRuntimeConfig` 使 `activeType()==CLOUD` 且 Key 为空 → 先 `awaitRefresh` 调用 `vm.refreshConfigReady()` 并等待完成，再调用 `submit()` → `state.phase==ERROR && state.errorKind==NOT_CONFIGURED && state.errorMessage=="还没有配置 AI，先去设置里填一下"`；且 fake `AiRuntime.stream()`/`complete()` 调用计数为 0 |
| INV-CFG-01 | T-CFG-01b（**GC-37 挑战 #5 新增**） | 构造 `AiRuntimeConfig` 使 `activeType()==MOCK` → 调用 `vm.refreshConfigReady()` 后 `submit()` → **不**短路为 `NOT_CONFIGURED`（`configReady` 保持 `true`），沿既有 GENERIC 路径正常执行（验证 MOCK 用户不受本批影响） |
| INV-CFG-02 | T-CFG-02 | 既有 `AiMealInputViewModelStreamTest` 全部 9 条、`GenerationProgressTest` 全部 4 条，**在两个文件零改动的前提下**，本批改动后依旧 9/9、4/4 绿（因为这两个文件从不调用 `refreshConfigReady()`，`configReady` 保持默认 `true`，`submit()` 短路分支永不触发，等价于本批改动前的行为） |
| INV-CFG-04 | T-CFG-03（**GC-37 挑战 #1 新增**） | 先触发 `submit()` 短路进入 `NOT_CONFIGURED`（同 T-CFG-01 前置）；随后调用 `vm.refreshConfigReady()`（此时 Key 已配置，模拟"从设置页回来"）+ 走一次真实生成流程直到 `handleSessionSnapshot` 命中 `:490-503` 分支（无合法餐食）→ 断言 `state.errorKind == AiMealErrorKind.GENERIC`（不是残留的 `NOT_CONFIGURED`） |
| INV-CFG-03 | 真机 E-K1A-CFG-01 | `ErrorPhase` 在 NOT_CONFIGURED 态显"去设置"按钮，点击后跳转 AI 设置页（无 Compose UI 测试基础设施，按既有项目惯例走真机验证，非单元测试） |
| INV-CFG-05 | 真机 E-K1A-CFG-02（**GC-37 挑战 #3 新增**） | 未配置态点发送 → 看到"还没有配置 AI"提示 → 点"去设置" → 填入真实 Key 保存 → 返回 → 再次点发送 → **必须**正常进入生成态（不再提示未配置），验证"配置后立即可用"这条最基本的可用性闭环 |
| INV-K1A-03 | 真机 E-K1A-01 | 预览页每道菜（含 CREATE 与 REUSE 两类）都显示营养行（宏量色点+钠提示或"营养待完善"），视觉与首页/食历一致；含 Group 估算食材的菜显"（估算）"尾注 |

---

## §9 交付台账模板（CODE 完成时填）

### STEP 勾销表

| STEP-ID | 状态 | 落地 commit | diff 定位 |
|---|---|---|---|
| STEP-K1A-1.1~1.3 | ⬜ | — | — |
| STEP-K1A-2.1 | ⬜ | — | — |
| STEP-K1A-3.1 | ⬜ | — | — |
| STEP-K1A-CFG-1~7 | ⬜ | — | — |
| STEP-K1A-T-1~3 | ⬜ | — | — |

### 真机待验证登记

交付时须在时间戳最新的 `真机待验证清单_<yyyyMMddHHmm>.md` 新增：
- `E-K1A-01`：AI 快捷记预览页营养展示（CREATE + REUSE 两类菜 + 估算尾注）
- `E-K1A-CFG-01`：AI 未配置时发送 → 诚实提示 + "去设置" CTA 跳转验证
- `E-K1A-CFG-02`：配置 Key 后返回可正常发送闭环验证（GC-37 挑战 #3）

---

## §10 独立挑战台账（GC-37）

**挑战方**：独立 opus 挑战 agent（Explore 只读模式，未见本蓝图起草过程，仅读蓝图成文 + 逐条核对当前真实源码）。**挑战范围**：全部技术性声明（代码现状描述、字段值域、竞态、blast radius、scope 边界）。

| # | 挑战项 | 裁决 | 处置 |
|---|---|---|---|
| 1 | `errorKind` 是 `.copy()` 沿用旧值的 sticky 字段，原稿"GENERIC 分支可不显式写"会导致"未配置→配置好→真解析失败"时 CTA 仍显示"去设置" | **CONFIRMED-ISSUE** | 采纳：`handleSessionSnapshot()` `:490-503` 新增显式 `errorKind = GENERIC`（INV-CFG-04，§4.1/§6/§7 STEP-CFG-3） |
| 2 | `DishNutritionLine(null)` 静默不渲染；`.takeIf{it.hasData}` 会把"算了但没数据"误判成"没算"，该菜整行消失而非显"营养待完善" | **CONFIRMED-ISSUE** | 采纳：STEP-K1A-1.2 去掉 `.takeIf` 过滤，INV-K1A-04 改写为区分"从未尝试"vs"尝试但无数据"两类 |
| 3 | `configReady` 只在 `init{}` 写一次；用户去设置页配置 Key 返回同一 Sheet 实例后，`configReady` 不会刷新，"去设置"CTA 变死路 | **CONFIRMED-ISSUE** | 采纳：改用 Compose `LaunchedEffect(Unit)` 驱动的 `refreshConfigReady()`（依赖 Nav 离开/返回时宿主 composable 整体离开/重进组合的标准行为），新增 STEP-CFG-7 + INV-CFG-05 + 真机 E-K1A-CFG-02 |
| 4 | `init{}` 挂起写入与既有测试直接构造 VM 后立即 `submit()` 存在真实竞态，13 条既有测试（9+4）在 IO 线程调度不利时会全部失败 | **CONFIRMED-ISSUE** | 采纳：整体改用方案 3 的 `refreshConfigReady()` 设计后自动解决——既有测试从不调用该函数，`configReady` 保持默认 `true`，零测试改动、零竞态 |
| 5 | `isModelReady()==false` 同时覆盖 MOCK（用户主动选择的离线规则模式，`AiSettingsScreen` 上是可选且已启用的选项）和 ON_DEVICE，把这些也误判成"未配置"，会把选了离线模式的用户导向一个"一切都已配置"的设置页、无路可走 | **CONFIRMED-ISSUE** | 采纳：判据收窄为 `activeType()==CLOUD && currentCloudApiKey().isBlank()`；GC-36 脚注、INV-CFG-01 同步修订；新增 T-CFG-01b 锁定 MOCK 不受影响 |
| 6 | `NutritionInput(unitGrams=1.0)` 恒非空，导致 `NutritionCalculator.resolveGrams()` 内部产生 `estimated=true` 的分支永远走不到；含 `Group` 均值猜测食材的 CREATE 菜品会丢失"（估算）"尾注，看起来像权威数值 | **CONFIRMED-ISSUE** | 采纳：STEP-K1A-1.2 新增 `anyGuessed` 判定并强制 `.copy(estimated=true)`；新增 INV-K1A-05 + T-K1A-01d |
| 7 | N+1 批量设计（`nutritionRepo`/`dishNutrition()` 签名/`previewAll()` 结构） | **CONFIRMED-FINE** | 无需改动 |
| 8 | "quantity 恒为克数"的结论正确，但 §4.4 原注释"本层口径下恒为克数"的表述容易让人误以为 AI 给的 unit 真的被转换过；实际 `SemanticIngredient.unit` 从未被读取 | **MINOR-NIT** | 采纳：§4.4 注释改为准确表述"preview/commit 口径一致，不代表做过真实单位换算"，并关联 §12 D5 |
| 9 | §4.2 原稿"仅 2 处生产代码"与自己表格里的 3 行矛盾 | **MINOR-NIT** | 采纳：订正为"共 3 处" |
| 10 | `AiMealRecorder.kt`（单日遗留 recorder）与 `DishPreview` 无关，REUSE-ingredients 的两个消费点不受影响 | **CONFIRMED-FINE** | 无需改动 |
| 11 | T-K1A-03"调用恰好 1 次"若按 VM/generation 生命周期理解会断言失败（`previewAll()` 每个 generation 内被多次调用） | **MINOR-NIT** | 采纳：§8.2 明确断言范围是单次 `previewAll()` 调用内部 |
| 12 | K1c 头部声明"三个函数均在生产路径"，实际只有 `weekdayToIso` 在流式主路径，另外两个函数只有孤儿路径 `useRuleFallback()` 可达 | **MINOR-NIT** | 采纳：头部依赖声明改为精确表述 |
| 13 | K1b（L1 闸门）、K1c（已实现）排除范围的判断本身正确 | **CONFIRMED-FINE** | 无需改动 |
| 14 | D4 因果链（`CloudAiRuntime`→`Failed`→`parseWarnings`）与全部引用行号核实无误；额外指出 `SwitchableAiRuntime` 未 override `stream()`，AI 记一餐链路实际未走真实 SSE——与本蓝图无关，仅供参考 | **CONFIRMED-FINE** | 无需改动；该观察记入 §12 供未来 fast-follow 参考 |

**结论**：14 项挑战全部处置完毕（6 CONFIRMED-ISSUE 已修订、4 MINOR-NIT 已采纳、4 CONFIRMED-FINE 无需改动），蓝图状态可从 `BLUEPRINT_READY` 转 `ACCEPTED`（见文件头）。

---

## §11 门禁与角色

- 本批 UI 改动全部为"复用已确立组件"或"纯文案/CTA"，豁免 Apple-UX 设计 agent 前置门禁（见 §5 说明）。
- CODE 完成、构建+单测通过后，仍须走 `google_quality_engineer` 代码质量终审（项目强制门禁，无豁免条件）。
- 文案"还没有配置 AI，先去设置里填一下"字数少、无新术语，豁免 `copywriter` 专项审校，但仍需符合文案准则（已在 §5 自查）。

## §12 弃置项登记（GC-03 前瞻）

| 项 | 状态 | 归宿 |
|---|---|---|
| 食材级 REUSE 精度（D5，用 guess 代替真实值） | 显式弃置 | 独立 fast-follow 蓝图（需新增批量食材营养查询，本身有 N+1/接口设计工作量，不塞进本批） |
| 每餐/每天营养合计 UI | 显式弃置 | 独立 fast-follow（需新 UI 组件，非纯复用） |
| 规则兜底解析按钮接线（`useRuleFallback()` 孤儿） | 显式弃置 | 独立待办（与本 bug 相邻但性质不同：一个是"报错文案诚实性"，一个是"失败后有无本地兜底路径"） |
| `SwitchableAiRuntime` 未 override `stream()`，AI 记一餐链路实际未走 `CloudAiRuntime` 真实 SSE 流式（走的是 `AiRuntime.kt` 默认 `flow{complete(...)}` 包装）（GC-37 挑战 #14 发现） | 显式记录，不修复 | 独立待办（与 K1a/AI 未配置报错均无关；影响的是"是否真流式"这一更基础的架构问题，需单独评估是否值得改） |
