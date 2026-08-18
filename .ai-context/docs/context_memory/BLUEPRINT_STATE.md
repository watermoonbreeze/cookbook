# BLUEPRINT_STATE
唯一握手状态文件。State 仅承载抽象角色和生命周期 Truth，禁止具体模型身份。

> **回写规则（2026-08-17 新增，防全景图漂移主承重）**：任一批次「状态」字段写入 `ACCEPTED`/`ACCEPT` 时，本批表格必须同批填写「全景图回写」字段，不得留空。三选一：
> - `07 §能力成熟度 +1 行（<sha>）` —— 本批有产品代码改动且已回写（限一行，禁止借机扩写 `07`）
> - `N/A — 纯治理/文档批次，无产品代码改动`
> - `DEFER — <理由>，到期批次 <批次号>`（到期批次的 ACCEPT 不得再次 DEFER，最多延一次）
>
> 该字段空白即视为批次未收口，不得转 TURN。设计与判据见 `projectReview/00` §落图与回写门禁、`projectReview/08` D-20。
---
## 当前批次：GOVERNANCE-PROJECTREVIEW-ANTIDRIFT-01（2026-08-17）
| 字段 | 值 |
|---|---|
| 任务/批次 | GOVERNANCE-PROJECTREVIEW-ANTIDRIFT-01 — 修正全景图内容漂移 + 建立防漂移长效机制 |
| 状态 | **ACCEPT / LOCAL ARCH DECISION** |
| TURN | USER |
| CODE | N/A（纯治理/文档批次，未委派 CODE 执行） |
| ARCH | 本机 ARCH |
| 背景 | 独立 Opus 子智能体评估发现 `07_项目现状.md` 停更 12 天/181 提交未同步 L1/K1i，且自称"SESSION 为最高事实源"与 `PROJECT.md` 冻结层级相反；修复后二次评估设计防漂移机制，过程中机械核验又发现 `04`"38 表"（实 39）、`03`"21+ 页面"（实 34）两条既有漂移。 |
| 已修正 | `00/01/03/04/05/06/07/08/20/21/22` 共 11 册：真相源倒挂订正、L1/K1i 补齐、D-16 状态后缀移除、新增 D-20、页脚格式统一（含此前批次误上抬 sha 的订正）、`04`/`03` 数字订正、`07` 移出死历史流水 17 行（→`context_memory/_archive/`）。 |
| 新建机制 | G1 主承重：本文件批次表新增「全景图回写」必填字段（见上方说明区）；G2 兜底：`SESSION_交接.md` 新增固定小节"全景图新鲜度"；G3 硬断言：Tier B 六册页脚新增「监视路径」「事实锚」，脚本 `.ai-context/tools/review_freshness.py`。11 册按 Tier A/B/C 分级见 `projectReview/00`；完整方案见 `08` D-20；退出条件（含"连续2次G2不处置即删除G2和脚本"）已写入 D-20。 |
| 全景图回写 | `N/A — 纯治理/文档批次，无产品代码改动` |
| 独立复核 | 用户要求方案评估/执行质量类复核统一交给独立 Opus 子智能体（不由本机 ARCH 自行代劳）。首次收口时连续 4 次在启动早期断连失败（基础设施问题），本机 ARCH 手工核验后先行无人值守提交（`bb8fe4f4`）；新会话第 5 次仍断连，**第 6 次成功**，独立复核针对交接文档点名的 4 项逐一给出判断，结论**有条件通过**（需修 3 项，无阻断项）。已按复核意见修复：①`review_freshness.py` 的 `check_volume` 不再把无监视路径/事实锚的册误标"Tier C"（改中性措辞，Tier 分级唯一真相源仍是 `projectReview/00`，不在脚本内重复维护防二次漂移）+ 补上"仅事实锚无路径也判 FRESH"的遗漏分支；②`.gitignore` 补 `__pycache__/`+`*.pyc` 全局规则；③本字段（全景图回写）措辞去掉自我削弱的"从下一个含产品代码改动的批次起生效"，改回规则原文。另顺手修：`FOOTER_PATH_RE`/未知锚 key 收紧匹配防静默误判、`03` 页脚全角引号→半角+`-iname`→`-name`对齐脚本大小写敏感、`06` 补页脚字段格式契约（字段内禁用`·`）、`08` 的 D-17~D-20 后缀统一为`｜生效`（原 4 条用`·生效`与自己新定的规则不符）。**未采纳/留待观察**：STALE 告警疲劳风险（复核建议加"复核至：&lt;sha&gt;"抑制字段）——这是需要用户拍板的设计改动，非本次机械修复范围，已记入 `08` D-20 备注供下次交接判断是否处理。 |
| 下一步 | ①本批次独立复核已收口，`git add -A` + commit（含本次修复）；②下一个真实 CODE 批次起，ARCH 复核须验证「全景图回写」字段已填；③裁决 `GOV-BP-P3-01`；④清真机验证积压。 |

