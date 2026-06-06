package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.model.ThemeMode

/**
 * @File : ThemeModeDialog
 * @Time : 2026/06/05
 * @Author : SXD-AI
 * @Desc : 通用主题模式选择弹框
 * <p>
 * 首页顶部主题按钮和“我的”页主题切换共用该组件，确保主题选项、文案和交互一致。
 * <p>
 * [AI生成] 用户要求首页点击主题图标直接弹出切换弹框，因此抽取原“我的”页私有弹框为通用组件。
 **/
@Composable
fun ThemeModeDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题切换") },
        text = {
            Column {
                listOf(
                    ThemeMode.SYSTEM to "跟随系统",
                    ThemeMode.LIGHT to "浅色",
                    ThemeMode.DARK to "深色",
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == current, onClick = { onSelect(mode) })
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}
