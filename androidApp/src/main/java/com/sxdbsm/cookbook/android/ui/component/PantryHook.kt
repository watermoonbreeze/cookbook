package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.domain.model.PreferenceKeys
import org.koin.compose.koinInject

/**
 * @File : PantryHook
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : "库存挂钩"总开关的 Composable 读取(default 开)
 * <p>
 * 集中一处，避免各库存消费点(详情/食历/AI规划/推荐/采购/食材Tab/入库出库…)逐字重复
 * koinInject + observeFlag + collectAsStateWithLifecycle 样板——"改一处漏五处"正是本功能
 * 一致性遗漏的根因(Google 审查建议2)。所有消费点统一用本 helper 读，降低漏点风险。
 * <p>
 * [AI生成] 库存挂钩开关统一读取，default=true(保留现有行为，老用户升级不丢功能)。
 **/
@Composable
fun rememberPantryHookEnabled(): State<Boolean> {
    val prefs = koinInject<PreferenceRepository>()
    return remember(prefs) {
        prefs.observeFlag(PreferenceKeys.PANTRY_HOOK_ENABLED, default = true)
    }.collectAsStateWithLifecycle(true)
}
