package com.sxdbsm.cookbook.data.seed

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.PreferenceKeys
import com.sxdbsm.cookbook.platform.Pinyin
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * 首次启动灌入字典与基础数据。[AI修改]
 *
 * 字典类小表仍保留代码内 seed；基础食材和多维分类改为读取 `shared/src/commonMain/resources/seed/` 下的 JSON 文件。
 * 这样后续扩展基础数据时，主要维护 JSON 文件即可，不需要频繁修改 Kotlin 逻辑。
 */
class PresetDataSeeder(private val db: CookbookDatabase) {
    private val json = Json { ignoreUnknownKeys = true } // [AI生成] JSON 后续新增字段时旧代码可继续解析。

    /**
     * 检查并灌入缺失的预置数据。[AI修改]
     */
    suspend fun seedIfNeeded() = withContext(Dispatchers.Default) {
        val q = db.cookbookQueries
        val now = DateTime.nowEpochSeconds()
        q.sanitizeLegacyNullTextFields() // [AI生成] 兼容旧库 NULL 文本字段，避免 SQLDelight 非空映射 NPE。
        q.sanitizeLegacyDishNullTextFields() // [AI生成] 菜品编辑加载前先修正旧数据空字段，避免图片/表单字段丢失。

        if (q.countCookingMethods().executeAsOne() == 0L) seedCookingMethods(now)
        if (q.countMeasurementUnits().executeAsOne() == 0L) seedMeasurementUnits()
        if (q.countCrowdTypes().executeAsOne() == 0L) seedCrowdTypes(now)
        if (q.countMealTypes().executeAsOne() == 0L) seedMealTypes()
        ensureFlexibleSnackMealType() // [AI修改] 兼容旧库：补充“加餐”餐次，供添加餐食页按需手动选择时间。
        if (q.countDishTags().executeAsOne() == 0L) seedDishTags(now)
        val categoryIdsByCode = seedFoodCategories(now) // [AI修改] 分类采用补齐式 JSON seed，旧库已有数据时仍能补新增分类。
        if (q.countUserPreferences().executeAsOne() == 0L) seedUserPreferences(now)
        seedFoundationIngredients(now, categoryIdsByCode) // [AI修改] 食材采用补齐式 JSON seed，并补齐默认 emoji。
        seedCrowdRules() // [AI生成] 慢病食材建议规则采用 JSON 补齐式 seed。
    }

    private fun seedCookingMethods(now: Long) {
        val q = db.cookbookQueries
        listOf("炒", "蒸", "煮", "炖", "烤", "凉拌", "煎", "炸", "焖", "卤").forEach { name ->
            q.insertCookingMethod(name, "preset", now)
        }
    }

    private fun seedMeasurementUnits() {
        val q = db.cookbookQueries
        listOf("克", "两", "斤", "毫升", "升", "个", "片", "勺", "颗", "把", "碗", "块", "根", "条", "段", "瓣", "只", "适量", "少许").forEach { name ->
            q.insertMeasurementUnit(name, "preset")
        }
    }

    private fun seedCrowdTypes(now: Long) {
        val q = db.cookbookQueries
        listOf(
            "高血压" to "需控钠、控脂",
            "糖尿病" to "需控糖、控碳水",
            "高血脂" to "需控胆固醇、饱和脂肪",
            "高尿酸" to "需控嘌呤",
        ).forEach { (name, desc) ->
            q.insertCrowdType(name, desc, "preset", now)
        }
    }

    private fun seedMealTypes() {
        val q = db.cookbookQueries
        listOf(
            Triple("BREAKFAST", "早餐", "07:30"),
            Triple("MORNING_SNACK", "上午餐", "10:00"),
            Triple("LUNCH", "中餐", "12:00"),
            Triple("AFTERNOON_SNACK", "下午餐", "15:00"),
            Triple("DINNER", "晚餐", "18:30"),
            Triple("NIGHT_SNACK", "宵夜", "21:30"),
        ).forEach { (code, name, time) ->
            q.insertMealType(code, name, time, 1, "preset")
        }
    }

    /**
     * 确保存在用户可手动指定时间的“加餐”。[AI修改]
     */
    private fun ensureFlexibleSnackMealType() {
        db.cookbookQueries.insertMealType("SNACK", "加餐", "23:59", 0, "preset")
    }

    private fun seedDishTags(now: Long) {
        val q = db.cookbookQueries
        listOf("#复制" to "preset", "家常" to "preset", "快手" to "preset", "少盐" to "preset").forEach { (name, src) ->
            q.insertDishTag(name, src, now)
        }
    }

    /**
     * 从 JSON 补齐食材分类，并返回 `code -> database id` 映射。[AI修改]
     */
    private fun seedFoodCategories(now: Long): Map<String, Long> {
        val q = db.cookbookQueries
        val categoryIdsByCode = mutableMapOf<String, Long>()
        val categories = loadFoodCategories()

        fun ensureCategory(seed: SeedFoodCategory, parentId: Long?, crowdTypeId: Long? = null): Long {
            val existing = if (parentId == null) {
                q.selectTopLevelCategories().executeAsList().firstOrNull { it.name == seed.name && it.dimension == seed.dimension }
            } else {
                q.selectChildCategories(parentId).executeAsList().firstOrNull { it.name == seed.name && it.dimension == seed.dimension }
            }
            if (existing != null) return existing.id
            q.insertFoodCategory(
                name = seed.name,
                dimension = seed.dimension,
                parent_id = parentId,
                crowd_type_id = crowdTypeId,
                sort_order = seed.sort,
                icon = seed.icon,
                source = "preset",
                created_at = now,
            )
            return q.lastInsertId().executeAsOne()
        }

        categories.forEach { seed ->
            val parentId = seed.parent?.let { categoryIdsByCode[it] }
            categoryIdsByCode[seed.code] = ensureCategory(seed, parentId)
        }

        val crowdParentId = categoryIdsByCode["crowd"]
        if (crowdParentId != null) {
            q.selectAllCrowdTypes().executeAsList().forEachIndexed { idx, crowd ->
                val seed = SeedFoodCategory(
                    code = "crowd_${crowd.id}",
                    name = crowd.name,
                    dimension = "crowd",
                    parent = "crowd",
                    sort = ((idx + 1) * 10).toLong(),
                )
                ensureCategory(seed, crowdParentId, crowd.id)
            }
        }
        return categoryIdsByCode
    }

