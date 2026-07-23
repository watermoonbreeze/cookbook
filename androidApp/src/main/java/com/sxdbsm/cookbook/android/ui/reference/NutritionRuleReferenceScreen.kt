package com.sxdbsm.cookbook.android.ui.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * @File : NutritionRuleReferenceScreen
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 营养计算规则参考页（「营养怎么算的」）——把 App 各种营养数字/评级的计算口径讲给用户
 * <p>
 * 用户要求：把所有营养计算/评级判定口径在「我的·参考资料」单独开一页，方便记忆、显权威可溯源。
 * 与「膳食参考依据」(阈值全表·真相源)、「健康状态参考」(病种关注啥) 切分：本页只讲「怎么算/公式」。
 * 阈值段只做速记 + 参见膳食参考依据(单一真相源防漂移)。守健康免责红线：仅供参考·非医嘱、
 * 嘌呤标「惯例·非国标」、GI 标「FAO/WHO」、正向指标只加提示不改评级、公式翻人话不吓人。
 * 展示文案 ≠ 判定引擎常量(两份独立·数值人工核对搬运·判定引擎不反读本页)。
 * <p>
 * [AI生成] 用户 2026-07-22：营养计算规则参考页。范式仿 HealthConditionReferenceScreen，
 * 加私有 FormulaBlock(人话+公式+示例) 讲清公式；来源复用 DietaryReference.sources 防漂移。
 **/
