# BLUEPRINT_STATE

唯一握手状态文件。开工前先 `git pull` 读本文件；`TURN` 不是自己 → 停手，只报告当前持球方，不改代码。开工前除看 `TURN`，还须看 `颗粒度` 行确认本批蓝图应达到的级别。完成本方动作后在同一提交内更新本文件再 push。

**ARCH/CODE 命名规则**：`ARCH`/`CODE`/`REVIEW`/`TURN` 这几个协议字段只写角色名+机器标识（如 `架构师@主力机`、`Coder@副机`），**禁止出现具体模型名称**（Claude/DeepSeek/GPT 等）——角色定义是抽象的，具体由哪个模型担任取决于当前会话，协议逻辑不依赖模型身份。

**模型执行力评估台账（2026-08-07 新增，与上条不冲突）**：独立文档 `docs/experience/14_模型执行力评估.md`，与本文件的抽象角色字段完全分离。CODE 完成本批交付时，去该文档追加一行记录（含实际模型名）；ARCH 复核后补简评。**本文件不重复该表**，避免同一数据两处维护。

---

## 当前批次：L1 云端AI首启同意（2026-08-08 用户已定序：L1→K1e→K1i→K1h，逐个出蓝图）

| 字段 | 值 |
|---|---|
| 任务/批次 | 用户明确指示"你只负责蓝图，不要编码"——本 session 的 ARCH 只起草蓝图，不实现。已按序完成 L1 蓝图：起草→GC-37 第一轮挑战（判定核心设计前提有误，要求结构性返工）→v2 重新设计闸门落点（`SwitchableAiRuntime.complete()`）→GC-37 第二轮挑战（核心设计确认成立，挑出 7 项局部缺口）→v2b 就地处置全部问题→转 `BLUEPRINT_READY` |
| 状态 | `BLUEPRINT_READY` |
| **TURN** | **CODE**（`docs/feature/L1_云端AI首启同意与合规免责_实施蓝图.md`，按其 §7 STEP 逐条机械执行；本蓝图颗粒度 L7，§0.1 是入口） |
| K1e 现状 | **已废弃**——蓝图起草后 GC-37 独立挑战证伪"紧凑JSON省token"前提（实测反而多耗60~80%），用户裁定不做。蓝图文件 `docs/feature/K1e_AI调用点紧凑结构转换层_实施蓝图.md` 状态改 `DISCARDED`，保留作调研记录，不移交 CODE |
| K1i 现状 | **`BLUEPRINT_READY`**（`docs/feature/K1i_AI流式渐进展示_实施蓝图.md`）——起草时发现 backlog"AI记餐NDJSON先落地"这句话不成立：`SwitchableAiRuntime` 从未重写 `stream()`，全App（含AI记一餐）实际都没有真正的网络级流式到达。蓝图只做地基修复（让 `stream()` 真正委托给 `CloudAiRuntime` 的真实SSE实现），UI层扩展到AI推荐/生成菜品/健康建议显式弃置为独立未来批次（量级参考AI记一餐B1~B6）。GC-37独立挑战一轮，3项CONFIRMED-ISSUE已处置（含与本批L1的双向依赖：L1 §4.4 的"stream()不重写"注释需在K1i落地时同步删除，已在两份蓝图互相记录）。**排在 L1 之后实施**（`cloudAiConsentGranted()` 需已存在，否则走留桩分支） |
| ARCH 下一步 | 用户已定序 L1→K1e(废弃)→K1i(已就绪)→**K1h**（自动添加算法调研，纯调研非蓝图，无需GC-37/BLUEPRINT_READY流程）。ARCH 现在做 K1h 调研 |
| K1b 蓝图现状（不变） | `docs/feature/AI记一餐_K1b膳食健康评价逐成员化_实施蓝图.md`，状态 `DRAFT·PARKED`，等 L1/K1e/K1i/K1h 这条主线收尾后再拾起处置 §10 已挑出的问题，不重新起草 |
| AI快捷记一餐真机验证（不变，仍未核实进度） | `真机待验证清单_202608081130.md` 里 E-B4-*/E-B5-*/E-B6-*/E-K1A-01/E-CFG-01~06 近 30 项进度仍待用户确认，与本轮 L1/K1e/K1i 工作并行，不阻塞 |

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

本轮执行模型记录（供跨模型能力评估）：

| 轮次 | 角色 | 模型 |
|------|------|------|
| 第一轮（AF-B456-01~09 全部关闭） | Coder@副机 | 未知（commit `234539aa` 未记录） |
| 第二轮（AF-B456-05 关闭） | Coder@副机 | deepseek-v4-pro（1M context） |
| 三次复核 + K1a 蓝图起草 | 架构师@主力机 | claude-sonnet-5 |
| K1a 蓝图 GC-37 独立挑战 | 独立挑战 agent | claude-opus-5 |
