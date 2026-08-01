package com.sxdbsm.cookbook.domain.autogen

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * @File : IngredientAliasResolver
 * @Time : 2026/08/01
 * @Author : SXD-AI
 * @Desc : 食材别名归一（K1f）——读 ingredient_aliases.json·名→归一名·纯函数可单测
 * <p>
 * 只映射高频~50-100 条别名；无命中返去空格原名。不依赖 DB、不写库。
 * <p>
 * [AI生成] 自动化基础能力层 Phase 1。
 **/
class IngredientAliasResolver(
    private val aliasMap: Map<String, String>,
) {
    /**
     * 归一食材名：去空格 → 查别名表 → 命中返归一名，无命中返去空格原名。[AI生成]
     */
    fun normalize(rawName: String): String {
        val trimmed = rawName.trim().replace(Regex("[\\s\\u3000]"), "")
        if (trimmed.isEmpty()) return rawName
        return aliasMap[trimmed] ?: trimmed
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** 从 ingredient_aliases.json 文本构建。[AI生成] */
        fun fromJson(jsonText: String): IngredientAliasResolver {
            val map = runCatching {
                json.decodeFromString<Map<String, String>>(jsonText)
            }.getOrDefault(emptyMap())
            // 过滤掉 _description 等元数据 key
            val aliasMap = map.filterKeys { !it.startsWith("_") }
            return IngredientAliasResolver(aliasMap)
        }
    }
}
