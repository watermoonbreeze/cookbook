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

class IngredientRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries

    fun observeAll(): Flow<List<Ingredient>> =
        q.selectAllIngredients().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    suspend fun search(keyword: String): List<Ingredient> {
        if (keyword.isBlank()) return q.selectAllIngredients().executeAsList().map { it.toDomain() }
        return q.searchIngredients("%${keyword.trim()}%").executeAsList().map { it.toDomain() }
    }

    suspend fun listByCategory(categoryId: Long): List<Ingredient> =
        q.selectIngredientsByCategory(categoryId).executeAsList().map { it.toDomain() }

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

    suspend fun createUserIngredient(name: String, alias: String = ""): Long {
        val now = DateTime.nowEpochSeconds()
        q.insertIngredient(
            name = name,
            alias = alias,
            pinyin = Pinyin.toPinyin(name),
            image_path = "",
            default_unit_id = null,
            source = "user",
            created_at = now,
        )
        return q.lastInsertId().executeAsOne()
    }

    suspend fun listMeasurementUnits(): List<MeasurementUnit> =
        q.selectAllMeasurementUnits().executeAsList().map { MeasurementUnit(id = it.id, name = it.name) }

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

class FoodCategoryRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries

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
