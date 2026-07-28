package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.DishNameIngredientGuesser
import com.sxdbsm.cookbook.domain.FoodGroup
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.platform.ioDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/**
 * @File : AiMealRecorder
 * @Time : 2026/07/28
 * @Author : SXD-AI
 * @Desc : AI 快捷输入记餐入库编排 UseCase
 * <p>
 * 编排流程：
 * Phase 0: 日期解析（today + offset）
 * Phase 1: 食材解析（AI提供→直接用；否则 DishNameIngredientGuesser 推演→自动创建）
 * Phase 2: 菜品解析（库中已有→复用；否则新建 source="ai"）
 * Phase 3: 组装 Drafts + 合并已有记录（D4：同餐次追加）→ saveDayMeals
 * Phase 4: 回填 eaten_ratio（非默认值）
 * <p>
 * [AI生成] K1 AI快捷输入记餐：入库编排层。
 **/
class AiMealRecorder(
    private val ingredientRepo: IngredientRepository,
    private val dishRepo: DishRepository,
    private val mealRepo: MealRecordRepository,
) {
    /**
     * 记录 AI 解析结果。[AI生成]
     *
     * @param parsed AI 解析结果
     * @param today 当前日期（用于偏移计算）
     * @param ingredientNames 已有食材名（供 DishNameIngredientGuesser 匹配）
     * @return 记录结果：保存成功的结果信息
     */
    suspend fun record(
        parsed: AiMealParseResult,
        today: LocalDate,
        ingredientNames: List<String> = emptyList(),
    ): RecordResult = withContext(ioDispatcher) {
        // Phase 0: 解析目标日期
        val targetDate = resolveDate(today, parsed.date_offset)

        // 预取餐次类型字典（用于 meal_type code → id 映射）
        val mealTypes = mealRepo.listMealTypes()
        val mealTypeByCode = mealTypes.associateBy { it.code }

        // Phase 1+2: 逐道菜解析食材→创建/复用菜品
        // dishName → dishId 缓存（同餐不同菜复用）
        val dishIdCache = mutableMapOf<String, Long>()

        // 组装每餐的 DayMealDraft
        val drafts = mutableListOf<DayMealDraft>()

        for (meal in parsed.meals) {
            val mealTypeId = resolveMealTypeId(meal.meal_type, mealTypes, mealTypeByCode)
            val mealTime = resolveMealTime(meal.meal_time, mealTypeId, mealTypes)
            val dishIds = mutableListOf<Long>()

            for (dish in meal.dishes) {
                val dishId = dishIdCache.getOrPut(dish.name) {
                    resolveDish(
                        dish = dish,
                        ingredientNames = ingredientNames,
                    )
                }
                if (dishId > 0) dishIds.add(dishId)
            }

            if (dishIds.isNotEmpty()) {
                drafts.add(
                    DayMealDraft(
                        mealTypeId = mealTypeId,
                        mealTime = mealTime,
                        note = meal.note,
                        dishIds = dishIds,
                    ),
                )
            }
        }

        if (drafts.isEmpty()) return@withContext RecordResult(mealsSaved = 0, dishesCreated = 0, targetDate = targetDate)

        // Phase 3: 合并已有记录（D4：同餐次追加）
        val mergedDrafts = mergeWithExisting(targetDate, drafts, mealTypes)

        // 保存
        val recordIds = mealRepo.saveDayMeals(date = targetDate, meals = mergedDrafts, bumpPreference = true)

        // Phase 4: 回填 eaten_ratio（非默认值）
        backfillEatenRatios(targetDate, parsed, recordIds, drafts)

        RecordResult(
            mealsSaved = mergedDrafts.size,
            dishesCreated = dishIdCache.count { (_, id) -> id > 0 },
            targetDate = targetDate,
        )
    }

    /** Phase 0: 日期偏移 → LocalDate。[AI生成] */
    private fun resolveDate(today: LocalDate, offset: Int): LocalDate {
        return when (offset) {
            -2 -> today.plus(DatePeriod(days = -2))
            -1 -> today.plus(DatePeriod(days = -1))
            1 -> today.plus(DatePeriod(days = 1))
            else -> today
        }
    }

    /** 解析 meal_type → meal_type_id。[AI生成] */
    private fun resolveMealTypeId(
        mealType: String?,
        mealTypes: List<MealType>,
        mealTypeByCode: Map<String, MealType>,
    ): Long {
        // AI 返回的 meal_type 是小写 ("breakfast")，DB code 是大写 ("BREAKFAST")
        val code = mealType?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        if (code != null) {
            mealTypeByCode[code]?.let { return it.id }
        }
        // 回退：按当前时间推断餐次
        return inferMealTypeByTime(mealTypes)
    }

    /** 按当前时间推断餐次。[AI生成] */
    private fun inferMealTypeByTime(mealTypes: List<MealType>): Long {
        // 用一个合理默认：优先午餐（最常见记餐时段）
        val lunch = mealTypes.firstOrNull { it.code == "LUNCH" }
        if (lunch != null) return lunch.id
        // 再回退到第一个可用餐次
        return mealTypes.firstOrNull()?.id ?: 1L
    }

    /** 解析时间 → LocalTime。[AI生成] */
    private fun resolveMealTime(
        mealTime: String?,
        mealTypeId: Long,
        mealTypes: List<MealType>,
    ): LocalTime {
        // AI 提供了具体时间
        if (!mealTime.isNullOrBlank()) {
            val parts = mealTime.trim().split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: return mealTypeDefaultTime(mealTypeId, mealTypes)
                val m = parts[1].toIntOrNull() ?: 0
                if (h in 0..23 && m in 0..59) return LocalTime(h, m)
            }
        }
        // 回退：餐次默认时间
        return mealTypeDefaultTime(mealTypeId, mealTypes)
    }

    private fun mealTypeDefaultTime(mealTypeId: Long, mealTypes: List<MealType>): LocalTime =
        mealTypes.firstOrNull { it.id == mealTypeId }?.defaultTime ?: LocalTime(12, 0)

    /**
     * Phase 1+2: 解析一道菜（食材创建 + 菜品创建/复用）。[AI生成]
     *
     * @return dishId（>0 成功，<=0 失败）
     */
    private suspend fun resolveDish(
        dish: AiParsedDish,
        ingredientNames: List<String>,
    ): Long {
        // 先查已有同名菜品
        val existingId = dishRepo.dishIdByName(dish.name)
        if (existingId != null) return existingId

        // 解析食材列表
        val resolvedIngredients = resolveIngredients(dish, ingredientNames)

        // 新建菜品（source="ai"）
        return dishRepo.saveDish(
            id = 0,
            name = dish.name,
            cookingMethodId = null,
            cookingMethodNames = dish.cooking_methods,
            specialNote = dish.note,
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = resolvedIngredients,
            steps = emptyList(),
            source = "ai", // [AI生成] AI 创建的菜品标 ai 源
        )
    }

    /**
     * Phase 1: 解析食材列表。[AI生成]
     *
     * AI 提供了食材→直接建；否则用 DishNameIngredientGuesser 从菜名推演。
     */
    private suspend fun resolveIngredients(
        dish: AiParsedDish,
        ingredientNames: List<String>,
    ): List<DishIngredient> {
        // AI 提供了食材→直接创建
        if (dish.ingredients.isNotEmpty()) {
            return dish.ingredients.map { aiIng ->
                val id = ingredientRepo.createUserIngredient(
                    name = aiIng.name,
                    source = "ai", // [AI生成] AI 创建的食材标 ai 源
                )
                DishIngredient(
                    ingredient = Ingredient(
                        id = id,
                        name = aiIng.name,
                        defaultUnitId = null,
                    ),
                    quantity = aiIng.quantity,
                    unitId = null, // 默认克，saveDish 会回填
                    isMain = aiIng.is_main,
                )
            }
        }

        // AI 无食材→菜名推演
        val guessed = DishNameIngredientGuesser.guessDetailed(dish.name, ingredientNames)
        if (guessed.isEmpty()) return emptyList()

        return guessed.map { g ->
            val id = ingredientRepo.createUserIngredient(
                name = g.name,
                source = if (g.inLibrary) "user" else "ai", // [AI生成] 库外新建标 ai 源；库内保持默认 user
            )
            DishIngredient(
                ingredient = Ingredient(
                    id = id,
                    name = g.name,
                    defaultUnitId = null,
                ),
                quantity = if (g.inLibrary) 100.0 else 100.0, // 默认 100g
                unitId = null,
                isMain = true,
            )
        }
    }

    /**
     * Phase 3: D4 合并已有记录——同餐次追加菜。[AI生成]
     *
     * 当天该餐次已有记录时，新菜追加到已有块，不新建第二条同餐次记录。
     */
    private suspend fun mergeWithExisting(
        date: LocalDate,
        newDrafts: List<DayMealDraft>,
        mealTypes: List<MealType>,
    ): List<DayMealDraft> {
        val existingMeals = mealRepo.loadDayMealsForEdit(date)
        if (existingMeals.isEmpty()) return newDrafts

        val merged = existingMeals.map { rec ->
            DayMealDraft(
                mealTypeId = rec.mealTypeId,
                mealTime = rec.mealTime,
                note = rec.note,
                dishIds = rec.dishes.map { it.id },
            )
        }.toMutableList()

        for (new in newDrafts) {
            val idx = merged.indexOfFirst { it.mealTypeId == new.mealTypeId }
            if (idx >= 0) {
                // D4：同餐次追加
                val existing = merged[idx]
                val combinedDishIds = (existing.dishIds + new.dishIds).distinct()
                val combinedNote = if (new.note.isNotBlank() && existing.note.isNotBlank()) {
                    "${existing.note}；${new.note}"
                } else {
                    existing.note.ifBlank { new.note }
                }
                merged[idx] = existing.copy(dishIds = combinedDishIds, note = combinedNote)
            } else {
                merged.add(new)
            }
        }

        return merged
    }

    /**
     * Phase 4: 回填 eaten_ratio。[AI生成]
     *
     * AI 可能为特定菜标记了"吃了一半"等非默认食用比例，需要在保存后回填。
     */
    private suspend fun backfillEatenRatios(
        date: LocalDate,
        parsed: AiMealParseResult,
        recordIds: List<Long>,
        drafts: List<DayMealDraft>,
    ) {
        // 构建 (mealTypeId, dishName) → eaten_ratio 映射
        val ratioMap = mutableMapOf<Pair<Long, String>, Double>()
        for (meal in parsed.meals) {
            val mealTypeId = drafts.firstOrNull()?.mealTypeId ?: continue
            for (dish in meal.dishes) {
                dish.eaten_ratio?.let { ratio ->
                    if (ratio != 1.0) {
                        ratioMap[mealTypeId to dish.name] = ratio
                    }
                }
            }
        }
        if (ratioMap.isEmpty()) return

        // 重新 load 当天餐食，按菜名匹配回填
        val meals = mealRepo.loadDayMealsForEdit(date)
        for (meal in meals) {
            for (dish in meal.dishes) {
                ratioMap[meal.mealTypeId to dish.name]?.let { ratio ->
                    mealRepo.setEatenRatio(meal.mealRecordId, dish.id, ratio)
                }
            }
        }
    }

    /**
     * 记录结果。[AI生成]
     */
    data class RecordResult(
        val mealsSaved: Int,
        val dishesCreated: Int,
        val targetDate: LocalDate,
    )
}
