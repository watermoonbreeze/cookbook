# K1i：全App AI输出流式/渐进展示 实施蓝图

> 状态：`BLUEPRINT_READY`（v1b，已按 GC-37 独立挑战的 3 项 CONFIRMED-ISSUE 处置，§10 记录挑战与处置对照。**下一步：`BLUEPRINT_STATE.md` 排期到 L1 之后，交由后续 CODE 角色按其 §7 STEP 机械执行**）
> **v1b 关键修订摘要**（处置详情见 §10）：① 发现并修正本批真正的行为风险——不是"解析器扛不扛得住分片"（已被既有单测覆盖，蓝图 v1 判断错了风险点），而是 `CloudAiRuntime.stream()` 用的 `buildStreamRequestBody()` **不带** `response_format:json_object`，与 `complete()` 路径的 `jsonMode=true` 不同：现状因为默认走 `complete()` 包装，NDJSON prompt 实际几乎总被强制成单一 JSON 对象输出、解析器一直在走"整体 JSON 规范化 fallback"这条**非设计主路径**；本批修好 `stream()` 委托后，模型才会真正按 NDJSON prompt 输出，解析主路径会切换到"逐行严格校验"——这是协议层面的真实行为切换，不是"事件到达节奏变了"这么简单，E-K1I-01 相应升级为带明确判据的阻断性真机项。② 补齐与 L1 的反向依赖闭环（L1 蓝图里"stream() 不重写"那句注释会被本批变成假话，须授权改写）。③ 测试策略改用 `runBlocking`，因为 `kotlinx-coroutines-test` 不在 `:shared` 测试 classpath 上。
> **本蓝图仅由 ARCH 起草，不含任何代码实现**——按用户 2026-08-08 指示"你只负责蓝图，不要编码"。
> **颗粒度：L7**（项目基线）。
> 起草日期：2026-08-08（K1e 起草后经 GC-37 证伪废弃，按用户既定顺序 L1→K1e(废弃)→K1i 起草）。
> **范围裁决（起草时与用户对齐）**：起草过程中发现 backlog 原描述"AI记餐NDJSON先落地，后续扩到AI推荐/生成菜品/健康建议"这句话里"先落地"三个字站不住——`SwitchableAiRuntime`（全 App 唯一的 AI 运行时分发入口）从未重写 `stream()`，导致包括 AI 记一餐在内的**全部**功能实际都没有真正的网络级流式到达，只是把一次性返回的整段响应文本包装成"一个 Delta + 一个 Completed"来模拟渐进。用户确认"先修这个地基缺口，再扩展 UI"。本蓝图**只做地基修复（批 K1i-1）**；"扩展到 AI 推荐/生成菜品/健康建议"的 UI 层工作（批 K1i-2）在 §1.3 明确弃置为独立未来批次，理由见该节——它不是"复用现成流式基建"的小改，而是要把 AI 推荐等功能的**输出协议**从"一次性 JSON"改造成"NDJSON 渐进协议"，量级与 AI 记一餐当年的 B1~B6 六个批次相当，不适合与地基修复捆绑仓促设计。

---

### §0.1 颗粒度勾销表（GRANULARITY = L7）

