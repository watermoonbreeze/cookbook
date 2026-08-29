package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * @File : MealInputHelpSheet
 * @Time : 2026/08/29
 * @Author : SXD-AI
 * @Desc : AI 快捷记一餐 · 输入说明弹窗（统一入口顶栏 ⓘ / 首次进入自动弹 共用）。
 * <p>
 * [AI生成] 输入格式统一：由 AiMealInputSheet 旧 HelpSheet 迁出为共享组件并按新格式规范重写——
 * 三种写法（一行式/两行式/列表式）+ 菜品（食材1，食材2）括号食材；文案与规则解析器/AI prompt
 * 的格式约定同源（AiMealPrompt.kt「输入格式约定」行）。视觉沿用 HelpSheet 既有 token
 * （灰卡分节 + bodyMedium/行高22sp），新增"灰卡内白底等宽示例块"承载可照抄的输入示例。
 * 设计规范由 apple_ux_designer 出具（2026-08-29）：节标题无 emoji、说明类内容不加"知道了"假按钮。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MealInputHelpSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "AI 快捷怎么用",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "一句话或几行字就能记一餐，三种写法挑顺手的用。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            // ── 三种写法（本弹窗核心：与解析器格式约定同源）──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "三种写法",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "一行式",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    FormatExample("晚餐：红烧肉，米饭，炒青菜")

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "两行式（餐次一行，菜品一行）",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    FormatExample("午餐\n番茄炒蛋，米饭，紫菜汤")

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "列表式（一行一个菜）",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    FormatExample("晚餐\n清蒸鲈鱼\n蒜蓉西兰花\n米饭")

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "这里记的是单天，不用写日期。要记多天，切到上方的「周期」：一天一个框，分别填当天吃的就行——分开记，识别也更稳。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            HelpSection(
                title = "想写细一点，可以带上食材",
                content = "菜名后加括号写食材：西红柿炒鸡蛋（西红柿，鸡蛋）。\n不写也行，会按菜名自动拆出食材。",
            )
            Spacer(Modifier.height(16.dp))

            HelpSection(
                title = "餐次怎么说",
                content = "早餐、午餐、晚餐、加餐都认识；\n说「早饭、午饭、晚饭、下午茶、宵夜」也可以。",
            )
            Spacer(Modifier.height(16.dp))

            HelpSection(
                title = "还能识别什么",
                content = "• 日期：今天/昨天/前天/明天\n" +
                        "• 份量：一碗/两盘/三个/半份\n" +
                        "• 吃多少：吃完/吃了一半/吃了少量\n" +
                        "• 备注：少盐/少油/不要辣",
            )
            Spacer(Modifier.height(16.dp))

            HelpSection(
                title = "注意",
                content = "• 越具体越好，说清菜名和份量\n" +
                        "• 新菜会自动创建，营养来自基础数据估算\n" +
                        "• 发送前可编辑文字，发送后先预览确认，再入库\n" +
                        "• 文字只在记餐时发送，不保存",
            )
        }
    }
}

/** 纯文本说明分节卡（沿用旧 HelpSection 视觉 token）。[AI生成] */
@Composable
private fun HelpSection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )
        }
    }
}

/**
 * 格式示例块：灰卡内的白底等宽块——一眼认出"这是要照着输入的字"。
 * [AI生成] 设计规范：RoundedCornerShape(8dp) + surface 底 + Monospace + 行高 20sp。
 */
@Composable
private fun FormatExample(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 20.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}
