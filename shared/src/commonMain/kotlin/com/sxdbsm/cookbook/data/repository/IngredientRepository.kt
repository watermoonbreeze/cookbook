package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientDetail
import com.sxdbsm.cookbook.domain.model.MeasurementUnit
import com.sxdbsm.cookbook.platform.Pinyin
import com.sxdbsm.cookbook.util.DateTime
import com.sxdbsm.cookbook.platform.ioDispatcher
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
        q.selectAllIngredients(::mapIngredientRow).asFlow().mapToList(ioDispatcher)

    /**
     * 关键词搜索食材。[AI修改]
     */
    suspend fun search(keyword: String): List<Ingredient> = withContext(ioDispatcher) {
        if (keyword.isBlank()) {
            q.selectAllIngredients(::mapIngredientRow).executeAsList()
        } else {
            q.searchIngredients("%${keyword.trim()}%", ::mapIngredientRow).executeAsList()
        }
    }

    /**
     * 按普通食材分类查询。[AI修改]
     */
    suspend fun listByCategory(categoryId: Long): List<Ingredient> = withContext(ioDispatcher) {
        q.selectIngredientsByCategory(categoryId, ::mapIngredientRow).executeAsList()
    }

    /**
     * 读取最近被餐食引用过的食材。[AI生成]
     *
     * 这是食材选择器左侧“最近使用”虚拟分类的数据源，不落库成真实分类。
     * 只要食材被保存进菜品，就会按最后引用时间展示在这里。[AI修改]
     */
    suspend fun listRecentlyUsed(): List<Ingredient> = withContext(ioDispatcher) {
        q.selectRecentlyUsedIngredients(::mapIngredientRow).executeAsList()
    }

    /**
     * 按健康人群查询食材，并附带推荐/限制/避免建议。[AI修改]
     */
    suspend fun listByCrowd(crowdTypeId: Long): List<Ingredient> = withContext(ioDispatcher) {
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
        defaultUnitId: Long? = null,
        categoryIds: List<Long> = categoryId?.let { listOf(it) }.orEmpty(),
    ): Long = withContext(ioDispatcher) {
        val now = DateTime.nowEpochSeconds()
        // [AI修改] 同名即复用：新建前先按去空格的名字查已有食材，命中则直接返回其 id、不再新建重复行。
        // 修复根因——用户新建菜品时若又建了个同名食材(如"五花肉")会得到不同 id，与库存里预置食材对不上，
        // 导致按 ingredient_id 匹配的库存推荐漏掉该菜。复用后同名食材始终同一 id。
        val trimmed = name.trim()
        // [AI修改] H1：先精确匹配(快)，未命中再按**归一名**比对(去内部/全角空格 + 小写)，让
        // "五花肉"/"五 花 肉"/"Egg"/"egg" 复用同一行，避免同名多 id 与下游按名匹配漏配。
        val existingId = q.selectActiveIngredientIdByName(trimmed).executeAsOneOrNull()
            ?: run {
                val key = normalizeNameKey(trimmed)
                q.selectActiveIngredientIdNames().executeAsList()
                    .firstOrNull { normalizeNameKey(it.name) == key }?.id
            }
        if (existingId != null) {
            // [AI修改] 复用已有同名食材，同时补绑本次传入的分类(linkIngredientCategory 为 INSERT OR REPLACE 幂等)。
            categoryIds.distinct().forEach { q.linkIngredientCategory(existingId, it) }
            return@withContext existingId
        }
        q.insertIngredient(
            name = trimmed,
            alias = alias,
            pinyin = Pinyin.toPinyin(trimmed), // [AI修改] L2：用 trimmed 生成拼音，避免首尾空格污染拼音索引/搜索。
            image_path = imagePath, // [AI修改] 新建食材时可保存可选图片路径，MVP 暂不接入系统相册。
            thumbnail_path = thumbnailPath, // [AI生成] 新建食材时保存缩略图路径，列表优先展示。
            emoji = "🥗", // [AI生成] 用户自建食材没有 JSON 预置图标时先使用通用食物图标。
            default_unit_id = defaultUnitId,
            source = "user",
            created_at = now,
        )
        val id = q.lastInsertId().executeAsOne()
        categoryIds.distinct().forEach { q.linkIngredientCategory(id, it) } // [AI修改] 新建食材时按多分类选择绑定分类。
        id
    }

    /**
     * 食材名归一 key（防重复匹配用）。[AI生成] H1
     *
     * 去首尾/内部空白(含全角空格 　、制表符) + 小写，让不同书写(空格/大小写)的同一食材归到同一 key。
     */
    private fun normalizeNameKey(raw: String): String =
        raw.trim().replace(Regex("[\\s\\u3000]"), "").lowercase()

    /**
     * 读取食材已绑定的分类 id。[AI生成]
     */
    suspend fun listCategoryIds(ingredientId: Long): List<Long> = withContext(ioDispatcher) {
        q.selectCategoryIdsByIngredient(ingredientId).executeAsList()
    }

    /**
     * 读取食材绑定的完整分类信息。[AI生成]
     */
    suspend fun listCategories(ingredientId: Long): List<FoodCategory> = withContext(ioDispatcher) {
        q.selectCategoriesByIngredient(ingredientId).executeAsList().map { row ->
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
     * 按多个分类筛选食材。[AI生成]
     */
    suspend fun listByCategories(categoryIds: List<Long>): List<Ingredient> = withContext(ioDispatcher) {
        if (categoryIds.isEmpty()) return@withContext search("")
        q.selectIngredientsByCategoryIds(categoryIds, ::mapIngredientRow).executeAsList()
    }

    /**
     * 调养 tab：按病种分类通过调养规则聚合食材。[AI生成]
     *
     * 调养维度的数据在 ingredient_care_rule 表而非 ingredient_category 关联表；
     * 返回的 Ingredient 附带 adviceLevel/adviceReason，同一食材命中多个分类时由调用方去重分组。
     */
    suspend fun listByCareCategories(categoryIds: List<Long>): List<Ingredient> = withContext(ioDispatcher) {
        if (categoryIds.isEmpty()) return@withContext emptyList()
        q.selectIngredientsByCareCategories(categoryIds) { id, name, alias, pinyin, imagePath, thumbnailPath, emoji, defaultUnitId, source, createdAt, adviceLevel, reason ->
            mapIngredientRow(id, name, alias, pinyin, imagePath, thumbnailPath, emoji, defaultUnitId, source, createdAt)
                .copy(adviceLevel = AdviceLevel.fromCode(adviceLevel), adviceReason = reason)
        }.executeAsList()
    }

    /**
     * 重置食材分类关系。[AI生成]
     *
     * 新版新增/编辑食材会一次选择常规、营养、调养等多个分类，这里用事务保证关系整体替换。
     */
    suspend fun replaceIngredientCategories(ingredientId: Long, categoryIds: List<Long>) = withContext(ioDispatcher) {
        db.transaction {
            q.unlinkIngredientCategoriesByIngredient(ingredientId)
            categoryIds.distinct().forEach { categoryId ->
                q.linkIngredientCategory(ingredientId, categoryId)
            }
        }
    }

    /**
     * 保存食材详情扩展信息。[AI生成]
     */
    suspend fun saveIngredientDetail(detail: IngredientDetail) = withContext(ioDispatcher) {
        q.upsertIngredientDetail(
            ingredient_id = detail.ingredientId,
            common_methods = detail.commonMethods.trim(),
            prep_tips = detail.prepTips.trim(),
            eating_notes = detail.eatingNotes.trim(),
            storage_tips = detail.storageTips.trim(),
            health_note = detail.healthNote.trim(),
            updated_at = DateTime.nowEpochSeconds(),
        )
    }

    /**
     * 读取食材详情扩展信息。[AI生成]
     */
    suspend fun getIngredientDetail(ingredientId: Long): IngredientDetail? = withContext(ioDispatcher) {
        q.selectIngredientDetail(ingredientId).executeAsOneOrNull()?.let { row ->
            IngredientDetail(
                ingredientId = row.ingredient_id,
                commonMethods = row.common_methods,
                prepTips = row.prep_tips,
                eatingNotes = row.eating_notes,
                storageTips = row.storage_tips,
                healthNote = row.health_note,
                updatedAt = row.updated_at,
            )
        }
    }

    /**
     * 重置食材调养规则。[AI生成]
     */
    suspend fun replaceCareRules(ingredientId: Long, rules: List<IngredientCareRule>) = withContext(ioDispatcher) {
        db.transaction {
            q.clearIngredientCareRules(ingredientId)
            rules.distinctBy { it.categoryId }.forEach { rule ->
                q.upsertIngredientCareRule(
                    ingredient_id = ingredientId,
                    category_id = rule.categoryId,
                    advice_level = rule.adviceLevel.code(),
                    reason = rule.reason.trim(),
                    source = rule.source,
                )
            }
        }
    }

    /**
     * 读取食材调养规则。[AI生成]
     */
    suspend fun listCareRules(ingredientId: Long): List<IngredientCareRule> = withContext(ioDispatcher) {
        q.selectCareRulesByIngredient(ingredientId).executeAsList().map { row ->
            IngredientCareRule(
                id = row.id,
                ingredientId = row.ingredient_id,
                categoryId = row.category_id,
                categoryName = row.category_name,
                adviceLevel = AdviceLevel.fromCode(row.advice_level) ?: AdviceLevel.LIMIT,
                reason = row.reason,
                source = row.source,
            )
        }
    }

    /**
     * 读取计量单位字典。[AI修改]
     */
    suspend fun listMeasurementUnits(): List<MeasurementUnit> = withContext(ioDispatcher) {
        q.selectAllMeasurementUnits().executeAsList().map { MeasurementUnit(id = it.id, name = it.name) }
    }

    /** 一组食材的营养维度标签(去重)。[AI生成] 菜品详情营养概要(标签版)。 */
    suspend fun nutritionTagsOf(ingredientIds: List<Long>): List<String> = withContext(ioDispatcher) {
        if (ingredientIds.isEmpty()) return@withContext emptyList()
        val set = ingredientIds.toSet()
        q.selectNutritionSeasonTags().executeAsList()
            .filter { it.dim == "nutrition" && it.ingredient_id in set }
            .map { it.tag_name }
            .distinct()
    }

    /**
     * 编辑食材基础信息。[AI修改]
     *
     * 修复9要求预设和自建食材都可以编辑；删除仍只允许自建食材。
     */
    suspend fun updateUserIngredient(
        id: Long,
        name: String,
        alias: String,
        imagePath: String,
        thumbnailPath: String,
        defaultUnitId: Long? = null,
    ) = withContext(ioDispatcher) {
        q.updateUserIngredient(
            name = name,
            alias = alias,
            image_path = imagePath,
            thumbnail_path = thumbnailPath,
            default_unit_id = defaultUnitId,
            id = id,
        )
    }

    /**
     * 删除用户自建食材（软删除/失效）。[AI修改]
     *
     * 预设食材不能删除。改为“失效保留”语义：只置 status=0 并记录失效原因，
     * **不再清理 dish_ingredient 关联**——被菜品引用的食材要在菜品里灰显保留、不断裂。
     * 失效食材从食材列表/选择器隐藏，可在“已失效”回收站恢复或彻底删除。
     */
    suspend fun deleteUserIngredient(id: Long, reason: String = "用户删除") = withContext(ioDispatcher) {
        q.deleteUserIngredient(reason = reason, id = id)
    }

    /**
     * 恢复失效的自定义食材。[AI生成]
     */
    suspend fun restoreUserIngredient(id: Long) = withContext(ioDispatcher) {
        q.restoreUserIngredient(id)
    }

    /**
     * 彻底删除失效的自定义食材（回收站硬删）。[AI生成]
     *
     * 真删除；被菜品引用的 dish_ingredient 关系随外键级联清除，请在 UI 侧先提示用户确认。
     */
    suspend fun hardDeleteUserIngredient(id: Long) = withContext(ioDispatcher) {
        db.transaction {
            q.hardDeleteDishIngredientsByIngredient(id) // 物理清除菜品关联，避免外键悬挂。
            q.hardDeleteUserIngredient(id)
        }
    }

    /**
     * 回收站：列出失效的自定义食材。[AI生成]
     */
    suspend fun listInactiveUserIngredients(): List<Ingredient> = withContext(ioDispatcher) {
        q.selectInactiveUserIngredients { id, name, alias, pinyin, imagePath, thumbnailPath, emoji, defaultUnitId, source, createdAt, reason ->
            Ingredient(
                id = id,
                name = name,
                alias = alias,
                pinyin = pinyin,
                imagePath = imagePath,
                thumbnailPath = thumbnailPath,
                emoji = emoji,
                defaultUnitId = defaultUnitId,
                source = source,
                status = 0,
                reason = reason,
            )
        }.executeAsList()
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

    /**
     * 调养建议等级落库 code。[AI生成]
     */
    private fun AdviceLevel.code(): String = when (this) {
        AdviceLevel.RECOMMEND -> "recommend"
        AdviceLevel.LIMIT -> "limit"
        AdviceLevel.AVOID -> "avoid"
    }
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
    suspend fun listTopLevel(): List<FoodCategory> = withContext(ioDispatcher) {
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
     * 读取某个分类下的直接子分类。[AI修改]
     *
     * 分类树已支持多级结构，返回每个子节点时同步计算它是否还有下一层。
     */
    suspend fun listChildren(parentId: Long): List<FoodCategory> = withContext(ioDispatcher) {
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
                hasChildren = q.countChildren(row.id).executeAsOne() > 0, // [AI修改] 多级分类树需要每层都能继续展开。
            )
        }
    }

    /**
     * 读取全部有效分类。[AI生成]
     *
     * 添加食材弹框和分类管理弹框不能依赖左侧是否展开，所以这里单独提供完整列表。
     */
    suspend fun listAll(): List<FoodCategory> = withContext(ioDispatcher) {
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
    suspend fun get(id: Long): FoodCategory? = withContext(ioDispatcher) {
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
    suspend fun createUserCategory(name: String, parentId: Long?, icon: String = ""): Long = withContext(ioDispatcher) {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "分类名称不能为空" }
        parentId?.let { parent ->
            val parentCategory = get(parent)
            require(parentCategory?.isEditableUserGeneralCategory() == true) {
                "只能在自定义分类下新增子分类"
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
    suspend fun renameUserCategory(id: Long, name: String, icon: String = "") = withContext(ioDispatcher) {
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
    suspend fun deleteUserCategory(id: Long) = withContext(ioDispatcher) {
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
