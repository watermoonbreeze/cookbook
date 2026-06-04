package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.CrowdType
import com.sxdbsm.cookbook.domain.model.HealthProfile
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 健康档案仓库。[AI修改]
 *
 * 管理用户启用的慢性病/健康人群标签，用于后续食材推荐和筛选。
 */
class HealthProfileRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries

    /**
     * 读取系统支持的全部健康人群类型。[AI修改]
     */
    suspend fun listAllCrowdTypes(): List<CrowdType> = withContext(Dispatchers.Default) {
        q.selectAllCrowdTypes().executeAsList().map { CrowdType(it.id, it.name, it.description) }
    }

    /**
     * 监听已启用的健康档案。[AI修改]
     */
    fun observeEnabled(): Flow<List<HealthProfile>> =
        q.selectEnabledHealthProfiles().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map {
                HealthProfile(
                    crowdTypeId = it.crowd_type_id,
                    crowdName = it.crowd_name,
                    crowdDescription = it.crowd_description,
                    enabled = it.enabled == 1L,
                )
            }
        }.flowOn(Dispatchers.Default)

    /**
     * 读取全部健康档案，不只包含启用项。[AI修改]
     */
    suspend fun listAll(): List<HealthProfile> = withContext(Dispatchers.Default) {
        q.selectAllHealthProfiles().executeAsList().map {
            HealthProfile(
                crowdTypeId = it.crowd_type_id,
                crowdName = it.crowd_name,
                crowdDescription = it.crowd_description,
                enabled = it.enabled == 1L,
            )
        }
    }

    /**
     * 新增并启用一个健康档案。[AI修改]
     */
    suspend fun add(crowdTypeId: Long) = withContext(Dispatchers.Default) {
        q.insertHealthProfile(crowdTypeId, DateTime.nowEpochSeconds())
        q.updateHealthProfileEnabled(1, crowdTypeId)
    }

    /** 禁用健康档案但保留记录。[AI修改] */
    suspend fun disable(crowdTypeId: Long) = withContext(Dispatchers.Default) { q.updateHealthProfileEnabled(0, crowdTypeId) }
    /** 重新启用健康档案。[AI修改] */
    suspend fun enable(crowdTypeId: Long) = withContext(Dispatchers.Default) { q.updateHealthProfileEnabled(1, crowdTypeId) }
    /** 删除健康档案记录。[AI修改] */
    suspend fun remove(crowdTypeId: Long) = withContext(Dispatchers.Default) { q.deleteHealthProfile(crowdTypeId) }
}
