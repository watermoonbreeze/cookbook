package com.sxdbsm.cookbook.util

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateTime {
    fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

    fun parseDate(text: String): LocalDate = LocalDate.parse(text)

    fun parseTime(text: String): LocalTime = LocalTime.parse(text)

    /** 把 LocalDate 序列化为 yyyy-MM-dd */
    fun formatDate(date: LocalDate): String = date.toString()

    /** 把 LocalTime 序列化为 HH:mm */
    fun formatTime(time: LocalTime): String {
        val hh = time.hour.toString().padStart(2, '0')
        val mm = time.minute.toString().padStart(2, '0')
        return "$hh:$mm"
    }
}
