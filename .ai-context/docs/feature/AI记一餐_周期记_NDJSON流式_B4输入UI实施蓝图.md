# AI记一餐：周期记 + NDJSON 流式 B4 输入 UI 实施蓝图

> 状态：`REVIEWED_BLOCKED → 补丁中（第二轮）` —— 2026-08-07 架构模型复核（覆盖 B4+B5+B6）未通过，9 项阻断 `AF-B456-01~09`；同日二次复核 8 项已关闭，**AF-B456-05 新形态复发，仍未关闭**，详见 `docs/context_memory/架构模型复核报告_B4B5B6_2026-08-07.md` §八。CODE 在本蓝图基础上打第二轮补丁，不重新起草。
> **颗粒度：L7**（项目基线；GC 条款清单与书写红线见 `experience/12_多模型协作与实施蓝图规范.md` §12；升级历史见 §13）。**§0.1 是本次补丁的入口——先读它，逐行对照落点章节。AF-B456-05 这一行的落点已改为 §3.5 末尾的"二次复核修订"小节。**
> **🔴 架构模型复核检查点**：第一轮已关闭 AF-B456-01~04/06~09（8 项），第二轮（本次）唯一待关闭项 = `AF-B456-05`（进度圆点把"尚未开始"的段误标成"流式中"）+ 配套 4 条新测试 + §9.4 映射表补填。关闭后交回 ARCH 做三次复核（范围仅限 AF-B456-05 + 新增测试，不重查已通过项）。
> 起草日期：2026-08-06；第一轮补丁：2026-08-07 上午；第二轮补丁：2026-08-07 下午
> 前置蓝图：`AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md`（AF-ARCH-01/02 已关闭，AF-ARCH-03 由本蓝图 §1 冻结）
> 必读：`AI记一餐_周期记_NDJSON流式开发规范.md` §2.1/§5.3、`AI记一餐_周期记_NDJSON流式改造落地方案.md` §四/§五、`experience/12_多模型协作与实施蓝图规范.md`（含 §12 颗粒度分级、§13 升级历史）、B3 蓝图 §11.7（ChatGPT 复核 4 项门禁）、`苹果风格UI设计方案.md` §九、`架构模型复核报告_B4B5B6_2026-08-07.md`（**本次补丁的事实依据，必读**）
> 基线：`a7fdf074`（B1+B2+B3 全部生产代码 + 645 tests 0 failures）；本次补丁基线：`ac664fa1`（B4+B5+B6 编码完成点）

---

## §0. B4 编码前门禁（ChatGPT 复核要求，逐项勾销）

| # | 门禁 | 本蓝图落点 | 状态 |
|---| ---- | --------- | :--: |
| 1 | B4 蓝图第一步冻结 AF-ARCH-03：N 次独立请求 × 每次 1 段 | §1 | ✅ 已冻结 |
| 2 | 原审核 S1~S6 逐条写入蓝图，注明采纳方案或不采纳原因 | §8 | ✅ 已逐条处理 |
| 3 | 确认 645 项测试结果运行在完整 SHA `a7fdf074...` 上 | — | ✅ 已确认（见 SESSION_交接.md） |
| 4 | AF-ARCH-02 边界检查表（7 项）写入 B4 蓝图不变量 | §2 不变量表 | ✅ 已纳入 |

---

### §0.1 颗粒度勾销表（GRANULARITY = L7 · 2026-08-07 补丁新增，CODE 从这里开始读）

> **怎么用这张表**：每行是一条 GC（Granularity Clause，防呆条款）。「本蓝图落点」列指向具体章节/表格，去那里能看到该条款要求你做的事和判据；「状态」列 `满足` 表示本蓝图已冻结、CODE 照做即可，`未满足·CODE 待办` 表示这正是你这次要关闭的阻断项，`N/A` 表示本批不触碰对应风险域。**任何一行标"未满足"未被处理，不得把本蓝图状态改回 `ACCEPTED`。**

| GC | 条款一句话 | 本蓝图落点 | 状态 |
|---|---|---|:--:|
| GC-01 | 分支写成 条件→唯一动作→禁止动作，歧义词零命中 | §1/§3/§7 全文 | 满足 |
| GC-02 | allowlist 表：文件×允许操作×禁止操作 | §2 Allowlist | 满足 |
| GC-03 | 上一批延后项归宿（转本批 ID / 显式弃置+理由） | §10（已补归宿列） | 满足 |
| GC-04 | 每条 INV 五列齐全，证据列禁空 | §3.1/§3.4/§3.5/§3.6 | 满足 |
| GC-05 | INV↔T 双向映射表 | §9.4（新增） | 满足 |
| GC-06 | 放行条件写命令原文，台账当次+分模块 | §11.1 | 满足 |
| GC-07 | 测试夹具职责边界（fake 只制造外部原因） | §9（沿用 B3 §10.1 固定测试夹具规则） | 满足 |
| GC-08 | 真机清单文件名+编号区间登记 | §13（须补 B6 分组，见 AF-B456-08） | ✅ 满足（2026-08-07 二次复核核实：`真机待验证清单_202608071200.md` 单文件，B6 分组 7 项齐全） |
| GC-09 | 不得失败的既有测试套件全名（回归基线锁定） | §7 步骤 2.4 | 满足 |
| GC-10 | 逐字段真相源表 | §7 步骤 2.0（新增） | ✅ 满足（二次复核逐点核对 W1~W6，`grep "inputText ="` 赋值形零命中） |
| GC-11 | 重叠字段写入点迁移清单 | §7 步骤 2.1（新增） | ✅ 满足（同上） |
| GC-12 | UI 判据/业务判据同源表 | §7 步骤 2.2（新增） | ✅ 满足（`enabled = state.quickDraftText.isNotBlank()` 与 `submit()` 同源） |
| GC-13 | fallback 复用主路径校验入口 | — | N/A：本批无 fallback 新增 |
| GC-14 | 对象生命周期表 | §5.8（新增） | ✅ 满足（`activeRecognizer` 已改 `MutableState` 传递，释放点/触发条件核对通过） |
| GC-15 | 跨 Composable 传递可变持有物声明传递形态 | §5.8（新增） | ✅ 满足（`QuickInputSection` 形参类型确认为 `MutableState<VoiceRecognizer?>`） |
| GC-16 | 搬迁代码块须列历史修复注释清单 | §5.8（新增） | ✅ 满足（历史修复注释、四态图标、脉冲动画均已核对恢复） |
| GC-17 | 列表逐项状态由数据层产出 `List<Status>` | §3.5（新增，**2026-08-07 二次复核追加"未开始"修订，见 §3.5 末尾**） | **❌ 未满足·CODE 待办（第二轮）**——`segmentStatuses` 已按字面产出，但"尚未开始"的段被兜底成 `STREAMING`（与"流式中"同值），不是真实的逐项状态，见架构模型复核报告 §8.2 |
| GC-18 | 序号字段标注索引空间 | §3.5（新增） | ✅ 满足（`currentSegmentIndex` 命名与取值已核实为显示下标，索引空间错配已解决） |
| GC-19 | 集合过滤/消费链路画出来 | §3.5（新增） | ✅ 满足（`allSegments--filter-->nonBlank--sortedBy-->显示序` 链路与代码一致） |
| GC-20 | 自动副作用清单表 | §3.6（新增） | ✅ 满足（快速记+周期记两处截断分支均弹 Snackbar，去重/复位规则核对通过） |
| GC-21 | INV 写"提示"必须有对应 STEP 落点 | §7 步骤 3.5（新增） | ✅ 满足 |
| GC-22 | 可见副作用配 T-ID/真机项 | §9.4 + §13 | **未满足·CODE 待办（第二轮）**——真机项（`E-B6-TRUNC-01` 等）已登记，但 T-B5-01~04 尚未新增，§9.4"当次结果"列仍空 |
| GC-23 | 实施脚本最小动作独立编号 STEP-ID | §7 全节（改写为 STEP 编号） | 满足（蓝图文本已具备，属 ARCH 产出，非 CODE 待办） |
| GC-24 | 交付台账 STEP 勾销表 | §7 步骤 6（新增） | **部分满足**——已填写但 Evidence 列引用的测试 ID 实际不存在（见架构模型复核报告 §8.4），第二轮须补真实测试后重新勾销 |
| GC-25 | 字面量完成形态 + grep 判据 | §7 步骤 3.4（标题文案） | ✅ 满足（`grep "AI 周期记一餐"` 命中） |
| GC-26 | 冻结值修订记录表 | §1 末尾（新增） | ✅ 满足（maxTokens 依据+影响评估已补） |
| GC-27 | 编辑类新入口必须核对是否路由收口函数（本项目=`invalidateGenerationToInput`） | §7 步骤 2.1 W5 行 | ✅ 满足（`setQuickDraft()` 已内部调用 `invalidateGenerationToInput`） |
| GC-28 | 构造时单例/全局字段在基数 1→N 场景须显式回答是否按基数分片 | — | N/A：本批不新增此类对象；已有实例（parser）在 B3 已按 AF-ARCH-02 解决 |
| GC-29 | 多来源写入同一聚合 key 须显式声明合并/覆盖 | — | N/A：本批不改 mapper |
| GC-30 | 状态转移分支须驱动完整声明的副作用链 | §7 步骤 3.5（截断提示分支） | ✅ 满足 |
| GC-31 | 挂起点清单 + 恢复后重新校验身份 | — | N/A：本批 `submit()`/`invalidateGenerationToInput` 无新增挂起点，沿用 B3 `isCurrentGeneration` 机制 |
| GC-32 | 高频事件触发 IO 须显式节流策略 | — | N/A：本批（B4 输入 UI）不改 preview 触发时机，归属 B5 追认件（见 AF-B456-05 关闭后一并处理） |
| GC-33 | 禁止生产类为测试暴露可变全局注入点 | — | N/A：本批不改 `sessionPort` 注入方式（S5 已裁决降级为技术债） |
| GC-34 | 注释/KDoc 与实现同步 | §1 末尾冻结值表；全文修改点 | 满足（要求随每条 STEP 一并核对，非独立新增表） |
| GC-35 | 协议双端事件枚举逐项对照，内部诊断默认不透传用户 | — | N/A：本批不改协议层（NDJSON 事件消费已在 B1/B3 冻结） |

---

## §1. AF-ARCH-03 冻结：请求段数策略

### 决策

**采用「N 次独立请求 × 每次 1 段」。**

### 理由

