package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 一个操作项。[AI生成] destructive=破坏性(红字)。 */
data class SheetAction(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * @File : ActionSheet
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 苹果风格底部操作面板（iOS Action Sheet）
 * <p>
 * 多操作选择 / 破坏性确认：从底部弹出，破坏项红字置于列表，"取消"独立置底。替代把多操作塞进居中
 * AlertDialog 的反模式。Material3 1.1.2 的 ModalBottomSheet 为实验 API。
 * <p>
 * [AI生成] 苹果风格改造 Phase1 组件库。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
    title: String? = null,
    message: String? = null,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            if (!title.isNullOrBlank()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
            if (!message.isNullOrBlank()) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }
            actions.forEach { action ->
                TextButton(
                    onClick = { action.onClick(); onDismiss() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    Text(
                        action.label,
                        style = MaterialTheme.typography.bodyLarge,
                        // [AI修改] iOS：普通操作黑字、仅破坏项红字(避免一屏全橙)。
                        color = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            InsetDivider(0)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            ) {
                Text(
                    "取消",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
