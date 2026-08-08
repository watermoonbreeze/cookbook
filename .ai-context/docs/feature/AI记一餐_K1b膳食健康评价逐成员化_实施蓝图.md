# AI记一餐 K1b：膳食健康评价逐成员化 实施蓝图

> 状态：`DRAFT · 已暂停（PARKED）`——蓝图起草 + GC-37 独立挑战均已完成（挑战结果见 §10：11 项 CONFIRMED-ISSUE + 9 项 MINOR-NIT 待处置），**但 2026-08-08 用户叫停**：AI快捷记一餐(K1系列) 上一批(B4+B5+B6+K1a+CFG) 的真机验证清单（`真机待验证清单_202608081130.md`）里 E-B4-*/E-B5-*/E-B6-*/E-K1A-01/E-CFG-01~06 近 30 项绝大多数仍是 `⬜`/`🔧`（未在真机确认），K1e/K1h/K1i/L1 也未启动——**AI快捷记一餐主线尚未真正做完，不应在此时插入新的 K1b 精细化工作**。本蓝图按用户指示**原样保留、不回退、不删除**，作为完整可用的未来产出；§10 的挑战结论留档待下次拾起时直接处置。**下一步不是继续本蓝图**，见 `SESSION_交接.md`。
> **颗粒度：L7**（项目基线；GC 条款清单见 `experience/12_多模型协作与实施蓝图规范.md` §12）。**§0.1 是入口——先读它，逐行对照落点章节。**
> 起草日期：2026-08-08（K1a 营养展示统一化批次 ACCEPTED 关闭后，ARCH@主力机 按 `BLUEPRINT_STATE.md` 候选 K1b 起草；同日完成 GC-37 独立挑战后用户叫停，转 PARKED）。
> 前置：`AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md`（K1b 姊妹批次，K1 系列既有约定/冻结字面量参考）、`2026-08-04_AI记一餐V2_双阶段解析与健康建议方案.md`（K1b 阶段二原始产品设计）。
> 依赖检查：K1a 已 `ACCEPTED`（`5c976a49`），`DishPreview.nutrition: DishNutrition?` 已可用，本批直接复用、不重算。

---

### §0.1 颗粒度勾销表（GRANULARITY = L7）

| GC | 条款一句话 | 本蓝图落点 | 状态 |
|---|---|---|:--:|
| GC-01 | 每个行为分支写成"条件→唯一动作→禁止动作"，对歧义词 grep 零命中 | §3 不变量表 | 满足 |
| GC-02 | allowlist：文件×允许操作×禁止操作 + 显式禁改清单 | §6 | 满足 |
| GC-03 | 上一批延后项归宿表 | §1.4 | 满足：K1a §12 弃置项与本批无交集（本批不涉及食材级 REUSE 精度/每餐合计 UI/规则兜底按钮），逐项在 §1.4 复核并注明"与本批无关" |
| GC-04 | 每条 INV 具备 ID/条件/必须结果/禁止结果/证据五列 | §3 | 满足 |
| GC-05 | INV↔T 双向映射表 | §8.2 | 满足 |
| GC-06 | 放行条件写出命令原文 | §7 末尾"验收命令" | 满足 |
| GC-07 | 测试夹具职责边界表 | §8.1 | 满足 |
| GC-08 | 交付台账含真机清单文件名+编号区间 | §9（交付时登记，本蓝图先声明编号前缀 `E-K1B-*`） | 满足 |
| GC-09 | 列出本批不得失败的既有回归套件全名 | §6 末尾"回归基线锁定" | 满足 |
| GC-10 | 逐字段真相源表 | §4.1 | 满足 |
| GC-11 | 新增/重命名与既有字段语义重叠字段时给旧字段全部写入点清单 | §4.2（`healthAdvice`→`memberAdvice`、`healthSummaryLabels()`→`MemberMealFactsBuilder` 两组迁移） | 满足 |
| GC-12 | UI 判据与业务判据同源表 | §4.3 | 满足 |
| GC-13 | fallback 先转换为主路径类型再复用主路径校验入口 | — | N/A：本批"解析失败回退整体展示"（INV-K1B-06）是纯字符串兜底，不涉及主路径校验入口复用类场景 |
| GC-14~16 | 对象生命周期表 / 可变持有物传递形态 / 搬迁历史注释清单 | — | N/A：本批不涉及系统资源持有对象（麦克风/Stream/Job），`MemberMealFactsBuilder` 是无状态单例（Koin `single`），不搬迁既有代码块 |
| GC-17~19 | 逐项状态 List&lt;Status&gt; / 索引空间标注 / 过滤链画出 | §4.1 脚注 | `memberIndex` 索引空间需说明（见脚注），非"列表逐项状态机"场景，其余 N/A |
| GC-20 | 自动副作用清单表 | §3 INV-K1B-06 | 满足：解析失败降级为整体展示是主动兜底非静默丢弃，已入 INV |
| GC-21 | INV 写"提示/告知"字样必须有 STEP 落点 | §7 STEP-K1B-6 | 满足 |
| GC-22 | 每条可见副作用配 T-ID/真机项编号 | §8.2 | 满足 |
| GC-23 | 实施脚本每个最小动作独立编号 STEP，含文件/定位/动作/完成形态 | §7 | 满足 |
| GC-24 | 交付台账含 STEP 勾销表，Evidence 只能引用真实存在的测试/commit | §9（交付时填，本蓝图预留表头） | 满足 |
| GC-25 | STEP 完成形态是字面量时写出目标字面量原文+grep判据 | §7 每条 STEP 末尾 | 满足 |
| GC-26 | 冻结值修订记录表 | — | N/A：`DISH_SODIUM_HIGH_MG=667.0` 数值不变，仅从 `DishNutritionLine.kt` 私有常量搬迁为 `NutritionLevelEvaluator` 公开常量，非阈值修订 |
| GC-27 | 编辑即失效收口函数（`invalidateGenerationToInput`）核对表 | §4.2 第三段（重要发现） | 满足——本批发现并修复既有缺口：该函数当前重置 `healthSafetyReport` 但漏了 `healthAdvice`/`healthAdviceConsentPending`/`healthAdviceLoading`/`healthAdviceError`，本批新增字段一并补齐 |
| GC-28 | 构造时创建、后续多次迭代复用对象是否按基数分片 | — | N/A：`MemberMealFactsBuilder.build()` 每次调用独立入参、无跨调用累积状态 |
| GC-29 | 多来源写入同一聚合目标必须声明合并/覆盖 | §4.1（`memberAdvice` 唯一写入者是 `confirmHealthAdvice()` 的一次 `_state.update`，非累积写） | 满足 |
| GC-30 | 状态转移驱动完整副作用链 | §3 INV-K1B-05（隐藏入口）、INV-K1B-06（解析+兜底+错误态三分支全给出） | 满足 |
| GC-31 | 挂起点清单 + 恢复后重新校验 generation 身份 | §4.4 挂起点表 | 满足 |
| GC-32 | 高频异步事件的节流/去重策略 | — | N/A：本批新增调用（`memberFactsBuilder.build()`）与既有 `buildHealthSafetyReport()` 同一触发点（PARTIAL_READY/PREVIEW_READY 边界），复用既有节流（`isBoundary`/`lastPreviewDays` 判重），未新增独立高频触发源 |
| GC-33 | 禁止为测试暴露新的可变全局注入点 | — | N/A：`MemberMealFactsBuilder` 通过构造参数注入，测试用 fake repo 走正常 DI，无新增 `var`+`replaceXxxForTest` |
| GC-34 | 复核注释/KDoc 与实现一致性 | §7 每条 STEP 要求同步改注释 | 满足 |
| GC-35 | 协议事件枚举与处理分支逐项对照 | — | N/A：本批不涉及网络协议/NDJSON 事件类型，AI 响应是纯文本+正则标记解析，非协议层枚举 |
| GC-36 | 数据层 List&lt;Status&gt; 前先列真实状态空间 | — | N/A：`memberAdvice: Map<Int,String>` 不是状态机，是"成员编号→建议文本"的稀疏映射，键存在性即代表"已生成"，无需值域覆盖论证 |
| GC-37 | 蓝图冻结前存在独立挑战台账 | §10 | 待跑（本蓝图 DRAFT 阶段，挑战完成后补齐） |

任一条"未满足"须先处理。当前 GC-37 待跑，其余全部满足；GC-37 处置完毕前蓝图不得转 `BLUEPRINT_READY`。

---

## §1 目标与范围

### 1.1 一句话价值

把"AI 快捷记一餐"确认页现有的单条聚合式健康建议，升级为**逐位家庭成员**分别评价——每位设有健康关注点的成员各自看到"这餐对我而言要注意什么"，而不是一段混在一起、看不出是说给谁听的通用文案。

### 1.2 触发来源与现状核查（重要：修正上一批遗留的过时判断）

