# FOUNDATION-OBSERVABILITY-02-R2 任务快照

- 任务包：`.ai-context/docs/外部方案/在线审核/FOUNDATION_OBSERVABILITY_02_R2_USER_INTERACTION_OBSERVABILITY_COMPLETION.zip`
- 基线：FOUNDATION-OBSERVABILITY-02-R1 完成版本，基线 commit `9720be03`
- 目标：补齐用户意图 → UI Event → Navigation → Screen Lifecycle → State Transition → Operation → Result 的 Trace。
- 级别/模式：L7 Mechanical Execution Contract；标准五阶段执行，当前由 CODE 执行后交 REVIEW。
- 允许范围：既有 Trace 模型、Logger、Android UI 点击/导航/页面生命周期/状态旁路记录、既有 AI Recommend 两入口、测试与证据/状态记录。
- 禁止范围：业务规则、AI Recommend 算法、数据库、业务流程、未定义 Event、未授权的其他埋点范围。
- 已知 R1 缺口：`ui.action.clicked` 与 R2 `ui.click` 不一致；无统一 action result；AI 入口和导航串联不完整。
- 风险：UI 回调埋点不得改变导航、状态或业务执行；Trace 字段只能使用代码标识，不记录用户输入、饮食文本或模型内容。
- 待验证：Trace 模型单测、Android 相关测试、shared 单测、assembleDebug、Evidence 静态检查；真机证据按清单保持待验证，不伪造 PASS。
