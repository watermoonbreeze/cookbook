package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.FoodGroup.Group
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : FoodGroupClassifyTest
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 锁 FoodGroup.classify 的脆弱"尾词/关键词"食材分类——跨类冲突红线 + 脏输入优雅退化
 * <p>
 * classify 是启发式 by-name 判定，历史多次踩坑(脱脂牛奶含"牛"被判红肉、鸡毛菜含"鸡"被判禽肉、
 * 鱼豆腐含"鱼"被判水产等)。现有测试只覆盖 nutritionLevel、零 classify 覆盖。本测试锁住这些
 * 靠"尾词优先 + 特例表 + 判定顺序"修好的红线，防后续改关键词表时回退。纯测试、零生产改动。
 * <p>
 * [AI生成] backlog 架构项:补 classify 跨类冲突&脏输入单测,锁脆弱 by-name 判定。
 **/
class FoodGroupClassifyTest {

    @Test
    fun `J12_今日还缺根因_蛋豆腐正确_常见蔬菜classify覆盖缺口`() {
        // 用户 07-23 吃:小黄瓜/白煮蛋/西兰花/胡萝卜/藕片/娃娃菜/豆腐,却提示缺优质蛋白+蔬菜。
        // 蛋/豆腐=优质蛋白源、娃娃菜=蔬菜:classify 正确,有正确主料时不误报缺(逻辑不是根因)。
        assertEquals(Group.EGG, FoodGroup.classify("白煮蛋"))
        assertEquals(Group.BEAN, FoodGroup.classify("豆腐"))
        assertEquals(Group.VEGETABLE, FoodGroup.classify("娃娃菜"))
        assertEquals(false, FoodGroup.nutritionGaps(FoodGroup.groupsOf(listOf("白煮蛋", "豆腐", "娃娃菜"))).contains("优质蛋白"))
        // 🔴 常见蔬菜若 classify 判不出→今日营养/色系墙误判"缺蔬菜"(尾词非"菜",走关键词也漏)。
        assertEquals(Group.VEGETABLE, FoodGroup.classify("西兰花"))
        assertEquals(Group.VEGETABLE, FoodGroup.classify("胡萝卜"))
        assertEquals(Group.VEGETABLE, FoodGroup.classify("莲藕"))
        assertEquals(Group.VEGETABLE, FoodGroup.classify("藕"))
    }

    @Test
    fun `尾词优先_前缀修饰词不误判`() {
        // 脱脂纯牛奶含"牛"→若按关键词会判红肉；尾词"奶"优先→DAIRY。
        assertEquals(Group.DAIRY, FoodGroup.classify("脱脂纯牛奶"))
        // 鸡毛菜含"鸡"→若按关键词判禽肉；尾词"菜"优先→蔬菜。
        assertEquals(Group.VEGETABLE, FoodGroup.classify("鸡毛菜"))
        // 咸鸭蛋含"鸭"→尾词"蛋"优先→蛋(非禽肉)。
        assertEquals(Group.EGG, FoodGroup.classify("咸鸭蛋"))
        // xxx肉:含鸡鸭鹅→禽,否则红肉。兔肉无禽字→红肉。
        assertEquals(Group.RED_MEAT, FoodGroup.classify("兔肉"))
        assertEquals(Group.WHITE_MEAT, FoodGroup.classify("鸡胸肉"))
    }

    @Test
    fun `血制品归红肉_不被禽字误判`() {
        // 鸭血含"鸭"→若按关键词判禽;血制品规则优先→红肉。
        assertEquals(Group.RED_MEAT, FoodGroup.classify("鸭血"))
        assertEquals(Group.RED_MEAT, FoodGroup.classify("猪血"))
    }

    @Test
    fun `菌藻优先于菜尾字`() {
        // 紫菜/海带以"菜/带"感但属菌藻,FUNGI 规则先于"菜"尾词。
        assertEquals(Group.FUNGI, FoodGroup.classify("紫菜"))
        assertEquals(Group.FUNGI, FoodGroup.classify("海带"))
        assertEquals(Group.FUNGI, FoodGroup.classify("黑木耳"))
        assertEquals(Group.FUNGI, FoodGroup.classify("猴头菇"))
    }

    @Test
    fun `腐归豆坚果_不被鱼字误判`() {
        // 鱼豆腐含"鱼"→若按关键词判水产;"腐"尾词优先→豆坚果。
        assertEquals(Group.BEAN, FoodGroup.classify("鱼豆腐"))
        assertEquals(Group.BEAN, FoodGroup.classify("臭豆腐"))
        assertEquals(Group.BEAN, FoodGroup.classify("豆腐"))
    }

    @Test
    fun `油不归九大类_油菜仍是蔬菜`() {
        // 各种油(花生油/橄榄油)不属营养大类→null;油菜以"菜"结尾→蔬菜(非油)。
        assertNull(FoodGroup.classify("花生油"))
        assertNull(FoodGroup.classify("橄榄油"))
        assertEquals(Group.VEGETABLE, FoodGroup.classify("油菜"))
    }

    @Test
    fun `水果优先于蔬菜_瓜类不混`() {
        // 木瓜/西瓜含"瓜"(蔬菜关键词)但属水果→FRUIT 先于 VEG;冬瓜/南瓜不在水果表→蔬菜。
        assertEquals(Group.FRUIT, FoodGroup.classify("木瓜"))
        assertEquals(Group.FRUIT, FoodGroup.classify("西瓜"))
        assertEquals(Group.FRUIT, FoodGroup.classify("哈密瓜"))
        assertEquals(Group.VEGETABLE, FoodGroup.classify("冬瓜"))
        assertEquals(Group.VEGETABLE, FoodGroup.classify("南瓜"))
    }

