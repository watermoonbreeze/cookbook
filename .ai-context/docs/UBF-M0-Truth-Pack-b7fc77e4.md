# UBF-M0 Truth Pack

## A. Export Metadata

- Task ID: `UBF-M0-EXPORT-01`
- Generated: 2026-08-12T10:40:52+08:00
- CookBook repository: `C:/Users/SXD-T480S/Documents/WorkSpace/Gitee/cookbook`
- Output: `C:/Users/SXD-T480S/Documents/WorkSpace/Gitee/cookbook/.ai-context/docs/UBF-M0-Truth-Pack-b7fc77e4.md`
- Historical Baseline Candidate: `598daf4e5083d62038adfe39b1635993a7d90fa4`
- Observed Remote Target: `b7fc77e4d442364e6f5db790b374ece4c5da409d`
- Local Current HEAD: `b7fc77e4d442364e6f5db790b374ece4c5da409d`
- Working Tree: `DIRTY`
- Overall collection: `COMPLETE`

## B. Pre-capture Git Evidence

### git rev-parse --show-toplevel

```text
C:/Users/SXD-T480S/Documents/WorkSpace/Gitee/cookbook
```

### git rev-parse HEAD

```text
b7fc77e4d442364e6f5db790b374ece4c5da409d
```

### git status --short --untracked-files=all

```text
 D ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V3-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-1-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-2-End-2E-Handoff.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-1-BlueDesign.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-2-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-3-R2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-4-R3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-5-END-Final-Accept-Phase3-Handoff.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2Z-Final-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3-\346\200\273\350\247\210.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3A-Preview.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3A-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/TruthPack-\346\234\254\345\234\260\346\226\207\346\241\243\345\257\274\345\207\272.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\346\211\247\350\241\214\351\203\250\345\210\206-9.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\347\254\254\344\272\214\351\230\266\346\256\265-2B-End-2C-Preview.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\347\254\254\344\272\214\351\230\266\346\256\265-2C-End-2D-Preview.md"
?? "C\357\200\272UsersSXD-T480SDocumentsWorkSpaceGiteecookbooktempclaudecommit_msg.txt"
?? temp/codex/af13_af14_review_diff.txt
?? temp/codex/generate_truthpack.ps1
?? temp/e.txt
?? temp/err.txt
?? temp/f.txt
?? temp/g.txt
?? temp/r3err.txt
?? temp/review_android_test.txt
?? temp/review_apk_build.txt
?? temp/review_shared_test.txt
?? temp/test_output.txt
```

### git log -5 --oneline --decorate

```text
b7fc77e4 (HEAD -> master, origin/master, origin/HEAD) docs: add universal blueprint architecture review
c87a43f1 docs(blueprint): close governance reviewability gaps
58665238 docs(blueprint): strengthen multidimensional governance review
e0ae8bc3 docs(project-graph): repair phase 3a governance closure
21e54015 docs(project-graph): execute phase 3a view inventory audit
```

### git diff --stat

```text
 .../Phase-2D-R1.md"                                | 1360 --------------------
 1 file changed, 1360 deletions(-)
```

### git diff --name-status

```text
D	".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-R1.md"
```

### git diff --check

```text

```

### git remote -v

```text
origin	https://gitee.com/sxdGit/cookbook.git (fetch)
origin	https://gitee.com/sxdGit/cookbook.git (push)
```

## C. Interposed Commit Evidence

BASELINE OBJECT NOT AVAILABLE LOCALLY

## D. Governance Inventory

| Relative path | Type | In collection |
|---|---|---|
| `C:\Users\SXD-T480S\.ai-context\codex\MODEL_ROUTING.md` | governance material | YES |
| `C:\Users\SXD-T480S\.ai-context\GLOBAL.md` | governance material | YES |
| `C:\Users\SXD-T480S\.ai-context\rules\blueprint_protocol.md` | governance material | YES |
| `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\docs\context_memory\BLUEPRINT_STATE.md` | governance material | YES |
| `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\docs\项目改造规划\Universal-Blueprint-Framework-Architecture-Review.md` | governance material | YES |
| `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\PROJECT.md` | governance material | YES |
| `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\GOV_BP_P3_01_AUDIT.md` | governance material | YES |
| `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE2_TO_PHASE3_HANDOFF.md` | governance material | YES |
| `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE2E_VIEW_DRIFT.md` | governance material | YES |
| `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE3_ARCHITECTURE_ACCEPT.md` | governance material | YES |
| `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE3A_AUDIT.md` | governance material | YES |
| `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE3A_BLUEPRINT.md` | governance material | YES |
| `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\project.yaml` | governance material | YES |

## E. Located Truth Candidates

| Requested | Actual path | Scope | Truth role | Status | SHA-256 | Lines | Notes |
|---|---|---|---|---|---|---:|---|
| `MODEL_ROUTING.md` | `C:\Users\SXD-T480S\.ai-context\codex\MODEL_ROUTING.md` | USER-LEVEL | canonical governance | FOUND | `9F33E674315094A7CC76C678305AF458537D0F7FD6BB66542427780FA9D708F9` | 15 | 鈥?|
| `GLOBAL.md` | `C:\Users\SXD-T480S\.ai-context\GLOBAL.md` | USER-LEVEL | canonical governance | FOUND | `73CF5C049585542B0F82EA216EAB55EE4864399B71BFB6131EFAEEC254E540D0` | 130 | 鈥?|
| `blueprint_protocol.md` | `C:\Users\SXD-T480S\.ai-context\rules\blueprint_protocol.md` | USER-LEVEL | canonical governance | FOUND | `C2C8332EB12D545CA89FCA4C80A15DBA7E2ACF5FAF7703A8CFE6815A0B5F0EB3` | 149 | 鈥?|
| `BLUEPRINT_STATE.md` | `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\docs\context_memory\BLUEPRINT_STATE.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `C93F25059DC7382F3C770E51DAEB6530453673B70A3610927B741C1B1EAD5463` | 90 | 鈥?|
| `Universal-Blueprint-Framework-Architecture-Review.md` | `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\docs\项目改造规划\Universal-Blueprint-Framework-Architecture-Review.md` | PROJECT-LEVEL | generated or review view | FOUND | `B49AF0F29C1A91D84FAB2999C28AE357BD0569728F4D5F17EEA5FE037E1E19EB` | 183 | 鈥?|
| `PROJECT.md` | `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\PROJECT.md` | PROJECT-LEVEL | canonical governance | FOUND | `F00233A7992539AD05521ACEDDE7ECC182DA2F8DB176D9C0C5772DF5F549DF37` | 52 | 鈥?|
| `GOV_BP_P3_01_AUDIT.md` | `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\GOV_BP_P3_01_AUDIT.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `71556F45B3AF38B5CD00C1CAEB980F41F0F69D802577707719E9165677CB5AFD` | 105 | 鈥?|
| `PHASE2_TO_PHASE3_HANDOFF.md` | `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE2_TO_PHASE3_HANDOFF.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `EC20B46284CDE13DDAC089709273BC991815FA2B80F95D4DA7DC2E7F1FC70867` | 53 | 鈥?|
| `PHASE2E_VIEW_DRIFT.md` | `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE2E_VIEW_DRIFT.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `1F6B19171248D221EBCF1E93E8E1A9C138903D2EC50C2A67EBC1955203052AA4` | 18 | 鈥?|
| `PHASE3_ARCHITECTURE_ACCEPT.md` | `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE3_ARCHITECTURE_ACCEPT.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `82ADB84CCE625C7B4CE2C277DDE50E97E5B6B373AEF3B5E42252E3040F0F03CF` | 34 | 鈥?|
| `PHASE3A_AUDIT.md` | `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE3A_AUDIT.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `F0C4A9A2E529C87BF9C252EFB79C78B6ABBB89FC7E5740F70B82B5BBA38CF9F2` | 146 | 鈥?|
| `PHASE3A_BLUEPRINT.md` | `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE3A_BLUEPRINT.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `EE0ACC657FD4E9B0A89D3621C84681AFEBC68DDB1E5DDA68AAA9DF89F8F9CB49` | 192 | 鈥?|
| `project.yaml` | `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\project.yaml` | PROJECT-LEVEL | canonical governance | FOUND | `2C756CE240C129E72276D7A97842C953580C006B768227BB06C086C270CA2F0F` | 58 | 鈥?|

## F. Complete File Contents

### MODEL_ROUTING.md

- Actual absolute path: `C:\Users\SXD-T480S\.ai-context\codex\MODEL_ROUTING.md`
- Scope: `USER-LEVEL`
- SHA-256: `9F33E674315094A7CC76C678305AF458537D0F7FD6BB66542427780FA9D708F9`
- Line count: 15

```markdown
# Codex 模型映射

当前可用最高档按用户约束为 `gpt-5.6-terra`，用推理强度实现三档分工：

| 共享层级 | Codex 设置 | 默认用途 |
|---|---|---|
| 旗舰 / Opus 等价 | `gpt-5.6-terra` + `xhigh` | 方案、架构、安全、复杂质量终审 |
| 主力 / Sonnet 等价 | `gpt-5.6-terra` + `medium` | 日常路由、编码、测试、常规验收、文档结构设计 |
| 资深研究工程师 / 证据支撑 | `gpt-5.6-terra` + `high` | 疑难复现与反证、源码/版本溯源、性能基准、影响地图、隔离 POC、风险预检 |
| 快速 / Haiku 等价 | `gpt-5.6-luna` + `low` | 已确认事实的文档、流程、索引、输出归纳 |

全局主线程默认保持 `model = "gpt-5.6-terra"`、`model_reasoning_effort = "medium"`；常规编码也默认此档。`dev-research-engineer` 是 Terra high 的只读研究支撑角色：只交付可验证的研究结论包，不代替主力实现，也不拥有架构、安全或发布决策权。`apple-doc-designer` 使用 Terra medium，`document-curator` 使用 Luna low。用户要求“开一个子智能体，按合适的模型处理”时，先按 `../MODEL_ROUTING.md` 定级，再使用对应 Agent TOML 委派；角色代理的默认强度由 `scripts/sync-codex-agents.ps1` 生成。

线上事件默认由 Terra medium 主责止血、协调和修复，`dev-research-engineer`（Terra high）并行进行只读排查；命中共享路由中列明的高风险条件时，升级 Terra xhigh 确定处置边界与终审。不得等待研究结束才执行既有、可逆的止血预案。
```

### GLOBAL.md

- Actual absolute path: `C:\Users\SXD-T480S\.ai-context\GLOBAL.md`
- Scope: `USER-LEVEL`
- SHA-256: `73CF5C049585542B0F82EA216EAB55EE4864399B71BFB6131EFAEEC254E540D0`
- Line count: 130

```markdown
# 跨模型通用工作规范

本文件适用于 Claude Code 与 Codex。项目级 `.ai-context/PROJECT.md` 可补充或覆盖其中的技术细节。

## 基本协作

- 默认使用中文交流；生成项目说明、交接和注释时使用项目既有语言。
- 先检查工作区状态，保留用户已有的未提交改动；只改与当前任务有关的文件。
- 编码、修复、重构或调研先按范围、复杂度、数据影响和风险评估任务深度。微任务可直接定位、修改、自检、交付；较大改动先说明方案与影响，再实施。
- 只在用户明确要求时 commit 或 push。提交前检查 `git status` 与 diff；只暂存相关路径，禁止以 `git add .` 混入无关文件。
- 不使用破坏性 Git 操作覆盖用户改动；删除、批量移动或不可逆配置修改前先确认准确目标与影响。

## 模型分工

- 先读取 `~/.ai-context/MODEL_ROUTING.md`。Claude 读取 `claude/MODEL_ROUTING.md`；Codex 读取 `codex/MODEL_ROUTING.md`。
- 默认使用主力档；编码任务默认使用主力档。快速档仅处理有明确证据的文档/流程整理；旗舰档只在共享路由条件满足时介入方案或高风险终审。

## 任务定级与无人值守

- 收到编码/修复/重构/调研任务时，先读当前模型详细编排文件获取流程与角色（Claude:`~/.ai-context/claude/workflow_auto_orchestration.md`；Codex:`~/.ai-context/codex/workflow_execution.md`）。本节是定级规则唯一真相源，详细文件只实现不重复定义定级维度。
- 再按改动范围、逻辑复杂度、数据影响、风险程度取最高级别：微任务、轻量、标准、深度；在分析或修改前说明“级别、交互模式、原因”。用户明确要求直接修改时可跳过。
- 微任务走“定位 → 修改 → 自检 → 交付”，不启动子代理也不等待方案确认。标准/深度任务按 `WORKFLOW.md` 的五阶段执行；Codex 使用 `/plan` 作为方案门禁。
- 用户明确说“无人值守”“自动执行”或同义表达时，才进入无人值守模式：任务深度至少升级一级，优先最小改动、既有模式、少依赖与向后兼容；高风险数据/安全/删除/核心配置操作记录为待确认，不执行。
- 无人值守的本地检查点提交仅在用户已明确授权该模式且项目策略允许时创建；绝不自动 push。

## 标准/深度任务的协作模式

- 三档，严格度递增：**A 常规五阶段 / B 高风险验收合同 / C 冻结蓝图协作**。默认 A；**不得自动进入更严档**。工件为包含关系，C ⊃ B ⊃ A，不存在两套并行文档。
- **A·常规五阶段（默认）**：按 §任务定级取级别，标准/深度按 `WORKFLOW.md` 五阶段执行。标准/深度须建任务卡：目标、非范围、影响模块、风险等级、关键风险、验收方式、协作模式。用户要求直接改时可先做最小安全修改，首个检查点补齐任务卡。**A 档不要求 `BLUEPRINT_READY`。**
- **B·高风险验收合同（命中即自动进入）**：命中 `MODEL_ROUTING.md` §高风险升级条件（唯一真相源）时，按“旗舰出验收合同 → 主力按合同实现 → 主力证据自检 → 旗舰终审”。合同用 `~/.ai-context/templates/深度任务验收合同模板.md`。旗舰只出可执行合同，不与主力重复撰写开放式长方案。
- **C·冻结蓝图协作（仅显式触发，永不自动）**：同时满足两条才进入 —— ① 用户明确说“用蓝图模式/冻结蓝图/BLUEPRINT”或等价表述，**或**项目 `.ai-context/PROJECT.md` 声明 `协作模式: BLUEPRINT`；② 已指名架构模型与编码模型（审查模型缺省 = 架构模型）。
- **任一条不满足，即使命中高风险升级条件也只走 B 档，不得自行升 C。** 高风险是任务属性，蓝图是协作形态，两者不互相蕴含。
- C 档规模分级：同时命中高风险升级条件 → `BLUEPRINT-FULL`（全套蓝图包）；未命中 → `BLUEPRINT-LITE`（任务卡 + 文件 allowlist + 不变量表 + 测试矩阵，四件）。LITE 同样禁止 allowlist 外改动与临场发挥，只减文档体量，不减约束强度。
- C 档规则正文在 `~/.ai-context/rules/blueprint_protocol.md`；跨机器时以项目内 `.ai-context/docs/experience/` 下的项目副本为准（对端可能无用户级目录）。**仅 C 档激活时读取；A/B 档不读、不受其约束。**
- C 档退出：批次达 `ACCEPTED`，或用户说“退出蓝图模式”。退出时置 `TURN=NONE` 并记录结论。**模式不跨任务默认延续。**

