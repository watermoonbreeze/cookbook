# L1：云端 AI 首启同意 + 合规免责声明 实施蓝图

> 状态：`BLUEPRINT_READY`（v2b，经两轮独立 GC-37 挑战：第一轮判定核心设计前提有误、要求结构性返工，已重做闸门落点（§1.3/§4.4）；第二轮确认核心设计成立、挑出 7 项局部设计缺口，已就地处置，见 §10 完整记录。**下一步：`BLUEPRINT_STATE.md` 同步 `TURN=CODE`，交由后续 CODE 角色按本文机械执行**）
> **本蓝图仅由 ARCH 起草，不含任何代码实现**——按用户 2026-08-08 指示"你只负责蓝图，不要编码"，本批不进入实施阶段，交由后续 CODE 角色按本文机械执行。
> **颗粒度：L7**（项目基线；GC 条款清单见 `experience/12_多模型协作与实施蓝图规范.md` §12）。**§0.1 是入口——先读它，逐行对照落点章节。**
> 起草日期：2026-08-08（v1 起草后即时派独立挑战 agent，判定核心设计前提"把同意收进 `isModelReady()`"与代码事实不符——`isModelReady()` 在现状代码里是展示/编排语义判据，不是任何调用点的真实网络出口闸门，全 App 至少 3 条路径可绕过它直接外发。v2 重新设计闸门落点为运行时分发层 `SwitchableAiRuntime.complete()`，并解决 v1 的对话框状态机、grandfather 判据覆盖、测试可执行性等全部阻断项）。
> 前置门禁：本批涉及"App 自动行为"（云端 AI 首启同意）与"新交互/新弹窗"，已按 CLAUDE.md 强制门禁分别派 `apple_software_behavior`（行为契约）与 `apple_ux_designer`（交互/视觉规范）两个独立 agent 产出设计输入，本蓝图 §3/§4/§5 直接转译其结论（v2 未推翻其行为/视觉判断，只修正 v1 自己在"落地成代码契约"这一步引入的错误）。

---

### §0.1 颗粒度勾销表（GRANULARITY = L7）

| GC | 条款一句话 | 本蓝图落点 | 状态 |
|---|---|---|:--:|
| GC-01 | 每个行为分支写成"条件→唯一动作→禁止动作"，对歧义词 grep 零命中 | §3 不变量表 | 满足 |
| GC-02 | allowlist：文件×允许操作×禁止操作 + 显式禁改清单 | §6 | 满足 |
| GC-03 | 上一批延后项归宿表 | §1.4 | 满足 |
| GC-04 | 每条 INV 具备 ID/条件/必须结果/禁止结果/证据五列 | §3 | 满足 |
| GC-05 | INV↔T 双向映射表 | §8.2 | 满足 |
| GC-06 | 放行条件写出命令原文 | §7 末尾"验收命令" | 满足 |
| GC-07 | 测试夹具职责边界表 | §8.1 | 满足（v2 已修正为真实例+内存库，见 §10 C-17 处置） |
| GC-08 | 交付台账含真机清单文件名+编号区间 | §9 | 满足 |
| GC-09 | 列出本批不得失败的既有回归套件全名 | §6 末尾"回归基线锁定" | 满足 |
| GC-10 | 逐字段真相源表 | §4.1 | 满足 |
| GC-11 | 新增/重命名与既有字段语义重叠字段时给旧字段全部写入点清单 | §4.2 | 满足 |
| GC-12 | UI 判据与业务判据同源表 | §4.3 | 满足 |
| GC-13 | fallback 先转换为主路径类型再复用主路径校验入口 | §4.6 | 满足：v2 新增内容——`SwitchableAiRuntime` 的同意拦截复用 `AiRuntime.stream()` 默认实现已有的 `Result.failure→LlmStreamEvent.Failed` 转换入口，不新造 fallback 类型 |
| GC-14~16 | 对象生命周期表 / 可变持有物传递形态 / 搬迁历史注释清单 | §4.5 | 满足（v2：`CloudAiKeySetupDialog` 拆分，Key 草稿改为宿主 `AiSettingsScreen` 持有，见 §10 C-11/C-12 处置） |
| GC-17~19 | 逐项状态 List&lt;Status&gt; / 索引空间标注 / 过滤链画出 | — | N/A：本批状态是单值枚举，非逐项列表状态机场景 |
| GC-20 | 自动副作用清单表 | §3 INV-L1-08 | 满足 |
| GC-21 | INV 写"提示/告知"字样必须有 STEP 落点 | §7 各 STEP | 满足（v2 修正 Snackbar 落点，见 §10 C-13 处置） |
| GC-22 | 每条可见副作用配 T-ID/真机项编号 | §8.2 | 满足 |
| GC-23 | 实施脚本每个最小动作独立编号 STEP，含文件/定位/动作/完成形态 | §7 | 满足（v2 拆分了 v1 过粗的 STEP-L1-4.1，见 §10 C-12 处置） |
| GC-24 | 交付台账含 STEP 勾销表，Evidence 只能引用真实存在的测试/commit | §9 | 满足 |
| GC-25 | STEP 完成形态是字面量时写出目标字面量原文+grep判据 | §7 每条 STEP 末尾 | 满足 |
| GC-26 | 冻结值修订记录表 | — | N/A：本批不改任何已冻结阈值/常量 |
| GC-27 | 编辑即失效收口函数核对表 | — | N/A：本批不涉及 AI 记一餐的 `invalidateGenerationToInput` |
| GC-28 | 构造时创建、后续多次迭代复用对象是否按基数分片 | — | N/A：`CloudAiConsent` 每次读写都是完整值对象 |
| GC-29 | 多来源写入同一聚合目标必须声明合并/覆盖 | §4.1 | 满足（v2：五个动作函数改为"构造一份完整 `next` 值对象→同一对象既持久化又镜像进 state"，杜绝 v1 的内存态部分镜像 bug，见 §10 C-06 处置） |
| GC-30 | 状态转移驱动完整副作用链 | §3 INV-L1-03~08 | 满足 |
| GC-31 | 挂起点清单 + 恢复后重新校验 generation 身份 | §4.5 | 满足 |
| GC-32 | 高频异步事件的节流/去重策略 | — | N/A：全部动作为用户单次点击触发，`isModelReady()` 保持不变未新增调用频次（v2 不再修改 `isModelReady()`，见 §10 C-01 处置） |
| GC-33 | 禁止为测试暴露新的可变全局注入点 | — | N/A：v2 测试策略改用真实 `PreferenceRepository`/`AiRuntimeConfig` + 内存 SQLite（见 §10 C-17 处置），不新增任何 `var`+`replaceXxxForTest` |
| GC-34 | 复核注释/KDoc 与实现一致性 | §7 每条 STEP | 满足（v1 §4.4 一处失实注释已删，见 §10 C-07 处置） |
| GC-35 | 协议事件枚举与处理分支逐项对照 | §4.6 | 满足：`LlmStreamEvent` 三态（Delta/Completed/Failed）本批新增的"同意未满足"信号复用既有 `Failed` 分支，不新增第四态，无需扩枚举 |
| GC-36 | 数据层 List&lt;Status&gt; 前先列真实状态空间 | §3 脚注 | 满足 |
| GC-37 | 蓝图冻结前存在独立挑战台账 | §10 | 满足——两轮独立挑战均已完成（v1 结构性、v2 局部），v2 挑出的 7 项问题已就地处置，见 §10 |

全部 GC 条款满足，蓝图已转 `BLUEPRINT_READY`。

---

## §1 目标与范围

### 1.1 一句话价值

用户首次真正启用云端 AI（保存某厂商 API Key 生效）前，先看清楚"会发给谁、发什么、不发什么、能不能反悔"，同意后才真正联网；**真正的联网闸门落在所有云端调用共用的运行时分发层，而不是某个 UI 入口**，因此无论用户从哪条路径（设置页单选、填 Key、重新选模型）尝试触发云端调用，未同意状态下都不会有数据离开设备。同时把隐私政策里对"是否上传数据"的表述从"绝对不上传"订正为"默认不上传，除非你自己开了云端 AI"。

### 1.2 触发来源

- **L1**（`待办_工程合规.md` 🔴⬜）：用户协议开头免责声明 + 首次开启云端AI弹窗告知第三方服务风险。
- **既有缺口**：`AiSettingsViewModel.onSaveVendorKey()` 目前保存 Key 后立即生效外发，全程零拦截、零同意；隐私政策 §一"你录入的信息只存在本机（不上传）"在用户启用云端 AI 后成为误导性表述（详见 §2）。

### 1.3 In Scope / Out of Scope

**In Scope**：
1. 新增 `CloudAiConsent` 同意状态模型（shared，偏好 JSON 存储，免迁移）：`NOT_ASKED`/`GRANTED`/`DECLINED`/`GRANDFATHER_PENDING` 四态。
2. **真正的联网闸门**：`SwitchableAiRuntime.complete()`（全 App 唯一的云端调用分发入口，AI 记一餐/AI 推荐/AI 周计划/未来任何新 AI 功能全部经过这一个函数）新增同意校验——`activeType()==CLOUD` 且同意未满足时，直接返回 `Result.failure(CloudAiConsentRequiredException(...))`，**不路由到 `CloudAiRuntime`**。此举利用 `AiRuntime.stream()` 默认实现已有的"失败→`LlmStreamEvent.Failed`"转换与各消费点**已经存在**的失败兜底逻辑（AI 记一餐的 `attemptRuleFallback()`、AI 推荐/周计划的 `RecommendationOrchestrator` 内 `RULE_FALLBACK` 分支），做到"零改动、真隔离"（v1 声称的这一效果实际不成立，v2 通过改变闸门落点使其成立，见 §10 C-01/C-28 处置）。
3. `AiRuntimeConfig.isModelReady()` **保持不变**（v1 曾计划修改它，经挑战证实它不是真实闸门、改它既无必要也会产生误导性的"标签与实际执行不一致"问题，v2 撤销该改动，见 §10 C-01 处置）。
4. `KeyDialog`（现有"填 Key"弹窗）保留为独立 `AlertDialog`（**不**与同意面板合并进同一个 `Dialog` 双页组件——v1 的"合并两页"设计在挑战中被证实产生无法实现的回调契约与未定义的弹层状态协调，v2 改为宿主 `AiSettingsScreen` 直接管理多个独立弹层的显隐，按顺序打开/关闭，见 §10 C-11/C-12 处置）；点"保存"后按同意状态分流到"直接保存"/"轻量换厂商确认"/"完整同意面板"三条路径之一，分流判断抽成纯函数 `routeOnSave()`（androidApp 层，非 Compose，可直接 JVM 单测）。
5. AI 设置页 `CloudSection` 新增常驻状态块（已启用/接收方/同意时间/发送哪些内容/关闭），"关闭"走 `ActionSheet` 两档（保留密钥/删除密钥），默认项在前、破坏项标红在后。
6. 存量已配置云端 Key 的老用户：进入 AI 设置页时若检测到"任一厂商有 Key 但无同意记录"，弹出差异化措辞（"这项功能你已在使用"）的补确认面板（grandfather 检测覆盖**全部**厂商，非仅当前选中模型的厂商，见 §10 C-14 处置）。
7. `PolicyContent.kt`：隐私政策 §一末尾加限定句，新增独立小节披露云端 AI 的正/负面发送清单与接收方说明，同步更新 `.ai-context/docs/feature/隐私政策与用户协议.md`（见 §10 C-21 处置）。

