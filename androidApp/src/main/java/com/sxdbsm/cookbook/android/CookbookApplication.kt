package com.sxdbsm.cookbook.android

import android.app.Application
import android.content.pm.ApplicationInfo
import com.sxdbsm.cookbook.android.di.androidModule
import com.sxdbsm.cookbook.di.sharedModule
import com.sxdbsm.cookbook.platform.CookbookDiag
import com.sxdbsm.cookbook.platform.CookbookStorage
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
        // [AI生成] 仅可调试(debug)包开启 shared 诊断日志(如库存推荐 PantryRec)；release 包默认关闭，避免生产开销与信息泄露。
        CookbookDiag.enabled = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        CookbookStorage.init(this) // [AI生成] P0：尽早存下 app Context，存储目录改用 app 专属目录、无需权限门禁。
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@CookbookApplication)
            modules(androidModule, sharedModule)
        }
        // [AI修改] 预置数据初始化延后到 MainActivity 获取 /sdcard 访问权限之后，避免授权前提前创建内部/外部数据库。
    }
}
