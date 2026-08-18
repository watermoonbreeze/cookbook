# 批B：AI记一餐 NDJSON 协议扩展——菜系/标签/做法透传 实施蓝图

> 状态：`BLUEPRINT_READY`
> **本蓝图仅由 ARCH 起草，不含任何代码实现**——按用户指示"你只负责蓝图，不要编码"。
> **颗粒度：L7**（项目基线）。
> 起草日期：2026-08-18（承接 B7 后续批 `cc806cb3` 的 3 项延后决策之二，Opus 架构设计会话核实现状后由 ARCH 固化为本蓝图）。
> **依赖关系**：与批A（`AI记一餐_死代码清理与菜系兜底_实施蓝图.md`）互不依赖，可任意顺序实施，**但在 `DishAutoGenerator.commit()` 与 `AutoGenModels.kt` 两处有共享改动点**，本蓝图 §4.1/§7 已用"先 grep 判定是否已存在"的幂等写法处理两种落地顺序，CODE 必须先执行判定 grep 再动手，不得假设顺序。批C（`AI记一餐_确认页展开UI_实施蓝图.md`）**依赖本批交付**（批C 的 UI 要展示的 tags/steps/cuisine 数据来源即本批产出）。
> **决策来源**：cuisine 真透传（收）、tags（收）、description（不收）、meal_slots（不收）、steps 用内联 `dish` 事件而非独立 `cooking_step` 事件——均为用户已拍板决策，本蓝图直接落地，不重新论证取舍（取舍论证见起草会话记录，如需追溯可查 `08_决策记录.md` 后续新增条目）。

---

### §0.1 颗粒度勾销表（GRANULARITY = L7）

| GC | 条款一句话 | 本蓝图落点 | 状态 |
|---|---|---|:--:|
| GC-01 | 每个行为分支写成"条件→唯一动作→禁止动作" | §3 | 满足 |
| GC-02 | allowlist | §6 | 满足 |
| GC-03 | 上一批延后项归宿表 | §1.4 | 满足 |
| GC-04 | INV 五列齐全 | §3 | 满足 |
| GC-05 | INV↔T 双向映射表 | §8.2 | 满足 |
| GC-06 | 命令原文 | §7 末尾 | 满足 |
| GC-07 | 测试夹具职责边界表 | §8.1 | 满足 |
| GC-08 | 真机清单登记 | §9 | 满足——**本批的核心不确定性不在代码正确性，在"AI 实际填充率"，真机验证是本批交付后的强制后续步骤，非可选** |
| GC-09 | 回归基线锁定 | §6 末尾 | 满足 |
| GC-10 | 逐字段真相源表 | §4.1 | 满足 |
| GC-11 | 字段/行为迁移清单 | §4.2 | 满足 |
| GC-12 | UI 判据与业务判据同源 | — | N/A：本批不改任何 UI/Compose 代码，纯 shared 层协议扩展 |
| GC-13 | fallback 复用主路径校验入口 | §4.6 | 满足：`cuisine` 归一逻辑（`CuisineNormalizer`）是纯函数单一入口，`handleDishEvent` 唯一调用点，不存在第二套归一逻辑 |
| GC-14~16 | 对象生命周期/可变持有物/搬迁注释 | — | N/A：`CuisineNormalizer` 是无状态纯函数 object，不持有可变状态 |
| GC-17~19 | 逐项状态 List&lt;Status&gt; | — | N/A：本批新增字段均为标量/字符串列表，非状态枚举 |
| GC-20 | 自动副作用清单 | §3 | 满足：新增字段透传到落库是本批唯一的用户可感副作用，已建模为 INV-B-05/06 |
| GC-21 | 提示/告知配 STEP 落点 | — | N/A：本批不新增用户提示（数据静默透传落库，不触发任何弹窗/Snackbar；是否需要在确认页展示新字段是批C的职责，与"透明准则"相关的告知设计留给批C+UX门禁） |
| GC-22 | 可见副作用配 T-ID | — | N/A：同上，本批交付后用户界面无可见变化（批C才有 UI），故本批本身无"用户可见副作用"需要配 T-ID；数据落库的正确性已在 §8 覆盖 |
| GC-23 | STEP 独立编号+完成形态字面量 | §7 | 满足 |
| GC-24 | STEP 勾销表 | §9 | 满足 |
| GC-25 | 完成形态字面量+grep判据 | §7 | 满足 |
| GC-26 | 冻结值修订记录 | — | N/A：不改任何既有阈值常量（新增 `Cuisines.ALL` 白名单不是"阈值"，是既有枚举数据，本批只读引用不修改） |
| GC-27 | 编辑即失效收口函数核对 | — | N/A：与手工编辑态无关，本批只影响 AI 记一餐入库链路 |
| GC-28 | 构造时创建对象按基数分片 | — | N/A：`CuisineNormalizer` 是纯函数调用，无跨调用累积状态；`StreamingMealParser` 已有的四个内部 Map（`mealIdAlias` 等）本批不新增，只是多写了几个字段进已有的 `DishDraftNode` |
| GC-29 | 多来源写入同一聚合目标 | §4.5 | 满足：dish 事件重复到达时 cuisine/tags/steps 三个新字段的合并规则已显式定义（"最新非空替换"，与既有 `cookingMethod`/`quantity` 字段同一模式），见 §4.5 |
| GC-30 | 状态转移驱动完整副作用链 | — | N/A：本批不改变 `StreamingMealParser` 的段/餐/菜状态机，只是给已有 `DishDraftNode` 多挂三个字段 |
| GC-31 | 挂起点+身份重校验 | — | N/A：`CuisineNormalizer.normalize()` 是同步纯函数，无挂起点 |
| GC-32 | 高频异步事件节流 | — | N/A：本批不新增事件类型（复用既有 `dish` 事件，只是字段变多），不改变事件到达频率 |
| GC-33 | 禁止测试专用可变全局注入点 | — | N/A：不新增测试基础设施依赖 |
| GC-34 | 注释/KDoc 一致性 | §7 | 满足 |
| GC-35 | 协议事件枚举逐项对照 | — | N/A：本批不新增/删除事件类型（`type` 字段的合法值集合不变），只在既有 `dish` 事件里加三个可选字段 |
| GC-36 | List&lt;Status&gt; 值域覆盖 | — | N/A：`tags: List<String>` 是自由文本列表非状态枚举 |
| GC-37 | 独立挑战台账 | §10 | 满足——2026-08-19 已跑一轮独立挑战，7 项 CONFIRMED-ISSUE（B-I-1~7）全部就地处置，见 §10 |

---

## §1 目标与范围

### 1.1 一句话价值

把 AI 记一餐 NDJSON 协议的 `dish` 事件扩展出 `cuisine`（菜系）、`tags`（标签，最多2个）、`steps`（做法步骤，最多3条）三个可选字段，并把这三个字段全链路打通到落库——现状是 `DishPreview` 已经有这三个字段的容器（B7 修复打通），但协议层从未产出过内容，AI 建的菜的菜系/标签/做法永远是空。本批交付后，AI 记一餐建的菜会开始携带这些信息，为批C（确认页展开 UI）提供真实可展示的数据，同时让 AI 建的菜正式进入菜系筛选维度（配合批A的落库兜底，双层收口）。

