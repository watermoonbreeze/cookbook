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
        q.selectAllIngredients().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    /**
     * 关键词搜索食材。[AI修改]
     */
    suspend fun search(keyword: String): List<Ingredient> {
        if (keyword.isBlank()) return q.selectAllIngredients().executeAsList().map { it.toDomain() }
        return q.searchIngredients("%${keyword.trim()}%").executeAsList().map { it.toDomain() }
    }

    /**
     * 按普通食材分类查询。[AI修改]
     */
    suspend fun listByCategory(categoryId: Long): List<Ingredient> =
        q.selectIngredientsByCategory(categoryId).executeAsList().map { it.toDomain() }

    /**
     * 按健康人群查询食材，并附带推荐/限制/避免建议。[AI修改]
     */
    suspend fun listByCrowd(crowdTypeId: Long): List<Ingredient> =
        q.selectIngredientsByCrowd(crowdTypeId).executeAsList().map { row ->
            Ingredient(
                id = row.id,
                name = row.name,
                alias = row.alias,
                pinyin = row.pinyin,
                imagePath = row.image_path,
                defaultUnitId = row.default_unit_id,
                source = row.source,
                adviceLevel = AdviceLevel.fromCode(row.advice_level),
                adviceReason = row.reason,
            )
        }

    /**
     * 创建用户自定义食材。[AI修改]
     */
    suspend fun createUserIngredient(
        name: String,
        alias: String = "",
        imagePath: String = "",
        categoryId: Long? = null,
    ): Long {
        val now = DateTime.nowEpochSeconds()
        q.insertIngredient(
            name = name,
            alias = alias,
            pinyin = Pinyin.toPinyin(name),
            image_path = imagePath, // [AI修改] 新建食材时可保存可选图片路径，MVP 暂不接入系统相册。
            default_unit_id = null,
            source = "user",
            created_at = now,
        )
        val id = q.lastInsertId().executeAsOne()
        categoryId?.let { q.linkIngredientCategory(id, it) } // [AI修改] 新建食材时按用户选择绑定分类。
        return id
    }

    /**
     * 读取计量单位字典。[AI修改]
     */
    suspend fun listMeasurementUnits(): List<MeasurementUnit> =
        q.selectAllMeasurementUnits().executeAsList().map { MeasurementUnit(id = it.id, name = it.name) }

    /**
     * SQLDelight 数据库实体转领域模型。[AI修改]
     *
     * Kotlin 扩展函数写法，等价于 Java 工具方法 `IngredientMapper.toDomain(row)`。
     */
    private fun com.sxdbsm.cookbook.db.Ingredient.toDomain() = Ingredient(
        id = id,
        name = name,
        alias = alias,
        pinyin = pinyin,
        imagePath = image_path,
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
    suspend fun listTopLevel(): List<FoodCategory> =
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

    /**
     * 读取某个一级分类下的二级分类。[AI修改]
     */
    suspend fun listChildren(parentId: Long): List<FoodCategory> =
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

    /**
     * 按 id 读取分类详情。[AI修改]
     */
    suspend fun get(id: Long): FoodCategory? =
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
