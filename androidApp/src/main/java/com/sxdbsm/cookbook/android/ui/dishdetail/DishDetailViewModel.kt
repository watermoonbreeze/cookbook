package com.sxdbsm.cookbook.android.ui.dishdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.Dish
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 菜品详情 ViewModel。[AI修改]
 *
 * 除菜品本身，附加派生洞察：库存可做/缺料/采购、健康适宜性、做过次数、营养概要。
 * 全部只影响展示、不改数据；无库存/无健康档案时对应块不展示，避免打扰。[AI修改]
 */
class DishDetailViewModel(
    private val dishRepo: DishRepository,
    private val pantryRepo: PantryRepository,
    private val healthRepo: HealthProfileRepository,
    private val ingredientRepo: IngredientRepository,
) : ViewModel() {

    var insights by mutableStateOf<DishInsights?>(null)
        private set

    var isFavorite by mutableStateOf(false) // [AI生成] B1：当前菜是否收藏(置顶)
        private set

    fun observeDish(dishId: Long): Flow<Dish?> = dishRepo.observeDishById(dishId)

    /** 加载收藏态。[AI生成] B1 */
    fun loadFavorite(dishId: Long) {
        viewModelScope.launch { isFavorite = dishId in dishRepo.favoriteDishIds() }
    }

    /** 切换收藏(置顶)。[AI生成] B1 */
    fun toggleFavorite(dishId: Long) {
        viewModelScope.launch {
            val next = !isFavorite
            runCatching { dishRepo.setDishFavorite(dishId, next) }.onSuccess { isFavorite = next }
        }
    }

    /** 加载详情洞察（进入详情/菜品变化时调）。[AI生成] */
    fun loadInsights(dish: Dish) {
        viewModelScope.launch {
            insights = computeInsights(dish)
        }
    }

    private suspend fun computeInsights(dish: Dish): DishInsights {
        val ingIds = dish.ingredients.map { it.ingredient.id }

        // 库存：主料不在库→采购、在库当前剩余份数≤0→缺；无库存则不判。
        // 注：详情页回答"以现有库存现在能否做这道菜"，用全局 remaining()——窗口是 [入库日, 今天] 闭区间，
        // 扣掉截至今天所有已发生餐 + 今日排期餐的占用(不预支未来)。故同一天多餐争用同一在库食材、或历史餐
        // 已把份数占光时，详情页会比食历卡片更早报"缺"。这是"现在整体能否做(保守)" vs 卡片按餐次时间序
        // rank 判"这张排期餐是否轮得到(乐观, PantryAllocation.shortages 只对 date>=今天的餐标缺)"的固有语义差异，非 bug。
        val servings = pantryRepo.servingCounts()
        val usingPantry = servings.isNotEmpty()
        val remaining = if (usingPantry) pantryRepo.remaining() else emptyMap()
        val purchase = mutableListOf<String>()
        val shortage = mutableListOf<String>()
        if (usingPantry) {
            dish.ingredients.filter { it.isMain }.forEach { di ->
                val id = di.ingredient.id
                when {
                    id !in servings.keys -> purchase += di.ingredient.name
                    (remaining[id] ?: 0) <= 0 -> shortage += di.ingredient.name
                }
            }
        }

        // 健康适宜性：按启用档案标食材 忌口/限量/推荐；无档案则不判。
        val profiles = healthRepo.listAll().filter { it.enabled }
        val careIds = profiles.map { it.crowdTypeId }
        val careIngredients = if (careIds.isEmpty()) emptyList() else ingredientRepo.listByCareCategories(careIds)
        val avoid = careIngredients.filter { it.adviceLevel == AdviceLevel.AVOID }.map { it.id }.toSet()
        val limit = careIngredients.filter { it.adviceLevel == AdviceLevel.LIMIT }.map { it.id }.toSet()
        val recommend = careIngredients.filter { it.adviceLevel == AdviceLevel.RECOMMEND }.map { it.id }.toSet()

        val stats = dishRepo.cookStats(dish.id)
        val nutrition = ingredientRepo.nutritionTagsOf(ingIds)

        // 相关菜品：与本菜共享主料的其它菜(按共享数排序)。
        val mainIds = dish.ingredients.filter { it.isMain }.map { it.ingredient.id }
        val related = if (mainIds.isEmpty()) emptyList()
        else dishRepo.findDishesByIngredients(mainIds, limit = 12).map { it.dish }.filter { it.id != dish.id }.take(8)

        return DishInsights(
            usingPantry = usingPantry,
            purchaseNames = purchase.distinct(),
            shortageNames = shortage.distinct(),
            hasHealthProfile = profiles.isNotEmpty(),
            avoidNames = dish.ingredients.filter { it.ingredient.id in avoid }.map { it.ingredient.name }.distinct(),
            limitNames = dish.ingredients.filter { it.ingredient.id in limit }.map { it.ingredient.name }.distinct(),
            recommendNames = dish.ingredients.filter { it.ingredient.id in recommend }.map { it.ingredient.name }.distinct(),
            cookedCount = stats.first,
            lastCookedDate = stats.second,
            nutritionTags = nutrition,
            related = related, // [AI修改] 修复：此前漏传导致"相关菜品"永不显示。
        )
    }
}

/** 菜品详情派生洞察。[AI生成] */
data class DishInsights(
    val usingPantry: Boolean,
    val purchaseNames: List<String>,
    val shortageNames: List<String>,
    val hasHealthProfile: Boolean,
    val avoidNames: List<String>,
    val limitNames: List<String>,
    val recommendNames: List<String>,
    val cookedCount: Int,
    val lastCookedDate: String?,
    val nutritionTags: List<String>,
    val related: List<com.sxdbsm.cookbook.domain.model.DishMini> = emptyList(),
) {
    /** 当前库存能否直接做（在用库存且无采购无缺料）。 */
    val canCook: Boolean get() = usingPantry && purchaseNames.isEmpty() && shortageNames.isEmpty()
}
