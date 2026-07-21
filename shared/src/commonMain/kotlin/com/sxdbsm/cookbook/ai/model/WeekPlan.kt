package com.sxdbsm.cookbook.ai.model

/**
 * @File : WeekPlan
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 周期规划的数据契约（天数 1~30 任选，非固定一周）
 * <p>
 * 从整个食材库出发，按季节/营养维度均衡/不重复/健康档案(≥80%利健康)规划 N(1~30) 天×若干餐。
 * 规则独立于数据源：PlanDish 已把季节/营养维度/健康标记算好，WeeklyPlanner 只按规则排。
 * <p>
 * [AI生成] 周期规划模式：一周完整计划，营养按维度标签均衡。
 **/

/** 规划引擎输入的候选菜（已算好营养/季节/健康标记）。[AI生成] */
data class PlanDish(
    val id: Long,
    val name: String,
    val mainNames: List<String>, // 主料(去重用)
    val nutritionTags: Set<String>, // 营养维度(优质蛋白/高膳食纤维/深色蔬菜…)
    val seasonTags: Set<String>, // 应季/春/夏/秋/冬
    val isHealthy: Boolean, // 含调养推荐且不含限量(有档案时)
    val hasAvoid: Boolean, // 含忌口食材 → 规划时剔除
    val isMeat: Boolean = false, // [AI生成] 是否荤菜(主料含肉/鱼/蛋等)；规划时同餐尽量荤素兼有
    val cuisine: String = "", // [AI生成] 用户#2:菜系(周计划一餐同菜系不混搭·中式优先)
    val isBreakfast: Boolean = false, // 是否早餐菜(粥/蒸蛋/豆浆奶/燕麦/面点/吐司面包等·由 MealSlot.BREAKFAST 判)；早餐档只从早餐菜里选，符合中式饮食。[AI修改] QW-3:去玉米/南瓜/薯误判
    val breakfastSoft: Boolean = false, // 早餐软/饮(粥/豆浆/奶/燕麦/蛋羹/面) vs 硬/主食(馒头/包子/油条/吐司等)，用于软硬搭配
    val recommendHits: List<String> = emptyList(), // 利于调养的食材(说明用)
    val limitHits: List<String> = emptyList(), // 限量食材(说明用)
    val cookingMethodNames: List<String> = emptyList(), // [AI生成] 做法(红烧/清蒸…)：供周计划"重油族跨餐去重"(与单餐 MMR 重油族同口径·cookingHeaviness)。
    // [AI生成] D4:高GI/高嘌呤主料命中(仅登记糖尿病/痛风时非空·已去重 avoid∪limit 防双罚)——周计划"偏营养"风格软降(与单餐同口径)。
    val highGiHits: List<String> = emptyList(), // 高GI(≥70)主料，仅登记糖尿病
    val highPurineHits: List<String> = emptyList(), // 高嘌呤"应避免"主料，仅登记痛风
)

/** 规划出的一道菜 + 侧重点说明。[AI生成] */
data class PlannedDish(
    val id: Long,
    val name: String,
    val reason: String, // 为什么推荐/好处/注意点
    val shortageNames: List<String> = emptyList(), // [AI生成] 库存有但份数不够的主料 → "缺"
    val purchaseNames: List<String> = emptyList(), // [AI生成] 库存里没有的主料 → "采购"
)

/** 一餐(如早餐)的若干菜。[AI生成] */
data class PlannedMeal(
    val mealName: String,
    val dishes: List<PlannedDish>,
    val fromRule: Boolean = false, // [AI生成] 该餐是否由规则/本地补充(AI 未覆盖时)，UI 需标注区分
)

/** 一天的餐次安排。[AI生成] */
data class DayPlan(
    val dayIndex: Int, // 0 起，相对今天的第几天
    val meals: List<PlannedMeal>,
)

/** 周期规划取数结果：候选菜 + 当前季节 + 是否有健康档案。[AI生成] */
data class PlanContext(
    val dishes: List<PlanDish>,
    val season: String, // 春季/夏季/秋季/冬季
    val healthAware: Boolean,
)

/** N(1~30)天完整计划。[AI生成] */
data class PeriodPlan(
    val days: List<DayPlan>,
    val healthAware: Boolean, // 是否结合了健康档案
    val healthyRatio: Double, // 实际利健康占比(用于校验≥80%)
)
