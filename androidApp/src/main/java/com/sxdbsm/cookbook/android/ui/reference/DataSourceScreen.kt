package com.sxdbsm.cookbook.android.ui.reference

import androidx.compose.runtime.Composable

/**
 * @File : DataSourceScreen
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 数据来源页——食材分类/营养/GI/嘌呤/预设菜品各自来源，复用参考页骨架
 * <p>
 * [AI生成] 用户要求：数据来源也做成和「膳食参考依据」一样的分类页，各标明来源；从「关于」链接进入。
 **/
@Composable
fun DataSourceScreen(onBack: () -> Unit) {
    ReferenceScaffold(
        title = "数据来源",
        disclaimer = DataSourceReference.disclaimer,
        categories = DataSourceReference.categories,
        sources = DataSourceReference.sources,
        footNote = "注：以上为参考整理、非逐项官方核验；标「规划中」为后续联网功能上线后开放。",
        onBack = onBack,
    )
}
