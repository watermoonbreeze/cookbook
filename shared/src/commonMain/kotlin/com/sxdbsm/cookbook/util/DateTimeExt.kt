package com.sxdbsm.cookbook.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * 跨平台日期时间工具。[AI修改]
 *
 * 这里统一使用 kotlinx-datetime，避免 shared 层直接依赖 Android/JVM 专属的时间 API。
 */
object DateTime {
    fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

    fun parseDate(text: String): LocalDate = LocalDate.parse(text)

    fun parseTime(text: String): LocalTime = LocalTime.parse(text)

    /** 日期加减天数，KMP 下不要使用 JVM 专属的 java.time。[AI生成] */
    fun plusDays(date: LocalDate, days: Int): LocalDate = date.plus(DatePeriod(days = days))

    /** 把 LocalDate 序列化为 yyyy-MM-dd */
    fun formatDate(date: LocalDate): String = date.toString()

    /** 把 LocalTime 序列化为 HH:mm */
    fun formatTime(time: LocalTime): String {
        val hh = time.hour.toString().padStart(2, '0')
        val mm = time.minute.toString().padStart(2, '0')
        return "$hh:$mm"
    }
}
