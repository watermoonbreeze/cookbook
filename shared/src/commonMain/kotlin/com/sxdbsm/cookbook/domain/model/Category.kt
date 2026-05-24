package com.sxdbsm.cookbook.domain.model

/**
 * 健康人群类型，例如高血压、糖尿病、高尿酸。[AI修改]
 */
data class CrowdType(
    val id: Long,
    val name: String,
    val description: String = "",
)

/**
 * 食材分类节点。[AI修改]
 *
 * `parentId` 为空表示一级分类；非空表示二级分类。部分分类会绑定 `crowdTypeId`，
 * 代表这是按健康人群筛选的入口。
 */
data class FoodCategory(
    val id: Long,
    val name: String,
    val dimension: String = "general",
    val parentId: Long? = null,
    /** 非空表示绑定到 crowd_type；食材列表走 crowd_ingredient 查询。[AI修改] */
    val crowdTypeId: Long? = null,
    val sortOrder: Int = 0,
    val icon: String = "",
    val source: String = "preset",
    /** 是否能展开二级。[AI修改] */
    val hasChildren: Boolean = false,
)

/**
 * 用户启用的健康档案。[AI修改]
 *
 * 我的页用它决定当前用户关注哪些慢性病饮食建议。
 */
data class HealthProfile(
    val crowdTypeId: Long,
    val crowdName: String,
    val crowdDescription: String,
    val enabled: Boolean,
)
