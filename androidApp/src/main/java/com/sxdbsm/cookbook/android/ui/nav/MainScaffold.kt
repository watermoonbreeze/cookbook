package com.sxdbsm.cookbook.android.ui.nav

import android.app.Activity
import com.sxdbsm.cookbook.android.util.AppLogger
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sxdbsm.cookbook.android.ui.addmeal.AddDayFoodScreen
import com.sxdbsm.cookbook.android.ui.kitchen.CookingTimerScreen
import com.sxdbsm.cookbook.android.ui.ai.AiRecommendScreen
import com.sxdbsm.cookbook.android.ui.ai.AiSettingsScreen
import com.sxdbsm.cookbook.android.ui.ingredients.IngredientJumpBus
import org.koin.compose.koinInject
import com.sxdbsm.cookbook.android.ui.dishdetail.DishDetailScreen
import com.sxdbsm.cookbook.android.ui.dishes.DishesScreen
import com.sxdbsm.cookbook.android.ui.home.HomeScreen
import com.sxdbsm.cookbook.android.ui.ingredients.IngredientsScreen
import com.sxdbsm.cookbook.android.ui.mine.MineScreen
import com.sxdbsm.cookbook.android.ui.newdish.NewDishScreen
import com.sxdbsm.cookbook.android.ui.search.SearchScreen
import com.sxdbsm.cookbook.android.ui.timeline.FoodTimelineScreen
import com.sxdbsm.cookbook.util.DateTime
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * App 主导航骨架。[AI修改]
 *
 * Scaffold 类似一个页面框架：底部栏、悬浮按钮、内容区都在这里统一装配。
 */
