package com.sxdbsm.cookbook.android.util

import android.content.Context
import com.sxdbsm.cookbook.platform.CookbookStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The sole persistent JSONL output. Failures are isolated from business execution. */
internal object JsonlLogWriter {
    private val lock = Any()
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun append(context: Context, line: String) {
        synchronized(lock) {
            val directory = CookbookStorage.requireSubDir(CookbookStorage.LOG_DIR_NAME, context)
            File(directory, fileNameFor(Date())).appendText(line + "\n")
        }
    }

    internal fun fileNameFor(date: Date): String = "${dayFormat.format(date)}.log"
}
