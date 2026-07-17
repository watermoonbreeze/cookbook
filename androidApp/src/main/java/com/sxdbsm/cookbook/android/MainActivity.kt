package com.sxdbsm.cookbook.android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sxdbsm.cookbook.android.ui.nav.MainScaffold
import com.sxdbsm.cookbook.android.ui.theme.CookbookTheme
import com.sxdbsm.cookbook.android.util.AppLogger
import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.platform.CookbookStorage
import org.koin.android.ext.android.inject

/**
 * Android 主 Activity。[AI修改]
 *
 * 只负责把 Compose 内容挂到窗口上，并根据偏好仓库中的主题设置包裹全局主题。
 */
class MainActivity : ComponentActivity() {

    private val prefs: PreferenceRepository by inject() // [AI修改] 从 Koin 获取 shared 层偏好仓库。
    private val seeder: PresetDataSeeder by inject() // [AI修改] 授权并创建公共目录后再初始化预置数据。
    private val familyRepo: com.sxdbsm.cookbook.data.repository.FamilyRepository by inject() // [AI生成] 家庭档案：首启建默认成员「我」并迁旧数据。
    private val openTimerRequested = mutableStateOf(false) // [AI生成] 通知点击请求打开烹饪计时页。

    companion object {
        const val EXTRA_OPEN_TIMER = "open_cooking_timer" // [AI生成] 计时通知点击打开计时页的 intent extra key。
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // [AI修改] 开启沉浸式布局，让 Compose 内容延伸到状态栏/导航栏区域。
        window.statusBarColor = Color.TRANSPARENT // [AI修改] 状态栏透明，交给页面背景承接。
        window.navigationBarColor = Color.TRANSPARENT // [AI修改] 导航栏透明，底部栏自行提供背景。
        // [AI生成] ①沉浸式：Android 10+ 默认给透明状态/导航栏加半透明遮罩(scrim)，关掉才真正沉浸、底栏背景直达屏底。
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_TIMER, false) == true) openTimerRequested.value = true // [AI生成] 计时通知点击进入。
        prepareStorage() // [AI修改] P0：数据改存 app 专属目录，无需权限门禁，直接准备目录即可。
        setContent {
            CookbookTheme(themeMode = ThemeMode.SYSTEM) {
                CookbookAppContent()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_TIMER, false)) openTimerRequested.value = true // [AI生成] App 已在前台时点击计时通知也能跳转。
    }

    /**
     * 准备 app 专属存储目录并启动文件日志。[AI修改]
     *
     * P0 存储合规后数据落在 app 专属目录，创建无需任何权限，失败也不阻塞（由后续初始化重试兜底）。
     */
    private fun prepareStorage() {
        runCatching {
            CookbookStorage.requireSubDir(CookbookStorage.DB_DIR_NAME)
            CookbookStorage.requireSubDir(CookbookStorage.IMG_DIR_NAME)
            CookbookStorage.requireSubDir(CookbookStorage.LOG_DIR_NAME)
            AppLogger.init(this) // [AI生成] 创建 log 目录后启动文件日志，内测可在应用内「日志查看」读取。
            AppLogger.installCrashHandler() // [AI生成] 把未捕获崩溃摘要写入同一份日期日志。
        }
    }

    @Composable
    private fun CookbookAppContent() {
        var initialized by remember { mutableStateOf(false) }
        var initError by remember { mutableStateOf<String?>(null) }
        var initAttempt by remember { mutableStateOf(0) }
        LaunchedEffect(initAttempt) {
            initError = null
            // [AI修改] seed 放到权限和公共目录准备之后，避免 Application 启动时提前创建数据库。
            runCatching { seeder.seedIfNeeded(); familyRepo.ensureInitialized() }
                .onSuccess {
                    initialized = true
                    AppLogger.event("app_start", mapOf("storageReady" to true, "initAttempt" to initAttempt)) // [AI生成] 内测埋点：记录应用启动和初始化成功。
                }
                .onFailure {
                    AppLogger.e("AppInit", "seed failed: attempt=$initAttempt", it) // [AI生成] 初始化失败写入本地日志，便于预测试排查。
                    it.printStackTrace()
                    initError = "初始化数据失败，请重启应用重试"
                }
        }
        if (!initialized) {
            InitializingScreen(
                message = initError ?: "初始化数据中…",
                failed = initError != null,
                onRetry = { initAttempt++ },
            )
            return
        } // [AI修改] 等旧库 NULL 清洗和预置数据初始化完成后，才允许进入首页/编辑页。

        // [AI修改] collectAsState 会把 Flow 转成 Compose State，主题变化后界面自动重组。
        val mode by prefs.observeThemeMode().collectAsState(initial = ThemeMode.SYSTEM)
        // [AI生成] 配色主题：切换即时生效(自动重组)。默认赤陶橘。
        val palette by prefs.observePalette().collectAsState(initial = com.sxdbsm.cookbook.domain.model.AppPalette.TERRACOTTA)
        val systemDark = isSystemInDarkTheme()
        val useDark = when (mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        SideEffect {
            // [AI修改] 沉浸式透明系统栏需要同步图标明暗，避免浅色/深色主题下状态栏图标看不清。
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
        CookbookTheme(themeMode = mode, palette = palette) {
            // [AI修改] 系统状态栏/导航栏色跟随 Compose 主题背景色(而非透明露出 android 默认 windowBackground)：
            // 修"app深色但系统仍浅色时,全屏页导航栏区域露白"——让系统栏与当前界面浑然一体、明暗自适应。
            val barColor = androidx.compose.material3.MaterialTheme.colorScheme.background
            SideEffect {
                window.statusBarColor = barColor.toArgb()
                window.navigationBarColor = barColor.toArgb()
            }
            MainScaffold(
                openTimer = openTimerRequested.value,
                onTimerConsumed = { openTimerRequested.value = false },
            )
        }
    }

    @Composable
    private fun InitializingScreen(message: String, failed: Boolean, onRetry: () -> Unit) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (!failed) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (failed) {
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                } // [AI生成] 初始化失败允许用户重试，避免停留在不可恢复页面。
            }
        }
    }

}
