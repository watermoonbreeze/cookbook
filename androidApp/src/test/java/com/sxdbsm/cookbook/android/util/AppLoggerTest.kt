package com.sxdbsm.cookbook.android.util

import com.sxdbsm.cookbook.platform.StructuredLogJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AppLoggerTest {
    @Test
    fun unsafeCodesAreNeverAcceptedByAndroidSinkCodec() {
        assertEquals("redacted_invalid_code", StructuredLogJson.sanitizeCode("健康档案原值"))
        assertEquals("redacted_invalid_code", StructuredLogJson.sanitizeCode("x".repeat(65)))
        assertNotEquals("prompt 内容", StructuredLogJson.sanitizeCode("prompt 内容"))
    }
}
