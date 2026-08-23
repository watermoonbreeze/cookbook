package com.sxdbsm.cookbook.android.ui.nav

import android.app.Activity
import com.sxdbsm.cookbook.android.util.AppLogger
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sxdbsm.cookbook.android.ui.addmeal.UnifiedAddMealScreen
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
import com.sxdbsm.cookbook.platform.BusinessTrace
import com.sxdbsm.cookbook.platform.ActionResultStatus
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first

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
    val prefs: com.sxdbsm.cookbook.data.repository.PreferenceRepository = koinInject() // [AI生成] 首启引导标记读写。
    val analytics: com.sxdbsm.cookbook.analytics.Analytics = koinInject() // [AI生成] 阶段3-b：first_launch/feature_used 埋点(未同意时闸门拦截)。
    var lastBackAt by remember { mutableStateOf(0L) }

    // [AI生成] 点击计时通知 → 进入烹饪计时页（非首页）。
    // [AI修改] 修华为A12等设备闪退：通知/全屏提醒拉起 Activity 时 openTimer=true 会让本 effect 立即 navigate，
    //   但此时 NavHost 尚未把导航图挂到 NavController（合成时序更慢的机型上尤其明显）→ 抛
    //   "Navigation graph has not been set" 崩溃(crash栈 MainScaffold.kt navigate)。
    //   跳转前先挂起等 currentBackStackEntryFlow 发出首个条目(=图已挂载就绪)再跳，彻底消除时序竞态。
    LaunchedEffect(openTimer) {
        if (!openTimer) return@LaunchedEffect
        nav.currentBackStackEntryFlow.first() // 等导航图就绪(首个回退栈条目出现)，避免图未挂时 navigate 崩溃
        if (nav.currentDestination?.route != Routes.COOKING_TIMER) nav.navigate(Routes.COOKING_TIMER)
        onTimerConsumed()
    }

    // [AI生成] 阶段3-c 合规门：首启先弹「隐私政策/用户协议同意」(不可绕过·同意后才 init 采数)，通过后才轮到功能介绍。
    var showPrivacyGate by remember { mutableStateOf(false) }
    // [AI生成] 首次启动：①未同意隐私→弹合规门 ②已同意→按需弹「功能介绍」(仅第一次·之后可从"我的"手动看)。
    LaunchedEffect(Unit) {
        if (!prefs.isPrivacyAgreed()) {
            showPrivacyGate = true // 未同意→弹门,先不判功能介绍(同意回调里再顺跑)
            return@LaunchedEffect
        }
        if (!prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.HAS_SEEN_GUIDE, false).first()) {
            prefs.setFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.HAS_SEEN_GUIDE, true)
            nav.navigate(Routes.FEATURE_GUIDE)
        }
    }
    // [AI生成] 阶段3-c 合规门弹窗：同意→写 PRIVACY_AGREED + 按复选框设匿名统计同意 + 顺跑功能介绍；不同意→退出。
    val gateScope = androidx.compose.runtime.rememberCoroutineScope()
    if (showPrivacyGate) {
        com.sxdbsm.cookbook.android.ui.onboarding.PrivacyConsentDialog(
            onAgree = { analyticsChecked ->
                gateScope.launch {
                    prefs.setPrivacyAgreed(true)
                    prefs.setAnalyticsEnabled(analyticsChecked)
                    analytics.setEnabled(analyticsChecked) // 同意后闸门即时放行(未勾则维持关)
                    showPrivacyGate = false
                    // 同意后才轮到功能介绍(首次)——FirstLaunch 埋点也挪到同意之后才有意义。
                    if (!prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.HAS_SEEN_GUIDE, false).first()) {
                        analytics.track(com.sxdbsm.cookbook.analytics.AnalyticsEvent.FirstLaunch)
                        prefs.setFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.HAS_SEEN_GUIDE, true)
                        nav.navigate(Routes.FEATURE_GUIDE)
                    }
                }
            },
            onDisagreeExit = { (context as? Activity)?.finishAffinity() },
        )
    }

    val showBottomBar = currentRoute in bottomTabs.map { it.route } // [AI修改] 详情/编辑页隐藏底部栏。

    // [AI生成] 阶段3-b：feature_used 去抖到"功能区切换"粒度——记上次上报的功能区,只有换了功能区才报(审查建议2)。
    //   否则 A→详情→返回A 会让 A 重复计数、漏斗失真;二级页(fromRoute→null)不改变 lastFeature、返回后同功能区不重报。
    var lastFeature by remember { mutableStateOf<com.sxdbsm.cookbook.analytics.FeatureTag?>(null) }
    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            AppLogger.event("screen_enter", mapOf("route" to route, "showBottomBar" to showBottomBar)) // [AI生成] 内测埋点：记录页面流转，便于分析异常跳转和常用路径。
            // [AI生成] 阶段3-b 匿名统计：用了某功能区(功能频次/发现漏斗)。**只上报白名单枚举**·未知路由不上报(fromRoute→null)、绝不回传原始路由串。
            com.sxdbsm.cookbook.analytics.FeatureTag.fromRoute(route)?.let { feature ->
                if (feature != lastFeature) {
                    lastFeature = feature
                    analytics.track(com.sxdbsm.cookbook.analytics.AnalyticsEvent.FeatureUsed(feature))
                }
            }
            val screen = route.substringBefore('?')
            BusinessTrace.navigationCompleted("previous", screen)
            BusinessTrace.screenEntered(screen)
            BusinessTrace.screenLoaded(screen)
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

    // [AI生成] §9.12：全 App 单一 Snackbar 宿主 + 控制器，经 LocalAppSnackbar 下发给各屏(统一保存/撤销反馈)。
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val snackScope = androidx.compose.runtime.rememberCoroutineScope()
    val appSnackbar = remember { com.sxdbsm.cookbook.android.ui.component.AppSnackbarController(snackbarHostState, snackScope) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 根 Scaffold 不再自动避让系统栏，由透明系统栏和页面背景承接沉浸式效果。
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) BottomBar(
                nav = nav,
                currentRoute = currentRoute,
                onAddMeal = { nav.navigate(Routes.UNIFIED_ADD_MEAL) },
            )
        },
    ) { padding ->
      androidx.compose.runtime.CompositionLocalProvider(com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar provides appSnackbar) {
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            // [AI修改] 沉浸式布局下，全屏页面(无底部导航栏)内容会伸到系统导航栏下被遮挡——
            // 统一给无底部栏路由补系统导航栏 inset 底padding；Tab 页由 NavigationBar 自身避让。
            modifier = Modifier
                .padding(padding)
                .then(if (showBottomBar) Modifier else Modifier.navigationBarsPadding()),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenTimeline = { nav.navigate(Routes.timelineAt()) },
                    onOpenDishes = { nav.navigateRootTab(Routes.DISHES) },
                    onOpenSearch = { nav.navigate(Routes.SEARCH) },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    onEditMealDate = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) },
                    onOpenTimelineAt = { date -> nav.navigate(Routes.timelineAt(DateTime.formatDate(date))) }, // [AI生成] 营养色系墙点色块→食历定位该日
                    onCopyMeal = { date -> nav.navigate(Routes.copyMealFrom(DateTime.formatDate(date))) }, // [AI生成] A1
                    onOpenWeekPlan = { nav.navigate(Routes.WEEK_PLAN) }, // [AI生成] B3
                    onOpenAiRecommend = {
                        val trace = BusinessTrace.action("home", "open_ai_recommend", "next_meal")
                        BusinessTrace.actionResult("home", "open_ai_recommend", ActionResultStatus.HANDLED, trace)
                        BusinessTrace.navigationStarted("home", "ai_recommend", trace)
                        nav.navigate(Routes.aiRecommend())
                    },
                    onOpenDietaryReference = { nav.navigate(Routes.DIETARY_REFERENCE) }, // [AI生成] P3-B:今日卡"各类每天吃多少"→膳食参考依据页
                )
            }
            composable(Routes.TIMELINE) {
                FoodTimelineScreen(
                    onEditMealDate = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    onCopyMeal = { date -> nav.navigate(Routes.copyMealFrom(DateTime.formatDate(date))) }, // [AI生成] F8
                )
            }
            composable(
                route = Routes.TIMELINE_FULL,
                arguments = listOf(navArgument("date") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                val jumpDate = entry.arguments?.getString("date")?.takeIf { it.isNotBlank() }?.let(DateTime::parseDate) // [AI生成] 营养色系墙传入的目标日期
                FoodTimelineScreen(
                    onEditMealDate = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    onCopyMeal = { date -> nav.navigate(Routes.copyMealFrom(DateTime.formatDate(date))) }, // [AI生成] F8
                    onBack = { nav.popBackStack() },
                    initialJumpDate = jumpDate, // [AI生成] 打开后定位到该日
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
                IngredientsScreen(onOpenNewDish = { nav.navigate(Routes.newDish()) }) // [AI生成] 从食材"组成菜品"→新建菜品(预填食材走总线)
            }
            composable(Routes.MINE) {
                MineScreen(
                    onOpenCookingTimer = { nav.navigate(Routes.COOKING_TIMER) },
                    onOpenAiSettings = { nav.navigate(Routes.AI_SETTINGS) },
                    onOpenAiRecommend = {
                        val trace = BusinessTrace.action("mine", "open_ai_recommend", "mine_ai")
                        BusinessTrace.actionResult("mine", "open_ai_recommend", ActionResultStatus.HANDLED, trace)
                        BusinessTrace.navigationStarted("mine", "ai_recommend", trace)
                        nav.navigate(Routes.aiRecommend())
                    },
                    onOpenFeatureSettings = { nav.navigate(Routes.FEATURE_SETTINGS) },
                    onOpenShoppingList = { nav.navigate(Routes.SHOPPING_LIST) },
                    onOpenFreePairing = { nav.navigate(Routes.FREE_PAIRING) },
                    onOpenNutritionTable = { nav.navigate(Routes.NUTRITION_TABLE) }, // [AI生成] 食材营养表
                    onOpenDietReport = { nav.navigate(Routes.DIET_REPORT) }, // [AI生成] 饮食报告
                    onOpenTcmReference = { nav.navigate(Routes.TCM_REFERENCE) }, // [AI生成] 食养参考
                    onOpenHealthConditionReference = { nav.navigate(Routes.HEALTH_CONDITION_REFERENCE) }, // [AI生成] 健康状态参考
                    onOpenDietaryReference = { nav.navigate(Routes.DIETARY_REFERENCE) }, // [AI生成] 膳食参考依据
                    onOpenNutritionRule = { nav.navigate(Routes.NUTRITION_RULE_REFERENCE) }, // [AI生成] 营养怎么算的(计算说明)
                    onOpenHealthScience = { nav.navigate(Routes.HEALTH_SCIENCE_REFERENCE) }, // [AI生成] 健康科普(消化代谢+食材机制)
                    onOpenVitamin = { nav.navigate(Routes.VITAMIN_REFERENCE) }, // [AI生成] 维生素小百科
                    onOpenDataSource = { nav.navigate(Routes.DATA_SOURCE) }, // [AI生成] 数据来源
                    onOpenUpdateLog = { nav.navigate(Routes.UPDATE_LOG) }, // [AI生成] F#8:基础数据更新记录
                    onOpenFeatureGuide = { nav.navigate(Routes.FEATURE_GUIDE) }, // [AI生成] 功能介绍
                    onOpenFamily = { nav.navigate(Routes.FAMILY) }, // [AI生成] 档案整合:家庭档案统一入口
                    onOpenUserAgreement = { nav.navigate(Routes.USER_AGREEMENT) }, // [AI生成] 阶段3-c 用户协议
                    onOpenPrivacyPolicy = { nav.navigate(Routes.PRIVACY_POLICY) }, // [AI生成] 阶段3-c 隐私政策
                )
            }
            composable(Routes.NUTRITION_TABLE) {
                com.sxdbsm.cookbook.android.ui.nutrition.NutritionTableScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.DIET_REPORT) {
                com.sxdbsm.cookbook.android.ui.report.DietReportScreen(
                    onBack = { nav.popBackStack() },
                    onGoAddMeal = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) }, // [AI生成] F#6：报告"记一餐"带周期目标日期(该日有餐=编辑/无=新增)
                    onGoWeekPlan = { date -> nav.navigate(Routes.weekPlanFrom(DateTime.formatDate(date))) }, // [AI生成] 空周期→跳一周计划定位该周(月则含月首日)
                )
            }
            composable(Routes.DIETARY_REFERENCE) {
                com.sxdbsm.cookbook.android.ui.reference.DietaryReferenceScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.NUTRITION_RULE_REFERENCE) {
                // [AI生成] 营养怎么算的：计算说明页 + 两条互链(阈值全表→膳食参考依据·病种关注→健康状态参考)
                com.sxdbsm.cookbook.android.ui.reference.NutritionRuleReferenceScreen(
                    onBack = { nav.popBackStack() },
                    onOpenDietaryReference = { nav.navigate(Routes.DIETARY_REFERENCE) },
                    onOpenHealthCondition = { nav.navigate(Routes.HEALTH_CONDITION_REFERENCE) },
                )
            }
            composable(Routes.HEALTH_SCIENCE_REFERENCE) {
                // [AI生成] 健康科普:食物消化吸收代谢 + 食材与健康状态为什么(分两层) + 两条互链
                com.sxdbsm.cookbook.android.ui.reference.HealthScienceReferenceScreen(
                    onBack = { nav.popBackStack() },
                    onOpenHealthCondition = { nav.navigate(Routes.HEALTH_CONDITION_REFERENCE) },
                    onOpenDietaryReference = { nav.navigate(Routes.DIETARY_REFERENCE) },
                )
            }
            composable(Routes.VITAMIN_REFERENCE) {
                // [AI生成] 维生素小百科:各维生素作用/来源/缺乏(脂溶A/D/E/K·水溶B族/C) + 互链健康科普
                com.sxdbsm.cookbook.android.ui.reference.VitaminReferenceScreen(
                    onBack = { nav.popBackStack() },
                    onOpenHealthScience = { nav.navigate(Routes.HEALTH_SCIENCE_REFERENCE) },
                )
            }
            composable(Routes.DATA_SOURCE) {
                com.sxdbsm.cookbook.android.ui.reference.DataSourceScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.UPDATE_LOG) {
                com.sxdbsm.cookbook.android.ui.reference.UpdateLogScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.TCM_REFERENCE) {
                com.sxdbsm.cookbook.android.ui.reference.TcmReferenceScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.USER_AGREEMENT) {
                com.sxdbsm.cookbook.android.ui.policy.PolicyScreen(
                    title = "用户协议",
                    sections = com.sxdbsm.cookbook.android.ui.policy.USER_AGREEMENT_SECTIONS,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.PRIVACY_POLICY) {
                com.sxdbsm.cookbook.android.ui.policy.PolicyScreen(
                    title = "隐私政策",
                    sections = com.sxdbsm.cookbook.android.ui.policy.PRIVACY_POLICY_SECTIONS,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.HEALTH_CONDITION_REFERENCE) {
                com.sxdbsm.cookbook.android.ui.reference.HealthConditionReferenceScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.FEATURE_GUIDE) {
                com.sxdbsm.cookbook.android.ui.guide.FeatureGuideScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.COOKING_TIMER) {
                CookingTimerScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.FEATURE_SETTINGS) {
                com.sxdbsm.cookbook.android.ui.settings.FeatureSettingsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenFamily = { nav.navigate(Routes.FAMILY) },
                )
            }
            composable(Routes.FAMILY) {
                com.sxdbsm.cookbook.android.ui.family.FamilyScreen(
                    onBack = { nav.popBackStack() },
                    onOpenStats = { nav.navigate(Routes.FAMILY_STATS) },
                )
            }
            composable(Routes.FAMILY_STATS) {
                com.sxdbsm.cookbook.android.ui.family.FamilyStatsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.SHOPPING_LIST) {
                com.sxdbsm.cookbook.android.ui.shopping.ShoppingListScreen(onBack = { nav.popBackStack() })
            }
            composable(
                Routes.WEEK_PLAN_ROUTE, // [AI修改] 可选 date 参数(报告空周期定位该周)；无参(如首页入口)date="" → initialDate=null 从今天所在周。
                arguments = listOf(navArgument("date") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                // [AI生成] B3 一周计划：逐日编辑/复制/安排复用既有添加餐食/复制路由。
                val dateArg = entry.arguments?.getString("date").orEmpty()
                val initialDate = dateArg.takeIf { it.isNotBlank() }?.let { runCatching { DateTime.parseDate(it) }.getOrNull() }
                com.sxdbsm.cookbook.android.ui.weekplan.WeekPlanScreen(
                    onBack = { nav.popBackStack() },
                    onEditMealDate = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) },
                    onCopyMeal = { date -> nav.navigate(Routes.copyMealFrom(DateTime.formatDate(date))) },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    initialDate = initialDate,
                )
            }
            composable(Routes.FREE_PAIRING) {
                com.sxdbsm.cookbook.android.ui.pairing.FreePairingScreen(
                    onBack = { nav.popBackStack() },
                    onOpenNewDish = { nav.navigate(Routes.newDish()) }, // [AI生成] 存为菜品→预填新建菜品页
                )
            }
            composable(
                route = Routes.AI_RECOMMEND,
                arguments = listOf(
                    navArgument("returnResult") { type = NavType.BoolType; defaultValue = false },
                    navArgument("slot") { type = NavType.StringType; defaultValue = "" }, // [AI生成] F#7:餐次块带入的 ai.MealSlot.code(空=全部)
                ),
            ) { entry ->
                val returnResult = entry.arguments?.getBoolean("returnResult") ?: false
                AiRecommendScreen(
                    initialSlotCode = entry.arguments?.getString("slot").orEmpty(), // [AI生成] F#7:预选该餐次
                    onBack = { nav.popBackStack() },
                    onPickMeal = { dishIds ->
                        if (returnResult) {
                            // [AI生成] 从餐次块进入：把菜品回传给上一页(加餐页)对应餐次，不新开页面。
                            nav.previousBackStackEntry?.savedStateHandle?.set(KEY_AI_PICKED_DISHES, dishIds.toLongArray())
                            val parentTrace = nav.previousBackStackEntry?.savedStateHandle?.get<String>(KEY_LIFECYCLE_TRACE_ID).orEmpty()
                            nav.previousBackStackEntry?.savedStateHandle?.set(KEY_LIFECYCLE_TRACE_ID, parentTrace)
                            nav.popBackStack()
                        } else {
                            nav.navigate(Routes.addMealWithDishes(dishIds))
                        }
                    },
                    onOpenDietaryReference = { nav.navigate(Routes.DIETARY_REFERENCE) }, // [AI生成] P3:"各类每天吃多少"→膳食参考依据页(权威每日份量)
                )
            }
            composable(Routes.AI_SETTINGS) {
                AiSettingsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.SEARCH) {
                val lifecycleTraceId by it.savedStateHandle
                    .getStateFlow(KEY_LIFECYCLE_TRACE_ID, "")
                    .collectAsStateWithLifecycle()
                val createdDishId by it.savedStateHandle
                    .getStateFlow(KEY_CREATED_DISH_ID, -1L)
                    .collectAsStateWithLifecycle()
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    onEditMealDate = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) },
                    onOpenIngredient = { ingredient -> // [AI修改] 点食材结果：设置跳转总线 + 切到食材页定位高亮。
                        ingredientJumpBus.request(ingredient)
                        nav.navigateRootTab(Routes.INGREDIENTS)
                    },
                    onNewDish = {
                        val trace = BusinessTrace.action("food_search", "open_new_dish", "search")
                        BusinessTrace.stateSnapshotBeforeNavigation("food_search", "search", "searching_or_idle", trace)
                        it.savedStateHandle[KEY_LIFECYCLE_TRACE_ID] = trace.value
                        nav.navigate(Routes.newDish())
                    }, // [AI生成] 全无结果新建菜品(预填名走总线)
                    createdDishId = createdDishId.takeIf { id -> id > 0 },
                    lifecycleTraceId = lifecycleTraceId.takeIf { value -> value.isNotBlank() },
                    onCreatedDishConsumed = {
                        it.savedStateHandle[KEY_CREATED_DISH_ID] = -1L
                        it.savedStateHandle[KEY_LIFECYCLE_TRACE_ID] = ""
                    },
                    onNewIngredient = { nav.navigateRootTab(Routes.INGREDIENTS) }, // [AI生成] 全无结果新建食材(按名开编辑器走总线)
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
                val lifecycleTraceId by it.savedStateHandle
                    .getStateFlow(KEY_LIFECYCLE_TRACE_ID, "")
                    .collectAsStateWithLifecycle()
                AppLogger.d("MealFlow", "nav addmeal args: route=${it.destination.route} date=$date preset=$presetDishIds createdDishId=$createdDishId aiPicked=${aiPicked.toList()}") // [AI生成] 记录添加/编辑餐食路由参数和回传 id。
                AddDayFoodScreen(
                    onBack = { nav.popBackStack() },
                    onAddNewDish = {
                        AppLogger.d("MealFlow", "nav from addmeal to newdish") // [AI生成] 记录从添加餐食进入新建菜品。
                        val trace = BusinessTrace.action("add_meal", "open_new_dish", "manual")
                        BusinessTrace.stateSnapshotBeforeNavigation("add_meal", "manual", "editing", trace)
                        it.savedStateHandle[KEY_LIFECYCLE_TRACE_ID] = trace.value
                        nav.navigate(Routes.newDish())
                    },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) }, // [AI生成] F1：餐次里点菜进详情
                    onOpenWeekPlan = { targetDate ->
                        // [AI修改] 多天 AI 记餐保存后离开编辑页，避免只刷新首日而掩盖其他天。
                        nav.navigate(Routes.weekPlanFrom(DateTime.formatDate(targetDate))) {
                            popUpTo(Routes.ADD_MEAL) { inclusive = true }
                        }
                    },
                    copyFromDate = copyFrom, // [AI生成] F8：复制来源→预填新建草稿
                    editDate = date,
                    presetDishIds = presetDishIds,
                    onOpenAiForBlock = { slotCode ->
                        val trace = BusinessTrace.action("add_meal", "open_ai_recommend", "meal_edit")
                        BusinessTrace.stateSnapshotBeforeNavigation("add_meal", "manual", "editing", trace)
                        it.savedStateHandle[KEY_LIFECYCLE_TRACE_ID] = trace.value
                        BusinessTrace.actionResult("add_meal", "open_ai_recommend", ActionResultStatus.HANDLED, trace)
                        BusinessTrace.navigationStarted("add_meal", "ai_recommend", trace)
                        BusinessTrace.recommendRoute("ai_recommend", "meal_edit", trace)
                        nav.navigate(Routes.aiRecommendForMeal(slotCode))
                    }, // [AI修改] 餐次块进入 AI 推荐(返回本页对应餐次)。[AI修改] F#7:带该餐次 slot 预选。
                    aiPickedDishIds = aiPicked.toList(),
                    lifecycleTraceId = lifecycleTraceId.takeIf { value -> value.isNotBlank() },
                    onAiPickedConsumed = {
                        it.savedStateHandle[KEY_AI_PICKED_DISHES] = LongArray(0)
                        it.savedStateHandle[KEY_LIFECYCLE_TRACE_ID] = ""
                    },
                    createdDishId = createdDishId.takeIf { id -> id > 0 },
                    onCreatedDishConsumed = {
                        AppLogger.d("MealFlow", "consume createdDishId: value=$createdDishId") // [AI生成] 记录导航结果消费，避免重复回填。
                        it.savedStateHandle[KEY_CREATED_DISH_ID] = -1L
                        it.savedStateHandle[KEY_LIFECYCLE_TRACE_ID] = ""
                    },
                )
            }
            composable(Routes.UNIFIED_ADD_MEAL) { entry ->
                val aiPicked by entry.savedStateHandle
                    .getStateFlow(KEY_AI_PICKED_DISHES, LongArray(0))
                    .collectAsStateWithLifecycle()
                val lifecycleTraceId by entry.savedStateHandle
                    .getStateFlow(KEY_LIFECYCLE_TRACE_ID, "")
                    .collectAsStateWithLifecycle()
                val createdDishId by entry.savedStateHandle
                    .getStateFlow(KEY_CREATED_DISH_ID, -1L)
                    .collectAsStateWithLifecycle()
                UnifiedAddMealScreen(
                    nav = nav,
                    aiPickedDishIds = aiPicked.toList(),
                    lifecycleTraceId = lifecycleTraceId.takeIf { value -> value.isNotBlank() },
                    onAiPickedConsumed = {
                        AppLogger.d("MealFlow", "consume unified AI recommend result: dishIds=${aiPicked.toList()}") // [AI修改] 统一入口消费 AI 推荐回传，避免返回后结果丢失或重复注入。
                        entry.savedStateHandle[KEY_AI_PICKED_DISHES] = LongArray(0)
                        entry.savedStateHandle[KEY_LIFECYCLE_TRACE_ID] = ""
                    },
                    createdDishId = createdDishId.takeIf { it > 0 },
                    onCreatedDishConsumed = {
                        AppLogger.d("MealFlow", "consume unified child result: createdDishId=$createdDishId") // [AI修改] 统一入口消费新建菜品结果，返回后合并到原餐次草稿。
                        entry.savedStateHandle[KEY_CREATED_DISH_ID] = -1L
                        entry.savedStateHandle[KEY_LIFECYCLE_TRACE_ID] = ""
                    },
                    onOpenAiForBlock = { slotCode ->
                        val trace = BusinessTrace.action("add_meal", "open_ai_recommend", "record_meal_manual")
                        BusinessTrace.stateSnapshotBeforeNavigation("unified_add_meal", "manual", "editing", trace)
                        entry.savedStateHandle[KEY_LIFECYCLE_TRACE_ID] = trace.value
                        BusinessTrace.actionResult("add_meal", "open_ai_recommend", ActionResultStatus.HANDLED, trace)
                        BusinessTrace.navigationStarted("add_meal", "ai_recommend", trace)
                        BusinessTrace.recommendRoute("ai_recommend", "record_meal_manual", trace)
                        nav.navigate(Routes.aiRecommendForMeal(slotCode))
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
                        val previous = nav.previousBackStackEntry
                        val previousRoute = previous?.destination?.route
                        val acceptsDishResult = previousRoute == Routes.SEARCH ||
                            previousRoute == Routes.UNIFIED_ADD_MEAL ||
                            previousRoute == Routes.ADD_MEAL ||
                            previousRoute?.startsWith("addmeal?") == true
                        if (acceptsDishResult) {
                            previous?.savedStateHandle?.set(KEY_CREATED_DISH_ID, savedDishId)
                            previous?.savedStateHandle?.get<String>(KEY_LIFECYCLE_TRACE_ID)?.let { traceId ->
                                previous.savedStateHandle[KEY_LIFECYCLE_TRACE_ID] = traceId
                            }
                        } else {
                            AppLogger.d("NewDishEdit", "ignore dish result for non-contract parent route=$previousRoute")
                        }
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
                    onCopyDish = { id -> nav.navigate(Routes.copyDish(id)) }, // [AI生成] 预设菜"另存为我的菜"
                    onAddToMeal = { id -> nav.navigate(Routes.addMealWithDishes(listOf(id))) }, // [AI生成] 详情"记这道菜"→加餐流程预填
                    onOpenMember = { _ -> nav.navigate(Routes.FAMILY) }, // [AI生成] Phase 2:点成员名→跳家庭档案页(二期可精确定位该成员)
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
      } // CompositionLocalProvider(LocalAppSnackbar)
    }
}

private const val KEY_AI_PICKED_DISHES = "aiPickedDishes" // [AI生成] AI 推荐从餐次进入时回传菜品 id 的导航结果 key。
private const val KEY_CREATED_DISH_ID = "createdDishId" // [AI生成] 添加餐食页从新建菜品页接收新菜品 id 的导航结果 key。
private const val KEY_LIFECYCLE_TRACE_ID = "stateLifecycleTraceId" // [AI生成] 结果回传沿用父页面生命周期 trace。

/**
 * 底部导航栏。[AI修改]
 */
@Composable
private fun BottomBar(nav: NavController, currentRoute: String?, onAddMeal: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f).height(58.dp),
            shape = RoundedCornerShape(29.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = currentRoute == tab.route,
                        onClick = { nav.navigateRootTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(22.dp)) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }
        CenterPlusFab(onClick = onAddMeal)
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
