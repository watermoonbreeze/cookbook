package com.sxdbsm.cookbook.android.ui.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.ShareLinkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 链接列表页 VM（路由作用域·LINK_LIST 回退栈条目销毁即随之销毁·ARCH-07）。[AI生成] STEP-L4-10.2
 *
 * 列表为一次性冷读（进页拉一次）+ 显式 refresh（软删/撤销后重拉）；组内排序由
 * listGrouped 的 SQL（parse_state 升序 + created_at 倒序）保证，本 VM 只分组不重排。
 */
class LinkListViewModel(
    private val linkRepo: ShareLinkRepository,
    private val dishRepo: DishRepository,
    /** Activity 域红点 VM（软删/恢复后红点经同一 SQLDelight flow 自动刷新）。[AI生成] */
    val badge: LinkBadgeViewModel,
) : ViewModel() {

    private val _rows = MutableStateFlow<List<ShareLinkRepository.ShareLinkRow>>(emptyList())

    /** 全量活跃链接行（SQL 已按 parse_state 升序 + created_at 倒序）。[AI生成] */
    val rows: StateFlow<List<ShareLinkRepository.ShareLinkRow>> = _rows.asStateFlow()

    init {
        refresh()
    }

    /** 重拉列表（软删/撤销后调）。[AI生成] */
    fun refresh() {
        viewModelScope.launch {
            _rows.value = runCatching { linkRepo.listGrouped() }.getOrDefault(emptyList())
        }
    }

    /**
     * 四组分组（待解析/已解析/解析失败/已存为菜品）；0 项组由 UI 整组不渲染（含组头）。[AI生成]
     * 组内排序已由 listGrouped 的 SQL 保证——只过滤归组，不重排。
     */
    fun groups(rows: List<ShareLinkRepository.ShareLinkRow>): List<Pair<String, List<ShareLinkRepository.ShareLinkRow>>> =
        listOf(
            "待解析" to rows.filter { it.parseState == 0L },
            "已解析" to rows.filter { it.parseState == 1L },
            "解析失败" to rows.filter { it.parseState == 2L },
            "已存为菜品" to rows.filter { it.parseState == 3L },
        )

    /**
     * 菜品活性（3 态行点击跳详情前查·Y-02 死菜兜底）：selectDishById SQL 已过滤 status=1，
     * 软删/不存在均返回 null → 失活（不额外读 status 字段）。[AI生成]
     */
    suspend fun isDishAlive(dishId: Long): Boolean =
        runCatching { dishRepo.getDishMiniById(dishId) }.getOrNull() != null

    /** 软删（回收站式可恢复·撤销走 restore）。[AI生成] */
    fun softDelete(row: ShareLinkRepository.ShareLinkRow) {
        viewModelScope.launch {
            runCatching { linkRepo.softDelete(row.id) }
            refresh()
        }
    }

    /** 撤销软删。[AI生成] */
    fun restore(row: ShareLinkRepository.ShareLinkRow) {
        viewModelScope.launch {
            runCatching { linkRepo.restore(row.id) }
            refresh()
        }
    }
}
