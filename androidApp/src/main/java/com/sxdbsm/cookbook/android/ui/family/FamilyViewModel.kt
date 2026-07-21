package com.sxdbsm.cookbook.android.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.domain.model.CrowdType
import com.sxdbsm.cookbook.domain.model.FamilyMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @File : FamilyViewModel
 * @Time : 2026/07/15
 * @Author : SXD-AI
 * @Desc : 家庭成员管理 ViewModel
 * <p>
 * 列出/增删改成员、切换主要关注成员；病种选项来自健康档案调养分类。
 * <p>
 * [AI生成] 多人家庭档案 P1 Stage2。
 **/
class FamilyViewModel(
    private val family: FamilyRepository,
    private val health: HealthProfileRepository,
    private val analytics: com.sxdbsm.cookbook.analytics.Analytics, // [AI生成] 阶段3-b：健康档案设置埋点(health_profile_set·仅布尔"设了")
    private val ingredients: IngredientRepository, // [AI生成] D1-2:成员编辑忌口食材按 id 查名,收进 VM(原 Composable 直注 IngredientRepository=越层)
) : ViewModel() {

    /** 按食材 id 批量查名（成员编辑页个人忌口食材 chip 展示用）。[AI生成] D1-2:数据访问收进 VM。 */
    suspend fun ingredientNamesByIds(ids: List<Long>): Map<Long, String> = ingredients.namesByIds(ids)

    val members: StateFlow<List<FamilyMember>> =
        family.observeMembers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _careOptions = MutableStateFlow<List<CrowdType>>(emptyList())
    val careOptions: StateFlow<List<CrowdType>> = _careOptions.asStateFlow()

    // [AI生成] v29:个人忌口可选分类项(chip 白名单·§9.22)。
    private val _avoidCategoryOptions = MutableStateFlow<List<com.sxdbsm.cookbook.domain.model.AvoidCategoryOption>>(emptyList())
    val avoidCategoryOptions: StateFlow<List<com.sxdbsm.cookbook.domain.model.AvoidCategoryOption>> = _avoidCategoryOptions.asStateFlow()

    init {
        viewModelScope.launch { family.ensureInitialized() }
        viewModelScope.launch { _careOptions.value = health.listAllCrowdTypes() }
        viewModelScope.launch { _avoidCategoryOptions.value = family.listAvoidCategoryOptions() }
    }

    /** 保存成员：id==0 新建、否则更新。[AI生成] */
    fun save(member: FamilyMember) {
        viewModelScope.launch {
            if (member.id == 0L) family.createMember(member) else family.updateMember(member)
            // [AI生成] 阶段3-b 匿名统计：设了健康档案(渗透率)。**仅布尔"设了"**·绝不带病种/忌口/身体数据。
            analytics.track(com.sxdbsm.cookbook.analytics.AnalyticsEvent.HealthProfileSet)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { family.deleteMember(id) }
    }

    // [AI生成] 多人关注:一次性提示(如取消最后一个关注被拒)→FamilyScreen 收集进 AppSnackbar。
    private val _messages = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: kotlinx.coroutines.flow.SharedFlow<String> = _messages

    /** 加入/移出关注集合(多选·至少留1)。取消最后一个→提示拒绝。[AI修改] 多人关注 */
    fun toggleFocus(id: Long) {
        viewModelScope.launch {
            if (!family.toggleFocus(id)) _messages.tryEmit("至少关注一位家人")
        }
    }
}
