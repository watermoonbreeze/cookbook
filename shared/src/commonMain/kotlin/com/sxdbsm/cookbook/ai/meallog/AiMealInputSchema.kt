package com.sxdbsm.cookbook.ai.meallog

import kotlinx.serialization.Serializable

/**
 * @File : AiMealInputSchema
 * @Time : 2026/07/28
 * @Author : SXD-AI
 * @Desc : AI 快捷输入记餐的 JSON Schema 数据类
 * <p>
 * 定义 AI 输出的结构化 JSON 约定（配 kotlinx.serialization 反序列化）。
 * AI 返回的 JSON 必须符合本 Schema，字段多余/缺失由 Json{ignoreUnknownKeys} + 默认值兼容。
 * 解析失败时走 AiMealParser.localFallback() 本地兜底。
 * <p>
 * ⚠️ [AI修改 2026-08-18] 无生产调用方：B3 NDJSON 流式改造后主链路走 StreamingMealParser，
 * 本文件仅存量供 AiMealParser/SchemaMigration 内部与其单测使用。改动前先确认是否应直接删除。
 * <p>
 * [AI生成] K1 AI快捷输入记餐：Schema 层，shared 纯数据，无平台依赖。
 **/

/**
 * AI 解析整次输入的顶层结果。[AI生成]
 *
 * @param date_offset 日期偏移：-2=前天, -1=昨天, 0=今天, 1=明天
 * @param meals 解析出的餐次列表，至少 1 项
 */
@Serializable
data class AiMealParseResult(
    val date_offset: Int = 0,
    val meals: List<AiParsedMeal> = emptyList(),
)

/**
 * AI 解析出的一餐。[AI生成]
 *
 * @param meal_type 餐次类型："breakfast"/"lunch"/"dinner"/"snack"；null=按时间推断
 * @param meal_time 用餐时间 "HH:MM"；null=餐次默认时间
 * @param note 整餐备注（如"少放盐"）
 * @param dishes 该餐包含的菜品，至少 1 项
 */
@Serializable
data class AiParsedMeal(
    val meal_type: String? = null,
    val meal_time: String? = null,
    val note: String = "",
    val dishes: List<AiParsedDish> = emptyList(),
)

/**
 * AI 解析出的一道菜。[AI生成]
 *
 * @param name 菜名，必填
 * @param quantity 份数（如"2碗饭"→2）
 * @param quantity_unit 份量单位："碗"/"盘"/"份"/"个"
 * @param eaten_ratio 食用比例：null=吃完(1.0), 0.5=一半, 0.75=大半, 0.25=少量
 * @param cooking_methods 烹饪方式：["炒","煮"]
 * @param note 单菜专属备注
 * @param ingredients AI 推断的食材拆解（可选，无食材信息时为空）
 */
@Serializable
data class AiParsedDish(
    val name: String,
    val quantity: Double = 1.0,
    val quantity_unit: String = "份",
    val eaten_ratio: Double? = null,
    val cooking_methods: List<String> = emptyList(),
    val note: String = "",
    val ingredients: List<AiParsedIngredient> = emptyList(),
)

/**
 * AI 推断的单味食材。[AI生成]
 *
 * @param name 食材名
 * @param quantity 克数，默认 100g
 * @param unit 单位，默认 "g"
 * @param is_main 是否主料
 */
@Serializable
data class AiParsedIngredient(
    val name: String,
    val quantity: Double = 100.0,
    val unit: String = "g",
    val is_main: Boolean = true,
)
