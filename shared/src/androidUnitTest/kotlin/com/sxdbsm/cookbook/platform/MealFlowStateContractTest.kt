package com.sxdbsm.cookbook.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MealFlowStateContractTest {
    @Test
    fun allMealFlowsShareSaveRestoreMergeContract() {
        assertEquals(5, MealFlowStateContract.flows.size)
        MealFlow.entries.forEach { flow ->
            val result = MealFlowStateContract.validate(
                flow,
                listOf(
                    StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.snapshot.before_navigation"),
                    StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.restore"),
                    StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.merge.result"),
                ),
            )
            assertTrue(result.complete, flow.code)
        }
    }

    @Test
    fun inventoryUsesSameContractWithoutInventingNavigationResult() {
        val result = MealFlowStateContract.validate(
            MealFlow.INVENTORY_SELECT,
            listOf(
                StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.snapshot.before_navigation"),
                StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.restore"),
                StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.merge.result"),
            ),
        )
        assertTrue(result.complete)
    }
}
