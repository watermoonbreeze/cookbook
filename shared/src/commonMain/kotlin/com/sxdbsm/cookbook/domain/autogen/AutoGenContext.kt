package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.NutritionGuessValues
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.platform.ioDispatcher
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.withContext

/**
 * @File : AutoGenContext
 * @Time : 2026/08/01
 * @Author : SXD-AI
 * @Desc : 自动化生成器一次性预取字典——让 preview/commit 全程复用、防 N+1
 * <p>
 * 工厂 [load] 从 DB 一次性拉取所有需要反复查询的参考数据：
 * 已有食材名→id、有营养数据的食材（供 NutritionGuesser 候选）、gram 单位 id、
 * 调料食材 id 集合、餐次类型字典。
 * <p>
 * [AI生成] 自动化基础能力层 Phase 1。
 **/
class AutoGenContext(
    /** 归一名 → 食材 id（供 dedup 复用） */
    val ingredientNameToId: Map<String, Long>,
    /** 有营养数据的食材名→营养值（供 NutritionGuesser 近似命中） */
    val nutritionCandidates: List<Pair<String, NutritionGuessValues>>,
    /** "g"单位 id（兜底 gramUnit） */
    val gramUnitId: Long,
    /** 调料类食材名集合（供 SeasoningDefaults.isSeasoning 判断） */
    val seasoningNames: Set<String>,
    /** 餐次类型列表（供 DayAutoGenerator 解析 mealTypeCode） */
    val mealTypes: List<MealType>,
    /** 餐次 code → MealType */
    val mealTypeByCode: Map<String, MealType>,
    /** food_category 名 → id（供 commit 时按 FoodGroup.CATEGORY_NAME 找分类 id） */
    val categoryNameToId: Map<String, Long>,
    /** 别名解析器（供 preview 归一） */
    val aliasResolver: IngredientAliasResolver,
) {
    companion object {
        /**
         * 一次性从 DB 加载所有预取字典。[AI生成]
         *
         * @param db 数据库实例
         * @param aliasJson ingredient_aliases.json 文本（为免迁移，由调用方从 SeedResourceLoader 读取传入）
         */
        suspend fun load(
            db: CookbookDatabase,
            aliasResolver: IngredientAliasResolver,
        ): AutoGenContext = withContext(ioDispatcher) {
            val q = db.cookbookQueries

            // 已有食材名→id（归一名 key）
            val nameToId = mutableMapOf<String, Long>()
            q.selectActiveIngredientIdNames().executeAsList().forEach { row ->
                val key = normalizeNameKey(row.name)
                // 同名多 id 取第一个（老库兼容）
                if (key !in nameToId) {
                    nameToId[key] = row.id
                }
            }

            // 营养候选：有任一营养数据的食材
            val nutritionCandidates = q.selectAllIngredientNutrition().executeAsList()
                .filter { row ->
                    row.energy_kcal != null || row.protein_g != null || row.fat_g != null ||
                    row.carb_g != null || row.fiber_g != null || row.sodium_mg != null ||
                    row.potassium_mg != null || row.calcium_mg != null || row.gi != null ||
                    row.purine_mg != null
                }
                .map { row ->
                    row.name to NutritionGuessValues(
                        energyKcal = row.energy_kcal,
                        proteinG = row.protein_g,
                        fatG = row.fat_g,
                        carbG = row.carb_g,
                        fiberG = row.fiber_g,
                        sodiumMg = row.sodium_mg,
                        potassiumMg = row.potassium_mg,
                        calciumMg = row.calcium_mg,
                        gi = row.gi,
                        purineMg = row.purine_mg,
                    )
                }

            // gram 单位 id（优先"g"，兜底"克"）
            val gramUnitId = q.selectMeasurementUnitIdByName("g").executeAsOneOrNull()
                ?: q.selectMeasurementUnitIdByName("克").executeAsOneOrNull()
                ?: 1L

            // 调料食材名集（用于 SeasoningDefaults isSeasoning 判断）
            val seasoningNames = q.selectSeasoningIngredientIds().executeAsList()
                .mapNotNull { id -> nameToId.entries.firstOrNull { it.value == id }?.key }
                .toSet()

            // 餐次类型
            val mealTypes = q.selectAllMealTypes().executeAsList().map {
                MealType(
                    id = it.id,
                    code = it.code,
                    name = it.name,
                    defaultTime = DateTime.parseTime(it.default_time),
                    isFixed = it.is_fixed == 1L,
                )
            }
            val mealTypeByCode = mealTypes.associateBy { it.code }

            // food_category 名→id（供 commit 时按 Group→categoryName→categoryId 挂分类）
            val categoryNameToId = q.selectAllFoodCategories().executeAsList()
                .associate { it.name to it.id }

            AutoGenContext(
                ingredientNameToId = nameToId,
                nutritionCandidates = nutritionCandidates,
                gramUnitId = gramUnitId,
                seasoningNames = seasoningNames,
                mealTypes = mealTypes,
                mealTypeByCode = mealTypeByCode,
                categoryNameToId = categoryNameToId,
                aliasResolver = aliasResolver,
            )
        }
    }
}

/** 同 IngredientRepository.normalizeNameKey 口径：去空格+小写。包级函数供 AutoGenContext 和 IngredientAliasResolver 共用。[AI生成] */
internal fun normalizeNameKey(raw: String): String =
    raw.trim().replace(Regex("[\\s\\u3000]"), "").lowercase()
