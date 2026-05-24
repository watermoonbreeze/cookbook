package com.sxdbsm.cookbook.data.seed

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.PreferenceKeys
import com.sxdbsm.cookbook.platform.Pinyin
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 首次启动灌入字典与基础数据。[AI修改]
 *
 * 每张表先 count，为 0 才灌入，避免重启重复。
 * MVP 仅灌入字典 + 一级分类 + 少量演示食材 + 人群分类。具体二级分类/食材后续扩充。
 */
class PresetDataSeeder(private val db: CookbookDatabase) {

    /**
     * 检查并灌入缺失的预置数据。[AI修改]
     */
    suspend fun seedIfNeeded() = withContext(Dispatchers.Default) {
        val q = db.cookbookQueries
        val now = DateTime.nowEpochSeconds()

        if (q.countCookingMethods().executeAsOne() == 0L) seedCookingMethods(now)
        if (q.countMeasurementUnits().executeAsOne() == 0L) seedMeasurementUnits()
        if (q.countCrowdTypes().executeAsOne() == 0L) seedCrowdTypes(now)
        if (q.countMealTypes().executeAsOne() == 0L) seedMealTypes()
        ensureFlexibleSnackMealType() // [AI修改] 兼容旧库：补充“加餐”餐次，供添加餐食页按需手动选择时间。
        if (q.countDishTags().executeAsOne() == 0L) seedDishTags(now)
        if (q.countFoodCategories().executeAsOne() == 0L) seedFoodCategories(now)
        if (q.countUserPreferences().executeAsOne() == 0L) seedUserPreferences(now)
        seedDemoIngredientsIfEmpty(now)
    }

    private fun seedCookingMethods(now: Long) {
        val q = db.cookbookQueries
        listOf("炒", "蒸", "煮", "炖", "烤", "凉拌", "煎", "炸", "焖", "卤").forEach { name ->
            q.insertCookingMethod(name, "preset", now)
        }
    }

