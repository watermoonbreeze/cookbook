package com.sxdbsm.cookbook.android.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val TIMELINE = "timeline"
    const val ADD_MEAL = "addmeal"
    const val DISHES = "dishes"
    const val MINE = "mine"

    const val NEW_DISH = "newdish?dishId={dishId}&importDishId={importDishId}"
    fun newDish(dishId: Long? = null) = "newdish?dishId=${dishId ?: -1L}&importDishId=-1"
    fun copyDish(dishId: Long) = "newdish?dishId=-1&importDishId=$dishId"

    const val DISH_DETAIL = "dish/{dishId}"
    fun dishDetail(dishId: Long) = "dish/$dishId"
}

data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomTabs = listOf(
    TabItem(Routes.HOME, "首页", Icons.Outlined.Home),
    TabItem(Routes.TIMELINE, "食历", Icons.Outlined.CalendarMonth),
    TabItem(Routes.DISHES, "菜品", Icons.Outlined.Restaurant),
    TabItem(Routes.MINE, "我的", Icons.Outlined.Person),
)
