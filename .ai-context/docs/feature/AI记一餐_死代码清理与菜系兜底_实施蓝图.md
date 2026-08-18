# 批A：AI记一餐死代码清理 + cuisine 落库兜底 实施蓝图

> 状态：`BLUEPRINT_READY`
> **本蓝图仅由 ARCH 起草，不含任何代码实现**——按用户指示"你只负责蓝图，不要编码"。
> **颗粒度：L7**（项目基线）。
> 起草日期：2026-08-18（承接 B7 后续批 `cc806cb3` 的 3 项延后决策，本批为其中风险最低的一批）。
> **依赖关系**：本批与批B（`AI记一餐_协议扩展_菜系标签做法透传_实施蓝图.md`）互不依赖，可先后任意顺序或并行实施；两批**不要合并成一次提交**（见 §6 独立 commit 要求），否则"删了 616 行"会淹没在协议改动 diff 里，回滚粒度变差。
> **依据**：Opus 架构设计会话（本次未存档独立文件，结论已在本蓝图与批B蓝图中固化）核实"依赖方向单向、纯删除无风险"，本蓝图起草时已重新独立 grep 复核，证据见 §2。

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
| GC-08 | 真机清单登记 | §9 | 满足（2026-08-19 独立挑战 A-I-3 修订）：cuisine 落库兜底是用户可感行为变化（AI 建菜从"菜系 Tab 永久缺席"变为"归入家常菜"），按 CLAUDE.md「交付必做：真机待验证登记」强制条款登记，非可选 |
| GC-09 | 回归基线锁定 | §6 末尾 | 满足 |
| GC-10 | 逐字段真相源表 | §4.1 | 满足 |
| GC-11 | 字段/行为迁移清单 | §4.2 | 满足 |
| GC-12 | UI 判据与业务判据同源 | — | N/A：本批不改任何 UI/Compose 代码 |
| GC-13 | fallback 复用主路径校验入口 | — | N/A：无 fallback 新增 |
| GC-14~16 | 对象生命周期/可变持有物/搬迁注释 | — | N/A：纯删除 + 1 行参数传值，无新增可变状态 |
| GC-17~19 | 逐项状态 List&lt;Status&gt; | — | N/A：无逐项状态渲染 |
| GC-20 | 自动副作用清单 | §3 INV-A-02 | 满足：cuisine 落库兜底是本批唯一的运行时行为变化，已建模为不变量 |
| GC-21 | 提示/告知配 STEP 落点 | — | N/A：本批不新增用户提示 |
| GC-22 | 可见副作用配 T-ID | §8.2 | 满足：INV-A-02 有 T-A-02 |
| GC-23 | STEP 独立编号+完成形态字面量 | §7 | 满足 |
| GC-24 | STEP 勾销表 | §9 | 满足 |
| GC-25 | 完成形态字面量+grep判据 | §7 | 满足 |
| GC-26 | 冻结值修订记录 | — | N/A：不改任何阈值常量 |
| GC-27 | 编辑即失效收口函数核对 | — | N/A：与编辑态无关 |
| GC-28 | 构造时创建对象按基数分片 | — | N/A：无新增构造时创建对象 |
| GC-29 | 多来源写入同一聚合目标 | — | N/A：无聚合写入 |
| GC-30 | 状态转移驱动完整副作用链 | — | N/A：无状态机改动 |
| GC-31 | 挂起点+身份重校验 | — | N/A：无新增挂起点 |
| GC-32 | 高频异步事件节流 | — | N/A：无高频事件 |
| GC-33 | 禁止测试专用可变全局注入点 | — | N/A：不新增测试基础设施 |
| GC-34 | 注释/KDoc 一致性 | §7 STEP-A-1.5 | 满足：文档同步点已列 |
| GC-35 | 协议事件枚举逐项对照 | — | N/A：不改协议事件 |
| GC-36 | List&lt;Status&gt; 值域覆盖 | — | N/A |
| GC-37 | 独立挑战台账 | §10 | 满足——2026-08-19 已跑一轮独立挑战，4 项 CONFIRMED-ISSUE（A-I-1~4）全部就地处置，见 §10 |

---

## §1 目标与范围

### 1.1 一句话价值

