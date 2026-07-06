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

    // [AI生成] 基础数据 seed 内容指纹：记录上次写入的 seed JSON 指纹，内容未变时跳过整段补齐式写入。
    const val SEED_CONTENT_FINGERPRINT = "seed_content_fingerprint"

    // [AI生成] 旧库 NULL 文本字段清洗标记：清洗只需执行一次，避免每次启动全表 UPDATE。
    const val SEED_LEGACY_SANITIZED = "seed_legacy_sanitized_v1"
}