1. **与 AF-ARCH-02 一致**：每个 segment 已有独立 parser（`segmentParsers: LinkedHashMap`），每次请求只传 1 段 → parser 的 `segments.size` 恒为 1 → 整体 JSON fallback 始终有效。
2. **支持按段重试**：某段失败可单段重试，不影响已完成段。
3. **支持按段进度**：UI 可展示"第 3/7 天生成中"。
4. **失败隔离**：一段的截断/解析错误不污染其他段。
5. **取消粒度**：用户可中途取消剩余段，已生成内容保留。

### 具体动作（本蓝图 §5 实施脚本第 0 步，B4 编码第一步）

| 动作 | 文件 | 内容 |
|------|------|------|
| 收窄 `buildStreamingRequest` 签名 | `AiMealPrompt.kt` | `fun buildStreamingRequest(segment: InputSegment): LlmRequest`（单段，非 List） |
| 删除多段 prompt 分支 | `AiMealPrompt.kt` | 删除 `buildStreamingUserPrompt` 中 `if (segments.size == 1) ... else ...` 的 else 分支（周期记多天拼装逻辑） |
| 删除 maxTokens 缩放 | `AiMealPrompt.kt` | `maxTokens` 固定为 2048（单段），删除 `nonBlank.size * 4096` 缩放 |
| 更新调用点 | `AiMealInputViewModel.kt` | `AiMealPrompt.buildStreamingRequest(seg)` 已传单段，仅需确认编译通过 |

### 不变

- `StreamingMealSession` 的 `orderedSegments` 遍历逻辑**不改**——它已经正确处理多段串行（`nextSegment()` 逐个取、`while` 循环逐个请求）。
- ViewModel 的 `submit()` 循环**不改**——它已经是"每段一次 `aiRuntime.stream()` + `collect`"。
- 唯一变化是 `buildStreamingRequest` 不再接受 `List`，调用方本就传 `listOf(seg)`，改为传 `seg` 即可。

### 冻结值修订记录表（GC-26 · 2026-08-07 补丁新增）

> 本蓝图冻结的常量/阈值发生变更时，登记本表；无记录的变更视为缺证据（复核报告 §3.2）。

| 冻结值 | 旧值 | 新值 | 日期 | commit | 依据 | 影响评估 | 状态 |
|---|---|---|---|---|---|---|:--:|
| `maxTokens`（单段请求） | 2048 | 4096 | 2026-08-07 | `ac664fa1` | 「单日 3 餐 × ~10 菜 NDJSON 输出超 2048 致 `finish_reason=length`」——B6 commit 日志确认截断，可复现输入：「周一\n早餐：小米粥（小米）鸡蛋（鸡蛋）馒头（面粉）\n午餐：红烧肉（猪肉）清蒸鱼（鲈鱼）炒青菜（上海青）番茄蛋汤（番茄鸡蛋）米饭\n晚餐：糖醋排骨（猪小排）炒豆角（豆角）凉拌黄瓜（黄瓜）紫菜汤（紫菜）馒头」≈2400 tokens 输出，2048 时截断丢失最后1-2道菜 | 单次请求 token 上限翻倍（2048→4096），成本约×2（按 DeepSeek 定价约 +¥0.002/次），延迟+0.5~1.5s；仅长输入（≥3餐×≥8菜）触发，日均调用量下影响可忽略 | ✅ 满足（2026-08-07 Coder@副机 补证据） |

**CODE 动作**：在关闭 AF-B456-01~09 的同一批次，把上表"待办"两格补齐（贴 `finish_reason=length` 的日志片段或给出可复现的输入样例；给出成本/延迟影响的一句话结论，例如"单次请求成本上限 ×2，但仅在长输入触发，日均调用量下影响可忽略"或实测数据），状态改 `满足`。

---

## §2. 目标、非目标与范围冻结

### 目标

将 `AiMealInputSheet` 的输入区从"单一文本输入框"改造为"**快速记 / 周期记**双模式输入"，支持周期记的连续日期段选择与每日独立输入。`AiMealInputViewModel.submit()` 从"构造单个 quick segment"扩展为"按输入模式构造 1 个或 N 个 segment"。

### 非目标

- **B5 确认页流式展示**（渐进卡片、进度条、截断/失败诊断、最终重排）——B5 另立蓝图
- **B6 收尾**（文档、真机清单、全量审查）
- 数据库 schema、SQLDelight 迁移
- DI/Koin 重构
- 健康建议行为变更
- 语音输入行为变更（快速记/周期记共用既有语音逻辑）
- Prompt 协议变更（已在 §1 AF-ARCH-03 中完成签名收窄，不改变 NDJSON_SYSTEM_PROMPT 内容）
- `StreamingMealSession` / `StreamingMealParser` 生产代码（已在 B3+AF-ARCH-02 中就位）

### Allowlist（仅允许修改的文件）

| 文件 | 允许操作 | 禁止操作 |
|------|---------|---------|
| `shared/.../ai/meallog/InputSegmentFactory.kt`（**新建**） | 纯函数：`forQuickRecord()` / `forPeriodicRecord()` / `mondayOfWeek()` | 访问 UI、Repository、数据库、网络 |
| `shared/.../ai/meallog/InputSegmentFactoryTest.kt`（**新建**） | 测试周一边界、跨年周、空白段过滤、charCount | — |
| `shared/.../ai/meallog/AiMealPrompt.kt` | 收窄 `buildStreamingRequest` 签名为单段 + 删多段分支 | 改 NDJSON_SYSTEM_PROMPT 内容、改 FLAT_SYSTEM_PROMPT |
| `androidApp/.../ui/ai/AiMealInputViewModel.kt` | 新增 `InputMode` 字段到 UiState、`submit()` 按模式构造 segments、`setInputMode()`/`setWeekInputText()` 等新动作 | 改 B3 的 generation 编排/状态机/保存/健康摘要逻辑 |
| `androidApp/.../ui/ai/AiMealInputSheet.kt` | InputPhase 内部重构：加 SegmentedControl、周期记日期列表+每日输入框、200 字限制 UI | 改 ParsingPhase/PreviewPhase/保存/关闭守卫 |
| `androidApp/.../test/.../AiMealInputViewModelStreamTest.kt` | 新增周期记多段测试 | 改既有 T-B3-01~09 |
| `shared/.../test/.../InputSegmentFactoryTest.kt`（**新建**） | 工厂纯单测 | — |
| `.ai-context/docs/...` | 更新 B4 台账、真机清单 | — |

**禁止修改：** `StreamingMealSession`、`StreamingMealParser`、`MealStreamDraftMapper`、`CloudAiRuntime`、`StreamTransport`、`CloudAiRequestConfig`、Repository、SQLDelight、Gradle、DI、B1/B2/B3 已有测试。

---

## §3. 不变量

### 3.1 B4 新增不变量

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|----|------|---------|---------|------|
| INV-B4-01 | 用户选择"周期记"模式 | 展示连续日期列表（默认当前周 Mon~Sun），每天一个独立输入框，各 ≤200 字 | 共用同一个输入框、日期不可选 | T-B4-01 |
| INV-B4-02 | 周期记模式点击发送 | 仅为非空白段构造 `InputSegment`（`segmentId="week-{weekAnchor}-day{N}"`），按 `ordinal` 升序排列；`weekAnchor` = 所选周周一 | 发送空白段、segmentId 格式错误、ordinal 乱序 | T-B4-02 |
| INV-B4-03 | 快速记模式点击发送 | 构造单个 `InputSegment`（`segmentId="quick-{targetDate}"`），行为与 B3 完全一致 | 周期记格式的 segmentId | T-B4-03 |
| INV-B4-04 | 切换输入模式（快速记 ↔ 周期记） | 各自保留草稿不丢失；切换回原模式时恢复原输入 | 切换清空对方草稿 | T-B4-04 |
| INV-B4-05 | 周期记某天输入超过 200 字 | 截取前 200 字（`take(200)`），并给出一次性可见提示 | 静默截断、阻止输入、发送超限文本 | T-B4-05 |
| INV-B4-06 | 粘贴内容到周期记某天输入框 | 超过 200 字截取并提示；未超限直接粘贴 | 静默截断 | T-B4-05 |
| INV-B4-07 | 周期记所有天均为空白时点击发送 | 发送按钮禁用（与快速记一致：`inputText.isBlank()` → 不可发送） | 发送空请求 | T-B4-02 |
| INV-B4-08 | `InputSegmentFactory` 被调用 | 纯函数，无副作用；segmentId 格式固定、weekAnchor 计算仅依赖传入参数 | 读系统时间、访问 DB/网络 | T-B4-02 |

### 3.2 B3 不变量（不变，B4 不得破坏）

B3 蓝图 INV-B3-01~08 全部保持。B4 仅改变 segment 的**构造方式**（从 VM 内联 `InputSegment(...)` → 经 `InputSegmentFactory`），不改变 session/mapper/Runtime 合同。

### 3.3 AF-ARCH-02 边界检查表（ChatGPT 复核，B4 编码验收条件）

| # | 检查项 | B4 如何保证 | 验收 |
|---| ------ | ---------- | ---- |
| 1 | 每个 parser 构造时只传入一个 `InputSegment` | `StreamingMealSession.segmentParsers.getOrPut(seg.segmentId) { StreamingMealParser(segments = listOf(seg), ...) }` —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 2 | delta 只进入当前 segment 的 parser | `session.onDelta(seg.segmentId, event.text)` —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 3 | `finish_reason=length` 只标记当前 parser | 每段 `onCompleted` 只 finish 自己的 parser —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 4 | parser 内的残余 buffer 不跨 segment | 每段独立 parser 实例 —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 5 | snapshot 合并顺序与原 segments 顺序一致 | `segmentParsers` 是 `LinkedHashMap`，按 `orderedSegments` 的 `forEach` 遍历合并 —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 6 | 多次调用 snapshot 不会重复累计结果 | `snapshot()` 每次重新遍历 parser 列表构建新 `MealStreamDraft`，不缓存 —— B3 已实现，B4 不动 | 既有 session 测试保持通过 |
| 7 | `segmentId` 重复时有明确行为 | 本蓝图 §3.4 冻结 | 见下方 |

### 3.4 segmentId 唯一性不变量（B4 新增）

| ID | 条件 | 必须结果 | 证据 |
|----|------|---------|------|
| INV-B4-09 | 同一 `StreamingMealRequest` 内的 `segments` 列表 | 所有 `segmentId` 必须唯一 | T-B4-02 |
| INV-B4-10 | `InputSegmentFactory.createWeekSegments()` | 生成的 7 个 segmentId 互不相同（`week-{anchor}-day1` ~ `week-{anchor}-day7`） | T-B4-02 |
| INV-B4-11 | 快速记 segmentId 与周期记 segmentId | 格式不同（`quick-*` vs `week-*`），不会碰撞 | T-B4-03 |

