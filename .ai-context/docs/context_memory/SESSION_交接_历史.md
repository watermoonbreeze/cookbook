# SESSION 交接历史（append-only 索引·每次交接追加一行）
- 2026-08-05 · `未提交` · 补齐周期记/NDJSON 的开发与验收基线；新增 `.ai-context/PROJECT.md`，收敛跨模型首读入口并将根 `docs/` 历史资料归档到 `.ai-context`。
- 2026-08-05 · `待提交` · AI记一餐V2验证收口+同日误关重开保留会话；已形成 `AI记一餐_周期记_NDJSON流式改造落地方案.md`，下个干净 session 按“周期记+NDJSON流式解析”分批开工。
- 2026-07-28 · 4Bug修复+文档（嘌呤数据驱动+K3搜索清空+K5备注显示+K6 allIngredientNames·7文件·真机待验清单49项）· `adcb1776`
- 2026-07-27 · 菜品编辑器主料分级（评估→UX→编码·2文件·isMain DB已有/UI接线·构建+单测通过）

> 主交接文件 `SESSION_交接.md` 只留当前状态；本文件是**历史速查索引**——每次交接在**顶部**追加一行（最新在上）：`日期 · 末位commit · 一句摘要`。要看某次完整交付：`git show <commit>` 或 `git log --oneline` 定位。

