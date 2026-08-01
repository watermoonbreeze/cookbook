package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.domain.DishNameIngredientGuesser
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.platform.ioDispatcher
import kotlinx.coroutines.withContext

/**
 * @File : DishAutoGenerator
 * @Time : 2026/08/01
 * @Author : SXD-AI
 * @Desc : 菜品级自动生成——preview(dishIdByName dedup + 逐料preview + 默认克数) / commit(建食材后 saveDish)
 * <p>
 * 两阶段：preview 只读 + commit 写库(db.transaction)。关键不变量：
 * - 建菜配料有量→热量>0（杜绝"0千卡"·INV-02）
 * - dish_ingredient 落库 unitId 非 NULL（saveDish 回填 gramUnit·INV-03）
 * - 空料→DishNameIngredientGuesser 推演（复用现有能力）
 * <p>
 * [AI生成] 自动化基础能力层 Phase 2。
 **/
class DishAutoGenerator(
    private val dishRepo: DishRepository,
    private val ingredientGen: IngredientAutoGenerator,
) {
    /**
     * 菜品预览：dishIdByName 命中→REUSE；未命中→逐料 preview + 默认克数。[AI生成]
     *
     * @param input 语义菜品输入
     * @param ctx 预取字典上下文
     * @return DishPreview（含逐食材 IngredientPreview + 估算热量）
     */
    suspend fun preview(
        input: SemanticDish,
        ctx: AutoGenContext,
    ): DishPreview = withContext(ioDispatcher) {
        val dishName = input.name.trim()
        if (dishName.isBlank()) {
            return@withContext DishPreview(
                inputName = input.name,
                resolution = ResolveKind.CREATE,
                existingId = null,
                ingredients = emptyList(),
                source = input.source,
                estimatedKcal = null,
            )
        }

        // 1) 查已有同名菜品（dishIdByName）
        val existingId = dishRepo.dishIdByName(dishName)
        if (existingId != null) {
            return@withContext DishPreview(
                inputName = dishName,
                resolution = ResolveKind.REUSE,
                existingId = existingId,
                ingredients = emptyList(), // REUSE 不需要逐料展开
                source = input.source,
                estimatedKcal = null, // REUSE 不重算
            )
        }

        // 2) CREATE：解析食材列表
        val semIngredients = if (input.ingredients.isNotEmpty()) {
            input.ingredients
        } else {
            // 空料→菜名推演（复用 DishNameIngredientGuesser）
            val ingredientNames = ctx.ingredientNameToId.keys.toList()
            val guessed = DishNameIngredientGuesser.guessDetailed(dishName, ingredientNames)
            guessed.map { g ->
                SemanticIngredient(
                    name = g.name,
                    isMain = true,
                    // inLibrary→让 preview 走 REUSE；!inLibrary→CREATE
                )
            }
        }

        // 逐料 preview
        val ingredientPreviews = semIngredients.map { si ->
            ingredientGen.preview(si, ctx)
        }

        // 估算热量：Σ(营养热量×克数/100)
        val estimatedKcal = ingredientPreviews.fold(0.0) { acc, ip ->
            val kcalPer100 = ip.nutrition.values?.energyKcal
            if (kcalPer100 != null) {
                acc + kcalPer100 * ip.quantity / 100.0
            } else acc
        }.takeIf { it > 0 } // 全缺料→null（显"营养待完善"非"约0"）

        DishPreview(
            inputName = dishName,
            resolution = ResolveKind.CREATE,
            existingId = null,
            ingredients = ingredientPreviews,
            source = input.source,
            estimatedKcal = estimatedKcal,
        )
    }

    /**
     * 菜品入库：REUSE→返已有id；CREATE→逐料commit→组DishIngredient→saveDish。[AI生成]
     *
     * saveDish 内部已回填 unitId=null→gramUnit（INV-03）。
     */
    suspend fun commit(preview: DishPreview): Long = withContext(ioDispatcher) {
        when (preview.resolution) {
            ResolveKind.REUSE -> {
                preview.existingId ?: error("REUSE but existingId is null for ${preview.inputName}")
            }
            ResolveKind.CREATE -> {
                // 逐料 commit
                val dishIngredients = preview.ingredients.map { ip ->
                    val ingredientId = ingredientGen.commit(ip)
                    DishIngredient(
                        ingredient = Ingredient(
                            id = ingredientId,
                            name = ip.normalizedName,
                            defaultUnitId = ip.unitId,
                        ),
                        quantity = ip.quantity,
                        unitId = ip.unitId, // saveDish 内部会回填 null→gramUnit
                        isMain = true,
                    )
                }

                // saveDish（内部 db.transaction·已回填 unitId=null→gramUnit）
                dishRepo.saveDish(
                    id = 0,
                    name = preview.inputName,
                    cookingMethodId = null,
                    cookingMethodNames = emptyList(),
                    specialNote = "",
                    description = "",
                    imagePath = "",
                    thumbnailPath = "",
                    tagNames = emptyList(),
                    ingredients = dishIngredients,
                    steps = emptyList(),
                    source = preview.source.ifBlank { "auto" },
                )
            }
        }
    }
}
