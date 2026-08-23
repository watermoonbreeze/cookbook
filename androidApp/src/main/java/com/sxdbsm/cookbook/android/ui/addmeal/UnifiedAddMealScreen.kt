package com.sxdbsm.cookbook.android.ui.addmeal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl
import com.sxdbsm.cookbook.android.ui.component.rememberUnsavedGuard
import com.sxdbsm.cookbook.android.ui.nav.Routes
import com.sxdbsm.cookbook.android.ui.weekplan.WeekPlanScreen
import com.sxdbsm.cookbook.platform.BusinessTrace
import com.sxdbsm.cookbook.util.DateTime
import org.koin.androidx.compose.koinViewModel
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
    var pageState by rememberSaveable(stateSaver = UnifiedAddMealUiStateSaver) {
        mutableStateOf(UnifiedAddMealUiState())
    }

    val aiDirty = aiState.phase != AiMealPhase.INPUT ||
        aiState.quickDraftText.isNotBlank() ||
        aiState.periodInputs.values.any { it.isNotBlank() }
    val requestBack = rememberUnsavedGuard(
        isDirty = { aiDirty || singleManualVm.isDirty() },
        onConfirmLeave = { aiVm.cancelGeneration(); nav.popBackStack() },
    )

    androidx.compose.material3.Scaffold(
        topBar = { AppTopBar(title = "记录饮食", onBack = requestBack) },
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
