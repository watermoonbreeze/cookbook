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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        // [AI生成] 阶段3 匿名统计：App 启动读"匿名统计"同意态 → 驱动埋点闸门(默认关·同意后才放行事件)。
        //   合规红线:未同意即闸门关、绝不上报;友盟 init 也应延到同意之后(见 UmengAnalyticsSink 接线说明)。
        initAnalyticsConsent()
        // [AI修改] 预置数据初始化延后到 MainActivity 获取 /sdcard 访问权限之后，避免授权前提前创建内部/外部数据库。
    }

    /**
     * 读匿名统计同意态并设置埋点闸门(一次性)。[AI生成] 阶段3
     *
     * 在 **Main** 协程里跑：`isAnalyticsEnabled()` 内部已 `withContext(io)` 读偏好，返回后 `setEnabled` 在主线程写；
     * 埋点 `track()` 也在主线程读 `enabled` → 读写同线程、无跨线程可见性问题(闸门 `enabled` 是普通 var 的前提)。
     */
    private fun initAnalyticsConsent() {
        val koin = org.koin.core.context.GlobalContext.get()
        val analytics = koin.get<com.sxdbsm.cookbook.analytics.Analytics>()
        val prefs = koin.get<com.sxdbsm.cookbook.data.repository.PreferenceRepository>()
        CoroutineScope(Dispatchers.Main).launch {
            analytics.setEnabled(prefs.isAnalyticsEnabled())
        }
    }
}
