package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.Dish
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
 * @File : MultiDayRecorder
 * @Time : 2026/07/29
 * @Author : SXD-AI
 * @Desc : 多天餐食入库编排 UseCase——逐天、逐餐、逐菜入库
 * <p>
 * 编排流程（逐天循环）：
 * Phase 0: 日期解析（date/date_offset + today → LocalDate）
 * Phase 1: 食材解析（food.name → 查已有/新建 createUserIngredient，AI 可自由创建）
 * Phase 2: 菜品解析（dish.name → 查已有 dishIdByName / saveDish(source="ai")）
 * Phase 3: 组装 Drafts + mergeWithExisting（同餐次追加）
 * Phase 4: eaten_ratio 回填
 * <p>
 * 支持 MergeMode：REPLACE（替换当天）/ APPEND（追加）/ MERGE（同餐次合并）
 * <p>
 * [AI生成] K2 AI快捷输入记餐专项重构：多天入库编排层。
 **/

/** 合并模式。[AI生成] */
enum class MergeMode { REPLACE, APPEND, MERGE }

/** 单天入库结果。[AI生成] */
data class DayRecordResult(
    val date: LocalDate,
    val mealsSaved: Int,
    val dishesCreated: Int,
    val ingredientsCreated: Int,
    val ingredientNamesCreated: List<String> = emptyList(),
    val dishNamesCreated: List<String> = emptyList(),
)

/** 多天入库汇总。[AI生成] */
data class MultiDayRecordResult(
    val days: List<DayRecordResult> = emptyList(),
    val totalMealsSaved: Int = 0,
    val totalDishesCreated: Int = 0,
    val totalIngredientsCreated: Int = 0,
) {
    val allIngredientNamesCreated: List<String> get() = days.flatMap { it.ingredientNamesCreated }
    val allDishNamesCreated: List<String> get() = days.flatMap { it.dishNamesCreated }
}

