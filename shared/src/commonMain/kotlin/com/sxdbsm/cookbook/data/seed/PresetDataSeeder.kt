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
     *
     * 性能优化（2026-07-03）：
     * - 旧库 NULL 清洗只在首次执行一次（preferences 标记守卫），不再每次启动全表 UPDATE。
     * - 字典类小表保持“表为空才写”。
     * - 食材/分类/详情/调养等大批量补齐式内容改为“内容指纹守卫”：seed JSON 未变化时整段跳过，
     *   避免每次启动重复覆写数百条记录；需要写入时整段包一个事务，把上千次独立 fsync 降为一次提交。
     */
    suspend fun seedIfNeeded() = withContext(Dispatchers.Default) {
        val q = db.cookbookQueries
        val now = DateTime.nowEpochSeconds()

        // [AI修改] 旧库 NULL 文本字段清洗只需一次；用 preferences 标记避免每次启动重复全表 UPDATE。
        if (q.selectPreference(PreferenceKeys.SEED_LEGACY_SANITIZED).executeAsOneOrNull()?.value_ != "1") {
            db.transaction {
                q.sanitizeLegacyNullTextFields()
                q.sanitizeLegacyDishNullTextFields()
            }
            q.upsertPreference(PreferenceKeys.SEED_LEGACY_SANITIZED, "1", now)
        }

        // 字典类小表：表为空才写入。
        if (q.countCookingMethods().executeAsOne() == 0L) seedCookingMethods(now)
        if (q.countMeasurementUnits().executeAsOne() == 0L) seedMeasurementUnits()
        seedMeasurementUnitGrams() // [AI生成] 每次启动幂等补齐单位克当量(v19 新列，老库回填)。
        if (q.countCrowdTypes().executeAsOne() == 0L) seedCrowdTypes(now)
        if (q.countMealTypes().executeAsOne() == 0L) seedMealTypes()
        ensureFlexibleSnackMealType() // [AI修改] 兼容旧库：补充“加餐”餐次，供添加餐食页按需手动选择时间。
        if (q.countDishTags().executeAsOne() == 0L) seedDishTags(now)
        seedStepTemplates(now) // [AI生成] #2 预设步骤模板：按名幂等补齐(缺则补，不覆盖用户改动)。
        seedIngredientGroups(now) // [AI生成] B5 预设配料组：按名幂等补齐。
        // [AI修改] 按具体 key 判断而非表是否为空：user_preferences 现在还会存 seed 元数据 key（指纹/清洗标记），
        // 不能再用 countUserPreferences==0 守卫，否则用户默认偏好会被漏写。
        if (q.selectPreference(PreferenceKeys.THEME_MODE).executeAsOneOrNull() == null) seedUserPreferences(now)

        // 大批量内容：内容指纹未变则整段跳过。
        reseedContentIfChanged(now, force = false)
    }

    /**
     * 手动重置/更新基础数据。[AI生成]
     *
     * 供“我的-更新基础数据”入口调用：强制忽略内容指纹，重新用内置（未来可替换为远程拉取的）JSON
     * 补齐式覆写预设内容，把预设食材/分类/详情/调养规则刷新到最新。
     * 语义为“刷新预设”而非“删除重建”：只做幂等 upsert，不删除任何行，因此用户自建数据、
     * 用户对预设食材的名称/图片修改、以及菜品对预设食材的引用关系都不受影响。
     *
     * @return 本次是否有内容写入（内容确有变化或强制刷新时为 true）。
     */
    suspend fun forceReseedBaseData(): Boolean = withContext(Dispatchers.Default) {
        val now = DateTime.nowEpochSeconds()
        reseedContentIfChanged(now, force = true)
    }

    /**
     * 内容指纹守卫下的补齐式写入。[AI生成]
     *
     * 计算 seed JSON 内容指纹并与上次写入的指纹比对；未变化且非强制时直接返回、零写入。
     * 需要写入时把分类/食材/详情/调养全套包进单个事务，保证要么整体成功要么回滚，且只提交一次。
     */
    private fun reseedContentIfChanged(now: Long, force: Boolean): Boolean {
        val q = db.cookbookQueries
        val categoriesJson = SeedResourceLoader.readText("seed/food_categories.json").orEmpty()
        val ingredientsJson = SeedResourceLoader.readText("seed/ingredients.json").orEmpty()
        val crowdRulesJson = SeedResourceLoader.readText("seed/crowd_rules.json").orEmpty()
        val detailsJson = SeedResourceLoader.readText("seed/ingredient_details.json").orEmpty()
        val careRulesJson = SeedResourceLoader.readText("seed/ingredient_care_rules.json").orEmpty()
        val dishesJson = SeedResourceLoader.readText("seed/dishes.json").orEmpty()
        val nutritionJson = SeedResourceLoader.readText("seed/ingredient_nutrition.json").orEmpty() // [AI生成] L2 营养数据文件
        val fingerprint = fingerprintOf(categoriesJson, ingredientsJson, crowdRulesJson, detailsJson, careRulesJson, dishesJson, nutritionJson)

        val stored = q.selectPreference(PreferenceKeys.SEED_CONTENT_FINGERPRINT).executeAsOneOrNull()?.value_
        if (!force && stored == fingerprint) {
            // [AI生成] 指纹一致跳过——排查"改了 seed 却没生效/新菜没出现"时看这条日志(需 force 或重装)。
            com.sxdbsm.cookbook.platform.CookbookLog.d("Seed", "内容指纹未变，跳过 seed (force=$force)")
            return false
        }
        com.sxdbsm.cookbook.platform.CookbookLog.d("Seed", "执行内容 seed (force=$force, 指纹变化=${stored != fingerprint})")

        db.transaction {
            val categoryIdsByCode = seedFoodCategories(now) // [AI修改] 分类补齐式 JSON seed。
            seedFoundationIngredients(now, categoryIdsByCode) // [AI修改] 食材补齐式 JSON seed，并补齐默认 emoji。
            seedCrowdRules() // [AI生成] 慢病食材建议规则补齐式 seed。
            seedIngredientDetails(now) // [AI生成] 食材详情补齐式 seed。
            seedIngredientCareRules(categoryIdsByCode) // [AI生成] 通用调养规则补齐式 seed。
            seedIngredientNutrition(now) // [AI生成] L2 食材营养素补齐式 seed（专用文件 ingredient_nutrition.json）。
            seedDishes(now) // [AI生成] 预设经典做法菜品补齐式 seed（关联主料/烹饪方式/配料/步骤）。
        }
        q.upsertPreference(PreferenceKeys.SEED_CONTENT_FINGERPRINT, fingerprint, now)
        return true
    }

    /**
     * 计算 seed 内容指纹。[AI生成]
     *
     * 用各文件“长度 + 内容 hashCode”组合，长度参与降低碰撞概率；Kotlin 的 String.hashCode 跨平台稳定，
     * 足以判断内容是否变化（最坏情况漏更新可由手动“更新基础数据”兜底）。
     */
    private fun fingerprintOf(vararg contents: String): String =
        contents.joinToString("|") { "${it.length}:${it.hashCode()}" }

    private fun seedCookingMethods(now: Long) {
        val q = db.cookbookQueries
        PRESET_COOKING_METHODS.forEach { name ->
            q.insertCookingMethod(name, "preset", now)
        }
    }

    private fun seedMeasurementUnits() {
        val q = db.cookbookQueries
        PRESET_MEASUREMENT_UNITS.forEach { (name, grams) ->
            q.insertMeasurementUnit(name, "preset", grams)
        }
    }

    /**
     * 补齐单位克当量（营养估算换算依据）。[AI生成]
     *
     * grams 列为 v19 新增，老库单位为 NULL；每次 seed 按名幂等回填，供“1两/1斤/1毫升→克”折算。
     * 计件单位(个/勺/片等)克当量为 null，留给食材 piece_gram。
     */
    private fun seedMeasurementUnitGrams() {
        val q = db.cookbookQueries
        PRESET_MEASUREMENT_UNITS.forEach { (name, grams) ->
            if (grams != null) q.updateMeasurementUnitGrams(grams, name)
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
     * 预设操作步骤模板。[AI生成] #2
     *
     * 按名幂等：已存在(用户可能改过同名)则跳过，不覆盖。仅内置几套常见做法，用户可在编辑菜品时自建更多。
     */
    private fun seedStepTemplates(now: Long) {
        val q = db.cookbookQueries
        PRESET_STEP_TEMPLATES.forEach { (name, steps) ->
            if (q.selectStepTemplateByName(name).executeAsOneOrNull() != null) return@forEach
            q.insertStepTemplate(name, "preset", now)
            val templateId = q.lastInsertId().executeAsOne()
            steps.forEachIndexed { index, text ->
                q.insertStepTemplateItem(templateId, index.toLong(), text)
            }
        }
    }

    /**
     * 预设常用配料组。[AI生成] B5
     *
     * 按名幂等：已存在则跳过，不覆盖用户改动。食材名须是预设/已存在食材(应用时按名解析，缺则兜底建)。
     */
    private fun seedIngredientGroups(now: Long) {
        val q = db.cookbookQueries
        PRESET_INGREDIENT_GROUPS.forEach { (name, items) ->
            if (q.selectIngredientGroupByName(name).executeAsOneOrNull() != null) return@forEach
            q.insertIngredientGroup(name, "preset", now)
            val groupId = q.lastInsertId().executeAsOne()
            items.forEachIndexed { index, (ingName, isMain) ->
                q.insertIngredientGroupItem(groupId, ingName, if (isMain) 1L else 0L, index.toLong(), null)
            }
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

        // [AI修改] 不再从 crowd_type 生成 crowd_${id} 调养分类：调养病种统一走 food_categories.json 的 care_ 节点，
        // 健康档案与调养 Tab / care rule 共用同一套 care_ 病种，避免两套病种重复。
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
            val ingredientId = if (existing == null) {
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
            } else {
                q.updateIngredientEmoji(seed.emoji.ifBlank { DEFAULT_INGREDIENT_EMOJI }, existing.id) // 补齐 JSON 维护的 emoji。
                existing.id
            }

            // [AI修改] 预设食材的有效/失效完全跟随后台数据包：JSON status=0 即下架（携带 reason），=1 即上架/恢复。
            q.updatePresetIngredientStatus(status = seed.status.toLong(), reason = seed.reason, id = ingredientId)

            seed.categories.forEach { categoryCode ->
                categoryIdsByCode[categoryCode]?.let { categoryId ->
                    q.linkIngredientCategory(ingredientId, categoryId) // [AI修改] 已存在食材也补齐新 JSON 分类。
                }
            }
        }
    }

    /**
     * 灌入食材营养素(L2 数值层)。[AI生成]
     *
     * 从专用文件 `ingredient_nutrition.json`(单表单文件，同 ingredient_details.json 模式)按食材名解析、补齐式 upsert。
     * 后续补营养数据只维护这一个文件。健康数据为参考整理值，带 ref/review。
     */
    private fun seedIngredientNutrition(now: Long) {
        val q = db.cookbookQueries
        loadIngredientNutrition().forEach { nu ->
            val ingredientId = q.selectIngredientIdByNameIncludingInactive(nu.ingredient).executeAsOneOrNull()?.id
                ?: return@forEach
            q.upsertIngredientNutrition(
                ingredient_id = ingredientId,
                energy_kcal = nu.kcal,
                protein_g = nu.protein,
                fat_g = nu.fat,
                carb_g = nu.carb,
                fiber_g = nu.fiber,
                sodium_mg = nu.sodium,
                potassium_mg = nu.potassium,
                calcium_mg = nu.calcium,
                gi = nu.gi,
                purine_mg = nu.purine,
                piece_gram = nu.pieceGram,
                ref = nu.ref,
                review = if (nu.review == "pending") 1L else 0L,
                updated_at = now,
            )
        }
    }

    private fun loadIngredientNutrition(): List<SeedIngredientNutrition> =
        SeedResourceLoader.readText("seed/ingredient_nutrition.json")
            ?.let { json.decodeFromString(it) }
            ?: emptyList()

    private fun loadFoodCategories(): List<SeedFoodCategory> =
        SeedResourceLoader.readText("seed/food_categories.json")
            ?.let { json.decodeFromString(it) }
            ?: emptyList()

    internal fun loadIngredientsForTest(): List<SeedIngredient> = loadIngredients() // [AI生成] 单元测试校验 JSON 维护质量。

    /**
     * 校验营养 seed 引用完整性：每条 ingredient 名都能对上一个食材。[AI生成]
     *
     * seeder 对解析不到的食材名静默跳过(名不匹配→营养白写)，故用单测守住。返回问题列表，空=全部可解析。
     */
    internal fun validateNutritionSeedForTest(): List<String> {
        val names = loadIngredients().map { it.name }.toSet()
        return loadIngredientNutrition().filter { it.ingredient !in names }.map { "营养数据未知食材:${it.ingredient}" }
    }

    /** 有营养数据的食材数(覆盖率参考)。[AI生成] */
    internal fun nutritionCountForTest(): Int = loadIngredientNutrition().size

    /**
     * 校验预设菜 JSON 引用完整性：食材名/烹饪方式/单位是否都能解析。[AI生成]
     *
     * seeder 对解析不到的食材/方式/单位会静默跳过（不崩但少关联），故用单测守住。
     * 返回问题列表，空表示全部可解析；对齐 DB 实际 seed 的方式与单位全集。
     */
    internal fun validateDishSeedForTest(): List<String> {
        val ingredientNames = loadIngredients().map { it.name }.toSet()
        val methods = PRESET_COOKING_METHODS.toSet() // [AI修改] 与 seedCookingMethods 同源，避免硬编码副本漂移。
        val units = unitNames() // [AI修改] 与 seedMeasurementUnits 同源(单位名全集)。
        val cuisines = com.sxdbsm.cookbook.domain.model.Cuisines.ALL.toSet()
        val problems = mutableListOf<String>()
        loadDishes().forEach { d ->
            if (d.method.isNotBlank() && d.method !in methods) problems += "[${d.name}] 未知烹饪方式:${d.method}"
            if (d.cuisine.isNotBlank() && d.cuisine !in cuisines) problems += "[${d.name}] 未知菜系:${d.cuisine}"
            if (d.ingredients.none { it.main }) problems += "[${d.name}] 缺主料"
            d.ingredients.forEach { di ->
                if (di.ingredient !in ingredientNames) problems += "[${d.name}] 未知食材:${di.ingredient}"
                if (di.unit.isNotBlank() && di.unit !in units) problems += "[${d.name}] 未知单位:${di.unit}"
            }
        }
        return problems
    }

    /**
     * 校验食材详情/分类 code/重复项 引用完整性。[AI生成]
     *
     * 补齐 validateDish/Nutrition 未覆盖的静默丢关联风险：详情按食材名匹配、食材挂类与调养规则按分类 code
     * 匹配，写错名/code 会静默丢关联而非报错，故用单测守住。返回问题列表，空=全部干净。
     */
    internal fun validateDetailAndCategorySeedForTest(): List<String> {
        val ingredients = loadIngredients()
        val names = ingredients.map { it.name }.toSet()
        val categoryCodes = loadFoodCategories().map { it.code }.toSet()
        val problems = mutableListOf<String>()

        // ① 详情引用的食材名必须存在
        loadIngredientDetails().filter { it.ingredient !in names }
            .forEach { problems += "详情未知食材:${it.ingredient}" }
        // ② 食材挂载的分类 code 必须存在
        ingredients.forEach { ing ->
            ing.categories.filter { it !in categoryCodes }
                .forEach { problems += "[${ing.name}] 未知分类code:$it" }
        }
        // ③ 调养规则引用的食材名 + 分类 code 必须存在
        loadIngredientCareRules().forEach { r ->
            if (r.ingredient !in names) problems += "调养规则未知食材:${r.ingredient}"
            if (r.category.isNotBlank() && r.category !in categoryCodes) problems += "[${r.ingredient}] 调养规则未知分类code:${r.category}"
        }
        // ④ 各 JSON 内部不得重复 name / code
        ingredients.groupingBy { it.name }.eachCount().filter { it.value > 1 }.keys
            .forEach { problems += "食材name重复:$it" }
        ingredients.groupingBy { it.code }.eachCount().filter { it.value > 1 }.keys
            .forEach { problems += "食材code重复:$it" }
        loadFoodCategories().groupingBy { it.code }.eachCount().filter { it.value > 1 }.keys
            .forEach { problems += "分类code重复:$it" }
        return problems
    }

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

    /**
     * 灌入食材详情。[AI生成]
     */
    private fun seedIngredientDetails(now: Long) {
        val q = db.cookbookQueries
        loadIngredientDetails().forEach { detail ->
            val ingredientId = q.selectIngredientIdByNameIncludingInactive(detail.ingredient).executeAsOneOrNull()?.id
                ?: return@forEach
            q.upsertIngredientDetail(
                ingredient_id = ingredientId,
                common_methods = detail.commonMethods,
                prep_tips = detail.prepTips,
                eating_notes = detail.eatingNotes,
                storage_tips = detail.storageTips,
                health_note = detail.healthNote,
                updated_at = now,
            )
        }
    }

    private fun loadIngredientDetails(): List<SeedIngredientDetail> =
        SeedResourceLoader.readText("seed/ingredient_details.json")
            ?.let { json.decodeFromString(it) }
            ?: emptyList()

    /**
     * 灌入新版调养规则。[AI生成]
     */
    private fun seedIngredientCareRules(categoryIdsByCode: Map<String, Long>) {
        val q = db.cookbookQueries
        loadIngredientCareRules().forEach { rule ->
            val ingredientId = q.selectIngredientIdByNameIncludingInactive(rule.ingredient).executeAsOneOrNull()?.id
                ?: return@forEach
            val categoryId = categoryIdsByCode[rule.category] ?: return@forEach
            q.upsertIngredientCareRule(
                ingredient_id = ingredientId,
                category_id = categoryId,
                advice_level = rule.level,
                reason = rule.reason,
                source = "preset",
            )
        }
    }

    private fun loadIngredientCareRules(): List<SeedIngredientCareRule> =
        SeedResourceLoader.readText("seed/ingredient_care_rules.json")
            ?.let { json.decodeFromString(it) }
            ?: emptyList()

    private companion object {
        const val DEFAULT_INGREDIENT_EMOJI = "🥗" // [AI生成] 所有兜底食材统一使用更清爽的沙拉图标。

        // [AI生成] 预设烹饪方式/计量单位全集：seed 写入与 dishes.json 引用完整性校验共用同一份，避免硬编码副本漂移。
        val PRESET_COOKING_METHODS = listOf("炒", "蒸", "煮", "炖", "烤", "凉拌", "煎", "炸", "焖", "卤")

        // [AI生成] 计量单位 → 克当量(营养换算)：重量/体积单位给明确克当量；
        // 计件/模糊单位(个/片/勺/颗…/适量/少许)克当量留 null，改由食材 piece_gram 折算。
        // 勺按“汤勺≈15g”粗估(多用于油盐酱)。dishes.json 引用完整性校验用 name 集合(见 unitNames())。
        val PRESET_MEASUREMENT_UNITS: List<Pair<String, Double?>> = listOf(
            "克" to 1.0, "两" to 50.0, "斤" to 500.0, "毫升" to 1.0, "升" to 1000.0,
            "个" to null, "片" to null, "勺" to 15.0, "颗" to null, "把" to null,
            "碗" to null, "块" to null, "根" to null, "条" to null, "段" to null,
            "瓣" to null, "只" to null, "适量" to null, "少许" to null,
        )

        /** 单位名全集(供 dishes.json 引用完整性校验)。[AI生成] */
        fun unitNames(): Set<String> = PRESET_MEASUREMENT_UNITS.map { it.first }.toSet()

        // [AI生成] #2 预设操作步骤模板：常见做法的通用步骤(文字)，编辑菜品"选择步骤"一键套用。
        val PRESET_STEP_TEMPLATES: List<Pair<String, List<String>>> = listOf(
            "家常炒菜" to listOf("备菜：食材洗净切配", "热锅冷油，下葱姜蒜爆香", "下主料大火快炒", "加辅料，调味翻炒", "炒匀出锅装盘"),
            "清蒸" to listOf("食材处理干净、摆盘", "简单调味腌制片刻", "水开后上锅蒸熟", "取出淋味汁、浇热油激香"),
            "红烧" to listOf("主料焯水或煎至上色", "起锅炒香配料（可炒糖色）", "下主料翻炒，加水与调料", "小火焖煮至软烂", "大火收汁出锅"),
            "煲汤" to listOf("食材焯水去腥", "冷水下锅，放姜片", "大火烧开转小火慢炖", "出锅前加盐调味"),
            "凉拌" to listOf("食材焯水或洗净切好", "调味汁：蒜末、生抽、醋、香油等", "食材与料汁拌匀", "腌几分钟入味即可"),
        )

        // [AI生成] B5 预设配料组：(食材名, 是否主料)。名字用常见调料/香辛料(应用时按名解析，缺则兜底建为自建食材)。
        val PRESET_INGREDIENT_GROUPS: List<Pair<String, List<Pair<String, Boolean>>>> = listOf(
            "基础调料包" to listOf("盐" to false, "生抽" to false, "食用油" to false),
            "葱姜蒜" to listOf("葱" to false, "姜" to false, "蒜" to false),
            "红烧料" to listOf("生抽" to false, "老抽" to false, "冰糖" to false, "料酒" to false, "八角" to false),
            // [AI生成] Phase3：从用户私人菜单沉淀的高频味型组合。
            "葱姜蒜辣" to listOf("葱" to false, "姜" to false, "蒜" to false, "小米辣" to false),
            "樟树椒洋葱香菜" to listOf("樟树椒" to true, "洋葱" to true, "香菜" to false),
            "凉拌汁" to listOf("大蒜" to false, "生抽" to false, "香醋" to false, "芝麻油" to false),
            "酸菜鱼料" to listOf("酸菜" to true, "泡椒" to false, "泡姜" to false, "花椒" to false, "小米辣" to false),
            "水煮料" to listOf("花椒" to false, "干辣椒" to false, "豆瓣酱" to false),
            "煎肉香料" to listOf("洋葱" to true, "迷迭香" to false, "黑胡椒粉" to false),
        )
    }

    /**
     * 预设经典做法菜品补齐式 seed。[AI生成]
     *
     * 每道菜按名解析烹饪方式/主料/配料/单位，写入 dish + dish_cooking_method_rel + dish_ingredient + dish_step。
     * 幂等：同名预设菜已存在则跳过（不覆盖，避免影响用户已引用/收藏）；食材名解析不到则跳过该配料。
     */
    private fun seedDishes(now: Long) {
        val q = db.cookbookQueries
        val methodIds = q.selectAllCookingMethods().executeAsList().associate { it.name to it.id }
        val unitIds = q.selectAllMeasurementUnits().executeAsList().associate { it.name to it.id }
        loadDishes().forEach dish@{ seed ->
            // [AI生成] 先给已存在的预设菜补菜系(幂等，仅当前为空才补)，再判断是否跳过插入——老库升级也能拿到菜系。
            if (seed.cuisine.isNotBlank()) q.updatePresetDishCuisineByName(cuisine = seed.cuisine, name = seed.name)
            if (q.selectPresetDishIdByName(seed.name).executeAsOneOrNull() != null) return@dish // 已存在则跳过。
            val methodId = methodIds[seed.method]
            q.insertDish(
                name = seed.name,
                cooking_method_id = methodId,
                special_note = "",
                description = seed.description,
                image_path = "",
                thumbnail_path = "",
                source = "preset",
                created_at = now,
                updated_at = now,
                cuisine = seed.cuisine,
            )
            val dishId = q.lastInsertId().executeAsOne()
            if (methodId != null) q.linkDishCookingMethod(dishId, methodId)
            seed.ingredients.forEach ing@{ di ->
                val ingredientId = q.selectIngredientIdByNameIncludingInactive(di.ingredient).executeAsOneOrNull()?.id ?: return@ing
                // [AI修改] 改具名参数：防生成查询日后插字段致位置错位(红线)。
                q.insertDishIngredient(
                    dish_id = dishId,
                    ingredient_id = ingredientId,
                    quantity = di.quantity,
                    unit_id = unitIds[di.unit],
                    is_main = if (di.main) 1L else 0L,
                )
            }
            seed.steps.forEachIndexed { idx, text ->
                q.insertDishStep(
                    dish_id = dishId,
                    sort_order = (idx + 1).toLong(),
                    text = text,
                    image_path = "",
                    thumbnail_path = "",
                    created_at = now,
                    updated_at = now,
                )
            }
        }
    }

    private fun loadDishes(): List<SeedDish> =
        SeedResourceLoader.readText("seed/dishes.json")
            ?.let { json.decodeFromString(it) }
            ?: emptyList()
}

@Serializable
private data class SeedDish(
    val code: String = "",
    val name: String,
    val method: String = "",
    val description: String = "",
    val cuisine: String = "", // [AI生成] 菜系(家常菜/川菜等)
    val ingredients: List<SeedDishIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
)

@Serializable
private data class SeedDishIngredient(
    val ingredient: String,
    val main: Boolean = false,
    val quantity: Double? = null,
    val unit: String = "",
)

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
    val status: Int = 1, // [AI生成] 1 有效 / 0 失效（后台下架）；默认有效，兼容旧 JSON 无此字段。
    val reason: String = "", // [AI生成] 失效原因，仅 status=0 时有意义。
)

