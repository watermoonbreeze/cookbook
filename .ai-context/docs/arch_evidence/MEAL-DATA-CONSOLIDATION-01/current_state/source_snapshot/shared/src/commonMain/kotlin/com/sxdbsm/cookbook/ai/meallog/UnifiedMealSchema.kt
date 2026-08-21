package com.sxdbsm.cookbook.ai.meallog

import kotlinx.serialization.Serializable

/**
 * @File : UnifiedMealSchema
 * @Time : 2026/07/29
 * @Author : SXD-AI
 * @Desc : AI 快捷记餐四级统一 JSON Schema（L1 食材 → L2 菜品 → L3 餐次 → L4 一天）
 * <p>
 * 设计原则：
 * - 层层内嵌复用：L1→L2→L3→L4，AI 可一次输出完整嵌套或只引用已有实体
 * - name 即引用键：对 AI 友好（不暴露内部 ID），入库时按 name 查已有→复用
 * - 全字段默认值：容错 AI 输出不完整；ignoreUnknownKeys 兼容多余字段
 * - FlatMealJson 扁平格式：AI 优先输出扁平行列表（降低嵌套深度，LLM 更稳定），
 *   后端 FlatToDayMealConverter 聚合为 DayMealJson
 * <p>
 * [AI生成] K2 AI快捷输入记餐专项重构：统一数据流水线 Schema 层。
 **/

// ═══════════════════════════════════════════════════════════
// L1: FoodJson — 食材创建数据
// ═══════════════════════════════════════════════════════════

/** 食材创建 JSON（对应添加食材界面全部字段 + source）。[AI生成] */
@Serializable
data class FoodJson(
    val name: String,                              // 必填：食材名
    val alias: String = "",                        // 别名（多个用/分隔）
    val emoji: String = "🥗",                       // 默认图标
    val default_unit: String = "g",                // 默认单位名
    val category_names: List<String> = emptyList(), // 分类名列表（如["蔬菜","叶菜"]）
    val food_group: String? = null,                 // 营养大类：meat/vegetable/staple/fruit/dairy/egg/bean/seafood/seasoning
    val nutrition: FoodNutritionJson? = null,       // 营养数值（可选）
    val detail: FoodDetailJson? = null,             // 详情扩展
    val image_hint: String = "",                    // 图片描述（AI 无法给真实图片，给文字提示）
    val source: String = "ai",                      // 来源：preset/user/ai/imported
)

/** 食材营养数值（每 100g）。[AI生成] */
@Serializable
data class FoodNutritionJson(
    val energy_kcal: Double? = null,
    val protein_g: Double? = null,
    val fat_g: Double? = null,
    val carb_g: Double? = null,
    val fiber_g: Double? = null,
    val sodium_mg: Double? = null,
    val potassium_mg: Double? = null,
    val calcium_mg: Double? = null,
    val gi: Double? = null,                // 升糖指数
    val purine_mg: Double? = null,         // 嘌呤 mg/100g
    val saturated_fat_g: Double? = null,
    val cholesterol_mg: Double? = null,
    val piece_gram: Double? = null,        // 单件克重（1个鸡蛋≈50g）
    val ref: String = "",                  // 数据来源
    val review: Boolean = false,           // 是否人工复核
)

/** 食材详情扩展。[AI生成] */
@Serializable
data class FoodDetailJson(
    val common_methods: String = "",       // 常用做法
    val prep_tips: String = "",           // 处理技巧
    val eating_notes: String = "",        // 食用注意
    val storage_tips: String = "",        // 保存方法
    val health_note: String = "",         // 健康提示
)

// ═══════════════════════════════════════════════════════════
// L2: DishJson — 菜品创建数据
// ═══════════════════════════════════════════════════════════

/** 菜品创建 JSON（对应菜品编辑界面全部字段 + source）。[AI生成] */
@Serializable
data class DishJson(
    val name: String,                              // 必填：菜名
    val cooking_methods: List<String> = emptyList(),// 烹饪方式
    val tags: List<String> = emptyList(),           // 标签（下饭菜/快手菜等）
    // [AI修改] 默认值改为空串：协议里没有任何来源会显式填这个字段(生产 0 处传参)，
    //   非空默认值会让"AI没提菜系"和"AI明确说是家常菜"两种语义无法区分，避免被误当真实信号采信。
    val cuisine: String = "",                       // 菜系
    val special_note: String = "",                  // 特别说明
    val description: String = "",                   // 菜品描述
    val steps: List<String> = emptyList(),           // 步骤描述（简单文字列表）
    val meal_slots: List<String> = emptyList(),      // 适合餐次：breakfast/lunch/dinner/snack
    val image_hint: String = "",                    // 图片描述
    val ingredients: List<DishIngredientJson> = emptyList(), // 食材列表
    val source: String = "ai",                      // 来源
)

/** 菜品中的食材引用/创建。[AI生成] */
@Serializable
data class DishIngredientJson(
    // 引用已有食材（二选一：ref 优先，food 用于新建）
    val ref: String? = null,             // 食材名引用 → 入库时按 name 查已有
    val food: FoodJson? = null,          // 新建食材的完整信息（ref=null 时用）
    val quantity: Double = 100.0,        // 用量
    val unit: String = "g",              // 单位
    val is_main: Boolean = true,         // 是否主料
    val note: String = "",               // 该食材在本菜中的备注
)

// ═══════════════════════════════════════════════════════════
// L3: MealJson — 餐次入库数据
// ═══════════════════════════════════════════════════════════

/** 餐次入库 JSON。[AI生成] */
@Serializable
data class MealJson(
    val meal_type: String? = null,            // breakfast/lunch/dinner/snack
    val meal_time: String? = null,            // HH:MM
    val note: String = "",                    // 整餐备注（少盐/清淡）
    val dishes: List<MealDishRefJson> = emptyList(),
)

