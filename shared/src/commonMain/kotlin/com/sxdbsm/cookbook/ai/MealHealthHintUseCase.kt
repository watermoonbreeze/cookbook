package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.HealthCondition
import com.sxdbsm.cookbook.domain.MealConcernKind
import com.sxdbsm.cookbook.domain.NutritionLevelEvaluator
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.NutritionTotals
import com.sxdbsm.cookbook.platform.ioDispatcher
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * @File : MealHealthHintUseCase
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 记一餐保存后"命中已登记慢病关注点"的轻提示判定(运营一期 #177 ③)
 * <p>
 * 输入本次保存的菜品 id + 日期，按**当前查看成员**的已登记病种(与首页今日卡同口径)判定这餐是否命中忌口/钠/GI/嘌呤/油脂，
 * 命中则返回**最重的一项** [MealConcernKind](take Top-1)供 UI 拼一句 Snackbar 轻提示；未登记病种/未命中/无数据→返回 null(不提示)。
 * 判定纯读、复用 [NutritionLevelEvaluator](评级算法)+ [HealthRuleEngine] 同源忌口口径(避重复造/防漂移)，
 * 失败由调用方降级为不提示、绝不阻断保存。
 * <p>
 * 门禁(apple_software_behavior T1 行为契约 + apple_ux_designer + copywriter)裁决：
 * - 触发三闸(调用方 gate)：仅已登记健康档案 + 保存成功 + 新增/复制新建(loadedFromDate==null)；编辑既有餐不触发、天然一次性。
 * - 个人视角×份额(share)：钠/油脂等数值按当前查看成员食用份额折算(与今日卡一致)；忌口(AVOID)按"这道含忌口食材"presence 判定(与份额无关·可能给别的家人吃)。
 * - 只返回维度种类不带文案/数字：文案(定性·不点病名·不出百分比)由 UI 层按 kind 映射，守热量个人概念红线。
 * <p>
 * [AI生成] 运营#177 ③ 记菜命中慢病轻提示：薄 VM + shared 判定 + 单测(守准则B)。
 **/
class MealHealthHintUseCase(
    private val db: CookbookDatabase,
    private val familyRepo: FamilyRepository,
    private val ingredientRepo: IngredientRepository,
    private val nutritionRepo: NutritionRepository,
) {
    private val q = db.cookbookQueries

    /**
     * 判定本次保存的餐是否命中当前查看成员的慢病关注点，命中返回 Top-1 维度 + 成员名(忌口文案用)。[AI生成]
     *
     * @param dishIds 本次保存写入的全部菜品 id(多餐次块整批合并·去重后传入)
     * @param date 保存的日期(用于取该日食用份额 share)
     * @return 命中的最重一项 [MealHealthHint]；未登记病种/无命中/无数据→null(调用方据此决定是否追加提示)
     */
    suspend fun evaluate(dishIds: List<Long>, date: LocalDate): MealHealthHint? = withContext(ioDispatcher) {
        if (dishIds.isEmpty()) return@withContext null
        // 闸1：当前查看成员(与今日卡同口径)；无关注成员→不提示。
        val viewing = familyRepo.observeViewingMember().first() ?: return@withContext null
        val careIds = viewing.careCategoryIds
        if (careIds.isEmpty()) return@withContext null
        // 闸2：已登记病种(通用模式/仅非慢病 care 分类→conditions 空→不提示)。
        val conditions = q.selectFoodCategoryNamesByIds(careIds).executeAsList()
            .flatMap { HealthCondition.fromCareName(it) }.toSet()
        if (conditions.isEmpty()) return@withContext null

        val seasoningIds = q.selectSeasoningIngredientIds().executeAsList().toSet()
        // 本餐全部配料(含辅料·一次批量取)——**两种口径消费**：忌口 presence 判定用全部食材(非调料任意角色，下:avoidHit)；
        //   营养维度的主料名只取 is_main(下:mains)。两处过滤条件不同，改动其一勿误改另一。
        val ings = q.selectDishIngredientsByDishIds(dishIds).executeAsList()

        val hits = mutableListOf<MealConcernKind>()

        // 忌口(AVOID)：这餐含"避免"级食材(非调料·任意角色)即命中——presence 判定，与份额无关(可能给别的家人吃，中性告知)。
        val avoidIds = ingredientRepo.listByCareCategories(careIds)
            .filter { it.adviceLevel == AdviceLevel.AVOID }.map { it.id }.toSet()
        if (avoidIds.isNotEmpty() &&
            ings.any { it.ingredient_id in avoidIds && it.ingredient_id !in seasoningIds }
        ) hits += MealConcernKind.AVOID

        // 营养数值维度(钠/GI/嘌呤/油脂)：个人视角×份额(与今日卡一致)；关注成员今天没在家吃(share≤0)→跳过数值维度。
        val share = familyRepo.observeFocusShareForDate(DateTime.formatDate(date)).first()
        if (share > 0.0) {
            val totals = nutritionRepo.totalOf(dishIds)
            if (totals.energyKcal > 0.0) {
                val mains = ings.filter { it.is_main == 1L && it.ingredient_id !in seasoningIds }.map { it.ingredient_name }
                val highPurine = if (HealthCondition.GOUT in conditions)
                    NutritionLevelEvaluator.matchHighPurineFoods(mains) else emptyList()
                val highGi = if (HealthCondition.DIABETES in conditions)
                    NutritionLevelEvaluator.matchHighGiFoods(mains, nutritionRepo.giByName()) else emptyList()
                // 个人份额折算：仅折算 [NutritionLevelEvaluator.topNutritionConcernKind] 判定所需的**负向维度**字段
                //   (热量守卫 + 钠/饱脂/胆固醇)；钾/纤维等正向维度不参与 Top-Kind 判定故不折算(用整份 copy 保全但不读)。
                //   若日后 topNutritionConcernKind 扩展读取更多字段，须在此同步补折算，避免用到未折算的整份值。
                val personal = totals.copy(
                    energyKcal = totals.energyKcal * share,
                    sodiumMg = totals.sodiumMg * share,
                    saturatedFatG = totals.saturatedFatG * share,
                    cholesterolMg = totals.cholesterolMg * share,
                )
                NutritionLevelEvaluator.topNutritionConcernKind(
                    totals = personal, conditions = conditions,
                    highPurineHits = highPurine, highGiFoods = highGi,
                )?.let { hits += it }
            }
        }

        // take Top-1：多命中取严重度最重(枚举 ordinal 小=更重：忌口>嘌呤>油脂>钠>GI)。
        val top = hits.minByOrNull { it.ordinal } ?: return@withContext null
        MealHealthHint(kind = top, memberName = viewing.name)
    }
}

/**
 * 记一餐命中慢病关注点的轻提示信号(Top-1)。[AI生成] 运营#177 ③
 *
 * @param kind 命中的最重维度(文案由 UI 层映射)
 * @param memberName 当前查看成员名(忌口文案"在{名}的忌口清单里"用；其余维度可不用)
 */
data class MealHealthHint(val kind: MealConcernKind, val memberName: String?)
