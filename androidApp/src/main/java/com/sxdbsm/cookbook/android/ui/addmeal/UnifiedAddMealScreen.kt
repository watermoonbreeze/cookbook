package com.sxdbsm.cookbook.android.ui.addmeal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sxdbsm.cookbook.android.ui.ai.AiMealInputViewModel
import com.sxdbsm.cookbook.android.ui.ai.AiMealPhase
import com.sxdbsm.cookbook.android.ui.ai.AiMealBody
import com.sxdbsm.cookbook.android.ui.ai.InputMode
import com.sxdbsm.cookbook.android.ui.ai.MealInputHelpSheet
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl
import com.sxdbsm.cookbook.android.ui.component.rememberUnsavedGuard
import com.sxdbsm.cookbook.android.ui.nav.Routes
import com.sxdbsm.cookbook.android.ui.weekplan.WeekPlanScreen
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.domain.model.PreferenceKeys
import com.sxdbsm.cookbook.platform.BusinessTrace
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.first
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * 统一添加餐食全屏入口。
 *
 * [AI生成] UEN：页面只编排四种 Body，不复制 AI、单天手动或周期计划业务逻辑。
 */
@Composable
fun UnifiedAddMealScreen(
    nav: NavController,
    onOpenAiForBlock: (String) -> Unit = {},
    aiPickedDishIds: List<Long> = emptyList(),
    onAiPickedConsumed: () -> Unit = {},
    createdDishId: Long? = null,
    lifecycleTraceId: String? = null,
    onCreatedDishConsumed: () -> Unit = {},
) {
    val aiVm: AiMealInputViewModel = koinViewModel(key = "unified-ai-meal") {
        parametersOf("", DateTime.today())
    }
    val singleManualVm: AddMealViewModel = koinViewModel(key = "unified-single-manual")
    val aiState by aiVm.state.collectAsStateWithLifecycle()
    val appSnackbar = LocalAppSnackbar.current
    val prefs: PreferenceRepository = koinInject() // [AI生成] 输入格式统一：首弹说明的一次性标记读写。
    var pageState by rememberSaveable(stateSaver = UnifiedAddMealUiStateSaver) {
        mutableStateOf(UnifiedAddMealUiState())
    }

    // [AI生成] 输入格式统一：顶栏 ⓘ 手动入口 + 首次进入 AI 快捷分支自动弹一次说明
    //   （默认分支即 AI，等价"首次进入记录饮食"；看过/关过不再自动弹，顶栏仍可手动看）。
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var autoHelpChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(pageState.method) {
        if (!autoHelpChecked && pageState.method == MealInputMethod.AI) {
            autoHelpChecked = true
            // 先置标记再弹（与 HAS_SEEN_GUIDE 同款，防旋转/进程重建重复弹——踩坑红线）。
            if (!prefs.observeFlag(PreferenceKeys.HAS_SEEN_MEAL_INPUT_HELP, default = false).first()) {
                prefs.setFlag(PreferenceKeys.HAS_SEEN_MEAL_INPUT_HELP, true)
                showHelp = true
            }
        }
    }
    if (showHelp) MealInputHelpSheet(onDismiss = { showHelp = false })

    // [AI修改] 输入入口统一·复审修复：恢复周期记"已有餐食"角标数据——原编辑页打开 AI Sheet 时
    //   注入 existingMealDates，入口收口后该写入丢失致 WeekStrip"已有"标记静默消失
    //   （google_quality_engineer 复审抓漏）。进周期+AI 分支时查一次当前周（与原行为一致，
    //   翻周不刷新是既有已知项 E-B6-04）。
    LaunchedEffect(pageState.method, pageState.range) {
        if (pageState.method == MealInputMethod.AI && pageState.range == MealRange.PERIOD) {
            val monday = com.sxdbsm.cookbook.ai.meallog.InputSegmentFactory.mondayOfWeek(DateTime.today())
            aiVm.setExistingMealDates(singleManualVm.datesWithMealsInWeek(monday))
        }
    }

    val aiDirty = aiState.phase != AiMealPhase.INPUT ||
        aiState.quickDraftText.isNotBlank() ||
        aiState.periodInputs.values.any { it.isNotBlank() }
    val requestBack = rememberUnsavedGuard(
        isDirty = { aiDirty || singleManualVm.isDirty() },
        onConfirmLeave = { aiVm.cancelGeneration(); nav.popBackStack() },
    )

    androidx.compose.material3.Scaffold(
        topBar = {
            AppTopBar(
                title = "记录饮食",
                onBack = requestBack,
                // [AI生成] 输入格式统一：恢复统一入口的操作说明入口（原 AiMealInputSheet 头部 ⓘ 因
                //   showHeader=false 不可达），照 AiRecommendScreen 顶栏说明图标先例，四分支常驻。
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "输入说明")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            SegmentedControl(
                options = listOf("单天", "周期"),
                selectedIndex = if (pageState.range == MealRange.SINGLE_DAY) 0 else 1,
                enabled = canChangeMealRange(aiState.phase),
                onSelect = { index ->
                    val range = if (index == 0) MealRange.SINGLE_DAY else MealRange.PERIOD
                    pageState = pageState.reduce(UnifiedAddMealEvent.SelectRange(range))
                    if (pageState.method == MealInputMethod.AI) {
                        aiVm.setInputMode(if (range == MealRange.SINGLE_DAY) InputMode.QUICK else InputMode.WEEK)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SegmentedControl(
                options = listOf("AI 快捷", "手动选择"),
                selectedIndex = if (pageState.method == MealInputMethod.AI) 0 else 1,
                enabled = canChangeMealMethod(aiState.phase),
                onSelect = { index ->
                    val method = if (index == 0) MealInputMethod.AI else MealInputMethod.MANUAL
                    pageState = pageState.reduce(UnifiedAddMealEvent.SelectMethod(method))
                    if (method == MealInputMethod.AI) {
                        aiVm.setInputMode(if (pageState.range == MealRange.SINGLE_DAY) InputMode.QUICK else InputMode.WEEK)
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                pageState.range == MealRange.SINGLE_DAY && pageState.method == MealInputMethod.AI ->
                    AiMealBody(
                        vm = aiVm,
                        onSaved = { savedState ->
                            if (shouldLeaveAfterAiSave(savedState.phase)) {
                                appSnackbar?.showMessage("已保存")
                                aiVm.reset()
                                nav.popBackStack()
                            }
                        },
                    )
                pageState.range == MealRange.SINGLE_DAY && pageState.method == MealInputMethod.MANUAL ->
                    AddDayFoodScreen(
                        onBack = { nav.popBackStack() },
                        onAddNewDish = {
                            // [AI生成] SAVE：离开统一入口前冻结页面选择器/手动草稿的代码摘要。
                            val trace = BusinessTrace.action("unified_add_meal", "open_new_dish", "manual")
                            BusinessTrace.stateSnapshotBeforeNavigation("unified_add_meal", "manual", "editing", trace)
                            nav.currentBackStackEntry?.savedStateHandle?.set("stateLifecycleTraceId", trace.value)
                            nav.navigate(Routes.newDish())
                        },
                        onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                        onOpenWeekPlan = { date -> nav.navigate(Routes.weekPlanFrom(DateTime.formatDate(date))) },
                        onOpenAiForBlock = onOpenAiForBlock,
                        aiPickedDishIds = aiPickedDishIds,
                        onAiPickedConsumed = onAiPickedConsumed,
                        createdDishId = createdDishId,
                        lifecycleTraceId = lifecycleTraceId,
                        onCreatedDishConsumed = onCreatedDishConsumed,
                        vm = singleManualVm,
                        embedded = true,
                    )
                pageState.range == MealRange.PERIOD && pageState.method == MealInputMethod.AI ->
                    AiMealBody(
                        vm = aiVm,
                        onSaved = { savedState ->
                            if (shouldLeaveAfterAiSave(savedState.phase)) {
                                appSnackbar?.showMessage("已保存")
                                aiVm.reset()
                                nav.popBackStack()
                            }
                        },
                    )
                else ->
                    WeekPlanScreen(
                        onBack = { nav.popBackStack() },
                        onEditMealDate = { date -> nav.navigate(Routes.addMeal(DateTime.formatDate(date))) },
                        onCopyMeal = { date -> nav.navigate(Routes.copyMealFrom(DateTime.formatDate(date))) },
                        onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                        embedded = true,
                    )
            }
        }
    }
}
