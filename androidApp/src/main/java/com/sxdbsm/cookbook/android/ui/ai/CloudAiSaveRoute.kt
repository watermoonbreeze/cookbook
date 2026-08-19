package com.sxdbsm.cookbook.android.ui.ai

import com.sxdbsm.cookbook.ai.CloudAiConsent
import com.sxdbsm.cookbook.ai.ConsentStatus

/**
 * @File : CloudAiSaveRoute
 * @Time : 2026/08/08
 * @Author : SXD-AI
 * @Desc : 保存 Key 的分流路径纯函数（非 Compose，可直接 JVM 单测）。蓝图 L1 §4.4（INV-L1-04/05/10）。
 * <p>
 * 从 AiSettingsScreen 的 KeyDialog 保存回调中抽出，避免分流逻辑内联在 Composable 里无法脱离 Compose 单测。
 * [AI生成] L1。
 **/
enum class SaveRoute { DIRECT, VENDOR_CONFIRM, FULL_CONSENT }

/** 保存 Key 时应走哪条路径（INV-L1-04/05）。[AI生成] */
fun routeOnSave(consent: CloudAiConsent, vendor: String, key: String): SaveRoute {
    if (key.isBlank()) return SaveRoute.DIRECT
    val satisfied = consent.status == ConsentStatus.GRANTED && consent.scopeVersion >= CloudAiDisclosure.SCOPE_VERSION
    if (!satisfied) return SaveRoute.FULL_CONSENT
    return if (vendor in consent.acknowledgedVendors) SaveRoute.DIRECT else SaveRoute.VENDOR_CONFIRM
}

/** [AI生成] v2b：AI 设置页常驻状态块是否渲染（INV-L1-10），抽纯函数供 JVM 单测覆盖（蓝图 §10 v2 挑战第19项）。 */
fun shouldShowCloudStatusBlock(consent: CloudAiConsent, keyByVendor: Map<String, String>, vendor: String): Boolean =
    consent.status == ConsentStatus.GRANTED && keyByVendor[vendor].orEmpty().isNotBlank()

/** [AI生成] 重新启用云端 AI 时应带入的 Key 草稿——沿用已保存的 Key，不清空（回应真机 E-L1-03）。（INV-L1-12，回应真机 E-L1-03） */
fun reenableKeyDraft(keyByVendor: Map<String, String>, vendor: String): String =
    keyByVendor[vendor].orEmpty()
