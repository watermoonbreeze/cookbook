# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 公共 AI 上下文目录（必读）

本项目为 Claude Code / Codex 双模式开发，**公共规范、经验、功能文档、上下文记忆统一存放在 `.ai-context/`**（说明见 `.ai-context/README.md`）：

- **通用强制规则**：`.ai-context/rules/通用规则.md` —— 任务编排门禁、任务前快照、工程一致性、单元测试、AI 注释、构建环境等，**每次任务开始前遵守**
- **经验手册**：`.ai-context/docs/experience/`（索引 `INDEX.md`，工程统一规范见 `09_工程统一规范.md`）
- **功能/方案文档**：`.ai-context/docs/feature/`
- **上下文记忆**：`.ai-context/docs/context_memory/`（双端共写共读，任务快照与阶段结论都写这里）

`.claude/` 只保留 Claude Code 专属内容（settings.json、agents/、hook 薄包装）；公共内容一律放 `.ai-context/`，不再双份维护。

## 会话交接协议（跨 session 续接，必守）

用户用两句话在 `/clear` 前后无缝续接工作，**唯一交接入口文件** = `.ai-context/docs/context_memory/SESSION_交接.md`：

- **用户说「切换session / 保存session / 交接 / 会话交接」** → 立即执行：①把当前对话的进行中任务、上下文、关键决策、经验**落地成文档**（分别写入 `context_memory/`、`experience/`、`feature/` 相应位置）；②**覆盖式更新** `SESSION_交接.md`（含：先读什么、工作规则、当前状态、⏭下一步）；③`git` 提交（在 git 仓库时，`[unattended]`/`docs:` 前缀，不自动 push）；④告知用户"已交接完成，clear 后说『查看session继续』即可接续"。
- **用户说「查看session继续 / 读session / 接续session / 会话继续」** → **先读 `SESSION_交接.md`**，按其"先读清单"补齐上下文，再按其"⏭下一步"接着干（不重复已完成项）。
- 该文件是"最新一次交接"的**唯一真相源**（每次交接覆盖），更细的历史流水在同目录的日期快照里。

## 语言设置

**必须使用中文**与用户对话。

## 体验设计准则（UX，凡体验/交互/UI 相关必守）

做本项目任何**体验/交互/UI**工作时，以 **UX 产品经理**身份要求自己，遵循苹果的设计理念，追求**简洁、高效、易操作**——让用户**少操作**就能达到目的：

- **简洁**：界面克制去噪、信息层级清晰；默认只露高频项，高级/低频项收纳（折叠/更多）。
- **高效**：减少必填、给合理默认值、一键复用、批量操作、撤销优于确认弹窗、常用置顶、保存有反馈。提方案先自问"能不能更少一步 / 更少一个必填 / 更合理的默认？"。
- **审美**：苹果式克制留白、一致的圆角/间距/字重、克制的强调色，动效自然不喧宾夺主。

家庭日常高频操作尤其要顺手（贴合"家庭记菜"定位）。发现啰嗦流程要主动提精简建议，而非被动实现。体验提升项沉淀在 `.ai-context/docs/feature/待办总览.md`。

**做交互/UI 前先读 `.ai-context/docs/feature/苹果风格UI设计方案.md`**（色彩/字体/形状/组件全套 + **§九 交互模式库**：勾选圈多选、搜索即开详情+分类路径、贴角小圆角标、系统栏色跟随主题、空态给下一步、悬浮可拖动按钮、低频区折叠、未保存返回守卫、撤销优于确认、详情底部主CTA、智能默认值等**已确立可复用规范**），新功能优先复用其中模式保持一致。

**【强制门禁】凡界面/交互类操作（新页面、新弹框/选择器、布局或交互流程改动、配色/视觉等），编码前必须先由 Apple UX 体验设计师参与交互设计**：先 spawn 一个 Apple-UX 设计 agent（让其读本设计方案 + 现状代码）产出可落地的交互/视觉规范（范式选型、精确布局 dp/sp、组件复用、图标处理等），据其方案再编码。**不得跳过设计直接写 UI**。仅"复用已确立 §九 模式的同类小改"或"纯文案/数据、无新交互"可免设计 agent，但仍须符合 §九 与本准则。设计产出沉淀回 `苹果风格UI设计方案.md`。

**【强制门禁】代码质量由 Google 高级工程师角色把关、符合最佳实践**：一批编码完成、构建+单测通过后，spawn 一个 **Google 资深工程师代码审查 agent**（读改动 diff + 相关代码）从**正确性/并发/性能(N+1、重组、主线程)/可读性/命名/复用/错误处理/边界/内存**等维度审查，按最佳实践给出问题分级（阻断/建议）；**阻断项必须修复并复验**后再交付/提交。小改（纯文案/一两行）可免。审查要点沉淀进 `experience/`。（本项目已有 `DEV_review_architect` 角色规范可参考。）

## 透明准则（App 所有行为都要让用户知道·用户 2026-07-20 确立·最高优先级）

**核心：用户对 App 的所有行为有「知情权 + 掌控权」——绝不静默改数据/隐私而不可查。** 但"透明 ≠ 弹框轰炸"（过度告知=弹框疲劳，也违反苹果克制准则）。按**影响×可逆性分级透明（Tiered Transparency）**：

| Tier | 行为 | 告知方式 | 控制 |
|---|---|---|---|
| **T0 可查即可** | 无副作用(缓存/纯读) | 不主动打扰，但"关于/更新记录"可查 | — |
| **T1 事后留痕** | 低风险·可逆·已做完(自动纠正一条数据) | Snackbar/横幅"已做X" | 撤销/查看 |
| **T2 事前告知+可选** | 改数据/联网(reseed、备份、AI补值) | **弹框+改动清单** | 同意/延后/关 |
| **T3 事前硬同意** | 隐私/不可逆(权限、上传、清库) | 强同意+二次确认 | 明确授权 |

