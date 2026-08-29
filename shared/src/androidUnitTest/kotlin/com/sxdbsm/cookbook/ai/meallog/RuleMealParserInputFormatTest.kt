package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 2026-08-29 输入格式统一回归测试：三种记录格式（冒号一行式 / 餐次行+菜品行 / 每行一菜）
 * + 菜品（食材1，食材2）括号食材逗号拆分 + "午餐: 午餐肉"二次剥前缀守卫。
 *
 * 格式规范（规则和 AI 通用，说明弹窗同源）：
 * - 格式1：`餐次: 菜品1，菜品2，菜品3`
 * - 格式2：`餐次\n菜品1，菜品2，菜品3`
 * - 格式3：`餐次\n菜品1\n菜品2\n菜品3`
 * - 菜品组合：`菜品（食材1，食材2）`，括号可省略
 */
class RuleMealParserInputFormatTest {

    private val today = LocalDate(2026, 8, 29)

    private fun dishNames(input: String): List<String> =
        RuleMealParser.parse(input, today = today)
            .flatMap { it.meals }
            .flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }

    private fun ingredientNames(input: String): List<String> =
        RuleMealParser.parse(input, today = today)
            .flatMap { it.meals }
            .flatMap { it.dishes }
            .flatMap { it.dish?.ingredients ?: emptyList() }
            .map { it.food?.name ?: it.ref.orEmpty() }
            .filter { it.isNotBlank() }

    // ═══════════════════════════════════════════════════
    // 格式1：餐次: 菜品1，菜品2，菜品3（冒号引导一行）
    // ═══════════════════════════════════════════════════

    @Test
    fun `格式1冒号引导一行式拆出全部菜品`() {
        val names = dishNames("晚餐: 红烧肉，米饭，炒青菜")
        assertEquals(listOf("红烧肉", "米饭", "炒青菜"), names)
        names.forEach { name ->
            assertTrue(!name.contains(":"), "菜名不应残留冒号，实际: $name")
        }
    }

    @Test
    fun `格式1全角冒号同样支持`() {
        val names = dishNames("晚餐：红烧肉，米饭")
        assertEquals(listOf("红烧肉", "米饭"), names)
    }

    // ═══════════════════════════════════════════════════
    // 格式2：餐次行 + 菜品行（逗号分隔）
    // ═══════════════════════════════════════════════════

    @Test
    fun `格式2餐次行加菜品逗号行`() {
        val names = dishNames("午餐\n番茄炒蛋，米饭，紫菜汤")
        assertEquals(listOf("番茄炒蛋", "米饭", "紫菜汤"), names)
    }

    // ═══════════════════════════════════════════════════
    // 格式3：餐次行 + 每行一菜（不被误判为多天）
    // ═══════════════════════════════════════════════════

    @Test
    fun `格式3每行一菜且不误判多天`() {
        val result = RuleMealParser.parse("晚餐\n清蒸鲈鱼\n蒜蓉西兰花\n米饭", today = today)
        assertEquals(1, result.size, "三个菜品行不应被切成多天，实际天数: ${result.size}")
        assertEquals(listOf("清蒸鲈鱼", "蒜蓉西兰花", "米饭"), dishNames("晚餐\n清蒸鲈鱼\n蒜蓉西兰花\n米饭"))
    }

    // ═══════════════════════════════════════════════════
    // 菜品（食材1，食材2）：括号食材逗号拆分（本次核心修复）
    // ═══════════════════════════════════════════════════

    @Test
    fun `括号内逗号分隔食材被拆开`() {
        val names = ingredientNames("西红柿炒鸡蛋（西红柿，鸡蛋）")
        // 不断言全集：菜名本身还会经 IngredientNameExtractor 额外推演食材（既有行为），
        // 本用例只锁定"括号内逗号分隔被拆开"这一件事。
        assertTrue("西红柿" in names && "鸡蛋" in names, "括号内逗号分隔的食材应各自成项，实际: $names")
        assertTrue(names.none { "," in it || "，" in it }, "不应出现名字带逗号的合并食材，实际: $names")
    }

    @Test
    fun `括号内顿号分隔食材同样拆开`() {
        val names = ingredientNames("凉皮（黄瓜丝、绿豆芽）")
        assertTrue("黄瓜丝" in names && "绿豆芽" in names, "顿号分隔食材应各自成项，实际: $names")
    }

    @Test
    fun `括号食材不污染菜名`() {
        val names = dishNames("午餐: 西红柿炒鸡蛋（西红柿，鸡蛋）")
        assertEquals(listOf("西红柿炒鸡蛋"), names, "括号内容应从菜名剥离")
    }

    @Test
    fun `菜名本身不算食材只有括号内的才算`() {
        // [AI修改] E-IFMT-05 真机反馈（用户 2026-08-29）："晚餐\n百页烧肉（百叶结，五花肉）\n米饭\n青菜"
        //   中百页烧肉的食材变成了[百页烧肉,百叶结,五花肉]——菜品本身不能算食材。修复后括号食材
        //   是权威声明，菜名推演不再混入（整名当库外候选的旧行为废除）。
        val names = ingredientNames("晚餐\n百页烧肉（百叶结，五花肉）\n米饭\n青菜")
        assertEquals(listOf("百叶结", "五花肉"), names)
        assertEquals(listOf("百页烧肉", "米饭", "青菜"), dishNames("晚餐\n百页烧肉（百叶结，五花肉）\n米饭\n青菜"))
    }

    @Test
    fun `无括号时菜名整名不进食材`() {
        // 无括号场景同口径：拆不动的菜名（如"百页烧肉"）整名不得作为食材。
        val names = ingredientNames("晚餐: 百页烧肉")
        assertTrue("百页烧肉" !in names, "菜名整名不应出现在食材里，实际: $names")
    }

    // ═══════════════════════════════════════════════════
    // 边角：二次剥前缀守卫
    // ═══════════════════════════════════════════════════

    @Test
    fun `午餐肉不被二次剥成肉`() {
        val names = dishNames("午餐: 午餐肉")
        assertEquals(listOf("午餐肉"), names, "'午餐: 午餐肉'的菜名应保留为'午餐肉'，不应剥成'肉'")
    }

    @Test
    fun `早餐饼不被二次剥成饼`() {
        val names = dishNames("早餐: 早餐饼")
        assertEquals(listOf("早餐饼"), names)
    }

    @Test
    fun `餐次词后带分隔符的单字菜名正常剥离`() {
        // 复审守卫反向边角："早餐 粥"这类分隔符+单字菜名必须照剥，餐次词不得残进菜名。
        assertEquals(listOf("粥"), dishNames("早餐 粥"))
        assertEquals(listOf("汤"), dishNames("早饭: 汤"))
    }

    @Test
    fun `括号内空格拆出的单字段不产出食材`() {
        // 复审："（少 盐）"按空格拆出的 1 字段不得成为食材（防污染食材字典）。
        val names = ingredientNames("午餐: 青菜（少 盐）")
        assertTrue(names.none { it.length < 2 }, "不应产出单字食材，实际: $names")
    }

    // ═══════════════════════════════════════════════════
    // 括号省略：普通菜名正常解析
    // ═══════════════════════════════════════════════════

    @Test
    fun `无括号普通菜名正常解析`() {
        val names = dishNames("晚餐: 红烧肉")
        assertEquals(listOf("红烧肉"), names)
    }
}
