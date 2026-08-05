package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json

/**
 * @File : StreamingMealParser
 * @Time : 2026/08/05
 * @Author : SXD-AI
 * @Desc : NDJSON 流式餐食解析器——接收 Delta 增量文本，按行解析 NDJSON 事件，
 * 执行归属校验与日期锚定，逐步构建 MealStreamDraft。
 * <p>
 * 职责：行缓冲、NDJSON 解析、归属校验、整体 JSON 回退、截断处理。
 * 不涉及：网络 IO、UI 状态、数据库写操作。
 * <p>
 * [AI修改] AF-03~05 修复：归属校验严格化、整体JSON进入同链、字段合并/去重/唯一补挂。
 * <p>
 * [AI生成] B1 周期记+NDJSON流式改造：协议解析层。
 **/
class StreamingMealParser(
    private val segments: List<InputSegment>,
    private val generationId: String,
    private val fallbackDate: LocalDate,
) {
    // 内部状态
    private val lineBuffer = StringBuilder()
    private val allRawText = StringBuilder()
    private val segmentMap = linkedMapOf<String, MutableSegmentDraft>()
    private val knownSegmentIds: Set<String> = segments.map { it.segmentId }.toSet()
    private val orphanDiagnostics = mutableListOf<StreamDiagnostic>()
    private var hasAnyNdjsonEvent = false
    private var finishReason: String? = null
    private var isLengthTruncated = false
    private var totalReceivedChars = 0

    // 整体 JSON 回退结果
    private var jsonFallbackDays: List<DayMealJson>? = null
    private var jsonFallbackErrors: List<String>? = null

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    // ═══════════════════════════════ 公开 API ═══════════════════════════════

    val currentDraft: MealStreamDraft get() = buildDraft()

    fun feedDelta(text: String) {
        if (text.isEmpty()) return
        totalReceivedChars += text.length
        allRawText.append(text)
        lineBuffer.append(text)

        val content = lineBuffer.toString()
        val lastNewline = content.lastIndexOf('\n')
        if (lastNewline < 0) return

        val complete = content.substring(0, lastNewline + 1)
        val remaining = content.substring(lastNewline + 1)
        lineBuffer.clear()
        lineBuffer.append(remaining)

        complete.lines().forEach { line ->
            val trimmed = line.trim('\r', '\n', ' ').takeIf { it.isNotEmpty() } ?: return@forEach
            processLine(trimmed)
        }
    }

    fun finish(reason: String): MealStreamDraft {
        finishReason = reason
        isLengthTruncated = reason == "length"

        val tail = lineBuffer.toString().trim()
        if (tail.isNotEmpty()) {
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.WARNING, null, null, null,
                    "响应结束时仍有 ${tail.length} 字符的未完成内容，已丢弃")
            )
            lineBuffer.clear()
        }

        if (!hasAnyNdjsonEvent) {
            tryWholeJsonFallback()
        }

        if (isLengthTruncated) {
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.WARNING, null, null, null,
                    "模型输出被截断（finish_reason=length），已保留截断前完整内容")
            )
        }

        return buildDraft()
    }

    // ═══════════════════════════════ 行解析 + 路由 ═══════════════════════════════

    private fun processLine(line: String) {
        val parsed = runCatching {
            json.decodeFromString<NdjsonLine>(line)
        }.getOrElse { e ->
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.ERROR, null, null, null,
                    "无法解析 NDJSON 行: ${e.message?.take(120)}")
            )
            return
        }

        // AF-03: segment_id 必须精确匹配已知分段，否则拒绝
        if (parsed.segment_id.isEmpty()) {
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.ERROR, null, null, null,
                    "NDJSON 行缺少 segment_id，已丢弃: ${line.take(80)}")
            )
            return
        }

        if (parsed.segment_id !in knownSegmentIds) {
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.ERROR, parsed.segment_id, null, null,
                    "segment_id「${parsed.segment_id}」不匹配本次请求的任何分段，已拒绝")
            )
            return // AF-03: 拒绝，不创建 segment
        }

        when (parsed.type) {
            "meal" -> handleMealEvent(parsed)
            "dish" -> handleDishEvent(parsed)
            "ingredient" -> handleIngredientEvent(parsed)
            "seasoning" -> handleSeasoningEvent(parsed)
            "cooking_step" -> handleCookingStepEvent(parsed)
            "warning" -> handleWarningEvent(parsed)
            "advice" -> handleAdviceEvent(parsed)
            "done" -> handleDoneEvent(parsed)
            else -> orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.WARNING, parsed.segment_id, null, null,
                    "未知事件类型「${parsed.type}」，已忽略")
            )
        }
    }

    // ═══════════════════════════════ Meal ═══════════════════════════════

    private fun handleMealEvent(line: NdjsonLine) {
        val mealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null, "meal 事件缺少 meal_id"))
            return
        }
        val date = line.date ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null, "meal 事件缺少 date"))
            return
        }
        val slot = line.slot ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null, "meal 事件缺少 slot"))
            return
        }

        // date 格式校验
        val validDate = runCatching { LocalDate.parse(date.replace('/', '-')) }.getOrNull()
        if (validDate == null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null, "meal 日期「$date」无效"))
            return
        }

        // AF-03: slot 非法则拒绝（不创建）
        if (slot !in VALID_SLOTS) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null,
                "餐次类型「$slot」无效，合法值: ${VALID_SLOTS.joinToString()}，已拒绝"))
            return
        }

        // meal_id 与 date|slot 必须一致
        val expectedMealId = "$date|$slot"
        if (mealId != expectedMealId) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null,
                "meal_id「$mealId」与 date|slot「$expectedMealId」不一致，已拒绝"))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }
        // AF-05: 同键合并——以最新非空字段覆盖
        val existing = seg.meals[mealId]
        if (existing == null) {
            seg.meals[mealId] = MealDraftNode(mealId = mealId, date = date, slot = slot, time = line.time, note = line.note)
        } else {
            seg.meals[mealId] = existing.copy(
                time = line.time ?: existing.time,
                note = mergeNote(existing.note, line.note),
            )
        }
    }

    // ═══════════════════════════════ Dish ═══════════════════════════════

    private val DISH_ID_PATTERN = Regex("""^(.+)\|d(\d+)$""")

    private fun handleDishEvent(line: NdjsonLine) {
        val mealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null, "dish 缺少 meal_id"))
            return
        }
        val dishId = line.dish_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null, "dish 缺少 dish_id"))
            return
        }
        val name = line.name ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId, "dish 缺少 name"))
            return
        }

        // AF-03: 校验 dish_id 格式 = {meal_id}|d{正整数}
        val dishIdMatch = DISH_ID_PATTERN.matchEntire(dishId)
        if (dishIdMatch == null || dishIdMatch.groupValues[1] != mealId) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "dish_id「$dishId」格式无效（期望 {meal_id}|d{正整数}），已拒绝"))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }

        // 查找父餐次
        if (mealId !in seg.meals) {
            // AF-03: 补建父餐次仅允许同一 dish 事件的合法 date+slot 与 meal_id/dish_id 同时精确一致
            val date = line.date
            val slot = line.slot
            if (date != null && slot != null && slot in VALID_SLOTS) {
                val newMealId = "$date|$slot"
                val validDate = runCatching { LocalDate.parse(date.replace('/', '-')) }.getOrNull()
                // meal_id 必须与 date|slot 一致
                if (validDate != null && mealId == newMealId) {
                    seg.meals[newMealId] = MealDraftNode(
                        mealId = newMealId, date = date, slot = slot,
                        warnings = listOf("菜品「$name」补建了父餐次「$newMealId」"),
                    )
                } else {
                    orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                        "dish meal_id「$mealId」与 date|slot「$newMealId」不一致，补建失败"))
                    return
                }
            } else {
                orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                    "菜品「$name」的 meal_id「$mealId」未找到父餐次且无法补建，进入待确认"))
                return
            }
        }

        // 冲突检测：dish_id 已存在于其他 meal_id
        val conflictingMeal = seg.meals.values.find { m -> m.mealId != mealId && dishId in m.dishes }
        if (conflictingMeal != null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "dish_id「$dishId」已存在于 meal「${conflictingMeal.mealId}」，拒绝跨餐冲突"))
            return
        }

        // AF-05: 同键合并——合并非空字段，保留已有子项
        val meal = seg.meals[mealId]!!
        val existingDish = meal.dishes[dishId]
        val dishNode = if (existingDish == null) {
            DishDraftNode(dishId = dishId, name = name,
                cookingMethod = line.cooking_method, quantity = line.quantity,
                unit = line.unit, eatenRatio = line.eaten_ratio, note = line.note)
        } else {
            existingDish.copy(
                name = name, // name always from latest
                cookingMethod = line.cooking_method ?: existingDish.cookingMethod,
                quantity = line.quantity ?: existingDish.quantity,
                unit = line.unit ?: existingDish.unit,
                eatenRatio = line.eaten_ratio ?: existingDish.eatenRatio,
                note = mergeNote(existingDish.note, line.note),
            )
        }
        seg.meals[mealId] = meal.copy(dishes = meal.dishes + (dishId to dishNode))
    }

    // ═══════════════════════════════ Ingredient / Seasoning ═══════════════════════════════

    private fun handleIngredientEvent(line: NdjsonLine) {
        handleDishChildEvent(line, isSeasoning = false)
    }

    private fun handleSeasoningEvent(line: NdjsonLine) {
        handleDishChildEvent(line, isSeasoning = true)
    }

    private fun handleDishChildEvent(line: NdjsonLine, isSeasoning: Boolean) {
        val mealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null, "缺少 meal_id"))
            return
        }
        val name = line.name ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null, "缺少 name"))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }
        val meal = seg.meals[mealId]
        if (meal == null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null,
                "食材/调料「$name」的 meal_id「$mealId」不存在"))
            return
        }

        // 先按 dish_id 查找
        var dish: DishDraftNode? = line.dish_id?.let { dishId -> meal.dishes[dishId] }

        if (dish == null) {
            // AF-05: dish_id 不存在或缺失时，尝试 dish_name + meal_id 唯一补挂
            val dishName = line.dish_name
            if (dishName != null) {
                val candidates = meal.dishes.values.filter { it.name == dishName }
                if (candidates.size == 1) {
                    dish = candidates.single()
                    seg.meals[mealId] = meal.copy(
                        dishes = meal.dishes + (dish.dishId to dish.copy(
                            warnings = dish.warnings + "食材「$name」按 dish_name「$dishName」唯一补挂"
                        ))
                    )
                } else {
                    val detail = if (candidates.isEmpty()) "无匹配" else "${candidates.size} 个候选"
                    orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId,
                        line.dish_id,
                        "食材/调料「$name」的 dish_name「$dishName」$detail，无法确定归属"))
                    return
                }
            } else {
                // 区分：dish_id 有值但未命中 vs 完全缺失
                val reason = if (line.dish_id != null) {
                    "食材/调料「$name」的 dish_id「${line.dish_id}」不存在且无 dish_name，已丢弃"
                } else {
                    "食材/调料「$name」缺少 dish_id 且无 dish_name，已丢弃"
                }
                orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId,
                    line.dish_id, reason))
                return
            }
        }

        // AF-05: 按名去重——同名字段合并，不重复展示
        val actualMeal = seg.meals[mealId]!!
        val actualDish = actualMeal.dishes[dish.dishId]!!

        if (isSeasoning) {
            val existingIdx = actualDish.seasonings.indexOfFirst { it.name == name }
            val updatedSeasonings = if (existingIdx >= 0) {
                val old = actualDish.seasonings[existingIdx]
                actualDish.seasonings.toMutableList().apply {
                    set(existingIdx, old.copy(
                        quantity = line.quantity ?: old.quantity, unit = line.unit ?: old.unit,
                        nutrients = line.nutrients ?: old.nutrients,
                    ))
                }
            } else {
                actualDish.seasonings + DraftSeasoning(name = name, quantity = line.quantity, unit = line.unit, nutrients = line.nutrients)
            }
            seg.meals[mealId] = actualMeal.copy(dishes = actualMeal.dishes + (dish.dishId to actualDish.copy(seasonings = updatedSeasonings)))
        } else {
            val existingIdx = actualDish.ingredients.indexOfFirst { it.name == name }
            val updatedIngredients = if (existingIdx >= 0) {
                val old = actualDish.ingredients[existingIdx]
                actualDish.ingredients.toMutableList().apply {
                    set(existingIdx, old.copy(
                        role = line.role ?: old.role, foodGroup = line.food_group ?: old.foodGroup,
                        quantity = line.quantity ?: old.quantity, unit = line.unit ?: old.unit,
                        nutrients = line.nutrients ?: old.nutrients, isMain = line.is_main ?: old.isMain,
                    ))
                }
            } else {
                actualDish.ingredients + DraftIngredient(name = name, role = line.role, foodGroup = line.food_group,
                    quantity = line.quantity, unit = line.unit, nutrients = line.nutrients, isMain = line.is_main)
            }
            seg.meals[mealId] = actualMeal.copy(dishes = actualMeal.dishes + (dish.dishId to actualDish.copy(ingredients = updatedIngredients)))
        }
    }

    // ═══════════════════════════════ CookingStep ═══════════════════════════════

    private fun handleCookingStepEvent(line: NdjsonLine) {
        val mealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null, "cooking_step 缺少 meal_id"))
            return
        }
        val dishId = line.dish_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null, "cooking_step 缺少 dish_id"))
            return
        }
        val text = line.text ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId, "cooking_step 缺少 text"))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }
        val meal = seg.meals[mealId] ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "cooking_step 的 meal_id「$mealId」不存在"))
            return
        }
        val dish = meal.dishes[dishId] ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "cooking_step 的 dish_id「$dishId」不存在"))
            return
        }

        // AF-05: 按 order 去重合并
        val existingIdx = dish.cookingSteps.indexOfFirst { it.text == text }
        val updatedSteps = if (existingIdx >= 0) {
            dish.cookingSteps.toMutableList().apply {
                line.order?.let { set(existingIdx, get(existingIdx).copy(order = it)) }
            }
        } else {
            dish.cookingSteps + DraftCookingStep(text = text, order = line.order)
        }
        seg.meals[mealId] = meal.copy(dishes = meal.dishes + (dishId to dish.copy(cookingSteps = updatedSteps)))
    }

    // ═══════════════════════════════ Warning / Advice / Done ═══════════════════════════════

    private fun handleWarningEvent(line: NdjsonLine) {
        val message = line.message ?: return
        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }

        if (line.dish_id != null && line.meal_id != null) {
            val meal = seg.meals[line.meal_id]
            if (meal != null) {
                val dish = meal.dishes[line.dish_id]
                if (dish != null) {
                    seg.meals[line.meal_id] = meal.copy(
                        dishes = meal.dishes + (line.dish_id to dish.copy(warnings = dish.warnings + message))
                    )
                    return
                }
            }
        }
        if (line.meal_id != null) {
            val meal = seg.meals[line.meal_id]
            if (meal != null) {
                seg.meals[line.meal_id] = meal.copy(warnings = meal.warnings + message)
                return
            }
        }
        seg.warnings.add(message)
    }

    private fun handleAdviceEvent(line: NdjsonLine) {
        val message = line.message ?: return
        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }

        if (line.meal_id != null) {
            val meal = seg.meals[line.meal_id]
            if (meal != null) {
                seg.meals[line.meal_id] = meal.copy(advices = meal.advices + message)
                return
            }
        }
        seg.warnings.add("💬 $message")
    }

    private fun handleDoneEvent(line: NdjsonLine) {
        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }
        seg.done = true
    }

    // ═══════════════════════════════ 整体 JSON 回退（AF-04 重写） ═══════════════════════════════

    /**
     * AF-04: 整体 JSON fallback 必须先规范化为 NDJSON 等价事件，复用同一归属校验链。[AI修改]
     *
     * 1. 解析 FlatMealJson/MultiDayJson → 生成 NdjsonLine 事件列表
     * 2. 每条事件分配本批次已知 segment_id（按日期映射到 InputSegment.targetDate）
     * 3. 统一生成 meal_id/dish_id 父子键
     * 4. 逐条 feed 进 processLine()，复用同一归属校验
     */
    private fun tryWholeJsonFallback() {
        val raw = allRawText.toString().trim()
        if (raw.isEmpty()) {
            jsonFallbackErrors = listOf("AI 返回为空")
            return
        }

        val syntheticLines = buildSyntheticNdjsonLines(raw)
        if (syntheticLines.isEmpty()) {
            jsonFallbackErrors = listOf("AI 返回不符合任何已知格式（NDJSON/FlatMealJson/MultiDayJson）")
            return
        }

        orphanDiagnostics.add(
            StreamDiagnostic(DiagnosticLevel.WARNING, null, null, null,
                "未检测到 NDJSON 事件，已按整体 JSON 格式规范化后重新校验")
        )

        // 将合成事件逐行喂入同一 processLine 管道
        for (syntheticLine in syntheticLines) {
            processLine(syntheticLine)
        }
    }

    /** AF-04: 从整体 JSON 构建合成 NdjsonLine 列表，逐一喂入归属校验链。[AI修改] */
    private fun buildSyntheticNdjsonLines(raw: String): List<String> {
        // 尝试 FlatMealJson
        val flatResult = runCatching { json.decodeFromString<FlatMealJson>(raw) }.getOrNull()
        if (flatResult != null && flatResult.items.isNotEmpty()) {
            return buildLinesFromFlatMeal(flatResult)
        }

        // 尝试 MultiDayJson
        val multiResult = runCatching { json.decodeFromString<MultiDayJson>(raw) }.getOrNull()
        if (multiResult != null && multiResult.days.isNotEmpty()) {
            return buildLinesFromMultiDay(multiResult)
        }

        return emptyList()
    }

    /** FlatMealJson item → 映射到本次已发送 segment 的 NDJSON 行。[AI修改] */
    private fun buildLinesFromFlatMeal(flat: FlatMealJson): List<String> {
        val lines = mutableListOf<String>()
        for (item in flat.items) {
            val resolvedDate = resolveDateFromItem(item)
            val segId = findSegmentForDate(resolvedDate) ?: segments.firstOrNull()?.segmentId ?: return emptyList()
            val slot = item.meal_type ?: "lunch"
            val dateStr = resolvedDate ?: fallbackDate.toString()
            val mealId = "$dateStr|$slot"
            val dishId = "$mealId|d1"

            lines.add("""{"type":"meal","segment_id":"$segId","meal_id":"$mealId","date":"$dateStr","slot":"$slot"}""")
            val dishLine = buildString {
                append("""{"type":"dish","segment_id":"$segId","meal_id":"$mealId","dish_id":"$dishId","name":"${escapeJson(item.dish_name)}"""")
                if (item.dish_cooking_methods.isNotEmpty()) append(""","cooking_method":"${item.dish_cooking_methods.first()}"""")
                if (item.dish_quantity != 1.0) append(""","quantity":${item.dish_quantity}""")
                append("}")
            }
            lines.add(dishLine)

            for (ing in item.ingredients) {
                val ingLine = buildString {
                    append("""{"type":"ingredient","segment_id":"$segId","meal_id":"$mealId","dish_id":"$dishId","name":"${escapeJson(ing.name)}"""")
                    if (ing.food_group != null) append(""","food_group":"${ing.food_group}"""")
                    append(""","quantity":${ing.quantity}""")
                    append("}")
                }
                lines.add(ingLine)
            }
        }
        return lines
    }

    /** MultiDayJson → 映射到本次已发送 segment 的 NDJSON 行。[AI修改] */
    private fun buildLinesFromMultiDay(multi: MultiDayJson): List<String> {
        val lines = mutableListOf<String>()
        for ((dayIdx, day) in multi.days.withIndex()) {
            val dateStr = day.date ?: fallbackDate.toString()
            val segId = findSegmentForDate(dateStr) ?: segments.getOrNull(dayIdx)?.segmentId ?: return emptyList()
            for ((mealIdx, meal) in day.meals.withIndex()) {
                val slot = meal.meal_type ?: "lunch"
                val mealId = "$dateStr|$slot"
                lines.add("""{"type":"meal","segment_id":"$segId","meal_id":"$mealId","date":"$dateStr","slot":"$slot"}""")
                for ((dishIdx, dishRef) in meal.dishes.withIndex()) {
                    val dishName = dishRef.name.ifBlank { dishRef.dish?.name ?: "未命名" }
                    val dishId = "$mealId|d${dishIdx + 1}"
                    lines.add("""{"type":"dish","segment_id":"$segId","meal_id":"$mealId","dish_id":"$dishId","name":"${escapeJson(dishName)}"}""")
                    for (ing in dishRef.dish?.ingredients ?: emptyList()) {
                        val ingName = ing.ref ?: ing.food?.name ?: ""
                        lines.add("""{"type":"ingredient","segment_id":"$segId","meal_id":"$mealId","dish_id":"$dishId","name":"${escapeJson(ingName)}","quantity":${ing.quantity}}""")
                    }
                }
            }
        }
        return lines
    }

    private fun resolveDateFromItem(item: FlatMealItem): String? {
        if (!item.date.isNullOrBlank()) return item.date
        if (item.date_offset != 0) {
            return runCatching {
                fallbackDate.plus(DatePeriod(days = item.date_offset)).toString()
            }.getOrNull()
        }
        return fallbackDate.toString()
    }

    private fun findSegmentForDate(dateStr: String?): String? {
        if (dateStr == null) return null
        val date = runCatching { LocalDate.parse(dateStr.replace('/', '-')) }.getOrNull() ?: return null
        return segments.find { it.targetDate == date }?.segmentId
            ?: segments.firstOrNull()?.segmentId
    }

    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    // ═══════════════════════════════ 辅助方法 ═══════════════════════════════

    private fun mergeNote(existing: String?, incoming: String?): String? {
        if (incoming.isNullOrBlank()) return existing
        if (existing.isNullOrBlank()) return incoming
        return "$existing; $incoming"
    }

    private fun buildDraft(): MealStreamDraft {
        if (jsonFallbackDays != null && segmentMap.isEmpty()) {
            // 旧回退路径（AF-04 未触及时的兜底）
            return buildDraftFromJsonFallback()
        }

        return MealStreamDraft(
            segments = segmentMap.mapValues { (_, mut) ->
                SegmentDraft(
                    segmentId = mut.segmentId,
                    meals = mut.meals.mapValues { (_, node) -> node },
                    warnings = mut.warnings.toList(),
                    done = mut.done,
                )
            },
            diagnostics = orphanDiagnostics.toList(),
            finishReason = finishReason,
            isTruncated = isLengthTruncated,
        )
    }

    /** 旧 JSON 回退兜底（已被 AF-04 替代，仅保留兼容）。 */
    private fun buildDraftFromJsonFallback(): MealStreamDraft {
        val days = jsonFallbackDays ?: return MealStreamDraft(
            diagnostics = listOf(StreamDiagnostic(DiagnosticLevel.ERROR, null, null, null,
                jsonFallbackErrors?.firstOrNull() ?: "解析失败")),
            finishReason = finishReason, isTruncated = isLengthTruncated,
        )
        val fallbackSegments = days.mapIndexed { idx, day ->
            val segId = "fallback-day$idx"
            val meals = day.meals.associate { meal ->
                val mealId = "${day.date ?: "unknown"}|${meal.meal_type ?: "unknown"}"
                mealId to MealDraftNode(
                    mealId = mealId, date = day.date ?: "", slot = meal.meal_type ?: "unknown",
                    time = meal.meal_time, note = meal.note,
                    dishes = meal.dishes.mapIndexed { di, dishRef ->
                        val dishId = "$mealId|d${di + 1}"
                        dishId to DishDraftNode(
                            dishId = dishId,
                            name = dishRef.name.ifBlank { dishRef.dish?.name ?: "未命名" },
                            quantity = dishRef.quantity, unit = dishRef.quantity_unit,
                            eatenRatio = dishRef.eaten_ratio, note = dishRef.note,
                            ingredients = dishRef.dish?.ingredients?.map { diJson ->
                                DraftIngredient(name = diJson.ref ?: diJson.food?.name ?: "",
                                    quantity = diJson.quantity, unit = diJson.unit, isMain = diJson.is_main)
                            } ?: emptyList(),
                        )
                    }.toMap(),
                )
            }
            segId to SegmentDraft(segmentId = segId, meals = meals)
        }.toMap()
        return MealStreamDraft(
            segments = fallbackSegments, diagnostics = orphanDiagnostics.toList(),
            finishReason = finishReason, isTruncated = isLengthTruncated,
        )
    }

    companion object {
        private val VALID_SLOTS = setOf("breakfast", "lunch", "dinner", "snack")
    }
}

/** 可变 SegmentDraft。[AI生成] */
internal class MutableSegmentDraft(
    val segmentId: String,
    val meals: MutableMap<String, MealDraftNode> = linkedMapOf(),
    val warnings: MutableList<String> = mutableListOf(),
    var done: Boolean = false,
)
