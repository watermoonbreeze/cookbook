# AUTOGEN-UNIFY B1 审核增量台账

> 蓝图：`docs/feature/自动入库统一管线_实施蓝图.md`（DRAFT）
> 规则：`experience/12_多模型协作与实施蓝图规范.md` §10.1——逐检查点追加，不等最终结论；已标 PASS 的检查点不重复扫描。
> 状态图：DRAFT →（审核+挑战+用户拍板决策点）→ BLUEPRINT_READY

## 台账头

| 项 | 值 |
|---|---|
| 审核启动 | 2026-09-03（会话内） |
| 审核范围 | 蓝图 DRAFT 全文 + §2 事实地图所列代码 |
| Reviewer ① | google_architecture_engineer（架构规范审）·进行中 |
| Reviewer ② | google_quality_engineer（质量+测试完备性审）·进行中 |
| Reviewer ③ | apple_software_behavior（行为透明/Tier 审）·进行中 |
| Reviewer ④ | GC-37 独立挑战者（只给蓝图不给设计理由·10 项固定挑战清单）·进行中 |
| 用户待拍板 | 决策点 1（大类权威：推荐 A=AI 优先+合法性校验）/ 决策点 2（营养值协议扩展：推荐 A=本批只留管线位） |

## 检查点记录（追加式）

### 2026-09-03 · Reviewer ③ apple_software_behavior（行为透明审）——已回·判定「需修订」

**总判定**：需修订（架构方向背书，不推翻；两个决策点 A/A 均背书）。

**代码事实三则**（reviewer 实读新发现，后续 reviewer 与编码批次共享）：
- 事实甲：`ingredient_nutrition.ref` 全 androidApp **无 UI 读取**（「营养为自动估算」文案均硬编码：`IngredientDetailSheet.kt:309`、`IngredientPickerScreen.kt:294`、`AiMealInputViewModel.kt:909`）。
- 事实乙：详情页复核横幅门禁失配——`IngredientPickerScreen.kt:684` 判 `source == "auto"` 而管线写 `source = "ai"`、待复核 Tab 筛 `source = 'ai'`（`Cookbook.sq:1812`）→ 横幅对管线食材**永不显示**（既有潜伏 bug）。
- 事实丙：回收站 `selectInactiveUserIngredients` 只列 `source='user'`（`Cookbook.sq:840`）→ **'ai' 食材删除后不进回收站、无恢复入口**；本批把快速自建食材从 'user' 切到 'ai' 会**放大**此缺口（删除不可逆=回归面）。

**结论清单**（Tier）：
| # | 行为 | Tier | 结论 |
|---|---|---|---|
| 1 | AI 判大类落库 | T1 | PASS（食材级 source='ai'+review=0+编辑器可改即留痕；预览页已有事前告知 `AiMealInputSheet.kt:903`） |
| 2 | ref 二值 | T1 | **AF-AU-B1（阻断）**：E-AU-01 判据「ref 显示」真机不可见；§5 ref 读取方与代码不符；措辞原则=用户可见层不并列二值，收敛「估算值·建议核对」，provenance 只进编辑器 banner |
| 3 | 菜名预选做法+Snackbar | T1 | **AF-AU-B2（阻断）**：去重/复位规则未定义（须 cookingMethodTouched 锁+去重键）；**字→字典条目映射未定义**（18 字中仅 9 字在字典：拌不在〔字典是"凉拌"〕、烧/熘/焗/烩/涮/煲/炝/熬 共 8 字不在——「预选第一项」会落空）；Snackbar 与既有「已按菜名添加 N 项」竞争覆盖——推荐改 **v28 内联提示行范式**（`NewDishScreen.kt:342-349`） |
| 4 | 管线建库升级 | T1 | PASS（留痕面比现状更多：待复核 Tab）；提示热量口径变化属改善 |
| 5 | 回填扩源 | T1 | PASS（幂等只填空）；**必改**：GC-20 副作用清单补回填条目 |
| 6 | 幂等/撤销 | — | 防覆盖矩阵完备；**台账登记**：事实乙横幅失配（一行修）+事实丙回收站缺口（查询放宽非 preset）——明确归宿须用户拍板（已升为蓝图决策点 3） |