@Composable
fun MainScaffold(
    openTimer: Boolean = false, // [AI生成] 计时通知点击请求打开烹饪计时页。
    onTimerConsumed: () -> Unit = {},
) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val currentRoute = current?.destination?.route
    val context = LocalContext.current
    val ingredientJumpBus: IngredientJumpBus = koinInject() // [AI生成] 搜索点食材→跳到该食材总线。
    var lastBackAt by remember { mutableStateOf(0L) }

    // [AI生成] 点击计时通知 → 进入烹饪计时页（非首页）。
    LaunchedEffect(openTimer) {
        if (openTimer) {
            if (currentRoute != Routes.COOKING_TIMER) nav.navigate(Routes.COOKING_TIMER)
            onTimerConsumed()
        }
    }

    val showBottomBar = currentRoute in bottomTabs.map { it.route } // [AI修改] 详情/编辑页隐藏底部栏。

    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            AppLogger.event("screen_enter", mapOf("route" to route, "showBottomBar" to showBottomBar)) // [AI生成] 内测埋点：记录页面流转，便于分析异常跳转和常用路径。
        }
    }

    BackHandler {
        // [AI生成] 统一物理返回键：优先走路由返回；路由到底后非首页回首页；首页双击退出。
        if (currentRoute == Routes.HOME) {
            val now = System.currentTimeMillis()
            if (now - lastBackAt < 1800) {
                (context as? Activity)?.finish()
            } else {
                lastBackAt = now
                Toast.makeText(context, "再按一次退出应用", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (nav.previousBackStackEntry != null && nav.popBackStack()) {
                return@BackHandler
            }
            nav.navigate(Routes.HOME) {
                popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 根 Scaffold 不再自动避让系统栏，由透明系统栏和页面背景承接沉浸式效果。
        bottomBar = {
            if (showBottomBar) BottomBar(
                nav = nav,
                currentRoute = currentRoute,
                onAddMeal = { nav.navigate(Routes.addMeal()) },
            )
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenTimeline = { nav.navigate(Routes.TIMELINE_FULL) },
                    onOpenDishes = { nav.navigateRootTab(Routes.DISHES) },
                    onOpenSearch = { nav.navigate(Routes.SEARCH) },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    onEditMealDate = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) },
                    onOpenAiRecommend = { nav.navigate(Routes.aiRecommend()) },
                )
            }
            composable(Routes.TIMELINE) {
                FoodTimelineScreen(
                    onEditMealDate = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    onCopyMeal = { date -> nav.navigate(Routes.copyMealFrom(DateTime.formatDate(date))) }, // [AI生成] F8
                )
            }
            composable(Routes.TIMELINE_FULL) {
                FoodTimelineScreen(
                    onEditMealDate = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    onCopyMeal = { date -> nav.navigate(Routes.copyMealFrom(DateTime.formatDate(date))) }, // [AI生成] F8
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.DISHES) {
                DishesScreen(
                    onAddDish = { nav.navigate(Routes.newDish()) },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    onEditDish = { id -> nav.navigate(Routes.newDish(id)) },
                    onCopyDish = { id -> nav.navigate(Routes.copyDish(id)) },
                )
            }
            composable(Routes.INGREDIENTS) {
                IngredientsScreen()
            }
            composable(Routes.MINE) {
                MineScreen(
                    onOpenCookingTimer = { nav.navigate(Routes.COOKING_TIMER) },
                    onOpenAiSettings = { nav.navigate(Routes.AI_SETTINGS) },
                    onOpenAiRecommend = { nav.navigate(Routes.aiRecommend()) },
                    onOpenFeatureSettings = { nav.navigate(Routes.FEATURE_SETTINGS) },
                    onOpenShoppingList = { nav.navigate(Routes.SHOPPING_LIST) },
                    onOpenFreePairing = { nav.navigate(Routes.FREE_PAIRING) },
                )
            }
            composable(Routes.COOKING_TIMER) {
                CookingTimerScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.FEATURE_SETTINGS) {
                com.sxdbsm.cookbook.android.ui.settings.FeatureSettingsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.SHOPPING_LIST) {
                com.sxdbsm.cookbook.android.ui.shopping.ShoppingListScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.FREE_PAIRING) {
                com.sxdbsm.cookbook.android.ui.pairing.FreePairingScreen(onBack = { nav.popBackStack() })
            }
            composable(
                route = Routes.AI_RECOMMEND,
                arguments = listOf(navArgument("returnResult") { type = NavType.BoolType; defaultValue = false }),
            ) { entry ->
                val returnResult = entry.arguments?.getBoolean("returnResult") ?: false
                AiRecommendScreen(
                    onBack = { nav.popBackStack() },
                    onPickMeal = { dishIds ->
                        if (returnResult) {
                            // [AI生成] 从餐次块进入：把菜品回传给上一页(加餐页)对应餐次，不新开页面。
                            nav.previousBackStackEntry?.savedStateHandle?.set(KEY_AI_PICKED_DISHES, dishIds.toLongArray())
                            nav.popBackStack()
                        } else {
                            nav.navigate(Routes.addMealWithDishes(dishIds))
                        }
                    },
                )
            }
            composable(Routes.AI_SETTINGS) {
                AiSettingsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    onEditMealDate = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) },
                    onOpenIngredient = { ingredient -> // [AI修改] 点食材结果：设置跳转总线 + 切到食材页定位高亮。
                        ingredientJumpBus.request(ingredient)
                        nav.navigateRootTab(Routes.INGREDIENTS)
                    },
                )
            }
            composable(Routes.ADD_MEAL) {
                val date = it.arguments?.getString("date")?.takeIf { value -> value.isNotBlank() }?.let(DateTime::parseDate)
                val copyFrom = it.arguments?.getString("copyFrom")?.takeIf { value -> value.isNotBlank() }?.let(DateTime::parseDate) // [AI生成] F8：复制来源日期
                val presetDishIds = it.arguments?.getString("dishIds")
                    ?.split(",")?.mapNotNull { id -> id.toLongOrNull() }?.filter { id -> id > 0 }
                    ?: emptyList() // [AI生成] AI 推荐"选它"带入的菜品 id。
                val createdDishId by it.savedStateHandle
                    .getStateFlow(KEY_CREATED_DISH_ID, -1L)
                    .collectAsStateWithLifecycle()
                val aiPicked by it.savedStateHandle
                    .getStateFlow(KEY_AI_PICKED_DISHES, LongArray(0))
                    .collectAsStateWithLifecycle()
                AppLogger.d("MealFlow", "nav addmeal args: route=${it.destination.route} date=$date preset=$presetDishIds createdDishId=$createdDishId aiPicked=${aiPicked.toList()}") // [AI生成] 记录添加/编辑餐食路由参数和回传 id。
                AddDayFoodScreen(
                    onBack = { nav.popBackStack() },
                    onAddNewDish = {
                        AppLogger.d("MealFlow", "nav from addmeal to newdish") // [AI生成] 记录从添加餐食进入新建菜品。
                        nav.navigate(Routes.newDish())
                    },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) }, // [AI生成] F1：餐次里点菜进详情
                    copyFromDate = copyFrom, // [AI生成] F8：复制来源→预填新建草稿
                    editDate = date,
                    presetDishIds = presetDishIds,
                    onOpenAiForBlock = { nav.navigate(Routes.aiRecommendForMeal()) }, // [AI修改] 餐次块进入 AI 推荐(返回本页对应餐次)。
                    aiPickedDishIds = aiPicked.toList(),
                    onAiPickedConsumed = { it.savedStateHandle[KEY_AI_PICKED_DISHES] = LongArray(0) },
                    createdDishId = createdDishId.takeIf { id -> id > 0 },
                    onCreatedDishConsumed = {
                        AppLogger.d("MealFlow", "consume createdDishId: value=$createdDishId") // [AI生成] 记录导航结果消费，避免重复回填。
                        it.savedStateHandle[KEY_CREATED_DISH_ID] = -1L
                    },
                )
            }
            composable(
                route = Routes.NEW_DISH,
                arguments = listOf(
                    navArgument("dishId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("importDishId") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { entry ->
                // [AI修改] 使用路径参数承载编辑/导入 id，避免 query 参数在部分 Navigation 版本下丢失导致误进新建模式。
                val dishId = entry.arguments?.getLong("dishId")?.takeIf { it > 0 }
                val importDishId = entry.arguments?.getLong("importDishId")?.takeIf { it > 0 }
                AppLogger.d("NewDishEdit", "nav newdish args: route=${entry.destination.route} dishId=$dishId importDishId=$importDishId") // [AI生成] 记录导航层解析出的编辑/导入参数，便于排查空白表单。
                NewDishScreen(
                    editingDishId = dishId,
                    importDishId = importDishId,
                    onBack = { nav.popBackStack() },
                    onSavedDish = { savedDishId ->
                        AppLogger.d("NewDishEdit", "saved dish return result: savedDishId=$savedDishId previousRoute=${nav.previousBackStackEntry?.destination?.route}") // [AI生成] 记录新建/编辑菜品保存后向上一页回传的 id。
                        nav.previousBackStackEntry?.savedStateHandle?.set(KEY_CREATED_DISH_ID, savedDishId)
                    },
                )
            }
            composable(Routes.DISH_DETAIL) { entry ->
                val dishId = entry.arguments?.getString("dishId")?.toLongOrNull() ?: return@composable
                DishDetailScreen(
                    dishId = dishId,
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate(Routes.newDish(id)) },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) }, // [AI生成] 相关菜品跳转
                    onStartCook = { id -> nav.navigate(Routes.cookMode(id)) }, // [AI生成] 进入分步烹饪
                )
            }
            composable(Routes.COOK_MODE) { entry ->
                val dishId = entry.arguments?.getString("dishId")?.toLongOrNull() ?: return@composable
                com.sxdbsm.cookbook.android.ui.cook.CookModeScreen(
                    dishId = dishId,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

private const val KEY_AI_PICKED_DISHES = "aiPickedDishes" // [AI生成] AI 推荐从餐次进入时回传菜品 id 的导航结果 key。
private const val KEY_CREATED_DISH_ID = "createdDishId" // [AI生成] 添加餐食页从新建菜品页接收新菜品 id 的导航结果 key。

/**
 * 底部导航栏。[AI修改]
 */
@Composable
private fun BottomBar(nav: NavController, currentRoute: String?, onAddMeal: () -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.clip(MaterialTheme.shapes.extraSmall), // [AI修改] 顶部/底部导航按规范保持 4dp 小圆角。
    ) {
        bottomTabs.take(2).forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    nav.navigateRootTab(tab.route)
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        // [AI修改] 中间加号融入 NavigationBar 正中，不再悬浮在导航栏上方。
        NavigationBarItem(
            selected = false,
            onClick = onAddMeal,
            icon = { CenterPlusFab(onClick = onAddMeal) },
            label = { Spacer(Modifier.height(0.dp)) },
            alwaysShowLabel = false,
        )
        bottomTabs.drop(2).forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    nav.navigateRootTab(tab.route)
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

/**
 * 切换底部主 Tab。[AI修改]
 *
 * 首页“更多”进入菜品/食历时也走同一逻辑，避免把 Tab 页面压到路由栈上导致再点首页无反应。
 */
private fun NavController.navigateRootTab(route: String) {
    if (route == Routes.HOME && popBackStack(Routes.HOME, false)) {
        return
    } // [AI修改] 从“更多”等入口压入 Tab 页面后，点击首页优先直接回到已有首页。
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * 中间添加餐食按钮。[AI修改]
 */
@Composable
private fun CenterPlusFab(onClick: () -> Unit) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)  // [AI修改] 中间添加按钮按暖杏规范使用主色。
            .padding(2.dp)
            .clip(CircleShape),
    ) {
        Icon(Icons.Filled.Add, contentDescription = "添加餐食", modifier = Modifier.size(28.dp))
    }
}
