package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.ai.LlmRequest

/**
 * @File : AiMealPrompt
 * @Time : 2026/07/28
 * @Author : SXD-AI
 * @Desc : AI 快捷输入记餐的 Prompt 构建器（纯函数，无副作用）
 * <p>
 * 构建 system prompt（AI 行为规范）和 user prompt（用户输入上下文）。
 * 调用参数按方案设计：temperature=0.2, jsonMode=true, maxTokens=1024。
 * <p>
 * [AI生成] K1 AI快捷输入记餐：Prompt 层。
 **/
object AiMealPrompt {

    /** AI 行为规范（system prompt）。[AI生成] */
    private val SYSTEM_PROMPT = """
你是家庭饮食记录助手。从用户自然语言中提取结构化用餐信息，输出 JSON。

规则：
1. 日期偏移 date_offset：未提→0；昨天/昨→-1；前天→-2；明天→1
2. 餐次 meal_type：早餐/早饭→"breakfast"；午餐/中饭/中午→"lunch"；
   晚餐/晚饭→"dinner"；加餐/宵夜/零食/下午茶→"snack"；未提→null
3. 菜品拆分：分隔符"和""跟""还有""+""、"→多道菜。
   "三个鸡蛋"=1道菜(quantity=3/unit="个")，非3道菜。
   数字+量词的组合(两碗饭、三盘菜、2个包子)一并处理。
4. 份量 quantity/quantity_unit：
   一碗→1/碗；两盘→2/盘；三个→3/个；半份→0.5/份
   未提数量→quantity=1, quantity_unit="份"
5. 食用比例 eaten_ratio：吃了/吃完→null(默认)；一半→0.5；大半→0.75；少量→0.25
6. 烹饪方式 cooking_methods：煮/蒸/炒/炸/煎/烤/炖/拌/烧/焖/卤
7. 备注：整餐通用归 meal.note（少盐/少油/清淡）；单菜专属归 dish.note
8. 食材拆解 ingredients（如"牛肉面"→牛肉+面条，估计正常份量克数），
   有把握的才填；不确定就留空。
9. **只输出 JSON，不要任何多余文字**。
""".trimIndent()

    /**
     * 构建 AI 请求。[AI生成]
     *
     * @param userInput 用户输入的文字（语音转文字后或直接打字）
     * @param today 当前日期 "YYYY-MM-DD"
     * @param weekday 星期几（中文，如"周三"）
     * @param nowTime 当前时间 "HH:MM"
     */
    fun buildRequest(
        userInput: String,
        today: String,
        weekday: String,
        nowTime: String,
    ): LlmRequest = LlmRequest(
        system = SYSTEM_PROMPT,
        user = buildUserPrompt(userInput, today, weekday, nowTime),
        temperature = 0.2,
        maxTokens = 1024,
    )

    /**
     * 构建用户 prompt。[AI生成]
     */
    private fun buildUserPrompt(
        userInput: String,
        today: String,
        weekday: String,
        nowTime: String,
    ): String = buildString {
        append("用户说：$userInput\n")
        append("当前日期：$today（$weekday）\n")
        append("当前时间：$nowTime")
    }
}
