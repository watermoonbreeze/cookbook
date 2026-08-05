package com.sxdbsm.cookbook.ai.meallog

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

        // date(string) -> day 聚合：mealId -> (餐次元信息, dishId -> dishNode)
        data class MealAgg(
            val slot: String,
            val time: String?,
            val note: String?,
            val dishes: MutableMap<String, DishDraftNode> = linkedMapOf(),
        )
        data class DayAgg(
            val meals: MutableMap<String, MealAgg> = linkedMapOf(),
            var sourceInput: String = "",
        )
        val byDate = linkedMapOf<String, DayAgg>()

        // AF-B3-06: 按 request segments 的 ordinal 顺序遍历已知 segmentId。
        for (seg in segments.sortedBy { it.ordinal }) {
            if (seg.segmentId !in knownSegmentIds) continue
            val draftSeg = draft.segments[seg.segmentId] ?: continue
            for (mealNode in draftSeg.meals.values) {
                val nonBlankDishes = mealNode.dishes.values.filter { !it.name.isBlank() }
                if (nonBlankDishes.isEmpty()) continue
                // ordinal 最小段首次创建该 day 时回填 raw_input
                val agg = byDate.getOrPut(mealNode.date) {
                    DayAgg().also { it.sourceInput = seg.inputText }
                }
                val mealAgg = agg.meals.getOrPut(mealNode.mealId) {
                    MealAgg(mealNode.slot, mealNode.time, mealNode.note)
                }
                // 同 date+mealId：按 dishId 合并，不覆盖
                for (dish in nonBlankDishes) {
                    mealAgg.dishes[dish.dishId] = dish
                }
            }
        }

        return byDate.map { (dateStr, agg) ->
            DayMealJson(
                date = dateStr,
                meals = agg.meals.map { (_, mealAgg) ->
                    MealJson(
                        meal_type = mealAgg.slot,
                        meal_time = mealAgg.time,
                        note = mealAgg.note.orEmpty(),
                        dishes = mealAgg.dishes.values.sortedBy { it.dishId }.map(::toDishRef),
                    )
                }.sortedBy { SLOT_ORDER[it.meal_type] ?: 99 },
                raw_input = agg.sourceInput,
                parse_method = "ai",
            )
        }.sortedBy { it.date }
    }

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
