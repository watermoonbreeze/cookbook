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
}
