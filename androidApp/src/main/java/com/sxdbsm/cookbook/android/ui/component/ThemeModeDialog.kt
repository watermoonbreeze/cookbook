package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.runtime.Composable
import com.sxdbsm.cookbook.domain.model.ThemeMode

/**
 * @File : ThemeModeDialog
 * @Time : 2026/06/05
 * @Author : SXD-AI
 * @Desc : 通用主题模式选择（苹果风格底部 Action Sheet）
 * <p>
 * 首页顶部主题按钮和"我的"页主题切换共用。[AI修改] 苹果风格：从居中 AlertDialog+RadioButton
 * 改为底部 ActionSheet；当前项在标签后加 ✓ 标记。
 * <p>
 * [AI生成] 用户要求首页点击主题图标直接弹出切换弹框，因此抽取原"我的"页私有弹框为通用组件。
 **/
@Composable
fun ThemeModeDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        ThemeMode.SYSTEM to "跟随系统",
        ThemeMode.LIGHT to "浅色",
        ThemeMode.DARK to "深色",
    )
    ActionSheet(
        title = "主题",
        actions = options.map { (mode, label) ->
            SheetAction(
                label = if (mode == current) "$label ✓" else label,
                onClick = { onSelect(mode) },
            )
        },
        onDismiss = onDismiss,
    )
}
