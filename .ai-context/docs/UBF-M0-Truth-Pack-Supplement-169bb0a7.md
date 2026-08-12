# UBF-M0 Truth Pack Supplement — 169bb0a7

Document Role: Supplemental Evidence and Errata
Status: COMPLETE
Execution Parent: 169bb0a70524c513fd4d2fd1cc72e06cac3ee27d
Original Capture HEAD: b7fc77e4d442364e6f5db790b374ece4c5da409d
Task ID: UBF-M0-REWORK-01

## A. Provenance and Scope

- Truth Pack path: `.ai-context/docs/UBF-M0-Truth-Pack-b7fc77e4.md`.
- Original Truth Pack SHA-256 before revision: `B2C8EBFAA7E64F42EA5EDFC885E0546F66DC43BBA613090D80734AE17B49587B`.
- Original capture occurred before the Downloads export; the user then authorized copying the export into the CookBook repository, followed by commit `169bb0a7`.
- Allowlist: the Truth Pack above and this supplement in the same directory.
- Denylist: protocol, registry, Phase 3 state, business code, tests, build/configuration files, and all paths outside the allowlist.
- This document adds missing evidence and corrections only; it does not mutate input evidence.
- Paths use repository-relative names or `<USER_HOME>` / `<COOKBOOK_REPO>` placeholders.

## B. Corrected Observation Register

| ID | Original Observation | Corrected Fact | Evidence | Effect |
|---|---|---|---|---|
| ERR-01 | Local HEAD and Observed Remote Target were reported as different. | At capture time they were identical: `b7fc77e4d442364e6f5db790b374ece4c5da409d`. | Original Truth Pack §A/§G; capture metadata. | Hash comparison corrected. |
| ERR-02 | Overall collection was `COMPLETE`. | It is `PARTIAL / SUPERSEDED IN PART BY UBF-M0-Truth-Pack-Supplement-169bb0a7.md`. | Original Truth Pack §A; this supplement. | Missing evidence is explicit. |
| ERR-03 | Canonical GC registry and two governance indexes were not collected. | All three required project files are included below. | Section C/D; repository files. | M0 evidence completed. |
| ERR-04 | Phase 3A, Phase 3B, and `GOV-BP-P3-01` states were not fully expanded. | State declarations are listed in Section E from their source files. | Section E. | No state is adjudicated. |
| ERR-05 | `project.yaml` and later Phase 3A records were not compared. | They contain conflicting lifecycle declarations; conflict remains unresolved. | Section F; source hashes/lines. | No source file is changed. |
| NOTE-01 | Downloads and repository paths differ. | This is the authorized transport chain, not original collection overreach. | Original capture provenance and commit `169bb0a7`. | Provenance clarified. |
| NOTE-02 | Working tree was already DIRTY before capture. | Existing dirty entries remain outside this commit and are preserved. | Original Truth Pack §B/§I; current preflight status. | No attribution to this task. |

## C. Canonical Governance Inventory Supplement

| Requested Material | Repository-relative Path | Scope | Truth Role | Status | SHA-256 | Line Count | Notes |
|---|---|---|---|---|---|---:|---|
| `12_多模型协作与实施蓝图规范.md` | `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md` | PROJECT-LEVEL | Project fallback canonical GC registry | FOUND | `44FEE0FDFC55FAAA61B0A599FE35A1F61757921AB8D63B48498A59BF64EBECFC` | 479 | Project-level fallback copy. |
| `INDEX.md` | `.ai-context/docs/experience/INDEX.md` | PROJECT-LEVEL | Experience index | FOUND | `54A252AA2ACB1BBCE0DC1FDD233AF80E43E8226743210C22024ABDF6249DF083` | 55 | Index identifies experience handbook. |
| `README.md` | `.ai-context/project_graph/README.md` | PROJECT-LEVEL | Project Graph entry/documentation | FOUND | `2E8EE6833D5CF672BC62118C41938436FCBC66EA24EFAD500838294B489A4677` | 448 | Project Graph documentation. |

## D. Complete Project-level File Contents

### .ai-context/docs/experience/12_多模型协作与实施蓝图规范.md
- SHA-256: `44FEE0FDFC55FAAA61B0A599FE35A1F61757921AB8D63B48498A59BF64EBECFC`
- Line count: 479