**已执行**：蓝图按 AF-AU-B1/B2+必改项就地修订（见蓝图 v1.1 修订记录）；事实乙/丙升为决策点 3 待用户拍板。

### 2026-09-03 · Reviewer ④ GC-37 独立挑战（10 项固定清单）——已回·阻断 3

| # | 挑战项 | 裁定 |
|---|---|---|
| 1 | groupHint 非 MultiDayRecorder 路径答案 | CONFIRMED-FINE（§5+STEP 已唯一自洽） |
| 2 | groupLabel 两构造处 | CONFIRMED-FINE（STEP「两分支」显式覆盖；blank 分支天然双 null） |
| 3 | REUSE 路径「预览落库同源」 | MINOR-NIT：库列不被污染（INV-AU-06），但 AutoGenContext 未预取 food_group 列、REUSE 展示口径与库内真值可割裂——§5 加例外注记 |
| 4 | ensureCreated runCatching 统一丢弃 | MINOR-NIT：语义保留非回归，但应补 onFailure 一行日志（T1 留痕，对齐 loadIngredientGroups 式样） |
| 5 | STEP-AU-5.5「未手动改过」判定载体 | **CONFIRMED-ISSUE**：NewDishUiState 无 cookingMethodTouched（有 mealSlotTouched 先例 :28-63 实读）；add/remove/clearCookingMethod（:415-444）均不设标志；baselineSig 混合无法区分——STEP 不可机械实现。修复=§4 立项字段+STEP 拆 5.5a（三入口设 true）/5.5b（守卫读它） |
| 6 | 回填扩源含 link 不可测分支 | CONFIRMED-FINE（单一 IN 条件可造夹具；建议 T-AU-09 顺手加 link 行断言） |
| 7 | seasoning→null 与 STEP-AU-3.1 | **CONFIRMED-ISSUE**：seasonings 分支变量是 DraftSeasoning（NdjsonEvents.kt:198-203）**没有 foodGroup 字段**——蓝图三处（§4/STEP-AU-3.1/allowlist）写「两分支都填」是错误事实假设，编译不过且与 allowlist 冲突。修复=三处删 seasonings 分支（保持 FoodJson(name=s.name) 现状）+决策点1 补边界注记（AI seasoning 提示不参与 isSeasoning，库外新调料默认克数靠名字关键词兜底，留后续批次） |
| 8 | stale ctx 与 dedup 双保险 | CONFIRMED-FINE：createUserIngredient 查重直查 DB 独立于 ctx（IngredientRepository:107-117），stale 不重复建行——但核验中发现 **A1** |
| 9 | T-AU-04 断言物 | CONFIRMED-FINE（内存直传无序列化往返；MultiDayRecorderK1aTest 有完整先例）；微瑕：SemanticIngredient.groupHint 是 private 中间产物不可观察——断言物改 previewAll() 输出的 IngredientPreview.group |
| 10 | 查询更名破坏面 | CONFIRMED-FINE（全仓真实调用仅 Cookbook.sq:2069 定义+PresetDataSeeder.kt:104 调用；纯查询零迁移） |

**附加发现 A1（CONFIRMED-ISSUE·阻断）**：commit CREATE 分支在 stale ctx 场景——`createUserIngredient` 内部复用已有 id（:113-116）后，蓝图的无条件 `upsertNutrition`（覆盖式 NutritionRepository:52-59）+`setFoodGroup` 会**覆盖用户刚手填的值**（编辑会话中切食材管理建同名食材再返回保存=可达窗口）。蓝图"会话内不重复 load"的 ctx 生命周期决策缺配套守卫。修复=STEP-AU-2.2/2.3 增"CREATE 返回 id 后写前查既有值，非空不覆盖（对齐 seeder 只填空纪律）"+新 T-AU-13。
**附加发现 A2（笔误）**：`setFoodGroup(id, preview.group)` 类型不符——setFoodGroup 收 String（IngredientRepository:303），须写 `it.name`（DB 存枚举名）。

