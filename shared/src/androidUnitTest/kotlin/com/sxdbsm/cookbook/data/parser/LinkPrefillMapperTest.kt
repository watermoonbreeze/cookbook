package com.sxdbsm.cookbook.data.parser

import com.sxdbsm.cookbook.domain.model.ParsedIngredient
import com.sxdbsm.cookbook.domain.model.ParsedRecipe
import com.sxdbsm.cookbook.domain.model.ParsedStep
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : LinkPrefillMapperTest
 * @Time : 2026/09/04
 * @Author : SXD-AI
 * @Desc : 链接导入预填映射单测（L4 分享链接·T-L4-09/15/16/17）
 * <p>
 * 覆盖：mapToPrefill 单位映射表（克/斤/毫升/个/勺/半勺/适量红牌）+ 占位负 id + 图片本地文件名映射
 * + source/linkId 透传；extractPageFromJson 双层 JSON 解码；横幅四态；URL 提取与尾随标点剥离。
 * <p>
 * [AI生成] 实施蓝图 `feature/分享链接解析_实施蓝图.md` §8 测试矩阵 shared 部分。
 **/
class LinkPrefillMapperTest {

    @Test
    fun `T-L4-09 mapToPrefill_单位映射_占位负id_图片文件名_source标记`() {
        // T-L4-09 手搓 ParsedRecipe（禁手搓的只有 config）→映射断言。
        val recipe = ParsedRecipe(
            title = "冬瓜丸子汤",
            ingredients = listOf(
                ParsedIngredient("冬瓜", 500.0, "克", true),
                ParsedIngredient("五花肉", 2.0, "斤", true),
                ParsedIngredient("清水", 500.0, "毫升", false),
                ParsedIngredient("鸡蛋", 1.0, "个", false),
                ParsedIngredient("生抽", 1.0, "勺", false),
                ParsedIngredient("蚝油", 0.0, "半勺", false),
                ParsedIngredient("盐", 0.0, "适量", false),
            ),
            steps = listOf(ParsedStep("切好", "https://img.example.com/s1.jpg")),
            cookingMethods = listOf("煮"),
            note = "碎碎念",
            imageUrl = "https://img.example.com/cover.jpg",
        )
        var seq = -100L
        val draft = LinkPrefillMapper.mapToPrefill(
            recipe = recipe,
            existingIdsByName = mapOf("冬瓜" to 101L),
            pendingSeq = { seq-- }, // 递减发号器：库外名占位负 id
            unitIdsByName = mapOf("g" to 5L, "个" to 7L),
            unitNamesByName = mapOf("g" to "g", "个" to "个"),
            // 合同偏差说明：合同字面 lambda `if (it=="生抽") 10 else 100` 会给蚝油 100→半勺=50，
            // 与合同自己的期望值（半勺=半折→5.0）矛盾；生产 SeasoningDefaults KEYWORD_GRAMS 明确
            // 蚝油→10（调料）。按生产口径收全：生抽/蚝油均 10，其余普通 100。
            defaultGramFor = { if (it == "生抽" || it == "蚝油") 10 else 100 },
            images = mapOf(
                "https://img.example.com/s1.jpg" to LinkPrefillMapper.StoredImageRef("s1.jpg", "s1_t.jpg"),
                "https://img.example.com/cover.jpg" to LinkPrefillMapper.StoredImageRef("c.jpg", "c_t.jpg"),
            ),
            sourceTag = "link",
            linkId = 42L,
        )
        assertEquals(7, draft.ingredients.size)
        val byName = draft.ingredients.associateBy { it.name }

        // 冬瓜：库内命中 101 + 克直传 + g 单位 + 主料
        val melon = byName.getValue("冬瓜")
        assertEquals(101L, melon.ingredientId)
        assertEquals(500.0, melon.quantity)
        assertEquals(5L, melon.unitId)
        assertEquals("g", melon.unitName)
        assertTrue(melon.isMain, "冬瓜应为主料")

        // 五花肉：库外→占位负 id；斤×500 折克；主料
        val pork = byName.getValue("五花肉")
        assertEquals(1000.0, pork.quantity, "斤×500 折克")
        assertEquals("g", pork.unitName)
        assertTrue(pork.ingredientId < 0, "库外名应发占位负 id")
        assertTrue(pork.isMain, "五花肉应为主料")

        // 清水：毫升≈1:1 折克
        val water = byName.getValue("清水")
        assertEquals(500.0, water.quantity, "毫升≈1:1")
        assertEquals("g", water.unitName)
        assertTrue(water.ingredientId < 0)

        // 鸡蛋：计件单位按名命中字典，不折克
        val egg = byName.getValue("鸡蛋")
        assertEquals(1.0, egg.quantity)
        assertEquals(7L, egg.unitId, "计件单位应命中字典 id=7")
        assertEquals("个", egg.unitName)
        assertFalse(egg.isMain, "isMain 两正一负：鸡蛋应为 false")

        // 生抽：勺走 defaultGramFor(生抽)=10，落 g 单位
        val soy = byName.getValue("生抽")
        assertEquals(10.0, soy.quantity, "勺应按 defaultGramFor(生抽)=10 落克")
        assertEquals(5L, soy.unitId)

        // 蚝油：半勺=半折 defaultGramFor(蚝油)10×0.5=5
        val oyster = byName.getValue("蚝油")
        assertEquals(5.0, oyster.quantity, "半勺应半折为 5g")
        assertTrue(oyster.ingredientId < 0)

        // 盐：适量→红牌 quantity=null（编辑器显式提示补数值），单位仍落 g
        val salt = byName.getValue("盐")
        assertNull(salt.quantity, "适量应红牌 quantity=null")
        assertEquals("g", salt.unitName)
        assertTrue(salt.ingredientId < 0)

        // source/linkId 透传
        assertEquals("link", draft.sourceTag)
        assertEquals(42L, draft.linkId)

        // 步骤图：映射为本地单文件名本身（无分隔符）
        assertEquals("切好", draft.steps[0].text)
        assertEquals("s1.jpg", draft.steps[0].imageFile)
        assertEquals("s1_t.jpg", draft.steps[0].thumbFile)
        // 封面：imageUrl 命中 images 映射
        assertEquals("c.jpg", draft.coverImage)
        assertEquals("c_t.jpg", draft.coverThumb)
        // 描述与烹饪方式透传
        assertEquals("碎碎念", draft.description)
        assertEquals(listOf("煮"), draft.cookingMethodNames)
    }

