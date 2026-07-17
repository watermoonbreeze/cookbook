package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 通用空状态组件。[AI修改]
 *
 * 当列表没有数据时显示图标和说明文字。
 */
@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    icon: String = "🍽",
    actionLabel: String? = null, // [AI生成] UX(§9.6):空态给"下一步"——传入则渲染一个可点动作胶囊按钮。
    onAction: (() -> Unit)? = null,
) {
    // [AI修改] 苹果风格：图标放大变淡、留白拉大、文案居中克制。
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 52.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            icon,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // [AI生成] 空态可点动作(§9.6 给下一步)：仅在同时传入 label+回调时显示。
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            CapsuleButton(text = actionLabel, onClick = onAction)
        }
    }
}
