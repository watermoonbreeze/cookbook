package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.db.CookbookDatabase

/**
 * @File : RepositoryTestDatabase
 * @Time : 2026/06/05
 * @Author : SXD-AI
 * @Desc : Repository 单元测试数据库工具
 * <p>
 * 使用 SQLDelight JVM 内存数据库创建干净的 CookbookDatabase，避免测试依赖 Android 真机或本地文件。
 * <p>
 * [AI生成] 为 shared 层 Repository 单元测试提供统一数据库创建入口。
 **/
internal object RepositoryTestDatabase {
    /**
     * 创建一份独立的内存数据库。[AI生成]
     *
     * 每个测试单独调用，保证数据互不影响。
     */
    fun create(): CookbookDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        return CookbookDatabase(driver)
    }
}