**Out of Scope（本批不做，理由见括号）**：
- **`AiSettingsUiState.type` 默认值"顺手修"**（v1 In Scope #7）——经挑战证实这是基于错误事实的改动：App 真实默认运行时就是 `CLOUD`（`AiRuntimeType.from(null) ?: CLOUD`），现有默认值本身没有问题，v1 改成 `MOCK` 反而会制造它自己想修的那类"界面首帧与实际状态不符"问题，v2 撤销（见 §10 C-04 处置）。
- **EngineSourceBadge 统一组件化**——与本批目标是两件事，独立 fast-follow（见 §12）。
- **全 App 云端调用点"静默回退"逐点审计**——AI 记一餐与 AI 推荐已核实具备可见回退提示（挑战独立验证属实，见 §10 C-25），本批新增的"同意未满足"分支复用同一套既有可见提示机制，无需额外审计。
- **"下一次真实云端请求前"作为 grandfather 补确认的第二触发点**——v2 的闸门已落在运行时分发层，即使补确认只在设置页触发、用户没去补确认，`GRANDFATHER_PENDING` 状态下 `cloudAiConsentGranted()` 判定为已满足（见 §1.3 In Scope #1 状态语义），存量用户功能不受影响、无迫痛点；且运行时闸门本身与"哪个 UI 入口提醒用户"是两件独立的事，UI 提醒覆盖面扩大是纯体验加强项，不影响数据是否外发这个安全底线，留独立 fast-follow。
- **政策文案要求老用户重新点一次"我同意"**——本次是补充披露非扩大采集范围，不重新触发首启同意流程。
- **接收方隐私政策跳转链接**——已有"如何申请密钥"指南含官网跳转，不重复加，标为可选加项。

### 1.4 上一批延后项归宿核对（GC-03）

同 v1，均与本批无功能交集。

---

## §2 现状与差距

| # | 现状 | 证据（file:line） | 差距/影响 |
|---|---|---|---|
| D1 | `AiSettingsViewModel.onSaveVendorKey()` 保存 Key 后立即持久化生效，此后任意 AI 入口即真实外发数据，全程零拦截 | `AiSettingsViewModel.kt:58-63` | 用户对"我的数据要发给第三方了"这件事没有任何知情/同意步骤 |
| D2 | 隐私政策 §一标题「你录入的信息只存在本机（不上传）」+ 正文「我们不收集、不上传、也无法访问这些内容」 | `PolicyContent.kt:22,25-26` | 该表述在用户启用云端 AI 后失真 |
| D3 | 全 App 只有 2 处生产代码调用 `AiRuntimeConfig.isModelReady()`（`AiRecommendViewModel.kt:89`、`AiPlanViewModel.kt:94`），它是"是否自动拉取推荐/展示什么标签"的**展示判据**；真正决定"是否发起云端网络请求"的是 `SwitchableAiRuntime.complete()` 按 `activeType()` 路由（`AiRuntimeConfig.kt` 内，不读 `isModelReady()`），以及 `AiMealInputViewModel` 自己的独立窄判据 `configReady`（`activeType==CLOUD && key非空`，同样不读 `isModelReady()`） | `AiRuntimeConfig.kt`（`SwitchableAiRuntime.complete()`）、`AiMealInputViewModel.kt:236`（`configReady` 判据）、`CloudAiRuntime.kt`（内部只判 Key 是否为空） | **这是本批 v1 的核心设计错误**：如果只把同意状态塞进 `isModelReady()`，全 App 至少 3 条真实网络出口路径（`SwitchableAiRuntime` 路由本身、`AiMealInputViewModel.configReady`、`CloudAiRuntime` 的 Key 判据）完全不受影响，用户在设置页里被拒绝/关闭之后，只要 `activeType` 还留在 `CLOUD` 且 Key 还在，任何一个 AI 入口都能继续把数据发出去，界面却显示"规则推荐"（因为 `isModelReady()` 被改成读同意状态后已经是 false）——**呈现为"看起来已合规"但实际未合规**，是最坏的一类缺陷 |

**D3 是 v2 相对 v1 的核心修订**：v1 的 D3/D4 已废弃（原 D4"UiState 默认值不一致"经证实是错误事实，见 §1.3 Out of Scope）。

---

## §3 不变量