### 1.2 触发来源与关键依赖

- 触发：B7 后续批（`cc806cb3`）待办"S2 决策项：cuisine/meal_slots 是否透传"+用户追加的"确认页展开做法/标签"诉求，经 Opus 架构设计会话（用户拍板：cuisine收/tags收/description不收/meal_slots不收/steps内联 dish 事件）固化为本蓝图。
- 关键依赖：与批A共享两处改动点（`DishAutoGenerator.commit()` 的 `cuisine` 传参行、`AutoGenModels.kt` 的 `SemanticDish`/`DishPreview.cuisine` 字段），已用幂等 grep 判定处理跨批次顺序，见 §4.1。
- 批C（确认页 UI）依赖本批交付 + 真机验证 AI 实际填充率数据，不得在本批交付前提前设计 UI 细节（已在批C蓝图中显式声明为 `PENDING_UX_DESIGN`）。

### 1.3 In Scope / Out of Scope

**In Scope**：
1. `NdjsonLine`/`DishDraftNode` 新增 `cuisine`/`tags`/`steps`（steps 复用既有 `cookingSteps`/`DraftCookingStep`，只是新增"从 dish 事件内联字段填充"这一条写入路径）三个可选字段。
2. 新增 `CuisineNormalizer`（纯函数白名单归一）。
3. `AiMealPrompt.NDJSON_SYSTEM_PROMPT` 的 `dish` 事件定义行 + 规则区改写，加入 cuisine/tags/steps 的填写指引。
4. `StreamingMealParser.handleDishEvent` 读取新字段、归一、写入草稿，含 dish 事件重复到达（AF-05 同键合并）时的三个新字段合并规则。
5. `MealStreamDraftMapper.toDishRef()` 透传 `cuisine`/`tags` 进 `DishJson`（`steps` 已经透传，见 §2 D-已通清单）。
6. `MultiDayRecorder.toSemanticDay()` 补 `cuisine` 透传（`tags`/`steps` 已透传）。
7. `AutoGenModels.kt` 的 `SemanticDish`/`DishPreview` 补 `cuisine` 字段（`tags`/`steps` 已存在），**若批A已先落地则此步跳过**（见 §4.1 判定）。
8. `DishAutoGenerator.preview()` CREATE 分支补 `cuisine = input.cuisine` 透传。
9. `DishAutoGenerator.commit()` 的 `saveDish(cuisine=...)` 传参——**若批A已先落地则此步跳过**（见 §4.1 判定），若批A未落地则本批需补上（否则本批交付后 cuisine 字段全链路打通到 `DishPreview` 却在最后一步 `commit()` 断链，用户可感效果为零）。

**Out of Scope**：
- `description`（不收，用户已拍板，不新增协议字段）。
- `meal_slots`（不收，用户已拍板，理由见起草会话：AI 无额外信息、会把偶然进食事实固化为菜品长期属性、用户不可见不可控、ROI 最差，四条理由详见批B起草记录）。
- 独立 `cooking_step` NDJSON 事件类型的 prompt 化——`StreamingMealParser.handleCookingStepEvent` 与 `NdjsonLine` 的 `text`/`order` 字段**保留不删**（前向兼容，AI 若自作主张输出仍可正确处理），但 prompt **不主动教** AI 使用这个独立事件（token 开销分析见 §2 D5），做法步骤走内联 `dish.steps` 数组。
- 确认页 UI 展示——批C范围。

### 1.4 上一批延后项归宿核对（GC-03）

见 §1.2；本批是 B7 后续批"S2 决策项"的正式落地，另有"确认页展开 UI 需要的数据来源"这条隐含依赖已被批C蓝图显式接住。

---

## §2 现状与差距

| # | 现状 | 证据（file:line） | 差距/影响 |
|---|---|---|---|
| D1 | `NdjsonLine`（`NdjsonEvents.kt:23-50`）的 dish 事件字段区（`dish_id`/`dish_name`/`name`/`cooking_method`/`quantity`/`unit`/`eaten_ratio`）**没有** `cuisine`/`tags`/`steps` | `NdjsonEvents.kt:32-39` | AI 即使想给出菜系/标签/做法，协议层也接不住——这不是"AI 不填"的问题，是"协议没这个位置" |
| D2 | `DishDraftNode`（`NdjsonEvents.kt:172-184`）已有 `cookingSteps: List<DraftCookingStep>` 字段+对应 `cooking_step` 独立事件类型的解析支持（`StreamingMealParser.handleCookingStepEvent`），但**没有** `cuisine`/`tags` 字段 | `NdjsonEvents.kt:172-184` | steps 的容器和事件都已就绪，只差 prompt 让 AI 用；cuisine/tags 连容器都没有 |
| D3（已通清单·steps/tags 的下游） | `MealStreamDraftMapper.toDishRef()`（`MealStreamDraftMapper.kt:82-96`）已经把 `dish.cookingSteps` 映射进 `DishJson.steps`；`DishJson`（`UnifiedMealSchema.kt:76-90`）本身已有 `tags`/`cuisine`/`steps` 三个字段；`MultiDayRecorder.toSemanticDay()`（`MultiDayRecorder.kt:70-103`）已经把 `dishJson?.tags`/`dishJson?.steps` 透传进 `SemanticDish`；`AutoGenModels.kt` 的 `SemanticDish`（32-42行）/`DishPreview`（89-105行）已有 `tags`/`steps` 字段；`DishAutoGenerator.preview()`（`DishAutoGenerator.kt:101-116`）CREATE 分支已把 `input.tags`/`input.steps` 透传进 `DishPreview`；`commit()`（函数体144-188行）已把 `preview.tags`/`preview.steps` 传进 `saveDish()` | 见上各行 | **tags/steps 下游全通，只缺协议层入口（D1/D2）+ prompt（D4）**；cuisine 下游**只到 `DishJson`/`MultiDayRecorder` 这一步缺失**（`toSemanticDay()` 没有 `cuisine = ...` 这一行）+ `SemanticDish`/`DishPreview` 无该字段 + `commit()` 未传参（批A可能已修，见 §4.1） |
| D4 | `AiMealPrompt.NDJSON_SYSTEM_PROMPT`（`AiMealPrompt.kt:31-57`）的 `dish` 事件定义行（第37行）与规则区（44-56行）不含 cuisine/tags/steps 相关字段或指引；`FLAT_SYSTEM_PROMPT`（61-88行）**已经**在要求 `dish_cuisine`（默认"家常菜"）/`dish_cooking_methods`/`dish_note`/`dish_tags`（69-74行）。**2026-08-19 独立挑战 B-I-3 修订**：`FLAT_SYSTEM_PROMPT` 的唯一发送入口是 `AiMealPrompt.buildRequest()`（`:137`）与 `SYSTEM_PROMPT`（`:125`），二者全仓**零生产调用方**（生产只用 `MAX_INPUT_CHARS` 与 `buildStreamingRequest`）——即该 prompt 在当前构建里**从不会被发给任何模型**；`StreamingMealParser.tryWholeJsonFallback()`（`:549`）只是对**已收到的原始文本**再按整体 JSON 解析一次，不发请求、不使用该 prompt。故本批**不改** `FLAT_SYSTEM_PROMPT`，其死代码归属另记 §12 | `AiMealPrompt.kt:31-57`（主路径，本批改动对象）vs `61-88`（已零调用，不改） | AI 缺乏输出指引，即使协议字段就绪也不会主动填 |
| D5 | 独立 `cooking_step` 事件 vs 内联 `dish.steps` 数组的 token 成本对比：一条 `cooking_step` 事件行的 `segment_id`/`meal_id`/`dish_id` 三个键值 ≈ 50 token 是纯重复开销，真正内容（如"猪肉切片焯水"）只有约 8 token，3 步 = 150 token 开销 + 24 token 内容，**开销占比约 86%**；`AiMealPrompt.kt:110` 注释记录过历史教训"B6-fix: 2048→4096，单日多餐(3餐×~10菜)NDJSON输出易超2048致finish_reason=length截断"，一天 3 餐 9 道菜若都用独立 `cooking_step` 事件约再加 1560 token 纯开销 | `AiMealPrompt.kt:110` 注释 | 决定了 §1.3 的取舍：steps 走内联数组，不用独立事件 |
| D6 | 白名单来源：`Cuisines.ALL`（`shared/.../domain/model/Cuisine.kt`）= `[家常菜,川菜,粤菜,鲁菜,苏菜,闽菜,浙菜,湘菜,徽菜,西餐,其他]`（11 项，含"其他"这一兜底项） | `Cuisine.kt` | `CuisineNormalizer` 的容错白名单用完整 `Cuisines.ALL`（含"其他"，防 AI 万一真输出"其他"也不误杀）；但 **prompt 里教给 AI 的可选值只列 10 个具体菜系（不含"其他"）**——"其他"是留给用户手工编辑时选的兜底态，不应引导 AI 主动选择这个模糊值，AI 判断不出具体菜系时 prompt 指引其直接省略该字段（走 §3 INV-B-01 的空信号路径），而不是教它说"其他" |
| D7 | `NdjsonLine` 全仓无任何处以位置参数（非具名参数）方式构造，本蓝图新增字段的插入位置对既有调用点零风险（`grep -rn "NdjsonLine(" shared/src` 排除定义处零命中，唯一构造点是 `StreamingMealParser` 内 `json.decodeFromString<NdjsonLine>(line)` 走 kotlinx.serialization 具名反序列化） | 本蓝图起草时实测 | 新增字段可以放在任意位置，不必强制"排末尾"（该红线只对位置参数构造风险场景生效，`NdjsonLine` 不适用）；但为可读性仍按"dish 事件字段"分组插入，见 §4.4 |

