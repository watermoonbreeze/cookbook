# AIMEAL-RELIABILITY-01 · Reality 与冻结前蓝图

> 状态：RESEARCH。不是 CODE 合同；任何子批在独立 L7 蓝图冻结前不得实施。

## 1. 已由代码事实关闭的历史项

| 原项 | 事实 | 处置 |
|---|---|---|
| B7F-03 / I6 的无限期流式卡死 | `StreamTransport.kt` 已有 watchdog、强制 disconnect 与 `STREAM_TIMEOUT_ERROR`；`StreamTransportTimeoutTest` 覆盖心跳阻塞。 | 不重复实施；保留真机回归。 |
| I7 AI 失败兜底 | `AiMealInputViewModel.attemptRuleFallback()` 已是生成链的既有分支。 | 不另造第二 fallback；后续只审用户可见说明。 |
| K1c weekday 偏移 | `RuleMealParserWeekdayTest` 已覆盖。 | 从 R2 移出，归 R8 已实现能力。 |
| 模糊量词“差不多一碗面” | `RuleMealParserRegressionTest` 已锁定“不当作食用比例”。 | 仅作为 I3~I5 语料扩展的基线。 |

## 2. 剩余问题与推荐拆批

| 子批 | 覆盖 | 冻结决策 | 必须先写的测试 |
|---|---|---|---|
| R2-A `AIMEAL-ADVICE-GENERATION-01` | I8 | `healthAdviceJob` 与生成代际绑定；输入失效时取消 job 并清空 advice/loading/error/consent；回写前核验 generationId 与 preview identity。 | 旧建议请求→编辑→旧回包；取消后无回写；新 preview 只显示新建议。 |
| R2-B `AIMEAL-PARSER-QUALITY-01` | I3/I4/I5、AIMEAL-RULE-TEMPLATE | 先用语料确认规则解析与 AI fallback 的分工；规则只做可确定的文本规范化，不能静默补未知食材。模板不再让用户输入日期。 | 复合菜、括号菜、连接词、模糊量词、空输入、日期被拒绝六类精确断言。 |
| R2-C `AIMEAL-VOICE-LIFECYCLE-01` | B7F-VOICE / Bug-1763 | 保持入口隐藏；先证明系统权限→单实例→start/stop/release 的唯一 owner，成功后才可打开开关。 | denied/granted、busy、onDispose、重复点击、旋转/离页、离线失败。 |
| R2-D `AIMEAL-DIAGNOSTIC-COPY-01` | HTTP 内部代号、diagnostic 桶 | 文案只暴露用户可行动信息；诊断最多三条+余数，不原文透传协议字段；`when` 必须覆盖 `DiagnosticCode`。 | HTTP/timeout/schema error 的人话文案、未知 code、超过三条。 |

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