| GC | 条款一句话 | 本蓝图落点 | 状态 |
|---|---|---|:--:|
| GC-01 | 每个行为分支写成"条件→唯一动作→禁止动作" | §3 | 满足 |
| GC-02 | allowlist | §6 | 满足 |
| GC-03 | 上一批延后项归宿表 | §1.4 | 满足 |
| GC-04 | INV 五列齐全 | §3 | 满足 |
| GC-05 | INV↔T 双向映射表 | §8.2 | 满足 |
| GC-06 | 命令原文 | §7 末尾 | 满足 |
| GC-07 | 测试夹具职责边界表 | §8.1 | 满足 |
| GC-08 | 真机清单登记 | §9 | 满足 |
| GC-09 | 回归基线锁定 | §6 末尾 | 满足 |
| GC-10 | 逐字段真相源表 | §4.1 | 满足 |
| GC-11 | 字段/行为迁移清单 | §4.2 | 满足（`SwitchableAiRuntime.stream()` 从"未重写=用默认包装"迁移为"显式重写=真委托"，唯一行为变化点） |
| GC-12 | UI 判据与业务判据同源 | — | N/A：本批不改任何 UI/Compose 代码，`LlmStreamEvent` 事件类型本身不变、UI 消费逻辑零改动。**但**（v1b 修订）本批会改变触达 UI 之前的"解析主路径"——NDJSON 逐行严格解析取代此前实际一直在跑的整体 JSON fallback（见 §2 D4），这不是 UI 层判据问题，已单独列 E-K1I-01 真机项把关，不在此 N/A 范围内混淆 |
| GC-13 | fallback 复用主路径校验入口 | §4.6 | 满足：`stream()` 的同意闸门与 `complete()` 复用同一个 `config.cloudAiConsentGranted()` 判据（见 §1.2 依赖说明），不新造第二套判据 |
| GC-14~16 | 对象生命周期/可变持有物/搬迁注释 | §4.5 | 满足：`stream()` 内部 `Flow` 构建与取消语义需要显式论证（见 §4.5） |
| GC-17~19 | 逐项状态 List&lt;Status&gt; | — | N/A：本批不改变 `LlmStreamEvent` 三态（Delta/Completed/Failed）本身，只改事件产生的真实性 |
| GC-20 | 自动副作用清单 | E-K1I-01 | 满足（v1b 由 N/A 改判）：本批会让 AI 记一餐的解析主路径从"整体 JSON 规范化 fallback"切到"NDJSON 逐行严格校验"（见 §2 D4），若解析结果与改造前不等价属需要处理的真实副作用，已登记 E-K1I-01 为阻断性真机项+回退方案，不再声称"用户可见结果必然不变" |
| GC-21 | 提示/告知配 STEP 落点 | — | N/A：本批不新增用户提示 |
| GC-22 | 可见副作用配 T-ID | — | N/A：同上 |
| GC-23 | STEP 独立编号+完成形态字面量 | §7 | 满足 |
| GC-24 | STEP 勾销表 | §9 | 满足 |
| GC-25 | 完成形态字面量+grep判据 | §7 | 满足 |
| GC-26 | 冻结值修订记录 | — | N/A：不改任何阈值常量 |
| GC-27 | 编辑即失效收口函数核对 | — | N/A：与 AI 记一餐编辑态无关，本批在更底层的 runtime 分发层 |
| GC-28 | 构造时创建对象按基数分片 | — | N/A：`SwitchableAiRuntime` 本身是 DI 单例，但 `stream()` 每次调用独立构建新 `Flow`，不存在跨调用累积状态 |
| GC-29 | 多来源写入同一聚合目标 | — | N/A：无聚合写入 |
| GC-30 | 状态转移驱动完整副作用链 | §3 | 满足 |
| GC-31 | 挂起点+身份重校验 | §4.5 | 满足：`stream()` 内部挂起点（`config.activeType()`/`cloudAiConsentGranted()`）论证见 §4.5——本类无 generation 概念，调用方（`AiMealInputViewModel`）自己的 generation 隔离在更上层，与本类无关，不重复处理 |
| GC-32 | 高频异步事件节流 | — | 满足（v1b 由 N/A 改判）：单次 generation 的 Delta 事件数从 1 变为 N（数十~数百），事件频率**确实改变**（原判"频率不变"是错的）；节流由既有 `AiMealInputViewModel.kt` B5 快速路径（`isBoundary`/`isFinal` 才重算 preview，见其 `:661-666,684-689`）承担，本批不新增/不改节流策略，也不需要——引用既有节流，非 N/A |
| GC-33 | 禁止测试专用可变全局注入点 | — | N/A：`SwitchableAiRuntime` 构造参数注入的 `runtimes: Map<AiRuntimeType, AiRuntime>` 已是现成的测试注入点（构造时传 fake map），本批不新增 |
| GC-34 | 注释/KDoc 一致性 | §7 | 满足 |
| GC-35 | 协议事件枚举逐项对照 | — | N/A：`LlmStreamEvent` 三态不变 |
| GC-36 | List&lt;Status&gt; 值域覆盖 | — | N/A |
| GC-37 | 独立挑战台账 | §10 | 满足——已跑一轮，3 项 CONFIRMED-ISSUE 全部就地处置，见 §10 |

---

## §1 目标与范围

### 1.1 一句话价值

让 `SwitchableAiRuntime.stream()`（AI 记一餐等一切走 `stream()` 接口的功能的唯一真实网络出口）把请求真正委托给底层运行时（云端时是 `CloudAiRuntime.stream()` 的真实 SSE 分片实现），而不是像现在这样把 `complete()` 的一次性整段结果包装成假的"一个 Delta"。这是"全 App AI 输出流式/渐进展示"这件事在网络层唯一真正缺失的一块拼图。

**真实可见收益（v1b 修订，原稿高估了收益形态）**：独立挑战核实，AI 记一餐现有的段内 UI 刷新受既有 B5 节流保护（只有段终态或最终态才重算 preview，见 GC-32 行），**段内逐条"陆续出现"的节奏本批不会改变**（那是 K1i-2 的事）；本批真实带来的收益是"**第一个预览更快出现**"——不必等整段响应完全落地才能开始解析展示，网络较慢时尤其明显。同时（见 §2 D4）本批会让模型请求真正按 NDJSON 协议输出（而非现状被强制成单一 JSON 对象），是对 AI 记一餐既有设计意图的一次真正的"扶正"，但也因此是一次需要真机把关的协议级行为变化，不是纯粹的无风险内部重构。

### 1.2 触发来源与关键依赖

