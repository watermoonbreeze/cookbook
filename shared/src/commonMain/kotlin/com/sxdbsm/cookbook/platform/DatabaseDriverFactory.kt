package com.sxdbsm.cookbook.platform

import app.cash.sqldelight.db.SqlDriver

/**
 * 跨平台数据库驱动工厂。[AI修改]
 *
 * 这里用 KMP 的 `expect/actual` 机制隐藏平台差异：
 * commonMain 只知道会拿到 `SqlDriver`，Android 端使用 AndroidSqliteDriver，
 * iOS 端可使用 NativeSqliteDriver。
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