> **建议**：在 `StreamingMealRequest` init 块加 `require(segments.map{it.segmentId}.distinct().size == segments.size) { "segmentId 重复" }` fail-fast。此为 🟡 建议（非阻断），若实施则需在 B3 session 测试中补"重复 segmentId 抛异常"用例。
>
> **CODE 待办（复核报告 §三.3.3）**：上述 fail-fast 已在代码里实施（`shared/.../ai/meallog/InputSegment.kt` `StreamingMealRequest.init`），但配套的"重复 segmentId 抛异常"用例未补——在 `StreamingMealSessionTest.kt` 补 `assertFailsWith<IllegalArgumentException> { StreamingMealRequest(segments = listOf(seg, seg.copy()), ...) }`。

### 3.5 逐项状态与索引空间不变量（GC-17/18/19 · 2026-08-07 因 AF-B456-05 追加 · CODE 关闭 AF-B456-05 的冻结锚点）

> 背景：B5 阶段 `SegmentProgressBar` 用 `completedSegments`/`currentSegmentOrdinal` 等标量反推逐段圆点状态，`ordinal`（业务序号，过滤前 0..6）与圆点 `index`（显示下标，过滤后 0..totalSegments-1）在有空白天时不相等，导致 ACTIVE 脉冲点永不出现、成败天对调显示；三角色审查曾"修复"过一次（`3f60c20f`）但只是换了个仍然跨索引空间比较的字段，未解决根因。本节是关闭 `AF-B456-05` 的唯一冻结依据，CODE 必须按此重写，不得重复上次那种"换字段名但仍标量反推"的做法。

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-B456-R05a | UI 需要按段渲染独立状态（进度圆点） | `GenerationProgress` 暴露 `segmentStatuses: List<StreamSegmentState>`，`size == totalSegments`，顺序 == 显示顺序（`nonBlank.sortedBy { it.ordinal }`）；`SegmentProgressBar` 只做 `segmentStatuses[index]` → `DotState` 的直接映射 | UI 用 `completedSegments`/`terminalSegments` 与 `index` 比较反推逐项状态 | T-B5-02 |
| INV-B456-R05b | 任何整型序号字段被 UI 消费 | 命名后缀标明索引空间；`currentSegmentOrdinal` 重命名为 `currentSegmentIndex`，取值 = `nonBlank.indexOfFirst { it.segmentId == current.segmentId }` | 跨索引空间直接比较（如 `index == currentSegmentOrdinal`） | T-B5-01 |
| INV-B456-R05c | 段集合被"非空白"过滤后消费 | 数据流写出 `allSegments(7) --filter{!isBlank}--> nonBlank(N) --sortedBy{ordinal}--> 显示序`，UI 唯一消费对象 = 显示序列表 | UI 消费过滤前列表的长度或下标 | T-B5-01 |

**索引空间对照表（GC-18 必填）**

| 字段 | 索引空间 | 取值域 | 转换到显示下标 |
|---|---|---|---|
| `InputSegment.ordinal` | 业务序号（周内第几天） | 0..6（过滤前，**不因过滤重编号**） | `nonBlank.indexOfFirst { it.ordinal == x }` |
| `GenerationProgress.currentSegmentIndex` | 显示下标 | `0..totalSegments-1` | 即显示下标 |
| `GenerationProgress.segmentStatuses` 的下标 | 显示下标 | 同上 | 即显示下标 |
| `SegmentProgressBar` `repeat(totalSegments){ index }` | 显示下标 | 同上 | 即显示下标 |

**反例锁定（GC-17 · 审查 grep 判据）**：`androidApp/.../ui/ai/` 内出现下列形态即判 AF——`index < progress.completedSegments`（隐含"成功项必排在失败项前"）、`index == progress.currentSegmentOrdinal`（跨索引空间比较）。

**新增测试**：T-B5-01（`{2:"周三",4:"周五"}` → `totalSegments==2 && currentSegmentIndex in 0..1`）、T-B5-02（段1 FAILED、段2 COMPLETED → `segmentStatuses == [FAILED, COMPLETED]`）、T-B5-03（全段 PENDING → `currentSegmentIndex==0`）。

#### 3.5.1 二次复核修订：`segmentStatuses` 缺"未开始"表达（INV-B456-R05d · 2026-08-07 二次复核追加，CODE 第二轮从这里开始）

> 背景：第一轮 CODE 精确实现了上面 3.5 的字面规格，但架构模型二次复核（`架构模型复核报告_B4B5B6_2026-08-07.md` §8.2）发现规格本身有空隙——`StreamSegmentState` 只有 `STREAMING/COMPLETED/FAILED` 三值，"尚未轮到"的段在 `computeProgress()` 里被 `states[seg.segmentId] ?: StreamSegmentState.STREAMING` 兜底成了 `STREAMING`，导致周期记 2 段以上时，所有未开始的段和真正在流的那段一起显示"脉冲中"。本节是唯一修订依据，取代 3.5 原表述中"`segmentStatuses: List<StreamSegmentState>`"这一点（其余 3.5 内容，含索引空间部分，不变、继续有效）。

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-B456-R05d | 段尚未被 `StreamingMealSession.nextSegment()` 触达（`segmentStates` 无该 segmentId 的 entry） | `GenerationProgress.segmentStatuses` 类型改为 `List<StreamSegmentState?>`；该段位置为 `null`，`SegmentProgressBar` 把 `null` 映射为 `DotState.PENDING`（空心圆） | 把"未开始"兜底成 `StreamSegmentState.STREAMING`（与"真正在流"同值不可区分） | T-B5-04 |

**唯一最小修复**（三处，均在 UI 层，不改 `StreamSegmentState` 枚举）：
- `GenerationProgress.kt`：`segmentStatuses: List<StreamSegmentState?> = emptyList()`。
- `AiMealInputViewModel.kt`：
  - `computeProgress()`：`val segmentStatuses = nonBlank.map { seg -> states[seg.segmentId] }`（删除 `?: StreamSegmentState.STREAMING` 兜底，直接透传 `null`）。
  - `submit()` 初始 `GenerationProgress`：`segmentStatuses = nonBlankSegments.map { null }`（会话尚未开始任何段，全部"未开始"）。
- `SegmentProgressBar.kt`：`when (segState) { null -> DotState.PENDING; StreamSegmentState.COMPLETED -> DotState.DONE; StreamSegmentState.FAILED -> DotState.FAILED; StreamSegmentState.STREAMING -> DotState.ACTIVE }`（穷尽 4 值，删除死 `else` 分支）。

**必须新增的测试**（新建 `androidApp/src/test/.../GenerationProgressTest.kt`，四条缺一不可，T-B5-01~03 语义不变但断言需按新类型调整）：
- T-B5-01：`{2:"周三",4:"周五"}` → `totalSegments==2 && currentSegmentIndex in 0..1`。
- T-B5-02：段1 FAILED、段2 COMPLETED（均已终态）→ `segmentStatuses == listOf(FAILED, COMPLETED)`。
- T-B5-03：会话构造后未调用任何 `nextSegment()` → `segmentStatuses == List(totalSegments){ null }` 且 `currentSegmentIndex==0`。
- **T-B5-04（新增，锁定本条回归）**：3 段中仅第 1 段已 `nextSegment()`（STREAMING），第 2/3 段未触达 → `segmentStatuses == listOf(StreamSegmentState.STREAMING, null, null)`。

**禁止扩大范围**：不得改 `StreamSegmentState` 枚举；不得改 `StreamingMealSession`/`segmentStates` 惰性写入时机；不得改进度条文案口径或圆点组件本体。

**关闭本条后**：`AF-B456-05` 在架构模型复核报告 §8.5 的判定改为"已关闭"，§0.1 表 GC-17/GC-22/GC-24 三行改回"满足"，§9.4 补齐"当次结果"列，`BLUEPRINT_STATE.md` 的 `TURN` 改回 `ARCH`。

### 3.6 自动副作用清单表（GC-20 · 2026-08-07 因 AF-B456-04 追加 · CODE 关闭 AF-B456-04 的冻结锚点）

> 背景：INV-B4-05/06 已要求截断"并给出一次性可见提示"，但 §7 实施脚本此前从未给出"提示"这一步的机械动作，CODE 严格按脚本做出了截断生效但完全静默的实现——这不是编码偏离，是蓝图本身的脚本空隙（GC-21 的由来）。本表 + §7 步骤 3.5 一起补齐这个空隙。

| 触发条件 | 用户可见载体 | 文案原文 | 去重与复位规则 | 透明 Tier |
|---|---|---|---|---|
| 快速记输入框单次输入/粘贴超过 `AiMealPrompt.MAX_INPUT_CHARS`（200）字 | `LocalAppSnackbar`（`AiMealInputSheet.kt` 已取得实例） | `"已截取前 200 字"` | `var truncNotified by remember { mutableStateOf(false) }`：同一输入框连续超限只提示一次；文本降到上限以下后复位为 `false` | T1（事后留痕，Snackbar 即可，不用 AlertDialog） |
| 周期记任一天输入框单次输入/粘贴超过 200 字 | 同上，`PeriodDayBlock` 新增可空回调参数 `onTruncated: (() -> Unit)? = null`（能力显隐由回调传入决定，非 mode 布尔） | 同上 | 同上，按天独立计数 | T1 |

---

## §4. 输入模型设计

### 4.1 InputMode（新增）

```kotlin
// 在 AiMealInputViewModel.kt 或 shared 中定义
enum class InputMode {
    /** 快速记：单输入框，最大 200 字 */
    QUICK,
    /** 周期记：按周/日期段，每天独立输入，最大 200 字/天 */
    WEEK,
}
```

放在 ViewModel 文件中（不跨模块使用），避免 shared 模块引入 UI 概念。

### 4.2 InputSegmentFactory（新建，shared）

> 设计来源：架构模型终审 S6（两位架构师独立一致结论）+ Google 架构工程师 B4 复审。