- **K1i**（`待办_功能算法.md` 🔴⬜）：全 App AI 输出流式/渐进显示兼容。
- **关键依赖：本蓝图与 L1（`L1_云端AI首启同意与合规免责_实施蓝图.md`）存在真实的实施顺序耦合，必须显式处理，否则会产生安全回归**：
  - L1 在 `SwitchableAiRuntime.complete()` 内新增了同意闸门（`activeType()==CLOUD && !cloudAiConsentGranted()` → 直接 `Result.failure`，不路由到 `CloudAiRuntime`）。
  - 本蓝图新增的 `SwitchableAiRuntime.stream()` override，如果只是简单委托给 `runtimes[type].stream(request)` 而不重复这条闸门判断，会**绕开 L1 刚建立的同意闸门**——因为委托后请求不再经过 `complete()` 这个函数体，L1 的检查代码在那里，`stream()` 走的是完全独立的代码路径。
  - **处置**：本蓝图 §4.4 给出的 `stream()` 实现**必须**自带同一份同意闸门判断（复用 `config.cloudAiConsentGranted()`，与 `complete()` 的判据完全同源，见 GC-13）。
  - **实施顺序**：`BLUEPRINT_STATE.md` 当前 `TURN=CODE` 指向 L1，本批（K1i-1）在 L1 之后实施最省事（`cloudAiConsentGranted()` 已存在，直接复用）；若因故本批先于 L1 落地，`stream()` 实现里对应的闸门判断留一个 `// TODO(L1): 见 L1 蓝图`桩，且 CODE **必须**在提交里同时更新两份蓝图的交付台账互相引用，防止后落地的那份忘记补上闸门（此为唯一允许延后的例外，且必须显式记录，不得默认"以后再说"）。

### 1.3 In Scope / Out of Scope

**In Scope（批 K1i-1：运行时真实流式委托）**：
1. `SwitchableAiRuntime` 新增 `override fun stream(request: LlmRequest): Flow<LlmStreamEvent>`，委托给 `runtimes[activeType()]` 的真实 `stream()` 实现（含 L1 同意闸门复用，见 §1.2）。
2. 新增测试覆盖：验证委托后真实收到底层 runtime 的多个 Delta（而非默认包装的单个 Delta），验证同意闸门在 `stream()` 路径同样生效，验证 `runtimes` 找不到对应类型时的回退行为。
3. 不改 `AiMealInputViewModel`/`AiMealPrompt`/UI 层任何代码——它们已经在消费 `Flow<LlmStreamEvent>`，本批只是让这个 Flow 的产生方式变得真实。**消费方代码零改动，但消费到的解析结果的产生路径会切换**（见 §2 D4：从"整体 JSON fallback"切到"NDJSON 逐行"），这不是"消费方零感知"，已用 E-K1I-01 单独把关（见 §8.2）。

**Out of Scope（批 K1i-2，显式弃置为独立未来批次）**：
- **把 AI 推荐（`AiRecommendViewModel`）/AI 周计划（`AiPlanViewModel`）/AI 记一餐健康建议（`confirmHealthAdvice()`）等目前用 `complete()`（非 `stream()`）的功能改造成流式渐进展示**——这些功能当前的 AI 输出协议是"一次性完整 JSON"（`{"suggestions":[...]}"`/`{"days":[...]}"`），要让它们真正"渐进出现"，必须先把输出协议本身改造成 NDJSON（逐条建议/逐天逐餐边生成边输出），这是**协议设计 + Prompt 重写 + 解析器重写 + 状态机设计 + UI 设计**的完整批次，量级参考 AI 记一餐当年的 B1~B6（六个批次、多轮架构复核）。本批（K1i-1）修的是"运行时分发层的委托缺陷"，与"某功能要不要上 NDJSON 协议"是两个不同维度的问题，即使本批修复完成，AI 推荐这类用 `complete()` 的功能依然不会自动获得渐进展示（`complete()` 本身语义就是"等完整结果"，不会因为 `stream()` 修好了而改变）。
- **NDJSON 逐行解析器对"跨 Delta 边界不完整行"的缓冲拼接能力**——v1 稿曾把这个列为需要真机验证的风险点，独立挑战核实**该能力已存在且已被单测覆盖**（`StreamingMealParser.kt:44-63` 的 `feedDelta()` 按 `\n` 缓冲完整行、残行写回缓冲；`StreamingMealParserTest.kt:66-76` 已有"字符串中间切开两个 Delta"的用例）。本批不需要为此新增测试或真机项，直接引用既有证据即可，**本批真正需要真机把关的是 §2 D4 那条协议切换风险**，不是这条。

### 1.4 上一批延后项归宿核对（GC-03）

除 §1.2 所述与 L1 的依赖关系（已显式处理）外，与其余近期批次（K1a/AI快捷记主线/K1e）无功能交集。

---

## §2 现状与差距