## 跨机器蓝图协作

- 适用：架构模型与编码模型不在同一机器或同一会话。传输通道是**项目 Git 仓库**，不是 `ai-share`。蓝图、台账、审查结论一律提交进项目 `.ai-context/`；**禁止放用户级 `~/.ai-context/`，禁止走 ai-share**（那里只同步规则，不同步任务）。
- 位置固定：蓝图 `.ai-context/docs/feature/<功能>_B<n>_实施蓝图.md`；握手状态 `.ai-context/docs/context_memory/BLUEPRINT_STATE.md`（**全项目唯一一份**）；协议副本 `.ai-context/docs/experience/`。**蓝图内只写项目内相对路径，不写 `~/` 开头路径。**
- `BLUEPRINT_STATE.md` 是唯一握手真相源，≤30 行。字段：任务/批次、蓝图文件、规模（FULL/LITE）、状态（`DRAFT / BLUEPRINT_READY / IMPLEMENTING / SELF_CHECKED / REVIEWING / ACCEPTED / BLOCKED`）、`TURN`（`ARCH / CODE / USER / NONE`）、`ARCH=`、`CODE=`、`REVIEW=`、基线 commit、未闭合 Q/AF、末次更新 commit 与时间。
- **开工前先 `git pull` 读 `BLUEPRINT_STATE.md`。`TURN` 不是自己 → 停止，只报告当前持球方，不改任何代码。**完成本方动作后，在同一提交内更新 `TURN`、状态、末次 commit，再 push。审查启动依赖此文件，不依赖人工复述。
- 蓝图首屏必须写 `ROLE_CONTRACT` 头，用**第二人称**写明各角色允许/禁止动作与停机动作。**本机模型标识与 `ARCH=`/`CODE=` 均不匹配时，一律按编码模型的最严约束执行**，不得自认架构角色。
- 编码模型只负责实现、指定测试和事实台账；不得改契约、删回归、弱化断言或扩范围。遇未冻结点：停止改代码 → 在蓝图 `§缺口台账` **追加** `Q-<批次>-NN：定位 / 可见事实 / 缺失决策 / 受阻操作` → 置 `TURN=ARCH` → 提交并结束会话。不得临场发挥。
- 编码模型交付必须给“蓝图 ID → 文件:行范围 → 测试 ID → commit”四列映射表；commit message 带批次与关键 ID，如 `[B4] feat: xxx (INV-B4-03, T-B4-07)`。审查模型据此 + `git log <基线commit>..HEAD` 反查，不自行猜测 diff 归属。
- 审查结论写回**同一蓝图文件** `§审查台账`（轮次 / 结论 / AF 编号 / 状态），不另开文件；超 200 行才拆 `..._台账.md`。结论只能是 `通过 / 阻断 / 缺证据`。阻断项 `AF-<批次>-NN` 须含：违反的 ID、文件定位、证据、唯一最小修复、必须新增或恢复的测试、禁止扩大范围。审查完置 `TURN=CODE` 或 `ACCEPTED`。
- Q/AF 编号沿用现有体系，跨机器不另立。**只在表尾追加，编号只增不复用；关闭项保留原行并标 `CLOSED@<commit>`，不删行** —— 避免两端并发编辑同一区块产生 Git 冲突。

## 自带验收框架的任务（2026-08-05 确立）

**当任务规范/方案中已附带显式验收框架（分批验收矩阵、编号测试用例清单、真机验证步骤等），实施模型必须逐项执行并报告，不可跳过或降级为”大概没问题”。验收框架是任务定义的一部分，不是可选参考。**

- **识别**：任务来源文档中标有「完成门槛」「必须自动化的用例」「真机验收」「分批交付与验收流程」等章节的即为验收框架。
- **逐项验收**：完成实现后，把验收框架每一项拉出，逐条标注状态（✅/⚠️/❌）+ 证据（测试名/构建结果/真机截图），不得以”感觉都覆盖了”跳过。
- **不入下一批**：有未通过项的必须在当前批次修复后重新验收，不得带着失败项进下一批。
- **真机项豁免**：纯 shared/后端/无 UI 批次的真机验证项可标”本批无 UI·待后续批次真机”，但自动化用例仍必须全绿。
- **任务编写者责任**：编写任务规范/方案时，必须在文档中显式列出可逐项核对的验收清单（分批 + 编号用例），并在任务交接时明确告知”请按验收框架逐项验收”，防止实施模型忽略。

## 项目知识与文档

- **项目全景图 / 项目地图**：每个项目应在项目级 `.ai-context/docs/projectReview/00_导读与索引.md`（或项目声明的等价唯一入口）维护可反查的全景说明书。凡需要理解项目整体架构、产品方向、主流程、界面流转、算法、数据、AI/网络、参考资料或预设来源时，先读该入口，再按其阅读路径下钻；不得只凭当前局部代码推断全局。
- **项目地图同步维护**：新增、删除或实质修改跨模块架构、主流程、页面/路由、领域真相源、数据模型/来源、AI/网络策略、预设/参考资料或公共复用能力时，同一任务必须更新受影响的项目说明书分册、诊断地图和功能路径索引；项目入口文件须声明本项目的地图位置与首读要求。
- 用户提出功能时，先查看项目 `.ai-context/docs/功能路径索引.md`（如存在）和 `docs/flows/INDEX.md`，再按需探索源码；发现新模块或真相源后同步更新索引。
- 项目文档、经验、速查表和会话交接统一保存在 `.ai-context/docs/`。`.claude/docs` 是兼容联接，不是新的写入位置。
- **规则/配置/AI 内部消费类 md 一律 AI 精简体**（GLOBAL、CLAUDE.md/AGENTS.md、workflow、MODEL_ROUTING、agents、skills、交接、速查表、context_memory、方案）：只要模型看懂即可，不为人类阅读优化——去铺垫/客套/重复，用短句/片段/表格，保留指令、路径、条件、映射、约束。**面向用户查看的文档（README、使用说明、迁移指南）保持人类可读，不适用**。
- 功能路径索引保持 AI-terse：声明路径缩写，按模块列“功能/别名：路径 + 极简职责”，单列关键真相源；新增、删除、重命名或移动关键页面、ViewModel、Repository、UseCase、Engine、领域模型或可复用组件时同步更新。
- 每完成重要节点，将结论、关键路径、决策及待办写入相关文档。会话交接的唯一入口为 `.ai-context/docs/context_memory/SESSION_交接.md`；主文件只保存最新状态，历史只追加到 `SESSION_交接_历史.md`。
- **（Claude 专属·可选）** 已装 `server-memory` MCP。摸清项目架构后，可把架构摘要（模块划分/关键路径/技术栈/核心约束）写入 server-memory 作跨会话快速缓存；新会话涉及该项目先查 memory 再读码，减少重复分析。**文件版 `context_memory/*.md` 仍为主**（可读·可移植·双模型通用）；server-memory 仅 Claude 侧补充，不强制、不替代、不过度维护。Codex 无 MCP，忽略此条。
- 一个会话聚焦一类工作。与当前主题无关的需求先登记到待办，除非用户明确要求立即处理或它确实依赖当前上下文。
- 人工/联网/真机验证单列“待测试验证”文档，按前置条件、操作、预期、失败判定、日志采集编排，与验收用例 ID 互相引用。
- **真机验证清单唯一文件**：项目 `.ai-context/docs/feature/` 只保留 `真机待验证清单_<yyyyMMddHHmm>.md` 一份；新增验证项时更新原文件并改名为当次时间戳，禁止复制/新建第二份；交付时报告最新文件名。

## 通用知识库与 Android / Maven 口径

- 跨项目知识库的唯一位置是 `~/.ai-context/knowledge/`；`.claude/memory` 是兼容联接。遇到 Android 权限、存储、Manifest、兼容性或构建问题，先读 `knowledge/MEMORY.md` 与相关 `android_*.md`，再修改代码。
- 涉及公司内部 Maven 依赖（`com.gagcx.*` / `com.gagc.*`）时，先读 `knowledge/gc_maven_libs.md`，定位 `D:\Company\Workspace\Project\GC_MAVEN` 中对应源码并阅读源码；不得只凭 jar、POM 或猜测 API。新发现的内部库同步回填该索引。
- Android 存储权限遵循版本分段：≤28 使用 READ/WRITE；29 结合 legacy storage 与 WRITE 上限；30+ 按普通文件、媒体文件分别使用匹配的现代方案。权限异常先检查合并后的 Manifest，尤其关注依赖库 `maxSdkVersion` 对集成方声明的污染。
- AUTO_DEV 自动化开发/测试**默认不启用**（驱动费大量 token），需用户显式开启。启用后：Android 项目声明 `AUTO_DEV` 且 `AUTO_TEST_HOME` 有效时按 L0–L6 分层验证，无效或未接入退回构建与已有测试并说明跳过原因。

## 质量与实现

- 代码改动应有与项目风格一致、能说明“为什么”的注释；遵循项目级 AI 标识要求。
- 输出精简省 token：不复述大段日志/命令输出，只留错误与关键行；减少铺垫客套；合并连续微小改动减少工具轮次；读大文件优先签名/摘要再按需读全文，禁止无差别递归整目录。
- 长时间或批量生成任务采用确定性分批、每批立即落盘和可续跑机制；优先使用具备幂等或续跑能力的现成工具。交付前检查完整性、去重和缺失项。
- 涉及内部 Maven 依赖时优先查找本机 Maven 缓存或可用源码，而非猜测 API；项目级文档可给出具体路径。
- 改动完成后执行与风险相称的构建、测试或静态检查；如因环境限制无法验证，明确说明未验证项和原因。
- AI 新建或实质修改代码时，按项目风格写清楚“为什么”的注释，并使用 `[AI生成]` / `[AI修改]` 追溯标识；新建 Java 类可采用 `@File`、`@Time`、`@Author: <作者>-AI`、`@Desc` 的类注释。注释语言跟随项目语言。
- 互斥主状态用单一 sealed/enum 字段表达，不用多个布尔拼出未定义组合。并发会话必须有 generation/会话隔离，旧会话事件不得改写新会话；取消不得伪装为失败或完成。
- 测试夹具只制造外部原因，不直接返回生产层期望结果。异步测试用 latch/channel/受控 dispatcher 表明到达阶段，禁止 `sleep`、真实公网、随机端口和依赖调度偶然性。重试、取消、归属、mapper 测试须断言调用数、事件顺序、终态次数和关键键/日期/归属，而非只断言非空或类型存在。
- 证据必须来自当前 commit：不得以“代码已写、测试以后补”、历史 XML 或缓存结果标记完成。删除或替换既有回归须有明确批准及一对一替代映射。

## 长任务恢复与 Git 纪律

- 对多轮、批量、联网或长时间的写入型任务，优先使用具备幂等/续跑能力的现成工具；否则按确定性编号分片，每批（建议 10–20 项）立即落盘。
- 流式数据优先 JSONL 追加；定长结果使用独立完整的编号文件。重启前先扫描已完成记录，只补缺失批次；汇总前核对数量、引用和去重，不把半成品当完成品。
- 每个可验证节点应形成清晰 Git 边界，但常规任务不自动提交。提交信息说明“改了什么 + 为什么”；不 amend 已有提交，不使用 `--no-verify`，不以 `reset --hard`、`push --force` 或 `checkout --` 覆盖用户改动。

## 共享资产

- `agents/` 是通用角色定义；可按任务需要参考，不绑定单一模型的调度工具。用户明确说“开一个子智能体，按合适的模型处理”时，主线程按 `MODEL_ROUTING.md` 定级后委派；默认只分派独立的分析、审查、测试或具备明确文件边界的实现工作，避免冲突写入。
- `skills/` 是可复用的工作流程。Claude Code 通过 `~/.claude/skills` 联接访问；Codex 通过 `~/.codex/skills/<skill>` 联接访问。
- `claude/` 下的斜杠命令、模板与 Claude 编排原文仅作 Claude Code 兼容资产；其中可迁移的原则应同步提炼到本文件。

## 共享运行资料

- 用户级 `.ai-context/shared-runtime/` 与项目级 `.ai-context/shared-runtime/` 仅保存可移植、非敏感且格式中立的运行资料；进入目录前先读各自 `README.md`。
- 可放入跨模型交接摘要、可再生成缓存、非敏感导出、插件源码/清单和不含密钥的配置样例；项目相关资料优先放项目级目录。
- 认证、token、cookie、API Key、私钥、证书、真实环境文件、原始会话、SQLite、历史、遥测、工具缓存、插件安装状态和锁文件必须保留在模型私有目录或获准的密钥管理系统，不得迁入共享运行资料。

## 模型切换增量同步

- 普通任务不扫描另一模型的角色、工作流或配置，避免启动成本和 token 浪费。
- 用户说“模型切换”，或明确说“已切换到 Codex/Claude，请同步增量”时，执行 `sync-ai-context`：根据当前模型的 `.ai-context-sync.md` 自动读取尚未应用的 `VERSION.md` / `CHANGELOG.md` 条目，无需说明来源模型。
- 新增或修改共享资产（Skill、角色、工作流、Prompt、Hook 或共享规则）时，维护方必须在同一改动中更新注册表、版本和日志，并在交付前核对变更资产路径已登记到当前版本条目；不得等待模型切换时再补记。另一端下次收到切换口令再按增量适配。