```markdown
# 多模型协作与实施蓝图规范

> 状态：长期有效；2026-08-05 起执行。
> 对象：架构设计模型、任意编码模型（含低能力/快速模型）、审查模型。
> 目标：架构模型冻结所有设计决策；编码模型只完成可机械执行、可自动验收的工作，不得用“合理猜测”补全需求。
> **用户级真相源为 `~/.ai-context/rules/blueprint_protocol.md`（2026-08-07 起，随 GLOBAL.md 三档协作模式改造）。本文件是项目内自带副本，供副机无用户级目录时兜底读取；两份如有出入以用户级为准，发现不一致应同步回填。** 本项目已在 `PROJECT.md` 声明 `协作模式: BLUEPRINT`，握手状态见 `docs/context_memory/BLUEPRINT_STATE.md`。

## 1. 核心定义

- **实施蓝图**：可直接交给编码模型的封闭执行合同，不是解释性方案。它必须使两名不同编码模型得到同一组类型、边界、状态转移、测试和提交范围。
- **决策闭合**：每一处会影响行为、数据归属、并发、异常、兼容、公共 API 或测试证据的选择都已给出唯一答案。
- **机械实现**：编码模型只需在指定文件中创建/替换指定类型、按指定顺序连接数据、填充指定测试夹具和断言；不需选择架构、策略或替代方案。
- **蓝图缺口**：编码模型无法从蓝图确定唯一行为。缺口不是编码模型自行发挥的许可，而是阻断项。

**硬原则：** 未被蓝图显式授权的新入口、新异步任务、新全局状态、新 fallback、新公开 API、新依赖和新持久化写入，一律禁止。

### 全局协议新增 Contract（fallback 语义同步）

- **Semantic Categories**：涉及冻结、状态、迁移、快照或生成时，区分 Stable Identity、Contract / Semantic、Lifecycle State、Acceptance Snapshot、Generated View；Acceptance Snapshot 与 Generated View 均不得冒充独立 Truth。
- **Frozen wording**：`FROZEN` / `FINAL` / `IMMUTABLE` 必须标明冻结的语义类别；当前状态值默认仍可按冻结状态机合法迁移。
- **Mutation Declaration**：Architecture、Migration、REWORK、Freeze、ACCEPT、FINAL ACCEPT、Handoff 蓝图逐项声明五类对象的 MUTABLE / IMMUTABLE / CONDITIONALLY MUTABLE / N/A + reason。
- **REWORK Reopen / Preserve**：每个 REWORK 存在 Verified Defect、Root Cause、Reopen Set、Preserve Set、Exact Repair、Regression Audit、STOP Gate，且默认收窄范围。
- **Blueprint Defect vs Execution Error**：收尾必须归因；Blueprint Defect 进入复发 / 扩容 / 开新级，Execution Error 记录执行偏离。
- **Ownership Evidence**：Source Semantic 加至少一个 authoritative responsibility evidence；实现证据对未实施工作不是强制前置条件，代码路径/关键词不得作为 sole ownership evidence。
- **Canonical Registry Discovery**：创建规范、registry、state ledger 或 experience truth 前先发现既有 canonical registry，并记录 Candidate Path、Existing Role、Authority、Overlap、Final Disposition。
- **Stable Entry / Pointer**：稳定入口只保留 pointer + truth role，不复制 TURN、current batch、current model 等短生命周期值。

本文件是 fallback 副本，要求与用户级协议保持语义一致，不与其建立第二套机制。

## 2. B1/B2 复审暴露的不足与永久对策

| 编号 | 失败类别 | B1/B2 中的表现 | 蓝图必须前置的约束 |
|---|---|---|---|
| BL-01 | 决策未冻结 | 取消边界、HTTP 重试与整体 JSON fallback 留给实现时判断。 | 每个分支写成“条件 -> 唯一动作 -> 禁止动作”，不能只写“正确处理”“注意取消”。 |
| BL-02 | 抽象所有权错误 | 一度出现跨请求的取消控制，破坏每请求独立性。 | 先给出对象生命周期表：创建者、持有者、可调用者、销毁点；未列出的共享状态不得出现。 |
| BL-03 | 单一真相源失守 | 日期锚点与整体 JSON fallback 发生错段、错日期、跨段污染。 | 数据流必须标明每个字段的唯一来源、转换点和禁止覆盖点；fallback 必须复用同一校验入口。 |
| BL-04 | 异步时序未建模 | disconnect 与阻塞 read 的竞态、取消后终态和重试边界不清。 | 所有并发功能必须提供事件时序表、代际隔离规则、取消后的唯一可见结果。 |
| BL-05 | 测试证明了结果而非原因 | fake 自己抛 `CancellationException`，未进入 production `catch(IOException)`。 | 测试夹具只能制造外部原因；断言必须观察生产分支的结果，禁止 fake 直接返回预期终态。 |
| BL-06 | 回归与证据不完整 | 删除既有 parser 回归、弱断言、缓存/历史测试被当作当前通过证据。 | 测试清单须锁定基线用例；删除必须有架构批准和一对一替代映射；证据必须来自当前 commit 的命令输出。 |
| BL-07 | 范围漂移 | 修复时可能顺带改 UI、DI、协议或依赖。 | 蓝图提供“允许文件 + 每文件允许操作 + 明确禁止文件”；审查按 allowlist 拒绝额外改动。 |
| BL-08 | 索引空间隐性耦合 | 2026-08-07 B4/B5 架构复核 AF-B456-05：UI 用 `completedSegments`/`currentSegmentOrdinal` 等标量计数反推列表逐项状态，业务序号（过滤前）与列表下标（过滤后）在有空白项时不等价，致高亮定位错位、成败状态对调；三角色审查一度把此项误判为已修复的阻断。 | 凡“列表逐项状态”必须由数据层直接产出 `List<Status>`（与显示顺序一一对应），UI 层只做映射，禁止用计数/标量 + 下标比较反推每项状态；蓝图的 D 类型表 / E 数据流须为此类状态显式声明为 `List<Status>` 而非计数字段。 |
| BL-09 | 基数掩盖式所有权错误 | 2026-08-06 B3 架构终审 AF-ARCH-02（全项目最严重阻断）：`StreamingMealSession` 的 parser 是构造时创建的单例，B3 固定单段（基数=1）掩盖了问题，B4 一旦多段（基数=N）立即整体失效——测试固定单段，无法暴露。 | 任何“构造时创建、后续被多次迭代复用”的对象或字段（parser/accumulator/缓存/累积状态），蓝图必须显式回答“迭代基数从 1 扩展到 N 时是否需要按基数分片”，不能靠“当前基数恰好是 1”掩盖；对象生命周期表（BL-02 工件）须标注其基数假设。 |
| BL-10 | 高频事件节流未定 | 2026-08-06 B3 架构终审建议 S2 → 2026-08-07 B5 落地时 R-10 指出“防抖从未真正实现”：同一决策缺口从建议阶段拖过两个批次仍未补齐，只停在“建议”层级不足以防住。 | 高频异步事件（如流式 Delta）若可能触发 IO/DB 查询等高开销操作，蓝图必须显式定义触发时机与节流/去重策略，并配不变量+测试锁定，不能只在建议表里记一笔就当已处理。 |
| BL-11 | 文档腐化（注释/KDoc 与实现不同步） | 累计四次独立命中：B1/B2 死代码注释（`NdjsonEvent` 文件头描述与实现相反）×2、B3 架构终审 AF-ARCH-01（Prompt 承诺的 `done` 事件未被消费，注释未警示）、B5 `CharCountLabel` KDoc 与实现不符（R-06）、`AF-B456-07`（`periodSelectedRange` 声称已加注释实际未改）。已达“两次遗漏升级”门槛的两倍，判定为最高频复发类别之一。 | 复核逐条比对注释/KDoc 与当前实现是否一致；不符即登记（不要求必然阻断，但必须登记进台账，不得放过）；交付台账不采信 commit message 自述，须逐行 diff 核对声称完成的项。 |
| BL-12 | 状态值域覆盖不全（借位表达） | 2026-08-07 二次复核 `AF-B456-05`：CODE 按字面关闭 GC-17（`segmentStatuses: List<StreamSegmentState>`），但 `StreamSegmentState` 只有 `STREAMING/COMPLETED/FAILED` 三值，无法表达"尚未开始"这个真实存在的第四态；实现把"未开始"兜底成 `STREAMING`（借用已有值代替缺失值），导致周期记多段场景下未轮到的段和真正在流的段同时显示"脉冲中"——**满足了 GC 的字面类型签名，但值域本身覆盖不了真实状态空间，是比 BL-08（索引数学算错）更隐蔽的一种"表面合规"**。 | 凡"数据层产出 List&lt;Status&gt; 供 UI 1:1 映射"类修复，交付前必须先列出真实状态空间的全部可区分值（含"尚未开始/不适用"等边界态），核对承载类型（枚举/密封类）的值域基数是否 ≥ 真实状态数；不足则必须显式加值（如 `List<Status?>` 用 `null` 表达缺失态，或扩展密封类），不得用现有值兜底代替缺失值；若约束"禁止改动某枚举"（如本项目 `StreamSegmentState`），须在 UI 承载层（非领域枚举本身）引入可空/包装类型解决。 |

这些分类是后续复审的固定标签。新问题先归类；若不能归类，先扩充本表和蓝图模板，再进入下一实现批次。

**BL-03 补充判据（2026-08-07 B4/B5 复核新增）**：新增与既有字段语义重叠的 state 字段时，若只迁移读取方而不迁移全部写入方，即构成“并存无同步保证”，直接判 BL-03（禁止，见硬原则）。审查时先 grep 旧字段的全部写入点，再核实新字段是否真的处处同步。若项目内已存在“编辑即失效”一类的唯一收口函数（如本项目 `invalidateGenerationToInput`），新增/修改任何编辑类入口方法时必须显式核对是否路由过它——此判据由 B3（`AF-B3-03`）→ B4（`AF-B456-01`）**同一 bug 跨批次复发一次**直接证明必要性，是目前唯一有两次独立复发实证的模式，颗粒度登记为 `GC-27`（见 §12）。

## 3. 三方职责与权限

| 角色 | 只负责 | 不得负责 | 交付物 |
|---|---|---|---|
| 架构模型 | 需求澄清、接口/数据/状态/时序/测试设计、范围冻结、验收裁决。 | 把未决设计转嫁给编码模型。 | 实施蓝图、验收矩阵、问题反馈单。 |
| 编码模型 | 按蓝图实现、补齐指定测试、报告事实和缺口。 | 改变契约、添加“更灵活”分支、删回归、替换测试策略。 | 最小代码 diff、测试、证据台账、缺口单。 |
| 审查模型 | 将 diff 和运行结果逐项映射回蓝图，发现新类别时升级蓝图。 | 根据代码事后认可未在蓝图内的设计。 | 通过/阻断结论、AF 问题单、蓝图改进记录。 |

编码模型遇到任何一个未闭合点，必须停止代码修改，在台账新增 `Q-<批次>-NN`：`定位 / 可见事实 / 缺失决策 / 受阻操作`。它不得自行选择方案，也不得以“兼容”“兜底”“防御性代码”为理由扩展范围。

## 4. 每个标准/深度任务必须交付的蓝图包

蓝图包可以是一份功能文档中的固定章节，也可以是同目录独立文件；必须包含下表全部工件，缺一项不得标记 `BLUEPRINT_READY`。

| 工件 | 必填内容 | 判定标准 |
|---|---|---|
| A. 任务卡 | 目标、非目标、风险级别、现状版本、批次边界、前置条件、**上一批延后项清单的归宿（每项须显式转为本批 ID 或标注“显式弃置+理由”，不得只留指针）**。 | 能判断本批是否该开始；延后项清单不得有“无归宿”条目。 |
| B. 事实地图 | 相关代码路径、当前真相源、已有契约/ADR、被替换和保留的路径。 | 不以文档猜测代替代码事实。 |
| C. 不变量表 | `INV-ID / 条件 / 必须结果 / 禁止结果 / 自动证据`。 | 每条至少有一个测试或明确真机项。 |
| D. 类型与 API 表面 | 新/改类型的包路径、可见性、字段、默认值、调用方向、是否持久化。 | 不存在“自行定义字段/接口”的空间。 |
| E. 数据流与真相源 | 输入、快照、转换、输出、唯一所有者、清理点。 | 每个关键字段只标一个写入所有者。 |
| F. 状态机与时序 | 状态集合、触发器、允许/拒绝转移、并发代际、取消与异常路径。 | 状态不能靠 UI 文案或布尔组合猜测。 |
| G. 实施脚本 | 文件 allowlist、每文件精确操作顺序、禁止项、完成后文件形态。 | 编码模型可逐项勾选。 |
| H. 测试矩阵 | 测试 ID、前置、刺激、可观察结果、精确断言、夹具职责、禁止伪证。 | 每个 INV 与至少一个测试 ID 双向关联。 |
| I. 交付台账 | 命令、当前 commit 输出、测试数、人工/真机项、Q/AF 状态。 | 不用历史 XML、缓存结果或口头描述代替。 |

## 5. 蓝图书写语法

蓝图中所有可执行要求使用下列形式，避免“适当”“必要时”“正确地”“尽量”等歧义词：

| 字段 | 含义 |
|---|---|
| `ID` | 稳定编号，例如 `INV-B3-04`、`T-B3-07`。 |
| `Owner` | 唯一持有写权限的类型或层。 |
| `While` | 前置状态（2026-08-07 借鉴 Kiro/EARS 记法新增）。同一 `Do` 若有多个互斥前置状态须各开一行，禁止只写常见态、其余隐式落默认分支（见 `blueprint_protocol.md` §2）。 |
| `When` | 精确触发条件或事件。 |
| `Input` | 类型、来源与是否为不可变快照。 |
| `Do` | 唯一动作，包含调用顺序和结果落点。 |
| `Must not` | 明确列出不得执行的替代动作。 |
| `Evidence` | 测试名、断言字段、构建命令或真机步骤。 |

示例语义：`When=取消 generation A 后收到 A 的 Delta；Do=丢弃且不更新 state；Must not=创建预览、更新 generation B、发终态；Evidence=T-B3-06`。这比“防止旧事件影响新会话”可执行。

`While` 字段示例（`AF-B456-05` 教训）：段状态渲染这条 `Do` 只写了 `While=该段已被 nextSegment() 触达`，漏了 `While=该段尚未被触达` 这一互斥前置态，于是"未开始"被隐式落进了默认分支、错误复用了 `STREAMING` 值。两个 `While` 都显式开一行，蓝图冻结时才能一眼看出值域是否覆盖完整（详见 `GC-36`/`GC-37`）。

## 6. 架构冻结门禁

架构模型交付前逐项自检；任一项为否，必须先补蓝图而非交给编码模型。

1. 每个新类、字段、方法、可见性与所在层均已指定。
2. 每个现有类是保留、替换还是仅编译适配已指定。
3. 每个数据字段有唯一真相源和不可再写边界。
4. 每个状态都有允许进入、退出、取消、失败和可见 UI 语义。
5. 每个并发请求有 generation/会话隔离、串并行策略与资源释放点。
6. 每个 fallback 与主路径共用的校验/转换入口已经写明。
7. 每个异常按“取消、可重试、不可重试、局部诊断”分类，且终态数量明确。
8. 每个测试都能制造原因并观察 production 后果。
9. 已锁定回归用例，不允许为获得绿灯删除或弱化断言。
10. allowlist 外无必要修改；如有，先回到蓝图修改并重新冻结。
11. 当前批可在不依赖下一批 UI/数据库工作的情况下构建、测试和验收。
12. 人工/真机项与自动化项分离，且前置条件、步骤、预期、失败判定都已写出。
13. **存在一份独立挑战台账**（`GC-37`，2026-08-07 评估主流 spec-driven 框架后新增）：架构模型交付前，须换一次独立视角（另一次会话/独立 agent 均可，只给蓝图本身、不给设计理由）重新挑战本蓝图，尤其是逐条核对每个"数据层产出 `List<Status>`"类字段的承载类型值域是否覆盖真实状态空间（呼应第 4 条，但第 4 条是程度命题"都有语义"，本条要求可 grep 核实的挑战产出物存在）。台账含挑战方标识、挑战项清单、每项裁决；无此台账不得转 `BLUEPRINT_READY`。

## 7. 编码模型执行合同

1. 先读本规范、当前任务蓝图、任务卡指定的代码和 ADR；只读这些材料足以实现本批。
2. 严格按“共享纯模型/适配器 -> 编排 -> UI 编译适配 -> 测试 -> 文档台账”顺序；不得从 UI 反向创造协议或业务规则。
3. 只创建蓝图列出的类型。若必须补一个私有辅助函数，名称、位置、输入输出也必须已在实施脚本中列出；否则发 `Q`。
4. 不得引入 `Any`、未类型化 `Map`、跨层 JSON 字符串、全局可变取消器、脱离作用域的协程或隐式系统日期。
5. 不得更改 DI、数据库、依赖版本、公开 API、日志口径、协议版本，除非蓝图将该文件和操作逐项授权。
6. 每做完一个步骤即运行该步骤指定测试；最终只提交 allowlist 内的代码、测试和必要文档。
7. 交付必须逐条填写 `测试 ID -> 结果 -> 命令 -> 当前 commit 证据`；无证据写“未验证”，不能写“通过”。

## 8. 状态机、数据流与异步的强制表达

### 8.1 状态机

- 用一个 sealed/enum 阶段字段表达互斥主状态；不能以多个布尔值拼出“正在生成但可保存”等未定义组合。
- 每条转移都写作 `来源状态 + 事件 -> 目标状态 + 原子状态更新`。未列出的转移一律拒绝且无副作用。
- `generationId` 必须由会话创建者单调生成；输入快照、分段、parser、状态更新均携带同一 ID。
- 旧 generation 的事件、失败、完成、取消不得写入当前 generation；取消不产生 `Failed` 或 `Completed`。
- 所有流/Job 的创建点、取消点和 finally 清理点必须在时序图中出现。

### 8.2 数据流

- 输入在会话开始时冻结为不可变快照；后续编辑只能创建下一 generation，不能修改进行中的请求。
- 每一层只能转换自己的数据：网络只产出 `LlmStreamEvent`，协议层只产出草稿/诊断，编排层只调用预览，持久化只接受用户确认后的 preview。
- fallback 必须先转换为主路径内部类型，再通过主路径校验；禁止产生主路径不存在的临时 ID、日期或归属规则。
- 原始输入、完整响应、Key 和服务端 body 不得进入可持久化 UI state、Release 日志或异常文本。

### 8.3 测试夹具

- fake 仅模拟外部系统：网络字节、时钟、仓库结果、取消触发。它不得直接抛出/返回业务层期望事件来替代 production 分支。
- 异步测试使用 latch/channel/受控 dispatcher 表明“已到达某阶段”；禁止 `sleep`、真实公网、随机端口或依赖调度偶然性。
- 每条重试测试断言 call 数、实例不同性、事件完整顺序和终态次数；每条取消测试断言无终态及资源取消效果。
- 每个 parser/mapper 测试断言关键键、日期、归属和数量，不以“非空”“包含某类型”作为唯一证据。

## 9. 批次状态与审查协议

`DRAFT -> BLUEPRINT_READY -> IMPLEMENTING -> SELF_CHECKED -> REVIEWING -> ACCEPTED`

- `DRAFT`：需求/事实尚未闭合。
- `BLUEPRINT_READY`：已通过第 6 节门禁，编码模型可以开始。
- `IMPLEMENTING`：只允许蓝图内操作。
- `SELF_CHECKED`：编码模型提交测试与台账；此状态不代表通过。
- `REVIEWING`：审查模型逐项对照不变量、allowlist 和当前命令结果。
- `ACCEPTED`：全部自动项通过；真机项已进入唯一清单或已完成。

审查结论只能是 `通过 / 阻断 / 缺证据`。阻断项使用 `AF-<批次>-NN`，必须包含：违反的 `INV/T`、文件定位、复现/证据、唯一最小修复、必须新增或恢复的测试、禁止扩大范围。不得只写“加强测试”“优化结构”“再检查一下”。

## 10. 长期进化机制

每次审查结束，架构模型执行以下闭环：

1. 将每个 AF 归入 BL-01 至 BL-07；新类别新增 `BL-XX`。
2. 判断根因是“蓝图缺失、编码偏离、测试伪证、环…5988 tokens truncated…量 + 下标反推逐项状态 | BL-08 / `AF-B456-05`：`index < completedSegments -> DONE` 隐含"成功项必排在失败项前"，成败天对调 | D / E | 0 |
| GC-18 | 每个整型序号字段在类型表标注索引空间（业务序号 / 过滤前下标 / 显示下标）+ 取值域；命名后缀约定 `*Ordinal`=业务序号、`*Index`=显示下标；跨空间使用须写出转换表达式 | `AF-B456-05`：`currentSegmentOrdinal ∈ {2,4}` 与圆点 `index ∈ {0,1}` 恒不相等，ACTIVE 点永不出现 | D | 0 |
| GC-19 | 凡集合经过滤 / 排序 / 分组后再被消费，数据流须画出 `原集合 --filter--> 子集 --sort--> 显示序` 链，并声明唯一 UI 消费对象 | `AF-B456-05`；同类：`nonBlankSegments` 过滤后未重编号 | E | 0 |
| GC-36 | 交付"数据层产出 `List<Status>`"类修复前，须先列出真实状态空间的全部可区分值，核对承载类型（枚举/密封类）值域基数是否覆盖；不足时在 UI 承载层加可空/包装类型表达缺失值，禁止用值域内现有值兜底代替缺失值（BL-12） | 二次复核 `AF-B456-05`：`segmentStatuses: List<StreamSegmentState>`（3 值）无法表达"尚未开始"，`states[id] ?: StreamSegmentState.STREAMING` 把未开始兜底成 STREAMING，多段场景下未轮到的段和真正在流的段同时显示 ACTIVE，`DotState.PENDING` 变死代码 | D | 0 |

#### L6 · 用户可见副作用闭合

| GC | 强制细化规则 | 触发案例 | 工件 | 复发计数 |
|---|---|---|---|:--:|
| GC-20 | 存在自动副作用清单表：凡系统自动执行的 截断/丢弃/纠正/降级/合并/静默跳过，逐条给出 `触发条件 / 用户可见载体 / 文案原文 / 去重与复位规则 / 透明 Tier（T0~T3）`；判"无需告知"的必须写理由 | `AF-B456-04`：三条截断路径全静默，全仓 grep 无"已截取"文案 | C / G | 0 |
| GC-21 | INV 的"必须结果"中出现"并提示 / 并告知 / 可见"字样的，实施脚本必须有对应 STEP 指明提示的代码落点（文件 + 分支 + 宿主组件）；只在 INV 写"提示"而脚本无落点 = 蓝图未完成 | `AF-B456-04` 精确根因：INV-B4-05/06 与 §5.1 都写了提示，但原 §7 步骤 3 无任何一条"截断分支弹 Snackbar"的机械步骤 | C / G | 0 |
| GC-22 | 每条可见副作用配 ≥1 个 T-ID 或真机项编号（进 GC-05 的双向映射表一起校验） | `AF-B456-04`：E-B4-03 ③ 写了但无人验，B6 又漏登记 | H / I | 0 |
| GC-32 | 高频异步事件（如流式 Delta）若可能触发 IO/DB 查询等高开销操作，蓝图必须显式定义触发时机与节流/去重策略，不能让"相同值不重复"当唯一防线，且须配不变量+测试锁定 | BL-10：B3 架构终审建议 S2 → B5 落地时 R-10 指出"防抖从未真正实现"，同一决策缺口从建议阶段拖过两批仍未补 | C / H | 1（已升审查必查） |

#### L7 · 脚本可勾销闭合

| GC | 强制细化规则 | 触发案例 | 工件 | 复发计数 |
|---|---|---|---|:--:|
| GC-23 | 实施脚本每个最小动作独立编号 `STEP-<批次>-<n.m>`，含 `文件 / 定位（函数名或行区间） / 动作 / 完成后形态（可比对的字面量或断言级描述）` | `AF-B456-09`：§7 步骤 3.4「标题跟随模式」是散文子项，被整条跳过且无人察觉 | G | 0 |
| GC-24 | 交付台账含 STEP 勾销表：`STEP-ID / 状态 / 落地 commit / diff 定位`；审查按此表逐条 diff 复核，不采信 commit message 自述 | `AF-B456-07`：`63fd3fec` 勾了"✅ periodSelectedRange 注释"实际未落地；`AF-B456-05` 假修复被误判关闭；2026-08-07 二次复核第 3 次命中：`234539aa` 的 STEP 勾销表 Evidence 列引用 `T-B5-01/02/03`、`T-B4-08/09/10` 等测试 ID，但 `git show` 确认该 commit 未触碰任何测试文件，引用的测试实际不存在 | I | 1（已达两次遗漏门槛，转"审查必查"：交付台账凡引用 T-ID/E-ID 作为 Evidence，审查须先 grep 该测试是否真实存在，再采信） |
| GC-25 | 凡 STEP 的完成形态是字面量（文案 / 常量 / 枚举值 / 命名），蓝图直接写出目标字面量原文，并注明审查判据 `grep "<字面量>" <文件>` | `AF-B456-09`：`if (QUICK) "AI 快捷记" else "AI 快捷记"` 两分支相同的死条件，一次 grep 即可现形 | G / I | 0 |
| GC-26 | 存在冻结值修订记录表：任何被蓝图冻结的阈值/常量/模型参数变更，须补 `日期 / commit / 依据证据（日志片段或复现输入） / 旧值→新值 / 影响评估`；无记录即判缺证据 | 复核报告 §3.2：`maxTokens` 2048→4096 推翻 B4 §1 冻结值，只说"日志已确认"无片段可反查 | A / D | 0 |

#### R1 扩容条款（GC-38~GC-47）

| GC | 强制细化规则（存在性命题） | 触发案例 | 工件 | 复发计数 |
|---|---|---|---|:--:|
| GC-38 | 每个 REWORK 蓝图存在 Verified Defect、Root Cause、Reopen Set、Preserve Set、Exact Repair、Regression Audit、STOP Gate；Reopen Set 与 Preserve Set 分别列出具体对象或文件/实体集合 | Project Graph Phase 2E R1/R2/R3 局部缺陷收窄 | A / G | 0 |
| GC-39 | Architecture / Migration / REWORK / Freeze / ACCEPT / FINAL ACCEPT / Handoff 蓝图存在 Mutation Declaration，逐项声明五类对象的可变性 | Phase 2 Final 冻结语义扩大 | A / D | 0 |
| GC-40 | 每个非机械 ownership/mapping 决策存在 Source Semantic + 至少一个 Authoritative Responsibility Evidence；候选 owner >1 时存在 disambiguation evidence 或 blocker | Over-No-Guess 与 Fuzzy Ownership | B / E | 0 |
| GC-41 | 涉及 ID 集合、覆盖率、migration source rows、duplicate/dangling/collision 或实体 counts 时，蓝图存在 programmatic recount/audit command 和实际输出落点 | Phase 2D 人工统计不闭合 | H / I | 0 |
| GC-42 | 新增规范、registry、state ledger 或 experience truth 文件前，蓝图存在 Canonical Registry Discovery 表：Candidate、Existing Role、Authority、Overlap、Final Disposition；已有同职责 registry 时只能 REUSE、MERGE 或 explicit replacement | `ae372d2` 重复创建项目 registry | A / G | 0 |
| GC-43 | 稳定入口/导航文档存在 Truth Role / Pointer 表；非唯一 Truth Source 的短生命周期状态只允许 pointer，不复制当前值 | PROJECT.md 长期复制 TURN 与当前批次 | A / G | 0 |
| GC-44 | 涉及 Acceptance Snapshot、Generated View 或 Handoff View 时，蓝图存在 Authoritative Truth Source、Derived/Generated 标识、Human Edit Policy、Regeneration / Update Authority | Project Graph Phase 2 Final / Phase 3 Views | E / G | 0 |
| GC-45 | 涉及 Project Graph WorkItem / Verification lifecycle 时，蓝图存在 Implementation State × Required Verification State × Current Status × Expected Status × Evidence 表，并明确 Plan completed ≠ WorkItem done | 已实现工作曾被写为 backlog | D / I | 0 |
| GC-46 | 存在 source identity 与 normalized/internal identity 时，蓝图存在 Source Identity Layer、Target Identity Layer、Deterministic Mapping Rule、1:1 Mapping Audit、Duplicate Audit、Collision Audit，并明确 normalization ≠ rename | F4-1 → E-F4-01 | D / E | 0 |
| GC-47 | 每个 AF / blocker / REWORK review report 存在 Error Attribution、Existing GC Coverage、Blueprint Escape Analysis、Multi-Dimension Improvement Review、Granularity Review、Scale Review、Cross-Validation Review、Blueprint Architecture Action、Future Blueprint Propagation、Over-design Check、Feedback Branch、Registry Action、Evidence 固定小节 | GOV-BP-P3-01：仅 repair-only feedback 无法反哺 Blueprint 设计 | I | 0 |
| GC-48 | 新增、修改或冻结治理规则后，蓝图中存在六列 Self-Application Audit（New / Modified Rule、Affected Governance Files、Self-Check Target、Compliance Result、Violation、Disposition）；当规则涉及 Granularity/Scale/Cross-validation/Propagation 时，目标同时覆盖当前治理文件、canonical stable entry/state/registry、新规则直接约束的当前 Blueprint 与明确受影响的 future Blueprint template/active phase design；denylist future Blueprint 只登记传播缺口，不直接修改 | R1 `PROJECT.md`/`BLUEPRINT_STATE` 自身副本问题；GOV-BP-P3-01 扩展到治理文件与未来模板的自应用 | A / I | 0 |

#### R2 Self-Application Audit（GC-48）

| New / Modified Rule | Affected Governance Files | Self-Check Target | Compliance Result | Violation | Disposition |
|---|---|---|---|---|---|
| Stable Entry / Pointer Rule | `.ai-context/PROJECT.md` | current TURN、current batch、lifecycle snapshot、temporary blocker、concrete implementation model | PASS | none | 仅保留 Project Graph、BLUEPRINT_STATE、SESSION 和模型评估 canonical pointer |
| Model Evaluation Canonical Owner | `.ai-context/docs/context_memory/BLUEPRINT_STATE.md` | concrete model-name table、model evaluation duplicate、14_模型执行力 pointer | PASS | 历史模型表已删除，pointer 已保留 | 模型事实统一由 `docs/experience/14_模型执行力评估.md` 持有 |
| Canonical GC Registry | `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md` | registry count、GC-01~GC-48、duplicate GC | PASS | none | canonical registry 新增 GC-48，复发计数 0 |
| Project Truth Pointer | `.ai-context/PROJECT.md` / `.ai-context/project_graph/` | projectReview 是否冒充 Project Truth | PASS | none | Project Graph 保持 Project Truth owner |

#### R3 Registry Count Audit

| 检查项 | 结果 |
|---|---:|
| Actual GC Entity Count | 48 |
| Unique GC IDs | 48 |
| Range | GC-01 ~ GC-48 |
| Missing | 0 |
| Duplicate | 0 |
| Current Baseline Label | 48 |
| GC Registry Section Label | 48 |
| Final Summary Label | 48 |
| Count Label Conflict | 0 |

本轮对 GC-48 的判定粒度完成细化：Self-Application Audit 除检查 authoritative entity set 的实体数量外，还必须检查所有 active derived labels 是否一致。本次为 existing GC refinement，不是 GC expansion。

### 12.4 §0.1 勾销表模板（供各蓝图直接复制）

```markdown
### §0.1 颗粒度勾销表（GRANULARITY = L<k>）

