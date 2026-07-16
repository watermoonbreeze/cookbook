package com.sxdbsm.cookbook.android.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * @File : UnsavedGuard
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 未保存返回守卫（统一封装）——有改动时返回先确认"放弃未保存的更改？"
 * <p>
 * §9.17：有 isDirty 的编辑表单统一用本组件，禁每屏内联复制。内部管 BackHandler + 放弃弹框，
 * 返回一个 `requestBack` 给顶栏返回按钮用。"放弃编辑"属主动丢弃、保留确认；"删除数据"属可逆走撤销(§9.12)。
 * <p>
 * [AI生成] 抽自 NewDishScreen 内联守卫，非包裹式(返回 requestBack)，便于套进大 Scaffold 屏而不重排缩进。
 **/
@Composable
fun rememberUnsavedGuard(
    isDirty: () -> Boolean,
    onConfirmLeave: () -> Unit,
    dialogText: String = "你的改动还没保存，返回将丢失。",
): () -> Unit {
    var prompt by remember { mutableStateOf(false) }
    val requestBack: () -> Unit = { if (isDirty()) prompt = true else onConfirmLeave() }
    BackHandler(enabled = true) { requestBack() }
    if (prompt) {
        AlertDialog(
            onDismissRequest = { prompt = false },
            title = { Text("放弃未保存的更改？") },
            text = { Text(dialogText) },
            confirmButton = {
                TextButton(
                    onClick = { prompt = false; onConfirmLeave() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("放弃更改") }
            },
            dismissButton = { TextButton(onClick = { prompt = false }) { Text("继续编辑") } },
        )
    }
    return requestBack
}