---

## §3 不变量

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-B-01 | `dish` 事件的 `cuisine` 字段缺失（`null`）或空白字符串 | `DishDraftNode.cuisine` 为 `null`（"从未收到信号"，不等价于"AI 说了家常菜"） | 把缺失值当场兜底成 `Cuisines.HOME`（那是批A `commit()` 层的职责，本层必须诚实留空，不提前伪造信号——呼应 B7 后续批 `DishJson.cuisine` 默认值改 `""` 时立下的同一条原则） | T-B-01 |
| INV-B-02 | `dish` 事件的 `cuisine` 字段有非空值 | 若该值（trim 后）命中 `Cuisines.ALL` 白名单，`DishDraftNode.cuisine` 写入该值；未命中则视同 INV-B-01（`null`），并记 `DiagnosticLevel.INFO` 诊断（非阻断，仅留痕，不影响写库） | 未命中白名单的自由文本原样写入 `DishDraftNode.cuisine`（防脏值污染 DB 菜系筛选维度） | T-B-02 |
| INV-B-03 | `dish` 事件的 `tags` 字段存在（非 `null`，可为空列表） | trim 每项 + 过滤空白项 + 去重后写入 `DishDraftNode.tags`（清洗逻辑与 `DishRepository.saveDish` 的 tagNames 清洗同口径，见 B7 后续批 S1） | 未清洗的原始字符串（含前后空格/空白项/重复项）直接写入 | T-B-03 |
| INV-B-04 | `dish` 事件的 `steps` 字段存在（非 `null`） | 按数组顺序生成 `DraftCookingStep(text=trim后文本, order=下标)` 列表写入 `DishDraftNode.cookingSteps`（复用既有字段容器） | 忽略该字段。**注意（2026-08-19 独立挑战 B-I-1 修订，纠正原文对既有代码的误述）**：本项目 `handleCookingStepEvent`（`StreamingMealParser.kt:476-484`）对独立 `cooking_step` 事件是**按 text 精确去重后追加**（非覆盖），且该处理器要求 `dishLastLocalByRawId` 已存在（`:465`），意味着 `cooking_step` 只可能在其 dish 事件之后到达——因此当 AI 同时使用内联 `dish.steps` 与独立 `cooking_step` 时，真实行为是"内联步骤先写入、独立事件文本不同则继续追加"（可能出现 3+2=5 步），本批**不改变**这一既有行为，也不新增跨来源合并代码。反向（重复 dish 事件携带 steps 到达于 cooking_step 之后）则会由 `steppedFromLine ?: existingDish.cookingSteps` 覆盖掉已追加的步骤。两者均属"prompt 不教、生产不预期触发"的未定义输入，见 §4.5 裁决 | T-B-04、T-B-07 |
| INV-B-05（GC-29：重复到达合并） | 同一 `(segment_id, mealId, dishId)` 的 `dish` 事件到达第 2 次及以上，第 2 次事件的 `cuisine`/`tags`/`steps` 任一字段为 `null`、空字符串或空数组（**2026-08-19 独立挑战 B-I-4 修订**：即"这次没有提供有效信号"，不止 `null` 一种） | 保留第 1 次已写入的对应字段值不变（"最新非空替换"，与既有 `cookingMethod`/`quantity`/`unit`/`eatenRatio` 字段合并模式完全同构，见 `StreamingMealParser.kt:325-332` 既有代码） | 第 2 次事件的空信号（`null`/`""`/`[]`）把第 1 次已写入的值覆盖清空（这是本批唯一需要新增代码防范的真实回归风险，`copy()` 误用 `line.cuisine` 而非 `line.cuisine ?: existingDish.cuisine`，或漏加 `takeIf{isNotEmpty()}` 导致空数组被当非空值直接覆盖，就会踩中） | T-B-05 |
| INV-B-06 | 完整链路：AI 输出的合法 `dish.cuisine` 一路透传到落库 | `DishRepository` 里该菜的 `cuisine` 列值等于 AI 给出的（经白名单归一后的）值，**不是** `Cuisines.HOME` 兜底值（除非 AI 就是给了"家常菜"这个值本身） | 全链路某一环节把真实透传值错误地替换/清空成兜底值或空字符串 | T-B-06a、T-B-06b |
| INV-B-07（2026-08-19 独立挑战 B-I-5 新增） | `dish` 事件的 `tags`/`steps` 字段类型与协议不符（如 `"tags":"下饭菜"` 标量、`"steps":[{...}]` 对象数组） | 沿用既有行为：整行 `decodeFromString` 失败 → 记 `DiagnosticCode.PARSE_ERROR` 并丢弃该行（`StreamingMealParser.kt:116-124`）。本批**不新增容错**，但须由 T-B-08 把该行为钉成回归基线，并在 §9 真机验证中统计其发生率 | 悄悄把它当成"AI 没填"处理而不留诊断；或为此把 `tags`/`steps` 改成 `JsonElement?` 做宽松提取（未经真机数据证明必要前属过度设计） | T-B-08 |

