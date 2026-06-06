package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.MeasurementUnit
import com.sxdbsm.cookbook.platform.Pinyin
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 食材数据仓库。[AI修改]
 *
 * 负责食材搜索、按分类筛选、创建用户自定义食材等操作。
 */
class IngredientRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries // [AI修改] SQLDelight 自动生成的查询集合。

    /**
     * 监听全部食材。[AI修改]
     */
    fun observeAll(): Flow<List<Ingredient>> =
        q.selectAllIngredients(::mapIngredientRow).asFlow().mapToList(Dispatchers.Default)

    /**
     * 关键词搜索食材。[AI修改]
     */
    suspend fun search(keyword: String): List<Ingredient> = withContext(Dispatchers.Default) {
        if (keyword.isBlank()) {
            q.selectAllIngredients(::mapIngredientRow).executeAsList()
        } else {
            q.searchIngredients("%${keyword.trim()}%", ::mapIngredientRow).executeAsList()
        }
    }

    /**
     * 按普通食材分类查询。[AI修改]
     */
    suspend fun listByCategory(categoryId: Long): List<Ingredient> = withContext(Dispatchers.Default) {
        q.selectIngredientsByCategory(categoryId, ::mapIngredientRow).executeAsList()
    }

    /**
     * 读取最近被餐食引用过的食材。[AI生成]
     *
     * 这是食材选择器左侧“最近使用”虚拟分类的数据源，不落库成真实分类。
     */
    suspend fun listRecentlyUsed(): List<Ingredient> = withContext(Dispatchers.Default) {
        q.selectRecentlyUsedIngredients(::mapIngredientRow).executeAsList()
    }

    /**
     * 按健康人群查询食材，并附带推荐/限制/避免建议。[AI修改]
     */
    suspend fun listByCrowd(crowdTypeId: Long): List<Ingredient> = withContext(Dispatchers.Default) {
        q.selectIngredientsByCrowd(crowdTypeId).executeAsList().map { row ->
            Ingredient(
                id = row.id,
                name = row.name,
                alias = row.alias,
                pinyin = row.pinyin,
                imagePath = row.image_path,
                thumbnailPath = row.thumbnail_path,
                emoji = row.emoji,
                defaultUnitId = row.default_unit_id,
                source = row.source,
                adviceLevel = AdviceLevel.fromCode(row.advice_level),
                adviceReason = row.reason,
            )
        }
    }

    /**
     * 创建用户自定义食材。[AI修改]
     */
    suspend fun createUserIngredient(
        name: String,
        alias: String = "",
        imagePath: String = "",
        thumbnailPath: String = "",
        categoryId: Long? = null,
    ): Long = withContext(Dispatchers.Default) {
        val now = DateTime.nowEpochSeconds()
        q.insertIngredient(
            name = name,
            alias = alias,
            pinyin = Pinyin.toPinyin(name),
            image_path = imagePath, // [AI修改] 新建食材时可保存可选图片路径，MVP 暂不接入系统相册。
            thumbnail_path = thumbnailPath, // [AI生成] 新建食材时保存缩略图路径，列表优先展示。
            emoji = "🥗", // [AI生成] 用户自建食材没有 JSON 预置图标时先使用通用食物图标。
            default_unit_id = null,
            source = "user",
            created_at = now,
        )
        val id = q.lastInsertId().executeAsOne()
        categoryId?.let { q.linkIngredientCategory(id, it) } // [AI修改] 新建食材时按用户选择绑定分类。
        id
    }

    /**
     * 读取计量单位字典。[AI修改]
     */
    suspend fun listMeasurementUnits(): List<MeasurementUnit> = withContext(Dispatchers.Default) {
        q.selectAllMeasurementUnits().executeAsList().map { MeasurementUnit(id = it.id, name = it.name) }
    }

    /**
     * 编辑食材基础信息。[AI修改]
     *
     * 修复9要求预设和自建食材都可以编辑；删除仍只允许自建食材。
     */
    suspend fun updateUserIngredient(id: Long, name: String, alias: String, imagePath: String, thumbnailPath: String) = withContext(Dispatchers.Default) {
        q.updateUserIngredient(
            name = name,
            alias = alias,
            image_path = imagePath,
            thumbnail_path = thumbnailPath,
            id = id,
        )
    }

    /**
     * 删除用户自建食材。[AI修改]
     *
     * 预设食材不能删除；用户食材删除前先清理菜品-食材关联，避免外键失败或孤儿关联。
     */
    suspend fun deleteUserIngredient(id: Long) = withContext(Dispatchers.Default) {
        db.transaction {
            q.deleteDishIngredientsByIngredient(id) // [AI修改] 先清理 dish_ingredient，确保删除不会留下无效关联。
            q.deleteUserIngredient(id)
        }
    }

    /**
     * SQLDelight 食材查询结果转领域模型。[AI修改]
     *
     * 食材查询使用 COALESCE 兼容旧库 NULL 字段，因此这里用显式 mapper，不依赖 `SELECT *` 表行类型。
     */
    private fun mapIngredientRow(
        id: Long,
        name: String,
        alias: String,
        pinyin: String,
        image_path: String,
        thumbnail_path: String,
        emoji: String,
        default_unit_id: Long?,
        source: String,
        @Suppress("UNUSED_PARAMETER") created_at: Long,
    ) = Ingredient(
        id = id,
        name = name,
        alias = alias,
        pinyin = pinyin,
        imagePath = image_path,
        thumbnailPath = thumbnail_path,
        emoji = emoji,
        defaultUnitId = default_unit_id,
        source = source,
    )
}