```kotlin
// shared/.../ai/meallog/InputSegmentFactory.kt

object InputSegmentFactory {

    /**
     * 快速记：单一 anchor 日期产生 1 个 segment。
     * segmentId = "quick-{targetDate}"
     */
    fun forQuickRecord(
        inputText: String,
        anchorDate: LocalDate,
    ): List<InputSegment> = listOf(
        InputSegment(
            segmentId = "quick-$anchorDate",
            targetDate = anchorDate,
            inputText = inputText.trim(),
            ordinal = 0,
        )
    )

    /**
     * 周期记：锚点日期所在周一为锚，产生该周 7 天（周一~周日）的 segments。
     * segmentId = "week-{weekAnchor}-day{1..7}"
     *
     * @param dayTexts 7 个字符串，index 0=Monday, ..., index 6=Sunday
     * @param weekAnchorDate 该周周一
     * @return 7 个 InputSegment（含空白段；由调用方通过 StreamingMealRequest.nonBlankSegments 过滤）
     */
    fun forPeriodicRecord(
        dayTexts: List<String>,
        weekAnchorDate: LocalDate,
    ): List<InputSegment> {
        require(dayTexts.size == 7) { "dayInputs must have exactly 7 elements" }
        return dayTexts.mapIndexed { index, text ->
            InputSegment(
                segmentId = "week-${weekAnchorDate}-day${index + 1}",
                targetDate = weekAnchorDate.plusDays(index),
                inputText = text.trim(),
                ordinal = index,
            )
        }
    }

    /**
     * 推算某日期所在周的周一（ISO 周一）。
     * 跨年周（如 2025-12-29 周一 → 周日 2026-01-04）正确处理。
     */
    fun mondayOfWeek(date: LocalDate): LocalDate {
        val dayOfWeek = date.dayOfWeek.ordinal  // 0=Mon, ..., 6=Sun
        return date.minusDays(dayOfWeek)
    }
}
```

**设计决策**：
- 快速记和周期记统一返回 `List<InputSegment>`（而非一个返回单个、一个返回列表）→ 调用方 `submit()` 的处理逻辑统一。
- 空白段保留在全量 7 段中 → `StreamingMealRequest.nonBlankSegments` 过滤（已有属性，不改）。
- `mondayOfWeek` 提取为 shared 公共函数 → 替代 ViewModel 私有的 `startOfWeek()`，消除双真相源风险（`weekAnchor` 与 `segmentId` 命名规则由同一工厂保证一致）。

**为什么下沉 shared**：
- 纯函数、无平台依赖 → 自然属于 commonMain
- `mondayOfWeek` 可被 shared 单测覆盖（跨年周、1月1日周一、12月31日所在周等边界）
- segmentId 命名规则 + weekAnchor 推算 + ordinal 排序在单一工厂内保证一致，消除当前 ViewModel 手动拼字符串的漂移风险
- 符合准则 B（共享逻辑不共享像素）

### 4.3 ViewModel 中的草稿管理

> 字段命名和交互行为由 Apple UX 设计师确定（2026-08-06）。

```kotlin
// AiMealInputUiState 新增/调整字段
data class AiMealInputUiState(
    // ... 既有字段不变 ...
    
    /** 当前输入模式（替代旧 InputMode.TEXT/VOICE，语音状态由 voiceState 独立表达） */
    val inputMode: InputMode = InputMode.QUICK,
    
    /** 快速记草稿（切换模式保留） */
    val quickDraftText: String = "",
    
    /** 周期记周锚点（所选周周一），默认当前日期所在周周一 */
    val periodWeekMonday: LocalDate = startOfWeek(today),
    
    /** 周期记选中日期范围（0..6 表示周一至周日），默认全选 7 天 */
    val periodSelectedRange: IntRange = 0..6,
    
    /** 周期记各天草稿（key=0..6=Mon..Sun，value=文本），仅保留有内容的天 */
    val periodInputs: Map<Int, String> = emptyMap(),
)
```

**设计决策（来自 UX 设计师）**：
- **`inputText` 保留但变更为计算属性**：快速记时 = `quickDraftText`，周期记时 = ""。`submit()` 内部按模式直接用 `quickDraftText` 或 `periodInputs`。
- **语音按钮仅在快速记模式显示**：周期记多输入框环境中语音指向不明确（需先点某输入框再长按语音 = 两步操作，不如直接打字）。周期记场景偏向"坐下来批量规划"而非"快速捕捉"。
- **标题文案跟随模式切换**："AI 快捷记一餐" vs "AI 周期记一餐"（双重编码：分段滑块+标题，消除迷惑）。
- **`InputMode` 枚举调整**：`QUICK, PERIOD`（`TEXT, VOICE` 语义已由 `voiceState` 覆盖）。

---

## §5. UI 设计（交互规范）

> 本节由 Apple UX 设计师产出（2026-08-06），包含精确布局 dp/sp、颜色、动效、适老化要求。编码时以此为唯一 UI 真相源。

### 5.1 快速记模式（基准态）

与 B3 基本一致，仅新增字符计数 + 发送按钮改用 CapsuleButton：

**改动点**：
- 输入框右下角内嵌字符计数 `"45 / 200"`（`bodySmall` 12sp），<180 灰色 → 180-199 琥珀 `#E0A23C` → ≥200 错误红 `#D14E3B` + 拒绝输入
- 粘贴超 200 字截断 + Snackbar "已截取前 200 字"
- 发送按钮从 Material `Button` 改为 **`CapsuleButton`**（全 App 主 CTA 统一，§9.10/§9.13）
- 语音按钮保留（仅快速记）

### 5.2 周期记模式（新增）

#### 5.2.1 模式切换

```
┌── ModalBottomSheet ──────────────────────────────┐
│ ✨ AI 周期记一餐                          (i)    │  ← 标题跟随模式
│                                                   │
│ ┌───────────────────────────────────────────────┐ │
│ │       快速记      │     周期记               │ │  ← SegmentedControl
│ └───────────────────────────────────────────────┘ │
│                                                   │
│ ┌── WeekStrip（7天日期段选择器·签名元素）──────┐  │
│ │  一    二    三    四    五    六    日       │  │
│ │  ██    ██    ██    ██    ██    ░░    ░░      │  │  ██=accent-container 赤陶橘
│ │   4     5     6     7     8     9    10      │  │  ░░=surfaceVariant 浅灰
│ └──────────────────────────────────────────────┘  │
│ 已选 5 天 · 周一至周五                            │
│                                                   │
│ ┌ 周一 8月4日 ─────────────────────────────────┐ │
│ │ ┌──────────────────────────────────────┐ 12/200│ │  ← PeriodDayBlock
│ │ │ 今天早餐吃得比较清淡…                  │       │ │  heightIn(100,160dp)
│ │ └──────────────────────────────────────┘       │ │
│ └───────────────────────────────────────────────┘ │
│ ... (已选天 × N，AnimatedVisibility 展开/收起)    │
│                                                   │
│ ┌───────────────────────────────────────────────┐ │
│ │           📤  发送（CapsuleButton）            │ │
│ └───────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────┘
```

#### 5.2.2 WeekStrip（新建组件·签名元素）

**文件**：`androidApp/.../ui/ai/WeekStrip.kt`

| 属性 | 取值 |
|------|------|
| 整体宽度 | `fillMaxWidth`，铺满 sheet 内容区 |
| 天格 | 7 格均分 `Modifier.weight(1f)`，高 `56dp`（≥触达底线 48dp + padding） |
| 天格间距 | `horizontalArrangement = Arrangement.spacedBy(0.dp)`，视觉间距由每格 `padding(horizontal = 1.5.dp)` 产生 |
| 选中天格 | `primaryContainer` 底色 + 文字 `onSurface`（深色模式自适配）+ 字重 `Semibold` |
| 未选天格 | `surfaceVariant` 底色 + 文字 `onSurfaceVariant` + `Normal` 字重 |
| 连续选中段 | 相邻选中格无缝连成 `primaryContainer` 色条（头尾 `RoundedCornerShape(10dp)`，中间格 `RoundedCornerShape(0)`） |
| 天格内部 | `Column`：上周几标签（`labelMedium` 13sp）+ 下日期数字（`bodyMedium` 15sp），居中 |
| 今天标识 | 日期数字始终 `primary` 字色 + `Semibold`（无论该格是否选中；全选态时今天底色与其他天一致，仍需要"我在哪"的定位锚） |
| 范围描述行 | 天格下方 4dp：`"已选 X 天"`（`labelSmall` 12sp, `onSurfaceVariant` 色，居中） |

**交互**（点击端点设置范围）：
- **默认**：全部 7 天选中（当前日期所在周的周一至周日）
- **点未选中天（相邻）**：扩展范围到该天
- **点未选中天（不连续）**：重置范围为仅该天
- **点选中段左端天**：左端点右移一天（取消最左）
- **点选中段右端天**：右端点左移一天（取消最右）
- **点选中段中间天**：无操作
- **仅剩 1 天时点它**：无操作（不可取消最后一天，最少 1 天）

**动效**：选中段背景 `animateColorAsState` 300ms `tween`；输入块出现/消失用 `AnimatedVisibility(expandVertically + fadeIn/fadeOut)` 200ms。

#### 5.2.3 PeriodDayBlock（新建组件）

**文件**：`androidApp/.../ui/ai/PeriodDayBlock.kt`

| 属性 | 取值 |
|------|------|
| 日期标签 | `titleSmall`（17sp Semibold），`label` 色，左对齐，格式"周一 8月4日" |
| 标签与输入框间距 | 8dp |
| 输入框 | `OutlinedTextField`，`heightIn(min = 100.dp, max = 160.dp)`，`RoundedCornerShape(12dp)` |
| 占位符 | "描述这一天的饮食…" |
| 字符计数 | 右下角内嵌 overlay（同快速记格式 `"XX / 200"` + 颜色分级） |
| 块间距 | 块与块之间 12dp（`Arrangement.spacedBy(12.dp)`） |

#### 5.2.4 发送按钮

- 组件：**`CapsuleButton`**（全 App 统一，替旧 Material `Button`）
- 文本：`"发送"` + `Send` 图标（18dp）
- `enabled`：周期记模式 `periodInputs.any { it.value.isNotBlank() }`；快速记 `quickDraftText.isNotBlank()`
- 文案：快速记 `"发送"`，周期记 `"发送 · N 天"`（N = 非空日期段数）
- 宽度：`fillMaxWidth`（底部主 CTA 标准）

### 5.3 复用组件清单

| 组件 | 来源 | 复用方式 | 改动 |
|------|------|---------|------|
| `SegmentedControl` | `component/SegmentedControl.kt` | 标题下插入，`options=["快速记","周期记"]`，`onSelect` 切 VM 模式 | 零改动 |
| `CapsuleButton` | `component/CapsuleButton.kt` | 替换底部发送 `Button` | 零改动 |
| `ModalBottomSheet` | M3 1.1.2 | 维持现有容器 | 零改动 |
| `OutlinedTextField` | M3 | 快速记 ×1、周期记 ×N | 仅调 `heightIn` 参数 |
| `Icons.Outlined.*` | Material Icons | 标题行、发送按钮图标 | 零改动 |
| `VoiceRecognizer` | `androidApp/ai/` | 仅在快速记模式保持 | 零改动 |
| 粘贴按钮 | 现有 `TextButton` overlay | 仅在快速记模式保留 | 零改动 |

