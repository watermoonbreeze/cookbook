# Cookbook 架构与代码质量审核报告

> 评审身份：资深 Android/Kotlin/KMP 架构评审。审核只提方案不改代码。
> 严谨性排序：正确性 > 并发安全 > 性能 > 可维护性 > 测试。
> 评审基于真实代码抽样：`:shared` 数据层/领域层/AI 推荐、`:androidApp` 主 VM、DI、平台适配、seed/迁移。
> 优先级：🔴 高（正确性/并发/数据一致性真风险） · 🟡 中（性能/可维护/健壮性） · ⚪ 低（风格/文档/加固）
> 每条区分 **[可直接修]**（纯质量/测试/常量/真 bug）与 **[需拍板]**（架构/迁移/大重构）。

---

## 总体评价

工程整体质量**高于同类个人 KMP 项目均线**：
- 数据层已系统性消除多处 N+1（`buildDishMinis`/`observeTimelineCards`/`dishNutrition` 均为批量取数）。
- 营养计算层（`NutritionCalculator`/`NutritionBalance`）除零、null、命名常量都处理到位，是全项目质量标杆。
- SQLDelight 方言限制（无 UPSERT）、迁移幂等、seed 指纹守卫、`INSERT OR REPLACE` 兜底都按红线执行，`shared` 的 `isReturnDefaultValues=true` 也已配置。
- 推荐打分权重已集中到 `RecommendationWeights`（历史散落 magic number 已收敛）。

主要遗留风险集中在三块：**(1) seed 静默跳过缺乏生产侧告警**、**(2) 少数 VM 的错误静默吞 + 冻结 today 可测性**、**(3) 大量 by-name/子串匹配的脆弱性**。以下逐条。

---

## 一、并发 / 状态

### 🟡 [风险:中][体量:S][并发] HomeViewModel.deleteDay 静默吞异常，无任何反馈
**[可直接修]** `androidApp/.../home/HomeViewModel.kt:79`
```kotlin
fun deleteDay(date: LocalDate) {
    viewModelScope.launch { runCatching { mealRepo.deleteDayMeals(date) } }
}
```
`runCatching` 结果直接丢弃：删除失败（DB 锁/异常）时用户无感知，首页却像删成功。同类还有 `yearAverages`（`getOrDefault(null to null)` 吞 `dateRange()` 失败，line 145）。
**建议**：`.onFailure { AppLogger.e(...) }` 至少落日志；删除失败可回灌一个一次性 `errorMessage`/`snackbar` 态。删除是破坏性操作，静默失败违反「保存有反馈」体验准则。

### ⚪ [风险:低][体量:S][可测性] VM 内冻结 `DateTime.today()`，跨午夜 + 单测不可控
**[可直接修]** `HomeViewModel.kt:85`、`TimelineViewModel.kt:47`、`AddMealViewModel`(多处 `DateTime.currentHour()/nowTime()`)、`RecommendationDataSource.currentSeason()`(下详)
```kotlin
private val today = DateTime.today()   // wallStart/wallEnd 都基于它
```
VM 生命周期内固定，长驻后台跨午夜色系墙/时间线索引会偏。红线明确要求「派生逻辑别依赖内部 today」。
**建议**：这类展示态跨午夜偏移影响小，可标注保留；但**新写**的派生逻辑一律把 `today` 提为默认参数（生产传 `DateTime.today()`），与 `RecommendationDataSource.gather(today=...)` 保持一致风格。

### ⚪ [风险:低][体量:M][并发] Picker/Family 多 init 加载器并发 + 捕获旧快照
**[可直接修]** `IngredientPickerViewModel.kt` `init { loadCategories(); loadAllIngredients(); loadUnits(); loadPantryIds(); loadHealthProfiles() }`
各 loader 独立 `launch` 后 `_state.value.copy(...)` 写回。现有代码多数已用**最新** `_state.value.copy` 写回（符合红线），但仍有 `selectMainTab` 里先 `val current = _state.value` 捕获、再经 `categoryRepo.listAll()`/`loadAllForTab()` 挂起、写回时用了 `current.allCategories`。挂起期间别处填的字段可能被旧 `current` 覆盖。
**建议**：所有经挂起后的写回统一 `_state.update { it.copy(...) }`；需要读源数据时读 `_state.value` 而非启动前捕获的快照（`grep '\.value = \w\+\.copy('` 巡检）。
**说明（已排除误报）**：`AddMealViewModel.save()`(line 320) 的 `val s=_state.value`+`drafts` 是**有意快照**（保存用户点下那一刻的草稿），且 `saving/done` 写回已用最新 `_state.value.copy`（注释 D10），`onFailure` 有处理——**非 bug，是正确模式**。