---

## §4 接口契约

### 4.1 GC-10：逐字段真相源表 + 跨批次共享改动点判定

| 字段/行为 | 唯一写入者 | 读取方 | 终局形态 |
|---|---|---|---|
| `NdjsonLine.cuisine`/`tags`/`steps` | kotlinx.serialization 反序列化（AI 原文） | `StreamingMealParser.handleDishEvent` | 新增字段 |
| `DishDraftNode.cuisine`/`tags` | `handleDishEvent`（唯一写入点） | `MealStreamDraftMapper.toDishRef` | 新增字段（`cookingSteps` 复用既有） |
| `DishJson.cuisine`/`tags`/`steps` | `MealStreamDraftMapper.toDishRef` | `MultiDayRecorder.toSemanticDay` | `cuisine` 新增透传，`tags`/`steps` 字段已存在（本批只是终于有真实值流入） |
| `SemanticDish.cuisine` | `MultiDayRecorder.toSemanticDay` | `DishAutoGenerator.preview()` | 新增字段（需判定，见下） |
| `DishPreview.cuisine` | `DishAutoGenerator.preview()` CREATE 分支 | `DishAutoGenerator.commit()` | 新增字段（需判定，见下） |
| `saveDish()` 的 `cuisine` 实参 | `DishAutoGenerator.commit()` | `DishRepository` | 需判定，见下 |

**跨批次共享改动点判定（CODE 执行本批前必做，不可跳过）**：

```
grep -n "val cuisine" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/AutoGenModels.kt
grep -n "cuisine = preview.cuisine" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/DishAutoGenerator.kt
```

- 第一条命中 **2 处**（`SemanticDish`/`DishPreview` 各一）→ 批A已落地，本批 §7 STEP-B-1.7（`AutoGenModels.kt` 加字段）**跳过**，直接使用已有字段。
- 第一条**零命中** → 批A未落地，本批 §7 STEP-B-1.7 **必须执行**：`SemanticDish` 末尾（`source` 字段之后）加 `val cuisine: String = ""`，`DishPreview` 末尾（`steps` 字段之后）加 `val cuisine: String = ""`——**字段名/类型/默认值/位置必须与批A蓝图 §4.1 描述完全一致**，这样无论哪个批次先落地，另一个批次的判定 grep 都能命中同一份字段定义，不会出现两次不同定义冲突。
- 第一条命中 **1 处**（半落地，2026-08-19 独立挑战 B-I-7 新增分支）→ **STOP**，不得凭猜测补齐缺失的那一个字段，转 Q 缺口单人工确认后再继续（半落地通常意味着上一批次执行中断，需要人工判断真实状态）。
- 第二条命中 → 批A已落地 `commit()` 的 cuisine 传参行，本批 §7 STEP-B-1.9 **跳过**。
- 第二条零命中 → 批A未落地，本批 §7 STEP-B-1.9 **必须执行**：`commit()` 补 `cuisine = preview.cuisine.ifBlank { Cuisines.HOME }`（与批A蓝图 §4.1/§4.2 描述的代码完全一致）。
- **STEP-B-1.10（`preview()` 的 `cuisine = input.cuisine`）由本批独占，无条件执行，但执行前必须先 `grep -n "cuisine = input.cuisine" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/DishAutoGenerator.kt`——零命中方可执行；已命中说明批A违反了其蓝图 allowlist（批A明确"不改 `preview()`"），STOP 并转 Q 缺口单，不得直接覆盖或跳过。**

此判定结果必须记入 §9 交付台账。

### 4.2 GC-11：字段/行为迁移清单

| 迁移对象 | 旧形态 | 新形态 |
|---|---|---|
| `AiMealPrompt.NDJSON_SYSTEM_PROMPT` 的 `dish` 事件定义行 | 无 cuisine/tags/steps | 见 §4.3 prompt 文本 |
| `StreamingMealParser.handleDishEvent` 的 create/merge 分支 | 只处理 name/cookingMethod/quantity/unit/eatenRatio/note | 新增 cuisine（归一后）/tags（清洗后）/cookingSteps（来自内联 steps） |
| `MealStreamDraftMapper.toDishRef` 的 `DishJson(...)` 构造 | 不传 `cuisine`/`tags` | 新增两个具名参数 |
| `MultiDayRecorder.toSemanticDay` 的 `SemanticDish(...)` 构造 | 不传 `cuisine` | 新增 `cuisine = dishJson?.cuisine.orEmpty()` |
| `DishAutoGenerator.preview()` CREATE 分支的 `DishPreview(...)` 构造 | 不传 `cuisine` | 新增 `cuisine = input.cuisine` |

**终局裁决**：全部为**新增**字段/参数，无替换既有字段语义。

### 4.3 新增 Prompt 文本（`AiMealPrompt.kt` 改动）

`NDJSON_SYSTEM_PROMPT`（`AiMealPrompt.kt:31-57`）改动两处：

**第 37 行（dish 事件定义行）**，原：
```
- {"type":"dish","segment_id":"<段ID>","meal_id":"<日期>|<餐次>","dish_id":"<meal_id>|d<序号>","name":"菜名","cooking_method":"做法","quantity":1,"unit":"份","eaten_ratio":null,"note":""}
```
改为：
```
- {"type":"dish","segment_id":"<段ID>","meal_id":"<日期>|<餐次>","dish_id":"<meal_id>|d<序号>","name":"菜名","cooking_method":"做法","cuisine":"菜系","tags":["标签"],"steps":["步骤1","步骤2"],"quantity":1,"unit":"份","eaten_ratio":null,"note":""}
```

**规则区（44-56行）新增三行**（插入位置：紧接第53行"不确定的字段省略不填，不编造；菜名是唯一必填"之后）：
```
- cuisine 可选，只能取以下之一：家常菜/川菜/粤菜/鲁菜/苏菜/闽菜/浙菜/湘菜/徽菜/西餐；菜名有公认菜系归属就填(如回锅肉→川菜、白切鸡→粤菜)，判断不了或就是普通家常做法一律省略该字段(不要自己编菜系词，也不要什么都填"家常菜")
- tags 可选，最多2个，只填菜品的稳定属性词(如 下饭菜/快手菜/清淡/汤羹/凉菜)，不填与本次进食有关的词(如 吃了一半/中午)；想不出就省略
- steps 可选，最多3条，每条≤15字，只写关键操作(备料→火候→调味→出锅)，不写克数和时间；用户没提做法且菜名推不出典型做法就整个省略，不编造
```

### 4.4 新增/变更类型定义

