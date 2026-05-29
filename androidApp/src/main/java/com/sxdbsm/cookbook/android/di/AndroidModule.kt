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

    viewModel { HomeViewModel(get(), get()) }
    viewModel { DishesViewModel(get(), get()) }
    viewModel { DishDetailViewModel(get()) }
    viewModel { NewDishViewModel(get(), get(), get(), get()) }
    viewModel { AddMealViewModel(get()) }
    viewModel { TimelineViewModel(get()) }
    viewModel { MineViewModel(get(), get(), get()) }
    viewModel { IngredientPickerViewModel(get(), get()) }
    viewModel { DishPickerViewModel(get()) }
    viewModel { SearchViewModel(get(), get(), get()) }
}
