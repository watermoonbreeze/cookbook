package com.sxdbsm.cookbook.android.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.domain.FilterMetric
import com.sxdbsm.cookbook.domain.NutrientBands
import com.sxdbsm.cookbook.domain.NutrientLevel
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
 * @Desc : 食材营养表（我的·全量食材营养总览）：搜索 + 大类筛选 + 排序 + 指标分级筛选
 * <p>
 * [AI生成] 用户要求在"我的"放一张全量食材营养表，方便查阅每个食材营养素。
 * [AI修改] 商业#7：加"按指标分级筛选"(GI/钠/嘌呤 · 低/中/高)——慢病家庭找"能吃的"食材。
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
    // [AI生成] 商业#7：指标分级筛选。metricFilter=当前筛哪个指标(单选)；levelFilter=选中级别集(空=不筛)。
    val metricFilter = MutableStateFlow(FilterMetric.GI)
    val levelFilter = MutableStateFlow<Set<NutrientLevel>>(emptySet())
    /** 选中(高亮)的食材名，横滑时锁定视线用；再次点同一行取消。[AI生成] */
    val selectedName = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { all.value = nutritionRepo.allIngredientNutrition() }
    }

    fun setQuery(q: String) { query.value = q }
    fun setGroup(g: String?) { groupFilter.value = g }
    fun setMetric(m: FilterMetric) { metricFilter.value = m } // [AI生成] #7 切指标(级别选择清空由 UI 无需·换指标保留级别集)
    /** 切换某级别选中(多选:低+中)。[AI生成] #7 */
    fun toggleLevel(l: NutrientLevel) {
        val cur = levelFilter.value
        levelFilter.value = if (l in cur) cur - l else cur + l
    }
    /** 清除分级筛选(不影响大类/搜索)。[AI生成] #7 */
    fun clearLevelFilter() { levelFilter.value = emptySet() }
    /** 点行切换高亮：点已选行=取消。[AI生成] */
    fun toggleSelect(name: String) { selectedName.value = if (selectedName.value == name) null else name }
    /** 点表头排序：同列切升降，换列默认升序(名称)/降序(数值,大在前更常用)。[AI生成] */
    fun setSort(key: NutriSortKey) {
        if (sortKey.value == key) sortDesc.value = !sortDesc.value
        else { sortKey.value = key; sortDesc.value = key != NutriSortKey.NAME }
    }

    // [AI修改] combine 已超 5 参上限→拆成 filterState + sortState 两层再合(踩坑:重建别丢字段,用命名参数)。
    private data class TableFilter(val query: String, val group: String?, val metric: FilterMetric, val levels: Set<NutrientLevel>)
    private val filterState = combine(query, groupFilter, metricFilter, levelFilter) { q, g, m, lv -> TableFilter(q, g, m, lv) }
    private val sortState = combine(sortKey, sortDesc) { k, d -> k to d }

    /** 应用 搜索∩大类∩分级 后的过滤集(不排序)。[AI生成] 供 rows 与"无数据计数"共用同口径。 */
    private fun applyFilters(list: List<IngredientNutritionRow>, f: TableFilter): List<IngredientNutritionRow> {
        var r = list
        if (f.query.isNotBlank()) r = r.filter { it.name.contains(f.query.trim()) }
        if (f.group != null) r = r.filter { it.foodGroup == f.group }
        if (f.levels.isNotEmpty()) r = r.filter { NutrientBands.matches(f.metric, f.levels, it) }
        return r
    }

    val rows: StateFlow<List<IngredientNutritionRow>> =
        combine(all, filterState, sortState) { l, f, s ->
            val (k, d) = s
            var r = applyFilters(l, f)
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

    /** 分级筛选生效时，被排除的"该指标无数据"食材数(透明交代给 UI 显"另有 N 项无数据未列出")。[AI生成] #7 */
    val excludedNoDataCount: StateFlow<Int> =
        combine(all, filterState) { l, f ->
            if (f.levels.isEmpty()) 0
            else {
                var r = l
                if (f.query.isNotBlank()) r = r.filter { it.name.contains(f.query.trim()) }
                if (f.group != null) r = r.filter { it.foodGroup == f.group }
                r.count { NutrientBands.valueOf(f.metric, it) == null }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