## 会话交接与继续

- 会话交接目录按顺序解析：优先项目 `.ai-context/docs/context_memory/`；尚未迁移的旧 Claude 项目则回退 `.claude/docs/context_memory/`。用户说“会话继续”“查看 session 继续”“读 session”或“接续 session”时，只读该目录的 `SESSION_交接.md`，再按其中“先读清单”和“下一步”继续；两处都不存在时明确说明暂无交接记录。
- 用户说“会话交接”“保存 session”或“切换 session”时，**保存上下文 + 总结经验一并做，不用分别说两次**：① 将当前结论写入已解析目录的相关文档，再覆盖更新该 `SESSION_交接.md`（历史仅追加到同目录 `SESSION_交接_历史.md`）；② 同时按 `zongjie` Skill 逻辑复盘本次任务，提炼可复用经验/红线归入项目经验分册。
- 这是项目资料读取规则，不属于双模型同步；普通“会话继续”不读取另一模型的版本日志。

## 快捷语义映射

- “总结经验 / 反思” → `zongjie` Skill（项目内经验分册，单项目范围）。
- “记到技术库 / 加进 AI-Dev-Insights / 记录到ADI中 / 加进ADI / 这个存成知识点 / 这个入库 / 沉淀一下这个坑” → `insight-add` Skill（跨项目/跨公司可复用技术经验，知识点为主、踩坑为辅，与 zongjie 按”技术库 vs 项目经验”分工，判据见 `AI-Dev-Insights/建设方案.md` §四）。
- “配置迁移 / 导出给 Codex” → `config-export` Skill；以当前双模型配置为准，旧文档中与 Codex Hook/MCP 支持冲突的结论不再沿用。
- “初始化项目 / myinit” → `myinit` Skill。
- “扫描速查表” → `quickref-scan` Skill；”跑自动化测试” → `autotest` Skill。
- “同步 ai-share / 同步aiShare / aiShare同步 / sync aiShare” → `sync-ai-share` Skill：将 `.ai-context` 增量同步到 ai-share Git 仓库并 push。Codex 通过 Bash 执行同步脚本和 git 命令。
```

### blueprint_protocol.md

- Actual absolute path: `C:\Users\SXD-T480S\.ai-context\rules\blueprint_protocol.md`
- Scope: `USER-LEVEL`
- SHA-256: `C2C8332EB12D545CA89FCA4C80A15DBA7E2ACF5FAF7703A8CFE6815A0B5F0EB3`
- Line count: 149

```markdown
# blueprint_protocol — C 档冻结蓝图协作细则

> 定位：`GLOBAL.md` §标准/深度任务的协作模式 中 **C 档**的完整规则。**仅 C 档被显式触发时读取生效**（用户明确说"用蓝图模式"，或项目 `PROJECT.md` 声明 `协作模式: BLUEPRINT`）。A/B 档不读、不受本文件约束。
> 跨机器操作机制（`BLUEPRINT_STATE.md`、`ROLE_CONTRACT`、TURN 握手）见 `GLOBAL.md` §跨机器蓝图协作，本文件不重复。

## 1. 核心定义

- **实施蓝图**：可直接交给编码模型的封闭执行合同，不是解释性方案。必须使两名不同编码模型得到同一组类型、边界、状态转移、测试和提交范围。
- **决策闭合**：每一处会影响行为、数据归属、并发、异常、兼容、公共 API 或测试证据的选择都已给出唯一答案。
- **机械实现**：编码模型只在指定文件中创建/替换指定类型、按指定顺序连接数据、填充指定测试夹具和断言；不选择架构、策略或替代方案。
- **蓝图缺口**：编码模型无法从蓝图确定唯一行为。缺口不是自行发挥的许可，是阻断项。

**硬原则**：未被蓝图显式授权的新入口、新异步任务、新全局状态、新 fallback、新公开 API、新依赖和新持久化写入，一律禁止。fallback 必须先转换为主路径内部类型，再复用主路径校验，禁止另造归属或日期规则。

### 语义类别

任何涉及“冻结 / 状态 / 迁移 / 快照 / 生成”的蓝图，必须先区分以下五类对象：

1. **Stable Identity**：实体是谁。
2. **Contract / Semantic**：规则、含义、状态机或约束是什么。
3. **Lifecycle State**：实体当前处于哪个生命周期状态。
4. **Acceptance Snapshot**：某次 ACCEPT / FINAL ACCEPT 时刻看到的派生状态。
5. **Generated View**：由 Truth 派生出的展示或交接视图。

Stable Identity 冻结 ≠ Lifecycle State 冻结；Contract 冻结 ≠ Current Value 冻结；Acceptance Snapshot ≠ 永久 Truth；Generated View ≠ 独立 Truth。

### Frozen 表述红线

禁止范围不明确的冻结表达，例如 `WorkItems frozen`、`statuses frozen`、`state frozen` 或 `project state frozen`。凡使用 `FROZEN` / `FINAL` / `IMMUTABLE`，必须明确冻结的是哪一语义类别。示例：`Stable Identity: FROZEN`、`Status Contract: FROZEN`、`Current Status Value: LIFECYCLE-MUTABLE`。

FINAL ACCEPT 中出现的当前数量、状态和值，若仅用于记录接受时点，必须显式标记为 `Acceptance Snapshot` 或 `Derived Acceptance Snapshot`。

### Mutation Declaration

涉及 Architecture、Migration、REWORK、Freeze、ACCEPT、FINAL ACCEPT 或 Handoff 的蓝图，必须显式声明本批对 Stable Identity、Contract / Semantic、Lifecycle State、Acceptance Snapshot、Generated View 五类的可变性；每类标记 `MUTABLE`、`IMMUTABLE`、`CONDITIONALLY MUTABLE` 或 `N/A + 理由`。该声明只是 GC 可验证表达形式，不是新的颗粒度轴。

## 2. 蓝图包（规模 × 颗粒度，两轴正交）

- **BLUEPRINT-FULL**（命中 `MODEL_ROUTING.md` §高风险升级条件时要求）：任务卡与现状事实地图、不变量表、类型/API 表面与可见性、数据流及每个关键字段的唯一真相源、状态机/并发时序/generation 取消规则、文件 allowlist 与每文件操作、测试矩阵与当前证据台账。任一项缺失不得标记 `BLUEPRINT_READY`。
- **BLUEPRINT-LITE**（未命中高风险升级条件）：任务卡、文件 allowlist、不变量表、测试矩阵，四件。约束强度不降，只减文档体量。
- 每条蓝图规则须采用 `ID / Owner / While / When / Input / Do / Must not / Evidence` 表达。不得使用"适当处理""必要时""尽量""合理兜底"等未给唯一行为的措辞。
- **`While`（前置状态）字段（2026-08-07 借鉴 Kiro/EARS 记法新增）**：同一个 `Do` 若存在多个互斥前置状态（如"已开始/未开始"“已终态/进行中”），每个前置状态必须各开一行 `While`，禁止只写最常见的一种、让其余前置状态隐式落入默认分支——这正是"承载类型值域覆盖不了真实状态空间"这类缺口（见 Cookbook 项目 `BL-12`）的直接对策。判定式：`该 Do 的 While 集合基数 ≥ 该字段/状态真实可区分取值数`，否则视为蓝图缺口。
- **规模轴回答"要写哪些工件"，不回答"每个工件写到多细"——后者是下面的颗粒度轴，二者正交。LITE 不得靠减工件降低颗粒度。**

### 2.1 颗粒度轴（`L1~LN`，数字越大越精细）

- 判定式（可判定，不主观）：对蓝图 `D` 与级别 `Lk`（其条款集合 `S_k`）：
  `覆盖率(D, Lk) = |D 中显式满足的 GC ∈ S_k| / |S_k − N/A 项|`
  `D 可声明 GRANULARITY = Lk ⟺ 覆盖率(D, Lk) = 100% 且 S_1..S_k 全部满足`
- **GC（Granularity Clause）书写红线**：每条 GC 必须是**存在性命题**（"蓝图中存在 XX 表 / XX 字段 / XX 字面量，或该项被标 N/A+理由"），禁止程度命题（"写清楚""考虑周全"）。写不成存在性命题的想法不许进 GC 表。
- 级别单调累积：`S_1 ⊂ S_2 ⊂ … ⊂ S_N`。
- **级别语义**（跨项目通用命名，条款内容各项目自行积累，**属项目专属事实，登记在项目 `experience/` 对应分册，不进本文件**）：

| 级别 | 主题 |
|---|---|
| L1 | 决策与范围闭合 |
| L2 | 证据闭合 |
| L3 | 真相源闭合 |
| L4 | 所有权与生命周期闭合 |
| L5 | 索引空间与集合投影闭合 |
| L6 | 用户可见副作用闭合 |
| L7 | 脚本可勾销闭合 |

项目可在 L7 之后继续开新级，级别语义（主题名）可反哺进本表。

### 2.2 GRANULARITY 声明规范

三处声明，缺一不判 `BLUEPRINT_READY`：
1. **批次级**：`BLUEPRINT_STATE.md` 表格新增 `颗粒度` 行。
2. **蓝图文件级**：文件头 front-matter 加一行 `> 颗粒度：Lk · 勾销表见 §0.1`。
3. **蓝图内勾销表**：挂在既有"编码前门禁"章节下新增 `§0.1 颗粒度勾销表`，逐条 GC 给出`落点`与状态（`满足` / `N/A：<理由>` / `未满足`）；任一条`未满足`不得标 `BLUEPRINT_READY`。
4. **项目基线级不得下调**；不适用的 GC 标 `N/A+理由`（一行成本），不是省略。纯文档/纯配置/零业务代码批次可整体声明降级（须在 `BLUEPRINT_STATE.md` 颗粒度行注明降级理由）。跨项目复用时新项目从 `L3` 起步（L1~L3 是与技术栈无关的通用地板）。

## 3. 编码模型职责与停机

- 只负责实现、指定测试和事实台账；不得改变契约、删减回归、弱化断言或扩展范围。
- 遇到唯一行为无法确定时，必须停止并记录 `Q-<批次>-NN：定位 / 可见事实 / 缺失决策 / 受阻操作`，等待架构模型闭合；不得临场发挥。
- 每批同时完成对应代码、测试与当前 commit 的证据台账；不得以"代码已写、测试以后补"、历史 XML 或缓存结果标记完成。删除/替换既有回归必须有架构批准及一对一替代映射。

## 4. 审查与阻断

- 审查模型只按蓝图逐项反查，结论只能为"通过 / 阻断 / 缺证据"。
- 阻断项使用 `AF-<批次>-NN`，须写明违反的 ID、文件定位、证据、唯一最小修复、必须新增/恢复的测试和禁止扩大范围。
- 每轮审查把问题归入"决策未冻结、抽象所有权、真相源、异步时序、测试因果、回归/证据、范围漂移、索引空间隐性耦合"或新增类别，同时按下列三分支之一处置（详见 §2.1 颗粒度机制）：
  1. **复发**：能被现有 GC 条款覆盖（蓝图本该按此写但没写，或写了但未执行）→ 该 GC 复发计数 +1，不新增级别。复发计数 1 时该 GC 升为"审查必查项"；复发计数 ≥2 时升为"自动检查项"（进 lint 脚本或明确 grep 判据）。
  2. **扩容**：无现有 GC 覆盖，但主题可归入某个现有级别（同类表达形式即可防住）→ 该级别新增一条 GC，级别号不变。
  3. **开新级**：无现有 GC 覆盖，且需要蓝图新增一种此前不存在的表达形式才能防住 → 开新级 `L{N+1}`，成为新基线。
- 每次复核收尾必须执行此判定并记入复核报告固定小节；未执行不得转 TURN。仅把可复用根因（级别语义、GC 的抽象表达）写回共享规则，功能专属事实（具体 GC 条款、触发案例）留在项目文档。
- 已验证的高价值通用检查项（源自 Cookbook 项目复核，抽象后收录，具体案例见项目文档）：
  - 蓝图包任务卡必须含"上一批延后项归宿"：每项延后项须显式转为本批 ID 或标注"显式弃置+理由"，不得只留指针。
  - 新增与既有字段语义重叠的 state 字段，先 grep 旧字段全部写入点：只迁移读取方、不迁移全部写入方即构成"并存无同步保证"，直接判真相源类阻断；若项目内已存在"编辑即失效"一类的唯一收口函数，新增/修改任何编辑类入口方法时必须显式核对是否路由过它。
  - **蓝图冻结前须存在一份独立挑战台账**（2026-08-07 评估主流 spec-driven 框架后新增，源自 Cookbook 项目复核事故：架构模型一次复核未发现自己写的规格本身有值域覆盖空隙，二次复核才发现——因为一次复核与设计出自同一角色，缺乏对规格本身的对抗性视角）：台账须含“挑战方标识 / 挑战项清单 / 每项裁决（采纳→改蓝图 / 驳回→写理由）”，缺失即视为未冻结。挑战方不要求是独立 agent（单人开发场景下可以是设计者换一次空白会话、只给蓝图本身不给设计理由地重新审视），**但产出物的存在性必须可 grep 核实**——这条只强制“挑战发生过且留痕”，不规定谁来挑战，避免把机制绑死在特定工具形态上。
  - **独立挑战必须从 canonical requirement 生成检查集**：挑战方同时读取 canonical protocol、项目 canonical GC registry 与 Blueprint，形成 `Canonical Requirement → Blueprint Location → Presence/Semantic Result → Evidence Method` coverage audit；不得仅依据 Blueprint 自带 checklist 自证 PASS。任一 mandatory contract 缺失或语义不成立，Challenge 不得 PASS。

### Blueprint Improvement Review（治理缺陷反哺闭环）

每个 Verified Defect、AF、REWORK blocker、Independent Review blocker 或 Cross-validation failure，除 Exact Repair 外，必须存在一份 Blueprint Improvement Review。固定字段为：Error Attribution、Blueprint Escape Analysis、Granularity Review、Scale Review、Horizontal Coverage Review、Vertical Coverage Review、Evidence Independence Review、Truth / Ownership Review、Automation Review、Temporal / Propagation Review、Over-design Review、Blueprint Architecture Action、Future Blueprint Propagation、GC Registry Action、Evidence。

