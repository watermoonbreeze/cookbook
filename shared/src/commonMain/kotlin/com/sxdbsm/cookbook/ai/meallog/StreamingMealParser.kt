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
 * [AI修改 2026-08-18 10-b/10-c] 在不削弱 AF-03「非法归属不可进入预览」目标的前提下收窄误伤：
 * - meal_id 自愈（10-b）：一条 meal 事件里 date/slot 各自已校验合法时，date|slot 是权威信号、
 *   meal_id 只是 AI 的复述——复述串不一致按 date|slot 归一(WARNING)而非整条拒绝(ERROR)；
 *   同一 AI meal_id 先后指向不同 date|slot 时仍按"后到冲突拒绝"处理，不放松。
 *   dish 补建父节点分支（唯一信号来源，无交叉校验）维持原有严格拒绝，不做自愈。
 * - dish_id 本地序号（10-c）：AI 若把同一 dish_id 复用给两道不同名的菜，此前会静默用第二道菜
 *   覆盖第一道菜（AF-05 同键合并对"同一道菜的重复描述"和"两道菜撞了同一个 dish_id"未加区分）。
 *   现按 (meal_id, 原始dish_id, name) 判定：同名→仍走 AF-05 合并；不同名→视为新菜，本地分配新序号
 *   并记 WARNING，不覆盖原有菜。dish_id 格式校验仍要求 AI 给出合法前缀（证明其理解归属关系），
 *   但序号部分不再是查找主键，只作首次出现的自证。
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

    // [AI修改 10-b] AI 声明的 "segmentId|原始meal_id" → 归一后的 canonical "date|slot"。
    private val mealIdAlias = mutableMapOf<String, String>()

    // [AI修改 10-c] "segmentId|mealId|原始dish_id|name" → 本地分配的 dishId（同名精确复用，AF-05 合并前提）。
    private val dishLocalKeyByName = mutableMapOf<String, String>()
    // "segmentId|mealId|原始dish_id" → 最近一次解析出的本地 dishId（供无 name 信息的子事件按"最近一次"挂靠）。
    private val dishLastLocalByRawId = mutableMapOf<String, String>()
    // "segmentId|mealId" → 下一个本地序号（不依赖 AI 自己数对，只用于生成本地 key）。
    private val dishSeqByMeal = mutableMapOf<String, Int>()

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
                    "响应结束时仍有 ${tail.length} 字符的未完成内容，已丢弃", code = DiagnosticCode.TAIL_INCOMPLETE)
            )
            lineBuffer.clear()
        }

        if (!hasAnyNdjsonEvent) {
            tryWholeJsonFallback()
        }

        if (isLengthTruncated) {
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.WARNING, null, null, null,
                    "模型输出被截断（finish_reason=length），已保留截断前完整内容", code = DiagnosticCode.TRUNCATED)
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
                    "无法解析 NDJSON 行: ${e.message?.take(120)}", code = DiagnosticCode.PARSE_ERROR)
            )
            return
        }

        // AF-03: segment_id 必须精确匹配已知分段，否则拒绝
        if (parsed.segment_id.isEmpty()) {
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.ERROR, null, null, null,
                    "NDJSON 行缺少 segment_id，已丢弃: ${line.take(80)}", code = DiagnosticCode.SEGMENT_MISMATCH)
            )
            return
        }

        if (parsed.segment_id !in knownSegmentIds) {
            orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.ERROR, parsed.segment_id, null, null,
                    "segment_id「${parsed.segment_id}」不匹配本次请求的任何分段，已拒绝", code = DiagnosticCode.SEGMENT_MISMATCH)
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
            "done" -> {} // AF-ARCH-01: done 是段结束标记，静默消费，不产生诊断
            else -> orphanDiagnostics.add(
                StreamDiagnostic(DiagnosticLevel.WARNING, parsed.segment_id, null, null,
                    "未知事件类型「${parsed.type}」，已忽略", code = DiagnosticCode.UNKNOWN_TYPE)
            )
        }
    }

    // ═══════════════════════════════ Meal ═══════════════════════════════

    private fun handleMealEvent(line: NdjsonLine) {
        val rawMealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null, "meal 事件缺少 meal_id"))
            return
        }
        val date = line.date ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, null, "meal 事件缺少 date"))
            return
        }
        val slot = line.slot ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, null, "meal 事件缺少 slot"))
            return
        }

        // date 格式校验
        val validDate = runCatching { LocalDate.parse(date.replace('/', '-')) }.getOrNull()
        if (validDate == null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, null,
                "meal 日期「$date」无效", code = DiagnosticCode.INVALID_DATE))
            return
        }

        // AF-03: slot 非法则拒绝（不创建）
        if (slot !in VALID_SLOTS) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, null,
                "餐次类型「$slot」无效，合法值: ${VALID_SLOTS.joinToString()}，已拒绝", code = DiagnosticCode.INVALID_SLOT))
            return
        }

        // [AI修改 10-b] meal_id 自愈：date/slot 都已各自校验合法，date|slot 是权威信号，
        //   meal_id 只是 AI 的复述——复述串对不上按 date|slot 归一(WARNING)，不整条拒绝。
        //   同一 AI meal_id 先后指向不同 date|slot 时仍按 AF-03 原则拒绝后到者。
        val expectedMealId = "$date|$slot"
        val aliasKey = "${line.segment_id}|$rawMealId"
        if (rawMealId != expectedMealId) {
            val prior = mealIdAlias[aliasKey]
            if (prior != null && prior != expectedMealId) {
                orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, null,
                    "meal_id「$rawMealId」先后指向不同餐次「$prior」与「$expectedMealId」，已拒绝后到",
                    code = DiagnosticCode.MEAL_ID_MISMATCH))
                return
            }
            mealIdAlias[aliasKey] = expectedMealId
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.WARNING, line.segment_id, expectedMealId, null,
                "meal_id「$rawMealId」已按 date|slot 归一为「$expectedMealId」", code = DiagnosticCode.MEAL_ID_NORMALIZED))
        }
        val mealId = expectedMealId

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

    // AF-08: d1 及以上（正整数），d0/d00 拒绝
    private val DISH_ID_PATTERN = Regex("""^(.+)\|d([1-9]\d*)$""")

    private fun handleDishEvent(line: NdjsonLine) {
        val rawMealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null, "dish 缺少 meal_id"))
            return
        }
        val rawDishId = line.dish_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, null, "dish 缺少 dish_id"))
            return
        }
        val name = line.name ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, rawDishId, "dish 缺少 name"))
            return
        }

        // AF-03: 校验 dish_id 格式 = {meal_id}|d{正整数}——对齐 AI 自己在本事件里给出的 meal_id（自证一致性），
        //   与 10-b 的 date|slot 归一是两回事：这里只证明 AI 理解"这道菜属于它自称的那个餐次"。
        val dishIdMatch = DISH_ID_PATTERN.matchEntire(rawDishId)
        if (dishIdMatch == null || dishIdMatch.groupValues[1] != rawMealId) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, rawDishId,
                "dish_id「$rawDishId」格式无效（期望 {meal_id}|d{正整数}），已拒绝", code = DiagnosticCode.DISH_ID_FORMAT))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }

        // [AI修改 10-b，Google质量复核🔴-1修复] 归一 rawMealId：真实存在的餐次必须优先于任何别名——
        //   否则"rawMealId 本身恰好也是另一个真实餐次的 key"时，陈旧别名会把它错误重定向，
        //   静默把整餐菜挂到错误餐次（比拒绝更糟）。查找父餐次改用归一后的 id；查不到时
        //  （含从未出现过 meal 事件）落回 rawMealId 走原有补建分支——补建分支只有这一个事件作为
        //   信号来源，没有独立信号交叉验证，维持严格、不自愈。
        val mealId = canonicalMealId(line.segment_id, rawMealId, seg.meals)

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
                    orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, rawDishId,
                        "dish meal_id「$mealId」与 date|slot「$newMealId」不一致，补建失败", code = DiagnosticCode.ORPHAN_DISH))
                    return
                }
            } else {
                orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, rawDishId,
                    "菜品「$name」的 meal_id「$mealId」未找到父餐次且无法补建，进入待确认", code = DiagnosticCode.ORPHAN_DISH))
                return
            }
        }

        // [AI修改 10-c] dish_id 本地化：同 (mealId, rawDishId, name) 精确复用同一本地 key（AF-05 合并前提）；
        //   同 rawDishId 换了 name → 视为 AI 复用了 dish_id 给另一道菜，本地分配新序号，不覆盖原有菜。
        // [Google质量复核🟡-3修复] name 判同用归一后的值(trim)：否则同一道菜第二次多打/少打一个空格，
        //   就会从"AF-05 合并"误判成"复用给了新菜"，这是本次改动新引入的敏感度，旧的按 id 合并逻辑没有这个问题。
        val nameKey = name.trim()
        val bucketKey = "${line.segment_id}|$mealId|$rawDishId"
        val exactKey = "$bucketKey|$nameKey"
        val dishId = dishLocalKeyByName[exactKey] ?: run {
            val reused = dishLastLocalByRawId.containsKey(bucketKey)
            val seqKey = "${line.segment_id}|$mealId"
            val seq = (dishSeqByMeal[seqKey] ?: 0) + 1
            dishSeqByMeal[seqKey] = seq
            val newLocalId = "$mealId|d$seq"
            dishLocalKeyByName[exactKey] = newLocalId
            if (reused) {
                orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.WARNING, line.segment_id, mealId, newLocalId,
                    "dish_id「$rawDishId」被复用于不同的菜「$name」，已按新菜处理，不覆盖原有菜", code = DiagnosticCode.DISH_ID_REUSED))
            }
            newLocalId
        }
        dishLastLocalByRawId[bucketKey] = dishId

        // 冲突检测：dish_id 已存在于其他 meal_id
        val conflictingMeal = seg.meals.values.find { m -> m.mealId != mealId && dishId in m.dishes }
        if (conflictingMeal != null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "dish_id「$dishId」已存在于 meal「${conflictingMeal.mealId}」，拒绝跨餐冲突", code = DiagnosticCode.DISH_CONFLICT))
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
                name = name, // name always from latest（同 exactKey 下 name 恒等，这里不会误改名）
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
        val rawMealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null, "缺少 meal_id"))
            return
        }
        val name = line.name ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, null, "缺少 name"))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }
        val mealId = canonicalMealId(line.segment_id, rawMealId, seg.meals)
        val meal = seg.meals[mealId]
        if (meal == null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, null,
                "食材/调料「$name」的 meal_id「$rawMealId」不存在", code = DiagnosticCode.ORPHAN_INGREDIENT))
            return
        }

        // 先按 dish_id 查找（10-c：dish_id 已本地化，用"最近一次解析出的本地 dishId"指针）
        var dish: DishDraftNode? = line.dish_id?.let { rawDishId ->
            dishLastLocalByRawId["${line.segment_id}|$mealId|$rawDishId"]?.let { meal.dishes[it] }
        }

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
                        "食材/调料「$name」的 dish_name「$dishName」$detail，无法确定归属", code = DiagnosticCode.ORPHAN_INGREDIENT))
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
                    line.dish_id, reason, code = DiagnosticCode.ORPHAN_INGREDIENT))
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
        val rawMealId = line.meal_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, null, null, "cooking_step 缺少 meal_id"))
            return
        }
        val rawDishId = line.dish_id ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, null, "cooking_step 缺少 dish_id"))
            return
        }
        val text = line.text ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, rawDishId, "cooking_step 缺少 text"))
            return
        }

        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }
        val mealId = canonicalMealId(line.segment_id, rawMealId, seg.meals)
        val meal = seg.meals[mealId] ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, rawMealId, rawDishId,
                "cooking_step 的 meal_id「$rawMealId」不存在"))
            return
        }
        val dishId = dishLastLocalByRawId["${line.segment_id}|$mealId|$rawDishId"] ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, rawDishId,
                "cooking_step 的 dish_id「$rawDishId」不存在"))
            return
        }
        val dish = meal.dishes[dishId] ?: run {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, line.segment_id, mealId, dishId,
                "cooking_step 的 dish_id「$rawDishId」不存在"))
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
        val mealId = line.meal_id?.let { canonicalMealId(line.segment_id, it, seg.meals) }
        val rawDishId = line.dish_id

        if (rawDishId != null && mealId != null) {
            val meal = seg.meals[mealId]
            // [AI修改 10-c，Google质量复核🟡-2修复] 本地 dishId 与 AI 原始 dish_id 同形但语义不同的两套命名空间，
            //   查不到本地指针时不得直接拿 rawDishId 当 key 去查 meal.dishes——那可能撞上另一道菜的本地 id，
            //   把 warning 挂到不相干的菜上。查不到就降级为餐次级 warning，信息不丢、不会错挂。
            val dishId = meal?.let { dishLastLocalByRawId["${line.segment_id}|$mealId|$rawDishId"] }
            if (meal != null && dishId != null) {
                val dish = meal.dishes[dishId]
                if (dish != null) {
                    seg.meals[mealId] = meal.copy(
                        dishes = meal.dishes + (dishId to dish.copy(warnings = dish.warnings + message))
                    )
                    return
                }
            }
        }
        if (mealId != null) {
            val meal = seg.meals[mealId]
            if (meal != null) {
                seg.meals[mealId] = meal.copy(warnings = meal.warnings + message)
                return
            }
        }
        seg.warnings.add(message)
    }

    private fun handleAdviceEvent(line: NdjsonLine) {
        val message = line.message ?: return
        hasAnyNdjsonEvent = true
        val seg = segmentMap.getOrPut(line.segment_id) { MutableSegmentDraft(line.segment_id) }
        val mealId = line.meal_id?.let { canonicalMealId(line.segment_id, it, seg.meals) }

        if (mealId != null) {
            val meal = seg.meals[mealId]
            if (meal != null) {
                seg.meals[mealId] = meal.copy(advices = meal.advices + message)
                return
            }
        }
        seg.warnings.add("💬 $message")
    }

    // ═══════════════════════════════ 整体 JSON 回退（AF-04 重写） ═══════════════════════════════

    // ============================================================
    // 整体 JSON fallback（AF-14: §7.5.5 单来源，不跨段猜测）
    // ============================================================

    /**
     * AF-14: 整体 JSON fallback 仅接受单一 owner segment。
     * 周期记每段独立请求→独立 parser，不能跨段推断来源。
     */
    private fun tryWholeJsonFallback() {
        val raw = allRawText.toString().trim()
        if (raw.isEmpty()) return

        // AF-14: segments.size != 1 → 拒绝
        if (segments.size != 1) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, null, null, null,
                "whole_json_fallback_requires_single_segment: 整体 JSON fallback 仅支持单一来源 segment，当前有 ${segments.size} 个"))
            return
        }
        val ownerSegment = segments.single()

        // 解析为 DayMealJson（FlatMealJson 或 MultiDayJson）
        val rawDays = parseWholeJsonToDays(raw)
        if (rawDays.isEmpty()) {
            return
        }

        orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.WARNING, ownerSegment.segmentId, null, null,
            "未检测到 NDJSON 事件，已按整体 JSON 格式规范化"))

        // 逐 day 调用 D-15 策略
        for ((di, rawDay) in rawDays.withIndex()) {
            val resolved = resolveWholeJsonFallbackDay(ownerSegment, rawDay) ?: continue
            buildSyntheticLinesFromResolvedDay(resolved, di)
        }
    }

    /** AF-14: 值对象——策略修正后的单天 fallback 结果。 */
    private data class ResolvedFallbackDay(
        val segmentId: String,
        val correctedDate: LocalDate,
        val day: DayMealJson,
    )

    /** AF-14: 对单个 rawDay 调用 MealDateAnchorPolicy，计算修正日期并归属 owner segment。 */
    private fun resolveWholeJsonFallbackDay(
        ownerSegment: InputSegment,
        rawDay: DayMealJson,
    ): ResolvedFallbackDay? {
        val policyResult = MealDateAnchorPolicy.apply(
            ownerSegment.inputText,
            ownerSegment.targetDate,
            listOf(rawDay),
        )
        val correctedDay = policyResult.days.firstOrNull() ?: return null

        // 解析修正日期
        val correctedDate: LocalDate = if (!correctedDay.date.isNullOrBlank()) {
            runCatching { LocalDate.parse(correctedDay.date!!.replace('/', '-')) }.getOrNull()
                ?: run {
                    orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.ERROR, ownerSegment.segmentId, null, null,
                        "fallback day ${correctedDay.date} 无效"))
                    return null
                }
        } else if (correctedDay.date_offset != 0) {
            ownerSegment.targetDate.plus(DatePeriod(days = correctedDay.date_offset))
        } else {
            ownerSegment.targetDate
        }

        if (policyResult.warning != null) {
            orphanDiagnostics.add(StreamDiagnostic(DiagnosticLevel.WARNING, ownerSegment.segmentId, null, null,
                "fallback 日期锚定: ${policyResult.warning}"))
        }

        return ResolvedFallbackDay(
            segmentId = ownerSegment.segmentId,
            correctedDate = correctedDate,
            day = correctedDay,
        )
    }

    /** AF-14: 从 ResolvedFallbackDay 生成合成 NdjsonLine 并喂入同链。修正日期同时用于 preview key/date/meal_id。 */
    private fun buildSyntheticLinesFromResolvedDay(resolved: ResolvedFallbackDay, dayIndex: Int) {
        val dateStr = resolved.correctedDate.toString()
        val segId = resolved.segmentId
        val dishIndexByMeal = mutableMapOf<String, Int>()

        for (meal in resolved.day.meals) {
            val slot = meal.meal_type ?: "lunch"
            val mealId = "$dateStr|$slot"
            if (mealId !in dishIndexByMeal) {
                processLine("""{"type":"meal","segment_id":"$segId","meal_id":"$mealId","date":"$dateStr","slot":"$slot"}""")
            }
            for (dishRef in meal.dishes) {
                val dishName = dishRef.name.ifBlank { dishRef.dish?.name ?: "未命名" }
                val dishIdx = dishIndexByMeal.getOrPut(mealId) { 0 } + 1
                dishIndexByMeal[mealId] = dishIdx
                val dishId = "$mealId|d$dishIdx"
                processLine("""{"type":"dish","segment_id":"$segId","meal_id":"$mealId","dish_id":"$dishId","name":"${escapeJson(dishName)}"}""")
                for (ing in dishRef.dish?.ingredients ?: emptyList()) {
                    val ingName = ing.ref ?: ing.food?.name ?: ""
                    processLine("""{"type":"ingredient","segment_id":"$segId","meal_id":"$mealId","dish_id":"$dishId","name":"${escapeJson(ingName)}","quantity":${ing.quantity}}""")
                }
            }
        }
    }

    /** AF-14: 解析整体 JSON → List<DayMealJson>。先 Flat，再 MultiDay。 */
    private fun parseWholeJsonToDays(raw: String): List<DayMealJson> {
        val flatResult = runCatching { json.decodeFromString<FlatMealJson>(raw) }.getOrNull()
        if (flatResult != null && flatResult.items.isNotEmpty()) {
            val converted = FlatToDayMealConverter.convert(flatResult, fallbackDate)
            return converted.days
        }
        val multiResult = runCatching { json.decodeFromString<MultiDayJson>(raw) }.getOrNull()
        if (multiResult != null && multiResult.days.isNotEmpty()) {
            return multiResult.days
        }
        return emptyList()
    }

    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    // ═══════════════════════════════ 辅助方法 ═══════════════════════════════

    /**
     * [AI修改 10-b，Google质量复核🔴-1修复] 把 AI 声明的 "segmentId|原始meal_id" 归一为自愈后的 canonical mealId。
     *
     * **真实存在的餐次永远优先于别名表**：raw 本身已是 [existingMeals] 里的真实 key 时直接原样返回，
     * 不查别名——否则一个"曾被错误复述过"的字符串，只要后来又恰好被另一条合法事件当成 raw 用了，
     * 就会被陈旧别名重定向到别处，静默挂错餐次。查不到真实 key 时才落回别名表；无归一记录则原样返回。
     */
    private fun canonicalMealId(segmentId: String, raw: String, existingMeals: Map<String, MealDraftNode>): String {
        if (raw in existingMeals) return raw
        return mealIdAlias["$segmentId|$raw"] ?: raw
    }

    private fun mergeNote(existing: String?, incoming: String?): String? {
        if (incoming.isNullOrBlank()) return existing
        if (existing.isNullOrBlank()) return incoming
        return "$existing; $incoming"
    }

    private fun buildDraft(): MealStreamDraft {
        return MealStreamDraft(
            segments = segmentMap.mapValues { (_, mut) ->
                SegmentDraft(
                    segmentId = mut.segmentId,
                    meals = mut.meals.mapValues { (_, node) -> node },
                    warnings = mut.warnings.toList(),
                )
            },
            diagnostics = orphanDiagnostics.toList(),
            finishReason = finishReason,
            isTruncated = isLengthTruncated,
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
)
