# ADR-0001：KMP shared 与 Android UI 分层边界

- 状态：`ACCEPTED / PROJECT BASELINE`
- 日期：2026-08-22
- 范围：`:shared`、`:androidApp`、KMP platform 适配
- 关联：`experience/09_工程统一规范.md`、`projectReview/01_架构与技术底座.md`、`OVN-P2-09`、commit `65b7b4d6`

## 决策

`:shared` 的 commonMain 保持平台无关，跨平台能力通过 expect/actual；`:androidApp` 负责 Android UI/导航并依赖 `:shared`。禁止 shared commonMain 反向导入 Android 或 androidApp，禁止 Gradle `shared → androidApp` 反向模块依赖。

## 证据

`.ai-context/tools/architecture_quality_check.py` 对当前仓库通过，并用 unittest 反例锁定 shared 导入 Android 时失败。

## 不决策

本 ADR 不改变现有模块、不迁移业务代码、不新增依赖。
