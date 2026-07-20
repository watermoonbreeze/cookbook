package com.sxdbsm.cookbook.android.ui.crash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.theme.CookbookTheme
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.android.util.CrashInfo
import com.sxdbsm.cookbook.android.util.CrashReporting
import kotlin.system.exitProcess

/**
 * @File : CrashActivity
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 崩溃兜底友好界面（替代系统"应用已停止"闪退）
 * <p>
 * 全局未捕获异常时由崩溃处理器拉起（独立 `:crash` 进程，原进程已崩不可用）。
 * 展示克制、诚实、不吓人的提示（守苹果文案准则），并给"发送报告/重启/关闭"三个明确出口；
 * 发送报告走 {@link CrashReporting} 抽象层（当前本地占位，后续接后台/友盟）。
 * <p>
 * [AI生成] 用户 2026-07-20 要求：崩溃至少不闪退、有友好提醒、可上报。
 **/
class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val info = CrashInfo(
            time = intent.getStringExtra(EXTRA_TIME).orEmpty(),
            threadName = intent.getStringExtra(EXTRA_THREAD).orEmpty(),
            exceptionType = intent.getStringExtra(EXTRA_TYPE).orEmpty(),
            message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty(),
            stackTrace = intent.getStringExtra(EXTRA_STACK).orEmpty(),
            deviceInfo = intent.getStringExtra(EXTRA_DEVICE).orEmpty(),
            appVersion = intent.getStringExtra(EXTRA_VERSION).orEmpty(),
        )
        setContent {
            CookbookTheme(themeMode = ThemeMode.SYSTEM) {
                CrashScreen(
                    info = info,
                    onRestart = { restartApp(); killSelf() },
                    onClose = { killSelf() },
                )
            }
        }
    }

    private fun restartApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        runCatching { if (launch != null) startActivity(launch) }
    }

    private fun killSelf() {
        finishAffinity()
        // 崩溃进程已不可用，彻底退出让系统回收；重启走全新进程。
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }

    companion object {
        private const val EXTRA_TIME = "crash_time"
        private const val EXTRA_THREAD = "crash_thread"
        private const val EXTRA_TYPE = "crash_type"
        private const val EXTRA_MESSAGE = "crash_message"
        private const val EXTRA_STACK = "crash_stack"
        private const val EXTRA_DEVICE = "crash_device"
        private const val EXTRA_VERSION = "crash_version"

        /** 由崩溃处理器构造拉起本界面的 Intent（独立任务）。[AI生成] */
        fun intent(context: Context, info: CrashInfo): Intent =
            Intent(context, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(EXTRA_TIME, info.time)
                putExtra(EXTRA_THREAD, info.threadName)
                putExtra(EXTRA_TYPE, info.exceptionType)
                putExtra(EXTRA_MESSAGE, info.message)
                putExtra(EXTRA_STACK, info.stackTrace.take(6000)) // Intent 额度有限，截断堆栈；完整栈在日志文件
                putExtra(EXTRA_DEVICE, info.deviceInfo)
                putExtra(EXTRA_VERSION, info.appVersion)
            }
    }
}

@Composable
private fun CrashScreen(info: CrashInfo, onRestart: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var reporting by remember { mutableStateOf(false) }
    var reported by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                Icons.Outlined.SentimentDissatisfied,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text("抱歉，出了点小问题", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "应用刚才意外中断了，已经自动记录下来。你可以把这次问题发给我们，帮助把它修好；也可以直接重启继续用。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // 主 CTA：发送报告（走抽象层，当前本地占位）。
            Button(
                onClick = {
                    if (reported || reporting) return@Button
                    reporting = true
                    CrashReporting.reporter.report(context.applicationContext, info) { ok ->
                        reporting = false
                        reported = ok
                        Toast.makeText(
                            context,
                            if (ok) "谢谢，已记录这次问题" else "记录失败，可稍后再试",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                enabled = !reporting && !reported,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (reported) "已发送，谢谢" else if (reporting) "发送中…" else "发送问题报告")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("重启应用") }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onClose) { Text("关闭") }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { showDetail = !showDetail }) {
                Text(if (showDetail) "收起详情" else "查看错误详情", style = MaterialTheme.typography.labelMedium)
            }
            if (showDetail) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "${info.exceptionType}: ${info.message}\n${info.deviceInfo} · ${info.appVersion}\n\n${info.stackTrace}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}
