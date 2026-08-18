package com.sxdbsm.cookbook.ai.meallog

import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate

/**
 * @File : AiMealParser
 * @Time : 2026/07/28
 * @Author : SXD-AI
 * @Desc : AI 快捷输入记餐的 JSON 解析 + 校验 + 本地规则兜底
 * <p>
 * 解析 AI 返回的 JSON，优先扁平格式(FlatMealJson→DayMealJson)，回退旧嵌套格式(AiMealParseResult)。
 * 失败/非法时返回 null，上层走 RuleMealParser 本地规则兜底。
 * 纯函数，无副作用，可单测。
 * <p>
 * [AI修改] 修复 AI 模式 Schema 不匹配：AI 输出 items(FlatMealJson) vs Parser 期望 meals(AiMealParseResult)
 * → 新增 parseToDayMealJsonList() 优先尝试扁平格式。
 * <p>
 * ⚠️ [AI修改 2026-08-18] 无生产调用方（全仓 grep 确认）：B3 NDJSON 流式改造后快速记/周期记
 * 均走 StreamingMealParser，本文件仅存量供其自身单测使用。改动前先确认是否应直接删除。
 **/
object AiMealParser {

    /** AI 解析的结构化结果；警告必须传到预览，错误必须阻断写库。 [AI修改] */
    data class ParseOutcome(
        val days: List<DayMealJson> = emptyList(),
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList(),
    ) {
        val isValid: Boolean get() = errors.isEmpty() && days.isNotEmpty()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        // [AI修改] LLM 常把可选字段写为 null；有默认值的非空字段应回退默认值，不能令整份餐食失效。
        coerceInputValues = true
    }

    /**
     * 解析 AI 响应 → DayMealJson 列表。[AI修改]
     *
     * 优先尝试当前 AI prompt 的扁平格式(FlatMealJson)，
     * 回退尝试旧嵌套格式(AiMealParseResult)，两者都失败返回 null。
     *
     * @param aiResponse AI 返回的原始文本（期望是纯 JSON）
     * @return 至少 1 个 DayMealJson；null=解析失败（上层走规则兜底）
     */
    fun parseToDayMealJsonList(aiResponse: String): List<DayMealJson>? {
        val outcome = parseOutcome(aiResponse, LocalDate(1970, 1, 1))
        return outcome.days.takeIf { outcome.isValid }
    }

    /** 解码→迁移/聚合→规范化→严格校验。 [AI修改] */
    fun parseOutcome(aiResponse: String, fallbackDate: LocalDate): ParseOutcome {
        val trimmed = aiResponse.trim()
        if (trimmed.isEmpty()) return ParseOutcome(errors = listOf("AI 返回为空"))
        val jsonText = extractJson(trimmed)
        if (jsonText.isBlank()) return ParseOutcome(errors = listOf("AI 返回中没有 JSON"))

        // ① 优先：扁平格式 FlatMealJson（当前 AI prompt 的输出格式）
        val flatDecode = runCatching {
            json.decodeFromString<FlatMealJson>(jsonText)
        }
        val flatResult = flatDecode.getOrNull()
        if (flatResult != null && flatResult.items.isNotEmpty()) {
            val converted = FlatToDayMealConverter.convert(flatResult, fallbackDate)
            return validate(MealParseCanonicalizer.canonicalize(converted.days), converted.warnings, converted.errors)
        }

        val flatError = flatDecode.exceptionOrNull()?.message?.take(500)
        return ParseOutcome(errors = listOf("AI 返回不符合当前扁平餐食格式${flatError?.let { ": $it" }.orEmpty()}"))
    }

    private fun validate(days: List<DayMealJson>, warnings: List<String>, errors: List<String>): ParseOutcome {
        val validation = SchemaValidator.validate(MultiDayJson(days = days))
        return ParseOutcome(days, (warnings + validation.warnings).distinct(), (errors + validation.errors).distinct())
    }

