package com.sxdbsm.cookbook.domain.model

/**
 * @File : ShoppingList
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 采购清单聚合模型
 * <p>
 * 把"今天及未来"所有餐食里需采购(主料未入库)/缺料(库存份数不足)的食材去重汇总成一张购物清单。
 * 纯派生、不落库；数据来源复用食历卡片已算好的 purchase/shortage 标记，保证与卡片显示一致。
 * <p>
 * [AI生成] 待办"采购清单聚合"：把周期规划/食历中的采购项汇总成购物清单。
 **/
data class ShoppingItem(
    val ingredientId: Long?, // 名字解析不到时为 null（仍展示名字）
    val ingredientName: String,
    val reason: ShoppingReason,
    val mealCount: Int, // 出现在多少个"餐-菜"里（同一食材跨多餐累加）
    val dates: List<String>, // 涉及的日期（yyyy-MM-dd，去重升序）
)

/** 购物项原因。[AI生成] */
enum class ShoppingReason {
    /** 主料完全未入库，需采购。 */
    PURCHASE,

    /** 已入库但份数不足（缺料）。 */
    SHORTAGE,
}
