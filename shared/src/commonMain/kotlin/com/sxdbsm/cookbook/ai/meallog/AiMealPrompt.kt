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
 * [AI修改] B1 周期记+NDJSON流式改造：新增 NDJSON 系统提示 + buildStreamingRequest()。
 * maxTokens 按非空 segment 数量缩放。
 * <p>
 * [AI生成] K1→K2 AI快捷输入记餐专项重构：Prompt 层。
 **/
object AiMealPrompt {

    // ============================================================
    // NDJSON 流式 Prompt（B1 新增·主路径）
    // ============================================================

    /** NDJSON 流式输出的系统提示。[AI生成] B1 */
    @Suppress("MaxLineLength")
    val NDJSON_SYSTEM_PROMPT = """
你是家庭营养师，将用户自然语言解析为 NDJSON（每行一个 JSON 对象，行尾 \n）。
只输出 NDJSON，禁止 Markdown、解释文本、空行和注释。每行仅一个 JSON 对象。

输出事件类型（type 字段必填）：
- {"type":"meal","segment_id":"<段ID>","meal_id":"<日期>|<餐次>","date":"YYYY-MM-DD","slot":"breakfast/lunch/dinner/snack","time":"HH:MM","note":"备注"}
- {"type":"dish","segment_id":"<段ID>","meal_id":"<日期>|<餐次>","dish_id":"<meal_id>|d<序号>","name":"菜名","cooking_method":"做法","quantity":1,"unit":"份","eaten_ratio":null,"note":""}
- {"type":"ingredient","segment_id":"<段ID>","meal_id":"...","dish_id":"...","dish_name":"所属菜名(缺dish_id时必填)","name":"食材名","role":"主料/辅料","food_group":"meat/vegetable/staple/...","quantity":100,"unit":"g","is_main":true,"nutrients":["蛋白质"]}
- {"type":"seasoning","segment_id":"<段ID>","meal_id":"...","dish_id":"...","dish_name":"所属菜名(缺dish_id时必填)","name":"盐","quantity":3,"unit":"g"}
- {"type":"warning","segment_id":"<段ID>","meal_id":"...","dish_id":"...","message":"诊断信息"}
- {"type":"advice","segment_id":"<段ID>","meal_id":"...","message":"健康建议"}
- {"type":"done","segment_id":"<段ID>","summary":"该段完成摘要"}

规则：
- segment_id 必须与输入给出的段 ID 完全一致；不允许跨段输出
- meal_id 格式 = "{date}|{slot}"，如 "2026-08-05|lunch"
- dish_id 格式 = "{meal_id}|d{正整数序号}"，如 "2026-08-05|lunch|d1"
- 先输出 meal → 再 dish → 最后 ingredient/seasoning（按父键关联）
- 绝对日期优先于所选日期；星期按所选日期所在周推算；无日期用所选日期
- dish_name 不含数量词；份量默认 1 份；食用比例 null=吃完/0.5=一半/0.75=大半/0.25=少量
- 食材用量参考：肉100-200g 菜100-150g 主食100-200g 蛋50-60g 奶200-250g 调料3-15g
- food_group: meat/vegetable/staple/fruit/dairy/egg/bean/seafood/seasoning
- 不确定的字段省略不填，不编造；菜名是唯一必填
- ingredient/seasoning 的 dish_id 必须引用已输出的 dish 的 dish_id；当无法提供正确的 dish_id 时，必须同时提供 dish_name（该食材所属的菜品名），用于同一 meal 内按菜品名唯一补挂；无唯一命中即诊断拒绝
- 输出 done 事件标记该段完成
- 逗号顿号加号换行→必拆菜；做法词(炒煮蒸)是菜名一部分不拆
""".trimIndent()

