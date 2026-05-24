package com.sxdbsm.cookbook.domain.model

enum class ThemeMode(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromCode(code: String?): ThemeMode = values().firstOrNull { it.code == code } ?: SYSTEM
    }
}

object PreferenceKeys {
    const val THEME_MODE = "theme_mode"
    const val HOME_RECENT_COUNT = "home_recent_count"
    const val HOME_POPULAR_COUNT = "home_popular_count"
}
