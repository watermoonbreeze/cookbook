package com.sxdbsm.cookbook.domain.model

data class CrowdType(
    val id: Long,
    val name: String,
    val description: String = "",
)

data class FoodCategory(
    val id: Long,
    val name: String,
    val dimension: String = "general",
    val parentId: Long? = null,
    /** 非空表示绑定到 crowd_type；食材列表走 crowd_ingredient 查询 */
    val crowdTypeId: Long? = null,
    val sortOrder: Int = 0,
    val icon: String = "",
    val source: String = "preset",
    /** 是否能展开二级 */
    val hasChildren: Boolean = false,
)

data class HealthProfile(
    val crowdTypeId: Long,
    val crowdName: String,
    val crowdDescription: String,
    val enabled: Boolean,
)
