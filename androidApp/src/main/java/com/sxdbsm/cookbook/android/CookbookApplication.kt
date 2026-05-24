package com.sxdbsm.cookbook.android

import android.app.Application
import com.sxdbsm.cookbook.android.di.androidModule
import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import com.sxdbsm.cookbook.di.sharedModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class CookbookApplication : Application() {

    private val seeder: PresetDataSeeder by inject()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@CookbookApplication)
            modules(androidModule, sharedModule)
        }
        // 首次启动灌入预置数据
        appScope.launch {
            runCatching { seeder.seedIfNeeded() }
                .onFailure { it.printStackTrace() }
        }
    }
}