| GC | 条款一句话 | 本蓝图落点 | 状态 |
|---|---|---|:--:|
| GC-<nn> | ... | ... | 满足 / N/A：<理由> / 未满足 |
```

任一条`未满足`未被处理，不得标 `BLUEPRINT_READY` 或改回 `ACCEPTED`。

---

## 13. 颗粒度升级历史

| 日期 | 触发 AF/事故 | 分支 | 动作 | 新增 GC | 级别 | 影响工件 | 判定人 | 关联报告 |
|---|---|:--:|---|---|:--:|---|---|---|
| 2026-08-07 | BL-01/BL-07（B1/B2 复审追溯） | — | 建立基线 | GC-01~04 | L1 | A/C/G | ARCH | 12_…规范.md §2 |
| 2026-08-07 | BL-05/06 + `AF-B456-06/08` | ③ | 开 L2 | GC-05~09 | L2 | C/H/I | ARCH | 复核报告 §二 |
| 2026-08-07 | `AF-B456-01/02` | ③ | 开 L3 | GC-10~13 | L3 | D/E | ARCH | 复核报告 §二 AF-01 |
| 2026-08-07 | `AF-B456-03` | ③ | 开 L4 | GC-14~16 | L4 | D/F/G | ARCH | 复核报告 §二 AF-03 |
| 2026-08-07 | `AF-B456-05` | ③ | 开 L5 | GC-17~19 | L5 | D/E | ARCH | 复核报告 §二 AF-05 |
| 2026-08-07 | `AF-B456-04` | ③ | 开 L6 | GC-20~22 | L6 | C/G/H | ARCH | 复核报告 §二 AF-04 |
| 2026-08-07 | `AF-B456-07/09` + 缺证据§3.2 | ③ | 开 L7 | GC-23~26 | L7 | G/I | ARCH | 复核报告 §二/§三 |
| 2026-08-07（第二轮·B1-B3 结论回溯，不新开级） | `AF-B3-03`→`AF-B456-01` 同一 bug 两批复发 | ①/② | L3 扩容 | GC-27 | L3 | D/E | ARCH | B3 蓝图 §8.2 AF-B3-03 |
| 2026-08-07（第二轮） | B3 架构终审 `AF-ARCH-02`（全项目最严重阻断，回溯建 BL-09） | ② | L4 扩容 | GC-28 | L4 | D/E | ARCH | B3 蓝图 §11.2 |
| 2026-08-07（第二轮） | B3.1 `AF-B3-06`（mapper 覆盖）+ `AF-B3-01/04/05`（分支副作用不完整） | ② | L1 扩容 | GC-29, GC-30 | L1 | E/C/G | ARCH | B3 蓝图 §8.2 |
| 2026-08-07（第二轮） | B3.2 `AF-B3-R2-01` + B3.3 `AF-B3-R3-01`（挂起点恢复未重校验，两次复现） | ①/② | L4 扩容 | GC-31 | L4 | F | ARCH | B3 蓝图 §9/§10 |
| 2026-08-07（第二轮） | B3 架构终审建议 S2 → B5 R-10 未落地（回溯建 BL-10） | ② | L6 扩容 | GC-32 | L6 | C/H | ARCH | B3 蓝图 §11.3；B5 建议 R-10 |
| 2026-08-07（第二轮） | B3 架构终审建议 S5（测试后门反模式） | ② | L4 扩容 | GC-33 | L4 | D | ARCH | B3 蓝图 §11.3 |
| 2026-08-07（第二轮） | 四次独立命中（B1/B2×2 + `AF-ARCH-01` + B5 `CharCountLabel` + `AF-B456-07`，回溯建 BL-11） | ①/② | L2 扩容 | GC-34 | L2 | I | ARCH | B3 蓝图 §11.4；复核报告 §二 AF-07 |
| 2026-08-07（第二轮） | B3 架构终审 `AF-ARCH-01`（协议双端未对齐） | ② | L3 扩容 | GC-35 | L3 | E/C | ARCH | B3 蓝图 §11.2 |
| 2026-08-07（第三轮·B4 蓝图二次复核） | `AF-B456-05` 二次复核：字面关闭 GC-17 后仍复发，根因是值域覆盖不全（借位表达，回溯建 BL-12） | ② | L5 扩容 | GC-36 | L5 | D | ARCH | 复核报告 §八 8.2 |
| 2026-08-07（第三轮） | STEP 勾销表 Evidence 引用不存在的测试 ID（`T-B5-01~03` 等），GC-24 第 3 次命中同类证据不实 | ① | GC-24 复发计数 0→1，转"审查必查" | — | L7 | I | ARCH | 复核报告 §八 8.4 |
| 2026-08-07（第四轮·主流框架借鉴评估，Opus 讨论） | `AF-B456-05` 二次复核暴露"一次复核=设计者本人，无对抗视角"；评估 BMAD/Kiro/Spec Kit 后，仅采纳"独立挑战台账须存在"这一存在性命题，不采纳 BMAD 12 角色人格 | ② | L1 扩容 | GC-37 | L1 | A | ARCH | §10.y；`12_…规范.md` §6 门禁13 |
| 2026-08-11 | Project Graph / Blueprint Governance review lessons | ② | L1~L7 扩容，不新开级 | GC-38~47 | L1/L2/L3/L4/L5/L7 | A/B/D/E/G/H/I | ARCH | Blueprint-Governance-Upgrade-V2-R1.md |
| 2026-08-11 | Blueprint Governance R2 self-application gap | ② | L7 扩容，不新开级 | GC-48 | L7 | A/I | ARCH | Blueprint-Governance-Upgrade-V2-R2.md §19 |

**当前基线：L7 · 48 条 GC · 5 条已升"审查必查"（GC-03/24/27/31/32）· 1 条已升"自动检查建议"（GC-34，复发计数 3）· GC-38~48 复发计数均为 0；不新增 L8。**

---

## 14. 模型名记录与跨模型能力评估（2026-08-07 第四轮确立）

> **背景**：模型执行力数据需要独立保存，避免把短生命周期模型信息写入握手状态。实际模型名、执行行为和能力观察的 canonical owner 是 `docs/experience/14_模型执行力评估.md`；本规范只引用该事实源，不在 BLUEPRINT_STATE 或稳定入口复制。

### 14.1 记录位置与格式

| 记录位置 | 内容 |
|---------|------|
| `docs/experience/14_模型执行力评估.md` | 实际模型名、角色、commit、执行行为、ARCH 简评和跨模型能力观察 |
| `BLUEPRINT_STATE.md` | 只记录抽象 role + machine，不记录具体模型名 |
| `SESSION_交接.md` | 可 pointer 到模型评估事实源，不重复完整模型观察 |

### 14.2 跨模型能力评估维度（ARCH 审查时校准）

不同模型可能有系统性偏向，ARCH 审查时应读取 `docs/experience/14_模型执行力评估.md` 中已有证据，再按已观察倾向加严对应维度。模型名称、行为观察和跨模型能力数据不在本规范中复制。

### 14.3 为什么必须记录

1. **ARCH 校准审查策略**：根据模型评估事实源中的已验证倾向，重点加严对应 GC，而不是无证据地对所有维度一律加严
2. **跨模型能力数据积累**：多项目、多模型、多轮次的数据汇集后，可形成"模型-缺陷类型"关联矩阵，指导未来模型选型与蓝图编写策略
3. **蓝图编写策略**：根据已验证的模型倾向决定是否需要穷尽完成形态字面量或增加禁止动作列
4. **交接透明**：后续接手的人（无论 ARCH 还是 CODE）能一眼知道"上一轮是谁干的、有什么坑"

### 14.4 首次记录触发点

本规则自 2026-08-07 第四轮起生效。后续模型执行记录统一写入 `docs/experience/14_模型执行力评估.md`；BLUEPRINT_STATE 只保留抽象 role + machine。

### 14.5 与已有规则的关系

本规则不新增 GC（不是颗粒度条款），是协作协议的补充。不改变 TURN 机制、BLUEPRINT_STATE 握手、GC 分级体系。模型能力 Truth 只由 `14_模型执行力评估.md` 持有。

### 14.6 Blueprint Improvement Review 长期闭环（GOV-BP-P3-01）

治理缺陷的固定传播链为：

```text
Defect → Pattern → Granularity Review → Scale Review → Cross-Dimension Review
→ Automation Review → Over-design Check → Blueprint Architecture Action
→ Propagation → Self-Application
```

每个 AF、blocker、REWORK review 或 cross-validation failure，除精确修复外，必须记录 Error Attribution、Blueprint Escape Analysis、Granularity/Scale/Horizontal/Vertical Review、Evidence Independence、Truth/Ownership、Automation、Temporal/Propagation、Over-design、Blueprint Architecture Action、Future Blueprint Propagation、GC Registry Action 和 Evidence。该表即 **Multi-Dimension Review / Cross-Dimension Review**；Escape 分类沿用用户级 canonical protocol；不能以“已修复”代替设计反哺。

规模按 `S0 Defect Object → S1 Sibling Objects → S2 Semantic Category → S3 Current Batch → S4 Cross-Phase → S5 Project-Wide → S6 Global Protocol` 逐层评估；升级必须有共同根因、语义契约、数据/所有权模式、蓝图模板模式或既有同类失败证据。更细或更大不是默认正确，只有提高可判定性、缺陷发现率、可复现性、自动化能力或跨批一致性才纳入；否则缩回范围。

### Cross-Validation Contract（CV-1~CV-4）

交叉验证固定执行 `CV-1 Evidence×Evidence`、`CV-2 Requirement→Evidence`、`CV-3 Evidence→Requirement`、`CV-4 Object-level + Category/Scale-level`。独立挑战必须从 canonical protocol + 本项目 GC registry + 当前 Blueprint 生成 coverage audit，不得只复核 Blueprint 自列清单。Self-Application 还要检查当前治理文件、canonical stable entry/state/registry、当前 Blueprint 及明确受影响的 future Blueprint/template；denylist 文件只登记传播要求，不越权修改。

Architecture Action Enum：`NONE`、`STRENGTHEN_EXISTING_RULE`、`STRENGTHEN_BLUEPRINT_TEMPLATE`、`ADD_PROGRAMMATIC_GATE`、`EXPAND_AUDIT_SCOPE`、`PROPAGATE_TO_FUTURE_BLUEPRINTS`、`GC_EXPANSION_CANDIDATE`。Over-design Check 必须确认扩展不制造第二 Truth、重复 Registry、无意义 N/A 或不可判定的 checklist。

### 14.7 治理批次身份与证据落点（GC-02 / GC-06）

治理批次必须在审计工件中分开记录 `Design Baseline`、`Execution Parent`、`Initial Delivery`、`Review Target`；allowlist diff 只能相对 `Execution Parent` 判定。所有 parity、coverage、count、matrix、audit 结果必须可从 Review Target 重跑，或以结构化表落入本批 `GOV_*_AUDIT.md`，不能只写摘要到 `BLUEPRINT_STATE.md`。

用户级 canonical file 发生 mutation 时，必须记录路径、Truth Role、执行机器、审查者直达性、SHA-256 前后值、变更方法与语义条款、项目 fallback 映射、本地 parity 命令、持久化落点和下一批 SHA 连续性门禁。审查者无法直接读取时标记 `REMOTE_ATTESTED_EXTERNAL_STATE`，不得标记为直接审查。

### 14.8 Canonical Sibling Entry Scan（GC-48）

每次治理规则 mutation 后，Self-Application 必须扫描同语义关键词、workflow entry、gate、template、lifecycle closure 的 sibling entry，并在六列表中记录当前治理文件、canonical stable entry/state/registry、当前 Blueprint 及明确受影响的 future Blueprint/template。denylist 文件只登记传播缺口，不直接修改；GC 总数保持 48，禁止借此新增 GC-49 或 L8。


```