### ⚪ [风险:低][体量:S][可维护] AiRecommendViewModel 用 `mutableStateOf` 而非 StateFlow
`AiRecommendViewModel.kt:33` 用 Compose `State`。主线程单线程更新，`recommend()` 已显式重新带回粘性字段（slot/window/style，注释修过「跳回综合」bug）。可接受，但与其余 VM 的 `MutableStateFlow` 风格不一致。**建议**：新 VM 统一 StateFlow + `data class UiState`（规范五）。

---

## 二、数据一致性（seed / 迁移 / 引用完整性）

### 🔴 [风险:高][体量:M][数据一致性] seed 按名解析缺失项**生产侧全静默**，仅测试有校验
**[可直接修 + 部分需拍板]** `PresetDataSeeder.kt`
- 菜品食材名查不到 → `?: return@ing` 静默丢配料（line ~595/623）
- 单位名查不到 → `unit_id = unitIds[di.unit]` 落 `NULL`（line ~601/629）
- 人群名查不到 → `crowds[rule.crowd] ?: return@forEach`（line ~460，**连测试校验都没有**）
- 分类 code 查不到 → `categoryIdsByCode[rule.category] ?: return@forEach`（line ~511）
- 营养名查不到 → `?: return@forEach`（line ~345，食材建了但零营养）

`validateDishSeedForTest` 等只在单测跑；**真机 seed 静默跳过，无一行日志**。红线明确「未知食材/分类 code、菜品的食材名/烹饪方式/单位被 seeder 静默跳过（不崩但少关联）」。
**建议**：
1. [可直接修] seed 各按名解析的 `?: return` 分支加 `CookbookLog.w("Seed", "skip ...:name")`（一次 seed 顶多几十条，可接受），生产也能从日志发现「扩了菜没生效」。
2. [需拍板] 把 `validateDishSeedForTest` 的引用完整性校验在 **debug 包 seed 后**跑一遍（release 跳过），缺失即打警告，避免只有 CI 才发现漏关联。

### 🟡 [风险:中][体量:S][数据一致性] seed 指纹守卫是**整包指纹**，单文件改动 + 逻辑版本未变则整段跳过
**[需拍板]** `PresetDataSeeder.kt:106-122`
`fingerprintOf(SEED_LOGIC_VERSION, categoriesJson, ingredientsJson, ..., dishesJson, nutritionJson)` 是所有 JSON 拼一起算一个指纹——只改 `dishes.json` 且逻辑版本没动时，若某种情况指纹恰好没变（正常会变，但依赖「所有文件参与」），只能靠「更新基础数据」按钮 `force=true`。普通用户不会用该按钮。
**现状实际是安全的**（任一 JSON 变，拼接串就变，指纹必变）→ 真正的坑是：**改了 seed 处理逻辑但没 bump `SEED_LOGIC_VERSION`** 时老库不会重跑。
**建议**：改 seed JSON 内容 = 天然生效；改 seed **代码逻辑**必须同步 `SEED_LOGIC_VERSION++`（已有此机制，需在 PR checklist 固化）。可加单测断言「dishes.json 内容变 → 指纹变」。

### 🟡 [风险:中][体量:S][数据一致性] `backfillFoodGroups` 每次启动**重刷预设食材**分类
**[需拍板]** `PresetDataSeeder.kt:71-83`
预设食材 `food_group` 每次启动按 `FoodGroup.classify()` **覆盖重刷**（用户食材只填空的）。`classify` 关键词改进即视为「修正历史归错」是刻意设计，但意味着**跨版本升级会悄悄改变预设食材归类**（如某食材从「蔬菜」变「菌菇」，色系墙/营养统计随之变）。
**建议**：保留策略，但在 `classify` 关键词/`NAME_OVERRIDE` 改动的 PR 里显式说明「会重分类哪些预设食材」；可加单测锁定一批代表食材的期望大类，防止关键词误改打断（红线：改 general 大类名会打断按名断言）。

