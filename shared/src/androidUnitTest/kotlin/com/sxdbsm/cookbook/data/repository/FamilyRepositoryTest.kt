package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import com.sxdbsm.cookbook.domain.model.BodyMetrics
import com.sxdbsm.cookbook.domain.model.FamilyMember
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : FamilyRepositoryTest
 * @Time : 2026/07/15
 * @Author : SXD-AI
 * @Desc : 家庭成员档案仓库单测（默认成员迁移 + CRUD + 关注 + 忌口并集）
 * <p>
 * [AI生成] 多人家庭档案 P1。注意：单测走 Schema.create（不跑迁移链），此处验证 ensureInitialized 的运行时逻辑。
 **/
class FamilyRepositoryTest {

    private suspend fun setup(): Triple<FamilyRepository, HealthProfileRepository, PreferenceRepository> {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        return Triple(FamilyRepository(db, PreferenceRepository(db)), HealthProfileRepository(db), PreferenceRepository(db))
    }

    @Test
    fun `首启建默认成员我_承接身体数据并迁移旧病种_且幂等`() = runBlocking {
        val (fam, health, prefs) = setup()
        prefs.setBodyMetrics(BodyMetrics(gender = "MALE", heightCm = 175.0, weightKg = 70.0, age = 30))
        val hbp = health.listAllCrowdTypes().first { it.name == "高血压" }
        health.add(hbp.id) // 旧全局病种

        fam.ensureInitialized()
        val members = fam.listMembers()
        assertEquals(1, members.size, "应建唯一默认成员")
        val me = members.first()
        assertTrue(me.isSelf && me.isFocus && me.name == "我", "默认成员=我(is_self+is_focus)")
        assertEquals(70.0, me.weightKg, "承接旧身体数据")
        assertTrue(hbp.id in me.careCategoryIds, "旧启用病种应迁到我")

        fam.ensureInitialized() // 再次不应重复建
        assertEquals(1, fam.listMembers().size, "ensureInitialized 幂等")
    }

    @Test
    fun `成员CRUD_关注切换_忌口取全家并集`() = runBlocking {
        val (fam, health, _) = setup()
        fam.ensureInitialized()
        val cares = health.listAllCrowdTypes()
        assertTrue(cares.size >= 2, "应有≥2个可选病种")
        val hbp = cares[0]
        val gout = cares[1]

        val me = fam.listMembers().first()
        fam.updateMember(me.copy(careCategoryIds = listOf(hbp.id)))
        val dadId = fam.createMember(FamilyMember(id = 0, name = "爸", careCategoryIds = listOf(gout.id)))
        assertEquals(2, fam.listMembers().size)

        // 忌口取并集：我(高血压) ∪ 爸(高尿酸)
        val union = fam.allEnabledCareIds().toSet()
        assertTrue(hbp.id in union && gout.id in union, "忌口取全家病种并集")

        // 关注切换到爸
        fam.setFocus(dadId)
        assertEquals(dadId, fam.focusMember()!!.id, "关注成员应为爸")

        // 删爸(非我可删)
        fam.deleteMember(dadId)
        assertEquals(1, fam.listMembers().size)
        // 我不可删
        fam.deleteMember(me.id)
        assertEquals(1, fam.listMembers().size, "默认成员我不可删")
    }

    @Test
    fun `删除关注成员_关注自动转移不丢失`() = runBlocking {
        val (fam, _, _) = setup()
        fam.ensureInitialized()
        val me = fam.listMembers().first()
        val dadId = fam.createMember(FamilyMember(id = 0, name = "爸"))
        fam.setFocus(dadId)
        assertEquals(dadId, fam.focusMember()!!.id)

        fam.deleteMember(dadId) // 删的是关注成员
        val focus = fam.focusMember()
        assertNotNull(focus, "删关注成员后应仍有关注成员")
        assertEquals(me.id, focus!!.id, "关注应转移给剩余的我")
        assertTrue(fam.listMembers().first { it.id == me.id }.isFocus, "我应被标为关注")
    }
}