    @Test
    fun `特例表最优先`() {
        // NAME_OVERRIDE 里的具体食材(关键词判不准的)最先命中。
        assertEquals(Group.VEGETABLE, FoodGroup.classify("西红柿"))
        assertEquals(Group.STAPLE, FoodGroup.classify("凉皮"))
        assertEquals(Group.FRUIT, FoodGroup.classify("柠檬"))
        assertEquals(Group.RED_MEAT, FoodGroup.classify("肥肠"))
    }

    @Test
    fun `常规主食蔬菜水产肉判定`() {
        assertEquals(Group.STAPLE, FoodGroup.classify("米饭"))
        assertEquals(Group.VEGETABLE, FoodGroup.classify("青菜"))
        assertEquals(Group.FISH, FoodGroup.classify("带鱼"))
        assertEquals(Group.FISH, FoodGroup.classify("基围虾"))
        assertEquals(Group.RED_MEAT, FoodGroup.classify("五花肉"))
        assertEquals(Group.WHITE_MEAT, FoodGroup.classify("鸡翅"))
    }

    @Test
    fun `脏输入优雅退化为null不崩`() {
        // 空/纯符号/无法归类的名字→null(不崩、不误归)。
        assertNull(FoodGroup.classify(""))
        assertNull(FoodGroup.classify("   ")) // 纯空白
        assertNull(FoodGroup.classify("料理包")) // 复合调料不属九大类(注释明确)
        assertNull(FoodGroup.classify("沙拉酱"))
        assertNull(FoodGroup.classify("???"))
        assertNull(FoodGroup.classify("12345"))
    }

    @Test
    fun `J15_主料空则回退全食材_修问题菜漏判主食`() {
        // 07-24 真机复现:炒饭全部 is_main=0 → mainNames 恒空;全食材含糙米(主食)。
        // 只看主料时(空)→零大类→误报缺主食;回退全食材→糙米被识别→不缺主食。
        val mains = emptyList<String>()
        val all = listOf("糙米", "生菜", "胡萝卜", "鸡蛋", "花菜")
        val names = FoodGroup.classificationNames(mains, all)
        val groups = FoodGroup.groupsOf(names)
        assertTrue(Group.STAPLE in groups, "糙米应被识别为主食")
        assertFalse("主食" in FoodGroup.nutritionGaps(groups), "有糙米不该再误报缺主食")
    }

    @Test
    fun `J15_主料非空保持主料口径_辅料不充数`() {
        // well-labeled 菜(如洋葱炒牛肉:主料=洋葱牛肉)保持主料口径——
        // 青椒/淀粉/蚝油等非主料辅料不该被并入,避免"单一荤菜"被辅料充成更均衡。
        val mains = listOf("洋葱", "牛肉")
        val all = listOf("青椒", "洋葱", "牛肉", "生抽", "蚝油", "花生油", "淀粉")
        val names = FoodGroup.classificationNames(mains, all)
        assertEquals(mains, names, "主料非空时应原样返回主料、不并全食材")
    }

    @Test
    fun `J15_全天回退后不再误报缺主食_端到端`() {
        // 镜像 07-24 全天(早/午/晚)问题菜混正常菜:午餐大排饭全 is_main=0、晚餐炒饭糙米 is_main=0。
        // 用 (mainNames,allNames) 对逐菜回退后汇总,断言全天不缺主食(用户报的误报被修)。
        val dishes = listOf(
            // 早餐
            Pair(listOf("南瓜"), listOf("南瓜")),               // 蒸南瓜(主料非空)
            Pair(emptyList(), listOf("黄瓜")),                  // 小黄瓜(is_main=0)
            Pair(emptyList(), listOf("脱脂纯牛奶")),             // 牛奶(is_main=0)
            Pair(emptyList(), listOf("鸡蛋")),                  // 白煮蛋(is_main=0)
            // 午餐 大排饭:全 is_main=0,且无米食材
            Pair(emptyList(), listOf("油菜", "黄瓜", "盐", "生抽", "大排", "食用油", "卤蛋")),
            // 晚餐
            Pair(listOf("西红柿", "鸡蛋"), listOf("西红柿", "鸡蛋")),  // 番茄蛋汤
            Pair(listOf("洋葱", "牛肉"), listOf("青椒", "洋葱", "牛肉", "生抽", "蚝油", "花生油", "淀粉")), // 洋葱炒牛肉
            Pair(emptyList(), listOf("糙米", "生菜", "胡萝卜", "鸡蛋", "花菜")), // 炒饭:糙米 is_main=0
        )
        val names = dishes.flatMap { FoodGroup.classificationNames(it.first, it.second) }
        val groups = FoodGroup.groupsOf(names)
        assertTrue(Group.STAPLE in groups, "晚餐炒饭的糙米应让全天含主食")
        assertTrue(FoodGroup.nutritionGaps(groups).isEmpty(), "全天蛋白/主食/蔬菜齐,不该有还缺项")
    }

    @Test
    fun `groupsOf去重保枚举顺序_explicit优先`() {
        // 多主料归纳去重、按枚举顺序;explicit(食材显式大类)优先于关键词。
        val groups = FoodGroup.groupsOf(listOf("米饭", "青菜", "带鱼", "青菜"))
        assertEquals(listOf(Group.STAPLE, Group.VEGETABLE, Group.FISH), groups)
        // explicit 覆盖:把"神秘食材"显式标为水果,即便 classify 判不出也归水果。
        val withExplicit = FoodGroup.groupsOf(listOf("神秘食材"), explicit = mapOf("神秘食材" to Group.FRUIT))
        assertEquals(listOf(Group.FRUIT), withExplicit)
    }
}
