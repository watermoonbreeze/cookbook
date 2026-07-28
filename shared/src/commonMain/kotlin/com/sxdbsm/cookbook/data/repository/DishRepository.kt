package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.sxdbsm.cookbook.ai.MealSlot
import com.sxdbsm.cookbook.ai.MealSlotMatcher
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.db.SelectDishForEditById
import com.sxdbsm.cookbook.domain.model.Dish
import com.sxdbsm.cookbook.domain.model.DishIngredientMatch
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.DishStep
import com.sxdbsm.cookbook.domain.model.CookingMethod
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.util.DateTime
import com.sxdbsm.cookbook.platform.ioDispatcher
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
    suspend fun listCookingMethods(): List<CookingMethod> = withContext(ioDispatcher) {
        q.selectAllCookingMethods().executeAsList().map { row ->
            CookingMethod(id = row.id, name = row.name, preset = row.source == "preset")
        }
    }

    /** 删除烹饪方式(仅用户自建，预设不可删)。[AI生成] T4 */
    suspend fun deleteCookingMethod(id: Long) = withContext(ioDispatcher) {
        q.softDeleteUserCookingMethod(id)
    }

    /** 标签库(预设+用户自建)。[AI生成] T3 */
    suspend fun listDishTags(): List<com.sxdbsm.cookbook.domain.model.DishTag> = withContext(ioDispatcher) {
        q.selectAllDishTags().executeAsList().map { row ->
            com.sxdbsm.cookbook.domain.model.DishTag(id = row.id, name = row.name, preset = row.source == "preset")
        }
    }

    /** 新建标签入库(按名幂等，返回是否新建)。[AI生成] T3 */
    suspend fun createDishTag(name: String) = withContext(ioDispatcher) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && q.selectDishTagByName(trimmed).executeAsOneOrNull() == null) {
            q.insertDishTag(trimmed, "user", DateTime.nowEpochSeconds())
        }
    }

    /** 删除标签(仅用户自建，预设不可删)。[AI生成] T3 */
    suspend fun deleteDishTag(id: Long) = withContext(ioDispatcher) {
        q.softDeleteUserDishTag(id)
    }

    /**
     * 按 id 批量取菜的封面/缩略图。[AI生成] 首页"下一餐"推荐卡显该菜真实图片(修"推荐图片与菜不符":原只显分类 emoji 常与具体菜不对应)。
     * 返回 id→(imagePath, thumbnailPath)；首页候选集很小,IN 展开安全(远低于 SQLite 999 变量上限)。
     */
    suspend fun dishImagesByIds(ids: List<Long>): Map<Long, Pair<String, String>> = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext emptyMap()
        q.selectDishImagesByIds(ids).executeAsList().associate { it.id to (it.image_path to it.thumbnail_path) }
    }

    /**
     * 按名称获取或创建烹饪方式。[AI修改]
     *
     * 新建/编辑菜品页支持多个烹饪方式；每个手动输入项都会先落到字典表，再写入关联表。
     */
    suspend fun ensureCookingMethod(name: String): Long? = withContext(ioDispatcher) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@withContext null
        q.insertCookingMethod(trimmed, "user", DateTime.nowEpochSeconds())
        // [AI修改] 按名精确查 id，替代全表拉取+内存 firstOrNull。
        q.selectCookingMethodIdByName(trimmed).executeAsOneOrNull()
    }

    /** 批量确保烹饪方式字典存在并返回 id。[AI生成] */
    private fun ensureCookingMethodIds(names: List<String>, now: Long): List<Long> =
        names.mapNotNull { raw ->
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return@mapNotNull null
            q.insertCookingMethod(trimmed, "user", now)
            q.selectCookingMethodIdByName(trimmed).executeAsOneOrNull() // [AI修改] 按名精确查，不再全表反查
        }.distinct()

    /**
     * 监听全部菜品的轻量列表。[AI修改]
     *
     * `Flow<List<...>>` 类似一个可持续推送数据变化的 Observable；数据库变更后 UI 会自动收到新列表。
     */
    fun observeAllDishes(): Flow<List<DishMini>> =
        q.selectAllDishes().asFlow().mapToList(ioDispatcher).map { rows ->
            buildDishMinis(
                rows.map { row ->
                    DishMiniSource(
                        id = row.id,
                        name = row.name,
                        imagePath = row.image_path,
                        thumbnailPath = row.thumbnail_path,
                        preference = row.preference.toInt(),
                        cookingMethodId = row.cooking_method_id,
                        source = row.source,
                        cuisine = row.cuisine,
                    )
                },
            )
        }.flowOn(ioDispatcher)

    /**
     * 监听喜爱度最高的菜品。[AI修改]
     */
    fun observePopularDishes(limit: Long = 6): Flow<List<DishMini>> =
        q.selectDishesByPopularity(limit).asFlow().mapToList(ioDispatcher).map { rows ->
            buildDishMinis(
                rows.map { row ->
                    DishMiniSource(
                        id = row.id,
                        name = row.name,
                        imagePath = row.image_path,
                        thumbnailPath = row.thumbnail_path,
                        preference = row.preference.toInt(),
                        cookingMethodId = row.cooking_method_id,
                        source = row.source,
                        cuisine = row.cuisine,
                    )
                },
            )
        }.flowOn(ioDispatcher)

    /**
     * 监听最近创建或更新的菜品。[AI修改]
     */
    fun observeRecentDishes(limit: Long = 6): Flow<List<DishMini>> =
        q.selectDishesByRecent(limit).asFlow().mapToList(ioDispatcher).map { rows ->
            buildDishMinis(
                rows.map { row ->
                    DishMiniSource(
                        id = row.id,
                        name = row.name,
                        imagePath = row.image_path,
                        thumbnailPath = row.thumbnail_path,
                        preference = row.preference.toInt(),
                        cookingMethodId = row.cooking_method_id,
                        source = row.source,
                        cuisine = row.cuisine,
                    )
                },
            )
        }.flowOn(ioDispatcher)

    /**
     * 按关键词搜索菜品。[AI修改]
     *
     * `suspend` 表示这是协程挂起函数，调用方要在协程里调用，类似异步方法但不用写回调。
     */
    suspend fun searchDishes(keyword: String): List<DishMini> = withContext(ioDispatcher) {
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
                        source = row.source,
                        cuisine = row.cuisine,
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
                        source = row.source,
                        cuisine = row.cuisine,
                    )
                },
            )
        }
    }

    /**
     * 按已有食材匹配可做菜品。[AI生成]
     *
     * 返回命中食材数和菜品总食材数，UI 可据此展示“食材齐全/差几项/部分匹配”。
     */
    suspend fun findDishesByIngredients(ingredientIds: List<Long>, limit: Long = 50): List<DishIngredientMatch> = withContext(ioDispatcher) {
        val ids = ingredientIds.distinct()
        if (ids.isEmpty()) return@withContext emptyList()
        val rows = q.selectDishesByIngredientMatch(ids, limit).executeAsList()
        // [AI修改] 消除 N+1：原来对每行调 buildDishMini(内部各跑4条查询)；改为一次性 buildDishMinis 批量取，
        // 再按顺序 zip 回 match/total 计数(buildDishMinis 保持输入顺序)。同时补齐 cuisine。
        val minis = buildDishMinis(
            rows.map { row ->
                DishMiniSource(
                    id = row.id, name = row.name, imagePath = row.image_path, thumbnailPath = row.thumbnail_path,
                    preference = row.preference.toInt(), cookingMethodId = row.cooking_method_id, source = row.source, cuisine = row.cuisine,
                )
            },
        )
        rows.zip(minis) { row, mini ->
            DishIngredientMatch(dish = mini, matchCount = row.match_count.toInt(), totalIngredientCount = row.total_count.toInt())
        }
    }

    /**
     * 全库菜品（列表用轻量模型），供 RANDOM 推荐候选。[AI生成]
     *
     * RANDOM 语义=整库都可做，不需要按在手食材求交集。故走无 `IN` 的 selectAllDishesForRandom，
     * 避开 findDishesByIngredients 把整库食材 id 展开成上千占位符导致的 SQLite 999 变量上限崩溃。
     * 按 preference+名排序取上限，复用 buildDishMinis 批量组装(同 searchDishes 空词分支范式)。
     */
    suspend fun listAllDishMinis(limit: Long = 50): List<DishMini> = withContext(ioDispatcher) {
        buildDishMinis(
            q.selectAllDishesForRandom(limit).executeAsList().map { row ->
                DishMiniSource(
                    id = row.id, name = row.name, imagePath = row.image_path, thumbnailPath = row.thumbnail_path,
                    preference = row.preference.toInt(), cookingMethodId = row.cooking_method_id, source = row.source, cuisine = row.cuisine,
                )
            },
        )
    }

    /**
     * 按 id 读取列表/选择器用的轻量菜品。[AI生成]
     *
     * 添加餐食页从“新建菜品”返回后只拿到新菜品 id，需要补成 `DishMini` 才能加入餐食模块。
     */
    suspend fun getDishMiniById(id: Long): DishMini? = withContext(ioDispatcher) {
        q.selectDishById(id).executeAsOneOrNull()?.let { row ->
            buildDishMini(
                id = row.id,
                name = row.name,
                imagePath = row.image_path,
                thumbnailPath = row.thumbnail_path,
                preference = row.preference.toInt(), // [AI修改] SQLDelight 表实体整数为 Long，领域列表模型使用 Int。
                cookingMethodId = row.cooking_method_id,
                source = row.source,
                cuisine = row.cuisine, // [AI修改] 补齐 cuisine(之前默认空，导致从选择器/AI回填的菜品丢菜系)
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
        // [AI生成] 主料名(is_main=1)：供食物分类图标/营养/主食判定；之前一直未填导致 mainIngredientNames 恒空。
        val mainNamesByDish = q.selectMainIngredientNamesByDishIds(dishIds)
            .executeAsList()
            .groupBy({ it.dish_id }, { it.ingredient_name })
        // [AI生成] 全部食材名(含非主料)：供营养大类判定的 main→all fallback。
        //   修 is_main 全0/缺标的问题菜(水煮蛋等)在预览时因 allIngredientNames 空→groups 空→误报"缺蛋白"。
        //   buildDishesByMealRecord 早已填充(J15修复)，此处补齐 buildDishMinis 侧(选菜器/预览/AI推荐)。
        val allNamesByDish = q.selectDishIngredientsByDishIds(dishIds)
            .executeAsList()
            .groupBy({ it.dish_id }, { it.ingredient_name })
        // [AI生成] v28：批量取菜的存储餐次(dish_meal_slot)；未打标(老库/新自建未 seed)回退 MealSlotMatcher 推断，恒非空。
        val mealSlotsByDish = q.selectMealSlotCodesByDishIds(dishIds)
            .executeAsList()
            .groupBy({ it.dish_id }, { MealSlot.fromCode(it.code) })
        return sources.map { source ->
            val relMethods = relMethodsByDish[source.id].orEmpty()
            val fallbackName = source.cookingMethodId?.let { cookingMethodNames[it] }
            val methodNames = relMethods.map { it.name }.ifEmpty { fallbackName?.let(::listOf).orEmpty() }
            // [AI生成] v28：存储餐次优先(滤掉未知 code 落到的 ALL)；未打标回退 Matcher 推断兜底(恒非空)。
            val storedSlots = mealSlotsByDish[source.id].orEmpty().filter { it != MealSlot.ALL }
            val mealSlots = storedSlots.ifEmpty { MealSlotMatcher.defaultSlotsFor(source.name) }
            DishMini(
                id = source.id,
                name = source.name,
                imagePath = source.imagePath,
                thumbnailPath = source.thumbnailPath,
                tags = tagsByDish[source.id].orEmpty(),
                preference = source.preference,
                mainIngredientNames = mainNamesByDish[source.id].orEmpty(),
                allIngredientNames = allNamesByDish[source.id].orEmpty(), // [AI生成] 全部食材名(含非主料)·供 fallback·修 is_main 全0误报
                cookingMethodName = methodNames.firstOrNull(),
                cookingMethodNames = methodNames,
                source = source.source,
                cuisine = source.cuisine,
                mealSlots = mealSlots,
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
        val source: String = "user",
        val cuisine: String = "", // [AI生成] 菜系
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
        source: String = "user",
        cuisine: String = "",
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
                    source = source,
                    cuisine = cuisine,
                ),
            ),
        ).first()

    /**
     * 监听某个菜品详情。[AI修改]
     */
    fun observeDishById(id: Long): Flow<Dish?> =
        q.selectDishForEditById(id).asFlow().mapToOneOrNull(ioDispatcher).map { row ->
            row?.let { loadFullDish(it) }
        }.flowOn(ioDispatcher)

    /**
     * 一次性读取某个菜品详情。[AI修改]
     */
    suspend fun getDishById(id: Long): Dish? = withContext(ioDispatcher) {
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
                    status = ing.ingredient_status.toInt(), // [AI生成] 携带失效态，供菜品里灰显失效食材。
                    reason = ing.ingredient_reason,
                ),
                quantity = ing.quantity,
                unitId = ing.unit_id,
                unitName = ing.unit_name.orEmpty(),
                isMain = ing.is_main == 1L,
            )
        }
        val steps = q.selectStepsOfDish(id).executeAsList().map { step ->
            DishStep(
                id = step.id,
                sortOrder = step.sort_order.toInt(),
                text = step.text,
                imagePath = step.image_path,
                thumbnailPath = step.thumbnail_path,
            )
        } // [AI生成] 编辑/详情统一读取菜品操作步骤，按 sort_order 保持用户录入顺序。
        // [AI生成] v28：读存储餐次(单列 code SELECT 返回 List<String>)；编辑回显真实存储值，空=老库未打标(由编辑页 Matcher 预选补齐)。
        val mealSlots = q.selectMealSlotCodesByDish(id).executeAsList()
            .map { MealSlot.fromCode(it) }.filter { it != MealSlot.ALL }
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
            cuisine = row.cuisine,
            createdAt = row.created_at,
            updatedAt = row.updated_at,
            tags = tags,
            ingredients = ingredients,
            steps = steps,
            mealSlots = mealSlots,
        )
    }

    /**
     * 保存一个菜品（含标签与食材）。新建时 id<=0，编辑时 id>0。[AI修改]
     */
    /** 按名查任意来源的有效菜品 id(不区分预设/自建)，用于"存为菜品"防重复建。[AI生成] */
    suspend fun dishIdByName(name: String): Long? = withContext(ioDispatcher) {
        q.selectDishIdByNameAny(name).executeAsOneOrNull()
    }

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
        steps: List<DishStep> = emptyList(),
        cuisine: String = "", // [AI生成] 菜系(可空)
        mealSlotCodes: List<String> = emptyList(), // [AI生成] v28：适合餐次 code(空则按菜名 Matcher 推断兜底)
    ): Long = withContext(ioDispatcher) {
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
                    cuisine = cuisine,
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
                    cuisine = cuisine,
                    id = id,
                )
            }

            // [AI生成] 同步多烹饪方式关联：主表只保留首个方式作兼容缓存，完整列表来自关联表。
            q.unlinkCookingMethodsOfDish(dishId)
            cookingMethodIds.forEach { methodId ->
                q.linkDishCookingMethod(dishId, methodId)
            }

            // [AI生成] v28：同步菜品适合餐次(全量替换)；空则按菜名 Matcher 推断兜底，保证"永不出现无餐次菜"。
            q.unlinkMealSlotsOfDish(dishId)
            val slotCodes = mealSlotCodes.filter { it.isNotBlank() }.distinct()
                .ifEmpty { MealSlotMatcher.defaultSlotsFor(name).map { it.code } }
            slotCodes.forEach { code ->
                val mealTypeId = q.selectMealTypeIdByCode(code).executeAsOneOrNull() ?: return@forEach
                q.linkDishMealSlot(dishId, mealTypeId)
            }

            // [AI修改] 同步标签：先清空关联再重建，避免编辑时残留旧标签。
            q.unlinkAllTagsOfDish(dishId)
            tagNames.distinct().forEach { tagName ->
                q.insertDishTag(name = tagName, source = "user", created_at = now)
                val tagId = q.selectDishTagByName(tagName).executeAsOneOrNull()?.id ?: return@forEach
                q.linkDishTag(dishId, tagId)
            }

            // [AI生成] 兜底解析"克"单位 id：编辑器加食材时若单位字典未就绪(gramUnit() 返 null)，unitId 会落 null。
            //   默认克数(SeasoningDefaults)本就是克，null 只来自时序 gap(用户显式选的单位都非 null)。存 NULL 会致
            //   重载详情按 default_unit_id 回退显"100.0个"、营养按错单位折算(踩坑红线)。此处统一回填"克"，让持久化永不留空单位。
            val gramUnitId = q.selectMeasurementUnitIdByName("g").executeAsOneOrNull()
                ?: q.selectMeasurementUnitIdByName("克").executeAsOneOrNull()
            // [AI修改] 同步食材：同样采用“全量替换”策略，简单且适合 MVP。
            q.deleteIngredientsOfDish(dishId)
            ingredients.forEach { di ->
                q.insertDishIngredient(
                    dish_id = dishId,
                    ingredient_id = di.ingredient.id,
                    quantity = di.quantity,
                    unit_id = di.unitId ?: gramUnitId, // [AI修改] 空单位回填"克"，防"100克显成100个"/营养算错(bug根因)
                    is_main = if (di.isMain) 1 else 0,
                )
                q.updateIngredientLastReferencedAt(
                    last_referenced_at = now,
                    id = di.ingredient.id,
                ) // [AI生成] 食材只要被保存进菜品，就进入“最近使用”并按最后引用时间排序。
            }

            // [AI生成] 同步操作步骤：MVP 采用全量替换，避免编辑时出现旧步骤和新步骤交叉残留。
            q.deleteStepsOfDish(updated_at = now, dish_id = dishId)
            steps
                .filter { it.text.isNotBlank() || it.imagePath.isNotBlank() || it.thumbnailPath.isNotBlank() }
                .forEachIndexed { index, step ->
                    q.insertDishStep(
                        dish_id = dishId,
                        sort_order = index.toLong(),
                        text = step.text.trim(),
                        image_path = step.imagePath,
                        thumbnail_path = step.thumbnailPath,
                        created_at = now,
                        updated_at = now,
                    )
                }
        }
        dishId
    }

    /**
     * 删除用户菜品。[AI修改]
     */
    suspend fun deleteDish(id: Long) = withContext(ioDispatcher) {
        db.transaction {
            q.deleteFavoriteComboDishesByDish(id) // [AI生成] favorite_combo_dish 对 dish 没有级联删除，先清关联避免外键失败。
            q.deleteDish(id)
        }
    }

    /** 收藏的菜品 id 集合(置顶用)。[AI生成] B1 */
    suspend fun favoriteDishIds(): Set<Long> = withContext(ioDispatcher) {
        q.selectFavoriteDishIds().executeAsList().toSet()
    }

    /** 设置/取消菜品收藏。[AI生成] B1 */
    suspend fun setDishFavorite(dishId: Long, favorite: Boolean) = withContext(ioDispatcher) {
        if (favorite) q.insertDishFavorite(dishId, DateTime.nowEpochSeconds()) else q.deleteDishFavorite(dishId)
    }

    /** 标记/取消"不再推荐"(负反馈踩)。[AI生成] 仅用户显式标才降权；推荐取数按此过滤。 */
    suspend fun setDishDisliked(dishId: Long, disliked: Boolean) = withContext(ioDispatcher) {
        if (disliked) q.insertDishDislike(dishId, DateTime.nowEpochSeconds()) else q.deleteDishDislike(dishId)
    }

    /** 某菜是否已标"不再推荐"(菜品详情恢复入口用)。[AI生成] */
    suspend fun isDishDisliked(dishId: Long): Boolean = withContext(ioDispatcher) {
        q.isDishDisliked(dishId).executeAsOne()
    }

    /** 菜品做过(记入餐食)的次数与最近日期。[AI生成] 详情页"做过N次"。 */
    suspend fun cookStats(dishId: Long): Pair<Int, String?> = withContext(ioDispatcher) {
        val r = q.selectDishCookStats(dishId).executeAsOne()
        r.cnt.toInt() to r.last_date
    }

    /**
     * 查询菜品被哪些餐食记录引用。[AI生成]
     */
    suspend fun listMealReferencesByDish(id: Long): List<DishMealReference> = withContext(ioDispatcher) {
        q.selectMealReferencesByDish(id).executeAsList().map { row ->
            DishMealReference(
                date = row.date,
                mealName = row.meal_name,
                mealTime = row.meal_time,
            )
        }
    }

    /**
     * 批量加载菜品配料信息（ingredientId + 是否主料）。[AI生成] Phase 2 成员红绿灯：列表徽章批量评估用。
     *
     * 复用已有 [selectDishIngredientsByDishIds] 查询，一次 IO 取多道菜的配料 ID/isMain，
     * 供 [MemberDishHealthUseCase] 批量评估时免 N+1。空入参返空 map（守 SQLite IN 空列表守卫）。
     */
    suspend fun loadDishIngredientInfo(dishIds: List<Long>): Map<Long, List<DishIngredientInfo>> = withContext(ioDispatcher) {
        if (dishIds.isEmpty()) return@withContext emptyMap()
        q.selectDishIngredientsByDishIds(dishIds).executeAsList().groupBy(
            { it.dish_id },
            { DishIngredientInfo(ingredientId = it.ingredient_id, ingredientName = it.ingredient_name, isMain = it.is_main != 0L) },
        )
    }
}

/** 菜品配料轻量信息（批查用，避免逐菜 loadFullDish）。[AI生成] Phase 2 成员红绿灯：列表徽章批量评估。 */
data class DishIngredientInfo(
    val ingredientId: Long,
    val ingredientName: String,
    val isMain: Boolean,
)