```kotlin
// NdjsonEvents.kt — NdjsonLine 的 dish 事件字段区（插入在 eaten_ratio 之后，ingredient 字段注释之前）
data class NdjsonLine(
    ...
    val eaten_ratio: Double? = null,
    val cuisine: String? = null,   // [新增] 菜系，可选
    val tags: List<String>? = null, // [新增] 标签，可选，最多2个（prompt约束，解析层不强制截断，仅清洗）
    val steps: List<String>? = null, // [新增] 做法步骤，可选，最多3条（同上）
    // ingredient 事件字段
    ...
)

// NdjsonEvents.kt — DishDraftNode 末尾新增两个字段（cookingSteps 复用既有，不新增）
data class DishDraftNode(
    ...
    val cookingSteps: List<DraftCookingStep> = emptyList(),
    val warnings: List<String> = emptyList(),
    val cuisine: String? = null,   // [新增，末尾追加，带默认值，防位置参数构造错位——虽 §2 D7 核实无位置参数构造，仍按红线执行]
    val tags: List<String> = emptyList(), // [新增，同上]
)
```

```kotlin
// 新文件 shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/CuisineNormalizer.kt
package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.domain.model.Cuisines

/**
 * NDJSON dish 事件 cuisine 字段白名单归一——纯函数，未命中返回 null（视同"AI未给出信号"，
 * 不由本层伪造兜底值；兜底职责在 DishAutoGenerator.commit() 落库层，见批A蓝图 INV-A-02）。
 * [AI生成] 批B：cuisine 真透传。
 */
internal object CuisineNormalizer {
    fun normalize(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return trimmed.takeIf { it in Cuisines.ALL }
    }
}
```

**放置位置说明**：独立小文件而非塞进 `NdjsonEvents.kt` 尾部——`NdjsonEvents.kt` 现有职责是纯数据类型定义（无逻辑函数），`CuisineNormalizer` 带有真实归一逻辑且需要独立单测，放独立文件保持"数据定义"与"处理逻辑"分离，与项目内 `MealDateAnchorPolicy.kt`/`TextNormalizer.kt` 等既有的同类小型纯函数归一工具同一放置模式（可见性收 `internal`，与 `MealStreamDraftMapper` 同例——后者已是 `internal object` 且被 `androidUnitTest` 正常访问，2026-08-19 独立挑战 N-B-2 核实）。

### 4.5 GC-29：`handleDishEvent` 合并规则改动（`StreamingMealParser.kt:317-334`）

现状代码（本蓝图起草时精确行号）：
```kotlin
// AF-05: 同键合并——合并非空字段，保留已有子项
val meal = seg.meals[mealId]!!
val existingDish = meal.dishes[dishId]
val dishNode = if (existingDish == null) {
    DishDraftNode(dishId = dishId, name = name,
        cookingMethod = line.cooking_method, quantity = line.quantity,
        unit = line.unit, eatenRatio = line.eaten_ratio, note = line.note)
} else {
    existingDish.copy(
        name = name,
        cookingMethod = line.cooking_method ?: existingDish.cookingMethod,
        quantity = line.quantity ?: existingDish.quantity,
        unit = line.unit ?: existingDish.unit,
        eatenRatio = line.eaten_ratio ?: existingDish.eatenRatio,
        note = mergeNote(existingDish.note, line.note),
    )
}
seg.meals[mealId] = meal.copy(dishes = meal.dishes + (dishId to dishNode))
```

改为（新增部分标注，2026-08-19 独立挑战 B-I-2/B-I-4 修订：补 INFO 诊断 + 空数组同义空缺省）：
```kotlin
// AF-05: 同键合并——合并非空字段，保留已有子项
val meal = seg.meals[mealId]!!
val existingDish = meal.dishes[dishId]
val normalizedCuisine = CuisineNormalizer.normalize(line.cuisine)          // [新增]
// [新增，INV-B-02] AI 给了 cuisine 但未命中白名单：留痕不阻断，供真机填充率复盘区分
//   "AI 没填" vs "AI 填了脏值被丢弃"。
if (!line.cuisine.isNullOrBlank() && normalizedCuisine == null) {
    orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.INFO, line.segment_id, mealId, dishId,
        "菜系「${line.cuisine}」不在可选菜系内，已忽略", code = DiagnosticCode.OTHER))
}
// [新增，B-I-4] 空列表与缺省同义：AI 按模板输出 "tags":[]/"steps":[] 属占位，不应清空上一条事件已写入的值
//   （与 cuisine 的空串经 normalize→null 后被 ?: 挡住保持语义对称）；steps 同时补上空白项过滤。
val cleanedTags = line.tags?.map { it.trim() }?.filter { it.isNotBlank() }?.distinct()?.takeIf { it.isNotEmpty() }
val steppedFromLine = line.steps?.map { it.trim() }?.filter { it.isNotBlank() }
    ?.takeIf { it.isNotEmpty() }?.mapIndexed { i, t -> DraftCookingStep(text = t, order = i) }
val dishNode = if (existingDish == null) {
    DishDraftNode(dishId = dishId, name = name,
        cookingMethod = line.cooking_method, quantity = line.quantity,
        unit = line.unit, eatenRatio = line.eaten_ratio, note = line.note,
        cuisine = normalizedCuisine,                                       // [新增]
        tags = cleanedTags ?: emptyList(),                                 // [新增]
        cookingSteps = steppedFromLine ?: emptyList())                     // [新增]
} else {
    existingDish.copy(
        name = name,
        cookingMethod = line.cooking_method ?: existingDish.cookingMethod,
        quantity = line.quantity ?: existingDish.quantity,
        unit = line.unit ?: existingDish.unit,
        eatenRatio = line.eaten_ratio ?: existingDish.eatenRatio,
        note = mergeNote(existingDish.note, line.note),
        cuisine = normalizedCuisine ?: existingDish.cuisine,                // [新增，INV-B-05]
        tags = cleanedTags ?: existingDish.tags,                           // [新增，INV-B-05]
        cookingSteps = steppedFromLine ?: existingDish.cookingSteps,       // [新增，INV-B-05]
    )
}
seg.meals[mealId] = meal.copy(dishes = meal.dishes + (dishId to dishNode))
```

**注意**（GC-29 判据，2026-08-19 独立挑战 B-I-1 修订）：`cookingSteps` 这一新写入路径（来自内联 `line.steps`）与既有的 `handleCookingStepEvent` 独立事件写入路径**共享同一个 `DishDraftNode.cookingSteps` 字段**，属于"多来源写入同一聚合目标"。
**真实行为（已核实源码，勿按直觉当成"覆盖"）**：`handleCookingStepEvent`（`StreamingMealParser.kt:476-484`）是**按 text 精确去重后追加**；且它在 `:465` 要求 `dishLastLocalByRawId` 已由 `handleDishEvent:307` 写入，故 `cooking_step` **必然晚于**其 dish 事件到达。因此二者共存时的实际结果是**"内联步骤 + 文本不同的独立步骤 叠加"**，而非"最新到达者覆盖"；只有"重复 dish 事件携带 steps 晚于 cooking_step"这一边角顺序才会发生覆盖（把已追加的独立步骤静默丢弃）。
**本蓝图裁决（不变）**：**不新增任何跨来源合并/去重代码**，接受上述既有行为。理由：prompt 从头到尾就没有把 `cooking_step` 列进事件清单（`AiMealPrompt.kt:35-42` 实测无该事件），AI 自发同时使用两种写法属未定义输入；为其设计跨来源合并策略的 ROI 为负。
**验收要求**：STEP-B-T-1 须新增一个"混用"用例（T-B-07），把上述叠加行为**钉成回归基线**，防止后续有人按"改成覆盖"理解而不自知，见 §8.2。