- **K1b**（`待办_功能算法.md` 🔄）：AI解析带入家庭健康档案做膳食营养健康评价，K1 Phase2 剩余项。
- **K1a 蓝图 §1.3 遗留记录已过时，本节予以订正**：K1a 蓝图（2026-08-07 起草）曾写"K1b 强依赖 L1 合规闸门，L1 未落地则继续冻结"。经本轮核查，**该判断基于不完整的代码现状**——实际上 K1b 阶段二的基础版本（`confirmHealthAdvice()`/`healthAdviceConsentPending` 同意弹窗/`healthSummaryLabels()` 脱敏摘要）早在 `fd9f1d2e`/`6a451f51`（2026-08 上旬，早于 K1a 蓝图起草）就已实现并随 `AiMealInputSheet` 一起交付，只是当时未被 K1a 蓝图作者注意到（K1a 专注营养展示，未逐行核对 K1b 相关代码）。该基础版本已自带**功能级 T2 事前告知**（`healthAdviceConsentPending` 弹窗逐条列出发送范围+拒绝选项），满足"改数据/联网前先弹窗+可选"的透明准则档位。
- **L1**（`待办_工程合规.md` ⬜）实际含义是"用户协议开头免责声明 + **首次开启云端 AI** 时的一次性风险弹窗"——这是覆盖**全 App 所有云端 AI 调用点**（含菜品自动生成、AI 推荐、AI 记餐解析本身）的统一入口级告知，与"某一次具体请求发送什么"的功能级弹窗是两个正交层次。当前 App 内包括 AI 记餐解析、AI 推荐等**全部**云端 AI 功能都已在 L1 未完成的情况下正常上线使用，唯独对 K1b 单独执行"强依赖 L1"的更严格标准，是不一致的既有判断，不构成本批继续冻结的理由。
- **结论**：L1 是独立的、应尽快推进的 App 级合规缺口（不因本批而降低优先级，仍是 `待办_工程合规.md` 的 🔴 项），但**不阻塞本批**——本批只是在一个已通过同意、已具备功能级告知的既有能力上做"从聚合到逐成员"的精细化，不引入新的"是否该发送数据给第三方"这一类决策，不扩大风险面。

### 1.3 In Scope / Out of Scope

**In Scope**：
1. 新增 `MemberMealFactsBuilder`（shared）：对预览中的每位**设有健康关注点**（`careCategoryIds` 非空）的家庭成员，用**本机既有规则**（复用 `IngredientRepository.listByCareCategories`/`NutritionLevelEvaluator.dishQualitativeHits`/`DishPreview.nutrition`，不重新发明判定逻辑）算出该成员与本次预览餐食的关联事实：忌口命中菜名、高嘌呤/高GI命中菜名、钠偏高命中菜名。
2. `AiMealHealthAdvice` 请求契约改造：`user` 内容从"一整段拼接摘要"改为按成员分节的结构化事实块；`system` 要求 AI 逐成员输出、用可机械解析的 `[[M{index}]]` 标记分节，且只转述给定事实、不得编造。
3. 新增 `parseMemberSections()`：把 AI 原始返回按标记切分为 `Map<成员编号, 建议文本>`；解析失败（AI 未遵循标记格式）时不丢弃，整体降级为一条通用建议（编号 0）。
4. `AiMealInputViewModel`：`healthAdvice: String?` → `memberAdvice: Map<Int,String>`；新增 `memberFacts: List<MemberMealFact>`；`requestHealthAdvice()` 增加"无关注成员则不生成"门禁；`buildHealthSafetyReport()` 复用 `memberFacts` 替代原 `healthSummaryLabels()` 独立查询（消除重复 DB 往返）；补齐 `invalidateGenerationToInput()` 遗漏的健康建议相关字段重置（GC-27 发现的既有缺口）。
5. `AiMealInputSheet.kt`：健康建议展示区从单条卡片改为逐成员卡片列表；"查看建议"入口按 `memberFacts.isNotEmpty()` 门控；同意弹窗文案更新为准确描述"发送本机判定事实、为每位有关注点的成员分别生成建议"。
6. 常量搬迁：`DishNutritionLine.kt` 私有 `SODIUM_HIGH_PER_DISH_MG` → `NutritionLevelEvaluator.DISH_SODIUM_HIGH_MG`（公开、shared 层，供 `MemberMealFactsBuilder` 复用同一钠阈值，数值不变）。
7. 删除死代码 `HealthContextBuilder`/`FamilyMemberHealth`（`shared/.../ai/meallog/HealthContextBuilder.kt`）：自 2026-08-01 起零生产调用、零测试覆盖，其数据形状（`ageYears`/`gender`/`chronicConditions`/`lifeStage`/`dietaryRestrictions` 分立字段）与实际数据模型（`FamilyMember.careCategoryIds` 统一承载病种与生命阶段）不匹配，本批用正确建模的 `MemberMealFactsBuilder` 替代，不整改复用。

**Out of Scope（本批不做，理由见括号）**：
- **L1 合规闸门本身**（App 级云端 AI 首次启用弹窗，独立待办，见 §1.2 分析——本批不做不代表降低其优先级，只是判定为并行而非串行依赖）。
- **无健康档案成员的通用饮食提示**（本批只服务"已设关注点"的成员，见 INV-K1B-01；覆盖全部成员需要新的产品设计——没有病种/生命阶段锚点时"建议"内容边界不清，容易滑向泛泛而谈或过度医疗化措辞，留作独立方案讨论）。
- **成员维度的每日/每周汇总健康评价**（本批只针对"这一次 AI 记餐"的预览内容，不做跨餐食/跨日期的累积评估——那是与 J17/首页今日卡更相关的独立命题）。
- **忌口/嘌呤/GI 事实计算的"仅主料"精度**（`HealthRuleEngine`/`MealHealthHintUseCase` 对已提交菜品用 `is_main` 精确区分主辅料，但 `IngredientPreview` 预览态未携带该字段；本批退化为"非调料食材全算"，见 §2 D3 与 §11 弃置项，精度提升需要扩展 `AutoGenModels.kt` 的 `IngredientPreview`，是独立且有一定 blast radius 的改动，不塞入本批）。
- **AI 建议持久化/历史回看**（沿用既有红线：仅本次确认会话展示，关闭/重来即清除，不入库不写日志，见 INV 继承既有约束，非本批新增能力）。

### 1.4 上一批（K1a）延后项归宿核对（GC-03）

K1a 蓝图 §12 弃置项共 4 条：食材级 REUSE 精度、每餐/每天营养合计 UI、规则兜底解析按钮接线、`SwitchableAiRuntime` 未走真流式。逐项核对：均与本批（健康评价逐成员化）无功能交集，不在本批处理范围，继续保持"显式弃置"状态，不重复登记。

---

## §2 现状与差距

| # | 现状 | 证据（file:line） | 差距/影响 |
|---|---|---|---|
| D1 | `confirmHealthAdvice()` 把全部家庭成员的健康标签拼成一整句、连同全部菜名一起发给 AI，AI 返回一段**不分成员**的通用建议 | `AiMealInputViewModel.kt:859-876`（`healthSummaryLabels().joinToString`、`AiMealHealthAdvice.request(healthSummary, mealSummary)` 单字符串入参） | 用户看到"建议少放盐"时不知道是说给谁听的；多个成员病种冲突时（如一人需低嘌呤、一人需低GI）建议会互相稀释成模糊的折中表述 |
| D2 | `healthSummaryLabels()` 只输出病种/生命阶段标签，不携带任何"这餐具体哪道菜有什么问题"的本机事实——AI 完全靠自己"猜"这餐是否有问题，属于自由发挥 | `AiMealInputViewModel.kt:878-888` | 违反"AI 估算/判断不能成为权威健康事实"红线的精神（虽未越权限保存，但展示内容本身可信度依赖 AI 自行判断而非本机规则），且更容易产生与本机 `healthSafetyReport` 矛盾的表述（本机说"待复核"、AI 却给出具体断言） |
| D3 | 本机已有成熟的忌口/高嘌呤/高GI/高钠判定引擎（`HealthRuleEngine`/`NutritionLevelEvaluator`/`MealHealthHintUseCase`），但均基于**已提交**菜品的 `dish_ingredient.is_main`/DB 查询，AI 记餐预览阶段（`DishPreview`/`IngredientPreview`，`resolution` 未必已 commit）完全没有复用这套引擎 | `HealthRuleEngine.kt:66,82`（`nonSeasoning`/`mainIngredients` 依赖 DB 角色列）、`MealHealthHintUseCase.kt:65`（`selectDishIngredientsByDishIds` 按已存在 dishIds 查库） | K1b 的"逐成员评价"若继续绕开本机引擎全靠 AI 自由发挥，等于重复造轮子且精度更差；本批需要一个"预览态可用"的轻量适配层 |
| D4 | `healthAdvice: String?`（旧字段）不受 `invalidateGenerationToInput()` 重置——用户重新编辑输入、发起新一轮生成后，旧建议仍原样挂在 UI 上直到下一次成功 `confirmHealthAdvice()` 覆盖 | `AiMealInputViewModel.kt:317-347`（reset 块无 `healthAdvice`/`healthAdviceConsentPending`/`healthAdviceLoading`/`healthAdviceError` 四行） | 本批发现的既有 sticky-field 缺口（GC-27 一类），与 D1-D3 无直接关系，但新字段引入前必须一并修复，否则新字段会重复同一缺陷 |
| D5 | `HealthContextBuilder`/`FamilyMemberHealth` 自 `20260801` 提交后零调用，其字段形状（`ageYears`/`gender`/`chronicConditions`/`lifeStage`/`dietaryRestrictions` 五个分立字段）无法从当前 `FamilyMember` 数据模型（病种与生命阶段统一存在 `careCategoryIds`，同为 `food_category` crowd 维度分类）正确构造，是设计阶段与实现阶段脱节留下的死代码 | `HealthContextBuilder.kt` 全文件；`FamilyMember.kt`（即 `HealthMetrics.kt:72-104`）无 `lifeStage`/`dietaryRestrictions` 独立字段 | 占用代码库但不产生任何行为，属于应清理的死代码（"复用优先于复制"红线的反例：新写的 `MemberMealFactsBuilder` 若不清理它会让下一个读代码的人误以为已有实现可用） |

