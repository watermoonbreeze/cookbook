# 🔖 SESSION 交接入口

## 下一会话启动卡（2026-08-27 · 待办分类续作）

- 用户授权：将未完成待办按大类依序处理；**所有真机验证统一留在最后**。当前为标准任务、BLUEPRINT 模式；不提交、不推送，除非用户另行授权。
- 当前批次：`DOC-GOV-20260827`，蓝图为 `feature/DOC-GOV-20260827_项目地图待办治理_实施蓝图_LITE.md`，握手状态见 `BLUEPRINT_STATE.md` 顶部。已完成 Feature 历史回写清账、生成视图重建、C6 共同一致性校验，以及横轴 `03/04/20/21` 的代码事实复核。
- 已验收命令：`feature_sync_check.py --range 511fa61c..29af225b` = `SYNC-OK 29af225b`；`--verify-state --head 29af225b`、`--backlog`、`--struct`、`--emit-index --write` 均通过；`review_freshness.py --md` 六册全为 FRESH。
- 工作树**有未提交文档/工具改动**，均属本批 allowlist：`BLUEPRINT_STATE`、`SESSION_交接`、待办分类快照/蓝图、横轴册、9 个 Feature `20_实现.md`+`STATE.yml`、生成视图、`feature_sync_check.py`、`review_freshness.py`。接手时不得清理、覆盖或混入产品代码。
- 独立终审已通过：逐功能回写均带提交 SHA 或 SYNC-NOOP 理由，且 allowlist、状态验收、生成视图、结构与 freshness 已闭合；所有真机项继续 `PENDING_DEVICE_VERIFICATION`。
- 分类状态：A 文档治理已收口；B 已有方案功能待逐批新建蓝图；C 产品/UX/健康口径待决，不擅自实现；D 真机/OEM 验证最后统一执行。

## 下一会话启动卡（2026-08-27）

- Bug-3393 已完成独立终审并为 `ARCH_ACCEPTED / AUTOMATED_GATES_PASS`：交付链 `2d946237`→`3de38f30`，治理收口 `b1b9c05e`→`8f4831ba`；定向 Shared XML 7/7、真实 Home VM+SQLite XML 2/2，allowlist checker 通过。`DEV-3393-01` 位于 `真机待验证清单_202608271041.md` 且保持 PENDING。
- 已完成：Bug-3393 与 Bug-2119 均为 `ARCH_ACCEPTED / AUTOMATED_GATES_PASS`；交付链分别为 `2d946237..ea4b5132` 与 `c8d40556..89bc16c3`。工作树在本次交接提交后应干净；不得将未执行的真机验证写成 PASS。
- 当前已接受：P4、P5-A、Bug-5305；`OBS-NEXT-A/B` 自动化门通过。所有真机项仍统一后置，最新唯一清单为 `真机待验证清单_202608271041.md`。
- 下一会话优先级：① 按 `真机待验证清单_202608271041.md` 统一执行 `DEV-3393-01`、`DEV-2119-01` 及既有 PENDING 项；② 独立文档治理批：处理 projectReview 横轴 STALE（01/03/04/20/21）与 `07_项目现状.md`、功能路径索引的 CONFIG-ERROR；③ 用 `feature_sync_check.py --backlog` 逐项清理 F-FAMILY 等功能回写欠账。上述均需各自新建任务卡/蓝图，勿混入已验收 Bug 批次。
- 新强制约定：每个待办/缺陷或新增功能，先查统一日志覆盖；缺少可诊断的 UI 状态、操作生命周期或数据阶段事件时，先在同一蓝图中补最小安全埋点，再写业务实现。详见 `projectReview/06_约定与红线.md`「待办/缺陷的日志先行」。
- 启动顺序：`AGENTS.md` → `.ai-context/PROJECT.md` → 本文件 → `BLUEPRINT_STATE.md` → 目标 Feature 的待办/缺陷 → 日志覆盖审计 → Sol 蓝图 → Terra 编码/单测 → Sol 收口。真机验证不阻断上述代码路径。

## 全景图与待办审计（2026-08-27）

