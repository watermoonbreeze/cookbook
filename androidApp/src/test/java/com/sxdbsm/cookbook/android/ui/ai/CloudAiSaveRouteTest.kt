package com.sxdbsm.cookbook.android.ui.ai

import com.sxdbsm.cookbook.ai.CloudAiConsent
import com.sxdbsm.cookbook.ai.ConsentSource
import com.sxdbsm.cookbook.ai.ConsentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L1 蓝图 §8.2：`routeOnSave`/`shouldShowCloudStatusBlock`/`reenableKeyDraft` 纯函数单测（JVM，无需 Compose/Robolectric）。
 *
 * INV-L1-04/05/08/10——分流逻辑从 Composable 抽出纯函数后可直接 JVM 单测（蓝图 §10 C-19）。
 * [AI生成] L1。
 */
class CloudAiSaveRouteTest {

    private fun grantedConsent(acknowledgedVendors: Set<String> = setOf("zhipu")) = CloudAiConsent(
        status = ConsentStatus.GRANTED,
        source = ConsentSource.EXPLICIT_FIRST_ENABLE,
        scopeVersion = CloudAiDisclosure.SCOPE_VERSION,
        acknowledgedVendors = acknowledgedVendors,
    )

    // ── INV-L1-04：routeOnSave 三分支 ──

    @Test
    fun `T-L1-04a GRANTED且已确认vendor 返回 DIRECT`() {
        assertEquals(SaveRoute.DIRECT, routeOnSave(grantedConsent(setOf("zhipu")), "zhipu", "sk-abc"))
    }

    @Test
    fun `T-L1-04b GRANTED但vendor未确认 返回 VENDOR_CONFIRM`() {
        assertEquals(SaveRoute.VENDOR_CONFIRM, routeOnSave(grantedConsent(setOf("zhipu")), "deepseek", "sk-abc"))
    }

    @Test
    fun `T-L1-04c 未满足状态 返回 FULL_CONSENT`() {
        assertEquals(SaveRoute.FULL_CONSENT, routeOnSave(CloudAiConsent(status = ConsentStatus.NOT_ASKED), "zhipu", "sk-abc"))
        assertEquals(SaveRoute.FULL_CONSENT, routeOnSave(CloudAiConsent(status = ConsentStatus.DECLINED), "zhipu", "sk-abc"))
        assertEquals(SaveRoute.FULL_CONSENT, routeOnSave(CloudAiConsent(status = ConsentStatus.GRANDFATHER_PENDING), "zhipu", "sk-abc"))
        // scopeVersion 落后于当前披露版本 → 需重新完整同意
        assertEquals(SaveRoute.FULL_CONSENT, routeOnSave(grantedConsent().copy(scopeVersion = CloudAiDisclosure.SCOPE_VERSION - 1), "zhipu", "sk-abc"))
    }

    // ── INV-L1-05：清空 Key 恒 DIRECT ──

    @Test
    fun `T-L1-05 空Key任意consent态恒DIRECT`() {
        assertEquals(SaveRoute.DIRECT, routeOnSave(CloudAiConsent(status = ConsentStatus.DECLINED), "zhipu", ""))
        assertEquals(SaveRoute.DIRECT, routeOnSave(CloudAiConsent(status = ConsentStatus.NOT_ASKED), "zhipu", "  "))
        assertEquals(SaveRoute.DIRECT, routeOnSave(grantedConsent(), "zhipu", ""))
    }

    // ── INV-L1-08：DECLINED 与 NOT_ASKED 走相同分支（不因"拒绝过"特殊处理） ──

    @Test
    fun `T-L1-08 DECLINED非空Key与NOT_ASKED一致 返回 FULL_CONSENT`() {
        assertEquals(
            routeOnSave(CloudAiConsent(status = ConsentStatus.NOT_ASKED), "zhipu", "sk-abc"),
            routeOnSave(CloudAiConsent(status = ConsentStatus.DECLINED), "zhipu", "sk-abc"),
        )
        assertEquals(SaveRoute.FULL_CONSENT, routeOnSave(CloudAiConsent(status = ConsentStatus.DECLINED), "zhipu", "sk-abc"))
    }

    // ── INV-L1-10：shouldShowCloudStatusBlock 双条件（GRANTED 且当前厂商 Key 非空） ──

    @Test
    fun `T-L1-10 状态块渲染条件双条件`() {
        assertFalse(shouldShowCloudStatusBlock(grantedConsent(), mapOf("zhipu" to ""), "zhipu")) // GRANTED+空Key → 不渲染
        assertTrue(shouldShowCloudStatusBlock(grantedConsent(), mapOf("zhipu" to "sk-abc"), "zhipu")) // GRANTED+非空Key → 渲染
        assertFalse(shouldShowCloudStatusBlock(CloudAiConsent(status = ConsentStatus.DECLINED), mapOf("zhipu" to "sk-abc"), "zhipu"))
        assertFalse(shouldShowCloudStatusBlock(CloudAiConsent(status = ConsentStatus.NOT_ASKED), mapOf("zhipu" to "sk-abc"), "zhipu"))
        assertFalse(shouldShowCloudStatusBlock(CloudAiConsent(status = ConsentStatus.GRANDFATHER_PENDING), mapOf("zhipu" to "sk-abc"), "zhipu"))
    }

    // ── hotfix E-L1-03：reenableKeyDraft ──

    @Test
    fun `T-L1-03a reenableKeyDraft在已配置Key时返回该Key`() {
        assertEquals("sk-abc123", reenableKeyDraft(mapOf("zhipu" to "sk-abc123"), "zhipu"))
    }

    @Test
    fun `T-L1-03b reenableKeyDraft在该厂商未配置Key时返回空串`() {
        assertEquals("", reenableKeyDraft(mapOf("deepseek" to "sk-xyz"), "zhipu"))
    }
}
