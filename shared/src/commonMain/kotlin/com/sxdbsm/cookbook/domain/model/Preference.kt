package com.sxdbsm.cookbook.domain.model

/**
 * App 主题模式。[AI修改]
 *
 * `code` 是写入数据库的稳定字符串；界面展示时使用 enum，避免到处比较裸字符串。
 */
enum class ThemeMode(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromCode(code: String?): ThemeMode = values().firstOrNull { it.code == code } ?: SYSTEM
    }
}

/**
 * App 配色主题（与明暗模式 [ThemeMode] 独立）。[AI生成]
 *
 * 6 套 Apple 风格高级配色，每套单一强调色贯穿 + 协调中性骨架；默认赤陶橘（现状）。
 * `code` 稳定写库；具体 light/dark 色板在 androidApp 的 Palettes.kt 按此枚举取。
 */
enum class AppPalette(val code: String, val displayName: String) {
    TERRACOTTA("terracotta", "赤陶橘"), // 默认：陶土暖橘，温暖有食欲(现状基准)
    CITRUS("citrus", "柑橘橙"), // 柑橘/南瓜/胡萝卜，更鲜亮开胃
    GARDEN("garden", "蔬鲜绿"), // 叶菜/西兰花/牛油果，均衡健康
    BLUEBERRY("blueberry", "蓝莓靛"), // 蓝莓/深色浆果，抗氧护心
    MINT("mint", "薄荷青"), // 薄荷/黄瓜/清茶，清新解腻
    TOMATO("tomato", "番茄红"), // 番茄/红椒/石榴，热情暖胃
    GRAPE("grape", "葡萄紫"); // 紫葡萄/紫甘蓝/茄子，彩虹饮食

    companion object {
        fun fromCode(code: String?): AppPalette = entries.firstOrNull { it.code == code } ?: TERRACOTTA
    }
}

/**
 * 用户偏好表的 key 常量集合。[AI修改]
 *
 * Kotlin 的 `object` 是单例，类似 Java 中只有 static 常量的工具类。
 */
object PreferenceKeys {
    const val THEME_MODE = "theme_mode"

    // [AI生成] 配色主题(赤陶橘默认 + 多套高级预设)：存 AppPalette.code，默认赤陶橘。与明暗(THEME_MODE)独立。
    const val APP_PALETTE = "app_palette"
    const val HOME_RECENT_COUNT = "home_recent_count"
    const val HOME_POPULAR_COUNT = "home_popular_count"

    // [AI生成] 分步执行开关：开启后菜品操作步骤显示"步骤N"序号并可进入分步烹饪；**默认开**(用户可关)。
    const val STEP_MODE_ENABLED = "step_mode_enabled"

    // [AI生成] 营养色系开关：开启后餐食卡片按营养均衡级别配背景色，并驱动首页"每天营养色系墙"；**默认开**(用户可关)。
    const val NUTRITION_COLOR_ENABLED = "nutrition_color_enabled"

    // [AI生成] 热量数值显示开关：开启后餐食卡片/详情显示估算热量与达标(数字)；与营养色系独立控制；**默认开**(用户可关)。
    const val CALORIE_NUMBER_ENABLED = "calorie_number_enabled"

    // [AI生成] P2:餐次结构建议开关：今日卡对"缺蔬菜/早餐缺蛋白"给一句浅灰下一步小字(T1事后留痕·鼓励非评判)；**默认开**(用户可关)。
    const val MEAL_STRUCTURE_HINT_ENABLED = "meal_structure_hint_enabled"

