package com.sxdbsm.cookbook.domain.projection

import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.MealDayContent
import com.sxdbsm.cookbook.domain.model.MealDayTemporalRole
import kotlinx.datetime.LocalDate

/** 将中性日内容投影为既有餐卡；显式 referenceDate 保证结果可测试且不依赖系统时钟。[AI修改] */
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