### ⚪ [风险:低][体量:S][文档] Schema 版本注释 `(v1)` 与实际 23 个 `.sqm` 严重不符
**[可直接修]** `Cookbook.sq:2` `-- Cookbook 数据库 schema (v1)`。真实 `Schema.version` 由 23 个 `.sqm` 推导=23。注释误导排查者。**建议**：改注释为「schema v23（版本=.sqm 文件数，见迁移目录）」，或直接删版本号避免维护漂移。

### ⚪ [风险:低][体量:S][数据一致性-待确认] `INSERT OR REPLACE INTO dish_ingredient` 是否真去重存疑
**[待确认]** `Cookbook.sq:1118` `dish_ingredient` 有自增 `id` 主键（`rel_id`）。若 (dish_id, ingredient_id) 上**无 UNIQUE 约束**，`INSERT OR REPLACE` 永不触发 REPLACE，seeder 的「补齐式追加」在重复 seed 时可能累积重复行。`saveDish` 路径因先 `deleteIngredientsOfDish` 不受影响，但 seeder 直接 insert。
**建议**：确认 `dish_ingredient` 是否有 (dish_id, ingredient_id) UNIQUE；若无，seeder 侧应「查在否再插」而非依赖 REPLACE。同样核对 `meal_record_dish`。

### ⚪ [风险:低][体量:S][数据一致性] `ensureLegacyColumns` 兜底补列是刻意保留的历史债
`DatabaseDriverFactory.android.kt:35` 在建驱动后 `runCatching { ALTER TABLE ingredient ADD COLUMN reason ... }`。属「列进了 Schema.create 却漏迁移(10.sqm 被 no-op 化)」的正确兜底，**保留**。仅提示：这是一处 schema creep 信号，后续新列必须 CREATE TABLE + `N.sqm` 一起改（红线已覆盖）。

---

## 三、性能

### 🟡 [风险:中][体量:S][性能] FoodCategory/Ingredient 分类树 `countChildren` N+1
**[可直接修]** `IngredientRepository.kt`/`FoodCategoryRepository`：`listChildren`/`listTopLevel`/`listAll`/`listCategories` 里对每个分类行 `.map { q.countChildren(row.id).executeAsOne() }`——N 个分类=N 次 count 查询。
**建议**：改一条带 `GROUP BY parent_id` 的子查询一次取全部子计数，内存 join。分类量当前不大（影响有限），但这是全项目最典型的残留 N+1，性价比高。

### ⚪ [风险:低][体量:S][性能] `loadFullDish` 单菜 4 条串行查询
`DishRepository.kt:354-389`：cookingMethods/tags/ingredients/steps 各一条。属单条详情读取（非循环），可接受；仅提示 `observeDishById` 每次库变都会重跑这 4 条。**建议**：保留；若详情页出现卡顿再考虑合并查询。

### ⚪ [风险:低][体量:S][性能] `IngredientRepository` 全表名归一化内存扫描
`IngredientRepository.kt:~104` `selectActiveIngredientIdNames().executeAsList().firstOrNull { normalizeNameKey(...)==key }`——为兼容老库同名多 id、且 sqlite_3_18 的 WHERE 不支持 `REPLACE/TRIM`（红线），只能拉全表在 Kotlin 侧比对。食材量级下可接受。**建议**：若食材表增长到数千，考虑加 `name_key` 冗余列（红线已提此方案）。

### 🟡 [风险:中][体量:S][性能] NewDishViewModel.applyIngredientGroup 逐项串行建食材
**[可直接修]** `NewDishViewModel.kt:381-387`：`group.items.forEach { createUserIngredient(it.name) }` 串行 N 次 DB 写。配料组 10 项=10 次串行往返。
**建议**：包一个 `db.transaction` 的批量 API，或 `map{ async{} }.awaitAll()` 并发。属体验顺手项。

---

## 四、架构 / 边界

### ⚪ [风险:低][体量:L][可维护] IngredientPickerViewModel 巨石化
`IngredientPickerViewModel.kt`（~1000+ 行）一肩挑：分类树展开、食材分页、防抖搜索、食材 CRUD、库存增减、菜品匹配、调养去重、含营养子表单的完整编辑器。认知负荷高、改动易牵连。
**[需拍板]** 建议按「浏览/搜索」「编辑器」「库存」拆子状态或抽 UseCase。非紧急，但后续该屏每次改动风险随行数上升。红线也点名此 VM 曾因跳分类未重建 tree 出静默空列表。