删除 AI 记一餐链路里 B3 NDJSON 流式改造后已确认零生产调用方的 4 个死文件（+1 个死测试文件），并顺手修复 B7 后续批遗留的一个真实缺口：AI 建的菜（`source='ai'`）因为不在"存量自建菜空菜系回填"的 `WHERE source='user'` 范围内，永久性地在菜系筛选 Tab 里缺席。两件事都是低风险、高确定性的收口，互不依赖，合并一批交付。

### 1.2 触发来源与关键依赖

- 触发：B7 后续批（commit `cc806cb3`）§F-AI-MEAL.md 待办"死代码整组清理"+"cuisine 落库兜底"两项延后决策。
- 无外部依赖，可独立于批B/批C实施。

### 1.3 In Scope / Out of Scope

**In Scope**：
1. 删除 `AiMealParser.kt`/`SchemaValidator.kt`/`SchemaMigration.kt`/`AiMealInputSchema.kt`（生产代码，均为零引用死文件）+ `AiMealParserTest.kt`（对应死测试）。
2. `FlatToDayMealConverter.kt:22` 的失实注释订正（引用了已删除的 `AiMealParseResult` 类型名）。
3. `DishAutoGenerator.commit()` 的 CREATE 分支为 `saveDish()` 传入 `cuisine = preview.cuisine.ifBlank { Cuisines.HOME }`（此刻 `preview.cuisine` 恒为 `""`，本批之后走批B协议扩展才会有真实值——但兜底逻辑本身现在就该修，不依赖批B）。
4. 4 个文件、5 处引用的文档同步（功能路径索引/05诊断地图/21专属册/F-AI-MEAL.md，见 §6）。

**Out of Scope**：
- `MealParseCanonicalizer`（`RuleMealParser.kt` 内部 `internal object`）与 `FlatToDayMealConverter.kt` 本身——两者都是活代码（前者被 `RuleMealParser` 自身使用，后者被 `StreamingMealParser.parseWholeJsonToDays()` 使用），本批**不删不改**，只订正 `FlatToDayMealConverter.kt` 一行失实注释。
- `NdjsonEvent` sealed 密封类族（`NdjsonEvents.kt:57-131` 的 `MealEvent`/`DishEvent`/`IngredientEvent` 等）——独立核实（本蓝图起草时 grep `NdjsonEvent\.` 全仓零命中，即该密封类族本身也是死代码，`StreamingMealParser` 直接操作 `NdjsonLine` 从未转换成这些强类型）——但这与"AI 记一餐待办 3 项"的既定范围无关，是起草时顺带发现的另一处死代码，**显式弃置**，见 §12，不纳入本批（避免范围蔓延，且需要单独确认是否有意保留作为"文档化的协议契约"）。
- cuisine **真透传**（NDJSON 协议加字段）——批B范围，本批只做落库层兜底。

### 1.4 上一批延后项归宿核对（GC-03）

B7 后续批（`cc806cb3`）F-AI-MEAL.md 待办列表中的"死代码整组清理"与"cuisine 落库兜底"两项，本批为其正式落地，无其余延后项与本批相关。

---

## §2 现状与差距

