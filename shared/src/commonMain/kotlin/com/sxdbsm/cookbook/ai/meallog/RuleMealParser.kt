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

    // 菜品分隔词：规则模板约定顶层菜品只用逗号；括号内的逗号/+ /、是食材分隔，必须由括号深度保护。
    private val SOFT_SPLIT = Regex("""(和|跟|还有|以及|加上|配上?|搭配|搭|就着|再来|外加|捎带|另|另外|还有一个|再加上|然后)""")

    // 份量+单位
    private val QUANTITY_UNIT = Regex("""^([一二两三四五六七八九十百半\\d]+\\.?\\d*)\\s*([个大碗盘碟只根片块勺份杯瓶盒袋笼屉][子]?)""")

    // 食用比例
    private val EATEN_PATTERNS = listOf(
        Regex("""(吃了?)?一半|剩[了]?一半|半份""") to 0.5,
        Regex("""(吃了?)?大半""") to 0.75,
        Regex("""少量|一点点|[一就]点[点]?|尝了?[一]?口|几口""") to 0.25,
    )

    // 括号备注
    private val PAREN_NOTE = Regex("""[（(]([^）)]+)[）)]""")
    // 时间必须在切菜前移除；否则“晚上七点半”会落入菜名。
    private val EXPLICIT_TIME = Regex("""(?:(凌晨|早上|上午|中午|下午|晚上|傍晚|夜里|夜间)\s*)?(\d{1,2}|[一二三四五六七八九十两]+)(?::|点|时)(\d{1,2}|[一二三四五六七八九十两]+|半)?(?:分)?""")
    private val LEADING_ABSOLUTE_DATE = Regex("""^(?:\d{4}[-/]\d{1,2}[-/]\d{1,2}|\d{1,2}月\d{1,2}[日号]|[一二三四五六七八九十两〇零]+月[一二三四五六七八九十两〇零]+[日号])\s*""")

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
        val days = dayBlocks.map { block -> parseDayBlock(block, libraryIngredientNames, today) }
        return MealParseCanonicalizer.canonicalize(anchorMultiDayPlan(days, today))
    }

    /**
     * 多天菜单不是历史回忆：星期必须落在目标日所在周，不能沿用“最近过去”算法。
     * [AI修改] 这是此前生成代码把周二/周三写到上周的根因；单天仍保留补记语义。
     */
    fun anchorMultiDayPlan(days: List<DayMealJson>, targetDate: LocalDate): List<DayMealJson> {
        if (days.size <= 1) return days
        val mondayOffset = targetDate.dayOfWeek.ordinal
        return days.map { day ->
            // [AI修改] 显式绝对日期是 AI/用户语义真相，周锚点不得覆盖。
            if (!day.date.isNullOrBlank()) return@map day
            val weekday = TextSegmenter.weekdayToIso(day.weekday) ?: return@map day
            day.copy(date = null, date_offset = weekday - 1 - mondayOffset)
        }
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

        val absoluteDate = resolveAbsoluteDate(block.dateHint, today)
        return DayMealJson(
            date = absoluteDate?.toString(),
            date_offset = if (absoluteDate == null) dateOffset else 0,
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
            val previous = merged.lastOrNull()
            if (previous == null || m.start >= previous.end) {
                // “晚上七点半晚饭”是一个晚餐块，不可被两个同类关键词切成两餐。
                val between = previous?.let { text.substring(it.end, m.start).trim() }.orEmpty()
                if (previous != null && previous.mealType == m.mealType && EXPLICIT_TIME.matches(between)) {
                    merged[merged.lastIndex] = previous.copy(end = m.end)
                } else {
                    merged.add(m)
                }
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

        val time = extractExplicitMealTime(text) ?: defaultMealTime(mealType)

        // 提取备注并从文本中移除（避坑：备注不再被误拆为菜品）
        val note = extractMealNote(text)
        var cleanText = removeMealNote(text)

        // 去掉餐次关键词本身
        cleanText = removeMealKeywordPrefix(cleanText)
        cleanText = cleanText.replace(EXPLICIT_TIME, "").trim()
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

    /** 解析常见显式时刻，如 12:30、晚上七点半；解析失败仍交由餐次默认时间。 [AI修改] */
    private fun extractExplicitMealTime(text: String): String? {
        val match = EXPLICIT_TIME.find(text) ?: return null
        var hour = parseNumber(match.groupValues[2]) ?: return null
        val minuteToken = match.groupValues[3]
        val minute = when (minuteToken) {
            "" -> 0
            "半" -> 30
            else -> parseNumber(minuteToken) ?: return null
        }
        if (minute !in 0..59 || hour !in 0..23) return null
        when (match.groupValues[1]) {
            "下午", "晚上", "傍晚", "夜里", "夜间" -> if (hour in 1..11) hour += 12
            "中午" -> if (hour in 1..10) hour += 12
        }
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
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

    /** 移除餐次关键词前缀。[AI修改] Bug修复：增加吃/喝动词+日期词剥离 */
    private fun removeMealKeywordPrefix(text: String): String {
        var t = text
        var removed: Boolean
        do {
            removed = false
            for (splitter in MEAL_SPLITTERS) {
                for (kw in splitter.keywords) {
                    if (t.startsWith(kw)) {
                        val raw = t.removePrefix(kw)
                        val candidate = raw.trimStart(' ', ':', '：', '-', '—', '~')
                        // [AI修改] 输入格式统一：守卫"二次剥前缀"把菜名剥残——"午餐: 午餐肉"第二轮
                        // 会把"午餐肉"剥成"肉"（1 字）。仅当餐次词与残料之间**没有分隔符**且残料非空
                        // 不足 2 字时，才视为剥掉的是菜名一部分而拒绝（"早餐饼"保住）；kw 后有分隔符
                        // （冒号/空格等，raw 与 candidate 等长说明没剥掉分隔符）即真前缀，照剥（"早餐 粥"→"粥"）；
                        // 剥后为空（纯餐次词输入，如"午饭"）也允许。复审：单字菜名（粥/汤/面）靠分隔符分支放行。
                        val hasSeparator = raw.length != candidate.length
                        if (!hasSeparator && candidate.isNotEmpty() && candidate.length < 2) break
                        t = candidate
                        removed = true
                        break
                    }
                }
                if (removed) break
            }
        } while (removed)
        // 剥离句首吃/喝动词上下文（类别化迭代贪心·EatDrinkStripper）
        t = EatDrinkStripper.strip(t)
        // 剥离裸露日期/时间词（无餐次关键词时残留，如"昨天红烧肉"→"红烧肉"）
        t = t.replace(Regex("""^(今天|今儿|今个|昨天|昨儿|昨个|前天|前儿|明天|明儿|后天|大前天|大后天)\s*"""), "")
        t = t.replace(LEADING_ABSOLUTE_DATE, "")
        return t
    }

    /** 日期块中的显式日期优先于 weekday/相对词；无年份时取目标日期所在年。 [AI修改] */
    private fun resolveAbsoluteDate(dateHint: String?, today: LocalDate): LocalDate? {
        val hint = dateHint?.trim().orEmpty()
        if (hint.isBlank()) return null
        val parts = Regex("""^(?:(\d{4})[-/])?([^月/-]+)[月/-]([^日号]+)""").find(hint)?.groupValues ?: return null
        val year = parts[1].toIntOrNull() ?: today.year
        val month = parseNumber(parts[2]) ?: return null
        val day = parseNumber(parts[3]) ?: return null
        return runCatching { LocalDate(year, month, day) }.getOrNull()
    }

    private fun parseNumber(raw: String): Int? {
        raw.toIntOrNull()?.let { return it }
        val digits = mapOf('零' to 0, '〇' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4, '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9)
        if (raw == "十") return 10
        if ('十' in raw) {
            val split = raw.split('十')
            val tens = split[0].takeIf { it.isNotBlank() }?.let { token -> token.singleOrNull()?.let(digits::get) } ?: 1
            val ones = split.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { token -> token.singleOrNull()?.let(digits::get) } ?: 0
            return tens * 10 + ones
        }
        return digits[raw.singleOrNull()]
    }

    // ═══════════════════════════════════════════════════
    // 菜品拆分
    // ═══════════════════════════════════════════════════

    /** 拆分菜品。[AI生成] */
    private fun splitDishes(text: String): List<String> {
        // 硬分隔
        // [AI修改] 仅在顶层拆菜，括号内的“+”属于配料而不是另一道菜。
        val hardParts = MealParseCanonicalizer.splitTopLevel(text, ",，、+\n")
        if (hardParts.size > 1) {
            return hardParts.flatMap { part ->
                val softParts = splitSoft(part)
                if (softParts.size > 1) softParts else listOf(part)
            }
        }

        return splitSoft(text).ifEmpty { listOf(text) }
    }

    /** 软分隔：两端都能独立成菜名才拆。[AI修改] 用 findAll 显式切分，避免 Regex.split 捕获组跨平台差异。 */
    private fun splitSoft(text: String): List<String> {
        val matches = SOFT_SPLIT.findAll(text)
            .filter { MealParseCanonicalizer.isTopLevel(text, it.range.first) }
            .toList()
        if (matches.isEmpty()) return emptyList()

        // 按分隔词位置手动切分
        val parts = mutableListOf<String>()
        var lastEnd = 0
        for (m in matches) {
            val before = text.substring(lastEnd, m.range.first).trim()
            if (before.isNotBlank()) parts.add(before)
            lastEnd = m.range.last + 1
        }
        val after = text.substring(lastEnd).trim()
        if (after.isNotBlank()) parts.add(after)

        if (parts.size >= 2 && parts.all { couldBeDish(it) }) return parts
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

        // 1. 括号内容→食材提示+note。[AI修改] "凉皮（黄瓜丝+绿豆芽）"→食材含黄瓜丝+绿豆芽
        var note = ""
        val parenIngredients = mutableListOf<IngredientNameExtractor.ExtractedIngredient>()
        PAREN_NOTE.find(t)?.let { match ->
            val parenText = match.groupValues[1].trim()
            // 括号内是食材说明（含+或、或,或纯中文≥2字）→拆为食材
            // [AI修改] 输入格式统一：TextNormalizer 已把全角，/、归一为半角逗号，此处判断集与
            // split 正则必须包含半角","，否则"菜（食材1，食材2）"会合并成一个名字带逗号的单一食材。
            if (parenText.any { it in setOf('+', '、', '，', ',', ' ') } || parenText.length >= 2) {
                // [AI修改] 复审：过滤 <2 字段——"（少 盐）"这类按空格拆出的单字段会以 FoodJson(source="ai")
                // 落库污染食材字典（与 couldBeDish 的 ≥2 字口径一致）。
                val segments = parenText.split(Regex("""[+、，, ]+""")).map { it.trim() }.filter { it.length >= 2 }
                parenIngredients.addAll(segments.map { IngredientNameExtractor.ExtractedIngredient(it, inLibrary = false) })
            } else {
                note = parenText  // 纯备注（如"加热""少盐"）
            }
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

        // 6. 食材推演。[AI修改] E-IFMT-05 真机反馈修复（用户 2026-08-29 口径：菜品本身不能算食材，只有（）内的才算）：
        //   ① 有括号食材时括号是权威声明，跳过菜名推演——"百页烧肉（百叶结，五花肉）"食材=百叶结+五花肉，
        //      不再混入菜名推演结果（旧逻辑把整名"百页烧肉"也当食材）；
        //   ② 无括号时推演结果剔除"整名==菜名"项——extractor 对拆不动的菜名会把整段当库外候选
        //      （"凉皮"整名进食材），按新口径菜名不是食材。
        val extracted: MutableList<IngredientNameExtractor.ExtractedIngredient> =
            if (parenIngredients.isNotEmpty()) {
                parenIngredients.toMutableList()
            } else {
                IngredientNameExtractor.extract(name, libraryNames)
                    .filterNot { it.name == name }
                    .toMutableList()
            }
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

/**
 * AI 与规则解析共用的结果整理入口。
 * [AI生成] 在入库前统一清理空白字段并固定 AI 创建实体的来源，避免两条路径语义漂移。
 */
internal object MealParseCanonicalizer {
    fun canonicalize(days: List<DayMealJson>): List<DayMealJson> = days.map { day ->
        day.copy(
            meals = day.meals.map { meal ->
                meal.copy(
                    dishes = meal.dishes.mapNotNull { dish ->
                        val name = (dish.dish?.name ?: dish.ref ?: dish.name).trim()
                        if (name.isBlank()) null else dish.copy(
                            name = name,
                            ref = dish.ref?.trim()?.takeIf { it.isNotEmpty() },
                            dish = dish.dish?.copy(
                                name = dish.dish.name.trim(),
                                source = "ai",
                                ingredients = dish.dish.ingredients.map { ingredient ->
                                    ingredient.copy(food = ingredient.food?.copy(
                                        name = ingredient.food.name.trim(),
                                        source = "ai",
                                    ))
                                },
                            ),
                        )
                    },
                )
            },
        )
    }

    /** 按顶层分隔符切分；不完整括号保持原文，避免丢失用户输入。 */
    fun splitTopLevel(text: String, separators: String): List<String> {
        val parts = mutableListOf<String>()
        var start = 0
        var depth = 0
        text.forEachIndexed { index, char ->
            when (char) {
                '(', '（' -> depth++
                ')', '）' -> if (depth > 0) depth--
                else -> if (depth == 0 && char in separators) {
                    text.substring(start, index).trim().takeIf { it.isNotEmpty() }?.let(parts::add)
                    start = index + 1
                }
            }
        }
        text.substring(start).trim().takeIf { it.isNotEmpty() }?.let(parts::add)
        return parts
    }

    fun isTopLevel(text: String, index: Int): Boolean {
        var depth = 0
        text.take(index).forEach { char ->
            when (char) {
                '(', '（' -> depth++
                ')', '）' -> if (depth > 0) depth--
            }
        }
        return depth == 0
    }
}
