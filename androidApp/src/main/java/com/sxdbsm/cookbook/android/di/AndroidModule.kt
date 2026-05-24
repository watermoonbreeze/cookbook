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
import com.sxdbsm.cookbook.platform.BackupManager
import com.sxdbsm.cookbook.platform.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { BackupManager(context = androidContext(), driverProvider = { get() }) }

    viewModel { HomeViewModel(get(), get()) }
    viewModel { DishesViewModel(get(), get()) }
    viewModel { DishDetailViewModel(get()) }
    viewModel { NewDishViewModel(get(), get(), get(), get()) }
    viewModel { AddMealViewModel(get(), get(), get()) }
    viewModel { TimelineViewModel(get()) }
    viewModel { MineViewModel(get(), get(), get()) }
    viewModel { IngredientPickerViewModel(get(), get()) }
    viewModel { DishPickerViewModel(get()) }
}
