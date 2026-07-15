package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.BodyMetrics
import com.sxdbsm.cookbook.domain.model.FamilyMember
import com.sxdbsm.cookbook.platform.ioDispatcher
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * @File : FamilyRepository
 * @Time : 2026/07/15
 * @Author : SXD-AI
 * @Desc : 家庭成员档案仓库（多人记菜）
 * <p>
 * 管理家庭成员的身体数据/饭量系数/病种。忌口取全家启用成员的病种并集；每日目标/达标按主要关注成员。
 * 首次使用时幂等建默认成员「我」并迁移旧单套身体数据(偏好)+旧健康档案(user_health_profile)。
 * <p>
 * [AI生成] 多人家庭档案 P1。
 **/
class FamilyRepository(
    private val db: CookbookDatabase,
    private val prefs: PreferenceRepository,
) {
    private val q = db.cookbookQueries

    /**
     * 幂等初始化：无任何成员时建默认「我」(is_self=1,is_focus=1)，承接旧单套身体数据 + 旧启用病种。[AI生成]
     *
     * 只在成员表为空时执行一次；老库升级(迁移只建空表)与全新装(Schema.create)都走这里，保证有个默认成员。
     */
    suspend fun ensureInitialized() = withContext(ioDispatcher) {
        if (q.countFamilyMembers().executeAsOne() > 0L) return@withContext
        val body = prefs.observeBodyMetrics().first()
        val coeff = FamilyMember.defaultCoefficient(body.gender, body.age)
        db.transaction {
            q.insertFamilyMember(
                name = "我",
                gender = body.gender,
                height_cm = body.heightCm,
                weight_kg = body.weightKg,
                age = body.age?.toLong(),
                activity = body.activity,
                portion_coefficient = coeff,
                is_self = 1L,
                is_focus = 1L,
                sort = 0L,
                created_at = DateTime.nowEpochSeconds(),
            )
            val selfId = q.selectSelfMemberId().executeAsOne()
            // 迁移旧健康档案(全局启用病种)到「我」
            q.selectEnabledHealthProfiles().executeAsList().forEach { row ->
                q.insertMemberCare(member_id = selfId, care_category_id = row.care_category_id)
            }
        }
    }

    /** 监听全部家庭成员(含各自病种)。[AI生成] */
    fun observeMembers(): Flow<List<FamilyMember>> =
        q.selectAllFamilyMembers().asFlow().mapToList(ioDispatcher)
            .map { rows -> rows.map { toModel(it.id, it.name, it.gender, it.height_cm, it.weight_kg, it.age, it.activity, it.portion_coefficient, it.is_self, it.is_focus) } }
            .flowOn(ioDispatcher)

    /** 读取全部家庭成员(含各自病种)。[AI生成] */
    suspend fun listMembers(): List<FamilyMember> = withContext(ioDispatcher) {
        q.selectAllFamilyMembers().executeAsList().map {
            toModel(it.id, it.name, it.gender, it.height_cm, it.weight_kg, it.age, it.activity, it.portion_coefficient, it.is_self, it.is_focus)
        }
    }

    /** 主要关注成员(无则回退「我」，再无则第一个)。[AI生成] 达标/色系墙默认围绕他。 */
    suspend fun focusMember(): FamilyMember? = withContext(ioDispatcher) {
        val focus = q.selectFocusMember().executeAsOneOrNull()
        val row = focus ?: q.selectAllFamilyMembers().executeAsList().firstOrNull() ?: return@withContext null
        toModel(row.id, row.name, row.gender, row.height_cm, row.weight_kg, row.age, row.activity, row.portion_coefficient, row.is_self, row.is_focus)
    }

    /** 新建成员，返回 id。[AI生成] */
    suspend fun createMember(m: FamilyMember): Long = withContext(ioDispatcher) {
        var newId = 0L
        db.transaction {
            q.insertFamilyMember(
                name = m.name.trim().ifBlank { "成员" },
                gender = m.gender,
                height_cm = m.heightCm,
                weight_kg = m.weightKg,
                age = m.age?.toLong(),
                activity = m.activity,
                portion_coefficient = m.portionCoefficient,
                is_self = 0L,
                is_focus = 0L,
                sort = (q.countFamilyMembers().executeAsOne()),
                created_at = DateTime.nowEpochSeconds(),
            )
            newId = q.lastInsertRowId().executeAsOne()
            m.careCategoryIds.forEach { q.insertMemberCare(member_id = newId, care_category_id = it) }
        }
        newId
    }

    /** 更新成员身体数据/系数/病种。[AI生成] */
    suspend fun updateMember(m: FamilyMember) = withContext(ioDispatcher) {
        db.transaction {
            q.updateFamilyMember(
                name = m.name.trim().ifBlank { "成员" },
                gender = m.gender,
                height_cm = m.heightCm,
                weight_kg = m.weightKg,
                age = m.age?.toLong(),
                activity = m.activity,
                portion_coefficient = m.portionCoefficient,
                id = m.id,
            )
            q.deleteAllMemberCare(m.id)
            m.careCategoryIds.forEach { q.insertMemberCare(member_id = m.id, care_category_id = it) }
        }
    }

    /** 删除成员(仅非「我」)。[AI生成] */
    suspend fun deleteMember(id: Long) = withContext(ioDispatcher) { q.softDeleteFamilyMember(id) }

    /** 设主要关注成员(单选)。[AI生成] */
    suspend fun setFocus(id: Long) = withContext(ioDispatcher) {
        db.transaction { q.clearAllFocus(); q.setFocusMember(id) }
    }

    /** 全家启用成员的病种并集(忌口口径)。[AI生成] */
    suspend fun allEnabledCareIds(): List<Long> = withContext(ioDispatcher) {
        q.selectAllEnabledCareIds().executeAsList()
    }

    private fun toModel(
        id: Long, name: String, gender: String, height: Double?, weight: Double?, age: Long?,
        activity: String, coeff: Double, isSelf: Long, isFocus: Long,
    ): FamilyMember = FamilyMember(
        id = id, name = name, gender = gender, heightCm = height, weightKg = weight, age = age?.toInt(),
        activity = activity, portionCoefficient = coeff, isSelf = isSelf == 1L, isFocus = isFocus == 1L,
        careCategoryIds = q.selectMemberCareIds(id).executeAsList(),
    )
}