**汇总**：10 项=7 FINE + 2 NIT + 2 ISSUE + 附加 A1 ISSUE + A2 笔误 → **阻断 3**（#5/#7/A1）。转 BLUEPRINT_READY 前消 3 阻断+2 NIT+1 笔误。

### 2026-09-03 · Reviewer ② google_quality_engineer（质量+测试完备性审）——已回·判定「需修订」（4 阻断/7 建议/5 可选）

**阻断**：
| AF | 问题 | 唯一最小修复 |
|---|---|---|
| AF-01 | **FLAT 整体 JSON 回退路径同样丢 food_group**（`FlatToDayMealConverter.kt:105-107` 不构造 FoodJson；FLAT prompt :76,78 明确要求 AI 输出 food_group——数据到了被扔；生产可达：`StreamingMealParser.kt:649-654` NDJSON 失败回退）。蓝图 §2 F5 只列 NDJSON 一处断链 | （推荐 a）STEP-AU-3 增 3.3 补 `FoodJson(name, food_group)` + allowlist 增该文件 + 新 T-AU-13 |
| AF-02 | 「未手动改过」无载体（=挑战5）+ **预选须整组替换而非 addCookingMethod 追加**（连续改菜名会累积 [蒸,烧]）+ Snackbar 触发跟随「本次推演真的改变了集合」 | §4 补 touched 字段+写入点；预选对齐 `updateMealSlotPreselect:328-334` 整组替换范式 |
| AF-03 | **allowlist 漏 `AndroidModule.kt:62`**（NewDishViewModel 构造 6 依赖的 Koin 传参行）——照 allowlist 干活编译卡死 | allowlist 增一行；§1 非目标措辞改「仅 SharedModule 新增注册+AndroidModule 一行传参」 |
| AF-04 | **测试恒真**：T-AU-01 夹具「低脂牛奶」+DAIRY，但 classify 也返 DAIRY（`FoodGroup.kt:34` 牛奶关键词）——实现不用 groupHint 断言也绿；T-AU-07 非空即过 | 夹具硬性约束「classify(name) ≠ groupHint」（如 豆腐 BEAN + hint DAIRY）；T-AU-04 断言下移 `FoodJson.food_group`；T-AU-07 钉具体值（group 枚举+ref） |

**建议**：S-01（=挑战附 A1，CREATE 内部复用覆盖，setFoodGroup 仅空列守卫）、S-02（=挑战7 DraftSeasoning 无字段）、S-03（§8 夹具口径改「RepositoryTestDatabase 真库+真实 repo」——既有基建非 fake，且 generator/repo 均 final 类）、S-04（T-AU-12 不可行：androidApp 无 newdish 测试目录；推荐 INV-AU-11 证据改 grep 判据+真机，删「NewDishViewModel 既有测试全集」幻影基线）、S-05（T-AU-10 空转：RuleMealParser 回归集不含 cooking_methods 断言，补 `parse("清蒸鲈鱼")→["蒸"]` parser 级新测）、S-06（NutritionGuessSource 消费点实为**三处**：DishAutoGenerator:96 / IngredientEditorDialogs:242（is None）/ :1136-1140（唯一 exhaustive when））、S-07（=挑战3 REUSE label 展示值注记）。

**可选（结论性）**：O-01（=A2 笔误 `.name`）、O-02（`IngredientPreview.group` 须带默认值 null，否则空名分支 :43-54 构造点编译断）、O-03 性能 PASS（懒加载 io 线程会话一次；逐项 4-5 条 SQL 为业务固有非 N+1）、O-04（ctx() 必须在 runCatching **内层**，防 load 失败崩 save——语义等价性措辞要求）、O-05 管线位安全 PASS（附准入条件：下一批引入 nutritionHint 生产写入者前，preview 必须先加区间/合理性校验门槛）。

**评价**：事实地图 F1~F11 逐条核实无误；决策点 1 词表收窄论证成立（10 枚举 vs 9 词、7 映射核实）。四项阻断均蓝图文本级修订，不动架构决策。

### 2026-09-03 · Reviewer ① google_architecture_engineer（架构规范审）——已回·判定「需修订」（3 阻断/7 建议/7 可选）·**四审全部完成**

