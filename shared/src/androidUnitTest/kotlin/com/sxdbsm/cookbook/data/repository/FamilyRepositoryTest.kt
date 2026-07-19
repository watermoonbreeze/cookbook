package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import com.sxdbsm.cookbook.domain.model.BodyMetrics
import com.sxdbsm.cookbook.domain.model.FamilyMember
import kotlinx.coroutines.flow.first
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
    fun `个人忌口具体食材_读写回显与全家并集与全量替换`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val fam = FamilyRepository(db, PreferenceRepository(db))
        fam.ensureInitialized()
        // 取两个真实食材 id
        val ingIds = db.cookbookQueries.selectAllIngredients().executeAsList().take(2).map { it.id }
        assertEquals(2, ingIds.size, "种子应有≥2食材")
        val a = ingIds[0]
        val b = ingIds[1]

        val me = fam.listMembers().first { it.isSelf }
        fam.updateMember(me.copy(avoidIngredientIds = listOf(a)))
        val dadId = fam.createMember(FamilyMember(id = 0, name = "爸", avoidIngredientIds = listOf(b)))
        assertTrue(dadId > 0)

        // 回显
        val meReloaded = fam.listMembers().first { it.isSelf }
        assertTrue(a in meReloaded.avoidIngredientIds, "我的具体食材忌口应回显")
        // 全家并集
        val union0 = fam.allPersonalAvoidIngredientIds().toSet()
        assertTrue(a in union0 && b in union0, "全家具体食材忌口取并集")
        // 全量替换：清掉我的、不影响爸的
        fam.updateMember(meReloaded.copy(avoidIngredientIds = emptyList()))
        val union1 = fam.allPersonalAvoidIngredientIds().toSet()
        assertTrue(a !in union1, "更新为空应移除我的")
        assertTrue(b in union1, "爸的具体食材忌口仍在")
        Unit
    }

    @Test
    fun `缺席微调_按天持久化且份额排除缺席者`() = runBlocking {
        val (fam, _, _) = setup()
        fam.ensureInitialized()
        val me = fam.listMembers().first()
        val dadId = fam.createMember(FamilyMember(id = 0, name = "爸"))
        fam.setFocus(me.id)
        val date = "2026-07-15"

        val share0 = fam.observeFocusShareForDate(date).first() // 无缺席：我 ÷ (我+爸)
        fam.setAbsent(date, dadId, true) // 爸没吃
        assertTrue(dadId in fam.observeAbsenteeIds(date).first(), "缺席应持久化")
        val share1 = fam.observeFocusShareForDate(date).first()
        assertTrue(share1 > share0, "缺席后关注成员份额变大")

        fam.setAbsent(date, me.id, true) // 关注成员自己也没吃
        assertEquals(0.0, fam.observeFocusShareForDate(date).first(), "关注成员当天缺席→份额0")

        fam.setAbsent(date, dadId, false) // 取消爸的缺席
        assertTrue(dadId !in fam.observeAbsenteeIds(date).first(), "取消缺席应生效")
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

    @Test
    fun `多人关注_toggleFocus保下限1且observe按当前查看指针自愈`() = runBlocking {
        val (fam, _, prefs) = setup()
        fam.ensureInitialized()
        val me = fam.listMembers().first { it.isSelf } // 默认 is_focus=1
        val dadId = fam.createMember(FamilyMember(id = 0, name = "爸"))
        // 多选关注:关注爸 → 我+爸都关注(不再单选清空)
        assertTrue(fam.toggleFocus(dadId))
        assertEquals(2, fam.listMembers().count { it.isFocus }, "多选:我+爸都关注")
        // 当前查看指针→爸,observeFocusName 跟随
        prefs.setFocusViewingMemberId(dadId)
        assertEquals("爸", fam.observeFocusName().first())
        // 取消关注爸 → 剩我;指针仍指爸(已不在关注集合)→自愈回退关注集合首位(我)
        assertTrue(fam.toggleFocus(dadId))
        assertEquals(me.name, fam.observeFocusName().first(), "指针失效自愈回退关注集合首位")
        // 取消最后一个关注(我) → 拒绝,至少留1
        assertTrue(!fam.toggleFocus(me.id), "取消最后一个关注人应被拒绝")
        assertEquals(1, fam.listMembers().count { it.isFocus })
        Unit
    }
}