---

## §3 不变量

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-K1B-01 | `MemberMealFactsBuilder.build(preview)` 被调用 | 只对 `familyRepo.listMembers()` 中 `careCategoryIds.isNotEmpty()` 的成员产出 `MemberMealFact`；`memberIndex` 取该成员在 `listMembers()` 原始返回顺序中的 1-based 位置（与既有 `healthSafetyReport` 编号口径一致，不因过滤而重新编号——例如原列表 `[无档案, 有档案]`，唯一产出的 `MemberMealFact.memberIndex==2`） | 把 `memberIndex` 按"过滤后列表"重新从 1 编号（会与旧编号口径不一致，且日后若 `healthSafetyReport` 与 `memberFacts` 需要交叉引用会错位——GC-18 索引空间红线） | T-K1B-01a |
| INV-K1B-02 | 计算某成员的 `avoidDishNames` | 只统计该菜**非调料**（`existingId !in ingredientRepo.seasoningIngredientIds()`）且 `existingId` 命中该成员 `AVOID` 级 care 食材（`ingredientRepo.listByCareCategories(member.careCategoryIds).filter{it.adviceLevel==AVOID}.map{it.id}`）的食材；`existingId==null`（全新未入库食材）一律不参与判定 | 把 `existingId==null` 的食材当作"未知即高风险"标记为 avoid（违反"新食材未复核不自动断言忌口"红线，与 K1a `CareFlag.PENDING_REVIEW` 语义一致） | T-K1B-01b |
| INV-K1B-03 | 计算某成员的 `highGiDishNames`/`highPurineDishNames` | 复用 `NutritionLevelEvaluator.dishQualitativeHits(mainNames, conditions, giByName, alreadyFlagged, purineByName)`（`mainNames` 取该菜非调料食材的 `normalizedName` 列表，`alreadyFlagged` 传该菜已判定的 `avoidDishNames` 命中食材名去重）；`conditions` 为空集时两个列表恒空 | 对未登记 `DIABETES`/`GOUT` 的成员也标注高GI/高嘌呤（等同于对其做了未经授权的"这病你该注意"暗示，越权诊断红线） | T-K1B-01c |
| INV-K1B-04 | 计算某成员的 `highSodiumDishNames` | 仅当 `HealthCondition.HYPERTENSION in conditions` 时，对每道菜用 `dish.nutrition` 判定：`nutrition!=null && nutrition.hasData && nutrition.totals.sodiumMg >= NutritionLevelEvaluator.DISH_SODIUM_HIGH_MG` → 命中，直接复用 K1a 已产出的 `DishPreview.nutrition`，不重新计算 | 对 `nutrition==null`（从未算过）或 `hasData==false`（算了没数据）的菜判定为"不高"（沉默地把缺数据等同于安全，误导用户） | T-K1B-01d |
| INV-K1B-05 | 用户点击"查看建议"（`requestHealthAdvice()`） | `state.memberFacts.isEmpty()` 时直接 `return`，不进入 `healthAdviceConsentPending=true`（UI 侧同一字段门控入口按钮是否渲染，见 §4.3） | 对无关注点成员的家庭仍弹出同意框、且发送后 AI 无事实可评只能空话应付（浪费一次网络请求+用户一次决策成本，违反"少操作"准则） | T-K1B-05 |
| INV-K1B-06 | `confirmHealthAdvice()` 收到 AI 成功返回的非空文本 `raw` | 用 `AiMealHealthAdvice.parseMemberSections(raw)` 解析；非空结果 → 写入 `state.memberAdvice`（key=真实成员编号）；解析结果为空 → 写入 `mapOf(0 to raw.trim())`（0 号为"整体兜底"保留位，UI 不显示成员标签，只显示原文） | 解析失败时把 `raw` 直接丢弃、`memberAdvice` 保持空（用户已同意发送、AI 已产出内容，静默丢弃既不透明也浪费） | T-K1B-03（正常解析二成员）、T-K1B-04（无标记兜底） |
| INV-K1B-07 | 构造发给 AI 的 `user` 内容（`AiMealHealthAdvice.request(memberFacts, mealSummary)`） | 只包含 `memberFacts` 的 `tags` + 四类命中菜名列表 + `mealSummary`（菜名摘要，沿用既有 `take(500)` 截断）；每位成员一行、以 `[[M{index}]]` 前缀标记（供 AI 回显同款标记，也供人工核对 prompt 内容） | 包含成员姓名/id/原始输入文本/历史餐食/体检数值（沿用既有红线，未变化，仅结构从"聚合字符串"改为"逐成员分节"） | T-K1B-02a |
| INV-K1B-08（GC-27 缺口修复） | `invalidateGenerationToInput()` 执行 | 同一次 `_state.update` 内一并重置 `memberFacts=emptyList()`、`memberAdvice=emptyMap()`、`healthAdviceConsentPending=false`、`healthAdviceLoading=false`、`healthAdviceError=null`（连同既有 `healthSafetyReport=null`） | 只重置 `healthSafetyReport` 漏掉健康建议四个字段（本批发现的既有缺口，见 §2 D4，必须借本批一并修复，否则新增的 `memberFacts`/`memberAdvice` 会重复同一问题） | T-K1B-06 |

---

## §4 接口契约

### 4.1 逐字段真相源表

| 字段 | 唯一写入者 | 读取方 | 终局形态 |
|---|---|---|---|
| `AiMealInputUiState.memberFacts: List<MemberMealFact>` | `AiMealInputViewModel` 生成流程内一次性写入（preview 成功后，与 `healthSafetyReport` 同一时机）；`invalidateGenerationToInput()` 重置为空 | `AiMealInputSheet.kt`（渲染卡片头部标签+"查看建议"可见性）、`buildHealthSafetyReport()`（派生聚合摘要） | 新增字段，默认 `emptyList()` |
| `AiMealInputUiState.memberAdvice: Map<Int,String>` | `confirmHealthAdvice()` 一次性 `_state.update`（唯一写入者）；`invalidateGenerationToInput()` 重置为空 | `AiMealInputSheet.kt` 逐成员卡片渲染（原 `healthAdvice: String?`） | **替换**（删除 `healthAdvice`，见 §4.2） |
| `MemberMealFact.memberIndex/tags/avoidDishNames/highPurineDishNames/highGiDishNames/highSodiumDishNames` | `MemberMealFactsBuilder.build()`（一次性构造，纯函数式聚合，无后续修改） | `AiMealHealthAdvice.request()`（组装 prompt）、`AiMealInputSheet.kt`（卡片头部标签） | 新增只读数据类，无持久化 |

**`memberIndex` 索引空间说明（GC-18）**：`memberIndex` 是"业务序号"（`familyRepo.listMembers()` 原始顺序的 1-based 位置），不是"过滤后 `memberFacts` 列表的下标"——两者在有成员被过滤（无健康档案）时不相等。`AiMealInputSheet.kt` 渲染时按 `memberFacts` 列表遍历（各元素自带 `memberIndex` 无需换算），不得反过来用列表下标拼编号文案。

### 4.2 GC-11：字段/函数迁移清单

**迁移一：`AiMealInputUiState.healthAdvice: String?` → `memberAdvice: Map<Int,String>`**

旧字段全部写入点（grep 已核对，仅 1 处生产代码）：

| 写入点 | 处置 |
|---|---|
| `AiMealInputViewModel.kt:870`（`confirmHealthAdvice()` 内 `healthAdvice = result.getOrNull()?.trim()?...`） | 改写为 §7 STEP-K1B-6 给出的解析+写入逻辑（见 INV-K1B-06） |