**`ConsentStatus` 值域边界论证（GC-36）**：四个值——`NOT_ASKED`、`GRANTED`、`DECLINED`、`GRANDFATHER_PENDING`——覆盖"是否问过 × 是否同意 × 是否历史遗留"全部可区分取值。**`GRANDFATHER_PENDING` 是推导态，不持久化**：只要偏好存储里没有 consent 记录、且检测到任一厂商已有非空 Key，`cloudAiConsent()` 每次读取都会重新推导出这个值；一旦用户在设置页做出任何显式选择（继续使用/关闭），才会真正写入 `GRANTED`/`DECLINED` 落盘。这个"推导态可能随 Key 增删而变化"的性质是刻意设计（见 INV-L1-01），不是缺陷。

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-L1-01 | `AiRuntimeConfig.cloudAiConsent()` 被调用 | **仅当** `prefs.get(KEY_CLOUD_AI_CONSENT)==null`（确无记录，非"有记录但解析失败"）时才进入 grandfather 推导：遍历**全部**厂商（`CloudModels.ALL.map{it.vendor}.distinct()`）查 `vendorApiKey(v)`，只要有一个非空 → 返回 `GRANDFATHER_PENDING`；全部为空 → 返回 `NOT_ASKED`。**有记录但反序列化失败** → fail-closed 直接返回 `CloudAiConsent(status=NOT_ASKED)`，**不**走 grandfather 推导（v2b 修正，见 §10 v2 挑战第9项） | 只查 `selectedModel().vendor` 一个厂商（会漏判"配过 A 厂商 Key、当前选中模型是没配 Key 的 B 厂商"）；把"解析失败"也当"确无记录"处理（会让一条损坏的 `DECLINED` 记录自动"升级"成已同意，等同于用 Key 非空隐式推断同意） | T-L1-01a（全部为空→`NOT_ASKED`）、T-L1-01b（当前选中厂商为空但另一厂商非空→仍判 `GRANDFATHER_PENDING`）、T-L1-01c（预写一条无法反序列化的脏字符串→`NOT_ASKED`，即使此时某厂商 Key 非空也不得判 grandfather） |
| INV-L1-02 | `SwitchableAiRuntime.complete(request)` 被调用 | 若 `config.activeType()==CLOUD` 且 `!config.cloudAiConsentGranted()`（即 `cloudAiConsent().status` 不在 `{GRANTED, GRANDFATHER_PENDING}`）→ 直接返回 `Result.failure(CloudAiConsentRequiredException())`，**不**调用 `runtimes[CLOUD]`；否则按原逻辑路由 | 在此分支之外的任何地方（UI 层、`AiMealInputViewModel`、`AiRecommendViewModel` 等）另行实现一套同意校验（单一闸门，见 GC-13） | T-L1-02a（`DECLINED`+非空Key → `complete()` 直接 failure，`runtimes[CLOUD].complete` 断言零调用）、T-L1-02b（`GRANTED` → 正常路由）、T-L1-02c（`GRANDFATHER_PENDING` → 正常路由）、T-L1-02d（`NOT_ASKED` → failure） |
| INV-L1-03 | `AiMealInputViewModel`（含其 `confirmHealthAdvice()`）/`AiRecommendViewModel`/`AiPlanViewModel` 等既有消费点在同意未满足时发起了一次云端调用 | 分两类降级形态，**本批均不修改这些消费点任何一行代码**：① 有本地兜底的路径（`AiMealInputViewModel.submit()`、`AiRecommendViewModel`/`AiPlanViewModel` 的推荐生成）→ 沿既有失败处理路径（`LlmStreamEvent.Failed`→`attemptRuleFallback()`，或 `RecommendationOrchestrator` 的 `raw==null`→`RULE_FALLBACK`）自动降级为规则/本地结果；② **无本地兜底的路径**（`AiMealInputViewModel.confirmHealthAdvice()`，见 §10 v2 挑战第5项——该函数失败时只是把 `exception.message` 原样放进 `state.healthAdviceError`，不生成任何替代内容）→ 用户看到"建议暂时不可用：还没有同意把数据发给云端 AI"这类既有 `healthAdviceError` 展示区展示本批新异常的 message，**这是本批唯一新增的"用户会看到闸门本身文案"的路径**，其余路径用户看到的都是既有"规则解析/离线规则"文案而非闸门文案本身 | 把 `confirmHealthAdvice()` 失败误当作"已生成规则版建议"描述（该功能本无规则版）；`CloudAiConsentRequiredException` 的 message 里出现任何暗示"已经有替代结果"的措辞（v2b 已把 message 改为中性的"还没有同意把数据发给云端 AI"，不预设任何路径的具体降级结果） | T-L1-03a（① 类：集成测试，构造 `activeType=CLOUD`+非空Key+`DECLINED`，走一遍 `AiMealInputViewModel` 真实 `submit()`→segment 失败→`attemptRuleFallback()` 触发，断言最终 `parseSourceMessage` 精确等于"本次结果：规则解析（AI 解析失败：还没有同意把数据发给云端 AI）"这一完整字面量，而非仅"含'规则解析'字样"——避免掩盖文案重复/病句类缺陷，见 §10 v2 挑战第6项）、T-L1-03b（② 类：同样构造场景走 `confirmHealthAdvice()`，断言 `state.healthAdviceError == "还没有同意把数据发给云端 AI"` 且 `state.healthAdvice == null`） |
| INV-L1-04 | 用户在（已存在的）`KeyDialog` 输入非空 Key 点击"保存" | `AiSettingsScreen` 用 `routeOnSave(consent, vendor, key)` 纯函数求值：① `DIRECT`（`consent.status==GRANTED && scopeVersion>=CURRENT && vendor∈acknowledgedVendors`）→ 关闭 `KeyDialog`，直接调用 `vm.onSaveVendorKey`；② `VENDOR_CONFIRM`（同意已满足但 vendor 未确认）→ 关闭 `KeyDialog`，Key 暂存进宿主 `pendingKeyDraft`，打开轻量换厂商确认 `AlertDialog`；③ `FULL_CONSENT`（其余情况）→ 关闭 `KeyDialog`，Key 暂存进 `pendingKeyDraft`，打开完整同意面板 `CloudAiConsentPanel` | 把 Key 保存动作放在同意判定之前；把分流逻辑写进 Composable 内部而不抽纯函数（导致无法脱离 Compose 环境单测，见 §10 C-19） | T-L1-04a/b/c（对应 `routeOnSave` 三个分支的纯函数单测） |
| INV-L1-05 | 用户在**空** Key 状态点击"保存"（清空已有 Key） | `routeOnSave` 恒返回 `DIRECT`，不经过任何同意判定，直接调用 `vm.onSaveVendorKey(vendor, "")` | 对清空 Key 这一动作也拦同意确认 | T-L1-05 |
| INV-L1-06 | 用户在完整同意面板点击"启用云端 AI" | `AiSettingsViewModel.grantConsent(vendor, key, EXPLICIT_FIRST_ENABLE)`：先扫描**此刻全部已配置 Key 的厂商**（`CloudModels.ALL.map{it.vendor}.distinct().filter{config.vendorApiKey(it).isNotBlank()}`，**注意此时 `vendor` 自己的 Key 可能还没落库**，故须显式并入）得到 `existingVendors`，构造一份完整 `next: CloudAiConsent`（`status=GRANTED`, `source`, `grantedAtEpochSeconds=now`, `scopeVersion=CURRENT`, `acknowledgedVendors=existingVendors+vendor`）→ **先** `config.setCloudAiConsent(next)` **后** `onSaveVendorKey(vendor, key)`（顺序见下方"禁止结果"）→ 同一个 `next` 对象镜像进 `state.cloudAiConsent`（不得只镜像部分字段）→ 宿主关闭面板，用宿主级 `pendingSnackbar` 状态（非弹层内直接调用，见 §4.4/§10 v2 挑战第10项）排队一条反馈 | 颠倒写入顺序；`next` 对象只写部分字段导致内存态与持久化态不一致；`acknowledgedVendors` 只加当前这一个 `vendor`（会让"首次同意时另一厂商已有 grandfather 遗留 Key"这一情形在用户切换模型下拉后绕开确认直接可用——同意面板从未提示过那个厂商的存在，见 §10 v2 挑战第8项）；在面板 Composable 内部直接调用 `LocalAppSnackbar` | T-L1-06a（写入顺序）、T-L1-06b（`acknowledgedVendors` 含"扫描到的全部已配置厂商 + 当前vendor"，非仅当前vendor）、T-L1-06c（Snackbar 延迟到面板关闭后触发，非弹层内联——降级为真机项 E-L1-04，见 §10 v2 挑战第19项，非单测覆盖） |
| INV-L1-07 | 用户在完整同意面板点击"暂不启用"，**或**在设置页常驻状态块点击"关闭"选"保留密钥" | `declineConsent()`/`closeCloudAi(vendor, deleteKey=false)`：`consent.copy(status=DECLINED)` 持久化+镜像；`activeType` 若为 `CLOUD` 同步置 `MOCK`；Key **不**被保存（面板路径）/**不**被清空（关闭路径） | `activeType` 停留在 `CLOUD` 且界面不提供任何恢复入口（见 INV-L1-12 补齐） | T-L1-07a（面板拒绝）、T-L1-07b（设置页关闭·保留密钥） |
| INV-L1-08 | `DECLINED` 状态下，用户再次点"保存"输入非空 Key | `routeOnSave` 与 `NOT_ASKED` 走相同分支（`FULL_CONSENT`），不因"之前拒绝过"而特殊处理 | 出现任何"你之前拒绝过"的阻断文案 | T-L1-08 |
| INV-L1-12（v2b 新增，回应 §10 v2 挑战第7项） | `state.type==CLOUD && state.cloudAiConsent.status==DECLINED`（用户拒绝/关闭后又手动把单选切回"云端大模型"，`onTypeChange` 本身无校验、本批不改它） | `CloudSection` 渲染一行"云端 AI 已被你关闭 · 重新启用 ›"，点击直接 `consentPanelOpen=true`（复用完整同意面板，走与首次同意相同的 `grantConsent`/`declineConsent` 路径，不新增第三种同意函数） | 该状态下用户唯一的恢复路径是"打开某个厂商的 KeyDialog 重新按一次保存"（不可发现，见挑战原文）；`CloudSection` 因 `status!=GRANTED` 完全不渲染任何相关提示（用户会以为"选了云端却什么都没有"，找不到出路） | T-L1-12，真机 E-L1-03 |
| INV-L1-09 | 用户进入 AI 设置页（组合首次进入且 `state.loaded` 变为 `true` 之后） | 若 `config.cloudAiConsent().status == GRANDFATHER_PENDING` → 弹出差异化措辞的补确认面板；"继续使用" → `resolveGrandfather(confirm=true)`：扫描全部厂商中 Key 非空的集合作为 `acknowledgedVendors`，写入 `GRANTED(source=GRANDFATHER_CONFIRMED)`；"关闭云端 AI" → `resolveGrandfather(confirm=false)`：写入 `DECLINED`+`activeType=MOCK`。**`resolveGrandfather` 不接收 `key` 参数**（grandfather 面板不是填 Key 面板，此前 v1 遗漏这一点，见 §10 C-16） | 在启动/其他页面主动弹出该确认；`resolveGrandfather` 内部尝试调用 `onSaveVendorKey` 或以任何方式改动已存 Key 内容 | T-L1-09，真机 E-L1-01 |
| INV-L1-10 | AI 设置页 `CloudSection` 渲染 | 常驻状态块渲染条件 = `shouldShowCloudStatusBlock(consent, keyByVendor, vendor)`（**抽成 `CloudAiSaveRoute.kt` 同文件内的纯函数**，v2b 修正——原内联写在 Composable 里无法被 JVM 单测覆盖，见 §10 v2 挑战第19项；函数体 = `consent.status == GRANTED && keyByVendor[vendor].orEmpty().isNotBlank()`，双条件而非仅 `status==GRANTED`，否则用户清空 Key 后仍会看到"已启用/接收方/同意时间"与红色"密钥：未配置"同屏矛盾） | 仅按 `status==GRANTED` 单条件渲染；把该判断内联写在 `CloudSection` Composable 里而不抽纯函数 | T-L1-10（纯函数 JVM 单测：`GRANTED`+空Key → false），真机 E-L1-02 |
| INV-L1-11 | `LaunchedEffect` 用于判断是否弹出 grandfather 补确认面板 | 以 `state.loaded` 作为 `key`（`LaunchedEffect(state.loaded)`），block 内 `if (!state.loaded) return@LaunchedEffect` 再判 consent 状态；**且额外要求 `!keyDialogOpen && !consentPanelOpen && !vendorConfirmOpen`**（v2b 新增，见 §10 v2 挑战第12项——冷启动首屏 `loaded` 从 false 翻 true 之前，用户完全可能已经手动点开了 `KeyDialog` 或其他弹层，若不加此守卫会与 grandfather 面板同屏堆叠）；`CloudAiConsentPanel` 的 `onClose` 参数（返回键/点外部）必须显式绑定为 `{ grandfatherPanelOpen = false }`（不写任何 consent 状态，等价于"这次先不处理，下次进页再问"） | 用 `LaunchedEffect(Unit)` 配合"等 `loaded==true`"的散文描述（`Unit` 恒定，`reload()` 的异步结果无法触发 block 重新执行，面板永不弹出）；grandfather 面板与 `keyDialogOpen`/`consentPanelOpen`/`vendorConfirmOpen` 任一同屏堆叠；`onClose` 缺失导致返回键无响应或误写 consent 状态 | T-L1-11（文档化断言：`reload()` 完成后 `LaunchedEffect` 确实重新求值，真机 E-L1-01 实测），真机 E-L1-05（冷启动首屏快速点开 KeyDialog 不应与 grandfather 面板同屏） |

---

## §4 接口契约

### 4.1 逐字段真相源表

| 字段 | 唯一写入者 | 读取方 | 终局形态 |
|---|---|---|---|
| `CloudAiConsent`（偏好 JSON，key=`PreferenceKeys.CLOUD_AI_CONSENT`） | `AiRuntimeConfig.setCloudAiConsent()`，调用方仅 `AiSettingsViewModel` 的 **5 个**动作函数：`grantConsent`/`declineConsent`/`confirmVendorSwitch`/`closeCloudAi`/`resolveGrandfather`（v1 §4.1 与 §4.4 函数清单不一致，v2 统一为这 5 个，见 §10 C-16） | `SwitchableAiRuntime.complete()`（唯一的真实闸门判据）、`AiSettingsScreen`（常驻状态块渲染、`routeOnSave` 输入） | 新增字段 |
| `KeyDialog`（组件） | — | — | **保留不变**（v1 计划替换为双页组件，v2 撤销，见 §10 C-11） |

### 4.2 GC-11：字段/组件迁移清单

**v1 计划的两处迁移（`AiSettingsUiState.type` 默认值、`KeyDialog`→双页组件）均已撤销，本版无字段迁移，仅新增字段（见 §4.1）。**

**唯一保留的迁移（政策文案，非代码字段）：隐私政策变更是否需要老用户重新首启同意** —— 判定不需要，理由不变（本次是补充披露非扩大采集范围，采集入口已有独立强同意兜底）。处理方式：`POLICY_UPDATED` 更新为 `"最近更新：2026年8月"`（GC-25 目标字面量），不触发 `PrivacyConsentDialog` 重新弹出。

### 4.3 GC-12：UI 判据与业务判据同源表

| UI 表现 | 判据来源 |
|---|---|
| AI 设置页常驻状态块是否渲染 | `state.cloudAiConsent.status == GRANTED && state.keyByVendor[vendor].orEmpty().isNotBlank()`（双条件，见 INV-L1-10） |
| `KeyDialog` 保存后走哪条分流路径 | `routeOnSave(state.cloudAiConsent, vendor, key)` 纯函数，唯一实现，UI 层与测试代码调用同一份逻辑 |
| AI 推荐/AI 记一餐"是否显示云端结果"的判据 | 沿用各自既有的 `isModelReady()`/`configReady`/`activeType()` 判据链路，**零改动**——本批不再声称"因为改了 `isModelReady()` 所以自动继承"（v1 的错误论证），而是"因为真实网络出口 `SwitchableAiRuntime.complete()` 本身会在未同意时返回失败，各消费点原有的失败兜底路径天然覆盖"，这是两条不同的因果链，v2 用 INV-L1-03 的集成测试实证这条链路，不是断言 |

### 4.4 新增/变更类型与函数签名

```kotlin
// shared/.../ai/CloudAiConsent.kt（新文件）
package com.sxdbsm.cookbook.ai

import kotlinx.serialization.Serializable

/** 云端 AI 数据外发同意状态。见蓝图 §3 GC-36 值域论证。[AI生成] */
@Serializable
enum class ConsentStatus { NOT_ASKED, GRANTED, DECLINED, GRANDFATHER_PENDING }

/** 同意来源，仅供审计/展示区分，不参与任何行为判据。[AI生成] */
@Serializable
enum class ConsentSource { EXPLICIT_FIRST_ENABLE, GRANDFATHER_CONFIRMED }

/**
 * 云端 AI 同意状态（偏好 JSON 存储，免迁移）。[AI生成]
 *
 * 默认值即 [ConsentStatus.NOT_ASKED] 语义——不得在任何路径把默认值改写成"已同意"或
 * 用"Key 非空"隐式推断同意（禁暗黑模式红线）。
 */
@Serializable
data class CloudAiConsent(
    val status: ConsentStatus = ConsentStatus.NOT_ASKED,
    val source: ConsentSource? = null,
    val grantedAtEpochSeconds: Long? = null, // [AI修改] v2：原 grantedAtEpochMs 是秒×1000 的假毫秒，改诚实的秒字段
    val scopeVersion: Int = 0,
    val acknowledgedVendors: Set<String> = emptySet(),
)

/** [AI生成] v2：SwitchableAiRuntime 用于表达"云端已选中但同意未满足"的内部信号；message 是可直接展示给用户的人话，
 *  不含任何内部代号（守"内部哨兵文案人性化"红线）——各消费点既有失败处理路径会把它当普通网络失败处理，天然不需要
 *  特殊识别这个异常类型，本类型存在只是为了让测试能精确断言"失败原因是同意未满足"而非猜测字符串。 */
class CloudAiConsentRequiredException :
    Exception("还没有同意把数据发给云端 AI") // [AI修改] v2b：不预设"已使用规则推荐"这一结果——confirmHealthAdvice() 这个消费点
    // 没有规则兜底，看到这条消息时并不会真的生成什么规则版结果（见 §10 v2 挑战第5项），message 措辞必须对所有消费点都成立
```

```kotlin
// shared/.../ai/AiRuntimeConfig.kt（新增方法；isModelReady() 不变）
class AiRuntimeConfig(private val prefs: PreferenceRepository) {
    // ... 既有内容完全不变（含 isModelReady()，v2 撤销 v1 对它的修改）...