### .ai-context/docs/experience/INDEX.md
- SHA-256: `54A252AA2ACB1BBCE0DC1FDD233AF80E43E8226743210C22024ABDF6249DF083`
- Line count: 55

```markdown
# AI智能体经验手册 - 索引

> 本手册由 /zongjie 指令自动维护，记录项目开发过程中的关键经验和知识。
> 拆分为多个主题文件，便于检索和维护。

## 元信息

- 上次总结会话点：2026-07-14下午/晚 营养素体系(ingredient_nutrition表+measurement_unit.grams克当量19.sqm+NutritionCalculator/Repository/Balance+专用seed文件ingredient_nutrition.json 65食材+菜品详情营养卡+餐食当天热量) + 增长型本地推荐(RecommendationWeights/Style综合/偏熟悉/偏新鲜/偏营养+HealthRuleEngine因子化+DataSource派生画像(偏好/主料重复/营养互补)+PeriodPlanner style+AI推荐页/周期计划风格选择器) + 色系墙固定本公历年/定位今天居中(snapshotFlow等布局)/往年平均色行 + 沉浸式insets(全屏页navigationBarsPadding/底栏去clip) + 标签烹饪库选择器(LibraryPickerDialog软删仅user) + 加餐默认日期(最后餐食+1/时段) + UX设计师5轮审查修2必修; 关键坑=沉浸式全屏页遮挡+底部双重padding、scrollToItem同帧layoutInfo旧、mapResult重建丢粘性字段、软删已选未同步复活
- 上次总结会话点：2026-07-14晚2 每日卡路里目标+推荐收尾+营养数据(BodyMetrics/CalorieTarget BMR×活动系数 存偏好JSON免迁移+功能设置录入+餐食达标+色系墙评级结合热量combine+mapLatest; 推荐prompt加风格画像+逐菜理由常做/补营养; 今日营养卡热量进度+宏量占比条; ingredient_nutrition 65→100; 5轮审核修录入写回竞态/数字过滤/图例) 关键坑=偏好JSON免迁移、表单多字段写回竞态用本地态单一真相源、数字过滤单小数点、combine+mapLatest批量异步
- 上次总结会话点：2026-07-15 家庭档案体系(family_member/member_care/day_absentee表20-21.sqm+FamilyRepository默认成员迁移/忌口并集/关注成员达标/饭量系数份额/缺席按天+成员管理页+膳食统计页+缺席微调持久化+健康膳食设置分组+色系墙与成员/热量解耦)+A1自定义食材归类(food_group列22.sqm+营养大类必选预选+映射分类树+classify显式覆盖)+常用单位英文化(克→g等,23.sqm rename保FK)+私人菜单数据补全(食材279→440/菜品147→516/营养100%/842用量)+修食材单位下拉为空bug(selectMainTab旧快照copy)+D10旧快照排查。关键坑=多init并发+旧快照copy写回丢字段、迁移rename保FK带守卫、加列升级无损、表无code按名映射、真机logcat+埋点定位UI数据问题
- 上次总结会话点：2026-07-15 数据核对轮(食材归类核准 classify尾词优先+NAME_OVERRIDE54项特例 + 全量440食材营养从权威平台nlc.chinanutri.cn/USDA/悉尼GI/2024痛风指南 15组agent联网核准 覆盖升级式合并verified278/pending162 + 食材营养表功能combine5筛选排序VM/横滚可排序表/数据来源弹窗 + 数据来源.md记档)。关键坑=中文食材分类看尾词head-noun非前缀关键词、健康数据禁编造出处必标ref+verified/pending、覆盖升级式合并保留auth未覆盖字段、agent联网分片核准数据模式
- 上次总结会话点：2026-07-15 无人值守UX深挖轮(4个Apple-UX agent深挖食材/菜品/餐食/健康膳食→方案→主线程落地15项安全改进+高风险入待确认队列 + 营养表体验行高亮/搜索进标题栏/横竖屏悬浮按钮/上划收筛选栏)。关键坑=字典英文化后按旧名硬编码查(gramUnit找"克"恒null丢unitId)、健康提示守免责红线(如实非医嘱nutritionGaps)、combine多源suspend可内联(别用list+cast)、无人值守研究agent产方案+主线程落地安全项模式(安全vs需拍板判定)
- 上次总结会话点：2026-07-15深夜 无人值守三评审+bug大轮(用户报bug:凉皮0千卡=seedDishes已存在即跳过致配料关联永久残缺→补齐式重挂+SEED_LOGIC_VERSION盐;随机推荐翻忌口菜=rotate随机翻到全忌口末批;膳食统计没吃回不来=togglePresent读冻结的stateIn.value + 3专家agent深挖算法/架构/创建流程,落地avoid50→5/onHandMain封顶/seed缺失项告警/deleteDay落日志/resolveGrams防负/编辑未保存守卫/详情记这道菜CTA/首页计划上移/备注折叠/存为菜品,需拍板项入待办E节)。关键坑=seed已存在即跳过致关联永久残缺(补齐式)、stateIn(WhileSubscribed)无订阅者value冻结致toggle失效、rotate随机翻罚分末批、打分硬约束巨值靠分层排序冗余、专家agent产方案+主线程落安全项
- 上次总结会话点：2026-07-16 体验修复轮(小米8拍照90°旋转=EXIF方向未应用加androidx.exifinterface读+Matrix摆正、主食角标改苹果式贴角小圆、个人健康档案整合进家庭档案=与成员"我"同套care分类纯UI去重取消独立入口用户卡取我的健康状态数据层保留、导航栏色跟随主题背景=透明系统栏在app深色但系统浅色时露android默认白windowBackground改SideEffect设navigationBarColor=colorScheme.background)。关键坑=拍照EXIF方向需读取+Matrix摆正(minSdk21用androidx.exifinterface)、透明系统栏≠跟随界面色需显式设Compose主题背景、健康档案与家庭成员同套care分类整合纯UI去重
- 上次总结会话点：2026-07-16 无人值守健康参考轮(排骨海带汤等预设菜0千卡新根因=补齐只补缺失关联忽略已关联NULL用量→fillDishIngredientQuantityIfNull回填空用量+SEED v3;软删菜reseed重插带新数据、未删走补齐分支被跳过;华为真机adb拉库python模拟368菜838行验证 + MMR批内多样性diversify主料Jaccard贪心只重排正常菜层四风格全开含单测 + 膳食参考依据页DietaryReference静态数据6分类12来源4片agent联网核准钠糖GI嘌呤脂肪+关于链接 + 营养级别评级方案只出文档 + 食材表单低频折叠+新建返回守卫 + build-cli.sh JDK17自动探测兼容Mac)。关键坑=0千卡补齐要回填空用量非只补缺失关联、Git Bash adb访问/sdcard需MSYS_NO_PATHCONV=1、db在getExternalFilesDir/cookbook/db非databases/、嘌呤三级分级非国标(WS/T560只定性)、GI 55/70是FAO/WHO非WS/T652、真机拉库python模拟SQL先证修复再改代码
- 上次总结会话点：2026-07-16 下半场(调料默认克数SeasoningDefaults只对分类调料缩小油菜仍100g + 数据来源页/参考资料组ReferenceScaffold复用 + 功能介绍页Apple欢迎范式 + 配色切换7套AppPalette/Palettes向iOS系统色调校鲜亮有活力+我的外观选择器 + 今日卡宏量渐变条段中心平滑过渡+慢病提示个人视角concerns琥珀点色系墙保持纯结构决策A + 组合部分选 + 交互模式库Apple-UX审阅§九增补9.12-9.17+交互组件复用指南+CLAUDE门禁 + B-1餐食页守卫UnsavedGuard复用件)。关键坑=Material3-1.1.2无HorizontalDivider用Divider、配色改走AppPalette+Palettes别硬编码散落、色系墙不关联热量慢病(热量个人放今日卡)、调料默认克数只对分类调料缩小、宏量渐变别实块糊接缝改段中心平滑过渡、未保存守卫用rememberUnsavedGuard非包裹式、UI交互先Apple-UX过再编码+做UI先查交互组件复用指南
- 上次总结会话点：2026-07-16 收尾(B批UX B-1~B-5 双门禁完成:UnsavedGuard复用件+餐食页守卫、AppSnackbar统一宿主、库存Tab MiniStepper就地加减、食历卡三图标收ActionSheet、删整天软删撤销snapshotDay+showUndo；渲染宏量渐变回退分段实色；Google代码审查阻断项=撤销saveDayMeals重复抬喜爱度→bumpPreference开关+回归单测；CLAUDE加代码质量门禁=Google工程师审查agent)。关键坑=删/还原走save路径别重复抬统计(saveDayMeals bumpPreference=false)、可逆删除软删+撤销别硬确认(快照读失败别照删/Snackbar Long/单job串行化)、未保存守卫非包裹式rememberUnsavedGuard、卡片能力用可选footer槽、CompositionLocal宿主null默认挂MainScaffold、两门禁(UI先Apple-UX后Google审查)
- 上次总结会话点：2026-07-17 会话续接+backlog安全项清理(DishPickerFlow埋点清理9fd8c5c/搜索占位省略号41bfa86/AlertDialog句号copywriter审校f3f995e)+大类推进campaign建立(用户授权无人值守全权推进UI/工程/产品三大类·每功能提交+飞书+授权push·高风险selectionMode重构/seed异步化入待确认队列)。关键点=AppLogger.d无release门控每次写文件属排查埋点用完必删、copywriter角色审校连⚪一致性也走门禁、提交协议改为每功能即提交+飞书+授权push(覆盖旧"不push"默认)
- 上次总结会话点：2026-07-20 真机反馈批(50道家常菜658→708 + 华为A12倒计时闪退根因修复[导航图未挂时navigate·等currentBackStackEntryFlow.first()]真机验证 + F#4酒水顶层分类[与其他并列·不上色系墙]+酒酿归谷薯 + 可乐重复分类修复[seeder补齐式只加不删→reconcile删旧general关联+unlinkIngredientCategory+SEED v8] + 酒类入库+临床忌口[漏接忌口系统致啤酒对痛风显可食·联网核准35条care规则+CrowdCare压制avoid→慎选红] + F#6报告记一餐带日期 + 食材最近Tab去字母条 + 透明准则确立分级透明T0-T3+apple_software_behavior角色 + 联网核准必列数据来源规则 + 参考内容数据驱动架构会商定案+全App行为透明清单)。关键坑=冷启动intent立即navigate须等导航图就绪、加健康食材必走忌口系统核对(数值判绿≠临床可食)、移预设食材分类要reconcile删旧关联、改共享域判定同步改单测、真机崩溃用logcat -b crash -d捞缓冲
- 上次总结会话点：2026-07-20 下半程(《App操作基调·设计系统》确立5设计师会诊+菜品编辑页P1家族化[封面coverStyle前移/保存下移FormBottomBar/步骤烹饪标签下沉§9.31折叠]Google审查无阻断 + 热量bug真机DB修复[用户给cookbook.db·python证实:12000根因=配料unit_id空+resolveGrams兜底×piece_gram/60→按克直取防天价;290停旧值=今日卡combine不含dish_ingredient→observeDishContentChanges并进combine])。关键坑=计件兜底×倍率要防大quantity(克数误存)放大、派生汇总依赖A表但源在B表要显式观察B表变化并入combine、数据bug先真机DB python证实再改、大块字段重排用一次原子Edit重写
- 上次总结会话点：2026-07-20~21 超长无人值守会话(17笔·全push·master `0e7b6f4`)：家族化P2食材编辑增量二(rememberSaveable全草稿+hydrated守卫)/P3两选择页统一SelectionSummaryBar(食材3步移除→就地×)/P4§1.4/F#1家庭Tab字母条 + 健康安全忌口补漏(CrowdCare引擎修「录低值反判绿」+联网核准104条care规则480→584+回归守卫单测) + F#7 AI按餐次入参 + F#8透明准则(changelog+SeedUpdateCenter双游标+更新记录中心)。方法论:分批增量交付/门禁编排(设计→编码→Google审)/健康数据联网核准加法流程/盲做安全边界(需真机/拍板/前置的记录不盲改)。红线新增:CrowdCare单向压制只升不降+care补漏加法流程、rememberSaveable草稿与异步DB回灌用hydrated守卫。踩坑详见experience/06「家族化P2-P4」「健康安全P3」段。
- 上次总结会话点：2026-07-21 超长无人值守 session(17笔·全push·master `4cf9ddb`)：D1-D5确定方案全落地(D3去红/D4周计划慢病软降抽ChronicDiseasePenalty/D2卡化/D1-1 CookMode+D1-2 FamilyEdit VM化min-fix F-Arch2结案) + 算法拍板批(B#6封顶/C#F1补菜/C#F2早餐软硬透传) + **周计划营养线全链路**(domain一期 WeeklyNutritionLineAggregator+Advisor结构层+8单测 / AiPlan「一周营养搭配」概览卡) + **AiPlan界面统一P1**(DayCard白卡化+控件折叠进「计划设置」弹层+标注去emoji§9.35) + **推荐带营养素热量**(每菜整份千卡+宏量·钠"偏咸"·受CALORIE_NUMBER_ENABLED开关§9.36) + 拍照删除迁全屏查看器§9.34&**查看器删图预览不刷新bug修**(produceState key preview恒null) + 报告空周跳一周计划(WEEK_PLAN_ROUTE可选date参数). 会商:营养线四角色/AiPlan统一/A#4A#6分析. 方法论:表单越层min-fix按准则A判/抽共享防漂移/把已算的盛出来优先造新算法/多角色会商汇总收敛. 红线新增:produceState key用真正变化的输入非派生null值、suspend传染grep调用点、热量个人概念=CALORIE_NUMBER_ENABLED开关. 踩坑详见experience/06「营养线&推荐&拍照 session」段、方法论见02。
- 上次总结会话点：2026-07-22 超长无人值守·数据体系大轮(第二批数据扩充脚本省token satFat+122/purine+31/gi+5·USDA全库本地匹配 + 全数据源交叉核对 能量Atwater/nlc/USDA修速冻虾仁+年糕生蚝玫瑰花 + **数值+属性双层健康判定标准**固化CLAUDE.md门禁 + **属性标签体系B** FoodAttribute8属性+FoodAttributeCare+ingredient_attributes.json+seed展开人工优先去重+AttributeGuesser+单测 + 加工食品入库 + 现有食材属性标签全面补全40食材 + 自建食材L3 shared逻辑+UX规范(UI待实现) + **判定口径必联网核实规则**(果糖-水果教训)脚本方案三-B + 自建食材营养自动获取规划三来源 + 资产化scripts/data+cron月度体检)。方法论:脚本优先省token/判定口径联网核实别想当然/交叉核对分层(Atwater自洽最灵敏)/数值+属性双层/属性标签体系数据驱动. 详见experience/06「2026-07-22数据体系大轮」段、02双层判定架构。
- 上次总结会话点：2026-08-02 AI 记一餐全链路 Bug 修复·三视角会诊（6 commit·RuleMealParser+EatDrinkStripper+FlatToDayMealConverter+CloudAi max_tokens+日期上下文+语音+数据防御+Prompt 精简·编码自查铁律跨项目通用 5 条）
- 上次总结会话点：2026-08-05 AI记餐V2验证收口（详情滚动/重复按钮/日期VM复用/家庭分类非预设过滤）+ 同日误关重开保留会话（日期切换/保存后才换key）+ 周期记/NDJSON流式大改的开发验收规范（B1–B6/父子键/流式零写库）+ 所有AI输出流式渐进显示横向规则 + 跨模型上下文收敛（`.ai-context/PROJECT.md` 首读、根 docs 历史归档、唯一时间戳真机清单）。关键坑=修陈旧参数不能每次重建，key生命周期按业务会话定义；长AI输出不要只等整体JSON闭合；当前状态必须由 PROJECT→SESSION→地图读取，历史资料不得反向覆盖。

- 上次总结会话点：2026-07-25 超长无人值守·数据/参考资料大轮(**食材库扩充阶段1** 爬nlc全量中国成分表**505→1177翻倍**·管线`data-pipeline/`全套[nlc_crawl分类浏览/parse解析/integrate集成/apply_care忌口]·净增672·忌口自动+抽样核[内脏/肠→痛风avoid·高嘌呤水产→limit·酒→CONTAINS_ALCOHOL·**联网核实痛风指南2024**]·**维生素6项**[胡萝卜素/视黄醇/B1/B2/烟酸/维C]独立seed863+**矿物质7项**[磷镁铁锌硒铜锰]873+胆固醇197 + **体检①揪出并修纤维列解析bug**[误把raw[11]灰分当膳食纤维·672全错·真纤维在[13]·物理约束纤维>碳水揪出] + **数据来源真实原则**[只列真实用到的源·撤4空挂名+核实加DRIs/膳食宝塔/营养科学全书] + **权威方法论优先准则**入CLAUDE.md[功能前查权威资料别自创·`功能总线_权威方法论对照.md`揪3处自创待换膳食宝塔:色系墙均衡/餐次差异化/推荐份量] + **健康科普页+维生素小百科页**[参考页范式+emoji配图] + 膳食宝塔/三餐分配/生命阶段膳食要点进膳食参考依据[权威地基]). 方法论:**爬虫3件套**[JSONL即写即存+已爬id跳过断点续连+--max-seconds控时]/**列映射用极端值食材+物理约束验证**[灰分误当纤维教训:普通食材看不出·魔芋/黄豆/金华火腿极端值+纤维≤碳水才证伪]/**权威优先别自创**[膳食结构走膳食宝塔如阈值走国标]/**数据来源真实**[用了才列非空挂名]/配忌口先按分类过滤+动物前缀防误报[羊肚菌/开心果误判内脏]/能量Atwater自洽当解析质检. 红线新增:列映射极端值+物理约束验证、数据来源真实、权威方法论优先、nlc缺项(GI/嘌呤/饱脂/维E/维D)省略不编造. 详见`feature/食材库扩充_阶段1_nlc全量.md`(管线+列映射定稿+经验)、`feature/功能总线_权威方法论对照.md`、`unattended_decisions.md`。
- 上次总结会话点：2026-08-04 项目地图系统与 AI 核心能力审计（全景图+路径索引双入口；AI 显式语义优先；AI 快捷记餐 P0 与隐私日志待修）。
- 上次总结会话点：2026-08-06 B4 输入 UI 改造闭环（AF-ARCH-01/02关闭·AF-ARCH-03冻结·B4蓝图+编码+三角色审查·新建9文件·shared+Android构建通过·`WORKFLOW_SINGLE_MODEL.md`落`~/.ai-context/`双模型共享·`WORKFLOW.md`+`WORKFLOW_SINGLE_MODEL.md`+`GLOBAL.md`三文件补算法工程师涉算法必加门禁·标题统一"AI快捷记"·裁决延后11项记入B4蓝图§10·单模型独立工作入口接入GLOBAL.md任务定级）
- 上次总结会话点：2026-08-07 B4+B5+B6 架构模型复核（BLUEPRINT C档常驻声明+BLUEPRINT_STATE握手文件·逐行核对`a7fdf074..ac664fa1`全量diff·**未通过**9阻断AF-B456-01~09+3缺证据+13建议·核心=`quickDraftText`/`inputText`双真相源致语音/粘贴发不出去+B3回归套件全线失效、`SegmentProgressBar`索引空间错配三角色审查假修复未被抓出、T-B4-01~07测试全缺Android定向测试从未运行、B5无独立蓝图·TURN转CODE等编码机修复·可复用根因回填`~/.ai-context/rules/blueprint_protocol.md`新增BL-08索引空间隐性耦合分类+延后项归宿门禁+字段真相源规则+CLAUDE.md踩坑红线2条，ai-share已同步`ecad3c3`）。关键坑=新增语义重叠state字段只切读取方不切写入方致静默失效、列表逐项状态禁用计数+下标反推、审查声称已修的阻断必须逐行diff复核不能信commit message自述。
- 上次总结会话点：2026-08-07下午 蓝图颗粒度分级机制建立并落地（用户要求可追踪、可分级的蓝图细化机制→Opus设计两轴正交(规模×颗粒度)+GC存在性命题红线+L1~L7+三分支升级(复发/扩容/开新级)→用户要求回溯B1~B3三轮复审结论(只看结论)补挖9条新GC(GC-27~35)+3个新BL(BL-09基数掩盖/BL-10高频节流未定/BL-11文档腐化)→**最有价值发现**：`AF-B3-03`→`AF-B456-01`同一bug(编辑收口函数被新入口绕过)跨批次真实复发、`AF-ARCH-02`(构造时单例被基数=1掩盖)全项目最严重阻断→落地：`~/.ai-context/rules/blueprint_protocol.md`§2.1/2.2机制定义(已同步ai-share)+项目`12_多模型协作与实施蓝图规范.md`新增§12(35条GC登记表)+§13(升级历史)+BLUEPRINT_STATE.md新增CODE入口小节+B4蓝图打补丁到L7(新增§0.1/§3.5/§3.6/§5.8+改写§7步骤2/3/6+§9.4，CODE下一步唯一要读的文件))。关键坑=颗粒度轴与规模轴(FULL/LITE)正交别混同、GC条款必须是存在性命题不能是程度命题("写清楚"不合格)、延后项只留指针不落蓝图两次即升必填门禁。
- 上次总结会话点：2026-08-07傍晚 B4/B5/B6二次复核(AF-B456-05未关闭:CODE精确按字面规格实现`segmentStatuses:List<StreamSegmentState>`但规格值域只有3值无法表达"未开始"→兜底成STREAMING→周期记多段场景未轮到的段和真在流的段同时"脉冲中"；蓝图要求的新增测试T-B5-01~03/T-B4-08~10一个未写)+新增BL-12/GC-36+GC-24第3次命中转审查必查+蓝图§3.5.1给唯一最小修复转回CODE + 借鉴主流spec-driven框架评估(Opus讨论BMAD-METHOD/GitHub Spec Kit/AWS Kiro→关键反证:本项目已用过独立视角审查(三角色审查`3f60c20f`)AF-B456-05假修复正是那轮自己产出又放行的,换眼睛非万能药→只采纳While前置状态字段+GC-37独立挑战台账门禁,不采纳BMAD12角色人格/全面换EARS/仿Kiro的SMT矛盾检测,决策记录`12_...`§10.y防重复讨论,全局改动已推送ai-share`cb82699`) + 建立模型执行力评估机制(新建`14_模型执行力评估.md`,与`BLUEPRINT_STATE.md`抽象角色字段分离,与`~/.ai-context/MODEL_ROUTING.md`抽象分层互补,含"证据不足不下结论"门槛)。关键坑=独立视角审查不必然管用(起作用的是存在性命题条款本身非"多个人审")、蓝图冻结前需要挑战规格本身而非只按蓝图逐项反查、模型能力实证结论需≥3批次证据门槛别拿n=1下判断。
- 上次总结会话点：2026-08-08 AI记一餐B4/B5/B6三次复核收尾+K1a蓝图起草+GC-37首次实战挑战+用户纠正bug修复方案后直接实施（ARCH三次复核发现T-B5-02断言弱于蓝图要求当场收紧，B4+B5+B6全部9项阻断关闭ACCEPTED→起草K1a营养展示统一化+AI未配置报错蓝图，派独立opus agent做GC-37挑战14项中6项真阻断（sticky字段/null契约/一次性初始化缺刷新/判定值域过宽/恒定输入致检测分支失活/日期锚点错误）全部修订→**用户当面否决CFG部分设计**：规则解析应是AI兜底非独立报错模式，要求标题旁常显引擎+AI失败自动（非手动）回退规则+确认页说明来源，用户选择跳过CODE角色由ARCH直接实施→两轮独立google_quality_engineer审查发现6处真阻断（含状态判断用位置索引API替代真实状态查询、跨结构归属用日期字符串而非segmentId匹配、内部哨兵代号未翻译进用户可见通道、二轮发现一轮修复本身引入"AI正常结束但没解析出内容"路径失去兜底的覆盖缺口）全部修复+16条单测全绿）。方法论：GC-37独立挑战证明有效（同人设计+复核天然缺对抗视角）；阻断修复后必须反向核对"是否恰好堵死另一条真实场景的兜底"；跳过角色分离时用多轮独立审查agent替代"另一双眼睛"仍要保留。详见 `06_问题与踩坑.md`"AI 快捷记引擎标签 + 自动兜底 session"、`12_多模型协作与实施蓝图规范.md` §10.z。
- 上次总结会话点：2026-08-08 晚 首次"编码模型不得自审"实战——ARCH 独立复核 L1(云端AI首启同意`ad1c5878`)+K1i(流式地基`d7240d6f`)，不采信 CODE 自评：diff 逐 INV 核对+三条构建命令实跑全绿(shared 652/652·androidApp 49/49·assembleDebug)+闸门唯一性 grep(SwitchableAiRuntime(生产仅2处·isModelReady()未改)，发现 K1i 自评"allowlist合规"实际有1处未如实记录的受控例外(改了L1定义的CloudAiConsent.kt，功能安全但越界未记录)，已订正台账；附带发现协程跨测试类泄漏噪音(CoroutinesInternalError，单独重跑不复现)记非阻断。盘点AI记一餐全链路：B1-B6/K1a/L1/K1i-1均ARCH通过但从未真机验证，K1b(逐成员化健康评价)PARKED待处置，K1i-2(健康建议流式化)未设计。整理真机待验证清单(97项未验证+17已验证汇总表+修复历史L1~L4编号撞名改LEG-1~4+文件改名新时间戳)+会话交接。方法论见12_多模型协作与实施蓝图规范.md §10.aa。
- 上次总结会话点：2026-08-10 Project Graph Phase 1 Rework-4 最终收口（用户指正「更新git先拉远程再看本地」·另一台机器提交 Rework-4 蓝图 `ddcea510`·E-K1I-02 Truth 修正 build/pass→device/pending·K1i 保持 verifying·测试54/54+pg check PASS·commit+push `83623a32`·Phase 2 未启动等架构审核·经验见06「Project Graph领域经验」段：Verification ID 语义不可变/Observed Fact 不自动建 Verification/多机协作先拉远程）
- 总结次数：53

## 文件索引

| 文件 | 内容概述 |
|---|---|
| [01_项目基础.md](01_项目基础.md) | 项目信息、模块结构、构建配置 |
| [02_架构规范.md](02_架构规范.md) | MVVM 规范、DataSource、数据流、页面跳转 |
| [03_数据库.md](03_数据库.md) | 数据库集成、表结构、升级记录、配置同步 |
| [04_业务功能.md](04_业务功能.md) | 登录、注册、打印等业务知识 |
| [05_UI组件.md](05_UI组件.md) | 自定义控件、布局规范、drawable |
| [06_问题与踩坑.md](06_问题与踩坑.md) | Bug 排查经验 + 注意事项与踩坑记录 |
| [07_操作记录.md](07_操作记录.md) | 关键操作记录（按时间倒序） |
| [08_用户习惯.md](08_用户习惯.md) | 用户工作习惯、代码要求、沟通风格 |
| [09_工程统一规范.md](09_工程统一规范.md) | KMP 架构、Android/iOS UI、数据库、代码风格与开发流程规范 |
| [10_云端AI校验本地规则方法论.md](10_云端AI校验本地规则方法论.md) | GLM-4.5-flash 互相生成比较校验本地规则的方法论 |
| [11_AI模型能力改进建议.md](11_AI模型能力改进建议.md) | 前序 AI 踩坑总结的 5 条编码规则（R1-R5）·所有参与模型必读 |
| [12_多模型协作与实施蓝图规范.md](12_多模型协作与实施蓝图规范.md) | 架构模型与任意编码模型的长期协作合同：决策冻结、蓝图包、状态机/数据流/测试硬门禁、AF 反哺机制。 |
| [13_单模型独立任务流程规范.md](13_单模型独立任务流程规范.md) | 项目指针 → 真相源在 `~/.ai-context/WORKFLOW_SINGLE_MODEL.md`（双模型共享）：单模型角色分化+交叉验证流程。 |
| [14_模型执行力评估.md](14_模型执行力评估.md) | 不同具体模型担任 CODE 角色的实证评估台账：哪个模型在什么复杂度任务下表现如何、执行力边界在哪，积累证据后反哺 `MODEL_ROUTING.md`。 |


```

