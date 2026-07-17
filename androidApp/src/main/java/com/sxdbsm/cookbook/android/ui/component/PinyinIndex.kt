package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

/**
 * 右侧字母定位条：拖动/点击某字母触发 [onLetterSelected]。[AI生成]
 *
 * 通用组件(原菜品页私有)，供菜品/食材库存等长列表右侧首字母定位复用。调用方负责把字母映射到滚动位置。
 */
@Composable
fun LetterIndexBar(
    letters: List<String>,
    modifier: Modifier = Modifier,
    onLetterSelected: (String) -> Unit,
) {
    var active by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f), MaterialTheme.shapes.small)
            .padding(vertical = 4.dp)
            .pointerInput(letters) {
                detectDragGestures(
                    onDragEnd = { active = null },
                    onDragCancel = { active = null },
                ) { change, _ ->
                    if (letters.isEmpty()) return@detectDragGestures
                    val index = ((change.position.y / size.height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
                    active = letters[index]
                    onLetterSelected(letters[index])
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val selected = active == letter
            Text(
                text = letter,
                style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(if (selected) 28.dp else 22.dp)
                    .height(if (selected) 28.dp else 20.dp)
                    .clickable {
                        active = letter
                        onLetterSelected(letter)
                    },
            )
        }
    }
}