### 4.6 GC-13：fallback 复用主路径校验入口

`CuisineNormalizer.normalize()` 是本批新增的唯一归一入口，`handleDishEvent` 唯一调用点，无第二套判据；`tags` 清洗逻辑（trim+过滤空白+去重）与 `DishRepository.saveDish` 的既有 tagNames 清洗（B7 后续批 S1）同口径但**不是同一份代码**（一个在协议解析层、一个在落库层，属于两层各自独立防御，均需要各自清洗——协议层清洗是为了不让脏数据进入草稿模型影响后续判定，落库层清洗是最后一道防线，两者不冲突不重复劳动，是纵深防御的合理设计，不违反"唯一校验入口"原则，因为它们不是对同一件事的重复判断，是防御链的两环）。

---

## §5 UI 设计

不适用（本批不涉及任何 UI/Compose 改动，纯 shared 层）。

---

## §6 文件改动清单 + Allowlist

| 文件 | 允许操作 | 禁止操作 |
|---|---|---|
| `shared/.../ai/meallog/NdjsonEvents.kt` | `NdjsonLine` 加 3 个可选字段（§4.4）；`DishDraftNode` 末尾加 2 个字段（§4.4） | 改动其余数据类/枚举 |
| 新建 `shared/.../ai/meallog/CuisineNormalizer.kt` | 按 §4.4 新建 | — |
| `shared/.../ai/meallog/AiMealPrompt.kt` | 按 §4.3 改 `NDJSON_SYSTEM_PROMPT` 第37行+规则区新增3行 | 改动 `FLAT_SYSTEM_PROMPT`/`buildRequest`/其余函数 |
| `shared/.../ai/meallog/StreamingMealParser.kt` | `handleDishEvent` 按 §4.5 新增归一/清洗/合并逻辑；顶部按需新增 `import` | 改动 `handleMealEvent`/`handleDishChildEvent`/`handleCookingStepEvent`/`handleWarningEvent`/`handleAdviceEvent`/`canonicalMealId`/`mealIdAlias`/`dishLocalKeyByName`/`dishLastLocalByRawId`/`dishSeqByMeal` 相关逻辑（10-b/10-c 既有实现不动） |
| `shared/.../ai/meallog/MealStreamDraftMapper.kt` | `toDishRef()` 的 `DishJson(...)` 构造新增 `cuisine`/`tags` 两个具名参数 | 改动 `toDayMealJson`/`toIngredients`/排序逻辑 |
| `shared/.../ai/meallog/MultiDayRecorder.kt` | `toSemanticDay()` 的 `SemanticDish(...)` 构造新增 `cuisine = dishJson?.cuisine.orEmpty()` | 改动其余逻辑 |
| `shared/.../domain/autogen/AutoGenModels.kt` | **仅当 §4.1 判定为"未落地"时**：`SemanticDish`/`DishPreview` 末尾各加 `val cuisine: String = ""` | 改动既有字段顺序 |
| `shared/.../domain/autogen/DishAutoGenerator.kt` | `preview()` CREATE 分支 `DishPreview(...)` 新增 `cuisine = input.cuisine`；**仅当 §4.1 判定为"未落地"时**额外在 `commit()` 新增 `cuisine = preview.cuisine.ifBlank { Cuisines.HOME }` + 按需 `import Cuisines` | 改动 REUSE 分支（REUSE 不传这些字段是既定语义，不变） |

**显式禁改文件清单**：
- `androidApp/.../ui/ai/AiMealInputSheet.kt`（批C范围，本批零 UI 改动）。
- `shared/.../data/repository/DishRepository.kt`（`saveDish` 签名已支持 `cuisine` 参数，本批不改该文件）。
- 批A涉及的死代码删除文件（`AiMealParser.kt` 等，与本批无关）。

**回归基线锁定（GC-09）**：
- `scripts\build-cli.bat :shared:testDebugUnitTest`（全量，0 failures，含既有 `StreamingMealParserTest` T-01~T-08/AF-03/05/08/D-01~D-08/10-b/10-c 全部保持绿，2026-08-19 独立挑战 N-B-1 订正：`StreamingMealParserTest.kt` 实际无 AF-07 用例，不要求存在）
- `scripts\build-cli.bat :androidApp:testDebugUnitTest`（全量，0 failures）
- `scripts\build-cli.bat :androidApp:assembleDebug`

---

## §7 分阶段实施步骤

**STEP-B-1.1**：执行 §4.1 两条判定 grep，记录结果，决定 STEP-B-1.7/1.9 是否执行。

**STEP-B-1.2**：`NdjsonEvents.kt` 按 §4.4 新增字段。
完成形态：`grep -n "val cuisine: String? = null" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/NdjsonEvents.kt` 命中 2 处（`NdjsonLine` 一处、`DishDraftNode` 一处，注意后者默认值写法相同但类型语境不同，均需命中）；`grep -n "val tags: List<String>" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/NdjsonEvents.kt` 命中 2 处。

**STEP-B-1.3**：新建 `CuisineNormalizer.kt`，按 §4.4 实现。
完成形态：`grep -n "object CuisineNormalizer" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/CuisineNormalizer.kt` 命中 1 处。

**STEP-B-1.4**：`AiMealPrompt.kt` 按 §4.3 改 `NDJSON_SYSTEM_PROMPT`。
完成形态：`grep -n "\"cuisine\":\"菜系\"" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/AiMealPrompt.kt` 命中 1 处；`grep -n "cuisine 可选，只能取以下之一" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/AiMealPrompt.kt` 命中 1 处。

**STEP-B-1.5**：`StreamingMealParser.kt` 的 `handleDishEvent` 按 §4.5 改写。
完成形态：`grep -n "CuisineNormalizer.normalize(line.cuisine)" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/StreamingMealParser.kt` 命中 1 处；`grep -n "cuisine = normalizedCuisine ?: existingDish.cuisine" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/StreamingMealParser.kt` 命中 1 处（merge 分支的防清空写法，是 INV-B-05 的核心判据，STEP 完成形态必须包含这条，不能只验证 create 分支）。

**STEP-B-1.6**：`MealStreamDraftMapper.kt` 按 §4.2 新增透传。
完成形态：`grep -n "cuisine = dish.cuisine.orEmpty()" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/MealStreamDraftMapper.kt` 命中 1 处；`grep -n "tags = dish.tags" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/MealStreamDraftMapper.kt` 命中 1 处。

**STEP-B-1.7**（条件执行，见 STEP-B-1.1 判定）：`AutoGenModels.kt` 加字段。
完成形态：`grep -n "val cuisine: String = \"\"" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/AutoGenModels.kt` 命中 2 处。

**STEP-B-1.8**：`MultiDayRecorder.kt` 按 §4.2 新增透传。
完成形态：`grep -n "cuisine = dishJson?.cuisine.orEmpty()" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/MultiDayRecorder.kt` 命中 1 处。

