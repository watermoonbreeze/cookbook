package com.sxdbsm.cookbook.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sxdbsm.cookbook.db.CookbookDatabase

/**
 * Android 端 SQLDelight 驱动工厂。[AI修改]
 *
 * `actual class` 是 commonMain 中 `expect class DatabaseDriverFactory` 的 Android 实现。
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = CookbookDatabase.Schema,
            context = context,
            name = DB_NAME,
        )

    companion object {
        const val DB_NAME = "cookbook.db"
    }
}
