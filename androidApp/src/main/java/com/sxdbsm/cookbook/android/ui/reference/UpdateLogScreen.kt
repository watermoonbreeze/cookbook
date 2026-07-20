package com.sxdbsm.cookbook.android.ui.reference

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.InsetGroup
import com.sxdbsm.cookbook.data.seed.SeedChangelogEntry
import com.sxdbsm.cookbook.data.seed.SeedUpdateCenter
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * @File : UpdateLogScreen
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 基础数据「更新记录」中心（F#8 透明准则·兜底可查入口）
 * <p>
 * 倒序列出全部内置 changelog：App 每次更新了哪些基础数据(新增/修复/调整)、都在此可查。
 * 进入即 `markNotified`(视为已看·消我的页红点)。守健康免责：基础数据仅供参考·非医嘱。
 * <p>
 * [AI生成] 透明准则 P0(F#8)：让"App 背着用户更新基础数据"可查可知。
 **/
@Composable
fun UpdateLogScreen(onBack: () -> Unit) {
    val center = koinInject<SeedUpdateCenter>()
    var entries by remember { mutableStateOf<List<SeedChangelogEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        // 读全部记录 + 标记已看(消红点)——DB/JSON 读放后台线程。
        entries = withContext(Dispatchers.Default) {
            center.markNotified(DateTime.nowEpochSeconds())
            center.allChangelog()
        }
    }

    Scaffold(topBar = { AppTopBar(title = "更新记录", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                "每次基础数据更新做了什么，都在这里。你自建的食材、菜品和用餐记录不受这些更新影响。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (entries.isEmpty()) {
                Text(
                    "暂无更新记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )
            } else {
                entries.forEach { entry -> UpdateLogCard(entry) }
            }
            Text(
                "基础数据为参考整理，仅供参考·非医嘱。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
            )
        }
    }
}

/** 单条更新记录卡片：版本名·日期 + 一句总述 + 分组条目(新增/修复/调整)。[AI生成] */
@Composable
private fun UpdateLogCard(entry: SeedChangelogEntry) {
    val title = listOf(entry.displayName, entry.date).filter { it.isNotBlank() }.joinToString(" · ")
    InsetGroup(title = title.ifBlank { "更新" }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (entry.summary.isNotBlank()) {
                Text(entry.summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            // 三组固定顺序：新增 / 修复 / 调整。
            listOf("added" to "新增", "fixed" to "修复", "adjusted" to "调整").forEach { (type, label) ->
                val items = entry.changes.filter { it.type == type && it.text.isNotBlank() }
                if (items.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        items.forEach { c ->
                            Text("· ${c.text}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
