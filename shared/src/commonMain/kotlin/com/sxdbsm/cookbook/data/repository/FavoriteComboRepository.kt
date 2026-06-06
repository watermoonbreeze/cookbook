package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.FavoriteCombo
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @File : FavoriteComboRepository
 * @Time : 2026/06/06
 * @Author : SXD-AI
 * @Desc : 收藏菜品组合仓库
 * <p>
 * 负责创建、读取和删除常用菜品组合。组合本身不复制菜品数据，只通过 `favorite_combo_dish` 关联菜品。
 * <p>
 * [AI生成] 为 MVP “复用困难”补充收藏组合入口。
 **/
class FavoriteComboRepository(
    private val db: CookbookDatabase,
    private val dishRepo: DishRepository,
) {
    private val q = db.cookbookQueries

    /**
     * 读取全部有效组合及其菜品。[AI生成]
     */
    suspend fun listCombos(): List<FavoriteCombo> = withContext(Dispatchers.Default) {
        q.selectAllCombos().executeAsList().map { combo ->
            val dishRows = q.selectDishesOfCombo(combo.id).executeAsList()
            val dishes = dishRepo.buildDishMinis(
                dishRows.map { row ->
                    DishRepository.DishMiniSource(
                        id = row.id,
                        name = row.name,
                        imagePath = row.image_path,
                        thumbnailPath = row.thumbnail_path,
                        preference = row.preference.toInt(),
                        cookingMethodId = row.cooking_method_id,
                    )
                },
            )
            FavoriteCombo(id = combo.id, name = combo.name, dishes = dishes)
        }
    }

    /**
     * 创建收藏组合。[AI生成]
     */
    suspend fun createCombo(name: String, dishIds: List<Long>): Long = withContext(Dispatchers.Default) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "组合名称不能为空" }
        val distinctDishIds = dishIds.distinct()
        require(distinctDishIds.isNotEmpty()) { "组合内至少需要一个菜品" }
        var comboId = 0L
        db.transaction {
            q.insertFavoriteCombo(trimmed, "user", DateTime.nowEpochSeconds())
            comboId = q.lastInsertId().executeAsOne()
            distinctDishIds.forEach { dishId -> q.linkComboDish(comboId, dishId) }
        }
        comboId
    }

    /**
     * 软删除收藏组合。[AI生成]
     */
    suspend fun deleteCombo(id: Long) = withContext(Dispatchers.Default) {
        q.deleteCombo(id)
    }
}
