package com.sxdbsm.cookbook.ai

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : MultiTurnChatTest
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : AI 对话生成前置基建——多轮 chat 默认折叠 + GlmProtocol 多轮 body 组装。
 * [AI生成]
 **/
class MultiTurnChatTest {

    /** 捕获传给 complete 的 LlmRequest，验证默认 chat 的折叠逻辑。[AI生成] */
    private class CapturingRuntime : AiRuntime {
        var captured: LlmRequest? = null
        override suspend fun complete(request: LlmRequest): Result<String> {
            captured = request
            return Result.success("ok")
        }
    }

    @Test
    fun 默认chat把system与对话历史折叠进单轮() {
        runBlocking {
            val rt = CapturingRuntime()
            val req = LlmChatRequest(
                listOf(
                    LlmMessage(LlmRole.SYSTEM, "你是助手"),
                    LlmMessage(LlmRole.USER, "早餐吃啥"),
                    LlmMessage(LlmRole.ASSISTANT, "小米粥配蛋"),
                    LlmMessage(LlmRole.USER, "换一个"),
                ),
            )
            val r = rt.chat(req)
            assertEquals("ok", r.getOrNull())
            assertEquals("你是助手", rt.captured!!.system)
            val u = rt.captured!!.user
            assertTrue(u.contains("用户: 早餐吃啥"))
            assertTrue(u.contains("助手: 小米粥配蛋"))
            assertTrue(u.contains("用户: 换一个"))
        }
    }

    @Test
    fun Mock默认chat返回canned兜底() {
        runBlocking {
            val rt = MockAiRuntime("{\"dish\":\"番茄炒蛋\"}")
            val r = rt.chat(LlmChatRequest(listOf(LlmMessage(LlmRole.USER, "推荐一道菜"))))
            assertEquals("{\"dish\":\"番茄炒蛋\"}", r.getOrNull())
        }
    }

    @Test
    fun GlmProtocol多轮body含全部消息与角色映射() {
        val body = GlmProtocol.buildChatRequestBody(
            "glm-4-flash",
            listOf(
                LlmMessage(LlmRole.SYSTEM, "sys"),
                LlmMessage(LlmRole.USER, "u1"),
                LlmMessage(LlmRole.ASSISTANT, "a1"),
                LlmMessage(LlmRole.USER, "u2"),
            ),
            temperature = 0.4,
        )
        assertTrue(body.contains("\"role\":\"system\""))
        assertTrue(body.contains("\"role\":\"user\""))
        assertTrue(body.contains("\"role\":\"assistant\""))
        assertTrue(body.contains("sys") && body.contains("u1") && body.contains("a1") && body.contains("u2"))
    }
}