## 上一批次：GOVERNANCE-UBF-RETIRE-AND-REFRAME-01（2026-08-17）
| 字段 | 值 |
|---|---|
| 任务/批次 | GOVERNANCE-UBF-RETIRE-AND-REFRAME-01 — UBF 退休 + 模型执行力评估框架重设计 + ARCH 角色本地化 |
| 状态 | **ACCEPT / LOCAL ARCH DECISION**（本机 ARCH 直接裁决，不再等待远程复核） |
| TURN | USER |
| CODE | N/A（本批为纯治理决策，未委派 CODE 执行） |
| ARCH | 本机 ARCH（自本批起，ARCH 角色由远程 ChatGPT 网页版转移为本机；见下） |
| Review mode | `LOCAL_DIRECT`（替代 `REMOTE_READ_ONLY_ARCH`，即日停用） |
| 决策 1 — UBF 退休 | `UBF-M0～M3`（含全部 Family-A/B/C 盲测、Commitment/Reveal、H1-H4、EGC-G05/G06、Universal Level/Profile/Selector 研究）正式终止，不再派生新批次。理由：跨度 08-11～08-15、142 个提交、0 行产品代码改动、Universal Level 数量/名称/阈值/映射全程=0；`Commitment/Reveal` 哈希在仓库内无脚本支撑（搜索 `hashlib`/`sha256`/`commitment`/`blind`/`Reveal` 均无结果），系未经工具验证的文本断言；执行者身份在证据链内多次自相矛盾（自报 model 与 package/ledger 执行者不一致）。完整证据与设计见 `.ai-context/docs/项目改造规划/UBF退休与模型执行力评估重设计.md`。 |
| 决策 2 — ARCH 角色本地化 | 即日起 ARCH 由本机直接承担，不再经远程 ChatGPT 网页版复核。 |
| 决策 3 — Phase 3A 待决 | `GOV-BP-P3-01`（Phase 3A 治理升级审计）**本批不裁决**，保持 `EXECUTED / PENDING ARCH REVIEW`，下一批由本机 ARCH 直接读 diff 裁决。 |
| 未变更 | Project Graph Phase 1/2 FROZEN 不变；`blueprint_protocol.md` Level/GC 体系不变；`14_模型执行力评估.md` 既有行只追加关闭说明，不删除/不覆盖。 |
| 下一步 | ①下一批直接裁决 `GOV-BP-P3-01`（ACCEPT/REWORK）；②真机验证积压（L1 的 E-L1-01~12、K1i 的 E-K1I-01/02，以及更早 AI记一餐 ~30 项）建议优先清理；③新真实 CODE 批次（当前模型 DeepSeek V4 Flash）按重设计后的循环执行，评估载体仅限真实 Cookbook 任务。 |

## 历史区块指针：UBF 支线（2026-08-11 ~ 2026-08-15，已归档）

本区块原有 `UBF-M0` 至 `UBF-M3-END-ACCEPT-AND-M3-TO-M4-NO-HANDOFF-01`（含 `GOV-BP-P3-01`、`Phase 3A Baseline` 两条前置批次）共 50+ 个批次条目，已整体搬至 `docs/context_memory/_archive/BLUEPRINT_STATE_UBF历史_20260817.md`（原文一字未改）。退休结论见上方当前批次；不删除、可反查，只是不再占用本文件默认读取的 token。

## 上一批次：L1 + K1i ARCH 独立复核通过 → 待真机验证（2026-08-08 更新）