### 5.4 新建组件清单

| 组件 | 文件 | 签名 |
|------|------|------|
| **`WeekStrip`** | `androidApp/.../ui/ai/WeekStrip.kt` | `fun WeekStrip(weekMonday: LocalDate, selectedRange: IntRange, onRangeChange: (IntRange) -> Unit, modifier: Modifier)` |
| **`PeriodDayBlock`** | `androidApp/.../ui/ai/PeriodDayBlock.kt` | `fun PeriodDayBlock(dateLabel: String, inputText: String, onTextChange: (String) -> Unit, modifier: Modifier)` |

> 字符计数逻辑不单独抽组件——通过 `Box` overlay 内联实现（2 行 Compose 代码）。

### 5.5 动效清单（克制）

| 元素 | 动效 | 时长 |
|------|------|------|
| SegmentedControl 滑块 | 现有内置滑动动效 | ~250ms |
| WeekStrip 范围变化 | 选中段 `animateColorAsState` 背景色 + 天格文字色平滑过渡 | 300ms `tween` |
| 输入块出现/消失 | `AnimatedVisibility` 垂直展开/收起（`expandVertically` + `fadeIn`/`fadeOut`） | 200ms |
| 字符计数变色 | `animateColorAsState` tertiary → warning → error | 200ms |
| 模式切换 | 输入区内容 `AnimatedVisibility` 淡入淡出 | 200ms |

### 5.6 适老化（Android fontScale 2.0 + 显示尺寸最大）

- WeekStrip 天格 ≥56dp（直接满足触达底线）
- 选中天格**三重编码**：颜色（赤陶橘实底）+ 字重（Semibold）+ 位置（左→右 Mon→Sun）+ 范围描述行文字确认
- 输入框 minHeight 随字号动态放大
- 字符计数位置不漂移、不截断
- 发送按钮 ≥56dp 高

### 5.7 设计决策记录

| 决策 | 理由 |
|------|------|
| WeekStrip 用均分 7 格而非横滚 | 7 格 × ~45dp = 315dp，在 360dp 宽 Sheet 内（扣除 40dp 边距剩 320dp）刚好铺满 |
| 连续选中用端点点击而非拖拽 | 拖拽在小格子上（~45dp）易误触；端点点击清晰简单，符合苹果"能点不拖" |
| 快速记保留语音、周期记移除 | 周期记多输入框语音指向不明确（需先点框再长按 = 两步操作） |
| 字符计数内嵌输入框右下角 | iOS 备忘录/信息/邮件均如此，不额外占行 |
| CapsuleButton 替旧 Button | 全 App 主 CTA 统一为胶囊按钮（§9.10/§9.13），趁机收敛不一致特例 |
| WeekStrip 圆角 10dp | SegmentedControl 轨道圆角同值，同为分段选中控件圆角一致 |
| 天格高度 56dp | 标准触达底线 48dp + padding，直接满足适老要求 |

### 5.8 对象生命周期表（GC-14/15/16 · 2026-08-07 因 AF-B456-03 追加 · CODE 关闭 AF-B456-03 的冻结锚点）

> 背景：B4 步骤 3 把 §5.2.2/§5.2.3 的语音长按手势从"共用语音逻辑，零改动"（§2 非目标已声明）实现成了把 `activeRecognizer` **以值**传给 `QuickInputSection`，子组件写不回父层，`DisposableEffect` 的清理逻辑变成死代码——`a7fdf074` 版本里"统一语音实例管理，避免松手 stop 到错误对象"的历史修复连带消失，麦克风在部分场景不释放。本表是唯一冻结依据，CODE 按此恢复，不得引入新的语音状态字段或改 `VoiceRecognizer` 本体。

| 对象 | 创建者 | 持有者 | 可调用者 | 释放点 | 释放触发条件 |
|---|---|---|---|---|---|
| `VoiceRecognizer` 实例 | `startVoiceRecognition(context, vm) { recognizer -> ... }` 回调 | `InputPhase` 层的 `remember { mutableStateOf<VoiceRecognizer?>(null) }`（**必须是 `MutableState`，以该 State 本身向下传递，见 GC-15**） | `QuickInputSection`（读+写，非只读值） | `tryAwaitRelease()` 后 `activeRecognizer.value?.stopListening(); activeRecognizer.value = null`；`DisposableEffect(Unit){ onDispose { activeRecognizer.value?.destroy() } }` | 长按手势松手；或组合体离开 Composition（Sheet 关闭） |

**GC-15 传递形态**：`QuickInputSection` 的形参类型必须是 `activeRecognizer: MutableState<VoiceRecognizer?>`（不是 `VoiceRecognizer?` 值参数，也不是回调），调用处传入 `InputPhase` 层持有的同一个 `MutableState` 实例本身。

**GC-16 搬迁历史修复清单**（重构语音长按代码块时逐条落点，不得遗漏）：

| `a7fdf074` 原有历史修复 | 定位 | B4 重构后落点 |
|---|---|---|
| `// Bug修复：统一语音实例管理，避免 startVoiceRecognition 内新建实例导致松手 stop 到错误对象` 注释 | 原 `AiMealInputSheet.kt` 长按手势块 | 随 `MutableState` 传递方式一并恢复，注释保留 |
| 松手 `activeRecognizer?.stopListening(); activeRecognizer = null` 配对逻辑 | 同上 | `QuickInputSection` 内 `tryAwaitRelease()` 之后 |
| `VoiceState` 四态图标/`contentDescription`（长按开始说话/松手结束录音/识别中…/识别失败可重试） | 同上 | 恢复随 `VoiceState` 变化的图标与无障碍描述，不得退化为固定 `Icons.Outlined.Mic` + 固定文案 |
| 录音中脉冲外圈动画 | 同上 | 恢复 |

**必须新增/恢复的真机项**：`E-B6-VOICE-01`——①长按语音说一句→松手→文本进入输入框②立刻点发送→能发出且预览含该内容③连按三次录音，`adb shell dumpsys media.audio_flinger` 无残留 record client。

**禁止扩大范围**：不得改 `VoiceRecognizer` 本体；不得给周期记加语音（§5.7 已裁决周期记不放语音）；不得改语音权限申请流程。

## §6. 数据流（B4 变更部分）

```
用户选择模式（SegmentedControl）
  ↓ vm.setInputMode(QUICK/PERIOD)
UiState.inputMode 更新
  ↓
用户输入（快速记: quickDraftText / 周期记: periodInputs[dayIndex]）
  ↓ vm.setQuickDraft(text) / vm.setPeriodInput(dayIndex, text)
UiState 草稿更新（each mode independent）
  ↓ 用户点击发送
vm.submit()
  ├─ QUICK: InputSegmentFactory.forQuickRecord(quickDraftText, targetDate)
  │   → StreamingMealRequest(segments=..., ...)
  │
  └─ PERIOD: InputSegmentFactory.forPeriodicRecord(periodInputs, periodWeekMonday)
      → StreamingMealRequest(segments=..., ...)
  ↓
StreamingMealSession(request)
  ↓ 与 B3 完全一致的 while(nextSegment()) 循环
AiMealPrompt.buildStreamingRequest(seg)  // 单段（§1 AF-ARCH-03 收窄）
  ↓
AiRuntime.stream(llmRequest).collect { ... }
  ↓
handleSessionSnapshot → previewAll → AutoGenPreview
  ↓ 用户确认
commitPreview
```

**与 B3 的唯一差异**：`submit()` 开头 15 行的 segment 构造逻辑。其余全部不变。

---

## §7. 逐文件机械实施脚本

> **命名约定（§4 真相源）**：`InputMode.QUICK/PERIOD`、`quickDraftText`、`periodInputs: Map<Int,String>`、`periodWeekMonday`、`periodSelectedRange`。以下全部代码与 §4/§5 精确一致。

### 步骤 0：AF-ARCH-03 冻结（必须先做，独立 commit）

**文件**：`shared/.../ai/meallog/AiMealPrompt.kt:93-127`

1. 改 `buildStreamingRequest` 签名：
   ```kotlin
   // 旧
   fun buildStreamingRequest(segments: List<InputSegment>): LlmRequest
   // 新
   fun buildStreamingRequest(segment: InputSegment): LlmRequest
   ```
2. 删 `buildStreamingUserPrompt` 中 `else { ... }` 多段分支（第 117-125 行）+ 删 `maxTokens` 缩放逻辑（第 98-101 行），固定 `maxTokens = 2048`。
3. 更新唯一调用点 `AiMealInputViewModel.kt:306`：`buildStreamingRequest(listOf(seg))` → `buildStreamingRequest(seg)`。
4. 运行 `:shared:testDebugUnitTest` 确认 0 失败。

### 步骤 1：新建 InputSegmentFactory + 测试（shared）

**文件**：`shared/.../ai/meallog/InputSegmentFactory.kt`（新建）

按 §4.2 精确签名实现 `forQuickRecord` / `forPeriodicRecord` / `mondayOfWeek`。

**测试**：`shared/.../test/.../InputSegmentFactoryTest.kt`（新建）
- `forQuickRecord`：segmentId 格式、inputText trim、ordinal=0、isBlank
- `forPeriodicRecord`：7 段生成、segmentId 格式 `week-{anchor}-day1..7`、ordinal 0-6、空白段保留
- `mondayOfWeek`：正常周、跨年周（2025-12-29→周一 2025-12-29）、1月1日周一、12月31日所在周

### 步骤 2：扩展 ViewModel（接入点 1, 5, 6 · 2026-08-07 因 AF-B456-01/02 全面改写，取代原 2.1~2.5 散文脚本）

**文件**：`androidApp/.../ui/ai/AiMealInputViewModel.kt`、`AiMealInputSheet.kt`

> 背景：原 2.1~2.5 只说"新增 `quickDraftText` 字段"和"`submit()` 读它"，从未说明旧字段 `inputText` 的四个写入点怎么办，也没说发送按钮 `enabled` 该读谁。CODE 严格照做，结果 100% 合规地写出了双真相源——`quickDraftText` 只有 `submit()` 在读，构造器/`onVoiceResult`/`appendText`/`invalidateGenerationToInput` 四处仍写 `inputText`，语音识别结果和「粘贴」按钮内容因此永远发不出去（`AF-B456-01/02`）。这不是 CODE 的自由发挥，是原脚本的空隙；本节是唯一执行依据，取代原文。