旧字段全部读取点（grep 已核对，仅 1 处生产代码）：

| 读取点 | 处置 |
|---|---|
| `AiMealInputSheet.kt:1014-1021`（`state.healthAdvice?.let{...}` 单卡片渲染） | 整块替换为 §5 给出的逐成员卡片列表渲染 |

**终局裁决**：**替换**（不并存）。理由：新字段完全覆盖旧字段语义（`memberAdvice[0]` 即旧字段的退化形式），并存会产生"AI 建议到底看哪个字段"的歧义。

**迁移二：`healthSummaryLabels()` 私有函数 → 被 `MemberMealFactsBuilder.build()` 取代**

旧函数全部调用点（grep 已核对，仅 1 处）：

| 调用点 | 处置 |
|---|---|
| `AiMealInputViewModel.kt:846`（`buildHealthSafetyReport()` 内部调用） | `buildHealthSafetyReport()` 签名改为接收已算好的 `memberFacts: List<MemberMealFact>` 作参数（不再自行查库），内部把 `healthSummaryLabels()` 的原逻辑替换为 `memberFacts.joinToString("、"){ "成员${it.memberIndex}:${it.tags.joinToString("·")}" }`（见 §7 STEP-K1B-4b） |

`confirmHealthAdvice()` 内对 `healthSummaryLabels()` 的**间接**依赖（`healthSummary` 变量）同步改为直接使用 `state.memberFacts` 构造 `AiMealHealthAdvice.request()`（见 §7 STEP-K1B-6）。

**终局裁决**：**删除**（`healthSummaryLabels()` 私有函数体连同其唯一调用点一并移除，不保留兼容包装——它只有 1 个内部调用点，无对外可见性，删除零风险）。

**迁移三：`DishNutritionLine.kt` 私有常量 `SODIUM_HIGH_PER_DISH_MG` → `NutritionLevelEvaluator.DISH_SODIUM_HIGH_MG`**

| 写入点/定义点 | 处置 |
|---|---|
| `DishNutritionLine.kt:36`（`private const val SODIUM_HIGH_PER_DISH_MG = 667.0`） | 删除该行 |
| `DishNutritionLine.kt:49`（`totals.sodiumMg >= SODIUM_HIGH_PER_DISH_MG`） | 改引用 `NutritionLevelEvaluator.DISH_SODIUM_HIGH_MG`（新增 import） |
| `NutritionLevel.kt`（`NutritionLevelEvaluator` object 内） | 新增 `const val DISH_SODIUM_HIGH_MG = 667.0`（原样值，仅搬迁位置，见 §0.1 GC-26 处置） |

**终局裁决**：**搬迁**（单一定义点从 androidApp UI 层移到 shared 领域层，因为现在有两个层都需要读它：既有 `DishNutritionLine` 与新增 `MemberMealFactsBuilder`）。

**迁移四：删除死代码 `HealthContextBuilder.kt`（`HealthContextBuilder` object + `FamilyMemberHealth` data class）**

grep 已核对：全文件零生产调用点、零测试文件引用（仅自身定义 + 3 篇历史文档提及）。直接删除整个文件，不做兼容包装。

### 4.3 GC-12：UI 判据与业务判据同源表

| UI 表现 | 判据来源 |
|---|---|
| "查看建议"入口按钮渲染 | `state.memberFacts.isNotEmpty()`（与 `requestHealthAdvice()` 内部的门禁判据是同一字段，见 INV-K1B-05） |
| 逐成员建议卡片是否显示 | `state.memberAdvice[fact.memberIndex] != null`（`fact` 遍历自 `state.memberFacts`，两者天然同源，不存在"UI 自己另定义一套成员列表"的第二真相源） |

### 4.4 新增/变更类型与函数签名

```kotlin
// shared/.../domain/NutritionLevel.kt（NutritionLevelEvaluator object 内新增）
object NutritionLevelEvaluator {
    // ... 既有内容不变 ...
    // [AI生成] K1b：单菜钠"偏咸"阈值，从 androidApp DishNutritionLine.kt 私有常量搬迁而来（数值不变，见 GC-26 处置）；
    //   供 UI 层(DishNutritionLine)与 shared 层(MemberMealFactsBuilder)共用同一阈值，避免未来独立调参产生漂移。
    const val DISH_SODIUM_HIGH_MG = 667.0
}
```

```kotlin
// shared/.../ai/meallog/MemberMealHealthFacts.kt（新文件）
package com.sxdbsm.cookbook.ai.meallog

data class MemberMealFact(
    val memberIndex: Int,                    // 1-based，familyRepo.listMembers() 原始顺序位置（见 §4.1 脚注）
    val tags: List<String>,                  // 该成员的年龄段+性别+病种/生命阶段标签（原 healthSummaryLabels 单条内容，不含"成员N:"前缀）
    val avoidDishNames: List<String>,        // 命中该成员忌口的菜名（去重）
    val highPurineDishNames: List<String>,   // 高嘌呤菜名（仅登记痛风才非空）
    val highGiDishNames: List<String>,       // 高GI菜名（仅登记糖尿病才非空）
    val highSodiumDishNames: List<String>,   // 钠偏高菜名（仅登记高血压才非空）
) {
    /** 该成员本次是否有任何值得一提的关联事实（供 UI/prompt 判断是否值得单独占一段）。 */
    val hasConcern: Boolean get() = avoidDishNames.isNotEmpty() || highPurineDishNames.isNotEmpty() ||
        highGiDishNames.isNotEmpty() || highSodiumDishNames.isNotEmpty()
}

class MemberMealFactsBuilder(
    private val familyRepo: com.sxdbsm.cookbook.data.repository.FamilyRepository,
    private val healthRepo: com.sxdbsm.cookbook.data.repository.HealthProfileRepository,
    private val ingredientRepo: com.sxdbsm.cookbook.data.repository.IngredientRepository,
    private val nutritionRepo: com.sxdbsm.cookbook.data.repository.NutritionRepository,
) {
    /** 对预览中每位有健康档案的家庭成员，算出与本次预览餐食相关的本机判定事实。见 INV-K1B-01~04。 */
    suspend fun build(preview: com.sxdbsm.cookbook.domain.autogen.AutoGenPreview): List<MemberMealFact> {
        val members = familyRepo.listMembers()
        val namesById = healthRepo.listAllCrowdTypes().associate { it.id to it.name }
        val seasoningIds = ingredientRepo.seasoningIngredientIds()
        val giByName = nutritionRepo.giByName()
        val purineByName = nutritionRepo.purineByName()
        val dishes = preview.days.flatMap { it.meals }.flatMap { it.dishes }

        return members.mapIndexedNotNull { idx, member ->
            if (member.careCategoryIds.isEmpty()) return@mapIndexedNotNull null
            val careNames = member.careCategoryIds.mapNotNull(namesById::get)
            val tags = careNames.distinct()
            if (tags.isEmpty()) return@mapIndexedNotNull null
            val conditions = careNames.flatMap { com.sxdbsm.cookbook.domain.HealthCondition.fromCareName(it) }.toSet()
            val avoidIds = ingredientRepo.listByCareCategories(member.careCategoryIds)
                .filter { it.adviceLevel == com.sxdbsm.cookbook.domain.model.AdviceLevel.AVOID }
                .map { it.id }.toSet()

            val avoidDishNames = mutableListOf<String>()
            val highPurineDishNames = mutableListOf<String>()
            val highGiDishNames = mutableListOf<String>()
            val highSodiumDishNames = mutableListOf<String>()

            dishes.forEach { dish ->
                val nonSeasoning = dish.ingredients.filter { it.existingId == null || it.existingId !in seasoningIds }
                val avoidHitNames = nonSeasoning.filter { it.existingId != null && it.existingId in avoidIds }.map { it.normalizedName }
                if (avoidHitNames.isNotEmpty()) avoidDishNames += dish.inputName

                val (gi, purine) = com.sxdbsm.cookbook.domain.NutritionLevelEvaluator.dishQualitativeHits(
                    mainNames = nonSeasoning.map { it.normalizedName },
                    conditions = conditions,
                    giByName = giByName,
                    alreadyFlagged = avoidHitNames.toSet(),
                    purineByName = purineByName,
                )
                if (gi.isNotEmpty()) highGiDishNames += dish.inputName
                if (purine.isNotEmpty()) highPurineDishNames += dish.inputName

                if (com.sxdbsm.cookbook.domain.HealthCondition.HYPERTENSION in conditions) {
                    val n = dish.nutrition
                    if (n != null && n.hasData && n.totals.sodiumMg >= com.sxdbsm.cookbook.domain.NutritionLevelEvaluator.DISH_SODIUM_HIGH_MG) {
                        highSodiumDishNames += dish.inputName
                    }
                }
            }

            MemberMealFact(
                memberIndex = idx + 1,
                tags = tags,
                avoidDishNames = avoidDishNames.distinct(),
                highPurineDishNames = highPurineDishNames.distinct(),
                highGiDishNames = highGiDishNames.distinct(),
                highSodiumDishNames = highSodiumDishNames.distinct(),
            )
        }
    }
}
```

