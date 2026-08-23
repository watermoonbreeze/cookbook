# State Lifecycle Observability R2 任务快照

- 需求：在 Unified Add Meal 状态恢复基础上，建设 state snapshot / restore / merge 的统一可观测能力，覆盖 AI Recommend、Food Search、Inventory、 新增菜品及记录饮食子流程。
- 级别：深度级；模式：常规五阶段；原因：跨 Android UI 状态、导航契约、结果合并、测试和治理文档，且涉及跨页面状态一致性。
- 角色计划：主线程负责源码定位、集成与最终验证；按项目门禁使用 DEV 分析/架构/测试角色参与独立检查。
- 当前状态：R2 任务包已解压并阅读；包内授权 REVIEW→CODE；基线 HEAD=`0c053f7d65233b049c94f5c0fc57aa6c35f84755`。
- 工作区已知变更：存在任务前的外部方案删除及新增目录/任务包，均不属于本批，提交时只暂存本批明确文件。
- 预计涉及：`androidApp` 状态/导航/VM 与对应单测；`.ai-context/docs/context_memory/BLUEPRINT_STATE.md`；`.ai-context/docs/experience/`；必要的上下文证据文件。
- 禁止范围：AI 推荐算法/策略、数据库结构、无关业务流程；不将真机缺失证据写成 PASS。
- 风险：现有 Unified Add Meal 已有状态恢复实现，需避免重复契约、导航行为改变和敏感业务字段进入日志；R2 包未给出固定文件 allowlist，需以源码事实和最小闭环限定范围。
- 待验证：snapshot、restore、merge 三类事件及字段；多入口回归；shared/androidApp 单测与 assembleDebug；远程推送结果。
