package com.sxdbsm.cookbook.android.ui.ingredients

import androidx.compose.runtime.Composable
import com.sxdbsm.cookbook.android.ui.picker.IngredientPickerScreen
import org.koin.androidx.compose.koinViewModel

/**
 * @File : IngredientsScreen
 * @Time : 2026/06/21
 * @Author : SXD-AI
 * @Desc : 食材一级页面
 * <p>
 * 复用食材选择器的分类、搜索、新增、编辑、删除和分类管理能力，但以底部 Tab 页面形式展示。
 * <p>
 * [AI生成] 任务10将食材从菜品选择弹窗提升为与菜品同等级的一级入口。
 **/
@Composable
fun IngredientsScreen() {
    IngredientPickerScreen(
        excludeIngredientIds = emptySet(),
        onDismiss = {},
        onConfirm = {},
        asDialog = false,
        selectionMode = false,
        vm = koinViewModel(),
    )
}