### .ai-context/project_graph/README.md
- SHA-256: `2E8EE6833D5CF672BC62118C41938436FCBC66EA24EFAD500838294B489A4677`
- Line count: 448

```markdown
# Project Graph（Cookbook 实施版 · Phase 1 — Model Contract）

> **Phase 1 — Model Contract**: FINAL ACCEPT / FROZEN
> **Phase 2A — Feature Universe**: ACCEPT / CLOSED
> **Phase 2B — Current WorkItem**: ACCEPT / CLOSED
> **Phase 2C — Plan + Relation + Deferred Semantics**: ACCEPT / CLOSED
> **Phase 2D — Verification Bootstrap**: ACCEPT / CLOSED
> **Phase 2E — Cross-Reconcile + Bootstrap Freeze**: ACCEPT / CLOSED
> **Phase 2 — Bootstrap**: FINAL ACCEPT / FROZEN
> **Phase 3**: AUTHORIZED / NOT STARTED
> **Graph Mode**: `draft`

> 本目录是 Project Graph 的 **数据真相源（Project Truth）**。Phase 1–2E 已完成接受并冻结；Phase 2E 完成 Verification 双层身份、ownership 与 verifying 状态对账；Phase 2 Final 已 **FINAL ACCEPT / FROZEN**。Phase 3 仅为 **AUTHORIZED / NOT STARTED**，Graph 仍为 `draft`，未正式启用。
>
> 维护角色：**AI Maintained, Human Read-Only**。
>
> 阶段记录：Phase 1 → [`migration/PHASE1_FINAL_ACCEPT.md`](migration/PHASE1_FINAL_ACCEPT.md) ｜ Phase 2A → [`migration/PHASE2A_ACCEPT.md`](migration/PHASE2A_ACCEPT.md) ｜ Phase 2A→2B 交接 → [`migration/PHASE2A_TO_2B_HANDOFF.md`](migration/PHASE2A_TO_2B_HANDOFF.md) ｜ Phase 2B Accept → [`migration/PHASE2B_ACCEPT.md`](migration/PHASE2B_ACCEPT.md) ｜ Phase 2B→2C 交接 → [`migration/PHASE2B_TO_2C_HANDOFF.md`](migration/PHASE2B_TO_2C_HANDOFF.md) ｜ Phase 2C Accept → [`migration/PHASE2C_ACCEPT.md`](migration/PHASE2C_ACCEPT.md) ｜ Phase 2C→2D 交接 → [`migration/PHASE2C_TO_2D_HANDOFF.md`](migration/PHASE2C_TO_2D_HANDOFF.md) ｜ Phase 2D→2E 交接 → [`migration/PHASE2D_TO_2E_HANDOFF.md`](migration/PHASE2D_TO_2E_HANDOFF.md) ｜ Phase 2E Accept → [`migration/PHASE2E_ACCEPT.md`](migration/PHASE2E_ACCEPT.md) ｜ Phase 2 Final Accept → [`migration/PHASE2_FINAL_ACCEPT.md`](migration/PHASE2_FINAL_ACCEPT.md) ｜ Phase 2→3 Handoff → [`migration/PHASE2_TO_PHASE3_HANDOFF.md`](migration/PHASE2_TO_PHASE3_HANDOFF.md)。

## 0. Phase 1 状态与冻结契约（Status & Frozen Contract）

### 0.1 Phase Progress

```text
Phase 1  — Model Contract      : FINAL ACCEPT / FROZEN   （Final Review Commit 83623a3）
Phase 2A — Feature Universe    : ACCEPT / CLOSED          （Review Commit b54246c1）
Phase 2B — Current WorkItem    : ACCEPT / CLOSED          （Review Commit e2127176）
Phase 2C — Plan+Relation+Deferred : ACCEPT / CLOSED       （Review Commit ced5f13f）
Phase 2D — Verification Bootstrap : ACCEPT / CLOSED       （Review Commit 1cfc96035237e708005b8919a5b624273e534a0c）
Phase 2E — Cross-Reconcile + Bootstrap Freeze : ACCEPT / CLOSED
Phase 2  — 整体                : FINAL ACCEPT / FROZEN
Phase 3  — Views + Activation  : AUTHORIZED / NOT STARTED
Graph Mode                     : draft
```

Phase 2A 已完成能力见 `migration/PHASE2A_ACCEPT.md`；Phase 2A→2B 执行强制入口见 `migration/PHASE2A_TO_2B_HANDOFF.md`；Phase 2B Accept 记录见 `migration/PHASE2B_ACCEPT.md`；Phase 2B→2C 强制入口见 `migration/PHASE2B_TO_2C_HANDOFF.md`；Phase 2C 决策/冲突见 `migration/PHASE2C_DECISIONS.md` / `migration/PHASE2C_CONFLICTS.md`；Phase 2C Accept 见 `migration/PHASE2C_ACCEPT.md`；Phase 2C→2D 强制入口见 `migration/PHASE2C_TO_2D_HANDOFF.md`。
**Phase 2B 已 ACCEPT / CLOSED（Review Commit `e2127176`）**：Migration Reconciliation（Rework）完成——Stable ID（FAM-AGE/FAM-MEAL/K15/J22）无损恢复、Source Coverage 100%（UNEXPLAINED=0）、TODO-* kind 全部修正、K1c 状态修正、L3 → F-TOOLS（FEATURE_SPLIT_CANDIDATE）。最终派生统计（From Graph）：Total 104 / Stable 51 / Generated 53。
**Phase 2C 已 ACCEPT / CLOSED（Review Commit `ced5f13f`）**：Plan lifecycle 冻结（Plan completed ≠ WorkItem done）、Observed vs Verification 冻结、BLUEPRINT_STATE Extension 边界冻结、FEAT-* 匿名 feature 约定结构化；Plan 迁移 PLAN-AI-NDJSON(completed)/PLAN-K1I(completed)/PLAN-K1A(completed)/PLAN-L1(completed)，K1g/K1i/K1a/L1 均保持 verifying；关系 K15↔I7、J22↔L2（related_to）、L3 affects 6 Feature 已建立；FEAT-AI-MEAL-001 / FEAT-RECOMMEND-001 已落位。**Phase 2D 与 Phase 2E 已 ACCEPT / CLOSED；Phase 2 Final 已 FINAL ACCEPT / FROZEN。执行入口见 `migration/PHASE2_FINAL_ACCEPT.md`。**

### 0.2 冻结契约（Phase 1 Frozen Contract）

以下 Phase 1 核心设计已冻结，后续 Phase 不得因迁移数据方便而随意改动：

- **核心实体**：Project / Feature / WorkItem / Plan / Verification / Relation / CodeMapping / CurrentWork
- **稳定 ID / Typed Reference / Feature Sharding**
- **Declared / Observed / Derived** 三分类契约
- **状态机**：Feature Lifecycle、WorkItem State Machine、Plan State Machine、Verification State Machine
- **Relation**：Canonical Direction、Semantic Matrix
- **Verification Closure Contract**
- **YAML Fail Closed / JSON Schema Validation / Semantic Validator / Duplicate Detection**

### 0.3 Frozen 的含义

Frozen **≠** 永远不可修改。Frozen 表示：

> 后续 Phase 不得因为迁移数据方便而随意改变核心 Contract。

只有满足以下**三者**才能修改 Frozen Contract：

1. 发现通用模型无法表达真实项目需求；
2. 形成明确 Architecture Change；
3. 经过独立架构复审。

**禁止**：`某模型发现某条数据不好迁 → 顺手改 Schema`。

### 0.4 mode: draft 与 Frozen 的区别

Contract = **Frozen** 与 Graph mode = **draft** 二者不冲突，同时成立：

- **Contract Frozen**：Project Graph 的**语言**已经稳定。
- **Graph draft**：Cookbook 的真实项目数据尚未完整 Bootstrap，Generated Views 尚未切换，Project Graph 尚未成为当前唯一 Project Truth 入口。

因此本阶段**绝对禁止**把 `mode` 改成 `active`。

### 0.5 Phase 2C 冻结语义（Plan / Observed / Extension / FEAT-*）

Phase 2C（Plan + Relation + Deferred Semantics）已实施并冻结以下治理语义，判据与来源见 `migration/PHASE2C_DECISIONS.md`：

- **Plan Lifecycle**（状态集合，非严格单向链）：`draft`、`reviewing`、`accepted`、`implementing`、`completed`、`superseded`。**`completed` 必须表示「Plan 要求的代码/文档实施步骤完成 + 设计/架构接受条件完成」**；`superseded` 是替换状态（当前 Plan 被另一正式 Plan 取代），**不要求以 `completed` 为前态**。
- **Plan completed ≠ WorkItem done**：`Plan: completed + WorkItem: verifying + Verification: device/pending` 完全合法。`completed` 只表示实施方案自身完成；WorkItem done 仍受 Verification Closure Contract 约束。
- **Observed vs Verification**：普通命令执行（build / test / lint / pg check）= **Observed Fact by default**，不是 Verification Entity。只有「稳定 Acceptance Semantic + 稳定 Verification ID + 明确验证对象」才成为 Verification（如 `E-K1I-01`）。禁止仅因 Gradle 成功新建 build Verification。Phase 2C **不实现 Observed Store**。
- **BLUEPRINT_STATE Extension 边界**：BLUEPRINT_STATE 的 `CODE/ARCH/REVIEW` 属于 **Cookbook-specific Project Graph Extension**，不是 Core Schema Semantic。禁止新增 `code_status` / `arch_status` / `review_status` 到 WorkItem/Plan/Verification Core Schema。
- **FEAT-* ID Convention**：匿名且确认为 `kind: feature` 的 WorkItem 使用 `FEAT-<FEATURE>-NNN`（max+1，永不重编号）。只是 Migration Stable ID Convention，不修改 JSON Schema。本批：`FEAT-AI-MEAL-001`、`FEAT-RECOMMEND-001`。

## 1. 这是什么

Project Graph 是 AI 开发时代的「项目结构化记忆层」。它回答：

> 项目现在发生了什么。

三种 Truth 各司其职：

```
Code                = Runtime Truth   （系统实际怎么运行）
Project Graph       = Project Truth    （项目当前处于什么状态）
Plan / ADR / 专项方案 = Decision Truth   （为什么这么设计）
```

本目录的 Markdown（README 之外不生成视图，Phase 1 不做）属于 View，可由 `pg render`（未来）重新生成，不作为真相源。

## 2. 目录结构

```
.ai-context/project_graph/
├── README.md                       # 本文件（设计说明）
├── project.yaml                    # Project 根：graph_version/mode/current/feature registry/extensions
├── features/
│   ├── F-AI-MEAL.yaml              # PoC 完整样例（WorkItem/Plan/Verification/Relation/CodeMapping）
│   └── F-MEAL.yaml                 # 简单样例（仅 match/code）
├── schema/
│   └── project-graph.schema.json   # JSON Schema（通用，不含业务字段）
└── tools/
    ├── yaml_lite.py                 # 零依赖 YAML 子集解析器
    ├── schema_checker.py            # 迷你 JSON Schema 校验器（零依赖）
    ├── project_graph.py             # 语义校验器 + `pg check` CLI
    └── tests/
        └── test_validator.py        # 覆盖 §28 全部场景的测试套件