| # | 现状 | 证据（file:line） | 差距/影响 |
|---|---|---|---|
| D1 | `SwitchableAiRuntime` 只 override `complete()`，未 override `stream()` | `shared/.../ai/AiRuntimeConfig.kt:69-80` | 全 App 任何调用 `aiRuntime.stream(...)` 的代码（当前唯一消费方是 AI 记一餐）实际收到的是 `AiRuntime` 接口默认实现（`AiRuntime.kt:42-53`）包装出的"单 Delta + Completed"，不是真实网络分片 |
| D2 | `CloudAiRuntime.stream()` 已有完整真实 SSE 实现（`callbackFlow` + `StreamTransport` + 重试/取消/`finish_reason` 处理） | `androidApp/.../ai/CloudAiRuntime.kt:75-163`（约） | 这份代码在生产环境**完全不可达**——因为 DI 注入进业务代码的是 `SwitchableAiRuntime`（`AiRuntimeConfig.kt` 类型 `AiRuntime`），业务代码从未直接持有 `CloudAiRuntime` 实例，`stream()` 调用永远走 `SwitchableAiRuntime`（进而走默认包装），`CloudAiRuntime.stream()` 的真实分片逻辑是死代码 |
| D4（v1b 新增，独立挑战核实出的本批真正风险点） | `complete()` 路径用 `GlmProtocol.buildRequestBody(..., jsonMode = model.supportsJsonMode)`（`CloudAiRuntime.kt:37`），默认模型 `supportsJsonMode=true` → 请求体带 `response_format:json_object`，**强制模型只能输出单一 JSON 对象**；`stream()` 路径用 `GlmProtocol.buildStreamRequestBody()`（`GlmProtocol.kt:93-112`）**不带** `response_format` 字段 | `CloudAiRuntime.kt:37`（complete jsonMode） vs `GlmProtocol.kt:93-112`（stream 无此参数）；`CloudModel.kt:31-41`（默认模型 `supportsJsonMode=true`） | 现状因 D1 的默认包装恒走 `complete()`，AI 记一餐的 NDJSON prompt（`AiMealPrompt.kt:99-112` 明确要求逐行 NDJSON）**实际几乎总被强制成单一 JSON 对象**，解析器（`StreamingMealParser.kt`）在生产上一直在走"整体 JSON 规范化 fallback"（`:470` `tryWholeJsonFallback()`）这条**非设计主路径**，而非 prompt 本意的逐行严格校验路径。本批修好 `stream()` 委托后，请求体不再强制 `json_object`，模型才会真正按 NDJSON 输出，**解析主路径会切换**——这是本批唯一需要真机把关的真实行为风险，不是"分片粒度"问题（那部分`StreamingMealParserTest.kt:66-76` 已有单测覆盖，v1 蓝图误判了风险点） |
| D3 | K1a 蓝图 GC-37 挑战 #14（`AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md` §12）已经记录过这一事实（"`SwitchableAiRuntime` 未 override `stream()`，AI 记一餐链路实际未走真实 SSE 流式"），但当时判定"与本批(K1a)无关，独立待办不修复" | K1a 蓝图 §12 弃置项表 | 本蓝图是该"独立待办"的正式落地，非新发现——K1a 已经预留了这个坑，本批填 |

---

## §3 不变量

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-K1I-01 | `SwitchableAiRuntime.stream(request)` 被调用，`activeType()` 有对应的已注册 runtime | 委托给 `runtimes[activeType()].stream(request)`，产出的 `Flow<LlmStreamEvent>` 原样透传（不额外包装/拆分/合并事件） | 内部再调用一次 `complete()` 把 `stream()` 退化回"假流式"（这正是当前默认实现的行为，本批要替换掉它，不能等价重造） | T-K1I-01 |
| INV-K1I-02（复用 L1，见 §1.2） | `stream()` 被调用，`activeType()==CLOUD` 且 `!config.cloudAiConsentGranted()` | 不委托给 `CloudAiRuntime`，直接 `emit(LlmStreamEvent.Failed(message=..., retryable=false))`（message 与 `complete()` 路径的 `CloudAiConsentRequiredException().message` 完全一致，同源判据同源文案） | 绕过闸门直接委托；闸门判据与 `complete()` 路径不一致（如误用别的判断条件） | T-K1I-02 |
| INV-K1I-03 | `activeType()` 对应的 runtime 在 `runtimes` map 里找不到（既有防御分支，`complete()` 里已有对应处理） | `stream()` 回退到 `runtimes[MOCK]`；再找不到则 `emit(Failed(...))`，与 `complete()` 的回退逻辑语义对齐（不新造一套不一致的回退规则） | `stream()` 与 `complete()` 在"找不到 runtime"这件事上出现不一致行为（如一个回退 MOCK 一个直接崩溃） | T-K1I-03 |
| INV-K1I-04 | 调用方取消对 `stream()` 返回 `Flow` 的收集（如 `AiMealInputViewModel` 的 generation 被 `invalidateGenerationToInput()` 取消） | 取消信号透传到底层 `runtime.stream()`（`CloudAiRuntime.stream()` 内部已有 `activeCall`/`Job` 取消处理，本批不改该部分，只需保证委托链路不吞掉取消信号） | `SwitchableAiRuntime.stream()` 的委托实现用了会拦截取消传播的写法（如内部另起不受结构化并发管辖的协程） | T-K1I-04 |

---

## §4 接口契约

### 4.1 逐字段真相源表

