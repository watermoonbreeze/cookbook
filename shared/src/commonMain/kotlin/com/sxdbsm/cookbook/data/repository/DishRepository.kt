package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.db.SelectDishForEditById
import com.sxdbsm.cookbook.domain.model.Dish
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.CookingMethod
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 菜品数据仓库。[AI修改]
 *
 * Repository 是 UI/ViewModel 与 SQLDelight 之间的隔离层，类似 Java 项目里的 DAO + Service 组合。
 * ViewModel 只依赖这里的领域方法，不直接操作 SQL 查询对象。
 */
class DishRepository(private val db: CookbookDatabase) {

    private val q = db.cookbookQueries // [AI修改] SQLDelight 根据 .sq 文件生成的类型安全查询入口。

    /**
     * 读取全部烹饪方式字典。[AI生成]
     *
     * 新建/编辑菜品页需要展示下拉候选；返回领域模型，避免 UI 直接依赖 SQLDelight 行类型。
     */
    suspend fun listCookingMethods(): List<CookingMethod> = withContext(Dispatchers.Default) {
        q.selectAllCookingMethods().executeAsList().map { row ->
            CookingMethod(id = row.id, name = row.name)
        }
    }

    /**
     * 按名称获取或创建烹饪方式。[AI修改]
     *
     * 新建/编辑菜品页支持多个烹饪方式；每个手动输入项都会先落到字典表，再写入关联表。
     */
    suspend fun ensureCookingMethod(name: String): Long? = withContext(Dispatchers.Default) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@withContext null
        q.insertCookingMethod(trimmed, "user", DateTime.nowEpochSeconds())
        q.selectAllCookingMethods().executeAsList().firstOrNull { it.name == trimmed }?.id
    }

    /** 批量确保烹饪方式字典存在并返回 id。[AI生成] */
    private fun ensureCookingMethodIds(names: List<String>, now: Long): List<Long> =
        names.mapNotNull { raw ->
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return@mapNotNull null
            q.insertCookingMethod(trimmed, "user", now)
            q.selectAllCookingMethods().executeAsList().firstOrNull { it.name == trimmed }?.id
        }.distinct()

    /**
     * 监听全部菜品的轻量列表。[AI修改]
     *
     * `Flow<List<...>>` 类似一个可持续推送数据变化的 Observable；数据库变更后 UI 会自动收到新列表。
     */
    fun observeAllDishes(): Flow<List<DishMini>> =
        q.selectAllDishes().asFlow().mapToList(Dispatchers.Default).map { rows ->
            buildDishMinis(
                rows.map { row ->
                    DishMiniSource(
                        id = row.id,
                        name = row.name,
                        imagePath = row.image_path,
                        thumbnailPath = row.thumbnail_path,
                        preference = row.preference.toInt(),
                        cookingMethodId = row.cooking_method_id,
                    )
                },
            )
        }.flowOn(Dispatchers.Default)

    /**
     * 监听喜爱度最高的菜品。[AI修改]
     */
    fun observePopularDishes(limit: Long = 6): Flow<List<DishMini>> =
        q.selectDishesByPopularity(limit).asFlow().mapToList(Dispatchers.Default).map { rows ->
            buildDishMinis(
                rows.map { row ->
                    DishMiniSource(
                        id = row.id,
                        name = row.name,
                        imagePath = row.image_path,
                        thumbnailPath = row.thumbnail_path,
                        preference = row.preference.toInt(),
                        cookingMethodId = row.cooking_method_id,
                    )
                },
            )
        }.flowOn(Dispatchers.Default)

    /**
     * 监听最近创建或更新的菜品。[AI修改]
     */
    fun observeRecentDishes(limit: Long = 6): Flow<List<DishMini>> =
        q.selectDishesByRecent(limit).asFlow().mapToList(Dispatchers.Default).map { rows ->
            buildDishMinis(
                rows.map { row ->
                    DishMiniSource(
                        id = row.id,
                        name = row.name,
                        imagePath = row.image_path,
                        thumbnailPath = row.thumbnail_path,
                        preference = row.preference.toInt(),
                        cookingMethodId = row.cooking_method_id,
                    )
                },
            )
        }.flowOn(Dispatchers.Default)

    /**
     * 按关键词搜索菜品。[AI修改]
     *
     * `suspend` 表示这是协程挂起函数，调用方要在协程里调用，类似异步方法但不用写回调。
     */
    suspend fun searchDishes(keyword: String): List<DishMini> = withContext(Dispatchers.Default) {
        if (keyword.isBlank()) {
            buildDishMinis(
                q.selectAllDishes().executeAsList().map { row ->
                    DishMiniSource(
                        id = row.id,
                        name = row.name,
                        imagePath = row.image_path,
                        thumbnailPath = row.thumbnail_path,
                        preference = row.preference.toInt(),
                        cookingMethodId = row.cooking_method_id,
                    )
                },
            )
        } else {
            val kw = "%${keyword.trim()}%"
            buildDishMinis(
                q.searchDishes(kw).executeAsList().map { row ->
                    DishMiniSource(
                        id = row.id,
                        name = row.name,
                        imagePath = row.image_path,
                        thumbnailPath = row.thumbnail_path,
                        preference = row.preference.toInt(),
                        cookingMethodId = row.cooking_method_id,
                    )
                },
            )
        }
    }

    /**
     * 按 id 读取列表/选择器用的轻量菜品。[AI生成]
     *
     * 添加餐食页从“新建菜品”返回后只拿到新菜品 id，需要补成 `DishMini` 才能加入餐食模块。
     */
    suspend fun getDishMiniById(id: Long): DishMini? = withContext(Dispatchers.Default) {
        q.selectDishById(id).executeAsOneOrNull()?.let { row ->
            buildDishMini(
                id = row.id,
                name = row.name,
                imagePath = row.image_path,
                thumbnailPath = row.thumbnail_path,
                preference = row.preference.toInt(), // [AI修改] SQLDelight 表实体整数为 Long，领域列表模型使用 Int。
                cookingMethodId = row.cooking_method_id,
            )
        }
    }

    /**
     * 批量组装列表用菜品模型。[AI修改]
     *
     * 原先每个菜品都会单独查询一次标签和一次烹饪方式，列表返回时容易形成 N+1 查询。
     * 这里对同一批菜品统一预取标签和字典，再在内存中合并，减少首页/选择器/食历切换卡顿。
     */
    internal fun buildDishMinis(sources: List<DishMiniSource>): List<DishMini> {
        if (sources.isEmpty()) return emptyList()
        val dishIds = sources.map { it.id }.distinct()
        val cookingMethodNames = q.selectAllCookingMethods()
            .executeAsList()
            .associate { it.id to it.name }
        val relMethodsByDish = q.selectCookingMethodsByDishIds(dishIds)
            .executeAsList()
            .groupBy({ it.dish_id }, { CookingMethod(id = it.id, name = it.name) })
        val tagsByDish = q.selectTagsByDishIds(dishIds)
            .executeAsList()
            .groupBy({ it.dish_id }, { it.name })
        return sources.map { source ->
            val relMethods = relMethodsByDish[source.id].orEmpty()
            val fallbackName = source.cookingMethodId?.let { cookingMethodNames[it] }
            val methodNames = relMethods.map { it.name }.ifEmpty { fallbackName?.let(::listOf).orEmpty() }
            DishMini(
                id = source.id,
                name = source.name,
                imagePath = source.imagePath,
                thumbnailPath = source.thumbnailPath,
                tags = tagsByDish[source.id].orEmpty(),
                preference = source.preference,
                cookingMethodName = methodNames.firstOrNull(),
                cookingMethodNames = methodNames,
            )
        }
    }

    /**
     * 列表模型组装的中间行数据。[AI生成]
     */
    internal data class DishMiniSource(
        val id: Long,
        val name: String,
        val imagePath: String,
        val thumbnailPath: String,
        val preference: Int,
        val cookingMethodId: Long?,
    )

    /**
     * 菜品被餐食引用的位置。[AI生成]
     *
     * 删除菜品前用于给用户明确提示“被哪一天的哪个餐次引用”。
     */
    data class DishMealReference(
        val date: String,
        val mealName: String,
        val mealTime: String,
    )

    /**
     * 统一组装单个列表用菜品模型，保留给详情附近的少量单条读取场景。[AI修改]
     */
    private fun buildDishMini(
        id: Long,
        name: String,
        imagePath: String,
        thumbnailPath: String,
        preference: Int,
        cookingMethodId: Long?,
    ): DishMini =
        buildDishMinis(
            listOf(
                DishMiniSource(
                    id = id,
                    name = name,
                    imagePath = imagePath,
                    thumbnailPath = thumbnailPath,
                    preference = preference,
                    cookingMethodId = cookingMethodId,
                ),
            ),
        ).first()

    /**
     * 监听某个菜品详情。[AI修改]
     */
    fun observeDishById(id: Long): Flow<Dish?> =
        q.selectDishForEditById(id).asFlow().mapToOneOrNull(Dispatchers.Default).map { row ->
            row?.let { loadFullDish(it) }
        }.flowOn(Dispatchers.Default)

    /**
     * 一次性读取某个菜品详情。[AI修改]
     */
    suspend fun getDishById(id: Long): Dish? = withContext(Dispatchers.Default) {
        q.selectDishForEditById(id).executeAsOneOrNull()?.let { row -> loadFullDish(row) }
    }

    /**
     * 把数据库行组装成完整领域对象。[AI修改]
     *
     * SQLDelight 的主表行只含 dish 自身字段，标签和食材需要额外查询后合并。
     */
    private fun loadFullDish(row: SelectDishForEditById): Dish {
        val id = row.id // [AI修改] 编辑/详情统一使用安全查询返回的真实主键，避免路由 id 与行 id 混用。
        val relMethods = q.selectCookingMethodsByDish(id).executeAsList().map { CookingMethod(id = it.id, name = it.name) }
        val fallbackMethod = row.cooking_method_id?.let { q.selectCookingMethodById(it).executeAsOneOrNull() }
            ?.let { CookingMethod(id = it.id, name = it.name) }
        val methods = relMethods.ifEmpty { fallbackMethod?.let(::listOf).orEmpty() }
        val tags = q.selectTagsByDish(id).executeAsList().map { it.name }
        val ingredients = q.selectIngredientsOfDish(id).executeAsList().map { ing ->
            DishIngredient(
                ingredient = Ingredient(
                    id = ing.ingredient_id,
                    name = ing.ingredient_name,
                    alias = ing.alias,
                    pinyin = ing.pinyin,
                    imagePath = ing.image_path,
                    thumbnailPath = ing.thumbnail_path,
                    emoji = ing.emoji,
                    defaultUnitId = ing.default_unit_id,
                ),
                quantity = ing.quantity,
                unitId = ing.unit_id,
                unitName = ing.unit_name.orEmpty(),
                isMain = ing.is_main == 1L,
            )
        }
        return Dish(
            id = row.id,
            name = row.name,
            cookingMethodId = methods.firstOrNull()?.id ?: row.cooking_method_id,
            cookingMethodName = methods.firstOrNull()?.name,
            cookingMethods = methods,
            preference = row.preference.toInt(),
            specialNote = row.special_note,
            description = row.description,
            imagePath = row.image_path,
            thumbnailPath = row.thumbnail_path,
            source = row.source,
            createdAt = row.created_at,
            updatedAt = row.updated_at,
            tags = tags,
            ingredients = ingredients,
        )
    }

    /**
     * 保存一个菜品（含标签与食材）。新建时 id<=0，编辑时 id>0。[AI修改]
     */
    suspend fun saveDish(
        id: Long,
        name: String,
        cookingMethodId: Long?,
        cookingMethodNames: List<String> = emptyList(),
        specialNote: String,
        description: String,
        imagePath: String,
        thumbnailPath: String,
        tagNames: List<String>,
        ingredients: List<DishIngredient>,
    ): Long = withContext(Dispatchers.Default) {
        val now = DateTime.nowEpochSeconds()
        var dishId = id
        db.transaction {
            val cookingMethodIds = (listOfNotNull(cookingMethodId) + ensureCookingMethodIds(cookingMethodNames, now)).distinct()
            val primaryCookingMethodId = cookingMethodIds.firstOrNull()
            // [AI修改] 菜品主表、标签、食材关联一次性提交，避免列表监听收到半完成状态。
            if (id <= 0) {
                q.insertDish(
                    name = name,
                    cooking_method_id = primaryCookingMethodId,
                    special_note = specialNote,
                    description = description,
                    image_path = imagePath,
                    thumbnail_path = thumbnailPath,
                    source = "user",
                    created_at = now,
                    updated_at = now,
                )
                dishId = q.lastInsertId().executeAsOne()
            } else {
                q.updateDish(
                    name = name,
                    cooking_method_id = primaryCookingMethodId,
                    special_note = specialNote,
                    description = description,
                    image_path = imagePath,
                    thumbnail_path = thumbnailPath,
                    updated_at = now,
                    id = id,
                )
            }

            // [AI生成] 同步多烹饪方式关联：主表只保留首个方式作兼容缓存，完整列表来自关联表。
            q.unlinkCookingMethodsOfDish(dishId)
            cookingMethodIds.forEach { methodId ->
                q.linkDishCookingMethod(dishId, methodId)
            }

            // [AI修改] 同步标签：先清空关联再重建，避免编辑时残留旧标签。
            q.unlinkAllTagsOfDish(dishId)
            tagNames.distinct().forEach { tagName ->
                q.insertDishTag(name = tagName, source = "user", created_at = now)
                val tagId = q.selectDishTagByName(tagName).executeAsOneOrNull()?.id ?: return@forEach
                q.linkDishTag(dishId, tagId)
            }

            // [AI修改] 同步食材：同样采用“全量替换”策略，简单且适合 MVP。
            q.deleteIngredientsOfDish(dishId)
            ingredients.forEach { di ->
                q.insertDishIngredient(
                    dish_id = dishId,
                    ingredient_id = di.ingredient.id,
                    quantity = di.quantity,
                    unit_id = di.unitId,
                    is_main = if (di.isMain) 1 else 0,
                )
            }
        }
        dishId
    }

    /**
     * 删除用户菜品。[AI修改]
     */
    suspend fun deleteDish(id: Long) = withContext(Dispatchers.Default) {
        db.transaction {
            q.deleteFavoriteComboDishesByDish(id) // [AI生成] favorite_combo_dish 对 dish 没有级联删除，先清关联避免外键失败。
            q.deleteDish(id)
        }
    }

    /**
     * 查询菜品被哪些餐食记录引用。[AI生成]
     */
    suspend fun listMealReferencesByDish(id: Long): List<DishMealReference> = withContext(Dispatchers.Default) {
        q.selectMealReferencesByDish(id).executeAsList().map { row ->
            DishMealReference(
                date = row.date,
                mealName = row.meal_name,
                mealTime = row.meal_time,
            )
        }
    }
}
