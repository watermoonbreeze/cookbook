package com.sxdbsm.cookbook.ai.meallog

/**
 * @File : FlatToDayMealConverter
 * @Time : 2026/08/02
 * @Author : SXD-AI
 * @Desc : AI 扁平输出 → DayMealJson 聚合转换器
 * <p>
 * AI 输出 FlatMealJson（每行一道菜·自包含全部信息），本转换器按 (date_offset, meal_type)
 * 分组聚合为嵌套 DayMealJson，供下游 MultiDayRecorder 统一处理。
 * <p>
 * 聚合规则：
 * - 同 date_offset + meal_type 的 items → 合并为一个 MealJson（多道菜）
 * - meal_note 取该组第一个 item 的值（AI prompt 约定：同餐多菜只在首道菜填）
 * - meal_time 取该组第一个 item 的值
 * - 纯函数·零副作用·可单测
 * <p>
 * [AI生成] 修复 AI 模式 Schema 不匹配：AI 输出 items(FlatMealJson) vs Parser 期望 meals(AiMealParseResult)。
 **/
object FlatToDayMealConverter {

    /**
     * FlatMealJson → List<DayMealJson>。[AI生成]
     *
     * @param flat AI 返回的扁平格式
     * @return 至少 1 个 DayMealJson（最差返回空 meals 的占位对象）
     */
    fun convert(flat: FlatMealJson): List<DayMealJson> {
        if (flat.items.isEmpty()) return emptyList()

        // 按 (date_offset, meal_type) 分组
        val groups = flat.items.groupBy { item ->
            Pair(item.date_offset, item.meal_type)
        }

        return groups.map { (key, items) ->
            val (dateOffset, mealType) = key
            val first = items.first()

            // 确定日期：优先绝对日期 date 字段
            val date = items.mapNotNull { it.date }.firstOrNull()

            DayMealJson(
                date = date,
                date_offset = dateOffset,
                meals = listOf(
                    MealJson(
                        meal_type = mealType,
                        meal_time = first.meal_time,
                        note = first.meal_note,  // 按 AI prompt 约定取首道菜的餐备注
                        dishes = items.map { item ->
                            MealDishRefJson(
                                name = item.dish_name,
                                quantity = item.dish_quantity,
                                quantity_unit = item.dish_unit,
                                eaten_ratio = item.dish_eaten_ratio,
                                note = item.dish_note,
                                dish = DishJson(
                                    name = item.dish_name,
                                    cooking_methods = item.dish_cooking_methods,
                                    tags = item.dish_tags,
                                    cuisine = item.dish_cuisine,
                                    ingredients = item.ingredients.map { ing ->
                                        DishIngredientJson(
                                            ref = ing.name,
                                            quantity = ing.quantity,
                                            unit = ing.unit,
                                            is_main = ing.is_main,
                                        )
                                    },
                                    source = "ai",
                                ),
                            )
                        },
                    )
                ),
                raw_input = "",
                parse_method = "ai",
            )
        }
    }
}
