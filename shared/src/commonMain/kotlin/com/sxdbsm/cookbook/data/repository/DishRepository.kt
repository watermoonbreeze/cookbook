package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.Dish
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DishRepository(private val db: CookbookDatabase) {

    private val q = db.cookbookQueries

    fun observeAllDishes(): Flow<List<DishMini>> =
        q.selectAllDishes().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { row ->
                DishMini(
                    id = row.id,
                    name = row.name,
                    imagePath = row.image_path,
                    preference = row.preference,
                )
            }
        }

    fun observePopularDishes(limit: Long = 6): Flow<List<DishMini>> =
        q.selectDishesByPopularity(limit).asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { row ->
                DishMini(id = row.id, name = row.name, imagePath = row.image_path, preference = row.preference)
            }
        }

    fun observeRecentDishes(limit: Long = 6): Flow<List<DishMini>> =
        q.selectDishesByRecent(limit).asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { row ->
                DishMini(id = row.id, name = row.name, imagePath = row.image_path, preference = row.preference)
            }
        }

    suspend fun searchDishes(keyword: String): List<DishMini> {
        if (keyword.isBlank()) return q.selectAllDishes().executeAsList().map { row ->
            DishMini(id = row.id, name = row.name, imagePath = row.image_path, preference = row.preference)
        }
        val kw = "%${keyword.trim()}%"
        return q.searchDishes(kw).executeAsList().map { row ->
            DishMini(id = row.id, name = row.name, imagePath = row.image_path, preference = row.preference)
        }
    }

    fun observeDishById(id: Long): Flow<Dish?> =
        q.selectDishById(id).asFlow().mapToOneOrNull(Dispatchers.Default).map { row ->
            row?.let { loadFullDish(id, it) }
        }

    suspend fun getDishById(id: Long): Dish? =
        q.selectDishById(id).executeAsOneOrNull()?.let { row -> loadFullDish(id, row) }

    private fun loadFullDish(id: Long, row: com.sxdbsm.cookbook.db.Dish): Dish {
        val cookingMethod = row.cooking_method_id?.let { q.selectCookingMethodById(it).executeAsOneOrNull() }
        val tags = q.selectTagsByDish(id).executeAsList().map { it.name }
        val ingredients = q.selectIngredientsOfDish(id).executeAsList().map { ing ->
            DishIngredient(
                ingredient = Ingredient(
                    id = ing.ingredient_id,
                    name = ing.ingredient_name,
                    alias = ing.alias,
                    pinyin = ing.pinyin,
                    imagePath = ing.image_path,
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
            cookingMethodId = row.cooking_method_id,
            cookingMethodName = cookingMethod?.name,
            preference = row.preference,
            specialNote = row.special_note,
            description = row.description,
            imagePath = row.image_path,
            source = row.source,
            createdAt = row.created_at,
            updatedAt = row.updated_at,
            tags = tags,
            ingredients = ingredients,
        )
    }

    /**
     * 保存一个菜品（含标签与食材）。新建时 id<=0，编辑时 id>0。
     */
    suspend fun saveDish(
        id: Long,
        name: String,
        cookingMethodId: Long?,
        specialNote: String,
        description: String,
        imagePath: String,
        tagNames: List<String>,
        ingredients: List<DishIngredient>,
    ): Long {
        val now = DateTime.nowEpochSeconds()
        var dishId = id

        if (id <= 0) {
            q.insertDish(
                name = name,
                cooking_method_id = cookingMethodId,
                special_note = specialNote,
                description = description,
                image_path = imagePath,
                source = "user",
                created_at = now,
                updated_at = now,
            )
            dishId = q.lastInsertId().executeAsOne()
        } else {
            q.updateDish(
                name = name,
                cooking_method_id = cookingMethodId,
                special_note = specialNote,
                description = description,
                image_path = imagePath,
                updated_at = now,
                id = id,
            )
        }

        // 同步标签
        q.unlinkAllTagsOfDish(dishId)
        tagNames.distinct().forEach { tagName ->
            q.insertDishTag(name = tagName, source = "user", created_at = now)
            val tagId = q.selectDishTagByName(tagName).executeAsOneOrNull()?.id ?: return@forEach
            q.linkDishTag(dishId, tagId)
        }

        // 同步食材
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
        return dishId
    }

    suspend fun deleteDish(id: Long) {
        q.deleteDish(id)
    }
}
