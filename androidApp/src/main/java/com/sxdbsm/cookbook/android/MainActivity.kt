package com.sxdbsm.cookbook.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    private val openTimerRequested = mutableStateOf(false) // [AI生成] 通知点击请求打开烹饪计时页。

    companion object {
        const val EXTRA_OPEN_TIMER = "open_cooking_timer" // [AI生成] 计时通知点击打开计时页的 intent extra key。
    }
    private val legacyStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // [AI生成] Android 9 及以下运行时权限回调后重算状态；Android 11+ 走系统设置页后由 ON_RESUME 处理。
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // [AI修改] 开启沉浸式布局，让 Compose 内容延伸到状态栏/导航栏区域。
        window.statusBarColor = Color.TRANSPARENT // [AI修改] 状态栏透明，交给页面背景承接。
        window.navigationBarColor = Color.TRANSPARENT // [AI修改] 导航栏透明，底部栏自行提供背景。
        if (intent?.getBooleanExtra(EXTRA_OPEN_TIMER, false) == true) openTimerRequested.value = true // [AI生成] 计时通知点击进入。
        val initialStorageReady = preparePublicStorageIfAllowed()
        setContent {
            var storageReady by remember { mutableStateOf(initialStorageReady) }
            CookbookTheme(themeMode = ThemeMode.SYSTEM) {
                if (storageReady) {
                    CookbookAppContent()
                } else {
                    StoragePermissionScreen(
                        onRequestPermission = { requestCookbookStoragePermission() },
                        onCheckPermission = { storageReady = preparePublicStorageIfAllowed() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_TIMER, false)) openTimerRequested.value = true // [AI生成] App 已在前台时点击计时通知也能跳转。
    }

    /**
     * 授权后先准备公共目录，再允许 App 读取数据库。[AI生成]
     */
    private fun preparePublicStorageIfAllowed(): Boolean =
        runCatching {
            if (!CookbookStorage.hasPublicStorageAccess(this)) return false
            CookbookStorage.migrateAppSpecificCookbookToPublic(this)
            CookbookStorage.requirePublicSubDir(CookbookStorage.DB_DIR_NAME)
            CookbookStorage.requirePublicSubDir(CookbookStorage.IMG_DIR_NAME)
            CookbookStorage.requirePublicSubDir(CookbookStorage.LOG_DIR_NAME)
            AppLogger.init(this) // [AI生成] 授权并创建 log 目录后启动文件日志，预测试可导出 /sdcard/cookbook/log/。
            AppLogger.installCrashHandler() // [AI生成] 预测试期间把未捕获崩溃摘要写入同一份日期日志。
            true
        }.getOrDefault(false)

    /**
     * 根据 Android 版本申请创建 `/sdcard/cookbook` 所需权限。[AI生成]
     */
    private fun requestCookbookStoragePermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val uri = Uri.parse("package:$packageName")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                runCatching { startActivity(intent) }
                    .onFailure { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
            }
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> {
                legacyStoragePermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ),
                )
            }
            else -> Unit
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
            runCatching { seeder.seedIfNeeded() }
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
                message = initError ?: "初始化数据中...",
                failed = initError != null,
                onRetry = { initAttempt++ },
            )
            return
        } // [AI修改] 等旧库 NULL 清洗和预置数据初始化完成后，才允许进入首页/编辑页。

        // [AI修改] collectAsState 会把 Flow 转成 Compose State，主题变化后界面自动重组。
        val mode by prefs.observeThemeMode().collectAsState(initial = ThemeMode.SYSTEM)
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
        CookbookTheme(themeMode = mode) {
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

    @Composable
    private fun StoragePermissionScreen(
        onRequestPermission: () -> Unit,
        onCheckPermission: () -> Unit,
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) onCheckPermission()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "需要存储权限",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "应用会把数据库和照片统一保存到 /sdcard/cookbook/。授权后才会创建 db 和 img 目录，并迁移旧数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onRequestPermission) {
                    Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "打开所有文件访问权限" else "授权读写存储")
                }
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                ) {
                    LaunchedEffect(Unit) { onCheckPermission() }
                }
            }
        }
    }
}
