package com.sxdbsm.cookbook.android.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.TimeZone

class JsonlLogWriterTest {
    @Test
    fun writerUsesOneDailyJsonlFileName() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("1970-01-01.log", JsonlLogWriter.fileNameFor(Date(0)))
    }
}
