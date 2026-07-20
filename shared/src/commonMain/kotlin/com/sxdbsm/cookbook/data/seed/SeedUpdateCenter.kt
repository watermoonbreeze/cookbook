package com.sxdbsm.cookbook.data.seed

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.PreferenceKeys
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * @File : SeedUpdateCenter
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : F#8 透明准则——基础数据自动更新(reseed)的「更新记录」读取与告知游标。
 * <p>
 * reseed 本身仍在启动时自动补齐式跑完(只补不删·不动用户自建数据)，本类负责其**透明告知**：
 * 读内置 changelog(`seed/changelog.json`) + 偏好里的两个整数游标(APPLIED/NOTIFIED·与内容指纹解耦)，
 * 判「有无未告知的新版数据」、取「待告知条目」「全部历史」，并标记已告知(幂等消红点)。
 * 启动弹窗 / 我的·更新记录中心 / 手动"更新基础数据"完成告知 三处共用同一份 changelog(单一真相源)。
 * <p>
 * [AI生成] 透明准则 P0(F#8)：让"App 背着用户更新基础数据"变为可知可查。
 **/
@Serializable
data class SeedChangelogEntry(
    val version: Int,
    val displayName: String = "",
    val date: String = "",
    val summary: String = "",
    val changes: List<SeedChangeItem> = emptyList(),
)

@Serializable
data class SeedChangeItem(
    val type: String = "", // added=新增 / fixed=修复 / adjusted=调整
    val text: String = "",
)

class SeedUpdateCenter(private val db: CookbookDatabase) {
    private val q get() = db.cookbookQueries
    private val json = Json { ignoreUnknownKeys = true }

    private fun loadAll(): List<SeedChangelogEntry> =
        SeedResourceLoader.readText("seed/changelog.json")
            ?.let { runCatching { json.decodeFromString<List<SeedChangelogEntry>>(it) }.getOrNull() }
            ?.sortedByDescending { it.version }
            ?: emptyList()

    private fun prefInt(key: String): Int? =
        q.selectPreference(key).executeAsOneOrNull()?.value_?.toIntOrNull()

    /** 内置 changelog 的最新版本号(无则 0)。[AI生成] */
    fun latestVersion(): Int = loadAll().maxOfOrNull { it.version } ?: 0

    /** 全部历史更新记录(倒序·供"更新记录中心")。[AI生成] */
    fun allChangelog(): List<SeedChangelogEntry> = loadAll()

    /**
     * 待告知的更新条目：version 在 (notified, applied] 内(倒序·跨多版本自动合并)。无则空。[AI生成]
     * applied 缺失(游标未初始化)→视为无(基线未建立·不弹)。notified 缺失→回退 applied(视为已对齐·不弹)。
     */
    fun pendingChangelog(): List<SeedChangelogEntry> {
        val applied = prefInt(PreferenceKeys.SEED_APPLIED_CHANGELOG_VERSION) ?: return emptyList()
        val notified = prefInt(PreferenceKeys.SEED_NOTIFIED_CHANGELOG_VERSION) ?: applied
        if (applied <= notified) return emptyList()
        return loadAll().filter { it.version in (notified + 1)..applied }
    }

    /** 是否有未告知的新版数据(驱动启动弹窗 / 我的页红点)。[AI生成] */
    fun hasUnnotified(): Boolean = pendingChangelog().isNotEmpty()

    /** 标记已告知到当前已应用版本(弹窗关闭 / 进更新记录中心后调用·幂等消红点)。[AI生成] */
    fun markNotified(nowEpochSeconds: Long) {
        val applied = prefInt(PreferenceKeys.SEED_APPLIED_CHANGELOG_VERSION) ?: return
        q.upsertPreference(PreferenceKeys.SEED_NOTIFIED_CHANGELOG_VERSION, applied.toString(), nowEpochSeconds)
    }
}