**STEP-B-1.9**（条件执行，见 STEP-B-1.1 判定）：`DishAutoGenerator.kt` 的 `commit()` 加 cuisine 传参。
完成形态：`grep -n "cuisine = preview.cuisine.ifBlank" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/DishAutoGenerator.kt` 命中 1 处。

**STEP-B-1.10**：`DishAutoGenerator.kt` 的 `preview()` CREATE 分支加 `cuisine = input.cuisine`。**执行前先跑** `grep -n "cuisine = input.cuisine" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/DishAutoGenerator.kt`，**零命中**方可执行；已命中即 STOP 转 Q 缺口单（见 §4.1 末条，2026-08-19 独立挑战 B-I-7）。
完成形态：`grep -n "cuisine = input.cuisine" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/DishAutoGenerator.kt` 命中 1 处。

**STEP-B-T-1**：新增测试，覆盖 §8.2 全部 T-ID。

**验收命令**：
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
| `CuisineNormalizerTest.kt`（新建，纯函数测试，无需数据库） | 验证白名单命中/未命中/空白/trim 边界 | 依赖任何 DB/Repo 基础设施 |
| 既有 `StreamingMealParserTest.kt` 的 `parser(...)` 辅助函数 | 验证 dish 事件的 cuisine/tags/steps 字段解析+归一+重复到达合并 | 弱化既有 T-01~T-08/AF-03/05/07/08/10-b/10-c 断言换取新用例通过 |
| 既有 `MultiDayRecorderK1aTest.kt`/`DishAutoGeneratorTest.kt` 的 `db`/`ctx` | 验证端到端 cuisine 透传到落库 | 绕过真实 `DishAutoGenerator.commit()` 直接操作 DB 伪造结果 |

### 8.2 INV↔T 双向映射表

| INV | T-ID | 断言要点 |
|---|---|---|
| INV-B-01 | T-B-01 | `CuisineNormalizer.normalize(null)`/`normalize("")`/`normalize("   ")` 均返回 `null` |
| INV-B-02 | T-B-02 | `CuisineNormalizerTest`：`normalize("川菜")=="川菜"`；`normalize(" 川菜 ")=="川菜"`（trim 后命中）；`normalize("四川菜")==null`（未命中白名单）。**另加**（2026-08-19 独立挑战 B-I-2 修订）`StreamingMealParserTest` 用例：dish 事件带 `"cuisine":"四川菜"` → ①`DishDraftNode.cuisine == null`（脏值未落草稿）②`draft.diagnostics` 中存在一条 `level==DiagnosticLevel.INFO` 且 message 含"菜系"的记录（钉住 INV-B-02 的留痕子句，防止只实现"归 null"半条） |
| INV-B-03 | T-B-03 | `StreamingMealParserTest` 新用例：dish 事件带 `"tags":[" 下饭菜 ","","下饭菜","快手菜"]` → `DishDraftNode.tags == ["下饭菜","快手菜"]`（去空格+去空白项+去重） |
| INV-B-04 | T-B-04 | `StreamingMealParserTest` 新用例：dish 事件带 `"steps":["焯水","炒香","出锅"]` → `dish.cookingSteps.map{it.text} == ["焯水","炒香","出锅"]` 且 `order` 依次为 0,1,2 |
| INV-B-04（混用路径行为锁定，2026-08-19 独立挑战 B-I-1 新增） | T-B-07 | `StreamingMealParserTest` 新用例：dish 事件带 `"steps":["焯水","炒香"]`，随后到 2 条 `cooking_step`（`text` 分别为 `"焯水"`（重复）与 `"出锅"`（新增））→ 断言 `dish.cookingSteps.map{it.text} == ["焯水","炒香","出锅"]`（重复文本去重、新文本追加，共 3 条），**并在用例注释里写明"这是既有 append 语义的行为锁定，不是期望的产品语义"** |
| INV-B-05 | T-B-05 | `StreamingMealParserTest` 新用例：同一 dish 先到一条带 `cuisine:"川菜",tags:["下饭菜"],steps:["焯水"]` 的事件，再到一条**不带**这三个字段（`null`）但改了 `quantity` 的事件 → 最终 `dish.cuisine=="川菜"`、`dish.tags==["下饭菜"]`、`dish.cookingSteps` 仍是 `["焯水"]`（三者均未被第二次事件的 `null` 清空）。**补充**（B-I-4）：再补一条第 3 次事件携带 `"cuisine":"","tags":[],"steps":[]` → 三者仍分别为 `"川菜"`/`["下饭菜"]`/`["焯水"]`（空值等同缺省，不清空） |
| INV-B-06（前半链路：草稿→JSON→语义，2026-08-19 独立挑战 B-I-6 拆分） | T-B-06a | `MealStreamDraftMapperTest` 新用例：构造 `DishDraftNode(cuisine="川菜", tags=listOf("下饭菜"), cookingSteps=listOf(DraftCookingStep("焯水",0)))` → `toDayMealJson(...)` → 断言产出的 `DishJson.cuisine=="川菜"`、`tags==["下饭菜"]`、`steps==["焯水"]`（钉住 STEP-B-1.6 两个新具名参数）。**并** `MultiDayRecorderK1aTest` 新用例：`DayMealJson` 里 `DishJson(cuisine="川菜")` → 经 `previewAll()` → 断言对应 `DishPreview.cuisine=="川菜"`（钉住 STEP-B-1.8 的 `toSemanticDay` 透传 + STEP-B-1.10 的 `preview()` 透传） |
| INV-B-06（后半链路：语义→落库） | T-B-06b | `DishAutoGeneratorTest` 新用例：`SemanticDish(name="回锅肉", cuisine="川菜")` → `preview()` → `commit()` → `DishRepository(db).getDishById(id)!!.cuisine == "川菜"`（**非** `"家常菜"`，证明真实值优先于 §4.1 的 `ifBlank` 兜底） |
| INV-B-07（2026-08-19 独立挑战 B-I-5 新增） | T-B-08 | `StreamingMealParserTest` 新用例：dish 事件写成 `"tags":"下饭菜"`（标量）→ ①该菜**不存在**于草稿（整行被拒）②`draft.diagnostics` 含 `code == DiagnosticCode.PARSE_ERROR` 一条。用例注释写明"锚定爆炸半径：dish 行被拒会连带 orphan 掉其全部子事件，真机若发现该诊断非零需回头评估宽松解析" |

---

## §9 交付台账（CODE 完成时填）

### §4.1 判定结果记录（必填）

- `grep -n "val cuisine" AutoGenModels.kt` 结果：______（命中数，决定 STEP-B-1.7 是否执行）
- `grep -n "cuisine = preview.cuisine" DishAutoGenerator.kt` 结果：______（命中数，决定 STEP-B-1.9 是否执行）

### STEP 勾销表

| STEP-ID | 状态 | 落地 commit | diff 定位 |
|---|---|---|---|
| STEP-B-1.1~1.10 | ⬜ | | |
| STEP-B-T-1 | ⬜ | | |

### 验收命令输出

（CODE 填）

### 代码质量门禁

（`google_quality_engineer` 终审结论）

### 真机待验证登记（必须，非可选，GC-08）

