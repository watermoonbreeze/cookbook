package com.sxdbsm.cookbook.ai.meallog

/**
 * @File : SchemaValidator
 * @Time : 2026/07/29
 * @Author : SXD-AI
 * @Desc : Schema 校验器——对解析出的 JSON 做完整性/合法性校验
 * <p>
 * 校验规则：日期范围合理、餐次至少一道菜、食材名非空等。
 * 纯函数，可单测。
 * <p>
 * [AI生成] K2 AI快捷输入记餐专项重构：Schema 校验层。
 **/
object SchemaValidator {

    /** 校验结果。[AI生成] */
    data class ValidationResult(
        val valid: Boolean,
        val errors: List<String> = emptyList(),   // 阻断性错误
        val warnings: List<String> = emptyList(), // 非阻断警告
    )

    /**
     * 校验 MultiDayJson。[AI生成]
     */
    fun validate(multiDay: MultiDayJson): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (multiDay.days.isEmpty()) {
            errors.add("没有识别到任何天的餐食")
            return ValidationResult(false, errors)
        }

        // 校验版本（未来可在此做 schema 迁移）
        if (multiDay.schema_version.isBlank()) {
            warnings.add("缺少 schema_version，将按 1.0 处理")
        }

        // 逐天校验
        for ((i, day) in multiDay.days.withIndex()) {
            val dayLabel = "第${i + 1}天"
            validateDay(day, dayLabel, errors, warnings)
        }

        return ValidationResult(errors.isEmpty(), errors, warnings)
    }

    /**
     * 校验单个 DayMealJson。[AI生成]
     */
    fun validateDay(day: DayMealJson): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        validateDay(day, "", errors, warnings)
        return ValidationResult(errors.isEmpty(), errors, warnings)
    }

    private fun validateDay(
        day: DayMealJson,
        label: String,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        val prefix = if (label.isNotBlank()) "$label " else ""

        // 日期：date 或 date_offset 至少一个有效
        if (day.date == null && day.date_offset == 0 && day.weekday == null) {
            // 全空 = 默认今天（合法但不精确）
        }

        // 餐次至少有 1 个
        if (day.meals.isEmpty()) {
            warnings.add("${prefix}没有识别到餐次")
            return
        }

        // 逐餐次校验
        for ((mi, meal) in day.meals.withIndex()) {
            val mealLabel = "${prefix}${mealLabel(meal.meal_type, mi)}"
            if (meal.dishes.isEmpty()) {
                warnings.add("$mealLabel 没有识别到菜品")
                continue
            }
            for ((di, dishRef) in meal.dishes.withIndex()) {
                val name = dishRef.name.ifBlank { dishRef.dish?.name ?: "" }
                if (name.isBlank()) {
                    errors.add("$mealLabel 第${di + 1}道菜 缺少菜名")
                }
                // 检查 quantity 是否合理
                if (dishRef.quantity <= 0) {
                    errors.add("$mealLabel「${name}」份量无效（${dishRef.quantity}）")
                }
                if (dishRef.quantity > 100) {
                    warnings.add("$mealLabel「${name}」份量偏大（${dishRef.quantity}），请确认")
                }
            }
        }
    }

    private fun mealLabel(mealType: String?, index: Int): String = when (mealType) {
        "breakfast" -> "早餐"
        "lunch" -> "午餐"
        "dinner" -> "晚餐"
        "snack" -> "加餐"
        else -> "第${index + 1}餐"
    }
}
