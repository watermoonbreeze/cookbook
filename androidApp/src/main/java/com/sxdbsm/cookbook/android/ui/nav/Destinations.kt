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
    const val TIMELINE_FULL = "timeline_full?date={date}" // [AI修改] 支持可选日期参数：从营养色系墙点色块跳到该日食历
    // [AI生成] 打开食历并可选定位到某日期(空=默认今天)。
    fun timelineAt(date: String? = null) = "timeline_full?date=${date.orEmpty()}"
    const val ADD_MEAL = "addmeal?date={date}&dishIds={dishIds}&copyFrom={copyFrom}"
    const val UNIFIED_ADD_MEAL = "unified_add_meal" // [AI生成] UEN：统一添加餐食全屏入口
    const val DISHES = "dishes"
    const val INGREDIENTS = "ingredients"
    const val SEARCH = "search"
    const val MINE = "mine"
    const val COOKING_TIMER = "cooking_timer"
    // [AI修改] F#7:加可选 slot 参数(ai.MealSlot.code)——从餐次块进入时带该餐次预选推荐；空/缺省=全部(默认值,不改原行为)。
    const val AI_RECOMMEND = "ai_recommend?returnResult={returnResult}&slot={slot}" // [AI生成] AI 推荐下一餐
    fun aiRecommend() = "ai_recommend?returnResult=false" // 从首页/我的进入：选它开新加餐页(不带 slot→默认全部)
    // [AI修改] F#7:从餐次块进入带该餐次 slot(code)预选；slot 为空则同原行为(全部)。
    fun aiRecommendForMeal(slot: String = "") = "ai_recommend?returnResult=true&slot=$slot" // 从餐次块进入：选它回传该餐次
    const val AI_SETTINGS = "ai_settings" // [AI生成] AI 设置(Key/运行时)
    const val FAMILY = "family"
    const val FAMILY_STATS = "family_stats" // [AI生成] 膳食统计 // [AI生成] 家庭成员管理
    const val FEATURE_SETTINGS = "feature_settings" // [AI生成] 功能设置(分步执行等开关)
    const val SHOPPING_LIST = "shopping_list" // [AI生成] 采购清单(今天及未来餐食采购/缺料汇总)
    const val WEEK_PLAN = "week_plan" // [AI生成] B3 一周计划视图(无参=从今天所在周)
    const val WEEK_PLAN_ROUTE = "week_plan?date={date}" // [AI生成] 路由模式:可选 date 参数(定位到该日期所在周)
    fun weekPlanFrom(date: String) = "week_plan?date=$date" // [AI生成] 报告空周期→带目标日期跳一周计划(定位该周·月则含月首日所在周)
    const val FREE_PAIRING = "free_pairing" // [AI生成] 食材自由搭配(离线规则轻搭配)
    const val NUTRITION_TABLE = "nutrition_table" // [AI生成] 食材营养表(全量营养总览)
    const val DIET_REPORT = "diet_report" // [AI生成] 饮食报告(周/月·家庭+个人)
    const val TCM_REFERENCE = "tcm_reference" // [AI生成] 中医食养参考(药食同源+免责)
    const val HEALTH_CONDITION_REFERENCE = "health_condition_reference" // [AI生成] 健康状态参考(4病种饮食关注点+口径+免责)
    const val DIETARY_REFERENCE = "dietary_reference" // [AI生成] 膳食参考依据(阈值分级引用的权威标准/指南透明展示)
    const val NUTRITION_RULE_REFERENCE = "nutrition_rule_reference" // [AI生成] 营养怎么算的(热量/摄入折算/分级口径的计算说明+免责)
    const val HEALTH_SCIENCE_REFERENCE = "health_science_reference" // [AI生成] 健康科普(食物消化吸收代谢+食材与健康状态为什么·分两层)
    const val VITAMIN_REFERENCE = "vitamin_reference" // [AI生成] 维生素小百科(各维生素作用/来源/缺乏·脂溶水溶分类)
    const val DATA_SOURCE = "data_source" // [AI生成] 数据来源(食材分类/营养/GI/嘌呤/预设菜品各自来源)
    const val UPDATE_LOG = "update_log" // [AI生成] F#8 透明准则:基础数据更新记录(每次更新做了什么·可查)
    const val FEATURE_GUIDE = "feature_guide" // [AI生成] 功能介绍(首次使用讲清app做什么/怎么用)
    const val USER_AGREEMENT = "user_agreement" // [AI生成] 阶段3-c 用户协议
    const val PRIVACY_POLICY = "privacy_policy" // [AI生成] 阶段3-c 隐私政策

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
