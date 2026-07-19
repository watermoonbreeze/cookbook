package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.domain.HealthCondition
import com.sxdbsm.cookbook.domain.model.Dish
import com.sxdbsm.cookbook.domain.model.FamilyMember
import com.sxdbsm.cookbook.domain.model.MemberDishVerdict

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
        // 先逐成员取约束(skipGi=true 不各查全表)；再仅当有糖尿病成员时把 GI 全表**只查一次**共用——
        //   无糖尿病成员=0 次查询、有则 1 次(优于"每个糖尿病成员各查一次")。成员数少,N 条轻 SQL 可接受;列表页大批量再缓存化(Phase 2)。
        val gathered = members.map { m -> m to recoDataSource.gatherConstraintsForMember(m, skipGi = true) }
        val giByName = if (gathered.any { HealthCondition.DIABETES in it.second.conditions }) nutritionRepo.giByName() else emptyMap()
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
            )
        }
    }

    /** 便捷：评估全部家庭成员。[AI生成] */
    suspend fun evaluateAllMembers(dish: Dish): List<MemberDishVerdict> =
        evaluate(dish, familyRepo.listMembers())
}