- 已运行 `feature_sync_check.py --struct`（通过）与 `--emit-index --write`：`07_项目现状.md`、`features/_INDEX.md`、`功能路径索引.md` 已刷新至 `729e92a3` 的生成视图。
- 横轴新鲜度为软信号：01/03/04/20/21 分别 `STALE(16/12/8/9/48)`；07 与功能路径索引存在页脚 `CONFIG-ERROR`，不把这些历史文档问题伪装成已解决，后续单列文档治理批处理。
- 代码类下一候选尚未冻结；先完成真机验证或文档治理任务卡，再依据当时 backlog 决定新批次。真机/OEM 依赖项继续后置，不得阻断已验收代码批次。

## 本次交接自检（2026-08-27）

- projectReview 横轴 freshness：01/03/04/20/21 分别 `STALE(16/12/9/9/48)`；07 与功能路径索引为 `CONFIG-ERROR`。这是软信号/文档治理待办，未伪造为已修复。
- feature_sync：`--struct` 通过；`--backlog` 报 F-AI-MEAL、F-DISH、F-FAMILY、F-INGREDIENT、F-MEAL、F-RECOMMEND、F-TIMELINE、F-TOOLS、F-WEEKPLAN 存在历史回写欠账。

## Bug-5305 手动食材分类优先收口（2026-08-27）

- 结论：**ARCH_ACCEPTED / AUTOMATED_GATES_PASS**，无 AF；`BLUEPRINT_STATE.md` 已转 `TURN=NONE`。
- 范围：仅把既有 `groupTouched` guard 固化为可测试策略并新增 Android 回归；未改 `FoodGroup.classify`、Repository、schema/seed、DI 或 UI。
- Runtime：`DEV-5305-01` 仍在 `真机验证/真机待验证清单_202608271041.md`，为 `PENDING_DEVICE_VERIFICATION`；未作真机 PASS 声明。

## P5-A Repository Mutation Boundary 收口（2026-08-26）

- 结论：**ARCH_ACCEPTED / AUTOMATED_GATES_PASS**；`BLUEPRINT_STATE.md` 已转 `TURN=NONE`。
- 范围：Home/Timeline/WeekPlan 与 `DayAutoGenerator` 的 MealRecord mutation 已收敛到 `MealRecordUseCase`；未改 schema、Repository 兼容 read、AI 算法/prompt 或 UI 视觉。
- 自动化：强制 shared/Android tests、shared compile、debug/release assemble 均有成功终态；release rerun 最终 `BUILD SUCCESSFUL in 7m53s`（83 tasks）。质量脚本通过。证据见 `arch_evidence/COOKBOOK_MEAL_ARCHITECTURE_EVOLUTION_PHASE5/EVIDENCE.md`。
- Runtime：`DEV-P5-01~05` 仍在 `真机验证/真机待验证清单_202608271041.md`，状态 `PENDING_DEVICE_VERIFICATION`；不得写真机 PASS。
- 后续：P5-B 必须先重新 Reality Verification（compatibility read/projection、Repository 内 preference/ratio policy）；**Phase 6 未授权**，不得清理兼容 API/Repository 行为。

## OBS-NEXT C批收口

- 已确认：`OBS-NEXT-A/B=AUTOMATED_GATES_PASS`。
- 未确认：`Runtime=PENDING_DEVICE_VERIFICATION`；不能声称真机 PASS、`ARCH_ACCEPTED`、`ACCEPTED` 或 `CLOSED`。
- 用户决定：真机验证末期统一执行；该待验证项不阻断当前代码。
- 引用：`architecture/ADR-0002-observability-trace.md`、`arch_evidence/MEAL-DATA-CONSOLIDATION-03/MDC3_CLOSE_PREPARATION.md`。

> **COOKBOOK Meal Architecture Evolution Phase 2 CODE 交付（2026-08-23）**：已读取 `COOKBOOK_MEAL_ARCHITECTURE_EVOLUTION_BATCH_PACKAGE.zip`，仅执行 UseCase Migration。新增 `MealRecordUseCase`/`MealRecordDraft`，AddMeal 的 create/save/query 相关入口经 UseCase 编排，保留 `MealRecordRepository`、旧模型、`meal_record` storage 和 Legacy Adapter；未执行 Projection/AI/Repository/Schema 变更。专项 UseCase 测试、`:shared:testDebugUnitTest`（117 tests）、`:androidApp:testDebugUnitTest`（15 tests）、`:androidApp:assembleDebug` 均通过。Evidence 已写入 `arch_evidence/COOKBOOK_MEAL_ARCHITECTURE_EVOLUTION_PHASE2/EVIDENCE.md`，BLUEPRINT_STATE 已转 `CODE_COMPLETE / PENDING ARCH REVIEW`、`TURN=REVIEW`；待提交 hash 回填后由 ARCH 复核。

