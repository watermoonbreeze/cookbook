package com.sxdbsm.cookbook.data.parser

import com.sxdbsm.cookbook.data.seed.SeedResourceLoader
import com.sxdbsm.cookbook.domain.model.ParseConfig
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * @File : RecipeParserTest
 * @Time : 2026/09/04
 * @Author : SXD-AI
 * @Desc : 菜谱纯文本解析器单测（L4 分享链接·T-L4-01/02/03/04/05/06）
 * <p>
 * 夹具口径（冻结·禁手搓）：config 与基准样本都走生产同路径 [SeedResourceLoader] 加载，
 * 加载失败直接 fail（测试不允许静默跳过）；基准样本覆盖真实下厨房页面结构
 * （含括号说明/半勺/适量/步骤图/创建时间行/推荐区噪音）。
 * <p>
 * [AI生成] 实施蓝图 `feature/分享链接解析_实施蓝图.md` §8 测试矩阵 shared 部分。
 **/
class RecipeParserTest {

    private lateinit var config: ParseConfig
    private lateinit var page: ExtractedPage
    private val parser = RecipeParser()

    @BeforeTest
    fun setUp() {
        val json = Json { ignoreUnknownKeys = true }
        val configText = SeedResourceLoader.readText("parsers/xiachufang.json")
        val baselineText = SeedResourceLoader.readText("parsers/baseline_xiachufang_106889995.txt")
        if (configText == null || baselineText == null) {
            fail("夹具加载失败：config=${configText != null} baseline=${baselineText != null}")
        }
        config = json.decodeFromString<ParseConfig>(configText)
        page = json.decodeFromString<ExtractedPage>(baselineText)
    }

    @Test
    fun `T-L4-01 基准样本主断言_标题_用料_封面`() {
        // T-L4-01 基准样本（下厨房冬瓜丸子汤）主断言。
        val r = parser.parse(page, config, listOf("炒", "煮", "蒸"))
        assertEquals("巨鲜美的冬瓜丸子汤", r.title)
        assertEquals(7, r.ingredients.size, "用料区应解析出 7 项")
        // [0] 冬瓜 500克 主料
        assertEquals("冬瓜", r.ingredients[0].name)
        assertEquals(500.0, r.ingredients[0].quantity)
        assertEquals("克", r.ingredients[0].unit)
        assertTrue(r.ingredients[0].isMain, "500g 主料应判 isMain")
        // [1] 猪肉馅（括号说明清洗掉）300g 主料
        assertEquals("猪肉馅", r.ingredients[1].name)
        assertEquals(300.0, r.ingredients[1].quantity)
        assertEquals("g", r.ingredients[1].unit)
        assertTrue(r.ingredients[1].isMain, "300g 主料应判 isMain")
        // [6] 盐 适量（无数字→0.0·整行作单位）
        assertEquals("盐", r.ingredients[6].name)
        assertEquals(0.0, r.ingredients[6].quantity)
        assertEquals("适量", r.ingredients[6].unit)
        // 封面 = og:image 快照值透传
        assertEquals(page.imageUrl, r.imageUrl)
    }

    @Test
    fun `T-L4-02 标题兜底_title为空时fallbackTitle命中`() {
        // T-L4-02 标题兜底：作者区过滤+噪音过滤+用料前最后短行→cleanPattern 提取。
        val r = parser.parse(page.copy(title = ""), config, listOf("炒", "煮", "蒸"))
        assertEquals("巨鲜美的冬瓜丸子汤", r.title)
    }

    @Test
    fun `T-L4-03 步骤锚定截断_创建时间与推荐区不入步骤`() {
        // T-L4-03 步骤锚定截断：以"菜谱创建时间"收口，推荐区(肉末炒冬瓜/评分)不混入。
        val r = parser.parse(page, config, listOf("炒", "煮", "蒸"))
        assertEquals(5, r.steps.size)
        assertTrue(r.steps[4].text.contains("男朋友"), "第 5 步应含“男朋友”，实得：${r.steps[4].text}")
        r.steps.forEachIndexed { i, s ->
            assertFalse(s.text.contains("肉末炒冬瓜"), "步骤${i + 1} 不应混入推荐区菜名")
            assertFalse(s.text.contains("评分"), "步骤${i + 1} 不应混入评分行")
            assertFalse(s.text.contains("菜谱创建时间"), "步骤${i + 1} 不应混入创建时间行")
        }
    }