```kotlin
// shared/.../ai/meallog/AiMealHealthAdvice.kt（重写）
package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.ai.LlmRequest

object AiMealHealthAdvice {
    fun request(memberFacts: List<MemberMealFact>, mealSummary: String): LlmRequest {
        val factsBlock = memberFacts.joinToString("\n") { f ->
            val hits = buildList {
                if (f.avoidDishNames.isNotEmpty()) add("忌口命中：${f.avoidDishNames.joinToString("、")}")
                if (f.highPurineDishNames.isNotEmpty()) add("高嘌呤：${f.highPurineDishNames.joinToString("、")}")
                if (f.highGiDishNames.isNotEmpty()) add("高GI：${f.highGiDishNames.joinToString("、")}")
                if (f.highSodiumDishNames.isNotEmpty()) add("钠偏高：${f.highSodiumDishNames.joinToString("、")}")
            }
            val hitsText = if (hits.isEmpty()) "本餐无命中事实" else hits.joinToString("；")
            "[[M${f.memberIndex}]] 标签：${f.tags.joinToString("·")}；$hitsText"
        }
        return LlmRequest(
            system = """
你是家庭饮食记录助手。系统已按本机健康规则算出每位家庭成员与本次餐食的关联事实（下方"家庭成员事实"部分），
你只需要把这些事实转述成简短、温和、非诊疗的建议，不得编造事实之外的病情或饮食禁忌，不得添加未在事实中出现的菜品或食材判断。
不得声称治疗、诊断、替代医生意见；不要复述或猜测姓名、身份、病史、用药、检查结果。
每位成员单独输出一段，段首必须是形如 [[M1]] 的标记（数字与"家庭成员事实"里的成员编号一一对应），
其后写 1~2 句建议；若该成员"本餐无命中事实"，就写"本餐没有需要特别关注的地方"。
不要在任何一段末尾添加免责声明，也不要输出与 [[M数字]] 无关的其他文字。
输出纯中文，不使用列表符号、不使用 Markdown。
            """.trimIndent(),
            user = "家庭成员事实：\n$factsBlock\n\n本餐摘要：$mealSummary",
            temperature = 0.2,
            maxTokens = 200 * memberFacts.size.coerceAtLeast(1) + 100,
        )
    }

    /** 按 [[M{数字}]] 标记切分 AI 原始返回；无法识别任何标记时返回空 map（调用方负责兜底，见 INV-K1B-06）。 */
    fun parseMemberSections(raw: String): Map<Int, String> {
        val regex = Regex("""\[\[M(\d+)\]\]""")
        val matches = regex.findAll(raw).toList()
        if (matches.isEmpty()) return emptyMap()
        return matches.mapIndexed { i, m ->
            val start = m.range.last + 1
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else raw.length
            m.groupValues[1].toIntOrNull() to raw.substring(start, end).trim()
        }.filter { it.first != null }.associate { it.first!! to it.second }
    }
}
```

```kotlin
// androidApp/.../ui/ai/AiMealInputViewModel.kt（关键变更）
class AiMealInputViewModel(
    private val initialText: String,
    targetDate: LocalDate,
    private val aiRuntime: AiRuntime,
    private val config: AiRuntimeConfig,
    private val recorder: MultiDayRecorder,
    private val ingredientRepo: IngredientRepository,
    private val healthRepo: HealthProfileRepository,
    private val familyRepo: FamilyRepository,
    private val memberFactsBuilder: com.sxdbsm.cookbook.ai.meallog.MemberMealFactsBuilder, // 新增
) : ViewModel() { /* ... */ }

data class AiMealInputUiState(
    // ... 既有字段不变 ...
    val healthSafetyReport: HealthSafetyReport? = null,
    val memberFacts: List<com.sxdbsm.cookbook.ai.meallog.MemberMealFact> = emptyList(), // 新增
    val healthAdviceConsentPending: Boolean = false,
    val healthAdviceLoading: Boolean = false,
    val memberAdvice: Map<Int, String> = emptyMap(), // 原 healthAdvice: String?
    val healthAdviceError: String? = null,
    val engineLabel: String = "",
)
```

### 4.5 挂起点清单（GC-31）

| 挂起点 | 恢复后身份重校验 |
|---|---|
| `memberFactsBuilder.build(preview)`（生成流程内，紧邻既有 `buildHealthSafetyReport` 调用之前） | 挂起返回后先 `if (!isCurrentGeneration(generationId)) return`（与既有 `buildHealthSafetyReport` 挂起点同一模式，见 §7 STEP-K1B-4a），旧 generation 的 `memberFacts` 结果不得写入 state |
| `aiRuntime.complete(AiMealHealthAdvice.request(...))`（`confirmHealthAdvice()` 内） | 沿用既有代码：该挂起点**不**受 generation 隔离（健康建议本就是"预览已 READY 之后"的独立会话内动作，不属于流式 generation 竞态范畴，与 K1a 蓝图 GC-31 对 `refreshConfigReady()` 的论证同理——不存在"新 generation 已开始、旧建议结果覆盖新预览"的场景，因为按 INV-K1B-05 只有预览存在时才可发起） |

---

## §5 UI 设计

复用范围内，无新交互范式，Apple-UX 门禁豁免理由：本批全部是"既有卡片/按钮/弹窗范式的内容细化"（单卡片→多卡片列表，仍是同一视觉语言：`Card`+`Column`+`labelLarge`/`bodySmall`），未引入新组件、新布局形态、新配色，符合 CLAUDE.md 门禁豁免条件"复用已确立 §九 模式的同类小改"。

- "查看建议"按钮：`enabled=!state.healthAdviceLoading`，仅当 `state.memberFacts.isNotEmpty()` 时渲染（连同其上下文一起，整个健康建议区块在无关注成员的家庭里完全不出现，比"出现但按钮置灰"更克制）。
- 逐成员卡片：每张卡片头部 `Text("成员${fact.memberIndex}·${fact.tags.joinToString("·")}", labelLarge)`，下方 `Text(建议文本, bodySmall)`；`memberAdvice[0]`（无标记兜底）单独渲染为一张不带成员头部的卡片，标题沿用"本次建议"。
- 全部卡片下方保留一行 `Text("仅供参考，非医嘱", ...)`（原逻辑，条件从"`healthAdvice!=null`"改为"`memberAdvice.isNotEmpty()`"）。
- 同意弹窗文案（`healthAdviceConsentPending`）更新为：

  > "将仅发送本机判定的健康关注事实（如某道菜含忌口食材/嘌呤或GI偏高/钠偏高）、健康档案标签和菜名摘要，为每位设有健康关注点的家庭成员分别生成建议；不发送姓名、原始输入、病史、体征、报告、用药或历史餐食。建议只在本次确认页展示，关闭后清除。"

  （比原文案更具体披露"发送的是本机判定事实"而非笼统的"标签"，符合透明准则"做了什么"要素；未设健康关注点的成员不参与，本身已由入口不可见传达，不在弹窗内重复说明以免冗长）。

---

## §6 文件改动清单 + Allowlist

| 文件 | 允许操作 | 禁止操作 |
|---|---|---|
| `shared/.../domain/NutritionLevel.kt` | `NutritionLevelEvaluator` object 内新增 `DISH_SODIUM_HIGH_MG` 常量 | 改其余既有常量/函数逻辑 |
| 新建 `shared/.../ai/meallog/MemberMealHealthFacts.kt` | 新建（`MemberMealFact` + `MemberMealFactsBuilder`） | — |
| 删除 `shared/.../ai/meallog/HealthContextBuilder.kt` | 整文件删除 | — |
| `shared/.../ai/meallog/AiMealHealthAdvice.kt` | 重写 `request()` 签名+实现；新增 `parseMemberSections()` | 改 `LlmRequest` 类型本身 |
| `shared/.../di/SharedModule.kt` | 新增 `single { MemberMealFactsBuilder(get(), get(), get(), get()) }` | 改其余既有绑定 |
| `androidApp/.../di/AndroidModule.kt` | `AiMealInputViewModel` 工厂调用新增一个 `get()` 参数 | 改其余既有绑定 |
| `androidApp/.../ui/ai/AiMealInputViewModel.kt` | 构造器新增 `memberFactsBuilder` 参数；`AiMealInputUiState` 字段迁移（见 §4.2）；`buildHealthSafetyReport()` 签名变更；`requestHealthAdvice()`/`confirmHealthAdvice()` 重写；`invalidateGenerationToInput()` 补齐重置字段；删除 `healthSummaryLabels()` | 改 `AiMealPhase`/`submit()`/流式生成主链路逻辑；改与本批无关的既有字段 |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | 健康建议展示区块（原单卡片渲染）替换为逐成员卡片列表；同意弹窗文案更新 | 改 `PreviewPhase`/`GeneratingPhase`/`ErrorPhase` 其他渲染逻辑 |
| `androidApp/.../ui/component/DishNutritionLine.kt` | 删除私有 `SODIUM_HIGH_PER_DISH_MG`，改引用 `NutritionLevelEvaluator.DISH_SODIUM_HIGH_MG` | 改其余渲染逻辑/阈值语义 |
| 新建 `shared/.../ai/meallog/MemberMealHealthFactsTest.kt` | 新建（T-K1B-01a~d） | — |
| 新建/追加 `shared/.../ai/meallog/AiMealHealthAdviceTest.kt`（若已存在则追加，若不存在则新建） | 新增 T-K1B-02a/b | 不删改与本批无关的既有断言（若文件已存在） |
| 新建 `androidApp/.../ui/ai/AiMealInputViewModelHealthAdviceTest.kt` | 新建，覆盖 T-K1B-03~06 | 不修改 `AiMealInputViewModelStreamTest.kt`（沿用 K1a 同款隔离纪律） |

