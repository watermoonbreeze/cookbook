package com.sxdbsm.cookbook.di

import app.cash.sqldelight.db.SqlDriver
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.MockAiRuntime
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
    single { PreferenceRepository(get()) }
    single { HealthProfileRepository(get()) }
    // [AI生成] AI 推荐取数层(S0)：聚合库存/菜品/忌口/最近餐 → 规则引擎输入。
    single { RecommendationDataSource(get(), get(), get(), get(), get()) }
    // [AI生成] AI 推荐(S1)：AiRuntime 默认 Mock(走规则兜底)，S2 由 androidApp 换成 CloudAiRuntime。
    single<AiRuntime> { MockAiRuntime() }
    single { RecommendationOrchestrator(get()) }
}