    @Test
    fun `T-L4-04 步骤图对位_缺图补空_多余图丢弃`() {
        // T-L4-04 步骤图对位：①基准 5 图逐位对上。
        val full = parser.parse(page, config, listOf("炒", "煮", "蒸"))
        assertEquals(5, full.steps.size)
        full.steps.forEachIndexed { i, s ->
            assertEquals(page.stepImages[i], s.imageUrl, "步骤${i + 1} 应与第 ${i + 1} 张步骤图对位")
        }
        // ②图少于步骤→缺位补空串、有图位正常。
        val fewer = parser.parse(page.copy(stepImages = page.stepImages.take(2)), config, listOf("炒", "煮", "蒸"))
        assertEquals(page.stepImages[0], fewer.steps[0].imageUrl)
        assertEquals(page.stepImages[1], fewer.steps[1].imageUrl)
        assertEquals("", fewer.steps[2].imageUrl, "第 3 步缺图应补空串")
        // ③图多于步骤→多余静默丢弃、无越界。
        val more = parser.parse(page.copy(stepImages = page.stepImages + listOf("extra")), config, listOf("炒", "煮", "蒸"))
        assertEquals(5, more.steps.size)
    }

    @Test
    fun `T-L4-05 烹饪方式双源_按传入字典序`() {
        // T-L4-05 ①基准样本：菜名无方式字、步骤全文含“煮”→与传入字典交集=[煮]。
        val soup = parser.parse(page, config, listOf("炒", "煮", "蒸"))
        assertEquals(listOf("煮"), soup.cookingMethods)
        // ②合成页：菜名含“炒”+步骤含“煮/炒”→双命中，输出按传入字典序（乱序字典验证）。
        val stirFry = parser.parse(
            page.copy(
                title = "炒时蔬",
                innerText = "用料\n西红柿\n500克\n炒时蔬的做法步骤\n步骤 1\n大火煮开再翻炒均匀即可\n菜谱创建时间：2026-01-01 00:00:00",
            ),
            config,
            listOf("煮", "炒"),
        )
        assertEquals(listOf("煮", "炒"), stirFry.cookingMethods)
        // ③空字典→空列表。
        assertEquals(emptyList(), parser.parse(page, config, emptyList()).cookingMethods)
    }

    @Test
    fun `T-L4-06 量行解析_数字_半勺_适量_斤`() {
        // T-L4-06 量行解析（经 parse 间接测·私有函数不直测）：数字行/半 前缀/适量/斤。
        val r = parser.parse(
            ExtractedPage(
                title = "xx",
                innerText = "用料\n土豆\n500克\n虾\n半勺\n葱\n适量\n蒜\n2斤\nxx的做法步骤\n步骤 1\n切好\n菜谱创建时间：x",
            ),
            config,
            listOf("煮"),
        )
        assertEquals(4, r.ingredients.size)
        // [0] 土豆 500克
        assertEquals("土豆", r.ingredients[0].name)
        assertEquals(500.0, r.ingredients[0].quantity)
        assertEquals("克", r.ingredients[0].unit)
        // [1] 虾 半勺（“半”前缀合法量行·数值 0、整行作单位）
        assertEquals("虾", r.ingredients[1].name)
        assertEquals(0.0, r.ingredients[1].quantity)
        assertEquals("半勺", r.ingredients[1].unit)
        // [2] 葱 适量
        assertEquals("葱", r.ingredients[2].name)
        assertEquals(0.0, r.ingredients[2].quantity)
        assertEquals("适量", r.ingredients[2].unit)
        // [3] 蒜 2斤
        assertEquals("蒜", r.ingredients[3].name)
        assertEquals(2.0, r.ingredients[3].quantity)
        assertEquals("斤", r.ingredients[3].unit)
    }

    /**
     * 终审 R-1：作者行与用料行**直接相邻**（作者值行缺失·匿名/注销作者/渲染差异）时 parse 不崩。[AI生成]
     * 根因=extractNote 的 subList(startIdx+2, endIdx) 在 from>endIdx 时抛 IllegalArgumentException；
     * 修复=先判界返回空 note。此用例锁定"任意页面文本都不能让解析器崩溃"（R-1 纵深+空结果防御的纯函数面）。
     */
    @Test
    fun `终审R-1 作者行与用料行相邻不崩`() {
        val r = parser.parse(
            ExtractedPage(
                title = "冬瓜汤",
                innerText = "冬瓜汤\n作者：\n用料\n土豆\n500克\n冬瓜汤的做法步骤\n步骤 1\n切好\n菜谱创建时间：x",
            ),
            config,
            listOf("煮"),
        )
        assertEquals(1, r.ingredients.size, "不崩且正常解析出食材")
        assertEquals("土豆", r.ingredients[0].name)
        assertEquals("", r.note) // 作者值与用料直接相邻，无 note 可提取
        assertEquals(1, r.steps.size)
    }
}