> **COOKBOOK-OBSERVABILITY-AND-MEAL-FLOW-GOVERNANCE-PHASE CODE 交付（2026-08-23）**：已按任务包 `COOKBOOK_OBSERVABILITY_AND_MEAL_FLOW_GOVERNANCE_PHASE.zip` 完成 Trace Diagnostic、Architecture Quality 门禁增强、五类 Meal Flow State Contract（AI Recommend/Food Search/Inventory Select/New Dish/Edit Meal）及经验沉淀。统一语义为 SAVE STATE → RESTORE STATE → MERGE RESULT；库存明确为同页 operation-backed，不伪造导航回传。静态门禁与 Python 4 tests、`:shared:testDebugUnitTest`、`:androidApp:testDebugUnitTest`、`:androidApp:assembleDebug` 均通过；真机尚未执行，保持待验证。当前 `BLUEPRINT_STATE.md` 已转 `CODE_COMPLETE / PENDING ARCH REVIEW`、`TURN=REVIEW`。实现/状态原子交付记录为 `d54d214b10c0`，等待 ARCH 复核。

> **FOUNDATION-OBSERVABILITY-01 CODE 续作（2026-08-22）**：已按冻结 allowlist 完成 `STEP-OBS-01~07` 的当前工作树实现：新增 `LogLevel/TraceModel/Logger`，统一 `CookbookLog` sink，`AppLogger` 改为 JSONL/session/seq/脱敏/单 executor，并保留原 CrashActivity 流程；新增三份测试。`:shared:testDebugUnitTest`、`:androidApp:testDebugUnitTest`、`:androidApp:assembleDebug` 均通过。真机清单已更新为 `真机待验证清单_202608221700.md`，E-OBS-01/03 与 E-MDC3-01~03 保持待验证，E-OBS-02 为 `DEFERRED_TO_FIRST_INSTRUMENTATION_BATCH`。`BLUEPRINT_STATE.md` 已转 `CODE_COMPLETE / PENDING ARCH REVIEW`、`TURN=REVIEW`。当前改动尚未提交（未获 commit 授权）；下一步由 ARCH 按 allowlist、INV/T/E、隐私/并发和当前 diff 复核，禁止写 ACCEPTED/真机 PASS。

> **FOUNDATION-OBSERVABILITY-01 ARCH 交接（2026-08-22）**：日志系统蓝图已按用户要求由本机 ARCH 完成冻结，`BLUEPRINT_STATE.md` 为 **`BLUEPRINT_READY / PENDING CODE`、`TURN=CODE`、`Holder=CODE`**。coder 新会话必须按顺序读：①本文件；②`context_memory/BLUEPRINT_STATE.md` 顶部 Foundation 条目；③`feature/FOUNDATION_OBSERVABILITY_01_CODE_BLUEPRINT.md` 的 §0、§2、§3.0/§3.6/§3.7、§4、§8~§10；④`context_memory/2026-08-22_FOUNDATION_OBSERVABILITY蓝图返工.md`。只执行 `STEP-OBS-01~07` 与 allowlist；目标=单一 AppLogger 文件出口、固定 JSONL、Trace 状态机、严格脱敏与 release 普通日志零落盘。不得改业务/Repository/schema/UI、不得新增日志出口或真实业务埋点，遇唯一行为缺口追加 `Q-OBS-01-NN` 并交回 ARCH。实施后必须跑 T-OBS-01~11、三条构建命令、真机清单 E-OBS；MDC3 的 E-MDC3-01~03 仍是独立待验证项，禁止伪造 PASS/ACCEPT。

> **MEAL-DATA-CONSOLIDATION-03 CODE 交付（2026-08-21）**：按 `COOKBOOK_MEAL_DATA_CONSOLIDATION_03_ARCH_BLUEPRINT_R1.zip` 执行 Read API 演进治理。已补 `MealDayContent` Stable Read Contract、`DayMealCardData` Shared Read Projection 与 Repository Compatibility API KDoc 标记；新增旧/新读取结果等价测试、`observeTimelineWindow` revision token 生命周期测试、调用关系证据与 F-MEAL 决策回写。未删除旧 API、未修改 schema、未重构 Repository、未改变用户行为。`:shared:testDebugUnitTest` 通过（672 tests）。当前 `BLUEPRINT_STATE.md` 为 `CODE_COMPLETE / PENDING ARCH REVIEW`、`TURN=REVIEW`、`Holder=ARCH`，待 ARCH 复核后收口；工作区其他用户改动未纳入本批。

