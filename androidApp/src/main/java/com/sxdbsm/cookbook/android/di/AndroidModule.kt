package com.sxdbsm.cookbook.android.di

import com.sxdbsm.cookbook.android.ui.home.HomeViewModel
import com.sxdbsm.cookbook.android.ui.dishes.DishesViewModel
import com.sxdbsm.cookbook.android.ui.dishdetail.DishDetailViewModel
import com.sxdbsm.cookbook.android.ui.newdish.NewDishViewModel
import com.sxdbsm.cookbook.android.ui.addmeal.AddMealViewModel
import com.sxdbsm.cookbook.android.ui.timeline.TimelineViewModel
import com.sxdbsm.cookbook.android.ui.mine.MineViewModel
import com.sxdbsm.cookbook.android.ui.picker.IngredientPickerViewModel
import com.sxdbsm.cookbook.android.ui.picker.DishPickerViewModel
import com.sxdbsm.cookbook.android.ui.search.SearchViewModel
import com.sxdbsm.cookbook.android.ui.ai.AiRecommendViewModel
import com.sxdbsm.cookbook.android.ui.ai.AiSettingsViewModel
import com.sxdbsm.cookbook.android.util.LogFileManager
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeType
import com.sxdbsm.cookbook.ai.MockAiRuntime
import com.sxdbsm.cookbook.ai.SwitchableAiRuntime
import com.sxdbsm.cookbook.android.ai.CloudAiRuntime
import com.sxdbsm.cookbook.android.ai.OnDeviceAiRuntime
import com.sxdbsm.cookbook.platform.BackupManager
import com.sxdbsm.cookbook.platform.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Android 层 Koin 依赖注册表。[AI修改]
 *
 * 注册平台实现和各页面 ViewModel。sharedModule 注册跨平台仓库，这里注册 Android 专属对象。
 */
val androidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { BackupManager(context = androidContext(), driverProvider = { get() }) }
    single { LogFileManager() } // [AI生成] 我的页日志查看读取 /sdcard/cookbook/log/。
    single { com.sxdbsm.cookbook.android.ui.ingredients.IngredientJumpBus() } // [AI生成] 跨屏跳到具体食材总线。

    // [AI生成] AI 运行时切换框架：按 AiRuntimeConfig 路由到 Mock/云端/端侧；加端侧只需扩这里的映射。
    single { CloudAiRuntime(get()) }
    single { OnDeviceAiRuntime() }
    single<AiRuntime> {
        SwitchableAiRuntime(
            config = get(),
            runtimes = mapOf(
                AiRuntimeType.MOCK to MockAiRuntime(),
                AiRuntimeType.CLOUD to get<CloudAiRuntime>(),
                AiRuntimeType.ON_DEVICE to get<OnDeviceAiRuntime>(),
            ),
        )
    }

    viewModel { HomeViewModel(get(), get(), get()) } // [AI修改] 首页主题弹框需要读取/写入主题偏好。
    viewModel { DishesViewModel(get(), get()) }
    viewModel { DishDetailViewModel(get()) }
    viewModel { NewDishViewModel(get(), get(), get(), get()) }
    viewModel { AddMealViewModel(get(), get(), get()) } // [AI修改] 添加餐食页还需要收藏组合仓库支持组合复用。
    viewModel { TimelineViewModel(get()) }
    viewModel { MineViewModel(get(), get(), get(), get(), get()) } // [AI修改] 追加 PresetDataSeeder 支持“更新基础数据”。
    viewModel { IngredientPickerViewModel(get(), get(), get(), get(), get()) } // [AI修改] 追加 Pantry/HealthProfile 支持库存 Tab 与忌口高亮。
    viewModel { DishPickerViewModel(get()) }
    viewModel { SearchViewModel(get(), get(), get()) }
    viewModel { AiRecommendViewModel(get(), get()) } // [AI生成] AI 推荐(取数层+编排器)。
    viewModel { AiSettingsViewModel(get()) } // [AI生成] AI 设置(运行时配置)。
    viewModel { com.sxdbsm.cookbook.android.ui.ai.AiPlanViewModel(get(), get(), get(), get()) } // [AI生成] 周期规划(取数/餐食/AI运行时/AI配置)。
}