| # | 现状 | 证据（file:line） | 差距/影响 |
|---|---|---|---|
| D1 | `AiMealParser.kt`/`SchemaValidator.kt`/`SchemaMigration.kt` 三个 `object` 与 `AiMealInputSchema.kt` 内 4 个 `data class`（`AiMealParseResult`/`AiParsedMeal`/`AiParsedDish`/`AiParsedIngredient`）全仓 grep（`grep -rn "AiMealParser\b\|SchemaValidator\b\|SchemaMigration\b\|AiMealParseResult\b\|AiParsedDish\b\|AiParsedIngredient\b\|AiParsedMeal\b" --include=*.kt shared/src androidApp/src`，排除这 4 个文件自身与其测试）**生产代码零命中** | 见上 grep 命令，本蓝图起草时实测 | B3 NDJSON 流式改造后主链路走 `StreamingMealParser`，这 4 个文件（原"K1→K2 非流式解析层"）已完全被架空 |
| D2 | `FlatToDayMealConverter.kt:22` 的 KDoc 注释提及 `AiMealParseResult`（"AI 输出 items(FlatMealJson) vs Parser 期望 meals(AiMealParseResult)"），但该文件**代码本体**（`import`/函数体）无任何处引用该类型——纯注释级引用 | `FlatToDayMealConverter.kt:22` | 删除 D1 四个文件后，该注释会引用一个已不存在的类型名，构成 BL-11（文档腐化）；`FlatToDayMealConverter.kt` 本身继续被 `StreamingMealParser.kt` 的 `parseWholeJsonToDays()` 使用，**不在删除范围** |
| D3 | `MealParseCanonicalizer` 是 `RuleMealParser.kt:529` 内的 `internal object`，仅被 `RuleMealParser` 自身使用（`RuleMealParser.kt:100/391/405`），与 D1 四个死文件**零关系**（不是"死文件依赖它"也不是"它依赖死文件"） | `RuleMealParser.kt:529` 定义处 + 三处调用 | 确认 `RuleMealParser.kt` 不在本批改动范围，无需处理任何纠缠 |
| D4 | 4 个死文件的 DI 注册：`grep -n "AiMealParser\|SchemaValidator\|SchemaMigration\|AiMealInputSchema" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/di/SharedModule.kt` **零命中** | `SharedModule.kt` | 无需删 DI 注册（本身就没注册），三者均为 `object`/纯 `data class`，无构造依赖需要断开 |
| D5 | `DishAutoGenerator.commit()`（函数体 144-188 行）CREATE 分支调 `dishRepo.saveDish(...)` **未传 `cuisine` 参数**，走 `saveDish()` 默认值 `cuisine: String = ""`（`DishRepository.kt:479`） | `DishAutoGenerator.kt:172-185`（`saveDish(...)` 调用本身的行区间，无 `cuisine=` 一行） | `PresetDataSeeder.kt` 的 `backfillEmptyUserDishCuisine`（依据 `Cookbook.sq:1133-1135` 注释）只对 `WHERE source='user'` 的空菜系存量自建菜幂等回填"家常菜"；AI 建的菜 `source='ai'`（见 `DishAutoGenerator.kt:184` `preview.source.ifBlank { "auto" }`，AI 路径下 `preview.source` 来自 `SemanticDish.source`，`MultiDayRecorder.toSemanticDay()` 里为 `dishJson?.source?.ifBlank { "ai" } ?: "ai"`），不在该回填 `WHERE` 范围内，永久留空菜系。空菜系的下游影响：①`androidApp/.../ui/dishes/DishesViewModel.kt:162-170` 菜系筛选 Tab 命中不到该菜；②`RecommendationDataSource.kt:170` 的 `selectCookedCuisineFreqSince` 在取数层剔除空菜系，AI 建的菜对口味画像 `TasteProfile.cuisineFreq` 零贡献 |

---

## §3 不变量

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-A-01 | 删除 D1 四个生产文件 + 1 个测试文件后 | `:shared:compileDebugKotlinAndroid` 与 `:shared:testDebugUnitTest` 均编译/运行通过，无未解析引用 | 任何生产代码因删除而编译失败（若发生，说明 §2 D1 的 grep 结论有遗漏，必须先补 grep 复核而非强行删除后修补） | T-A-01（编译验证，非独立单测，见 §7 STEP-A-1.4） |
| INV-A-02 | `DishAutoGenerator.commit()` 处理 `ResolveKind.CREATE` 分支，`preview.cuisine` 为空白字符串（`""`，含本批之后、批B落地之前的全部实际场景） | `saveDish(cuisine=...)` 实际写入的值为 `Cuisines.HOME`（"家常菜"） | `saveDish()` 收到空字符串（即不传该参数、退回其自身默认值 `""`）；`preview.cuisine` 非空白时被强行覆盖成 `Cuisines.HOME`（本批只处理"空白兜底"，非空白值原样透传是批B的职责，本批不触碰非空分支——因为本批交付时 `preview.cuisine` 客观上恒为 `""`，`DishPreview` 尚无 `cuisine` 字段，见 §4.1，`ifBlank` 写法本身已经是"非空原样透传+空白兜底"的正确表达，为批B预留了透传接口，不是"占位以后要改"） | T-A-02 |

---

## §4 接口契约

### 4.1 逐字段真相源表

| 字段/行为 | 唯一写入者 | 读取方 | 终局形态 |
|---|---|---|---|
| `DishAutoGenerator.commit()` 传给 `saveDish()` 的 `cuisine` 实参 | 本函数内部单一表达式 `preview.cuisine.ifBlank { Cuisines.HOME }` | `DishRepository.saveDish()` | 新增实参传递（此前完全不传该具名参数，退默认值） |

