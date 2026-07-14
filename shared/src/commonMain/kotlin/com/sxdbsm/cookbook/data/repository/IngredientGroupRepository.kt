package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.IngredientGroup
import com.sxdbsm.cookbook.domain.model.IngredientGroupItem
import com.sxdbsm.cookbook.platform.ioDispatcher
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.withContext

/**
 * @File : IngredientGroupRepository
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 常用配料组仓库（列/建/删；预设+自建）
 * <p>
 * 配料组=一组食材名(+是否主料)，编辑菜品"配料组"一键加入食材清单。预设不可删、自建可删。
 * <p>
 * [AI生成] B5
 **/
class IngredientGroupRepository(
    private val db: CookbookDatabase,
) {
    private val q = db.cookbookQueries

    /** 列出全部有效配料组(预设在前)及其食材项。[AI生成] */
    suspend fun listGroups(): List<IngredientGroup> = withContext(ioDispatcher) {
        q.selectIngredientGroups().executeAsList().map { g ->
            IngredientGroup(
                id = g.id,
                name = g.name,
                source = g.source,
                items = q.selectIngredientGroupItems(g.id).executeAsList()
                    .map { IngredientGroupItem(name = it.ingredient_name, isMain = it.is_main == 1L) },
            )
        }
    }

    /**
     * 新建自建配料组(同名自建复用同一 id、重建其项)。[AI生成]
     */
    suspend fun createGroup(name: String, items: List<IngredientGroupItem>): Long = withContext(ioDispatcher) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "配料组名称不能为空" }
        val clean = items.map { it.copy(name = it.name.trim()) }.filter { it.name.isNotBlank() }.distinctBy { it.name }
        require(clean.isNotEmpty()) { "配料组至少需要一味食材" }
        var groupId = 0L
        db.transaction {
            val existing = q.selectIngredientGroupByName(trimmed).executeAsOneOrNull()
            if (existing != null && existing.source != "preset") {
                groupId = existing.id
                q.deleteIngredientGroupItems(groupId)
            } else {
                q.insertIngredientGroup(trimmed, "user", DateTime.nowEpochSeconds())
                groupId = q.lastInsertId().executeAsOne()
            }
            clean.forEachIndexed { index, item ->
                q.insertIngredientGroupItem(groupId, item.name, if (item.isMain) 1L else 0L, index.toLong())
            }
        }
        groupId
    }

    /** 软删除配料组。[AI生成] */
    suspend fun deleteGroup(id: Long) = withContext(ioDispatcher) {
        db.transaction {
            q.deleteIngredientGroupItems(id)
            q.deleteIngredientGroup(id)
        }
    }
}
