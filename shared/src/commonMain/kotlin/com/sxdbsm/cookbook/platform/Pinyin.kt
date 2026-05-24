package com.sxdbsm.cookbook.platform

/**
 * 拼音转换。Android 端可用第三方库 pinyin4j，iOS 端用 CFStringTransform。
 *
 * MVP 暂用简化实现：返回原字符串（搜索功能仍能用 name 和 alias 匹配）。
 * 一期再接入真正的拼音转换库。
 */
expect object Pinyin {
    /** 取中文字符串的拼音全拼，如 "西红柿" → "xihongshi" */
    fun toPinyin(text: String): String
}