    /** 扁平格式系统提示（保留供整体 JSON 回退使用）。[AI修改] K2 紧凑版 */
    @Suppress("MaxLineLength")
    val FLAT_SYSTEM_PROMPT = """
你是家庭营养师，将用户自然语言解析为扁平 JSON。只输出 JSON，不要任何解释。

输出格式 {"schema_version":"2.0","items":[{...}]}，每个 item 自包含一道菜：

字段(短key见示例，填不出用默认)：
- date: 仅当用户原文明确给出绝对日期时填 YYYY-MM-DD；原文没有绝对日期时不要虚构 date
- date_offset: 以"选择的餐食日期"为 0 的相对天数；原文没有日期时填 0，出现周几时按该日期所在周推算
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
- "选择的餐食日期"是唯一日期锚点，不是设备当前日期；没有绝对日期时不得把今天或推测日期写入 date
- 多天按周一分段→相对"选择的餐食日期"所在周推算 date_offset
- 调料is_main=false; 不确定食材宁缺毋滥; 不吃/没吃的餐次不建item
- 时间、餐次、菜名、食材、调料、做法、食材大类和营养尽量填；菜名是唯一必填，其余未知直接省略或null；输出纯JSON, 无markdown, 字段能填则填勿臆造
""".trimIndent()

    /**
     * 构建流式请求（B1 新增·主路径）。[AI生成] B1
     *
     * @param segments 非空分段列表（至少 1 个）
     * @return LLM 请求（使用 NDJSON prompt + 按段缩放 maxTokens）
     */
    fun buildStreamingRequest(
        segments: List<InputSegment>,
    ): LlmRequest {
        val nonBlank = segments.filter { !it.isBlank }
        val userPrompt = buildStreamingUserPrompt(nonBlank)
        val maxTokens = when {
            nonBlank.size <= 1 -> 2048
            else -> (nonBlank.size * 4096).coerceAtMost(8192)
        }
        return LlmRequest(
            system = NDJSON_SYSTEM_PROMPT,
            user = userPrompt,
            temperature = 0.2,
            maxTokens = maxTokens,
        )
    }

    /** 构建流式用户 prompt。[AI生成] B1 */
    private fun buildStreamingUserPrompt(segments: List<InputSegment>): String = buildString {
        if (segments.size == 1) {
            val seg = segments.single()
            append("用户说：${seg.inputText}\n")
            append("segment_id：${seg.segmentId}\n")
            append("锚点日期：${seg.targetDate}（${dayOfWeekName(seg.targetDate)}）\n")
        } else {
            append("周期记（${segments.size} 天），每天一段：\n\n")
            segments.forEachIndexed { i, seg ->
                append("--- 第${i + 1}天 ---\n")
                append("segment_id：${seg.segmentId}\n")
                append("锚点日期：${seg.targetDate}（${dayOfWeekName(seg.targetDate)}）\n")
                append("用户说：${seg.inputText}\n\n")
            }
        }
        append("请逐段输出 NDJSON，每段以 done 事件结束。")
    }

    /** 日期→星期中文名。[AI生成] B1 */
    private fun dayOfWeekName(date: kotlinx.datetime.LocalDate): String {
        val names = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return names[date.dayOfWeek.ordinal]
    }

    // ============================================================
    // 旧非流式 Prompt（保留供整体 JSON 回退和规则降级使用）
    // ============================================================

    @Deprecated("B1 新协议使用 NDJSON_SYSTEM_PROMPT", ReplaceWith("NDJSON_SYSTEM_PROMPT"))
    val SYSTEM_PROMPT: String get() = FLAT_SYSTEM_PROMPT

    /**
     * 构建非流式 AI 请求（保留供旧调用和回退使用）。[AI修改]
     */
    fun buildRequest(
        userInput: String,
        today: String,
        weekday: String,
        nowTime: String,
        rulePreview: String? = null,
    ): LlmRequest = LlmRequest(
        system = FLAT_SYSTEM_PROMPT,
        user = buildFlatUserPrompt(userInput, today, weekday, nowTime, rulePreview),
        temperature = 0.2,
        maxTokens = 4096,
    )

    private fun buildFlatUserPrompt(
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
