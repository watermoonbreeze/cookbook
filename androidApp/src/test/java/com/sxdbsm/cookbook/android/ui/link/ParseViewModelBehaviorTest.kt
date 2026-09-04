package com.sxdbsm.cookbook.android.ui.link

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.android.link.RemoteImageSaver
import com.sxdbsm.cookbook.android.link.WebViewTextExtractor
import com.sxdbsm.cookbook.data.parser.RecipeParser
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.ShareLinkRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * @File : ParseViewModelBehaviorTest
 * @Time : 2026/09/04
 * @Author : SXD-AI
 * @Desc : 链接解析 VM 行为级单测（终审 R-2 断言·2026-09-04）
 * <p>
 * 核心断言：**同域非菜谱页（解析出空食材+空步骤）必须判 FAILED 并 markFailed**——
 * 防"0 食材 0 步骤的空菜"经 SUCCESS + parse_state=1（状态机只前进）被存进菜品库。
 * 走 rebuildFrom 本地重建流（不联网不碰 WebView/下载，纯 JVM 可测）。
 * <p>
 * [AI生成] L4-P1 终审阻断修复配套。
 **/
@OptIn(ExperimentalCoroutinesApi::class)
class ParseViewModelBehaviorTest {

    private lateinit var db: CookbookDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var linkRepo: ShareLinkRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        db = CookbookDatabase(driver)
        linkRepo = ShareLinkRepository(db)
    }

    @After
    fun tearDown() {
        driver.close()
        Dispatchers.resetMain()
    }

    /** 构造可纯 JVM 组装的 VM：saver 的 connectionFactory 恒 404（本测试不触发下载·仅兜底装配）。[AI生成] */
    private fun createVm(): ParseViewModel {
        val context = android.app.Application()
        val saver = RemoteImageSaver(
            context = context,
            connectionFactory = { url ->
                (URL(url).openConnection() as HttpURLConnection).apply { /* 不会被调用：测试行无图 */ }
            },
            dirProvider = { File(System.getProperty("java.io.tmpdir"), "l4-parse-vm-test").apply { mkdirs() } },
        )
        return ParseViewModel(
            linkId = 1L,
            sourceName = "下厨房",
            linkRepo = linkRepo,
            parser = RecipeParser(),
            extractor = WebViewTextExtractor(context),
            saver = saver,
            dishRepo = DishRepository(db),
            ingredientRepo = IngredientRepository(db),
        )
    }

    @Test
    fun `终审R-2 非菜谱页空结果判FAILED并markFailed`() = runBlocking {
        // 行内 clearText 是"没有用料区/步骤区"的普通页面文本（如下厨房首页/分类页 innerText）。
        val id = linkRepo.insert("xiachufang", "https://m.xiachufang.com/", "")
        linkRepo.applyParseResult(id, "下厨房", "", "随便一段不是菜谱的页面文本\n没有用料区\n没有步骤区", "")
        val row = linkRepo.getById(id)!!

        val vm = createVm()
        vm.rebuildFrom(row)
        // rebuildFrom 走 Dispatchers.IO 异步（真实线程·非测试调度器）——runBlocking 阻塞等 state 离开 LOADING。
        vm.state.first { it.phase != ParsePhase.LOADING }

        // 空食材+空步骤 → FAILED（不进 SUCCESS，不给存空菜）
        assertEquals(ParsePhase.FAILED, vm.state.value.phase)
        // 失败收口：行状态只前进到 2
        assertEquals(2L, linkRepo.getById(id)!!.parseState)
        Unit
    }

    @Test
    fun `终审R-2反弹 菜谱页正常重建进SUCCESS`() = runBlocking {
        // 反向护栏：正常菜谱文本（用料+步骤俱全）不受空结果防御误伤。
        val text = """
            用料
            冬瓜
            500克
            xx汤的做法步骤
            步骤 1
            切块煮开
            菜谱创建时间：2026-01-01 00:00:00
        """.trimIndent()
        val id = linkRepo.insert("xiachufang", "https://m.xiachufang.com/recipe/1/", "")
        linkRepo.applyParseResult(id, "xx汤", "", text, "")
        val row = linkRepo.getById(id)!!

        val vm = createVm()
        vm.rebuildFrom(row)
        vm.state.first { it.phase != ParsePhase.LOADING }

        assertEquals(ParsePhase.SUCCESS, vm.state.value.phase)
        assertTrue("菜谱页应解析出食材", vm.state.value.recipe!!.ingredients.isNotEmpty())
        Unit
    }
}