```

## 3. 核心实体

| 实体 | 说明 | ID 形式 |
|------|------|---------|
| **Project** | 根节点：graph_version / mode / current / feature registry / extensions | — |
| **Feature** | 长期稳定的产品/系统能力（不是 Screen/VM/Class/阶段） | `F-XXX` |
| **WorkItem** | Bug/Todo/功能/重构等的统一项，用 `kind` 区分；保留现有 I/J/K/L 编号 | `K1i` `AF-B456-05` |
| **Plan** | WorkItem 的实施设计；一个 Plan → 多个 WorkItem（`work_items`） | `PLAN-XXX` |
| **Verification** | 闭环验证，关联 WorkItem | `E-XXX` |
| **Relation** | 正式关系概念，Typed Reference 表达 | `work:K1i` |
| **CodeMapping** | Feature↔Code 双向映射（`match` glob + `code` 路径） | — |
| **CurrentWork** | 当前 feature/work_item/phase/blocker 单一入口 | — |

Feature 按「文件分片」组织（`features/<id>.yaml`），AI 处理任务时只需读 `project.yaml` + 当前 Feature 文件，降低 Token。

**Feature shard Contract**：`features/F-A.yaml` 中的 WorkItem 的 **primary feature 必须等于 F-A**（`work_item.feature` 字段，语义校验器强制）；跨 Feature 影响使用 `affects` / `depends_on` / `related_to` 等 relation 表达。**禁止把属于 F-B 的 WorkItem 放进 F-A shard。**

## 4. 状态机（enum，禁止自由字符串）

| 实体 | 取值 |
|------|------|
| Graph mode | `draft` `active` |
| Feature lifecycle | `planned` `active` `mature` `deprecated` |
| Feature activity（Derived） | `idle` `developing` `reviewing` `verifying` `blocked` |
| Feature health（Derived） | `green` `yellow` `red` |
| WorkItem status | `backlog` `ready` `in_progress` `blocked` `review` `verifying` `done` `parked` `cancelled` |
| Plan status | `draft` `reviewing` `accepted` `implementing` `completed` `superseded` |
| Verification status | `pending` `pass` `fail` `blocked` `not_required` |
| WorkItem kind | `feature` `bug` `todo` `tech_debt` `refactor` `compliance` `research` `maintenance` |
| Relation type | `belongs_to` `implemented_by` `verified_by` `depends_on` `blocks` `affects` `supersedes` `related_to` |

## 5. Declared / Observed / Derived（§19）

同一事实只维护一次；能 Derive 的绝不要求 AI 手工重复同步。

| 类别 | 来源 | 示例 |
|------|------|------|
| **Declared** | AI 维护 | `status` / feature relation / plan relation / priority / intent |
| **Observed** | 工具采集 | commit / changed files / test exit code / timestamp |
| **Derived** | 程序计算 | open bug count / pending verify count / activity / health / affected features |

- **Final V1 Contract**：`Feature.lifecycle = Declared`；`Feature.activity = Derived`；`Feature.health = Derived`。核心 Feature YAML 中**不存储 activity/health**（Schema 的 Feature 无此字段，AI 不得声明或覆盖）。`project_graph.py` 已实现 `derive_activity` / `derive_health` 作为推导契约演示。
- 如未来确需 override activity/health，属于 Extension 设计，**本阶段不实现**。

## Source Provenance（Phase 2A · source_refs）

Entity → `source_refs` → authoritative repository artifact：

```yaml
source_refs:
  - .ai-context/docs/feature/AI记一餐_周期记_NDJSON流式开发规范.md
  - .ai-context/docs/功能路径索引.md#记录
