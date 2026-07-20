package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * @File : FormBottomBar
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 表单底部常驻 CTA 栏（§9.13）——主 CTA 用胶囊 CapsuleButton、右对齐、一屏一个；可选次操作纯文字在其左侧。
 * <p>
 * 统一"保存 / 保存并继续"摆位：primaryText+onPrimary(胶囊主按钮)，可选 secondaryText+onSecondary(次文字，
 * 仅当两者都传入才渲染——能力显隐由回调是否传入决定，便于建材/建菜等表单按需启用连续录入)。
 * 顶部 1dp 分隔线替代阴影(苹果式克制)；navigationBarsPadding 只在本组件消费一次防双下边距。
 * <p>
 * [AI生成] B 批 B-6：食材"保存并继续"复用基建。
 **/
@Composable
fun FormBottomBar(
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    secondaryEnabled: Boolean = true,
    // [AI生成] 是否自消费导航栏 inset。全屏 Dialog 场景=true(自己避让);已在 NavHost 层被 navigationBarsPadding 避让的路由页=false(防双重下边距·§9.30 红线)。
    navBarPadding: Boolean = true,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column {
            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (navBarPadding) Modifier.navigationBarsPadding() else Modifier) // [AI修改] 只在需要处消费导航栏 inset，防与外层 navigationBarsPadding 双下边距
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                // [AI生成] 次操作：仅当文案+回调都传入才显（能力由回调是否传入决定，非内部 mode 硬编码）。
                if (secondaryText != null && onSecondary != null) {
                    // TextButton 默认 contentColor=primary，禁用态自动变浅，不显式覆盖色以保留禁用视觉。
                    TextButton(onClick = onSecondary, enabled = secondaryEnabled) {
                        Text(secondaryText)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                CapsuleButton(text = primaryText, onClick = onPrimary, enabled = primaryEnabled)
            }
        }
    }
}
