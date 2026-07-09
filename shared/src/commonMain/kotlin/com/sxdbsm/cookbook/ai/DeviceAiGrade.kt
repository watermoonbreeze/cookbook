package com.sxdbsm.cookbook.ai

/**
 * @File : DeviceAiGrade
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 端侧本地模型「设备自测」的流畅度分级与阈值判定（纯逻辑，可单测）
 * <p>
 * 采集(内存/CPU/架构)是平台相关、留在各平台；**分级决策是纯逻辑**，下沉 shared 以便单测与跨平台复用。
 * 阈值按 GiB，偏保守宁可低估；真实速度以导入模型后实测为准。
 * <p>
 * [AI修改] 审核建议：阈值判定下沉 shared 并补单测。
 **/

/** 端侧运行流畅度等级。[AI生成] */
enum class DeviceAiGrade(val label: String, val desc: String) {
    SMOOTH("流畅", "内存充足，可较流畅运行 0.5B~1.5B 小参数本地模型"),
    USABLE("可用", "可运行小参数模型，生成速度中等，长文本会稍慢"),
    SLOW("偏慢", "可尝试，但生成较慢，建议用更小模型或改用云端"),
    UNSUPPORTED("不建议", "内存或架构不足，建议使用云端大模型或规则推荐"),
}

object DeviceAiGrading {
    /**
     * 按规格预估端侧模型流畅度。[AI生成]
     *
     * @param totalRamGb 设备总内存(GiB)
     * @param cores CPU 核数
     * @param arm64 是否支持 arm64-v8a（多数端侧运行时仅支持）
     */
    fun gradeFor(totalRamGb: Double, cores: Int, arm64: Boolean): DeviceAiGrade = when {
        !arm64 -> DeviceAiGrade.UNSUPPORTED
        totalRamGb >= 7.0 && cores >= 8 -> DeviceAiGrade.SMOOTH
        totalRamGb >= 5.5 -> DeviceAiGrade.USABLE
        totalRamGb >= 3.5 -> DeviceAiGrade.SLOW
        else -> DeviceAiGrade.UNSUPPORTED
    }
}
