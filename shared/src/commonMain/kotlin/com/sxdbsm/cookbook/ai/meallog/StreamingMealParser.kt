package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate
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
 * 线程安全：单线程使用（ViewModel scope 内调用），不保证多线程安全。
 * <p>
 * [AI生成] B1 周期记+NDJSON流式改造：协议解析层。
 **/
class StreamingMealParser(
    /** 本次请求的已知分段列表（用于 segment_id 校验） */
    private val segments: List<InputSegment>,
    /** 生成标识（用于日志追踪） */
    private val generationId: String,
    /** 日期锚定使用的基准日期（用户选择的添加页日期/周期周一） */
    private val fallbackDate: LocalDate,
) {
    // ═══════════════════════════════════════
    // 内部状态
    // ═══════════════════════════════════════

    private val lineBuffer = StringBuilder()
    private val allRawText = StringBuilder()

    /** segment_id → SegmentDraft（可变，逐步构建） */
    private val segmentMap = linkedMapOf<String, MutableSegmentDraft>()

    /** 已知 segment_id 集合（快速校验） */
    private val knownSegmentIds: Set<String> = segments.map { it.segmentId }.toSet()

    /** 未归属事件（孤儿） */
    private val orphanDiagnostics = mutableListOf<StreamDiagnostic>()

    /** 是否有任何合法的 NDJSON 事件被成功解析 */
    private var hasAnyNdjsonEvent = false

    /** 完成原因 */
    private var finishReason: String? = null
    private var isLengthTruncated = false
    private var totalReceivedChars = 0

    // 用于整体 JSON 回退
    private var jsonFallbackDays: List<DayMealJson>? = null
    private var jsonFallbackErrors: List<String>? = null

    // ═══════════════════════════════════════
    // JSON 解析器（复用 kotlinx.serialization）
    // ═══════════════════════════════════════

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // ═══════════════════════════════════════
    // 公开 API
    // ═══════════════════════════════════════

    /** 当前草稿快照（不可变）。[AI生成] */
    val currentDraft: MealStreamDraft
        get() = buildDraft()

    /**
     * 喂入一段 Delta 文本（来自 SSE 流事件）。[AI生成]
     *
     * 将文本追加到行缓冲，以 \n 切分出完整行 → 逐行解析 → 归属路由。
     * SSE 分片可能在行中间断开：不完整行留在缓冲中，等后续 Delta 补齐。
     */
    fun feedDelta(text: String) {
        if (text.isEmpty()) return
        totalReceivedChars += text.length
        allRawText.append(text)
        lineBuffer.append(text)

        // 以 \n 切分完整行；最后一段（无 \n 结尾）留在缓冲
        val content = lineBuffer.toString()
        val lastNewline = content.lastIndexOf('\n')
        if (lastNewline < 0) return // 尚无完整行

        val complete = content.substring(0, lastNewline + 1)
        val remaining = content.substring(lastNewline + 1)
        lineBuffer.clear()
        lineBuffer.append(remaining)

        // 逐行处理
        complete.lines().forEach { line ->
            val trimmed = line.trim('\r', '\n', ' ').takeIf { it.isNotEmpty() } ?: return@forEach
            processLine(trimmed)
        }
    }

    /**
     * 网络流完成时调用。[AI生成]
     *
     * ① 处理缓冲中剩余的不完整行（记录诊断、不解析）
     * ② 若无任何 NDJSON 事件 → 尝试整体 JSON 回退
     * ③ 返回最终 MealStreamDraft
     */
    fun finish(reason: String): MealStreamDraft {
        finishReason = reason
        isLengthTruncated = reason == "length"

        // 处理缓冲尾部
        val tail = lineBuffer.toString().trim()
        if (tail.isNotEmpty()) {
            orphanDiagnostics.add(
                StreamDiagnostic(
                    level = DiagnosticLevel.WARNING,
                    segmentId = null, mealId = null, dishId = null,
                    message = "响应结束时仍有 ${tail.length} 字符的未完成内容，已丢弃",
                )
            )
            lineBuffer.clear()
        }

        // 如果没有任何 NDJSON 事件 → 整体 JSON 回退
        if (!hasAnyNdjsonEvent) {
            tryWholeJsonFallback()
        }

        // 截断提示
        if (isLengthTruncated) {
            orphanDiagnostics.add(
                StreamDiagnostic(
                    level = DiagnosticLevel.WARNING,
                    segmentId = null, mealId = null, dishId = null,
                    message = "模型输出被截断（finish_reason=length），已保留截断前完整内容",
                )
            )
        }

        return buildDraft()
    }

    // ═══════════════════════════════════════
    // 行解析 + 事件路由
    // ═══════════════════════════════════════

    /** 解析一行 NDJSON → 路由到对应处理方法。[AI生成] */
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

        // 校验 segment_id
        if (parsed.segment_id.isEmpty()) {
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.ERROR, null, null, null,
                    "NDJSON 行缺少 segment_id，已丢弃: ${line.take(80)}")
            )
            return
        }

        if (parsed.segment_id !in knownSegmentIds) {
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.WARNING, parsed.segment_id, null, null,
                    "segment_id「${parsed.segment_id}」不匹配本次请求的任何分段")
            )
            // 仍继续处理（可能是整体 JSON 回退产生的）
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

    // ═══════════════════════════════════════
    // 各事件类型处理
    // ═══════════════════════════════════════

    private fun handleMealEvent(line: NdjsonLine) {
        val mealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null,
                "meal 事件缺少 meal_id，已丢弃"))
            return
        }
        val date = line.date ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null,
                "meal 事件缺少 date，已丢弃"))
            return
        }
        val slot = line.slot ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null,
                "meal 事件缺少 slot，已丢弃"))
            return
        }

        // 校验 date 格式
        val validDate = runCatching { LocalDate.parse(date.replace('/', '-')) }.getOrNull()
        if (validDate == null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null,
                "meal 日期「$date」无效"))
            return
        }

        // 校验 slot
        if (slot !in VALID_SLOTS) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.WARNING, line.segment_id, mealId, null,
                "餐次类型「$slot」非标准值"))
        }

        // 校验 meal_id 格式 = date|slot
        val expectedMealId = "$date|$slot"
        if (mealId != expectedMealId) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null,
                "meal_id「$mealId」与 date|slot「$expectedMealId」不一致，已拒绝"))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = getOrCreateSegment(line.segment_id)
        seg.meals.getOrPut(mealId) {
            MealDraftNode(mealId = mealId, date = date, slot = slot, time = line.time, note = line.note)
        }.let { existing ->
            // 同键重复：合并（以最新非空字段覆盖）
            if (line.time != null) seg.meals[mealId] = existing.copy(time = line.time)
            if (line.note != null && line.note != existing.note) {
                seg.meals[mealId] = seg.meals[mealId]!!.copy(
                    note = if (existing.note.isNullOrBlank()) line.note else "${existing.note}; ${line.note}"
                )
            }
        }
    }

    private fun handleDishEvent(line: NdjsonLine) {
        val mealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null,
                "dish 事件缺少 meal_id，已丢弃"))
            return
        }
        val dishId = line.dish_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null,
                "dish 事件缺少 dish_id，已丢弃"))
            return
        }
        val name = line.name ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "dish 事件缺少 name，已丢弃"))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = getOrCreateSegment(line.segment_id)

        // 查找父餐次；若不存在，尝试从 dish 事件的 date+slot 推断
        if (mealId !in seg.meals) {
            // 尝试同 segment 下按 date+slot 匹配已有餐次
            val inferredMeal = seg.meals.values.find { m ->
                m.date == (line.date ?: "") && m.slot == (line.slot ?: "")
            }
            if (inferredMeal != null) {
                seg.meals[inferredMeal.mealId] = inferredMeal.copy(
                    dishes = inferredMeal.dishes + (
                        dishId to DishDraftNode(
                            dishId = dishId, name = name,
                            cookingMethod = line.cooking_method,
                            quantity = line.quantity, unit = line.unit,
                            eatenRatio = line.eaten_ratio, note = line.note,
                        )
                    )
                )
                seg.meals[inferredMeal.mealId] = seg.meals[inferredMeal.mealId]!!.copy(
                    warnings = seg.meals[inferredMeal.mealId]!!.warnings +
                        "菜品「$name」的 meal_id「$mealId」未找到父餐次，已按 date+slot 匹配到「${inferredMeal.mealId}」"
                )
                return
            }

            // 尝试从 date+slot 新建父餐次
            val date = line.date
            val slot = line.slot
            if (date != null && slot != null) {
                val newMealId = "$date|$slot"
                val validDate = runCatching { LocalDate.parse(date.replace('/', '-')) }.getOrNull()
                if (validDate != null && slot in VALID_SLOTS) {
                    seg.meals[newMealId] = MealDraftNode(
                        mealId = newMealId, date = date, slot = slot,
                        warnings = listOf("菜品「$name」补建了父餐次「$newMealId」")
                    )
                    seg.meals[newMealId] = seg.meals[newMealId]!!.copy(
                        dishes = seg.meals[newMealId]!!.dishes + (
                            dishId to DishDraftNode(
                                dishId = dishId, name = name,
                                cookingMethod = line.cooking_method,
                                quantity = line.quantity, unit = line.unit,
                                eatenRatio = line.eaten_ratio, note = line.note,
                            )
                        )
                    )
                    return
                }
            }

            // 无法补建 → 孤儿
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "菜品「$name」的 meal_id「$mealId」未找到父餐次且无法补建，进入待确认"))
            return
        }

        // 检查 dish_id 是否已存在于其他 meal_id 下（冲突检测）
        val conflictingMeal = seg.meals.values.find { m -> m.mealId != mealId && dishId in m.dishes }
        if (conflictingMeal != null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "dish_id「$dishId」已存在于 meal「${conflictingMeal.mealId}」，拒绝跨餐冲突"))
            return
        }

        // 正常添加
        seg.meals[mealId] = seg.meals[mealId]!!.copy(
            dishes = seg.meals[mealId]!!.dishes + (
                dishId to DishDraftNode(
                    dishId = dishId, name = name,
                    cookingMethod = line.cooking_method,
                    quantity = line.quantity, unit = line.unit,
                    eatenRatio = line.eaten_ratio, note = line.note,
                )
            )
        )
    }

    private fun handleIngredientEvent(line: NdjsonLine) {
        handleDishChildEvent(line, isSeasoning = false)
    }

    private fun handleSeasoningEvent(line: NdjsonLine) {
        handleDishChildEvent(line, isSeasoning = true)
    }

    /** dish 子项（ingredient/seasoning/cooking_step）的通用归属查找。[AI生成] */
    private fun handleDishChildEvent(line: NdjsonLine, isSeasoning: Boolean) {
        val mealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null,
                "缺少 meal_id，已丢弃"))
            return
        }
        val dishId = line.dish_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null,
                "缺少 dish_id，已丢弃"))
            return
        }
        val name = line.name ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "食材/调料缺少 name，已丢弃"))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = getOrCreateSegment(line.segment_id)
        val meal = seg.meals[mealId]
        if (meal == null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "食材/调料「$name」的 meal_id「$mealId」不存在"))
            return
        }
        val dish = meal.dishes[dishId]
        if (dish == null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "食材/调料「$name」的 dish_id「$dishId」不存在于餐次「$mealId」"))
            return
        }

        if (isSeasoning) {
            val updatedDish = dish.copy(seasonings = dish.seasonings +
                DraftSeasoning(name = name, quantity = line.quantity, unit = line.unit, nutrients = line.nutrients))
            seg.meals[mealId] = meal.copy(dishes = meal.dishes + (dishId to updatedDish))
        } else {
            val updatedDish = dish.copy(ingredients = dish.ingredients +
                DraftIngredient(
                    name = name, role = line.role, foodGroup = line.food_group,
                    quantity = line.quantity, unit = line.unit, nutrients = line.nutrients, isMain = line.is_main
                ))
            seg.meals[mealId] = meal.copy(dishes = meal.dishes + (dishId to updatedDish))
        }
    }

    private fun handleCookingStepEvent(line: NdjsonLine) {
        val mealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null,
                "cooking_step 缺少 meal_id，已丢弃"))
            return
        }
        val dishId = line.dish_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, null,
                "cooking_step 缺少 dish_id，已丢弃"))
            return
        }
        val text = line.text ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "cooking_step 缺少 text，已丢弃"))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = getOrCreateSegment(line.segment_id)
        val meal = seg.meals[mealId] ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "cooking_step 的 meal_id「$mealId」不存在"))
            return
        }
        val dish = meal.dishes[dishId] ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "cooking_step 的 dish_id「$dishId」不存在于餐次「$mealId」"))
            return
        }

        val step = DraftCookingStep(text = text, order = line.order)
        val updatedDish = dish.copy(cookingSteps = dish.cookingSteps + step)
        seg.meals[mealId] = meal.copy(dishes = meal.dishes + (dishId to updatedDish))
    }

    private fun handleWarningEvent(line: NdjsonLine) {
        val message = line.message ?: return
        hasAnyNdjsonEvent = true
        val seg = getOrCreateSegment(line.segment_id)

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

        // 段级 warning
        seg.warnings.add(message)
    }

    private fun handleAdviceEvent(line: NdjsonLine) {
        val message = line.message ?: return
        hasAnyNdjsonEvent = true
        val seg = getOrCreateSegment(line.segment_id)

        if (line.meal_id != null) {
            val meal = seg.meals[line.meal_id]
            if (meal != null) {
                seg.meals[line.meal_id] = meal.copy(advices = meal.advices + message)
                return
            }
        }

        // 段级建议
        seg.warnings.add("💬 $message") // advice 暂存为 warning 级别展示
    }

    private fun handleDoneEvent(line: NdjsonLine) {
        hasAnyNdjsonEvent = true
        val seg = getOrCreateSegment(line.segment_id)
        seg.done = true
    }

    // ═══════════════════════════════════════
    // 整体 JSON 回退
    // ═══════════════════════════════════════

    /** 无 NDJSON 事件时，尝试把累计原文当整体 JSON 解析。[AI生成] */
    private fun tryWholeJsonFallback() {
        val raw = allRawText.toString().trim()
        if (raw.isEmpty()) {
            jsonFallbackErrors = listOf("AI 返回为空")
            return
        }

        // 先尝试 FlatMealJson（当前 prompt 格式）
        val flatResult = runCatching {
            json.decodeFromString<FlatMealJson>(raw)
        }.getOrNull()

        if (flatResult != null && flatResult.items.isNotEmpty()) {
            val converted = FlatToDayMealConverter.convert(flatResult, fallbackDate)
            jsonFallbackDays = converted.days
            jsonFallbackErrors = converted.errors.ifEmpty { null }
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.WARNING, null, null, null,
                    "未检测到 NDJSON 事件，已按整体 JSON 格式解析；请确认结果完整性")
            )
            hasAnyNdjsonEvent = true
            return
        }

        // 回退：尝试 MultiDayJson 格式
        val multiResult = runCatching {
            json.decodeFromString<MultiDayJson>(raw)
        }.getOrNull()

        if (multiResult != null && multiResult.days.isNotEmpty()) {
            jsonFallbackDays = multiResult.days
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.WARNING, null, null, null,
                    "已按 MultiDayJson 格式解析；请确认结果完整性")
            )
            hasAnyNdjsonEvent = true
            return
        }

        jsonFallbackErrors = listOf("AI 返回不符合任何已知格式（NDJSON/FlatMealJson/MultiDayJson）")
    }

    // ═══════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════

    /** 获取或创建 segment 的可变草稿。[AI生成] */
    private fun getOrCreateSegment(segmentId: String): MutableSegmentDraft {
        return segmentMap.getOrPut(segmentId) { MutableSegmentDraft(segmentId = segmentId) }
    }

    /** 从当前可变状态构建不可变快照。[AI生成] */
    private fun buildDraft(): MealStreamDraft {
        // 若有 JSON 回退结果，从中构建
        if (jsonFallbackDays != null && segmentMap.isEmpty()) {
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

    /** 从整体 JSON 回退结果构建草稿。[AI生成] */
    private fun buildDraftFromJsonFallback(): MealStreamDraft {
        val days = jsonFallbackDays ?: return MealStreamDraft(
            diagnostics = listOf(StreamDiagnostic(DiagnosticLevel.ERROR, null, null, null,
                jsonFallbackErrors?.firstOrNull() ?: "解析失败")),
            finishReason = finishReason,
            isTruncated = isLengthTruncated,
        )

        // 将 DayMealJson 列表转换为 SegmentDraft
        val fallbackSegments = days.mapIndexed { idx, day ->
            val segId = "fallback-day$idx"
            val meals = day.meals.associate { meal ->
                val mealId = "${day.date ?: "unknown"}|${meal.meal_type ?: "unknown"}"
                mealId to MealDraftNode(
                    mealId = mealId,
                    date = day.date ?: "",
                    slot = meal.meal_type ?: "unknown",
                    time = meal.meal_time,
                    note = meal.note,
                    dishes = meal.dishes.mapIndexed { di, dishRef ->
                        val dishId = "$mealId|d${di + 1}"
                        dishId to DishDraftNode(
                            dishId = dishId,
                            name = dishRef.name.ifBlank { dishRef.dish?.name ?: "未命名" },
                            quantity = dishRef.quantity,
                            unit = dishRef.quantity_unit,
                            eatenRatio = dishRef.eaten_ratio,
                            note = dishRef.note,
                            ingredients = dishRef.dish?.ingredients?.map { diJson ->
                                DraftIngredient(
                                    name = diJson.ref ?: diJson.food?.name ?: "",
                                    quantity = diJson.quantity,
                                    unit = diJson.unit,
                                    isMain = diJson.is_main,
                                )
                            } ?: emptyList(),
                        )
                    }.toMap(),
                )
            }
            segId to SegmentDraft(segmentId = segId, meals = meals)
        }.toMap()

        return MealStreamDraft(
            segments = fallbackSegments,
            diagnostics = orphanDiagnostics.toList(),
            finishReason = finishReason,
            isTruncated = isLengthTruncated,
        )
    }

    companion object {
        private val VALID_SLOTS = setOf("breakfast", "lunch", "dinner", "snack")
    }
}

// ═══════════════════════════════════════
// 内部可变草稿（解析过程中使用）
// ═══════════════════════════════════════

/** 可变 SegmentDraft。[AI生成] */
internal class MutableSegmentDraft(
    val segmentId: String,
    val meals: MutableMap<String, MealDraftNode> = linkedMapOf(),
    val warnings: MutableList<String> = mutableListOf(),
    var done: Boolean = false,
)
