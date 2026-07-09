package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @File : DeviceAiGradingTest
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 端侧设备自测分级阈值单测
 * <p>
 * [AI生成] 审核建议：阈值判定下沉 shared 后补边界单测。
 **/
class DeviceAiGradingTest {

    @Test
    fun `非arm64一律不建议`() {
        assertEquals(DeviceAiGrade.UNSUPPORTED, DeviceAiGrading.gradeFor(totalRamGb = 16.0, cores = 8, arm64 = false))
    }

    @Test
    fun `高配arm64为流畅`() {
        assertEquals(DeviceAiGrade.SMOOTH, DeviceAiGrading.gradeFor(totalRamGb = 7.0, cores = 8, arm64 = true))
        assertEquals(DeviceAiGrade.SMOOTH, DeviceAiGrading.gradeFor(totalRamGb = 12.0, cores = 8, arm64 = true))
    }

    @Test
    fun `核数不足降为可用`() {
        // 内存够但核数<8，不进 SMOOTH
        assertEquals(DeviceAiGrade.USABLE, DeviceAiGrading.gradeFor(totalRamGb = 8.0, cores = 6, arm64 = true))
    }

    @Test
    fun `中低内存分档`() {
        assertEquals(DeviceAiGrade.USABLE, DeviceAiGrading.gradeFor(totalRamGb = 5.5, cores = 8, arm64 = true))
        assertEquals(DeviceAiGrade.SLOW, DeviceAiGrading.gradeFor(totalRamGb = 3.5, cores = 8, arm64 = true))
        assertEquals(DeviceAiGrade.SLOW, DeviceAiGrading.gradeFor(totalRamGb = 5.4, cores = 8, arm64 = true))
    }

    @Test
    fun `极低内存不建议`() {
        assertEquals(DeviceAiGrade.UNSUPPORTED, DeviceAiGrading.gradeFor(totalRamGb = 3.4, cores = 8, arm64 = true))
        assertEquals(DeviceAiGrade.UNSUPPORTED, DeviceAiGrading.gradeFor(totalRamGb = 2.0, cores = 4, arm64 = true))
    }
}
