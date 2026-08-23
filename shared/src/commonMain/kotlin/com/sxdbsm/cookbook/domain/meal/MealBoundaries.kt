package com.sxdbsm.cookbook.domain.meal

/** [AI生成] Projection 只读 Meal 事实，不拥有身份、生命周期或写入能力。 */
data class MealProjection(
    val mealId: MealId,
    val title: String,
    val lifecycle: MealLifecycle,
)

fun Meal.toProjection(): MealProjection = MealProjection(
    mealId = id,
    title = metadata.title,
    lifecycle = lifecycle,
)

/** [AI生成] AI 输出边界：建议与上下文不具备创建 Meal Truth 的 API。 */
data class MealSuggestion(
    val title: String,
    val context: String = "",
)

/** [AI生成] 迁移只负责旧模型到 Canonical Meal 的适配，不改变旧 Repository 行为。 */
interface LegacyMealAdapter<in L> {
    fun adapt(legacy: L): Meal
}

