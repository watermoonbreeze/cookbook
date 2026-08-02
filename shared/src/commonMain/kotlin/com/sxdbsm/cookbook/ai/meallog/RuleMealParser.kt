package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.domain.IngredientNameExtractor
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.LocalDate

/**
 * @File : RuleMealParser
 * @Time : 2026/07/29
 * @Author : SXD-AI
 * @Desc : 规则解析引擎——用正则+关键词将用户自然语言文本解析为 DayMealJson 列表
 * <p>
 * 顶层流程：TextNormalizer → TextSegmenter → RuleMealParser.parse() → List<DayMealJson>
 * 每条 DayMealJson 内：按餐次关键词分段→每段拆菜品→每道菜拆份量/食材/备注
 * 纯函数，离线可用，不依赖 AI。
 * <p>
 * [AI生成] K2 AI快捷输入记餐专项重构：规则解析引擎。
 **/
object RuleMealParser {

    // ═══════════════════════════════════════════════════
    // 餐次关键词（长词→短词，先匹配长的避免误截）
    // ═══════════════════════════════════════════════════

    private data class MealSplitter(
        val mealType: String,
        val keywords: List<String>,  // 按长度降序
    )

    private val MEAL_SPLITTERS = listOf(
        MealSplitter("breakfast", listOf(
            "早上饭", "早晨饭", "早起饭", "早饭", "早餐", "早点", "早晨", "早上", "晨餐",
            "晨间", "过早", "早茶", "早膳", "朝食", "食朝", "吃朝", "切早饭",
        ).sortedByDescending { it.length }),
        MealSplitter("lunch", listOf(
            "中午饭", "晌午饭", "早午饭", "早中饭", "午饭", "午餐", "中饭", "中餐", "中午",
            "午间", "晌午", "午膳", "午食", "食晏", "食晏昼", "食昼", "食日昼",
            "吃晏", "切中饭", "响午饭", "brunch",
        ).sortedByDescending { it.length }),
        MealSplitter("dinner", listOf(
            "晚上饭", "晚间饭", "晚饭", "晚餐", "晚点", "晚上", "晚间", "夜间", "傍晚",
            "夜饭", "夜餐", "晚膳", "晚食", "晚夕饭", "食夜", "食暗", "吃晚",
            "切夜饭",
        ).sortedByDescending { it.length }),
        MealSplitter("snack", listOf(
            "下午茶", "垫吧垫吧", "加餐", "宵夜", "夜宵", "零食", "点心", "小吃", "小食",
            "零嘴", "零嘴儿", "间食", "茶点", "茶歇", "早茶", "午后点心",
            "垫垫", "垫补", "填补", "零碎", "零碎儿", "吃夜", "煮夜", "深夜",
        ).sortedByDescending { it.length }),
    )

    // 菜品分隔词
    private val HARD_SPLIT = Regex("""[,，、+\n]""")
    private val SOFT_SPLIT = Regex("""(和|跟|还有|以及|加上|配上?|搭配|搭|就着|再来|外加|捎带|另|另外|还有一个|再加上)""")

    // 份量+单位
    private val QUANTITY_UNIT = Regex("""^([一二两三四五六七八九十百半\\d]+\\.?\\d*)\\s*([个大碗盘碟只根片块勺份杯瓶盒袋笼屉][子]?)""")

    // 食用比例
    private val EATEN_PATTERNS = listOf(
        Regex("""(吃了?)?一半|剩[了]?一半|半份""") to 0.5,
        Regex("""(吃了?)?大半|差不多""") to 0.75,
        Regex("""少量|一点点|[一就]点[点]?|尝了?[一]?口|几口""") to 0.25,
    )

    // 括号备注
    private val PAREN_NOTE = Regex("""[（(]([^）)]+)[）)]""")

    // 中文数字
    private val CHINESE_NUM_MAP = mapOf(
        "半" to 0.5, "一" to 1.0, "二" to 2.0, "两" to 2.0, "三" to 3.0,
        "四" to 4.0, "五" to 5.0, "六" to 6.0, "七" to 7.0, "八" to 8.0,
        "九" to 9.0, "十" to 10.0,
    )

    // ═══════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════