/**
 * 食材分类数据仓库。[AI修改]
 *
 * 单独成类是为了让选择器可以只关心分类树，不和食材增删逻辑混在一起。
 */
class FoodCategoryRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries

    /**
     * 读取一级分类。[AI修改]
     */
    suspend fun listTopLevel(): List<FoodCategory> = withContext(Dispatchers.Default) {
        q.selectTopLevelCategories().executeAsList().map { row ->
            FoodCategory(
                id = row.id,
                name = row.name,
                dimension = row.dimension,
                parentId = row.parent_id,
                crowdTypeId = row.crowd_type_id,
                sortOrder = row.sort_order.toInt(),
                icon = row.icon,
                source = row.source,
                hasChildren = q.countChildren(row.id).executeAsOne() > 0,
            )
        }
    }

    /**
     * 读取某个一级分类下的二级分类。[AI修改]
     */
    suspend fun listChildren(parentId: Long): List<FoodCategory> = withContext(Dispatchers.Default) {
        q.selectChildCategories(parentId).executeAsList().map { row ->
            FoodCategory(
                id = row.id,
                name = row.name,
                dimension = row.dimension,
                parentId = row.parent_id,
                crowdTypeId = row.crowd_type_id,
                sortOrder = row.sort_order.toInt(),
                icon = row.icon,
                source = row.source,
                hasChildren = false,
            )
        }
    }

    /**
     * 读取全部有效分类。[AI生成]
     *
     * 添加食材弹框和分类管理弹框不能依赖左侧是否展开，所以这里单独提供完整列表。
     */
    suspend fun listAll(): List<FoodCategory> = withContext(Dispatchers.Default) {
        q.selectAllFoodCategories().executeAsList().map { row ->
            FoodCategory(
                id = row.id,
                name = row.name,
                dimension = row.dimension,
                parentId = row.parent_id,
                crowdTypeId = row.crowd_type_id,
                sortOrder = row.sort_order.toInt(),
                icon = row.icon,
                source = row.source,
                hasChildren = q.countChildren(row.id).executeAsOne() > 0,
            )
        }
    }

    /**
     * 按 id 读取分类详情。[AI修改]
     */
    suspend fun get(id: Long): FoodCategory? = withContext(Dispatchers.Default) {
        q.selectCategoryById(id).executeAsOneOrNull()?.let { row ->
            FoodCategory(
                id = row.id,
                name = row.name,
                dimension = row.dimension,
                parentId = row.parent_id,
                crowdTypeId = row.crowd_type_id,
                sortOrder = row.sort_order.toInt(),
                icon = row.icon,
                source = row.source,
                hasChildren = q.countChildren(row.id).executeAsOne() > 0,
            )
        }
    }

    /**
     * 创建用户自建通用分类。[AI生成]
     *
     * 方案 A 只开放普通食材分类编辑；慢病/营养等维度继续作为预设体系维护。
     */
    suspend fun createUserCategory(name: String, parentId: Long?, icon: String = ""): Long = withContext(Dispatchers.Default) {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "分类名称不能为空" }
        parentId?.let { parent ->
            val parentCategory = get(parent)
            require(parentCategory?.dimension == "general" && parentCategory.crowdTypeId == null) {
                "只能在普通分类下新增子分类"
            }
        }
        q.insertFoodCategory(
            name = trimmedName,
            dimension = "general",
            parent_id = parentId,
            crowd_type_id = null,
            sort_order = DateTime.nowEpochSeconds(),
            icon = icon.trim(),
            source = "user",
            created_at = DateTime.nowEpochSeconds(),
        )
        q.lastInsertId().executeAsOne()
    }

    /**
     * 编辑用户自建通用分类。[AI生成]
     */
    suspend fun renameUserCategory(id: Long, name: String, icon: String = "") = withContext(Dispatchers.Default) {
        val category = get(id)
        require(category?.isEditableUserGeneralCategory() == true) { "预设分类不可编辑" }
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "分类名称不能为空" }
        q.updateUserFoodCategory(name = trimmedName, icon = icon.trim(), id = id)
    }

    /**
     * 软删除用户自建通用分类。[AI生成]
     *
     * 仅删除分类与食材的关系，不删除食材；有子分类时先阻止，避免形成不可见层级。
     */
    suspend fun deleteUserCategory(id: Long) = withContext(Dispatchers.Default) {
        db.transaction {
            val category = q.selectCategoryById(id).executeAsOneOrNull()?.let { row ->
                FoodCategory(
                    id = row.id,
                    name = row.name,
                    dimension = row.dimension,
                    parentId = row.parent_id,
                    crowdTypeId = row.crowd_type_id,
                    sortOrder = row.sort_order.toInt(),
                    icon = row.icon,
                    source = row.source,
                    hasChildren = q.countChildren(row.id).executeAsOne() > 0,
                )
            }
            require(category?.isEditableUserGeneralCategory() == true) { "预设分类不可删除" }
            require(q.countChildren(id).executeAsOne() == 0L) { "请先删除子分类" }
            q.unlinkIngredientCategoryByCategory(id) // [AI生成] 只解除分类关系，食材仍保留。
            q.softDeleteUserFoodCategory(id)
        }
    }

    /**
     * 判断分类是否属于方案 A 允许维护的范围。[AI生成]
     */
    private fun FoodCategory.isEditableUserGeneralCategory(): Boolean =
        source == "user" && dimension == "general" && crowdTypeId == null
}