### ⚪ [风险:低][体量:S][边界] 首页营养/热量计算在 VM 内联，但已委托 domain 静态方法
`HomeViewModel.kt:100-137` 的 `FoodGroup.nutritionLevel/groupsOf/nutritionGaps`、`CalorieTarget.dailyTarget/status` 都是 **domain 层静态函数**，VM 只做编排（× share、roundToInt）——**属合理编排，非逻辑泄漏**（此前评审曾误判为泄漏）。仅 `× share` 折算这一步可考虑下沉到 domain（`CalorieTarget.personalShare(...)`）供多屏复用。可选。

### ⚪ [风险:低][体量:M][重复] 食材分类逻辑双份实现
`FoodGroup.classify()`（按**名**关键词/尾词/override）与 `RecommendationDataSource.classifyPairRole()`（按**分类维度**子串）是两套策略，关键词集不同、不共享。同一食材两处可能判定不一致，一处修 bug 另一处不跟。
**[需拍板]** 建议长期收敛为单一分类入口（名 + 分类双信号）。当前功能不同（搭配角色 vs 营养大类），可先标注「两处若判定分歧以 X 为准」。

---

## 五、健壮性

### 🟡 [风险:中][体量:M][健壮性] 大量 by-name / 子串匹配脆弱，happy-path 之外无测试
**[可直接修:补测试 / 需拍板:改判定]**
- `FoodGroup`：`name.contains("蛋")`→蛋、`contains("肉")`→红肉。**"蛋挞"命中"蛋"、"肉桂"命中"肉"、"蘑菇菜"同时命中菌/菜**（靠代码顺序 first-match，脆弱）。已有尾词优先 + `NAME_OVERRIDE` 缓解。
- `HealthRuleEngine.seasoningCaution`：`contains("糖")`→糖 caution，**"糖醋"会命中"糖"**而非酱油。
- `RecommendationDataSource.classifyPairRole`：`cats.contains("蛋")`→EGG，分类名含该字即命中。
- `RecommendationDataSource` 库存按名扩展 `selectIngredientIdsByNames`：**"鸡蛋" vs "鸡蛋粉"** 子串式扩展可能误纳（需确认该 SQL 是精确名还是 LIKE）。
**建议**：为这些分类/caution 函数补**跨类冲突单测**（蛋挞/肉桂/糖醋/鸡蛋豆腐），锁定期望，防关键词改动回归。判定本身用「override 表 + 尾词」已是务实方案，保留但加测试网。

### 🟡 [风险:中][体量:S][健壮性] LLM 输出解析：无效 dishId / 空 meal 静默丢弃且无告警
**[可直接修]** `RecommendationParser.kt` + `PlanOrchestrator.parseAiDays`(line ~136-150)：`rm.dishIds.mapNotNull { byId[it] }`，某餐全部 dishId 无效 → 该餐静默变空 → 某天可能缺一餐（后由 ruleDay 兜，但不保证）。`reason.trim()` 依赖 `@Serializable` 默认 `""`（当前安全）。
**建议**：解析后统计「丢弃了 N 个无效 dishId / M 餐」写 diag 日志；`RecommendationParserTest` 补 malformed 用例（`dishIds:null`、`["abc"]`、空 suggestions、超大 id）。

### ⚪ [风险:低][体量:S][健壮性] `NutritionCalculator.resolveGrams` 未防负 grams
`Nutrition.kt:136-165`：`grams = qty * unitGrams`，若 `qty<0`（当前 UI 应拦但无领域断言）会产生负营养。`grams==0` 安全。**建议**：`resolveGrams` 对结果 `coerceAtLeast(0.0)` 或 `require(qty>=0)`；补 0g/负值边界单测。

### ⚪ [风险:低][体量:S][健壮性] `avoid` 权重 50.0 量级远超其他（0.2~1.0）
`HealthRuleEngine`：`weights.avoid=50.0` 意在把忌口菜「压到最后」。数值合理（要压过所有正向加分之和），但**无注释论证「为何 50 足够」**——若未来正向权重累加超过 50，忌口菜可能重新冒头。**建议**：改用 `Double.MAX` 级别的显式 `AVOID_PENALTY` 或注释「必须 > 所有正向权重之和」并加断言/单测锁定。