#### 2.0 字段真相源冻结表（GC-10）

| 字段 | 唯一写入者 | 读取方（全部） | 禁止覆盖点 | 终局形态 |
|---|---|---|---|---|
| `quickDraftText` | `setQuickDraft()`（唯一收口） | `submit()` QUICK 分支 · `AiMealInputSheet.kt` 发送按钮 `enabled` 判据 · `InputPhase` TextField 回灌 | 除 `setQuickDraft()` 外任何 `_state.update{ it.copy(quickDraftText=…) }` | **唯一真相源** |
| `inputText` | **无**（禁止任何写入） | 兼容读点（若仍有代码引用） | 全部 | **派生**：`val inputText: String get() = quickDraftText`（计算属性，与 §4.3 原设计"`inputText` 保留但变更为计算属性"一致） |

#### 2.1 旧字段写入点迁移清单（GC-11/GC-27 · 逐行勾销，不得跳行）

**前置动作**：CODE 先执行 `grep -n "inputText" androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/*.kt` 并与下表逐行比对。**若 grep 出表外的写入点 → 停机发 `Q-B4-NN`，不得自行处理。**

| # | 写入点（定位） | 现状 | 处置 | 完成形态（审查判据） |
|---|---|---|---|---|
| W1 | `AiMealInputViewModel.kt:159-161` VM 构造 | `AiMealInputUiState(inputText = initialText, …)` | 改写为 `quickDraftText = initialText` | 构造实参列表 `grep "inputText ="` 零命中 |
| W2 | `AiMealInputViewModel.kt:265-292` `invalidateGenerationToInput()` | `prev.copy(inputText = nextInput, …)` | 改为 `prev.copy(quickDraftText = nextInput, …)` | 函数体 `grep "inputText"` 零命中 |
| W3 | `AiMealInputViewModel.kt:332-336` `onVoiceResult()` | 读 `_state.value.inputText` | 改读 `_state.value.quickDraftText` | 同上 |
| W4 | `AiMealInputViewModel.kt:355-359` `appendText()` | 读 `_state.value.inputText` | 改读 `_state.value.quickDraftText` | 同上 |
| W5（**GC-27，本项目"编辑收口函数"核对**） | `AiMealInputViewModel.kt:211-214` `setQuickDraft()` | 只 `take(200)` 写字段，**未调** `invalidateGenerationToInput()`——本项目"编辑即失效"的唯一收口函数是 `invalidateGenerationToInput`（B3 为修复 `AF-B3-03` 而建立）；任何新增编辑入口都必须核对是否路由过它，这条核对本身就是 B3→B4 同一 bug 复发的直接教训 | 内部改为调用 `invalidateGenerationToInput(trimmed, targetDate)` 收口 | 函数体出现 `invalidateGenerationToInput(` |
| W6 | `AiMealInputUiState` 字段声明 | `val inputText: String = ""`（可写字段） | 改为 `val inputText: String get() = quickDraftText`（无 backing field，计算属性） | data class 主构造函数参数列表中不含 `inputText` |

**Must not**：保留 `inputText` 为可写字段并"两边都写"（并存无同步保证，直接判 AF）；**Must not**：把 `invalidateGenerationToInput` 的粘性字段清单顺手重构（禁止扩大范围）。

#### 2.2 UI 判据 / 业务判据同源表（GC-12）

| 判据 | 位置 | 唯一表达式（字面量） |
|---|---|---|
| 发送按钮可用 | `AiMealInputSheet.kt:459-463` `enabled =` | `state.quickDraftText.isNotBlank()` |
| 提交前置校验 | `AiMealInputViewModel.submit()` QUICK 分支 | `state.quickDraftText.trim().isBlank() -> return` |

审查判据：`grep -n "enabled = state\." AiMealInputSheet.kt` 结果与 `submit()` QUICK 分支引用同一字段；不同源即判 AF。

#### 2.3 其余字段与方法（原 2.1/2.2 内容，未涉及 AF，照原样保留）

`AiMealInputUiState` 另新增（与 §4.3 一致）：
```kotlin
val inputMode: InputMode = InputMode.QUICK,
val periodWeekMonday: LocalDate = ...,  // 默认当前日期所在周周一
val periodSelectedRange: IntRange = 0..6,
val periodInputs: Map<Int, String> = emptyMap(),
```

新增 VM 动作：
```kotlin
fun setInputMode(mode: InputMode) { ... }        // 切换模式，保留对方草稿
fun setPeriodInput(dayIndex: Int, text: String) { ... }  // take(200)，写 periodInputs；截断分支须调用 GC-30（见 §7 步骤 3.5）
fun setWeekRange(start: Int, end: Int) { ... }    // 调整 periodSelectedRange
fun advanceWeek() { ... }                         // periodWeekMonday + 7，清空 periodInputs
fun retreatWeek() { ... }                         // periodWeekMonday - 7，清空 periodInputs
```

`submit()` 改造（接入点 1）：
```kotlin
fun submit() {
    val state = _state.value
    val segments = when (state.inputMode) {
        InputMode.QUICK -> {
            val text = state.quickDraftText.trim()
            if (text.isBlank()) return
            InputSegmentFactory.forQuickRecord(text, state.targetDate)
        }
        InputMode.WEEK -> {
            InputSegmentFactory.forPeriodicRecord(
                dayTexts = (0..6).map { state.periodInputs[it] ?: "" },
                weekAnchorDate = state.periodWeekMonday,
            )
        }
    }
    if (segments.all { it.isBlank }) return

    val generationId = "meal-${++generationCounter}"
    val request = StreamingMealRequest(
        segments = segments,
        generationId = generationId,
        weekAnchor = InputSegmentFactory.mondayOfWeek(
            segments.firstOrNull { !it.isBlank }?.targetDate ?: state.targetDate
        ),
    )
    // ... 后续与 B3 完全一致（session 创建、generation 清空、while 循环）...
}
```

**删除 `startOfWeek()` 私有方法**（`AiMealInputViewModel.kt:486-489`）→ 替换为 `InputSegmentFactory.mondayOfWeek()`。

#### 2.4 STEP 勾销 + 回归基线锁定（GC-09/23/24）

| STEP-ID | 文件:定位 | 动作 | 完成后形态 | Evidence |
|---|---|---|---|---|
| STEP-B4-2.1 | VM `AiMealInputUiState` + 四个写入点 | 按 2.1 表 W1~W6 逐行迁移 | 全仓 `grep "inputText ="`（赋值形）零命中 | T-B4-03/08/09 |
| STEP-B4-2.2 | `AiMealInputSheet.kt:459` | `enabled` 改为 `state.quickDraftText.isNotBlank()` | 字面量匹配 2.2 表 | 真机 E-B6-VOICE-01 |
| STEP-B4-2.3 | VM `setQuickDraft()` | 内部收口调 `invalidateGenerationToInput()` | 函数体含该调用 | T-B4-10 |
| STEP-B4-2.4 | VM `startOfWeek()` | 删除，替换为 `InputSegmentFactory.mondayOfWeek()` | `grep startOfWeek` 零命中 | T-B4-07 |
| STEP-B4-2.5 | VM 新增字段/方法 | 按 2.3 落地 `inputMode`/`periodWeekMonday`/`periodSelectedRange`/`periodInputs`/`setInputMode`/`setPeriodInput`/`setWeekRange`/`advanceWeek`/`retreatWeek` | 编译通过 + T-B4-01/02/04 | T-B4-01/02/04 |

**本批不得失败**（回归基线锁定）：`AiMealInputViewModelStreamTest`（T-B3-01~09 全部）、`InputSegmentFactoryTest`、`StreamingMealSessionTest`。**不批准**任何 B3 断言弱化；如需修改，先发 `Q-B4-NN`。

### 步骤 3：新建 UI 组件 + 扩展 InputPhase（2026-08-07 补丁：拆 STEP 编号 + 补 3.4/3.5 落点）

**新建文件**：
- `androidApp/.../ui/ai/WeekStrip.kt`（§5.2.2 规格）
- `androidApp/.../ui/ai/PeriodDayBlock.kt`（§5.2.3 规格，原名 PeriodDayInputBlock）

**修改文件**：`androidApp/.../ui/ai/AiMealInputSheet.kt`

| STEP-ID | 动作 | 完成后形态 | Evidence |
|---|---|---|---|
| STEP-B4-3.1 | `InputPhase` 内部按 `state.inputMode` 分叉 → `QuickInputSection`（既有代码搬迁，**须按 §5.8 对象生命周期表恢复 `activeRecognizer` 的 `MutableState` 传递方式**，不得以值传参）/ `PeriodInputSection`（新增） | §5.8 GC-14/15/16 全部满足 | 真机 E-B6-VOICE-01 |
| STEP-B4-3.2 | `PeriodInputSection` 使用 `WeekStrip` + `PeriodDayBlock` 列表 + `CapsuleButton` | 编译通过 + E-B4-01/02 | E-B4-01/02 |
| STEP-B4-3.3 | 发送按钮改为 `CapsuleButton`，文案：快速记 `"发送"` / 周期记 `"发送 · N 天"` | 字面量匹配 | 真机目测 |
| STEP-B4-3.4（**GC-25**） | 标题文案跟随模式 | `text = if (state.inputMode == InputMode.QUICK) "AI 快捷记一餐" else "AI 周期记一餐"`（**当前代码两分支相同，是死条件，AF-B456-09**） | 审查判据：`grep "AI 周期记一餐" AiMealInputSheet.kt` 须命中 |
| STEP-B4-3.5（**GC-21/30，关闭 AF-B456-04**） | 按 §3.6 自动副作用清单表，在 `QuickInputSection`/`PeriodDayBlock` 的 `onValueChange` 截断分支（`truncatedText.length != newVal.text.length`）弹一次 `LocalAppSnackbar.current.showSnackbar("已截取前 ${AiMealPrompt.MAX_INPUT_CHARS} 字")`；`PeriodDayBlock` 经新增可空回调 `onTruncated` 接收，由 `PeriodInputSection` 注入；`truncNotified` 复位规则见 §3.6 | 两处截断分支均出现 Snackbar 调用；文本降到上限以下不重复弹 | T-B4-05a/05b（沿用 §9.2）+ 真机 `E-B6-TRUNC-01` |

### 步骤 4：测试

见 §9（含 §9.4 INV↔T 双向映射表）。

### 步骤 5：构建与台账

```bash
scripts\build-cli.bat :shared:testDebugUnitTest
scripts\build-cli.bat :androidApp:testDebugUnitTest --tests "com.sxdbsm.cookbook.android.ui.ai.AiMealInputViewModelStreamTest"
scripts\build-cli.bat :androidApp:assembleDebug
```

