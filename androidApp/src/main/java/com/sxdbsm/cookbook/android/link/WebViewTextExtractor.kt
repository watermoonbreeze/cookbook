package com.sxdbsm.cookbook.android.link

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebView 页面采集器——一次 JS 注入提取菜谱结构化字段+块文本（分享链接解析·蓝图 STEP-L4-7）。[AI生成]
 *
 * **主线程调用**（调用方保证——WebView 创建与 loadUrl 须在主线程，本类不做线程切换）。
 *
 * 生命周期红线（蓝图 §6 对象表·GC-14）：WebView 由 [extract] 方法局部创建、**不 attach 任何
 * 视图**（后台不可见采集·方案 附一红线 6）；成功/error/超时三路结束均经 [finish] 兜底
 * `stopLoading/removeAllViews/destroy`（INV-L4-11：destroy 必达）。
 *
 * 回调单发保证（蓝图 S-02）：三路（JS 成功 / onReceivedError / 20s 超时）共用每次 extract
 * 独立的 `AtomicBoolean once`，先 `compareAndSet(false,true)` 抢到才回调——迟到的第二路
 * 直接丢弃，杜绝双发；抢到后先 removeCallbacks 撤掉超时任务。
 *
 * JS 为方案 §3.3 冻结原文（表达式形式，直接可作 evaluateJavascript 入参）；步骤图块 id
 * 前缀选择器与 `#steps` 容器**硬编码**——Phase2 由 ParseConfig 的 selector 配置接管
 * （蓝图 S-05 死配置注记，模板迭代时才改）。回调值是 evaluateJavascript 的**双层 JSON
 * 编码**（蓝图 F24），本类原样上抛，由 `ExtractedPage.extractPageFromJson` 双层解码。
 */
class WebViewTextExtractor(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 加载 [url] 并在页面完成后注入 JS 采集，结果单次回调 [onResult]。[AI生成]
     *
     * @param onResult 单发回调（三路 CAS 保证恰一次）：成功=evaluateJavascript 原始返回值
     *   （双层 JSON 编码字符串，可能为 "null"——JS 出错时 WebView 的行为，由解码层兜）；
     *   失败/超时=null。
     */
    fun extract(url: String, onResult: (String?) -> Unit) {
        // 回调单发保证：once 为每次 extract 的局部量——重试会再建新 WebView 与新 once。[AI生成]
        val once = AtomicBoolean(false)
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            // 不 attach 任何视图：纯后台采集，用户只见 BottomSheet（方案 附一红线 6）。[AI生成]
        }
        val timeoutRunnable = object : Runnable {
            override fun run() {
                deliver(webView, once, onResult, this, null)
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, finishedUrl: String?) {
                view.evaluateJavascript(EXTRACT_JS) { json ->
                    deliver(webView, once, onResult, timeoutRunnable, json)
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                // API 23+：仅主框架错误判采集失败；子资源（favicon/图）失败不干扰整页采集。[AI生成]
                if (request.isForMainFrame) deliver(webView, once, onResult, timeoutRunnable, null)
            }

            @Suppress("DEPRECATION")
            @Deprecated("Deprecated in Java")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
                // API 21/22 走旧签名（无 request 可辨主/子框架）——保守判失败，用户可重试兜底。[AI生成]
                deliver(webView, once, onResult, timeoutRunnable, null)
            }
        }
        // 超时先于 loadUrl 注册，杜绝"极快完成回调先于超时注册"的窗口外竞态。[AI生成]
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS)
        webView.loadUrl(url)
    }

    /**
     * 单发投递：CAS 抢到才继续（第二路丢弃），撤掉超时任务后交 [finish]。[AI生成]
     */
    private fun deliver(
        webView: WebView,
        once: AtomicBoolean,
        onResult: (String?) -> Unit,
        timeoutRunnable: Runnable,
        output: String?,
    ) {
        if (!once.compareAndSet(false, true)) return
        mainHandler.removeCallbacks(timeoutRunnable)
        finish(webView, output, onResult)
    }

    /**
     * finally 语义：三路（成功/error/超时）统一经此销毁 WebView 再回调——destroy 必达、
     * 逐段 runCatching 保证回调不因清理异常而丢（蓝图 §6 对象表·INV-L4-11）。[AI生成]
     */
    private fun finish(webView: WebView, output: String?, onResult: (String?) -> Unit) {
        runCatching { webView.stopLoading() }
        runCatching { webView.removeAllViews() }
        runCatching { webView.destroy() }
        onResult(output)
    }

    companion object {
        private const val TIMEOUT_MS = 20_000L

        /**
         * 方案 §3.3 冻结原文（表达式形式·无 $ 模板字符可安全内嵌 Kotlin raw string）：
         * 结构化字段走 DOM 查询（h1/meta author/og:image），步骤图按块 id 数字序取
         * currentSrc||src||data-src（懒加载兼容），块文本走 innerText。[AI生成]
         */
        private const val EXTRACT_JS = """
            JSON.stringify({
                title: document.querySelector('h1')?.textContent?.trim() || '',
                author: document.querySelector('meta[name="author"]')?.content || '',
                imageUrl: document.querySelector('meta[property="og:image"]')?.content || '',
                stepImages: [...document.querySelectorAll("[id^='step-cover-']")]
                    .sort((a, b) => (parseInt(a.id.replace(/\D/g, '')) || 0) - (parseInt(b.id.replace(/\D/g, '')) || 0))
                    .map(el => el.querySelector('img')?.currentSrc || el.querySelector('img')?.src
                                || el.querySelector('img')?.getAttribute('data-src') || ''),
                innerText: document.body.innerText
            })
            """
    }
}