| 字段/行为 | 唯一写入者 | 读取方 | 终局形态 |
|---|---|---|---|
| `SwitchableAiRuntime.stream()` 的返回 `Flow<LlmStreamEvent>` | 本函数内部一次性构建（每次调用独立） | 全部既有 `stream()` 消费方（当前仅 AI 记一餐），零改动 | 新增 override，替代继承自接口的默认实现 |

### 4.2 GC-11：行为迁移清单

**迁移对象：`AiRuntime.stream()` 默认实现 → `SwitchableAiRuntime` 显式 override**

旧行为（默认实现，`AiRuntime.kt:42-53`）：调 `complete()` 一次，成功则 `emit(Delta(全部文本)); emit(Completed(...))`，失败则 `emit(Failed(...))`。

新行为：直接委托给底层真实 `stream()`。

全部消费方（grep 已核对，仅 1 处生产代码）：

| 消费点 | 影响 |
|---|---|
| `AiMealInputViewModel.kt:494`（`aiRuntime.stream(llmRequest)`） | 零代码改动；行为上从"一次性收到全部内容"变为"陆续收到网络真实分片"，`StreamingMealSession`/NDJSON 逐行解析器需要能正确处理"一个 Delta 可能不是完整一行"的情况——**这不是本批要修的代码，是本批要真机验证的既有代码是否已经扛得住**（见 §1.3 Out of Scope 说明 + §9 真机项） |

**终局裁决**：**替换**（默认实现→显式 override，AI 接口层面无字段变化，只是"谁提供了实现"这件事变了）。

### 4.3 GC-12：N/A（见 §0.1）

### 4.4 新增/变更函数签名

```kotlin
// shared/.../ai/AiRuntimeConfig.kt（SwitchableAiRuntime 新增 override）
class SwitchableAiRuntime(
    private val config: AiRuntimeConfig,
    private val runtimes: Map<AiRuntimeType, AiRuntime>,
) : AiRuntime {
    override suspend fun complete(request: LlmRequest): Result<String> {
        // ... 既有实现不变（含 L1 已新增的同意闸门，若 L1 尚未落地则是本批实施时的既有原始实现）...
    }

    /**
     * [AI生成] K1i：真实委托给底层 runtime 的 stream()，替代 AiRuntime 接口默认的"complete() 包装成假流式"。
     *
     * 同意闸门与 [complete] 复用同一判据 [AiRuntimeConfig.cloudAiConsentGranted]（GC-13，防止 stream() 路径
     * 绕开 L1 建立的闸门——见蓝图 §1.2 依赖说明）。
     */
    override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = flow {
        val type = config.activeType()
        if (type == AiRuntimeType.CLOUD && !config.cloudAiConsentGranted()) {
            emit(LlmStreamEvent.Failed(message = CloudAiConsentRequiredException().message ?: "还没有同意把数据发给云端 AI", retryable = false))
            return@flow
        }
        val runtime = runtimes[type] ?: runtimes[AiRuntimeType.MOCK]
        if (runtime == null) {
            emit(LlmStreamEvent.Failed(message = "no AiRuntime registered for $type", retryable = false))
            return@flow
        }
        emitAll(runtime.stream(request))
    }
}
```

**若本批先于 L1 落地**（§1.2 允许的唯一例外）：上述代码块里 `if (type == AiRuntimeType.CLOUD && !config.cloudAiConsentGranted()) {...}` 这三行整体注释掉并标 `// TODO(L1 落地后取消注释，见 L1 蓝图 §4.4 SwitchableAiRuntime.complete())`，CODE 必须在两份蓝图的 §9 交付台账里互相记一笔"依赖对方未完成，已留桩"。

### 4.5 挂起点清单（GC-31）+ 取消语义（GC-14~16）

| 挂起点 | 说明 |
|---|---|
| `flow { val type = config.activeType(); ... }` 内的 `config.activeType()`/`config.cloudAiConsentGranted()` | 均为轻量偏好读取（无网络），挂起时间可忽略；无 generation 概念（本类不感知上层业务的 generation 隔离，那是调用方 `AiMealInputViewModel` 自己的职责，与本类无关，见 GC-31 判据） |
| `emitAll(runtime.stream(request))` | **取消透明性是本 STEP 的核心正确性要求**：`emitAll` 是 kotlinx.coroutines 标准的"透传上游 Flow 的取消/异常"操作符，收集方（调用方）取消对 `SwitchableAiRuntime.stream()` 返回值的收集时，取消信号沿 `emitAll` 传播到 `runtime.stream(request)` 内部（`CloudAiRuntime.stream()` 的 `callbackFlow` 已有 `awaitClose`/`activeCall` 取消处理，`CloudAiRuntime.kt` 本批不改），不需要本类额外做取消转发——`flow{}` builder 本身就是结构化并发的，不引入新的取消处理逻辑（GC-15：本类不持有任何需要显式传递的可变持有物，`emitAll` 是唯一合法的委托写法） |

### 4.6 GC-13：fallback 复用主路径校验入口

