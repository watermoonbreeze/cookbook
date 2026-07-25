package com.sxdbsm.cookbook.android.ui.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
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
 * @File : HealthScienceReferenceScreen
 * @Time : 2026/07/25
 * @Author : SXD-AI
 * @Desc : 健康科普页——用人话把「食物在身体里怎么消化吸收转化」和「食材为什么影响某些健康状态」讲清楚
 * <p>
 * 用户 2026-07-25：在「我的·参考资料」加一个健康科普。合议后采用**统一入口·分两层**：
 *   ①食物在身体里的旅程(消化吸收代谢基础) ②食物与健康状态·为什么(机制·连忌口·兼做"App为什么这么判"透明)。
 * 填现有参考页的"机制/WHY"空白(现有页都讲 WHAT=数值/阈值)。与「健康状态参考」(每病关注点)互链不重复。
 * <p>
 * 守健康免责红线：仅供科普了解·非医嘱、不构成诊疗；每段配权威源(生理学/营养学共识·食养指南)。
 * 范式仿 NutritionRuleReferenceScreen(参考页系列统一)。
 * [AI生成] 用户 2026-07-25：健康科普(统一入口·分两层)。
 **/
@Composable
fun HealthScienceReferenceScreen(
    onBack: () -> Unit,
    onOpenHealthCondition: () -> Unit = {}, // 互链:每种慢病关注哪个指标(避免重复)
    onOpenDietaryReference: () -> Unit = {}, // 互链:各阈值全表与出处
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { AppTopBar(title = "健康科普", onBack = onBack) },
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
                    "下面是按营养学、生理学的公认知识整理的科普内容，帮你理解「吃进去的东西在身体里发生了什么」，" +
                        "仅供了解、非医嘱，不构成任何诊断或治疗建议。具体到你该怎么吃，请听你的医生。",
                )
                Body("这一页分两部分：先讲食物在身体里怎么被消化吸收、转化成什么；再讲为什么某些食物会影响血压、血糖、尿酸这些健康状态。")

                // ========== 第一层：基础篇 ==========
                Body("一、食物在身体里的旅程")

                InsetGroup(title = "从一口饭到能量：消化道走一遍") {
                    EmojiChain(
                        listOf("👄" to "口腔·嚼", "🫃" to "胃·拆", "🌀" to "小肠·吸收", "🚽" to "大肠·排"),
                    )
                    Para("· 口腔：牙齿嚼碎食物，唾液里的淀粉酶已经开始把主食的淀粉分解一点点。")
                    Para("· 胃：胃酸和胃蛋白酶把食物搅成食糜，蛋白质在这里先被初步拆解。")
                    Para("· 小肠（主战场）：胰液、胆汁、肠液一起上，把三大营养素彻底分解成小分子，绝大部分营养从这里吸收进血液。")
                    Para("· 大肠：主要吸收水分；没被消化的膳食纤维在这里被肠道菌群发酵、养益生菌，剩下的形成粪便排出。")
                    Ref("整理自《生理学》(人卫教材) 与《中国营养科学全书》消化吸收章节的公认知识，科普性质、非医嘱。")
                }

                InsetGroup(title = "吃了什么，最后变成什么") {
                    NutrientFlow(
                        name = "碳水（主食、糖）",
                        chain = "淀粉/糖 →（淀粉酶）→ 葡萄糖 → 进血液=血糖",
                        fate = "① 直接供能 ② 存成肝糖原、肌糖原备用 ③ 吃太多、用不完的转化成脂肪存起来。精制主食、含糖饮料升血糖快。",
                        flow = listOf("🍚" to "主食/糖", "🍬" to "葡萄糖", "🩸" to "血糖", "⚡" to "能量/存脂"),
                    )
                    NutrientFlow(
                        name = "蛋白质（肉蛋奶豆）",
                        chain = "蛋白质 →（蛋白酶）→ 氨基酸 → 吸收",
                        fate = "① 主要用来盖房子：合成你自身的肌肉、酶、激素、抗体 ② 多余的才拿去供能或转成脂肪。",
                        flow = listOf("🥚" to "蛋白质", "🧩" to "氨基酸", "💪" to "肌肉/酶"),
                    )
                    NutrientFlow(
                        name = "脂肪（油、肥肉、坚果）",
                        chain = "脂肪 →（胆汁乳化 + 脂肪酶）→ 甘油 + 脂肪酸 → 吸收",
                        fate = "① 供能效率最高（每克约 9 千卡，是碳水/蛋白的 2 倍多）② 用不完存进脂肪组织。饱和脂肪吃多了影响血脂。",
                        flow = listOf("🫒" to "脂肪", "💧" to "脂肪酸", "🔥" to "供能", "🧈" to "储存"),
                    )
                    Para("· 能量货币：三大营养素代谢后都变成身体能用的能量（ATP）；长期「吃进来 > 用出去」，多余能量统一存成脂肪 → 慢慢增重。")
                    Para("· 不供能但重要：膳食纤维（帮助消化、平稳血糖血脂、养益生菌）、水、维生素、矿物质，参与身体各种代谢调节。")
                    Ref("整理自《中国营养科学全书》与《中国居民膳食指南(2022)》能量与营养素代谢内容，数值为通用估算、仅供参考、非医嘱。")
                }

                // ========== 第二层：应用篇 ==========
                Body("二、食物与健康状态：为什么")

                InsetGroup(title = "为什么这些食物要留意") {
                    NutrientFlow(
                        name = "高嘌呤 → 尿酸 → 痛风",
                        chain = "内脏、部分海鲜、浓肉汤里的嘌呤 → 在体内代谢成尿酸",
                        fate = "尿酸高了容易在关节沉积、诱发痛风，所以 App 会把动物内脏、贝虾蟹等高嘌呤食物标「避免/限制」。",
                        flow = listOf("🥩" to "内脏/海鲜", "🔺" to "尿酸↑", "🦶" to "痛风"),
                    )
                    NutrientFlow(
                        name = "高钠 → 血压",
                        chain = "盐、腌腊、加工食品里的钠 → 让身体留住更多水",
                        fate = "血容量变大、血压升高，所以高血压要少盐少钠（每天钠建议不超过约 2000mg）。App 会对偏咸的菜给提示。",
                        flow = listOf("🧂" to "钠/盐", "💧" to "留水", "🩸" to "血压↑"),
                    )
                    NutrientFlow(
                        name = "高 GI → 血糖",
                        chain = "精制主食、含糖食物（高升糖指数）→ 血糖升得又快又高",
                        fate = "胰岛素负担大，糖尿病人更适合选低 GI 的粗粮、豆类。App 会在糖尿病档案下提示可换低 GI。",
                        flow = listOf("🍚" to "高GI", "📈" to "血糖快升", "💉" to "胰岛素负担"),
                    )
                    NutrientFlow(
                        name = "饱和脂肪/胆固醇 → 血脂",
                        chain = "肥肉、加工肉、部分油里的饱和脂肪 → 升高「坏胆固醇」(LDL)",
                        fate = "长期偏高影响血脂和血管，所以高血脂要控制饱和脂肪、少吃加工肉。",
                        flow = listOf("🥓" to "饱和脂肪", "🔺" to "坏胆固醇↑", "🫀" to "血脂/血管"),
                    )
                    Para("· 一个规律：越是天然、少加工的食物越友好；越是精制、腌腊、加糖、油炸的加工品，上面这些风险越高。")
                    Ref(
                        "机制与判定口径整理自：《成人高尿酸血症与痛风食养指南(2024)》《成人高血压/高脂血症/糖尿病食养指南(2023)》" +
                            "《中国居民膳食营养素参考摄入量》WS/T 578 等权威指南。为科普与提示口径、仅供参考，具体请遵医嘱。",
                    )
                }

                LinkRow("→ 看每种健康状态具体关注什么（健康状态参考）", onOpenHealthCondition)
                LinkRow("→ 看各项营养的界线和出处（膳食参考依据）", onOpenDietaryReference)
                Body("")
            }
        }
    }
}

/** 营养流转块（灰底卡）：名称 + emoji 示意链 + 文字转化链 + 去向/意义。[AI生成] 健康科普 */
@Composable
private fun NutrientFlow(name: String, chain: String, fate: String, flow: List<Pair<String, String>>? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp),
    ) {
        Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (flow != null) EmojiChain(flow)
        Text(chain, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        Text(
            fate,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** emoji 流程示意链（横向·可滚·大 emoji + 小标签，箭头连接）。[AI生成] 健康科普配图 */
@Composable
private fun EmojiChain(items: List<Pair<String, String>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, (emoji, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (i < items.lastIndex) {
                Text(
                    "→",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
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
