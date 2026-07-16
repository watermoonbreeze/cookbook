package com.sxdbsm.cookbook.android.ui.ingredients

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.picker.IngredientPickerScreen
import com.sxdbsm.cookbook.android.ui.picker.IngredientPickerViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * @File : IngredientsScreen
 * @Time : 2026/06/21
 * @Author : SXD-AI
 * @Desc : 食材一级页面
 * <p>
 * 复用食材选择器的分类、搜索、新增、编辑、删除和分类管理能力，但以底部 Tab 页面形式展示。
 * <p>
 * [AI生成] 任务10将食材从菜品选择弹窗提升为与菜品同等级的一级入口。
 * [AI修改] 消费 IngredientJumpBus，从全局搜索点食材结果时跳到该食材并高亮。
 **/
@Composable
fun IngredientsScreen() {
    val vm: IngredientPickerViewModel = koinViewModel()
    val jumpBus: IngredientJumpBus = koinInject()
    val pending by jumpBus.pending.collectAsStateWithLifecycle()

    // [AI修改] 收到跨屏跳转请求 → 直接开该食材详情(顶部显分类路径),不再 jumpToIngredient 定位网格
    // (自建/家庭食材会跳错到常规 Tab)。消费后清空。
    var jumpDetail by remember { mutableStateOf<com.sxdbsm.cookbook.domain.model.Ingredient?>(null) }
    LaunchedEffect(pending) {
        pending?.let {
            jumpDetail = it
            jumpBus.consume()
        }
    }

    IngredientPickerScreen(
        excludeIngredientIds = emptySet(),
        onDismiss = {},
        onConfirm = {},
        asDialog = false,
        selectionMode = false,
        openDetailFor = jumpDetail,
        vm = vm,
    )
}
