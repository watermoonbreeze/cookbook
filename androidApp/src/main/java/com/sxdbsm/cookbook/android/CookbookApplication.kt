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

/**
 * Android 应用入口。[AI修改]
 *
 * 类似 Java Android 项目里的自定义 Application：负责启动 Koin 依赖注入，
 * 并在后台协程中初始化预置数据。
 */
class CookbookApplication : Application() {

    private val seeder: PresetDataSeeder by inject() // [AI修改] Koin 属性注入，首次访问时从容器取实例。
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default) // [AI修改] 应用级后台协程作用域。

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@CookbookApplication)
            modules(androidModule, sharedModule)
        }
        // [AI修改] 首次启动灌入预置数据；失败只打印日志，避免阻塞应用进入首页。
        appScope.launch {
            runCatching { seeder.seedIfNeeded() }
                .onFailure { it.printStackTrace() }
        }
    }
}