**显式禁改文件清单**：
- `shared/.../ai/meallog/StreamingMealSession.kt`、`GenerationProgress.kt`、`SegmentProgressBar.kt`、`AiMealPrompt.kt`、`UnifiedMealSchema.kt`（与本批无关的流式解析主链路）
- `shared/.../domain/autogen/*`（K1a 已 `ACCEPTED` 冻结区，本批只读取 `DishPreview.nutrition`/`IngredientPreview.existingId` 等既有字段，不改其结构）
- `shared/.../ai/HealthRuleEngine.kt`、`MealHealthHintUseCase.kt`（推荐引擎/记菜后轻提示，与预览态健康评价是不同应用场景，本批不改，只读用其中的 `AdviceLevel`/`HealthCondition` 等既有类型）
- `androidApp/.../ui/ai/AiMealInputViewModel.kt` 内 `submit()`/`handleSessionSnapshot()`/流式相关函数体（本批只涉及 preview 成功之后的健康建议分支）

**回归基线锁定（GC-09，本批完成后必须仍为绿）**：
- `:shared:testDebugUnitTest`（全量，0 failures）
- `:androidApp:testDebugUnitTest --tests "*.AiMealInputViewModelStreamTest"`（现有条数，零改动仍全绿）
- `:androidApp:testDebugUnitTest --tests "*.GenerationProgressTest"`
- `:androidApp:assembleDebug`

---

## §7 分阶段实施步骤

### 批 K1B-1：本机事实层

**STEP-K1B-1.1**：`shared/.../domain/NutritionLevel.kt` `NutritionLevelEvaluator` object 内新增 `const val DISH_SODIUM_HIGH_MG = 667.0`。
完成形态：`grep "const val DISH_SODIUM_HIGH_MG = 667.0" NutritionLevel.kt` 命中 1 处。

**STEP-K1B-1.2**：`androidApp/.../ui/component/DishNutritionLine.kt` 删除 `SODIUM_HIGH_PER_DISH_MG` 私有常量定义，`:49` 改引用 `NutritionLevelEvaluator.DISH_SODIUM_HIGH_MG`，新增 import。
完成形态：`grep "SODIUM_HIGH_PER_DISH_MG" DishNutritionLine.kt` 零命中；`grep "NutritionLevelEvaluator.DISH_SODIUM_HIGH_MG" DishNutritionLine.kt` 命中 1 处。

**STEP-K1B-1.3**：新建 `shared/.../ai/meallog/MemberMealHealthFacts.kt`，按 §4.4 完整实现 `MemberMealFact` + `MemberMealFactsBuilder`。
完成形态：`grep "class MemberMealFactsBuilder" MemberMealHealthFacts.kt` 命中 1 处；`grep "data class MemberMealFact" MemberMealHealthFacts.kt` 命中 1 处。

**STEP-K1B-1.4**：删除 `shared/.../ai/meallog/HealthContextBuilder.kt` 整文件。
完成形态：`grep -r "HealthContextBuilder\|FamilyMemberHealth" shared/src/` 零命中（除本蓝图/历史文档外）。

**STEP-K1B-1.5**：`shared/.../di/SharedModule.kt` 新增 `single { com.sxdbsm.cookbook.ai.meallog.MemberMealFactsBuilder(get(), get(), get(), get()) }`（紧邻既有 `MealHealthHintUseCase` 绑定附近）。
完成形态：`grep "MemberMealFactsBuilder(get(), get(), get(), get())" SharedModule.kt` 命中 1 处。

### 批 K1B-2：AI 请求契约改造

**STEP-K1B-2.1**：`shared/.../ai/meallog/AiMealHealthAdvice.kt` 按 §4.4 完整重写。
完成形态：`grep "fun request(memberFacts: List<MemberMealFact>" AiMealHealthAdvice.kt` 命中 1 处；`grep "fun parseMemberSections" AiMealHealthAdvice.kt` 命中 1 处。

### 批 K1B-3：ViewModel 接入

**STEP-K1B-3.1**：`AiMealInputViewModel.kt` 构造器新增 `private val memberFactsBuilder: com.sxdbsm.cookbook.ai.meallog.MemberMealFactsBuilder` 参数；`AndroidModule.kt` 对应工厂调用追加一个 `get()`。
完成形态：`grep "memberFactsBuilder: com.sxdbsm.cookbook.ai.meallog.MemberMealFactsBuilder" AiMealInputViewModel.kt` 命中 1 处；`grep "AiMealInputViewModel(initialText, targetDate, get(), get(), get(), get(), get(), get(), get())" AndroidModule.kt` 命中 1 处。

**STEP-K1B-3.2**：`AiMealInputUiState` 按 §4.4 迁移字段：`healthAdvice: String?` 删除，新增 `memberFacts: List<MemberMealFact> = emptyList()` 与 `memberAdvice: Map<Int,String> = emptyMap()`。
完成形态：`grep "val healthAdvice: String?" AiMealInputViewModel.kt` 零命中；`grep "val memberFacts: List<" AiMealInputViewModel.kt` 命中 1 处；`grep "val memberAdvice: Map<Int, String>" AiMealInputViewModel.kt` 命中 1 处。

**STEP-K1B-3.3**：既有生成流程内、原 `buildHealthSafetyReport(preview)` 调用处（`:706-710` 附近），紧邻其前插入：
```kotlin
val memberFacts = runCatching {
    withContext(kotlinx.coroutines.NonCancellable) { memberFactsBuilder.build(preview) }
}.getOrDefault(emptyList())
if (!isCurrentGeneration(generationId)) return
```
`buildHealthSafetyReport` 调用改为 `buildHealthSafetyReport(preview, memberFacts)`；`_state.update` 内新增 `memberFacts = memberFacts,`（紧邻既有 `healthSafetyReport = safetyReport,`）。
完成形态：`grep "memberFactsBuilder.build(preview)" AiMealInputViewModel.kt` 命中 1 处；`grep "buildHealthSafetyReport(preview, memberFacts)" AiMealInputViewModel.kt` 命中 1 处。

**STEP-K1B-3.4**：`buildHealthSafetyReport()` 签名与实现按 §4.2 迁移二改写：
```kotlin
private fun buildHealthSafetyReport(
    preview: AutoGenPreview,
    memberFacts: List<com.sxdbsm.cookbook.ai.meallog.MemberMealFact>,
): HealthSafetyReport {
    val pendingIngredients = preview.days.flatMap { it.meals }.flatMap { it.dishes }
        .flatMap { it.ingredients }.count { it.careFlag == com.sxdbsm.cookbook.domain.autogen.CareFlag.PENDING_REVIEW }
    return HealthSafetyReport(buildList {
        if (memberFacts.isNotEmpty()) add(
            "已结合健康档案：" + memberFacts.joinToString("、") { "成员${it.memberIndex}:${it.tags.joinToString("·")}" }
        )
        if (pendingIngredients > 0) add("本餐有 $pendingIngredients 种新食材，营养和适宜性待复核")
        if (isEmpty()) add("未设置健康档案；可按个人情况核对本餐")
    })
}
```
删除原 `healthSummaryLabels()` 私有函数体。函数不再是 `suspend`（不再自行查库）。
完成形态：`grep "private fun healthSummaryLabels" AiMealInputViewModel.kt` 零命中；`grep "private fun buildHealthSafetyReport(\s*$" AiMealInputViewModel.kt` 或 `grep "memberFacts: List<com.sxdbsm.cookbook.ai.meallog.MemberMealFact>," AiMealInputViewModel.kt` 命中 1 处。

**STEP-K1B-3.5**：`requestHealthAdvice()` 开头新增门禁：
```kotlin
fun requestHealthAdvice() {
    if (_state.value.autoGenPreview == null || _state.value.healthAdviceLoading) return
    if (_state.value.memberFacts.isEmpty()) return
    _state.update { it.copy(healthAdviceConsentPending = true, healthAdviceError = null) }
}
```
完成形态：`grep "if (_state.value.memberFacts.isEmpty()) return" AiMealInputViewModel.kt` 命中 1 处。

