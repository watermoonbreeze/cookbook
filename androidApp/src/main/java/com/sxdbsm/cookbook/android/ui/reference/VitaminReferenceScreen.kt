package com.sxdbsm.cookbook.android.ui.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.InsetGroup

/**
 * @File : VitaminReferenceScreen
 * @Time : 2026/07/25
 * @Author : SXD-AI
 * @Desc : 维生素专项科普页——用人话介绍各维生素干什么、从哪吃、缺了会怎样
 * <p>
 * 用户 2026-07-25：在「我的·参考资料」开一个维生素类专项。内容按营养学公认知识整理，
 * 每类维生素给「作用 / 食物来源 / 缺乏」，脂溶性(A/D/E/K)与水溶性(B族/C)分开。
 * 守健康免责红线：科普性质·非医嘱；每段配权威源(营养科学全书/膳食指南/DRIs)。范式仿参考页系列。
 * [AI生成] 用户 2026-07-25：维生素专项科普页。
 **/
@Composable
fun VitaminReferenceScreen(
    onBack: () -> Unit,
    onOpenHealthScience: () -> Unit = {}, // 互链:健康科普(食物消化吸收代谢)
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { AppTopBar(title = "维生素小百科", onBack = onBack) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DisclaimerBanner(
                    "下面按营养学公认知识介绍常见维生素的作用、食物来源和缺乏表现，帮你了解「吃什么补什么」，" +
                        "仅供科普、非医嘱。均衡饮食一般就能满足，是否需要额外补充请听医生的。",
                )
                Body("维生素是身体必需、但自己几乎不能合成、要靠吃来获得的一类微量营养素。分两大类：脂溶性（随脂肪吸收、能在体内存起来）和水溶性（不易存、要常吃）。")

                Body("一、脂溶性维生素（A·D·E·K）")
                InsetGroup(title = "能存起来，但补过量也可能中毒") {
                    VitaminBlock("🥕", "维生素 A", "护眼睛（暗光视力）、皮肤黏膜、免疫力。", "动物肝、蛋黄；深色蔬菜里的 β-胡萝卜素（胡萝卜、菠菜、南瓜）可在体内转成维A。", "缺：夜盲、皮肤干燥。")
                    VitaminBlock("☀️", "维生素 D", "帮助钙吸收、强壮骨骼。", "晒太阳（皮肤自己合成）、深海鱼、蛋黄、强化奶。", "缺：儿童佝偻病、成人骨软化。老人、少晒太阳的人易缺。")
                    VitaminBlock("🌰", "维生素 E", "抗氧化、保护细胞。", "植物油、坚果、种子、麦胚。", "一般不易缺。")
                    VitaminBlock("🥬", "维生素 K", "帮助凝血、维护骨骼。", "绿叶蔬菜、纳豆。", "一般不易缺。")
                    Ref("整理自《中国营养科学全书》与《中国居民膳食指南(2022)》维生素相关内容，科普性质、非医嘱。")
                }

                Body("二、水溶性维生素（B 族 · C）")
                InsetGroup(title = "不易存，要天天吃点") {
                    VitaminBlock("🌾", "维生素 B1（硫胺素）", "能量代谢、维护神经。", "全谷杂粮、豆类、瘦肉。精米白面吃太多、爱喝酒的人易缺。", "缺：脚气病、乏力。")
                    VitaminBlock("🥛", "维生素 B2（核黄素）", "能量代谢、护皮肤黏膜。", "奶、蛋、动物肝、绿叶菜。", "缺：口角炎、舌炎。")
                    VitaminBlock("🍗", "烟酸（B3）", "能量代谢。", "肉、鱼、全谷、花生。", "缺：癞皮病。")
                    VitaminBlock("🥬", "叶酸（B9）", "造血、孕早期预防胎儿神经管畸形（很关键）。", "深绿叶菜、豆类、动物肝。备孕/孕早期常需额外补充。", "缺：贫血；孕期缺→胎儿神经管缺陷。")
                    VitaminBlock("🥩", "维生素 B12", "造血、维护神经。", "几乎只来自动物性食物（肉、蛋、奶）。", "长期纯素食、老人吸收差者易缺。")
                    VitaminBlock("🍊", "维生素 C", "抗氧化、促进铁吸收、合成胶原（皮肤/血管）。", "新鲜蔬菜水果（橙、猕猴桃、青椒、鲜枣）。久放、久煮会流失。", "缺：牙龈出血、坏血病。")
                    Ref("整理自《中国营养科学全书》《中国居民膳食指南(2022)》与《中国居民膳食营养素参考摄入量(DRIs)》，科普性质、非医嘱。")
                }

                InsetGroup(title = "几个小提示") {
                    Para("· 食物多样、餐餐有蔬菜天天有水果，一般就能满足大部分维生素，不必盲目吃补剂。")
                    Para("· 脂溶性维生素（尤其 A、D）补剂吃过量会在体内蓄积、可能中毒，补充前最好问医生。")
                    Para("· 特殊人群多留意：备孕/孕妇（叶酸）、老人和少晒太阳者（维D）、长期纯素食者（B12）。")
                    Para("· 蔬菜别久煮久放、现做现吃，能减少维C等水溶性维生素的流失。")
                    Ref("以上为一般性膳食建议，个体需要（尤其孕期、慢病、老年）请遵医嘱或营养师指导。")
                }

                LinkRow("→ 看食物在身体里怎么消化吸收（健康科普）", onOpenHealthScience)
                Body("")
            }
        }
    }
}

/** 维生素条目（灰底卡）：emoji + 名称 + 作用 + 食物来源 + 缺乏。[AI生成] 维生素科普 */
@Composable
private fun VitaminBlock(emoji: String, name: String, role: String, source: String, lack: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp),
    ) {
        Row {
            Text("$emoji  ", style = MaterialTheme.typography.titleMedium)
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Text("作用：$role", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        Text("来源：$source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Text(lack, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

/** 顶部免责横幅（灰底圆角卡）。[AI生成] */
@Composable
private fun DisclaimerBanner(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
    )
}

/** 页面导读/分节小标题小字。[AI生成] */
@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

/** 正文要点。[AI生成] */
@Composable
private fun Para(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/** 口径/来源小字。[AI生成] */
@Composable
private fun Ref(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

/** 互链行（primary 色·整行可点·文末 →）。[AI生成] */
@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
