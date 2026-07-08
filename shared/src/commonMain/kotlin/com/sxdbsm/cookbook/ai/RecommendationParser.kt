package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.MealSuggestion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * @File : RecommendationParser
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 解析模型输出的 JSON 为 MealSuggestion（容错：截取 JSON 子串、忽略未知字段）
 * <p>
 * 模型可能夹带多余文字，先截首个 '{' 到末个 '}' 再解析；解析失败返回 null，由 Orchestrator 兜底。
 * <p>
 * [AI生成] S1：模型输出必须先过解析+校验才转业务对象，绝不直接采信。
 **/
object RecommendationParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 解析为建议列表；失败返回 null。[AI生成] */
    fun parse(raw: String): List<MealSuggestion>? {
        val jsonText = extractJsonObject(raw) ?: return null
        return runCatching {
            json.decodeFromString<RawDraft>(jsonText).suggestions.map {
                MealSuggestion(
                    dishIds = it.dishIds,
                    reason = it.reason.trim(),
                    cookingHint = it.cookingHint?.trim()?.ifBlank { null },
                )
            }
        }.getOrNull()
    }

    /** 截取首个 '{' 到最后一个 '}' 的子串。[AI生成] */
    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start in 0 until end) raw.substring(start, end + 1) else null
    }

    @Serializable
    private data class RawDraft(val suggestions: List<RawSuggestion> = emptyList())

    @Serializable
    private data class RawSuggestion(
        val dishIds: List<Long> = emptyList(),
        val reason: String = "",
        val cookingHint: String? = null,
    )
}