`stream()` 的同意闸门与 `complete()` 复用同一个 `config.cloudAiConsentGranted()`（L1 已定义），不新造判据；两处失败文案也复用同一个 `CloudAiConsentRequiredException().message`，保证"用户看到的拒绝理由"在 `complete()`/`stream()` 两条路径上完全一致。

---

## §5 UI 设计

不适用（本批不涉及任何 UI/Compose 改动）。

---

## §6 文件改动清单 + Allowlist

| 文件 | 允许操作 | 禁止操作 |
|---|---|---|
| `shared/.../ai/AiRuntimeConfig.kt` | `SwitchableAiRuntime` 新增 `override fun stream(...)`（按 §4.4）；**若 L1 已落地，额外允许删除/改写 `complete()` 函数体内那一行"仅本批新增的同意闸门分支"周边不涉及**——但若 L1 尚未落地、本批先行，**不改 `complete()` 本身**（那是 L1 的范围） | 改 `complete()` 除新增同意闸门以外的既有逻辑；改 `activeType`/`setActiveType`/`selectedModel`/`vendorApiKey`/`setVendorApiKey`/`currentCloudApiKey`/`isModelReady`/`cloudAiConsent`/`setCloudAiConsent`/`cloudAiConsentGranted`（L1 已定义或将定义的方法，本批只读引用不改） |
| 新建 `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/ai/SwitchableAiRuntimeStreamTest.kt` | 新增 T-K1I-01~04 | — |
| `docs/feature/L1_云端AI首启同意与合规免责_实施蓝图.md`（v1b 新增授权，仅当 L1 晚于本批落地时适用） | 该文件 §4.4 代码片段里"`// stream() 不重写：...本次改动零新增行为分歧`"这一整行注释，以及 §0.1 GC-13 对应行的论证文字，**必须**同步删除/改写为"`stream()` 已由 K1i 批次重写，同意闸门见 K1i 蓝图 §4.4"——否则该注释在 K1i 落地后成为生产代码里的假话（独立挑战第3项） | — |

**显式禁改文件清单**：
- `androidApp/.../ai/CloudAiRuntime.kt`（其 `stream()` 真实 SSE 实现已存在且正确，本批只是让它变得可达，不改它本身）
- `shared/.../ai/meallog/*`（AI 记一餐主线，本批预期它零改动即可受益，不主动碰）
- `androidApp/.../ui/ai/*`（本批不涉及任何 UI）
- L1 蓝图涉及的所有文件（除本批显式需要读取的 `cloudAiConsentGranted()`/`CloudAiConsentRequiredException`，只读引用不修改其定义）

**回归基线锁定（GC-09）**：
- `:shared:testDebugUnitTest`（全量，0 failures）
- `:androidApp:testDebugUnitTest`（全量，含既有 `AiMealInputViewModelStreamTest` 零改动仍全绿——本批预期该文件的既有 fake `AiRuntime`/`sessionPort` 测试基础设施不受影响，因为那些测试本就不经过真实 `SwitchableAiRuntime`）
- `:androidApp:assembleDebug`

---

## §7 分阶段实施步骤

### 批 K1I-1：运行时真实流式委托

**STEP-K1I-1.1**：确认 `AiRuntimeConfig.kt` 内是否已存在 `cloudAiConsentGranted()`/`CloudAiConsentRequiredException`（`grep "fun cloudAiConsentGranted" AiRuntimeConfig.kt`）。**当前代码库现状是两者均不存在**（L1 蓝图虽已 `BLUEPRINT_READY` 但尚未被 CODE 实施），故 CODE 执行本批时大概率走"留桩"分支（§4.4 的注释掉三行写法）——这是**当前默认主路径**，不是边缘例外；若届时 L1 已先落地，才走 §4.4 的正式代码块分支。两种分支实施后都要在本批 §9 交付台账记一笔"当时 L1 是否已落地"。
完成形态：`grep "fun cloudAiConsentGranted" AiRuntimeConfig.kt` 命中数（0 或 1）人工核对后决定走哪条分支，不可省略这步直接假设。

**STEP-K1I-1.2**：`AiRuntimeConfig.kt` 的 `SwitchableAiRuntime` 按 §4.4 新增 `override fun stream(...)`。
完成形态：`grep "override fun stream" AiRuntimeConfig.kt` 命中 1 处；`grep "emitAll(runtime.stream(request))" AiRuntimeConfig.kt` 命中 1 处。

**STEP-K1I-1.3**：确认 `AiRuntimeConfig.kt` 顶部已 import `kotlinx.coroutines.flow.flow`/`kotlinx.coroutines.flow.emitAll`（现状文件是否已有需先 grep 核实，缺则补）。
完成形态：`grep "import kotlinx.coroutines.flow.flow" AiRuntimeConfig.kt` 命中 1 处；`grep "import kotlinx.coroutines.flow.emitAll" AiRuntimeConfig.kt` 命中 1 处。

### 批 K1I-T：测试