**关键前提（避免与批B的字段规划冲突）**：`DishPreview`（`AutoGenModels.kt:89-105`）当前**没有** `cuisine` 字段。本批 STEP-A-1.3 里 `preview.cuisine` 这个表达式在批A单独落地时**编译不通过**（`DishPreview` 无此属性）。两种处理方式二选一，由 CODE 在实施时按 §1.2 依赖关系判断：
- **若批B已先于批A落地**：`DishPreview.cuisine` 字段已存在（批B新增），`preview.cuisine.ifBlank { Cuisines.HOME }` 直接编译通过，语义如 §3 INV-A-02 所述。
- **若批A先于批B落地（推荐顺序，风险更低，2026-08-19 独立挑战 A-I-2 修订）**：`DishPreview` 尚无 `cuisine` 字段，本批 STEP-A-1.3 **必须同时**在 `AutoGenModels.kt` 的 `SemanticDish` 末尾（`source` 之后）与 `DishPreview` 末尾（`steps` 之后）各新增 `val cuisine: String = ""`（与批B §4.1 规划的字段名/类型/默认值/位置逐字一致，先到先加，后到者跳过）。
  **本批不改 `DishAutoGenerator.preview()`**：`DishPreview.cuisine` 有默认值 `""`，且批A交付时 `MultiDayRecorder.toSemanticDay()` 尚未透传 cuisine、`SemanticDish.cuisine` 客观恒为 `""`，`preview()` 传与不传结果完全相同——把 `cuisine = input.cuisine` 留给批B STEP-B-1.10 独占，可避免两批都改同一构造调用产生重复具名实参编译错误。因此批A交付时 `preview.cuisine` 恒为 `""`，`ifBlank` 恒触发，INV-A-02 断言的"恒兜底为家常菜"在批A单独交付时是可验证的确定性行为，不依赖批B。

**CODE 执行前必须先判定（两条，缺一不可，2026-08-19 独立挑战 A-I-1 修订）**：
```
grep -n "val cuisine" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/AutoGenModels.kt
grep -n "cuisine = preview.cuisine" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/DishAutoGenerator.kt
```
- **第一条命中（应为 2 处）** → `SemanticDish`/`DishPreview.cuisine` 已由批B加好，本批**不改** `AutoGenModels.kt`；**零命中** → 批A独立先行，本批须在 `SemanticDish` 末尾（`source` 之后）与 `DishPreview` 末尾（`steps` 之后）各加 `val cuisine: String = ""`，字段名/类型/默认值/位置必须与批B蓝图 §4.1 逐字一致。命中数为 1 属异常（半落地）→ **STOP**，转 Q 缺口单。
- **第二条命中** → 批B已代为补上 `commit()` 的 cuisine 传参，本批 STEP-A-1.3 的 `commit()` 改动**整条跳过**（只做 §7 其余步骤），不得重复添加，否则编译报「An argument is already passed for this parameter」；**零命中** → 按 §4.2 新增该行。

两条判定结果均须记入 §9 交付台账，不可省略。

### 4.2 GC-11：字段/行为迁移清单

| 迁移对象 | 旧形态 | 新形态 |
|---|---|---|
| `DishAutoGenerator.commit()` 的 `saveDish()` 调用 | 不传 `cuisine`，退 `saveDish()` 默认值 `""` | 显式传 `cuisine = preview.cuisine.ifBlank { Cuisines.HOME }` |

**终局裁决**：**新增**（原调用点没有这个具名参数，非替换某已有值）。

---

## §5 UI 设计

不适用（本批不涉及任何 UI/Compose 改动）。

---

## §6 文件改动清单 + Allowlist

