package com.sxdbsm.cookbook.android.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.data.repository.ShoppingListRepository
import com.sxdbsm.cookbook.domain.model.ShoppingItem
import com.sxdbsm.cookbook.domain.model.ShoppingReason
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * @File : ShoppingListViewModel
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 采购清单 ViewModel
 * <p>
 * 拉取"今天及未来"餐食聚合出的采购/缺料购物清单；本地记录已勾选项(仅当次会话，纯前端)。
 * <p>
 * [AI生成] 待办"采购清单聚合"。
 **/
class ShoppingListViewModel(
    private val repo: ShoppingListRepository,
    private val pantryRepo: PantryRepository, // [AI生成] 采购勾选 → 按份数入库。
    private val familyRepo: com.sxdbsm.cookbook.data.repository.FamilyRepository, // [AI生成] #4:按家人忌口标注(哪些采购食材是某家人不吃的)。
    private val recoDataSource: com.sxdbsm.cookbook.ai.RecommendationDataSource, // [AI生成] #4:复用逐成员个人忌口聚合。
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val items: List<ShoppingItem> = emptyList(),
        val checked: Set<String> = emptySet(), // [AI修改] 已勾选项的 key(按 keyOf:有 id 用 id、否则名)，防同名多 id 串。
        val servings: Map<String, Int> = emptyMap(), // [AI修改] 各项计划采购份数(key→份数)，缺省视为1
        val stocked: Set<String> = emptySet(), // [AI修改] 本次已入库项的 key(划掉灰显)
        val avoidedBy: Map<String, List<String>> = emptyMap(), // [AI生成] #4:项key→忌口该食材的家人名(个人忌口·购买提醒"给其他家人采购")
        val toast: String? = null, // [AI生成] 一次性提示(入库结果/请先勾选)
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** 采购项的稳定标识：有食材 id 用 id、否则用名——避免"同名多 id"的项在勾选/份数/入库上互相串。[AI生成] */
    fun keyOf(item: ShoppingItem): String = item.ingredientId?.let { "id:$it" } ?: "name:${item.ingredientName}"

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val items = repo.aggregate(DateTime.today())
            // [AI修改] N1：份数默认按该食材在未来餐食中的实际所需(mealCount)预填，用户可再调，而非都默认1。
            // 刷新=重新拉取并重置所有临时态(勾选/已入库/提示)。
            val servings = items.associate { keyOf(it) to it.mealCount.coerceAtLeast(1) }
            // [AI生成] #4:按家人"个人忌口"标注——逐成员取个人忌口食材 id(skipGi 省 GI 全表),交叉采购项 id。
            //   只用个人忌口(明确"不吃")、不含病种"不宜"(避免痛风等一标一大片噪音);null-id 项无法按 id 匹配→跳过。
            val avoidSets = familyRepo.listMembers().map { m ->
                m.name to recoDataSource.gatherConstraintsForMember(m, skipGi = true).constraints.personalAvoidIngredientIds
            }
            val avoidedBy = items.mapNotNull { item ->
                val id = item.ingredientId ?: return@mapNotNull null
                val who = avoidSets.filter { id in it.second }.map { it.first }
                if (who.isEmpty()) null else keyOf(item) to who
            }.toMap()
            _state.value = UiState(loading = false, items = items, servings = servings, avoidedBy = avoidedBy)
        }
    }

    /** 勾选/取消某项（买到了打勾）。[AI生成] */
    fun toggleChecked(key: String) {
        val cur = _state.value.checked
        _state.value = _state.value.copy(checked = if (key in cur) cur - key else cur + key)
    }

    /** 全选/取消全选某组(按 reason)。[AI生成] */
    fun toggleSelectAll(reason: ShoppingReason) {
        val groupKeys = _state.value.items.filter { it.reason == reason }.map { keyOf(it) }.toSet()
        val cur = _state.value.checked
        val allChecked = groupKeys.isNotEmpty() && groupKeys.all { it in cur }
        _state.value = _state.value.copy(
            checked = if (allChecked) cur - groupKeys else cur + groupKeys,
        )
    }

    /** 调整某项份数(±，最小1)。[AI生成] */
    fun changeServing(key: String, delta: Int) {
        val cur = _state.value.servings[key] ?: 1
        val next = (cur + delta).coerceAtLeast(1)
        _state.value = _state.value.copy(servings = _state.value.servings + (key to next))
    }

    private var stocking = false // [AI生成] 防止入库进行中重复点击导致并发累加/计数错乱。

    /** 把已勾选项按份数入库。[AI生成] 没勾选则提示；无法解析 id 的项跳过并计数。 */
    fun stockInChecked() {
        if (stocking) return
        val checked = _state.value.checked
        if (checked.isEmpty()) {
            _state.value = _state.value.copy(toast = "请先勾选要入库的食材")
            return
        }
        // [AI修改] 入库开始即固化目标(key/份数)，全程用快照，避免挂起期间 state 变化导致划掉项与实际入库项不一致。
        val targets = _state.value.items.filter { keyOf(it) in checked && it.ingredientId != null }
        val servingsSnapshot = _state.value.servings
        val targetKeys = targets.map { keyOf(it) }.toSet()
        stocking = true
        viewModelScope.launch {
            var ok = 0
            targets.forEach { item ->
                runCatching { pantryRepo.addServings(item.ingredientId!!, servingsSnapshot[keyOf(item)] ?: 1) }
                    .onSuccess { ok++ }
            }
            val skipped = checked.size - targets.size
            val msg = buildString {
                append("已入库 $ok 项")
                if (skipped > 0) append("，$skipped 项未入库(食材已失效或已刷新)")
            }
            _state.value = _state.value.copy(
                stocked = _state.value.stocked + targetKeys,
                checked = _state.value.checked - targetKeys,
                toast = msg,
            )
            stocking = false
            refresh() // [AI修改] bug3：入库后自动刷新，缺料/在手状态即时更新，无需手动刷新按钮。
        }
    }

    /** 消费一次性提示。[AI生成] */
    fun consumeToast() {
        _state.value = _state.value.copy(toast = null)
    }
}
