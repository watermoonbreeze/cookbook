package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.CrowdType
import com.sxdbsm.cookbook.domain.model.HealthProfile
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HealthProfileRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries

    suspend fun listAllCrowdTypes(): List<CrowdType> =
        q.selectAllCrowdTypes().executeAsList().map { CrowdType(it.id, it.name, it.description) }

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
        }

    suspend fun listAll(): List<HealthProfile> =
        q.selectAllHealthProfiles().executeAsList().map {
            HealthProfile(
                crowdTypeId = it.crowd_type_id,
                crowdName = it.crowd_name,
                crowdDescription = it.crowd_description,
                enabled = it.enabled == 1L,
            )
        }

    suspend fun add(crowdTypeId: Long) {
        q.insertHealthProfile(crowdTypeId, DateTime.nowEpochSeconds())
        q.updateHealthProfileEnabled(1, crowdTypeId)
    }

    suspend fun disable(crowdTypeId: Long) = q.updateHealthProfileEnabled(0, crowdTypeId)
    suspend fun enable(crowdTypeId: Long) = q.updateHealthProfileEnabled(1, crowdTypeId)
    suspend fun remove(crowdTypeId: Long) = q.deleteHealthProfile(crowdTypeId)
}
