# 2026-08-22 · FOUNDATION-OBSERVABILITY 蓝图返工

## 任务卡

| 项 | 内容 |
|---|---|
| 目标 | 把 Foundation Observability 从 `DRAFT / REWORK_REQUIRED` 补齐为可机械执行的 L7/48-GC 蓝图；随后按蓝图继续实施统一日志最小闭环。 |
| 模式 | 深度 / BLUEPRINT-FULL / L7；跨 shared/androidMain/androidApp，含隐私、异步日志、公共契约。 |
| ARCH | 主线程；Sol 只读产出独立架构契约与验收合同。 |
| CODE | 待蓝图冻结后分派；冻结前不修改生产代码。 |
| 非范围 | 全项目日志迁移、第二日志出口、业务/Repository/schema/UI 行为改动、原始饮食文本/prompt/token 持久化。 |
| 当前事实 | AppLogger 是 Android debug 文件唯一落点；shared 在 sink 安装后转入 AppLogger，初始化前保留 Logcat fallback。MDC3 静态证据已完成，`E-MDC3-01~03` 真机日志仍待补。 |
| 已知阻断 | 缺 ROLE_CONTRACT、L7 工件、Trace/时钟/并发/sink 生命周期/字段序列化/release 策略/测试合同的唯一决策。 |
| 验收 | 蓝图检查与独立挑战通过；实施后 T-OBS-01~07、三条构建命令、ARCH diff 审核；真机清单用可操作步骤+预期日志+失败判据。 |

## ARCH 冻结交接（2026-08-22）

- 用户指定 ARCH 自审；已补 ROLE_CONTRACT、L7 勾销、冻结 API/状态机/JSONL/隐私/sink 生命周期、INV、STEP、T/E 与 coder 四列交付规则。
- 握手已转 `BLUEPRINT_READY / TURN=CODE`。CODE 首读：`BLUEPRINT_STATE.md` → `FOUNDATION_OBSERVABILITY_01_CODE_BLUEPRINT.md` §0/§2/§3/§4/§8~§10。
- CODE 只执行 `STEP-OBS-01~07`；MDC3 `E-MDC3-01~03` 与 Foundation 真机项均不得伪造 PASS。
