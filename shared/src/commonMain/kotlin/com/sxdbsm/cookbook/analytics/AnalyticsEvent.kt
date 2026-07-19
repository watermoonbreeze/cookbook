package com.sxdbsm.cookbook.analytics

/**
 * @File : AnalyticsEvent
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 匿名统计业务事件（密封类·只暴露 §2.1 八个去标识化事件）
 * <p>
 * 阶段3 账号匿名统计的**数据边界红线载体**：调用点只能构造这里定义的 8 个事件，
 * 每个事件的参数**只允许枚举/布尔/计数**（无自由文本、无菜名/食材名/病种/身体数据/成员信息）。
 * 从**类型系统层面**杜绝"把健康明细传进埋点"的越界——想传也没有对应参数可传。
 * <p>
 * 对齐方案 `账号体系与用户统计方案.md` §2.1 / §四数据边界红线。
 * <p>
 * [AI生成] 阶段3 匿名统计：埋点抽象层的事件模型。
 **/
sealed interface AnalyticsEvent {
    /** 上报事件名（友盟/自建后端统一用此稳定串）。 */
    val name: String

    /** 去标识化属性（全部枚举/布尔/计数字符串·绝不含健康明细）。 */
    val params: Map<String, String> get() = emptyMap()

    /** 打开 App（DAU/WAU、留存、会话）。 */
    data object AppOpen : AnalyticsEvent {
        override val name = "app_open"
    }

    /** 首次启动（新增、留存队列起点）。 */
    data object FirstLaunch : AnalyticsEvent {
        override val name = "first_launch"
    }

    /** 记了一餐（"记一餐"周活跃频次·核心）。只带餐次枚举，**不带菜名/给谁/命中什么**。 */
    data class MealLogged(val slot: MealSlotTag) : AnalyticsEvent {
        override val name = "meal_logged"
        override val params = mapOf("slot" to slot.tag)
    }

    /** 请求了一次推荐（推荐使用率）。只带来源枚举。 */
    data class RecommendRequested(val source: RecommendSourceTag) : AnalyticsEvent {
        override val name = "recommend_requested"
        override val params = mapOf("source" to source.tag)
    }

    /** 采纳了推荐（推荐采纳率·核心）。只带来源枚举。 */
    data class RecommendAdopted(val source: RecommendSourceTag) : AnalyticsEvent {
        override val name = "recommend_adopted"
        override val params = mapOf("source" to source.tag)
    }

    /** 本次出现过"忌口标注"（忌口命中真实场景比例·核心）。**仅布尔存在性**，不带病种/菜/食材。 */
    data object AvoidHitShown : AnalyticsEvent {
        override val name = "avoid_hit_shown"
    }

    /** 用了某功能区（功能频次分布、发现漏斗）。只带功能区枚举。 */
    data class FeatureUsed(val feature: FeatureTag) : AnalyticsEvent {
        override val name = "feature_used"
        override val params = mapOf("feature" to feature.tag)
    }

    /** 设置了健康档案（健康档案渗透率）。**仅布尔"设了"**，绝不带病种/忌口/身体数据。 */
    data object HealthProfileSet : AnalyticsEvent {
        override val name = "health_profile_set"
    }
}

/** 餐次枚举（去标识化·仅四类）。[AI生成] 阶段3 */
enum class MealSlotTag(val tag: String) {
    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner"),
    SNACK("snack"),
}

/** 推荐来源枚举（去标识化）。[AI生成] 阶段3 */
enum class RecommendSourceTag(val tag: String) {
    PANTRY("pantry"), // 库存推荐
    RANDOM("random"), // 随机
    PLAN("plan"), // 周计划
    AI("ai"), // 云端 AI
    HOME("home"), // 首页"下一餐"引流卡
}

/**
 * 功能区枚举（去标识化·白名单）。[AI生成] 阶段3
 *
 * feature_used 只上报**枚举白名单**内的功能区；未知路由一律映射 OTHER（绝不把原始路由串直接上报）。
 */
enum class FeatureTag(val tag: String) {
    HOME("home"),
    DISHES("dishes"),
    INGREDIENTS("ingredients"),
    PANTRY("pantry"),
    SEARCH("search"),
    REPORT("report"),
    TIMER("timer"),
    WEEK_PLAN("week_plan"),
    AI_RECOMMEND("ai_recommend"),
    FAMILY("family"),
    MINE("mine"),
    OTHER("other");

    companion object {
        /**
         * 由导航路由映射到白名单功能区（未知→null，调用方决定是否上报 OTHER）。
         * 只做前缀/包含匹配，**不回传原始路由**，保证不越界。
         */
        fun fromRoute(route: String?): FeatureTag? {
            if (route.isNullOrBlank()) return null
            val r = route.substringBefore('?').substringBefore('/')
            return when {
                r.startsWith("home") -> HOME
                r.startsWith("dishes") -> DISHES
                r.startsWith("ingredient") -> INGREDIENTS
                r.startsWith("pantry") -> PANTRY
                r.startsWith("search") -> SEARCH
                r.startsWith("report") || r.startsWith("diet_report") -> REPORT
                r.startsWith("timer") || r.startsWith("cook") -> TIMER
                r.startsWith("week_plan") || r.startsWith("plan") -> WEEK_PLAN
                r.startsWith("ai_recommend") || r.startsWith("ai_plan") -> AI_RECOMMEND
                r.startsWith("family") -> FAMILY
                r.startsWith("mine") || r.startsWith("settings") -> MINE
                else -> null
            }
        }
    }
}