Blueprint Escape Analysis 必须从以下枚举中选一项并给出证据：`REQUIREMENT_MISSING`、`REQUIREMENT_AMBIGUOUS`、`REQUIREMENT_TOO_COARSE`、`SCOPE_TOO_NARROW`、`EVIDENCE_MISSING`、`EVIDENCE_NOT_INDEPENDENT`、`GATE_NOT_MECHANICAL`、`STEP_NOT_CLOSABLE`、`OWNERSHIP_AMBIGUOUS`、`SELF_APPLICATION_MISSING`、`EXECUTION_DEVIATION_DESPITE_SUFFICIENT_BLUEPRINT`、`OTHER + exact evidence`。即使归因于执行偏离，也必须完成 Improvement Review。

Review 必须逐项记录 `CHANGE`、`NO_CHANGE + reason` 或 `N/A + reason`，覆盖粒度（Requirement/STEP/INV/Test/Ownership/Gate/Evidence）、规模、横向同类对象、纵向生命周期、证据独立性、Truth/Ownership、自动化、时间传播、后续 Blueprint 传播和过度设计。该表即 **Multi-Dimension Review / Cross-Dimension Review**。规模按 `S0 Defect Object → S1 Sibling Objects → S2 Semantic Category → S3 Current Batch → S4 Cross-Phase → S5 Project-Wide → S6 Global Protocol` 评估；每次升级必须有 same root cause、same semantic contract、same data/ownership pattern、same blueprint template pattern 或既有同类失败证据，不能仅因“可能有问题”扩大。

### Governance Batch Identity and Evidence Landing

治理批次必须分别记录 `Design Baseline`、`Execution Parent`、`Initial Delivery`、`Review Target`。allowlist diff 只能相对本批 `Execution Parent` 判定，不能用 Design Baseline 代替；若实际 HEAD 与蓝图声明的 Execution Parent 不一致，必须先解释 interposed commit 或 STOP。验收中的 `PASS`、coverage、parity、count、matrix、audit 等结果必须可由 Review Target 重跑，或以结构化证据落入本批审计工件；不得只写进 `BLUEPRINT_STATE` 的摘要字段。

涉及用户级 canonical file 的 mutation，审计工件必须记录：Path、Truth Role、Execution Machine、Reviewer Direct Access、SHA-256 Before/After、Mutation Method、Changed Semantic Clauses、Project Fallback Mapping、Local Parity Command、Persisted Evidence Landing、Next-Batch Continuity Gate。审查者不能直接读取该文件时，Reviewer Direct Access 必须标为 `NO`，结论只能称为 `REMOTE_ATTESTED_EXTERNAL_STATE`，不得伪称 `DIRECTLY_REVIEWED_EXTERNAL_STATE`；下一批必须重新计算 SHA-256 与本批 accepted SHA-after 比对，不一致即 STOP。

### Canonical Sibling Entry Scan

治理规则发生 mutation 后，Self-Application 必须扫描同一 semantic keyword、workflow entry、gate、template 和 lifecycle closure 的 sibling entry，并把结果落在审计工件六列表中。当前批次、canonical stable entry/state/registry、直接约束的 Blueprint 与明确受影响的 future Blueprint/template 均须有结果；denylist future Blueprint 只记录传播缺口，不直接修改。

### Cross-Validation Contract（CV-1~CV-4）

交叉验证固定区分：`CV-1 Evidence × Evidence`（识别共同错误来源）、`CV-2 Requirement → Evidence`、`CV-3 Evidence → Requirement`、`CV-4 Object-level + Category/Scale-level`。任一方向不一致时，必须依据 Truth Hierarchy、Authoritative Source 和 Evidence Independence 判定；无法唯一判定即 BLOCKER/STOP。

Blueprint Architecture Action（Architecture Action Enum）只能取 `NONE`、`STRENGTHEN_EXISTING_RULE`、`STRENGTHEN_BLUEPRINT_TEMPLATE`、`ADD_PROGRAMMATIC_GATE`、`EXPAND_AUDIT_SCOPE`、`PROPAGATE_TO_FUTURE_BLUEPRINTS`、`GC_EXPANSION_CANDIDATE`，可多选。所有扩大粒度/范围的动作都必须通过 Over-design Gate（Over-design Check），确认不制造第二 Truth、重复 Registry、无意义 N/A、不可独立取得的证据或无法提高可判定性的 checklist。改进结果必须登记对 Current REWORK、Sibling batches、Remaining current Phase、Future Phases、Project template、Global protocol 的传播结论；Self-Application 只登记 denylist future Blueprint 的缺口，不得越权修改。

### REWORK 最小闭环

每个 REWORK 蓝图必须存在：Verified Defect、Root Cause、Reopen Set、Preserve Set、Exact Repair、Regression Audit、STOP Gate。Reopen Set 是因本次已验证缺陷而重新允许判断/修改的对象集合；Preserve Set 是上一轮已接受且本次缺陷没有推翻的对象集合。REWORK 默认必须比原批范围更窄；除非证据证明核心 Contract 本身失效，不得因局部缺陷重新打开整个 Phase。

### 错误归因

每个 AF / REWORK 收尾必须判断问题属于 `Execution Error` 或 `Blueprint Defect`。Execution Error 是蓝图已给出唯一、可判定、无歧义的行为但执行方未遵守；Blueprint Defect 是两个合格执行模型严格遵守当前蓝图仍可能合理地产生不同实现、不同解释或错误结果。属于 Blueprint Defect 时必须执行现有复发 / 扩容 / 开新级判定，不得全部归因于执行模型。

### Ownership Evidence 红线

影响范围证据 ≠ 责任所有权证据。“某文件 / 模块 / 路径被某功能影响”不能单独证明某项工作责任属于该功能/任务。Ownership 必须由 formal responsibility、accepted specification、original task definition 或 implementation responsibility evidence 之一形成可追溯责任链；代码路径、模块路径、关键词相似度只能作为辅助证据，不得作为 sole ownership evidence。

### Evidence Resolution 中线

Source 未直接写 Stable ID 不得自动判定为 BLOCKED；关键词、文件或技术名词相似也不得自动建立 ownership。正确路径是 `Source Semantic + 至少一个 authoritative responsibility evidence → 唯一责任链`。责任证据可来自 formal responsibility、accepted specification、original task definition 或 implementation responsibility evidence。已实施工作中的 implementation evidence 是高价值 ownership / disambiguation evidence；尚未实施工作不得以 implementation evidence 作为强制前置条件。仍存在多个合理 owner 时必须 blocker / Q。

### Canonical Registry Discovery

在创建 governance specification、registry、state ledger、experience registry、migration truth ledger 或 canonical index 前，蓝图必须存在 Canonical Registry Discovery 结果，至少列出 `Candidate Path / Existing Role / Existing Authority / Overlap / Final Disposition`。Final Disposition 只能是 `REUSE_EXISTING`、`MERGE_INTO_EXISTING`、`EXPLICIT_REPLACEMENT` 或 `CREATE_NEW_NO_OVERLAP`。已有等职责 canonical registry 时禁止平行创建，除非同时声明 replacement、deprecation、migration 和 pointer update。

### Stable Entry / Pointer Rule

稳定入口文档不得复制短生命周期状态，除非该文档本身就是该状态的唯一 Truth Source。TURN、current batch、current implementation model、temporary blocker、current WorkItem status 和 current validation progress 等状态，在稳定入口中只能以 pointer + truth role 表达，不得复制当前值。

FINAL / FROZEN 文档必须区分 Stable Identity、Contract、Lifecycle State、Acceptance Snapshot、Generated View。出现 `frozen status`、`frozen state`、`frozen count` 等 broad wording 时，必须证明当前值本身确需永久不可变化，否则判为冻结语义缺口。生命周期状态机冻结时，当前实体状态默认仍可依据冻结状态机合法迁移。

## 5. 与 A/B 档的关系

- C 档工件是 A/B 档工件的超集（任务卡、验收合同均为蓝图子集），不另建平行文档。
- 状态机声明、并发 generation 隔离、测试夹具禁 sleep、证据须来自当前 commit 这几条属通用工程底线，已同步进 `GLOBAL.md` §质量与实现，对 A/B/C 三档同等生效，不是 C 档专属。
```

### BLUEPRINT_STATE.md

- Actual absolute path: `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\docs\context_memory\BLUEPRINT_STATE.md`
- Scope: `PROJECT-LEVEL`
- SHA-256: `C93F25059DC7382F3C770E51DAEB6530453673B70A3610927B741C1B1EAD5463`
- Line count: 90

```markdown
# BLUEPRINT_STATE

唯一握手状态文件。开工前先 `git pull` 读本文件；`TURN` 不是自己 → 停手，只报告当前持球方，不改代码。开工前除看 `TURN`，还须看 `颗粒度` 行确认本批蓝图应达到的级别。完成本方动作后在同一提交内更新本文件再 push。

**ARCH/CODE 命名规则**：`ARCH`/`CODE`/`REVIEW`/`TURN` 这几个协议字段只写角色名+机器标识（如 `架构师@主力机`、`Coder@副机`），**禁止出现具体模型名称**（Claude/DeepSeek/GPT 等）——角色定义是抽象的，具体由哪个模型担任取决于当前会话，协议逻辑不依赖模型身份。

**模型执行力评估台账（2026-08-07 新增，与上条不冲突）**：独立文档 `docs/experience/14_模型执行力评估.md`，与本文件的抽象角色字段完全分离。CODE 完成本批交付时，去该文档追加一行记录（含实际模型名）；ARCH 复核后补简评。**本文件不重复该表**，避免同一数据两处维护。

---

## 当前批次：GOV-BP-P3-01 Blueprint Governance Upgrade（2026-08-11）

| 字段 | 值 |
|---|---|
| 任务/批次 | 升级用户级与 CookBook 项目级 Blueprint Governance；强化 GC-37/47/48 的独立挑战、改进反哺、粒度/规模/交叉验证/传播与自应用契约。 |
| 颗粒度 | L7（不新增 L8；GC=48） |
| 状态 | **EXECUTED / PENDING INDEPENDENT ARCH REVIEW**；R1 审计工件已落盘；Phase 3A 仍 EXECUTED / REWORK REQUIRED / PAUSED，不得启动 Phase 3B。 |
| TURN | REVIEW |
| CODE | Coder@当前机 |
| ARCH | 架构师@主力机 |
| 基线 | 21e54015ec5ce0fb02d0f47911a6442400a8c44b |
| 基线 | Design Baseline=`21e54015ec5ce0fb02d0f47911a6442400a8c44b`；Execution Parent=`586652388cde269b614728d8160e7963bd88452c`；Initial Delivery=`586652388cde269b614728d8160e7963bd88452c`；Review Target=本批最终 commit。 |
| 证据 | 详见 `.ai-context/project_graph/migration/GOV_BP_P3_01_AUDIT.md`：global/project semantic parity=10/10；canonical GC registry=48/48 unique，missing=0，duplicate=0，GC-49=0；L7 unchanged、L8=0；validator=61/61；pg check=OK（13/109/4/98/10，mode=draft）；denylist=0 diff；user protocol SHA-256 before=C4F3A116265DE97B105CE988AA65B50957C80FA2661B4811EE752F53D46537F5 after=C2C8332EB12D545CA89FCA4C80A15DBA7E2ACF5FAF7703A8CFE6815A0B5F0EB3；INV↔Evidence 双向审计=PASS。 |
| 下一步 | 独立 ARCH 读取本批精确提交，按升级后的 GC-37/47/48 及本蓝图 Delivery Gate 判定 ACCEPT 或 REWORK；本 CODE 批到此 STOP。 |

---

## 上一批次：Phase 3A Baseline + View Inventory / Classification（2026-08-11）

| 字段 | 值 |
|---|---|
| 任务/批次 | 用户指示按 `Phase-3A-Preview.md` 继续执行 Phase 3A CODE；本批完成 baseline + view inventory/classification。 |
| 状态 | **EXECUTED / PENDING INDEPENDENT ARCH REVIEW**；不得自称 Phase 3A ACCEPT，不得启动 Phase 3B。 |
| **TURN** | **REVIEW** |
| CODE | `Coder@当前机` |
| ARCH | `架构师@主力机` |
| 基线 | `598daf4e5083d62038adfe39b1635993a7d90fa4` |
| 证据 | validator 61/61；`pg check` OK；counts 13/109/4/98/10；duplicate=0；dangling=0；Phase2E 9/9 classified。 |
| 下一步 | 独立 ARCH 读取精确提交并按 allowlist / audit / command evidence 判定 ACCEPT 或 REWORK；本 CODE 批到此 STOP。 |

---

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
```

### Universal-Blueprint-Framework-Architecture-Review.md

- Actual absolute path: `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\docs\项目改造规划\Universal-Blueprint-Framework-Architecture-Review.md`
- Scope: `PROJECT-LEVEL`
- SHA-256: `B49AF0F29C1A91D84FAB2999C28AE357BD0569728F4D5F17EEA5FE037E1E19EB`
- Line count: 183

```markdown
# Universal Blueprint Framework Architecture Review

## 0. 评估范围与结论

本评估以 `执行部分-9.md` 的第一性目标为输入，审查 Blueprint、Granularity、coder 能力画像、项目 Overlay 与现有协议的关系。

本批只做理论和架构评估：

- 不修改用户级 `blueprint_protocol`；
- 不新增 GC；
- 不建立独立的 Granularity 评分体系；
- 不预设必须扩展到 L8/L10；
- 不迁移或删除现有 CookBook L7、GC-01~GC-48。

结论：现有体系的主要方向正确，但 `Level` 目前同时承担“委托细度”“风险治理”“验收闭环”三类职责，导致跨项目不可比、文档膨胀和 coder 能力被误判。建议保留 `FULL/LITE`、单调 Level、GC 治理和历史兼容边界，先把它们重新归位，再设计兼容迁移。

## 1. Blueprint 的第一性定义

Blueprint 不是详细需求文档，也不是检查清单。它是 Sol 将任务中需要由 coder 自行判断的决策空间显式闭合后，交给 coder 执行的委托接口。

```text
原始任务
  → Sol 完成问题解释、边界判断和必要决策
  → Blueprint 固化可验证的委托合同
  → coder 在剩余决策空间内实现
  → 通过证据验证交付
```