/** 餐次中菜品引用/创建。[AI生成] */
@Serializable
data class MealDishRefJson(
    val name: String = "",                   // 菜名（便利字段，=ref ?: dish?.name ?: ""）
    // 引用已有菜品（二选一：ref 优先，dish 用于新建）
    val ref: String? = null,             // 菜品名引用 → 入库时按 name 查已有
    val dish: DishJson? = null,          // 新建菜品的完整信息（ref=null 时用）
    val quantity: Double = 1.0,           // 本餐份数
    val quantity_unit: String = "份",     // 份量单位
    val eaten_ratio: Double? = null,      // 单菜食用比例（null=吃完即1.0）
    val note: String = "",                // 单菜备注
)

// ═══════════════════════════════════════════════════════════
// L4: DayMealJson — 一天餐食数据
// ═══════════════════════════════════════════════════════════

/** 一天餐食 JSON。[AI生成] */
@Serializable
data class DayMealJson(
    // 日期（二选一：date 优先）
    val date: String? = null,                // YYYY-MM-DD 绝对日期
    val date_offset: Int = 0,                // 相对今天偏移：-2/-1/0/1/2...
    val weekday: String? = null,             // 星期几（辅助校验，非必填）
    // 餐次列表
    val meals: List<MealJson> = emptyList(),
    // 元信息
    val raw_input: String = "",              // 原始用户输入（可溯源）
    val parse_method: String = "",           // ai/rule/hybrid/ai_chat
    // [AI生成] P2-1: K1b 健康评价(AI返回·可空·向后兼容)
    val health_evaluation: HealthEvaluation? = null,
)

// ═══════════════════════════════════════════════════════════
// 多天汇总（周计划用）
// ═══════════════════════════════════════════════════════════

/** 多天汇总 JSON（周计划入口用）。[AI生成] */
@Serializable
data class MultiDayJson(
    val schema_version: String = "1.0",         // Schema 版本（演进兼容）
    val days: List<DayMealJson> = emptyList(),
    val meta: MultiDayMetaJson? = null,
)

/** 多天汇总元信息。[AI生成] */
@Serializable
data class MultiDayMetaJson(
    val start_date: String = "",             // YYYY-MM-DD
    val end_date: String = "",               // YYYY-MM-DD
    val total_days: Int = 0,
    val source: String = "",                 // weekly_import/ai_plan/rule_parse
    val conversation_id: String = "",        // 对话式 AI 溯源预留
)

// ═══════════════════════════════════════════════════════════
// 扁平中间格式（AI 优先输出此格式，后端再聚合为 DayMealJson）
// ═══════════════════════════════════════════════════════════

/**
 * AI 输出用的扁平格式——每行一道菜，自包含全部信息。
 * LLM 生成深层嵌套 JSON 容易出错，扁平格式每行独立、无需管理嵌套括号，
 * 后端 FlatToDayMealConverter 聚合为嵌套 DayMealJson（纯函数，零风险）。
 * [AI生成]
 */
@Serializable
data class FlatMealJson(
    // [AI修改] 未发布版本直接采用 V2；不保留旧协议迁移分支。
    val schema_version: String = "2.0",
    val items: List<FlatMealItem> = emptyList(),
)

/** 扁平格式的一行：一道菜 + 所属餐次 + 所属日期 + 食材。[AI生成] */
@Serializable
data class FlatMealItem(
    // 日期信息
    val date: String? = null,              // YYYY-MM-DD
    val date_offset: Int = 0,              // 相对偏移（date 为空时用）
    val weekday: String? = null,            // 星期几

    // 餐次信息
    val meal_type: String? = null,          // breakfast/lunch/dinner/snack
    val meal_time: String? = null,          // HH:MM
    val meal_note: String = "",             // 整餐备注（同餐多菜时只在第一道菜上填即可，聚合时会合并）

    // 菜品信息
    val dish_name: String = "",             // 菜名
    val dish_quantity: Double = 1.0,        // 份数
    val dish_unit: String = "份",           // 份数单位
    val dish_eaten_ratio: Double? = null,   // 食用比例
    val dish_note: String = "",             // 单菜备注
    val dish_cooking_methods: List<String> = emptyList(), // 烹饪方式
    val dish_tags: List<String> = emptyList(),     // 标签
    val dish_cuisine: String = "",           // 菜系

    // 食材信息（该道菜包含的食材列表）
    val ingredients: List<FlatIngredientItem> = emptyList(),
)

/** 扁平格式的食材行。[AI生成] */
@Serializable
data class FlatIngredientItem(
    val name: String,                       // 食材名
    val quantity: Double = 100.0,           // 克数
    val unit: String = "g",                 // 单位
    val is_main: Boolean = true,            // 是否主料
    val food_group: String? = null,         // 营养大类
    val note: String = "",                  // 食材备注
)

// ═══════════════════════════════════════════════════════════
// P2-1: K1b 健康评价（AI 返回·可空·向后兼容·免责）
// ═══════════════════════════════════════════════════════════

/** AI 返回的健康评价。[AI生成] P2-1 */
@Serializable
data class HealthEvaluation(
    val perMember: List<MemberEval> = emptyList(),
    val perMeal: List<MealEval> = emptyList(),
    val overall: String = "",
)

/** 单成员评价。[AI生成] P2-1 */
@Serializable
data class MemberEval(
    val memberRef: String = "",  // "成员1"/"张三"
    val note: String = "",       // 评价文字·守免责
)

/** 单餐评价。[AI生成] P2-1 */
@Serializable
data class MealEval(
    val mealRef: String = "",    // "早餐"/"午餐"/"晚餐"
    val note: String = "",       // 评价文字·守免责
)