```

- `source_refs` 可挂在 **Feature / WorkItem / Plan / Verification** 上（可选，通常 1~3 条），指向**权威来源**（功能路径索引 / 项目业务地图 / 专项方案），不是罗列所有提到该实体的文档。
- 语义校验器（PG-P2A-A01）轻量检查：去掉 `#anchor` 后，path 不得绝对、不得含 `..` 逃逸仓库、文件必须存在（相对仓库根解析）。Anchor 本身本阶段不验证。错误码 `PG-E-SOURCE_REF`。
- **`source_refs` 不改变 Truth Source 优先级，只记录证据来源**：Project Graph 仍是 Project Truth；Code / SESSION / BLUEPRINT_STATE / 项目地图 的现有优先级不变。

## 6. Typed Reference 与 Relation Canonical Direction（§18）

内部引用标准化为带类型：

```
feature:F-AI-MEAL   work:K1i   plan:PLAN-AI-NDJSON   verify:E-K1I-01
```

### Relation canonical direction

所有 relation 都有**唯一 canonical 方向**（本蓝图确认的最终 Contract）：

```text
belongs_to       work → feature
implemented_by   work → plan
verified_by      work → verification
```

即：WorkItem 属于 Feature（belongs_to）、被 Plan 实现（implemented_by）、被 Verification 验证（verified_by）——**三者都以 work 为起点**。不得按书写顺序反向理解。

### Shorthand 与 canonical relation 分开说明

为编辑友好，YAML 中 shorthand 字段用裸 ID（由字段上下文确定类型）。**shorthand 只是存储形态，normalized graph semantic 必须回到 canonical direction**：

```yaml
work_items:
  - id: K1i
    feature: F-AI-MEAL        # shorthand → normalized: work:K1i --belongs_to--> feature:F-AI-MEAL
plans:
  - id: PLAN-X
    work_items:
      - WORK-X               # storage shorthand → normalized: work:WORK-X --implemented_by--> plan:PLAN-X
verifications:
  - id: E-K1I-01
    work_item: K1i            # shorthand → normalized: work:K1i --verified_by--> verify:E-K1I-01
```

> 注意：`plan.work_items: [WORK-X]` 是 **Storage Shorthand**——它表示 `work:WORK-X --implemented_by--> plan:PLAN-X`，**不是** `plan → work`。Verification 同理（`work:WORK-Y --verified_by--> verify:E-Y`，不是 `verify → work`）。

跨切关系（depends_on / blocks / affects / supersedes / related_to）用 `relations` 显式声明带类型引用：

```yaml
relations:
  - source: work:WORK-X
    type: depends_on
    target: work:WORK-Y
```

每种关系**唯一声明源**，不重复：belongs_to 在 work_item.feature、implemented_by 在 plan.work_items、verified_by 在 verification.work_item，其余在 relations。

## 7. Code Mapping 双向用途（§21）

```yaml
match:                 # 文件 glob，支持 Code → Feature 反向定位
  - shared/**/ai/meallog/**
code:                  # 关键路径（真实 repo-relative path 数组），支持 Feature → Code 生成功能路径索引
  ui:
    - androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputSheet.kt
  viewmodel:
    - androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputViewModel.kt
  domain:
    - shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/StreamingMealSession.kt
    - shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/StreamingMealParser.kt
  tests:
    - shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/ai/meallog/StreamingMealParserTest.kt
```

- **Forward（Feature → Code）**：`Feature → CodeMapping → 功能路径索引`（AI 代码定位）。
- **Reverse（Code → Feature）**：`git diff changed files → 匹配 match glob → affected Feature`（未来 `pg affected`）。

`code` 键只允许：`entry` `ui` `viewmodel` `domain` `data` `core` `tests` `other`（语义校验器强制）。**路径必须相对仓库 root、用 `/` 分隔；禁止 `...` 缩略、类名代替路径、逗号拼接 CSV。**

## 8. Affected 契约（§22，Phase 1 仅定义不实现）

未来流水线：

```
Changed Files → Code Mapping(match) → Direct Features → Dependents/Related
```

设计上保证 `match` 能反向定位，不会出现「未来无法反向定位的 Code Mapping」。

## 9. Extensions（§24）

核心不写死角色/平台/业务。Cookbook 现有 `BLUEPRINT_STATE` 的 CODE/ARCH/REVIEW/TURN 通过 `extensions` 承载：

```yaml
extensions:
  blueprint_state:
    roles: [CODE, ARCH, REVIEW]
    turn: USER
```

语义校验器检查 extension 键不侵入核心顶层字段（`kind`/`graph_version`/`mode`/`project`/`current`/`features`/`extensions`）。

