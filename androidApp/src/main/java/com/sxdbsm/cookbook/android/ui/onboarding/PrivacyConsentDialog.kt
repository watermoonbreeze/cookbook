package com.sxdbsm.cookbook.android.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sxdbsm.cookbook.android.ui.component.CapsuleButton
import com.sxdbsm.cookbook.android.ui.policy.PRIVACY_POLICY_SECTIONS
import com.sxdbsm.cookbook.android.ui.policy.PolicySection
import com.sxdbsm.cookbook.android.ui.policy.USER_AGREEMENT_SECTIONS

/**
 * @File : PrivacyConsentDialog
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 首启隐私合规门弹窗（不可绕过·同意《用户协议》《隐私政策》后才可用）
 * <p>
 * 合规硬要求：不可点外/返回键关闭；友盟等 SDK 延迟到同意后才初始化采数。含可选"匿名使用统计"复选框（默认不勾）。
 * **协议/隐私政策在门内内嵌切换查看**（同一 Dialog 窗口·不被自身 scrim 遮挡·返回不丢勾选态·审查阻断#1 方案B）。
 * "不同意"给一次温和二次挽留后退出（不做暗黑弱化）。文案交 copywriter 定稿。
 * <p>
 * [AI生成] 阶段3-c：apple_ux_designer §9.25 首启隐私门规范。
 **/
@Composable
fun PrivacyConsentDialog(
    onAgree: (analyticsChecked: Boolean) -> Unit,
    onDisagreeExit: () -> Unit,
) {
    var analyticsChecked by remember { mutableStateOf(false) } // 匿名统计默认不勾（最强隐私姿态）
    var confirmExit by remember { mutableStateOf(false) } // 不同意的二次挽留
    // 0=同意面板；1=看用户协议；2=看隐私政策（门内切换·同一窗口不遮挡）
    var viewing by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = { /* 合规：不可绕过 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false, // 让 fillMaxWidth 真正生效（审查建议6）
        ),
    ) {
        BackHandler(enabled = true) { if (viewing != 0) viewing = 0 /* 政策视图返回同意面板·否则吞掉 */ }
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        ) {
            when (viewing) {
                0 -> ConsentPanel(
                    analyticsChecked = analyticsChecked,
                    onAnalyticsCheckedChange = { analyticsChecked = it },
                    onAgree = { onAgree(analyticsChecked) },
                    onDisagree = { confirmExit = true },
                    onOpenUserAgreement = { viewing = 1 },
                    onOpenPrivacyPolicy = { viewing = 2 },
                )
                else -> InlinePolicyPanel(
                    title = if (viewing == 1) "用户协议" else "隐私政策",
                    sections = if (viewing == 1) USER_AGREEMENT_SECTIONS else PRIVACY_POLICY_SECTIONS,
                    onBack = { viewing = 0 },
                )
            }
        }
    }

    // 不同意的二次挽留（标准 AlertDialog·防误触 + 温和）
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("确定不同意吗？") },
            text = { Text("你的数据只存在这台手机上，不会上传。需要同意后才能开始使用。") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; onDisagreeExit() }) {
                    Text("退出应用", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("再想想") }
            },
        )
    }
}

/** 同意面板（图标+标题+说明+协议链接+匿名统计复选框+两按钮）。[AI生成] */
@Composable
private fun ConsentPanel(
    analyticsChecked: Boolean,
    onAnalyticsCheckedChange: (Boolean) -> Unit,
    onAgree: () -> Unit,
    onDisagree: () -> Unit,
    onOpenUserAgreement: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("欢迎使用「今天吃啥」", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
            Text(
                "我们很重视你的隐私。你记录的饭菜、健康档案都只保存在你手机上，不会上传。\n\n" +
                    "为了改进产品，App 会用友盟统计——只在你同意后，上报去掉身份的功能使用情况（比如打开了哪个页面），不会上报你的饭菜和健康信息。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(12.dp))
            ConsentLinksLine(onOpenUserAgreement, onOpenPrivacyPolicy)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().toggleable(value = analyticsChecked, onValueChange = onAnalyticsCheckedChange),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(checked = analyticsChecked, onCheckedChange = onAnalyticsCheckedChange)
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f).padding(top = 12.dp)) {
                Text("同意匿名使用统计（可选）", style = MaterialTheme.typography.bodyMedium)
                Text("帮助我们改进产品，随时可在「我的」里关闭", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Spacer(Modifier.height(20.dp))
        CapsuleButton(text = "同意并继续", onClick = onAgree, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onDisagree, modifier = Modifier.fillMaxWidth()) {
            Text("不同意", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 门内内嵌政策视图（返回箭头+标题+可滚动分节正文）。[AI生成] */
@Composable
private fun InlinePolicyPanel(title: String, sections: List<PolicySection>, onBack: () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Column(
            modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            sections.forEachIndexed { i, section ->
                if (i > 0) Spacer(Modifier.height(20.dp))
                Text(section.heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(section.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** "继续即表示你已阅读并同意《用户协议》和《隐私政策》"——两处可点。[AI生成] */
@Composable
private fun ConsentLinksLine(onOpenUserAgreement: () -> Unit, onOpenPrivacyPolicy: () -> Unit) {
    val linkColor = MaterialTheme.colorScheme.primary
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = baseColor)) { append("继续即表示你已阅读并同意 ") }
        pushStringAnnotation("UA", "ua")
        withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)) { append("《用户协议》") }
        pop()
        withStyle(SpanStyle(color = baseColor)) { append(" 和 ") }
        pushStringAnnotation("PP", "pp")
        withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)) { append("《隐私政策》") }
        pop()
        withStyle(SpanStyle(color = baseColor)) { append("。") }
    }
    ClickableText(text = text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)) { offset ->
        text.getStringAnnotations("UA", offset, offset).firstOrNull()?.let { onOpenUserAgreement(); return@ClickableText }
        text.getStringAnnotations("PP", offset, offset).firstOrNull()?.let { onOpenPrivacyPolicy() }
    }
}
