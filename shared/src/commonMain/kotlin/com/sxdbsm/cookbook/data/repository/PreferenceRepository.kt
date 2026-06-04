package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.PreferenceKeys
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 用户偏好仓库。[AI修改]
 *
 * 当前主要保存主题模式，后续也可以保存首页展示数量等简单 key-value 配置。
 */
class PreferenceRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries

    /**
     * 监听主题模式。[AI修改]
     */
    fun observeThemeMode(): Flow<ThemeMode> =
        q.selectPreference(PreferenceKeys.THEME_MODE).asFlow().mapToOneOrNull(Dispatchers.Default).map { row ->
            ThemeMode.fromCode(row?.value_)
        }

    /**
     * 写入主题模式。[AI修改]
     */
    suspend fun setThemeMode(mode: ThemeMode) = withContext(Dispatchers.Default) {
        q.upsertPreference(PreferenceKeys.THEME_MODE, mode.code, DateTime.nowEpochSeconds())
    }

    /**
     * 读取任意偏好 key。[AI修改]
     */
    suspend fun get(key: String): String? = withContext(Dispatchers.Default) {
        q.selectPreference(key).executeAsOneOrNull()?.value_
    }

    /**
     * 写入任意偏好 key。[AI修改]
     */
    suspend fun set(key: String, value: String) = withContext(Dispatchers.Default) {
        q.upsertPreference(key, value, DateTime.nowEpochSeconds())
    }
}