配套：①建**统一"更新记录/App 做了什么"中心**（我的·关于下·单一真相源·所有自动行为留痕可查，天然承载 F#8 changelog）；②每条告知统一含 **做了什么/为什么/影响什么/怎么控制** 四要素、一套组件全 App 一致；③**能自动做好又无害的就"做+告知"、别"弹框问"**（减少决策疲劳，呼应"少操作"）；④默认值对用户最有利（不预勾选采集/上传）；⑤能自动化的给设置开关+状态可见。守苹果"诚实、克制、不操纵、用户掌控"，禁暗黑模式（默认勾选/诱导/制造焦虑/假紧迫）。落地第一步：由 `apple_software_behavior` 产出**全 App 自动行为清单+分级**，逐条定 Tier + 告知方案。

**【强制门禁】App 行为规范由「Apple UX 设计师」+「Apple 软件行为师」总负责**：凡涉及"App 会**自动做什么**"的行为（后台任务、自动改数据、迁移、纠正、联网、权限、通知、同步、缓存等），编码前先由 **`apple_software_behavior`（软件行为师·行为契约/告知必要性/时机/副作用/幂等/诚实不操纵）** 审查，配合 **`apple_ux_designer`（告知的界面呈现）**，产出可落地的行为规范；**当前静默的行为须补明确告知**。重要行为二者交叉评审。（典型：更新基础数据 reseed 必须弹框+变更说明，见 `待办总览` F#8。）

## 文案准则（凡界面文字/文案/描述相关必守）

做本项目任何**界面文字**（标签/按钮/标题/说明/空态/错误提示/引导/通知/运营话术）时，以 **Apple 标准文案/运营策划师**身份要求自己（角色规范见 `.claude/agents/copywriter.md`），中文母语级、自然不翻译腔：

- **清晰 > 精简 > 说人话**：一眼看懂、能删就删、像体贴的人在说话（用"你"）；操作用**动词开头**（记一餐/换一批/存为菜品），短标签不加句号。
- **术语一致**：同物同词（菜品/食材/餐次/记一餐/库存/收藏/菜系/计划vs食历/忌口·限量·推荐），别一处一个说法。
- **鼓励非责备**：空态给下一步；错误说"发生了什么+怎么办"，不责怪用户、不吓唬、不夸大、不暗黑话术。
- **健康免责红线**：营养/慢病/评级/忌口文案守"**仅供参考·非医嘱**"，禁"降压/治疗/达标"等医疗断言，无源口径标"惯例·非国标"。

**新增较多文案或需通读审校时**，spawn `copywriter` 角色 agent 产出分级审校报告（🔴必改/🟡建议/⚪可选），据其结论按批落地。纯一两处小改可直接按本准则写。文案沉淀与术语表见该角色文件。

## 架构与代码质量准则（凡编码必守·用户 2026-07-21 确立·经 Google×Apple 架构会诊精修）

> **两条正交准则，别混为一谈**（会诊结论：原"数据驱动界面"一句混了两个层级）：**A=UI 层怎么流动状态（UDF），B=跨平台共享到哪一层（共享逻辑·原生 UI）**。UDF 是 A 的原则，与 B（是否跨平台/共享什么）**无关**。

**准则 A · UI 层单向数据流（UDF·所有平台通用）**
- **UI 是 State 的纯函数**：ViewModel 持**单一真相源 UiState**，UI **只渲染 State + 上抛事件**（State 下行、Event 上行 `vm.xxx()`）；UI 不持可变业务状态、不写业务逻辑；派生态由 State 计算（不另存易漂移副本）；副作用（DB/网络/IO）在 VM/UseCase。
- **落地 = MVVM + UDF（单向数据流·不可变 UiState 作单一真相源）**，**非双向绑定 MVVM / MVP**：撕裂根源是"双向绑定 / 命令式逐步改视图 = 多个可变真相源"（如"草稿 vs DB 回灌打架"），**不是 MVVM 这个名字本身**——UDF 是"数据怎么流"的纪律、MVVM 是"代码怎么分层"的结构，**在 MVVM 里应用 UDF**：VM 暴露不可变 UiState、UI 只渲染、事件上抛、VM 产出**新** State（单向、单一真相源）。**Android** VM 暴露 `StateFlow<UiState>`；**状态载体用各端原生表达**——iOS 用 `@Observable`/`@State`（**别把 `StateFlow` 强加给 Swift**），鸿蒙 ArkUI 用其响应式。声明式 UI（Compose/SwiftUI/ArkUI/Compose Desktop）天然要求 UDF，无更优替代。**全量上 MVI/Redux=过度设计**（本项目无此规模痛点；仅极复杂状态机页面才值得 reducer）。
- **配套纪律（源自踩坑·UDF 的具体约束）**：以**本地 UI 态**为写回单一真相源 + 异步回灌加 `hydrated` 守卫；`_state.update{it.copy()}` 而非"捕获 value→挂起→写回"；**重建 UiState 用 `.copy()` 保留粘性字段**（更优：重建函数接收 `prev` 源头兜底·见踩坑红线）；`stateIn` 冻结坑；能力显隐由**回调/参数**传入决定而非 mode 布尔硬编码。

**准则 B · 跨平台共享边界（战略·共享逻辑不共享像素）**
- **共享边界按"逻辑 vs 像素"划**：**共享（`commonMain`）= Domain（规则/算法·纯 Kotlin 无平台依赖 + 完整单测·如 `HealthRuleEngine`/`MealCompositionScorer`）+ Data（Repository/SQLDelight·对外暴露 `Flow`/`suspend` 平台中立接口）**——现状已达标，持续下沉。判据一句话：**"能脱离任何 UI 框架、无平台 UI/导航/生命周期概念"的才进 `shared`，沾了就留该端。**
- **薄表现层 + 原生 UI**：ViewModel 只做**状态编排 + 生命周期**，业务一律调 shared UseCase（**"薄 VM"即未来跨平台上移的最优低成本预留**）；**UI 各平台原生**（Android=Compose，iOS=SwiftUI），**不追求 UI 像素级共享**，保平台体验契合（本 App 核心竞争力=顺手）。
- **平台边界与时机（避免过度设计·当前只交付 Android）**：iOS 暂不做，真立项时升 Kotlin 2.0+、VM 可下移 `commonMain`（首选**官方 KMP ViewModel**）连 Swift 互操作一起验证——但**默认原生 SwiftUI，不上 CMP-iOS**（牺牲手势/无障碍/系统集成·体验敏感页默认否决）、不上 TCA；桌面（mac/Win）弱需求，要上用 **Compose Multiplatform Desktop**；**鸿蒙（ArkTS/ArkUI）非 KMP 目标·Kotlin 不能直接共享→ArkUI 原生 + 逻辑重写/桥接·现在不做任何架构预留**（预留即负债）。
- **🔴 反过度设计红线**：**不为不存在的平台消费者**提前上移 VM / 引入 Decompose·MVIKotlin·moko 等框架（那是 iOS/桌面真立项时的决策）。唯一"现在就该做且服务所有未来平台"的动作=**保持 VM 薄 + 持续把业务逻辑下沉 `commonMain`+单测**（详见 `feature/跨平台架构策略.md`）。

