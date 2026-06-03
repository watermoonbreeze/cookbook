package com.sxdbsm.cookbook.android

import android.app.Application
import com.sxdbsm.cookbook.android.di.androidModule
import com.sxdbsm.cookbook.di.sharedModule
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
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@CookbookApplication)
            modules(androidModule, sharedModule)
        }
        // [AI修改] 预置数据初始化延后到 MainActivity 获取 /sdcard 访问权限之后，避免授权前提前创建内部/外部数据库。
    }
}
