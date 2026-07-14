package com.sxdbsm.cookbook.di

import app.cash.sqldelight.db.SqlDriver
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.RecommendationDataSource
import com.sxdbsm.cookbook.ai.RecommendationOrchestrator
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.CookingTimerRepository
import com.sxdbsm.cookbook.data.repository.FavoriteComboRepository
import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.data.repository.ShoppingListRepository
import com.sxdbsm.cookbook.data.repository.StepTemplateRepository
import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.platform.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * shared 层 Koin 依赖注册表。[AI修改]
 *
 * `single { ... }` 表示整个进程共用一个实例，类似 Java DI 框架里的 Singleton scope。
 */
val sharedModule: Module = module {
    single<SqlDriver> { get<DatabaseDriverFactory>().createDriver() }
    single { CookbookDatabase(get()) }
    single { PresetDataSeeder(get()) }
    single { DishRepository(get()) }
    single { CookingTimerRepository(get()) }
    single { FavoriteComboRepository(get(), get()) }
    single { IngredientRepository(get()) }
    single { PantryRepository(get()) }
    single { FoodCategoryRepository(get()) }
    single { MealRecordRepository(get()) }
    single { ShoppingListRepository(get()) } // [AI生成] 采购清单聚合(今天及未来餐食的采购/缺料汇总)。
    single { PreferenceRepository(get()) }
    single { HealthProfileRepository(get()) }
    single { com.sxdbsm.cookbook.data.repository.NutritionRepository(get()) } // [AI生成] 营养素 L2 数值层：菜品/餐/日营养估算。
    single { StepTemplateRepository(get()) } // [AI生成] #2 操作步骤模板(预设+自建，编辑菜品"选择步骤"套用)。
    single { com.sxdbsm.cookbook.data.repository.IngredientGroupRepository(get()) } // [AI生成] B5 常用配料组(编辑菜品"配料组"一键加入)。
    // [AI生成] AI 推荐取数层(S0)：聚合库存/菜品/忌口/最近餐 → 规则引擎输入。
    single { RecommendationDataSource(get(), get(), get(), get(), get()) }
    // [AI生成] AI 运行时配置(云/端/Key)；AiRuntime 具体实现由 androidModule 绑定 SwitchableAiRuntime。
    single { AiRuntimeConfig(get()) }
    single { RecommendationOrchestrator(get()) }
    // [AI生成] 选择性同步：导出/合并导入 菜品/食材/库存/健康/收藏/餐食。
    single { com.sxdbsm.cookbook.sync.SyncRepository(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