    /**
     * 解析用户输入为 DayMealJson 列表。[AI修改] K1c：加 today 参数，支持 weekday→date_offset 推算。
     *
     * @param input 用户原始输入文本（内部会先过 TextNormalizer）
     * @param libraryIngredientNames 库内食材名列表（供 IngredientNameExtractor 匹配）
     * @param today 当前日期，用于 weekday→offset 推算（默认 DateTime.today()）
     * @return 至少 1 个 DayMealJson（最差返回空 meals 的占位对象）
     */
    fun parse(
        input: String,
        libraryIngredientNames: List<String> = emptyList(),
        today: LocalDate = DateTime.today(),
    ): List<DayMealJson> {
        val normalized = TextNormalizer.normalize(input)
        if (normalized.isBlank()) return listOf(DayMealJson(raw_input = input, parse_method = "rule"))

        val dayBlocks = TextSegmenter.segment(normalized)
        return dayBlocks.map { block -> parseDayBlock(block, libraryIngredientNames, today) }
    }

    // ═══════════════════════════════════════════════════
    // 单天解析
    // ═══════════════════════════════════════════════════

    private fun parseDayBlock(
        block: RawDayBlock,
        libraryNames: List<String>,
        today: LocalDate,
    ): DayMealJson {
        // 提取日期偏移（K1c：优先相对日期词，其次 weekday hint）
        val dateOffset = extractDateOffset(block, today)

        // 按餐次关键词分段
        val mealBlocks = splitByMealKeywords(block.text)

        val meals = mealBlocks.mapIndexed { index, mealText ->
            parseMealBlock(mealText, index, libraryNames)
        }

        return DayMealJson(
            date_offset = dateOffset,
            weekday = block.weekdayHint,
            meals = meals,
            raw_input = block.text,
            parse_method = "rule",
        )
    }

    /**
     * 从 RawDayBlock 提取日期偏移。[AI修改] K1c：优先相对词（昨天/前天等），兜底用 weekday hint 推算。
     *
     * 注意：weekday 只取"最近过去"（≤0），不取未来。
     */
    private fun extractDateOffset(block: RawDayBlock, today: LocalDate): Int {
        // 优先：文本内相对日期关键词
        val text = block.text
        when {
            Regex("""大前天""").containsMatchIn(text) -> return -3
            Regex("""前[天日]|前天|前儿|前日""").containsMatchIn(text) -> return -2
            Regex("""昨[天日]|昨儿|昨个|夜来|夜个""").containsMatchIn(text) -> return -1
            Regex("""今[天日]|今儿|今个""").containsMatchIn(text) -> return 0
            Regex("""明[天日]|明儿|明个|赶明""").containsMatchIn(text) -> return 1
            Regex("""后[天日]|后儿|赶后""").containsMatchIn(text) -> return 2
            Regex("""大后天""").containsMatchIn(text) -> return 3
        }
        // 兜底：块的 weekday hint（TextSegmenter 已解析出"周三"/"礼拜五"等）
        val weekdayIso = TextSegmenter.weekdayToIso(block.weekdayHint)
        if (weekdayIso != null) {
            return TextSegmenter.weekdayToDateOffset(weekdayIso, today)
        }
        return 0
    }

    // ═══════════════════════════════════════════════════
    // 餐次分段
    // ═══════════════════════════════════════════════════

    /**
     * 按餐次关键词将文本切为多个餐段。[AI生成]
     * 每个餐段 = [餐次词] + 后续文本到下一个餐次词之前。
     * 无餐次词→整段当一餐（按顺序推断餐次类型）。
     */
    private fun splitByMealKeywords(text: String): List<String> {
        // 收集所有餐次词在文本中的位置
        data class MatchPos(val start: Int, val end: Int, val mealType: String)
        val matches = mutableListOf<MatchPos>()

        for (splitter in MEAL_SPLITTERS) {
            for (kw in splitter.keywords) {
                var idx = text.indexOf(kw)
                while (idx >= 0) {
                    matches.add(MatchPos(idx, idx + kw.length, splitter.mealType))
                    idx = text.indexOf(kw, idx + 1)
                }
            }
        }

        if (matches.isEmpty()) return listOf(text)

        // 按位置排序，合并重叠匹配（取更长的）
        matches.sortBy { it.start }
        val merged = mutableListOf<MatchPos>()
        for (m in matches) {
            if (merged.isEmpty() || m.start >= merged.last().end) {
                merged.add(m)
            }
        }

        // 按匹配位置切分
        val result = mutableListOf<String>()
        for (i in merged.indices) {
            val start = merged[i].start
            val end = if (i + 1 < merged.size) merged[i + 1].start else text.length
            // 保留餐次关键词本身（后续解析需要）
            result.add(text.substring(start, end).trim())
        }

        // if 没有任何餐次词命中，返回原文本
        return result.ifEmpty { listOf(text) }
    }