| 文件 | 允许操作 | 禁止操作 |
|---|---|---|
| `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/AiMealParser.kt` | **删除整个文件** | — |
| `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/SchemaValidator.kt` | **删除整个文件** | — |
| `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/SchemaMigration.kt` | **删除整个文件** | — |
| `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/AiMealInputSchema.kt` | **删除整个文件** | — |
| `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/ai/meallog/AiMealParserTest.kt` | **删除整个文件** | — |
| `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/FlatToDayMealConverter.kt` | 仅改第 22 行 KDoc 注释，去掉 `AiMealParseResult` 类型名引用（改写为描述性表述，如"旧嵌套 meals 结构（已删除）"） | 改动该文件除注释外的任何代码 |
| `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/DishAutoGenerator.kt` | `commit()` 的 CREATE 分支 `saveDish(...)` 调用新增 `cuisine = preview.cuisine.ifBlank { Cuisines.HOME }` 一行 + 顶部按需新增 `import com.sxdbsm.cookbook.domain.model.Cuisines` | 改动该文件其余逻辑 |
| `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/AutoGenModels.kt` | **仅当批A独立先行时**（§4.1 判定分支）：`SemanticDish`/`DishPreview` 末尾各加 `val cuisine: String = ""` | 改动其余字段顺序/既有字段 |
| `.ai-context/docs/功能路径索引.md` | 第 31 行去掉 `AiMealInputSchema→meal-log.v2` 与 `AiMealParser` 两个词条 | 改动该行其余内容 |
| `.ai-context/docs/projectReview/05_诊断地图.md` | 第 37/42 行去掉 `AiMealParser` 引用，第 42 行改为指向 `StreamingMealParser`+`CloudAiRuntime` | 改动其余排查入口 |
| `.ai-context/docs/projectReview/21_AI与网络请求策略（专属）.md` | 第 82 行 `ai/meallog/AiMealParser` 改为 `ai/meallog/StreamingMealParser` | — |
| `.ai-context/docs/projectReview/features/F-AI-MEAL.md` | 待办列表"死代码整组清理"条目勾掉/改为已完成记录；S4 段落末尾补一句完成说明 | 改动其余现状/待办条目 |

**显式禁改文件清单**：
- `shared/.../ai/meallog/RuleMealParser.kt`（含内部 `MealParseCanonicalizer`）——不改，见 §2 D3。
- `shared/.../ai/meallog/NdjsonEvents.kt`——本批不处理 `NdjsonEvent` 密封类族死代码（见 §1.3 Out of Scope、§12）。
- 批B涉及的全部文件（`AiMealPrompt.kt`/`MealStreamDraftMapper.kt`/`StreamingMealParser.kt` 的 `handleDishEvent` 逻辑本体等）——本批与批B独立，不预先改动。

**回归基线锁定（GC-09）**：
- `scripts\build-cli.bat :shared:testDebugUnitTest`（全量，0 failures）
- `scripts\build-cli.bat :androidApp:testDebugUnitTest`（全量，0 failures——**上批教训：此模块必须显式跑，不能只跑 shared**）
- `scripts\build-cli.bat :androidApp:assembleDebug`

---

## §7 分阶段实施步骤

**STEP-A-1.1**：执行 §2 D1 的 grep 命令自行复核零引用结论（不得直接采信本蓝图，必须实际执行一遍，因为距起草可能已有代码变化）：
```
grep -rn "AiMealParser\b\|SchemaValidator\b\|SchemaMigration\b\|AiMealParseResult\b\|AiParsedDish\b\|AiParsedIngredient\b\|AiParsedMeal\b" --include=*.kt shared/src androidApp/src
```
完成形态：命中结果只出现在 `AiMealParser.kt`/`SchemaValidator.kt`/`SchemaMigration.kt`/`AiMealInputSchema.kt`/`AiMealParserTest.kt` 五个文件自身。若出现其他文件命中，**STOP**，不得继续删除，转入 Q 缺口单上报。

**STEP-A-1.2**：删除 §6 allowlist 中标注"删除整个文件"的 5 个文件。
完成形态：`git status --short` 显示这 5 个文件为 `D`（deleted）。

**STEP-A-1.3**：`DishAutoGenerator.commit()` 按 §4.1/§4.2 新增 `cuisine` 传参；先执行 §4.1 的判定 grep 决定是否需要连带在 `AutoGenModels.kt` 加字段。
完成形态：`grep -n "cuisine = preview.cuisine.ifBlank" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/DishAutoGenerator.kt` 命中 1 处。

**STEP-A-1.4**：`FlatToDayMealConverter.kt:22` 注释订正。
完成形态：`grep -n "AiMealParseResult" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/FlatToDayMealConverter.kt` 零命中。