> **MEAL-DATA-CONSOLIDATION-02 CODE 交付（2026-08-21）**：已按外部包 `COOKBOOK_MEAL_DATA_CONSOLIDATION_02_ARCH_BLUEPRINT_R1_FINAL.zip` 执行 Projection Boundary Refinement。`MealDayContent → MealDayCardProjector → DayMealCardData` 已补稳定契约 KDoc、边界测试与 F-MEAL/F-TIMELINE/F-NUTRITION 文档同步；未改 schema、未新增 MealPlan/领域实体/Projection Service、未改变用户行为。`:shared:testDebugUnitTest` 通过。当前 `BLUEPRINT_STATE.md` 为 `CODE_COMPLETE / PENDING ARCH REVIEW`、`TURN=REVIEW`，待 ARCH 复核后收口；工作区另有用户原有的外部方案 zip 删除/新增变更，未纳入本批范围。

> **UEN CODE 交付更新（2026-08-20）**：远端 `8b824f0d` 将 `TURN` 握手切为 `CODE` 后，luna 已完成统一添加入口/悬浮导航栏工作区实现，并推送提交 `6f2b201c`。`AiMealBody`、`UnifiedAddMealScreen/State`、统一路由、悬浮胶囊导航、嵌入式单天手动/周期手动分支及 `E-UEN-01~15` 真机清单已落地；`:shared:testDebugUnitTest`、`:androidApp:testDebugUnitTest`、`:androidApp:assembleDebug` 和结构体检均通过。当前 `BLUEPRINT_STATE.md` 为 `REVIEWING/TURN=REVIEW`；待远程 ARCH 逐文件复核、质量复审与真机验证。

> 更新时间：**2026-08-20（同日第五次）· 统一添加入口 + 悬浮导航栏蓝图已升级为项目 L7/48-GC 全量合规版，`BLUEPRINT_STATE.md` 已正式握手，TURN=CODE 交给远程主机 luna 落地编码**
> 当前工作域：延续同日前三次交接（`edbaa711`/`b71bce13`/`9cc864c9`）之后，用户开了新话题——App 里"添加餐食"入口太碎（首页+号只管单天手动、AI快捷记藏在顶栏、周期记和一周计划各自独立）+ 导航栏太高，要求方案讨论。走了"讨论→用户拿去网页版ChatGPT出方案→我审核ChatGPT方案+补代码级事实校正→出实施蓝图"的链路后，**用户当面指出蓝图交接漏了项目"蓝图模式"协议的两处硬要求**：①`BLUEPRINT_STATE.md` 握手状态文件未同步登记本批次、`TURN` 未设为 `CODE`；②蓝图本身未按 `12_多模型协作与实施蓝图规范.md` 的 L7/48-GC 全量模板成文（缺 §0.1 颗粒度勾销表/allowlist固定块/INV表/STEP编号脚本/测试矩阵等）。两处均已按用户选定方案（"补齐全量模板后再交付"）修复。**本 session 本身不写业务代码，只产出规划/蓝图文档**，实际编码交给用户远程主机上的 `luna` 执行。
> 执行角色：本机 ARCH（Claude Sonnet）主导方案讨论、审核外部（ChatGPT）方案、补代码级事实校正、出 L7 全量蓝图、维护 `BLUEPRINT_STATE.md` 握手；`dev_research_engineer` 子智能体独立完成 Material3 升级可行性调研（部分结论因本环境网络限制未闭环，已标注）。

---

## 一、先读清单（按序）

