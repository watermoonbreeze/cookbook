# AIMEAL-RELIABILITY-01 · Reality 与冻结前蓝图

> 状态：DECIDED。SOL 裁决见 `SOL-DECISIONS-20260829.md` §二；不是 CODE 合同，任何子批在独立 L7 蓝图冻结前不得实施。

## 1. 已由代码事实关闭的历史项

| 原项 | 事实 | 处置 |
|---|---|---|
| B7F-03 / I6 的无限期流式卡死 | `StreamTransport.kt` 已有 watchdog、强制 disconnect 与 `STREAM_TIMEOUT_ERROR`；`StreamTransportTimeoutTest` 覆盖心跳阻塞。 | 不重复实施；保留真机回归。 |
| I7 AI 失败兜底 | `AiMealInputViewModel.attemptRuleFallback()` 已是生成链的既有分支。 | **不可直接关闭**：当前为自动切换，而历史需求要求“保留原文、由用户确认是否转规则模板”；归入 R2-E 决策，不得借修复顺带改行为。 |
| K1c weekday 偏移 | `RuleMealParserWeekdayTest` 已覆盖。 | 从 R2 移出，归 R8 已实现能力。 |
| 模糊量词“差不多一碗面” | `RuleMealParserRegressionTest` 已锁定“不当作食用比例”。 | 仅作为 I3~I5 语料扩展的基线。 |

## 2. 剩余问题与推荐拆批

| 子批 | 覆盖 | 冻结决策 | 必须先写的测试 |
|---|---|---|---|
| R2-A `AIMEAL-ADVICE-GENERATION-01` | I8 | `healthAdviceJob` 与生成代际绑定；输入失效时取消 job 并清空 advice/loading/error/consent；回写前核验 generationId 与 preview identity。 | 旧建议请求→编辑→旧回包；取消后无回写；新 preview 只显示新建议。 |
| R2-B `AIMEAL-PARSER-QUALITY-01` | I3/I4/I5、AIMEAL-RULE-TEMPLATE | 先用语料确认规则解析与 AI fallback 的分工；规则只做可确定的文本规范化，不能静默补未知食材。模板不再让用户输入日期。 | 复合菜、括号菜、连接词、模糊量词、空输入、日期被拒绝六类精确断言。 |
| R2-C `AIMEAL-VOICE-LIFECYCLE-01` | B7F-VOICE / Bug-1763 | 保持入口隐藏；先证明系统权限→单实例→start/stop/release 的唯一 owner，成功后才可打开开关。 | denied/granted、busy、onDispose、重复点击、旋转/离页、离线失败。 |
| R2-D `AIMEAL-DIAGNOSTIC-COPY-01` | HTTP 内部代号、diagnostic 桶 | 文案只暴露用户可行动信息；诊断最多三条+余数，不原文透传协议字段；`when` 必须覆盖 `DiagnosticCode`。 | HTTP/timeout/schema error 的人话文案、未知 code、超过三条。 |
| R2-E `AIMEAL-FALLBACK-CONSENT-01` | I7 | 先由产品/架构明确“自动兜底+告知”还是“保留原文、用户确认”；两者改变失败时的可恢复性和确认页语义，未决前不改。 | AI 失败、规则成功/失败、用户拒绝切换、编辑重试四态矩阵。 |

### R2-B Reality：可冻结范围与决策门

`RuleMealParser` 已能保护括号深度、识别部分连接词，并已有“凉皮（黄瓜丝+绿豆芽）”“差不多一碗面”的基础回归；但现有模糊量词测试只断言 `eaten_ratio`，没有证明份量或菜名被规范化。缺陷 I3/I4/I5 中的两类诉求必须分开：

| 类别 | 例子 | 结论 |
|---|---|---|
| 可确定语法规范化 | 顶层“还有”、括号内 `+`、日期/时间残留、模糊前缀导致的份量锚点失效 | 可作为 `AIMEAL-PARSER-QUALITY-01` 的后续 L7 范围；先建精确输入→输出语料，不能只测“不崩溃”。 |
| 菜名语义拆解/补全 | “薄皮椒炒肉丝”拆食材与做法、“炒饭/大排饭”自动补米饭 | 与双阶段方案“规则模式才生成候选”、R2 继承不变量“不得静默补未知食材”冲突；须有菜品知识来源、置信/来源标记和用户可见确认，转 R3/R4 数据与产品决策，R2-B 不实施。 |