**STEP-K1I-T-1**：新建 `SwitchableAiRuntimeStreamTest.kt`，覆盖 T-K1I-01~04（见 §8.2），用 fake `AiRuntime`（构造 `runtimes` map 传入，既有可测试性设计，见 §0.1 GC-33）模拟"多个 Delta"验证真实委托（而非默认包装的单 Delta）。

**验收命令**：
```
scripts\build-cli.bat :shared:testDebugUnitTest
scripts\build-cli.bat :androidApp:testDebugUnitTest
scripts\build-cli.bat :androidApp:assembleDebug
```

---

## §8 测试矩阵

### 8.1 测试夹具职责边界（GC-07）

| 夹具 | 职责 | 禁止 |
|---|---|---|
| fake `AiRuntime`（`stream()` 返回预设的**多个** `Delta` 事件 Flow，如 `flowOf(Delta("段1"), Delta("段2"), Completed(...))`） | 验证 `SwitchableAiRuntime.stream()` 委托后收到的事件序列与 fake 的**多 Delta**完全一致（区别于默认实现只可能产出单 Delta），这是本批"真委托 vs 假包装"的核心区分测试 | 让 fake 直接返回业务终态掩盖委托逻辑本身是否正确 |

**测试策略修订（v1b）**：独立挑战核实 `:shared` 模块的 `androidUnitTest` 测试 classpath **没有** `kotlinx-coroutines-test`（该库只在 `androidApp/build.gradle.kts` 硬编码为 test 依赖），而本批 §6 allowlist 未授权改 `shared/build.gradle.kts`/`libs.versions.toml`。故全部测试改用 `kotlinx.coroutines.runBlocking`（`shared` 模块已有先例，`FlowIntegrationTest.kt:16`），**不使用** `TestScope`/`runTest`。T-K1I-04 的取消测试用 `runBlocking { val job = launch { runtime.stream(req).collect{} }; syncPoint.await(); job.cancelAndJoin() }` 写法，fake runtime 的 `stream()` 内先 `emit(Delta("x"))` 建立一个确定性同步点（外层收到后再触发 cancel），再 `awaitCancellation()`，`finally` 里置位 `cancelled=true` 供断言，避免无同步点的竞态测试。

### 8.2 INV↔T 双向映射表

| INV | T-ID | 断言要点 |
|---|---|---|
| INV-K1I-01 | T-K1I-01 | fake CLOUD runtime 的 `stream()` 返回 3 个 Delta，`SwitchableAiRuntime.stream()` 收集到的事件序列**恰好是这 3 个 Delta**（而非默认实现产出的 1 个大 Delta）——直接证伪"委托"与"包装"的行为差异；用 `runBlocking { runtime.stream(req).toList() }` |
| INV-K1I-02 | T-K1I-02 | `activeType=CLOUD`+consent 未满足（**仅当 L1 已落地时此测试才适用**，见 STEP-K1I-1.1 分支判断，若走留桩分支则本条暂缓，随 L1 落地后补齐并在两份蓝图交付台账互相记录）→ `stream()` 首个（唯一）事件是 `Failed(message=CloudAiConsentRequiredException 同款文案, retryable=false)`，fake CLOUD runtime 的 `stream()` 断言**零调用** |
| INV-K1I-03 | T-K1I-03 | `runtimes` map 不含 `activeType()` 对应类型 → 回退 `MOCK`；`runtimes` 两者都没有 → `Failed(...)`；`runBlocking { ... }` |
| INV-K1I-04 | T-K1I-04 | 见上方"测试策略修订"给出的 `runBlocking + launch/cancelAndJoin + 同步点` 写法，断言 fake runtime 的 `cancelled` 标志在 `job.cancelAndJoin()` 后为 `true` |
| INV-K1I-01/D4 | 真机 E-K1I-01（GC-08，**阻断性**真机项，v1b 由"非断言项"升级） | 配置真实云端 AI，AI 记一餐快速记+周期记各跑一次，断言（非仅"观察"）：①单次生成过程中 `AppLogger`/诊断信息里出现 **≥2 次** Delta 事件（证明真委托生效，而非改造前恒定的 1 次）；②诊断列表**不再出现**"未检测到 NDJSON 事件，已按整体 JSON 格式规范化"这条 WARNING（改造前因 D4 描述的 `response_format` 强制而几乎必然出现，改造后应消失，因为模型开始真正输出 NDJSON）；③最终解析出的餐食/菜品内容与改造前对同一输入的结果**等价**（人工比对，允许措辞细节差异但结构/数量不能变）；④若①②③任一不满足，回退方案：`AndroidModule.kt` 里 `SwitchableAiRuntime` 绑定改回不接线本批新 override（即临时 revert `AiRuntimeConfig.kt` 单文件），不需要连带 revert 其他文件 |

---

## §9 交付台账（CODE 完成时填）

### STEP 勾销表

| STEP-ID | 状态 | 落地 commit | diff 定位 |
|---|---|---|---|
| STEP-K1I-1.1~1.3 | ⬜ | | |
| STEP-K1I-T-1 | ⬜ | | |

