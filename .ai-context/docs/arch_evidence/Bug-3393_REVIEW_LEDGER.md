# Bug-3393 · REVIEW 增量台账

| 时间 | reviewer | commit 范围 | 检查 | 可见证据 | 结论 | 下一步 |
|---|---|---|---|---|---|---|
| 2026-08-27 | Sol（初审） | `2d946237..a59dcb0a` | STEP-3393-03 日志字段 | `HomeViewModel.kt` 中 `stateChanged` 的 state 曾为 `effective_viewer`，与蓝图不符 | AF-3393-01 | 已在 `3de38f30` 改为 state=`nutrition_member_switcher`；待二审复核 |
| 2026-08-27 | Sol（初审） | 同上 | T-3393-05 | 原 `HomeFocusSwitcherTest` 仅 DTO 映射，未验证真实 Home 流程 | AF-3393-02 | `3de38f30` 新增 SQLite+HomeViewModel 流程测试；待二审复核 |
| 2026-08-27 | Sol（初审） | 同上 | 交接一致性/allowlist | SESSION 有旧清单和“未启动”残留；中文路径使 checker 默认输出误报 | AF-3393-03 | `3de38f30` 修正 SESSION；allowlist 核对须以 `core.quotePath=false` 的等价命令执行 |
| 2026-08-27 | REVIEW | `2d946237..3de38f30` | 二审启动 | Sol 二审受账户用量限制中断，未生成结论 | 待查 | 已交给独立替代复核者；从本行继续 |
| 2026-08-27 | Codex 独立替代复核 | `2d946237..3de38f30` | 二审启动/范围冻结 | 已读取项目 BLUEPRINT 常驻声明、工作流与现有初审 AF；本轮唯一写入此台账 | 进行中 | 待查：蓝图合同/allowlist、HomeViewModel 日志、真实 VM+SQLite 测试、SESSION/真机清单、定向测试证据 |
| 2026-08-27 | Codex 独立替代复核 | `3de38f30` | STEP-3393-03 日志合同 | `TraceModel.kt:201` 签名为 `(screen, previous, state, reason, traceId)`；`HomeViewModel.kt:316` 实参=`home,effective_viewer,nutrition_member_switcher,absent_viewer_fallback`，精确符合蓝图 §5 | PASS | AF-3393-01 已关闭；仍待定向测试实跑 |
| 2026-08-27 | Codex 独立替代复核 | `3de38f30` | T-3393-05 静态构造链 | `HomeFocusSwitcherTest.kt:38-99` 使用 `JdbcSqliteDriver(IN_MEMORY)`+真实 `CookbookDatabase`/repositories/`HomeViewModel`；制造 member 缺席与餐食/营养，再断言 tab、`todayNutrition`、持久化 viewing 指针 | PASS（静态） | 待运行该定向测试，确认非编译或时序假绿 |
| 2026-08-27 | Codex 独立替代复核 | `a59dcb0a..3de38f30` | SESSION / 真机状态 | `SESSION_交接.md:5-8` 仅引用最新 `真机待验证清单_202608271041.md`、状态=`CODE_COMPLETE / TURN=REVIEW`；`DEV-3393-01` 保持 `PENDING_DEVICE_VERIFICATION` | PASS（静态） | 待查完整提交范围 allowlist 与当次测试结果 |
| 2026-08-27 | Codex 独立替代复核 | `3de38f30` | T-3393-05 定向实跑 | `scripts\\build-cli.bat :androidApp:testDebugUnitTest --tests "com.sxdbsm.cookbook.android.ui.home.HomeFocusSwitcherTest" --rerun-tasks` 在 604s 工具上限超时，未输出测试报告或失败栈 | 缺证据 | 不可将静态测试结构或历史口头 PASS 视为当次运行证据；待用可观测的定向命令完成 |
| 2026-08-27 | Codex 独立替代复核 | `3de38f30` | T-3393-05 定向实跑补证 | 同次命令实际生成 `androidApp/build/test-results/testDebugUnitTest/TEST-com.sxdbsm.cookbook.android.ui.home.HomeFocusSwitcherTest.xml`：tests=2、failures=0、errors=0；真实 VM 回退用例耗时 5.927s | PASS | 外层 CLI 未在 604s 内结束，故不将其等同完整 Gradle BUILD SUCCESSFUL；但定向用例本身已有结果 XML 证据 |
| 2026-08-27 | Codex 独立替代复核 | `0e5422c6..3de38f30` | allowlist 初检 | 改动恰为 10 个蓝图授权路径；`blueprint_check.py` 默认 Git 中文 quotePath 转义将 5 个中文文档误报为 UNLISTED（与初审已知限制一致） | 待复核 | 待以无持久化 `core.quotePath=false` 环境重跑 checker；禁止将默认误报判为范围越界 |
| 2026-08-27 | Codex 独立替代复核 | `0e5422c6..3de38f30` | allowlist 定论 | 以进程级 `core.quotePath=false` 重跑 `blueprint_check.py` 后，中文路径误报消失；仍唯一报 `.ai-context/docs/context_memory/SESSION_交接.md` UNLISTED。该文件在蓝图 §3 固定 allowlist 中不存在 | AF-3393-04 OPEN | 最小修复：ARCH 按协议为该必要 SESSION 变更补正式受控例外/重冻结授权，再由 CODE 回填同一批证据；不得扩大源码范围 |
| 2026-08-27 | Codex 独立替代复核 | `0e5422c6..3de38f30` | 蓝图交付证据闭合 | 蓝图 §9 的 `STEP-3393-01~05`/`Gate-3393-01` commit 栏仍为“待本地提交回填”；`BLUEPRINT_STATE.md:12` Base 仍为“HEAD at CODE start”，没有精确基线 SHA，违反交付四列映射的可反查要求 | AF-3393-05 OPEN | 最小修复：仅回填精确基线 `0e5422c6`、交付链 `2d946237..3de38f30`、文件/测试 ID 映射及当次 XML/命令结果；不改生产代码或测试语义 |
| 2026-08-27 | Codex 独立替代复核 | `0e5422c6..3de38f30` | 终审二审汇总 | 功能合同、日志字段、Shared/Android 定向测试 XML、唯一真机清单与 SESSION 状态均 PASS；未发现运行时/数据安全缺陷。AF-3393-04/05 为冻结蓝图范围与可反查交付证据阻断 | 阻断 | 仅完成两项治理最小修复并复跑 allowlist checker 后可定向复审；真机 DEV-3393-01 仍 PENDING、不要求伪造 PASS |
| 2026-08-27 | Codex 独立替代复核 | `0e5422c6..HEAD` | AF-3393-04 返修复核 | 蓝图 §3 已精确列出 `SESSION_交接.md`，例外仅限本批状态和唯一真机清单文件名；进程级 `core.quotePath=false` 执行 `blueprint_check.py --range 0e5422c6..HEAD`：全部改动均匹配 allowlist，exit 0 | CLOSED@WORKTREE | 受控例外未放宽源码或产品规则范围；AF-3393-04 关闭 |
| 2026-08-27 | Codex 独立替代复核 | `0e5422c6..HEAD` | AF-3393-05 返修复核 | 蓝图与 `BLUEPRINT_STATE.md` 均回填基线 `0e5422c6`；§9/State 记录交付链 `2d946237→a59dcb0a→3de38f30`、Shared XML 7/7、Android 真实 VM+SQLite XML 2/2 及三条历史构建门禁 | CLOSED@WORKTREE | 基线、提交、测试证据可反查；AF-3393-05 关闭 |
| 2026-08-27 | Codex 独立替代复核 | `0e5422c6..HEAD` | AF-3393-04/05 定向终审 | 仅 AF 修复文档处于工作树；初始功能/安全/测试结论未变；无未授权源码 diff，真机 `DEV-3393-01` 仍 PENDING_DEVICE_VERIFICATION | 通过（可 ARCH_ACCEPT） | 审查结论不替代后续由持球方写入 BLUEPRINT_STATE 的 ACCEPTED 状态 |
