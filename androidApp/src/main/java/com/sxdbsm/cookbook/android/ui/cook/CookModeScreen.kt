package com.sxdbsm.cookbook.android.ui.cook

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.StoredImage
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import com.sxdbsm.cookbook.data.repository.DishRepository
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * @File : CookModeScreen
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 分步烹饪全屏模式
 * <p>
 * 一步一屏、大字大图，左右翻页；每步自动从步骤文字解析时长，一键开计时。
 * 全程 KEEP_SCREEN_ON 不息屏；计时按 elapsedRealtime 结束时刻算剩余（非每秒递减），
 * 短暂切后台回来剩余仍准。到点响一次系统提示音。
 * <p>
 * [AI生成] D3 烹饪体验：分步烹饪 + 步骤一键计时。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookModeScreen(
    dishId: Long,
    onBack: () -> Unit,
    repo: DishRepository = koinInject(),
) {
    // [AI修改] 用 remember(dishId) 缓存冷流，避免每次重组新建 Flow 反复订阅查库(Compose 红线)。
    val dish by remember(dishId) { repo.observeDishById(dishId) }.collectAsStateWithLifecycle(null)
    val steps = dish?.steps.orEmpty().sortedBy { it.sortOrder }

    // [AI生成] 烹饪中保持亮屏，退出恢复。
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    var index by remember { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(0, (steps.size - 1).coerceAtLeast(0))

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = dish?.name?.let { "分步烹饪 · $it" } ?: "分步烹饪",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.Close, contentDescription = "退出") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        if (steps.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("该菜品暂无操作步骤", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        val step = steps[safeIndex]
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = (safeIndex + 1f) / steps.size,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "第 ${safeIndex + 1} / ${steps.size} 步",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = step.text.ifBlank { "（本步无文字说明）" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(16.dp))
                val images = decodeImagePaths(step.imagePath)
                val thumbs = decodeImagePaths(step.thumbnailPath)
                images.forEachIndexed { i, path ->
                    StoredImage(
                        imagePath = path,
                        thumbnailPath = thumbs.getOrNull(i).orEmpty(),
                        fallbackText = "步骤",
                        fallbackEmoji = "🍳",
                        seedId = step.id + i,
                        size = 220.dp,
                        corner = 16.dp,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // [AI生成] 从步骤文字解析时长，命中则展示一键计时。
                val seconds = parseStepSeconds(step.text)
                if (seconds != null) {
                    StepTimerCard(defaultSeconds = seconds, resetKey = safeIndex)
                }
                Spacer(Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { if (safeIndex > 0) index = safeIndex - 1 },
                    enabled = safeIndex > 0,
                    modifier = Modifier.weight(1f),
                ) { Text("上一步") }
                if (safeIndex < steps.size - 1) {
                    Button(onClick = { index = safeIndex + 1 }, modifier = Modifier.weight(1f)) { Text("下一步") }
                } else {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text("完成") }
                }
            }
        }
    }
}

/**
 * 单步内嵌计时卡片。[AI生成]
 *
 * 按 elapsedRealtime 结束时刻算剩余（不每秒递减，切后台回来仍准）；到点响一次系统提示音。
 * resetKey 变化（翻到别的步骤）时重置为未开始态。
 */
@Composable
private fun StepTimerCard(defaultSeconds: Int, resetKey: Int) {
    val context = LocalContext.current
    var endAt by remember(resetKey) { mutableStateOf<Long?>(null) }
    var remaining by remember(resetKey) { mutableIntStateOf(defaultSeconds) }
    var finished by remember(resetKey) { mutableStateOf(false) }

    LaunchedEffect(endAt) {
        val target = endAt ?: return@LaunchedEffect
        while (true) {
            val rem = (((target - SystemClock.elapsedRealtime()) + 999) / 1000).toInt().coerceAtLeast(0)
            remaining = rem
            if (rem <= 0) {
                finished = true
                endAt = null
                playFinishSound(context)
                break
            }
            delay(400)
        }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (finished) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (finished) "计时结束" else formatMmSs(remaining),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    finished -> MaterialTheme.colorScheme.error
                    endAt != null -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.weight(1f))
            if (endAt != null) {
                Button(
                    onClick = { endAt = null; remaining = defaultSeconds },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("停止")
                }
            } else {
                Button(onClick = {
                    finished = false
                    remaining = defaultSeconds
                    endAt = SystemClock.elapsedRealtime() + defaultSeconds * 1000L
                }) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (finished) "重新计时" else "开始计时")
                }
            }
        }
    }
}

/**
 * 从步骤文字解析计时秒数。[AI生成]
 *
 * 命中 "N分钟/N分" 取分钟；命中 "N秒" 取秒；同时命中取更靠前者。取第一处时长，找不到返回 null。
 */
private fun parseStepSeconds(text: String): Int? {
    if (text.isBlank()) return null
    val minute = Regex("(\\d+)\\s*分(钟|)").find(text)
    val second = Regex("(\\d+)\\s*秒").find(text)
    val minSec = minute?.groupValues?.get(1)?.toIntOrNull()?.let { it * 60 }
    val secSec = second?.groupValues?.get(1)?.toIntOrNull()
    return when {
        minSec != null && secSec != null ->
            if ((minute.range.first) <= (second.range.first)) minSec else secSec
        minSec != null -> minSec
        secSec != null -> secSec
        else -> null
    }?.takeIf { it in 1..7200 }
}

private fun formatMmSs(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

private fun playFinishSound(context: Context) {
    runCatching {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
        RingtoneManager.getRingtone(context.applicationContext, uri)?.also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.isLooping = false
            it.play()
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