class MultiDayRecorder(
    private val ingredientRepo: IngredientRepository,
    private val dishRepo: DishRepository,
    private val mealRepo: MealRecordRepository,
) {
    /**
     * 批量入库多天餐食。[AI生成]
     *
     * @param days 待入库的多天餐食列表
     * @param today 基准日期（用于 date_offset 推算）
     * @param ingredientNames 已有食材名（供食材匹配）
     * @param mergeMode 合并模式（默认 MERGE：同餐次追加）
     * @return 逐天入库结果
     */
    suspend fun recordAll(
        days: List<DayMealJson>,
        today: LocalDate,
        ingredientNames: List<String> = emptyList(),
        mergeMode: MergeMode = MergeMode.MERGE,
    ): MultiDayRecordResult = withContext(ioDispatcher) {
        // 预取餐次类型字典
        val mealTypes = mealRepo.listMealTypes()
        val mealTypeByCode = mealTypes.associateBy { it.code }

        // dishName → dishId 全局缓存（跨天复用）
        val dishIdCache = mutableMapOf<String, Long>()

        val dayResults = mutableListOf<DayRecordResult>()
        val createdIngredientNames = mutableListOf<String>()
        val createdDishNames = mutableListOf<String>()

        for (dayJson in days) {
            val result = recordDay(
                dayJson = dayJson,
                today = today,
                mealTypes = mealTypes,
                mealTypeByCode = mealTypeByCode,
                ingredientNames = ingredientNames,
                dishIdCache = dishIdCache,
                mergeMode = mergeMode,
                createdIngredientNames = createdIngredientNames,
                createdDishNames = createdDishNames,
            )
            if (result != null) {
                dayResults.add(result)
            }
        }

        MultiDayRecordResult(
            days = dayResults,
            totalMealsSaved = dayResults.sumOf { it.mealsSaved },
            totalDishesCreated = dayResults.sumOf { it.dishesCreated },
            totalIngredientsCreated = dayResults.sumOf { it.ingredientsCreated },
        )
    }

    private suspend fun recordDay(
        dayJson: DayMealJson,
        today: LocalDate,
        mealTypes: List<MealType>,
        mealTypeByCode: Map<String, MealType>,
        ingredientNames: List<String>,
        dishIdCache: MutableMap<String, Long>,
        mergeMode: MergeMode,
        createdIngredientNames: MutableList<String>,
        createdDishNames: MutableList<String>,
    ): DayRecordResult? {
        // Phase 0: 日期解析
        val targetDate = resolveDate(today, dayJson.date, dayJson.date_offset)

        // Phase 1+2: 逐餐逐菜解析
        val drafts = mutableListOf<DayMealDraft>()
        var dishesCreated = 0
        var ingredientsCreated = 0

        for (meal in dayJson.meals) {
            val mealTypeId = resolveMealTypeId(meal.meal_type, mealTypes, mealTypeByCode)
            val mealTime = resolveMealTime(meal.meal_time, mealTypeId, mealTypes)
            val dishIds = mutableListOf<Long>()

            for (dishRef in meal.dishes) {
                val dishName = dishRef.name.ifBlank { dishRef.dish?.name ?: "" }
                if (dishName.isBlank()) continue

                val dishId = dishIdCache.getOrPut(dishName) {
                    val result = resolveDish(
                        dishRef = dishRef,
                        dishName = dishName,
                        ingredientNames = ingredientNames,
                    )
                    if (result.isNew) {
                        dishesCreated++
                        createdDishNames.add(dishName)
                    }
                    ingredientsCreated += result.ingredientsCreated
                    createdIngredientNames.addAll(result.newIngredientNames)
                    result.dishId
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
                    )
                )
            }
        }

        if (drafts.isEmpty()) return null

        // Phase 3: 合并已有记录
        val mergedDrafts = when (mergeMode) {
            MergeMode.REPLACE -> drafts
            else -> mergeWithExisting(targetDate, drafts, mealTypes)
        }

        // 保存
        val recordIds = mealRepo.saveDayMeals(
            date = targetDate,
            meals = mergedDrafts,
            bumpPreference = true,
        )

        // Phase 4: 回填 eaten_ratio
        backfillEatenRatios(targetDate, dayJson, recordIds, drafts)

        return DayRecordResult(
            date = targetDate,
            mealsSaved = mergedDrafts.size,
            dishesCreated = dishesCreated,
            ingredientsCreated = ingredientsCreated,
            ingredientNamesCreated = createdIngredientNames.toList(),
            dishNamesCreated = createdDishNames.toList(),
        )
    }

    // ═══════════════════════════════════════════════════
    // Phase 0: 日期解析
    // ═══════════════════════════════════════════════════

    private fun resolveDate(today: LocalDate, dateStr: String?, dateOffset: Int): LocalDate {
        // date 优先
        if (!dateStr.isNullOrBlank()) {
            val parts = dateStr.trim().split("-", "/")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull() ?: return today.plus(DatePeriod(days = dateOffset))
                val m = parts[1].toIntOrNull() ?: return today.plus(DatePeriod(days = dateOffset))
                val d = parts[2].toIntOrNull() ?: return today.plus(DatePeriod(days = dateOffset))
                return LocalDate(y, m, d)
            }
        }
        return today.plus(DatePeriod(days = dateOffset))
    }

    // ═══════════════════════════════════════════════════
    // Phase 1+2: 食材+菜品解析
    // ═══════════════════════════════════════════════════

    private data class DishResolution(
        val dishId: Long,
        val isNew: Boolean,
        val ingredientsCreated: Int = 0,
        val newIngredientNames: List<String> = emptyList(),
    )

    private suspend fun resolveDish(
        dishRef: MealDishRefJson,
        dishName: String,
        ingredientNames: List<String>,
    ): DishResolution {
        // 先查已有同名菜品
        val existingId = dishRepo.dishIdByName(dishName)
        if (existingId != null) return DishResolution(existingId, false)

        // 新建菜品
        val dishJson = dishRef.dish ?: DishJson(name = dishName, source = "ai")

        // 解析食材列表
        val resolvedIngredients = resolveIngredients(dishJson, dishName, ingredientNames)
        val ingredientsCreated = resolvedIngredients.newCount
        val newIngredientNames = resolvedIngredients.newNames

        val dishId = dishRepo.saveDish(
            id = 0,
            name = dishName,
            cookingMethodId = null,
            cookingMethodNames = dishJson.cooking_methods,
            specialNote = dishJson.special_note,
            description = dishJson.description,
            imagePath = "",
            thumbnailPath = "",
            tagNames = dishJson.tags,
            ingredients = resolvedIngredients.list,
            steps = emptyList(),
            source = dishJson.source.ifBlank { "ai" },
        )

        return DishResolution(dishId, true, ingredientsCreated, newIngredientNames)
    }

    private data class ResolvedIngredients(
        val list: List<DishIngredient>,
        val newCount: Int = 0,
        val newNames: List<String> = emptyList(),
    )

    private suspend fun resolveIngredients(
        dishJson: DishJson,
        dishName: String,
        ingredientNames: List<String>,
    ): ResolvedIngredients {
        var newCount = 0
        val newNames = mutableListOf<String>()

        // AI 提供了食材列表→直接使用
        if (dishJson.ingredients.isNotEmpty()) {
            val list = dishJson.ingredients.map { diJson ->
                val name = diJson.ref ?: diJson.food?.name ?: ""
                if (name.isBlank()) {
                    DishIngredient(
                        ingredient = Ingredient(id = 0, name = ""),
                        quantity = diJson.quantity,
                        unitId = null,
                        isMain = diJson.is_main,
                    )
                } else {
                    // [AI生成] 按名查已有→复用；无→新建（AI 不受食材库限制）
                    val id = ingredientRepo.createUserIngredient(
                        name = name,
                        source = "ai",
                    )
                    // 判断是否新建（ID 之前是否已存在需要对比，简化处理：source=ai 即为可能新建）
                    if (id > 0) {
                        newCount++
                        newNames.add(name)
                    }
                    DishIngredient(
                        ingredient = Ingredient(id = id, name = name, defaultUnitId = null),
                        quantity = diJson.quantity,
                        unitId = null,
                        isMain = diJson.is_main,
                    )
                }
            }
            return ResolvedIngredients(list, newCount, newNames)
        }

        // 无食材→菜名推演
        val guessed = com.sxdbsm.cookbook.domain.DishNameIngredientGuesser.guessDetailed(dishName, ingredientNames)
        if (guessed.isEmpty()) return ResolvedIngredients(emptyList())

        val list = guessed.map { g ->
            val id = ingredientRepo.createUserIngredient(
                name = g.name,
                source = if (g.inLibrary) "user" else "ai",
            )
            if (!g.inLibrary) {
                newCount++
                newNames.add(g.name)
            }
            DishIngredient(
                ingredient = Ingredient(id = id, name = g.name, defaultUnitId = null),
                quantity = 100.0,
                unitId = null,
                isMain = true,
            )
        }
        return ResolvedIngredients(list, newCount, newNames)
    }

    // ═══════════════════════════════════════════════════
    // 餐次类型/时间解析
    // ═══════════════════════════════════════════════════

    private fun resolveMealTypeId(
        mealType: String?,
        mealTypes: List<MealType>,
        mealTypeByCode: Map<String, MealType>,
    ): Long {
        val code = mealType?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        if (code != null) {
            mealTypeByCode[code]?.let { return it.id }
        }
        // 回退：午餐
        val lunch = mealTypes.firstOrNull { it.code == "LUNCH" }
        if (lunch != null) return lunch.id
        return mealTypes.firstOrNull()?.id ?: 1L
    }

    private fun resolveMealTime(
        mealTime: String?,
        mealTypeId: Long,
        mealTypes: List<MealType>,
    ): LocalTime {
        if (!mealTime.isNullOrBlank()) {
            val parts = mealTime.trim().split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                if (h != null && m != null && h in 0..23 && m in 0..59) {
                    return LocalTime(h, m)
                }
            }
        }
        return mealTypes.firstOrNull { it.id == mealTypeId }?.defaultTime ?: LocalTime(12, 0)
    }

    // ═══════════════════════════════════════════════════
    // Phase 3: 合并已有记录
    // ═══════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════
    // Phase 4: 回填 eaten_ratio
    // ═══════════════════════════════════════════════════

    private suspend fun backfillEatenRatios(
        date: LocalDate,
        dayJson: DayMealJson,
        recordIds: List<Long>,
        drafts: List<DayMealDraft>,
    ) {
        val ratioMap = mutableMapOf<Pair<Long, String>, Double>()
        dayJson.meals.forEachIndexed { index, meal ->
            val mealTypeId = drafts.getOrNull(index)?.mealTypeId ?: return@forEachIndexed
            for (dish in meal.dishes) {
                val name = dish.name.ifBlank { dish.dish?.name ?: "" }
                dish.eaten_ratio?.let { ratio ->
                    if (ratio != 1.0) {
                        ratioMap[mealTypeId to name] = ratio
                    }
                }
            }
        }
        if (ratioMap.isEmpty()) return

        val meals = mealRepo.loadDayMealsForEdit(date)
        for (meal in meals) {
            for (dish in meal.dishes) {
                ratioMap[meal.mealTypeId to dish.name]?.let { ratio ->
                    mealRepo.setEatenRatio(meal.mealRecordId, dish.id, ratio)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // 已有餐食冲突检查（供 UI 层调用）
    // ═══════════════════════════════════════════════════

    /** 检查指定日期是否已有餐食记录。[AI生成] */
    suspend fun hasExistingMeals(date: LocalDate): Boolean {
        return mealRepo.loadDayMealsForEdit(date).isNotEmpty()
    }

    /** 批量检查多天是否有已有餐食。[AI生成] */
    suspend fun findConflicts(
        dates: List<LocalDate>,
    ): Map<LocalDate, Boolean> {
        return dates.associateWith { hasExistingMeals(it) }
    }
}