    @Test
    fun `T-L4-15 extractPageFromJson_双层解码与空值`() {
        // T-L4-15 双层 JSON 解码：中文+emoji+换行还原+stepImages；剥层失败/空串/"null"→null。
        val page = ExtractedPage(
            title = "冬瓜丸子汤➕",
            author = "吃喝玩乐MISSJ",
            imageUrl = "https://img.example.com/cover.jpg",
            stepImages = listOf("https://img.example.com/s1.jpg", "https://img.example.com/s2.jpg"),
            innerText = "用料\n冬瓜\n500克\n步骤 1\n切块➕下锅",
        )
        val inner = Json.encodeToString(page) // 第一层：对象→JSON 文本
        val wrapped = Json.encodeToString(inner) // 第二层：JSON 文本→字符串字面量（模拟 evaluateJavascript 回调）
        val decoded = extractPageFromJson(wrapped)
        assertNotNull(decoded, "双层 JSON 应解出 ExtractedPage")
        assertEquals(page.title, decoded.title)
        assertEquals(2, decoded.stepImages.size, "stepImages 应还原 2 个 URL")
        assertTrue(decoded.innerText.contains("\n"), "innerText 换行应由第二层解码还原")
        assertEquals(page, decoded, "双层解码后应与原快照全等")
        // 空值边界
        assertNull(extractPageFromJson(null))
        assertNull(extractPageFromJson(""))
        assertNull(extractPageFromJson("null"))
    }

    @Test
    fun `终审S-3 勺类数量参与折算_2勺等于双倍`() {
        // [AI生成] 2026-09-04 终审 S-3 配套：勺类不得丢弃数量（「生抽 2勺」=2×每勺克数；1 勺/半勺口径回归见 T-L4-09）。
        val recipe = ParsedRecipe(
            title = "t",
            ingredients = listOf(ParsedIngredient("生抽", 2.0, "勺", false)),
            steps = emptyList(),
            cookingMethods = emptyList(),
        )
        val draft = LinkPrefillMapper.mapToPrefill(
            recipe = recipe,
            existingIdsByName = emptyMap(),
            pendingSeq = { -1L },
            unitIdsByName = mapOf("g" to 5L),
            unitNamesByName = mapOf("g" to "g"),
            defaultGramFor = { 10 },
            images = emptyMap(),
        )
        assertEquals(20.0, draft.ingredients.single().quantity)
        Unit
    }

    @Test
    fun `T-L4-16 shouldShowLinkBanner_四态`() {
        // T-L4-16 四态：待解析+未关+非当天→显；当天已显/永久关/无待解析→不显。
        assertTrue(
            LinkPrefillMapper.shouldShowLinkBanner(2, false, "2026-09-01", "2026-09-04"),
            "有待解析+未永久关+当天未显→应显示",
        )
        assertFalse(
            LinkPrefillMapper.shouldShowLinkBanner(2, false, "2026-09-04", "2026-09-04"),
            "当天已显过→不再显示",
        )
        assertFalse(
            LinkPrefillMapper.shouldShowLinkBanner(2, true, "2026-09-01", "2026-09-04"),
            "永久关→不显示",
        )
        assertFalse(
            LinkPrefillMapper.shouldShowLinkBanner(0, false, "", "2026-09-04"),
            "无待解析→不显示",
        )
    }

    @Test
    fun `T-L4-17 extractUrl_提取_尾随标点剥离_无链接`() {
        // T-L4-17 URL 提取：整串/混在文本中/尾随中文句号剥离/无链接。
        assertEquals(
            "https://m.xiachufang.com/recipe/106889995/",
            LinkPrefillMapper.extractUrl("https://m.xiachufang.com/recipe/106889995/"),
        )
        assertEquals(
            "https://m.xiachufang.com/recipe/1/",
            LinkPrefillMapper.extractUrl("看看这个 https://m.xiachufang.com/recipe/1/ 好吃"),
        )
        assertEquals("https://x.com/a", LinkPrefillMapper.extractUrl("https://x.com/a。"), "尾随句号应剥离")
        assertNull(LinkPrefillMapper.extractUrl("没有链接"))
    }
}
