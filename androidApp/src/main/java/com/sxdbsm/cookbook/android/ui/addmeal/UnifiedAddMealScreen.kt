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
import androidx.compose.runtime.remember
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
) {
    val aiVm: AiMealInputViewModel = koinViewModel(key = "unified-ai-meal") {
        parametersOf("", DateTime.today())
    }
    val singleManualVm: AddMealViewModel = koinViewModel(key = "unified-single-manual")
    val aiState by aiVm.state.collectAsStateWithLifecycle()
    val appSnackbar = LocalAppSnackbar.current
    var pageState by remember { mutableStateOf(UnifiedAddMealUiState()) }
    var pendingChange by remember { mutableStateOf<(() -> Unit)?>(null) }

    val aiDirty = aiState.phase != AiMealPhase.INPUT ||
        aiState.quickDraftText.isNotBlank() ||
        aiState.periodInputs.values.any { it.isNotBlank() }
    val requestBack = rememberUnsavedGuard(
        isDirty = { aiDirty || singleManualVm.isDirty() },
        onConfirmLeave = { aiVm.cancelGeneration(); nav.popBackStack() },
    )

    fun requestChange(action: () -> Unit) {
        if (pageState.method == MealInputMethod.AI && aiState.phase == AiMealPhase.SAVING) return
        if (pageState.method == MealInputMethod.AI && aiState.phase != AiMealPhase.INPUT) {
            pendingChange = action
        } else {
            action()
        }
    }

    if (pendingChange != null) {
        AlertDialog(
            onDismissRequest = { pendingChange = null },
            title = { Text("放弃当前预览？") },
            text = { Text("AI 已解析的餐食预览将不会被保存。") },
            confirmButton = {
                TextButton(onClick = {
                    val action = pendingChange
                    pendingChange = null
                    aiVm.cancelGeneration()
                    action?.invoke()
                }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { pendingChange = null }) { Text("继续编辑") }
            },
        )
    }

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
                onSelect = { index ->
                    val range = if (index == 0) MealRange.SINGLE_DAY else MealRange.PERIOD
                    requestChange {
                        pageState = pageState.reduce(UnifiedAddMealEvent.SelectRange(range))
                        if (pageState.method == MealInputMethod.AI) {
                            aiVm.setInputMode(if (range == MealRange.SINGLE_DAY) InputMode.QUICK else InputMode.WEEK)
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SegmentedControl(
                options = listOf("AI 快捷", "手动选择"),
                selectedIndex = if (pageState.method == MealInputMethod.AI) 0 else 1,
                onSelect = { index ->
                    val method = if (index == 0) MealInputMethod.AI else MealInputMethod.MANUAL
                    requestChange {
                        pageState = pageState.reduce(UnifiedAddMealEvent.SelectMethod(method))
                        if (method == MealInputMethod.AI) {
                            aiVm.setInputMode(if (pageState.range == MealRange.SINGLE_DAY) InputMode.QUICK else InputMode.WEEK)
                        }
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
                        onAddNewDish = { nav.navigate(Routes.newDish()) },
                        onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                        onOpenWeekPlan = { date -> nav.navigate(Routes.weekPlanFrom(DateTime.formatDate(date))) },
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
