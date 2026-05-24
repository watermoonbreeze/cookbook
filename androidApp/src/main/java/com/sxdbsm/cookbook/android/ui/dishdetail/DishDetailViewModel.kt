package com.sxdbsm.cookbook.android.ui.dishdetail

import androidx.lifecycle.ViewModel
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.domain.model.Dish
import kotlinx.coroutines.flow.Flow

/**
 * 菜品详情 ViewModel。[AI修改]
 *
 * 当前只做一件事：把菜品 id 转成可观察的菜品详情 Flow。
 */
class DishDetailViewModel(
    private val dishRepo: DishRepository,
) : ViewModel() {
    /**
     * 监听菜品详情。[AI修改]
     */
    fun observeDish(dishId: Long): Flow<Dish?> = dishRepo.observeDishById(dishId)
}
