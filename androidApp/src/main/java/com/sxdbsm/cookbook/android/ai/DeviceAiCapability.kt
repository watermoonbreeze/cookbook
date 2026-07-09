package com.sxdbsm.cookbook.android.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.sxdbsm.cookbook.ai.DeviceAiGrade
import com.sxdbsm.cookbook.ai.DeviceAiGrading

/**
 * @File : DeviceAiCapability
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 端侧本地模型「设备自测」——采集本机内存/CPU/架构，调用 shared 分级逻辑
 * <p>
 * 用户选择端侧模型前，先给一个流畅度等级(流畅/可用/偏慢/不建议)让其有预期。
 * 采集为平台相关(本类)，**分级阈值判定在 shared `DeviceAiGrading`(可单测)**；当前为规格预估、非实测 tok/s。
 * <p>
 * [AI修改] 分级逻辑下沉 shared，本类只负责平台采集。
 **/

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
        val grade = DeviceAiGrading.gradeFor(totalRamGb, cores, arm64) // [AI修改] 分级下沉 shared
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