    // [AI修改] 功能开关默认值集中定义(防散落漂移·踩坑红线:observeFlag 默认各调用点传易漂移)——所有调用点统一引用这些常量。
    //   用户 2026-07-22 决策"先默认展示健康膳食能力·可关闭"(透明 opt-out)：热量数值/营养色系/分步执行由默认关翻为**默认开**。
    //   热量数字仍守"仅供参考·非医嘱"免责(旧"热量个人概念·默认关"红线按此决策更新为"默认开·可关")。
    const val DEFAULT_STEP_MODE = true
    const val DEFAULT_NUTRITION_COLOR = true
    const val DEFAULT_CALORIE_NUMBER = true
    const val DEFAULT_PANTRY_HOOK = true
    const val DEFAULT_MEAL_STRUCTURE_HINT = true // [AI生成] P2:餐次结构建议默认开(透明 opt-out·可关)

    // [AI生成] 推荐风格(增长型推荐轻干预)：BALANCED/FAMILIAR/FRESH/NUTRITION，默认综合(BALANCED)。
    const val RECOMMEND_STYLE = "recommend_style"

    // [AI生成] 慢病知情引导(F4b)一次性标记：已登记痛风/糖尿病用户在推荐页提示"切偏营养=高GI/嘌呤菜靠后"，关过/切过后永不再显。
    const val NUTRITION_HINT_DISMISSED = "nutrition_hint_dismissed_v1"

    // [AI生成] 身体数据(JSON: BodyMetrics)：身高/体重/年龄/性别/活动量，用于算每日卡路里目标。免迁移存偏好。
    const val BODY_METRICS = "body_metrics"

    // [AI生成] 基础数据 seed 内容指纹：记录上次写入的 seed JSON 指纹，内容未变时跳过整段补齐式写入。
    const val SEED_CONTENT_FINGERPRINT = "seed_content_fingerprint"

    // [AI生成] F#8 透明准则:基础数据"更新记录"游标(与指纹解耦·免迁移存偏好)。
    //   APPLIED=DB 现有数据对应的 changelog 版本(reseed 后=内置最新)；NOTIFIED=已告知用户到的版本。
    //   APPLIED>NOTIFIED → 有未告知的新版数据(该弹更新说明+我的页红点)。首装/首次引入本功能→基线对齐(不追溯弹旧变更)。
    const val SEED_APPLIED_CHANGELOG_VERSION = "seed_applied_changelog_version"
    const val SEED_NOTIFIED_CHANGELOG_VERSION = "seed_notified_changelog_version"

    // [AI生成] 旧库 NULL 文本字段清洗标记：清洗只需执行一次，避免每次启动全表 UPDATE。
    const val SEED_LEGACY_SANITIZED = "seed_legacy_sanitized_v1"

    // [AI生成] 首次启动引导：已看过功能介绍则不再自动弹(仍可从"我的"入口手动看)。
    const val HAS_SEEN_GUIDE = "has_seen_guide_v1"

    // [AI生成] 库存挂钩开关：开=App 记住家里有哪些食材并提示"现在可做/需采购/缺料"；关=纯菜谱/记菜,全 App 不再出现
    //   库存与采购标注(去噪)。**默认开**(既有行为的可选关闭,老用户升级不丢功能)——与营养色系/热量(新增可选,默认关)相反。
    const val PANTRY_HOOK_ENABLED = "pantry_hook_enabled"

    // [AI生成] v多人关注：当前查看的家庭成员 id(今日卡+报告共用指针)。免迁移存偏好;空/失效自愈回退关注集合首位。
    const val FOCUS_VIEWING_MEMBER_ID = "focus_viewing_member_id"

    // [AI生成] 阶段3 匿名统计：是否已同意匿名使用统计。**默认关**(首启询问式·最强隐私姿态)。关闭时埋点抽象层统一拦截、绝不上报。
    const val ANALYTICS_ENABLED = "analytics_enabled"
    // [AI生成] 阶段3 匿名统计：匿名标识(首启本地随机 UUID·不用任何设备硬标识·卸载重装即换新·绝不可复原到个人)。
    const val ANALYTICS_UUID = "analytics_uuid"

    // [AI生成] 阶段3-c 合规：是否已同意《用户协议》《隐私政策》(首启弹窗·未同意不放行·合规 gate)。默认未同意。
    const val PRIVACY_AGREED = "privacy_agreed_v1"
}
