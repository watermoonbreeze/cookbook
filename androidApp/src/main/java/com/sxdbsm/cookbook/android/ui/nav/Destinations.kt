package com.sxdbsm.cookbook.android.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 应用内路由常量与构造方法。[AI修改]
 *
 * Compose Navigation 使用字符串描述页面路径，类似 Web 路由或 Android DeepLink。
 */
object Routes {
    const val HOME = "home"
    const val TIMELINE = "timeline"
    const val TIMELINE_FULL = "timeline_full"
    const val ADD_MEAL = "addmeal?date={date}&dishIds={dishIds}&copyFrom={copyFrom}"
    const val DISHES = "dishes"
    const val INGREDIENTS = "ingredients"
    const val SEARCH = "search"
    const val MINE = "mine"
    const val COOKING_TIMER = "cooking_timer"
    const val AI_RECOMMEND = "ai_recommend?returnResult={returnResult}" // [AI生成] AI 推荐下一餐
    fun aiRecommend() = "ai_recommend?returnResult=false" // 从首页/我的进入：选它开新加餐页
    fun aiRecommendForMeal() = "ai_recommend?returnResult=true" // 从餐次块进入：选它回传该餐次
    const val AI_SETTINGS = "ai_settings" // [AI生成] AI 设置(Key/运行时)
    const val FEATURE_SETTINGS = "feature_settings" // [AI生成] 功能设置(分步执行等开关)
    const val SHOPPING_LIST = "shopping_list" // [AI生成] 采购清单(今天及未来餐食采购/缺料汇总)
    const val WEEK_PLAN = "week_plan" // [AI生成] B3 一周计划视图
    const val FREE_PAIRING = "free_pairing" // [AI生成] 食材自由搭配(离线规则轻搭配)

    fun addMeal(date: String? = null) = "addmeal?date=${date.orEmpty()}&dishIds=&copyFrom="

    // [AI生成] AI 推荐"选它"：带入菜品 id 到加餐页预填（用户确认后再保存）。
    fun addMealWithDishes(dishIds: List<Long>, date: String? = null) =
        "addmeal?date=${date.orEmpty()}&dishIds=${dishIds.joinToString(",")}&copyFrom="

    // [AI生成] F8：食历"复制"→按来源日期预填成新建草稿(日期默认源+1，可改)。
    fun copyMealFrom(sourceDate: String) = "addmeal?date=&dishIds=&copyFrom=$sourceDate"

    const val NEW_DISH = "newdish/{dishId}/{importDishId}"
    fun newDish(dishId: Long? = null) = "newdish/${dishId ?: -1L}/-1"
    fun copyDish(dishId: Long) = "newdish/-1/$dishId"

    const val DISH_DETAIL = "dish/{dishId}"
    fun dishDetail(dishId: Long) = "dish/$dishId"

    const val COOK_MODE = "cook/{dishId}" // [AI生成] 分步烹饪全屏
    fun cookMode(dishId: Long) = "cook/$dishId"
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
    TabItem(Routes.DISHES, "菜品", Icons.Outlined.Restaurant),
    TabItem(Routes.INGREDIENTS, "食材", Icons.Outlined.SoupKitchen),
    TabItem(Routes.MINE, "我的", Icons.Outlined.Person),
)
