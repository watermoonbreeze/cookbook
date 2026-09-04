package com.sxdbsm.cookbook.data.seed

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.db.CookbookDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @File : ParseDictionarySeedTest
 * @Time : 2026/09/04
 * @Author : SXD-AI
 * @Desc : 解析字典 seed 单测（L4 分享链接·T-L4-08·真库）
 * <p>
 * 验证 parse_dictionary（分享来源→解析配置文件路由表）seed 后落库且 force 重跑幂等
 * （INSERT OR IGNORE + UNIQUE(source) 兜幂等·sqlite_3_18 无 UPSERT）。
 * <p>
 * 查询方式说明：parse_dictionary 在 Cookbook.sq 仅有 insertParseDictionary（seed 专用），
 * 无生成 select 查询；[RepositoryTestDatabase.create] 不暴露 driver（SQLDelight 2.0
 * TransacterImpl.driver 非 public），故按其内部同式自建 driver+内存库，用原始 SQL 断言行数。
 * <p>
 * [AI生成] 实施蓝图 `feature/分享链接解析_实施蓝图.md` §8 测试矩阵 shared 部分。
 **/
class ParseDictionarySeedTest {

    @Test
    fun `T-L4-08 parse_dictionary seed_幂等_force重跑不重复`() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        val db = CookbookDatabase(driver)
        val seeder = PresetDataSeeder(db)

        // seedIfNeeded → force → force：两次强制重跑
        seeder.seedIfNeeded()
        seeder.forceReseedBaseData()
        seeder.forceReseedBaseData()

        val sources = driver.executeQuery(
            null,
            "SELECT source FROM parse_dictionary",
            { cursor ->
                val list = mutableListOf<String>()
                while (cursor.next().value) {
                    list.add(cursor.getString(0)!!)
                }
                QueryResult.Value(list)
            },
            0,
        ).value
        assertEquals(1, sources.size, "两次 force 重跑后 parse_dictionary 应仍只有 1 行（UNIQUE(source) 幂等）")
        assertEquals("xiachufang", sources.single(), "唯一行应为下厨房来源")
        Unit
    }
}
