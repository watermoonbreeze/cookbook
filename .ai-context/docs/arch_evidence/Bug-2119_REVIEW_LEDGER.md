# Bug-2119 · REVIEW 增量台账

| 时间 | reviewer | commit 范围 | 检查项 | 可见证据 | 结论 | 下一步 |
|---|---|---|---|---|---|---|
| 2026-08-27 | Codex | `ea4b5132..c8d40556` | 启动与范围冻结 | 蓝图、任务前快照、`BLUEPRINT_STATE.md` 已记录本批基线与 `TURN=REVIEW` | 进行中 | 核对成员分支、真实 VM 回归、真机状态与文档范围 |
| 2026-08-27 | Codex | 同上 | 自动化证据 | `FamilyStatsViewModelTest` 定向 BUILD SUCCESSFUL；`:androidApp:testDebugUnitTest` 与 `:androidApp:assembleDebug` BUILD SUCCESSFUL | PASS | 仅等待独立代码审查；真机仍 PENDING |
| 2026-08-27 | Codex | 同上 | allowlist | `core.quotePath=false` 下 blueprint checker exit 0，改动文件数 7 | PASS | 独立复核最终判定 |
| 2026-08-27 | 独立复核 | `ea4b5132..c8d40556` | T-2119 覆盖 | 在场成员仅断言非零，缺 T-2119-01/03 精确证明 | AF-2119-01 | 同一真实夹具补精确在场、缺席承接与全家 breakdown 断言 |
| 2026-08-27 | 独立复核 | 同上 | 历史日期口径 | 今日 `excluded` 的 share 被复用于近 7 天循环 | AF-2119-02 | 历史日期使用既有全员份额，仅今日使用在场份额；补昨日回归 |