**STEP-K1B-3.6**：`confirmHealthAdvice()` 按 INV-K1B-06/07 重写：
```kotlin
fun confirmHealthAdvice() {
    val memberFacts = _state.value.memberFacts
    if (memberFacts.isEmpty()) { _state.update { it.copy(healthAdviceConsentPending = false) }; return }
    val preview = _state.value.autoGenPreview ?: return
    _state.update { it.copy(healthAdviceConsentPending = false, healthAdviceLoading = true, healthAdviceError = null) }
    viewModelScope.launch {
        val mealSummary = preview.days.flatMap { it.meals }.flatMap { it.dishes }
            .joinToString("、") { it.inputName }.take(500)
        val result = aiRuntime.complete(AiMealHealthAdvice.request(memberFacts, mealSummary))
        val raw = result.getOrNull()?.trim()
        val parsed = raw?.takeIf(String::isNotBlank)?.let { text ->
            AiMealHealthAdvice.parseMemberSections(text).ifEmpty { mapOf(0 to text) }
        } ?: emptyMap()
        _state.update {
            it.copy(
                healthAdviceLoading = false,
                memberAdvice = parsed,
                healthAdviceError = result.exceptionOrNull()?.message?.take(120)
                    ?: if (parsed.isEmpty()) "暂时无法生成建议" else null,
            )
        }
    }
}
```
完成形态：`grep "AiMealHealthAdvice.parseMemberSections" AiMealInputViewModel.kt` 命中 1 处；`grep "mapOf(0 to text)" AiMealInputViewModel.kt` 命中 1 处。

**STEP-K1B-3.7**（INV-K1B-08，GC-27 缺口修复）：`invalidateGenerationToInput()` 的 `_state.update { prev -> prev.copy(...) }` 内，紧邻既有 `healthSafetyReport = null,` 一行，新增：
```kotlin
memberFacts = emptyList(),
memberAdvice = emptyMap(),
healthAdviceConsentPending = false,
healthAdviceLoading = false,
healthAdviceError = null,
```
完成形态：`grep -A6 "healthSafetyReport = null," AiMealInputViewModel.kt` 输出包含以上 5 行（在 `invalidateGenerationToInput` 函数体内，非生成流程内的另一处 `healthSafetyReport = safetyReport,` 附近）。

### 批 K1B-4：UI 展示切换

**STEP-K1B-4.1**：`AiMealInputSheet.kt` 原 `:1014-1021`（`state.healthAdvice?.let{...}` 块）替换为 §5 给出的逐成员卡片列表渲染；"查看建议" `TextButton` 增加 `state.memberFacts.isNotEmpty()` 条件包裹。
完成形态：`grep "state.healthAdvice" AiMealInputSheet.kt` 零命中；`grep "state.memberFacts.isNotEmpty()" AiMealInputSheet.kt` 命中 ≥1 处；`grep "state.memberAdvice\[" AiMealInputSheet.kt` 命中 ≥1 处。

**STEP-K1B-4.2**：同意弹窗（`:1082-1090` 附近）`text` 文案替换为 §5 给出的新文案。
完成形态：`grep "为每位设有健康关注点的家庭成员分别生成建议" AiMealInputSheet.kt` 命中 1 处。

### 批 K1B-T：测试

**STEP-K1B-T-1**：新建 `MemberMealHealthFactsTest.kt`，覆盖 T-K1B-01a~d（见 §8.2）。

**STEP-K1B-T-2**：`AiMealHealthAdviceTest.kt` 新增/追加 T-K1B-02a/b。

**STEP-K1B-T-3**：新建 `AiMealInputViewModelHealthAdviceTest.kt`，覆盖 T-K1B-03~06。

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
| shared 测试用 `FakeFamilyRepository`/`FakeHealthProfileRepository`/`FakeIngredientRepository`/`FakeNutritionRepository`（若未存在则新建最小 fake） | 按测试用例预设固定返回值（成员列表/care 分类名/avoid 食材/gi&purine 映射） | 内部做业务判断逻辑（判断逻辑必须留在 `MemberMealFactsBuilder` 生产代码里被测试，不能测试夹具自己算好答案再原样返回） |
| androidApp 测试用现有 fake `AiRuntime` | 记录 `complete()` 入参、返回预设字符串（含"格式良好"与"无标记"两类样本） | — |

### 8.2 INV↔T 双向映射表

| INV | T-ID | 断言要点 |
|---|---|---|
| INV-K1B-01 | T-K1B-01a | 3 名成员（1 有 careCategoryIds、1 无、1 有）→ `build()` 返回长度 2 的列表，`memberIndex` 分别为 1、3（原始顺序位置，非重新编号） |
| INV-K1B-02 | T-K1B-01b | 某成员 avoid 命中一个调料食材（`existingId in seasoningIds`）→ 不计入 `avoidDishNames`；命中一个 `existingId==null` 的新食材 → 不计入；命中一个非调料+有 id+在 avoid 集合的食材 → 计入 |
| INV-K1B-03 | T-K1B-01c | 成员未登记糖尿病但菜品含高GI主料 → `highGiDishNames` 为空；登记糖尿病且命中 → 非空 |
| INV-K1B-04 | T-K1B-01d | 成员登记高血压：`dish.nutrition=null` → 不计入；`hasData=false` → 不计入；`sodiumMg` 恰等于 `DISH_SODIUM_HIGH_MG` → 计入（`>=`边界） |
| INV-K1B-07 | T-K1B-02a | `AiMealHealthAdvice.request()` 构造的 `user` 字符串包含 `[[M1]]`/成员标签/命中菜名，不包含任何成员姓名（fake `MemberMealFact.tags` 用占位标签，断言姓名字符串不在输出中） |
| INV-K1B-06 | T-K1B-02b | `parseMemberSections("[[M1]] 建议A[[M2]] 建议B")` 返回 `{1:"建议A", 2:"建议B"}`；`parseMemberSections("没有标记的纯文本")` 返回空 map |
| INV-K1B-06 | T-K1B-03 | fake `aiRuntime.complete()` 返回 `"[[M1]] 少吃咸菜[[M2]] 本餐没有需要特别关注的地方"`，`state.memberFacts` 含编号 1、2 → `confirmHealthAdvice()` 后 `state.memberAdvice == {1:"少吃咸菜", 2:"本餐没有需要特别关注的地方"}` |
| INV-K1B-06 | T-K1B-04 | fake 返回不含任何 `[[M数字]]` 标记的纯文本 → `state.memberAdvice == {0: <该文本 trim 后>}` |
| INV-K1B-05 | T-K1B-05 | `state.memberFacts` 为空时调用 `requestHealthAdvice()` → `state.healthAdviceConsentPending` 保持 `false` |
| INV-K1B-08 | T-K1B-06 | 先 `confirmHealthAdvice()` 产出非空 `memberAdvice`，再触发一次 `invalidateGenerationToInput`（如重新提交/清空输入路径）→ `state.memberFacts==emptyList()`、`state.memberAdvice==emptyMap()`、`state.healthAdviceError==null` |
| INV-K1B-01~04 | 真机 E-K1B-01 | 家庭档案至少 2 名有健康关注点成员，AI 记一餐预览含忌口/高钠菜品 → 点"查看建议"、同意后，看到对应成员各自的卡片，内容与本机"健康提示"区块的事实描述一致 |

---

## §9 交付台账（CODE 完成时填）

### STEP 勾销表

| STEP-ID | 状态 | 落地 commit | diff 定位 |
|---|---|---|---|
| STEP-K1B-1.1~1.5 | ⬜ | | |
| STEP-K1B-2.1 | ⬜ | | |
| STEP-K1B-3.1~3.7 | ⬜ | | |
| STEP-K1B-4.1~4.2 | ⬜ | | |
| STEP-K1B-T-1~3 | ⬜ | | |

### 验收命令输出

（交付时填）

### 真机待验证登记

（交付时登记 `E-K1B-01` 至时间戳最新的 `真机待验证清单_<yyyyMMddHHmm>.md`）

---

## §10 独立挑战台账（GC-37）

**挑战方**：独立 Explore/opus 挑战 agent（只读，未见起草过程，只给蓝图成文 + 逐条核对当前真实源码）。**结果：11 项 CONFIRMED-ISSUE（阻断）+ 9 项 MINOR-NIT + 9 项 CONFIRMED-FINE**。**本蓝图已 PARKED，以下问题未处置，留档待拾起时按序处理，处置完毕才可转 `BLUEPRINT_READY`**：

