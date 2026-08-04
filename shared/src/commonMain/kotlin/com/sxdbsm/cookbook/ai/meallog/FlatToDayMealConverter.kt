package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

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

    /** 转换阶段的可见诊断；错误会阻断 AI 结果进入预览。 [AI修改] */
    data class ConversionResult(
        val days: List<DayMealJson>,
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList(),
    )

    /**
     * FlatMealJson → List<DayMealJson>。[AI生成]
     *
     * @param flat AI 返回的扁平格式
     * @return 至少 1 个 DayMealJson（最差返回空 meals 的占位对象）
     */
    fun convert(flat: FlatMealJson, fallbackDate: LocalDate): ConversionResult {
        if (flat.items.isEmpty()) return ConversionResult(emptyList())
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val byDate = linkedMapOf<LocalDate, MutableList<FlatMealItem>>()

        flat.items.forEachIndexed { index, item ->
            val resolvedDate = resolveDate(item, fallbackDate, index, warnings, errors)
            byDate.getOrPut(resolvedDate) { mutableListOf() }.add(item)
        }

        val days = byDate.map { (date, items) ->
            val meals = items.groupBy { it.meal_type }
                .map { (mealType, mealItems) ->
                    val first = mealItems.first()
                    MealJson(
                        meal_type = mealType,
                        meal_time = first.meal_time,
                        note = first.meal_note,
                        dishes = mealItems.map(::toDishRef),
                    )
                }
            DayMealJson(
                date = date.toString(),
                meals = meals,
                weekday = items.first().weekday,
                raw_input = "",
                parse_method = "ai",
            )
        }
        return ConversionResult(days, warnings.distinct(), errors.distinct())
    }

    /** 兼容旧调用；新 AI 链路必须显式传入目标日期。 [AI修改] */
    fun convert(flat: FlatMealJson): List<DayMealJson> = convert(flat, LocalDate(1970, 1, 1)).days

    private fun resolveDate(
        item: FlatMealItem,
        fallbackDate: LocalDate,
        index: Int,
        warnings: MutableList<String>,
        errors: MutableList<String>,
    ): LocalDate {
        val rawDate = item.date?.trim().orEmpty()
        if (rawDate.isNotBlank()) {
            return runCatching { LocalDate.parse(rawDate.replace('/', '-')) }.getOrElse {
                errors += "第${index + 1}道菜的日期「$rawDate」无效"
                fallbackDate
            }
        }
        if (item.date_offset != 0) return fallbackDate.plus(DatePeriod(days = item.date_offset))
        warnings += "第${index + 1}道菜未给出日期，已按当前选择日期记录"
        return fallbackDate
    }

    private fun toDishRef(item: FlatMealItem) = MealDishRefJson(
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
                DishIngredientJson(ref = ing.name, quantity = ing.quantity, unit = ing.unit, is_main = ing.is_main)
            },
            source = "ai",
        ),
    )
}