因此 Blueprint 的最小闭环是：目标、范围、不可变约束、责任边界、可观察结果、验证证据。文档长度、章节数量和 GC 数量都不是 Blueprint 本体。

## 2. Granularity Level 应衡量什么

Granularity 应衡量 coder 在不获得额外架构协助时仍需自行解决的决策空间，而不是文字数量。

```text
Level 越低 → coder 剩余决策空间越大
Level 越高 → Sol 预先闭合的决策越多
```

可比较的 Level 必须描述“闭合了哪一类决策”，例如目标与边界、架构职责、数据/状态流、文件与符号、错误与并发、测试与证据。它不应描述某个项目的具体目录或某批 GC。

Level 的单调性可以保留，但应允许任务只声明实际需要的闭合层级；不能因为 FULL、风险或治理要求存在，就机械追加与委托无关的细节。

## 3. 现有三轴的保留与归位

| 现有概念 | 保留 | 应明确的职责 |
|---|---|---|
| `FULL/LITE` | 保留 | 工件集合与治理覆盖范围，不表示委托细度 |
| `L1~LN` | 保留为统一阶梯 | 只表示剩余决策空间的闭合程度 |
| `GC-01~GC-48` | 保留历史兼容 | 治理约束、审计和经验，不改变 Level 语义 |
| `Q/AF` | 保留 | 缺口与阻断反馈，不作为 Level 分数 |
| Self-Application | 保留 | 检查 Blueprint 是否遵守自己的边界，不增加新的颗粒度轴 |

推荐的任务描述形式是：

```text
Blueprint mode = FULL or LITE
Required closure = 某个统一 Level
Task profile = 任务类型与风险条件
Project overlay = 项目特殊约束
Governance = 适用的 GC / Q / AF
```

## 4. Global Core + Project Overlay

Global Core 只定义跨项目可比较的语义：每一级闭合什么决策、最低需要什么证据、何时必须停止并升级。Project Overlay 只定义本项目的真相源、构建命令、目录约束、平台风险和历史兼容规则。

Overlay 不得重新定义“L5 在本项目是什么意思”，只能补充“本项目中 L5 的通用闭合要求如何落地”。例如，KMP 的 shared/androidApp 边界是项目约束；它不是第二套 Level。

判断规则：若一条规则可以脱离 Cookbook 仍然成立，它候选进入 Global Core；若规则依赖本项目文件、工具或业务状态，则留在 Overlay；若只是一次任务的选择，则留在 Task profile。

## 5. 项目经验晋升机制

现有 GC 和经验文档不应直接改写为新的 Level。项目经验只有在满足以下条件后，才可提出 Global Core 候选：

1. 同类失败在至少两个独立任务或项目中重复出现；
2. 能抽象为与具体目录、模型和工具无关的决策缺口；
3. 能写成可执行的边界或证据要求；
4. 不与现有 Level、GC 或真相源冲突；
5. 经过独立审查后，才进入后续协议演进批次。

本评估不新增 GC，也不把现有 GC 自动晋升为 Universal Level。

## 6. Coder Capability Profile

coder 能力不应压缩成一个永久数字。建议按任务类型记录最低可靠闭合层级，并同时记录证据质量：

```text
coder capability:
  task_type: bugfix / feature / refactor / migration / concurrency / test
  observed_minimum_level: 任务在该 Level 下稳定完成的最低值
  sample_count: 有效样本数
  rework_count: 返工次数
  scope_violation_count: 越界次数
  evidence_completeness: 证据完整度
```

单次成功不能升级画像。至少需要同类任务的重复样本，并区分“coder 能力不足”“Blueprint 缺口”“环境失败”和“需求变化”。

## 7. 同一任务的能力测试

能力测试应使用同一任务的等价 Blueprint 变体：只改变预先闭合的决策范围，不改变目标、验收标准和输入数据。比较指标包括：

- 首次通过率；
- 返工次数；
- 越界或擅自决策次数；
- Q/AF 数量及归因；
- 测试和证据完整度；
- 完成时间与额外澄清次数。

若低 Level 失败、高 Level 成功，只能说明该 coder 在该任务类型上的最低可靠闭合层级尚未确定；还必须排除 Blueprint 缺口和环境因素。

## 8. 最低安全 Level 的选择

不建立第二套评分系统。Level 选择应是已有统一 Level 阶梯上的取最大值：

```text
required level = max(
  coder/task-type minimum,
  task risk floor,
  project governance floor
)
```

这里的三个输入不是三个新等级体系：

- coder/task-type minimum：能力画像中的历史观察值；
- task risk floor：任务本身对闭合程度的最低要求；
- project governance floor：项目既有规则规定的最低门槛。

如果输入无法由证据确定，默认选择更保守的既有 Level，并记录不确定性，而不是临时创造新分数。

## 9. 如何避免高 Level 退化成清单堆叠

每增加一层，必须回答“减少了 coder 哪类决策”。如果只是增加格式、审计栏或重复说明，而没有减少实际决策空间，就不应提升 Level。

Blueprint 应以最小充分闭合为目标：

```text
足够详细 = coder 能在边界内独立完成并给出证据
过度设计 = Sol 已替 coder 完成实现选择，coder 只剩抄写
```

高 Level 允许接近机械执行，但不应成为默认选项；只有任务风险、coder 画像或重复失败证据支持时才使用。

## 10. 对当前用户级协议的 Gap Analysis

### 已经符合第一性目标的部分

- 区分 `FULL/LITE` 与 Level；
- 强调蓝图缺口不能由 coder 自行发挥；
- 保留 Q/AF、审计和 Self-Application 反馈闭环；
- 要求测试、证据和真相源；
- 支持项目级特殊规则和历史 GC。

### 需要后续重审的结构性偏差

1. Level 语义与项目治理条款耦合，跨项目可比性不足；
2. FULL/LITE、Level、GC、审计字段可能重复表达同一约束；
3. 当前规则更擅长规定“蓝图必须包含什么”，但较少规定“替 coder 消除了哪类决策”；
4. coder 能力、任务风险和项目最低门槛尚未形成清晰的输入关系；
5. Q/AF 反馈已有闭环，但尚未稳定区分 coder 失败、蓝图缺陷和环境失败。

这些是后续重构候选，不是本批对用户级协议的修改。

## 11. 兼容迁移建议

迁移必须分阶段进行：

1. 冻结现有 L7 与 GC-01~GC-48 的历史含义，建立映射说明；
2. 在不改协议的前提下，为新任务增加“委托闭合说明”字段的试运行样例；
3. 用真实任务验证哪些 Level 描述具有跨项目稳定性；
4. 只把重复、可泛化的缺口提交为后续 Global Core 变更候选；
5. 最后才讨论协议字段或用户级规则迁移。

任何迁移批次都必须保持旧蓝图可审计、旧 GC 可反查、旧项目文档可读取。不得通过批量改名、删除历史记录或重写既有 GC 来制造“迁移完成”。

## 12. 决策与后续入口

本评估支持继续研究 Universal Blueprint Framework，但不支持现在直接重构用户级协议。下一批若获得明确授权，应先选择少量真实任务做对照试验，验证“Level = 剩余决策空间闭合程度”是否比现有描述更能预测 coder 的可靠性，再决定是否进入协议迁移设计。

本文件不改变运行时代码、数据库、测试 fixture、用户级规则或项目当前治理状态。
```

### PROJECT.md

- Actual absolute path: `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\PROJECT.md`
- Scope: `PROJECT-LEVEL`
- SHA-256: `F00233A7992539AD05521ACEDDE7ECC182DA2F8DB176D9C0C5772DF5F549DF37`
- Line count: 52

```markdown
# Cookbook 项目上下文入口

> 用途：Claude、Codex、DeepSeek 或其他模型首次接手本仓库时的最小必读导航。
> 定位：CookBook 项目的稳定导航入口。短生命周期事实由各 canonical source 持有，本文件不维护其副本。

## 项目事实

- 产品：面向慢性病家庭的饮食规划 App，核心链路为记录餐食、食历复用、饮食推荐和健康提示。
- 技术：Kotlin Multiplatform；`shared` 承载 Domain/Data/SQLDelight，`androidApp` 承载 Compose UI；当前只交付 Android。
- 文档规则：项目通用规范、状态、方案、经验和交接只写本 `.ai-context/`；`.claude/`、`.codex/` 只保留工具专属配置。

## 首读顺序与真相优先级

1. 项目入口规则：仓库根 `AGENTS.md`（Codex）或 `CLAUDE.md`（Claude）。
2. 项目视图与历史资料：`docs/projectReview/00_导读与索引.md`，再按其阅读路径下钻；Project Truth 仍以 Project Graph 为准。
3. Project Truth 入口：`.ai-context/project_graph/README.md`、`.ai-context/project_graph/project.yaml`；Feature、WorkItem、Plan、Verification、Relation、CurrentWork 以 Project Graph 为准。
4. 当前进行中状态：`docs/context_memory/SESSION_交接.md`；它是 Handoff Context，不覆盖 Project Graph 或已接受决策。
5. 代码定位：`docs/功能路径索引.md`。
6. 任务范围：`docs/feature/待办索引.md` 与相应专项文档；工程与踩坑：`docs/experience/INDEX.md`。
7. 具体功能按需读 `docs/feature/`；架构、流程、数据、AI 和诊断按需读 `docs/projectReview/`。

Phase 2 Frozen Truth Hierarchy：Runtime Truth（Code / DB / schema / runtime config）> Project Truth（Project Graph）> Decision Truth（Accepted Plan / ADR / Formal Blueprint）> Execution Extension（BLUEPRINT_STATE）> Handoff Context（SESSION）。任何“待实现”不等于已经存在于代码。

## 协作模式

- **协作模式: BLUEPRINT**（常驻声明，跨机器协作时无需每次口令触发）。ARCH / CODE / REVIEW / TURN 以 `docs/context_memory/BLUEPRINT_STATE.md` 当前值为准。
- 规则正文：用户级真相源 `~/.ai-context/rules/blueprint_protocol.md`；CookBook canonical GC / fallback：`docs/experience/12_多模型协作与实施蓝图规范.md`。
- 握手状态唯一文件：`docs/context_memory/BLUEPRINT_STATE.md`。开工前先 `git pull` 读取；`TURN` 不是自己则停手、只报告持球方。
- 具体模型执行记录唯一事实源：`docs/experience/14_模型执行力评估.md`。

## 稳定导航指针

- 当前批次 / TURN / 当前执行状态：唯一读取 `docs/context_memory/BLUEPRINT_STATE.md`。
- 任何编码模型实施前必须读 `docs/experience/12_多模型协作与实施蓝图规范.md`（蓝图协议 + GC 条款）。
- 规则与反查：`docs/projectReview/21_AI与网络请求策略（专属）.md`、`08_决策记录.md` D-15/D-16、`05_诊断地图.md`。
- 真机验证只认 `docs/feature/真机待验证清单_<yyyyMMddHHmm>.md` 中时间最新的一份。

## 文档分层

| 位置 | 内容与使用方式 |
|---|---|
| `docs/projectReview/` | 架构、流程、UI、数据、决策、诊断等项目视图与历史资料；Project Truth 仍以 Project Graph 为准。 |
| `docs/context_memory/SESSION_交接.md` | 唯一当前会话接续入口；其他日期文件均为历史快照。 |
| `docs/feature/` | 当前功能方案、待办、验收与唯一真机清单。 |
| `docs/experience/` | 可复用工程经验与踩坑；由 `INDEX.md` 导航。 |
| `docs/feature/_archive/` | 历史资料，只用于追溯，不能覆盖当前方案或状态。 |

## 代码近旁资料

- `data-pipeline/` 的 README、脚本说明、候选与映射 review 是数据生产工具的**代码近旁资料**，不作为项目通用首读入口；涉及预设/营养数据生产时，从 `docs/projectReview/22_预设与参考资料治理（专属）.md` 进入，再按需读取。
- 根目录 `docs/` 的历史需求/规划已迁入 `docs/feature/_archive/legacy_root_docs/`；不得再创建新的根 `docs/` 项目知识副本。
```

### GOV_BP_P3_01_AUDIT.md

- Actual absolute path: `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\GOV_BP_P3_01_AUDIT.md`
- Scope: `PROJECT-LEVEL`
- SHA-256: `71556F45B3AF38B5CD00C1CAEB980F41F0F69D802577707719E9165677CB5AFD`
- Line count: 105

```markdown
# GOV-BP-P3-01 R1 Audit

> 角色：CODE 交付审计；状态：EXECUTED / PENDING INDEPENDENT ARCH REVIEW；本文件是持久化证据，不是治理 registry、state ledger 或 Project Truth。

## A. Baseline Identity

| 项目 | 值 |
|---|---|
| Design Baseline | `21e54015ec5ce0fb02d0f47911a6442400a8c44b` |
| Interposed Phase 3A R1 | `e0ae8bc3f925ae6974c41f2aa9d844e2c95219ff` |
| Initial Governance Delivery | `586652388cde269b614728d8160e7963bd88452c` |
| R1 Execution Parent | `586652388cde269b614728d8160e7963bd88452c` |
| Review Target | 本批最终 commit（push 后由 ARCH 读取 exact hash） |
| Comparison base | allowlist diff 相对 R1 Execution Parent；不以 Design Baseline 判定越界 |

## B. Global → Project Semantic Parity Matrix

| Canonical requirement | Global location | Project location | Result |
|---|---|---|---|
| Baseline Identity | `blueprint_protocol.md` Governance Batch Identity | `12_...规范.md` §14.7；本文件 A；STATE 基线行 | PASS |
| Persisted Evidence Landing | `blueprint_protocol.md` Governance Batch Identity | 本文件 C/D/E/F | PASS |
| External Canonical Evidence | `blueprint_protocol.md` External canonical clause | `12_...规范.md` §14.7；本文件 E | PASS |
| Blueprint Escape | protocol §4 + §14.6 | `12_...规范.md` §14.6；本蓝图 §3 | PASS |
| Granularity | protocol §2.1/§2.2 | `12_...规范.md` §12/GC registry；STATE L7 | PASS |
| Scale | protocol Improvement Review | `12_...规范.md` §14.6；本蓝图 §5 | PASS |
| CV-1~CV-4 | protocol Cross-Validation Contract | `12_...规范.md` §14.6；本文件 C/D | PASS |
| Propagation | protocol Improvement Review / Sibling Scan | `12_...规范.md` §14.8；本文件 F | PASS |
| Over-design | protocol Architecture Action | `12_...规范.md` §14.6；本蓝图 §19 | PASS |
| Self-Application | protocol Canonical Sibling Entry Scan | `12_...规范.md` §14.8；本文件 F | PASS |

