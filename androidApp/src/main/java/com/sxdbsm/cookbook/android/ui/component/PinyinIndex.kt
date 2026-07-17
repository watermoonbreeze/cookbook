package com.sxdbsm.cookbook.android.ui.component

import java.nio.charset.Charset

/**
 * @File : PinyinIndex
 * @Time : 2026/07/17
 * @Author : SXD-AI
 * @Desc : 通用「名称→拼音首字母」工具 + 字母索引条组件（跨列表复用）
 * <p>
 * 项目未引入完整拼音库，用 GBK 区位码估算常用汉字拼音首字母；英文名直接取首字母，其余归 "#"。
 * 原实现在菜品页 `dishes/PinyinInitial.kt(dishInitial)`，为支撑食材库存等其它列表复用字母定位而提取到通用组件层。
 * <p>
 * [AI生成] 用户要求库存按默认(拼音)排序 + 右侧首字母定位，并可复用到其它列表。
 **/

/** 取名称的拼音首字母(A-Z)，无法识别归 "#"。[AI生成] */
fun pinyinInitial(name: String): String {
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

/**
 * 按拼音首字母 + 名称排序的比较器。[AI生成]
 *
 * "#"(未识别)统一排到最后；同首字母内按名称本身排序，稳定可预期。
 */
fun <T> pinyinComparator(nameOf: (T) -> String): Comparator<T> =
    compareBy<T>({ pinyinInitial(nameOf(it)).let { c -> if (c == "#") "￿" else c } }, { nameOf(it) })