| 字段 | 值 |
|---|---|
| 任务/批次 | 用户 2026-08-08 指示：转 CODE 实施 L1，随后指示"把有蓝图的全部做完，一起审核"。**L1、K1i 两份蓝图 CODE 已交付并经 ARCH 独立复核通过**（K1e `DISCARDED`、K1h 调研完成不变）。 |
| 状态 | **L1：ACCEPTED**（Google 终审无阻断 + copywriter 审校落地 + ARCH 独立复核通过）；**K1i：ACCEPTED**（Google 终审无阻断，走 L1 已落地正式分支无留桩 + ARCH 独立复核通过，1 处 allowlist 台账订正见下）；两份真机清单项 `真机待验证清单_202608082330.md` E-L1-01~12 + E-K1I-01/02 待真机验证；K1b 仍 `DRAFT·PARKED`（不变） |
| **TURN** | **USER**——ARCH 复核已完成（2026-08-08，审核模型，未参考 CODE 自评结论独立复核），批次关闭。交回用户做真机验证（E-L1-01~12 + E-K1I-01/02）或决定续做其他批次 |
| L1 CODE 交付 + ARCH 复核 | commit `ad1c5878`；蓝图 §9 台账已填；真机 E-L1-01~12；模型执行力台账 L1 行 ARCH 简评已补——**无阻断**：diff 逐文件核对 INV-L1-01~12、三条构建命令复验全绿（shared 652/652、androidApp 49/49、assembleDebug 均 BUILD SUCCESSFUL）、闸门唯一性 grep 确认（`SwitchableAiRuntime(` 生产代码仅 2 处、`isModelReady()` 未改、无 `CloudAiRuntime` 绕过注入） |
| K1i CODE 交付 + ARCH 复核 | commit `d7240d6f`；蓝图 §9 台账已填；真机 E-K1I-01/02；模型执行力台账 K1i 行 ARCH 简评已补——**无阻断**，但订正 1 处台账准确性问题：`DEFAULT_MESSAGE` 常量提取实际改了 L1 定义的 `CloudAiConsent.kt`（K1i allowlist 未授权，CODE 自评"allowlist 合规"表述与实情不符），核实功能安全（字面量不变、L1 全部测试仍绿）予以放行，已在 K1i 蓝图 §9 补记为受控例外；非阻断观察项：全量 androidApp 测试偶发 `CoroutinesInternalError`（协程生命周期泄漏，0 failures，建议 fast-follow）。L1 蓝图 §4.4/§0.1 失实注释已按 K1i §6 授权改写 |
| L1↔K1i 交叉依赖提醒 | 已闭环：K1i 的 `stream()` override 复用 L1 的 `cloudAiConsentGranted()` 闸门（同源判据+同源文案）；L1 蓝图"stream() 不重写"注释已改写为"已由 K1i 重写"（防生产假话）。**后续改动 `stream()`/`complete()` 任一须同时核对两处闸门** |
| ARCH 下一步 | ① 复核 L1 + K1i 交付（走查 diff + 实跑三命令，无阻断即关闭）；② 与用户一起做真机验证（E-L1-01~12 + E-K1I-01/02）；③ 之后决定是否续做其他批次（K1b 等）；④ AI快捷记一餐真机验证进度仍未核实（见下条） |
| K1b 蓝图现状（不变） | `docs/feature/AI记一餐_K1b膳食健康评价逐成员化_实施蓝图.md`，状态 `DRAFT·PARKED`，等这条主线（含 L1/K1i 的真机验证）彻底收尾后再拾起处置 §10 已挑出的问题，不重新起草 |
| AI快捷记一餐真机验证（不变，仍未核实进度） | `真机待验证清单_202608082330.md` 里 E-B4-*/E-B5-*/E-B6-*/E-K1A-01/E-CFG-01~06 近 30 项进度仍待用户确认——**早前就悬而未决**，连同 E-L1-01~12 / E-K1I-01~02 应一起跟用户核实 |

---

## 历史批次（已关闭，供参考）

### AI记一餐 K1a 营养展示统一化 + AI 未配置诚实报错

| 字段 | 值 |
|---|---|
| 蓝图文件 | `docs/feature/AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md` |
| 规模 / 颗粒度 | BLUEPRINT-FULL / L7（项目基线 · 37 条 GC） |
| 状态 | **ACCEPTED**（ARCH 独立复核通过：diff 走查 + `:shared:testDebugUnitTest` 641/641 绿 + `:androidApp:assembleDebug` 绿 + 全仓 grep 确认 `estimatedKcal` 无死引用，无阻断项） |
| 基线 commit | `dae39fc2` |
| CODE 交付 commit | `5c976a49`（营养统一化全部 STEP + 新增 2 测试文件）；CFG 部分已在更早历史批次实施 |
| ARCH 复核 commit | `226142bf`（本次复核未改代码，仅补台账文档） |
| 末次更新 | 2026-08-08（ARCH@主力机：独立复核通过——diff 走查 + 实跑构建/测试验证，批次关闭，K1a 待办标 ✅。） |

---

### AI记一餐 周期记 NDJSON流式 / B4+B5+B6

| 字段 | 值 |
|---|---|
| 蓝图文件 | `docs/feature/AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md`、`..._B4输入UI实施蓝图.md` |
| 状态 | **ACCEPTED**（ARCH 三次复核通过，AF-B456-01~09 全部 9 项阻断关闭） |
| 基线 commit | `dfac266a` |
| 复核报告 | `docs/context_memory/架构模型复核报告_B4B5B6_2026-08-07.md` §八 |
| 末次更新 | `dfac266a` · 2026-08-07（ARCH@主力机：三次复核通过，收紧 `T-B5-02` 断言为精确匹配并复跑验证。批次关闭。） |

具体模型执行与能力评估：

Canonical Owner：`docs/experience/14_模型执行力评估.md`

BLUEPRINT_STATE 仅维护 ARCH / CODE / REVIEW / TURN 的抽象角色 + 机器标识，不在本文件重复具体模型名称或模型能力评价。
