package com.sxdbsm.cookbook.domain.model

import kotlinx.serialization.Serializable

/** 下厨房等分享来源的解析配置（xiachufang.json v3 的 Kotlin 投影）。[AI生成] STEP-L4-2.1 */
@Serializable
data class ParseConfig(
    val version: Int,
    val source: String,
    val title: TitleCfg,
    val ingredients: IngredientsCfg,
    val steps: StepsCfg,
    val cookingMethods: CookingMethodsCfg,
    val image: ImageCfg,
    val note: NoteCfg? = null,
    val meta: MetaCfg? = null,
)

@Serializable
data class TitleCfg(
    val domSelector: String,
    val fallbackPattern: String,
    val fallbackNoiseFilter: List<String> = emptyList(),
    val cleanPattern: String,
)

@Serializable
data class IngredientsCfg(
    val sectionStart: String,
    val sectionEnd: String,
    val layout: String,
    val quantityPattern: String,
)

@Serializable
data class StepsCfg(
    val sectionStart: String,
    val sectionEnd: String,
    val itemStart: String,
    val layout: String,
    // [AI生成] S-05 死配置注记：本批 JS 侧选择器硬编码(WebViewTextExtractor)，这两字段 Phase2 由 JS 读配置接管才生效。
    val stepsRootSelector: String = "",
    val stepItemSelector: String = "",
    val imagesPerStep: Int = 1,
)

@Serializable
data class CookingMethodsCfg(
    val fromTitle: Boolean = true,
    val fromSteps: Boolean = true,
)

@Serializable
data class ImageCfg(
    val cssSelector: String,
    val attr: String,
)

@Serializable
data class NoteCfg(
    val between: List<String> = emptyList(),
    val maxLines: Int = 10,
)

@Serializable
data class MetaCfg(
    val author: String = "",
)
