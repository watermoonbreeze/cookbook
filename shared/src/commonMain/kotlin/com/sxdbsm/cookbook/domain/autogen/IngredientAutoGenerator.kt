package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.domain.FoodGroup
import com.sxdbsm.cookbook.domain.NutritionGuess
import com.sxdbsm.cookbook.domain.NutritionGuesser
import com.sxdbsm.cookbook.domain.NutritionGuessSource
import com.sxdbsm.cookbook.domain.SeasoningDefaults
import com.sxdbsm.cookbook.domain.model.IngredientNutrition
import com.sxdbsm.cookbook.platform.ioDispatcher
import kotlinx.coroutines.withContext

/**
 * @File : IngredientAutoGenerator
 * @Time : 2026/08/01
 * @Author : SXD-AI
 * @Desc : 食材级自动生成——preview(只读·归一+dedup+classify+营养估算+单位+careFlag) / commit(建食材+营养)
 * <p>
 * 两阶段 API：preview 只读零写库（可重算无副作用），commit 落库（db.transaction 原子）。
 * 复用：createUserIngredient（已有去空格名 dedup）、upsertNutrition、NutritionGuesser、
 * SeasoningDefaults、FoodGroup.classify——全部零改。
 * <p>
 * [AI生成] 自动化基础能力层 Phase 1。
 **/
class IngredientAutoGenerator(
    private val ingredientRepo: IngredientRepository,
    private val nutritionRepo: NutritionRepository,
) {
    /**
     * 食材预览：归一→dedup→classify→营养估算→单位→careFlag。零写库。[AI生成]
     *
     * @param input 语义输入（最少只需 name）
     * @param ctx 预取字典上下文
     * @return 完整 IngredientPreview（resolution 明确 REUSE/CREATE）
     */
    suspend fun preview(
        input: SemanticIngredient,
        ctx: AutoGenContext,
    ): IngredientPreview = withContext(ioDispatcher) {
        val rawName = input.name.trim()
        if (rawName.isBlank()) {
            return@withContext IngredientPreview(
                inputName = input.name,
                normalizedName = "",
                resolution = ResolveKind.CREATE,
                existingId = null,
                groupLabel = null,
                categoryId = null,
                nutrition = NutritionGuess(null, NutritionGuessSource.None),
                quantity = SeasoningDefaults.DEFAULT_INGREDIENT_GRAMS.toDouble(),
                unitId = ctx.gramUnitId,
                careFlag = CareFlag.PENDING_REVIEW,
            )
        }

        // 1) 别名归一（蕃茄/西红柿→番茄）
        val normalized = ctx.aliasResolver.normalize(rawName)
        val nameKey = normalizeNameKey(normalized)

        // 2) dedup：查已有食材
        val existingId = ctx.ingredientNameToId[nameKey]
        if (existingId != null) {
            // REUSE：命中库内已有食材·不建重复
            val group = FoodGroup.classify(normalized)
            val categoryName = group?.let { FoodGroup.CATEGORY_NAME[it] }
            val categoryId = categoryName?.let { ctx.categoryNameToId[it] }
            // 仍跑一次营养推演（供上层展示预览营养）
            val guessedNutrition = NutritionGuesser.guess(normalized, ctx.nutritionCandidates, group)
            // isSeasoning: 在调料名集即为调料
            val isSeasoning = nameKey in ctx.seasoningNames
            val defaultQty = SeasoningDefaults.defaultGramFor(normalized, isSeasoning).toDouble()
            return@withContext IngredientPreview(
                inputName = input.name,
                normalizedName = normalized,
                resolution = ResolveKind.REUSE,
                existingId = existingId,
                groupLabel = group?.label,
                categoryId = categoryId,
                nutrition = guessedNutrition,
                quantity = input.quantity?.takeIf { it > 0 } ?: defaultQty,
                unitId = ctx.gramUnitId,
                careFlag = CareFlag.INHERITED, // 归一命中库内→继承既有 care
            )
        }

        // 3) CREATE：新食材→classify+营养估算+单位+careFlag=PENDING
        val group = FoodGroup.classify(normalized)
        val categoryName = group?.let { FoodGroup.CATEGORY_NAME[it] }
        val categoryId = categoryName?.let { ctx.categoryNameToId[it] }

        // 营养推演
        val guessedNutrition = NutritionGuesser.guess(normalized, ctx.nutritionCandidates, group)

        // 默认克数：调料用小值、普通食材按大类
        val isSeasoning = nameKey in ctx.seasoningNames || group == null
        val defaultQty = SeasoningDefaults.defaultGramFor(normalized, isSeasoning).toDouble()

        IngredientPreview(
            inputName = input.name,
            normalizedName = normalized,
            resolution = ResolveKind.CREATE,
            existingId = null,
            groupLabel = group?.label,
            categoryId = categoryId,
            nutrition = guessedNutrition,
            quantity = input.quantity?.takeIf { it > 0 } ?: defaultQty,
            unitId = ctx.gramUnitId,
            careFlag = CareFlag.PENDING_REVIEW, // 新建食材 care 留待人工复核（健康红线）
        )
    }

    /**
     * 食材入库：REUSE→直接返已有 id；CREATE→createUserIngredient+upsertNutrition。[AI生成]
     *
     * @param preview 由 [preview] 产出的预览（必先 preview 再 commit）
     * @return 食材 id
     */
    suspend fun commit(preview: IngredientPreview): Long = withContext(ioDispatcher) {
        when (preview.resolution) {
            ResolveKind.REUSE -> {
                // 已存在：直接复用已有 id
                preview.existingId ?: error("REUSE but existingId is null for ${preview.normalizedName}")
            }
            ResolveKind.CREATE -> {
                // 新建食材
                val id = ingredientRepo.createUserIngredient(
                    name = preview.normalizedName,
                    categoryId = preview.categoryId,
                    source = "auto", // autogen 建的标 auto 源·隐含估算
                )

                // 写入营养估算（缺字段留 null 不填 0）
                val nutritionValues = preview.nutrition.values
                if (nutritionValues != null) {
                    nutritionRepo.upsertNutrition(
                        IngredientNutrition(
                            ingredientId = id,
                            energyKcal = nutritionValues.energyKcal,
                            proteinG = nutritionValues.proteinG,
                            fatG = nutritionValues.fatG,
                            carbG = nutritionValues.carbG,
                            fiberG = nutritionValues.fiberG,
                            sodiumMg = nutritionValues.sodiumMg,
                            potassiumMg = nutritionValues.potassiumMg,
                            calciumMg = nutritionValues.calciumMg,
                            gi = nutritionValues.gi,
                            purineMg = nutritionValues.purineMg,
                            ref = "自动估算·待核", // 标"估算"非权威（INV-09）
                        )
                    )
                }

                id
            }
        }
    }
}