    private val consentJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /**
     * 读取云端 AI 同意状态；**只有"确无记录"**（`prefs.get(...)==null`）才扫描全部厂商推导 grandfather 态（INV-L1-01）。[AI修改] v2b：
     * "记录存在但解码失败"必须 fail-closed 返回 `NOT_ASKED`，不得落入 grandfather 推导——否则一条被损坏的 `DECLINED` 记录会
     * 自动"升级"成已同意（§10 v2 挑战第9项），且这本质上正是蓝图自己禁止的"用 Key 非空隐式推断同意"。
     */
    suspend fun cloudAiConsent(): CloudAiConsent {
        val raw = prefs.get(KEY_CLOUD_AI_CONSENT)
        if (raw != null) {
            return runCatching { consentJson.decodeFromString<CloudAiConsent>(raw) }
                .getOrDefault(CloudAiConsent(status = ConsentStatus.NOT_ASKED)) // fail-closed，不做二次推导
        }
        val anyVendorKeyPresent = CloudModels.ALL.map { it.vendor }.distinct().any { vendorApiKey(it).isNotBlank() }
        return if (anyVendorKeyPresent) CloudAiConsent(status = ConsentStatus.GRANDFATHER_PENDING) else CloudAiConsent()
    }

    /** 写入云端 AI 同意状态。[AI生成] */
    suspend fun setCloudAiConsent(consent: CloudAiConsent) =
        prefs.set(KEY_CLOUD_AI_CONSENT, consentJson.encodeToString(consent))

    /** 同意是否已满足（供运行时闸门与 UI 共用同一判据）。[AI生成] */
    suspend fun cloudAiConsentGranted(): Boolean =
        cloudAiConsent().status.let { it == ConsentStatus.GRANTED || it == ConsentStatus.GRANDFATHER_PENDING }

    companion object {
        // ... 既有 KEY_* 不变 ...
        const val KEY_CLOUD_AI_CONSENT = "cloud_ai_consent"
    }
}

/** [AI修改] v2：唯一的真实联网闸门——CLOUD 已选中但同意未满足时直接短路，不路由到 CloudAiRuntime（INV-L1-02）。 */
class SwitchableAiRuntime(
    private val config: AiRuntimeConfig,
    private val runtimes: Map<AiRuntimeType, AiRuntime>,
) : AiRuntime {
    override suspend fun complete(request: LlmRequest): Result<String> {
        val type = config.activeType()
        if (type == AiRuntimeType.CLOUD && !config.cloudAiConsentGranted()) {
            return Result.failure(CloudAiConsentRequiredException())
        }
        val runtime = runtimes[type]
            ?: runtimes[AiRuntimeType.MOCK]
            ?: return Result.failure(IllegalStateException("no AiRuntime registered for $type"))
        return runtime.complete(request)
    }
    // stream() 不重写：AiRuntime 接口默认实现已把 complete() 的 Result.failure 转成 LlmStreamEvent.Failed（AiRuntime.kt:42-53），
    // 本类本就未重写 stream()（K1a GC-37 挑战 #14 已记录此既有事实），本次改动零新增行为分歧。
}
```

**[AI修改] v2b：撤销 `PreferenceKeys.CLOUD_AI_CONSENT` 常量**——`PreferenceKeys` 里现有的 AI 运行时相关 key（`activeType`/`selectedModelId`/vendor key 等）全部只私有于 `AiRuntimeConfig.companion`，从未在 `PreferenceKeys` 里重复登记过（`Preference.kt` 现状完全不含任何 AI 相关常量）；本批新增的 `KEY_CLOUD_AI_CONSENT` 沿用这一既有惯例、只留在 `AiRuntimeConfig` 一处，不在 `PreferenceKeys` 再放一份同值常量（避免 GC-10"单一真相源"被破坏，见 §10 v2 挑战第16项）。

```kotlin
// androidApp/.../ui/ai/CloudAiDisclosure.kt（新文件，不变）
package com.sxdbsm.cookbook.android.ui.ai

object CloudAiDisclosure {
    const val SCOPE_VERSION = 1
    val WILL_SEND: List<String> = listOf(
        "在手食材名（比如"西红柿、鸡蛋"）", "粗略的健康标签（比如"忌高嘌呤"）", "候选菜名", "你在 AI 记一餐里输入的那句话",
    )
    val WONT_SEND: List<String> = listOf(
        "姓名、账号", "体检数值、病历、用药记录", "照片", "完整健康档案、历史餐食",
    )
    val INCREMENT_NOTES: Map<Int, String> = emptyMap()
}
```

```kotlin
// androidApp/.../ui/ai/CloudAiSaveRoute.kt（新文件，纯函数，非 Compose，可直接 JVM 单测——解决 v1 C-19 缺陷）
package com.sxdbsm.cookbook.android.ui.ai

import com.sxdbsm.cookbook.ai.CloudAiConsent
import com.sxdbsm.cookbook.ai.ConsentStatus

enum class SaveRoute { DIRECT, VENDOR_CONFIRM, FULL_CONSENT }

/** 保存 Key 时应走哪条路径（INV-L1-04/05）。[AI生成] */
fun routeOnSave(consent: CloudAiConsent, vendor: String, key: String): SaveRoute {
    if (key.isBlank()) return SaveRoute.DIRECT
    val satisfied = consent.status == ConsentStatus.GRANTED && consent.scopeVersion >= CloudAiDisclosure.SCOPE_VERSION
    if (!satisfied) return SaveRoute.FULL_CONSENT
    return if (vendor in consent.acknowledgedVendors) SaveRoute.DIRECT else SaveRoute.VENDOR_CONFIRM
}

/** [AI生成] v2b：AI 设置页常驻状态块是否渲染（INV-L1-10），抽纯函数供 JVM 单测覆盖（§10 v2 挑战第19项）。 */
fun shouldShowCloudStatusBlock(consent: CloudAiConsent, keyByVendor: Map<String, String>, vendor: String): Boolean =
    consent.status == ConsentStatus.GRANTED && keyByVendor[vendor].orEmpty().isNotBlank()
```

```kotlin
// androidApp/.../ui/ai/CloudAiConsentPanel.kt（新文件，独立弹层，不再是"某对话框内部页1"——见 §10 C-11 处置）
package com.sxdbsm.cookbook.android.ui.ai

/**
 * 完整同意面板（含只读态，能力显隐由回调是否传入决定，禁 mode 布尔——项目红线）。[AI生成]
 *
 * 独立 Dialog，由宿主 AiSettingsScreen 按需打开/关闭；grandfather=true 时首句改"这项功能你已在使用"口径。
 */
@Composable
fun CloudAiConsentPanel(
    vendorName: String,
    grandfather: Boolean = false,
    onAgree: (() -> Unit)? = null,   // 非空 → 双按钮态；null → 只读态仅显"知道了"
    onDecline: (() -> Unit)? = null,
    onClose: () -> Unit,             // 返回键/点外部/只读态"知道了"统一走这个，不写任何 consent 状态
)
```

```kotlin
// androidApp/.../ui/ai/AiSettingsViewModel.kt（新增字段与方法）
class AiSettingsViewModel(private val config: AiRuntimeConfig) : ViewModel() {
    // ... 既有 reload/onTypeChange/onSelectModel/onSaveVendorKey/selectedModel 完全不变 ...

    private fun reload() {
        viewModelScope.launch {
            // ... 既有赋值不变 ...
            state = state.copy(/* ...既有字段..., */ cloudAiConsent = config.cloudAiConsent(), loaded = true)
        }
    }

    /**
     * [AI修改] v2b：同意后的完整保存流程——先写 consent 再写 Key（INV-L1-06），next 一次构造两处复用（GC-29）。
     * `acknowledgedVendors` 扫描**此刻全部已配置 Key 的厂商**（非仅当前 vendor）——否则用户此前（grandfather 时期）
     * 已配置过的另一厂商 Key，会在本次只针对 vendor A 同意之后，被用户切换模型下拉悄悄启用而未经任何确认
     * （§10 v2 挑战第8项）。`vendor` 自身此刻可能还没落库（`onSaveVendorKey` 在这之后才调），须显式并入。
     */
    fun grantConsent(vendor: String, key: String, source: ConsentSource) {
        viewModelScope.launch {
            val existingVendors = CloudModels.ALL.map { it.vendor }.distinct()
                .filter { config.vendorApiKey(it).isNotBlank() }.toSet()
            val next = state.cloudAiConsent.copy(
                status = ConsentStatus.GRANTED, source = source,
                grantedAtEpochSeconds = com.sxdbsm.cookbook.util.DateTime.nowEpochSeconds(),
                scopeVersion = CloudAiDisclosure.SCOPE_VERSION,
                acknowledgedVendors = existingVendors + vendor,
            )
            config.setCloudAiConsent(next)
            state = state.copy(cloudAiConsent = next)
            onSaveVendorKey(vendor, key)
        }
    }

    /** [AI生成] 拒绝/关闭——Key 不动，consent 置 DECLINED，activeType 回退 MOCK（INV-L1-07）。 */
    fun declineConsent() {
        viewModelScope.launch {
            val next = state.cloudAiConsent.copy(status = ConsentStatus.DECLINED)
            config.setCloudAiConsent(next)
            if (state.type == AiRuntimeType.CLOUD) onTypeChange(AiRuntimeType.MOCK)
            state = state.copy(cloudAiConsent = next)
        }
    }

    /** [AI生成] 换厂商轻量确认——仅追加 vendor 到已确认集合。 */
    fun confirmVendorSwitch(vendor: String, key: String) {
        viewModelScope.launch {
            val next = state.cloudAiConsent.copy(acknowledgedVendors = state.cloudAiConsent.acknowledgedVendors + vendor)
            config.setCloudAiConsent(next)
            state = state.copy(cloudAiConsent = next)
            onSaveVendorKey(vendor, key)
        }
    }

    /** [AI生成] 设置页常驻状态块"关闭"入口；deleteKey=true 时一并清空当前厂商 Key。 */
    fun closeCloudAi(vendor: String, deleteKey: Boolean) {
        viewModelScope.launch {
            val next = state.cloudAiConsent.copy(status = ConsentStatus.DECLINED)
            config.setCloudAiConsent(next)
            onTypeChange(AiRuntimeType.MOCK)
            if (deleteKey) onSaveVendorKey(vendor, "")
            state = state.copy(cloudAiConsent = next)
        }
    }

    /** [AI生成] grandfather 补确认专用——不接收/不改动 Key（INV-L1-09），acknowledgedVendors 取全部已配置厂商。 */
    fun resolveGrandfather(confirm: Boolean) {
        viewModelScope.launch {
            if (confirm) {
                val vendors = CloudModels.ALL.map { it.vendor }.distinct()
                    .filter { config.vendorApiKey(it).isNotBlank() }.toSet()
                val next = CloudAiConsent(
                    status = ConsentStatus.GRANTED, source = ConsentSource.GRANDFATHER_CONFIRMED,
                    grantedAtEpochSeconds = com.sxdbsm.cookbook.util.DateTime.nowEpochSeconds(),
                    scopeVersion = CloudAiDisclosure.SCOPE_VERSION, acknowledgedVendors = vendors,
                )
                config.setCloudAiConsent(next)
                state = state.copy(cloudAiConsent = next)
            } else {
                val next = CloudAiConsent(status = ConsentStatus.DECLINED)
                config.setCloudAiConsent(next)
                onTypeChange(AiRuntimeType.MOCK)
                state = state.copy(cloudAiConsent = next)
            }
        }
    }
}

