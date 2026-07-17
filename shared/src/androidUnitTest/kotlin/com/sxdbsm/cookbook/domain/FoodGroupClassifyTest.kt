package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.FoodGroup.Group
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun `groupsOf去重保枚举顺序_explicit优先`() {
        // 多主料归纳去重、按枚举顺序;explicit(食材显式大类)优先于关键词。
        val groups = FoodGroup.groupsOf(listOf("米饭", "青菜", "带鱼", "青菜"))
        assertEquals(listOf(Group.STAPLE, Group.VEGETABLE, Group.FISH), groups)
        // explicit 覆盖:把"神秘食材"显式标为水果,即便 classify 判不出也归水果。
        val withExplicit = FoodGroup.groupsOf(listOf("神秘食材"), explicit = mapOf("神秘食材" to Group.FRUIT))
        assertEquals(listOf(Group.FRUIT), withExplicit)
    }
}
