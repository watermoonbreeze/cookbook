package com.sxdbsm.cookbook.di

import app.cash.sqldelight.db.SqlDriver
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.platform.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule: Module = module {
    single<SqlDriver> { get<DatabaseDriverFactory>().createDriver() }
    single { CookbookDatabase(get()) }
    single { PresetDataSeeder(get()) }
    single { DishRepository(get()) }
    single { IngredientRepository(get()) }
    single { FoodCategoryRepository(get()) }
    single { MealRecordRepository(get()) }
    single { PreferenceRepository(get()) }
    single { HealthProfileRepository(get()) }
}
