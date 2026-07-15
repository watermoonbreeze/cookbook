package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/**
 * @File : AppTopBar
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 统一顶栏（苹果风）——收敛各屏重复内联的 topAppBarColors/返回图标/标题字重
 * <p>
 * 标准：背景=background、标题 titleLarge(TopAppBar 默认字号，与各屏内联顶栏一致) SemiBold onBackground、
 * 返回/操作图标 primary tint。大标题页(首页/我的)用 LargeTopAppBar，不走本组件。
 * <p>
 * [AI生成] UX C1：统一顶栏，消除 10+ 屏的重复颜色配置。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        // [AI修改] 用 TopAppBar 默认标题字号(titleLarge)+SemiBold，与其余各屏内联顶栏完全一致(不缩小)。
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.primary,
        ),
    )
}