台账须贴**当次**输出，Shared / Android 分别列出测试计数（GC-06）；不得只写 `Shared tests: 0 failures`。

### 步骤 6：STEP 勾销表（GC-24 · 2026-08-07 补丁新增，交付时由 CODE 填）

| STEP-ID | 状态 | 落地 commit | diff 定位 |
|---|---|---|---|---|:--:|
| STEP-B4-2.1~2.5 | ✅ | 待填 commit | VM: W1~W6 迁移完成，inputText→computed，quickDraftText 统一真相源 + Sheet: enabled/TextField/CharCountLabel 同源 |
| STEP-B4-3.1~3.5 | ✅ | 待填 commit | Sheet: MutableState 传递 activeRecognizer + 松手 stopListening + 四态图标/无障碍/脉冲 + 截断 Snackbar + 标题文案 |
| §1 冻结值修订记录表（maxTokens） | ✅ | 待填 commit | 证据+影响评估已补 |
| §3.5 INV-B456-R05a/b/c | ✅ | 待填 commit | GenerationProgress.segmentStatuses + currentSegmentIndex + SegmentProgressBar 1:1 映射 |
| §3.6 自动副作用清单表 | ✅ | 待填 commit | QuickInputSection + PeriodDayBlock 截断 Snackbar + truncNotified 去重 |
| §5.8 对象生命周期表 | ✅ | 待填 commit | QuickInputSection 接受 MutableState + 松手配对 + 四态恢复 |
| AF-B456-07 台账修正 | ✅ | 待填 commit | periodSelectedRange 注释补真实语义 |
| AF-B456-08 真机清单 | ✅ | 待填 commit | B6 分组 E-B6-01~05+VOICE-01+TRUNC-01 + 文件重命名 |
| AF-B456-09 标题文案 | ✅ | 待填 commit | "AI 快捷记一餐" / "AI 周期记一餐" |

**审查以此表逐条 diff 复核，不采信 commit message 自述**（`AF-B456-07` 的教训：`63fd3fec` 曾勾了"periodSelectedRange 注释已完成"但代码与首次编写时完全一致）。

### B4 最小改动集（架构工程师确认，命名已对齐 §4）

| # | 文件 | 改动 | 依赖 |
|---|------|------|------|
| 1 | **新建** `shared/.../meallog/InputSegmentFactory.kt` | `forQuickRecord` / `forPeriodicRecord` / `mondayOfWeek` + 单测 | 无 |
| 2 | `AiMealPrompt.kt:93-127` | `buildStreamingRequest` 签名收窄为单段，删多段分支+maxTokens缩放 | 无 |
| 3 | `AiMealInputViewModel.kt:268-280,486-489` | `submit()` 改用 `InputSegmentFactory` + 删 `startOfWeek()` + 新增 `InputMode`/草稿字段/方法 | 依赖 #1 |
| 4 | `AiMealInputViewModel.kt:306` | `buildStreamingRequest(listOf(seg))` → `buildStreamingRequest(seg)` | 依赖 #2 |
| 5 | **新建** `WeekStrip.kt` + `PeriodDayBlock.kt` | 周期记日期段选择器 + 每日输入块 | 依赖 #3 |
| 6 | `AiMealInputSheet.kt` InputPhase | 模式切换 + PeriodInputSection + CapsuleButton | 依赖 #3,#5 |

**不改**：`StreamingMealSession`、`StreamingMealParser`、`AiRuntime`、`MealStreamDraftMapper`、Repository、DI。

---

## §8. S1~S6 逐条显式处理（B3 蓝图 §11.3，ChatGPT 复核门禁 #2）

| # | 原建议 | B4 处理 | 理由 |
|---| ------ | ------- | ---- |
| **S1** | `PARTIAL_READY` 保存会取消后续段 | 🟡 **纳入 B5 蓝图**。B4 不改变保存逻辑（`PARTIAL_READY` 可保存，`generationJob?.cancel()` 取消剩余段）。B5 需定策略：`isTerminal` 前禁止保存，或保存前告知"仅保存已完成 N 天，其余 M 天将放弃"。B4 周期记的 segment 列表已含 ordinal 顺序，底层已就绪。 | B4 只做输入 UI，保存行为归 B5。B3 单段无此问题；B4 周期记可能暴露但非本批范围。 |
| **S2** | Delta 路径 preview 太频繁 | 🟡 **纳入 B5 蓝图**。B4 不改 `handleSessionSnapshot` 的调用时机。B5 应将 preview 触发改为"段终态 + final + 防抖"而非每 Delta。 | B4 不改变 B3 的生成/预览链路。周期记 7 段时此优化价值更大，但归 B5。 |
| **S3** | snapshot 结构需表达 per-day 失败信息 | 🟡 **纳入 B5 蓝图**。`StreamingSessionSnapshot` 目前已有 `segmentStates: Map<String, StreamSegmentState>`，B5 UI 可通过它展示"第 N 天失败"。如需更丰富信息（如 per-segment 诊断分组），B5 扩展 snapshot。 | 当前结构已支持基本的 per-segment 状态展示。B4 不改 snapshot。 |
| **S4** | `AiMealSessionPort` 签名需多天支持 | 🟡 **纳入 B5 蓝图**。`preview(days, targetDate)` 和 `commit(preview)` 当前支持多天（`days` 已是 `List<DayMealJson>`）。`parseRule(input, targetDate)` 仅单串——如 B5 需要按天降级，届时扩展。 | B4 不改 port。当前签名对多天场景足够。 |
| **S5** | `sessionPort` 改为构造参数注入 | 🟡 **降级到 B4 后技术债批次**。`replaceSessionPortForTest` 已存在且正常工作。改为构造参数注入属纯重构（零功能变更），但需改 ViewModel 构造器签名 + Koin DI + 全部测试构造点，对 B4 输入 UI 无实际依赖。B4 最小化改动面优先。 | 消除可变注入点的并发面价值认同，但 B4 不改 sessionPort 调用路径（仅 `submit()` 中 segment 构造逻辑变化），此重构与 B4 正交。 |
| **S6a** | 周起始日算法下沉 InputSegmentFactory | ✅ **已采纳**。`InputSegmentFactory` 含 `forQuickRecord` + `forPeriodicRecord` + `mondayOfWeek` 三个纯函数。ViewModel 的 `startOfWeek()` 删除，统一调用 `mondayOfWeek()`。周期记 segment 构造经 `forPeriodicRecord` 而非 ViewModel 内联。 | 架构工程师复审确认：segmentId 命名规则 + weekAnchor 推算 + ordinal 排序在单一工厂内保证一致。 |
| **S6b** | 健康摘要脱敏下沉 HealthContextSummarizer | 🟡 **纳入后续技术债批次**（不阻塞 B4/B5）。涉及"绝不发送姓名/ID"红线，但目前快速记/周期记的健康上下文来自 `buildHealthSafetyReport`（已在 VM 内），B4 不新增联网风险。 | B4 不改健康摘要。下沉可独立做、不绑定 B4。 |

---

## §9. 测试矩阵

### 9.1 Shared 测试（InputSegmentFactoryTest，新建）

| ID | 前置与刺激 | 精确断言 |
|----|----------|---------|
| T-B4-F1 | `forQuickRecord("  午饭  ", date)` | `segmentId = "quick-{date}"`, `inputText = "午饭"`, `ordinal = 0`, `isBlank = false`, 返回 `List` 含 1 元素 |
| T-B4-F2 | `forQuickRecord("   ", date)` | `isBlank = true`（空白段保留在列表中） |
| T-B4-F3 | `forPeriodicRecord(listOf("a","","c","","","",""), anchor)` | 返回 7 个 segment；segmentId 为 `week-{anchor}-day1..7`；ordinal 0..6；空白段 isBlank=true 但保留 |
| T-B4-F4 | `mondayOfWeek(2025-12-31)` | 返回 `2025-12-29`（跨年周周一） |
| T-B4-F5 | `mondayOfWeek(2026-01-01)` | 返回 `2025-12-29`（1月1日周四，所在周周一） |

### 9.2 ViewModel 测试（扩展 AiMealInputViewModelStreamTest）

| ID | 前置与刺激 | 精确断言 |
|----|----------|---------|
| **T-B4-01** | 初始 state；调用 `setInputMode(WEEK)` | `inputMode = WEEK`；`quickDraftText` 保持原值；`periodInputs` 为空 Map |
| **T-B4-02** | WEEK 模式，`periodInputs = mapOf(0 to "周一文本", 2 to "周三文本", 4 to "周五文本")`，其余天无 key；调用 `submit()` | 构造的 `StreamingMealRequest` 仅含 3 个 segment（空白被 `nonBlankSegments` 过滤）；segmentId 为 `week-{anchor}-day1`、`day3`、`day5`；ordinal 为 0, 2, 4；targetDate 分别为周一/三/五 |
| **T-B4-03** | QUICK 模式，`quickDraftText = "午饭"`；调用 `submit()` | `StreamingMealRequest` 含 1 个 segment；segmentId = `quick-{date}`；行为与 B3 完全一致 |
| **T-B4-04** | QUICK 模式 `setQuickDraft("午饭")` → `setInputMode(WEEK)` → `setInputMode(QUICK)` | `quickDraftText = "午饭"`（未丢失）；`inputMode = QUICK` |
| **T-B4-05a** | WEEK 模式 `setPeriodInput(0, 250字文本)` | `periodInputs[0]?.length = 200`（截断） |
| **T-B4-05b** | QUICK 模式 `setQuickDraft(250字文本)` | `quickDraftText.length = 200`（截断） |
| **T-B4-06** | WEEK 模式 `periodInputs` 全部空 → `submit()` | `submit()` 直接 return（`segments.all { it.isBlank }`），不创建 session |
| **T-B4-07** | WEEK 模式，`periodWeekMonday = 2025-12-29` | `forPeriodicRecord` 生成的 day7 = 2026-01-04（跨年周日），日期计算正确 |

### 9.3 B3 回归测试

B3 既有测试（T-B3-01~09 + session/mapper/parser/runtime）全部保持通过。

### 9.4 INV ↔ T 双向映射表（GC-05 · 2026-08-07 补丁新增）

> 每条 INV 至少 1 个 T-ID，每个 T-ID 至少 1 个 INV；出现孤儿项即判蓝图未完成。本表覆盖 B4 补丁涉及的全部新增不变量与测试，交付时补齐"当次结果"列。

