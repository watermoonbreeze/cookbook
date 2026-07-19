package com.sxdbsm.cookbook.android.ui.policy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sxdbsm.cookbook.android.ui.component.AppTopBar

/**
 * @File : PolicyScreen
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 政策查看页（《用户协议》《隐私政策》复用同一页·传 title + 分节正文）
 * <p>
 * 二级页复用 AppTopBar；正文纯文本分节排版（Compose 无 markdown），阅读优先：节标题+正文层级、舒适留白。
 * <p>
 * [AI生成] 阶段3-c：apple_ux_designer §9.25 政策页规范。
 **/
@Composable
fun PolicyScreen(
    title: String,
    sections: List<PolicySection>,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { AppTopBar(title = title, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                POLICY_UPDATED,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(16.dp))
            sections.forEachIndexed { i, section ->
                if (i > 0) Spacer(Modifier.height(24.dp))
                Text(
                    section.heading,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    section.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp,
                )
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}