**STEP-A-1.5**：按 §6 完成 5 处文档同步。
完成形态：`grep -n "AiMealParser" .ai-context/docs/功能路径索引.md .ai-context/docs/projectReview/05_诊断地图.md ".ai-context/docs/projectReview/21_AI与网络请求策略（专属）.md"` 零命中（`05_诊断地图.md` 该行改指向 `StreamingMealParser` 后不应再含 `AiMealParser` 字面量）。

**STEP-A-T-1**：新增 `DishAutoGenerator.commit()` cuisine 兜底的单测（放入既有 `DishAutoGeneratorTest.kt`），覆盖 INV-A-02。

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
| 既有 `DishAutoGeneratorTest.kt` 的 `db`/`ctx`/`generator`（`@BeforeTest setUp()`）+ 测试自建 `DishRepository(db)` 实例（`generator` 内部持有的是 private 实例，断言读回值需另建一份） | 验证 CREATE 分支 `commit()` 后 `DishRepository.getDishById(id).cuisine == "家常菜"`，输入 `SemanticDish` 不显式设置 cuisine（若批A单独先行走"加空字段"分支，默认值本就是 `""`，天然满足"未设置"） | 伪造 `preview.cuisine` 已经非空来间接验证（那是批B的测试职责，本测试只锁"空白兜底"这一条路径） |

### 8.2 INV↔T 双向映射表

| INV | T-ID | 断言要点 |
|---|---|---|
| INV-A-01 | T-A-01 | 删除后 `:shared:compileDebugKotlinAndroid`/`:shared:testDebugUnitTest`/`:androidApp:testDebugUnitTest` 全部编译通过（编译验证本身即证据，非独立断言型单测） |
| INV-A-02 | T-A-02 | `DishAutoGeneratorTest` 新增用例：构造 `SemanticDish(name="测试兜底菜", ...)`（不设置 cuisine 或设置为默认值 `""`）→ `preview()` → `commit()` → `DishRepository.getDishById(id)!!.cuisine == "家常菜"`。**观测点补充**（N-A-5）：AI 建菜统一记"家常菜"后会进入 `RecommendationDataSource.kt:169-170` 的 `TasteProfile.cuisineFreq` 聚合，可能让"家常菜"在口味画像里被放大（此前这些菜零贡献）——不阻断本批，记入 §9 真机项供观察 |

---

## §9 交付台账（CODE 完成时填）

### §4.1 判定分支记录（必填）

`grep -n "val cuisine" shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/AutoGenModels.kt` 执行结果：______（命中/未命中，命中则说明批B已先落地）

### STEP 勾销表

| STEP-ID | 状态 | 落地 commit | diff 定位 |
|---|---|---|---|
| STEP-A-1.1 | ⬜ | | |
| STEP-A-1.2 | ⬜ | | |
| STEP-A-1.3 | ⬜ | | |
| STEP-A-1.4 | ⬜ | | |
| STEP-A-1.5 | ⬜ | | |
| STEP-A-T-1 | ⬜ | | |

### 验收命令输出

（CODE 填：三条命令的实际输出/BUILD SUCCESSFUL 状态；`:shared:testDebugUnitTest` 删除前/后用例总数各填一次，预期减少 4——`AiMealParserTest.kt` 删除导致，非回归）

### 代码质量门禁

（`google_quality_engineer` 终审结论填于此，须包含对本蓝图 §2 D1 grep 结论的复核确认）

### 真机待验证登记（强制，按 CLAUDE.md「交付必做」执行，2026-08-19 独立挑战 A-I-3 修订）

本批有用户可感行为变化（AI 建的菜从"菜系 Tab 永久缺席"变为"归入家常菜"），须登记至最新真机待验证清单（含 `验证结果`/`原因` 两列）：
- ①AI 记一餐新建一道库中没有的菜 → ②菜品管理页切到"菜系"Tab → ③预期：该菜出现在"家常菜"下（若批B已落地且 AI 给出了具体菜系，则出现在对应菜系下）。

---

## §10 独立挑战台账（GC-37）

**挑战方**：独立 general-purpose/opus agent（只读蓝图成文，未见起草过程，逐条回源码 file:line 核实）。

**结论**：§2 现状表全部核实无误（4 死文件零引用、DI 零注册、`FlatToDayMealConverter.kt`/`MealParseCanonicalizer` 现状判断均精确）。挑出 4 项 CONFIRMED-ISSUE，均已就地处置：

