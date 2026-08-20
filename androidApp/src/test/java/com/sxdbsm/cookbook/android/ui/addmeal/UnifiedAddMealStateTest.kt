package com.sxdbsm.cookbook.android.ui.addmeal

import org.junit.Assert.assertEquals
import org.junit.Test

/** UEN 页面壳状态回归测试，覆盖 T-UEN-01~03/06/07 的纯状态不变量。[AI生成] */
class UnifiedAddMealStateTest {
    @Test
    fun defaultStateIsSingleDayAi() {
        assertEquals(
            UnifiedAddMealUiState(MealRange.SINGLE_DAY, MealInputMethod.AI),
            UnifiedAddMealUiState(),
        )
    }

    @Test
    fun rangeChangePreservesMethod() {
        val state = UnifiedAddMealUiState(MealRange.SINGLE_DAY, MealInputMethod.MANUAL)
        assertEquals(
            UnifiedAddMealUiState(MealRange.PERIOD, MealInputMethod.MANUAL),
            state.reduce(UnifiedAddMealEvent.SelectRange(MealRange.PERIOD)),
        )
    }

    @Test
    fun methodChangePreservesRange() {
        val state = UnifiedAddMealUiState(MealRange.PERIOD, MealInputMethod.AI)
        assertEquals(
            UnifiedAddMealUiState(MealRange.PERIOD, MealInputMethod.MANUAL),
            state.reduce(UnifiedAddMealEvent.SelectMethod(MealInputMethod.MANUAL)),
        )
    }
}
