# MIGRATION RISK

| 风险面 | 当前事实 | 风险等级 | 必须守住的条件 |
|---|---|---:|---|
| UI 影响 | Card UI 读取 `date`、`temporalRole`、`meals`；多个页面直接保存 Card 列表状态 | 中 | 投影结果字段与时间角色保持等价；不把中性内容直接塞给 Card UI |
| 测试影响 | Projector、Repository、Nutrition 聚合已有按模型断言；旧 `observeTodayPlusFuture` 测试仍存在 | 中 | 迁移前增加 API 等价性测试，覆盖空天、今天、未来、过去、排序和监听刷新 |
| 数据语义 | `MealDayContent` 不含时间角色；`DayMealCardData` 含唯一 `temporalRole` | 高 | 时间角色只能由 projector 依据显式 reference date 计算，不能由数据库日期或 UI 猜测 |
| 生命周期 | Flow API 绑定 SQLDelight query 变化；窗口观察额外监听关联表 revision token | 高 | 迁移不得丢失 `meal_record_dish`、`dish_ingredient`、`dish` 变更触发；取消/重订阅行为需回归 |
| Search 分页 | Search 通过 `searchMealCards` 间接依赖旧 Card API | 中 | 保留关键词、limit/offset、日期排序和 `distinctBy(date)` 行为 |
| 外部消费者 | 仓库内未发现旧 API 直接调用，但公共 Repository 可能被仓外消费者使用 | 中 | Deprecated 前先完成发布面确认；若无确认，不删除符号 |
| 误扩范围 | Repository 同时存在多条历史读取路径 | 高 | 本批仅形成分析；不得顺手合并 `observeTodayPlusFuture`/`observeTimelineWindow` 等其他路径 |

## 生命周期特别说明

`observeTimelineWindow` 当前通过 A 表记录 Flow 与三个 revision token 合并，确保餐食关联、配料和菜品修改也能刷新卡片（`MealRecordRepository.kt:199-221`）。任何将 Card 读取统一到新 API 的方案，若只替换查询而未复刻这些触发源，会产生“数据库已变、页面仍旧”的隐性回归。

## 本批结论

不执行 API 迁移。当前证据足以支持“读取层向 `MealDayContent` 收敛、投影层继续保留”的方向，但不足以授权删除旧 API 或合并全部观察链。
