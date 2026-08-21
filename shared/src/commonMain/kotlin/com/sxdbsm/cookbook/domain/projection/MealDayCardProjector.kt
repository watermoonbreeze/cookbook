package com.sxdbsm.cookbook.domain.projection

import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.MealDayContent
import com.sxdbsm.cookbook.domain.model.MealDayTemporalRole
import kotlinx.datetime.LocalDate

/**
 * MDC2 projection boundary：将稳定的 [MealDayContent] 投影为共享餐卡。
 *
 * Feature 不应自行从内容推导时间角色；显式 referenceDate 也保证结果可测试且不依赖系统时钟。
 */
object MealDayCardProjector {
    fun temporalRole(date: LocalDate, referenceDate: LocalDate): MealDayTemporalRole = when {
        date < referenceDate -> MealDayTemporalRole.PAST
        date == referenceDate -> MealDayTemporalRole.TODAY
        else -> MealDayTemporalRole.FUTURE
    }

    fun project(content: MealDayContent, referenceDate: LocalDate): DayMealCardData =
        DayMealCardData(
            date = content.date,
            temporalRole = temporalRole(content.date, referenceDate),
            meals = content.meals,
        )
}
