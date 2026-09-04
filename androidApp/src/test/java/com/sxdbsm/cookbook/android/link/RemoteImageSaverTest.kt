package com.sxdbsm.cookbook.android.link

import com.sxdbsm.cookbook.android.ui.component.StoredImagePair
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * T-L4-10: RemoteImageSaver 失败兜底与批量并发下载测试（蓝图 STEP-L4-13·§8）。
 *
 * 环境与模式（对齐既有先例）：
 * - 纯 JVM 测试（androidApp 测试无 Robolectric 依赖），fake HttpURLConnection 模式照
 *   `StreamTransportTimeoutTest`（`HttpURLConnection(null)` 匿名子类 + 工厂注入）。
 * - `connectionFactory`/`dirProvider` 走 RemoteImageSaver 的可测试性注入（GC-33），context
 *   传空转 MockContext（注入 dirProvider 后该字段从不被触碰，不走任何 Android API）。
 *
 * 覆盖：
 * - 404（非 2xx，inputStream 抛 IOException）→ download 返 null 不抛（INV-L4-10）。
 * - 读超时（SocketTimeoutException）→ download 返 null 不抛（INV-L4-10）。
 * - 非图片内容（纯文本字节，BitmapFactory 解码失败）→ download 返 null 不抛。
 * - awaitAll：成功全保留；超时时已完成者保留、未完成者置 null 且 map 恰每 url 一条（DP-P1-6）。
 *
 * 已知局限（报告已注明）：「下载真实 JPEG → 压缩落盘成对文件」的成功路径需真实
 * BitmapFactory/Bitmap（纯 JVM 桩恒返回 null，到不了 savePair），须 Robolectric 或仪器测试；
 * 本项目测试依赖未含 Robolectric，故以 awaitAll 的 loader 注入路径覆盖成功收集语义。
 */
class RemoteImageSaverTest {

    /**
     * 空转 Context：仅满足非空构造参数；dirProvider 注入后 context 永不被触碰。[AI生成] T-L4-10
     * 用 Application（具体类·经 ContextWrapper 继承 Context）而非手写 Context 子类——
     * android.test.mock.MockContext 在 compileSdk 34 的 android.jar 已移除。
     */
    private class NoopContext : android.app.Application()

    /** fake connection（照 StreamTransportTimeoutTest 模式）：inputStreamError 优先抛（模拟 404/超时），否则回 bytes。[AI生成] */
    private fun fakeConn(
        bytes: ByteArray = ByteArray(0),
        inputStreamError: IOException? = null,
        disconnectCount: AtomicInteger = AtomicInteger(0),
    ): HttpURLConnection = object : HttpURLConnection(null) {
        override fun connect() { /* download 不显式 connect（getInputStream 隐式连接），故超时模拟放 getInputStream */ }
        override fun disconnect() { disconnectCount.incrementAndGet() }
        override fun usingProxy(): Boolean = false
        override fun getResponseCode(): Int = 200
        override fun getInputStream(): InputStream = inputStreamError?.let { throw it } ?: ByteArrayInputStream(bytes)
        override fun getErrorStream(): InputStream? = null
    }

    private fun saver(connectionFactory: (String) -> HttpURLConnection): RemoteImageSaver =
        RemoteImageSaver(
            context = NoopContext(),
            connectionFactory = connectionFactory,
            dirProvider = { java.nio.file.Files.createTempDirectory("remote-img-test").toFile() },
        )

    /** 用例② 404：fake 的 inputStream 抛 IOException（非 2xx 行为，生产 HttpURLConnection 同）→ null 不抛 + disconnect 兜底。[AI生成] */
    @Test
    fun downloadReturnsNullOnHttpError() {
        val disconnects = AtomicInteger(0)
        val saver = saver(
            connectionFactory = { fakeConn(inputStreamError = IOException("HTTP 404"), disconnectCount = disconnects) },
        )
        val result = runBlocking { saver.download("https://fake/img.jpg") }
        assertNull("404 应返回 null（失败不阻断·INV-L4-10）", result)
        assertTrue("disconnect 应被 finally 兜底调用", disconnects.get() >= 1)
    }

    /** 用例③ 超时：读阶段抛 SocketTimeoutException（生产 readTimeout 15s 到点的真实异常类型）→ null 不抛。[AI生成] */
    @Test
    fun downloadReturnsNullOnSocketTimeout() {
        val saver = saver(
            connectionFactory = { fakeConn(inputStreamError = SocketTimeoutException("read timeout")) },
        )
        val result = runBlocking { saver.download("https://fake/slow.jpg") }
        assertNull("超时应返回 null 不抛（INV-L4-10）", result)
    }

    /**
     * 用例④ 非图内容：纯文本字节 → BitmapFactory 解码失败 → download 返 null 不抛。[AI生成]
     * 注：纯 JVM 桩下 decodeByteArray 恒 null（与真实解码成功无法在此环境区分），本用例锁定
     * 「解码失败路径不抛异常、返 null」契约；成功解码路径见类注释局限说明。
     */
    @Test
    fun downloadReturnsNullOnNonImageBytes() {
        val saver = saver(
            connectionFactory = { fakeConn(bytes = "plain text, not an image at all".toByteArray(Charsets.UTF_8)) },
        )
        val result = runBlocking { saver.download("https://fake/fake.jpg") }
        assertNull("非图片内容应返回 null 不抛（INV-L4-10）", result)
    }

    /** 补充：awaitAll 成功路径——loader 注入 fake 成对结果，全部保留且 map 恰每 url 一条。[AI生成] */
    @Test
    fun awaitAllKeepsAllFinishedResults() {
        val saver = saver(connectionFactory = { throw AssertionError("loader 注入后不应触网") })
        val result = runBlocking {
            saver.awaitAll(setOf("https://fake/a.jpg", "https://fake/b.jpg"), loader = {
                StoredImagePair(imagePath = "a.jpg", thumbnailPath = "a_thum.jpg")
            })
        }
        assertEquals("每个 url 恰一个条目（null 也要有）", 2, result.size)
        assertNotNull("成功结果应保留", result["https://fake/a.jpg"])
        assertNotNull("成功结果应保留", result["https://fake/b.jpg"])
    }

    /**
     * 补充：awaitAll 超时——已完成者保留（getCompleted），未完成者取消置 null（DP-P1-6）。[AI生成]
     * 红线自查：快/慢 url 分别断言（fast 保留 + slow 为 null），若实现把 fast 也误取消或 slow 误保留都会红。
     */
    @Test
    fun awaitAllTimeoutKeepsFinishedAndNullsPending() {
        val saver = saver(connectionFactory = { throw AssertionError("loader 注入后不应触网") })
        val result = runBlocking {
            saver.awaitAll(
                urls = setOf("https://fake/fast.jpg", "https://fake/slow.jpg"),
                timeoutMs = 300,
                loader = { url ->
                    if (url == "https://fake/slow.jpg") {
                        delay(60_000) // 远超窗口，必被取消
                    }
                    StoredImagePair(imagePath = "x.jpg", thumbnailPath = "x_thum.jpg")
                },
            )
        }
        assertEquals("map 恰两键", 2, result.size)
        assertNotNull("窗口内已完成者应保留", result["https://fake/fast.jpg"])
        assertNull("未完成者应置 null", result["https://fake/slow.jpg"])
    }
}
