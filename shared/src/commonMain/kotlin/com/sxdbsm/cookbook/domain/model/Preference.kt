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
 * 用户偏好表的 key 常量集合。[AI修改]
 *
 * Kotlin 的 `object` 是单例，类似 Java 中只有 static 常量的工具类。
 */
object PreferenceKeys {
    const val THEME_MODE = "theme_mode"
    const val HOME_RECENT_COUNT = "home_recent_count"
    const val HOME_POPULAR_COUNT = "home_popular_count"

    // [AI生成] 分步执行开关：开启后菜品操作步骤显示"步骤N"序号并可进入分步烹饪；默认关(只按用户书写顺序展示)。
    const val STEP_MODE_ENABLED = "step_mode_enabled"

    // [AI生成] 营养色系开关：开启后餐食卡片按营养均衡级别配背景色，并驱动首页"每天营养色系墙"；默认关。
    const val NUTRITION_COLOR_ENABLED = "nutrition_color_enabled"

    // [AI生成] 热量数值显示开关：开启后餐食卡片/详情显示估算热量与达标(数字)；与营养色系独立控制；默认关。
    const val CALORIE_NUMBER_ENABLED = "calorie_number_enabled"

    // [AI生成] 推荐风格(增长型推荐轻干预)：BALANCED/FAMILIAR/FRESH/NUTRITION，默认综合(BALANCED)。
    const val RECOMMEND_STYLE = "recommend_style"

    // [AI生成] 身体数据(JSON: BodyMetrics)：身高/体重/年龄/性别/活动量，用于算每日卡路里目标。免迁移存偏好。
    const val BODY_METRICS = "body_metrics"

    // [AI生成] 基础数据 seed 内容指纹：记录上次写入的 seed JSON 指纹，内容未变时跳过整段补齐式写入。
    const val SEED_CONTENT_FINGERPRINT = "seed_content_fingerprint"

    // [AI生成] 旧库 NULL 文本字段清洗标记：清洗只需执行一次，避免每次启动全表 UPDATE。
    const val SEED_LEGACY_SANITIZED = "seed_legacy_sanitized_v1"
}
