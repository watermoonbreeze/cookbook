package com.sxdbsm.cookbook.platform

/**
 * MVP 简化实现：返回原字符串。
 * 待一期接入 pinyin4j 或 com.github.promeg:tinypinyin 提供真实拼音转换。
 */
actual object Pinyin {
    actual fun toPinyin(text: String): String = text.lowercase()
}
