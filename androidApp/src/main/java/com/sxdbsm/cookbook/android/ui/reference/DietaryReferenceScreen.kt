package com.sxdbsm.cookbook.android.ui.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.InsetGroup

/**
 * @File : DietaryReferenceScreen
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 膳食参考依据——把 App 用到的营养阈值/分级引用的权威标准透明列给用户
 * <p>
 * 分类清楚(钠钾/糖GI/嘌呤/血脂/能量宏量/营养标签)，每条给阈值+口径+适用+依据(标准全称+编号+机构+年)，
 * 顶部免责、底部全部来源清单。纯静态展示(数据在 [DietaryReference])，无 DB/迁移。
 * <p>
 * [AI生成] 让慢病家庭"看得到我们凭什么这么判"，提升可信与接受度；守健康数据免责红线。
 **/
@Composable
fun DietaryReferenceScreen(onBack: () -> Unit) {
    ReferenceScaffold(
        title = "膳食参考依据",
        disclaimer = DietaryReference.disclaimer,
        categories = DietaryReference.categories,
        sources = DietaryReference.sources,
        footNote = "注：以上为公开标准/指南的口径整理，个别数值以原文最新版为准；标「待核」项尚未取得一手确切数值，未采用估算参与任何判定。",
        onBack = onBack,
    )
}

/**
 * 参考类页面通用骨架（膳食参考依据 / 数据来源 共用）。[AI生成]
 * 顶部免责 + 分类分组(每条指标+级别+口径+适用+依据) + 底部全部来源清单 + 脚注。
 */
@Composable
fun ReferenceScaffold(
    title: String,
    disclaimer: String,
    categories: List<DietaryRefCategory>,
    sources: List<DietaryRefSource>,
    footNote: String,
    onBack: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { AppTopBar(title = title, onBack = onBack) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DisclaimerBanner(disclaimer)

                categories.forEach { cat ->
                    InsetGroup(title = cat.title) {
                        if (cat.intro.isNotBlank()) {
                            Text(
                                cat.intro,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        cat.items.forEachIndexed { i, item ->
                            if (i > 0) Spacer(Modifier.height(6.dp))
                            RefItemBlock(item)
                        }
                    }
                }

                // 全部参考来源清单（去重）
                InsetGroup(title = "全部参考来源") {
                    sources.forEachIndexed { i, s ->
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text(
                                "${i + 1}. ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column {
                                Text(s.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    listOfNotNull(s.no.ifBlank { null }, s.org, s.year.ifBlank { null })
                                        .joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (footNote.isNotBlank()) {
                    Text(
                        footNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun DisclaimerBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RefItemBlock(item: DietaryRefItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(item.indicator, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (item.pending) {
                Spacer(Modifier.width(6.dp))
                PendingTag()
            }
        }
        item.levels.forEach { lv ->
            Row {
                Text(
                    "· ${lv.label}：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(lv.value, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            "口径：${item.caliber}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.appliesTo.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item.appliesTo.forEach { AppliesChip(it) }
            }
        }
        Text(
            "依据：${item.source}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AppliesChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PendingTag() {
    Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(
            "待核",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