**可维护 + 最佳实践（通用）**：单一真相源、抽共享防调参/逻辑漂移（如 `MealCompositionScorer`）、命名一致、复用优先于复制、消除 N+1、错误处理与边界完备、纯逻辑必带单测。**Screen/组件不得注入 `Repository`**（数据访问收进 VM·见踩坑红线）。

**【强制归口】Android 端编码由「Google 高级技术工程师」把关、iOS 端由「Apple 高级技术工程师」把关：**
- **Android**：一批编码完成后由 **`google_quality_engineer`（代码质量终审）** + 涉架构/模块边界/技术债时加 **`google_architecture_engineer`（架构规范）** 审查，按最佳实践给阻断/建议分级，**阻断必修复复验**（沿用「代码质量门禁」流程）。
- **iOS**：由 **`apple_architect`（架构）** + **`apple_quality_engineer`（质量打磨）** 把关，守 Apple 平台惯用法与体验打磨。
- 重大架构决策可 Google（规模/规范）× Apple（简洁/平台契合）双视角交叉评审。**新架构/大重构前先出设计、审查后再编码**，不跳门禁。

## 临时目录

除用户明确要求外，处理问题需要创建的临时文件放在 `temp/claude/`。

## 项目概述

Cookbook 是一款面向慢性病（三高、痛风等）患者的饮食规划 APP，核心价值是帮助用户解决"每天吃什么"的决策疲劳问题。基于 Kotlin Multiplatform (KMP) 跨平台架构，Android 端使用 Jetpack Compose，iOS 端使用 SwiftUI。

MVP 三大核心功能（快速记录每餐、查看历史菜单、复用菜单）已完成，当前处于**功能扩展与打磨阶段**（食材体系、厨房小助手、搜索等已落地）。详细规划见 `docs/` 目录和 `.ai-context/docs/feature/`。

## 踩坑红线（必避）

> 每条一行、命令式、可识别；详情见 `.ai-context/docs/experience/06_问题与踩坑.md`。