- **2026-07-26** · 战略/交接 session（多 docs commit·未 push） · **竞品全方位对比三视角会诊 + 功能路径索引 + AI-交接文档 + 配置导出给 Codex**。会诊(PM+运营+架构)收敛三张单：🥇P0 成员化健康红绿灯(家庭×慢病可感化·底座已成) + 🥇P0 引擎正向兑现到推荐质量(多成员折算贯穿推荐+忌口扩正向推有利菜) + 🥈P1 生命阶段调养做透一个场景；补短板=条码优先不追识菜；该弃=拍照识菜all-in/大库/超市/UGC/热量主叙事/CMP-iOS。产出 `核心竞争价值.md`+`全方位竞品对比_侧重亮点深挖.md`+`功能路径索引.md`(AI-terse·全局规范+/myinit B4)+`AI-交接文档.md`(Codex入口)。摄入模型:餐食系数已归一(无1.8问题)·改占比%呈现入待办(纯UX修)。配置导出 `~/.claude-config-export-20260726_155826/`(22agents/5skills/5commands+Codex迁移指南)。⏭ 战略三张单各单开session。
- **2026-07-25** · `e51ee46`(P3主体)+`de87d7da`(A+B)+`83202f8d`(青菜待办) · 算法·权威化重审 **P3 推荐份量对齐膳食宝塔**（会商算法+UX+产品拍板"呈现补齐+口径收敛"·Google终审无阻断·双绿·未push）：**克数选菜/进度条/达成度=数据不可靠+焦虑+免责红线·会诊明确否决不做**;权威每日克数份量只在参考页详列。**③口径收敛**(核心)：周计划营养线`NutritionLine`缺口口径 自创三支柱→**膳食宝塔四正向层**(`pillarGapDays→layerGapDays`·`GAP_PILLAR→GAP_LAYER`·引用`DietaryGuideline`单一真相源·advise显式按POSITIVE_LAYERS序tie-break)→全App单一宝塔结构口径·能识别"顿顿有肉却不喝奶不吃豆"的奶豆坚果缺层。**①单餐入口补齐**:库存/随机推荐(`AiRecommendScreen`)补餐次结构建议行(`MealStructureHintRow`·与AiPlan逐字同款)=份量对齐覆盖所有推荐入口。**②权威份量轻入口**:推荐页+周期计划页脚+今日卡"各类每天吃多少›"(`DailyAmountRefLink`·internal跨包复用)→膳食参考依据页。**A+B追加**:A=NutritionLineTest补已排周7天窗口回归(锁WeekPlan/AiPlan共享消费口径·不搭androidApp VM架=反过度设计)·B=今日卡轻入口(不动6处共用DayMealCardView)。文档:对照doc三自创点全✅·待办第50行·UI方案§9.42·营养线方案P3注。**权威化重审三大自创点(P1色系墙/P2餐次/P3份量)全部收官。**⏭ 剩🟡深化项(EER/生命阶段/多样性/微量营养素·各单开会商)+青菜泛称食材补录(数据session)。
- **2026-07-25** · `bac5dad`+`5950a00`(代码)+交接commit · 算法·权威化重审 **P2 餐次差异化**（会商算法+UX+Google终审无阻断·双绿·未push）：推荐引擎按**餐次期待层**(权威`DietaryGuideline.MealEnergyShare.expectedLayers`)挑菜——`MealCompositionScorer` 加纯函数 `candidateLayers`(FoodGroup.classify→宝塔层·永不空集)+`compositionBonus` 餐次差异分支【①补缺口+0.5 ②晚餐纯肉轻降-0.3·**收窄仅纯ANIMAL且该餐不期待荤**·不误伤豆腐/豆浆/牛奶/合炒·甄别修正 2b 原设计缺陷】·默认参不传=旧行为(兼容开关)；`PeriodPlanner`(精确餐名)/`RecommendationOrchestrator`(单餐早=BREAKFAST/非早=LUNCH)接期待层·coveredLayers外提消热点。UI(apple_ux门禁·克制)：推荐页每餐静态结构建议行(读hint)+页脚口径尾注·今日卡「缺蔬菜/早餐缺蛋白」阈值提示(鼓励非评判·一处一条·可关)·新开关`MEAL_STRUCTURE_HINT_ENABLED`(默认开)。测试:8纯函数+1 plan()集成(晚餐不以纯肉打头·荤≤午·确定性)。文档:对照doc餐次差异化→✅已权威化·待办第50行P2完成+6项留待办·UI方案§9.41范式。⏭ **P3 推荐份量对齐宝塔`DAILY_AMOUNTS`+J17营养线统一(换session会商L)**。
- **2026-07-25** · `481c643`(代码)+交接commit · 算法·权威化重审 P0+P1：**膳食宝塔权威真相源下沉 + 色系墙均衡评级对齐宝塔四层**。P0=shared 新建 `DietaryGuideline`(commonMain·宝塔五层份量`DAILY_AMOUNTS`+三餐能量分配`MEAL_ENERGY_SHARES`含各餐`expectedLayers`+多样性/饮水+九大类→宝塔层`LAYER_OF_GROUP`+`coveredLayers`·与UI层`DietaryReference`同源·`DietaryGuidelineTest`7用例)；P1=`FoodGroup.nutritionLevel` 自创三支柱→**宝塔四正向层覆盖度**(谷薯/蔬果/鱼禽肉蛋/奶豆坚果·0-4 API与label不变·6处消费端零改·`FoodGroupNutritionLevelTest`同步·奶豆坚果成独立层→正餐营养优变少但全天照样达4·守色系墙"纯结构不关联热量慢病"红线)。双构建单测绿。**剩 P2 餐次差异化(消费侧:推荐引擎按餐次差异化+per-meal呈现·`MealCompositionScorer`现零餐次差异)/ P3 推荐份量对齐宝塔(连J17)=待办点名"会商+UX"·单开会商 session 做**。文档:对照doc标色系墙✅已换、待办第50行记进度。未 push。
- **2026-07-22** · `c920973`(代码) · Bug 修复 session：3 真机 bug 全修+push——①今日卡"吃了多少"弹层不刷新/退出重进才生效/不能滑动(根因=todayCards combine 透传 observeTimelineWindow 旧cards·eaten_ratio令牌不重建·stateIn去重白并→令牌**下沉进 observeTimelineWindow**重跑buildDayMealCards+sheet verticalScroll+删死代码令牌)②编辑加菜后"少量"消失(单测证实**数据没丢**·加菜后整餐混合态不高亮→saveDayMeals新菜**继承本餐统一吃完度**·待用户确认此产品语义)③食材"100.0个"+营养按错单位(加食材unit_id存NULL→`saveDish`空单位**回填克**·顺带修小剂量调料营养放大)。测试+6(含营养不放大回归网)·CLAUDE.md红线+2(combine令牌透传反模式/存菜unit_id别留NULL)·experience/06+3笔(含"单测复现取真相"元经验)。Google门禁×2无阻断·双构建绿。**+待办分类重整**(顶部按session类型导航层+登记5项用户反馈)。⏭off-type队列:我的页归类/两编辑页统一/营养走势三线/餐状态机/数据核准。真机待验(GCL)。
- **2026-07-22** · `9c9f101`+`bcf835c`+交接commit(代码) · 功能 session：**是否吃完(食用比例)per-dish 全功能**(五方会商→拍板现在做→`meal_record_dish.eaten_ratio`/31.sqm/`IntakeCalculator`单一真相源/今日卡+报告接入/`observeEatenRatioChanges`令牌/`EatenAdjustSheet`四档整餐+分菜折叠/saveDayMeals快照回填防丢·方案`食用比例吃完度_摄入会商方案.md`) + **三开关默认关→默认开**(热量数值/营养色系/分步执行·透明opt-out·`PreferenceKeys.DEFAULT_*`集中·首启告知一屏·旧热量默认关红线更新) + **口径修复**(今日营养vs今日餐食热量不对应·`DayMealCardView`由distinct少算同菜多餐次→对齐逐实例+IntakeCalculator·真机07-22验过) + 透明文案修 + 测试(IntakeCalculator/DietReport折算/MealRecordRepo数据保全2条)。google门禁无阻断+/code-review无新发现+双构建绿+真机迁移无损验证(设备GCL·version32·eaten_ratio列+1098行全1.0)。off-type队列:餐状态机/两编辑页统一/营养走势三线/健康数据核准。
- **2026-07-22** · `417e0a9`(代码) · 数据/编辑 session：菜品食材剂量默认值专项——P1 修加食材单位丢失race(gramUnit未加载→100g落到计件个/只·`unitsReady`+`cachedGramUnit`+await+移除defaultUnitId错兜底) + P2 分类智能默认克数(FoodGroup.classify·蛋50/菜150/奶200) + P4 幂等修历史错单位数据(quantity=100且unit空/计件→gram·`SEED_LOGIC_VERSION`v10·真机库副本验证) + changelog v2 透明告知。google门禁无阻断+独立/code-review无新bug·双构建绿。off-type新登记:两编辑页UI统一、营养走势三线同显+周月评估。**续 `95acf72`**:A1 修自建菜空用量配料(0营养·seeder 幂等按分类补默认克数+克单位·仅source=user·v11·加回归测试) + A2/C 查证关待办(dish_ingredient 有 UNIQUE 索引·RANDOM IN 999隐患早已修=先查证不虚做)。
- **2026-07-22** · `6c47381`(代码) · UI session(续2)：饮食报告营养走势折线(`6c47381`·宏量克数单条+三色切换chip·96dp·min=0归一·断线分段·周画点月不画·锚点轴标·空/少数据三态·个人视角专属·§9.40·apple_ux_designer+Google 门禁+防漂移单测·shared NutritionTotals.times+DietReport.perDayNutrition 逐日序列作单一真相源)。本 UI session 主线两批(餐次简洁化+文案通用化 / 营养折线)全清·剩 off-type 队列。
- **2026-07-22** · `339383c`(代码) · UI session(续)：餐次界面简洁化四改(`339383c`·⋯ActionSheet 收纳低频/加菜主次按钮/展示·操作分区半透 hairline/常吃chips归操作区·§9.39·Google 门禁无阻断) + 周计划营养卡文案通用化(NutritionLineCard「这份计划的营养搭配/整套合计/这几天安排了」修口径bug + AiPlan空态去"这一周")。⏭报告营养趋势折线(批2·按 `营养趋势曲线方案.md` Step1·大)。
- **2026-07-22** · `f6b1cf5`(代码) · UI session：记菜命中慢病轻提示 #177③(`834789a`·shared `MealHealthHintUseCase`+`MealConcernKind`+单测·4门禁) + AI推荐"整份·全家分食"口径注(`f6b1cf5`·`PORTION_HINT`+ⓘ弹窗·守热量红线) + 营养趋势曲线方案&拍板(只放饮食报告+宏量三色·周计划不做·`营养趋势曲线方案.md`) + 餐次界面简洁化UX方案(三入口分层+展示/操作分区+四改·§9.39·待编码) + 多新待办(食材剂量默认值专项/是否吃完影响摄入会商)。⏭先编码餐次简洁化+文案通用化。
- **2026-07-22** · `c255aa4` · L3 属性双层判定 UI(`b2f565b`) + 管理页误选中 bug 修(`efdb9cc`) + 编辑器封面上移&名称同行(`c255aa4`) + 运营#177方案(`e13f2f1`) + 确立"一个session一类"全局规则 + 交接协议改为纯当前状态。A#6-B 暗因子已摸底未实现。
- **2026-07-22** · `6a08ecf`(代码) · 数据体系大轮：数据扩充(satFat/purine/gi)+全数据源交叉核对+资产化cron + 数值×属性双层标准 + 属性标签体系(`FoodAttribute`/`FoodAttributeCare`) + 加工食品入库 + 食材数据全处理。经验已总结(第41次)。
- **2026-07-21** · `4cf9ddb` · 确定方案 D 批 + 算法拍板批(B#6/C#F1/C#F2) + 周计划营养线全链路(domain+AiPlan概览卡) + AiPlan界面统一P1 + 推荐带营养素热量 + 拍照迁全屏查看器。
- **2026-07-21** · `4ac74bf` · 营养线三件套(AiPlan逐日卡营养行/菜品编辑分类折叠/WeekPlan周营养概览卡) + A2孕哺不评热量 + F#5结构日历两排 + UI清理批(#204/#190/#192)。
# 2026-08-04 用户级 Codex 配置与插件调研交接

- 用户级共享配置/ai-share 已核对为 1.31.3；Codex 已补 `insight-add` Skill 联接并刷新 24 个 Agent TOML。
- 调研文档：`~/.ai-context/docs/Codex插件与Claude工具映射调研.md`。
- 项目开发接续点不变：先按 `SESSION_交接.md` 执行真机验证 D1-D5/F1-F4。

# 2026-08-04 项目地图与 AI 核心能力审计交接

- 建立项目地图系统：`projectReview/00` 为全景首读入口，`功能路径索引` 为代码级导航；项目规则、Claude 与 Codex 入口均要求全局关联任务先读并同变更维护。
- 新增专属册：`21_AI与网络请求策略（专属）`、`22_预设与参考资料治理（专属）`；同步刷新 00–08、20，核对 schema 38 表/迁移至 32/seed 13 文件。
- 核心决策 D-13：AI 显式结构化语义优先，本地仅规范化、查重、校验、入库；菜名规则仅作缺失或低置信补全。
- 下一 session 先处理 AI 快捷记餐 P0 数据正确性与隐私日志，不应在当前文档整理 session 直接开始大改。

---
## 2026-08-05/06 会话交接：B3 会话层第三轮复审修复完成

- B1/B2 八审通过，B3 三轮复审修复完成（AF-B3-R3-01~05），待架构模型定向复审。
- B3 核心交付：MealStreamDraftMapper+StreamingMealSession+AiMealInputViewModel 会话链，约 500 测试 0 失败。
- 下个 session 先查看 B3 蓝图 §11 是否有新复审反馈，确认基线后修复→定向复审→通过→启动 B4。