## C. Requirement → Evidence

| Requirement | Evidence source | Reproduction / inspection | Landing | Result |
|---|---|---|---|---|
| R1-01~02 baseline split | A + STATE | `git rev-parse HEAD^` and A | A | PASS |
| R1-03 GC-02 | 12 registry + §14.7 | inspect allowlist/denylist and batch base | B/F | PASS |
| R1-04 GC-06 | protocol + §14.7 | inspect command/evidence landing contract | B/C | PASS |
| R1-05 GC-37 / §6 | registry + §14.6 | B row and canonical coverage scan | B | PASS |
| R1-06 GC-47 / §10 / §14.6 | registry + §14.6 | inspect fixed review fields | B/F | PASS |
| R1-07~08 GC-48 recurrence and sibling scan | registry + §14.8 | F six-column scan | F | PASS |
| R1-09 parity | B | read all 10 rows | B | PASS |
| R1-10~11 bidirectional matrices | C/D | inspect each row and source mapping | C/D | PASS |
| R1-12~14 external evidence | E | SHA/method/access/continuity fields | E | PASS |
| R1-15~16 history and model ledger | 12 history + 14 ledger | inspect dated history and initial commit | F / 14 | PASS |
| R1-17~19 GC and level | 12 registry | recount IDs, search GC-49/L8 | G | PASS |
| R1-20~21 graph/Phase 3A preserve | `git diff` | denylist diff audit | G | PASS |
| R1-22 TURN | STATE | inspect current batch row | STATE | PASS |

## D. Evidence → Requirement

| Evidence | Mapped requirements | Source / command | Result |
|---|---|---|---|
| A baseline table | R1-01, R1-02, R1-16 | `git rev-parse HEAD^`; `git show --format=` | PASS |
| B parity matrix | R1-05, R1-06, R1-09 | global/project marker scan | PASS |
| C/D bidirectional tables | R1-10, R1-11 | table review against B | PASS |
| E external table | R1-12, R1-13, R1-14 | `Get-FileHash` before/after; method/access fields | PASS |
| F self-application table | R1-07, R1-08, R1-15 | sibling keyword/entry/gate/template/lifecycle scan | PASS |
| G command results | R1-17~21 | commands recorded below | PASS |

## E. External Canonical Evidence

| Field | Value |
|---|---|
| Path | `~/.ai-context/rules/blueprint_protocol.md` |
| Truth Role | Global Blueprint Truth; project file is fallback, not co-owner |
| Execution Machine | `当前机` |
| Reviewer Direct Access | `NO`（审计工件可提交；ARCH 需在可访问该用户目录的机器重算） |
| SHA-256 Before | `C4F3A116265DE97B105CE988AA65B50957C80FA2661B4811EE752F53D46537F5` |
| SHA-256 After | `C2C8332EB12D545CA89FCA4C80A15DBA7E2ACF5FAF7703A8CFE6815A0B5F0EB3` |
| Mutation Method | `apply_patch`；变更前后 `Get-FileHash -Algorithm SHA256` |
| Changed Semantic Clauses | 增加 Governance Batch Identity and Evidence Landing；增加 Canonical Sibling Entry Scan |
| Project Fallback Mapping | `12_...规范.md` §14.7/§14.8；GC-02/GC-06/GC-48 保持项目语义一致 |
| Local Parity Command | `rg -n "Baseline Identity|Persisted Evidence|External Canonical|Canonical Sibling|CV-1|Propagation|Over-design" <global> <project-12>` |
| Persisted Evidence Landing | 本文件 E；STATE 只保留摘要与指针 |
| Next-Batch Continuity Gate | 下一批重算该路径 SHA-256，必须等于本行 SHA-after；否则 STOP，并标记 `REMOTE_ATTESTED_EXTERNAL_STATE` |

## F. GC-48 Six-Column Self-Application

| New / Modified Rule or Record | Affected Governance Files | Self-Check Target | Compliance Result | Violation | Disposition |
|---|---|---|---|---|---|
| Batch identity / evidence landing | global protocol; 12; STATE; current audit | baseline, parent, review target, evidence landing | PASS | none | keep split truth roles |
| External canonical mutation contract | global protocol; 12; current audit | SHA, access label, continuity gate | PASS | none | remote-attested boundary recorded |
| Canonical sibling scan | global protocol; 12; current audit | keyword, entry, gate, template, lifecycle siblings | PASS | none | no stale parallel clause found |
| GC-02 registry | 12; current Blueprint | allowlist/denylist/base | PASS | none | strengthen existing GC-02; no GC-49 |
| GC-06 evidence | 12; current Blueprint | command + persisted landing | PASS | none | strengthen existing GC-06 |
| GC-37 challenge | 12; current Blueprint | canonical requirement coverage | PASS | none | preserve and propagate |
| GC-47 review loop | 12; current Blueprint | attribution through evidence | PASS | none | preserve and propagate |
| GC-48 self-application | 12; current Blueprint; future Phase 3B~3J | six-column coverage and denylist | PASS | none | future propagation only; no denylist mutation |
| Stable project entry / state | PROJECT.md; BLUEPRINT_STATE; 14 | pointer ownership and no duplicate model facts | PASS | none | preserve existing owners |

## G. Programmatic Checks and Boundary

Commands executed for this batch:

```text
git rev-parse HEAD                  -> 586652388cde269b614728d8160e7963bd88452c
git rev-parse HEAD^                 -> e0ae8bc3f925ae6974c41f2aa9d844e2c95219ff
python -m unittest test_validator -v  -> 61 tests, 61 passed, 0 failed, 0 errors
python .ai-context/project_graph/tools/project_graph.py check -> PG OK; mode=draft; counts 13/109/4/98/10
GC registry recount                -> 48 unique; missing=0; duplicate=0; GC-49 absent; L7; L8 absent
denylist diff vs 5866523           -> 0 (Phase3A / Graph / PROJECT / SESSION / legacy / production)
```

This audit records CODE facts only. It does not grant ARCH acceptance and does not authorize Phase 3B.
```

### PHASE2_TO_PHASE3_HANDOFF.md

- Actual absolute path: `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE2_TO_PHASE3_HANDOFF.md`
- Scope: `PROJECT-LEVEL`
- SHA-256: `EC20B46284CDE13DDAC089709273BC991815FA2B80F95D4DA7DC2E7F1FC70867`
- Line count: 53

```markdown
# Phase 2 → Phase 3 Handoff

> Phase 2 已 FINAL ACCEPT / FROZEN。本文件是 Phase 3 新会话的唯一治理交接入口；它授权后续设计，不代表 Phase 3 已开始实施。

## Handoff status

```text
From: Phase 2 FINAL ACCEPT / FROZEN
To: Phase 3
Phase 3 Status: AUTHORIZED / NOT STARTED
Phase 3 Mission: Views + Activation
Graph Mode: draft
Phase 2 Review Commit: fd3ded5e080fe772d820815366269fb536e463df
```

## Phase 3 must read first

1. `migration/PHASE2_FINAL_ACCEPT.md`
2. `migration/PHASE2E_ACCEPT.md`
3. `.ai-context/project_graph/README.md`
4. `.ai-context/project_graph/project.yaml`
5. `.ai-context/docs/context_memory/SESSION_交接.md`
6. `.ai-context/project_graph/migration/PHASE2E_VIEW_DRIFT.md`

## Authorized scope

Phase 3 may design and, after a new architecture blueprint is accepted, implement:

- Graph-derived AI_INDEX and human/AI views for project status, features, Todo, Bug, Plan, Verification and Current Work/Handoff。
- A thin SESSION handoff view derived from Project Graph truth。
- Renderer/lifecycle tooling and the activation readiness path。
- The explicit governance decision for `mode: draft → active`。

## Invariants to preserve

- Phase 1 Frozen Core Contract and all Phase 2 Project Truth entities/IDs。
- Feature Registry = 13 and all stable Feature/WorkItem/Plan/Verification identities。
- Verification closure and WorkItem status semantics。
- CurrentWork `F-AI-MEAL / K1i / verifying`。
- `E-K1G-01 = ACCEPTED_LEGACY / not_required` and L3 `F-TOOLS / RESOLVED_NO_REGISTRY_CHANGE`。
- One Truth, Multiple Views: generated views must not become independent truth sources。

## Explicit stop conditions

- Do not begin Phase 3 implementation in this handoff commit。
- Do not set Graph mode to `active`。
- Do not modify schema, validator contract, production code, Feature YAML, WorkItem/Verification/Plan/Relation semantics or legacy views。
- Do not create `PHASE3_*.md` or renderer code here。

## Next authorized action

Open a fresh Phase 3 control conversation, read the files above, issue a Phase 3 architecture blueprint, and wait for its acceptance before implementation.
```

### PHASE2E_VIEW_DRIFT.md

- Actual absolute path: `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE2E_VIEW_DRIFT.md`
- Scope: `PROJECT-LEVEL`
- SHA-256: `1F6B19171248D221EBCF1E93E8E1A9C138903D2EC50C2A67EBC1955203052AA4`
- Line count: 18

```markdown
# Phase 2E Legacy View Drift

> This ledger identifies drift only. No legacy view is rewritten in Phase 2E; replacement belongs to Phase 3.

| View | Currently authoritative? | Known drift | Future owner | Action now |
|---|---|---|---|---|
| `.ai-context/docs/projectReview/07_项目现状.md` | NO / PARTIAL | Human project-status snapshot can lag Graph phase/status and WorkItem truth | Phase 3 generated/human view | NONE |
| `.ai-context/docs/功能路径索引.md` | NO / PARTIAL | Navigation index is maintained separately from Graph entities | Phase 3 generated view | NONE |
| `.ai-context/docs/feature/待办索引.md` | NO / PARTIAL | Legacy completion markers may differ from Graph status | Phase 3 generated/human view | NONE |
| `.ai-context/docs/feature/待办_Bug修复.md` | NO / PARTIAL | Historical bug list is not a Graph registry | Phase 3 generated/human view | NONE |
| `.ai-context/docs/feature/待办_功能算法.md` | NO / PARTIAL | Legacy feature/planning status may lag accepted Plans/WorkItems | Phase 3 generated/human view | NONE |
| `.ai-context/docs/feature/待办_UI交互.md` | NO / PARTIAL | Human UI backlog is not authoritative for Graph CurrentWork | Phase 3 generated/human view | NONE |
| `.ai-context/docs/feature/待办_数据健康.md` | NO / PARTIAL | Human health backlog is not authoritative for Verification closure | Phase 3 generated/human view | NONE |
| `.ai-context/docs/feature/待办_工程合规.md` | NO / PARTIAL | Compliance checklist may retain stale implementation markers | Phase 3 generated/human view | NONE |
| `.ai-context/docs/feature/待办_战略会商.md` | NO / PARTIAL | L3 remains a historical/product planning source; Graph owns current relation/ownership | Phase 3 generated/human view | NONE |

SESSION is excluded from this no-edit list because it is the current handoff context and is reconciled separately.
```

### PHASE3_ARCHITECTURE_ACCEPT.md

- Actual absolute path: `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE3_ARCHITECTURE_ACCEPT.md`
- Scope: `PROJECT-LEVEL`
- SHA-256: `82ADB84CCE625C7B4CE2C277DDE50E97E5B6B373AEF3B5E42252E3040F0F03CF`
- Line count: 34

```markdown
# Phase 3 Architecture Accept Record

> Immutable architecture review record. This is not Project Truth, lifecycle state, or a generated view.

- Baseline: `598daf4e5083d62038adfe39b1635993a7d90fa4`
- Architecture Decision: `ACCEPT`
- Phase 3 Mission: Views + Activation
- Graph Mode at Architecture Acceptance: `draft`

## Architecture Decisions

1. One Truth, Multiple Views.
2. Project Graph remains Project Truth.
3. Renderer is one-way: Graph → Views.
4. Renderer must never mutate Graph.
5. Formal renderer CLI direction remains `pg render`.
6. Renderer contract will require deterministic behavior.
7. Drift-check mode must be zero-write.
8. Renderer failure model is fail-closed.
9. `PROJECT.md` remains the stable entry/pointer layer.
10. `AI_INDEX` is a generated current-state view, not replacement Project Truth.
11. `SESSION` is a thin handoff target, not independent truth.
12. Legacy views require classify → migrate/hybrid/retire, not dual-write.
13. Activation Readiness and draft→active are separate phases.
14. draft→active occurs only in the final isolated activation batch.
15. Phase 3 is decomposed 3A→3J, with explicit STOP/Handoff between accepted subphases.

## Not Frozen by This Record

Current WorkItem status, current Verification status, CurrentWork future transitions,
derived counts, final generated-file list, renderer file layout, functional-path disposition,
activation date, and exact Phase 3A implementation details remain open to their owning phases.
```

### PHASE3A_AUDIT.md

- Actual absolute path: `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE3A_AUDIT.md`
- Scope: `PROJECT-LEVEL`
- SHA-256: `F0C4A9A2E529C87BF9C252EFB79C78B6ABBB89FC7E5740F70B82B5BBA38CF9F2`
- Line count: 146

```markdown
# Phase 3A R1 Audit — Governance Closure + Classification Correction

## Baseline and fresh regression

- Rework baseline: 21e54015ec5ce0fb02d0f47911a6442400a8c44b.
- CH-P3A-R1-01..08: PASS; BLOCKER=0 before mutation.
- CurrentWork observation: feature=F-AI-MEAL, work_item=K1i, phase=verifying, blocker=.
- Validator: python -m unittest test_validator -v → 61 tests, 61 passed, 0 failed, 0 errors.
- pg check: OK; features=13, work_items=109, plans=4, verifications=98, relations=10; mode=draft; graph_version=1.
- Independent recount: 13/109/4/98/10; duplicate=0; dangling=0; all issues=0.

## Repository-wide Markdown discovery

Method:

    git ls-files .ai-context
    python one-shot scan using CurrentWork, Current Work, 当前工作, 当前状态,
    Graph Mode, WorkItem, Verification, Plan, Feature Registry, 待办, Bug,
    TURN, blocker, verifying, in_progress, done

