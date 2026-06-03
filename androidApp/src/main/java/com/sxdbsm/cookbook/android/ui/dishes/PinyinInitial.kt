package com.sxdbsm.cookbook.android.ui.dishes

import java.nio.charset.Charset

/**
 * @File : PinyinInitial
 * @Time : 2026/05/30
 * @Author : SXD-AI
 * @Desc : 菜品首字母索引工具
 * <p>
 * 用于菜品页“全部”列表的右侧字母索引。项目当前未引入完整拼音库，
 * 这里用 GBK 区位码估算常用汉字拼音首字母；英文菜名直接取首字母。
 * <p>
 * [AI生成] 支撑修复9中的拼音首字母排序和右侧索引滑动定位。
 **/
internal fun dishInitial(name: String): String {
    val first = name.trim().firstOrNull() ?: return "#"
    val upper = first.uppercaseChar()
    if (upper in 'A'..'Z') return upper.toString()
    val code = runCatching {
        val bytes = first.toString().toByteArray(Charset.forName("GBK"))
        if (bytes.size < 2) return@runCatching -1
        (bytes[0].toInt() and 0xff) * 256 + (bytes[1].toInt() and 0xff)
    }.getOrDefault(-1)
    if (code < 45217 || code > 55289) return "#"
    val ranges = intArrayOf(
        45217, 45253, 45761, 46318, 46826, 47010, 47297, 47614, 48119,
        49062, 49324, 49896, 50371, 50614, 50622, 50906, 51387, 51446,
        52218, 52698, 52980, 53689, 54481, 55290,
    )
    val letters = "ABCDEFGHJKLMNOPQRSTWXYZ"
    for (i in 0 until ranges.lastIndex) {
        if (code >= ranges[i] && code < ranges[i + 1]) return letters[i].toString()
    }
    return "#"
}
