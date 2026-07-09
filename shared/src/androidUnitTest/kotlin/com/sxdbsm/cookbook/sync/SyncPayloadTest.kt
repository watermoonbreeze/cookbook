package com.sxdbsm.cookbook.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @File : SyncPayloadTest
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 双设备同传二维码载荷编解码单测
 * <p>
 * [AI生成] 审核建议：协议下沉 shared 后补往返/异常单测。
 **/
class SyncPayloadTest {

    @Test
    fun `编码解码往返一致`() {
        val text = SyncPayload.encode("192.168.1.7", 8969, "1234")
        val parsed = SyncPayload.parse(text)
        assertEquals(Triple("192.168.1.7", "8969", "1234"), parsed)
    }

    @Test
    fun `含TAG前缀`() {
        assertEquals("COOKBOOKSYNC|10.0.0.2|5000|9999", SyncPayload.encode("10.0.0.2", 5000, "9999"))
    }

    @Test
    fun `非本应用二维码解析为null`() {
        assertNull(SyncPayload.parse("https://example.com"))
        assertNull(SyncPayload.parse("OTHER|a|b|c"))
        assertNull(SyncPayload.parse("COOKBOOKSYNC|only|three")) // 字段数不足
        assertNull(SyncPayload.parse(""))
    }

    @Test
    fun `多余字段视为无效`() {
        assertNull(SyncPayload.parse("COOKBOOKSYNC|ip|port|code|extra"))
    }
}
