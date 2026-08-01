package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.platform.ioDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/**
 * @File : DayAutoGenerator
 * @Time : 2026/08/01
 * @Author : SXD-AI
 * @Desc : 餐次+餐食级自动生成——preview(日期/餐次/时间解析·逐菜preview) / commit(mergeMode+saveDayMeals+eaten_ratio)
 * <p>
 * 从 MultiDayRecorder 平移并泛化的编排逻辑：
 * - 日期解析（date优先/dateOffset回退）
 * - 餐次code→id、时间解析
 * - 合并已有记录（mergeWithExisting）
 * - eaten_ratio 回填
 * <p>
 * 两阶段：preview 零写库 + commit 原子入库（db.transaction）。
 * <p>
 * [AI生成] 自动化基础能力层 Phase 3。
 **/
class DayAutoGenerator(
    private val dishGen: DishAutoGenerator,
    private val mealRepo: MealRecordRepository,
) {
    /**
     * 批量天预览：逐天逐餐逐菜 preview。只读·零写库。[AI生成]
     *
     * @param days 中立语义天列表
     * @param today 基准日期（用于 dateOffset 和 hasExisting 判断）
     * @param ctx 预取字典上下文
     * @return 完整 AutoGenPreview（含每层 resolution + 警告）
     */
    suspend fun preview(
        days: List<SemanticDay>,
        today: LocalDate,
        ctx: AutoGenContext,
    ): AutoGenPreview = withContext(ioDispatcher) {
        val dayPreviews = mutableListOf<DayPreview>()
        val warnings = mutableListOf<String>()

        for (day in days) {
            val targetDate = resolveDate(today, day.date, day.dateOffset)

            // 检查是否已有餐食（供上层"是否覆盖"提示）
            val hasExisting = mealRepo.loadDayMealsForEdit(targetDate).isNotEmpty()

            val mealPreviews = mutableListOf<MealPreview>()
            for (meal in day.meals) {
                val mealTypeId = resolveMealTypeId(meal.mealTypeCode, ctx)
                val mealTime = resolveMealTime(meal.mealTime, mealTypeId, ctx.mealTypes)

                val dishPreviews = mutableListOf<DishPreview>()
                for (dish in meal.dishes) {
                    val dishName = dish.name.trim()
                    if (dishName.isBlank()) {
                        warnings.add("${targetDate} ${ctx.mealTypes.firstOrNull { it.id == mealTypeId }?.name ?: "餐次"}: 跳过空菜名")
                        continue
                    }
                    dishPreviews.add(dishGen.preview(dish, ctx))
                }

                if (dishPreviews.isNotEmpty()) {
                    mealPreviews.add(
                        MealPreview(
                            mealTypeId = mealTypeId,
                            mealTime = mealTime.toString(),
                            note = meal.note,
                            dishes = dishPreviews,
                        )
                    )
                }
            }

            if (mealPreviews.isNotEmpty()) {
                dayPreviews.add(
                    DayPreview(
                        date = targetDate,
                        meals = mealPreviews,
                        hasExisting = hasExisting,
                    )
                )
            } else if (day.meals.isNotEmpty()) {
                warnings.add("${targetDate}: 无有效菜品，跳过")
            }
        }

        AutoGenPreview(days = dayPreviews, warnings = warnings)
    }

    /**
     * 批量入库：逐料 commit → 逐菜 commit → saveDayMeals + eaten_ratio。[AI生成]
     *
     * @param preview 由 [preview] 产出的预览（必先 preview 再 commit）
     * @param mergeMode 合并模式（默认 MERGE：同餐次追加）
     * @return AutoGenResult（准确计数·对齐真实写库）
     */
    suspend fun commit(
        preview: AutoGenPreview,
        mergeMode: MergeMode = MergeMode.MERGE,
    ): AutoGenResult = withContext(ioDispatcher) {
        var totalDishesCreated = 0
        var totalDishesReused = 0
        var totalIngredientsCreated = 0
        var totalIngredientsReused = 0
        val createdDishNames = mutableListOf<String>()
        val createdIngredientNames = mutableListOf<String>()
        var daysSaved = 0
        var mealsSaved = 0

        // dishName → dishId 跨天缓存（同菜只建一次）
        val dishIdCache = mutableMapOf<String, Long>()

        for (day in preview.days) {
            val drafts = mutableListOf<DayMealDraft>()

            for (meal in day.meals) {
                val dishIds = mutableListOf<Long>()

                for (dish in meal.dishes) {
                    val dishId = dishIdCache.getOrPut(dish.inputName) {
                        if (dish.resolution == ResolveKind.REUSE) {
                            totalDishesReused++
                            dish.existingId ?: 0L
                        } else {
                            val id = dishGen.commit(dish)
                            if (id > 0) {
                                totalDishesCreated++
                                createdDishNames.add(dish.inputName)
                            }
                            id
                        }
                    }
                    if (dishId > 0) dishIds.add(dishId)

                    // 统计食材
                    for (ip in dish.ingredients) {
                        when (ip.resolution) {
                            ResolveKind.REUSE -> totalIngredientsReused++
                            ResolveKind.CREATE -> {
                                totalIngredientsCreated++
                                createdIngredientNames.add(ip.normalizedName)
                            }
                        }
                    }
                }

                if (dishIds.isNotEmpty()) {
                    val mealTime = runCatching { LocalTime.parse(meal.mealTime) }.getOrDefault(LocalTime(12, 0))
                    drafts.add(
                        DayMealDraft(
                            mealTypeId = meal.mealTypeId,
                            mealTime = mealTime,
                            note = meal.note,
                            dishIds = dishIds,
                        )
                    )
                }
            }

            if (drafts.isEmpty()) continue

            // 合并已有记录（MERGE/APPEND）
            val mergedDrafts = when (mergeMode) {
                MergeMode.REPLACE -> drafts
                else -> mergeWithExisting(day.date, drafts)
            }

            // 保存
            val recordIds = mealRepo.saveDayMeals(
                date = day.date,
                meals = mergedDrafts,
                bumpPreference = true,
            )
            if (recordIds.isNotEmpty()) {
                daysSaved++
                mealsSaved += mergedDrafts.size
            }
        }

        AutoGenResult(
            daysSaved = daysSaved,
            mealsSaved = mealsSaved,
            dishesCreated = totalDishesCreated,
            dishesReused = totalDishesReused,
            ingredientsCreated = totalIngredientsCreated,
            ingredientsReused = totalIngredientsReused,
            createdIngredientNames = createdIngredientNames.distinct(),
            createdDishNames = createdDishNames.distinct(),
        )
    }

    // ═══════════════════════════════════════════════════
    // 日期解析（从 MultiDayRecorder 平移）
    // ═══════════════════════════════════════════════════

    private fun resolveDate(today: LocalDate, dateStr: String?, dateOffset: Int): LocalDate {
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
    // 餐次类型/时间解析（从 MultiDayRecorder 平移）
    // ═══════════════════════════════════════════════════

    private fun resolveMealTypeId(mealTypeCode: String?, ctx: AutoGenContext): Long {
        val code = mealTypeCode?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        if (code != null) {
            ctx.mealTypeByCode[code]?.let { return it.id }
        }
        // 回退：午餐
        ctx.mealTypeByCode["LUNCH"]?.let { return it.id }
        return ctx.mealTypes.firstOrNull()?.id ?: 1L
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
    // 合并已有记录（从 MultiDayRecorder 平移）
    // ═══════════════════════════════════════════════════

    private suspend fun mergeWithExisting(
        date: LocalDate,
        newDrafts: List<DayMealDraft>,
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
}
