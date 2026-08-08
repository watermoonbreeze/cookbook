package com.sxdbsm.cookbook.android.ui.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sxdbsm.cookbook.android.ui.component.CapsuleButton
import com.sxdbsm.cookbook.android.ui.component.CapsuleOutlineButton

/**
 * @File : CloudAiConsentPanel
 * @Time : 2026/08/08
 * @Author : SXD-AI
 * @Desc : 云端 AI 完整同意面板（独立弹层，能力显隐由回调是否传入决定，禁 mode 布尔——项目红线）。蓝图 L1 §4.4/§5.1。
 * <p>
 * 独立 Dialog，由宿主 AiSettingsScreen 按需打开/关闭；grandfather=true 时首句改"这项功能你已在使用"口径。
 * onAgree 非空 → 双按钮态（启用/暂不启用）；onAgree==null → 只读态仅显"知道了"（走 onClose，不写任何 consent 状态）。
 * <p>
 * 布局沿用 PrivacyConsentDialog.kt 已验证范式（28dp 圆角 + tonalElevation 6 + usePlatformDefaultWidth=false）。
 * [AI生成] L1。
 **/
@Composable
fun CloudAiConsentPanel(
    vendorName: String,
    grandfather: Boolean = false,
    onAgree: (() -> Unit)? = null, // 非空 → 双按钮态；null → 只读态仅显"知道了"
    onDecline: (() -> Unit)? = null,
    onClose: () -> Unit, // 返回键/点外部/只读态"知道了"统一走这个，不写任何 consent 状态
) {
    BackHandler(onBack = onClose)
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = false, // 统一走 onClose（等价"这次先不处理"）
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false, // 让 fillMaxWidth 真正生效（沿用 PrivacyConsentDialog 审查建议6）
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 16.dp)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    if (grandfather) "这项功能你已在使用" else "启用云端 AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "启用云端 AI 后，以下内容会发送给 $vendorName：", // [AI修改] copywriter 🔴#2：去"处理"赘字 + 改"启用后"持续态
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(14.dp))
                SendList(
                    title = "会发送",
                    items = CloudAiDisclosure.WILL_SEND,
                    icon = Icons.Outlined.Public,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                SendList(
                    title = "不会发送",
                    items = CloudAiDisclosure.WONT_SEND,
                    icon = Icons.Outlined.VisibilityOff,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                // 影响卡（守透明准则：做了什么/影响什么/怎么控制）
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(18.dp).padding(top = 2.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("启用后会怎样", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) // [AI修改] copywriter 🟡#7：说人话
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "只在你主动用 AI 记一餐、AI 推荐等功能时发送；关闭后自动回退本地规则，不影响日常记餐。", // [AI修改] copywriter 🟡#8：半角斜杠改顿号 + 消歧"AI 记一餐 vs 记一餐"
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "你可以随时在「AI 设置」里关闭云端 AI。", // [AI修改] copywriter 🔴#5：删冗余"同意状态可改"（能关闭即能改同意状态）
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(18.dp))
                if (onAgree != null) {
                    Column(Modifier.fillMaxWidth()) {
                        CapsuleButton(text = "启用云端 AI", onClick = onAgree, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = { onDecline?.invoke() ?: onClose() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("暂不启用", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                } else {
                    Column(Modifier.fillMaxWidth()) {
                        CapsuleOutlineButton(text = "知道了", onClick = onClose, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/** 发送/不发送清单行（图标 + 标题 + 逐项）。[AI生成] L1 */
@Composable
private fun SendList(
    title: String,
    items: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.width(18.dp).padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
            Spacer(Modifier.height(4.dp))
            items.forEach { item ->
                Text("· $item", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }
        }
    }
}