    // ═══════════════════════════════════════════════════
    // 单餐解析
    // ═══════════════════════════════════════════════════

    private data class ParsedMeal(
        val mealType: String?,
        val mealTime: String?,
        val note: String,
        val dishes: List<MealDishRefJson>,
    )

    private fun parseMealBlock(
        text: String,
        index: Int,
        libraryNames: List<String>,
    ): MealJson {
        val mealType = extractMealType(text) ?: inferMealTypeByOrder(index)

        val time = defaultMealTime(mealType)

        // 提取备注并从文本中移除（避坑：备注不再被误拆为菜品）
        val note = extractMealNote(text)
        var cleanText = removeMealNote(text)

        // 去掉餐次关键词本身
        cleanText = removeMealKeywordPrefix(cleanText)

        // 拆菜品
        val dishTexts = splitDishes(cleanText)
        val dishes = dishTexts.map { parseDish(it, libraryNames) }

        return MealJson(
            meal_type = mealType,
            meal_time = time,
            note = note,
            dishes = dishes,
        )
    }

    /** 从文本提取餐次类型。[AI生成] */
    private fun extractMealType(text: String): String? {
        for (splitter in MEAL_SPLITTERS) {
            for (kw in splitter.keywords) {
                if (text.contains(kw)) return splitter.mealType
            }
        }
        return null
    }

    /** 按出现顺序推断餐次（第一个=早餐，第二个=午餐…）。[AI生成] */
    private fun inferMealTypeByOrder(index: Int): String = when (index) {
        0 -> "breakfast"
        1 -> "lunch"
        2 -> "dinner"
        else -> "snack"
    }

    /** 餐次默认时间。[AI生成] */
    private fun defaultMealTime(mealType: String?): String? = when (mealType) {
        "breakfast" -> "07:30"
        "lunch" -> "12:00"
        "dinner" -> "18:00"
        "snack" -> "15:00"
        else -> null
    }

    /** 提取整餐备注。[AI生成] */
    private fun extractMealNote(text: String): String {
        val notes = mutableListOf<String>()
        if (Regex("""少[盐放]盐|少盐|淡[一点些]|清淡|低盐""").containsMatchIn(text)) notes.add("少盐")
        if (Regex("""少油|少[放淋]油|低油|控油""").containsMatchIn(text)) notes.add("少油")
        if (Regex("""不[要放加]辣|免辣|去辣|微辣""").containsMatchIn(text)) notes.add("不要辣")
        if (Regex("""少糖|无糖|不加糖""").containsMatchIn(text)) notes.add("少糖")
        return notes.joinToString("；")
    }

    /** 从文本中移除已提取的备注关键词（避坑：防止"少放盐"被误拆为菜品）。[AI生成] */
    private fun removeMealNote(text: String): String {
        var t = text
        t = t.replace(Regex("""少[盐放]盐|少盐|淡[一点些]|清淡|低盐"""), "")
        t = t.replace(Regex("""少油|少[放淋]油|低油|控油"""), "")
        t = t.replace(Regex("""不[要放加]辣|免辣|去辣|微辣"""), "")
        t = t.replace(Regex("""少糖|无糖|不加糖"""), "")
        return t.trim()
    }

    /** 移除餐次关键词前缀。[AI生成] */
    private fun removeMealKeywordPrefix(text: String): String {
        var t = text
        for (splitter in MEAL_SPLITTERS) {
            for (kw in splitter.keywords) {
                if (t.startsWith(kw)) {
                    t = t.removePrefix(kw).trimStart(' ', ':', '：', '-', '—', '~')
                    return t
                }
            }
        }
        return t
    }

