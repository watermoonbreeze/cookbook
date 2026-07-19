package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.AppPalette
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

    /** 监听配色主题（默认赤陶橘）。[AI生成] */
    fun observePalette(): Flow<AppPalette> =
        q.selectPreference(PreferenceKeys.APP_PALETTE).asFlow().mapToOneOrNull(ioDispatcher).map { row ->
            AppPalette.fromCode(row?.value_)
        }

    /** 写入配色主题。[AI生成] */
    suspend fun setPalette(palette: AppPalette) = withContext(ioDispatcher) {
        q.upsertPreference(PreferenceKeys.APP_PALETTE, palette.code, DateTime.nowEpochSeconds())
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

    /** 监听"当前查看成员"指针(多人关注·今日卡+报告共用)。空/非数字→null(由 resolveViewing 自愈回退)。[AI生成] */
    fun observeFocusViewingMemberId(): Flow<Long?> =
        q.selectPreference(PreferenceKeys.FOCUS_VIEWING_MEMBER_ID).asFlow().mapToOneOrNull(ioDispatcher).map { it?.value_?.toLongOrNull() }

    /** 一次性读"当前查看成员"指针(suspend·focusMember 用)。[AI生成] */
    suspend fun focusViewingMemberId(): Long? = withContext(ioDispatcher) {
        q.selectPreference(PreferenceKeys.FOCUS_VIEWING_MEMBER_ID).executeAsOneOrNull()?.value_?.toLongOrNull()
    }

    /** 设置"当前查看成员"指针。[AI生成] */
    suspend fun setFocusViewingMemberId(id: Long) = withContext(ioDispatcher) {
        q.upsertPreference(PreferenceKeys.FOCUS_VIEWING_MEMBER_ID, id.toString(), DateTime.nowEpochSeconds())
    }

    /**
     * 写入任意偏好 key。[AI修改]
     */
    suspend fun set(key: String, value: String) = withContext(ioDispatcher) {
        q.upsertPreference(key, value, DateTime.nowEpochSeconds())
    }

    // ============ 阶段3 匿名统计（默认关·首启询问式·守隐私红线） ============

    /** 监听"匿名使用统计"开关（默认关）。[AI生成] 阶段3 */
    fun observeAnalyticsEnabled(): Flow<Boolean> =
        observeFlag(PreferenceKeys.ANALYTICS_ENABLED, default = false)

    /** 一次性读"匿名使用统计"是否开启（App 启动初始化埋点闸门用）。[AI生成] 阶段3 */
    suspend fun isAnalyticsEnabled(): Boolean = withContext(ioDispatcher) {
        q.selectPreference(PreferenceKeys.ANALYTICS_ENABLED).executeAsOneOrNull()?.value_ == "1"
    }

    /** 设置"匿名使用统计"开关。[AI生成] 阶段3 */
    suspend fun setAnalyticsEnabled(enabled: Boolean) = setFlag(PreferenceKeys.ANALYTICS_ENABLED, enabled)

    /**
     * 取匿名标识（不存在则首次生成随机 UUID 并持久化）。[AI生成] 阶段3
     *
     * 不用任何设备硬标识；卸载重装即换新 UUID（略微低估长周期留存，换取"绝不可复原到个人"）。
     * 注：读→写非原子——极低概率的首启并发调用可能生成两个 UUID、后写覆盖先写；匿名标识"换一个"无业务损害(不涉去重计费)，可接受、不加事务。
     */
    suspend fun getOrCreateAnalyticsUuid(): String = withContext(ioDispatcher) {
        val existing = q.selectPreference(PreferenceKeys.ANALYTICS_UUID).executeAsOneOrNull()?.value_
        if (!existing.isNullOrBlank()) return@withContext existing
        val uuid = com.sxdbsm.cookbook.platform.randomUuid()
        q.upsertPreference(PreferenceKeys.ANALYTICS_UUID, uuid, DateTime.nowEpochSeconds())
        uuid
    }
}
