package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate

/**
 * @File : MealStreamDraftMapper
 * @Time : 2026/08/05
 * @Author : SXD-AI
 * @Desc : B3: 纯 mapper —— MealStreamDraft -> List<DayMealJson>。
 * <p>
 * 唯一增加映射的入口；不联网、不写库、不生成 fallback ID。
 * 规则见 `B3会话实施蓝图` §3.2。
 * <p>
 * [AI生成] B3 会话层。
 */
internal object MealStreamDraftMapper {

    private val SLOT_ORDER = mapOf(
        "breakfast" to 0, "lunch" to 1, "dinner" to 2, "snack" to 3,
    )

    /**
     * MealStreamDraft -> List<DayMealJson>。
     *
     * 1. 只遍历 draft 中精确命中 [segments] 的 segmentId；未知项不生成 day。
     * 2. MealDraftNode 仅在至少含一条非空菜名时转换。
     * 3. 同日期餐次按 meal_id 合并；排序：日期升序、餐次 slot 顺序、菜品 dish_id。
     * 4. 原始输入只取匹配 InputSegment.inputText；parse_method="ai"。
     */
    fun toDayMealJson(
        draft: MealStreamDraft,
        segments: List<InputSegment>,
    ): List<DayMealJson> {
        val knownSegmentIds = segments.map { it.segmentId }.toSet()
        val segmentById = segments.associateBy { it.segmentId }

        // date(string) -> (餐次按 meal_id 去重, 来源 segment 的 inputText)
        data class DayAgg(val meals: MutableMap<String, MealJson> = linkedMapOf(), var sourceInput: String = "")
        val byDate = linkedMapOf<String, DayAgg>()

        for (draftSeg in draft.segments.values) {
            if (draftSeg.segmentId !in knownSegmentIds) continue
            val sourceInput = segmentById[draftSeg.segmentId]?.inputText.orEmpty()
            for (mealNode in draftSeg.meals.values) {
                val dishes = mealNode.dishes.values
                    .filter { !it.name.isBlank() }
                    .sortedBy { it.dishId }
                if (dishes.isEmpty()) continue

                val dateStr = mealNode.date
                val agg = byDate.getOrPut(dateStr) { DayAgg() }
                agg.sourceInput = sourceInput
                agg.meals[mealNode.mealId] = toMealJson(mealNode, dishes)
            }
        }

        return byDate.map { (dateStr, agg) ->
            DayMealJson(
                date = dateStr,
                meals = agg.meals.values.sortedBy { SLOT_ORDER[it.meal_type] ?: 99 },
                raw_input = agg.sourceInput,
                parse_method = "ai",
            )
        }.sortedBy { it.date }
    }

    private fun toMealJson(mealNode: MealDraftNode, dishes: List<DishDraftNode>): MealJson =
        MealJson(
            meal_type = mealNode.slot,
            meal_time = mealNode.time,
            note = mealNode.note.orEmpty(),
            dishes = dishes.map(::toDishRef),
        )

    private fun toDishRef(dish: DishDraftNode): MealDishRefJson = MealDishRefJson(
        name = dish.name,
        ref = null,
        quantity = dish.quantity ?: 1.0,
        quantity_unit = dish.unit ?: "份",
        eaten_ratio = dish.eatenRatio,
        note = dish.note.orEmpty(),
        dish = DishJson(
            name = dish.name,
            cooking_methods = listOfNotNull(dish.cookingMethod),
            steps = dish.cookingSteps.sortedBy { it.order ?: Int.MAX_VALUE }.map { it.text },
            ingredients = toIngredients(dish),
            source = "ai",
        ),
    )

    private fun toIngredients(dish: DishDraftNode): List<DishIngredientJson> {
        val main = dish.ingredients.map { ing ->
            DishIngredientJson(
                ref = null,
                food = FoodJson(name = ing.name),
                quantity = ing.quantity ?: DEFAULT_GRAM,
                unit = ing.unit ?: DEFAULT_UNIT,
                is_main = ing.isMain ?: true,
            )
        }
        // B3: seasonings 作为 is_main=false 的食材追加，不丢失、不作主料。
        val seasonings = dish.seasonings.map { s ->
            DishIngredientJson(
                ref = null,
                food = FoodJson(name = s.name),
                quantity = s.quantity ?: DEFAULT_GRAM,
                unit = s.unit ?: DEFAULT_UNIT,
                is_main = false,
            )
        }
        return main + seasonings
    }

    private const val DEFAULT_GRAM = 100.0
    private const val DEFAULT_UNIT = "g"
}
