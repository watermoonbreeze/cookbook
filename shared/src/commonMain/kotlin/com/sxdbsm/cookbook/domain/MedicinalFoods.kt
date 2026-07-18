package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.data.seed.SeedResourceLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * @File : MedicinalFoods
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 药食同源白名单（国家卫健委法定目录·106 项）——纯事实标签维度，与慢病评级物理隔离
 * <p>
 * 一期"食补/药膳"最小集(PM 规划)：只做"药食同源"官方白名单这一法定、零分歧维度。
 * 数据=资源 `seed/yaoshi_tongyuan.json`(名+别名)，运行时按去空格名精确匹配，**不入库、免迁移**。
 * **红线：只做正向事实标签/筛选展示，绝不接入忌口/限量/慢病评级/罚分**（现代营养体系保持纯净）。
 * 措辞守"仅供参考·非医嘱；药食同源指食品安全管理认定，非疗效背书"，由 UI 承接。
 * <p>
 * [AI生成] 药膳食补维度一期（用户 2026-07-18 拍板做药食同源）。
 **/
object MedicinalFoods {

    @Serializable
    private data class Item(val name: String, val aliases: List<String> = emptyList())

    private val json = Json { ignoreUnknownKeys = true }

    /** 去空格名 → 是否药食同源（含名与别名）。懒加载一次，缓存。[AI生成] */
    private val names: Set<String> by lazy {
        val text = SeedResourceLoader.readText("seed/yaoshi_tongyuan.json") ?: return@lazy emptySet()
        val items = runCatching { json.decodeFromString<List<Item>>(text) }.getOrDefault(emptyList())
        val s = mutableSetOf<String>()
        items.forEach { item ->
            norm(item.name).takeIf { it.isNotEmpty() }?.let { s += it }
            item.aliases.forEach { a -> norm(a).takeIf { it.isNotEmpty() }?.let { s += it } }
        }
        s
    }

    private fun norm(n: String): String = n.trim().replace(" ", "")

    /** 该食材名是否属药食同源官方白名单（精确名/别名匹配，避免误标）。[AI生成] */
    fun isMedicinal(name: String): Boolean {
        val core = norm(name)
        return core.isNotEmpty() && core in names
    }

    /** 一组配料名里有几个药食同源（供 AI"食补"过滤按含量排序）。[AI生成] */
    fun countIn(ingredientNames: List<String>): Int = ingredientNames.count { isMedicinal(it) }

    /** 菜里是否含药食同源食材。[AI生成] */
    fun anyIn(ingredientNames: List<String>): Boolean = ingredientNames.any { isMedicinal(it) }
}
