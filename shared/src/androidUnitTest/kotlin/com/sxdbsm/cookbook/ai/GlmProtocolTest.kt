package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : GlmProtocolTest
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 云端请求体组装单测（R3 JSON 强约束模式）
 * <p>
 * [AI生成] R3：验证 jsonMode 开时请求体带 response_format:json_object、关时不带(老模型兼容)。
 **/
class GlmProtocolTest {

    @Test
    fun `jsonMode开_请求体含response_format_json_object`() {
        val body = GlmProtocol.buildRequestBody("glm-4.5-flash", "sys", "usr", 0.3, jsonMode = true)
        assertTrue(body.contains("response_format"), "jsonMode 开应含 response_format")
        assertTrue(body.contains("json_object"), "应为 json_object 类型")
    }

    @Test
    fun `jsonMode关_请求体不带response_format_老模型兼容`() {
        val body = GlmProtocol.buildRequestBody("glm-4-flash", "sys", "usr", 0.3, jsonMode = false)
        assertFalse(body.contains("response_format"), "jsonMode 关不应带 response_format(老模型不支持·带了会400)")
    }

    @Test
    fun `默认云端模型为glm45flash且免费支持JSON`() {
        val d = CloudModels.DEFAULT
        assertTrue(d.model == "glm-4.5-flash", "默认模型换为 glm-4.5-flash")
        assertTrue(d.free, "默认模型免费")
        assertTrue(d.supportsJsonMode, "默认模型支持 JSON 模式")
        // glm-4-flash 仍在列表(老用户 key 通用·可选)，但不支持 JSON。
        val old = CloudModels.byId("zhipu_glm4_flash")
        assertFalse(old.supportsJsonMode, "老 glm-4-flash 不支持 JSON 模式")
    }

    // ============================================================
    // B2 SSE 流式解析测试
    // ============================================================

    @Test
    fun `B2 buildStreamRequestBody 包含 stream_true`() {
        val body = GlmProtocol.buildStreamRequestBody("glm-4.5-flash", "sys", "usr", 0.3)
        assertTrue(body.contains("\"stream\":true"), "流式请求体应包含 stream:true")
    }

    @Test
    fun `B2 buildStreamRequestBody GLM45 关思考`() {
        val body = GlmProtocol.buildStreamRequestBody("glm-4.5-flash", "sys", "usr", 0.3)
        assertTrue(body.contains("\"type\":\"disabled\""), "GLM-4.5 流式请求应关思考")
    }

    @Test
    fun `B2 parseSseLine 解析普通delta内容`() {
        val dataLine = """{"choices":[{"delta":{"content":"番茄炒蛋"},"finish_reason":null}]}"""
        val chunk = GlmProtocol.parseSseLine(dataLine)
        assertEquals("番茄炒蛋", chunk.deltaContent)
        assertEquals(null, chunk.finishReason)
        assertFalse(chunk.isDone)
    }

    @Test
    fun `B2 parseSseLine 解析DONE标记`() {
        val chunk = GlmProtocol.parseSseLine("[DONE]")
        assertTrue(chunk.isDone)
        assertEquals("", chunk.deltaContent)
    }

    @Test
    fun `B2 parseSseLine 解析finish_reason等于stop`() {
        val dataLine = """{"choices":[{"delta":{},"finish_reason":"stop"}]}"""
        val chunk = GlmProtocol.parseSseLine(dataLine)
        assertEquals("stop", chunk.finishReason)
        assertEquals("", chunk.deltaContent)
    }

    @Test
    fun `B2 parseSseLine 解析finish_reason等于length`() {
        val dataLine = """{"choices":[{"delta":{"content":"半截"},"finish_reason":"length"}]}"""
        val chunk = GlmProtocol.parseSseLine(dataLine)
        assertEquals("length", chunk.finishReason)
        assertEquals("半截", chunk.deltaContent)
    }

    @Test
    fun `B2 parseSseLine 非法JSON返回空chunk`() {
        val chunk = GlmProtocol.parseSseLine("not valid json")
        assertEquals("", chunk.deltaContent)
        assertEquals(null, chunk.finishReason)
        assertFalse(chunk.isDone)
    }

    @Test
    fun `B2 parseSseLine choices为空数组`() {
        val chunk = GlmProtocol.parseSseLine("""{"choices":[]}""")
        assertEquals("", chunk.deltaContent)
        assertEquals(null, chunk.finishReason)
    }

    @Test
    fun `B2 buildStreamRequestBody 非流式请求不含stream字段`() {
        val body = GlmProtocol.buildRequestBody("glm-4.5-flash", "sys", "usr", 0.3)
        assertFalse(body.contains("\"stream\""), "非流式请求不应含 stream 字段")
    }
}
