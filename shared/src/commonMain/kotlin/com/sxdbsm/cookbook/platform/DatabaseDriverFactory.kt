package com.sxdbsm.cookbook.platform

import app.cash.sqldelight.db.SqlDriver

/**
 * 跨平台数据库驱动工厂。Android 端使用 AndroidSqliteDriver，iOS 端使用 NativeSqliteDriver。
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
