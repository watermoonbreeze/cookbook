package com.sxdbsm.cookbook.domain.meal

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** [AI生成] Meal Domain 的稳定身份；旧存储 id 只能通过 Adapter 映射进入该身份。 */
@JvmInline
value class MealId(val value: String) {
    init {
        require(value.isNotBlank()) { "MealId must not be blank" }
    }
}

/** [AI生成] 事实来源分类；AI 结果保持 suggestion/context，不直接成为 MealSource。 */
enum class MealSource {
    USER,
    LEGACY_ADAPTER,
}

/** [AI生成] Meal 的描述性元数据，不承载投影或 AI 推荐状态。 */
data class MealMetadata(
    val title: String = "",
    val note: String = "",
    val createdAtEpochMillis: Long = 0L,
)

/** [AI生成] 一次发生记录与 Meal 聚合身份分离。 */
data class MealOccurrence(
    val occurrenceId: String,
    val mealId: MealId,
    val date: LocalDate,
    val time: LocalTime? = null,
) {
    init {
        require(occurrenceId.isNotBlank()) { "occurrenceId must not be blank" }
    }
}

/** [AI生成] Canonical Meal Aggregate Root；事实只从 Meal 及其受控生命周期读取。 */
data class Meal(
    val id: MealId,
    val source: MealSource,
    val metadata: MealMetadata = MealMetadata(),
    val lifecycle: MealLifecycle = MealLifecycle.DRAFT,
    val occurrences: List<MealOccurrence> = emptyList(),
)