- SQLDelight：改 `.sq` 表结构必须同步加 `N.sqm` 迁移；DB 真实版本由 `.sqm` 文件数推导（build.gradle `version` 无效），判断版本看生成的 `Schema.version`。
- SQLDelight 新迁移文件名 = 目录里**最大 `N.sqm` + 1**（现有到 `13.sqm` 就建 `14.sqm`，别按版本号命名）；命名错会漏迁移/版本乱。
- 给表**加列**要一次改全：CREATE TABLE 也加该列（全新装走 Schema.create 不跑迁移）+ 新 `N.sqm` `ALTER ADD COLUMN`（列序与 CREATE 一致，都追加末尾）+ insertX/updateX 及**所有调用点**传参 + 显式列 SELECT 手动加 + 所有构造该模型处读新列 + 领域模型加字段（漏 loadFullDish 构造会 "Cannot find parameter"）；seed 补齐式对**已存在行**要单独幂等 UPDATE（放"已存在则跳过"判断之前）。
- SQLDelight 方言是 `sqlite_3_18`，**无 UPSERT**（`ON CONFLICT DO UPDATE` 编译失败）：累加/幂等改 `INSERT OR IGNORE` + `UPDATE ... x=x+:d` 两步放同一 `db.transaction{}`。
- SQLDelight `sqlite_3_18` 的 **WHERE 不支持 `REPLACE`/`TRIM` 等字符串函数**（`<expr> expected`）：名称去空格归一比对别写进 `.sq`，查 id+原名后在 Kotlin 侧归一比对（或加 `name_key` 冗余列）。
- DB 恢复/覆盖库必须**原子+回滚**：覆盖 `currentDb` 前先存回滚区，失败即还原（否则中途失败毁库）；关的是单例 driver，恢复后需重启应用。
- SQLDelight **单列 SELECT** 的 `executeAsList()` 返回 `List<列类型>`（如 `List<String>`），不是行对象——别 `.map{it.name}`（编译失败）。
- JUnit4 `@Test` 须返回 void：`fun x()=runBlocking{…}` 末尾禁用返回非 Unit 的断言（如 `assertNotNull`），否则 `InvalidTestClassError`，末尾补 `Unit`。
- SQLDelight 迁移：单测走 `Schema.create` 不跑迁移链、迁移错误测不出——改动涉及迁移必推演旧库各历史版本升级；`ALTER ADD COLUMN` 对已有列会崩，用幂等/无副作用写法（否则真机「初始化数据失败」）。
- 大批量改 seed（食材/分类/详情/菜品）用脚本 + 引用完整性校验 + `:shared:testDebugUnitTest`；未知食材/分类 code、以及菜品的**食材名/烹饪方式/单位（均按名解析）**被 seeder 静默跳过（不崩但少关联）——扩菜品只用已存在名字并加/跑引用完整性单测；改 general 大类名会打断测试按名断言。
- `seedDishes` 对**已存在同名预设菜"补齐式"重挂缺失配料**（只加不删、不改用量），别"已存在即 return 跳过"：菜先于其食材入库时配料没关联上→之后补了食材/营养重 seed 仍跳过→**关联永久残缺→热量算 0**（"凉皮 0 千卡"根因）。菜品热量算 0 先查 dish_ingredient 是否真关联上了。
- seed **处理逻辑**变更（非 JSON 内容）要让已装老库跑一次：把 `SEED_LOGIC_VERSION` 盐混入内容指纹（`fingerprintOf(SEED_LOGIC_VERSION, …)`）+1，否则指纹不变、老库跳过 seed 拿不到修复；用户侧"更新基础数据"(force) 也可即时修复。
- seedDishes 补齐**只补"缺失关联"不够**：早期 seed 无 quantity 时**已关联但 `quantity/unit_id=NULL`** 的行按克算营养恒 0（"排骨海带汤 0 千卡"），`if(id in linked) return@ing` 会跳过它永不修——补齐分支须再跑 `fillDishIngredientQuantityIfNull`（`UPDATE...WHERE quantity IS NULL`，只回填空值不覆盖用户）+ `SEED_LOGIC_VERSION`+1。软删菜(status=0)reseed 会当新菜重插带新数据、未删的走补齐分支——解释"删掉再更新就好、没删的一直 0"。
- 健康数据（食材/营养/详情）为 AI 参考整理、非权威核对：涉及数据来源必须如实标注 + 免责，禁编造权威出处。
- **凡联网数据校验/核准（营养/GI/嘌呤/忌口/care 规则等），收尾必须把权威来源加进「我的·数据来源」页**（`androidApp/.../ui/reference/DataSourceReference.kt` 的 `categories.levels/source` + 底部 `sources` 清单），与 seed 数据一起交付（用户多次强调·透明可溯源）。来源具体到 标准/指南名+机构+年份。**加食材/菜品不能只填营养——健康判定=营养数值+食材属性双层，缺一不可**（`ingredient_care_rules.json` + `IngredientCrowdCare`·数值判绿但属性有风险→care 压制红/黄·取更严）：数值层(成分表 mg)测不出代谢机制/加工/天然vs添加，纯数值必漏——低嘌呤啤酒(酒精升尿酸)、可乐嘌呤=0但果糖促尿酸→痛风均应限；**反式脂肪/添加糖/草酸数值层无字段·只能靠属性 care**。加食材必评估属性风险配 care(带指南 ref)。**但属性判定要核实指南口径、别过度**：新鲜水果虽含果糖，食养指南2024明确「与痛风无显著相关」(维C/钾/纤维改变影响)→不做痛风忌口，限的是含糖饮料/果汁/果葡糖浆/果脯蜜饯(加工浓缩果糖)。框架/审视/方案见 `feature/健康判定_数值加属性双层.md`。
- `IngredientCrowdCare` 人工 care 压制是**单向只升不降**（care AVOID→慎选红 / LIMIT→留意黄·从任意数据级取"更严者"·`fit!=NO_DATA`）：内脏录**偏低**嘌呤判黄→加 care avoid 才压成红（此即修过的「录低值反判绿」bug·原压制只作用 FIT 现改任意级）；反之**数值已判红**的项(植物高嘌呤 purine≥150)加 care limit **无效**(压制软化不了数值红·红线禁 care 洗白)。批量补 care 走**加法**：名精确匹配 `ingredients.json`(python 校验 0 跳过·缩写名如奶酪要对上实名)+原有条目不动(语义比对前缀)+改 JSON 内容即触发指纹自重跑老库(免 `SEED_LOGIC_VERSION`)+引用完整性/回归守卫单测+来源入数据来源页。
- 冷启动 intent（通知/全屏提醒/deeplink）拉起 Activity 后**禁在 `LaunchedEffect` 里对 `rememberNavController` 同步 `navigate`**：NavHost 可能还没挂图（慢机型）→ `Navigation graph has not been set` 崩溃；先 `nav.currentBackStackEntryFlow.first()` 等图就绪再跳。真机崩溃用 `adb -s <序列> logcat -b crash -d` 捞缓冲（勿先清）。
- 营养折算防"天价"：`dish_ingredient.unit_id` 为空时（`gramUnit()` 在 `availableUnits` 未加载返 null）`resolveGrams` 兜底 `quantity×piece_gram/60` 会把"100 克"当"100 个"放大数十倍（"鸡腿饭 12000 千卡"）；已在计算层加 `quantity>PIECE_QUANTITY_MAX(20)` 按克直取兜住。派生汇总（今日卡等）依赖 A 表 Flow 但源数据在 B 表（`dish_ingredient`）→改 B 不触发 A 的 Flow，须显式 `observeDishContentChanges()` 并入 combine。数据 bug 先 `python sqlite3` 拉真机库证实再改（列名 `energy_kcal` 非 kcal，先 `PRAGMA table_info`）。
- 移动预设食材的 browse 分类（改 `categories` 顶层，如 other→beverage）必须 reconcile 删旧：seeder `linkIngredientCategory`（`INSERT OR REPLACE`）**只加不删**→旧关联残留致"两分类都显"；`seedFoundationIngredients` 删 general 维度里 JSON 已无的旧关联（`unlinkIngredientCategory`·只动 general·营养/调养标签不碰）+ `SEED_LOGIC_VERSION`+1。
- 营养阈值/分级用**国标口径**：钠(膳食指南 5g/2000mg、高血压 2400mg)、GB 28050-2011 NRV 与低/高含量声称、GI 低≤55/中/高≥70(**FAO/WHO 口径，非 WS/T 652-2019——该标准只规定测定方法**)有据可依；**嘌呤"低/中/高"三级(25/150 mg/100g)无国标**(WS/T 560-2017 只给"应避免/限制/可选择"定性食物清单、附录嘌呤单位 mg/kg 不设临界值)——用则必标"非国标·惯例口径"，别当权威阈值。参考页/评级见 `feature/膳食参考依据`、`营养级别评级方案.md`。
- 每个新文件用 Write 写（bash heredoc 遇引号/emoji 易挂）；git 提交多行信息用 `-F 文件`（Git Bash 无 PowerShell here-string）。
- 食材 `name` 全新库有 **UNIQUE 约束**（`CREATE TABLE ingredient ... name TEXT NOT NULL UNIQUE`），老库经迁移升级可能没有、仍存同名多 id。`createUserIngredient` 必须**按去空格名先查复用已有 id**（全新库防 UNIQUE 崩、老库防同名多 id）；库存推荐按名扩展(`selectIngredientIdsByNames`)兼容老库同名多 id。
- 库存推荐"某菜没推出来"排查：先看 `PantryRec` 日志——②预筛候选(用到在手食材)有、④规则评估后没了 = 被 `HealthRuleEngine` **忌口(avoid)过滤**（启用了健康档案，如高血脂忌五花肉）。忌口菜现为**保留+`AVOID_PENALTY`排最后+`avoidNames`标红**，不再隐藏（家庭 app 列出告知而非替用户隐藏）。
- 健康档案忌口/限量只作用于**非调料**食材(`HealthRuleEngine` 用 `nonSeasoning`、`gatherForPlan` 排除 `seasoningIds`)：否则"盐对高血压忌口"让每道菜都忌口。调料的忌口/限量转"少盐/少糖"做法提示(`cookingCautions`)，不剔除菜。
- shared 高频诊断日志(如库存推荐逐候选)用 `CookbookDiag.log("Tag"){ "msg" }`(lambda 延迟构造，`enabled` 默认关、androidApp 按 `FLAG_DEBUGGABLE` 仅 debug 开)，禁裸 `CookbookLog.d` 逐条打——release 不 minify，每次 gather 会跑上百次 Log + 构造字符串。
- 改 data class 字段顺序/插字段后，全仓搜**位置参数构造**(尤其测试 helper)改命名参数，否则参数错位静默失效。
- 倒计时禁用 `delay` 每秒递减（息屏被挂起会停走）：记 `elapsedRealtime` 结束时刻按墙钟算剩余；后台响铃用 `AlarmManager.setExactAndAllowWhileIdle` + 注册 Receiver。
- 前台服务通知不立刻显示：加 `setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)`（默认最多延迟10秒）；A14 FGS 须声明 `foregroundServiceType`。
- Android 14 全屏提醒/亮屏需 `USE_FULL_SCREEN_INTENT`（非闹钟类默认关，需引导用户到系统设置开）。
- 加联网功能先声明 `INTERNET`（本地优先 App 默认没有）：缺则 HTTP 静默失败，云端调用一直回退——排查"云端不通"先 curl 直测 key（通=App端问题）。
- 改 Manifest 权限/组件后必须**重装 APK**（热更/增量装不重读），排查前先确认用户装的是新包。
- 声明 `CAMERA` 后拍照(`TakePicture`/`ACTION_IMAGE_CAPTURE`)也需**运行时授予 CAMERA**，否则失败——加 in-app 扫码/相机(引 zxing 会并入 CAMERA)时须在拍照前也申请该权限。
- 拍照存图必须**应用 EXIF 方向**：部分设备(小米/华为)方向只写 EXIF、像素不转，`BitmapFactory` 解码不读 EXIF → 存的图偏 90/180/270°。用 `androidx.exifinterface`(全 API 支持 InputStream，minSdk21 首选，非 `android.media.ExifInterface`)读 `TAG_ORIENTATION` → `Matrix` 摆正后再存。
- 注释/KDoc 内禁写 `/*`（如 `img/*`）：Kotlin 块注释可嵌套，`/*` 未配 `*/` → 编译 `Unclosed comment`（报在 EOF），改文字表述。
- **Compose inline 布局 composable（`Column`/`Row`/`Box`）内容 lambda 禁「提前 return」**（`return@Column`/`return@Row`/`return@Box`）：致组 Start/End 失衡→**重组时**崩 `ArrayIndexOutOfBoundsException length=0 index=-N @ SlotTableKt.key`（栈常无自己代码帧·常伴 Scaffold/SubcomposeLayout 子合成·**"某场景才崩"极迷惑**，如"仅编辑态崩=封面空→出图重组时炸"）。变体组件（封面/普通两态）用 `if/else` 平衡分支，别"先渲一种再 return"。真机无栈时看 App 崩溃处理器落的 `files/cookbook/log` 或 `adb logcat -b crash -d`。详见全局经验 `~/.claude/memory/android_compose_slottable_early_return.md`（issuetracker 248513437）。别被"Scaffold 无界高度/嵌套 bottomBar"表象带偏（那是测量崩·另一类）。
- DB 存文件引用（图片等）一律存**相对文件名**、读时按当前目录解析；存绝对路径遇目录迁移/跨设备即失效。
- 存储合规：数据放 **app 专属目录**（`getExternalFilesDir`，零权限、免 `MANAGE_EXTERNAL_STORAGE`）；用户要拿数据走 **SAF**（`CreateDocument`/`OpenDocument`）。完整备份须打包 **db+图片**（zip），只备 `.db` 丢照片。
- shared 若用 `android.util.Log`（如经 expect/actual 的 `CookbookLog`）：`shared/build.gradle.kts` 必加 `android{testOptions{unitTests.isReturnDefaultValues=true}}`，否则一旦被测路径触达日志，`:shared` 单测因 Log 桩抛 `RuntimeException` 全红。
- Compose：`LaunchedEffect` 依赖内容变化时 key 用**整个 data 对象**（非仅 `id`，否则同 id 内容变不重跑→数据陈旧）；冷流（`observeFlag` 等）别在 Composable body 裸调 `collectAsState`，用 `remember` 包或进 VM `stateIn`（否则每次重组新建订阅）。
- 表单 `rememberSaveable` 全字段草稿（进程被杀不丢）与**异步 DB 回灌打架**：DB flow 变会再触发回灌 effect 用 DB 值覆盖刚恢复的草稿→加 `var hydrated by rememberSaveable(id){false}` 守卫，回灌 effect 首行 `if(hydrated) return@LaunchedEffect`（协程内 return 安全）、编辑态 `if(id!=null && (editorLoading||editorIngredientId!=id)) return`(等本对象 DB 载完)、末尾 `hydrated=true`；首装/恢复靠 hydrated 持久决定灌不灌。非基础类型草稿(List/Set/枚举?/富对象)用 `rememberSaveable(id, stateSaver=自定义Saver)`（序列化成 String·别用不可见控制字符做分隔·会被 Edit 吞→用 `\uXXXX` 或可打印多字符）。
- LazyColumn 手工算 `animateScrollToItem` 偏移（字母索引等）：新增/条件插入任一 item 必须同步偏移量**并**纳入 `remember` key，否则跳转偏位。
- 派生逻辑别依赖内部 `DateTime.today()`（否则固定日期单测测不了）：把 today 提为参数，生产传 `DateTime.today()`。
- `DishMini` 有一堆默认空字段（`mainIngredientNames` 等）：用某字段前先 grep 确认真被赋值——`mainIngredientNames` 曾在 `buildDishMinis`/`buildDishesByMealRecord` 都没填、恒空，导致依赖它的分类图标/主食判定/主料副文本静默失效。
- Material3 版本为 **1.1.2**：**无 `SelectableDates`、无 `SegmentedButton`、无 `HorizontalDivider`**（均 1.2.0+）——DatePicker 禁选日期改在**确认回调**校验+提示；分段控件自绘胶囊；分隔线用 `Divider`（非 `HorizontalDivider`）。（有 `ModalBottomSheet`/`SwipeToDismiss`/`LargeTopAppBar`/`FilterChip` 实验但可用）。
- **做 UI/交互先查 `.ai-context/docs/feature/交互组件复用指南.md` + §九(9.1–9.17)**：能复用的 22 个组件/统一件必复用（`AppTopBar`/`AppSearchField`/`CapsuleButton`/`SegmentedControl`/`InsetGroup`/`ActionSheet`/`MiniStepper`/`EmptyState`/`UnsavedGuard` 等），别内联复制。未保存返回守卫用 `rememberUnsavedGuard`（非包裹式返 requestBack）；就地份数/克数增减用 `MiniStepper`；多操作收 `ActionSheet`；保存反馈 Snackbar 优先(Toast 仅纯告知)、可逆删除走撤销不硬确认；大标题页(Tab落地)用 `LargeTopAppBar`、带返回二级页用 `AppTopBar`。
- **改主题色/加配色**走 `AppPalette`枚举(shared) + `theme/Palettes.kt`(每套 light/dark ColorScheme+代表色) + `CookbookTheme(themeMode,palette)` + `PreferenceRepository.observe/setPalette`，别把色值硬编码散落；默认赤陶橘(复用 Color.kt LightColors/DarkColors)。宏量三色放 `ExtendedColors`(固定不随主题)。宏量渐变条别"实块+糊接缝"(显假)，用段中心纯色+两端锚色的 `Brush.horizontalGradient` 整条平滑过渡。
- **色系墙只看膳食结构、不关联热量/慢病**(用户决策)：热量是个人概念(需身体数据)→只放今日卡；慢病提示(钠等)→个人视角(今日卡 concerns 琥珀点+免责)。`NutritionLevelEvaluator`(热量+钠,缺数据退多样性)只服务个人卡，`FoodGroup.nutritionLevel`(色系墙)维持纯结构不变。
- 调料加进菜品默认克数用 `SeasoningDefaults.defaultGramFor(name,isSeasoning)`(盐3/酱油油10…)：**只对分类判定为调料**(`seasoningIngredientIds`=调味品/油脂类)缩小，普通食材(含名带油的油菜)仍 100g，否则钠/油脂算爆。
- 删"死导入"易翻车：`perl` 用 `$` 锚行尾在 **CRLF** 文件匹配不到；肉眼判"没用"可能删掉**仍被引用**的 import → 编译红。删 import 用 `Edit` 逐个删、删前 `Grep` 确认无引用；死导入只是 warning，拿不准就留。
- 可复用组件的"能力显隐"由**回调是否传入**决定（如 `IngredientDetailSheet` 编辑区），别在组件内用 `!selectionMode` 等 mode 布尔硬编码，否则换场景要复用时被挡。
- 多入口共享一个 ViewModel（新增/编辑/复制）：每个入口用**独立一次性守卫**（如 `copyConfigured`），别共用一个 `configured`，否则被 `init` 默认 configure 抢跑 `if(configured)return` 吞掉；"改日期=移动删旧"只在真编辑既有日期(loadedFromDate!=null)时触发。
- **"还原/撤销"走 save 路径别重复抬统计**：`saveDayMeals` 按"该日旧菜集"判"新出现的菜"抬喜爱度，删整天撤销时该日刚删空→旧集空→所有恢复菜误 +1 污染排序。凡还原/撤销/纯搬运走保存路径都自问"这是新记一餐吗？"不是就传 `bumpPreference=false`(或等价基线)不抬。同类："移动日期重复抬喜爱度"。
- **可逆破坏操作=软删+撤销**(§9.12)，别用"不可撤销"硬确认弹框：`snapshotDay` 快照→删→`showUndo` 撤销 `saveDayMeals(bumpPreference=false)` 还原；**快照读失败/为空(异常吞成空)不该照删**(`getOrNull()+isNullOrEmpty` 守卫)；撤销 Snackbar 用 `Long`；统一宿主 show* **单 job 串行化**防未点撤销被挤丢。未保存守卫用 `rememberUnsavedGuard`(§9.17,非包裹式)。
- VM 里"跳转视图/改选中项"后，凡该视图的**查询依赖某派生态**（如按分类查食材是从左侧展开树 `tree` 找节点），必须**同步重建那个派生态**，否则查不到静默空列表——`IngredientPicker` 曾因保存后跳到新分类却没重建 `tree`，`reloadCurrentList` 在陈旧树里找不到新分类节点返回空（自建分类挂食材"看不到"）。稳妥做法：让查询直接依赖**源数据**（`allCategories`）而非展开态，减少这类隐性耦合。
- **沉浸式 edge-to-edge**（`setDecorFitsSystemWindows(false)`+导航栏透明）：全屏页(无底部栏)内容会伸到系统导航栏下被遮挡——统一在 `MainScaffold` 的 `NavHost` 对无底栏路由加 `navigationBarsPadding()`；**嵌套 Scaffold 的 bottomBar 别再自加 `navigationBarsPadding()`**且要 `contentWindowInsets = WindowInsets(0,0,0,0)`，否则底部按钮**双重下边距**。
- **系统栏色跟随界面**：`navigationBarColor=TRANSPARENT`+edge-to-edge **不等于**跟界面色——`styles.xml` 未覆盖 `windowBackground` 时透明区露 android 默认白底，app 深色但系统主题浅色时割裂。须在 Compose 主题内 `SideEffect{ window.statusBarColor=navigationBarColor=colorScheme.background.toArgb() }`(明暗自适应)。
- Compose `LazyList` **`scrollToItem(idx)` 后同帧读 `layoutInfo` 做居中/偏移会拿到旧布局**（visibleItemsInfo 尚未重排）→ 居中静默失效只剩靠左。用 `snapshotFlow{listState.layoutInfo}.firstOrNull{ 目标 index 已在 visibleItemsInfo }` 等布局出来再 `scrollBy`。
- VM 里用"**重建整个 UiState**"(如 `mapResult(...)` 返回 new state)替换 `state` 会**丢掉未列出的字段**（粘性选择：推荐风格/餐次/去重周期）→ 结果用 `.copy(那些字段=旧值)` 保留；`onFailure` 走 `state.copy` 天然保留。
- 字典/库**软删只删 `source='user'`**（预设不可删）；从库删某项时若它**已被当前表单选中**，要**同步移除已选**，否则保存 `INSERT OR IGNORE` 会把删掉的自建项"复活"。
- SQLDelight 加**纯新查询/软删**（`selectAll*`/`softDelete* WHERE source='user'`）不改表结构 → **无需 `.sqm` 迁移**；只有改 CREATE TABLE 列才要迁移。营养/字典类**批量基础数据**用**独立 seed 文件**（如 `ingredient_nutrition.json`，同 `ingredient_details.json` 按名 upsert），别内联进 `ingredients.json`——单表单文件后续只填数据。
- 小型结构化配置（身体数据等单行低频、非查询维度）**存偏好 JSON 免迁移**：`@Serializable` data class 序列化成一个 `user_preferences` key（`PreferenceRepository.observe/setXxx` + `Json{ignoreUnknownKeys}`），加改字段零迁移；别为它新建表/加列。
- 表单**多字段"改一个 copy 写回全部"有竞态**：字段 onChange 用异步 flow 回灌的值 `flow.copy(该字段=新值)` 写回时，快速连改两字段会用**旧值**覆盖丢数据。所有字段写回**以本地 UI 态为单一真相源**（`build()` 带全部本地值 + 仅覆盖显式变更项），不读迟滞 flow。
- 数字输入框**别只 `filter{isDigit()||'.'}`**：会放行 `1.7.5`/`30.` → `toDouble/IntOrNull` 恒 null → 依赖值(目标等)静默消失。小数字段限最多一个小数点、整数字段(年龄)禁小数点。
- StateFlow 结合**多源 + 逐项异步计算**用 `combine(...)+mapLatest{}`（mapLatest 可 suspend、新值取消旧算）；里面批量 `dishNutrition(allIds)` 一次查再按 id map，别 `.map{}`(不能 suspend)也别逐项查。
- VM 多 init 加载器**并发**（loadUnits/loadCategories… 各自 launch）：写回一律用**最新** `_state.value.copy(...)` 或 `_state.update{it.copy()}`；禁 `val cur=_state.value` 捕获后经**挂起**再 `_state.value=cur.copy()` 写回——会把挂起期间别处填的字段冲掉（曾致食材单位下拉空）。排查 grep `\.value = \w+\.copy(`。
- `stateIn(WhileSubscribed)` 的 flow **无人直接 collect 时 `.value` 冻结在初始值**（upstream 不激活）：禁用它做 toggle 方向判定（`id !in flow.value` 会恒判一个方向→"点了回不来"，如膳食统计"没吃"）；toggle 类"读当前态再取反"改**读实时 DB**（repo 加 suspend `toggleX`/查询）。
- 推荐 `rotate` 分批：**RANDOM 模式 `rotation%batches` 会随机翻到"罚分末批"（整批忌口/低分）**→轮转批次只按"可接受(非忌口)"候选数算(`indexOfFirst{avoidNames非空}`为界)。忌口排末已由 `sortedWith` 分层保证，别再靠 `avoid=50` 巨值混进 score（排序冗余、淹没其余因子）；单因子(onHandMain)封顶 `min(count,3)` 防线性碾压。
- SQLDelight 迁移**改字典项名**（如单位统一英文 克→g/毫升→ml/升→L）：用 `UPDATE ...SET name=` **保 id 不变**（`unit_id`/FK 不断、数据不丢）；name 有 UNIQUE 时**先删重复**(`WHERE name IN(...) AND source='preset'`)**再带** `AND NOT EXISTS(SELECT 1 ...WHERE name='目标')` 守卫重命名（防迁移 UNIQUE 崩=真机"初始化失败"）；同步改 seed json 的 unit 字段 + `PRESET_MEASUREMENT_UNITS` + 按名单测(`units["克"]`→`units["g"]`)。
- 重命名字典项(单位/分类等)后必 grep **全仓按旧名硬编码查的代码**(`=="克"`/`firstOrNull{it.name=="克"}` 等)改兼容新旧或按 id 查：漏改会静默失效（`gramUnit()` 找"克"恒 null→改克数丢 `unitId`/克当量算不出）。改 seed/迁移只是一半，代码硬编码名是另一半。
- 加列**升级无损**：`ALTER ADD COLUMN col ...DEFAULT ''`(老行只补默认值零改动)+CREATE TABLE 同步加列；预设值回填放 seeder **只填空的**(`WHERE col=''`,不覆盖用户)；老用户数据靠"**编辑时预填+保存即应用**"补齐（如老自建食材编辑按名预选营养大类、点保存补挂分类），非强制数据迁移。
- `FoodCategory`/`food_category` 表**无 code 列**：Group→顶层分类只能**按 name 映射**(`FoodGroup.CATEGORY_NAME`)；改 general 大类名会打断映射/按名断言。
- 中文食材 `classify` 按**尾词**(末尾 head-noun：菜/苗/肉/奶/蛋/腐/油)判定，优先于前缀关键词 + `NAME_OVERRIDE` 特例表；否则"脱脂纯牛奶"含牛→肉、"鸡毛菜"含鸡→禽。DAIRY 判在 meat/FISH 前、FRUIT 在 VEG 前。新特例加进 `NAME_OVERRIDE` 而非堆关键词。
- 权威数据核准(营养/GI/嘌呤)用**分片后台 agent 联网**(先 `ToolSearch select:WebSearch,WebFetch`)各写 `temp/*_N.json`→python **覆盖升级式**合并(auth 值覆盖、**保留 auth 未覆盖字段**不 null 老值、ref+review 取 auth)；查不到的字段**省略不编造**、口径不确定标 `pending`+ref 注明，一手权威成分表才 `verified`；合并后跑 `validateNutritionSeedForTest`+`:shared:testDebugUnitTest`。
- 真机诊断"数据有但没传到 UI"：`adb -s <序列号>`(多设备)；Compose 底栏文本不进无障碍树(`uiautomator dump` 抓不到)；华为等 shell 无 `sqlite3`，`adb exec-out run-as <pkg> cat databases/x.db>本地` 用 python 读；在**查(repo)→存(state)→读(UI)** 三处埋 `AppLogger.d`/`CookbookLog.d`，一次 logcat 定位断点，完事删日志。
- 本项目 db **不在默认 `databases/`**：落 `getExternalFilesDir(null)/cookbook/db/cookbook.db`(app 专属外部目录，零权限)，`run-as ... cat databases/` 取不到——直接 `adb pull` 该外部路径(无需 run-as)。**Git Bash 调 adb 访问 `/sdcard/...` 必须 `export MSYS_NO_PATHCONV=1`**，否则被转成 `C:/Program Files/Git/sdcard/...` 报 No such file。数据 bug 修复先 `adb pull` 拉库→python 模拟要跑的 SQL 统计影响行数+抽查目标→证明有效再改代码。
- Compose `produceState`/`LaunchedEffect` 的 **key 用真正随内容变化的输入(如 path)**，别用某分支恒定的派生值：`rememberImageBitmap` 曾 `key1=cacheKey`(preview 时 cacheKey 恒 null)→path 变不重解码→全屏查看器删当前图预览不刷新(左右滑才变)；修 `key1=path,key2=preview`。
- 给被多处调用的函数加 `suspend`(为批量查库/IO)前**先 grep 所有调用点**：有同步调用点编译红(`Suspend function should be called only from a coroutine`)，包 `viewModelScope.launch` 或把 IO 移出(mapResult 加营养查询变 suspend 打断了 setMedicinalFilter 同步重排)。
- **热量数字统一受 `CALORIE_NUMBER_ENABLED` 开关(默认关·"热量是个人概念"红线)**：推荐卡/餐食卡/今日卡关=只显宏量隐千卡(用 `rememberCalorieNumberEnabled`)；推荐显"整份约X千卡"**不按成员 share 折算**；不显达标/占比%；钠慢病提示浅灰"偏咸"**不点病名不用红**；有料但用量缺致热量恒0→算"营养待完善"别显"约0千卡"。
- 表单越层修复按**准则 A 复核实质违规**：表单 20+ 本地态/isDirty 不是违规(准则A背书本地态作写回单一真相源+hydrated守卫)，**只有 `koinInject<Repository>` 是真越层**→min-fix 收进 VM+回调下传即可，别硬抽完整表单 VM(重引 hydration/数据丢失坑=过度设计)。

