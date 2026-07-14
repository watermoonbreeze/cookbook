package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.PreferenceKeys
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.util.DateTime
import com.sxdbsm.cookbook.platform.ioDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * 用户偏好仓库。[AI修改]
 *
 * 当前主要保存主题模式，后续也可以保存首页展示数量等简单 key-value 配置。
 */
class PreferenceRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries
    private val bodyJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true } // [AI生成] 身体数据序列化

    /**
     * 监听主题模式。[AI修改]
     */
    fun observeThemeMode(): Flow<ThemeMode> =
        q.selectPreference(PreferenceKeys.THEME_MODE).asFlow().mapToOneOrNull(ioDispatcher).map { row ->
            ThemeMode.fromCode(row?.value_)
        }

    /**
     * 写入主题模式。[AI修改]
     */
    suspend fun setThemeMode(mode: ThemeMode) = withContext(ioDispatcher) {
        q.upsertPreference(PreferenceKeys.THEME_MODE, mode.code, DateTime.nowEpochSeconds())
    }

    /**
     * 监听布尔开关偏好。[AI生成]
     *
     * 存储值 "1"=true / 其它=false；无记录时返回 default。供功能设置页开关响应式驱动 UI。
     */
    fun observeFlag(key: String, default: Boolean): Flow<Boolean> =
        q.selectPreference(key).asFlow().mapToOneOrNull(ioDispatcher).map { row ->
            row?.value_?.let { it == "1" } ?: default
        }

    /**
     * 写入布尔开关偏好。[AI生成]
     */
    suspend fun setFlag(key: String, value: Boolean) = set(key, if (value) "1" else "0")

    /**
     * 监听字符串偏好（无记录返回 default）。[AI生成]
     *
     * 供推荐风格等枚举型偏好响应式驱动 UI。
     */
    fun observeString(key: String, default: String): Flow<String> =
        q.selectPreference(key).asFlow().mapToOneOrNull(ioDispatcher).map { row -> row?.value_ ?: default }

    /**
     * 读取任意偏好 key。[AI修改]
     */
    suspend fun get(key: String): String? = withContext(ioDispatcher) {
        q.selectPreference(key).executeAsOneOrNull()?.value_
    }

    /** 监听身体数据(每日卡路里目标用)。无记录/解析失败返回默认空 BodyMetrics。[AI生成] */
    fun observeBodyMetrics(): Flow<com.sxdbsm.cookbook.domain.model.BodyMetrics> =
        q.selectPreference(PreferenceKeys.BODY_METRICS).asFlow().mapToOneOrNull(ioDispatcher).map { row ->
            row?.value_?.let { runCatching { bodyJson.decodeFromString<com.sxdbsm.cookbook.domain.model.BodyMetrics>(it) }.getOrNull() }
                ?: com.sxdbsm.cookbook.domain.model.BodyMetrics()
        }

    /** 保存身体数据。[AI生成] */
    suspend fun setBodyMetrics(m: com.sxdbsm.cookbook.domain.model.BodyMetrics) = withContext(ioDispatcher) {
        q.upsertPreference(PreferenceKeys.BODY_METRICS, bodyJson.encodeToString(m), DateTime.nowEpochSeconds())
    }

    /**
     * 写入任意偏好 key。[AI修改]
     */
    suspend fun set(key: String, value: String) = withContext(ioDispatcher) {
        q.upsertPreference(key, value, DateTime.nowEpochSeconds())
    }
}
