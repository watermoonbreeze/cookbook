package com.sxdbsm.cookbook.sync

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.DishStep
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientDetail
import com.sxdbsm.cookbook.platform.ioDispatcher
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
) {
    private val q = db.cookbookQueries
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun toJson(bundle: SyncBundle): String = json.encodeToString(SyncBundle.serializer(), bundle)
    fun fromJson(text: String): SyncBundle = json.decodeFromString(SyncBundle.serializer(), text)

    /** 导出选中数据域为 Bundle。[AI生成] */
    suspend fun export(includeIngredients: Boolean, includeDishes: Boolean): SyncBundle = withContext(ioDispatcher) {
        val dishes = if (includeDishes) {
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
        // 导出的食材名 = 自建食材 ∪ 被菜品引用(保证 Bundle 自包含)
        val names = LinkedHashSet<String>()
        if (includeIngredients) q.selectSyncUserIngredients().executeAsList().forEach { names.add(it.name) }
        dishes.forEach { d -> d.ingredients.forEach { names.add(it.ingredient.name) } }
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
        SyncBundle(
            schemaVersion = CookbookDatabase.Schema.version.toInt(),
            ingredients = syncIngredients,
            dishes = syncDishes,
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
        val nameToId = HashMap<String, Long>()

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

        // 菜品：同名更新/缺则建；食材引用经 name→id 重映射(缺失食材兜底建)。
        for (sd in bundle.dishes) {
            val name = sd.name.trim()
            if (name.isEmpty()) continue
            val dishIngredients = sd.ingredients.mapNotNull { di ->
                val iname = di.name.trim()
                if (iname.isEmpty()) return@mapNotNull null
                val tid = nameToId[iname]
                    ?: q.selectActiveIngredientIdByName(iname).executeAsOneOrNull()
                    ?: ingredientRepo.createUserIngredient(name = iname).also { nameToId[iname] = it }
                DishIngredient(ingredient = Ingredient(id = tid, name = iname), quantity = di.quantity, isMain = di.isMain)
            }
            val existingDishId = q.selectUserDishIdByName(name).executeAsOneOrNull() ?: 0L
            dishRepo.saveDish(
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
            if (existingDishId > 0) dishUpdated++ else dishAdded++
        }
        SyncImportResult(ingredientsAdded = ingAdded, dishesAdded = dishAdded, dishesUpdated = dishUpdated)
    }
}
