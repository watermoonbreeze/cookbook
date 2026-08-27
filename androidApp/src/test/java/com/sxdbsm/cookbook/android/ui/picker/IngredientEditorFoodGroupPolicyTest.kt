package com.sxdbsm.cookbook.android.ui.picker

import com.sxdbsm.cookbook.domain.FoodGroup.Group
import org.junit.Assert.assertEquals
import org.junit.Test

/** [AI生成] Bug-5305：锁定编辑器手选营养大类高于后续自动候选的合同。 */
class IngredientEditorFoodGroupPolicyTest {
    @Test
    fun `T-5305-01 untouched editor accepts automatic candidate`() {
        assertEquals(
            Group.VEGETABLE,
            resolveIngredientEditorFoodGroup(Group.VEGETABLE, null, groupTouched = false),
        )
    }

    @Test
    fun `T-5305-02 name change cannot replace touched group`() {
        assertEquals(
            Group.EGG,
            resolveIngredientEditorFoodGroup(Group.VEGETABLE, Group.EGG, groupTouched = true),
        )
    }

    @Test
    fun `T-5305-03 options reload cannot replace touched group`() {
        assertEquals(
            Group.DAIRY,
            resolveIngredientEditorFoodGroup(Group.RED_MEAT, Group.DAIRY, groupTouched = true),
        )
    }

    @Test
    fun `T-5305-04 reset unlocks automatic candidate`() {
        assertEquals(
            Group.FRUIT,
            resolveIngredientEditorFoodGroup(Group.FRUIT, null, groupTouched = false),
        )
    }
}