Actual result: tracked_markdown=202; marker_hit_files=59; candidate_views=18; unresolved_candidates=0.

Marker-hit file list:

    .ai-context/PROJECT.md
    .ai-context/docs/context_memory/2026-06-03_fix11_dish_refresh_edit_storage.md
    .ai-context/docs/context_memory/2026-06-03_fix12_permission_edit_ingredient_storage.md
    .ai-context/docs/context_memory/2026-06-03_pretask_fix11_dish_refresh_edit_storage.md
    .ai-context/docs/context_memory/2026-06-03_pretask_fix12_edit_ingredient_storage.md
    .ai-context/docs/context_memory/2026-06-03_pretask_sqlite_downgrade_v2_to_v1.md
    .ai-context/docs/context_memory/2026-06-04_pretask_dish_edit_blank.md
    .ai-context/docs/context_memory/2026-06-04_pretask_edit_dish_first_open.md
    .ai-context/docs/context_memory/2026-06-05_ingredient_category_research_plan.md
    .ai-context/docs/context_memory/2026-06-10_fix_ingredient_recent_after_dish_save_result.md
    .ai-context/docs/context_memory/2026-06-10_pretask_fix_ingredient_recent_after_dish_save.md
    .ai-context/docs/context_memory/2026-06-21_ingredient_detail_toggle_close_and_multilevel_tree_done.md
    .ai-context/docs/context_memory/2026-06-21_ingredient_sheet_and_image_click_fix_done.md
    .ai-context/docs/context_memory/2026-06-21_ingredient_unified_detail_sheet_done.md
    .ai-context/docs/context_memory/2026-06-21_pretask_ingredient_detail_toggle_close_and_next_step.md
    .ai-context/docs/context_memory/2026-06-21_pretask_ingredient_unified_detail_sheet.md
    .ai-context/docs/context_memory/2026-06-21_pretask_selected_ingredients_popover_and_duplicates.md
    .ai-context/docs/context_memory/2026-06-21_pretask_step_delete_and_tab_layout_fix.md
    .ai-context/docs/context_memory/2026-06-21_selected_ingredients_popover_and_duplicates_done.md
    .ai-context/docs/context_memory/2026-06-22_ingredient_ui_bugs_done.md
    .ai-context/docs/context_memory/2026-06-22_pretask_ingredient_ui_bugs.md
    .ai-context/docs/context_memory/2026-06-25_pretask_ingredient_custom_category_rules.md
    .ai-context/docs/context_memory/2026-07-03_pretask_ai_context_and_ingredient_feature.md
    .ai-context/docs/context_memory/BLUEPRINT_STATE.md
    .ai-context/docs/context_memory/unattended_decisions.md
    .ai-context/docs/experience/INDEX.md
    .ai-context/project_graph/README.md
    .ai-context/project_graph/migration/PHASE1_FINAL_ACCEPT.md
    .ai-context/project_graph/migration/PHASE2A_ACCEPT.md
    .ai-context/project_graph/migration/PHASE2A_REVIEW.md
    .ai-context/project_graph/migration/PHASE2A_TO_2B_HANDOFF.md
    .ai-context/project_graph/migration/PHASE2B_ACCEPT.md
    .ai-context/project_graph/migration/PHASE2B_CONFLICTS.md
    .ai-context/project_graph/migration/PHASE2B_INVENTORY.md
    .ai-context/project_graph/migration/PHASE2B_SOURCE_COVERAGE.md
    .ai-context/project_graph/migration/PHASE2B_TO_2C_HANDOFF.md
    .ai-context/project_graph/migration/PHASE2C_ACCEPT.md
    .ai-context/project_graph/migration/PHASE2C_CONFLICTS.md
    .ai-context/project_graph/migration/PHASE2C_DECISIONS.md
    .ai-context/project_graph/migration/PHASE2C_PLAN_INVENTORY.md
    .ai-context/project_graph/migration/PHASE2C_TO_2D_HANDOFF.md
    .ai-context/project_graph/migration/PHASE2D_ACCEPT.md
    .ai-context/project_graph/migration/PHASE2D_CONFLICTS.md
    .ai-context/project_graph/migration/PHASE2D_INVENTORY.md
    .ai-context/project_graph/migration/PHASE2D_SOURCE_COVERAGE.md
    .ai-context/project_graph/migration/PHASE2D_TO_2E_HANDOFF.md
    .ai-context/project_graph/migration/PHASE2E_ACCEPT.md
    .ai-context/project_graph/migration/PHASE2E_CONFLICTS.md
    .ai-context/project_graph/migration/PHASE2E_CONFLICT_RECONCILIATION.md
    .ai-context/project_graph/migration/PHASE2E_DECISIONS.md
    .ai-context/project_graph/migration/PHASE2E_RECONCILIATION.md
    .ai-context/project_graph/migration/PHASE2E_VERIFICATION_RECONCILIATION.md
    .ai-context/project_graph/migration/PHASE2E_VIEW_DRIFT.md
    .ai-context/project_graph/migration/PHASE2_FINAL_ACCEPT.md
    .ai-context/project_graph/migration/PHASE2_TO_PHASE3_HANDOFF.md
    .ai-context/project_graph/migration/PHASE3A_AUDIT.md
    .ai-context/project_graph/migration/PHASE3A_BLUEPRINT.md
    .ai-context/project_graph/migration/PHASE3_ARCHITECTURE_ACCEPT.md

The 41 marker-hit historical context/experience/migration files are NOT_A_VIEW because their markers are evidence or narrative, not a current state/view/handoff target. Candidate resolution is complete; unresolved=0.

## View classification matrix

| Path | Source semantic | Canonical Truth Owner | Target Class | Action Owner | Closure Verifier | Human Edit Policy Target | Update / Regeneration Authority | Evidence |
|---|---|---|---|---|---|---|---|---|
| projectReview/07_项目现状.md | status + runtime narrative | Graph / Runtime by section | HYBRID_COVERAGE_AUDIT_REQUIRED | Phase 3E | Phase 3H | human narrative sections | Phase 3E migration | P2E-01 |
| docs/功能路径索引.md | mapping + navigation narrative | Graph + repository | HYBRID_COVERAGE_AUDIT_REQUIRED | Phase 3F | Phase 3H | human runtime pointers | Phase 3F migration | P2E-02 |
| feature/待办索引.md | legacy backlog taxonomy | Graph lifecycle | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase 3H | human taxonomy | Phase 3E | P2E-03 |
| feature/待办_Bug修复.md | historical bug narrative | Runtime / Graph by field | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase 3H | human history | Phase 3E | P2E-04 |
| feature/待办_功能算法.md | planning taxonomy | Graph accepted plans | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase 3H | human taxonomy | Phase 3E | P2E-05 |
| feature/待办_UI交互.md | UI backlog taxonomy | Runtime / Graph by field | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase 3H | human taxonomy | Phase 3E | P2E-06 |
| feature/待办_数据健康.md | health backlog taxonomy | Runtime / Graph by field | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase 3H | human taxonomy | Phase 3E | P2E-07 |
| feature/待办_工程合规.md | compliance narrative | Governance records / Graph | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase 3H | human checklist | Phase 3E | P2E-08 |
| feature/待办_战略会商.md | historical planning/relations | Graph relations | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase 3H | human planning | Phase 3E | P2E-09 |
| PROJECT.md | stable navigation/pointers | PROJECT pointer layer | STABLE_ENTRY_POINTER | Phase 3G | Phase 3I | preserve pointer | human + governance review | direct read |
| SESSION_交接.md | handoff context | Graph / state / decisions by section | THIN_HANDOFF_CANDIDATE | Phase 3G | Phase 3H | handoff authoring | Phase 3G | direct read |
| BLUEPRINT_STATE.md | execution handshake | BLUEPRINT_STATE | EXECUTION_STATE_CANONICAL | PRESERVE / CONTINUOUS HANDSHAKE | Phase 3I | CODE/ARCH handshake only | same-file handshake | direct read |
| AI_INDEX | absent generated target | Graph | ABSENT_TARGET | Phase 3D | Phase 3H | generated only | Phase 3D pilot | absent |
| Current Work View | absent projection | Graph current | ABSENT_TARGET | Phase 3D | Phase 3H | generated only | Phase 3D pilot | absent |
| Plan View | absent projection | Graph plans | ABSENT_TARGET | Phase 3E | Phase 3H | generated only | Phase 3E | absent |
| Verification View | absent projection | Graph verifications | ABSENT_TARGET | Phase 3E | Phase 3H | generated only | Phase 3E | absent |
| Handoff View | absent projection | SESSION / Graph sections | ABSENT_TARGET | Phase 3G | Phase 3H | generated + handoff text | Phase 3G | absent |

Truth owner, action owner, and closure verifier are intentionally distinct. Phase 3H verifies closure; it is not normal migration implementation owner.

## Phase2E carry-forward matrix (9/9)

| View | Action Owner | Closure Verifier | Status |
|---|---|---|---|
| project status | Phase 3E | Phase 3H | CLASSIFIED |
| functional path | Phase 3F | Phase 3H | DEFERRED_WITH_OWNER |
| todo index | Phase 3E | Phase 3H | DEFERRED_WITH_OWNER |
| bug todo | Phase 3E | Phase 3H | DEFERRED_WITH_OWNER |
| algorithm todo | Phase 3E | Phase 3H | DEFERRED_WITH_OWNER |
| UI todo | Phase 3E | Phase 3H | DEFERRED_WITH_OWNER |
| health todo | Phase 3E | Phase 3H | DEFERRED_WITH_OWNER |
| engineering todo | Phase 3E | Phase 3H | DEFERRED_WITH_OWNER |
| strategy todo | Phase 3E | Phase 3H | DEFERRED_WITH_OWNER |

No Phase2E row is CLOSED.

## GC-48 Self-Application Audit

| New / Modified Rule or Record | Affected Governance Files | Self-Check Target | Compliance Result | Violation | Disposition |
|---|---|---|---|---|---|
| PHASE3_ARCHITECTURE_ACCEPT | architecture record | preserve; no broad freeze | PASS | none | preserve |
| PHASE3A_BLUEPRINT | blueprint | 48 GC, minimal STEP, R1 structure | PASS | none | repaired |
| PHASE3A_AUDIT | audit | not drift registry; owner/verifier separation | PASS | none | repaired |
| PROJECT | PROJECT.md | stable pointer; no current state duplicate | PASS | none | preserve |
| BLUEPRINT_STATE | handshake | abstract role + machine only | PASS | none | continuous handshake |
| 14_模型执行力评估 | experience ledger | model row commit=21e54015 | PASS | none | same row corrected |
| PHASE2E_VIEW_DRIFT | Phase2E ledger | unchanged sole drift registry | PASS | none | preserve |

GC registry mutation: NONE. Existing GC-01..GC-48 remain canonical.

## R1 delivery gate

R1-01..R1-20 PASS: 48/48 GC; no range STEP; 12/12 closure; six-column GC-48; scan recorded; unresolved=0; ownership corrected; 3D pilot-only; 3H closure-only; continuous handshake; model commit fixed; four-file diff; preserved Graph, legacy, PROJECT, SESSION, architecture record; validator PASS; pg check PASS; mode=draft; TURN=REVIEW.

Final state: Phase 3A R1 EXECUTED / PENDING INDEPENDENT ARCH REVIEW; STOP.
```

### PHASE3A_BLUEPRINT.md

- Actual absolute path: `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\migration\PHASE3A_BLUEPRINT.md`
- Scope: `PROJECT-LEVEL`
- SHA-256: `EE0ACC657FD4E9B0A89D3621C84681AFEBC68DDB1E5DDA68AAA9DF89F8F9CB49`
- Line count: 192

```markdown
# Phase 3A Blueprint — CODE Execution Record

## Baseline and Mutation Declaration

- Architecture baseline: `598daf4e5083d62038adfe39b1635993a7d90fa4`
- Graph mode remains `draft`.
- Stable identity, Graph contract, lifecycle state, CurrentWork, Graph data, and legacy views are immutable in 3A.
- Only new audit evidence, the two Phase 3 records, the execution ledger, and the handshake/model records are mutable.
- No renderer, generated view, legacy rewrite, SESSION rewrite, production-code, or Phase 3B change is authorized.

## Allowlist / Denylist

Allowlist: `migration/PHASE3_ARCHITECTURE_ACCEPT.md`, `migration/PHASE3A_BLUEPRINT.md`,
`migration/PHASE3A_AUDIT.md`, `docs/context_memory/BLUEPRINT_STATE.md`, and
`docs/experience/14_模型执行力评估.md`.

Denylist: Graph YAML/schema/tools, `PROJECT.md`, `SESSION_交接.md`, `PHASE2*.md`,
projectReview views, feature backlogs, production code, Gradle/config/runtime code,
renderer implementation, AI_INDEX generation, legacy migration, and mode activation.

## Truth Source Map

| Semantic | Authoritative source |
|---|---|
| Feature / WorkItem / Plan / Verification / Relation / CurrentWork | Project Graph |
| Phase 1/2 accepted architecture | Immutable migration records |
| Phase 3 architecture | `PHASE3_ARCHITECTURE_ACCEPT.md` |
| CODE/ARCH/REVIEW/TURN | `BLUEPRINT_STATE.md` |
| Concrete execution history | `14_模型执行力评估.md` |
| Existing Phase 2E drift | `PHASE2E_VIEW_DRIFT.md` |
| Handoff context | `SESSION_交接.md` |

## Independent Challenge Ledger

| ID | Result | Evidence |
|---|---|---|
| CH-3A-01 | PASS | One new Phase 3A audit is evidence only; Graph remains the sole project-state owner. |
| CH-3A-02 | PASS | `PHASE2E_VIEW_DRIFT.md` remains the only Phase 2E drift registry. |
| CH-3A-03 | PASS | Architecture ACCEPT is recorded separately; final 3A state is PENDING ARCH REVIEW. |
| CH-3A-04 | PASS | Counts are labeled fresh observations; no contract or lifecycle freeze is added. |
| CH-3A-05 | PASS | Final diff is restricted to the five-file allowlist. |
| CH-3A-06 | PASS | No renderer, generated view, or SESSION migration is performed. |
| CH-3A-07 | PASS | Deferred ledger below gives an exact Phase 3 owner for every open item. |
| CH-3A-08 | PASS | Ambiguous classifications are deferred with evidence and owner; no ownership is guessed. |
| CH-3A-09 | PASS | Acceptance is mechanically checked by validator, `pg check`, recount, and diff audits. |
| CH-3A-10 | PASS | No GC registry mutation; GC-48 self-application is recorded in the audit. |

