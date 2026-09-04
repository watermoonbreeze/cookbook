package com.sxdbsm.cookbook.android.ui.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.ShareLinkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 待解析链接红点计数 VM（Activity 域单实例·MainScaffold 体内 koinViewModel——DishesScreen 红点
 * 与 LinkListScreen/首页横幅共用同一 SQLDelight flow 源·ARCH-07）。[AI生成] STEP-L4-10.1
 *
 * WhileSubscribed(5000)：无订阅者即停（不可见页面不空转），5s 内重订阅复用上游不重查。
 */
class LinkBadgeViewModel(linkRepo: ShareLinkRepository) : ViewModel() {

    /** 待解析（parse_state=0·status=1）链接数：红点 >0 显、横幅计数同源。[AI生成] */
    val pendingCount: StateFlow<Int> = linkRepo.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
