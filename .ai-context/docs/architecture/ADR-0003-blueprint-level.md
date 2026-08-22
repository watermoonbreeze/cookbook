# ADR-0003：Blueprint L1→L7 颗粒度基线

- 状态：`ACCEPTED / PROJECT BASELINE`
- 日期：2026-08-22
- 范围：Blueprint 编写、CODE 执行、ARCH 复核
- 关联：`~/.ai-context/rules/blueprint_protocol.md` §2.1/§2.2、`experience/18_BLUEPRINT_LEVEL_STANDARD.md`、`OVN-P2-08`、commit `6177b0de`

## 决策

Level 表示 ARCH 为执行模型消除的决策空间，不是风险等级或模型能力等级；Cookbook 当前基线为 L7。L1→L7 依次闭合范围、证据、真相源、生命周期、索引投影、用户可见副作用和脚本勾销。

## 证据

项目 canonical protocol 与 experience/12 §12 已有 L1→L7/GC 登记；本批新增可引用的简明标准，不新增 L8、不下调 L7。

## 不决策

不根据单个模型样本自动提级、不改 `MODEL_ROUTING`、不修改实际模型配置。
