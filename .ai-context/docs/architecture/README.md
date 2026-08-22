# Cookbook Architecture Knowledge Base

本目录沉淀可反查的架构决策，作为 Blueprint 的引用入口；不替代项目全景图、`projectReview/08_决策记录.md` 或 `BLUEPRINT_STATE.md`。

## 入口与来源

| 类型 | 入口 | 事实来源 |
|---|---|---|
| ADR 模板 | [`ADR_TEMPLATE.md`](ADR_TEMPLATE.md) | 本目录统一格式 |
| KMP/模块边界 | [`ADR-0001-kmp-boundary.md`](ADR-0001-kmp-boundary.md) | `experience/09_工程统一规范.md`、projectReview/01 |
| Observability/Trace | [`ADR-0002-observability-trace.md`](ADR-0002-observability-trace.md) | TraceModel、Logger、OVN-01~03 |
| Blueprint/Level | [`ADR-0003-blueprint-level.md`](ADR-0003-blueprint-level.md) | canonical protocol §2.1、experience/12 §12、OVN-P2-08 |
| 产品/治理决策总表 | `projectReview/08_决策记录.md` | 项目决策唯一入口 |
| 执行经验 | `experience/` | 模型执行与工程经验台账 |

## 关联规则

新增 ADR 必须注明状态、日期、影响范围、证据、关联 Blueprint/commit；已被项目决策总表承载的内容只建立链接，不复制整段正文。
