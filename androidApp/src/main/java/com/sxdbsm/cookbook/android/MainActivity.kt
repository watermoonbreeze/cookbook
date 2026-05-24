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

class MainActivity : ComponentActivity() {

    private val prefs: PreferenceRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mode by prefs.observeThemeMode().collectAsState(initial = ThemeMode.SYSTEM)
            CookbookTheme(themeMode = mode) {
                MainScaffold()
            }
        }
    }
}
