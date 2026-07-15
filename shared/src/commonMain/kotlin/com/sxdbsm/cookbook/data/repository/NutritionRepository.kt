package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.DishNutrition
import com.sxdbsm.cookbook.domain.model.IngredientNutrition
import com.sxdbsm.cookbook.domain.model.NutritionCalculator
import com.sxdbsm.cookbook.domain.model.NutritionInput
import com.sxdbsm.cookbook.domain.model.NutritionTotals
import com.sxdbsm.cookbook.platform.ioDispatcher
import kotlinx.coroutines.withContext

/**
 * @File : NutritionRepository
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 营养素读取与菜品/餐/日营养估算（L2 数值层的数据层入口）
 * <p>
 * 把 dish_ingredient 用量 + measurement_unit 克当量 + ingredient_nutrition 每100g 值，
 * 交给 NutritionCalculator 折算成菜品营养；上层(菜品详情/餐食/首页)按需消费。
 * <p>
 * [AI生成] 营养素 L2 数值层数据层。
 **/
class NutritionRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries

    /** 读取单个食材每100g营养(无数据返回 null)。[AI生成] */
    suspend fun ingredientNutrition(ingredientId: Long): IngredientNutrition? = withContext(ioDispatcher) {
        q.selectIngredientNutrition(ingredientId).executeAsOneOrNull()?.let { r ->
            IngredientNutrition(
                ingredientId = r.ingredient_id,
                energyKcal = r.energy_kcal,
                proteinG = r.protein_g,
                fatG = r.fat_g,
                carbG = r.carb_g,
                fiberG = r.fiber_g,
                sodiumMg = r.sodium_mg,
                potassiumMg = r.potassium_mg,
                calciumMg = r.calcium_mg,
                gi = r.gi,
                purineMg = r.purine_mg,
                pieceGram = r.piece_gram,
                ref = r.ref,
                review = r.review == 1L,
            )
        }
    }

    /** 写入/更新食材营养(自定义食材填的每100g值)。[AI生成] Item4 */
    suspend fun upsertNutrition(n: IngredientNutrition) = withContext(ioDispatcher) {
        q.upsertIngredientNutrition(
            ingredient_id = n.ingredientId,
            energy_kcal = n.energyKcal,
            protein_g = n.proteinG,
            fat_g = n.fatG,
            carb_g = n.carbG,
            fiber_g = n.fiberG,
            sodium_mg = n.sodiumMg,
            potassium_mg = n.potassiumMg,
            calcium_mg = n.calciumMg,
            gi = n.gi,
            purine_mg = n.purineMg,
            piece_gram = n.pieceGram,
            ref = if (n.ref.isBlank()) "用户填写" else n.ref,
            review = 0L,
            updated_at = com.sxdbsm.cookbook.util.DateTime.nowEpochSeconds(),
        )
    }

    /** 是否有任何量化营养数据(用于"数据不全"提示)。[AI生成] */
    suspend fun hasNutrition(ingredientId: Long): Boolean = ingredientNutrition(ingredientId)?.hasAny == true

    /** 全量食材+营养(左连，供"食材营养表"页)。[AI生成] */
    suspend fun allIngredientNutrition(): List<com.sxdbsm.cookbook.domain.model.IngredientNutritionRow> = withContext(ioDispatcher) {
        q.selectAllIngredientNutrition().executeAsList().map { r ->
            com.sxdbsm.cookbook.domain.model.IngredientNutritionRow(
                name = r.name, foodGroup = r.food_group,
                kcal = r.energy_kcal, protein = r.protein_g, fat = r.fat_g, carb = r.carb_g, fiber = r.fiber_g,
                sodium = r.sodium_mg, potassium = r.potassium_mg, calcium = r.calcium_mg, gi = r.gi, purine = r.purine_mg,
            )
        }
    }

    /**
     * 批量估算多道菜的营养。[AI生成]
     *
     * @return dishId -> DishNutrition（含覆盖率/是否估算）。查询不到配料的菜返回空营养。
     */
    suspend fun dishNutrition(dishIds: List<Long>): Map<Long, DishNutrition> = withContext(ioDispatcher) {
        if (dishIds.isEmpty()) return@withContext emptyMap()
        val rows = q.selectNutritionInputsByDishIds(dishIds).executeAsList()
        val byDish = rows.groupBy { it.dish_id }
        dishIds.associateWith { id ->
            val inputs = byDish[id].orEmpty().map { r ->
                val nutrition = if (
                    r.energy_kcal != null || r.protein_g != null || r.fat_g != null || r.carb_g != null ||
                    r.fiber_g != null || r.sodium_mg != null || r.potassium_mg != null ||
                    r.calcium_mg != null || r.purine_mg != null || r.piece_gram != null
                ) {
                    IngredientNutrition(
                        ingredientId = r.ingredient_id,
                        energyKcal = r.energy_kcal,
                        proteinG = r.protein_g,
                        fatG = r.fat_g,
                        carbG = r.carb_g,
                        fiberG = r.fiber_g,
                        sodiumMg = r.sodium_mg,
                        potassiumMg = r.potassium_mg,
                        calciumMg = r.calcium_mg,
                        gi = r.gi,
                        purineMg = r.purine_mg,
                        pieceGram = r.piece_gram,
                    )
                } else {
                    null
                }
                NutritionInput(quantity = r.quantity, unitGrams = r.unit_grams, nutrition = nutrition)
            }
            NutritionCalculator.dishNutrition(inputs)
        }
    }

    /** 多道菜营养总和(一餐/一天)。[AI生成] */
    suspend fun totalOf(dishIds: List<Long>): NutritionTotals = withContext(ioDispatcher) {
        NutritionCalculator.sumTotals(dishNutrition(dishIds).values.map { it.totals })
    }
}