---

## 六、值得补的关键单测

| # | 目标 | 覆盖点 | 现状 |
|---|------|--------|------|
| 1 | **seed 引用完整性（生产路径）** | dishes/nutrition/care/crowd 按名解析缺失项的**告警而非静默** | 仅 `validateXxxForTest` 在测试跑，生产无 |
| 2 | **FoodGroup.classify 跨类冲突** | 蛋挞/肉桂/糖醋/鸡蛋豆腐/蘑菇菜 → 期望大类锁定 | happy-path only |
| 3 | **seasoningCaution 歧义名** | 糖醋/盐糖/复合调料 → 期望 caution | 仅标准名 |
| 4 | **RecommendationParser 脏输入** | `dishIds:null`/`["abc"]`/空/超大 id/非法 JSON | happy-path only |
| 5 | **NutritionCalculator 边界** | 0g / 负 qty / 全 null 营养 / 单料缺 unitGrams 走 DEFAULT_PIECE_GRAM | 主路径已测 |
| 6 | **迁移升级链推演** | 从 v9/v10/v11 各历史版本升级到 v23 不崩（`ensureLegacyColumns` + 10.sqm no-op + 12.sqm 重建） | 单测走 Schema.create 不跑迁移链（红线），需专门推演 |
| 7 | **seed 指纹敏感性** | 改 dishes.json 内容 → 指纹必变；改逻辑必 bump SEED_LOGIC_VERSION | 无 |
| 8 | **avoid 权重不变量** | 忌口菜恒排最后（正向权重上限 < AVOID_PENALTY） | 有 avoid 保留测试，无量级不变量断言 |
| 9 | **StateFlow 写回并发** | Picker init 多 loader 并发不丢字段 | 无 |

---

## 七、建议优先修 Top 8（按性价比）

| 序 | 优先级 | 项 | 类型 | 一句话 |
|----|--------|-----|------|--------|
| 1 | 🔴 | seed 按名解析缺失项加 `CookbookLog.w` 告警（§二.1） | 可直接修 | 一处一行日志，生产终于能发现「扩菜没生效」 |
| 2 | 🟡 | FoodGroup / seasoningCaution / Parser 补冲突&脏输入单测（§六.2/3/4） | 可直接修 | 锁住脆弱 by-name 判定，防关键词改动回归 |
| 3 | 🟡 | deleteDay / yearAverages 等静默 runCatching 补 `onFailure` 落日志（§一.1） | 可直接修 | 破坏性操作失败可见 |
| 4 | 🟡 | 分类树 `countChildren` N+1 → 一条 GROUP BY 子查询（§三.1） | 可直接修 | 最典型残留 N+1，改一处收全部分类页 |
| 5 | ⚪ | Cookbook.sq 版本注释 v1→v23（§二.4） | 可直接修 | 5 秒改，止住排查误导 |
| 6 | 🟡 | applyIngredientGroup 批量/并发建食材（§三.4） | 可直接修 | 配料组套用不再逐项串行卡 |
| 7 | ⚪ | 确认 dish_ingredient/meal_record_dish 的 UNIQUE 与 REPLACE 语义（§二.6） | 待确认 | 排除 seeder 重复行累积 |
| 8 | ⚪ | 新写派生逻辑统一 today 提参 + `_state.update{}` 写回（§一.2/3） | 可直接修 | 巡检 `\.value = \w+\.copy(`，收敛可测性与并发 |

**需拍板（不建议顺手做）**：IngredientPickerViewModel 拆分（§四.1）、双份分类逻辑收敛（§四.4）、seed 指纹粒度改造（§二.2）、debug 包 seed 后跑引用校验（§二.1.2）——均属架构级，宜单独立项评估。

---

*说明：本报告已剔除子代理的若干误报——AddMealViewModel.save() 的快照+`!!` 是有意不变量非 bug；HomeViewModel 营养计算是委托 domain 静态方法非逻辑泄漏；NutritionCalculator 除零/null 处理完善。所有「待确认」项已如实标注，未编造不存在的问题。健康/营养数据本身为 AI 参考整理、非权威核对（项目已声明免责），不在本次代码质量审核范围。*