1. **`SESSION_交接.md`**（本文件）——当前状态与 ⏭下一步。
2. **`BLUEPRINT_STATE.md` 当前批次 `AIMEAL-UNIFIED-ENTRY-NAVCOMPACT-01`**——**唯一握手状态**，`状态=BLUEPRINT_READY`、`TURN=CODE`，luna 开工前必读，交付后要按此表回填基线sha/交付commit/全景图回写并把状态转 `ACCEPTED`。
3. **`.ai-context/docs/feature/统一添加入口+悬浮导航栏_实施蓝图.md`**——**交给 `luna` 执行的唯一权威实施文档，已是 L7/48-GC 全量合规版**（§0.1颗粒度勾销表/§2.1 allowlist固定块/§13 STEP编号脚本/§17 INV表/§18测试矩阵齐全）。核心审核修正在 §3-§12：ChatGPT 原方案有一个不成立的假设（"四个 Body 各自独立需要四份 Draft 状态"），实际 AI 相关的单天/周期两格本来就共享同一个 `AiMealInputViewModel`，不要只看 ChatGPT 原文档而漏看这个修正。
4. **`.ai-context/docs/外部方案/Cookbook_统一添加入口与悬浮导航栏_实现规格_20260820.md`** + **`Cookbook_统一添加入口_页面级交互与状态流转规格_20260820.md`**——ChatGPT 产出的原始方案（产品模型/2×2矩阵/悬浮栏dp规格/状态机骨架），已审核通过，蓝图开头声明了与本文件冲突时以蓝图为准的优先级。
5. **`F-AI-MEAL/30_待办.md` 的 `AIMEAL-UX-REDESIGN` 条目④**——统一入口范围决策的完整历史（承载形式/2×2矩阵/收编范围/AI推荐不并入/`AiPlanScreen`死代码误判已撤回）。
6. **`F-TOOLS/30_待办.md` 的 `NAV-COMPACT`/`MATERIAL3-UPGRADE` 两条**——悬浮导航栏决策 + Material3 升级独立评估项（调研结论已登记，含遗留未闭环项清单）。
6. **`F-MEAL/30_待办.md` 的 `HOME-MERGE` 条目**——首页"今日+计划"合并三天视图，本次讨论的第二个话题，**尚未过 apple_ux_designer 设计，排在统一入口之后做**，本 session 未深入。
7. **`F-WEEKPLAN/30_待办.md`** 底部的跨功能提醒——指回 `AIMEAL-UX-REDESIGN`，`WeekPlanScreen` 会成为"周期+手动"分支承载体。

---

## 二、工作规则（当前任务域）

- **本 session 定位是"规划/蓝图"，不是"编码"**：用户明确要把蓝图交给远程主机上的 `luna` 去实施，本机（当前会话）不要抢着写统一入口/悬浮导航栏的业务代码，除非用户明确改口要求本机做。
- **蓝图是审核过的产物，不是照抄 ChatGPT**：ChatGPT 原方案质量很高（产品模型/dp值/状态机骨架直接采纳），但它没读过项目真实代码，"四个 Body 各自需要独立 Draft 状态"这个假设是错的——`AiMealInputViewModel` 已经用 `quickDraftText`/`periodInputs` 两个字段原生支持"切模式不丢草稿"，统一入口的 AI 两格（单天AI/周期AI）应该复用同一个 VM 实例，不是新建两份状态。这类"外部方案 vs 本地真实代码"的审核工作模式，以后遇到类似"网页AI出方案→本机审核校正→出蓝图"的需求可以复用。
- **Material3 升级是独立评估项，不阻塞当前工作**：调研已完成但留了几个关键未闭环点（Kotlin 是否强制升 2.0+、Material3 1.3.0 精确对应的 Compose UI 版本号），登记进 `MATERIAL3-UPGRADE` 待办，**真要做之前必须先开分支实测钉死这些点**，不能直接照调研报告动手。
- 其余通用规则见 `.ai-context/rules/通用规则.md` + 全局 `~/.ai-context/GLOBAL.md`。

---

## 三、当前状态

### 本次完成的工作