## 10. Schema 标准（§25）

```
YAML Data → yaml_lite 解析 → JSON 对象 → JSON Schema(schema_checker) → 语义校验器(project_graph)
```

- JSON Schema（`project-graph.schema.json`）负责：类型 / 必填 / enum / ID 格式 / 基本结构。**通用，不含 `AI记一餐`/`Kotlin`/`营养`/`Android` 等业务约束。**
- Python Semantic Validator 负责：跨文件引用、ID 唯一、Relation 合法、depends_on 循环、Code Mapping 类型、CurrentWork、Extension、Done 闭环等。

## 11. Validator 检查项（§27）

`pg check` 至少执行：

1. YAML 可解析
2. JSON Schema 校验通过
3. Graph 版本合法
4. ID 唯一
5. Feature 引用存在（registry）
6. WorkItem.feature 存在
7. Plan 引用存在
8. Verification 引用存在
9. Relation source 存在
10. Relation target 存在
11. 状态合法（enum）
12. 非法自引用
13. depends_on 明显循环
14. Code Mapping 类型合法
15. CurrentWork 引用合法
16. Extension 不破坏核心 Schema

附加语义（**Verification Done Contract**，与代码 `_check_done_rule` 一致）：
- WorkItem `status = done` 要求**至少存在一个 Verification**；
- 且所有 `required = true` 的 Verification 都必须为 `pass` 或 `not_required + reason`；
- 任何 `required = true` 的 Verification 处于 `pending` / `fail` / `blocked` → **禁止 Done**；
- `required = false`（可选）的 Verification 不阻止 Done；`not_required` 必须带 `reason`（§16）。

## 12. 错误输出格式（§32）

避免裸 stack trace，结构化输出：

```
PG-E-RELATION
file: features/F-AI-MEAL.yaml
source: work:K1g
target: plan:PLAN-X
reason: target does not exist
```

错误码：`PG-E-LOAD` `PG-E-SCHEMA` `PG-E-GRAPH_VERSION` `PG-E-DUP_ID` `PG-E-UNKNOWN_FEATURE` `PG-E-WORK_FEATURE` `PG-E-PLAN_REF` `PG-E-VERIFY_REF` `PG-E-RELATION_SOURCE` `PG-E-RELATION_TARGET` `PG-E-SELF_REF` `PG-E-CYCLE` `PG-E-CODE_MAPPING` `PG-E-CURRENT` `PG-E-EXTENSION` `PG-E-DONE_NO_VERIFY` `PG-E-VERIFY_REASON` `PG-E-REGISTRY_MISMATCH` `PG-E-SOURCE_REF`。

## 13. Tool Contract（§31）

Phase 1 真正可用：`pg check`。其余为未来 CLI 预定义，**本阶段不实现**。

| 命令 | 职责 | Phase 1 |
|------|------|---------|
| `pg check` | schema + 语义 + relation 校验 | ✅ 可用 |
| `pg begin` | 读 AI_INDEX、定位 Feature/WorkItem、写 CurrentWork | 未实现 |
| `pg affected` | changed files → affected features | 未实现 |
| `pg verify` | 执行验证命令、记录 Observed Facts | 未实现 |
| `pg reconcile` | git diff ↔ graph 一致性 | 未实现 |
| `pg render` | 生成 Views（deterministic） | 未实现 |
| `pg finish` | check pass 才允许宣布完成 | 未实现 |

## 14. 使用

```bash
# 校验 Project Graph（默认本目录）
python .ai-context/project_graph/tools/project_graph.py check

# 校验指定目录
python .ai-context/project_graph/tools/project_graph.py check <graph_dir>

# 运行测试套件（零依赖，unittest）
cd .ai-context/project_graph/tools/tests
python -m unittest test_validator -v
```

CLI 退出码：0 = 通过，1 = 有 issue，2 = 用法错误。

## 15. 设计原则（§33，成熟思想来源）

不照搬大型平台，吸收其思想：

| 来源 | 吸收 |
|------|------|
| Backstage | Stable Entity / Typed Relation |
| GitOps | Declarative / Versioned / Reconciliation |
| Nx Project Graph | Code Mapping / Affected Detection |
| GitHub Projects | One Data Model, Multiple Views |
| C4 | Different Zoom Levels / Different Audiences |
| JSON Schema | Standard Schema Validation |

## 16. 不做（§34）

Phase 1 明确不引入：Backstage 平台 / Nx 工具本身 / Neo4j / 图数据库 / GitHub Projects 作 Truth / Web Dashboard / 后台服务 / Event Sourcing。

保持：**Git-native + File-based + AI-native**。

## 17. 跨切质量要求

- **Git-native（§35）**：所有数据可 `git diff` / `git review` / `git revert`，禁止只有本地数据库。
- **Token 友好（§36）**：按 Feature 分文件，普通任务只读 `project.yaml` + 当前 Feature 文件，不加载整个 Graph。
- **Deterministic（§37）**：同样输入 → 同样输出；未来 `pg render` 两次执行须 0 diff。
- **零依赖**：工具仅用 Python 标准库（无 PyYAML/jsonschema 也能运行，适配本仓库无网络环境）。`yaml_lite` 是受约束子集解析器；正式环境可换 PyYAML（接口兼容）。

## 18. 现有状态冲突（§41）

Phase 1 若发现 SESSION / 07 / 待办 / 方案 / 功能路径互相冲突，**只记录**（`MIGRATION_NOTE`），不在本阶段顺手修复——属于 Phase 2 Bootstrap。

## 19. Phase 1 验收对照（§39）

- [x] Schema 能表达 Cookbook 现有主要工作模型（Feature/WorkItem/Plan/Verification/Relation/CodeMapping/CurrentWork/Extension）
- [x] Schema 不依赖 Cookbook 业务（无业务字段）
- [x] Feature 是稳定长期实体
- [x] 现有 I/J/K/L 编号无损接入（PoC 用 K1g/K1i/K1b/I8/AF-* 形式）
- [x] WorkItem 统一模型成立
- [x] Plan 可关联多个 WorkItem
- [x] Verification 可闭环 WorkItem
- [x] Typed Relation 模型成立
- [x] Declared/Observed/Derived 明确
- [x] Code Mapping 支持正向和反向
- [x] JSON Schema 实际可运行
- [x] Semantic Validator 实际可运行
- [x] Validator 有充分测试（`tools/tests/test_validator.py`，覆盖 §28 全部场景 + 额外语义 + 解析 + 真实数据 + Rework 回归，运行：`python -m unittest test_validator`）
- [x] 没有迁移全量 Cookbook（仅 F-AI-MEAL + F-MEAL 两个样例）
- [x] 没有修改产品运行行为（无生产代码/DB 变更）

### Phase 1 验收最终状态

- Architecture Review: **FINAL ACCEPT**
- Final Review Commit: `83623a3`
- Known Blockers: **0**
- Contract: **FROZEN**

## 20. 门禁（§44）

Phase 1 实施时执行 **STOP 门禁**：完成 Phase 1 后 STOP，不继续迁移 Cookbook / 生成 AI_INDEX / 重写 07 / 重写功能路径索引 / 自动维护生命周期 / Git Hook / CI。

该门禁已完成：**Phase 1 已于最终审核提交 `83623a3` 后获得 FINAL ACCEPT，核心 Contract 已 FROZEN**（Phase 1 → Phase 2 完整承接见 `migration/PHASE1_FINAL_ACCEPT.md`）。

**当前阶段状态**：

```text
Phase 1
FINAL ACCEPT / FROZEN

Phase 2A
ACCEPT / CLOSED

Phase 2B
ACCEPT / CLOSED

Phase 2C
ACCEPT / CLOSED

Current Stage:
Pre-Phase-3 Handoff Complete

Phase 2D Status:
ACCEPT / CLOSED
Phase 2D Review Commit:
1cfc96035237e708005b8919a5b624273e534a0c
Phase 2E:
ACCEPT / CLOSED

Phase 2:
FINAL ACCEPT / FROZEN

Phase 3:
AUTHORIZED / NOT STARTED

Graph Mode:
draft
```

每一批独立 commit / push / architecture review，禁止连续自动执行。**禁止提前把 `mode` 切到 `active`。**


```

## E. Phase and Protocol State Evidence

| Subject | Source Path | Source Role | Exact Status Text | Line(s) | SHA-256 | Observation |
|---|---|---|---|---|---|---|
| Phase 3 | `.ai-context/project_graph/project.yaml` | canonical project graph declaration | `Phase 3: AUTHORIZED / NOT STARTED` | 9-10 | `2C756CE240C129E72276D7A97842C953580C006B768227BB06C086C270CA2F0F` | Lifecycle declaration. |
| Phase 3A | `.ai-context/project_graph/migration/PHASE3A_AUDIT.md` | audit snapshot | `Final state: Phase 3A R1 EXECUTED / PENDING INDEPENDENT ARCH REVIEW; STOP.` | 144 | `F0C4A9A2E529C87BF9C252EFB79C78B6ABBB89FC7E5740F70B82B5BBA38CF9F2` | Acceptance snapshot. |
| Phase 3A | `.ai-context/project_graph/migration/PHASE3A_BLUEPRINT.md` | execution blueprint | `Final state is EXECUTED / PENDING INDEPENDENT ARCH REVIEW, TURN=REVIEW.` | 188-190 | `EE0ACC657FD4E9B0A89D3621C84681AFEBC68DDB1E5DDA68AAA9DF89F8F9CB49` | Execution/lifecycle evidence. |
| Phase 3B | `.ai-context/project_graph/migration/PHASE3A_BLUEPRINT.md` | deferred phase map | `Renderer Contract | Phase 3B`; Phase 3B requires separate ARCH accept/handoff. | 176, 190 | `EE0ACC657FD4E9B0A89D3621C84681AFEBC68DDB1E5DDA68AAA9DF89F8F9CB49` | No Phase 3B execution evidence. |
| GOV-BP-P3-01 | `.ai-context/docs/context_memory/BLUEPRINT_STATE.md` | handshake/lifecycle snapshot | `EXECUTED / PENDING INDEPENDENT ARCH REVIEW` | 17-24 | `C93F25059DC7382F3C770E51DAEB6530453673B70A3610927B741C1B1EAD5463` | Current batch awaits ARCH review. |
| blueprint_protocol.md | `<USER_HOME>/.ai-context/rules/blueprint_protocol.md` | user-level canonical protocol | `Stable Identity / Contract / Lifecycle State / Acceptance Snapshot / Generated View` | 15-25 | `C2C8332EB12D545CA89FCA4C80A15DBA7E2ACF5FAF7703A8CFE6815A0B5F0EB3` | Full text omitted from repository copy. |
| Canonical stable entry | `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md` | project fallback copy | `本文件是 fallback 副本` | 8, 32 | `44FEE0FDFC55FAAA61B0A599FE35A1F61757921AB8D63B48498A59BF64EBECFC` | Stable governance entry. |
| Lifecycle state | `.ai-context/docs/context_memory/BLUEPRINT_STATE.md` | handshake state | `TURN=REVIEW` | 17-24 | `C93F25059DC7382F3C770E51DAEB6530453673B70A3610927B741C1B1EAD5463` | Current value belongs to state file. |
| Acceptance snapshot | `.ai-context/project_graph/migration/PHASE3A_AUDIT.md` | audit snapshot | `EXECUTED / PENDING INDEPENDENT ARCH REVIEW` | 144 | `F0C4A9A2E529C87BF9C252EFB79C78B6ABBB89FC7E5740F70B82B5BBA38CF9F2` | Not a lifecycle adjudication. |
| Generated view | UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN | No source uniquely identified. |

## F. Conflict and Absence Register

- `project.yaml` declares Phase 3 `AUTHORIZED / NOT STARTED` at lines 9-10.
- `PHASE3A_AUDIT.md` and `PHASE3A_BLUEPRINT.md` declare Phase 3A executed and pending independent architecture review.
- This is **UNRESOLVED AUTHORITY / LIFECYCLE CONFLICT**; no side is selected and no source is modified.
- Phase 3B execution evidence is ABSENT; its ownership is a deferred phase / separate ARCH accept-handoff.
- `GOV-BP-P3-01` has no contradictory status in inspected sources.
- No required project-level governance input was absent.
- User-level full contents are intentionally absent; integrity is preserved by path, hash, and line count.

## G. Working Tree Preservation Evidence

- Requested execution parent: `169bb0a70524c513fd4d2fd1cc72e06cac3ee27d`.
- Actual preflight HEAD: `3e08ab9b7f07d7ba54a0981e74a78193fa315e05`; user-authorized interposed documentation commit `3e08ab9b`.
- Preflight staged state: empty.
- Preflight unstaged/untracked state: pre-existing dirty documentation and temporary files; all unrelated entries remain untouched.
- Allowlist after write: only Truth Pack modified and this supplement added.
- Existing dirty items were not modified, deleted, staged, or committed by this task.

## H. Completion Assessment

All required project-level files are present with complete original contents. Phase/protocol evidence, absence records, conflict registration, hash correction, user-level omission, and dirty-tree preservation are complete. No sensitive values were found.
Result: COMPLETE.

## I. Integrity

- Revised Truth Pack SHA-256: SELF_SHA256_REPORTED_EXTERNALLY
- Revised Truth Pack line count: reported after write.
- Supplement path: `.ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md`
- Supplement file size and line count: reported after write.
- Supplement SHA-256: SELF_SHA256_REPORTED_EXTERNALLY
- Expected commit files:
  - M `.ai-context/docs/UBF-M0-Truth-Pack-b7fc77e4.md`
  - A `.ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md`
- Unresolved Q/STOP: `NONE`.

