package com.sxdbsm.cookbook.ai.meallog

/**
 * @File : SchemaMigration
 * @Time : 2026/07/29
 * @Author : SXD-AI
 * @Desc : 新旧 Schema 互转兼容层
 * <p>
 * - toDayMealJson(AiMealParseResult) → DayMealJson（旧→新）
 * - toAiMealParseResult(DayMealJson) → AiMealParseResult（新→旧，用于旧预览 UI 过渡）
 * 纯函数，无副作用。
 * <p>
 * [AI生成] K2 AI快捷输入记餐专项重构：Schema 迁移兼容层。
 **/
object SchemaMigration {

    /** 旧 AiMealParseResult → 新 DayMealJson（单天）。[AI生成] */
    fun toDayMealJson(old: AiMealParseResult): DayMealJson {
        return DayMealJson(
            date = null,
            date_offset = old.date_offset,
            meals = old.meals.map { meal ->
                MealJson(
                    meal_type = meal.meal_type,
                    meal_time = meal.meal_time,
                    note = meal.note,
                    dishes = meal.dishes.map { dish ->
                        MealDishRefJson(
                            name = dish.name,
                            quantity = dish.quantity,
                            quantity_unit = dish.quantity_unit,
                            eaten_ratio = dish.eaten_ratio,
                            note = dish.note,
                            dish = if (dish.ingredients.isNotEmpty() || dish.cooking_methods.isNotEmpty()) {
                                DishJson(
                                    name = dish.name,
                                    cooking_methods = dish.cooking_methods,
                                    ingredients = dish.ingredients.map { aiIng ->
                                        DishIngredientJson(
                                            ref = aiIng.name,
                                            quantity = aiIng.quantity,
                                            unit = aiIng.unit,
                                            is_main = aiIng.is_main,
                                        )
                                    },
                                    source = "ai",
                                )
                            } else null,
                        )
                    },
                )
            },
            raw_input = "",
            parse_method = "ai",
        )
    }

    /** 新 DayMealJson → 旧 AiMealParseResult（反向兼容）。[AI生成] */
    fun toAiMealParseResult(new: DayMealJson): AiMealParseResult {
        return AiMealParseResult(
            date_offset = new.date_offset,
            meals = new.meals.map { meal ->
                AiParsedMeal(
                    meal_type = meal.meal_type,
                    meal_time = meal.meal_time,
                    note = meal.note,
                    dishes = meal.dishes.map { dishRef ->
                        AiParsedDish(
                            name = dishRef.name.ifBlank { dishRef.dish?.name ?: "" },
                            quantity = dishRef.quantity,
                            quantity_unit = dishRef.quantity_unit,
                            eaten_ratio = dishRef.eaten_ratio,
                            cooking_methods = dishRef.dish?.cooking_methods ?: emptyList(),
                            note = dishRef.note,
                            ingredients = dishRef.dish?.ingredients?.map { di ->
                                AiParsedIngredient(
                                    name = di.ref ?: di.food?.name ?: "",
                                    quantity = di.quantity,
                                    unit = di.unit,
                                    is_main = di.is_main,
                                )
                            } ?: emptyList(),
                        )
                    },
                )
            },
        )
    }
}