| # | 工作 | 产出 | 状态 |
|---|---|---|---|
| ① | 统一添加入口方案讨论（承载形式/收编范围/AiRecommendScreen是否并入/悬浮导航栏形态） | 对话内 4 轮 `AskUserQuestion` 拍板，登记进 `F-AI-MEAL/30_待办.md` `AIMEAL-UX-REDESIGN`④ | 完成 |
| ② | 首页布局（`HOME-MERGE`）+ 导航栏（`NAV-COMPACT`）方案讨论 | 登记进 `F-MEAL/30_待办.md`/`F-TOOLS/30_待办.md`，均标"方案讨论级，需先过apple_ux_designer" | 完成（HOME-MERGE 排后面，本次未深入） |
| ③ | 排查澄清：`AiPlanScreen.kt` 不是死代码 | 撤回此前误判，登记进 `AIMEAL-UX-REDESIGN`④e | 完成 |
| ④ | 生成独立讨论材料文档供用户粘贴到网页版ChatGPT | `temp/claude/统一添加入口+悬浮导航栏_讨论材料_20260820.md`（含角色设定/项目背景/现状拆解/已锁定决策/需产出内容） | 完成（temp/ 已 gitignore，不进仓库） |
| ⑤ | 审核 ChatGPT 产出的两份外部方案 | 归档至 `.ai-context/docs/外部方案/`，用户放的位置，本次只读未改动原文件 | 完成 |
| ⑥ | 代码级事实核验（实读 `AiMealInputViewModel.kt`/`AiMealInputSheet.kt`/`AddDayFoodScreen.kt`/`WeekPlanScreen.kt`/`UnsavedGuard.kt`/`MainScaffold.kt`/`Destinations.kt`），发现并修正 ChatGPT 方案的状态管理假设错误 | 见 ⑦ | 完成 |
| ⑦ | 出实施蓝图（第一版） | `.ai-context/docs/feature/统一添加入口+悬浮导航栏_实施蓝图.md`（14节轻量补丁体裁） | 完成（后被 ⑪ 升级） |
| ⑧ | Material3 1.1.2→1.3.x 升级可行性调研（`dev_research_engineer` 子智能体后台完成） | 结论登记进 `F-TOOLS/30_待办.md` `MATERIAL3-UPGRADE`（含遗留未闭环项） | 完成 |
| ⑨ | 全景图新鲜度自检 | 见下方第六节 | 完成 |
| ⑩ | 提交+推送第一版蓝图（用户授权本次推送） | commit `a3849046`，已 push `origin/master` | 完成 |
| ⑪ | **用户指出蓝图交接流程漏了项目"蓝图模式"两处硬要求**：`BLUEPRINT_STATE.md` 未握手/`TURN`未设、蓝图未按L7/48-GC全量模板成文 | 询问用户后按"补齐全量模板"方案处理：①蓝图重写为 L7 全量合规版（§0.1颗粒度勾销表48条全部勾销/§2.1 allowlist固定块/§13 STEP编号脚本/§17 INV表16条/§18测试矩阵/§21交付/§22 Q-AF格式）；②`BLUEPRINT_STATE.md` 新增当前批次 `AIMEAL-UNIFIED-ENTRY-NAVCOMPACT-01`，`状态=BLUEPRINT_READY`，`TURN=CODE` | 完成 |
| ⑫ | 会话交接（本文件，覆盖式更新） | — | 完成 |

### 未完成/明确留待下次的部分

- **蓝图尚未被 `luna` 执行**：`BLUEPRINT_STATE.md` 当前批次 `TURN=CODE`，等 luna 按蓝图 §20 Phase 1-5 执行。下次交接时先看 `BLUEPRINT_STATE.md` 本批状态是否已变化（luna 或用户可能已推进），若已交付要按蓝图 §21/§24 走 ARCH 复核（diff逐STEP核对+实跑三条构建命令+INV逐条对照+真机验证长按复制粘贴），通过后回写状态→`ACCEPTED`+基线sha+交付commit+全景图回写+`TURN`→`USER`。
- **`HOME-MERGE`（首页改版）尚未设计**：按讨论顺序排在统一入口之后，`apple_ux_designer` 门禁还没走。
- **Material3 升级的遗留未闭环项**：Kotlin 是否强制2.0+、精确 Compose UI 版本号、issue tracker 具体状态，见 `MATERIAL3-UPGRADE` 待办，真要做之前必须先在分支里实测钉死。
- 上一轮遗留的"全项目其余 `ModalBottomSheet`+Snackbar 组合排查"（`EatenAdjustSheet`/`ActionSheet`）——本次调研顺带确认了这些文件确实都用 `ModalBottomSheet`（Material3升级评估的一部分佐证），但排查/修复本身仍未做，如果 Material3 升级被采纳会一并解决，否则仍是独立欠账。
- `21_AI与网络请求策略（专属）.md` 仍 STALE(42)，与历次交接一致，本次改动未触及其监视路径，继续 DEFER。

---

## 四、⏭ 下一步

