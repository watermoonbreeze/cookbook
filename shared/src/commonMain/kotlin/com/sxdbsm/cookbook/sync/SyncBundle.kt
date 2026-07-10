package com.sxdbsm.cookbook.sync

import kotlinx.serialization.Serializable

/**
 * @File : SyncBundle
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 双设备选择性同步的结构化数据包（按名称匹配合并，跨设备可携带）
 * <p>
 * 与"整库替换"不同：Bundle 只带**选中数据域的行**，接收端按名称匹配合并(加/更新)、ID 重映射，不整库覆盖。
 * 图片以相对文件名携带(实际字节由 androidApp 侧打进 zip 的 img/)。P1：食材 + 菜品。
 * <p>
 * [AI生成] 方案 `双设备选择性同步方案.md` P1。
 **/
@Serializable
data class SyncBundle(
    val version: Int = 1, // Bundle 格式版本
    val schemaVersion: Int, // 生成时 DB Schema 版本(导入时校验不高于本机)
    val ingredients: List<SyncIngredient> = emptyList(),
    val dishes: List<SyncDish> = emptyList(),
    val pantry: List<SyncPantry> = emptyList(), // P2 库存
    val healthCrowds: List<String> = emptyList(), // P2 健康档案(启用的人群/病种名)
    val favorites: List<SyncFavorite> = emptyList(), // P2 收藏组合
    val meals: List<SyncMeal> = emptyList(), // P3 餐食历史
)

/** 要同步的数据域选择。[AI生成] 选某域自动带依赖(库存/收藏/餐食→其食材/菜品)。 */
data class SyncSelection(
    val ingredients: Boolean = false,
    val dishes: Boolean = false,
    val pantry: Boolean = false,
    val health: Boolean = false,
    val favorites: Boolean = false,
    val meals: Boolean = false,
) {
    val any: Boolean get() = ingredients || dishes || pantry || health || favorites || meals
}

/** 库存条目(合并键=ingredientName)。[AI生成] */
@Serializable
data class SyncPantry(val ingredientName: String, val servingCount: Int)

/** 收藏组合(合并键=name; 菜品按名引用)。[AI生成] */
@Serializable
data class SyncFavorite(val name: String, val dishNames: List<String> = emptyList())

/** 餐食记录(合并键=date+mealTypeCode; 菜品按名引用)。[AI生成] */
@Serializable
data class SyncMeal(
    val date: String,
    val mealTypeCode: String,
    val mealTime: String,
    val note: String = "",
    val dishNames: List<String> = emptyList(),
)

/** 同步的食材(自建 + 被菜品引用的)。[AI生成] 合并键=name。 */
@Serializable
data class SyncIngredient(
    val name: String,
    val alias: String = "",
    val emoji: String = "🥗",
    val imagePath: String = "", // 相对文件名(| 分隔)
    val thumbnailPath: String = "",
    val detail: SyncIngredientDetail? = null,
)

@Serializable
data class SyncIngredientDetail(
    val commonMethods: String = "",
    val prepTips: String = "",
    val eatingNotes: String = "",
    val storageTips: String = "",
    val healthNote: String = "",
)

/** 同步的菜品(自建)。[AI生成] 合并键=name；食材/做法/标签按 name 引用。 */
@Serializable
data class SyncDish(
    val name: String,
    val specialNote: String = "",
    val description: String = "",
    val imagePath: String = "",
    val thumbnailPath: String = "",
    val cookingMethodNames: List<String> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val ingredients: List<SyncDishIngredient> = emptyList(),
    val steps: List<SyncStep> = emptyList(),
)

@Serializable
data class SyncDishIngredient(
    val name: String, // 食材名(合并键)
    val isMain: Boolean = true,
    val quantity: Double? = null,
)

@Serializable
data class SyncStep(
    val sortOrder: Int = 0,
    val text: String = "",
    val imagePath: String = "",
    val thumbnailPath: String = "",
)

/** 导入合并结果。[AI生成] */
data class SyncImportResult(
    val ingredientsAdded: Int = 0,
    val dishesAdded: Int = 0,
    val dishesUpdated: Int = 0,
    val pantryMerged: Int = 0,
    val healthMerged: Int = 0,
    val favoritesAdded: Int = 0,
    val mealsMerged: Int = 0,
)
