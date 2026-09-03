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

        // [AI生成] AUTOGEN-UNIFY 决策点1A：营养大类=字段级「AI 优先、本地兜底」——
        //   解析层给出的 groupHint(NDJSON 7 精确词映射)非空直用；null(规则路径/meat/seasoning/未知词)→本地 classify 全权。
        //   REUSE 路径此值为展示/估算口径(不读不写库内列)。
        val group = input.groupHint ?: FoodGroup.classify(normalized)

        // 2) dedup：查已有食材
        val existingId = ctx.ingredientNameToId[nameKey]
        if (existingId != null) {
            // REUSE：命中库内已有食材·不建重复
            val categoryName = group?.let { FoodGroup.CATEGORY_NAME[it] }
            val categoryId = categoryName?.let { ctx.categoryNameToId[it] }
            // 仍跑一次营养推演（供上层展示预览营养）
            val guessedNutrition = guessNutrition(input, normalized, group, ctx)
            // isSeasoning: 在调料名集即为调料
            val isSeasoning = nameKey in ctx.seasoningNames
            val defaultQty = SeasoningDefaults.defaultGramFor(normalized, isSeasoning).toDouble()
            return@withContext IngredientPreview(
                inputName = input.name,
                normalizedName = normalized,
                resolution = ResolveKind.REUSE,
                existingId = existingId,
                group = group,
                categoryId = categoryId,
                nutrition = guessedNutrition,
                quantity = input.quantity?.takeIf { it > 0 } ?: defaultQty,
                unitId = ctx.gramUnitId,
                careFlag = CareFlag.INHERITED, // 归一命中库内→继承既有 care
            )
        }

        // 3) CREATE：新食材→classify+营养估算+单位+careFlag=PENDING
        val categoryName = group?.let { FoodGroup.CATEGORY_NAME[it] }
        val categoryId = categoryName?.let { ctx.categoryNameToId[it] }

        // 营养推演（AI hint 优先·本地 NutritionGuesser 兜底）
        val guessedNutrition = guessNutrition(input, normalized, group, ctx)

        // 默认克数：调料用小值、普通食材按大类；group==null 仅代表未归类·不等于调料
        val isSeasoning = nameKey in ctx.seasoningNames
        val defaultQty = SeasoningDefaults.defaultGramFor(normalized, isSeasoning).toDouble()

        IngredientPreview(
            inputName = input.name,
            normalizedName = normalized,
            resolution = ResolveKind.CREATE,
            existingId = null,
            group = group,
            categoryId = categoryId,
            nutrition = guessedNutrition,
            quantity = input.quantity?.takeIf { it > 0 } ?: defaultQty,
            unitId = ctx.gramUnitId,
            careFlag = CareFlag.PENDING_REVIEW, // 新建食材 care 留待人工复核（健康红线）
        )
    }

    /**
     * 营养推演：AI hint 优先、本地 NutritionGuesser 兜底。[AI生成] AUTOGEN-UNIFY 决策点2A。
     * nutritionHint 本批无生产写入者（管线位），下一批「AI 营养补全」接入。
     */
    private fun guessNutrition(
        input: SemanticIngredient,
        normalized: String,
        group: FoodGroup.Group?,
        ctx: AutoGenContext,
    ): NutritionGuess =
        input.nutritionHint?.let { NutritionGuess(it, NutritionGuessSource.AI) }
            ?: NutritionGuesser.guess(normalized, ctx.nutritionCandidates, group)

    /**
     * 食材入库：REUSE→直接返已有 id；CREATE→createUserIngredient+仅空列补齐(food_group/营养)。[AI生成]
     *
     * @param preview 由 [preview] 产出的预览（必先 preview 再 commit）
     * @param source 入库来源标记：AI 记餐路径默认 "ai"；用户侧快速自建(NewDishViewModel)须显式传 "user"
     *   （保住删除按钮/回收站/备份导出三处按 source 门控——AUTOGEN-UNIFY 架构审 AF-AU-01）。
     * @return 食材 id
     */
    suspend fun commit(preview: IngredientPreview, source: String = "ai"): Long = withContext(ioDispatcher) {
        when (preview.resolution) {
            ResolveKind.REUSE -> {
                // 已存在：直接复用已有 id
                // [AI修改] 日志门禁：不拼具体食材名(用户饮食文本)，理由同 DishAutoGenerator.commit() 的同类改动。
                preview.existingId ?: error("IngredientPreview inconsistent: resolution=REUSE but existingId=null (nameLen=${preview.normalizedName.length})")
            }
            ResolveKind.CREATE -> {
                // 新建食材（createUserIngredient 内部自带"精确+归一名"双层去重——陈旧 ctx 误判 CREATE 时
                // 这里返回的是既有行 id，下方仅空列守卫确保不覆盖既有值·AUTOGEN-UNIFY 审核 A1）
                val id = ingredientRepo.createUserIngredient(
                    name = preview.normalizedName,
                    categoryId = preview.categoryId,
                    source = source, // [AI修改] AUTOGEN-UNIFY：source 参数化（默认 "ai" 维持 AI 记餐现状）
                )

                // [AI生成] AUTOGEN-UNIFY：写 food_group 列（仅空列守卫——ingredient.name UNIQUE，按名查即按行查；
                //   createUserIngredient 内部复用既有行时该行可能已有用户手选/AI 写过的大类，非空不覆盖·对齐 seeder「只填空」纪律）。
                preview.group?.let { g ->
                    val existingGroup = ingredientRepo.foodGroupByName()[preview.normalizedName].orEmpty()
                    if (existingGroup.isEmpty()) {
                        ingredientRepo.setFoodGroup(id, g.name) // 列存 Group 枚举名（如 WHITE_MEAT）
                    }
                }

                // 写入营养估算（缺字段留 null 不填 0；仅空守卫——已有营养行(如用户手填)不覆盖）
                val nutritionValues = preview.nutrition.values
                if (nutritionValues != null && !nutritionRepo.hasNutrition(id)) {
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
                            // [AI生成] AUTOGEN-UNIFY：AI 估值与本地估算区分来源（T1 留痕·数据层；用户可见层不并列二值——行为审）
                            ref = if (preview.nutrition.source is NutritionGuessSource.AI) "AI 估算" else "自动估算", // 标"估算"非权威（INV-09）·复核状态见 review 字段
                        )
                    )
                }

                id
            }
        }
    }

    /**
     * 便捷门面：preview+commit 直通返回食材 id（归一/分类/营养预估/food_group 一次做齐）。[AI生成] AUTOGEN-UNIFY
     *
     * 陈旧 ctx 防护：preview 误判 CREATE 时，commit 内 createUserIngredient 双层去重返回既有 id、
     * 仅空列守卫不覆盖既有值——无需入口额外查库。
     *
     * **新增调用方须过架构审**（L4 分享链接导入即将复用）。
     *
     * @param name 食材名（归一/dedup 在管线内做）
     * @param source 入库来源：AI 记餐默认 "ai"；用户侧快速自建传 "user"
     */
    suspend fun ensureCreated(name: String, ctx: AutoGenContext, source: String = "ai"): Long =
        commit(preview(SemanticIngredient(name = name), ctx), source)
}