**阻断**：
| AF | 问题 | 唯一最小修复 |
|---|---|---|
| **AF-AU-01（最高优先·独有发现）** | **source 语义翻转**：ensureCreated 走 commit CREATE 硬编码 `source="ai"`（IngredientAutoGenerator.kt:128-132），快速自建现状默认 `"user"`——触发三处门控回归：①删除按钮消失（`IngredientCard.kt:75` `canDelete = source=="user"`）；②回收站丢失（`Cookbook.sq:840`）；③**备份导出丢失**（`Cookbook.sq:1785` `selectSyncUserIngredients` 只导 'user'——触碰完整备份红线） | `ensureCreated(name, ctx, source: String = "ai")` 透传到 createUserIngredient（签名本就支持）；NewDishViewModel 两调用点显式传 `"user"`；§5 增 source 行；新 T 断言两点建库 source=='user'、AI 记餐仍 'ai' |
| AF-AU-02 | =挑战7/质量S-02（DraftSeasoning 无字段，STEP 3.1 不可编译） | 同前修 |
| AF-AU-03 | =质量AF-03 + 冻结参数清单：NewDishViewModel 构造需 **+3 参数**（gen/db/aliasResolver），`AndroidModule.kt:62` 必改 | STEP-AU-5.1 冻结三参数；allowlist 增 AndroidModule |

**建议**：S-01（flat 断链=质量AF-01，**补充：`buildSyntheticLinesFromResolvedDay`（StreamingMealParser:640-642）合成行也不带 food_group——需把 StreamingMealParser 的 forbidden 收窄为「仅合成行增 food_group」**）、S-02（ctx() Mutex 单飞防双 load；ensureCreated commit 前精确查库消除陈旧 CREATE 碰撞——与质量审 A1 守卫互补双保险）、S-03（=B2+**第四入口 selectCookingMethod**；Snackbar 槽位问题 v1.1 已改内联提示行解决；提示行落点 NewDishScreen 需入 allowlist）、S-04（T-AU-12 真库模式）、S-05（=挑战9 T-AU-04 公开断言点）、S-06（§5 补第4写入者：seeder preset 覆写式刷新 `PresetDataSeeder.kt:100-103`；ingredient_nutrition 手动编辑写入 `IngredientPickerViewModel.kt:744`）、S-07（决策点1 风险补两条：**FUNGI 回归**——NDJSON 词表无 fungi，菌菇 AI 给 vegetable 压过 classify 的 FUNGI 判定，挂「蔬菜类」非「菌藻类」+GROUP_AVG 25/30 千卡差；**AI 大类不可溯源**——列值无法区分 AI/本地判）。

**可选**：⚪-1（=A2 `.name`）、⚪-2（group 默认值=O-02）、⚪-3（groupLabel 生产 UI 零读取仅测试断言——可改 body 计算属性根除双真相源）、⚪-4（F1 措辞：createUserIngredient 本身支持 categoryId，「零归类」仅对裸调点成立）、⚪-5（「唯一合法调用方」改「新增调用方须过架构审」——L4 即将复用）、⚪-6（行号漂移：FoodJson.food_group 实在 UnifiedMealSchema.kt:33；过期注释实在 :766）、⚪-7（E-AU-01 判据弱注记）。

**正面结论**：模块边界全部正确（Map 放 ai/meallog 依赖方向合法、Inferrer 放 domain 判据符合、映射在 MultiDayRecorder 适配边界正确）；决策点 2A 干净；GC-11 构造点核查属实；18 字表逐字一致。核心设计站得住，三阻断均蓝图文本级修订。

**四审汇总**：行为审（需修订·2阻断）+ 独立挑战（3阻断）+ 质量审（4阻断）+ 架构审（3阻断），去重后**独立阻断 8 项**：①source 语义翻转（架构）②seasonings 指令不可编译（三审同发现）③AndroidModule 漏列（两审同发现）④flat 回退路径漏修（两审同发现）⑤touched 载体缺失（三审同发现）⑥测试恒真（质量）⑦CREATE 陈旧碰撞覆盖（两审同发现）⑧E-AU-01 判据不可见（行为）。全部蓝图文本级修订，架构决策零推翻——v1.2 逐项落入。