| # | 挑战项摘要 | 裁决 |
|---|---|---|
| 1 | `AiMealInputViewModel` 新增无默认值构造参数会让 §6 列为"禁改+须全绿"的 `AiMealInputViewModelStreamTest.kt` 编译打不过（该文件具名构造 VM，未传新参） | CONFIRMED-ISSUE |
| 2 | 删除 `healthSummaryLabels()` 丢了 `legacy = healthRepo.listAll()`（旧版 `user_health_profile`）分支，只用家庭成员档案的 `MemberMealFactsBuilder` 会让这类老用户的"查看建议"入口整个消失（功能回归，非重构） | CONFIRMED-ISSUE |
| 3 | STEP-K1B-4.1 完成形态 `grep "state.healthAdvice" 零命中` 是永远无法满足的判据——`healthAdviceLoading`/`healthAdviceError`/`healthAdviceConsentPending` 等 4 处不删的字段都含该子串 | CONFIRMED-ISSUE |
| 4 | INV-K1B-04 必须结果与禁止结果自相矛盾：`MemberMealFact` 无"数据缺失"第三态，缺数据的菜必然落入"不高"，而禁止结果明写"不得判定为不高"；T-K1B-01d 正在断言被禁止的行为 | CONFIRMED-ISSUE |
| 5 | 钠判据与 `DishNutritionLine.toDishNutritionUi()` 不同源：UI 侧门控是 `hasData && energyKcal>0.0`，§4.4 只判 `hasData`，同一道"有料缺量"的菜会在预览卡片显"营养待完善"、逐成员建议却说"钠偏高"，自相矛盾 | CONFIRMED-ISSUE |
| 6 | §4.5"健康建议协程不需要 generation 隔离"论证错误：`confirmHealthAdvice()` 用裸 `viewModelScope.launch`，不受 `invalidateGenerationToInput()` 的 `generationJob?.cancel()` 管辖；用户点"查看建议"→加载中点"修改"清空预览→旧协程完成后把**上一餐**的逐成员建议写回新预览，`memberIndex` 还能碰巧对上，UI 无法分辨 | CONFIRMED-ISSUE（本批最实的逻辑洞） |
| 7 | `parseMemberSections` 解析出的 key 集合与 `memberFacts` 的 `memberIndex` 集合不要求交集非空；AI 把编号理解成"过滤后序号"（跳号编号本身诱导误解）会导致某成员卡片静默空白、`healthAdviceError` 仍为 null——正是 INV-K1B-06 明文禁止的"静默丢弃" | CONFIRMED-ISSUE |
| 8 | `giByName()`/`purineByName()` 是全表扫描，`dishQualitativeHits` 文档要求"不需要就传空 map 省查询"，§4.4 无条件调用；触发点在每个流式段边界，周期记 7 天场景成本被放大数倍 | CONFIRMED-ISSUE（效率/契约） |
| 9 | `memberFactsBuilder.build()` 异常时静默降级为 `emptyList()`，导致整个健康建议区块（含入口按钮）消失，且"DB 异常"与"这家人没设关注点"两种情况不可区分，未入 GC-20 自动副作用清单 | CONFIRMED-ISSUE |
| 10 | §5"仅供参考·非医嘱"一行的"原逻辑"描述失实：该行实际在 `healthSafetyReport` 卡片内，与旧 `healthAdvice` 无关；新的逐成员建议卡片当前设计里没有这行，属医疗免责红线，不能含糊 | CONFIRMED-ISSUE |
| 11 | §4.2 GC-11 迁移表称 `healthSummaryLabels()` "仅 1 处调用"为假，实际 2 处（`:842`/`:863`），且行号引用有误（`:846`不是调用行） | CONFIRMED-ISSUE（GC-11 完整性） |
| 12 | 删 `healthSummaryLabels()` 后 `healthRepo`/`familyRepo` 在 VM 构造器成为死参数（全部用处都在该函数里） | MINOR-NIT |
| 13 | `MemberMealFact.tags` 的 KDoc 写"年龄段+性别"，实现只填 care 分类名，未读 `FamilyMember.age`/`gender` | MINOR-NIT |
| 14 | `MemberMealFact.hasConcern` 定义后全文零引用，出生即死代码 | MINOR-NIT |
| 15 | `parseMemberSections` 未定义重复标记（后者覆盖前者静默丢失）与相邻标记（产出空串卡片）两个边角值域 | MINOR-NIT |
| 16 | 个人忌口（`avoidCategoryIds`/`avoidIngredientIds`）未覆盖：① 弹窗文案"忌口命中"会让用户误以为包含个人忌口；② 只设个人忌口、未设病种的成员被 INV-K1B-01 的 `careCategoryIds.isNotEmpty()` 闸门排除在外，未登记进 §12 弃置项 | MINOR-NIT |
| 17 | WEEK 模式下 7 天预览被拍平成"本餐"措辞（prompt/UI 文案都写"本餐"），语义失真在健康断言语境被放大；`maxTokens` 按成员数算不按内容量算 | MINOR-NIT |
| 18 | `buildHealthSafetyReport` 改非 suspend 后，外层 `runCatching{withContext(NonCancellable){...}}` 包裹变成无意义的死防御外壳，STEP 未提示清理 | MINOR-NIT |
| 19 | `confirmHealthAdvice()` 无 `healthAdviceLoading` 守卫（`requestHealthAdvice()` 有），快速双击可并发两个协程、后完成覆盖先完成（既有缺陷，借本批一并修可以但蓝图未提） | MINOR-NIT |
| 20 | §1.2"不扩大风险面"措辞不成立：逐成员事实把"病种标签"升级为"个体饮食行为-疾病关联推断"，敏感度确实上升，但升级后的 §5 弹窗文案同层覆盖了这个增量，结论（L1 不阻塞）仍站得住，只是论证措辞需要订正为"扩大了粒度与敏感度，故同步升级功能级告知覆盖增量" | MINOR-NIT（结论保留，措辞必须改） |
| 21 | §2 D5"仅 3 篇历史文档提及"应为 4 篇 | MINOR-NIT |
| 22~30 | `memberIndex` 索引空间设计正确、`IngredientPreview` 确无 `isMain`、全部引用的函数签名/包名与现实一致、`HealthContextBuilder` 确系零调用死代码、`SODIUM_HIGH_PER_DISH_MG` 搬迁裁决正确、Koin 参数个数正确、§2 证据行号（除 #11 一处）精度很高、sticky 字段重置点排查无遗漏（真正的洞是 #6"清空后被异步回填"而非"漏了重置点"）、`tags` 不泄漏用户自定义命名 | CONFIRMED-FINE，无需改动 |

**结论**：蓝图代码核实精度高（行号/签名/字段/Koin 参数个数几乎全对），但存在 11 项阻断，其中 #1/#3 是"照做就编译失败/判据永远不满足"级别，#6 是最实的逻辑洞（旧建议异步复活）。**下次拾起本蓝图时，先处置 #1~#11（须在文本层给出唯一动作，不能留判断给 CODE），再吸收 #12/#13/#16/#20 等 MINOR-NIT，方可转 `BLUEPRINT_READY`。**

---

## §11 门禁与角色

- 本批 UI 改动全部为"既有卡片/按钮/弹窗范式的内容细化"，豁免 Apple-UX 设计 agent 前置门禁（见 §5 说明）。
- CODE 完成、构建+单测通过后，仍须走 `google_quality_engineer` 代码质量终审（项目强制门禁，无豁免条件）。
- 同意弹窗新文案字数增加但无新术语、无医疗断言升级，豁免 `copywriter` 专项审校，但仍需符合文案准则（已在 §5 自查："仅供参考·非医嘱"红线未变、鼓励非责备语气未变）。
- 涉及健康判定新增计算路径（`MemberMealFactsBuilder`），按项目"权威方法论优先准则"核查：本批**未新增判定口径**，全部复用已核准的既有引擎（`AdviceLevel`/`HealthCondition.fromCareName`/`NutritionLevelEvaluator.dishQualitativeHits`/`DISH_SODIUM_HIGH_MG` 均为既有权威口径来源，见这些定义处已标注的国标/指南依据），不触发"新算法先查权威资料"的前置调研步骤。

## §12 弃置项登记（GC-03 前瞻）

| 项 | 状态 | 归宿 |
|---|---|---|
| 忌口/嘌呤/GI 事实计算的"仅主料"精度（当前退化为"全部非调料食材"） | 显式弃置 | 需扩展 `AutoGenModels.kt` 的 `IngredientPreview` 补充 `isMain` 字段，涉及 K1a 已冻结区域，独立蓝图评估 |
| 无健康档案成员的通用饮食提示 | 显式弃置 | 独立产品方案讨论（内容边界需先定义，避免滑向泛泛而谈或过度医疗化措辞） |
| 成员维度跨餐食/跨日期累积健康评价 | 显式弃置 | 与 J17（一周计划营养线统一算法）方向相关，留给该批次或独立方案 |
| L1 合规闸门（App 级云端 AI 首次启用弹窗） | 不属于本批弃置项，是独立并行待办 | `待办_工程合规.md` L1，保持 🔴 优先级不降 |
