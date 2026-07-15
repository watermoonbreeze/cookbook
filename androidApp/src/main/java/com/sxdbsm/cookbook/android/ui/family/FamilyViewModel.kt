package com.sxdbsm.cookbook.android.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
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
) : ViewModel() {

    val members: StateFlow<List<FamilyMember>> =
        family.observeMembers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _careOptions = MutableStateFlow<List<CrowdType>>(emptyList())
    val careOptions: StateFlow<List<CrowdType>> = _careOptions.asStateFlow()

    init {
        viewModelScope.launch { family.ensureInitialized() }
        viewModelScope.launch { _careOptions.value = health.listAllCrowdTypes() }
    }

    /** 保存成员：id==0 新建、否则更新。[AI生成] */
    fun save(member: FamilyMember) {
        viewModelScope.launch {
            if (member.id == 0L) family.createMember(member) else family.updateMember(member)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { family.deleteMember(id) }
    }

    fun setFocus(id: Long) {
        viewModelScope.launch { family.setFocus(id) }
    }
}
