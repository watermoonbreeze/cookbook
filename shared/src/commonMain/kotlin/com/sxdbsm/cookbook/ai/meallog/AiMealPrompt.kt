package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.ai.LlmRequest

/**
 * @File : AiMealPrompt
 * @Time : 2026/07/28
 * @Author : SXD-AI
 * @Desc : AI 快捷输入记餐的 Prompt 构建器（纯函数，无副作用）
 * <p>
 * [AI修改] K2 重写：完整 JSON 示例 + 多场景规则覆盖 + 食材估量参考范围。
 * 支持单餐/多餐/跨天/整周。规则引擎预解析结果作为 hint 提升准确率。
 * 调用参数：temperature=0.2, maxTokens=4096（多天场景需更大）。
 * <p>
 * [AI生成] K1→K2 AI快捷输入记餐专项重构：Prompt 层。
 **/
object AiMealPrompt {

    /** AI 行为规范（system prompt）。[AI修改] K2 重写：完整示例+多场景+食材估量 */
    // [AI修改] 紧凑版：去掉冗余 JSON 示例和表格，用 compact 短 key + 规则要点，省 ~50% token
    val SYSTEM_PROMPT = """
你是家庭营养师，将用户自然语言解析为扁平 JSON。只输出 JSON，不要任何解释。

输出格式 {"schema_version":"2.0","items":[{...}]}，每个 item 自包含一道菜：

字段(短key见示例，填不出用默认)：
- date: 仅当用户原文明确给出绝对日期时填 YYYY-MM-DD；原文没有绝对日期时不要虚构 date
- date_offset: 以“选择的餐食日期”为 0 的相对天数；原文没有日期时填 0，出现周几时按该日期所在周推算
- meal_type: "breakfast"/"lunch"/"dinner"/"snack", 未提null
- meal_time: "HH:MM", 未提null; meal_note: 整餐备注(少盐/少油等), 同餐首菜填即可
- dish_name: 菜名必填, 不含数量词
- dish_quantity: 默认1; dish_unit: 份/碗/盘/个/杯; dish_eaten_ratio: null=吃完/0.5=一半/0.75=大半/0.25=少量
- dish_cooking_methods: [炒煮蒸炸煎烤炖拌烧焖卤]
- dish_cuisine: 默认"家常菜"; dish_note/dish_tags: 可选[]/""
- ingredients[{name,quantity(g),unit:"g",is_main:bool, food_group, nutrition?}]；nutrition 可按已知填写能量、蛋白、脂肪、碳水、纤维、钠、钾、钙、GI、嘌呤等，不确定则省略
  quantity参考: 肉100-200 菜100-150 主食100-200 蛋50-60 奶200-250 调料3-15
  food_group: meat/vegetable/staple/fruit/dairy/egg/bean/seafood/seasoning

规则:
- 逗号顿号加号换行→必拆菜; "和/跟/还有"两端都是菜名→拆; 做法词(炒煮蒸)是菜名一部分不拆
- 数量词前置: "三个鸡蛋"→dish_name=鸡蛋 qty=3; 食用比例: "吃了一半"→0.5
- 括号: "鲜牛奶(加热)"→note="加热"; "凉皮(黄瓜丝+绿豆芽)"→食材=黄瓜丝+绿豆芽
- 菜名推断食材(不确定留空): 番茄炒蛋→番茄100+鸡蛋50, 红烧肉→五花肉150+酱油10+糖5
- “选择的餐食日期”是唯一日期锚点，不是设备当前日期；没有绝对日期时不得把今天或推测日期写入 date
- 多天按周一分段→相对“选择的餐食日期”所在周推算 date_offset
- 调料is_main=false; 不确定食材宁缺毋滥; 不吃/没吃的餐次不建item
- 时间、餐次、菜名、食材、调料、做法、食材大类和营养尽量填；菜名是唯一必填，其余未知直接省略或null；输出纯JSON, 无markdown, 字段能填则填勿臆造
""".trimIndent()

    /**
     * 构建 AI 请求。[AI修改] K2：maxTokens 提升到 4096，支持多天场景
     *
     * @param userInput 用户输入的文字
     * @param today 当前日期 "YYYY-MM-DD"
     * @param weekday 星期几（中文，如"周三"）
     * @param nowTime 当前时间 "HH:MM"
     * @param rulePreview 规则引擎预解析结果（可选，作为 hint 提升 AI 准确率）
     */
    fun buildRequest(
        userInput: String,
        today: String,
        weekday: String,
        nowTime: String,
        rulePreview: String? = null,
    ): LlmRequest = LlmRequest(
        system = SYSTEM_PROMPT,
        user = buildUserPrompt(userInput, today, weekday, nowTime, rulePreview),
        temperature = 0.2,
        maxTokens = 4096,
    )

    /** 构建用户 prompt。[AI修改] K2：追加规则引擎预解析作为 hint */
    private fun buildUserPrompt(
        userInput: String,
        today: String,
        weekday: String,
        nowTime: String,
        rulePreview: String?,
    ): String = buildString {
        append("用户说：$userInput\n")
        append("选择的餐食日期（唯一日期锚点）：$today（$weekday）\n")
        append("当前时间：$nowTime\n")
        if (!rulePreview.isNullOrBlank()) {
            append("\n--- 规则引擎初步解析（供参考，可能不准确，请纠正和补全）---\n")
            append(rulePreview)
        }
    }
}
