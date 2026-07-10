package com.sxdbsm.cookbook.sync

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FavoriteComboRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.DishStep
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientDetail
import com.sxdbsm.cookbook.platform.ioDispatcher
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * @File : SyncRepository
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 双设备选择性同步——导出/合并导入（P1：食材 + 菜品）
 * <p>
 * 导出：采 source=user 食材(+被菜品引用的食材)与自建菜品 → `SyncBundle`。
 * 合并：按 name 匹配 upsert，食材缺则建(同名复用不覆盖)，菜品同名更新/缺则建；dish_ingredient 经 name→id 重映射。
 * 复用 `DishRepository.saveDish`/`IngredientRepository.createUserIngredient` 等成熟写入路径。
 * <p>
 * [AI生成] 方案 `双设备选择性同步方案.md` P1。
 **/
class SyncRepository(
    private val db: CookbookDatabase,
    private val dishRepo: DishRepository,
    private val ingredientRepo: IngredientRepository,
    private val pantryRepo: PantryRepository,
    private val healthRepo: HealthProfileRepository,
    private val favoriteRepo: FavoriteComboRepository,
) {
    private val q = db.cookbookQueries
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun toJson(bundle: SyncBundle): String = json.encodeToString(SyncBundle.serializer(), bundle)
    fun fromJson(text: String): SyncBundle = json.decodeFromString(SyncBundle.serializer(), text)

    /** 导出选中数据域为 Bundle（自动带依赖）。[AI修改] */
    suspend fun export(selection: SyncSelection): SyncBundle = withContext(ioDispatcher) {
        // 菜品被 收藏/餐食 依赖：任一选中就导出全部自建菜品(保证引用完整, 多余的合并无害)。
        val exportDishes = selection.dishes || selection.favorites || selection.meals
        val dishes = if (exportDishes) {
            q.selectSyncUserDishIds().executeAsList().mapNotNull { dishRepo.getDishById(it) }
        } else emptyList()
        val syncDishes = dishes.map { d ->
            SyncDish(
                name = d.name,
                specialNote = d.specialNote,
                description = d.description,
                imagePath = d.imagePath,
                thumbnailPath = d.thumbnailPath,
                cookingMethodNames = d.cookingMethods.map { it.name }.ifEmpty { listOfNotNull(d.cookingMethodName) },
                tagNames = d.tags,
                ingredients = d.ingredients.map { SyncDishIngredient(it.ingredient.name, it.isMain, it.quantity) },
                steps = d.steps.map { SyncStep(it.sortOrder, it.text, it.imagePath, it.thumbnailPath) },
            )
        }
        val pantryRows = if (selection.pantry) q.selectSyncPantry().executeAsList() else emptyList()
        // 食材名 = 自建食材(若选) ∪ 被菜品引用 ∪ 库存食材(若选)；保证自包含。
        val names = LinkedHashSet<String>()
        if (selection.ingredients) q.selectSyncUserIngredients().executeAsList().forEach { names.add(it.name) }
        dishes.forEach { d -> d.ingredients.forEach { names.add(it.ingredient.name) } }
        pantryRows.forEach { names.add(it.ingredient_name) }
        val syncIngredients = names.mapNotNull { name ->
            val row = q.selectSyncIngredientByName(name).executeAsOneOrNull() ?: return@mapNotNull null
            val det = q.selectSyncIngredientDetail(row.id).executeAsOneOrNull()
            SyncIngredient(
                name = row.name,
                alias = row.alias,
                emoji = row.emoji,
                imagePath = row.image_path,
                thumbnailPath = row.thumbnail_path,
                detail = det?.let {
                    SyncIngredientDetail(it.common_methods, it.prep_tips, it.eating_notes, it.storage_tips, it.health_note)
                },
            )
        }
        val syncPantry = pantryRows.map { SyncPantry(it.ingredient_name, it.serving_count.toInt()) }
        val health = if (selection.health) healthRepo.listAll().filter { it.enabled }.map { it.crowdName } else emptyList()
        val favorites = if (selection.favorites) {
            favoriteRepo.listCombos().map { c -> SyncFavorite(c.name, c.dishes.map { it.name }) }
        } else emptyList()
        val meals = if (selection.meals) {
            q.selectSyncMeals().executeAsList().map { m ->
                SyncMeal(
                    date = m.date, mealTypeCode = m.meal_type_code, mealTime = m.meal_time, note = m.note,
                    dishNames = q.selectSyncMealDishNames(m.meal_record_id).executeAsList(), // 单列直接是 List<String>
                )
            }
        } else emptyList()
        SyncBundle(
            schemaVersion = CookbookDatabase.Schema.version.toInt(),
            ingredients = syncIngredients,
            dishes = syncDishes,
            pantry = syncPantry,
            healthCrowds = health,
            favorites = favorites,
            meals = meals,
        )
    }

    /** 合并导入 Bundle。[AI生成] 同名复用/更新, ID 重映射。 */
    suspend fun import(bundle: SyncBundle): SyncImportResult = withContext(ioDispatcher) {
        require(bundle.schemaVersion <= CookbookDatabase.Schema.version.toInt()) {
            "数据来自更高版本(schema=${bundle.schemaVersion})，请升级应用后再导入"
        }
        var ingAdded = 0
        var dishAdded = 0
        var dishUpdated = 0
        var pantryMerged = 0
        var healthMerged = 0
        var favAdded = 0
        var mealsMerged = 0
        val nameToId = HashMap<String, Long>() // 食材名→id
        val dishNameToId = HashMap<String, Long>() // 菜品名→id

        // 食材：同名复用(不覆盖对方已有)，缺则新建 + 详情。
        for (si in bundle.ingredients) {
            val name = si.name.trim()
            if (name.isEmpty()) continue
            val existing = q.selectActiveIngredientIdByName(name).executeAsOneOrNull()
            val id = existing ?: run {
                val newId = ingredientRepo.createUserIngredient(
                    name = name, alias = si.alias.trim(), imagePath = si.imagePath, thumbnailPath = si.thumbnailPath,
                )
                si.detail?.let {
                    ingredientRepo.saveIngredientDetail(
                        IngredientDetail(newId, it.commonMethods, it.prepTips, it.eatingNotes, it.storageTips, it.healthNote),
                    )
                }
                ingAdded++
                newId
            }
            nameToId[name] = id
        }
        suspend fun resolveIngredientId(rawName: String): Long? {
            val n = rawName.trim(); if (n.isEmpty()) return null
            return nameToId[n]
                ?: q.selectActiveIngredientIdByName(n).executeAsOneOrNull()
                ?: ingredientRepo.createUserIngredient(name = n).also { nameToId[n] = it }
        }

        // 菜品：同名更新/缺则建；食材引用经 name→id 重映射(缺失食材兜底建)。
        for (sd in bundle.dishes) {
            val name = sd.name.trim()
            if (name.isEmpty()) continue
            val dishIngredients = sd.ingredients.mapNotNull { di ->
                val tid = resolveIngredientId(di.name) ?: return@mapNotNull null
                DishIngredient(ingredient = Ingredient(id = tid, name = di.name.trim()), quantity = di.quantity, isMain = di.isMain)
            }
            val existingDishId = q.selectUserDishIdByName(name).executeAsOneOrNull() ?: 0L
            val savedId = dishRepo.saveDish(
                id = existingDishId,
                name = name,
                cookingMethodId = null,
                cookingMethodNames = sd.cookingMethodNames,
                specialNote = sd.specialNote,
                description = sd.description,
                imagePath = sd.imagePath,
                thumbnailPath = sd.thumbnailPath,
                tagNames = sd.tagNames,
                ingredients = dishIngredients,
                steps = sd.steps.map { DishStep(sortOrder = it.sortOrder, text = it.text, imagePath = it.imagePath, thumbnailPath = it.thumbnailPath) },
            )
            dishNameToId[name] = savedId
            if (existingDishId > 0) dishUpdated++ else dishAdded++
        }
        fun resolveDishId(rawName: String): Long? { // 仅查询, 无需 suspend
            val n = rawName.trim(); if (n.isEmpty()) return null
            return dishNameToId[n] ?: q.selectDishIdByNameAny(n).executeAsOneOrNull()
        }

        // 库存：解析食材 → 覆盖对方份数(sync 语义=让对方与我一致)。
        val now = DateTime.nowEpochSeconds()
        for (sp in bundle.pantry) {
            val ingId = resolveIngredientId(sp.ingredientName) ?: continue
            db.transaction {
                q.insertPantryIfAbsent(ingredient_id = ingId, added_at = now)
                q.activatePantry(serving_count = sp.servingCount.coerceAtLeast(0).toLong(), added_at = now, ingredient_id = ingId)
            }
            pantryMerged++
        }

        // 健康档案：按人群名启用(seed 人群, 名字稳定)。
        if (bundle.healthCrowds.isNotEmpty()) {
            val crowds = healthRepo.listAllCrowdTypes()
            for (crowdName in bundle.healthCrowds) {
                crowds.firstOrNull { it.name == crowdName.trim() }?.let { healthRepo.add(it.id); healthMerged++ }
            }
        }

        // 收藏组合：同名跳过；解析菜品名→id, 至少1个则建。
        if (bundle.favorites.isNotEmpty()) {
            val existingComboNames = favoriteRepo.listCombos().map { it.name }.toSet()
            for (fav in bundle.favorites) {
                val fname = fav.name.trim()
                if (fname.isEmpty() || fname in existingComboNames) continue
                val dishIds = fav.dishNames.mapNotNull { resolveDishId(it) }.distinct()
                if (dishIds.isNotEmpty()) {
                    favoriteRepo.createCombo(fname, dishIds)
                    favAdded++
                }
            }
        }

        SyncImportResult(
            ingredientsAdded = ingAdded, dishesAdded = dishAdded, dishesUpdated = dishUpdated,
            pantryMerged = pantryMerged, healthMerged = healthMerged, favoritesAdded = favAdded, mealsMerged = mealsMerged,
        )
    }
}
