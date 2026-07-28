package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.domain.HealthCondition
import com.sxdbsm.cookbook.domain.model.Dish
import com.sxdbsm.cookbook.domain.model.FamilyMember
import com.sxdbsm.cookbook.domain.model.MemberDishVerdict
import com.sxdbsm.cookbook.domain.model.TrafficLight

/**
 * @File : MemberDishHealthUseCase
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 成员化红绿灯 UseCase——逐位家人评估同一道菜的健康适宜度（商业#1 护城河）
 * <p>
 * 对每位成员各构造一套健康约束（[RecommendationDataSource.gatherConstraintsForMember]，与推荐/详情同口径），
 * 再由 [MemberDishVerdict.of] 映射成红/黄/绿 + 归因。UI 据此显"这道菜对张三绿灯、对痛风的李四黄灯"。
 * <p>
 * 守红线：物理隔离（不接色系墙营养级别）、免责（仅供参考·非医嘱·UI 承接）。评估只读、不改数据。
 * [AI生成] 商业#1 P0 阶段1（详情页）。列表页逐项评估为后续 Phase 2。
 **/
class MemberDishHealthUseCase(
    private val recoDataSource: RecommendationDataSource,
    private val familyRepo: FamilyRepository,
    private val ingredientRepo: IngredientRepository,
    private val nutritionRepo: NutritionRepository, // [AI生成] GI 全表由此只查一次共用(见 evaluate)
    private val dishRepo: DishRepository, // [AI生成] Phase 2 列表徽章：批量取菜品配料信息(免逐菜 loadFullDish)
) {

    /**
     * 逐成员评估一道菜的红绿灯。[AI生成]
     *
     * @param members 调用方筛好的成员集合（如全部家庭成员或仅关注成员）。空则返回空（UI 不显该区块）。
     * @return 按传入顺序，每位成员一条 [MemberDishVerdict]。
     */
    suspend fun evaluate(dish: Dish, members: List<FamilyMember>): List<MemberDishVerdict> {
        if (members.isEmpty()) return emptyList()
        val seasoningIds = ingredientRepo.seasoningIngredientIds()
        // 先逐成员取约束(skipGi=true 不各查全表)；再仅当有糖尿病/痛风成员时把 GI/嘌呤全表**只查一次**共用——
        //   无对应病种=0 次查询、有则 1 次(优于"每个成员各查一次")。成员数少,N 条轻 SQL 可接受;列表页大批量再缓存化(Phase 2)。
        val gathered = members.map { m -> m to recoDataSource.gatherConstraintsForMember(m, skipGi = true) }
        val hasDiabetic = gathered.any { HealthCondition.DIABETES in it.second.conditions }
        val hasGout = gathered.any { HealthCondition.GOUT in it.second.conditions }
        val giByName = if (hasDiabetic) nutritionRepo.giByName() else emptyMap()
        // [AI生成] 嘌呤数据驱动补漏：与 giByName 同模式——全表只查一次、痛风成员共用。
        //   补原 matchHighPurineFoods 关键词法漏网的中嘌呤食材(如草鱼 purine=140 不命中关键词但应留意)。
        val purineByName = if (hasGout) nutritionRepo.purineByName() else emptyMap()
        return gathered.map { (m, rc) ->
            MemberDishVerdict.of(
                dish = dish,
                memberId = m.id,
                memberName = m.name,
                avoidIds = rc.constraints.avoidIngredientIds,
                personalAvoidIds = rc.constraints.personalAvoidIngredientIds,
                limitIds = rc.constraints.limitIngredientIds,
                conditions = rc.conditions,
                giByName = giByName, // 共用;of() 内仅糖尿病成员的 conditions 会用到,非糖尿病成员传入无副作用
                seasoningIds = seasoningIds,
                purineByName = purineByName, // [AI生成] 嘌呤数据驱动补漏(与 giByName 同模式·仅痛风成员用到)
            )
        }
    }

    /** 便捷：评估全部家庭成员。[AI生成] */
    suspend fun evaluateAllMembers(dish: Dish): List<MemberDishVerdict> =
        evaluate(dish, familyRepo.listMembers())

    /**
     * 批量评估多道菜对一位成员的交通灯颜色（仅返回灯色，不含归因名）。[AI生成] Phase 2 列表徽章。
     *
     * 与 [evaluate]（单菜→全员）正交：这里是**多菜→一人**。用于菜品列表（选菜/菜品库/推荐等）
     * 每道菜旁显示一个小圆点，让用户在选菜时一眼看到"这道菜对当前查看者是否合适"。
     *
     * **缓存策略**：本方法内部对成员约束只查一次（[gatherConstraintsForMember]），
     * 菜品配料信息走批量 SQL（[DishRepository.loadDishIngredientInfo]），免逐菜 loadFullDish N+1。
     * **调用方**应在 dish 列表变化时重新调用，成员约束不变期间不需重复调用。
     *
     * **当前口径**：仅判忌口(红)+限量(黄)+安全(绿)，不做 GI/嘌呤定性（列表徽章空间小·详情页已有完整判定）。
     * 忌口取病种+个人并集（含调料）、限量只取主料（门槛代理）。
     *
     * @return dishId → TrafficLight，未查到的 dishId 默认 GREEN。
     */
    suspend fun evaluateDishLightsForMember(
        dishIds: List<Long>,
        member: FamilyMember,
    ): Map<Long, TrafficLight> {
        if (dishIds.isEmpty()) return emptyMap()
        // 1. 该成员约束只查一次（gatherConstraintsForMember 轻量·几条 SQL）
        val rc = recoDataSource.gatherConstraintsForMember(member, skipGi = true)
        val avoidIds = rc.constraints.avoidIngredientIds
        val personalAvoidIds = rc.constraints.personalAvoidIngredientIds
        val limitIds = rc.constraints.limitIngredientIds
        val seasoningIds = ingredientRepo.seasoningIngredientIds()
        // 2. 批量取全部菜的配料 ID + isMain（一条 SQL·免逐菜 loadFullDish N+1）
        val ingredientsByDish = dishRepo.loadDishIngredientInfo(dishIds)
        // 3. 逐菜判灯（纯内存 set 交运算）
        return dishIds.associateWith { dishId ->
            val ings = ingredientsByDish[dishId].orEmpty()
            val hasAvoid = ings.any { (ingId, _, _) ->
                (ingId in avoidIds && ingId !in seasoningIds) || ingId in personalAvoidIds
            }
            val hasLimit = ings.any { (ingId, _, isMain) ->
                isMain && ingId in limitIds
            }
            when {
                hasAvoid -> TrafficLight.RED
                hasLimit -> TrafficLight.YELLOW
                else -> TrafficLight.GREEN
            }
        }
    }
}