1. **确认 `luna` 在远程主机的实施进度**：先看 `BLUEPRINT_STATE.md` 当前批次字段（`TURN`是否还是`CODE`）。如果还没开始，提醒用户去启动 luna；如果已完成，按蓝图 §20-§21 验收（STEP勾销表逐条diff核对 + 构建三件套 + `google_quality_engineer` 复审 + 真机验证长按复制粘贴菜单是否恢复，即 `E-UEN-13`），通过后回写 `BLUEPRINT_STATE.md` 状态。
2. **验收通过后**：更新真机待验证清单登记新入口的验证项，同批维护 `F-AI-MEAL`/`F-MEAL`/`F-WEEKPLAN`/`F-TOOLS` 四个 Feature 文件夹的 `20_实现.md`/`30_待办.md`。
3. **`HOME-MERGE` 首页改版设计**：等统一入口落地效果稳定后，按讨论顺序过 `apple_ux_designer` 门禁出正式交互规范。
4. 若有余力：评估是否要为 Material3 升级开一个短分支，实测钉死遗留未闭环项（不代表要真升级，只是把决策依据补完整）。

---

## 五、本轮沉淀

- 待办：`F-AI-MEAL/30_待办.md`（`AIMEAL-UX-REDESIGN`④）、`F-MEAL/30_待办.md`（`HOME-MERGE`）、`F-TOOLS/30_待办.md`（`NAV-COMPACT`、`MATERIAL3-UPGRADE`）、`F-WEEKPLAN/30_待办.md`（跨功能提醒）。
- 新文档：`.ai-context/docs/feature/统一添加入口+悬浮导航栏_实施蓝图.md`（新建）、`.ai-context/docs/外部方案/`（用户放入的 ChatGPT 产出，2份，本次已审核引用）。
- 工作模式沉淀（记入本文件第二节"工作规则"，暂未提炼成独立经验条目，如果这个"外部AI出方案+本机审核校正"模式后续复用超过2次，下次交接时考虑升级成正式经验条目）：审核外部方案时，光看方案文字不够，必须实读对应的项目真实代码去验证方案的架构假设是否成立——本次抓到的"四个Body各自独立Draft"假设错误就是典型例子，如果直接照抄会导致 luna 做出不必要的状态拆分重构。

---

## 六、全景图新鲜度（每次交接必填，禁止留空或写"待查"）

**最近一次执行（2026-08-20，本次交接时重跑）**：本次改动全部是文档（待办登记+新蓝图文档+归档外部方案），未触碰任何监视路径覆盖的源码，横轴判定与上次交接（`9cc864c9`）一致，无新增 STALE。

### 横轴（`review_freshness.py`）

| 册 | sha | 之后提交数 | 判定 | 处置 |
|---|---|---|---|---|
| 00_导读与索引 | — | — | N/A | — |
| 01_架构与技术底座 | 742611ce | 7 | STALE(7) | DEFER，与历次交接一致 |
| 02_业务流程全景 | 742611ce | — | N/A | — |
| 03_界面与交互 | 742611ce | 2 | STALE(2) | DEFER，与历次交接一致 |
| 04_数据层 | 742611ce | 1 | STALE(1) | DEFER，与历次交接一致 |
| 05_诊断地图 | 742611ce | — | N/A | — |
| 06_约定与红线 | — | — | N/A | — |
| 07_项目现状 | None | — | CONFIG-ERROR（已知良性） | 不处置 |
| 08_决策记录 | — | — | N/A | — |
| 09_跨功能待办与战略 | — | — | N/A | — |
| 20_健康与算法逻辑（专属） | 742611ce | 3 | STALE(3) | DEFER，与历次交接一致 |
| 21_AI与网络请求策略（专属） | 742611ce | 42 | STALE(42) | DEFER（既有欠账延续，本次改动未触及其监视路径） |
| 22_预设与参考资料治理（专属） | 742611ce | 0 | FRESH | — |
| 功能路径索引 | None | — | CONFIG-ERROR（已知良性） | 不处置 |

无 `ANCHOR-MISMATCH`。两个 `CONFIG-ERROR` 均为已知良性（同历次交接）。

### 纵轴（`feature_sync_check.py`）

- `--struct`：**[OK] 结构体检通过**。
- `--backlog --since 9cc864c9`：**[OK] 无历史欠账**。
- `--range`：本次未做任何源码改动，无需跑 `--range`（该检查针对代码改动是否同批更新 Feature 文件夹，本次只有 Feature 文件夹自身的待办登记 + 新增规划文档，不涉及代码）。
- `--emit-index --write`：本次未执行——未新增/改动任何 Screen/VM/Repo 等代码落点，只是规划文档，功能路径索引不受影响。

止损条件见 `08` D-20（横轴）与 D-25（纵轴）。下次交接重跑本命令覆盖本表。
