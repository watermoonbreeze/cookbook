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

class PreferenceRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries

    fun observeThemeMode(): Flow<ThemeMode> =
        q.selectPreference(PreferenceKeys.THEME_MODE).asFlow().mapToOneOrNull(Dispatchers.Default).map { row ->
            ThemeMode.fromCode(row?.value_)
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        q.upsertPreference(PreferenceKeys.THEME_MODE, mode.code, DateTime.nowEpochSeconds())
    }

    suspend fun get(key: String): String? = q.selectPreference(key).executeAsOneOrNull()?.value_
    suspend fun set(key: String, value: String) {
        q.upsertPreference(key, value, DateTime.nowEpochSeconds())
    }
}
