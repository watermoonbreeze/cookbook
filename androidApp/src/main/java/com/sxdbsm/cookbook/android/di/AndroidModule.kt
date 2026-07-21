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
    // [AI生成] 阶段3-d 匿名统计后端=友盟(AppKey 从 BuildConfig/local.properties·同意后才 init·空 key 则仅日志)。
    single<com.sxdbsm.cookbook.analytics.AnalyticsSink> { com.sxdbsm.cookbook.android.analytics.UmengAnalyticsSink(androidContext()) }
    single { BackupManager(context = androidContext(), driverProvider = { get() }) }
    single { LogFileManager() } // [AI生成] 我的页日志查看读取 /sdcard/cookbook/log/。
    single { com.sxdbsm.cookbook.android.ui.ingredients.IngredientJumpBus() } // [AI生成] 跨屏跳到具体食材总线。
    single { com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefillBus() } // [AI生成] 新建菜品预填(搜索点此新建/食材组成菜品)总线。
    single { com.sxdbsm.cookbook.android.ui.ingredients.IngredientCreateBus() } // [AI生成] 首页搜索"新建食材"按名开编辑器总线。

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

    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get()) } // [AI修改] 追加 HealthProfileRepository(A-1) + RecommendationDataSource(阶段2 首页下一餐推荐卡)。
    viewModel { DishesViewModel(get()) } // [AI修改] 移除未使用的 MealRecordRepository 死依赖。
    viewModel { com.sxdbsm.cookbook.android.ui.weekplan.WeekPlanViewModel(get()) } // [AI生成] B3 一周计划
    viewModel { DishDetailViewModel(get(), get(), get(), get(), get(), get(), get()) } // [AI修改] 详情洞察: 库存/健康/统计/营养(含营养估算)+PreferenceRepository(库存挂钩开关)+MemberDishHealthUseCase(成员化红绿灯)
    viewModel { NewDishViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { AddMealViewModel(get(), get(), get(), get()) } // [AI修改] 添加餐食页还需要收藏组合仓库支持组合复用 + 阶段3-b Analytics(meal_logged)。
    viewModel { TimelineViewModel(get()) }
    viewModel { MineViewModel(get(), get(), get(), get(), get(), get(), get()) } // [AI修改] 追加 FamilyRepository + 阶段3-c Analytics(匿名统计开关)。
    viewModel { com.sxdbsm.cookbook.android.ui.report.DietReportViewModel(get(), get(), get()) } // [AI生成] 报告模块:MealRecord+Nutrition+Family
    viewModel { com.sxdbsm.cookbook.android.ui.family.FamilyViewModel(get(), get(), get(), get()) } // [AI修改] 家庭成员管理 + 阶段3-b Analytics + D1-2 IngredientRepository(忌口查名收进VM)
    viewModel { com.sxdbsm.cookbook.android.ui.family.FamilyStatsViewModel(get(), get(), get()) } // [AI生成] 膳食统计
    viewModel { IngredientPickerViewModel(get(), get(), get(), get(), get(), get()) } // [AI修改] 追加 Pantry/HealthProfile/Nutrition 支持库存 Tab、忌口高亮、自定义营养录入。
    viewModel { DishPickerViewModel(get()) }
    viewModel { SearchViewModel(get(), get(), get()) }
    viewModel { AiRecommendViewModel(get(), get(), get(), get(), get(), get()) } // [AI修改] AI 推荐(取数层+编排器+AI配置+偏好+Analytics+§9.36 NutritionRepository 每菜营养)。
    viewModel { AiSettingsViewModel(get()) } // [AI生成] AI 设置(运行时配置)。
    viewModel { com.sxdbsm.cookbook.android.ui.nutrition.NutritionTableViewModel(get()) } // [AI生成] 食材营养表(全量营养总览)。
    viewModel { com.sxdbsm.cookbook.android.ui.settings.FeatureSettingsViewModel(get()) } // [AI修改] 身体数据编辑移入家庭档案，不再需要 FamilyRepository。
    viewModel { com.sxdbsm.cookbook.android.ui.shopping.ShoppingListViewModel(get(), get(), get(), get()) } // [AI生成] 采购清单聚合 + 按份数入库 + 按家人忌口标注(#4)。
    viewModel { com.sxdbsm.cookbook.android.ui.pairing.FreePairingViewModel(get(), get()) } // [AI生成] 食材自由搭配(离线规则)+存为菜品(get()=IngredientRepository)。
    single { com.sxdbsm.cookbook.android.sync.SelectiveSyncBundler(get()) } // [AI生成] 选择性同步打包/合并。
    viewModel { com.sxdbsm.cookbook.android.ui.sync.DeviceSyncViewModel(get(), get()) } // [AI修改] 双设备同传(整库替换/选择性合并)。
    viewModel { com.sxdbsm.cookbook.android.ui.ai.AiPlanViewModel(get(), get(), get(), get(), get(), get()) } // [AI生成] 周期规划(取数/餐食/AI运行时/AI配置/偏好/§9.36 NutritionRepository 每菜营养)。
    viewModel { com.sxdbsm.cookbook.android.ui.cook.CookModeViewModel(get()) } // [AI生成] D1:分步烹饪页薄 VM(数据访问收进 VM·F-Arch3)。
}