## 技术栈

- **Kotlin**: 1.9.20，**AGP**: 8.2.2
- **Android UI**: Jetpack Compose 1.5.4 + Material3 1.1.2
- **数据库**: SQLDelight（跨平台 SQLite），**依赖注入**: Koin
- **构建工具**: Gradle (Kotlin DSL)，版本目录 (`gradle/libs.versions.toml`)
- **包名**: `com.sxdbsm.cookbook`（shared），`com.sxdbsm.cookbook.android`（Android App）
- **最低 Android SDK**: 21，**目标/编译 SDK**: 34，**JVM Target**: 1.8
- **Maven 仓库**: 优先使用阿里云镜像

## 架构

采用 Clean Architecture 简化版（UI / Domain / Data 三层），UI 与业务逻辑按模块分离：

- **`:shared`** — 跨平台共享模块（`commonMain`/`androidMain`/`iosMain`），存放 Domain 层（Model、UseCase）和 Data 层（Repository、SQLDelight）。通过 `expect/actual` 适配各平台，iOS 端编译为静态 framework。
- **`:androidApp`** — Android 应用模块，依赖 `:shared`，仅负责 UI 层（Compose 页面、ViewModel、Theme、Navigation）。
- **`iosApp/`** — iOS 应用工程（Xcode/SwiftUI），调用 shared framework。

