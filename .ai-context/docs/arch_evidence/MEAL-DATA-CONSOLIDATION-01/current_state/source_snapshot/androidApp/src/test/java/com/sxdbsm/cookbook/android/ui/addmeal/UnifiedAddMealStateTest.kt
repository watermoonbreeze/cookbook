package com.sxdbsm.cookbook.android.ui.addmeal

import org.junit.Assert.assertEquals
import org.junit.Test
import com.sxdbsm.cookbook.android.ui.ai.AiMealPhase

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

    @Test
    fun aiDoneRequiresLeavingUnifiedEntry() {
        assertEquals(true, shouldLeaveAfterAiSave(AiMealPhase.DONE))
        assertEquals(false, shouldLeaveAfterAiSave(AiMealPhase.PREVIEW_READY))
    }

    @Test
    fun previewLocksRangeButAllowsMethodChange() {
        assertEquals(false, canChangeMealRange(AiMealPhase.PREVIEW_READY))
        assertEquals(true, canChangeMealMethod(AiMealPhase.PREVIEW_READY))
    }

    @Test
    fun generatingPartialAndSavingLockBothSelectors() {
        listOf(AiMealPhase.GENERATING, AiMealPhase.PARTIAL_READY, AiMealPhase.SAVING).forEach { phase ->
            assertEquals(false, canChangeMealRange(phase))
            assertEquals(false, canChangeMealMethod(phase))
        }
    }

    @Test
    fun errorLocksRangeButAllowsMethodChange() {
        assertEquals(false, canChangeMealRange(AiMealPhase.ERROR))
        assertEquals(true, canChangeMealMethod(AiMealPhase.ERROR))
    }
}
