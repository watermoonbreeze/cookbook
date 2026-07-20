package com.sxdbsm.cookbook.domain

/**
 * @File : NumberInput
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 用户数字输入的容错解析——防"以小数点结尾/开头"致 toDoubleOrNull 恒 null、值静默丢失
 * <p>
 * 输入框为不打断打字，通常放行中途态 "30." / ".5"；但保存时直接 `toDoubleOrNull("30.")` = null → 该字段静默变空
 * (身高/体重/营养值丢失·踩坑红线)。本函数在**解析/保存点**归一后再解析，稳住数据正确性。
 * <p>
 * [AI生成] UX 走查 H3：数字输入结尾/开头小数点容错。纯函数、可单测。
 **/

/** 解析用户小数输入：容错结尾/开头多余小数点("30."→30.0、".5"→0.5、"."/""→null、"1.7.5"→null)。[AI生成] */
fun parseDecimalInput(s: String): Double? {
    val t = s.trim().trimEnd('.')
    if (t.isEmpty()) return null
    val normalized = if (t.startsWith('.')) "0$t" else t
    return normalized.toDoubleOrNull()
}
