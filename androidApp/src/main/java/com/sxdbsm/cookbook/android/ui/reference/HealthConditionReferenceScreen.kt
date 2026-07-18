package com.sxdbsm.cookbook.android.ui.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.InsetGroup

/**
 * @File : HealthConditionReferenceScreen
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 健康状态参考（App 支持的 4 种慢病·各自饮食关注点 + App 怎么提示 + 口径 + 免责）
 * <p>
 * 用户要求：把一直深挖的健康状态在「我的·参考」下开一页记录，针对每个健康状态明确说明。
 * 纯科普透明展示，守健康免责红线：仅供参考·非医嘱、阈值标口径(国标/FAO-WHO/指南)、无国标标「惯例」、禁医疗断言。
 * <p>
 * [AI生成] 用户 2026-07-18：健康状态参考页。
 **/
@Composable
fun HealthConditionReferenceScreen(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { AppTopBar(title = "健康状态参考", onBack = onBack) },
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
                Body(
                    "App 支持常见慢病 + 部分生命阶段人群的饮食参考。以下为饮食关注点的参考科普，仅供了解、非医嘱，" +
                        "不构成任何诊断或治疗建议；是否适合你、该吃多少，请听你的医生。App 的相关提示只对你在「我的」里登记的健康状态生效。",
                )

                InsetGroup(title = "高血压 · 关注「钠（盐）」") {
                    Para("· 日常参考：清淡少盐、多吃蔬果。膳食指南建议每日食盐 < 5g（钠 < 2000mg）；高血压人群更严，约 2400mg 钠以下为常见参考。")
                    Para("· App 怎么用：今日营养卡按当天摄入的钠估算，偏高时提示「偏咸」；高钠食材在忌口里会标注。多钾（蔬果 / DASH 膳食方向）作正向提示，但不抵消钠、不改评级。")
                    Ref("口径参考：《中国居民膳食指南》《中国高血压防治指南》。")
                }

                InsetGroup(title = "糖尿病 · 关注「升糖（GI）」") {
                    Para("· 日常参考：少精制糖和甜食，主食尽量选低 GI（GI ≤ 55 为低、≥ 70 为高），搭配膳食纤维。")
                    Para("· App 怎么用：菜里含高 GI 食物时提示「可换低 GI 或控量」。膳食纤维作正向提示，与 GI 各自独立、不互相抵消。")
                    Ref("口径参考：GI 分级用 FAO/WHO 口径（我国 WS/T 652-2019 只规定 GI 测定方法、不设食物分级）。")
                }

                InsetGroup(title = "痛风 / 高尿酸 · 关注「嘌呤」") {
                    Para("· 日常参考：急性期避免高嘌呤食物（动物内脏、浓肉汤、部分海鲜如沙丁鱼等），多喝水、限酒。")
                    Para("· App 怎么用：菜里含高嘌呤食物时给「建议避免」的定性提示（不做精确毫克计算）。")
                    Ref("口径参考：《WS/T 560-2017 高尿酸血症与痛风患者膳食指导》（按「应避免 / 限制 / 可选择」给食物清单）。注：嘌呤「高 / 中 / 低」的具体毫克阈值无国家标准，本 App 用惯例定性、非权威分级。")
                }

                InsetGroup(title = "高血脂 · 关注「饱和脂肪、胆固醇」") {
                    Para("· 日常参考：少油腻和肥肉、少动物油，控制高胆固醇食物（如动物内脏，蛋黄适量），多膳食纤维。")
                    Para("· App 怎么用：菜的饱和脂肪或胆固醇偏高时提示「留意」。（部分食材的饱和脂肪 / 胆固醇数据仍在陆续补齐中。）")
                    Ref("口径参考：《中国血脂管理指南（2023）》《中国居民膳食指南》。一般参考：每日饱和脂肪约占能量 < 10%（≈20g）、胆固醇 < 300mg。")
                }

                InsetGroup(title = "孕期 · 关注「叶酸 / 铁 / 钙 / DHA（该多吃）」") {
                    Para("· 多吃方向：深绿叶菜（叶酸）、瘦红肉（铁）、奶和蛋、豆制品、低汞的深海鱼虾（DHA、优质蛋白），种类丰富、清淡为主。")
                    Para("· 要避开：酒（无安全量）、生食（生鱼片等别吃）、部分高汞鱼；动物肝维生素 A 高、限量别多吃；浓茶咖啡限咖啡因、少高盐腌制加工肉。")
                    Para("· App 怎么用：登记「孕期」后，这些避免项进忌口（排末标红），调养里可看孕期推荐食材。")
                    Ref("口径参考：《中国居民膳食指南（2022）·孕期妇女》。孕期饮食请遵产科医生 / 营养师。")
                }

                InsetGroup(title = "哺乳期 · 关注「优质蛋白 / 钙 / 多汤水」") {
                    Para("· 多吃方向：鱼禽蛋瘦肉、奶和豆制品（钙、蛋白）、深色蔬果，适当多喝汤水。")
                    Para("· 要避开：酒（可入乳）、限咖啡因浓茶、少高盐腌制加工。")
                    Ref("口径参考：《中国居民膳食指南（2022）·哺乳期妇女》。请遵医嘱。")
                }

                InsetGroup(title = "婴幼儿（0-2 岁） · ⚠ 先看安全提示") {
                    Para("⚠ 安全提示（不是偏好，是安全）：1 岁内不加盐、不喂蜂蜜（肉毒杆菌风险）；不给整颗坚果（呛噎风险）；不喝含糖 / 碳酸饮料；不用重调味料。")
                    Para("· 该多吃：强化铁米粉、蛋黄、肉泥 / 肝泥（补铁优先）、豆腐、软烂深色蔬菜、刺少的嫩鱼——原味、少盐少糖。")
                    Para("· App 怎么用：登记「婴幼儿」后，禁盐 / 禁蜜 / 禁整坚果等安全项进忌口并更醒目标注。")
                    Ref("口径参考：《中国居民膳食指南（2022）·婴幼儿（0-2 岁）喂养》。婴幼儿喂养务必以儿保科 / 儿科医生指导为准。")
                }

                InsetGroup(title = "学龄前儿童（2-6 岁） · 关注「均衡 / 钙 / 好习惯」") {
                    Para("· 多吃方向：每天奶、鸡蛋、鱼禽瘦肉、深色蔬果、全谷薯类，规律三餐。")
                    Para("· 要少吃：含糖饮料和糖果、油炸 / 膨化食品、高盐腌制。")
                    Ref("口径参考：《中国居民膳食指南（2022）·学龄前儿童》。请遵医嘱 / 儿保科。")
                }

                InsetGroup(title = "学龄儿童（6-17 岁·含青少年） · 关注「钙 / 铁 / 早餐」") {
                    Para("· 多吃方向：天天喝奶（钙、长个）、瘦红肉和动物肝（铁，女生尤其需要）、鱼蛋、全谷蔬果、适量坚果，好好吃早餐。")
                    Para("· 要少吃 / 别碰：含糖饮料、油炸烧烤膨化、高盐；禁酒、少能量饮料。")
                    Ref("口径参考：《中国居民膳食指南（2022）·学龄儿童》。请遵医嘱。")
                }

                InsetGroup(title = "忌口与提示怎么来的") {
                    Para("· 忌口 / 限量 / 推荐：按你登记的健康状态，从食材层给出，家庭里多位成员的忌口会合并（并集）。忌口菜不会被隐藏，只排到最后并标红，家里其他人仍可选。")
                    Para("· 这些都是「仅供参考」，App 只列出告知、不替你决定吃不吃。")
                }

                InsetGroup(title = "重要说明") {
                    Text(
                        "以上均为按公开标准 / 指南整理的参考内容，非权威逐条核对，不作医疗依据。慢病数据为参考整理、部分待完善。" +
                            "尤其孕期与婴幼儿营养关系健康与安全，本 App 内容仅为方向性科普，任何饮食决定请咨询你的医生 / 营养师 / 儿保科。本 App 不提供诊断或治疗建议。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun Para(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun Ref(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    )
}
