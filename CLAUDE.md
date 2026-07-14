# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 公共 AI 上下文目录（必读）

本项目为 Claude Code / Codex 双模式开发，**公共规范、经验、功能文档、上下文记忆统一存放在 `.ai-context/`**（说明见 `.ai-context/README.md`）：

- **通用强制规则**：`.ai-context/rules/通用规则.md` —— 任务编排门禁、任务前快照、工程一致性、单元测试、AI 注释、构建环境等，**每次任务开始前遵守**
- **经验手册**：`.ai-context/docs/experience/`（索引 `INDEX.md`，工程统一规范见 `09_工程统一规范.md`）
- **功能/方案文档**：`.ai-context/docs/feature/`
- **上下文记忆**：`.ai-context/docs/context_memory/`（双端共写共读，任务快照与阶段结论都写这里）

`.claude/` 只保留 Claude Code 专属内容（settings.json、agents/、hook 薄包装）；公共内容一律放 `.ai-context/`，不再双份维护。

## 语言设置

**必须使用中文**与用户对话。

## 体验设计准则（UX，凡体验/交互/UI 相关必守）

做本项目任何**体验/交互/UI**工作时，以 **UX 产品经理**身份要求自己，遵循苹果的设计理念，追求**简洁、高效、易操作**——让用户**少操作**就能达到目的：

- **简洁**：界面克制去噪、信息层级清晰；默认只露高频项，高级/低频项收纳（折叠/更多）。
- **高效**：减少必填、给合理默认值、一键复用、批量操作、撤销优于确认弹窗、常用置顶、保存有反馈。提方案先自问"能不能更少一步 / 更少一个必填 / 更合理的默认？"。
- **审美**：苹果式克制留白、一致的圆角/间距/字重、克制的强调色，动效自然不喧宾夺主。