### 2026-09-03 · v1.2 修订落地记录（ARCH）

四审全部合入，蓝图现状态 DRAFT v1.2：8 阻断全消（①ensureCreated source 参数+两调用点传 "user" ②STEP 3.1/allowlist 改仅主料分支 ③AndroidModule+三参数冻结入 STEP 5.1/8.1 ④STEP 3.3/3.4 flat+合成行 ⑤§4 四入口 touched+整组替换 ⑥§8 判别性硬约束+真库口径 ⑦入口防陈旧+CREATE 仅空列守卫双保险+T-AU-14 ⑧E-AU-01 判据改待复核 Tab+局限注记）；建议/可选项全部吸收（REUSE 注记/失败日志/计算属性 groupLabel/真相源补录/FUNGI+溯源风险/T-AU-15/E-AU-05/基线删幻影/STEP-AU-10 决策点3 顺手修）。**剩余唯一步骤：用户拍板决策点 1/2/3 → BLUEPRINT_READY**（可选：AF-04/AF-01 修订处抽查复验）。

### 2026-09-03 · 用户拍板 + 编码交付（ARCH/CODE 同会话）

- **用户拍板**：决策点 1=A（AI 优先+合法性校验）、2=A（本批只留管线位）、3=A（门控缺口顺手修）→ 蓝图 **BLUEPRINT_READY** 冻结。
- **编码完成**：STEP-AU-1~10 全落地（勾销表见蓝图 §9）；三命令 BUILD SUCCESSFUL；新测试 15 用例（AutoGenUnifyTest 9 + NdjsonFoodGroupMapTest 6）全过；真机清单 `202609031105` 登记 E-AU-01~05。
- **编码期就地修订两处**（台账登记）：①STEP-AU-2.4 入口精确查库简化为 commit 内双守卫（createUserIngredient 内部去重已覆盖，避免向 forbidden 的 IngredientRepository 加查询）；②STEP-AU-5.5 增加"预选到做法时自动展开更多信息折叠段"（NewDishScreen·LaunchedEffect——否则预选藏在折叠区不可见）。
- **终审**：google_quality_engineer 编码后终审——**1 阻断（AF-1）+5 建议+4 可选**：
  - 🔴 AF-1：T-AU-04 缩链（直接构造 DayMealJson 跳过 mapper 段→主路断链修复本体 `MealStreamDraftMapper.kt:104` 零测试覆盖）。**已修复**：补 T-AU-04a 用例（MealStreamDraft+segments → MealStreamDraftMapper.toDayMealJson → 断言主料 FoodJson.food_group=="dairy" 且调料分支 null）。
  - 🟡 S-1（ensureCreated 漂移）/S-2（cookingHintKey 替代）：蓝图修订记录 v1.3 正式登记采纳。
  - 🟡 S-3（applyPrefill 漏做法预选）：**已修**（markBaseline 前补 updateCookingMethodPreselect）。S-4（字典未就绪漏预选）：**已修**（availableCookingMethods.isEmpty() 显式 return+防抖自然补偿）。
  - 🟡 S-5（T-AU-09 回填写入断言）：延后下批（private 函数+公开入口过重·理由入蓝图 v1.3）。
  - ⚪ O-1~4（全表单值取用可接受/LaunchedEffect 必要配套/合成行无 T-ID 属蓝图缺口/预选进 contentSig 无伤害）：登记不阻断。
  - 终审确认：生产代码正确性/并发/边界/资源**零阻断**；21 标记文件全在 allowlist 零越界；forbidden 冻结点实读未动。
- **修复后复验**：AF-1/S-3/S-4 修复后重跑 `:shared:testDebugUnitTest` + `:androidApp:testDebugUnitTest` + `:androidApp:assembleDebug` → **BUILD SUCCESSFUL**；NdjsonFoodGroupMapTest tests=7 failures=0（T-AU-04a 已入）——**阻断清零，终审通过，批次可交付**。
