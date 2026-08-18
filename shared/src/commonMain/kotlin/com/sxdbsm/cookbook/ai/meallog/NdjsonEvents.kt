package com.sxdbsm.cookbook.ai.meallog

import kotlinx.serialization.Serializable

/**
 * @File : NdjsonEvents
 * @Time : 2026/08/05
 * @Author : SXD-AI
 * @Desc : AI 记一餐 NDJSON 流式协议事件类型定义。
 * <p>
 * 每行一个 JSON 对象（NdjsonLine），按 type 路由到不同处理分支。
 * 内部用 NdjsonEvent sealed 族做强类型处理，解析层用 NdjsonLine 容错收口。
 * <p>
 * [AI生成] B1 周期记+NDJSON流式改造：协议层事件定义。
 **/

// ═══════════════════════════════════════════════════════════
// 传输层：AI 输出的单行 JSON（flat，所有字段可选）
// ═══════════════════════════════════════════════════════════

/** AI 输出的单行 NDJSON 对象——涵盖所有事件类型的可能字段。[AI生成] */
@Serializable
data class NdjsonLine(
    val type: String = "",
    val segment_id: String = "",
    // meal 事件字段
    val meal_id: String? = null,
    val date: String? = null,
    val slot: String? = null,
    val time: String? = null,
    val note: String? = null,
    // dish 事件字段
    val dish_id: String? = null,
    val dish_name: String? = null, // [AI修改] AF-05: ingredient 事件中携带的所属菜品名，用于 dish_name+meal_id 唯一补挂
    val name: String? = null,
    val cooking_method: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val eaten_ratio: Double? = null,
    // ingredient 事件字段
    val role: String? = null,
    val food_group: String? = null,
    val nutrients: List<String>? = null,
    val is_main: Boolean? = null,
    // cooking_step 事件字段
    val text: String? = null,
    val order: Int? = null,
    // warning/advice 事件字段
    val message: String? = null,
)

// ═══════════════════════════════════════════════════════════
// 内部类型：强类型事件（解析后使用）
// ═══════════════════════════════════════════════════════════

/** NDJSON 事件内部强类型表示。[AI生成] */
sealed class NdjsonEvent {
    abstract val segmentId: String

    /** 餐次事件：创建/更新一个餐次父节点。[AI生成] */
    data class MealEvent(
        override val segmentId: String,
        val mealId: String,   // "{date}|{slot}"
        val date: String,     // "YYYY-MM-DD"
        val slot: String,     // breakfast/lunch/dinner/snack
        val time: String?,
        val note: String?,
    ) : NdjsonEvent()

    /** 菜品事件：创建一道菜。[AI生成] */
    data class DishEvent(
        override val segmentId: String,
        val mealId: String,
        val dishId: String,   // "{mealId}|d{index}"
        val name: String,
        val cookingMethod: String?,
        val quantity: Double?,
        val unit: String?,
        val eatenRatio: Double?,
        val note: String?,
    ) : NdjsonEvent()

    /** 食材事件：加入一道菜的食材。[AI生成] */
    data class IngredientEvent(
        override val segmentId: String,
        val mealId: String,
        val dishId: String,
        val name: String,
        val role: String?,
        val foodGroup: String?,
        val quantity: Double?,
        val unit: String?,
        val nutrients: List<String>?,
        val isMain: Boolean?,
    ) : NdjsonEvent()

    /** 调料事件：加入非主料调料。[AI生成] */
    data class SeasoningEvent(
        override val segmentId: String,
        val mealId: String,
        val dishId: String,
        val name: String,
        val quantity: Double?,
        val unit: String?,
        val nutrients: List<String>?,
    ) : NdjsonEvent()

    /** 烹饪步骤事件：仅展示，不写库。[AI生成] */
    data class CookingStepEvent(
        override val segmentId: String,
        val mealId: String,
        val dishId: String,
        val text: String,
        val order: Int?,
    ) : NdjsonEvent()

    /** 警告事件：解析/数据质量诊断。[AI生成] */
    data class WarningEvent(
        override val segmentId: String,
        val message: String,
        val mealId: String?,
        val dishId: String?,
    ) : NdjsonEvent()