## R1 Verified Defects and Repair Boundary

| ID | Defect | Repair |
|---|---|---|
| V-P3A-R1-01 | Missing full L7 GC disposition | Add GC-01 through GC-48 below. |
| V-P3A-R1-02 | STEP ledger used range rows | Use one row per minimal action. |
| V-P3A-R1-03 | GC-48 used a generic table | Use the canonical six-column table in the audit. |
| V-P3A-R1-04 | Action owner and closure verifier were conflated | Separate both fields and correct phase ownership. |
| V-P3A-R1-05 | Markdown discovery evidence was incomplete | Record actual scan counts, hit list, and unresolved=0. |
| V-P3A-R1-06 | Model row had self-referential commit semantics | Set the existing row commit to 21e54015; no new R1 row. |

Root cause: the previous blueprint declared L7 without embedding all mandatory disposition and minimal-step closure evidence. R1 is governance-document repair only.

## GC-01 ~ GC-48 Disposition

| GC | Disposition |
|---|---|
| GC-01 | PASS — unique defect/repair/STOP branch. |
| GC-02 | PASS — strict allowlist and denylist. |
| GC-03 | PASS — preserve/deferred owner is explicit. |
| GC-04 | PASS — invariants include condition/must/must-not/evidence. |
| GC-05 | PASS — invariant/test mapping is bidirectional. |
| GC-06 | PASS — commands and evidence locations are explicit. |
| GC-07 | N/A — no test fixture change. |
| GC-08 | N/A — no runtime or user-visible implementation. |
| GC-09 | PASS — validator suite rerun. |
| GC-10 | PASS — truth and action owners are separate. |
| GC-11 | N/A — no business state field. |
| GC-12 | N/A — no UI/business predicate. |
| GC-13 | N/A — no fallback path. |
| GC-14 | N/A — no resource holder. |
| GC-15 | N/A — no Compose state ownership. |
| GC-16 | N/A — no production block moved. |
| GC-17 | N/A — no UI projection. |
| GC-18 | N/A — no ordinal/index. |
| GC-19 | N/A — no collection consumer. |
| GC-20 | N/A — no user-visible side effect. |
| GC-21 | N/A — no runtime notification. |
| GC-22 | N/A — no real-device test. |
| GC-23 | PASS — each minimal repair has an independent STEP. |
| GC-24 | PASS — closure table has ID/status/landing/diff. |
| GC-25 | PASS — literals have grep/row evidence. |
| GC-26 | N/A — no threshold/model/constant. |
| GC-27 | N/A — no edit/invalidation entry. |
| GC-28 | N/A — no runtime cardinality. |
| GC-29 | N/A — no same-key runtime source. |
| GC-30 | N/A — no runtime transition. |
| GC-31 | N/A — no suspend/state write. |
| GC-32 | N/A — no async event. |
| GC-33 | N/A — no test injection. |
| GC-34 | PASS — declarations checked against final diff. |
| GC-35 | N/A — no protocol enum. |
| GC-36 | N/A — no List<Status> model. |
| GC-37 | PASS — challenge blocker means zero mutation/commit. |
| GC-38 | PASS — defect/root cause/reopen/preserve/repair/STOP present. |
| GC-39 | PASS — five mutation categories declared. |
| GC-40 | PASS — ownership has source semantic and authority evidence. |
| GC-41 | PASS — recount and Markdown scan recorded. |
| GC-42 | PASS — no new registry/state canonical file. |
| GC-43 | PASS — PROJECT remains stable pointer. |
| GC-44 | PASS — view truth/target/edit/update fields present. |
| GC-45 | N/A — no lifecycle change. |
| GC-46 | N/A — no verification ID mapping. |
| GC-47 | PASS — error attribution and feedback branch explicit. |
| GC-48 | PASS — canonical six-column self-application audit. |

Programmatic disposition check: unique=48, missing=0, duplicate=0.

## STEP Ledger

| Step | Result | Evidence |
|---|---|---|
| P3A-0.1 | PASS | Canonical inputs and user-level protocol were read; baseline is exact. |
| P3A-0.2 | PASS | CH-3A-01..10 resolved; blockers=0. |
| P3A-1.1 | PASS | Isolated worktree clean at baseline. |
| P3A-1.2 | PASS | Validator: 61 tests, 61 passed, 0 failed, 0 errors. |
| P3A-1.3 | PASS | `pg check`: OK; mode=draft; graph_version=1. |
| P3A-1.4 | PASS | Independent recount: 13/109/4/98/10; duplicates=0; dangling=0. |
| P3A-2.1 | PASS | Canonical registry discovery recorded in `PHASE3A_AUDIT.md`. |
| P3A-2.2 | PASS | Architecture accept record created. |
| P3A-2.3 | PASS | This execution blueprint created. |
| P3A-3.1 | PASS | Audit: Phase2E row 1 classified. |
| P3A-3.2 | PASS | Audit: Phase2E row 2 classified. |
| P3A-3.3 | PASS | Audit: Phase2E row 3 classified. |
| P3A-3.4 | PASS | Audit: Phase2E row 4 classified. |
| P3A-3.5 | PASS | Audit: Phase2E row 5 classified. |
| P3A-3.6 | PASS | Audit: Phase2E row 6 classified. |
| P3A-3.7 | PASS | Audit: Phase2E row 7 classified. |
| P3A-3.8 | PASS | Audit: Phase2E row 8 classified. |
| P3A-3.9 | PASS | Audit: Phase2E row 9 classified. |
| P3A-3.10 | PASS | Audit: PROJECT candidate classified. |
| P3A-3.11 | PASS | Audit: SESSION candidate classified. |
| P3A-3.12 | PASS | Audit: BLUEPRINT_STATE candidate classified. |
| P3A-3.13 | PASS | Audit: AI_INDEX absence classified. |
| P3A-3.14 | PASS | Audit: Current Work View absence classified. |
| P3A-3.15 | PASS | Audit: Plan View absence classified. |
| P3A-3.16 | PASS | Audit: Verification View absence classified. |
| P3A-3.17 | PASS | Audit: Handoff View absence classified. |
| P3A-4.1 | PASS | Mutation audit recorded. |
| P3A-4.2 | PASS | GC-48 self-application audit recorded. |
| P3A-5.1 | PASS | Existing Phase3A model row corrected. |
| P3A-5.2 | PASS | BLUEPRINT_STATE TURN=REVIEW. |
| P3A-5.3 | PASS | Final allowlist audit. |
| P3A-5.4 | PASS | Final `pg check`. |

No STEP range identifiers remain; all R1 repair actions use independent rows.

## STEP Closure Table

| STEP-ID | Status | Commit-or-baseline | Diff location |
|---|---|---|---|
| STEP-P3A-R1-01 | PASS | R1 worktree | R1 defects |
| STEP-P3A-R1-02 | PASS | R1 worktree | GC disposition |
| STEP-P3A-R1-03 | PASS | R1 worktree | STEP ledger |
| STEP-P3A-R1-04 | PASS | R1 worktree | Audit view matrix |
| STEP-P3A-R1-05 | PASS | R1 worktree | Phase2E carry-forward |
| STEP-P3A-R1-06 | PASS | R1 worktree | Repository discovery |
| STEP-P3A-R1-07 | PASS | R1 worktree | Audit GC-48 |
| STEP-P3A-R1-08 | PASS | R1 worktree | Model ledger row |
| STEP-P3A-R1-09 | PASS | R1 worktree | BLUEPRINT_STATE current batch |
| STEP-P3A-R1-10 | PASS | R1 worktree | Preserve-set command |
| STEP-P3A-R1-11 | PASS | R1 worktree | Validator/pg check |
| STEP-P3A-R1-12 | PASS | R1 worktree | This closure table |

## Deferred Item Ledger

| Item | Exact owner |
|---|---|
| Renderer Contract | Phase 3B |
| Renderer Implementation | Phase 3C |
| Generated Pilot | Phase 3D |
| Graph-owned Legacy Migration | Phase 3E |
| Functional Path / Hybrid Coverage | Phase 3F |
| SESSION Thin + AI Entry | Phase 3G |
| View Drift Closure | Phase 3H |
| Activation Readiness | Phase 3I |
| draft→active | Phase 3J |

## Acceptance Gate / STOP Gate

All CODE delivery gates A1–A23 pass in the accompanying audit. This record does not self-accept Phase 3A.
Final state is `EXECUTED / PENDING INDEPENDENT ARCH REVIEW`, `TURN=REVIEW`.
After commit and push, stop. Phase 3B and all later phases require a separate ARCH accept/handoff.
```

### project.yaml

- Actual absolute path: `C:\Users\SXD-T480S\Documents\WorkSpace\Gitee\cookbook\.ai-context\project_graph\project.yaml`
- Scope: `PROJECT-LEVEL`
- SHA-256: `2C756CE240C129E72276D7A97842C953580C006B768227BB06C086C270CA2F0F`
- Line count: 58

```markdown
# Project Graph — Project 根
# Phase 1 Model Contract: FINAL ACCEPT / FROZEN
# Phase 2A Feature Universe: ACCEPT / CLOSED
# Phase 2B Current WorkItem: ACCEPT / CLOSED
# Phase 2C Plan + Relation + Deferred Semantics: ACCEPT / CLOSED
# Phase 2D Verification Bootstrap: ACCEPT / CLOSED
# Phase 2E Cross-Reconcile + Bootstrap Freeze: ACCEPT / CLOSED
# Phase 2 Final: FINAL ACCEPT / FROZEN
# Current Bootstrap Stage: Phase 2 complete / pre-Phase-3
# Phase 3: AUTHORIZED / NOT STARTED
# mode: draft — Contract 已冻结；Graph 数据仍处 Bootstrap draft；Phase 3 完成前不得切 active。
# Truth hierarchy: Runtime=Code/DB/schema/config; Project=Project Graph; Decision=accepted Plan/ADR/Blueprint; Execution Extension=BLUEPRINT_STATE; Handoff=SESSION。
kind: project
graph_version: "1"
mode: draft

project:
  id: cookbook
  name: Cookbook
  root: .

# 当前工作（CurrentWork）——单一入口，不散落多文件。Phase 1 只定义，不替换 SESSION。
current:
  feature: F-AI-MEAL
  work_item: K1i
  phase: verifying
  blocker: ""

# Feature Registry ——长期稳定的产品/系统能力清单（声明宇宙）。
# Feature Universe established in Phase 2A: 13 / 13
# Feature Registry is frozen during Phase 2 bootstrap.
# New Feature creation requires architecture review.
features:
  - F-MEAL
  - F-AI-MEAL
  - F-TIMELINE
  - F-INGREDIENT
  - F-DISH
  - F-PANTRY
  - F-RECOMMEND
  - F-NUTRITION
  - F-HEALTH
  - F-FAMILY
  - F-WEEKPLAN
  - F-SYNC
  - F-TOOLS

# 扩展机制——核心不写死角色/平台/业务。Cookbook 现有 BLUEPRINT_STATE CODE/ARCH 作为 Extension。
# 不得侵入核心顶层字段。
extensions:
  blueprint_state:
    roles:
      - CODE
      - ARCH
      - REVIEW
    turn: USER
    current_batch: L1+K1i
```

## G. Conflict and Absence Register

- Existing working tree changes were observed before capture and were not modified.
- Local HEAD differs from the historical observed remote target; no adjudication performed.
- No credentials, tokens, cookies, keys, or `.env` files were collected.
- No absent requested material detected among the explicit paths; wildcard inventory is limited to located governance candidates.

## H. Phase and Protocol Observations

- Phase 3A / Phase 3B / GOV-BP-P3-01: recorded only from the collected source documents; no state changes performed.
- `blueprint_protocol.md` was collected from the user-level path above.
- Canonical truth, lifecycle state, acceptance snapshot, and generated view labels are preserved as observations, not adjudications.
- Undetermined fields remain `UNKNOWN`.

## I. Post-capture Verification

### git rev-parse HEAD

```text
b7fc77e4d442364e6f5db790b374ece4c5da409d
```

### git status --short --untracked-files=all

```text
 D ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V3-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-1-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-2-End-2E-Handoff.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-1-BlueDesign.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-2-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-3-R2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-4-R3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-5-END-Final-Accept-Phase3-Handoff.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2Z-Final-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3-\346\200\273\350\247\210.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3A-Preview.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3A-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/TruthPack-\346\234\254\345\234\260\346\226\207\346\241\243\345\257\274\345\207\272.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\346\211\247\350\241\214\351\203\250\345\210\206-9.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\347\254\254\344\272\214\351\230\266\346\256\265-2B-End-2C-Preview.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\347\254\254\344\272\214\351\230\266\346\256\265-2C-End-2D-Preview.md"
?? "C\357\200\272UsersSXD-T480SDocumentsWorkSpaceGiteecookbooktempclaudecommit_msg.txt"
?? temp/codex/af13_af14_review_diff.txt
?? temp/codex/generate_truthpack.ps1
?? temp/e.txt
?? temp/err.txt
?? temp/f.txt
?? temp/g.txt
?? temp/r3err.txt
?? temp/review_android_test.txt
?? temp/review_apk_build.txt
?? temp/review_shared_test.txt
?? temp/test_output.txt
```

### git diff --stat

```text
 .../Phase-2D-R1.md"                                | 1360 --------------------
 1 file changed, 1360 deletions(-)
```

### git diff --name-status

```text
D	".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-R1.md"
```

- HEAD unchanged: YES
- Repository files produced by this task: NO
- Commit/push performed: NO (explicitly prohibited by export specification)

## J. Export Integrity

- Filename: `UBF-M0-Truth-Pack-b7fc77e4.md`
- Absolute path: `C:\Users\SXD-T480S\Downloads\UBF-M0-Truth-Pack-b7fc77e4.md`
- File size / line count / SHA-256: reported externally after write.
- Collection: `COMPLETE`
- Unresolved STOP/Q items: `NONE`
- SELF_SHA256_REPORTED_EXTERNALLY
