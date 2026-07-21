package com.sxdbsm.cookbook.android.ui.cook

import androidx.lifecycle.ViewModel
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.domain.model.Dish
import kotlinx.coroutines.flow.Flow

/**
 * @File : CookModeViewModel
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 分步烹饪页 ViewModel（薄 VM·把数据访问从 Composable 收进 VM）
 * <p>
 * 分步烹饪页原直接在 Composable 注入 [DishRepository] 订阅冷流（越层·违反「Screen 不得注入 Repository」准则）。
 * 抽薄 VM 后 Screen 只 collect [observeDish] 产出的菜品流，不再触达 data 层；
 * 当前步索引/单步计时器为纯 UI 瞬态，仍留在 Composable（审计 F-Arch3：index 属纯 UI 态可留）。
 * <p>
 * [AI生成] D1 无 VM 页 VM 化专项·F-Arch3：数据访问收进 VM，范式对齐 DishDetailViewModel.observeDish。
 **/
class CookModeViewModel(
    private val dishRepo: DishRepository,
) : ViewModel() {

    /** 订阅某菜品（含步骤），编辑保存后自动刷新。调用方用 remember(dishId) 缓存冷流。[AI生成] */
    fun observeDish(dishId: Long): Flow<Dish?> = dishRepo.observeDishById(dishId)
}