@Composable
fun NutritionRuleReferenceScreen(
    onBack: () -> Unit,
    onOpenDietaryReference: () -> Unit = {}, // [AI生成] 互链:阈值全表与出处(单一真相源)
    onOpenHealthCondition: () -> Unit = {}, // [AI生成] 互链:每种慢病关注哪个指标
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { AppTopBar(title = "营养怎么算的", onBack = onBack) },
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
                    "以下都是按公开标准和权威指南整理的参考口径，仅供了解、非医嘱，不构成任何诊断或治疗建议。" +
                        "具体到你该吃多少、适不适合，请听你的医生。",
                )
                Body("这一页把 App 里各种营养数字是怎么算出来的、分级的界线画在哪，一次讲清楚，方便你查看和记忆。")

                // 一道菜/餐/天的营养和热量怎么从食材算出来（食材每100g × 用量克重 ÷ 100·累加）
                InsetGroup(title = "一道菜的营养和热量怎么算") {
                    FormulaBlock(
                        title = "一道菜 = 各食材的营养加起来",
                        formula = "每种食材（每100克营养 × 用了多少克 ÷ 100），再全部相加",
                        example = "举例：1 个鸡蛋约 50 克，鸡蛋每 100 克约 144 千卡 → 这个蛋约 144×50÷100 ≈ 72 千卡；菜里每样食材都这么算再加起来，就是这道菜的热量和营养（蛋白、脂肪、钠等都同理）。",
                    )
                    Para("· 用量的「份 / 个 / 勺」会先换成克再算：像 1 两 = 50 克、1 斤 = 500 克、1 勺油盐酱 ≈ 15 克；按个数的（鸡蛋、玉米等）用常见单个重量估（1 个鸡蛋 ≈ 50 克），没记录单个重量的按每件约 60 克粗估。")
                    Para("· 一餐 = 这餐所有菜加起来；一天 = 各餐加起来。再按你「吃了多少、分到多少」折成个人摄入（见下一节）。")
                    Para("· 有的食材还没有营养数据，App 会如实告诉你「几种食材有数据·估算」，不会把没有的当成 0；用量没填全时会显示「营养待完善」，不硬凑一个数。")
                    Ref("口径参考：食材每 100 克营养值主要据《中国食物成分表》（中国疾控中心营养与健康所），并与美国 USDA FoodData Central 交叉核对；部分饱和脂肪 / 胆固醇 / GI / 嘌呤数据仍在陆续补齐。数值仅供参考、非医嘱。")
                }

                // 吃了多少怎么折算（整份 → 个人摄入）
                InsetGroup(title = "吃了多少怎么折算") {
                    FormulaBlock(
                        title = "把整桌菜摊到你个人",
                        formula = "你吃进的 = 整份营养 × 吃掉比例 × 你的份额",
                        example = "举例：一盘菜约 400 千卡，你只吃了一半（×0.5），两口人分你占大约六成（×0.6）→ 你大约摄入 120 千卡。",
                    )
                    Para("· 吃掉了多少：记一餐时你可以选吃完、大部分、一半，还是少量（不选就默认吃完）。少吃一点，个人摄入就相应少算。")
                    Para("· 你分到多少：按家里几个人分、每人饭量不同来估——小孩约 0.5 份、成年女性 1 份、成年男性约 1.2 份、老人约 0.8 份，这些都能在家庭成员里自己调。")
                    Para("· 这套算法只是把整桌菜合理地摊到你个人头上，方便家庭分着吃时各自看摄入，数值仅供参考。")
                }

                // 热量怎么算（个人一天该消耗多少·BMR/TDEE·达标参照）
                InsetGroup(title = "个人一天该消耗多少热量") {
                    FormulaBlock(
                        title = "每天大概消耗多少热量",
                        formula = "基础代谢 × 活动量 = 一天消耗",
                        example = "举例：30 岁女性、60kg、165cm、平时久坐，基础代谢约 1320 千卡，久坐 ×1.2 → 一天约消耗 1580 千卡。",
                        note = "基础代谢用 Mifflin-St Jeor 公式：男 = 10×体重(kg)+6.25×身高(cm)−5×年龄+5，女 = 末项换成 −161。",
                    )
                    Para("· 基础代谢 = 你安静不动时的消耗，按体重、身高、年龄、性别估算；再乘上平时的活动量，就是一天大致消耗。")
                    Para("· 活动量分四档：几乎不动约 1.2 倍、偶尔活动约 1.4 倍、经常活动约 1.55 倍、体力活动多约 1.7 倍。")
                    Para("· 「今天吃得合不合适」不卡一个死数字：在你目标热量上下留了约 15% 的余地，落在这个区间就算合适——多一顿少一顿都很正常，不用为一两百千卡较真。")
                    Para("· 只给 18 岁以上成人估热量；孕期、哺乳期不估（这些阶段需要专门的营养指导）。")
                    Para("· 热量是很个人的事，得结合你的身体数据，这里只是帮你有个大概概念，仅供参考、非医嘱。")
                    Ref(
                        "口径参考：基础代谢用 Mifflin-St Jeor 公式（营养学通用估算法）；每日能量可另参照" +
                            "《中国居民膳食营养素参考摄入量(DRIs)》WS/T 578 系列的能量需要量（EER，按性别、年龄、活动量查表）。个体差异较大，数值为估算。",
                    )
                }

                // 3. 三大营养素怎么看
                InsetGroup(title = "三大营养素怎么看") {
                    Para("· 一天里蛋白质、脂肪、碳水这三类的大致比例，参考是蛋白质约 20%、脂肪约 28%、碳水约 52%（按《中国居民膳食指南》整理）。")
                    Para("· 这只是「看你最近吃得偏了哪一类」的参考——比如肉吃得多、菜和主食少，App 会在推荐时帮你补一补，不是硬性指标，也不用天天卡着比例吃。")
                    Ref("口径参考：三大营养素供能比例据《中国居民膳食指南(2022)》整理。")
                }

                // 4. 一餐搭得均不均衡（色系墙）
                InsetGroup(title = "一餐搭得均不均衡") {
                    Para("· 一餐或一天搭得均不均衡，App 分几档看：吃得很单一是「偏单一」，凑齐三大类是「均衡」，再多几个大类就是「很棒」。")
                    Para("· 三大类指的是：优质蛋白（鱼、肉、禽、蛋、奶、豆任选）+ 主食碳水 + 蔬菜水果。三类都有，就算搭配得不错。")
                    Para("· 这里只看「吃得杂不杂、搭没搭全」，不牵扯热量和慢病那些——那些在首页「今日」里看。")
                }

                // 5. 各项分级阈值（速记）→ 完整表指向膳食参考依据(单一真相源)
                InsetGroup(title = "各项分级阈值（速记）") {
                    Para("下面这些界线是 App 判断「偏高要留意」的参考。同一项还分一般人群和慢病人群（后者更严）。正向的钾、膳食纤维只用来加一句「不错」的提示，不抵消其它项、也不改变分级。")
                    Para("· 钠（咸淡）：一般每天参考不超过约 2000 毫克（相当于食盐 5 克）；高血压人群同样收紧到约 2000 毫克以内（更要严格执行）。偏高时提示「偏咸」。")
                    Para("· 升糖快慢（GI）：一般 55 以下算慢（低）、70 以上算快（高）。App 只按主要食材判断某道菜是否含升糖快的食物、给个提示，不给整道菜算一个精确的 GI 数字（避免看着像很准反而误导）。")
                    Para("· 嘌呤（和痛风、尿酸相关）：习惯上按每 100 克约 25 毫克以下为低、150 毫克以上为高来说。App 只按主要食材做「建议避免 / 留意」这类定性提示，不做精确的毫克计算。")
                    Para("· 饱和脂肪：一般每天约 20 克以内（大致相当于一天热量的 10% 以下）。胆固醇：一般每天不超过约 300 毫克，已有高胆固醇的人更严、约 200 毫克以内。偏高时提示「留意」。")
                    Ref("钾、膳食纤维是正向指标（蔬果、全谷、豆里多），App 只把它们当一句正向提示，不会抵消钠或升糖判断、也不改变分级。")
                    Ref(
                        "口径参考：钠据《中国居民膳食指南(2022)》《中国高血压防治指南(2018)》；GI 分级用 FAO/WHO 口径" +
                            "（我国 WS/T 652-2019 只规定测定方法、不设食物分级）；嘌呤「低 / 中 / 高」三级为惯例说法、没有国家标准、非权威分级" +
                            "（WS/T 560-2017 只给「应避免 / 限制 / 可选择」清单）；饱和脂肪、胆固醇据《中国血脂管理指南(2023)》《成人高脂血症食养指南(2023)》。",
                    )
                    LinkRow("完整阈值表与出处 → 膳食参考依据", onOpenDietaryReference)
                    LinkRow("想知道每种慢病该关注哪个指标 → 健康状态参考", onOpenHealthCondition)
                }

                // 全部参考来源（复用膳食参考依据同批权威·防两页漂移）+ 热量估算说明
                InsetGroup(title = "全部参考来源") {
                    DietaryReference.sources.forEachIndexed { i, s ->
                        SourceRow(i + 1, s.title, listOfNotNull(s.no.ifBlank { null }, s.org, s.year).joinToString(" · "))
                    }
                    SourceRow(
                        DietaryReference.sources.size + 1,
                        "基础代谢估算 Mifflin-St Jeor 公式",
                        "M.D. Mifflin 等 · 1990（营养学与临床常用的成人基础代谢估算法，非国家标准）",
                    )
                }

                Text(
                    "以上都是按公开标准和权威指南整理的参考口径，为帮助你理解 App 里营养提示是怎么来的，不是权威的逐条核对，也不作医疗依据。" +
                        "数值和分级会因人而异，部分数据仍在陆续完善。任何饮食安排，尤其涉及慢病、孕期、儿童，请以你的医生或营养师的意见为准。本 App 不提供诊断或治疗建议。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                Spacer(Modifier.height(16.dp))
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

/** 页面导读小字。[AI生成] */
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

/**
 * 公式块（灰底卡）：人话标题 + 人话公式 + 一个真实数字示例（+ 可选精确公式灰字）。[AI生成]
 * 公式绝不裸奔——三层递进让普通家庭用户看懂，专业用户可按 note 核对。
 */
@Composable
private fun FormulaBlock(title: String, formula: String, example: String, note: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(formula, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        Text(
            example,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (note != null) {
            Text(
                note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
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

/** 来源清单一行：序号 + 标题 + 出处灰字。[AI生成] */
@Composable
private fun SourceRow(no: Int, title: String, meta: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text("$no. $title", style = MaterialTheme.typography.bodyMedium)
        if (meta.isNotBlank()) {
            Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
