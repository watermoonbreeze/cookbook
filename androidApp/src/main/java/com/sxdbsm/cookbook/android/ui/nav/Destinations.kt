package com.sxdbsm.cookbook.android.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 应用内路由常量与构造方法。[AI修改]
 *
 * Compose Navigation 使用字符串描述页面路径，类似 Web 路由或 Android DeepLink。
 */
object Routes {
    const val HOME = "home"
    const val TIMELINE = "timeline"
    const val ADD_MEAL = "addmeal?date={date}"
    const val DISHES = "dishes"
    const val SEARCH = "search"
    const val MINE = "mine"
    const val COOKING_TIMER = "cooking_timer"

    fun addMeal(date: String? = null) = "addmeal?date=${date.orEmpty()}"

    const val NEW_DISH = "newdish/{dishId}/{importDishId}"
    fun newDish(dishId: Long? = null) = "newdish/${dishId ?: -1L}/-1"
    fun copyDish(dishId: Long) = "newdish/-1/$dishId"

    const val DISH_DETAIL = "dish/{dishId}"
    fun dishDetail(dishId: Long) = "dish/$dishId"
}

/**
 * 底部 Tab 的展示配置。[AI修改]
 */
data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * 底部导航栏固定的四个入口。[AI修改]
 */
val bottomTabs = listOf(
    TabItem(Routes.HOME, "首页", Icons.Outlined.Home),
    TabItem(Routes.TIMELINE, "食历", Icons.Outlined.CalendarMonth),
    TabItem(Routes.DISHES, "菜品", Icons.Outlined.Restaurant),
    TabItem(Routes.MINE, "我的", Icons.Outlined.Person),
)