    /** 从可能含 markdown 代码块的文本中提取 JSON。[AI生成] */
    private fun extractJson(text: String): String {
        // "```json ... ```" 或 "``` ... ```"
        val md = Regex("```(?:json)?\\s*\\n?([\\s\\S]*?)```", RegexOption.MULTILINE)
        val match = md.find(text)
        if (match != null) return match.groupValues[1].trim()
        // 找第一个 { 到最后一个 } 之间的内容
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start >= 0 && end > start) return text.substring(start, end + 1)
        return text
    }

    // ===== 本地规则兜底 =====

    /**
     * 本地规则兜底解析（AI 不可用时）。[AI生成]
     *
     * 纯正则+关键词解析，不依赖网络。覆盖短句，长句/复合句仍建议走 AI。
     */
    @Suppress("UNUSED_PARAMETER")
    fun localFallback(input: String, today: String = "", nowTime: String = ""): AiMealParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return AiMealParseResult()

        // 1. 日期偏移
        val dateOffset = when {
            Regex("""前天""").containsMatchIn(trimmed) -> -2
            Regex("""昨[天晚]|昨天""").containsMatchIn(trimmed) -> -1
            Regex("""明[天早]|明天""").containsMatchIn(trimmed) -> 1
            else -> 0
        }

        // 2. 餐次
        val mealType: String? = when {
            Regex("""早[饭餐]|早饭|早上""").containsMatchIn(trimmed) -> "breakfast"
            Regex("""午[饭餐]|中[午饭]|中午""").containsMatchIn(trimmed) -> "lunch"
            Regex("""晚[饭餐]|晚饭|晚上""").containsMatchIn(trimmed) -> "dinner"
            Regex("""加餐|宵夜|夜宵|零食|下午茶""").containsMatchIn(trimmed) -> "snack"
            else -> null
        }

        // 3. 用餐时间
        val mealTime: String? = when {
            mealType == "breakfast" -> "07:30"
            mealType == "lunch" -> "12:00"
            mealType == "dinner" -> "18:00"
            mealType == "snack" -> "15:00"
            else -> nowTime.ifBlank { null }
        }

        // 4. 整餐备注
        val note = extractMealNote(trimmed)

        // 5. 菜品拆分 + 份量 + 食用比例
        val dishes = splitDishes(trimmed)

        return AiMealParseResult(
            date_offset = dateOffset,
            meals = listOf(
                AiParsedMeal(
                    meal_type = mealType,
                    meal_time = mealTime,
                    note = note,
                    dishes = dishes,
                ),
            ),
        )
    }

    /** 提取整餐备注（"少盐""少油""清淡""不要辣"等）。[AI生成] */
    private fun extractMealNote(text: String): String {
        val notes = mutableListOf<String>()
        if (Regex("""少[盐放]盐|少盐|淡[一点些]|清淡""").containsMatchIn(text)) notes.add("少盐")
        if (Regex("""少油|少[放淋]油""").containsMatchIn(text)) notes.add("少油")
        if (Regex("""不[要放加]辣|免辣|去辣""").containsMatchIn(text)) notes.add("不要辣")
        return notes.joinToString("；")
    }

    /**
     * 拆分菜品。[AI生成]
     *
     * 分隔策略：
     * - 硬分隔：`、` `，` `,` `+` → 直接拆分
     * - 软分隔：`和` `跟` `还有` → 两端都能独立成菜名则拆
     */
    private fun splitDishes(text: String): List<AiParsedDish> {
        // 先去日期/餐次前缀和时间词，留菜品部分
        var body = text
            .replace(Regex("""(昨天|前天|明天|今天)(早上|中午|晚上|下午)?"""), "")
            .replace(Regex("""(早餐|中餐|午餐|晚饭|晚餐|早饭|中饭|午饭|中午|早上|晚上|下午|吃了?|刚[刚才]|加餐|宵夜)"""), "")
            .trim()

        // 硬分隔
        // [AI修改] 与规则解析共用顶层切分，配料括号中的“+”不得拆成菜品。
        val hardSplit = MealParseCanonicalizer.splitTopLevel(body, "、，,+")
        if (hardSplit.size > 1) {
            return hardSplit.flatMap { splitSoft(it) }
        }

        return splitSoft(body)
    }

    /** 软分隔：按 "和""跟""还有" 拆分，两端都能独立成菜名才拆。[AI修改] 修复分隔词被当菜品+单字分隔词误杀 */
    private fun splitSoft(segment: String): List<AiParsedDish> {
        // 先尝试软分隔拆分
        val parts = segment.split(Regex("""(和|跟|还有)""")).map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size >= 3) {
            // 只检查偶数位（实际菜名），分隔词（奇数位）不参与检查也不建菜
            val dishParts = parts.filterIndexed { i, _ -> i % 2 == 0 }
            if (dishParts.all { couldBeDishName(it) }) {
                return dishParts.map { parseDish(it) }
            }
        }
        // 不拆分，整个是一道菜
        return listOf(parseDish(segment))
    }

    /** 判断一个片段是否可能是一道菜名（≥2个中文字，不含连接词）。[AI生成] */
    private fun couldBeDishName(text: String): Boolean {
        val t = text.trim()
        if (t.length < 2) return false
        // 至少含一个中文字符
        if (!Regex("""[一-鿿]""").containsMatchIn(t)) return false
        return true
    }

    /**
     * 解析单道菜（份量+单位+食用比例）。[AI生成]
     *
     * 典型输入：
     * - "红烧肉" → quantity=1, unit="份"
     * - "三个鸡蛋" → quantity=3, unit="个"
     * - "一大碗面" → quantity=1, unit="碗"（"大碗"归单位）
     * - "半份米饭" → quantity=0.5, unit="份"
     * - "红烧肉一半" → eaten_ratio=0.5
     */
    private fun parseDish(text: String): AiParsedDish {
        var t = text.trim()
        var quantity = 1.0
        var unit = "份"
        var eatenRatio: Double? = null

        // 食用比例
        if (Regex("""一半|吃了一半|剩[了]?一半""").containsMatchIn(t)) {
            eatenRatio = 0.5
            t = t.replace(Regex("""(吃了?)?一半|剩[了]?一半"""), "").trim()
        } else if (Regex("""大半|吃了大半""").containsMatchIn(t)) {
            eatenRatio = 0.75
            t = t.replace(Regex("""(吃了?)?大半"""), "").trim()
        } else if (Regex("""少量|一点点|[一就]点""").containsMatchIn(t)) {
            eatenRatio = 0.25
            t = t.replace(Regex("""(吃了?)?少量|一点点|[一就]点"""), "").trim()
        }

        // 份量 + 单位："三个鸡蛋"→3个，"两碗饭"→2碗，"一大碗面"→1大碗
        val qtyMatch = Regex("""^([一二两三四五六七八九十百半\d]+\.?\d*)\s*([个大碗盘碟只根片块勺份][子]?)""").find(t)
        if (qtyMatch != null) {
            val numStr = qtyMatch.groupValues[1]
            quantity = chineseNumberToDouble(numStr)
            unit = qtyMatch.groupValues[2]
            t = t.removeRange(qtyMatch.range).trim()
        } else {
            // "红烧肉" 前面的 "红烧" 是做法不是数字，保持默认 1份
        }

        // 去烹饪方式前缀留菜名（"红烧肉"→"红烧肉"是完整菜名，不剥离"红烧"）
        // 菜名就是剩余文本
        val name = t.ifBlank { text.trim() } // 全被剥离了就用原文本

        return AiParsedDish(
            name = name,
            quantity = quantity,
            quantity_unit = unit,
            eaten_ratio = eatenRatio,
        )
    }

    /** 中文数字→Double（支持"半"=0.5）。[AI生成] */
    private fun chineseNumberToDouble(s: String): Double = when (s) {
        "半" -> 0.5
        "一" -> 1.0; "二", "两" -> 2.0; "三" -> 3.0; "四" -> 4.0; "五" -> 5.0
        "六" -> 6.0; "七" -> 7.0; "八" -> 8.0; "九" -> 9.0; "十" -> 10.0
        else -> s.toDoubleOrNull() ?: 1.0
    }
}