/**
 * 食材营养素(每 100g 可食部)——对应专用文件 ingredient_nutrition.json。[AI生成]
 *
 * 单表单文件(同 ingredient_details.json)：后续补数据只维护该文件、按食材名匹配。全部可空=未知。
 * kcal/protein/fat/carb/fiber(g)、sodium/potassium/calcium/purine(mg)、gi(0~100)、pieceGram(计件默认克重)。
 * 健康数据为 AI 参考整理、非权威核对：来源填 ref、待审填 review。
 */
@Serializable
internal data class SeedIngredientNutrition(
    val ingredient: String,
    val kcal: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carb: Double? = null,
    val fiber: Double? = null,
    val sodium: Double? = null,
    val potassium: Double? = null,
    val calcium: Double? = null,
    val gi: Double? = null,
    val purine: Double? = null,
    val pieceGram: Double? = null,
    val ref: String = "",
    val review: String = "", // "pending"=待审
)

@Serializable
private data class SeedCrowdRule(
    val crowd: String,
    val ingredient: String,
    val level: String,
    val reason: String = "",
    val dailyLimit: Double? = null,
)

@Serializable
private data class SeedIngredientDetail(
    val ingredient: String,
    val commonMethods: String = "",
    val prepTips: String = "",
    val eatingNotes: String = "",
    val storageTips: String = "",
    val healthNote: String = "",
)

@Serializable
private data class SeedIngredientCareRule(
    val ingredient: String,
    val category: String,
    val level: String,
    val reason: String = "",
)
