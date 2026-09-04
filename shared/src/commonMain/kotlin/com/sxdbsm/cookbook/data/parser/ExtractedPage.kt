package com.sxdbsm.cookbook.data.parser

import com.sxdbsm.cookbook.data.seed.SeedResourceLoader
import com.sxdbsm.cookbook.domain.model.ParseConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** WebView 一次 JS 注入采集的页面快照（结构化字段+块文本）。[AI生成] STEP-L4-2.3 */
@Serializable
data class ExtractedPage(
    val title: String = "",
    val author: String = "",
    val imageUrl: String = "",
    val stepImages: List<String> = emptyList(),
    val innerText: String = "",
)

/**
 * 解析 evaluateJavascript 回调原始串为 [ExtractedPage]。[AI生成] STEP-L4-2.3 冻结实现（蓝图 F24/GC-37#9）。
 *
 * 回调值是**双层 JSON 编码**（外层字符串包裹内层对象）：先 decodeFromString<String> 解一层
 * 得到真正的 JSON 文本，再解对象；innerText 的换行转义由第二层解码自然还原。
 * 剥层失败/空串/"null" 一律返回 null（调用方走 FAILED 分支）。
 */
fun extractPageFromJson(raw: String?): ExtractedPage? {
    if (raw.isNullOrBlank() || raw == "null") return null
    return runCatching {
        val text = Json.decodeFromString<String>(raw)
        Json { ignoreUnknownKeys = true }.decodeFromString<ExtractedPage>(text)
    }.getOrNull()
}

/**
 * 按资源路径加载解析配置（DP-P1-10 路由表消费方）。[AI生成] STEP-L4-9.2 审核修正(2026-09-04)。
 *
 * 下沉 shared：androidApp 侧 ParseViewModel 经此加载（与测试同一条路径·Q-02/Y-4），androidApp
 * 无须自引 kotlinx-serialization（蓝图零触 gradle 红线）。path 为 null（来源不支持）或加载/解码失败均返 null。
 */
fun loadParseConfig(path: String?): ParseConfig? {
    if (path == null) return null
    return runCatching {
        SeedResourceLoader.readText(path)?.let { Json { ignoreUnknownKeys = true }.decodeFromString<ParseConfig>(it) }
    }.getOrNull()
}
