package com.sxdbsm.cookbook.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
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

    /** 当前本地小时(0~23)。[AI生成] 供"按时段推算默认餐食日期"(晚上→明天)。 */
    fun currentHour(): Int =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour

    /** 当前本地时间(时:分)。[AI生成] A2：非固定餐次默认当前时间，免强制手动选。 */
    fun nowTime(): LocalTime =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time.let { LocalTime(it.hour, it.minute) }

    fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

    /** epoch 秒 → 本地日期字符串(yyyy-MM-dd)。[AI生成] 供库存"入库日窗口"比较餐食日期。 */
    fun epochSecondsToDate(seconds: Long): String =
        Instant.fromEpochSeconds(seconds).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

    fun parseDate(text: String): LocalDate = LocalDate.parse(text)

    fun parseTime(text: String): LocalTime = LocalTime.parse(text)

    /** 日期加减天数，KMP 下不要使用 JVM 专属的 java.time。[AI生成] */
    fun plusDays(date: LocalDate, days: Int): LocalDate = date.plus(DatePeriod(days = days))

    /** from→to 相差天数(to 晚为正)。[AI生成] B2：算"近N天吃过"的 N。 */
    fun daysBetween(from: LocalDate, to: LocalDate): Int = from.daysUntil(to)

    /** 把 LocalDate 序列化为 yyyy-MM-dd */
    fun formatDate(date: LocalDate): String = date.toString()

    /** 把 LocalTime 序列化为 HH:mm */
    fun formatTime(time: LocalTime): String {
        val hh = time.hour.toString().padStart(2, '0')
        val mm = time.minute.toString().padStart(2, '0')
        return "$hh:$mm"
    }
}
