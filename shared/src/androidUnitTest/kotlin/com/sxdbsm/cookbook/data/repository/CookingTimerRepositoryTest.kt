package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.domain.model.CookingTimerTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

/**
 * @File : CookingTimerRepositoryTest
 * @Time : 2026/06/12
 * @Author : SXD-AI
 * @Desc : 烹饪计时模板仓库单元测试
 * <p>
 * 覆盖倒计时模板保存、编辑、软删除和固定排序读取能力。
 * <p>
 * [AI生成] 用户要求厨房小助手倒计时模板持久化后补充 Repository 层回归测试。
 **/
class CookingTimerRepositoryTest {

    @Test
    fun saveEditAndDeleteTimerTemplate() = runBlocking {
        val repo = CookingTimerRepository(RepositoryTestDatabase.create())

        val firstId = repo.saveTemplate(
            CookingTimerTemplate(
                name = "蒸红薯",
                durationSeconds = 630,
                note = "水开后计时",
                ringtoneUri = "content://ringtone/1",
                ringtoneTitle = "厨房铃声",
            ),
        )
        val secondId = repo.saveTemplate(
            CookingTimerTemplate(
                name = "煮鸡蛋",
                durationSeconds = 480,
            ),
        )

        assertEquals(listOf("蒸红薯", "煮鸡蛋"), repo.listTemplates().map { it.name })

        repo.saveTemplate(
            CookingTimerTemplate(
                id = firstId,
                name = "蒸红薯块",
                durationSeconds = 720,
                note = "上汽后计时",
                ringtoneUri = "content://ringtone/2",
                ringtoneTitle = "提示音",
            ),
        )

        val edited = repo.listTemplates().first { it.id == firstId }
        assertEquals("蒸红薯块", edited.name)
        assertEquals(720, edited.durationSeconds)
        assertEquals("提示音", edited.ringtoneTitle)

        repo.deleteTemplate(secondId)

        assertEquals(listOf("蒸红薯块"), repo.listTemplates().map { it.name })
    }
}
