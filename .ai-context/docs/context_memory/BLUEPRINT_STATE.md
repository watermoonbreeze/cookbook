# BLUEPRINT_STATE

唯一握手状态文件。开工前先 `git pull` 读本文件；`TURN` 不是自己 → 停手，只报告当前持球方，不改代码。开工前除看 `TURN`，还须看 `颗粒度` 行确认本批蓝图应达到的级别。完成本方动作后在同一提交内更新本文件再 push。

**ARCH/CODE 命名规则**：`ARCH`/`CODE`/`REVIEW`/`TURN` 这几个协议字段只写角色名+机器标识（如 `架构师@主力机`、`Coder@副机`），**禁止出现具体模型名称**（Claude/DeepSeek/GPT 等）——角色定义是抽象的，具体由哪个模型担任取决于当前会话，协议逻辑不依赖模型身份。

**模型执行力评估台账（2026-08-07 新增，与上条不冲突）**：独立文档 `docs/experience/14_模型执行力评估.md`，与本文件的抽象角色字段完全分离。CODE 完成本批交付时，去该文档追加一行记录（含实际模型名）；ARCH 复核后补简评。**本文件不重复该表**，避免同一数据两处维护。

## CODE 入口（第二轮，本批必读，按顺序）

1. `docs/context_memory/架构模型复核报告_B4B5B6_2026-08-07.md` **§八（二次复核）** —— AF-B456-01~04/06~09 已确认关闭，不必再碰；`AF-B456-05` 仍未关闭，§8.2 给出新证据+唯一最小修复。
2. `docs/feature/AI记一餐_周期记_NDJSON流式_B4输入UI实施蓝图.md` **§3.5.1（二次复核修订，GC-36）** —— 本轮唯一要做的事：`GenerationProgress.segmentStatuses` 类型改 `List<StreamSegmentState?>`（null=未开始），`computeProgress()`/`submit()` 去掉 `?: StreamSegmentState.STREAMING` 兜底，`SegmentProgressBar` 补 `null -> PENDING` 分支；新增 4 条测试 T-B5-01~04。§0.1 表里 GC-17/22/24 三行已标回"未满足·CODE 待办（第二轮）"。
3. 关闭后：§9.4 补齐"当次结果"列（此前第一轮遗漏，一并补）→ §0.1 GC-17/22/24 改回满足 → 蓝图头状态改回 `ACCEPTED` → 本文件 `TURN` 改回 `ARCH` → 同一提交 `git push`。
4. 遇到不清楚的点 → 按 `~/.ai-context/rules/blueprint_protocol.md` §3 记 `Q-B4-NN`，不得自行发挥。
5. **台账纪律（本轮新增，见 GC-24 复发计数 1，已转"审查必查"）**：STEP 勾销表 Evidence 列只能填**真实存在**的测试/commit，不得引用尚未创建的 T-ID——上一轮的教训是引用了 T-B5-01~03 等 ID 但代码库里根本没有对应测试文件。
6. **交付时**：在 `docs/experience/14_模型执行力评估.md` 追加一行，据实填写本批实际使用的模型名（第 5 条以外唯一允许写具体模型名的地方）。

| 字段 | 值 |
|---|---|
| 任务/批次 | AI记一餐 周期记 NDJSON流式 / B4+B5+B6 |
| 蓝图文件 | `docs/feature/AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md`、`..._B4输入UI实施蓝图.md`（B5 无独立蓝图，见复核报告 §3.1，须补 LITE 追认件） |
| 规模 | BLUEPRINT-FULL |
| **颗粒度** | **L7**（项目基线 · 36 条 GC，本轮新增 GC-36 · 定义见 `experience/12_多模型协作与实施蓝图规范.md` §12 · 升级历史见 §13 · 本批逐条勾销见 B4 蓝图 §0.1） |
| 状态 | REVIEWED_BLOCKED（第二轮：ARCH 二次复核确认 AF-B456-01~04/06~09 已关闭，`AF-B456-05` 未关闭，转回 CODE） |
| **TURN** | **CODE** |
| ARCH | 架构师@主力机 |
| CODE | Coder@副机 |
| REVIEW | =ARCH |
| 基线 commit | `234539aa`（第一轮 CODE 完成点，AF-B456-01~04/06~09 已确认关闭） |
| 复核报告 | `docs/context_memory/架构模型复核报告_B4B5B6_2026-08-07.md` §八（**二次复核未通过**：`AF-B456-05` 未关闭 + T-B5-01~04/T-B4-08~10 测试缺失 + §9.4 未填） |
| 未闭合 | ❌ `AF-B456-05`（`segmentStatuses` 值域覆盖不全，"未开始"段被兜底成 STREAMING）。其余 8 项已确认关闭（含实跑 Shared/Android 测试 + assembleDebug 复验，非采信 commit message）。 |
| 末次更新 | 待填 commit · 2026-08-07 下午（架构师@主力机：二次复核，逐 diff 核对 8 项通过 + 实跑三条构建命令复验；`AF-B456-05` 发现新形态复发（GC-36/BL-12），给出唯一最小修复+4条新测试，转回 CODE。蓝图 §0.1/§3.5.1、复核报告 §八、`12_多模型协作与实施蓝图规范.md`§2/§12/§13 已同步更新。） |
