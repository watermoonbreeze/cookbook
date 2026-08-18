# F-AI-MEAL · AI 快捷记一餐

> 全景图纵轴：单功能深度详情。跨功能机制/算法/架构/约定见横轴册（`01/02/04/06/20/21/22`），本文件只放该功能独有内容，跨功能内容放摘要+链接，不重复权威内容。
> Project Graph 对应：`.ai-context/project_graph/features/F-AI-MEAL.yaml`（`lifecycle: active`，Project Graph 的 PoC 样例功能，全实体演示）
> **当前项目主线之一**：本功能是"AI 与网络请求策略"（`21`）册的主要案例来源。

## 目标
AI 主导解析用户输入（文字/语音）生成餐食记录，双阶段：阶段一 AI 主导宽松结构化解析、本地规范化入库；阶段二经单次同意生成健康建议，只在确认页展示、不持久化（`08_决策记录.md` D-13/D-14）。

## 现状
- ✅ **AI 记餐长输入改造（NDJSON 流式）已实现，K1g 真机验证中**：快速记 ≤200 字；周期记按周/日期分段；云端 max token 按场景放大覆盖一周；确认页发送后即进入生成页 + NDJSON/JSONL 流式解析 + 已合法内容可确认；整体 JSON 作为同一协议的兼容输入规范化。代码见 `StreamingMealParser.kt`/`StreamingMealSession.kt`。验收门禁见 `feature/AI记一餐_周期记_NDJSON流式开发规范.md` B1–B6（→`21`）。B3–B6 均已交付并 ACCEPTED（`.ai-context/docs/功能路径索引.md` 此前"B4-B6未开始"描述过期，2026-08-18 已订正）。
- ✅ **L1 云端 AI 首启同意 + 合规免责**已落地（同意状态模型 + 运行时闸门 + 设置页政策披露，`ad1c5878`），ARCH 独立复核通过，真机待验 `E-L1-01~12`。
- ✅ **K1i AI 流式渐进展示**已落地（`SwitchableAiRuntime.stream()` 真实委托，复用 L1 同意闸门，`d7240d6f`），ARCH 独立复核通过，真机待验 `E-K1I-01`（阻断性）/`E-K1I-02`。
- ✅ **B7：CREATE 路径做法/烹饪方式/标签/描述/特别说明丢失 bug 已修复（2026-08-18，`b7d7c254`）**：根因 `DishAutoGenerator.preview()` 未把 `SemanticDish` 已解析字段透传进 `DishPreview`，`commit()` 又对 `dishRepo.saveDish()` 硬编码空值——AI 解析出的做法从未真正落库。改动：`AutoGenModels.kt`（`SemanticDish`/`DishPreview` 补字段）+ `DishAutoGenerator.kt`（preview/commit 透传）+ `MultiDayRecorder.kt`（补 `steps` 映射），回归测试 `T-B7-01`。
- ✅ **B7 后续批：日志门禁 + 标签清洗 + 回归测试 + 死代码清理 + NDJSON 协议容错收窄（2026-08-18）**：Opus 出详细方案（含对原判断的关键订正）后按方案实施，构建+全量 shared 单测通过，经 `google_quality_engineer` 审查。要点：
  - **S1 标签清洗**：`DishRepository.saveDish` 的 `tagNames` 补 trim+去空过滤（`DishRepositoryTest` 新增回归）；排查发现该 sink 在生产 AI 链路上恒为空（AI 从不产出 `tags`），主要防的是手工建菜路径。
  - **日志门禁**：`AppLogger.write()` 单点加 `isDebuggable()` 门禁——release 包不再持久化写盘 `i/w/e` 级别日志（崩溃摘要走独立的 `writeSync`，不受影响）；`DishAutoGenerator`/`IngredientAutoGenerator` 的防御性 `error()` 不再拼具体菜名/食材名；`AiMealInputViewModel` 两处日志调用去重复拼接、错误文案不再暴露原始异常信息给用户。
  - **S3 回归测试**：`DishAutoGeneratorTest` 新增 `T-B7-02`（REUSE 不被同名新内容覆盖）、`MultiDayRecorderK1aTest` 新增 `T-B7-03`（`toSemanticDay→DB` 的 `steps` 端到端）。
  - **S2 决策**：`DishJson.cuisine` 默认值由 `"家常菜"` 改为 `""`（消除"AI没提"和"AI明确说家常菜"两种语义被默认值混淆的陷阱），**本批不做 cuisine/meal_slots 真透传**——NDJSON 协议目前没有这两个字段的信号来源，且 `meal_slots` 若透传会因大小写口径不对（协议小写四值 vs `meal_type.code` 大写五值）打穿"永不出现无餐次菜"的安全网，见待办。
  - **S4**：确认 `AiMealRecorder.kt` 全仓零生产引用（仅 DI 注册）后直接删除该文件 + `SharedModule.kt` 对应注册；`AiMealParser.kt`/`SchemaValidator.kt`/`SchemaMigration.kt`/`AiMealInputSchema.kt` 确认同样无生产调用方（B3 后主链路走 `StreamingMealParser`），本批只加 KDoc 警示注释未删除（牵连 `FlatToDayMealConverter`/`MealParseCanonicalizer`，留待单独批次理清）。
  - **透明准则子集**：确认页健康摘要文案扩成"本次将新建 X 道菜、Y 种食材（营养为自动估算，仅供参考）"（`buildHealthSafetyReport`），只做免设计的纯文案小改；展开显示做法/标签/食材清单的完整 UI 判定需先过 Apple UX 设计门禁，且 AI 目前从不产出 `cooking_step`/`tags`/`description` 事件（prompt 未要求），做了也是空的，故本批不做，见待办。
  - **NDJSON 协议容错（10-a/10-b/10-c，见下）**：排查"AI 返回内容被本地规则拦截"问题，确认快速记（`AiMealParser`/`SchemaValidator`）路径已是死代码、真正的拦截发生在周期记走的 `StreamingMealParser`。在不削弱 AF-03「非法归属不可进入预览」目标的前提下收窄了两处误伤 + 修了一个新发现的隐藏 bug：
    - **诊断人话化**：`StreamDiagnostic` 新增 `DiagnosticCode` 分类字段，VM 侧 `summarizeDiagnostics()` 按类合并计数出人话文案（原先逐条透传"dish_id「xxx」格式无效，已拒绝"这类协议原文，用户看不懂）。
    - **meal_id 自愈**：一条 `meal` 事件里 `date`/`slot` 各自校验合法时，`meal_id` 只是 AI 的复述——复述串对不上按 `date|slot` 归一（WARNING，不再整条拒绝）；同一 AI `meal_id` 先后指向不同 `date|slot` 仍按冲突拒绝后到者（不放松）。`dish` 补建父节点分支（唯一信号来源、无交叉校验）维持严格不自愈。
    - **dish_id 本地序号（连带修了一个隐藏 bug）**：AI 若把同一 `dish_id` 复用给两道不同名的菜，此前会**静默**用后者覆盖前者、无任何诊断（比拒绝更糟——AF-05"同键合并"对"同一道菜的重复描述"和"两道菜撞了同一个 dish_id"未加区分）。现按 `(mealId, 原始dish_id, name)` 判定：同名精确复用同一本地 key（仍走 AF-05 合并），不同名分配新本地序号且不覆盖原菜（记 WARNING）。
    - 新增 5 条 `StreamingMealParserTest` 用例锁定新行为，原有全部测试（T-01~T-08、AF-03/05/07/08、D-01~D-08）保持绿。规范文档 `AI记一餐_周期记_NDJSON流式开发规范.md` §4.3 已同步订正。
  - **交付前审查发现并修复 2 项阻断（`google_quality_engineer` 首轮复核）**：①`canonicalMealId` 的别名优先级颠倒——AI 若把 meal_id 打错复制成另一个真实存在的餐次字符串，会被陈旧别名重定向、整餐菜静默挂错餐次（比拒绝更糟），修复为"真实存在的餐次 key 永远优先于别名表"并补 2 条正反顺序回归测试；②`StreamingMealSession.snapshot()` 包装的段级失败诊断（如 `STREAM_ENDED_WITHOUT_TERMINAL`）未带 `DiagnosticCode` 默认落 `OTHER`，被 `summarizeDiagnostics` 的 8 个分类桶漏接、静默丢弃，且发现审查范围遗漏了 `:androidApp:testDebugUnitTest`（该模块单测此前从未在本批跑过，`T-B3-02` 因此没能暴露回归），补一个"未分类 ERROR 兜底走 `humanizeWarning`"分支后复验通过。二次复核见下方待办登记。