data class AiSettingsUiState(
    val type: AiRuntimeType = AiRuntimeType.CLOUD, // [AI修改] v2：撤销 v1 的"顺手修"，保持原值不动（见 §10 C-04）
    val models: List<CloudModel> = CloudModels.ALL,
    val selectedModelId: String = CloudModels.DEFAULT.id,
    val keyByVendor: Map<String, String> = emptyMap(),
    val loaded: Boolean = false,
    val cloudAiConsent: com.sxdbsm.cookbook.ai.CloudAiConsent = com.sxdbsm.cookbook.ai.CloudAiConsent(),
)
```

```kotlin
// androidApp/.../ui/ai/AiSettingsScreen.kt（宿主直接管理多个独立弹层，KeyDialog 保持既有 AlertDialog 形态不变）
@Composable
fun AiSettingsScreen(onBack: () -> Unit, vm: AiSettingsViewModel = koinViewModel()) {
    val state = vm.state
    var keyDialogOpen by remember { mutableStateOf(false) }
    var guideOpen by remember { mutableStateOf(false) } // 既有不变
    // [AI修改] v2b：改用 remember（非 rememberSaveable）——API Key 草稿不应落进 SavedStateRegistry/Bundle 持久化存储，
    //   否则进程被杀后系统可能把明文密钥暂存到磁盘（§10 v2 挑战第20项）；代价是配置变更/进程重建会丢草稿，可接受。
    var pendingKeyDraft by remember { mutableStateOf("") }
    var vendorConfirmOpen by remember { mutableStateOf(false) }
    var consentPanelOpen by remember { mutableStateOf(false) }
    var grandfatherPanelOpen by remember { mutableStateOf(false) }
    var readonlyPanelOpen by remember { mutableStateOf(false) }
    var closeSheetOpen by remember { mutableStateOf(false) }
    var pendingSnackbar by remember { mutableStateOf<String?>(null) }
    // [AI修改] v2b：hoist 在 Composable 作用域读取（CompositionLocal.current 是 @Composable getter，
    //   不能在 LaunchedEffect 的 suspend block 里直接读——§10 v2 挑战第10项，原写法编译不过）；
    //   照抄既有范例 AddDayFoodScreen.kt:189,213 的用法，类型可空（AppSnackbar.kt:51）。
    val snackbar = com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar.current

    // [AI修改] v2b：额外加三个弹层守卫，避免冷启动首屏 loaded 从 false 翻 true 之前用户已手动打开其他弹层时同屏堆叠
    //   （§10 v2 挑战第12项）。
    LaunchedEffect(state.loaded) {
        if (state.loaded && state.cloudAiConsent.status == ConsentStatus.GRANDFATHER_PENDING &&
            !keyDialogOpen && !consentPanelOpen && !vendorConfirmOpen
        ) grandfatherPanelOpen = true
    }
    pendingSnackbar?.let { msg ->
        LaunchedEffect(msg) { snackbar?.showMessage(msg); pendingSnackbar = null }
    }
    // ... 其余渲染见 §7 STEP-L1-4.x 逐条落点，不在此穷举 Compose 树 ...
}
```

### 4.5 对象生命周期表（GC-14~16）

| 对象 | 创建者 | 持有者 | 可调用者 | 释放点 |
|---|---|---|---|---|
| `pendingKeyDraft`（`rememberSaveable`） | `AiSettingsScreen` | `AiSettingsScreen`（唯一持有者，值传参给各弹层的初始值，不做跨 Composable 可变引用传递——GC-15） | `AiSettingsScreen` 内各回调 | 三条分流路径任一走完（保存成功/取消/拒绝）后清空为 `""` |
| `keyDialogOpen`/`vendorConfirmOpen`/`consentPanelOpen`/`grandfatherPanelOpen`/`readonlyPanelOpen`/`closeSheetOpen`（6 个互斥或半互斥的 `remember` 布尔） | `AiSettingsScreen` | 同上 | 同上 | 各自对应弹层关闭时置 false；**互斥关系**：`keyDialogOpen` 关闭是打开其余任一弹层的前置条件（同一时刻至多一个"保存流程"相关弹层可见），`readonlyPanelOpen` 与 `closeSheetOpen`/`grandfatherPanelOpen` 相互独立（用户从常驻状态块两个不同入口触发，不会同时打开） |

### 4.6 GC-13：fallback 复用主路径校验入口

`SwitchableAiRuntime.complete()` 的同意拦截直接复用 `AiRuntime` 接口既有的 `Result<String>` 契约与 `stream()` 默认实现的 `Result.failure → LlmStreamEvent.Failed` 转换（`AiRuntime.kt:42-53`，本批不改这段代码，只是让它多一种触发失败的原因）。所有消费点已有的"AI 失败→规则/本地兜底"路径就是这里说的"主路径校验入口"，不新造。

### 4.7 挂起点清单（GC-31）

| 挂起点 | 恢复后身份重校验 |
|---|---|
| `AiSettingsViewModel` 的 5 个动作函数内的 `config.setCloudAiConsent(...)`/`onSaveVendorKey(...)` | 无需 generation 隔离——单个 `viewModelScope.launch` 内顺序执行到底，无并发同类动作场景 |
| `SwitchableAiRuntime.complete()` 内 `config.cloudAiConsentGranted()` | 无需重校验——每次 `complete()` 调用独立求值当前状态，不存在"跨调用复用旧判定结果"的场景 |
| `AiSettingsScreen` 的 `LaunchedEffect(state.loaded)` | 见 INV-L1-11，`key` 用 `state.loaded` 而非 `Unit`，是本条挂起点的核心修正 |

---

## §5 UI 设计（转译 apple_ux_designer 设计输出，视觉规范部分与 v1 一致，仅弹层拆分方式随 §4 调整）

### 5.1 弹层拆分（v2 调整）

- `KeyDialog` **保持现状**（`AlertDialog`，填 Key + 说明 + 保存/取消），不做任何视觉改动。
- 完整同意面板 `CloudAiConsentPanel` 独立成一个 `Dialog`+自绘 `Surface`（28dp 圆角，`tonalElevation=6.dp`，`usePlatformDefaultWidth=false`+`fillMaxWidth().padding(horizontal=24.dp)`，沿用 `PrivacyConsentDialog.kt` 已验证范式），内容结构（标题/接收方句/会发送清单/不会发送清单/影响卡/控制说明/双按钮）与 v1 §5.1 完全一致，**只是不再需要内部"页0/页1切换"这一层**——它现在本身就是唯一的一页。
- 轻量换厂商确认：小 `AlertDialog`，与 v1 §5.2 一致；其"看看发送哪些内容 ›"下钻改为**关闭换厂商确认框、打开只读态 `CloudAiConsentPanel`**，只读面板"知道了"关闭后**不自动弹回**换厂商确认框（用户若仍要换厂商，需重新点一次"编辑"）——这是 v2 显式简化，消除 v1 未定义的"三层弹层返回路径"问题（见 §10 C-12）。
- 设置页常驻状态块、"关闭" `ActionSheet` 两档：与 v1 §5.3 一致。
- 反馈文案（"已启用云端 AI"等）一律通过宿主级 `pendingSnackbar` 状态在弹层关闭**之后**的下一次重组展示，不在任何弹层内部回调里直接调用 `LocalAppSnackbar`（见 §10 C-13、§4.4 `AiSettingsScreen` 片段）。

其余视觉细节（颜色基调、间距、按钮等宽等高垂直堆叠、无障碍）与 v1 §5 完全一致，不重复摘录。

---

## §6 文件改动清单 + Allowlist

| 文件 | 允许操作 | 禁止操作 |
|---|---|---|
| 新建 `shared/.../ai/CloudAiConsent.kt` | 新建（`ConsentStatus`/`ConsentSource`/`CloudAiConsent`/`CloudAiConsentRequiredException`） | — |
| `shared/.../ai/AiRuntimeConfig.kt` | 新增 `cloudAiConsent()`/`setCloudAiConsent()`/`cloudAiConsentGranted()`/`KEY_CLOUD_AI_CONSENT`；`SwitchableAiRuntime.complete()` 新增同意拦截分支 | **不得修改 `isModelReady()`**（v1 遗留禁令倒转——v2 明确它保持原样）；不得修改 `activeType`/`setActiveType`/`selectedModel`/`vendorApiKey`/`setVendorApiKey`/`currentCloudApiKey`；`SwitchableAiRuntime` 不得新增重写 `stream()`/`chat()`（两者均走 `AiRuntime` 默认实现自动转调 `complete()`，若被重写会绕开本批闸门） |
| 新建 `androidApp/.../ui/ai/CloudAiDisclosure.kt` | 新建 | — |
| 新建 `androidApp/.../ui/ai/CloudAiSaveRoute.kt` | 新建（`SaveRoute` 枚举 + `routeOnSave`/`shouldShowCloudStatusBlock` 纯函数） | — |
| 新建 `androidApp/.../ui/ai/CloudAiConsentPanel.kt` | 新建 | — |
| `androidApp/.../ui/ai/AiSettingsScreen.kt` | `KeyDialog` **保留不动**；新增 6 个弹层状态变量 + `pendingKeyDraft`（`remember`，非 `rememberSaveable`）/`pendingSnackbar`；`CloudSection` 内新增常驻状态块+"DECLINED 后重新启用"行（INV-L1-12）；新增关闭 `ActionSheet`；新增 `LaunchedEffect(state.loaded)`（含三弹层互斥守卫） | 改 `ApiKeyGuideSheet`/`OnDeviceSelfTestSection`/`RuntimeRow`/`maskKey`/`KeyDialog` 内部实现逻辑 |
| `androidApp/.../ui/ai/AiSettingsViewModel.kt` | 新增 `grantConsent`/`declineConsent`/`confirmVendorSwitch`/`closeCloudAi`/`resolveGrandfather` 五方法；`reload()` 新增一行读取；`AiSettingsUiState` 新增 `cloudAiConsent` 字段（**`type` 默认值不动**） | 改 `onTypeChange`/`onSelectModel`/`onSaveVendorKey`/`selectedModel` 既有函数体 |
| `androidApp/.../ui/component/CapsuleButton.kt` | 新增 `CapsuleOutlineButton`（含 `OutlinedButton`/`BorderStroke` 完整 import） | 改既有 `CapsuleButton` |
| `androidApp/.../ui/policy/PolicyContent.kt` | 隐私政策 §一末尾加限定句；在现 §三"设备权限与用途"之后、现 §四"第三方 SDK"之前新增一个独立小节成为新 §四，原 §四~§八顺延为 §五~§九（**允许改既有小节标题的中文序号字符**——本条明确豁免于"禁止改其余既有小节文字"，仅限序号顺延这一项操作）；`POLICY_UPDATED` 更新为 `"最近更新：2026年8月"` | 改隐私政策/用户协议任何小节的实质正文内容（序号顺延本身不算实质内容变更） |
| `.ai-context/docs/feature/隐私政策与用户协议.md` | 同步 `PolicyContent.kt` 的本批变更（新增小节+序号顺延），保持两处文档一致 | — |
| 新建 `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/ai/AiRuntimeConfigConsentTest.kt` | 新增 T-L1-01a/b/c、T-L1-02a/b/c/d（含 `SwitchableAiRuntime` 集成断言） | — |
| 新建 `androidApp/.../ui/ai/CloudAiSaveRouteTest.kt` | 新增 T-L1-04a/b/c、T-L1-05、T-L1-08、T-L1-10（纯函数单测，JVM，含 `shouldShowCloudStatusBlock`） | — |
| 新建 `androidApp/.../ui/ai/AiSettingsViewModelConsentTest.kt` | 新增 T-L1-06a/b、T-L1-07a/b、T-L1-09、T-L1-12 | — |
| 新建/追加 `androidApp/.../ui/ai/AiMealInputViewModelStreamTest.kt` 的姊妹集成测试文件（**不得修改该既有文件本身**） | 新增独立文件覆盖 T-L1-03a/b | 不修改 `AiMealInputViewModelStreamTest.kt` 任何一行 |

**显式禁改文件清单**：
- `shared/.../ai/meallog/*`（AI 记一餐主线）
- `androidApp/.../ui/ai/AiMealInputViewModel.kt`/`AiMealInputSheet.kt`（K1a/CFG 已冻结区；本批 v2 不再需要触碰，因为闸门已下沉到运行时层，见 §1.3 In Scope #2/#3）
- `androidApp/.../ui/ai/AiRecommendViewModel.kt`/`AiPlanViewModel.kt`（零改动，运行时闸门透传覆盖）
- `androidApp/.../ui/onboarding/PrivacyConsentDialog.kt`

**回归基线锁定（GC-09）**：
- `:shared:testDebugUnitTest`（全量，0 failures）
- `:androidApp:testDebugUnitTest`（全量，含既有 `AiMealInputViewModelStreamTest`/`GenerationProgressTest` 零改动仍全绿）
- `:androidApp:assembleDebug`

---

## §7 分阶段实施步骤

### 批 L1-1：同意状态模型 + 运行时闸门

**STEP-L1-1.1**：新建 `shared/.../ai/CloudAiConsent.kt`，按 §4.4 完整实现。
完成形态：`grep "enum class ConsentStatus" CloudAiConsent.kt` 命中 1 处；`grep "class CloudAiConsentRequiredException" CloudAiConsent.kt` 命中 1 处。

**STEP-L1-1.2**：`shared/.../ai/AiRuntimeConfig.kt` 按 §4.4 新增 `cloudAiConsent()`/`setCloudAiConsent()`/`cloudAiConsentGranted()`/`KEY_CLOUD_AI_CONSENT`。**不改 `isModelReady()` 任何一行**。
完成形态：`grep "suspend fun cloudAiConsentGranted" AiRuntimeConfig.kt` 命中 1 处；`git diff` 对 `isModelReady` 函数体零改动（人工核对）。

**STEP-L1-1.3**：`SwitchableAiRuntime.complete()` 按 §4.4 插入同意拦截分支（在 `val type = config.activeType()` 之后、`runtimes[type]` 查找之前）。
完成形态：`grep "CloudAiConsentRequiredException()" AiRuntimeConfig.kt` 命中 1 处（在 `SwitchableAiRuntime` 内）；`grep -B2 "runtimes\[type\]" AiRuntimeConfig.kt` 前两行应含 `cloudAiConsentGranted` 判断（人工核对分支顺序）。

### 批 L1-2：同意面板 + 分流纯函数

**STEP-L1-2.1**：新建 `androidApp/.../ui/ai/CloudAiDisclosure.kt`，按 §4.4 实现（占位文案，交 `copywriter` 定稿前不阻断本批其余 STEP）。
完成形态：`grep "const val SCOPE_VERSION = 1" CloudAiDisclosure.kt` 命中 1 处。

**STEP-L1-2.2**：新建 `androidApp/.../ui/ai/CloudAiSaveRoute.kt`，按 §4.4 实现 `SaveRoute`/`routeOnSave`/`shouldShowCloudStatusBlock`。
完成形态：`grep "fun routeOnSave" CloudAiSaveRoute.kt` 命中 1 处；`grep "fun shouldShowCloudStatusBlock" CloudAiSaveRoute.kt` 命中 1 处。

**STEP-L1-2.3**：新建 `androidApp/.../ui/ai/CloudAiConsentPanel.kt`，按 §4.4 签名 + §5.1 布局实现（双按钮态/只读态由 `onAgree` 是否为 null 决定，**禁止**提前 `return` 分支，必须 `if/else` 平衡——Compose SlotTable 崩溃红线）。
完成形态：`grep "onAgree: (() -> Unit)? = null" CloudAiConsentPanel.kt` 命中 1 处；`grep "return@Column\|return@Row\|return@Box" CloudAiConsentPanel.kt` 零命中。

**STEP-L1-2.4**：`androidApp/.../ui/component/CapsuleButton.kt` 追加 `CapsuleOutlineButton`，import 补全 `androidx.compose.material3.OutlinedButton`、`androidx.compose.foundation.BorderStroke`。
完成形态：`grep "fun CapsuleOutlineButton" CapsuleButton.kt` 命中 1 处；`grep "import androidx.compose.foundation.BorderStroke" CapsuleButton.kt` 命中 1 处。

### 批 L1-3：ViewModel 接入

**STEP-L1-3.1**：`AiSettingsViewModel.kt` 按 §4.4 新增 5 个方法；`reload()` 新增读取一行；`AiSettingsUiState` 新增 `cloudAiConsent` 字段（`type` 默认值**不动**）。
完成形态：`grep "fun grantConsent\|fun declineConsent\|fun confirmVendorSwitch\|fun closeCloudAi\|fun resolveGrandfather" AiSettingsViewModel.kt` 各命中 1 处（共 5 处）；`grep "val type: AiRuntimeType = AiRuntimeType.CLOUD" AiSettingsViewModel.kt` 命中 1 处（确认未被误改）。

### 批 L1-4：设置页接入（拆细，回应 §10 C-12）

**STEP-L1-4.1**：`AiSettingsScreen.kt` 顶部新增 6 个弹层布尔状态 + `pendingKeyDraft`/`pendingSnackbar`（按 §4.4 片段）。
完成形态：`grep "var pendingKeyDraft" AiSettingsScreen.kt` 命中 1 处；`grep "var vendorConfirmOpen\|var consentPanelOpen\|var grandfatherPanelOpen\|var readonlyPanelOpen\|var closeSheetOpen" AiSettingsScreen.kt` 各命中 1 处。

**STEP-L1-4.2**：`KeyDialog` 的 `onConfirm` 回调改为：先 `keyDialogOpen=false`，再调用 `routeOnSave(vm.state.cloudAiConsent, model.vendor, key)`，按 `SaveRoute` 三态分别执行——`DIRECT`→`vm.onSaveVendorKey(model.vendor, key)`；`VENDOR_CONFIRM`→`pendingKeyDraft=key; vendorConfirmOpen=true`；`FULL_CONSENT`→`pendingKeyDraft=key; consentPanelOpen=true`。
完成形态：`grep "routeOnSave(" AiSettingsScreen.kt` 命中 1 处；`grep "SaveRoute.DIRECT\|SaveRoute.VENDOR_CONFIRM\|SaveRoute.FULL_CONSENT" AiSettingsScreen.kt` 各命中 ≥1 处。

**STEP-L1-4.3**：新增 `vendorConfirmOpen` 分支渲染（轻量换厂商确认 `AlertDialog`，按 §5.1 规范；"取消"清空 `pendingKeyDraft`；"看看发送哪些内容"关闭本弹窗并打开 `readonlyPanelOpen=true`）。
完成形态：`grep "if (vendorConfirmOpen)" AiSettingsScreen.kt` 命中 1 处。

**STEP-L1-4.4**：新增 `consentPanelOpen` 分支渲染（`CloudAiConsentPanel`，`onAgree` 调 `vm.grantConsent(model.vendor, pendingKeyDraft, ConsentSource.EXPLICIT_FIRST_ENABLE)` 后清空 `pendingKeyDraft`+关闭+排队 `pendingSnackbar="已启用云端 AI"`；`onDecline` 调 `vm.declineConsent()` 后同样清空+关闭+排队反馈；`onClose`——即返回键/点外部——仅关闭+清空 `pendingKeyDraft`，**不**调用任何 VM 方法、**不**写任何 consent 状态）。
完成形态：`grep "if (consentPanelOpen)" AiSettingsScreen.kt` 命中 1 处；`grep "vm.grantConsent(model.vendor, pendingKeyDraft" AiSettingsScreen.kt` 命中 1 处。

**STEP-L1-4.5**：新增 `readonlyPanelOpen` 分支渲染（`CloudAiConsentPanel(onAgree=null, onDecline=null, onClose={readonlyPanelOpen=false})`，**不**在关闭后重开 `vendorConfirmOpen`——见 §5.1 显式简化）。
完成形态：`grep "if (readonlyPanelOpen)" AiSettingsScreen.kt` 命中 1 处。

**STEP-L1-4.6**：`CloudSection` 内按 §5.3 新增常驻状态块，渲染条件调用 `shouldShowCloudStatusBlock(state.cloudAiConsent, state.keyByVendor, model.vendor)`（INV-L1-10）；"发送哪些内容"点击 `readonlyPanelOpen=true`；"关闭"点击 `closeSheetOpen=true`；**另新增一行**（INV-L1-12）：当 `state.type==CLOUD && state.cloudAiConsent.status==ConsentStatus.DECLINED` 时渲染"云端 AI 已被你关闭 · 重新启用 ›"，点击 `consentPanelOpen=true`（复用同一个完整同意面板，`onAgree`/`onDecline` 回调与 STEP-L1-4.4 完全一致）。
完成形态：`grep "云端 AI 已启用" AiSettingsScreen.kt` 命中 1 处；`grep "shouldShowCloudStatusBlock(" AiSettingsScreen.kt` 命中 1 处；`grep "云端 AI 已被你关闭" AiSettingsScreen.kt` 命中 1 处。

**STEP-L1-4.7**：新增 `closeSheetOpen` 分支渲染（`ActionSheet` 两档，`destructive=true` 标红项在后，按 §5.3）。
完成形态：`grep "SheetAction(" AiSettingsScreen.kt` 命中 ≥2 处。

**STEP-L1-4.8**：新增 `grandfatherPanelOpen` 的 `LaunchedEffect(state.loaded)`（按 INV-L1-11，非 `LaunchedEffect(Unit)`，条件含 `&& !keyDialogOpen && !consentPanelOpen && !vendorConfirmOpen` 三弹层互斥守卫）；渲染分支 `onAgree` 调 `vm.resolveGrandfather(confirm=true)`，`onDecline` 调 `vm.resolveGrandfather(confirm=false)`，`onClose` 显式绑定 `{ grandfatherPanelOpen = false }`（不写任何 consent 状态）。
完成形态：`grep "LaunchedEffect(state.loaded)" AiSettingsScreen.kt` 命中 1 处；`grep "!keyDialogOpen && !consentPanelOpen && !vendorConfirmOpen" AiSettingsScreen.kt`（或等价三元互斥表达式）命中 1 处；`grep "resolveGrandfather(confirm" AiSettingsScreen.kt` 命中 2 处；`grep "onClose = { grandfatherPanelOpen = false }" AiSettingsScreen.kt` 命中 1 处。

**STEP-L1-4.9**：`pendingSnackbar` 的展示机制——顶层先 `val snackbar = com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar.current`（Composable 作用域内 hoist，不得在 `LaunchedEffect` block 内读 `.current`——GC-25，字面照抄 `AddDayFoodScreen.kt:189` 的 `val appSnackbar = ...LocalAppSnackbar.current` 与 `:213` 的 `appSnackbar?.showMessage(...)` 两处既有写法）；`pendingSnackbar?.let{msg -> LaunchedEffect(msg){ snackbar?.showMessage(msg); pendingSnackbar=null } }`。STEP-L1-4.4/4.6 的写入点分别设 `pendingSnackbar="已启用云端 AI"`/等价关闭反馈文案。
完成形态：`grep "LocalAppSnackbar.current" AiSettingsScreen.kt` 命中 1 处（在 Composable 顶层，非 `LaunchedEffect` block 内——人工核对缩进层级）；`grep "pendingSnackbar" AiSettingsScreen.kt` 命中 ≥3 处（声明+至少两处写入+一处消费）。

### 批 L1-5：政策文案

**STEP-L1-5.1**：`PolicyContent.kt` 隐私政策 §一正文末尾追加一句限定（内容方向不变，逐字文案交 `copywriter`）。
完成形态：`grep "云端 AI" PolicyContent.kt` 命中 ≥1 处（在 §一对应段落）。

**STEP-L1-5.2**：`PolicyContent.kt` 新增独立小节——**插在现 §三"设备权限与用途"之后、现 §四"第三方 SDK"之前，作为新 §四**；原 §四~§八（"第三方 SDK"~"政策更新与联系"）依次顺延为新 §五~§九（v2b 修正 v1 的自相矛盾表述，见 §10 v2 挑战第17项）。标题方向"云端 AI（默认关闭 · 需你自行配置密钥并单独同意）"。
完成形态：`grep "九、政策更新与联系" PolicyContent.kt` 命中 1 处（顺延后的唯一新序号，确认已顺延）；`grep "八、政策更新与联系" PolicyContent.kt` 零命中（确认旧序号已不存在，v2b 修正 v1 坏判据，见 §10 v2 挑战第18项——原判据在改动前就已对既有 §五~§八 误命中，不具区分力）；新小节标题字符串命中 1 处（人工核对内容）。

**STEP-L1-5.3**：`POLICY_UPDATED` 改为字面量 `"最近更新：2026年8月"`。
完成形态：`grep "最近更新：2026年8月" PolicyContent.kt` 命中 1 处。

**STEP-L1-5.4**：同步更新 `.ai-context/docs/feature/隐私政策与用户协议.md`，使其与 `PolicyContent.kt` 本批变更内容一致（见 §10 C-21）。
完成形态：人工核对两份文档新增小节内容一致。

### 批 L1-T：测试

**STEP-L1-T-1**：新建 `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/ai/AiRuntimeConfigConsentTest.kt`，用真实 `PreferenceRepository(db)`/`AiRuntimeConfig(prefs)`（`db` 走既有 `RepositoryTestDatabase` 内存 SQLite 惯例，参照 `AiMealInputViewModelStreamTest.kt:186-198` 既有写法），覆盖 T-L1-01a/b/c、T-L1-02a/b/c/d（见 §8.2）。

**STEP-L1-T-2**：新建 `androidApp/.../ui/ai/CloudAiSaveRouteTest.kt`（纯 JVM 单测，无需 Compose/Robolectric），覆盖 T-L1-04a/b/c、T-L1-05、T-L1-08、T-L1-10。

**STEP-L1-T-3**：新建 `androidApp/.../ui/ai/AiSettingsViewModelConsentTest.kt`，覆盖 T-L1-06a/b、T-L1-07a/b、T-L1-09、T-L1-12。

**STEP-L1-T-4**：新建独立集成测试文件覆盖 T-L1-03a/b（**不修改** `AiMealInputViewModelStreamTest.kt`）。

**验收命令**：
```
scripts\build-cli.bat :shared:testDebugUnitTest
scripts\build-cli.bat :androidApp:testDebugUnitTest
scripts\build-cli.bat :androidApp:assembleDebug
```

---

## §8 测试矩阵

### 8.1 测试夹具职责边界（GC-07，v2 修正）

| 夹具 | 职责 | 禁止 |
|---|---|---|
| **真实** `PreferenceRepository(db)` + `AiRuntimeConfig(prefs)`，`db` 用既有 `RepositoryTestDatabase`（内存 SQLite 驱动） | 测试通过预写偏好值构造初态，验证真实持久化往返；**v1 计划的 fake `AiRuntimeConfig`/`PreferenceRepository` 已废弃**——两者均为 `final class`，无接口可 mock（见 §10 C-17） | — |
| shared 测试用 fake `AiRuntime`（`runtimes` map 里的 `CLOUD` 实现） | 记录 `complete()` 是否被调用（用于 T-L1-02a 断言"零调用"） | 内部做同意判断逻辑（判断逻辑必须留在 `SwitchableAiRuntime` 里被测） |

### 8.2 INV↔T 双向映射表

| INV | T-ID | 断言要点 |
|---|---|---|
| INV-L1-01 | T-L1-01a | 无 consent 记录 + 全部厂商 Key 均空 → `NOT_ASKED` |
| INV-L1-01 | T-L1-01b | 无 consent 记录 + 当前选中厂商 Key 空但**另一厂商**非空 → `GRANDFATHER_PENDING`（v2 核心修复点） |
| INV-L1-01 | T-L1-01c（v2b 新增） | 预写一条**无法反序列化**的脏字符串到 `KEY_CLOUD_AI_CONSENT`，即使此时某厂商 Key 非空 → 仍返回 `NOT_ASKED`（fail-closed，不落入 grandfather 推导） |
| INV-L1-02 | T-L1-02a | `DECLINED`+非空Key → `SwitchableAiRuntime.complete()` 返回 `Result.failure`（`isFailure`），fake `CLOUD` runtime 的 `complete()` 断言**零调用** |
| INV-L1-02 | T-L1-02b/c | `GRANTED`/`GRANDFATHER_PENDING`+非空Key → 正常路由到 `CLOUD` runtime，`complete()` 断言调用 1 次 |
| INV-L1-02 | T-L1-02d | `NOT_ASKED`+非空Key → `Result.failure`（该初态需测试直接预写偏好 JSON 构造，正常运行时不可达——`NOT_ASKED`+任一厂商 Key 非空这一组合在 INV-L1-01 的推导规则下不会自然产生，此测试是防御性覆盖） |
| INV-L1-03 | T-L1-03a | 集成测试：`AiMealInputViewModel` 真实构造（`activeType=CLOUD`+非空Key+`DECLINED`），走一遍 `submit()`，断言最终 `state.parseSourceMessage` **精确等于**"本次结果：规则解析（AI 解析失败：还没有同意把数据发给云端 AI）"这一完整字面量（非仅"含'规则解析'字样"，避免文案重复/病句类缺陷被掩盖），且 `state.phase` 不是 `ERROR` |
| INV-L1-03 | T-L1-03b（v2b 新增） | 同场景走 `confirmHealthAdvice()`，断言 `state.healthAdviceError == "还没有同意把数据发给云端 AI"` 且 `state.healthAdvice == null`（该消费点无规则兜底，见 §10 v2 挑战第5项） |
| INV-L1-04 | T-L1-04a/b/c | `routeOnSave` 纯函数三分支：`DIRECT`（已满足+已确认vendor）、`VENDOR_CONFIRM`（已满足+新vendor）、`FULL_CONSENT`（`NOT_ASKED`/`DECLINED`/`GRANDFATHER_PENDING`/scopeVersion落后） |
| INV-L1-05 | T-L1-05 | 空 Key 任意 consent 状态 → 恒 `DIRECT` |
| INV-L1-06 | T-L1-06a | mock 断言 `setCloudAiConsent` 调用先于 `onSaveVendorKey`（透传的底层保存） |
| INV-L1-06 | T-L1-06b | 写入 `next.acknowledgedVendors` 含"此刻扫描到的全部已配置Key厂商 ∪ {当前vendor}"（非仅当前vendor），且与 `state.cloudAiConsent` 完全一致（同一对象镜像） |
| INV-L1-06 | 真机 E-L1-04（v2b 从单测降级，见 §10 v2 挑战第19项） | `pendingSnackbar` 在弹层实际关闭之后才展示反馈，不与 Dialog 遮挡；`consentPanelOpen` 是纯 Compose 局部状态，无法在当前项目的 JVM/VM 单测设施下断言，改走真机验证 |
| INV-L1-07 | T-L1-07a/b | a：`declineConsent()` → `status=DECLINED`，`onSaveVendorKey` 未被调用；b：`closeCloudAi(vendor, false)` → `status=DECLINED`，`activeType=MOCK`，`onSaveVendorKey` 未被调用 |
| INV-L1-08 | T-L1-08 | `DECLINED` 初始态走 `routeOnSave` 非空Key → `FULL_CONSENT`，与 `NOT_ASKED` 初始态结果一致 |
| INV-L1-09 | T-L1-09 | `resolveGrandfather(true)` → `source==GRANDFATHER_CONFIRMED`，`acknowledgedVendors` 含全部已配置厂商；`resolveGrandfather(false)` → `DECLINED`+`activeType=MOCK`；两者均**不**调用 `onSaveVendorKey`；真机 E-L1-01 |
| INV-L1-10 | T-L1-10 | 纯函数 `shouldShowCloudStatusBlock` 的 JVM 单测（v2b 从"逻辑层用状态计算断言"这一无法兑现的描述改为真正可执行的纯函数测试，见 §10 v2 挑战第19项）：`GRANTED`+空Key → false；`GRANTED`+非空Key → true；`DECLINED`/`NOT_ASKED`/`GRANDFATHER_PENDING` → 恒 false；真机 E-L1-02 复核渲染层 |
| INV-L1-11 | 真机 E-L1-01/E-L1-05 | `reload()` 异步完成后 `state.loaded` 从 false 变 true、`LaunchedEffect(state.loaded)` 因 key 变化重新执行（Compose 语义保证，非纯单元测试可验证）；E-L1-05 另验冷启动首屏快速点开 KeyDialog 不应与 grandfather 面板同屏 |
| INV-L1-12（v2b 新增） | T-L1-12 | 构造 `state.type==CLOUD && cloudAiConsent.status==DECLINED`，`shouldShowCloudStatusBlock` 为 false 的同时，"重新启用"行渲染判据为 true（纯函数或状态计算断言）；真机 E-L1-03 |

---

## §9 交付台账（CODE 完成时填）

### STEP 勾销表

| STEP-ID | 状态 | 落地 commit | diff 定位 |
|---|---|---|---|
| STEP-L1-1.1~1.4 | ⬜ | | |
| STEP-L1-2.1~2.4 | ⬜ | | |
| STEP-L1-3.1 | ⬜ | | |
| STEP-L1-4.1~4.9 | ⬜ | | |
| STEP-L1-5.1~5.4 | ⬜ | | |
| STEP-L1-T-1~4 | ⬜ | | |

### 验收命令输出 / 真机待验证登记

（交付时填；`E-L1-01`/`E-L1-02` 登记至时间戳最新的 `真机待验证清单_<yyyyMMddHHmm>.md`，另补录第一版 apple_software_behavior 设计输出中"建议真机验证条目"7 条完整场景）

---

## §10 独立挑战台账（GC-37）

### v1 挑战记录（2026-08-08，独立 Explore/opus agent，只读源码+v1 成文）

**结论：不可进入 `BLUEPRINT_READY`，判定需结构性返工**。共 28 项挑战：17 项 CONFIRMED-ISSUE、5 项 MINOR-NIT、6 项 CONFIRMED-FINE。核心发现：`isModelReady()` 在现状代码里不是任何真实调用点的网络出口闸门（`SwitchableAiRuntime.complete()` 按 `activeType()` 路由、`AiMealInputViewModel.configReady` 独立窄判据、`CloudAiRuntime` 只判 Key 是否为空，三条路径均不读 `isModelReady()`），v1 把同意收进 `isModelReady()` 的设计前提本身不成立。

**v2 处置对照表**：

| v1 编号 | 挑战摘要 | 裁决 | v2 处置 |
|---|---|---|---|
| C-01 | `isModelReady()` 不是真实外发闸门 | CONFIRMED-ISSUE | 闸门改落 `SwitchableAiRuntime.complete()`（§1.3 In Scope #2、§4.4） |
| C-02 | `onTypeChange(CLOUD)` 无闸 | CONFIRMED-ISSUE | 运行时层闸门自动覆盖，无需在此额外拦截 |
| C-03 | AI 记一餐被禁改，闸门对其无效 | CONFIRMED-ISSUE | 运行时层闸门透传覆盖，`AiMealInputViewModel` 保持禁改，新增 INV-L1-03 集成测试实证（非仅论证） |
| C-04 | D4 事实错误（真实默认是 CLOUD 不是 MOCK） | CONFIRMED-ISSUE | 撤销该"顺手修"，§1.3 移入 Out of Scope 并说明原因 |
| C-06 | `grantConsent()` 内存态部分镜像 | CONFIRMED-ISSUE | 五个动作函数统一"构造一份 `next` 值对象、两处复用同一对象"写法 |
| C-07 | 错误注释"内部会再 reload" | CONFIRMED-ISSUE | 删除该注释 |
| C-08 | `LaunchedEffect(Unit)`+`loaded` 组合失效 | CONFIRMED-ISSUE | 改 `LaunchedEffect(state.loaded)`，新增 INV-L1-11 |
| C-09 | `grantedAtEpochMs` 秒×1000 假毫秒 | MINOR-NIT | 字段改名 `grantedAtEpochSeconds`，直接用 `nowEpochSeconds()` |
| C-10 | `CapsuleOutlineButton` 缺 import | MINOR-NIT | STEP 补全 import 清单 |
| C-11 | 对话框签名自相矛盾（内部页1 vs 回调给宿主） | CONFIRMED-ISSUE | 撤销"双页合一对话框"设计，改为宿主管理多个独立弹层（§4.4/§5.1） |
| C-12 | 轻量确认宿主状态协调无 STEP | CONFIRMED-ISSUE | §7 批 L1-4 拆为 9 条独立 STEP，逐个弹层显式状态管理 |
| C-13 | Snackbar 无宿主+撞 Dialog 遮挡踩坑 | CONFIRMED-ISSUE | 引入 `pendingSnackbar` 宿主级状态，弹层关闭后下一帧展示 |
| C-14 | grandfather 判据只查当前厂商 | CONFIRMED-ISSUE | `cloudAiConsent()` 遍历全部厂商（INV-L1-01） |
| C-15 | `GRANTED`+空Key 状态块仍渲染的矛盾 | CONFIRMED-ISSUE | INV-L1-10 双条件渲染 |
| C-16 | grandfather 确认无处取 key/vendor，函数清单不一致 | CONFIRMED-ISSUE | 新增 `resolveGrandfather(confirm: Boolean)` 不接收 key，`acknowledgedVendors` 扫描全部已配置厂商；§4.1/§4.4/§6/STEP 统一为 5 个函数名 |
| C-17 | fake `AiRuntimeConfig`/`PreferenceRepository` 因 final 不可实现 | CONFIRMED-ISSUE | §8.1 改真实例+内存库 |
| C-18 | shared 无 `commonTest` 源集 | MINOR-NIT | 路径写死 `androidUnitTest` |
| C-19 | 分流逻辑在 Composable 里，JVM 测不到 | CONFIRMED-ISSUE | 抽出 `routeOnSave()` 纯函数（§4.4） |
| C-20 | grep 判据反引号错位+跨行不命中 | MINOR-NIT | 改用 `rg -U` 跨行模式（STEP-L1-5.2） |
| C-21 | allowlist 与章节顺延互斥+漏同步文档 | CONFIRMED-ISSUE | allowlist 显式豁免序号顺延；新增 STEP-L1-5.4 同步 `隐私政策与用户协议.md` |
| C-22 | `POLICY_UPDATED` 未给字面量 | MINOR-NIT | 写死 `"最近更新：2026年8月"` |
| C-28 | "全App零改动继承"断言有3条例外 | CONFIRMED-ISSUE | 随 C-01 一并解决，新增 INV-L1-03 实证测试而非仅断言 |
| C-05/23/24/25/26/27 | 各类事实核实 | CONFIRMED-FINE | 无需改动，v2 保留对应设计 |

### v2 挑战记录（2026-08-08，独立 Explore/opus agent 第二轮，只读 v2 成文+源码，未参与返工过程）

**结论：核心闸门设计前提这次站住了**——独立穷举全仓 `CloudAiRuntime` 触达路径（Koin 绑定、四个 runtime 调用点、`AiRuntime.stream()`/`chat()` 默认实现）确认 `SwitchableAiRuntime.complete()` 确实是唯一分发入口，`RecommendationOrchestrator`/`PlanOrchestrator` 的 `RULE_FALLBACK` 兜底路径也独立复核为真实存在且可见。v1 的 C-01 类结构性错误未复发。但挑出 **7 项 CONFIRMED-ISSUE**（含 3 项设计缺口）+ 6 项 MINOR-NIT + 8 项 CONFIRMED-FINE。裁决："不能直接进入 `BLUEPRINT_READY`，但不需要像 v1 那样结构性返工"，建议局部修订后可转 `BLUEPRINT_READY`。

**v2b 局部修订对照表**（已就地处置，本蓝图当前正文已是修订后版本）：

| 编号 | 挑战摘要 | 裁决 | v2b 处置 |
|---|---|---|---|
| 第1~4项 | 闸门唯一性/`stream()`未重写/调用链无绕过/`RULE_FALLBACK`路径真实存在 | CONFIRMED-FINE | 无需改动，核心设计确认成立 |
| 第5项 | `confirmHealthAdvice()` 是被漏掉的第四个消费点，且异常文案在该路径上"已使用规则推荐"是事实错误（该路径无规则兜底） | CONFIRMED-ISSUE | 异常 message 改为中性"还没有同意把数据发给云端 AI"；INV-L1-03 补录该消费点+新增 T-L1-03b |
| 第6项 | AI 记一餐既有文案模板拼出"规则解析…规则推荐"重复病句 | CONFIRMED-ISSUE | 随第5项 message 改写一并解决；T-L1-03a 改为断言完整字面量而非"含关键字" |
| 第7项 | `DECLINED`+重选 CLOUD 是无出口死角，且 INV-L1-07 有悬空引用 | CONFIRMED-ISSUE | 新增 INV-L1-12 + `CloudSection` "重新启用"行 + STEP-L1-4.6/T-L1-12/真机 E-L1-03 |
| 第8项 | `acknowledgedVendors` 只加当前vendor，可被"切模型下拉到已有grandfather Key的另一厂商"绕过确认 | CONFIRMED-ISSUE | `grantConsent()` 改为扫描此刻全部已配置Key的厂商并入 `acknowledgedVendors`，非仅当前vendor |
| 第9项 | consent JSON 解析失败时 fail-open 落入 grandfather 推导，与蓝图自定红线冲突 | CONFIRMED-ISSUE | `cloudAiConsent()` 改为仅"确无记录"才推导 grandfather，"有记录但解析失败"fail-closed 返回 `NOT_ASKED`；新增 T-L1-01c |
| 第10项 | `pendingSnackbar` 片段在 `LaunchedEffect` block 内读 `CompositionLocal.current` 编译不过 | CONFIRMED-ISSUE | 改为 Composable 顶层 hoist `val snackbar = LocalAppSnackbar.current`，照抄 `AddDayFoodScreen.kt:189,213` 既有范例 |
| 第11项 | STEP-L1-4.9 指引不够精确到可直接照抄 | MINOR-NIT | STEP 直接写死 `AddDayFoodScreen.kt:189,213` 作为样板 |
| 第12项 | `grandfatherPanelOpen` 与 `keyDialogOpen` 未互斥，冷启动首屏窗口期可同屏堆叠；`onClose` 未定义 | CONFIRMED-ISSUE | `LaunchedEffect` 条件加三弹层互斥守卫；`onClose` 显式绑定为仅关闭不写状态 |
| 第13项 | `CloudModels.ALL` 遍历写法对新增/下线厂商场景语义合理 | CONFIRMED-FINE | 无需改动 |
| 第14项 | `grantConsent`→`onSaveVendorKey` 的连续 `state=state.copy()` 是否互相覆盖 | CONFIRMED-FINE（附带 MINOR-NIT：`closeCloudAi` 写入顺序与其余四函数不一致） | 核实不构成竞态，无需改动；写入顺序不一致暂不动（不影响正确性，非阻断） |
| 第15项 | 新代码片段可编译性 | CONFIRMED-FINE | 无需改动 |
| 第16项 | `PreferenceKeys.CLOUD_AI_CONSENT` 是无消费者的重复常量，双真相源 | MINOR-NIT | 撤销该常量，只保留 `AiRuntimeConfig.KEY_CLOUD_AI_CONSENT` 一处（同步删 STEP-L1-1.4/§6 对应行） |
| 第17项 | 政策新小节插入位置描述自相矛盾（"§二之后"与"原四~八→五~九"对不上） | CONFIRMED-ISSUE | 改为无歧义单句："插在现§三之后、现§四之前，成为新§四；原§四~§八顺延为§五~§九" |
| 第18项 | grep 判据在改动前就已对既有 §五~§八 误命中，不具区分力 | MINOR-NIT | 改判据为"新增 §九标题命中 + 旧§八标题零命中" |
| 第19项 | T-L1-06c/T-L1-10/T-L1-11 分派进纯 JVM VM 测试文件，但断言对象是 Compose 局部状态，测不到（C-19 同类复发） | CONFIRMED-ISSUE | `shouldShowCloudStatusBlock` 抽纯函数纳入 T-L1-10（JVM可测）；T-L1-06c/T-L1-11 降级为真机项 E-L1-04/E-L1-01/E-L1-05，从 §8.2 单测列移出 |
| 第20项 | `pendingKeyDraft` 用 `rememberSaveable` 会把明文 Key 落进系统持久化存储 | MINOR-NIT | 改用 `remember`（代价：进程重建丢草稿，可接受） |
| 第21项 | T-L1-02d 测的是"正常流程不可达"的初态，未加说明 | MINOR-NIT | §8.2 补充说明该初态需直接预写偏好 JSON 构造 |

**处置后自查结论**：第5/8/9 三项设计缺口（reviewer 点名的"改完需就地自查是否真闭环"）均已在 §3/§4.4/§7/§8.2 同步修订，非仅改动 message 文案层面。本蓝图 v2b 修订完毕，**转 `BLUEPRINT_READY`**——按 GC-37 机制，蓝图冻结前的独立挑战义务已满足两轮（v1 结构性挑战 + v2 局部挑战），第二轮裁决明确"不需要第三轮完整挑战"，改动集中于起草方自查范围内的设计缺口，未引入新的结构性不确定性。

---

## §11 门禁与角色

- 本批含新交互，已完成强制的 `apple_software_behavior`+`apple_ux_designer` 前置设计门禁。
- `CloudAiDisclosure.WILL_SEND`/`WONT_SEND` 与政策新增小节文案，**必须**经 `copywriter` 专项审校。
- CODE 完成、构建+单测通过后，须走 `google_quality_engineer` 代码质量终审；本批涉及运行时分发层改动（`SwitchableAiRuntime`），建议 `google_architecture_engineer` 补充视角核对是否存在绕过该分发层的其他云端调用路径（呼应 §10 v2 挑战重点①）。

## §12 弃置项登记（GC-03 前瞻）

| 项 | 状态 | 归宿 |
|---|---|---|
| `EngineSourceBadge` 统一组件化 | 显式弃置 | 独立 fast-follow |
| 全 App 云端调用点"静默回退"逐点审计 | 显式弃置（核心路径已验证） | 独立排查任务 |
| Grandfather 补确认的"下一次真实云端请求前"第二触发点 | 显式弃置 | 运行时闸门已覆盖安全底线，UI 提醒覆盖面是体验加强项，独立 fast-follow |
| 接收方隐私政策跳转链接 | 显式弃置，非阻断可选加项 | 已有申请指南提供官网跳转 |
| `CloudAiDisclosure.INCREMENT_NOTES`（外发范围扩大时的增量说明文案） | 预留空实现 | 下次 `SCOPE_VERSION` 递增时随该次改动补齐 |