| INV | T-ID | 覆盖 AF | 当次结果 |
|---|---|---|:--:|
| INV-B4-01~11（原有） | T-B4-01~07（§9.2） | — | 待填 |
| （字段真相源 §7 步骤 2.0） | T-B4-08（`onVoiceResult` 发送验证）、T-B4-09（`appendText` 拼接验证）、T-B4-10（`setQuickDraft` 生成中编辑打断验证） | AF-B456-01/02 | 待填 |
| INV-B456-R05a/b/c（§3.5） | T-B5-01/02/03 | AF-B456-05 | 待填 |
| INV-B4-05/06（截断提示，§3.6） | T-B4-05a/05b（沿用）+ 真机 `E-B6-TRUNC-01` | AF-B456-04 | 待填 |
| segmentId 唯一性（§3.4） | `StreamingMealSessionTest` 新增用例 | 缺证据 §3.3 | 待填 |
| §5.8 语音生命周期 | 无法单测，真机 `E-B6-VOICE-01` | AF-B456-03 | 待填 |
| §7 步骤 3.4 标题文案 | 无单测，并入真机 `E-B4-01` 预期结果 | AF-B456-09 | 待填 |

---

## §10. 审查裁决延后项（2026-08-06 三角色审查记录，2026-08-07 补 GC-03 归宿列）

> **GC-03（延后项归宿）说明**：本表原版全部标"待 B5/B6"是"只留指针"，这正是 `AF-B456` 复核报告 §3.1 指出的"B4→B5 连续两批延后项无归宿"问题的源头。下表已补齐归宿——已处理项标实际关闭 commit，未处理项转成本次补丁的具体条目或显式弃置+理由，不得再留纯指针。

| 来源 | 项目 | 裁决 | 归宿（GC-03） |
|------|------|------|---------|
| Apple #3 | advanceWeek/retreatWeek 未接通 UI | ✅ B5 已接通（`63fd3fec` WeekStrip 切周箭头） | 已关闭 `63fd3fec` |
| Google质量 B4-S1 | periodSelectedRange 语义澄清 | ⚠️ B5 声称已加注释（`63fd3fec`），复核核实**注释与首次编写时完全一致，未真正落地**（`AF-B456-07`） | 转本次补丁：CODE 需在 `periodSelectedRange` 字段补真实注释"仅控制输入区可见性，`submit()` 提交 `periodInputs` 中全部非空白天，不受本范围限制" |
| Google架构 S1 | mondayOfWeek 在 submit() 中冗余反推 | ⚪ 逻辑安全，非本次阻断 | 显式弃置：转下一次维护批次顺手清（B6 说要做但未做，本次不追加范围，避免范围漂移） |
| Google架构 S2 | 200 字上限散布 4 处，未提取常量 | ✅ 已提取 `AiMealPrompt.MAX_INPUT_CHARS`（`63fd3fec`） | 已关闭 `63fd3fec` |
| Google架构 S3 | InputSegment.charCount 死代码 | ⚪ 保留不删 | 显式弃置：1 行 getter，伤害低，不处理 |
| Apple #3 | VoiceRecognizer 泄漏（B3 预存） | 🔴 B4 步骤 3 实际**加重**了此问题（`AF-B456-03`） | 转本次补丁：§5.8 对象生命周期表 |
| Apple #4 | 两模式字符计数动画不一致 | ✅ 已抽 `CharCountLabel`（`63fd3fec`），但 KDoc 与实现不符（R-06） | 部分关闭 `63fd3fec`；KDoc 修正转下一维护批次（非阻断，不在本次 9 项 AF 范围内） |
| Apple #5 | 截断层不一致（VM 层 vs UI 层） | ⚠️ B5 声称"统一到 VM 层"，但 B6 又在 UI 层加回截断（`ac664fa1`），现为双层截断，"统一"结论已失效 | 转本次补丁：§3.6 承认双层截断为现状（UI 层截断负责即时视觉反馈+提示，VM 层截断负责最终写入值一致），不强行合并为单层，避免范围漂移；文档表述已更正 |
| Apple #7 | 切周清空草稿无撤销 | ✅ 已加 `AppSnackbar.showUndo`（`63fd3fec`） | 已关闭 `63fd3fec` |
| Apple #8 | 字符计数 overlay 可能压文字 | ⚠️ B5 声称已加 padding，复核发现 padding 加在调用处而非组件内，KDoc 描述与实现不符（R-06） | 转下一维护批次（非阻断） |

---

## §11. 交付与放行判定

### 11.1 放行条件

1. §0 的 4 项 ChatGPT 门禁全部勾销（§1 AF-ARCH-03 冻结 + §8 S1~S6 处理 + 测试确认 + §3.3 边界检查表）。
2. AF-ARCH-03 代码落地（`buildStreamingRequest` 签名收窄为单段 + 多段分支删除）并通过 shared 单测。
3. T-B4-F1~F3 全部通过（shared 新增）。
4. T-B4-01~07 全部通过（ViewModel 新增）。
5. 既有 B3 测试（T-B3-01~09 + session/mapper/parser/runtime）全部保持 0 失败。
6. 三条构建命令串行当次成功：
   - `scripts\build-cli.bat :shared:testDebugUnitTest`
   - `scripts\build-cli.bat :androidApp:testDebugUnitTest --tests "com.sxdbsm.cookbook.android.ui.ai.AiMealInputViewModelStreamTest"`
   - `scripts\build-cli.bat :androidApp:assembleDebug`
7. 本蓝图状态由 `DRAFT` → `ACCEPTED`。
8. **（2026-08-07 补丁新增）** §0.1 颗粒度勾销表 26+9=35 条 GC 中标"未满足·CODE 待办"的全部转为"满足"，无遗留；`AF-B456-01~09` 全部按对应章节的"唯一最小修复"关闭；§7 步骤 6 的 STEP 勾销表全部填写落地 commit 与 diff 定位；真机清单（§13）补齐 B6 分组并按当次时间重命名文件。全部满足后，把本文件头的状态由 `REVIEWED_BLOCKED → 补丁中` 改回 `ACCEPTED`，并把 `BLUEPRINT_STATE.md` 的 `TURN` 改回 `ARCH`、`git push`，交回架构模型做二次复核（仅限本轮 9 项 AF + 补充测试范围，不重新审查已通过项）。

### 11.2 B4 → B5 过渡条件

B4 编码完成后、B5 开始前：
- B4 全部放行条件满足
- 真机验证通过（快速记 + 周期记输入 UI）
- B5 蓝图起草（确认页流式展示）

---

## §12. 与 B3 蓝图的关系

B3 蓝图的 §11.6 放行判定要求本文（B4 蓝图）完成后将 B3 蓝图状态由 `BLOCKED` 改为 `ACCEPTED`。

**时序**：
1. B3 蓝图 §11.6 → 检查本文 §0 的 4 项门禁全部满足 → B3 蓝图 `BLOCKED` → `ACCEPTED`
2. 本文 §10.1 全部满足 → 本文 `DRAFT` → `ACCEPTED`
3. 开始 B4 编码

两个蓝图可以同时 `ACCEPTED`，但 B4 编码必须在 B3 蓝图 `ACCEPTED` + 本文 `ACCEPTED` 之后。

---

## §13. 真机待验证清单（B4 预登记）

| 编号 | 验证项 | 操作步骤 | 状态 |
|------| ------ | -------- | :--: |
| E-B4-01 | 快速记/周期记模式切换 | ①打开 AI 记一餐 Sheet → 看到默认"快速记"②点击"周期记"→ 输入区切换为 7 天列表③切回"快速记"→ 之前输入的草稿还在 | ⬜ |
| E-B4-02 | 周期记日期段导航 | ①周期记模式 → 点击左右箭头切换周②确认日期标签正确（周一~周日）③当前周高亮或默认选中 | ⬜ |
| E-B4-03 | 每日输入 + 200 字限制 | ①周期记模式下在周一输入框输入文字 → 字符计数实时更新②输入超过 200 字 → 自动截断，计数变红③粘贴超过 200 字内容 → 截断 + Toast 提示 | ⬜ |
| E-B4-04 | 周期记发送 → 流式生成 | ①在周一/周三/周五填内容，其余留空 → 点击发送②确认只发了 3 个请求（非 7 个）③生成过程中可看到进度（第 1/3 天...）④空白天不产生预览内容 | ⬜ |
| E-B4-05 | 草稿独立保留 | ①快速记输入"午饭"②切换到周期记③切换回快速记 → "午饭"还在④周期记周一写"早餐"⑤切换到快速记⑥切换回周期记 → 周一"早餐"还在 | ⬜ |
| E-B4-06 | 全部空白不可发送 | ①周期记模式全部 7 天空白 → 发送按钮灰色不可点击②快速记模式输入框空白 → 发送按钮灰色 | ⬜ |

### B6 分组（2026-08-07 补丁新增，GC-08 · 关闭 AF-B456-08）

> B6（`ac664fa1`）五项真机改动此前零验证项登记，本次补齐。**CODE 完成 AF-B456-01~09 修复后，须把本文件重命名为 `真机待验证清单_<当次 yyyyMMddHHmm>.md`（唯一清单原则，不得新建第二份），并把下表状态更新为实测结果。**

| 编号 | 验证项 | 操作步骤 | 状态 |
|------|--------|----------|:--:|
| E-B6-01 | 单天保存后 Sheet 关闭不冻屏 | 快速记/周期记保存一次 → Sheet 关闭动画正常，无卡顿黑屏，重复 5 次 | ⬜ |
| E-B6-02 | 长输入不截断 | 单日 3 餐 × ~10 菜的长输入 → AI 完整解析出全部菜品，不出现 `finish_reason=length` 截断 | ⬜ |
| E-B6-03 | 已有餐食灰显 | 打开周期记，本周已记录过的日期显示半透明+「已有」小标，仍可选中 | ⬜ |
| E-B6-04（R-01，先记录再修） | 切周后灰显是否随之更新 | ①周期记打开时看到当周灰显正确②点→切到下一周③检查该周已有餐食日是否也正确灰显（**已知缺陷：目前只在打开 Sheet 时查一次，切周后不刷新，此项预期失败，记录现象即可，不算本批阻断**） | ⬜ |
| E-B6-05 | 粘贴超 200 字截断 + 提示 | 快速记/周期记粘贴 300 字内容 → 截断到 200 字 + Snackbar「已截取前 200 字」出现（与 `AF-B456-04` 关闭状态一并验） | ⬜ |
| E-B6-VOICE-01（AF-B456-03） | 语音实例正确释放 | 见 §5.8 | ⬜ |
| E-B6-TRUNC-01（AF-B456-04） | 截断提示不重复弹 | 见 §3.6 | ⬜ |