### 与 L1 的交叉引用（若实施顺序交叠，见 §1.2/§4.4）

（交付时填：若本批与 L1 交叠实施，在此记录桩代码位置+对方蓝图交付台账的对应记录）

### 验收命令输出 / 真机待验证登记

（交付时填；`E-K1I-01` 登记至时间戳最新的真机待验证清单）

---

## §10 独立挑战台账（GC-37）

**挑战方**：独立 Explore/opus agent（只读，未见起草过程，只给蓝图成文 + 逐条核对当前真实源码）。

**结论**：核心技术判断成立——取消传播语义（`awaitClose` 的 `finally` 保证 + `activeCall` 竞态窗口分析）、shared/androidApp 解耦与 DI 绑定、其他 `AiRuntime` 实现类不受影响、唯一消费方定位，均经独立核实 `CONFIRMED-FINE`。挑出 3 项 `CONFIRMED-ISSUE` + 若干 `MINOR-NIT`，均已就地处置：

| # | 挑战摘要 | 裁决 | v1b 处置 |
|---|---|---|---|
| 1 | `emitAll(callbackFlow)` 取消传播是否真的正确 | CONFIRMED-FINE | 独立核实 `awaitClose` 的 `finally` 语义 + `activeCall` 保留策略 + 无竞态窗口，无需改动 |
| 2 | shared/androidApp 解耦、DI 绑定是否如蓝图所述 | CONFIRMED-FINE | 无需改动 |
| 3 | 与 L1 的反向依赖是否闭合（L1 那句"stream() 不重写"注释会被本批变成假话，且 allowlist 未授权改它） | **CONFIRMED-ISSUE** | §6 allowlist 新增一行显式授权修改 L1 文件对应注释；STEP-K1I-1.1 重写为"L1 未落地是当前默认主路径"而非边缘例外 |
| 4 | NDJSON 解析器缓冲拼接能力 + 本批真正的行为风险 | **CONFIRMED-ISSUE**（风险点判断错误，真风险是协议切换非分片粒度） | §2 新增 D4；§1.1/§1.3/GC-12/GC-20 相应改判；E-K1I-01 从"非断言主观项"升级为带 4 条明确判据+回退方案的阻断性真机项 |
| 5 | 其他 `AiRuntime` 实现类是否受影响 | CONFIRMED-FINE | 无需改动 |
| 6 | 测试可行性（`TestScope` 在 shared 模块是否可用） | **CONFIRMED-ISSUE** | §8.1/§8.2 测试策略改为 `runBlocking`+`launch/cancelAndJoin`+同步点写法，不依赖 `kotlinx-coroutines-test` |
| 7 | §1.1 价值声明与 B5 既有节流是否冲突（"段内逐条出现"的收益是否真实） | MINOR-NIT | §1.1 改写为"首个预览更快出现"，明确段内逐条刷新是 K1i-2 的事 |
| 8 | GC-32"频率不变"判断是否成立 | MINOR-NIT | 由 N/A 改判"满足"，如实描述频率从 1→N、引用既有 B5 节流 |
| 9 | `retryable` 字段两条路径不一致、行号漂移 | MINOR-NIT | 已知差异记录，无实际影响（全仓无生产代码读取该字段），行号已订正 |

处置后自查：第 3/4/6 三项（唯三 CONFIRMED-ISSUE）均已在 §1/§2/§4/§6/§7/§8 同步修订，非仅局部打补丁。**蓝图转 `BLUEPRINT_READY`**。

---

## §11 门禁与角色

- 本批不涉及 UI/交互/合规文案，豁免 `apple_ux_designer`/`apple_software_behavior`/`copywriter` 前置门禁。
- CODE 完成、构建+单测通过后，须走 `google_quality_engineer` 代码质量终审；鉴于本批改动点是全 App AI 请求的唯一分发入口，建议同时过 `google_architecture_engineer` 视角，核对是否存在其他隐藏的 `AiRuntime` 直接实例化点会绕开 `SwitchableAiRuntime`（该项已在 L1 蓝图 §10 v2 挑战第 1 项独立核实过"全仓仅 `SwitchableAiRuntime` 一条分发路径"，本批可直接引用该结论，不必重复排查，除非代码库在此期间有新变化）。

## §12 弃置项登记（GC-03 前瞻）

| 项 | 状态 | 归宿 |
|---|---|---|
| K1i-2：AI 推荐/周计划/健康建议改造为 NDJSON 渐进协议+UI 展示 | 显式弃置为独立未来批次 | 量级参考 AI 记一餐 B1~B6，需要独立的 apple_ux_designer 设计门禁+多轮架构复核，不与本批（运行时委托修复）捆绑，见 §1.3 |
| AI 记一餐 NDJSON 逐行解析器对"真实分片粒度"的健壮性专项测试 | 显式弃置，转真机验证项 | 见 §1.3/§9，本批交付后走一次真机验证而非新增单测（网络分片的真实粒度无法在单测里稳定模拟，真机是唯一能观察真实行为的地方） |
