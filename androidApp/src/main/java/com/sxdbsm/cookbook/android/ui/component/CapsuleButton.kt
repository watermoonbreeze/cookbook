package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * @File : CapsuleButton
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 苹果风格主按钮（胶囊填充，accent 底白字）
 * <p>
 * 用于每屏唯一主 CTA(保存/确定)。iOS 主按钮多为胶囊填充，视觉重、指向明确。
 * <p>
 * [AI生成] 苹果风格改造 Phase1 组件库。
 **/
@Composable
fun CapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50), // 胶囊
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * 苹果风格描边主按钮（胶囊描边，主色边+主色字）。[AI生成] L1：同意面板只读态"知道了"用。
 *
 * 与 [CapsuleButton] 同款胶囊/字重，仅底/边/字色反转为描边态——多用于"次主 CTA"或只读确认。
 */
@Composable
fun CapsuleOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50), // 胶囊
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
    }
}