## 讨论中的方案
- 🔄 **核心实体能力层统一**：手工建菜/食材库/自由搭配仍有路径绕过 `IngredientAutoGenerator`，目标是统一的食材、营养、菜品语义能力入口（跨 F-DISH/F-INGREDIENT）。
- K1b（膳食健康评价逐成员化）蓝图仍 `DRAFT·PARKED`，等本线真机验证彻底收尾后再拾起。
- K1i-2（AI 推荐/周计划健康建议流式化）仅登记名字，未设计。

## 已知问题
- ⚠ **AI 日志隐私风险**：云端 Runtime 仍可能记录完整请求/响应，须脱敏后才能宣称符合隐私口径。

## 待办
- 真机验证详单见 `真机验证/真机待验证清单_<最新>.md`（`E-L1-01~12`/`E-K1I-01~02`/`E-B4/B5/B6-*`/本批新增项 等 ~40+ 项），本文件不重复摘抄。
- **cuisine/meal_slots 真透传（决策项，需先扩协议）**：NDJSON 事件加可选 `cuisine`/`meal_slots` 字段 + prompt 明确"不确定不填"，透传时加空值守卫；`meal_slots` 若收，只能做"菜名推断兜底 ∪ AI 给的值"的并集（归一后为空要退回兜底），不能让 AI 的窄口径替换掉宽口径兜底。
- **确认页展开 UI（需先过 Apple UX 设计门禁）**：展示新建菜的做法/标签/食材清单摘要，建议与下一条"prompt 补 `cooking_step` 等事件"合并成一批——否则做出来的展开区在真机上恒空、验不出效果。
- **AI 从未产出 steps/tags/description 事件**：`AiMealPrompt.NDJSON_SYSTEM_PROMPT` 目前只要求 `meal`/`dish`/`ingredient`/`seasoning` 事件，未要求 `cooking_step`；即使 B7 修复了透传链路，AI 现在也没有内容可透传。是否要求 AI 产出这些内容（token 成本 vs 内容完整性）待决策。
- **死代码整组清理**：`AiMealParser.kt`/`SchemaValidator.kt`/`SchemaMigration.kt`/`AiMealInputSchema.kt` 已确认零生产调用方，本批只加了警示注释；完整删除需先理清 `FlatToDayMealConverter`（仍被 `StreamingMealParser` 用）/`MealParseCanonicalizer`（仍被 `RuleMealParser` 用）的共用关系，避免误删还在用的依赖。
- **dish_id 本地序号的边界情况**：当前实现里，无 `dish_id` 也无 `dish_name` 的子事件（食材/调料/做法步骤）仍无法归属，这是协议本身的限制（AI 必须提供至少一种归属信号），非本批引入。
- **`google_quality_engineer` 复核遗留 4 项建议（非阻断，本批未做）**：①`summarizeDiagnostics` 的 8 个分类桶穷尽性靠人工维护，新增 `DiagnosticCode` 时容易漏接（已加通用 ERROR 兜底缓解，但精细分类计数仍需手动维护，建议改 `when` 穷尽式让编译器强制处理）；②`handleDishChildEvent`/`handleCookingStepEvent` 等子事件按"最近一次解析出的本地 dishId"指针挂靠，dish_id 被复用后**迟到**的子事件仍可能挂错菜（`dish_name` 精确匹配优先级应高于"最近一次"指针，当前是反过来的）；③`summarizeDiagnostics` 无专门单测，目前靠 `AiMealInputViewModelStreamTest` 间接覆盖；④二轮复核新发现：`summarizeDiagnostics` 的未分类 ERROR 兜底分支是**原文透传**（未做人话化），且无条数上限——极端情况下会在诊断区刷出十几条开发者协议措辞（如"NDJSON 行缺少 segment_id，已丢弃: …"），建议后续把 `SEGMENT_MISMATCH`/`INVALID_DATE` 也纳入分类桶 + 兜底列表加条数上限（如最多 3 条+"等 N 条"）。

## 关联横轴
- `21_AI与网络请求策略（专属）.md`（本功能的 AI 调用/隐私/重试策略权威定义，本功能是该册主案例）
- `20_健康与算法逻辑（专属）.md`（阶段二健康建议算法）
- `08_决策记录.md` D-13/D-14/D-15/D-16/D-17（AI 显式语义优先、双阶段、日期锚点、NDJSON 流式、冻结蓝图门禁）

---
最后更新：2026-08-18 · 来源：B7 修复批次 + B7 后续批（10 项待办，含 Opus 详细方案 + NDJSON 协议容错收窄）实核，B7 之前的条目来自 `07_项目现状.md` 能力成熟度表重组迁移未逐条重新核实，后续随真实开发批次继续校准。
