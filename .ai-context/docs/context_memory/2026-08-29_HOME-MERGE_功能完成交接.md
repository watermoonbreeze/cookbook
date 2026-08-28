# HOME-MERGE 功能完成交接（2026-08-29）

## 结论

`HOME-MERGE-01` 已完成自动化验收，状态为 **ARCH_ACCEPTED / AUTOMATED_GATES_PASS / PENDING_DEVICE_VERIFICATION**。本轮通过事实复核关闭历史 `BLUEPRINT_READY` 文档漂移，不新增业务代码。

## 已验证事实

- `c7160d31` 完成首页统一三天视图和 `observeTodayPlusFuture` 读取修正；`c207e125` 补齐首页层级间距与证据。
- `HomeScreen` 当前固定顺序为一周计划卡 → “今天和接下来” → 今天卡或 `DayPlaceholderCard` → 最多两张真实未来卡 → “查看全部食历”。未来为空时不显示伪空态。
- `MealRecordRepositoryTest` 通过，含“今天缺失只取最早两个未来日期”和“今天加最多两个未来日期”边界；Android Debug 构建通过。

## 下一模型操作边界

1. 不重复实现 HOME-MERGE，也不扩大改动到推荐卡、营养色系墙、周计划内容或数据库结构。
2. R10 真机阶段仅使用 `.ai-context/docs/真机验证/真机待验证清单_202608282315.md` 的 `E-HM-01~08`；视觉、无障碍和导航跳转仍须真机确认。
3. 真机若发现问题，按最小缺陷批次修复并回写本蓝图；不得回滚到旧的“今日/计划”双区设计。

## 全景图落点

- 蓝图：`feature/首页今日与计划合并_实施蓝图.md` §11.2.1。
- 功能事实与待办：`projectReview/features/F-MEAL/20_实现.md`、`30_待办.md`。
- 路线图：`projectReview/10_后续执行路线图与蓝图库.md` R1。
