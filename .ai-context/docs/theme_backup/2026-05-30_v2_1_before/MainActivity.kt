package com.sxdbsm.cookbook.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.sxdbsm.cookbook.android.ui.nav.MainScaffold
import com.sxdbsm.cookbook.android.ui.theme.CookbookTheme
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.domain.model.ThemeMode
import org.koin.android.ext.android.inject

/**
 * Android 主 Activity。[AI修改]
 *
 * 只负责把 Compose 内容挂到窗口上，并根据偏好仓库中的主题设置包裹全局主题。
 */
class MainActivity : ComponentActivity() {

    private val prefs: PreferenceRepository by inject() // [AI修改] 从 Koin 获取 shared 层偏好仓库。

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // [AI修改] 开启沉浸式布局，让 Compose 内容延伸到状态栏/导航栏区域。
        window.statusBarColor = Color.TRANSPARENT // [AI修改] 状态栏透明，交给页面背景承接。
        window.navigationBarColor = Color.TRANSPARENT // [AI修改] 导航栏透明，底部栏自行提供背景。
        setContent {
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
                MainScaffold()
            }
        }
    }
}