    private fun seedMeasurementUnits() {
        val q = db.cookbookQueries
        listOf("克", "两", "斤", "毫升", "升", "个", "片", "勺", "颗", "把", "适量", "少许").forEach { name ->
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
     *
     * `meal_type.default_time` 数据库字段非空，所以用 23:59 作为排序占位；
     * UI 层会识别 `isFixed=false` 并要求用户手动选择具体用餐时间。
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
     * 一级分类 + 健康饮食/人群分类下的二级分类。[AI修改]
     * 一级 id 由 AUTOINCREMENT 决定，假定按插入顺序为 1-8。
     */
    private fun seedFoodCategories(now: Long) {
        val q = db.cookbookQueries
        // 一级 (1-8)
        val topLevel = listOf(
            // name, dimension, sort, icon
            arrayOf("主食", "general", 10, ""),
            arrayOf("蔬菜", "general", 20, ""),
            arrayOf("肉类", "general", 30, ""),
            arrayOf("水产", "general", 40, ""),
            arrayOf("蛋奶豆", "general", 50, ""),
            arrayOf("调味料", "general", 60, ""),
            arrayOf("健康饮食", "nutrition", 70, ""),
            arrayOf("人群分类", "crowd", 80, ""),
        )
        topLevel.forEach { row ->
            q.insertFoodCategory(
                name = row[0] as String,
                dimension = row[1] as String,
                parent_id = null,
                crowd_type_id = null,
                sort_order = (row[2] as Int).toLong(),
                icon = row[3] as String,
                source = "preset",
                created_at = now,
            )
        }
        val nutritionParentId = q.selectTopLevelCategories().executeAsList().first { it.name == "健康饮食" }.id
        val crowdParentId = q.selectTopLevelCategories().executeAsList().first { it.name == "人群分类" }.id

        // 健康饮食 二级
        listOf(
            "低嘌呤" to "purine", "低 Gi" to "gi", "低糖" to "sugar", "低脂" to "fat", "低钠" to "sodium",
        ).forEachIndexed { idx, (name, dim) ->
            q.insertFoodCategory(
                name = name,
                dimension = dim,
                parent_id = nutritionParentId,
                crowd_type_id = null,
                sort_order = ((idx + 1) * 10).toLong(),
                icon = "",
                source = "preset",
                created_at = now,
            )
        }

        // 人群分类 二级（关联到 crowd_type）
        q.selectAllCrowdTypes().executeAsList().forEachIndexed { idx, crowd ->
            q.insertFoodCategory(
                name = crowd.name,
                dimension = "crowd",
                parent_id = crowdParentId,
                crowd_type_id = crowd.id,
                sort_order = ((idx + 1) * 10).toLong(),
                icon = "",
                source = "preset",
                created_at = now,
            )
        }
    }

    private fun seedUserPreferences(now: Long) {
        val q = db.cookbookQueries
        q.upsertPreference(PreferenceKeys.THEME_MODE, "system", now)
        q.upsertPreference(PreferenceKeys.HOME_RECENT_COUNT, "6", now)
        q.upsertPreference(PreferenceKeys.HOME_POPULAR_COUNT, "6", now)
    }

    /**
     * 灌入少量演示食材，方便首次启动后用户能直接选用。[AI修改]
     * 若 ingredient 表已有数据则跳过。
     */
    private fun seedDemoIngredientsIfEmpty(now: Long) {
        val q = db.cookbookQueries
        val existing = q.selectAllIngredients().executeAsList()
        if (existing.isNotEmpty()) return

        val units = q.selectAllMeasurementUnits().executeAsList().associateBy { it.name }
        val categories = (q.selectTopLevelCategories().executeAsList() +
            q.selectTopLevelCategories().executeAsList()
                .flatMap { q.selectChildCategories(it.id).executeAsList() })
            .associateBy { it.name }

        data class DemoIngredient(
            val name: String, val alias: String, val unit: String, val categories: List<String>,
        )

        val demos = listOf(
            DemoIngredient("西红柿", "番茄", "个", listOf("蔬菜")),
            DemoIngredient("鸡蛋", "", "个", listOf("蛋奶豆")),
            DemoIngredient("米饭", "白米饭", "碗", listOf("主食")),
            DemoIngredient("青菜", "小白菜", "把", listOf("蔬菜")),
            DemoIngredient("猪肉", "里脊肉", "克", listOf("肉类")),
            DemoIngredient("鸡肉", "鸡胸肉", "克", listOf("肉类")),
            DemoIngredient("豆腐", "嫩豆腐", "块", listOf("蛋奶豆")),
            DemoIngredient("胡萝卜", "红萝卜", "根", listOf("蔬菜")),
            DemoIngredient("土豆", "马铃薯", "个", listOf("主食")),
            DemoIngredient("面条", "挂面", "把", listOf("主食")),
            DemoIngredient("鲈鱼", "", "条", listOf("水产")),
            DemoIngredient("虾", "基围虾", "克", listOf("水产")),
            DemoIngredient("牛奶", "", "毫升", listOf("蛋奶豆")),
            DemoIngredient("盐", "食盐", "克", listOf("调味料")),
            DemoIngredient("生抽", "酱油", "勺", listOf("调味料")),
            DemoIngredient("食用油", "菜籽油", "勺", listOf("调味料")),
            DemoIngredient("姜", "生姜", "片", listOf("调味料")),
            DemoIngredient("葱", "大葱", "段", listOf("调味料")),
            DemoIngredient("大蒜", "蒜", "瓣", listOf("调味料")),
            DemoIngredient("黄瓜", "青瓜", "根", listOf("蔬菜")),
        )

        demos.forEach { d ->
            val unitId = units[d.unit]?.id
            // unit 可能预置中没有，跳过单位即可
            q.insertIngredient(
                name = d.name,
                alias = d.alias,
                pinyin = Pinyin.toPinyin(d.name),
                image_path = "",
                default_unit_id = unitId,
                source = "preset",
                created_at = now,
            )
            val ingredientId = q.lastInsertId().executeAsOne()
            d.categories.forEach { catName ->
                categories[catName]?.let { cat -> q.linkIngredientCategory(ingredientId, cat.id) }
            }
        }
    }
}
