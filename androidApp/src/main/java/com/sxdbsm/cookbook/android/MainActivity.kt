package com.sxdbsm.cookbook.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        setContent {
            // [AI修改] collectAsState 会把 Flow 转成 Compose State，主题变化后界面自动重组。
            val mode by prefs.observeThemeMode().collectAsState(initial = ThemeMode.SYSTEM)
            CookbookTheme(themeMode = mode) {
                MainScaffold()
            }
        }
    }
}