## 常用命令

统一使用 CLI 构建脚本（显式 JDK 17，原理与换机说明见 `.ai-context/rules/通用规则.md` 第八节）：

```bash
# 构建 Android 应用（Windows）
scripts\build-cli.bat :androidApp:assembleDebug

# 运行 shared Android 单元测试（当前工程未注册 :shared:allTests）
scripts\build-cli.bat :shared:testDebugUnitTest

# 构建 shared 模块
scripts\build-cli.bat :shared:build

# 清理构建产物
scripts\build-cli.bat clean
```

macOS/Linux 使用 `./scripts/build-cli.sh <任务>`。直接 `./gradlew` 依赖全局 `org.gradle.java.home=jdk-17`，可用但不作为标准入口。IDE（AS Hedgehog）当前打开本项目会报模块实体错误，不作为构建路径。

## 规划文档

- `docs/菜谱功能.md` — 原始需求描述
- `docs/产品规划方案.md` — 完整产品规划（MVP → 一期 → 二期）
- `docs/MVP开发规划.md` — MVP 详细开发任务、数据模型、页面设计
- `docs/技术栈与主题风格.md` — 技术选型与 Material3 主题配色规范
- `.ai-context/docs/feature/` — 实施方案（MVP 实施、数据库设计、食材体系重构、端侧 AI、UI 控件命名清单等）
