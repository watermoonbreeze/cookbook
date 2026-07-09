package com.sxdbsm.cookbook.android.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * @File : DeviceAiCapability
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 端侧本地模型「设备自测」——按本机内存/CPU/架构预估能否流畅运行小参数模型
 * <p>
 * 用户选择端侧模型前，先给一个流畅度等级(流畅/可用/偏慢/不建议)让其有预期。
 * 当前为**规格预估**(总内存 + arm64 架构 + 核数)，非真实推理实测；端侧模型接入(Step2)后可升级为实测 tok/s。
 * <p>
 * [AI生成] Req: 选端侧模型时旁边加测试, 看手机速度给等级。
 **/

/** 端侧运行流畅度等级。[AI生成] */
enum class DeviceAiGrade(val label: String, val desc: String) {
    SMOOTH("流畅", "内存充足，可较流畅运行 0.5B~1.5B 小参数本地模型"),
    USABLE("可用", "可运行小参数模型，生成速度中等，长文本会稍慢"),
    SLOW("偏慢", "可尝试，但生成较慢，建议用更小模型或改用云端"),
    UNSUPPORTED("不建议", "内存或架构不足，建议使用云端大模型或规则推荐"),
}

/** 设备自测结果。[AI生成] */
data class DeviceAiReport(
    val grade: DeviceAiGrade,
    val totalRamGb: Double,
    val cores: Int,
    val arm64: Boolean,
    val deviceModel: String,
    val abi: String,
) {
    /** 规格摘要，供 UI 展示。 */
    fun specLine(): String {
        val ram = (Math.round(totalRamGb * 10) / 10.0)
        val arch = if (arm64) "arm64" else abi.ifBlank { "未知架构" }
        return "$deviceModel · 内存 ${ram}GB · ${cores}核 · $arch"
    }
}

object DeviceAiCapability {
    /**
     * 按本机规格预估端侧模型流畅度。[AI生成]
     *
     * 主要看总内存(小模型需 1~2GB 空闲) + 是否 arm64(多数端侧运行时仅支持) + 核数。
     * 阈值偏保守，宁可低估；真实速度以导入模型后实测为准。
     */
    fun evaluate(context: Context): DeviceAiReport {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalRamGb = memInfo.totalMem / GB
        val cores = Runtime.getRuntime().availableProcessors()
        val abis = Build.SUPPORTED_ABIS?.toList().orEmpty()
        val arm64 = abis.contains("arm64-v8a")
        val grade = when {
            !arm64 -> DeviceAiGrade.UNSUPPORTED // 端侧运行时基本只支持 arm64
            // [AI修改] 阈值按 GiB(系统预留后标称8GB约7.2~7.5GiB)：7.0 让主流8GB机进 SMOOTH。
            totalRamGb >= 7.0 && cores >= 8 -> DeviceAiGrade.SMOOTH
            totalRamGb >= 5.5 -> DeviceAiGrade.USABLE
            totalRamGb >= 3.5 -> DeviceAiGrade.SLOW
            else -> DeviceAiGrade.UNSUPPORTED
        }
        return DeviceAiReport(
            grade = grade,
            totalRamGb = totalRamGb,
            cores = cores,
            arm64 = arm64,
            deviceModel = Build.MODEL ?: "本机",
            abi = abis.firstOrNull().orEmpty(),
        )
    }

    private const val GB = 1024.0 * 1024.0 * 1024.0
}
