package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.db.CookbookDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : MigrationV25Test
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : v25 迁移推演——ingredient_nutrition 加 saturated_fat_g/cholesterol_mg 两列(高血脂负向维度)
 * <p>
 * 单测走 Schema.create 不跑迁移链(红线)，故此处手工推演旧库(v24 形态)升级：
 * ①旧表形态(无两列)+插旧行 → 跑 25.sqm 的两条 ALTER → 断言两列存在、旧行读 NULL、可更新；
 * ②对比 Schema.create(最新 v25) 的列集合/顺序 与 旧表+ALTER 后的列集合/顺序**完全一致**
 *   (防"CREATE 与 ALTER 列序不一致→SQLDelight 迁移校验/真机初始化失败"红线)。
 * <p>
 * [AI生成] 高血脂 DB 迁移安全性验证。
 **/
class MigrationV25Test {

    /** v25 之前的 ingredient_nutrition 建表(与迁移前 CREATE TABLE 一致，去掉 FK 引用以便隔离建表)。[AI生成] */
    private val oldCreate = """
        CREATE TABLE ingredient_nutrition (
            ingredient_id INTEGER NOT NULL PRIMARY KEY,
            energy_kcal REAL,
            protein_g REAL,
            fat_g REAL,
            carb_g REAL,
            fiber_g REAL,
            sodium_mg REAL,
            potassium_mg REAL,
            calcium_mg REAL,
            gi REAL,
            purine_mg REAL,
            piece_gram REAL,
            ref TEXT NOT NULL DEFAULT '',
            review INTEGER NOT NULL DEFAULT 0,
            updated_at INTEGER NOT NULL DEFAULT 0,
            status INTEGER NOT NULL DEFAULT 1
        );
    """.trimIndent()

    // 25.sqm 的两条迁移语句(与迁移文件逐字一致)。
    private val alter1 = "ALTER TABLE ingredient_nutrition ADD COLUMN saturated_fat_g REAL;"
    private val alter2 = "ALTER TABLE ingredient_nutrition ADD COLUMN cholesterol_mg REAL;"

    /** 读取某表列名(按物理顺序)。[AI生成] */
    private fun columnsOf(driver: SqlDriver, table: String): List<String> =
        driver.executeQuery(null, "PRAGMA table_info($table)", { cursor ->
            val names = mutableListOf<String>()
            while (cursor.next().value) { names.add(cursor.getString(1)!!) }
            QueryResult.Value(names.toList())
        }, 0).value

    @Test
    fun `v25迁移_旧库升级不崩_两列可空_旧行为NULL`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, oldCreate, 0)
        // 插一条 v24 形态的旧行(无两列)。
        driver.execute(null, "INSERT INTO ingredient_nutrition(ingredient_id, energy_kcal) VALUES (1, 100.0);", 0)
        // 跑 25.sqm 两条 ALTER —— 不应抛异常(可空、无 DEFAULT，最安全形态)。
        driver.execute(null, alter1, 0)
        driver.execute(null, alter2, 0)

        // 旧行两列应为 NULL(评级器缺数据不触发的前提)。
        val nullCount = driver.executeQuery(null,
            "SELECT COUNT(*) FROM ingredient_nutrition WHERE ingredient_id=1 AND saturated_fat_g IS NULL AND cholesterol_mg IS NULL",
            { cursor -> cursor.next(); QueryResult.Value(cursor.getLong(0)!!) }, 0).value
        assertEquals(1L, nullCount, "升级后旧行两列应为 NULL")

        // 可写入新值(补齐式回填/用户填写路径)。
        driver.execute(null, "UPDATE ingredient_nutrition SET saturated_fat_g=10.4, cholesterol_mg=79.0 WHERE ingredient_id=1;", 0)
        val filled = driver.executeQuery(null,
            "SELECT COUNT(*) FROM ingredient_nutrition WHERE saturated_fat_g=10.4 AND cholesterol_mg=79.0",
            { cursor -> cursor.next(); QueryResult.Value(cursor.getLong(0)!!) }, 0).value
        assertEquals(1L, filled, "两列应可正常写入")
        driver.close()
    }

    @Test
    fun `v25迁移_CREATE与ALTER列集合顺序一致`() {
        // 最新 v25：Schema.create 建全库，取 ingredient_nutrition 列序。
        val fresh = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(fresh)
        val freshCols = columnsOf(fresh, "ingredient_nutrition")

        // 旧库(v24)+ 跑 25.sqm ALTER 后的列序。
        val migrated = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        migrated.execute(null, oldCreate, 0)
        migrated.execute(null, alter1, 0)
        migrated.execute(null, alter2, 0)
        val migratedCols = columnsOf(migrated, "ingredient_nutrition")

        assertEquals(freshCols, migratedCols, "CREATE(新装) 与 ALTER(升级) 后列名与顺序必须一致，否则迁移校验/真机初始化会失败")
        assertTrue("saturated_fat_g" in freshCols && "cholesterol_mg" in freshCols, "新列应在最新 schema 中")
        // 两列应追加在末尾(status 之后)。
        assertEquals(listOf("saturated_fat_g", "cholesterol_mg"), freshCols.takeLast(2), "两列应位于表末尾")
        fresh.close()
        migrated.close()
    }
}