    /** 健康建议事件：仅会话展示，不入库。[AI生成] */
    data class AdviceEvent(
        override val segmentId: String,
        val message: String,
        val mealId: String?,
    ) : NdjsonEvent()
}

// ═══════════════════════════════════════════════════════════
// 内部草稿模型（解析中逐步构建）
// ═══════════════════════════════════════════════════════════

/** 流式解析过程中逐步构建的餐食草稿。[AI生成] */
data class MealStreamDraft(
    /** segment_id → 分段草稿 */
    val segments: Map<String, SegmentDraft> = emptyMap(),
    /** 解析过程中产生的诊断信息 */
    val diagnostics: List<StreamDiagnostic> = emptyList(),
    /** 网络层 finish_reason */
    val finishReason: String? = null,
    /** 是否被截断 */
    val isTruncated: Boolean = false,
)

/** 单个分段的草稿。[AI生成] */
data class SegmentDraft(
    val segmentId: String,
    /** meal_id → 餐次草稿 */
    val meals: Map<String, MealDraftNode> = emptyMap(),
    /** 该分段的段级警告 */
    val warnings: List<String> = emptyList(),
)

/** 餐次节点。[AI生成] */
data class MealDraftNode(
    val mealId: String,
    val date: String,
    val slot: String,
    val time: String? = null,
    val note: String? = null,
    /** dish_id → 菜品节点 */
    val dishes: Map<String, DishDraftNode> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val advices: List<String> = emptyList(),
)

/** 菜品节点。[AI生成] */
data class DishDraftNode(
    val dishId: String,
    val name: String,
    val cookingMethod: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val eatenRatio: Double? = null,
    val note: String? = null,
    val ingredients: List<DraftIngredient> = emptyList(),
    val seasonings: List<DraftSeasoning> = emptyList(),
    val cookingSteps: List<DraftCookingStep> = emptyList(),
    val warnings: List<String> = emptyList(),
)

/** 食材草稿项。[AI生成] */
data class DraftIngredient(
    val name: String,
    val role: String? = null,
    val foodGroup: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val nutrients: List<String>? = null,
    val isMain: Boolean? = null,
)

/** 调料草稿项。[AI生成] */
data class DraftSeasoning(
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val nutrients: List<String>? = null,
)

/** 烹饪步骤草稿项。[AI生成] */
data class DraftCookingStep(
    val text: String,
    val order: Int? = null,
)

/** 流式解析过程中的诊断信息。[AI生成] */
data class StreamDiagnostic(
    val level: DiagnosticLevel,
    val segmentId: String?,
    val mealId: String?,
    val dishId: String?,
    val message: String,
    // [AI修改 10-a] 按分类出人话文案 + 同类合并计数用，默认值保证既有位置参数构造不受影响。
    val code: DiagnosticCode = DiagnosticCode.OTHER,
)

enum class DiagnosticLevel { WARNING, ERROR, INFO }

/** 诊断分类代号——供上层按类合并/出人话文案，不依赖脆弱的 message 字符串匹配。[AI生成 10-a] */
enum class DiagnosticCode {
    INVALID_SLOT,       // 餐次类型不合法
    MEAL_ID_MISMATCH,   // meal_id 与 date|slot 冲突（先后指向不同餐次，拒绝）
    MEAL_ID_NORMALIZED, // meal_id 已按 date|slot 自愈归一（非阻断，仅提示）
    DISH_ID_FORMAT,     // dish_id 格式/前缀不合法
    DISH_ID_REUSED,     // dish_id 被复用于不同的菜，已按新菜处理（非阻断，仅提示）
    ORPHAN_DISH,        // 菜品找不到父餐次
    ORPHAN_INGREDIENT,  // 食材/调料找不到所属菜品
    DISH_CONFLICT,      // dish_id 跨餐冲突
    TRUNCATED,          // 响应被截断
    TAIL_INCOMPLETE,    // 结束时仍有未完成的半行内容
    UNKNOWN_TYPE,       // 未知事件类型
    SEGMENT_MISMATCH,   // segment_id 不匹配/缺失
    INVALID_DATE,       // 日期格式无效
    PARSE_ERROR,        // NDJSON 行解析失败
    OTHER,              // 未分类（含整体 JSON fallback 相关提示）
}
