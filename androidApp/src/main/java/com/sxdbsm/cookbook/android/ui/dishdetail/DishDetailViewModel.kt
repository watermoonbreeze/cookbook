package com.sxdbsm.cookbook.android.ui.dishdetail

import androidx.lifecycle.ViewModel
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.domain.model.Dish
import kotlinx.coroutines.flow.Flow

class DishDetailViewModel(
    private val dishRepo: DishRepository,
) : ViewModel() {
    fun observeDish(dishId: Long): Flow<Dish?> = dishRepo.observeDishById(dishId)
}
