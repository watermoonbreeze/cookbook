package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.domain.DishNameIngredientGuesser
import com.sxdbsm.cookbook.domain.NutritionGuessSource
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.DishNutrition
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientNutrition
import com.sxdbsm.cookbook.domain.model.NutritionCalculator
import com.sxdbsm.cookbook.domain.model.NutritionInput
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
                nutrition = null, // 菜名为空→从未尝试计算（INV-K1A-04 ①类）
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
                nutrition = null, // REUSE 占位；MultiDayRecorder.previewAll() 后处理会按 id 批量回填真实营养
                eatenRatio = input.eatenRatio,
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

        // K1a 营养计算统一：走 NutritionCalculator.dishNutrition()（单一真相源，产出蛋白/脂肪/碳水/钠等全字段）。
        val rawNutrition = NutritionCalculator.dishNutrition(ingredientPreviews.map { it.toNutritionInput() })
        // GC-37 挑战 #6：unitGrams 固定非空导致 resolveGrams() 永不走 est=true 分支，NutritionCalculator 自身的
        // estimated 对"营养值是猜的"不敏感——本层显式补上：只要有一个食材不是高置信度 Match，就标估算
        // （None/Group 均视为猜测，仅 Match 为确切命中）。
        val anyGuessed = ingredientPreviews.any { it.nutrition.source !is NutritionGuessSource.Match }
        val nutrition = rawNutrition.copy(estimated = rawNutrition.estimated || anyGuessed)
        // GC-37 挑战 #2：不做 hasData 过滤——"算了但没数据"必须仍是非空 DishNutrition，
        // 交给 DishNutritionLine 自己渲染"营养待完善"（INV-K1A-04）。

        DishPreview(
            inputName = dishName,
            resolution = ResolveKind.CREATE,
            existingId = null,
            ingredients = ingredientPreviews,
            source = input.source,
            nutrition = nutrition,
            eatenRatio = input.eatenRatio,
        )
    }

    /**
     * IngredientPreview → NutritionCalculator 输入。quantity 在 preview 与 commit 两条路径下都被当作克数使用
     * （IngredientAutoGenerator.preview() 固定 unitId=gramUnit，DishAutoGenerator.commit() 原样传 unitId 落库）——
     * 这是"preview/commit 口径一致"的既有事实，不代表 AI 给出的原始 unit（如"个/勺"）真的做过克重换算。
     * unitGrams=1.0：quantity 本身已是克数，按克折算。nutrition 为 null（未匹配）时 NutritionCalculator 贡献 0。[AI修改]
     */
    private fun IngredientPreview.toNutritionInput(): NutritionInput = NutritionInput(
        quantity = quantity,
        unitGrams = 1.0,
        nutrition = nutrition.values?.let { v ->
            // 同步守卫：若 NutritionGuessValues 增字段，需同步加到此映射（逐字段映射无编译期校验）。
            IngredientNutrition(
                ingredientId = existingId ?: 0L, // 占位；NutritionCalculator.dishNutrition() 不读取该字段
                energyKcal = v.energyKcal, proteinG = v.proteinG, fatG = v.fatG, carbG = v.carbG,
                fiberG = v.fiberG, sodiumMg = v.sodiumMg, potassiumMg = v.potassiumMg,
                calciumMg = v.calciumMg, gi = v.gi, purineMg = v.purineMg,
            )
        },
    )

    /**
     * 菜品入库：REUSE→返已有id；CREATE→逐料commit→组DishIngredient→saveDish。[AI修改] B1修复：防commit retry重复创建。
     *
     * saveDish 内部已回填 unitId=null→gramUnit（INV-03）。
     */
    suspend fun commit(preview: DishPreview): Long = withContext(ioDispatcher) {
        when (preview.resolution) {
            ResolveKind.REUSE -> {
                preview.existingId ?: error("REUSE but existingId is null for ${preview.inputName}")
            }
            ResolveKind.CREATE -> {
                // 防御 commit retry 重复创建：preview 缓存在 state 中，retry 仍为 CREATE
                val existingId = dishRepo.dishIdByName(preview.inputName)
                if (existingId != null) return@withContext existingId

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
