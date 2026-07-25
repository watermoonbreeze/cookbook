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
        prefs.observeFlag(PreferenceKeys.PANTRY_HOOK_ENABLED, default = PreferenceKeys.DEFAULT_PANTRY_HOOK)
    }.collectAsStateWithLifecycle(PreferenceKeys.DEFAULT_PANTRY_HOOK)
}

/**
 * "热量数值显示"总开关的 Composable 读取(**default=开**)。[AI修改] §9.36
 *
 * [AI修改] 2026-07-22 用户决策"先默认展示健康膳食能力·可关闭"(透明 opt-out)：热量数字由默认关翻为**默认开**
 *   (旧"热量个人概念·默认关"红线按此更新)。推荐卡/餐食卡/今日卡等热量**数字**显隐统一读此·仍守"仅供参考·非医嘱"。
 * 关时只显宏量结构(客观食物构成·非个人能量概念)、不显千卡数字。集中一处避免各消费点重复样板。
 */
@Composable
fun rememberCalorieNumberEnabled(): State<Boolean> {
    val prefs = koinInject<PreferenceRepository>()
    return remember(prefs) {
        prefs.observeFlag(PreferenceKeys.CALORIE_NUMBER_ENABLED, default = PreferenceKeys.DEFAULT_CALORIE_NUMBER)
    }.collectAsStateWithLifecycle(PreferenceKeys.DEFAULT_CALORIE_NUMBER)
}

/**
 * "餐次结构建议"开关的 Composable 读取(**default=开**)。[AI生成] P2
 *
 * 今日卡对"缺蔬菜/早餐缺蛋白"给一句浅灰下一步小字(T1 事后留痕·鼓励非评判·可关)。集中一处避免消费点重复样板。
 */
@Composable
fun rememberMealStructureHintEnabled(): State<Boolean> {
    val prefs = koinInject<PreferenceRepository>()
    return remember(prefs) {
        prefs.observeFlag(PreferenceKeys.MEAL_STRUCTURE_HINT_ENABLED, default = PreferenceKeys.DEFAULT_MEAL_STRUCTURE_HINT)
    }.collectAsStateWithLifecycle(PreferenceKeys.DEFAULT_MEAL_STRUCTURE_HINT)
}
