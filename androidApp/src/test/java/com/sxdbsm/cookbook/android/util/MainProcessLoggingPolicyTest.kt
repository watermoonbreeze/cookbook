package com.sxdbsm.cookbook.android.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainProcessLoggingPolicyTest {
    @Test
    fun onlyTheApplicationProcessInstallsTheOrdinarySink() {
        assertTrue(MainProcessLoggingPolicy.isMainProcessName("pkg", "pkg"))
        assertFalse(MainProcessLoggingPolicy.isMainProcessName("pkg", "pkg:crash"))
    }
}