    private fun seedUserPreferences(now: Long) {
        val q = db.cookbookQueries
        q.upsertPreference(PreferenceKeys.THEME_MODE, "system", now)
        q.upsertPreference(PreferenceKeys.HOME_RECENT_COUNT, "6", now)
        q.upsertPreference(PreferenceKeys.HOME_POPULAR_COUNT, "6", now)
    }

    /**
     * 从 JSON 灌入第一阶段基础食材。[AI修改]
     *
     * 规则：同名食材已存在时不重复插入，但仍补齐默认 emoji 和分类关系；软删除的预设食材会被恢复。
     */
    private fun seedFoundationIngredients(now: Long, categoryIdsByCode: Map<String, Long>) {
        val q = db.cookbookQueries
        val units = q.selectAllMeasurementUnits().executeAsList().associateBy { it.name }

        loadIngredients().forEach { seed ->
            val unitId = units[seed.unit]?.id
            val pinyin = Pinyin.toPinyin(seed.name)
            val existing = q.selectIngredientIdByNameIncludingInactive(seed.name).executeAsOneOrNull()
            val ingredientId = when {
                existing == null -> {
                    q.insertIngredient(
                        name = seed.name,
                        alias = seed.alias,
                        pinyin = pinyin,
                        image_path = "",
                        thumbnail_path = "",
                        emoji = seed.emoji.ifBlank { DEFAULT_INGREDIENT_EMOJI },
                        default_unit_id = unitId,
                        source = "preset",
                        created_at = now,
                    )
                    q.lastInsertId().executeAsOne()
                }
                existing.status == 0L -> {
                    q.restorePresetIngredient(
                        alias = seed.alias,
                        pinyin = pinyin,
                        emoji = seed.emoji.ifBlank { DEFAULT_INGREDIENT_EMOJI },
                        default_unit_id = unitId,
                        id = existing.id,
                    )
                    existing.id
                }
                else -> {
                    q.updateIngredientEmoji(seed.emoji.ifBlank { DEFAULT_INGREDIENT_EMOJI }, existing.id)
                    existing.id
                }
            }

            seed.categories.forEach { categoryCode ->
                categoryIdsByCode[categoryCode]?.let { categoryId ->
                    q.linkIngredientCategory(ingredientId, categoryId) // [AI修改] 已存在食材也补齐新 JSON 分类。
                }
            }
        }
    }

    private fun loadFoodCategories(): List<SeedFoodCategory> =
        SeedResourceLoader.readText("seed/food_categories.json")
            ?.let { json.decodeFromString(it) }
            ?: emptyList()

    internal fun loadIngredientsForTest(): List<SeedIngredient> = loadIngredients() // [AI生成] 单元测试校验 JSON 维护质量。

    private fun loadIngredients(): List<SeedIngredient> =
        SeedResourceLoader.readText("seed/ingredients.json")
            ?.let { json.decodeFromString(it) }
            ?: emptyList()

    private fun seedCrowdRules() {
        val q = db.cookbookQueries
        val crowds = q.selectAllCrowdTypes().executeAsList().associateBy { it.name }
        loadCrowdRules().forEach { rule ->
            val crowd = crowds[rule.crowd] ?: return@forEach
            val ingredientId = q.selectIngredientIdByNameIncludingInactive(rule.ingredient).executeAsOneOrNull()?.id ?: return@forEach
            q.upsertCrowdIngredient(
                crowd_id = crowd.id,
                ingredient_id = ingredientId,
                advice_level = rule.level,
                reason = rule.reason,
                daily_limit = rule.dailyLimit,
                source = "preset",
            )
        }
    }

    private fun loadCrowdRules(): List<SeedCrowdRule> =
        SeedResourceLoader.readText("seed/crowd_rules.json")
            ?.let { json.decodeFromString(it) }
            ?: emptyList()

    private companion object {
        const val DEFAULT_INGREDIENT_EMOJI = "🥗" // [AI生成] 所有兜底食材统一使用更清爽的沙拉图标。
    }
}

@Serializable
private data class SeedFoodCategory(
    val code: String,
    val name: String,
    val dimension: String = "general",
    val parent: String? = null,
    val sort: Long = 0,
    val icon: String = "",
)

@Serializable
internal data class SeedIngredient(
    val code: String,
    val name: String,
    val alias: String = "",
    val unit: String = "",
    val emoji: String = "🥗",
    val categories: List<String> = emptyList(),
)

@Serializable
private data class SeedCrowdRule(
    val crowd: String,
    val ingredient: String,
    val level: String,
    val reason: String = "",
    val dailyLimit: Double? = null,
)
