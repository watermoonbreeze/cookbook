package com.sxdbsm.cookbook.domain.model

import kotlinx.serialization.Serializable

/** 菜谱解析结果（RecipeParser 纯函数产出·未持久化；持久化投影=share_link 四列）。[AI生成] STEP-L4-2.2 */
@Serializable
data class ParsedRecipe(
    val title: String,
    val ingredients: List<ParsedIngredient>,
    val steps: List<ParsedStep>,
    val cookingMethods: List<String>,
    val note: String = "",
    val imageUrl: String = "",
)

@Serializable
data class ParsedIngredient(
    val name: String,
    val quantity: Double = 0.0,
    val unit: String = "",
    val isMain: Boolean = false,
)

@Serializable
data class ParsedStep(
    val text: String,
    val imageUrl: String = "",
)