本批交付后**必须**真机验证 AI 实际填充率——这是批C（确认页 UI）能否启动设计的前置输入数据，不是可选的锦上添花项。登记至最新真机待验证清单，至少包含：
- 多组不同菜品输入（含"回锅肉"这类有明确菜系归属的、"番茄炒蛋"这类家常菜、"薯片"这类零食/非正餐）观察 `cuisine` 实际填充率与准确率。
- 观察 `tags`/`steps` 的实际填充率（AI 是否倾向于"能填就填"还是"经常省略"）。
- 观察 `DiagnosticCode.TRUNCATED`/`isTruncated` 是否因新增字段导致 token 压力上升而更频繁出现（对比本批交付前的基线），**平均输出 token 数与 `TRUNCATED` 发生率对比为必填基线数据**（2026-08-19 独立挑战 N-B-5），非泛泛观察——若填充率/截断率异常升高，回退方案见 §12（从 prompt 模板行移除 tags/steps 示例、只保留规则区文字描述）。
- 统计 `DiagnosticCode.PARSE_ERROR` 在 dish 事件上的发生率（2026-08-19 独立挑战 B-I-5，见 INV-B-07）：本批新增两个 `List<String>` 字段后，模型写错类型（如 `"tags":"下饭菜"` 标量）会导致整条 dish 事件被丢弃并 orphan 其全部子事件；若非零，回填到 §12 作为"是否改宽松解析"的决策输入。

---

## §10 独立挑战台账（GC-37）

**挑战方**：独立 general-purpose/opus agent（只读蓝图成文，未见起草过程，逐条回源码 file:line 核实）。

**结论**：§2 现状表 20 余处 file:line 抽检，除 B-I-3 外全部准确（含 D6 `Cuisines.ALL` 11 项、`NdjsonLine` 无位置参数构造、既有测试不受影响等）。挑出 7 项 CONFIRMED-ISSUE，均已就地处置：

| # | 挑战摘要 | 裁决 | 处置 |
|---|---|---|---|
| B-I-1（阻断） | §4.5/INV-B-04 声称"谁最后写入听谁的/覆盖"，与 `handleCookingStepEvent`（`StreamingMealParser.kt:476-484`）真实的"按text去重后追加"行为相反，且 `cooking_step` 必然晚于其 dish 事件到达 | CONFIRMED-ISSUE | INV-B-04 禁止结果列 + §4.5「注意」段整段改写为真实行为描述；新增 T-B-07 把叠加行为钉成回归基线 |
| B-I-2（阻断） | INV-B-02 要求未命中白名单时记 `DiagnosticLevel.INFO` 诊断，但 §4.5 代码骨架与 §8.2 测试均未覆盖，是"契约与骨架不一致" | CONFIRMED-ISSUE | §4.5 代码块补 INFO 诊断分支；T-B-02 补一条 `StreamingMealParserTest` 用例锁住诊断留痕 |
| B-I-3 | §2 D4 对 `FLAT_SYSTEM_PROMPT` 触发条件描述失实（声称被 `tryWholeJsonFallback()` 间接触发，实际该函数不发请求也不用该 prompt；真相是 `buildRequest()`/`SYSTEM_PROMPT` 已零调用） | CONFIRMED-ISSUE | D4 改写为准确描述；§12 新增弃置项登记 |
| B-I-4 | merge 代码只挡 `null`，AI 若输出 `"tags":[]`/`"steps":[]`（模板占位常见行为）会静默清空已写入值，与 cuisine 的空值语义不对称 | CONFIRMED-ISSUE | §4.5 代码块改为 `takeIf{isNotEmpty()}`，空数组同义空缺省；INV-B-05 条件列+T-B-05 补充断言 |
| B-I-5 | 新增两个 `List<String>` 字段扩大了"整条 dish 事件被反序列化失败丢弃"的爆炸半径（会连带 orphan 其全部子事件），蓝图未提及 | CONFIRMED-ISSUE | 新增 INV-B-07 + T-B-08，§9 真机项加 `PARSE_ERROR` 发生率统计 |
| B-I-6 | T-B-06 起点是 `SemanticDish`，跳过了本批新写的 mapper/recorder 两跳透传，INV-B-06"完整链路"名不副实 | CONFIRMED-ISSUE | 拆分为 T-B-06a（草稿→JSON→语义）+ T-B-06b（语义→落库） |
| B-I-7 | §4.1 判定分支缺"半落地"（命中1处）分支，且 STEP-B-1.10 无条件执行会与批A（若批A违反自身allowlist改了`preview()`）产生重复传参 | CONFIRMED-ISSUE | §4.1 补半落地 STOP 分支；STEP-B-1.10 加执行前置 grep 判定 |

处置后自查：7 项均已在 §2/§3/§4.1/§4.5/§7/§8/§9/§12 同步修订。另有 6 项 MINOR-NIT（AF-07 不存在的措辞订正、`CuisineNormalizer` 可见性说明、`commit()` 行号统一、order 排序观察、token/截断基线数据要求、条数上限决策记录）已一并采纳修订。**蓝图转 `BLUEPRINT_READY`**。

---

## §11 门禁与角色

- 本批不涉及 UI/交互/用户可见文案（prompt 文本不是面向用户的文案，不触发 `copywriter` 门禁；不新增用户提示/后台自动行为的告知设计，不触发 `apple_software_behavior` 门禁），豁免 `apple_ux_designer`/`apple_software_behavior`/`copywriter` 前置门禁。
- CODE 完成、构建+单测通过后，须走 `google_quality_engineer` 代码质量终审，终审范围须显式包含：①§4.1 跨批次判定是否被正确执行且如实记入 §9；②§4.5 合并语义的三个新增字段是否真的用了"`?:` 保留旧值"写法而非直接覆盖（INV-B-05 的核心风险点）。

## §12 弃置项登记（GC-03 前瞻）

| 项 | 状态 | 归宿 |
|---|---|---|
| `description` 真透传 | 显式弃置，用户已拍板不收 | 若未来产品侧改变判断，需要新开决策记录重新评估，不在本批/批C范围内重议 |
| `meal_slots` 真透传 | 显式弃置，用户已拍板不收 | 同上 |
| 独立 `cooking_step` 事件类型的 prompt 化 | 显式弃置，token 成本考量下选择内联方案 | `handleCookingStepEvent` 与相关字段保留不删（前向兼容），若未来场景变化（如需要单条步骤级别的独立诊断/编辑）可重新评估 |
| 确认页 UI 展示这批产出的新数据 | 依赖交付给批C | 见 `AI记一餐_确认页展开UI_实施蓝图.md` |
| `AiMealPrompt.buildRequest()`/`SYSTEM_PROMPT`/`FLAT_SYSTEM_PROMPT` 已零生产调用（2026-08-19 独立挑战 B-I-3） | 显式弃置，不纳入本批 | 与批A §12 的 `NdjsonEvent` 密封族同性质，统一留待下次 `ai/meallog` 死代码批处理 |
| 解析层/落库层均不对 tags（2个）/steps（3条）做条数硬上限，靠 prompt 软约束（2026-08-19 独立挑战 N-B-6） | 有意为之，非遗漏 | 若真机验证发现 AI 超量输出，再评估在 `handleDishEvent` 加 `.take(2)`/`.take(3)` 硬截断 |