| # | 挑战摘要 | 裁决 | 处置 |
|---|---|---|---|
| A-I-1（阻断） | §4.1 判定 grep 漏了 `DishAutoGenerator.commit()` 一条，批B先落地时批A会重复传 `cuisine` 实参，编译报「An argument is already passed for this parameter」 | CONFIRMED-ISSUE | §4.1「CODE 执行前必须先判定」改为两条 grep（`AutoGenModels.kt`+`DishAutoGenerator.kt`），命中/未命中/半落地三分支均已写明 |
| A-I-2（阻断） | §4.1 要求批A改 `DishAutoGenerator.preview()`，与 §6 allowlist「禁止改动该文件其余逻辑」自相矛盾，且会与批B STEP-B-1.10 重复传参 | CONFIRMED-ISSUE | §4.1 明确"本批不改 `preview()`"，该步骤由批B STEP-B-1.10 独占；批A交付时 `preview.cuisine` 恒为 `""`，INV-A-02 不受影响 |
| A-I-3 | §9 真机验证登记写"建议非阻断"，与 CLAUDE.md「交付必做：真机待验证登记」强制条款冲突，且本批确有用户可感行为变化 | CONFIRMED-ISSUE | §0.1 GC-08 行改判"满足"，§9 标题改"强制"并给出具体验证步骤 |
| A-I-4 | 起草时漏登记另一处同性质死代码：`AiMealPrompt.buildRequest()`/`SYSTEM_PROMPT`/`FLAT_SYSTEM_PROMPT` 已零生产调用 | CONFIRMED-ISSUE | §12 弃置项登记新增一行，不纳入本批范围 |

处置后自查：4 项均已在 §0.1/§4.1/§9/§12 同步修订。另有 6 项 MINOR-NIT（GC-37/§10 状态文字一致性、`commit()` 精确行号统一、测试夹具表补充说明、测试数量变化登记、`cuisineFreq` 偏移观测点、"5处/4个文件"措辞统一）已一并采纳修订。**蓝图转 `BLUEPRINT_READY`**。

---

## §11 门禁与角色

- 本批不涉及 UI/交互/合规文案，豁免 `apple_ux_designer`/`apple_software_behavior`/`copywriter` 前置门禁。
- CODE 完成、构建+单测通过后，须走 `google_quality_engineer` 代码质量终审，**终审范围须显式包含**：①复核 §2 D1 的"零引用"grep 结论（防止蓝图起草与 CODE 执行之间代码库发生变化）；②核对 §4.1 判定分支是否被正确执行且记入 §9。

## §12 弃置项登记（GC-03 前瞻）

| 项 | 状态 | 归宿 |
|---|---|---|
| `NdjsonEvent` 密封类族（`MealEvent`/`DishEvent`/`IngredientEvent`/`SeasoningEvent`/`CookingStepEvent`/`WarningEvent`/`AdviceEvent`，`NdjsonEvents.kt:57-131`）——本蓝图起草时独立核实全仓 `NdjsonEvent\.` 零命中，`StreamingMealParser` 从未把 `NdjsonLine` 转换成这些强类型，是另一处独立于本批既定范围的死代码 | 显式弃置，不纳入本批 | 需要先确认这组密封类是否有意保留作为"协议契约的强类型文档"（部分项目会保留未使用的强类型定义作为可读性锚点），还是纯历史遗留可删——这是一个需要单独判断的小决策，不与本次"AI记一餐3项待办"批次绑定，留待下次触及 `ai/meallog` 包时顺手处理或由用户明确决定 |
| `AiMealPrompt.buildRequest()` / `SYSTEM_PROMPT` / `FLAT_SYSTEM_PROMPT`（`AiMealPrompt.kt:61/125/137`）——2026-08-19 独立挑战（A-I-4）实测全仓零生产调用方（生产只用 `MAX_INPUT_CHARS` 与 `buildStreamingRequest`），即 FLAT prompt 当前从不会被发给任何模型 | 显式弃置，不纳入本批 | 与 `NdjsonEvent` 密封族同性质（B3 前非流式路径遗留）。删除前需确认 `StreamingMealParser.tryWholeJsonFallback()` 对整体 JSON 的兼容解析是否仍希望保留一份"扁平格式契约文档"；与本次 3 项待办范围无关，留待下次触及 `ai/meallog` 包时一并决定 |
