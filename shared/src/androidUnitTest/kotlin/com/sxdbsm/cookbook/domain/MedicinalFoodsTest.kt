package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : MedicinalFoodsTest
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 药食同源白名单——命中官方项/别名，非白名单不误标
 * <p>
 * [AI生成] 守：只精确名/别名匹配，避免把普通食材误标药食同源(事实标签不能错)。
 **/
class MedicinalFoodsTest {

    @Test
    fun `命中官方药食同源与别名`() {
        assertTrue(MedicinalFoods.isMedicinal("山药"), "山药是2002批")
        assertTrue(MedicinalFoods.isMedicinal("生姜"), "生姜(姜)在目录")
        assertTrue(MedicinalFoods.isMedicinal("枸杞"), "别名枸杞→枸杞子")
        assertTrue(MedicinalFoods.isMedicinal("红枣"), "别名红枣→大枣")
        assertTrue(MedicinalFoods.isMedicinal(" 桂圆 "), "别名桂圆→龙眼肉，且容首尾空格")
    }

    @Test
    fun `普通食材不误标`() {
        assertFalse(MedicinalFoods.isMedicinal("猪肉"))
        assertFalse(MedicinalFoods.isMedicinal("白菜"))
        assertFalse(MedicinalFoods.isMedicinal(""))
    }

    @Test
    fun `菜含药食同源计数`() {
        assertTrue(MedicinalFoods.anyIn(listOf("排骨", "山药", "枸杞")))
        assertTrue(MedicinalFoods.countIn(listOf("山药", "枸杞", "猪肉")) == 2)
        assertFalse(MedicinalFoods.anyIn(listOf("猪肉", "白菜")))
    }
}