R2-B 在语料冻结前仍为 **RESEARCH**。最低语料合同：括号菜不拆错、顶层连接词不残留、模糊量词不作为 eaten ratio 且若声称识别份量须精确断言、空输入零菜、文本日期不改变当前会话日期。任何“自动补食材”用例必须先有独立 ADR。

### R2-D Reality：诊断展示先过隐私决策

现有确认页的 `AiMealInputSheet` 可从 `AiMealAttemptDiagnostic.rawResponse` 展示“查看原始返回”；而项目既有方案同时要求用户可见错误不透传协议字段、原始响应不进入日志/备份/同步。是否允许仅在内存中由用户二次点击查看原始模型响应，仍缺少数据分级、长度上限、敏感字段脱敏和截图/无障碍可见性规则，不能仅改 `summarizeDiagnostics()` 后宣称隐私关闭。

R2-D 的冻结前问题：①原始响应是否根本不进入 UI；②若保留，哪些字段必须结构化脱敏、最大长度是多少；③`DiagnosticCode.OTHER` 与未分类 transport message 的人话映射由哪个层拥有；④诊断对象是否可能跨配置变更/进程重建泄漏。未回答前只允许补测试和事实文档，不改显示逻辑。

### R2-C Reality：语音入口继续关闭

`VOICE_INPUT_ENABLED=false` 仍是唯一用户入口闸门，Manifest 虽声明 `RECORD_AUDIO`，但不会触发。现有 `VoiceRecognizer` 的创建、权限 launcher、start/stop/release 与 Compose `DisposableEffect` 都在 `AiMealInputSheet`；ViewModel 仅承载展示状态。真机曾复现“语音引擎繁忙”，而现有 Android 单测没有系统识别服务 fake 或 owner 生命周期断言。

因此 R2-C 尚不能冻结实现蓝图。先决产物应为：一个由 UI 单一拥有的可替换 recognizer port、权限结果到 start 的显式状态转换、每次离页/重复按压/系统 busy 的 stop+release 合同，以及 API 21/真实识别服务真机矩阵。满足前 `VOICE_INPUT_ENABLED`、Manifest 声明和帮助文案均不得改动。

## 3. 不变量（后续每个子批继承）

| ID | 必须 | 禁止 |
|---|---|---|
| INV-R2-01 | 失败/超时都复用既有主路径的 generation 与 fallback 校验。 | 新建旁路 parser 或独立日期/归属规则。 |
| INV-R2-02 | 用户未明确确认时，不持久化 AI/OCR 推断结果。 | 静默写入餐食、菜品或健康建议。 |
| INV-R2-03 | 被编辑失效的 generation 不得更新任何可见建议、错误或 loading 状态。 | 以裸 `viewModelScope.launch` 回写旧结果。 |
| INV-R2-04 | 语音入口在所有生命周期测试通过前保持 `VOICE_INPUT_ENABLED=false`。 | 为修 UI 直接重新开启。 |
| INV-R2-05 | 用户可见错误不得带 transport/protocol 内部代号、原始响应或隐私输入。 | 直接透传 exception/NDJSON 文本。 |

## 4. 执行顺序

`R2-A` → `R2-D` → `R2-B` → `R2-C`。

理由：A 是确定性状态正确性缺陷；D 降低异常信息暴露；B 需要先冻结产品语法；C 依赖真实设备/权限服务，最后独立实施并接受。

## 5. 验收入口

- 自动化：每子批独立 `T-R2x-*`，并运行 `:shared:testDebugUnitTest`、`:androidApp:testDebugUnitTest`、`:androidApp:assembleDebug`。
- 范围：机器可解析 allowlist、`feature_sync_check --range <base>..<head>`、`blueprint_check --allowlist`。
- 真机：只向唯一最新真机清单追加 `DEV-R2A/B/C/D-*`，不得覆盖既有条目或写 PASS。
- 终审：旗舰模型逐 INV、STEP、测试和当前 commit 审查；仅 ARCH 可以接受。

---
最后更新：2026-08-28 · 证据：`StreamTransport.kt`/`StreamTransportTimeoutTest`、`AiMealInputViewModel.kt`、`RuleMealParser*Test`、F-AI-MEAL 待办与缺陷档案。
