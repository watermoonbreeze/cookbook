package com.sxdbsm.cookbook.android.ai

import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.GlmProtocol
import com.sxdbsm.cookbook.ai.LlmRequest
import com.sxdbsm.cookbook.android.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * @File : CloudAiRuntime
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 云端 AI 运行时（智谱 GLM-4-Flash，免费）
 * <p>
 * 用 HttpURLConnection 调 OpenAI 兼容的 chat/completions，零额外依赖；JSON 编解码交给 shared 的 GlmProtocol。
 * Key 从 AiRuntimeConfig 读取（只存本机、不写日志）。失败返回 Result.failure，由 Orchestrator 回退纯规则推荐。
 * <p>
 * [AI生成] S2：接真实云端；换厂商只改 ENDPOINT/MODEL/鉴权，业务不动。
 **/
class CloudAiRuntime(private val config: AiRuntimeConfig) : AiRuntime {

    override suspend fun complete(request: LlmRequest): Result<String> = withContext(Dispatchers.IO) {
        val model = config.selectedModel()
        val key = config.currentCloudApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("${model.vendorName} API Key 未配置"))
        }
        // [AI修改] 修超时(2026-07-19):对 GLM-4.5/4.6 关默认思考(在 buildRequestBody 内按 model 名 gate)+显式 max_tokens 上限,防思考吃满/话痨截断。
        val body = GlmProtocol.buildRequestBody(model.model, request.system, request.user, request.temperature, jsonMode = model.supportsJsonMode, maxTokens = RESPONSE_MAX_TOKENS)
        AppLogger.d("CloudAi", "req[${model.id}] endpoint=${model.endpoint} body=$body") // [AI生成] 请求日志(prompt只含食材/约束标签/候选菜名,无敏感健康档案)。
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching { postOnce(model.endpoint, key, body) }
            result.onSuccess { return@withContext Result.success(it) }
            lastError = result.exceptionOrNull()
            AppLogger.w("CloudAi", "[${model.id}] attempt ${attempt + 1} failed: ${lastError?.message}") // 仅记错误摘要，不记内容。
        }
        Result.failure(lastError ?: IOException("unknown"))
    }

    private fun postOnce(endpoint: String, key: String, body: String): String {
        val started = System.currentTimeMillis()
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            AppLogger.d("CloudAi", "http=$code cost=${System.currentTimeMillis() - started}ms resp=$text") // [AI修改] 深挖：记状态/耗时/原始响应。
            if (code !in 200..299) throw IOException("HTTP $code")
            return GlmProtocol.parseContent(text) ?: throw IOException("empty content")
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        // endpoint/model 来自 CloudModels（选中模型），支持多厂商 OpenAI 兼容接口。
        private const val CONNECT_TIMEOUT = 15000
        // [AI修改] 修超时(2026-07-19):读取超时 30s→45s 留冗余(慢网络/大响应)。主因是 GLM-4.5 默认思考拖慢——已在 buildRequestBody 关思考,此处再加保险,宁可慢成功也别误判失败(失败会回退规则,但用户已配模型时应尽量用上)。
        private const val READ_TIMEOUT = 45000
        private const val RESPONSE_MAX_TOKENS = 2048 // [AI生成] 输出上限:够当前3组餐搭配 JSON+未来均衡档多组预取,又防话痨截断。
        private const val MAX_ATTEMPTS = 2 // 首次失败重试一次。
    }
}
