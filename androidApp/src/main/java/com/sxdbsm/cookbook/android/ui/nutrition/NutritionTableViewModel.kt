package com.sxdbsm.cookbook.android.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.domain.model.IngredientNutritionRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @File : NutritionTableViewModel
 * @Time : 2026/07/15
 * @Author : SXD-AI
 * @Desc : 食材营养表（我的·全量食材营养总览）：搜索 + 大类筛选 + 排序
 * <p>
 * [AI生成] 用户要求在"我的"放一张全量食材营养表，方便查阅每个食材营养素。
 **/
enum class NutriSortKey { NAME, KCAL, PROTEIN, FAT, CARB, FIBER, SODIUM, POTASSIUM, CALCIUM, GI, PURINE }

class NutritionTableViewModel(
    private val nutritionRepo: NutritionRepository,
) : ViewModel() {

    private val all = MutableStateFlow<List<IngredientNutritionRow>>(emptyList())
    val query = MutableStateFlow("")
    val groupFilter = MutableStateFlow<String?>(null) // FoodGroup.Group 名，null=全部
    val sortKey = MutableStateFlow(NutriSortKey.NAME)
    val sortDesc = MutableStateFlow(false)
    /** 选中(高亮)的食材名，横滑时锁定视线用；再次点同一行取消。[AI生成] */
    val selectedName = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { all.value = nutritionRepo.allIngredientNutrition() }
    }

    fun setQuery(q: String) { query.value = q }
    fun setGroup(g: String?) { groupFilter.value = g }
    /** 点行切换高亮：点已选行=取消。[AI生成] */
    fun toggleSelect(name: String) { selectedName.value = if (selectedName.value == name) null else name }
    /** 点表头排序：同列切升降，换列默认升序(名称)/降序(数值,大在前更常用)。[AI生成] */
    fun setSort(key: NutriSortKey) {
        if (sortKey.value == key) sortDesc.value = !sortDesc.value
        else { sortKey.value = key; sortDesc.value = key != NutriSortKey.NAME }
    }

    val rows: StateFlow<List<IngredientNutritionRow>> =
        combine(all, query, groupFilter, sortKey, sortDesc) { l, q, g, k, d ->
            var r = l
            if (q.isNotBlank()) r = r.filter { it.name.contains(q.trim()) }
            if (g != null) r = r.filter { it.foodGroup == g }
            r = if (k == NutriSortKey.NAME) {
                r.sortedBy { it.name }
            } else {
                val num: (IngredientNutritionRow) -> Double? = when (k) {
                    NutriSortKey.KCAL -> { it -> it.kcal }
                    NutriSortKey.PROTEIN -> { it -> it.protein }
                    NutriSortKey.FAT -> { it -> it.fat }
                    NutriSortKey.CARB -> { it -> it.carb }
                    NutriSortKey.FIBER -> { it -> it.fiber }
                    NutriSortKey.SODIUM -> { it -> it.sodium }
                    NutriSortKey.POTASSIUM -> { it -> it.potassium }
                    NutriSortKey.CALCIUM -> { it -> it.calcium }
                    NutriSortKey.GI -> { it -> it.gi }
                    NutriSortKey.PURINE -> { it -> it.purine }
                    NutriSortKey.NAME -> { _ -> null }
                }
                r.sortedWith(compareBy(nullsLast()) { num(it) })
            }
            if (d) r.reversed() else r
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