家庭日常高频操作尤其要顺手（贴合"家庭记菜"定位）。发现啰嗦流程要主动提精简建议，而非被动实现。体验提升项沉淀在 `.ai-context/docs/feature/待办总览.md`。

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
- 健康数据（食材/营养/详情）为 AI 参考整理、非权威核对：涉及数据来源必须如实标注 + 免责，禁编造权威出处。
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
- 注释/KDoc 内禁写 `/*`（如 `img/*`）：Kotlin 块注释可嵌套，`/*` 未配 `*/` → 编译 `Unclosed comment`（报在 EOF），改文字表述。
- DB 存文件引用（图片等）一律存**相对文件名**、读时按当前目录解析；存绝对路径遇目录迁移/跨设备即失效。
- 存储合规：数据放 **app 专属目录**（`getExternalFilesDir`，零权限、免 `MANAGE_EXTERNAL_STORAGE`）；用户要拿数据走 **SAF**（`CreateDocument`/`OpenDocument`）。完整备份须打包 **db+图片**（zip），只备 `.db` 丢照片。
- shared 若用 `android.util.Log`（如经 expect/actual 的 `CookbookLog`）：`shared/build.gradle.kts` 必加 `android{testOptions{unitTests.isReturnDefaultValues=true}}`，否则一旦被测路径触达日志，`:shared` 单测因 Log 桩抛 `RuntimeException` 全红。
- Compose：`LaunchedEffect` 依赖内容变化时 key 用**整个 data 对象**（非仅 `id`，否则同 id 内容变不重跑→数据陈旧）；冷流（`observeFlag` 等）别在 Composable body 裸调 `collectAsState`，用 `remember` 包或进 VM `stateIn`（否则每次重组新建订阅）。
- LazyColumn 手工算 `animateScrollToItem` 偏移（字母索引等）：新增/条件插入任一 item 必须同步偏移量**并**纳入 `remember` key，否则跳转偏位。
- 派生逻辑别依赖内部 `DateTime.today()`（否则固定日期单测测不了）：把 today 提为参数，生产传 `DateTime.today()`。
- `DishMini` 有一堆默认空字段（`mainIngredientNames` 等）：用某字段前先 grep 确认真被赋值——`mainIngredientNames` 曾在 `buildDishMinis`/`buildDishesByMealRecord` 都没填、恒空，导致依赖它的分类图标/主食判定/主料副文本静默失效。
- Material3 版本为 **1.1.2**：**无 `SelectableDates`、无 `SegmentedButton`**（均 1.2.0+）——DatePicker 禁选日期改在**确认回调**校验+提示；分段控件自绘胶囊（有 `ModalBottomSheet`/`SwipeToDismiss`/`LargeTopAppBar`/`FilterChip` 实验但可用）。
- 删"死导入"易翻车：`perl` 用 `$` 锚行尾在 **CRLF** 文件匹配不到；肉眼判"没用"可能删掉**仍被引用**的 import → 编译红。删 import 用 `Edit` 逐个删、删前 `Grep` 确认无引用；死导入只是 warning，拿不准就留。
- 可复用组件的"能力显隐"由**回调是否传入**决定（如 `IngredientDetailSheet` 编辑区），别在组件内用 `!selectionMode` 等 mode 布尔硬编码，否则换场景要复用时被挡。
- 多入口共享一个 ViewModel（新增/编辑/复制）：每个入口用**独立一次性守卫**（如 `copyConfigured`），别共用一个 `configured`，否则被 `init` 默认 configure 抢跑 `if(configured)return` 吞掉；"改日期=移动删旧"只在真编辑既有日期(loadedFromDate!=null)时触发。
- VM 里"跳转视图/改选中项"后，凡该视图的**查询依赖某派生态**（如按分类查食材是从左侧展开树 `tree` 找节点），必须**同步重建那个派生态**，否则查不到静默空列表——`IngredientPicker` 曾因保存后跳到新分类却没重建 `tree`，`reloadCurrentList` 在陈旧树里找不到新分类节点返回空（自建分类挂食材"看不到"）。稳妥做法：让查询直接依赖**源数据**（`allCategories`）而非展开态，减少这类隐性耦合。
- **沉浸式 edge-to-edge**（`setDecorFitsSystemWindows(false)`+导航栏透明）：全屏页(无底部栏)内容会伸到系统导航栏下被遮挡——统一在 `MainScaffold` 的 `NavHost` 对无底栏路由加 `navigationBarsPadding()`；**嵌套 Scaffold 的 bottomBar 别再自加 `navigationBarsPadding()`**且要 `contentWindowInsets = WindowInsets(0,0,0,0)`，否则底部按钮**双重下边距**。
- Compose `LazyList` **`scrollToItem(idx)` 后同帧读 `layoutInfo` 做居中/偏移会拿到旧布局**（visibleItemsInfo 尚未重排）→ 居中静默失效只剩靠左。用 `snapshotFlow{listState.layoutInfo}.firstOrNull{ 目标 index 已在 visibleItemsInfo }` 等布局出来再 `scrollBy`。
- VM 里用"**重建整个 UiState**"(如 `mapResult(...)` 返回 new state)替换 `state` 会**丢掉未列出的字段**（粘性选择：推荐风格/餐次/去重周期）→ 结果用 `.copy(那些字段=旧值)` 保留；`onFailure` 走 `state.copy` 天然保留。
- 字典/库**软删只删 `source='user'`**（预设不可删）；从库删某项时若它**已被当前表单选中**，要**同步移除已选**，否则保存 `INSERT OR IGNORE` 会把删掉的自建项"复活"。
- SQLDelight 加**纯新查询/软删**（`selectAll*`/`softDelete* WHERE source='user'`）不改表结构 → **无需 `.sqm` 迁移**；只有改 CREATE TABLE 列才要迁移。营养/字典类**批量基础数据**用**独立 seed 文件**（如 `ingredient_nutrition.json`，同 `ingredient_details.json` 按名 upsert），别内联进 `ingredients.json`——单表单文件后续只填数据。
- 小型结构化配置（身体数据等单行低频、非查询维度）**存偏好 JSON 免迁移**：`@Serializable` data class 序列化成一个 `user_preferences` key（`PreferenceRepository.observe/setXxx` + `Json{ignoreUnknownKeys}`），加改字段零迁移；别为它新建表/加列。
- 表单**多字段"改一个 copy 写回全部"有竞态**：字段 onChange 用异步 flow 回灌的值 `flow.copy(该字段=新值)` 写回时，快速连改两字段会用**旧值**覆盖丢数据。所有字段写回**以本地 UI 态为单一真相源**（`build()` 带全部本地值 + 仅覆盖显式变更项），不读迟滞 flow。
- 数字输入框**别只 `filter{isDigit()||'.'}`**：会放行 `1.7.5`/`30.` → `toDouble/IntOrNull` 恒 null → 依赖值(目标等)静默消失。小数字段限最多一个小数点、整数字段(年龄)禁小数点。
- StateFlow 结合**多源 + 逐项异步计算**用 `combine(...)+mapLatest{}`（mapLatest 可 suspend、新值取消旧算）；里面批量 `dishNutrition(allIds)` 一次查再按 id map，别 `.map{}`(不能 suspend)也别逐项查。

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
