package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : AttributeGuesserTest
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 自建食材属性推断（AttributeGuesser）单测：命中/排除词/不误判守卫。
 * [AI生成] L2 智能推断守卫，防关键词误判（酒酿/鱼肝油）与漏判。
 **/
class AttributeGuesserTest {

    @Test
    fun `酒类推断为含酒精`() {
        assertTrue(FoodAttribute.CONTAINS_ALCOHOL in AttributeGuesser.guess("啤酒"))
        assertTrue(FoodAttribute.CONTAINS_ALCOHOL in AttributeGuesser.guess("自酿米酒"))
    }

    @Test
    fun `酒酿与醋排除不误判酒精`() {
        assertEquals(emptyList(), AttributeGuesser.guess("酒酿"))
        assertEquals(emptyList(), AttributeGuesser.guess("苹果醋"))
    }

    @Test
    fun `含糖饮料推断为加工果糖`() {
        assertTrue(FoodAttribute.PROCESSED_FRUCTOSE in AttributeGuesser.guess("珍珠奶茶"))
        assertTrue(FoodAttribute.PROCESSED_FRUCTOSE in AttributeGuesser.guess("可乐"))
    }

    @Test
    fun `植脂末推断为反式脂肪`() {
        assertTrue(FoodAttribute.TRANS_FAT in AttributeGuesser.guess("植脂末"))
        assertTrue(FoodAttribute.TRANS_FAT in AttributeGuesser.guess("咖啡伴侣"))
    }

    @Test
    fun `内脏推断但鱼肝油排除`() {
        assertTrue(FoodAttribute.ORGAN_HIGH_CHOLESTEROL in AttributeGuesser.guess("猪肝"))
        assertEquals(emptyList(), AttributeGuesser.guess("鱼肝油"))
    }

    @Test
    fun `加工肉腌制油炸浓汤正确推断`() {
        assertTrue(FoodAttribute.CURED_PROCESSED_MEAT in AttributeGuesser.guess("培根"))
        assertTrue(FoodAttribute.PICKLED_HIGH_SALT in AttributeGuesser.guess("榨菜"))
        assertTrue(FoodAttribute.DEEP_FRIED in AttributeGuesser.guess("炸鸡"))
        assertTrue(FoodAttribute.RICH_BROTH in AttributeGuesser.guess("排骨汤"))
    }

    @Test
    fun `普通食材不误判为任何属性`() {
        assertEquals(emptyList(), AttributeGuesser.guess("西红柿"))
        assertEquals(emptyList(), AttributeGuesser.guess("鸡胸肉"))
        assertEquals(emptyList(), AttributeGuesser.guess(""))
    }
}