    // ═══════════════════════════════════════════════════
    // 菜品拆分
    // ═══════════════════════════════════════════════════

    /** 拆分菜品。[AI生成] */
    private fun splitDishes(text: String): List<String> {
        // 硬分隔
        val hardParts = text.split(HARD_SPLIT).map { it.trim() }.filter { it.isNotBlank() }
        if (hardParts.size > 1) {
            return hardParts.flatMap { part ->
                val softParts = splitSoft(part)
                if (softParts.size > 1) softParts else listOf(part)
            }
        }

        return splitSoft(text).ifEmpty { listOf(text) }
    }

    /** 软分隔：两端都能独立成菜名才拆。[AI生成] */
    private fun splitSoft(text: String): List<String> {
        val parts = SOFT_SPLIT.split(text).map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size >= 3 && parts.all { couldBeDish(it) }) {
            // 提取奇数字段（实际菜品名，跳过软分隔词本身）
            return parts.filterIndexed { i, _ -> i % 2 == 0 }
        }
        return emptyList()
    }

    /** 一个字符串是否可能是菜名（≥2字，含中文，非纯数字/标点）。[AI生成] */
    private fun couldBeDish(text: String): Boolean {
        val t = text.trim()
        if (t.length < 2) return false
        return t.any { it in '一'..'鿿' }
    }

    // ═══════════════════════════════════════════════════
    // 单菜解析
    // ═══════════════════════════════════════════════════

    private fun parseDish(text: String, libraryNames: List<String>): MealDishRefJson {
        var t = text.trim()
        if (t.isEmpty()) return MealDishRefJson(name = text)

        // 1. 括号说明→note
        var note = ""
        PAREN_NOTE.find(t)?.let { match ->
            note = match.groupValues[1].trim()
            t = t.replace(match.value, "").trim()
        }

        // 2. 食用比例
        var eatenRatio: Double? = null
        for ((pattern, ratio) in EATEN_PATTERNS) {
            if (pattern.containsMatchIn(t)) {
                eatenRatio = ratio
                t = t.replace(pattern, "").trim()
                break
            }
        }

        // 3. 份量+单位
        var quantity = 1.0
        var unit = "份"
        QUANTITY_UNIT.find(t)?.let { match ->
            quantity = chineseNumToDouble(match.groupValues[1])
            unit = match.groupValues[2]
            t = t.removeRange(match.range).trim()
        }

        // 4. 菜名
        val name = t.ifBlank { text.trim() }

        // 5. 烹饪方式提取（从菜名中识别做法关键词）
        val cookingMethods = extractCookingMethods(name)

        // 6. 食材推演
        val extracted = IngredientNameExtractor.extract(name, libraryNames)
        val dishJson = if (extracted.isNotEmpty()) {
            DishJson(
                name = name,
                cooking_methods = cookingMethods,
                ingredients = extracted.map { e ->
                    DishIngredientJson(
                        ref = if (e.inLibrary) e.name else null,
                        food = if (!e.inLibrary) FoodJson(name = e.name, source = "ai") else null,
                        quantity = if (e.inLibrary) 100.0 else 100.0,
                        is_main = true,
                    )
                },
                source = "ai",
            )
        } else null

        return MealDishRefJson(
            ref = null, // 规则引擎不查已有菜品，统一走新建（入库时 dishRepo.dishIdByName 会自动复用）
            dish = dishJson,
            name = name,
            quantity = quantity,
            quantity_unit = unit,
            eaten_ratio = eatenRatio,
            note = note,
        )
    }

    /** 从菜名中识别烹饪方式关键词。[AI生成] */
    private fun extractCookingMethods(dishName: String): List<String> {
        val methods = listOf("炒", "煮", "蒸", "炸", "煎", "烤", "炖", "拌", "烧", "焖", "卤", "熘", "焗", "烩", "涮", "煲", "炝", "熬")
        return methods.filter { dishName.contains(it) }
    }

    /** 中文数字→Double。[AI生成] */
    private fun chineseNumToDouble(s: String): Double {
        CHINESE_NUM_MAP[s]?.let { return it }
        return s.toDoubleOrNull() ?: 1.0
    }
}
