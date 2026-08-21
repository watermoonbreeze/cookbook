# Unresolved facts

以下事项无法仅凭本次仓库源码唯一确定，未做语义补齐：

1. 未发现独立 `MealPlan`、`DayMealData` 类型或计划表；只能确定当前 `isPlanState` 是 `DayMealCardData` 投影字段，不能确定外部方案是否将其作为历史概念、产品术语或未来目标。
2. `selectUpcomingMealRecords` 的 `UPCOMING_ROW_LIMIT` 足够覆盖“完整日期”的上限是否有数据规模保证，源码只展示常量和调用，未证明不会截断极端日期数据。
3. `DayMealCardData.isToday` 的精确计算包含在 repository 私有 `buildDayMealCard` 中，但本清单没有推导其产品意图；只能引用 `MealRecordRepository.kt:421-442` 的实际赋值。
4. AI `AutoGenPreview` 的 `hasExisting`、mergeMode 具体交互含义应以 `AutoGenModels.kt`/`DayAutoGenerator.kt` 字段和分支为准；本证据包不把“覆盖/合并”解释成目标架构决策。
5. `DONE` 后各宿主导航行为由 `AiMealBody`/`UnifiedAddMealScreen` 回调组合决定；本任务不判定哪种导航是规范行为。
6. `MealDateCalendarDialog` 的日期集合来源随调用点不同；组件本身没有统一日期真相源，不能推断应由 Repository 还是 VM 负责统一。
7. 本次搜索未将所有“可能间接影响 meal_record 的同步、导入导出、库存派生”文件纳入主投影链；相关 SQL 片段保留在 `Cookbook.sq`，不能据此声称已覆盖所有周边业务。
