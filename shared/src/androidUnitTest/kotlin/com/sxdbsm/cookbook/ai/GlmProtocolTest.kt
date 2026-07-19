package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertFalse
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
}
